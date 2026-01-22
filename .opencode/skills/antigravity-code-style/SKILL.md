---
name: antigravity-code-style
description: Antigravity 首席辅助的代码风格规范 - 涵盖 Java、Python、前端的命名规范、注释规则、核心偏好
---

# Antigravity 代码风格规范

## 使用场景
- 编写任何代码时（Java、Python、JavaScript/TypeScript）
- 代码审查时检查代码风格
- 重构或优化代码时遵循统一标准
- 新项目初始化时建立代码规范

## 语言优先级
- 核心逻辑优先使用 **中文注释**
- 确保代码通俗易懂，便于团队协作

## 命名规范

### 通用规则
- **变量/函数**: camelCase (e.g., `getUserData`, `isLoading`)
- **类/组件/接口**: PascalCase (e.g., `UserProfile`, `IDataService`)
- **常量**: UPPER_SNAKE_CASE (e.g., `MAX_RETRY_COUNT`, `DEFAULT_TIMEOUT`)
- **私有成员**: 以下划线前缀标识 (e.g., `_internalCache`, `_validateInput`)
- **布尔值**: 以 `is`, `has`, `can` 开头 (e.g., `isValid`, `hasPermission`)

### Java 特有
- **包名**: 全小写，单词间不使用分隔符 (e.g., `com.antigravity.service`)
- **泛型**: 单字母大写 (e.g., `T`, `K`, `V`) 或描述性名称 (e.g., `UserDTO`)
- **异常类**: 以 `Exception` 结尾 (e.g., `BusinessException`)

### Python 特有
- **私有方法/属性**: 单下划线前缀 (e.g., `_calculate_metrics`)
- **Magic 方法**: 双下划线开头和结尾 (e.g., `__init__`, `__str__`)
- **类型别名**: 使用 TypeAlias (e.g., `UserId: TypeAlias = int`)

### 前端特有 (TypeScript/React)
- **组件**: PascalCase (e.g., `UserProfile.tsx`)
- **Hook**: 以 `use` 开头 (e.g., `useUserAuth`)
- **Props 接口**: 以 `Props` 结尾 (e.g., `ButtonProps`)

## 注释规则

### 必须使用中文注释
- 所有逻辑说明、复杂算法、业务规则必须使用中文注释
- 注释应当解释 **为什么**，而非 **做什么**（代码本身应清晰表达做什么）

### 注释示例
```java
// ❌ 错误：明显的注释
int getAge() {
    return age + 1; // 返回年龄加一
}

// ✅ 正确：解释业务逻辑
int getAge() {
    // 业务规则：周岁计算，出生当年算 1 岁
    return age + 1;
}
```

```python
# ✅ 正确：中文注释解释业务逻辑
def calculate_loan_interest(principal: float, rate: float, days: int) -> float:
    """
    计算贷款利息

    Args:
        principal: 本金金额
        rate: 日利率
        days: 计息天数

    Returns:
        应计利息金额
    """
    # 按日计息，到期一次性还本付息
    return principal * rate * days
```

### 注释位置规范
- **类/接口**: 类定义正下方
- **公开方法**: 方法定义正下方
- **复杂逻辑**: 逻辑块正上方
- **常量**: 常量定义正上方

### TODO/FIXME 标记
- 使用标准格式: `TODO: [描述] - [负责人]`
- 示例: `TODO: 后续优化缓存策略 - 张三`

## 代码格式化

### 缩进与空格
- **缩进**: 2 空格（禁止使用 Tab）
- **行宽**: 最大 100 字符
- **大括号**: Java 使用 K&R 风格 (左大括号不换行)
- **空格**: 运算符两侧必须空格 (e.g., `a + b`，而非 `a+b`)

### 引号与分号
- **JavaScript/TypeScript**:
  - 普通字符串: 单引号 `'string'`
  - JSX 属性: 双引号 `"<Button label='确定' />"`
- **Python**: 单引号 `'string'` 或双引号 `"string"` 均可，保持文件内一致
- **Java**: 必须使用分号

### 导入顺序 (Java)
1. `java.*` 包
2. `javax.*` 包
3. 第三方库 (按字母顺序)
4. 项目内部模块

## 架构模式

