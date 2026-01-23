# AGENTS.md - AI 编码助手项目指南

> 本文件为 AI 编码助手在此代码库工作时提供指引。
> **语言要求**：所有回复、文档、代码注释必须使用 **中文**。
> **例外情况**：变量名、方法名、技术术语使用英文。

---

## 🚀 快速命令

### 环境配置

```bash
# ⚠️ 项目需要 Java 17，当前系统默认是 Java 8
# 临时切换（推荐添加到 ~/.zshrc）：
export JAVA_HOME=/usr/local/opt/openjdk@17
export PATH=$JAVA_HOME/bin:$PATH

# 或者添加 alias 到 ~/.zshrc：
alias java17='export JAVA_HOME=/usr/local/opt/openjdk@17 && export PATH=$JAVA_HOME/bin:$PATH'

# 验证版本
java -version  # 应显示 openjdk 17.x
```

### 构建与运行

```bash
cd server

# 编译项目
./mvnw compile

# 运行应用
./mvnw spring-boot:run
# 服务运行在 http://localhost:8080

# 清理并构建
./mvnw clean package

# 跳过测试构建
./mvnw clean package -DskipTests
```

### 测试命令

```bash
cd server

# 运行全部测试
./mvnw test

# 运行单个测试类
./mvnw test -Dtest=PokerEngineTest

# 运行单个测试方法
./mvnw test -Dtest=PokerEngineTest#testFlush

# 模式匹配运行测试
./mvnw test -Dtest=*Engine*

# 详细输出模式
./mvnw test -X
```

### 验证命令

```bash
# 仅检查编译是否通过
./mvnw compile -q && echo "✅ 编译成功" || echo "❌ 编译失败"

# 运行单元测试（排除集成测试）
./mvnw test -Dtest=\!*IntegrationTest
```

---

## 📁 项目结构

```
pocket-holdem-4j/
├── server/                      # Java Spring Boot 后端
│   ├── pom.xml                  # Maven 配置 (Java 17, Spring Boot 3.2)
│   └── src/
│       ├── main/java/com/pocketholdem/
│       │   ├── PocketHoldemApplication.java  # 入口类
│       │   ├── config/          # WebSocket/STOMP 配置
│       │   ├── controller/      # 消息处理器
│       │   ├── engine/          # 核心扑克逻辑（纯函数）
│       │   ├── model/           # 数据模型 (Card, Player, Room 等)
│       │   └── service/         # 业务逻辑层
│       ├── main/resources/
│       │   └── application.yml  # 应用配置
│       └── test/java/com/pocketholdem/
│
├── client/                      # UniApp 前端（计划中）
├── docs/                        # 项目文档
│   ├── PRD.md                   # 产品需求文档
│   ├── architecture.md          # 技术架构文档
│   ├── websocket-protocol.md    # WebSocket/STOMP 协议规范
│   ├── PLAN.md                  # 实施计划
│   └── CHANGELOG.md             # 变更日志
│
├── .opencode/                   # AI 助手配置
│   └── AGENT.md                 # 旧版规则（参见本文件）
└── README.md
```

---

## 🛠 技术栈

| 组件 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 17 |
| 框架 | Spring Boot | 3.2.x |
| 通信 | WebSocket (STOMP) | - |
| 构建工具 | Maven Wrapper | 3.9+ |
| 测试框架 | JUnit 5 | 5.x |
| 工具库 | Lombok | latest |
| JSON | Jackson | latest |
| 存储 | 内存 (ConcurrentHashMap) | - |

---

## 📝 代码风格规范

### 语言与注释

```java
// ✅ 正确：使用中文注释
/**
 * 计算边池
 * 当多个玩家 All-In 且筹码不一致时，需要拆分为多层底池
 * 
 * @param players 参与手牌的玩家列表
 * @return 底池列表（主池 + 边池）
 */
public List<Pot> calculateSidePots(List<Player> players) { }

// ❌ 错误：使用英文注释
// Calculate side pots when multiple players all-in
```

### 命名规范

| 元素 | 规范 | 示例 |
|------|------|------|
| 类名 | 大驼峰 | `PokerEngine`, `GameController` |
| 接口 | 大驼峰 | `GameState`, `PlayerAction` |
| 方法 | 小驼峰 | `calculateSidePots()`, `dealHoleCards()` |
| 变量 | 小驼峰 | `currentBet`, `playerIndex` |
| 常量 | 大写下划线 | `MAX_PLAYERS`, `DEFAULT_TIMEOUT` |
| 枚举 | 大驼峰 + 大写值 | `enum Suit { HEARTS, DIAMONDS }` |
| 包名 | 全小写 | `com.pocketholdem.engine` |

### Java 代码模式

```java
// ✅ 使用 Record 定义不可变数据
public record Card(Suit suit, Rank rank) {}

// ✅ 使用 Lombok 减少样板代码
@Data
@Slf4j
@Builder
public class Player {
    private String id;
    private String nickname;
    private int chips;
}

// ✅ 使用 Optional 处理可空返回
public Optional<Player> findPlayerById(String id) { }

// ❌ 禁止：集合类型返回 null，应返回空集合
public List<Card> getCommunityCards() {
    return communityCards != null ? communityCards : Collections.emptyList();
}
```

