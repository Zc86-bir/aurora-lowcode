# Phase 10 Enterprise RAG Design

## Status
- Derived from approved Phase 10 V1.1 blueprint
- Track: Enterprise RAG

## Objective
Enable Aurora to retrieve tenant-private knowledge and inject it into AI workflows without breaking multi-tenant isolation or existing validation controls.

## Primary Use Cases
- code generation aligned with enterprise conventions
- Copilot responses grounded in tenant documents
- future extension points for broader skill execution

## Knowledge Model

### Knowledge Spaces
- `TENANT`: organization-wide rules and standards
- `PROJECT`: app or product-line specific conventions
- `MODULE`: local rules for forms, reports, workflows, components, or features

### Authorization Model
Chosen boundary: `tenant + knowledge space + role`.

Retrieval must always filter by:
- tenant
- allowed space hierarchy
- current user role or visibility policy

## Data Model

### `tenant_knowledge_document`
Document-level metadata:
- `id`
- `tenant_id`
- `project_id` nullable
- `module_id` nullable
- `knowledge_scope`
- `source_type`
- `title`
- `source_uri` or storage path
- `status`
- `visibility_policy`
- `checksum`
- `created_by`
- `created_at`
- `updated_at`
- `failure_message` nullable

### `tenant_knowledge_chunk`
Chunk-level searchable content:
- `id`
- `document_id`
- `tenant_id`
- `project_id` nullable
- `module_id` nullable
- `knowledge_scope`
- `chunk_index`
- `content`
- `token_count`
- `visibility_policy`
- `semantic_tags` JSONB nullable
- `created_at`

### `vector_store`
Embedding rows for similarity search:
- `id`
- `tenant_id`
- `chunk_id`
- `embedding vector(...)`
- metadata fields needed for filtering

Use HNSW index for similarity search and always preserve metadata fields needed for tenant and space filtering.

## Retrieval Policy

### Search Defaults
- `topK = 3`
- `similarity threshold = 0.75`
- module-level preferred over project-level, project-level preferred over tenant-level
- empty result falls back to normal generation path without error

### Retrieval Control Model
Chosen model: mixed.

Behavior:
- Host automatically resolves default search scope from request context.
- Advanced users may request override.
- Host still validates and constrains override to allowed scopes.

## Vector Store Boundary

### Chosen Approach
Introduce `TenantAwareVectorStore` as a decorator around Spring AI pgvector access.

Responsibilities:
- inject mandatory tenant filter on every similarity search
- inject knowledge-space and visibility filters
- deny searches when tenant context is missing
- centralize retrieval policy so callers cannot bypass filtering

### Hard Rule
No caller may pass a raw similarity query directly to pgvector without going through `TenantAwareVectorStore`.

## Ingestion Pipeline

### Supported Inputs In V1.1
- PDF
- TXT
- MD
- DOCX
- HTML
- URL / webpage import

### Pipeline Stages
1. `PENDING`
2. `PARSING`
3. `SPLITTING`
4. `EMBEDDING`
5. `COMPLETED`
6. `FAILED`

### Implementation Shape
- `KnowledgeBaseController` receives upload/import request.
- `KnowledgeIngestionService` persists document metadata immediately.
- Background processing runs asynchronously using `StructuredTaskScope`-driven orchestration.
- Each stage updates durable state in PostgreSQL.
- On completion, publish `KnowledgeIngestedEvent`.

## AI Integration Points

### V1.1 Active Targets
- code generation prompt path
- Copilot conversation prompt path

### Injection Point
Retrieve before prompt rendering.

Flow:
1. resolve retrieval context
2. fetch relevant chunks
3. assemble `enterprise_context`
4. inject into prompt template
5. run existing LLM + AST + schema + business-rule pipeline unchanged

## Prompt Contract
`code_generator_v1.0.0.j2` gains:
- `enterprise_context`
- optional retrieval provenance metadata if useful for debugging

Prompt contract must remain backward-compatible when no retrieved context exists.

## Testing Strategy

### Isolation Tests
- tenant filter cannot be bypassed
- project/module boundaries are respected
- role-based visibility is enforced

### Ingestion Tests
- status progression is durable
- failed parse or embedding writes `FAILED`
- duplicate checksum policy is explicit and deterministic

### RAG Pipeline Tests
- retrieved context is injected into prompt
- no-result fallback leaves generation path intact
- validation pipeline still rejects invalid model output

## Observability
- ingestion lifecycle metrics per stage
- retrieval hit/miss counters
- average similarity score
- prompt augmentation size and truncation counters
- per-tenant ingestion failure counts

## Risks And Mitigations
- cross-tenant leakage: centralize search in `TenantAwareVectorStore`
- irrelevant retrieval: top-k cap, threshold, and scope priority
- oversized prompt context: strict byte/token cap and truncation
- long-running ingestion: asynchronous execution and durable status machine

## Deliverables
- Flyway `V6__init_pgvector.sql`
- pgvector Spring AI starter wiring
- `VectorStoreConfig.java`
- `TenantAwareVectorStore`
- `KnowledgeDocumentEntity` and repository
- `KnowledgeIngestionService`
- `KnowledgeBaseController`
- prompt template update
- `RagPipelineIntegrationTest`
