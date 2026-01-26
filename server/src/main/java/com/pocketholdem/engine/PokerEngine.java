package com.pocketholdem.engine;

import com.pocketholdem.model.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 扑克引擎（纯函数工具类）
 * 负责核心牌型逻辑计算：牌型评估、比牌、得分计算。
 * 所有方法均为静态且无状态，线程安全。
 */
@Slf4j
public final class PokerEngine {

    // 牌型权重基数：10亿 (10^9)，确保牌型等级优先于踢脚牌
    private static final long RANK_WEIGHT_BASE = 1_000_000_000L;

    private PokerEngine() {
        // 工具类，禁止实例化
    }

    /**
     * 评估5张牌的牌型
     * 
     * @param cards 5张扑克牌数组
     * @return 评估结果（包含牌型、最佳组合、得分等）
     */
    public static EvaluatedHand evaluateFiveCards(Card[] cards) {
        if (cards == null || cards.length != 5) {
            log.error("评估手牌失败：必须提供5张牌");
            throw new IllegalArgumentException("必须提供5张牌进行评估");
        }

        // 复制并排序，避免修改原数组
        Card[] sorted = Arrays.copyOf(cards, 5);
        // 按点数降序排序 (Ace=14 -> 2)
        Arrays.sort(sorted, (a, b) -> Integer.compare(b.rank().getValue(), a.rank().getValue()));

        // 1. 预计算牌型特征
        CardGroups groups = groupCards(sorted);
        Card[] straight = detectStraight(sorted);
        boolean isFlush = Arrays.stream(cards)
                .map(Card::suit)
                .distinct()
                .count() == 1;

        if (log.isTraceEnabled()) {
            log.trace("评估手牌: {}, 同花: {}, 顺子: {}",
                    Arrays.toString(sorted), isFlush, straight != null);
        }

        // 2. 依次判断牌型（从大到小）

        // 皇家同花顺 & 同花顺
        if (isFlush && straight != null) {
            if (straight[0].rank() == Rank.ACE && straight[1].rank() == Rank.KING) {
                // A, K, Q, J, 10
                return createEvaluatedHand(HandRank.ROYAL_FLUSH, "皇家同花顺", straight);
            } else {
                return createEvaluatedHand(HandRank.STRAIGHT_FLUSH, "同花顺", straight);
            }
        }

        // 四条
        if (!groups.four().isEmpty()) {
            Card[] best = new Card[5];
            System.arraycopy(groups.four().toArray(new Card[0]), 0, best, 0, 4);
            best[4] = groups.singles().get(0);
            return createEvaluatedHand(HandRank.FOUR_OF_A_KIND, "四条", best);
        }

        // 葫芦 (三条 + 对子)
        if (!groups.three().isEmpty() && !groups.pairs().isEmpty()) {
            Card[] best = new Card[5];
            System.arraycopy(groups.three().toArray(new Card[0]), 0, best, 0, 3);
            System.arraycopy(groups.pairs().toArray(new Card[0]), 0, best, 3, 2);
            return createEvaluatedHand(HandRank.FULL_HOUSE, "葫芦", best);
        }

        // 同花
        if (isFlush) {
            return createEvaluatedHand(HandRank.FLUSH, "同花", sorted);
        }

        // 顺子
        if (straight != null) {
            return createEvaluatedHand(HandRank.STRAIGHT, "顺子", straight);
        }

        // 三条
        if (!groups.three().isEmpty()) {
            Card[] best = new Card[5];
            System.arraycopy(groups.three().toArray(new Card[0]), 0, best, 0, 3);
            best[3] = groups.singles().get(0);
            best[4] = groups.singles().get(1);
            return createEvaluatedHand(HandRank.THREE_OF_A_KIND, "三条", best);
        }

        // 两对
        if (groups.pairs().size() >= 4) { // pairs列表包含所有对子的牌，2对=4张
            Card[] best = new Card[5];
            Card[] pairsArray = groups.pairs().toArray(new Card[0]);
            // 取最大的两对 (已排序)
            System.arraycopy(pairsArray, 0, best, 0, 4);
            // 找最大的单张作为踢脚
            for (int i = 4, j = 0; i < 5 && j < groups.singles().size(); i++, j++) {
                best[i] = groups.singles().get(j);
            }
            return createEvaluatedHand(HandRank.TWO_PAIR, "两对", best);
        }

        // 一对
        if (!groups.pairs().isEmpty()) {
            Card[] best = new Card[5];
            Card[] pairsArray = groups.pairs().toArray(new Card[0]);
            System.arraycopy(pairsArray, 0, best, 0, 2);
            // 补齐3个单张踢脚
            for (int i = 2, j = 0; i < 5 && j < groups.singles().size(); i++, j++) {
                best[i] = groups.singles().get(j);
            }
            return createEvaluatedHand(HandRank.ONE_PAIR, "一对", best);
        }

        // 高牌
        return createEvaluatedHand(HandRank.HIGH_CARD, "高牌", sorted);
    }

