# Pocket Holdem 技术架构蓝图 (Architecture Lite)

> 本文档是开发真正能用的技术蓝图，将 PRD 规则转换为可运行的技术结构。

---

## 〇、工程哲学声明

> [!IMPORTANT]
> **本项目遵循以下核心原则，贯穿所有设计与实现决策：**

| 原则 | 含义 |
|------|------|
| **Server 是唯一权威** | 所有游戏状态、逻辑计算、规则校验均在服务端完成 |
| **客户端仅渲染** | 前端不参与任何游戏逻辑推理，只接收并展示服务端下发的状态 |
| **稳定优先于功能** | MVP 阶段以"能跑、可维护、正确"为目标，不追求完美 |
| **单房间串行** | 同一房间内所有操作严格串行处理，杜绝竞态条件 |
| **状态版本化** | 每次状态变更携带版本号，支持幂等和断线恢复 |

---

## 一、并发与执行模型

> [!NOTE]
> **Java 重构版本并发模型**：由于从 Node.js 迁移到 Java，并发模型发生重大变化。
> 详见 [并发模型设计文档](./concurrency-model.md)。

### 1.1 服务端执行模型

#### Node.js 版本（原项目）

```
┌─────────────────────────────────────────────────────────────────┐
│                    Node.js 单线程事件循环                        │
│                                                                 │
│    ┌──────────┐   ┌──────────┐   ┌──────────┐                  │
│    │  Room A  │   │  Room B  │   │  Room C  │   ...            │
│    │ (Queue)  │   │ (Queue)  │   │ (Queue)  │                  │
│    └────┬─────┘   └────┬─────┘   └────┬─────┘                  │
│         │              │              │                         │
│         ▼              ▼              ▼                         │
│    ┌──────────────────────────────────────────┐                │
│    │            事件循环统一调度                │                │
│    │  (天然保证单房间操作串行执行)              │                │
│    └──────────────────────────────────────────┘                │
└─────────────────────────────────────────────────────────────────┘
```

#### Java 版本（重构后）

```
┌─────────────────────────────────────────────────────────────────┐
│                   Java 多线程 + 线程池                          │
│                                                                 │
│    ┌──────────┐   ┌──────────┐   ┌──────────┐                  │
│    │  Room A  │   │  Room B  │   │  Room C  │   ...            │
│    │Reentrant │   │Reentrant │   │Reentrant │                  │
│    │   Lock   │   │   Lock   │   │   Lock   │                  │
│    └────┬─────┘   └────┬─────┘   └────┬─────┘                  │
│         │              │              │                         │
└────────┼──────────────┼──────────────┼─────────────────────────┘
         │              │              │
         └─── ┌────────┴────────────┐                │
               ▼                    ▼               │
     ┌────────────┐         ┌────────────┐       │
     │ Thread-1   │         │ Thread-2   │       │
     │ (CPU核心1) │         │ (CPU核心2) │       │
     └────────────┘         └────────────┘       │

依赖：Tomcat线程池（默认200线程）
```

### 1.2 并发安全声明

| 保障项 | Node.js 实现 | Java 实现 | 说明 |
|--------|-------------|-----------|------|
| **单房间串行** | 单线程 + 同步处理 | ReentrantLock | 同一房间的所有 PLAYER_ACTION 严格串行 |
| **多房间并发** | 房间隔离 | 不同房间不同锁 | 不同房间互不影响，可真正并发处理 |
| **无竞态条件** | 天然无竞态 | 显式锁 + CopyOnWriteArrayList | 需手动保证线程安全 |
| **幂等校验** | stateVersion + requestId | stateVersion + requestId | 重复请求被拒绝，不产生副作用 |

### 1.3 操作处理流程

```typescript
// 伪代码：PLAYER_ACTION 处理流程
function handlePlayerAction(socketId, payload) {
  // 1. 查找房间和玩家（同步）
  const { room, player } = findBySocket(socketId);
  
  // 2. 校验操作合法性（同步）
  const validation = validateAction(room, player, payload);
  if (!validation.valid) {
    return sendError(socketId, validation.error);
  }
  
  // 3. 执行操作，修改状态（同步）
  applyAction(room, player, payload);
  
  // 4. 递增 stateVersion（同步）
  room.gameState.stateVersion++;
  
  // 5. 广播新状态（异步但无状态依赖）
  broadcastState(room);
}
```

