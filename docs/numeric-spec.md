# 数值计算规范

> 文档版本: v1.0
> 创建日期: 2026-01-23
> 作者: AI 助手

---

## 📋 概述

本文档定义了项目中所有数值计算的规范，特别是筹码计算的最小单位、边界条件、溢出处理策略和未来扩展接口。这些规范确保数值计算的准确性、安全性和可扩展性。

---

## 1. 筹码最小单位

### 1.1 定义

**最小筹码单位**：**1元/筹码**

- 筹码计算时不能出现小数
- 所有筹码操作基于整数计算
- 不支持小数筹码（如0.5筹码）

### 1.2 代码示例

```java
// ✅ 正确：整数筹码
player.addChips(100);
player.deductChips(50);
player.bet(25);  // 下注25筹码

// ❌ 错误：小数筹码（不支持）
player.addChips(0.5);  // 编译错误（类型不匹配）
player.bet(10.5);      // 编译错误
```

### 1.3 数据类型选择

```java
// ✅ 正确：使用int类型存储筹码
public class Player {
    private int chips;  // 筹码数量

    public void addChips(int amount) {
        this.chips += amount;
    }

    public void deductChips(int amount) {
        this.chips -= amount;
    }
}

// ❌ 错误：使用double或float存储筹码
public class Player {
    private double chips;  // 错误：会出现精度问题
}
```

**原因**：
- `int` 类型足够存储游戏中的筹码数量
- 避免浮点数精度问题（如 `0.1 + 0.2 != 0.3`）
- 计算速度快，内存占用小

---

## 2. 筹码最大值边界

### 2.1 最大值估算

| 项目 | 数值 | 说明 |
|------|------|------|
| **初始筹码** | 1000 | 每位玩家初始筹码 |
| **最大玩家数** | 9 | 每个房间最多9人 |
| **单局最大筹码** | 1000 * 9 = 9000 | 所有玩家All-in的情况 |
| **边池累积** | 9000 * 10 = 90000 | 考虑10个边池 |
| **int范围** | ±2,147,483,647 | 约21亿 |

**结论**：使用 `int` 类型足够，但需要溢出检查。

### 2.2 溢出场景分析

```java
// 场景1：加法溢出
int a = 2_000_000_000;
int b = 1_000_000_000;
int result = a + b;  // 溢出！实际值为 -1,294,967,296

// 场景2：减法溢出
int a = -2_000_000_000;
int b = 1_000_000_000;
int result = a - b;  // 溢出！实际值为 1,294,967,296

// 场景3：乘法溢出
int a = 1_000_000_000;
int b = 3;
int result = a * b;  // 溢出！实际值为 -1,294,967,296
```

### 2.3 溢出检测工具类

```java
package com.pocketholdem.util;

import lombok.extern.slf4j.Slf4j;

/**
 * 筹码计算工具类（带溢出检查）
 */
@Slf4j
public class ChipCalculator {

    /**
     * 安全加法（带溢出检查）
     */
    public static int safeAdd(int a, int b) {
        long result = (long) a + (long) b;
        if (result > Integer.MAX_VALUE || result < Integer.MIN_VALUE) {
            throw new ArithmeticException(
                String.format("加法溢出: %d + %d = %d (超出int范围)", a, b, result)
            );
        }
        return (int) result;
    }

    /**
     * 安全减法（带溢出检查）
     */
    public static int safeSubtract(int a, int b) {
        long result = (long) a - (long) b;
        if (result > Integer.MAX_VALUE || result < Integer.MIN_VALUE) {
            throw new ArithmeticException(
                String.format("减法溢出: %d - %d = %d (超出int范围)", a, b, result)
            );
        }
        return (int) result;
    }

    /**
     * 安全乘法（带溢出检查）
     */
    public static int safeMultiply(int a, int b) {
        long result = (long) a * (long) b;
        if (result > Integer.MAX_VALUE || result < Integer.MIN_VALUE) {
            throw new ArithmeticException(
                String.format("乘法溢出: %d * %d = %d (超出int范围)", a, b, result)
            );
        }
        return (int) result;
    }

    /**
     * 安全除法（检查除零和溢出）
     */
    public static int safeDivide(int a, int b) {
        // 检查1：除零
        if (b == 0) {
            throw new ArithmeticException("除零错误");
        }

        // 检查2：除法溢出（Integer.MIN_VALUE / -1 = Integer.MIN_VALUE，实际上溢出了）
        if (a == Integer.MIN_VALUE && b == -1) {
            throw new ArithmeticException(
                String.format("除法溢出: %d / %d 超出int范围", a, b)
            );
        }

        return a / b;
    }

    /**
     * 安全取模（检查除零）
     */
    public static int safeModulo(int a, int b) {
        // 检查1：除零
        if (b == 0) {
            throw new ArithmeticException("除零错误");
        }

        return a % b;
    }

    /**
     * 安全幂运算（用于赔率计算）
     */
    public static int safePower(int base, int exponent) {
        if (exponent < 0) {
            throw new IllegalArgumentException("指数不能为负数: " + exponent);
        }

        long result = 1;
        for (int i = 0; i < exponent; i++) {
            result *= base;
            if (result > Integer.MAX_VALUE || result < Integer.MIN_VALUE) {
                throw new ArithmeticException(
                    String.format("幂运算溢出: %d^%d = %d 超出int范围", base, exponent, result)
                );
            }
        }

        return (int) result;
    }
}
```

