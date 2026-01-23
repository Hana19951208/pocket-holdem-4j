# 测试策略文档

> 文档版本: v1.0
> 创建日期: 2026-01-23
> 作者: AI 助手

---

## 📋 概述

本文档定义了项目的测试策略、测试金字塔、覆盖率目标、并发测试计划和自动化流程。这些策略确保代码质量、系统稳定性和可维护性。

---

## 1. 测试金字塔

### 1.1 金字塔结构

```
         /\
        /  \
       /E2E \        10% - 端到端测试（5个）
      /------\
     /Integration\   20% - 集成测试（10个）
    /------------\
   /  Unit Tests  \  70% - 单元测试（165个）
  /----------------\
```

### 1.2 各层说明

| 测试层级 | 数量 | 覆盖率 | 目的 | 工具 |
|----------|------|--------|------|------|
| **单元测试** | ~80个 | >85% | 验证单个类/方法的正确性 | JUnit 5, Mockito |
| **集成测试** | ~10个 | >70% | 验证多个组件协作 | Spring Boot Test, TestContainers |
| **E2E测试** | ~5个 | 主要流程 | 验证完整业务流程 | WebSocket Client, JMeter |
| **并发测试** | ~10个 | - | 验证线程安全 | ConcurrentTestRunner |
| **压力测试** | 持续进行 | - | 验证系统性能 | JMeter, Gatling |

---

## 2. 并发测试的重要性

### 2.1 并发测试覆盖

#### 场景1：多线程竞争测试

```java
package com.pocketholdem.concurrency;

import com.pocketholdem.model.Player;
import com.pocketholdem.model.Room;
import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 多线程竞争测试
 */
class MultiThreadRaceTest {

    @Test
    @DisplayName("多个玩家同时坐下应串行处理")
    void multiplePlayersSitDownConcurrently() throws InterruptedException {
        // Given
        Room room = new Room("test-room", 9);
        CountDownLatch latch = new CountDownLatch(9);
        ExecutorService executor = Executors.newFixedThreadPool(9);
        AtomicInteger successCount = new AtomicInteger(0);

        // When - 9个玩家同时坐下
        for (int i = 0; i < 9; i++) {
            final int seatIndex = i;
            executor.submit(() -> {
                try {
                    Player player = Player.builder()
                        .id("player-" + seatIndex)
                        .nickname("Player " + seatIndex)
                        .chips(1000)
                        .build();

                    room.sitDown(player, seatIndex);
                    successCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);

        // Then - 所有玩家都应该成功坐下
        assertThat(successCount.get()).isEqualTo(9);
        assertThat(room.getSeatedPlayers()).hasSize(9);
    }

    @Test
    @DisplayName("多个玩家同时下注应正确累积底池")
    void multiplePlayersBetConcurrently() throws InterruptedException {
        // Given
        Room room = createRoomWithPlayers(9);
        CountDownLatch latch = new CountDownLatch(9);
        ExecutorService executor = Executors.newFixedThreadPool(9);

        // When - 9个玩家同时下注10筹码
        for (int i = 0; i < 9; i++) {
            final String playerId = "player-" + i;
            executor.submit(() -> {
                try {
                    Player player = findPlayer(room, playerId);
                    player.bet(10);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);

        // Then - 底池应该累积90筹码
        assertThat(room.getPot()).isEqualTo(90);
    }
}
```

#### 场景2：死锁测试

