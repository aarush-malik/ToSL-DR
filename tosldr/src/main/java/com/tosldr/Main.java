package com.tosldr;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.tosldr.gui.MainGUI;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(MainGUI::new);
    }
}