---

## 3. 筹码增减操作的合法性检查

### 3.1 增加筹码

**规则**：
- 增加筹码不能为负数
- 增加筹码后不能溢出

```java
public class Player {
    private int chips;

    /**
     * 增加筹码（带合法性检查）
     */
    public void addChips(int amount) {
        // 检查1：不能为负数
        if (amount < 0) {
            throw new IllegalArgumentException(
                String.format("增加筹码数量不能为负数: %d", amount)
            );
        }

        // 检查2：溢出检查
        long newChips = (long) this.chips + (long) amount;
        if (newChips > Integer.MAX_VALUE) {
            throw new ArithmeticException(
                String.format("筹码溢出: %d + %d > %d", chips, amount, Integer.MAX_VALUE)
            );
        }

        this.chips = (int) newChips;
    }
}
```

### 3.2 扣除筹码

**规则**：
- 扣除筹码不能为负数
- 扣除筹码不能超过当前筹码

```java
public class Player {
    private int chips;

    /**
     * 扣除筹码（带合法性检查）
     */
    public void deductChips(int amount) {
        // 检查1：不能为负数
        if (amount < 0) {
            throw new IllegalArgumentException(
                String.format("扣除筹码数量不能为负数: %d", amount)
            );
        }

        // 检查2：不能超过当前筹码
        if (amount > chips) {
            throw new IllegalArgumentException(
                String.format("筹码不足: 尝试扣除%d，但只有%d", amount, chips)
            );
        }

        this.chips -= amount;
    }
}
```

### 3.3 下注操作

```java
public class Player {
    private int chips;
    private int currentBet;  // 当前回合已下注

    /**
     * 下注（带合法性检查）
     */
    public void bet(int amount) {
        // 检查1：下注金额必须为正数
        if (amount <= 0) {
            throw new IllegalArgumentException(
                String.format("下注金额必须为正数: %d", amount)
            );
        }

        // 检查2：筹码是否足够
        if (amount > chips) {
            throw new IllegalArgumentException(
                String.format("筹码不足: 尝试下注%d，但只有%d", amount, chips)
            );
        }

        // 扣除筹码
        chips -= amount;
        currentBet += amount;
    }

    /**
     * All-in（全押）
     */
    public int allIn() {
        int amount = chips;
        currentBet += amount;
        chips = 0;
        return amount;
    }
}
```

---

## 4. 溢出时的处理策略

### 4.1 策略定义

**策略**：
1. **立即抛出异常**：检测到溢出时立即抛出 `ArithmeticException`
2. **记录错误日志**：使用 `log.error()` 记录详细的错误信息
3. **返回错误给客户端**：捕获异常，返回 `ErrorResponse` 给客户端
4. **不静默处理**：绝对不能静默处理溢出（如取最大值、循环回到最小值）

### 4.2 实现示例

```java
package com.pocketholdem.service;

import com.pocketholdem.dto.response.ErrorResponse;
import com.pocketholdem.exception.ErrorCodes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 游戏服务（溢出处理）
 */
@Slf4j
@Service
public class GameService {

    /**
     * 处理筹码增加操作（带溢出处理）
     */
    public void addChips(String playerId, int amount) {
        try {
            Player player = findPlayerById(playerId);
            player.addChips(amount);
        } catch (ArithmeticException e) {
            // 记录错误日志
            log.error("筹码溢出: 玩家[{}]增加筹码{}失败", playerId, amount, e);

            // 抛出业务异常
            throw new BusinessException(
                ErrorCodes.CHIP_OVERFLOW,
                "筹码数量溢出，请联系管理员",
                Map.of("playerId", playerId, "amount", amount)
            );
        }
    }
}
```

