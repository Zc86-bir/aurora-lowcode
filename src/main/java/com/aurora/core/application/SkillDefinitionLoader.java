package com.aurora.core.application;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Skill Definition Loader
 *
 * Loads and caches MCP skill definitions from YAML files.
 * Supports hot-reload via version comparison and diff sync.
 */
public class SkillDefinitionLoader {

    private final ConcurrentHashMap<String, SkillDefinition> cache = new ConcurrentHashMap<>();
    private final Path skillsDirectory;

    public SkillDefinitionLoader(Path skillsDirectory) {
        this.skillsDirectory = skillsDirectory;
    }

    /**
     * Load all skill definitions from the skills directory.
     */
    public List<SkillDefinition> loadAll() throws IOException {
        if (!Files.exists(skillsDirectory)) {
            return List.of();
        }

        List<SkillDefinition> definitions = new ArrayList<>();

        try (var stream = Files.list(skillsDirectory)) {
            stream.filter(p -> p.toString().endsWith(".yaml") || p.toString().endsWith(".yml"))
                .map(this::loadFromFile)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(def -> {
                    cache.put(def.skillId(), def);
                    definitions.add(def);
                });
        }

        return List.copyOf(definitions);
    }

    /**
     * Load a single skill definition by ID.
     */
    public Optional<SkillDefinition> loadById(String skillId) {
        SkillDefinition cached = cache.get(skillId);
        if (cached != null) {
            return Optional.of(cached);
        }

        Path yamlFile = skillsDirectory.resolve(skillId + ".yaml");
        if (!Files.exists(yamlFile)) {
            yamlFile = skillsDirectory.resolve(skillId + ".yml");
        }
        if (!Files.exists(yamlFile)) {
            return Optional.empty();
        }

        return loadFromFile(yamlFile);
    }

    /**
     * Reload a skill definition from file (hot-reload).
     */
    public Optional<SkillDefinition> reload(String skillId) throws IOException {
        Path yamlFile = skillsDirectory.resolve(skillId + ".yaml");
        if (!Files.exists(yamlFile)) {
            yamlFile = skillsDirectory.resolve(skillId + ".yml");
        }
        if (!Files.exists(yamlFile)) {
            cache.remove(skillId);
            return Optional.empty();
        }

        Optional<SkillDefinition> result = loadFromFile(yamlFile);
        result.ifPresent(def -> cache.put(skillId, def));
        return result;
    }

    /**
     * Get cached definition by ID.
     */
    public Optional<SkillDefinition> get(String skillId) {
        return Optional.ofNullable(cache.get(skillId));
    }

    /**
     * Check if a skill is registered.
     */
    public boolean isRegistered(String skillId) {
        return cache.containsKey(skillId);
    }

    /**
     * Get all registered skill IDs.
     */
    public List<String> getRegisteredIds() {
        return List.copyOf(cache.keySet());
    }

    /**
     * Clear all cached definitions.
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * Get cache size.
     */
    public int cacheSize() {
        return cache.size();
    }

    // Internal

