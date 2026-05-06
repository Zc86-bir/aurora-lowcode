package com.aurora.core.adapter.web;

import com.aurora.core.contract.TenantContext;
import com.aurora.core.infrastructure.database.entity.KnowledgeDocumentEntity;
import com.aurora.core.infrastructure.database.repository.KnowledgeDocumentRepositoryJpa;
import com.aurora.core.infrastructure.knowledge.KnowledgeIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Knowledge Base", description = "Enterprise knowledge document ingestion and management")
@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeBaseController {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseController.class);

    private final KnowledgeDocumentRepositoryJpa documentRepository;
    private final KnowledgeIngestionService ingestionService;
    private final TenantContext tenantContext;

    public KnowledgeBaseController(KnowledgeDocumentRepositoryJpa documentRepository,
                                    KnowledgeIngestionService ingestionService,
                                    TenantContext tenantContext) {
        this.documentRepository = documentRepository;
        this.ingestionService = ingestionService;
        this.tenantContext = tenantContext;
    }

    @Operation(summary = "Upload a knowledge document",
               description = "Accepts PDF, TXT, MD, DOCX, and HTML files. Ingestion runs asynchronously.")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Accepted for ingestion"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam("scope") String scope,
            @RequestParam(value = "projectId", required = false) String projectId,
            @RequestParam(value = "moduleId", required = false) String moduleId,
            @RequestParam("visibilityPolicy") String visibilityPolicy) {

        UUID tenantId = tenantContext.getCurrentTenantId();
        if (tenantId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }

        try {
            byte[] fileBytes = file.getBytes();
            String checksum = sha256(fileBytes);
            String sourceType = detectSourceType(file.getOriginalFilename());

            KnowledgeDocumentEntity document = new KnowledgeDocumentEntity();
            document.setId(UUID.randomUUID());
            document.setTenantId(tenantId);
            document.setProjectId(projectId);
            document.setModuleId(moduleId);
            document.setKnowledgeScope(scope);
            document.setSourceType(sourceType);
            document.setTitle(file.getOriginalFilename());
            document.setSourceUri("upload://" + file.getOriginalFilename());
            document.setChecksum(checksum);
            document.setVisibilityPolicy(visibilityPolicy);
            document.setStatus("PENDING");
            document.setCreatedBy(String.valueOf(tenantContext.getCurrentUserId()));
            documentRepository.save(document);

            ingestionService.ingestAsync(document.getId());

            log.info("Document uploaded: {} tenant={} scope={}", document.getId(), tenantId, scope);

            return ResponseEntity.accepted().body(Map.of(
                    "documentId", document.getId().toString(),
                    "status", "PENDING",
                    "checksum", checksum,
                    "title", document.getTitle()
            ));
        } catch (Exception e) {
            log.error("Upload failed: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }

    @Operation(summary = "Import a knowledge document from URL")
    @PostMapping(path = "/import-url")
    public ResponseEntity<Map<String, Object>> importUrl(@RequestBody Map<String, String> body) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        if (tenantId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        String url = body.get("url");
        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "URL is required"));
        }

        String scope = body.getOrDefault("scope", "TENANT");
        String projectId = body.get("projectId");
        String moduleId = body.get("moduleId");
        String visibilityPolicy = body.getOrDefault("visibilityPolicy", "authenticated");

        try {
            String checksum = sha256(url.getBytes(StandardCharsets.UTF_8));

            KnowledgeDocumentEntity document = new KnowledgeDocumentEntity();
            document.setId(UUID.randomUUID());
            document.setTenantId(tenantId);
            document.setProjectId(projectId);
            document.setModuleId(moduleId);
            document.setKnowledgeScope(scope);
            document.setSourceType("URL");
            document.setTitle(url);
            document.setSourceUri(url);
            document.setChecksum(checksum);
            document.setVisibilityPolicy(visibilityPolicy);
            document.setStatus("PENDING");
            document.setCreatedBy(String.valueOf(tenantContext.getCurrentUserId()));
            documentRepository.save(document);

            ingestionService.ingestAsync(document.getId());

            return ResponseEntity.accepted().body(Map.of(
                    "documentId", document.getId().toString(),
                    "status", "PENDING"
            ));
        } catch (Exception e) {
            log.error("URL import failed: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Import failed: " + e.getMessage()));
        }
    }

    @Operation(summary = "List knowledge documents for current tenant")
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listDocuments() {
        UUID tenantId = tenantContext.getCurrentTenantId();
        if (tenantId == null) {
            return ResponseEntity.status(401).body(List.of());
        }

        List<KnowledgeDocumentEntity> docs = documentRepository.findByTenantId(tenantId);
        List<Map<String, Object>> result = docs.stream()
                .map(d -> Map.<String, Object>of(
                        "id", d.getId().toString(),
                        "title", d.getTitle(),
                        "scope", d.getKnowledgeScope(),
                        "sourceType", d.getSourceType(),
                        "status", d.getStatus(),
                        "failureMessage", d.getFailureMessage() != null ? d.getFailureMessage() : "",
                        "createdAt", d.getCreatedAt().toString()
                ))
                .toList();

        return ResponseEntity.ok(result);
    }

    private static String sha256(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private static String detectSourceType(String filename) {
        if (filename == null) return "UNKNOWN";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) return "PDF";
        if (lower.endsWith(".txt")) return "TXT";
        if (lower.endsWith(".md")) return "MD";
        if (lower.endsWith(".docx")) return "DOCX";
        if (lower.endsWith(".doc")) return "DOCX";
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "HTML";
        return "UNKNOWN";
    }
}
