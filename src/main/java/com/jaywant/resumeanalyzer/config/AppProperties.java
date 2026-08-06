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
    private Rag rag = new Rag();

    @Getter
    @Setter
    public static class Rag {
        private int chunkSize = 500;
        private int chunkOverlap = 100;
        private int topK = 4;
    }
}
