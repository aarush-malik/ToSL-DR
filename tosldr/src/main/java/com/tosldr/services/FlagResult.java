package com.tosldr.services;

public class FlagResult {

    public enum Severity { HIGH, MEDIUM, LOW }

    private final String title;
    private final String category;
    private final String explanation;
    private final Severity severity;
    private final String snippet;

    public FlagResult(String title,
                      String category,
                      String explanation,
                      Severity severity,
                      String snippet) {
        this.title = title;
        this.category = category;
        this.explanation = explanation;
        this.severity = severity;
        this.snippet = snippet;
    }

    public String getTitle()      { return title; }
    public String getCategory()   { return category; }
    public String getExplanation(){ return explanation; }
    public Severity getSeverity() { return severity; }
    public String getSnippet()    { return snippet; }

    @Override
    public String toString() {
        // Fallback text (renderer will format anyway)
        return "[" + severity + "] " + title + " — " + explanation;
    }
}