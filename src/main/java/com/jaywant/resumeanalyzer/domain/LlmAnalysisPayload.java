package com.jaywant.resumeanalyzer.domain;

import java.util.List;

/**
 * Structured fields produced by the LLM. ATS keywords and RAG citations are filled in code.
 */
public record LlmAnalysisPayload(
        int matchScore,
        List<String> matchedSkills,
        List<String> missingSkills,
        String experienceFit,
        List<String> topSuggestions) {
}
