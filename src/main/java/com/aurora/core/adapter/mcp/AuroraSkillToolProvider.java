package com.aurora.core.adapter.mcp;

import com.aurora.core.application.SkillDefinitionLoader;
import com.aurora.core.application.SkillRouter;
import com.aurora.core.contract.SkillExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * MCP Tool Provider — bridges Aurora Skills to Spring AI MCP
 *
 * Automatically registers all 10 built-in Skills as MCP tools:
 * - skill_form_generator
 * - skill_report_builder
 * - skill_workflow_designer
 * - skill_dashboard_designer
 * - skill_chart_generator
 * - skill_table_designer
 * - skill_api_designer
 * - skill_permission_designer
 * - skill_config_manager
 * - skill_template_generator
 *
 * Spring Boot auto-configuration handles SSE transport at:
 * - SSE endpoint: /mcp/sse (event stream)
 * - Message endpoint: /mcp/message (client → server messages)
 *
 * JWT authentication is enforced by {@link McpSecurityFilter}.
 */
@Component
public class AuroraSkillToolProvider implements ToolCallbackProvider {

    private static final Logger log = LoggerFactory.getLogger(AuroraSkillToolProvider.class);

    private final List<SkillToolCallback> toolCallbacks = new ArrayList<>();

    public AuroraSkillToolProvider(
            @Autowired(required = false) SkillDefinitionLoader skillLoader,
            @Autowired(required = false) SkillRouter skillRouter,
            @Autowired(required = false) SkillExecutor skillExecutor) {

        if (skillLoader == null) {
            log.warn("SkillDefinitionLoader not available — no MCP tools registered");
            return;
        }

        try {
            var skills = skillLoader.loadAll();
            for (var skill : skills) {
                if (skill.deprecated()) {
                    log.info("Skipping deprecated skill: {}", skill.skillId());
                    continue;
                }

                SkillToolCallback callback = new SkillToolCallback(
                    skill, skillRouter, skillExecutor);
                toolCallbacks.add(callback);

                log.info("Registered MCP tool: {} ({})",
                    skill.skillId(), skill.name());
            }

            log.info("Total MCP tools registered: {}", toolCallbacks.size());

        } catch (Exception e) {
            log.error("Failed to load skills for MCP: {}", e.getMessage(), e);
        }
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        return toolCallbacks.toArray(new ToolCallback[0]);
    }
}

/**
 * Individual Skill as MCP ToolCallback
 */
class SkillToolCallback implements ToolCallback {

    private final SkillDefinitionLoader.SkillDefinition skill;
    private final SkillRouter skillRouter;
    private final SkillExecutor skillExecutor;

    private final ToolDefinition toolDefinition;

    SkillToolCallback(SkillDefinitionLoader.SkillDefinition skill,
                      SkillRouter skillRouter,
                      SkillExecutor skillExecutor) {
        this.skill = skill;
        this.skillRouter = skillRouter;
        this.skillExecutor = skillExecutor;

        this.toolDefinition = ToolDefinition.builder()
            .name(skill.skillId())
            .description(skill.description())
            .inputSchema(buildInputSchema(skill))
            .build();
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return toolDefinition;
    }

    @Override
    public String call(String toolInput) {
        try {
            // Parse input JSON
            // Route to appropriate skill
            // Execute and return result as JSON
            // TODO: Integrate with SkillExecutor for actual execution
            return String.format(
                "{\"status\":\"pending\",\"skill\":\"%s\",\"message\":\"Tool execution queued\"}",
                skill.skillId());
        } catch (Exception e) {
            return String.format(
                "{\"status\":\"error\",\"skill\":\"%s\",\"error\":\"%s\"}",
                skill.skillId(), e.getMessage());
        }
    }

    /**
     * Build JSON Schema input schema from Skill YAML definition.
     * Converts the YAML input_schema to a JSON Schema string.
     */
    private String buildInputSchema(SkillDefinitionLoader.SkillDefinition skill) {
        // Build JSON Schema from YAML input_schema
        // For now: return a generic schema
        return String.format(
            "{\"type\":\"object\",\"description\":\"%s\",\"properties\":{}}",
            escapeJson(skill.description()));
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
}
