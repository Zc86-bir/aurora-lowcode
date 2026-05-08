# Aurora Platform Capability Expansion Design

## Summary

This design expands Aurora from a partially implemented low-code and AI platform into a complete enterprise control console organized by product domains. The requested capability set is too large for a single implementation batch, so the design defines a phased delivery model, a unified domain object model, a stable information architecture, and shared runtime chains that later implementation work must follow.

The core decision is to deliver the platform by business domains while enforcing a platform-foundation-first execution order. This keeps the product structure aligned with the final console while preventing repeated reinvention of menus, permissions, tenant controls, dictionaries, and runtime projections.

## Goals

- Expand Aurora toward the full enterprise capability matrix described by the user.
- Preserve the current DDD and hexagonal architecture while introducing product-scale module organization.
- Create a scalable information architecture covering system management, online low-code, AI platform, reporting, operations, advanced features, and enterprise enhancements.
- Standardize shared platform concepts such as menus, permissions, tenants, dictionaries, departments, feature flags, and runtime projections.
- Separate definition-time, publish-time, and runtime models for low-code and AI-generated assets.
- Provide a phased roadmap that can be implemented safely without destabilizing the current codebase.

## Non-Goals

- Do not implement the entire capability matrix in a single phase.
- Do not duplicate core platform models across system, low-code, AI, reporting, and enterprise modules.
- Do not let AI flows and business process flows collapse into one runtime abstraction.
- Do not continue growing the platform by adding disconnected pages without unified routing, permissions, and module registration.

## Recommended Delivery Model

Three decomposition strategies were considered:

### Approach A: Split by business domains

Deliver modules according to the final product navigation, such as system management, online low-code, AI platform, reporting, operations, and enterprise enhancements.

Pros:
- Best match to the requested capability list
- Easiest to communicate to stakeholders
- Each phase can become a real product slice

Cons:
- Requires discipline so shared platform concerns are not rebuilt inside each domain

### Approach B: Split by technical layers

Deliver foundation, then backend engines, then frontend consoles, then enhancements.

Pros:
- Clearest engineering dependency chain
- Easier for backend-first execution

Cons:
- Weak product visibility in early phases
- Harder to validate against the requested feature matrix

### Approach C: Split by delivery form

Deliver shell and placeholders first, then usable modules, then enterprise upgrades.

Pros:
- Fastest way to show a full platform surface
- Good demo effect

Cons:
- Highest rework risk
- Encourages shallow implementations

### Recommendation

Use Approach A, split by business domains, with an internal rule that the platform foundation must be implemented first and shared by every later phase.

## Delivery Phases

### P0 Platform Foundation

Scope:
- Menu and route model
- Module registry
- Permission model
- Tenant and feature entitlement model
- Dictionary and classification model
- Organization model for departments and positions
- Unified page shells for list, detail, form, designer, and ops views

Goal:
Create the shared platform substrate required by every later module.

### P1 System Management Center

Scope:
- User management
- Role management
- Menu management
- Button permissions
- Data permissions
- Form permissions
- Department management
- My departments
- Dictionary management
- Classification dictionary
- System notices
- Position management
- Contacts
- Multi-data-source management
- Multi-tenant management

Goal:
Establish the enterprise governance and administration center.

### P2 Online Low-Code Center

Scope:
- Online forms
- Online code generator
- Online reports
- Dashboard designer
- Naming rules
- Validation rules

Goal:
Turn existing skills and runtime services into a usable low-code control center.

### P3 AI Application Platform

Scope:
- Knowledge-base Q and A
- Model management
- AI orchestration
- AI workflow designer
- Image-capable chat
- AI assistant chat
- AI form and table generation
- Third-party embedded chat window
- Mobile chat window
- OCR samples

Goal:
Turn existing AI infrastructure into a coherent managed product domain.

### P4 Reporting and Big Screen Center

Scope:
- Jimu report designer
- Print designer
- Data report designer
- Chart report designer
- Reporting examples
- Big-screen templates

Goal:
Move reporting and big-screen capabilities from definitions into assetized design workflows.

