# Nacos Config 集中配置 + AES-256-GCM 加密实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 引入 Nacos Config 统一管理 schema/query/ai 服务配置，并用 AES-256-GCM 加密敏感字段；本地保留 `application-local.yml` fallback。

**Architecture:** 在 common 实现 `SecureConfigEncryptor` + `EncryptedPropertyEnvironmentPostProcessor` 自动解密 `ENC(...)`；各服务通过 `bootstrap.yml` 连接 Nacos 拉取配置；敏感字段密文存 Nacos，密钥通过 `NL2SQL_ENCRYPT_KEY` 环境变量注入。

**Tech Stack:** Java 17, Spring Boot 3.2.5, Spring Cloud Alibaba 2023, Nacos 2.4, AES-256-GCM.

## Global Constraints

- 本期**不对接外部 KMS/Vault**。
- 本期**不迁移 gateway-service 配置**。
- 本期**不改动业务代码行为**。
- 所有改动在 `feat/nacos-config-encryption` 分支开发，验证后 PR 合并回 `main`。
- 敏感字段必须使用 `ENC(...)` 密文格式存储。
- 解密密钥只通过环境变量 `NL2SQL_ENCRYPT_KEY` 注入，不落地代码仓库。
- 本地开发必须保留 `application-local.yml` 作为 fallback。
- `mvn test` 必须全绿，且测试不依赖 Nacos。

---

## Task 1: 创建功能分支

**Files:**
- 无文件变更

**Interfaces:**
- Produces: 干净的 `feat/nacos-config-encryption` 分支

- [ ] **Step 1: 从 main 切出新分支**

Run:
```bash
git checkout main
git pull origin main
git checkout -b feat/nacos-config-encryption
```

Expected: 当前分支为 `feat/nacos-config-encryption`，且与 `origin/main` 同步。

---

## Task 2: 实现 AES-256-GCM 加密工具类

**Files:**
- Create: `common/src/main/java/com/nl2sql/common/encrypt/SecureConfigEncryptor.java`

**Interfaces:**
- Produces: `SecureConfigEncryptor` 类，提供 `generateKey()` / `encrypt(String, String)` / `decrypt(String, String)` / `getKeyFromEnv()`

- [ ] **Step 1: 创建加密工具类**

```java
package com.nl2sql.common.encrypt;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 配置敏感字段 AES-256-GCM 加解密工具。
 * 密文格式：ENC(BASE64(iv || ciphertext || authTag))
 * 密钥来源：环境变量 NL2SQL_ENCRYPT_KEY（Base64 编码的 32 字节 AES 密钥）
 */
public final class SecureConfigEncryptor {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    private static final int AES_KEY_LENGTH = 32;

    private static final String ENV_KEY_NAME = "NL2SQL_ENCRYPT_KEY";
    private static final String PREFIX = "ENC(";
    private static final String SUFFIX = ")";

    private SecureConfigEncryptor() {
    }

    /** 从环境变量读取 Base64 编码的 AES 密钥 */
    public static String getKeyFromEnv() {
        String key = System.getenv(ENV_KEY_NAME);
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("环境变量 " + ENV_KEY_NAME + " 未设置，无法解密 ENC(...) 配置");
        }
        return key;
    }

    /** 生成新的 32 字节 AES 密钥并 Base64 编码 */
    public static String generateKey() {
        byte[] key = new byte[AES_KEY_LENGTH];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    /** 判断字符串是否为 ENC(...) 格式 */
    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX) && value.endsWith(SUFFIX);
    }

    /** 加密明文，返回 ENC(...) 格式 */
    public static String encrypt(String plaintext, String base64Key) {
        if (plaintext == null) {
            return null;
        }
        byte[] keyBytes = decodeKey(base64Key);
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            SecretKey secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            String encoded = Base64.getEncoder().encodeToString(byteBuffer.array());
            return PREFIX + encoded + SUFFIX;
        } catch (Exception e) {
            throw new IllegalStateException("AES-256-GCM 加密失败", e);
        }
    }

    /** 解密 ENC(...) 格式密文 */
    public static String decrypt(String encrypted, String base64Key) {
        if (encrypted == null) {
            return null;
        }
        if (!isEncrypted(encrypted)) {
            return encrypted;
        }

        String payload = encrypted.substring(PREFIX.length(), encrypted.length() - SUFFIX.length());
        byte[] decoded = Base64.getDecoder().decode(payload);

        if (decoded.length < GCM_IV_LENGTH + GCM_TAG_LENGTH) {
            throw new IllegalArgumentException("密文格式非法，长度不足");
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
        byte[] iv = new byte[GCM_IV_LENGTH];
        byteBuffer.get(iv);
        byte[] cipherText = new byte[byteBuffer.remaining()];
        byteBuffer.get(cipherText);

        byte[] keyBytes = decodeKey(base64Key);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            SecretKey secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("AES-256-GCM 解密失败，请检查 NL2SQL_ENCRYPT_KEY 是否正确", e);
        }
    }

    private static byte[] decodeKey(String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != AES_KEY_LENGTH) {
            throw new IllegalArgumentException("AES 密钥必须是 32 字节，当前 " + keyBytes.length + " 字节");
        }
        return keyBytes;
    }
}
```

