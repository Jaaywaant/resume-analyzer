package com.jaywant.resumeanalyzer.service;

import com.jaywant.resumeanalyzer.ai.AnalysisOutputValidator;
import com.jaywant.resumeanalyzer.ai.PromptService;
import com.jaywant.resumeanalyzer.ai.StructuredOutputClient;
import com.jaywant.resumeanalyzer.config.AppProperties;
import com.jaywant.resumeanalyzer.domain.ResumeReviewResult;
import com.jaywant.resumeanalyzer.parser.TextTruncator;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ResumeReviewService {

    private final StructuredOutputClient structuredOutputClient;
    private final AnalysisOutputValidator analysisOutputValidator;
    private final PromptService promptService;
    private final AppProperties appProperties;

    public ResumeReviewResult review(String resumeText) {
        String resume = TextTruncator.truncate(resumeText, appProperties.getResumeCharLimit());

        BeanOutputConverter<ResumeReviewResult> converter = new BeanOutputConverter<>(ResumeReviewResult.class);
        String template = promptService.loadPrompt(appProperties.getPrompts().getReview());
        String prompt = promptService.render(template, Map.of(
                "resume", resume,
                "format", converter.getFormat()));

        ResumeReviewResult result = structuredOutputClient.generate(
                prompt,
                ResumeReviewResult.class,
                analysisOutputValidator::validateRawReviewJson,
                analysisOutputValidator::validateReview);

        return new ResumeReviewResult(
                result.overallScore(),
                safeList(result.strengths()),
                safeList(result.improvements()),
                safeList(result.redFlags()),
                safeList(result.atsTips()));
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }
}
