package com.jaywant.resumeanalyzer.ai;

import com.jaywant.resumeanalyzer.service.AtsKeywordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Deterministic helpers the LLM can invoke during analysis (function calling).
 * Tools run Java logic; the model decides when to call them.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisTools {

    private static final Map<String, String> CANONICAL = Map.ofEntries(
            Map.entry("js", "JavaScript"),
            Map.entry("javascript", "JavaScript"),
            Map.entry("ts", "TypeScript"),
            Map.entry("typescript", "TypeScript"),
            Map.entry("react", "React"),
            Map.entry("react.js", "React"),
            Map.entry("reactjs", "React"),
            Map.entry("node", "Node.js"),
            Map.entry("nodejs", "Node.js"),
            Map.entry("node.js", "Node.js"),
            Map.entry("springboot", "Spring Boot"),
            Map.entry("spring boot", "Spring Boot"),
            Map.entry("spring-boot", "Spring Boot"),
            Map.entry("k8s", "Kubernetes"),
            Map.entry("kubernetes", "Kubernetes"),
            Map.entry("postgres", "PostgreSQL"),
            Map.entry("postgresql", "PostgreSQL"),
            Map.entry("mongo", "MongoDB"),
            Map.entry("mongodb", "MongoDB"),
            Map.entry("aws", "AWS"),
            Map.entry("gcp", "GCP"),
            Map.entry("ci/cd", "CI/CD"),
            Map.entry("rest api", "REST APIs"),
            Map.entry("rest apis", "REST APIs"),
            Map.entry("rest", "REST APIs"));

    private final AtsKeywordService atsKeywordService;

    @Tool(description = "Extract likely technical skills and keywords from resume or job-description text. "
            + "Call this before listing matchedSkills or missingSkills.")
    public String extractSkills(
            @ToolParam(description = "Raw resume or job description text") String text) {
        List<String> skills = atsKeywordService.extractKeywords(text);
        log.info("Tool extractSkills returned {} keyword(s)", skills.size());
        if (skills.isEmpty()) {
            return "No technical keywords found.";
        }
        return "skills=[" + String.join(", ", skills) + "]";
    }

    @Tool(description = "Score ATS keyword coverage between a resume and a job description. "
            + "Returns coverage percent plus found and missing keyword lists. "
            + "Call this to ground matchScore in deterministic keyword overlap.")
    public String scoreAtsKeywords(
            @ToolParam(description = "Full resume text") String resumeText,
            @ToolParam(description = "Full job description text") String jobDescription) {
        List<String> keywords = atsKeywordService.extractKeywords(jobDescription);
        List<String> found = atsKeywordService.findPresentKeywords(resumeText, keywords);
        List<String> missing = atsKeywordService.findMissingKeywords(resumeText, keywords);
        int coverage = keywords.isEmpty() ? 0 : (found.size() * 100) / keywords.size();
        log.info("Tool scoreAtsKeywords coverage={}%% (found={}, missing={})",
                coverage, found.size(), missing.size());
        return "coveragePercent=" + coverage
                + "; found=[" + String.join(", ", found) + "]"
                + "; missing=[" + String.join(", ", missing) + "]";
    }

    @Tool(description = "Normalize a skill name to a canonical label "
            + "(e.g. react.js -> React, node.js -> Node.js, k8s -> Kubernetes).")
    public String normalizeSkill(
            @ToolParam(description = "Raw skill string to normalize") String skill) {
        if (skill == null || skill.isBlank()) {
            return "";
        }
        String trimmed = skill.strip();
        String key = trimmed.toLowerCase(Locale.ROOT);
        String canonical = CANONICAL.getOrDefault(key, titleCasePreserveTech(trimmed));
        log.debug("Tool normalizeSkill '{}' -> '{}'", trimmed, canonical);
        return canonical;
    }

    /**
     * Normalizes a list of skills (used by tests / future batch tools).
     */
    public List<String> normalizeSkills(List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return List.of();
        }
        Set<String> out = new LinkedHashSet<>();
        for (String skill : skills) {
            String n = normalizeSkill(skill);
            if (!n.isBlank()) {
                out.add(n);
            }
        }
        return List.copyOf(out);
    }

    private static String titleCasePreserveTech(String value) {
        // Keep short all-caps tokens (AWS, SQL, JPA) as-is when already uppercase-ish.
        if (value.length() <= 4 && value.equals(value.toUpperCase(Locale.ROOT))) {
            return value.toUpperCase(Locale.ROOT);
        }
        return value.chars()
                .mapToObj(c -> String.valueOf((char) c))
                .collect(Collectors.joining())
                .replaceAll("\\s+", " ")
                .strip();
    }
}