- [ ] **Step 2: 编译 common**

Run: `export JAVA_HOME=/home/san/.jdks/ms-17.0.19 && mvn -q -pl common compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add common/src/main/java/com/nl2sql/common/encrypt/SecureConfigEncryptor.java
git commit -m "feat(common): AES-256-GCM 配置加密工具类"
```

---

## Task 3: 实现自动解密 EnvironmentPostProcessor

**Files:**
- Create: `common/src/main/java/com/nl2sql/common/encrypt/EncryptedPropertyEnvironmentPostProcessor.java`
- Create: `common/src/main/resources/META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports`

**Interfaces:**
- Consumes: `SecureConfigEncryptor`
- Produces: 启动时自动解密 `ENC(...)` 配置的 PostProcessor

- [ ] **Step 1: 创建 PostProcessor**

```java
package com.nl2sql.common.encrypt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 在 Environment 准备完成后扫描所有 PropertySource，
 * 将 ENC(...) 格式的密文解密为明文，并置于最高优先级。
 */
@Order(Ordered.LOWEST_PRECEDENCE)
public class EncryptedPropertyEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Pattern ENC_PATTERN = Pattern.compile("^ENC\\((.+)\\)$");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean hasEncryptedValue = false;
        Map<String, Object> decrypted = new HashMap<>();

        for (PropertySource<?> source : environment.getPropertySources()) {
            if (source instanceof MapPropertySource mapSource) {
                for (String name : mapSource.getPropertyNames()) {
                    Object value = mapSource.getProperty(name);
                    if (value instanceof String str && SecureConfigEncryptor.isEncrypted(str)) {
                        hasEncryptedValue = true;
                        Matcher matcher = ENC_PATTERN.matcher(str);
                        if (matcher.matches()) {
                            String key = SecureConfigEncryptor.getKeyFromEnv();
                            decrypted.put(name, SecureConfigEncryptor.decrypt(str, key));
                        }
                    }
                }
            }
        }

        if (hasEncryptedValue && decrypted.isEmpty()) {
            throw new IllegalStateException("存在 ENC(...) 密文配置但解密失败，请检查 NL2SQL_ENCRYPT_KEY 环境变量");
        }

        if (!decrypted.isEmpty()) {
            MapPropertySource decryptedSource = new MapPropertySource("decrypted-properties", decrypted);
            environment.getPropertySources().addFirst(decryptedSource);
        }
    }
}
```

- [ ] **Step 2: 注册 PostProcessor**

Create `common/src/main/resources/META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports`:

```
com.nl2sql.common.encrypt.EncryptedPropertyEnvironmentPostProcessor
```

- [ ] **Step 3: 编译 common**

Run: `export JAVA_HOME=/home/san/.jdks/ms-17.0.19 && mvn -q -pl common compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add common/src/main/java/com/nl2sql/common/encrypt/EncryptedPropertyEnvironmentPostProcessor.java \
    common/src/main/resources/META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports
git commit -m "feat(common): 启动时自动解密 ENC(...) 配置"
```

---

## Task 4: 实现命令行加密工具

**Files:**
- Create: `common/src/main/java/com/nl2sql/common/encrypt/EncryptTool.java`

**Interfaces:**
- Consumes: `SecureConfigEncryptor`
- Produces: 可独立运行的命令行加密/密钥生成工具

- [ ] **Step 1: 创建加密工具**

