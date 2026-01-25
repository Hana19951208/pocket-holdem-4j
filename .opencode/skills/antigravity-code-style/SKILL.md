---
name: antigravity-code-style
description: Antigravity 首席辅助的代码风格规范 - 涵盖 Java、前端的命名规范、注释规则、核心偏好
---

# Antigravity 代码风格规范

## 使用场景
- 编写任何代码时（Java、JavaScript/TypeScript）
- 代码审查时检查代码风格
- 重构或优化代码时遵循统一标准
- 新项目初始化时建立代码规范

## 语言优先级
- 核心逻辑优先使用 **中文注释**
- 确保代码通俗易懂，便于团队协作

### Java 工具库使用规范 (Hutool)

#### 使用原则
- **优先使用 Hutool**: 项目中优先使用 Hutool 工具包处理通用操作
- **避免重复造轮子**: Hutool 已提供成熟稳定的工具方法
- **保持一致性**: 统一使用 Hutool 提升代码可读性和可维护性

#### Hutool 依赖配置
```xml
<!-- pom.xml -->
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>5.8.28</version>
</dependency>
```

#### 常用工具类

##### 1. 集合工具 (CollUtil)
```java
import cn.hutool.core.collection.CollUtil;

// ✅ 正确：使用 CollUtil 初始化集合
List<String> players = CollUtil.newArrayList("player1", "player2");
Map<String, Integer> scores = CollUtil.newHashMap();

// ✅ 正确：使用 CollUtil 判空
if (CollUtil.isEmpty(list)) {
    return Collections.emptyList();
}

// ✅ 正确：使用 CollUtil 转换
List<Integer> ints = CollUtil.convert(Arrays.asList("1", "2", "3"), Integer::parseInt);

// ❌ 错误：手动初始化
List<String> players = new ArrayList<>();
players.add("player1");
```

##### 2. 字符串工具 (StrUtil)
```java
import cn.hutool.core.util.StrUtil;

// ✅ 正确：使用 StrUtil 判空
if (StrUtil.isBlank(username)) {
    throw new IllegalArgumentException("用户名不能为空");
}

// ✅ 正确：使用 StrUtil 格式化
String message = StrUtil.format("玩家 {} 加入了房间", playerId);

// ✅ 正确：使用 StrUtil 截取
String shortName = StrUtil.sub(nickname, 0, 10);

// ❌ 错误：手动判空
if (username == null || username.trim().isEmpty()) {
    throw new IllegalArgumentException("用户名不能为空");
}
```

##### 3. 对象工具 (ObjectUtil)
```java
import cn.hutool.core.util.ObjectUtil;

// ✅ 正确：使用 ObjectUtil 判空
if (ObjectUtil.isNull(user)) {
    return Optional.empty();
}

// ✅ 正确：使用 ObjectUtil 比较
if (ObjectUtil.equal(card1, card2)) {
    return true;
}

// ✅ 正确：使用 ObjectUtil 默认值
String nickname = ObjectUtil.defaultIfNull(user.getNickname(), "匿名");
```

##### 4. 数字工具 (NumberUtil)
```java
import cn.hutool.core.util.NumberUtil;

// ✅ 正确：使用 NumberUtil 运算（注意：本项目筹码计算仍需使用 ChipCalculator）
double result = NumberUtil.add(a, b);
boolean isGreater = NumberUtil.isGreater(a, b);

// ✅ 正确：使用 NumberUtil 格式化
String formatted = NumberUtil.formatDecimal(amount);

// ⚠️ 注意：筹码计算仍需使用 ChipCalculator（包含溢出检查）
// 不能使用 NumberUtil 替代 ChipCalculator 的安全运算
```

