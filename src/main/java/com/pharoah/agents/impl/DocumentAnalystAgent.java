package com.pharoah.agents.impl;

import com.pharoah.agents.AbstractAgent;
import com.pharoah.ai.AIService;
import com.pharoah.core.enums.TaskType;
import com.pharoah.core.model.Task;
import com.pharoah.knowledge.SubjectMemory;

/**
 * Specialized agent for document analysis, text extraction, and contextual Q&A.
 */
public class DocumentAnalystAgent extends AbstractAgent {

    public DocumentAnalystAgent(AIService aiService) {
        super("DocumentAnalystAgent", "Extracts key insights and answers inquiries from uploaded documents.", aiService);
    }

    @Override
    public boolean canHandle(TaskType taskType) {
        return taskType == TaskType.DOCUMENT_ANALYSIS;
    }

    @Override
    protected String doExecute(Task task, SubjectMemory memory, String contextSummary) {
        String docAnalysis = aiService.generateSummary(task.getInputPayload(), 5);
        return String.format("""
            ### 📄 Document Intelligence Report: %s
            **Analysis Mode:** Deep Contextual Extraction
            
            %s
            """, task.getTitle(), docAnalysis);
    }
}
