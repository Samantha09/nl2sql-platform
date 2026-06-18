# 架构与数据流

## 微服务交互

```
前端(5173) -> 网关(8080) -> schema/query/ai 服务
                            <-> Nacos(8848) 注册发现
```

网关按路径前缀路由（`lb://` 负载均衡）：
- `/api/schema/**` -> schema-service
- `/api/query/**` -> query-service
- `/api/ai/**` -> ai-service

## NL 查询 MQ 事件链路

```
query-service  --nl2sql.event-->      ai-service（Mock LLM 转 SQL）
ai-service     --sql.ready.event-->   query-service（执行/记录）
query-service  --result.ready.event-->（内部可视化模块，占位）
```

- 三组事件，每组 exchange/queue/routingKey 常量集中在 `common` 的 `MqConst`
- 事件 DTO 在 `common/event/`：`NL2SQLEvent` / `SQLReadyEvent` / `ResultReadyEvent`
- 消息体 JSON 传输（非 JDK 序列化），每队列带死信（DLX/DLQ）

## 缓存设计

- 单层 Redis（`@Cacheable`/`@CacheEvict`），key 前缀 `nl2sql:`，按 cacheName 差异化 TTL
- 缓存区常量在 `common` 的 `CacheNames`，TTL 默认在 `CacheTtl`
- L2 二级缓存（Caffeine L1 + Redis L2）为后续 TODO，扩展点已预留

## 前端数据流

- API 层 `frontend/src/api/`：fetch 封装，经 Vite proxy 转发到网关 8080
- 智能查询：**真实后端优先，失败回退本地 mock**（`lib/adapt.ts` 归一两种结果）

详见 [基础设施约定](infrastructure.md)。