    /**
     * 检测是否为顺子
     * 支持常规顺子 (10-J-Q-K-A) 和轮盘顺 (A-2-3-4-5)
     * 
     * @param sortedCards 已降序排列的5张牌
     * @return 如果是顺子返回排序后的数组（轮盘顺会将A移到最后），否则返回 null
     */
    private static Card[] detectStraight(Card[] sortedCards) {
        boolean isConsecutive = true;
        for (int i = 0; i < 4; i++) {
            if (sortedCards[i].rank().getValue() - sortedCards[i + 1].rank().getValue() != 1) {
                isConsecutive = false;
                break;
            }
        }

        if (isConsecutive) {
            return sortedCards;
        }

        // 特殊处理轮盘顺 (Wheel Straight): A-5-4-3-2
        // 原始排序为: A(14), 5(5), 4(4), 3(3), 2(2)
        if (sortedCards[0].rank() == Rank.ACE &&
                sortedCards[1].rank() == Rank.FIVE &&
                sortedCards[2].rank() == Rank.FOUR &&
                sortedCards[3].rank() == Rank.THREE &&
                sortedCards[4].rank() == Rank.TWO) {

            // 重新排列为: 5-4-3-2-A (A作为1使用)
            return new Card[] {
                    sortedCards[1], sortedCards[2], sortedCards[3],
                    sortedCards[4], sortedCards[0]
            };
        }

        return null;
    }

    /**
     * 按点数对牌进行分组（四条、三条、对子、单张）
     * 
     * @param sortedCards 已排序的牌数组
     * @return 分组结果
     */
    private static CardGroups groupCards(Card[] sortedCards) {
        Map<Rank, List<Card>> rankMap = new HashMap<>();

        for (Card card : sortedCards) {
            rankMap.computeIfAbsent(card.rank(), k -> new ArrayList<>()).add(card);
        }

        List<Card> four = new ArrayList<>();
        List<Card> three = new ArrayList<>();
        List<Card> pairs = new ArrayList<>();
        List<Card> singles = new ArrayList<>();

        for (List<Card> group : rankMap.values()) {
            switch (group.size()) {
                case 4:
                    four.addAll(group);
                    break;
                case 3:
                    three.addAll(group);
                    break;
                case 2:
                    pairs.addAll(group);
                    break;
                case 1:
                    singles.addAll(group);
                    break;
            }
        }

        // 各组内按点数降序排序
        four.sort((a, b) -> Integer.compare(b.rank().getValue(), a.rank().getValue()));
        three.sort((a, b) -> Integer.compare(b.rank().getValue(), a.rank().getValue()));
        pairs.sort((a, b) -> Integer.compare(b.rank().getValue(), a.rank().getValue()));
        singles.sort((a, b) -> Integer.compare(b.rank().getValue(), a.rank().getValue()));

        return new CardGroups(four, three, pairs, singles);
    }

    /**
     * 创建评估结果对象并计算得分
     */
    private static EvaluatedHand createEvaluatedHand(
            HandRank rank,
            String rankName,
            Card[] bestCards) {

        Rank[] kickers = Arrays.stream(bestCards)
                .map(Card::rank)
                .toArray(Rank[]::new);

        long score = calculateScore(rank, kickers);

        if (log.isDebugEnabled()) {
            log.debug("牌型识别: {} (等级={}), 得分: {}", rankName, rank.getValue(), score);
        }

        return new EvaluatedHand(rank, rankName, bestCards, kickers, score);
    }

