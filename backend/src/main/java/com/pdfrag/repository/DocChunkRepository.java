package com.pdfrag.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pdfrag.model.DocChunk;

public interface DocChunkRepository extends JpaRepository<DocChunk, Long> {

    List<DocChunk> findByDocumentIdIn(Collection<Long> documentIds);

    void deleteByDocumentId(Long documentId);
}