---

## 二、游戏生命周期建模

### 2.1 房间生命周期

```
┌─────────────┐
│   CREATED   │ ← 房主创建房间
└──────┬──────┘
       │ 玩家加入/入座
       ▼
┌─────────────┐
│   WAITING   │ ← 等待开始（玩家入座/站起/踢人）
└──────┬──────┘
       │ 房主点击开始 && 人数 ≥ 2
       ▼
┌─────────────┐
│   PLAYING   │ ← 游戏进行中（循环手牌）
└──────┬──────┘
       │ 仅剩 1 人有筹码 / 房主结束
       ▼
┌─────────────┐
│    ENDED    │ ← 游戏结束
└─────────────┘
```

### 2.2 单手牌生命周期 (Hand Lifecycle)

```
┌─────────────────────────────────────────────────────────────────┐
│                        HAND START                               │
│  1. 生成 handId（唯一标识本手牌）                                 │
│  2. 轮转庄位（跳过淘汰玩家）                                       │
│  3. 确定大小盲位置                                                │
│  4. 标记 WAITING 玩家为 ACTIVE / 保持 ELIMINATED 不变             │
│  5. 洗牌 → 收取盲注 → 发底牌                                      │
│  6. stateVersion++                                              │
└────────────────────────┬────────────────────────────────────────┘
                         │
            ┌────────────▼────────────┐
            │       PRE_FLOP          │ ← 从大盲后一位开始
            │   (roundId = uuid())    │
            └────────────┬────────────┘
                         │ 下注轮完成
            ┌────────────▼────────────┐
            │         FLOP            │ ← 发3张公共牌
            │   (roundId = uuid())    │
            └────────────┬────────────┘
                         │ 下注轮完成
            ┌────────────▼────────────┐
            │         TURN            │ ← 发1张公共牌
            │   (roundId = uuid())    │
            └────────────┬────────────┘
                         │ 下注轮完成
            ┌────────────▼────────────┐
            │        RIVER            │ ← 发1张公共牌
            │   (roundId = uuid())    │
            └────────────┬────────────┘
                         │ 下注轮完成
            ┌────────────▼────────────┐
            │       SHOWDOWN          │ ← 比牌 / 结算
            └────────────┬────────────┘
                         │
┌────────────────────────▼────────────────────────────────────────┐
│                        HAND END                                 │
│  1. 计算边池 → 分配筹码给赢家                                     │
│  2. 检查淘汰：筹码=0 的玩家标记 ELIMINATED                         │
│  3. 检查游戏结束：仅剩 1 人有筹码?                                 │
│  4. 重置玩家状态 → 开始下一手牌                                    │
└─────────────────────────────────────────────────────────────────┘
```

**提前结束条件**：
- 任意阶段如果仅剩 1 人未弃牌 → 直接结算，无需摊牌
- 所有人 All-In → 直接发完所有公共牌，进入摊牌

### 2.3 单下注轮生命周期 (Betting Round)

```
┌─────────────────────────────────────────────────────────────────┐
│                      BETTING ROUND START                        │
│  1. 生成 roundId（唯一标识本下注轮）                              │
│  2. 重置 currentBet = 0（PRE_FLOP 除外，保留盲注）                 │
│  3. 重置所有玩家 hasActed = false, currentBet = 0               │
│  4. 确定第一个行动者                                              │
│  5. stateVersion++                                              │
└────────────────────────┬────────────────────────────────────────┘
                         │
         ┌───────────────▼───────────────┐
         │      等待当前玩家行动          │←──┐
         │      (30秒超时)               │   │
         └───────────────┬───────────────┘   │
                         │                   │
    ┌────────────────────┼────────────────────┐
    │                    │                    │
    ▼                    ▼                    ▼
┌───────┐          ┌───────┐           ┌────────┐
│ FOLD  │          │CHECK/ │           │ RAISE  │
│       │          │ CALL  │           │        │
└───┬───┘          └───┬───┘           └───┬────┘
    │                  │                   │
    │                  │                   │ 更新 currentBet
    │                  │                   │ 重置其他人 hasActed
    │                  ▼                   │
    │           标记 hasActed=true          │
    │           stateVersion++             │
    └──────────────────┼───────────────────┘
                       │
                       ▼
              ┌───────────────┐
              │ 检查轮结束条件 │
              │ (isRoundDone) │
              └───────┬───────┘
                      │
         ┌────────────┴────────────┐
         │ YES                     │ NO
         ▼                         │
┌─────────────────┐               │
│ BETTING ROUND   │               │
│     END         │               │
└─────────────────┘               │
                                  │
         找到下一个行动者 ──────────┘
```

