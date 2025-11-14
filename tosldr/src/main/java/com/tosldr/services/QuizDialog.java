package com.tosldr.services;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;

public class QuizDialog {

    private static final Random rand = new Random();

    public static void showQuiz(JFrame parent, List<FlagResult> allFlags) {

        if (allFlags == null || allFlags.isEmpty()) {
            JOptionPane.showMessageDialog(parent,
                    "Not enough clauses to run quiz.",
                    "Quiz Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 1) Pick one real risky clause
        FlagResult realFlag = allFlags.get(rand.nextInt(allFlags.size()));

        // 2) Generate one fake clause
        String fakeClause = generateFakeClause();

        // 3) Mix them together
        List<String> options = new ArrayList<>();
        options.add(realFlag.getSnippet());
        options.add(fakeClause);
        Collections.shuffle(options);

        // 4) Ask the question
        String question =
                "<html><b>Spot the real risky clause from this ToS:</b><br>"
                        + "One is real, one is AI-generated.<br><br>"
                        + "A) " + escape(options.get(0)) + "<br><br>"
                        + "B) " + escape(options.get(1)) + "</html>";

        String[] buttons = {"A", "B"};
        int response = JOptionPane.showOptionDialog(
                parent,
                question,
                "Spot the Red Flag!",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                buttons,
                buttons[0]
        );

        // 5) Check answer
        String chosen = (response == 0 ? options.get(0) : options.get(1));
        boolean correct = chosen.equals(realFlag.getSnippet());

        if (correct) {
            JOptionPane.showMessageDialog(parent,
                    "Correct! 🎉\nThat was the real clause.\n\nExplanation:\n" +
                            realFlag.getExplanation(),
                    "Nice!",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(parent,
                    "Not quite!\nThe REAL clause was:\n\n" +
                            realFlag.getSnippet() +
                            "\n\nExplanation:\n" +
                            realFlag.getExplanation(),
                    "Answer",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private static String escape(String s) {
        return s.replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;");
    }

    private static String generateFakeClause() {
        String[] templates = {
                "We reserve the right to adjust service features at our discretion.",
                "Your account activity may be reviewed to improve service personalization.",
                "Usage data may be analyzed for maintaining system integrity.",
                "We may modify certain functionalities without prior notice.",
                "Aggregated statistics may be processed for performance optimization."
        };
        return templates[rand.nextInt(templates.length)];
    }
}