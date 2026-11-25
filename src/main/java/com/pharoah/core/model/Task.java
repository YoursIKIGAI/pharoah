package com.pharoah.core.model;

import com.pharoah.core.enums.Priority;
import com.pharoah.core.enums.TaskStatus;
import com.pharoah.core.enums.TaskType;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

// SANCHIT & VARIJ: Implemented Task domain entity.
// Supports priority scheduling, project association, assigned agent, and task dependencies.
public class Task implements Comparable<Task> {

    private final String taskId;
    private final String projectId;
    private final String title;
    private final TaskType type;
    private final Priority priority;
    private final String inputPayload;
    private final String dependsOnTaskId; // ID of prerequisite task
    private final LocalDateTime createdAt;

    private String assignedAgentId;
    private TaskStatus status;
    private String finalOutput;
    private String reviewNotes;
    private LocalDateTime completedAt;

    public Task(String taskId, String projectId, String title, TaskType type, Priority priority,
                String inputPayload, String assignedAgentId, String dependsOnTaskId,
                TaskStatus status, String finalOutput, String reviewNotes,
                LocalDateTime createdAt, LocalDateTime completedAt) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Task title cannot be empty.");
        }
        if (type == null) {
            throw new IllegalArgumentException("TaskType cannot be null.");
        }
        if (priority == null) {
            throw new IllegalArgumentException("Priority cannot be null.");
        }

        this.taskId = (taskId != null && !taskId.trim().isEmpty()) ? taskId : UUID.randomUUID().toString();
        this.projectId = (projectId != null && !projectId.trim().isEmpty()) ? projectId.trim() : "GLOBAL";
        this.title = title.trim();
        this.type = type;
        this.priority = priority;
        this.inputPayload = (inputPayload != null) ? inputPayload.trim() : "";
        this.assignedAgentId = assignedAgentId;
        this.dependsOnTaskId = dependsOnTaskId;
        this.status = (status != null) ? status : TaskStatus.PENDING;
        this.finalOutput = finalOutput;
        this.reviewNotes = reviewNotes;
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
        this.completedAt = completedAt;
    }

    public Task(String projectId, String title, TaskType type, Priority priority, String inputPayload, String assignedAgentId, String dependsOnTaskId) {
        this(UUID.randomUUID().toString(), projectId, title, type, priority, inputPayload, assignedAgentId, dependsOnTaskId, TaskStatus.PENDING, null, null, LocalDateTime.now(), null);
    }

    public Task(String title, TaskType type, Priority priority, String inputPayload, String projectId) {
        this(UUID.randomUUID().toString(), projectId, title, type, priority, inputPayload, null, null, TaskStatus.PENDING, null, null, LocalDateTime.now(), null);
    }

    public Task(String title, TaskType type, Priority priority, String inputPayload) {
        this(title, type, priority, inputPayload, "GLOBAL");
    }

    public String getTaskId() {
        return taskId;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getSubjectId() {
        return projectId; // Backwards compatibility alias for subject/project workspace
    }

    public String getTitle() {
        return title;
    }

    public TaskType getType() {
        return type;
    }

    public Priority getPriority() {
        return priority;
    }

    public String getInputPayload() {
        return inputPayload;
    }

    public String getAssignedAgentId() {
        return assignedAgentId;
    }

    public void setAssignedAgentId(String assignedAgentId) {
        this.assignedAgentId = assignedAgentId;
    }

    public String getDependsOnTaskId() {
        return dependsOnTaskId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void updateStatus(TaskStatus status) {
        this.status = status;
    }

    public String getFinalOutput() {
        return finalOutput;
    }

    public void setFinalOutput(String finalOutput) {
        this.finalOutput = finalOutput;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void markCompleted(String output) {
        this.status = TaskStatus.COMPLETED;
        this.finalOutput = output;
        this.completedAt = LocalDateTime.now();
    }

    public void markUnderReview(String output) {
        this.status = TaskStatus.UNDER_REVIEW;
        this.finalOutput = output;
    }

    public void markFailed(String errorMessage) {
        this.status = TaskStatus.FAILED;
        this.finalOutput = "ERROR: " + errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    @Override
    public int compareTo(Task other) {
        if (other == null) return -1;
        // Priority descending
        int p = Integer.compare(other.priority.getLevel(), this.priority.getLevel());
        if (p != 0) return p;
        // Timestamp ascending (FIFO)
        return this.createdAt.compareTo(other.createdAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task task)) return false;
        return Objects.equals(taskId, task.taskId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId);
    }

    @Override
    public String toString() {
        return String.format("[%s] Task #%s | %-16s | Priority: %-6s | Status: %-12s | Project: %s",
                createdAt.toLocalTime().withNano(0),
                taskId.substring(0, Math.min(8, taskId.length())),
                type,
                priority,
                status,
                projectId);
    }
}
