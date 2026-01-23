package com.pocketholdem.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 扑克牌（不可变Record）
 * 代表一张具体的扑克牌，由花色和点数组成。
 * 
 * @param suit 花色 (Spades, Hearts, Diamonds, Clubs)
 * @param rank 点数 (2-10, J, Q, K, A)
 */
public record Card(
        @JsonProperty("suit") Suit suit,
        @JsonProperty("rank") Rank rank
) {
    /**
     * 创建扑克牌的便捷工厂方法
     * 
     * @param suit 花色字符串 (英文，大小写不敏感)
     * @param rank 点数字符串 (英文，大小写不敏感)
     * @return 扑克牌实例
     */
    public static Card of(String suit, String rank) {
        return new Card(
                Suit.valueOf(suit.toUpperCase()),
                Rank.valueOf(rank.toUpperCase())
        );
    }
}
