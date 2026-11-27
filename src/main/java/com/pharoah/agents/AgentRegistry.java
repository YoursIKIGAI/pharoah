package com.pharoah.agents;

import com.pharoah.agents.impl.*;
import com.pharoah.ai.AIService;
import com.pharoah.ai.OpenSourceAIService;
import com.pharoah.llm.LlmClient;

import java.util.*;

/**
 * Central registry and directory of all specialized polymorphic agents in PHAROAH.
 * Maps operational roles and task categories to active open-source agent instances.
 */
public class AgentRegistry {

    private final Map<String, Agent> agentsByRole = new LinkedHashMap<>();
    private final Map<String, Agent> agentsByName = new LinkedHashMap<>();
    private final AIService aiService;

    public AgentRegistry(AIService aiService) {
        this.aiService = aiService;
        registerDefaultAgents();
    }

    public AgentRegistry(LlmClient llmClient) {
        this(new OpenSourceAIService(llmClient));
    }

    private void registerDefaultAgents() {
        register(new ResearchAgent(aiService), "researcher", "research", "survey", "literature");
        register(new DataAnalystAgent(aiService), "analyst", "data_analyst", "metrics", "quantitative");
        register(new WriterAgent(aiService), "writer", "synthesis", "author", "writing");
        register(new VerificationAgent(aiService), "critic", "verifier", "fact_checker", "audit");
        register(new SummariserAgent(aiService), "summariser", "summary", "briefing", "abstract");
        register(new ComparatorAgent(aiService), "comparator", "compare", "benchmark", "tradeoff");
        register(new PaperReaderAgent(aiService), "paper_reader", "academic", "deep_read");
        register(new StudyAgent(aiService), "study_agent", "tutor", "education", "study");
        register(new DocumentAnalystAgent(aiService), "document_analyst", "doc_parser", "document");
    }

    public void register(Agent agent, String primaryRole, String... aliases) {
        agentsByName.put(agent.getName().toLowerCase(), agent);
        agentsByRole.put(primaryRole.toLowerCase(), agent);
        for (String alias : aliases) {
            agentsByRole.put(alias.toLowerCase(), agent);
        }
    }

    public Agent getAgentForRole(String role) {
        if (role == null || role.isBlank()) {
            return agentsByRole.get("researcher");
        }
        String key = role.toLowerCase().trim();
        Agent found = agentsByRole.get(key);
        if (found != null) return found;

        found = agentsByName.get(key);
        if (found != null) return found;

        for (Map.Entry<String, Agent> entry : agentsByRole.entrySet()) {
            if (key.contains(entry.getKey()) || entry.getKey().contains(key)) {
                return entry.getValue();
            }
        }
        return agentsByRole.get("researcher");
    }

    public List<Agent> getAllAgents() {
        return new ArrayList<>(new LinkedHashSet<>(agentsByName.values()));
    }

    public AIService getAiService() {
        return aiService;
    }

    public void printAgentDirectory(String activeModel) {
        System.out.println("\n" + "=".repeat(84));
        System.out.println("                 PHAROAH OPEN-SOURCE AGENT SWARM DIRECTORY");
        System.out.println("=".repeat(84));
        System.out.printf("  Active Engine   : Open-Source LLM [%s] + Python smolagents Tools\n", activeModel);
        System.out.println("  Tool Stack      : DuckDuckGo Live Search, Local File Inspection, Statistical Engines");
        System.out.println("-".repeat(84));
        System.out.printf("  %-22s | %-16s | %s\n", "AGENT CLASS", "SPECIALTY ROLE", "CORE RESPONSIBILITY");
        System.out.println("-".repeat(84));

        for (Agent agent : getAllAgents()) {
            System.out.printf("  %-22s | %-16s | %s\n",
                    agent.getName(),
                    lookupPrimaryRole(agent),
                    agent.getDescription());
        }

        System.out.println("-".repeat(84));
        System.out.println("  [TOOL-AGENT] python/tool_agent.py (smolagents ToolCallingAgent + DDGS Web Search)");
        System.out.println("  [ORCHESTRATOR] PlannerAgent (DAG decomposition) + ReviewerAgent (Peer Quality Audit)");
        System.out.println("=".repeat(84) + "\n");
    }

    private String lookupPrimaryRole(Agent agent) {
        for (Map.Entry<String, Agent> entry : agentsByRole.entrySet()) {
            if (entry.getValue() == agent) {
                return entry.getKey().toUpperCase();
            }
        }
        return "SPECIALIZED";
    }
}
