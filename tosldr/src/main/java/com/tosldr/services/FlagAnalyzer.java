package com.tosldr.services;

import java.util.ArrayList;
import java.util.List;

/**
 * Naive keyword-based detector for risky ToS clauses.
 * This is your own logic = good for the project.
 */
public class FlagAnalyzer {

    private static final String[] KEYWORDS = {
            "arbitration",
            "binding arbitration",
            "class action",
            "third-party",
            "share your data",
            "sell your data",
            "marketing partners",
            "tracking",
            "cookies",
            "auto-renew",
            "subscription",
            "terminate your account",
            "without notice"
    };

    public List<String> analyze(String text) {
        List<String> found = new ArrayList<>();
        String lower = text.toLowerCase();

        for (String keyword : KEYWORDS) {
            if (lower.contains(keyword.toLowerCase())) {
                found.add("Potential issue: \"" + keyword + "\" found in terms.");
            }
        }

        if (found.isEmpty()) {
            found.add("No obvious red-flag keywords detected (but this is not legal advice).");
        }

        return found;
    }
}
