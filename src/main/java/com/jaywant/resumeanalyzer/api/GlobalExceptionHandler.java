package com.jaywant.resumeanalyzer.api;

import com.jaywant.resumeanalyzer.ai.StructuredOutputException;
import com.jaywant.resumeanalyzer.service.JobDescriptionScrapeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequest(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(JobDescriptionScrapeException.class)
    public ProblemDetail handleScrapeFailure(JobDescriptionScrapeException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY,
                ex.getMessage());
        detail.setTitle("Job posting scrape failed");
        detail.setProperty("hint",
                "Use a public job URL that returns HTML (not a login wall or heavy JS SPA). "
                        + "Or paste the JD text into POST /api/v1/analyze instead.");
        return detail;
    }

    @ExceptionHandler(StructuredOutputException.class)
    public ProblemDetail handleStructuredOutput(StructuredOutputException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage());
        detail.setTitle("Invalid AI structured output");
        detail.setProperty("hint",
                "The model returned JSON that failed schema validation even after one retry. "
                        + "Try again, or check that Ollama is running llama3.2.");
        return detail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred while processing the request");
    }
}
