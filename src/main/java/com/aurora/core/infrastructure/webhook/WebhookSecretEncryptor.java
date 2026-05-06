package com.aurora.core.infrastructure.webhook;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Webhook Secret Encryptor — AES-256-GCM encryption at rest.
 *
 * <p>Webhook signing secrets must not be stored in plaintext in the database.
 * This encryptor encrypts secrets before persistence and decrypts them
 * for signing operations.
 *
 * <p>The encryption key is configured via {@code aurora.webhook.encryption-key}
 * (must be 256-bit/32-byte hex). If not configured, a warning is logged
 * and secrets are stored in plaintext (dev mode only).
 */
@Component
public class WebhookSecretEncryptor {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKey encryptionKey;

    public WebhookSecretEncryptor(
            @Value("${aurora.webhook.encryption-key:}") String keyHex) {
        if (keyHex == null || keyHex.isBlank()) {
            this.encryptionKey = null;
            org.slf4j.LoggerFactory.getLogger(WebhookSecretEncryptor.class)
                    .warn("aurora.webhook.encryption-key NOT SET — webhook secrets stored in PLAINTEXT! " +
                          "Set this to a 32-byte hex string for production.");
        } else {
            byte[] keyBytes = HexFormat.of().parseHex(keyHex);
            if (keyBytes.length != 32) {
                throw new IllegalArgumentException(
                        "aurora.webhook.encryption-key must be 32 bytes (64 hex chars)");
            }
            this.encryptionKey = new SecretKeySpec(keyBytes, "AES");
        }
    }

    /**
     * Encrypt a plaintext secret.
     *
     * @param plaintext the secret to encrypt
     * @return base64-encoded ciphertext (IV + ciphertext)
     */
    public String encrypt(String plaintext) {
        if (encryptionKey == null) {
            return "PLAIN:" + plaintext;  // dev mode fallback
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey,
                    new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[GCM_IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH);
            System.arraycopy(ciphertext, 0, combined, GCM_IV_LENGTH, ciphertext.length);

            return "AES:" + Base64.getEncoder().encodeToString(combined);

        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt webhook secret", e);
        }
    }

    /**
     * Decrypt an encrypted secret.
     *
     * @param ciphertext the value stored in the database
     * @return the original plaintext secret
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null) return null;

        if (ciphertext.startsWith("PLAIN:")) {
            return ciphertext.substring(6);
        }

        if (!ciphertext.startsWith("AES:")) {
            return ciphertext;
        }

        if (encryptionKey == null) {
            return ciphertext;
        }

        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext.substring(4));

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey,
                    new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] plaintext = cipher.doFinal(encrypted);
            return new String(plaintext, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt webhook secret", e);
        }
    }
}
