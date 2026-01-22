---
name: antigravity-code-reviewer
description: 基于 Antigravity 规范的全面代码审查流程 - 安全性、性能、可维护性、测试覆盖
---

# 代码审查指南 (Antigravity Style)

## 使用场景
- 代码合并前的质量检查
- 发现潜在问题和改进点
- 确保代码符合 antigravity-code-style 规范
- 保障代码安全性、性能和可维护性

## 审查维度

### 1. 安全性 (Security)
- **SQL 注入**: 检查参数化查询使用
- **XSS 攻击**: 检查输出编码
- **CSRF 攻击**: 检查 Token 验证
- **敏感数据**: 检查脱敏处理
- **权限控制**: 检查认证授权逻辑

### 2. 性能 (Performance)
- **数据库**: N+1 查询、索引使用
- **缓存**: 缓存策略、一致性
- **代码**: 循环优化、异步处理
- **资源**: 连接池、内存管理

### 3. 可维护性 (Maintainability)
- **代码风格**: 命名、格式、注释
- **复杂度**: 函数/类长度、圈复杂度
- **设计**: 单一职责、依赖关系
- **重复**: 代码重复抽取

### 4. 测试覆盖 (Test Coverage)
- **单元测试**: 核心逻辑覆盖
- **边界测试**: 异常情况覆盖
- **测试质量**: 用例独立、可重复

### 5. 业务逻辑 (Business Logic)
- **正确性**: 功能实现正确
- **完整性**: 边界条件处理
- **一致性**: 与需求一致

## 审查流程

### Step 1: 审查准备
1. **理解需求**: 确认功能目标和验收标准
2. **查看变更**: 了解改动范围和内容
3. **关联文档**: 查看 PRD、设计文档
4. **加载规范**: 加载 antigravity-code-style 技能

### Step 2: 自动化检查
1. **代码格式**: ESLint/Prettier 检查
2. **类型检查**: TypeScript/Javaava 类型
3. **安全扫描**: 依赖漏洞扫描
4. **测试覆盖**: 覆盖率报告

### Step 3: 人工审查
按照 `references/review-checklist.md` 逐项检查：

#### 3.1 安全性检查
```markdown
## 安全性检查
- [ ] SQL 注入检查
  ```java
  // ✅ 正确：参数化查询
  @Query("SELECT u FROM User u WHERE u.id = :id")
  User findById(@Param("id") Long id);
  
  // ❌ 错误：字符串拼接
  @Query("SELECT u FROM User u WHERE u.id = " + id)
  ```
  
- [ ] XSS 攻击检查
  ```typescript
  // ✅ 正确：输出编码
  <div dangerouslySetInnerHTML={{ __html: sanitize(userContent) }} />
  
  // ❌ 危险：直接渲染未处理内容
  <div dangerouslySetInnerHTML={{ __html: userContent }} />
  ```
  
- [ ] 敏感数据脱敏
  ```java
  // ✅ 正确：日志脱敏
  logger.info("用户登录: phone={}", PhoneUtils.mask(phone));
  ```
```

#### 3.2 性能检查
```markdown
## 性能检查
- [ ] N+1 查询问题
  ```java
  // ❌ 错误：循环查询
  for (Order order : orders) {
      List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
  }
  
  // ✅ 正确：批量查询
  List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());
  Map<Long, List<OrderItem>> itemsMap = orderItemRepository.findByOrderIds(orderIds);
  ```
  
- [ ] 缓存使用
  ```java
  // ✅ 正确：缓存命中
  @Cacheable(value = "user", key = "#id")
  public User findById(Long id) {
      return userRepository.findById(id).orElse(null);
  }
  ```
```

#### 3.3 代码规范检查
```markdown
## 代码规范检查
- [ ] 命名清晰
  ```java
  // ❌ 错误：命名不清晰
  public void doIt(List d) {
      for (Object o : d) {
          // ...
      }
  }
  
  // ✅ 正确：命名清晰
  public void processUserData(List<UserDTO> userList) {
      for (UserDTO user : userList) {
          // ...
      }
  }
  ```
  
- [ ] 中文注释
  ```java
  // ✅ 正确：中文注释解释业务逻辑
  // 业务规则：用户年满 18 岁才能进行实名认证
  if (user.getAge() >= 18) {
      // ...
  }
  ```
```

