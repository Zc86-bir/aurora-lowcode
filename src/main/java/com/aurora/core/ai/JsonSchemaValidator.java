package com.aurora.core.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSON Schema Validator (Draft 2020-12)
 *
 * Validates LLM output against JSON Schema Draft 2020-12.
 * All skill inputs and outputs must include $id and version fields.
 *
 * Schema caching for performance: compiled schemas are cached by $id.
 */
public class JsonSchemaValidator {

    private static final Logger log = LoggerFactory.getLogger(JsonSchemaValidator.class);

    private static final String DRAFT_2020_12 = "https://json-schema.org/draft/2020-12/schema";

    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory schemaFactory;

    // Compiled schema cache
    private final Map<String, JsonSchema> schemaCache = new ConcurrentHashMap<>();

    public JsonSchemaValidator() {
        this.objectMapper = new ObjectMapper();
        this.schemaFactory = JsonSchemaFactory.builder(
                JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012))
            .build();
    }

    /**
     * Validate JSON string against a schema.
     */
    public AiSelfCorrectionLoop.SchemaValidationResult validate(String json, String schemaJson) {
        List<String> errors = new ArrayList<>();

        try {
            // Parse input
            JsonNode inputNode = objectMapper.readTree(json);
            JsonNode schemaNode = objectMapper.readTree(schemaJson);

            // Validate required fields: $id and version
            validateMetaFields(inputNode, errors);

            // Validate against schema
            JsonSchema schema = schemaFactory.getSchema(schemaNode);
            Set<ValidationMessage> violations = schema.validate(inputNode);

            for (ValidationMessage vm : violations) {
                errors.add(vm.getMessage());
            }

        } catch (Exception e) {
            errors.add("Schema validation error: " + e.getMessage());
        }

        if (errors.isEmpty()) {
            return AiSelfCorrectionLoop.SchemaValidationResult.ok();
        }

        return AiSelfCorrectionLoop.SchemaValidationResult.fail(List.copyOf(errors));
    }

    /**
     * Register a schema by $id for reuse.
     */
    public void registerSchema(String schemaId, String schemaJson) {
        try {
            JsonNode schemaNode = objectMapper.readTree(schemaJson);
            JsonSchema schema = schemaFactory.getSchema(schemaNode);
            schemaCache.put(schemaId, schema);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid schema for $id: " + schemaId, e);
        }
    }

    /**
     * Validate against a registered schema by ID.
     */
    public AiSelfCorrectionLoop.SchemaValidationResult validateById(String json, String schemaId) {
        JsonSchema schema = schemaCache.get(schemaId);
        if (schema == null) {
            return AiSelfCorrectionLoop.SchemaValidationResult.fail(
                List.of("Schema not found: " + schemaId));
        }

        List<String> errors = new ArrayList<>();
        try {
            JsonNode inputNode = objectMapper.readTree(json);
            validateMetaFields(inputNode, errors);

            Set<ValidationMessage> violations = schema.validate(inputNode);
            for (ValidationMessage vm : violations) {
                errors.add(vm.getMessage());
            }
        } catch (Exception e) {
            errors.add("Validation error: " + e.getMessage());
        }

        if (errors.isEmpty()) {
            return AiSelfCorrectionLoop.SchemaValidationResult.ok();
        }

        return AiSelfCorrectionLoop.SchemaValidationResult.fail(List.copyOf(errors));
    }

    /**
     * Clear schema cache.
     */
    public void clearCache() {
        schemaCache.clear();
    }

    // Internal

    private void validateMetaFields(JsonNode node, List<String> errors) {
        if (!node.has("$id")) {
            errors.add("Missing required field: $id");
        }
        if (!node.has("version")) {
            errors.add("Missing required field: version");
        }
    }
}