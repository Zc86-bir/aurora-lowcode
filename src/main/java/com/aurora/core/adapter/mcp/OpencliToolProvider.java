package com.aurora.core.adapter.mcp;

import com.aurora.core.infrastructure.search.WebSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * MCP Tool Provider — registers web search and fetch tools powered by opencli.
 *
 * <p>Provides two MCP tools for AI model use:
 * <ul>
 *   <li>{@code web_search} — search the web for information</li>
 *   <li>{@code web_fetch} — fetch and read content from a URL</li>
 * </ul>
 *
 * <p>Activated when {@link WebSearchService} bean is available
 * ({@code aurora.search.enabled=true}).
 */
@Component
@ConditionalOnBean(WebSearchService.class)
public class OpencliToolProvider implements ToolCallbackProvider {

    private static final Logger log = LoggerFactory.getLogger(OpencliToolProvider.class);

    private final List<ToolCallback> toolCallbacks = new ArrayList<>();

    public OpencliToolProvider(WebSearchService searchService) {
        toolCallbacks.add(new SearchToolCallback(searchService));
        toolCallbacks.add(new FetchToolCallback(searchService));
        log.info("Registered {} opencli MCP tools", toolCallbacks.size());
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        return toolCallbacks.toArray(new ToolCallback[0]);
    }

    // ==================== Web Search Tool ====================

    static class SearchToolCallback implements ToolCallback {

        private final WebSearchService searchService;
        private final ToolDefinition definition;

        SearchToolCallback(WebSearchService searchService) {
            this.searchService = searchService;
            this.definition = ToolDefinition.builder()
                    .name("web_search")
                    .description("Search the web for information. Use this to find current information, news, documentation, or any online content.")
                    .inputSchema("""
                            {"type":"object","properties":{
                              "query":{"type":"string","description":"The search query"},
                              "limit":{"type":"integer","description":"Max results (1-10)","default":5}
                            },"required":["query"]}
                            """)
                    .build();
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return definition;
        }

        @Override
        public String call(String toolInput) {
            try {
                // Parse input JSON to extract query
                String query = extractJsonField(toolInput, "query");
                int limit = parseIntField(toolInput, "limit", 5);

                if (query == null || query.isBlank()) {
                    return "{\"status\":\"error\",\"message\":\"Missing 'query' parameter\"}";
                }

                return searchService.search(query, limit);
            } catch (Exception e) {
                log.error("web_search tool failed: {}", e.getMessage());
                return "{\"status\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}";
            }
        }
    }

    // ==================== Web Fetch Tool ====================

    static class FetchToolCallback implements ToolCallback {

        private final WebSearchService searchService;
        private final ToolDefinition definition;

        FetchToolCallback(WebSearchService searchService) {
            this.searchService = searchService;
            this.definition = ToolDefinition.builder()
                    .name("web_fetch")
                    .description("Fetch and read the content of a web page. Returns the page as plain text/markdown.")
                    .inputSchema("""
                            {"type":"object","properties":{
                              "url":{"type":"string","description":"The URL to fetch"}
                            },"required":["url"]}
                            """)
                    .build();
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return definition;
        }

        @Override
        public String call(String toolInput) {
            try {
                String url = extractJsonField(toolInput, "url");
                if (url == null || url.isBlank()) {
                    return "{\"status\":\"error\",\"message\":\"Missing 'url' parameter\"}";
                }
                return searchService.fetch(url);
            } catch (Exception e) {
                log.error("web_fetch tool failed: {}", e.getMessage());
                return "{\"status\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}";
            }
        }
    }

    // ==================== JSON Helpers ====================

    private static String extractJsonField(String json, String field) {
        // Simple JSON field extraction (no Jackson dependency in this class)
        String searchKey = "\"" + field + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex < 0) return null;

        int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex < 0) return null;

        int start = colonIndex + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        if (start >= json.length()) return null;

        if (json.charAt(start) == '"') {
            start++;
            StringBuilder sb = new StringBuilder();
            for (int i = start; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '\\') {
                    i++;
                    if (i < json.length()) sb.append(json.charAt(i));
                } else if (c == '"') {
                    break;
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        } else {
            // Number value
            StringBuilder sb = new StringBuilder();
            for (int i = start; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == ',' || c == '}' || c == ']' || Character.isWhitespace(c)) break;
                sb.append(c);
            }
            return sb.toString();
        }
    }

    private static int parseIntField(String json, String field, int defaultValue) {
        String val = extractJsonField(json, field);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
