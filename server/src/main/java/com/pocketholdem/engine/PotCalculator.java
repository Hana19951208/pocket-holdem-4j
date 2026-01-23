package com.pocketholdem.engine;

import com.pocketholdem.model.EvaluatedHand;
import com.pocketholdem.model.Pot;
import com.pocketholdem.util.ChipCalculator;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 底池计算器（纯函数工具类）
 * 负责计算边池和底池分配逻辑。
 * 遵循 numeric-spec.md 第6章规范。
 */
@Slf4j
public final class PotCalculator {

    private PotCalculator() {
        // 工具类，禁止实例化
    }

    /**
     * 计算边池
     * 按照玩家投入筹码从小到大排序，逐层切割创建主池和边池。
     *
     * @param playerIds 玩家ID数组（与bets数组一一对应）
     * @param bets 每个玩家投入的筹码
     * @return 底池列表（按产生顺序排列）
     * @throws IllegalArgumentException 如果输入参数无效
     */
    public static Pot[] calculateSidePots(String[] playerIds, int[] bets) {
        if (playerIds == null || bets == null || playerIds.length != bets.length) {
            log.error("边池计算参数错误: playerIds={}, bets={}", 
                playerIds == null ? "null" : playerIds.length, 
                bets == null ? "null" : bets.length);
            throw new IllegalArgumentException("玩家ID和下注数组长度必须一致且不为空");
        }

        if (playerIds.length == 0) {
            return new Pot[0];
        }

        if (log.isDebugEnabled()) {
            log.debug("开始计算边池，参与玩家数: {}", playerIds.length);
        }

        // 创建玩家投入映射
        Map<String, Integer> playerBets = new HashMap<>();
        for (int i = 0; i < playerIds.length; i++) {
            playerBets.put(playerIds[i], bets[i]);
        }

        // 按投入排序（从小到大）
        String[] sortedPlayers = Arrays.copyOf(playerIds, playerIds.length);
        Arrays.sort(sortedPlayers,
            (a, b) -> Integer.compare(playerBets.get(a), playerBets.get(b))
        );

        List<Pot> pots = new ArrayList<>();
        int prevBet = 0;

        // 逐层切割底池
        for (int i = 0; i < sortedPlayers.length; i++) {
            String currentPlayer = sortedPlayers[i];
            int currentBet = playerBets.get(currentPlayer);
            int betDiff = ChipCalculator.safeSubtract(currentBet, prevBet);

            if (betDiff > 0) {
                // 计算参与此层底池的玩家数
                int contributorsCount = sortedPlayers.length - i;

                // 计算本层底池金额 = 差额 * 参与人数
                int potAmount = ChipCalculator.safeMultiply(betDiff, contributorsCount);

                // 确定有资格的玩家（当前及之后的玩家，即投入 >= currentBet 的玩家）
                String[] eligiblePlayers = Arrays.copyOfRange(
                    sortedPlayers, i, sortedPlayers.length
                );

                pots.add(new Pot(potAmount, eligiblePlayers));
                
                if (log.isTraceEnabled()) {
                    log.trace("创建底池: 金额={}, 参与人数={}, 资格玩家={}", 
                        potAmount, contributorsCount, Arrays.toString(eligiblePlayers));
                }

                prevBet = currentBet;
            }
        }

        return pots.toArray(new Pot[0]);
    }

    /**
     * 分配底池给获胜玩家
     * 处理平分底池和余数分配（按座位顺序）。
     *
     * @param pots 底池列表
     * @param handRanks 玩家ID到牌型的映射
     * @return 玩家ID到赢得筹码的映射
     */
    public static Map<String, Integer> awardPots(
            Pot[] pots,
            Map<String, EvaluatedHand> handRanks) {

        Map<String, Integer> winnings = new HashMap<>();

        // 初始化所有玩家为0
        for (String playerId : handRanks.keySet()) {
            winnings.put(playerId, 0);
        }

        log.debug("开始分配底池，底池数量: {}", pots.length);

        // 逐个底池分配
        for (int i = 0; i < pots.length; i++) {
            Pot pot = pots[i];
            List<String> winners = findWinners(pot, handRanks);
            
            if (log.isDebugEnabled()) {
                log.debug("分配第 {} 个底池 (金额={}): 获胜者={}", i + 1, pot.amount(), winners);
            }
            
            distributePot(pot, winners, winnings);
        }

        return winnings;
    }

    /**
     * 找出某个底池的最高分玩家
     * 仅在有资格的玩家中比较。
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

        // 找出最高分
        for (String playerId : eligiblePlayers) {
            EvaluatedHand hand = handRanks.get(playerId);
            if (hand != null && hand.score() > highestScore) {
                highestScore = hand.score();
            }
        }

        // 找出所有达到最高分的玩家
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
     * 执行实际的筹码计算，包含除法和余数处理。
     */
    private static void distributePot(
            Pot pot,
            List<String> winners,
            Map<String, Integer> winnings) {

        if (winners.isEmpty()) {
            log.warn("底池分配异常：无获胜者，金额={}将被丢弃", pot.amount());
            return;
        }

        int potAmount = pot.amount();
        int winnerCount = winners.size();

        // 计算平均分配
        int share = ChipCalculator.safeDivide(potAmount, winnerCount);
        // 计算余数
        int remainder = ChipCalculator.safeSubtract(
            potAmount,
            ChipCalculator.safeMultiply(share, winnerCount)
        );

        // 分配平均份额
        for (String winner : winners) {
            int currentWinnings = winnings.getOrDefault(winner, 0);
            winnings.put(winner, ChipCalculator.safeAdd(currentWinnings, share));
        }

        // 余数按座位顺序分配（这里简化为按列表顺序，即第一个玩家多拿余数）
        // 实际上列表顺序通常就是座位顺序或入局顺序
        if (remainder > 0) {
            String firstWinner = winners.get(0);
            int currentWinnings = winnings.get(firstWinner);
            winnings.put(firstWinner, ChipCalculator.safeAdd(currentWinnings, remainder));
            
            if (log.isDebugEnabled()) {
                log.debug("分配余数: {} 给玩家 {}", remainder, firstWinner);
            }
        }
    }
}