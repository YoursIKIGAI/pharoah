package com.pharoah.agents.impl;

import com.pharoah.agents.AbstractAgent;
import com.pharoah.ai.AIService;
import com.pharoah.core.enums.TaskType;
import com.pharoah.core.model.Task;
import com.pharoah.knowledge.SubjectMemory;

/**
 * Specialized agent for side-by-side technology and concept comparison.
 */
public class ComparatorAgent extends AbstractAgent {

    public ComparatorAgent(AIService aiService) {
        super("ComparatorAgent", "Conducts structured comparative analysis across paradigms, frameworks, and algorithms.", aiService);
    }

    @Override
    public boolean canHandle(TaskType taskType) {
        return taskType == TaskType.COMPARISON;
    }

    @Override
    protected String doExecute(Task task, SubjectMemory memory, String contextSummary) {
        String title = task.getTitle();
        String[] parts = title.split("(?i)\\s+(vs\\.?|versus|against)\\s+");
        String a = parts.length > 0 ? parts[0] : "Option A";
        String b = parts.length > 1 ? parts[1] : "Option B";

        return aiService.generateComparison(a, b, task.getInputPayload());
    }
}
