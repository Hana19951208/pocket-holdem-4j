# DTO 体系设计规范

> 文档版本: v1.0
> 创建日期: 2026-01-23
> 作者: AI 助手

---

## 📋 概述

本文档定义了项目中数据传输对象（DTO）的分层结构、序列化规范、字段访问控制规则和消息类型多态体系。这些规范确保服务端与客户端的数据传输一致性、安全性和可维护性。

---

## 1. DTO 分层结构

### 1.1 分层原则

```
┌─────────────────────────────────────────────┐
│           响应DTO层 (Response)             │
│  - 服务端→客户端的响应模型                   │
│  - 包含公开数据，不包含敏感信息              │
│  - CreateRoomResponse, JoinRoomResponse   │
└─────────────────────────────────────────────┘
                    ▲
                    │ 映射
┌──────────────────┼─────────────────────────┐
│          视图DTO层 (View)                  │
│  - 根据场景的公开/私密数据                  │
│  - PlayerDTO, RoomDTO, GameStateDTO        │
└──────────────────┼─────────────────────────┘
                    │
                    │ 依赖
┌──────────────────┼─────────────────────────┐
│          基础DTO层 (Base)                  │
│  - 包含所有字段的完整数据模型                │
│  - Player, Room, GameState (实体类)         │
└──────────────────┼─────────────────────────┘
                    ▲
                    │ 转换
┌──────────────────┼─────────────────────────┐
│          请求DTO层 (Request)               │
│  - 客户端→服务端的请求模型                   │
│  - CreateRoomRequest, PlayerActionRequest  │
└─────────────────────────────────────────────┘
```

### 1.2 各层职责

| 层级 | 职责 | 示例 |
|------|------|------|
| **请求DTO** | 接收客户端请求，包含请求数据和元数据（requestId, timestamp） | `CreateRoomRequest`, `PlayerActionRequest` |
| **基础实体** | 完整的业务实体，包含所有字段和业务逻辑 | `Player`, `Room`, `GameState` |
| **视图DTO** | 根据场景暴露必要字段，隐藏敏感信息（手牌、内部ID） | `PlayerDTO`, `RoomDTO`, `GameStateDTO` |
| **响应DTO** | 封装操作结果，包含状态码、消息、数据 | `CreateRoomResponse`, `ErrorResponse` |

### 1.3 代码示例

#### 基础层：实体类

```java
package com.pocketholdem.model;

import lombok.Data;
import lombok.Builder;
import java.util.List;

/**
 * 玩家实体类（包含所有字段）
 */
@Data
@Builder
public class Player {
    private String id;                    // 玩家ID
    private String nickname;              // 昵称
    private int chips;                     // 筹码
    private PlayerStatus status;          // 状态（WATCHING, PLAYING, FOLDED）
    private List<Card> holeCards;         // 手牌（敏感信息）
    private int seatIndex;                 // 座位号
    private boolean isHost;               // 是否房主

    /**
     * 转换为公开DTO（不包含手牌）
     */
    public PlayerDTO toPublicDTO() {
        return PlayerDTO.builder()
            .id(id)
            .nickname(nickname)
            .chips(chips)
            .status(status)
            .seatIndex(seatIndex)
            .isHost(isHost)
            .holeCards(null)  // 不包含手牌
            .build();
    }

    /**
     * 转换为私密DTO（包含手牌）
     */
    public PlayerDTO toPrivateDTO() {
        return PlayerDTO.builder()
            .id(id)
            .nickname(nickname)
            .chips(chips)
            .status(status)
            .seatIndex(seatIndex)
            .isHost(isHost)
            .holeCards(holeCards.stream()
                .map(Card::toDTO)
                .collect(Collectors.toList()))  // 包含手牌
            .build();
    }
}
```

#### 视图层：DTO

```java
package com.pocketholdem.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pocketholdem.model.PlayerStatus;
import com.pocketholdem.model.Card;
import lombok.Data;
import lombok.Builder;
import java.util.List;

/**
 * 玩家DTO（根据场景包含/排除字段）
 */
@Data
@Builder
public class PlayerDTO {
    @JsonProperty("player_id")
    private String playerId;

    private String nickname;

    private int chips;

    @JsonProperty("player_status")
    private PlayerStatus status;

    @JsonProperty("seat_index")
    private Integer seatIndex;  // 旁观者为null

    @JsonProperty("is_host")
    private boolean isHost;

    @JsonProperty("is_dealer")
    private boolean isDealer;

    @JsonProperty("current_bet")
    private int currentBet;

    @JsonProperty("has_acted")
    private boolean hasActed;

    @JsonProperty("hole_cards")
    @JsonInclude(JsonInclude.Include.NON_NULL)  // 敏感字段：为null时不序列化
    private List<CardDTO> holeCards;  // 可能为null（公开DTO）

    @JsonProperty("last_action")
    private String lastAction;  // 最后一次操作（如"FOLD", "CHECK"）

    @JsonProperty("last_action_time")
    private Long lastActionTime;  // 最后操作时间戳
}
```

