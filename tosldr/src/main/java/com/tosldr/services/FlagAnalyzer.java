package com.tosldr.services;

import java.util.ArrayList;
import java.util.List;

public class FlagAnalyzer {

    private static class Rule {
        final String keyword;
        final String title;
        final String category;
        final String explanation;
        final FlagResult.Severity severity;

        Rule(String keyword, String title, String category,
             String explanation, FlagResult.Severity severity) {
            this.keyword = keyword;
            this.title = title;
            this.category = category;
            this.explanation = explanation;
            this.severity = severity;
        }
    }

    private static final Rule[] RULES = {
        new Rule("binding arbitration",
                "Binding arbitration clause",
                "Dispute resolution",
                "You may be forced to resolve disputes in private arbitration instead of going to court.",
                FlagResult.Severity.HIGH),
        new Rule("arbitration",
                "Arbitration requirement",
                "Dispute resolution",
                "Disputes might have to be handled through arbitration, which can limit your options.",
                FlagResult.Severity.MEDIUM),
        new Rule("class action",
                "Class action waiver",
                "Legal rights",
                "You may be waiving your right to join a class action lawsuit.",
                FlagResult.Severity.HIGH),
        new Rule("third party",
                "Sharing with third parties",
                "Data sharing",
                "Your data may be shared with third parties, possibly for analytics or advertising.",
                FlagResult.Severity.MEDIUM),
        new Rule("sell your data",
                "Sale of personal data",
                "Data sharing",
                "Your personal data might be sold, which is a strong privacy risk.",
                FlagResult.Severity.HIGH),
        new Rule("marketing partners",
                "Marketing partners",
                "Data sharing",
                "Your information may be shared with marketing partners for targeted advertising.",
                FlagResult.Severity.MEDIUM),
        new Rule("auto-renew",
                "Auto-renewing subscription",
                "Billing",
                "Your subscription may renew automatically unless you cancel in time.",
                FlagResult.Severity.MEDIUM),
        new Rule("subscription",
                "Subscription terms",
                "Billing",
                "Check how often you’re billed and how to cancel before renewal.",
                FlagResult.Severity.LOW),
        new Rule("terminate your account",
                "Unilateral termination",
                "Account control",
                "The service can terminate your account, possibly with limited notice.",
                FlagResult.Severity.MEDIUM),
        new Rule("without notice",
                "Changes without notice",
                "Account control",
                "Terms may change or your access may be limited without prior notice.",
                FlagResult.Severity.MEDIUM),
        new Rule("cookies",
                "Extensive cookie tracking",
                "Tracking",
                "The site may track your activity using cookies and similar technologies.",
                FlagResult.Severity.LOW),
        new Rule("tracking",
                "User tracking",
                "Tracking",
                "Your behavior may be tracked across the site or other services.",
                FlagResult.Severity.LOW)
    };

    public List<FlagResult> analyze(String text) {
        List<FlagResult> found = new ArrayList<>();
        String lower = text.toLowerCase();

        for (Rule rule : RULES) {
            int idx = lower.indexOf(rule.keyword.toLowerCase());
            if (idx >= 0) {
                String snippet = buildSnippet(text, idx, rule.keyword.length());
                FlagResult result = new FlagResult(
                        rule.title,
                        rule.category,
                        rule.explanation,
                        rule.severity,
                        snippet
                );
                found.add(result);
            }
        }

        if (found.isEmpty()) {
            found.add(new FlagResult(
                    "No obvious red flags",
                    "General",
                    "No major issues were detected by this simple checker (this is not legal advice).",
                    FlagResult.Severity.LOW,
                    ""
            ));
        }

        return found;
    }

    private String buildSnippet(String full, int index, int length) {
        int window = 120;
        int start = Math.max(0, index - window);
        int end = Math.min(full.length(), index + length + window);
        String snippet = full.substring(start, end).trim();
        return snippet.replaceAll("\\s+", " ");
    }
}