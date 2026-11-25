package com.pharoah.core.model;

import com.pharoah.core.enums.TaskStatus;

import java.time.LocalDateTime;

/**
 * An immutable data record representing the final execution outcome of a task.
 * 
 * Uses Java Record (introduced for immutable data-carrier classes) which automatically
 * generates constructor, getters, equals(), hashCode(), and toString().
 */
public record TaskResult(
        String taskId,
        TaskStatus status,
        String output,
        long executionDurationMs,
        String executedByAgent,
        LocalDateTime finishedAt
) {
    public static TaskResult success(String taskId, String output, long durationMs, String agentName) {
        return new TaskResult(taskId, TaskStatus.COMPLETED, output, durationMs, agentName, LocalDateTime.now());
    }

    public static TaskResult failure(String taskId, String errorMessage, long durationMs, String agentName) {
        return new TaskResult(taskId, TaskStatus.FAILED, errorMessage, durationMs, agentName, LocalDateTime.now());
    }
}
