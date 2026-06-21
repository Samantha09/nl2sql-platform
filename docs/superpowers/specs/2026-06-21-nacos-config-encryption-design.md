# Nacos Config 集中配置 + AES-256-GCM 加密设计规格

> 创建：2026-06-21  
> 背景：NL2SQL 平台测试基础设施已补齐，下一步按 backlog 推进配置外部化与敏感信息处理。当前配置分散在 3 个业务服务的 `application.yml` 中，且 MySQL/RabbitMQ 密码使用明文默认值。本规格定义引入 Nacos Config 集中管理配置，并对敏感字段使用 AES-256-GCM 加密。

---

## 1. 目标

1. 引入 Nacos Config 配置中心，统一管理 `schema-service`、`query-service`、`ai-service` 的所有配置。
2. 将 MySQL、RabbitMQ 等敏感密码从明文默认值改为 AES-256-GCM 密文，存储在 Nacos 上。
3. 解密密钥通过环境变量 `NL2SQL_ENCRYPT_KEY` 注入，不落地到配置文件或代码仓库。
4. 保留本地 `application-local.yml` 作为开发期 fallback，降低本地开发门槛。
5. 所有改动在新分支开发，验证无误后通过 PR 合并回 `main`。

---

## 2. 非目标

- 本期**不对接外部 KMS/Vault**（如阿里云 KMS、HashiCorp Vault）。
- 本期**不迁移 gateway-service 配置**（网关配置相对独立，可后续单独处理）。
- 本期**不改动业务代码行为**，仅调整配置加载方式与敏感字段存储形式。

---

## 3. 总体架构

```
                 ┌─────────────────────────────────────┐
                 │           Nacos Config              │
                 │  ┌─────────────────────────────┐    │
                 │  │ nl2sql-common.yml           │    │
                 │  │ (Redis/RabbitMQ/Actuator/   │    │
                 │  │  Swagger/Nacos discovery)   │    │
                 │  └─────────────────────────────┘    │
                 │  ┌─────────────────────────────┐    │
                 │  │ nl2sql-schema-service.yml   │    │
                 │  │ nl2sql-query-service.yml    │    │
                 │  │ nl2sql-ai-service.yml       │    │
                 │  └─────────────────────────────┘    │
                 │              ↑ ENC(...) 密文         │
                 └──────────────┬──────────────────────┘
                                │ 启动时拉取配置
     NL2SQL_ENCRYPT_KEY ────────┼──────────┐
     (环境变量 Base64 AES 密钥)   │          │
                                ↓          ↓
                 ┌─────────────────────────────────────┐
                 │ EncryptedPropertyEnvironmentPostProcessor
                 │    识别 ENC(...) 并调用 SecureConfigEncryptor
                 │           解密后注入 Environment
                 └─────────────────────────────────────┘
                                ↓
                 ┌─────────────┬─────────────┬──────────┐
                 │schema-service│query-service│ai-service│
                 └─────────────┴─────────────┴──────────┘
```

---

## 4. Nacos Config 配置方案

### 4.1 依赖

根 pom 已管理 `spring-cloud-alibaba-dependencies`。各服务引入：

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
```

### 4.2 本地 bootstrap.yml

每个业务服务新增 `src/main/resources/bootstrap.yml`，只保留 Nacos 连接信息与服务名：

```yaml
spring:
  application:
    name: nl2sql-schema-service   # 每个服务不同
  cloud:
    nacos:
      config:
        server-addr: ${NACOS_SERVER:localhost:8848}
        namespace: ${NACOS_NAMESPACE:}
        group: ${NACOS_GROUP:DEFAULT_GROUP}
        file-extension: yml
        shared-configs:
          - data-id: nl2sql-common.yml
            group: ${NACOS_GROUP:DEFAULT_GROUP}
            refresh: true
