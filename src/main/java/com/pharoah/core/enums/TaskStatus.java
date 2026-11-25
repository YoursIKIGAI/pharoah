package com.pharoah.core.enums;

// SANCHIT: Implemented TaskStatus enum.
// Tracks the complete lifecycle of tasks including human review approval.
public enum TaskStatus {
    PENDING("Task created and waiting in backlog"),
    QUEUED("Task placed in queue for worker scheduling"),
    IN_PROGRESS("Currently being processed by an agent"),
    UNDER_REVIEW("Execution completed; awaiting human review and approval"),
    COMPLETED("Reviewed and approved as complete"),
    FAILED("Execution encountered an error or was rejected"),
    CANCELLED("Aborted by the user");

    private final String description;

    TaskStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
}
