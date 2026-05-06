package com.aurora.core.infrastructure.database.repository;

import com.aurora.core.infrastructure.database.entity.KnowledgeChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KnowledgeChunkRepositoryJpa extends JpaRepository<KnowledgeChunkEntity, UUID> {

    List<KnowledgeChunkEntity> findByDocumentIdOrderByChunkIndexAsc(UUID documentId);
}
