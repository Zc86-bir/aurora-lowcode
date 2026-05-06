# AGENTS.md — Aurora LowCode

## Backend (Java 25 + Spring Boot 3.5.0 BOM)

- **JDK**: Eclipse Temurin 25.0.2 LTS with `--enable-preview` (required for `ScopedValue`, `StructuredTaskScope`)
- **Build**: Maven (not Gradle). Use `pom.xml` properties for version control.
- **Spring Boot**: BOM import scope, **not** parent POM.
- **Test skip flags** (critical for fast iteration in CI/Docker):
  - `-DskipSpringdoc=true` — skips springdoc-openapi (prevents "Connection refused" in Docker builds)
  - `-DskipITs=true` / `-DskipUTs=true` — skip integration or unit tests
  - `-Djacoco.skip=true` — skip JaCoCo
  - `-Dmaven.javadoc.skip=true -Dmaven.source.skip=true` — skip javadoc/source jars (prevents `cp target/*.jar` multi-match)
- **SpotBugs**: `spotbugs:check` in CI (must be 0 bugs). Exclusions in `src/main/resources/spotbugs-exclude.xml`.
- **OWASP**: `dependency-check-maven` 12.1.0. OSS Index Analyzer disabled (`<ossindexAnalyzerEnabled>false</ossindexAnalyzerEnabled>`). Requires `-Ddependency-check.skip=true` in non-security CI jobs.
- **JaCoCo**: threshold at 10% (instruction + branch), TODO to raise to 80%.
- **ArchUnit 1.4.0**: limited Java 25 bytecode support. All rules use `.allowEmptyShould(true)`.
- **Lombok**: used. Deprecation warning about `sun.misc.Unsafe` is expected (not an error).

## Java 25 Red Lines (must follow)

1. **No `synchronized` I/O** — use `ReentrantLock` for thread safety.
2. **No `ThreadLocal`** in new code — use `ScopedValue` for context propagation (`ScopedValue.<UUID>newInstance()`, `ScopedValue.where().run()`). Exception: `ScopedValueTenantContext` uses ThreadLocal fallback for filter compatibility.
3. **StructuredTaskScope** over `CompletableFuture` — use `StructuredTaskScope.<T,R>open(Joiner.awaitAll(), cfg -> cfg.withTimeout(...))`. Handle `TimeoutException` explicitly. Check `Subtask.State`.

## Architecture

- **DDD hexagonal**: `adapter/` → `application/` → `domain/` (architecture + contract) ← `infrastructure/`
- `application/` must NOT depend on `infrastructure.database.repository` (use contract interfaces)
- `contract/` defines sealed interfaces with 10+ permits
- `infrastructure/ai/LlmProviderRouter` — circuit breaker for Anthropic→OpenAI fallback
- `infrastructure/tenancy/ScopedValueTenantContext` — TenantContext implementation
- `adapter/web/AuthController` — login/logout. Seed: `admin@aurora.dev` / `admin123` (force_password_change=true)
- `adapter/security/JwtAuthenticationFilter` — validates JWT + checks token blacklist
- All entities in `infrastructure.database.entity`, all repositories in `infrastructure.database.repository`

## Tests

- **Unit**: `mvn test -DskipITs -Djacoco.skip=true -DskipSpringdoc=true`. Or: `mvn test -Dtest=JwtTokenProviderTest`.
- **Integration**: `mvn verify -DskipUTs -Djacoco.skip=true`. Full suite: 5 test classes (53 cases).
- **ArchUnit**: `mvn test -Dtest="ArchitectureTest,LayerDependencyTest,Java25RedLineTest,NamingConventionTest,OpenApiAnnotationTest"`
- **Frontend**: `cd frontend && pnpm vitest run`

## Frontend (Vue 3.5 + Vite 6)

- **Package manager**: pnpm (enforced via `only-allow pnpm`)
- **Commands**: `pnpm dev` (port 3000), `pnpm build` (runs `vue-tsc --noEmit` first), `pnpm test` (vitest)
- **API client**: auto-generated from OpenAPI via `pnpm generate:api` or `generate:api:remote`
- **API interceptor**: `src/plugins/api-interceptor.ts` — auto-injects `Authorization`, `X-Tenant-Id`, `Accept-Language`
- **Auth**: `src/stores/auth.ts` (Pinia) — JWT decode + expiry check
- **i18n**: `vue-i18n` with locale detection. Files in `src/i18n/locales/`
- **E2E**: Playwright in `e2e/` directory. Run: `pnpm exec playwright test`

## CI Workflows

- **ci.yml**: 6 jobs — `build`, `spotbugs`, `security`, `coverage`, `docker`, `e2e` (all depend on `build`)
- **cd.yml**: tag push (v*) → GHCR + Helm
- **opencode.yml**: OpenCode GitHub App (trigger via `/oc` on issues/PRs, model: `opencode-go/kimi-k2.6`)
- **test-secret.yml**: manual workflow to verify `NVD_API_KEY` is set
- Add `[skip ci]` to commit messages to suppress CI

## Common Gotchas

- Maven Aliyun mirror is configured as primary (faster in China). Don't remove.
- Flyway scripts in `src/main/resources/db/migration/`. H2 used in test profile (`ddl-auto: create-drop`).
- JBcrypt uses BCrypt for passwords. `BCryptPasswordEncoder` bean is in `SecurityFilterChainConfig`.
- `JWT_SECRET` env var must be ≥ 32 bytes for HS256.
- Docker build uses `maven:3.9-eclipse-temurin-25-alpine` (build) → `eclipse-temurin:25-jre-alpine` (runtime).
- MinIO was replaced with Aliyun OSS. Object storage config is at `aurora.storage.oss.*`.
- WebSocket endpoint at `/ws/collaborate?documentId=xxx&token=jwt` (binary Yjs protocol).
- Email notifications are disabled by default (`aurora.notification.email.enabled=false`).
