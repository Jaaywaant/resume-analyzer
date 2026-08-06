package com.jaywant.resumeanalyzer.service;

import com.jaywant.resumeanalyzer.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end).strip());
            if (end >= text.length()) {
                break;
            }
            start = end - overlap;
        }
        return chunks;
    }
}
