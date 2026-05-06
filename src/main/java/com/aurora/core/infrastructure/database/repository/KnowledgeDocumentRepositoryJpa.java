package com.aurora.core.infrastructure.database.repository;

import com.aurora.core.infrastructure.database.entity.KnowledgeDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KnowledgeDocumentRepositoryJpa extends JpaRepository<KnowledgeDocumentEntity, UUID> {

    List<KnowledgeDocumentEntity> findByTenantId(UUID tenantId);

    Optional<KnowledgeDocumentEntity> findByTenantIdAndChecksum(UUID tenantId, String checksum);
}
