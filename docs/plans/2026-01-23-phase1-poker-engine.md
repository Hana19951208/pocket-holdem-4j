# Pocket Holdem 阶段1：核心引擎开发实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将 Node.js/TypeScript 扑克引擎重构为 Java 17 版本，实现牌型评估、比牌逻辑和边池计算。

**Architecture:** 采用纯函数式设计，PokerEngine作为无状态工具类，所有核心逻辑均为纯函数，不可变数据结构使用Record，可变实体使用Lombok。

**Tech Stack:** Java 17, Spring Boot 3.2.4, Lombok, JUnit 5, JaCoCo (覆盖率)

---

## 子冲刺 A：基础数据结构实现（预计3-4小时）

### Task A1: 实现花色枚举 Suit

**Files:**
- Create: `server/src/main/java/com/pocketholdem/model/Suit.java`
- Test: `server/src/test/java/com/pocketholdem/model/SuitTest.java`

**Step 1: 写失败的测试**

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("花色枚举测试")
class SuitTest {

    @Test
    @DisplayName("应该有4种花色")
    void shouldHaveFourSuits() {
        assertEquals(4, Suit.values().length);
    }

    @Test
    @DisplayName("花色应该有序")
    void suitsShouldBeOrdered() {
        Suit[] suits = Suit.values();
        assertEquals(Suit.SPADES, suits[3]);
    }

    @Test
    @DisplayName("花色序列化应该输出英文")
    void suitShouldSerializeToEnglish() {
        assertEquals("spades", Suit.SPADES.name().toLowerCase());
    }
}
```

**Step 2: 运行测试验证失败**

```bash
cd server
./mvnw test -Dtest=SuitTest
```

Expected: ❌ 找不到类 Suit

**Step 3: 实现最小代码**

```java
package com.pocketholdem.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 扑克花色枚举
 * 四种花色：黑桃、红桃、方块、梅花
 */
public enum Suit {
    CLUBS("clubs"),
    DIAMONDS("diamonds"),
    HEARTS("hearts"),
    SPADES("spades");

    private final String value;

