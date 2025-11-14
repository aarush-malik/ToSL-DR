package com.tosldr.gui;

import com.tosldr.services.FlagAnalyzer;
import com.tosldr.services.FlagResult;
import com.tosldr.services.HuggingFaceClient;
import com.tosldr.services.SummarizerService;

import javax.swing.*;
import java.awt.*;

public class MainGUI extends JFrame {

    private JTextArea inputArea;
    private JTextField urlField;
    private JButton fetchButton;
    private JButton summarizeButton;
    private JTextArea summaryArea;
    private JList<FlagResult> flagsList;
    private JProgressBar progressBar;

    private final SummarizerService summarizerService;
    private final FlagAnalyzer flagAnalyzer;

    public MainGUI() {
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
        fetchButton = new JButton("Fetch ToS from URL");
        urlPanel.setBorder(BorderFactory.createTitledBorder("Load from URL"));
        urlPanel.add(urlField, BorderLayout.CENTER);
        urlPanel.add(fetchButton, BorderLayout.EAST);

        // Left: input text (EDITABLE)
        inputArea = new JTextArea();
        inputArea.setEditable(true);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);

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

        // Right bottom: flags list with color-coded renderer
        flagsList = new JList<>();
        flagsList.setCellRenderer(new FlagCellRenderer());

        // click flag → jump to snippet in ToS
        flagsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                FlagResult flag = flagsList.getSelectedValue();
                if (flag == null || flag.getSnippet().isEmpty()) return;

                String full = inputArea.getText();
                int idx = full.indexOf(flag.getSnippet());
                if (idx >= 0) {
                    inputArea.requestFocusInWindow();
                    inputArea.setCaretPosition(idx);
                    inputArea.select(idx, idx + flag.getSnippet().length());
                }
            }
        });

        JScrollPane flagsScroll = new JScrollPane(flagsList);

        JPanel flagsPanel = new JPanel(new BorderLayout());
        flagsPanel.setBorder(
                BorderFactory.createTitledBorder("Risk & Key Clauses (color-coded)")
        );
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
        summarizeButton = new JButton("Summarize & Analyze");
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

        // TEMP fetch action
        fetchButton.addActionListener(e -> {
            String url = urlField.getText().trim();
            if (url.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter a URL first.");
            } else {
                inputArea.setText(
                        "Mock fetched content from: " + url + "\n\n(real fetch coming later)");
            }
        });

        // Summarize button
        summarizeButton.addActionListener(e -> {
            String text = inputArea.getText().trim();
            if (text.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Paste ToS text or load from URL first.");
                return;
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

    // Legend panel for flag colors
    private JPanel buildLegendPanel() {
        JPanel legend = new JPanel();
        legend.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 2));

        legend.add(makeLegendLabel("● High risk", new Color(180, 0, 0)));
        legend.add(makeLegendLabel("● Medium",  new Color(200, 120, 0)));
        legend.add(makeLegendLabel("● Low / info", new Color(0, 90, 0)));

        return legend;
    }

    private JLabel makeLegendLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(new Font("SansSerif", Font.PLAIN, 11));
        return label;
    }
}