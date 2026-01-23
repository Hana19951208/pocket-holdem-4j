package com.pocketholdem.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("底池数据结构测试")
class PotTest {

    @Test
    @DisplayName("应该创建底池")
    void shouldCreatePot() {
        String[] eligiblePlayers = {"player1", "player2", "player3"};
        Pot pot = new Pot(1000, eligiblePlayers);

        assertEquals(1000, pot.amount());
        assertEquals(3, pot.eligiblePlayerIds().length);
    }

    @Test
    @DisplayName("应该序列化为snake_case")
    void shouldSerializeToSnakeCase() throws Exception {
        String[] eligiblePlayers = {"player1"};
        Pot pot = new Pot(1000, eligiblePlayers);
        String json = new ObjectMapper().writeValueAsString(pot);
        assertTrue(json.contains("\"amount\""));
        assertTrue(json.contains("\"eligible_player_ids\""));
    }
}
