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
            Card.of("spades", "ten")
        };

        EvaluatedHand result = PokerEngine.evaluateFiveCards(hand);

        assertEquals(HandRank.HIGH_CARD, result.rank());
        assertEquals(5, result.bestCards().length);
    }
}