    Suit(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
```

**Step 4: 运行测试验证通过**

```bash
cd server
./mvnw test -Dtest=SuitTest
```

Expected: ✅ 所有测试通过

**Step 5: 提交**

```bash
git add server/src/main/java/com/pocketholdem/model/Suit.java server/src/test/java/com/pocketholdem/model/SuitTest.java
git commit -m "feat: 实现花色枚举Suit"
```

---

### Task A2: 实现点数枚举 Rank

**Files:**
- Create: `server/src/main/java/com/pocketholdem/model/Rank.java`
- Test: `server/src/test/java/com/pocketholdem/model/RankTest.java`

**Step 1: 写失败的测试**

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("点数枚举测试")
class RankTest {

    @Test
    @DisplayName("应该有13个点数（2-10, J, Q, K, A）")
    void shouldHaveThirteenRanks() {
        assertEquals(13, Rank.values().length);
    }

    @Test
    @DisplayName("点数应该从2到A有序")
    void ranksShouldBeOrdered() {
        assertEquals(2, Rank.TWO.getValue());
        assertEquals(10, Rank.TEN.getValue());
        assertEquals(11, Rank.JACK.getValue());
        assertEquals(12, Rank.QUEEN.getValue());
        assertEquals(13, Rank.KING.getValue());
        assertEquals(14, Rank.ACE.getValue());
    }

    @Test
    @DisplayName("A应该是最高的")
    void aceShouldBeHighest() {
        assertEquals(14, Rank.ACE.getValue());
    }

    @Test
    @DisplayName("序列化应该输出小写英文")
    void rankShouldSerializeToLowercase() {
        assertEquals("ace", Rank.ACE.getValue().toLowerCase());
    }
}
```

**Step 2: 运行测试验证失败**

```bash
cd server
./mvnw test -Dtest=RankTest
```

Expected: ❌ 找不到类 Rank

**Step 3: 实现最小代码**

```java
package com.pocketholdem.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 扑克点数枚举
 * 2-10（数值），J=11, Q=12, K=13, A=14
 */
public enum Rank {
    TWO("two", 2),
    THREE("three", 3),
    FOUR("four", 4),
    FIVE("five", 5),
    SIX("six", 6),
    SEVEN("seven", 7),
    EIGHT("eight", 8),
    NINE("nine", 9),
    TEN("ten", 10),
    JACK("jack", 11),
    QUEEN("queen", 12),
    KING("king", 13),
    ACE("ace", 14);

    private final String name;
    private final int value;

    Rank(String name, int value) {
        this.name = name;
        this.value = value;
    }

    @JsonValue
    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }
}
```

**Step 4: 运行测试验证通过**

```bash
cd server
./mvnw test -Dtest=RankTest
```

Expected: ✅ 所有测试通过

**Step 5: 提交**

```bash
git add server/src/main/java/com/pocketholdem/model/Rank.java server/src/test/java/com/pocketholdem/model/RankTest.java
git commit -m "feat: 实现点数枚举Rank"
```

---

### Task A3: 实现扑克牌 Record Card

**Files:**
- Create: `server/src/main/java/com/pocketholdem/model/Card.java`
- Test: `server/src/test/java/com/pocketholdem/model/CardTest.java`

**Step 1: 写失败的测试**

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("扑克牌Record测试")
class CardTest {

    @Test
    @DisplayName("应该创建一张牌")
    void shouldCreateCard() {
        Card card = new Card(Suit.SPADES, Rank.ACE);
        assertEquals(Suit.SPADES, card.suit());
        assertEquals(Rank.ACE, card.rank());
    }

    @Test
    @DisplayName("应该序列化为snake_case")
    void shouldSerializeToSnakeCase() {
        Card card = new Card(Suit.SPADES, Rank.ACE);
        String json = new ObjectMapper().writeValueAsString(card);
        assertTrue(json.contains("\"suit\":\"spades\""));
        assertTrue(json.contains("\"rank\":\"ace\""));
    }

    @Test
    @DisplayName("相同的花色和点数应该相等")
    void sameSuitAndRankShouldBeEqual() {
        Card card1 = new Card(Suit.SPADES, Rank.ACE);
        Card card2 = new Card(Suit.SPADES, Rank.ACE);
        assertEquals(card1, card2);
    }

    @Test
    @DisplayName("不同的花色或点数应该不相等")
    void differentShouldNotBeEqual() {
        Card card1 = new Card(Suit.SPADES, Rank.ACE);
        Card card2 = new Card(Suit.HEARTS, Rank.ACE);
        assertNotEquals(card1, card2);
    }
}
```

**Step 2: 运行测试验证失败**

```bash
cd server
./mvnw test -Dtest=CardTest
```

Expected: ❌ 找不到类 Card

**Step 3: 实现最小代码**

```java
package com.pocketholdem.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 扑克牌（不可变Record）
 */
public record Card(
        @JsonProperty("suit") Suit suit,
        @JsonProperty("rank") Rank rank
) {
    /**
     * 创建用于测试的便捷方法
     */
    public static Card of(String suit, String rank) {
        return new Card(
                Suit.valueOf(suit.toUpperCase()),
                Rank.valueOf(rank.toUpperCase())
        );
    }
}
```

**Step 4: 运行测试验证通过**

```bash
cd server
./mvnw test -Dtest=CardTest
```

Expected: ✅ 所有测试通过

**Step 5: 提交**

```bash
git add server/src/main/java/com/pocketholdem/model/Card.java server/src/test/java/com/pocketholdem/model/CardTest.java
git commit -m "feat: 实现扑克牌Record Card"
```

---

### Task A4: 实现牌型等级枚举 HandRank

**Files:**
- Create: `server/src/main/java/com/pocketholdem/model/HandRank.java`
- Test: `server/src/test/java/com/pocketholdem/model/HandRankTest.java`

**Step 1: 写失败的测试**

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("牌型等级枚举测试")
class HandRankTest {

    @Test
    @DisplayName("应该有10种牌型")
    void shouldHaveTenHandRanks() {
        assertEquals(10, HandRank.values().length);
    }

    @Test
    @DisplayName("牌型应该从高牌到皇家同花顺有序")
    void handRanksShouldBeOrdered() {
        assertEquals(1, HandRank.HIGH_CARD.getValue());
        assertEquals(10, HandRank.ROYAL_FLUSH.getValue());
    }

    @Test
    @DisplayName("皇家同花顺应该是最高的")
    void royalFlushShouldBeHighest() {
        assertEquals(10, HandRank.ROYAL_FLUSH.getValue());
    }

    @Test
    @DisplayName("高牌应该是最低的")
    void highCardShouldBeLowest() {
        assertEquals(1, HandRank.HIGH_CARD.getValue());
    }
}
```

**Step 2: 运行测试验证失败**

```bash
cd server
./mvnw test -Dtest=HandRankTest
```

Expected: ❌ 找不到类 HandRank

**Step 3: 实现最小代码**

```java
package com.pocketholdem.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 扑克牌型等级枚举
 * 从高牌（1）到皇家同花顺（10）
 */
public enum HandRank {
    HIGH_CARD("high_card", 1),
    ONE_PAIR("one_pair", 2),
    TWO_PAIR("two_pair", 3),
    THREE_OF_A_KIND("three_of_a_kind", 4),
    STRAIGHT("straight", 5),
    FLUSH("flush", 6),
    FULL_HOUSE("full_house", 7),
    FOUR_OF_A_KIND("four_of_a_kind", 8),
    STRAIGHT_FLUSH("straight_flush", 9),
    ROYAL_FLUSH("royal_flush", 10);

    private final String name;
    private final int value;

    HandRank(String name, int value) {
        this.name = name;
        this.value = value;
    }

    @JsonValue
    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }
}
```

**Step 4: 运行测试验证通过**

```bash
cd server
./mvnw test -Dtest=HandRankTest
```

Expected: ✅ 所有测试通过

**Step 5: 提交**

```bash
git add server/src/main/java/com/pocketholdem/model/HandRank.java server/src/test/java/com/pocketholdem/model/HandRankTest.java
git commit -m "feat: 实现牌型等级枚举HandRank"
```

---

### Task A5: 实现 ChipCalculator 工具类（溢出检查）

**Files:**
- Create: `server/src/main/java/com/pocketholdem/util/ChipCalculator.java`
- Test: `server/src/test/java/com/pocketholdem/util/ChipCalculatorTest.java`

**Step 1: 写失败的测试**

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("筹码计算器测试")
class ChipCalculatorTest {

    @Test
    @DisplayName("正常加法应该成功")
    void normalAdditionShouldSucceed() {
        assertEquals(300, ChipCalculator.safeAdd(100, 200));
    }

    @Test
    @DisplayName("溢出加法应该抛出异常")
    void overflowAdditionShouldThrowException() {
        int max = Integer.MAX_VALUE;
        assertThrows(ArithmeticException.class,
            () -> ChipCalculator.safeAdd(max, 1));
    }

    @Test
    @DisplayName("正常减法应该成功")
    void normalSubtractionShouldSucceed() {
        assertEquals(100, ChipCalculator.safeSubtract(300, 200));
    }

    @Test
    @DisplayName("下溢减法应该抛出异常")
    void underflowSubtractionShouldThrowException() {
        assertThrows(ArithmeticException.class,
            () -> ChipCalculator.safeSubtract(100, 200));
    }

    @Test
    @DisplayName("正常乘法应该成功")
    void normalMultiplicationShouldSucceed() {
        assertEquals(600, ChipCalculator.safeMultiply(100, 6));
    }

    @Test
    @DisplayName("溢出乘法应该抛出异常")
    void overflowMultiplicationShouldThrowException() {
        int max = Integer.MAX_VALUE / 2;
        assertThrows(ArithmeticException.class,
            () -> ChipCalculator.safeMultiply(max, 3));
    }

    @Test
    @DisplayName("正常除法应该成功")
    void normalDivisionShouldSucceed() {
        assertEquals(50, ChipCalculator.safeDivide(100, 2));
    }

    @Test
    @DisplayName("除零应该抛出异常")
    void divisionByZeroShouldThrowException() {
        assertThrows(ArithmeticException.class,
            () -> ChipCalculator.safeDivide(100, 0));
    }
}
```

**Step 2: 运行测试验证失败**

```bash
cd server
./mvnw test -Dtest=ChipCalculatorTest
```

Expected: ❌ 找不到类 ChipCalculator

**Step 3: 实现最小代码**

```java
package com.pocketholdem.util;

/**
 * 筹码计算工具类
 * 所有运算都进行溢出检查，防止整数溢出
 *
 * 参考：docs/numeric-spec.md
 */
public final class ChipCalculator {

    private ChipCalculator() {
        // 工具类，禁止实例化
    }

    /**
     * 安全加法，检查溢出
     */
    public static int safeAdd(int a, int b) {
        if (b > 0 && a > Integer.MAX_VALUE - b) {
            throw new ArithmeticException("整数加法溢出: " + a + " + " + b);
        }
        if (b < 0 && a < Integer.MIN_VALUE - b) {
            throw new ArithmeticException("整数加法下溢: " + a + " + " + b);
        }
        return a + b;
    }

    /**
     * 安全减法，检查溢出
     */
    public static int safeSubtract(int a, int b) {
        if (b < 0 && a > Integer.MAX_VALUE + b) {
            throw new ArithmeticException("整数减法溢出: " + a + " - " + b);
        }
        if (b > 0 && a < Integer.MIN_VALUE + b) {
            throw new ArithmeticException("整数减法下溢: " + a + " - " + b);
        }
        return a - b;
    }

    /**
     * 安全乘法，检查溢出
     */
    public static int safeMultiply(int a, int b) {
        if (a > b) {
            // 交换，减少检查次数
            return safeMultiply(b, a);
        }

        if (a < 0) {
            // 处理负数
            if (b < 0) {
                if (a < Integer.MAX_VALUE / b) {
                    throw new ArithmeticException("整数乘法溢出: " + a + " * " + b);
                }
            } else if (b > 0) {
                if (a < Integer.MIN_VALUE / b) {
                    throw new ArithmeticException("整数乘法溢出: " + a + " * " + b);
                }
            }
            // b == 0 无需检查
        } else if (a > 0) {
            if (a > Integer.MAX_VALUE / b) {
                throw new ArithmeticException("整数乘法溢出: " + a + " * " + b);
            }
        }

        return a * b;
    }

    /**
     * 安全除法，检查除零
     */
    public static int safeDivide(int a, int b) {
        if (b == 0) {
            throw new ArithmeticException("整数除以零: " + a + " / " + b);
        }
        return a / b;
    }

    /**
     * 检查是否合法增加筹码（增加不能为负）
     */
    public static void validateIncrement(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("增加筹码不能为负数: " + amount);
        }
    }

    /**
     * 检查是否合法扣除筹码（扣除不能超过当前筹码）
     */
    public static void validateDecrement(int currentChips, int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("扣除筹码不能为负数: " + amount);
        }
        if (amount > currentChips) {
            throw new IllegalArgumentException(
                "扣除筹码不能超过当前筹码: " + amount + " > " + currentChips
            );
        }
    }
}
```

**Step 4: 运行测试验证通过**

```bash
cd server
./mvnw test -Dtest=ChipCalculatorTest
```

Expected: ✅ 所有测试通过

**Step 5: 提交**

```bash
git add server/src/main/java/com/pocketholdem/util/ChipCalculator.java server/src/test/java/com/pocketholdem/util/ChipCalculatorTest.java
git commit -m "feat: 实现筹码计算工具类ChipCalculator，支持溢出检查"
```

---

## 子冲刺 B：牌型评估逻辑实现（预计6-8小时）

### Task B1: 实现 PokerEngine 框架和基础方法

**Files:**
- Create: `server/src/main/java/com/pocketholdem/engine/PokerEngine.java`
- Test: `server/src/test/java/com/pocketholdem/engine/PokerEngineTest.java`

**Step 1: 写失败的测试**

```java
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
```

**Step 2: 运行测试验证失败**

```bash
cd server
./mvnw test -Dtest=PokerEngineTest
```

Expected: ❌ 找不到类 PokerEngine, EvaluatedHand

**Step 3: 实现最小代码**

```java
package com.pocketholdem.engine;

