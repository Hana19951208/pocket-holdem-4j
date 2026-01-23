# STOMP 协议适配设计文档

> 文档版本: v1.0
> 创建日期: 2026-01-23
> 作者: AI 助手

---

## 📋 概述

本文档详细说明了从 Socket.io 迁移到 STOMP 的适配方案，包括自动重连机制、基于Session的房间管理、消息确认机制和完整的消息协议规范。这些设计确保 WebSocket 通信的可靠性和稳定性。

---

## 1. Socket.io 与 STOMP 的差异

### 1.1 特性对比表

| 特性 | Socket.io | STOMP |
|------|-----------|-------|
| **自动重连** | 内置 | 需手动实现 |
| **房间管理** | `socket.join('room')` | 需基于session管理 |
| **消息可靠性** | 有ack机制 | 需显式配置 |
| **跨域处理** | 配置简单 | 需CORS配置 |
| **二进制传输** | 原生支持 | 需转换器 |
| **心跳检测** | 内置 | 内置 |
| **消息类型** | 事件驱动 | 基于订阅/发布 |
| **协议** | 自定义协议 | STOMP协议（RFC 6455） |

### 1.2 Socket.io 示例（原项目）

```javascript
// 前端（Socket.io）
const socket = io('http://localhost:8080');

// 自动重连（内置）
socket.on('disconnect', () => {
    console.log('断线，自动重连中...');
});

// 房间管理（内置）
socket.emit('join-room', { roomId: '123456', nickname: 'Alice' });
socket.on('room-joined', (data) => {
    console.log('已加入房间', data);
});

// 私发消息（内置）
socket.emit('player-action', { action: 'FOLD', roundIndex: 1 });
socket.on('deal-cards', (cards) => {
    console.log('收到手牌', cards);
});
```

### 1.3 STOMP 示例（目标）

```javascript
// 前端（STOMP）
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

// 连接（需手动处理重连）
stompClient.connect({}, onConnected, onError);

function onConnected() {
    console.log('已连接到服务器');

    // 订阅房间公共频道
    stompClient.subscribe('/topic/room/123456', onRoomUpdated);

    // 订阅私聊频道（手牌等）
    stompClient.subscribe('/user/queue/private', onPrivateMessage);
}

function onError(error) {
    console.log('连接错误', error);
    reconnect();  // 手动重连
}
```

---

## 2. STOMP 层重连机制

### 2.1 后端部分

#### 配置 STOMP 为 ack 模式

```java
package com.pocketholdem.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 配置
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 启用简单消息代理（基于内存）
        registry.enableSimpleBroker("/topic", "/queue");

        // 设置应用程序前缀
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册 STOMP 端点
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")  // 允许跨域
            .withSockJS();  // 支持 SockJS 降级
    }
}
```

#### 未确认消息队列管理

```java
package com.pocketholdem.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.*;

/**
 * 未确认消息管理器
 */
@Slf4j
@Service
public class UnacknowledgedMessageManager {

    // 未确认消息队列：sessionId -> messageList
    private final Map<String, Queue<PendingMessage>> pendingMessages = new ConcurrentHashMap<>();

    // 消息重发定时器
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * 记录待确认消息
     */
    public void recordMessage(String sessionId, String destination, Object message) {
        PendingMessage pendingMessage = new PendingMessage(
            UUID.randomUUID().toString(),
            destination,
            message,
            System.currentTimeMillis()
        );

        pendingMessages.computeIfAbsent(sessionId, k -> new ConcurrentLinkedQueue<>())
            .add(pendingMessage);

        log.debug("记录待确认消息: sessionId={}, messageId={}", sessionId, pendingMessage.getMessageId());
    }

    /**
     * 确认消息
     */
    public void acknowledgeMessage(String sessionId, String messageId) {
        Queue<PendingMessage> queue = pendingMessages.get(sessionId);
        if (queue != null) {
            queue.removeIf(msg -> msg.getMessageId().equals(messageId));
            log.debug("消息已确认: sessionId={}, messageId={}", sessionId, messageId);
        }
    }

    /**
     * 清理超时未确认的消息
     */
    public void cleanupExpiredMessages() {
        long now = System.currentTimeMillis();
        long timeout = 30000;  // 30秒超时

        pendingMessages.forEach((sessionId, queue) -> {
            Iterator<PendingMessage> iterator = queue.iterator();
            while (iterator.hasNext()) {
                PendingMessage msg = iterator.next();
                if (now - msg.getTimestamp() > timeout) {
                    iterator.remove();
                    log.warn("消息超时未确认: sessionId={}, messageId={}", sessionId, msg.getMessageId());
                }
            }
        });
    }

    /**
     * 定时清理任务
     */
    @PostConstruct
    public void startCleanupTask() {
        scheduler.scheduleAtFixedRate(
            this::cleanupExpiredMessages,
            10,  // 初始延迟10秒
            10,  // 每10秒执行一次
            TimeUnit.SECONDS
        );
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
    }

    /**
     * 待确认消息
     */
    @Data
    @AllArgsConstructor
    private static class PendingMessage {
        private String messageId;
        private String destination;
        private Object message;
        private long timestamp;
    }
}
```

