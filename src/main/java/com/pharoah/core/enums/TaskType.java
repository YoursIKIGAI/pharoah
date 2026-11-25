package com.pharoah.core.enums;

/**
 * Defines the intellectual task categories supported by PHAROAH.
 * Each type maps to the specialized agent best suited for handling it.
 */
public enum TaskType {
    RESEARCH("Topic Research & Literature Review", "ResearchAgent"),
    WRITING("Essay, Report & Content Generation", "WriterAgent"),
    COMPARISON("Technology & Concept Comparison", "ComparatorAgent"),
    VERIFICATION("Fact-Checking & Claim Verification", "VerificationAgent"),
    DOCUMENT_ANALYSIS("Document & PDF Intelligence", "DocumentAnalystAgent"),
    STUDY_ASSISTANCE("Concepts, Notes & MCQ Generation", "StudyAgent"),
    DATA_ANALYSIS("Dataset, CSV & Statistical Analysis", "DataAnalystAgent"),
    PLANNING("Task Breakdown & Scheduling", "PlannerAgent"),
    CODING("Code Review, Bug Fixing & Explanation", "CodingAgent");

    private final String description;
    private final String primaryAgentName;

    TaskType(String description, String primaryAgentName) {
        this.description = description;
        this.primaryAgentName = primaryAgentName;
    }

    public String getDescription() {
        return description;
    }

    public String getPrimaryAgentName() {
        return primaryAgentName;
    }
}
