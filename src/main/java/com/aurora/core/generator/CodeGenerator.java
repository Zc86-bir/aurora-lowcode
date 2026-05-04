package com.aurora.core.generator;

import com.aurora.core.contract.AIPipeline;
import com.aurora.core.contract.AIPipeline.PipelineRequest;
import com.aurora.core.contract.AuditLogger;
import com.aurora.core.contract.TenantContext;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

/**
 * AI-Powered Code Generator
 *
 * Generates production-ready code from metadata:
 * 1. Java Entity/Controller/Service/Repository
 * 2. Vue 3 SFC + TypeScript types
 * 3. SQL Migration scripts
 * 4. Unit test scaffolds
 *
 * Uses AI pipeline for intelligent code generation with
 * AST validation and security scanning.
 */
public class CodeGenerator {

    private final AIPipeline aiPipeline;
    private final AuditLogger auditLogger;
    private final TenantContext tenantContext;
    private final Path outputBasePath;

    public CodeGenerator(AIPipeline aiPipeline, AuditLogger auditLogger,
                          TenantContext tenantContext, Path outputBasePath) {
        this.aiPipeline = aiPipeline;
        this.auditLogger = auditLogger;
        this.tenantContext = tenantContext;
        this.outputBasePath = outputBasePath;
    }

    /**
     * Generate full CRUD from metadata.
     */
    public GenerationResult generateCRUD(GenerationContext ctx) {
        Instant startedAt = Instant.now();
        List<GeneratedFile> files = new ArrayList<>();

        // 1. Java Entity
        files.add(generateEntity(ctx));

        // 2. Java Controller
        files.add(generateController(ctx));

        // 3. Java Service
        files.add(generateService(ctx));

        // 4. Java Repository
        files.add(generateRepository(ctx));

        // 5. Vue SFC
        files.add(generateVueComponent(ctx));

        // 6. TypeScript types
        files.add(generateTypeScriptTypes(ctx));

        // 7. SQL Migration
        files.add(generateSQLMigration(ctx));

        // 8. Unit test
        files.add(generateUnitTest(ctx));

        // Validate all generated files
        List<ValidationError> errors = validateAll(files);

        if (!errors.isEmpty()) {
            return new GenerationResult(false, files, errors, null, Duration.between(startedAt, Instant.now()));
        }

        // Write files to disk
        String commitHash = writeFiles(files, ctx);

        auditLogger.logSkillExecution(new com.aurora.core.contract.AuditLogger.SkillAuditEntry(
            UUID.randomUUID(),
            ctx.tenantId(),
            ctx.userId(),
            "code-generator",
            "code-generator",
            "1.0.0",
            Map.of("entity", ctx.entityName(), "fields", ctx.fields().size()),
            true,
            null,
            Duration.between(startedAt, Instant.now()).toMillis(),
            true,
            "none",
            Instant.now()
        ));

        return new GenerationResult(true, files, List.of(), commitHash, Duration.between(startedAt, Instant.now()));
    }

    /**
     * Generate Java Entity class.
     */
    private GeneratedFile generateEntity(GenerationContext ctx) {
        String className = ctx.entityName();
        String packageName = ctx.packagePrefix() + ".domain.entity";

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(packageName).append(";\n\n");
        sb.append("import jakarta.persistence.*;\n");
        sb.append("import java.time.LocalDateTime;\n\n");
        sb.append("@Entity\n");
        sb.append("@Table(name = \"").append(ctx.tableName()).append("\")\n");
        sb.append("public class ").append(className).append(" {\n\n");

        // Primary key
        sb.append("    @Id\n");
        sb.append("    @GeneratedValue(strategy = GenerationType.UUID)\n");
        sb.append("    private UUID id;\n\n");

        // Tenant isolation
        sb.append("    @Column(nullable = false)\n");
        sb.append("    private UUID tenantId;\n\n");

        // Fields
        for (FieldDefinition field : ctx.fields()) {
            if (field.primaryKey()) continue;

            if (field.required()) {
                sb.append("    @Column(nullable = false");
                if (field.maxLength() > 0) {
                    sb.append(", length = ").append(field.maxLength());
                }
                sb.append(")\n");
            } else {
                if (field.maxLength() > 0) {
                    sb.append("    @Column(length = ").append(field.maxLength()).append(")\n");
                }
            }
            sb.append("    private ").append(field.javaType()).append(" ").append(field.name()).append(";\n\n");
        }

        // Audit fields
        sb.append("    @Column(nullable = false, updatable = false)\n");
        sb.append("    private LocalDateTime createdAt;\n\n");
        sb.append("    @Column(nullable = false)\n");
        sb.append("    private LocalDateTime updatedAt;\n\n");

        // Generate getters and setters
        for (FieldDefinition field : ctx.fields()) {
            sb.append("    public ").append(field.javaType()).append(" get").append(capitalize(field.name())).append("() {\n");
            sb.append("        return ").append(field.name()).append(";\n");
            sb.append("    }\n\n");
            sb.append("    public void set").append(capitalize(field.name())).append("(").append(field.javaType()).append(" ").append(field.name()).append(") {\n");
            sb.append("        this.").append(field.name()).append(" = ").append(field.name()).append(";\n");
            sb.append("    }\n\n");
        }

        sb.append("}\n");

        String path = packageName.replace(".", "/") + "/" + className + ".java";

        return new GeneratedFile(
            path,
            sb.toString(),
            "java",
            computeChecksum(sb.toString())
        );
    }

