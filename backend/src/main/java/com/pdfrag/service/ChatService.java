package com.pdfrag.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.pdfrag.client.OpenAiClient;
import com.pdfrag.config.AppProperties;
import com.pdfrag.dto.Dtos.ChatMessageDto;
import com.pdfrag.dto.Dtos.ChatRequest;
import com.pdfrag.dto.Dtos.ChatResponse;
import com.pdfrag.dto.Dtos.SourceDto;
import com.pdfrag.exception.ApiException;
import com.pdfrag.model.PdfDocument;
import com.pdfrag.repository.PdfDocumentRepository;
import com.pdfrag.service.RetrievalService.ScoredChunk;

/**
 * The "generation" half of RAG: put the best chunks into a prompt and let the LLM answer.
 */
@Service
public class ChatService {

    private static final int MAX_HISTORY_MESSAGES = 6;

    private static final String SYSTEM_PROMPT = """
            You are a helpful assistant that answers questions about the user's PDF documents.
            Use ONLY the context passages provided in the user message.
            If the answer is not in the context, say clearly that you could not find it in the uploaded documents.
            Mention the page number like (p. 3) when you use a passage.
            Reply in the same language the user wrote the question in.
            """;

    private final RetrievalService retrieval;
    private final OpenAiClient openAi;
    private final PdfDocumentRepository documents;
    private final AppProperties props;

    public ChatService(RetrievalService retrieval, OpenAiClient openAi,
                       PdfDocumentRepository documents, AppProperties props) {
        this.retrieval = retrieval;
        this.openAi = openAi;
        this.documents = documents;
        this.props = props;
    }

    public ChatResponse ask(ChatRequest request) {
        String question = request.question() == null ? "" : request.question().trim();
        if (question.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Please type a question.");
        }
        if (documents.count() == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Upload at least one PDF first.");
        }

        // Retrieve: find the most relevant chunks
        List<ScoredChunk> hits = retrieval.search(question, request.documentIds(), props.rag().topK());
        if (hits.isEmpty()) {
            return new ChatResponse("I could not find anything in the selected documents.", List.of());
        }

        Set<Long> documentIds = hits.stream().map(h -> h.chunk().getDocumentId()).collect(Collectors.toSet());
        Map<Long, String> fileNames = new HashMap<>();
        for (PdfDocument d : documents.findAllById(documentIds)) {
            fileNames.put(d.getId(), d.getFileName());
        }

        // Augment: build the prompt = context passages + question
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < hits.size(); i++) {
            ScoredChunk hit = hits.get(i);
            context.append("[").append(i + 1).append("] ")
                    .append(fileNames.getOrDefault(hit.chunk().getDocumentId(), "document"))
                    .append(", page ").append(hit.chunk().getPageNumber()).append("\n")
                    .append(hit.chunk().getContent()).append("\n\n");
        }

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        addHistory(messages, request.history());
        messages.add(Map.of("role", "user",
                "content", "Context:\n" + context + "Question: " + question));

        // Generate: ask the LLM
        String answer = openAi.chat(messages);

        List<SourceDto> sources = hits.stream()
                .map(h -> new SourceDto(
                        h.chunk().getDocumentId(),
                        fileNames.getOrDefault(h.chunk().getDocumentId(), "document"),
                        h.chunk().getPageNumber(),
                        Math.round(h.score() * 1000.0) / 1000.0,
                        snippet(h.chunk().getContent())))
                .toList();

        return new ChatResponse(answer, sources);
    }

    /** Adds the last few chat messages so follow-up questions make sense. */
    private void addHistory(List<Map<String, String>> messages, List<ChatMessageDto> history) {
        if (history == null || history.isEmpty()) {
            return;
        }
        int from = Math.max(0, history.size() - MAX_HISTORY_MESSAGES);
        for (ChatMessageDto m : history.subList(from, history.size())) {
            boolean validRole = "user".equals(m.role()) || "assistant".equals(m.role());
            if (validRole && m.content() != null && !m.content().isBlank()) {
                messages.add(Map.of("role", m.role(), "content", m.content()));
            }
        }
    }

    private static String snippet(String text) {
        return text.length() > 320 ? text.substring(0, 320) + "…" : text;
    }
}
