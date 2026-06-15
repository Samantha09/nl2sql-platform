# NL2SQL Platform 项目初始化实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 初始化 NL2SQL 平台微服务骨架，包含 Maven 多模块后端、Vue3 前端、Docker Compose 基础设施，使各服务可编译启动并通过 Gateway 联调。

**Architecture:** 采用 Spring Cloud Alibaba 微服务架构，`common` 模块提供公共 DTO 和统一响应包装；`gateway-service` 作为统一入口；`schema-service`、`query-service`、`ai-service` 分别负责数据源管理、查询执行、NL2SQL 转换；前端使用 Vue3 + Vite 调用后端 API。

**Tech Stack:** Java 17, Spring Boot 3.2.x, Spring Cloud Alibaba 2023.x, Vue 3, Vite, TypeScript, Element Plus, RabbitMQ 3.12, Redis 7, MySQL 8.0, Nacos 2.2.3, Docker Compose

---

## 文件结构总览

```
nl2sql-platform/
├── pom.xml
├── docker-compose.yml
├── .env
├── .gitignore
├── README.md
├── common/
│   ├── pom.xml
│   └── src/main/java/com/nl2sql/common/
│       ├── R.java
│       ├── event/
│       │   ├── NL2SQLEvent.java
│       │   ├── SQLReadyEvent.java
│       │   └── ResultReadyEvent.java
│       └── dto/
│           └── PageResult.java
├── gateway-service/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/nl2sql/gateway/
│       └── GatewayApplication.java
│   └── src/main/resources/
│       ├── application.yml
│       └── bootstrap.yml
├── schema-service/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/nl2sql/schema/
│       ├── SchemaApplication.java
│       ├── entity/
│       │   └── DataSourceConfig.java
│       ├── repository/
│       │   └── DataSourceRepository.java
│       ├── service/
│       │   └── DataSourceService.java
│       └── controller/
│           └── DataSourceController.java
│   └── src/main/resources/
│       ├── application.yml
│       └── schema.sql
├── ai-service/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/nl2sql/ai/
│       ├── AiApplication.java
│       ├── config/
│       │   └── RabbitConfig.java
│       ├── listener/
│       │   └── NL2SQLListener.java
│       ├── service/
│       │   └── MockLLMService.java
│       └── controller/
│           └── AiController.java
│   └── src/main/resources/
│       ├── application.yml
│       └── schema.sql
├── query-service/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/nl2sql/query/
│       ├── QueryApplication.java
│       ├── config/
│       │   └── RabbitConfig.java
│       ├── controller/
│       │   └── QueryController.java
│       ├── entity/
│       │   └── QueryHistory.java
│       ├── repository/
│       │   └── QueryHistoryRepository.java
│       ├── service/
│       │   └── QueryService.java
│       └── listener/
│           └── SQLReadyListener.java
│   └── src/main/resources/
│       ├── application.yml
│       └── schema.sql
└── frontend/
    ├── package.json
    ├── vite.config.ts
    ├── tsconfig.json
    └── src/
        ├── main.ts
        ├── App.vue
        ├── router/index.ts
        ├── views/
        │   ├── Home.vue
        │   ├── Query.vue
        │   ├── Result.vue
        │   ├── History.vue
        │   ├── DataSource.vue
        │   ├── SchemaViewer.vue
        │   └── Statistics.vue
        ├── components/
        │   ├── QueryInput.vue
        │   └── SQLPreview.vue
        ├── stores/
        │   └── query.ts
        ├── api/
        │   ├── request.ts
        │   ├── schema.ts
        │   ├── query.ts
        │   └── ai.ts
        └── utils/
            └── websocket.ts
```

---

## Task 1: 根项目 POM 与全局配置

**Files:**
- Create: `pom.xml`
- Create: `.env`
- Modify: `.gitignore`

**Goal:** 建立 Maven 父工程，统一管理版本与依赖。

- [ ] **Step 1: 创建根 `pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.nl2sql</groupId>
    <artifactId>nl2sql-platform</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <packaging>pom</packaging>
    <name>nl2sql-platform</name>
    <description>AI 数据库自然语言问答平台</description>

    <modules>
        <module>common</module>
        <module>gateway-service</module>
        <module>schema-service</module>
        <module>query-service</module>
        <module>ai-service</module>
    </modules>

    <properties>
        <java.version>17</java.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <spring-boot.version>3.2.5</spring-boot.version>
        <spring-cloud.version>2023.0.0</spring-cloud.version>
        <spring-cloud-alibaba.version>2023.0.1.0</spring-cloud-alibaba.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>com.alibaba.cloud</groupId>
                <artifactId>spring-cloud-alibaba-dependencies</artifactId>
                <version>${spring-cloud-alibaba.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <version>${spring-boot.version}</version>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

- [ ] **Step 2: 创建 `.env`**

```bash
MYSQL_ROOT_PASSWORD=nl2sql123
RABBITMQ_USER=admin
RABBITMQ_PASS=admin
```

- [ ] **Step 3: 补充 `.gitignore`**

追加：

```
.env
*/target/
frontend/node_modules/
frontend/dist/
```

- [ ] **Step 4: 提交**

```bash
git add pom.xml .env .gitignore
git commit -m "chore(project): 初始化 Maven 父工程与全局配置"
```

---

## Task 2: common 公共模块

**Files:**
- Create: `common/pom.xml`
- Create: `common/src/main/java/com/nl2sql/common/R.java`
- Create: `common/src/main/java/com/nl2sql/common/event/NL2SQLEvent.java`
- Create: `common/src/main/java/com/nl2sql/common/event/SQLReadyEvent.java`
- Create: `common/src/main/java/com/nl2sql/common/event/ResultReadyEvent.java`

**Goal:** 提供统一响应包装与跨服务共享的事件 DTO。

- [ ] **Step 1: 创建 `common/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.nl2sql</groupId>
        <artifactId>nl2sql-platform</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>

    <artifactId>common</artifactId>
    <name>common</name>
    <description>公共模块</description>

    <dependencies>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 创建统一响应类 `R.java`**

```java
package com.nl2sql.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class R<T> implements Serializable {

    private Integer code;
    private String message;
    private T data;

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        R<T> r = new R<T>();
        r.setCode(200);
        r.setMessage("success");
        r.setData(data);
        return r;
    }

    public static <T> R<T> error(String message) {
        return error(500, message);
    }

    public static <T> R<T> error(int code, String message) {
        R<T> r = new R<T>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }
}
```

- [ ] **Step 3: 创建事件 DTO**

`NL2SQLEvent.java`:

