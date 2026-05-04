package com.aurora.core.infrastructure.policy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ABAC Policy Engine
 *
 * Supports subject + resource + action + env four-dimensional policy evaluation.
 * Policy rules are loaded from YAML files and hot-reloadable.
 *
 * Policy format:
 *   - id: rule_001
 *     effect: ALLOW | DENY
 *     subject: { role: admin, department: finance }
 *     resource: { type: report, name: * }
 *     action: { operation: read | write | delete }
 *     env: { timeRange: 09:00-18:00, ipRange: 10.0.0.0/8 }
 *
 * Evaluation order: DENY rules take precedence over ALLOW rules.
 */
public class AbacPolicyEngine {

    private static final Logger log = LoggerFactory.getLogger(AbacPolicyEngine.class);

    private final List<PolicyRule> rules = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Instant> ruleVersions = new ConcurrentHashMap<>();
    private final Path policyPath;

    public AbacPolicyEngine(Path policyPath) {
        this.policyPath = policyPath;
    }

    /**
     * Evaluate a policy request against all rules.
     *
     * @return true if ALLOW, false if DENY or no matching rule
     */
    public EvaluationResult evaluate(PolicyRequest request) {
        List<String> matchedRules = new ArrayList<>();
        boolean hasDeny = false;

        // DENY rules evaluated first (deny-by-default)
        for (PolicyRule rule : rules) {
            if (!rule.effect().equals(PolicyEffect.DENY)) continue;
            if (matches(rule, request)) {
                hasDeny = true;
                matchedRules.add(rule.id());
            }
        }

        if (hasDeny) {
            return new EvaluationResult(false, matchedRules, "Denied by rule: " + matchedRules);
        }

        // ALLOW rules
        List<String> allowMatches = new ArrayList<>();
        for (PolicyRule rule : rules) {
            if (!rule.effect().equals(PolicyEffect.ALLOW)) continue;
            if (matches(rule, request)) {
                allowMatches.add(rule.id());
            }
        }

        if (!allowMatches.isEmpty()) {
            return new EvaluationResult(true, allowMatches, "Allowed by rules: " + allowMatches);
        }

        // Default deny
        return new EvaluationResult(false, List.of(), "No matching rule (default deny)");
    }

    private final ReentrantLock writeLock = new ReentrantLock();

    /**
     * Load policy rules from file.
     * Uses ReentrantLock instead of synchronized to prevent thread pinning with virtual threads.
     */
    public void loadPolicies(Path path) throws IOException {
        writeLock.lock();
        try {
            String content = Files.readString(path);
            // Simple YAML parser for policy rules
            List<PolicyRule> loaded = parsePolicyYaml(content);
            rules.clear();
            rules.addAll(loaded);
            for (PolicyRule rule : loaded) {
                ruleVersions.put(rule.id(), Instant.now());
            }
            log.info("Loaded {} policy rules from {}", loaded.size(), path);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Hot-reload policies from configured path.
     */
    public void reload() throws IOException {
        if (policyPath != null && Files.exists(policyPath)) {
            loadPolicies(policyPath);
        }
    }

    /**
     * Add a single rule dynamically.
     */
    public void addRule(PolicyRule rule) {
        rules.add(rule);
        ruleVersions.put(rule.id(), Instant.now());
    }

    /**
     * Remove a rule by ID.
     */
    public boolean removeRule(String ruleId) {
        boolean removed = rules.removeIf(r -> r.id().equals(ruleId));
        if (removed) ruleVersions.remove(ruleId);
        return removed;
    }

    /**
     * Get loaded rule count.
     */
    public int getRuleCount() {
        return rules.size();
    }

    // Internal

    @SuppressWarnings("unchecked")
    private List<PolicyRule> parsePolicyYaml(String content) {
        List<PolicyRule> result = new ArrayList<>();
        // Simplified parser — in production, use SnakeYAML
        String[] blocks = content.split("\n\\s*---\\s*");
        for (String block : blocks) {
            if (block.trim().isEmpty()) continue;
            PolicyRule rule = parseSingleRule(block.trim());
            if (rule != null) result.add(rule);
        }
        return result;
    }

    private PolicyRule parseSingleRule(String block) {
        Map<String, String> kv = new LinkedHashMap<>();
        for (String line : block.split("\n")) {
            line = line.trim();
            if (line.startsWith("- ") || line.startsWith("#")) continue;
            if (line.contains(":")) {
                int idx = line.indexOf(':');
                String key = line.substring(0, idx).trim();
                String val = line.substring(idx + 1).trim();
                kv.put(key, val);
            }
        }
        if (!kv.containsKey("id") || !kv.containsKey("effect")) return null;

        PolicySubject subject = new PolicySubject(
            kv.getOrDefault("subject_role", "*"),
            kv.getOrDefault("subject_department", "*")
        );
        PolicyResource resource = new PolicyResource(
            kv.getOrDefault("resource_type", "*"),
            kv.getOrDefault("resource_name", "*")
        );
        PolicyAction action = new PolicyAction(
            kv.getOrDefault("action_operation", "*")
        );
        PolicyEnv env = new PolicyEnv(
            kv.getOrDefault("env_time_range", "*"),
            kv.getOrDefault("env_ip_range", "*")
        );

        return new PolicyRule(
            kv.get("id"),
            PolicyEffect.valueOf(kv.get("effect")),
            subject,
            resource,
            action,
            env
        );
    }

    private boolean matches(PolicyRule rule, PolicyRequest request) {
        return matchWildcard(rule.subject().role(), request.subjectRole())
            && matchWildcard(rule.subject().department(), request.subjectDepartment())
            && matchWildcard(rule.resource().type(), request.resourceType())
            && matchWildcard(rule.resource().name(), request.resourceName())
            && matchWildcard(rule.action().operation(), request.action())
            && matchWildcard(rule.env().timeRange(), request.envTime())
            && matchWildcard(rule.env().ipRange(), request.envIp());
    }

    private boolean matchWildcard(String pattern, String value) {
        if (pattern.equals("*")) return true;
        return pattern.equals(value);
    }

    // Value types

    public record PolicyRequest(
        String subjectRole,
        String subjectDepartment,
        String resourceType,
        String resourceName,
        String action,
        String envTime,
        String envIp
    ) {}

    public record EvaluationResult(
        boolean allowed,
        List<String> matchedRuleIds,
        String reason
    ) {}

    public record PolicyRule(
        String id,
        PolicyEffect effect,
        PolicySubject subject,
        PolicyResource resource,
        PolicyAction action,
        PolicyEnv env
    ) {}

    public enum PolicyEffect { ALLOW, DENY }

    public record PolicySubject(String role, String department) {}
    public record PolicyResource(String type, String name) {}
    public record PolicyAction(String operation) {}
    public record PolicyEnv(String timeRange, String ipRange) {}
}