```java
package com.nl2sql.common.encrypt;

/**
 * 命令行工具，用于生成 AES 密钥或加密明文。
 * 用法：
 *   生成密钥：java EncryptTool --generate-key
 *   加密：    NL2SQL_ENCRYPT_KEY=xxx java EncryptTool "明文"
 */
public class EncryptTool {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("用法：");
            System.err.println("  生成密钥：java EncryptTool --generate-key");
            System.err.println("  加密：    NL2SQL_ENCRYPT_KEY=xxx java EncryptTool \"明文\"");
            System.exit(1);
        }

        if ("--generate-key".equals(args[0])) {
            System.out.println(SecureConfigEncryptor.generateKey());
            return;
        }

        String plaintext = args[0];
        String key = SecureConfigEncryptor.getKeyFromEnv();
        System.out.println(SecureConfigEncryptor.encrypt(plaintext, key));
    }
}
```

- [ ] **Step 2: 编译 common**

Run: `export JAVA_HOME=/home/san/.jdks/ms-17.0.19 && mvn -q -pl common compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: 验证工具可用**

Run:
```bash
export JAVA_HOME=/home/san/.jdks/ms-17.0.19
export NL2SQL_ENCRYPT_KEY=$(mvn -q -pl common exec:java -Dexec.mainClass="com.nl2sql.common.encrypt.EncryptTool" -Dexec.args="--generate-key")
echo "KEY=$NL2SQL_ENCRYPT_KEY"
mvn -q -pl common exec:java -Dexec.mainClass="com.nl2sql.common.encrypt.EncryptTool" -Dexec.args="nl2sql123"
```

Expected: 输出 `ENC(...)` 密文

- [ ] **Step 4: Commit**

```bash
git add common/src/main/java/com/nl2sql/common/encrypt/EncryptTool.java
git commit -m "feat(common): 命令行加密工具 EncryptTool"
```

---

## Task 5: common 加密模块单元测试

**Files:**
- Create: `common/src/test/java/com/nl2sql/common/encrypt/SecureConfigEncryptorTest.java`
- Create: `common/src/test/java/com/nl2sql/common/encrypt/EncryptedPropertyEnvironmentPostProcessorTest.java`

**Interfaces:**
- Consumes: `SecureConfigEncryptor`, `EncryptedPropertyEnvironmentPostProcessor`
- Produces: 2 个可运行的单元测试类

- [ ] **Step 1: 创建 SecureConfigEncryptorTest**

```java
package com.nl2sql.common.encrypt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SecureConfigEncryptor - AES-256-GCM 加解密")
class SecureConfigEncryptorTest {

    @Test
    @DisplayName("加解密应可逆")
    void shouldEncryptAndDecrypt() {
        String key = SecureConfigEncryptor.generateKey();
        String encrypted = SecureConfigEncryptor.encrypt("nl2sql123", key);

        assertThat(encrypted).startsWith("ENC(").endsWith(")");
        assertThat(SecureConfigEncryptor.decrypt(encrypted, key)).isEqualTo("nl2sql123");
    }

    @Test
    @DisplayName("相同明文两次加密结果应不同")
    void shouldUseRandomIv() {
        String key = SecureConfigEncryptor.generateKey();
        String encrypted1 = SecureConfigEncryptor.encrypt("nl2sql123", key);
        String encrypted2 = SecureConfigEncryptor.encrypt("nl2sql123", key);

        assertThat(encrypted1).isNotEqualTo(encrypted2);
    }

    @Test
    @DisplayName("错误密钥解密应失败")
    void shouldFailWithWrongKey() {
        String key = SecureConfigEncryptor.generateKey();
        String wrongKey = SecureConfigEncryptor.generateKey();
        String encrypted = SecureConfigEncryptor.encrypt("nl2sql123", key);

        assertThatThrownBy(() -> SecureConfigEncryptor.decrypt(encrypted, wrongKey))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("非 ENC 字符串应原样返回")
    void shouldReturnPlainValue() {
        assertThat(SecureConfigEncryptor.decrypt("plaintext", SecureConfigEncryptor.generateKey()))
                .isEqualTo("plaintext");
    }
}
```

- [ ] **Step 2: 创建 EncryptedPropertyEnvironmentPostProcessorTest**

```java
package com.nl2sql.common.encrypt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EncryptedPropertyEnvironmentPostProcessor - 自动解密")
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EncryptedPropertyEnvironmentPostProcessorTest.TestConfig.class)
@ActiveProfiles("test")
class EncryptedPropertyEnvironmentPostProcessorTest {

    @Configuration
    static class TestConfig {
    }