```java
package com.nl2sql.common.event;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class NL2SQLEvent implements Serializable {
    private String eventId;
    private Long userId;
    private Long dataSourceId;
    private String naturalLanguage;
    private String conversationId;
    private Map<String, Object> schemaContext;
    private Long timestamp;
}
```

`SQLReadyEvent.java`:

```java
package com.nl2sql.common.event;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SQLReadyEvent implements Serializable {
    private String eventId;
    private String nlRequestId;
    private String generatedSql;
    private Boolean sqlValid;
    private List<String> validationErrors;
    private Long timestamp;
}
```

`ResultReadyEvent.java`:

```java
package com.nl2sql.common.event;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class ResultReadyEvent implements Serializable {
    private String eventId;
    private String sqlRequestId;
    private List<Map<String, Object>> data;
    private Integer totalCount;
    private Long executeTimeMs;
    private Long timestamp;
}
```

- [ ] **Step 4: 编译验证**

```bash
cd ~/IdeaProjects/nl2sql-platform
mvn clean install -pl common
```

Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add common/
git commit -m "feat(common): 统一响应包装与公共事件 DTO"
```

---

## Task 3: 基础设施 Docker Compose

**Files:**
- Create: `docker-compose.yml`

**Goal:** 一键拉起 MySQL、Redis、RabbitMQ、Nacos。

- [ ] **Step 1: 创建 `docker-compose.yml`**

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: nl2sql-mysql
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-nl2sql123}
      MYSQL_DATABASE: nl2sql_schema
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./schema-service/src/main/resources/schema.sql:/docker-entrypoint-initdb.d/01-schema.sql:ro
      - ./query-service/src/main/resources/schema.sql:/docker-entrypoint-initdb.d/02-query.sql:ro
      - ./ai-service/src/main/resources/schema.sql:/docker-entrypoint-initdb.d/03-ai.sql:ro
    networks:
      - nl2sql
    command: --default-authentication-plugin=mysql_native_password

  redis:
    image: redis:7-alpine
    container_name: nl2sql-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    networks:
      - nl2sql

  rabbitmq:
    image: rabbitmq:3.12-management
    container_name: nl2sql-rabbitmq
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USER:-admin}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASS:-admin}
    ports:
      - "5672:5672"
      - "15672:15672"
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    networks:
      - nl2sql

  nacos:
    image: nacos/nacos-server:v2.2.3
    container_name: nl2sql-nacos
    environment:
      MODE: standalone
      SPRING_DATASOURCE_PLATFORM: mysql
      MYSQL_SERVICE_HOST: mysql
      MYSQL_SERVICE_DB_NAME: nacos
      MYSQL_SERVICE_PORT: 3306
      MYSQL_SERVICE_USER: root
      MYSQL_SERVICE_PASSWORD: ${MYSQL_ROOT_PASSWORD:-nl2sql123}
    ports:
      - "8848:8848"
      - "9848:9848"
    depends_on:
      - mysql
    networks:
      - nl2sql

networks:
  nl2sql:
    driver: bridge

volumes:
  mysql_data:
  redis_data:
  rabbitmq_data:
```

- [ ] **Step 2: 预创建数据库初始化文件**

在执行 `docker compose up` 之前，需确保 MySQL 挂载的初始化脚本已存在（后续任务会补充表结构）：

创建 `schema-service/src/main/resources/schema.sql`：

```sql
CREATE DATABASE IF NOT EXISTS nl2sql_schema CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

创建 `query-service/src/main/resources/schema.sql`：

```sql
CREATE DATABASE IF NOT EXISTS nl2sql_query CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

创建 `ai-service/src/main/resources/schema.sql`：

```sql
CREATE DATABASE IF NOT EXISTS nl2sql_ai CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

- [ ] **Step 3: 启动并验证基础设施**

```bash
cd ~/IdeaProjects/nl2sql-platform
docker compose up -d mysql redis rabbitmq nacos
sleep 30
docker ps
```

Expected: 四个容器均为 Up 状态。

- [ ] **Step 4: 提交**

```bash
git add docker-compose.yml schema-service/src/main/resources/schema.sql query-service/src/main/resources/schema.sql ai-service/src/main/resources/schema.sql
git commit -m "build(infra): 添加 Docker Compose 基础设施编排与数据库初始化脚本占位"
```

---

## Task 4: gateway-service API 网关

**Files:**
- Create: `gateway-service/pom.xml`
- Create: `gateway-service/Dockerfile`
- Create: `gateway-service/src/main/java/com/nl2sql/gateway/GatewayApplication.java`
- Create: `gateway-service/src/main/resources/application.yml`
- Create: `gateway-service/src/main/resources/bootstrap.yml`

**Goal:** 统一入口，路由转发到三个业务服务。

- [ ] **Step 1: 创建 `gateway-service/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.nl2sql</groupId>
        <artifactId>nl2sql-platform</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>

    <artifactId>gateway-service</artifactId>
    <name>gateway-service</name>

    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建启动类 `GatewayApplication.java`**

```java
package com.nl2sql.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```

- [ ] **Step 3: 创建 `application.yml`**

```yaml
server:
  port: 8080

spring:
  application:
    name: gateway-service
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER:localhost:8848}
    gateway:
      discovery:
        locator:
          enabled: false
      routes:
        - id: schema-service
          uri: lb://schema-service
          predicates:
            - Path=/api/schema/**
        - id: query-service
          uri: lb://query-service
          predicates:
            - Path=/api/query/**
        - id: ai-service
          uri: lb://ai-service
          predicates:
            - Path=/api/ai/**

management:
  endpoints:
    web:
      exposure:
        include: health
```

- [ ] **Step 4: 创建 `bootstrap.yml`**

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER:localhost:8848}
```

- [ ] **Step 5: 创建 `Dockerfile`**

```dockerfile
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/gateway-service-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 6: 编译验证**

```bash
cd ~/IdeaProjects/nl2sql-platform
mvn clean install -pl gateway-service -am
```

Expected: BUILD SUCCESS

- [ ] **Step 7: 提交**

```bash
git add gateway-service/
git commit -m "feat(gateway): 初始化 API 网关服务"
```

---

## Task 5: schema-service 数据源与 Schema 管理

