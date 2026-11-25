package com.pharoah.orchestrator;

import com.pharoah.agents.Agent;
import com.pharoah.concurrency.ThreadPoolManager;
import com.pharoah.core.enums.TaskStatus;
import com.pharoah.core.model.Task;
import com.pharoah.core.model.TaskResult;
import com.pharoah.knowledge.SubjectContextManager;
import com.pharoah.knowledge.SubjectMemory;
import com.pharoah.persistence.dao.TaskDao;
import com.pharoah.tasks.TaskManager;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * The Central Brain of PHAROAH: Multi-Agent Orchestrator.
 * 
 * Coordinates:
 * 1. Task Intake & Validation.
 * 2. Polymorphic Agent Selection: Maps tasks to the best suited agent in the swarm.
 * 3. Asynchronous Concurrency: Executes tasks in parallel using ThreadPoolManager.
 * 4. Context Isolation: Supplies the correct SubjectMemory without cross-talk.
 * 5. Persistent State Synchronization: Updates SQLite database via TaskDao.
 */
public class TaskOrchestrator {

    private final TaskManager taskManager;
    private final ThreadPoolManager poolManager;
    private final SubjectContextManager contextManager;
    private final TaskDao taskDao;
    private final com.pharoah.persistence.dao.SubjectDao subjectDao;
    private final List<Agent> registeredAgents = new ArrayList<>();

    public TaskOrchestrator(TaskManager taskManager,
                            ThreadPoolManager poolManager,
                            SubjectContextManager contextManager,
                            TaskDao taskDao,
                            com.pharoah.persistence.dao.SubjectDao subjectDao) {
        this.taskManager = taskManager;
        this.poolManager = poolManager;
        this.contextManager = contextManager;
        this.taskDao = taskDao;
        this.subjectDao = subjectDao;
    }

    public TaskOrchestrator(TaskManager taskManager,
                            ThreadPoolManager poolManager,
                            SubjectContextManager contextManager,
                            TaskDao taskDao) {
        this(taskManager, poolManager, contextManager, taskDao, null);
    }

    /**
     * Registers a specialized agent into the orchestrator swarm.
     */
    public synchronized void registerAgent(Agent agent) {
        if (agent != null) {
            registeredAgents.add(agent);
            System.out.printf("[ORCHESTRATOR] Registered agent: %s (%s)\n",
                    agent.getName(), agent.getDescription());
        }
    }

    /**
     * Polymorphically discovers the best agent equipped to handle the task.
     */
    public synchronized Optional<Agent> selectAgent(Task task) {
        for (Agent agent : registeredAgents) {
            if (agent.canHandle(task.getType())) {
                return Optional.of(agent);
            }
        }
        return Optional.empty();
    }

    /**
     * Submits a task for asynchronous execution across worker threads.
     * Returns a Future yielding the eventual TaskResult.
     */
    public Future<TaskResult> submitTaskAsync(Task task) {
        // 1. Submit to Collection Manager
        taskManager.submitTask(task);

        // 2. Persist to Relational Database (if DAO available)
        if (taskDao != null) {
            try {
                if (subjectDao != null && task.getSubjectId() != null) {
                    if (subjectDao.findByName(task.getSubjectId()) == null) {
                        subjectDao.insertSubject(new com.pharoah.core.model.Subject(task.getSubjectId(), task.getSubjectId(), "Auto-created workspace", null));
                    }
                }
                taskDao.insertTask(task);
            } catch (SQLException e) {
                System.err.println("[ORCHESTRATOR DB WARN] Failed to persist queued task: " + e.getMessage());
            }
        }

        // 3. Delegate to ThreadPoolManager as Callable
        return poolManager.submitTask(() -> {
            Agent agent = selectAgent(task)
                    .orElseThrow(() -> new IllegalStateException("No registered agent found for: " + task.getType()));

            SubjectMemory memory = contextManager.getOrCreateMemory(task.getSubjectId());

            task.updateStatus(TaskStatus.IN_PROGRESS);

            // Execute agent
            TaskResult result = agent.execute(task, memory);

            // Update in-memory collections
            taskManager.completeTask(task.getTaskId(), result);

            // Update database
            if (taskDao != null) {
                try {
                    taskDao.updateTask(task);
                } catch (SQLException e) {
                    System.err.println("[ORCHESTRATOR DB WARN] Failed to update completed task: " + e.getMessage());
                }
            }

            return result;
        });
    }

    /**
     * Executes a batch of tasks in parallel, waiting for all to complete.
     */
    public List<TaskResult> executeBatchParallel(List<Task> tasks) throws Exception {
        List<Future<TaskResult>> futures = new ArrayList<>();
        for (Task t : tasks) {
            futures.add(submitTaskAsync(t));
        }

        List<TaskResult> results = new ArrayList<>();
        for (Future<TaskResult> future : futures) {
            results.add(future.get());
        }
        return results;
    }

    public List<Agent> getRegisteredAgents() {
        return Collections.unmodifiableList(registeredAgents);
    }
}
