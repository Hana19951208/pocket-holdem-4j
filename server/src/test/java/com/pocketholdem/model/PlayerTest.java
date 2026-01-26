package com.pocketholdem.model;

import com.pocketholdem.dto.PlayerDTO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Player类测试
 * 
 * 测试覆盖:
 * - 玩家创建
 * - 筹码增减
 * - 负筹码验证
 * - 状态转换
 * - 座位分配
 * - 手牌防御性拷贝
 * - DTO导出
 */
class PlayerTest {

    @Test
    void testPlayerCreation() {
        // Given
        Player player = Player.builder()
                .id("player1")
                .nickname("TestPlayer")
                .chips(1000)
                .build();

        // Then
        assertEquals("player1", player.id());
        assertEquals("TestPlayer", player.nickname());
        assertEquals(1000, player.chips());
        assertEquals(PlayerStatus.WAITING, player.status());
        assertNull(player.seatIndex());
        assertFalse(player.isHost());
        assertEquals(0, player.holeCards().size());
    }

    @Test
    void testAddDeductChips() {
        // Given
        Player player = Player.builder()
                .id("player1")
                .nickname("TestPlayer")
                .chips(1000)
                .build();

        // When
        player = player.addChips(500);
        player = player.addChips(200);

        // Then
        assertEquals(1700, player.chips());
    }

    @Test
    void testDeductChips() {
        // Given
        Player player = Player.builder()
                .id("player1")
                .nickname("TestPlayer")
                .chips(1000)
                .status(PlayerStatus.ACTIVE)
                .build();

        // When
        player = player.deductChips(200);
        player = player.deductChips(500);

        // Then
        assertEquals(300, player.chips());
        assertFalse(player.isAllIn());
        assertEquals(PlayerStatus.ACTIVE, player.status());
    }

    @Test
    void testDeductChipsAllIn() {
        // Given
        Player player = Player.builder()
                .id("player1")
                .nickname("TestPlayer")
                .chips(100)
                .status(PlayerStatus.ACTIVE)
                .build();

        // When - 扣除超过筹码的金额
        player = player.deductChips(150);

        // Then - 最多只扣除所有筹码
        assertEquals(0, player.chips());
        assertEquals(PlayerStatus.ALL_IN, player.status());
        assertTrue(player.isAllIn());
        assertEquals(100, player.totalBetThisHand()); // 只扣了100
    }

    @Test
    void testNegativeChipsNotAllowed() {
        // Given
        Player player = Player.builder()
                .id("player1")
                .nickname("TestPlayer")
                .chips(1000)
                .build();

        // Then & When
        assertThrows(IllegalArgumentException.class, () -> {
            player.addChips(-100);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            player.deductChips(-100);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            player.addChips(0);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            player.deductChips(0);
        });
    }

    @Test
    void testStatusTransitions() {
        // Given
        Player player = Player.builder()
                .id("player1")
                .nickname("TestPlayer")
                .chips(1000)
                .status(PlayerStatus.SPECTATING)
                .build();

        // When: 观战 -> 入座 -> 等待
        player = player.sitDown(0);
        assertEquals(PlayerStatus.WAITING, player.status());
        assertEquals(Integer.valueOf(0), player.seatIndex());

        // 弃牌
        player = player.fold();
        assertEquals(PlayerStatus.FOLDED, player.status());

        // Folded状态已经有座位,不能再次sitDown
        Player foldedPlayer = player;
        assertThrows(IllegalStateException.class, () -> {
            foldedPlayer.sitDown(1);
        });
    }

    @Test
    void testSeatAssignment() {
        // Given
        Player player = Player.builder()
                .id("player1")
                .nickname("TestPlayer")
                .chips(1000)
                .status(PlayerStatus.SPECTATING)
                .build();

        // When
        player = player.sitDown(3);

        // Then: 已经入座时不能再次入座
        assertEquals(Integer.valueOf(3), player.seatIndex());
        Player seatedPlayer = player;
        assertThrows(IllegalStateException.class, () -> {
            seatedPlayer.sitDown(4);
        });
    }

    @Test
    void testStandUp() {
        // Given
        Player player = Player.builder()
                .id("player1")
                .nickname("TestPlayer")
                .chips(1000)
                .status(PlayerStatus.WAITING)
                .seatIndex(0)
                .build();

        // When
        player = player.standUp();

        // Then
        assertNull(player.seatIndex());
        assertEquals(PlayerStatus.SPECTATING, player.status());
        assertEquals(0, player.currentBet());
        assertEquals(0, player.totalBetThisHand());
        assertTrue(player.holeCards().isEmpty());
    }

    @Test
    void testPublicDTO() {
        // Given
        Player player = Player.builder()
                .id("player1")
                .nickname("TestPlayer")
                .chips(1000)
                .status(PlayerStatus.ACTIVE)
                .holeCards(java.util.List.of(
                        new Card(Suit.HEARTS, Rank.ACE),
                        new Card(Suit.SPADES, Rank.TWO)))
                .seatIndex(0)
                .isReady(true)
                .online(true)
                .build();

        // When
        PlayerDTO dto = player.toPublicDTO();

        // Then
        assertEquals("player1", dto.id());
        assertEquals("TestPlayer", dto.nickname());
        assertEquals(1000, dto.chips());
        assertEquals(PlayerStatus.ACTIVE, dto.status());
        assertEquals(Integer.valueOf(0), dto.seatIndex());
        assertNull(dto.holeCards()); // 公开DTO不包含手牌
    }

