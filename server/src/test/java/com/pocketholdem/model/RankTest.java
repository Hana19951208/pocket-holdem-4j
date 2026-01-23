package com.pocketholdem.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("点数枚举测试")
class RankTest {

    @Test
    @DisplayName("应该有13个点数（2-10, J, Q, K, A）")
    void shouldHaveThirteenRanks() {
        assertEquals(13, Rank.values().length);
    }

    @Test
    @DisplayName("点数应该从2到A有序")
    void ranksShouldBeOrdered() {
        assertEquals(2, Rank.TWO.getValue());
        assertEquals(10, Rank.TEN.getValue());
        assertEquals(11, Rank.JACK.getValue());
        assertEquals(12, Rank.QUEEN.getValue());
        assertEquals(13, Rank.KING.getValue());
        assertEquals(14, Rank.ACE.getValue());
    }

    @Test
    @DisplayName("A应该是最高的")
    void aceShouldBeHighest() {
        assertEquals(14, Rank.ACE.getValue());
    }

    @Test
    @DisplayName("序列化应该输出小写英文")
    void rankShouldSerializeToLowercase() {
        assertEquals("ace", Rank.ACE.getName().toLowerCase());
    }
}