#### 超时重发机制

```java
package com.pocketholdem.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.*;

/**
 * 消息重发服务
 */
@Slf4j
@Service
public class MessageResendService {

    private final SimpMessagingTemplate messagingTemplate;
    private final UnacknowledgedMessageManager unackManager;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public MessageResendService(SimpMessagingTemplate messagingTemplate,
                               UnacknowledgedMessageManager unackManager) {
        this.messagingTemplate = messagingTemplate;
        this.unackManager = unackManager;
    }

    /**
     * 发送消息并记录待确认
     */
    public void sendWithAck(String sessionId, String destination, Object message) {
        String messageId = UUID.randomUUID().toString();

        // 1. 发送消息（包含messageId）
        Map<String, Object> payload = new HashMap<>();
        payload.put("messageId", messageId);
        payload.put("data", message);

        messagingTemplate.convertAndSendToUser(sessionId, destination, payload);

        // 2. 记录待确认消息
        unackManager.recordMessage(sessionId, destination, payload);

        // 3. 5秒后检查是否确认，未确认则重发
        scheduler.schedule(() -> {
            // 检查是否已确认
            boolean isAcked = checkMessageAcknowledged(sessionId, messageId);
            if (!isAcked) {
                log.warn("消息未确认，重发: sessionId={}, messageId={}", sessionId, messageId);
                messagingTemplate.convertAndSendToUser(sessionId, destination, payload);
            }
        }, 5, TimeUnit.SECONDS);
    }

    private boolean checkMessageAcknowledged(String sessionId, String messageId) {
        // 检查消息是否已确认（待实现）
        return false;
    }
}
```

### 2.2 前端部分

#### 封装 STOMP 连接管理器

```javascript
/**
 * STOMP 连接管理器
 */
class StompConnectionManager {
    constructor() {
        this.stompClient = null;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 10;
        this.reconnectDelay = 1000;  // 初始1秒
        this.maxReconnectDelay = 30000;  // 最大30秒
        this.isConnected = false;
        this.subscriptions = new Map();  // 订阅列表
    }

    /**
     * 连接到服务器
     */
    connect(onConnected, onError) {
        const socket = new SockJS('http://localhost:8080/ws');
        this.stompClient = Stomp.over(socket);

        this.stompClient.connect(
            {},  // 连接头信息
            () => {
                console.log('已连接到服务器');
                this.isConnected = true;
                this.reconnectAttempts = 0;
                this.reconnectDelay = 1000;

                // 重连成功后恢复订阅
                this.restoreSubscriptions();

                // 发送重连消息恢复游戏状态
                this.sendReconnectMessage();

                if (onConnected) onConnected();
            },
            (error) => {
                console.error('连接错误', error);
                this.isConnected = false;

                if (onError) onError(error);

                // 自动重连
                this.scheduleReconnect();
            }
        );
    }

    /**
     * 计划重连（指数退避）
     */
    scheduleReconnect() {
        if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            console.error('重连失败次数过多，停止重连');
            return;
        }

        const delay = Math.min(
            this.reconnectDelay * Math.pow(2, this.reconnectAttempts),
            this.maxReconnectDelay
        );

        console.log(`${delay}ms 后尝试第 ${this.reconnectAttempts + 1} 次重连...`);

        setTimeout(() => {
            this.reconnectAttempts++;
            this.connect(() => {}, (error) => {});
        }, delay);
    }

    /**
     * 恢复订阅
     */
    restoreSubscriptions() {
        console.log('恢复订阅...');
        this.subscriptions.forEach((callback, destination) => {
            this.subscribe(destination, callback);
        });
    }

    /**
     * 发送重连消息
     */
    sendReconnectMessage() {
        const roomId = localStorage.getItem('roomId');
        const playerId = localStorage.getItem('playerId');

        if (roomId && playerId) {
            console.log('发送重连消息:', { roomId, playerId });

            this.stompClient.send('/app/reconnect', {}, JSON.stringify({
                type: 'RECONNECT',
                roomId: roomId,
                playerId: playerId,
                requestId: generateRequestId(),
                timestamp: Date.now()
            }));
        }
    }

    /**
     * 订阅频道
     */
    subscribe(destination, callback) {
        const subscription = this.stompClient.subscribe(destination, (message) => {
            // ACK确认
            message.ack();

            // 调用回调
            if (callback) callback(JSON.parse(message.body));
        }, { ack: 'client' });

        // 保存订阅
        this.subscriptions.set(destination, subscription);
    }

    /**
     * 发送消息
     */
    send(destination, message) {
        if (!this.isConnected) {
            console.error('未连接到服务器，无法发送消息');
            return;
        }

        this.stompClient.send(destination, {}, JSON.stringify(message));
    }

    /**
     * 断开连接
     */
    disconnect() {
        if (this.stompClient) {
            this.stompClient.disconnect();
            this.isConnected = false;
            console.log('已断开连接');
        }
    }
}

// 使用示例
const connectionManager = new StompConnectionManager();

connectionManager.connect(
    () => {
        // 连接成功
        connectionManager.subscribe('/topic/room/123456', (message) => {
            console.log('收到房间更新:', message);
        });
    },
    (error) => {
        // 连接错误
        console.error('连接失败:', error);
    }
);
```