```java
package com.pocketholdem.concurrency;

import com.pocketholdem.model.Room;
import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 死锁测试
 */
class DeadlockTest {

    @Test
    @DisplayName("锁超时应抛出异常而非死锁")
    void lockTimeoutShouldThrowException() {
        // Given
        Room room = new Room("test-room", 9);
        AtomicBoolean isDeadlocked = new AtomicBoolean(false);

        // 线程1：持有锁，等待10秒
        Thread thread1 = new Thread(() -> {
            try {
                room.getLock().lock();
                Thread.sleep(10000);  // 持有锁10秒
                room.getLock().unlock();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 线程2：尝试获取锁，5秒超时
        Thread thread2 = new Thread(() -> {
            try {
                boolean acquired = room.getLock().tryLock(5, TimeUnit.SECONDS);
                if (!acquired) {
                    isDeadlocked.set(true);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // When
        thread1.start();
        thread2.start();

        thread2.join();  // 等待线程2完成
        thread1.interrupt();
        thread1.join();

        // Then - 不应死锁，应检测到锁超时
        assertThat(isDeadlocked.get()).isTrue();
    }
}
```

#### 场景3：压力测试

```java
package com.pocketholdem.concurrency;

import com.pocketholdem.service.RoomManager;
import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 压力测试
 */
class StressTest {

    @Test
    @DisplayName("100个房间同时创建应成功")
    void create100RoomsConcurrently() throws InterruptedException {
        // Given
        RoomManager roomManager = new RoomManager();
        CountDownLatch latch = new CountDownLatch(100);
        ExecutorService executor = Executors.newFixedThreadPool(50);
        AtomicInteger successCount = new AtomicInteger(0);

        // When - 100个房间同时创建
        for (int i = 0; i < 100; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    roomManager.createRoom("room-" + index, 9, 10, 20);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);

        // Then - 所有房间都应该创建成功
        assertThat(successCount.get()).isEqualTo(100);
        assertThat(roomManager.getRoomCount()).isEqualTo(100);
    }

    @Test
    @DisplayName("1000个并发WebSocket连接应成功")
    void handle1000WebSocketConnections() throws InterruptedException {
        // Given
        WebSocketTestClient client = new WebSocketTestClient("ws://localhost:8080/ws");
        CountDownLatch latch = new CountDownLatch(1000);
        ExecutorService executor = Executors.newFixedThreadPool(100);
        AtomicInteger successCount = new AtomicInteger(0);

        // When - 1000个连接同时建立
        for (int i = 0; i < 1000; i++) {
            executor.submit(() -> {
                try {
                    WebSocketSession session = client.connect();
                    if (session != null && session.isConnected()) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(60, TimeUnit.SECONDS);

        // Then - 所有连接都应该成功
        assertThat(successCount.get()).isEqualTo(1000);
    }
}
```

### 2.2 并发测试工具

| 工具 | 用途 | 使用场景 |
|------|------|----------|
| **JUnit 5 + ConcurrentTestRunner** | 单元级并发测试 | 多线程竞争测试 |
| **JMeter** | 压力测试 | 100个房间并发、1000个WebSocket连接 |
| **Gatling** | 性能测试 | 响应时间P99、吞吐量 |
| **Java VisualVM** | 性能分析 | 内存泄漏、CPU使用率 |
| **JConsole** | 监控 | 线程池状态、锁竞争 |

---

## 3. 测试覆盖率目标

### 3.1 覆盖率目标表

| 模块 | 目标覆盖率 | 测试方法数 | 说明 |
|------|-----------|-----------|------|
| **核心引擎 (PokerEngine)** | >85% | ~35个 | 牌型比较、底池计算、手牌评估 |
| **状态管理 (Player/Room)** | >90% | ~20个 | 玩家状态、房间状态、CopyOnWriteArrayList |
| **控制器 (GameController)** | >85% | ~12个 | WebSocket消息处理、异常处理 |
| **DTO层** | 100% | ~15个 | 序列化/反序列化、字段验证 |
| **WebSocket层** | >80% | ~18个 | Session管理、消息广播、重连机制 |
| **并发测试** | - | ~10个 | 线程安全、死锁、压力测试 |
| **集成测试** | - | ~5个 | 完整游戏流程、STOMP协议 |
| **E2E测试** | - | ~5个 | 创建房间→加入→坐下→游戏→结束 |
| **总计** | **>80%** | **~120个** | - |

