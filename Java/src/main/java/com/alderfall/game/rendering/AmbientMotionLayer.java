package com.alderfall.game;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class AmbientMotionLayer {
    private static final int MAX_PARTICLES = 52;
    private static final int SPAWN_RADIUS = 9;

    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random(9241);

    public void tick(GameState state) {
        if (!activeInMode(state)) {
            particles.clear();
            return;
        }
        particles.removeIf(particle -> !particle.mapId.equals(state.currentMapId));
        for (Iterator<Particle> iterator = particles.iterator(); iterator.hasNext(); ) {
            Particle particle = iterator.next();
            particle.age++;
            particle.x += particle.dx;
            particle.y += particle.dy;
            particle.dy += particle.gravity;
            if (particle.age >= particle.life) {
                iterator.remove();
            }
        }
        if (particles.size() < MAX_PARTICLES) {
            int rolls = "interior".equals(state.world.kind(state.currentMapId)) ? 1 : 3;
            for (int i = 0; i < rolls && particles.size() < MAX_PARTICLES; i++) {
                if (random.nextDouble() < spawnChance(state)) {
                    spawnParticle(state);
                }
            }
        }
    }

    public void draw(Graphics2D g, WorldRenderer.PropContext context) {
        if (particles.isEmpty()) {
            return;
        }
        Composite oldComposite = g.getComposite();
        for (Particle particle : particles) {
            if (!particle.mapId.equals(context.state().currentMapId)) {
                continue;
            }
            double sx = (particle.x - context.camX()) * context.tileSize();
            double sy = (particle.y - context.camY()) * context.tileSize();
            if (sx < -context.tileSize() || sy < -context.tileSize()
                    || sx > (context.visibleCols() + 1) * context.tileSize()
                    || sy > (context.visibleRows() + 1) * context.tileSize()) {
                continue;
            }
            double progress = particle.age / (double) particle.life;
            float alpha = (float) Math.max(0.0, particle.alpha * Math.sin(Math.PI * progress));
            if (alpha <= 0.01f) {
                continue;
            }
            int size = Math.max(2, (int) Math.round(particle.size * context.zoom() / 100.0));
            g.setComposite(AlphaComposite.SrcOver.derive(alpha));
            g.setColor(particle.color);
            if (particle.kind == Kind.LEAF) {
                g.fillOval((int) Math.round(sx), (int) Math.round(sy), size + 1, Math.max(2, size - 1));
            } else if (particle.kind == Kind.BUBBLE) {
                g.drawOval((int) Math.round(sx), (int) Math.round(sy), size, size);
            } else {
                g.fillOval((int) Math.round(sx), (int) Math.round(sy), size, size);
            }
        }
        g.setComposite(oldComposite);
    }

    private boolean activeInMode(GameState state) {
        return state.mode == GameMode.EXPLORE || state.mode == GameMode.DEFENSE;
    }

    private double spawnChance(GameState state) {
        String kind = state.world.kind(state.currentMapId);
        if ("interior".equals(kind)) {
            return 0.16;
        }
        if ("dungeon".equals(kind)) {
            return 0.22;
        }
        return 0.36 + state.windStrength() * 0.18;
    }

    private void spawnParticle(GameState state) {
        int mapWidth = state.world.width(state.currentMapId);
        int mapHeight = state.world.height(state.currentMapId);
        int tileX = clamp(state.playerX + random.nextInt(SPAWN_RADIUS * 2 + 1) - SPAWN_RADIUS, 0, Math.max(0, mapWidth - 1));
        int tileY = clamp(state.playerY + random.nextInt(SPAWN_RADIUS * 2 + 1) - SPAWN_RADIUS, 0, Math.max(0, mapHeight - 1));
        char tile = state.world.tileAt(state.currentMapId, tileX, tileY);
        String kind = state.world.kind(state.currentMapId);

        Kind particleKind;
        Color color;
        double dx;
        double dy;
        double gravity = 0.0;
        int life;
        int size;
        float alpha;
        if (tile == 'w' || tile == '~') {
            particleKind = Kind.BUBBLE;
            color = new Color(202, 236, 255);
            dx = randomSigned(0.006);
            dy = -0.010 - random.nextDouble() * 0.012;
            life = 70 + random.nextInt(46);
            size = 3 + random.nextInt(3);
            alpha = 0.34f;
        } else if ("interior".equals(kind) || "dungeon".equals(kind)) {
            particleKind = Kind.DUST;
            color = "dungeon".equals(kind) ? new Color(170, 164, 145) : new Color(214, 204, 176);
            dx = randomSigned(0.004);
            dy = -0.004 - random.nextDouble() * 0.006;
            life = 90 + random.nextInt(58);
            size = 2 + random.nextInt(3);
            alpha = 0.18f;
        } else if (random.nextDouble() < 0.45) {
            particleKind = Kind.LEAF;
            color = leafColor(tile);
            double wind = state.windStrength();
            dx = Math.cos(state.windRadians()) * (0.010 + wind * 0.026) + randomSigned(0.012);
            dy = Math.sin(state.windRadians()) * (0.006 + wind * 0.010) + randomSigned(0.010);
            gravity = 0.00016;
            life = 95 + random.nextInt(75);
            size = 3 + random.nextInt(4);
            alpha = 0.42f;
        } else {
            particleKind = Kind.MOTE;
            color = new Color(248, 224, 145);
            dx = randomSigned(0.009);
            dy = -0.012 - random.nextDouble() * 0.014;
            life = 80 + random.nextInt(62);
            size = 2 + random.nextInt(3);
            alpha = 0.28f;
        }
        double x = tileX + 0.12 + random.nextDouble() * 0.76;
        double y = tileY + 0.15 + random.nextDouble() * 0.72;
        particles.add(new Particle(state.currentMapId, particleKind, x, y, dx, dy, gravity, life, size, alpha, color));
    }

    private Color leafColor(char tile) {
        if (tile == 's' || tile == 't') {
            return new Color(220, 232, 238);
        }
        if (tile == 'd' || tile == 'b') {
            return new Color(202, 148, 74);
        }
        return random.nextBoolean() ? new Color(119, 178, 91) : new Color(214, 177, 76);
    }

    private double randomSigned(double amount) {
        return (random.nextDouble() * 2.0 - 1.0) * amount;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum Kind {
        MOTE,
        LEAF,
        BUBBLE,
        DUST
    }

    private static final class Particle {
        private final String mapId;
        private final Kind kind;
        private double x;
        private double y;
        private final double dx;
        private double dy;
        private final double gravity;
        private final int life;
        private final int size;
        private final float alpha;
        private final Color color;
        private int age;

        private Particle(String mapId, Kind kind, double x, double y, double dx, double dy, double gravity,
                         int life, int size, float alpha, Color color) {
            this.mapId = mapId;
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            this.gravity = gravity;
            this.life = life;
            this.size = size;
            this.alpha = alpha;
            this.color = color;
        }
    }
}
