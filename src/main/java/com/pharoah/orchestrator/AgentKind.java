package com.pharoah.orchestrator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Specialized agent kinds in PHAROAH.
 * Each kind has distinct intellectual objectives, analytical angles,
 * and dynamically formulated sub-topics.
 */
public enum AgentKind {
    RESEARCH_PAPER("Research Paper", "Academic analysis, theoretical foundations, and methodological rigor"),
    DATA_ENQUIRY("Data Enquiry", "Quantitative datasets, empirical metrics, error margins, and statistical validation"),
    CREATIVE("Creative", "Narrative perspectives, field scenarios, stakeholder dialogues, and human impact"),
    TECHNICAL_ARCHITECTURE("Technical Architecture", "System topology, data pipeline flows, interface contracts, and concurrency"),
    STRATEGIC_ANALYSIS("Strategic Analysis", "Advantages, disadvantages, institutional trade-offs, and governance");

    private final String displayName;
    private final String description;

    AgentKind(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Dynamically generates three tailored sub-topics based on the agent's kind and specific topic.
     */
    public List<String> generateSubTopics(String topic) {
        String cleanTopic = (topic != null && !topic.isBlank()) ? topic.trim() : "Domain Analysis";
        String lower = cleanTopic.toLowerCase(Locale.ROOT);
        List<String> subTopics = new ArrayList<>();

        switch (this) {
            case RESEARCH_PAPER -> {
                subTopics.add("Theoretical Foundations and Conceptual Formulations for " + cleanTopic);
                subTopics.add("Experimental Methodology, Hypothesis Testing, and Validation Protocols");
                subTopics.add("Literature Consensus, Unresolved Research Gaps, and Formal Verification");
            }
            case DATA_ENQUIRY -> {
                subTopics.add("Dataset Sampling, Statistical Cohorts, and Data Distribution for " + cleanTopic);
                subTopics.add("Empirical Performance Metrics, Sensitivity Analysis, and Error Margins");
                subTopics.add("Quantitative Benchmark Comparisons, Throughput, and Scaling Latency");
            }
            case CREATIVE -> {
                subTopics.add("Environmental Setting, Atmospheric Context, and Scenario World-Building for " + cleanTopic);
                subTopics.add("Stakeholder Perspective, Professional Dialogue, and Field Dynamics");
                subTopics.add("Ethical Dilemmas, Human-Machine Tensions, and Narrative Resolution");
            }
            case TECHNICAL_ARCHITECTURE -> {
                subTopics.add("Component Decomposition, Data Flow Pipelines, and Structural Topology for " + cleanTopic);
                subTopics.add("Interface Contracts, Concurrency Synchronization, and State Invariants");
                subTopics.add("Fault Isolation, Resilience Mechanisms, and Operational Bottlenecks");
            }
            case STRATEGIC_ANALYSIS -> {
                subTopics.add("Strategic Value Proposition and Operational Capabilities of " + cleanTopic);
                subTopics.add("Systemic Disadvantages, Security Liabilities, and Critical Trade-Offs");
                subTopics.add("Regulatory Compliance, Risk Governance, and Future Strategic Roadmap");
            }
        }

        return subTopics;
    }
}
