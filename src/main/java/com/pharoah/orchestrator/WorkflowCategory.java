package com.pharoah.orchestrator;

import com.pharoah.core.enums.Priority;

import java.util.ArrayList;
import java.util.List;

// VARIJ: Supported writing and research workflow categories in PHAROAH.
// Generates category-specific multi-agent deployment plans with realistic dependency graphs.
public enum WorkflowCategory {
    RESEARCH_PAPER("Research Paper", "Academic research paper conforming to IEEE/ACM structure with literature and methodology"),
    CREATIVE_WRITING("Creative Writing", "Immersive creative narrative with character design, world-building, and dialogue"),
    TECHNICAL_DOCUMENTATION("Technical Documentation", "Engineering documentation covering architecture, code specs, and API references"),
    BUSINESS_REPORT("Business Report", "Strategic business report with market problem, financial forecast, and governance"),
    CUSTOM_WORKFLOW("Custom Workflow", "User-defined multi-agent deployment with custom responsibilities and dependency links");

    private final String displayName;
    private final String description;

    WorkflowCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    // Proposes category-specific agent deployment plan
    public List<AgentDeployment> createProposedDeployment(String topic) {
        List<AgentDeployment> list = new ArrayList<>();

        switch (this) {
            case RESEARCH_PAPER -> {
                list.add(new AgentDeployment("agent-01", "Literature Research Agent", "RESEARCH_SPECIALIST",
                        "Literature review, academic foundations, and citations for " + topic, Priority.HIGH));
                list.add(new AgentDeployment("agent-02", "Data Collection Agent", "DATA_ANALYST",
                        "Empirical evidence, metrics, and dataset synthesis for " + topic, Priority.HIGH));
                list.add(new AgentDeployment("agent-03", "Methodology Agent", "METHODOLOGY_ARCHITECT",
                        "Experimental setup, hypothesis formulation, and analytical framework", Priority.MEDIUM, List.of("agent-01", "agent-02")));
                list.add(new AgentDeployment("agent-04", "Introduction Agent", "TECHNICAL_WRITER",
                        "Introductory scope, motivation, and problem statement for " + topic, Priority.MEDIUM));
                list.add(new AgentDeployment("agent-05", "Results & Discussion Agent", "ANALYSIS_SPECIALIST",
                        "Critical discussion, comparative benchmarks, and research gap resolution", Priority.MEDIUM, List.of("agent-02", "agent-03")));
                list.add(new AgentDeployment("agent-06", "Review & Formatting Agent", "QUALITY_REVIEWER",
                        "Consolidated bibliography, reference citations, and format compliance", Priority.LOW, List.of("agent-01", "agent-04", "agent-05")));
            }
            case CREATIVE_WRITING -> {
                list.add(new AgentDeployment("agent-01", "World-Building & Lore Agent", "CONCEPT_DEVELOPER",
                        "Establish universe rules, atmospheric tone, and environmental lore for " + topic, Priority.HIGH));
                list.add(new AgentDeployment("agent-02", "Character Conception Agent", "CHARACTER_DEVELOPER",
                        "Develop protagonist profiles, motivations, internal conflicts, and foils", Priority.HIGH));
                list.add(new AgentDeployment("agent-03", "Plot Development Agent", "PLOT_ARCHITECT",
                        "Outline rising action, pivotal crisis points, and climax", Priority.MEDIUM, List.of("agent-01", "agent-02")));
                list.add(new AgentDeployment("agent-04", "Scene & Dialogue Agent", "SCENE_WRITER",
                        "Compose dialogue beats, tension pacing, and character interactions", Priority.MEDIUM, List.of("agent-03")));
                list.add(new AgentDeployment("agent-05", "Style & Editorial Agent", "STYLE_REVIEWER",
                        "Polishing narrative voice, rhythm, figurative language, and continuity", Priority.LOW, List.of("agent-04")));
            }
            case TECHNICAL_DOCUMENTATION -> {
                list.add(new AgentDeployment("agent-01", "Requirement Analysis Agent", "REQUIREMENTS_ENGINEER",
                        "Scope functional specifications and system constraints for " + topic, Priority.HIGH));
                list.add(new AgentDeployment("agent-02", "Architecture Design Agent", "SYSTEM_ARCHITECT",
                        "Component breakdown, dependency matrices, and data flow models", Priority.HIGH, List.of("agent-01")));
                list.add(new AgentDeployment("agent-03", "Code Specification Agent", "SOFTWARE_ENGINEER",
                        "Implementation patterns, interface contracts, and code snippets", Priority.MEDIUM, List.of("agent-02")));
                list.add(new AgentDeployment("agent-04", "API Documentation Agent", "API_SPECIALIST",
                        "Endpoint definitions, payload schemas, and error code tables", Priority.MEDIUM, List.of("agent-02")));
                list.add(new AgentDeployment("agent-05", "Audit & Formatting Agent", "QUALITY_REVIEWER",
                        "Cross-reference specifications, style guidelines, and setup instructions", Priority.LOW, List.of("agent-03", "agent-04")));
            }
            case BUSINESS_REPORT -> {
                list.add(new AgentDeployment("agent-01", "Executive Summary Agent", "BUSINESS_STRATEGIST",
                        "Executive problem overview, market drivers, and strategic intent for " + topic, Priority.HIGH));
                list.add(new AgentDeployment("agent-02", "Market Research Agent", "MARKET_ANALYST",
                        "Competitive landscape, addressable market size, and customer friction", Priority.HIGH));
                list.add(new AgentDeployment("agent-03", "Solution Strategy Agent", "SOLUTION_ARCHITECT",
                        "Proposed business model, core value proposition, and operational rollout", Priority.HIGH, List.of("agent-02")));
                list.add(new AgentDeployment("agent-04", "Financial Modeling Agent", "FINANCIAL_ANALYST",
                        "Cost structure, 3-year revenue forecast, break-even, and ROI models", Priority.MEDIUM, List.of("agent-03")));
                list.add(new AgentDeployment("agent-05", "Governance & Risk Agent", "RISK_AUDITOR",
                        "Regulatory compliance, mitigation strategies, and executive sign-off", Priority.LOW, List.of("agent-03", "agent-04")));
            }
            case CUSTOM_WORKFLOW -> {
                list.add(new AgentDeployment("agent-01", "Domain Investigation Agent", "RESEARCH_SPECIALIST",
                        "Investigate core concepts and foundational elements for " + topic, Priority.HIGH));
                list.add(new AgentDeployment("agent-02", "Analytical Deconstruction Agent", "ANALYSIS_SPECIALIST",
                        "Deconstruct problem space, evaluate trade-offs, and compare alternatives", Priority.HIGH));
                list.add(new AgentDeployment("agent-03", "Synthesis & Synthesis Agent", "TECHNICAL_WRITER",
                        "Draft main findings, recommendations, and strategic deliverables", Priority.MEDIUM, List.of("agent-01", "agent-02")));
                list.add(new AgentDeployment("agent-04", "Quality Audit Agent", "QUALITY_REVIEWER",
                        "Verify accuracy, enforce consistency, and compile final artifact", Priority.LOW, List.of("agent-03")));
            }
        }

        return list;
    }
}
