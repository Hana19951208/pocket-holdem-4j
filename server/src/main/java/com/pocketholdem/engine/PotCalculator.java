package com.pocketholdem.engine;

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
}