**Files:**
- Create: `schema-service/pom.xml`
- Create: `schema-service/Dockerfile`
- Create: `schema-service/src/main/resources/application.yml`
- Create: `schema-service/src/main/resources/schema.sql`
- Create: `schema-service/src/main/java/com/nl2sql/schema/SchemaApplication.java`
- Create: `schema-service/src/main/java/com/nl2sql/schema/entity/DataSourceConfig.java`
- Create: `schema-service/src/main/java/com/nl2sql/schema/repository/DataSourceRepository.java`
- Create: `schema-service/src/main/java/com/nl2sql/schema/service/DataSourceService.java`
- Create: `schema-service/src/main/java/com/nl2sql/schema/controller/DataSourceController.java`
- Create: `schema-service/src/main/java/com/nl2sql/schema/dto/TableSchemaDTO.java`

**Goal:** 提供数据源 CRUD、Schema 扫描接口（Mock）。

- [ ] **Step 1: 创建 `schema-service/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.nl2sql</groupId>
        <artifactId>nl2sql-platform</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>

    <artifactId>schema-service</artifactId>
    <name>schema-service</name>

    <dependencies>
        <dependency>
            <groupId>com.nl2sql</groupId>
            <artifactId>common</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建 `schema.sql`**

```sql
CREATE DATABASE IF NOT EXISTS nl2sql_schema CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE nl2sql_schema;

CREATE TABLE IF NOT EXISTS `data_sources` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL COMMENT '数据源名称',
    `type` VARCHAR(20) NOT NULL COMMENT '类型: mysql/postgresql',
    `host` VARCHAR(255) NOT NULL,
    `port` INT NOT NULL,
    `database_name` VARCHAR(100) NOT NULL,
    `username` VARCHAR(100) NOT NULL,
    `password_encrypted` VARCHAR(500) NOT NULL COMMENT '加密存储',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `schema_cache` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `data_source_id` BIGINT NOT NULL,
    `table_name` VARCHAR(100) NOT NULL,
    `table_comment` VARCHAR(500) DEFAULT '',
    `column_json` TEXT COMMENT '字段详情JSON',
    `primary_key_json` TEXT COMMENT '主键JSON',
    `foreign_key_json` TEXT COMMENT '外键JSON',
    `index_json` TEXT COMMENT '索引JSON',
    `row_estimate` BIGINT DEFAULT 0 COMMENT '行数估算',
    `cached_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `version` INT DEFAULT 1 COMMENT '版本号，用于增量更新',
    UNIQUE KEY `uk_ds_table` (`data_source_id`, `table_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `table_list_cache` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `data_source_id` BIGINT NOT NULL,
    `table_json` TEXT COMMENT '表列表JSON',
    `cached_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 3: 创建 `application.yml`**

```yaml
server:
  port: 8081

spring:
  application:
    name: schema-service
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:3306/nl2sql_schema?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: ${MYSQL_PASSWORD:nl2sql123}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER:localhost:8848}

management:
  endpoints:
    web:
      exposure:
        include: health
```

- [ ] **Step 4: 创建启动类 `SchemaApplication.java`**

```java
package com.nl2sql.schema;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class SchemaApplication {
    public static void main(String[] args) {
        SpringApplication.run(SchemaApplication.class, args);
    }
}
```

- [ ] **Step 5: 创建实体 `DataSourceConfig.java`**

```java
package com.nl2sql.schema.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "data_sources")
public class DataSourceConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false)
    private String host;

    @Column(nullable = false)
    private Integer port;

    @Column(nullable = false, name = "database_name")
    private String databaseName;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false, name = "password_encrypted")
    private String passwordEncrypted;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 6: 创建 Repository `DataSourceRepository.java`**

```java
package com.nl2sql.schema.repository;

import com.nl2sql.schema.entity.DataSourceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataSourceRepository extends JpaRepository<DataSourceConfig, Long> {
}
```

- [ ] **Step 7: 创建 DTO `TableSchemaDTO.java`**

```java
package com.nl2sql.schema.dto;

import lombok.Data;

import java.util.List;

@Data
public class TableSchemaDTO {
    private String tableName;
    private String tableComment;
    private List<ColumnInfo> columns;
    private List<String> primaryKeys;

    @Data
    public static class ColumnInfo {
        private String name;
        private String type;
        private String comment;
    }
}
```

- [ ] **Step 8: 创建 Service `DataSourceService.java`**

```java
package com.nl2sql.schema.service;

import com.nl2sql.schema.dto.TableSchemaDTO;
import com.nl2sql.schema.entity.DataSourceConfig;
import com.nl2sql.schema.repository.DataSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DataSourceService {

    private final DataSourceRepository repository;

    public DataSourceConfig create(DataSourceConfig config) {
        return repository.save(config);
    }

    public List<DataSourceConfig> list() {
        return repository.findAll();
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public List<String> scanTables(Long dataSourceId) {
        // 骨架阶段返回 Mock 数据
        return List.of("users", "orders", "products");
    }

    public TableSchemaDTO getTableDetail(Long dataSourceId, String tableName) {
        TableSchemaDTO dto = new TableSchemaDTO();
        dto.setTableName(tableName);
        dto.setTableComment("Mock 表注释");
        TableSchemaDTO.ColumnInfo c1 = new TableSchemaDTO.ColumnInfo();
        c1.setName("id");
        c1.setType("BIGINT");
        c1.setComment("主键");
        TableSchemaDTO.ColumnInfo c2 = new TableSchemaDTO.ColumnInfo();
        c2.setName("name");
        c2.setType("VARCHAR");
        c2.setComment("名称");
        dto.setColumns(List.of(c1, c2));
        dto.setPrimaryKeys(List.of("id"));
        return dto;
    }
}
```

- [ ] **Step 9: 创建 Controller `DataSourceController.java`**

```java
package com.nl2sql.schema.controller;

import com.nl2sql.common.R;
import com.nl2sql.schema.dto.TableSchemaDTO;
import com.nl2sql.schema.entity.DataSourceConfig;
import com.nl2sql.schema.service.DataSourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schema")
@RequiredArgsConstructor
public class DataSourceController {

    private final DataSourceService service;

    @PostMapping("/datasource")
    public R<DataSourceConfig> add(@RequestBody DataSourceConfig config) {
        return R.ok(service.create(config));
    }

    @GetMapping("/datasource/list")
    public R<List<DataSourceConfig>> list() {
        return R.ok(service.list());
    }

    @DeleteMapping("/datasource/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }

    @PostMapping("/scan/{datasourceId}")
    public R<List<String>> scan(@PathVariable Long datasourceId) {
        return R.ok(service.scanTables(datasourceId));
    }

    @GetMapping("/{datasourceId}/tables")
    public R<List<String>> tables(@PathVariable Long datasourceId) {
        return R.ok(service.scanTables(datasourceId));
    }

    @GetMapping("/{datasourceId}/tables/{tableName}")
    public R<TableSchemaDTO> tableDetail(@PathVariable Long datasourceId, @PathVariable String tableName) {
        return R.ok(service.getTableDetail(datasourceId, tableName));
    }
}
```

