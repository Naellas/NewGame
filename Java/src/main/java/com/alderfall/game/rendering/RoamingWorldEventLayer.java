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
import java.util.Random;

public final class RoamingWorldEventLayer {
    private static final int MAX_EVENTS = 5;
    private static final int MAX_INTERACTIVE_EVENTS = 2;

    private final List<WorldEvent> events = new ArrayList<>();
    private final Random random = new Random(48133);
    private int nextSpawnTick;

    public void tick(GameState state) {
        events.removeIf(event -> !event.mapId.equals(state.currentMapId));
        for (Iterator<WorldEvent> iterator = events.iterator(); iterator.hasNext(); ) {
            WorldEvent event = iterator.next();
            event.age++;
            event.x += event.dx;
            event.y += event.dy;
            if (event.age >= event.life) {
                iterator.remove();
            }
        }
        if (state.mode != GameMode.EXPLORE && state.mode != GameMode.DEFENSE) {
            events.clear();
            return;
        }
        if (state.worldTick < nextSpawnTick || events.size() >= MAX_EVENTS) {
            return;
        }
        String mapKind = state.world.kind(state.currentMapId);
        int delay = "interior".equals(mapKind) ? 220 : 120 + random.nextInt(150);
        nextSpawnTick = state.worldTick + delay;
        if (random.nextDouble() < spawnChance(mapKind)) {
            spawnEvent(state, mapKind);
        }
    }

    public NearbyPrompt nearestPrompt(GameState state) {
        WorldEvent event = nearestInteractiveEvent(state);
        if (event == null) {
            return null;
        }
        return new NearbyPrompt(event.tileX(), event.tileY(), event.interaction.target, Quest.ObjectiveKind.SEARCH, event.interaction.action);
    }

    public boolean interact(GameState state) {
        WorldEvent event = nearestInteractiveEvent(state);
        if (event == null) {
            return false;
        }
        resolveInteractiveEvent(state, event);
        events.remove(event);
        return true;
    }

    public void draw(Graphics2D g, WorldRenderer.PropContext context) {
        if (events.isEmpty()) {
            return;
        }
        Composite oldComposite = g.getComposite();
        Stroke oldStroke = g.getStroke();
        for (WorldEvent event : events) {
            if (!event.mapId.equals(context.state().currentMapId)) {
                continue;
            }
            double progress = event.age / (double) event.life;
            float alpha = (float) (Math.sin(Math.PI * progress) * event.alpha);
            if (alpha <= 0.01f) {
                continue;
            }
            int x = (int) Math.round((event.x - context.camX()) * context.tileSize());
            int y = (int) Math.round((event.y - context.camY()) * context.tileSize());
            if (x < -context.tileSize() * 2 || y < -context.tileSize() * 2
                    || x > (context.visibleCols() + 2) * context.tileSize()
                    || y > (context.visibleRows() + 2) * context.tileSize()) {
                continue;
            }
            g.setComposite(AlphaComposite.SrcOver.derive(alpha));
            if (event.kind == Kind.GUST) {
                drawGust(g, context, event, x, y);
            } else if (event.kind == Kind.FIREFLY_CLUSTER) {
                drawFireflyCluster(g, context, event, x, y);
            } else if (event.kind == Kind.ROAD_DUST) {
                drawRoadDust(g, context, event, x, y, progress);
            } else {
                drawWaterSkippers(g, context, event, x, y, progress);
            }
            if (event.interaction != null) {
                drawInteractionMarker(g, context, event, x, y, progress);
            }
        }
        g.setStroke(oldStroke);
        g.setComposite(oldComposite);
    }

