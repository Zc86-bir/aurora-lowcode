-- V6: pgvector extension + knowledge tables + index
-- Dev-safe: falls back when pgvector extension is unavailable.

DO $$
BEGIN
    BEGIN
        CREATE EXTENSION IF NOT EXISTS vector;
    EXCEPTION
        WHEN OTHERS THEN
            RAISE NOTICE 'pgvector extension unavailable: %', SQLERRM;
    END;
END
$$;

CREATE TABLE IF NOT EXISTS tenant_knowledge_document (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL,
    project_id        VARCHAR(128),
    module_id         VARCHAR(128),
    knowledge_scope   VARCHAR(16) NOT NULL,
    source_type       VARCHAR(16) NOT NULL,
    title             VARCHAR(512) NOT NULL,
    source_uri        VARCHAR(2048),
    checksum          VARCHAR(64) NOT NULL,
    visibility_policy VARCHAR(64) NOT NULL,
    status            VARCHAR(16) NOT NULL,
    failure_message   VARCHAR(1024),
    created_by        VARCHAR(128) NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    version_lock      INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS tenant_knowledge_chunk (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id       UUID NOT NULL REFERENCES tenant_knowledge_document(id) ON DELETE CASCADE,
    tenant_id         UUID NOT NULL,
    project_id        VARCHAR(128),
    module_id         VARCHAR(128),
    knowledge_scope   VARCHAR(16) NOT NULL,
    chunk_index       INTEGER NOT NULL,
    content           TEXT NOT NULL,
    token_count       INTEGER NOT NULL,
    visibility_policy VARCHAR(64) NOT NULL,
    semantic_tags     JSONB,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(document_id, chunk_index)
);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector') THEN
        CREATE TABLE IF NOT EXISTS vector_store (
            id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            tenant_id         UUID NOT NULL,
            chunk_id          UUID NOT NULL REFERENCES tenant_knowledge_chunk(id) ON DELETE CASCADE,
            project_id        VARCHAR(128),
            module_id         VARCHAR(128),
            knowledge_scope   VARCHAR(16) NOT NULL,
            visibility_policy VARCHAR(64) NOT NULL,
            content           TEXT NOT NULL,
            embedding         vector(1536) NOT NULL,
            created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
        );
    ELSE
        CREATE TABLE IF NOT EXISTS vector_store (
            id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            tenant_id         UUID NOT NULL,
            chunk_id          UUID NOT NULL REFERENCES tenant_knowledge_chunk(id) ON DELETE CASCADE,
            project_id        VARCHAR(128),
            module_id         VARCHAR(128),
            knowledge_scope   VARCHAR(16) NOT NULL,
            visibility_policy VARCHAR(64) NOT NULL,
            content           TEXT NOT NULL,
            embedding         TEXT NOT NULL,
            created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
        );
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_knowledge_document_tenant ON tenant_knowledge_document(tenant_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_tenant_scope ON tenant_knowledge_chunk(tenant_id, knowledge_scope);
CREATE INDEX IF NOT EXISTS idx_vector_store_tenant_scope ON vector_store(tenant_id, knowledge_scope, visibility_policy);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector') THEN
        CREATE INDEX IF NOT EXISTS idx_vector_store_embedding_hnsw
            ON vector_store USING hnsw (embedding vector_cosine_ops);
    END IF;
END
$$;
