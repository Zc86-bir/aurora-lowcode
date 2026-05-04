# Aurora LowCode - Project Structure

> Enterprise-grade AI-driven Low-Code Platform based on Java 25 + Spring Boot 3.4 + Vue 3.5

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           AURORA-LOWCODE                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐ │
│  │   Frontend   │  │   REST API   │  │    Skills    │  │     AI      │ │
│  │   (Vue 3)    │◄─┤   Adapters   │◄─┤   (MCP)      │◄─┤  Pipeline   │ │
│  └──────────────┘  └──────────────┘  └──────────────┘  └─────────────┘ │
│         │                  │                  │                │        │
│         ▼                  ▼                  ▼                ▼        │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                      APPLICATION Layer                            │  │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ │  │
│  │  │ UseCase     │ │ SkillExec   │ │ Metadata    │ │ CodeGen     │ │  │
│  │  │ Orchestrator│ │ Orchestrator│ │ Manager     │ │ Orchestrator│ │  │
│  │  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘ │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                              │                                           │
│                              ▼                                           │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                        DOMAIN Layer                               │  │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ │  │
│  │  │ Metadata    │ │ Skill       │ │ Tenant      │ │ Permission  │ │  │
│  │  │ Aggregate   │ │ Aggregate   │ │ Aggregate   │ │ Aggregate   │ │  │
│  │  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘ │  │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ │  │
│  │  │ Domain      │ │ Repository  │ │ Domain      │ │ Specification│ │  │
│  │  │ Events      │ │ Interfaces  │ │ Services    │ │             │ │  │
│  │  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘ │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                              │                                           │
│                              ▼                                           │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                    INFRASTRUCTURE Layer                           │  │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ │  │
│  │  │ PostgreSQL  │ │   Redis     │ │   Kafka     │ │ OpenTelemetry│ │  │
│  │  │ Adapter     │ │   Adapter   │ │   Adapter   │ │   Adapter   │ │  │
│  │  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘ │  │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ │  │
│  │  │ LLM Client  │ │ Cache       │ │ Event       │ │ File        │ │  │
│  │  │ Adapter     │ │ Manager     │ │ Publisher   │ │ Storage     │ │  │
│  │  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘ │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

## 📂 Directory Structure

