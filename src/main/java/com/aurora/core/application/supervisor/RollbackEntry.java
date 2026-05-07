package com.aurora.core.application.supervisor;

import java.util.UUID;

public record RollbackEntry(
    UUID metadataId,
    String metadataName,
    int targetVersion,
    String taskId
) {

    public RollbackEntry {
        metadataId = ImmutablePayloads.requireNonNull(metadataId, "metadataId");
        metadataName = ImmutablePayloads.requireNonBlank(metadataName, "metadataName");
        ImmutablePayloads.requirePositive(targetVersion, "targetVersion");
        taskId = ImmutablePayloads.requireNonBlank(taskId, "taskId");
    }
}
