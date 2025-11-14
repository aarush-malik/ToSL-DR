package com.tosldr.services;

import java.util.ArrayList;
import java.util.List;

public class FlagAnalyzer {

    private static class Rule {
        final String title;
        final String category;
        final String explanation;
        final FlagResult.Severity severity;
        final String[] patterns;

        Rule(String title,
             String category,
             String explanation,
             FlagResult.Severity severity,
             String... patterns) {
            this.title = title;
            this.category = category;
            this.explanation = explanation;
            this.severity = severity;
            this.patterns = patterns;
        }
    }

    // You can tweak wording / severities if you want
    private static final Rule[] RULES = new Rule[] {
        new Rule(
            "Arbitration requirement",
            "Dispute resolution",
            "Disputes may have to be handled through arbitration, which can limit your ability to go to court.",
            FlagResult.Severity.MEDIUM,
            "binding arbitration", "arbitration"
        ),
        new Rule(
            "Class action waiver",
            "Legal rights",
            "You may be waiving your right to join a class action lawsuit.",
            FlagResult.Severity.HIGH,
            "class action waiver", "waive your right to bring a class action", "class action"
        ),
        new Rule(
            "Sharing with third parties",
            "Data sharing",
            "Your data may be shared with third parties, possibly for analytics or advertising.",
            FlagResult.Severity.MEDIUM,
            "third parties", "third-party", "third party"
        ),
        new Rule(
            "Subscription / auto-renew",
            "Billing",
            "This service may auto-renew or charge you on a recurring basis unless you cancel.",
            FlagResult.Severity.MEDIUM,
            "auto-renew", "auto renew", "automatically renew", "subscription"
        ),
        new Rule(
            "Account termination",
            "Account control",
            "The provider may suspend or terminate your account, sometimes without much notice.",
            FlagResult.Severity.MEDIUM,
            "terminate your account", "suspend your account", "without notice"
        )
    };

    public List<FlagResult> analyze(String text) {
        List<FlagResult> results = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return results;
        }

        String lower = text.toLowerCase();

        for (Rule rule : RULES) {
            for (String pattern : rule.patterns) {
                String p = pattern.toLowerCase();
                int idx = lower.indexOf(p);

                while (idx >= 0) {
                    String snippet = extractSnippet(text, idx, p.length());
                    results.add(new FlagResult(
                            rule.title,
                            rule.category,
                            rule.explanation,
                            rule.severity,
                            snippet,
                            idx
                    ));
                    idx = lower.indexOf(p, idx + p.length());
                }
            }
        }

        if (results.isEmpty()) {
            results.add(new FlagResult(
                    "No obvious red-flag keywords detected",
                    "General",
                    "We did not find matches for our current rule set, but this is not legal advice.",
                    FlagResult.Severity.LOW,
                    "",
                    -1
            ));
        }

        return results;
    }

        // --- Overall risk scoring helpers ---

    /**
     * Compute a simple 0–100 "safety" score from the list of flags.
     * 100 = very user-friendly, 0 = very risky.
     * This is just a heuristic, not legal advice.
     */
    public static int computeRiskScore(java.util.List<FlagResult> flags) {
        if (flags == null || flags.isEmpty()) {
            return 80; // neutral-ish if nothing found
        }

        int penalty = 0;

        for (FlagResult f : flags) {
            // Skip the "no obvious flags" informational item
            if (f.getStartIndex() < 0) continue;

            switch (f.getSeverity()) {
                case HIGH -> penalty += 25;
                case MEDIUM -> penalty += 15;
                case LOW -> penalty += 5;
            }
        }

        int score = 100 - penalty;
        if (score < 0) score = 0;
        if (score > 100) score = 100;
        return score;
    }

    /**
     * Human-friendly label for the score.
     */
    public static String labelForScore(int score) {
        if (score >= 80) return "Generally user-friendly";
        if (score >= 60) return "Some concerns";
        if (score >= 40) return "Risky for users";
        return "Very risky / one-sided";
    }

    private String extractSnippet(String text, int index, int length) {
        int context = 90; // chars around the match
        int start = Math.max(0, index - context);
        int end = Math.min(text.length(), index + length + context);

        String raw = text.substring(start, end);
        // flatten whitespace for display
        return raw.replaceAll("\\s+", " ").trim();
    }
}