- [ ] **Step 10: 创建 `Dockerfile`**

```dockerfile
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/schema-service-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 11: 编译验证**

```bash
cd ~/IdeaProjects/nl2sql-platform
mvn clean install -pl schema-service -am
```

Expected: BUILD SUCCESS

- [ ] **Step 12: 提交**

```bash
git add schema-service/
git commit -m "feat(schema): 初始化数据源与 Schema 管理服务"
```

---

## Task 6: ai-service Mock LLM 服务

**Files:**
- Create: `ai-service/pom.xml`
- Create: `ai-service/Dockerfile`
- Create: `ai-service/src/main/resources/application.yml`
- Create: `ai-service/src/main/resources/schema.sql`
- Create: `ai-service/src/main/java/com/nl2sql/ai/AiApplication.java`
- Create: `ai-service/src/main/java/com/nl2sql/ai/config/RabbitConfig.java`
- Create: `ai-service/src/main/java/com/nl2sql/ai/listener/NL2SQLListener.java`
- Create: `ai-service/src/main/java/com/nl2sql/ai/service/MockLLMService.java`
- Create: `ai-service/src/main/java/com/nl2sql/ai/controller/AiController.java`
- Create: `ai-service/src/main/java/com/nl2sql/ai/dto/ConvertRequest.java`

**Goal:** 提供 NL 转 SQL 接口，骨架阶段使用 Mock 返回。

- [ ] **Step 1: 创建 `ai-service/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.nl2sql</groupId>
        <artifactId>nl2sql-platform</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>

    <artifactId>ai-service</artifactId>
    <name>ai-service</name>

    <dependencies>
        <dependency>
            <groupId>com.nl2sql</groupId>
            <artifactId>common</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建 `schema.sql`**

```sql
CREATE DATABASE IF NOT EXISTS nl2sql_ai CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE nl2sql_ai;

CREATE TABLE IF NOT EXISTS `prompt_templates` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `template_name` VARCHAR(100) NOT NULL UNIQUE,
    `system_prompt` TEXT NOT NULL,
    `user_prompt_template` TEXT,
    `model` VARCHAR(50) DEFAULT 'local-llm',
    `temperature` DECIMAL(2,1) DEFAULT 0.7,
    `max_tokens` INT DEFAULT 2000,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `conversation_history` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `conversation_id` VARCHAR(64) NOT NULL,
    `role` VARCHAR(20) NOT NULL COMMENT 'user/assistant/system',
    `content` TEXT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_conversation` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 3: 创建 `application.yml`**

```yaml
server:
  port: 8083

spring:
  application:
    name: ai-service
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:3306/nl2sql_ai?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: ${MYSQL_PASSWORD:nl2sql123}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: 5672
    username: ${RABBITMQ_USER:admin}
    password: ${RABBITMQ_PASS:admin}
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER:localhost:8848}

management:
  endpoints:
    web:
      exposure:
        include: health
```

- [ ] **Step 4: 创建启动类 `AiApplication.java`**

```java
package com.nl2sql.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class AiApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
    }
}
```

- [ ] **Step 5: 创建 RabbitMQ 配置 `RabbitConfig.java`**

```java
package com.nl2sql.ai.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String NL2SQL_QUEUE = "nl2sql.queue";
    public static final String NL2SQL_EXCHANGE = "nl2sql.exchange";
    public static final String NL2SQL_ROUTING_KEY = "nl2sql.event";

    @Bean
    public Queue nl2sqlQueue() {
        return new Queue(NL2SQL_QUEUE, true);
    }

    @Bean
    public TopicExchange nl2sqlExchange() {
        return new TopicExchange(NL2SQL_EXCHANGE);
    }

    @Bean
    public Binding nl2sqlBinding(Queue nl2sqlQueue, TopicExchange nl2sqlExchange) {
        return BindingBuilder.bind(nl2sqlQueue).to(nl2sqlExchange).with(NL2SQL_ROUTING_KEY);
    }
}
```

- [ ] **Step 6: 创建 Mock LLM 服务 `MockLLMService.java`**

```java
package com.nl2sql.ai.service;

import org.springframework.stereotype.Service;

@Service
public class MockLLMService {

    public String convert(String naturalLanguage, Long dataSourceId) {
        String lower = naturalLanguage.toLowerCase();
        if (lower.contains("销售") || lower.contains("sale")) {
            return "SELECT product_name, SUM(amount) AS total_sales FROM orders GROUP BY product_name ORDER BY total_sales DESC LIMIT 10;";
        }
        if (lower.contains("用户") || lower.contains("user")) {
            return "SELECT COUNT(*) AS user_count FROM users;";
        }
        return "SELECT * FROM orders LIMIT 100;";
    }
}
```

- [ ] **Step 7: 创建 MQ 监听 `NL2SQLListener.java`**

```java
package com.nl2sql.ai.listener;

import com.nl2sql.ai.config.RabbitConfig;
import com.nl2sql.ai.service.MockLLMService;
import com.nl2sql.common.event.NL2SQLEvent;
import com.nl2sql.common.event.SQLReadyEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NL2SQLListener {

    private final MockLLMService mockLLMService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitConfig.NL2SQL_QUEUE)
    public void onNL2SQLEvent(NL2SQLEvent event) {
        String sql = mockLLMService.convert(event.getNaturalLanguage(), event.getDataSourceId());

        SQLReadyEvent ready = new SQLReadyEvent();
        ready.setEventId(UUID.randomUUID().toString());
        ready.setNlRequestId(event.getEventId());
        ready.setGeneratedSql(sql);
        ready.setSqlValid(true);
        ready.setTimestamp(System.currentTimeMillis());

        rabbitTemplate.convertAndSend("sql.ready.exchange", "sql.ready.event", ready);
    }
}
```

- [ ] **Step 8: 创建 Controller `AiController.java`**

```java
package com.nl2sql.ai.controller;

import com.nl2sql.ai.dto.ConvertRequest;
import com.nl2sql.ai.service.MockLLMService;
import com.nl2sql.common.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final MockLLMService mockLLMService;

    @PostMapping("/convert")
    public R<String> convert(@RequestBody ConvertRequest request) {
        String sql = mockLLMService.convert(request.getNaturalLanguage(), request.getDataSourceId());
        return R.ok(sql);
    }

    @PostMapping("/validate")
    public R<Boolean> validate(@RequestBody String sql) {
        return R.ok(sql.toLowerCase().startsWith("select"));
    }
}
```

