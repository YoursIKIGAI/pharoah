package com.pharoah.orchestrator;

import com.pharoah.llm.LlmClient;
import com.pharoah.llm.LlmException;
import com.pharoah.llm.PromptLoader;

import java.util.Map;

/**
 * Senior Peer Reviewer agent executing real LLM verification against
 * all generated multi-agent outputs.
 */
public class ReviewerAgent {

    private final LlmClient llmClient;

    public ReviewerAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public String review(String topic, Map<String, AgentOutput> agentOutputs) throws LlmException {
        String systemPrompt = PromptLoader.loadPrompt("reviewer");

        StringBuilder sb = new StringBuilder();
        sb.append("Inquiry Topic: ").append(topic).append("\n\n");
        sb.append("Multi-Agent Generated Sections to Review:\n\n");

        for (Map.Entry<String, AgentOutput> entry : agentOutputs.entrySet()) {
            AgentOutput out = entry.getValue();
            sb.append("=== SECTION: ").append(out.getAgentName()).append(" (").append(out.getResponsibility()).append(") ===\n");
            sb.append(out.getContent()).append("\n\n");
        }

        return llmClient.chat(systemPrompt, sb.toString());
    }
}
