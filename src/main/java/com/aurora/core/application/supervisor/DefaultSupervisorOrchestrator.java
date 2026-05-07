package com.aurora.core.application.supervisor;

import com.aurora.core.contract.SkillExecutor;
import com.aurora.core.runtime.MetadataHotReloadManager;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.locks.ReentrantLock;

public class DefaultSupervisorOrchestrator implements SupervisorOrchestrator {

    private static final Duration DEFAULT_TASK_TIMEOUT = Duration.ofMinutes(2);

    private final SkillExecutor skillExecutor;
    private final MetadataHotReloadManager hotReloadManager;
    private final DagValidator dagValidator;
    @SuppressWarnings("unused")
    private final SupervisorPromptFactory promptFactory;
    private final Duration taskTimeout;

    public DefaultSupervisorOrchestrator(
        SkillExecutor skillExecutor,
        MetadataHotReloadManager hotReloadManager,
        DagValidator dagValidator,
        SupervisorPromptFactory promptFactory
    ) {
        this(skillExecutor, hotReloadManager, dagValidator, promptFactory, DEFAULT_TASK_TIMEOUT);
    }

    DefaultSupervisorOrchestrator(
        SkillExecutor skillExecutor,
        MetadataHotReloadManager hotReloadManager,
        DagValidator dagValidator,
        SupervisorPromptFactory promptFactory,
        Duration taskTimeout
    ) {
        this.skillExecutor = ImmutablePayloads.requireNonNull(skillExecutor, "skillExecutor");
        this.hotReloadManager = ImmutablePayloads.requireNonNull(hotReloadManager, "hotReloadManager");
        this.dagValidator = ImmutablePayloads.requireNonNull(dagValidator, "dagValidator");
        this.promptFactory = ImmutablePayloads.requireNonNull(promptFactory, "promptFactory");
        this.taskTimeout = ImmutablePayloads.requireNonNull(taskTimeout, "taskTimeout");
    }

    @Override
    public SupervisorPlan plan(SupervisorRequest request) {
        throw new UnsupportedOperationException("Structured-output planner wiring is not available yet");
    }

    @Override
    public SupervisorResult execute(SupervisorRequest request) {
        SupervisorRequest validatedRequest = ImmutablePayloads.requireNonNull(request, "request");
        SupervisorPlan supervisorPlan = ImmutablePayloads.requireNonNull(plan(validatedRequest), "planned supervisorPlan");
        DagValidationResult dag = dagValidator.validate(supervisorPlan.tasks());

        Map<String, SupervisorSubTask> tasksById = indexTasksById(supervisorPlan.tasks());
        Map<String, SupervisorResult.TaskResult> taskResultsByTaskId = new LinkedHashMap<>();
        List<RollbackEntry> rollbackEntries = new ArrayList<>();
        List<String> rollbackAuditMessages = new ArrayList<>();

        for (List<String> batch : buildBatches(dag)) {
            BatchExecutionOutcome outcome = executeBatch(batch, tasksById, validatedRequest);

            for (TaskExecutionOutcome taskOutcome : outcome.completedTasks()) {
                taskResultsByTaskId.put(taskOutcome.taskId(), toTaskResult(taskOutcome.result()));
                if (taskOutcome.result().isSuccess()) {
                    captureRollbackEntry(taskOutcome.taskId(), taskOutcome.result(), rollbackEntries);
                }
            }

            String batchError = outcome.errorMessage();

            if (batchError != null) {
                materializeBatchFailureTaskResults(taskResultsByTaskId, outcome.failureTaskIds(), batchError);
                rollbackCompletedTasks(validatedRequest.tenantId(), rollbackEntries, rollbackAuditMessages);
                return failureResult(
                    validatedRequest,
                    supervisorPlan,
                    taskResultsByTaskId,
                    rollbackEntries,
                    rollbackAuditMessages,
                    batchError
                );
            }
        }

        return new SupervisorResult(
            true,
            validatedRequest.requestId(),
            supervisorPlan.planName(),
            Instant.now(),
            taskResultsByTaskId,
            rollbackEntries,
            rollbackAuditMessages,
            null
        );
    }