#### 重连成功后恢复游戏状态

```javascript
/**
 * 游戏状态管理器
 */
class GameStateManager {
    constructor(connectionManager) {
        this.connectionManager = connectionManager;
        this.state = null;
        this.stateVersion = 0;
    }

    /**
     * 更新游戏状态（带版本检查）
     */
    updateState(newState) {
        if (newState.stateVersion <= this.stateVersion) {
            console.warn('收到旧版本状态，忽略:', newState.stateVersion);
            return;
        }

        this.state = newState;
        this.stateVersion = newState.stateVersion;

        console.log('游戏状态已更新:', this.stateVersion);
        this.render();
    }

    /**
     * 处理重连后的状态恢复
     */
    handleReconnectState(reconnectState) {
        console.log('收到重连状态:', reconnectState);

        // 1. 恢复游戏状态
        this.updateState(reconnectState.gameState);

        // 2. 恢复玩家手牌（如果是自己的手牌）
        if (reconnectState.holeCards) {
            this.state.myHoleCards = reconnectState.holeCards;
        }

        // 3. 重新渲染
        this.render();
    }

    /**
     * 渲染游戏界面
     */
    render() {
        if (!this.state) return;

        // 渲染玩家列表
        renderPlayers(this.state.players);

        // 渲染公共牌
        renderCommunityCards(this.state.communityCards);

        // 渲染底池
        renderPot(this.state.pot);

        // 渲染当前玩家
        renderCurrentPlayer(this.state.currentPlayerId);
    }
}

// 前端订阅重连消息
connectionManager.subscribe('/user/queue/private', (message) => {
    if (message.type === 'RECONNECT_STATE') {
        gameStateManager.handleReconnectState(message);
    }
});
```

---

## 3. 基于 Session 的房间管理方案

### 3.1 Session 映射

