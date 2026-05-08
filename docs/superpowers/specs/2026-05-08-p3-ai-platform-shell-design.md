# P3 AI Platform Shell Design

## Summary

This design defines P3 as the first coherent AI product domain for Aurora rather than a full implementation of every planned AI capability. The phase converts scattered AI-related surfaces into a formal `AI Platform` console domain with a hub page, unified navigation, consistent route metadata, and thin frontend adapters that expose current capability maturity without forcing heavy backend expansion.

The key decision is to treat this phase as a platform-shell integration phase. Existing AI functionality such as model management, the global copilot panel, and AI-assisted application generation are gathered into a single domain. Planned capabilities such as knowledge-base Q and A, AI workflow design, image chat, embedded chat, and mobile chat are represented as formal entry or status pages so the information architecture becomes stable before deeper implementation work begins.

## Goals

- Create a first-class `AI Platform` domain in the enterprise console.
- Turn the AI area into a usable hub with consistent navigation and route titles.
- Reuse existing AI functionality where it already exists instead of rebuilding it.
- Introduce a thin frontend contract layer for AI platform summary data and entry metadata.
- Make partially implemented AI capabilities visible through clear status and entry pages.
- Keep this phase frontend-first and low-risk so it fits the established P0-P2 delivery pattern.

## Non-Goals

- Do not implement full RAG or knowledge-base runtime in this phase.
- Do not build a full AI workflow designer or orchestration engine in this phase.
- Do not deliver production-grade multimodal image chat in this phase.
- Do not implement real embedded chat SDK integrations or mobile chat containers in this phase.
- Do not introduce heavy new backend persistence or orchestration dependencies purely to support the shell.

## Scope Decision

Three candidate scopes were considered for P3:

### Approach A: AI Platform Shell

Build a formal AI domain with a hub page, structured navigation, existing model management integration, assistant and AI generation entry pages, OCR entry, and status pages for the remaining planned capabilities.

Pros:
- Matches the delivery maturity of the current codebase.
- Reuses existing surfaces instead of creating shallow duplicate features.
- Produces a stable product domain that later phases can deepen safely.
- Keeps verification and regression risk low.

Cons:
- Several AI pages remain status-first instead of full feature implementations.

### Approach B: Broader Feature Slice

Build the shell and also implement first-pass knowledge-base, workflow designer, and image chat functionality in the same phase.

Pros:
- Delivers a broader visible AI capability set.
- Reduces the number of later placeholder-to-real transitions.

Cons:
- Too large for a stable single phase.
- Pushes the frontend and backend into speculative implementations.
- Increases verification burden and rework risk.

### Approach C: Minimal Migration

Only move the existing model configuration page and expose the existing copilot capability under an AI menu.

Pros:
- Fastest possible implementation.
- Lowest immediate risk.

Cons:
- Too small to count as a meaningful P3 domain.
- Leaves most of the AI information architecture undefined.

### Recommendation

Use Approach A. P3 should establish the `AI Platform` shell and organize current capabilities into a stable domain without pretending that incomplete capabilities are fully implemented.

## Information Architecture

P3 introduces a new console domain rooted at `/ai`.

### Domain Routes

- `/ai`
  - AI platform home and capability hub.
- `/ai/models`
  - Existing model management experience, now treated as part of the AI domain.
- `/ai/assistant`
  - Formal AI assistant entry page tied to the existing copilot capability.
- `/ai/generation`
  - Formal AI generation entry page tied to the AI application generation capability already present in the generator flow.
- `/ai/ocr`
  - OCR samples and capability overview page.
- `/ai/knowledge`
  - Knowledge-base Q and A status and future integration page.
- `/ai/workflows`
  - AI orchestration and workflow designer status page.
- `/ai/image-chat`
  - Image-capable chat status page.
- `/ai/embed`
  - Third-party embedded chat status page.
- `/ai/mobile`
  - Mobile chat window status page.

### Navigation Model

The console shell should expose the AI domain the same way it exposes `system` and `online`:

- `AI Platform`
- `Model Management`
- `AI Assistant`
- `AI Generation`
- `OCR Samples`
- `Knowledge Q&A`
- `AI Workflows`
- `Image Chat`
- `Embedded Chat`
- `Mobile Chat`

All AI routes should use the same `meta.title` and `meta.titleKey` pattern used by the P2 online domain so topbar behavior and i18n stay consistent.

## Component Design

### New Shared Frontend Units

- `frontend/src/components/ai/AiModuleCardGrid.vue`
  - Shared card renderer for AI module entry pages.
  - Displays title, description, capability status, and a primary entry action.

- `frontend/src/api/ai-platform-contract.ts`
  - Thin frontend aggregation layer for AI hub data.
  - Supplies capability cards, maturity labels, route metadata, and page copy.
  - May mix static definitions, simple adapters, and existing API-backed data.

### New Views

- `frontend/src/views/ai/AiPlatformHomeView.vue`
  - AI domain hub page.
  - Shows core modules, current maturity, and recommended entry points.

- `frontend/src/views/ai/assistant/AiAssistantView.vue`
  - Formal entry page for assistant usage.
  - Explains the assistant role and links conceptually to the existing floating copilot panel.

- `frontend/src/views/ai/generation/AiGenerationView.vue`
  - Entry page for AI app generation.
  - Reuses or links to the existing AI app generation path rather than duplicating generator logic.

- `frontend/src/views/ai/ocr/AiOcrView.vue`
  - OCR samples overview page.

- `frontend/src/views/ai/knowledge/AiKnowledgeView.vue`
  - Status page for knowledge-base Q and A.

