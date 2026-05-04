package com.aurora.core.application;

import com.aurora.core.contract.MetadataRepository;
import com.aurora.core.contract.MetadataRepository.MetadataAggregate;
import com.aurora.core.contract.MetadataRepository.MetadataStatus;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Metadata Validator
 *
 * Validates metadata entities through three layers:
 * 1. JSON Schema validation (structure)
 * 2. Business rule validation (domain constraints)
 * 3. Security rule validation (injection, access patterns)
 *
 * All validation is deterministic and stateless except for
 * uniqueness checks which require repository access.
 */
public class MetadataValidator {

    private static final Pattern SQL_INJECTION = Pattern.compile(
        "(?i)(\\b(SELECT|INSERT|UPDATE|DELETE|DROP|ALTER|CREATE|EXEC|EXECUTE)\\b.*\\b(FROM|INTO|TABLE|WHERE|SET)\\b|'\\s*(OR|AND)\\s*'?\\d+\\s*=\\s*\\d+|--\\s*$|;\\s*(DROP|DELETE|UPDATE))"
    );

    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i)<\\s*(script|iframe|object|embed|form|input|svg|img|link|meta|base)\\b[^>]*>|javascript\\s*:|on\\w+\\s*=",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern VALID_IDENTIFIER = Pattern.compile(
        "^[a-zA-Z_][a-zA-Z0-9_]{0,63}$"
    );

    private static final int MAX_NAME_LENGTH = 128;
    private static final int MAX_SCHEMA_SIZE = 65536;

    /**
     * Validate metadata before creation.
     */
    public ValidationResult validateCreate(MetadataAggregate aggregate) {
        List<ValidationError> errors = new ArrayList<>();

        validateBase(aggregate, errors);
        validateSchema(aggregate, errors);
        validateBusinessRules(aggregate, errors);
        validateSecurity(aggregate, errors);

        return new ValidationResult(errors.isEmpty(), List.copyOf(errors));
    }

    /**
     * Validate metadata before update.
     */
    public ValidationResult validateUpdate(MetadataAggregate existing, MetadataAggregate updated) {
        List<ValidationError> errors = new ArrayList<>();

        validateBase(updated, errors);
        validateSchema(updated, errors);
        validateSecurity(updated, errors);
        validateVersionConsistency(existing, updated, errors);

        return new ValidationResult(errors.isEmpty(), List.copyOf(errors));
    }

    /**
     * Validate metadata before deletion.
     */
    public ValidationResult validateDelete(MetadataAggregate aggregate) {
        List<ValidationError> errors = new ArrayList<>();

        if (aggregate == null) {
            errors.add(new ValidationError("aggregate", "NULL", "Metadata cannot be null"));
            return new ValidationResult(false, List.copyOf(errors));
        }

        if (aggregate.getStatus() == MetadataStatus.ARCHIVED) {
            errors.add(new ValidationError("status", "ARCHIVED", "Cannot delete archived metadata"));
        }

        return new ValidationResult(errors.isEmpty(), List.copyOf(errors));
    }

    /**
     * Validate schema structure.
     */
    public ValidationResult validateSchemaStructure(MetadataAggregate aggregate) {
        List<ValidationError> errors = new ArrayList<>();
        validateSchema(aggregate, errors);
        return new ValidationResult(errors.isEmpty(), List.copyOf(errors));
    }

    /**
     * Validate uniqueness constraints.
     */
    public ValidationResult validateUniqueness(MetadataAggregate aggregate,
                                                MetadataRepository repository) {
        List<ValidationError> errors = new ArrayList<>();

        if (aggregate == null) {
            errors.add(new ValidationError("aggregate", "NULL", "Metadata cannot be null"));
            return new ValidationResult(false, List.copyOf(errors));
        }

        if (repository.existsByTenantAndName(aggregate.getTenantId(), aggregate.getName())) {
            errors.add(new ValidationError(
                "name", "DUPLICATE",
                "Metadata name '%s' already exists in tenant".formatted(aggregate.getName())
            ));
        }

        return new ValidationResult(errors.isEmpty(), List.copyOf(errors));
    }

    // Internal validation methods

    private void validateBase(MetadataAggregate aggregate, List<ValidationError> errors) {
        if (aggregate == null) {
            errors.add(new ValidationError("aggregate", "NULL", "Metadata cannot be null"));
            return;
        }

        if (aggregate.getTenantId() == null) {
            errors.add(new ValidationError("tenant_id", "NULL", "Tenant ID is required"));
        }

        String name = aggregate.getName();
        if (name == null || name.isBlank()) {
            errors.add(new ValidationError("name", "NULL", "Name is required"));
        } else if (name.length() > MAX_NAME_LENGTH) {
            errors.add(new ValidationError(
                "name", "TOO_LONG",
                "Name exceeds maximum length of %d characters".formatted(MAX_NAME_LENGTH)
            ));
        } else if (!VALID_IDENTIFIER.matcher(name).matches()) {
            errors.add(new ValidationError(
                "name", "INVALID_FORMAT",
                "Name must be a valid identifier (alphanumeric and underscore only)"
            ));
        }

        if (aggregate.getType() == null) {
            errors.add(new ValidationError("type", "NULL", "Type is required"));
        }

        validateTimestamps(aggregate, errors);
    }

    private void validateTimestamps(MetadataAggregate aggregate, List<ValidationError> errors) {
        if (aggregate.getCreatedAt() == null) {
            errors.add(new ValidationError("created_at", "NULL", "Created timestamp is required"));
        }

        if (aggregate.getUpdatedAt() != null && aggregate.getCreatedAt() != null) {
            if (aggregate.getUpdatedAt().isBefore(aggregate.getCreatedAt())) {
                errors.add(new ValidationError(
                    "updated_at", "INVALID",
                    "Updated timestamp cannot be before created timestamp"
                ));
            }
        }
    }

    private void validateSchema(MetadataAggregate aggregate, List<ValidationError> errors) {
        if (aggregate == null) return;

        switch (aggregate) {
            case MetadataAggregate.FormMetadata form -> validateFormSchema(form, errors);
            case MetadataAggregate.ReportMetadata report -> validateReportSchema(report, errors);
            case MetadataAggregate.WorkflowMetadata workflow -> validateWorkflowSchema(workflow, errors);
            case MetadataAggregate.DashboardMetadata dashboard -> validateDashboardSchema(dashboard, errors);
            case MetadataAggregate.ChartMetadata chart -> validateChartSchema(chart, errors);
            case MetadataAggregate.TableMetadata table -> validateTableSchema(table, errors);
            case MetadataAggregate.ApiMetadata api -> validateApiSchema(api, errors);
            case MetadataAggregate.ConfigMetadata config -> validateConfigSchema(config, errors);
            case MetadataAggregate.PermissionMetadata perm -> validatePermissionSchema(perm, errors);
            case MetadataAggregate.TemplateMetadata template -> validateTemplateSchema(template, errors);
        }
    }

    private void validateFormSchema(MetadataAggregate.FormMetadata form, List<ValidationError> errors) {
        if (form.schema() == null) {
            errors.add(new ValidationError("schema", "NULL", "Form schema is required"));
        }
        if (form.layout() == null) {
            errors.add(new ValidationError("layout", "NULL", "Form layout is required"));
        }
    }

    private void validateReportSchema(MetadataAggregate.ReportMetadata report, List<ValidationError> errors) {
        if (report.dataSource() == null) {
            errors.add(new ValidationError("data_source", "NULL", "Data source configuration is required"));
        }
        if (report.columns() == null) {
            errors.add(new ValidationError("columns", "NULL", "Column definitions are required"));
        }
    }

    private void validateWorkflowSchema(MetadataAggregate.WorkflowMetadata workflow, List<ValidationError> errors) {
        if (workflow.bpmnDefinition() == null) {
            errors.add(new ValidationError("bpmn_definition", "NULL", "BPMN definition is required"));
        }
    }

    private void validateDashboardSchema(MetadataAggregate.DashboardMetadata dashboard, List<ValidationError> errors) {
        if (dashboard.components() == null) {
            errors.add(new ValidationError("components", "NULL", "Dashboard components are required"));
        }
    }

    private void validateChartSchema(MetadataAggregate.ChartMetadata chart, List<ValidationError> errors) {
        if (chart.chartConfig() == null) {
            errors.add(new ValidationError("chart_config", "NULL", "Chart configuration is required"));
        }
    }

    private void validateTableSchema(MetadataAggregate.TableMetadata table, List<ValidationError> errors) {
        if (table.columns() == null) {
            errors.add(new ValidationError("columns", "NULL", "Column definitions are required"));
        }
    }

    private void validateApiSchema(MetadataAggregate.ApiMetadata api, List<ValidationError> errors) {
        if (api.endpoint() == null) {
            errors.add(new ValidationError("endpoint", "NULL", "API endpoint configuration is required"));
        }
    }

    private void validateConfigSchema(MetadataAggregate.ConfigMetadata config, List<ValidationError> errors) {
        if (config.properties() == null) {
            errors.add(new ValidationError("properties", "NULL", "Configuration properties are required"));
        }
    }

    private void validatePermissionSchema(MetadataAggregate.PermissionMetadata perm, List<ValidationError> errors) {
        if (perm.rules() == null) {
            errors.add(new ValidationError("rules", "NULL", "Permission rules are required"));
        }
    }

    private void validateTemplateSchema(MetadataAggregate.TemplateMetadata template, List<ValidationError> errors) {
        if (template.templateContent() == null) {
            errors.add(new ValidationError("template_content", "NULL", "Template content is required"));
        }
    }

    private void validateBusinessRules(MetadataAggregate aggregate, List<ValidationError> errors) {
        if (aggregate == null) return;

        validateNameUniqueness(aggregate, errors);
        validateVersionSequence(aggregate, errors);
        validateStatusTransition(aggregate, errors);
    }

    private void validateNameUniqueness(MetadataAggregate aggregate, List<ValidationError> errors) {
        String name = aggregate.getName();
        if (name != null && name.length() > MAX_NAME_LENGTH) {
            errors.add(new ValidationError(
                "name", "TOO_LONG",
                "Name must not exceed %d characters".formatted(MAX_NAME_LENGTH)
            ));
        }
    }

    private void validateVersionSequence(MetadataAggregate aggregate, List<ValidationError> errors) {
        if (aggregate.getVersion() < 1) {
            errors.add(new ValidationError(
                "version", "INVALID",
                "Version must be >= 1"
            ));
        }
    }

    private void validateStatusTransition(MetadataAggregate aggregate, List<ValidationError> errors) {
        MetadataStatus status = aggregate.getStatus();
        if (status == null) {
            errors.add(new ValidationError("status", "NULL", "Status is required"));
        }
    }

    private void validateVersionConsistency(MetadataAggregate existing, MetadataAggregate updated,
                                             List<ValidationError> errors) {
        if (existing == null || updated == null) return;

        if (existing.getVersion() >= updated.getVersion()) {
            errors.add(new ValidationError(
                "version", "CONFLICT",
                "Updated version (%d) must be greater than existing version (%d)"
                    .formatted(updated.getVersion(), existing.getVersion())
            ));
        }

        if (!existing.getTenantId().equals(updated.getTenantId())) {
            errors.add(new ValidationError(
                "tenant_id", "MISMATCH",
                "Tenant ID cannot be changed"
            ));
        }
    }

    private void validateSecurity(MetadataAggregate aggregate, List<ValidationError> errors) {
        if (aggregate == null) return;

        String name = aggregate.getName();
        if (name != null) {
            if (containsSqlInjection(name)) {
                errors.add(new ValidationError(
                    "name", "SQL_INJECTION",
                    "Name contains potential SQL injection pattern"
                ));
            }
            if (containsXss(name)) {
                errors.add(new ValidationError(
                    "name", "XSS",
                    "Name contains potential XSS pattern"
                ));
            }
        }

        validateSchemaSecurity(aggregate, errors);
    }

    private void validateSchemaSecurity(MetadataAggregate aggregate, List<ValidationError> errors) {
        String content = aggregateToString(aggregate);
        if (content == null) return;

        if (content.length() > MAX_SCHEMA_SIZE) {
            errors.add(new ValidationError(
                "schema", "TOO_LARGE",
                "Schema exceeds maximum size of %d bytes".formatted(MAX_SCHEMA_SIZE)
            ));
        }

        if (containsSqlInjection(content)) {
            errors.add(new ValidationError(
                "schema", "SQL_INJECTION",
                "Schema contains potential SQL injection pattern"
            ));
        }

        if (containsXss(content)) {
            errors.add(new ValidationError(
                "schema", "XSS",
                "Schema contains potential XSS pattern"
            ));
        }
    }

    private boolean containsSqlInjection(String input) {
        return SQL_INJECTION.matcher(input).find();
    }

    private boolean containsXss(String input) {
        return XSS_PATTERN.matcher(input).find();
    }

    private String aggregateToString(MetadataAggregate aggregate) {
        return switch (aggregate) {
            case MetadataAggregate.FormMetadata f ->
                f.schema().toString() + f.layout().toString();
            case MetadataAggregate.ReportMetadata r ->
                r.dataSource().toString() + r.columns().toString();
            case MetadataAggregate.WorkflowMetadata w ->
                w.bpmnDefinition().toString();
            case MetadataAggregate.DashboardMetadata d ->
                d.components().toString() + d.layout().toString();
            case MetadataAggregate.ChartMetadata c ->
                c.chartConfig().toString() + c.dataBinding().toString();
            case MetadataAggregate.TableMetadata t ->
                t.columns().toString() + t.query().toString();
            case MetadataAggregate.ApiMetadata a ->
                a.endpoint().toString() + a.swagger().toString();
            case MetadataAggregate.ConfigMetadata c ->
                c.properties().toString();
            case MetadataAggregate.PermissionMetadata p ->
                p.rules().toString();
            case MetadataAggregate.TemplateMetadata t ->
                t.templateContent().toString();
        };
    }

    // Value types

    /**
     * Validation result
     */
    public record ValidationResult(
        boolean isValid,
        List<ValidationError> errors
    ) {
        public static ValidationResult valid() {
            return new ValidationResult(true, List.of());
        }

        public static ValidationResult invalid(List<ValidationError> errors) {
            return new ValidationResult(false, errors);
        }
    }

    /**
     * Validation error
     */
    public record ValidationError(
        String field,
        String code,
        String message
    ) {}
}