import com.pocketholdem.model.*;

import java.util.*;

/**
 * 扑克引擎（纯函数工具类）
 * 所有方法都是静态的，无状态
 */
public final class PokerEngine {

    private PokerEngine() {
        // 工具类，禁止实例化
    }

    /**
     * 评估5张牌的牌型
     */
    public static EvaluatedHand evaluateFiveCards(Card[] cards) {
        // 实现基础框架
        return new EvaluatedHand(
            HandRank.HIGH_CARD,
            "高牌",
            Arrays.copyOf(cards, 5),
            new Rank[5],
            0
        );
    }

    /**
     * 从手牌和公共牌中找出最佳5张牌组合
     */
    public static EvaluatedHand evaluateHand(Card[] holeCards, Card[] communityCards) {
        // 实现基础框架
        return evaluateFiveCards(holeCards);
    }

    /**
     * 比较两个牌型
     * 返回正数表示hand1更强，负数表示hand2更强，0表示平局
     */
    public static int compareHands(EvaluatedHand hand1, EvaluatedHand hand2) {
        return Integer.compare(hand1.score(), hand2.score());
    }
}
```

同时需要创建 EvaluatedHand Record：

```java
package com.pocketholdem.model;

/**
 * 牌型评估结果（不可变Record）
 */
public record EvaluatedHand(
        HandRank rank,          // 牌型等级
        String rankName,        // 牌型名称
        Card[] bestCards,       // 最佳5张牌
        Rank[] kickers,         // 踢脚牌
        int score               // 综合得分
) {}
```

**Step 4: 运行测试验证通过**

```bash
cd server
./mvnw test -Dtest=PokerEngineTest
```

Expected: ✅ 测试通过（虽然逻辑还未完整）

**Step 5: 提交**

```bash
git add server/src/main/java/com/pocketholdem/engine/PokerEngine.java server/src/test/java/com/pocketholdem/engine/PokerEngineTest.java server/src/main/java/com/pocketholdem/model/EvaluatedHand.java
git commit -m "feat: 实现PokerEngine基础框架"
```

---

### Task B2: 实现顺子检测（包括轮盘顺A-2-3-4-5）

**Files:**
- Modify: `server/src/main/java/com/pocketholdem/engine/PokerEngine.java`
- Test: `server/src/test/java/com/pocketholdem/engine/PokerEngineTest.java`

**Step 1: 写失败的测试**

```java
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
        Card.of("spades", "nine")  // 缺10
    };

    EvaluatedHand result = PokerEngine.evaluateFiveCards(hand);
    assertNotEquals(HandRank.STRAIGHT, result.rank());
}
```

**Step 2: 运行测试验证失败**

```bash
cd server
./mvnw test -Dtest=PokerEngineTest#shouldRecognizeNormalStraight
```

Expected: ❌ 逻辑未实现

**Step 3: 实现顺子检测**

在 PokerEngine 中添加私有方法：

```java
/**
 * 检测是否为顺子
 * 返回排序后的牌数组，如果不是顺子则返回null
 */
