package com.alderfall.game;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public final class GameWindow {
    private final JFrame frame;
    private final GamePanel panel;
    private final GraphicsDevice device;
    private Rectangle windowedBounds;
    private boolean windowedResizable;
    private boolean fullscreen;

    private GameWindow(Path javaRoot) {
        this.frame = new JFrame("Echoes of Alderfall - Java");
        this.panel = new GamePanel(javaRoot);
        this.device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    }

    public static void launch(Path javaRoot) {
        SwingUtilities.invokeLater(() -> new GameWindow(javaRoot).show());
    }

    private void show() {
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                panel.shutdown();
            }
        });
        frame.setResizable(false);
        frame.setContentPane(panel);
        panel.setFullscreenToggle(this::toggleFullscreen);
        installFullscreenKeys();
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        panel.requestFocusInWindow();
    }

    private void installFullscreenKeys() {
        String actionName = "toggleFullscreen";
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), actionName);
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK), actionName);
        frame.getRootPane().getActionMap().put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                toggleFullscreen();
            }
        });
    }

    private void toggleFullscreen() {
        setFullscreen(!fullscreen);
    }

    private void setFullscreen(boolean enabled) {
        if (fullscreen == enabled) {
            return;
        }
        if (enabled) {
            windowedBounds = frame.getBounds();
            windowedResizable = frame.isResizable();
            frame.dispose();
            frame.setResizable(false);
            frame.setUndecorated(true);
            frame.setExtendedState(JFrame.NORMAL);
            frame.setBounds(device.getDefaultConfiguration().getBounds());
            frame.setVisible(true);
            fullscreen = true;
        } else {
            frame.dispose();
            frame.setUndecorated(false);
            frame.setResizable(windowedResizable);
            frame.setExtendedState(JFrame.NORMAL);
            if (windowedBounds == null) {
                frame.pack();
                frame.setLocationRelativeTo(null);
            } else {
                frame.setBounds(windowedBounds);
            }
            frame.setVisible(true);
            fullscreen = false;
        }
        frame.validate();
        panel.requestFocusInWindow();
    }
}
