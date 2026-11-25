package com.pharoah.agents.impl;

import com.pharoah.agents.AbstractAgent;
import com.pharoah.ai.AIService;
import com.pharoah.core.enums.TaskType;
import com.pharoah.core.model.Task;
import com.pharoah.knowledge.SubjectMemory;

/**
 * Specialized agent for essays, technical reports, proofreading, and rewriting.
 */
public class WriterAgent extends AbstractAgent {

    public WriterAgent(AIService aiService) {
        super("WriterAgent", "Drafts, refines, and rewrites essays, reports, and technical prose.", aiService);
    }

    @Override
    public boolean canHandle(TaskType taskType) {
        return taskType == TaskType.WRITING;
    }

    @Override
    protected String doExecute(Task task, SubjectMemory memory, String contextSummary) {
        return aiService.generateWriting(task.getInputPayload(), "Academic & Technical", contextSummary);
    }
}
