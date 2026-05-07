package com.aurora.core.application.supervisor;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SupervisorDtoShapeTest {

    @Test
    void shouldExposeTaskOneSupervisorDtoFields() {
        var prerequisite = new SupervisorSubTask(
            "T0",
            "jeecg-system",
            List.of(),
            Map.of("description", "bootstrap lookup")
        );
        var task = new SupervisorSubTask(
            "T1",
            "jeecg-onlform",
            List.of("T0"),
            Map.of("description", "customer form")
        );
        var plan = new SupervisorPlan("CRM bootstrap", List.of(prerequisite, task));
        var request = new SupervisorRequest(UUID.randomUUID(), UUID.randomUUID(), "build customer onboarding", "REQ-1");
        var rollback = new RollbackEntry(UUID.randomUUID(), "customerForm", 3, "T1");
        var taskResult = new SupervisorResult.TaskResult(
            true,
            Map.of("formCode", "customer_form"),
            null
        );
        var result = new SupervisorResult(
            true,
            "REQ-1",
            "CRM bootstrap",
            Instant.parse("2026-01-01T00:00:03Z"),
            Map.of("T1", taskResult),
            List.of(rollback),
            List.of("Rollback ready"),
            null
        );

        assertThat(plan.planName()).isEqualTo("CRM bootstrap");
        assertThat(plan.tasks()).extracting(SupervisorSubTask::taskId).containsExactly("T0", "T1");
        assertThat(task.dependencies()).containsExactly("T0");
        assertThat(task.parameters()).containsEntry("description", "customer form");
        assertThat(request.requestId()).isEqualTo("REQ-1");
        assertThat(request.prompt()).isEqualTo("build customer onboarding");
        assertThat(rollback.metadataName()).isEqualTo("customerForm");
        assertThat(result.taskResultsByTaskId()).containsOnlyKeys("T1");
        assertThat(result.taskResultsByTaskId().get("T1").output()).containsEntry("formCode", "customer_form");
        assertThat(result.taskResultsByTaskId().get("T1").errorMessage()).isNull();
        assertThat(result.rollbackEntries()).singleElement().extracting(RollbackEntry::taskId).isEqualTo("T1");
        assertThat(result.rollbackAuditMessages()).containsExactly("Rollback ready");
    }

    @Test
    void shouldDefensivelyCopyMutableCollections() {
        var dependencies = new ArrayList<>(List.of("T0"));
        var parameters = new LinkedHashMap<String, Object>(Map.of("description", "customer form"));
        var taskResultsByTaskId = new LinkedHashMap<String, SupervisorResult.TaskResult>();
        var rollbackEntries = new ArrayList<RollbackEntry>();
        var rollbackAuditMessages = new ArrayList<>(List.of("Rollback ready"));

        var prerequisite = new SupervisorSubTask("T0", "jeecg-system", List.of(), Map.of("description", "bootstrap lookup"));
        var task = new SupervisorSubTask("T1", "jeecg-onlform", dependencies, parameters);
        var plan = new SupervisorPlan("CRM bootstrap", new ArrayList<>(List.of(prerequisite, task)));
        var rollback = new RollbackEntry(UUID.randomUUID(), "customerForm", 3, "T1");
        var taskResultOutput = new LinkedHashMap<String, Object>(Map.of("formCode", "customer_form"));
        var taskResult = new SupervisorResult.TaskResult(
            true,
            taskResultOutput,
            null
        );
        taskResultsByTaskId.put("T1", taskResult);
        rollbackEntries.add(rollback);

        var result = new SupervisorResult(
            true,
            "REQ-1",
            "CRM bootstrap",
            Instant.parse("2026-01-01T00:00:03Z"),
            taskResultsByTaskId,
            rollbackEntries,
            rollbackAuditMessages,
            null
        );

        dependencies.add("T2");
        parameters.put("description", "mutated");
        taskResultOutput.put("formCode", "mutated_form");
        taskResultsByTaskId.put("T2", taskResult);
        rollbackEntries.add(new RollbackEntry(UUID.randomUUID(), "other", 1, "T2"));
        rollbackAuditMessages.add("mutated");

        assertThat(task.dependencies()).containsExactly("T0");
        assertThat(task.parameters()).containsEntry("description", "customer form");
        assertThat(plan.tasks()).extracting(SupervisorSubTask::taskId).containsExactly("T0", "T1");
        assertThat(result.taskResultsByTaskId()).containsOnlyKeys("T1");
        assertThat(result.taskResultsByTaskId().get("T1").output()).containsEntry("formCode", "customer_form");
        assertThat(result.taskResultsByTaskId().get("T1").errorMessage()).isNull();
        assertThat(result.rollbackEntries()).containsExactly(rollback);
        assertThat(result.rollbackAuditMessages()).containsExactly("Rollback ready");

        assertThatThrownBy(() -> task.dependencies().add("T3")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> task.parameters().put("extra", true)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> plan.tasks().add(task)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.taskResultsByTaskId().put("T2", taskResult)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.taskResultsByTaskId().get("T1").output().put("extra", true)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.rollbackEntries().add(rollback)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.rollbackAuditMessages().add("boom")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldCanonicalizeTaskResultsByTaskIdKeyOrderDeterministically() {
        var taskResultA = new SupervisorResult.TaskResult(true, Map.of("a", 1), null);
        var taskResultB = new SupervisorResult.TaskResult(true, Map.of("b", 2), null);

        var firstInsertionOrder = new LinkedHashMap<String, SupervisorResult.TaskResult>();
        firstInsertionOrder.put("T2", taskResultB);
        firstInsertionOrder.put("T1", taskResultA);

        var secondInsertionOrder = new LinkedHashMap<String, SupervisorResult.TaskResult>();
        secondInsertionOrder.put("T1", taskResultA);
        secondInsertionOrder.put("T2", taskResultB);

        var firstResult = new SupervisorResult(
            true,
            "REQ-1",
            "CRM bootstrap",
            Instant.parse("2026-01-01T00:00:03Z"),
            firstInsertionOrder,
            List.of(),
            List.of(),
            null
        );
        var secondResult = new SupervisorResult(
            true,
            "REQ-1",
            "CRM bootstrap",
            Instant.parse("2026-01-01T00:00:03Z"),
            secondInsertionOrder,
            List.of(),
            List.of(),
            null
        );

        assertThat(firstResult.taskResultsByTaskId().keySet()).containsExactly("T1", "T2");
        assertThat(secondResult.taskResultsByTaskId().keySet()).containsExactly("T1", "T2");
    }

    @Test
    void shouldNormalizeNullableCollectionsToEmpty() {
        var task = new SupervisorSubTask("T1", "jeecg-onlform", null, null);
        var plan = new SupervisorPlan("CRM bootstrap", null);
        var result = new SupervisorResult(
            true,
            "REQ-1",
            "CRM bootstrap",
            Instant.parse("2026-01-01T00:00:03Z"),
            null,
            null,
            null,
            null
        );
        var taskResult = new SupervisorResult.TaskResult(true, null, null);

        assertThat(task.dependencies()).isEmpty();
        assertThat(task.parameters()).isEmpty();
        assertThat(plan.tasks()).isEmpty();
        assertThat(result.taskResultsByTaskId()).isEmpty();
        assertThat(result.rollbackEntries()).isEmpty();
        assertThat(result.rollbackAuditMessages()).isEmpty();
        assertThat(taskResult.output()).isEmpty();
    }

    @Test
    void shouldRecursivelyFreezeNestedPayloadStructures() {
        var nestedList = new ArrayList<>(List.of("fieldA"));
        var nestedSet = new LinkedHashSet<String>(List.of("rollback", "cleanup"));
        var nestedMap = new LinkedHashMap<String, Object>();
        nestedMap.put("fields", nestedList);
        nestedMap.put("flags", nestedSet);

        var parameters = new LinkedHashMap<String, Object>();
        parameters.put("config", nestedMap);

        var outputNestedList = new ArrayList<>(List.of(Map.of("name", "customer_form")));
        var output = new LinkedHashMap<String, Object>();
        output.put("artifacts", outputNestedList);

        var subTask = new SupervisorSubTask("T1", "jeecg-onlform", List.of(), parameters);
        var taskResult = new SupervisorResult.TaskResult(true, output, null);

        nestedList.add("fieldB");
        nestedSet.add("archive");
        nestedMap.put("extra", true);
        outputNestedList.add(Map.of("name", "mutated_form"));

        assertThat(subTask.parameters()).containsOnlyKeys("config");
        var copiedConfig = asStringObjectMap(subTask.parameters().get("config"));
        var copiedFields = asStringList(copiedConfig.get("fields"));
        var copiedFlags = asStringSet(copiedConfig.get("flags"));
        var copiedArtifacts = asObjectMapList(taskResult.output().get("artifacts"));

        assertThat(copiedConfig).containsOnlyKeys("fields", "flags");
        assertThat(copiedFields).containsExactly("fieldA");
        assertThat(copiedFlags).containsExactly("cleanup", "rollback");
        assertThat(copiedArtifacts)
            .containsExactly(Map.of("name", "customer_form"));

        assertThatThrownBy(() -> copiedConfig.put("other", false))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> copiedFields.add("fieldC"))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> copiedFlags.add("archive"))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> copiedArtifacts.add(Map.of("name", "boom")))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldRejectIndirectTaskDependencyCycles() {
        assertThatThrownBy(() -> new SupervisorPlan(
            "CRM bootstrap",
            List.of(
                new SupervisorSubTask("T1", "skill-a", List.of("T2"), Map.of()),
                new SupervisorSubTask("T2", "skill-b", List.of("T3"), Map.of()),
                new SupervisorSubTask("T3", "skill-c", List.of("T1"), Map.of())
            )
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cycle")
            .hasMessageContaining("T1")
            .hasMessageContaining("T2")
            .hasMessageContaining("T3");
    }

    @Test
    void shouldCanonicalizeNestedSetPayloadValuesDeterministically() {
        var nestedFirst = new LinkedHashSet<Object>();
        nestedFirst.add(Map.of("step", 2, "label", "cleanup"));
        nestedFirst.add("rollback");
        nestedFirst.add(List.of("b", "a"));

        var nestedSecond = new LinkedHashSet<Object>();
        nestedSecond.add(List.of("b", "a"));
        nestedSecond.add("rollback");
        nestedSecond.add(Map.of("step", 2, "label", "cleanup"));

        var firstTask = new SupervisorSubTask(
            "T1",
            "jeecg-onlform",
            List.of(),
            Map.of("config", Map.of("flags", nestedFirst))
        );
        var secondTask = new SupervisorSubTask(
            "T2",
            "jeecg-onlform",
            List.of(),
            Map.of("config", Map.of("flags", nestedSecond))
        );

        var firstFlags = asObjectSet(asStringObjectMap(firstTask.parameters().get("config")).get("flags"));
        var secondFlags = asObjectSet(asStringObjectMap(secondTask.parameters().get("config")).get("flags"));

        assertThat(firstFlags).containsExactlyElementsOf(secondFlags);
        assertThat(firstFlags)
            .containsExactly(
                "rollback",
                List.of("b", "a"),
                Map.of("label", "cleanup", "step", 2)
            );
    }

    @Test
    void shouldRejectInvalidDtoConstruction() {
        var tenantId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var taskResultsWithNullKey = new LinkedHashMap<String, SupervisorResult.TaskResult>();
        taskResultsWithNullKey.put(null, new SupervisorResult.TaskResult(true, Map.of(), null));
        var taskResultsWithBlankKey = new LinkedHashMap<String, SupervisorResult.TaskResult>();
        taskResultsWithBlankKey.put(" ", new SupervisorResult.TaskResult(true, Map.of(), null));
        var taskResultsWithNullValue = new LinkedHashMap<String, SupervisorResult.TaskResult>();
        taskResultsWithNullValue.put("T1", null);

        assertThatThrownBy(() -> new SupervisorPlan(" ", List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("planName");
        assertThatThrownBy(() -> new SupervisorSubTask("", "skill", List.of(), Map.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("taskId");
        assertThatThrownBy(() -> new SupervisorSubTask("T1", " ", List.of(), Map.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("skillId");
        assertThatThrownBy(() -> new SupervisorSubTask("T1", "skill", List.of(" "), Map.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("dependencies entry");
        assertThatThrownBy(() -> new SupervisorSubTask("T1", "skill", new ArrayList<>(java.util.Arrays.asList("T0", null)), Map.of()))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("dependencies entry");
        assertThatThrownBy(() -> new SupervisorSubTask("T1", "skill", List.of("T0", "T0"), Map.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("dependencies")
            .hasMessageContaining("T0");
        assertThatThrownBy(() -> new SupervisorPlan("CRM bootstrap", new ArrayList<>(java.util.Collections.singletonList(null))))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("tasks entry");
        assertThatThrownBy(() -> new SupervisorPlan(
            "CRM bootstrap",
            List.of(
                new SupervisorSubTask("T1", "skill-a", List.of(), Map.of()),
                new SupervisorSubTask("T1", "skill-b", List.of(), Map.of())
            )
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tasks.taskId")
            .hasMessageContaining("T1");
        assertThatThrownBy(() -> new SupervisorPlan(
            "CRM bootstrap",
            List.of(new SupervisorSubTask("T1", "skill", List.of("T1"), Map.of()))
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("depend on itself");
        assertThatThrownBy(() -> new SupervisorPlan(
            "CRM bootstrap",
            List.of(new SupervisorSubTask("T1", "skill", List.of("T9"), Map.of()))
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("known taskId")
            .hasMessageContaining("T9");
        assertThatThrownBy(() -> new SupervisorRequest(null, userId, "prompt", "REQ-1"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("tenantId");
        assertThatThrownBy(() -> new SupervisorRequest(tenantId, null, "prompt", "REQ-1"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("userId");
        assertThatThrownBy(() -> new SupervisorRequest(tenantId, userId, " ", "REQ-1"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("prompt");
        assertThatThrownBy(() -> new SupervisorRequest(tenantId, userId, "prompt", " "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("requestId");
        assertThatThrownBy(() -> new RollbackEntry(UUID.randomUUID(), "customerForm", 0, "T1"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("targetVersion");
        assertThatThrownBy(() -> new SupervisorResult(
            true,
            "REQ-1",
            "CRM bootstrap",
            null,
            Map.of(),
            List.of(),
            List.of(),
            null
        )).isInstanceOf(NullPointerException.class)
            .hasMessageContaining("completedAt");
        assertThatThrownBy(() -> new SupervisorResult(
            true,
            "REQ-1",
            "CRM bootstrap",
            Instant.parse("2026-01-01T00:00:03Z"),
            Map.of(),
            List.of(),
            List.of(),
            "boom"
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("success is true");
        assertThatThrownBy(() -> new SupervisorResult(
            false,
            "REQ-1",
            "CRM bootstrap",
            Instant.parse("2026-01-01T00:00:03Z"),
            Map.of(),
            List.of(),
            List.of(),
            " "
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("success is false");
        assertThatThrownBy(() -> new SupervisorResult.TaskResult(true, Map.of(), "boom"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("success is true");
        assertThatThrownBy(() -> new SupervisorResult.TaskResult(false, Map.of(), " "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("success is false");
        assertThatThrownBy(() -> new SupervisorResult(
            true,
            "REQ-1",
            "CRM bootstrap",
            Instant.parse("2026-01-01T00:00:03Z"),
            taskResultsWithNullKey,
            List.of(),
            List.of(),
            null
        )).isInstanceOf(NullPointerException.class)
            .hasMessageContaining("taskResultsByTaskId key");
        assertThatThrownBy(() -> new SupervisorResult(
            true,
            "REQ-1",
            "CRM bootstrap",
            Instant.parse("2026-01-01T00:00:03Z"),
            taskResultsWithBlankKey,
            List.of(),
            List.of(),
            null
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("taskResultsByTaskId key");
        assertThatThrownBy(() -> new SupervisorResult(
            true,
            "REQ-1",
            "CRM bootstrap",
            Instant.parse("2026-01-01T00:00:03Z"),
            taskResultsWithNullValue,
            List.of(),
            List.of(),
            null
        )).isInstanceOf(NullPointerException.class)
            .hasMessageContaining("taskResultsByTaskId value");
    }

    @Test
    void shouldCanonicalizeNestedSetOrderForImmutablePayloadCopies() {
        var orderedNestedSet = new LinkedHashSet<String>();
        orderedNestedSet.add("rollback");
        orderedNestedSet.add("cleanup");

        var orderedParameters = new LinkedHashMap<String, Object>();
        orderedParameters.put("first", 1);
        orderedParameters.put("second", 2);
        orderedParameters.put("flags", orderedNestedSet);

        var task = new SupervisorSubTask("T1", "jeecg-onlform", List.of(), orderedParameters);

        assertThat(task.parameters().keySet()).containsExactly("first", "flags", "second");
        assertThat(asStringSet(task.parameters().get("flags"))).containsExactly("cleanup", "rollback");
        assertThatThrownBy(() -> asStringSet(task.parameters().get("flags")).add("archive"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldRejectNestedNonStringMapKeys() {
        var nestedPayload = new LinkedHashMap<Object, Object>();
        nestedPayload.put(1, "invalid");

        var parameters = new LinkedHashMap<String, Object>();
        parameters.put("config", nestedPayload);

        assertThatThrownBy(() -> new SupervisorSubTask("T1", "jeecg-onlform", List.of(), parameters))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("map key")
            .hasMessageContaining("String");
    }

    @Test
    void shouldRejectNestedNullMapValues() {
        var nestedPayload = new LinkedHashMap<String, Object>();
        nestedPayload.put("valid", null);

        var parameters = new LinkedHashMap<String, Object>();
        parameters.put("config", nestedPayload);

        assertThatThrownBy(() -> new SupervisorSubTask("T1", "jeecg-onlform", List.of(), parameters))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("map value");
    }

    @Test
    void shouldCanonicalizeNestedMapOrderIndependentlyOfInsertionOrder() {
        var firstNested = new LinkedHashMap<String, Object>();
        firstNested.put("zeta", 1);
        firstNested.put("alpha", 2);
        firstNested.put("middle", 3);

        var secondNested = new LinkedHashMap<String, Object>();
        secondNested.put("middle", 3);
        secondNested.put("zeta", 1);
        secondNested.put("alpha", 2);

        var firstTask = new SupervisorSubTask(
            "T1",
            "jeecg-onlform",
            List.of(),
            Map.of("config", firstNested)
        );
        var secondTask = new SupervisorSubTask(
            "T2",
            "jeecg-onlform",
            List.of(),
            Map.of("config", secondNested)
        );

        var firstConfig = asStringObjectMap(firstTask.parameters().get("config"));
        var secondConfig = asStringObjectMap(secondTask.parameters().get("config"));

        assertThat(firstConfig.keySet()).containsExactly("alpha", "middle", "zeta");
        assertThat(secondConfig.keySet()).containsExactly("alpha", "middle", "zeta");
        assertThat(firstConfig).containsExactlyEntriesOf(secondConfig);
    }

    @Test
    void shouldRejectMutableLeafPayloadValues() {
        var stringBuilderParameters = new LinkedHashMap<String, Object>();
        stringBuilderParameters.put("description", new StringBuilder("mutable"));

        var byteArrayParameters = new LinkedHashMap<String, Object>();
        byteArrayParameters.put("blob", new byte[] {1, 2, 3});

        var atomicIntegerParameters = new LinkedHashMap<String, Object>();
        atomicIntegerParameters.put("counter", new AtomicInteger(1));

        assertThatThrownBy(() -> new SupervisorSubTask("T1", "jeecg-onlform", List.of(), stringBuilderParameters))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unsupported payload leaf type")
            .hasMessageContaining(StringBuilder.class.getName());
        assertThatThrownBy(() -> new SupervisorSubTask("T1", "jeecg-onlform", List.of(), byteArrayParameters))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unsupported payload leaf type")
            .hasMessageContaining(byte[].class.getName());
        assertThatThrownBy(() -> new SupervisorSubTask("T1", "jeecg-onlform", List.of(), atomicIntegerParameters))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unsupported payload leaf type")
            .hasMessageContaining(AtomicInteger.class.getName());
    }

    @Test
    void shouldCanonicalizeTopLevelPayloadOrderIndependentlyOfInsertionOrder() {
        var firstParameters = new LinkedHashMap<String, Object>();
        firstParameters.put("zeta", 1);
        firstParameters.put("alpha", 2);
        firstParameters.put("middle", 3);

        var secondParameters = new LinkedHashMap<String, Object>();
        secondParameters.put("middle", 3);
        secondParameters.put("zeta", 1);
        secondParameters.put("alpha", 2);

        var firstTask = new SupervisorSubTask("T1", "jeecg-onlform", List.of(), firstParameters);
        var secondTask = new SupervisorSubTask("T2", "jeecg-onlform", List.of(), secondParameters);

        assertThat(firstTask.parameters().keySet()).containsExactly("alpha", "middle", "zeta");
        assertThat(secondTask.parameters().keySet()).containsExactly("alpha", "middle", "zeta");
        assertThat(firstTask.parameters()).containsExactlyEntriesOf(secondTask.parameters());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asStringObjectMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<String> asStringList(Object value) {
        return (List<String>) value;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> asStringSet(Object value) {
        return (Set<String>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> asObjectMapList(Object value) {
        return (List<Map<String, Object>>) value;
    }

    @SuppressWarnings("unchecked")
    private static Set<Object> asObjectSet(Object value) {
        return (Set<Object>) value;
    }
}