    /**
     * 从手牌和公共牌中找出最佳5张牌组合
     * 使用组合算法遍历所有 C(7,5) = 21 种可能性
     * 
     * @param holeCards      2张手牌
     * @param communityCards 3-5张公共牌
     * @return 最佳牌型评估结果
     */
    public static EvaluatedHand evaluateHand(Card[] holeCards, Card[] communityCards) {
        if (holeCards == null || communityCards == null) {
            throw new IllegalArgumentException("手牌和公共牌不能为空");
        }

        int totalCards = holeCards.length + communityCards.length;
        if (totalCards < 5) {
            throw new IllegalArgumentException("总牌数不足5张，无法评估");
        }

        // 合并所有可用牌
        Card[] allCards = new Card[totalCards];
        System.arraycopy(holeCards, 0, allCards, 0, holeCards.length);
        System.arraycopy(communityCards, 0, allCards, holeCards.length, communityCards.length);

        if (log.isDebugEnabled()) {
            log.debug("开始评估最佳组合，总牌数: {}", totalCards);
        }

        // 生成所有5张牌的组合
        List<Card[]> combinations = getCombinations(allCards, 5);

        EvaluatedHand bestHand = null;
        for (Card[] combo : combinations) {
            EvaluatedHand evaluated = evaluateFiveCards(combo);
            // 更新最佳牌型
            if (bestHand == null || evaluated.score() > bestHand.score()) {
                bestHand = evaluated;
            }
        }

        if (bestHand != null) {
            log.info("最佳牌型: {}, 得分: {}", bestHand.rankName(), bestHand.score());
        }

        return bestHand;
    }

    /**
     * 生成从n张牌中选k张的所有组合 (递归实现)
     */
    private static List<Card[]> getCombinations(Card[] cards, int k) {
        List<Card[]> result = new ArrayList<>();
        Card[] current = new Card[k];
        combine(cards, k, 0, 0, current, result);
        return result;
    }

    private static void combine(
            Card[] cards,
            int k,
            int start,
            int index,
            Card[] current,
            List<Card[]> result) {

        if (index == k) {
            result.add(Arrays.copyOf(current, k));
            return;
        }

        for (int i = start; i < cards.length; i++) {
            current[index] = cards[i];
            combine(cards, k, i + 1, index + 1, current, result);
        }
    }

    /**
     * 比较两个牌型的大小
     * 
     * @param hand1 牌型1
     * @param hand2 牌型2
     * @return 正数表示hand1大，负数表示hand2大，0表示平局
     */
    public static int compareHands(EvaluatedHand hand1, EvaluatedHand hand2) {
        return Long.compare(hand1.score(), hand2.score());
    }

    /**
     * 计算牌型综合得分
     * 算法：牌型等级 * 10亿 + Σ(踢脚牌点数 * 10^(8-2i))
     * 保证牌型等级权重绝对高于任何踢脚牌组合
     */
    private static long calculateScore(HandRank rank, Rank[] kickers) {
        long score = (long) rank.getValue() * RANK_WEIGHT_BASE;

        for (int i = 0; i < kickers.length && i < 5; i++) {
            int kickerValue = kickers[i].getValue();
            // 权重依次递减：10^8, 10^6, 10^4, 10^2, 10^0
            long weight = (long) Math.pow(10, 8 - i * 2);
            score += (long) kickerValue * weight;
        }

        return score;
    }

    // ==================== 牌组管理方法 ====================

