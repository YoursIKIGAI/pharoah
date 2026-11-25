package com.pharoah.agents.impl;

import com.pharoah.agents.AbstractAgent;
import com.pharoah.ai.AIService;
import com.pharoah.core.enums.TaskType;
import com.pharoah.core.model.Task;
import com.pharoah.knowledge.SubjectMemory;

/**
 * Specialized agent for concise summarization and executive briefings.
 */
public class SummariserAgent extends AbstractAgent {

    public SummariserAgent(AIService aiService) {
        super("SummariserAgent", "Compresses lengthy texts, transcripts, and documents into key takeaways.", aiService);
    }

    @Override
    public boolean canHandle(TaskType taskType) {
        return taskType == TaskType.WRITING || taskType == TaskType.DOCUMENT_ANALYSIS;
    }

    @Override
    protected String doExecute(Task task, SubjectMemory memory, String contextSummary) {
        String summary = aiService.generateSummary(task.getInputPayload(), 4);
        return "### 📝 Executive Summary\n" + summary;
    }
}
