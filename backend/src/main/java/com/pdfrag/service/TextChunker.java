package com.pdfrag.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits long text into small overlapping pieces ("chunks").
 *
 * Why? An LLM can only read a limited amount of text, and search works better on small
 * focused pieces. The overlap keeps sentences that sit on a boundary from being cut off.
 */
public final class TextChunker {

    private TextChunker() {
    }

    public static List<String> split(String text, int size, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null) {
            return chunks;
        }
        String clean = text.replaceAll("\\s+", " ").trim();
        if (clean.isEmpty()) {
            return chunks;
        }
        if (size < 50) {
            size = 50;
        }
        if (overlap < 0 || overlap >= size) {
            overlap = size / 5;
        }

        int start = 0;
        while (start < clean.length()) {
            int end = Math.min(start + size, clean.length());

            // try to end on a space so words are not cut in half
            if (end < clean.length()) {
                int lastSpace = clean.lastIndexOf(' ', end);
                if (lastSpace > start + size / 2) {
                    end = lastSpace;
                }
            }

            String piece = clean.substring(start, end).trim();
            if (!piece.isEmpty()) {
                chunks.add(piece);
            }
            if (end >= clean.length()) {
                break;
            }

            // next chunk starts a little before this one ended (the overlap)
            start = Math.max(end - overlap, start + 1);
            int space = clean.indexOf(' ', start);
            if (space != -1 && space < end) {
                start = space + 1;
            }
        }
        return chunks;
    }
}
