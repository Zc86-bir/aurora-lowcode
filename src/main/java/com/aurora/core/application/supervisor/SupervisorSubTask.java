package com.aurora.core.application.supervisor;

import java.util.List;
import java.util.Map;

public record SupervisorSubTask(
    String taskId,
    String skillId,
    List<String> dependencies,
    Map<String, Object> parameters
) {

    public SupervisorSubTask {
        taskId = ImmutablePayloads.requireNonBlank(taskId, "taskId");
        skillId = ImmutablePayloads.requireNonBlank(skillId, "skillId");
        dependencies = ImmutablePayloads.immutableNonBlankStringList(dependencies, "dependencies entry");
        ImmutablePayloads.requireDistinct(dependencies, dependency -> dependency, "dependencies");
        parameters = ImmutablePayloads.copyOf(parameters);
    }
}
