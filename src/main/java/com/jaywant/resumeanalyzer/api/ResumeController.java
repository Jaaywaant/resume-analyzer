package com.jaywant.resumeanalyzer.api;

import com.jaywant.resumeanalyzer.domain.ResumeReviewResult;
import com.jaywant.resumeanalyzer.service.DocumentService;
import com.jaywant.resumeanalyzer.service.ResumeReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/resume")
@RequiredArgsConstructor
@Tag(name = "Resume Review")
public class ResumeController {

    private final DocumentService documentService;
    private final ResumeReviewService resumeReviewService;

    @PostMapping(value = "/review", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Review a resume without a job description")
    public ResumeReviewResult review(@RequestPart("resume") MultipartFile resume) {
        String resumeText = documentService.extractText(resume);
        return resumeReviewService.review(resumeText);
    }
}
