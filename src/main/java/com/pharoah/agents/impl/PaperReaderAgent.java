package com.pharoah.agents.impl;

import com.pharoah.agents.AbstractAgent;
import com.pharoah.ai.AIService;
import com.pharoah.core.enums.TaskType;
import com.pharoah.core.model.Task;
import com.pharoah.knowledge.SubjectMemory;

/**
 * Specialized agent for academic research paper parsing and methodology dissection.
 */
public class PaperReaderAgent extends AbstractAgent {

    public PaperReaderAgent(AIService aiService) {
        super("PaperReaderAgent", "Extracts abstracts, methodologies, theorems, and benchmarks from academic papers.", aiService);
    }

    @Override
    public boolean canHandle(TaskType taskType) {
        return taskType == TaskType.RESEARCH || taskType == TaskType.DOCUMENT_ANALYSIS;
    }

    @Override
    protected String doExecute(Task task, SubjectMemory memory, String contextSummary) {
        return String.format("""
            ### 📑 Academic Paper Dissection: %s
            %s
            
            #### Core Theoretical Contributions
            * Formal problem abstraction and architectural proofs.
            * Rigorous evaluation matrix against baseline models.
            """, task.getTitle(), aiService.synthesizeResearch(task.getTitle(), task.getInputPayload()));
    }
}
