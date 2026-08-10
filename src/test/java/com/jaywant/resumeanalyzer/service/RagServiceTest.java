package com.jaywant.resumeanalyzer.service;

import com.jaywant.resumeanalyzer.config.AppProperties;
import com.jaywant.resumeanalyzer.domain.Citation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private EmbeddingSearchService embeddingSearchService;

    @Mock
    private TextChunker textChunker;

    @Test
    void retrieveDisabledReturnsEmptyCitations() {
        AppProperties props = new AppProperties();
        props.getRag().setEnabled(false);
        RagService ragService = new RagService(embeddingSearchService, textChunker, props);

        RagService.RagRetrieval retrieval = ragService.retrieve("resume", "job", false);

        assertTrue(retrieval.isEmpty());
        assertTrue(retrieval.citations().isEmpty());
        verify(embeddingSearchService, never()).findTopMatches(anyString(), anyList(), anyInt());
    }

    @Test
    void retrieveEnabledBuildsCitationsFromTopMatches() {
        AppProperties props = new AppProperties();
        props.getRag().setEnabled(true);
        props.getRag().setTopK(2);

        when(textChunker.chunk("Java Spring Boot resume")).thenReturn(List.of("Java Spring Boot resume"));
        when(textChunker.chunk("Need Java developer")).thenReturn(List.of("Need Java developer"));
        when(embeddingSearchService.findTopMatches(anyString(), anyList(), anyInt()))
                .thenReturn(List.of(
                        new RagService.ScoredChunk("Java Spring Boot resume", "resume", 0.91),
                        new RagService.ScoredChunk("Need Java developer", "job", 0.77)));

        RagService ragService = new RagService(embeddingSearchService, textChunker, props);
        RagService.RagRetrieval retrieval = ragService.retrieve("Java Spring Boot resume", "Need Java developer", true);

        assertEquals(2, retrieval.citations().size());
        Citation first = retrieval.citations().get(0);
        assertEquals("resume", first.source());
        assertEquals(0.91, first.similarityScore());
        assertTrue(retrieval.promptContext().contains("Java Spring Boot resume"));
    }
}
