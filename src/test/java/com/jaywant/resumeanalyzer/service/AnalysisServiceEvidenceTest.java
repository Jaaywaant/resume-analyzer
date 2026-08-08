package com.jaywant.resumeanalyzer.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisServiceEvidenceTest {

    private final AnalysisService service = new AnalysisService(null, null, null, null, null);

    private static final String RESUME = """
            Java Spring Boot Kafka AWS Docker MySQL JUnit Mockito Angular REST APIs
            """;

    @Test
    void keepEvidencedDropsHallucinatedStacks() {
        List<String> kept = service.keepEvidenced(RESUME, List.of("Java", "React.js", "Apache Spark", "Docker"));
        assertEquals(List.of("Java", "Docker"), kept);
    }

    @Test
    void dropEvidencedRemovesSkillsPresentOnResume() {
        List<String> missing = service.dropEvidenced(RESUME, List.of("Kafka streaming", "Airflow", "Node.js"));
        assertFalse(missing.contains("Kafka streaming"));
        assertTrue(missing.contains("Airflow"));
        assertTrue(missing.contains("Node.js"));
    }

    @Test
    void keepEvidencedRejectsSoftPhraseFalsePositives() {
        List<String> kept = service.keepEvidenced(RESUME, List.of("data engineering", "SQL"));
        assertEquals(List.of("SQL"), kept);
    }

    @Test
    void adjustScoreDampensWhenCriticalTechMissing() {
        int adjusted = service.adjustScore(
                80,
                List.of(),
                List.of(),
                List.of("job", "engineer"),
                List.of("react", "node", "express", "mongodb"));
        assertEquals(35, adjusted);
    }

    @Test
    void adjustScoreRaisesUndershotGoodMatch() {
        int adjusted = service.adjustScore(
                0,
                List.of("Java", "Spring Boot"),
                List.of("Java", "Spring Boot"),
                List.of("java", "spring", "boot", "docker", "mysql", "rest", "api", "git"),
                List.of("title", "hiring"));
        assertEquals(75, adjusted);
    }
}
