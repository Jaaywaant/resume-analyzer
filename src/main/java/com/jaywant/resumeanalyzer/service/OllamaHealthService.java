package com.jaywant.resumeanalyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaywant.resumeanalyzer.domain.OllamaHealthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaHealthService {

    private final ObjectMapper objectMapper;

    @Value("${spring.ai.ollama.base-url}")
    private String ollamaBaseUrl;

    @Value("${spring.ai.ollama.chat.model}")
    private String chatModel;

    @Value("${spring.ai.ollama.embedding.model}")
    private String embeddingModel;

    public OllamaHealthResponse checkHealth() {
        try {
            String body = RestClient.create(ollamaBaseUrl)
                    .get()
                    .uri("/api/tags")
                    .retrieve()
                    .body(String.class);

            List<String> models = parseModels(body);
            boolean hasChatModel = models.stream().anyMatch(model -> model.startsWith(chatModel));
            boolean hasEmbeddingModel = models.stream().anyMatch(model -> model.startsWith(embeddingModel));

            String message;
            if (hasChatModel && hasEmbeddingModel) {
                message = "Ollama is ready";
            }
            else {
                message = "Ollama is running but required models are missing. Run: ollama pull "
                        + chatModel + " && ollama pull " + embeddingModel;
            }

            return new OllamaHealthResponse(true, chatModel, embeddingModel, models, message);
        }
        catch (Exception ex) {
            log.warn("Ollama health check failed: {}", ex.getMessage());
            return new OllamaHealthResponse(
                    false,
                    chatModel,
                    embeddingModel,
                    List.of(),
                    "Ollama is not reachable at " + ollamaBaseUrl + ". Start Ollama and pull required models.");
        }
    }

    private List<String> parseModels(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode modelsNode = root.path("models");
        List<String> models = new ArrayList<>();
        if (modelsNode.isArray()) {
            for (JsonNode modelNode : modelsNode) {
                models.add(modelNode.path("name").asText());
            }
        }
        return models;
    }
}
