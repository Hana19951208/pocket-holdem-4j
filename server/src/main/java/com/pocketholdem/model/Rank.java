package com.pocketholdem.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 扑克点数枚举
 * 2-10（数值），J=11, Q=12, K=13, A=14
 */
@Getter
public enum Rank {
    TWO("two", 2),
    THREE("three", 3),
    FOUR("four", 4),
    FIVE("five", 5),
    SIX("six", 6),
    SEVEN("seven", 7),
    EIGHT("eight", 8),
    NINE("nine", 9),
    TEN("ten", 10),
    JACK("jack", 11),
    QUEEN("queen", 12),
    KING("king", 13),
    ACE("ace", 14);

    @JsonValue
    private final String name;
    private final int value;

    Rank(String name, int value) {
        this.name = name;
        this.value = value;
    }
}
