# Enterprise Console Phase 1 Design

## Summary

Phase 1 will rebuild the current Aurora frontend into a mature enterprise control console with a new information architecture, a production-grade permissions center, and a self-service AI model configuration center. The implementation will use a new frontend shell and new backend interface contracts first, while backend internals are replaced incrementally.

This phase does not attempt to fully implement the entire low-code and AI platform in one step. It establishes the platform foundation that later modules will plug into: navigation, permissions, tenant-aware admin workflows, and AI configuration management.

## Goals

- Replace the current demo-style console shell with an enterprise control console.
- Introduce a scalable frontend structure based on Vue 3 and Ant Design Vue.
- Deliver two real usable modules in Phase 1:
  - Permissions Center
  - AI Model Configuration Center
- Expose stable frontend-facing backend contracts for admin and AI configuration workflows.
- Preserve a front-backend separation model and allow phased backend replacement.

## Non-Goals

- Do not fully implement online forms, form designer, BPM designer, portal designer, OA workflows, AI knowledge base, AI orchestration, AI chat, or reporting engines in Phase 1.
- Do not fully migrate Aurora into a complete Spring Cloud Alibaba microservice topology in this phase.
- Do not introduce a full ABAC or policy language engine in Phase 1.
- Do not redesign code generation, Jeecg skill execution, or low-code runtime engines in this phase.

## Product Scope

### Phase 1 Includes

- Enterprise console shell
- Workbench homepage
- Permissions Center
  - Users
  - Roles
  - Menus
  - Button permissions
  - Data permissions
- AI Model Configuration Center
  - Model list
  - Create/edit/delete
  - Default model selection
  - Enable/disable
  - Connection test
- Product-grade placeholder entry pages for the remaining platform domains

### Phase 1 Placeholder Modules

- Online forms
- Form designer
- Workflow designer
- Portal designer
- Report designer
- Large screen / BI designer
- OA office modules
- AI applications
- AI knowledge base
- AI orchestration
- AI chat

Each placeholder page must still look like a real product page, with module introduction, capability cards, future entry points, and recommended next actions.

## Primary User Outcomes

- Platform administrators can manage users, roles, menus, button-level permissions, and data-level access boundaries.
- AI platform administrators can configure model endpoints by filling in only:
  - model id
  - api key
  - request url
- Business and technical users can understand the overall platform structure from a unified enterprise workspace.

## Recommended Delivery Approach

Three approaches were considered:

### Approach A: Enterprise shell + permissions + AI config

Build the new enterprise shell first, then implement the permissions center and AI model configuration center as the two real modules.

Pros:
- Best fit for the selected Phase 1 goals
- Creates reusable platform foundation for all later modules
- Minimizes rework when adding more business systems later

Cons:
- Does not deliver a full AI application workflow in the first phase

### Approach B: Enterprise shell + permissions + portal home

Prioritize homepage richness and platform presentation over AI configuration.

Pros:
- Strongest demo effect
- Good for stakeholder presentation

Cons:
- Weaker platform foundation than Approach A
- AI management gets deferred

### Approach C: Enterprise shell + AI config + AI chat

Prioritize visible AI workflows ahead of the permissions platform.

Pros:
- Strong AI product feel
- More directly aligned with AI platform messaging

Cons:
- Weak enterprise governance foundation
- Higher rework risk for large-scale business system adoption

### Recommendation

Use Approach A.

The first phase is explicitly about enterprise console modernization and permissions infrastructure. A robust AI model configuration center is the right second module because it is foundational, self-contained, and fits the platform direction without exploding scope.

## Information Architecture

### Top-Level Navigation

- Workbench
- Design Center
- AI Platform
- Reports and Screens
- OA Office
- System Management

### Second-Level Navigation

#### Workbench
- Home
- Recent Access
- Tasks and Notifications

#### Design Center
- Online Forms
- Form Designer
- Workflow Designer
- Portal Designer

#### AI Platform
- Model Configuration
- AI Applications
- Knowledge Base
- Orchestration
- AI Chat

#### Reports and Screens
- Report Designer
- Large Screen Designer

#### OA Office
- Approval Center
- Launch Request

#### System Management
- User Management
- Role Management
- Menu Management
- Button Permissions
- Data Permissions

## UX and Visual Direction

