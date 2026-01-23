package com.pocketholdem.engine;

import com.pocketholdem.model.*;

import java.util.Arrays;

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

        if (isFlush) {
            return createEvaluatedHand(HandRank.FLUSH, "同花", sorted);
        }

        if (straight != null) {
            return createEvaluatedHand(HandRank.STRAIGHT, "顺子", straight);
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

    private static EvaluatedHand createEvaluatedHand(
            HandRank rank,
            String rankName,
            Card[] bestCards) {

        Rank[] kickers = Arrays.stream(bestCards)
            .map(Card::rank)
            .toArray(Rank[]::new);

        int score = calculateScore(rank, kickers);

        return new EvaluatedHand(rank, rankName, bestCards, kickers, score);
    }

    /**
     * 从手牌和公共牌中找出最佳5张牌组合
     */
    public static EvaluatedHand evaluateHand(Card[] holeCards, Card[] communityCards) {
        return evaluateFiveCards(holeCards);
    }

    /**
     * 比较两个牌型
     * 返回正数表示hand1更强，负数表示hand2更强，0表示平局
     */
    public static int compareHands(EvaluatedHand hand1, EvaluatedHand hand2) {
        return Integer.compare(hand1.score(), hand2.score());
    }

    private static int calculateScore(HandRank rank, Rank[] kickers) {
        int score = rank.getValue() * 100_000_000;

        for (int i = 0; i < kickers.length && i < 5; i++) {
            int kickerValue = kickers[i].getValue();
            int weight = (int) Math.pow(10, 8 - i * 2);
            score += kickerValue * weight;
        }

        return score;
    }
}