### P5 Operations and Messaging Center

Scope:
- Message management
- Template management
- Jobs and schedulers
- System logs
- Data logs
- Notifications
- SQL monitoring
- OpenAPI management
- Gateway routes
- Performance monitoring for Redis, Tomcat, JVM, disk, request tracing, and servers

Goal:
Expose platform operations and messaging as first-class control-center features.

### P6 Advanced and Enterprise Enhancements

Scope:
- Portal designer
- Enhanced form and screen designers
- Task center and history views
- Process instance management
- Process listeners and expressions
- Delegation, CC, and jump operations
- OA office components
- Zero-code application management
- AI writing and field suggestions
- External submission support
- Multi-view design, including calendar, table, kanban, and gantt

Goal:
Deliver enterprise and commercial enhancement capabilities on top of a stable base platform.

## Version Packaging

- Foundation: P0 + P1
- LowCode: P0 + P1 + P2
- AI Suite: P0 + P1 + P2 + P3 + P4
- Enterprise Plus: P0 through P6

## Domain Object Model

### Platform Foundation Objects

- `Tenant`
- `OrgUnit`
- `Position`
- `User`
- `Role`
- `Menu`
- `PermissionPolicy`
- `DictionaryCategory`
- `DictionaryItem`
- `Notice`
- `DataSourceConfig`

These are shared platform objects and must not be redefined independently inside later domains.

### Low-Code Objects

- `AppDefinition`
- `FormDefinition`
- `FormFieldDefinition`
- `FormViewDefinition`
- `ReportDefinition`
- `ChartDefinition`
- `DashboardDefinition`
- `BigScreenDefinition`
- `CodegenProject`
- `NamingRule`
- `ValidationRule`

### AI Platform Objects

- `ModelProvider`
- `ModelConfig`
- `KnowledgeBase`
- `KnowledgeDocument`
- `ConversationSession`
- `ConversationMessage`
- `AiWorkflowDefinition`
- `AiAgentNode`
- `AiExecutionRecord`
- `OcrJob`
- `EmbeddingAppBinding`

### Process and Enterprise Objects

- `ProcessDefinition`
- `ProcessInstance`
- `TaskInstance`
- `TaskDelegation`
- `FormDesignDefinition`
- `PortalDefinition`
- `ZeroCodeAppDefinition`

### Operations and Messaging Objects

- `MessageTemplate`
- `MessageRecord`
- `SystemNotification`
- `JobDefinition`
- `ApiCredential`
- `GatewayRouteDefinition`
- `AuditSnapshot`
- `MetricPanel`

## Aggregate Boundaries

- `IdentityAggregate`: `User`, `Role`, `Position`, `OrgUnit`
- `AccessControlAggregate`: `Menu`, `PermissionPolicy`
- `MetadataAggregate`: `DictionaryCategory`, `DictionaryItem`, `NamingRule`, `ValidationRule`
- `LowCodeAggregate`: `AppDefinition`, `FormDefinition`, `ReportDefinition`, `DashboardDefinition`, `BigScreenDefinition`
- `AiAggregate`: `ModelProvider`, `ModelConfig`, `KnowledgeBase`, `ConversationSession`, `AiWorkflowDefinition`
- `ProcessAggregate`: `ProcessDefinition`, `ProcessInstance`, `TaskInstance`
- `OpsAggregate`: `MessageTemplate`, `MessageRecord`, `JobDefinition`, `GatewayRouteDefinition`, `AuditSnapshot`

## Module Dependency Rules

- P0 is required by every other phase.
- P1 provides users, roles, menus, permissions, organization, and tenant governance to all later phases.
- P2 depends on P0 and P1, and becomes the structural base for low-code assets used by P3, P4, and P6.
- P3 depends on P0 and P1, and may land outputs into P2 asset drafts.
- P4 depends on P0, P1, and P2.
- P5 depends on P0 and P1, and projects platform events from all other domains.
- P6 depends on all earlier phases.

The following dependency mistakes are explicitly forbidden:

- A domain creating its own user, role, menu, or data-source model
- AI directly writing formal runtime business objects without draft and publish flow
- Reporting, big screen, and portal modules maintaining separate data-source abstractions
- Frontend pages inventing permission rules instead of consuming resolved permission projections
- AI orchestration and business workflow sharing one universal process model

## Information Architecture

### Top-Level Navigation

- Workbench
- System Management
- Online Low-Code
- AI Platform
- Reporting and Screens
- Messaging and Operations
- Showcase and Components
- Advanced Features
- Enterprise Enhancements

### Domain Menu Mapping

#### Workbench
- Console home
- My tasks
- Recent applications
- AI quick entry
- Common actions

#### System Management
- User management
- Role management
- Menu management
- Permission settings
- Form permissions
- Department management
- My departments
- Dictionary management
- Classification dictionary
- System notices
- Position management
- Contacts
- Multi-data-source management
- Multi-tenant management

#### Online Low-Code
- Online forms
- Online code generator
- Online reports
- Dashboard designer
- Naming rules
- Validation rules

#### AI Platform
- Knowledge Q and A
- Model management
- AI orchestration
- AI workflow designer
- AI assistant chat
- AI form generation
- Vision chat
- OCR
- Embedded chat configuration
- Mobile chat configuration

#### Reporting and Screens
- Jimu report designer
- Print designer
- Data report designer
- Chart report designer
- Reporting examples
- Big-screen templates

#### Messaging and Operations
- Messages
- Templates
- Jobs
- System logs
- Data logs
- Notifications
- SQL monitoring
- OpenAPI management
- Gateway routes
- Performance monitoring

#### Showcase and Components
- Common examples
- Shared components
- Page templates

#### Advanced Features
- SSO
- App publishing
- WebSocket messaging
- Electron packaging
- Docker support
- Mobile application framework
- Mobile low-code design

#### Enterprise Enhancements
- Process designer
- Simple-flow designer
- Portal designer
- Form designer
- Screen designer
- My tasks
- History flows
- Process instance management
- Process listener management
- Process expressions
- Flows I started
- My carbon copies
- Delegation and jump actions
- OA components
- Zero-code application management
- AI writing
- AI field suggestions
- External submission
- Multi-view design

### Frontend Route Strategy

Frontend views should be reorganized by product domains rather than broad demo-style pages.

Recommended route namespaces:

- `/workbench/*`
- `/system/*`
- `/online/*`
- `/ai/*`
- `/analytics/*`
- `/ops/*`
- `/showcase/*`
- `/advanced/*`
- `/enterprise/*`

Recommended frontend directory layout:

```text
frontend/src/views/
  workbench/
  system/
  online/
  ai/
  analytics/
  ops/
  showcase/
  advanced/
  enterprise/
```

Existing broad views such as `FormsView.vue`, `ReportsView.vue`, `WorkflowsView.vue`, and `SettingsView.vue` should be incrementally decomposed into domain-based views.

## Backend Menu and Permission Mapping

Each menu item should map to a backend resource code. Recommended resource naming:

- Menu codes: `menu:domain:module`
- Action codes: `domain:module:action`

Examples:

- `menu:system:users`
- `menu:online:forms`
- `menu:ai:models`
- `system:user:view`
- `system:user:create`
- `online:form:publish`
- `ai:model:manage`
- `ops:gateway:view`

This supports consistent menu visibility, action permissions, tenant gating, and frontend projection.

## Tenant Packaging and Feature Gating

Three gating layers are required:

1. Package layer
- Foundation
- LowCode
- AI Suite
- Enterprise Plus

2. Module layer
- `system`
- `online`
- `ai`
- `analytics`
- `ops`
- `advanced`
- `enterprise`

3. Feature layer
- Fine-grained feature flags such as `ai.vision-chat`, `enterprise.portal`, or `ops.openapi`

Frontend evaluation order:

`package -> module enablement -> menu permission -> action permission`

## Runtime Data Flows

Six core runtime chains define the whole platform.

