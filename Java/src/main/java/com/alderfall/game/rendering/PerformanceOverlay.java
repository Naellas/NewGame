package com.alderfall.game;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

/** EDT-owned diagnostics. Spatial timings are estimates distributed over draw bounds. */
final class PerformanceOverlay {
    static final int COLS = 12, ROWS = 8;
    private int mode;
    private int width, height;
    private final double[] cells = new double[COLS * ROWS];
    private final double[] displayedCells = new double[COLS * ROWS];
    private long previousPaint, windowStart, intervalTotal, intervalMax;
    private int intervals;
    private double fps, frameMs, worstMs, paintMs;
    private Map<String, Long> timings = Map.of();

    boolean enabled() { return mode != 0; }
    boolean heatmap() { return mode == 2; }
    String modeLabel() { return switch (mode) { case 1 -> "Stats"; case 2 -> "Heatmap"; default -> "Off"; }; }
    void cycle() {
        cycle(1);
    }

    void cycle(int direction) {
        mode = Math.floorMod(mode + direction, 3);
        previousPaint = windowStart = intervalTotal = intervalMax = 0;
        intervals = 0;
        fps = frameMs = worstMs = paintMs = 0;
        timings = Map.of();
        Arrays.fill(cells, 0);
        Arrays.fill(displayedCells, 0);
    }

    void beginFrame(int width, int height) {
        this.width = width;
        this.height = height;
        Arrays.fill(cells, 0);
    }

    long startRegion() { return heatmap() ? System.nanoTime() : 0; }

    void endRegion(Graphics2D g, Rectangle bounds, long start) {
        if (start == 0 || !heatmap()) return;
        long nanos = System.nanoTime() - start;
        Rectangle clip = g.getClipBounds();
        if (clip != null) bounds = bounds.intersection(clip);
        addRegion(g.getTransform().createTransformedShape(bounds).getBounds(), nanos);
    }

    void addRegion(Rectangle bounds, long nanos) {
        if (!heatmap() || width <= 0 || height <= 0 || nanos <= 0) return;
        Rectangle visible = bounds.intersection(new Rectangle(0, 0, width, height));
        if (visible.isEmpty()) return;
        double area = (double) visible.width * visible.height;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                Rectangle cell = cellBounds(col, row);
                Rectangle overlap = cell.intersection(visible);
                if (!overlap.isEmpty()) cells[row * COLS + col] += nanos * ((double) overlap.width * overlap.height / area);
            }
        }
    }

    private Rectangle cellBounds(int col, int row) {
        int x = col * width / COLS, y = row * height / ROWS;
        return new Rectangle(x, y, (col + 1) * width / COLS - x, (row + 1) * height / ROWS - y);
    }

    void finishFrame(long now, long paintNanos, Map<String, Long> stages) {
        if (!enabled()) return;
        if (previousPaint != 0) {
            long interval = now - previousPaint;
            intervalTotal += interval;
            intervalMax = Math.max(intervalMax, interval);
            intervals++;
        } else windowStart = now;
        previousPaint = now;
        // Current-frame spatial samples deliberately do not trail moving objects/cameras.
        System.arraycopy(cells, 0, displayedCells, 0, cells.length);
        if (now - windowStart >= 500_000_000L && intervals > 0) {
            fps = intervals * 1_000_000_000.0 / intervalTotal;
            frameMs = intervalTotal / (intervals * 1_000_000.0);
            worstMs = intervalMax / 1_000_000.0;
            paintMs = paintNanos / 1_000_000.0;
            timings = Map.copyOf(stages);
            windowStart = now;
            intervalTotal = intervalMax = 0;
            intervals = 0;
        }
    }

    double fps() { return fps; }
    double sampledNanos() { return Arrays.stream(cells).sum(); }

    void draw(Graphics2D graphics) {
        if (!enabled()) return;
        Graphics2D g = (Graphics2D) graphics.create();
        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        if (heatmap()) {
            double peak = Math.max(1, Arrays.stream(displayedCells).max().orElse(0));
            for (int row = 0; row < ROWS; row++) for (int col = 0; col < COLS; col++) {
                double cost = displayedCells[row * COLS + col];
                if (cost <= 0) continue;
                Rectangle cell = cellBounds(col, row);
                Color color = Color.getHSBColor((float) (0.34 * (1 - cost / peak)), 0.85f, 1);
                g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 65));
                g.fill(cell);
                g.setColor(new Color(255, 255, 255, 100));
                g.draw(cell);
                g.setColor(Color.WHITE);
                g.drawString(String.format(Locale.ROOT, "%.2f ms", cost / 1_000_000.0), cell.x + 4, cell.y + 15);
            }
        }
        Runtime runtime = Runtime.getRuntime();
        String[] lines = {
            "PERFORMANCE  [F4: Off / Stats / Heatmap]",
            String.format(Locale.ROOT, "Paint FPS %.1f | interval %.1f ms | worst %.1f ms", fps, frameMs, worstMs),
            String.format(Locale.ROOT, "Paint %.1f ms | update %.1f ms | world %.1f ms", paintMs, ms("update"), ms("world")),
            String.format(Locale.ROOT, "Terrain %.1f | scenery %.1f | lighting %.1f ms", ms("world.terrain"), ms("world.scenery"), ms("world.lighting")),
            String.format(Locale.ROOT, "Heap %d / %d MiB (used / committed), max %d", (runtime.totalMemory() - runtime.freeMemory()) / 1048576, runtime.totalMemory() / 1048576, runtime.maxMemory() / 1048576),
            heatmap() ? "Heat: sampled objects + UI; green low -> red peak" : "Timings refresh every 0.5s; paint FPS excludes display sync",
            heatmap() ? "Time spread over bounds; blank = unmeasured, not free" : "F4 adds regional render-cost estimates",
            "Java2D elapsed time, not per-region CPU/GPU or memory"
        };
        int boxWidth = Math.min(width - 16, 580);
        int y = Math.max(8, height - 166);
        g.setColor(new Color(8, 12, 20, 230));
        g.fillRoundRect(8, y, boxWidth, 154, 10, 10);
        g.clipRect(8, y, boxWidth, 154);
        for (int i = 0; i < lines.length; i++) {
            g.setColor(i == 0 ? new Color(244, 213, 141) : Color.WHITE);
            g.drawString(lines[i], 18, y + 20 + i * 17);
        }
        g.dispose();
    }

    private double ms(String stage) { return timings.getOrDefault(stage, 0L) / 1_000_000.0; }
}
