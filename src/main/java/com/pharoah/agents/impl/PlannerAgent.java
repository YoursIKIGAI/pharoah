package com.pharoah.agents.impl;

import com.pharoah.agents.AbstractAgent;
import com.pharoah.ai.AIService;
import com.pharoah.core.enums.TaskType;
import com.pharoah.core.model.Task;
import com.pharoah.knowledge.SubjectMemory;

/**
 * Specialized agent for project planning, task decomposition, and milestone scheduling.
 */
public class PlannerAgent extends AbstractAgent {

    public PlannerAgent(AIService aiService) {
        super("PlannerAgent", "Breaks down complex goals into milestone phases and prioritized roadmaps.", aiService);
    }

    @Override
    public boolean canHandle(TaskType taskType) {
        return taskType == TaskType.PLANNING;
    }

    @Override
    protected String doExecute(Task task, SubjectMemory memory, String contextSummary) {
        return aiService.generatePlan(task.getTitle(), task.getInputPayload());
    }
}
