package com.pocketholdem.engine;

import com.pocketholdem.model.Pot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("边池计算测试")
class PotCalculatorTest {

    @Test
    @DisplayName("单一All-in应该只创建主池")
    void singleAllInShouldCreateOnlyMainPot() {
        String[] playerIds = {"player1", "player2", "player3"};
        int[] bets = {100, 100, 100};  // 所有人都下注相同

        Pot[] pots = PotCalculator.calculateSidePots(playerIds, bets);

        assertEquals(1, pots.length);
        assertEquals(300, pots[0].amount());
        assertArrayEquals(playerIds, pots[0].eligiblePlayerIds());
    }

    @Test
    @DisplayName("两个不同All-in应该创建主池和一个边池")
    void twoDifferentAllInsShouldCreateMainAndSidePot() {
        String[] playerIds = {"player1", "player2", "player3"};
        int[] bets = {50, 100, 100};  // player1 All-in 50

        Pot[] pots = PotCalculator.calculateSidePots(playerIds, bets);

        assertEquals(2, pots.length);
        assertEquals(150, pots[0].amount());  // 主池：50 * 3
        assertEquals(100, pots[1].amount());  // 边池：50 * 2
        assertArrayEquals(playerIds, pots[0].eligiblePlayerIds());
        assertArrayEquals(new String[]{"player2", "player3"}, pots[1].eligiblePlayerIds());
    }

    @Test
    @DisplayName("三个不同All-in应该创建主池和两个边池")
    void threeDifferentAllInsShouldCreateMainAndTwoSidePots() {
        String[] playerIds = {"player1", "player2", "player3"};
        int[] bets = {30, 50, 100};  // 三个不同的All-in

        Pot[] pots = PotCalculator.calculateSidePots(playerIds, bets);

        assertEquals(3, pots.length);
        assertEquals(90, pots[0].amount());   // 主池：30 * 3
        assertEquals(40, pots[1].amount());    // 第一个边池：20 * 2
        assertEquals(50, pots[2].amount());   // 第二个边池：50 * 1
    }
}