### 4.3 避免的反模式

```java
// ❌ 错误：静默处理溢出
public void addChips(int amount) {
    try {
        this.chips += amount;
    } catch (ArithmeticException e) {
        this.chips = Integer.MAX_VALUE;  // 错误：静默处理
    }
}

// ❌ 错误：忽略溢出
public void addChips(int amount) {
    this.chips += amount;  // 溢出后变成负数或错误值
}

// ❌ 错误：使用try-catch捕获但不处理
public void addChips(int amount) {
    try {
        this.chips += amount;
    } catch (ArithmeticException e) {
        // 什么都不做，程序继续运行
    }
}
```

---

## 5. 未来扩展接口

### 5.1 接口定义

```java
package com.pocketholdem.model;

import java.math.BigDecimal;

/**
 * 筹码数量接口（支持未来扩展）
 */
public interface ChipAmount {
    /**
     * 转换为int
     */
    int toInt();

    /**
     * 转换为long
     */
    long toLong();

    /**
     * 转换为BigDecimal（用于支持小数筹码）
     */
    BigDecimal toBigDecimal();

    /**
     * 加法
     */
    ChipAmount add(ChipAmount other);

    /**
     * 减法
     */
    ChipAmount subtract(ChipAmount other);

    /**
     * 乘法
     */
    ChipAmount multiply(int factor);

    /**
     * 比较
     */
    int compareTo(ChipAmount other);
}
```

### 5.2 当前实现

```java
package com.pocketholdem.model.impl;

import com.pocketholdem.model.ChipAmount;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * 整数筹码实现（当前版本）
 */
public class IntegerChips implements ChipAmount {
    private final int amount;

    public IntegerChips(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("筹码不能为负数: " + amount);
        }
        this.amount = amount;
    }

    @Override
    public int toInt() {
        return amount;
    }

    @Override
    public long toLong() {
        return amount;
    }

    @Override
    public BigDecimal toBigDecimal() {
        return BigDecimal.valueOf(amount);
    }

    @Override
    public ChipAmount add(ChipAmount other) {
        long result = this.amount + other.toInt();
        if (result > Integer.MAX_VALUE) {
            throw new ArithmeticException("筹码溢出: " + result);
        }
        return new IntegerChips((int) result);
    }

    @Override
    public ChipAmount subtract(ChipAmount other) {
        long result = this.amount - other.toInt();
        if (result < 0) {
            throw new ArithmeticException("筹码不足: " + this.amount + " - " + other.toInt());
        }
        return new IntegerChips((int) result);
    }

    @Override
    public ChipAmount multiply(int factor) {
        if (factor < 0) {
            throw new IllegalArgumentException("乘数不能为负数: " + factor);
        }
        long result = (long) this.amount * factor;
        if (result > Integer.MAX_VALUE) {
            throw new ArithmeticException("筹码溢出: " + result);
        }
        return new IntegerChips((int) result);
    }

    @Override
    public int compareTo(ChipAmount other) {
        return Integer.compare(this.amount, other.toInt());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntegerChips that = (IntegerChips) o;
        return amount == that.amount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount);
    }

    @Override
    public String toString() {
        return String.valueOf(amount);
    }
}
```

### 5.3 未来扩展：小数筹码实现

```java
package com.pocketholdem.model.impl;

import com.pocketholdem.model.ChipAmount;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 小数筹码实现（未来版本）
 * 支持最小单位0.01元
 */
public class DecimalChips implements ChipAmount {
    private static final BigDecimal MIN_UNIT = new BigDecimal("0.01");
    private final BigDecimal amount;

    public DecimalChips(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("筹码不能为负数: " + amount);
        }
        // 保留2位小数
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public int toInt() {
        return amount.intValue();
    }

    @Override
    public long toLong() {
        return amount.longValue();
    }

    @Override
    public BigDecimal toBigDecimal() {
        return amount;
    }

    @Override
    public ChipAmount add(ChipAmount other) {
        BigDecimal result = this.amount.add(other.toBigDecimal());
        return new DecimalChips(result);
    }

    @Override
    public ChipAmount subtract(ChipAmount other) {
        BigDecimal result = this.amount.subtract(other.toBigDecimal());
        return new DecimalChips(result);
    }

    @Override
    public ChipAmount multiply(int factor) {
        if (factor < 0) {
            throw new IllegalArgumentException("乘数不能为负数: " + factor);
        }
        BigDecimal result = this.amount.multiply(BigDecimal.valueOf(factor));
        return new DecimalChips(result);
    }

    @Override
    public int compareTo(ChipAmount other) {
        return this.amount.compareTo(other.toBigDecimal());
    }

    // equals, hashCode, toString 省略...
}
```

