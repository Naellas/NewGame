package com.alderfall.game;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;

final class RenderBackBuffer {
    private static final String VOLATILE_PROPERTY = "alderfall.volatileBackBuffer";

    private final boolean preferVolatile;
    private BufferedImage bufferedImage;
    private VolatileImage volatileImage;
    private int width;
    private int height;

    private RenderBackBuffer(boolean preferVolatile) {
        this.preferVolatile = preferVolatile;
    }

    static RenderBackBuffer create() {
        return new RenderBackBuffer(Boolean.parseBoolean(System.getProperty(VOLATILE_PROPERTY, "true")));
    }

    Graphics2D createGraphics(Component component, int width, int height) {
        ensure(component, width, height);
        if (volatileImage != null) {
            int validation = volatileImage.validate(component.getGraphicsConfiguration());
            if (validation == VolatileImage.IMAGE_INCOMPATIBLE) {
                volatileImage.flush();
                volatileImage = createVolatile(component, width, height);
            }
            if (volatileImage != null) {
                return volatileImage.createGraphics();
            }
        }
        if (bufferedImage == null || bufferedImage.getWidth() != width || bufferedImage.getHeight() != height) {
            bufferedImage = new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_ARGB);
        }
        return bufferedImage.createGraphics();
    }

    void drawTo(Graphics2D g, int x, int y, int width, int height) {
        Image image = volatileImage != null ? volatileImage : bufferedImage;
        if (image != null) {
            g.drawImage(image, x, y, width, height, null);
        }
    }

    boolean contentsLost() {
        return volatileImage != null && volatileImage.contentsLost();
    }

    private void ensure(Component component, int width, int height) {
        width = Math.max(1, width);
        height = Math.max(1, height);
        if (this.width == width && this.height == height && (volatileImage != null || bufferedImage != null)) {
            return;
        }
        this.width = width;
        this.height = height;
        if (volatileImage != null) {
            volatileImage.flush();
            volatileImage = null;
        }
        bufferedImage = null;
        if (preferVolatile) {
            volatileImage = createVolatile(component, width, height);
        }
        if (volatileImage == null) {
            bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }
    }

    private VolatileImage createVolatile(Component component, int width, int height) {
        if (component.getGraphicsConfiguration() == null) {
            return null;
        }
        try {
            return component.createVolatileImage(width, height);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
