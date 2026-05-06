package com.aurora.core.infrastructure.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.StructuredTaskScope;

/**
 * Web Search Service — wraps opencli CLI for web search and content fetch.
 *
 * <p>Uses {@code opencli smart-search} for web search and {@code opencli web fetch}
 * for content retrieval. Both execute as subprocesses with StructuredTaskScope timeout.
 *
 * <p>Activation: {@code aurora.search.enabled=true} (default: false).
 * opencli must be installed on the system path.
 */
@Service
@ConditionalOnProperty(name = "aurora.search.enabled", havingValue = "true", matchIfMissing = false)
public class WebSearchService {

    private static final Logger log = LoggerFactory.getLogger(WebSearchService.class);
    private static final Duration SEARCH_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration FETCH_TIMEOUT = Duration.ofSeconds(15);

    /**
     * Search the web using opencli smart-search.
     *
     * @param query  search query
     * @param limit  max results (1-10)
     * @return JSON result string from opencli
     */
    public String search(String query, int limit) {
        Instant start = Instant.now();
        try (var scope = StructuredTaskScope.<String, Void>open(
                StructuredTaskScope.Joiner.awaitAll(),
                cfg -> cfg.withTimeout(SEARCH_TIMEOUT))) {

            var task = scope.fork(() -> executeOpencli(
                    "smart-search", query, "-f", "json", "--limit", String.valueOf(Math.min(limit, 10))));

            scope.join();
            String result = task.get();
            log.info("Web search completed in {}ms for query: {}",
                    Duration.between(start, Instant.now()).toMillis(), query);
            return result;

        } catch (StructuredTaskScope.TimeoutException e) {
            log.warn("Web search timed out after {}s for query: {}", SEARCH_TIMEOUT.getSeconds(), query);
            return String.format(
                    "{\"status\":\"timeout\",\"message\":\"Search timed out after %d seconds\"}",
                    SEARCH_TIMEOUT.getSeconds());

        } catch (Exception e) {
            log.error("Web search failed for query '{}': {}", query, e.getMessage());
            return String.format(
                    "{\"status\":\"error\",\"message\":\"Search failed: %s\"}",
                    escapeJson(e.getMessage()));
        }
    }

    /**
     * Fetch and read content from a URL using opencli.
     *
     * @param url the URL to fetch
     * @return content as markdown text
     */
    public String fetch(String url) {
        Instant start = Instant.now();
        try (var scope = StructuredTaskScope.<String, Void>open(
                StructuredTaskScope.Joiner.awaitAll(),
                cfg -> cfg.withTimeout(FETCH_TIMEOUT))) {

            var task = scope.fork(() -> executeOpencli("web", "read", url, "-f", "plain"));

            scope.join();
            String result = task.get();
            log.info("Web fetch completed in {}ms for URL: {}",
                    Duration.between(start, Instant.now()).toMillis(), url);
            return result;

        } catch (StructuredTaskScope.TimeoutException e) {
            log.warn("Web fetch timed out after {}s for URL: {}", FETCH_TIMEOUT.getSeconds(), url);
            return "Error: fetch timed out after " + FETCH_TIMEOUT.getSeconds() + "s";

        } catch (Exception e) {
            log.error("Web fetch failed for URL '{}': {}", url, e.getMessage());
            return "Error: fetch failed - " + escapeJson(e.getMessage());
        }
    }

    private String executeOpencli(String... args) throws Exception {
        String[] cmd = new String[args.length + 1];
        cmd[0] = "opencli";
        System.arraycopy(args, 0, cmd, 1, args.length);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(false);

        Process process = pb.start();

        try (var stdout = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
             var stderr = new BufferedReader(new InputStreamReader(process.getErrorStream(), "UTF-8"))) {

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = stdout.readLine()) != null) {
                output.append(line).append("\n");
            }

            StringBuilder errorOutput = new StringBuilder();
            while ((line = stderr.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.warn("opencli exited with code {} for command '{}': {}",
                        exitCode, args[0], errorOutput);
                throw new RuntimeException("opencli failed (exit=" + exitCode + "): " + errorOutput);
            }

            return output.toString().strip();
        }
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