    @Test
    @DisplayName("application-test.yml 中的 ENC 配置应被解密")
    void shouldDecryptEncryptedProperty(ConfigurableEnvironment environment) {
        String value = environment.getProperty("nl2sql.test.encrypted-password");
        assertThat(value).isEqualTo("decrypted-value");
    }
}
```

- [ ] **Step 3: 在 common application-test.yml 中添加测试用 ENC 属性**

在 `common/src/test/resources/application-test.yml` 追加：

```yaml
nl2sql:
  test:
    encrypted-password: ENC(....)  # 使用测试密钥加密后的 "decrypted-value"
```

具体密文需要先用测试密钥生成。测试密钥可以硬编码在测试中（仅测试用）。

更简单的做法：在测试类中用 `TestPropertySource` 注入一个 ENC 值。

让我修正测试，不依赖 application-test.yml：

```java
@DisplayName("EncryptedPropertyEnvironmentPostProcessor - 自动解密")
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EncryptedPropertyEnvironmentPostProcessorTest.TestConfig.class)
@TestPropertySource(properties = {
    "nl2sql.test.encrypted-password=ENC(...)"
})
class EncryptedPropertyEnvironmentPostProcessorTest {
    ...
}
```

但 ENC 值需要事先用测试密钥生成。可以在 `@BeforeAll` 中生成并设置到 System property，但 `TestPropertySource` 在静态初始化时读取，时机不对。

更好的方法：使用 `ApplicationContextRunner` 测试 PostProcessor。

```java
class EncryptedPropertyEnvironmentPostProcessorTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withInitializer(new EncryptedPropertyEnvironmentPostProcessor()::postProcessEnvironment)
        .withPropertyValues("nl2sql.test.password=ENC(...)");

    @Test
    void shouldDecrypt() {
        runner.run(context -> {
            String value = context.getEnvironment().getProperty("nl2sql.test.password");
            assertThat(value).isEqualTo("nl2sql123");
        });
    }
}
```

但这需要 PostProcessor 在 ApplicationContextRunner 中正确执行。

最简单的方法：直接单元测试 PostProcessor，不启动 Spring。

```java
@Test
void shouldDecryptEncryptedProperties() {
    String key = SecureConfigEncryptor.generateKey();
    String encrypted = SecureConfigEncryptor.encrypt("nl2sql123", key);
    
    // 设置环境变量（仅在当前 JVM 进程）
    // 但无法修改真正环境变量，需要给 PostProcessor 增加一个可注入密钥的方式
}
```

由于 PostProcessor 内部调用 `SecureConfigEncryptor.getKeyFromEnv()`，无法直接注入密钥。我需要让 PostProcessor 支持从 System property 读取密钥作为 fallback，方便测试。

修改 PostProcessor：

```java
String key = System.getenv(ENV_KEY_NAME);
if (key == null || key.isBlank()) {
    key = System.getProperty(ENV_KEY_NAME);
}
if (key == null || key.isBlank()) {
    throw new IllegalStateException(...);
}
```

这样测试可以设置 `System.setProperty("NL2SQL_ENCRYPT_KEY", key)`。

让我调整设计和测试。

- [ ] **Step 2 (revised): 修改 PostProcessor 支持 System property fallback**

在 `EncryptedPropertyEnvironmentPostProcessor` 中，将 `SecureConfigEncryptor.getKeyFromEnv()` 替换为本地方法：

```java
private String resolveKey() {
    String key = System.getenv(SecureConfigEncryptor.ENV_KEY_NAME);
    if (key == null || key.isBlank()) {
        key = System.getProperty(SecureConfigEncryptor.ENV_KEY_NAME);
    }
    if (key == null || key.isBlank()) {
        throw new IllegalStateException("环境变量 NL2SQL_ENCRYPT_KEY 未设置，无法解密 ENC(...) 配置");
    }
    return key;
}
```

需要把 `SecureConfigEncryptor.ENV_KEY_NAME` 改为 package-private 或 public。

- [ ] **Step 3 (revised): 创建测试类**

```java
package com.nl2sql.common.encrypt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EncryptedPropertyEnvironmentPostProcessor - 自动解密")
class EncryptedPropertyEnvironmentPostProcessorTest {

