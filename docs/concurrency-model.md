# 并发模型设计文档

> 文档版本: v1.0
> 创建日期: 2026-01-23
> 作者: AI 助手

---

## 📋 概述

本文档详细说明了从 Node.js 迁移到 Java 后的并发模型差异、设计原则和实现规范，确保多线程环境下的线程安全和性能优化。

---

## 1. Node.js Event Loop 与 Java Thread Pool 的差异

### 1.1 Node.js 并发模型

```
┌─────────────────────────────────────────┐
│         Node.js 单线程事件循环           │
│                                         │
│   ┌──────────┐  ┌──────────┐         │
│   │  Room A  │  │  Room B  │         │
│   │  (队列)  │  │  (队列)  │         │
│   └────┬─────┘  └────┬─────┘         │
│        │              │               │
│        └──────┬───────┘               │
│               ▼                        │
│   ┌──────────────────────┐            │
│   │  事件循环串行调度     │            │
│   │ (天然单房间无竞态)   │            │
│   └──────────────────────┘            │
└─────────────────────────────────────────┘

特性：
- 单线程：同一房间操作天然串行
- 无竞态：无需考虑线程安全
- 阻塞风险：长时间计算会阻塞整个进程
```

**Node.js 特性总结**：
- **单线程事件循环**：所有房间操作在同一个线程中串行执行
- **天然线程安全**：无需显式锁机制
- **阻塞风险**：长时间计算或 I/O 会阻塞所有房间
- **CPU利用率**：只能利用单个 CPU 核心

### 1.2 Java 并发模型

```
┌─────────────────────────────────────────┐
│      Java 多线程 + 线程池               │
│                                         │
│   ┌──────────┐  ┌──────────┐         │
│   │  Room A  │  │  Room B  │         │
│   │ Reentrant │  │ Reentrant │         │
│   │   Lock    │  │   Lock    │         │
│   └────┬─────┘  └────┬─────┘         │
│        │              │               │
└────────┼──────────────┼───────────────┘
          │              │
          └─── ┌────────┴────┐               │
              ▼               ▼              │
    ┌────────────┐   ┌────────────┐        │
    │ Thread-1   │   │ Thread-2   │        │
    │ (CPU核心1) │   │ (CPU核心2) │        │
    └────────────┘   └────────────┘        │

依赖：Tomcat线程池（默认200线程）

特性：
- 多线程：不同房间可并发处理
- 显式锁：需手动保证房间级串行
- 性能优势：利用多核CPU
- 风险：锁竞争、死锁、内存可见性
```

**Java 特性总结**：
- **多线程处理**：不同房间可在不同线程中并发执行
- **需要显式锁**：同一房间内操作需要加锁保证串行
- **性能优势**：可充分利用多核 CPU 资源
- **风险点**：需要处理锁竞争、死锁、内存可见性问题

### 1.3 关键差异对比表

| 维度 | Node.js | Java |
|------|---------|------|
| **执行模型** | 单线程事件循环 | 多线程 + 线程池 |
| **房间并发** | 串行（天然） | 可并发（需加锁） |
| **锁机制** | 无需显式锁 | ReentrantLock |
| **CPU利用** | 单核 | 多核 |
| **阻塞影响** | 全局阻塞 | 仅阻塞当前线程 |
| **开发复杂度** | 简单 | 中等（需考虑线程安全） |
| **性能上限** | 受限于单核 | 可扩展到多核 |

---

## 2. Java 并发模型的保证和边界

### 2.1 保证机制

#### 保证1：同一房间内的操作严格串行
```java
public class Room {
    private final ReentrantLock lock = new ReentrantLock();

    public void processAction(Player player, Action action) {
        lock.lock();
        try {
            // 同一房间内的所有操作在这里串行执行
            updateGameState(player, action);
            broadcastState();
        } finally {
            lock.unlock();
        }
    }
}
```

**实现**：每个房间实例持有一个独立的 `ReentrantLock`，所有修改房间状态的操作都必须持有该锁。

#### 保证2：不同房间之间的操作可以并发

#### 保证3：内存可见性

**问题**：在多线程环境中，一个线程对共享变量的修改可能不会立即对其他线程可见。

