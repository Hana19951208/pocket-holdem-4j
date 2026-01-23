package com.pocketholdem.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 扑克牌（不可变Record）
 */
public record Card(
        @JsonProperty("suit") Suit suit,
        @JsonProperty("rank") Rank rank
) {
    /**
     * 创建用于测试的便捷方法
     */
    public static Card of(String suit, String rank) {
        return new Card(
                Suit.valueOf(suit.toUpperCase()),
                Rank.valueOf(rank.toUpperCase())
        );
    }
}
