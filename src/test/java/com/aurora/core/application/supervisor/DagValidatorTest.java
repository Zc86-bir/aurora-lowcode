package com.aurora.core.application.supervisor;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DagValidatorTest {

    private final DagValidator dagValidator = new DagValidator();

    @Test
    void shouldRejectMissingDependency() {
        var tasks = List.of(new SupervisorSubTask("T1", "jeecg-onlform", List.of("T404"), Map.of()));

        assertThatThrownBy(() -> dagValidator.validate(tasks))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Missing dependency");
    }

    @Test
    void shouldRejectDuplicateTaskIds() {
        var tasks = List.of(
            new SupervisorSubTask("T1", "jeecg-onlform", List.of(), Map.of()),
            new SupervisorSubTask("T1", "jeecg-bpmn", List.of(), Map.of())
        );

        assertThatThrownBy(() -> dagValidator.validate(tasks))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Duplicate taskId");
    }

    @Test
    void shouldRejectSelfDependency() {
        var tasks = List.of(new SupervisorSubTask("T1", "jeecg-onlform", List.of("T1"), Map.of()));

        assertThatThrownBy(() -> dagValidator.validate(tasks))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("itself");
    }

    @Test
    void shouldRejectDuplicateDependenciesWithinTask() {
        assertThatThrownBy(() -> List.of(
                new SupervisorSubTask("T1", "jeecg-onlform", List.of(), Map.of()),
                new SupervisorSubTask("T2", "jeecg-bpmn", List.of("T1", "T1"), Map.of())
            ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("dependencies must not contain duplicates")
            .hasMessageContaining("T1");
    }

    @Test
    void shouldRejectCycleAfterProcessingIndependentPrefixAndReportBlockedTasks() {
        var tasks = List.of(
            new SupervisorSubTask("T1", "jeecg-system", List.of(), Map.of()),
            new SupervisorSubTask("T2", "jeecg-onlform", List.of("T1"), Map.of()),
            new SupervisorSubTask("T3", "jeecg-bpmn", List.of("T4"), Map.of()),
            new SupervisorSubTask("T4", "jeecg-codegen", List.of("T3"), Map.of()),
            new SupervisorSubTask("T5", "jeecg-desform", List.of("T2", "T3"), Map.of())
        );

        assertThatThrownBy(() -> dagValidator.validate(tasks))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cycle")
            .hasMessageContaining("blocked tasks")
            .hasMessageContaining("T3")
            .hasMessageContaining("T4")
            .hasMessageContaining("T5")
            .hasMessageNotContaining("T1")
            .hasMessageNotContaining("T2");
    }

    @Test
    void shouldReturnStableTopologicalOrderAndDependencyDepthForValidDag() {
        var tasks = List.of(
            new SupervisorSubTask("T1", "jeecg-system", List.of(), Map.of()),
            new SupervisorSubTask("T2", "jeecg-onlform", List.of("T1"), Map.of()),
            new SupervisorSubTask("T3", "jeecg-bpmn", List.of("T1"), Map.of()),
            new SupervisorSubTask("T4", "jeecg-codegen", List.of("T2", "T3"), Map.of())
        );

        var result = dagValidator.validate(tasks);

        assertThat(result.stableTopologicalOrder()).containsExactly("T1", "T2", "T3", "T4");
        assertThat(result.dependencyDepthByTaskId())
            .containsEntry("T1", 0)
            .containsEntry("T2", 1)
            .containsEntry("T3", 1)
            .containsEntry("T4", 2);
    }

    @Test
    void shouldProduceCanonicalOrderForEquivalentDagsRegardlessOfValidInputOrder() {
        var tasksInOneValidOrder = List.of(
            new SupervisorSubTask("T1", "jeecg-system", List.of(), Map.of()),
            new SupervisorSubTask("T3", "jeecg-bpmn", List.of("T1"), Map.of()),
            new SupervisorSubTask("T2", "jeecg-onlform", List.of("T1"), Map.of()),
            new SupervisorSubTask("T4", "jeecg-codegen", List.of("T2", "T3"), Map.of())
        );
        var tasksInAnotherValidOrder = List.of(
            new SupervisorSubTask("T4", "jeecg-codegen", List.of("T3", "T2"), Map.of()),
            new SupervisorSubTask("T2", "jeecg-onlform", List.of("T1"), Map.of()),
            new SupervisorSubTask("T1", "jeecg-system", List.of(), Map.of()),
            new SupervisorSubTask("T3", "jeecg-bpmn", List.of("T1"), Map.of())
        );

        var firstResult = dagValidator.validate(tasksInOneValidOrder);
        var secondResult = dagValidator.validate(tasksInAnotherValidOrder);

        assertThat(firstResult.stableTopologicalOrder()).containsExactly("T1", "T2", "T3", "T4");
        assertThat(secondResult.stableTopologicalOrder()).containsExactly("T1", "T2", "T3", "T4");
        assertThat(firstResult.stableTopologicalOrder()).isEqualTo(secondResult.stableTopologicalOrder());
        assertThat(firstResult.dependencyDepthByTaskId().keySet()).containsExactly("T1", "T2", "T3", "T4");
        assertThat(secondResult.dependencyDepthByTaskId().keySet()).containsExactly("T1", "T2", "T3", "T4");
        assertThat(firstResult.dependencyDepthByTaskId()).isEqualTo(secondResult.dependencyDepthByTaskId());
    }

    @Test
    void shouldExposeDependencyDepthAsUnmodifiableMapWithDeterministicIterationOrder() {
        var tasks = List.of(
            new SupervisorSubTask("T1", "jeecg-system", List.of(), Map.of()),
            new SupervisorSubTask("T3", "jeecg-bpmn", List.of("T1"), Map.of()),
            new SupervisorSubTask("T2", "jeecg-onlform", List.of("T1"), Map.of()),
            new SupervisorSubTask("T4", "jeecg-codegen", List.of("T2", "T3"), Map.of())
        );

        var dependencyDepthByTaskId = dagValidator.validate(tasks).dependencyDepthByTaskId();

        assertThat(dependencyDepthByTaskId.keySet()).containsExactly("T1", "T2", "T3", "T4");
        assertThatThrownBy(() -> dependencyDepthByTaskId.put("TX", 99))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldExposeCanonicalOrderWhenInputOrderDiffersFromTopologicalOrder() {
        var tasks = List.of(
            new SupervisorSubTask("T4", "jeecg-codegen", List.of("T2", "T3"), Map.of()),
            new SupervisorSubTask("T2", "jeecg-onlform", List.of("T1"), Map.of()),
            new SupervisorSubTask("T3", "jeecg-bpmn", List.of("T1"), Map.of()),
            new SupervisorSubTask("T1", "jeecg-system", List.of(), Map.of())
        );

        var result = dagValidator.validate(tasks);

        assertThat(result.stableTopologicalOrder()).containsExactly("T1", "T2", "T3", "T4");
        assertThat(result.dependencyDepthByTaskId().keySet()).containsExactly("T1", "T2", "T3", "T4");
        assertThat(result.dependencyDepthByTaskId()).containsExactly(
            Map.entry("T1", 0),
            Map.entry("T2", 1),
            Map.entry("T3", 1),
            Map.entry("T4", 2)
        );
    }
}