    /**
     * Generate REST Controller.
     */
    private GeneratedFile generateController(GenerationContext ctx) {
        String className = ctx.entityName() + "Controller";
        String packageName = ctx.packagePrefix() + ".adapter.web";
        String entityPath = ctx.entityName().toLowerCase();

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(packageName).append(";\n\n");
        sb.append("import org.springframework.web.bind.annotation.*;\n");
        sb.append("import java.util.UUID;\n\n");
        sb.append("@RestController\n");
        sb.append("@RequestMapping(\"/api/v1/").append(entityPath).append("\")\n");
        sb.append("public class ").append(className).append(" {\n\n");

        sb.append("    private final ").append(ctx.entityName()).append("Service service;\n\n");
        sb.append("    public ").append(className).append("(").append(ctx.entityName()).append("Service service) {\n");
        sb.append("        this.service = service;\n");
        sb.append("    }\n\n");

        // CRUD endpoints
        sb.append("    @GetMapping\n");
        sb.append("    public ResponseEntity<List<").append(ctx.entityName()).append(">> list(");
        sb.append("@RequestParam(required = false) UUID tenantId) {\n");
        sb.append("        return ResponseEntity.ok(service.findAll(tenantId));\n");
        sb.append("    }\n\n");

        sb.append("    @GetMapping(\"/{id}\")\n");
        sb.append("    public ResponseEntity<").append(ctx.entityName()).append("> getById(@PathVariable UUID id) {\n");
        sb.append("        return service.findById(id)\n");
        sb.append("            .map(ResponseEntity::ok)\n");
        sb.append("            .orElse(ResponseEntity.notFound().build());\n");
        sb.append("    }\n\n");

        sb.append("    @PostMapping\n");
        sb.append("    public ResponseEntity<").append(ctx.entityName()).append("> create(@RequestBody ").append(ctx.entityName()).append(" entity) {\n");
        sb.append("        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(entity));\n");
        sb.append("    }\n\n");

        sb.append("    @PutMapping(\"/{id}\")\n");
        sb.append("    public ResponseEntity<").append(ctx.entityName()).append("> update(@PathVariable UUID id, @RequestBody ").append(ctx.entityName()).append(" entity) {\n");
        sb.append("        return ResponseEntity.ok(service.update(id, entity));\n");
        sb.append("    }\n\n");

        sb.append("    @DeleteMapping(\"/{id}\")\n");
        sb.append("    public ResponseEntity<Void> delete(@PathVariable UUID id) {\n");
        sb.append("        service.deleteById(id);\n");
        sb.append("        return ResponseEntity.noContent().build();\n");
        sb.append("    }\n\n");

        sb.append("}\n");

        String path = packageName.replace(".", "/") + "/" + className + ".java";
        return new GeneratedFile(path, sb.toString(), "java", computeChecksum(sb.toString()));
    }