    private BatchExecutionOutcome executeBatch(
        List<String> batch,
        Map<String, SupervisorSubTask> tasksById,
        SupervisorRequest request
    ) {
        BatchState batchState = new BatchState();

        try (var scope = StructuredTaskScope.open(
            StructuredTaskScope.Joiner.<TaskExecutionOutcome>allUntil(
                subtask -> subtask.state() == StructuredTaskScope.Subtask.State.FAILED
            ),
            cfg -> cfg.withTimeout(taskTimeout)
        )) {
            for (String taskId : batch) {
                batchState.recordTaskStarted(taskId);
                scope.fork(() -> {
                    try {
                        SkillExecutor.SkillResult result = executeTask(request, tasksById.get(taskId));
                        TaskExecutionOutcome outcome = new TaskExecutionOutcome(
                            taskId,
                            result,
                            batchState.nextCompletionSequence()
                        );
                        if (result.isSuccess()) {
                            batchState.recordCompletedSuccess(outcome);
                            afterTaskSuccess(outcome);
                            batchState.publishSuccess(outcome);
                            return outcome;
                        }

                        String failureMessage = batchFailureMessage(result);
                        batchState.recordFailure(taskId, failureMessage, outcome);
                        throw new BatchFailureException(failureMessage);
                    } catch (RuntimeException e) {
                        if (!(e instanceof BatchFailureException) && !batchState.hasFailure()) {
                            batchState.recordFailure(taskId, batchFailureMessage(e), null);
                        }
                        throw e;
                    } catch (Exception e) {
                        if (e instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                            throw e;
                        }
                        String failureMessage = batchFailureMessage(e);
                        batchState.recordFailure(taskId, failureMessage, null);
                        throw new BatchFailureException(failureMessage, e);
                    }
                });
            }

            try {
                scope.join();
            } catch (StructuredTaskScope.TimeoutException e) {
                batchState.recordTimeout(batch, "Supervisor batch timed out after " + taskTimeout);
            }

            return batchState.toOutcome();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            batchState.recordInterrupted(batch, "Supervisor batch execution interrupted");
            return batchState.toOutcome();
        }
    }

    private SkillExecutor.SkillResult executeTask(SupervisorRequest request, SupervisorSubTask task) {
        return skillExecutor.execute(new SkillExecutor.SkillRequest(
            task.skillId(),
            request.tenantId(),
            request.userId(),
            task.parameters(),
            new SkillExecutor.SkillRequest.ExecutionContext(
                taskTimeout,
                true,
                false,
                Map.of("supervisorTaskId", task.taskId()),
                request.requestId()
            )
        ));
    }

    void afterTaskSuccess(TaskExecutionOutcome outcome) {
    }

    private static Map<String, SupervisorSubTask> indexTasksById(List<SupervisorSubTask> tasks) {
        var tasksById = new LinkedHashMap<String, SupervisorSubTask>(tasks.size());
        for (SupervisorSubTask task : tasks) {
            tasksById.put(task.taskId(), task);
        }
        return Map.copyOf(tasksById);
    }

    private static List<List<String>> buildBatches(DagValidationResult dag) {
        var batches = new ArrayList<List<String>>();
        var currentBatch = new ArrayList<String>();
        Integer currentDepth = null;

        for (String taskId : dag.stableTopologicalOrder()) {
            int depth = dag.dependencyDepthByTaskId().get(taskId);
            if (currentDepth != null && depth != currentDepth) {
                batches.add(List.copyOf(currentBatch));
                currentBatch = new ArrayList<>();
            }
            currentDepth = depth;
            currentBatch.add(taskId);
        }

        if (!currentBatch.isEmpty()) {
            batches.add(List.copyOf(currentBatch));
        }

        return List.copyOf(batches);
    }

    private static SupervisorResult.TaskResult toTaskResult(SkillExecutor.SkillResult result) {
        return switch (result) {
            case SkillExecutor.SkillResult.Success success ->
                new SupervisorResult.TaskResult(true, success.output(), null);
            case SkillExecutor.SkillResult.FallbackApplied fallbackApplied ->
                new SupervisorResult.TaskResult(true, fallbackApplied.output(), null);
            case SkillExecutor.SkillResult.Failure failure ->
                new SupervisorResult.TaskResult(false, Map.of(), failure.errorMessage());
            case SkillExecutor.SkillResult.Timeout timeout ->
                new SupervisorResult.TaskResult(false, Map.of(), "Timed out after " + timeout.timeout());
        };
    }