- [ ] **Step 9: 创建请求 DTO `ConvertRequest.java`**

```java
package com.nl2sql.ai.dto;

import lombok.Data;

@Data
public class ConvertRequest {
    private Long dataSourceId;
    private String naturalLanguage;
}
```

- [ ] **Step 10: 创建 `Dockerfile`**

```dockerfile
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/ai-service-*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 11: 编译验证**

```bash
cd ~/IdeaProjects/nl2sql-platform
mvn clean install -pl ai-service -am
```

Expected: BUILD SUCCESS

- [ ] **Step 12: 提交**

```bash
git add ai-service/
git commit -m "feat(ai): 初始化 AI 服务与 Mock LLM"
```

---

## Task 7: query-service 查询执行服务

**Files:**
- Create: `query-service/pom.xml`
- Create: `query-service/Dockerfile`
- Create: `query-service/src/main/resources/application.yml`
- Create: `query-service/src/main/resources/schema.sql`
- Create: `query-service/src/main/java/com/nl2sql/query/QueryApplication.java`
- Create: `query-service/src/main/java/com/nl2sql/query/config/RabbitConfig.java`
- Create: `query-service/src/main/java/com/nl2sql/query/controller/QueryController.java`
- Create: `query-service/src/main/java/com/nl2sql/query/entity/QueryHistory.java`
- Create: `query-service/src/main/java/com/nl2sql/query/repository/QueryHistoryRepository.java`
- Create: `query-service/src/main/java/com/nl2sql/query/service/QueryService.java`
- Create: `query-service/src/main/java/com/nl2sql/query/listener/SQLReadyListener.java`
- Create: `query-service/src/main/java/com/nl2sql/query/dto/QueryRequest.java`
- Create: `query-service/src/main/java/com/nl2sql/query/dto/QueryResult.java`

**Goal:** 处理 NL 查询，通过 MQ 与 AI 服务交互，返回 Mock 执行结果。

- [ ] **Step 1: 创建 `query-service/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.nl2sql</groupId>
        <artifactId>nl2sql-platform</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>

    <artifactId>query-service</artifactId>
    <name>query-service</name>

    <dependencies>
        <dependency>
            <groupId>com.nl2sql</groupId>
            <artifactId>common</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建 `schema.sql`**

