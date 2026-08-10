package com.jaywant.resumeanalyzer.api;

import com.jaywant.resumeanalyzer.domain.AnalysisResult;
import com.jaywant.resumeanalyzer.service.AnalysisService;
import com.jaywant.resumeanalyzer.service.DocumentService;
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

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Analyze resume against a job description",
            description = "Returns match analysis plus RAG citations. Set useRag=false to compare without retrieval.")
    public AnalysisResult analyze(
            @RequestPart("resume") MultipartFile resume,
            @RequestPart("jobDescription") @NotBlank String jobDescription,
            @Parameter(description = "When false, skips embedding retrieval (RAG off). Default true.")
            @RequestPart(value = "useRag", required = false) String useRag) {
        String resumeText = documentService.extractText(resume);
        boolean ragEnabled = useRag == null || !useRag.equalsIgnoreCase("false");
        return analysisService.analyze(resumeText, jobDescription, ragEnabled);
    }
}
