package com.aurora.core.application.supervisor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class SupervisorPromptFactoryTest {

    @Test
    void shouldIncludeStrictJsonShapeAndOutputRules() {
        String prompt = new SupervisorPromptFactory().buildPlannerPrompt("Build a CRM app");

        assertThat(prompt)
            .contains("Return valid JSON only.")
            .contains("Do not wrap the response in markdown, code fences, or commentary.")
            .contains("\"planName\": \"string\"")
            .contains("\"tasks\": [")
            .contains("\"taskId\": \"string\"")
            .contains("\"skillId\": \"string\"")
            .contains("\"dependencies\": [\"string\"]")
            .contains("\"parameters\": {}")
            .contains("User request:\nBuild a CRM app");
    }

    @Test
    void shouldStateRequiredNonNullAndNoExtraPropertyRules() {
        String prompt = new SupervisorPromptFactory().buildPlannerPrompt("Build a CRM app");

        assertThat(prompt)
            .contains("- All fields are required.")
            .contains("- No field may be null.")
            .contains("- The root object must not contain extra properties.")
            .contains("- Each task object must not contain extra properties.");
    }

    @Test
    void shouldDistinguishClosedOuterObjectsFromOpenParametersObject() {
        String prompt = new SupervisorPromptFactory().buildPlannerPrompt("Build a CRM app");

        assertThat(prompt)
            .contains("The response must be a single JSON object with exactly this top-level shape:")
            .contains("- The root object is closed: only planName and tasks are allowed.")
            .contains("- Each task object is closed: only taskId, skillId, dependencies, and parameters are allowed.")
            .contains("- parameters must be a JSON object and is open: it may contain any task-specific fields.")
            .doesNotContain("no additional fields anywhere")
            .doesNotContain("- planName, tasks, taskId, skillId, dependencies, and parameters are the only allowed fields.");
    }

    @Test
    void shouldStateDependenciesAndParametersTypeRules() {
        String prompt = new SupervisorPromptFactory().buildPlannerPrompt("Build a CRM app");

        assertThat(prompt)
            .contains("- dependencies: JSON array of prerequisite taskId string values; use [] when none.")
            .contains("- parameters must be a JSON object and is open: it may contain any task-specific fields.")
            .contains("- parameters: use {} when empty.");
    }

    @Test
    void shouldStateDagIntegrityRules() {
        String prompt = new SupervisorPromptFactory().buildPlannerPrompt("Build a CRM app");

        assertThat(prompt)
            .contains("- Task IDs must be unique.")
            .contains("- A task must not depend on itself.")
            .contains("- A task must not list the same dependency more than once.")
            .contains("- Every dependency must reference a known taskId from this same plan.")
            .contains("- The task dependency graph must not contain cycles.")
            .contains("- Output must be parseable as strict JSON.");
    }

    @Test
    void shouldRejectNullInput() {
        assertThatNullPointerException()
            .isThrownBy(() -> new SupervisorPromptFactory().buildPlannerPrompt(null))
            .withMessage("userPrompt");
    }

    @Test
    void shouldRejectBlankInput() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new SupervisorPromptFactory().buildPlannerPrompt("   \t  "))
            .withMessage("userPrompt must not be blank");
    }
}