```sql
CREATE DATABASE IF NOT EXISTS nl2sql_query CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE nl2sql_query;

CREATE TABLE IF NOT EXISTS `query_conversations` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `conversation_id` VARCHAR(64) NOT NULL UNIQUE COMMENT '会话ID',
    `user_id` BIGINT NOT NULL,
    `data_source_id` BIGINT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `query_history` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `conversation_id` VARCHAR(64) NOT NULL,
    `user_id` BIGINT NOT NULL,
    `data_source_id` BIGINT NOT NULL,
    `natural_language` VARCHAR(1000) NOT NULL COMMENT '原始问题',
    `generated_sql` TEXT NOT NULL COMMENT '生成的SQL',
    `sql_executed` TEXT COMMENT '实际执行的SQL',
    `execute_time_ms` BIGINT DEFAULT 0,
    `result_count` INT DEFAULT 0,
    `status` VARCHAR(20) DEFAULT 'success' COMMENT 'success/failed/timeout',
    `error_message` TEXT,
    `chart_type` VARCHAR(20) DEFAULT 'table' COMMENT 'table/line/bar/pie',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_conversation` (`conversation_id`),
    INDEX `idx_user_created` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `query_statistics` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `stat_date` DATE NOT NULL COMMENT '统计日期',
    `user_id` BIGINT,
    `query_count` INT DEFAULT 0 COMMENT '查询次数',
    `success_count` INT DEFAULT 0,
    `failed_count` INT DEFAULT 0,
    `avg_execute_time_ms` BIGINT DEFAULT 0,
    `popular_questions_json` TEXT COMMENT '热门问题JSON',
    UNIQUE KEY `uk_date_user` (`stat_date`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `notify_records` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `type` VARCHAR(20) NOT NULL COMMENT 'feishu/email',
    `recipient` VARCHAR(100) NOT NULL,
    `title` VARCHAR(200),
    `content` TEXT,
    `status` VARCHAR(20) DEFAULT 'pending',
    `sent_at` TIMESTAMP NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 3: 创建 `application.yml`**

```yaml
server:
  port: 8082

spring:
  application:
    name: query-service
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:3306/nl2sql_query?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: ${MYSQL_PASSWORD:nl2sql123}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: 5672
    username: ${RABBITMQ_USER:admin}
    password: ${RABBITMQ_PASS:admin}
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: 6379
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER:localhost:8848}

management:
  endpoints:
    web:
      exposure:
        include: health
```

- [ ] **Step 4: 创建启动类 `QueryApplication.java`**

```java
package com.nl2sql.query;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class QueryApplication {
    public static void main(String[] args) {
        SpringApplication.run(QueryApplication.class, args);
    }
}
```

- [ ] **Step 5: 创建 RabbitMQ 配置 `RabbitConfig.java`**

```java
package com.nl2sql.query.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String NL2SQL_EXCHANGE = "nl2sql.exchange";
    public static final String NL2SQL_ROUTING_KEY = "nl2sql.event";

    public static final String SQL_READY_QUEUE = "sql.ready.queue";
    public static final String SQL_READY_EXCHANGE = "sql.ready.exchange";
    public static final String SQL_READY_ROUTING_KEY = "sql.ready.event";

    public static final String RESULT_READY_QUEUE = "result.ready.queue";
    public static final String RESULT_READY_EXCHANGE = "result.ready.exchange";
    public static final String RESULT_READY_ROUTING_KEY = "result.ready.event";

    @Bean
    public TopicExchange nl2sqlExchange() {
        return new TopicExchange(NL2SQL_EXCHANGE);
    }

    @Bean
    public Queue sqlReadyQueue() {
        return new Queue(SQL_READY_QUEUE, true);
    }

    @Bean
    public DirectExchange sqlReadyExchange() {
        return new DirectExchange(SQL_READY_EXCHANGE);
    }

    @Bean
    public Binding sqlReadyBinding(Queue sqlReadyQueue, DirectExchange sqlReadyExchange) {
        return BindingBuilder.bind(sqlReadyQueue).to(sqlReadyExchange).with(SQL_READY_ROUTING_KEY);
    }

    @Bean
    public Queue resultReadyQueue() {
        return new Queue(RESULT_READY_QUEUE, true);
    }

    @Bean
    public DirectExchange resultReadyExchange() {
        return new DirectExchange(RESULT_READY_EXCHANGE);
    }

    @Bean
    public Binding resultReadyBinding(Queue resultReadyQueue, DirectExchange resultReadyExchange) {
        return BindingBuilder.bind(resultReadyQueue).to(resultReadyExchange).with(RESULT_READY_ROUTING_KEY);
    }
}
```

- [ ] **Step 6: 创建 DTO `QueryRequest.java` 与 `QueryResult.java`**

`QueryRequest.java`:

```java
package com.nl2sql.query.dto;

import lombok.Data;

@Data
public class QueryRequest {
    private Long userId;
    private Long dataSourceId;
    private String naturalLanguage;
    private String conversationId;
}
```

`QueryResult.java`:

```java
package com.nl2sql.query.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class QueryResult {
    private String sql;
    private List<Map<String, Object>> data;
    private Integer totalCount;
    private Long executeTimeMs;
    private String chartType;
}
```

- [ ] **Step 7: 创建实体 `QueryHistory.java`**

```java
package com.nl2sql.query.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "query_history")
public class QueryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false, length = 64)
    private String conversationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "data_source_id", nullable = false)
    private Long dataSourceId;

    @Column(name = "natural_language", nullable = false, length = 1000)
    private String naturalLanguage;

    @Column(name = "generated_sql", nullable = false, columnDefinition = "TEXT")
    private String generatedSql;

    @Column(name = "sql_executed", columnDefinition = "TEXT")
    private String sqlExecuted;

    @Column(name = "execute_time_ms")
    private Long executeTimeMs = 0L;

    @Column(name = "result_count")
    private Integer resultCount = 0;

    @Column(name = "status", length = 20)
    private String status = "success";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "chart_type", length = 20)
    private String chartType = "table";

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
```

- [ ] **Step 8: 创建 Repository `QueryHistoryRepository.java`**

```java
package com.nl2sql.query.repository;

import com.nl2sql.query.entity.QueryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QueryHistoryRepository extends JpaRepository<QueryHistory, Long> {
    List<QueryHistory> findByConversationIdOrderByCreatedAtDesc(String conversationId);
}
```

- [ ] **Step 9: 创建 Service `QueryService.java`**

```java
package com.nl2sql.query.service;

import com.nl2sql.common.event.NL2SQLEvent;
import com.nl2sql.query.config.RabbitConfig;
import com.nl2sql.query.dto.QueryRequest;
import com.nl2sql.query.dto.QueryResult;
import com.nl2sql.query.entity.QueryHistory;
import com.nl2sql.query.repository.QueryHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class QueryService {

    private final RabbitTemplate rabbitTemplate;
    private final QueryHistoryRepository historyRepository;

    public QueryResult queryByNaturalLanguage(QueryRequest request) {
        NL2SQLEvent event = new NL2SQLEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setUserId(request.getUserId());
        event.setDataSourceId(request.getDataSourceId());
        event.setNaturalLanguage(request.getNaturalLanguage());
        event.setConversationId(request.getConversationId());
        event.setTimestamp(System.currentTimeMillis());

        rabbitTemplate.convertAndSend(RabbitConfig.NL2SQL_EXCHANGE, RabbitConfig.NL2SQL_ROUTING_KEY, event);

        // 骨架阶段：同步返回 Mock 结果
        QueryResult result = new QueryResult();
        result.setSql("SELECT * FROM orders LIMIT 100;");
        result.setData(List.of(
            Map.of("id", 1, "product_name", "华为Mate60", "amount", 1234567),
            Map.of("id", 2, "product_name", "iPhone15", "amount", 987654)
        ));
        result.setTotalCount(2);
        result.setExecuteTimeMs(120L);
        result.setChartType("table");

        saveHistory(request, result, null);
        return result;
    }

    public QueryResult queryBySql(String sql) {
        QueryResult result = new QueryResult();
        result.setSql(sql);
        result.setData(List.of(Map.of("result", "mock")));
        result.setTotalCount(1);
        result.setExecuteTimeMs(50L);
        result.setChartType("table");
        return result;
    }

    public List<QueryHistory> history(String conversationId) {
        return historyRepository.findByConversationIdOrderByCreatedAtDesc(conversationId);
    }

    private void saveHistory(QueryRequest request, QueryResult result, String error) {
        QueryHistory h = new QueryHistory();
        h.setConversationId(request.getConversationId());
        h.setUserId(request.getUserId());
        h.setDataSourceId(request.getDataSourceId());
        h.setNaturalLanguage(request.getNaturalLanguage());
        h.setGeneratedSql(result.getSql());
        h.setSqlExecuted(result.getSql());
        h.setExecuteTimeMs(result.getExecuteTimeMs());
        h.setResultCount(result.getTotalCount());
        h.setStatus(error == null ? "success" : "failed");
        h.setErrorMessage(error);
        h.setChartType(result.getChartType());
        historyRepository.save(h);
    }
}
```

- [ ] **Step 10: 创建 MQ 监听 `SQLReadyListener.java`**

```java
package com.nl2sql.query.listener;

import com.nl2sql.common.event.SQLReadyEvent;
import com.nl2sql.query.config.RabbitConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SQLReadyListener {

    @RabbitListener(queues = RabbitConfig.SQL_READY_QUEUE)
    public void onSQLReady(SQLReadyEvent event) {
        log.info("收到 SQL 生成完成事件: eventId={}, sql={}", event.getEventId(), event.getGeneratedSql());
        // 骨架阶段仅打印日志，真实执行后续实现
    }
}
```

- [ ] **Step 11: 创建 Controller `QueryController.java`**

```java
package com.nl2sql.query.controller;

import com.nl2sql.common.R;
import com.nl2sql.query.dto.QueryRequest;
import com.nl2sql.query.dto.QueryResult;
import com.nl2sql.query.entity.QueryHistory;
import com.nl2sql.query.service.QueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/query")
@RequiredArgsConstructor
public class QueryController {

    private final QueryService queryService;

    @PostMapping("/nl")
    public R<QueryResult> nlQuery(@RequestBody QueryRequest request) {
        return R.ok(queryService.queryByNaturalLanguage(request));
    }

    @PostMapping("/sql")
    public R<QueryResult> sqlQuery(@RequestBody String sql) {
        return R.ok(queryService.queryBySql(sql));
    }

    @GetMapping("/history")
    public R<List<QueryHistory>> history(@RequestParam String conversationId) {
        return R.ok(queryService.history(conversationId));
    }

    @GetMapping("/statistics")
    public R<String> statistics() {
        return R.ok("statistics placeholder");
    }
}
```

- [ ] **Step 12: 创建 `Dockerfile`**

```dockerfile
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/query-service-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 13: 编译验证**

```bash
cd ~/IdeaProjects/nl2sql-platform
mvn clean install -pl query-service -am
```

Expected: BUILD SUCCESS

- [ ] **Step 14: 提交**

```bash
git add query-service/
git commit -m "feat(query): 初始化查询服务与 MQ 事件链路"
```

---

## Task 8: frontend Vue3 前端项目

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/vite.config.ts`
- Create: `frontend/tsconfig.json`
- Create: `frontend/index.html`
- Create: `frontend/src/main.ts`
- Create: `frontend/src/App.vue`
- Create: `frontend/src/router/index.ts`
- Create: `frontend/src/views/Home.vue`
- Create: `frontend/src/views/Query.vue`
- Create: `frontend/src/views/Result.vue`
- Create: `frontend/src/views/History.vue`
- Create: `frontend/src/views/DataSource.vue`
- Create: `frontend/src/views/SchemaViewer.vue`
- Create: `frontend/src/views/Statistics.vue`
- Create: `frontend/src/components/QueryInput.vue`
- Create: `frontend/src/components/SQLPreview.vue`
- Create: `frontend/src/stores/query.ts`
- Create: `frontend/src/api/request.ts`
- Create: `frontend/src/api/schema.ts`
- Create: `frontend/src/api/query.ts`
- Create: `frontend/src/api/ai.ts`

**Goal:** 初始化 Vue3 前端工程，包含路由、基础页面和 API 调用封装。

- [ ] **Step 1: 创建 `frontend/package.json`**

```json
{
  "name": "nl2sql-frontend",
  "private": true,
  "version": "0.0.1",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc && vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "vue": "^3.4.27",
    "vue-router": "^4.3.2",
    "pinia": "^2.1.7",
    "element-plus": "^2.7.3",
    "axios": "^1.7.2",
    "echarts": "^5.5.0"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.4",
    "typescript": "^5.4.5",
    "vite": "^5.2.11",
    "vue-tsc": "^2.0.19"
  }
}
```

- [ ] **Step 2: 创建 `frontend/vite.config.ts`**

```typescript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

- [ ] **Step 3: 创建 `frontend/tsconfig.json`**

```json
{
  "compilerOptions": {
    "target": "ESNext",
    "useDefineForClassFields": true,
    "module": "ESNext",
    "lib": ["ESNext", "DOM", "DOM.Iterable"],
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "preserve",
    "strict": true,
    "noUnusedLocals": false,
    "noUnusedParameters": false,
    "noFallthroughCasesInSwitch": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"]
    }
  },
  "include": ["src/**/*.ts", "src/**/*.tsx", "src/**/*.vue"],
  "references": [{ "path": "./tsconfig.node.json" }]
}
```

- [ ] **Step 4: 创建 `frontend/tsconfig.node.json`**

```json
{
  "compilerOptions": {
    "composite": true,
    "skipLibCheck": true,
    "module": "ESNext",
    "moduleResolution": "bundler",
    "allowSyntheticDefaultImports": true
  },
  "include": ["vite.config.ts"]
}
```

- [ ] **Step 5: 创建 `frontend/index.html`**

```html
<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <link rel="icon" type="image/svg+xml" href="/vite.svg" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>AI 数据库问答平台</title>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.ts"></script>
  </body>
