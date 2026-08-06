package com.jaywant.resumeanalyzer.domain;

import java.util.List;

public record AnalysisResult(
        int matchScore,
        List<String> matchedSkills,
        List<String> missingSkills,
        String experienceFit,
        List<String> topSuggestions,
        List<String> atsKeywordsFound,
        List<String> atsKeywordsMissing) {
}
