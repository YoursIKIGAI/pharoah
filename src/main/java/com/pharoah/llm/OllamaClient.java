package com.pharoah.llm;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Semaphore;

/**
 * Local open-source LLM client communicating with Ollama REST API.
 * Includes connection health checking, model verification, automated retries,
 * and concurrency throttling via Semaphore.
 */
public class OllamaClient implements LlmClient {

    private final String baseUrl;
    private final String model;
    private final Duration timeout;
    private final int maxRetries;
    private final Semaphore rateLimiter;
    private final HttpClient httpClient;

    public OllamaClient(String baseUrl, String model, int timeoutSeconds, int maxConcurrent, int maxRetries) {
        this.baseUrl = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl.replaceAll("/+$", "") : "http://localhost:11434";
        this.model = (model != null && !model.isBlank()) ? model : "llama3.1:8b";
        this.timeout = Duration.ofSeconds(timeoutSeconds > 0 ? timeoutSeconds : 180);
        this.maxRetries = Math.max(1, maxRetries);
        this.rateLimiter = new Semaphore(Math.max(1, maxConcurrent), true);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public OllamaClient(String baseUrl, String model, int timeoutSeconds, int maxConcurrent) {
        this(baseUrl, model, timeoutSeconds, maxConcurrent, 2);
    }

    public OllamaClient() {
        this("http://localhost:11434", "llama3.1:8b", 180, 2, 2);
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) throws LlmException {
        try {
            rateLimiter.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("Interrupted waiting for LLM execution permit", e);
        }

        try {
            JSONObject payload = new JSONObject();
            payload.put("model", model);
            payload.put("stream", false);

            JSONArray messages = new JSONArray();
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
            }
            messages.put(new JSONObject().put("role", "user").put("content", userPrompt != null ? userPrompt : ""));
            payload.put("messages", messages);

            String requestBody = payload.toString();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/chat"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(timeout)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            LlmException lastError = null;
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        JSONObject json = new JSONObject(response.body());
                        if (json.has("message") && json.getJSONObject("message").has("content")) {
                            return json.getJSONObject("message").getString("content").trim();
                        } else if (json.has("response")) {
                            return json.getString("response").trim();
                        } else {
                            throw new LlmException("Unexpected response structure from Ollama: " + response.body());
                        }
                    } else {
                        throw new LlmException("Ollama returned HTTP " + response.statusCode() + ": " + response.body());
                    }

                } catch (IOException e) {
                    lastError = new LlmException("Ollama communication failure on attempt " + attempt + "/" + maxRetries + ": " + e.getMessage(), e);
                    if (attempt < maxRetries) {
                        try {
                            Thread.sleep(1000L * attempt);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new LlmException("Interrupted during retry backoff", ie);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new LlmException("Ollama request interrupted", e);
                }
            }

            throw lastError != null ? lastError : new LlmException("Ollama request failed after " + maxRetries + " attempts");

        } finally {
            rateLimiter.release();
        }
    }

    @Override
    public HealthCheckResult checkHealth() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/tags"))
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return HealthCheckResult.unhealthy("Ollama", model, "Server responded with HTTP " + response.statusCode());
            }

            JSONObject json = new JSONObject(response.body());
            JSONArray models = json.optJSONArray("models");
            if (models == null) {
                return HealthCheckResult.unhealthy("Ollama", model, "No models array returned by Ollama");
            }

            boolean modelFound = false;
            String baseModelName = model.contains(":") ? model.substring(0, model.indexOf(':')) : model;
            for (int i = 0; i < models.length(); i++) {
                JSONObject m = models.getJSONObject(i);
                String name = m.optString("name", "");
                if (name.equalsIgnoreCase(model) || name.startsWith(baseModelName + ":")) {
                    modelFound = true;
                    break;
                }
            }

            if (!modelFound) {
                return HealthCheckResult.unhealthy("Ollama", model,
                        "Model '" + model + "' is not installed. Start Ollama and run: ollama pull " + model);
            }

            return HealthCheckResult.healthy("Ollama", model);

        } catch (Exception e) {
            return HealthCheckResult.unhealthy("Ollama", model,
                    "Cannot connect to Ollama at " + baseUrl + " (" + e.getMessage() + "). Start Ollama and run: ollama pull " + model);
        }
    }

    @Override
    public String getModelName() {
        return model;
    }

    @Override
    public boolean isMock() {
        return false;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