The target style is a mature enterprise SaaS console, not a startup marketing page and not a generic demo admin template.

### Visual Principles

- Dark primary navigation with light content workspace
- High information density with disciplined hierarchy
- Consistent use of cards, drawers, tabs, filters, and tables
- Operational homepage instead of decorative dashboard
- Product-grade placeholder pages instead of empty stubs

### Layout Model

- Left side navigation with top-level and current-domain navigation
- Top global bar with:
  - global search
  - tenant/environment switch
  - notifications
  - help
  - current user menu
- Main content region with:
  - breadcrumb
  - title
  - action bar
  - content body
- Optional context drawer for help, audit, or detail assist

## Page System Design

### Workbench Homepage

The homepage should present the system as an operating console.

Recommended sections:

- Platform overview
  - application count
  - configured model count
  - active users
  - daily request volume
- Quick access
  - users
  - roles
  - menus
  - model configuration
- Recent access
- Tasks and notifications
- Platform capability navigation cards

### Permissions Center

Permissions is one of the two fully usable Phase 1 domains.

#### User Management
- Table view
- Search and filters
- Enable/disable status
- Role assignment
- Department binding
- Password reset

#### Role Management
- Role list
- Menu authorization
- Button authorization
- Data permission binding

#### Menu Management
- Tree structure
- Route configuration
- Display status
- icon
- sort order

#### Button Permissions
- Page-bound action codes such as:
  - add
  - edit
  - delete
  - export
  - approve
  - test model

#### Data Permissions
- Supported rule types in Phase 1:
  - SELF
  - DEPT
  - DEPT_AND_CHILD
  - CUSTOM

Custom rules should be represented as structured conditions rather than a full expression language in this phase.

### AI Model Configuration Center

This is the second fully usable Phase 1 module.

Although the required user-managed fields are only model id, api key, and request url, the UI should present a proper enterprise configuration center.

#### Suggested Form Fields
- configuration name
- provider
- model id
- api key
- request url
- is default
- enabled
- remark

#### Supported Provider Types in Phase 1
- openai
- deepseek
- ollama
- custom

#### Supported Actions
- create
- edit
- delete
- set default
- enable / disable
- test connection

#### Security Expectations
- Do not show raw api keys in list pages
- Show masked values in detail views
- Allow keeping the existing key during edit
- Keep connection testing explicit and user-triggered

## Frontend Architecture

### Stack

- Vue 3
- TypeScript
- Ant Design Vue
- Vue Router
- Pinia
- Server-state layer such as TanStack Query

### Frontend Structure

Recommended structure:

- `layouts/`
  - enterprise shell
  - auth shell
- `pages/`
  - workbench
  - permissions pages
  - AI model configuration pages
  - placeholder domain pages
- `components/`
  - page headers
  - filter bars
  - table toolbars
  - permission-wrapped buttons
  - model configuration forms
  - status tags
- `api/`
  - admin
  - ai
  - auth
- `stores/`
  - session
  - menu state
  - permission snapshot
  - UI preferences
- `types/`
  - user
  - role
  - menu
  - permission action
  - data permission rule
  - AI model config

### Frontend Authorization Model

After login, the frontend should fetch a permission snapshot containing:

- current user
- current tenant
- roles
- visible menu tree
- permission action codes
- data permission summary

Frontend usage:

- route visibility
- menu visibility
- page action visibility
- row action visibility
- conditional batch actions

The frontend is responsible only for display control. The backend remains the source of truth for authorization enforcement.

## Backend Contract Strategy

### Delivery Principle

Phase 1 uses new contracts first, while backend implementation is replaced incrementally.

This means:

- do not fully rewrite backend internals in one pass
- introduce stable frontend-facing contracts
- reuse existing Aurora pieces where practical
- add new admin and AI config surfaces where necessary

### Target Backend Direction

Target stack direction:

- Spring Boot 3
- Spring Cloud Alibaba
- MyBatis-Plus

However, the actual Phase 1 implementation should organize by future service boundaries first, even if still deployed as a modular monolith.

Recommended logical service boundaries:

- `console-gateway`
- `iam-service`
- `ai-config-service`

## Data and Permission Model

### Core Entities

- User
- Role
- Menu
- PermissionAction
- DataPermissionRule
- Tenant
- Department
- AiModelConfig

### Relationship Model