- `frontend/src/views/ai/workflows/AiWorkflowsView.vue`
  - Status page for AI workflow orchestration.

- `frontend/src/views/ai/image-chat/AiImageChatView.vue`
  - Status page for image-capable chat.

- `frontend/src/views/ai/embed/AiEmbeddedChatView.vue`
  - Status page for third-party embedded chat.

- `frontend/src/views/ai/mobile/AiMobileChatView.vue`
  - Status page for mobile chat.

### Existing Units To Reuse

- `frontend/src/views/AiModelConfigView.vue`
  - Remains the primary implementation for model management.
  - It may stay in its current file location for now if that keeps the change small.

- `frontend/src/components/copilot/AICopilotPanel.vue`
  - Remains the global floating assistant surface.
  - P3 does not replace it; instead, P3 gives it a proper domain entry page so the product structure is coherent.

- `frontend/src/views/GenerateView.vue`
  - Its AI app generation capability is referenced or wrapped by the AI generation domain page.
  - The goal is to expose the capability consistently, not clone the implementation.

## Data Flow And Contracts

P3 should use a mixed data strategy routed through a thin frontend adapter.

### Data Source Types

1. Existing real APIs
- Model management should continue using the existing AI contract API because it already has meaningful behavior.

2. Frontend aggregated definitions
- AI home cards, module descriptions, maturity badges, and status summaries should come from `ai-platform-contract.ts`.

3. Existing capability mappings
- Entry pages that represent existing behaviors, such as AI app generation, should map to the current implementation path rather than rebuild it.

### Why This Contract Layer Exists

- It keeps AI home and entry-page copy out of the router and view internals.
- It prevents page components from mixing static product metadata with live API logic.
- It allows later phases to swap mock or static summaries with real backend signals without changing the page structure.

## Page Types And Behavioral Rules

P3 intentionally uses three page categories.

### Real Pages

- `/ai/models`

These pages already have real CRUD or integration behavior and should be treated as mature capability pages.

### Capability Entry Pages

- `/ai`
- `/ai/assistant`
- `/ai/generation`
- `/ai/ocr`

These pages are product-grade entries that explain the capability, surface key actions, and route the user into the current usable flow.

### Status Pages

- `/ai/knowledge`
- `/ai/workflows`
- `/ai/image-chat`
- `/ai/embed`
- `/ai/mobile`

These pages must clearly state that the capability exists in the roadmap and current domain model but is not fully implemented in this phase. They should include a short description, current phase status, and the intended future direction. They must not present fake interactive experiences that imply production readiness.

## Error Handling And UX Rules

- AI hub and status pages should degrade gracefully because most data is local or adapter-backed.
- Model management retains its existing loading, error, and empty states.
- Entry pages should prefer explicit capability-state messaging over silent empty states.
- If an AI entry points to an existing capability that is not currently available, the page should show a concise unavailable state instead of broken navigation.

## Testing Strategy

P3 should follow the same layered verification model used in P2.

### Route Tests

- Add `frontend/src/router/__tests__/ai-routes.spec.ts`
- Verify:
  - `/ai` resolves correctly.
  - Each new AI route resolves correctly.
  - `/ai/models` continues to resolve correctly.

### View Render Tests

- Add `frontend/src/views/ai/__tests__/ai-domain-render.spec.ts`
- Verify:
  - AI home renders key module text.
  - Assistant, generation, and OCR entry pages render expected headings and summary copy.
  - Status pages render explicit capability-state messaging.

### Shared Component Tests

- Add `frontend/src/components/ai/__tests__/ai-module-card-grid.spec.ts`
- Verify the AI module card grid renders card title, description, status label, and entry link text.

### Playwright Smoke

- Add `frontend/e2e/ai-platform.spec.ts`
- Verify:
  - Authenticated user can load `/ai`.
  - Core links are visible on the AI hub.
  - Navigation to `/ai/models`, `/ai/assistant`, and `/ai/generation` works.
  - At least one status page loads with the expected heading and status copy.

The smoke should remain light. Deep interaction testing belongs only to pages with meaningful behavior already present.

## Acceptance Criteria

P3 is complete when all of the following are true:

- `AI Platform` is a first-class console domain.
- The sidebar exposes a complete AI capability tree for this phase.
- `/ai` functions as a usable hub page.
- Existing model management is integrated into the AI domain structure.
- Assistant, generation, and OCR have formal entry pages.
- Knowledge, workflows, image chat, embedded chat, and mobile chat have explicit status pages.
- Route metadata and topbar titles behave consistently with the rest of the console.
- AI domain i18n entries exist for navigation and core page text.
- Route tests, render tests, shared component tests, and smoke tests all pass.

## Risks And Mitigations

### Risk: The phase looks shallow

Mitigation:
- Make the hub page and entry pages product-grade, not placeholder stubs.
- Reuse real capabilities where they already exist.
- Use explicit maturity/status messaging so the domain still feels intentional.

### Risk: Existing AI capabilities remain fragmented behind new wrappers

Mitigation:
- Route users through clear domain entry pages.
- Keep AI copy, links, and capability definitions centralized in the thin contract layer.

### Risk: P3 drifts into backend-heavy work

Mitigation:
- Keep the scope boundary strict.
- Only use real APIs where the codebase already has working behavior.
- Represent future capabilities as status pages rather than speculative implementations.

## Final Decision

P3 should be executed as an AI platform shell phase. The purpose of this phase is not to finish every AI feature, but to make Aurora's AI capability set coherent, navigable, testable, and ready for later deepening work.
