package com.jaywant.resumeanalyzer.service;

import com.jaywant.resumeanalyzer.config.AppProperties;
import com.jaywant.resumeanalyzer.domain.Citation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Retrieval-Augmented Generation helper: chunk → embed → top-K similar snippets.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final EmbeddingSearchService embeddingSearchService;
    private final TextChunker textChunker;
    private final AppProperties appProperties;

    /**
     * Builds prompt context and citation evidence from resume + job description.
     * When RAG is disabled, returns empty context/citations so callers can compare on vs off.
     */
    public RagRetrieval retrieve(String resumeText, String jobDescription) {
        return retrieve(resumeText, jobDescription, appProperties.getRag().isEnabled());
    }

    public RagRetrieval retrieve(String resumeText, String jobDescription, boolean enabled) {
        if (!enabled) {
            log.info("RAG disabled — skipping retrieval");
            return RagRetrieval.empty();
        }

        List<ScoredChunk> chunks = new ArrayList<>();
        chunks.addAll(toChunks(textChunker.chunk(resumeText), "resume"));
        chunks.addAll(toChunks(textChunker.chunk(jobDescription), "job"));

        if (chunks.isEmpty()) {
            return RagRetrieval.empty();
        }

        String query = buildQuery(jobDescription);
        int topK = appProperties.getRag().getTopK();
        List<ScoredChunk> topMatches = embeddingSearchService.findTopMatches(query, chunks, topK);

        StringBuilder context = new StringBuilder();
        List<Citation> citations = new ArrayList<>();
        for (ScoredChunk match : topMatches) {
            context.append("[")
                    .append(match.source())
                    .append(" | score=")
                    .append(String.format(Locale.ROOT, "%.3f", match.score()))
                    .append("] ")
                    .append(match.text())
                    .append("\n\n");
            citations.add(new Citation(
                    match.source(),
                    truncateExcerpt(match.text(), 280),
                    roundScore(match.score())));
        }

        log.info("RAG retrieved {} citation(s) with top-k={}", citations.size(), topK);
        return new RagRetrieval(context.toString().strip(), List.copyOf(citations));
    }

    private String buildQuery(String jobDescription) {
        // Prefer the JD itself as the retrieval query so resume chunks align to requirements.
        String jd = jobDescription == null ? "" : jobDescription.strip();
        if (jd.length() > 600) {
            jd = jd.substring(0, 600);
        }
        if (jd.isBlank()) {
            return "skills experience requirements responsibilities qualifications";
        }
        return "Match resume evidence to these job requirements: " + jd;
    }

    private List<ScoredChunk> toChunks(List<String> chunks, String source) {
        return chunks.stream()
                .filter(chunk -> chunk != null && !chunk.isBlank())
                .map(chunk -> new ScoredChunk(chunk, source, 0.0))
                .toList();
    }

    private static String truncateExcerpt(String text, int maxChars) {
        String trimmed = text.strip();
        if (trimmed.length() <= maxChars) {
            return trimmed;
        }
        return trimmed.substring(0, maxChars).strip() + "…";
    }

    private static double roundScore(double score) {
        return Math.round(score * 1000.0) / 1000.0;
    }

    public record ScoredChunk(String text, String source, double score) {
    }

    public record RagRetrieval(String promptContext, List<Citation> citations) {
        public static RagRetrieval empty() {
            return new RagRetrieval("", List.of());
        }

        public boolean isEmpty() {
            return promptContext == null || promptContext.isBlank();
        }
    }
}