> **调整说明**：从 ~176个 调整为 ~120个，减少测试数量但仍保证核心逻辑覆盖。测试实施分阶段：
> - **MVP 阶段**：完成 40%（~48个测试）
> - **Beta 阶段**：完成 70%（~84个测试）
> - **Release 阶段**：完成 100%（~120个测试）

### 3.2 覆盖率工具配置

#### Maven 配置

```xml
<!-- pom.xml -->
<build>
    <plugins>
        <!-- JaCoCo 代码覆盖率插件 -->
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.10</version>
            <executions>
                <execution>
                    <goals>
                        <goal>prepare-agent</goal>
                    </goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>test</phase>
                    <goals>
                        <goal>report</goal>
                    </goals>
                </execution>
                <execution>
                    <id>check</id>
                    <goals>
                        <goal>check</goal>
                    </goals>
                    <configuration>
                        <rules>
                            <rule>
                                <element>PACKAGE</element>
                                <limits>
                                    <limit>
                                        <counter>LINE</counter>
                                        <value>COVEREDRATIO</value>
                                        <minimum>0.80</minimum>
                                    </limit>
                                </limits>
                            </rule>
                        </rules>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

#### 生成覆盖率报告

```bash
# 运行测试并生成覆盖率报告
cd server
./mvnw clean test jacoco:report

# 查看报告
open target/site/jacoco/index.html
```

---

## 4. 压力测试和性能测试计划

### 4.1 压力测试场景

#### 场景1：100个房间同时创建

```java
package com.pocketholdem.performance;

import com.pocketholdem.service.RoomManager;
import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 压力测试：100个房间同时创建
 */
class PressureTest {