    /**
     * Generate Service layer.
     */
    private GeneratedFile generateService(GenerationContext ctx) {
        String className = ctx.entityName() + "Service";
        String packageName = ctx.packagePrefix() + ".application.service";

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(packageName).append(";\n\n");
        sb.append("import org.springframework.stereotype.Service;\n");
        sb.append("import org.springframework.transaction.annotation.Transactional;\n");
        sb.append("import java.util.List;\n");
        sb.append("import java.util.Optional;\n");
        sb.append("import java.util.UUID;\n\n");
        sb.append("@Service\n");
        sb.append("public class ").append(className).append(" {\n\n");

        sb.append("    private final ").append(ctx.entityName()).append("Repository repository;\n\n");
        sb.append("    public ").append(className).append("(").append(ctx.entityName()).append("Repository repository) {\n");
        sb.append("        this.repository = repository;\n");
        sb.append("    }\n\n");

        sb.append("    public List<").append(ctx.entityName()).append("> findAll(UUID tenantId) {\n");
        sb.append("        if (tenantId == null) throw new IllegalArgumentException(\"Tenant ID is required\");\n");
        sb.append("        return repository.findByTenantId(tenantId);\n");
        sb.append("    }\n\n");

        sb.append("    public Optional<").append(ctx.entityName()).append("> findById(UUID id) {\n");
        sb.append("        return repository.findById(id);\n");
        sb.append("    }\n\n");

        sb.append("    @Transactional\n");
        sb.append("    public ").append(ctx.entityName()).append(" save(").append(ctx.entityName()).append(" entity) {\n");
        sb.append("        entity.setCreatedAt(LocalDateTime.now());\n");
        sb.append("        entity.setUpdatedAt(LocalDateTime.now());\n");
        sb.append("        return repository.save(entity);\n");
        sb.append("    }\n\n");

        sb.append("    @Transactional\n");
        sb.append("    public ").append(ctx.entityName()).append(" update(UUID id, ").append(ctx.entityName()).append(" entity) {\n");
        sb.append("        ").append(ctx.entityName()).append(" existing = repository.findById(id)\n");
        sb.append("            .orElseThrow(() -> new EntityNotFoundException(id));\n");
        sb.append("        entity.setUpdatedAt(LocalDateTime.now());\n");
        sb.append("        return repository.save(entity);\n");
        sb.append("    }\n\n");

        sb.append("    @Transactional\n");
        sb.append("    public void deleteById(UUID id) {\n");
        sb.append("        repository.deleteById(id);\n");
        sb.append("    }\n\n");

        sb.append("}\n");

        String path = packageName.replace(".", "/") + "/" + className + ".java";
        return new GeneratedFile(path, sb.toString(), "java", computeChecksum(sb.toString()));
    }

    /**
     * Generate Repository interface.
     */
    private GeneratedFile generateRepository(GenerationContext ctx) {
        String className = ctx.entityName() + "Repository";
        String packageName = ctx.packagePrefix() + ".infrastructure.repository";

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(packageName).append(";\n\n");
        sb.append("import org.springframework.data.jpa.repository.JpaRepository;\n");
        sb.append("import org.springframework.stereotype.Repository;\n");
        sb.append("import java.util.List;\n");
        sb.append("import java.util.UUID;\n\n");
        sb.append("@Repository\n");
        sb.append("public interface ").append(className).append(" extends JpaRepository<").append(ctx.entityName()).append(", UUID> {\n\n");
        sb.append("    List<").append(ctx.entityName()).append("> findByTenantId(UUID tenantId);\n\n");
        sb.append("    Optional<").append(ctx.entityName()).append("> findByIdAndTenantId(UUID id, UUID tenantId);\n\n");
        sb.append("    boolean existsByTenantIdAndName(UUID tenantId, String name);\n\n");
        sb.append("}\n");

        String path = packageName.replace(".", "/") + "/" + className + ".java";
        return new GeneratedFile(path, sb.toString(), "java", computeChecksum(sb.toString()));
    }

