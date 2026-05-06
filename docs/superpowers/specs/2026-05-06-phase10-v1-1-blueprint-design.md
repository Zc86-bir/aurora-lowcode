# Phase 10 V1.1 Blueprint Design

## Status
- Proposed and approved in conversation
- Scope: Phase 10, V1.1 evolution
- Tracks: parallel delivery of Enterprise RAG and Micro-Frontends

## Objective
Evolve Aurora from a V1.0 AI-driven low-code platform into a V1.1 enterprise platform with:
- private-domain knowledge retrieval and generation augmentation
- extensible remote UI capability through a governed host platform

## Guiding Model
Chosen approach: **Host-Orchestrated Intelligence**.

This means:
- Host owns retrieval, policy, prompt injection, security boundaries, and extension governance.
- Remotes consume platform capabilities but do not own vector search or bypass host controls.
- Both tracks ship under the same V1.1 milestone but remain independently understandable and testable.

## Scope

### In Scope
- PostgreSQL 17 `pgvector` enablement and tenant-aware vector retrieval
- enterprise knowledge ingestion and lifecycle tracking
- RAG augmentation for code generation and Copilot
- Vite Module Federation host architecture
- trusted remote registration and CSP-safe loading
- host capability boundary for future remote-to-host AI workflows

### Out of Scope
- direct remote access to vector store internals
- unrestricted runtime remote URL registration in V1.1
- full plugin marketplace and billing model
- general-purpose RAG for every skill in V1.1

## Shared Architecture Boundaries

### Host-Owned Capabilities
- tenant and user context
- role and knowledge-space authorization
- vector search execution
- enterprise context assembly
- prompt injection
- route governance and remote loading
- i18n, theme tokens, and shared server-state APIs

### Remote-Owned Capabilities
- feature-local UI rendering
- route-level page composition
- capability requests to host
- module context declaration

### Explicit Non-Goals For Remotes
- no direct pgvector access
- no direct embedding or retrieval execution
- no bypass of host auth, tenant, or knowledge policies

## Parallel Track Model

### Track A: Enterprise RAG
- multi-level knowledge spaces: tenant, project, module
- mixed retrieval control: system default plus advanced user override
- first-class ingestion for PDF, TXT, MD, DOCX, HTML, and URL imports
- V1.1 runtime augmentation targets: code generation and Copilot only

### Track B: Micro-Frontends
- static remote registration in V1.1
- future-ready dynamic registration model reserved in design only
- dual trust model: platform whitelist plus tenant enablement
- shared dependencies: Vue, Router, i18n, tokens, TanStack Query, `useServerState`

## Coupling Strategy
Chosen coupling: **partial coupling**.

Definition:
- Remotes can declare module and generation context.
- Host can expose a governed "knowledge-enhanced generation" capability.
- Remotes can request enhanced generation outcomes.
- Remotes cannot execute retrieval directly or manage enterprise context assembly.

## End-to-End Data Flow

### Knowledge Flow
1. User uploads or imports document.
2. Host stores document metadata and status.
3. Background ingestion parses, splits, embeds, and stores vectors.
4. Host publishes ingestion event and audit trail.

### Generation Flow
1. User or Copilot submits prompt.
2. Host resolves retrieval context.
3. Host performs tenant-aware similarity search.
4. Host assembles `enterprise_context`.
5. Host injects context into prompt.
6. Existing LLM, AST, schema, and business-rule validation pipeline runs unchanged.

### Remote Flow
1. Host loads approved remotes.
2. Remote declares route and context metadata.
3. Remote invokes host-governed capability if knowledge enhancement is needed.
4. Host performs secured orchestration and returns result.

## Delivery Milestones
- M1: pgvector base and tenant-aware search
- M2: ingestion pipeline and knowledge lifecycle
- M3: RAG augmentation in generation and Copilot
- M4: Module Federation host and remote registry
- M5: remote developer guide and capability contract
- M6: cross-track integration and hardening

## Acceptance Criteria

### RAG
- tenant isolation cannot be bypassed in vector search
- ingestion completes with explicit status progression
- enterprise context can augment generation and Copilot
- existing safety validation remains intact

### Micro-Frontends
- approved remotes load without breaking host build
- unapproved remotes cannot load
- shared dependency contract remains stable
- remote failures degrade locally and do not crash host navigation

### Cross-Track
- remotes can request governed knowledge-enhanced generation
- host performs all retrieval and injection
- tenant, role, and knowledge-space policies remain enforced end to end

## Risks And Controls
- retrieval overreach: controlled by tenant-aware store and role filtering
- prompt bloat: controlled by top-k, threshold, and context size cap
- remote supply chain risk: controlled by whitelist, CSP, compatibility gates
- remote-host drift: controlled by explicit shared contract and route registry

## Recommended Execution Order
- Build RAG foundation first.
- Build micro-frontend host capabilities second.
- Converge both tracks in cross-track integration last.

This preserves the requested parallel milestone while keeping implementation risk low.
