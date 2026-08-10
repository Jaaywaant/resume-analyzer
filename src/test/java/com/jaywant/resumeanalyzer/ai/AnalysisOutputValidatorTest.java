package com.jaywant.resumeanalyzer.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaywant.resumeanalyzer.domain.LlmAnalysisPayload;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisOutputValidatorTest {

    private final AnalysisOutputValidator validator = new AnalysisOutputValidator(new ObjectMapper());

    @Test
    void acceptsValidAnalysisJson() {
        String json = """
                {
                  "matchScore": 72,
                  "matchedSkills": ["Java"],
                  "missingSkills": ["Kubernetes"],
                  "experienceFit": "Solid Java backend fit.",
                  "topSuggestions": ["Add Kafka metrics"]
                }
                """;
        assertDoesNotThrow(() -> validator.validateRawAnalysisJson(json));
        assertDoesNotThrow(() -> validator.validateAnalysis(new LlmAnalysisPayload(
                72,
                List.of("Java"),
                List.of("Kubernetes"),
                "Solid Java backend fit.",
                List.of("Add Kafka metrics"))));
    }

    @Test
    void rejectsMissingMatchScore() {
        String json = """
                {
                  "matchedSkills": [],
                  "missingSkills": [],
                  "experienceFit": "Ok",
                  "topSuggestions": []
                }
                """;
        StructuredOutputException ex = assertThrows(
                StructuredOutputException.class,
                () -> validator.validateRawAnalysisJson(json));
        assertTrue(ex.getMessage().contains("matchScore"));
    }

    @Test
    void rejectsOutOfRangeMatchScore() {
        String json = """
                {
                  "matchScore": 140,
                  "matchedSkills": [],
                  "missingSkills": [],
                  "experienceFit": "Ok",
                  "topSuggestions": []
                }
                """;
        StructuredOutputException ex = assertThrows(
                StructuredOutputException.class,
                () -> validator.validateRawAnalysisJson(json));
        assertTrue(ex.getMessage().contains("between 0 and 100"));
    }

    @Test
    void rejectsBlankExperienceFit() {
        String json = """
                {
                  "matchScore": 50,
                  "matchedSkills": [],
                  "missingSkills": [],
                  "experienceFit": "   ",
                  "topSuggestions": []
                }
                """;
        StructuredOutputException ex = assertThrows(
                StructuredOutputException.class,
                () -> validator.validateRawAnalysisJson(json));
        assertTrue(ex.getMessage().contains("experienceFit"));
    }
}