    @Test
    @DisplayName("100个房间同时创建应在5秒内完成")
    void create100RoomsWithin5Seconds() throws InterruptedException {
        // Given
        RoomManager roomManager = new RoomManager();
        CountDownLatch latch = new CountDownLatch(100);
        ExecutorService executor = Executors.newFixedThreadPool(50);
        long startTime = System.currentTimeMillis();
        AtomicInteger successCount = new AtomicInteger(0);

        // When - 100个房间同时创建
        for (int i = 0; i < 100; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    roomManager.createRoom("room-" + index, 9, 10, 20);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then - 所有房间应该创建成功，且在5秒内完成
        assertThat(successCount.get()).isEqualTo(100);
        assertThat(duration).isLessThan(5000);
    }
}
```

#### 场景2：100个房间同时进行游戏

```java
@Test
@DisplayName("100个房间同时进行游戏应正常运行")
void run100GamesConcurrently() throws InterruptedException {
    // Given
    RoomManager roomManager = new RoomManager();
    List<Room> rooms = new ArrayList<>();

    // 创建100个房间
    for (int i = 0; i < 100; i++) {
        Room room = roomManager.createRoom("room-" + i, 9, 10, 20);
        rooms.add(room);

        // 添加9个玩家
        for (int j = 0; j < 9; j++) {
            Player player = Player.builder()
                .id("player-" + i + "-" + j)
                .nickname("Player " + j)
                .chips(1000)
                .build();
            room.sitDown(player, j);
        }

        // 开始游戏
        room.startGame();
    }

    // When - 所有房间同时进行游戏
    CountDownLatch latch = new CountDownLatch(100);
    ExecutorService executor = Executors.newFixedThreadPool(50);

    for (Room room : rooms) {
        executor.submit(() -> {
            try {
                // 模拟游戏进行
                for (int round = 0; round < 10; round++) {
                    room.processRound();
                    Thread.sleep(100);  // 模拟思考时间
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await(30, TimeUnit.SECONDS);

    // Then - 所有房间应该正常完成游戏
    for (Room room : rooms) {
        assertThat(room.getGameState()).isEqualTo(GameState.FINISHED);
    }
}
```

#### 场景3：1000个并发WebSocket连接

```java
@Test
@DisplayName("1000个并发WebSocket连接应在10秒内建立")
void connect1000WebSocketClientsWithin10Seconds() throws InterruptedException {
    // Given
    WebSocketTestServer server = new WebSocketTestServer(8080);
    server.start();

    CountDownLatch latch = new CountDownLatch(1000);
    ExecutorService executor = Executors.newFixedThreadPool(100);
    AtomicInteger successCount = new AtomicInteger(0);
    long startTime = System.currentTimeMillis();

    // When - 1000个客户端同时连接
    for (int i = 0; i < 1000; i++) {
        executor.submit(() -> {
            try {
                WebSocketTestClient client = new WebSocketTestClient("ws://localhost:8080/ws");
                WebSocketSession session = client.connect();
                if (session != null && session.isConnected()) {
                    successCount.incrementAndGet();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await(10, TimeUnit.SECONDS);
    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime;

    // Then - 所有连接应该成功，且在10秒内建立
    assertThat(successCount.get()).isEqualTo(1000);
    assertThat(duration).isLessThan(10000);

    server.stop();
}
```

### 4.2 性能指标

| 指标 | 目标值 | 说明 |
|------|--------|------|
| **响应时间 (P99)** | <100ms | 99%的请求在100ms内完成 |
| **并发房间数** | ≥100 | 支持100个房间同时游戏 |
| **并发WebSocket连接** | ≥1000 | 支持1000个WebSocket连接 |
| **内存占用** | <512MB | 100个房间运行时内存占用 |
| **CPU使用率** | <80% | 100个房间运行时CPU使用率 |
| **无内存泄漏** | 0 | 连续运行24小时无内存泄漏 |
| **无死锁** | 0 | 连续运行24小时无死锁 |

### 4.3 性能监控

```java
package com.pocketholdem.monitor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.lang.management.*;

/**
 * 性能监控组件
 */
@Slf4j
@Component
public class PerformanceMonitor {

    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

    /**
     * 打印性能指标
     */
    public void printPerformanceMetrics() {
        // 内存使用
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        long usedMemory = heapUsage.getUsed() / 1024 / 1024;
        long maxMemory = heapUsage.getMax() / 1024 / 1024;

        // 线程数量
        int threadCount = threadBean.getThreadCount();

        // CPU使用率
        double cpuUsage = osBean.getSystemLoadAverage();

        log.info("性能指标 - 内存: {}MB/{}MB, 线程数: {}, CPU: {}%",
            usedMemory, maxMemory, threadCount, cpuUsage);
    }

    /**
     * 检查内存泄漏
     */
    public void checkMemoryLeak() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        double memoryUsageRatio = (double) heapUsage.getUsed() / heapUsage.getMax();

        if (memoryUsageRatio > 0.9) {
            log.error("内存使用率过高: {}%", memoryUsageRatio * 100);
            alertManager.sendAlert("内存泄漏检测");
        }
    }
}
```

---

## 5. 测试自动化流程

### 5.1 GitHub Actions CI/CD 配置

```yaml
# .github/workflows/test.yml
name: Test

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'maven'

      - name: Run unit tests
        run: |
          cd server
          ./mvnw test -Dtest='!*IntegrationTest'

      - name: Run integration tests
        run: |
          cd server
          ./mvnw test -Dtest='*IntegrationTest'

      - name: Generate coverage report
        run: |
          cd server
          ./mvnw jacoco:report

      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          files: server/target/site/jacoco/jacoco.xml
          flags: unittests
          name: codecov-umbrella

      - name: Check coverage threshold
        run: |
          cd server
          ./mvnw jacoco:check
```

### 5.2 本地测试命令

```bash
# 运行全部测试
cd server
./mvnw test

# 运行单元测试（排除集成测试）
./mvnw test -Dtest='!*IntegrationTest'

# 运行集成测试
./mvnw test -Dtest='*IntegrationTest'

# 运行并发测试
./mvnw test -Dtest='*ConcurrencyTest'

# 运行特定测试类
./mvnw test -Dtest=PokerEngineTest

# 运行特定测试方法
./mvnw test -Dtest=PokerEngineTest#testFlush

# 生成覆盖率报告
./mvnw clean test jacoco:report

# 检查覆盖率是否达标
./mvnw jacoco:check

# 详细输出模式
./mvnw test -X
```

---

## 6. 测试最佳实践

### 6.1 单元测试规范

```java
package com.pocketholdem.engine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 单元测试示例
 */
class PokerEngineTest {

    @Test
    @DisplayName("皇家同花顺应该击败四条")
    void royalFlushShouldBeatFourOfAKind() {
        // Given - 准备测试数据
        List<Card> royalFlush = createRoyalFlush();
        List<Card> fourOfAKind = createFourOfAKind();

        // When - 执行被测方法
        int result = PokerEngine.compareHands(royalFlush, fourOfAKind);

        // Then - 验证结果
        assertThat(result).isGreaterThan(0);
    }

    @Test
    @DisplayName("筹码不足时下注应抛出异常")
    void betWithInsufficientChipsShouldThrowException() {
        // Given
        Player player = Player.builder()
            .id("player-001")
            .chips(100)
            .build();

        // When & Then
        assertThatThrownBy(() -> player.bet(200))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("筹码不足");
    }
}
```

### 6.2 集成测试规范

```java
package com.pocketholdem.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 集成测试示例
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GameIntegrationTest {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Test
    @DisplayName("完整的游戏流程：创建房间→加入→坐下→游戏→结束")
    void completeGameFlow() {
        // Given - 创建房间
        Room room = roomManager.createRoom("test-room", 9, 10, 20);

        // When - 9个玩家坐下并开始游戏
        for (int i = 0; i < 9; i++) {
            Player player = Player.builder()
                .id("player-" + i)
                .nickname("Player " + i)
                .chips(1000)
                .build();
            room.sitDown(player, i);
        }

        room.startGame();

        // Then - 验证游戏正常进行
        assertThat(room.getGameState()).isNotEqualTo(GameState.WAITING);
        assertThat(room.getSeatedPlayers()).hasSize(9);
    }
}
```

### 6.3 并发测试规范

```java
@Test
@DisplayName("多线程环境下应保证线程安全")
void shouldBeThreadSafe() throws InterruptedException {
    // Given
    Room room = new Room("test-room", 9);
    CountDownLatch latch = new CountDownLatch(100);
    ExecutorService executor = Executors.newFixedThreadPool(10);

    // When - 100个操作同时执行
    for (int i = 0; i < 100; i++) {
        executor.submit(() -> {
            try {
                // 执行操作
                room.someOperation();
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await(5, TimeUnit.SECONDS);

    // Then - 验证结果正确
    assertThat(room.getSomeValue()).isEqualTo(expectedValue);
}
```

---

## 7. 总结

### 7.1 测试金字塔

- **单元测试（70%）**：快速反馈，高覆盖率
- **集成测试（20%）**：验证组件协作
- **E2E测试（10%）**：验证完整流程

### 7.2 关键指标

| 指标 | 目标 | 当前 |
|------|------|------|
| **总体覆盖率** | >80% | 待统计 |
| **核心引擎覆盖率** | >85% | 待统计 |
| **并发房间数** | ≥100 | 待测试 |
| **响应时间 (P99)** | <100ms | 待测试 |
| **内存泄漏** | 0 | 待验证 |

### 7.3 持续改进

1. **每日运行**：GitHub Actions 自动运行测试
2. **覆盖率检查**：覆盖率低于80%时阻止合并
3. **性能回归**：定期运行压力测试
4. **并发测试**：每次提交后运行并发测试

---

**文档版本**: v1.0
**最后更新**: 2026-01-23
