package com.pharoah.agents.impl;

import com.pharoah.agents.AbstractAgent;
import com.pharoah.ai.AIService;
import com.pharoah.core.enums.TaskType;
import com.pharoah.core.model.Task;
import com.pharoah.knowledge.SubjectMemory;

/**
 * Specialized agent for dataset analysis, statistical summaries, and CSV pattern extraction.
 */
public class DataAnalystAgent extends AbstractAgent {

    public DataAnalystAgent(AIService aiService) {
        super("DataAnalystAgent", "Processes datasets, CSV files, and computes statistical summaries.", aiService);
    }

    @Override
    public boolean canHandle(TaskType taskType) {
        return taskType == TaskType.DATA_ANALYSIS;
    }

    @Override
    protected String doExecute(Task task, SubjectMemory memory, String contextSummary) {
        return aiService.analyzeData(task.getInputPayload());
    }
}