#### 请求层：请求DTO

```java
package com.pocketholdem.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.pocketholdem.dto.WebSocketMessage;

/**
 * 创建房间请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CreateRoomRequest extends WebSocketMessage {

    @JsonProperty("nickname")
    private String nickname;

    @JsonProperty("max_players")
    private Integer maxPlayers;  // 默认9人

    @JsonProperty("small_blind")
    private Integer smallBlind;  // 默认10

    @JsonProperty("big_blind")
    private Integer bigBlind;    // 默认20
}

/**
 * 玩家操作请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlayerActionRequest extends WebSocketMessage {

    @JsonProperty("player_id")
    private String playerId;

    @JsonProperty("action")
    private ActionType action;  // FOLD, CHECK, CALL, RAISE, ALL_IN

    @JsonProperty("amount")
    private Integer amount;  // 加注金额（仅RAISE时需要）

    @JsonProperty("round_index")
    private Integer roundIndex;  // 回合索引，用于幂等性检查
}
```

#### 响应层：响应DTO

```java
package com.pocketholdem.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pocketholdem.dto.RoomDTO;
import lombok.Data;
import lombok.Builder;

/**
 * 创建房间响应
 */
@Data
@Builder
public class CreateRoomResponse {
    @JsonProperty("room_id")
    private String roomId;

    @JsonProperty("room")
    private RoomDTO room;

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("message")
    private String message;
}

/**
 * 加入房间响应
 */
@Data
@Builder
public class JoinRoomResponse {
    @JsonProperty("room_id")
    private String roomId;

    @JsonProperty("room")
    private RoomDTO room;

    @JsonProperty("player_id")
    private String playerId;

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("message")
    private String message;
}

/**
 * 坐下响应
 */
@Data
@Builder
public class SitDownResponse {
    @JsonProperty("room_id")
    private String roomId;

    @JsonProperty("player_id")
    private String playerId;

    @JsonProperty("seat_index")
    private Integer seatIndex;

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("message")
    private String message;
}

/**
 * 游戏状态响应
 */
@Data
@Builder
public class GameStateResponse {
    @JsonProperty("room_id")
    private String roomId;

    @JsonProperty("state_version")
    private Long stateVersion;

    @JsonProperty("game_phase")
    private String gamePhase;

    @JsonProperty("current_player_id")
    private String currentPlayerId;

    @JsonProperty("pot")
    private Integer pot;

    @JsonProperty("community_cards")
    private List<CardDTO> communityCards;

    @JsonProperty("seated_players")
    private List<PlayerDTO> seatedPlayers;

    @JsonProperty("timestamp")
    private Long timestamp;
}

/**
 * 统一错误响应
 */
@Data
@Builder
public class ErrorResponse {
    @JsonProperty("code")
    private String code;  // 错误码（如"INVALID_JSON", "VALIDATION_ERROR"）

    @JsonProperty("message")
    private String message;  // 错误描述

    @JsonProperty("timestamp")
    private String timestamp;  // ISO格式时间戳

    @JsonProperty("details")
    private Object details;  // 额外详情（可选）
}
```

---

## 2. 字段访问控制规则

### 2.1 注解规范

#### 2.1.1 @JsonProperty - 明确JSON字段名

```java
@Data
@Builder
public class PlayerDTO {
    // ✅ 正确：使用蛇形命名明确JSON字段名
    @JsonProperty("player_id")
    private String playerId;

    @JsonProperty("player_status")
    private PlayerStatus status;

    @JsonProperty("seat_index")
    private Integer seatIndex;

    // ❌ 错误：不明确JSON字段名，可能因Jackson配置不同导致不一致
    private String playerId;  // 序列化结果可能是 "playerId" 或 "player_id"
}
```

**规则**：所有POJO类的公开字段必须使用 `@JsonProperty` 明确JSON字段名（统一使用蛇形命名）

#### 2.1.2 @JsonIgnore - 隐藏敏感字段

```java
@Data
@Builder
public class PlayerDTO {
    private String playerId;
    private String nickname;

    // ❌ 错误：敏感信息（内部ID）未隐藏
    private String internalId;  // 可能被序列化到JSON中

    // ✅ 正确：隐藏敏感字段
    @JsonIgnore
    private String internalId;

    @JsonIgnore
    private String sessionToken;  // 会话令牌
}
```