### Step 4: 输出审查报告

按照标准格式输出审查结果：

```markdown
## 代码审查结果

### 🔴 严重问题 (必须修复)
| 文件 | 行号 | 问题 | 影响 | 建议 |
|------|------|------|------|------|
| UserService.java | 45 | SQL 注入风险 | 数据库可能被攻击 | 使用参数化查询 |

### 🟡 建议优化 (强烈建议)
| 文件 | 行号 | 问题 | 影响 | 建议 |
|------|------|------|------|------|
| OrderController.java | 120 | 方法过长 | 可维护性差 | 拆分为多个私有方法 |

### ✅ 通过项
- 业务逻辑实现正确
- 符合 antigravity-code-style 规范
- 异常处理完整
- 日志记录规范

### 总体评价
整体代码质量良好，建议修复严重问题后合并。

### 下一步行动
- [ ] 修复 UserService.java:45 的 SQL 注入问题
- [ ] 优化 OrderController.java:120 的长方法
- [ ] 补充 OrderService 的单元测试
- [ ] 更新 README.md
```

## 审查重点

### Java 专项
- **Spring**: AOP 使用、Bean 生命周期、事务边界
- **设计模式**: 合理使用、避免过度设计
- **性能**: 数据库查询优化、缓存使用
- **异常**: 业务异常 vs 系统异常区分

### Python 专项
- **类型提示**: 完整的类型定义
- **日志**: logging 模块使用
- **异常**: 明确异常类型
- **依赖**: requirements.txt 完整

### 前端专项 (React/TypeScript)
- **类型安全**: 无 any 类型
- **状态管理**: React Hook 正确使用
- **性能**: 避免不必要的重渲染
- **组件**: Ant Design 标准用法

## 与其他技能配合

### 与 antigravity-code-style 配合
- 加载 antigravity-code-style 技能进行规范检查
- 确保代码符合 antigravity 命名规范和注释要求

### 与 antigravity-prd-writer 配合
- 审查代码是否与 PRD 一致
- 验证功能是否满足验收标准

### 与 antigravity-java-expert 配合
- 复杂 Java 问题深入分析
- Spring 最佳实践建议

## 常见问题与解决方案

### Q1: 发现性能问题怎么办？
**方案**: 记录在建议优化项，附上性能分析数据和优化建议

### Q2: 发现安全问题怎么办？
**方案**: 必须标记为严重问题，要求立即修复

### Q3: 代码风格不一致怎么办？
**方案**: 指出不符合 antigravity-code-style 的地方，提供正确示例

### Q4: 缺少测试怎么办？
**方案**: 记录在建议优化项，建议补充测试用例和覆盖范围

## 审查检查清单

### 审查前
- [ ] 理解需求和功能目标
- [ ] 查看变更范围和内容
- [ ] 加载 antigravity-code-style 技能
- [ ] 运行自动化检查工具

### 审查中
- [ ] 安全性检查通过
- [ ] 性能检查通过
- [ ] 代码规范检查通过
- [ ] 业务逻辑检查通过
- [ ] 测试覆盖检查通过

### 审查后
- [ ] 输出结构化审查报告
- [ ] 区分严重问题和优化建议
- [ ] 提供具体修复建议
- [ ] 确定下一步行动

## 最佳实践

1. **客观公正**: 关注代码质量，不针对个人
2. **建设性**: 提出的问题要有建议解决方案
3. **及时响应**: 尽快完成审查，不阻塞流程
4. **持续改进**: 总结审查经验，优化规范
5. **知识共享**: 将典型问题分享给团队

## 审查效率技巧

1. **先看整体**: 了解架构设计和代码结构
2. **重点突破**: 关注核心逻辑和复杂部分
3. **工具辅助**: 充分利用 IDE 和自动化工具
4. **批量处理**: 集中时间进行审查，提高效率
5. **记录总结**: 积累常见问题和解决方案

## 注意事项

- **尊重他人**: 以建设性的方式提出问题
- **关注重点**: 不纠缠于个人偏好，关注核心问题
- **及时反馈**: 快速完成审查，不拖延
- **持续学习**: 了解新技术和最佳实践