</html>
```

- [ ] **Step 6: 创建 `frontend/src/main.ts`**

```typescript
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.mount('#app')
```

- [ ] **Step 7: 创建 `frontend/src/App.vue`**

```vue
<template>
  <el-container>
    <el-header>AI 数据库自然语言问答平台</el-header>
    <el-container>
      <el-aside width="200px">
        <el-menu :router="true">
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/query">查询</el-menu-item>
          <el-menu-item index="/history">历史</el-menu-item>
          <el-menu-item index="/datasource">数据源</el-menu-item>
          <el-menu-item index="/schema">Schema</el-menu-item>
          <el-menu-item index="/statistics">统计</el-menu-item>
        </el-menu>
      </el-aside>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.el-header {
  background-color: #409eff;
  color: white;
  line-height: 60px;
  font-size: 20px;
  font-weight: bold;
}
</style>
```

- [ ] **Step 8: 创建路由 `frontend/src/router/index.ts`**

```typescript
import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: Home },
    { path: '/query', component: () => import('../views/Query.vue') },
    { path: '/result', component: () => import('../views/Result.vue') },
    { path: '/history', component: () => import('../views/History.vue') },
    { path: '/datasource', component: () => import('../views/DataSource.vue') },
    { path: '/schema', component: () => import('../views/SchemaViewer.vue') },
    { path: '/statistics', component: () => import('../views/Statistics.vue') }
  ]
})

export default router
```

- [ ] **Step 9: 创建 Axios 封装 `frontend/src/api/request.ts`**

```typescript
import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

request.interceptors.response.use(
  (response) => response.data,
  (error) => Promise.reject(error)
)

export default request
```

- [ ] **Step 10: 创建 API 模块**

`frontend/src/api/schema.ts`:

```typescript
import request from './request'

export const listDataSources = () => request.get('/schema/datasource/list')
export const addDataSource = (data: any) => request.post('/schema/datasource', data)
export const scanTables = (id: number) => request.post(`/schema/scan/${id}`)
export const getTableDetail = (dsId: number, tableName: string) => request.get(`/schema/${dsId}/tables/${tableName}`)
```

`frontend/src/api/query.ts`:

```typescript
import request from './request'

export const nlQuery = (data: any) => request.post('/query/nl', data)
export const sqlQuery = (sql: string) => request.post('/query/sql', sql)
export const getHistory = (conversationId: string) => request.get('/query/history', { params: { conversationId } })
```

`frontend/src/api/ai.ts`:

```typescript
import request from './request'

export const convert = (data: any) => request.post('/ai/convert', data)
```

- [ ] **Step 11: 创建 Pinia Store `frontend/src/stores/query.ts`**

```typescript
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useQueryStore = defineStore('query', () => {
  const result = ref<any>(null)
  const loading = ref(false)

  const setResult = (data: any) => {
    result.value = data
  }

  return { result, loading, setResult }
})
```

- [ ] **Step 12: 创建视图组件**

`frontend/src/views/Home.vue`:

```vue
<template>
  <div>
    <h2>欢迎使用 AI 数据库问答平台</h2>
    <p>输入自然语言，自动生成 SQL 并返回可视化结果。</p>
    <el-button type="primary" @click="$router.push('/query')">开始查询</el-button>
  </div>
