# 项目规则与上下文

> ⚠️ **重要**: 本项目是基于 `/Users/Hana/Codes/pocket-holdem-mvp` 的 **Java 重构版本**，不是从零开始开发。
> 所有功能实现必须参考原项目的 TypeScript 代码和文档。

---

## 🌏 语言规范 (最高优先级)

| 规则 | 说明 |
|------|------|
| **所有回复** | 必须使用 **中文** |
| **文档编写** | 必须使用 **中文** |
| **代码注释** | 必须使用 **中文** |
| **Git Commit 信息** | 必须使用 **中文** |
| **变量/方法命名** | 英文 (符合 Java 规范) |

---

## 📝 文档更新规范

每次交互涉及文件变更时，**必须** 执行以下操作：

1. **更新 `docs/CHANGELOG.md`**
   - 记录变更内容、日期、影响范围
   - 使用语义化版本格式

2. **检查 `README.md` 是否需要同步更新**
   - 新增功能 → 更新功能列表
   - 目录结构变化 → 更新目录树
   - 启动方式变化 → 更新快速开始

3. **必要时更新 `docs/PLAN.md`**
   - 任务完成后打勾 `[x]`
   - 发现新任务时添加

---

## 🔗 原项目参考

| 参考项 | 路径 |
|--------|------|
| 原项目根目录 | `/Users/Hana/Codes/pocket-holdem-mvp` |
| 产品需求文档 (PRD) | `docs/PRD.md` (已复制) |
| 技术架构文档 | `docs/architecture.md` (已复制) |
| WebSocket 协议 | `docs/websocket-protocol.md` (已复制) |
| 原 TypeScript 后端 | `/Users/Hana/Codes/pocket-holdem-mvp/server/src/` |

**开发原则**: 
- 先阅读原项目对应模块的 TypeScript 实现
- 再用 Java 等价实现，保持功能一致
- 测试用例应能通过相同的输入输出验证

---

## 🛠 技术栈

### 后端 (本项目重点)
| 项目 | 版本/说明 |
|------|-----------|
| **语言** | Java 17 |
| **框架** | Spring Boot 3.2.x |
| **通信协议** | WebSocket (STOMP) |
| **构建工具** | Maven |
| **数据存储** | 内存 (ConcurrentHashMap)，MVP 阶段不使用数据库 |

### 前端 (后续重构)
| 项目 | 版本/说明 |
|------|-----------|
| **框架** | UniApp (Vue 3 + TypeScript) |
| **UI 库** | Wot-Design-Uni |
| **目标平台** | H5 (手机网页) + 微信小程序 |

---

## 💻 编码规范

### Java 代码规范

```java
// ✅ 正确：使用 Record 定义不可变数据
public record Card(Suit suit, Rank rank) {}

// ✅ 正确：使用 Lombok 减少样板代码
@Data
@Slf4j
public class Player {
    private String id;
    private String nickname;
    // ...
}

// ✅ 正确：中文注释
/**
 * 计算边池
 * 当多个玩家 All-In 且筹码不一致时，需要拆分为多层底池
 * 
 * @param players 参与手牌的玩家列表
 * @return 底池列表（主池 + 边池）
 */
public List<Pot> calculateSidePots(List<Player> players) {
    // ...
}

// ❌ 错误：英文注释
// Calculate side pots when multiple players all-in
```

### 并发控制规范

```java
// ✅ 正确：房间级别的锁保护
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
```

---

## 🔀 STOMP 协议规范

| 类型 | 路径模式 | 说明 |
|------|----------|------|
| **订阅 (广播)** | `/topic/room/{roomId}` | 房间状态更新 |
| **订阅 (私信)** | `/user/queue/private` | 个人手牌等私密信息 |
| **发送 (操作)** | `/app/action` | 玩家操作 |
| **发送 (加入)** | `/app/join` | 加入房间 |

---

## ✅ 工作流程

1. **开始任务前**：阅读原项目对应的 TypeScript 代码
2. **编写代码时**：保持功能一致，使用中文注释
3. **完成任务后**：
   - 运行测试 `./mvnw test`
   - 更新 `docs/CHANGELOG.md`
   - 检查 `README.md` 是否需要更新
   - 更新 `docs/PLAN.md` 中的任务状态

---

## 🧪 测试要求

- 使用 JUnit 5 编写单元测试
- 核心逻辑 (PokerEngine) 测试覆盖率 > 80%
- 测试用例参考原项目的 `*.test.ts` 文件
