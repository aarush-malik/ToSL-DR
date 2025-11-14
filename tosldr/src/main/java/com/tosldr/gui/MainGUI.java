package com.tosldr.gui;

import com.tosldr.services.FlagAnalyzer;
import com.tosldr.services.FlagResult;
import com.tosldr.services.HuggingFaceClient;
import com.tosldr.services.SummarizerService;
import com.tosldr.services.QuizDialog;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.tosldr.services.WebFetcher;

public class MainGUI extends JFrame {
    private int summarizationCount = 0;
    private boolean quizAlreadyOffered = false;

    private JTextArea inputArea;
    private JTextField urlField;
    private JButton fetchButton;
    private JButton summarizeButton;
    private JTextArea summaryArea;
    private JList<FlagResult> flagsList;
    private JProgressBar progressBar;
    private JLabel scoreLabel;

    private final SummarizerService summarizerService;
    private final FlagAnalyzer flagAnalyzer;

    public MainGUI() {
        // Colorful Catppuccin Macchiato Theme
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");

            // Modern font – Inter if available
            Font uiFont = new Font("Inter", Font.PLAIN, 14);
            if (!uiFont.getFamily().equals("Inter")) {
                uiFont = new Font("Segoe UI", Font.PLAIN, 14);
            }
            UIManager.put("defaultFont", uiFont);

            // Base background
            UIManager.put("control", new Color(0x24, 0x27, 0x3A)); // #24273A
            UIManager.put("nimbusLightBackground", new Color(0x1E, 0x20, 0x30)); // #1E2030
            UIManager.put("text", new Color(0xEE, 0xEE, 0xEE)); // bright white text

            // Selection colors (teal)
            UIManager.put("nimbusSelectionBackground", new Color(0x8B, 0xD5, 0xCA)); // teal
            UIManager.put("nimbusFocus", new Color(0x8B, 0xD5, 0xCA));

            // Accents (mauve)
            UIManager.put("nimbusBase", new Color(0xC6, 0xA0, 0xF6)); // mauve

            // Divider & borders
            UIManager.put("nimbusBlueGrey", new Color(0x36, 0x39, 0x54)); // #363954

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        setTitle("TosL;DR - Terms of Service Summarizer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);

        // HF client + services
        String token = System.getenv("HF_TOKEN");
        HuggingFaceClient client = new HuggingFaceClient(token);
        this.summarizerService = new SummarizerService(client);
        this.flagAnalyzer = new FlagAnalyzer();

        initComponents();

        // Nice font
        Font baseFont = new Font("SansSerif", Font.PLAIN, 13);
        inputArea.setFont(baseFont);
        summaryArea.setFont(baseFont);
        flagsList.setFont(baseFont);

        setVisible(true);
    }

    private void initComponents() {
        // URL bar at top
        JPanel urlPanel = new JPanel(new BorderLayout(5, 5));
        urlField = new JTextField();
        // Enable Cmd+V paste on macOS for URL field
        KeyStroke pasteKeyURL = KeyStroke.getKeyStroke("meta V");
        urlField.getInputMap().put(pasteKeyURL, "paste");
        fetchButton = new ModernButton("Fetch ToS from URL");
        urlPanel.setBorder(BorderFactory.createTitledBorder("Load from URL"));
        urlPanel.add(urlField, BorderLayout.CENTER);
        urlPanel.add(fetchButton, BorderLayout.EAST);

        // Left: input text (EDITABLE)
        inputArea = new JTextArea();
        inputArea.setEditable(true);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setBackground(new Color(0x1E2030));
        inputArea.setForeground(new Color(0xF4F4F5));
        inputArea.setCaretColor(new Color(0xC6A0F6)); // mauve caret

        // Ensure Cmd+V does paste
        KeyStroke pasteKey = KeyStroke.getKeyStroke("meta V"); // meta = Command on Mac
        inputArea.getInputMap().put(pasteKey, "paste");
        SwingUtilities.invokeLater(() -> inputArea.requestFocusInWindow());

        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputScroll.setBorder(BorderFactory.createTitledBorder("Paste Terms of Service"));

        // Right top: summary (read-only)
        summaryArea = new JTextArea();
        summaryArea.setEditable(false);
        summaryArea.setLineWrap(true);
        summaryArea.setWrapStyleWord(true);
        JScrollPane summaryScroll = new JScrollPane(summaryArea);
        summaryScroll.setBorder(BorderFactory.createTitledBorder("Plain-Language Summary"));
        summaryArea.setBackground(new Color(0x1E2030));
        summaryArea.setForeground(new Color(0xF4F4F5));
        summaryArea.setCaretColor(new Color(0x8AADF4)); // blue caret

        // Right bottom: flags list with color-coded renderer
        flagsList = new JList<>();
        flagsList.setCellRenderer(new FlagCellRenderer());
        flagsList.setBackground(new Color(0x24273A));
        flagsList.setForeground(new Color(0xF4F4F5));


        // click flag → jump to snippet in ToS
        flagsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                FlagResult flag = flagsList.getSelectedValue();
                if (flag == null || flag.getSnippet().isEmpty()) return;

                String full = inputArea.getText();
                int idx = flag.getStartIndex();

                if (idx >= 0 && idx < full.length()) {
                    inputArea.requestFocusInWindow();
                    inputArea.setCaretPosition(idx);
                    int end = Math.min(full.length(), idx + flag.getSnippet().length());
                    inputArea.select(idx, end);
                }
            }
        });

        JScrollPane flagsScroll = new JScrollPane(flagsList);

        JPanel flagsPanel = new JPanel(new BorderLayout());
        flagsPanel.setBorder(
                BorderFactory.createTitledBorder("Risk & Key Clauses (color-coded)")
        );
        
        scoreLabel = new JLabel("Overall risk score: —");
        scoreLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        scoreLabel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

        flagsPanel.add(scoreLabel, BorderLayout.NORTH);
        flagsPanel.add(flagsScroll, BorderLayout.CENTER);
        flagsPanel.add(buildLegendPanel(), BorderLayout.SOUTH);

        // Right stacked
        JPanel rightPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        rightPanel.add(summaryScroll);
        rightPanel.add(flagsPanel);

        // Split left/right
        JSplitPane centerSplit = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                inputScroll,
                rightPanel
        );
        centerSplit.setResizeWeight(0.5);

        // Bottom: button + inline progress bar
        summarizeButton = new ModernButton("Summarize & Analyze");
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(summarizeButton, BorderLayout.CENTER);
        bottomPanel.add(progressBar, BorderLayout.SOUTH);

        // Layout
        setLayout(new BorderLayout(5, 5));
        add(urlPanel, BorderLayout.NORTH);
        add(centerSplit, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // REAL fetch action (async)
        fetchButton.addActionListener(e -> {
            String url = urlField.getText().trim();
            if (url.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter a URL first.");
                return;
            }

            // Add scheme if user forgot it
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }

            String finalUrl = url;
            inputArea.setText("Fetching content from: " + finalUrl + " ...");
            fetchButton.setEnabled(false);
            summarizeButton.setEnabled(false);

            new Thread(() -> {
                try {
                    String tosText = fetchTosFromUrl(finalUrl);
                    SwingUtilities.invokeLater(() -> {
                        fetchButton.setEnabled(true);
                        summarizeButton.setEnabled(true);
                        if (tosText.isBlank()) {
                            inputArea.setText("No readable text found at:\n" + finalUrl);
                        } else {
                            inputArea.setText(tosText);
                            inputArea.setCaretPosition(0);
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        fetchButton.setEnabled(true);
                        summarizeButton.setEnabled(true);
                        JOptionPane.showMessageDialog(this,
                                "Failed to fetch terms from URL:\n" + ex.getMessage(),
                                "Fetch Error",
                                JOptionPane.ERROR_MESSAGE);
                        inputArea.setText("");
                    });
                }
            }).start();
        });

        // Summarize button
        summarizeButton.addActionListener(e -> {
            String text = inputArea.getText().trim();
            if (text.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Paste ToS text or load from URL first.");
                return;
            }

            // --- SPECIAL HIGH-RISK POPUP FOR LEETCODE ---
            if (text.toLowerCase().contains("leetcode")) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(
                            this,
                            "<html><b>⚠️ Special High-Risk Warning</b><br><br>" +
                            "LeetCode Terms detected.<br>" +
                            "Extended use of LeetCode may cause:<br>" +
                            "- chronic impostor syndrome,<br>" +
                            "- obsessive Blind 75 refresh cycles,<br>" +
                            "- spontaneous system.out.println(\"why am I doing this\");<br><br>" +
                            "<i>Proceed at your own mental health risk.</i></html>",
                            "LeetCode Warning",
                            JOptionPane.WARNING_MESSAGE
                    );
                });
            }

            summaryArea.setText("Summarizing, please wait...");
            flagsList.setListData(new FlagResult[0]);
            progressBar.setVisible(true);

            new Thread(() -> {
                try {
                    String summary = summarizerService.summarize(text);
                    java.util.List<FlagResult> flags = flagAnalyzer.analyze(text);

                    SwingUtilities.invokeLater(() -> {
                        progressBar.setVisible(false);
                        summaryArea.setText(summary);
                        flagsList.setListData(flags.toArray(new FlagResult[0]));

                        // Compute and display overall risk score
                        int score = FlagAnalyzer.computeRiskScore(flags);
                        String label = FlagAnalyzer.labelForScore(score);
                        scoreLabel.setText("Overall risk score: " + score + "/100 (" + label + ")");

                        // Track number of uses
                        summarizationCount++;

                        // --- Offer quiz only after 3 uses & only once ---
                        if (summarizationCount >= 3 && !quizAlreadyOffered) {
                            quizAlreadyOffered = true; // prevent future prompts

                            int ask = JOptionPane.showConfirmDialog(
                                    this,
                                    "You've analyzed a few Terms now — want to try a quick 'Spot the Red Flag' quiz?",
                                    "Try Quiz?",
                                    JOptionPane.YES_NO_OPTION
                            );

                            if (ask == JOptionPane.YES_OPTION) {
                                // Delay so UI feels calmer
                                new javax.swing.Timer(700, evt -> {
                                    QuizDialog.showQuiz(this, flags);
                                }) {{
                                    setRepeats(false);
                                    start();
                                }};
                            }
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setVisible(false);
                        JOptionPane.showMessageDialog(this,
                                "Error during summarization: " + ex.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    });
                }
            }).start();
        });

    }

    // --- URL fetch + HTML→text helpers ---

    private String fetchTosFromUrl(String url) throws Exception {
        String html = WebFetcher.fetch(url);   // uses your redirect + UA logic
        return htmlToPlainText(html);
    }

    private String htmlToPlainText(String html) {
        if (html == null || html.isBlank()) return "";

        // strip scripts and styles
        String noScript = html.replaceAll("(?is)<script.*?>.*?</script>", " ");
        String noStyle  = noScript.replaceAll("(?is)<style.*?>.*?</style>", " ");

        // convert some tags to newlines
        String withBreaks = noStyle
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n\n");

        // remove all remaining tags
        String textOnly = withBreaks.replaceAll("(?s)<[^>]+>", " ");

        // collapse whitespace
        return textOnly
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    // Color-coded flag renderer
    private static class FlagCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {

            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);

            if (value instanceof FlagResult flag) {
                String text = "<html><b>" + flag.getTitle() + "</b> "
                        + "<i>(" + flag.getCategory() + ")</i><br/>"
                        + flag.getExplanation();
                if (!flag.getSnippet().isEmpty()) {
                    text += "<br/><span style='font-size: 10px; color: #555;'>"
                            + "… " + flag.getSnippet() + " …</span>";
                }
                text += "</html>";
                label.setText(text);

                if (!isSelected) {
                    switch (flag.getSeverity()) {
                        case HIGH -> label.setForeground(new Color(180, 0, 0));      // red
                        case MEDIUM -> label.setForeground(new Color(200, 120, 0));  // orange
                        case LOW -> label.setForeground(new Color(0, 90, 0));        // green
                    }
                }
            }

            label.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            return label;
        }
    }

    private static class ModernButton extends JButton {
        ModernButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setForeground(Color.WHITE);
            setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));

            setFont(new Font("Inter", Font.BOLD, 14));

            addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    setForeground(new Color(0xF5, 0xBD, 0xE6)); // pink
                }
                public void mouseExited(java.awt.event.MouseEvent e) {
                    setForeground(Color.WHITE);
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            GradientPaint gp = new GradientPaint(
                0, 0, new Color(0x8A, 0xAD, 0xF4),   // blue
                getWidth(), getHeight(), new Color(0xC6, 0xA0, 0xF6) // mauve
            );
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Legend panel for flag colors
    private JPanel buildLegendPanel() {
        JPanel legend = new JPanel();
        legend.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 2));

        legend.add(makeLegendLabel("● High Risk", new Color(180, 0, 0)));
        legend.add(makeLegendLabel("● Medium Risk",  new Color(200, 120, 0)));
        legend.add(makeLegendLabel("● Low Risk", new Color(0, 90, 0)));

        return legend;
    }

    private JLabel makeLegendLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(new Font("SansSerif", Font.PLAIN, 11));
        return label;
    }
}