package com.aurora.core.application.supervisor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

final class ImmutablePayloads {

    private ImmutablePayloads() {
    }

    static Map<String, Object> copyOf(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return Map.of();
        }

        var copy = new LinkedHashMap<String, Object>(payload.size());
        payload.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                String validatedKey = requireNonBlank(entry.getKey(), "payload key");
                Object validatedValue = requireNonNull(entry.getValue(), "payload value");
                copy.put(validatedKey, copyValue(validatedValue));
            });
        return Collections.unmodifiableMap(copy);
    }

    static <K, V> Map<K, V> immutableMapCopy(Map<K, V> payload) {
        if (payload == null || payload.isEmpty()) {
            return Map.of();
        }

        return Collections.unmodifiableMap(new LinkedHashMap<>(payload));
    }

    static <V> Map<String, V> immutableValidatedStringKeyMapCopy(Map<String, V> payload, String fieldName) {
        if (payload == null || payload.isEmpty()) {
            return Map.of();
        }

        var copy = new LinkedHashMap<String, V>(payload.size());
        payload.forEach((key, value) -> {
            String validatedKey = requireNonBlank(key, fieldName + " key");
            V validatedValue = requireNonNull(value, fieldName + " value");
            copy.put(validatedKey, validatedValue);
        });
        return Collections.unmodifiableMap(copy);
    }

    static <T> List<T> immutableListCopy(List<T> payload) {
        if (payload == null || payload.isEmpty()) {
            return List.of();
        }

        return List.copyOf(payload);
    }

    static List<String> immutableNonBlankStringList(List<String> values, String fieldName) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        return List.copyOf(values.stream()
            .map(value -> requireNonBlank(value, fieldName))
            .toList());
    }

    static <T, K> void requireDistinct(List<T> values, Function<T, K> keyExtractor, String fieldName) {
        var seen = new LinkedHashSet<K>();
        for (T value : values) {
            K key = keyExtractor.apply(value);
            if (!seen.add(key)) {
                throw new IllegalArgumentException(fieldName + " must not contain duplicates: " + key);
            }
        }
    }

    static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    static <T> T requireNonNull(T value, String fieldName) {
        return Objects.requireNonNull(value, fieldName + " must not be null");
    }

    static void requirePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }

    static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static Object copyValue(Object value) {
        if (value instanceof Map<?, ?> nestedMap) {
            var copy = new LinkedHashMap<Object, Object>(nestedMap.size());
            nestedMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(String::valueOf)))
                .forEach(entry -> {
                    Object rawKey = entry.getKey();
                    if (!(rawKey instanceof String stringKey)) {
                        throw new IllegalArgumentException("nested map key must be a non-blank String key");
                    }

                    String validatedKey = requireNonBlank(stringKey, "nested map key");
                    Object nestedValue = requireNonNull(entry.getValue(), "nested map value");
                    copy.put(validatedKey, copyValue(nestedValue));
                });
            return Collections.unmodifiableMap(copy);
        }
        if (value instanceof List<?> nestedList) {
            return List.copyOf(nestedList.stream().map(ImmutablePayloads::copyValue).toList());
        }
        if (value instanceof Set<?> nestedSet) {
            var canonicalValues = nestedSet.stream()
                .map(ImmutablePayloads::copyValue)
                .sorted(Comparator.comparing(ImmutablePayloads::canonicalizeValue))
                .toList();
            var copy = new LinkedHashSet<Object>(canonicalValues.size());
            canonicalValues.forEach(copy::add);
            return Collections.unmodifiableSet(copy);
        }
        if (isSupportedLeafValue(value)) {
            return value;
        }
        throw new IllegalArgumentException("unsupported payload leaf type: " + value.getClass().getName());
    }

    private static boolean isSupportedLeafValue(Object value) {
        return value instanceof String
            || value instanceof Byte
            || value instanceof Short
            || value instanceof Integer
            || value instanceof Long
            || value instanceof Float
            || value instanceof Double
            || value instanceof Boolean
            || value instanceof Character
            || value instanceof UUID
            || value instanceof Instant
            || value instanceof BigDecimal
            || value instanceof BigInteger
            || value instanceof Enum<?>;
    }

    private static String canonicalizeValue(Object value) {
        if (value instanceof Map<?, ?> nestedMap) {
            return nestedMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(String::valueOf)))
                .map(entry -> canonicalizeValue(entry.getKey()) + ":" + canonicalizeValue(entry.getValue()))
                .collect(java.util.stream.Collectors.joining(",", "map{", "}"));
        }
        if (value instanceof List<?> nestedList) {
            return nestedList.stream()
                .map(ImmutablePayloads::canonicalizeValue)
                .collect(java.util.stream.Collectors.joining(",", "list[", "]"));
        }
        if (value instanceof Set<?> nestedSet) {
            return nestedSet.stream()
                .map(ImmutablePayloads::canonicalizeValue)
                .sorted()
                .collect(java.util.stream.Collectors.joining(",", "set(", ")"));
        }
        return value == null ? "null" : value.getClass().getName() + ":" + String.valueOf(value);
    }
}
