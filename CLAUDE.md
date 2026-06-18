# CLAUDE.md

NL2SQL 平台：用中文自然语言查询数据库，自动生成 SQL 并可视化。

## 技术栈

- 后端：Spring Boot 3.2.5 + Spring Cloud Alibaba 2023 + Java 17 + Maven（多模块）
- 前端：React 18 + TypeScript + Vite（`frontend/`，手写 SVG 图表，零图表依赖）
- 中间件：MySQL 8 / Redis 7 / RabbitMQ 3.12 / Nacos 2.4

## 模块与端口

| 模块 | 端口 | 职责 |
|------|------|------|
| gateway-service | 8080 | 网关路由（`/api/{schema,query,ai}/**`） |
| schema-service | 8081 | 数据源 & Schema 管理 |
| query-service | 8082 | NL 查询、SQL 执行、历史 |
| ai-service | 8083 | NL2SQL 转换（当前 Mock LLM） |
| common | — | 事件 DTO、统一响应 R、缓存/MQ/枚举/异常/i18n 基础设施 |
| frontend | 5173 | Vite dev server |

## 启动

```bash
# 1. 中间件
docker compose up -d
# 2. 后端（需 JAVA_HOME 指向 JDK17，见 docs/dev/operations.md）
mvn -DskipTests clean install
# 各服务：java -jar <module>/target/<module>-0.0.1-SNAPSHOT.jar
# 3. 前端
cd frontend && npm install && npm run dev
```

详细启动/重启/排错见 [docs/dev/operations.md](docs/dev/operations.md)。

## 约定

- 提交规范：Conventional Commits（`feat(scope): ...`），中文描述
- REST 统一返回 `R<T>`：`{code, message, data}`
- 仅在用户要求时提交；提交前清理构建产物，勿提交 `node_modules`/`target`/`*.tsbuildinfo`
- common 的基础设施依赖均为 `optional`，下游服务需显式声明 `data-redis`/`amqp` 才激活

## 索引

- [架构与数据流](docs/dev/architecture.md) — 微服务交互、MQ 事件链路、缓存设计
- [基础设施约定](docs/dev/infrastructure.md) — 缓存（Spring Cache 增强）、MQ（JSON+死信重试）、枚举/异常/i18n
- [运维操作](docs/dev/operations.md) — JAVA_HOME、启动/重启、常见排错（端口、MQ 拓扑冲突）
- [初始化设计规格](docs/superpowers/specs/2026-06-16-nl2sql-init-design.md)
- [待办：L2 二级缓存](docs/superpowers/plans/2026-06-18-cache-mq-infra-progress.md)
