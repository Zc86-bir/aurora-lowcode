package com.aurora.core.application;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Skill Router
 *
 * Routes natural language requests to the appropriate skill:
 * 1. Intent Recognition → Parse user input into intent categories
 * 2. Skill Matching    → Find best skill based on keywords, category, and confidence
 * 3. Protocol Conversion → Convert intent to skill metadata protocol
 * 4. Execution Callback → Invoke skill executor with converted request
 */
public class SkillRouter {

    private final Map<String, IntentPattern> intentPatterns = new ConcurrentHashMap<>();
    private final SkillDefinitionLoader definitionLoader;

    public SkillRouter(SkillDefinitionLoader definitionLoader) {
        this.definitionLoader = definitionLoader;
        registerDefaultIntents();
    }

    /**
     * Route a natural language request to the best matching skill.
     */
    public SkillRoute route(String userInput, UUID tenantId, UUID userId) {
        IntentRecognition recognized = recognizeIntent(userInput);
        SkillDefinitionLoader.SkillDefinition bestMatch = findBestSkill(recognized);

        if (bestMatch == null) {
            return new SkillRoute(
                null, recognized, null, 0.0,
                "No matching skill found for: " + userInput
            );
        }

        double confidence = calculateConfidence(recognized, bestMatch);
        SkillRequest request = convertToRequest(recognized, bestMatch, tenantId, userId);

        return new SkillRoute(bestMatch, recognized, request, confidence, null);
    }

    /**
     * Register a custom intent pattern.
     */
    public void registerIntent(String intentId, IntentPattern pattern) {
        intentPatterns.put(intentId, pattern);
    }

    /**
     * List all registered intents.
     */
    public List<String> listIntents() {
        return List.copyOf(intentPatterns.keySet());
    }

    // Internal

    private void registerDefaultIntents() {
        registerIntent("form_create", new IntentPattern(
            "form_create",
            List.of("create form", "new form", "build form", "generate form", "表单", "新建表单"),
            List.of("skill_form_generator"),
            0.7
        ));

        registerIntent("report_create", new IntentPattern(
            "report_create",
            List.of("create report", "new report", "build report", "generate report", "报表"),
            List.of("skill_report_builder"),
            0.7
        ));

        registerIntent("workflow_create", new IntentPattern(
            "workflow_create",
            List.of("create workflow", "new process", "design process", "create process", "流程"),
            List.of("skill_workflow_designer"),
            0.7
        ));

        registerIntent("dashboard_create", new IntentPattern(
            "dashboard_create",
            List.of("create dashboard", "new dashboard", "build dashboard", "dashboard", "仪表盘"),
            List.of("skill_dashboard_designer"),
            0.7
        ));

        registerIntent("chart_create", new IntentPattern(
            "chart_create",
            List.of("create chart", "new chart", "build chart", "generate chart", "图表"),
            List.of("skill_chart_generator"),
            0.7
        ));

        registerIntent("table_create", new IntentPattern(
            "table_create",
            List.of("create table", "new table", "build table", "data grid", "表格"),
            List.of("skill_table_designer"),
            0.7
        ));

        registerIntent("api_create", new IntentPattern(
            "api_create",
            List.of("create api", "new endpoint", "build api", "generate api", "接口"),
            List.of("skill_api_designer"),
            0.7
        ));

        registerIntent("permission_create", new IntentPattern(
            "permission_create",
            List.of("set permission", "configure permission", "access control", "权限"),
            List.of("skill_permission_designer"),
            0.7
        ));

        registerIntent("config_create", new IntentPattern(
            "config_create",
            List.of("configure", "set config", "update config", "配置"),
            List.of("skill_config_manager"),
            0.7
        ));

        registerIntent("template_create", new IntentPattern(
            "template_create",
            List.of("create template", "new template", "generate template", "模板"),
            List.of("skill_template_generator"),
            0.7
        ));
    }

    private IntentRecognition recognizeIntent(String userInput) {
        String normalized = userInput.toLowerCase().trim();
        String matchedIntent = null;
        double bestScore = 0.0;

        for (Map.Entry<String, IntentPattern> entry : intentPatterns.entrySet()) {
            IntentPattern pattern = entry.getValue();
            double score = calculateIntentScore(pattern, normalized);
            if (score > bestScore) {
                bestScore = score;
                matchedIntent = entry.getKey();
            }
        }

        List<String> extractedEntities = extractEntities(userInput);

        return new IntentRecognition(
            matchedIntent,
            bestScore,
            userInput,
            extractedEntities,
            determineCategory(normalized)
        );
    }

    private double calculateIntentScore(IntentPattern pattern, String input) {
        int matchedKeywords = 0;
        for (String keyword : pattern.keywords()) {
            if (input.contains(keyword.toLowerCase())) {
                matchedKeywords++;
            }
        }

        if (matchedKeywords == 0) return 0.0;
        return (double) matchedKeywords / pattern.keywords().size();
    }