**下注轮结束条件**：
1. 仅剩 1 人（其他人全弃牌）
2. 所有未弃牌玩家 hasActed=true 且 currentBet 相等
3. 所有人 All-In

---

## 三、状态版本化设计

### 3.1 GameState 版本化字段

```typescript
interface GameState {
  // === 版本化字段（新增） ===
  stateVersion: number;      // 全局递增版本号，每次状态变更 +1
  handId: string;            // 当前手牌唯一标识 (UUID)
  roundId: string;           // 当前下注轮唯一标识 (UUID)
  
  // === 现有字段 ===
  phase: GamePhase;
  communityCards: Card[];
  pots: Pot[];
  currentPlayerIndex: number | null;
  dealerIndex: number;
  smallBlindIndex: number;
  bigBlindIndex: number;
  currentBet: number;
  minRaise: number;
  roundIndex: number;        // 保留，用于简单幂等
  turnTimeout: number;
  deck: Card[];
  handNumber: number;
}
```

### 3.2 版本控制规则

| 事件 | stateVersion 变化 | handId 变化 | roundId 变化 |
|------|-------------------|-------------|--------------|
| 新手牌开始 | +1 | 新生成 | 新生成 |
| 玩家操作 | +1 | 不变 | 不变 |
| 阶段推进 | +1 | 不变 | 新生成 |
| 手牌结束 | +1 | 清空 | 清空 |

### 3.3 客户端版本校验

```typescript
// 客户端状态更新逻辑
function handleSyncState(payload: SyncStatePayload) {
  const currentVersion = store.gameState?.stateVersion ?? 0;
  
  // 拒绝旧版本状态
  if (payload.stateVersion <= currentVersion) {
    console.warn('忽略旧版本状态:', payload.stateVersion, '<=', currentVersion);
    return;
  }
  
  // 接受新版本状态
  store.updateState(payload);
}
```

---

## 四、核心实体关系

### 4.1 实体结构图

```
                    ┌─────────────────────┐
                    │     RoomManager     │ ← 单例，管理所有房间
                    │  (rooms: Map)       │
                    └──────────┬──────────┘
                               │ 1:N
                               ▼
                    ┌─────────────────────┐
                    │        Room         │
                    │  - id              │
                    │  - config          │
                    │  - hostId          │
┌──────────────┐    │  - players: Map    │    ┌─────────────────┐
│    Player    │◄───│  - seatMap: []     │───►│   GameState     │
│  - id        │    │  - gameState       │    │  - stateVersion │ ← 新增
│  - nickname  │    │  - isPlaying       │    │  - handId       │ ← 新增
│  - chips     │    └─────────────────────┘    │  - roundId      │ ← 新增
│  - status    │                               │  - phase        │
│  - holeCards │                               │  - pots[]       │
│  - seatIndex │                               │  - deck         │
└──────────────┘                               └─────────────────┘
                                                       │
                                                       │ 计算
                                                       ▼
                                               ┌─────────────────┐
                                               │   PokerEngine   │
                                               │  - 洗牌/发牌    │ ← 纯函数，无副作用
                                               │  - 边池计算      │
                                               │  - 牌型评估      │
                                               └─────────────────┘
```

### 4.2 现有实体映射

