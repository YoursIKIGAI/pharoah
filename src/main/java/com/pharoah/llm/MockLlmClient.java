package com.pharoah.llm;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Offline simulation LLM client used exclusively when --mock is passed.
 * Explicitly labels all generated outputs with [MOCK] to avoid confusing
 * mock text with real model completions.
 */
public class MockLlmClient implements LlmClient {

    private final String simulatedModel;

    public MockLlmClient(String simulatedModel) {
        this.simulatedModel = (simulatedModel != null) ? simulatedModel : "mock-llama3.1:8b";
    }

    public MockLlmClient() {
        this("mock-llama3.1:8b");
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) throws LlmException {
        // Planner prompt detection
        if (systemPrompt != null && (systemPrompt.contains("\"agents\"") || systemPrompt.contains("planner"))) {
            return generateMockPlan(userPrompt);
        }

        // Reviewer prompt detection
        if (systemPrompt != null && (systemPrompt.contains("Reviewer") || systemPrompt.contains("critique") || systemPrompt.contains("verdict"))) {
            return generateMockReview(userPrompt);
        }

        // Standard agent generation
        return generateMockAgentOutput(systemPrompt, userPrompt);
    }

    private String generateMockPlan(String userPrompt) {
        String topic = (userPrompt != null && !userPrompt.isBlank()) ? userPrompt : "General Research and Technical Inquiry";

        JSONObject root = new JSONObject();
        JSONArray agents = new JSONArray();

        JSONObject a1 = new JSONObject();
        a1.put("id", "a1");
        a1.put("name", "Literature Researcher");
        a1.put("role", "researcher");
        a1.put("task", "Conduct academic literature review and gather foundational citations on: " + topic);
        a1.put("priority", "HIGH");
        a1.put("dependsOn", new JSONArray());
        agents.put(a1);

        JSONObject a2 = new JSONObject();
        a2.put("id", "a2");
        a2.put("name", "Technical Analyst");
        a2.put("role", "analyst");
        a2.put("task", "Analyze core methodologies, empirical paradigms, and performance metrics for: " + topic);
        a2.put("priority", "HIGH");
        a2.put("dependsOn", new JSONArray().put("a1"));
        agents.put(a2);

        JSONObject a3 = new JSONObject();
        a3.put("id", "a3");
        a3.put("name", "Synthesis Writer");
        a3.put("role", "writer");
        a3.put("task", "Synthesize findings into cohesive technical exposition and implementation strategies for: " + topic);
        a3.put("priority", "MEDIUM");
        a3.put("dependsOn", new JSONArray().put("a2"));
        agents.put(a3);

        JSONObject a4 = new JSONObject();
        a4.put("id", "a4");
        a4.put("name", "Critical Examiner");
        a4.put("role", "critic");
        a4.put("task", "Examine limitations, ethical implications, security vulnerabilities, and open challenges for: " + topic);
        a4.put("priority", "LOW");
        a4.put("dependsOn", new JSONArray().put("a3"));
        agents.put(a4);

        root.put("agents", agents);
        return root.toString(2);
    }

    private String generateMockReview(String userPrompt) {
        return """
            [MOCK] VERDICT: APPROVED WITH MINOR OBSERVATIONS
            
            Critique:
            The synthesized analysis demonstrates robust conceptual coherence and rigorous thematic progression across all stages.
            The literature survey establishes strong theoretical foundations, while the analytical breakdown provides clear distinctions
            between contemporary architectural models. The critical evaluation appropriately surfaces operational bottlenecks and security
            trade-offs. Recommended enhancement: deepen the discussion on edge deployment overheads in future revisions.
            """;
    }

    private String generateMockAgentOutput(String systemPrompt, String userPrompt) {
        return String.format("""
            [MOCK] Synthesis Output
            
            This report addresses the requested task based on foundational literature and empirical paradigms:
            %s
            
            Key Insights and Formulations:
            1. Conceptual Scoping: Foundational models establish deterministic invariants across multi-tier orchestrations.
            2. Empirical Evaluation: Decentralized processing pathways reduce systemic contention by up to 34%% under peak load.
            3. Operational Invariants: Upstream context integration ensures unbroken narrative and technical continuity.
            
            Conclusion:
            All core analytical parameters have been evaluated and verified against target specifications.
            """, (userPrompt != null && !userPrompt.isBlank() ? userPrompt : "General technical analysis."));
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckResult.healthy("Mock Simulator", simulatedModel);
    }

    @Override
    public String getModelName() {
        return simulatedModel;
    }

    @Override
    public boolean isMock() {
        return true;
    }
}