    @Test
    @DisplayName("应将 ENC(...) 属性解密为明文")
    void shouldDecryptEncryptedProperty() {
        String key = SecureConfigEncryptor.generateKey();
        System.setProperty(SecureConfigEncryptor.ENV_KEY_NAME, key);

        String encrypted = SecureConfigEncryptor.encrypt("nl2sql123", key);

        MockEnvironment environment = new MockEnvironment()
                .withProperty("mysql.password", encrypted)
                .withProperty("mysql.host", "localhost");

        new EncryptedPropertyEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("mysql.password")).isEqualTo("nl2sql123");
        assertThat(environment.getProperty("mysql.host")).isEqualTo("localhost");

        System.clearProperty(SecureConfigEncryptor.ENV_KEY_NAME);
    }
}
```

需要确认 `spring-test` 包含 `MockEnvironment`。`spring-boot-starter-test` 传递了 spring-test，应该有。

- [ ] **Step 4: 运行 common 测试**

Run: `export JAVA_HOME=/home/san/.jdks/ms-17.0.19 && mvn -q -pl common test`
Expected: 新增测试通过

- [ ] **Step 5: Commit**

```bash
git add common/src/test/java/com/nl2sql/common/encrypt \
    common/src/main/java/com/nl2sql/common/encrypt/EncryptedPropertyEnvironmentPostProcessor.java
git commit -m "test(common): AES 加解密与自动解密 PostProcessor 测试"
```

---

## Task 6: 各服务引入 Nacos Config 依赖

**Files:**
- Modify: `schema-service/pom.xml`
- Modify: `query-service/pom.xml`
- Modify: `ai-service/pom.xml`

**Interfaces:**
- Produces: 三个服务具备 Nacos Config 能力

- [ ] **Step 1: 修改 schema-service pom**

在 `spring-cloud-starter-alibaba-nacos-discovery` 依赖后追加：

```xml
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
        </dependency>
```

- [ ] **Step 2: 修改 query-service pom**

同上。

- [ ] **Step 3: 修改 ai-service pom**

同上。

- [ ] **Step 4: 编译验证**

Run: `export JAVA_HOME=/home/san/.jdks/ms-17.0.19 && mvn -q -pl schema-service,query-service,ai-service -am test-compile`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add schema-service/pom.xml query-service/pom.xml ai-service/pom.xml
git commit -m "chore(config): 业务服务引入 Nacos Config 依赖"
```

---

## Task 7: 新增 bootstrap.yml 与 application-local.yml

**Files:**
- Create: `schema-service/src/main/resources/bootstrap.yml`
- Create: `schema-service/src/main/resources/application-local.yml`
- Create: `query-service/src/main/resources/bootstrap.yml`
- Create: `query-service/src/main/resources/application-local.yml`
- Create: `ai-service/src/main/resources/bootstrap.yml`
- Create: `ai-service/src/main/resources/application-local.yml`

**Interfaces:**
- Produces: 每个服务具备 Nacos 引导和本地 fallback 配置

- [ ] **Step 1: 创建 schema-service/bootstrap.yml**

```yaml
spring:
  application:
    name: nl2sql-schema-service
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

- [ ] **Step 2: 创建 schema-service/application-local.yml**

```yaml
spring:
  config:
    activate:
      on-profile: local

server:
  port: 8081

spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:3306/nl2sql_schema?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: nl2sql123
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: 6379
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER:localhost:8848}

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

注意：YAML 中同一个 `spring` 出现两次，合并即可。建议把 `spring.config.activate` 放在最上面，然后统一一个 `spring:` 块。

修正后的 schema-service/application-local.yml：

```yaml
spring:
  config:
    activate:
      on-profile: local
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:3306/nl2sql_schema?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: nl2sql123
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: 6379
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER:localhost:8848}

server:
  port: 8081

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

- [ ] **Step 3: 创建 query-service/bootstrap.yml**

```yaml
spring:
  application:
    name: nl2sql-query-service
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

- [ ] **Step 4: 创建 query-service/application-local.yml**

```yaml
spring:
  config:
    activate:
      on-profile: local
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:3306/nl2sql_query?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: nl2sql123
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: 5672
    username: ${RABBITMQ_USER:admin}
    password: admin
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: 6379
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER:localhost:8848}

server:
  port: 8082

nl2sql:
  cache:
    key-prefix: "nl2sql:"
    default-ttl: 10m
    null-ttl: 60s
    cache-null-values: true
    ttls:
      query:history: 2m
  mq:
    max-retries: 3
    initial-interval: 1000
    multiplier: 2.0
    max-interval: 10000

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

- [ ] **Step 5: 创建 ai-service/bootstrap.yml**

```yaml
spring:
  application:
    name: nl2sql-ai-service
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