    private void spawnEvent(GameState state, String mapKind) {
        Interaction interaction = chooseInteraction(state, mapKind);
        Kind kind = interaction == null ? chooseKind(state, mapKind) : interaction.visualKind;
        double angle = kind == Kind.GUST ? state.windRadians() + randomSigned(0.5) : random.nextDouble() * Math.PI * 2.0;
        double speed = switch (kind) {
            case GUST -> 0.030 + state.windStrength() * 0.050;
            case FIREFLY_CLUSTER -> 0.010 + random.nextDouble() * 0.012;
            case ROAD_DUST -> 0.012 + random.nextDouble() * 0.014;
            case WATER_SKIPPERS -> 0.018 + random.nextDouble() * 0.018;
        };
        int spreadX = interaction == null ? 17 : 9;
        int spreadY = interaction == null ? 13 : 9;
        double x = state.playerX + random.nextInt(spreadX) - spreadX / 2 + random.nextDouble();
        double y = state.playerY + random.nextInt(spreadY) - spreadY / 2 + random.nextDouble();
        int life = switch (kind) {
            case GUST -> 145 + random.nextInt(80);
            case FIREFLY_CLUSTER -> 210 + random.nextInt(110);
            case ROAD_DUST -> 96 + random.nextInt(60);
            case WATER_SKIPPERS -> 130 + random.nextInt(70);
        };
        if (interaction != null) {
            life = Math.max(life, 520 + random.nextInt(260));
            speed *= 0.18;
        }
        float alpha = kind == Kind.FIREFLY_CLUSTER ? 0.62f : 0.34f;
        events.add(new WorldEvent(state.currentMapId, kind, x, y, Math.cos(angle) * speed, Math.sin(angle) * speed,
                life, alpha, random.nextInt(999_999), interaction));
    }

    private Interaction chooseInteraction(GameState state, String mapKind) {
        if ("interior".equals(mapKind) || state.mode != GameMode.EXPLORE
                || interactiveEventCount(state.currentMapId) >= MAX_INTERACTIVE_EVENTS) {
            return null;
        }
        double chance = "dungeon".equals(mapKind) ? 0.32 : 0.38;
        if (random.nextDouble() >= chance) {
            return null;
        }
        char tile = state.world.tileAt(state.currentMapId, state.playerX, state.playerY);
        if (tile == 'w' || tile == '~' || tile == 'B') {
            return Interaction.FISH_RUN;
        }
        if ("dungeon".equals(mapKind)) {
            return random.nextBoolean() ? Interaction.UNSTABLE_SHRINE : Interaction.AMBUSH_TRACKS;
        }
        if (isNight(state) && random.nextDouble() < 0.35) {
            return Interaction.WOUNDED_TRAVELER;
        }
        if (Terrain.roadLike(tile) || tile == 'p' || tile == 'j' || tile == 'l' || tile == 'a') {
            return random.nextBoolean() ? Interaction.LOST_SATCHEL : Interaction.AMBUSH_TRACKS;
        }
        return random.nextBoolean() ? Interaction.UNSTABLE_SHRINE : Interaction.WOUNDED_TRAVELER;
    }

    private int interactiveEventCount(String mapId) {
        int count = 0;
        for (WorldEvent event : events) {
            if (event.interaction != null && event.mapId.equals(mapId)) {
                count++;
            }
        }
        return count;
    }

    private WorldEvent nearestInteractiveEvent(GameState state) {
        WorldEvent best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (WorldEvent event : events) {
            if (event.interaction == null || !event.mapId.equals(state.currentMapId)) {
                continue;
            }
            int distance = Math.abs(event.tileX() - state.playerX) + Math.abs(event.tileY() - state.playerY);
            if (distance <= 1 && distance < bestDistance) {
                best = event;
                bestDistance = distance;
            }
        }
        return best;
    }

