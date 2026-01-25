---
name: antigravity-java-expert
description: Java 后端深度专家模式 - Spring Framework、设计模式、重构、性能优化
---

# Java 后端专家指南 (Antigravity Style)

## 使用场景
- 处理 Java 后端逻辑、架构设计
- Spring Framework 相关问题（AOP、Bean 生命周期）
- 代码重构和性能优化
- 设计模式应用和最佳实践

## 核心技术栈

### 后端框架
- **Spring Boot**: 自动配置、内嵌服务器
- **Spring MVC**: Web 层开发
- **Spring Data JPA**: 数据访问层
- **Spring Security**: 安全框架

### 核心概念

#### 1. Spring Bean 生命周期
```
实例化 → 属性填充 → 初始化 → 销毁

阶段详解:
1. 实例化: 调用构造函数创建实例
2. 属性填充: 注入依赖（@Autowired、@Value）
3. 初始化:
   - BeanNameAware.setBeanName()
   - BeanFactoryAware.setBeanFactory()
   - ApplicationContextAware.setApplicationContext()
   - @PostConstruct 注解方法
   - InitializingBean.afterPropertiesSet()
   - 自定义 init-method
4. 销毁:
   - @PreDestroy 注解方法
   - DisposableBean.destroy()
   - 自定义 destroy-method
```

#### 2. AOP (面向切面编程)
```java
// 定义切面
@Aspect
@Component
public class LoggingAspect {

    // 切入点表达式
    @Pointcut("execution(* com.antigravity.service.*.*(..))")
    public void serviceLayer() {}

    // 前置通知
    @Before("serviceLayer()")
    public void logBefore(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        logger.info("方法 {} 开始执行，参数: {}", methodName, args);
    }

    // 后置通知
    @AfterReturning(pointcut = "serviceLayer()", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().getName();
        logger.info("方法 {} 执行完成，返回: {}", methodName, result);
    }

    // 环绕通知
    @Around("serviceLayer()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - startTime;
        logger.info("方法 {} 执行耗时: {} ms", joinPoint.getSignature(), duration);
        return result;
    }
}
```

#### 3. 事务管理
```java
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    // 事务传播行为
    @Transactional(propagation = Propagation.REQUIRED, 
                   isolation = Isolation.DEFAULT,
                   timeout = 30,
                   rollbackFor = Exception.class)
    public void createUserWithOrder(User user, Order order) {
        // 保存用户
        userRepository.save(user);
        
        // 设置用户ID到订单
        order.setUserId(user.getId());
        
        // 保存订单
        orderRepository.save(order);
        
        // 如果抛出异常，事务自动回滚
    }
}
```

## 设计模式应用

### 常用设计模式

#### 1. 单例模式 (Singleton)
```java
// 饿汉式（线程安全，推荐）
@Configuration
public class AppConfig {
    
    @Bean
    @Scope("singleton")
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("users");
    }
}

// 懒汉式（双重检查锁）
public class Singleton {
    private static volatile Singleton instance;
    
    private Singleton() {}
    
    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```

#### 2. 工厂模式 (Factory)
```java
// 简单工厂
public class PaymentFactory {
    
    public static Payment createPayment(String type) {
        return switch (type.toLowerCase()) {
            case "alipay" -> new AlipayPayment();
            case "wechat" -> new WechatPayment();
            case "credit" -> new CreditCardPayment();
            default -> throw new IllegalArgumentException("不支持的支付方式: " + type);
        };
    }
}

// 使用
Payment payment = PaymentFactory.createPayment("alipay");
payment.pay(100.0);
```

#### 3. 策略模式 (Strategy)
```java
// 策略接口
public interface DiscountStrategy {
    BigDecimal calculate(BigDecimal amount);
}

// 具体策略
public class PercentageDiscount implements DiscountStrategy {
    private final BigDecimal percentage;
    
    public PercentageDiscount(BigDecimal percentage) {
        this.percentage = percentage.divide(BigDecimal.valueOf(100));
    }
    
    @Override
    public BigDecimal calculate(BigDecimal amount) {
        return amount.multiply(BigDecimal.ONE.subtract(percentage));
    }
}

// 上下文
public class DiscountContext {
    private DiscountStrategy strategy;
    
    public void setStrategy(DiscountStrategy strategy) {
        this.strategy = strategy;
    }
    
    public BigDecimal execute(BigDecimal amount) {
        return strategy.calculate(amount);
    }
}
```

#### 4. 观察者模式 (Observer)
```java
// 观察者接口
public interface Observer {
    void update(String message);
}

// 主题
public class Subject {
    private List<Observer> observers = new ArrayList<>();
    
    public void attach(Observer observer) {
        observers.add(observer);
    }
    
    public void detach(Observer observer) {
        observers.remove(observer);
    }
    
    public void notify(String message) {
        observers.forEach(observer -> observer.update(message));
    }
}
```

