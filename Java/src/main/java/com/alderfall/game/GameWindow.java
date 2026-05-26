package com.alderfall.game;

import java.nio.file.Path;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public final class GameWindow {
    private GameWindow() {
    }

    public static void launch(Path javaRoot, Path pythonRoot) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Echoes of Alderfall - Java");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.setContentPane(new GamePanel(javaRoot, pythonRoot));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
