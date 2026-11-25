package com.pharoah.concurrency;

import com.pharoah.core.model.Task;
import com.pharoah.core.model.TaskResult;

import java.util.concurrent.Callable;

/**
 * Callable task representing an agent executing a unit of intellectual work.
 * 
 * Demonstrates:
 * 1. Callable<V> vs Runnable: Callable can return a computed result and throw checked exceptions.
 * 2. Execution timing measurement.
 * 3. Safe error containment inside asynchronous execution threads.
 */
public class AgentExecutionTask implements Callable<TaskResult> {

    private final Task task;
    private final String agentName;
    private final long simulatedWorkDurationMs;

    public AgentExecutionTask(Task task, String agentName, long simulatedWorkDurationMs) {
        this.task = task;
        this.agentName = agentName;
        this.simulatedWorkDurationMs = simulatedWorkDurationMs;
    }

    public AgentExecutionTask(Task task, String agentName) {
        this(task, agentName, 100L);
    }

    @Override
    public TaskResult call() throws Exception {
        long startTime = System.currentTimeMillis();

        try {
            // Simulate agent intelligence processing time
            if (simulatedWorkDurationMs > 0) {
                Thread.sleep(simulatedWorkDurationMs);
            }

            String synthesizedOutput = String.format(
                    "[%s] Completed task '%s' (Type: %s, Priority: %s). Analysis: %s",
                    agentName,
                    task.getTitle(),
                    task.getType(),
                    task.getPriority(),
                    task.getInputPayload()
            );

            long duration = System.currentTimeMillis() - startTime;
            return TaskResult.success(task.getTaskId(), synthesizedOutput, duration, agentName);

        } catch (InterruptedException e) {
            long duration = System.currentTimeMillis() - startTime;
            Thread.currentThread().interrupt(); // Restore interrupted status
            return TaskResult.failure(task.getTaskId(), "Agent execution was interrupted.", duration, agentName);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return TaskResult.failure(task.getTaskId(), "Agent failed: " + e.getMessage(), duration, agentName);
        }
    }
}
