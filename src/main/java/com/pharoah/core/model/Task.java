package com.pharoah.core.model;

import com.pharoah.core.enums.Priority;
import com.pharoah.core.enums.TaskStatus;
import com.pharoah.core.enums.TaskType;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing an intellectual task submitted to PHAROAH.
 * 
 * Demonstrates Core Java concepts:
 * 1. OOP Encapsulation: Private state guarded with getters and business validation.
 * 2. Comparable interface: Implements natural ordering for PriorityQueue scheduling.
 * 3. equals() & hashCode() contract: Ensures consistent behavior inside Hash-based collections (HashSet, HashMap).
 */
public class Task implements Comparable<Task> {

    private final String taskId;
    private final String title;
    private final TaskType type;
    private final Priority priority;
    private final String inputPayload;
    private final String subjectId; // Optional: Link to a specific Subject Knowledge Space
    private final LocalDateTime createdAt;

    private TaskStatus status;
    private String finalOutput;
    private LocalDateTime completedAt;

    /**
     * Constructor to create a new task with automatic unique ID generation.
     *
     * @param title        Brief title describing the work.
     * @param type         Category of task (e.g. RESEARCH, COMPARISON).
     * @param priority     Urgency level (HIGH, MEDIUM, LOW).
     * @param inputPayload Detailed instructions or document text to process.
     * @param subjectId    Optional identifier linking to a Subject Knowledge Space.
     */
    public Task(String title, TaskType type, Priority priority, String inputPayload, String subjectId) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Task title cannot be null or empty.");
        }
        if (type == null) {
            throw new IllegalArgumentException("TaskType cannot be null.");
        }
        if (priority == null) {
            throw new IllegalArgumentException("Priority cannot be null.");
        }
        if (inputPayload == null || inputPayload.trim().isEmpty()) {
            throw new IllegalArgumentException("Task input payload cannot be null or empty.");
        }

        this.taskId = UUID.randomUUID().toString();
        this.title = title.trim();
        this.type = type;
        this.priority = priority;
        this.inputPayload = inputPayload.trim();
        this.subjectId = (subjectId != null && !subjectId.trim().isEmpty()) ? subjectId.trim() : "GENERAL";
        this.createdAt = LocalDateTime.now();
        this.status = TaskStatus.PENDING;
    }

    /**
     * Overloaded convenience constructor for standalone general tasks.
     */
    public Task(String title, TaskType type, Priority priority, String inputPayload) {
        this(title, type, priority, inputPayload, "GENERAL");
    }

    // ================= Getters =================

    public String getTaskId() {
        return taskId;
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

    public String getSubjectId() {
        return subjectId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getFinalOutput() {
        return finalOutput;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    // ================= Business State Transitions =================

    public void updateStatus(TaskStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("New status cannot be null.");
        }
        this.status = newStatus;
    }

    public void markCompleted(String output) {
        this.status = TaskStatus.COMPLETED;
        this.finalOutput = output;
        this.completedAt = LocalDateTime.now();
    }

    public void markFailed(String errorMessage) {
        this.status = TaskStatus.FAILED;
        this.finalOutput = "ERROR: " + errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    // ================= Comparable Implementation =================

    /**
     * Defines natural ordering for PriorityQueue scheduling:
     * 1. Higher priority levels come first (e.g. HIGH (3) comes before LOW (1)).
     * 2. If priorities are equal, the earlier task (older createdAt) comes first (FIFO).
     */
    @Override
    public int compareTo(Task other) {
        if (other == null) {
            return -1;
        }

        // Compare Priority descending: larger priority level should be dequeued first
        int priorityComparison = Integer.compare(other.priority.getLevel(), this.priority.getLevel());
        if (priorityComparison != 0) {
            return priorityComparison;
        }

        // If priority is the same, older timestamp comes first (FIFO)
        return this.createdAt.compareTo(other.createdAt);
    }

    // ================= equals & hashCode =================

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

    // ================= String Representation =================

    @Override
    public String toString() {
        return String.format("[%s] Task #%s | %-15s | Priority: %-6s | Status: %-11s | Subject: %s",
                createdAt.toLocalTime().withNano(0),
                taskId.substring(0, 8),
                type,
                priority,
                status,
                subjectId);
    }
}
