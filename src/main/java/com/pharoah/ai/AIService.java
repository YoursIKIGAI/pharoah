package com.pharoah.ai;

/**
 * Pluggable AI Service contract for intellectual reasoning and content synthesis.
 * Supports both Offline Algorithmic Heuristics and Remote LLM API connectors.
 */
public interface AIService {

    String synthesizeResearch(String topic, String context);

    String generateWriting(String prompt, String style, String context);

    String generateSummary(String text, int maxSentences);

    String generateComparison(String entityA, String entityB, String criteria);

    String verifyClaims(String claim, String sourceContext);

    String generateStudyAid(String concept, String context);

    String generatePlan(String projectGoal, String constraints);

    String analyzeData(String dataInput);
}