```java
package com.pocketholdem.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.context.event.EventListener;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket Session 管理器
 */
@Slf4j
@Service
public class WebSocketSessionManager {

    // sessionId -> roomId 映射
    private final ConcurrentHashMap<String, String> sessionToRoom = new ConcurrentHashMap<>();

    // roomId -> sessionId -> playerId 映射
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> roomToSessions =
        new ConcurrentHashMap<>();

    // playerId -> sessionId 映射
    private final ConcurrentHashMap<String, String> playerToSession = new ConcurrentHashMap<>();

    /**
     * 建立连接
     */
    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        String sessionId = event.getMessage().getHeaders().get("simpSessionId").toString();
        log.info("Session 已连接: {}", sessionId);
    }

    /**
     * 断开连接
     */
    @EventListener
    public void handleSessionDisconnected(SessionDisconnectEvent event) {
        String sessionId = event.getMessage().getHeaders().get("simpSessionId").toString();
        log.info("Session 已断开: {}", sessionId);

        // 清理映射
        String roomId = sessionToRoom.remove(sessionId);
        if (roomId != null) {
            ConcurrentHashMap<String, String> sessions = roomToSessions.get(roomId);
            if (sessions != null) {
                sessions.remove(sessionId);
            }
        }
    }

    /**
     * 玩家加入房间
     */
    public void joinRoom(String sessionId, String roomId, String playerId) {
        // 建立映射
        sessionToRoom.put(sessionId, roomId);
        playerToSession.put(playerId, sessionId);

        // 更新房间Session列表
        roomToSessions.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
            .put(sessionId, playerId);

        log.info("玩家[{}]加入房间[{}], sessionId={}", playerId, roomId, sessionId);
    }

    /**
     * 玩家离开房间
     */
    public void leaveRoom(String sessionId) {
        String roomId = sessionToRoom.remove(sessionId);
        if (roomId != null) {
            ConcurrentHashMap<String, String> sessions = roomToSessions.get(roomId);
            if (sessions != null) {
                String playerId = sessions.remove(sessionId);
                playerToSession.remove(playerId);
                log.info("玩家[{}]离开房间[{}]", playerId, roomId);
            }
        }
    }

    /**
     * 广播到房间
     */
    public void broadcastToRoom(String roomId, Object message) {
        ConcurrentHashMap<String, String> sessions = roomToSessions.get(roomId);
        if (sessions != null) {
            log.debug("广播到房间[{}]: {}", roomId, message);
            // 通过 SimpMessagingTemplate 发送
            // messagingTemplate.convertAndSend("/topic/room/" + roomId, message);
        }
    }

    /**
     * 私发给玩家
     */
    public void sendToPlayer(String playerId, Object message) {
        String sessionId = playerToSession.get(playerId);
        if (sessionId != null) {
            log.debug("私发给玩家[{}]: {}", playerId, message);
            // 通过 SimpMessagingTemplate 发送
            // messagingTemplate.convertAndSendToUser(sessionId, "/queue/private", message);
        }
    }

    /**
     * 获取房间内的所有玩家ID
     */
    public Set<String> getRoomPlayerIds(String roomId) {
        ConcurrentHashMap<String, String> sessions = roomToSessions.get(roomId);
        if (sessions != null) {
            return new HashSet<>(sessions.values());
        }
        return Set.of();
    }

    /**
     * 获取玩家所在房间ID
     */
    public String getPlayerRoomId(String playerId) {
        String sessionId = playerToSession.get(playerId);
        if (sessionId != null) {
            return sessionToRoom.get(sessionId);
        }
        return null;
    }
}
```

### 3.2 消息广播实现

```java
package com.pocketholdem.service;

import com.pocketholdem.dto.RoomDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * 消息广播服务
 */
@Slf4j
@Service
public class MessageBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionManager sessionManager;

    public MessageBroadcastService(SimpMessagingTemplate messagingTemplate,
                                   WebSocketSessionManager sessionManager) {
        this.messagingTemplate = messagingTemplate;
        this.sessionManager = sessionManager;
    }

    /**
     * 广播房间状态更新到房间内所有玩家
     */
    public void broadcastRoomState(String roomId, RoomDTO roomState) {
        String destination = "/topic/room/" + roomId;
        log.debug("广播房间状态: roomId={}", roomId);

        messagingTemplate.convertAndSend(destination, roomState);
    }

    /**
     * 广播玩家操作到房间内所有玩家
     */
    public void broadcastPlayerAction(String roomId, String playerId, String action) {
        String destination = "/topic/room/" + roomId;
        log.debug("广播玩家操作: roomId={}, playerId={}, action={}", roomId, playerId, action);

        Map<String, Object> payload = Map.of(
            "type", "PLAYER_ACTED",
            "playerId", playerId,
            "action", action,
            "timestamp", System.currentTimeMillis()
        );

        messagingTemplate.convertAndSend(destination, payload);
    }

    /**
     * 私发手牌给指定玩家
     */
    public void sendHoleCardsToPlayer(String playerId, List<CardDTO> holeCards) {
        String sessionId = sessionManager.getPlayerSessionId(playerId);
        if (sessionId == null) {
            log.warn("玩家[{}]未找到对应的Session", playerId);
            return;
        }

        log.debug("发送手牌给玩家: playerId={}", playerId);

        Map<String, Object> payload = Map.of(
            "type", "DEAL_CARDS",
            "holeCards", holeCards,
            "timestamp", System.currentTimeMillis()
        );

        messagingTemplate.convertAndSendToUser(sessionId, "/queue/private", payload);
    }

    /**
     * 私发重连状态给指定玩家
     */
    public void sendReconnectState(String playerId, Object reconnectState) {
        String sessionId = sessionManager.getPlayerSessionId(playerId);
        if (sessionId == null) {
            log.warn("玩家[{}]未找到对应的Session", playerId);
            return;
        }

        log.debug("发送重连状态给玩家: playerId={}", playerId);

        messagingTemplate.convertAndSendToUser(sessionId, "/queue/private", reconnectState);
    }
}
```

