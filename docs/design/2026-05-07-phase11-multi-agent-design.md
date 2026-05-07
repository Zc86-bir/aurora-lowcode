# Phase 11 Design: Multi-Agent Orchestration, Text-to-SQL BI, and MFE Remote

Date: 2026-05-07
Status: Approved for planning, pending implementation review gate

## 1. Scope

Phase 11 introduces five connected capabilities:

1. A Supervisor-based multi-agent orchestration flow that decomposes a single natural-language app request into a DAG of skill executions.
2. A secure Text-to-SQL engine for BI/dashboard generation.
3. A real micro-frontend Remote plugin that validates the existing Host federation base.
4. An AI telemetry and cost analytics dashboard for tenant admins.
5. Architecture and memory-bank updates.

Execution remains ordered as Task 1 -> Task 5, but implementation is intentionally skeleton-first so that the riskiest concurrency and security boundaries are designed before feature depth is added.

## 2. Architecture Direction

### 2.1 Supervisor Layer

- Add a new backend subdomain under `application/supervisor`.
- `SupervisorOrchestrator` is responsible for:
  - interpreting a composite prompt,
  - validating the structured plan,
  - checking DAG legality,
  - orchestrating task execution in dependency batches,
  - coordinating compensating rollback.
- `SupervisorOrchestrator` does not generate forms, workflows, or reports directly. It delegates to existing skill execution contracts.

### 2.2 Skill Execution Reuse

- Existing `SkillExecutor` remains the execution surface for sub-tasks.
- Existing skill IDs and alias routing remain authoritative.
- Supervisor plans produce `SkillRequest` instances plus orchestration metadata rather than introducing a second generation pipeline.

### 2.3 Batch DAG Scheduling

- DAG execution uses a topological, level-by-level scheduler.
- Each batch contains only tasks whose dependencies are already complete.
- Each batch is executed with `StructuredTaskScope`, using virtual threads only.
- No task is allowed to block on another future inside the same scope; dependency edges are enforced before launching the batch.
- This avoids scope-internal waiting cycles and reduces deadlock risk.

### 2.4 Rollback Model

- Composite app generation is not one global transaction.
- Each sub-task writes independently, then records enough metadata identity/version information for compensation.
- If any downstream task fails:
  - already-committed task outputs are collected,
  - the orchestrator calls `MetadataHotReloadManager.rollback(...)` per affected metadata item,
  - rollback is performed in reverse completion order.
- This avoids long-lived cross-thread database transactions while still providing partial-failure recovery.

## 3. Task 1 Design: Supervisor Agent

### 3.1 Structured Output Contract

Supervisor output must be forced into strict JSON using Spring AI structured output.

Canonical payload:

```json
{
  "planName": "CRM system bootstrap",
  "tasks": [
    {
      "taskId": "T1",
      "skillId": "jeecg-onlform",
      "dependencies": [],
      "parameters": {
        "description": "customer form"
      }
    },
    {
      "taskId": "T2",
      "skillId": "jeecg-bpmn",
      "dependencies": ["T1"],
      "parameters": {
        "description": "follow-up workflow"
      }
    }
  ]
}
```

Required schema rules:

- `planName`: non-empty string.
- `tasks`: non-empty array.
- Each task must contain:
  - `taskId`: unique string.
  - `skillId`: registered skill string.
  - `dependencies`: array of task IDs.
  - `parameters`: object.
- No dependency may reference a missing task.
- No task may depend on itself.
- Cycles are rejected before execution.

Validator hard guards:

- During DAG validation, every dependency edge must be checked against the task registry first.
- If a dependency points to a missing task, validation must fail immediately with an `IllegalArgumentException`, for example:
  `if (!taskMap.containsKey(dep)) { throw new IllegalArgumentException(...); }`
- This is required to prevent invalid dependency references from degrading into scheduler stalls or retry loops.

Topological sorting must use `dependencies` from this schema only. No heuristic ordering is allowed.

### 3.2 Orchestration Data Model

Planned DTOs:

- `SupervisorPlan`
- `SupervisorSubTask`
- `SupervisorExecutionResult`
- `SupervisorTaskState`
- `RollbackEntry`

The execution result should distinguish:

- plan parse failure,
- DAG validation failure,
- batch execution failure,
- rollback success/failure,
- final partial or full success.

### 3.3 Scheduling Algorithm

Execution flow:

1. Parse prompt into `SupervisorPlan`.
2. Validate schema and skill registration.
3. Build adjacency and in-degree maps.
4. Produce ready-batches from zero in-degree nodes.
5. For each batch:
   - open a `StructuredTaskScope`,
   - fork one virtual-thread task per ready node,
   - join and collect all results,
   - if any fail, stop DAG progression and trigger rollback,
   - otherwise reduce in-degrees and form the next batch.

Cycle/skip protection rules:

- The scheduler must track a `processedCount` (or equivalent) for successfully dequeued / scheduled nodes.
- After topological scheduling completes, if `processedCount != totalTaskCount`, the graph must be treated as invalid and execution must fail fast.
- This guards against cases where a cycle leaves the ready queue empty and the scheduler would otherwise keep skipping batches or waiting forever.