### 并发安全

```java
// ✅ 房间级别锁保护线程安全
public class Room {
    private final ReentrantLock lock = new ReentrantLock();
    
    public void processAction(Player player, Action action) {
        lock.lock();
        try {
            // 处理玩家操作
        } finally {
            lock.unlock();
        }
    }
}

// ✅ 使用 ConcurrentHashMap 管理共享状态
private final Map<String, Room> rooms = new ConcurrentHashMap<>();
```

### 导入顺序

1. `java.*` (标准库)
2. `javax.*`
3. `org.springframework.*`
4. 第三方库
5. `com.pocketholdem.*` (项目包)

---

## 🔗 原项目参考

> ⚠️ 本项目是 `/Users/Hana/Codes/pocket-holdem-mvp` (TypeScript) 的 Java 重构版。
> 实现功能前必须先参考原 TypeScript 实现。

### 关键参考文件

| 功能 | 原 TypeScript | Java 目标 |
|------|---------------|-----------|
| 扑克引擎 | `server/src/PokerEngine.ts` | `engine/PokerEngine.java` |
| 接口定义 | `server/src/Interfaces.ts` | `model/*.java` |
| 房间管理 | `server/src/RoomManager.ts` | `service/RoomManager.java` |
| 游戏控制 | `server/src/GameController.ts` | `controller/GameController.java` |
| 测试用例 | `server/src/poker.test.ts` | `test/*Test.java` |

---

## 🧪 测试要求

### 覆盖率目标
- 核心逻辑 (PokerEngine): **>80%**
- 控制器层: **>60%**
- 总体: **>70%**

### 测试结构

```java
@Test
@DisplayName("皇家同花顺应该击败四条")  // 中文测试名称
void royalFlushShouldBeatFourOfAKind() {
    // Given - 准备测试数据
    List<Card> royalFlush = createRoyalFlush();
    List<Card> fourOfAKind = createFourOfAKind();
    
    // When - 执行被测方法
    int result = PokerEngine.compareHands(royalFlush, fourOfAKind);
    
    // Then - 验证结果
    assertThat(result).isGreaterThan(0);
}
```

---

## 📋 文档更新要求

每次修改代码后，**必须**：

1. **更新 `docs/CHANGELOG.md`** - 记录日期、变更内容、影响文件
2. **检查 `README.md`** - 新功能或结构变化需同步更新
3. **更新 `docs/PLAN.md`** - 完成的任务标记 `[x]`
4. **自主提交代码** - 完成上述文档更新后，自动调用 `commit-manager` subagent 进行代码提交
   - 使用 `commit-manager` 自动分析变更并生成符合 Conventional Commits 标准的提交信息
   - 无需等待用户明确提示，在文件更新完成后即可自主执行提交流程
   - 提交信息需使用中文，清晰描述变更内容

---

## 🔌 WebSocket/STOMP 协议

### 端点模式

| 类型 | 路径 | 用途 |
|------|------|------|
| 订阅（广播） | `/topic/room/{roomId}` | 房间状态更新 |
| 订阅（私信） | `/user/queue/private` | 手牌等私密信息 |
| 发送（操作） | `/app/action` | 玩家操作 |
| 发送（加入） | `/app/join` | 加入房间 |

### 主要消息类型

- 客户端 → 服务端: `CREATE_ROOM`, `JOIN_ROOM`, `SIT_DOWN`, `PLAYER_ACTION`, `RECONNECT`
- 服务端 → 客户端: `ROOM_CREATED`, `SYNC_STATE`, `DEAL_CARDS`, `PLAYER_TURN`, `HAND_RESULT`

完整协议详见 `docs/websocket-protocol.md`。

---

## ⚠️ 关键约束

### 必须做

- [x] 所有回复使用 **中文**
- [x] 所有代码注释使用 **中文**
- [x] 所有 Git 提交信息使用 **中文**
- [x] 实现前先参考原 TypeScript 代码
- [x] 完成后运行 `mvn test` 验证
- [x] 变更后更新 CHANGELOG.md

### 禁止做

- [ ] 使用英文注释
- [ ] 不看原 TypeScript 就实现功能
- [ ] 跳过核心逻辑的测试
- [ ] 集合类型返回 `null`（应返回空集合）
- [ ] 吞掉异常不记录日志

---

## 🎯 架构原则

| 原则 | 说明 |
|------|------|
| **服务端权威** | 所有游戏逻辑仅在服务端执行 |
| **客户端只渲染** | 前端不参与游戏状态计算 |
| **房间级串行** | 同一房间内操作严格串行处理 |
| **状态版本化** | 每次状态变更递增 `stateVersion` |
| **操作幂等** | `requestId` + `roundIndex` 防止重复处理 |

---

## 🔄 工作流程总结

1. **编码前**: 阅读原项目对应的 TypeScript 实现
2. **编码中**: 中文注释，遵循上述规范
3. **编码后**:
   - 运行 `./mvnw test`
   - 更新 `docs/CHANGELOG.md`
   - 检查 `README.md` 是否需要同步
   - 在 `docs/PLAN.md` 标记完成的任务
