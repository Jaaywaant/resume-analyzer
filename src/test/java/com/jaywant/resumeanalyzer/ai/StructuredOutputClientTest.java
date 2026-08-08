package com.jaywant.resumeanalyzer.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StructuredOutputClientTest {

    @Test
    void extractJsonStripsMarkdownFence() {
        String raw = """
                ```json
                {"matchScore": 10}
                ```
                """;
        assertEquals("{\"matchScore\": 10}", StructuredOutputClient.extractJson(raw));
    }

    @Test
    void extractJsonFindsObjectInsideProse() {
        String raw = "Here you go:\n{\"matchScore\": 10, \"experienceFit\": \"ok\"}\nThanks";
        assertEquals("{\"matchScore\": 10, \"experienceFit\": \"ok\"}", StructuredOutputClient.extractJson(raw));
    }
}
