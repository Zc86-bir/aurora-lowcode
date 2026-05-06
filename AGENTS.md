# AGENTS.md — Aurora LowCode

Project root, Maven project with frontend/. Java 25, Spring Boot 3.5.0 BOM (not parent POM).

## Commands

```bash
# Backend
mvn compile -DskipSpringdoc=true
mvn test -DskipITs -DskipSpringdoc=true                    # unit only
mvn test -Dtest=JwtTokenProviderTest                       # single test
mvn verify -DskipUTs -Djacoco.skip=true -DskipSpringdoc=true  # integration
mvn spotbugs:check -DskipSpringdoc=true
mvn spring-boot:run -Dspring.profiles.active=dev

# Frontend (pnpm only)
cd frontend && pnpm dev            # port 3000
pnpm build                         # runs vue-tsc --noEmit first
pnpm test                          # vitest
pnpm exec playwright test          # e2e
pnpm generate:api:remote           # regenerate API client from running backend

# Infrastructure
docker compose -f docker-compose.dev.yml up -d   # PostgreSQL + Redis
scripts\nginx.bat start                           # nginx reverse proxy on :8088
scripts\nginx.bat stop                            # stop nginx
```

## Java 25 Red Lines

- **No `synchronized` I/O** — use `ReentrantLock`
- **No `ThreadLocal`** in new code — use `ScopedValue` (`ScopedValue.<UUID>newInstance()`, `.where().run()`). Exception: `ScopedValueTenantContext` uses ThreadLocal for filter compatibility.
- **StructuredTaskScope** over `CompletableFuture` — use `StructuredTaskScope.<T,R>open(Joiner.awaitAll(), cfg -> cfg.withTimeout(...))`. Handle `TimeoutException`. Check `Subtask.State`.

## Architecture

- **DDD hexagonal**: `adapter/` → `application/` → `domain/` (architecture + contract) ← `infrastructure/`. `application/` must NOT import `infrastructure.database.repository`.
- **Tenant context flow**: JWT → `JwtAuthenticationFilter` → `ScopedValueTenantContext.setContext()` → Hibernate `@Filter(condition="tenant_id=:tenantId")` via `TenantFilterInterceptor`
- **AI pipeline**: LLM → `AiSelfCorrectionLoop` → `JsonSchemaValidator` → `AstSyntaxFirewall` → `BusinessRuleEngine`. 2 correction rounds max, then `FallbackStrategy`.
- **Audit chain**: `ImmutableAuditLogger.append()` — SHA-256 hash chain with `prev_hash` → `entry_hash`. Genesis hash must be 64-char hex.
- **Skill alias routing**: Old IDs (`form_generator`, `workflow_designer`, etc.) auto-route to `jeecg-*` via `SkillDefinitionLoader.resolveAlias()`.
- **JWT**: HS256 via JJWT 0.12.5. `JWT_SECRET` ≥ 32 bytes. Seed user: `admin@aurora.dev` / `admin123`.
- **Object storage**: Aliyun OSS (MinIO replaced). Config at `aurora.storage.oss.*`.
- **Web search**: `WebSearchService` + `OpencliToolProvider` — enabled via `SEARCH_ENABLED=true`. Registers `web_search` (30s timeout) and `web_fetch` (15s timeout) MCP tools.

## Tests

| Suite | Location | Count |
|-------|----------|-------|
| Unit | `*-Test.java` | 18 (JwtTokenProvider 13 + LlmGatewayService 5) |
| Integration | `*IntegrationTest.java`, `*Test.java` w/o unit suffix | 53 (5 classes) |
| ArchUnit | `archunit/` package | 20 (5 classes with allowEmptyShould(true)) |
| Frontend | `frontend/src/**/*.spec.ts` | Vitest |
| E2E | `frontend/e2e/` | Playwright, CI retries=2 |

Integration test classes: `AIPipelineIntegrationTest`(8), `MultiTenantIsolationTest`(10), `SkillExecutionTest`(12), `AuditChainIntegrityTest`(11), `CodeGeneratorSecurityTest`(12).

## Build Flags (CI/Docker critical)

