package com.pharoah.ai;

import com.pharoah.llm.LlmClient;
import com.pharoah.llm.PromptLoader;

/**
 * OpenSourceAIService implements AIService by routing high-level intellectual
 * synthesis requests through open-source LLMs (Ollama / Open-Source Models) using LlmClient.
 */
public class OpenSourceAIService implements AIService {

    private final LlmClient llmClient;

    public OpenSourceAIService(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public LlmClient getLlmClient() {
        return llmClient;
    }

    @Override
    public String synthesizeResearch(String topic, String context) {
        String systemPrompt = PromptLoader.loadPrompt("researcher");
        String userPrompt = "DOMAIN TOPIC: " + topic + "\n\n"
                + "CONTEXT / PREREQUISITES:\n" + (context != null ? context : "None") + "\n\n"
                + "Conduct a rigorous literature survey with authoritative citations, theoretical frameworks, and research gap identification.";
        return callModel(systemPrompt, userPrompt);
    }

    @Override
    public String generateWriting(String prompt, String style, String context) {
        String systemPrompt = PromptLoader.loadPrompt("writer");
        String userPrompt = "TARGET FOCUS: " + prompt + "\n"
                + "STYLE / FORMAT: " + (style != null ? style : "Academic & Technical") + "\n\n"
                + "UPSTREAM AGENT FINDINGS:\n" + (context != null ? context : "None") + "\n\n"
                + "Draft cohesive, logically structured technical prose synthesizing the above insights.";
        return callModel(systemPrompt, userPrompt);
    }

    @Override
    public String generateSummary(String text, int maxSentences) {
        String systemPrompt = PromptLoader.loadPrompt("reviewer");
        String userPrompt = "SOURCE TEXT:\n" + text + "\n\n"
                + "Synthesize an executive summary distilling key takeaways and critical implications in at most "
                + maxSentences + " concise, high-impact sentences.";
        return callModel(systemPrompt, userPrompt);
    }

    @Override
    public String generateComparison(String entityA, String entityB, String criteria) {
        String systemPrompt = PromptLoader.loadPrompt("analyst");
        String userPrompt = "COMPARATIVE ANALYSIS: " + entityA + " VS " + entityB + "\n\n"
                + "EVALUATION CRITERIA:\n" + (criteria != null ? criteria : "Performance, scalability, resilience, complexity") + "\n\n"
                + "Provide a multi-dimensional comparative breakdown, trade-off matrix, and architectural recommendation.";
        return callModel(systemPrompt, userPrompt);
    }

    @Override
    public String verifyClaims(String claim, String sourceContext) {
        String systemPrompt = PromptLoader.loadPrompt("critic");
        String userPrompt = "CORE ASSERTION / CLAIM:\n" + claim + "\n\n"
                + "SOURCE DATA & CONTEXT:\n" + (sourceContext != null ? sourceContext : "Unspecified context") + "\n\n"
                + "Audit this claim for empirical validity, boundary conditions, logical consistency, and potential vulnerabilities.";
        return callModel(systemPrompt, userPrompt);
    }

    @Override
    public String generateStudyAid(String concept, String context) {
        String systemPrompt = PromptLoader.loadPrompt("researcher");
        String userPrompt = "CONCEPT: " + concept + "\n\n"
                + "CONTEXT:\n" + (context != null ? context : "") + "\n\n"
                + "Construct structured conceptual flashcards, core mathematical/theoretical definitions, and self-assessment questions.";
        return callModel(systemPrompt, userPrompt);
    }

    @Override
    public String generatePlan(String projectGoal, String constraints) {
        String systemPrompt = PromptLoader.loadPrompt("planner");
        String userPrompt = "GOAL: " + projectGoal + "\n"
                + "CONSTRAINTS: " + (constraints != null ? constraints : "None") + "\n\n"
                + "Decompose this into a structured JSON execution plan of specialized agents with priorities and dependencies.";
        return callModel(systemPrompt, userPrompt);
    }

    @Override
    public String analyzeData(String dataInput) {
        String systemPrompt = PromptLoader.loadPrompt("analyst");
        String userPrompt = "DATA & METRICS INPUT:\n" + dataInput + "\n\n"
                + "Perform a quantitative and methodological analysis of this data, extracting statistical variances, benchmark comparisons, and empirical proofs.";
        return callModel(systemPrompt, userPrompt);
    }

    private String callModel(String systemPrompt, String userPrompt) {
        try {
            return llmClient.chat(systemPrompt, userPrompt);
        } catch (Exception e) {
            return "[AGENT ERROR] Open-source model invocation failed: " + e.getMessage();
        }
    }
}
