package com.pocketholdem.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("牌型等级枚举测试")
class HandRankTest {

    @Test
    @DisplayName("应该有10种牌型")
    void shouldHaveTenHandRanks() {
        assertEquals(10, HandRank.values().length);
    }

    @Test
    @DisplayName("牌型应该从高牌到皇家同花顺有序")
    void handRanksShouldBeOrdered() {
        assertEquals(1, HandRank.HIGH_CARD.getValue());
        assertEquals(10, HandRank.ROYAL_FLUSH.getValue());
    }

    @Test
    @DisplayName("皇家同花顺应该是最高的")
    void royalFlushShouldBeHighest() {
        assertEquals(10, HandRank.ROYAL_FLUSH.getValue());
    }

    @Test
    @DisplayName("高牌应该是最低的")
    void highCardShouldBeLowest() {
        assertEquals(1, HandRank.HIGH_CARD.getValue());
    }
}
