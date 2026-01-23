package com.pocketholdem.model;

/**
 * 牌型评估结果（不可变Record）
 * 包含最终确定的5张最佳手牌及其得分。
 * 
 * @param rank      牌型等级 (High Card -> Royal Flush)
 * @param rankName  牌型名称 (如 "皇家同花顺")
 * @param bestCards 选出的最佳5张牌
 * @param kickers   踢脚牌（用于同级比较）
 * @param score     综合得分（用于直接比较大小，已包含权重）
 */
public record EvaluatedHand(
        HandRank rank,
        String rankName,
        Card[] bestCards,
        Rank[] kickers,
        long score
) {}
