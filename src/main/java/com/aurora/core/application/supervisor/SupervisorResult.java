package com.aurora.core.application.supervisor;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record SupervisorResult(
    boolean success,
    String requestId,
    String planName,
    Instant completedAt,
    Map<String, TaskResult> taskResultsByTaskId,
    List<RollbackEntry> rollbackEntries,
    List<String> rollbackAuditMessages,
    String errorMessage
) {

    public SupervisorResult {
        requestId = ImmutablePayloads.requireNonBlank(requestId, "requestId");
        planName = ImmutablePayloads.requireNonBlank(planName, "planName");
        completedAt = ImmutablePayloads.requireNonNull(completedAt, "completedAt");
        taskResultsByTaskId = canonicalizeTaskResultsByTaskId(taskResultsByTaskId);
        rollbackEntries = ImmutablePayloads.immutableListCopy(rollbackEntries);
        rollbackAuditMessages = ImmutablePayloads.immutableListCopy(rollbackAuditMessages);

        if (success && ImmutablePayloads.hasText(errorMessage)) {
            throw new IllegalArgumentException("errorMessage must be blank when success is true");
        }
        if (!success && !ImmutablePayloads.hasText(errorMessage)) {
            throw new IllegalArgumentException("errorMessage must be provided when success is false");
        }
    }

    private static Map<String, TaskResult> canonicalizeTaskResultsByTaskId(Map<String, TaskResult> taskResultsByTaskId) {
        if (taskResultsByTaskId == null || taskResultsByTaskId.isEmpty()) {
            return Map.of();
        }

        taskResultsByTaskId.forEach((taskId, taskResult) -> {
            ImmutablePayloads.requireNonBlank(taskId, "taskResultsByTaskId key");
            ImmutablePayloads.requireNonNull(taskResult, "taskResultsByTaskId value");
        });

        var copy = new LinkedHashMap<String, TaskResult>(taskResultsByTaskId.size());
        taskResultsByTaskId.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> copy.put(entry.getKey(), entry.getValue()));
        return Collections.unmodifiableMap(copy);
    }

    public record TaskResult(
        boolean success,
        Map<String, Object> output,
        String errorMessage
    ) {

        public TaskResult {
            output = ImmutablePayloads.copyOf(output);

            if (success && ImmutablePayloads.hasText(errorMessage)) {
                throw new IllegalArgumentException("errorMessage must be blank when success is true");
            }
            if (!success && !ImmutablePayloads.hasText(errorMessage)) {
                throw new IllegalArgumentException("errorMessage must be provided when success is false");
            }
        }
    }
}