```

### 4.3 Nacos 上创建的配置

| data-id | group | 说明 |
|---------|-------|------|
| `nl2sql-common.yml` | `DEFAULT_GROUP` | 共享配置：Redis、RabbitMQ、Nacos discovery、Actuator、Swagger、日志 |
| `nl2sql-schema-service.yml` | `DEFAULT_GROUP` | schema-service 专有：端口、数据源、JPA、缓存 TTL |
| `nl2sql-query-service.yml` | `DEFAULT_GROUP` | query-service 专有：端口、数据源、JPA、RabbitMQ、缓存/MQ 配置 |
| `nl2sql-ai-service.yml` | `DEFAULT_GROUP` | ai-service 专有：端口、数据源、JPA、RabbitMQ、MQ 配置 |

### 4.4 配置内容示例

**nl2sql-common.yml**

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: 6379
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: 5672
    username: ${RABBITMQ_USER:admin}
    password: ENC(ABCD...==)
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER:localhost:8848}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always

springdoc:
  swagger-ui:
    path: /swagger-ui.html
  api-docs:
    path: /v3/api-docs

logging:
  level:
    root: info
```

**nl2sql-schema-service.yml**

```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:3306/nl2sql_schema?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: ENC(XXXX...==)
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

nl2sql:
  cache:
    key-prefix: "nl2sql:"
    default-ttl: 10m
    null-ttl: 60s
    cache-null-values: true
    ttls:
      ds:list: 5m
      schema:table: 30m
      schema:tables: 10m
```

query/ai 服务类似，按原 `application.yml` 拆分。

### 4.5 本地开发 fallback

每个服务保留 `src/main/resources/application-local.yml`，内容与 Nacos 未加密版一致，方便本地无 Nacos 时开发：

```yaml
spring:
  config:
    activate:
      on-profile: local
```

启动时加 `--spring.profiles.active=local` 或设置 `SPRING_PROFILES_ACTIVE=local`。

---

## 5. AES-256-GCM 加密方案

### 5.1 算法选择

- **算法**：AES-256-GCM
- **密钥长度**：256 bit（32 字节）
- **IV 长度**：96 bit（12 字节），每次随机生成
- **Auth Tag**：128 bit（16 字节），GCM 内置
- **填充**：NoPadding（GCM 不需要）

### 5.2 密文格式

```
ENC(BASE64(iv || ciphertext || authTag))
```

- `||` 表示字节拼接
- Base64 编码整个拼接结果
- 外层包 `ENC(...)`，便于 `EnvironmentPostProcessor` 识别

### 5.3 工具类 `SecureConfigEncryptor`

位置：`common/src/main/java/com/nl2sql/common/encrypt/SecureConfigEncryptor.java`

方法：

```java
public class SecureConfigEncryptor {
    /** 从环境变量 NL2SQL_ENCRYPT_KEY 获取密钥 */
    public static String getKeyFromEnv();
    
    /** 生成新的 32 字节密钥并 Base64 编码 */
    public static String generateKey();
    
    /** 加密明文，返回 ENC(...) 格式 */
    public static String encrypt(String plaintext, String base64Key);
    
    /** 解密 ENC(...) 格式密文 */
    public static String decrypt(String encrypted, String base64Key);
}
```

### 5.4 自动解密 `EncryptedPropertyEnvironmentPostProcessor`

位置：`common/src/main/java/com/nl2sql/common/encrypt/EncryptedPropertyEnvironmentPostProcessor.java`

行为：

1. 在 `ApplicationEnvironmentPreparedEvent` 阶段执行。
2. 遍历 `ConfigurableEnvironment` 的所有 `PropertySource`。
3. 对 value 为字符串且匹配 `ENC\((.+)\)` 的属性，调用 `SecureConfigEncryptor.decrypt` 解密。
4. 将解密后的值放入一个新的 `PropertySource` 并置于最高优先级。
5. 若 `NL2SQL_ENCRYPT_KEY` 未设置且存在 `ENC(...)` 配置，启动失败（fail-fast）。

注册：`META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports`

```
com.nl2sql.common.encrypt.EncryptedPropertyEnvironmentPostProcessor
```

### 5.5 命令行加密工具

位置：`common/src/main/java/com/nl2sql/common/encrypt/EncryptTool.java`

独立 main 方法，用于生成密文：

