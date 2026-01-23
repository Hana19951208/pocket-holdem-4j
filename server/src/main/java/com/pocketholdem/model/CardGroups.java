package com.pocketholdem.model;

import java.util.List;

/**
 * 牌型分组结果
 * 用于辅助牌型判断，将5张牌按点数出现次数分类。
 * 
 * @param four    四张点数相同的牌 (四条)
 * @param three   三张点数相同的牌 (三条)
 * @param pairs   两张点数相同的牌 (对子)
 * @param singles 单张牌
 */
public record CardGroups(
        List<Card> four,
        List<Card> three,
        List<Card> pairs,
        List<Card> singles
) {}
