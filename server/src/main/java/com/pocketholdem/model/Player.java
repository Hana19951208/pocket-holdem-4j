package com.pocketholdem.model;

import cn.hutool.core.util.ObjectUtil;
import com.pocketholdem.dto.PlayerDTO;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * 玩家状态封装
 *
 * 设计原则：使用 Record 保证线程安全不可变对象
 * 参考原项目：Player.ts (290行)
 *
 * 核心字段：
 * - id：玩家唯一ID
 * - nickname：昵称
 * - chips：筹码数量
 * - status：玩家状态（PlayerStatus枚举）
 * - seatIndex：座位索引（null=观战）
 * - isHost：是否为房主
 * - holeCards：手牌（仅自己可见）
 * - isReady：是否准备就绪
 * - joinTime：加入时间戳（房主转移用）
 * - online：是否在线（WebSocket心跳更新）
 * - currentBet：当前本轮下注
 * - totalBetThisHand：本手牌累计投入
 * - isDealer：是否为庄家
 * - isCurrentTurn：是否轮到该玩家
 * - hasActed：本轮是否已行动
 * - isFolded：是否已弃牌
 * - isAllIn：是否全押
 */
@Slf4j
public record Player(
        String id,
        String nickname,
        int chips,
        PlayerStatus status,
        Integer seatIndex,
        boolean isHost,
        List<Card> holeCards,
        boolean isReady,
        Long joinTime,
        boolean online,
        int currentBet,
        int totalBetThisHand,
        boolean isDealer,
        boolean isCurrentTurn,
        boolean hasActed,
        boolean isFolded,
        boolean isAllIn) {

    /**
     * 紧凑构造函数：验证 + 防御性复制
     *
     * 在此处处理所有字段的验证和标准化
     */
    public Player {
        // 必填字段验证
        Objects.requireNonNull(id, "玩家ID不能为null");
        Objects.requireNonNull(nickname, "玩家昵称不能为null");
        Objects.requireNonNull(status, "玩家状态不能为null");

        // 数值约束：筹码不能为负数
        chips = Math.max(chips, 0);

        // 防御性复制：防止外部修改手牌列表
        holeCards = ObjectUtil.isNull(holeCards) ? List.of() : List.copyOf(holeCards);
    }

    // ==================== 公共构建方法 ====================

    /**
     * 使用默认值创建玩家（静态工厂方法）
     *
     * @param id       玩家ID
     * @param nickname 玩家昵称
     * @return 新的 Player 实例
     */
    public static Player create(String id, String nickname) {
        return Player.builder()
                .id(id)
                .nickname(nickname)
                .chips(1000)
                .status(PlayerStatus.WAITING)
                .joinTime(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建观战玩家
     *
     * @param id       玩家ID
     * @param nickname 玩家昵称
     * @return 新的 Player 实例
     */
    public static Player spectator(String id, String nickname) {
        return Player.builder()
                .id(id)
                .nickname(nickname)
                .status(PlayerStatus.SPECTATING)
                .build();
    }

    // ==================== Builder 模式 ====================

    /**
     * 创建 Builder 实例
     *
     * @return Builder 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 玩家构建器
     *
     * 提供流式 API 构建 Player 对象
     */
    public static class Builder {
        private String id;
        private String nickname;
        private int chips = 1000;
        private PlayerStatus status = PlayerStatus.WAITING;
        private Integer seatIndex = null;
        private boolean isHost = false;
        private List<Card> holeCards = List.of();
        private boolean isReady = false;
        private Long joinTime = null;
        private boolean online = false;
        private int currentBet = 0;
        private int totalBetThisHand = 0;
        private boolean isDealer = false;
        private boolean isCurrentTurn = false;
        private boolean hasActed = false;
        private boolean isFolded = false;
        private boolean isAllIn = false;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder nickname(String nickname) {
            this.nickname = nickname;
            return this;
        }

        public Builder chips(int chips) {
            this.chips = Math.max(chips, 0);
            return this;
        }

        public Builder status(PlayerStatus status) {
            this.status = status;
            return this;
        }

        public Builder seatIndex(Integer seatIndex) {
            this.seatIndex = seatIndex;
            return this;
        }

        public Builder isHost(boolean isHost) {
            this.isHost = isHost;
            return this;
        }

        public Builder holeCards(List<Card> holeCards) {
            this.holeCards = ObjectUtil.isNull(holeCards) ? List.of() : List.copyOf(holeCards);
            return this;
        }

        public Builder isReady(boolean isReady) {
            this.isReady = isReady;
            return this;
        }

        public Builder joinTime(Long joinTime) {
            this.joinTime = ObjectUtil.isNull(joinTime) ? System.currentTimeMillis() : joinTime;
            return this;
        }

        public Builder online(boolean online) {
            this.online = online;
            return this;
        }

        public Builder currentBet(int currentBet) {
            this.currentBet = Math.max(currentBet, 0);
            return this;
        }

        public Builder totalBetThisHand(int totalBetThisHand) {
            this.totalBetThisHand = Math.max(totalBetThisHand, 0);
            return this;
        }

        public Builder isDealer(boolean isDealer) {
            this.isDealer = isDealer;
            return this;
        }

        public Builder isCurrentTurn(boolean isCurrentTurn) {
            this.isCurrentTurn = isCurrentTurn;
            return this;
        }

        public Builder hasActed(boolean hasActed) {
            this.hasActed = hasActed;
            return this;
        }

        public Builder isFolded(boolean isFolded) {
            this.isFolded = isFolded;
            return this;
        }

        public Builder isAllIn(boolean isAllIn) {
            this.isAllIn = isAllIn;
            return this;
        }

        public Player build() {
            return new Player(
                    Objects.requireNonNull(id, "玩家ID不能为null"),
                    Objects.requireNonNull(nickname, "玩家昵称不能为null"),
                    chips,
                    Objects.requireNonNull(status, "玩家状态不能为null"),
                    seatIndex,
                    isHost,
                    holeCards,
                    isReady,
                    joinTime,
                    online,
                    currentBet,
                    totalBetThisHand,
                    isDealer,
                    isCurrentTurn,
                    hasActed,
                    isFolded,
                    isAllIn);
        }
    }

    // ==================== With 方法 - 使用构建器减少冗余 ====================

    /**
     * 更新单个字段的通用方法（减少17参数重复）
     *
     * @param mutator 字段更新器
     * @return 新的 Player 实例
     */
    private Player update(Function<Builder, Builder> mutator) {
        Builder builder = new Builder()
                .id(this.id)
                .nickname(this.nickname)
                .chips(this.chips)
                .status(this.status)
                .seatIndex(this.seatIndex)
                .isHost(this.isHost)
                .holeCards(this.holeCards)
                .isReady(this.isReady)
                .joinTime(this.joinTime)
                .online(this.online)
                .currentBet(this.currentBet)
                .totalBetThisHand(this.totalBetThisHand)
                .isDealer(this.isDealer)
                .isCurrentTurn(this.isCurrentTurn)
                .hasActed(this.hasActed)
                .isFolded(this.isFolded)
                .isAllIn(this.isAllIn);

        mutator.apply(builder);
        return builder.build();
    }

    public Player withChips(int newChips) {
        return update(b -> b.chips(Math.max(newChips, 0)));
    }

    public Player withStatus(PlayerStatus newStatus) {
        return update(b -> b.status(newStatus));
    }

    public Player withSeatIndex(Integer newSeatIndex) {
        return update(b -> b.seatIndex(newSeatIndex));
    }

    public Player withIsHost(boolean newIsHost) {
        return update(b -> b.isHost(newIsHost));
    }

    public Player withHoleCards(List<Card> newHoleCards) {
        List<Card> copiedCards = ObjectUtil.isNull(newHoleCards) ? List.of() : List.copyOf(newHoleCards);
        return update(b -> b.holeCards(copiedCards));
    }

    public Player withIsReady(boolean newIsReady) {
        return update(b -> b.isReady(newIsReady));
    }

    public Player withOnline(boolean newOnline) {
        return update(b -> b.online(newOnline));
    }

    public Player withCurrentBet(int newCurrentBet) {
        return update(b -> b.currentBet(Math.max(newCurrentBet, 0)));
    }

    public Player withTotalBetThisHand(int newTotalBetThisHand) {
        return update(b -> b.totalBetThisHand(Math.max(newTotalBetThisHand, 0)));
    }

    public Player withIsDealer(boolean newIsDealer) {
        return update(b -> b.isDealer(newIsDealer));
    }

    public Player withIsCurrentTurn(boolean newIsCurrentTurn) {
        return update(b -> b.isCurrentTurn(newIsCurrentTurn));
    }

    public Player withHasActed(boolean newHasActed) {
        return update(b -> b.hasActed(newHasActed));
    }

    public Player withIsFolded(boolean newIsFolded) {
        return update(b -> b.isFolded(newIsFolded));
    }

    public Player withIsAllIn(boolean newIsAllIn) {
        return update(b -> b.isAllIn(newIsAllIn));
    }

    // ==================== 筹码操作 ====================

    public Player addChips(int amount) {
        if (amount <= 0) {
            log.error("玩家 {} 筹码增加失败：增加金额 {} 必须大于0", nickname, amount);
            throw new IllegalArgumentException("筹码增加金额必须大于0: " + amount);
        }
        int newChips = chips + amount;
        log.info("玩家 {} 赢得筹码：{} → {}", nickname, chips, newChips);
        return withChips(newChips);
    }

    public Player deductChips(int amount) {
        if (amount <= 0) {
            log.error("玩家 {} 筹码扣除失败：扣除金额 {} 必须大于0", nickname, amount);
            throw new IllegalArgumentException("筹码扣除金额必须大于0: " + amount);
        }

        int actualAmount = Math.min(amount, this.chips);
        int newChips = this.chips - actualAmount;
        int newCurrentBet = this.currentBet + actualAmount;
        int newTotalBetThisHand = this.totalBetThisHand + actualAmount;

        if (newChips == 0 && this.status == PlayerStatus.ACTIVE) {
            log.info("玩家 {} 全押：下注 {}, 剩余筹码 {}", nickname, actualAmount, newChips);
            return update(b -> b
                    .chips(0)
                    .status(PlayerStatus.ALL_IN)
                    .currentBet(newCurrentBet)
                    .totalBetThisHand(newTotalBetThisHand)
                    .isAllIn(true));
        }

        log.debug("玩家 {} 下注：{}, 剩余筹码 {}", nickname, actualAmount, newChips);
        return update(b -> b
                .chips(newChips)
                .currentBet(newCurrentBet)
                .totalBetThisHand(newTotalBetThisHand));
    }

    // ==================== 状态操作 ====================

    public Player sitDown(int newSeatIndex) {
        if (this.seatIndex != null) {
            log.warn("玩家 {} 已在座位上，无法重复入座", nickname);
            throw new IllegalStateException("玩家已在座位上: " + nickname);
        }

        log.info("玩家 {} 入座，座位索引: {}", nickname, newSeatIndex);
        return update(b -> b
                .status(PlayerStatus.WAITING)
                .seatIndex(newSeatIndex)
                .isReady(false)
                .currentBet(0)
                .totalBetThisHand(0));
    }

    public Player standUp() {
        if (this.seatIndex == null) {
            log.warn("玩家 {} 未在座位上，无法站起", nickname);
            throw new IllegalStateException("玩家未在座位上: " + nickname);
        }

        log.info("玩家 {} 站起，回到观战席", nickname);
        return update(b -> b
                .status(PlayerStatus.SPECTATING)
                .seatIndex(null)
                .holeCards(List.of())
                .isReady(false)
                .currentBet(0)
                .totalBetThisHand(0)
                .isCurrentTurn(false));
    }

    public Player fold() {
        if (!canPlay()) {
            log.warn("玩家 {} 无法弃牌：当前状态为 {}", nickname, status);
            throw new IllegalStateException("只有活跃玩家可以弃牌: " + nickname + ", 当前状态: " + status);
        }

        log.info("玩家 {} 弃牌", nickname);
        return update(b -> b
                .status(PlayerStatus.FOLDED)
                .hasActed(true)
                .isFolded(true));
    }

    public Player allIn() {
        if (this.chips <= 0) {
            log.warn("玩家 {} 没有筹码，无法全押", nickname);
            throw new IllegalStateException("没有筹码无法全押: " + nickname);
        }

        int allInAmount = this.chips;
        log.info("玩家 {} 全押：下注 {}", nickname, allInAmount);

        return update(b -> b
                .chips(0)
                .status(PlayerStatus.ALL_IN)
                .currentBet(currentBet + allInAmount)
                .totalBetThisHand(totalBetThisHand + allInAmount)
                .isCurrentTurn(false)
                .hasActed(true)
                .isAllIn(true));
    }

    public Player resetForNewHand() {
        log.debug("玩家 {} 重置为新一手牌", nickname);
        return update(b -> b
                .status(PlayerStatus.WAITING)
                .holeCards(List.of())
                .isReady(false)
                .currentBet(0)
                .totalBetThisHand(0)
                .isDealer(false)
                .isCurrentTurn(false)
                .hasActed(false)
                .isFolded(false)
                .isAllIn(false));
    }

    public Player resetForNewRound() {
        log.debug("玩家 {} 重置为新一轮", nickname);
        return update(b -> b
                .currentBet(0)
                .hasActed(false)
                .isCurrentTurn(false));
    }

    public Player clearHoleCards() {
        return withHoleCards(List.of());
    }

    // ==================== 状态查询 ====================

    public boolean canPlay() {
        return this.seatIndex != null
                && this.chips > 0
                && this.status != PlayerStatus.ELIMINATED
                && this.status != PlayerStatus.SPECTATING
                && this.status != PlayerStatus.WAITING;
    }

    public boolean isInHand() {
        return this.canPlay() && !this.isFolded;
    }

    public boolean canAct() {
        return this.isInHand()
                && !this.isAllIn
                && this.isCurrentTurn;
    }

    // ==================== 数据导出 ====================

    public PlayerDTO toPublicDTO() {
        return new PlayerDTO(
                this.id, this.nickname, this.chips, this.status,
                this.seatIndex, this.isHost, this.isReady, this.online,
                this.currentBet, this.isDealer, this.isCurrentTurn,
                this.hasActed, this.isFolded, this.isAllIn, null);
    }

    public PlayerDTO toPrivateDTO() {
        return new PlayerDTO(
                this.id, this.nickname, this.chips, this.status,
                this.seatIndex, this.isHost, this.isReady, this.online,
                this.currentBet, this.isDealer, this.isCurrentTurn,
                this.hasActed, this.isFolded, this.isAllIn, this.holeCards);
    }

    // ==================== 在线状态管理 ====================

    public Player updateOnline() {
        if (!this.online) {
            log.info("玩家 {} 上线", nickname);
        }
        return withOnline(true);
    }

    public Player markDisconnected() {
        if (this.online) {
            log.info("玩家 {} 离线", nickname);
        }
        return withOnline(false);
    }

    public boolean isDisconnected() {
        return !this.online;
    }
}