    /**
     * 创建52张标准扑克牌组
     * 牌组顺序：按花色从 clubs → diamonds → hearts → spades，
     * 每个花色内部按点数从 ace → 2 降序排列
     *
     * @return 52张牌的列表
     */
    public static List<Card> createDeck() {
        List<Card> deck = new ArrayList<>(52);

        // 外层循环：花色（CLUBS → DIAMONDS → HEARTS → SPADES）
        for (Suit suit : Suit.values()) {
            // 内层循环：点数（ACE → 2 降序）
            for (Rank rank : Rank.values()) {
                deck.add(Card.of(suit.getValue(), rank.getName()));
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("创建牌组完成，共{}张牌", deck.size());
        }

        return deck;
    }

    /**
     * Fisher-Yates 洗牌算法（原地打乱）
     * 时间复杂度：O(n)
     * 线程安全：每次调用操作不同的牌组实例，无共享状态
     *
     * @param deck 牌组（会被修改）
     */
    public static void shuffleDeck(List<Card> deck) {
        if (deck == null || deck.size() <= 1) {
            return; // 空牌组或单张牌无需洗牌
        }

        Random random = new Random();
        int n = deck.size();

        // 从后向前，每次随机交换
        for (int i = n - 1; i > 0; i--) {
            // 生成 [0, i] 的随机索引
            int j = random.nextInt(i + 1);

            // 交换 deck[i] 和 deck[j]
            Card temp = deck.get(i);
            deck.set(i, deck.get(j));
            deck.set(j, temp);
        }

        if (log.isDebugEnabled()) {
            log.debug("牌组洗牌完成");
        }
    }

    /**
     * 从牌堆顶部抽取指定数量的牌
     *
     * @param deck  牌组（会被修改）
     * @param count 要抽取的数量
     * @return 抽取的牌列表（如果牌堆不足，返回全部剩余牌）
     */
    public static List<Card> dealCards(List<Card> deck, int count) {
        if (deck == null || count <= 0) {
            return new ArrayList<>();
        }

        // 计算实际可抽取的数量
        int actualCount = Math.min(count, deck.size());

        if (actualCount == 0) {
            return new ArrayList<>();
        }

        // 抽取顶部的牌
        List<Card> dealt = new ArrayList<>(deck.subList(0, actualCount));

        // 从牌堆移除已抽取的牌
        deck.subList(0, actualCount).clear();

        if (log.isDebugEnabled()) {
            log.debug("从牌堆抽取{}张牌，剩余{}张", actualCount, deck.size());
        }

        return dealt;
    }

    /**
     * 给多个玩家发底牌（每人2张）
     * 发牌顺序：从索引0开始，每人依次发1张，循环2轮
     *
     * @param deck        牌组（会被修改）
     * @param playerCount 玩家数量
     * @return Map<玩家索引, 手牌列表>
     */
    public static Map<Integer, List<Card>> dealHoleCards(List<Card> deck, int playerCount) {
        if (deck == null || playerCount <= 0) {
            return new HashMap<>();
        }

        Map<Integer, List<Card>> holeCards = new HashMap<>();

        // 循环2轮（每人2张）
        for (int round = 0; round < 2; round++) {
            for (int playerIndex = 0; playerIndex < playerCount; playerIndex++) {
                if (deck.isEmpty()) {
                    break; // 牌堆不足，停止发牌
                }

                Card card = deck.remove(0);
                holeCards.computeIfAbsent(playerIndex, k -> new ArrayList<>()).add(card);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("发底牌完成，玩家数: {}, 每轮发牌: {}", playerCount, playerCount * 2);
        }

        return holeCards;
    }

    /**
     * 发公共牌
     * FLOP: 烧1张 + 发3张
     * TURN: 烧1张 + 发1张
     * RIVER: 烧1张 + 发1张
     * PRE_FLOP/SHOWDOWN: 不发牌
     *
     * @param deck  牌组（会被修改）
     * @param phase 当前游戏阶段
     * @return 本轮发的公共牌列表
     */
    public static List<Card> dealCommunityCards(List<Card> deck, GamePhase phase) {
        if (deck == null || deck.isEmpty()) {
            return new ArrayList<>();
        }

        return switch (phase) {
            case FLOP -> {
                // 烧1张 + 发3张
                if (!deck.isEmpty())
                    deck.remove(0);
                List<Card> flop = dealCards(deck, 3);
                yield flop;
            }
            case TURN -> {
                // 烧1张 + 发1张
                if (!deck.isEmpty())
                    deck.remove(0);
                List<Card> turn = dealCards(deck, 1);
                yield turn;
            }
            case RIVER -> {
                // 烧1张 + 发1张
                if (!deck.isEmpty())
                    deck.remove(0);
                List<Card> river = dealCards(deck, 1);
                yield river;
            }
            default -> new ArrayList<>(); // PRE_FLOP, SHOWDOWN 不发牌
        };
    }
}