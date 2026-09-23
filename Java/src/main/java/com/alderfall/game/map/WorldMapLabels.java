package com.alderfall.game.map;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** One collision budget for every map label, with quest destinations first. */
public final class WorldMapLabels {
    public record Placed(String text, Rectangle bounds) {}
    private record Request(String text, int x, int y, Color color, int priority) {}
    private final Rectangle map;
    private final List<Rectangle> occupied = new ArrayList<>();
    private final List<Request> requests = new ArrayList<>();

    public WorldMapLabels(WorldMapViewport viewport) {
        map = new Rectangle(viewport.screenX() + 5, viewport.screenY() + 5,
                viewport.screenW() - 10, viewport.screenH() - 10);
    }

    public void reserve(Rectangle bounds) { occupied.add(new Rectangle(bounds)); }

    public void add(String text, int x, int y, Color color, int priority) {
        if (text != null && !text.isBlank()) requests.add(new Request(text, x, y, color, priority));
    }

    public List<Placed> draw(Graphics2D g) {
        List<Placed> placed = new ArrayList<>();
        requests.sort(Comparator.comparingInt(Request::priority).reversed());
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        FontMetrics fm = g.getFontMetrics();
        int height = fm.getHeight() + 6;
        List<Request> drawn = new ArrayList<>();
        for (Request request : requests) {
            if (drawn.stream().anyMatch(other -> other.text().equals(request.text())
                    && Math.hypot(other.x() - request.x(), other.y() - request.y()) < 48)) continue;
            int width = fm.stringWidth(request.text()) + 12;
            Rectangle chosen = null;
            for (int dy : new int[]{-height - 5, 7, -height / 2, -height * 2 - 8, height + 12, -height * 3 - 12}) {
                for (int dx : new int[]{12, -width - 12, -width / 2}) {
                    Rectangle candidate = new Rectangle(request.x() + dx, request.y() + dy, width, height);
                    if (!map.contains(candidate) || occupied.stream().anyMatch(r -> r.intersects(candidate))) continue;
                    chosen = candidate;
                    break;
                }
                if (chosen != null) break;
            }
            if (chosen == null) continue; // Icons and their hover descriptions remain available.
            int endX = Math.max(chosen.x, Math.min(request.x(), chosen.x + chosen.width));
            int endY = Math.max(chosen.y, Math.min(request.y(), chosen.y + chosen.height));
            g.setColor(new Color(12, 23, 28, 180));
            g.drawLine(request.x(), request.y(), endX, endY);
            g.setColor(new Color(13, 24, 30, 232));
            g.fillRoundRect(chosen.x, chosen.y, width, height, 7, 7);
            g.setColor(new Color(request.color().getRed(), request.color().getGreen(), request.color().getBlue(), 95));
            g.drawRoundRect(chosen.x, chosen.y, width, height, 7, 7);
            g.setColor(request.color());
            g.drawString(request.text(), chosen.x + 6, chosen.y + 3 + fm.getAscent());
            placed.add(new Placed(request.text(), chosen));
            drawn.add(request);
            Rectangle padded = new Rectangle(chosen);
            padded.grow(4, 3);
            occupied.add(padded);
        }
        return placed;
    }
}
