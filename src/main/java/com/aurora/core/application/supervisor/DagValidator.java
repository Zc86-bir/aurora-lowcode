package com.aurora.core.application.supervisor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;

public class DagValidator {

    public DagValidationResult validate(List<SupervisorSubTask> tasks) {
        List<SupervisorSubTask> validatedTasks = ImmutablePayloads.immutableListCopy(tasks);
        Map<String, SupervisorSubTask> tasksById = new LinkedHashMap<>();
        Map<String, Integer> indegree = new LinkedHashMap<>();
        Map<String, Integer> dependencyDepth = new LinkedHashMap<>();
        Map<String, Set<String>> edges = new LinkedHashMap<>();

        for (SupervisorSubTask task : validatedTasks) {
            ImmutablePayloads.requireNonNull(task, "tasks entry");
            if (tasksById.putIfAbsent(task.taskId(), task) != null) {
                throw new IllegalArgumentException("Duplicate taskId: " + task.taskId());
            }
            indegree.put(task.taskId(), 0);
            dependencyDepth.put(task.taskId(), 0);
            edges.put(task.taskId(), new TreeSet<>());
        }

        for (SupervisorSubTask task : validatedTasks) {
            for (String dependency : task.dependencies()) {
                if (dependency.equals(task.taskId())) {
                    throw new IllegalArgumentException("Task cannot depend on itself: " + dependency);
                }
                if (!tasksById.containsKey(dependency)) {
                    throw new IllegalArgumentException("Missing dependency: " + dependency + " for task " + task.taskId());
                }
                edges.get(dependency).add(task.taskId());
                indegree.compute(task.taskId(), (taskId, value) -> value == null ? 1 : value + 1);
            }
        }

        PriorityQueue<String> readyQueue = new PriorityQueue<>();
        indegree.forEach((taskId, value) -> {
            if (value == 0) {
                readyQueue.add(taskId);
            }
        });

        List<String> topologicalOrder = new ArrayList<>(validatedTasks.size());
        int processedCount = 0;

        while (!readyQueue.isEmpty()) {
            String currentTaskId = readyQueue.remove();
            topologicalOrder.add(currentTaskId);
            processedCount++;

            for (String dependentTaskId : edges.get(currentTaskId)) {
                dependencyDepth.put(
                    dependentTaskId,
                    Math.max(dependencyDepth.get(dependentTaskId), dependencyDepth.get(currentTaskId) + 1)
                );

                int updatedIndegree = indegree.compute(
                    dependentTaskId,
                    (taskId, value) -> value == null ? 0 : value - 1
                );
                if (updatedIndegree == 0) {
                    readyQueue.add(dependentTaskId);
                }
            }
        }

        if (processedCount != validatedTasks.size()) {
            List<String> blockedTaskIds = indegree.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(Map.Entry::getKey)
                .toList();
            throw new IllegalArgumentException(
                "Detected cycle in supervisor plan DAG; blocked tasks: " + blockedTaskIds
            );
        }

        Map<String, Integer> dependencyDepthByTaskId = new LinkedHashMap<>();
        for (String taskId : topologicalOrder) {
            dependencyDepthByTaskId.put(taskId, dependencyDepth.get(taskId));
        }

        return new DagValidationResult(topologicalOrder, dependencyDepthByTaskId);
    }
}