private static Card[] detectStraight(Card[] sortedCards) {
    // 检测常规顺子
    boolean isConsecutive = true;
    for (int i = 0; i < 4; i++) {
        if (sortedCards[i + 1].rank().getValue() - sortedCards[i].rank().getValue() != 1) {
            isConsecutive = false;
            break;
        }
    }

    if (isConsecutive) {
        return sortedCards;
    }

    // 检测轮盘顺（A-2-3-4-5）
    // A=14, 2=2, 3=3, 4=4, 5=5
    if (sortedCards[0].rank() == Rank.ACE &&
        sortedCards[1].rank() == Rank.FIVE &&
        sortedCards[2].rank() == Rank.FOUR &&
        sortedCards[3].rank() == Rank.THREE &&
        sortedCards[4].rank() == Rank.TWO) {

        // 重新排列：5-4-3-2-A（A作为最小牌）
        return new Card[] {
            sortedCards[1], sortedCards[2], sortedCards[3],
            sortedCards[4], sortedCards[0]
        };
    }

    return null;
}
```

更新 evaluateFiveCards 方法：

```java
public static EvaluatedHand evaluateFiveCards(Card[] cards) {
    // 按点数排序（降序）
    Card[] sorted = Arrays.copyOf(cards, 5);
    Arrays.sort(sorted, (a, b) -> Integer.compare(b.rank().getValue(), a.rank().getValue()));

    // 检测顺子
    Card[] straight = detectStraight(sorted);

    // 检测同花
    boolean isFlush = Arrays.stream(cards)
        .map(Card::suit)
        .distinct()
        .count() == 1;

    // 简化的牌型判断（后续步骤会扩展）
    if (isFlush && straight != null) {
        if (straight[0].rank() == Rank.ACE) {
            return createEvaluatedHand(HandRank.ROYAL_FLUSH, "皇家同花顺", straight);
        } else {
            return createEvaluatedHand(HandRank.STRAIGHT_FLUSH, "同花顺", straight);
        }
    }

    if (isFlush) {
        return createEvaluatedHand(HandRank.FLUSH, "同花", sorted);
    }

    if (straight != null) {
        return createEvaluatedHand(HandRank.STRAIGHT, "顺子", straight);
    }

    return createEvaluatedHand(HandRank.HIGH_CARD, "高牌", sorted);
}

private static EvaluatedHand createEvaluatedHand(
        HandRank rank,
        String rankName,
        Card[] bestCards) {

    Rank[] kickers = Arrays.stream(bestCards)
        .map(Card::rank)
        .toArray(Rank[]::new);

    int score = calculateScore(rank, kickers);

    return new EvaluatedHand(rank, rankName, bestCards, kickers, score);
}

private static int calculateScore(HandRank rank, Rank[] kickers) {
    int score = rank.getValue() * 100_000_000;

    for (int i = 0; i < kickers.length && i < 5; i++) {
        score += kickers[i].getValue() * Math.pow(10, 8 - i * 2);
    }

    return score;
}
```

**Step 4: 运行测试验证通过**

```bash
cd server
./mvnw test -Dtest=PokerEngineTest
```

Expected: ✅ 顺子检测测试通过

**Step 5: 提交**

```bash
git add server/src/main/java/com/pocketholdem/engine/PokerEngine.java server/src/test/java/com/pocketholdem/engine/PokerEngineTest.java
git commit -m "feat: 实现顺子检测（包括轮盘顺）"
```

---

### Task B3: 实现牌型分组（四条、三条、对子）

**Files:**
- Modify: `server/src/main/java/com/pocketholdem/engine/PokerEngine.java`
- Test: `server/src/test/java/com/pocketholdem/engine/PokerEngineTest.java`

**Step 1: 写失败的测试**

```java
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
```

**Step 2: 运行测试验证失败**

```bash
cd server
./mvnw test -Dtest=PokerEngineTest#shouldRecognizeFourOfAKind
```

Expected: ❌ 逻辑未实现

**Step 3: 实现牌型分组**

创建分组结果 Record：

```java
package com.pocketholdem.model;

import java.util.List;

/**
 * 牌型分组结果
 */
public record CardGroups(
        List<Card> four,      // 四条
        List<Card> three,     // 三条
        List<Card> pairs,     // 对子（可能有多个）
        List<Card> singles    // 单张
) {}
```

在 PokerEngine 中添加分组方法：

```java
/**
 * 按点数统计牌型分组
 */
