package com.jaywant.resumeanalyzer.service;

import com.jaywant.resumeanalyzer.config.AppProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextChunkerTest {

    @Test
    void prefersParagraphBoundariesInsideChunkWindow() {
        AppProperties props = new AppProperties();
        props.getRag().setChunkSize(80);
        props.getRag().setChunkOverlap(10);
        TextChunker chunker = new TextChunker(props);

        String text = """
                First paragraph about Java and Spring Boot skills.

                Second paragraph about Docker and Kafka experience on production systems.
                """;

        List<String> chunks = chunker.chunk(text);
        assertFalse(chunks.isEmpty());
        assertTrue(chunks.get(0).contains("First paragraph"));
        assertTrue(chunks.stream().anyMatch(c -> c.contains("Second paragraph")));
    }
}