### 1. Menu and Module Assembly

```text
Tenant Package / Feature Flags
  -> Module Registry
  -> Menu Definitions
  -> Role Permission Binding
  -> Frontend Navigation Tree
  -> Route Guard
  -> Page Render
```

Menus must come from a backend-managed truth source. Frontend should consume menu trees instead of hardcoding the full console structure.

### 2. Permission Resolution

```text
User
  -> Roles
  -> Permission Policies
  -> Resource Scope Resolution
  -> API Authorization
  -> UI Capability Projection
  -> Button / Data / Field Effect
```

Permission resolution has four levels:

- Menu permissions
- Button permissions
- Data permissions
- Form permissions

Backend performs final authorization. Frontend consumes resolved projections for presentation.

Recommended outputs:

- `ResolvedPermissionSet`
- `ResolvedFormSchema`

### 3. Low-Code Runtime

```text
Dictionary / ValidationRule / NamingRule
  -> FormDefinition / ReportDefinition / DashboardDefinition
  -> Publish Version
  -> Runtime Resolver
  -> Render / Query / Execute
  -> Audit / Snapshot / Notification
```

Definitions must be separated into:

- Draft state
- Published state
- Runtime projection

### 4. AI Collaboration Runtime

```text
Prompt / User Intent
  -> Model Selection
  -> Knowledge / Context Retrieval
  -> Supervisor / Workflow Planning
  -> Tool or Skill Execution
  -> Validation / Correction
  -> Domain Landing
  -> Conversation / Audit Persistence
```

The AI platform is organized into:

- Conversation layer
- Knowledge layer
- Execution layer
- Orchestration layer

AI must write draft assets, not formal runtime assets.

### 5. Process and Task Runtime

```text
Form / Business Object
  -> ProcessDefinition
  -> ProcessInstance
  -> TaskInstance
  -> Approver Resolution
  -> Action
  -> Message / Audit / Snapshot
```

AI flows and business process flows are distinct domains. They may cooperate but must not collapse into one model.

### 6. Operations and Messaging Loop

```text
Business Event / AI Event / Process Event
  -> Domain Event Bus
  -> Message Template Resolution
  -> Notification Dispatch
  -> Log / Metric / Trace
  -> Ops Console Projection
```

Messages, auditing, and observability are platform event projections, not independent silo systems.

## Unified Runtime Projections

To reduce frontend duplication, backend should expose common projection models:

- `MenuTreeView`
- `ResolvedPermissionSet`
- `ResolvedFormSchema`
- `ResolvedReportSchema`
- `ResolvedDashboardSchema`
- `ModuleCapabilityView`
- `TenantFeatureView`

## Backend and Frontend Responsibilities

### Backend Responsibilities

- Core domain models
- Permission resolution and authorization
- Data-scope resolution
- Draft, publish, and version management
- Runtime projection generation
- Audit, notifications, tasks, and event emission

### Frontend Responsibilities

- Enterprise console shell
- Navigation rendering
- Page interaction and layouts
- Consumption of backend runtime projections
- Presentation-level capability handling

Frontend must not become the truth source for tenant packaging, menu truth, or field-level permission rules.

## Failure and Rollback Rules

- Failed low-code publish keeps the last published version active.
- Failed AI generation remains in draft state with correction history.
- Failed process execution moves the instance into an exception state for manual or automated follow-up.
- Failed notification dispatch is recorded and retryable without blocking core business state.
- Failed monitoring collection must not affect business execution.

## Testing Strategy

### Unit Tests

Focus on policy and projection engines, not just CRUD.

Recommended new unit suites:

- `MenuVisibilityResolverTest`
- `PermissionProjectionServiceTest`
- `FormPermissionResolverTest`
- `ModuleCapabilityResolverTest`
- `DefinitionPublishServiceTest`
- `TenantFeatureResolverTest`

### Integration Tests

Key scenarios:

- Login to menu-tree resolution
- Role changes affecting visible actions and data scopes
- Draft to publish to runtime resolution for low-code assets
- AI generation landing in drafts and then publishing
- Process launch, task generation, approval, rejection, and CC flows
- Event to notification recording
- Multi-tenant isolation across menus, data, and model configuration

