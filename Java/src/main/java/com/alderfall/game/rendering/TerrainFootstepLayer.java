package com.alderfall.game;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class TerrainFootstepLayer {
    private static final int MAX_STEPS = 34;
    private static final double STEP_DISTANCE = 0.36;

    private final List<StepEffect> steps = new ArrayList<>();
    private final Random random = new Random(7331);
    private String lastMapId = "";
    private double lastStepX = Double.NaN;
    private double lastStepY = Double.NaN;
    private boolean leftFoot;

    public void tick(GameState state, double playerX, double playerY, boolean moving, int facingDx, int facingDy) {
        steps.removeIf(step -> !step.mapId.equals(state.currentMapId));
        for (Iterator<StepEffect> iterator = steps.iterator(); iterator.hasNext(); ) {
            StepEffect step = iterator.next();
            step.age++;
            if (step.age >= step.life) {
                iterator.remove();
            }
        }
        if (!moving || (state.mode != GameMode.EXPLORE && state.mode != GameMode.DEFENSE)) {
            resetAnchorIfMapChanged(state, playerX, playerY);
            return;
        }
        if (!state.currentMapId.equals(lastMapId) || Double.isNaN(lastStepX) || Double.isNaN(lastStepY)) {
            lastMapId = state.currentMapId;
            lastStepX = playerX;
            lastStepY = playerY;
            return;
        }
        double distance = Math.hypot(playerX - lastStepX, playerY - lastStepY);
        if (distance < STEP_DISTANCE || steps.size() >= MAX_STEPS) {
            return;
        }
        spawnStep(state, playerX, playerY, facingDx, facingDy);
        lastStepX = playerX;
        lastStepY = playerY;
    }

    public void draw(Graphics2D g, WorldRenderer.PropContext context) {
        if (steps.isEmpty()) {
            return;
        }
        Composite oldComposite = g.getComposite();
        for (StepEffect step : steps) {
            if (!step.mapId.equals(context.state().currentMapId)) {
                continue;
            }
            int x = (int) Math.round((step.x - context.camX()) * context.tileSize());
            int y = (int) Math.round((step.y - context.camY()) * context.tileSize());
            if (x < -context.tileSize() || y < -context.tileSize()
                    || x > (context.visibleCols() + 1) * context.tileSize()
                    || y > (context.visibleRows() + 1) * context.tileSize()) {
                continue;
            }
            double progress = step.age / (double) step.life;
            float alpha = (float) ((1.0 - progress) * step.alpha);
            int size = Math.max(2, (int) Math.round(step.size * context.zoom() / 100.0));
            g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0.0f, alpha)));
            g.setColor(step.color);
            if (step.kind == Kind.RIPPLE) {
                int radius = Math.max(size, (int) Math.round(size + progress * context.tileSize() * 0.45));
                g.drawOval(x - radius / 2, y - radius / 4, radius, radius / 2);
            } else if (step.kind == Kind.SNOW) {
                g.fillOval(x - size / 2, y - size / 3, size, Math.max(2, size * 2 / 3));
                g.fillOval(x + size / 3, y - size / 4, Math.max(2, size / 2), Math.max(2, size / 2));
            } else if (step.kind == Kind.GRASS) {
                g.drawArc(x - size / 2, y - size / 2, size, size, 20, 135);
                g.drawArc(x - size / 3, y - size / 3, size * 2 / 3, size * 2 / 3, 195, 110);
            } else {
                g.fillOval(x - size / 2, y - size / 3, size, Math.max(2, size * 2 / 3));
            }
        }
        g.setComposite(oldComposite);
    }

    private void spawnStep(GameState state, double playerX, double playerY, int facingDx, int facingDy) {
        double sideX = facingDy == 0 ? 0.0 : 0.11 * (leftFoot ? -1.0 : 1.0);
        double sideY = facingDx == 0 ? 0.0 : 0.11 * (leftFoot ? 1.0 : -1.0);
        leftFoot = !leftFoot;
        double x = playerX + 0.5 + sideX + randomSigned(0.025);
        double y = playerY + 0.78 + sideY + randomSigned(0.025);
        int tileX = clamp((int) Math.floor(playerX + 0.5), 0, Math.max(0, state.world.width(state.currentMapId) - 1));
        int tileY = clamp((int) Math.floor(playerY + 0.5), 0, Math.max(0, state.world.height(state.currentMapId) - 1));
        char tile = state.world.tileAt(state.currentMapId, tileX, tileY);
        Kind kind = kindFor(tile);
        steps.add(new StepEffect(state.currentMapId, kind, x, y, colorFor(tile, kind), alphaFor(kind), lifeFor(kind), sizeFor(kind)));
    }

    private void resetAnchorIfMapChanged(GameState state, double playerX, double playerY) {
        if (!state.currentMapId.equals(lastMapId)) {
            lastMapId = state.currentMapId;
            lastStepX = playerX;
            lastStepY = playerY;
        }
    }

    private Kind kindFor(char tile) {
        if (tile == 'w' || tile == '~' || tile == 'B') {
            return Kind.RIPPLE;
        }
        if (tile == 'n') {
            return Kind.SNOW;
        }
        if (tile == 'g' || tile == 'f' || tile == 'v' || tile == 'A' || tile == 'y') {
            return Kind.GRASS;
        }
        return Kind.DUST;
    }

    private Color colorFor(char tile, Kind kind) {
        if (kind == Kind.RIPPLE) {
            return new Color(196, 236, 246);
        }
        if (kind == Kind.SNOW) {
            return new Color(236, 245, 249);
        }
        if (kind == Kind.GRASS) {
            return tile == 'v' ? new Color(111, 162, 126) : new Color(149, 191, 102);
        }
        if (tile == 'K' || tile == 'C' || tile == 'G' || tile == 'p' || tile == 'j' || tile == 'l') {
            return new Color(190, 180, 160);
        }
        if (tile == 'D' || tile == 'F' || tile == 'M' || tile == 'R' || tile == 'S' || tile == 'L') {
            return new Color(142, 136, 138);
        }
        return new Color(204, 179, 128);
    }

    private float alphaFor(Kind kind) {
        return switch (kind) {
            case RIPPLE -> 0.42f;
            case SNOW -> 0.34f;
            case GRASS -> 0.32f;
            case DUST -> 0.28f;
        };
    }

    private int lifeFor(Kind kind) {
        return switch (kind) {
            case RIPPLE -> 32;
            case SNOW -> 38;
            case GRASS -> 24;
            case DUST -> 30;
        };
    }

    private int sizeFor(Kind kind) {
        return switch (kind) {
            case RIPPLE -> 11;
            case SNOW -> 10;
            case GRASS -> 13;
            case DUST -> 9;
        };
    }

    private double randomSigned(double amount) {
        return (random.nextDouble() * 2.0 - 1.0) * amount;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum Kind {
        DUST,
        GRASS,
        RIPPLE,
        SNOW
    }

    private static final class StepEffect {
        private final String mapId;
        private final Kind kind;
        private final double x;
        private final double y;
        private final Color color;
        private final float alpha;
        private final int life;
        private final int size;
        private int age;

        private StepEffect(String mapId, Kind kind, double x, double y, Color color, float alpha, int life, int size) {
            this.mapId = mapId;
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.color = color;
            this.alpha = alpha;
            this.life = life;
            this.size = size;
        }
    }
}