### 5.4 迁移策略

```java
/**
 * 筹码工厂类（用于迁移）
 */
public class ChipAmountFactory {
    private static final boolean USE_DECIMAL = false;  // 配置开关

    public static ChipAmount fromInt(int amount) {
        if (USE_DECIMAL) {
            return new DecimalChips(BigDecimal.valueOf(amount));
        } else {
            return new IntegerChips(amount);
        }
    }

    public static ChipAmount fromString(String amount) {
        if (USE_DECIMAL) {
            return new DecimalChips(new BigDecimal(amount));
        } else {
            return new IntegerChips(Integer.parseInt(amount));
        }
    }
}
```

---

## 6. 底池计算规范

### 6.1 底池类型

```java
package com.pocketholdem.model;

import lombok.Data;
import lombok.Builder;

/**
 * 底池
 */
@Data
@Builder
public class Pot {
    private int amount;           // 底池总筹码
    private List<String> playerIds;  // 参与此底池的玩家ID
    private int maxBet;           // 此底池的最大下注额
}

/**
 * 边池（当玩家All-in时创建）
 */
@Data
@Builder
public class SidePot extends Pot {
    private int parentPotId;      // 父底池ID
}
```

### 6.2 边池计算规则

```java
package com.pocketholdem.engine;

import com.pocketholdem.model.Player;
import com.pocketholdem.model.Pot;
import com.pocketholdem.model.SidePot;
import java.util.*;

/**
 * 边池计算引擎
 */
public class PotCalculator {

    /**
     * 计算边池
     *
     * 规则：
     * 1. 所有All-in的玩家按筹码数量排序
     * 2. 从小到大依次创建边池
     * 3. 每个边池包含当前All-in玩家和筹码更多的玩家
     */
    public List<Pot> calculateSidePots(List<Player> players) {
        // 1. 找出所有All-in的玩家
        List<Player> allInPlayers = players.stream()
            .filter(p -> p.getChips() == 0)
            .sorted(Comparator.comparingInt(Player::getTotalBet))
            .toList();

        if (allInPlayers.isEmpty()) {
            // 没有All-in，只有一个主池
            Pot mainPot = createMainPot(players);
            return List.of(mainPot);
        }

        // 2. 创建边池
        List<Pot> pots = new ArrayList<>();
        int previousBet = 0;

        for (Player allInPlayer : allInPlayers) {
            int currentBet = allInPlayer.getTotalBet();
            int potAmount = calculatePotAmount(players, previousBet, currentBet);

            // 创建边池
            SidePot sidePot = SidePot.builder()
                .amount(potAmount)
                .playerIds(getEligiblePlayers(players, currentBet))
                .maxBet(currentBet)
                .parentPotId(pots.isEmpty() ? 0 : pots.get(pots.size() - 1).getPotId())
                .build();

            pots.add(sidePot);
            previousBet = currentBet;
        }

        // 3. 创建主池（剩余筹码）
        if (hasRemainingPlayers(players, previousBet)) {
            Pot mainPot = Pot.builder()
                .amount(calculateMainPotAmount(players, previousBet))
                .playerIds(getRemainingPlayers(players, previousBet))
                .maxBet(getMaxBet(players))
                .build();

            pots.add(mainPot);
        }

        return pots;
    }

    /**
     * 计算单个边池的筹码数量
     */
    private int calculatePotAmount(List<Player> players, int fromBet, int toBet) {
        int amount = 0;
        for (Player player : players) {
            int contribution = Math.min(player.getTotalBet(), toBet) - fromBet;
            amount += contribution;
        }
        return amount;
    }
}
```

### 6.3 边池分配规则

