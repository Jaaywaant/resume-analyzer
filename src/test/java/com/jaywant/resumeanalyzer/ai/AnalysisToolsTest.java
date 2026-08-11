package com.jaywant.resumeanalyzer.ai;

import com.jaywant.resumeanalyzer.service.AtsKeywordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisToolsTest {

    private AnalysisTools tools;

    @BeforeEach
    void setUp() {
        tools = new AnalysisTools(new AtsKeywordService());
    }

    @Test
    void extractSkillsReturnsKeywordList() {
        String result = tools.extractSkills(
                "Looking for Java backend engineer with Spring Boot and Kafka experience.");
        assertTrue(result.contains("java"));
        assertTrue(result.contains("spring") || result.contains("boot") || result.contains("kafka"));
    }

    @Test
    void scoreAtsKeywordsReportsCoverage() {
        String resume = "Experienced Java developer with Spring Boot and REST APIs.";
        String job = "Need Java, Spring Boot, Kubernetes, and Kafka.";

        String result = tools.scoreAtsKeywords(resume, job);

        assertTrue(result.contains("coveragePercent="));
        assertTrue(result.contains("java"));
        assertTrue(result.contains("found="));
        assertTrue(result.contains("missing="));
        assertTrue(result.toLowerCase().contains("kubernetes") || result.toLowerCase().contains("kafka"));
    }

    @Test
    void normalizeSkillCanonicalizesAliases() {
        assertEquals("React", tools.normalizeSkill("react.js"));
        assertEquals("Node.js", tools.normalizeSkill("nodejs"));
        assertEquals("Kubernetes", tools.normalizeSkill("k8s"));
        assertEquals("Spring Boot", tools.normalizeSkill("spring-boot"));
        assertEquals("PostgreSQL", tools.normalizeSkill("postgres"));
    }
}