```java
// ❌ 错误：没有内存可见性保证
public class Room {
    private boolean isGameStarted;  // 非volatile

    public void startGame() {
        // 线程1
        this.isGameStarted = true;
    }

    public boolean isGameStarted() {
        // 线程2：可能看不到线程1的修改
        return this.isGameStarted;  // 可能返回false
    }
}
```

**解决方案**：使用 `volatile` 关键字保证内存可见性

```java
// ✅ 正确：使用volatile保证可见性
public class Room {
    private volatile boolean isGameStarted;  // volatile保证可见性

    public void startGame() {
        // 线程1：写操作
        this.isGameStarted = true;  // 立即对所有线程可见
    }

    public boolean isGameStarted() {
        // 线程2：读操作
        return this.isGameStarted;  // 必须看到最新值
    }
}
```

**volatile 关键字作用**：
- **可见性**：写操作立即刷新到主内存，读操作从主内存读取
- **禁止指令重排序**：防止 JVM 优化导致指令乱序执行
- **不保证原子性**：复合操作（如 `count++`）仍需加锁

**适用场景**：

| 场景 | 是否需要volatile | 说明 |
|------|-----------------|------|
| **标志位**（如 `isGameStarted`） | ✅ 是 | 只需要可见性，不需要原子性 |
| **计数器**（如 `playerCount`） | ❌ 否 | 需要 `AtomicInteger` 或加锁 |
| **对象引用**（如 `currentPlayer`） | ✅ 是 | 引写本身就是原子的 |
| **复合操作**（如 `count++`） | ❌ 否 | volatile不保证原子性 |

**RoomManager 线程安全**：

```java
// ✅ 正确：使用ConcurrentHashMap保证线程安全
public class RoomManager {
    // ConcurrentHashMap是线程安全的，读操作无需锁
    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();

    public Room getRoom(String roomId) {
        return rooms.get(roomId);  // 线程安全
    }

    public void createRoom(Room room) {
        rooms.put(room.getId(), room);  // 线程安全
    }

    public void removeRoom(String roomId) {
        rooms.remove(roomId);  // 线程安全
    }
}
```

**总结**：
- 标志位（boolean）使用 `volatile`
- 计数器使用 `AtomicInteger` 或加锁
- 集合使用线程安全类（`ConcurrentHashMap`, `CopyOnWriteArrayList`）
```java
// 房间A的锁不会影响房间B
Map<String, Room> rooms = new ConcurrentHashMap<>();

// 线程1处理房间A
Thread-1: rooms.get("roomA").processAction(player1, action1);
Thread-1: rooms.get("roomA").lock() -> 获取成功

// 线程2处理房间B（与线程1并发）
Thread-2: rooms.get("roomB").processAction(player2, action2);
Thread-2: rooms.get("roomB").lock() -> 获取成功

// 两个房间操作在不同线程中并发执行
```

#### 保证3：读取操作使用防御性拷贝
```java
public class Room {
    private final List<Player> seatedPlayers = new CopyOnWriteArrayList<>();

    // 查询操作不持有锁，返回不可变副本
    public List<PlayerDTO> getSeatedPlayers() {
        return seatedPlayers.stream()
            .map(Player::toPublicDTO)
            .collect(Collectors.toList()); // 创建新列表
    }
}
```

**原因**：查询操作频率高，持有锁会阻塞写操作，返回副本可以避免查询和写操作的锁竞争。

### 2.2 边界限制

| 边界项 | 限制值 | 说明 |
|--------|--------|------|
| **最大并发房间数** | Tomcat 线程池大小（默认200） | 超过会排队等待 |
| **单房间最大并发操作数** | 1（锁保证） | 所有操作串行执行 |
| **锁超时时间** | 5秒 | 避免死锁长时间阻塞 |
| **最大房间数** | 无限制（受内存限制） | 使用 ConcurrentHashMap 管理 |

---

## 3. 房间级锁的设计原则

### 3.1 锁粒度

**原则**：房间级锁（ReentrantLock per Room）

```java
// ✅ 正确：每个房间独立锁
public class Room {
    private final String roomId;
    private final ReentrantLock lock = new ReentrantLock();
    private List<Player> players = new ArrayList<>();

    public void addPlayer(Player player) {
        lock.lock();
        try {
            players.add(player);
        } finally {
            lock.unlock();
        }
    }
}

// ❌ 错误：全局锁会导致所有房间串行
public class RoomManager {
    private final ReentrantLock globalLock = new ReentrantLock();

    public void addPlayerToRoom(String roomId, Player player) {
        globalLock.lock(); // 错误：所有房间操作都串行
        try {
            Room room = rooms.get(roomId);
            room.addPlayer(player);
        } finally {
            globalLock.unlock();
        }
    }
}
```

