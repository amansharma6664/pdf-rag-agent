package com.pdfrag.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Small data carriers used by the REST API (what goes in and out as JSON).
 */
public final class Dtos {

    private Dtos() {
    }

    public record DocumentDto(Long id, String fileName, int pageCount, int chunkCount, LocalDateTime createdAt) {
    }

    public record ChatMessageDto(String role, String content) {
    }

    public record ChatRequest(String question, List<Long> documentIds, List<ChatMessageDto> history) {
    }

    public record SourceDto(Long documentId, String fileName, int page, double score, String snippet) {
    }

    public record ChatResponse(String answer, List<SourceDto> sources) {
    }

    public record StatusDto(boolean openaiConfigured, String chatModel, String embeddingModel, int topK) {
    }
}