private static CardGroups groupCards(Card[] sortedCards) {
    Map<Rank, List<Card>> rankMap = new HashMap<>();

    for (Card card : sortedCards) {
        rankMap.computeIfAbsent(card.rank(), k -> new ArrayList<>()).add(card);
    }

    List<Card> four = new ArrayList<>();
    List<Card> three = new ArrayList<>();
    List<Card> pairs = new ArrayList<>();
    List<Card> singles = new ArrayList<>();

    for (List<Card> group : rankMap.values()) {
        switch (group.size()) {
            case 4:
                four.addAll(group);
                break;
            case 3:
                three.addAll(group);
                break;
            case 2:
                pairs.addAll(group);
                break;
            case 1:
                singles.addAll(group);
                break;
        }
    }

    // 按点数降序排序
    four.sort((a, b) -> Integer.compare(b.rank().getValue(), a.rank().getValue()));
    three.sort((a, b) -> Integer.compare(b.rank().getValue(), a.rank().getValue()));
    pairs.sort((a, b) -> Integer.compare(b.rank().getValue(), a.rank().getValue()));
    singles.sort((a, b) -> Integer.compare(b.rank().getValue(), a.rank().getValue()));

    return new CardGroups(four, three, pairs, singles);
}
```

更新 evaluateFiveCards 方法以支持牌型分组：

```java
public static EvaluatedHand evaluateFiveCards(Card[] cards) {
    Card[] sorted = Arrays.copyOf(cards, 5);
    Arrays.sort(sorted, (a, b) -> Integer.compare(b.rank().getValue(), a.rank().getValue()));

    CardGroups groups = groupCards(sorted);
    Card[] straight = detectStraight(sorted);
    boolean isFlush = Arrays.stream(cards)
        .map(Card::suit)
        .distinct()
        .count() == 1;

    // 按优先级判断牌型
    if (isFlush && straight != null) {
        if (straight[0].rank() == Rank.ACE) {
            return createEvaluatedHand(HandRank.ROYAL_FLUSH, "皇家同花顺", straight);
        } else {
            return createEvaluatedHand(HandRank.STRAIGHT_FLUSH, "同花顺", straight);
        }
    }

    if (!groups.four().isEmpty()) {
        Card[] best = new Card[5];
        System.arraycopy(groups.four().toArray(new Card[0]), 0, best, 0, 4);
        best[4] = groups.singles().get(0);
        return createEvaluatedHand(HandRank.FOUR_OF_A_KIND, "四条", best);
    }

    if (!groups.three().isEmpty() && !groups.pairs().isEmpty()) {
        Card[] best = new Card[5];
        System.arraycopy(groups.three().toArray(new Card[0]), 0, best, 0, 3);
        System.arraycopy(groups.pairs().toArray(new Card[0]), 0, best, 3, 2);
        return createEvaluatedHand(HandRank.FULL_HOUSE, "葫芦", best);
    }

    if (isFlush) {
        return createEvaluatedHand(HandRank.FLUSH, "同花", sorted);
    }

    if (straight != null) {
        return createEvaluatedHand(HandRank.STRAIGHT, "顺子", straight);
    }

    if (!groups.three().isEmpty()) {
        Card[] best = new Card[5];
        System.arraycopy(groups.three().toArray(new Card[0]), 0, best, 0, 3);
        best[3] = groups.singles().get(0);
        best[4] = groups.singles().get(1);
        return createEvaluatedHand(HandRank.THREE_OF_A_KIND, "三条", best);
    }

    if (groups.pairs().size() >= 2) {
        Card[] best = new Card[5];
        System.arraycopy(groups.pairs().toArray(new Card[0]), 0, best, 0, 4);
        best[4] = groups.singles().get(0);
        return createEvaluatedHand(HandRank.TWO_PAIR, "两对", best);
    }

    if (!groups.pairs().isEmpty()) {
        Card[] best = new Card[5];
        System.arraycopy(groups.pairs().toArray(new Card[0]), 0, best, 0, 2);
        best[2] = groups.singles().get(0);
        best[3] = groups.singles().get(1);
        best[4] = groups.singles().get(2);
        return createEvaluatedHand(HandRank.ONE_PAIR, "一对", best);
    }

    return createEvaluatedHand(HandRank.HIGH_CARD, "高牌", sorted);
}
```

**Step 4: 运行测试验证通过**

```bash
cd server
./mvnw test -Dtest=PokerEngineTest
```

Expected: ✅ 牌型分组测试通过

**Step 5: 提交**

```bash
git add server/src/main/java/com/pocketholdem/engine/PokerEngine.java server/src/main/java/com/pocketholdem/model/CardGroups.java server/src/test/java/com/pocketholdem/engine/PokerEngineTest.java
git commit -m "feat: 实现牌型分组（四条、三条、对子、葫芦）"
```

---

### Task B4: 实现从7张牌中找出最佳5张组合

**Files:**
- Modify: `server/src/main/java/com/pocketholdem/engine/PokerEngine.java`
- Test: `server/src/test/java/com/pocketholdem/engine/PokerEngineTest.java`

**Step 1: 写失败的测试**

```java
@Test
@DisplayName("应该从2张手牌+5张公共牌中找出最佳组合")
void shouldFindBestHandFromSevenCards() {
    Card[] holeCards = {
        Card.of("spades", "ace"),
        Card.of("spades", "king")
    };

    Card[] communityCards = {
        Card.of("spades", "queen"),
        Card.of("spades", "jack"),
        Card.of("spades", "ten"),
        Card.of("hearts", "two"),
        Card.of("hearts", "three")
    };

    EvaluatedHand result = PokerEngine.evaluateHand(holeCards, communityCards);

    // A-K-Q-J-10 同花顺
    assertEquals(HandRank.ROYAL_FLUSH, result.rank());
}

@Test
@DisplayName("应该从7张牌中选出最大的同花")
void shouldSelectBestFlushFromSevenCards() {
    Card[] holeCards = {
        Card.of("spades", "ace"),
        Card.of("hearts", "two")
    };

    Card[] communityCards = {
        Card.of("spades", "king"),
        Card.of("spades", "queen"),
        Card.of("spades", "jack"),
        Card.of("spades", "ten"),
        Card.of("spades", "nine")
    };

    EvaluatedHand result = PokerEngine.evaluateHand(holeCards, communityCards);

    // A-K-Q-J-10 同花顺
    assertEquals(HandRank.STRAIGHT_FLUSH, result.rank());
}
```

**Step 2: 运行测试验证失败**

```bash
cd server
./mvnw test -Dtest=PokerEngineTest#shouldFindBestHandFromSevenCards
```

Expected: ❌ 逻辑未实现

**Step 3: 实现组合生成和最佳选择**

```java
/**
 * 从7张牌中选出最佳5张牌组合
 */
public static EvaluatedHand evaluateHand(Card[] holeCards, Card[] communityCards) {
    // 合并所有牌
    Card[] allCards = new Card[holeCards.length + communityCards.length];
    System.arraycopy(holeCards, 0, allCards, 0, holeCards.length);
    System.arraycopy(communityCards, 0, allCards, holeCards.length, communityCards.length);

    // 生成所有C(7,5)=21种组合
    List<Card[]> combinations = getCombinations(allCards, 5);

    // 评估每个组合，选择得分最高的
    EvaluatedHand bestHand = null;
    for (Card[] combo : combinations) {
        EvaluatedHand evaluated = evaluateFiveCards(combo);
        if (bestHand == null || evaluated.score() > bestHand.score()) {
            bestHand = evaluated;
        }
    }

    return bestHand;
}

/**
 * 生成从n张牌中选k张的所有组合
 */
private static List<Card[]> getCombinations(Card[] cards, int k) {
    List<Card[]> result = new ArrayList<>();
    Card[] current = new Card[k];
    combine(cards, k, 0, 0, current, result);
    return result;
}