This design intentionally avoids nested scopes waiting on sibling futures.

### 3.4 Concurrency and Deadlock Prevention

- No shared write transaction spans a whole plan.
- No `synchronized` I/O.
- Tenant and request context must flow through `ScopedValue`, not `ThreadLocal` bridges.
- Each DB-writing sub-task runs in its own bounded transaction.
- Rollback is coordinated after failure, not by keeping locks open across dependent tasks.

## 4. Task 2 Design: Text-to-SQL BI Engine

### 4.1 Generation Flow

1. Collect tenant-visible table metadata and columns.
2. Prompt LLM to produce SQL for analytics intent.
3. Parse SQL with JSQLParser.
4. Reject if the AST is not a single `Select` statement.
5. Extract table names with `TablesNamesFinder`.
6. Reject any table not in the tenant-approved metadata whitelist.
7. Execute under `@Transactional(readOnly = true)`.
8. Convert rows to ECharts `dataset` JSON.

### 4.2 AST Firewall Requirements

This component must not rely on regex.

Implementation contract:

- Parse into `net.sf.jsqlparser.statement.Statement`.
- If `!(statement instanceof Select)`, throw `SecurityException`.
- Reject:
  - `INSERT`
  - `UPDATE`
  - `DELETE`
  - `DROP`
  - `ALTER`
  - `TRUNCATE`
  - `MERGE`
  - stored procedure / call patterns
  - multi-statement chains
- Use `TablesNamesFinder` (or equivalent visitor) to enumerate referenced tables.
- Every referenced table must exist in the tenant metadata whitelist before execution.

### 4.3 Database Last-Line Defense

- Query execution must be wrapped in `@Transactional(readOnly = true)`.
- On PostgreSQL this ensures session-level read-only transaction behavior (`SET TRANSACTION READ ONLY`).
- Even if the parser layer is bypassed, the DB layer remains a write-blocking backstop.

## 5. Task 3 Design: MFE Remote Plugin

### 5.1 Remote App

- Add a standalone `frontend-remote-demo` Vite + Vue 3 project.
- Configure federation:
  - `name: 'remote_app'`
  - `filename: 'remoteEntry.js'`
  - `exposes: { './CustomWidget': './src/components/CustomWidget.vue' }`
  - `shared: ['vue']`
- Build target must be `esnext`.

### 5.2 Cross-Origin and MIME Safety

- Remote Vite dev server must set `server.cors = true`.
- Host registration must load `remoteEntry.js` as an ES module-compatible asset.
- Design must explicitly account for:
  - CORS request acceptance,
  - correct `Content-Type` / module MIME delivery,
  - browser `Strict MIME type checking` failures.

### 5.3 Isolation Rules

- Remote CSS should default to component-scoped styles.
- Shared dependency surface should stay minimal; only `vue` is shared in the first iteration.
- The Host should pass props or theme tokens explicitly rather than sharing mutable global runtime state.

## 6. Task 4 Design: AI Telemetry Dashboard

- Reuse `SkillTelemetry` and `skill_execution_log` as the data source.
- Add a backend aggregation endpoint for daily cost trend and per-model / per-skill distribution.
- Frontend analytics widgets should reuse the Task 2 ECharts integration path so BI rendering is not duplicated.

## 7. Task 5 Design: Documentation

- Update `docs/ARCHITECTURE.md` with a new section covering:
  - Supervisor DAG execution,
  - compensation rollback,
  - Text-to-SQL firewall,
  - MFE runtime integration.
- Update `.claude_memory.md` with:
  - JSQLParser introduction,
  - Supervisor data flow,
  - Phase 11 completion notes.

## 8. Testing Strategy

### 8.1 Supervisor

- Unit test DAG validation:
  - duplicate IDs,
  - missing dependencies,
  - self-dependency,
  - cycle detection.
- Scheduler tests using mock `SkillExecutor`:
  - parallel execution of independent tasks,
  - dependency gating,
  - failure short-circuit,
  - rollback trigger ordering.

### 8.2 Text-to-SQL

- Parser tests:
  - valid `SELECT`,
  - forbidden write statements,
  - multiple statements,
  - unauthorized table reference.
- Service tests:
  - ECharts dataset mapping,
  - read-only execution path.

### 8.3 MFE

- Dev-time remote loading verification at `/settings/mfe-test`.
- Confirm host can mount remote widget with props and without CSS leakage.

## 9. Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| Supervisor output not parseable | strict JSON schema via Spring AI structured output |
| DAG deadlock or invalid wait graph | topological level batching, no sibling wait-in-scope |
| Partial writes after failure | reverse-order metadata rollback via `MetadataHotReloadManager` |
| SQL injection or destructive SQL | JSQLParser AST allowlist + table whitelist + read-only transaction |
| MFE remote load failure | explicit CORS, MIME compatibility, minimal shared deps |

## 10. Immediate Next Step

After this spec is approved, create the Task 1 skeleton only:

- `application/supervisor` package
- DTOs and plan model
- `SupervisorOrchestrator` interface and initial implementation
- DAG validator and batch scheduler core
- mock-based scheduling tests

No Task 2-4 implementation should begin before Task 1 skeleton is reviewed.
