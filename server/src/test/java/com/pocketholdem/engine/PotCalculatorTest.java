package com.pocketholdem.engine;

import com.pocketholdem.model.Card;
import com.pocketholdem.model.EvaluatedHand;
import com.pocketholdem.model.Pot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;

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

    @Test
    @DisplayName("应该将底池分配给单个获胜者")
    void shouldAwardPotToSingleWinner() {
        Pot[] pots = {
            new Pot(300, new String[]{"player1", "player2", "player3"})
        };

        Map<String, EvaluatedHand> handRanks = new HashMap<>();
        handRanks.put("player1",
            PokerEngine.evaluateFiveCards(new Card[]{
                Card.of("spades", "ace"), Card.of("spades", "king"),
                Card.of("spades", "queen"), Card.of("spades", "jack"),
                Card.of("spades", "ten")
            }));
        handRanks.put("player2",
            PokerEngine.evaluateFiveCards(new Card[]{
                Card.of("hearts", "two"), Card.of("hearts", "three"),
                Card.of("hearts", "four"), Card.of("hearts", "five"),
                Card.of("hearts", "six")
            }));
        handRanks.put("player3",
            PokerEngine.evaluateFiveCards(new Card[]{
                Card.of("clubs", "seven"), Card.of("clubs", "eight"),
                Card.of("clubs", "nine"), Card.of("clubs", "ten"),
                Card.of("clubs", "jack")
            }));

        Map<String, Integer> winnings = PotCalculator.awardPots(pots, handRanks);

        assertEquals(300, winnings.get("player1"));
        assertEquals(0, winnings.get("player2"));
        assertEquals(0, winnings.get("player3"));
    }

    @Test
    @DisplayName("应该在多个获胜者之间平分底池")
    void shouldSplitPotAmongMultipleWinners() {
        Pot[] pots = {
            new Pot(300, new String[]{"player1", "player2", "player3"})
        };

        Card[] sameCards = {
            Card.of("spades", "ace"), Card.of("hearts", "king"),
            Card.of("diamonds", "queen"), Card.of("clubs", "jack"),
            Card.of("spades", "nine")
        };

        Map<String, EvaluatedHand> handRanks = new HashMap<>();
        handRanks.put("player1", PokerEngine.evaluateFiveCards(sameCards));
        handRanks.put("player2", PokerEngine.evaluateFiveCards(sameCards));
        handRanks.put("player3", PokerEngine.evaluateFiveCards(sameCards));

        Map<String, Integer> winnings = PotCalculator.awardPots(pots, handRanks);

        assertEquals(100, winnings.get("player1"));
        assertEquals(100, winnings.get("player2"));
        assertEquals(100, winnings.get("player3"));
    }

    @Test
    @DisplayName("余数应该按座位顺序分配")
    void shouldDistributeRemainderBySeatOrder() {
        Pot[] pots = {
            new Pot(301, new String[]{"player1", "player2", "player3"})
        };

        Card[] sameCards = {
            Card.of("spades", "ace"), Card.of("hearts", "king"),
            Card.of("diamonds", "queen"), Card.of("clubs", "jack"),
            Card.of("spades", "nine")
        };

        Map<String, EvaluatedHand> handRanks = new HashMap<>();
        handRanks.put("player1", PokerEngine.evaluateFiveCards(sameCards));
        handRanks.put("player2", PokerEngine.evaluateFiveCards(sameCards));
        handRanks.put("player3", PokerEngine.evaluateFiveCards(sameCards));

        Map<String, Integer> winnings = PotCalculator.awardPots(pots, handRanks);

        assertEquals(101, winnings.get("player1"));
        assertEquals(100, winnings.get("player2"));
        assertEquals(100, winnings.get("player3"));
    }

    @Test
    @DisplayName("边池应该分配给有资格的玩家")
    void shouldAwardSidePotToEligiblePlayers() {
        Pot[] pots = {
            new Pot(150, new String[]{"player1", "player2", "player3"}),
            new Pot(100, new String[]{"player2", "player3"})
        };

        Map<String, EvaluatedHand> handRanks = new HashMap<>();
        handRanks.put("player1",
            PokerEngine.evaluateFiveCards(new Card[]{
                Card.of("spades", "two"), Card.of("hearts", "three"),
                Card.of("diamonds", "four"), Card.of("clubs", "five"),
                Card.of("spades", "six")
            }));
        handRanks.put("player2",
            PokerEngine.evaluateFiveCards(new Card[]{
                Card.of("spades", "ace"), Card.of("hearts", "king"),
                Card.of("diamonds", "queen"), Card.of("clubs", "jack"),
                Card.of("spades", "ten")
            }));
        handRanks.put("player3",
            PokerEngine.evaluateFiveCards(new Card[]{
                Card.of("hearts", "ace"), Card.of("diamonds", "king"),
                Card.of("clubs", "queen"), Card.of("spades", "jack"),
                Card.of("hearts", "nine")
            }));

        Map<String, Integer> winnings = PotCalculator.awardPots(pots, handRanks);

        assertEquals(0, winnings.get("player1"));
        assertEquals(250, winnings.get("player2"));
        assertEquals(0, winnings.get("player3"));
    }
}
