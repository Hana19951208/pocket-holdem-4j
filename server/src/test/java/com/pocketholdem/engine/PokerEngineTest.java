package com.pocketholdem.engine;

import com.pocketholdem.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;

@DisplayName("扑克引擎框架测试")
class PokerEngineTest {

    @Test
    @DisplayName("引擎应该评估高牌")
    void shouldEvaluateHighCard() {
        Card[] hand = {
                Card.of("spades", "ace"),
                Card.of("hearts", "king"),
                Card.of("diamonds", "queen"),
                Card.of("clubs", "jack"),
                Card.of("spades", "nine")
        };

        EvaluatedHand result = PokerEngine.evaluateFiveCards(hand);

        assertEquals(HandRank.HIGH_CARD, result.rank());
        assertEquals(5, result.bestCards().length);
    }

    @Test
    @DisplayName("应该识别常规顺子（3-4-5-6-7）")
    void shouldRecognizeNormalStraight() {
        // 构造一副 3-4-5-6-7 的杂色顺子
        Card[] hand = {
                Card.of("spades", "seven"),
                Card.of("hearts", "six"),
                Card.of("diamonds", "five"),
                Card.of("clubs", "four"),
                Card.of("spades", "three")
        };

        EvaluatedHand result = PokerEngine.evaluateFiveCards(hand);
        assertEquals(HandRank.STRAIGHT, result.rank());
    }

    @Test
    @DisplayName("应该识别轮盘顺（A-2-3-4-5）")
    void shouldRecognizeWheelStraight() {
        // 构造一副 A-2-3-4-5 的轮盘顺（A作为最小牌）
        Card[] hand = {
                Card.of("spades", "ace"),
                Card.of("hearts", "five"),
                Card.of("diamonds", "four"),
                Card.of("clubs", "three"),
                Card.of("spades", "two")
        };

        EvaluatedHand result = PokerEngine.evaluateFiveCards(hand);
        assertEquals(HandRank.STRAIGHT, result.rank());
    }

    @Test
    @DisplayName("不应该识别非顺子")
    void shouldNotRecognizeNonStraight() {
        // 构造一副 A-K-Q-J-9 的牌（缺少10，不是顺子）
        Card[] hand = {
                Card.of("spades", "ace"),
                Card.of("hearts", "king"),
                Card.of("diamonds", "queen"),
                Card.of("clubs", "jack"),
                Card.of("spades", "nine") // 缺10
        };

        EvaluatedHand result = PokerEngine.evaluateFiveCards(hand);
        assertNotEquals(HandRank.STRAIGHT, result.rank());
    }

    @Test
    @DisplayName("应该识别同花顺")
    void shouldRecognizeStraightFlush() {
        // 构造一副 2-3-4-5-6 的同花顺（全是黑桃）
        Card[] hand = {
                Card.of("spades", "six"),
                Card.of("spades", "five"),
                Card.of("spades", "four"),
                Card.of("spades", "three"),
                Card.of("spades", "two")
        };

        EvaluatedHand result = PokerEngine.evaluateFiveCards(hand);
        assertEquals(HandRank.STRAIGHT_FLUSH, result.rank());
    }

    @Test
    @DisplayName("应该识别四条")
    void shouldRecognizeFourOfAKind() {
        Card[] hand = {
                Card.of("spades", "ace"),
                Card.of("hearts", "ace"),
                Card.of("diamonds", "ace"),
                Card.of("clubs", "ace"),
                Card.of("spades", "king")
        };

        EvaluatedHand result = PokerEngine.evaluateFiveCards(hand);
        assertEquals(HandRank.FOUR_OF_A_KIND, result.rank());
    }

    @Test
    @DisplayName("应该识别三条")
    void shouldRecognizeThreeOfAKind() {
        Card[] hand = {
                Card.of("spades", "ace"),
                Card.of("hearts", "ace"),
                Card.of("diamonds", "ace"),
                Card.of("clubs", "king"),
                Card.of("spades", "queen")
        };

        EvaluatedHand result = PokerEngine.evaluateFiveCards(hand);
        assertEquals(HandRank.THREE_OF_A_KIND, result.rank());
    }

    @Test
    @DisplayName("应该识别一对")
    void shouldRecognizeOnePair() {
        Card[] hand = {
                Card.of("spades", "ace"),
                Card.of("hearts", "ace"),
                Card.of("diamonds", "king"),
                Card.of("clubs", "queen"),
                Card.of("spades", "jack")
        };

        EvaluatedHand result = PokerEngine.evaluateFiveCards(hand);
        assertEquals(HandRank.ONE_PAIR, result.rank());
    }

