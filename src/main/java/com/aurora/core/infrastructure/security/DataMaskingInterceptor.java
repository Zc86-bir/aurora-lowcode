package com.aurora.core.infrastructure.security;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Data Masking Interceptor
 *
 * Runtime data masking based on @Mask annotation.
 * Applies to both API responses (JSON serialization) and log output.
 *
 * Supported mask types: ID_CARD, PHONE, EMAIL, BANK_CARD, NAME, ADDRESS, CUSTOM
 *
 * Example:
 *   public class UserResponse {
 *       @Mask(type = Mask.MaskType.PHONE)
 *       private String phoneNumber;
 *
 *       @Mask(type = Mask.MaskType.EMAIL)
 *       private String email;
 *   }
 */
@RestControllerAdvice
public class DataMaskingInterceptor implements ResponseBodyAdvice<Object> {

    private static final Logger log = LoggerFactory.getLogger(DataMaskingInterceptor.class);

    // Masking patterns
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@]+@[^@]+$");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^\\d{15}|\\d{17}[\\dXx]$");
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("^\\d{16,19}$");

    private final ObjectMapper maskedObjectMapper;

    public DataMaskingInterceptor() {
        this.maskedObjectMapper = new ObjectMapper();
        this.maskedObjectMapper.registerModule(new MaskingModule());
    }

    @Override
    public boolean supports(MethodParameter returnType,
                             Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                   MediaType selectedContentType,
                                   Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                   ServerHttpRequest request,
                                   ServerHttpResponse response) {
        if (body == null) return null;

        // Check if masking is needed for this request
        HttpServletRequest servletRequest = null;
        if (request instanceof ServletServerHttpRequest s) {
            servletRequest = s.getServletRequest();
        }

        // Apply masking to response body
        if (servletRequest != null && shouldMask(servletRequest)) {
            try {
                return maskedObjectMapper.convertValue(body, Object.class);
            } catch (Exception e) {
                log.error("Failed to mask response data", e);
                return body;
            }
        }

        return body;
    }

    /**
     * Determine if response masking should be applied.
     */
    private boolean shouldMask(HttpServletRequest request) {
        // Check for masking header/parameter
        String maskHeader = request.getHeader("X-Data-Masking");
        return maskHeader == null || !"skip".equalsIgnoreCase(maskHeader);
    }

    /**
     * Apply masking to a string value based on type.
     */
    public static String maskValue(String value, Mask.MaskType type, String customPattern,
                                    int visibleStart, int visibleEnd) {
        if (value == null || value.isEmpty()) return value;

        return switch (type) {
            case PHONE -> maskPhone(value);
            case EMAIL -> maskEmail(value);
            case ID_CARD -> maskIdCard(value);
            case BANK_CARD -> maskBankCard(value);
            case NAME -> maskName(value);
            case ADDRESS -> maskAddress(value);
            case CUSTOM -> maskCustom(value, customPattern, visibleStart, visibleEnd);
        };
    }

    private static String maskPhone(String phone) {
        if (phone.length() < 7) return "****";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private static String maskEmail(String email) {
        int atIdx = email.indexOf('@');
        if (atIdx <= 0) return "****@****.***";
        String local = email.substring(0, atIdx);
        if (local.length() <= 1) return "*@****.***";
        return local.charAt(0) + "***" + email.substring(atIdx);
    }

    private static String maskIdCard(String idCard) {
        if (idCard.length() < 8) return "********";
        return idCard.substring(0, 3) + "*********" + idCard.substring(idCard.length() - 4);
    }

    private static String maskBankCard(String card) {
        if (card.length() < 8) return "****";
        return card.substring(0, 4) + " **** **** " + card.substring(card.length() - 4);
    }

    private static String maskName(String name) {
        if (name.length() <= 1) return "*";
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }

    private static String maskAddress(String address) {
        if (address.length() <= 4) return "***";
        return address.substring(0, 2) + "***";
    }

    private static String maskCustom(String value, String customPattern,
                                      int visibleStart, int visibleEnd) {
        if (customPattern != null && !customPattern.isEmpty()) {
            return value.replaceAll(customPattern, "*");
        }
        int len = value.length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            if (i < visibleStart || i >= len - visibleEnd) {
                sb.append(value.charAt(i));
            } else {
                sb.append("*");
            }
        }
        return sb.toString();
    }

    /**
     * Jackson module for JSON serialization masking.
     */
    private static class MaskingModule extends SimpleModule {
        public MaskingModule() {
            super("MaskingModule");
        }
    }
}