**规则**：敏感字段（内部ID、会话令牌、密码等）必须使用 `@JsonIgnore` 隐藏

#### 2.1.3 @JsonInclude - 排除null值

```java
@Data
@Builder
public class RoomDTO {
    private String roomId;
    private List<PlayerDTO> seatedPlayers;

    // ✅ 正确：排除null值，减少JSON体积
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<PlayerDTO> spectators;  // 旁观者列表（可能为null）

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String winnerId;  // 赢家ID（仅游戏结束时存在）
}
```

**规则**：可选字段（可能为null）使用 `@JsonInclude(NON_NULL)` 排除null值

#### 2.1.4 @JsonFormat - 格式化日期/时间

```java
@Data
@Builder
public class RoomDTO {
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastActionTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime gameStartTime;  // 指定时区
}
```

**规则**：日期时间字段使用 `@JsonFormat` 格式化为ISO格式（`yyyy-MM-dd HH:mm:ss`）

#### 2.1.5 @JsonValue - 自定义枚举输出

```java
public enum PlayerStatus {
    WATCHING("WATCHING"),
    PLAYING("PLAYING"),
    FOLDED("FOLDED"),
    ALL_IN("ALL_IN");

    private final String value;

    PlayerStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String toValue() {
        return value;
    }
}

// 序列化结果：
// {
//   "player_status": "PLAYING"  // 而非 "player_status": {"name": "PLAYING", "value": "PLAYING"}
// }
```

**规则**：枚举类型使用 `@JsonValue` 输出英文值（如"FOLD", "CHECK"）

### 2.2 序列化规则总结

| 规则 | 注解 | 适用场景 | 示例 |
|------|------|----------|------|
| **明确字段名** | `@JsonProperty` | 所有POJO类公开字段 | `@JsonProperty("player_id")` |
| **隐藏敏感字段** | `@JsonIgnore` | 内部ID、令牌、密码 | `@JsonIgnore` |
| **排除null值** | `@JsonInclude(NON_NULL)` | 可选字段 | `@JsonInclude(JsonInclude.Include.NON_NULL)` |
| **格式化时间** | `@JsonFormat` | 日期时间字段 | `@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")` |
| **枚举输出** | `@JsonValue` | 所有枚举类型 | `@JsonValue` |

---

## 3. JSON 序列化全局配置

### 3.1 Jackson 全局设置

```java
package com.pocketholdem.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson全局配置
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // 1. 容错处理：未知属性不报错
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 2. 时间格式：ISO格式而非时间戳
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        // 3. 缩进输出（开发环境，便于调试）
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        // 4. 空对象不报错
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        // 5. 枚举序列化 - 使用枚举上的@JsonValue注解
        // 注意：不要启用WRITE_ENUMS_USING_TO_STRING，会与@JsonValue冲突
        // 枚举通过@JsonValue或重写toString()方法控制序列化

        // 6. Java模块（支持LocalDateTime等）
        mapper.registerModule(new JavaTimeModule());

        return mapper;
    }
}
```

### 3.2 全局异常处理器

```java
package com.pocketholdem.exception;

import com.pocketholdem.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final DateTimeFormatter ISO_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * JSON解析错误（客户端发送了非法JSON）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleJsonParseException(
        HttpMessageNotReadableException ex
    ) {
        log.warn("JSON解析错误: {}", ex.getMostSpecificCause().getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .code("INVALID_JSON")
            .message("JSON格式错误: " + ex.getMostSpecificCause().getMessage())
            .timestamp(LocalDateTime.now().format(ISO_FORMATTER))
            .build();

        return ResponseEntity.badRequest().body(error);
    }

    /**
     * 参数校验错误（@Valid验证失败）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
        MethodArgumentNotValidException ex
    ) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));

        log.warn("参数校验失败: {}", message);

        ErrorResponse error = ErrorResponse.builder()
            .code("VALIDATION_ERROR")
            .message(message)
            .timestamp(LocalDateTime.now().format(ISO_FORMATTER))
            .build();

        return ResponseEntity.badRequest().body(error);
    }

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        log.warn("业务异常: [{}] {}", ex.getErrorCode(), ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .code(ex.getErrorCode())
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now().format(ISO_FORMATTER))
            .details(ex.getDetails())
            .build();

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);
    }

    /**
     * 未知异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknownException(Exception ex) {
        log.error("未知异常", ex);

        ErrorResponse error = ErrorResponse.builder()
            .code("INTERNAL_ERROR")
            .message("服务器内部错误")
            .timestamp(LocalDateTime.now().format(ISO_FORMATTER))
            .build();

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(error);
    }
}
```

