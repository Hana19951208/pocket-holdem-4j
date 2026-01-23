# Pocket Holdem - 阶段二补充设计方案

> 本文档补充 `architecture.md` 和 `concurrency-model.md` 中遗漏的关键实现细节。
> 重点关注：资源生命周期、异常并发处理、游戏规则完整性。

---

## 📋 文档概述

### 文档目的
- 明确 5 个关键遗漏点的实现策略
- 提供详细的伪代码和流程描述
- 定义接口规范和约束条件
- 为开发实施提供明确指导

### 适用范围
- **阶段二：状态管理与并发控制**
- **组件层级**：RoomManager、Room、GameController、TimerManager、RebuyService
- **约束条件**：Java 17 + Spring Boot 3.2，服务端权威，高并发支持（1000+游戏房间）

---

## 1️⃣ 房间生命周期与回收 (Room GC)

### 问题陈述
当前设计未定义空闲房间的销毁机制，会导致内存泄漏。

### 推荐方案：混合策略（事件驱动 + 定期扫描）

**核心思想**：
- **事件驱动**：玩家离开时立即触发延迟清理，响应快
- **定期扫描**：每分钟扫描兜底，防止遗漏

### 1.1 清理策略配置

```java
public class RoomCleanupConfig {
    // 空闲阈值：30分钟无活动
    private static final long IDLE_THRESHOLD_MS = 30 * 60 * 1000;

    // 延迟清理时间：给玩家重连时间
    private static final long CLEANUP_DELAY_MS = 2 * 60 * 1000;

    // 扫描频率：每分钟一次
    private static final long SCAN_INTERVAL_MS = 60 * 1000;

    // 最大重连时间窗（客户端localStorage保留）
    private static final long RECONNECT_WINDOW_MS = 5 * 60 * 1000;
}
```

### 1.2 核心实现 - RoomLifecycleManager

```java
@Service
@Slf4j
public class RoomLifecycleManager {

    private final RoomManager roomManager;
    private final ScheduledExecutorService scheduler;

    // 延迟清理任务映射：roomId -> ScheduledFuture
    private final ConcurrentHashMap<String, ScheduledFuture<?>> cleanupTasks = new ConcurrentHashMap<>();

    public RoomLifecycleManager(RoomManager roomManager) {
        this.roomManager = roomManager;
        this.scheduler = Executors.newScheduledThreadPool(2,
            new ThreadFactoryBuilder()
                .setNameFormat("room-cleanup-%d")
                .setDaemon(true)
                .build()
        );

        // 启动定期扫描
        startPeriodicScan();
    }

    /**
     * 玩家离开时的处理（事件驱动）
     */
    public void handlePlayerLeft(String roomId) {
        Room room = roomManager.getRoom(roomId);
        if (room == null) {
            log.debug("房间[{}]已不存在", roomId);
            return;
        }

        // 检查房间是否为空
        if (room.getActivePlayerCount() == 0) {
            // 取消之前的清理任务（如果存在）
            ScheduledFuture<?> existingTask = cleanupTasks.remove(roomId);
            if (existingTask != null && !existingTask.isDone()) {
                existingTask.cancel(false);
                log.debug("取消房间[{}]的旧清理任务", roomId);
            }

            // 安排延迟清理，给玩家重连时间
            ScheduledFuture<?> newTask = scheduler.schedule(
                () -> cleanupRoom(roomId),
                RoomCleanupConfig.CLEANUP_DELAY_MS,
                TimeUnit.MILLISECONDS
            );
            cleanupTasks.put(roomId, newTask);

            log.info("房间[{}]空闲，安排{}分钟后清理",
                roomId, RoomCleanupConfig.CLEANUP_DELAY_MS / 60000);
        }
    }

    /**
     * 定期扫描兜底（防止遗漏）
     */
    private void startPeriodicScan() {
        scheduler.scheduleAtFixedRate(
            this::scanIdleRooms,
            RoomCleanupConfig.SCAN_INTERVAL_MS,
            RoomCleanupConfig.SCAN_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        );
    }

    /**
     * 扫描并清理空闲房间
     */
    private void scanIdleRooms() {
        long now = System.currentTimeMillis();
        int scanned = 0;
        int cleaned = 0;

        try {
            for (Room room : roomManager.getAllRooms()) {
                scanned++;

                // 检查是否有活跃玩家
                if (room.getActivePlayerCount() > 0) {
                    continue;
                }

                // 检查是否已在清理队列
                if (cleanupTasks.containsKey(room.getId())) {
                    continue;
                }

                // 检查最后活动时间
                long idleTime = now - room.getLastActivityTime();
                if (idleTime > RoomCleanupConfig.IDLE_THRESHOLD_MS) {
                    log.warn("发现空闲房间[{}]未被清理（空闲{}分钟），立即执行",
                        room.getId(), idleTime / 60000);
                    cleanupRoom(room.getId());
                    cleaned++;
                }
            }

            if (cleaned > 0) {
                log.info("定期扫描完成：扫描{}房间，清理{}房间", scanned, cleaned);
            }

        } catch (Exception e) {
            log.error("定期扫描房间时发生异常", e);
        }
    }

    /**
     * 实际清理房间
     */
    private void cleanupRoom(String roomId) {
        try {
            Room room = roomManager.getRoom(roomId);
            if (room == null) {
                log.debug("房间[{}]已被清理", roomId);
                return;
            }

            // 再次检查是否有活跃玩家（防止重连后清理）
            if (room.getActivePlayerCount() == 0) {
                roomManager.destroyRoom(roomId);
                cleanupTasks.remove(roomId);
                log.info("房间[{}]清理完成", roomId);
            } else {
                log.info("房间[{}]有新玩家加入，取消清理", roomId);
                cleanupTasks.remove(roomId);
            }

        } catch (Exception e) {
            log.error("清理房间[{}]失败", roomId, e);
            // 失败不重试，等待下一次扫描
        }
    }

    /**
     * 应用关闭时的清理
     */
    @PreDestroy
    public void shutdown() {
        log.info("RoomLifecycleManager 关闭中...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```

### 1.3 RoomManager 补充接口

