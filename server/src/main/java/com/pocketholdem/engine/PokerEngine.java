package com.pocketholdem.engine;

import com.pocketholdem.model.*;

import java.util.*;

/**
 * 扑克引擎（纯函数工具类）
 * 所有方法都是静态的，无状态
 */
public final class PokerEngine {

    private PokerEngine() {
        // 工具类，禁止实例化
    }

    /**
     * 评估5张牌的牌型
     */
    public static EvaluatedHand evaluateFiveCards(Card[] cards) {
        Card[] sorted = Arrays.copyOf(cards, 5);
        Arrays.sort(sorted, (a, b) -> Integer.compare(b.rank().getValue(), a.rank().getValue()));

        CardGroups groups = groupCards(sorted);
        Card[] straight = detectStraight(sorted);
        boolean isFlush = Arrays.stream(cards)
            .map(Card::suit)
            .distinct()
            .count() == 1;

        if (isFlush && straight != null) {
            if (straight[0].rank() == Rank.ACE && straight[1].rank() == Rank.KING) {
                return createEvaluatedHand(HandRank.ROYAL_FLUSH, "皇家同花顺", straight);
            } else {
                return createEvaluatedHand(HandRank.STRAIGHT_FLUSH, "同花顺", straight);
            }
        }

        if (!groups.four().isEmpty()) {
            Card[] best = new Card[5];
            System.arraycopy(groups.four().toArray(new Card[0]), 0, best, 0, 4);
            best[4] = groups.singles().get(0);
            return createEvaluatedHand(HandRank.FOUR_OF_A_KIND, "四条", best);
        }

        if (!groups.three().isEmpty() && !groups.pairs().isEmpty()) {
            Card[] best = new Card[5];
            System.arraycopy(groups.three().toArray(new Card[0]), 0, best, 0, 3);
            System.arraycopy(groups.pairs().toArray(new Card[0]), 0, best, 3, 2);
            return createEvaluatedHand(HandRank.FULL_HOUSE, "葫芦", best);
        }

        if (isFlush) {
            return createEvaluatedHand(HandRank.FLUSH, "同花", sorted);
        }

        if (straight != null) {
            return createEvaluatedHand(HandRank.STRAIGHT, "顺子", straight);
        }

        if (!groups.three().isEmpty()) {
            Card[] best = new Card[5];
            System.arraycopy(groups.three().toArray(new Card[0]), 0, best, 0, 3);
            best[3] = groups.singles().get(0);
            best[4] = groups.singles().get(1);
            return createEvaluatedHand(HandRank.THREE_OF_A_KIND, "三条", best);
        }

        if (groups.pairs().size() >= 4) {
            Card[] best = new Card[5];
            Card[] pairsArray = groups.pairs().toArray(new Card[0]);
            System.arraycopy(pairsArray, 0, best, 0, Math.min(pairsArray.length, 4));
            for (int i = pairsArray.length, j = 0; i < 5 && j < groups.singles().size(); i++, j++) {
                best[i] = groups.singles().get(j);
            }
            return createEvaluatedHand(HandRank.TWO_PAIR, "两对", best);
        }

        if (groups.pairs().size() >= 2) {
            Card[] best = new Card[5];
            Card[] pairsArray = groups.pairs().toArray(new Card[0]);
            System.arraycopy(pairsArray, 0, best, 0, 2);
            for (int i = 2, j = 0; i < 5 && j < groups.singles().size(); i++, j++) {
                best[i] = groups.singles().get(j);
            }
            return createEvaluatedHand(HandRank.ONE_PAIR, "一对", best);
        }

        if (groups.pairs().size() >= 4) {
            Card[] best = new Card[5];
            Card[] pairsArray = groups.pairs().toArray(new Card[0]);
            System.arraycopy(pairsArray, 0, best, 0, Math.min(pairsArray.length, 4));
            for (int i = pairsArray.length, j = 0; i < 5 && j < groups.singles().size(); i++, j++) {
                best[i] = groups.singles().get(j);
            }
            return createEvaluatedHand(HandRank.TWO_PAIR, "两对", best);
        }

        if (!groups.pairs().isEmpty()) {
            Card[] best = new Card[5];
            Card[] pairsArray = groups.pairs().toArray(new Card[0]);
            System.arraycopy(pairsArray, 0, best, 0, Math.min(pairsArray.length, 2));
            for (int i = pairsArray.length, j = 0; i < 5 && j < groups.singles().size(); i++, j++) {
                best[i] = groups.singles().get(j);
            }
            return createEvaluatedHand(HandRank.ONE_PAIR, "一对", best);
        }

        return createEvaluatedHand(HandRank.HIGH_CARD, "高牌", sorted);
    }