##### 5. Map 工具 (MapUtil)
```java
import cn.hutool.core.map.MapUtil;

// ✅ 正确：使用 MapUtil 初始化
Map<String, Integer> map = MapUtil.<String, Integer>builder()
    .put("key1", 1)
    .put("key2", 2)
    .build();

// ✅ 正确：使用 MapUtil 判空
if (MapUtil.isEmpty(map)) {
    return Collections.emptyMap();
}

// ✅ 正确：使用 MapUtil 获取值（带默认值）
Integer value = MapUtil.get(map, "key", 0);
```

##### 6. 数组工具 (ArrayUtil)
```java
import cn.hutool.core.util.ArrayUtil;

// ✅ 正确：使用 ArrayUtil 判空
if (ArrayUtil.isEmpty(array)) {
    return new String[0];
}

// ✅ 正确：使用 ArrayUtil 复制
String[] copy = ArrayUtil.copy(array);

// ✅ 正确：使用 ArrayUtil 拼接
String[] result = ArrayUtil.addAll(array1, array2);
```

##### 7. JSON 工具 (JSONUtil)
```java
import cn.hutool.json.JSONUtil;

// ✅ 正确：使用 JSONUtil 序列化
String json = JSONUtil.toJsonStr(object);

// ✅ 正确：使用 JSONUtil 反序列化
User user = JSONUtil.toBean(jsonStr, User.class);

// ✅ 正确：使用 JSONUtil 转换为 JSON 对象
JSONObject json = JSONUtil.parseObj(jsonStr);
```

##### 8. ID 生成器 (IdUtil)
```java
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.IdUtil;

// ✅ 正确：使用 UUID 生成唯一 ID
String roomId = UUID.randomUUID().toString();

// ✅ 正确：使用雪花算法生成 ID（分布式唯一）
long id = IdUtil.getSnowflakeNextId();
```

##### 9. 参数校验工具 (Validate)
```java
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.Assert;

// ✅ 正确：使用 Validator 校验
if (!Validator.isEmail(email)) {
    throw new IllegalArgumentException("邮箱格式不正确");
}

// ✅ 正确：使用 Assert 断言
Assert.notNull(user, "用户不能为空");
Assert.isTrue(age >= 18, "年龄必须大于等于18岁");
```

#### 使用场景对照表

| 操作 | JDK 原生 | Hutool 推荐 |
|------|----------|------------|
| 集合初始化 | `new ArrayList<>()` | `CollUtil.newArrayList()` |
| 集合判空 | `list == null \|\| list.isEmpty()` | `CollUtil.isEmpty(list)` |
| 字符串判空 | `str == null \|\| str.trim().isEmpty()` | `StrUtil.isBlank(str)` |
| Map 初始化 | `new HashMap<>()` | `CollUtil.newHashMap()` 或 `MapUtil.builder()` |
| 对象判空 | `obj == null` | `ObjectUtil.isNull(obj)` |
| JSON 序列化 | `ObjectMapper.writeValueAsString()` | `JSONUtil.toJsonStr()` |
| ID 生成 | `UUID.randomUUID()` | `UUID.randomUUID().toString()` 或 `IdUtil.getSnowflakeNextId()` |

#### 注意事项
1. **筹码计算**: 本项目筹码计算必须使用 `ChipCalculator`，不能被 Hutool 的 `NumberUtil` 替代
2. **溢出检查**: 涉及溢出检查的数值计算必须使用 `ChipCalculator`
3. **线程安全**: 多线程环境下的集合操作仍需使用 `ConcurrentHashMap` 等并发容器

### Java 项目特定规范 (Pocket Holdem 4j)

#### Java 代码模式
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

#### 导入顺序
1. `java.*` (标准库)
2. `javax.*`
3. `org.springframework.*`
4. 第三方库
5. `com.pocketholdem.*` (项目包)

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

## 特定语言最佳实践

### Java 最佳实践
- **Lombok**: 合理使用 `@Data`, `@Builder`, `@AllArgsConstructor`
- **Stream API**: 优先使用流式操作处理集合
- **Optional**: 避免 null 检查，使用 Optional 包装

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
