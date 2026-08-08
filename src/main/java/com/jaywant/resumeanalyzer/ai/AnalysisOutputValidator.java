package com.jaywant.resumeanalyzer.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaywant.resumeanalyzer.domain.AnalysisResult;
import com.jaywant.resumeanalyzer.domain.ResumeReviewResult;
import org.springframework.stereotype.Component;

/**
 * Validates structured LLM payloads before they are trusted by the app.
 */
@Component
public class AnalysisOutputValidator {

    private final ObjectMapper objectMapper;

    public AnalysisOutputValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void validateRawAnalysisJson(String rawJson) {
        JsonNode root = readObject(rawJson);
        requireScore(root, "matchScore");
        requireNonBlankText(root, "experienceFit");
        requireArray(root, "matchedSkills");
        requireArray(root, "missingSkills");
        requireArray(root, "topSuggestions");
    }

    public void validateAnalysis(AnalysisResult result) {
        if (result == null) {
            throw new StructuredOutputException("Analysis result is null");
        }
        requireScoreValue(result.matchScore(), "matchScore");
        if (result.experienceFit() == null || result.experienceFit().isBlank()) {
            throw new StructuredOutputException("experienceFit is required and must be non-blank");
        }
        if (result.matchedSkills() == null || result.missingSkills() == null || result.topSuggestions() == null) {
            throw new StructuredOutputException("matchedSkills, missingSkills, and topSuggestions must be present (arrays)");
        }
    }

    public void validateRawReviewJson(String rawJson) {
        JsonNode root = readObject(rawJson);
        requireScore(root, "overallScore");
        requireArray(root, "strengths");
        requireArray(root, "improvements");
        requireArray(root, "redFlags");
        requireArray(root, "atsTips");
    }

    public void validateReview(ResumeReviewResult result) {
        if (result == null) {
            throw new StructuredOutputException("Resume review result is null");
        }
        requireScoreValue(result.overallScore(), "overallScore");
        if (result.strengths() == null || result.improvements() == null
                || result.redFlags() == null || result.atsTips() == null) {
            throw new StructuredOutputException("strengths, improvements, redFlags, and atsTips must be present (arrays)");
        }
    }

    private JsonNode readObject(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            if (root == null || !root.isObject()) {
                throw new StructuredOutputException("LLM response must be a JSON object");
            }
            return root;
        }
        catch (StructuredOutputException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new StructuredOutputException("LLM response is not valid JSON: " + ex.getMessage(), ex);
        }
    }

    private void requireScore(JsonNode root, String field) {
        if (!root.has(field) || root.get(field).isNull()) {
            throw new StructuredOutputException(field + " is required");
        }
        JsonNode node = root.get(field);
        if (!node.isNumber()) {
            throw new StructuredOutputException(field + " must be a number between 0 and 100");
        }
        requireScoreValue(node.asInt(), field);
    }

    private void requireScoreValue(int score, String field) {
        if (score < 0 || score > 100) {
            throw new StructuredOutputException(field + " must be between 0 and 100, got: " + score);
        }
    }

    private void requireNonBlankText(JsonNode root, String field) {
        if (!root.has(field) || root.get(field).isNull()) {
            throw new StructuredOutputException(field + " is required");
        }
        if (!root.get(field).isTextual() || root.get(field).asText().isBlank()) {
            throw new StructuredOutputException(field + " must be a non-blank string");
        }
    }

    private void requireArray(JsonNode root, String field) {
        if (!root.has(field) || root.get(field).isNull()) {
            throw new StructuredOutputException(field + " is required");
        }
        if (!root.get(field).isArray()) {
            throw new StructuredOutputException(field + " must be a JSON array");
        }
    }
}