| PRD 概念 | 代码实体 | 文件位置 |
|----------|----------|----------|
| 房间 | `Room` interface | `Interfaces.ts` |
| 玩家 | `Player` class | `Player.ts` |
| 牌局 | `GameState` interface | `Interfaces.ts` |
| 底池 | `Pot` interface | `Interfaces.ts` |
| 扑克牌 | `Card` interface | `Interfaces.ts` |
| 牌组 | `deck: Card[]` | GameState 内部 |
| 下注轮 | 隐式（由 phase + roundId 驱动） | 无独立实体 |

---

## 五、玩家状态机

```
                    ┌─────────────┐
   加入房间 ────────►│ SPECTATING  │◄──── 站起(非游戏中)
                    │  (观战中)    │
                    └──────┬──────┘
                           │ 入座
                           ▼
                    ┌─────────────┐
                    │   WAITING   │◄──── 新加入(游戏进行中)
                    │ (等待下一局) │◄──── 手牌结束后重置
                    └──────┬──────┘
                           │ 新一手牌开始 && chips > 0
                           ▼
                    ┌─────────────┐
          ┌────────│   ACTIVE    │────────┐
          │        │  (参与中)    │        │
          │        └──────┬──────┘        │
          │               │               │
          │ 弃牌          │ All-In        │ 筹码归零
          ▼               ▼               │
   ┌─────────────┐ ┌─────────────┐        │
   │   FOLDED    │ │   ALL_IN    │        │
   │  (已弃牌)   │ │  (已全押)   │        │
   └─────────────┘ └─────────────┘        │
          │               │               │
          └───────────────┴───────────────┘
                          │ 手牌结束 && chips = 0
                          ▼
                   ┌─────────────┐
                   │ ELIMINATED  │ ← 保留座位，不发牌
                   │  (已淘汰)   │
                   └─────────────┘
```

**状态转换规则表**：

| 当前状态 | 触发事件 | 目标状态 | 条件 |
|----------|----------|----------|------|
| SPECTATING | 入座 | WAITING | 游戏进行中 |
| SPECTATING | 入座 | ACTIVE | 游戏未开始 |
| WAITING | 新手牌开始 | ACTIVE | chips > 0 |
| WAITING | 新手牌开始 | ELIMINATED | chips = 0 |
| ACTIVE | 弃牌 | FOLDED | - |
| ACTIVE | All-In | ALL_IN | chips = 0 |
| ACTIVE/FOLDED/ALL_IN | 手牌结束 | ELIMINATED | chips = 0 |
| ACTIVE/FOLDED/ALL_IN | 手牌结束 | WAITING | chips > 0 |
| 任意(已入座) | 站起 | SPECTATING | 非游戏进行中 |

---

## 六、边池 (Side Pot) 算法

### 6.1 核心思路

当多个玩家 All-In 且筹码不一致时，需要拆分为多层底池。

**算法步骤**：
1. 收集所有下注玩家，按 `currentBet` 升序排列
2. 逐层切割，每层金额 = (本层bet - 上层bet) × 本层及以上人数
3. 每个池的 `eligiblePlayerIds` = 投入 ≥ 该层下注额 且 未弃牌的玩家

### 6.2 示例

| 玩家 | 下注额 | 弃牌? |
|------|--------|-------|
| A    | 50     | 否    |
| B    | 100    | 否    |
| C    | 200    | 否    |
| D    | 200    | 是    |

**边池计算**：
```
池1 (主池): 50 × 4 = 200  → eligible: [A, B, C] (D弃牌)
池2 (边池1): (100-50) × 3 = 150 → eligible: [B, C]
池3 (边池2): (200-100) × 2 = 200 → eligible: [C] (D弃牌不分配)
总计: 550
```

### 6.3 分配规则

- 每个池独立比牌，最大牌型获得该池
- 同分平分，余数给位置最靠前的（离庄最近）

### 6.4 MVP 降级策略

> [!TIP]
> 边池算法已在 `PokerEngine.calculateSidePots()` 中实现，但为保证 MVP 交付稳定性：

