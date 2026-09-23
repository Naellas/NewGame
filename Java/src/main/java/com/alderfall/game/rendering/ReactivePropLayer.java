package com.alderfall.game;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ReactivePropLayer {
    private static final int PULSE_LIFE = 34;
    private static final int MAX_PULSES = 28;

    private final List<PropPulse> pulses = new ArrayList<>();
    private String lastMapId = "";
    private int lastPlayerX = Integer.MIN_VALUE;
    private int lastPlayerY = Integer.MIN_VALUE;

    public void tick(GameState state, List<WorldProp> nearbyProps) {
        pulses.removeIf(pulse -> !pulse.mapId.equals(state.currentMapId));
        for (Iterator<PropPulse> iterator = pulses.iterator(); iterator.hasNext(); ) {
            PropPulse pulse = iterator.next();
            pulse.age++;
            if (pulse.age >= PULSE_LIFE) {
                iterator.remove();
            }
        }
        if (state.mode != GameMode.EXPLORE && state.mode != GameMode.DEFENSE) {
            return;
        }
        boolean moved = !state.currentMapId.equals(lastMapId) || state.playerX != lastPlayerX || state.playerY != lastPlayerY;
        lastMapId = state.currentMapId;
        lastPlayerX = state.playerX;
        lastPlayerY = state.playerY;
        if (!moved || pulses.size() >= MAX_PULSES) {
            return;
        }
        for (WorldProp prop : nearbyProps) {
            if (prop.visualSlot() >= 0) continue;
            if (!isReactiveAsset(prop.asset())) {
                continue;
            }
            int dx = prop.x() - state.playerX;
            int dy = prop.y() - state.playerY;
            int distance = Math.abs(dx) + Math.abs(dy);
            if (distance > 2) {
                continue;
            }
            int seed = Math.abs(prop.asset().hashCode() + prop.x() * 928371 + prop.y() * 364479 + state.worldTick);
            if (distance == 0 || seed % 3 != 0) {
                addPulse(state, prop, effectKind(prop.asset(), distance));
            }
            if (pulses.size() >= MAX_PULSES) {
                return;
            }
        }
    }

    public void draw(Graphics2D g, WorldRenderer.PropContext context, List<WorldProp> visibleProps) {
        if (pulses.isEmpty()) {
            return;
        }
        Composite oldComposite = g.getComposite();
        Stroke oldStroke = g.getStroke();
        for (PropPulse pulse : pulses) {
            if (!pulse.mapId.equals(context.state().currentMapId)) {
                continue;
            }
            if (!visibleNear(pulse, context)) {
                continue;
            }
            double progress = pulse.age / (double) PULSE_LIFE;
            int baseX = (pulse.x - context.camX()) * context.tileSize();
            int baseY = (pulse.y - context.camY()) * context.tileSize();
            int cx = baseX + (int) Math.round(pulse.anchorX * context.tileSize());
            int cy = baseY + (int) Math.round(pulse.anchorY * context.tileSize());
            int radius = Math.max(4, (int) Math.round((0.28 + progress * 0.56) * context.tileSize()));
            float alpha = (float) ((1.0 - progress) * pulse.alpha);
            g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0.0f, alpha)));
            g.setColor(pulse.color);
            if (pulse.kind == Kind.BRUSH) {
                g.setStroke(new BasicStroke(Math.max(1f, context.zoom() / 55f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawArc(cx - radius / 2, cy - radius / 2, radius, radius, 20, 220);
                g.drawArc(cx - radius / 3, cy - radius / 3, radius * 2 / 3, radius * 2 / 3, 205, 130);
            } else if (pulse.kind == Kind.SPARK) {
                int sparks = 5;
                for (int i = 0; i < sparks; i++) {
                    double angle = progress * 2.5 + i * Math.PI * 2.0 / sparks + pulse.seed * 0.01;
                    int px = cx + (int) Math.round(Math.cos(angle) * radius * 0.65);
                    int py = cy + (int) Math.round(Math.sin(angle) * radius * 0.45) - (int) Math.round(progress * context.tileSize() * 0.22);
                    int size = Math.max(2, context.tileSize() / 12);
                    g.fillOval(px - size / 2, py - size / 2, size, size);
                }
            } else {
                g.setStroke(new BasicStroke(Math.max(1f, context.zoom() / 48f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawOval(cx - radius / 2, cy - radius / 4, radius, radius / 2);
            }
        }
        g.setStroke(oldStroke);
        g.setComposite(oldComposite);
    }

    private void addPulse(GameState state, WorldProp prop, Kind kind) {
        PropPlacement.Placement placement = PropPlacement.at(state.world, state.currentMapId, prop);
        pulses.add(new PropPulse(
                state.currentMapId,
                prop.x(),
                prop.y(),
                placement.kind() == PropPlacement.Kind.FIXED ? 0.5 : placement.x(),
                placement.kind() == PropPlacement.Kind.FIXED ? 0.5 : placement.y(),
                prop.asset().hashCode() ^ prop.x() * 7349 ^ prop.y() * 9127,
                kind,
                colorFor(prop.asset(), kind),
                kind == Kind.SPARK ? 0.58f : 0.38f
        ));
    }

    private Kind effectKind(String asset, int distance) {
        if (asset.contains("fire") || asset.contains("forge") || asset.contains("oven")
                || asset.contains("portal") || asset.contains("rune") || asset.contains("crystal")
                || asset.contains("shrine") || asset.contains("lantern") || asset.contains("candle")) {
            return Kind.SPARK;
        }
        if (asset.contains("lily") || asset.contains("water") || asset.contains("duckweed")
                || asset.contains("reed") || asset.contains("cattail")) {
            return Kind.RIPPLE;
        }
        return distance == 0 ? Kind.RIPPLE : Kind.BRUSH;
    }

    private Color colorFor(String asset, Kind kind) {
        if (kind == Kind.SPARK) {
            if (asset.contains("portal") || asset.contains("rune") || asset.contains("crystal") || asset.contains("shrine")) {
                return new Color(122, 210, 255);
            }
            return new Color(255, 206, 101);
        }
        if (kind == Kind.RIPPLE) {
            return new Color(172, 224, 232);
        }
        return new Color(174, 218, 128);
    }

    private boolean isReactiveAsset(String asset) {
        return asset.contains("grass")
                || asset.contains("reed")
                || asset.contains("flower")
                || asset.contains("bush")
                || asset.contains("tree")
                || asset.contains("pine")
                || asset.contains("plant")
                || asset.contains("lily")
                || asset.contains("water")
                || asset.contains("duckweed")
                || asset.contains("cattail")
                || asset.contains("fire")
                || asset.contains("forge")
                || asset.contains("oven")
                || asset.contains("portal")
                || asset.contains("rune")
                || asset.contains("crystal")
                || asset.contains("shrine")
                || asset.contains("lantern")
                || asset.contains("candle");
    }

    private boolean visibleNear(PropPulse pulse, WorldRenderer.PropContext context) {
        return pulse.x >= context.camX() - 1
                && pulse.y >= context.camY() - 1
                && pulse.x < context.camX() + context.visibleCols() + 1
                && pulse.y < context.camY() + context.visibleRows() + 1;
    }

    private enum Kind {
        BRUSH,
        RIPPLE,
        SPARK
    }

    private static final class PropPulse {
        private final String mapId;
        private final int x;
        private final int y;
        private final double anchorX;
        private final double anchorY;
        private final int seed;
        private final Kind kind;
        private final Color color;
        private final float alpha;
        private int age;

        private PropPulse(String mapId, int x, int y, double anchorX, double anchorY, int seed, Kind kind, Color color, float alpha) {
            this.mapId = mapId;
            this.x = x;
            this.y = y;
            this.anchorX = anchorX;
            this.anchorY = anchorY;
            this.seed = seed;
            this.kind = kind;
            this.color = color;
            this.alpha = alpha;
        }
    }
}
