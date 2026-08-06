package com.jaywant.resumeanalyzer.service;

import com.jaywant.resumeanalyzer.ai.PromptService;
import com.jaywant.resumeanalyzer.config.AppProperties;
import com.jaywant.resumeanalyzer.domain.AnalysisResult;
import com.jaywant.resumeanalyzer.parser.TextTruncator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final ChatClient.Builder chatClientBuilder;
    private final PromptService promptService;
    private final RagService ragService;
    private final AppProperties appProperties;
    private final AtsKeywordService atsKeywordService;

    public AnalysisResult analyze(String resumeText, String jobDescription) {
        String resume = TextTruncator.truncate(resumeText, appProperties.getResumeCharLimit());
        String job = TextTruncator.truncate(jobDescription, appProperties.getJobDescriptionCharLimit());
        String ragContext = ragService.buildContext(resume, job);

        BeanOutputConverter<AnalysisResult> converter = new BeanOutputConverter<>(AnalysisResult.class);
        String template = promptService.loadPrompt("analyze-v1.st");
        String prompt = promptService.render(template, Map.of(
                "resume", resume,
                "jobDescription", job,
                "ragContext", ragContext.isBlank() ? "No additional context retrieved." : ragContext,
                "format", converter.getFormat()));

        AnalysisResult result = chatClientBuilder.build()
                .prompt(prompt)
                .call()
                .entity(AnalysisResult.class);

        List<String> jobKeywords = atsKeywordService.extractKeywords(job);
        List<String> found = atsKeywordService.findPresentKeywords(resume, jobKeywords);
        List<String> missing = atsKeywordService.findMissingKeywords(resume, jobKeywords);

        return new AnalysisResult(
                clampScore(result.matchScore()),
                safeList(result.matchedSkills()),
                safeList(result.missingSkills()),
                result.experienceFit(),
                safeList(result.topSuggestions()),
                found,
                missing);
    }

    private int clampScore(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }
}
