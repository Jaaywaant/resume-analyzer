package com.jaywant.resumeanalyzer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private int resumeCharLimit = 4000;
    private int jobDescriptionCharLimit = 4000;
    private Prompts prompts = new Prompts();
    private Rag rag = new Rag();

    @Getter
    @Setter
    public static class Prompts {
        /** Prompt template under classpath:/prompts/ (e.g. analyze-v1.st, analyze-v2.st). */
        private String analyze = "analyze-v2.st";
        private String review = "review-v1.st";
    }

    @Getter
    @Setter
    public static class Rag {
        /** When false, analyze skips embedding retrieval (useful to compare RAG on vs off). */
        private boolean enabled = true;
        private int chunkSize = 400;
        private int chunkOverlap = 80;
        private int topK = 5;
    }
}
