package com.pharoah.agents;

// VARIJ: Implemented AgentRole enum.
// Defines the 7 specialized agent roles supported by PHAROAH's multi-agent swarm.
public enum AgentRole {
    RESEARCH_AGENT("Research Agent", "Investigates topics, surveys literature, and extracts academic findings."),
    WRITING_AGENT("Writing Agent", "Drafts, refines, and composes essays, technical sections, and reports."),
    ANALYSIS_AGENT("Analysis Agent", "Analyzes data, compares technologies, and detects empirical patterns."),
    CODING_AGENT("Coding Agent", "Generates, reviews, debugs, and refactors code snippets and algorithms."),
    DOCUMENTATION_AGENT("Documentation Agent", "Authors technical documentation, architecture specs, and user guides."),
    REVIEWER_AGENT("Reviewer Agent", "Evaluates section validity, fact-checks claims, and inspects quality."),
    PLANNING_AGENT("Planning Agent", "Breaks down goals into dependent tasks, schedules, and roadmaps.");

    private final String displayName;
    private final String description;

    AgentRole(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
