package com.pdfrag.service;

/**
 * Helpers for working with embedding vectors.
 */
public final class VectorMath {

    private VectorMath() {
    }

    /**
     * Cosine similarity: 1.0 means "same meaning", 0 means "unrelated".
     * It compares the direction of two vectors, not their length.
     */
    public static double cosine(double[] a, double[] b) {
        int n = Math.min(a.length, b.length);
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < n; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /** Vector -> "0.1,0.2,0.3" so it can be stored in a MySQL text column. */
    public static String serialize(double[] vector) {
        StringBuilder sb = new StringBuilder(vector.length * 10);
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append((float) vector[i]); // OpenAI sends 32-bit numbers, so floats lose nothing and keep the text short
        }
        return sb.toString();
    }

    /** "0.1,0.2,0.3" -> vector. */
    public static double[] parse(String text) {
        if (text == null || text.isEmpty()) {
            return new double[0];
        }
        String[] parts = text.split(",");
        double[] vector = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Double.parseDouble(parts[i]);
        }
        return vector;
    }
}
