package com.aurora.core.infrastructure.webhook;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Set;

/**
 * URL Validation Utility — prevents SSRF attacks on webhook endpoints.
 *
 * <p>Validates that URLs:
 * <ul>
 *   <li>Use HTTPS (required for production)</li>
 *   <li>Do not target private/loopback/link-local IPs</li>
 *   <li>Do not contain embedded credentials</li>
 *   <li>Are well-formed URIs</li>
 * </ul>
 */
public final class UrlValidator {

    private static final Set<String> BLOCKED_HOSTS = Set.of(
            "localhost", "127.0.0.1", "0.0.0.0",
            "169.254.169.254",   // AWS IMDS
            "metadata.google.internal"  // GCP metadata
    );

    private UrlValidator() {}

    /**
     * Validate a webhook URL for SSRF safety.
     *
     * @param url the URL to validate
     * @param requireHttps whether HTTPS is required
     * @throws IllegalArgumentException if the URL is unsafe
     */
    public static void validate(String url, boolean requireHttps) {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid URL format: " + url);
        }

        if (uri.getScheme() == null) {
            throw new IllegalArgumentException("URL must have a scheme (https://)");
        }

        if (requireHttps && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("URL must use HTTPS for security: " + url);
        }

        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("URL must have a host: " + url);
        }

        String host = uri.getHost().toLowerCase();

        // Block known SSRF targets
        if (BLOCKED_HOSTS.contains(host)) {
            throw new IllegalArgumentException("URL targets a blocked host: " + host);
        }

        // Block raw IP addresses in private/link-local/loopback ranges
        try {
            InetAddress addr = InetAddress.getByName(host);
            if (addr.isLoopbackAddress() || addr.isLinkLocalAddress()
                    || addr.isSiteLocalAddress() || addr.isAnyLocalAddress()) {
                throw new IllegalArgumentException(
                        "URL targets a private/internal network address: " + host);
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Cannot resolve host: " + host);
        }

        // Block embedded credentials in URL
        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("URL must not contain credentials");
        }
    }
}
