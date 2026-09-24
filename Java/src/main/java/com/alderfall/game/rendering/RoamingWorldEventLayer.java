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
import com.alderfall.game.map.WorldMap;

public final class RoamingWorldEventLayer {
    private static final int MAX_EVENTS = 5;
    private static final int MAX_INTERACTIVE_EVENTS = 2;

    private final List<WorldEvent> events = new ArrayList<>();
    private final Random random = new Random(48133);
    private int nextSpawnTick;
    private final java.util.Set<String> resolvedShrines = new java.util.HashSet<>();

    public void reset() { events.clear(); resolvedShrines.clear(); nextSpawnTick = 0; }


    public void tick(GameState state) {
        if (state.mode != GameMode.EXPLORE && state.mode != GameMode.DEFENSE) return;
        events.removeIf(event -> !event.mapId.equals(state.currentMapId));
        for (Iterator<WorldEvent> iterator = events.iterator(); iterator.hasNext(); ) {
            WorldEvent event = iterator.next();
            event.age++;
            event.x += event.dx;
            event.y += event.dy;
            if (event.age >= event.life || event.interaction != null
                    && Math.hypot(event.x - state.playerX, event.y - state.playerY) > 24) {
                iterator.remove();
            }
        }
        if (triggerEnteredEvent(state)) {
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
        if (state.mode != GameMode.EXPLORE) return false;
        WorldEvent event = nearestInteractiveEvent(state);
        if (event == null) {
            return false;
        }
        resolveInteractiveEvent(state, event);
        events.remove(event);
        return true;
    }

    public void draw(Graphics2D g, WorldRenderer.PropContext context, AssetStore assets, WorldDepthRenderer depth) {
        if (events.isEmpty()) {
            return;
        }
        Composite oldComposite = g.getComposite();
        Stroke oldStroke = g.getStroke();
        for (WorldEvent event : events) {
            if (!event.mapId.equals(context.state().currentMapId)) {
                continue;
            }
            if (event.interaction == Interaction.UNSTABLE_SHRINE) continue;
            if (event.interaction == Interaction.WOUNDED_TRAVELER
                    || event.interaction == Interaction.HIDDEN_TRAP || event.interaction == Interaction.FISH_RUN) {
                int size = context.tileSize();
                int px = (event.tileX() - context.camX()) * size;
                int py = (event.tileY() - context.camY()) * size;
                java.awt.Rectangle bounds = new java.awt.Rectangle(px - size / 2, py - size / 2, size * 2, size * 2);
                if (event.interaction == Interaction.WOUNDED_TRAVELER) {
                    depth.character(g, new java.awt.Rectangle(px, py, size, size),
                            target -> drawTraveler(target, assets, px, py, size));
                } else {
                    depth.scenery(g, py + size * .8, bounds, target -> {
                        if (event.interaction == Interaction.HIDDEN_TRAP) drawTrap(target, px, py, size);
                        else drawFish(target, px, py, size, context.frame());
                    });
                }
                continue;
            }
            if (event.interaction == Interaction.AMBUSH_TRACKS || event.interaction == Interaction.LOST_SATCHEL) {
                int size = context.tileSize();
                int px = (event.tileX() - context.camX()) * size;
                int py = (event.tileY() - context.camY()) * size;
                g.setComposite(AlphaComposite.SrcOver);
                if (event.interaction == Interaction.AMBUSH_TRACKS) {
                    g.drawImage(assets.spriteFit("event_ambush_brush", size, size), px, py, null);
                } else {
                    int objectSize = Math.max(16, size * 2 / 3);
                    g.drawImage(assets.spriteFit("event_lost_purse", objectSize, objectSize),
                            px + (size - objectSize) / 2, py + size - objectSize - size / 12, null);
                }
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
        }
        g.setStroke(oldStroke);
        g.setComposite(oldComposite);
    }

    private void spawnEvent(GameState state, String mapKind) {
        Interaction interaction = chooseInteraction(state, mapKind);
        if (interaction == Interaction.UNSTABLE_SHRINE) {
            List<WorldProp> shrines = state.world.props(state.currentMapId).stream()
                    .filter(p -> ShrineEventVisuals.supports(p.asset()))
                    .filter(p -> Math.hypot(p.x() - state.playerX, p.y() - state.playerY) <= 9)
                    .toList();
            if (!shrines.isEmpty()) spawnShrineAt(state, shrines.get(random.nextInt(shrines.size())), random.nextInt());
            return;
        }
        if (interaction == Interaction.AMBUSH_TRACKS || interaction == Interaction.LOST_SATCHEL) {
            for (int attempt = 0; attempt < 60; attempt++) {
                int x = state.playerX + random.nextInt(17) - 8;
                int y = state.playerY + random.nextInt(17) - 8;
                if (Math.hypot(x - state.playerX, y - state.playerY) < 3) continue;
                if (interaction == Interaction.AMBUSH_TRACKS) {
                    if (spawnAmbushAt(state, x, y, random.nextInt())) return;
                } else if (spawnPurseAt(state, x, y, random.nextInt())) return;
            }
            return;
        }
        if (interaction != null) {
            for (int attempt = 0; attempt < 60; attempt++) {
                int x = state.playerX + random.nextInt(17) - 8;
                int y = state.playerY + random.nextInt(17) - 8;
                if (spawnStationaryAt(state, x, y, random.nextInt(), interaction)) return;
            }
            return;
        }
        Kind kind = chooseKind(state, mapKind);
        double angle = kind == Kind.GUST ? state.windRadians() + randomSigned(0.5) : random.nextDouble() * Math.PI * 2.0;
        double speed = switch (kind) {
            case GUST -> 0.030 + state.windStrength() * 0.050;
            case FIREFLY_CLUSTER -> 0.010 + random.nextDouble() * 0.012;
            case ROAD_DUST -> 0.012 + random.nextDouble() * 0.014;
            case WATER_SKIPPERS -> 0.018 + random.nextDouble() * 0.018;
        };
        int spreadX = 17;
        int spreadY = 13;
        double x = state.playerX + random.nextInt(spreadX) - spreadX / 2 + random.nextDouble();
        double y = state.playerY + random.nextInt(spreadY) - spreadY / 2 + random.nextDouble();
        int life = switch (kind) {
            case GUST -> 145 + random.nextInt(80);
            case FIREFLY_CLUSTER -> 210 + random.nextInt(110);
            case ROAD_DUST -> 96 + random.nextInt(60);
            case WATER_SKIPPERS -> 130 + random.nextInt(70);
        };
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
            int pick = random.nextInt(3);
            return pick == 0 ? Interaction.LOST_SATCHEL : Interaction.HIDDEN_TRAP;
        }
        if (isNight(state) && random.nextDouble() < 0.35) {
            return Interaction.WOUNDED_TRAVELER;
        }
        if (Terrain.roadLike(tile) || tile == 'p' || tile == 'j' || tile == 'l' || tile == 'a') {
            int pick = random.nextInt(3);
            return pick == 0 ? Interaction.LOST_SATCHEL : pick == 1 ? Interaction.HIDDEN_TRAP
                    : WorldMap.OVERWORLD_ID.equals(state.currentMapId) ? Interaction.AMBUSH_TRACKS : Interaction.LOST_SATCHEL;
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
            if (event.interaction == null || event.interaction == Interaction.AMBUSH_TRACKS || !event.mapId.equals(state.currentMapId)) {
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
        switch (event.interaction) {
            case LOST_SATCHEL -> RoamingEventWorkflow.openLostSatchelPrompt(state, event.seed);
            case UNSTABLE_SHRINE -> {
                resolvedShrines.add(shrineKey(event.mapId, event.tileX(), event.tileY()));
                RoamingEventWorkflow.openShrinePrompt(state, event.seed);
            }
            case WOUNDED_TRAVELER -> RoamingEventWorkflow.openWoundedTravelerPrompt(state, event.seed);
            case FISH_RUN -> RoamingEventWorkflow.openFishRunPrompt(state, event.seed);
            case AMBUSH_TRACKS -> RoamingEventWorkflow.openAmbushPrompt(state, event.seed);
            case HIDDEN_TRAP -> RoamingEventWorkflow.openTrapPrompt(state, event.seed);
        }
    }

    public boolean triggerEnteredEvent(GameState state) {
        if (state.mode != GameMode.EXPLORE) return false;
        for (Iterator<WorldEvent> iterator = events.iterator(); iterator.hasNext(); ) {
            WorldEvent event = iterator.next();
            if (event.interaction == null
                    || !event.interaction.autoTriggerOnEnter
                    || !event.mapId.equals(state.currentMapId)
                    || event.tileX() != state.playerX
                    || event.tileY() != state.playerY) {
                continue;
            }
            iterator.remove();
            if (event.interaction == Interaction.AMBUSH_TRACKS) {
                List<String> enemies = state.roadAmbushers(event.tileX(), event.tileY(), event.seed);
                if (!enemies.isEmpty()) {
                    state.openRoadAmbushPrompt(enemies);
                    return true;
                }
            }
            if (event.interaction == Interaction.HIDDEN_TRAP) {
                RoamingEventWorkflow.triggerTrapTile(state, event.seed);
                return true;
            }
        }
        return false;
    }

    private boolean freeEventTile(GameState state, int x, int y) {
        return state.world.isPassable(state.currentMapId, x, y)
                && PropCollision.clear(state.world, state.currentMapId, x + .5, y + .5)
                && state.world.transitionAt(state.currentMapId, x, y) == null
                && state.world.propsAt(state.currentMapId, x, y).isEmpty()
                && state.npcAt(state.currentMapId, x, y) == null
                && state.questObjectiveAt(state.currentMapId, x, y) == null
                && (!WorldMap.OVERWORLD_ID.equals(state.currentMapId)
                    || state.world.adventureMarkers().stream().noneMatch(m -> Math.hypot(m.x() - x, m.y() - y) < 5))
                && events.stream().noneMatch(e -> e.tileX() == x && e.tileY() == y);
    }

    boolean spawnTravelerAt(GameState state, int x, int y, int seed) {
        return spawnStationaryAt(state, x, y, seed, Interaction.WOUNDED_TRAVELER);
    }

    boolean spawnTrapAt(GameState state, int x, int y, int seed) {
        return spawnStationaryAt(state, x, y, seed, Interaction.HIDDEN_TRAP);
    }

    boolean spawnFishAt(GameState state, int x, int y, int seed) {
        return spawnStationaryAt(state, x, y, seed, Interaction.FISH_RUN);
    }

    private boolean spawnStationaryAt(GameState state, int x, int y, int seed, Interaction interaction) {
        if (Math.hypot(x - state.playerX, y - state.playerY) < 2) return false;
        if (interaction == Interaction.FISH_RUN) {
            char tile = state.world.tileAt(state.currentMapId, x, y);
            if (tile != 'w' && tile != '~') return false;
            boolean approach = false;
            for (int[] step : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int ax = x + step[0], ay = y + step[1];
                approach |= state.world.isPassable(state.currentMapId, ax, ay)
                        && state.world.waterDepth(state.currentMapId, ax, ay) != WaterDepth.DEEP
                        && PropCollision.clear(state.world, state.currentMapId, ax + .5, ay + .5);
            }
            if (!approach || !state.world.propsAt(state.currentMapId, x, y).isEmpty()
                    || events.stream().anyMatch(e -> e.tileX() == x && e.tileY() == y)) return false;
        } else if (!freeEventTile(state, x, y)
                || state.world.waterDepth(state.currentMapId, x, y) != WaterDepth.DRY) return false;
        events.add(new WorldEvent(state.currentMapId, interaction.visualKind, x, y, 0, 0,
                Integer.MAX_VALUE, 1, seed, interaction));
        return true;
    }

    boolean spawnPurseAt(GameState state, int x, int y, int seed) {
        if (!freeEventTile(state, x, y) || Math.hypot(x - state.playerX, y - state.playerY) < 2) return false;
        events.add(new WorldEvent(state.currentMapId, Kind.ROAD_DUST, x, y, 0, 0,
                Integer.MAX_VALUE, 1, seed, Interaction.LOST_SATCHEL));
        return true;
    }

    boolean spawnAmbushAt(GameState state, int x, int y, int seed) {
        if (!WorldMap.OVERWORLD_ID.equals(state.currentMapId) || !freeEventTile(state, x, y)
                || Math.hypot(x - state.playerX, y - state.playerY) < 3) return false;
        char tile = state.world.tileAt(state.currentMapId, x, y);
        if (tile != Terrain.DIRT_ROAD && tile != Terrain.PACKED_ROAD && tile != Terrain.COBBLESTONE_ROAD) return false;
        if (state.world.nearestDungeonInBiome(x, y) == null) return false;
        events.add(new WorldEvent(state.currentMapId, Kind.ROAD_DUST, x, y, 0, 0,
                Integer.MAX_VALUE, 1, seed, Interaction.AMBUSH_TRACKS));
        return true;
    }

    boolean spawnShrineAt(GameState state, WorldProp prop, int seed) {
        if (!ShrineEventVisuals.supports(prop.asset()) || !state.world.props(state.currentMapId).contains(prop)
                || resolvedShrines.contains(shrineKey(state.currentMapId, prop.x(), prop.y()))
                || events.stream().anyMatch(e -> e.tileX() == prop.x() && e.tileY() == prop.y())) return false;
        boolean approach = false;
        for (int[] step : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}})
            approach |= state.world.isPassable(state.currentMapId, prop.x() + step[0], prop.y() + step[1]);
        if (!approach) return false;
        events.add(new WorldEvent(state.currentMapId, Kind.FIREFLY_CLUSTER, prop.x(), prop.y(), 0, 0,
                Integer.MAX_VALUE, 1, seed, Interaction.UNSTABLE_SHRINE));
        return true;
    }

    public int shrineVariant(String map, WorldProp prop) {
        if (!ShrineEventVisuals.supports(prop.asset())) return -1;
        for (WorldEvent event : events) if (event.mapId.equals(map) && event.interaction == Interaction.UNSTABLE_SHRINE
                && event.tileX() == prop.x() && event.tileY() == prop.y()) return Math.floorMod(event.seed, 3);
        return -1;
    }

    public void drawShrineHighlights(Graphics2D g, WorldRenderer.PropContext context, WorldRenderer renderer) {
        for (WorldEvent event : events) {
            if (event.interaction != Interaction.UNSTABLE_SHRINE || !event.mapId.equals(context.state().currentMapId)) continue;
            for (WorldProp prop : context.state().world.propsAt(event.mapId, event.tileX(), event.tileY())) {
                if (!ShrineEventVisuals.supports(prop.asset())) continue;
                java.awt.Rectangle bounds = renderer.propBounds(prop, context.tileSize(), context.camX(), context.camY());
                ShrineEventVisuals.glow(g, bounds, Math.floorMod(event.seed, 3), context.frame());
            }
        }
    }

    private static String shrineKey(String map, int x, int y) { return map + ":" + x + ":" + y; }

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

    private static void drawTraveler(Graphics2D g, AssetStore assets, int x, int y, int size) {
        Graphics2D local = (Graphics2D) g.create();
        local.setComposite(AlphaComposite.SrcOver);
        local.setColor(new Color(15, 20, 16, 90));
        local.fillOval(x, y + size / 2, size, size / 3);
        local.drawImage(assets.spriteFit("dungeon_detail_bandit_bedroll", size, size), x, y, null);
        // Recline the existing dialogue character on the bedroll, with their pack nearby.
        local.translate(x + size * .5, y + size * .48);
        local.rotate(-Math.PI / 3);
        local.drawImage(assets.spriteFit("npc_mira_sunwarden_model_down", size, size * 3 / 2),
                -size / 2, -size * 3 / 4, null);
        local.dispose();
        g.drawImage(assets.spriteFit("event_lost_purse", size / 3, size / 3),
                x + size * 2 / 3, y + size * 2 / 3, null);
    }

    private static void drawTrap(Graphics2D g, int x, int y, int size) {
        Graphics2D local = (Graphics2D) g.create();
        local.setComposite(AlphaComposite.SrcOver);
        local.translate(x, y);
        local.scale(size / 48.0, size / 48.0);
        local.setColor(new Color(27, 24, 20, 150));
        local.fillOval(6, 22, 36, 17);
        local.setStroke(new BasicStroke(3));
        local.setColor(new Color(100, 88, 69));
        local.drawOval(8, 21, 32, 14);
        local.setColor(new Color(162, 151, 121));
        for (int i = 0; i < 5; i++) {
            int tx = 10 + i * 6;
            local.fillPolygon(new int[]{tx, tx + 3, tx + 5}, new int[]{23, 17, 24}, 3);
            local.fillPolygon(new int[]{tx, tx + 3, tx + 5}, new int[]{33, 28, 34}, 3);
        }
        local.setColor(new Color(118, 102, 76));
        local.fillRect(20, 25, 9, 5);
        local.setStroke(new BasicStroke(1));
        local.drawLine(39, 28, 46, 36);
        local.dispose();
    }

    private static void drawFish(Graphics2D g, int x, int y, int size, int frame) {
        Graphics2D local = (Graphics2D) g.create();
        local.setComposite(AlphaComposite.SrcOver);
        local.clipRect(x, y, size, size);
        local.translate(x, y);
        local.scale(size / 48.0, size / 48.0);
        for (int i = 0; i < 3; i++) {
            double phase = frame * .035 + i * 2.1;
            int fx = 12 + i * 10 + (int) (Math.sin(phase) * 3);
            int fy = 13 + i * 10;
            local.setColor(new Color(31, 65, 66, 220));
            local.fillOval(fx - 6, fy - 2, 13, 5);
            local.fillPolygon(new int[]{fx - 5, fx - 10, fx - 10}, new int[]{fy, fy - 4, fy + 4}, 3);
            local.setColor(new Color(179, 204, 186, 200));
            local.drawLine(fx - 2, fy - 1, fx + 4, fy - 1);
            local.setColor(new Color(178, 221, 223, 100));
            local.drawArc(fx - 9, fy - 5, 22, 10, 200, 110);
        }
        local.dispose();
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
        LOST_SATCHEL("Lost Purse", "Search", Kind.ROAD_DUST, new Color(238, 203, 118)),
        UNSTABLE_SHRINE("Unstable Shrine", "Stabilize", Kind.FIREFLY_CLUSTER, new Color(145, 213, 255)),
        WOUNDED_TRAVELER("Wounded Traveler", "Aid", Kind.GUST, new Color(235, 132, 112)),
        FISH_RUN("Fish Run", "Time", Kind.WATER_SKIPPERS, new Color(139, 224, 238)),
        AMBUSH_TRACKS("Roadside Bush", "", Kind.ROAD_DUST, new Color(232, 172, 91), true),
        HIDDEN_TRAP("Hidden Trap", "Inspect", Kind.ROAD_DUST, new Color(210, 88, 78), true);

        private final String target;
        private final String action;
        private final Kind visualKind;
        private final Color color;
        private final boolean autoTriggerOnEnter;

        Interaction(String target, String action, Kind visualKind, Color color) {
            this(target, action, visualKind, color, false);
        }

        Interaction(String target, String action, Kind visualKind, Color color, boolean autoTriggerOnEnter) {
            this.target = target;
            this.action = action;
            this.visualKind = visualKind;
            this.color = color;
            this.autoTriggerOnEnter = autoTriggerOnEnter;
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