```java
@Service
public class RoomManager {

    // ... 现有代码 ...

    /**
     * 销毁房间
     *
     * @param roomId 房间ID
     */
    public void destroyRoom(String roomId) {
        Room removed = rooms.remove(roomId);
        if (removed != null) {
            log.info("房间[{}]已销毁", roomId);

            // 清理玩家映射
            removed.getAllPlayers().forEach(player -> {
                playerRoomMap.remove(player.getId());
            });

            // 通知房间销毁事件（如需要）
            // eventPublisher.publishEvent(new RoomDestroyedEvent(roomId));
        }
    }

    /**
     * 获取所有房间（用于扫描）
     *
     * @return 房间列表的快照
     */
    public List<Room> getAllRooms() {
        return new ArrayList<>(rooms.values());
    }

    /**
     * 获取房间最后活动时间
     *
     * @param roomId 房间ID
     * @return 最后活动时间（毫秒时间戳）
     */
    public Optional<Long> getLastActivityTime(String roomId) {
        Room room = rooms.get(roomId);
        return room != null ? Optional.of(room.getLastActivityTime()) : Optional.empty();
    }
}
```

### 1.4 Room 类补充字段

```java
public class Room {

    // ... 现有字段 ...

    /**
     * 最后活动时间（用于空闲检测）
     */
    private volatile long lastActivityTime;

    public Room(String roomId, String hostId) {
        // ... 现有初始化代码 ...
        this.lastActivityTime = System.currentTimeMillis();
    }

    /**
     * 更新活动时间
     */
    public void updateActivityTime() {
        this.lastActivityTime = System.currentTimeMillis();
    }

    /**
     * 获取活动玩家数量（不包括已淘汰、观战的玩家）
     */
    public int getActivePlayerCount() {
        return (int) players.values().stream()
            .filter(p -> p.isOnline() && p.getStatus() != PlayerStatus.ELIMINATED)
            .count();
    }

    // ... 其他方法 ...
}
```

### 1.5 客户端重连流程

**客户端逻辑**：

```javascript
// 客户端断线重连处理
class RoomReconnectHandler {
    constructor() {
        this.roomId = localStorage.getItem('currentRoomId');
        this.playerId = localStorage.getItem('playerId');
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 10;
    }

    async reconnect() {
        if (!this.roomId) {
            console.log('无房间信息，无需重连');
            return;
        }

        while (this.reconnectAttempts < this.maxReconnectAttempts) {
            try {
                // 尝试重新连接
                const response = await fetch(`/api/room/${this.roomId}/reconnect`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${localStorage.getItem('token')}`
                    },
                    body: JSON.stringify({
                        playerId: this.playerId
                    })
                });

                if (response.ok) {
                    const roomState = await response.json();
                    console.log('重连成功，恢复房间状态');
                    this.restoreRoomState(roomState);
                    return;
                } else if (response.status === 404) {
                    console.log('房间已销毁，清除本地存储');
                    localStorage.removeItem('currentRoomId');
                    this.showRoomDestroyedMessage();
                    return;
                }

            } catch (error) {
                console.error(`重连失败（第${this.reconnectAttempts + 1}次）`, error);
            }

            this.reconnectAttempts++;
            await this.sleep(2000); // 等待2秒后重试
        }

        console.error('重连失败，显示错误页面');
        this.showReconnectFailedMessage();
    }

    sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
}
```

### 1.6 关键设计决策

| 决策点 | 推荐方案 | 理由 |
|--------|---------|------|
| **清理策略** | 事件驱动 + 定期扫描 | 事件驱动响应快，定期扫描兜底安全 |
| **空闲阈值** | 30分钟 | 符合熟人社交场景，平衡内存和用户体验 |
| **清理延迟** | 2分钟 | 给玩家充分重连时间，防止误删 |
| **扫描频率** | 每分钟 | 1000房间规模下可快速发现遗漏 |
| **重连时间窗** | 5分钟 | 客户端localStorage保留，支持断线重连 |

### 1.7 风险与缓解

| 风险 | 严重度 | 缓解措施 |
|------|--------|----------|
| **重连期间房间被删除** | 🟡 中 | 2分钟延迟 + 清理前二次检查 + 客户端错误提示 |
| **定时器任务泄露** | 🟡 中 | ConcurrentHashMap + 取消逻辑 + shutdown清理 |
| **高负载扫描慢** | 🟢 低 | 并行流处理 + 限制每次扫描数量 |
| **重连玩家数据丢失** | 🟡 中 | localStorage保留roomId + 清理时返回404 |

---

## 2️⃣ 房主自动转移 (Host Migration)

### 问题陈述
房主掉线或退出后，房间将变为无主状态，无法开始游戏。

### 推荐方案：座位号优先级 + 异步广播

**核心思想**：
- **自动转移**：房主离开/掉线立即触发转移
- **优先级规则**：seatIndex最小的在座玩家成为新房主
- **异步广播**：避免锁内IO，提升并发性能

### 2.1 核心实现 - Room 类

```java
public class Room {

    // ... 现有字段 ...

    /**
     * 房主ID
     */
    private volatile String hostId;

    // WebSocket 消息模板（通过构造函数注入）
    private final SimpMessagingTemplate messagingTemplate;

    public Room(String roomId, String hostId, SimpMessagingTemplate messagingTemplate) {
        this.roomId = roomId;
        this.hostId = hostId;
        this.messagingTemplate = messagingTemplate;

        // 标记初始房主
        Player host = players.get(hostId);
        if (host != null) {
            host.setHost(true);
        }
    }

    /**
     * 移除玩家（含房主转移逻辑）
     *
     * @param playerId 玩家ID
     * @return 生成的事件列表
     */
    public List<GameEvent> removePlayer(String playerId) {
        List<GameEvent> events = new ArrayList<>();

        lock.lock();
        try {
            Player player = players.get(playerId);
            if (player == null) {
                log.debug("玩家[{}]不在房间[{}]中", playerId, roomId);
                return events;
            }

            boolean wasHost = player.isHost();

            // 移除玩家
            players.remove(playerId);
            if (player.isSeated()) {
                seats[player.getSeatIndex()] = null;
            }

            log.info("玩家[{}]离开房间[{}]，当前{}人", playerId, roomId, players.size());

            // 更新活动时间
            updateActivityTime();

            // 生成玩家离开事件
            events.add(GameEvent.playerLeft(playerId));

            // 如果移除的是房主，触发房主转移
            if (wasHost && !players.isEmpty()) {
                GameEvent hostTransferEvent = transferHost();
                if (hostTransferEvent != null) {
                    events.add(hostTransferEvent);
                }
            }

            // 检查房间是否为空，触发清理
            if (players.isEmpty()) {
                // 通知RoomLifecycleManager
                // eventPublisher.publishEvent(new RoomEmptyEvent(roomId));
            }

        } finally {
            lock.unlock();
        }

        return events;
    }

