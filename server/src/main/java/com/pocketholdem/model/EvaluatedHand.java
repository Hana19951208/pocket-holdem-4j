package com.pocketholdem.model;

/**
 * 牌型评估结果（不可变Record）
 */
public record EvaluatedHand(
        HandRank rank,          // 牌型等级
        String rankName,        // 牌型名称
        Card[] bestCards,       // 最佳5张牌
        Rank[] kickers,         // 踢脚牌
        long score              // 综合得分
) {}
