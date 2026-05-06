package com.aurora.core.infrastructure.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * HMAC-SHA256 Webhook Signer.
 *
 * <p>Generates the {@code X-Aurora-Signature} header value for webhook
 * payload verification by recipients. The signature format is:
 * {@code sha256=<hex-encoded HMAC-SHA256 of payload>}.
 *
 * <p>Recipients can verify the signature by computing their own
 * HMAC-SHA256 of the received payload body using the shared secret
 * and comparing it to the signature header.
 */
public final class WebhookSigner {

    private static final Logger log = LoggerFactory.getLogger(WebhookSigner.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    private WebhookSigner() {}

    /**
     * Sign a payload with HMAC-SHA256 using the given secret.
     *
     * @param payload the JSON payload body
     * @param secret  the shared secret for this webhook endpoint
     * @return signature string in format "sha256=<hex>"
     */
    public static String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] hmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return SIGNATURE_PREFIX + HexFormat.of().formatHex(hmac);
        } catch (NoSuchAlgorithmException e) {
            log.error("HMAC-SHA256 not available", e);
            throw new RuntimeException("HMAC-SHA256 algorithm not available", e);
        } catch (InvalidKeyException e) {
            log.error("Invalid key for HMAC-SHA256", e);
            throw new RuntimeException("Invalid webhook secret key", e);
        }
    }

    /**
     * Generate a secure random webhook secret.
     *
     * @return a 256-bit (32-byte) hex-encoded secret
     */
    public static String generateSecret() {
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    /**
     * Verify a signature against a payload and secret.
     * Uses constant-time comparison to prevent timing attacks.
     *
     * @param payload   the raw request body
     * @param secret    the shared secret
     * @param signature the signature from X-Aurora-Signature header
     * @return true if the signature is valid
     */
    public static boolean verify(String payload, String secret, String signature) {
        if (signature == null || !signature.startsWith(SIGNATURE_PREFIX)) {
            return false;
        }
        String expected = sign(payload, secret);
        return constantTimeEquals(expected, signature);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