    private static void materializeBatchFailureTaskResults(
        Map<String, SupervisorResult.TaskResult> taskResultsByTaskId,
        List<String> failureTaskIds,
        String failureMessage
    ) {
        if (failureTaskIds == null || failureTaskIds.isEmpty()) {
            return;
        }
        for (String failureTaskId : failureTaskIds) {
            taskResultsByTaskId.putIfAbsent(
                failureTaskId,
                new SupervisorResult.TaskResult(false, Map.of(), failureMessage)
            );
        }
    }

    private static SupervisorResult failureResult(
        SupervisorRequest request,
        SupervisorPlan supervisorPlan,
        Map<String, SupervisorResult.TaskResult> taskResultsByTaskId,
        List<RollbackEntry> rollbackEntries,
        List<String> rollbackAuditMessages,
        String errorMessage
    ) {
        return new SupervisorResult(
            false,
            request.requestId(),
            supervisorPlan.planName(),
            Instant.now(),
            taskResultsByTaskId,
            rollbackEntries,
            rollbackAuditMessages,
            errorMessage
        );
    }

    private static void captureRollbackEntry(
        String taskId,
        SkillExecutor.SkillResult result,
        List<RollbackEntry> rollbackEntries
    ) {
        Map<String, Object> output = switch (result) {
            case SkillExecutor.SkillResult.Success success -> success.output();
            case SkillExecutor.SkillResult.FallbackApplied fallbackApplied -> fallbackApplied.output();
            case SkillExecutor.SkillResult.Failure ignored -> null;
            case SkillExecutor.SkillResult.Timeout ignored -> null;
        };
        if (output == null) {
            return;
        }

        Object metadataName = output.get("metadataName");
        Object version = output.get("version");
        if (metadataName instanceof String name && version instanceof Number number) {
            rollbackEntries.add(new RollbackEntry(UUID.randomUUID(), name, number.intValue(), taskId));
        }
    }

    private void rollbackCompletedTasks(
        UUID tenantId,
        List<RollbackEntry> rollbackEntries,
        List<String> rollbackAuditMessages
    ) {
        for (int index = rollbackEntries.size() - 1; index >= 0; index--) {
            RollbackEntry entry = rollbackEntries.get(index);
            MetadataHotReloadManager.RollbackResult rollbackResult = hotReloadManager.rollback(
                tenantId,
                entry.metadataName(),
                entry.targetVersion()
            );
            rollbackAuditMessages.add(entry.taskId() + ":" + rollbackResult.success());
        }
    }

    record TaskExecutionOutcome(String taskId, SkillExecutor.SkillResult result, long completionSequence) {
    }