Recommended integration suites:

- `SystemManagementIntegrationTest`
- `LowCodePublishFlowIT`
- `AiDraftToPublishIT`
- `ProcessTaskLifecycleIT`
- `TenantFeatureIsolationIT`

### Frontend Tests

Focus on shared console framework behavior:

- Navigation tree rendering
- Permission-projected action visibility
- Dynamic form field visibility and editability
- Shared list and detail shell behavior
- Basic designer-page interaction
- Feature-unavailable and package-lock states

### E2E Tests

Use a small number of high-value cross-domain flows:

- Tenant setup and feature enablement
- Role assignment and menu access
- Online form draft and publish
- AI-generated draft to publish path
- Business process initiation and task completion
- Operations user reviewing notifications and logs

### Architecture Governance

Extend current ArchUnit-style governance with checks for:

- Domain package boundaries
- Route and page organization by product domain
- Unified permission resolution layer usage
- No duplicated tenant or package gating logic in frontend
- Separation between definition-state and runtime-state objects

## Implementation Order

### Stage A
- P0 foundation

### Stage B
- P1 system management center

### Stage C
- P2 online low-code center

### Stage D
- P3 AI application platform

### Stage E
- P4 reporting and big-screen center

### Stage F
- P5 operations and messaging center

### Stage G
- P6 advanced and enterprise enhancements

Recommended sequencing inside the first major line:

- Menus and module registry first
- Permission projections second
- System management domain third
- Low-code publish chain fourth
- AI landing on draft assets fifth

## Key Risks and Controls

### Risk: Scope explosion

Control:
- Each phase must define explicit out-of-scope items.
- Enterprise enhancements must not leak into foundation phases.

### Risk: Model duplication

Control:
- Shared platform objects must be introduced before later modules.
- New module design reviews must reject parallel identity, menu, permission, or data-source abstractions.

### Risk: Navigation drift

Control:
- Product-domain route namespaces
- Backend-managed menu tree
- Shared naming conventions for routes and views

### Risk: Scattered permission logic

Control:
- Single resolution layer for permissions
- Frontend only consumes resolved projections

### Risk: AI and low-code coupling drift

Control:
- AI lands into drafts only
- Publish remains a controlled boundary

### Risk: Enterprise features polluting the mainline

Control:
- Separate top-level enterprise domain
- Package and feature gating by tenant

### Risk: Test cost explosion

Control:
- Test platform chains and shared projections
- Keep E2E focused on business-critical flows

## Milestones

- M1: Platform foundation stable
- M2: System management usable
- M3: Low-code draft and publish chain usable
- M4: AI domain landing usable
- M5: Reporting and big-screen asset workflows usable
- M6: Operations and messaging center usable
- M7: Enterprise enhancement layer independently shippable

## Current-Codebase Impact

The current Aurora repository already contains relevant foundations such as:

- `SkillDefinitionLoader`
- `MetadataHotReloadManager`
- `DefaultSupervisorOrchestrator`
- `ImmutableAuditLogger`
- `LlmGatewayService`
- `WebSearchService`
- `YjsWebSocketHandler`

This design does not replace them blindly. It repositions them:

- `SkillDefinitionLoader` can evolve toward broader module and capability registration.
- `MetadataHotReloadManager` can support draft and publish version switching.
- `DefaultSupervisorOrchestrator` remains an AI orchestration primitive, not a business process engine.
- `ImmutableAuditLogger` becomes the shared audit path for publish, process, AI, and admin actions.
- Search, LLM, and collaboration services become managed sub-capabilities of the AI platform and runtime ecosystem.

## Final Recommendation

Implement the platform using two main execution tracks after planning:

- Track 1: Platform foundation and system governance
- Track 2: Low-code and AI product mainline

Do not run reporting, full ops, and enterprise enhancement work in parallel with the first mainline phases unless the platform foundation and governance surfaces are already stable.
