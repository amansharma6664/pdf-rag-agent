package com.pdfrag.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.pdfrag.client.OpenAiClient;
import com.pdfrag.config.AppProperties;
import com.pdfrag.dto.Dtos.DocumentDto;
import com.pdfrag.exception.ApiException;
import com.pdfrag.model.DocChunk;
import com.pdfrag.model.PdfDocument;
import com.pdfrag.repository.DocChunkRepository;
import com.pdfrag.repository.PdfDocumentRepository;
import com.pdfrag.service.PdfTextExtractor.PageText;
import com.pdfrag.service.PdfTextExtractor.PdfContent;

/**
 * The "ingestion" half of RAG: PDF -> text -> chunks -> embeddings -> MySQL.
 */
@Service
public class DocumentService {

    private final PdfTextExtractor extractor;
    private final OpenAiClient openAi;
    private final PdfDocumentRepository documents;
    private final DocChunkRepository chunks;
    private final AppProperties props;

    public DocumentService(PdfTextExtractor extractor, OpenAiClient openAi,
                           PdfDocumentRepository documents, DocChunkRepository chunks,
                           AppProperties props) {
        this.extractor = extractor;
        this.openAi = openAi;
        this.documents = documents;
        this.chunks = chunks;
        this.props = props;
    }

    public DocumentDto upload(MultipartFile file) {
        String fileName = file.getOriginalFilename() == null ? "document.pdf" : file.getOriginalFilename();
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The file is empty.");
        }
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only PDF files are supported.");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Could not read the uploaded file.");
        }

        // Step 1: read the text of every page
        PdfContent content = extractor.extract(bytes);

        // Step 2: cut each page into small overlapping chunks (remember the page number)
        List<String> texts = new ArrayList<>();
        List<Integer> pageNumbers = new ArrayList<>();
        for (PageText page : content.pages()) {
            List<String> pieces = TextChunker.split(page.text(), props.rag().chunkSize(), props.rag().chunkOverlap());
            for (String piece : pieces) {
                texts.add(piece);
                pageNumbers.add(page.page());
            }
        }
        if (texts.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "No readable text found in this PDF. Scanned (image-only) PDFs are not supported.");
        }

        // Step 3: turn every chunk into an embedding with OpenAI
        List<double[]> vectors = openAi.embed(texts);

        // Step 4: save the document and its chunks in MySQL
        PdfDocument document = new PdfDocument();
        document.setFileName(fileName);
        document.setPageCount(content.pageCount());
        document.setChunkCount(texts.size());
        document = documents.save(document);

        try {
            List<DocChunk> rows = new ArrayList<>();
            for (int i = 0; i < texts.size(); i++) {
                DocChunk chunk = new DocChunk();
                chunk.setDocumentId(document.getId());
                chunk.setPageNumber(pageNumbers.get(i));
                chunk.setChunkIndex(i);
                chunk.setContent(texts.get(i));
                chunk.setEmbedding(VectorMath.serialize(vectors.get(i)));
                rows.add(chunk);
            }
            chunks.saveAll(rows);
        } catch (RuntimeException e) {
            documents.deleteById(document.getId());
            throw e;
        }
        return toDto(document);
    }

    public List<DocumentDto> list() {
        return documents.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(DocumentService::toDto)
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        if (!documents.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Document not found.");
        }
        chunks.deleteByDocumentId(id);
        documents.deleteById(id);
    }

    private static DocumentDto toDto(PdfDocument d) {
        return new DocumentDto(d.getId(), d.getFileName(), d.getPageCount(), d.getChunkCount(), d.getCreatedAt());
    }
}
