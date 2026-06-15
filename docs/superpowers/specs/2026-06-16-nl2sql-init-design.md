# NL2SQL 平台项目初始化设计

> 版本：v1.0
> 日期：2026-06-16
> 来源：基于 `~/Downloads/NL2SQL项目起始文档.md` 进行初始化设计

---

## 1. 项目命名与位置

- **项目名**：`nl2sql-platform`
- **根目录**：`~/IdeaProjects/nl2sql-platform`
- 作为独立 Git 仓库初始化，包含 `.gitignore`、`README.md`、Conventional Commits 规范。

## 2. 项目范围

本次为**项目骨架初始化**，目标是把《AI 数据库自然语言问答平台》技术栈落地为可编译、可启动、可联调的微服务工程，业务逻辑以 Mock / 占位实现为主，真实能力在后续迭代中补齐。

关键决策（已与用户确认）：

| 决策项 | 选择 |
|--------|------|
| 初始化深度 | 完整骨架（所有模块 + Docker Compose + 基础配置） |
| 实现方案 | A. 严格按文档的微服务骨架 |
| AI 服务 | Mock LLM，保证端到端链路可跑通 |
| 前端范围 | 完整页面结构（所有视图 + 路由 + Pinia store） |

## 3. 后端目录结构

采用 Maven 多模块聚合工程：

```
nl2sql-platform/
├── pom.xml                         # 父 POM，统一定义版本、依赖、插件
├── common/                         # 公共模块（事件 DTO、统一响应 R、工具类）
├── gateway-service/                # Spring Cloud Gateway (8080)
├── schema-service/                 # 数据源 & Schema 管理 (8081)
├── query-service/                  # NL 查询 & SQL 执行 & 可视化 (8082)
└── ai-service/                     # NL2SQL 转换（本次为 Mock LLM）(8083)
```

- **Java 版本**：17
- **Spring Boot**：3.2.x
- **Spring Cloud Alibaba**：2023.x
- 每个服务拥有独立 `pom.xml` 与 `Dockerfile`，可独立构建镜像。

## 4. 基础设施

使用单一 `docker-compose.yml` 拉起全部中间件：

| 组件 | 镜像 | 端口 | 说明 |
|------|------|------|------|
| MySQL | `mysql:8.0` | 3306 | 三个库：`nl2sql_schema`、`nl2sql_query`、`nl2sql_ai` |
| Redis | `redis:7-alpine` | 6379 | 单机运行，文档中的 Cluster 逻辑以单机模拟 |
| RabbitMQ | `rabbitmq:3.12-management` | 5672 / 15672 | 预置 `nl2sql.exchange` 等交换机和队列 |
| Nacos | `nacos/nacos-server:v2.2.3` | 8848 / 9848 | standalone 模式，作为注册中心与配置中心 |

启动顺序遵循文档：先启动 MySQL / Redis / RabbitMQ / Nacos，再启动网关与各业务服务，最后启动前端。

## 5. 数据库初始化

每个服务对应独立数据库，通过 `schema.sql` 初始化：

- **`nl2sql_schema`**（schema-service）
  - `data_sources`：数据源配置
  - `schema_cache`：表结构缓存
  - `table_list_cache`：表列表缓存

- **`nl2sql_query`**（query-service）
  - `query_conversations`：查询会话
  - `query_history`：查询历史
  - `query_statistics`：查询统计
  - `notify_records`：通知记录

- **`nl2sql_ai`**（ai-service）
  - `prompt_templates`：Prompt 模板
  - `conversation_history`：对话历史

## 6. 前端目录结构

```
frontend/
├── package.json
├── vite.config.ts
├── tsconfig.json
└── src/
    ├── main.ts
    ├── App.vue
    ├── router/           # Vue Router 路由配置
    ├── views/            # Home / Query / Result / History / DataSource / SchemaViewer / Statistics
    ├── components/       # QueryInput / SQLPreview / ResultTable / ChartView / ConversationList / DataSourceForm
    ├── stores/           # Pinia：query / schema / user
    ├── api/              # Axios 封装 + schema/query/ai API
    └── utils/            # request.ts / websocket.ts
```

- **框架**：Vue 3 + Composition API
- **构建工具**：Vite
- **语言**：TypeScript
- **UI 库**：Element Plus
- **图表库**：ECharts（本次图表组件为占位实现）
- **状态管理**：Pinia
- **HTTP 客户端**：Axios

## 7. 业务逻辑范围（骨架阶段）

| 服务 | 本次实现 | 暂不实现 |
|------|----------|----------|
| gateway-service | 路由转发 `/api/schema/**`、`/api/query/**`、`/api/ai/**` | JWT 鉴权、Sentinel 限流（保留扩展入口） |
| schema-service | 数据源 CRUD、Schema 扫描接口（返回 Mock 数据）、表/列查询 API | 真实 JDBC 扫描、定时刷新、加密存储 |
| query-service | NL 查询接口（走 MQ → AI → 执行 Mock SQL → 返回结果）、SQL 查询接口、历史记录 CRUD | 真实 SQL 执行、SQL 安全校验、限流、飞书通知 |
| ai-service | `/api/ai/convert` Mock LLM、Prompt 模板读取 | 真实 LLM 调用、多轮对话上下文补全 |
| frontend | 页面路由、基础 UI、调用后端 API | WebSocket 实时推送、完整图表渲染 |

## 8. 事件与 MQ 设计（骨架阶段）

预置以下交换机和队列，服务间通过 RabbitMQ 解耦：

| 事件 | 生产者 | 消费者 | 说明 |
|------|--------|--------|------|
| `nl2sql.event` | query-service | ai-service | NL 转 SQL 请求 |
| `sql.ready.event` | ai-service | query-service | SQL 生成完成 |
| `result.ready.event` | query-service | query-service（可视化模块） | 结果待可视化 |
| `error.event` | query-service | notify 模块 | 异常告警（占位） |

公共事件 DTO 定义在 `common` 模块，供各服务共享。

## 9. 统一响应格式

后端所有 REST API 返回统一包装：

```json
{
  "code": 200,
  "message": "success",
  "data": { }
}
```

错误响应：

```json
{
  "code": 400,
  "message": "SQL validation failed",
  "data": null
}
```

## 10. 验证目标

骨架完成后应达到以下标准：

1. `docker compose up` 可启动 MySQL、Redis、RabbitMQ、Nacos。
2. 各 Java 服务可通过 `mvn spring-boot:run` 或容器方式启动，无启动期异常。
3. 前端 `npm run dev` 可正常显示首页，路由切换无报错。
4. 通过 Gateway 调用 schema / query / ai 三个服务 API，返回统一 `R<T>` 格式。
5. AI 服务 Mock 返回 SQL，Query 服务能返回 Mock 结果，端到端链路可跑通。

## 11. 后续迭代方向

- Phase 1 完成后继续补齐：真实 JDBC Schema 扫描、本地 LLM 集成、SQL 安全校验、Redis 限流、WebSocket 实时推送、图表自动推荐、飞书告警。

---

_文档版本：v1.0_
_最后更新：2026-06-16_
