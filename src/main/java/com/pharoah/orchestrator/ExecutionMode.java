package com.pharoah.orchestrator;

/**
 * Execution strategy for multi-agent workflows.
 * PARALLEL: Dispatches all independent agents concurrently across available worker threads.
 * PRIORITY_BASED: Executes higher-priority eligible agents (HIGH -> MEDIUM -> LOW) in staged phases.
 */
public enum ExecutionMode {
    PARALLEL("Parallel"),
    PRIORITY_BASED("Priority-Based");

    private final String displayName;

    ExecutionMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