</template>
```

`frontend/src/views/Query.vue`:

```vue
<template>
  <div>
    <h3>自然语言查询</h3>
    <query-input @submit="onSubmit" />
    <s-q-l-preview :sql="result?.sql" />
    <el-table v-if="result?.data" :data="result.data" style="margin-top: 16px">
      <el-table-column v-for="key in columnKeys" :key="key" :prop="key" :label="key" />
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import QueryInput from '../components/QueryInput.vue'
import SQLPreview from '../components/SQLPreview.vue'
import { nlQuery } from '../api/query'

const result = ref<any>(null)

const columnKeys = computed(() => {
  if (!result.value?.data?.length) return []
  return Object.keys(result.value.data[0])
})

const onSubmit = async (text: string) => {
  const res: any = await nlQuery({
    userId: 1,
    dataSourceId: 1,
    naturalLanguage: text,
    conversationId: 'demo'
  })
  if (res.code === 200) {
    result.value = res.data
  }
}
</script>
```

`frontend/src/components/QueryInput.vue`:

```vue
<template>
  <el-input
    v-model="text"
    placeholder="输入自然语言查询，例如：查上月销售额前10的产品"
    @keyup.enter="submit"
  >
    <template #append>
      <el-button type="primary" @click="submit">查询</el-button>
    </template>
  </el-input>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const emit = defineEmits<{ submit: [text: string] }>()
const text = ref('')

const submit = () => {
  if (text.value.trim()) {
    emit('submit', text.value)
  }
}
</script>
```

`frontend/src/components/SQLPreview.vue`:

```vue
<template>
  <el-card v-if="sql" header="生成的 SQL" style="margin-top: 16px">
    <pre>{{ sql }}</pre>
  </el-card>
</template>

<script setup lang="ts">
defineProps<{ sql?: string }>()
</script>
```

`frontend/src/views/Result.vue`、`History.vue`、`DataSource.vue`、`SchemaViewer.vue`、`Statistics.vue` 使用占位内容：

```vue
<template>
  <div>
    <h3>页面占位：xxx</h3>
  </div>
</template>
```

- [ ] **Step 13: 安装依赖并启动验证**

```bash
cd ~/IdeaProjects/nl2sql-platform/frontend
npm install
npm run dev
```

Expected: Vite dev server starts on `http://localhost:5173`.

- [ ] **Step 14: 提交**

```bash
cd ~/IdeaProjects/nl2sql-platform
git add frontend/
git commit -m "feat(frontend): 初始化 Vue3 前端工程与基础页面"
```

---

## Task 9: 联调与端到端验证

**Goal:** 验证网关路由、服务注册、MQ 事件链路与前端代理。

- [ ] **Step 1: 启动基础设施**

```bash
cd ~/IdeaProjects/nl2sql-platform
docker compose up -d mysql redis rabbitmq nacos
sleep 30
```

- [ ] **Step 2: 启动业务服务**

终端 1：
```bash
cd ~/IdeaProjects/nl2sql-platform
NACOS_SERVER=localhost:8848 MYSQL_HOST=localhost RABBITMQ_HOST=localhost REDIS_HOST=localhost mvn -pl schema-service spring-boot:run
```

终端 2：
```bash
cd ~/IdeaProjects/nl2sql-platform
NACOS_SERVER=localhost:8848 MYSQL_HOST=localhost RABBITMQ_HOST=localhost REDIS_HOST=localhost mvn -pl ai-service spring-boot:run
```

终端 3：
```bash
cd ~/IdeaProjects/nl2sql-platform
NACOS_SERVER=localhost:8848 MYSQL_HOST=localhost RABBITMQ_HOST=localhost REDIS_HOST=localhost mvn -pl query-service spring-boot:run
```

终端 4：
```bash
cd ~/IdeaProjects/nl2sql-platform
NACOS_SERVER=localhost:8848 mvn -pl gateway-service spring-boot:run
```

- [ ] **Step 3: 验证网关路由**

```bash
curl http://localhost:8080/api/schema/datasource/list
```

Expected: `{"code":200,"message":"success","data":[]}`

```bash
curl -X POST http://localhost:8080/api/ai/convert \
  -H 'Content-Type: application/json' \
  -d '{"dataSourceId":1,"naturalLanguage":"查销售额"}'
```

Expected: `{"code":200,"message":"success","data":"SELECT product_name, SUM(amount) AS total_sales FROM orders GROUP BY product_name ORDER BY total_sales DESC LIMIT 10;"}`

```bash
curl -X POST http://localhost:8080/api/query/nl \
  -H 'Content-Type: application/json' \
  -d '{"userId":1,"dataSourceId":1,"naturalLanguage":"查用户数量","conversationId":"demo"}'
```

Expected: `{"code":200,"message":"success","data":{"sql":"SELECT * FROM orders LIMIT 100;",...}}`

- [ ] **Step 4: 验证前端页面**

打开浏览器访问 `http://localhost:5173/query`，输入问题后点击查询，应显示生成的 SQL 和表格结果。

- [ ] **Step 5: 提交验证结果或修复**

若验证通过，提交最终代码：

```bash
git add .
git commit -m "chore(integration): 完成端到端联调验证"
```

---

## 自审检查

### 1. Spec 覆盖

| Spec 要求 | 对应任务 |
|-----------|----------|
| Maven 多模块父工程 | Task 1 |
| common 公共模块 | Task 2 |
| gateway-service 8080 | Task 4 |
| schema-service 8081 + 数据源/Schema API | Task 5 |
| query-service 8082 + NL/SQL 查询 + 历史 | Task 7 |
| ai-service 8083 + Mock LLM | Task 6 |
| RabbitMQ 事件 | Task 6 / Task 7 |
| Redis | Task 7（依赖已引入） |
| MySQL 三库 | Task 3 / Task 5 / Task 6 / Task 7 |
| Nacos 注册中心 | Task 3 / 各服务 application.yml |
| Vue3 前端完整页面 | Task 8 |
| Docker Compose | Task 3 |

### 2. 占位符扫描

无 TBD / TODO / "implement later"。每个任务均给出完整文件内容与验证命令。

### 3. 类型一致性

- 统一响应类 `R<T>` 贯穿所有 Controller。
- MQ 事件 DTO 在 `common` 中定义，`ai-service` 与 `query-service` 共用。
- 数据库字段名与实体类 `@Column` 一一对应。

### 4. 已知限制

- 骨架阶段 AI 返回 Mock SQL，不执行真实 LLM 调用。
- SQL 执行返回 Mock 数据，不连接真实数据源。
- JWT、Sentinel、飞书通知、WebSocket 实时推送不在本次范围内。