| 场景 | 策略 |
|------|------|
| 标准边池（≤3层） | 完整支持 |
| 极端多人 All-In（>4人不同筹码） | 完整支持，但优先进行功能测试 |
| 发现 Bug | 可临时回退到"简化版"（合并为单一底池） |

**简化版降级逻辑**（仅作为紧急后备）：
```typescript
function calculateSidePotsSimplified(players: Player[]): Pot[] {
  const total = players.reduce((sum, p) => sum + p.currentBet, 0);
  const eligible = players.filter(p => !p.isFolded).map(p => p.id);
  return [{ amount: total, eligiblePlayerIds: eligible }];
}
```

---

## 七、断线重连机制（唯一真相模型）

### 7.1 核心原则

> [!IMPORTANT]
> **重连恢复的唯一真相来源 = Server 内存中的 GameState**
> 
> 客户端永不推导游戏状态，只接收并渲染服务端下发的完整快照。

### 7.2 重连数据结构

客户端本地存储：
```typescript
localStorage: {
  'pocket_holdem_player_id': string,  // UUID
  'pocket_holdem_room_id': string     // 6位房间号
}
```

### 7.3 重连流程

```
客户端                              服务端
  │                                    │
  │──── RECONNECT {playerId, roomId} ──►│
  │                                    │
  │                           1. 检查房间是否存在
  │                           2. 检查玩家是否在房间中
  │                           3. 更新 player.socketId
  │                           4. 不触发任何 Timer 重置
  │                                    │
  │◄── RECONNECTED {                   │
  │      room: PublicRoomInfo,         │  ← 完整房间状态
  │      myPlayerId: string,           │
  │      myCards: Card[],              │  ← 恢复手牌
  │      stateVersion: number          │  ← 版本号
  │    } ─────────────────────────────│
  │                                    │
```

### 7.4 完整恢复字段清单

| 字段 | 来源 | 用途 |
|------|------|------|
| `room` | RoomManager | 房间全貌（玩家、配置、游戏状态） |
| `myPlayerId` | localStorage / Server 确认 | 确认身份 |
| `myCards` | `player.holeCards` | 恢复手牌显示 |
| `gameState` | `room.gameState` | 恢复当前牌局状态 |
| `stateVersion` | `gameState.stateVersion` | 版本校验 |

### 7.5 Timer 处理规则

| 规则 | 说明 |
|------|------|
| Timer 只存在服务端 | 客户端显示的倒计时仅为 UI，不作为判断依据 |
| 断线不暂停 Timer | 玩家断线期间，30秒超时继续计时 |
| 断线不重启 Timer | 重连成功后，继续使用原有 Timer 剩余时间 |
| 不 duplicate Timer | 重连不会创建新的 Timer 实例 |

### 7.6 异常情形处理

| 场景 | 处理方式 |
|------|----------|
| 房间不存在 | 返回 ERROR `ROOM_NOT_FOUND`，客户端清除 localStorage |
| 玩家不在房间 | 返回 ERROR `PLAYER_NOT_FOUND`，客户端清除 localStorage |
| Server 重启 | 所有房间丢失（MVP 允许），客户端提示"房间已失效" |

---

## 八、WebSocket 协议（工程化升级）

### 8.1 核心原则

- **Server 权威**: 所有状态计算在服务端完成
- **单向广播**: 服务端主动推送，客户端仅渲染
- **隐私隔离**: 他人手牌永不下发
- **版本校验**: 客户端只接受更高版本的状态

### 8.2 SYNC_STATE 协议

所有状态同步必须携带版本化字段：

```typescript
interface SyncStatePayload {
  room: PublicRoomInfo;
  myCards?: Card[];
  myPlayerId: string;
  // === 新增字段 ===
  stateVersion: number;
  handId?: string;
  roundId?: string;
}
```

### 8.3 PLAYER_ACTION 幂等协议