### 通用原则
1. **单一职责**: 每个类/函数只做一件事
2. **开闭原则**: 对扩展开放，对修改关闭
3. **依赖倒置**: 依赖抽象，而非具体实现
4. **DRY 原则**: Don't Repeat Yourself

### 函数设计
- **长度限制**: 单个函数不超过 50 行
- **参数数量**: 最多 4 个参数，超过则使用 DTO/参数对象
- **返回类型**: 避免返回 null，使用 Optional 或空集合

### 类设计
- **类长度**: 单个类不超过 500 行
- **方法数量**: 单个类不超过 20 个公开方法
- **继承深度**: 继承链不超过 3 层

## 错误处理

### 异常处理规范
- **Java**: 优先使用自定义业务异常 (e.g., `BusinessException`)
- **Python**: 明确异常类型，避免捕获泛型 `Exception`
- **前端**: 统一的错误处理 Hook

### 异步错误处理
- **必须**: 所有异步操作必须使用 try-catch
- **示例**:
```typescript
async function fetchUserData(userId: string): Promise<UserData> {
  try {
    const response = await api.get(`/users/${userId}`);
    return response.data;
  } catch (error) {
    // 记录错误日志，便于排查
    logger.error(`获取用户数据失败: userId=${userId}`, error);
    // 转换为业务异常，保持调用方接口稳定
    throw new UserOperationException('获取用户信息失败，请稍后重试');
  }
}
```

## 类型安全

### TypeScript/JavaScript
- **禁止使用**: `any` 类型（除非临时处理第三方库）
- **必须使用**: 显式类型定义、联合类型、泛型

### Python
- **必须使用**: 类型提示 (Type Hints)
- **类型检查**: 使用 mypy 或 pyright 进行静态类型检查

## Docker 与命令行规范

### Docker Compose V2 语法
- ✅ **正确**: `docker compose up -d`
- ❌ **错误**: `docker-compose up -d`

### HuggingFace 模型下载
- ✅ **正确**: `hf download --resume-download <model_id>`
- ❌ **错误**: `huggingface-cli download <model_id>`

### 前端包管理
- ✅ **正确**: `pnpm install`, `pnpm add <package>`
- ❌ **错误**: `npm install`, `yarn add`

## Git 提交规范

### 提交消息格式
- **必须使用中文描述**
- 格式: `<类型>(<范围>): <描述>`
- 示例:
  - `feat(用户模块): 新增用户登录功能`
  - `fix(支付模块): 修复订单金额计算错误`
  - `docs(README): 更新部署文档`

### 提交类型
- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档更新
- `style`: 代码格式（不影响功能）
- `refactor`: 重构
- `perf`: 性能优化
- `test`: 测试相关
- `chore`: 构建或辅助工具修改

## 文件同步检查

### README.md 同步
- 新增功能必须同步更新 README.md
- API 变更需要更新接口文档
- 配置变更需要更新部署文档

### 文件命名
- **配置文件**: `config.yaml`, `application.yml`
- **数据库脚本**: `V1__init_schema.sql`, `V2__add_user_table.sql`
- **测试文件**: `*Test.java`, `*_test.py`, `*.spec.ts`

## 特定语言最佳实践

### Java 最佳实践
- **Lombok**: 合理使用 `@Data`, `@Builder`, `@AllArgsConstructor`
- **Stream API**: 优先使用流式操作处理集合
- **Optional**: 避免 null 检查，使用 Optional 包装

### Python 最佳实践
- **虚拟环境**: 使用 conda 或 venv
- **依赖管理**: 使用 `requirements.txt` 或 `pyproject.toml`
- **日志规范**: 使用 `logging` 模块，统一日志格式

### 前端最佳实践
- **Ant Design**: 优先使用标准组件
- **状态管理**: 使用 React Hook 或 Zustand
- **样式方案**: 优先使用 CSS Modules 或 styled-components

## 性能优化准则

### 通用优化
- **数据库**: 避免 N+1 查询，合理使用索引
- **缓存**: 多级缓存策略（本地 + 分布式）
- **异步**: 合理使用异步处理提高吞吐量

### 代码层面
- **循环优化**: 避免在循环中执行数据库查询
- **对象复用**: 复用数据库连接、HttpClient 等资源
- **懒加载**: 按需加载，减少首屏时间
