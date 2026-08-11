package com.jaywant.resumeanalyzer.service;

import com.jaywant.resumeanalyzer.ai.AnalysisOutputValidator;
import com.jaywant.resumeanalyzer.ai.AnalysisTools;
import com.jaywant.resumeanalyzer.ai.PromptService;
import com.jaywant.resumeanalyzer.ai.StructuredOutputClient;
import com.jaywant.resumeanalyzer.config.AppProperties;
import com.jaywant.resumeanalyzer.domain.AnalysisResult;
import com.jaywant.resumeanalyzer.domain.LlmAnalysisPayload;
import com.jaywant.resumeanalyzer.parser.TextTruncator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private static final Pattern NON_TECH = Pattern.compile("[^a-z0-9.+#/\\s-]+");
    private static final Set<String> FILLER = Set.of(
            "and", "or", "the", "with", "for", "using", "strong", "familiarity", "experience",
            "basics", "systems", "knowledge", "skills", "design", "development", "collaboration",
            "relational", "databases", "messaging", "preferred", "required", "senior", "years",
            "streaming", "engineer", "developer", "backend", "frontend");

    private static final Set<String> TECH_TOKENS = Set.of(
            "java", "spring", "boot", "kafka", "aws", "docker", "mysql", "sql", "junit", "mockito",
            "hibernate", "jpa", "angular", "react", "node", "express", "mongodb", "typescript",
            "spark", "pyspark", "airflow", "snowflake", "bigquery", "redshift", "graphql",
            "jenkins", "gradle", "maven", "oci", "postgresql", "postgres", "redis", "kubernetes");

    private static final Set<String> CRITICAL_MISSING = Set.of(
            "react", "react.js", "node", "node.js", "express", "mongodb",
            "spark", "pyspark", "airflow", "snowflake", "bigquery", "redshift",
            "graphql", "next.js", "typescript");

    private static final Set<String> SOFT_ATS = Set.of(
            "job", "title", "skills", "experience", "development", "engineer", "developer",
            "required", "preferred", "strong", "looking", "need", "years", "team", "work");

    private static final String TOOL_GUIDANCE_ON = """
            TOOLS (optional but preferred when available):
            - Call extractSkills on the Resume and Job description before listing skills.
            - Call scoreAtsKeywords(resume, job) to ground matchScore in keyword coverage.
            - Call normalizeSkill for skill names before putting them in matchedSkills/missingSkills.
            After any tool calls, still return ONLY the required JSON as the final answer.
            """;

    private static final String TOOL_GUIDANCE_OFF =
            "Tools are disabled for this request. Rely on the Resume and Job text alone.";

    private final StructuredOutputClient structuredOutputClient;
    private final AnalysisOutputValidator analysisOutputValidator;
    private final PromptService promptService;
    private final RagService ragService;
    private final AppProperties appProperties;
    private final AtsKeywordService atsKeywordService;
    private final AnalysisTools analysisTools;

    public AnalysisResult analyze(String resumeText, String jobDescription) {
        return analyze(
                resumeText,
                jobDescription,
                appProperties.getRag().isEnabled(),
                appProperties.getTools().isEnabled());
    }

    public AnalysisResult analyze(String resumeText, String jobDescription, boolean useRag) {
        return analyze(resumeText, jobDescription, useRag, appProperties.getTools().isEnabled());
    }

    public AnalysisResult analyze(String resumeText, String jobDescription, boolean useRag, boolean useTools) {
        String resume = TextTruncator.truncate(resumeText, appProperties.getResumeCharLimit());
        String job = TextTruncator.truncate(jobDescription, appProperties.getJobDescriptionCharLimit());

        RagService.RagRetrieval retrieval = ragService.retrieve(resume, job, useRag);

        BeanOutputConverter<LlmAnalysisPayload> converter = new BeanOutputConverter<>(LlmAnalysisPayload.class);
        String promptFile = appProperties.getPrompts().getAnalyze();
        log.info("Using analyze prompt template: {} (rag={}, tools={})", promptFile, useRag, useTools);
        String template = promptService.loadPrompt(promptFile);
        String prompt = promptService.render(template, Map.of(
                "resume", resume,
                "jobDescription", job,
                "ragContext", retrieval.isEmpty() ? "No additional context retrieved." : retrieval.promptContext(),
                "toolGuidance", useTools ? TOOL_GUIDANCE_ON : TOOL_GUIDANCE_OFF,
                "format", converter.getFormat()));

        LlmAnalysisPayload result;
        if (useTools) {
            result = structuredOutputClient.generate(
                    prompt,
                    LlmAnalysisPayload.class,
                    analysisOutputValidator::validateRawAnalysisJson,
                    analysisOutputValidator::validateAnalysis,
                    analysisTools);
        }
        else {
            result = structuredOutputClient.generate(
                    prompt,
                    LlmAnalysisPayload.class,
                    analysisOutputValidator::validateRawAnalysisJson,
                    analysisOutputValidator::validateAnalysis);
        }

        List<String> rawMatched = safeList(result.matchedSkills());
        List<String> matched = keepEvidenced(resume, rawMatched);
        List<String> missing = dropEvidenced(resume, safeList(result.missingSkills()));

        List<String> jobKeywords = atsKeywordService.extractKeywords(job);
        List<String> found = atsKeywordService.findPresentKeywords(resume, jobKeywords);
        List<String> atsMissing = atsKeywordService.findMissingKeywords(resume, jobKeywords);

        int score = adjustScore(result.matchScore(), rawMatched, matched, found, atsMissing);

        return new AnalysisResult(
                score,
                matched,
                missing,
                result.experienceFit().strip(),
                safeList(result.topSuggestions()),
                found,
                atsMissing,
                retrieval.citations());
    }

    /**
     * Corrects LLM scores when they conflict with resume evidence / ATS overlap.
     */
    int adjustScore(
            int score,
            List<String> rawMatched,
            List<String> matched,
            List<String> atsFound,
            List<String> atsMissing) {
        int adjusted = score;
        int dropped = Math.max(0, rawMatched.size() - matched.size());
        if (dropped >= 2 && matched.size() * 2 < rawMatched.size()) {
            adjusted = Math.min(adjusted, 35 + matched.size() * 8);
        }

        long criticalMissing = atsMissing.stream()
                .map(k -> k.toLowerCase(Locale.ROOT))
                .filter(CRITICAL_MISSING::contains)
                .count();
        if (criticalMissing >= 3 && adjusted >= 50) {
            adjusted = Math.min(adjusted, 35);
        }

        long strongHits = atsFound.stream()
                .map(k -> k.toLowerCase(Locale.ROOT))
                .filter(k -> !SOFT_ATS.contains(k))
                .filter(k -> k.length() >= 4)
                .count();
        // Small models sometimes undershoot a clearly good stack match.
        if (criticalMissing == 0 && strongHits >= 6 && adjusted < 60) {
            adjusted = Math.max(adjusted, 75);
        }

        if (adjusted != score) {
            log.info("Adjusted matchScore {} -> {} (droppedMatched={}, criticalMissing={}, strongHits={})",
                    score, adjusted, dropped, criticalMissing, strongHits);
        }
        return adjusted;
    }

    List<String> keepEvidenced(String resumeText, List<String> skills) {
        String resume = resumeText.toLowerCase(Locale.ROOT);
        List<String> kept = new ArrayList<>();
        for (String skill : skills) {
            if (isEvidenced(resume, skill)) {
                kept.add(skill);
            }
            else {
                log.debug("Dropping unmatched skill claimed by LLM: {}", skill);
            }
        }
        return kept;
    }

    List<String> dropEvidenced(String resumeText, List<String> skills) {
        String resume = resumeText.toLowerCase(Locale.ROOT);
        List<String> kept = new ArrayList<>();
        for (String skill : skills) {
            if (!isEvidenced(resume, skill)) {
                kept.add(skill);
            }
            else {
                log.debug("Dropping falsely missing skill: {}", skill);
            }
        }
        return kept;
    }

    private boolean isEvidenced(String resumeLower, String skill) {
        if (skill == null || skill.isBlank()) {
            return false;
        }
        String normalized = skill.toLowerCase(Locale.ROOT).trim();
        if (resumeLower.contains(normalized)) {
            return true;
        }

        String cleaned = NON_TECH.matcher(normalized).replaceAll(" ");
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : cleaned.split("\\s+")) {
            String t = token.trim();
            if (t.length() >= 3 && !FILLER.contains(t)) {
                tokens.add(t);
            }
        }
        if (tokens.isEmpty()) {
            return false;
        }

        List<String> techTokens = tokens.stream().filter(TECH_TOKENS::contains).toList();
        if (!techTokens.isEmpty()) {
            // "Kafka streaming" → kafka only; "Apache Spark" → spark must be present.
            return techTokens.stream().allMatch(resumeLower::contains);
        }
        // Soft multi-word phrases must appear contiguously (avoid "data"+"engineering" false hits).
        return resumeLower.contains(String.join(" ", tokens));
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }
}
