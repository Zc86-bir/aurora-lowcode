package com.aurora.core.infrastructure.security;

import java.lang.annotation.*;

/**
 * Mask annotation for runtime data masking.
 * Apply to fields in response DTOs to automatically mask sensitive data.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface Mask {
    Mask.MaskType type();
    String customPattern() default "";
    int visibleStart() default 0;
    int visibleEnd() default 4;

    enum MaskType {
        ID_CARD, PHONE, EMAIL, BANK_CARD, NAME, ADDRESS, CUSTOM
    }
}