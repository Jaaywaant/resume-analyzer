package com.jaywant.resumeanalyzer.domain;

import java.util.List;

public record ResumeReviewResult(
        int overallScore,
        List<String> strengths,
        List<String> improvements,
        List<String> redFlags,
        List<String> atsTips) {
}
