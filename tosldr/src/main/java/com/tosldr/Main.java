package com.tosldr;

import javax.swing.SwingUtilities;
import com.tosldr.gui.MainGUI;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainGUI::new);
    }
}