```
aurora-lowcode/
│
├── pom.xml                                    # Maven parent POM (Java 25, Spring Boot 3.4)
├── PROJECT_STRUCTURE.md                       # This file
├── README.md                                  # Project overview
├── LICENSE                                    # Apache 2.0
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── aurora/
│   │   │           │
│   │   │           ├── AuroraApplication.java            # Main entry point
│   │   │           │
│   │   │           ├── core/                             # Core abstractions
│   │   │           │   ├── architecture/
│   │   │           │   │   ├── AggregateRoot.java        # DDD Aggregate Root marker
│   │   │           │   │   ├── DomainEvent.java          # Domain Event base
│   │   │           │   │   ├── Entity.java               # Entity marker
│   │   │           │   │   ├── ValueObject.java          # Value Object marker
│   │   │           │   │   ├── Repository.java           # Repository marker
│   │   │           │   │   ├── DomainService.java        # Domain Service marker
│   │   │           │   │   ├── Specification.java        # Specification pattern
│   │   │           │   │   └── UseCase.java              # Use Case marker
│   │   │           │   │
│   │   │           │   ├── contract/
│   │   │           │   │   ├── MetadataRepository.java   # Metadata persistence
│   │   │           │   │   ├── SkillExecutor.java        # Skill execution
│   │   │           │   │   ├── AIPipeline.java           # AI processing
│   │   │           │   │   ├── TenantContext.java        # Tenant context holder
│   │   │           │   │   ├── AuditLogger.java          # Audit logging
│   │   │           │   │   ├── PermissionChecker.java    # Permission checking
│   │   │           │   │   ├── DataMasker.java           # Data masking
│   │   │           │   │   ├── CacheProvider.java        # Cache abstraction
│   │   │           │   │   ├── EventBus.java             # Event bus
│   │   │           │   │   └── LockProvider.java         # Distributed lock
│   │   │           │   │
│   │   │           │   ├── exception/
│   │   │           │   │   ├── AuroraException.java      # Base exception
│   │   │           │   │   ├── DomainException.java      # Domain exception
│   │   │           │   │   ├── ValidationException.java  # Validation error
│   │   │           │   │   ├── NotFoundException.java    # Resource not found
│   │   │           │   │   ├── PermissionDeniedException.java
│   │   │           │   │   ├── TenantIsolationException.java
│   │   │           │   │   ├── SkillExecutionException.java
│   │   │           │   │   └── AIGenerationException.java
│   │   │           │   │
│   │   │           │   └── result/
│   │   │           │   │   ├── Result.java               # Generic result type
│   │   │           │   │   ├── PageResult.java           # Paginated result
│   │   │           │   │   ├── ExecutionResult.java      # Skill execution result
│   │   │           │   │   └── ValidationResult.java     # Validation result
│   │   │           │   │
│   │   │           │   └── config/
│   │   │           │   │   ├── AuroraConfig.java        # Main config properties
│   │   │           │   │   ├── VirtualThreadConfig.java  # Virtual thread pool
│   │   │           │   │   ├── SecurityConfig.java       # Security settings
│   │   │           │   │   ├── TenantConfig.java         # Tenant settings
│   │   │           │   │   └── SkillConfig.java          # Skill settings
│   │   │           │
│   │   │           ├── domain/                           # Domain layer
│   │   │           │   ├── metadata/
│   │   │           │   │   ├── Metadata.java             # Metadata aggregate
│   │   │           │   │   ├── MetadataId.java           # Metadata identifier
│   │   │           │   │   ├── MetadataType.java         # Metadata type enum
│   │   │           │   │   ├── MetadataVersion.java      # Version info
│   │   │           │   │   ├── MetadataSchema.java       # Schema definition
│   │   │           │   │   ├── MetadataPermission.java   # Permission rules
│   │   │           │   │   ├── MetadataDiff.java         # Diff calculation
│   │   │           │   │   ├── MetadataRepositoryImpl.java
│   │   │           │   │   ├── MetadataService.java      # Domain service
│   │   │           │   │   └─ events/
│   │   │           │   │       ├── MetadataCreatedEvent.java
│   │   │           │   │       ├── MetadataUpdatedEvent.java
│   │   │           │   │       ├── MetadataDeletedEvent.java
│   │   │           │   │       └── MetadataVersionedEvent.java
│   │   │           │   │
│   │   │           │   ├── skill/
│   │   │           │   │   ├── Skill.java                # Skill aggregate
│   │   │           │   │   ├── SkillId.java               # Skill identifier
│   │   │           │   │   ├── SkillVersion.java          # Skill version
│   │   │           │   │   ├── SkillSchema.java           # Input/output schema
│   │   │           │   │   ├── SkillExecutorConfig.java   # Execution config
│   │   │           │   │   ├── SkillFallback.java         # Fallback strategy
│   │   │           │   │   ├── SkillSandbox.java          # Sandbox config
│   │   │           │   │   ├── SkillRegistry.java         # Skill registry
│   │   │           │   │   └─ events/
│   │   │           │   │       ├── SkillExecutedEvent.java
│   │   │           │   │       ├── SkillFailedEvent.java
│   │   │           │   │       └ SkillFallbackTriggeredEvent.java
│   │   │           │   │
│   │   │           │   ├── tenant/
│   │   │           │   │   ├── Tenant.java               # Tenant aggregate
│   │   │           │   │   ├── TenantId.java             # Tenant identifier
│   │   │           │   │   ├── TenantConfig.java         # Tenant configuration
│   │   │           │   │   ├── TenantQuota.java          # Resource quota
│   │   │           │   │   ├── TenantIsolationPolicy.java # Isolation rules
│   │   │           │   │   └─ events/
│   │   │           │   │       ├── TenantCreatedEvent.java
│   │   │           │   │       ├── TenantSuspendedEvent.java
│   │   │           │   │
│   │   │           │   ├── permission/
│   │   │           │   │   ├── Permission.java           # Permission aggregate
│   │   │           │   │   ├── Role.java                  # Role entity
│   │   │           │   │   ├── User.java                  # User entity
│   │   │           │   │   ├── Resource.java              # Resource definition
│   │   │           │   │   ├── Action.java                # Action enum
│   │   │           │   │   ├── PermissionPolicy.java      # ABAC policy
│   │   │           │   │   ├── DataPermission.java        # Row-level permission
│   │   │           │   │   ├── FieldPermission.java       # Field-level permission
│   │   │           │   │   ├── PermissionSpecification.java
│   │   │           │   │   └─ events/
│   │   │           │   │       ├── PermissionGrantedEvent.java
│   │   │           │   │       ├── PermissionDeniedEvent.java
│   │   │           │   │
│   │   │           │   └ audit/
│   │   │           │   │   ├── AuditLog.java             # Audit log entity
│   │   │           │   │   ├── AuditEntry.java           # Audit entry
│   │   │           │   │   ├── AuditType.java            # Audit type enum
│   │   │           │   │   ├── AuditSeverity.java        # Severity level
│   │   │           │   │   └─ events/
│   │   │           │   │       ├── AuditRecordedEvent.java
│   │   │           │
│   │   │           ├── application/                      # Application layer
│   │   │           │   ├── usecase/
│   │   │           │   │   ├── metadata/
│   │   │           │   │   │   ├── CreateMetadataUseCase.java
│   │   │           │   │   │   ├── UpdateMetadataUseCase.java
│   │   │           │   │   │   ├── DeleteMetadataUseCase.java
│   │   │           │   │   │   ├── QueryMetadataUseCase.java
│   │   │           │   │   │   ├── VersionMetadataUseCase.java
│   │   │           │   │   │   ├── DiffMetadataUseCase.java
│   │   │           │   │   │   ├── RollbackMetadataUseCase.java
│   │   │           │   │   │   └ HotReloadMetadataUseCase.java
│   │   │           │   │   │
│   │   │           │   │   ├── skill/
│   │   │           │   │   │   ├── ExecuteSkillUseCase.java
│   │   │           │   │   │   ├── RegisterSkillUseCase.java
│   │   │           │   │   │   ├── QuerySkillUseCase.java
│   │   │           │   │   │   ├── ValidateSkillSchemaUseCase.java
│   │   │           │   │   │
│   │   │           │   │   ├── tenant/
│   │   │           │   │   │   ├── CreateTenantUseCase.java
│   │   │           │   │   │   ├── SwitchTenantUseCase.java
│   │   │           │   │   │   ├── QueryTenantUseCase.java
│   │   │           │   │   │
│   │   │           │   │   └ permission/
│   │   │           │   │   │   ├── GrantPermissionUseCase.java
│   │   │           │   │   │   ├── CheckPermissionUseCase.java
│   │   │           │   │   │   ├── QueryPermissionUseCase.java
│   │   │           │   │   │
│   │   │           │   ├── orchestrator/
│   │   │           │   │   ├── SkillExecutionOrchestrator.java
│   │   │           │   │   ├── AIPipelineOrchestrator.java
│   │   │           │   │   ├── CodeGenOrchestrator.java
│   │   │           │   │   ├── MetadataSyncOrchestrator.java
│   │   │           │   │
│   │   │           │   ├── dto/
│   │   │           │   │   ├── request/
│   │   │           │   │   │   ├── MetadataRequest.java
│   │   │           │   │   │   ├── SkillRequest.java
│   │   │           │   │   │   ├── TenantRequest.java
│   │   │           │   │   │   ├── PermissionRequest.java
│   │   │           │   │   │   ├── QueryRequest.java
│   │   │           │   │   │   ├── PageRequest.java
│   │   │           │   │   │
│   │   │           │   │   ├── response/
│   │   │           │   │   │   ├── MetadataResponse.java
│   │   │           │   │   │   ├── SkillResponse.java
│   │   │           │   │   │   ├── TenantResponse.java
│   │   │           │   │   │   ├── PermissionResponse.java
│   │   │           │   │   │   ├── PageResponse.java
│   │   │           │   │   │
│   │   │           │   └ mapper/
│   │   │           │   │   ├── MetadataMapper.java
│   │   │           │   │   ├── SkillMapper.java
│   │   │           │   │   ├── TenantMapper.java
│   │   │           │   │   ├── PermissionMapper.java
│   │   │           │
│   │   │           ├── adapter/                          # Adapter layer
│   │   │           │   ├── rest/
│   │   │           │   │   ├── controller/
│   │   │           │   │   │   ├── MetadataController.java
│   │   │           │   │   │   ├── SkillController.java
│   │   │           │   │   │   ├── TenantController.java
│   │   │           │   │   │   ├── PermissionController.java
│   │   │           │   │   │   ├── CodeGenController.java
│   │   │           │   │   │   ├── HealthController.java
│   │   │           │   │   │
│   │   │           │   │   ├── dto/
│   │   │           │   │   │   ├── ApiRequest.java
│   │   │           │   │   │   ├── ApiResponse.java
│   │   │           │   │   │   ├── ErrorResponse.java
│   │   │           │   │   │
│   │   │           │   │   ├── filter/
│   │   │           │   │   │   ├── TenantFilter.java
│   │   │           │   │   │   ├── AuditFilter.java
│   │   │           │   │   │   ├── SecurityFilter.java
│   │   │           │   │   │   ├── RateLimitFilter.java
│   │   │           │   │   │
│   │   │           │   │   ├── exception/
│   │   │           │   │   │   ├── GlobalExceptionHandler.java
│   │   │           │   │   │
│   │   │           │   │   ├── security/
│   │   │           │   │   │   ├── JwtAuthenticationFilter.java
│   │   │           │   │   │   ├── ApiKeyAuthenticationFilter.java
│   │   │           │   │   │   ├── SecurityFilterChain.java
│   │   │           │   │   │
│   │   │           │   ├── persistence/
│   │   │           │   │   ├── entity/
│   │   │           │   │   │   ├── MetadataEntity.java
│   │   │           │   │   │   ├── SkillEntity.java
│   │   │           │   │   │   ├── TenantEntity.java
│   │   │           │   │   │   ├── PermissionEntity.java
│   │   │           │   │   │   ├── RoleEntity.java
│   │   │           │   │   │   ├── UserEntity.java
│   │   │           │   │   │   ├── AuditEntity.java
│   │   │           │   │   │
│   │   │           │   │   ├── repository/
│   │   │           │   │   │   ├── MetadataJpaRepository.java
│   │   │           │   │   │   ├── SkillJpaRepository.java
│   │   │           │   │   │   ├── TenantJpaRepository.java
│   │   │           │   │   │   ├── PermissionJpaRepository.java
│   │   │           │   │   │   ├── RoleJpaRepository.java
│   │   │           │   │   │   ├── UserJpaRepository.java
│   │   │           │   │   │   ├── AuditJpaRepository.java
│   │   │           │   │   │
│   │   │           │   │   ├── adapter/
│   │   │           │   │   │   ├── MetadataRepositoryAdapter.java
│   │   │           │   │   │   ├── SkillRepositoryAdapter.java
│   │   │           │   │   │   ├── TenantRepositoryAdapter.java
│   │   │           │   │   │   ├── PermissionRepositoryAdapter.java
│   │   │           │   │   │   ├── AuditRepositoryAdapter.java
│   │   │           │   │   │
│   │   │           │   │   ├── tenant/
│   │   │           │   │   │   ├── TenantRoutingDataSource.java
│   │   │           │   │   │   ├── TenantConnectionProvider.java
│   │   │           │   │   │   ├── TenantSchemaManager.java
│   │   │           │   │   │
│   │   │           │   ├── ai/
│   │   │           │   │   ├── client/
│   │   │           │   │   │   ├── LLMClient.java
│   │   │           │   │   │   ├── ClaudeClient.java
│   │   │           │   │   │   ├── OpenAIClient.java
│   │   │           │   │   │
│   │   │           │   │   ├── pipeline/
│   │   │           │   │   │   ├── AIPipelineOrchestrator.java
│   │   │           │   │   │   ├── PromptBuilder.java
│   │   │           │   │   │   ├── OutputParser.java
│   │   │           │   │   │
│   │   │           │   │   ├── validator/
│   │   │           │   │   │   ├── SchemaValidator.java
│   │   │           │   │   │   ├── BusinessRuleValidator.java
│   │   │           │   │   │   ├── StaticAnalyzer.java
│   │   │           │   │   │
│   │   │           │   ├── cache/
│   │   │           │   │   ├── RedisCacheAdapter.java
│   │   │           │   │   ├── CaffeineCacheAdapter.java
│   │   │           │   │   ├── CacheManager.java
│   │   │           │   │   ├── CacheKeyGenerator.java
│   │   │           │   │
│   │   │           │   ├── event/
│   │   │           │   │   ├── KafkaEventPublisher.java
│   │   │           │   │   ├── KafkaEventConsumer.java
│   │   │           │   │   ├── RedisEventPublisher.java
│   │   │           │   │   ├── LocalEventBus.java
│   │   │           │   │
│   │   │           │   ├── messaging/
│   │   │           │   │   ├── WebSocketHandler.java
│   │   │           │   │   ├── SSEHandler.java
│   │   │           │   │   ├── NotificationService.java
│   │   │           │   │
│   │   │           │   └ storage/
│   │   │           │   │   ├── FileStorageAdapter.java
│   │   │           │   │   ├── S3StorageAdapter.java
│   │   │           │   │   ├── MinioStorageAdapter.java
│   │   │           │
│   │   │           ├── infrastructure/                   # Infrastructure layer
│   │   │           │   ├── config/
│   │   │           │   │   ├── DatabaseConfig.java
│   │   │           │   │   ├── RedisConfig.java
│   │   │           │   │   ├── KafkaConfig.java
│   │   │           │   │   ├── VirtualThreadConfig.java
│   │   │           │   │   ├── OpenTelemetryConfig.java
│   │   │           │   │   ├── CacheConfig.java
│   │   │           │   │   ├── SecurityConfig.java
│   │   │           │   │   ├── JacksonConfig.java
│   │   │           │   │   ├── MetricsConfig.java
│   │   │           │   │   ├── SwaggerConfig.java
│   │   │           │   │
│   │   │           │   ├── observability/
│   │   │           │   │   ├── TracingInterceptor.java
│   │   │           │   │   ├── MetricsRecorder.java
│   │   │           │   │   ├── LoggingAspect.java
│   │   │           │   │   ├── HealthIndicator.java
│   │   │           │   │   ├── SlowQueryMonitor.java
│   │   │           │   │   ├── MemoryMonitor.java
│   │   │           │   │
│   │   │           │   ├── security/
│   │   │           │   │   ├── JwtProvider.java
│   │   │           │   │   ├── ApiKeyProvider.java
│   │   │           │   │   ├── PasswordEncoder.java
│   │   │           │   │   ├── DataMaskingService.java
│   │   │           │   │   ├── CsrfProtection.java
│   │   │           │   │   ├── XssProtection.java
│   │   │           │   │   ├── SqlInjectionProtection.java
│   │   │           │   │
│   │   │           │   ├── tenant/
│   │   │           │   │   ├── TenantContextHolder.java
│   │   │           │   │   ├── TenantRouter.java
│   │   │           │   │   ├── TenantInterceptor.java
│   │   │           │   │
│   │   │           │   ├── audit/
│   │   │           │   │   ├── AuditLoggerImpl.java
│   │   │           │   │   ├── AuditStorage.java
│   │   │           │   │
│   │   │           │   ├── lock/
│   │   │           │   │   ├── RedisLockProvider.java
│   │   │           │   │   ├── DistributedLock.java
│   │   │           │
│   │   ├── resources/
│   │   │   ├── application.yml                        # Main configuration
│   │   │   ├── application-dev.yml                    # Development config
│   │   │   ├── application-test.yml                   # Test config
│   │   │   ├── application-prod.yml                   # Production config
│   │   │   │
│   │   │   ├── db/
│   │   │   │   ├── migration/
│   │   │   │   │   ├── V1__Init_schema.sql
│   │   │   │   │   ├── V2__Tenant_isolation.sql
│   │   │   │   │   ├── V3__Permission_system.sql
│   │   │   │   │   ├── V4__Metadata_tables.sql
│   │   │   │   │   ├── V5__Skill_registry.sql
│   │   │   │   │   ├── V6__Audit_logs.sql
│   │   │   │   │
│   │   │   ├── skills/
│   │   │   │   ├── jeecg-bpmn.yaml
│   │   │   │   ├── jeecg-codegen.yaml
│   │   │   │   ├── jeecg-desform.yaml
│   │   │   │   ├── jeecg-onlchart.yaml
│   │   │   │   ├── jeecg-onlform.yaml
│   │   │   │   ├── jeecg-onlreport.yaml
│   │   │   │   ├── jeecg-system.yaml
│   │   │   │   ├── jimubi-bigscreen.yaml
│   │   │   │   ├── jimubi-dashboard.yaml
│   │   │   │   ├── jimureport.yaml
│   │   │   │   │
│   │   │   ├── schema/
│   │   │   │   ├── metadata.schema.json
│   │   │   │   ├── skill.schema.json
│   │   │   │   ├── form.schema.json
│   │   │   │   ├── report.schema.json
│   │   │   │   ├── workflow.schema.json
│   │   │   │   │
│   │   │   ├── generator/
│   │   │   │   ├── templates/
│   │   │   │   │   ├── entity.ftl
│   │   │   │   │   ├── repository.ftl
│   │   │   │   │   ├── service.ftl
│   │   │   │   │   ├── controller.ftl
│   │   │   │   │   ├── dto.ftl
│   │   │   │   │   ├── mapper.ftl
│   │   │   │   │   ├── test.ftl
│   │   │   │   │   │
│   │   │   ├── i18n/
│   │   │   │   ├── messages_en.properties
│   │   │   │   ├── messages_zh.properties
│   │   │   │   ├── messages_ja.properties
│   │   │   │
│   │   │   ├── logback-spring.xml
│   │   │
│   ├── test/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── aurora/
│   │   │           ├── test/
│   │   │           │   ├── TestBase.java
│   │   │           │   ├── TestcontainersConfig.java
│   │   │           │   ├── TestDataFactory.java
│   │   │           │   │
│   │   │           ├── domain/
│   │   │           │   ├── metadata/
│   │   │           │   │   ├── MetadataTest.java
│   │   │           │   │   ├── MetadataServiceTest.java
│   │   │           │   │   │
│   │   │           │   ├── skill/
│   │   │           │   │   ├── SkillTest.java
│   │   │           │   │   ├── SkillRegistryTest.java
│   │   │           │   │   │
│   │   │           │   ├── tenant/
│   │   │           │   │   ├── TenantTest.java
│   │   │           │   │   │
│   │   │           │   ├── permission/
│   │   │           │   │   ├── PermissionTest.java
│   │   │           │   │   ├── PermissionSpecificationTest.java
│   │   │           │   │
│   │   │           ├── application/
│   │   │           │   ├── usecase/
│   │   │           │   │   ├── CreateMetadataUseCaseTest.java
│   │   │           │   │   ├── ExecuteSkillUseCaseTest.java
│   │   │           │   │
│   │   │           │   ├── orchestrator/
│   │   │           │   │   ├── AIPipelineOrchestratorTest.java
│   │   │           │   │
│   │   │           ├── adapter/
│   │   │           │   ├── rest/
│   │   │           │   │   ├── MetadataControllerTest.java
│   │   │           │   │   ├── SkillControllerTest.java
│   │   │           │   │
│   │   │           │   ├── persistence/
│   │   │           │   │   ├── MetadataRepositoryAdapterTest.java
│   │   │           │   │
│   │   │           │   ├── ai/
│   │   │           │   │   ├── AIPipelineTest.java
│   │   │           │
│   │   ├── resources/
│   │   │   ├── application-test.yml
│   │   │   ├── test-data/
│   │   │   │   ├── metadata-sample.json
│   │   │   │   ├── skill-sample.json
│   │   │
│   ├── frontend/                                   # Frontend application
│   │   ├── package.json
│   │   ├── vite.config.ts
│   │   ├── tsconfig.json
│   │   ├── index.html
│   │   │
│   │   ├── src/
│   │   │   ├── main.ts
│   │   │   ├── App.vue
│   │   │   │
│   │   │   ├── tokens.css                           # Design tokens
│   │   │   │
│   │   │   ├── core/
│   │   │   │   ├── api/
│   │   │   │   │   ├── client.ts
│   │   │   │   │   ├── metadata.ts
│   │   │   │   │   ├── skill.ts
│   │   │   │   │   ├── tenant.ts
│   │   │   │   │   ├── permission.ts
│   │   │   │   │
│   │   │   │   ├── store/
│   │   │   │   │   ├── metadataStore.ts
│   │   │   │   │   ├── skillStore.ts
│   │   │   │   │   ├── tenantStore.ts
│   │   │   │   │   ├── permissionStore.ts
│   │   │   │   │   ├── uiStore.ts
│   │   │   │   │
│   │   │   │   ├── router/
│   │   │   │   │   ├── index.ts
│   │   │   │   │   ├── routes.ts
│   │   │   │   │   ├── guards.ts
│   │   │   │   │
│   │   │   │   ├── utils/
│   │   │   │   │   ├── request.ts
│   │   │   │   │   ├── response.ts
│   │   │   │   │   ├── cache.ts
│   │   │   │   │   ├── error.ts
│   │   │   │   │
│   │   │   ├── components/
│   │   │   │   ├── common/
│   │   │   │   │   ├── Button.vue
│   │   │   │   │   ├── Input.vue
│   │   │   │   │   ├── Select.vue
│   │   │   │   │   ├── Table.vue
│   │   │   │   │   ├── Pagination.vue
│   │   │   │   │   ├── Modal.vue
│   │   │   │   │   ├── Skeleton.vue
│   │   │   │   │
│   │   │   │   ├── lowcode/
│   │   │   │   │   ├── DynamicForm.vue
│   │   │   │   │   ├── DataTable.vue
│   │   │   │   │   ├── ChartRenderer.vue
│   │   │   │   │   ├── WorkflowDesigner.vue
│   │   │   │   │   ├── MetadataEditor.vue
│   │   │   │   │   ├── SchemaEditor.vue
│   │   │   │   │   ├── PermissionEditor.vue
│   │   │   │   │   ├── AIPromptBox.vue
│   │   │   │   │   ├── DesignCanvas.vue
│   │   │   │   │   ├── PropertyPanel.vue
│   │   │   │   │
│   │   │   │   ├── layout/
│   │   │   │   │   ├── AppLayout.vue
│   │   │   │   │   ├── Header.vue
│   │   │   │   │   ├── Sidebar.vue
│   │   │   │   │   ├── Footer.vue
│   │   │   │   │
│   │   │   ├── views/
│   │   │   │   ├── dashboard/
│   │   │   │   │   ├── DashboardView.vue
│   │   │   │   │
│   │   │   │   ├── metadata/
│   │   │   │   │   ├── MetadataList.vue
│   │   │   │   │   ├── MetadataDetail.vue
│   │   │   │   │   ├── MetadataVersion.vue
│   │   │   │   │
│   │   │   │   ├── skill/
│   │   │   │   │   ├── SkillList.vue
│   │   │   │   │   ├── SkillDetail.vue
│   │   │   │   │   ├── SkillExecute.vue
│   │   │   │   │
│   │   │   │   ├── designer/
│   │   │   │   │   ├── FormDesigner.vue
│   │   │   │   │   ├── WorkflowDesigner.vue
│   │   │   │   │   ├── ReportDesigner.vue
│   │   │   │   │   ├── DashboardDesigner.vue
│   │   │   │   │
│   │   │   │   ├── admin/
│   │   │   │   │   ├── TenantAdmin.vue
│   │   │   │   │   ├── PermissionAdmin.vue
│   │   │   │   │   ├── UserAdmin.vue
│   │   │   │   │
│   │   │   ├── styles/
│   │   │   │   ├── variables.css
│   │   │   │   ├── themes/
│   │   │   │   │   ├── light.css
│   │   │   │   │   ├── dark.css
│   │   │   │   │   ├── high-contrast.css
│   │   │   │
│   │   │   ├── i18n/
│   │   │   │   ├── en.ts
│   │   │   │   ├── zh.ts
│   │   │   │   ├── ja.ts
│   │   │
│   │   ├── public/
│   │   │   ├── favicon.ico
│   │   │
│   │   ├── tests/
│   │   │   ├── components/
│   │   │   │   ├── DynamicForm.spec.ts
│   │   │   │   ├── DataTable.spec.ts
│   │   │
│   │   ├── e2e/
│   │   │   ├── metadata.cy.ts
│   │   │   ├── skill.cy.ts
│   │   │
│   ├── docs/
│   │   ├── ARCHITECTURE.md
│   │   ├── API.md
│   │   ├── SKILLS.md
│   │   ├── SECURITY.md
│   │   ├── TENANT.md
│   │   ├── METADATA.md
│   │   ├── FRONTEND.md
│   │   ├── TESTING.md
│   │   ├── DEPLOYMENT.md
│   │   │
│   │   ├── diagrams/
│   │   │   ├── architecture.svg
│   │   │   ├── skill-flow.svg
│   │   │   ├── ai-pipeline.svg
│   │   │
│   ├── scripts/
│   │   ├── build.sh
│   │   ├── deploy.sh
│   │   ├── test.sh
│   │   ├── migration.sh
│   │
│   ├── docker/
│   │   ├── Dockerfile
│   │   ├── docker-compose.yml
│   │   ├── docker-compose.dev.yml
│   │
│   ├── .github/
│   │   ├── workflows/
│   │   │   ├── ci.yml
│   │   │   ├── release.yml
│   │   │   ├── security.yml
│   │   │
│   ├── .gitignore
│   ├── .editorconfig
│   ├── LICENSE
│   └── README.md
```