    /**
     * 检测是否为顺子
     * 返回排序后的牌数组，如果不是顺子则返回null
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

        if (sortedCards[0].rank() == Rank.ACE &&
            sortedCards[1].rank() == Rank.FIVE &&
            sortedCards[2].rank() == Rank.FOUR &&
            sortedCards[3].rank() == Rank.THREE &&
            sortedCards[4].rank() == Rank.TWO) {

            return new Card[] {
                sortedCards[1], sortedCards[2], sortedCards[3],
                sortedCards[4], sortedCards[0]
            };
        }

        return null;
    }

    /**
     * 按点数统计牌型分组
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

        four.sort((a, b) -> Integer.compare(b.rank().getValue(), a.rank().getValue()));
        three.sort((a, b) -> Integer.compare(b.rank().getValue(), a.rank().getValue()));
        pairs.sort((a, b) -> Integer.compare(b.rank().getValue(), a.rank().getValue()));
        singles.sort((a, b) -> Integer.compare(b.rank().getValue(), a.rank().getValue()));

        return new CardGroups(four, three, pairs, singles);
    }

    private static EvaluatedHand createEvaluatedHand(
            HandRank rank,
            String rankName,
            Card[] bestCards) {

        Rank[] kickers = Arrays.stream(bestCards)
            .map(Card::rank)
            .toArray(Rank[]::new);

        long score = calculateScore(rank, kickers);

        return new EvaluatedHand(rank, rankName, bestCards, kickers, score);
    }

    /**
     * 从手牌和公共牌中找出最佳5张牌组合
     */
    public static EvaluatedHand evaluateHand(Card[] holeCards, Card[] communityCards) {
        Card[] allCards = new Card[holeCards.length + communityCards.length];
        System.arraycopy(holeCards, 0, allCards, 0, holeCards.length);
        System.arraycopy(communityCards, 0, allCards, holeCards.length, communityCards.length);

        List<Card[]> combinations = getCombinations(allCards, 5);

        EvaluatedHand bestHand = null;
        for (Card[] combo : combinations) {
            EvaluatedHand evaluated = evaluateFiveCards(combo);
            if (bestHand == null || evaluated.score() > bestHand.score()) {
                bestHand = evaluated;
            }
        }

        return bestHand;
    }

    /**
     * 生成从n张牌中选k张的所有组合
     */
    private static List<Card[]> getCombinations(Card[] cards, int k) {
        List<Card[]> result = new ArrayList<>();
        Card[] current = new Card[k];
        combine(cards, k, 0, 0, current, result);
        return result;
    }

    /**
     * 递归生成组合
     */
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
     * 比较两个牌型
     * 返回正数表示hand1更强，负数表示hand2更强，0表示平局
     */
    public static int compareHands(EvaluatedHand hand1, EvaluatedHand hand2) {
        return Long.compare(hand1.score(), hand2.score());
    }

    private static long calculateScore(HandRank rank, Rank[] kickers) {
        long score = (long) rank.getValue() * 1_000_000_000L;

        for (int i = 0; i < kickers.length && i < 5; i++) {
            int kickerValue = kickers[i].getValue();
            long weight = (long) Math.pow(10, 8 - i * 2);
            score += (long) kickerValue * weight;
        }

        return score;
    }
}