    private void resolveInteractiveEvent(GameState state, WorldEvent event) {
        int roll = Math.floorMod(event.seed + state.player.dexterity + state.player.intelligence + state.worldTick, 100);
        switch (event.interaction) {
            case LOST_SATCHEL -> {
                int gold = 8 + roll % 18;
                state.player.gold += gold;
                state.player.addItem("trail_rations", 1);
                state.openRoamingEventDialogue("Lost Satchel", "npc_merchant", List.of(
                        "Lost Satchel: You search the straps, shake out road grit, and keep the dry supplies from spoiling.",
                        "Recovered: " + gold + "g and trail rations.",
                        "Minigame seed: sort fragile, wet, and useful items before the satchel collapses."
                ));
            }
            case UNSTABLE_SHRINE -> {
                int xp = 10 + roll % 16;
                state.player.gainXp(xp);
                state.worldAbilityTimers.merge("battle_advantage", 80 + roll % 60, Math::max);
                state.openRoamingEventDialogue("Unstable Shrine", "npc_rowan", List.of(
                        "Unstable Shrine: The glyphs flare as you press the cracked stone back into its old rhythm.",
                        "Result: +" + xp + " XP. Your next fight starts with battle advantage.",
                        "Minigame seed: time rune pulses in sequence before the shrine overloads."
                ));
            }
            case WOUNDED_TRAVELER -> {
                int xp = 8 + roll % 12;
                state.player.gainXp(xp);
                state.player.hp = Math.min(state.player.maxHp, state.player.hp + 6 + roll % 8);
                state.openRoamingEventDialogue("Wounded Traveler", "npc_mira_sunwarden", List.of(
                        "Wounded Traveler: You keep their breathing steady long enough for the fear to loosen its grip.",
                        "Result: +" + xp + " XP. Shared supplies restore a little health.",
                        "Minigame seed: choose bandage, water, pressure, or reassurance under a short timer."
                ));
            }
            case FISH_RUN -> {
                state.player.addItem("quest_clean_water_skin", 1);
                state.player.addItem("trail_rations", 1);
                state.openRoamingEventDialogue("Fish Run", "npc_ren", List.of(
                        "Fish Run: The water wrinkles in fast silver lines. You wait for the honest ripple and scoop.",
                        "Recovered: clean water and trail rations.",
                        "Minigame seed: tap on true ripple rings while false splashes try to bait you early."
                ));
            }
            case AMBUSH_TRACKS -> {
                int xp = 12 + roll % 14;
                state.player.gainXp(xp);
                state.worldAbilityTimers.merge("battle_advantage", 60 + roll % 50, Math::max);
                state.openRoamingEventDialogue("Ambush Tracks", "npc_quartermaster", List.of(
                        "Ambush Tracks: Heel marks, dragged brush, and one careless boot tell the road's secret before steel does.",
                        "Result: +" + xp + " XP. Your next fight starts with battle advantage.",
                        "Minigame seed: trace the correct track chain before wind covers the clues."
                ));
            }
        }
    }

    private Kind chooseKind(GameState state, String mapKind) {
        if ("interior".equals(mapKind) || "dungeon".equals(mapKind)) {
            return Kind.ROAD_DUST;
        }
        char tile = state.world.tileAt(state.currentMapId, state.playerX, state.playerY);
        if (tile == 'w' || tile == '~') {
            return Kind.WATER_SKIPPERS;
        }
        if (isNight(state) && random.nextDouble() < 0.48) {
            return Kind.FIREFLY_CLUSTER;
        }
        if (tile == 'r' || tile == 'R' || tile == 'p') {
            return Kind.ROAD_DUST;
        }
        return Kind.GUST;
    }

    private double spawnChance(String mapKind) {
        if ("interior".equals(mapKind)) {
            return 0.36;
        }
        if ("dungeon".equals(mapKind)) {
            return 0.44;
        }
        return 0.72;
    }

    private boolean isNight(GameState state) {
        int minute = state.timeOfDayMinutes();
        return minute >= 19 * 60 || minute < 5 * 60;
    }