    @Test
    @DisplayName("应该识别两对")
    void shouldRecognizeTwoPair() {
        Card[] hand = {
                Card.of("spades", "ace"),
                Card.of("hearts", "ace"),
                Card.of("diamonds", "king"),
                Card.of("clubs", "king"),
                Card.of("spades", "queen")
        };

        EvaluatedHand result = PokerEngine.evaluateFiveCards(hand);
        assertEquals(HandRank.TWO_PAIR, result.rank());
    }

    @Test
    @DisplayName("应该识别葫芦")
    void shouldRecognizeFullHouse() {
        Card[] hand = {
                Card.of("spades", "ace"),
                Card.of("hearts", "ace"),
                Card.of("diamonds", "ace"),
                Card.of("clubs", "king"),
                Card.of("spades", "king")
        };

        EvaluatedHand result = PokerEngine.evaluateFiveCards(hand);
        assertEquals(HandRank.FULL_HOUSE, result.rank());
    }

    @Test
    @DisplayName("应该从2张手牌+5张公共牌中找出最佳组合")
    void shouldFindBestHandFromSevenCards() {
        // 手牌：A, K (黑桃)
        Card[] holeCards = {
                Card.of("spades", "ace"),
                Card.of("spades", "king")
        };

        // 公共牌：Q, J, 10 (黑桃) + 2, 3 (梅花)
        Card[] communityCards = {
                Card.of("spades", "queen"),
                Card.of("spades", "jack"),
                Card.of("spades", "ten"),
                Card.of("clubs", "two"),
                Card.of("clubs", "three")
        };

        // 预期最佳组合：A-K-Q-J-10 (皇家同花顺)
        EvaluatedHand result = PokerEngine.evaluateHand(holeCards, communityCards);

        assertEquals(HandRank.ROYAL_FLUSH, result.rank());
    }

    @Test
    @DisplayName("应该从7张牌中选出同花")
    void shouldSelectFlushFromSevenCards() {
        // 手牌：A(黑桃), 2(红桃)
        Card[] holeCards = {
                Card.of("spades", "ace"),
                Card.of("hearts", "two")
        };

        // 公共牌：K, Q, J, 9(黑桃) + 10(梅花)
        // 注意：这里能凑出 A, K, Q, J, 9 的黑桃同花
        Card[] communityCards = {
                Card.of("spades", "king"),
                Card.of("spades", "queen"),
                Card.of("spades", "jack"),
                Card.of("spades", "nine"),
                Card.of("clubs", "ten")
        };

        EvaluatedHand result = PokerEngine.evaluateHand(holeCards, communityCards);

        assertEquals(HandRank.FLUSH, result.rank());
    }

    @Test
    @DisplayName("皇家同花顺应该击败四条")
    void royalFlushShouldBeatFourOfAKind() {
        Card[] royalFlush = {
                Card.of("spades", "ace"),
                Card.of("spades", "king"),
                Card.of("spades", "queen"),
                Card.of("spades", "jack"),
                Card.of("spades", "ten")
        };

        Card[] fourOfAKind = {
                Card.of("spades", "ace"),
                Card.of("hearts", "ace"),
                Card.of("diamonds", "ace"),
                Card.of("clubs", "ace"),
                Card.of("spades", "king")
        };

        EvaluatedHand hand1 = PokerEngine.evaluateFiveCards(royalFlush);
        EvaluatedHand hand2 = PokerEngine.evaluateFiveCards(fourOfAKind);

        int result = PokerEngine.compareHands(hand1, hand2);
        assertTrue(result > 0, "皇家同花顺应该更强");
    }

    @Test
    @DisplayName("相同牌型应该比较踢脚牌")
    void sameRankShouldCompareKickers() {
        Card[] hand1 = {
                Card.of("spades", "ace"),
                Card.of("hearts", "king"),
                Card.of("diamonds", "queen"),
                Card.of("clubs", "jack"),
                Card.of("spades", "nine")
        };

        Card[] hand2 = {
                Card.of("hearts", "ace"),
                Card.of("diamonds", "king"),
                Card.of("clubs", "queen"),
                Card.of("spades", "jack"),
                Card.of("hearts", "eight")
        };

        EvaluatedHand result1 = PokerEngine.evaluateFiveCards(hand1);
        EvaluatedHand result2 = PokerEngine.evaluateFiveCards(hand2);

        int comparison = PokerEngine.compareHands(result1, result2);
        assertTrue(comparison > 0, "踢脚牌更大的应该赢");
    }

