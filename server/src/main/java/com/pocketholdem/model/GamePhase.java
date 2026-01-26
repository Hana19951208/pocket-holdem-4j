package com.pocketholdem.model;

/**
 * 游戏阶段枚举
 *
 * 阶段推进顺序：PRE_FLOP → FLOP → TURN → RIVER → SHOWDOWN
 */
public enum GamePhase {

    /**
     * 翻牌前 - 每人发2张底牌，开始盲注下注
     */
    PRE_FLOP,

    /**
     * 翻牌 - 发3张公共牌，进行第一轮下注
     */
    FLOP,

    /**
     * 转牌 - 发第4张公共牌，进行第二轮下注
     */
    TURN,

    /**
     * 河牌 - 发第5张公共牌，进行第三轮下注
     */
    RIVER,

    /**
     * 摊牌 - 比较手牌，分配底池给获胜者
     */
    SHOWDOWN;

    // ==================== 阶段查询方法 ====================

    /**
     * 是否为下注阶段
     *
     * SHOWDOWN 不是下注阶段
     *
     * @return true 如果是下注阶段
     */
    public boolean isBettingPhase() {
        return this != SHOWDOWN;
    }

    /**
     * 是否为公共牌阶段
     *
     * FLOP、TURN、RIVER 有公共牌
     *
     * @return true 如果是公共牌阶段
     */
    public boolean hasCommunityCards() {
        return this == FLOP || this == TURN || this == RIVER;
    }

    /**
     * 获取公共牌数量
     *
     * @return 公共牌数量（0, 3, 4, 或 5）
     */
    public int getCommunityCardCount() {
        return switch (this) {
            case PRE_FLOP -> 0;
            case FLOP -> 3;
            case TURN -> 4;
            case RIVER -> 5;
            case SHOWDOWN -> 5;
        };
    }

    /**
     * 是否可以执行加注
     *
     * 摊牌阶段不能加注
     *
     * @return true 如果可以加注
     */
    public boolean canRaise() {
        return this != SHOWDOWN;
    }

    /**
     * 是否为最后一轮下注
     *
     * @return true 如果是 RIVER 或 SHOWDOWN
     */
    public boolean isLastBettingRound() {
        return this == RIVER || this == SHOWDOWN;
    }

    /**
     * 推进到下一阶段
     *
     * @return 下一个游戏阶段
     * @throws IllegalStateException 如果已是 SHOWDOWN
     */
    public GamePhase nextPhase() {
        return switch (this) {
            case PRE_FLOP -> FLOP;
            case FLOP -> TURN;
            case TURN -> RIVER;
            case RIVER -> SHOWDOWN;
            case SHOWDOWN -> throw new IllegalStateException("SHOWDOWN 是最终阶段，无法继续推进");
        };
    }

    /**
     * 是否需要发公共牌
     *
     * @return true 如果需要发公共牌
     */
    public boolean needsCommunityCards() {
        return hasCommunityCards() && this != SHOWDOWN;
    }

    /**
     * 获取阶段的友好名称
     *
     * @return 阶段的中文描述
     */
    public String getDisplayName() {
        return switch (this) {
            case PRE_FLOP -> "翻牌前";
            case FLOP -> "翻牌";
            case TURN -> "转牌";
            case RIVER -> "河牌";
            case SHOWDOWN -> "摊牌";
        };
    }

    /**
     * 获取阶段的简短描述
     *
     * @return 简短描述
     */
    public String getShortDescription() {
        return switch (this) {
            case PRE_FLOP -> "盲注";
            case FLOP -> "翻牌圈";
            case TURN -> "转牌圈";
            case RIVER -> "河牌圈";
            case SHOWDOWN -> "摊牌";
        };
    }
}
