package com.aurora.core.application.supervisor;

import com.aurora.core.contract.SkillExecutor;
import com.aurora.core.runtime.MetadataHotReloadManager;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultSupervisorOrchestratorTest {

    @Test
    void shouldExecuteIndependentTasksAndThenDependentTask() {
        SkillExecutor skillExecutor = mock(SkillExecutor.class);
        MetadataHotReloadManager reloadManager = mock(MetadataHotReloadManager.class);
        List<String> executionOrder = new CopyOnWriteArrayList<>();

        when(skillExecutor.execute(any())).thenAnswer(invocation -> {
            SkillExecutor.SkillRequest request = invocation.getArgument(0);
            executionOrder.add(request.context().metadata().get("supervisorTaskId"));
            return new SkillExecutor.SkillResult.Success(
                request.skillId(),
                request.context().requestId(),
                Map.of("metadataName", request.skillId(), "version", 1),
                Instant.now(),
                Duration.ofMillis(10),
                Map.of()
            );
        });

        SupervisorPlan plan = new SupervisorPlan("CRM", List.of(
            new SupervisorSubTask("T1", "jeecg-onlform", List.of(), Map.of()),
            new SupervisorSubTask("T2", "jeecg-bpmn", List.of(), Map.of()),
            new SupervisorSubTask("T3", "jeecg-onlreport", List.of("T1", "T2"), Map.of())
        ));

        var orchestrator = new DefaultSupervisorOrchestrator(skillExecutor, reloadManager, new DagValidator(), new SupervisorPromptFactory()) {
            @Override
            public SupervisorPlan plan(SupervisorRequest request) {
                return plan;
            }
        };

        SupervisorResult result = orchestrator.execute(
            new SupervisorRequest(UUID.randomUUID(), UUID.randomUUID(), "Build CRM", "req-1")
        );

        assertThat(result.success()).isTrue();
        assertThat(result.errorMessage()).isNull();
        assertThat(result.taskResultsByTaskId().keySet()).containsExactly("T1", "T2", "T3");
        assertThat(executionOrder).containsExactlyInAnyOrder("T1", "T2", "T3");
        assertThat(executionOrder.indexOf("T3")).isGreaterThan(executionOrder.indexOf("T1"));
        assertThat(executionOrder.indexOf("T3")).isGreaterThan(executionOrder.indexOf("T2"));
        verify(skillExecutor, times(3)).execute(any());
        verify(reloadManager, never()).rollback(any(), any(), any(Integer.class));
    }

    @Test
    void shouldRollbackCompletedTasksWhenLaterTaskFails() {
        SkillExecutor skillExecutor = mock(SkillExecutor.class);
        MetadataHotReloadManager reloadManager = mock(MetadataHotReloadManager.class);

        when(skillExecutor.execute(any()))
            .thenReturn(new SkillExecutor.SkillResult.Success(
                "jeecg-onlform",
                "req-2",
                Map.of("metadataName", "customerForm", "version", 2),
                Instant.now(),
                Duration.ofMillis(5),
                Map.of()
            ))
            .thenReturn(new SkillExecutor.SkillResult.Failure(
                "jeecg-bpmn",
                "req-2",
                "EXECUTION_FAILED",
                "boom",
                List.of(),
                Instant.now(),
                Duration.ofMillis(5)
            ));

        when(reloadManager.rollback(any(), eq("customerForm"), eq(2)))
            .thenReturn(new MetadataHotReloadManager.RollbackResult(true, "customerForm", 2, null, Duration.ofMillis(5)));

        SupervisorPlan plan = new SupervisorPlan("CRM", List.of(
            new SupervisorSubTask("T1", "jeecg-onlform", List.of(), Map.of()),
            new SupervisorSubTask("T2", "jeecg-bpmn", List.of("T1"), Map.of())
        ));

        var orchestrator = new DefaultSupervisorOrchestrator(skillExecutor, reloadManager, new DagValidator(), new SupervisorPromptFactory()) {
            @Override
            public SupervisorPlan plan(SupervisorRequest request) {
                return plan;
            }
        };

        SupervisorResult result = orchestrator.execute(
            new SupervisorRequest(UUID.randomUUID(), UUID.randomUUID(), "Build CRM", "req-2")
        );

        assertThat(result.success()).isFalse();
        assertThat(result.taskResultsByTaskId()).containsOnlyKeys("T1", "T2");
        assertThat(result.rollbackEntries()).singleElement().extracting(RollbackEntry::taskId).isEqualTo("T1");
        assertThat(result.rollbackAuditMessages()).containsExactly("T1:true");
        verify(reloadManager).rollback(any(), eq("customerForm"), eq(2));
    }

    @Test
    void shouldCaptureRollbackForFallbackAppliedTaskWhenLaterTaskFails() {
        SkillExecutor skillExecutor = mock(SkillExecutor.class);
        MetadataHotReloadManager reloadManager = mock(MetadataHotReloadManager.class);

        when(skillExecutor.execute(any()))
            .thenReturn(new SkillExecutor.SkillResult.FallbackApplied(
                "jeecg-onlform",
                "req-2b",
                Map.of("metadataName", "customerFormFallback", "version", 4),
                "primary failed",
                "CACHE",
                Instant.now(),
                Duration.ofMillis(5)
            ))
            .thenReturn(new SkillExecutor.SkillResult.Failure(
                "jeecg-bpmn",
                "req-2b",
                "EXECUTION_FAILED",
                "boom",
                List.of(),
                Instant.now(),
                Duration.ofMillis(5)
            ));

        when(reloadManager.rollback(any(), eq("customerFormFallback"), eq(4)))
            .thenReturn(new MetadataHotReloadManager.RollbackResult(true, "customerFormFallback", 4, null, Duration.ofMillis(5)));

        SupervisorPlan plan = new SupervisorPlan("CRM", List.of(
            new SupervisorSubTask("T1", "jeecg-onlform", List.of(), Map.of()),
            new SupervisorSubTask("T2", "jeecg-bpmn", List.of("T1"), Map.of())
        ));

        var orchestrator = new DefaultSupervisorOrchestrator(skillExecutor, reloadManager, new DagValidator(), new SupervisorPromptFactory()) {
            @Override
            public SupervisorPlan plan(SupervisorRequest request) {
                return plan;
            }
        };

        SupervisorResult result = orchestrator.execute(
            new SupervisorRequest(UUID.randomUUID(), UUID.randomUUID(), "Build CRM", "req-2b")
        );

        assertThat(result.success()).isFalse();
        assertThat(result.taskResultsByTaskId()).containsOnlyKeys("T1", "T2");
        assertThat(result.taskResultsByTaskId().get("T1").success()).isTrue();
        assertThat(result.rollbackEntries()).singleElement().extracting(RollbackEntry::taskId).isEqualTo("T1");
        assertThat(result.rollbackAuditMessages()).containsExactly("T1:true");
        verify(reloadManager).rollback(any(), eq("customerFormFallback"), eq(4));
    }

    @Test
    void shouldRollbackOnlyCompletedTasksInReverseOrderAndStopFurtherBatches() {
        SkillExecutor skillExecutor = mock(SkillExecutor.class);
        MetadataHotReloadManager reloadManager = mock(MetadataHotReloadManager.class);
        List<String> executedTaskIds = new ArrayList<>();

        when(skillExecutor.execute(any())).thenAnswer(invocation -> {
            SkillExecutor.SkillRequest request = invocation.getArgument(0);
            String taskId = request.context().metadata().get("supervisorTaskId");
            executedTaskIds.add(taskId);
            return switch (taskId) {
                case "T1" -> new SkillExecutor.SkillResult.Success(
                    request.skillId(),
                    request.context().requestId(),
                    Map.of("metadataName", "firstMeta", "version", 1),
                    Instant.now(),
                    Duration.ofMillis(5),
                    Map.of()
                );
                case "T2" -> new SkillExecutor.SkillResult.Success(
                    request.skillId(),
                    request.context().requestId(),
                    Map.of("metadataName", "secondMeta", "version", 2),
                    Instant.now(),
                    Duration.ofMillis(5),
                    Map.of()
                );
                case "T3" -> new SkillExecutor.SkillResult.Failure(
                    request.skillId(),
                    request.context().requestId(),
                    "EXECUTION_FAILED",
                    "batch failed",
                    List.of(),
                    Instant.now(),
                    Duration.ofMillis(5)
                );
                default -> throw new IllegalStateException("Unexpected task " + taskId);
            };
        });

        when(reloadManager.rollback(any(), eq("secondMeta"), eq(2)))
            .thenReturn(new MetadataHotReloadManager.RollbackResult(true, "secondMeta", 2, null, Duration.ofMillis(5)));
        when(reloadManager.rollback(any(), eq("firstMeta"), eq(1)))
            .thenReturn(new MetadataHotReloadManager.RollbackResult(true, "firstMeta", 1, null, Duration.ofMillis(5)));

        SupervisorPlan plan = new SupervisorPlan("CRM", List.of(
            new SupervisorSubTask("T1", "jeecg-onlform", List.of(), Map.of()),
            new SupervisorSubTask("T2", "jeecg-bpmn", List.of("T1"), Map.of()),
            new SupervisorSubTask("T3", "jeecg-onlreport", List.of("T2"), Map.of()),
            new SupervisorSubTask("T4", "jeecg-codegen", List.of("T3"), Map.of())
        ));

        var orchestrator = new DefaultSupervisorOrchestrator(skillExecutor, reloadManager, new DagValidator(), new SupervisorPromptFactory()) {
            @Override
            public SupervisorPlan plan(SupervisorRequest request) {
                return plan;
            }
        };

        SupervisorResult result = orchestrator.execute(
            new SupervisorRequest(UUID.randomUUID(), UUID.randomUUID(), "Build CRM", "req-3")
        );

        assertThat(result.success()).isFalse();
        assertThat(executedTaskIds).containsExactly("T1", "T2", "T3");
        assertThat(result.taskResultsByTaskId()).containsOnlyKeys("T1", "T2", "T3");
        assertThat(result.rollbackEntries()).extracting(RollbackEntry::taskId).containsExactly("T1", "T2");
        assertThat(result.rollbackAuditMessages()).containsExactly("T2:true", "T1:true");
        verify(skillExecutor, never()).execute(argThat(request ->
            "T4".equals(request.context().metadata().get("supervisorTaskId"))
        ));

        var inOrder = inOrder(reloadManager);
        inOrder.verify(reloadManager).rollback(any(), eq("secondMeta"), eq(2));
        inOrder.verify(reloadManager).rollback(any(), eq("firstMeta"), eq(1));
    }

    @Test
    void shouldCaptureSuccessfulSiblingBeforeSameBatchFailureRollback() {
        SkillExecutor skillExecutor = mock(SkillExecutor.class);
        MetadataHotReloadManager reloadManager = mock(MetadataHotReloadManager.class);
        CountDownLatch successFinished = new CountDownLatch(1);

        when(skillExecutor.execute(any())).thenAnswer(invocation -> {
            SkillExecutor.SkillRequest request = invocation.getArgument(0);
            String taskId = request.context().metadata().get("supervisorTaskId");
            if ("T1".equals(taskId)) {
                successFinished.countDown();
                return new SkillExecutor.SkillResult.Success(
                    request.skillId(),
                    request.context().requestId(),
                    Map.of("metadataName", "siblingMeta", "version", 7),
                    Instant.now(),
                    Duration.ofMillis(5),
                    Map.of()
                );
            }
            assertThat(successFinished.await(1, TimeUnit.SECONDS)).isTrue();
            return new SkillExecutor.SkillResult.Failure(
                request.skillId(),
                request.context().requestId(),
                "EXECUTION_FAILED",
                "sibling failed",
                List.of(),
                Instant.now(),
                Duration.ofMillis(5)
            );
        });

        when(reloadManager.rollback(any(), eq("siblingMeta"), eq(7)))
            .thenReturn(new MetadataHotReloadManager.RollbackResult(true, "siblingMeta", 7, null, Duration.ofMillis(5)));

        SupervisorPlan plan = new SupervisorPlan("CRM", List.of(
            new SupervisorSubTask("T1", "jeecg-onlform", List.of(), Map.of()),
            new SupervisorSubTask("T2", "jeecg-bpmn", List.of(), Map.of())
        ));

        var orchestrator = new DefaultSupervisorOrchestrator(skillExecutor, reloadManager, new DagValidator(), new SupervisorPromptFactory()) {
            @Override
            public SupervisorPlan plan(SupervisorRequest request) {
                return plan;
            }
        };

        SupervisorResult result = orchestrator.execute(
            new SupervisorRequest(UUID.randomUUID(), UUID.randomUUID(), "Build CRM", "req-4")
        );

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("sibling failed");
        assertThat(result.taskResultsByTaskId()).containsOnlyKeys("T1", "T2");
        assertThat(result.taskResultsByTaskId().get("T1").success()).isTrue();
        assertThat(result.taskResultsByTaskId().get("T2").success()).isFalse();
        assertThat(result.rollbackEntries()).singleElement().extracting(RollbackEntry::taskId).isEqualTo("T1");
        assertThat(result.rollbackAuditMessages()).containsExactly("T1:true");
        verify(reloadManager).rollback(any(), eq("siblingMeta"), eq(7));
    }

    @Test
    void shouldRollbackCompletedSiblingWhosePublicationIsDelayedUntilAfterFailureRecorded() throws Exception {
        SkillExecutor skillExecutor = mock(SkillExecutor.class);
        MetadataHotReloadManager reloadManager = mock(MetadataHotReloadManager.class);
        CountDownLatch successFinished = new CountDownLatch(1);
        CountDownLatch successPublicationBlocked = new CountDownLatch(1);
        CountDownLatch allowSuccessPublication = new CountDownLatch(1);
        AtomicReference<SupervisorResult> resultRef = new AtomicReference<>();

        when(skillExecutor.execute(any())).thenAnswer(invocation -> {
            SkillExecutor.SkillRequest request = invocation.getArgument(0);
            String taskId = request.context().metadata().get("supervisorTaskId");
            if ("T1".equals(taskId)) {
                successFinished.countDown();
                return new SkillExecutor.SkillResult.Success(
                    request.skillId(),
                    request.context().requestId(),
                    Map.of("metadataName", "delayedSiblingMeta", "version", 13),
                    Instant.now(),
                    Duration.ofMillis(5),
                    Map.of()
                );
            }
            assertThat(successFinished.await(1, TimeUnit.SECONDS)).isTrue();
            return new SkillExecutor.SkillResult.Failure(
                request.skillId(),
                request.context().requestId(),
                "EXECUTION_FAILED",
                "sibling failed after completion",
                List.of(),
                Instant.now(),
                Duration.ofMillis(5)
            );
        });

        when(reloadManager.rollback(any(), eq("delayedSiblingMeta"), eq(13)))
            .thenReturn(new MetadataHotReloadManager.RollbackResult(true, "delayedSiblingMeta", 13, null, Duration.ofMillis(5)));

        SupervisorPlan plan = new SupervisorPlan("CRM", List.of(
            new SupervisorSubTask("T1", "jeecg-onlform", List.of(), Map.of()),
            new SupervisorSubTask("T2", "jeecg-bpmn", List.of(), Map.of())
        ));

        var orchestrator = new DefaultSupervisorOrchestrator(skillExecutor, reloadManager, new DagValidator(), new SupervisorPromptFactory()) {
            @Override
            public SupervisorPlan plan(SupervisorRequest request) {
                return plan;
            }

            @Override
            void afterTaskSuccess(TaskExecutionOutcome outcome) {
                if ("T1".equals(outcome.taskId())) {
                    successPublicationBlocked.countDown();
                    try {
                        assertThat(allowSuccessPublication.await(1, TimeUnit.SECONDS)).isTrue();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(e);
                    }
                }
            }
        };

        Thread executionThread = Thread.ofVirtual().start(() -> resultRef.set(orchestrator.execute(
            new SupervisorRequest(UUID.randomUUID(), UUID.randomUUID(), "Build CRM", "req-4b")
        )));

        assertThat(successFinished.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(successPublicationBlocked.await(1, TimeUnit.SECONDS)).isTrue();
        allowSuccessPublication.countDown();
        executionThread.join(1000);

        assertThat(executionThread.isAlive()).isFalse();
        SupervisorResult result = resultRef.get();

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("sibling failed after completion");
        assertThat(result.taskResultsByTaskId()).containsOnlyKeys("T1", "T2");
        assertThat(result.taskResultsByTaskId().get("T1").success()).isTrue();
        assertThat(result.taskResultsByTaskId().get("T2").success()).isFalse();
        assertThat(result.rollbackEntries()).singleElement().extracting(RollbackEntry::taskId).isEqualTo("T1");
        assertThat(result.rollbackAuditMessages()).containsExactly("T1:true");
        verify(reloadManager).rollback(any(), eq("delayedSiblingMeta"), eq(13));
    }

    @Test
    void shouldFailFastWithoutWaitingForBlockedSiblingInSameBatch() throws Exception {
        SkillExecutor skillExecutor = mock(SkillExecutor.class);
        MetadataHotReloadManager reloadManager = mock(MetadataHotReloadManager.class);
        CountDownLatch slowTaskStarted = new CountDownLatch(1);
        CountDownLatch slowTaskInterrupted = new CountDownLatch(1);
        AtomicBoolean slowTaskFinished = new AtomicBoolean(false);
        AtomicReference<SupervisorResult> resultRef = new AtomicReference<>();

        when(skillExecutor.execute(any())).thenAnswer(invocation -> {
            SkillExecutor.SkillRequest request = invocation.getArgument(0);
            String taskId = request.context().metadata().get("supervisorTaskId");
            if ("T1".equals(taskId)) {
                return new SkillExecutor.SkillResult.Failure(
                    request.skillId(),
                    request.context().requestId(),
                    "EXECUTION_FAILED",
                    "fast failure",
                    List.of(),
                    Instant.now(),
                    Duration.ofMillis(5)
                );
            }
            slowTaskStarted.countDown();
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException e) {
                slowTaskInterrupted.countDown();
                Thread.currentThread().interrupt();
                throw e;
            }
            slowTaskFinished.set(true);
            return new SkillExecutor.SkillResult.Success(
                request.skillId(),
                request.context().requestId(),
                Map.of("metadataName", "lateMeta", "version", 10),
                Instant.now(),
                Duration.ofMillis(250),
                Map.of()
            );
        });

        SupervisorPlan plan = new SupervisorPlan("CRM", List.of(
            new SupervisorSubTask("T1", "jeecg-onlform", List.of(), Map.of()),
            new SupervisorSubTask("T2", "jeecg-bpmn", List.of(), Map.of())
        ));

        var orchestrator = new DefaultSupervisorOrchestrator(
            skillExecutor,
            reloadManager,
            new DagValidator(),
            new SupervisorPromptFactory(),
            Duration.ofSeconds(5)
        ) {
            @Override
            public SupervisorPlan plan(SupervisorRequest request) {
                return plan;
            }
        };

        Thread executionThread = Thread.ofVirtual().start(() -> resultRef.set(orchestrator.execute(
            new SupervisorRequest(UUID.randomUUID(), UUID.randomUUID(), "Build CRM", "req-5")
        )));

        assertThat(slowTaskStarted.await(1, TimeUnit.SECONDS)).isTrue();

        long startedAt = System.nanoTime();
        executionThread.join(200);
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

        assertThat(executionThread.isAlive()).isFalse();
        SupervisorResult result = resultRef.get();

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("fast failure");
        assertThat(result.taskResultsByTaskId()).containsOnlyKeys("T1");
        assertThat(result.taskResultsByTaskId().get("T1").success()).isFalse();
        assertThat(result.taskResultsByTaskId().get("T1").errorMessage()).isEqualTo("fast failure");
        assertThat(result.rollbackEntries()).isEmpty();
        assertThat(result.rollbackAuditMessages()).isEmpty();
        assertThat(elapsedMillis).isLessThan(200);
        assertThat(slowTaskFinished).isFalse();
        assertThat(slowTaskInterrupted.await(1, TimeUnit.SECONDS)).isTrue();
        verify(reloadManager, never()).rollback(any(), any(), any(Integer.class));
    }

    @Test
    void shouldNotCaptureRollbackForSiblingThatOnlyFinishesAfterFailure() throws Exception {
        SkillExecutor skillExecutor = mock(SkillExecutor.class);
        MetadataHotReloadManager reloadManager = mock(MetadataHotReloadManager.class);
        CountDownLatch slowTaskStarted = new CountDownLatch(1);
        AtomicBoolean slowTaskCompleted = new AtomicBoolean(false);
        AtomicReference<SupervisorResult> resultRef = new AtomicReference<>();

        when(skillExecutor.execute(any())).thenAnswer(invocation -> {
            SkillExecutor.SkillRequest request = invocation.getArgument(0);
            String taskId = request.context().metadata().get("supervisorTaskId");
            if ("T1".equals(taskId)) {
                return new SkillExecutor.SkillResult.Failure(
                    request.skillId(),
                    request.context().requestId(),
                    "EXECUTION_FAILED",
                    "first failed",
                    List.of(),
                    Instant.now(),
                    Duration.ofMillis(5)
                );
            }
            slowTaskStarted.countDown();
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            }
            slowTaskCompleted.set(true);
            return new SkillExecutor.SkillResult.Success(
                request.skillId(),
                request.context().requestId(),
                Map.of("metadataName", "lateMeta", "version", 11),
                Instant.now(),
                Duration.ofMillis(250),
                Map.of()
            );
        });

        SupervisorPlan plan = new SupervisorPlan("CRM", List.of(
            new SupervisorSubTask("T1", "jeecg-onlform", List.of(), Map.of()),
            new SupervisorSubTask("T2", "jeecg-bpmn", List.of(), Map.of())
        ));

        var orchestrator = new DefaultSupervisorOrchestrator(skillExecutor, reloadManager, new DagValidator(), new SupervisorPromptFactory()) {
            @Override
            public SupervisorPlan plan(SupervisorRequest request) {
                return plan;
            }
        };

        Thread executionThread = Thread.ofVirtual().start(() -> resultRef.set(orchestrator.execute(
            new SupervisorRequest(UUID.randomUUID(), UUID.randomUUID(), "Build CRM", "req-6")
        )));

        assertThat(slowTaskStarted.await(1, TimeUnit.SECONDS)).isTrue();
        executionThread.join(200);

        assertThat(executionThread.isAlive()).isFalse();
        SupervisorResult result = resultRef.get();

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("first failed");
        assertThat(result.taskResultsByTaskId()).containsOnlyKeys("T1");
        assertThat(result.taskResultsByTaskId().get("T1").success()).isFalse();
        assertThat(result.rollbackEntries()).isEmpty();
        assertThat(result.rollbackAuditMessages()).isEmpty();
        assertThat(slowTaskCompleted).isFalse();
        verify(reloadManager, never()).rollback(any(), any(), any(Integer.class));
    }

    @Test
    void shouldReturnPromptlyOnTimeoutWithoutAwaitingSlowSiblingCompletion() throws Exception {
        SkillExecutor skillExecutor = mock(SkillExecutor.class);
        MetadataHotReloadManager reloadManager = mock(MetadataHotReloadManager.class);
        CountDownLatch slowTaskStarted = new CountDownLatch(1);
        CountDownLatch executionFinished = new CountDownLatch(1);
        CountDownLatch slowTaskInterrupted = new CountDownLatch(1);
        AtomicBoolean slowTaskCompleted = new AtomicBoolean(false);
        AtomicReference<SupervisorResult> resultRef = new AtomicReference<>();

        when(skillExecutor.execute(any())).thenAnswer(invocation -> {
            SkillExecutor.SkillRequest request = invocation.getArgument(0);
            String taskId = request.context().metadata().get("supervisorTaskId");

            if ("T1".equals(taskId)) {
            return new SkillExecutor.SkillResult.Success(
                request.skillId(),
                request.context().requestId(),
                Map.of("metadataName", "timedOutBatchMeta", "version", 1),
                Instant.now(),
                Duration.ofMillis(5),
                Map.of()
            );
            }

            slowTaskStarted.countDown();
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException e) {
                slowTaskInterrupted.countDown();
                Thread.currentThread().interrupt();
                throw e;
            }

            slowTaskCompleted.set(true);
            return new SkillExecutor.SkillResult.Success(
                request.skillId(),
                request.context().requestId(),
                Map.of("metadataName", request.skillId(), "version", 2),
                Instant.now(),
                Duration.ofMillis(250),
                Map.of()
            );
        });

        when(reloadManager.rollback(any(), eq("timedOutBatchMeta"), eq(1)))
            .thenReturn(new MetadataHotReloadManager.RollbackResult(true, "timedOutBatchMeta", 1, null, Duration.ofMillis(5)));

        SupervisorPlan plan = new SupervisorPlan("CRM", List.of(
            new SupervisorSubTask("T1", "jeecg-onlform", List.of(), Map.of()),
            new SupervisorSubTask("T2", "jeecg-bpmn", List.of(), Map.of())
        ));

        var orchestrator = new DefaultSupervisorOrchestrator(
            skillExecutor,
            reloadManager,
            new DagValidator(),
            new SupervisorPromptFactory(),
            Duration.ofMillis(40)
        ) {
            @Override
            public SupervisorPlan plan(SupervisorRequest request) {
                return plan;
            }
        };

        Thread executionThread = Thread.ofVirtual().start(() -> {
            resultRef.set(orchestrator.execute(
                new SupervisorRequest(UUID.randomUUID(), UUID.randomUUID(), "Build CRM", "req-7")
            ));
            executionFinished.countDown();
        });

        assertThat(slowTaskStarted.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(executionFinished.await(1, TimeUnit.SECONDS)).isTrue();
        executionThread.join(1000);

        assertThat(executionThread.isAlive()).isFalse();
        SupervisorResult result = resultRef.get();

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("Supervisor batch timed out after PT0.04S");
        assertThat(result.taskResultsByTaskId()).containsOnlyKeys("T1", "T2");
        assertThat(result.taskResultsByTaskId().get("T1").success()).isTrue();
        assertThat(result.taskResultsByTaskId().get("T2").success()).isFalse();
        assertThat(result.taskResultsByTaskId().get("T2").errorMessage()).isEqualTo("Supervisor batch timed out after PT0.04S");
        assertThat(result.rollbackEntries()).singleElement().extracting(RollbackEntry::taskId).isEqualTo("T1");
        assertThat(result.rollbackAuditMessages()).containsExactly("T1:true");
        assertThat(slowTaskCompleted).isFalse();
        assertThat(slowTaskInterrupted.await(1, TimeUnit.SECONDS)).isTrue();
        verify(reloadManager).rollback(any(), eq("timedOutBatchMeta"), eq(1));
    }

    @Test
    void shouldUseFallbackBatchErrorAndRollbackSuccessfulSiblingWhenSameBatchThrowsNullMessage() {
        SkillExecutor skillExecutor = mock(SkillExecutor.class);
        MetadataHotReloadManager reloadManager = mock(MetadataHotReloadManager.class);

        when(skillExecutor.execute(any())).thenAnswer(invocation -> {
            SkillExecutor.SkillRequest request = invocation.getArgument(0);
            String taskId = request.context().metadata().get("supervisorTaskId");
            if ("T1".equals(taskId)) {
                return new SkillExecutor.SkillResult.Success(
                    request.skillId(),
                    request.context().requestId(),
                    Map.of("metadataName", "nullMessageMeta", "version", 11),
                    Instant.now(),
                    Duration.ofMillis(5),
                    Map.of()
                );
            }
            throw new IllegalStateException((String) null);
        });

        when(reloadManager.rollback(any(), eq("nullMessageMeta"), eq(11)))
            .thenReturn(new MetadataHotReloadManager.RollbackResult(true, "nullMessageMeta", 11, null, Duration.ofMillis(5)));

        SupervisorPlan plan = new SupervisorPlan("CRM", List.of(
            new SupervisorSubTask("T1", "jeecg-onlform", List.of(), Map.of()),
            new SupervisorSubTask("T2", "jeecg-bpmn", List.of(), Map.of())
        ));

        var orchestrator = new DefaultSupervisorOrchestrator(skillExecutor, reloadManager, new DagValidator(), new SupervisorPromptFactory()) {
            @Override
            public SupervisorPlan plan(SupervisorRequest request) {
                return plan;
            }
        };

        SupervisorResult result = orchestrator.execute(
            new SupervisorRequest(UUID.randomUUID(), UUID.randomUUID(), "Build CRM", "req-8")
        );

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("Supervisor batch task failed");
        assertThat(result.taskResultsByTaskId().keySet()).contains("T2");
        assertThat(result.taskResultsByTaskId().get("T2").success()).isFalse();
        assertThat(result.taskResultsByTaskId().get("T2").errorMessage()).isEqualTo("Supervisor batch task failed");
        if (result.taskResultsByTaskId().containsKey("T1")) {
            assertThat(result.taskResultsByTaskId().get("T1").success()).isTrue();
            assertThat(result.rollbackEntries()).singleElement().extracting(RollbackEntry::taskId).isEqualTo("T1");
            assertThat(result.rollbackAuditMessages()).containsExactly("T1:true");
            verify(reloadManager).rollback(any(), eq("nullMessageMeta"), eq(11));
        } else {
            assertThat(result.rollbackEntries()).isEmpty();
            assertThat(result.rollbackAuditMessages()).isEmpty();
            verify(reloadManager, never()).rollback(any(), any(), any(Integer.class));
        }
    }

    private static SkillExecutor.SkillRequest argThat(java.util.function.Predicate<SkillExecutor.SkillRequest> predicate) {
        return org.mockito.ArgumentMatchers.argThat(predicate::test);
    }
}
