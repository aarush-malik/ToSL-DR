package com.tosldr.gui;

import javax.swing.*;
import java.awt.*;
import com.tosldr.services.FlagAnalyzer;
import com.tosldr.services.HuggingFaceClient;
import com.tosldr.services.SummarizerService;

public class MainGUI extends JFrame {

    private JTextArea inputArea;
    private JTextField urlField;
    private JButton fetchButton;
    private JButton summarizeButton;
    private JTextArea summaryArea;
    private JTextArea flagsArea;
    private final SummarizerService summarizerService;
    private final FlagAnalyzer flagAnalyzer;


    public MainGUI() {
        setTitle("TosL;DR - Terms of Service Summarizer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);

        // Read Hugging Face token from environment variable HF_TOKEN
        String token = System.getenv("HF_TOKEN");
        HuggingFaceClient client = new HuggingFaceClient(token);
        this.summarizerService = new SummarizerService(client);
        this.flagAnalyzer = new FlagAnalyzer();

        initComponents();
        setVisible(true);
    }

    private void initComponents() {
        // URL bar at top
        JPanel urlPanel = new JPanel(new BorderLayout(5, 5));
        urlField = new JTextField();
        fetchButton = new JButton("Fetch ToS from URL");
        urlPanel.setBorder(BorderFactory.createTitledBorder("Load from URL"));
        urlPanel.add(urlField, BorderLayout.CENTER);
        urlPanel.add(fetchButton, BorderLayout.EAST);

        // Left: input text
        inputArea = new JTextArea();
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputScroll.setBorder(BorderFactory.createTitledBorder("Paste Terms of Service"));

        // Right top: summary
        summaryArea = new JTextArea();
        summaryArea.setEditable(false);
        summaryArea.setLineWrap(true);
        summaryArea.setWrapStyleWord(true);
        JScrollPane summaryScroll = new JScrollPane(summaryArea);
        summaryScroll.setBorder(BorderFactory.createTitledBorder("Plain-Language Summary"));

        // Right bottom: flags
        flagsArea = new JTextArea();
        flagsArea.setEditable(false);
        flagsArea.setLineWrap(true);
        flagsArea.setWrapStyleWord(true);
        JScrollPane flagsScroll = new JScrollPane(flagsArea);
        flagsScroll.setBorder(BorderFactory.createTitledBorder("Red Flags Detected"));

        // Right stacked
        JPanel rightPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        rightPanel.add(summaryScroll);
        rightPanel.add(flagsScroll);

        // Split left/right
        JSplitPane centerSplit = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                inputScroll,
                rightPanel
        );
        centerSplit.setResizeWeight(0.5);

        // Bottom button
        summarizeButton = new JButton("Summarize & Analyze");
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(summarizeButton);

        // Layout
        setLayout(new BorderLayout(5, 5));
        add(urlPanel, BorderLayout.NORTH);
        add(centerSplit, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // TEMP actions so it does something
        fetchButton.addActionListener(e -> {
            String url = urlField.getText().trim();
            if (url.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter a URL first.");
            } else {
                inputArea.setText("Mock fetched content from: " + url + "\n\n(real fetch coming later)");
            }
        });

                summarizeButton.addActionListener(e -> {
            String text = inputArea.getText().trim();
            if (text.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Paste ToS text or load from URL first.");
                return;
            }

            summaryArea.setText("Summarizing, please wait...");
            flagsArea.setText("");

            // Run summarization in a background thread so GUI doesn't freeze
            new Thread(() -> {
                try {
                    String summary = summarizerService.summarize(text);
                    var flags = flagAnalyzer.analyze(text);

                    SwingUtilities.invokeLater(() -> {
                        summaryArea.setText(summary);
                        flagsArea.setText(String.join("\n", flags));
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(this,
                                    "Error during summarization: " + ex.getMessage(),
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE)
                    );
                }
            }).start();
        });
    }
}