# ADR-001: Use Java 25 Virtual Threads for Concurrent Request Handling

**Date**: 2026-05-03
**Status**: Accepted
**Authors**: Aurora Architecture Team
**Context**: PHASE 0 — Architecture Foundation

## Context

The platform requires high-throughput concurrent request handling for:
- AI skill execution (parallel LLM calls)
- Form/report rendering (concurrent data fetching)
- Workflow parallel gateways (BPMN node execution)
- Event handler dispatch

Traditional thread pool models (fixed-size `ExecutorService`) require careful tuning of pool sizes per workload type. Misconfiguration leads to either thread starvation or memory exhaustion.

## Decision

Adopt Java 25 Virtual Threads (Project Loom) as the default concurrency model for all I/O-bound operations.

### Implementation Rules
- All event handlers execute via `Executors.newVirtualThreadPerTaskExecutor()`
- Database connections use HikariCP sized for virtual threads (max 50-100, not 1000+)
- Lock providers use `Thread.yield()` instead of `Thread.sleep()` for spinning
- Tenant context propagation via `ScopedValue` or `ThreadLocal` with explicit capture/restore

### Trade-offs

| Aspect | Impact |
|--------|--------|
| JDK version | Requires JDK 25+ — drops support for JDK 17/21 |
| Memory | ~1KB per virtual thread vs ~1MB for platform threads |
| CPU-bound tasks | Virtual threads offer no benefit; use platform thread pools |
| Pinning | `synchronized` blocks pin virtual threads to carrier threads; use `ReentrantLock` |

## Alternatives Considered

1. **Fixed ThreadPool per workload** — Requires manual tuning, hard to scale
2. **CompletableFuture with common pool** — Shared pool risks starvation
3. **Reactive (WebFlux)** — Steep learning curve, incompatible with JPA/Hibernate

## Consequences

- `application.yml`: `spring.threads.virtual.enabled: true`
- All `@Async` methods default to virtual thread executor
- Lock implementations avoid `synchronized` — use `ReentrantLock` or `StampedLock`