/**
 * 递归生成组合
 */
private static void combine(
        Card[] cards,
        int k,
        int start,
        int index,
        Card[] current,
        List<Card[]> result) {

    if (index == k) {
        result.add(Arrays.copyOf(current, k));
        return;
    }

    for (int i = start; i < cards.length; i++) {
        current[index] = cards[i];
        combine(cards, k, i + 1, index + 1, current, result);
    }
}
```

**Step 4: 运行测试验证通过**

```bash
cd server
./mvnw test -Dtest=PokerEngineTest
```

Expected: ✅ 最佳组合选择测试通过

**Step 5: 提交**

```bash
git add server/src/main/java/com/pocketholdem/engine/PokerEngine.java server/src/test/java/com/pocketholdem/engine/PokerEngineTest.java
git commit -m "feat: 实现从7张牌中找出最佳5张组合"
```

---

### Task B5: 实现完整的比牌逻辑

**Files:**
- Modify: `server/src/main/java/com/pocketholdem/engine/PokerEngine.java`
- Test: `server/src/test/java/com/pocketholdem/engine/PokerEngineTest.java`

**Step 1: 写失败的测试**

```java
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
```

**Step 2: 运行测试验证失败**

```bash
cd server
./mvnw test -Dtest=PokerEngineTest#royalFlushShouldBeatFourOfAKind
```

Expected: ❌ 可能通过（取决于之前的实现）

**Step 3: 优化得分计算方法**

```java
/**
 * 计算牌型综合得分
 * 牌型权重：10^10级别
 * 踢脚牌权重：按位置递减（10^8, 10^6, 10^4, 10^2, 10^0）
 */
private static int calculateScore(HandRank rank, Rank[] kickers) {
    int score = rank.getValue() * 100_000_000;

    for (int i = 0; i < kickers.length && i < 5; i++) {
        int kickerValue = kickers[i].getValue();
        int weight = (int) Math.pow(10, 8 - i * 2);
        score += kickerValue * weight;
    }

    return score;
}

/**
 * 比较两个牌型
 * 返回正数表示hand1更强，负数表示hand2更强，0表示平局
 */
public static int compareHands(EvaluatedHand hand1, EvaluatedHand hand2) {
    // 先比较得分
    if (hand1.score() != hand2.score()) {
        return Integer.compare(hand1.score(), hand2.score());
    }

    // 得分相同，逐个比较踢脚牌
    Rank[] kickers1 = hand1.kickers();
    Rank[] kickers2 = hand2.kickers();

    for (int i = 0; i < Math.min(kickers1.length, kickers2.length); i++) {
        if (kickers1[i].getValue() != kickers2[i].getValue()) {
            return Integer.compare(kickers1[i].getValue(), kickers2[i].getValue());
        }
    }

    return 0; // 完全平局
}
```

**Step 4: 运行测试验证通过**

```bash
cd server
./mvnw test -Dtest=PokerEngineTest
```

Expected: ✅ 比牌测试通过

**Step 5: 提交**

```bash
git add server/src/main/java/com/pocketholdem/engine/PokerEngine.java server/src/test/java/com/pocketholdem/engine/PokerEngineTest.java
git commit -m "feat: 实现完整的比牌逻辑"
```

---

## 子冲刺 C：底池与边池算法实现（预计5-6小时）

### Task C1: 实现底池数据结构 Pot

**Files:**
- Create: `server/src/main/java/com/pocketholdem/model/Pot.java`
- Test: `server/src/test/java/com/pocketholdem/model/PotTest.java`

**Step 1: 写失败的测试**

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("底池数据结构测试")
class PotTest {

    @Test
    @DisplayName("应该创建底池")
    void shouldCreatePot() {
        String[] eligiblePlayers = {"player1", "player2", "player3"};
        Pot pot = new Pot(1000, eligiblePlayers);

        assertEquals(1000, pot.amount());
        assertEquals(3, pot.eligiblePlayerIds().length);
    }

    @Test
    @DisplayName("应该序列化为snake_case")
    void shouldSerializeToSnakeCase() {
        String[] eligiblePlayers = {"player1"};
        Pot pot = new Pot(1000, eligiblePlayers);
        String json = new ObjectMapper().writeValueAsString(pot);
        assertTrue(json.contains("\"amount\""));
        assertTrue(json.contains("\"eligible_player_ids\""));
    }
}
```

**Step 2: 运行测试验证失败**

```bash
cd server
./mvnw test -Dtest=PotTest
```

Expected: ❌ 找不到类 Pot

**Step 3: 实现最小代码**

```java
package com.pocketholdem.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 底池信息（不可变Record）
 * 参考：numeric-spec.md 第6章
 */
public record Pot(
        @JsonProperty("amount") int amount,
        @JsonProperty("eligible_player_ids") String[] eligiblePlayerIds
) {}
```

**Step 4: 运行测试验证通过**

```bash
cd server
./mvnw test -Dtest=PotTest
```

Expected: ✅ 所有测试通过

**Step 5: 提交**

```bash
git add server/src/main/java/com/pocketholdem/model/Pot.java server/src/test/java/com/pocketholdem/model/PotTest.java
git commit -m "feat: 实现底池数据结构Pot"
```

---

### Task C2: 实现边池计算核心算法

**Files:**
- Create: `server/src/main/java/com/pocketholdem/engine/PotCalculator.java`
- Test: `server/src/test/java/com/pocketholdem/engine/PotCalculatorTest.java`

**Step 1: 写失败的测试**

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("边池计算测试")
class PotCalculatorTest {

    @Test
    @DisplayName("单一All-in应该只创建主池")
    void singleAllInShouldCreateOnlyMainPot() {
        String[] playerIds = {"player1", "player2", "player3"};
        int[] bets = {100, 100, 100};  // 所有人都下注相同

        Pot[] pots = PotCalculator.calculateSidePots(playerIds, bets);

        assertEquals(1, pots.length);
        assertEquals(300, pots[0].amount());
        assertArrayEquals(playerIds, pots[0].eligiblePlayerIds());
    }

