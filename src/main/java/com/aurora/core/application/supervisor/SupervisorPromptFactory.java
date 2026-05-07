package com.aurora.core.application.supervisor;

import java.util.Objects;

public class SupervisorPromptFactory {

    public String buildPlannerPrompt(String userPrompt) {
        String prompt = Objects.requireNonNull(userPrompt, "userPrompt").trim();
        if (prompt.isEmpty()) {
            throw new IllegalArgumentException("userPrompt must not be blank");
        }

        return """
            You are Aurora's supervisor planner.
            Break the user request into a dependency-safe task DAG for downstream execution.

            Return valid JSON only.
            Do not wrap the response in markdown, code fences, or commentary.
            The response must be a single JSON object with exactly this top-level shape:
            {
              "planName": "string",
              "tasks": [
                {
                  "taskId": "string",
                  "skillId": "string",
                  "dependencies": ["string"],
                  "parameters": {}
                }
              ]
            }

            Contract requirements:
            - All fields are required.
            - No field may be null.
            - The root object must not contain extra properties.
            - Each task object must not contain extra properties.
            - The root object is closed: only planName and tasks are allowed.
            - Each task object is closed: only taskId, skillId, dependencies, and parameters are allowed.
            - planName: short, human-readable name for the plan.
            - taskId: stable unique identifier for each task.
            - skillId: the exact skill identifier to execute.
            - dependencies: JSON array of prerequisite taskId string values; use [] when none.
            - parameters must be a JSON object and is open: it may contain any task-specific fields.
            - parameters: use {} when empty.
            - Task IDs must be unique.
            - A task must not depend on itself.
            - A task must not list the same dependency more than once.
            - Every dependency must reference a known taskId from this same plan.
            - The task dependency graph must not contain cycles.
            - Output must be parseable as strict JSON.

            User request:
            %s
            """.formatted(prompt);
    }
}
