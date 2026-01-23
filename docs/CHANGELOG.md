# 变更日志

本文件记录项目的所有重要变更。格式遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)。

---

## [未发布]

### 2026-01-23

#### 新增
- 实现花色枚举 Suit（TDD 流程）
  - 新增 `server/src/main/java/com/pocketholdem/model/Suit.java`
  - 新增 `server/src/test/java/com/pocketholdem/model/SuitTest.java`
  - 使用 `@JsonValue` 输出英文序列化值
  - 4 种花色：CLUBS, DIAMONDS, HEARTS, SPADES

#### 变更
- 重构 OpenCode AI 助手配置系统
  - 重命名 `.opencode/AGENT.md` -> `.opencode/AGENTS.md`
  - 新增项目级 `AGENTS.md`（328 行，包含完整开发规范）
  - 新增 `.opencode/skills/` 目录，包含 6 个专业技能包：
    - antigravity-code-reviewer（代码审查专家）
    - antigravity-code-style（代码风格专家）
    - antigravity-frontend-mentor（前端导师）
    - antigravity-java-expert（Java 专家）
    - antigravity-prd-writer（PRD 撰写专家）
    - antigravity-python-ai（Python AI 开发专家）
- 完善项目实施计划（docs/PLAN.md）
  - 整合架构师视角的核心考量点（架构对齐与差异处理、数据一致性与类型安全）
  - 新增阶段0：架构设计文档补充（2天）
  - 新增阶段四：测试与优化专项（2天）
  - 调整总工期至15-18天，新增255个测试方法，覆盖率达>80%
- 新增架构整改建议文档（.sisyphus/architecture-review-recommendations.md）
  - 对照原TS项目，识别10项核心改进建议
  - 定义P0/P1/P2风险项分级（3/3/2项）
  - 提供完整的6阶段实施方案
- 新增阶段0详细计划（.sisyphus/plans/phase0-architecture-design.md）
  - 包含5份架构设计文档：并发模型、DTO体系、数值计算、STOMP适配、测试策略

### 2024-01-20

#### 新增
- 初始化项目结构
- 创建 Spring Boot 3 + Java 17 工程骨架
- 创建项目文档体系
  - `README.md` - 项目说明
  - `docs/PRD.md` - 产品需求文档 (从原项目复制并整理)
  - `docs/PLAN.md` - 实施计划
  - `docs/CHANGELOG.md` - 变更日志
  - `.opencode/AGENT.md` - AI 助手规则

#### 确立
- 技术栈选型：Java 17 + Spring Boot 3 + STOMP + UniApp
- 项目规范：所有文档、注释、回复使用中文
- 原项目参考路径：`/Users/Hana/Codes/pocket-holdem-mvp`

---

## 版本说明

- **[未发布]**: 正在开发中的功能
- **[x.y.z]**: 已发布的版本
  - x: 主版本号 (不兼容的 API 变更)
  - y: 次版本号 (向后兼容的功能新增)
  - z: 修订号 (向后兼容的问题修复)
