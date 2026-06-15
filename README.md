# NL2SQL Platform

AI 数据库自然语言问答平台：让非技术人员通过中文自然语言查询数据库，自动生成 SQL 并返回可视化结果。

## 技术栈

- 前端：Vue 3 + Vite + TypeScript + Element Plus + Pinia + ECharts
- 后端：Spring Boot 3.2 + Spring Cloud Alibaba 2023 + Maven
- 中间件：MySQL 8.0 + Redis 7 + RabbitMQ 3.12 + Nacos 2.2.3
- 部署：Docker + Docker Compose

## 项目结构

```
nl2sql-platform/
├── common/              # 公共模块
├── gateway-service/     # API 网关 (8080)
├── schema-service/      # Schema 服务 (8081)
├── query-service/       # 查询服务 (8082)
├── ai-service/          # AI 服务 (8083)
├── frontend/            # 前端应用
├── docker-compose.yml   # 基础设施编排
└── README.md
```

## 快速开始

```bash
# 1. 启动基础设施
docker compose up -d mysql redis rabbitmq nacos

# 2. 等待约 30 秒后启动业务服务
# （各服务目录下执行）
mvn spring-boot:run

# 3. 启动前端
cd frontend
npm install
npm run dev
```

## 文档

- [项目初始化设计](docs/superpowers/specs/2026-06-16-nl2sql-init-design.md)
