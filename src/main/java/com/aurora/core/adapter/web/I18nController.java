package com.aurora.core.adapter.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * I18N Controller — returns all message keys for a given locale.
 *
 * <p>{@code GET /api/v1/i18n/{locale}} returns all key-value pairs
 * from the corresponding {@code messages_{locale}.properties} file.
 */
@Tag(name = "I18N", description = "Internationalization message endpoints")
@RestController
@RequestMapping("/api/v1/i18n")
public class I18nController {

    private final MessageSource messageSource;

    public I18nController(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Operation(summary = "Get all i18n messages for a locale")
    @ApiResponse(responseCode = "200", description = "Messages retrieved successfully")
    @GetMapping("/{locale}")
    public ResponseEntity<Map<String, String>> getMessages(@PathVariable String locale) {
        Locale targetLocale = parseLocale(locale);

        try {
            ResourceBundle bundle = ResourceBundle.getBundle("messages", targetLocale);
            Map<String, String> messages = new TreeMap<>();
            bundle.keySet().forEach(key ->
                    messages.put(key, bundle.getString(key)));
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            // Fallback: return empty map for unsupported locales
            return ResponseEntity.ok(Map.of());
        }
    }

    private Locale parseLocale(String locale) {
        return switch (locale.toLowerCase().replace("-", "_")) {
            case "zh", "zh_cn" -> Locale.SIMPLIFIED_CHINESE;
            case "zh_tw" -> Locale.TRADITIONAL_CHINESE;
            case "ja" -> Locale.JAPANESE;
            case "ko" -> Locale.KOREAN;
            default -> Locale.ENGLISH;
        };
    }
}
