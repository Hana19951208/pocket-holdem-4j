# 变更日志

本文件记录项目的所有重要变更。格式遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)。

---

## [未发布]

### 2026-01-23

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
