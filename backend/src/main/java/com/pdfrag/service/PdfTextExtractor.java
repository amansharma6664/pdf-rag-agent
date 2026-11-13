package com.pdfrag.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.pdfrag.exception.ApiException;

/**
 * Reads the text of a PDF, page by page, using Apache PDFBox.
 */
@Component
public class PdfTextExtractor {

    public record PageText(int page, String text) {
    }

    public record PdfContent(int pageCount, List<PageText> pages) {
    }

    public PdfContent extract(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            int pageCount = document.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            List<PageText> pages = new ArrayList<>();

            for (int page = 1; page <= pageCount; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(document);
                if (text != null && !text.isBlank()) {
                    pages.add(new PageText(page, text));
                }
            }
            return new PdfContent(pageCount, pages);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Could not read this PDF. It may be damaged or password protected.");
        }
    }
}