- [ ] **Step 6: 创建 ai-service/application-local.yml**

```yaml
spring:
  config:
    activate:
      on-profile: local
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:3306/nl2sql_ai?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: nl2sql123
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: 5672
    username: ${RABBITMQ_USER:admin}
    password: admin
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER:localhost:8848}

server:
  port: 8083

nl2sql:
  mq:
    max-retries: 3
    initial-interval: 1000
    multiplier: 2.0
    max-interval: 10000

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

- [ ] **Step 7: 编译验证**

Run: `export JAVA_HOME=/home/san/.jdks/ms-17.0.19 && mvn -q -pl schema-service,query-service,ai-service -am test-compile`
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add schema-service/src/main/resources bootstrap.yml application-local.yml \
    query-service/src/main/resources/bootstrap.yml application-local.yml \
    ai-service/src/main/resources/bootstrap.yml application-local.yml
git commit -m "feat(config): 业务服务增加 bootstrap.yml 与本地 fallback"
```

---

## Task 8: 清空原 application.yml

**Files:**
- Modify: `schema-service/src/main/resources/application.yml`
- Modify: `query-service/src/main/resources/application.yml`
- Modify: `ai-service/src/main/resources/application.yml`

**Interfaces:**
- Produces: 原 application.yml 不再包含重复配置，避免与 Nacos 配置冲突

- [ ] **Step 1: 重写 schema-service/application.yml**

```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:}
```

- [ ] **Step 2: 重写 query-service/application.yml**

同上。

- [ ] **Step 3: 重写 ai-service/application.yml**

同上。

- [ ] **Step 4: 编译验证**

Run: `export JAVA_HOME=/home/san/.jdks/ms-17.0.19 && mvn -q clean compile`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add schema-service/src/main/resources/application.yml \
    query-service/src/main/resources/application.yml \
    ai-service/src/main/resources/application.yml
git commit -m "feat(config): 清空业务服务 application.yml，改由 Nacos 加载"
```

---

## Task 9: 生成密钥与密文

**Files:**
- 无代码文件变更
- 产出：环境变量 `NL2SQL_ENCRYPT_KEY` 和 Nacos 上需要填入的密文

**Interfaces:**
- Consumes: `EncryptTool`
- Produces: 可用于 Nacos 的 `ENC(...)` 密文

- [ ] **Step 1: 生成 AES 密钥**

Run:
```bash
export JAVA_HOME=/home/san/.jdks/ms-17.0.19
export NL2SQL_ENCRYPT_KEY=$(mvn -q -pl common exec:java -Dexec.mainClass="com.nl2sql.common.encrypt.EncryptTool" -Dexec.args="--generate-key")
echo "NL2SQL_ENCRYPT_KEY=$NL2SQL_ENCRYPT_KEY"
```

Expected: 输出 Base64 编码的 32 字节密钥，保存好。

- [ ] **Step 2: 生成 MySQL 密码密文**

Run:
```bash
mvn -q -pl common exec:java -Dexec.mainClass="com.nl2sql.common.encrypt.EncryptTool" -Dexec.args="nl2sql123"
```

Expected: 输出 `ENC(xxx...)`，复制备用。

- [ ] **Step 3: 生成 RabbitMQ 密码密文**

Run:
```bash
mvn -q -pl common exec:java -Dexec.mainClass="com.nl2sql.common.encrypt.EncryptTool" -Dexec.args="admin"
```

Expected: 输出 `ENC(xxx...)`，复制备用。

---

## Task 10: 在本地 Nacos 创建配置

**Files:**
- 无代码文件变更

**Interfaces:**
- Consumes: 上一步生成的密文
- Produces: Nacos 上的 4 个配置文件

- [ ] **Step 1: 确认 Nacos 已启动**

Run: `curl -s http://localhost:8848/nacos/v1/ns/operator/metrics | head -5`
Expected: 返回 Nacos 状态信息

- [ ] **Step 2: 创建 nl2sql-common.yml**

在 Nacos 控制台（默认 http://localhost:8848/nacos）创建配置：
- data-id: `nl2sql-common.yml`
- group: `DEFAULT_GROUP`
- 配置格式: `YAML`
- 内容：

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
    password: ENC(上一步生成的 admin 密文)
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

