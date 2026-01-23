package com.pocketholdem.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("扑克牌Record测试")
class CardTest {

    @Test
    @DisplayName("应该创建一张牌")
    void shouldCreateCard() {
        Card card = new Card(Suit.SPADES, Rank.ACE);
        assertEquals(Suit.SPADES, card.suit());
        assertEquals(Rank.ACE, card.rank());
    }

    @Test
    @DisplayName("应该序列化为snake_case")
    void shouldSerializeToSnakeCase() throws Exception {
        Card card = new Card(Suit.SPADES, Rank.ACE);
        String json = new ObjectMapper().writeValueAsString(card);
        assertTrue(json.contains("\"suit\":\"spades\""));
        assertTrue(json.contains("\"rank\":\"ace\""));
    }

    @Test
    @DisplayName("相同的花色和点数应该相等")
    void sameSuitAndRankShouldBeEqual() {
        Card card1 = new Card(Suit.SPADES, Rank.ACE);
        Card card2 = new Card(Suit.SPADES, Rank.ACE);
        assertEquals(card1, card2);
    }

    @Test
    @DisplayName("不同的花色或点数应该不相等")
    void differentShouldNotBeEqual() {
        Card card1 = new Card(Suit.SPADES, Rank.ACE);
        Card card2 = new Card(Suit.HEARTS, Rank.ACE);
        assertNotEquals(card1, card2);
    }
}