#### 5. 责任链模式 (Chain of Responsibility)
```java
// 处理者抽象
public abstract class Handler {
    protected Handler next;
    
    public void setNext(Handler next) {
        this.next = next;
    }
    
    public abstract void handle(Request request);
}

// 具体处理者
public class AuthHandler extends Handler {
    @Override
    public void handle(Request request) {
        if (request.isAuthenticated()) {
            System.out.println("认证通过");
            if (next != null) {
                next.handle(request);
            }
        } else {
            System.out.println("认证失败");
        }
    }
}

public class ValidationHandler extends Handler {
    @Override
    public void handle(Request request) {
        if (request.isValid()) {
            System.out.println("参数验证通过");
            if (next != null) {
                next.handle(request);
            }
        } else {
            System.out.println("参数验证失败");
        }
    }
}
```

## 代码重构

### 重构技巧

#### 1. 提取方法
```java
// 重构前
public void processOrder(Order order) {
    // 验证订单
    if (order.getItems() == null || order.getItems().isEmpty()) {
        throw new IllegalArgumentException("订单不能为空");
    }
    if (order.getCustomer() == null) {
        throw new IllegalArgumentException("客户信息不能为空");
    }
    
    // 计算总价
    BigDecimal total = BigDecimal.ZERO;
    for (Item item : order.getItems()) {
        total = total.add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
    }
    
    // 保存订单
    orderRepository.save(order);
    
    // 发送通知
    notificationService.sendOrderCreated(order);
}

// 重构后
public void processOrder(Order order) {
    validateOrder(order);
    BigDecimal total = calculateTotal(order);
    saveOrder(order);
    sendNotification(order);
}

private void validateOrder(Order order) {
    if (order.getItems() == null || order.getItems().isEmpty()) {
        throw new IllegalArgumentException("订单不能为空");
    }
    if (order.getCustomer() == null) {
        throw new IllegalArgumentException("客户信息不能为空");
    }
}
```

#### 2. 用多态替代条件
```java
// 重构前
public BigDecimal calculateDiscount(OrderType type, BigDecimal amount) {
    return switch (type) {
        case NEW_CUSTOMER -> amount.multiply(BigDecimal.valueOf(0.1));
        case VIP -> amount.multiply(BigDecimal.valueOf(0.15));
        case HOLIDAY -> amount.multiply(BigDecimal.valueOf(0.2));
        default -> BigDecimal.ZERO;
    };
}

// 重构后
public interface DiscountStrategy {
    BigDecimal calculate(BigDecimal amount);
}

public class NewCustomerDiscount implements DiscountStrategy {
    @Override
    public BigDecimal calculate(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(0.1));
    }
}

// 使用策略模式
public BigDecimal calculateDiscount(DiscountStrategy strategy, BigDecimal amount) {
    return strategy.calculate(amount);
}
```

#### 3. 引入参数对象
```java
// 重构前
public User findUsers(String name, Integer age, String city, 
                      String status, Integer page, Integer size) {
    // ...
}

// 重构后
public class UserQuery {
    private String name;
    private Integer age;
    private String city;
    private String status;
    private Integer page = 1;
    private Integer size = 10;
    
    // getter/setter
}

public User findUsers(UserQuery query) {
    // ...
}
```

## 性能优化

### 1. 数据库优化
```java
// 使用投影查询，只查询需要的字段
@Query("SELECT new com.antigravity.dto.UserDTO(u.id, u.name, u.email) " +
       "FROM User u WHERE u.status = :status")
List<UserDTO> findUserDTOsByStatus(@Param("status") String status);

// 批量操作，减少数据库访问次数
@Transactional
public void batchInsertUsers(List<User> users) {
    users.forEach(userRepository::save);  // ❌ 错误：每次都提交事务
    
    // ✅ 正确：使用 batch
    userRepository.saveAll(users);
}

// 使用 @EntityGraph 解决 N+1 问题
@EntityGraph(attributePaths = {"orders", "roles"})
Optional<User> findByUsername(String username);
```

### 2. 缓存优化
```java
// 多级缓存
@Cacheable(value = "users", key = "#id", unless = "#result == null")
public User findById(Long id) {
    return userRepository.findById(id).orElse(null);
}

// 缓存预热
@EventListener(ApplicationReadyEvent.class)
public void cacheWarmup() {
    logger.info("开始缓存预热...");
    List<User> users = userRepository.findAll();
    users.forEach(user -> cacheManager.getCache("users").put(user.getId(), user));
    logger.info("缓存预热完成，共 {} 条数据", users.size());
}
```

### 3. 异步处理
```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}

@Service
public class NotificationService {

    @Async("taskExecutor")
    public void sendAsyncNotification(User user, String message) {
        // 异步发送通知，不阻塞主流程
        emailService.send(user.getEmail(), message);
    }
}
```

### 4. 连接池优化
```yaml
# application.yml
spring:
  datasource:
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      idle-timeout: 300000
      max-lifetime: 1200000
      connection-timeout: 30000
```

## 最佳实践

