# 项目实施计划

> 本项目是基于 `/Users/Hana/Codes/pocket-holdem-mvp` 的 Java 重构版本。
> 所有功能实现需参考原项目的 TypeScript 代码。

---

## 📅 阶段一：Java 后端核心引擎 (当前阶段)

**目标**: 完成不依赖网络的纯逻辑部分，确保游戏规则正确。

### 任务清单
- [x] 搭建 Spring Boot 3 + Java 17 工程骨架
- [ ] 移植核心枚举类型 (Suit, Rank, GamePhase, ActionType, PlayerStatus)
- [ ] 移植数据结构 (Card, Pot, Player, Room, GameState)
- [ ] 实现 PokerEngine.java
  - [ ] 创建牌组 (createDeck)
  - [ ] Fisher-Yates 洗牌 (shuffleDeck)
  - [ ] 发牌逻辑 (dealHoleCards, dealCommunityCards)
  - [ ] 牌型评估 (evaluateHand) - 7选5最佳组合
  - [ ] 10种牌型判定 (皇家同花顺 → 高牌)
- [ ] 实现 SidePotCalculator.java (边池计算算法)
- [ ] 编写 JUnit 单元测试，覆盖核心逻辑

### 参考文件
- `/Users/Hana/Codes/pocket-holdem-mvp/server/src/PokerEngine.ts`
- `/Users/Hana/Codes/pocket-holdem-mvp/server/src/Interfaces.ts`
- `/Users/Hana/Codes/pocket-holdem-mvp/server/src/poker.test.ts`

---

## 📅 阶段二：状态管理与并发控制

**目标**: 实现房间管理和安全的并发读写。

### 任务清单
- [ ] 实现 Player.java (玩家状态封装)
- [ ] 实现 Room.java (房间状态)
- [ ] 实现 RoomManager.java (单例，使用 ConcurrentHashMap)
- [ ] 实现 GameController.java (游戏状态机)
- [ ] 添加 ReentrantLock 锁机制防止竞态条件
- [ ] 实现请求 ID 去重缓存 (LRU)

### 参考文件
- `/Users/Hana/Codes/pocket-holdem-mvp/server/src/Player.ts`
- `/Users/Hana/Codes/pocket-holdem-mvp/server/src/RoomManager.ts`
- `/Users/Hana/Codes/pocket-holdem-mvp/server/src/GameController.ts`

---

## 📅 阶段三：WebSocket 网络层接入

**目标**: 连通前端，替换 Node.js 后端。

### 任务清单
- [ ] 配置 WebSocketConfig.java (STOMP 端点)
- [ ] 实现 GameMessageController.java (@MessageMapping)
- [ ] 实现广播机制 (SimpMessagingTemplate)
- [ ] 实现私发机制 (convertAndSendToUser)
- [ ] 调试 CORS 和协议兼容问题
- [ ] 实现超时处理 (ScheduledExecutorService)
- [ ] 实现断线重连逻辑

### 参考文件
- `/Users/Hana/Codes/pocket-holdem-mvp/server/src/index.ts`
- `/Users/Hana/Codes/pocket-holdem-mvp/docs/websocket-protocol.md`

---

## 📅 阶段四：前端重构 (UniApp)

**目标**: 使用 UniApp 重写前端，支持 H5 和微信小程序。

### 任务清单
- [ ] 初始化 UniApp + Vue 3 + TypeScript 项目
- [ ] 封装 useStomp.ts (兼容 H5 和小程序)
- [ ] 实现首页 (创建/加入房间)
- [ ] 实现房间页 (座位、准备)
- [ ] 实现游戏页 (牌桌、手牌、操作面板)
- [ ] 实现 Showdown 结算弹窗
- [ ] 实现倒计时 UI
- [ ] 对接后端 STOMP 接口

### 参考文件
- `/Users/Hana/Codes/pocket-holdem-mvp/client/src/App.vue`
- `/Users/Hana/Codes/pocket-holdem-mvp/client/src/composables/useSocket.ts`

---

## 📅 阶段五：优化与发布

**目标**: 达到 MVP 交付标准。

### 任务清单
- [ ] 全流程测试（从开房到结算）
- [ ] 6人同时在线压力测试
- [ ] 优化断线重连体验
- [ ] 微信小程序真机测试
- [ ] 部署到服务器
- [ ] 编写用户使用指南

---

## 📊 进度追踪

| 阶段 | 状态 | 预估工作量 |
|------|------|-----------|
| 阶段一 | 🔄 进行中 | 2-3 天 |
| 阶段二 | ⏳ 待开始 | 1-2 天 |
| 阶段三 | ⏳ 待开始 | 1-2 天 |
| 阶段四 | ⏳ 待开始 | 2-3 天 |
| 阶段五 | ⏳ 待开始 | 1-2 天 |