```bash
export NL2SQL_ENCRYPT_KEY="$(java -cp common/target/classes com.nl2sql.common.encrypt.EncryptTool --generate-key)"
java -cp common/target/classes com.nl2sql.common.encrypt.EncryptTool "nl2sql123"
# 输出 ENC(xxx...)
```

---

## 6. 代码改动清单

### 6.1 Maven 依赖

- `schema-service/pom.xml`、`query-service/pom.xml`、`ai-service/pom.xml`：新增 `spring-cloud-starter-alibaba-nacos-config`。
- 根 `pom.xml`：确认 `spring-cloud-alibaba-dependencies` 已包含 Nacos Config 版本（已有）。

### 6.2 新增文件

| 文件 | 说明 |
|------|------|
| `common/src/main/java/com/nl2sql/common/encrypt/SecureConfigEncryptor.java` | AES-256-GCM 加解密 |
| `common/src/main/java/com/nl2sql/common/encrypt/EncryptedPropertyEnvironmentPostProcessor.java` | 自动解密 ENC(...) |
| `common/src/main/java/com/nl2sql/common/encrypt/EncryptTool.java` | 命令行加密工具 |
| `common/src/main/resources/META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports` | 注册 PostProcessor |
| `schema-service/src/main/resources/bootstrap.yml` | Nacos Config 引导 |
| `schema-service/src/main/resources/application-local.yml` | 本地开发 fallback |
| `query-service/src/main/resources/bootstrap.yml` | Nacos Config 引导 |
| `query-service/src/main/resources/application-local.yml` | 本地开发 fallback |
| `ai-service/src/main/resources/bootstrap.yml` | Nacos Config 引导 |
| `ai-service/src/main/resources/application-local.yml` | 本地开发 fallback |

### 6.3 修改文件

| 文件 | 说明 |
|------|------|
| `schema-service/src/main/resources/application.yml` | 清空或仅保留 spring.profiles.active |
| `query-service/src/main/resources/application.yml` | 同上 |
| `ai-service/src/main/resources/application.yml` | 同上 |

### 6.4 新增测试

| 测试 | 说明 |
|------|------|
| `common/src/test/java/com/nl2sql/common/encrypt/SecureConfigEncryptorTest.java` | 加解密正确性、IV 随机性、不同密钥失败 |
| `common/src/test/java/com/nl2sql/common/encrypt/EncryptedPropertyEnvironmentPostProcessorTest.java` | ENC 属性被正确解密 |

---

## 7. 实施与验证步骤

1. 创建新分支 `feat/nacos-config-encryption`。
2. 实现 `SecureConfigEncryptor` 与 `EncryptedPropertyEnvironmentPostProcessor`。
3. 修改 pom 依赖。
4. 新增 `bootstrap.yml` 与 `application-local.yml`。
5. 清空原 `application.yml`。
6. 生成本地测试用的 AES 密钥与密文。
7. 在本地 Nacos 创建 4 个配置文件并填入密文。
8. 启动 schema/query/ai 服务验证连通性。
9. 运行 `mvn test` 确认测试全绿。
10. 提交 PR 合并回 `main`。

---

## 8. 风险与应对

| 风险 | 应对 |
|------|------|
| Nacos 未启动导致服务起不来 | 本地用 `application-local.yml` fallback；生产必须配 Nacos |
| 密钥丢失无法解密 | 密钥由运维单独保管；Nacos 只存密文 |
| 测试因缺少 Nacos 失败 | 测试 profile 走 `application-local.yml`，不连 Nacos |
| GCM nonce 重复使用 | 每次加密随机生成 12 字节 IV |
| 密钥从环境变量泄露 | 不在日志打印；不提交到 git |

---

## 9. 参考

- [2026-06-18-infra-backlog.md](../plans/2026-06-18-infra-backlog.md)
- [Nacos Config Spring Cloud 文档](https://nacos.io/zh-cn/docs/quick-start-spring-cloud.html)
- [Spring Cloud Alibaba Nacos Config](https://sca.aliyun.com/zh-cn/docs/2023.0.1.0/user-guide/nacos/advanced-guide)
- [AES-GCM Java 实现参考](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/javax/crypto/Cipher.html)