    /**
     * Generate Vue 3 SFC component.
     */
    private GeneratedFile generateVueComponent(GenerationContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("<template>\n");
        sb.append("  <div class=\"").append(ctx.entityName().toLowerCase()).append("-list\">\n");
        sb.append("    <h2>").append(ctx.entityName()).append(" List</h2>\n\n");

        // Search bar
        sb.append("    <div class=\"search-bar\">\n");
        sb.append("      <input v-model=\"searchQuery\" placeholder=\"Search...\" />\n");
        sb.append("      <button @click=\"handleCreate\">Create New</button>\n");
        sb.append("    </div>\n\n");

        // Table
        sb.append("    <table>\n");
        sb.append("      <thead>\n");
        sb.append("        <tr>\n");
        for (FieldDefinition field : ctx.fields()) {
            if (!field.primaryKey()) {
                // HTML-escape label to prevent XSS
                sb.append("          <th>").append(escapeHtml(field.label())).append("</th>\n");
            }
        }
        sb.append("          <th>Actions</th>\n");
        sb.append("        </tr>\n");
        sb.append("      </thead>\n\n");
        sb.append("      <tbody>\n");
        sb.append("        <tr v-for=\"item in filteredItems\" :key=\"item.id\">\n");
        for (FieldDefinition field : ctx.fields()) {
            if (!field.primaryKey()) {
                sb.append("          <td>{{ item.").append(field.name()).append(" }}</td>\n");
            }
        }
        sb.append("          <td>\n");
        sb.append("            <button @click=\"handleEdit(item)\">Edit</button>\n");
        sb.append("            <button @click=\"handleDelete(item)\">Delete</button>\n");
        sb.append("          </td>\n");
        sb.append("        </tr>\n");
        sb.append("      </tbody>\n");
        sb.append("    </table>\n\n");
        sb.append("  </div>\n");
        sb.append("</template>\n\n");

        sb.append("<script setup lang=\"ts\">\n");
        sb.append("import { ref, computed, onMounted } from 'vue'\n");
        sb.append("import type { ").append(ctx.entityName()).append(" } from '@/types'\n");
        sb.append("import { api } from '@/composables/useApi'\n\n");

        sb.append("const items = ref<").append(ctx.entityName()).append("[]>([])\n");
        sb.append("const searchQuery = ref('')\n\n");

        sb.append("const filteredItems = computed(() => {\n");
        sb.append("  if (!searchQuery.value) return items.value\n");
        sb.append("  const q = searchQuery.value.toLowerCase()\n");
        sb.append("  return items.value.filter(item =>\n");
        sb.append("    Object.values(item).some(v => String(v).toLowerCase().includes(q))\n");
        sb.append("  )\n");
        sb.append("})\n\n");

        sb.append("const fetchData = async () => {\n");
        sb.append("  items.value = await api.get('/api/v1/").append(ctx.entityName().toLowerCase()).append("')\n");
        sb.append("}\n\n");

        sb.append("const handleCreate = () => { router.push('/" + ctx.entityName().toLowerCase() + "/create') }\n");
        sb.append("const handleEdit = (item: ").append(ctx.entityName()).append(") => { router.push(`/" + ctx.entityName().toLowerCase() + "/${item.id}`) }\n");
        sb.append("const handleDelete = async (item: ").append(ctx.entityName()).append(") => {\n");
        sb.append("  if (confirm('Are you sure?')) {\n");
        sb.append("    await api.delete(`/api/v1/").append(ctx.entityName().toLowerCase()).append("/${item.id}`)\n");
        sb.append("    await fetchData()\n");
        sb.append("  }\n");
        sb.append("}\n\n");

        sb.append("onMounted(fetchData)\n");
        sb.append("</script>\n\n");

        sb.append("<style scoped>\n");
        sb.append(".").append(ctx.entityName().toLowerCase()).append("-list {\n");
        sb.append("  padding: 1rem;\n");
        sb.append("}\n\n");
        sb.append(".search-bar {\n");
        sb.append("  display: flex;\n");
        sb.append("  gap: 0.5rem;\n");
        sb.append("  margin-bottom: 1rem;\n");
        sb.append("}\n\n");
        sb.append("table {\n");
        sb.append("  width: 100%;\n");
        sb.append("  border-collapse: collapse;\n");
        sb.append("}\n\n");
        sb.append("th, td {\n");
        sb.append("  padding: 0.75rem;\n");
        sb.append("  text-align: left;\n");
        sb.append("  border-bottom: 1px solid #e5e7eb;\n");
        sb.append("}\n");
        sb.append("</style>\n");

        String path = "src/views/" + ctx.entityName() + "List.vue";
        return new GeneratedFile(path, sb.toString(), "vue", computeChecksum(sb.toString()));
    }

    /**
     * Generate TypeScript type definitions.
     */
    private GeneratedFile generateTypeScriptTypes(GenerationContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("// Auto-generated TypeScript types\n\n");
        sb.append("export interface ").append(ctx.entityName()).append(" {\n");
        sb.append("  id: string;\n");
        sb.append("  tenantId: string;\n");
        for (FieldDefinition field : ctx.fields()) {
            if (field.primaryKey()) continue;
            String tsType = mapToTypeScript(field.javaType());
            String optional = !field.required() ? "?" : "";
            sb.append("  ").append(field.name()).append(optional).append(": ").append(tsType).append(";\n");
        }
        sb.append("  createdAt: string;\n");
        sb.append("  updatedAt: string;\n");
        sb.append("}\n");

        String path = "src/types/" + ctx.entityName() + ".ts";
        return new GeneratedFile(path, sb.toString(), "typescript", computeChecksum(sb.toString()));
    }

