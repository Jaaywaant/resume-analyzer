package com.jaywant.resumeanalyzer.ai;

/**
 * Thrown when the LLM response cannot be parsed or fails schema/guardrail validation
 * after the allowed retry attempts.
 */
public class StructuredOutputException extends RuntimeException {

    public StructuredOutputException(String message) {
        super(message);
    }

    public StructuredOutputException(String message, Throwable cause) {
        super(message, cause);
    }
}
