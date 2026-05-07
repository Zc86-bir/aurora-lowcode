package com.aurora.core.application.supervisor;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record SupervisorPlan(
    String planName,
    List<SupervisorSubTask> tasks
) {

    public SupervisorPlan {
        planName = ImmutablePayloads.requireNonBlank(planName, "planName");
        var mutableTasks = tasks == null ? List.<SupervisorSubTask>of() : new java.util.ArrayList<>(tasks);
        mutableTasks.forEach(task -> ImmutablePayloads.requireNonNull(task, "tasks entry"));
        tasks = List.copyOf(mutableTasks);
        ImmutablePayloads.requireDistinct(tasks, SupervisorSubTask::taskId, "tasks.taskId");

        var taskIds = tasks.stream()
            .map(SupervisorSubTask::taskId)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
        for (SupervisorSubTask task : tasks) {
            for (String dependency : task.dependencies()) {
                if (task.taskId().equals(dependency)) {
                    throw new IllegalArgumentException("task must not depend on itself: " + task.taskId());
                }
                if (!taskIds.contains(dependency)) {
                    throw new IllegalArgumentException("task dependency must reference a known taskId: " + dependency);
                }
            }
        }

        detectCycles(tasks);
    }

    private static void detectCycles(List<SupervisorSubTask> tasks) {
        Map<String, SupervisorSubTask> tasksById = new LinkedHashMap<>();
        for (SupervisorSubTask task : tasks) {
            tasksById.put(task.taskId(), task);
        }

        var visiting = new ArrayDeque<String>();
        var visited = new java.util.HashSet<String>();
        for (SupervisorSubTask task : tasks) {
            detectCyclesFrom(task.taskId(), tasksById, visiting, visited);
        }
    }

    private static void detectCyclesFrom(
        String taskId,
        Map<String, SupervisorSubTask> tasksById,
        ArrayDeque<String> visiting,
        Set<String> visited
    ) {
        if (visited.contains(taskId)) {
            return;
        }

        int cycleStart = indexOf(visiting, taskId);
        if (cycleStart >= 0) {
            var path = visiting.stream().toList();
            var cycle = new java.util.ArrayList<>(path.subList(cycleStart, path.size()));
            cycle.add(taskId);
            throw new IllegalArgumentException("task dependencies must not contain a cycle: " + String.join(" -> ", cycle));
        }

        visiting.addLast(taskId);
        for (String dependency : tasksById.get(taskId).dependencies()) {
            detectCyclesFrom(dependency, tasksById, visiting, visited);
        }
        visiting.removeLast();
        visited.add(taskId);
    }

    private static int indexOf(ArrayDeque<String> visiting, String taskId) {
        int index = 0;
        for (String candidate : visiting) {
            if (candidate.equals(taskId)) {
                return index;
            }
            index++;
        }
        return -1;
    }
}
