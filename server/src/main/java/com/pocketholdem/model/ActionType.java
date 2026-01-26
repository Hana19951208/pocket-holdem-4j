package com.pocketholdem.model;

/**
 * 玩家操作类型枚举
 *
 * 用于表示玩家在游戏中的行动
 */
public enum ActionType {

    /**
     * 弃牌 - 放弃本手牌，不再参与当前手牌
     */
    FOLD,

    /**
     * 过牌 - 当前无需跟注，选择跳过行动
     * 注意：只有当前下注额为0时才能过牌
     */
    CHECK,

    /**
     * 跟注 - 跟进当前下注额，保持与最高下注一致
     */
    CALL,

    /**
     * 加注 - 提高下注额，增加竞争
     */
    RAISE,

    /**
     * 全押 - 押上所有筹码
     */
    ALL_IN;

    // ==================== 操作查询方法 ====================

    /**
     * 是否需要下注
     *
     * FOLD 不需要下注，其他操作都需要
     *
     * @return true 如果需要下注
     */
    public boolean requiresBet() {
        return this != FOLD && this != CHECK;
    }

    /**
     * 是否为激进操作
     *
     * RAISE 和 ALL_IN 是激进操作，会增加底池
     *
     * @return true 如果是激进操作
     */
    public boolean isAggressive() {
        return this == RAISE || this == ALL_IN;
    }

    /**
     * 是否为被动操作
     *
     * CHECK 和 CALL 是被动操作
     *
     * @return true 如果是被动操作
     */
    public boolean isPassive() {
        return this == CHECK || this == CALL;
    }

    /**
     * 是否结束行动
     *
     * FOLD 结束当前手牌，ALL_IN 可能触发边池计算
     *
     * @return true 如果结束行动
     */
    public boolean endsTurn() {
        return this == FOLD || this == ALL_IN;
    }

    /**
     * 是否可以执行过牌
     *
     * 当前下注额为0时可以过牌
     *
     * @param currentBet 当前最高下注
     * @return true 如果可以过牌
     */
    public boolean canCheck(int currentBet) {
        return this == CHECK && currentBet == 0;
    }

    /**
     * 是否可以执行跟注
     *
     * 当前有下注时可以跟注
     *
     * @param currentBet 当前最高下注
     * @return true 如果可以跟注
     */
    public boolean canCall(int currentBet) {
        return this == CALL && currentBet > 0;
    }

    /**
     * 是否需要投入额外筹码
     *
     * FOLD 不需要，CHECK 在无下注时不需要，其他需要
     *
     * @param currentBet 当前最高下注
     * @param playerBet 玩家当前下注
     * @return true 如果需要投入筹码
     */
    public boolean requiresChips(int currentBet, int playerBet) {
        if (this == FOLD) {
            return false;
        }
        if (this == CHECK) {
            return currentBet > playerBet;
        }
        return true;
    }

    /**
     * 获取操作的最小下注额
     *
     * @param currentBet 当前最高下注
     * @param playerBet 玩家当前下注
     * @param minRaise 最小加注额
     * @return 最小下注额
     */
    public int getMinBetAmount(int currentBet, int playerBet, int minRaise) {
        return switch (this) {
            case CALL -> currentBet - playerBet;
            case RAISE -> currentBet - playerBet + minRaise;
            case ALL_IN -> Integer.MAX_VALUE; // 全押下所有
            default -> 0;
        };
    }

    /**
     * 获取操作的友好名称
     *
     * @return 操作的中文描述
     */
    public String getDisplayName() {
        return switch (this) {
            case FOLD -> "弃牌";
            case CHECK -> "过牌";
            case CALL -> "跟注";
            case RAISE -> "加注";
            case ALL_IN -> "全押";
        };
    }

    /**
     * 获取操作的简短描述
     *
     * @return 简短描述
     */
    public String getShortName() {
        return switch (this) {
            case FOLD -> "弃";
            case CHECK -> "过";
            case CALL -> "跟";
            case RAISE -> "加";
            case ALL_IN -> "全";
        };
    }

    /**
     * 是否为有效操作
     *
     * @param currentBet 当前最高下注
     * @param playerBet 玩家当前下注
     * @return true 如果是有效操作
     */
    public boolean isValid(int currentBet, int playerBet) {
        if (this == FOLD) {
            return true; // 任何时候都可以弃牌
        }
        if (this == CHECK) {
            return currentBet == playerBet; // 只能在下注为0时过牌
        }
        if (this == CALL) {
            return currentBet > playerBet; // 只能在下注大于玩家下注时跟注
        }
        return true; // RAISE 和 ALL_IN 总是有效
    }

    /**
     * 验证操作是否有效
     *
     * @param currentBet 当前最高下注
     * @param playerBet 玩家当前下注
     * @throws IllegalArgumentException 如果操作无效
     */
    public void validate(int currentBet, int playerBet) {
        if (!isValid(currentBet, playerBet)) {
            String message = switch (this) {
                case CHECK -> String.format("过牌无效：当前下注额为 %d，需要先跟注", currentBet);
                case CALL -> String.format("跟注无效：当前下注额为 %d，玩家已下注 %d", currentBet, playerBet);
                default -> "无效操作";
            };
            throw new IllegalArgumentException(message);
        }
    }
}