```typescript
interface PlayerActionPayload {
  action: ActionType;
  amount?: number;
  // === 幂等字段 ===
  roundIndex: number;      // 轮次索引
  requestId: string;       // 请求唯一 ID (UUID)
}

// 服务端处理
function handleAction(payload: PlayerActionPayload) {
  // 1. requestId 去重
  if (processedRequests.has(payload.requestId)) {
    return { success: false, error: 'DUPLICATE_REQUEST' };
  }
  
  // 2. roundIndex 校验
  if (payload.roundIndex !== gameState.roundIndex) {
    return { success: false, error: 'STALE_REQUEST' };
  }
  
  // 3. 执行操作
  // ...
  
  // 4. 记录 requestId（保留最近 100 个）
  processedRequests.add(payload.requestId);
}
```

### 8.4 同步事件矩阵

| 事件 | 触发时机 | 广播范围 | 携带 stateVersion | 携带手牌 |
|------|----------|----------|-------------------|----------|
| SYNC_STATE | 任意状态变更 | 全房间 | ✅ | 仅自己的 |
| DEAL_CARDS | 发底牌 | 单独发送 | ✅ | 仅自己的 |
| PLAYER_ACTED | 玩家操作 | 全房间 | ✅ | 否 |
| HAND_RESULT | 手牌结束 | 全房间 | ✅ | 摊牌时公开 |
| RECONNECTED | 断线恢复 | 单独发送 | ✅ | 仅自己的 |

### 8.5 异常事件协议

```typescript
// 房主解散房间
interface RoomDismissedPayload {
  reason: 'HOST_DISMISSED' | 'HOST_LEFT';
}

// 房间结束
interface GameEndedPayload {
  winnerId: string;
  winnerNickname: string;
  reason: 'LAST_PLAYER_STANDING' | 'HOST_ENDED';
}

// 错误响应（增强）
interface ErrorPayload {
  code: string;
  message: string;
  shouldClearSession?: boolean;  // 是否清除本地存储
}
```

---

## 九、庄位轮换与特殊玩家处理

### 9.1 庄位轮换规则

```typescript
function getNextDealerIndex(currentIndex: number, players: Player[]): number {
  // 1. 筛选可当庄的玩家（座位非空、chips > 0、非 ELIMINATED）
  // 2. 按座位顺序找 > currentIndex 的第一个
  // 3. 找不到则回到最小座位号
}
```

### 9.2 特殊情况处理

| 场景 | 处理方式 |
|------|----------|
| 当前庄家本局淘汰 | 庄位跳过他，给下一个非淘汰玩家 |
| 新玩家中途加入 | 允许入座，标记 WAITING，本局不发牌 |
| 小/大盲玩家刚淘汰 | 盲注由下一个未淘汰玩家承担 |

### 9.3 盲注处理策略

```
假设座位: [0] [1] [2] [3] [4] (5人)
当前庄家: 2

可能的盲注分配:
- 正常: 小盲=3, 大盲=4
- 3号淘汰: 小盲=4, 大盲=0 (跳过3)
- 3,4号都淘汰: 小盲=0, 大盲=1
```

---

## 十、待补齐模块 Checklist

### 10.1 Server 端

- [ ] `GameController.ts` - 游戏控制器（处理 PLAYER_ACTION）
- [ ] `TimeoutManager.ts` - 超时定时器管理
- [ ] `processAction()` 方法 - 完整的操作处理流程
- [ ] requestId 去重缓存（LRU）

### 10.2 Interfaces 调整（必须）

```typescript
// GameState 新增字段
export interface GameState {
  // === 版本化字段 ===
  stateVersion: number;
  handId: string;
  roundId: string;
  
  // ... 现有字段
  actionHistory: ActionHistory[];  // 操作历史
}

// 操作历史
export interface ActionHistory {
  playerId: string;
  action: ActionType;
  amount: number;
  phase: GamePhase;
  timestamp: number;
}

// PlayerActionPayload 新增 requestId
export interface PlayerActionPayload {
  action: ActionType;
  amount?: number;
  roundIndex: number;
  requestId: string;  // 新增
}
```

### 10.3 Client 端

- [ ] 首页组件（创建/加入房间）
- [ ] 房间页组件（等待开始）
- [ ] 游戏页整合（牌桌+手牌+操作）
- [ ] 倒计时 UI
- [ ] 状态版本校验逻辑
- [ ] requestId 生成

---

