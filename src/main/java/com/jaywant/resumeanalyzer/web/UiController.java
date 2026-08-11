package com.jaywant.resumeanalyzer.web;

import com.jaywant.resumeanalyzer.domain.AnalysisResult;
import com.jaywant.resumeanalyzer.service.AnalysisService;
import com.jaywant.resumeanalyzer.service.DocumentService;
import com.jaywant.resumeanalyzer.service.JobDescriptionScrapeException;
import com.jaywant.resumeanalyzer.service.JobDescriptionScraperService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Controller
@RequiredArgsConstructor
public class UiController {

    static final String SESSION_RESUME_TEXT = "ui.resumeText";
    static final String SESSION_RESUME_NAME = "ui.resumeFileName";

    private final DocumentService documentService;
    private final AnalysisService analysisService;
    private final JobDescriptionScraperService jobDescriptionScraperService;

    @GetMapping("/")
    public String index(Model model, HttpSession session) {
        applySavedResume(model, session);
        return "index";
    }

    @PostMapping("/analyze-ui")
    public String analyze(
            @RequestParam(value = "resume", required = false) MultipartFile resume,
            @RequestParam(value = "jobDescription", required = false) String jobDescription,
            @RequestParam(value = "jobUrl", required = false) String jobUrl,
            Model model,
            HttpSession session) {
        model.addAttribute("jobDescription", jobDescription);
        model.addAttribute("jobUrl", jobUrl);

        try {
            String resumeText = resolveResumeText(resume, session);
            String resumeFileName = (String) session.getAttribute(SESSION_RESUME_NAME);
            applySavedResume(model, session);

            String jd = resolveJobDescription(jobDescription, jobUrl);
            AnalysisResult result = analysisService.analyze(resumeText, jd);
            model.addAttribute("result", result);
            model.addAttribute("resumeFileName", resumeFileName);
        }
        catch (IllegalArgumentException | JobDescriptionScrapeException ex) {
            applySavedResume(model, session);
            model.addAttribute("error", ex.getMessage());
        }
        catch (Exception ex) {
            log.error("UI analyze failed", ex);
            applySavedResume(model, session);
            model.addAttribute("error",
                    "Analysis failed. Check that Ollama is running with llama3.2 and nomic-embed-text.");
        }
        return "index";
    }

    private String resolveResumeText(MultipartFile resume, HttpSession session) {
        if (resume != null && !resume.isEmpty()) {
            String text = documentService.extractText(resume);
            session.setAttribute(SESSION_RESUME_TEXT, text);
            session.setAttribute(SESSION_RESUME_NAME, resume.getOriginalFilename());
            return text;
        }
        Object saved = session.getAttribute(SESSION_RESUME_TEXT);
        if (saved instanceof String text && !text.isBlank()) {
            return text;
        }
        throw new IllegalArgumentException("Upload a resume file to get started.");
    }

    private void applySavedResume(Model model, HttpSession session) {
        Object name = session.getAttribute(SESSION_RESUME_NAME);
        Object text = session.getAttribute(SESSION_RESUME_TEXT);
        boolean saved = name instanceof String && text instanceof String;
        model.addAttribute("hasSavedResume", saved);
        if (saved) {
            model.addAttribute("resumeFileName", name);
        }
    }

    private String resolveJobDescription(String jobDescription, String jobUrl) {
        boolean hasJd = jobDescription != null && !jobDescription.isBlank();
        boolean hasUrl = jobUrl != null && !jobUrl.isBlank();
        if (!hasJd && !hasUrl) {
            throw new IllegalArgumentException("Paste a job description or provide a job posting URL.");
        }
        if (hasUrl) {
            return jobDescriptionScraperService.scrape(jobUrl.strip());
        }
        return jobDescription.strip();
    }
}
