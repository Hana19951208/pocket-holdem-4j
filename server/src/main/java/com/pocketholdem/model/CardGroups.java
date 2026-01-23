package com.pocketholdem.model;

import java.util.List;

/**
 * 牌型分组结果
 */
public record CardGroups(
        List<Card> four,      // 四条
        List<Card> three,     // 三条
        List<Card> pairs,     // 对子（可能有多个）
        List<Card> singles    // 单张
) {}