    @Test
    @DisplayName("两个不同All-in应该创建主池和一个边池")
    void twoDifferentAllInsShouldCreateMainAndSidePot() {
        String[] playerIds = {"player1", "player2", "player3"};
        int[] bets = {50, 100, 100};  // player1 All-in 50

        Pot[] pots = PotCalculator.calculateSidePots(playerIds, bets);

        assertEquals(2, pots.length);
        assertEquals(150, pots[0].amount());  // 主池：50 * 3
        assertEquals(100, pots[1].amount());  // 边池：50 * 2
        assertArrayEquals(playerIds, pots[0].eligiblePlayerIds());
        assertArrayEquals(new String[]{"player2", "player3"}, pots[1].eligiblePlayerIds());
    }

    @Test
    @DisplayName("三个不同All-in应该创建主池和两个边池")
    void threeDifferentAllInsShouldCreateMainAndTwoSidePots() {
        String[] playerIds = {"player1", "player2", "player3"};
        int[] bets = {30, 50, 100};  // 三个不同的All-in

        Pot[] pots = PotCalculator.calculateSidePots(playerIds, bets);

        assertEquals(3, pots.length);
        assertEquals(90, pots[0].amount());   // 主池：30 * 3
        assertEquals(100, pots[1].amount());  // 第一个边池：20 * 2
        assertEquals(100, pots[2].amount());  // 第二个边池：50 * 1
    }
}
```

**Step 2: 运行测试验证失败**

```bash
cd server
./mvnw test -Dtest=PotCalculatorTest
```

Expected: ❌ 找不到类 PotCalculator

**Step 3: 实现最小代码**

```java
package com.pocketholdem.engine;

import com.pocketholdem.model.Pot;
import com.pocketholdem.util.ChipCalculator;

import java.util.*;

/**
 * 底池计算器（纯函数工具类）
 * 参考：docs/numeric-spec.md 第6章
 */
public final class PotCalculator {

    private PotCalculator() {
        // 工具类，禁止实例化
    }

