package com.pharoah.llm;

import com.pharoah.config.AppConfig;

/**
 * Factory for instantiating the active LlmClient with startup health checking.
 * Guarantees NO silent fallback to fake text when running in standard mode.
 */
public class LlmClientFactory {

    public static LlmClient createClient(boolean mockRequested) {
        AppConfig config = AppConfig.getInstance();

        if (mockRequested || "mock".equalsIgnoreCase(config.getLlmProvider())) {
            System.out.println("[LLM] Running in MOCK SIMULATION mode (--mock). All outputs will be labeled [MOCK].");
            return new MockLlmClient(config.getLlmModel());
        }

        // Standard Ollama execution
        OllamaClient client = new OllamaClient(
                config.getLlmBaseUrl(),
                config.getLlmModel(),
                config.getLlmTimeoutSeconds(),
                config.getLlmMaxConcurrent()
        );

        LlmClient.HealthCheckResult health = client.checkHealth();
        if (!health.isHealthy()) {
            System.err.println("\n" + "=".repeat(76));
            System.err.println("[LLM STARTUP ERROR] Ollama is not ready!");
            System.err.println("-".repeat(76));
            System.err.println(health.errorMessage());
            System.err.println("\nInstructions to resolve:");
            System.err.println("  1. Ensure Ollama is running: Check http://localhost:11434");
            System.err.println("  2. Pull the required model:  ollama pull " + config.getLlmModel());
            System.err.println("  3. Or run with mock data:    --mock");
            System.err.println("=".repeat(76) + "\n");
            System.exit(1);
        }

        System.out.printf("[LLM] Connected to Ollama (%s) on %s [Max Concurrent: %d]\n",
                client.getModelName(), client.getBaseUrl(), config.getLlmMaxConcurrent());
        return client;
    }
}
