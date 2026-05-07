package com.aurora.core.application.supervisor;

import java.util.UUID;

public record SupervisorRequest(
    UUID tenantId,
    UUID userId,
    String prompt,
    String requestId
) {

    public SupervisorRequest {
        tenantId = ImmutablePayloads.requireNonNull(tenantId, "tenantId");
        userId = ImmutablePayloads.requireNonNull(userId, "userId");
        prompt = ImmutablePayloads.requireNonBlank(prompt, "prompt");
        requestId = ImmutablePayloads.requireNonBlank(requestId, "requestId");
    }
}
