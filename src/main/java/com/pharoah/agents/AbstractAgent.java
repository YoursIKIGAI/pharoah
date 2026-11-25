package com.pharoah.agents;

import com.pharoah.ai.AIService;
import com.pharoah.core.enums.TaskStatus;
import com.pharoah.core.model.Task;
import com.pharoah.core.model.TaskResult;
import com.pharoah.knowledge.SubjectMemory;

import java.util.UUID;

/**
 * Abstract Base Class for all agents, implementing the Template Method Pattern.
 * 
 * Provides:
 * 1. Automatic execution duration timing.
 * 2. Unified error handling and exception containment.
 * 3. Execution logging.
 * 4. Access to the pluggable AIService.
 */
public abstract class AbstractAgent implements Agent {

    private final String agentId;
    private final String name;
    private final String description;
    protected final AIService aiService;

    public AbstractAgent(String name, String description, AIService aiService) {
        this.agentId = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.aiService = aiService;
    }

    @Override
    public String getAgentId() {
        return agentId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Template method: Handles common lifecycle, timing, and error handling.
     */
    @Override
    public TaskResult execute(Task task, SubjectMemory memory) {
        long startTime = System.currentTimeMillis();
        System.out.printf("[%s] Commencing execution on Task #%s ('%s')...\n",
                getName(), task.getTaskId().substring(0, 8), task.getTitle());

        try {
            String contextSummary = (memory != null) ? memory.synthesizeContextSummary() : "No Subject Context";
            String output = doExecute(task, memory, contextSummary);

            long duration = System.currentTimeMillis() - startTime;
            System.out.printf("[%s] Successfully completed in %d ms.\n", getName(), duration);

            // Record this interaction back into subject memory if applicable
            if (memory != null) {
                memory.addConversationMessage(getName(), "Produced output for: " + task.getTitle());
            }

            return TaskResult.success(task.getTaskId(), output, duration, getName());

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            System.err.printf("[%s ERROR] Failed while processing Task #%s: %s\n",
                    getName(), task.getTaskId().substring(0, 8), e.getMessage());
            return TaskResult.failure(task.getTaskId(), e.getMessage(), duration, getName());
        }
    }

    /**
     * Subclasses implement their specialized intellectual logic here.
     */
    protected abstract String doExecute(Task task, SubjectMemory memory, String contextSummary) throws Exception;
}
