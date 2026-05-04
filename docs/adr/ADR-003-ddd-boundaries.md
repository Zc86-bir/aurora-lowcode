# ADR-003: DDD Tactical Patterns as Interface Contracts, Not Runtime Framework

**Date**: 2026-05-03
**Status**: Accepted
**Authors**: Aurora Architecture Team
**Context**: PHASE 0 — Layer Boundary Definition

## Context

Traditional DDD implementations embed tactical patterns (AggregateRoot, Entity, Repository, DomainEvent) as base classes or framework code. This creates tight coupling between the domain model and infrastructure, making it difficult to:
- Swap persistence technologies (JPA → JDBC → NoSQL)
- Test domain logic in isolation
- Reuse domain contracts across bounded contexts

Low-code platforms face additional complexity: metadata IS the domain model, but it's generated at runtime, not compiled.

## Decision

Implement DDD tactical patterns as **pure Java interfaces** in `com.aurora.core.architecture`, with NO implementation details:

```
architecture/
  AggregateRoot<ID>     — interface only
  DomainEvent           — sealed interface only
  Entity<ID>            — interface only
  ValueObject           — interface only
  Repository<T, ID>     — interface only
  Specification<T>      — interface only
  UseCase<R, T>         — interface only
```

Implementations live in `infrastructure/` and `application/` layers, satisfying the contracts.

### Boundary Rules
- `contract/` depends on NOTHING (pure interfaces)
- `architecture/` depends on NOTHING (pure interfaces)
- `application/` depends on `contract/` + `architecture/`
- `infrastructure/` depends on `contract/` + `architecture/`
- `adapter/` depends on `application/` + `infrastructure/` (via DI)
- NO circular dependencies enforced by JPMS `module-info.java`

### Low-Code Metadata Handling
Metadata aggregates (`MetadataAggregate.FormMetadata`, etc.) implement `AggregateRoot` interface but are `record` types, not JPA entities. They are serialized/deserialized via JSON, not ORM.

## Trade-offs

| Aspect | Impact |
|--------|--------|
| Boilerplate | More files (interface + implementation per pattern) |
| Type safety | Records provide compile-time guarantees for metadata |
| Testability | Domain logic testable with mock implementations |
| Flexibility | Infrastructure can change without touching domain |

## Alternatives Considered

1. **Base classes with shared code** — Tight coupling, hard to swap
2. **Annotations-only (@Entity, @Aggregate)** — Runtime reflection, no compile-time safety
3. **Full framework (Axon, Eventuate)** — Heavy, opinionated, steep learning curve

## Consequences

- `module-info.java` enforces one-way dependency flow
- No `@Entity` annotations on architecture interfaces
- Metadata records implement contracts, not extend base classes
- Spring Data repositories implement `Repository<T, ID>` interface