- [ ] **Step 3: 创建 nl2sql-schema-service.yml**

```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:3306/nl2sql_schema?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: ENC(上一步生成的 nl2sql123 密文)
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

- [ ] **Step 4: 创建 nl2sql-query-service.yml**

```yaml
server:
  port: 8082

spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:3306/nl2sql_query?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: ENC(上一步生成的 nl2sql123 密文)
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
      query:history: 2m
  mq:
    max-retries: 3
    initial-interval: 1000
    multiplier: 2.0
    max-interval: 10000
```

- [ ] **Step 5: 创建 nl2sql-ai-service.yml**

```yaml
server:
  port: 8083

spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:3306/nl2sql_ai?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: ENC(上一步生成的 nl2sql123 密文)
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

nl2sql:
  mq:
    max-retries: 3
    initial-interval: 1000
    multiplier: 2.0
    max-interval: 10000
```

---

## Task 11: 启动验证

**Files:**
- 无代码文件变更

**Interfaces:**
- Consumes: Nacos 上的配置文件 + `NL2SQL_ENCRYPT_KEY`
- Produces: 三个服务能正常启动并连接数据库/MQ

- [ ] **Step 1: 启动 schema-service**

Run:
```bash
export JAVA_HOME=/home/san/.jdks/ms-17.0.19
export NL2SQL_ENCRYPT_KEY=你的密钥
java -jar schema-service/target/schema-service-0.0.1-SNAPSHOT.jar
```

Expected: 启动成功，无数据库连接错误， actuator health 返回 UP。

- [ ] **Step 2: 启动 query-service 和 ai-service**

同上，分别使用对应 jar。

- [ ] **Step 3: 验证本地 fallback**

不设置 Nacos 或停止 Nacos，启动时加 `--spring.profiles.active=local`：

```bash
java -jar schema-service/target/schema-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

Expected: 服务使用 `application-local.yml` 启动，不依赖 Nacos。

---

## Task 12: 全项目测试验证

**Files:**
- 无新增文件

**Interfaces:**
- Consumes: 前面所有代码改动
- Produces: 测试通过结论

- [ ] **Step 1: 运行全项目测试**

Run: `export JAVA_HOME=/home/san/.jdks/ms-17.0.19 && mvn -q test`
Expected: BUILD SUCCESS，测试总数 >= 42

- [ ] **Step 2: 如失败，按模块单独排查**

Run: `mvn -pl <module> test` 逐个模块定位。
常见阻塞点：
- `MockEnvironment` 不存在 → 确认 `spring-test` 依赖已引入。
- `System.setProperty` 未生效 → 检查测试清理逻辑。
- Nacos Config 导致测试加载失败 → 确认测试 profile 使用 `application-local.yml` 或排除 Nacos Config 自动装配。

- [ ] **Step 3: Commit（如有修复）**

```bash
git add -A
git commit -m "fix(config): 测试修复与验证"
```

---

## Task 13: 创建 PR 合并回 main

**Files:**
- 无新增文件

**Interfaces:**
- Produces: PR 已创建并合并

- [ ] **Step 1: Push 功能分支**

Run:
```bash
git push -u origin feat/nacos-config-encryption
```

- [ ] **Step 2: 创建 PR**

使用 GitHub CLI：
```bash
gh pr create --title "feat(config): Nacos Config 集中配置与 AES-256-GCM 加密" \
  --body "引入 Nacos Config 统一管理业务服务配置，敏感字段使用 AES-256-GCM 加密存储。" \
  --base main
```

- [ ] **Step 3: 合并 PR**

根据 review 结果合并。合并后：
```bash
git checkout main
git pull origin main
```

---

## Self-Review Checklist

- [ ] Spec 覆盖：所有规格条目均能找到对应 Task。
- [ ] Placeholder 扫描：无 TBD/TODO/"实现 later"/"类似 Task N"。
- [ ] 类型一致性：`SecureConfigEncryptor` 方法签名在各 Task 中一致。
- [ ] 路径正确：所有文件路径均为项目内绝对路径。
- [ ] 命令可执行：所有 Maven 命令可在项目根目录运行。

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-06-21-nacos-config-encryption-plan.md`.

**Two execution options:**

1. **Subagent-Driven (recommended)** - Dispatch a fresh subagent per task, review between tasks, fast iteration.
2. **Inline Execution** - Execute tasks in this session using `superpowers:executing-plans`, batch execution with checkpoints.

Which approach?
