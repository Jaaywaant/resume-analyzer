package com.jaywant.resumeanalyzer.domain;

import java.util.List;

public record OllamaHealthResponse(
        boolean reachable,
        String chatModel,
        String embeddingModel,
        List<String> availableModels,
        String message) {
}
