# Phase 10 Micro-Frontends Design

## Status
- Derived from approved Phase 10 V1.1 blueprint
- Track: Micro-Frontends / Module Federation

## Objective
Turn Aurora frontend into a governed host application that can load trusted Vue remotes without sacrificing tenant safety, shared UX consistency, or build stability.

## Chosen Model
- Host-Orchestrated extensions
- static registration in V1.1
- dynamic registration model reserved in design only
- dual trust: platform whitelist plus tenant enablement

## Core Roles

### Host
Owns:
- authentication and tenant context
- i18n and theme tokens
- `useServerState` and shared API access
- route registration and navigation integration
- CSP-safe script loading
- capability gateway for future knowledge-enhanced actions

### Remote
Owns:
- feature-local UI and route content
- declared capability needs
- module context metadata

Remote must not:
- load arbitrary untrusted host scripts
- replace host auth or tenant context
- directly access RAG internals

## Registration Model

### V1.1 Static Registry
Host maintains a configuration registry with entries like:
- `remoteId`
- `remoteName`
- `entryUrl`
- `exposes`
- `routeBase`
- `displayName`
- `requiredCapabilities`
- `allowedTenants`
- `compatibilityRange`
- `enabled`

This registry is the source of truth for what the host may load.

### Future Dynamic Model
Reserved for later versions:
- persisted remote registry
- admin UI for enabling/disabling remotes
- signed manifests and rollout controls

## Trust Model

### Platform Layer
Approves remote sources globally.

### Tenant Layer
Can only enable remotes from the approved global list.

This prevents a tenant from introducing arbitrary script origins while preserving controlled customization.

## Shared Dependency Policy

### Shared In V1.1
- `vue`
- `vue-router`
- `pinia` when needed
- `vue-i18n`
- `@tanstack/vue-query`
- `useServerState`
- `tokens.css`

### Not Shared By Default
- host page internals
- private business components
- arbitrary generated SDK internals unless intentionally exported

## Routing Model

### Core Routes
- owned by host

### Extension Routes
- registered from static remote registry
- injected through a controlled route registration path

Each remote route must declare:
- `routeBase`
- `mountComponent`
- `navMeta`
- `requiredCapabilities`
- optional context tags for host orchestration

## Security Model

### Hard Constraints
- only approved remote origins may be loaded
- CSP must explicitly allow trusted remote script origins
- no unrestricted `unsafe-eval` and no broad wildcard script sources
- remote version compatibility must be checked before mount
- remote load failure must degrade locally, not globally

### Runtime Guards
- integrity and compatibility validation before route registration
- explicit fallback UI for missing or failed remote entry
- audit trail for remote enablement and load failures

## Capability Bridge
V1.1 partial coupling with RAG is expressed through a host capability bridge.

Remotes may:
- declare module context
- request knowledge-enhanced generation behavior through host API

Remotes may not:
- access vector store directly
- assemble enterprise context directly

## Build Strategy
- Vite Module Federation via `@originjs/vite-plugin-federation`
- Host exposes shared platform dependencies and selected utility surfaces
- build verification must prove federation does not break current host output or CSP assumptions

## Testing Strategy

### Build Tests
- host build still succeeds with federation enabled
- remotes resolve shared dependencies correctly

### Security Tests
- unapproved remote origins are rejected
- CSP allows approved remote entry points only

### Runtime Tests
- remote load success path mounts correctly
- remote load failure shows fallback UI
- route registration does not break existing routes

### Cross-Track Tests
- remote can declare context and call host capability
- host remains sole executor for knowledge-enhanced generation

## Documentation Deliverable
Create `frontend/MICRO_FRONTEND_GUIDE.md` with:
- remote project structure expectations
- required shared dependencies
- route manifest expectations
- compatibility rules
- CSP and trust model explanation
- local development workflow

## Risks And Controls
- supply-chain risk: whitelist and compatibility gates
- shared dependency drift: explicit shared contract
- host fragility: error boundaries and fallback route behavior
- security header regression: dedicated CSP review before release

## Deliverables
- `@originjs/vite-plugin-federation` dependency
- updated `frontend/vite.config.ts`
- router extension hooks in `frontend/src/router/index.ts`
- static remote registry model
- `frontend/MICRO_FRONTEND_GUIDE.md`