    private static String batchFailureMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return (message == null || message.isBlank()) ? "Supervisor batch task failed" : message;
    }

    private static String batchFailureMessage(SkillExecutor.SkillResult result) {
        return switch (result) {
            case SkillExecutor.SkillResult.Failure failure -> {
                String message = failure.errorMessage();
                yield (message == null || message.isBlank()) ? "Supervisor batch task failed" : message;
            }
            case SkillExecutor.SkillResult.Timeout timeout -> "Timed out after " + timeout.timeout();
            case SkillExecutor.SkillResult.Success ignored -> "Supervisor batch task failed";
            case SkillExecutor.SkillResult.FallbackApplied ignored -> "Supervisor batch task failed";
        };
    }

    private static final class BatchState {
        private final ReentrantLock lock = new ReentrantLock();
        private final AtomicLong completionSequence = new AtomicLong();
        private final Map<String, TaskExecutionOutcome> visibleOutcomesByTaskId = new LinkedHashMap<>();
        private final Map<String, TaskExecutionOutcome> pendingSuccessesByTaskId = new LinkedHashMap<>();
        private final List<String> startedTaskIds = new ArrayList<>();
        private final List<String> finishedTaskIds = new ArrayList<>();
        private String failureTaskId;
        private List<String> failureTaskIds = List.of();
        private String failureMessage;
        private long failureSequence = Long.MAX_VALUE;

        void recordTaskStarted(String taskId) {
            lock.lock();
            try {
                startedTaskIds.add(taskId);
            } finally {
                lock.unlock();
            }
        }

        long nextCompletionSequence() {
            return completionSequence.incrementAndGet();
        }

        void recordCompletedSuccess(TaskExecutionOutcome outcome) {
            lock.lock();
            try {
                finishedTaskIds.add(outcome.taskId());
                if (outcome.completionSequence() < failureSequence) {
                    pendingSuccessesByTaskId.put(outcome.taskId(), outcome);
                }
            } finally {
                lock.unlock();
            }
        }

        void publishSuccess(TaskExecutionOutcome outcome) {
            lock.lock();
            try {
                TaskExecutionOutcome recordedOutcome = pendingSuccessesByTaskId.get(outcome.taskId());
                if (recordedOutcome != null && recordedOutcome.completionSequence() < failureSequence) {
                    visibleOutcomesByTaskId.put(outcome.taskId(), recordedOutcome);
                    pendingSuccessesByTaskId.remove(outcome.taskId());
                }
            } finally {
                lock.unlock();
            }
        }

        void recordFailure(String taskId, String message, TaskExecutionOutcome outcome) {
            lock.lock();
            try {
                finishedTaskIds.add(taskId);
                if (failureMessage == null) {
                    failureTaskId = taskId;
                    failureTaskIds = List.of(taskId);
                    failureMessage = message;
                    failureSequence = outcome == null ? completionSequence.incrementAndGet() : outcome.completionSequence();
                    if (outcome != null) {
                        visibleOutcomesByTaskId.put(taskId, outcome);
                    }
                }
            } finally {
                lock.unlock();
            }
        }

        void recordTimeout(List<String> batchTaskIds, String message) {
            recordBatchLevelFailure(unfinishedTaskIds(batchTaskIds), message);
        }

        void recordInterrupted(List<String> batchTaskIds, String message) {
            recordBatchLevelFailure(unfinishedTaskIds(batchTaskIds), message);
        }

        private void recordBatchLevelFailure(List<String> candidateTaskIds, String message) {
            lock.lock();
            try {
                if (failureMessage == null) {
                    List<String> failedTaskIds = candidateTaskIds.isEmpty()
                        ? fallbackTaskIds()
                        : List.copyOf(candidateTaskIds);
                    if (failedTaskIds.isEmpty()) {
                        return;
                    }
                    failureTaskId = failedTaskIds.getFirst();
                    failureTaskIds = failedTaskIds;
                    failureMessage = message;
                    failureSequence = completionSequence.incrementAndGet();
                }
            } finally {
                lock.unlock();
            }
        }

        private List<String> unfinishedTaskIds(List<String> batchTaskIds) {
            lock.lock();
            try {
                List<String> unfinishedTaskIds = new ArrayList<>();
                for (String taskId : batchTaskIds) {
                    if (!finishedTaskIds.contains(taskId)) {
                        unfinishedTaskIds.add(taskId);
                    }
                }
                return unfinishedTaskIds;
            } finally {
                lock.unlock();
            }
        }

        private List<String> fallbackTaskIds() {
            if (!startedTaskIds.isEmpty()) {
                return List.of(startedTaskIds.getFirst());
            }
            return failureTaskId == null ? List.of() : List.of(failureTaskId);
        }

        boolean hasFailure() {
            lock.lock();
            try {
                return failureMessage != null;
            } finally {
                lock.unlock();
            }
        }

        String failureTaskIdOr(String fallbackTaskId) {
            lock.lock();
            try {
                return failureTaskId == null ? fallbackTaskId : failureTaskId;
            } finally {
                lock.unlock();
            }
        }

        BatchExecutionOutcome toOutcome() {
            lock.lock();
            try {
                Map<String, TaskExecutionOutcome> mergedOutcomesByTaskId = new LinkedHashMap<>(visibleOutcomesByTaskId);
                for (TaskExecutionOutcome pendingOutcome : pendingSuccessesByTaskId.values()) {
                    if (pendingOutcome.completionSequence() < failureSequence) {
                        mergedOutcomesByTaskId.putIfAbsent(pendingOutcome.taskId(), pendingOutcome);
                    }
                }
                List<TaskExecutionOutcome> completedTasks = mergedOutcomesByTaskId.values().stream()
                    .sorted((left, right) -> Long.compare(left.completionSequence(), right.completionSequence()))
                    .toList();
                return new BatchExecutionOutcome(completedTasks, failureTaskIds, failureMessage);
            } finally {
                lock.unlock();
            }
        }
    }

    private static final class BatchFailureException extends RuntimeException {
        private BatchFailureException(String message) {
            super(message);
        }

        private BatchFailureException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private record BatchExecutionOutcome(List<TaskExecutionOutcome> completedTasks, List<String> failureTaskIds, String errorMessage) {
    }
}