## 十一、下一步实现优先级

```
P0 (核心游戏流程)
├── 1. Interfaces.ts 增加版本化字段
├── 2. processAction() 完整实现
├── 3. 下注轮流转逻辑
├── 4. 摊牌结算
└── 5. 新局初始化

P1 (健壮性)
├── 6. requestId 幂等去重
├── 7. 30秒超时处理
└── 8. 断线重连完整测试

P2 (用户体验)
├── 9. 客户端页面整合
└── 10. 结算动画
```

---

## 十二、Java 重构专项设计文档

> [!IMPORTANT]
> **Java 重构版本需要特别关注以下专项设计文档**：

| 文档 | 说明 | 状态 |
|------|------|------|
| [并发模型设计文档](./concurrency-model.md) | Node.js → Java 多线程迁移、房间级锁、死锁防护 | ✅ 已完成 |
| [DTO 体系设计规范](./dto-design-spec.md) | DTO 分层结构、JSON 序列化、字段访问控制 | ✅ 已完成 |
| [数值计算规范](./numeric-spec.md) | 筹码最小单位、溢出检查、边池计算规范 | ✅ 已完成 |
| [STOMP 协议适配设计](./stomp-adapter-design.md) | Socket.io → STOMP 迁移、自动重连、Session 管理 | ✅ 已完成 |
| [测试策略文档](./test-strategy.md) | 测试金字塔、并发测试、覆盖率目标、压力测试 | ✅ 已完成 |

### 12.1 并发模型关键差异

| 维度 | Node.js | Java |
|------|---------|------|
| **执行模型** | 单线程事件循环 | 多线程 + 线程池 |
| **房间并发** | 串行（天然） | 可并发（需加锁） |
| **锁机制** | 无需显式锁 | ReentrantLock（房间级） |
| **查询优化** | 天然无阻塞 | CopyOnWriteArrayList（去锁化） |

### 12.2 DTO 设计关键点

| 特性 | 实现方式 |
|------|----------|
| **分层结构** | 请求层 → 基础层 → 视图层 → 响应层 |
| **JSON 字段名** | `@JsonProperty` 明确指定（蛇形命名） |
| **敏感字段** | `@JsonIgnore` 隐藏手牌、内部ID |
| **序列化配置** | Jackson 全局配置 + 全局异常处理 |

### 12.3 数值计算关键规范

| 规则 | 说明 |
|------|------|
| **最小单位** | 1元/筹码（不支持小数） |
| **数据类型** | 使用 `int` 存储筹码 |
| **溢出检查** | 所有运算必须检查溢出 |
| **错误处理** | 溢出时立即抛出 `ArithmeticException` |

### 12.4 STOMP 协议迁移要点

| 特性 | Socket.io | STOMP |
|------|-----------|-------|
| **自动重连** | 内置 | 需手动实现（指数退避） |
| **房间管理** | 内置 API | 需基于 Session 管理 |
| **消息确认** | 内置 | 需显式配置 ACK |
| **协议标准** | 自定义 | RFC 6455 |

### 12.5 测试策略目标

| 测试类型 | 数量 | 覆盖率目标 |
|----------|------|------------|
| **单元测试** | ~165个 | >85% |
| **集成测试** | ~10个 | >70% |
| **并发测试** | ~10个 | - |
| **E2E 测试** | ~5个 | 主要流程 |
| **压力测试** | 持续进行 | 性能指标 |

---

## 十三、技术栈对比总结

| 组件 | 原技术栈 (Node.js) | 新技术栈 (Java) |
|------|-------------------|----------------|
| **语言** | TypeScript | Java 17 |
| **框架** | Express | Spring Boot 3.2+ |
| **通信** | Socket.io | WebSocket (STOMP) |
| **并发模型** | 单线程事件循环 | 多线程 + 线程池 |
| **线程安全** | 天然无竞态 | ReentrantLock + CopyOnWriteArrayList |
| **JSON 序列化** | JSON.stringify | Jackson |
| **测试框架** | Jest/Mocha | JUnit 5 + Mockito |
| **构建工具** | npm | Maven Wrapper |
