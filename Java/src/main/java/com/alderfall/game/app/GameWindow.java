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
    private GamePanel panel;
    private final GraphicsDevice device;
    private Rectangle windowedBounds;
    private boolean windowedResizable;
    private boolean fullscreen;

    private GameWindow(Path javaRoot,GamePanel.Prepared prepared) {
        this.frame = new JFrame("Echoes of Alderfall - Java");
        this.panel = new GamePanel(javaRoot,true,prepared);
        panel.setReplacementHandler(this::replacePanel);
        this.device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    }

    public static void launch(Path javaRoot) {
        SwingUtilities.invokeLater(() -> {try{openOnEventThread(javaRoot);}catch(RuntimeException ex){javax.swing.JOptionPane.showMessageDialog(null,ex.getMessage(),"Could not start Alderfall",javax.swing.JOptionPane.ERROR_MESSAGE);}});
    }

    public static void openOnEventThread(Path javaRoot) {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Open game windows on the Swing event thread.");
        LoadingScreen.run(null,"Starting Alderfall",progress -> GamePanel.prepare(javaRoot,progress),prepared -> new GameWindow(javaRoot,prepared).show());
    }

    public static void launchPlaytest(Path javaRoot, com.alderfall.game.editor.MapDocument document) {
        SwingUtilities.invokeLater(() -> {
            try{
                LoadingScreen.run(null,"Opening map playtest",progress -> GamePanel.prepare(javaRoot,progress),prepared -> {
                    GameWindow window=new GameWindow(javaRoot,prepared);
                    try{window.panel.startEditorPlaytest(document);window.show();}
                    catch(RuntimeException ex){window.panel.shutdown();window.frame.dispose();throw ex;}
                });
            }catch(RuntimeException ex){javax.swing.JOptionPane.showMessageDialog(null,ex.getMessage(),"Map import",javax.swing.JOptionPane.ERROR_MESSAGE);}
        });
    }

    private void replacePanel(GamePanel next){
        panel=next;panel.setReplacementHandler(this::replacePanel);panel.setFullscreenToggle(this::toggleFullscreen);
        frame.setContentPane(panel);frame.validate();panel.requestFocusInWindow();
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
        SwingUtilities.invokeLater(panel::requestFocusInWindow);
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