**好处**：
- 不同房间的锁互不干扰
- 不同房间可以并发处理
- 避免全局锁导致的性能瓶颈

### 3.2 锁策略

#### 写操作：必须加锁
```java
// 以下操作必须持有锁
- addPlayer()          // 添加玩家
- removePlayer()       // 移除玩家
- sitDown()            // 坐下
- processAction()      // 处理玩家操作
- updateGameState()    // 更新游戏状态
- dealHoleCards()      // 发手牌
```

#### 读操作：使用缓存副本，不持有锁
```java
// 以下操作不持有锁，返回缓存副本
- getSeatedPlayers()   // 返回已坐下玩家列表（CopyOnWriteArrayList）
- getActivePlayers()   // 返回活跃玩家列表
- getGameStateDTO()    // 返回游戏状态DTO（防御性拷贝）
```

#### 复杂操作：持锁时间尽量短
```java
public void processComplexAction(Player player, Action action) {
    // ✅ 正确：锁内只做状态更新
    GameState newState;
    lock.lock();
    try {
        newState = engine.calculateNextState(currentState, action);
        this.currentState = newState;
    } finally {
        lock.unlock();
    }

    // 锁外做耗时操作（WebSocket广播、日志记录等）
    simpMessagingTemplate.convertAndSend("/topic/room/" + roomId, newState);
    log.info("Action processed: {}", action);
}
```

---

## 4. 死锁防护策略

### 4.1 死锁场景分析

#### 场景1：锁嵌套导致死锁
```java
// ❌ 危险：锁内调用可能需要其他锁的方法
lock.lock();
try {
    updateGameState();
    // 如果 broadcastToRoom 内部也需要获取其他锁，可能形成死锁
    broadcastToRoom();
} finally {
    lock.unlock();
}
```

#### 场景2：WebSocket广播与房间锁竞争
```java
// 线程A：持有房间锁，尝试获取WebSocket广播锁
Thread-A: room.lock()
Thread-A: broadcastToRoom() -> 需要获取WebSocket连接锁

// 线程B：持有WebSocket连接锁，尝试获取房间锁
Thread-B: wsConnection.lock()
Thread-B: room.processAction() -> 需要获取房间锁

// 形成死锁：Thread-A等Thread-B释放WebSocket锁，Thread-B等Thread-A释放房间锁
```

### 4.2 防护措施

#### 措施1：锁超时（5秒）
```java
public void processAction(Player player, Action action) {
    try {
        // 尝试获取锁，最多等待5秒
        boolean acquired = lock.tryLock(5, TimeUnit.SECONDS);
        if (!acquired) {
            throw new IllegalStateException("房间处理超时，可能存在死锁");
        }

        try {
            updateGameState(player, action);
        } finally {
            lock.unlock();
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("线程被中断", e);
    }
}
```

#### 措施2：避免锁嵌套
```java
// ❌ 错误：锁内调用可能需要锁的方法
lock.lock();
try {
    updateGameState();
    broadcastToRoom(); // 可能需要其他锁
} finally {
    lock.unlock();
}

// ✅ 正确：锁内更新，锁外广播
lock.lock();
try {
    updateGameState();
} finally {
    lock.unlock();
}
broadcastToRoom(); // 在锁外执行
```

#### 措施3：WebSocket广播移出锁外
```java
public class Room {
    private final SimpMessagingTemplate messagingTemplate;
    private final ReentrantLock lock = new ReentrantLock();

    // ✅ 正确：WebSocket广播在锁外
    public void processAction(Player player, Action action) {
        // 准备广播数据
        RoomDTO result;

        // 锁内：只更新状态
        lock.lock();
        try {
            this.currentPlayer = getNextPlayer();
            result = toRoomDTO();
        } finally {
            lock.unlock();
        }

        // 锁外：WebSocket广播
        messagingTemplate.convertAndSend("/topic/room/" + roomId, result);
    }
}
```

