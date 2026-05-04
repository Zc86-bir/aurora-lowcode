# 🌌 Aurora — AI 驱动的企业级低代码平台

> **用自然语言构建企业应用。** 一句话描述需求，AI 自动生成完整的前后端代码、表单、报表、工作流。

---

## 🎯 一句话介绍

Aurora 是一个基于 **Java 25 + Spring Boot 3.5 + Vue 3.5** 的 SaaS 级低代码平台，它让业务人员通过自然语言描述即可生成完整的企业应用（表单 + 报表 + 工作流 + API），同时满足金融级安全与合规要求。

---

## 💡 解决什么问题？

| 传统开发模式 | Aurora 低代码模式 |
|-------------|-------------------|
| 手写 CRUD 需要数天 | 自然语言描述，**30 秒** 生成 |
| 表单/报表需要前端 + 后端联调 | AI 自动生成全栈代码，**零联调** |
| 权限配置需要开发介入 | 自然语言定义权限策略，**即时生效** |
| 流程变更需要改代码重新部署 | 可视化流程设计器，**热更新无需重启** |
| 多租户架构需要从头设计 | 内置 Schema 级租户隔离，**开箱即用** |

---

## 🚀 核心能力

### 1. AI 代码生成 — 说人话，出代码

```
用户输入: "帮我建一个客户管理模块，包含姓名、电话、邮箱、公司、状态字段"

AI 自动生成:
  ✅ Java Entity + Controller + Service + Repository
  ✅ Vue 3 表单组件 + 数据表格
  ✅ TypeScript 类型定义
  ✅ SQL Migration 脚本
  ✅ JUnit5 单元测试
  ✅ OpenAPI 3.1 文档
```

### 2. 智能防幻觉 — 三重校验，绝不输出错误代码

```
LLM 输出 → [1. JSON Schema 校验] → [2. AST 语法分析] → [3. 业务规则检查]
                ↓                      ↓                      ↓
          结构是否正确          语法是否合法          是否符合业务逻辑
                ↓
          全部通过 → 写入文件
          任一失败 → 自动纠错（最多 2 轮）→ 仍失败 → 优雅降级
```

### 3. 实时协同设计 — 多人同时编辑，无冲突

基于 Yjs CRDT 算法，设计器支持多人实时协同编辑，类似 Google Docs 体验：
- 多光标实时可见
- 冲突自动合并
- 离线编辑，网络恢复后自动同步
- 每人独立 Undo/Redo

### 4. 多租户 SaaS 架构 — 一套代码，服务千家企业

| 隔离级别 | 实现方式 | 适用场景 |
|---------|----------|----------|
| Schema 隔离（默认） | 每租户独立 PostgreSQL Schema | 中型企业 |
| RLS 行级安全 | PostgreSQL Row-Level Security | 小微企业 |
| 数据库隔离 | 每租户独立数据库 | 政企/金融 |

租户全生命周期管理：开通 → 配额 → 归档 → 软删除 → 清除，自动化闭环。

### 5. 不可篡改审计 — 每条操作都可追溯，防篡改

基于 SHA-256 哈希链的审计日志系统：
```
Entry 0: hash_0 = SHA256(GENESIS + content_0)
Entry 1: hash_1 = SHA256(hash_0 + content_1)  ← 依赖上一条
Entry 2: hash_2 = SHA256(hash_1 + content_2)  ← 依赖上一条
...
```
修改任意一条记录会导致后续所有 hash 不匹配，完美满足等保/PIPL/GDPR 合规要求。

---

## 🏗️ 技术架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        Aurora 平台架构                           │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌────────────────┐  │
│  │ Vue 3 前端│  │ REST API │  │ 10个Skill│  │ AI 防幻觉流水线 │  │
│  │ Vue 3.5  │◄─┤ 网关层   │◄─┤ MCP协议  │◄─┤ 三重校验+自纠错│  │
│  └──────────┘  └──────────┘  └──────────┘  └────────────────┘  │
│       │             │             │               │             │
│       ▼             ▼             ▼               ▼             │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                  应用层 — 用例编排                         │  │
│  │  代码生成 | 元数据同步 | AI流水线 | 表单/报表/工作流运行时  │  │
│  └──────────────────────────────────────────────────────────┘  │
│                              │                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                  领域层 — DDD 聚合                         │  │
│  │  元数据 | 技能 | 租户 | 权限 | 审计                        │  │
│  └──────────────────────────────────────────────────────────┘  │
│                              │                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                  基础设施层                                │  │
│  │  PostgreSQL | Redis | OpenTelemetry | Resilience4j       │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### 核心技术栈

