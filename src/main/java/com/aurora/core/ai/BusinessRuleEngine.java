package com.aurora.core.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Business Rule Engine
 *
 * Validates generated content against configurable business rules.
 * Rules are defined per skill and checked after schema + AST validation.
 *
 * Rule types:
 * - PATTERN: Regex-based content validation
 * - FORBIDDEN: Forbidden keyword/pattern detection
 * - REQUIRED: Required content verification
 * - LIMIT: Count/size limits
 */
public class BusinessRuleEngine {

    private static final Logger log = LoggerFactory.getLogger(BusinessRuleEngine.class);

    // Per-skill rules
    private final Map<String, List<BusinessRule>> skillRules = new ConcurrentHashMap<>();

    // Global forbidden patterns (security)
    private static final List<Pattern> GLOBAL_FORBIDDEN_PATTERNS = List.of(
        Pattern.compile("(?i)DROP\\s+(TABLE|DATABASE|SCHEMA)"),
        Pattern.compile("(?i)DELETE\\s+FROM\\s+\\w+\\s*;"),
        Pattern.compile("(?i)\\bexec\\s*\\("),
        Pattern.compile("(?i)\\beval\\s*\\("),
        Pattern.compile("(?i)<script[^>]*>"),
        Pattern.compile("(?i)javascript\\s*:"),
        Pattern.compile("(?i)\\.getRuntime\\(\\)"),
        Pattern.compile("(?i)ProcessBuilder"),
        Pattern.compile("(?i)Runtime\\.exec")
    );

    public BusinessRuleEngine() {}

    /**
     * Register rules for a skill.
     */
    public void registerRules(String skillId, List<BusinessRule> rules) {
        skillRules.put(skillId, List.copyOf(rules));
    }

    /**
     * Validate output against business rules.
     */
    public AiSelfCorrectionLoop.RuleValidationResult validate(String output, String skillId) {
        List<String> errors = new ArrayList<>();

        // 1. Global forbidden patterns
        for (Pattern pattern : GLOBAL_FORBIDDEN_PATTERNS) {
            if (pattern.matcher(output).find()) {
                errors.add("Forbidden pattern detected: " + pattern.pattern());
            }
        }

        // 2. Per-skill rules
        List<BusinessRule> rules = skillRules.getOrDefault(skillId, List.of());
        for (BusinessRule rule : rules) {
            String result = rule.evaluate(output);
            if (result != null) {
                errors.add(result);
            }
        }

        if (!errors.isEmpty()) {
            return AiSelfCorrectionLoop.RuleValidationResult.fail(List.copyOf(errors));
        }

        return AiSelfCorrectionLoop.RuleValidationResult.ok();
    }

    /**
     * Get registered rules for a skill.
     */
    public List<BusinessRule> getRules(String skillId) {
        return skillRules.getOrDefault(skillId, List.of());
    }

    /**
     * Clear all rules.
     */
    public void clearRules() {
        skillRules.clear();
    }

    /**
     * Business rule interface.
     */
    public sealed interface BusinessRule
        permits BusinessRule.PatternRule, BusinessRule.ForbiddenRule,
               BusinessRule.RequiredRule, BusinessRule.LimitRule {

        String id();
        String description();
        RuleType type();

        /**
         * Evaluate the rule. Returns error message if violated, null if passed.
         */
        String evaluate(String content);

        enum RuleType { PATTERN, FORBIDDEN, REQUIRED, LIMIT }

        record PatternRule(
            String id,
            String description,
            Pattern pattern,
            boolean shouldMatch
        ) implements BusinessRule {
            @Override
            public RuleType type() { return RuleType.PATTERN; }

            @Override
            public String evaluate(String content) {
                boolean matches = pattern.matcher(content).find();
                if (shouldMatch && !matches) {
                    return "Rule " + id + " violated: " + description;
                }
                if (!shouldMatch && matches) {
                    return "Rule " + id + " violated: " + description;
                }
                return null;
            }
        }

        record ForbiddenRule(
            String id,
            String description,
            Pattern forbiddenPattern
        ) implements BusinessRule {
            @Override
            public RuleType type() { return RuleType.FORBIDDEN; }

            @Override
            public String evaluate(String content) {
                if (forbiddenPattern.matcher(content).find()) {
                    return "Forbidden content detected: " + description;
                }
                return null;
            }
        }

        record RequiredRule(
            String id,
            String description,
            String requiredText
        ) implements BusinessRule {
            @Override
            public RuleType type() { return RuleType.REQUIRED; }

            @Override
            public String evaluate(String content) {
                if (!content.contains(requiredText)) {
                    return "Required content missing: " + description;
                }
                return null;
            }
        }

        record LimitRule(
            String id,
            String description,
            int maxLines,
            int maxChars
        ) implements BusinessRule {
            @Override
            public RuleType type() { return RuleType.LIMIT; }

            @Override
            public String evaluate(String content) {
                String[] lines = content.split("\n");
                if (maxLines > 0 && lines.length > maxLines) {
                    return "Content exceeds max lines: " + lines.length + " > " + maxLines;
                }
                if (maxChars > 0 && content.length() > maxChars) {
                    return "Content exceeds max chars: " + content.length() + " > " + maxChars;
                }
                return null;
            }
        }
    }
}