## 📦 Module Dependencies

```
┌─────────────────────────────────────────────────────────────┐
│                     Module Dependency Graph                  │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  frontend ──────────────────────────────────► REST Adapter   │
│                                                              │
│  REST Adapter ─────────────────────────────► Application     │
│                                                              │
│  Application ──────────────────────────────► Domain          │
│                                                              │
│  Domain ───────────────────────────────────► Core            │
│                                                              │
│  Infrastructure ───────────────────────────► Core            │
│  Infrastructure ───────────────────────────► Domain          │
│                                                              │
│  AI Adapter ───────────────────────────────► Application     │
│  AI Adapter ───────────────────────────────► Domain          │
│                                                              │
│  Persistence Adapter ──────────────────────► Domain          │
│                                                              │
│  Cache Adapter ────────────────────────────► Infrastructure  │
│                                                              │
│  Event Adapter ────────────────────────────► Infrastructure  │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## 🔧 Technology Stack

| Layer | Technology |
|-------|------------|
| **Backend** | Java 25, Spring Boot 3.5, Spring AI 1.0.0-M6, Spring Security |
| **Frontend** | Vue 3.5, TypeScript 5.5, Vite 6, UnoCSS, Pinia |
| **Database** | PostgreSQL 16+, MySQL 8+, Redis 7+, Kafka 3.6+ |
| **AI** | Spring AI, MCP Java SDK, Claude API, OpenAI API |
| **Observability** | OpenTelemetry, Micrometer, SLF4J/Logback |
| **Testing** | Testcontainers, WireMock, Pact, JUnit 5 |
| **Build** | Maven 3.9+, Docker, GitHub Actions |

## 🎯 Design Principles

1. **Metadata-as-Code**: All configurations stored as YAML/JSON, versioned in Git
2. **Hexagonal Architecture**: Domain isolated from infrastructure
3. **DDD Tactical Patterns**: Aggregates, Entities, Value Objects, Domain Events
4. **Skill MCP Compliance**: All skills follow MCP specification
5. **Zero Demo Code**: No placeholders, hardcoded values, or mock data
6. **Production Ready**: Complete error handling, security, observability
7. **Virtual Threads**: High concurrency via Java 25 virtual threads
8. **Accessibility**: WCAG 2.1 AA compliance for all frontend components