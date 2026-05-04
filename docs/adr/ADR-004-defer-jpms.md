# ADR-004: Defer JPMS (module-info.java) Until Spring Boot 4.0

**Date**: 2026-05-04
**Status**: Accepted
**Authors**: Aurora Architecture Team
**Context**: PHASE 0 Supplement — Module Boundary Enforcement

## Context

JPMS (Java Platform Module System) provides compile-time dependency enforcement via `module-info.java`. The original plan was to use JPMS to prevent circular dependencies between architecture layers.

## Decision

**Defer JPMS adoption.** Use package-level conventions and static analysis (ArchUnit tests) instead.

### Reasons
1. Spring Boot 3.4+ does not fully support JPMS — `spring-boot-starter-*` jars are automatic modules with auto-generated names
2. Many transitive dependencies (Jackson, HikariCP, Lettuce) lack proper `module-info.java`
3. `spring-boot-maven-plugin` repackages jars in a way incompatible with JPMS
4. JPMS adds significant build complexity with minimal runtime benefit for a monolith

### Alternative: ArchUnit Tests
Instead of JPMS, enforce layer boundaries via ArchUnit integration tests:
```java
@ArchTest
static final ArchRule noCircularDependencies = noClasses()
    .that().resideInAPackage("..infrastructure..")
    .should().dependOnClassesThat().resideInAPackage("..adapter..");
```

## Alternatives Considered

1. **Full JPMS** — Rejected due to ecosystem incompatibility
2. **Maven Enforcer for cycles** — Only detects cycles in Maven modules, not packages
3. **ArchUnit** — Chosen: works with classpath, detects package-level violations

## Consequences

- Module boundaries enforced via ArchUnit tests, not compiler
- Package naming conventions must be documented and reviewed
- Circular dependency detection runs in CI test phase, not compile phase
