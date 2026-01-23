package com.pocketholdem.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 扑克牌型等级枚举
 * 从高牌（1）到皇家同花顺（10）
 */
@Getter
public enum HandRank {
    HIGH_CARD("high_card", 1),
    ONE_PAIR("one_pair", 2),
    TWO_PAIR("two_pair", 3),
    THREE_OF_A_KIND("three_of_a_kind", 4),
    STRAIGHT("straight", 5),
    FLUSH("flush", 6),
    FULL_HOUSE("full_house", 7),
    FOUR_OF_A_KIND("four_of_a_kind", 8),
    STRAIGHT_FLUSH("straight_flush", 9),
    ROYAL_FLUSH("royal_flush", 10);

    @JsonValue
    private final String name;
    private final int value;

    HandRank(String name, int value) {
        this.name = name;
        this.value = value;
    }
}
