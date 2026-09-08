package com.intellitrip.exception;

/**
 * Thrown when the Gemini API returns HTTP 429 (RESOURCE_EXHAUSTED) or rate limit errors
 * and retry attempts have been exhausted or quota is fully depleted.
 */
public class GeminiQuotaExceededException extends RuntimeException {

    public GeminiQuotaExceededException(String message) {
        super(message);
    }

    public GeminiQuotaExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}