    @Test
    @DisplayName("完全相同的牌应该平局")
    void identicalHandsShouldTie() {
        Card[] hand = {
                Card.of("spades", "ace"),
                Card.of("hearts", "king"),
                Card.of("diamonds", "queen"),
                Card.of("clubs", "jack"),
                Card.of("spades", "ten")
        };

        EvaluatedHand result1 = PokerEngine.evaluateFiveCards(hand);
        EvaluatedHand result2 = PokerEngine.evaluateFiveCards(hand);

        int comparison = PokerEngine.compareHands(result1, result2);
        assertEquals(0, comparison, "相同的牌应该平局");
    }

    @Test
    @DisplayName("四条应该击败三条")
    void fourOfAKindShouldBeatThreeOfAKind() {
        Card[] four = {
                Card.of("spades", "ace"),
                Card.of("hearts", "ace"),
                Card.of("diamonds", "ace"),
                Card.of("clubs", "ace"),
                Card.of("spades", "king")
        };

        Card[] three = {
                Card.of("spades", "king"),
                Card.of("hearts", "king"),
                Card.of("diamonds", "king"),
                Card.of("clubs", "ace"),
                Card.of("spades", "queen")
        };

        EvaluatedHand hand1 = PokerEngine.evaluateFiveCards(four);
        EvaluatedHand hand2 = PokerEngine.evaluateFiveCards(three);

        int comparison = PokerEngine.compareHands(hand1, hand2);
        assertTrue(comparison > 0);
    }

    // ========== P0: 皇家同花顺边界测试 ==========

    @Test
    @DisplayName("皇家同花顺：黑桃 A-K-Q-J-10 应该被正确识别")
    void royalFlushSpadesShouldBeRecognized() {
        Card[] royalFlush = {
                Card.of("spades", "ace"),
                Card.of("spades", "king"),
                Card.of("spades", "queen"),
                Card.of("spades", "jack"),
                Card.of("spades", "ten")
        };

        EvaluatedHand result = PokerEngine.evaluateFiveCards(royalFlush);
        assertEquals(HandRank.ROYAL_FLUSH, result.rank());
        assertEquals("皇家同花顺", result.rankName());
        assertEquals(5, result.bestCards().length);
    }

    @Test
    @DisplayName("皇家同花顺：红桃 A-K-Q-J-10 应该被正确识别")
    void royalFlushHeartsShouldBeRecognized() {
        Card[] royalFlush = {
                Card.of("hearts", "ace"),
                Card.of("hearts", "king"),
                Card.of("hearts", "queen"),
                Card.of("hearts", "jack"),
                Card.of("hearts", "ten")
        };

        EvaluatedHand result = PokerEngine.evaluateFiveCards(royalFlush);
        assertEquals(HandRank.ROYAL_FLUSH, result.rank());
        assertEquals("皇家同花顺", result.rankName());
    }

    @Test
    @DisplayName("皇家同花顺：方块 A-K-Q-J-10 应该被正确识别")
    void royalFlushDiamondsShouldBeRecognized() {
        Card[] royalFlush = {
                Card.of("diamonds", "ace"),
                Card.of("diamonds", "king"),
                Card.of("diamonds", "queen"),
                Card.of("diamonds", "jack"),
                Card.of("diamonds", "ten")
        };

        EvaluatedHand result = PokerEngine.evaluateFiveCards(royalFlush);
        assertEquals(HandRank.ROYAL_FLUSH, result.rank());
    }

    @Test
    @DisplayName("皇家同花顺：梅花 A-K-Q-J-10 应该被正确识别")
    void royalFlushClubsShouldBeRecognized() {
        Card[] royalFlush = {
                Card.of("clubs", "ace"),
                Card.of("clubs", "king"),
                Card.of("clubs", "queen"),
                Card.of("clubs", "jack"),
                Card.of("clubs", "ten")
        };

        EvaluatedHand result = PokerEngine.evaluateFiveCards(royalFlush);
        assertEquals(HandRank.ROYAL_FLUSH, result.rank());
    }

    @Test
    @DisplayName("同花顺：K-Q-J-9-8 非连续，不应该误判为皇家同花顺")
    void nonConsecutiveSpadesShouldNotBeRoyalFlush() {
        Card[] hand = {
                Card.of("spades", "king"),
                Card.of("spades", "queen"),
                Card.of("spades", "jack"),
                Card.of("spades", "nine"),
                Card.of("spades", "eight")
        };

        EvaluatedHand result = PokerEngine.evaluateFiveCards(hand);
        assertNotEquals(HandRank.ROYAL_FLUSH, result.rank());
        assertEquals(HandRank.FLUSH, result.rank());
    }

