package com.pharoah.agents.impl;

import com.pharoah.agents.AbstractAgent;
import com.pharoah.ai.AIService;
import com.pharoah.core.enums.TaskType;
import com.pharoah.core.model.Task;
import com.pharoah.knowledge.SubjectMemory;

/**
 * Specialized agent for fact-checking, citation checking, and claim validation.
 */
public class VerificationAgent extends AbstractAgent {

    public VerificationAgent(AIService aiService) {
        super("VerificationAgent", "Cross-checks facts, claims, and statistical assertions against known context.", aiService);
    }

    @Override
    public boolean canHandle(TaskType taskType) {
        return taskType == TaskType.VERIFICATION;
    }

    @Override
    protected String doExecute(Task task, SubjectMemory memory, String contextSummary) {
        return aiService.verifyClaims(task.getTitle(), contextSummary + "\n" + task.getInputPayload());
    }
}