---

## 4. 消息确认机制

### 4.1 服务端配置

```java
package com.pocketholdem.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 配置（带ACK）
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 启用简单消息代理
        registry.enableSimpleBroker("/topic", "/queue");

        // 设置应用程序前缀
        registry.setApplicationDestinationPrefixes("/app");

        // 启用ACK
        registry.setPreservePublishOrder(true);  // 保证消息顺序
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 配置入站通道线程池
        registration.taskExecutor()
            .corePoolSize(4)
            .maxPoolSize(8);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        // 配置出站通道线程池
        registration.taskExecutor()
            .corePoolSize(4)
            .maxPoolSize(8);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .withSockJS();
    }
}
```

### 4.2 客户端 ACK

```javascript
/**
 * 订阅消息（带ACK）
 */
connectionManager.subscribe('/topic/room/123456', (message) => {
    console.log('收到消息:', message);

    // 处理消息
    handleMessage(message);

    // 发送ACK确认
    message.ack();
}, { ack: 'client' });

/**
 * 订阅私聊消息（带ACK）
 */
connectionManager.subscribe('/user/queue/private', (message) => {
    console.log('收到私聊消息:', message);

    // 处理消息
    handlePrivateMessage(message);

    // 发送ACK确认
    message.ack();
}, { ack: 'client' });
```

### 4.3 ACK 失败处理

```javascript
/**
 * 订阅消息（带NACK）
 */
connectionManager.subscribe('/topic/room/123456', (message) => {
    try {
        console.log('收到消息:', message);

        // 处理消息
        handleMessage(message);

        // 处理成功，发送ACK
        message.ack();
    } catch (error) {
        console.error('处理消息失败:', error);

        // 处理失败，发送NACK
        message.nack(error);
    }
}, { ack: 'client' });
```

---

## 5. STOMP 消息协议规范

### 5.1 客户端 → 服务端

#### 创建房间

```
目的: /app/create-room
消息类型: CREATE_ROOM
字段:
  - nickname: 玩家昵称（必填）
  - maxPlayers: 最大玩家数（可选，默认9）
  - smallBlind: 小盲（可选，默认10）
  - bigBlind: 大盲（可选，默认20）

示例:
{
  "type": "CREATE_ROOM",
  "nickname": "Alice",
  "maxPlayers": 9,
  "smallBlind": 10,
  "bigBlind": 20,
  "requestId": "req-001",
  "timestamp": 1706000000000
}
```

#### 加入房间

```
目的: /app/join-room
消息类型: JOIN_ROOM
字段:
  - roomId: 房间ID（必填）
  - nickname: 玩家昵称（必填）

示例:
{
  "type": "JOIN_ROOM",
  "roomId": "123456",
  "nickname": "Bob",
  "requestId": "req-002",
  "timestamp": 1706000000000
}
```

#### 坐下

```
目的: /app/sit-down
消息类型: SIT_DOWN
字段:
  - playerId: 玩家ID（必填）
  - seatIndex: 座位号（必填，0-8）

示例:
{
  "type": "SIT_DOWN",
  "playerId": "player-001",
  "seatIndex": 0,
  "requestId": "req-003",
  "timestamp": 1706000000000
}
```

#### 玩家操作

```
目的: /app/player-action
消息类型: PLAYER_ACTION
字段:
  - playerId: 玩家ID（必填）
  - action: 操作类型（必填）
    - FOLD: 弃牌
    - CHECK: 过牌
    - CALL: 跟注
    - RAISE: 加注
    - ALL_IN: 全押
  - amount: 加注金额（可选，仅RAISE时需要）
  - roundIndex: 回合索引（必填，用于幂等性检查）

示例:
{
  "type": "PLAYER_ACTION",
  "playerId": "player-001",
  "action": "RAISE",
  "amount": 50,
  "roundIndex": 1,
  "requestId": "req-004",
  "timestamp": 1706000000000
}
```