- user to role: many-to-many
- role to menu: many-to-many
- role to action permission: many-to-many
- role to data rules: many-to-many
- user to department: many-to-one
- all admin entities tenant-scoped

### Button Permission Examples

- `user:add`
- `user:edit`
- `user:delete`
- `user:export`
- `role:grant`
- `model:test`
- `model:set-default`

### Data Permission Rule Shape

Each rule should support:

- rule type
- target resource
- optional condition set
- scope summary for frontend display

For `CUSTOM`, use structured conditions rather than a free-form policy DSL in Phase 1.

## API Contracts

### Admin APIs

- `GET /api/admin/me`
- `GET /api/admin/menus`
- `GET /api/admin/users`
- `POST /api/admin/users`
- `PUT /api/admin/users/{id}`
- `GET /api/admin/roles`
- `POST /api/admin/roles`
- `PUT /api/admin/roles/{id}/menus`
- `PUT /api/admin/roles/{id}/actions`
- `PUT /api/admin/roles/{id}/data-rules`

### AI Model Configuration APIs

- `GET /api/ai/models`
- `POST /api/ai/models`
- `PUT /api/ai/models/{id}`
- `DELETE /api/ai/models/{id}`
- `POST /api/ai/models/{id}/test`
- `PUT /api/ai/models/{id}/default`

### Standard Response Envelope

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {}
}
```

### Standard List Envelope

```json
{
  "success": true,
  "data": {
    "items": [],
    "page": 1,
    "pageSize": 20,
    "total": 120
  }
}
```

### Error Codes

- `UNAUTHORIZED`
- `FORBIDDEN`
- `VALIDATION_ERROR`
- `RESOURCE_NOT_FOUND`
- `MODEL_TEST_FAILED`
- `DUPLICATE_NAME`

## Incremental Replacement Plan

### Phase 1 Replacement Rules

- Build the new enterprise frontend shell without depending on the current demo-style layout.
- Introduce new contract surfaces for admin and AI config.
- Keep current Aurora internals where reuse is cheap and safe.
- Add dedicated implementation modules for new contract areas where reuse is poor.

### Post-Phase-1 Expansion Path

The next phases can plug into the established shell and contract model:

- forms and form designer
- workflow designer
- report and large-screen design
- OA modules
- AI application platform
- knowledge base and orchestration

## Acceptance Criteria

### UI Acceptance

- The platform presents a unified enterprise console shell.
- The homepage feels operational rather than demonstrational.
- All top-level modules are reachable.
- Real modules follow a consistent enterprise interaction model.
- Main desktop resolutions are supported cleanly.

### Functional Acceptance

- Users, roles, menus, button permissions, and data permissions can be configured through the UI.
- AI model configurations support create, edit, delete, set default, enable/disable, and connection test.
- Users can self-manage model id, api key, and request url.
- Login returns a usable permission snapshot for the frontend.

### Architecture Acceptance

- The new frontend shell is structurally separated from the old view stack.
- New contracts can evolve independently from old runtime modules.
- Permissions and AI config are reusable platform foundations.
- Placeholder domains are ready for later feature activation without major IA changes.

### Security and Reliability Acceptance

- Permissions apply at page, button, and data-scope levels.
- Sensitive secrets are not exposed in plaintext in list views.
- API responses follow one consistent contract shape.
- Errors are explicit and understandable.

## Risks and Mitigations

### Risk: Scope expansion into full platform rewrite
Mitigation: Keep Phase 1 limited to shell, permissions, and model config.

### Risk: Frontend redesign tied too tightly to old Aurora runtime modules
Mitigation: Introduce new API contracts and adapt existing backend incrementally.

### Risk: Permission model grows into a policy engine too early
Mitigation: Limit Phase 1 to RBAC + button permissions + structured data rules.

### Risk: AI model config appears too weak for enterprise use
Mitigation: Keep user-entered inputs simple but present them inside a richer governance-oriented management UI.

## Open Decisions Resolved in This Spec

- Phase 1 target: enterprise workbench + permissions platform
- Delivery model: new frontend shell + backend contracts first, backend replacement later
- Permission scope: RBAC + button permissions + data permissions
- AI model config scope: user manages model id, api key, request url
- Real modules in Phase 1: permissions management + model configuration
- Other domains: product-grade placeholder pages
