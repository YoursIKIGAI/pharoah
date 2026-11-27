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
            if (Boolean.getBoolean("pharoah.strict.llm") || "true".equalsIgnoreCase(System.getenv("REQUIRE_OLLAMA"))) {
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

            System.out.println("\n" + "=".repeat(78));
            System.out.println("[NOTICE] Local Ollama server is not detected on " + config.getLlmBaseUrl());
            System.out.println("  -> " + health.errorMessage());
            System.out.println("  -> To run real local open-source models (e.g. Llama 3.1, Qwen 2.5):");
            System.out.println("       1. Install Ollama: https://ollama.com");
            System.out.println("       2. Run in terminal: ollama run " + config.getLlmModel());
            System.out.println("  -> Operating with active Agent Swarm simulation and live web search tools.");
            System.out.println("=".repeat(78) + "\n");
            return new MockLlmClient(config.getLlmModel());
        }

        System.out.printf("[LLM] Connected to Ollama (%s) on %s [Max Concurrent: %d]\n",
                client.getModelName(), client.getBaseUrl(), config.getLlmMaxConcurrent());
        return client;
    }
}