### 3.3 业务异常定义

```java
package com.pocketholdem.exception;

import lombok.Getter;
import java.util.Map;

/**
 * 业务异常
 */
@Getter
public class BusinessException extends RuntimeException {
    private final String errorCode;
    private final Map<String, Object> details;

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = null;
    }

    public BusinessException(String errorCode, String message, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }
}
```

### 3.4 错误码定义

```java
package com.pocketholdem.exception;

/**
 * 错误码常量
 */
public class ErrorCodes {
    // 参数验证错误 (4xx)
    public static final String INVALID_JSON = "INVALID_JSON";
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String MISSING_REQUIRED_FIELD = "MISSING_REQUIRED_FIELD";

    // 业务逻辑错误 (4xx)
    public static final String ROOM_NOT_FOUND = "ROOM_NOT_FOUND";
    public static final String PLAYER_NOT_FOUND = "PLAYER_NOT_FOUND";
    public static final String PLAYER_ALREADY_IN_ROOM = "PLAYER_ALREADY_IN_ROOM";
    public static final String INVALID_ACTION = "INVALID_ACTION";
    public static final String INSUFFICIENT_CHIPS = "INSUFFICIENT_CHIPS";
    public static final String ROOM_FULL = "ROOM_FULL";

    // 服务器错误 (5xx)
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String DEADLOCK_DETECTED = "DEADLOCK_DETECTED";
}
```

---

## 4. WebSocket 消息类型多态体系

### 4.1 基类设计

```java
package com.pocketholdem.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * WebSocket消息基类（多态支持）
 */
@Data
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = CreateRoomRequest.class, name = "CREATE_ROOM"),
    @JsonSubTypes.Type(value = JoinRoomRequest.class, name = "JOIN_ROOM"),
    @JsonSubTypes.Type(value = PlayerActionRequest.class, name = "PLAYER_ACTION"),
    @JsonSubTypes.Type(value = ReconnectRequest.class, name = "RECONNECT"),
    @JsonSubTypes.Type(value = SitDownRequest.class, name = "SIT_DOWN")
})
public abstract class WebSocketMessage {

    @JsonProperty("type")
    private String type;  // 消息类型（用于多态分发）

    @JsonProperty("request_id")
    private String requestId;  // 请求ID（用于幂等性）

    @JsonProperty("timestamp")
    private Long timestamp;  // 客户端发送时间戳
}
```

### 4.2 子类示例

```java
package com.pocketholdem.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pocketholdem.dto.WebSocketMessage;
import com.pocketholdem.model.ActionType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 玩家操作请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlayerActionRequest extends WebSocketMessage {

    @JsonProperty("player_id")
    private String playerId;

    @JsonProperty("action")
    private ActionType action;  // FOLD, CHECK, CALL, RAISE, ALL_IN

    @JsonProperty("amount")
    private Integer amount;  // 加注金额（仅RAISE时需要）

    @JsonProperty("round_index")
    private Integer roundIndex;  // 回合索引，用于幂等性检查
}

/**
 * 创建房间请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CreateRoomRequest extends WebSocketMessage {

    @JsonProperty("nickname")
    private String nickname;

    @JsonProperty("max_players")
    private Integer maxPlayers;  // 默认9人

    @JsonProperty("small_blind")
    private Integer smallBlind;  // 默认10

    @JsonProperty("big_blind")
    private Integer bigBlind;    // 默认20
}

/**
 * 加入房间请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class JoinRoomRequest extends WebSocketMessage {

    @JsonProperty("room_id")
    private String roomId;

    @JsonProperty("nickname")
    private String nickname;
}

/**
 * 坐下请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SitDownRequest extends WebSocketMessage {

    @JsonProperty("player_id")
    private String playerId;

    @JsonProperty("seat_index")
    private Integer seatIndex;  // 座位号（0-8）
}

/**
 * 重连请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReconnectRequest extends WebSocketMessage {

    @JsonProperty("room_id")
    private String roomId;

    @JsonProperty("player_id")
    private String playerId;
}
```

### 4.3 消息转换器（可选方案）

如果不想使用Jackson的多态注解，可以使用消息转换器手动处理：

