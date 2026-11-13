package com.pdfrag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed view of the "app.*" section in application.yml.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(OpenAi openai, Rag rag) {

    public record OpenAi(String apiKey, String baseUrl, String embeddingModel, String chatModel) {
    }

    public record Rag(int chunkSize, int chunkOverlap, int topK) {
    }
}