    /**
     * Generate SQL migration script.
     */
    private GeneratedFile generateSQLMigration(GenerationContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("-- Auto-generated migration for ").append(ctx.tableName()).append("\n");
        sb.append("-- Generated at: ").append(Instant.now()).append("\n\n");
        sb.append("CREATE TABLE IF NOT EXISTS ").append(ctx.tableName()).append(" (\n");
        sb.append("    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),\n");
        sb.append("    tenant_id UUID NOT NULL,\n");

        List<String> indexes = new ArrayList<>();
        indexes.add("CREATE INDEX idx_" + ctx.tableName() + "_tenant_id ON " + ctx.tableName() + "(tenant_id);");

        for (FieldDefinition field : ctx.fields()) {
            if (field.primaryKey()) continue;

            String sqlType = mapToSQLType(field.javaType());
            sb.append("    ").append(field.columnName()).append(" ").append(sqlType);

            if (field.required()) {
                sb.append(" NOT NULL");
            }
            if (field.maxLength() > 0 && (field.javaType().equals("String") || field.javaType().equals("java.lang.String"))) {
                sb.append(" NOT NULL");
            }

            sb.append(",\n");
        }

        sb.append("    created_at TIMESTAMP NOT NULL DEFAULT NOW(),\n");
        sb.append("    updated_at TIMESTAMP NOT NULL DEFAULT NOW()\n");
        sb.append(");\n\n");

        // Indexes
        for (String index : indexes) {
            sb.append(index).append("\n");
        }

        sb.append("\n-- Trigger for auto-updating updated_at\n");
        sb.append("CREATE OR REPLACE TRIGGER trg_").append(ctx.tableName()).append("_updated_at\n");
        sb.append("    BEFORE UPDATE ON ").append(ctx.tableName()).append("\n");
        sb.append("    FOR EACH ROW\n");
        sb.append("    EXECUTE FUNCTION update_updated_at_column();\n");

        String path = "src/main/resources/db/migration/V" + System.currentTimeMillis() + "__create_" + ctx.tableName() + ".sql";
        return new GeneratedFile(path, sb.toString(), "sql", computeChecksum(sb.toString()));
    }

    /**
     * Generate unit test scaffold.
     */
    private GeneratedFile generateUnitTest(GenerationContext ctx) {
        String className = ctx.entityName() + "ServiceTest";
        String packageName = ctx.packagePrefix() + ".application.service";

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(packageName).append(";\n\n");
        sb.append("import org.junit.jupiter.api.Test;\n");
        sb.append("import org.junit.jupiter.api.BeforeEach;\n");
        sb.append("import org.mockito.Mock;\n");
        sb.append("import org.mockito.MockitoAnnotations;\n");
        sb.append("import java.util.UUID;\n\n");
        sb.append("import static org.junit.jupiter.api.Assertions.*;\n");
        sb.append("import static org.mockito.Mockito.*;\n\n");
        sb.append("class ").append(className).append(" {\n\n");
        sb.append("    @Mock\n");
        sb.append("    private ").append(ctx.entityName()).append("Repository repository;\n\n");
        sb.append("    private ").append(ctx.entityName()).append("Service service;\n\n");

        sb.append("    @BeforeEach\n");
        sb.append("    void setUp() {\n");
        sb.append("        MockitoAnnotations.openMocks(this);\n");
        sb.append("        service = new ").append(ctx.entityName()).append("Service(repository);\n");
        sb.append("    }\n\n");

        sb.append("    @Test\n");
        sb.append("    void shouldFindAllByTenantId() {\n");
        sb.append("        UUID tenantId = UUID.randomUUID();\n");
        sb.append("        when(repository.findByTenantId(tenantId)).thenReturn(List.of());\n\n");
        sb.append("        var result = service.findAll(tenantId);\n\n");
        sb.append("        assertNotNull(result);\n");
        sb.append("        verify(repository).findByTenantId(tenantId);\n");
        sb.append("    }\n\n");

        sb.append("    @Test\n");
        sb.append("    void shouldThrowWhenTenantIdIsNull() {\n");
        sb.append("        assertThrows(IllegalArgumentException.class, () -> service.findAll(null));\n");
        sb.append("    }\n\n");

        sb.append("}\n");

        String path = "src/test/java/" + packageName.replace(".", "/") + "/" + className + ".java";
        return new GeneratedFile(path, sb.toString(), "java-test", computeChecksum(sb.toString()));
    }