```java
package com.pocketholdem.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pocketholdem.dto.WebSocketMessage;
import com.pocketholdem.dto.request.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * WebSocket消息转换器
 */
@Slf4j
@Component
public class WebSocketMessageConverter {

    private final ObjectMapper objectMapper;
    private final Map<String, Class<? extends WebSocketMessage>> typeMap;

    public WebSocketMessageConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        // 消息类型映射
        this.typeMap = Map.of(
            "CREATE_ROOM", CreateRoomRequest.class,
            "JOIN_ROOM", JoinRoomRequest.class,
            "PLAYER_ACTION", PlayerActionRequest.class,
            "SIT_DOWN", SitDownRequest.class,
            "RECONNECT", ReconnectRequest.class
        );
    }

    /**
     * 将JSON字符串转换为WebSocketMessage对象
     */
    public WebSocketMessage convert(String json) throws Exception {
        // 1. 先解析出type字段
        Map<String, Object> map = objectMapper.readValue(json, Map.class);
        String type = (String) map.get("type");

        // 2. 根据type确定目标类
        Class<? extends WebSocketMessage> clazz = typeMap.get(type);
        if (clazz == null) {
            log.error("未知消息类型: {}", type);
            throw new IllegalArgumentException("未知消息类型: " + type);
        }

        // 3. 反序列化为目标类
        return objectMapper.readValue(json, clazz);
    }
}
```

### 4.4 WebSocket消息处理器

```java
package com.pocketholdem.controller;

import com.pocketholdem.dto.WebSocketMessage;
import com.pocketholdem.dto.request.*;
import com.pocketholdem.service.GameService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

/**
 * WebSocket消息处理器
 */
@Slf4j
@Controller
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    /**
     * 处理所有WebSocket消息（多态分发）
     */
    @MessageMapping("/action")
    public void handleMessage(@Payload WebSocketMessage message) {
        log.info("收到消息: {}", message.getType());

        switch (message.getType()) {
            case "CREATE_ROOM":
                gameService.createRoom((CreateRoomRequest) message);
                break;
            case "JOIN_ROOM":
                gameService.joinRoom((JoinRoomRequest) message);
                break;
            case "SIT_DOWN":
                gameService.sitDown((SitDownRequest) message);
                break;
            case "PLAYER_ACTION":
                gameService.processPlayerAction((PlayerActionRequest) message);
                break;
            case "RECONNECT":
                gameService.reconnect((ReconnectRequest) message);
                break;
            default:
                log.error("未知消息类型: {}", message.getType());
        }
    }
}
```

---

## 5. 最佳实践总结

### 5.1 DTO命名规范

| 类型 | 命名规范 | 示例 |
|------|----------|------|
| **实体类** | 单数名词 | `Player`, `Room`, `GameState` |
| **视图DTO** | 实体名 + DTO | `PlayerDTO`, `RoomDTO`, `GameStateDTO` |
| **请求DTO** | 动作 + Request | `CreateRoomRequest`, `PlayerActionRequest` |
| **响应DTO** | 动作 + Response | `CreateRoomResponse`, `JoinRoomResponse` |
| **错误响应** | ErrorResponse | `ErrorResponse` |

### 5.2 转换方法命名

```java
// 实体 → DTO
public PlayerDTO toPublicDTO() { }
public PlayerDTO toPrivateDTO() { }
public RoomDTO toRoomDTO() { }
public GameStateDTO toDTO() { }

// DTO → 实体（通常不需要，请求直接处理）
// Player fromDTO(PlayerDTO dto) { }
```

### 5.3 不变性原则

```java
// ✅ 推荐：DTO使用@Builder和final字段（不可变）
@Data
@Builder
public class PlayerDTO {
    private final String playerId;  // final字段
    private final String nickname;
    private final int chips;
}

// ❌ 不推荐：可变的DTO
public class PlayerDTO {
    private String playerId;
    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }
}
```

---

## 6. 常见问题

### 6.1 为什么要分层？

**原因**：
- **安全**：实体类包含敏感信息（手牌、内部ID），不能直接暴露给客户端
- **灵活**：不同场景返回不同数据（公开DTO vs 私密DTO）
- **解耦**：客户端和服务端通过DTO交互，实体类可以独立演进

### 6.2 为什么使用蛇形命名？

**原因**：
- **前端友好**：JavaScript社区普遍使用蛇形命名（如snake_case）
- **一致性**：统一JSON字段名风格，避免混淆
- **可读性**：蛇形命名在URL、API文档中更易读

### 6.3 为什么枚举使用@JsonValue？

**原因**：
- **简洁**：输出 `"PLAYING"` 而非 `{"name":"PLAYING","value":"PLAYING"}`
- **兼容性**：与JavaScript枚举风格一致
- **减少体积**：减少JSON数据传输量

---

**文档版本**: v1.0
**最后更新**: 2026-01-23
