# 运维操作与排错

## JAVA_HOME

系统 PATH 无 java，使用 IDEA 内置 JDK17：

```bash
export JAVA_HOME=~/.jdks/ms-17.0.19
export PATH=$JAVA_HOME/bin:$PATH
```

Maven 在 `~/.local/bin/mvn`（3.9.9）。

## 启动 / 重启后端服务

```bash
# 编译打包
mvn -DskipTests clean install
# 后台启动单个服务（nohup + disown 才能稳定持久化）
nohup $JAVA_HOME/bin/java -jar <module>/target/<module>-0.0.1-SNAPSHOT.jar \
  > /tmp/nl2sql-logs/<module>.log 2>&1 & disown
```

服务启动很快（**5-10 秒**）。判断就绪：日志出现 `Started XxxApplication in N seconds` 且端口监听。

## 常见排错

### 误判"启动慢"
日志文件被旧进程写过时，旧的 `Started ... in N seconds` 时间戳会误导。
**务必同时核对**：`ss -ltn | grep :<port>`（端口监听）+ `ps -p <PID>`（进程存活），不要只看日志。

### kill 后端口不释放
query-service 优雅关闭要等 ~30s（rabbitConnectionFactory）。
重启前确认端口释放：`until ! ss -ltn | grep -q ":<port> "; do sleep 1; done`。
必要时 `pkill -9 -f "<module>-0.0.1-SNAPSHOT.jar"`。

### MQ 拓扑冲突（PRECONDITION_FAILED）
改了 exchange 类型/队列参数后，RabbitMQ 中已存在的同名拓扑不可变，新声明被拒
（日志 `inequivalent arg 'type' ... received 'direct' but current is 'topic'`）。
删除旧拓扑让服务重连重建：

```bash
docker exec nl2sql-rabbitmq rabbitmqadmin -u admin -p admin delete exchange name=<ex>
docker exec nl2sql-rabbitmq rabbitmqadmin -u admin -p admin delete queue name=<q>
```

### 验证缓存 / MQ
```bash
docker exec nl2sql-redis redis-cli KEYS 'nl2sql:*'           # 缓存 key
docker exec nl2sql-redis redis-cli TTL 'nl2sql:schema:table::1:users'
docker exec nl2sql-rabbitmq rabbitmqctl list_queues name arguments  # 死信参数
```

## 中间件管理台

- Nacos: http://localhost:8848/nacos （nacos/nacos）
- RabbitMQ: http://localhost:15672 （admin/admin）

## 凭据（开发环境，见 .env）

MySQL root / `nl2sql123`，RabbitMQ admin / `admin`。

## 核心链路验证（NL → SQL → 真实执行）

前置：
1. 中间件已起：`docker compose up -d`（mysql / redis / rabbitmq / nacos）。
2. 配置 `MINIMAX_API_KEY`（或在 Nacos/local 用 `ENC(...)` 加密后的 key）。
3. 在 schema-service 录入一个 MySQL 数据源并触发扫描（`POST /api/schema/scan/{id}`），确保 `schema_cache` 有数据。
4. 启动 gateway / schema / query / ai 四个服务（见「启动 / 重启后端服务」）。

验证：
```bash
curl -X POST http://localhost:8080/api/query/nl \
  -H 'Content-Type: application/json' \
  -d '{"dataSourceId":1,"databaseName":"<你的库>","naturalLanguage":"查询订单总金额"}'
```
期望返回 `code=200`，`data.data` 为真实查询结果（非 mock）。LLM 不可用应返回 `code=12001`，目标库连不上返回 `code=13002`。

> 安全提示：本期无只读护栏，被查库请使用**只读账号**。
