package com.jaywant.resumeanalyzer.service;

/**
 * Thrown when a job posting URL cannot be fetched or yields unusable text.
 */
public class JobDescriptionScrapeException extends RuntimeException {

    public JobDescriptionScrapeException(String message) {
        super(message);
    }

    public JobDescriptionScrapeException(String message, Throwable cause) {
        super(message, cause);
    }
}
