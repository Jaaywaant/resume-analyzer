package com.jaywant.resumeanalyzer.api;

import com.jaywant.resumeanalyzer.domain.OllamaHealthResponse;
import com.jaywant.resumeanalyzer.service.OllamaHealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Tag(name = "Health")
public class HealthController {

    private final OllamaHealthService ollamaHealthService;

    @GetMapping("/ollama")
    @Operation(summary = "Check Ollama connectivity and required models")
    public OllamaHealthResponse checkOllama() {
        return ollamaHealthService.checkHealth();
    }
}
