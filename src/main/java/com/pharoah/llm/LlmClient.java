package com.pharoah.llm;

/**
 * Universal interface for Large Language Model communication.
 */
public interface LlmClient {

    /**
     * Executes a chat completion with a system instruction and user prompt.
     *
     * @param systemPrompt The guiding persona, constraints, and instructions.
     * @param userPrompt   The task input and contextual data.
     * @return Raw model completion text.
     * @throws LlmException If communication fails or times out.
     */
    String chat(String systemPrompt, String userPrompt) throws LlmException;

    /**
     * Performs a health check against the backing LLM provider.
     */
    HealthCheckResult checkHealth();

    /**
     * Returns the name of the model in use (e.g. "llama3.1:8b" or "mock-simulator").
     */
    String getModelName();

    /**
     * Returns whether this client is running in offline simulation/mock mode.
     */
    boolean isMock();

    /**
     * Represents the outcome of an LLM provider health check.
     */
    record HealthCheckResult(boolean isHealthy, String provider, String model, String errorMessage) {
        public static HealthCheckResult healthy(String provider, String model) {
            return new HealthCheckResult(true, provider, model, null);
        }

        public static HealthCheckResult unhealthy(String provider, String model, String error) {
            return new HealthCheckResult(false, provider, model, error);
        }
    }
}
