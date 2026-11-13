package com.pdfrag.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pdfrag.model.PdfDocument;

public interface PdfDocumentRepository extends JpaRepository<PdfDocument, Long> {
}
