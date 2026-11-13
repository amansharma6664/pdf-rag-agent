package com.pdfrag.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.pdfrag.client.OpenAiClient;
import com.pdfrag.model.DocChunk;
import com.pdfrag.repository.DocChunkRepository;

/**
 * The "retrieval" half of RAG: find the chunks whose meaning is closest to the question.
 */
@Service
public class RetrievalService {

    public record ScoredChunk(DocChunk chunk, double score) {
    }

    private final OpenAiClient openAi;
    private final DocChunkRepository chunks;

    public RetrievalService(OpenAiClient openAi, DocChunkRepository chunks) {
        this.openAi = openAi;
        this.chunks = chunks;
    }

    public List<ScoredChunk> search(String question, List<Long> documentIds, int topK) {
        // 1. embed the question the same way the chunks were embedded
        double[] questionVector = openAi.embed(List.of(question)).get(0);

        // 2. load the candidate chunks from MySQL (all documents, or only the selected ones)
        List<DocChunk> candidates = (documentIds == null || documentIds.isEmpty())
                ? chunks.findAll()
                : chunks.findByDocumentIdIn(documentIds);

        // 3. score every chunk with cosine similarity and keep the best ones
        return candidates.stream()
                .map(c -> new ScoredChunk(c, VectorMath.cosine(questionVector, VectorMath.parse(c.getEmbedding()))))
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(topK)
                .toList();
    }
}
