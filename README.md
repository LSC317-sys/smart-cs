# SmartCS - AI 智能客服系统

基于 RAG（Retrieval-Augmented Generation）架构的企业级智能客服平台，支持知识库管理、文档解析、语义检索与多轮 AI 对话。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.5.2 / Spring Cloud 2025.0.0 |
| 微服务治理 | Spring Cloud Alibaba (Nacos + Gateway) |
| 持久层 | MyBatis-Plus + MySQL + PostgreSQL(pgvector) |
| 缓存 | Redis |
| AI 服务 | SiliconFlow API (Embedding + Chat) |
| 文件存储 | MinIO |
| 前端 | Vue 3 + Vite + Element Plus + ECharts |
| 部署 | Docker + Docker Compose |
| 监控 | Prometheus + Loki + Grafana |

## 系统架构

```
前端 (Vue 3 + Element Plus + ECharts)
         | HTTP / SSE
  Gateway (Spring Cloud Gateway :8080)
  ----+----+----+----+----+
 Chat  Knowledge User Admin Common
(8081) (8082) (8083)(8084) [公共]
  |      |      |     |
  MySQL + PostgreSQL(pgvector) + Redis
```

### 服务模块

| 服务 | 端口 | 职责 |
|------|------|------|
| smart-cs-gateway | 8080 | API 网关，统一路由与 CORS |
| smart-cs-chat | 8081 | 多轮对话、RAG 问答、SSE 流式响应 |
| smart-cs-knowledge | 8082 | 知识库管理、文档解析与向量化 |
| smart-cs-user | 8083 | 用户认证与权限管理（JWT） |
| smart-cs-admin | 8084 | 后台统计与反馈管理 |

## 核心功能

- **RAG 智能问答**：文档解析 -> 文本分块 -> Embedding 向量化 -> pgvector 语义检索 -> LLM 生成回答
- **知识库管理**：文档上传、版本控制、回滚、多分类管理
- **多轮对话**：基于会话上下文的连续问答，支持流式（SSE）输出
- **权限控制**：JWT 认证 + RBAC 角色权限
- **统计看板**：对话量、知识库、反馈数据可视化

## 快速开始

### 前置要求

- JDK 17+
- Docker & Docker Compose
- Node.js 18+（前端开发）

### 使用 Docker Compose 一键部署

```bash
cd docker
cp .env.example .env
docker compose up -d
```

### 本地开发

```bash
# 启动中间件
cd docker && docker compose up -d mysql redis postgres

# 启动后端服务
mvn spring-boot:run -pl smart-cs-gateway
mvn spring-boot:run -pl smart-cs-user
mvn spring-boot:run -pl smart-cs-knowledge
mvn spring-boot:run -pl smart-cs-chat
mvn spring-boot:run -pl smart-cs-admin

# 启动前端
cd smart-cs-frontend && npm install && npm run dev
```

## 项目结构

```
smart-cs/
  smart-cs-gateway/       API 网关
  smart-cs-chat/          对话服务
  smart-cs-knowledge/     知识库服务
  smart-cs-user/          用户服务
  smart-cs-admin/         管理后台服务
  smart-cs-common/        公共模块
  smart-cs-frontend/      Vue3 前端
  docker/                 Docker 编排
  docs/                   设计文档
```

## 线上演示

项目已部署，可访问：http://121.199.172.24
