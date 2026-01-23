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

        Rank[] kickers = Arrays.stream(sorted).map(Card::rank).toArray(Rank[]::new);
        int score = calculateScore(HandRank.HIGH_CARD, kickers);

        return new EvaluatedHand(
            HandRank.HIGH_CARD,
            "高牌",
            sorted,
            kickers,
            score
        );
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
