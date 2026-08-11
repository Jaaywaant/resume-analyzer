package com.jaywant.resumeanalyzer.api;

import com.jaywant.resumeanalyzer.domain.AnalysisResult;
import com.jaywant.resumeanalyzer.service.AnalysisService;
import com.jaywant.resumeanalyzer.service.DocumentService;
import com.jaywant.resumeanalyzer.service.JobDescriptionScraperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@Tag(name = "Analysis")
public class AnalysisController {

    private final DocumentService documentService;
    private final AnalysisService analysisService;
    private final JobDescriptionScraperService jobDescriptionScraperService;

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Analyze resume against a job description",
            description = "Returns match analysis plus RAG citations. "
                    + "Set useRag=false / useTools=false to compare retrieval and function-calling off.")
    public AnalysisResult analyze(
            @RequestPart("resume") MultipartFile resume,
            @RequestPart("jobDescription") @NotBlank String jobDescription,
            @Parameter(description = "When false, skips embedding retrieval (RAG off). Default true.")
            @RequestPart(value = "useRag", required = false) String useRag,
            @Parameter(description = "When false, skips Spring AI @Tool function calling. Default true.")
            @RequestPart(value = "useTools", required = false) String useTools) {
        String resumeText = documentService.extractText(resume);
        return analysisService.analyze(
                resumeText,
                jobDescription,
                isEnabled(useRag),
                isEnabled(useTools));
    }

    @PostMapping(value = "/analyze-from-url", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Analyze resume against a job posting URL",
            description = "Scrapes the job page with Jsoup (code), then runs the same LLM analysis pipeline. "
                    + "Hybrid AI: deterministic fetch/parse, LLM for matching and suggestions.")
    public AnalysisResult analyzeFromUrl(
            @RequestPart("resume") MultipartFile resume,
            @RequestPart("jobUrl") @NotBlank String jobUrl,
            @Parameter(description = "When false, skips embedding retrieval (RAG off). Default true.")
            @RequestPart(value = "useRag", required = false) String useRag,
            @Parameter(description = "When false, skips Spring AI @Tool function calling. Default true.")
            @RequestPart(value = "useTools", required = false) String useTools) {
        String resumeText = documentService.extractText(resume);
        String jobDescription = jobDescriptionScraperService.scrape(jobUrl);
        return analysisService.analyze(
                resumeText,
                jobDescription,
                isEnabled(useRag),
                isEnabled(useTools));
    }

    private static boolean isEnabled(String flag) {
        return flag == null || !flag.equalsIgnoreCase("false");
    }
}
