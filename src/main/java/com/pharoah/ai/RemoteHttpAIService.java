package com.pharoah.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Cloud LLM API Client using modern Java 21 HttpClient.
 * 
 * Demonstrates:
 * 1. java.net.http.HttpClient, HttpRequest, and HttpResponse.
 * 2. Asynchronous / synchronous HTTP communication.
 * 3. Graceful fallback: If no API key is supplied or network fails,
 *    it transparently falls back to the OfflineHeuristicAIService!
 */
public class RemoteHttpAIService implements AIService {

    private final String apiKey;
    private final String endpointUrl;
    private final HttpClient httpClient;
    private final AIService fallbackService;
    private boolean warnedMissingKey = false;

    public RemoteHttpAIService(String apiKey, String endpointUrl) {
        String key = apiKey;
        if (key == null || key.isBlank()) {
            key = System.getenv("GEMINI_API_KEY");
        }
        this.apiKey = key;
        this.endpointUrl = (endpointUrl != null) ? endpointUrl : "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.fallbackService = new OfflineHeuristicAIService();
    }

    public RemoteHttpAIService() {
        this(System.getenv("PHAROAH_API_KEY"), null);
    }

    private boolean isCloudApiAvailable() {
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            return true;
        }
        if (!warnedMissingKey) {
            System.err.println("[AI WARNING] No LLM API key configured (PHAROAH_API_KEY or GEMINI_API_KEY is unset). Cleanly falling back to OfflineHeuristicAIService.");
            warnedMissingKey = true;
        }
        return false;
    }

    @Override
    public String synthesizeResearch(String topic, String context) {
        if (!isCloudApiAvailable()) {
            return fallbackService.synthesizeResearch(topic, context);
        }
        return queryCloudApi("Conduct structured academic research on: " + topic + ". Context: " + context);
    }

    @Override
    public String generateWriting(String prompt, String style, String context) {
        if (!isCloudApiAvailable()) {
            return fallbackService.generateWriting(prompt, style, context);
        }
        return queryCloudApi("Write a " + style + " composition for: " + prompt + ". Context: " + context);
    }

    @Override
    public String generateSummary(String text, int maxSentences) {
        if (!isCloudApiAvailable()) {
            return fallbackService.generateSummary(text, maxSentences);
        }
        return queryCloudApi("Summarize in " + maxSentences + " sentences: " + text);
    }

    @Override
    public String generateComparison(String entityA, String entityB, String criteria) {
        if (!isCloudApiAvailable()) {
            return fallbackService.generateComparison(entityA, entityB, criteria);
        }
        return queryCloudApi("Compare " + entityA + " vs " + entityB + " based on " + criteria);
    }

    @Override
    public String verifyClaims(String claim, String sourceContext) {
        if (!isCloudApiAvailable()) {
            return fallbackService.verifyClaims(claim, sourceContext);
        }
        return queryCloudApi("Verify the credibility of claim: " + claim + ". Source context: " + sourceContext);
    }

    @Override
    public String generateStudyAid(String concept, String context) {
        if (!isCloudApiAvailable()) {
            return fallbackService.generateStudyAid(concept, context);
        }
        return queryCloudApi("Generate study notes, flashcards, and an MCQ for: " + concept + ". Context: " + context);
    }

    @Override
    public String generatePlan(String projectGoal, String constraints) {
        if (!isCloudApiAvailable()) {
            return fallbackService.generatePlan(projectGoal, constraints);
        }
        return queryCloudApi("Break down project goal: " + projectGoal + " under constraints: " + constraints);
    }

    @Override
    public String analyzeData(String dataInput) {
        if (!isCloudApiAvailable()) {
            return fallbackService.analyzeData(dataInput);
        }
        return queryCloudApi("Analyze patterns, statistics, and findings from data: " + dataInput);
    }

    /**
     * Executes HTTP POST request using java.net.http.HttpClient.
     */
    private String queryCloudApi(String prompt) {
        try {
            String requestBody = String.format("{\"contents\": [{\"parts\": [{\"text\": \"%s\"}]}]}",
                    prompt.replace("\"", "\\\"").replace("\n", " "));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpointUrl + "?key=" + apiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return response.body();
            } else {
                System.err.printf("[AI WARNING] Cloud API returned status %d. Cleanly falling back to OfflineHeuristicAIService.\n", response.statusCode());
                return fallbackService.generateWriting(prompt, "Academic", "Fallback");
            }
        } catch (Exception e) {
            System.err.printf("[AI WARNING] Cloud API call failed: %s. Cleanly falling back to OfflineHeuristicAIService.\n", e.getMessage());
            return fallbackService.generateWriting(prompt, "Academic", "Fallback");
        }
    }
}
