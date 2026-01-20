# Pocket Holdem (Java 重构版)

> 一款极致轻量化、专为熟人社交设计的德州扑克"在线自动发牌器"。
>
> **本项目是 [pocket-holdem-mvp](../pocket-holdem-mvp) 的 Java 重构版本**，采用 Spring Boot + UniApp 技术栈。

---

## 🎯 项目愿景

解决线下聚会没有筹码、洗牌慢、计算底池混乱的痛点。无需下载 App，点开即玩。

### 核心价值
- **零成本接入**：H5 环境（微信/浏览器）点开即玩
- **极致公平**：纯服务器端校验逻辑，杜绝前端抓包看牌
- **稳定体验**：完善的断线重连机制

---

## 🛠 技术栈

### 后端 (Server)
| 项目 | 技术 |
|------|------|
| 语言 | Java 17 |
| 框架 | Spring Boot 3.2+ |
| 通信 | WebSocket (STOMP) |
| 存储 | 内存 (ConcurrentHashMap) |

### 前端 (Client) - 计划中
| 项目 | 技术 |
|------|------|
| 框架 | UniApp (Vue 3 + TypeScript) |
| UI | Wot-Design-Uni |
| 平台 | H5 + 微信小程序 |

---

## 📂 目录结构

```
pocket-holdem-4j/
├── server/                      # Java Spring Boot 后端
│   └── src/main/java/com/pocketholdem/
│       ├── config/              # WebSocket 配置
│       ├── controller/          # 消息处理器
│       ├── engine/              # 核心扑克逻辑
│       ├── model/               # 数据模型
│       └── service/             # 业务逻辑
│
├── client/                      # UniApp 前端 (计划中)
│
├── docs/                        # 项目文档
│   ├── PRD.md                   # 产品需求文档
│   ├── architecture.md          # 技术架构文档
│   ├── websocket-protocol.md    # WebSocket 协议
│   ├── PLAN.md                  # 实施计划
│   └── CHANGELOG.md             # 变更日志
│
├── .opencode/
│   └── AGENT.md                 # AI 助手规则
│
└── README.md
```

---

## 🚀 快速开始

### 后端启动

```bash
cd server
./mvnw spring-boot:run
# 服务运行在 http://localhost:8080
```

### 运行测试

```bash
cd server
./mvnw test
```

---

## 📖 相关文档

- [产品需求文档 (PRD)](docs/PRD.md)
- [技术架构文档](docs/architecture.md)
- [WebSocket 协议规范](docs/websocket-protocol.md)
- [实施计划](docs/PLAN.md)
- [变更日志](docs/CHANGELOG.md)

---

## 🔗 原项目参考

本项目基于以下原型进行重构：
- **原项目路径**: `/Users/Hana/Codes/pocket-holdem-mvp`
- **原技术栈**: Node.js + Socket.io + Vue 3

---

## 📜 许可证

仅供技术研究与娱乐记分使用，**严禁**用于任何形式的赌博或非法集资活动。
