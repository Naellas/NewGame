package com.alderfall.game;

import java.awt.BasicStroke;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.IntConsumer;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public final class GamePanel extends JPanel {
    private static final Map<Character, String> MUSIC_BY_TERRAIN = Map.ofEntries(
            Map.entry('g', "zone_grasslands"),
            Map.entry('r', "zone_grasslands"),
            Map.entry('f', "zone_forest"),
            Map.entry('s', "zone_desert"),
            Map.entry('n', "zone_tundra"),
            Map.entry('v', "zone_marsh"),
            Map.entry('b', "zone_badlands"),
            Map.entry('m', "zone_mountains"),
            Map.entry('q', "zone_mountains"),
            Map.entry('w', "zone_water"),
            Map.entry('c', "town_village"),
            Map.entry('u', "town_village"),
            Map.entry('d', "dungeon_crypt")
    );
    private static final Set<GameMode> WORLD_MUSIC_MODES = Set.of(
            GameMode.EXPLORE,
            GameMode.DIALOG,
            GameMode.QUEST_LOG,
            GameMode.SKILLS,
            GameMode.INVENTORY,
            GameMode.SHOP,
            GameMode.WORLD_MAP
    );
    private static final Set<String> BOSS_MUSIC_KEYS = new HashSet<>(Set.of(
            "acid_broodmother",
            "bone_knight",
            "crypt_revenant",
            "elder_wraith",
            "goblin_king",
            "goblin_warlord",
            "ice_golem",
            "orc_champion"
    ));
    private static final int PLAYER_MOVE_FRAMES = 7;
    private final GameState state;
    private final AssetStore assets;
    private final SaveSystem saves;
    private final MusicManager music;
    private final Path javaRoot;
    private final Timer timer;
    private final List<UiButton> buttons = new ArrayList<>();
    private int frame;
    private int lastCamX;
    private int lastCamY;
    private int lastVisibleCols;
    private int lastVisibleRows;
    private BufferedImage backBuffer;
    private double renderScaleX = 1.0;
    private double renderScaleY = 1.0;
    private int renderOffsetX;
    private int renderOffsetY;
    private String playerMoveMapId = WorldMap.OVERWORLD_ID;
    private int playerMoveStartFrame = -PLAYER_MOVE_FRAMES;
    private int playerMoveFromX = WorldMap.START_POSITION.x();
    private int playerMoveFromY = WorldMap.START_POSITION.y();
    private int playerFacingDx;
    private int playerFacingDy = 1;
    private final List<TilePoint> playerPath = new ArrayList<>();
    private TilePoint playerPathDestination;
    private Runnable fullscreenToggle = () -> {
    };

    public GamePanel(Path javaRoot) {
        this.javaRoot = javaRoot;
        this.state = new GameState(GameConfig.loadWithSettings(javaRoot));
        this.assets = new AssetStore(javaRoot.resolve("assets"));
        this.saves = new SaveSystem(javaRoot);
        this.music = new MusicManager(javaRoot.resolve("assets").resolve("music"));
        setPreferredSize(new Dimension(GameConfig.WIDTH, GameConfig.HEIGHT));
        setBackground(new Color(15, 17, 24));
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        addKeyListener(new Keys());
        Mouse mouse = new Mouse();
        addMouseListener(mouse);
        addMouseWheelListener(mouse);
        timer = new Timer(GameConfig.FPS_MS, event -> {
            frame++;
            state.tickWorld();
            tickPlayerPath();
            if (state.mode == GameMode.BATTLE && state.battle != null) {
                state.battle.tick();
            }
            updateMusic();
            repaint();
        });
        timer.start();
    }

    public void setFullscreenToggle(Runnable fullscreenToggle) {
        this.fullscreenToggle = fullscreenToggle == null ? () -> {
        } : fullscreenToggle;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        updateViewportTransform();
        if (backBuffer == null) {
            backBuffer = new BufferedImage(GameConfig.WIDTH, GameConfig.HEIGHT, BufferedImage.TYPE_INT_ARGB);
        }

        buttons.clear();
        Graphics2D bufferGraphics = backBuffer.createGraphics();
        bufferGraphics.setColor(getBackground());
        bufferGraphics.fillRect(0, 0, GameConfig.WIDTH, GameConfig.HEIGHT);
        renderGame(bufferGraphics);
        bufferGraphics.dispose();

        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        int scaledWidth = (int) Math.round(GameConfig.WIDTH * renderScaleX);
        int scaledHeight = (int) Math.round(GameConfig.HEIGHT * renderScaleY);
        g.drawImage(backBuffer, renderOffsetX, renderOffsetY, scaledWidth, scaledHeight, null);
        g.dispose();
    }

    private void renderGame(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        if (state.mode == GameMode.MAIN_MENU) {
            drawMainMenu(g);
        } else if (state.mode == GameMode.CLASS_SELECT) {
            drawClassSelect(g);
        } else if (state.mode == GameMode.SETTINGS && state.settingsReturnMode == GameMode.MAIN_MENU) {
            drawTitleBackground(g);
            drawSettingsMenu(g, false);
        } else if (state.mode == GameMode.SAVE_MENU && state.saveMenuReturnMode == GameMode.MAIN_MENU) {
            drawTitleBackground(g);
            drawSaveMenu(g, false);
        } else {
            drawWorld(g);
            drawSidebar(g);
            GameMode visibleMode = visibleGameplayMode();
            if (visibleMode == GameMode.BATTLE) {
                drawBattle(g);
            } else if (visibleMode == GameMode.DIALOG) {
                drawDialog(g);
            } else if (visibleMode == GameMode.QUEST_LOG) {
                drawQuestLog(g);
            } else if (visibleMode == GameMode.SKILLS) {
                drawSkillTree(g);
            } else if (visibleMode == GameMode.INVENTORY) {
                drawInventory(g);
            } else if (visibleMode == GameMode.SHOP) {
                drawShop(g);
            } else if (visibleMode == GameMode.WORLD_MAP) {
                drawWorldMap(g);
            }
            if (state.mode == GameMode.PAUSE_MENU) {
                drawPauseMenu(g);
            } else if (state.mode == GameMode.SETTINGS) {
                drawSettingsMenu(g, true);
            } else if (state.mode == GameMode.SAVE_MENU) {
                drawSaveMenu(g, true);
            }
        }
    }

    private GameMode visibleGameplayMode() {
        if (state.mode == GameMode.PAUSE_MENU) {
            return state.pauseReturnMode;
        }
        if (state.mode == GameMode.SETTINGS) {
            return state.settingsReturnMode == GameMode.PAUSE_MENU ? state.pauseReturnMode : state.settingsReturnMode;
        }
        if (state.mode == GameMode.SAVE_MENU) {
            return state.saveMenuReturnMode == GameMode.PAUSE_MENU ? state.pauseReturnMode : state.saveMenuReturnMode;
        }
        return state.mode;
    }

    private void updateMusic() {
        music.setVolume(effectiveMusicVolume());
        String track = desiredMusicTrack();
        if (track == null) {
            music.stop();
        } else {
            music.play(track);
        }
    }

    private String desiredMusicTrack() {
        if (state.mode == GameMode.MAIN_MENU || state.mode == GameMode.CLASS_SELECT) {
            return null;
        }
        if (state.mode == GameMode.PAUSE_MENU) {
            return trackForMode(state.pauseReturnMode);
        }
        if (state.mode == GameMode.SETTINGS) {
            return trackForMode(state.settingsReturnMode == GameMode.PAUSE_MENU ? state.pauseReturnMode : state.settingsReturnMode);
        }
        if (state.mode == GameMode.SAVE_MENU) {
            return trackForMode(state.saveMenuReturnMode == GameMode.PAUSE_MENU ? state.pauseReturnMode : state.saveMenuReturnMode);
        }
        if (state.mode == GameMode.BATTLE && state.battle != null) {
            return battleMusicTrack();
        }
        if (WORLD_MUSIC_MODES.contains(state.mode)) {
            return worldMusicTrack();
        }
        return null;
    }

    private String trackForMode(GameMode mode) {
        if (mode == GameMode.BATTLE && state.battle != null) {
            return battleMusicTrack();
        }
        if (mode == GameMode.MAIN_MENU || mode == GameMode.CLASS_SELECT) {
            return null;
        }
        return worldMusicTrack();
    }

    private float effectiveMusicVolume() {
        return (state.config.masterVolume / 100.0f) * (state.config.musicVolume / 100.0f);
    }

    private String worldMusicTrack() {
        String kind = state.world.kind(state.currentMapId);
        if ("city".equals(kind) || "village".equals(kind) || "interior".equals(kind)) {
            return "town_village";
        }
        if ("dungeon".equals(kind)) {
            return "dungeon_crypt";
        }
        char tile = state.world.tileAt(state.currentMapId, state.playerX, state.playerY);
        return MUSIC_BY_TERRAIN.getOrDefault(tile, "zone_grasslands");
    }

    private String battleMusicTrack() {
        if (state.battle.monsterSpecs.size() >= 3) {
            return "battle_boss";
        }
        for (GameData.MonsterSpec spec : state.battle.monsterSpecs) {
            if (BOSS_MUSIC_KEYS.contains(spec.key())) {
                return "battle_boss";
            }
        }
        return "battle_standard";
    }

    private void drawTitleBackground(Graphics2D g) {
        g.drawImage(assets.cover("battle_forest_backdrop", viewWidth(), viewHeight()), 0, 0, null);
        g.setPaint(new GradientPaint(0, 0, new Color(8, 10, 16, 150), 0, viewHeight(), new Color(8, 10, 16, 238)));
        g.fillRect(0, 0, viewWidth(), viewHeight());
        g.setPaint(null);
        g.setColor(new Color(68, 83, 104, 105));
        for (int i = 0; i < 18; i++) {
            int x = (i * 89 + frame * 2) % (viewWidth() + 160) - 80;
            int y = 82 + (i * 37) % 600;
            g.drawLine(x, y, x + 48, y - 5);
        }
        g.drawImage(assets.spriteFit("class_knight_model", 120, 152), 116, 540, null);
        g.drawImage(assets.spriteFit("class_mage_model", 120, 152), viewWidth() - 238, 534, null);
        g.drawImage(assets.spriteFit("class_ranger_model", 110, 146), viewWidth() - 370, 560, null);
    }

    private void drawMainMenu(Graphics2D g) {
        drawTitleBackground(g);
        g.setFont(new Font("Serif", Font.BOLD, 46));
        g.setColor(new Color(244, 213, 141));
        drawCentered(g, "Echoes of Alderfall", 116);
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.setColor(new Color(215, 228, 239));
        drawCentered(g, "Alderfall remembers every name.", 158);

        int x = 430;
        int y = 252;
        int w = 300;
        actionButton(g, x, y, w, 52, "New Adventure", state::openClassSelect, new Color(66, 93, 49), new Color(126, 176, 95), true);
        boolean hasSave = saves.exists();
        actionButton(g, x, y + 72, w, 52, "Load Adventure", () -> state.openSaveMenu(false), new Color(57, 71, 102), new Color(110, 127, 160), hasSave);
        if (!hasSave) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.setColor(new Color(165, 174, 193));
            drawCenteredIn(g, "No saved adventures yet.", x, y + 146, w);
        }
        actionButton(g, x, y + 164, w, 46, "Settings", state::openSettings, new Color(74, 66, 93), new Color(124, 107, 155), true);
        actionButton(g, x, y + 230, 144, 46, "Fullscreen", fullscreenToggle, new Color(74, 67, 80), new Color(117, 107, 128), true);
        actionButton(g, x + 156, y + 230, 144, 46, "Exit", this::exitGame, new Color(91, 60, 60), new Color(165, 111, 98), true);

        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(126, 137, 159));
        drawCentered(g, "Enter starts a new adventure. L loads. S opens settings. F11 toggles fullscreen.", viewHeight() - 70);
    }

    private void drawClassSelect(Graphics2D g) {
        drawTitleBackground(g);
        g.setFont(new Font("SansSerif", Font.BOLD, 44));
        g.setColor(new Color(238, 232, 210));
        drawCentered(g, "Echoes of Alderfall", 130);
        g.setFont(new Font("SansSerif", Font.PLAIN, 20));
        drawCentered(g, "New Adventure", 168);

        int nameX = 520;
        int nameY = 198;
        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.setColor(new Color(215, 228, 239));
        drawCentered(g, "Character Name", nameY - 14);
        g.setColor(new Color(22, 27, 38, 230));
        g.fillRoundRect(nameX, nameY, 400, 50, 8, 8);
        g.setColor(new Color(95, 107, 132));
        g.drawRoundRect(nameX, nameY, 400, 50, 8, 8);
        g.setFont(new Font("SansSerif", Font.PLAIN, 24));
        String displayName = state.pendingPlayerName.isBlank() ? "Arin" : state.pendingPlayerName;
        g.setColor(state.pendingPlayerName.isBlank() ? new Color(142, 151, 168) : new Color(244, 239, 220));
        drawCenteredIn(g, displayName + (frame % 24 < 12 ? "_" : ""), nameX, nameY + 33, 400);

        drawClassCard(g, 230, 260, "1", "Knight", "Durable front-line fighter", new Color(115, 138, 184));
        drawClassCard(g, 560, 260, "2", "Mage", "High MP and ranged spells", new Color(154, 117, 190));
        drawClassCard(g, 890, 260, "3", "Ranger", "Balanced damage and field healing", new Color(111, 165, 119));
        buttons.add(new UiButton(new java.awt.Rectangle(230, 260, 250, 230), "class:Knight", () -> {
            state.chooseClass("Knight");
            syncPlayerAnimationToState();
            repaint();
        }));
        buttons.add(new UiButton(new java.awt.Rectangle(560, 260, 250, 230), "class:Mage", () -> {
            state.chooseClass("Mage");
            syncPlayerAnimationToState();
            repaint();
        }));
        buttons.add(new UiButton(new java.awt.Rectangle(890, 260, 250, 230), "class:Ranger", () -> {
            state.chooseClass("Ranger");
            syncPlayerAnimationToState();
            repaint();
        }));

        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.setColor(new Color(196, 198, 205));
        drawCentered(g, "Press 1, 2, or 3 to start. Esc returns to the main menu.", 610);
        actionButton(g, 32, 32, 110, 34, "Back", state::openMainMenu, new Color(48, 55, 70), new Color(89, 102, 125), true);
    }

    private void drawClassCard(Graphics2D g, int x, int y, String key, String title, String subtitle, Color color) {
        g.setColor(new Color(28, 31, 43));
        g.fillRoundRect(x, y, 250, 230, 8, 8);
        g.setColor(color);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(x, y, 250, 230, 8, 8);
        g.drawImage(assets.sprite("class_" + title.toLowerCase(), 96), x + 77, y + 28, null);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(244, 239, 220));
        drawCenteredIn(g, key + " - " + title, x, y + 145, 250);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(205, 208, 216));
        drawCenteredIn(g, subtitle, x, y + 178, 250);
    }

    private void drawWorld(Graphics2D g) {
        int mapWidth = state.world.width(state.currentMapId);
        int mapHeight = state.world.height(state.currentMapId);
        int tileSize = tileSize();
        int visibleCols = Math.max(1, (GameConfig.MAP_COLS * GameConfig.TILE + tileSize - 1) / tileSize);
        int visibleRows = Math.max(1, (viewHeight() + tileSize - 1) / tileSize);
        int camX = clamp(state.playerX - visibleCols / 2, 0, Math.max(0, mapWidth - visibleCols));
        int camY = clamp(state.playerY - visibleRows / 2, 0, Math.max(0, mapHeight - visibleRows));
        lastCamX = camX;
        lastCamY = camY;
        lastVisibleCols = visibleCols;
        lastVisibleRows = visibleRows;
        for (int sy = 0; sy < visibleRows; sy++) {
            for (int sx = 0; sx < visibleCols; sx++) {
                int wx = camX + sx;
                int wy = camY + sy;
                char tile = state.world.tileAt(state.currentMapId, wx, wy);
                char terrainTile = visibleTerrainTile(tile, wx, wy);
                int px = sx * tileSize;
                int py = sy * tileSize;
                g.drawImage(assets.image(terrainImageName(terrainTile, wx, wy), tileSize, tileSize), px, py, null);
                drawTerrainEdges(g, terrainTile, wx, wy, px, py);
                if ("interior".equals(state.world.kind(state.currentMapId))) {
                    drawHouseTile(g, tile, wx, wy, px, py);
                } else if (tile == 'h') {
                    g.setColor(new Color(72, 47, 36, 96));
                    g.fillRect(px + scaled(6), py + scaled(8), tileSize - scaled(12), tileSize - scaled(10));
                } else if (tile == 'x' || tile == 'o') {
                    g.setColor(new Color(40, 38, 45, 120));
                    g.fillRect(px, py, tileSize, tileSize);
                }
                String landmark = state.world.landmarkAt(state.currentMapId, wx, wy);
                if (landmark != null) {
                    g.setColor(new Color(255, 245, 174));
                    g.fillOval(px + scaled(19), py + scaled(4), scaled(10), scaled(10));
                }
            }
        }

        drawGroundPropOverlays(g, camX, camY);
        drawMountainMassifOverlays(g, camX, camY);
        drawRoadConnectors(g, camX, camY);
        drawCityBuildingEntities(g, camX, camY);
        drawSettlementOverlays(g, camX, camY);
        drawCloudLayer(g, camX, camY);

        List<WorldProp> visibleProps = new ArrayList<>();
        for (WorldProp prop : state.world.props(state.currentMapId)) {
            if (prop.x() < camX || prop.y() < camY || prop.x() >= camX + visibleCols || prop.y() >= camY + visibleRows) {
                continue;
            }
            if (!isGroundProp(prop.asset())) {
                visibleProps.add(prop);
            }
        }
        visibleProps.sort((a, b) -> {
            int byY = Integer.compare(a.y(), b.y());
            return byY != 0 ? byY : Integer.compare(a.x(), b.x());
        });
        for (WorldProp prop : visibleProps) {
            int size = scaled(prop.size());
            int px = (prop.x() - camX) * tileSize + (tileSize - size) / 2;
            int py = (prop.y() - camY) * tileSize + tileSize - size;
            if (castsPropShadow(prop.asset())) {
                drawShadow(g, px + size / 6, py + size - scaled(8), size * 2 / 3, scaled(8));
            }
            g.drawImage(assets.spriteFit(prop.asset(), size, size), px, py, null);
        }
        drawQuestObjectives(g, camX, camY);

        for (GameState.NpcMotion motion : state.npcMotionsForMap(state.currentMapId)) {
            Npc npc = motion.npc();
            double nx = renderNpcX(motion);
            double ny = renderNpcY(motion);
            if (nx < camX - 1 || ny < camY - 1 || nx >= camX + visibleCols + 1 || ny >= camY + visibleRows + 1) {
                continue;
            }
            int px = (int) Math.round((nx - camX) * tileSize);
            int py = (int) Math.round((ny - camY) * tileSize);
            int step = npcMoving(motion) ? (state.worldTick / 4) % 4 + 1 : 0;
            drawShadow(g, px + scaled(8), py + scaled(36), scaled(32), scaled(8));
            drawCharacterSprite(g, npc.sprite() + "_model", px + scaled(7), py - scaled(2), scaled(34), scaled(46), motion.facingDx(), motion.facingDy(), step);
            g.setColor(new Color(255, 245, 174));
            g.fillOval(px + scaled(31), py + scaled(2) - (step > 0 ? scaled(2) : 0), scaled(7), scaled(7));
        }

        double playerX = renderPlayerX();
        double playerY = renderPlayerY();
        int playerPx = (int) Math.round((playerX - camX) * tileSize);
        int playerPy = (int) Math.round((playerY - camY) * tileSize);
        int step = playerMoving() ? (frame / 3) % 4 + 1 : 0;
        int bob = step > 0 ? 0 : (int) Math.round(Math.sin(frame * 0.12) * 1.2);
        drawShadow(g, playerPx + scaled(8), playerPy + scaled(39), scaled(34), scaled(9));
        drawCharacterSprite(g, state.player.worldSprite, playerPx + scaled(6), playerPy - scaled(5) + bob, scaled(36), scaled(50), playerFacingDx, playerFacingDy, step);
    }

    private void drawQuestObjectives(Graphics2D g, int camX, int camY) {
        int ts = tileSize();
        for (GameState.QuestObjective objective : state.activeQuestObjectives()) {
            if (!objective.mapId().equals(state.currentMapId)) {
                continue;
            }
            if (objective.x() < camX || objective.y() < camY || objective.x() >= camX + lastVisibleCols || objective.y() >= camY + lastVisibleRows) {
                continue;
            }
            int sx = objective.x() - camX;
            int sy = objective.y() - camY;
            int px = sx * ts;
            int py = sy * ts;
            int pulse = (int) Math.round(Math.sin(frame * 0.15) * scaled(2));
            Color ring = objective.kind() == Quest.ObjectiveKind.GATHER
                    ? new Color(112, 220, 128, 210)
                    : new Color(233, 89, 83, 220);
            g.setColor(new Color(ring.getRed(), ring.getGreen(), ring.getBlue(), 62));
            g.fillOval(px + scaled(5), py + scaled(32), ts - scaled(10), scaled(13));
            g.setColor(ring);
            g.drawOval(px + scaled(5), py + scaled(32), ts - scaled(10), scaled(13));

            if (objective.kind() == Quest.ObjectiveKind.GATHER) {
                int size = scaled(42);
                int drawX = px + (ts - size) / 2;
                int drawY = py + ts - size - scaled(3);
                g.drawImage(assets.spriteFit(objective.asset(), size, size), drawX, drawY, null);
            } else {
                int width = scaled(42);
                int height = scaled(54);
                drawShadow(g, px + scaled(8), py + scaled(38), scaled(34), scaled(9));
                drawCharacterSprite(g, objective.asset(), px + (ts - width) / 2, py - scaled(9) + pulse, width, height, 0, 1, (frame / 8) % 4);
            }

            g.setColor(new Color(255, 245, 174));
            int markerY = py + scaled(1) - pulse;
            Polygon marker = new Polygon(
                    new int[]{px + ts / 2, px + ts / 2 - scaled(6), px + ts / 2 + scaled(6)},
                    new int[]{markerY, markerY + scaled(10), markerY + scaled(10)},
                    3
            );
            g.fillPolygon(marker);
            g.setColor(new Color(44, 34, 19));
            g.drawPolygon(marker);
        }
    }

    private void drawCharacterSprite(Graphics2D g, String sprite, int x, int y, int width, int height, int facingDx, int facingDy, int step) {
        BufferedImage image = assets.spriteFit(sprite, width, height);
        int bob = step > 0 ? (step % 2 == 0 ? -scaled(2) : 0) : 0;
        int lean = facingDx == 0 ? 0 : facingDx * scaled(2);
        int lift = facingDy < 0 && step > 0 ? -scaled(1) : 0;
        if (facingDx < 0) {
            g.drawImage(image, x + width + lean, y + bob + lift, -width, height, null);
        } else {
            g.drawImage(image, x + lean, y + bob + lift, width, height, null);
        }
    }

    private boolean npcMoving(GameState.NpcMotion motion) {
        return state.worldTick - motion.moveStartTick() < GameState.NpcMotion.MOVE_TICKS
                && (motion.x() != motion.fromX() || motion.y() != motion.fromY());
    }

    private double renderNpcX(GameState.NpcMotion motion) {
        if (!npcMoving(motion)) {
            return motion.x();
        }
        double t = smoothStep((state.worldTick - motion.moveStartTick()) / (double) GameState.NpcMotion.MOVE_TICKS);
        return motion.fromX() + (motion.x() - motion.fromX()) * t;
    }

    private double renderNpcY(GameState.NpcMotion motion) {
        if (!npcMoving(motion)) {
            return motion.y();
        }
        double t = smoothStep((state.worldTick - motion.moveStartTick()) / (double) GameState.NpcMotion.MOVE_TICKS);
        return motion.fromY() + (motion.y() - motion.fromY()) * t;
    }

    private boolean playerMoving() {
        return state.currentMapId.equals(playerMoveMapId)
                && frame - playerMoveStartFrame < PLAYER_MOVE_FRAMES
                && (state.playerX != playerMoveFromX || state.playerY != playerMoveFromY);
    }

    private double renderPlayerX() {
        if (!playerMoving()) {
            return state.playerX;
        }
        double t = smoothStep((frame - playerMoveStartFrame) / (double) PLAYER_MOVE_FRAMES);
        return playerMoveFromX + (state.playerX - playerMoveFromX) * t;
    }

    private double renderPlayerY() {
        if (!playerMoving()) {
            return state.playerY;
        }
        double t = smoothStep((frame - playerMoveStartFrame) / (double) PLAYER_MOVE_FRAMES);
        return playerMoveFromY + (state.playerY - playerMoveFromY) * t;
    }

    private double smoothStep(double value) {
        double t = Math.max(0.0, Math.min(1.0, value));
        return t * t * (3.0 - 2.0 * t);
    }

    private void drawCityBuildingEntities(Graphics2D g, int camX, int camY) {
        String kind = state.world.kind(state.currentMapId);
        if (!"city".equals(kind) && !"village".equals(kind)) {
            return;
        }
        int ts = tileSize();
        for (CityBuilding building : state.world.cityBuildings(state.currentMapId)) {
            if (building.x2() < camX - 3 || building.y2() < camY - 3 || building.x1() >= camX + lastVisibleCols + 3 || building.y1() >= camY + lastVisibleRows + 3) {
                continue;
            }
            int lotX = (building.x1() - camX) * ts;
            int lotY = (building.y1() - camY) * ts;
            int lotW = building.width() * ts;
            int frontY = (building.y2() - camY + 1) * ts;
            int seed = Math.abs((building.x1() + building.x2()) * 928371 + (building.y1() + building.y2()) * 364479 + building.palette() * 811);

            g.setColor(new Color(17, 20, 22, 140));
            g.fillOval(lotX + scaled(4), frontY - scaled(14), Math.max(scaled(18), lotW - scaled(8)), scaled(14));
            g.setColor(new Color(45, 54, 55));
            g.fillRect(lotX + scaled(4), frontY - scaled(8), Math.max(scaled(10), lotW - scaled(8)), scaled(6));
            g.setColor(new Color(115, 122, 116));
            g.drawLine(lotX + scaled(4), frontY - scaled(9), lotX + Math.max(scaled(4), lotW - scaled(4)), frontY - scaled(9));

            int modules = buildingModuleCount(building);
            for (int index = 0; index < modules; index++) {
                String asset = buildingSprite(building, seed, index);
                int targetH = Math.max(scaled(78), Math.min(scaled(112), building.depth() * ts + scaled(24)));
                int targetW = Math.max(scaled(46), Math.min(lotW / Math.max(1, modules) + scaled(22), scaled(84)));
                int centerX = lotX + (int) Math.round((index + 0.5) * lotW / modules);
                int jitter = ((seed >> (index * 4)) & 7) - 3;
                int drawX = centerX - targetW / 2 + jitter * ts / GameConfig.TILE;
                int drawY = frontY - targetH - scaled(3);
                g.drawImage(assets.spriteFit(asset, targetW, targetH), drawX, drawY, null);
            }

            for (TilePoint door : state.world.cityBuildingDoorTiles(building)) {
                if (door.x() < camX || door.x() >= camX + lastVisibleCols || door.y() < camY || door.y() >= camY + lastVisibleRows) {
                    continue;
                }
                int markerX = (door.x() - camX) * ts;
                int markerY = (door.y() - camY + 1) * ts;
                g.setColor(new Color(45, 36, 29));
                g.fillRect(markerX + scaled(11), markerY - scaled(9), ts - scaled(22), scaled(7));
                g.setColor(new Color(122, 96, 65));
                g.drawRect(markerX + scaled(11), markerY - scaled(9), ts - scaled(22), scaled(7));
                g.setColor(new Color(202, 162, 98));
                g.fillRect(markerX + scaled(14), markerY - scaled(3), ts - scaled(28), scaled(2));
            }

            if (building.width() >= 4) {
                int lanternH = scaled(18);
                int lanternW = scaled(10);
                g.drawImage(assets.spriteFit("city_lantern", lanternW, lanternH), lotX + scaled(4), frontY - lanternH - scaled(20), null);
                g.drawImage(assets.spriteFit("city_lantern", lanternW, lanternH), lotX + lotW - lanternW - scaled(4), frontY - lanternH - scaled(20), null);
            }
        }
    }

    private int buildingModuleCount(CityBuilding building) {
        boolean civic = List.of("hall", "guild", "barracks", "warehouse").contains(building.style());
        if (civic) {
            return Math.max(1, Math.min(4, (building.width() + 2) / 3));
        }
        int modules = Math.max(1, Math.min(building.width(), (building.width() + 1) / 2));
        if ("row".equals(building.style()) && building.width() >= 6) {
            modules = Math.min(building.width(), modules + 1);
        }
        return modules;
    }

    private String buildingSprite(CityBuilding building, int seed, int index) {
        String[] sprites = switch (building.style()) {
            case "hall" -> new String[]{"city_building_stone_hall", "city_building_stone_shop"};
            case "guild" -> new String[]{"city_building_stone_shop", "city_building_stone_hall"};
            case "barracks" -> new String[]{"city_building_stone_tower", "city_building_stone_shop"};
            case "warehouse" -> new String[]{"city_building_stone_shop", "city_building_town_shop"};
            case "shop" -> new String[]{"city_building_town_shop", "city_building_town_gabled", "city_building_town_small"};
            case "inn" -> new String[]{"city_building_town_gabled", "city_building_town_shop", "city_building_town_tall"};
            case "row" -> new String[]{"city_building_town_narrow", "city_building_town_gabled", "city_building_town_tall", "city_building_town_small", "city_building_town_shop"};
            default -> new String[]{"city_building_town_gabled", "city_building_town_small", "city_building_town_tall"};
        };
        return sprites[Math.floorMod(seed + building.palette() + index * 3, sprites.length)];
    }

    private void drawHouseTile(Graphics2D g, char tile, int wx, int wy, int px, int py) {
        if (tile == 'o') {
            drawHouseWallTile(g, wx, wy, px, py);
        } else if (tile == 'k') {
            drawHouseFurnitureTile(g, wx, wy, px, py);
        } else if (tile == 'i' || tile == 'e' || tile == 'z') {
            drawHouseFloorTile(g, tile, wx, wy, px, py);
        }
    }

    private void drawHouseFloorTile(Graphics2D g, char tile, int wx, int wy, int px, int py) {
        int ts = tileSize();
        int seed = Math.abs(wx * 928371 + wy * 364479 + tile * 71);
        g.setColor(((wx + wy) & 1) == 0 ? new Color(139, 101, 66) : new Color(124, 89, 58));
        g.fillRect(px, py, ts, ts);
        g.setColor(new Color(95, 63, 42));
        for (int y = py + scaled(7); y < py + ts; y += scaled(10)) {
            g.drawLine(px, y, px + ts, y + ((seed >> (y & 7)) & 1));
        }
        g.setColor(new Color(164, 122, 80, 120));
        for (int x = px + scaled(8); x < px + ts; x += scaled(16)) {
            g.drawLine(x, py + scaled(2), x, py + ts - scaled(2));
        }
        if (tile == 'z') {
            g.setColor((seed & 1) == 0 ? new Color(63, 107, 118) : new Color(155, 77, 68));
            g.fillRect(px + scaled(4), py + scaled(7), ts - scaled(8), ts - scaled(14));
            g.setColor(new Color(208, 180, 110));
            g.drawRect(px + scaled(9), py + scaled(13), ts - scaled(18), ts - scaled(26));
        } else if (tile == 'e') {
            g.setColor(new Color(81, 51, 31));
            g.fillRect(px + scaled(8), py + scaled(10), ts - scaled(16), ts - scaled(14));
            g.setColor(new Color(214, 189, 114));
            g.drawLine(px + scaled(12), py + scaled(15), px + ts - scaled(12), py + scaled(15));
            g.fillOval(px + ts - scaled(17), py + scaled(29), scaled(4), scaled(4));
        }
    }

    private void drawHouseWallTile(Graphics2D g, int wx, int wy, int px, int py) {
        int ts = tileSize();
        int seed = Math.abs(wx * 928371 + wy * 364479 + 79);
        g.setColor(new Color(111, 87, 64));
        g.fillRect(px, py, ts, ts);
        g.setColor(new Color(141, 116, 80));
        g.fillRect(px + scaled(2), py + scaled(2), ts - scaled(4), ts - scaled(4));
        g.setColor(new Color(79, 56, 40));
        g.drawRect(px + scaled(2), py + scaled(2), ts - scaled(4), ts - scaled(4));
        g.setColor(new Color(178, 154, 109));
        for (int y = py + scaled(9); y < py + ts; y += scaled(13)) {
            g.drawLine(px + scaled(4), y, px + ts - scaled(4), y);
        }
        if (seed % 5 == 0) {
            g.setColor(new Color(78, 52, 36));
            g.fillRect(px + scaled(17), py + scaled(15), scaled(14), scaled(13));
            g.setColor(new Color(209, 154, 67));
            g.fillRect(px + scaled(19), py + scaled(17), scaled(10), scaled(9));
        } else if (seed % 5 == 1) {
            g.setColor(new Color(90, 56, 34));
            g.fillRect(px + scaled(10), py + scaled(11), ts - scaled(20), scaled(5));
            g.setColor(new Color(184, 138, 79));
            for (int x = px + scaled(13); x < px + ts - scaled(12); x += scaled(7)) {
                g.fillRect(x, py + scaled(17), scaled(3), scaled(11));
            }
        }
    }

    private void drawHouseFurnitureTile(Graphics2D g, int wx, int wy, int px, int py) {
        drawHouseFloorTile(g, 'i', wx, wy, px, py);
        int kind = Math.abs(wx * 928371 + wy * 364479 + 83) % 4;
        if (kind == 0) {
            g.setColor(new Color(109, 67, 41));
            g.fillRect(px + scaled(7), py + scaled(12), scaled(32), scaled(21));
            g.setColor(new Color(217, 193, 133));
            g.fillRect(px + scaled(11), py + scaled(15), scaled(24), scaled(6));
            g.setColor(new Color(143, 77, 63));
            g.fillRect(px + scaled(11), py + scaled(22), scaled(24), scaled(8));
        } else if (kind == 1) {
            g.setColor(new Color(123, 77, 45));
            g.fillOval(px + scaled(12), py + scaled(13), scaled(24), scaled(24));
            g.setColor(new Color(183, 139, 81));
            g.fillOval(px + scaled(17), py + scaled(18), scaled(14), scaled(14));
        } else if (kind == 2) {
            g.setColor(new Color(91, 58, 39));
            g.fillRect(px + scaled(9), py + scaled(8), scaled(29), scaled(31));
            g.setColor(new Color(162, 120, 72));
            for (int y = py + scaled(13); y < py + scaled(36); y += scaled(6)) {
                g.drawLine(px + scaled(11), y, px + scaled(36), y);
            }
            g.setColor(new Color(201, 157, 94));
            for (int x = px + scaled(13); x < px + scaled(35); x += scaled(7)) {
                g.fillRect(x, py + scaled(15), scaled(3), scaled(19));
            }
        } else {
            g.setColor(new Color(116, 69, 43));
            g.fillRect(px + scaled(8), py + scaled(18), scaled(32), scaled(16));
            g.setColor(new Color(91, 58, 39));
            g.fillRect(px + scaled(13), py + scaled(11), scaled(22), scaled(10));
            g.setColor(new Color(106, 141, 85));
            g.fillOval(px + scaled(18), py + scaled(20), scaled(6), scaled(6));
        }
    }

    private void drawTerrainEdges(Graphics2D g, char tile, int wx, int wy, int px, int py) {
        if (!"overworld".equals(state.world.kind(state.currentMapId))) {
            return;
        }
        int[][] dirs = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        for (int i = 0; i < dirs.length; i++) {
            int nx = wx + dirs[i][0];
            int ny = wy + dirs[i][1];
            char other = state.world.tileAt(state.currentMapId, nx, ny);
            if (other == tile || other == 'r' || tile == 'r' || other == 'c' || other == 'u' || tile == 'c' || tile == 'u') {
                continue;
            }
            if (!shouldBlend(tile, other)) {
                continue;
            }
            Color blend = edgeColor(tile, other);
            g.setColor(blend);
            int wobble = Math.abs((wx * 31 + wy * 17 + i * 11) % 7) - 3;
            int ts = tileSize();
            switch (i) {
                case 0 -> g.fillPolygon(new int[]{px, px + ts, px + ts, px},
                        new int[]{py, py, py + scaled(5) + wobble, py + scaled(9) - wobble}, 4);
                case 1 -> g.fillPolygon(new int[]{px + ts, px + ts, px + ts - scaled(8) + wobble, px + ts - scaled(5) - wobble},
                        new int[]{py, py + ts, py + ts, py}, 4);
                case 2 -> g.fillPolygon(new int[]{px, px + ts, px + ts, px},
                        new int[]{py + ts - scaled(7) + wobble, py + ts - scaled(10) - wobble, py + ts, py + ts}, 4);
                case 3 -> g.fillPolygon(new int[]{px, px + scaled(8) - wobble, px + scaled(5) + wobble, px},
                        new int[]{py, py, py + ts, py + ts}, 4);
                default -> {
                }
            }
        }
    }

    private boolean shouldBlend(char tile, char other) {
        return isNatural(tile) && isNatural(other) && tile != other;
    }

    private boolean isNatural(char tile) {
        return "gfsnvbmqw".indexOf(tile) >= 0;
    }

    private Color edgeColor(char tile, char other) {
        if (tile == 'w' || other == 'w') {
            return new Color(122, 191, 139, 170);
        }
        Color a = Terrain.color(tile);
        Color b = Terrain.color(other);
        return new Color((a.getRed() + b.getRed()) / 2, (a.getGreen() + b.getGreen()) / 2, (a.getBlue() + b.getBlue()) / 2, 112);
    }

    private void drawRoadConnectors(Graphics2D g, int camX, int camY) {
        int ts = tileSize();
        String mapKind = state.world.kind(state.currentMapId);
        for (int sy = 0; sy < lastVisibleRows; sy++) {
            for (int sx = 0; sx < lastVisibleCols; sx++) {
                int wx = camX + sx;
                int wy = camY + sy;
                char tile = state.world.tileAt(state.currentMapId, wx, wy);
                if (tile != 'r' && tile != 'q') {
                    continue;
                }
                int px = sx * ts;
                int py = sy * ts;
                if ("overworld".equals(mapKind) || "village".equals(mapKind)) {
                    int bits = roadBits(wx, wy);
                    String suffix = "0" + Integer.toHexString(bits);
                    String asset = "road_overlay_" + suffix.substring(suffix.length() - 2);
                    drawSoftRoadShoulder(g, wx, wy, px, py);
                    int inset = scaled(4);
                    g.drawImage(assets.image(asset, ts + inset * 2, ts + inset * 2), px - inset, py - inset, null);
                    drawRoadEdgeFlecking(g, wx, wy, px, py);
                    continue;
                }
                Color road = mapKind.equals("dungeon")
                        ? new Color(93, 82, 115)
                        : new Color(176, 130, 76);
                g.setColor(new Color(52, 38, 22, 150));
                drawRoadShape(g, px + 1, py + 1, wx, wy, scaled(18));
                g.setColor(road);
                drawRoadShape(g, px, py, wx, wy, scaled(16));
                g.setColor(new Color(232, 195, 124, 80));
                if (connectsRoad(wx - 1, wy) || connectsRoad(wx + 1, wy)) {
                    g.drawLine(px + scaled(4), py + ts / 2 - scaled(5), px + ts - scaled(4), py + ts / 2 - scaled(5));
                } else {
                    g.drawLine(px + ts / 2 - scaled(5), py + scaled(4), px + ts / 2 - scaled(5), py + ts - scaled(4));
                }
            }
        }
    }

    private int roadBits(int wx, int wy) {
        int bits = 0;
        if (connectsRoad(wx, wy - 1)) {
            bits |= 1;
        }
        if (connectsRoad(wx, wy + 1)) {
            bits |= 2;
        }
        if (connectsRoad(wx - 1, wy)) {
            bits |= 4;
        }
        if (connectsRoad(wx + 1, wy)) {
            bits |= 8;
        }
        return bits;
    }

    private void drawRoadShape(Graphics2D g, int px, int py, int wx, int wy, int half) {
        int c = tileSize() / 2;
        g.fillRect(px + c - half / 2, py + c - half / 2, half, half);
        if (connectsRoad(wx, wy - 1)) {
            g.fillRect(px + c - half / 2, py, half, c);
        }
        if (connectsRoad(wx + 1, wy)) {
            g.fillRect(px + c, py + c - half / 2, c, half);
        }
        if (connectsRoad(wx, wy + 1)) {
            g.fillRect(px + c - half / 2, py + c, half, c);
        }
        if (connectsRoad(wx - 1, wy)) {
            g.fillRect(px, py + c - half / 2, c, half);
        }
    }

    private boolean connectsRoad(int x, int y) {
        char tile = state.world.tileAt(state.currentMapId, x, y);
        return tile == 'r' || tile == 'q' || tile == 'c' || tile == 'u' || tile == 'd';
    }

    private char visibleTerrainTile(char tile, int wx, int wy) {
        String mapKind = state.world.kind(state.currentMapId);
        if ((tile == 'r' || tile == 'q') && ("overworld".equals(mapKind) || "village".equals(mapKind))) {
            return roadUnderlayTile(tile, wx, wy);
        }
        if (WorldMap.OVERWORLD_ID.equals(state.currentMapId) && (tile == 'c' || tile == 'u')) {
            return settlementUnderlayTile(wx, wy);
        }
        return tile;
    }

    private char settlementUnderlayTile(int wx, int wy) {
        char best = 'g';
        int bestScore = -1;
        char[] candidates = {'g', 'f', 's', 'n', 'v', 'b', 'm', 'q'};
        for (char candidate : candidates) {
            int score = 0;
            for (int oy = -5; oy <= 5; oy++) {
                for (int ox = -5; ox <= 5; ox++) {
                    if (ox == 0 && oy == 0) {
                        continue;
                    }
                    char neighbor = state.world.tileAt(state.currentMapId, wx + ox, wy + oy);
                    if (neighbor != candidate) {
                        continue;
                    }
                    int distance = Math.max(Math.abs(ox), Math.abs(oy));
                    score += Math.max(1, 6 - distance);
                }
            }
            if (score > bestScore || (score == bestScore && terrainBlendPriority(candidate) > terrainBlendPriority(best))) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    private int terrainBlendPriority(char tile) {
        return switch (tile) {
            case 'n' -> 8;
            case 's' -> 7;
            case 'v' -> 6;
            case 'b' -> 5;
            case 'f' -> 4;
            case 'm', 'q' -> 3;
            case 'g' -> 2;
            default -> 1;
        };
    }

    private char roadUnderlayTile(char tile, int wx, int wy) {
        if (tile == 'q') {
            return 'q';
        }
        char best = 'g';
        int bestScore = -1;
        char[] candidates = {'g', 'f', 's', 'n', 'v', 'b', 'm', 'q'};
        for (char candidate : candidates) {
            int score = 0;
            for (int oy = -2; oy <= 2; oy++) {
                for (int ox = -2; ox <= 2; ox++) {
                    if (ox == 0 && oy == 0) {
                        continue;
                    }
                    char neighbor = state.world.tileAt(state.currentMapId, wx + ox, wy + oy);
                    if (neighbor == candidate) {
                        score += Math.abs(ox) + Math.abs(oy) <= 1 ? 3 : 1;
                    }
                }
            }
            if (score > bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    private void drawSoftRoadShoulder(Graphics2D g, int wx, int wy, int px, int py) {
        char underlay = roadUnderlayTile(state.world.tileAt(state.currentMapId, wx, wy), wx, wy);
        Color terrain = Terrain.color(underlay);
        Graphics2D roadG = (Graphics2D) g.create();
        roadG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
        roadG.setColor(new Color(196, 153, 92, 132));
        drawRoadShape(roadG, px, py, wx, wy, scaled(30));
        roadG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.22f));
        roadG.setColor(terrain);
        drawRoadShape(roadG, px, py, wx, wy, scaled(36));
        roadG.dispose();
    }

    private void drawRoadEdgeFlecking(Graphics2D g, int wx, int wy, int px, int py) {
        int ts = tileSize();
        int seed = Math.abs(wx * 928371 + wy * 364479 + 31337);
        g.setColor(new Color(93, 72, 42, 72));
        for (int i = 0; i < 6; i++) {
            int offset = 5 + Math.floorMod(seed >> (i * 3), Math.max(1, ts - 10));
            int fleck = Math.max(1, scaled(2 + (seed + i) % 3));
            if (connectsRoad(wx - 1, wy) || connectsRoad(wx + 1, wy)) {
                g.fillOval(px + offset, py + scaled(8 + i % 2 * 25), fleck, fleck);
            }
            if (connectsRoad(wx, wy - 1) || connectsRoad(wx, wy + 1)) {
                g.fillOval(px + scaled(8 + i % 2 * 25), py + offset, fleck, fleck);
            }
        }
    }

    private void drawMountainMassifOverlays(Graphics2D g, int camX, int camY) {
        if (!WorldMap.OVERWORLD_ID.equals(state.currentMapId)) {
            return;
        }
        List<TilePoint> anchors = new ArrayList<>();
        for (int wy = camY - 4; wy < camY + lastVisibleRows + 4; wy++) {
            for (int wx = camX - 4; wx < camX + lastVisibleCols + 4; wx++) {
                if (isMountainMassifAnchor(wx, wy)) {
                    anchors.add(new TilePoint(wx, wy));
                }
            }
        }
        anchors.sort((a, b) -> {
            int byY = Integer.compare(a.y(), b.y());
            return byY != 0 ? byY : Integer.compare(a.x(), b.x());
        });
        int ts = tileSize();
        for (TilePoint anchor : anchors) {
            int seed = mountainAnchorHash(anchor.x(), anchor.y());
            int drawSize = scaled(92 + seed % 34);
            int jitterX = scaled(Math.floorMod(seed / 7, 15) - 7);
            int jitterY = scaled(Math.floorMod(seed / 17, 11) - 5);
            int px = (anchor.x() - camX) * ts + ts / 2 - drawSize / 2 + jitterX;
            int py = (anchor.y() - camY) * ts + ts - drawSize + scaled(8) + jitterY;
            drawShadow(g, px + drawSize / 5, py + drawSize - scaled(15), drawSize * 3 / 5, scaled(12));
            g.drawImage(assets.spriteFit("mountain_massif", drawSize, drawSize), px, py, null);
        }
    }

    private boolean isMountainMassifAnchor(int wx, int wy) {
        if (state.world.tileAt(state.currentMapId, wx, wy) != 'm') {
            return false;
        }
        int mountainCount = 0;
        int closeMountainCount = 0;
        for (int oy = -2; oy <= 2; oy++) {
            for (int ox = -2; ox <= 2; ox++) {
                char tile = state.world.tileAt(state.currentMapId, wx + ox, wy + oy);
                if (tile == 'm') {
                    mountainCount++;
                    if (Math.max(Math.abs(ox), Math.abs(oy)) <= 1) {
                        closeMountainCount++;
                    }
                }
                if (Math.abs(ox) + Math.abs(oy) <= 1 && tile == 'q') {
                    return false;
                }
            }
        }
        if (closeMountainCount < 4 || mountainCount < 8) {
            return false;
        }
        int chance = Math.min(82, 28 + mountainCount * 5 + closeMountainCount * 3);
        int hash = mountainAnchorHash(wx, wy);
        return Math.floorMod(hash, 100) < chance;
    }

    private int mountainAnchorHash(int wx, int wy) {
        return Math.abs(wx * 374761393 + wy * 668265263 + 6203);
    }

    private void drawSettlementOverlays(Graphics2D g, int camX, int camY) {
        if (!WorldMap.OVERWORLD_ID.equals(state.currentMapId)) {
            return;
        }
        drawSettlement(g, camX, camY, 82, 105, "city_overworld_cluster_large", 230);
        drawSettlement(g, camX, camY, 152, 145, "city_overworld_cluster_large", 230);
        drawSettlement(g, camX, camY, 205, 78, "city_overworld_cluster_large", 230);
        drawSettlement(g, camX, camY, 228, 185, "city_overworld_cluster_large", 230);
        drawSettlement(g, camX, camY, 150, 230, "city_overworld_cluster_large", 230);
        drawSettlement(g, camX, camY, 112, 158, "city_overworld_village_green", 176);
        drawSettlement(g, camX, camY, 83, 62, "city_overworld_village_snow", 176);
        drawSettlement(g, camX, camY, 102, 245, "city_overworld_village_desert", 176);
        drawSettlement(g, camX, camY, 240, 153, "city_overworld_village_marsh", 176);
    }

    private void drawSettlement(Graphics2D g, int camX, int camY, int wx, int wy, String asset, int size) {
        if (wx < camX - 3 || wy < camY - 3 || wx >= camX + lastVisibleCols + 3 || wy >= camY + lastVisibleRows + 3) {
            return;
        }
        int ts = tileSize();
        int drawSize = scaled(size);
        int px = (wx - camX) * ts + ts / 2 - drawSize / 2;
        int py = (wy - camY) * ts + ts / 2 - drawSize / 2;
        drawSettlementApron(g, wx, wy, px, py, drawSize);
        drawShadow(g, px + drawSize / 5, py + drawSize - scaled(18), drawSize * 3 / 5, scaled(16));
        g.drawImage(assets.spriteFit(asset, drawSize, drawSize), px, py, null);
    }

    private void drawSettlementApron(Graphics2D g, int wx, int wy, int px, int py, int drawSize) {
        char underlay = settlementUnderlayTile(wx, wy);
        Color terrain = Terrain.color(underlay);
        Graphics2D apron = (Graphics2D) g.create();
        apron.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int cx = px + drawSize / 2;
        int cy = py + drawSize / 2 + scaled(10);
        int radiusX = Math.max(scaled(70), drawSize / 2 + scaled(18));
        int radiusY = Math.max(scaled(56), drawSize / 3 + scaled(14));
        apron.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.22f));
        apron.setColor(new Color(terrain.getRed(), terrain.getGreen(), terrain.getBlue(), 168));
        apron.fillOval(cx - radiusX, cy - radiusY, radiusX * 2, radiusY * 2);

        int seed = Math.abs(wx * 928371 + wy * 364479 + assetLikeHash(underlay));
        apron.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.16f));
        for (int i = 0; i < 12; i++) {
            int angleSeed = seed + i * 61;
            double angle = (angleSeed % 628) / 100.0;
            int fleckX = cx + (int) Math.round(Math.cos(angle) * (radiusX * (55 + angleSeed % 34) / 100.0));
            int fleckY = cy + (int) Math.round(Math.sin(angle) * (radiusY * (50 + angleSeed % 38) / 100.0));
            int fleckW = scaled(12 + (angleSeed % 20));
            int fleckH = scaled(5 + ((angleSeed / 7) % 10));
            apron.setColor(apronDetailColor(underlay, angleSeed));
            apron.fillOval(fleckX - fleckW / 2, fleckY - fleckH / 2, fleckW, fleckH);
        }
        apron.dispose();
    }

    private int assetLikeHash(char tile) {
        return switch (tile) {
            case 'n' -> 101;
            case 's' -> 107;
            case 'v' -> 113;
            case 'b' -> 149;
            case 'f' -> 157;
            case 'm', 'q' -> 131;
            default -> 41;
        };
    }

    private Color apronDetailColor(char tile, int seed) {
        return switch (tile) {
            case 'n' -> new Color(235, 246, 246, 120);
            case 's' -> new Color(143, 99, 57, 112);
            case 'v' -> new Color(48, 92, 76, 112);
            case 'b' -> new Color(119, 85, 65, 112);
            case 'f' -> new Color(45, 99, 55, 112);
            case 'm', 'q' -> new Color(118, 121, 119, 112);
            default -> seed % 2 == 0 ? new Color(92, 139, 64, 112) : new Color(157, 146, 82, 104);
        };
    }

    private void drawShadow(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(new Color(0, 0, 0, 85));
        g.fillOval(x, y, w, h);
    }

    private void drawGroundPropOverlays(Graphics2D g, int camX, int camY) {
        int ts = tileSize();
        int inset = scaled(2);
        for (WorldProp prop : state.world.props(state.currentMapId)) {
            if (!isGroundProp(prop.asset())) {
                continue;
            }
            if (prop.x() < camX || prop.y() < camY || prop.x() >= camX + lastVisibleCols || prop.y() >= camY + lastVisibleRows) {
                continue;
            }
            int px = (prop.x() - camX) * ts - inset;
            int py = (prop.y() - camY) * ts - inset;
            g.drawImage(assets.image(prop.asset(), ts + inset * 2, ts + inset * 2), px, py, null);
        }
    }

    private boolean isGroundProp(String asset) {
        return asset.equals("location_farmland_tilled")
                || asset.equals("location_graveyard_dirt")
                || asset.equals("location_graveyard_path");
    }

    private boolean castsPropShadow(String asset) {
        return !asset.equals("location_farmland_tilled")
                && !asset.equals("location_farmland_wheat")
                && !asset.equals("location_graveyard_dirt")
                && !asset.equals("location_graveyard_path");
    }

    private void drawCloudLayer(Graphics2D g, int camX, int camY) {
        if (!WorldMap.OVERWORLD_ID.equals(state.currentMapId)) {
            return;
        }
        Graphics2D cloudG = (Graphics2D) g.create();
        cloudG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.95f));
        for (int i = 0; i < 4; i++) {
            int sizeW = scaled(202 + i * 16);
            int sizeH = scaled(82 + i * 6);
            int drift = (frame * (1 + i) / 2 + i * 173) % (GameConfig.MAP_COLS * GameConfig.TILE + 260);
            int x = drift - 220;
            int y = scaled(8 + i * 30) + (int) Math.round(Math.sin((frame + i * 37) * 0.025) * scaled(5));
            if (i == 2) {
                x = GameConfig.MAP_COLS * GameConfig.TILE - drift / 2;
                y += scaled(18);
            }
            cloudG.drawImage(assets.image("cloud_billow_" + (i % 3), sizeW, sizeH), x, y, null);
        }
        cloudG.dispose();
    }

    private String terrainImageName(char tile, int wx, int wy) {
        String base = Terrain.assetName(tile);
        int count = switch (base) {
            case "grass", "forest", "tundra" -> 8;
            case "water", "road", "desert", "marsh", "badlands", "mountain", "mountain_massif_tile" -> 4;
            default -> 1;
        };
        if (count <= 1) {
            return base;
        }
        int variant = Math.abs((wx * 928371 + wy * 364479 + base.hashCode()) % count);
        if (variant == 0) {
            return base;
        }
        return base + "_variant_" + variant;
    }

    private void drawSidebar(Graphics2D g) {
        int left = GameConfig.MAP_COLS * GameConfig.TILE;
        g.setColor(new Color(18, 20, 29));
        g.fillRect(left, 0, GameConfig.SIDEBAR_WIDTH, viewHeight());
        g.setColor(new Color(59, 64, 82));
        g.drawLine(left, 0, left, viewHeight());

        Actor p = state.player;
        int x = left + 28;
        int y = 42;
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.setColor(new Color(243, 238, 219));
        g.drawString(p.className, x, y);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(198, 202, 211));
        g.drawString("Level " + p.level + "   Gold " + p.gold, x, y + 28);
        bar(g, x, y + 52, p.hp, p.maxHp, new Color(190, 76, 82), "HP");
        bar(g, x, y + 86, p.mp, p.maxMp, new Color(84, 129, 205), "MP");
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(198, 202, 211));
        String partyLine = state.allies.isEmpty()
                ? "Party: no allies"
                : "Party: " + String.join(", ", state.allies.stream().map(ally -> ally.name).toList());
        wrap(g, partyLine, x, y + 122, GameConfig.SIDEBAR_WIDTH - 56, 17);

        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(new Color(230, 225, 206));
        g.drawString("Position", x, y + 158);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(198, 202, 211));
        g.drawString(state.world.label(state.currentMapId), x, y + 183);
        g.drawString(state.playerX + ", " + state.playerY + "  " + Terrain.name(state.world.tileAt(state.currentMapId, state.playerX, state.playerY)), x, y + 205);
        wrap(g, state.status, x, y + 234, GameConfig.SIDEBAR_WIDTH - 56, 20);

        int actionY = 282;
        sidebarButton(g, left + 28, actionY, 246, 30, "Talk / Enter", state::interact);
        sidebarButton(g, left + 28, actionY + 38, 246, 30, "Quest Log", state::toggleQuestLog);
        sidebarButton(g, left + 28, actionY + 76, 246, 30, "World Map", state::toggleWorldMap);
        sidebarButton(g, left + 28, actionY + 114, 116, 30, "Skills (" + state.player.skillPoints + ")", state::toggleSkills);
        sidebarButton(g, left + 158, actionY + 114, 116, 30, "Inventory", state::toggleInventory);

        int miniY = actionY + 166;
        drawMiniMap(g, left + 28, miniY, 112, 70);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(230, 225, 206));
        g.drawString("Zoom", left + 158, miniY + 12);
        sidebarButton(g, left + 158, miniY + 24, 34, 26, "-", () -> state.adjustZoom(-10));
        sidebarButton(g, left + 240, miniY + 24, 34, 26, "+", () -> state.adjustZoom(10));
        g.setColor(new Color(70, 76, 94));
        g.drawRect(left + 196, miniY + 35, 38, 5);
        g.setColor(new Color(235, 211, 132));
        int knobX = left + 196 + (int) Math.round((state.zoom - 70) / 80.0 * 38);
        g.fillOval(knobX - 4, miniY + 31, 9, 9);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(198, 202, 211));
        g.drawString(state.zoom + "%", left + 158, miniY + 66);
        sidebarButton(g, left + 158, miniY + 78, 116, 28, "Fullscreen", fullscreenToggle);

        int controlsY = viewHeight() - 96;
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(new Color(230, 225, 206));
        g.drawString("Log", x, controlsY);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(new Color(198, 202, 211));
        wrap(g, state.status, x, controlsY + 24, GameConfig.SIDEBAR_WIDTH - 56, 18);
    }

    private void sidebarButton(Graphics2D g, int x, int y, int w, int h, String label, Runnable action) {
        actionButton(g, x, y, w, h, label, action, new Color(35, 39, 54), new Color(86, 98, 128), true);
    }

    private void actionButton(
            Graphics2D g,
            int x,
            int y,
            int w,
            int h,
            String label,
            Runnable action,
            Color fill,
            Color border,
            boolean enabled
    ) {
        if (enabled) {
            buttons.add(new UiButton(new Rectangle(x, y, w, h), label, () -> {
                action.run();
                repaint();
            }));
        }
        g.setColor(enabled ? fill : new Color(42, 44, 52));
        g.fillRoundRect(x, y, w, h, 6, 6);
        g.setColor(enabled ? border : new Color(73, 75, 84));
        g.drawRoundRect(x, y, w, h, 6, 6);
        g.setFont(new Font("SansSerif", Font.BOLD, h >= 34 ? 13 : 12));
        g.setColor(enabled ? new Color(238, 239, 244) : new Color(142, 146, 156));
        FontMetrics metrics = g.getFontMetrics();
        String display = label;
        while (metrics.stringWidth(display) > w - 12 && display.length() > 4) {
            display = display.substring(0, display.length() - 4) + "...";
        }
        g.drawString(display, x + (w - metrics.stringWidth(display)) / 2, y + h / 2 + 5);
    }

    private void drawMiniMap(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(new Color(11, 13, 20));
        g.fillRect(x, y, w, h);
        int stepX = Math.max(1, WorldMap.COLS / w);
        int stepY = Math.max(1, WorldMap.ROWS / h);
        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                char tile = state.world.tileAt(WorldMap.OVERWORLD_ID, px * stepX, py * stepY);
                g.setColor(Terrain.color(tile));
                g.fillRect(x + px, y + py, 1, 1);
            }
        }
        if (WorldMap.OVERWORLD_ID.equals(state.currentMapId)) {
            int playerX = x + Math.min(w - 1, state.playerX * w / WorldMap.COLS);
            int playerY = y + Math.min(h - 1, state.playerY * h / WorldMap.ROWS);
            g.setColor(new Color(255, 245, 174));
            g.fillOval(playerX - 2, playerY - 2, 5, 5);
        }
        for (GameState.QuestObjective objective : state.activeQuestObjectives()) {
            if (!WorldMap.OVERWORLD_ID.equals(objective.mapId())) {
                continue;
            }
            int ox = x + Math.min(w - 1, objective.x() * w / WorldMap.COLS);
            int oy = y + Math.min(h - 1, objective.y() * h / WorldMap.ROWS);
            g.setColor(objective.kind() == Quest.ObjectiveKind.GATHER ? new Color(112, 220, 128) : new Color(233, 89, 83));
            g.fillRect(ox - 2, oy - 2, 5, 5);
        }
        g.setColor(new Color(81, 88, 108));
        g.drawRect(x, y, w, h);
    }

    private void drawBattle(Graphics2D g) {
        Battle battle = state.battle;
        if (battle == null) {
            return;
        }
        int x = 0;
        int y = 0;
        int w = GameConfig.MAP_COLS * GameConfig.TILE;
        int h = GameConfig.MAP_ROWS * GameConfig.TILE;
        drawBattleBackdrop(g, battle, x, y, w, h);
        g.setColor(new Color(124, 112, 83));
        g.setStroke(new BasicStroke(2f));
        g.drawRect(x, y, w - 1, h - 1);

        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(244, 239, 220));
        g.drawString("Battle", x + 34, y + 52);
        Actor active = battle.activeActor();

        List<Actor> party = battle.partyMembers();
        int visibleParty = Math.min(4, party.size());
        for (int i = 0; i < visibleParty; i++) {
            drawBattlePartyMember(g, battle, party.get(i), party.get(i) == active, x, y, w, h);
        }
        if (party.size() > visibleParty) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.setColor(new Color(196, 198, 205));
            g.drawString("+" + (party.size() - visibleParty) + " waiting", x + 52, y + h - 322);
        }

        for (Actor foe : battle.enemies()) {
            drawBattleEnemy(g, battle, foe, x, y, w, h);
        }

        drawBattleEffect(g, battle, x, y, w, h);
        drawBattleFloaters(g, battle, x, y);
        if (battle.flashTimer > 0) {
            Graphics2D flash = (Graphics2D) g.create();
            flash.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(0.18f, battle.flashTimer / 24.0f)));
            flash.setColor(Color.WHITE);
            flash.fillRect(x, y, w, h);
            flash.dispose();
        }

        if (active != null) {
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.setColor(new Color(246, 224, 151));
            g.drawString(active.name + "'s turn", x + 42, y + h - 278);
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g.setColor(new Color(218, 220, 226));
            String abilityText = active.abilities.isEmpty()
                    ? "No abilities"
                    : "1-" + active.abilities.size() + " " + active.abilities.stream().map(Ability::name).toList();
            wrap(g, abilityText, x + 42, y + h - 254, 520, 18);
        }
        Actor selectedEnemy = battle.selectedEnemy();
        Actor selectedAlly = battle.selectedPartyMember();
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(new Color(226, 229, 236));
        g.drawString("Enemy target: " + (selectedEnemy == null ? "none" : selectedEnemy.name), x + w - 356, y + 44);
        g.drawString("Ally target: " + (selectedAlly == null ? "none" : selectedAlly.name), x + w - 356, y + 68);

        g.setColor(new Color(12, 14, 22, 178));
        g.fillRoundRect(x + 34, y + h - 230, 620, 104, 8, 8);
        g.setColor(new Color(85, 92, 118));
        g.drawRoundRect(x + 34, y + h - 230, 620, 104, 8, 8);
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g.setColor(new Color(218, 220, 226));
        int logY = y + h - 200;
        List<String> messages = new ArrayList<>(battle.log);
        int start = Math.max(0, messages.size() - 4);
        for (int i = start; i < messages.size(); i++) {
            g.drawString(messages.get(i), x + 54, logY);
            logY += 24;
        }
        drawBattleActionBar(g, battle, x + 34, y + h - 112, w - 68, 88);
    }

    private void drawBattleBackdrop(Graphics2D g, Battle battle, int x, int y, int w, int h) {
        g.drawImage(assets.cover(battle.backdrop, w, h), x, y, null);
        g.setPaint(new GradientPaint(x, y, new Color(8, 10, 16, 70), x, y + h, new Color(8, 10, 16, 218)));
        g.fillRect(x, y, w, h);
        g.setPaint(null);
        g.setColor(new Color(20, 18, 18, 92));
        g.fillRect(x, y + h - 300, w, 300);
    }

    private String battleBackdropName() {
        char terrain = state.world.tileAt(state.currentMapId, state.playerX, state.playerY);
        if ("dungeon".equals(state.world.kind(state.currentMapId)) || terrain == 'd') {
            if (state.currentMapId.contains("blackvault")) {
                return "battle_dungeon_crypt_backdrop";
            }
            if (state.currentMapId.contains("miredepth") || state.currentMapId.contains("frosthollow")) {
                return "battle_dungeon_cavern_backdrop";
            }
            return "battle_dungeon_hall_backdrop";
        }
        return switch (terrain) {
            case 'n' -> "battle_snow_backdrop";
            case 's', 'b' -> "battle_desert_backdrop";
            case 'm', 'q' -> "battle_mountain_backdrop";
            case 'g', 'r' -> "battle_plains_backdrop";
            default -> "battle_forest_backdrop";
        };
    }

    private void drawBattleActionBar(Graphics2D g, Battle battle, int x, int y, int w, int h) {
        g.setColor(new Color(12, 14, 22, 224));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(94, 101, 126));
        g.drawRoundRect(x, y, w, h, 8, 8);
        if (battle.finished) {
            String label = battle.victory ? "Continue" : "Revive";
            Runnable action = battle.victory ? state::leaveFinishedBattle : state::revive;
            Color fill = battle.victory ? new Color(66, 93, 49) : new Color(106, 61, 61);
            actionButton(g, x + 18, y + 26, 140, 40, label, action, fill, new Color(151, 177, 112), true);
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.setColor(new Color(238, 232, 210));
            g.drawString(battle.victory ? "Victory rewards are ready." : "Return to Oakhaven and recover.", x + 180, y + 51);
            return;
        }
        Actor active = battle.activeActor();
        boolean canAct = active != null && active.alive();
        actionButton(g, x + 18, y + 14, 112, 34, "Attack", state::battleAttack, new Color(92, 79, 50), new Color(156, 132, 76), canAct);
        actionButton(g, x + 138, y + 14, 104, 34, "Potion", () -> state.useItem("potion_small"), new Color(53, 82, 70), new Color(98, 151, 117), state.player.hasItem("potion_small"));
        actionButton(g, x + 250, y + 14, 104, 34, "Ether", () -> state.useItem("ether"), new Color(54, 74, 103), new Color(104, 132, 176), state.player.hasItem("ether"));
        int abilityX = x + 372;
        List<Ability> abilities = active == null ? List.of() : active.abilities;
        for (int i = 0; i < Math.min(3, abilities.size()); i++) {
            Ability ability = abilities.get(i);
            int buttonX = abilityX + i * 138;
            boolean enabled = canAct && active.mp >= ability.cost();
            String label = (i + 1) + " " + ability.name() + " " + ability.cost() + "MP";
            int index = i;
            actionButton(g, buttonX, y + 14, 130, 34, label, () -> state.battleAbility(index), new Color(76, 53, 93), new Color(139, 107, 168), enabled);
        }
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(186, 190, 202));
        g.drawString("Keyboard: A/Space attack, 1-3 abilities, E/Tab enemy target, Q ally target, H potion, J ether.", x + 20, y + 72);
    }

    private void drawBattlePartyMember(Graphics2D g, Battle battle, Actor actor, boolean active, int panelX, int panelY, int panelW, int panelH) {
        int[] slot = battlePartyPosition(battle, actor, panelX, panelY, panelW, panelH);
        boolean selected = actor == battle.selectedPartyMember();
        int lunge = active ? battle.playerLunge : 0;
        int shake = active ? battle.playerOffset : 0;
        int bob = actor.alive() ? (int) Math.round(Math.sin((frame + battle.partyMembers().indexOf(actor) * 8) * 0.18) * 3.0) : 0;
        int drawX = slot[0] + lunge + shake;
        int drawY = slot[1] + bob;
        if (actor.alive()) {
            buttons.add(new UiButton(new Rectangle(drawX - 10, drawY - 8, 148, 234), "party-target:" + actor.name, () -> state.selectBattlePartyMember(battle.partyMembers().indexOf(actor))));
        }
        drawShadow(g, drawX + 18, drawY + 126, 86, 16);
        g.drawImage(assets.spriteFit(actor.worldSprite, 118, 148), drawX, drawY, null);
        if (selected && actor.alive()) {
            g.setColor(new Color(132, 190, 255, 190));
            g.setStroke(new BasicStroke(3f));
            g.drawOval(drawX + 4, drawY + 118, 110, 30);
        }
        if (active && actor.alive()) {
            g.setColor(new Color(246, 224, 151, 170));
            g.setStroke(new BasicStroke(2f));
            g.drawOval(drawX + 10, drawY + 122, 98, 24);
        }
        int cardX = drawX - 8;
        int cardY = drawY + 148;
        g.setColor(new Color(12, 14, 22, 205));
        g.fillRoundRect(cardX, cardY, 136, 78, 8, 8);
        g.setColor(selected ? new Color(132, 190, 255) : active ? new Color(246, 224, 151) : new Color(86, 98, 128));
        g.drawRoundRect(cardX, cardY, 136, 78, 8, 8);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(238, 239, 244));
        drawCenteredIn(g, actor.name, cardX, cardY + 20, 136);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(198, 202, 211));
        drawCenteredIn(g, actor.className, cardX, cardY + 38, 136);
        smallBar(g, cardX + 12, cardY + 46, actor.hp, actor.maxHp, new Color(190, 76, 82), "HP");
        smallBar(g, cardX + 12, cardY + 62, actor.mp, actor.maxMp, new Color(84, 129, 205), "MP");
        drawStatusIcons(g, battle, actor, cardX + 142, cardY + 8);
        if (!actor.alive()) {
            g.setColor(new Color(9, 10, 16, 150));
            g.fillRoundRect(drawX, drawY, 118, 148, 8, 8);
            g.setColor(new Color(238, 239, 244));
            drawCenteredIn(g, "Down", drawX, drawY + 86, 118);
        }
    }

    private void drawBattleEnemy(Graphics2D g, Battle battle, Actor foe, int panelX, int panelY, int panelW, int panelH) {
        int[] slot = battleEnemyPosition(battle, foe, panelX, panelY, panelW, panelH);
        int index = Math.max(0, battle.enemies().indexOf(foe));
        boolean selected = foe == battle.selectedEnemy();
        int lunge = battle.enemyLunge(foe);
        int shake = foe.alive() ? battle.monsterOffset : 0;
        int bob = foe.alive() ? (int) Math.round(Math.sin((frame + index * 10) * 0.16) * 3.0) : 0;
        int drawX = slot[0] + shake - lunge;
        int drawY = slot[1] + bob;
        if (foe.alive()) {
            buttons.add(new UiButton(new Rectangle(drawX - 12, drawY - 8, 180, 226), "enemy-target:" + foe.name, () -> state.selectBattleEnemy(index)));
        }
        float fade = Math.max(0.0f, Math.min(1.0f, battle.enemyFade(foe) / 255.0f));
        Graphics2D enemyG = (Graphics2D) g.create();
        enemyG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fade));
        drawShadow(enemyG, drawX + 18, drawY + 132, 112, 18);
        enemyG.drawImage(assets.spriteFit(foe.sprite, 152, 156), drawX, drawY, null);
        enemyG.dispose();
        if (selected && foe.alive()) {
            g.setColor(new Color(255, 220, 150, 210));
            g.setStroke(new BasicStroke(3f));
            g.drawOval(drawX + 8, drawY + 126, 134, 30);
        } else if (foe.alive() && foe == battle.livingEnemies().stream().findFirst().orElse(null)) {
            g.setColor(new Color(255, 220, 150, 180));
            g.setStroke(new BasicStroke(2f));
            g.drawOval(drawX + 12, drawY + 128, 126, 26);
        }
        int cardX = drawX - 10;
        int cardY = drawY + 156;
        g.setColor(new Color(12, 14, 22, 205));
        g.fillRoundRect(cardX, cardY, 174, 62, 8, 8);
        g.setColor(selected ? new Color(255, 220, 150) : new Color(100, 76, 76));
        g.drawRoundRect(cardX, cardY, 174, 62, 8, 8);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(new Color(238, 239, 244));
        drawCenteredIn(g, foe.name, cardX, cardY + 20, 174);
        smallBar(g, cardX + 12, cardY + 34, foe.hp, foe.maxHp, new Color(190, 76, 82), "HP");
        drawStatusIcons(g, battle, foe, cardX + 104, cardY + 34);
    }

    private void drawStatusIcons(Graphics2D g, Battle battle, Actor actor, int x, int y) {
        int offset = 0;
        for (EffectStack stack : battle.statusesFor(actor)) {
            g.setColor("buff".equals(stack.effect.affectType()) ? new Color(63, 91, 69) : new Color(95, 57, 61));
            g.fillRoundRect(x + offset, y, 28, 20, 5, 5);
            g.setColor(new Color(226, 229, 236));
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            g.drawString(StatusEffects.iconLabel(stack.effect.key()), x + offset + 5, y + 14);
            offset += 32;
            if (offset > 160) {
                break;
            }
        }
    }

    private void drawBattleFloaters(Graphics2D g, Battle battle, int panelX, int panelY) {
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        for (FloatingText floater : battle.floaters) {
            Graphics2D textG = (Graphics2D) g.create();
            textG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.05f, Math.min(1.0f, floater.life / 34.0f))));
            textG.setColor(new Color(0, 0, 0, 120));
            textG.drawString(floater.text, panelX + floater.x + 1, panelY + floater.y + 1);
            textG.setColor(floater.color);
            textG.drawString(floater.text, panelX + floater.x, panelY + floater.y);
            textG.dispose();
        }
    }

    private void drawBattleEffect(Graphics2D g, Battle battle, int panelX, int panelY, int panelW, int panelH) {
        if (battle.effectTimer <= 0 || battle.effectTarget == null) {
            return;
        }
        int[] source = battleActorCenter(battle, battle.effectSource, panelX, panelY, panelW, panelH);
        int[] target = battleActorCenter(battle, battle.effectTarget, panelX, panelY, panelW, panelH);
        float alpha = Math.max(0.08f, Math.min(0.85f, battle.effectTimer / 20.0f));
        Graphics2D fx = (Graphics2D) g.create();
        fx.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        fx.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        String kind = battle.effectKind == null ? "strike" : battle.effectKind;
        switch (kind) {
            case "heal", "regeneration" -> {
                fx.setColor(new Color(126, 232, 154));
                fx.drawOval(target[0] - 34, target[1] - 42, 68, 68);
                fx.drawOval(target[0] - 22, target[1] - 30, 44, 44);
            }
            case "shield", "ward" -> {
                fx.setColor(new Color(142, 191, 255));
                fx.drawArc(target[0] - 42, target[1] - 50, 84, 94, 205, 130);
                fx.drawArc(target[0] - 32, target[1] - 40, 64, 74, 210, 120);
            }
            case "fire", "burn" -> {
                fx.setColor(new Color(255, 132, 65));
                fx.drawLine(source[0], source[1], target[0], target[1]);
                fx.setColor(new Color(255, 206, 91));
                fx.fillOval(target[0] - 22, target[1] - 22, 44, 44);
            }
            case "frost" -> {
                fx.setColor(new Color(150, 222, 255));
                fx.drawLine(source[0], source[1], target[0], target[1]);
                fx.drawLine(target[0] - 26, target[1], target[0] + 26, target[1]);
                fx.drawLine(target[0], target[1] - 26, target[0], target[1] + 26);
            }
            case "poison", "acid", "web", "thorn" -> {
                fx.setColor(new Color(130, 220, 108));
                fx.drawLine(source[0], source[1], target[0], target[1]);
                fx.drawOval(target[0] - 28, target[1] - 20, 22, 22);
                fx.drawOval(target[0] + 6, target[1] - 32, 28, 28);
            }
            case "volley", "pierce" -> {
                fx.setColor(new Color(246, 224, 151));
                for (int i = -1; i <= 1; i++) {
                    fx.drawLine(source[0], source[1] + i * 10, target[0], target[1] + i * 8);
                }
            }
            case "slash", "cleave", "fang" -> {
                fx.setColor(new Color(255, 240, 206));
                fx.drawArc(target[0] - 48, target[1] - 38, 96, 70, 25, 120);
                fx.drawArc(target[0] - 34, target[1] - 28, 68, 52, 25, 120);
            }
            case "shadow", "sonic", "bone", "dust", "howl" -> {
                fx.setColor(new Color(180, 154, 220));
                fx.drawOval(target[0] - 36, target[1] - 30, 72, 60);
                fx.drawLine(source[0], source[1], target[0], target[1]);
            }
            default -> {
                fx.setColor(new Color(255, 240, 206));
                fx.drawLine(source[0], source[1], target[0], target[1]);
                fx.drawArc(target[0] - 38, target[1] - 28, 76, 52, 25, 120);
            }
        }
        fx.dispose();
    }

    private int[] battleActorCenter(Battle battle, Actor actor, int panelX, int panelY, int panelW, int panelH) {
        if (battle.enemies().contains(actor)) {
            int[] slot = battleEnemyPosition(battle, actor, panelX, panelY, panelW, panelH);
            return new int[]{slot[0] + 76 + battle.monsterOffset - battle.enemyLunge(actor), slot[1] + 74};
        }
        int[] slot = battlePartyPosition(battle, actor, panelX, panelY, panelW, panelH);
        int x = slot[0] + 59;
        int y = slot[1] + 74;
        if (actor == battle.activeActor()) {
            x += battle.playerLunge + battle.playerOffset;
        }
        return new int[]{x, y};
    }

    private int[] battlePartyPosition(Battle battle, Actor actor, int panelX, int panelY, int panelW, int panelH) {
        int index = Math.max(0, battle.partyMembers().indexOf(actor));
        index = Math.min(3, index);
        int count = Math.max(1, Math.min(4, battle.partyMembers().size()));
        int[][] one = {{panelX + 176, panelY + 260}};
        int[][] two = {{panelX + 222, panelY + 190}, {panelX + 92, panelY + 356}};
        int[][] three = {{panelX + 236, panelY + 170}, {panelX + 82, panelY + 290}, {panelX + 236, panelY + 410}};
        int[][] four = {{panelX + 236, panelY + 154}, {panelX + 82, panelY + 154}, {panelX + 82, panelY + 394}, {panelX + 236, panelY + 394}};
        int[][] positions = switch (count) {
            case 1 -> one;
            case 2 -> two;
            case 3 -> three;
            default -> four;
        };
        return positions[index];
    }

    private int[] battleEnemyPosition(Battle battle, Actor actor, int panelX, int panelY, int panelW, int panelH) {
        int index = Math.max(0, battle.enemies().indexOf(actor));
        index = Math.min(3, index);
        int count = Math.max(1, Math.min(4, battle.enemies().size()));
        int right = panelX + panelW;
        int[][] one = {{right - 300, panelY + 250}};
        int[][] two = {{right - 374, panelY + 180}, {right - 238, panelY + 356}};
        int[][] three = {{right - 386, panelY + 158}, {right - 226, panelY + 286}, {right - 386, panelY + 414}};
        int[][] four = {{right - 386, panelY + 150}, {right - 226, panelY + 150}, {right - 386, panelY + 390}, {right - 226, panelY + 390}};
        int[][] positions = switch (count) {
            case 1 -> one;
            case 2 -> two;
            case 3 -> three;
            default -> four;
        };
        return positions[index];
    }

    private void drawDialog(Graphics2D g) {
        Npc npc = state.activeNpc;
        if (npc == null) {
            return;
        }
        int panelX = 170;
        int panelY = 560;
        int panelW = 810;
        int panelH = 220;
        drawOverlayBase(g, panelX, panelY, panelW, panelH);
        drawNpcPortraitCard(g, npc, panelX + 26, panelY + 28, 112, 126);

        int textX = panelX + 166;
        int textW = panelW - 222;
        g.setFont(new Font("SansSerif", Font.BOLD, 25));
        g.setColor(new Color(244, 239, 220));
        g.drawString(npc.name(), textX, panelY + 54);
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.setColor(new Color(218, 220, 226));
        String line = npc.dialog().isEmpty() ? "..." : npc.dialog().get(Math.min(state.dialogIndex, npc.dialog().size() - 1));
        drawWrapped(g, line, textX, panelY + 88, textW, 25, 3);
        Quest quest = npc.questId() == null ? null : state.quests.get(npc.questId());
        int actionY = panelY + panelH - 54;
        if (quest != null) {
            g.setColor(new Color(246, 224, 151));
            String questText = quest.completed
                    ? "Quest complete"
                    : quest.accepted
                    ? quest.title + " " + quest.progress + "/" + quest.needed + " - " + quest.objectiveAction() + " " + quest.target
                    : "Quest available: " + quest.title;
            g.setFont(new Font("SansSerif", Font.BOLD, 15));
            drawWrapped(g, questText, textX, panelY + 158, textW - 170, 20, 1);
        }
        if (npc.recruitId() != null && npc.recruitCost() > 0 && !state.isRecruited(npc.recruitId())) {
            actionButton(g, panelX + panelW - 220, actionY, 140, 32, "Hire " + npc.recruitCost() + "g", state::hireActiveRecruit, new Color(69, 62, 88), new Color(125, 107, 166), true);
        } else if (npc.recruitId() != null && state.isRecruited(npc.recruitId())) {
            g.setColor(new Color(144, 215, 150));
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.drawString("Travels with you", panelX + panelW - 214, actionY + 21);
        }
        g.setColor(new Color(196, 198, 205));
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.drawString("Enter/E continues. H hires when available.", textX, actionY + 22);
        String continueLabel = activeDialogWillOpenShop(npc) ? "Open Shop" : "Continue";
        actionButton(g, panelX + panelW - 380, actionY, 140, 32, continueLabel, state::advanceDialog, new Color(58, 72, 100), new Color(107, 126, 166), true);
        actionButton(g, panelX + panelW - 62, actionY, 40, 32, "Esc", state::closeOverlay, new Color(83, 61, 61), new Color(149, 96, 88), true);
    }

    private boolean activeDialogWillOpenShop(Npc npc) {
        return state.dialogIndex >= npc.dialog().size() - 1 && state.activeShop != null;
    }

    private void drawQuestLog(Graphics2D g) {
        drawOverlayBase(g, 210, 110, 740, 650);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(244, 239, 220));
        g.drawString("Quest Log", 250, 160);
        int y = 210;
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        for (Quest quest : state.quests.values()) {
            if (!quest.accepted && !quest.completed) {
                continue;
            }
            g.setColor(quest.completed ? new Color(144, 215, 150) : new Color(246, 224, 151));
            g.setFont(new Font("SansSerif", Font.BOLD, 17));
            g.drawString(quest.title + "  " + quest.progress + "/" + quest.needed, 250, y);
            y += 24;
            g.setFont(new Font("SansSerif", Font.PLAIN, 15));
            g.setColor(new Color(210, 213, 222));
            wrap(g, quest.description, 250, y, 620, 20);
            y += 42;
            if (quest.hasWorldObjective()) {
                g.setColor(new Color(164, 211, 255));
                String objectiveLine = quest.objectiveAction() + " marked objective: " + quest.target;
                wrap(g, objectiveLine, 250, y, 620, 18);
                y += 28;
            } else {
                y += 12;
            }
        }
        if (y == 210) {
            g.setColor(new Color(210, 213, 222));
            g.drawString("No accepted quests yet.", 250, y);
        }
        g.setColor(new Color(196, 198, 205));
        g.drawString("Q or Esc close", 250, 725);
    }

    private void drawSkillTree(Graphics2D g) {
        int x = 170;
        int y = 72;
        int w = 920;
        int h = 760;
        drawOverlayBase(g, x, y, w, h);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(244, 239, 220));
        g.drawString("Skills", x + 36, y + 48);
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g.setColor(new Color(210, 213, 222));
        g.drawString(state.player.className + "  Level " + state.player.level + "  Points " + state.player.skillPoints, x + 36, y + 78);

        drawSkillColumn(g, "Common", SkillTrees.COMMON_SKILL_TREE, x + 36, y + 118, 404);
        drawSkillColumn(g, state.player.className, SkillTrees.skillTreeForClass(state.player.className), x + 480, y + 118, 404);

        int spent = SkillTrees.spent(state.player.skillAllocations);
        int respecCost = SkillTrees.respecCost(state.player.level, spent);
        boolean canRespec = spent > 0 && state.player.gold >= respecCost;
        actionButton(g, x + w - 330, y + h - 54, 154, 34, "Respec " + respecCost + "g", state::respecSkills, new Color(96, 66, 59), new Color(165, 103, 88), canRespec);
        actionButton(g, x + w - 160, y + h - 54, 120, 34, "Close", state::toggleSkills, new Color(58, 72, 100), new Color(107, 126, 166), true);
    }

    private void drawSkillColumn(Graphics2D g, String title, Map<String, SkillNode> tree, int x, int y, int w) {
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.setColor(new Color(246, 224, 151));
        g.drawString(title, x, y);
        int rowY = y + 22;
        for (SkillNode node : tree.values()) {
            drawSkillNode(g, node, x, rowY, w);
            rowY += 60;
        }
    }

    private void drawSkillNode(Graphics2D g, SkillNode node, int x, int y, int w) {
        int rank = state.player.skillRank(node.id());
        boolean learned = rank > 0;
        boolean canLearn = canShowAllocate(node);
        g.setColor(learned ? new Color(38, 50, 45, 232) : new Color(28, 32, 43, 232));
        g.fillRoundRect(x, y, w, 52, 8, 8);
        g.setColor(learned ? new Color(103, 151, 117) : canLearn ? new Color(96, 106, 133) : new Color(65, 68, 82));
        g.drawRoundRect(x, y, w, 52, 8, 8);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(235, 236, 240));
        g.drawString(node.name() + "  " + rank + "/" + node.maxRank(), x + 12, y + 19);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(new Color(176, 182, 196));
        g.drawString(shortText(node.description(), 42), x + 12, y + 38);
        actionButton(g, x + w - 74, y + 12, 58, 28, "+", () -> state.allocateSkill(node.id()), new Color(68, 90, 53), new Color(110, 139, 92), canLearn);
    }

    private boolean canShowAllocate(SkillNode node) {
        if (state.player.skillPoints <= 0 || state.player.skillRank(node.id()) >= node.maxRank()) {
            return false;
        }
        if (!SkillTrees.availableSkillTree(state.player.className).containsKey(node.id())) {
            return false;
        }
        for (String required : node.requires()) {
            if (state.player.skillRank(required) <= 0) {
                return false;
            }
        }
        return true;
    }

    private String shortText(String text, int maxChars) {
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, Math.max(0, maxChars - 3)) + "...";
    }

    private void drawInventory(Graphics2D g) {
        drawOverlayBase(g, 190, 105, 820, 650);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(244, 239, 220));
        g.drawString("Inventory", 230, 155);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(198, 202, 211));
        g.drawString("ATK " + state.player.attack + "   DEF " + state.player.defense + "   Skill Points " + state.player.skillPoints, 230, 184);
        int slotY = 225;
        for (String slot : List.of("weapon", "armor", "accessory")) {
            Equipment equipment = state.player.equippedItem(slot);
            g.setColor(new Color(28, 32, 43));
            g.fillRoundRect(230, slotY - 28, 320, 42, 8, 8);
            g.setColor(new Color(82, 92, 116));
            g.drawRoundRect(230, slotY - 28, 320, 42, 8, 8);
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.setColor(new Color(246, 224, 151));
            g.drawString(slot.toUpperCase(), 246, slotY - 3);
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g.setColor(new Color(222, 225, 232));
            g.drawString(equipment == null ? "None" : equipment.name(), 352, slotY - 3);
            if (equipment != null) {
                actionButton(g, 456, slotY - 22, 76, 30, "Unequip", () -> state.unequipSlot(slot), new Color(88, 56, 56), new Color(149, 96, 88), true);
            }
            slotY += 52;
        }

        int y = 225;
        int index = 1;
        g.setFont(new Font("SansSerif", Font.PLAIN, 17));
        for (var entry : state.player.inventory.entrySet()) {
            int buttonIndex = index - 1;
            Item item = GameData.ITEMS.get(entry.getKey());
            Equipment equipment = GameData.EQUIPMENT.get(entry.getKey());
            String action = equipment == null ? "Use" : "Equip";
            g.setColor(new Color(30, 34, 45));
            g.fillRoundRect(590, y - 28, 360, 46, 6, 6);
            g.setColor(new Color(82, 92, 116));
            g.drawRoundRect(590, y - 28, 360, 46, 6, 6);
            g.drawImage(assets.sprite(GameData.itemIcon(entry.getKey()), 34), 600, y - 22, null);
            g.setColor(new Color(222, 225, 232));
            g.drawString(index + ". " + GameData.itemName(entry.getKey()) + " x" + entry.getValue(), 642, y - 2);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.setColor(new Color(176, 182, 196));
            String detail = equipment != null ? String.join("  ", equipment.statLines()) : item == null ? "" : itemBenefit(item);
            g.drawString(detail, 642, y + 14);
            g.setFont(new Font("SansSerif", Font.PLAIN, 17));
            actionButton(g, 860, y - 20, 72, 30, action, () -> state.useInventoryItem(buttonIndex), new Color(68, 90, 53), new Color(110, 139, 92), true);
            y += 54;
            index++;
        }
        if (index == 1) {
            g.setColor(new Color(210, 213, 222));
            g.drawString("Your pack is empty.", 590, y);
        }
        g.setColor(new Color(196, 198, 205));
        g.drawString("Press item number to use/equip. I or Esc close.", 230, 714);
        actionButton(g, 846, 694, 124, 34, "Close", state::toggleInventory, new Color(83, 61, 61), new Color(149, 96, 88), true);
    }

    private void drawShop(Graphics2D g) {
        Shop shop = state.activeShop;
        if (shop == null) {
            return;
        }
        int panelX = 176;
        int panelY = 72;
        int panelW = 840;
        int panelH = 704;
        drawOverlayBase(g, panelX, panelY, panelW, panelH);
        g.setFont(new Font("SansSerif", Font.BOLD, 30));
        g.setColor(new Color(244, 239, 220));
        g.drawString(shop.name(), panelX + 44, panelY + 54);
        g.setFont(new Font("SansSerif", Font.PLAIN, 17));
        g.setColor(new Color(210, 213, 222));
        g.drawString("Gold: " + state.player.gold, panelX + 44, panelY + 88);
        if (state.activeNpc != null) {
            drawNpcPortraitCard(g, state.activeNpc, panelX + panelW - 164, panelY + 24, 116, 132);
        }

        g.setColor(new Color(96, 88, 72, 120));
        g.drawLine(panelX + 44, panelY + 122, panelX + panelW - 204, panelY + 122);

        int y = panelY + 168;
        for (int i = 0; i < shop.stock().size(); i++) {
            int buttonIndex = i;
            String itemKey = shop.stock().get(i);
            Item item = GameData.ITEMS.get(itemKey);
            Equipment equipment = GameData.EQUIPMENT.get(itemKey);
            int cost = GameData.itemCost(itemKey);
            if (cost <= 0) {
                continue;
            }
            boolean affordable = state.player.gold >= cost;
            int rowX = panelX + 42;
            int rowW = panelW - 84;
            int rowH = 64;
            drawShopRowBackground(g, rowX, y, rowW, rowH, affordable);
            g.setColor(new Color(238, 231, 207));
            g.fillRoundRect(rowX + 16, y + 10, 44, 44, 6, 6);
            g.drawImage(assets.sprite(GameData.itemIcon(itemKey), 38), rowX + 19, y + 13, null);
            g.setFont(new Font("SansSerif", Font.BOLD, 17));
            g.setColor(new Color(235, 236, 240));
            g.drawString((i + 1) + ". " + GameData.itemName(itemKey), rowX + 78, y + 25);
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g.setColor(new Color(176, 182, 196));
            g.drawString(equipment == null ? itemBenefit(item) : equipmentBenefit(equipment), rowX + 78, y + 48);
            Color buyFill = affordable ? new Color(68, 90, 53) : new Color(58, 59, 66);
            actionButton(g, rowX + rowW - 154, y + 14, 118, 36, "Buy " + cost + "g", () -> state.buyShopItem(buttonIndex), buyFill, new Color(110, 139, 92), affordable);
            y += 74;
        }
        if (state.activeNpc != null && state.activeNpc.recruitId() != null && state.activeNpc.recruitCost() > 0) {
            if (state.isRecruited(state.activeNpc.recruitId())) {
                g.setColor(new Color(144, 215, 150));
                g.setFont(new Font("SansSerif", Font.BOLD, 15));
                g.drawString(state.activeNpc.name() + " travels with you.", panelX + 44, y + 14);
            } else {
                actionButton(g, panelX + 44, y - 8, 258, 34, "Hire " + state.activeNpc.name() + " - " + state.activeNpc.recruitCost() + "g", state::hireActiveRecruit, new Color(69, 62, 88), new Color(125, 107, 166), true);
            }
        }
        g.setColor(new Color(196, 198, 205));
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString("Number keys buy matching items. H hires when available.", panelX + 44, panelY + panelH - 32);
        actionButton(g, panelX + panelW - 176, panelY + panelH - 54, 132, 36, "Leave Shop", state::closeOverlay, new Color(83, 61, 61), new Color(149, 96, 88), true);
    }

    private void drawNpcPortraitCard(Graphics2D g, Npc npc, int x, int y, int w, int h) {
        g.setPaint(new GradientPaint(x, y, new Color(31, 36, 50, 238), x, y + h, new Color(13, 16, 24, 238)));
        g.fillRoundRect(x, y, w, h, 10, 10);
        g.setColor(new Color(133, 122, 91, 170));
        g.drawRoundRect(x, y, w, h, 10, 10);
        int imageSize = Math.min(w - 22, h - 38);
        g.drawImage(assets.sprite(npc.sprite(), imageSize), x + (w - imageSize) / 2, y + 10, null);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(new Color(244, 213, 141));
        drawCenteredIn(g, npc.name(), x, y + h - 14, w);
    }

    private void drawShopRowBackground(Graphics2D g, int x, int y, int w, int h, boolean enabled) {
        Color top = enabled ? new Color(31, 36, 48, 236) : new Color(30, 31, 38, 182);
        Color bottom = enabled ? new Color(21, 25, 35, 236) : new Color(22, 23, 29, 182);
        g.setPaint(new GradientPaint(x, y, top, x + w, y + h, bottom));
        g.fillRoundRect(x, y, w, h, 10, 10);
        g.setColor(enabled ? new Color(93, 103, 130, 150) : new Color(72, 70, 80, 120));
        g.drawRoundRect(x, y, w, h, 10, 10);
        g.setColor(new Color(255, 255, 255, enabled ? 18 : 8));
        g.drawLine(x + 14, y + 1, x + w - 14, y + 1);
    }

    private String itemBenefit(Item item) {
        if (item == null) {
            return "Field provision";
        }
        if (item.heal() > 0 && item.mp() > 0) {
            return "Restores " + item.heal() + " HP and " + item.mp() + " MP";
        }
        if (item.heal() > 0) {
            return "Restores " + item.heal() + " HP";
        }
        if (item.mp() > 0) {
            return "Restores " + item.mp() + " MP";
        }
        return "Field provision";
    }

    private String equipmentBenefit(Equipment equipment) {
        String stats = String.join("  ", equipment.statLines());
        return stats.isBlank() ? equipment.description() : equipment.slot() + "   " + stats;
    }

    private void drawWorldMap(Graphics2D g) {
        int x = 170;
        int y = 64;
        int w = 920;
        int h = 760;
        drawOverlayBase(g, x, y, w, h);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(244, 239, 220));
        g.drawString("World Map", x + 36, y + 48);
        int mapX = x + 36;
        int mapY = y + 80;
        int mapW = 720;
        int mapH = 640;
        g.setColor(new Color(8, 10, 16));
        g.fillRect(mapX, mapY, mapW, mapH);
        for (int py = 0; py < mapH; py++) {
            int wy = py * WorldMap.ROWS / mapH;
            for (int px = 0; px < mapW; px++) {
                int wx = px * WorldMap.COLS / mapW;
                char tile = state.world.tileAt(WorldMap.OVERWORLD_ID, wx, wy);
                g.setColor(Terrain.color(tile));
                g.fillRect(mapX + px, mapY + py, 1, 1);
            }
        }
        drawMapMarker(g, mapX, mapY, mapW, mapH, 82, 105, "Riverside");
        drawMapMarker(g, mapX, mapY, mapW, mapH, 152, 145, "Archive");
        drawMapMarker(g, mapX, mapY, mapW, mapH, 205, 78, "Highwall");
        drawMapMarker(g, mapX, mapY, mapW, mapH, 228, 185, "Belltower");
        drawMapMarker(g, mapX, mapY, mapW, mapH, 150, 230, "Sanctum");
        drawMapMarker(g, mapX, mapY, mapW, mapH, 112, 158, "Oakhaven");
        drawMapMarker(g, mapX, mapY, mapW, mapH, 83, 62, "Snowrest");
        drawMapMarker(g, mapX, mapY, mapW, mapH, 102, 245, "Dunewick");
        drawMapMarker(g, mapX, mapY, mapW, mapH, 240, 153, "Mireford");
        for (GameState.QuestObjective objective : state.activeQuestObjectives()) {
            if (WorldMap.OVERWORLD_ID.equals(objective.mapId())) {
                drawQuestMapMarker(g, mapX, mapY, mapW, mapH, objective);
            }
        }
        if (WorldMap.OVERWORLD_ID.equals(state.currentMapId)) {
            int px = mapX + state.playerX * mapW / WorldMap.COLS;
            int py = mapY + state.playerY * mapH / WorldMap.ROWS;
            g.setColor(new Color(255, 245, 174));
            g.fillOval(px - 5, py - 5, 10, 10);
            g.setColor(Color.BLACK);
            g.drawOval(px - 5, py - 5, 10, 10);
        }
        g.setColor(new Color(81, 88, 108));
        g.drawRect(mapX, mapY, mapW, mapH);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(210, 213, 222));
        wrap(g, "Yellow marks settlements. Green and red pins mark active quest objectives.", x + 780, y + 96, 110, 20);
        sidebarButton(g, x + 780, y + h - 70, 96, 30, "Close", state::toggleWorldMap);
    }

    private void drawMapMarker(Graphics2D g, int mapX, int mapY, int mapW, int mapH, int wx, int wy, String label) {
        int px = mapX + wx * mapW / WorldMap.COLS;
        int py = mapY + wy * mapH / WorldMap.ROWS;
        g.setColor(new Color(245, 214, 117));
        g.fillRect(px - 3, py - 3, 7, 7);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.drawString(label, px + 6, py + 4);
    }

    private void drawQuestMapMarker(Graphics2D g, int mapX, int mapY, int mapW, int mapH, GameState.QuestObjective objective) {
        int px = mapX + objective.x() * mapW / WorldMap.COLS;
        int py = mapY + objective.y() * mapH / WorldMap.ROWS;
        Color color = objective.kind() == Quest.ObjectiveKind.GATHER ? new Color(112, 220, 128) : new Color(233, 89, 83);
        g.setColor(new Color(0, 0, 0, 130));
        g.fillOval(px - 6, py - 6, 13, 13);
        g.setColor(color);
        g.fillOval(px - 4, py - 4, 9, 9);
        g.setColor(new Color(245, 246, 236));
        g.drawOval(px - 7, py - 7, 15, 15);
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.drawString(objective.title(), px + 8, py + 4);
    }

    private void drawPauseMenu(Graphics2D g) {
        g.setColor(new Color(8, 11, 16, 170));
        g.fillRect(0, 0, viewWidth(), viewHeight());
        int x = 456;
        int y = 188;
        int w = 528;
        int h = 416;
        drawOverlayBase(g, x, y, w, h);
        g.setFont(new Font("Serif", Font.BOLD, 34));
        g.setColor(new Color(244, 213, 141));
        drawCenteredIn(g, "Paused", x, y + 58, w);

        int buttonX = x + 138;
        int buttonW = w - 276;
        actionButton(g, buttonX, y + 96, buttonW, 42, "Resume", state::resumeGame, new Color(66, 93, 49), new Color(126, 176, 95), true);
        actionButton(g, buttonX, y + 150, buttonW, 42, "Save / Load", () -> state.openSaveMenu(true), new Color(57, 71, 102), new Color(110, 127, 160), true);
        actionButton(g, buttonX, y + 204, buttonW, 42, "Settings", state::openSettings, new Color(74, 66, 93), new Color(124, 107, 155), true);
        actionButton(g, buttonX, y + 258, buttonW, 42, "New Adventure", state::openClassSelect, new Color(74, 67, 80), new Color(117, 107, 128), true);
        actionButton(g, buttonX, y + 312, buttonW, 42, "Main Menu", state::openMainMenu, new Color(91, 60, 60), new Color(165, 111, 98), true);
        actionButton(g, buttonX, y + 366, 120, 38, "Fullscreen", fullscreenToggle, new Color(48, 55, 70), new Color(89, 102, 125), true);
        actionButton(g, buttonX + buttonW - 120, y + 366, 120, 38, "Exit", this::exitGame, new Color(91, 60, 60), new Color(165, 111, 98), true);
    }

    private void drawSettingsMenu(Graphics2D g, boolean overlay) {
        if (overlay) {
            g.setColor(new Color(8, 11, 16, 170));
            g.fillRect(0, 0, viewWidth(), viewHeight());
        }
        int x = 314;
        int y = 82;
        int w = 812;
        int h = 704;
        drawOverlayBase(g, x, y, w, h);
        g.setFont(new Font("Serif", Font.BOLD, 32));
        g.setColor(new Color(244, 213, 141));
        drawCenteredIn(g, "Settings", x, y + 46, w);

        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.setColor(new Color(230, 225, 206));
        g.drawString("Monster Spawn Ratios", x + 52, y + 96);
        drawChanceRow(g, x + 52, y + 132, "Wild", state.config.wildEncounterChance, "wild");
        drawChanceRow(g, x + 52, y + 184, "Dungeon", state.config.dungeonEncounterChance, "dungeon");
        drawChanceRow(g, x + 52, y + 236, "Dungeon Entrance", state.config.dungeonEntranceEncounterChance, "dungeon_entrance");

        g.drawString("Monster Group Ratios", x + 52, y + 316);
        drawChanceRow(g, x + 52, y + 352, "Two Monsters", state.config.twoMonsterChance, "two_monsters");
        drawChanceRow(g, x + 52, y + 404, "Three Monsters", state.config.threeMonsterChance, "three_monsters");

        g.drawString("Audio", x + 470, y + 96);
        drawVolumeRow(g, x + 470, y + 132, "Master", state.config.masterVolume, "master");
        drawVolumeRow(g, x + 470, y + 184, "Music", state.config.musicVolume, "music");
        drawVolumeRow(g, x + 470, y + 236, "SFX", state.config.sfxVolume, "sfx");
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(165, 174, 193));
        wrap(g, "Music follows the current biome, town, dungeon, or battle and uses the master and music volume settings.", x + 470, y + 294, 260, 18);

        actionButton(g, x + w - 170, y + h - 58, 120, 34, "Back", state::closeSettings, new Color(48, 55, 70), new Color(89, 102, 125), true);
    }

    private void drawChanceRow(Graphics2D g, int x, int y, String label, double value, String key) {
        drawSettingRow(g, x, y, label, Math.round(value * 100) + "%", () -> adjustChanceSetting(key, -0.01), () -> adjustChanceSetting(key, 0.01), value);
    }

    private void drawVolumeRow(Graphics2D g, int x, int y, String label, int value, String key) {
        drawSettingRow(g, x, y, label, value + "%", () -> adjustVolumeSetting(key, -5), () -> adjustVolumeSetting(key, 5), value / 100.0);
    }

    private void drawSettingRow(Graphics2D g, int x, int y, String label, String value, Runnable decrease, Runnable increase, double fraction) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(220, 224, 232));
        g.drawString(label, x, y + 22);
        g.setColor(new Color(42, 47, 62));
        g.fillRoundRect(x + 150, y + 8, 150, 10, 5, 5);
        g.setColor(new Color(235, 211, 132));
        g.fillRoundRect(x + 150, y + 8, (int) Math.round(150 * Math.max(0.0, Math.min(1.0, fraction))), 10, 5, 5);
        g.setColor(new Color(220, 224, 232));
        g.drawString(value, x + 314, y + 22);
        actionButton(g, x + 366, y, 34, 28, "-", decrease, new Color(35, 39, 54), new Color(86, 98, 128), true);
        actionButton(g, x + 408, y, 34, 28, "+", increase, new Color(35, 39, 54), new Color(86, 98, 128), true);
    }

    private void drawSaveMenu(Graphics2D g, boolean overlay) {
        if (overlay) {
            g.setColor(new Color(8, 11, 16, 170));
            g.fillRect(0, 0, viewWidth(), viewHeight());
        }
        int x = 342;
        int y = 112;
        int w = 756;
        int h = 650;
        drawOverlayBase(g, x, y, w, h);
        g.setFont(new Font("Serif", Font.BOLD, 32));
        g.setColor(new Color(244, 213, 141));
        drawCenteredIn(g, state.saveMenuCanSave ? "Save / Load Adventure" : "Load Adventure", x, y + 48, w);

        int rowY = y + 92;
        if (state.saveMenuCanSave) {
            actionButton(g, x + 52, rowY, 260, 38, "Save Current Adventure", this::saveGame, new Color(66, 93, 49), new Color(126, 176, 95), true);
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g.setColor(new Color(198, 202, 211));
            g.drawString("Slot: " + state.player.name + " (" + state.player.className + ")", x + 332, rowY + 25);
            rowY += 66;
        }

        List<SaveSystem.SaveSummary> summaries = saves.listSaves();
        if (summaries.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 17));
            g.setColor(new Color(198, 202, 211));
            drawCenteredIn(g, "No saved adventures yet.", x, rowY + 60, w);
        } else {
            g.setFont(new Font("SansSerif", Font.BOLD, 17));
            g.setColor(new Color(230, 225, 206));
            g.drawString("Existing Adventures", x + 52, rowY + 18);
            rowY += 34;
            int index = 1;
            for (SaveSystem.SaveSummary summary : summaries.stream().limit(7).toList()) {
                int buttonIndex = index;
                String saveId = summary.saveId();
                g.setColor(new Color(28, 32, 43, 238));
                g.fillRoundRect(x + 52, rowY, w - 104, 58, 8, 8);
                g.setColor(new Color(82, 92, 116));
                g.drawRoundRect(x + 52, rowY, w - 104, 58, 8, 8);
                g.setFont(new Font("SansSerif", Font.BOLD, 16));
                g.setColor(new Color(235, 236, 240));
                g.drawString(buttonIndex + ". " + summary.name(), x + 70, rowY + 24);
                g.setFont(new Font("SansSerif", Font.PLAIN, 13));
                g.setColor(new Color(176, 182, 196));
                g.drawString(summary.className() + "  Level " + summary.level() + "  " + state.world.label(summary.mapId()), x + 70, rowY + 43);
                actionButton(g, x + w - 172, rowY + 13, 104, 32, "Load", () -> loadGame(saveId), new Color(57, 71, 102), new Color(110, 127, 160), true);
                rowY += 70;
                index++;
            }
        }
        actionButton(g, x + w - 170, y + h - 58, 120, 34, "Back", state::closeSaveMenu, new Color(48, 55, 70), new Color(89, 102, 125), true);
    }

    private void drawOverlayBase(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(new Color(9, 10, 16, 226));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(124, 112, 83));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(x, y, w, h, 8, 8);
    }

    private void bar(Graphics2D g, int x, int y, int value, int max, Color fill, String label) {
        int width = 220;
        int height = 18;
        g.setColor(new Color(40, 42, 52));
        g.fillRoundRect(x, y, width, height, 6, 6);
        int filled = max <= 0 ? 0 : (int) Math.round(width * Math.max(0.0, Math.min(1.0, value / (double) max)));
        g.setColor(fill);
        g.fillRoundRect(x, y, filled, height, 6, 6);
        g.setColor(new Color(235, 235, 235));
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString(label + " " + value + "/" + max, x + 8, y + 14);
    }

    private void smallBar(Graphics2D g, int x, int y, int value, int max, Color fill, String label) {
        int width = 80;
        int height = 14;
        g.setColor(new Color(40, 42, 52));
        g.fillRoundRect(x, y, width, height, 5, 5);
        int filled = max <= 0 ? 0 : (int) Math.round(width * Math.max(0.0, Math.min(1.0, value / (double) max)));
        g.setColor(fill);
        g.fillRoundRect(x, y, filled, height, 5, 5);
        g.setColor(new Color(235, 235, 235));
        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        g.drawString(label + " " + value + "/" + max, x + 5, y + 11);
    }

    private void wrap(Graphics2D g, String text, int x, int y, int width, int lineHeight) {
        FontMetrics metrics = g.getFontMetrics();
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (metrics.stringWidth(candidate) > width && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        for (String output : lines) {
            g.drawString(output, x, y);
            y += lineHeight;
        }
    }

    private void drawWrapped(Graphics2D g, String text, int x, int y, int width, int lineHeight, int maxLines) {
        FontMetrics metrics = g.getFontMetrics();
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (metrics.stringWidth(candidate) > width && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        int count = Math.min(lines.size(), Math.max(1, maxLines));
        for (int i = 0; i < count; i++) {
            String output = lines.get(i);
            if (i == count - 1 && lines.size() > count) {
                output = fitWithEllipsis(metrics, output, width);
            }
            g.drawString(output, x, y + i * lineHeight);
        }
    }

    private String fitWithEllipsis(FontMetrics metrics, String text, int width) {
        String output = text;
        while (metrics.stringWidth(output + "...") > width && output.length() > 1) {
            output = output.substring(0, output.length() - 1);
        }
        return output + (output.length() < text.length() ? "..." : "");
    }

    private void drawCentered(Graphics2D g, String text, int y) {
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, (viewWidth() - metrics.stringWidth(text)) / 2, y);
    }

    private void drawCenteredIn(Graphics2D g, String text, int x, int y, int width) {
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, x + (width - metrics.stringWidth(text)) / 2, y);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int tileSize() {
        return Math.max(24, GameConfig.TILE * state.zoom / 100);
    }

    private int scaled(int value) {
        return Math.max(1, value * state.zoom / 100);
    }

    private int viewWidth() {
        return GameConfig.WIDTH;
    }

    private int viewHeight() {
        return GameConfig.HEIGHT;
    }

    private void updateViewportTransform() {
        renderScaleX = getWidth() / (double) GameConfig.WIDTH;
        renderScaleY = getHeight() / (double) GameConfig.HEIGHT;
        if (!Double.isFinite(renderScaleX) || renderScaleX <= 0.0) {
            renderScaleX = 1.0;
        }
        if (!Double.isFinite(renderScaleY) || renderScaleY <= 0.0) {
            renderScaleY = 1.0;
        }
        renderOffsetX = 0;
        renderOffsetY = 0;
    }

    private Point logicalPoint(MouseEvent event) {
        updateViewportTransform();
        int scaledWidth = (int) Math.round(GameConfig.WIDTH * renderScaleX);
        int scaledHeight = (int) Math.round(GameConfig.HEIGHT * renderScaleY);
        if (event.getX() < renderOffsetX || event.getY() < renderOffsetY
                || event.getX() >= renderOffsetX + scaledWidth || event.getY() >= renderOffsetY + scaledHeight) {
            return null;
        }
        int x = clamp((int) Math.floor((event.getX() - renderOffsetX) / renderScaleX), 0, GameConfig.WIDTH - 1);
        int y = clamp((int) Math.floor((event.getY() - renderOffsetY) / renderScaleY), 0, GameConfig.HEIGHT - 1);
        return new Point(x, y);
    }

    private void clearPlayerPath() {
        playerPath.clear();
        playerPathDestination = null;
    }

    private void setPlayerPathDestination(int targetX, int targetY) {
        if (state.mode != GameMode.EXPLORE) {
            return;
        }
        int directDx = targetX - state.playerX;
        int directDy = targetY - state.playerY;
        if (Math.abs(directDx) + Math.abs(directDy) == 1
                && !state.world.isPassable(state.currentMapId, targetX, targetY)) {
            startPlayerMove(directDx, directDy);
            return;
        }
        TilePoint target = nearestPathTarget(targetX, targetY);
        if (target == null) {
            clearPlayerPath();
            state.status = "No walkable route there.";
            return;
        }
        if (target.x() == state.playerX && target.y() == state.playerY) {
            clearPlayerPath();
            state.interact();
            return;
        }
        List<TilePoint> path = findPlayerPath(new TilePoint(state.playerX, state.playerY), target);
        if (path.isEmpty()) {
            clearPlayerPath();
            state.status = "No walkable route there.";
            return;
        }
        playerPath.clear();
        playerPath.addAll(path);
        playerPathDestination = target;
        tickPlayerPath();
    }

    private TilePoint nearestPathTarget(int targetX, int targetY) {
        if (walkableForPath(targetX, targetY)) {
            return new TilePoint(targetX, targetY);
        }
        TilePoint best = null;
        int bestPlayerDistance = Integer.MAX_VALUE;
        int bestTargetDistance = Integer.MAX_VALUE;
        for (int radius = 1; radius < 10; radius++) {
            for (int y = targetY - radius; y <= targetY + radius; y++) {
                for (int x = targetX - radius; x <= targetX + radius; x++) {
                    int targetDistance = Math.abs(x - targetX) + Math.abs(y - targetY);
                    if (targetDistance != radius || !walkableForPath(x, y)) {
                        continue;
                    }
                    int playerDistance = Math.abs(x - state.playerX) + Math.abs(y - state.playerY);
                    if (playerDistance < bestPlayerDistance
                            || (playerDistance == bestPlayerDistance && targetDistance < bestTargetDistance)) {
                        best = new TilePoint(x, y);
                        bestPlayerDistance = playerDistance;
                        bestTargetDistance = targetDistance;
                    }
                }
            }
            if (best != null) {
                return best;
            }
        }
        return null;
    }

    private boolean walkableForPath(int x, int y) {
        return x >= 0
                && y >= 0
                && x < state.world.width(state.currentMapId)
                && y < state.world.height(state.currentMapId)
                && state.world.isPassable(state.currentMapId, x, y)
                && state.npcAt(state.currentMapId, x, y) == null
                && state.blockingQuestObjectiveAt(state.currentMapId, x, y) == null;
    }

    private List<TilePoint> findPlayerPath(TilePoint start, TilePoint target) {
        int width = state.world.width(state.currentMapId);
        int height = state.world.height(state.currentMapId);
        PriorityQueue<PathNode> frontier = new PriorityQueue<>(Comparator.comparingInt(PathNode::priority));
        Map<TilePoint, TilePoint> cameFrom = new java.util.HashMap<>();
        Map<TilePoint, Integer> costSoFar = new java.util.HashMap<>();
        frontier.add(new PathNode(start, 0, heuristic(start, target)));
        cameFrom.put(start, null);
        costSoFar.put(start, 0);
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        while (!frontier.isEmpty()) {
            PathNode currentNode = frontier.poll();
            TilePoint current = currentNode.point();
            if (current.equals(target)) {
                break;
            }
            Integer currentCost = costSoFar.get(current);
            if (currentCost == null || currentNode.cost() != currentCost) {
                continue;
            }
            for (int[] direction : directions) {
                int nx = current.x() + direction[0];
                int ny = current.y() + direction[1];
                if (nx < 0 || ny < 0 || nx >= width || ny >= height || !walkableForPath(nx, ny)) {
                    continue;
                }
                TilePoint next = new TilePoint(nx, ny);
                int newCost = currentCost + 1;
                if (!costSoFar.containsKey(next) || newCost < costSoFar.get(next)) {
                    costSoFar.put(next, newCost);
                    cameFrom.put(next, current);
                    frontier.add(new PathNode(next, newCost, newCost + heuristic(next, target)));
                }
            }
        }

        if (!cameFrom.containsKey(target)) {
            return List.of();
        }
        List<TilePoint> path = new ArrayList<>();
        TilePoint current = target;
        while (current != null && !current.equals(start)) {
            path.add(0, current);
            current = cameFrom.get(current);
        }
        return path;
    }

    private int heuristic(TilePoint point, TilePoint target) {
        return Math.abs(point.x() - target.x()) + Math.abs(point.y() - target.y());
    }

    private void tickPlayerPath() {
        if (state.mode != GameMode.EXPLORE) {
            clearPlayerPath();
            return;
        }
        if (playerPath.isEmpty()) {
            playerPathDestination = null;
            return;
        }
        if (playerMoving()) {
            return;
        }
        TilePoint next = playerPath.remove(0);
        int dx = next.x() - state.playerX;
        int dy = next.y() - state.playerY;
        if (Math.abs(dx) + Math.abs(dy) != 1) {
            clearPlayerPath();
            return;
        }
        startPlayerMove(dx, dy, true);
        if (state.mode != GameMode.EXPLORE || playerPathDestination == null) {
            clearPlayerPath();
        }
    }

    private void startPlayerMove(int dx, int dy) {
        startPlayerMove(dx, dy, false);
    }

    private void startPlayerMove(int dx, int dy, boolean keepPath) {
        if (!keepPath) {
            clearPlayerPath();
        }
        if (playerMoving()) {
            return;
        }
        String mapBefore = state.currentMapId;
        int fromX = state.playerX;
        int fromY = state.playerY;
        playerFacingDx = dx;
        playerFacingDy = dy;
        boolean moved = state.move(dx, dy);
        if (moved && state.currentMapId.equals(mapBefore)
                && Math.abs(state.playerX - fromX) + Math.abs(state.playerY - fromY) == 1) {
            playerMoveMapId = mapBefore;
            playerMoveFromX = fromX;
            playerMoveFromY = fromY;
            playerMoveStartFrame = frame;
        } else {
            syncPlayerAnimationToState();
            if (keepPath) {
                clearPlayerPath();
            }
        }
    }

    private void syncPlayerAnimationToState() {
        clearPlayerPath();
        playerMoveMapId = state.currentMapId;
        playerMoveFromX = state.playerX;
        playerMoveFromY = state.playerY;
        playerMoveStartFrame = frame - PLAYER_MOVE_FRAMES;
    }

    private void saveGame() {
        try {
            saves.save(state);
            state.status = "Saved " + state.player.name + ".";
        } catch (IOException ex) {
            state.status = "Save failed: " + ex.getMessage();
        }
    }

    private void loadGame() {
        try {
            if (!saves.load(state)) {
                state.status = "No Java save found.";
            } else {
                state.resetNpcRuntime();
                syncPlayerAnimationToState();
            }
        } catch (IOException ex) {
            state.status = "Load failed: " + ex.getMessage();
        }
    }

    private void loadGame(String saveId) {
        try {
            if (!saves.load(state, saveId)) {
                state.status = "No Java save found.";
            } else {
                state.resetNpcRuntime();
                syncPlayerAnimationToState();
            }
        } catch (IOException ex) {
            state.status = "Load failed: " + ex.getMessage();
        }
    }

    private void adjustChanceSetting(String key, double delta) {
        state.adjustChanceSetting(key, delta);
        saveSettings();
    }

    private void adjustVolumeSetting(String key, int delta) {
        state.adjustVolumeSetting(key, delta);
        saveSettings();
    }

    private void saveSettings() {
        try {
            state.config.save(javaRoot);
        } catch (IOException ex) {
            state.status = "Settings save failed: " + ex.getMessage();
        }
    }

    private void exitGame() {
        music.shutdown();
        java.awt.Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.dispose();
        }
        System.exit(0);
    }

    public void shutdown() {
        music.shutdown();
    }

    private record PathNode(TilePoint point, int cost, int priority) {
    }

    private final class Keys extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent event) {
            int code = event.getKeyCode();
            if (state.mode == GameMode.MAIN_MENU) {
                if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_N) {
                    state.openClassSelect();
                } else if ((code == KeyEvent.VK_L || code == KeyEvent.VK_F9) && saves.exists()) {
                    state.openSaveMenu(false);
                } else if (code == KeyEvent.VK_S) {
                    state.openSettings();
                } else if (code == KeyEvent.VK_ESCAPE) {
                    exitGame();
                }
                repaint();
                return;
            }
            if (state.mode == GameMode.CLASS_SELECT) {
                if (code == KeyEvent.VK_1) {
                    state.chooseClass("Knight");
                    syncPlayerAnimationToState();
                } else if (code == KeyEvent.VK_2) {
                    state.chooseClass("Mage");
                    syncPlayerAnimationToState();
                } else if (code == KeyEvent.VK_3) {
                    state.chooseClass("Ranger");
                    syncPlayerAnimationToState();
                } else if (code == KeyEvent.VK_F9) {
                    loadGame();
                } else if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_BACK_SPACE) {
                    if (state.pendingPlayerName.length() > 0 && code == KeyEvent.VK_BACK_SPACE) {
                        state.setPendingPlayerName(state.pendingPlayerName.substring(0, state.pendingPlayerName.length() - 1));
                    } else {
                        state.openMainMenu();
                    }
                } else if (code == KeyEvent.VK_L && saves.exists()) {
                    state.openSaveMenu(false);
                } else {
                    char ch = event.getKeyChar();
                    if ((Character.isLetterOrDigit(ch) || ch == ' ' || ch == '-' || ch == '_') && state.pendingPlayerName.length() < 24) {
                        state.setPendingPlayerName(state.pendingPlayerName + ch);
                    }
                }
                repaint();
                return;
            }
            if (state.mode == GameMode.PAUSE_MENU) {
                if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_ENTER) {
                    state.resumeGame();
                } else if (code == KeyEvent.VK_F5 || code == KeyEvent.VK_S) {
                    state.openSaveMenu(true);
                } else if ((code == KeyEvent.VK_F9 || code == KeyEvent.VK_L) && saves.exists()) {
                    state.openSaveMenu(true);
                } else if (code == KeyEvent.VK_T) {
                    state.openSettings();
                } else if (code == KeyEvent.VK_N) {
                    state.openClassSelect();
                } else if (code == KeyEvent.VK_M) {
                    state.openMainMenu();
                }
                repaint();
                return;
            }
            if (state.mode == GameMode.SETTINGS) {
                if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_BACK_SPACE) {
                    state.closeSettings();
                }
                repaint();
                return;
            }
            if (state.mode == GameMode.SAVE_MENU) {
                if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_BACK_SPACE) {
                    state.closeSaveMenu();
                } else if ((code == KeyEvent.VK_S || code == KeyEvent.VK_F5) && state.saveMenuCanSave) {
                    saveGame();
                } else if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_7) {
                    int index = code - KeyEvent.VK_1;
                    List<SaveSystem.SaveSummary> summaries = saves.listSaves();
                    if (index < summaries.size()) {
                        loadGame(summaries.get(index).saveId());
                    }
                }
                repaint();
                return;
            }

            if (code == KeyEvent.VK_F5) {
                saveGame();
            } else if (code == KeyEvent.VK_F9) {
                loadGame();
            } else if (code == KeyEvent.VK_ESCAPE) {
                if (state.mode == GameMode.EXPLORE || state.mode == GameMode.BATTLE || state.mode == GameMode.DIALOG) {
                    state.openPauseMenu();
                } else if (state.mode != GameMode.EXPLORE && state.mode != GameMode.BATTLE) {
                    state.closeOverlay();
                }
            } else if (code == KeyEvent.VK_P) {
                state.openPauseMenu();
            } else if (state.mode == GameMode.BATTLE) {
                handleBattleKey(event);
            } else if (state.mode == GameMode.DIALOG) {
                if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_E) {
                    state.advanceDialog();
                } else if (code == KeyEvent.VK_H) {
                    state.hireActiveRecruit();
                }
            } else if (state.mode == GameMode.QUEST_LOG) {
                if (code == KeyEvent.VK_Q) {
                    state.toggleQuestLog();
                }
            } else if (state.mode == GameMode.SKILLS) {
                if (code == KeyEvent.VK_K || code == KeyEvent.VK_Q) {
                    state.toggleSkills();
                }
            } else if (state.mode == GameMode.WORLD_MAP) {
                if (code == KeyEvent.VK_M || code == KeyEvent.VK_Q) {
                    state.toggleWorldMap();
                }
            } else if (state.mode == GameMode.INVENTORY) {
                if (code == KeyEvent.VK_I) {
                    state.toggleInventory();
                } else if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
                    state.useInventoryItem(code - KeyEvent.VK_1);
                }
            } else if (state.mode == GameMode.SHOP) {
                if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
                    state.buyShopItem(code - KeyEvent.VK_1);
                } else if (code == KeyEvent.VK_H) {
                    state.hireActiveRecruit();
                }
            } else if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP) {
                startPlayerMove(0, -1);
            } else if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) {
                startPlayerMove(0, 1);
            } else if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT) {
                startPlayerMove(-1, 0);
            } else if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) {
                startPlayerMove(1, 0);
            } else if (code == KeyEvent.VK_E) {
                state.interact();
            } else if (code == KeyEvent.VK_Q) {
                state.toggleQuestLog();
            } else if (code == KeyEvent.VK_M) {
                state.toggleWorldMap();
            } else if (code == KeyEvent.VK_K) {
                state.toggleSkills();
            } else if (code == KeyEvent.VK_I) {
                state.toggleInventory();
            } else if (code == KeyEvent.VK_H) {
                state.useItem("potion_small");
            } else if (code == KeyEvent.VK_J) {
                state.useItem("ether");
            }
            repaint();
        }

        private void handleBattleKey(KeyEvent event) {
            int code = event.getKeyCode();
            if (code == KeyEvent.VK_A || code == KeyEvent.VK_SPACE) {
                state.battleAttack();
            } else if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_3) {
                state.battleAbility(code - KeyEvent.VK_1);
            } else if (code == KeyEvent.VK_TAB) {
                if (event.isShiftDown()) {
                    state.cycleBattlePartyTarget(1);
                } else {
                    state.cycleBattleEnemyTarget(1);
                }
            } else if (code == KeyEvent.VK_E) {
                state.cycleBattleEnemyTarget(1);
            } else if (code == KeyEvent.VK_Q) {
                state.cycleBattlePartyTarget(1);
            } else if (code == KeyEvent.VK_ENTER) {
                state.leaveFinishedBattle();
            } else if (code == KeyEvent.VK_R && state.battle != null && state.battle.finished && !state.battle.victory) {
                state.revive();
            } else if (code == KeyEvent.VK_H) {
                state.useItem("potion_small");
            } else if (code == KeyEvent.VK_J) {
                state.useItem("ether");
            }
        }

    }

    private final class Mouse extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent event) {
            requestFocusInWindow();
            Point point = logicalPoint(event);
            if (point == null) {
                return;
            }
            for (int i = buttons.size() - 1; i >= 0; i--) {
                UiButton button = buttons.get(i);
                if (button.contains(point.x, point.y)) {
                    button.action().run();
                    return;
                }
            }
            if (state.mode == GameMode.EXPLORE && point.x < GameConfig.MAP_COLS * GameConfig.TILE && point.y < viewHeight()) {
                clickWorld(point.x, point.y);
            }
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent event) {
            if (state.mode != GameMode.MAIN_MENU && state.mode != GameMode.CLASS_SELECT && state.mode != GameMode.PAUSE_MENU) {
                state.adjustZoom(event.getWheelRotation() < 0 ? 10 : -10);
                repaint();
            }
        }

        private void clickWorld(int x, int y) {
            int tileSize = tileSize();
            int targetX = lastCamX + x / tileSize;
            int targetY = lastCamY + y / tileSize;
            setPlayerPathDestination(targetX, targetY);
            repaint();
        }
    }
}
