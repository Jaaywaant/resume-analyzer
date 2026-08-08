package com.jaywant.resumeanalyzer.service;

import com.jaywant.resumeanalyzer.ai.AnalysisOutputValidator;
import com.jaywant.resumeanalyzer.ai.PromptService;
import com.jaywant.resumeanalyzer.ai.StructuredOutputClient;
import com.jaywant.resumeanalyzer.config.AppProperties;
import com.jaywant.resumeanalyzer.domain.AnalysisResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Golden-dataset evaluation for {@link AnalysisService}.
 *
 * <p>These tests use fixed resume/JD fixtures under {@code src/test/resources/samples/}
 * and stub the LLM layer with representative model outputs. Assertions check quality
 * bands (score ranges + required skills), not brittle exact strings from a live model.
 *
 * <p>Expected score bands (documented for regression):
 * <ul>
 *   <li>job-1 (Java backend good match): 60–85</li>
 *   <li>job-2 (React/Node mismatch): 0–45</li>
 *   <li>job-3 (Senior data engineer mismatch): 0–45</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    static final int GOOD_MATCH_MIN = 60;
    static final int GOOD_MATCH_MAX = 85;
    static final int POOR_MATCH_MAX = 45;

    @Mock
    private StructuredOutputClient structuredOutputClient;

    @Mock
    private RagService ragService;

    private AnalysisService analysisService;
    private String resume;
    private String job1;
    private String job2;
    private String job3;

    @BeforeEach
    void setUp() throws IOException {
        resume = loadSample("resume-1.txt");
        job1 = loadSample("job-1.txt");
        job2 = loadSample("job-2.txt");
        job3 = loadSample("job-3.txt");

        when(ragService.buildContext(anyString(), anyString())).thenReturn("No additional context retrieved.");

        analysisService = new AnalysisService(
                structuredOutputClient,
                new AnalysisOutputValidator(new com.fasterxml.jackson.databind.ObjectMapper()),
                new PromptService(),
                ragService,
                new AppProperties(),
                new AtsKeywordService());
    }

    @Test
    @DisplayName("job-1 good Java match: score 60–85 and matchedSkills contains Java")
    void goodJavaMatch_scoreInRange_andContainsJava() {
        stubLlm(new AnalysisResult(
                78,
                List.of("Java", "Spring Boot", "REST APIs", "SQL", "Docker", "Git"),
                List.of("PostgreSQL"),
                "Strong Java/Spring backend fit for an SDE-1 role.",
                List.of("Call out Kafka metrics more clearly", "Mention Agile ceremonies", "Quantify API latency wins"),
                List.of(),
                List.of()));

        AnalysisResult result = analysisService.analyze(resume, job1);

        assertTrue(
                result.matchScore() >= GOOD_MATCH_MIN && result.matchScore() <= GOOD_MATCH_MAX,
                "Expected good-match score in " + GOOD_MATCH_MIN + "-" + GOOD_MATCH_MAX
                        + " but was " + result.matchScore());
        assertTrue(
                containsIgnoreCase(result.matchedSkills(), "Java"),
                "matchedSkills should contain Java, got: " + result.matchedSkills());
        assertTrue(containsIgnoreCase(result.atsKeywordsFound(), "java"));
    }

    @Test
    @DisplayName("job-2 React/Node mismatch: score ≤45 and React not kept as matched")
    void reactNodeMismatch_scoreDampened_andDropsHallucinatedReact() {
        // Simulate a generous/hallucinating model — pipeline must correct it.
        stubLlm(new AnalysisResult(
                80,
                List.of("React.js", "Node.js", "Express", "CSS", "REST APIs"),
                List.of("Next.js", "GraphQL"),
                "Candidate has React and Node experience.",
                List.of("Add Next.js projects", "Learn GraphQL", "Highlight CI/CD"),
                List.of(),
                List.of()));

        AnalysisResult result = analysisService.analyze(resume, job2);

        assertTrue(
                result.matchScore() <= POOR_MATCH_MAX,
                "Expected poor-match score ≤" + POOR_MATCH_MAX + " but was " + result.matchScore());
        assertFalse(
                containsIgnoreCase(result.matchedSkills(), "React"),
                "React must not remain in matchedSkills without resume evidence");
        assertFalse(containsIgnoreCase(result.matchedSkills(), "Node"));
    }

    @Test
    @DisplayName("job-3 data-engineer mismatch: score ≤45 and Spark not kept as matched")
    void dataEngineerMismatch_scoreLow_andDropsHallucinatedSpark() {
        stubLlm(new AnalysisResult(
                70,
                List.of("data engineering", "Apache Spark", "Airflow", "SQL", "Snowflake"),
                List.of("dbt", "Kubernetes"),
                "Candidate looks like a data engineer.",
                List.of("Add Spark projects", "Learn Airflow", "Highlight warehouses"),
                List.of(),
                List.of()));

        AnalysisResult result = analysisService.analyze(resume, job3);

        assertTrue(
                result.matchScore() <= POOR_MATCH_MAX,
                "Expected poor-match score ≤" + POOR_MATCH_MAX + " but was " + result.matchScore());
        assertFalse(
                containsIgnoreCase(result.matchedSkills(), "Spark"),
                "Spark must not remain in matchedSkills without resume evidence");
        assertFalse(containsIgnoreCase(result.matchedSkills(), "Airflow"));
        assertTrue(
                containsIgnoreCase(result.matchedSkills(), "SQL")
                        || result.matchedSkills().stream().anyMatch(s -> s.equalsIgnoreCase("SQL")),
                "SQL overlap may remain when evidenced; got: " + result.matchedSkills());
    }

    @SuppressWarnings("unchecked")
    private void stubLlm(AnalysisResult llmResult) {
        when(structuredOutputClient.generate(
                anyString(),
                eq(AnalysisResult.class),
                ArgumentMatchers.<Consumer<String>>any(),
                ArgumentMatchers.<Consumer<AnalysisResult>>any()))
                .thenReturn(llmResult);
    }

    private static boolean containsIgnoreCase(List<String> values, String expected) {
        return values.stream().anyMatch(v -> v.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT)));
    }

    private static String loadSample(String name) throws IOException {
        String path = "samples/" + name;
        try (InputStream in = AnalysisServiceTest.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("Missing classpath sample: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