    private Optional<SkillDefinition> loadFromFile(Path path) {
        try {
            String content = Files.readString(path);
            SkillDefinition def = parseYaml(content);
            if (def.skillId() == null) {
                return Optional.empty();
            }
            return Optional.of(def);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private SkillDefinition parseYaml(String content) {
        Map<String, Object> raw = parseYamlContent(content);

        Map<String, Object> inputSchema = (Map<String, Object>) raw.getOrDefault("input_schema", Map.of());
        Map<String, Object> outputSchema = (Map<String, Object>) raw.getOrDefault("output_schema", Map.of());
        List<String> tags = toStringList(raw.get("tags"));
        List<String> businessRules = toStringList(raw.get("business_rules"));
        List<String> securityRules = toStringList(raw.get("security_rules"));

        return new SkillDefinition(
            getString(raw, "skill_id"),
            getString(raw, "name"),
            getString(raw, "description"),
            getString(raw, "version"),
            getString(raw, "category"),
            getString(raw, "executor"),
            getString(raw, "author"),
            getString(raw, "created_at"),
            getString(raw, "updated_at"),
            getBoolean(raw, "deprecated"),
            tags,
            inputSchema,
            outputSchema,
            businessRules,
            securityRules
        );
    }

    /**
     * Recursive YAML parser that handles nested objects via indentation.
     */
    private Map<String, Object> parseYamlContent(String content) {
        String[] lines = content.split("\n");
        return parseYamlLines(lines, 0, 0);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseYamlLines(String[] lines, int startIdx, int baseIndent) {
        Map<String, Object> result = new LinkedHashMap<>();
        int i = startIdx;
        String currentKey = null;
        List<String> currentList = null;

        while (i < lines.length) {
            String line = lines[i];
            String trimmed = line.trim();

            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                i++;
                continue;
            }

            int indent = getIndentLevel(line);
            if (indent < baseIndent) {
                break;
            }
            if (indent > baseIndent && currentKey == null) {
                i++;
                continue;
            }

            // List item at current indent level
            if (trimmed.startsWith("- ")) {
                if (currentKey != null) {
                    if (currentList == null) {
                        currentList = new ArrayList<>();
                    }
                    String itemValue = trimmed.substring(2).trim();
                    // Check if it's a nested object like "- type: string"
                    if (itemValue.contains(":") && !itemValue.startsWith("\"")) {
                        // It's a map item in a list - parse as nested object
                        List<Map<String, Object>> objectList = new ArrayList<>();
                        Map<String, Object> obj = new LinkedHashMap<>();
                        int itemIndent = indent + 2;
                        // Parse the first key-value from the "- key: value"
                        int colonIdx = itemValue.indexOf(':');
                        String k = itemValue.substring(0, colonIdx).trim();
                        String v = itemValue.substring(colonIdx + 1).trim();
                        obj.put(k, parseScalarValue(v));

                        i++;
                        // Continue parsing nested object properties
                        while (i < lines.length) {
                            String nestedLine = lines[i];
                            String nestedTrimmed = nestedLine.trim();
                            if (nestedTrimmed.isEmpty() || nestedTrimmed.startsWith("#")) {
                                i++;
                                continue;
                            }
                            int nestedIndent = getIndentLevel(nestedLine);
                            if (nestedIndent <= indent) {
                                break;
                            }
                            if (nestedTrimmed.startsWith("- ")) {
                                break;
                            }
                            if (nestedTrimmed.contains(":")) {
                                int ci = nestedTrimmed.indexOf(':');
                                String nk = nestedTrimmed.substring(0, ci).trim();
                                String nv = nestedTrimmed.substring(ci + 1).trim();
                                if (nv.isEmpty()) {
                                    // Nested object under this key
                                    int nextIdx = i + 1;
                                    if (nextIdx < lines.length) {
                                        int childIndent = getIndentLevel(lines[nextIdx]);
                                        if (childIndent > nestedIndent) {
                                            obj.put(nk, parseYamlLines(lines, nextIdx, childIndent));
                                            i = nextIdx + 1;
                                            // Skip lines that were parsed
                                            while (i < lines.length && getIndentLevel(lines[i]) > nestedIndent) {
                                                i++;
                                            }
                                            continue;
                                        }
                                    }
                                    obj.put(nk, Map.of());
                                } else {
                                    obj.put(nk, parseScalarValue(nv));
                                }
                            }
                            i++;
                        }
                        objectList.add(obj);
                        // Check for more objects at same list level
                        while (i < lines.length) {
                            String nextLine = lines[i];
                            String nextTrimmed = nextLine.trim();
                            if (nextTrimmed.isEmpty() || nextTrimmed.startsWith("#")) {
                                i++;
                                continue;
                            }
                            if (getIndentLevel(nextLine) != indent || !nextTrimmed.startsWith("- ")) {
                                break;
                            }
                            String nextItem = nextTrimmed.substring(2).trim();
                            Map<String, Object> nextObj = new LinkedHashMap<>();
                            if (nextItem.contains(":") && !nextItem.startsWith("\"")) {
                                int ci2 = nextItem.indexOf(':');
                                nextObj.put(nextItem.substring(0, ci2).trim(),
                                    parseScalarValue(nextItem.substring(ci2 + 1).trim()));
                            }
                            i++;
                            while (i < lines.length) {
                                String nl = lines[i];
                                String nt = nl.trim();
                                if (nt.isEmpty() || nt.startsWith("#")) {
                                    i++;
                                    continue;
                                }
                                if (getIndentLevel(nl) <= indent) {
                                    break;
                                }
                                if (nt.startsWith("- ")) {
                                    break;
                                }
                                if (nt.contains(":")) {
                                    int ci2 = nt.indexOf(':');
                                    String nk2 = nt.substring(0, ci2).trim();
                                    String nv2 = nt.substring(ci2 + 1).trim();
                                    if (nv2.isEmpty()) {
                                        int nextIdx2 = i + 1;
                                        if (nextIdx2 < lines.length && getIndentLevel(lines[nextIdx2]) > getIndentLevel(nl)) {
                                            nextObj.put(nk2, parseYamlLines(lines, nextIdx2, getIndentLevel(lines[nextIdx2])));
                                            i = nextIdx2 + 1;
                                            while (i < lines.length && getIndentLevel(lines[i]) > getIndentLevel(nl)) {
                                                i++;
                                            }
                                            continue;
                                        }
                                        nextObj.put(nk2, Map.of());
                                    } else {
                                        nextObj.put(nk2, parseScalarValue(nv2));
                                    }
                                }
                                i++;
                            }
                            objectList.add(nextObj);
                        }
                        result.put(currentKey, List.copyOf(objectList));
                        currentList = null;
                        currentKey = null;
                        continue;
                    } else {
                        currentList.add(parseScalarValue(itemValue).toString());
                    }
                    i++;
                    continue;
                }
                i++;
                continue;
            }

            // Flush pending list
            if (currentList != null && currentKey != null && !currentList.isEmpty()) {
                result.put(currentKey, List.copyOf(currentList));
                currentList = null;
            }

            // Key: value pair
            if (trimmed.contains(":")) {
                int colonIndex = trimmed.indexOf(':');
                String key = trimmed.substring(0, colonIndex).trim();
                String value = trimmed.substring(colonIndex + 1).trim();

                if (value.isEmpty()) {
                    // Check if next line starts a nested block
                    int nextIdx = i + 1;
                    if (nextIdx < lines.length) {
                        String nextLine = lines[nextIdx];
                        String nextTrimmed = nextLine.trim();
                        if (!nextTrimmed.isEmpty() && !nextTrimmed.startsWith("#")) {
                            int nextIndent = getIndentLevel(nextLine);
                            int currentLineIndent = getIndentLevel(line);
                            if (nextIndent > currentLineIndent) {
                                // Nested object
                                if (!nextTrimmed.startsWith("- ")) {
                                    Map<String, Object> nested = parseYamlLines(lines, nextIdx, nextIndent);
                                    result.put(key, nested);
                                    i = nextIdx;
                                    // Skip lines consumed by nested parse
                                    while (i < lines.length) {
                                        String checkLine = lines[i];
                                        String checkTrimmed = checkLine.trim();
                                        if (checkTrimmed.isEmpty() || checkTrimmed.startsWith("#")) {
                                            i++;
                                            continue;
                                        }
                                        if (getIndentLevel(checkLine) <= currentLineIndent) {
                                            break;
                                        }
                                        i++;
                                    }
                                    currentKey = null;
                                    continue;
                                }
                            }
                        }
                    }
                    // Start of a list or empty block
                    currentKey = key;
                    currentList = new ArrayList<>();
                } else {
                    result.put(key, parseScalarValue(value));
                    currentKey = null;
                    currentList = null;
                }
            }
            i++;
        }

        // Flush last list
        if (currentList != null && currentKey != null && !currentList.isEmpty()) {
            result.put(currentKey, List.copyOf(currentList));
        }

        return result;
    }

    private int getIndentLevel(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') count++;
            else break;
        }
        return count;
    }

    private Object parseScalarValue(String value) {
        if (value.equals("true")) return Boolean.TRUE;
        if (value.equals("false")) return Boolean.FALSE;
        if (value.equals("null")) return null;
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        if (value.startsWith("[") && value.endsWith("]")) {
            // Inline list: [vertical, horizontal, grid]
            String inner = value.substring(1, value.length() - 1).trim();
            if (inner.isEmpty()) return List.of();
            List<String> items = new ArrayList<>();
            for (String item : inner.split(",")) {
                items.add(item.trim().replace("\"", ""));
            }
            return List.copyOf(items);
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // ignore
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            // ignore
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private List<String> toStringList(Object value) {
        if (value == null) return List.of();
        if (value instanceof List<?> list) {
            return list.stream()
                .map(Object::toString)
                .toList();
        }
        return List.of();
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private boolean getBoolean(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean b) return b;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    /**
     * Skill definition record
     */
    public record SkillDefinition(
        String skillId,
        String name,
        String description,
        String version,
        String category,
        String executor,
        String author,
        String createdAt,
        String updatedAt,
        boolean deprecated,
        List<String> tags,
        Map<String, Object> inputSchema,
        Map<String, Object> outputSchema,
        List<String> businessRules,
        List<String> securityRules
    ) {
        /**
         * Check if this skill is compatible with the given version.
         */
        public boolean isCompatibleWith(String requiredVersion) {
            if (version == null || requiredVersion == null) return false;
            String[] currentParts = version.split("\\.");
            String[] requiredParts = requiredVersion.split("\\.");

            if (currentParts.length < 1 || requiredParts.length < 1) return false;

            // Major version must match
            return currentParts[0].equals(requiredParts[0]);
        }

        /**
         * Get the primary category as an enum-like string.
         */
        public String primaryCategory() {
            return category != null ? category : "unknown";
        }
    }
}