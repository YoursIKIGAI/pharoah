package com.pharoah.core.enums;

/**
 * Tracks the lifecycle states of a task in PHAROAH.
 */
public enum TaskStatus {
    PENDING("Created, awaiting submission"),
    QUEUED("Placed in queue for worker scheduling"),
    IN_PROGRESS("Currently being processed by an agent"),
    COMPLETED("Successfully finished with final output"),
    FAILED("Execution encountered an unrecoverable error"),
    CANCELLED("Manually aborted by user or orchestrator");

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
