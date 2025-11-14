package com.tosldr.services;

public class FlagResult {

    public enum Severity {
        HIGH, MEDIUM, LOW
    }

    private final String title;
    private final String category;
    private final String explanation;
    private final Severity severity;
    private final String snippet;
    // index of the snippet in the original full text
    private final int startIndex;

    public FlagResult(String title,
                      String category,
                      String explanation,
                      Severity severity,
                      String snippet,
                      int startIndex) {
        this.title = title;
        this.category = category;
        this.explanation = explanation;
        this.severity = severity;
        this.snippet = snippet;
        this.startIndex = startIndex;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getExplanation() {
        return explanation;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getSnippet() {
        return snippet;
    }

    public int getStartIndex() {
        return startIndex;
    }
}