#### 措施4：日志监控
```java
@Slf4j
public class Room {
    private final ReentrantLock lock = new ReentrantLock();

    public void processAction(Player player, Action action) {
        long startTime = System.currentTimeMillis();
        try {
            boolean acquired = lock.tryLock(5, TimeUnit.SECONDS);
            if (!acquired) {
                long waitTime = System.currentTimeMillis() - startTime;
                log.warn("房间[{}]锁等待超时，等待时间: {}ms", roomId, waitTime);
                throw new IllegalStateException("房间处理超时");
            }

            try {
                updateGameState(player, action);
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("房间[{}]线程被中断", roomId, e);
            throw new IllegalStateException("线程被中断", e);
        }

        long totalTime = System.currentTimeMillis() - startTime;
        if (totalTime > 100) {
            log.warn("房间[{}]操作耗时: {}ms", roomId, totalTime);
        }
    }
}
```

### 4.3 死锁检测与恢复

```java
@Configuration
public class DeadlockMonitor {
    @Scheduled(fixedRate = 10000) // 每10秒检查一次
    public void checkDeadlocks() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        long[] deadlockedThreads = threadBean.findDeadlockedThreads();

        if (deadlockedThreads != null && deadlockedThreads.length > 0) {
            log.error("检测到死锁线程数: {}", deadlockedThreads.length);
            ThreadInfo[] threadInfos = threadBean.getThreadInfo(deadlockedThreads);
            for (ThreadInfo info : threadInfos) {
                log.error("死锁线程: {} - 状态: {}", info.getThreadName(), info.getThreadState());
            }

            // 触发告警通知
            alertManager.sendAlert("检测到死锁，请立即处理");
        }
    }
}
```

---

## 5. 查询方法去锁化

### 5.1 问题分析

**问题**：查询方法持有锁会阻塞写操作

```java
// ❌ 错误：查询方法持有锁
public class Room {
    private final List<Player> players = new ArrayList<>();

    public List<Player> getSeatedPlayers() {
        lock.lock();
        try {
            return new ArrayList<>(players); // 阻塞写操作
        } finally {
            lock.unlock();
        }
    }
}
```

**影响**：
- 高频查询（如前端轮询房间状态）会频繁获取锁
- 写操作（玩家坐下、下注）被查询操作阻塞
- 成为性能瓶颈

### 5.2 解决方案：使用CopyOnWriteArrayList

```java
// ✅ 正确：使用CopyOnWriteArrayList
public class Room {
    // 写操作时复制，读操作无需锁
    private final CopyOnWriteArrayList<Player> seatedPlayers = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Player> activePlayers = new CopyOnWriteArrayList<>();

    // 写操作：自动复制
    public void addPlayer(Player player) {
        seatedPlayers.add(player); // 内部创建新数组，无需手动加锁
    }

    // 读操作：无需锁，直接读取
    public List<PlayerDTO> getSeatedPlayers() {
        return seatedPlayers.stream()
            .map(Player::toPublicDTO)
            .collect(Collectors.toList());
    }
}
```

**CopyOnWriteArrayList 特性**：
- **写操作**：创建新数组副本，修改后替换原数组
- **读操作**：直接读取当前数组，无需锁
- **内存开销**：写操作时需要复制整个数组

**适用范围**：

| 场景 | 是否适用 | 说明 |
|------|----------|------|
| **低频修改，高频查询** | ✅ 适合 | 如 `seatedPlayers`（仅坐下、离席时修改） |
| **高频修改** | ❌ 不适合 | 写操作频繁会复制大量数组，性能差 |
| **小集合** | ✅ 适合 | 写操作复制开销小 |
| **大集合（>1000元素）** | ⚠️ 谨慎 | 写操作复制开销大，考虑其他方案 |

**本项目适用场景**：
- `seatedPlayers` - ✅ 适合（仅坐下、离席时修改）
- `activePlayers` - ✅ 适合（仅淘汰、重置时修改）
- `gameHistory` - ⚠️ 谨慎（每手牌可能修改多次，考虑普通List+锁）

### 5.3 权衡分析