```java
package com.pocketholdem.engine;

import com.pocketholdem.model.Player;
import com.pocketholdem.model.Pot;
import java.util.*;

/**
 * 底池分配引擎
 */
public class PotDistributor {

    /**
     * 分配底池筹码给获胜玩家
     *
     * 规则：
     * 1. 从第一个边池开始分配
     * 2. 找出该边池的获胜玩家（可能多个）
     * 3. 平分底池筹码给获胜玩家
     */
    public Map<String, Integer> distributePots(List<Pot> pots, Map<String, HandRank> playerHandRanks) {
        Map<String, Integer> winnings = new HashMap<>();

        for (Pot pot : pots) {
            // 1. 找出该底池的候选玩家
            List<Player> candidates = pot.getPlayerIds().stream()
                .map(this::findPlayer)
                .filter(Objects::nonNull)
                .toList();

            // 2. 找出获胜玩家（可能多个）
            List<Player> winners = findWinners(candidates, playerHandRanks);

            // 3. 平分底池筹码
            int amountPerWinner = pot.getAmount() / winners.size();

            for (Player winner : winners) {
                winnings.merge(winner.getId(), amountPerWinner, Integer::sum);
            }
        }

        return winnings;
    }

    /**
     * 找出获胜玩家（可能多个）
     */
    private List<Player> findWinners(List<Player> candidates, Map<String, HandRank> playerHandRanks) {
        Player bestPlayer = candidates.get(0);
        HandRank bestRank = playerHandRanks.get(bestPlayer.getId());

        List<Player> winners = new ArrayList<>();
        winners.add(bestPlayer);

        for (int i = 1; i < candidates.size(); i++) {
            Player player = candidates.get(i);
            HandRank rank = playerHandRanks.get(player.getId());

            int comparison = rank.compareTo(bestRank);

            if (comparison > 0) {
                // 更好的牌型
                bestPlayer = player;
                bestRank = rank;
                winners.clear();
                winners.add(player);
            } else if (comparison == 0) {
                // 平局
                winners.add(player);
            }
        }

        return winners;
    }
}
```

---

## 7. 数值计算最佳实践

### 7.1 避免浮点数运算

```java
// ❌ 错误：使用浮点数计算
public double calculatePotPercentage(int playerBet, int totalPot) {
    return playerBet / totalPot;  // 0.0（整数除法）
}

// ✅ 正确：使用整数计算或转换为double
public double calculatePotPercentage(int playerBet, int totalPot) {
    return (double) playerBet / totalPot;  // 0.5
}

// ✅ 更好：返回百分比（避免浮点数）
public int calculatePotPercentage(int playerBet, int totalPot) {
    return (playerBet * 100) / totalPot;  // 50（表示50%）
}
```

### 7.2 避免精度丢失

```java
// ❌ 错误：先乘后除可能丢失精度
public int calculateShare(int total, int percentage) {
    return (total * percentage) / 100;  // 可能溢出
}

// ✅ 正确：先除后乘（使用long避免溢出）
public int calculateShare(int total, int percentage) {
    return (int) ((long) total * percentage / 100);
}
```

### 7.3 使用Optional避免NPE

```java
// ❌ 错误：可能返回null
public Integer getWinnerPrize(String playerId) {
    Player player = findPlayer(playerId);
    if (player != null && player.isWinner()) {
        return player.getPrize();
    }
    return null;  // 可能导致NPE
}

// ✅ 正确：使用Optional
public Optional<Integer> getWinnerPrize(String playerId) {
    Player player = findPlayer(playerId);
    if (player != null && player.isWinner()) {
        return Optional.of(player.getPrize());
    }
    return Optional.empty();
}

// 使用
Optional<Integer> prize = getWinnerPrize(playerId);
if (prize.isPresent()) {
    System.out.println("奖品: " + prize.get());
}
```

---

## 8. 总结

### 8.1 核心规则

| 规则 | 说明 |
|------|------|
| **最小单位** | 1元/筹码，不支持小数 |
| **数据类型** | 使用 `int` 存储筹码 |
| **溢出检查** | 所有运算必须检查溢出 |
| **合法性检查** | 增加/扣除筹码必须检查合法 |
| **错误处理** | 溢出时立即抛出异常，记录日志 |
| **不静默处理** | 绝对不能静默处理溢出 |

### 8.2 工具类

| 工具类 | 用途 |
|--------|------|
| `ChipCalculator` | 安全的四则运算（带溢出检查） |
| `PotCalculator` | 计算边池 |
| `PotDistributor` | 分配底池筹码 |

### 8.3 扩展性

通过 `ChipAmount` 接口，未来可以无缝切换到小数筹码实现，无需修改业务逻辑代码。

---

**文档版本**: v1.0
**最后更新**: 2026-01-23