    private List<String> extractEntities(String userInput) {
        List<String> entities = new ArrayList<>();

        // Extract UUID patterns
        Pattern uuidPattern = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
        );
        Matcher uuidMatcher = uuidPattern.matcher(userInput);
        while (uuidMatcher.find()) {
            entities.add("uuid:" + uuidMatcher.group());
        }

        // Extract quoted strings as named entities
        Pattern quotedPattern = Pattern.compile("\"([^\"]+)\"");
        Matcher quotedMatcher = quotedPattern.matcher(userInput);
        while (quotedMatcher.find()) {
            entities.add("entity:" + quotedMatcher.group(1));
        }

        // Extract key:value pairs
        Pattern kvPattern = Pattern.compile("(\\w+):\\s*(\\S+)");
        Matcher kvMatcher = kvPattern.matcher(userInput);
        while (kvMatcher.find()) {
            entities.add("param:" + kvMatcher.group(1) + "=" + kvMatcher.group(2));
        }

        return List.copyOf(entities);
    }

    private String determineCategory(String input) {
        if (input.contains("form") || input.contains("表单")) return "form";
        if (input.contains("report") || input.contains("报表")) return "report";
        if (input.contains("workflow") || input.contains("流程")) return "workflow";
        if (input.contains("dashboard") || input.contains("仪表盘")) return "dashboard";
        if (input.contains("chart") || input.contains("图表")) return "chart";
        if (input.contains("table") || input.contains("表格")) return "table";
        if (input.contains("api") || input.contains("接口")) return "api";
        if (input.contains("permission") || input.contains("权限")) return "permission";
        if (input.contains("config") || input.contains("配置")) return "config";
        if (input.contains("template") || input.contains("模板")) return "template";
        return "unknown";
    }

    private SkillDefinitionLoader.SkillDefinition findBestSkill(IntentRecognition recognized) {
        if (recognized.intentId() == null) return null;

        IntentPattern pattern = intentPatterns.get(recognized.intentId());
        if (pattern == null) return null;

        for (String skillId : pattern.preferredSkills()) {
            String resolvedId = definitionLoader.resolveAlias(skillId);
            Optional<SkillDefinitionLoader.SkillDefinition> def = definitionLoader.loadById(resolvedId);
            if (def.isPresent()) {
                return def.get();
            }
        }

        // Fallback: search by category
        String category = recognized.category();
        for (String skillId : definitionLoader.getRegisteredIds()) {
            Optional<SkillDefinitionLoader.SkillDefinition> def = definitionLoader.loadById(skillId);
            if (def.isPresent() && def.get().category().equals(category)) {
                return def.get();
            }
        }

        return null;
    }

    private double calculateConfidence(IntentRecognition recognized,
                                        SkillDefinitionLoader.SkillDefinition skill) {
        if (recognized == null || skill == null) return 0.0;
        return recognized.confidence() * 0.6 + (skill.deprecated() ? 0.0 : 0.4);
    }

    private SkillRequest convertToRequest(IntentRecognition recognized,
                                           SkillDefinitionLoader.SkillDefinition skill,
                                           UUID tenantId, UUID userId) {
        Map<String, Object> extracted = extractRequestParameters(recognized);
        extracted.put("description", recognized.userInput());

        return new SkillRequest(
            skill.skillId(),
            tenantId,
            userId,
            Map.copyOf(extracted)
        );
    }

    private Map<String, Object> extractRequestParameters(IntentRecognition recognized) {
        Map<String, Object> params = new HashMap<>();
        for (String entity : recognized.entities()) {
            if (entity.startsWith("uuid:")) {
                params.put("tenant_id", entity.substring(5));
            } else if (entity.startsWith("entity:")) {
                params.put("entity", entity.substring(7));
            } else if (entity.startsWith("param:")) {
                String[] parts = entity.substring(6).split("=", 2);
                if (parts.length == 2) {
                    params.put(parts[0], parts[1]);
                }
            }
        }
        return params;
    }

    // Value types

    /**
     * Intent recognition result
     */
    public record IntentRecognition(
        String intentId,
        double confidence,
        String userInput,
        List<String> entities,
        String category
    ) {}

    /**
     * Intent pattern for matching
     */
    public record IntentPattern(
        String intentId,
        List<String> keywords,
        List<String> preferredSkills,
        double minConfidence
    ) {}

    /**
     * Skill request converted from intent
     */
    public record SkillRequest(
        String skillId,
        UUID tenantId,
        UUID userId,
        Map<String, Object> parameters
    ) {}

    /**
     * Route result
     */
    public record SkillRoute(
        SkillDefinitionLoader.SkillDefinition matchedSkill,
        IntentRecognition recognized,
        SkillRequest request,
        double confidence,
        String errorMessage
    ) {
        public boolean isSuccessful() {
            return matchedSkill != null && errorMessage == null;
        }
    }
}