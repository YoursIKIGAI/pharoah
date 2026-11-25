package com.pharoah.tasks;

import com.pharoah.core.enums.Priority;
import com.pharoah.core.enums.TaskStatus;
import com.pharoah.core.enums.TaskType;
import com.pharoah.core.model.Task;
import com.pharoah.core.model.TaskResult;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central Task Manager for PHAROAH.
 * 
 * Demonstrates practical usage of Java Collections Framework:
 * 1. ArrayList: Dynamic ordered list for complete task audit and history.
 * 2. HashSet: O(1) set to enforce unique task identifiers and fast duplicate checking.
 * 3. HashMap: O(1) key-value dictionary for instant task retrieval by ID.
 * 4. PriorityQueue: Min/Max Binary Heap that automatically schedules HIGH priority tasks first.
 * 5. ConcurrentHashMap: Thread-safe map tracking tasks currently in active execution.
 */
public class TaskManager {

    // 1. ArrayList: Retains chronological insertion order for history logging
    private final List<Task> taskHistory = new ArrayList<>();

    // 2. HashSet: Provides instant O(1) membership check to ensure unique IDs
    private final Set<String> registeredTaskIds = new HashSet<>();

    // 3. HashMap: O(1) lookup table mapping taskId -> Task instance
    private final Map<String, Task> taskLookupMap = new HashMap<>();

    // 4. PriorityQueue: Automatically maintains order based on Task.compareTo()
    private final PriorityQueue<Task> priorityTaskQueue = new PriorityQueue<>();

    // 5. ConcurrentHashMap: Safe for concurrent reads/writes when worker threads pick up tasks
    private final Map<String, Task> activeTasksMap = new ConcurrentHashMap<>();

    /**
     * Submits a new task into the system.
     * Inserts the task into history, registry, lookup map, and priority scheduling queue.
     *
     * @param task The task to be scheduled.
     * @return true if successfully registered, false if already exists (duplicate ID).
     */
    public synchronized boolean submitTask(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null.");
        }

        String id = task.getTaskId();

        // HashSet check: Ensure ID uniqueness in O(1) time
        if (registeredTaskIds.contains(id)) {
            return false; // Duplicate detected
        }

        registeredTaskIds.add(id);
        taskLookupMap.put(id, task);
        taskHistory.add(task);

        task.updateStatus(TaskStatus.QUEUED);
        priorityTaskQueue.offer(task);

        return true;
    }

    /**
     * Retrieves and removes the highest priority pending task from the scheduling queue.
     * If multiple tasks share the same priority, the oldest one is returned (FIFO).
     *
     * @return The next Task to execute, or null if the queue is empty.
     */
    public synchronized Task pollNextPriorityTask() {
        Task nextTask = priorityTaskQueue.poll();
        if (nextTask != null) {
            nextTask.updateStatus(TaskStatus.IN_PROGRESS);
            // Move into ConcurrentHashMap tracking active worker tasks
            activeTasksMap.put(nextTask.getTaskId(), nextTask);
        }
        return nextTask;
    }

    /**
     * Marks an active task as completed with its final result.
     *
     * @param taskId The ID of the task.
     * @param result The execution result.
     */
    public synchronized boolean completeTask(String taskId, TaskResult result) {
        if (taskId == null || result == null) return false;

        Task task = taskLookupMap.get(taskId);
        if (task == null) return false;

        if (result.status() == TaskStatus.COMPLETED) {
            task.markCompleted(result.output());
        } else {
            task.markFailed(result.output());
        }

        // Remove from active execution map
        activeTasksMap.remove(taskId);
        return true;
    }

    /**
     * Cancels a task if it has not yet reached a terminal state.
     *
     * @param taskId The ID of the task to abort.
     * @return true if cancelled, false if not found or already finished.
     */
    public synchronized boolean cancelTask(String taskId) {
        Task task = taskLookupMap.get(taskId);
        if (task == null || task.getStatus().isTerminal()) {
            return false;
        }

        task.updateStatus(TaskStatus.CANCELLED);
        priorityTaskQueue.remove(task);
        activeTasksMap.remove(taskId);
        return true;
    }

    /**
     * Fast O(1) lookup of any task by its unique ID.
     */
    public synchronized Task getTaskById(String taskId) {
        return taskLookupMap.get(taskId);
    }

    /**
     * Checks if a task with the given ID exists.
     */
    public synchronized boolean containsTask(String taskId) {
        return registeredTaskIds.contains(taskId);
    }

    /**
     * Returns the number of tasks waiting in the PriorityQueue.
     */
    public synchronized int getPendingCount() {
        return priorityTaskQueue.size();
    }

    /**
     * Returns the number of tasks currently in-flight across workers.
     */
    public int getActiveCount() {
        return activeTasksMap.size();
    }

    /**
     * Returns the total number of tasks ever submitted.
     */
    public synchronized int getTotalSubmittedCount() {
        return taskHistory.size();
    }

    /**
     * Returns an unmodifiable view of the complete task history (ArrayList).
     */
    public synchronized List<Task> getTaskHistory() {
        return Collections.unmodifiableList(new ArrayList<>(taskHistory));
    }

    /**
     * Filters tasks by their intellectual category using standard collection iteration.
     */
    public synchronized List<Task> getTasksByType(TaskType type) {
        List<Task> filtered = new ArrayList<>();
        for (Task t : taskHistory) {
            if (t.getType() == type) {
                filtered.add(t);
            }
        }
        return filtered;
    }

    /**
     * Filters tasks assigned to a specific Subject Knowledge Space.
     */
    public synchronized List<Task> getTasksBySubject(String subjectId) {
        List<Task> filtered = new ArrayList<>();
        for (Task t : taskHistory) {
            if (t.getSubjectId().equalsIgnoreCase(subjectId)) {
                filtered.add(t);
            }
        }
        return filtered;
    }

    /**
     * Clears all in-memory collections (useful for resetting testing state).
     */
    public synchronized void clear() {
        taskHistory.clear();
        registeredTaskIds.clear();
        taskLookupMap.clear();
        priorityTaskQueue.clear();
        activeTasksMap.clear();
    }
}
