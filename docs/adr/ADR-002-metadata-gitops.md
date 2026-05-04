# ADR-002: Metadata GitOps for Version-Controlled Configuration

**Date**: 2026-05-03
**Status**: Accepted
**Authors**: Aurora Architecture Team
**Context**: PHASE 0 — Metadata Architecture

## Context

Low-code platforms manage metadata (forms, reports, workflows, permissions) as runtime configuration. Traditional approaches store metadata in database tables with manual version tracking, leading to:
- No audit trail of who changed what and when
- No ability to rollback to a previous configuration
- No diff between versions
- Configuration drift between environments (dev → test → prod)

## Decision

Implement GitOps-style metadata versioning where every metadata change:
1. Generates a SHA-256 checksum of the metadata content
2. Records the change with `version`, `checksum`, `changedBy`, `changedAt`
3. Supports rollback to any previous version
4. Provides diff computation between versions

### Implementation
- `MetadataRepository` exposes `getVersions()`, `computeDiff()`, `rollback()`
- `MetadataHotReloadManager` performs atomic swap with checksum validation
- All metadata aggregates include `version`, `checksum`, `createdBy`, `updatedBy` fields
- Export/import metadata as YAML for external Git tracking

### Schema Pattern
```sql
metadata_version (
    id UUID,
    metadata_id UUID,
    version INT,
    checksum CHAR(64),
    content JSONB,
    changed_by UUID,
    changed_at TIMESTAMP
)
```

## Trade-offs

| Aspect | Impact |
|--------|--------|
| Storage | Each change creates a new version row (mitigated by retention policy) |
| Performance | Hot-reload uses in-memory checksum cache, no DB hit on read |
| Complexity | Diff engine requires JSON tree comparison |

## Alternatives Considered

1. **Database audit triggers** — Opaque, no application-level control
2. **Event sourcing (all changes as events)** — Overkill for metadata, high storage cost
3. **No versioning (last-write-wins)** — Unacceptable for enterprise use

## Consequences

- `MetadataAggregate` sealed interface includes version and checksum
- `MetadataHotReloadManager` detects changes via checksum comparison
- Export endpoint serializes metadata to YAML for Git commit
