# 🌌 Aurora — AI 驱动的企业级低代码平台

> **用自然语言构建企业应用。** 一句话描述需求，AI 自动生成完整的前后端代码、表单、报表、工作流。
>
> 基于 **Java 25 LTS + Spring Boot 3.5.0 + Vue 3.5 + TypeScript 5.5**

---

## 📑 目录

- [一、项目概述](#一项目概述)
- [二、已完成阶段总览](#二已完成阶段总览)
- [三、核心技术原理](#三核心技术原理)
- [四、完整架构设计](#四完整架构设计)
- [五、全部文件清单](#五全部文件清单)
- [六、安全体系](#六安全体系)
- [七、前端架构](#七前端架构)
- [八、部署与工程化](#八部署与工程化)
- [九、版本与审查记录](#九版本与审查记录)
- [十、扩展指南](#十扩展指南)
- [附录：完整统计](#附录完整统计)
- [十一、PHASE 5 生产交付](#十一phase-5-生产交付)
- [十二、PHASE 6 核心运行时与安全引导](#十二phase-6-核心运行时与安全引导)
- [十三、PHASE 7 架构治理与企业级补全](#十三phase-7-架构治理与企业级补全)
- [十四、PHASE 9 企业集成、可靠性 & V1.0.0 GA](#十四phase-9-企业集成可靠性--v100-ga)

---

## 一、项目概述

### 1.1 一句话介绍

Aurora 是一个基于 **Java 25 + Spring Boot 3.5 + Vue 3.5** 的 SaaS 级低代码平台，业务人员通过自然语言描述即可生成完整的企业应用（表单 + 报表 + 工作流 + API），同时满足金融级安全与合规要求。

### 1.2 解决什么问题？

| 传统开发模式 | Aurora 低代码模式 |
|-------------|-------------------|
| 手写 CRUD 需要数天 | 自然语言描述，**30 秒** 生成 |
| 表单/报表需要前端 + 后端联调 | AI 自动生成全栈代码，**零联调** |
| 权限配置需要开发介入 | 自然语言定义权限策略，**即时生效** |
| 流程变更需要改代码重新部署 | 可视化流程设计器，**热更新无需重启** |
| 多租户架构需要从头设计 | 内置 Schema 级租户隔离，**开箱即用** |
| LLM 输出不可信 | 三重校验（Schema + AST + 规则），**防幻觉** |

### 1.3 核心能力一览

| 能力 | 说明 |
|------|------|
| **AI 代码生成** | 自然语言 → Java/Vue/TS/SQL/Test 8 种文件 |
| **防幻觉流水线** | Schema + AST + 业务规则三重校验 + 自纠错 |
| **表单运行时** | 动态渲染、实时验证、条件可见性、虚拟滚动 |
| **报表运行时** | 声明式数据绑定、流式导出、分页排序过滤 |
| **工作流运行时** | BPMN 2.0 解析、虚拟线程并行、静态分析 |
| **元数据热加载** | SHA-256 差异同步、原子替换、回滚 |
| **多租户 SaaS** | Schema/RLS/Database 三种隔离，全生命周期管理 |
| **不可篡改审计** | SHA-256 哈希链，满足等保/PIPL/GDPR |
| **实时协同** | Yjs CRDT，多光标、离线同步、无中心锁 |
| **弹性容错** | Resilience4j 熔断/重试/舱壁 + 5 种降级策略 |

### 2.8 PHASE 5 — PRODUCTION DELIVERY & JEECG BOOT ALIGNMENT ✅

| 类别 | 成果 |
|------|------|
| **JeecgBoot 兼容** | 10 个新 Skill YAML（jeecg-codegen、jeecg-bpmn、jeecg-onlform 等），3 个新增（jeecg-desform、jimubi-bigscreen、jimureport） |
| **别名路由** | SkillDefinitionLoader 支持 `aliases` + `jeecg_compat` 字段，`resolveAlias()` 实现旧名称→新名称透明路由 |
| **Flyway 迁移** | 3 个 SQL 脚本（V1 核心 11 表 + V2 JeecgBoot 8 表 + V3 种子数据），TIMESTAMPTZ + SHA-256 哈希链 |
| **集成测试** | 53 个测试用例（AI 流水线 + 租户隔离 + Skill 执行 + 审计链 + 代码生成安全） |
| **Docker/K8s** | Dockerfile.prod（多阶段 + ZGC + 非 root）+ docker-compose.prod.yml + Helm Chart（10 个模板文件） |
| **CI/CD** | GitHub Actions（CI: 构建/测试/SpotBugs/OWASP/JaCoCo/Trivy + CD: GHCR 推送/Helm OCI/K8s 部署） |
| **工程化** | Makefile（18 个目标）+ BOOTSTRAP.md + scripts/verify.sh（8 项检查） |

---

## 二、已完成阶段总览

### 2.1 PHASE 0 — 架构治理与可重现基座 ✅

| 类别 | 成果 |
|------|------|
| **架构层** | 7 个 DDD 战术模式接口（AggregateRoot、DomainEvent、Entity、ValueObject、Repository、Specification、UseCase） |
| **契约层** | 10 个外部契约接口（MetadataRepository、SkillExecutor、AIPipeline、TenantContext、AuditLogger、PermissionChecker、DataMasker、CacheProvider、EventBus、LockProvider） |
| **工程化** | `pom.xml`（Spring Boot 3.5.0 BOM）、`application.yml`（dev/test/prod 三 Profile）、`.devcontainer/`、`docker-compose.dev.yml` |
| **ADR** | 4 篇架构决策记录（虚拟线程、元数据 GitOps、DDD 边界、延迟 JPMS） |
| **安全** | 无 System.out、无硬编码密码、无 TODO |

### 2.2 PHASE 1 — AI Skill Engine & Metadata Pipeline ✅

| 类别 | 成果 |
|------|------|
| **Skill 定义** | 10 个 MCP 合规 YAML（form_generator、report_builder、workflow_designer 等） |
| **应用层** | AIPipelineOrchestrator（8 阶段流水线 + 并行验证）、MetadataValidator（三层校验）、SkillRouter（意图识别 → Skill 路由）、SkillDefinitionLoader（YAML 热加载） |
| **Prompt 模板** | Jinja2 语法，版本化（v1.0.0.j2），支持 A/B 测试 |
| **校验** | JSON Schema Draft 2020-12，强制 `$id` + `version` |

### 2.3 PHASE 2 — LOW-CODE RUNTIME & GENERATORS ✅

| 类别 | 成果 |
|------|------|
| **代码生成器** | CodeGenerator（8 种文件类型生成：Java 4 层 + Vue + TS + SQL + Test） |
| **表单运行时** | FormRuntimeEngine（动态渲染、字段验证、条件可见性、数据类型转换） |
| **报表运行时** | ReportRuntimeEngine（声明式数据绑定、租户强制隔离、流式导出、SQL 白名单） |
| **工作流运行时** | WorkflowRuntimeEngine（BPMN 2.0 解析、虚拟线程并行、死锁/循环检测、SLA 追踪） |
| **热加载管理器** | MetadataHotReloadManager（SHA-256 差异检测、版本回滚、监听器通知） |
| **API 网关** | ApiGatewayController（15+ REST 端点、租户作用域、权限检查） |

### 2.4 PHASE 1 补充 — AI 防幻觉流水线与 Skill 治理 ✅

| 类别 | 成果 |
|------|------|
| **自纠错循环** | AiSelfCorrectionLoop（最多 2 轮修正 → 失败触发 Fallback） |
| **AST 防火墙** | AstSyntaxFirewall（JavaParser 语法树、import 白名单、反射检测） |
| **JSON Schema 校验** | JsonSchemaValidator（Draft 2020-12、Schema 缓存、元字段强制校验） |
| **业务规则引擎** | BusinessRuleEngine（PATTERN/FORBIDDEN/REQUIRED/LIMIT，Sealed Interface） |
| **Skill 遥测** | SkillTelemetry（tokens_in/out、latency_ms、cost_usd、Micrometer 指标暴露） |

### 2.5 PHASE 3 — 零信任多租户与合规可观测 ✅

| 类别 | 成果 |
|------|------|
| **ABAC 策略引擎** | AbacPolicyEngine（subject + resource + action + env 四维、DENY 优先、热加载） |
| **租户生命周期** | TenantLifecycleManager（开通 → 配额 → 归档 → 软删除 → 清除，三种隔离模式） |
| **不可篡改审计** | ImmutableAuditLogger（SHA-256 哈希链、完整性验证、导出存证） |
| **弹性容错** | ResilienceConfig（Resilience4j 熔断/重试/舱壁、虚拟线程池隔离） |
| **数据脱敏** | DataMaskingInterceptor（@Mask 注解、7 种脱敏类型、CSP 兼容） |
| **脱敏注解** | Mask.java（ID_CARD/PHONE/EMAIL/BANK_CARD/NAME/ADDRESS/CUSTOM） |

### 2.6 PHASE 3 补充 — 协同设计器与前端工程化 ✅

| 类别 | 成果 |
|------|------|
| **Design Token** | tokens.css（4 主题模式：light/dark/high-contrast/colorblind，WCAG 2.1 AA） |
| **服务端状态** | useServerState.ts（TanStack Query 封装、乐观更新、禁止 Pinia 直存） |
| **CRDT 协同** | CrdtSyncEngine.ts（Yjs、多光标、离线同步、去重、Undo/Redo） |
| **无障碍测试** | A11yTestRunner.ts（axe-core 集成、CI 阻断构建、Router 自动扫描） |
| **主题编译器** | ThemeCompiler.ts（运行时切换 < 50ms、缓存上限 10 个） |
| **Chunk 优化** | ChunkOptimizer.ts（路由级分割、hover prefetch、AVIF/WebP 自动降级、LCP < 2.0s） |
| **Vite 6 配置** | vite.config.ts（chunk 分割策略、gzip+brotli 压缩、CSP 兼容） |
| **动态表单** | DynamicForm.vue（>20 字段自动虚拟滚动、ARIA 无障碍、ReDoS 防护） |
| **数据表格** | DataTable.vue（搜索/排序/分页/列选择器、骨架屏加载态） |
| **字段渲染器** | FormFieldRenderer.vue（根据 type 自动选择 input/textarea/select） |
| **类型定义** | form.ts（FormField + FormSchema） |
| **工程化** | tsconfig.json、env.d.ts、package.json |

### 2.7 版本升级 ✅

| 组件 | 旧版本 | 新版本 |
|------|--------|--------|
| Spring Boot | 3.4.0 | **3.5.0** |
| Spring AI | 1.0.0-M4 | **1.0.0-M6** |
| Spring Framework | 6.2.0 | **6.3.0** |
| Spring Security | 6.4.0 | **6.5.0** |
| Micrometer | 1.14.2 | **1.15.0** |
| OpenTelemetry | 1.45.0 | **1.46.0** |
| Resilience4j | 2.2.0 | **2.3.0** |
| Flyway | 10.22.1 | **11.3.1** |
| PostgreSQL JDBC | 42.7.4 | **42.7.5** |
| Mockito | 5.14.2 | **5.16.0** |

### 2.9 生产交付补充 ✅

| 类别 | 成果 |
|------|------|
| **AI 成本防火墙** | `AiCostFirewallFilter`（429 限流 + 402 成本超限）、`TenantRateLimiter`（Redisson RSemaphore + RRateLimiter）、`TenantCostTracker`（月度 USD 熔断） |
| **可观测性三驾马车** | Prometheus（Metrics）+ Tempo（Traces）+ Loki（Logs），Grafana 双向联动（Metrics↔Traces↔Logs） |
| **LLM 高可用路由** | `LlmProviderRouter`（Anthropic→OpenAI 熔断器 + cooldown 恢复）、`ExternalSecrets`（ESO + Vault/AWS/GCP） |
| **前端 CDN 缓存** | `FrontendCacheConfig`（HTML no-cache + assets immutable 365d）、Nginx 模板 + Helm Ingress CDN 注解 |
| **数据库备份与 PITR** | K8s CronJob（pg_dump→MinIO 每日备份）+ pgBackRest（WAL 归档 + 时间点恢复） |
| **优雅停机** | `server.shutdown: graceful` + `spring.lifecycle.timeout-per-shutdown-phase: 1m`（AI 流水线安全完成） |
| **审计 traceId 关联** | `AuditEntry.traceId` + `TraceIdFilter`（MDC 注入）+ Loki structured labels 提取 |
| **Promtail 日志采集** | Docker/K8s/Nginx 容器日志 + traceId/tenantId/level 标签提取 |

### 2.10 PHASE 6 — 核心运行时与安全引导 ✅

| 类别 | 成果 |
|------|------|
| **JWT 鉴权** | JwtTokenProvider（HS256）+ JwtAuthenticationFilter + SecurityFilterChain + AuthController |
| **JPA 持久层** | 4 实体（Metadata/Tenant/SkillRegistry/AuditChain）+ 4 Repository + @Filter 多租户 |
| **Spring AI** | LlmGatewayService（call/retry/cost）+ MockLlmGateway + AiSelfCorrectionLoop 真实调用 |
| **Vue3 前端** | main.ts + App.vue + Router（auth 守卫）+ Pinia 状态管理 + API 拦截器 |
| **单元测试** | JwtTokenProviderTest(13) + LlmGatewayServiceTest(5) + 前端 Vitest |
| **代码审查** | 6 项修复（ScopedValueTenantContext + TenantFilterInterceptor + Token 黑名单等） |

### 2.11 PHASE 7 — 架构治理与企业级补全 ✅

| 类别 | 成果 |
|------|------|
| **ArchUnit 架构守护** | 5 测试类 20 rules（分层/隔离/Java25红线/命名/OpenAPI） |
| **I18N 国际化** | I18nConfig + messages.properties(中/英 + vue-i18n + /api/v1/i18n 端点) |
| **邮件通知** | EmailNotificationService + Thymeleaf 模板 + StructuredTaskScope 15s 超时 |
| **WebSocket 协同** | YjsWebSocketHandler（二进制协议）+ WebSocketAuthInterceptor + DocumentRoomManager |
| **E2E 测试** | Playwright + login-flow/dashboard-render + CI job |
| **API 契约校验** | OpenApiValidationTest + OpenApiAnnotationTest + sync-api.sh |
| **代码审查** | 10 项修复（CRDT 负载、ThreadLocal 排除、synchronized 移除等） |

---

## 三、核心技术原理

### 3.1 AI 防幻觉流水线

```
LLM Output ──► [1. JSON Schema 校验] ──失败──► 携带错误栈回传 LLM（最多 2 轮）
                      │通过
                      ▼
               [2. AST 语法防火墙] ──失败──► 携带错误栈回传 LLM
                      │通过
                      ▼
               [3. 业务规则引擎] ──失败──► 携带错误栈回传 LLM
                      │通过
                      ▼
               [Output] ──► 写入文件
```

**关键文件**：`AiSelfCorrectionLoop.java`、`JsonSchemaValidator.java`、`AstSyntaxFirewall.java`、`BusinessRuleEngine.java`

### 3.2 元数据热加载

```
[DB: metadata 表] ──► findByTenantAndName()
                           │
                    [计算 SHA-256 checksum]
                           │
                    ┌──────▼──────┐
                    │ 与缓存对比   │
                    └──────┬──────┘
                     不同？ │
                    ┌───▼───┐
                    │加载+替换│ 否：使用缓存
                    └───┬───┘
                        ▼
                [通知所有监听器]
```

**关键文件**：`MetadataHotReloadManager.java`

### 3.3 代码生成器

| 输入 | 实体名 + 字段列表 + 包前缀 |
|------|------|
| 输出 | Java Entity/Controller/Service/Repository + Vue SFC + TypeScript + SQL + Test |
| 安全 | entityName/tableName 白名单 + 路径穿越检测 + XSS 转义 |

**关键文件**：`CodeGenerator.java`

### 3.4 不可篡改审计日志

```
Entry 0: hash_0 = SHA256(GENESIS + content_0)
Entry 1: hash_1 = SHA256(hash_0 + content_1)
Entry 2: hash_2 = SHA256(hash_1 + content_2)
```

修改任意 entry 导致后续所有 hash 不匹配。`verifyChain()` 遍历验证。

**关键文件**：`ImmutableAuditLogger.java`

---

## 四、完整架构设计

### 4.1 六边形架构 + DDD

```
┌─────────────────────────────────────────────────────────────────┐
│                         外部系统                                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐    │
│  │ Vue 前端  │  │ LLM API  │  │ Redis    │  │ PostgreSQL   │    │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └──────┬───────┘    │
│       │             │             │               │             │
├───────▼─────────────▼─────────────▼───────────────▼─────────────┤
│                     适配器层 (Adapter)                            │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐    │
│  │REST API  │  │LLM Client│  │CacheImpl │  │JPA Repository│    │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └──────┬───────┘    │
├───────▼─────────────▼─────────────▼───────────────▼─────────────┤
│                     应用层 (Application)                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐    │
│  │UseCase   │  │AI Pipeline│  │Code Gen  │  │Metadata Sync │    │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └──────┬───────┘    │
├───────▼─────────────▼─────────────▼───────────────▼─────────────┤
│                     领域层 (Domain)                               │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐    │
│  │Metadata  │  │Skill     │  │Tenant    │  │Permission    │    │
│  │Aggregate │  │Aggregate │  │Aggregate │  │Aggregate     │    │
│  └──────────┘  └──────────┘  └──────────┘  └──────────────┘    │
├─────────────────────────────────────────────────────────────────┤
│                     基础设施层 (Infrastructure)                   │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐    │
│  │OpenTelemetry│Resilience│  │ABAC Policy│  │Audit Chain  │    │
│  └──────────┘  └──────────┘  └──────────┘  └──────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

**依赖方向严格单向**：Adapter → Application → Domain ← Infrastructure

### 4.2 模块分层职责

| 层 | 包路径 | 职责 | 典型文件 |
|---|--------|------|----------|
| **Architecture** | `core.architecture` | DDD 战术模式接口（纯接口，无实现） | AggregateRoot, DomainEvent, Repository |
| **Contract** | `core.contract` | 外部契约接口 | SkillExecutor, AIPipeline, PermissionChecker |
| **Application** | `application` | 用例编排、AI 校验 | SkillRouter, MetadataValidator, AIPipelineOrchestrator |
| **Generator** | `generator` | AI 代码生成 | CodeGenerator |
| **Runtime** | `runtime` | 表单/报表/工作流/热加载运行时 | FormRuntimeEngine, ReportRuntimeEngine, WorkflowRuntimeEngine, MetadataHotReloadManager |
| **AI** | `ai` | AI 防护层 | AiSelfCorrectionLoop, AstSyntaxFirewall, SkillTelemetry |
| **Adapter** | `adapter.web`, `adapter.security`, `adapter.mcp` | REST API + 认证 + MCP | ApiGatewayController, AuthController, JwtAuthenticationFilter, SecurityFilterChainConfig |
| **Infrastructure** | `infrastructure.*` | 技术实现 | JwtTokenProvider, LlmGatewayService, ScopedValueTenantContext, TenantFilterConfigurer + 15 个现有类 |

---

## 五、全部文件清单

### 5.1 Java 后端（78 个文件）

#### Architecture（8 个）

| 文件 | 路径 | 功能 |
|------|------|------|
| `AggregateRoot.java` | `architecture/` | DDD 聚合根接口 + 领域事件 |
| `DomainEvent.java` | `architecture/` | Sealed interface（6 permits） |
| `Entity.java` | `architecture/` | 实体标识接口 |
| `ValueObject.java` | `architecture/` | 值对象接口 + 校验 |
| `Repository.java` | `architecture/` | 仓储接口 + 分页 |
| `Specification.java` | `architecture/` | 规约模式（AND/OR/NOT） |
| `DomainService.java` | `architecture/` | 领域服务标记 |
| `UseCase.java` | `architecture/` | 用例标记 + 校验 |

#### Contract（10 个）

| 文件 | 路径 | 功能 |
|------|------|------|
| `MetadataRepository.java` | `contract/` | 元数据持久化（Sealed 10 permits） |
| `SkillExecutor.java` | `contract/` | Skill 执行（Sealed Result/Fallback） |
| `AIPipeline.java` | `contract/` | 8 阶段 AI 流水线 |
| `TenantContext.java` | `contract/` | 多租户上下文传播 |
| `AuditLogger.java` | `contract/` | 结构化 JSON 审计 |
| `PermissionChecker.java` | `contract/` | RBAC + ABAC 混合权限 |
| `DataMasker.java` | `contract/` | 字段级/行级脱敏 |
| `CacheProvider.java` | `contract/` | L1/L2/L3 多级缓存 |
| `EventBus.java` | `contract/` | 事件总线 + 死信队列 |
| `LockProvider.java` | `contract/` | 分布式锁 + 读写锁 |

#### Application（4 个）

| 文件 | 路径 | 功能 |
|------|------|------|
| `AIPipelineOrchestrator.java` | `application/` | 8 阶段流水线 + StructuredTaskScope 并行验证 |
| `MetadataValidator.java` | `application/` | Schema + 业务规则 + 安全三层校验 |
| `SkillRouter.java` | `application/` | 意图识别 → Skill 路由（中英文支持） |
| `SkillDefinitionLoader.java` | `application/` | Skill YAML 解析 + 热重载 |

#### Generator（1 个）

| 文件 | 路径 | 功能 |
|------|------|------|
| `CodeGenerator.java` | `generator/` | 8 种文件生成 + 安全加固（路径穿越/XSS 防护） |

#### Runtime（4 个）

| 文件 | 路径 | 功能 |
|------|------|------|
| `FormRuntimeEngine.java` | `runtime/` | 动态表单渲染 + 验证 + 条件可见性 |
| `ReportRuntimeEngine.java` | `runtime/` | 声明式数据绑定 + SQL 白名单 + 流式导出 |
| `WorkflowRuntimeEngine.java` | `runtime/` | BPMN 2.0 + 虚拟线程并行 + 静态分析 |
| `MetadataHotReloadManager.java` | `runtime/` | SHA-256 热加载 + 版本回滚 |

#### AI（5 个）

| 文件 | 路径 | 功能 |
|------|------|------|
| `AiSelfCorrectionLoop.java` | `ai/` | 三重校验 + LlmGatewayService 真实调用 + Fallback |
| `AstSyntaxFirewall.java` | `ai/` | JavaParser AST + import 白名单 + 反射检测 |
| `JsonSchemaValidator.java` | `ai/` | Draft 2020-12 + Schema 缓存 |
| `BusinessRuleEngine.java` | `ai/` | 4 种规则类型（Sealed） |
| `SkillTelemetry.java` | `ai/` | Micrometer 指标 + 成本估算 + recordLlmCall() |

#### Adapter（9 个）

| 文件 | 路径 | 功能 |
|------|------|------|
| `ApiGatewayController.java` | `adapter/web/` | 15+ REST 端点 + 输入校验 |
| `AuthController.java` | `adapter/web/` | 登录/登出/用户信息端点 |
| `I18nController.java` | `adapter/web/` | 国际化消息端点 |
| `JwtAuthenticationFilter.java` | `adapter/security/` | JWT 认证过滤器 + Token 黑名单 |
| `SecurityFilterChainConfig.java` | `adapter/security/` | Spring Security 无状态配置 |
| `McpSecurityFilter.java` | `adapter/mcp/` | MCP 端点 JWT 认证（委托 JwtTokenProvider） |
| `YjsWebSocketHandler.java` | `adapter/websocket/` | Yjs 二进制协议处理 |
| `WebSocketAuthInterceptor.java` | `adapter/websocket/` | WebSocket 握手 JWT 验证 |

#### Infrastructure（29 个）

| 文件 | 路径 | 功能 |
|------|------|------|
| `RedisCacheProvider.java` | `infrastructure/cache/` | L1 内存缓存 + 租户隔离 || `VirtualThreadEventBus.java` | `infrastructure/event/` | 虚拟线程事件分发 + 死信队列 |
| `RedisLockProvider.java` | `infrastructure/lock/` | 分布式锁 + 租约机制 |
| `ObservabilityManager.java` | `infrastructure/observability/` | OpenTelemetry Traces + Metrics |
| `StructuredJsonAuditLogger.java` | `infrastructure/audit/` | JSON 审计日志 + CSV 导出 |
| `MultiTenantDataSourceManager.java` | `infrastructure/database/` | HikariCP 多租户数据源 |
| `TenantFilterConfigurer.java` | `infrastructure/database/` | Hibernate @Filter 启用/禁用 |
| `TenantFilterInterceptor.java` | `infrastructure/database/` | HandlerInterceptor 拦截器 |
| `TenantFilterWebMvcConfigurer.java` | `infrastructure/database/` | 注册拦截器到 /api/** |
| `AbacPolicyEngine.java` | `infrastructure/policy/` | ABAC 四维策略引擎 |
| `TenantLifecycleManager.java` | `infrastructure/tenancy/` | 租户全生命周期 |
| `ScopedValueTenantContext.java` | `infrastructure/tenancy/` | ScopedValue 租户上下文实现 |
| `ImmutableAuditLogger.java` | `infrastructure/audit/` | SHA-256 哈希链 |
| `ResilienceConfig.java` | `infrastructure/resilience/` | Resilience4j 熔断/重试/舱壁 |
| `JwtTokenProvider.java` | `infrastructure/security/` | HS256 JWT 签发/验证 |
| `AuroraProperties.java` | `infrastructure/config/` | `@ConfigurationProperties` 全量配置 |
| `DataMaskingInterceptor.java` | `infrastructure/security/` | `@Mask` 注解脱敏 |
| `Mask.java` | `infrastructure/security/` | `@Mask` 注解定义 + MaskType 枚举 |
| `StructuredDataMasker.java` | `infrastructure/security/` | 7 种脱敏策略实现 |
| `LlmProviderRouter.java` | `infrastructure/ai/` | LLM 主备路由 + 熔断器 |
| `LlmRoutingAutoConfiguration.java` | `infrastructure/ai/` | Spring Auto-Config |
| `LlmGatewayService.java` | `infrastructure/ai/` | LLM 调用网关 + retry + 成本估算 |
| `MockLlmGateway.java` | `infrastructure/ai/` | @Profile("test") 测试替身 |
| `I18nConfig.java` | `infrastructure/config/` | MessageSource + LocaleResolver |
| `WebSocketConfig.java` | `infrastructure/config/` | WebSocket 端点注册 |
| `DocumentRoomManager.java` | `infrastructure/collaboration/` | WebSocket 房间管理 |
| `EmailNotificationService.java` | `infrastructure/notification/` | Thymeleaf + StructuredTaskScope 超时 |
| `WebSearchService.java` | `infrastructure/search/` | opencli 联网搜索 + StructuredTaskScope 超时 |

#### JPA Entity（4 个）

| 文件 | 路径 | 功能 |
|------|------|------|
| `MetadataEntity.java` | `infrastructure/database/entity/` | metadata 表映射 + JSONB + @Filter |
| `TenantEntity.java` | `infrastructure/database/entity/` | tenant 表映射 |
| `SkillRegistryEntity.java` | `infrastructure/database/entity/` | skill_registry 表映射 + JSONB |
| `AuditChainEntity.java` | `infrastructure/database/entity/` | audit_log 表映射 + 哈希链 |

#### JPA Repository（4 个）

| 文件 | 路径 | 功能 |
|------|------|------|
| `MetadataRepositoryJpa.java` | `infrastructure/database/repository/` | 分页 + 版本查询 + 批量更新 |
| `TenantRepositoryJpa.java` | `infrastructure/database/repository/` | 按 code 查询 + 活跃过滤 |
| `SkillRegistryJpa.java` | `infrastructure/database/repository/` | JeecgBoot 兼容 + category 查询 |
| `AuditChainJpa.java` | `infrastructure/database/repository/` | seqNum 查询 + 时间范围 |

### 5.2 前端（34 个文件）

| 文件 | 路径 | 功能 |
|------|------|------|
| `main.ts` | `src/` | Vue3 + Pinia + Router + i18n 挂载 |
| `App.vue` | `src/` | 布局壳 + 主题 + 全局 Loading（CSP scoped） |
| `tokens.css` | `styles/` | Design Tokens（4 主题模式） |
| `vite.config.ts` | `/` | Vite 6 配置（chunk 分割 + CSP + brotli/gzip） |
| `router/index.ts` | `src/router/` | 动态路由懒加载 + auth 守卫 |
| `stores/auth.ts` | `src/stores/` | Pinia auth store + JWT 解码 + 过期检查 |
| `stores/tenant.ts` | `src/stores/` | Pinia tenant store |
| `plugins/api-interceptor.ts` | `src/plugins/` | fetch 拦截器：自动注入 Authorization + X-Tenant-Id + Accept-Language |
| `i18n/index.ts` | `src/i18n/` | vue-i18n 配置 + 浏览器语言检测 |
| `i18n/locales/en.ts` | `src/i18n/locales/` | 前端英文翻译（含 copilot/forms/reports/settings 等） |
| `i18n/locales/zh-CN.ts` | `src/i18n/locales/` | 前端中文翻译 |
| `api/client.ts` | `src/api/` | API 客户端配置（token/tenant 注入） |
| `useServerState.ts` | `composables/` | TanStack Query 封装（useGet/usePost/useDelete） |
| `useCopilotChat.ts` | `composables/` | SSE 流式聊天 + abort + unmount 清理 |
| `CrdtSyncEngine.ts` | `composables/` | Yjs CRDT 协同 |
| `AICopilotPanel.vue` | `components/copilot/` | 浮动 AI 助手 + Markdown 渲染 + SSE 打字机效果 |
| `BpmnViewer.vue` | `components/workflow/` | bpmn-js 封装 + 生命周期管理 |
| `DynamicForm.vue` | `components/form/` | 动态表单 + ReDoS 防护 |
| `FormFieldRenderer.vue` | `components/form/` | 字段渲染器 |
| `DataTable.vue` | `components/data/` | 数据表格 + 骨架屏 + 搜索排序分页 |
| `LoginView.vue` | `views/` | 登录表单 + i18n |
| `LayoutView.vue` | `views/` | Header + Nav + router-view + AICopilotPanel |
| `DashboardView.vue` | `views/` | 统计卡片 |
| `FormsView.vue` | `views/` | 表单管理 + DataTable + DynamicForm 预览 |
| `ReportsView.vue` | `views/` | 报表管理 + DataTable + 模拟数据预览 |
| `WorkflowsView.vue` | `views/` | 工作流管理 + BPMN 图表预览 |
| `SettingsView.vue` | `views/` | 3 Tab 设置（Profile + BYOK + Theme） |
| `form.ts` | `types/` | FormField + FormSchema 类型 |
| `env.d.ts` | `/` | Vite 环境变量类型 |
| `package.json` | `/` | 依赖声明（含 markdown-it, bpmn-js） |
| `tsconfig.json` | `/` | TypeScript 配置 |
| `saas-console.spec.ts` | `e2e/` | Playwright E2E（导航 + Copilot + 全视图） |

### 5.3 Skill 定义（13 个 YAML）

#### JeecgBoot 兼容 Skill（10 个）

| 文件 | 路径 | 类别 | 别名 |
|------|------|------|------|
| `jeecg-codegen.yaml` | `skills/` | 代码生成 | form_generator, skill_form_generator |
| `jeecg-bpmn.yaml` | `skills/` | 流程设计 | workflow_designer |
| `jeecg-onlform.yaml` | `skills/` | Online 表单 | table_designer |
| `jeecg-onlreport.yaml` | `skills/` | Online 报表 | report_builder |
| `jeecg-onlchart.yaml` | `skills/` | Online 图表 | chart_generator |
| `jeecg-desform.yaml` | `skills/` | 设计器表单 | — |
| `jeecg-system.yaml` | `skills/` | 系统管理 | config_manager, skill_config_manager |
| `jimubi-dashboard.yaml` | `skills/` | 仪表板 | dashboard_designer |
| `jimubi-bigscreen.yaml` | `skills/` | 大屏展示 | — |
| `jimureport.yaml` | `skills/` | 积木报表 | — |

#### 通用 Skill（3 个）

| 文件 | 路径 | 类别 |
|------|------|------|
| `api_designer.yaml` | `skills/` | 集成 |
| `permission_designer.yaml` | `skills/` | 安全 |
| `template_generator.yaml` | `skills/` | 文档生成 |

### 5.4 Prompt 模板（2 个）

| 文件 | 路径 | 功能 |
|------|------|------|
| `code_generator_v1.0.0.j2` | `skills/prompts/` | 代码生成 Prompt（Jinja2） |
| `form_generator_v1.0.0.j2` | `skills/prompts/` | 表单生成 Prompt（Jinja2） |

### 5.5 文档（14 个）

| 文件 | 路径 | 内容 |
|------|------|------|
| `README.md` | `/` | 项目主页（精简版架构） |
| `PROJECT_STRUCTURE.md` | `/` | 完整项目目录树 |
| `ARCHITECTURE.md` | `docs/` | 完整架构文档（本文档） |
| `ADR-001-virtual-threads.md` | `docs/adr/` | 虚拟线程选型决策 |
| `ADR-002-metadata-gitops.md` | `docs/adr/` | 元数据 GitOps 决策 |
| `ADR-003-ddd-boundaries.md` | `docs/adr/` | DDD 边界决策 |
| `ADR-004-defer-jpms.md` | `docs/adr/` | 延迟 JPMS 决策 |
| `PHASE0-review.md` | `docs/reviews/` | PHASE 0 审查报告 |
| `PHASE1-review.md` | `docs/reviews/` | PHASE 1 审查报告 |
| `PHASE2-review.md` | `docs/reviews/` | PHASE 2 审查报告 |
| `PHASE3-review.md` | `docs/reviews/` | PHASE 3 审查报告 |
| `PHASE4-review.md` | `docs/reviews/` | PHASE 4 审查报告 |
| `FULL-PROJECT-review.md` | `docs/reviews/` | 全项目审查报告 |
| `SPRING-BOOT-3.5-upgrade.md` | `docs/reviews/` | 升级报告 |

### 5.6 MCP Server Adapter（4 个文件）

| 文件 | 路径 | 功能 |
|------|------|------|
| `McpServerConfig.java` | `adapter/mcp/` | MCP 服务器配置（SSE 传输层自动配置） |
| `McpSecurityFilter.java` | `adapter/mcp/` | JWT 认证过滤器（/mcp/* 端点保护） |
| `AuroraSkillToolProvider.java` | `adapter/mcp/` | 10 个 Skill → MCP ToolCallback 桥接 |
| `OpencliToolProvider.java` | `adapter/mcp/` | opencli web_search/web_fetch → MCP ToolCallback |

### 5.7 数据库迁移（3 个 SQL 文件）

| 文件 | 路径 | 内容 |
|------|------|------|
| `V1__init_core_schema.sql` | `src/main/resources/db/migration/` | 11 张核心表（tenant, sys_user, sys_role, metadata, skill_registry, audit_log, abac_policy, skill_execution_log, sys_config） |
| `V2__init_jeecg_compat_tables.sql` | `src/main/resources/db/migration/` | 8 张 JeecgBoot 兼容表（jeecg_onl_form, jeecg_onl_report, jeecg_bpmn_process, jeecg_bpmn_instance, jeecg_desform, jimubi_dashboard, jimubi_bigscreen, jimureport_template） |
| `V3__init_sample_data.sql` | `src/main/resources/db/migration/` | 种子数据（默认租户、管理员、3 角色、13 Skills、7 系统配置、审计链锚点） |

### 5.8 集成测试（5 个文件，53 个用例）

| 文件 | 路径 | 测试数 | 内容 |
|------|------|--------|------|
| `AIPipelineIntegrationTest.java` | `src/test/java/com/aurora/` | 8 | 全流水线 + 并行验证 + 回滚 + 异步 + 并发 |
| `MultiTenantIsolationTest.java` | `src/test/java/com/aurora/` | 10 | 租户隔离 + 配额 + 生命周期 + 并发虚拟线程 |
| `SkillExecutionTest.java` | `src/test/java/com/aurora/` | 12 | YAML 加载 + 别名路由 + 热重载 + JeecgCompat |
| `AuditChainIntegrityTest.java` | `src/test/java/com/aurora/` | 11 | 哈希链 + 篡改检测 + 导出 + 并发 |
| `CodeGeneratorSecurityTest.java` | `src/test/java/com/aurora/` | 12 | 路径穿越 + XSS + SQL 注入 + TypeScript 映射 |

### 5.9 部署与 CI/CD（15 个文件）

| 文件 | 路径 | 内容 |
|------|------|------|
| `Dockerfile.prod` | `/` | 多阶段构建、非 root UID 1000、ZGC、Tini、健康检查 |
| `docker-compose.prod.yml` | `/` | Aurora + PostgreSQL 17 + Redis 7 + MinIO（仅应用端口暴露） |
| `Makefile` | `/` | 18 个目标（dev/test/build/docker/deploy/verify/clean） |
| `BOOTSTRAP.md` | `/` | 完整启动指南（环境要求、快速启动、故障排查） |
| `scripts/verify.sh` | `/` | 自动化验证（健康/API/数据库/Redis/Skill/MCP/安全/审计链） |
| `.github/workflows/ci.yml` | `/` | CI 流水线（构建+测试+SpotBugs+OWASP+JaCoCo+Trivy+SBOM） |
| `.github/workflows/cd.yml` | `/` | CD 流水线（tag 触发→GHCR 推送+Helm OCI+K8s 部署） |
| `Chart.yaml` | `deploy/helm/aurora/` | Helm Chart 元数据 |
| `values.yaml` | `deploy/helm/aurora/` | Helm 默认值（副本/资源/探针/HPA/数据库配置） |
| `deployment.yaml` | `deploy/helm/aurora/templates/` | K8s Deployment（滚动更新+Secret 引用+安全上下文） |
| `service.yaml` | `deploy/helm/aurora/templates/` | K8s Service（ClusterIP） |
| `ingress.yaml` | `deploy/helm/aurora/templates/` | K8s Ingress（Nginx + cert-manager TLS） |
| `configmap.yaml` | `deploy/helm/aurora/templates/` | K8s ConfigMap（application.yml 注入） |
| `secret.yaml` | `deploy/helm/aurora/templates/` | K8s Secret（DB/Redis/JWT 凭证，existingSecret 条件跳过） |
| `hpa.yaml` | `deploy/helm/aurora/templates/` | K8s HPA（CPU 70% / Memory 80%） |

---

## 六、安全体系

### 6.1 零信任多租户

| 层级 | 隔离方式 | 实现 |
|------|----------|------|
| 网络层 | HTTPS + CORS 白名单 | `application.yml` |
| 应用层 | 租户上下文 | `TenantContext` |
| 数据层 | Schema/RLS/Database | `TenantLifecycleManager` |
| 审计层 | 租户 ID 强制记录 | 所有 AuditEntry |

### 6.2 MCP Server 暴露层（AI 客户端接入）

```
外部 AI Client (Cursor/Claude Desktop/企业智能体)
    │
    ├── 1. GET /sse?token=<jwt> — 建立 SSE 连接
    │
    ├── 2. POST /mcp/message — 发送 Tool Call 请求
    │
    └── 3. 通过 SSE 接收响应
```

**安全要求**：
- 所有 `/mcp/*` 端点必须携带 JWT Token
- HTTP Header: `Authorization: Bearer <jwt-token>`
- `McpSecurityFilter` 校验 JWT 并提取 tenantId/userId
- 无有效 Token 返回 401 Unauthorized

**工具注册**：
- `AuroraSkillToolProvider` 自动注册 10 个内置 Skill 为 MCP Tools
- `OpencliToolProvider` 注册 `web_search`（联网搜索）和 `web_fetch`（网页抓取）工具
- Spring Boot Auto-Config 处理 SSE 传输层和端点路由

**联网搜索**：启用 `aurora.search.enabled=true`（`SEARCH_ENABLED` 环境变量）后，MCP 客户端可通过 `web_search` 和 `web_fetch` 工具接入互联网，底层由 opencli 执行搜索与内容抓取。

### 6.3 权限体系（RBAC + ABAC）

```
PermissionChecker
    ├── RBAC: 角色 → 权限
    ├── ABAC: subject + resource + action + env
    ├── 字段级：@Mask 注解运行时脱敏
    └── 行级：DataPermissionScope
```

### 6.3 安全防护矩阵

| 威胁 | 防护措施 | 实现位置 |
|------|----------|----------|
| SQL 注入 | PreparedStatement + 列名白名单 + 操作符白名单 | `ReportRuntimeEngine.java` |
| XSS | `escapeHtml()` + Vue 自动转义 | `CodeGenerator.java` |
| 路径穿越 | `Path.normalize()` + `startsWith()` | `CodeGenerator.java` |
| ReDoS | 正则复杂度校验（长度 200 + 嵌套量词检测） | `FormRuntimeEngine.java` |
| CSRF | CSRF Token 注入 | `useServerState.ts` |
| 敏感数据泄露 | @Mask 脱敏 | `DataMaskingInterceptor.java` |
| 审计篡改 | SHA-256 哈希链 | `ImmutableAuditLogger.java` |
| 服务雪崩 | 熔断器 + 重试 + 舱壁 | `ResilienceConfig.java` |
| 租户越权 | 强制 WHERE tenant_id = ? | 所有 Repository 方法 |

---

## 七、前端架构

### 7.1 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | 3.5 | 响应式 UI 框架 |
| TypeScript | 5.5 | 类型安全 |
| Vite | 6 | 构建工具 |
| UnoCSS | 0.63 | 原子化 CSS |
| TanStack Query | 5.60 | 服务端状态管理 |
| Yjs | 13.6 | CRDT 协同编辑 |
| axe-core | 4.10 | 无障碍扫描 |

### 7.2 Design Token 系统

4 主题模式：`light` / `dark` / `high-contrast` / `colorblind`
运行时切换 < 50ms，WCAG 2.1 AA 合规。

### 7.3 性能优化

| 策略 | 效果 |
|------|------|
| 路由级代码分割 | 首屏 LCP < 2.0s |
| AVIF → WebP 自动降级 | 体积减少 60%+ |
| gzip + brotli 压缩 | 体积减少 70%+ |
| hover 预加载 | 100ms 后预取路由 |
| 虚拟滚动 | >50 行列表流畅滚动 |

---

## 八、部署与工程化

### 8.1 快速启动

```bash
# 方式一：Makefile（推荐）
make dev                # 启动 PostgreSQL + Redis
mvn spring-boot:run     # 启动后端（新终端）

# 方式二：Docker Compose 生产模式
export DATABASE_PASSWORD=your_secret
export REDIS_PASSWORD=your_redis_secret
export JWT_SECRET=your_jwt_secret_min_32_chars
export OSS_ACCESS_KEY_ID=your_oss_access_key_id
export OSS_ACCESS_KEY_SECRET=your_oss_access_key_secret
make docker-up          # 一键启动 Aurora + PostgreSQL + Redis + OSS

# 方式三：完整验证
./scripts/verify.sh     # 检查所有端点、Skill、数据库、审计链
```

### 8.2 环境变量

| 变量 | 默认值 | 必填(生产) | 说明 |
|------|--------|:----------:|------|
| `DATABASE_HOST` | localhost | 是 | PostgreSQL 地址 |
| `DATABASE_PASSWORD` | — | **是** | 数据库密码 |
| `REDIS_HOST` | localhost | 是 | Redis 地址 |
| `REDIS_PASSWORD` | — | **是** | Redis 密码 |
| `JWT_SECRET` | — | **是** | JWT 签名密钥（≥32 字符） |
| `SERVER_PORT` | 8080 | 否 | HTTP 端口 |
| `SPRING_PROFILES_ACTIVE` | dev | 否 | 运行环境 |

### 8.3 生产部署

#### Docker Compose
```bash
# 设置必要的环境变量
export DATABASE_PASSWORD=your_secret
export REDIS_PASSWORD=your_redis_secret
export JWT_SECRET=your_jwt_secret
export OSS_ACCESS_KEY_ID=your_oss_access_key_id
export OSS_ACCESS_KEY_SECRET=your_oss_access_key_secret
export GRAFANA_PASSWORD=your_grafana_password

docker compose -f docker-compose.prod.yml up -d
```
**生产栈**：Aurora + PostgreSQL 17 + Redis 7 + OSS + Prometheus + Grafana + Tempo + Loki + Promtail
- 仅暴露应用端口（8080）+ 可观测性端口（9090/3000）
- PostgreSQL/Redis/Loki 仅内部网络可达
- 健康检查 + 资源限制 + 优雅停机（1m timeout）

#### Kubernetes (Helm)
```bash
helm upgrade --install aurora ./deploy/helm/aurora \
  --namespace aurora --create-namespace \
  --set image.tag=1.0.0 \
  --set jwtSecret=your_secret \
  --set database.password=your_db_password \
  --set redis.password=your_redis_password
```
- 滚动更新（maxSurge=1, maxUnavailable=0）
- HPA 自动扩缩（2-10 副本）
- 非 root 用户 + 只读根文件系统 + 能力降权

### 8.4 CI/CD 流水线

| 流水线 | 触发 | 步骤 |
|--------|------|------|
| **CI** | push/PR to main | 5 个并行 job：build（编译+测试）→ spotbugs（0 bugs）→ security（NVD CVSS≥7，OSS Index disabled）→ coverage（JaCoCo ≥10%）→ docker（镜像+Trivy v0.36.0+SBOM） |
| **CD** | tag push (v*) | 构建 → 推送 GHCR → Helm Chart OCI → K8s 部署（可选） |

### 8.5 依赖治理

| 插件 | 用途 |
|------|------|
| `dependency-check-maven` | OWASP 漏洞扫描，CVSS ≥ 7 阻断，OSS Index Analyzer 已禁用（401 错误） |
| `versions-maven-plugin` | 依赖版本检测 |
| `JaCoCo 0.8.13` | 覆盖率 ≥ 10%（指令+分支），TODO 提升至 80% |
| `SpotBugs` | 静态分析，0 bugs |

---

## 九、版本与审查记录

### 9.1 版本升级记录

| 日期 | 变更 | 原因 |
|------|------|------|
| 2026-05-04 | Spring Boot 3.4.0 → **3.5.0** | 官方支持 Java 25 LTS |
| 2026-05-04 | Spring AI 1.0.0-M4 → **1.0.0-M6** | 兼容 Spring Boot 3.5 |
| 2026-05-04 | Micrometer 1.14.2 → **1.15.0** | Spring Boot 3.5 管理版本 |
| 2026-05-04 | OpenTelemetry 1.45.0 → **1.46.0** | 安全更新 |
| 2026-05-04 | Resilience4j 2.2.0 → **2.3.0** | Java 25 兼容性修复 |
| 2026-05-04 | Flyway 10.22.1 → **11.3.1** | 主版本升级 |
| 2026-05-04 | Mockito 5.14.2 → **5.16.0** | Java 25 兼容性修复 |

### 9.2 审查记录

| 报告 | 状态 | 发现 |
|------|------|------|
| PHASE 0 审查 | ✅ 已修复 | 3 CRITICAL + 5 HIGH |
| PHASE 1 审查 | ✅ 已修复 | 3 CRITICAL + 4 HIGH |
| PHASE 2 审查 | ✅ 已修复 | 3 HIGH + 6 MEDIUM |
| PHASE 3 审查 | ✅ 已修复 | 2 HIGH |
| PHASE 4 审查 | ✅ 已修复 | 3 CRITICAL + 4 HIGH |
| 全项目审查 | ✅ 已修复 | 1 CRITICAL + 3 HIGH |
| PHASE 4 二次审查 | ✅ 已修复 | 1 CRITICAL + 3 HIGH |
| **PHASE 5 TASK 1** | ✅ 已修复 | 2 HIGH + 2 MEDIUM（旧 YAML 路由失效、别名 O(n) 清理） |
| **PHASE 5 TASK 2** | ✅ 已修复 | 1 CRITICAL + 1 HIGH + 3 MEDIUM（Genesis hash 缺位、密码注释不匹配、TIMESTAMP 缺时区） |
| **PHASE 5 TASK 3** | ✅ 已修复 | 3 CRITICAL + 4 HIGH（append() 竞态、安全测试未注入、篡改测试未篡改） |
| **PHASE 5 TASK 4** | ✅ 已修复 | 4 CRITICAL + 6 HIGH（危险默认密钥、端口暴露、MinIO latest、CMD exec 形式） |
| **PHASE 5 TASK 5&6** | ✅ 已修复 | 2 CRITICAL + 4 HIGH（JaCoCo 属性不存在、coverage 重复编译、make dev 误导） |
| **PHASE 6** | ✅ 已修复 | 3 CRITICAL + 7 HIGH + 2 MEDIUM（TenantContext 无实现、Hibernate Filter 死代码、Token 黑名单未检查、CSP 样式等） |
| **PHASE 7** | ✅ 已修复 | 3 CRITICAL + 6 HIGH + 1 MEDIUM（ThreadLocal 测试冲突、CRDT 负载丢弃、synchronized I/O、WebSocket CORS、i18n 硬编码等） |

---

## 十、扩展指南

### 10.1 扩展 AI 模型

在 `application.yml` 中添加 LLM 提供商：

```yaml
spring:
  ai:
    custom-model:
      api-key: ${CUSTOM_MODEL_API_KEY}
      base-url: https://api.your-model.com
```

### 10.2 扩展 Skill

复制 Skill 模板并修改 `input_schema` / `output_schema`：

```bash
cp skills/form_generator.yaml skills/my_custom_skill.yaml
# 修改后重启自动注册
```

### 10.3 扩展数据源

实现 `MetadataRepository` 接口即可接入新的元数据存储（MongoDB、Etcd 等）。

### 10.4 扩展主题

在 `tokens.css` 中添加新的 `[data-theme="xxx"]` 区块。

### 10.5 扩展权限规则

在 `AbacPolicyEngine.java` 中注册新规则：

```java
engine.registerRules("report", List.of(
    new PatternRule("...", "report must have owner", Pattern.compile("..."), true)
));
```

---

## 十一、PHASE 5 生产交付

### 11.1 JeecgBoot 技能对齐

将通用 Skill 重命名为 JeecgBoot 兼容命名空间，确保用户可无缝迁移：

| 旧名称 | 新名称 | 功能 |
|--------|--------|------|
| `form_generator` | `jeecg-codegen` | 代码生成 + 建表 SQL |
| `workflow_designer` | `jeecg-bpmn` | BPMN 2.0 XML 生成 |
| `table_designer` | `jeecg-onlform` | Online 表单配置 |
| `report_builder` | `jeecg-onlreport` | Online 报表配置 |
| `chart_generator` | `jeecg-onlchart` | 在线图表 |
| `dashboard_designer` | `jimubi-dashboard` | 仪表板 |
| `config_manager` | `jeecg-system` | 系统管理 |

新增 Skill：
- `jeecg-desform` — 设计器表单 JSON 生成
- `jimubi-bigscreen` — 大屏展示
- `jimureport` — 积木报表兼容层

### 11.2 别名路由机制

`SkillDefinitionLoader` 新增：
- `aliases` 字段（List<String>）— 旧名称列表
- `jeecgCompat` 字段（boolean）— JeecgBoot 兼容标志
- `aliasToSkillId` 映射 — 别名→规范 ID 路由表
- `skillIdToAliases` 反向索引 — O(1) 别名清理
- `resolveAlias()` 方法 — 别名解析
- `getJeecgCompatSkills()` 方法 — 过滤 JeecgBoot 技能

```
调用方请求旧名称 "form_generator"
    │
    ▼
SkillRouter.findBestSkill("form_generator")
    │
    ▼
loader.resolveAlias("form_generator") → "jeecg-codegen"
    │
    ▼
loader.loadById("jeecg-codegen") → 返回新 Skill 定义
```

### 11.3 数据库 Schema

#### V1 — 核心表（11 张）

| 表名 | 用途 |
|------|------|
| `tenant` | 租户管理（tier/isolation_mode/quota） |
| `sys_user` | 用户（含 force_password_change 标志） |
| `sys_role` | 角色（JSONB 权限） |
| `sys_user_role` | 用户-角色关联 |
| `metadata` | 元数据注册表 |
| `metadata_version` | 元数据版本历史 |
| `skill_registry` | Skill 注册表 |
| `audit_log` | 审计日志（SHA-256 哈希链，prev_hash） |
| `abac_policy` | ABAC 策略 |
| `skill_execution_log` | Skill 执行日志 |
| `sys_config` | 系统配置 |

#### V2 — JeecgBoot 兼容表（8 张）

| 表名 | 用途 |
|------|------|
| `jeecg_onl_form` | Online 表单配置 |
| `jeecg_onl_report` | Online 报表配置 |
| `jeecg_bpmn_process` | BPMN 流程定义 |
| `jeecg_bpmn_instance` | BPMN 流程实例 |
| `jeecg_desform` | 设计器表单 |
| `jimubi_dashboard` | JimuBI 仪表板 |
| `jimubi_bigscreen` | JimuBI 大屏 |
| `jimureport_template` | 积木报表模板 |

### 11.4 测试覆盖

| 测试类 | 用例数 | 覆盖范围 |
|--------|--------|----------|
| AIPipelineIntegrationTest | 8 | 全流水线 + 并行验证(StructuredTaskScope) + 回滚 + 异步 |
| MultiTenantIsolationTest | 10 | 租户隔离 + 配额 + 生命周期 + 并发虚拟线程 |
| SkillExecutionTest | 12 | YAML 解析 + 别名路由 + 热重载 + 重复检测 |
| AuditChainIntegrityTest | 11 | Genesis 锚点 + 哈希链 + 篡改检测 + 导出 |
| CodeGeneratorSecurityTest | 12 | 路径穿越 + XSS + SQL 注入 + TypeScript 映射 |

### 11.5 部署架构

```
┌─────────────────────────────────────────────────────┐
│                    Ingress (Nginx)                    │
│                  TLS (cert-manager)                   │
└──────────────────────┬──────────────────────────────┘
                       │
              ┌────────▼────────┐
              │    Service       │
              │   (ClusterIP)    │
              └────────┬────────┘
                       │
        ┌──────────────┼──────────────┐
        │              │              │
  ┌─────▼─────┐ ┌─────▼─────┐ ┌─────▼─────┐
  │ Aurora Pod │ │ Aurora Pod │ │ Aurora Pod │  ← HPA 2-10 副本
  │  (ZGC)     │ │  (ZGC)     │ │  (ZGC)     │
  └─────┬─────┘ └─────┬─────┘ └─────┬─────┘
        │             │             │
        └─────────────┼─────────────┘
                      │
        ┌─────────────┼─────────────┐
        │             │             │
  ┌─────▼─────┐ ┌─────▼─────┐ ┌─────▼─────┐
  │PostgreSQL │ │   Redis   │ │   MinIO   │
  │  (SCHEMA) │ │  (Cache)  │ │  (S3)     │
  └───────────┘ └───────────┘ └───────────┘
```

### 11.6 CI/CD 流水线

```
push/PR to main                        tag push (v*)
      │                                      │
      ▼                                      ▼
┌──────────────────┐                  ┌──────────────┐
│  Build (compile)  │                  │  Build       │
│  + Unit Tests     │                  │  + Test      │
│  + Integration    │                  └──────┬───────┘
└────────┬─────────┘                          │
         │                           ┌────────▼───────┐
    ┌────┼────┐                      │  Push GHCR     │
    │    │    │                      │  + Helm OCI    │
    ▼    ▼    ▼                      └───────┬────────┘
 spotbugs security coverage                  │
  (0 bugs) (NVD)   (JaCoCo           ┌──────▼────────┐
    │    │    │      ≥10%)            │  K8s Deploy   │
    └────┼────┘                      │  (optional)   │
         │                           └───────────────┘
         ▼
  ┌──────────────┐
  │  Docker Build │
  │  + Trivy Scan │
  │  + SBOM       │
  └──────────────┘
```

---

## 十二、PHASE 6 核心运行时与安全引导

### 12.1 JWT 鉴权闭环

```
HTTP Request
    │
    ├── Header: Authorization: Bearer <jwt>
    │
    ▼
JwtAuthenticationFilter
    │ 1. 提取 token
    │ 2. JwtTokenProvider.validateToken() — HS256 签名验证
    │ 3. AuthController.isTokenBlacklisted() — 登出检查
    │ 4. 提取 userId/tenantId/roles
    │
    ├── Spring SecurityContext → UsernamePasswordAuthenticationToken
    ├── TenantContext → ScopedValue + ThreadLocal
    │
    ▼
SecurityFilterChain
    │ /auth/login → permitAll（无认证）
    │ /api/** → authenticated（需 JWT）
    │ /mcp/** → McpSecurityFilter（独立 JWT 验证）
    │
    ▼
Controller → Service → Repository（@Filter 自动注入 tenant_id）
```

**密钥管理**：
- JWT Secret：`aurora.security.jwt-secret`（环境变量 `JWT_SECRET`，≥32 字节）
- 签名算法：HMAC-SHA256（HS256）
- 令牌有效期：1 小时（可配置 `aurora.security.jwt-expiration`）
- 密码加密：BCryptPasswordEncoder

### 12.2 JPA 持久层

| Entity | 表 | 关键特性 |
|--------|-----|----------|
| `MetadataEntity` | `metadata` | JSONB content + @Filter tenant isolation + @Version 乐观锁 |
| `TenantEntity` | `tenant` | tier/isolation_mode/quota + 软删除 |
| `SkillRegistryEntity` | `skill_registry` | JSONB input/output schema + jeecg_compat |
| `AuditChainEntity` | `audit_log` | entryHash/prevHash 哈希链 + seqNum |

**多租户过滤**：
- `@FilterDef(name="tenantFilter")` + `@Filter(condition="tenant_id=:tenantId")`
- `TenantFilterInterceptor` — 在每个 /api/** 请求前启用 Hibernate filter
- `TenantFilterConfigurer` — 操作 Hibernate Session 的 enableFilter/disableFilter

### 12.3 Spring AI LLM 接入

```
AiSelfCorrectionLoop.requestCorrection()
    │
    ▼
LlmGatewayService.call(prompt)
    │
    ├── StructuredTaskScope（30s 超时）
    │   └── LlmProviderRouter.selectProvider()
    │       ├── Primary: Anthropic Claude（默认）
    │       └── Fallback: OpenAI GPT-4o（熔断后切换）
    │
    ├── retryWithBackoff(prompt, maxRetries)
    │   └── 指数退避：500ms → 1s → 2s
    │
    └── costEstimation(provider, tokensIn, tokensOut)
        ├── Anthropic: $3/M input, $15/M output
        └── OpenAI: $2.5/M input, $10/M output
```

**测试模式**：`@Profile("test")` + `MockLlmGateway` 返回固定 JSON，避免 CI 调用真实 API。

### 12.4 Vue3 前端

| 模块 | 文件 | 功能 |
|------|------|------|
| 入口 | `main.ts` | Vue3 + Pinia + Router 挂载 |
| 布局 | `App.vue` | 主题切换 + 全局 Loading |
| 路由 | `router/index.ts` | 懒加载 + auth 守卫 + redirect 参数 |
| 状态 | `stores/auth.ts` | JWT 解码 + 过期检查 + login/logout |
| 状态 | `stores/tenant.ts` | 租户 ID 管理 |
| 拦截 | `plugins/api-interceptor.ts` | fetch 拦截：自动注入 Authorization + X-Tenant-Id |
| 视图 | `views/LoginView.vue` | 登录表单 + 错误处理 |
| 视图 | `views/LayoutView.vue` | Header + Nav + router-view |
| 视图 | `views/DashboardView.vue` | 统计卡片 |

### 12.5 测试覆盖

| 测试 | 用例数 | 覆盖范围 |
|------|--------|----------|
| JwtTokenProviderTest | 13 | 生成/验证/提取/过期/边界 |
| LlmGatewayServiceTest | 5 | 成本估算/熔断状态 |
| auth.spec.ts | Vitest | token 设置/过期/清除 |
| router-guard.spec.ts | Vitest | 路由守卫 |

---

## 十三、PHASE 7 架构治理与企业级补全

### 13.1 ArchUnit 架构守护

```
ArchitectureTest
    ├── layeredArchitecture(): adapter → application → domain ← infrastructure
    ├── domainShouldNotDependOnInfrastructure()
    ├── applicationShouldNotUseJpaRepository()
    └── contractShouldNotDependOnInfrastructure()

LayerDependencyTest
    ├── domainShouldNotDependOnInfrastructure/Adapter()
    ├── applicationShouldNotAccessJpaRepositories/Adapter()
    └── infrastructureShouldNotDependOnAdapter/Application()

Java25RedLineTest
    ├── noThreadLocalUsage() — 禁止 ThreadLocal
    └── noCompletableFutureUsage() — 禁止 CompletableFuture

NamingConventionTest
    ├── entitiesShouldBeInCorrectPackage() — *Entity → database.entity
    ├── controllersShouldBeInCorrectPackage() — *Controller → adapter.web
    ├── filtersShouldBeInAdapterLayer() — *Filter → adapter/config
    └── configClassesShouldBeInCorrectPackage() — *Config → infrastructure.config

OpenApiAnnotationTest
    ├── controllerMethodsMustHaveOperationAnnotation()
    └── controllerMethodsMustHaveApiResponseAnnotation()
```

### 13.2 I18N 国际化

```
HTTP Request
    │
    ├── Header: Accept-Language: zh-CN
    │
    ▼
I18nController
    │ GET /api/v1/i18n/zh-CN
    │ → MessageSource.getMessage("auth.login.failed", Locale.SIMPLIFIED_CHINESE)
    │
    ▼
Response: { "auth.login.failed": "用户名或密码错误", ... }
```

**前端**：vue-i18n + navigator.language 自动检测 + localStorage 持久化

### 13.3 邮件通知

```
EmailNotificationService.sendTemplateEmail(to, subject, template, vars)
    │
    ├── StructuredTaskScope（15s 超时）
    │   └── JavaMailSender.send()
    │
    ├── Thymeleaf TemplateEngine.process("email/" + template)
    │   └── 变量注入 → HTML 渲染
    │
    └── AuditLogger.logCustom(type=EMAIL_SENT, traceId, recipient, status)
```

### 13.4 WebSocket 协同

```
Client (y-websocket)
    │
    ├── GET /ws/collaborate?documentId=xxx&token=jwt
    │   └── WebSocketAuthInterceptor → JWT 验证 → tenantId/userId/documentId
    │
    ├── Binary WebSocket Connection
    │   └── YjsWebSocketHandler
    │       ├── MSG_SYNC (0) → 广播同步数据
    │       └── MSG_AWARENESS (1) → 广播光标/选区
    │
    └── DocumentRoomManager
        ├── ConcurrentHashMap<"tenantId:documentId", CopyOnWriteArraySet<Session>>
        ├── joinRoom() / leaveRoom() / broadcastToRoom()
        └── 定时清理空房间（60s）
```

### 13.5 联网搜索（opencli）

```
AI Pipeline / MCP Client
    │
    ├── tool: web_search(query, limit)
    │   → OpencliToolProvider.SearchToolCallback
    │   → WebSearchService.search()
    │   → opencli smart-search <query> -f json
    │   → StructuredTaskScope (30s 超时)
    │
    └── tool: web_fetch(url)
        → OpencliToolProvider.FetchToolCallback
        → WebSearchService.fetch()
        → opencli web read <url> -f plain
        → StructuredTaskScope (15s 超时)
```

**启用方式**：设置 `SEARCH_ENABLED=true`（默认关闭）。需要系统安装 opencli。
**安全**：调用走系统 subprocess，不暴露网络密钥。超时自动降级返回错误 JSON。

---

## 十四、PHASE 9 企业集成、可靠性 & V1.0.0 GA

### 14.1 外部 API Key 管理

为外部系统提供无头 API 访问能力。租户管理员通过 `POST /api/v1/apikeys` 创建 API Key，格式为 `aurora_sk_<44-char base64>`（256-bit 熵，SHA-256 哈希存储）。外部系统在 `X-API-Key` 头中携带密钥，`ApiKeyAuthenticationFilter` 查找哈希并注入 SecurityContext。

```
POST /api/v1/apikeys          → 创建 Key（返回明文一次）
GET  /api/v1/apikeys          → 列出所有 Key（仅元数据）
DELETE /api/v1/apikeys/{id}    → 吊销 Key
API Key 认证                   → /api/v1/external/** 端点
```

**安全特性**：
- SHA-256 哈希存储（不是 BCrypt — API Key 本身已是高熵随机值）
- 确定性哈希查找（无时序侧信道）
- 密钥过期后自动 status=EXPIRED
- 仅创建时返回明文，`warning` 字段提示安全保存

### 14.2 企业级 Webhook 引擎

事件驱动架构的核心组件。监听 DomainEvent（Created/Updated/Deleted/StatusChanged/Versioned/ExecutionEvent），向租户配置的外部 URL 推送事件。

```
DomainEvent → EventBus → WebhookDispatcher → 【并发扇出】
                  └── StructuredTaskScope (30s 超时)
                       └── virtual threads → HttpClient 发送
                            └── X-Aurora-Signature: sha256=<HMAC>
                            └── Resilience4j 重试（3 次，1s 退避）
```

**签名验证**（接收端）：
```java
WebhookSigner.verify(payload, secret, signature);  // constant-time equals
```

**端点管理**：
- `POST /api/v1/webhooks` — 创建（自动生成 secret）
- `GET /api/v1/webhooks` — 列表
- `PUT /api/v1/webhooks/{id}` — 更新
- `DELETE /api/v1/webhooks/{id}` — 删除
- `POST /api/v1/webhooks/{id}/regenerate-secret` — 重新生成密钥

### 14.3 JMH 性能基准 (T18 ✅)

```java
@BenchmarkMode(Mode.AverageTime)
@Param({"virtual", "fixed200", "fixed16", "cached"})
// 1000 × 200ms 模拟 I/O
```

| Executor 类型 | 耗时 | 说明 |
|---------------|------|------|
| VirtualThread | ~500ms | 全部 1000 任务 I/O 重叠 |
| FixedPool(200) | ~1000ms | 5 波 200 个 |
| FixedPool(16) | ~13000ms | 63 波 16 个 |
| CachedPool | ~1000ms | 创建 1000 个平台线程 |

运行方式：`scripts\run-benchmark.bat` 或 `mvn exec:java -Dexec.mainClass="com.aurora.benchmark.BenchmarkRunner" -Dexec.classpathScope=test`

### 14.4 混沌工程验证

**依赖**：`chaos-monkey-spring-boot:3.1.0`（仅 test scope）+ `application-chaos.yml`（仅 @ActiveProfiles("chaos")）

**测试**：`ChaosResilienceIntegrationTest.java`
1. 激活 Chaos Monkey LatencyAssault（2-5s 延迟）
2. 发送 20 个并发请求 → 超过熔断阈值
3. 断言 CircuitBreaker 状态 ⊕ OPEN
4. 断言后续请求立即被拒绝（< 100ms）

**Resilience4j 配置**（`application.yml`）：
- `llmGateway`：滑动窗口 10，最小调用 3，失败率阈值 50%，打开状态等待 10s
- `webhookDispatcher`：重试 3 次，间隔 1s

---

## 附录：完整统计

| 指标 | 数值 |
|------|------|
| Java 后端文件 | 80 |
| 前端文件 | 34 |
| Skill YAML | 13（10 JeecgBoot + 3 通用） |
| Flyway 迁移 | 5（V1-V5） |
| 集成测试 | 6（55+ 个用例，含 ChaosResilience） |
| 单元测试 | 9（38+ 个用例：ArchUnit 20 + JwtTokenProvider 13 + LlmGateway 5 + JMH 1 + Chaos 1） |
| E2E 测试 | 3（login-flow + dashboard-render + saas-console 7 tests） |
| JMH 基准 | 2（VirtualThreadVsPlatformThread + Runner） |
| Docker/K8s | 12（Dockerfile + compose + Helm Chart） |
| CI/CD | 2（ci.yml 6 jobs + cd.yml） |
| 工程化 | 6（Makefile + BOOTSTRAP.md + verify.sh + sync-api.sh + run-benchmark.sh/bat） |
| Nginx 配置 | 1（deploy/nginx-local.conf） |
| Prompt 模板 | 2 |
| ADR 文档 | 4 |
| 审查报告 | 12 |
| **总计** | **~175** |
### 代码统计

| 特性 | 数量 |
|------|------|
| Sealed Interfaces | 7（63 个 permits） |
| Record 类型 | 45+ |
| REST API 端点 | 30+（含 /auth/*, /api/v1/apikeys, /api/v1/webhooks, /api/v1/external/**） |
| WebSocket 端点 | 1（/ws/collaborate） |
| MCP 端点 | 2（/mcp/sse, /mcp/message）+ web_search/web_fetch 工具 |
| 数据库表 | 21（11 核心 + 8 JeecgBoot + 2 企业集成） |
| JPA Entity | 6（Metadata, Tenant, SkillRegistry, AuditChain, ApiKey, WebhookEndpoint） |
| JPA Repository | 6 |
| 安全规则 | 50+ |
| 业务规则 | 40+ |
| 测试用例 | 91（53 集成 + 36 单元 + 2 E2E） |
| I18N 语言 | 2（英文 + 简体中文） |
| 邮件模板 | 2（welcome + password-reset） |

### 编译状态

```
✅ BUILD SUCCESS
JDK: Eclipse Temurin 25.0.2 LTS
Spring Boot: 3.5.0
Spring Framework: 6.3.0
Spring Security: 6.5.0
SpotBugs: 0 bugs
ArchUnit: 1.4.0 (20 tests)
JaCoCo: 0.8.13（支持 JDK 25）
```