    private void drawGust(Graphics2D g, WorldRenderer.PropContext context, WorldEvent event, int x, int y) {
        g.setColor(new Color(236, 229, 196));
        g.setStroke(new BasicStroke(Math.max(1f, context.zoom() / 42f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int span = Math.max(18, context.tileSize());
        for (int i = 0; i < 4; i++) {
            int ox = (int) Math.round(Math.sin(event.seed * 0.01 + i * 1.7) * context.tileSize() * 0.45);
            int oy = i * context.tileSize() / 4;
            g.drawArc(x + ox - span / 2, y + oy - span / 3, span, span / 2, 18, 128);
        }
    }

    private void drawFireflyCluster(Graphics2D g, WorldRenderer.PropContext context, WorldEvent event, int x, int y) {
        Color glow = new Color(210, 245, 128);
        g.setColor(glow);
        for (int i = 0; i < 8; i++) {
            double angle = event.age * (0.035 + i * 0.003) + event.seed * 0.002 + i * 2.1;
            int px = x + (int) Math.round(Math.cos(angle) * context.tileSize() * (0.35 + i % 3 * 0.12));
            int py = y + (int) Math.round(Math.sin(angle * 1.4) * context.tileSize() * 0.34);
            int size = Math.max(2, context.tileSize() / 12);
            g.fillOval(px - size / 2, py - size / 2, size, size);
        }
    }

    private void drawRoadDust(Graphics2D g, WorldRenderer.PropContext context, WorldEvent event, int x, int y, double progress) {
        g.setColor(new Color(205, 185, 145));
        for (int i = 0; i < 7; i++) {
            int radius = Math.max(4, (int) Math.round(context.tileSize() * (0.08 + progress * 0.16 + i * 0.012)));
            int px = x + Math.floorMod(event.seed + i * 31, context.tileSize()) - context.tileSize() / 2;
            int py = y + Math.floorMod(event.seed / (i + 3), context.tileSize()) - context.tileSize() / 2;
            g.fillOval(px - radius / 2, py - radius / 2, radius, radius);
        }
    }

    private void drawWaterSkippers(Graphics2D g, WorldRenderer.PropContext context, WorldEvent event, int x, int y, double progress) {
        g.setColor(new Color(189, 235, 245));
        g.setStroke(new BasicStroke(Math.max(1f, context.zoom() / 60f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 4; i++) {
            int px = x + Math.floorMod(event.seed + i * 47, context.tileSize() * 2) - context.tileSize();
            int py = y + Math.floorMod(event.seed / (i + 5), context.tileSize()) - context.tileSize() / 2;
            int radius = Math.max(5, (int) Math.round(context.tileSize() * (0.18 + progress * 0.22)));
            g.drawOval(px - radius / 2, py - radius / 4, radius, radius / 2);
        }
    }

    private void drawInteractionMarker(Graphics2D g, WorldRenderer.PropContext context, WorldEvent event, int x, int y, double progress) {
        int pulse = (int) Math.round(Math.sin(context.frame() * 0.16 + event.seed * 0.01) * context.tileSize() * 0.05);
        int radius = Math.max(16, context.tileSize() / 2 + pulse);
        int cx = x;
        int cy = y + context.tileSize() / 2;
        Color color = event.interaction.color;
        g.setComposite(AlphaComposite.SrcOver.derive(0.26f + (float) (0.12 * Math.sin(progress * Math.PI))));
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 180));
        g.fillOval(cx - radius / 2, cy - radius / 4, radius, radius / 2);
        g.setComposite(AlphaComposite.SrcOver.derive(0.78f));
        g.setStroke(new BasicStroke(Math.max(1.2f, context.zoom() / 42f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawOval(cx - radius / 2, cy - radius / 4, radius, radius / 2);
        int diamond = Math.max(9, context.tileSize() / 5);
        int topY = y - context.tileSize() / 4 + pulse;
        g.fillPolygon(
                new int[]{cx, cx + diamond / 2, cx, cx - diamond / 2},
                new int[]{topY, topY + diamond / 2, topY + diamond, topY + diamond / 2},
                4
        );
    }

    private double randomSigned(double amount) {
        return (random.nextDouble() * 2.0 - 1.0) * amount;
    }

    private enum Kind {
        GUST,
        FIREFLY_CLUSTER,
        ROAD_DUST,
        WATER_SKIPPERS
    }

    private enum Interaction {
        LOST_SATCHEL("Lost Satchel", "Search", Kind.ROAD_DUST, new Color(238, 203, 118)),
        UNSTABLE_SHRINE("Unstable Shrine", "Stabilize", Kind.FIREFLY_CLUSTER, new Color(145, 213, 255)),
        WOUNDED_TRAVELER("Wounded Traveler", "Aid", Kind.GUST, new Color(235, 132, 112)),
        FISH_RUN("Fish Run", "Time", Kind.WATER_SKIPPERS, new Color(139, 224, 238)),
        AMBUSH_TRACKS("Ambush Tracks", "Read", Kind.ROAD_DUST, new Color(232, 172, 91));

        private final String target;
        private final String action;
        private final Kind visualKind;
        private final Color color;

        Interaction(String target, String action, Kind visualKind, Color color) {
            this.target = target;
            this.action = action;
            this.visualKind = visualKind;
            this.color = color;
        }
    }

    private static final class WorldEvent {
        private final String mapId;
        private final Kind kind;
        private double x;
        private double y;
        private final double dx;
        private final double dy;
        private final int life;
        private final float alpha;
        private final int seed;
        private final Interaction interaction;
        private int age;

        private WorldEvent(String mapId, Kind kind, double x, double y, double dx, double dy, int life, float alpha,
                           int seed, Interaction interaction) {
            this.mapId = mapId;
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            this.life = life;
            this.alpha = alpha;
            this.seed = seed;
            this.interaction = interaction;
        }

        private int tileX() {
            return (int) Math.round(x);
        }

        private int tileY() {
            return (int) Math.round(y);
        }
    }
}
