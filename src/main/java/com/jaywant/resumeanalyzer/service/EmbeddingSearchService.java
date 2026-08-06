package com.jaywant.resumeanalyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmbeddingSearchService {

    private final EmbeddingModel embeddingModel;

    public List<RagService.ScoredChunk> findTopMatches(String query, List<RagService.ScoredChunk> chunks, int topK) {
        if (chunks.isEmpty()) {
            return List.of();
        }

        float[] queryVector = embeddingModel.embed(query);
        return chunks.stream()
                .map(chunk -> new RagService.ScoredChunk(
                        chunk.text(),
                        chunk.source(),
                        cosineSimilarity(queryVector, embeddingModel.embed(chunk.text()))))
                .sorted(Comparator.comparingDouble(RagService.ScoredChunk::score).reversed())
                .limit(topK)
                .toList();
    }

    private double cosineSimilarity(float[] left, float[] right) {
        if (left.length != right.length) {
            return 0.0;
        }

        double dot = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;
        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }

        if (leftNorm == 0.0 || rightNorm == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}