#### 重连

```
目的: /app/reconnect
消息类型: RECONNECT
字段:
  - roomId: 房间ID（必填）
  - playerId: 玩家ID（必填）

示例:
{
  "type": "RECONNECT",
  "roomId": "123456",
  "playerId": "player-001",
  "requestId": "req-005",
  "timestamp": 1706000000000
}
```

### 5.2 服务端 → 客户端

#### 房间已创建

```
目的: /user/queue/private
消息类型: ROOM_CREATED
字段:
  - roomId: 房间ID
  - room: 房间DTO
  - requestId: 请求ID

示例:
{
  "type": "ROOM_CREATED",
  "roomId": "123456",
  "room": {
    "room_id": "123456",
    "host_id": "player-001",
    "seated_players": [],
    "game_state": null
  },
  "requestId": "req-001",
  "timestamp": 1706000000000
}
```

#### 房间状态更新

```
目的: /topic/room/{roomId}
消息类型: ROOM_UPDATED
字段:
  - stateVersion: 状态版本号
  - gameState: 游戏状态DTO

示例:
{
  "type": "ROOM_UPDATED",
  "stateVersion": 1,
  "gameState": {
    "phase": "PREFLOP",
    "current_player_id": "player-001",
    "pot": 30,
    "community_cards": [],
    "seated_players": [...]
  },
  "timestamp": 1706000000000
}
```

#### 玩家操作通知

```
目的: /topic/room/{roomId}
消息类型: PLAYER_ACTED
字段:
  - playerId: 玩家ID
  - action: 操作类型
  - stateVersion: 状态版本号

示例:
{
  "type": "PLAYER_ACTED",
  "playerId": "player-001",
  "action": "FOLD",
  "stateVersion": 2,
  "timestamp": 1706000000000
}
```

#### 发牌

```
目的: /user/queue/private
消息类型: DEAL_CARDS
字段:
  - holeCards: 手牌列表
  - communityCards: 公共牌列表

示例:
{
  "type": "DEAL_CARDS",
  "holeCards": [
    { "suit": "HEARTS", "rank": "A" },
    { "suit": "SPADES", "rank": "K" }
  ],
  "communityCards": [],
  "timestamp": 1706000000000
}
```

#### 重连状态恢复

```
目的: /user/queue/private
消息类型: RECONNECT_STATE
字段:
  - gameState: 游戏状态DTO
  - holeCards: 手牌列表（如果是自己）

示例:
{
  "type": "RECONNECT_STATE",
  "gameState": {
    "phase": "FLOP",
    "current_player_id": "player-002",
    "pot": 150,
    "community_cards": [
      { "suit": "HEARTS", "rank": "A" },
      { "suit": "DIAMONDS", "rank": "7" },
      { "suit": "CLUBS", "rank": "3" }
    ],
    "seated_players": [...]
  },
  "holeCards": [
    { "suit": "SPADES", "rank": "K" },
    { "suit": "HEARTS", "rank": "Q" }
  ],
  "timestamp": 1706000000000
}
```

---

## 6. 总结

### 6.1 核心特性

| 特性 | 实现方案 |
|------|----------|
| **自动重连** | 前端指数退避策略 + 服务端重连消息 |
| **房间管理** | 基于 Session 的三重映射 |
| **消息确认** | STOMP ACK 机制 + 超时重发 |
| **幂等性** | requestId + roundIndex 防止重复处理 |
| **状态恢复** | 重连后发送完整游戏状态 |

### 6.2 对比 Socket.io

| 项目 | Socket.io | STOMP |
|------|-----------|-------|
| **自动重连** | ✅ 内置 | ⚠️ 需手动实现 |
| **房间管理** | ✅ 内置API | ⚠️ 需自己管理 |
| **消息确认** | ✅ 内置 | ⚠️ 需显式配置 |
| **协议标准** | ❌ 自定义 | ✅ RFC 6455 |
| **Spring集成** | ⚠️ 需第三方库 | ✅ 原生支持 |

### 6.3 最佳实践

1. **重连机制**：使用指数退避策略，避免频繁重连
2. **消息确认**：重要消息必须ACK，失败时重发
3. **状态版本**：使用 stateVersion 检测消息顺序
4. **幂等性**：使用 requestId 防止重复处理
5. **Session管理**：维护 sessionId ↔ roomId ↔ playerId 映射

---

**文档版本**: v1.0
**最后更新**: 2026-01-23
