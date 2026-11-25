package com.pharoah.agents.impl;

import com.pharoah.agents.AbstractAgent;
import com.pharoah.ai.AIService;
import com.pharoah.core.enums.TaskType;
import com.pharoah.core.model.Task;
import com.pharoah.knowledge.SubjectMemory;

/**
 * Specialized agent for concept explanations, study notes, flashcards, and MCQ generation.
 */
public class StudyAgent extends AbstractAgent {

    public StudyAgent(AIService aiService) {
        super("StudyAgent", "Creates study guides, conceptual revision notes, and self-assessment MCQs.", aiService);
    }

    @Override
    public boolean canHandle(TaskType taskType) {
        return taskType == TaskType.STUDY_ASSISTANCE;
    }

    @Override
    protected String doExecute(Task task, SubjectMemory memory, String contextSummary) {
        return aiService.generateStudyAid(task.getTitle(), contextSummary + "\n" + task.getInputPayload());
    }
}
