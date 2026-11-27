package com.pharoah.llm;

/**
 * Checked exception thrown when LLM interaction fails (network, model error, invalid response).
 */
public class LlmException extends Exception {

    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