| 方案 | 一致性 | 性能 | 内存开销 | 适用场景 |
|------|--------|------|----------|----------|
| **加锁查询** | 强一致性 | 查询和写互斥阻塞 | 低 | 数据一致性要求极高 |
| **CopyOnWriteArrayList** | 最终一致性 | 读操作零阻塞，写操作略慢 | 中等 | 读多写少 |
| **缓存+锁** | 最终一致性 | 读操作不阻塞写 | 高 | 高频查询 |

**本项目选择**：CopyOnWriteArrayList
- **原因**：
  - 玩家列表（seatedPlayers, activePlayers）修改频率低（仅坐下、离席、淘汰）
  - 查询频率高（前端频繁获取房间状态）
  - 最终一致性可接受（<10ms延迟不影响游戏逻辑）

### 5.4 实现示例

```java
public class Room {
    private final String roomId;
    private final CopyOnWriteArrayList<Player> seatedPlayers = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Player> activePlayers = new CopyOnWriteArrayList<>();
    private GameState gameState;

    // 写操作：直接调用，无需加锁
    public void sitDown(Player player, int seatIndex) {
        if (seatIndex >= MAX_PLAYERS) {
            throw new IllegalArgumentException("座位号超出范围");
        }
        seatedPlayers.add(player);
    }

    public void removePlayer(String playerId) {
        seatedPlayers.removeIf(p -> p.getId().equals(playerId));
    }

    // 读操作：无需锁，直接返回
    public List<PlayerDTO> getSeatedPlayers() {
        // 返回新的列表，防止外部修改
        return seatedPlayers.stream()
            .map(Player::toPublicDTO)
            .collect(Collectors.toList());
    }

    public RoomDTO toRoomDTO() {
        return RoomDTO.builder()
            .roomId(roomId)
            .seatedPlayers(getSeatedPlayers())
            .activePlayers(getActivePlayers())
            .gameState(GameStateDTO.from(gameState))
            .build();
    }
}
```

---

## 6. 性能优化建议

### 6.1 线程池调优

```yaml
# application.yml
server:
  tomcat:
    threads:
      max: 200          # 最大线程数（根据CPU核心数调整）
      min-spare: 10     # 最小空闲线程数
    accept-count: 100   # 队列长度
    connection-timeout: 20000ms
```

**调优建议**：
- **CPU密集型**：线程数 = CPU核心数 + 1
- **I/O密集型**：线程数 = CPU核心数 * 2
- **本项目**：I/O密集型（WebSocket通信为主），建议200线程

### 6.2 监控指标

| 指标 | 告警阈值 | 说明 |
|------|----------|------|
| **锁等待时间** | >100ms (P99) | 单次操作锁等待时间 |
| **锁超时次数** | >10次/分钟 | 锁等待超时次数 |
| **线程池活跃度** | >80% | 线程池使用率 |
| **响应时间** | >100ms (P99) | 单次操作响应时间 |
| **并发房间数** | >150 | 当前并发房间数 |

---

## 7. 总结

### 7.1 核心原则

1. **房间级锁**：每个房间独立锁，不同房间并发
2. **写加锁，读去锁**：写操作必须加锁，读操作使用CopyOnWriteArrayList
3. **锁外广播**：WebSocket广播移出锁外，避免死锁
4. **锁超时**：所有锁调用添加超时参数（5秒）
5. **避免锁嵌套**：锁内不调用可能需要其他锁的方法

### 7.2 风险点与对策

| 风险 | 对策 |
|------|------|
| **死锁** | 锁超时 + 避免锁嵌套 + 死锁监控 |
| **性能瓶颈** | 读操作去锁化 + 线程池调优 |
| **内存可见性** | 使用volatile + final保证可见性 |
| **锁竞争** | 房间级锁 + 不同房间并发 |

### 7.3 最佳实践

```java
// ✅ 推荐模式
public void processAction(Player player, Action action) {
    // 1. 准备数据
    RoomDTO result;

    // 2. 锁内：只更新状态
    try {
        lock.tryLock(5, TimeUnit.SECONDS);
        try {
            updateGameState(player, action);
            result = toRoomDTO();
        } finally {
            lock.unlock();
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("操作被中断", e);
    }

    // 3. 锁外：WebSocket广播
    messagingTemplate.convertAndSend("/topic/room/" + roomId, result);

    // 4. 记录日志
    log.info("操作处理完成: {}", action);
}
```

---

**文档版本**: v1.0
**最后更新**: 2026-01-23
