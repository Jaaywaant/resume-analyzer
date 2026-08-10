package com.jaywant.resumeanalyzer.service;

import com.jaywant.resumeanalyzer.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits long text into overlapping chunks for embedding retrieval.
 * Prefers paragraph / sentence boundaries near the target chunk size.
 */
@Service
@RequiredArgsConstructor
public class TextChunker {

    private final AppProperties appProperties;

    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        int chunkSize = appProperties.getRag().getChunkSize();
        int overlap = appProperties.getRag().getChunkOverlap();
        if (chunkSize <= overlap) {
            throw new IllegalStateException("chunk-size must be greater than chunk-overlap");
        }

        String normalized = text.replace("\r\n", "\n").strip();
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int hardEnd = Math.min(start + chunkSize, normalized.length());
            int end = chooseBoundary(normalized, start, hardEnd);
            String piece = normalized.substring(start, end).strip();
            if (!piece.isEmpty()) {
                chunks.add(piece);
            }
            if (end >= normalized.length()) {
                break;
            }
            start = Math.max(end - overlap, start + 1);
        }
        return chunks;
    }

    /**
     * Prefer breaking on blank lines, then sentence enders, near the hard limit.
     */
    private int chooseBoundary(String text, int start, int hardEnd) {
        if (hardEnd >= text.length()) {
            return text.length();
        }

        int windowStart = start + Math.max(1, (hardEnd - start) / 2);
        int paragraphBreak = text.lastIndexOf("\n\n", hardEnd);
        if (paragraphBreak >= windowStart) {
            return paragraphBreak + 2;
        }

        int best = -1;
        for (int i = hardEnd; i >= windowStart; i--) {
            char c = text.charAt(i - 1);
            if (c == '.' || c == '!' || c == '?' || c == '\n') {
                best = i;
                break;
            }
        }
        return best > windowStart ? best : hardEnd;
    }
}
