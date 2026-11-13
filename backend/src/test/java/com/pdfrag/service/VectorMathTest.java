package com.pdfrag.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class VectorMathTest {

    @Test
    void identicalVectorsHaveSimilarityOne() {
        double[] v = {1.0, 2.0, 3.0};
        assertEquals(1.0, VectorMath.cosine(v, v), 1e-9);
    }

    @Test
    void perpendicularVectorsHaveSimilarityZero() {
        assertEquals(0.0, VectorMath.cosine(new double[] {1, 0}, new double[] {0, 1}), 1e-9);
    }

    @Test
    void serializeAndParseRoundTrip() {
        double[] v = {0.25, -1.5, 3.0};
        double[] back = VectorMath.parse(VectorMath.serialize(v));
        assertEquals(3, back.length);
        assertEquals(0.25, back[0], 1e-12);
        assertEquals(-1.5, back[1], 1e-12);
        assertEquals(3.0, back[2], 1e-12);
    }
}
