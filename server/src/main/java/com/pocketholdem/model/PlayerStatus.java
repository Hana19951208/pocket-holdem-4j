package com.pocketholdem.model;

/**
 * 玩家状态枚举
 *
 * 状态转换规则：
 * - SPECTATING → WAITING：玩家入座
 * - WAITING → ACTIVE：等待下一局开始
 * - ACTIVE → FOLDED：玩家弃牌
 * - ACTIVE → ALL_IN：玩家全押
 * - ACTIVE → ELIMINATED：筹码归零
 * - FOLDED/ELIMINATED/ALL_IN → SPECTATING：站起回到观战席
 */
public enum PlayerStatus {

    /**
     * 观战状态 - 未入座或已站起
     */
    SPECTATING,

    /**
     * 等待状态 - 已入座，但等待下一局开始
     */
    WAITING,

    /**
     * 活跃状态 - 正在参与当前手牌
     */
    ACTIVE,

    /**
     * 已弃牌 - 本轮已弃牌，等待下一手牌
     */
    FOLDED,

    /**
     * 全押状态 - 已押上所有筹码
     */
    ALL_IN,

    /**
     * 已淘汰 - 筹码归零，等待补充筹码或观战
     */
    ELIMINATED;

    // ==================== 状态查询方法 ====================

    /**
     * 是否可以参与当前手牌
     *
     * 只有 ACTIVE 状态可以参与游戏（正在下注、决策）
     *
     * @return true 如果可以参与当前手牌
     */
    public boolean canPlay() {
        return this == ACTIVE;
    }

    /**
     * 是否可以执行操作
     *
     * ACTIVE 状态可以执行操作（跟注、加注、弃牌等）
     *
     * @return true 如果可以执行玩家操作
     */
    public boolean canAct() {
        return this == ACTIVE;
    }

    /**
     * 是否已淘汰
     *
     * @return true 如果已无法参与游戏（ELIMINATED 或全下但不在手牌中）
     */
    public boolean isEliminated() {
        return this == ELIMINATED;
    }

    /**
     * 是否已结束当前手牌
     *
     * 包括弃牌、全押、已淘汰状态
     *
     * @return true 如果已结束当前手牌
     */
    public boolean isHandOver() {
        return this == FOLDED || this == ALL_IN || this == ELIMINATED;
    }

    /**
     * 是否需要等待下一手牌
     *
     * 弃牌、全押、淘汰、等待状态都需要等待
     *
     * @return true 如果需要等待下一手牌
     */
    public boolean isWaitingForNextHand() {
        return this == FOLDED || this == ALL_IN || this == ELIMINATED || this == WAITING;
    }

    /**
     * 获取状态的友好名称
     *
     * @return 状态的中文描述
     */
    public String getDisplayName() {
        return switch (this) {
            case SPECTATING -> "观战中";
            case WAITING -> "等待开始";
            case ACTIVE -> "游戏中";
            case FOLDED -> "已弃牌";
            case ALL_IN -> "全押";
            case ELIMINATED -> "已淘汰";
        };
    }
}