    /**
     * 计算边池
     * 按照玩家投入筹码从小到大排序，逐层创建边池
     *
     * @param playerIds 玩家ID数组（与bets数组一一对应）
     * @param bets 每个玩家投入的筹码
     * @return 底池列表（主池 + 边池）
     */
    public static Pot[] calculateSidePots(String[] playerIds, int[] bets) {
        if (playerIds.length != bets.length) {
            throw new IllegalArgumentException(
                "玩家ID和下注数组长度不一致"
            );
        }

        if (playerIds.length == 0) {
            return new Pot[0];
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

                // 计算底池金额
                int potAmount = ChipCalculator.safeMultiply(betDiff, contributorsCount);

                // 确定有资格的玩家（当前及之后的玩家）
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
```

**Step 4: 运行测试验证通过**

```bash
cd server
./mvnw test -Dtest=PotCalculatorTest
```

Expected: ✅ 边池计算测试通过

**Step 5: 提交**

```bash
git add server/src/main/java/com/pocketholdem/engine/PotCalculator.java server/src/test/java/com/pocketholdem/engine/PotCalculatorTest.java
git commit -m "feat: 实现边池计算核心算法"
```

---

### Task C3: 实现底池分配逻辑

**Files:**
- Modify: `server/src/main/java/com/pocketholdem/engine/PotCalculator.java`
- Test: `server/src/test/java/com/pocketholdem/engine/PotCalculatorTest.java`

**Step 1: 写失败的测试**

```java
@Test
@DisplayName("应该将底池分配给单个获胜者")
void shouldAwardPotToSingleWinner() {
    Pot[] pots = {
        new Pot(300, new String[]{"player1", "player2", "player3"})
    };

    Map<String, EvaluatedHand> handRanks = new HashMap<>();
    handRanks.put("player1",
        PokerEngine.evaluateFiveCards(new Card[]{
            Card.of("spades", "ace"), Card.of("hearts", "king"),
            Card.of("diamonds", "queen"), Card.of("clubs", "jack"),
            Card.of("spades", "ten")
        }));
    handRanks.put("player2",
        PokerEngine.evaluateFiveCards(new Card[]{
            Card.of("hearts", "two"), Card.of("hearts", "three"),
            Card.of("hearts", "four"), Card.of("hearts", "five"),
            Card.of("hearts", "six")
        }));
    handRanks.put("player3",
        PokerEngine.evaluateFiveCards(new Card[]{
            Card.of("clubs", "seven"), Card.of("clubs", "eight"),
            Card.of("clubs", "nine"), Card.of("clubs", "ten"),
            Card.of("clubs", "jack")
        }));

    Map<String, Integer> winnings = PotCalculator.awardPots(pots, handRanks);

    assertEquals(300, winnings.get("player1"));
    assertEquals(0, winnings.get("player2"));
    assertEquals(0, winnings.get("player3"));
}

@Test
@DisplayName("应该在多个获胜者之间平分底池")
void shouldSplitPotAmongMultipleWinners() {
    Pot[] pots = {
        new Pot(300, new String[]{"player1", "player2", "player3"})
    };

    // 创建三个相同的牌（高牌 A-K-Q-J-9）
    Card[] sameCards = {
        Card.of("spades", "ace"), Card.of("hearts", "king"),
        Card.of("diamonds", "queen"), Card.of("clubs", "jack"),
        Card.of("spades", "nine")
    };

    Map<String, EvaluatedHand> handRanks = new HashMap<>();
    handRanks.put("player1", PokerEngine.evaluateFiveCards(sameCards));
    handRanks.put("player2", PokerEngine.evaluateFiveCards(sameCards));
    handRanks.put("player3", PokerEngine.evaluateFiveCards(sameCards));

    Map<String, Integer> winnings = PotCalculator.awardPots(pots, handRanks);

    assertEquals(100, winnings.get("player1"));
    assertEquals(100, winnings.get("player2"));
    assertEquals(100, winnings.get("player3"));
}

@Test
@DisplayName("余数应该按座位顺序分配")
void shouldDistributeRemainderBySeatOrder() {
    Pot[] pots = {
        new Pot(301, new String[]{"player1", "player2", "player3"})
    };

    // 三个相同的牌
    Card[] sameCards = {
        Card.of("spades", "ace"), Card.of("hearts", "king"),
        Card.of("diamonds", "queen"), Card.of("clubs", "jack"),
        Card.of("spades", "nine")
    };

    Map<String, EvaluatedHand> handRanks = new HashMap<>();
    handRanks.put("player1", PokerEngine.evaluateFiveCards(sameCards));
    handRanks.put("player2", PokerEngine.evaluateFiveCards(sameCards));
    handRanks.put("player3", PokerEngine.evaluateFiveCards(sameCards));

    Map<String, Integer> winnings = PotCalculator.awardPots(pots, handRanks);

    // 301 / 3 = 100 余 1，余数给player1
    assertEquals(101, winnings.get("player1"));
    assertEquals(100, winnings.get("player2"));
    assertEquals(100, winnings.get("player3"));
}

@Test
@DisplayName("边池应该分配给有资格的玩家")
void shouldAwardSidePotToEligiblePlayers() {
    Pot[] pots = {
        new Pot(150, new String[]{"player1", "player2", "player3"}),  // 主池
        new Pot(100, new String[]{"player2", "player3"})             // 边池
    };

    Map<String, EvaluatedHand> handRanks = new HashMap<>();
    handRanks.put("player1",
        PokerEngine.evaluateFiveCards(new Card[]{
            Card.of("spades", "two"), Card.of("hearts", "three"),
            Card.of("diamonds", "four"), Card.of("clubs", "five"),
            Card.of("spades", "six")
        }));
    handRanks.put("player2",
        PokerEngine.evaluateFiveCards(new Card[]{
            Card.of("spades", "ace"), Card.of("hearts", "king"),
            Card.of("diamonds", "queen"), Card.of("clubs", "jack"),
            Card.of("spades", "ten")
        }));
    handRanks.put("player3",
        PokerEngine.evaluateFiveCards(new Card[]{
            Card.of("hearts", "ace"), Card.of("diamonds", "king"),
            Card.of("clubs", "queen"), Card.of("spades", "jack"),
            Card.of("hearts", "nine")
        }));

    Map<String, Integer> winnings = PotCalculator.awardPots(pots, handRanks);

    // player2赢得主池和边池
    assertEquals(0, winnings.get("player1"));
    assertEquals(250, winnings.get("player2"));
    assertEquals(0, winnings.get("player3"));
}
```

**Step 2: 运行测试验证失败**

```bash
cd server
./mvnw test -Dtest=PotCalculatorTest#shouldAwardPotToSingleWinner
```

Expected: ❌ 逻辑未实现

**Step 3: 实现底池分配**

```java
/**
 * 分配底池给获胜玩家
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

    // 逐个底池分配
    for (Pot pot : pots) {
        // 找出此底池的最高分玩家
        List<String> winners = findWinners(pot, handRanks);

        // 计算每个获胜者应得的筹码
        distributePot(pot, winners, winnings);
    }

    return winnings;
}

/**
 * 找出某个底池的最高分玩家
 */
private static List<String> findWinners(
        Pot pot,
        Map<String, EvaluatedHand> handRanks) {

    String[] eligiblePlayers = pot.eligiblePlayerIds();

    if (eligiblePlayers.length == 0) {
        return Collections.emptyList();
    }

    List<String> winners = new ArrayList<>();
    int highestScore = Integer.MIN_VALUE;

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
 */
private static void distributePot(
        Pot pot,
        List<String> winners,
        Map<String, Integer> winnings) {

    if (winners.isEmpty()) {
        return;
    }

    int potAmount = pot.amount();
    int winnerCount = winners.size();

    // 计算平均分配
    int share = ChipCalculator.safeDivide(potAmount, winnerCount);
    int remainder = ChipCalculator.safeSubtract(potAmount, ChipCalculator.safeMultiply(share, winnerCount));

    // 分配平均份额
    for (String winner : winners) {
        int currentWinnings = winnings.getOrDefault(winner, 0);
        winnings.put(winner, ChipCalculator.safeAdd(currentWinnings, share));
    }

    // 余数按座位顺序分配（第一个玩家多拿余数）
    if (remainder > 0 && !winners.isEmpty()) {
        String firstWinner = winners.get(0);
        int currentWinnings = winnings.get(firstWinner);
        winnings.put(firstWinner, ChipCalculator.safeAdd(currentWinnings, remainder));
    }
}
```

**Step 4: 运行测试验证通过**

```bash
cd server
./mvnw test -Dtest=PotCalculatorTest
```

Expected: ✅ 底池分配测试通过

**Step 5: 提交**

```bash
git add server/src/main/java/com/pocketholdem/engine/PotCalculator.java server/src/test/java/com/pocketholdem/engine/PotCalculatorTest.java
git commit -m "feat: 实现底池分配逻辑（支持平分和余数处理）"
```

---

## 最终验证步骤

### Task V1: 运行完整测试套件并检查覆盖率

**Step 1: 运行所有测试**

```bash
cd server
./mvnw test
```

Expected: ✅ 所有测试通过（至少35个测试方法）

**Step 2: 生成覆盖率报告**

```bash
cd server
./mvnw clean test jacoco:report
```

Expected: ✅ 生成 JaCoCo HTML 报告

**Step 3: 验证覆盖率**

打开 `server/target/site/jacoco/index.html`

Expected:
- PokerEngine 覆盖率 >85%
- PotCalculator 覆盖率 >85%
- 总体覆盖率 >80%

**Step 4: 编译检查**

```bash
cd server
./mvnw compile
```

Expected: ✅ 编译成功，无警告

**Step 5: 提交最终代码**

```bash
git add server/
git commit -m "test: 完成阶段1核心引擎开发，通过所有测试，覆盖率达标"
```

---

## 验收标准检查清单

- [ ] 所有35+个测试用例通过
- [ ] PokerEngine 覆盖率 >85%
- [ ] PotCalculator 覆盖率 >85%
- [ ] 总体覆盖率 >80%
- [ ] 所有筹码计算使用 ChipCalculator 且包含溢出检查
- [ ] 所有 Record 类使用 @JsonProperty 指定 snake_case
- [ ] 枚举使用 @JsonValue 输出英文值
- [ ] 代码注释使用简体中文
- [ ] 遵循 TDD 流程（测试先行）
- [ ] 更新 docs/CHANGELOG.md

---

## 参考文档

- `/Users/Hana/Codes/pocket-holdem-mvp/server/src/PokerEngine.ts` - 原TypeScript实现
- `docs/numeric-spec.md` - 数值计算规范
- `docs/dto-design-spec.md` - DTO设计规范
- `docs/concurrency-model.md` - 并发模型规范
- `docs/test-strategy.md` - 测试策略规范