    @Test
    @DisplayName("同花：A-2-4-5-7 点数不连续，不应该误判为顺子")
    void nonConsecutiveRanksShouldNotBeStraight() {
        Card[] hand = {
                Card.of("spades", "ace"),
                Card.of("spades", "two"),
                Card.of("spades", "four"),
                Card.of("spades", "five"),
                Card.of("spades", "seven")
        };

        EvaluatedHand result = PokerEngine.evaluateFiveCards(hand);
        assertNotEquals(HandRank.STRAIGHT, result.rank());
        assertNotEquals(HandRank.STRAIGHT_FLUSH, result.rank());
        assertNotEquals(HandRank.ROYAL_FLUSH, result.rank());
        assertEquals(HandRank.FLUSH, result.rank());
    }

    @Test
    @DisplayName("同花顺：只有2张A，不是皇家同花顺")
    void twoAcesShouldNotBeRoyalFlush() {
        Card[] hand = {
                Card.of("spades", "ace"),
                Card.of("hearts", "ace"),
                Card.of("spades", "king"),
                Card.of("spades", "queen"),
                Card.of("spades", "jack")
        };

        EvaluatedHand result = PokerEngine.evaluateFiveCards(hand);
        assertNotEquals(HandRank.ROYAL_FLUSH, result.rank());
        assertNotEquals(HandRank.STRAIGHT_FLUSH, result.rank());
    }

    // ========== P0: 边界条件测试 ==========

    @Test
    @DisplayName("边界：空数组应该抛出异常")
    void emptyArrayShouldThrowException() {
        Card[] empty = new Card[0];

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> PokerEngine.evaluateFiveCards(empty));