    /**
     * 转移房主给seatIndex最小的在座玩家
     *
     * @return 房主转移事件
     */
    private GameEvent transferHost() {
        // 1. 查找候选玩家（在座、在线）
        List<Player> candidates = players.values().stream()
            .filter(Player::isSeated)
            .filter(Player::isOnline)
            .sorted(Comparator.comparingInt(Player::getSeatIndex))
            .toList();

        if (candidates.isEmpty()) {
            log.warn("房间[{}]无有效房主候选人", roomId);
            return null;
        }

        // 2. 选择seatIndex最小的
        Player newHost = candidates.get(0);
        String oldHostId = this.hostId;
        Player oldHost = players.get(oldHostId);

        // 3. 更新房主
        this.hostId = newHost.getId();

        // 更新玩家标记
        newHost.setHost(true);
        if (oldHost != null) {
            oldHost.setHost(false);
        }

        log.info("房主转移: {}[{}] → {}[{}]",
            oldHost != null ? oldHost.getNickname() : "无", oldHostId,
            newHost.getNickname(), newHost.getSeatIndex());

        // 4. 返回房主转移事件（锁外广播）
        return GameEvent.hostTransferred(oldHostId, newHost.getId(), gameState.getStateVersion());
    }
}
```

### 2.2 Player 类补充

```java
public class Player {

    // ... 现有字段 ...

    /**
     * 是否为房主
     */
    private boolean isHost;

    /**
     * 是否在线（由WebSocket心跳更新）
     */
    private volatile boolean online;

    // Getters and Setters
    public boolean isHost() {
        return isHost;
    }

    public void setHost(boolean host) {
        isHost = host;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }
}
```

### 2.3 WebSocket 事件定义

```java
public record GameEvent(EventType type, Object data) {

    /**
     * 房主转移事件
     */
    public static GameEvent hostTransferred(String oldHostId, String newHostId, long stateVersion) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("oldHostId", oldHostId);
        eventData.put("newHostId", newHostId);
        eventData.put("stateVersion", stateVersion);

        return new GameEvent(EventType.HOST_TRANSFERRED, eventData);
    }

    /**
     * 玩家离开事件
     */
    public static GameEvent playerLeft(String playerId) {
        return new GameEvent(EventType.PLAYER_LEFT, Map.of("playerId", playerId));
    }
}

public enum EventType {
    SYNC_STATE,
    PLAYER_LEFT,
    HOST_TRANSFERRED,
    // ... 其他事件类型 ...
}
```

### 2.4 WebSocket 广播逻辑（锁外执行）

```java
@Service
public class GameEventBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public GameEventBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 广播事件列表（异步执行）
     */
    @Async
    public void broadcast(String roomId, List<GameEvent> events) {
        for (GameEvent event : events) {
            switch (event.getType()) {
                case SYNC_STATE:
                    messagingTemplate.convertAndSend(
                        "/topic/room/" + roomId,
                        Map.of("type", "SYNC_STATE", "data", event.data())
                    );
                    break;

                case PLAYER_LEFT:
                    messagingTemplate.convertAndSend(
                        "/topic/room/" + roomId,
                        Map.of("type", "PLAYER_LEFT", "data", event.data())
                    );
                    break;

                case HOST_TRANSFERRED:
                    messagingTemplate.convertAndSend(
                        "/topic/room/" + roomId,
                        Map.of("type", "HOST_TRANSFERRED", "data", event.data())
                    );
                    break;

                default:
                    log.warn("未知事件类型: {}", event.getType());
            }
        }
    }
}
```

### 2.5 关键设计决策

| 决策点 | 推荐方案 | 理由 |
|--------|---------|------|
| **转移规则** | seatIndex最小优先 | 简单、确定性、符合直觉 |
| **权限验证** | 仅需在线验证 | MVP阶段不需要筹码/时间要求 |
| **广播时机** | 异步广播 | 避免锁内IO，提升并发性能 |
| **触发条件** | 房主离开/掉线立即触发 | 最快响应，用户体验好 |

### 2.6 权限验证权衡分析

| 验证项 | 建议 | 权衡 |
|--------|------|------|
| **最小筹码要求** | ❌ 不需要 | 熟人社交场景不适用 |
| **在线时长要求** | ❌ 不需要 | 过度设计，增加复杂度 |
| **在线状态检查** | ✅ 必需 | 防止掉线玩家成为房主 |
| **在座要求** | ✅ 必需 | 避免观战玩家成为房主 |

### 2.7 风险与缓解

| 风险 | 严重度 | 缓解措施 |
|------|--------|----------|
| **频繁房主转移混乱** | 🟢 低 | WebSocket心跳监控在线状态（10秒） |
| **新玩家立即成为房主** | 🟢 低 | 可配置"至少参与X手牌"规则（P2功能） |
| **异步广播延迟** | 🟡 中 | 使用@Async + 线程池监控 |
| **房主掉线检测延迟** | 🟡 中 | STOMP心跳(10s) + 超时触发(30s) |

---

## 3️⃣ 定时器并发争抢 (Timer Retry)

### 问题陈述
30秒倒计时触发时，如果无法获取房间锁（tryLock 失败），超时逻辑会被跳过。

### 推荐方案：指数退避 + 3次重试上限

**核心思想**：
- **指数退避**：500ms → 1000ms → 2000ms，平衡响应时间和系统负载
- **重试上限**：3次后放弃，避免无限重试资源耗尽
- **失败降级**：记录告警，通知运维

### 3.1 核心实现 - TimerManager

```java
@Service
@Slf4j
public class TimerManager {

