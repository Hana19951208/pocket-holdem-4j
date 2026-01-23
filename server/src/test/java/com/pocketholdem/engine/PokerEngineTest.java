package com.pocketholdem.engine;

import com.pocketholdem.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

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
        Card[] hand = {
            Card.of("spades", "ace"),
            Card.of("hearts", "king"),
            Card.of("diamonds", "queen"),
            Card.of("clubs", "jack"),
            Card.of("spades", "nine")
        };

        EvaluatedHand result = PokerEngine.evaluateFiveCards(hand);
        assertNotEquals(HandRank.STRAIGHT, result.rank());
    }

    @Test
    @DisplayName("应该识别同花顺")
    void shouldRecognizeStraightFlush() {
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
}
