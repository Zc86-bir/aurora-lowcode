package com.aurora.core.application.supervisor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record DagValidationResult(
    List<String> stableTopologicalOrder,
    Map<String, Integer> dependencyDepthByTaskId
) {

    public DagValidationResult {
        stableTopologicalOrder = ImmutablePayloads.immutableNonBlankStringList(
            stableTopologicalOrder,
            "stableTopologicalOrder entry"
        );
        dependencyDepthByTaskId = Collections.unmodifiableMap(new LinkedHashMap<>(
            ImmutablePayloads.immutableValidatedStringKeyMapCopy(dependencyDepthByTaskId, "dependencyDepthByTaskId")
        ));

        if (stableTopologicalOrder.size() != dependencyDepthByTaskId.size()) {
            throw new IllegalArgumentException("stableTopologicalOrder and dependencyDepthByTaskId must have the same size");
        }

        var depthEntries = dependencyDepthByTaskId.entrySet().iterator();
        for (String taskId : stableTopologicalOrder) {
            if (!depthEntries.hasNext()) {
                throw new IllegalArgumentException("stableTopologicalOrder and dependencyDepthByTaskId must contain the same taskIds");
            }

            var depthEntry = depthEntries.next();
            if (!taskId.equals(depthEntry.getKey())) {
                throw new IllegalArgumentException("stableTopologicalOrder and dependencyDepthByTaskId must contain the same taskIds");
            }
        }
    }
}