### 1. 异常处理
```java
// 自定义业务异常
public class BusinessException extends RuntimeException {
    private final String code;
    private final HttpStatus httpStatus;
    
    public BusinessException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }
}

// 全局异常处理
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        ErrorResponse error = new ErrorResponse(ex.getCode(), ex.getMessage());
        return new ResponseEntity<>(error, ex.getHttpStatus());
    }
}
```

### 2. 日志规范
```java
// 使用占位符，避免字符串拼接
logger.info("用户登录成功: userId={}, username={}", userId, username);

// 避免在生产环境打印敏感信息
logger.debug("用户请求参数: {}", JsonUtils.toJson(request));  // 仅调试时使用

// 记录关键操作
@Aspect
@Component
public class AuditLogAspect {

    @Around("@annotation(auditLog)")
    public Object audit(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        String operation = auditLog.value();
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            logger.info("操作成功: {}，耗时: {}ms", operation, System.currentTimeMillis() - startTime);
            return result;
        } catch (Exception e) {
            logger.error("操作失败: {}", operation, e);
            throw e;
        }
    }
}
```

### 3. API 设计规范
```java
// 统一响应格式
@Data
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;
    private long timestamp;
    
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("success");
        response.setData(data);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }
    
    public static <T> ApiResponse<T> error(String code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(Integer.parseInt(code));
        response.setMessage(message);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }
}
```

### 4. 参数校验
```java
// 使用注解校验
@Data
public class CreateUserRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20之间")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$", 
             message = "密码必须包含大小写字母和数字，且长度不少于8位")
    private String password;
    
    @Email(message = "邮箱格式不正确")
    private String email;
}

// Controller 层使用
@PostMapping("/users")
public ApiResponse<UserDTO> createUser(@Valid @RequestBody CreateUserRequest request, 
                                        BindingResult result) {
    if (result.hasErrors()) {
        throw new ValidationException(result.getFieldErrors());
    }
    // ...
}
```

## WebSocket/STOMP 协议规范

### 使用场景
- 实现基于 WebSocket 的实时通信功能
- 处理 STOMP 消息协议
- 管理房间订阅和消息广播

### 端点模式

| 类型 | 路径 | 用途 |
|------|------|------|
| 订阅（广播） | `/topic/room/{roomId}` | 房间状态更新 |
| 订阅（私信） | `/user/queue/private` | 手牌等私密信息 |
| 发送（操作） | `/app/action` | 玩家操作 |
| 发送（加入） | `/app/join` | 加入房间 |

### 主要消息类型

- **客户端 → 服务端**: `CREATE_ROOM`, `JOIN_ROOM`, `SIT_DOWN`, `PLAYER_ACTION`, `RECONNECT`
- **服务端 → 客户端**: `ROOM_CREATED`, `SYNC_STATE`, `DEAL_CARDS`, `PLAYER_TURN`, `HAND_RESULT`

### Spring Boot 配置示例

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-poker")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 启用简单消息代理，用于订阅
        registry.enableSimpleBroker("/topic", "/queue");
        // 设置应用目标前缀，用于发送
        registry.setApplicationDestinationPrefixes("/app");
    }
}
```

### 消息处理器示例

```java
@Controller
public class GameController {

    @MessageMapping("/action")
    @SendTo("/topic/room/{roomId}")
    public GameState handlePlayerAction(
            @DestinationVariable String roomId,
            PlayerAction action,
            Principal principal) {
        // 处理玩家操作并返回新状态
        return gameService.processAction(roomId, principal.getName(), action);
    }
}
```

### 最佳实践

1. **消息幂等性**: 使用 `requestId` + `roundIndex` 防止重复处理
2. **状态版本化**: 每次状态变更递增 `stateVersion`
3. **房间级串行**: 同一房间内操作严格串行处理
4. **错误处理**: 统一的异常处理，避免连接断开

---

## 常见问题与解决方案

### Q1: @Autowired vs @Resource 区别？
**方案**:
- `@Autowired` 是 Spring 注解，优先按类型匹配
- `@Resource` 是 Java 标准注解，优先按名称匹配
- **推荐**: Field 注入使用 `@Autowired`，Constructor 注入不使用注解

### Q2: @Transactional 不生效？
**方案**:
- 检查方法是否为 public
- 检查是否在同一类中调用（代理失效）
- 检查异常是否被 catch 吞掉
- 检查传播行为是否正确

### Q3: 如何处理循环依赖？
**方案**:
- 使用 `@Lazy` 延迟注入
- 重构代码，消除循环依赖
- 使用 `@PostConstruct` 打破依赖链

### Q4: 大表分页优化？
**方案**:
- 使用游标分页（基于 ID）
- 避免使用 OFFSET
- 索引优化

## 审查重点

### 1. Spring 最佳实践
- Bean 生命周期理解
- AOP 使用场景
- 事务边界正确
- 依赖注入方式

### 2. 设计模式应用
- 避免过度设计
- 模式使用场景正确
- 代码可读性

### 3. 性能优化
- 数据库查询优化
- 缓存使用正确
- 异步处理合理

### 4. 代码规范
- 命名清晰
- 中文注释
- 异常处理规范
