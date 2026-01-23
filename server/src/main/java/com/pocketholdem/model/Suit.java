package com.pocketholdem.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 扑克花色枚举
 * 四种花色：黑桃、红桃、方块、梅花
 */
public enum Suit {
    CLUBS("clubs"),
    DIAMONDS("diamonds"),
    HEARTS("hearts"),
    SPADES("spades");

    private final String value;

    Suit(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
