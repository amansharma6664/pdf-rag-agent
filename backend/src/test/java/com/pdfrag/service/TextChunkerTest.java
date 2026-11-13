package com.pdfrag.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class TextChunkerTest {

    @Test
    void shortTextBecomesOneChunk() {
        List<String> chunks = TextChunker.split("hello world", 100, 20);
        assertEquals(1, chunks.size());
        assertEquals("hello world", chunks.get(0));
    }

    @Test
    void blankTextGivesNoChunks() {
        assertTrue(TextChunker.split("   \n  ", 100, 20).isEmpty());
    }

    @Test
    void longTextIsSplitIntoChunksThatRespectTheSize() {
        String text = "word ".repeat(500);
        List<String> chunks = TextChunker.split(text, 100, 20);
        assertTrue(chunks.size() > 1);
        for (String chunk : chunks) {
            assertTrue(chunk.length() <= 100);
        }
    }
}