| 层级 | 技术 | 为什么选它 |
|------|------|-----------|
| **运行时** | Java 25 LTS | 虚拟线程 = 高并发 + 低内存 |
| **框架** | Spring Boot 3.5 | 官方支持 Java 25，生态完善 |
| **AI** | Spring AI + MCP | 标准化 AI 集成协议 |
| **前端** | Vue 3.5 + TS 5.5 + Vite 6 | 响应式 + 类型安全 + 极速构建 |
| **数据库** | PostgreSQL 17 | 原生 JSONB + RLS + 全文搜索 |
| **缓存** | Redis 7.4 | 多级缓存 + 分布式锁 |
| **协同** | Yjs CRDT | 无中心锁，最终一致性 |
| **可观测** | OpenTelemetry | Traces + Metrics + Logs 统一 |

---

## 📂 项目结构（精简版）

```
aurora-lowcode/
├── src/main/java/com/aurora/core/
│   ├── architecture/     ← DDD 战术模式（接口）
│   ├── contract/         ← 外部契约（Skill/AI/权限等）
│   ├── application/      ← 用例编排（Skill路由/AI校验）
│   ├── generator/        ← AI 代码生成器（8种文件类型）
│   ├── runtime/          ← 运行时引擎（表单/报表/工作流/热加载）
│   ├── ai/               ← AI 防护层（防幻觉/AST/规则/遥测）
│   ├── adapter/web/      ← REST API 网关
│   └── infrastructure/   ← 技术实现（缓存/审计/安全/弹性/租户）
│
├── frontend/             ← Vue 3 前端
│   └── src/
│       ├── components/   ← DynamicForm, DataTable...
│       ├── composables/  ← useServerState, CrdtSyncEngine
│       ├── utils/        ← ThemeCompiler, ChunkOptimizer, A11yTestRunner
│       └── styles/       ← Design Tokens (4主题模式)
│
├── skills/               ← 10 个 Skill 定义（MCP 协议）
│   ├── form_generator.yaml
│   ├── report_builder.yaml
│   └── ...
│
├── docs/
│   ├── ARCHITECTURE.md   ← 详细架构文档
│   ├── adr/              ← 架构决策记录（4篇）
│   └── reviews/          ← 代码审查报告（8份）
│
└── docker-compose.dev.yml ← 一键启动开发环境
```

---

## 🚀 快速开始

### 方式一：DevContainer（推荐）

```bash
git clone <repo-url>
code .                    # VS Code 自动打开 DevContainer
# 等待容器启动后：
cd AURORA-LOWCODE && mvn spring-boot:run   # 后端
cd frontend && pnpm dev                     # 前端
```

### 方式二：本地环境

```bash
# 前置要求：JDK 25 + Node 20 + pnpm 9 + Docker
docker compose -f docker-compose.dev.yml up -d   # 启动 PostgreSQL + Redis
mvn spring-boot:run                               # 后端 (localhost:8080)
cd frontend && pnpm dev                           # 前端 (localhost:3000)
```

### 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `DATABASE_HOST` | localhost | PostgreSQL 地址 |
| `DATABASE_PORT` | 5432 | PostgreSQL 端口 |
| `DATABASE_NAME` | aurora | 数据库名 |
| `DATABASE_USER` | aurora | 用户名 |
| `DATABASE_PASSWORD` | aurora_dev_password | 密码 |
| `REDIS_HOST` | localhost | Redis 地址 |
| `REDIS_PORT` | 6379 | Redis 端口 |
| `JWT_SECRET` | (必填) | JWT 签名密钥（生产环境） |
| `SPRING_PROFILES_ACTIVE` | dev | 运行环境 (dev/test/prod) |

---

## 🧩 可扩展能力

平台设计为**插件化架构**，所有核心能力均可替换或扩展：

### 扩展 AI 模型

```yaml
# 在 application.yml 中添加新的 LLM 提供商
spring:
  ai:
    custom-model:
      api-key: ${CUSTOM_MODEL_API_KEY}
      base-url: https://api.your-model.com
```

### 扩展 Skill

```bash
# 复制一个 Skill 模板，修改 input_schema / output_schema
cp skills/form_generator.yaml skills/my_custom_skill.yaml
# 重启即可自动注册
```

### 扩展数据源

实现 `MetadataRepository` 接口，即可接入新的元数据存储（MongoDB、Etcd 等）。

### 扩展主题

在 `tokens.css` 中添加新的 `data-theme` 区块，即可支持自定义主题。

---

## 📊 项目数据

| 指标 | 数值 |
|------|------|
| Java 文件 | 47 |
| 前端文件 | 11 |
| Skill 定义 | 10 |
| REST API 端点 | 15+ |
| Sealed Interfaces | 7（63 个 permits） |
| Record 类型 | 40+ |
| 安全规则 | 50+ |
| 业务规则 | 40+ |
| 编译状态 | ✅ PASS (JDK 25.0.2) |

---

## 📄 许可证

Apache License 2.0

---

> **了解更多**：阅读 [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) 获取完整架构文档。
