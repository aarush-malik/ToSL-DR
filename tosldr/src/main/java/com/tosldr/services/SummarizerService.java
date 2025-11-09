package com.tosldr.services;

/**
 * High-level wrapper around the HuggingFaceClient.
 * Handles long text by splitting into chunks.
 */
public class SummarizerService {

    private final HuggingFaceClient client;

    // simple character-based chunk size (safe-ish for demo)
    private static final int CHUNK_SIZE = 2000;

    public SummarizerService(HuggingFaceClient client) {
        this.client = client;
    }

    public String summarize(String text) throws Exception {
        text = text.trim();
        if (text.isEmpty()) {
            return "No text provided.";
        }

        if (text.length() <= CHUNK_SIZE) {
            return client.summarize(text);
        }

        // If long: split into chunks, summarize each, then join
        StringBuilder combined = new StringBuilder();
        int start = 0;
        int part = 1;

        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            String chunk = text.substring(start, end);
            String partial = client.summarize(chunk);
            combined.append("Part ").append(part).append(":\n")
                    .append(partial).append("\n\n");
            start = end;
            part++;
        }

        return combined.toString().trim();
    }
}