package com.alderfall.game;

import com.alderfall.game.render.world.WorldLightingRenderer;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import javax.swing.JPanel;

/** Pixel and invalidation checks for rendering caches, runnable without a display. */
public final class RenderCacheTest {
    private RenderCacheTest() { }

    public static void main(String[] args) throws Exception {
        checkOpaqueTerrain();
        checkBackBuffer();
        checkVignette();
        System.out.println("Render cache checks passed.");
    }

    private static void checkOpaqueTerrain() throws Exception {
        WorldRenderer renderer = new WorldRenderer(null, null, null, null);
        Method convert = WorldRenderer.class.getDeclaredMethod("opaqueTerrainImage", BufferedImage.class);
        convert.setAccessible(true);
        BufferedImage source = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                source.setRGB(x, y, 0xff000000 | x * 31 << 16 | y * 31 << 8 | 123);
            }
        }
        BufferedImage opaque = (BufferedImage) convert.invoke(renderer, source);
        require(opaque.getType() == BufferedImage.TYPE_INT_RGB, "Opaque terrain was not converted");
        compare(source, opaque, 0);
        source.setRGB(7, 7, 0x80776655);
        BufferedImage transparent = (BufferedImage) convert.invoke(renderer, source);
        require(transparent == source, "Transparent terrain must retain its alpha channel");
    }

    private static void checkBackBuffer() {
        RenderBackBuffer buffer = RenderBackBuffer.create();
        JPanel component = new JPanel();
        for (int size : new int[]{16, 24, 8}) {
            Graphics2D graphics = buffer.createGraphics(component, size, size);
            graphics.setColor(new Color(21, 47, 93));
            graphics.fillRect(0, 0, size, size);
            graphics.dispose();
            BufferedImage target = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            graphics = target.createGraphics();
            buffer.drawTo(graphics, 0, 0, size, size);
            graphics.dispose();
            require(target.getRGB(size - 1, size - 1) == 0xff152f5d, "Back buffer resize/presentation failed");
        }
    }

    private static void checkVignette() throws Exception {
        GameState state = new GameState(GameConfig.load(Path.of("").toAbsolutePath().normalize()));
        int[] dimensions = {320, 180};
        WorldLightingRenderer.Effects effects = (WorldLightingRenderer.Effects) Proxy.newProxyInstance(
                WorldLightingRenderer.Effects.class.getClassLoader(),
                new Class<?>[]{WorldLightingRenderer.Effects.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "gameAreaWidth" -> dimensions[0];
                    case "viewHeight" -> dimensions[1];
                    default -> throw new AssertionError("Unexpected callback: " + method.getName());
                });
        WorldLightingRenderer renderer = new WorldLightingRenderer(state, effects);
        Field cache = WorldLightingRenderer.class.getDeclaredField("vignetteCache");
        cache.setAccessible(true);
        state.worldTick = 0;
        drawAndCompareVignette(renderer, dimensions);
        Object nightCache = cache.get(renderer);
        drawAndCompareVignette(renderer, dimensions);
        require(cache.get(renderer) == nightCache, "Unchanged vignette was rebuilt");
        // Derive noon from the public clock, avoiding assumptions about ticks per day.
        while (state.timeOfDayMinutes() < 720) {
            state.worldTick++;
        }
        drawAndCompareVignette(renderer, dimensions);
        require(cache.get(renderer) != nightCache, "Daylight change did not invalidate vignette");
        Object dayCache = cache.get(renderer);
        dimensions[0] += 73;
        dimensions[1] += 41;
        drawAndCompareVignette(renderer, dimensions);
        require(cache.get(renderer) != dayCache, "Viewport resize did not invalidate vignette");
    }

    private static void drawAndCompareVignette(WorldLightingRenderer renderer, int[] dimensions) {
        int width = dimensions[0];
        int height = dimensions[1];
        BufferedImage actual = backdrop(width, height);
        BufferedImage expected = backdrop(width, height);
        Graphics2D graphics = actual.createGraphics();
        graphics.setComposite(AlphaComposite.SrcOver.derive(0.5f));
        graphics.setColor(Color.MAGENTA);
        renderer.drawWorldVignette(graphics);
        require(graphics.getColor().equals(Color.MAGENTA), "Vignette changed caller paint");
        require(graphics.getComposite().equals(AlphaComposite.SrcOver.derive(0.5f)), "Vignette changed caller composite");
        graphics.dispose();
        graphics = expected.createGraphics();
        float alpha = 0.10f + renderer.nightFactor() * 0.12f;
        graphics.setPaint(new RadialGradientPaint(width * 0.46f, height * 0.42f,
                Math.max(width, height) * 0.68f, new float[]{0.0f, 0.74f, 1.0f},
                new Color[]{new Color(0, 0, 0, 0), new Color(10, 13, 20, Math.round(alpha * 70)),
                        new Color(8, 10, 18, Math.round(alpha * 255))}));
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        // Rendering through a transparent cache may round color channels by one level.
        compare(expected, actual, 1);
    }

    private static BufferedImage backdrop(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(71, 114, 163));
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        return image;
    }

    private static void compare(BufferedImage expected, BufferedImage actual, int tolerance) {
        for (int y = 0; y < expected.getHeight(); y++) {
            for (int x = 0; x < expected.getWidth(); x++) {
                int a = expected.getRGB(x, y);
                int b = actual.getRGB(x, y);
                for (int shift : new int[]{0, 8, 16, 24}) {
                    require(Math.abs(((a >>> shift) & 255) - ((b >>> shift) & 255)) <= tolerance,
                            "Pixel mismatch at " + x + "," + y);
                }
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