        assertEquals("必须提供5张牌进行评估", exception.getMessage());
    }

    @Test
    @DisplayName("边界：null数组应该抛出异常")
    void nullArrayShouldThrowException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> PokerEngine.evaluateFiveCards(null));

        assertEquals("必须提供5张牌进行评估", exception.getMessage());
    }

    @Test
    @DisplayName("边界：单张牌应该抛出异常")
    void singleCardShouldThrowException() {
        Card[] single = {
                Card.of("spades", "ace")
        };

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> PokerEngine.evaluateFiveCards(single));

        assertEquals("必须提供5张牌进行评估", exception.getMessage());
    }

    @Test
    @DisplayName("边界：4张牌应该抛出异常")
    void fourCardsShouldThrowException() {
        Card[] four = {
                Card.of("spades", "ace"),
                Card.of("hearts", "king"),
                Card.of("diamonds", "queen"),
                Card.of("clubs", "jack")
        };

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> PokerEngine.evaluateFiveCards(four));

        assertEquals("必须提供5张牌进行评估", exception.getMessage());
    }

    @Test
    @DisplayName("边界：6张牌应该抛出异常")
    void sixCardsShouldThrowException() {
        Card[] six = {
                Card.of("spades", "ace"),
                Card.of("hearts", "king"),
                Card.of("diamonds", "queen"),
                Card.of("clubs", "jack"),
                Card.of("spades", "ten"),
                Card.of("hearts", "nine")
        };

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> PokerEngine.evaluateFiveCards(six));

        assertEquals("必须提供5张牌进行评估", exception.getMessage());
    }

    @Test
    @DisplayName("边界：手牌null应该抛出异常")
    void nullHoleCardsShouldThrowException() {
        Card[] community = {
                Card.of("spades", "ace"),
                Card.of("hearts", "king"),
                Card.of("diamonds", "queen"),
                Card.of("clubs", "jack"),
                Card.of("spades", "ten")
        };

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> PokerEngine.evaluateHand(null, community));

        assertEquals("手牌和公共牌不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("边界：公共牌null应该抛出异常")
    void nullCommunityCardsShouldThrowException() {
        Card[] hole = {
                Card.of("spades", "ace"),
                Card.of("hearts", "king")
        };

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> PokerEngine.evaluateHand(hole, null));

        assertEquals("手牌和公共牌不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("边界：总牌数不足5张应该抛出异常")
    void insufficientTotalCardsShouldThrowException() {
        Card[] hole = {
                Card.of("spades", "ace"),
                Card.of("hearts", "king")
        };

        Card[] community = {
                Card.of("diamonds", "queen"),
                Card.of("clubs", "jack")
        };

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> PokerEngine.evaluateHand(hole, community));

        assertEquals("总牌数不足5张，无法评估", exception.getMessage());
    }

    // ========== P0: 组合算法边界测试 ==========

    @Test
    @DisplayName("7张牌：应该从7张相同花色的牌中选出最佳5张")
    void shouldSelectBestFromSevenSameSuit() {
        Card[] hole = {
                Card.of("spades", "ace"),
                Card.of("spades", "two")
        };

        Card[] community = {
                Card.of("spades", "king"),
                Card.of("spades", "queen"),
                Card.of("spades", "jack"),
                Card.of("spades", "ten"),
                Card.of("spades", "nine")
        };

        EvaluatedHand result = PokerEngine.evaluateHand(hole, community);
        assertEquals(HandRank.ROYAL_FLUSH, result.rank());
        assertEquals("皇家同花顺", result.rankName());
    }

    @Test
    @DisplayName("7张牌：应该正确选择四条加最大踢脚牌")
    void shouldSelectFourOfAKindWithMaxKicker() {
        Card[] hole = {
                Card.of("spades", "ace"),
                Card.of("hearts", "ace")
        };

        Card[] community = {
                Card.of("diamonds", "ace"),
                Card.of("clubs", "ace"),
                Card.of("spades", "king"),
                Card.of("hearts", "queen"),
                Card.of("diamonds", "jack")
        };

        EvaluatedHand result = PokerEngine.evaluateHand(hole, community);
        assertEquals(HandRank.FOUR_OF_A_KIND, result.rank());
        assertEquals("四条", result.rankName());
    }

    @Test
    @DisplayName("7张牌：应该正确选择三条加最大两个踢脚牌")
    void shouldSelectThreeOfAKindWithMaxKickers() {
        Card[] hole = {
                Card.of("spades", "ace"),
                Card.of("hearts", "king")
        };

        Card[] community = {
                Card.of("diamonds", "ace"),
                Card.of("clubs", "ace"),
                Card.of("spades", "queen"),
                Card.of("hearts", "nine"),
                Card.of("diamonds", "eight")
        };

        EvaluatedHand result = PokerEngine.evaluateHand(hole, community);
        assertEquals(HandRank.THREE_OF_A_KIND, result.rank());
        assertEquals("三条", result.rankName());
    }

    @Test
    @DisplayName("7张牌：应该正确选择两对加最大踢脚牌")
    void shouldSelectTwoPairsWithMaxKicker() {
        Card[] hole = {
                Card.of("spades", "ace"),
                Card.of("hearts", "king")
        };

        Card[] community = {
                Card.of("diamonds", "ace"),
                Card.of("clubs", "king"),
                Card.of("spades", "queen"),
                Card.of("hearts", "nine"),
                Card.of("diamonds", "eight")
        };

        EvaluatedHand result = PokerEngine.evaluateHand(hole, community);
        assertEquals(HandRank.TWO_PAIR, result.rank());
        assertEquals("两对", result.rankName());
    }

    @Test
    @DisplayName("7张牌：应该正确选择一对加最大三个踢脚牌")
    void shouldSelectOnePairWithMaxKickers() {
        Card[] hole = {
                Card.of("spades", "ace"),
                Card.of("hearts", "king")
        };

        Card[] community = {
                Card.of("diamonds", "ace"),
                Card.of("clubs", "queen"),
                Card.of("spades", "jack"),
                Card.of("hearts", "nine"),
                Card.of("diamonds", "eight")
        };

        EvaluatedHand result = PokerEngine.evaluateHand(hole, community);
        assertEquals(HandRank.ONE_PAIR, result.rank());
        assertEquals("一对", result.rankName());
    }

    // ========== P0: 同花顺边界测试 ==========

    @Test
    @DisplayName("同花顺：9-8-7-6-5 杂色应该被识别为同花顺")
    void straightFlushMixedSuitsShouldBeRecognized() {
        Card[] hand = {
                Card.of("spades", "nine"),
                Card.of("spades", "eight"),
                Card.of("spades", "seven"),
                Card.of("spades", "six"),
                Card.of("spades", "five")
        };

        EvaluatedHand result = PokerEngine.evaluateFiveCards(hand);
        assertEquals(HandRank.STRAIGHT_FLUSH, result.rank());
        assertEquals("同花顺", result.rankName());
    }

    @Test
    @DisplayName("同花：同花但非连续的5张牌应该识别为同花")
    void flushNonConsecutiveShouldBeFlush() {
        Card[] hand = {
                Card.of("spades", "ace"),
                Card.of("spades", "king"),
                Card.of("spades", "queen"),
                Card.of("spades", "jack"),
                Card.of("spades", "nine")
        };

        EvaluatedHand result = PokerEngine.evaluateFiveCards(hand);
        assertEquals(HandRank.FLUSH, result.rank());
        assertNotEquals(HandRank.STRAIGHT_FLUSH, result.rank());
    }

    @Test
    @DisplayName("同花顺：轮盘顺 A-2-3-4-5 同花应该被识别为同花顺")
    void wheelStraightFlushShouldBeRecognized() {
        Card[] hand = {
                Card.of("spades", "ace"),
                Card.of("spades", "two"),
                Card.of("spades", "three"),
                Card.of("spades", "four"),
                Card.of("spades", "five")
        };

        EvaluatedHand result = PokerEngine.evaluateFiveCards(hand);
        assertEquals(HandRank.STRAIGHT_FLUSH, result.rank());
        assertEquals("同花顺", result.rankName());
    }

    // ========== P1: 四条牌边界测试 ==========

    @Test
    @DisplayName("四条：四条A加K踢脚牌应该被识别")
    void fourOfAcesWithKingShouldBeRecognized() {
        Card[] hand = {
                Card.of("spades", "ace"),
                Card.of("hearts", "ace"),
                Card.of("diamonds", "ace"),
                Card.of("clubs", "ace"),
                Card.of("spades", "king")
        };

        EvaluatedHand result = PokerEngine.evaluateFiveCards(hand);
        assertEquals(HandRank.FOUR_OF_A_KIND, result.rank());
        assertEquals("四条", result.rankName());
    }

    @Test
    @DisplayName("四条：四条K加A踢脚牌应该被识别")
    void fourOfKingsWithAceShouldBeRecognized() {
        Card[] hand = {
                Card.of("spades", "king"),
                Card.of("hearts", "king"),
                Card.of("diamonds", "king"),
                Card.of("clubs", "king"),
                Card.of("spades", "ace")
        };

        EvaluatedHand result = PokerEngine.evaluateFiveCards(hand);
        assertEquals(HandRank.FOUR_OF_A_KIND, result.rank());
    }

    @Test
    @DisplayName("四条：四条2加3踢脚牌应该被识别")
    void fourOfTwosShouldBeRecognized() {
        Card[] hand = {
                Card.of("spades", "two"),
                Card.of("hearts", "two"),
                Card.of("diamonds", "two"),
                Card.of("clubs", "two"),
                Card.of("spades", "three")
        };

        EvaluatedHand result = PokerEngine.evaluateFiveCards(hand);
        assertEquals(HandRank.FOUR_OF_A_KIND, result.rank());
    }

    @Test
    @DisplayName("四条：所有牌相同点数应该是四条（实际只能出现4张）")
    void fourOfAKindShouldRecognize() {
        Card[] hand = {
                Card.of("spades", "ace"),
                Card.of("hearts", "ace"),
                Card.of("diamonds", "ace"),
                Card.of("clubs", "ace"),
                Card.of("spades", "king")
        };

        EvaluatedHand result = PokerEngine.evaluateFiveCards(hand);
        assertEquals(HandRank.FOUR_OF_A_KIND, result.rank());
    }

    // ========== P1: 三条牌边界测试 ==========

    @Test
    @DisplayName("三条：三条A加KQ踢脚牌应该被识别")
    void threeOfAcesWithKingQueenShouldBeRecognized() {
        Card[] hand = {
                Card.of("spades", "ace"),
                Card.of("hearts", "ace"),
                Card.of("diamonds", "ace"),
                Card.of("clubs", "king"),
                Card.of("spades", "queen")
        };

        EvaluatedHand result = PokerEngine.evaluateFiveCards(hand);
        assertEquals(HandRank.THREE_OF_A_KIND, result.rank());
        assertEquals("三条", result.rankName());
    }

    @Test
    @DisplayName("三条：三条2加34踢脚牌应该被识别")
    void threeOfTwosShouldBeRecognized() {
        Card[] hand = {
                Card.of("spades", "two"),
                Card.of("hearts", "two"),
                Card.of("diamonds", "two"),
                Card.of("clubs", "three"),
                Card.of("spades", "four")
        };

        EvaluatedHand result = PokerEngine.evaluateFiveCards(hand);
        assertEquals(HandRank.THREE_OF_A_KIND, result.rank());
    }

    @Test
    @DisplayName("三条：不同花色但相同点数的三条牌应该被识别")
    void threeOfAKindDifferentSuitShouldBeRecognized() {
        Card[] hand = {
                Card.of("spades", "ace"),
                Card.of("hearts", "ace"),
                Card.of("diamonds", "ace"),
                Card.of("clubs", "king"),
                Card.of("spades", "queen")
        };

        EvaluatedHand result = PokerEngine.evaluateFiveCards(hand);
        assertEquals(HandRank.THREE_OF_A_KIND, result.rank());
    }

    // ==================== 牌组管理测试 ====================

    /**
     * 牌组管理测试类
     * 测试 createDeck(), shuffleDeck(), dealCards() 方法
     */
    @DisplayName("牌组管理测试")
    @Nested
    class CardDeckTests {

        @Test
        @DisplayName("createDeck 应该创建52张牌")
        void testCreateDeckHas52Cards() {
            List<Card> deck = PokerEngine.createDeck();
            assertEquals(52, deck.size(), "牌组应该有52张牌");
        }

        @Test
        @DisplayName("createDeck 所有牌应该唯一")
        void testCreateDeckAllUnique() {
            List<Card> deck = PokerEngine.createDeck();
            Set<Card> uniqueCards = new HashSet<>(deck);
            assertEquals(52, uniqueCards.size(), "所有牌应该唯一");
        }

        @Test
        @DisplayName("createDeck 应该包含所有52种组合")
        void testCreateDeckAllCombinations() {
            List<Card> deck = PokerEngine.createDeck();
            for (Suit suit : Suit.values()) {
                for (Rank rank : Rank.values()) {
                    Card expectedCard = Card.of(suit.getValue(), rank.getName());
                    assertTrue(deck.contains(expectedCard),
                            "牌组应包含: " + suit.getValue() + " " + rank.getValue());
                }
            }
        }

        @Test
        @DisplayName("createDeck 默认顺序正确")
        void testCreateDeckOrder() {
            List<Card> deck = PokerEngine.createDeck();
            // 验证默认顺序（按suit顺序，再按rank从大到小）
            // 期望: clubs, diamonds, hearts, spades 每种花色从 ace 到 2
            for (int i = 0; i < deck.size(); i++) {
                Card card = deck.get(i);
                int expectedSuitIndex = i / 13; // 每13张牌换一种花色
                Suit expectedSuit = Suit.values()[expectedSuitIndex];
                assertEquals(expectedSuit, card.suit(),
                        "第" + (i + 1) + "张牌的花色应为" + expectedSuit.getValue());
            }
        }

        // ==================== 洗牌测试 ====================

        @Test
        @DisplayName("shuffleDeck 洗牌后数量不变")
        void testShuffleDeckSizeUnchanged() {
            List<Card> deck = PokerEngine.createDeck();
            int originalSize = deck.size();
            PokerEngine.shuffleDeck(deck);
            assertEquals(originalSize, deck.size(), "洗牌后数量应不变");
        }

        @Test
        @DisplayName("shuffleDeck 随机性验证")
        void testShuffleDeckRandomness() {
            List<Card> deck1 = PokerEngine.createDeck();
            List<Card> deck2 = PokerEngine.createDeck();

            PokerEngine.shuffleDeck(deck1);
            PokerEngine.shuffleDeck(deck2);

            // 两次洗牌结果应不同（概率极低相同）
            // 注意：由于随机性，这个测试偶尔可能失败，但概率极低
            assertNotEquals(deck1, deck2, "两次洗牌结果应不同（概率99.9999%以上）");
        }

        @Test
        @DisplayName("shuffleDeck 洗牌后无重复牌")
        void testShuffleDeckAllUnique() {
            List<Card> deck = PokerEngine.createDeck();
            PokerEngine.shuffleDeck(deck);

            Set<Card> uniqueCards = new HashSet<>(deck);
            assertEquals(52, uniqueCards.size(), "洗牌后应无重复牌");
        }

        @Test
        @DisplayName("shuffleDeck 原地操作验证")
        void testShuffleDeckImmutability() {
            List<Card> deck = PokerEngine.createDeck();
            // 记录原始引用
            List<Card> originalReference = deck;

            PokerEngine.shuffleDeck(deck);

            // 验证是原地操作（引用不变）
            assertSame(originalReference, deck, "洗牌应为原地操作（引用不变）");
        }

        // ==================== 发牌测试 ====================

        @Test
        @DisplayName("dealCards 应该抽取指定数量的牌")
        void testDealCardsCorrectCount() {
            List<Card> deck = PokerEngine.createDeck();
            int originalSize = deck.size();

            List<Card> dealt = PokerEngine.dealCards(deck, 5);

            assertEquals(5, dealt.size(), "应抽取5张牌");
            assertEquals(originalSize - 5, deck.size(), "牌组应减少5张");
        }

        @Test
        @DisplayName("dealCards 应该从顶部抽牌")
        void testDealCardsFromTop() {
            List<Card> deck = PokerEngine.createDeck();
            // 记录前5张牌
            List<Card> firstFive = new ArrayList<>(deck.subList(0, 5));

            List<Card> dealt = PokerEngine.dealCards(deck, 5);

            assertEquals(firstFive, dealt, "应从牌堆顶部抽牌");
        }

        @Test
        @DisplayName("dealCards 牌堆不足时应返回全部剩余")
        void testDealCardsInsufficient() {
            List<Card> deck = PokerEngine.createDeck();
            deck.subList(0, 48).clear(); // 只剩4张

            List<Card> dealt = PokerEngine.dealCards(deck, 5);

            assertEquals(4, dealt.size(), "应返回全部剩余4张牌");
            assertTrue(deck.isEmpty(), "牌堆应为空");
        }

        @Test
        @DisplayName("dealCards 空牌堆应返回空列表")
        void testDealCardsEmptyDeck() {
            List<Card> deck = new ArrayList<>();

            List<Card> dealt = PokerEngine.dealCards(deck, 5);

            assertTrue(dealt.isEmpty(), "空牌堆应返回空列表");
        }

        // ==================== 底牌和公共牌测试 ====================

        @Test
        @DisplayName("dealHoleCards 应该每人发2张牌")
        void testDealHoleCardsTwoPerPlayer() {
            List<Card> deck = PokerEngine.createDeck();
            Map<Integer, List<Card>> holeCards = PokerEngine.dealHoleCards(deck, 3);

            assertEquals(3, holeCards.size(), "应为3个玩家发牌");
            for (Map.Entry<Integer, List<Card>> entry : holeCards.entrySet()) {
                assertEquals(2, entry.getValue().size(), "玩家" + entry.getKey() + "应有2张底牌");
            }
        }

        @Test
        @DisplayName("dealHoleCards 所有底牌应唯一")
        void testDealHoleCardsUniqueCards() {
            List<Card> deck = PokerEngine.createDeck();
            Map<Integer, List<Card>> holeCards = PokerEngine.dealHoleCards(deck, 4);

            // 收集所有底牌
            List<Card> allCards = new ArrayList<>();
            for (List<Card> cards : holeCards.values()) {
                allCards.addAll(cards);
            }

            Set<Card> uniqueCards = new HashSet<>(allCards);
            assertEquals(allCards.size(), uniqueCards.size(), "所有底牌应唯一");
        }

        @Test
        @DisplayName("dealHoleCards 发牌顺序正确")
        void testDealHoleCardsOrder() {
            List<Card> deck = PokerEngine.createDeck();
            Map<Integer, List<Card>> holeCards = PokerEngine.dealHoleCards(deck, 3);

            // 玩家0应该拿到第1张和第4张牌（A♠和9♠，假设顺序是CLUBS→DIAMONDS→HEARTS→SPADES）
            List<Card> player0Cards = holeCards.get(0);
            assertEquals(Card.of("clubs", "ace"), player0Cards.get(0), "玩家0第1张牌应是ACE of CLUBS");
            assertEquals(Card.of("clubs", "nine"), player0Cards.get(1), "玩家0第2张牌应是9 of CLUBS");
        }

        @Test
        @DisplayName("dealCommunityCards FLOP应发3张")
        void testDealCommunityCardsFlop() {
            List<Card> deck = PokerEngine.createDeck();
            List<Card> community = PokerEngine.dealCommunityCards(deck, GamePhase.FLOP);

            assertEquals(3, community.size(), "FLOP应发3张公共牌");
            assertEquals(52 - 1 - 3, deck.size(), "牌堆应减少4张（1张烧牌+3张公共牌）");
        }

        @Test
        @DisplayName("dealCommunityCards TURN应发1张")
        void testDealCommunityCardsTurn() {
            List<Card> deck = PokerEngine.createDeck();
            List<Card> community = PokerEngine.dealCommunityCards(deck, GamePhase.TURN);

            assertEquals(1, community.size(), "TURN应发1张公共牌");
            assertEquals(52 - 1 - 1, deck.size(), "牌堆应减少2张（1张烧牌+1张公共牌）");
        }

        @Test
        @DisplayName("dealCommunityCards RIVER应发1张")
        void testDealCommunityCardsRiver() {
            List<Card> deck = PokerEngine.createDeck();
            List<Card> community = PokerEngine.dealCommunityCards(deck, GamePhase.RIVER);

            assertEquals(1, community.size(), "RIVER应发1张公共牌");
            assertEquals(52 - 1 - 1, deck.size(), "牌堆应减少2张（1张烧牌+1张公共牌）");
        }

        @Test
        @DisplayName("dealCommunityCards PRE_FLOP不应发牌")
        void testDealCommunityCardsPreFlop() {
            List<Card> deck = PokerEngine.createDeck();
            List<Card> community = PokerEngine.dealCommunityCards(deck, GamePhase.PRE_FLOP);

            assertTrue(community.isEmpty(), "PRE_FLOP不应发公共牌");
            assertEquals(52, deck.size(), "牌堆应不变");
        }
    }
}
