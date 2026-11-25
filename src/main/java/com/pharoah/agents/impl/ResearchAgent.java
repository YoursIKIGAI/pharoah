package com.pharoah.agents.impl;

import com.pharoah.agents.AbstractAgent;
import com.pharoah.ai.AIService;
import com.pharoah.core.enums.TaskType;
import com.pharoah.core.model.Task;
import com.pharoah.knowledge.SubjectMemory;

/**
 * Specialized agent for topic research, literature reviews, and research gap identification.
 */
public class ResearchAgent extends AbstractAgent {

    public ResearchAgent(AIService aiService) {
        super("ResearchAgent", "Explores domain topics, surveys literature, and extracts academic findings.", aiService);
    }

    @Override
    public boolean canHandle(TaskType taskType) {
        return taskType == TaskType.RESEARCH;
    }

    @Override
    protected String doExecute(Task task, SubjectMemory memory, String contextSummary) {
        return aiService.synthesizeResearch(task.getTitle(), contextSummary + "\nDetails: " + task.getInputPayload());
    }
}
