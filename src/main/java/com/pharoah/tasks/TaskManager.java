package com.pharoah.tasks;

import com.pharoah.core.enums.Priority;
import com.pharoah.core.enums.TaskStatus;
import com.pharoah.core.enums.TaskType;
import com.pharoah.core.model.Task;
import com.pharoah.core.model.TaskResult;
import com.pharoah.persistence.dao.TaskDao;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central Task Manager for PHAROAH.
 * 
 * Manages in-memory priority scheduling and synchronizes task state
 * transitions directly into the MySQL database via TaskDao.
 */
public class TaskManager {

    private final TaskDao taskDao;

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

    public TaskManager() {
        this(null);
    }

    public TaskManager(TaskDao taskDao) {
        this.taskDao = taskDao;
    }

    /**
     * Submits a new task into the system.
     * Inserts the task into history, registry, lookup map, and priority scheduling queue,
     * and persists the record to MySQL if TaskDao is configured.
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

        if (taskDao != null) {
            try {
                taskDao.insertTask(task);
            } catch (SQLException e) {
                // If already present in DB, update instead
                try {
                    taskDao.updateTask(task);
                } catch (SQLException ignored) {}
            }
        }

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
            if (taskDao != null) {
                try {
                    taskDao.updateTask(nextTask);
                } catch (SQLException ignored) {}
            }
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

        if (taskDao != null) {
            try {
                taskDao.updateTask(task);
            } catch (SQLException ignored) {}
        }

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

        if (taskDao != null) {
            try {
                taskDao.updateTask(task);
            } catch (SQLException ignored) {}
        }

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
