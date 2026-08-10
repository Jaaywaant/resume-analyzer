package com.jaywant.resumeanalyzer.domain;

/**
 * A retrieved text snippet used as RAG evidence for an analysis.
 *
 * @param source           {@code resume} or {@code job}
 * @param excerpt          chunk text shown to the model / caller
 * @param similarityScore  cosine similarity to the retrieval query (0–1)
 */
public record Citation(
        String source,
        String excerpt,
        double similarityScore) {
}
