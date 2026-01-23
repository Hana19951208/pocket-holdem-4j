# 变更日志

本文件记录项目的所有重要变更。格式遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)。

---

## [未发布]

### 2026-01-23

#### 新增
- 实现筹码计算工具类 ChipCalculator（TDD 流程）
  - 新增 `server/src/main/java/com/pocketholdem/util/ChipCalculator.java`
  - 新增 `server/src/test/java/com/pocketholdem/util/ChipCalculatorTest.java`
  - 支持安全加法、减法、乘法、除法运算，包含溢出检查
  - 提供筹码增减合法性验证方法 `validateIncrement()` 和 `validateDecrement()`
- 实现扑克牌Record Card（TDD 流程）
  - 新增 `server/src/main/java/com/pocketholdem/model/Card.java`
  - 新增 `server/src/test/java/com/pocketholdem/model/CardTest.java`
  - 使用 `@JsonProperty` 指定 snake_case 序列化
  - 提供便捷方法 `Card.of(String suit, String rank)`
- 实现点数枚举 Rank（TDD 流程）
  - 新增 `server/src/main/java/com/pocketholdem/model/Rank.java`
  - 新增 `server/src/test/java/com/pocketholdem/model/RankTest.java`
  - 使用 `@JsonValue` 输出英文序列化值
  - 13 个点数：TWO(2) ~ ACE(14)，其中 J=11, Q=12, K=13, A=14
- 实现花色枚举 Suit（TDD 流程）
  - 新增 `server/src/main/java/com/pocketholdem/model/Suit.java`
  - 新增 `server/src/test/java/com/pocketholdem/model/SuitTest.java`
  - 使用 `@JsonValue` 输出英文序列化值
  - 4 种花色：CLUBS, DIAMONDS, HEARTS, SPADES
- 实现牌型等级枚举 HandRank（TDD 流程）
  - 新增 `server/src/main/java/com/pocketholdem/model/HandRank.java`
  - 新增 `server/src/test/java/com/pocketholdem/model/HandRankTest.java`
  - 使用 `@JsonValue` 输出 snake_case 序列化值
  - 10 种牌型：HIGH_CARD(1) ~ ROYAL_FLUSH(10)
- 实现 PokerEngine 框架和基础方法（TDD 流程）
  - 新增 `server/src/main/java/com/pocketholdem/model/EvaluatedHand.java`
    - 牌型评估结果 Record（包含 rank, rankName, bestCards, kickers, score）
  - 新增 `server/src/main/java/com/pocketholdem/engine/PokerEngine.java`
    - 纯函数工具类，所有方法均为静态
    - 实现 `evaluateFiveCards()` - 评估5张牌的牌型
    - 实现 `evaluateHand()` - 从手牌和公共牌中找出最佳5张牌组合
    - 实现 `compareHands()` - 比较两个牌型
    - 实现 `calculateScore()` - 计算牌型综合得分
   - 新增 `server/src/test/java/com/pocketholdem/engine/PokerEngineTest.java`
     - 测试引擎能够正确评估高牌
- 实现顺子检测功能（TDD 流程）
  - 新增 `detectStraight()` 私有方法
    - 支持常规顺子检测（5张连续点数）
    - 支持轮盘顺（A-2-3-4-5）特殊处理
    - 返回排序后的牌数组，非顺子返回 null
  - 新增 `createEvaluatedHand()` 私有辅助方法
  - 更新 `evaluateFiveCards()` 方法
    - 添加顺子检测逻辑
    - 添加同花检测逻辑
    - 支持同花顺、皇家同花顺识别
  - 修正 `shouldEvaluateHighCard()` 测试数据（原测试数据 A-K-Q-J-10 实际是顺子）
  - 新增 4 个顺子相关测试：
    - `shouldRecognizeNormalStraight()` - 常规顺子 3-4-5-6-7
    - `shouldRecognizeWheelStraight()` - 轮盘顺 A-2-3-4-5
    - `shouldNotRecognizeNonStraight()` - 非顺子（缺一张）
    - `shouldRecognizeStraightFlush()` - 同花顺 2-3-4-5-6

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
