# 变更日志

本文件记录项目的所有重要变更。格式遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)。

---

## [0.1.0] - 2026-01-25

### 阶段二：AI 助手配置优化 ✅

- **2026-01-25 14:06**：refactor(docs) 重构 AGENTS.md，实现渐进式技能加载

**技术亮点**：
- AGENTS.md 从 421 行精简到 188 行（-55%）
- 首次加载上下文从 ~14K 减少到 ~6K（-57%）
- 通用规范提取到可复用 skill，按需加载
- 新增 3 个项目特定规范章节：
  - Java 代码模式（antigravity-code-style）
  - WebSocket/STOMP 协议规范（antigravity-java-expert）
  - 项目测试要求（antigravity-code-reviewer）

---

### 阶段一：核心引擎开发 ✅

- **2026-01-23 16:33**：feat(engine) 完成阶段1核心引擎开发（TDD流程）
- **2026-01-23 16:19**：feat(engine) 实现底池分配逻辑
- **2026-01-23 15:17**：feat(engine) 实现边池计算核心算法

**技术亮点**：
- 测试覆盖率：93%（PokerEngine 行覆盖率 93%，分支覆盖率 84%）
- 测试用例：78 个（全部通过）
- 严格遵循 TDD 流程
- 所有筹码计算使用 ChipCalculator 安全运算

---

## [未发布]

### 阶段三：核心功能开发 🚧

**进行中的任务**：
- 实现房间管理服务（RoomManager）
- 实现游戏控制器（GameController）
- 实现 WebSocket/STOMP 消息处理

---

## 版本说明

- **[未发布]**: 正在开发中的功能
- **[x.y.z]**: 已发布的版本
  - x: 主版本号 (不兼容的 API 变更)
  - y: 次版本号 (向后兼容的功能新增)
  - z: 修订号 (向后兼容的问题修复)