    @Test
    void testPrivateDTO() {
        // Given
        Player player = Player.builder()
                .id("player1")
                .nickname("TestPlayer")
                .chips(1000)
                .status(PlayerStatus.ACTIVE)
                .holeCards(java.util.List.of(
                        new Card(Suit.HEARTS, Rank.ACE),
                        new Card(Suit.SPADES, Rank.TWO)))
                .seatIndex(0)
                .isReady(true)
                .online(true)
                .build();

        // When
        PlayerDTO dto = player.toPrivateDTO();

        // Then
        assertEquals("player1", dto.id());
        assertEquals("TestPlayer", dto.nickname());
        assertEquals(1000, dto.chips());
        assertEquals(PlayerStatus.ACTIVE, dto.status());
        assertEquals(Integer.valueOf(0), dto.seatIndex());
        assertNotNull(dto.holeCards()); // 私有DTO包含手牌
        assertEquals(2, dto.holeCards().size());
    }

    @Test
    void testCanPlay() {
        // Given - 正常情况可以玩
        Player player = Player.builder()
                .id("player1")
                .nickname("TestPlayer")
                .chips(1000)
                .status(PlayerStatus.ACTIVE)
                .seatIndex(0)
                .build();

        // Then
        assertTrue(player.canPlay());

        // When: 状态为WAITING不能玩
        Player waitingPlayer = player.withStatus(PlayerStatus.WAITING);
        assertFalse(waitingPlayer.canPlay());

        // When: 筹码归零不能玩
        Player noChipsPlayer = player.withChips(0);
        assertFalse(noChipsPlayer.canPlay());
    }

    @Test
    void testCanAct() {
        // Given
        Player player = Player.builder()
                .id("player1")
                .nickname("TestPlayer")
                .chips(1000)
                .status(PlayerStatus.ACTIVE)
                .seatIndex(0)
                .currentBet(50)
                .isCurrentTurn(true)
                .build();

        // Then
        assertTrue(player.canAct());

        // When: 不是当前回合
        Player notTurnPlayer = player.withIsCurrentTurn(false);
        assertFalse(notTurnPlayer.canAct());
    }

    @Test
    void testIsInHand() {
        // Given
        Player player = Player.builder()
                .id("player1")
                .nickname("TestPlayer")
                .chips(1000)
                .status(PlayerStatus.ACTIVE)
                .seatIndex(0)
                .build();

        // Then
        assertTrue(player.isInHand());

        // When: 弃牌
        player = player.fold();
        assertFalse(player.isInHand());
    }

    @Test
    void testAllIn() {
        // Given
        Player player = Player.builder()
                .id("player1")
                .nickname("TestPlayer")
                .chips(500)
                .status(PlayerStatus.ACTIVE)
                .seatIndex(0)
                .build();

        // When
        player = player.allIn();

        // Then
        assertEquals(0, player.chips());
        assertEquals(PlayerStatus.ALL_IN, player.status());
        assertTrue(player.isAllIn());
        assertEquals(500, player.totalBetThisHand());
    }

    @Test
    void testResetForNewHand() {
        // Given
        Player player = Player.builder()
                .id("player1")
                .nickname("TestPlayer")
                .chips(1000)
                .status(PlayerStatus.ACTIVE)
                .seatIndex(0)
                .currentBet(100)
                .totalBetThisHand(500)
                .isDealer(true)
                .isCurrentTurn(true)
                .hasActed(true)
                .isFolded(true)
                .isAllIn(false)
                .holeCards(java.util.List.of(
                        new Card(Suit.HEARTS, Rank.ACE),
                        new Card(Suit.SPADES, Rank.TWO)))
                .build();

        // When
        player = player.resetForNewHand();

        // Then
        assertEquals(1000, player.chips());
        assertEquals(PlayerStatus.WAITING, player.status());
        assertEquals(Integer.valueOf(0), player.seatIndex());
        assertEquals(0, player.currentBet());
        assertEquals(0, player.totalBetThisHand());
        assertFalse(player.isDealer());
        assertFalse(player.isCurrentTurn());
        assertFalse(player.hasActed());
        assertFalse(player.isFolded());
        assertFalse(player.isAllIn());
        assertTrue(player.holeCards().isEmpty());
    }

    @Test
    void testResetForNewRound() {
        // Given
        Player player = Player.builder()
                .id("player1")
                .nickname("TestPlayer")
                .chips(1000)
                .status(PlayerStatus.ACTIVE)
                .currentBet(100)
                .hasActed(true)
                .isFolded(true)
                .build();

        // When
        player = player.resetForNewRound();

        // Then
        assertEquals(0, player.currentBet());
        assertFalse(player.hasActed());
        // isFolded 和 isAllIn 不应该重置
        assertTrue(player.isFolded());
        assertFalse(player.isAllIn());
    }

    @Test
    void testClearHoleCards() {
        // Given
        Player player = Player.builder()
                .id("player1")
                .nickname("TestPlayer")
                .chips(1000)
                .status(PlayerStatus.ACTIVE)
                .holeCards(java.util.List.of(
                        new Card(Suit.HEARTS, Rank.ACE),
                        new Card(Suit.SPADES, Rank.TWO)))
                .build();

        // When
        player = player.clearHoleCards();

        // Then
        assertTrue(player.holeCards().isEmpty());
    }

    @Test
    void testOnlineStatus() {
        // Given
        Player player = Player.builder()
                .id("player1")
                .nickname("TestPlayer")
                .chips(1000)
                .online(false)
                .build();

        // When
        player = player.updateOnline();

        // Then
        assertTrue(player.online());
        assertFalse(player.isDisconnected());
    }

    @Test
    void testDisconnectedStatus() {
        // Given
        Player player = Player.builder()
                .id("player1")
                .nickname("TestPlayer")
                .chips(1000)
                .online(true)
                .build();

        // When
        player = player.markDisconnected();

        // Then
        assertFalse(player.online());
        assertTrue(player.isDisconnected());
    }
}