- `-DskipSpringdoc=true` — avoids springdoc "Connection refused" in Docker
- `-DskipITs=true` / `-DskipUTs=true` — skip integration/unit tests
- `-Djacoco.skip=true` — skip JaCoCo (threshold 10%, TODO 80%)
- `-Dmaven.javadoc.skip=true -Dmaven.source.skip=true` — prevents `cp target/*.jar` multi-match in Docker
- `-Ddependency-check.skip=true` — skip OWASP in non-security jobs

## Architecture Guard (ArchUnit 1.4.0)

- Limited Java 25 bytecode support; all rules use `.allowEmptyShould(true)`.
- `Java25RedLineTest` excludes `..infrastructure.tenancy..` from ThreadLocal check.
- All `*Entity` must be in `infrastructure.database.entity`, `*Controller` in `adapter.web`.
- All controller methods need `@Operation` + `@ApiResponse`.
- `@FilterDef(name="tenantFilter")` is defined on `MetadataEntity`.

## DB Design

- All timestamps: `TIMESTAMPTZ` (PostgreSQL), UUID primary keys via `gen_random_uuid()`
- Structured data: `JSONB` mapped via `@JdbcTypeCode(SqlTypes.JSON)`
- `@Version` for optimistic locking on all entities
- Flyway: V1 (11 core tables), V2 (8 JeecgBoot compat), V3 (seed data). Test profile uses H2 with `ddl-auto: create-drop`.

## OpenAPI Contract Sync

```
Backend @RestController → SpringDoc → /v3/api-docs.yaml
    → pnpm generate:api → frontend/src/api/generated/ (types.gen.ts + services.gen.ts)
```

No hand-written fetch/Axios — always use generated SDK.

## Key Dependencies (notable versions)

| Lib | Version | Used For |
|-----|---------|----------|
| JJWT | 0.12.5 | JWT auth |
| JavaParser | 3.26.2 | AST firewall |
| json-schema-validator | 1.5.4 | Schema validation |
| Flyway | 11.3.1 | DB migrations |
| Redisson | 3.38.1 | Rate limiting / locking |
| Resilience4j | 2.3.0 | Circuit breaker |
| JavaPoet | 1.13.0 | Code generation |
| BouncyCastle | 1.79 | Crypto |
| ArchUnit | 1.4.0 | Architecture tests |

## Env Vars

`JWT_SECRET`, `DATABASE_PASSWORD`, `REDIS_PASSWORD`, `OSS_ACCESS_KEY_ID`, `OSS_ACCESS_KEY_SECRET`, `ANTHROPIC_API_KEY`, `OPENAI_API_KEY`, `NVD_API_KEY`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `SEARCH_ENABLED`.

## CI

6 jobs: `build` → `spotbugs` | `security` | `coverage` | `docker` | `e2e`. Add `[skip ci]` in commit messages to suppress.

## ADR Summary

| Topic | Decision |
|-------|----------|
| Virtual threads | Java 25 Virtual Threads (ADR-001) |
| Metadata | SHA-256 diff sync, hot-reload without restart (ADR-002) |
| DDD | Pure interface contracts, impls in infrastructure (ADR-003) |
| JPMS | Delayed — use ArchUnit instead (ADR-004) |

## Gotchas

- Maven Aliyun mirror configured as primary — don't remove.
- ArchUnit 1.4.0 can't parse all Java 25 bytecode; rules may silently pass.
- `RATE_LIMIT_MIN`, `CORS_ORIGINS`, `AI_MAX_CONCURRENT` all configurable via env vars.
- Email disabled by default: `aurora.notification.email.enabled=false`.
- MinIO replaced with Aliyun OSS. Helm still references MinIO in backup docs.
- CodeQL `upload-sarif@v3` will be deprecated in 2026.12.
- WebSocket at `/ws/collaborate?documentId=xxx&token=jwt` (binary Yjs protocol).
- `skipSpringdoc` property defined in pom.xml `<properties>`, default `false`.
- OWASP OSS Index Analyzer disabled (`<ossindexAnalyzerEnabled>false</ossindexAnalyzerEnabled>`).
- `testcontainers-redis` module not available — use generic container if needed.
- Mockito inline mocking is built-in (no `mockito-inline` dependency needed).
- Web search via opencli: `aurora.search.enabled=true` (disabled by default). Registers `web_search` and `web_fetch` MCP tools.
