package com.pocketholdem.engine;

import com.pocketholdem.model.EvaluatedHand;
import com.pocketholdem.model.Pot;
import com.pocketholdem.util.ChipCalculator;

import java.util.*;

public final class PotCalculator {

    private PotCalculator() {
    }

    public static Pot[] calculateSidePots(String[] playerIds, int[] bets) {
        if (playerIds.length != bets.length) {
            throw new IllegalArgumentException(
                "玩家ID和下注数组长度不一致"
            );
        }

        if (playerIds.length == 0) {
            return new Pot[0];
        }

        Map<String, Integer> playerBets = new HashMap<>();
        for (int i = 0; i < playerIds.length; i++) {
            playerBets.put(playerIds[i], bets[i]);
        }

        String[] sortedPlayers = Arrays.copyOf(playerIds, playerIds.length);
        Arrays.sort(sortedPlayers,
            (a, b) -> Integer.compare(playerBets.get(a), playerBets.get(b))
        );

        List<Pot> pots = new ArrayList<>();
        int prevBet = 0;

        for (int i = 0; i < sortedPlayers.length; i++) {
            String currentPlayer = sortedPlayers[i];
            int currentBet = playerBets.get(currentPlayer);
            int betDiff = ChipCalculator.safeSubtract(currentBet, prevBet);

            if (betDiff > 0) {
                int contributorsCount = sortedPlayers.length - i;

                int potAmount = ChipCalculator.safeMultiply(betDiff, contributorsCount);

                String[] eligiblePlayers = Arrays.copyOfRange(
                    sortedPlayers, i, sortedPlayers.length
                );

                pots.add(new Pot(potAmount, eligiblePlayers));

                prevBet = currentBet;
            }
        }

        return pots.toArray(new Pot[0]);
    }

    /**
     * 分配底池给获胜玩家
     *
     * @param pots 底池列表
     * @param handRanks 玩家ID到牌型的映射
     * @return 玩家ID到赢得筹码的映射
     */
    public static Map<String, Integer> awardPots(
            Pot[] pots,
            Map<String, EvaluatedHand> handRanks) {

        Map<String, Integer> winnings = new HashMap<>();

        for (String playerId : handRanks.keySet()) {
            winnings.put(playerId, 0);
        }

        for (Pot pot : pots) {
            List<String> winners = findWinners(pot, handRanks);
            distributePot(pot, winners, winnings);
        }

        return winnings;
    }

    /**
     * 找出某个底池的最高分玩家
     */
    private static List<String> findWinners(
            Pot pot,
            Map<String, EvaluatedHand> handRanks) {

        String[] eligiblePlayers = pot.eligiblePlayerIds();

        if (eligiblePlayers.length == 0) {
            return Collections.emptyList();
        }

        List<String> winners = new ArrayList<>();
        long highestScore = Long.MIN_VALUE;

        for (String playerId : eligiblePlayers) {
            EvaluatedHand hand = handRanks.get(playerId);
            if (hand != null && hand.score() > highestScore) {
                highestScore = hand.score();
            }
        }

        for (String playerId : eligiblePlayers) {
            EvaluatedHand hand = handRanks.get(playerId);
            if (hand != null && hand.score() == highestScore) {
                winners.add(playerId);
            }
        }

        return winners;
    }

    /**
     * 分配底池给获胜者
     */
    private static void distributePot(
            Pot pot,
            List<String> winners,
            Map<String, Integer> winnings) {

        if (winners.isEmpty()) {
            return;
        }

        int potAmount = pot.amount();
        int winnerCount = winners.size();

        int share = ChipCalculator.safeDivide(potAmount, winnerCount);
        int remainder = ChipCalculator.safeSubtract(
            potAmount,
            ChipCalculator.safeMultiply(share, winnerCount)
        );

        for (String winner : winners) {
            int currentWinnings = winnings.getOrDefault(winner, 0);
            winnings.put(winner, ChipCalculator.safeAdd(currentWinnings, share));
        }

        if (remainder > 0 && !winners.isEmpty()) {
            String firstWinner = winners.get(0);
            int currentWinnings = winnings.get(firstWinner);
            winnings.put(firstWinner, ChipCalculator.safeAdd(currentWinnings, remainder));
        }
    }
}
