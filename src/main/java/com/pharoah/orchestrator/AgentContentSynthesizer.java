package com.pharoah.orchestrator;

import java.util.List;

/**
 * Fallback content synthesizer for PHAROAH agents in simulation/mock mode.
 * Labels all outputs with [MOCK] to strictly separate mock text from genuine LLM completions.
 */
public class AgentContentSynthesizer {

    public static String synthesize(AgentDeployment deployment, String upstreamContext) {
        AgentKind kind = deployment.getKind();
        String topic = deployment.getResponsibility();
        List<String> subTopics = deployment.getSubTopics();

        return generateDynamicSubTopicProse(deployment, kind, topic, subTopics, upstreamContext);
    }

    private static String generateDynamicSubTopicProse(AgentDeployment deployment,
                                                       AgentKind kind,
                                                       String topic,
                                                       List<String> subTopics,
                                                       String upstreamContext) {

        StringBuilder sb = new StringBuilder();
        String cleanTopic = (topic != null && !topic.isBlank()) ? topic.trim() : "Domain Analysis";

        sb.append("[MOCK] Synthetic Evaluation Output\n\n");

        for (int i = 0; i < subTopics.size(); i++) {
            String subTopic = subTopics.get(i);
            sb.append("Sub-topic ").append(i + 1).append(": ").append(subTopic).append("\n\n");

            String paragraph = composeDynamicParagraph(kind, cleanTopic, subTopic, i + 1, upstreamContext);
            sb.append(paragraph).append("\n\n");
        }

        return sb.toString().trim();
    }

    private static String composeDynamicParagraph(AgentKind kind,
                                                  String topic,
                                                  String subTopic,
                                                  int subIndex,
                                                  String upstreamContext) {

        StringBuilder sb = new StringBuilder();

        switch (subIndex) {
            case 1 -> {
                sb.append("The theoretical examination of ").append(topic)
                  .append(" establishes a rigorous conceptual framework anchored in foundational literature. ")
                  .append("Scholars across this domain emphasize that understanding the operational dynamics requires decomposing the subject into formal mathematical formulations and behavioral assumptions. ")
                  .append("Recent academic discourse highlights that conventional heuristic approximations are being replaced by provable invariants and modular abstractions, which drastically enhance systemic fault isolation. ")
                  .append("By grounding the inquiry in formal principles, modern researchers demonstrate that system reliability and predictable state evolution are direct consequences of disciplined theoretical scoping.");
            }
            case 2 -> {
                sb.append("Methodologically, investigating ").append(topic)
                  .append(" demands reproducible experimental protocols and controlled hypothesis evaluation. ")
                  .append("Standardized benchmarking across empirical test suites reveals that decoupled execution pipelines achieve measurable latency reductions while maintaining strict data integrity. ")
                  .append("Furthermore, contemporary validation models integrate ablation studies to isolate individual algorithmic components, verifying that observed throughput gains stem directly from core design innovations rather than transient environment anomalies. ")
                  .append("Academic integrity is maintained by adhering to transparent parameter reporting and standardized data collection procedures.");
            }
            default -> {
                sb.append("Scholarly consensus regarding ").append(topic)
                  .append(" demonstrates emerging convergence alongside notable unresolved research frontiers. ")
                  .append("While empirical benchmarks confirm substantial performance improvements across standard operational parameters, researchers cite persistent vulnerabilities when operating under extreme contention or out-of-distribution drift. ")
                  .append("Future literature directions prioritize formal verification frameworks, verifiable cross-disciplinary citations, and reproducible validation corpora. ")
                  .append("Bridging these documented research gaps will be essential for transitioning theoretical formulations into dependable, production-grade applications.");
            }
        }

        if (upstreamContext != null && !upstreamContext.isBlank() && !upstreamContext.startsWith("No prior")) {
            sb.append("\n\nIncorporating synthesized findings from prerequisite upstream agents, this analysis confirms full alignment with preceding findings, guaranteeing unbroken narrative and factual continuity across workflow boundaries.");
        }

        return sb.toString();
    }
}