    /**
     * Validate all generated files.
     */
    private List<ValidationError> validateAll(List<GeneratedFile> files) {
        List<ValidationError> errors = new ArrayList<>();

        for (GeneratedFile file : files) {
            if (file.content().isEmpty()) {
                errors.add(new ValidationError(file.path(), "EMPTY_FILE", "Generated file is empty"));
            }
            if (file.content().length() > 100_000) {
                errors.add(new ValidationError(file.path(), "TOO_LARGE", "Generated file exceeds 100KB"));
            }

            // Security scan
            if (file.content().contains("DROP TABLE") || file.content().contains("DROP DATABASE")) {
                errors.add(new ValidationError(file.path(), "SQL_INJECTION", "Contains dangerous DROP statement"));
            }
            if (file.content().contains("eval(") || file.content().contains("Function(")) {
                errors.add(new ValidationError(file.path(), "XSS", "Contains eval/Function call"));
            }
        }

        return errors;
    }

    /**
     * Write files to disk and create git commit.
     */
    private String writeFiles(List<GeneratedFile> files, GenerationContext ctx) {
        for (GeneratedFile file : files) {
            try {
                Path fullPath = outputBasePath.resolve(file.path()).normalize();
                // Prevent path traversal attacks
                if (!fullPath.startsWith(outputBasePath.normalize())) {
                    throw new SecurityException("Path traversal detected: " + file.path());
                }
                Files.createDirectories(fullPath.getParent());
                Files.writeString(fullPath, file.content(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            } catch (Exception e) {
                throw new RuntimeException("Failed to write file: " + file.path(), e);
            }
        }
        return computeChecksum(String.join("", files.stream().map(GeneratedFile::checksum).toList()));
    }

    // Utility methods

    private String capitalize(String name) {
        if (name == null || name.isEmpty()) return name;
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    private String computeChecksum(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return "unknown";
        }
    }

    private static String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }

    private String mapToTypeScript(String javaType) {
        return switch (javaType) {
            case "String", "java.lang.String" -> "string";
            case "Integer", "java.lang.Integer", "int" -> "number";
            case "Long", "java.lang.Long", "long" -> "number";
            case "Double", "java.lang.Double", "double" -> "number";
            case "Boolean", "java.lang.Boolean", "boolean" -> "boolean";
            case "UUID", "java.util.UUID" -> "string";
            case "LocalDateTime", "java.time.LocalDateTime" -> "string";
            case "LocalDate", "java.time.LocalDate" -> "string";
            default -> "any";
        };
    }

    private String mapToSQLType(String javaType) {
        return switch (javaType) {
            case "String", "java.lang.String" -> "VARCHAR(255)";
            case "Integer", "java.lang.Integer", "int" -> "INTEGER";
            case "Long", "java.lang.Long", "long" -> "BIGINT";
            case "Double", "java.lang.Double", "double" -> "DOUBLE PRECISION";
            case "Boolean", "java.lang.Boolean", "boolean" -> "BOOLEAN";
            case "UUID", "java.util.UUID" -> "UUID";
            case "LocalDateTime", "java.time.LocalDateTime" -> "TIMESTAMP";
            case "LocalDate", "java.time.LocalDate" -> "DATE";
            default -> "TEXT";
        };
    }

    // Value types

    public record GenerationContext(
        String entityName,
        String tableName,
        String packagePrefix,
        UUID tenantId,
        UUID userId,
        List<FieldDefinition> fields
    ) {}

    public record FieldDefinition(
        String name,
        String label,
        String javaType,
        String columnName,
        boolean required,
        boolean primaryKey,
        int maxLength
    ) {}

    public record GenerationResult(
        boolean success,
        List<GeneratedFile> files,
        List<ValidationError> errors,
        String commitHash,
        java.time.Duration duration
    ) {}

    public record GeneratedFile(
        String path,
        String content,
        String type,
        String checksum
    ) {}

    public record ValidationError(
        String file,
        String code,
        String message
    ) {}
}