# AGENTS.md - AI 编码助手项目指南

> 本文件为 AI 编码助手在此代码库工作时提供指引。
> **语言要求**：所有回复、文档、代码注释必须使用 **中文**。
> **例外情况**：变量名、方法名、技术术语使用英文。

---

## 📚 Skill 引用 (按需加载)

以下内容由专门的 skill 提供，按需自动加载：

| 技能名称 | 描述 | 加载时机 |
|----------|------|----------|
| `antigravity-code-style` | 代码风格规范（命名、注释、并发、导入顺序） | 编写/审查 Java 代码时 |
| `antigravity-java-expert` | Java 专家模式（Spring Boot、WebSocket/STOMP） | 实现后端功能时 |
| `antigravity-code-reviewer` | 代码审查（测试覆盖率、安全性、性能） | 代码审查时 |
| `antigravity-prd-writer` | PRD 编写规范 | 编写需求文档时 |

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
│   └── AGENT.md                 # 本文件
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

## 🌏 语言规范 (最高优先级)

| 规则 | 说明 |
|------|------|
| **所有回复** | 必须使用 **中文** |
| **文档编写** | 必须使用 **中文** |
| **代码注释** | 必须使用 **中文** |
| **Git Commit 信息** | 必须使用 **中文** |
| **变量/方法命名** | 英文 (符合 Java 规范) |

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

## 📝 文档更新规范

每次修改代码后，**必须**调用 `commit-manager` subagent 进行提交：
- 自动分析代码变更并生成符合 Conventional Commits 标准的提交信息
- 完成功能后可选择里程碑发布模式，自动更新 CHANGELOG.md
- 无需等待用户明确提示，在文件更新完成后即可自主执行提交流程
- 提交信息需使用中文，清晰描述变更内容

**commit-manager 路径**：`/Users/Hana/.config/opencode/agents/commit-manager.md`

---

## 🔄 工作流程总结

1. **编码前**: 阅读原项目对应的 TypeScript 实现
2. **编码中**: 中文注释，遵循代码风格规范（加载 `antigravity-code-style`）
3. **编码后**:
   - 运行测试（如需审查则加载 `antigravity-code-reviewer`）
   - 调用 `commit-manager` 提交代码
   - 在 `docs/PLAN.md` 标记完成的任务
