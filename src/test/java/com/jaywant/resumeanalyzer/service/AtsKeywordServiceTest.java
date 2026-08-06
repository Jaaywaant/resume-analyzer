package com.jaywant.resumeanalyzer.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtsKeywordServiceTest {

    private final AtsKeywordService atsKeywordService = new AtsKeywordService();

    @Test
    void findsPresentAndMissingKeywords() {
        String resume = "Experienced Java developer with Spring Boot and REST APIs.";
        List<String> keywords = List.of("java", "spring", "kubernetes", "rest");

        assertEquals(List.of("java", "spring", "rest"), atsKeywordService.findPresentKeywords(resume, keywords));
        assertEquals(List.of("kubernetes"), atsKeywordService.findMissingKeywords(resume, keywords));
    }

    @Test
    void extractsKeywordsFromJobDescription() {
        List<String> keywords = atsKeywordService.extractKeywords(
                "Looking for Java backend engineer with Spring Boot and SQL experience.");

        assertTrue(keywords.contains("java"));
        assertTrue(keywords.contains("spring"));
    }
}
