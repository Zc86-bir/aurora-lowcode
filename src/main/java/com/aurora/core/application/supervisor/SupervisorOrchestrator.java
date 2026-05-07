package com.aurora.core.application.supervisor;

public interface SupervisorOrchestrator {

    SupervisorPlan plan(SupervisorRequest request);

    SupervisorResult execute(SupervisorRequest request);
}
