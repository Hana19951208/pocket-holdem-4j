package com.pocketholdem.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("花色枚举测试")
class SuitTest {

    @Test
    @DisplayName("应该有4种花色")
    void shouldHaveFourSuits() {
        assertEquals(4, Suit.values().length);
    }

    @Test
    @DisplayName("花色应该有序")
    void suitsShouldBeOrdered() {
        Suit[] suits = Suit.values();
        assertEquals(Suit.SPADES, suits[3]);
    }

    @Test
    @DisplayName("花色序列化应该输出英文")
    void suitShouldSerializeToEnglish() {
        assertEquals("spades", Suit.SPADES.name().toLowerCase());
    }
}
