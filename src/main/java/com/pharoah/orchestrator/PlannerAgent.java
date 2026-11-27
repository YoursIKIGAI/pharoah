package com.pharoah.orchestrator;

import com.pharoah.core.enums.Priority;
import com.pharoah.llm.LlmClient;
import com.pharoah.llm.LlmException;
import com.pharoah.llm.PromptLoader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Intelligent planning agent that decomposes a free-form user prompt into
 * a validated Directed Acyclic Graph (DAG) of specialized agents.
 */
public class PlannerAgent {

    private final LlmClient llmClient;

    public PlannerAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public WorkflowPlan plan(String userPrompt, int maxWorkers, ExecutionMode executionMode) throws LlmException {
        String systemPrompt = PromptLoader.loadPrompt("planner");
        String effectivePrompt = (userPrompt != null && !userPrompt.isBlank()) ? userPrompt.trim() : "Investigate the impact of artificial intelligence on cybersecurity.";

        String rawResponse = llmClient.chat(systemPrompt, effectivePrompt);
        JSONObject planJson = null;

        try {
            planJson = extractAndParseJson(rawResponse);
        } catch (Exception firstError) {
            // Retry once with error feedback as requested in Step 3
            String retryPrompt = effectivePrompt + "\n\nCRITICAL: Your previous response was invalid JSON: " 
                    + firstError.getMessage() + ". Return ONLY a raw JSON object matching the requested schema.";
            String retryResponse = llmClient.chat(systemPrompt, retryPrompt);
            try {
                planJson = extractAndParseJson(retryResponse);
            } catch (Exception secondError) {
                throw new LlmException("Planner failed to produce valid JSON plan after retry. Response was: " + retryResponse, secondError);
            }
        }

        JSONArray agentsArray = planJson.optJSONArray("agents");
        if (agentsArray == null || agentsArray.isEmpty()) {
            throw new LlmException("Planner JSON did not contain any 'agents' array.");
        }

        List<AgentDeployment> deployments = new ArrayList<>();
        for (int i = 0; i < agentsArray.length(); i++) {
            JSONObject ag = agentsArray.getJSONObject(i);
            String id = ag.optString("id", "a" + (i + 1));
            String name = ag.optString("name", "Agent " + (i + 1));
            String role = ag.optString("role", "researcher").toLowerCase();
            String task = ag.optString("task", "Conduct specialized investigation.");
            String prioStr = ag.optString("priority", "MEDIUM").toUpperCase();

            Priority priority = switch (prioStr) {
                case "HIGH" -> Priority.HIGH;
                case "LOW" -> Priority.LOW;
                default -> Priority.MEDIUM;
            };

            List<String> dependencies = new ArrayList<>();
            JSONArray depsArray = ag.optJSONArray("dependsOn");
            if (depsArray != null) {
                for (int d = 0; d < depsArray.length(); d++) {
                    dependencies.add(depsArray.getString(d));
                }
            }

            AgentKind kind = switch (role) {
                case "analyst" -> AgentKind.DATA_ENQUIRY;
                case "writer" -> AgentKind.TECHNICAL_ARCHITECTURE;
                case "critic" -> AgentKind.STRATEGIC_ANALYSIS;
                default -> AgentKind.RESEARCH_PAPER;
            };

            AgentDeployment deployment = new AgentDeployment(id, name, kind, task, priority, dependencies);
            deployment.setRole(role);
            deployments.add(deployment);
        }

        // WorkflowPlan constructor automatically validates dependencies and runs cycle detection
        try {
            return new WorkflowPlan(
                    WorkflowCategory.CUSTOM_WORKFLOW,
                    effectivePrompt,
                    deployments,
                    maxWorkers,
                    executionMode
            );
        } catch (Exception e) {
            throw new LlmException("Generated workflow plan failed validation: " + e.getMessage(), e);
        }
    }

    private JSONObject extractAndParseJson(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Empty response received from LLM");
        }

        String cleaned = text.trim();
        // Remove markdown code fences if model wrapped response in ```json ... ```
        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            int lastFence = cleaned.lastIndexOf("```");
            if (firstNewline != -1 && lastFence > firstNewline) {
                cleaned = cleaned.substring(firstNewline + 1, lastFence).trim();
            }
        }

        // Locate opening { and closing }
        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            cleaned = cleaned.substring(firstBrace, lastBrace + 1);
        }

        return new JSONObject(cleaned);
    }
}
