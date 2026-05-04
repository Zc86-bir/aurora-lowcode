package com.aurora.core.infrastructure.security;

import com.aurora.core.contract.DataMasker;
import com.aurora.core.contract.DataMasker.MaskingStrategy;
import com.aurora.core.contract.DataMasker.FieldMaskingRule;
import com.aurora.core.contract.DataMasker.RowLevelFilter;
import com.aurora.core.contract.DataMasker.FieldMaskingPolicy;
import com.aurora.core.contract.DataMasker.MaskingConfig;
import com.aurora.core.contract.DataMasker.UnmaskResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Structured Data Masker Implementation
 */
public class StructuredDataMasker implements DataMasker {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^([a-zA-Z0-9_\\-\\.]+)@([a-zA-Z0-9_\\-\\.]+)\\.([a-zA-Z]{2,5})$"
    );

    private final Map<String, FieldMaskingRule> rules = new ConcurrentHashMap<>();
    private final Map<String, RowLevelFilter> rowFilters = new ConcurrentHashMap<>();
    private final Map<String, CustomMaskingStrategy> customStrategies = new ConcurrentHashMap<>();

    @Override
    public String maskField(String fieldName, String value, MaskingStrategy strategy) {
        if (value == null) return null;

        FieldMaskingRule rule = rules.get(fieldName);
        if (rule != null) {
            return rule.mask(value);
        }

        return switch (strategy) {
            case FULL_MASK -> "****";
            case PARTIAL_MASK -> applyPartialMask(value);
            case HASH -> hashValue(value);
            case RANDOMIZE -> randomMask(value);
            case TOKENIZE -> "TOKEN_" + hashValue(value).substring(0, 8);
            case REDACT -> null;
            case NULLIFY -> null;
            case GENERALIZE -> value;
            case PSEUDONYMIZE -> "FAKE_" + hashValue(value).substring(0, 8);
            case CUSTOM -> {
                CustomMaskingStrategy cs = customStrategies.get(fieldName);
                yield cs != null ? cs.mask(value, Map.of()) : value;
            }
        };
    }

    @Override
    public Map<String, Object> maskRecord(Map<String, Object> record, MaskingConfig config) {
        Map<String, Object> masked = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : record.entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();

            if (value == null) {
                masked.put(field, null);
                continue;
            }

            MaskingStrategy strategy = config.fieldStrategies().getOrDefault(
                field, MaskingStrategy.FULL_MASK);
            masked.put(field, maskField(field, value.toString(), strategy));
        }
        return masked;
    }

    @Override
    public List<Map<String, Object>> maskRecords(
        List<Map<String, Object>> records,
        MaskingConfig config
    ) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> record : records) {
            if (!passesRowFilter(config.resource(), null, record)) {
                continue;
            }
            result.add(maskRecord(record, config));
        }
        return result;
    }

    @Override
    public Map<String, Object> maskForUser(
        Map<String, Object> record,
        UUID userId,
        UUID tenantId,
        String resource
    ) {
        Map<String, Object> masked = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : record.entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();

            if (value == null) {
                masked.put(field, null);
                continue;
            }

            FieldMaskingRule rule = rules.get(field);
            if (rule != null) {
                masked.put(field, rule.mask(value.toString()));
            } else {
                masked.put(field, maskField(field, value.toString(), MaskingStrategy.FULL_MASK));
            }
        }
        return masked;
    }

    @Override
    public MaskingStrategy getStrategy(String fieldName, String resource) {
        FieldMaskingRule rule = rules.get(fieldName);
        return rule != null ? rule.strategy() : MaskingStrategy.FULL_MASK;
    }

    @Override
    public void registerStrategy(String strategyName, CustomMaskingStrategy strategy) {
        customStrategies.put(strategyName, strategy);
    }

    @Override
    public MaskingValidationResult validateConfig(MaskingConfig config) {
        Set<String> coveredFields = new HashSet<>(config.sensitiveFields());
        Set<String> uncovered = new LinkedHashSet<>();

        for (String field : config.sensitiveFields()) {
            if (!config.fieldStrategies().containsKey(field)
                && !config.customRules().containsKey(field)) {
                uncovered.add(field);
            }
        }

        List<MaskingValidationResult.MaskingValidationError> errors = uncovered.stream()
            .map(f -> new MaskingValidationResult.MaskingValidationError(
                f, "NO_STRATEGY",
                "No masking strategy configured for sensitive field: " + f,
                "Add a field strategy or custom rule"
            ))
            .toList();

        return new MaskingValidationResult(
            errors.isEmpty(),
            errors,
            coveredFields,
            uncovered
        );
    }

    @Override
    public UnmaskResult unmaskForAudit(
        Map<String, Object> maskedRecord,
        UUID userId,
        UUID tenantId,
        String auditReason
    ) {
        return new UnmaskResult(
            false,
            Map.of(),
            UUID.randomUUID(),
            auditReason,
            java.time.Instant.now(),
            "UNMASK_REQUIRES_ADMIN_PERMISSION"
        );
    }

    public String maskEmail(String email) {
        if (email == null) return null;
        var m = EMAIL_PATTERN.matcher(email);
        if (!m.matches()) return "****@****.***";
        String local = m.group(1);
        String domain = m.group(2);
        String tld = m.group(3);
        if (local.length() <= 2) return "**@" + domain + "." + tld;
        return local.charAt(0) + "***" + local.charAt(local.length() - 1)
            + "@" + domain.charAt(0) + "***" + "." + tld;
    }

    public String maskPhone(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() < 4) return "****";
        return digits.substring(0, 3) + "****" + digits.substring(digits.length() - 4);
    }

    public String maskIdCard(String idCard) {
        if (idCard == null) return null;
        if (idCard.length() < 8) return "********";
        return idCard.substring(0, 3) + "*********" + idCard.substring(idCard.length() - 4);
    }

    public String maskBankCard(String bankCard) {
        if (bankCard == null) return null;
        String digits = bankCard.replaceAll("[^0-9]", "");
        if (digits.length() < 8) return "****";
        return digits.substring(0, 4) + " **** **** " + digits.substring(digits.length() - 4);
    }

    public String maskName(String name) {
        if (name == null) return null;
        if (name.length() <= 1) return "*";
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }

    public String maskAddress(String address) {
        if (address == null) return null;
        String[] parts = address.split("[\\s,，]+");
        if (parts.length <= 2) return "***";
        return String.join(" ", Arrays.copyOf(parts, 2)) + " ***";
    }

    public void registerFieldRule(String fieldName, FieldMaskingRule rule) {
        rules.put(fieldName, rule);
    }

    public void registerRowFilter(String resourceType, RowLevelFilter filter) {
        rowFilters.put(resourceType, filter);
    }

    public FieldMaskingPolicy getEffectivePolicy(String fieldName) {
        FieldMaskingRule rule = rules.get(fieldName);
        if (rule == null) {
            return new FieldMaskingPolicy(fieldName, MaskingStrategy.FULL_MASK, false, null);
        }
        return new FieldMaskingPolicy(
            fieldName, rule.strategy(), true, rule.description()
        );
    }

    public Set<String> getMaskedFields(Set<String> allFields) {
        Set<String> masked = new LinkedHashSet<>();
        for (String field : allFields) {
            if (rules.containsKey(field)) {
                masked.add(field);
            }
        }
        return masked;
    }

    // Internal

    private String applyPartialMask(String value) {
        if (value.length() <= 4) return "****";
        int show = Math.min(2, value.length() / 4);
        return value.substring(0, show)
            + "*".repeat(value.length() - show * 2)
            + value.substring(value.length() - show);
    }

    private String hashValue(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return "HASH_ERROR";
        }
    }

    private String randomMask(String value) {
        return "*".repeat(value.length());
    }

    private boolean passesRowFilter(String resourceType, UUID userId, Map<String, Object> record) {
        RowLevelFilter filter = rowFilters.get(resourceType);
        if (filter == null) return true;
        return userId != null && filter.test(userId, record);
    }
}