    private final RoomManager roomManager;
    private final AlertService alertService;

    // 定时器线程池
    private final ScheduledExecutorService scheduler;

    // 房间定时器映射：roomId -> ScheduledFuture
    private final ConcurrentHashMap<String, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();

    // 重试配置
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_DELAY_MS = 500;
    private static final double BACKOFF_MULTIPLIER = 2.0;

    public TimerManager(RoomManager roomManager, AlertService alertService) {
        this.roomManager = roomManager;
        this.alertService = alertService;

        this.scheduler = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors(),
            new ThreadFactoryBuilder()
                .setNameFormat("room-timer-%d")
                .setDaemon(true)
                .build()
        );
    }

    /**
     * 启动房间定时器
     *
     * @param roomId  房间ID
     * @param action  要执行的操作
     * @param delayMs 延迟时间（毫秒）
     */
    public void scheduleRoomAction(String roomId, Runnable action, long delayMs) {
        // 取消已存在的定时器
        cancelTimer(roomId);

        // 安排新的定时器
        ScheduledFuture<?> future = scheduler.schedule(
            () -> executeWithRetry(roomId, action, 0),
            delayMs,
            TimeUnit.MILLISECONDS
        );

        timers.put(roomId, future);
        log.debug("房间[{}]定时器已安排，{}ms后执行", roomId, delayMs);
    }

    /**
     * 取消房间定时器
     *
     * @param roomId 房间ID
     */
    public void cancelTimer(String roomId) {
        ScheduledFuture<?> future = timers.remove(roomId);
        if (future != null && !future.isDone()) {
            future.cancel(false);
            log.debug("房间[{}]定时器已取消", roomId);
        }
    }

    /**
     * 带指数退避重试的执行逻辑
     *
     * @param roomId     房间ID
     * @param action     要执行的操作
     * @param retryCount 当前重试次数
     */
    private void executeWithRetry(String roomId, Runnable action, int retryCount) {
        try {
            // 获取房间
            Room room = roomManager.getRoom(roomId);
            if (room == null) {
                log.debug("房间[{}]已不存在，取消定时器", roomId);
                timers.remove(roomId);
                return;
            }

            // 尝试获取锁（最多5秒）
            boolean acquired = room.lock().tryLock(5, TimeUnit.SECONDS);
            if (!acquired) {
                throw new ConcurrentLockException("获取房间锁失败，可能存在并发争抢");
            }

            try {
                // 执行操作
                action.run();
                log.debug("房间[{}]定时器执行成功", roomId);

                // 执行成功，清除定时器映射
                timers.remove(roomId);

            } finally {
                room.lock().unlock();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("房间[{}]定时器执行被中断", roomId, e);
            timers.remove(roomId);

        } catch (ConcurrentLockException e) {
            // 锁竞争，指数退避重试
            if (retryCount < MAX_RETRIES) {
                long delayMs = (long) (INITIAL_DELAY_MS * Math.pow(BACKOFF_MULTIPLIER, retryCount));

                log.warn("房间[{}]定时器锁竞争（第{}次重试），{}ms后重试",
                    roomId, retryCount + 1, delayMs);

                scheduler.schedule(
                    () -> executeWithRetry(roomId, action, retryCount + 1),
                    delayMs,
                    TimeUnit.MILLISECONDS
                );
            } else {
                log.error("房间[{}]定时器重试{}次后仍失败，放弃执行", roomId, MAX_RETRIES);

                // 触发降级处理：记录错误、通知管理员
                alertService.sendAlert("定时器重试失败", roomId,
                    "房间[" + roomId + "]定时器重试" + MAX_RETRIES + "次后仍失败");

                timers.remove(roomId);
            }

        } catch (Exception e) {
            log.error("房间[{}]定时器执行异常", roomId, e);

            // 其他异常不重试，记录告警
            alertService.sendAlert("定时器执行异常", roomId, e.getMessage());

            timers.remove(roomId);
        }
    }

    /**
     * 应用关闭时的清理
     */
    @PreDestroy
    public void shutdown() {
        log.info("TimerManager 关闭中...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```

### 3.2 自定义异常类

```java
/**
 * 并发锁异常
 */
public class ConcurrentLockException extends RuntimeException {
    public ConcurrentLockException(String message) {
        super(message);
    }
}
```

### 3.3 告警服务接口

```java
/**
 * 告警服务（简单实现）
 */
@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    /**
     * 发送告警
     *
     * @param type   告警类型
     * @param roomId 房间ID
     * @param message 告警消息
     */
    public void sendAlert(String type, String roomId, String message) {
        // 简单实现：记录到日志
        log.error("告警[{}] 房间[{}]: {}", type, roomId, message);

        // 生产环境可集成：
        // - 钉钉/企业微信机器人
        // - Sentry 错误追踪
        // - Prometheus 告警规则
    }
}
```

### 3.4 使用示例 - 超时处理

```java
@Service
public class GameController {

    private final TimerManager timerManager;

    /**
     * 开始玩家回合
     *
     * @param roomId  房间ID
     * @param playerId 玩家ID
     */
    public void startPlayerTurn(String roomId, String playerId) {
        Room room = roomManager.getRoom(roomId);
        if (room == null) {
            return;
        }

        // 更新游戏状态
        // ...

        // 启动30秒超时定时器
        timerManager.scheduleRoomAction(roomId, () -> handleTimeout(roomId, playerId), 30_000);
    }

    /**
     * 处理超时
     *
     * @param roomId  房间ID
     * @param playerId 玩家ID
     */
    private void handleTimeout(String roomId, String playerId) {
        Room room = roomManager.getRoom(roomId);
        if (room == null) {
            return;
        }

        try {
            // 获取房间锁（由TimerManager内部处理重试）
            if (room.lock().tryLock(5, TimeUnit.SECONDS)) {
                try {
                    // 检查是否仍为该玩家的回合
                    if (room.getCurrentPlayerId().equals(playerId)) {
                        // 自动弃牌
                        List<GameEvent> events = room.processFold(playerId, true);
                        // 广播事件...
                    }
                } finally {
                    room.lock().unlock();
                }
            }
        } catch (Exception e) {
            log.error("处理超时异常", e);
        }
    }
}
```

### 3.5 关键设计决策

| 决策点 | 推荐方案 | 理由 |
|--------|---------|------|
| **重试策略** | 指数退避 | 平衡响应时间和系统负载 |
| **重试次数** | 3次上限 | 避免无限重试资源耗尽 |
| **初始延迟** | 500ms | 快速响应首次争抢 |
| **失败降级** | 告警+记录 | 确保问题可追溯 |

### 3.6 指数退避 vs 固定延迟

| 策略 | 延迟序列(ms) | 优点 | 缺点 |
|------|--------------|------|------|
| **固定延迟** | 500, 500, 500 | 简单 | 可能加剧争抢 |
| **指数退避** | 500, 1000, 2000 | 系统负载平衡 | 失败总时间长 |
| **线性退避** | 500, 1000, 1500 | 折中方案 | 平衡性一般 |

### 3.7 风险与缓解

| 风险 | 严重度 | 缓解措施 |
|------|--------|----------|
| **定时器任务泄露** | 🟡 中 | ConcurrentHashMap + 取消逻辑 + shutdown清理 |
| **高频重试风暴** | 🟡 中 | 指数退避 + 重试上限 |
| **锁超时死循环** | 🟡 中 | 5秒超时 + 日志告警 |
| **操作幂等性** | 🟢 低 | requestId + stateVersion双重校验 |

---

## 4️⃣ 补充筹码与复活 (Rebuy System)

### 问题陈述
玩家输光后只能退房，无法补充筹码继续玩。

### 推荐方案：Hand End时刻执行 + 频率限制

**核心思想**：
- **时机限制**：只能在手牌结束后（Hand End）或自己未参与时执行
- **状态转换**：ELIMINATED → WAITING，保持座位，下一局自动参与
- **防作弊**：频率限制、手数限制、金额上限

### 4.1 状态机设计

```
    ┌───────────┐
    │  ACTIVE   │ (参与当前手牌)
    └─────┬─────┘
          │ Chips = 0 (手牌结束)
          ▼
    ┌───────────┐
    │ELIMINATED │ ◄─── 保持座位，不发牌，观战状态
    └─────┬─────┘
          │ Rebuy Request (Hand End)
          │ + 频率检查通过
          │ + 手数检查通过
          ▼
    ┌───────────┐
    │  WAITING  │ ◄─── 等待下一局开始，将自动参与
    └─────┬─────┘
          │ 新手牌开始
          ▼
    ┌───────────┐
    │  ACTIVE   │
    └───────────┘
```

### 4.2 核心实现 - RebuyService

```java
@Service
@Slf4j
public class RebuyService {

    private final RoomManager roomManager;

    // 频率限制配置
    private static final int MAX_REBUY_PER_HOUR = 3;
    private static final int MIN_HANDS_BEFORE_REBUY = 2;
    private static final int MAX_CHIPS_TOTAL = 10000; // 单人总筹码上限

    // 玩家Rebuy记录
    private final ConcurrentHashMap<String, PlayerRebuyRecord> rebuyRecords = new ConcurrentHashMap<>();

    public RebuyService(RoomManager roomManager) {
        this.roomManager = roomManager;
    }

    /**
     * 请求Rebuy
     *
     * @param roomId  房间ID
     * @param playerId 玩家ID
     * @param amount   补充筹码数量
     * @return Rebuy结果
     */
    public RebuyResult requestRebuy(String roomId, String playerId, int amount) {
        Room room = roomManager.getRoom(roomId);
        if (room == null) {
            return RebuyResult.error("房间不存在");
        }

        // 获取房间锁
        boolean acquired = room.lock().tryLock(5, TimeUnit.SECONDS);
        if (!acquired) {
            return RebuyResult.error("系统繁忙，请稍后重试");
        }

        try {
            Player player = room.getPlayer(playerId);
            if (player == null) {
                return RebuyResult.error("玩家不存在");
            }

            // 1. 验证金额
            if (amount <= 0) {
                return RebuyResult.error("补充筹码必须大于0");
            }

            // 2. 验证状态（必须ELIMINATED）
            if (player.getStatus() != PlayerStatus.ELIMINATED) {
                return RebuyResult.error("只有在淘汰后才能补充筹码");
            }

            // 3. 验证时机（Hand End）
            if (room.isPlaying() && !room.isHandEnding()) {
                return RebuyResult.error("只能在手牌结束后补充筹码");
            }

            // 4. 验证总筹码上限
            if (amount > MAX_CHIPS_TOTAL) {
                return RebuyResult.error("单次补充筹码超过上限");
            }

            // 5. 验证频率（每小时最多N次）
            PlayerRebuyRecord record = rebuyRecords.computeIfAbsent(playerId,
                k -> new PlayerRebuyRecord());
            if (!record.canRebuy(MAX_REBUY_PER_HOUR)) {
                long waitTime = record.getWaitTimeUntilNextRebuy();
                return RebuyResult.error("操作过于频繁，请" + waitTime + "秒后再试");
            }

            // 6. 验证最低参与手数
            int handsSinceLastRebuy = room.getHandNumber() - record.getLastRebuyHandNumber();
            if (handsSinceLastRebuy < MIN_HANDS_BEFORE_REBUY) {
                return RebuyResult.error("至少参与" + MIN_HANDS_BEFORE_REBUY + "手牌后才能补充");
            }

            // 7. 执行Rebuy
            player.addChips(amount);
            player.setStatus(PlayerStatus.WAITING);
            record.recordRebuy(room.getHandNumber(), amount);

            log.info("玩家[{}]Rebuy成功: {}筹码，当前总筹码: {}", playerId, amount, player.getChips());

            // 更新房间活动时间
            room.updateActivityTime();

            // 返回成功结果
            return RebuyResult.success(player.getChips());

        } finally {
            room.lock().unlock();
        }
    }

    /**
     * Rebuy结果
     */
    @Data
    @Builder
    public static class RebuyResult {
        private boolean success;
        private String message;
        private Integer currentChips;

        public static RebuyResult success(int currentChips) {
            return RebuyResult.builder()
                .success(true)
                .message("补充筹码成功")
                .currentChips(currentChips)
                .build();
        }

        public static RebuyResult error(String message) {
            return RebuyResult.builder()
                .success(false)
                .message(message)
                .build();
        }
    }

    /**
     * 玩家Rebuy记录（防作弊）
     */
    @Data
    private static class PlayerRebuyRecord {
        private final List<Long> rebuyTimestamps = new ArrayList<>();
        private int lastRebuyHandNumber;
        private int totalRebuyAmount;

        public boolean canRebuy(int maxPerHour) {
            long oneHourAgo = System.currentTimeMillis() - 3600000;

            // 清理超过1小时的记录
            rebuyTimestamps.removeIf(t -> t < oneHourAgo);

            // 检查频率
            return rebuyTimestamps.size() < maxPerHour;
        }

        public void recordRebuy(int handNumber, int amount) {
            rebuyTimestamps.add(System.currentTimeMillis());
            lastRebuyHandNumber = handNumber;
            totalRebuyAmount += amount;
        }

        public long getWaitTimeUntilNextRebuy() {
            if (rebuyTimestamps.isEmpty()) return 0;
            long earliest = rebuyTimestamps.get(0);
            long oneHourAgo = System.currentTimeMillis() - 3600000;
            return Math.max(0, earliest + 3600000 - oneHourAgo) / 1000;
        }
    }
}
```

### 4.3 Room 类补充

```java
public class Room {

    // ... 现有字段 ...

    /**
     * 检查是否正在游戏
     */
    public boolean isPlaying() {
        return gameState.getPhase() != GamePhase.IDLE;
    }

    /**
     * 检查是否手牌结束阶段
     */
    public boolean isHandEnding() {
        return gameState.getPhase() == GamePhase.SHOWDOWN;
    }

    /**
     * 获取当前手牌序号
     */
    public int getHandNumber() {
        return gameState.getHandNumber();
    }
}
```

### 4.4 WebSocket 消息类型

```java
/**
 * Rebuy请求消息
 */
@Data
public class RebuyRequestMessage {
    private String roomId;
    private String playerId;
    private int amount;
}

/**
 * Rebuy响应消息
 */
@Data
public class RebuyResponseMessage {
    private boolean success;
    private String message;
    private Integer currentChips;
}
```

### 4.5 WebSocket 控制器

```java
@Controller
public class GameControllerWebSocket {

    private final RebuyService rebuyService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 处理Rebuy请求
     */
    @MessageMapping("/rebuy")
    @SendTo("/topic/room/{roomId}")
    public RebuyResponseMessage handleRebuy(
        @DestinationVariable String roomId,
        RebuyRequestMessage request) {

        RebuyResult result = rebuyService.requestRebuy(
            roomId, request.getPlayerId(), request.getAmount());

        return new RebuyResponseMessage(
            result.isSuccess(),
            result.getMessage(),
            result.getCurrentChips()
        );
    }
}
```

### 4.6 关键设计决策

| 决策点 | 推荐方案 | 理由 |
|--------|---------|------|
| **状态转换** | ELIMINATED → WAITING | 保持座位，下一局自动参与 |
| **执行时机** | Hand End时刻 | 确保游戏一致性，防止中途干扰 |
| **频率限制** | 每小时最多3次 | 防止作弊，平衡用户体验 |
| **手数限制** | 至少2手牌 | 防止立即Rebuy规避损失 |

### 4.7 执行时机权衡分析

| 时机 | 一致性 | 实时性 | 复杂度 | 防作弊 |
|------|--------|--------|--------|--------|
| **Hand End** | ✅ 高 | ❌ 低 | ✅ 低 | ✅ 高 |
| **任意时刻** | ❌ 低 | ✅ 高 | ❌ 高 | ⚠️ 中 |
| **盲注级别** | ⚠️ 中 | ⚠️ 中 | ⚠️ 中 | ✅ 高 |

### 4.8 防作弊机制

| 机制 | 实现 | 作用 |
|------|------|------|
| **频率限制** | 每小时最多N次Rebuy | 防止频繁调整筹码 |
| **手数限制** | 至少参与X手牌 | 防止立即Rebuy规避损失 |
| **金额上限** | 单人总筹码上限 | 防止刷筹码 |
| **IP监控** | 后期可接入（P2） | 关联账户检测 |

### 4.9 风险与缓解

| 风险 | 严重度 | 缓解措施 |
|------|--------|----------|
| **等待Rebuy期间无聊** | 🟢 低 | 客户端显示倒计时+说明 |
| **关联账户作弊** | 🟡 中 | IP监控+行为分析（P2功能） |
| **金额溢出** | 🟢 低 | ChipCalculator溢出检查 |
| **Rebuy记录泄露** | 🟢 低 | ConcurrentHashMap内存存储 |

---

## 5️⃣ 逻辑与IO分离 (Logic/IO Separation)

### 问题陈述
防止在锁内执行耗时IO（如WebSocket广播）导致阻塞。

### 推荐方案：直接返回事件列表（避免过度设计）

**核心思想**：
- **锁内计算**：只修改内存状态，生成事件列表
- **锁外广播**：所有IO操作在锁外异步执行
- **轻量级事件**：事件列表自动GC，无需额外管理

### 5.1 核心原则

```java
// ❌ 错误：锁内执行IO
public void processActionBad(String roomId, PlayerAction action) {
    room.lock();
    try {
        // 计算新状态
        GameState newState = calculateState(action);

        // ❌ 错误：锁内广播，阻塞其他操作
        messagingTemplate.convertAndSend("/topic/room/" + roomId, newState);
    } finally {
        room.unlock();
    }
}

// ✅ 正确：锁内计算，锁外IO
public List<GameEvent> processActionGood(String roomId, PlayerAction action) {
    List<GameEvent> events;

    room.lock();
    try {
        // 计算新状态
        GameState newState = calculateState(action);
        room.updateState(newState);

        // ✅ 正确：生成事件列表（纯内存操作）
        events = generateEvents(newState);
    } finally {
        room.unlock();
    }

    // ✅ 正确：锁外异步广播
    asyncBroadcast(roomId, events);

    return events;
}
```

### 5.2 核心实现 - GameController

```java
@Service
@Slf4j
public class GameController {

    private final RoomManager roomManager;
    private final GameEventBroadcaster eventBroadcaster;

    /**
     * 处理玩家操作，返回事件列表
     *
     * @param roomId  房间ID
     * @param playerId 玩家ID
     * @param payload 操作负载
     * @return 生成的事件列表
     */
    public List<GameEvent> processAction(String roomId, String playerId, PlayerActionPayload payload) {
        List<GameEvent> events = new ArrayList<>();

        Room room = roomManager.getRoom(roomId);
        if (room == null) {
            log.warn("房间[{}]不存在", roomId);
            return events;
        }

        // 获取房间锁
        boolean acquired = room.lock().tryLock(5, TimeUnit.SECONDS);
        if (!acquired) {
            log.warn("房间[{}]处理超时", roomId);
            throw new IllegalStateException("房间处理超时");
        }

        try {
            // 1. 幂等校验
            if (!validateRequestId(room, payload.getRequestId())) {
                log.debug("重复请求，忽略: roomId={}, requestId={}", roomId, payload.getRequestId());
                return events;
            }

            // 2. 执行游戏逻辑（纯计算）
            GameState newState = calculateNewState(room, playerId, payload);

            // 3. 更新房间状态
            room.updateGameState(newState);

            // 4. 更新活动时间
            room.updateActivityTime();

            // 5. 生成事件列表（IO操作在锁外）
            events.addAll(generateEvents(newState, room));

            log.debug("房间[{}]操作处理完成，生成{}个事件", roomId, events.size());

        } finally {
            room.unlock();
        }

        // 6. 锁外执行IO（广播、日志等）
        eventBroadcaster.broadcast(roomId, events);

        return events;
    }

    /**
     * 幂等校验
     */
    private boolean validateRequestId(Room room, String requestId) {
        if (requestId == null) {
            return true; // 允许无requestId的请求（兼容旧客户端）
        }

        String processedId = room.getLastProcessedRequestId();
        if (requestId.equals(processedId)) {
            return false; // 重复请求
        }

        room.setLastProcessedRequestId(requestId);
        return true;
    }

    /**
     * 计算新状态（纯函数）
     */
    private GameState calculateNewState(Room room, String playerId, PlayerActionPayload payload) {
        // 调用PokerEngine计算新状态
        // ...

        return room.getGameState();
    }

    /**
     * 生成事件列表
     */
    private List<GameEvent> generateEvents(GameState newState, Room room) {
        List<GameEvent> events = new ArrayList<>();

        // 1. 状态同步事件
        events.add(GameEvent.syncState(room.toPublicDTO()));

        // 2. 玩家行动事件
        if (newState.getLastAction() != null) {
            events.add(GameEvent.playerActed(newState.getLastAction()));
        }

        // 3. 阶段变化事件
        if (newState.isPhaseChanged()) {
            events.add(GameEvent.phaseChanged(newState.getPhase()));
        }

        // 4. 手牌结束事件
        if (newState.isHandEnd()) {
            events.addAll(generateHandResultEvents(newState));
        }

        return events;
    }

    /**
     * 生成手牌结果事件
     */
    private List<GameEvent> generateHandResultEvents(GameState gameState) {
        List<GameEvent> events = new ArrayList<>();

        // TODO: 摊牌结算逻辑
        // events.add(GameEvent.handResult(result));

        return events;
    }
}
```

### 5.3 事件定义

```java
/**
 * 游戏事件（轻量级）
 */
@Data
@AllArgsConstructor
public class GameEvent {
    private final EventType type;
    private final Object data;

    /**
     * 状态同步事件
     */
    public static GameEvent syncState(PublicRoomInfo room) {
        return new GameEvent(EventType.SYNC_STATE, room);
    }

    /**
     * 玩家行动事件
     */
    public static GameEvent playerActed(PlayerAction action) {
        return new GameEvent(EventType.PLAYER_ACTED, action);
    }

    /**
     * 阶段变化事件
     */
    public static GameEvent phaseChanged(GamePhase phase) {
        return new GameEvent(EventType.PHASE_CHANGED, phase);
    }
}

/**
 * 事件类型枚举
 */
public enum EventType {
    SYNC_STATE,        // 状态同步
    PLAYER_ACTED,      // 玩家行动
    PHASE_CHANGED,     // 阶段变化
    HAND_RESULT,       // 手牌结果
    PLAYER_LEFT,       // 玩家离开
    HOST_TRANSFERRED,  // 房主转移
    // ...
}
```

### 5.4 广播服务（异步）

```java
@Service
@Slf4j
public class GameEventBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 广播事件列表（异步执行）
     *
     * @param roomId 房间ID
     * @param events 事件列表
     */
    @Async
    public void broadcast(String roomId, List<GameEvent> events) {
        for (GameEvent event : events) {
            try {
                switch (event.getType()) {
                    case SYNC_STATE:
                        messagingTemplate.convertAndSend(
                            "/topic/room/" + roomId,
                            Map.of("type", "SYNC_STATE", "data", event.data())
                        );
                        break;

                    case PLAYER_ACTED:
                        messagingTemplate.convertAndSend(
                            "/topic/room/" + roomId,
                            Map.of("type", "PLAYER_ACTED", "data", event.data())
                        );
                        break;

                    case PHASE_CHANGED:
                        messagingTemplate.convertAndSend(
                            "/topic/room/" + roomId,
                            Map.of("type", "PHASE_CHANGED", "data", event.data())
                        );
                        break;

                    case HAND_RESULT:
                        messagingTemplate.convertAndSend(
                            "/topic/room/" + roomId,
                            Map.of("type", "HAND_RESULT", "data", event.data())
                        );
                        break;

                    default:
                        log.warn("未知事件类型: {}", event.getType());
                }
            } catch (Exception e) {
                log.error("广播事件失败: type={}, roomId={}", event.getType(), roomId, e);
            }
        }
    }
}
```

### 5.5 DTO体系

```java
/**
 * 公开房间信息（广播给所有玩家）
 */
@Data
@Builder
public class PublicRoomInfo {
    private String roomId;
    private String hostId;
    private List<PublicPlayerInfo> players;
    private GamePhase phase;
    private int currentBet;
    private long stateVersion;

    // ... 游戏状态字段
}

/**
 * 公开玩家信息（无手牌）
 */
@Data
@Builder
public class PublicPlayerInfo {
    private String id;
    private String nickname;
    private int chips;
    private PlayerStatus status;
    private int seatIndex;
    private boolean isHost;
    private boolean isOnline;

    // 不包含：holeCards（私密信息）
}

/**
 * 私密玩家信息（包含手牌，仅发送给该玩家）
 */
@Data
@Builder
public class PrivatePlayerInfo extends PublicPlayerInfo {
    private List<Card> holeCards;
}
```

### 5.6 架构对比

| 方案 | 复杂度 | 性能 | 可测试性 | 适用场景 |
|------|--------|------|----------|----------|
| **直接返回事件列表** | ✅ 低 | ✅ 高 | ✅ 高 | 单机、MVP |
| **EventBus** | ❌ 高 | ⚠️ 中 | ⚠️ 中 | 微服务、复杂事件流 |
| **Event Sourcing** | ❌ 极高 | ⚠️ 低 | ⚠️ 中 | 审计需求、复杂状态 |

### 5.7 一致性模型

**推荐：最终一致性（延迟<10ms）**

```java
// 最终一致性实现
public List<GameEvent> processAction(...) {
    // 锁内：计算状态
    GameState newState = calculateNewState(...);
    room.updateGameState(newState);

    // 锁外：生成事件列表
    List<GameEvent> events = generateEvents(newState);

    // 锁外：异步广播
    asyncBroadcast(events);

    return events;
}
```

**为什么选择最终一致性**：
- ✅ 游戏状态更新在锁内，保证单房间强一致
- ✅ WebSocket广播延迟<10ms可接受
- ✅ 客户端有stateVersion校验，可忽略过期消息
- ✅ 避免过度设计，实现简单

### 5.8 Event Sourcing适用性分析

❌ **不推荐Event Sourcing**：

| 维度 | 评估 | 原因 |
|------|------|------|
| **状态复杂度** | 🟢 低 | 扑克游戏状态相对简单 |
| **审计需求** | 🟢 低 | 熟人社交，无需完整审计 |
| **重放需求** | 🟢 低 | 不需要重现历史状态 |
| **存储成本** | 🟡 中 | 事件流会显著增加存储 |
| **实现复杂度** | 🔴 高 | 需要快照、压缩、删除策略 |

**何时考虑Event Sourcing**：
- 未来需要完整的对局回放功能
- 需要详细的作弊检测和审计
- 状态变化极其复杂，难以直接重建

### 5.9 风险与缓解

| 风险 | 严重度 | 缓解措施 |
|------|--------|----------|
| **事件生成遗漏** | 🟡 中 | 完整的事件生成逻辑+单元测试 |
| **IO操作阻塞** | 🟢 低 | @Async异步执行 |
| **事件顺序错误** | 🟢 低 | 锁内生成，顺序保证 |
| **内存泄露** | 🟢 低 | 事件列表自动GC |

---

## 📊 实现优先级

### P0（必须实现，阻塞发布）

| 任务 | 工作量 | 风险 | 说明 |
|------|--------|------|------|
| **房间生命周期管理** | 2天 | 🟡 | 内存安全基础 |
| **定时器管理器** | 1.5天 | 🟡 | 并发安全核心 |
| **Rebuy系统** | 1天 | 🟢 | 用户体验关键 |

**总工期：4.5天**

### P1（强烈建议，影响稳定性）

| 任务 | 工作量 | 风险 | 说明 |
|------|--------|------|------|
| **房主转移机制** | 1天 | 🟢 | 用户体验提升 |
| **逻辑/IO分离重构** | 1天 | 🟡 | 代码质量提升 |

**总工期：2天**

### P2（可选优化，影响扩展性）

| 任务 | 工作量 | 风险 | 说明 |
|------|--------|------|------|
| **关联账户检测** | 2天 | 🟢 | 防作弊增强 |
| **Event Sourcing准备** | 3天 | 🟡 | 未来扩展预留 |

**总工期：5天**

---

## ✅ 与现有设计文档一致性

### 完全一致（无需调整）

1. ✅ **并发模型设计** - 所有方案遵循房间级锁原则
2. ✅ **状态版本化** - 所有状态变更携带stateVersion
3. ✅ **幂等校验** - requestId + roundIndex双重校验保持不变
4. ✅ **锁外广播** - WebSocket广播始终在锁外执行
5. ✅ **数值计算规范** - 使用ChipCalculator安全计算

### 需要补充文档

1. ⚠️ **WebSocket协议扩展** - 增加新消息类型
2. ⚠️ **DTO体系扩展** - 增加事件和请求DTO
3. ⚠️ **测试策略补充** - 增加并发测试用例
4. ⚠️ **运维文档** - 房间清理策略和监控指标

### 需要更新文档

1. 🔴 **architecture.md** - 补充Rebuy状态机
2. 🔴 **PLAN.md** - 更新任务分解
3. 🔴 **concurrency-model.md** - 补充定时器争抢处理

---

## 📝 总结

### 核心原则

1. **务实优先** - 选择简单可靠的方案，避免过度设计
2. **性能平衡** - 最终一致性满足游戏场景需求
3. **安全第一** - 频率限制、溢出检查、锁超时防护
4. **渐进扩展** - P0功能完成后，再考虑P2增强

### 关键权衡

| 决策点 | 选择 | 原因 |
|--------|------|------|
| **Room GC** | 混合策略 | 响应快+安全网 |
| **房主转移** | 座位优先级 | 简单+确定性 |
| **定时器重试** | 指数退避3次 | 平衡响应+负载 |
| **Rebuy时机** | Hand End | 一致性+防作弊 |
| **Logic/IO分离** | 事件列表 | 避免过度设计 |

### 下一步行动

1. ✅ **评审通过** - 架构方案可进入实施
2. 📝 **更新文档** - 补充新协议定义和状态机
3. 🚀 **开始实施** - 按P0→P1→P2顺序开发
4. 🧪 **补充测试** - 并发测试+集成测试

**预计工期**：P0(4.5天) + P1(2天) = **6.5天**核心开发，P2按需补充。

---

**文档版本**: v1.0
**最后更新**: 2026-01-23
**维护者**: Sisyphus AI Assistant
