package com.jaywant.resumeanalyzer.service;

import com.jaywant.resumeanalyzer.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RagService {

    private final EmbeddingSearchService embeddingSearchService;
    private final TextChunker textChunker;
    private final AppProperties appProperties;

    public String buildContext(String resumeText, String jobDescription) {
        List<ScoredChunk> chunks = new ArrayList<>();
        chunks.addAll(toChunks(textChunker.chunk(resumeText), "resume"));
        chunks.addAll(toChunks(textChunker.chunk(jobDescription), "job"));

        if (chunks.isEmpty()) {
            return "";
        }

        String query = "skills experience requirements responsibilities qualifications";
        List<ScoredChunk> topMatches = embeddingSearchService.findTopMatches(
                query,
                chunks,
                appProperties.getRag().getTopK());

        StringBuilder context = new StringBuilder();
        for (ScoredChunk match : topMatches) {
            context.append("[")
                    .append(match.source())
                    .append("] ")
                    .append(match.text())
                    .append("\n\n");
        }
        return context.toString().strip();
    }

    private List<ScoredChunk> toChunks(List<String> chunks, String source) {
        return chunks.stream()
                .map(chunk -> new ScoredChunk(chunk, source, 0.0))
                .toList();
    }

    public record ScoredChunk(String text, String source, double score) {
    }
}
