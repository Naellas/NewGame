package com.alderfall.game;

import com.alderfall.game.map.WorldTransition;
import com.alderfall.game.camera.CameraController;
import com.alderfall.game.camera.CameraMode;
import com.alderfall.game.camera.CameraView;
import com.alderfall.game.inventory.Equipment;
import com.alderfall.game.inventory.InventoryDrag;
import com.alderfall.game.inventory.InventoryDragZone;
import com.alderfall.game.inventory.InventoryDropKind;
import com.alderfall.game.inventory.InventoryDropZone;
import com.alderfall.game.inventory.Item;
import com.alderfall.game.inventory.ItemRarity;
import com.alderfall.game.map.WorldMap;
import com.alderfall.game.map.WorldMapOverlayRenderer;
import com.alderfall.game.map.WorldMapViewport;
import com.alderfall.game.render.WorldMapTerrainCache;
import java.awt.BasicStroke;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Point;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.TexturePaint;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public final class GamePanel extends JPanel {
    private static final String[][] ATTRIBUTE_STATS = {
            {"STR", "strength"},
            {"INT", "intelligence"},
            {"DEX", "dexterity"},
            {"CHA", "charisma"},
            {"CON", "constitution"},
            {"WIL", "willpower"}
    };
    private static final int PLAYER_MOVE_FRAMES = 6;
    private static final double ROAD_MOVE_FRAME_MULTIPLIER = 0.68;
    private static final int MIN_ROAD_MOVE_FRAMES = 3;
    private static final int WALK_CYCLE_FRAMES_PER_TILE = 4;
    private static final int WALK_ANIMATION_FRAMES = 18;
    private static final int CLOUD_LAYER_COUNT = 96;
    private static final double WEATHER_VISIBILITY_EPSILON = 0.01;
    private static final Color DEFAULT_SHADOW_LIGHT = new Color(255, 205, 130);
    private static final Color BIOME_TINT_TUNDRA = new Color(212, 235, 255);
    private static final Color BIOME_TINT_DESERT = new Color(255, 215, 145);
    private static final Color BIOME_TINT_MARSH = new Color(92, 150, 128);
    private static final Color BIOME_TINT_BADLANDS = new Color(190, 123, 92);
    private static final Color BIOME_TINT_FOREST = new Color(45, 93, 66);
    private static final Color BIOME_TINT_MOUNTAIN = new Color(185, 196, 205);
    private static final Color BIOME_TINT_WATER = new Color(70, 165, 215);
    private static final Color BIOME_TINT_ROAD = new Color(230, 196, 130);
    private static final Color BIOME_TINT_GRASS = new Color(176, 205, 127);
    private static final float[] RADIAL_GLOW_FRACTIONS = {0.0f, 0.42f, 1.0f};
    private static final int MAX_ACTIVE_WORLD_LIGHTS = 48;
    private static final int MAX_BATTLE_PARTICLES = 22;
    private static final int MAX_STATUS_LOG_ENTRIES = 100;
    private static final int[][] MINIMAP_COVERAGE = {
            {15, 9},
            {21, 13},
            {31, 17},
            {43, 23},
            {57, 31}
    };
    private static final double WORLD_MAP_MIN_ZOOM = 1.0;
    private static final double WORLD_MAP_MAX_ZOOM = 6.0;
    private static final double WORLD_MAP_ZOOM_STEP = 1.25;
    private static final int[] CLOUD_SEEDS = createCloudSeeds();
    private static final DateTimeFormatter SAVE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());
    final GameState state;
    private final AssetStore assets;
    private final BattleRenderer battleRenderer;
    final SaveSystem saves;
    private final GameAudioController audio;
    private final WeatherSystem weather;
    private final WorldRenderer worldRenderer;

    private final CameraController cameraController = new CameraController();
    private final RenderMetrics renderMetrics = RenderMetrics.create();
    private final Map<String, BufferedImage> particleSprites = new HashMap<>();
    private final Path javaRoot;
    private final Timer timer;
    private final List<UiButton> buttons = new ArrayList<>();
    private final List<UiSlider> sliders = new ArrayList<>();
    private final List<TooltipZone> tooltipZones = new ArrayList<>();
    private boolean uiHidden;
    boolean battleVfxDebug;
    private int frame;
    private int lastCamX;
    private int lastCamY;
    private double lastCameraX;
    private double lastCameraY;
    private int lastVisibleCols;
    private int lastVisibleRows;
    private final RenderBackBuffer backBuffer = RenderBackBuffer.create();
    private BufferedImage fogBuffer;
    private final WorldMapTerrainCache worldMapTerrainCache = new WorldMapTerrainCache();
    private Rectangle worldMapBounds = new Rectangle();
    private WorldMapViewport worldMapViewport = WorldMapViewport.full();
    double worldMapZoom = WORLD_MAP_MIN_ZOOM;
    private double worldMapCenterX = WorldMap.START_POSITION.x() + 0.5;
    private double worldMapCenterY = WorldMap.START_POSITION.y() + 0.5;
    private int worldMapQuestFocusIndex;
    private Point worldMapDragStart;
    private double worldMapDragStartCenterX;
    private double worldMapDragStartCenterY;
    private boolean worldMapDragged;
    private final WeatherLayerCacheStore weatherLayerCaches = new WeatherLayerCacheStore();
    private final WaterTileRenderer waterTileRenderer = new WaterTileRenderer();
    private double renderScaleX = 1.0;
    private double renderScaleY = 1.0;
    private int renderWidth = GameConfig.WIDTH;
    private int renderOffsetX;
    private int renderOffsetY;
    private String playerMoveMapId = WorldMap.OVERWORLD_ID;
    private int playerMoveStartFrame = -PLAYER_MOVE_FRAMES;
    private int playerMoveDurationFrames = PLAYER_MOVE_FRAMES;
    private int playerMoveFromX = WorldMap.START_POSITION.x();
    private int playerMoveFromY = WorldMap.START_POSITION.y();
    private String followerTrailMapId = WorldMap.OVERWORLD_ID;
    private int playerWalkAnimationTileStart;
    private int playerWalkAnimationTiles;
    int playerFacingDx;
    int playerFacingDy = 1;
    private int queuedMoveDx;
    private int queuedMoveDy;
    private boolean moveUpHeld;
    private boolean moveDownHeld;
    private boolean moveLeftHeld;
    private boolean moveRightHeld;
    private int lastHeldMoveDx;
    private int lastHeldMoveDy;
    private final List<TilePoint> playerPath = new ArrayList<>();
    private int playerPathIndex;
    private final List<FollowerTrailStep> playerTrail = new ArrayList<>();
    private final Map<String, FollowerVisualState> followerVisuals = new HashMap<>();
    private final List<PartyFollowerRender> visiblePartyFollowers = new ArrayList<>();
    private final List<WorldLight> activeWorldLights = new ArrayList<>();
    private final List<WorldProp> nearbyWorldProps = new ArrayList<>();
    private final List<WorldProp> visibleWorldProps = new ArrayList<>();
    private final Map<String, BufferedImage> shadowMaskCache = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> eldest) {
            return size() > 96;
        }
    };
    private double shadowWorldOffsetX;
    private double shadowWorldOffsetY;
    private TilePoint playerPathDestination;
    private Runnable fullscreenToggle = () -> {
    };
    private Point hoverPoint;
    private TilePoint contextMenuTile;
    private WorldMap.SettlementSite contextMenuSettlement;
    private CityBuilding contextMenuBuilding;
    private Actor contextMenuPartyMember;
    private Point contextMenuPoint;
    private TilePoint pendingGatherTile;
    private TilePoint pendingReadTile;
    private TilePoint pendingCraftTile;
    private Npc pendingTalkNpc;
    private Actor pendingTalkPartyMember;
    private TilePoint pendingTalkPartyTile;
    private CityBuilding pendingEnterBuilding;
    private TilePoint pendingPortalTile;
    int inventoryItemScroll;
    int craftingRecipeScroll;
    private int craftingRecipeCategoryIndex;
    int shopItemScroll;
    int saveListScroll;
    private int villageListScroll;
    private int battleLogScroll;
    private Rectangle battleLogBounds = new Rectangle();
    private final List<String> statusLog = new ArrayList<>();
    private int statusLogScroll;
    private Rectangle statusLogBounds = new Rectangle();
    int dialogOptionScroll;
    private Rectangle dialogOptionScrollBounds = new Rectangle();
    private String dialogOptionScrollKey = "";
    boolean questLogCompletedTab;
    int questLogScroll;
    String selectedQuestLogId = "";
    private String trackedQuestId = "";
    private boolean questDetailTimelineTab;
    private String lastLoggedStatus = "";
    private int minimapZoomIndex = 1;
    private int lastVillageTab = -1;
    private String lastVillagePropCategory = "";
    private String lastInteriorAssetCategory = "";
    private String villageBuildSearch = "";
    private String lastVillageBuildSearch = "";
    boolean villageSearchFocused;
    private Rectangle villageSearchBounds = new Rectangle();
    boolean saveListCurrentCharacterOnly = true;
    private String pendingOverwriteSaveId = "";
    private String pendingOverwriteSaveName = "";
    String selectedLoadCharacterId = "";
    private boolean importingCharacter;
    private final List<InventoryDragZone> inventoryDragZones = new ArrayList<>();
    private final List<InventoryDropZone> inventoryDropZones = new ArrayList<>();
    private final List<AbilityDragZone> abilityDragZones = new ArrayList<>();
    private final List<AbilityDropZone> abilityDropZones = new ArrayList<>();
    private final List<PartyPortraitZone> partyPortraitZones = new ArrayList<>();
    private InventoryDrag inventoryDrag;
    private Point inventoryDragStart;
    private Point inventoryDragPoint;
    private boolean inventoryDragMoved;
    private AbilityDrag abilityDrag;
    private Point abilityDragStart;
    private Point abilityDragPoint;
    private boolean abilityDragMoved;
    private boolean suppressNextClick;
    private Rectangle pressedButtonBounds;
    private String pressedButtonLabel;
    private UiSlider activeSlider;
    int partySkillScroll;
    int partyLoadoutScroll;
    Rectangle partyLoadoutBounds = new Rectangle();
    int skillTreeScroll;
    SkillTab activeSkillTab = SkillTab.SURVIVAL;
    private String activeProfessionId = Profession.WOODCUTTING.id();
    private final List<PlacementPulse> placementPulses = new ArrayList<>();

    private static int[] createCloudSeeds() {
        int[] seeds = new int[CLOUD_LAYER_COUNT];
        for (int i = 0; i < seeds.length; i++) {
            seeds[i] = Math.abs(i * 734287 + 19349663);
        }
        return seeds;
    }

    private enum SliderKind {
        CHANCE,
        DIFFICULTY,
        VOLUME,
        CAMERA
    }

    private enum MovementSpeed {
        RELAXED("relaxed", "Relaxed", 8),
        NORMAL("normal", "Normal", PLAYER_MOVE_FRAMES),
        QUICK("quick", "Quick", 5);

        private final String key;
        private final String label;
        private final int frames;

        MovementSpeed(String key, String label, int frames) {
            this.key = key;
            this.label = label;
            this.frames = frames;
        }
    }

    enum SkillTab {
        SURVIVAL("Survival"),
        CLASS("Class"),
        PROFESSIONS("Professions"),
        LOADOUT("Loadout");

        private final String label;

        SkillTab(String label) {
            this.label = label;
        }
    }

    private record UiSlider(Rectangle bounds, Rectangle track, SliderKind kind, String key) {
        boolean contains(int x, int y) {
            return bounds.contains(x, y);
        }
    }

    private record AbilityDrag(String abilityName, int sourceSlot) {
    }

    private record AbilityDragZone(Rectangle bounds, String abilityName, int sourceSlot) {
    }

    private record AbilityDropZone(Rectangle bounds, int slot, boolean remove) {
    }

    private record PartyPortraitZone(Rectangle bounds, Actor actor) {
    }

    private record BuildingVisualLayout(
            Rectangle bounds,
            int drawX,
            int drawY,
            int drawW,
            int drawH,
            int lotX,
            int lotW,
            int frontY,
            TilePoint door
    ) {
    }

    private record FollowerTrailStep(
            String mapId,
            int x,
            int y,
            int fromX,
            int fromY,
            int startFrame,
            int durationFrames,
            int facingDx,
            int facingDy
    ) {
    }

    private record PartyFollowerRender(
            Actor actor,
            TilePoint tile,
            Rectangle screenBounds
    ) {
    }

    private static final class FollowerVisualState {
        private String mapId;
        private double fromX;
        private double fromY;
        private int targetX;
        private int targetY;
        private int startFrame;
        private int durationFrames;
        private int facingDx;
        private int facingDy = 1;
        private int walkTileStart;

        private FollowerVisualState(String mapId, int x, int y, int facingDx, int facingDy, int frame, int durationFrames) {
            this.mapId = mapId;
            this.fromX = x;
            this.fromY = y;
            this.targetX = x;
            this.targetY = y;
            this.startFrame = frame - Math.max(1, durationFrames);
            this.durationFrames = Math.max(1, durationFrames);
            if (Math.abs(facingDx) + Math.abs(facingDy) == 1) {
                this.facingDx = facingDx;
                this.facingDy = facingDy;
            }
        }
    }

    static final class LoadFolder {
        final String characterId;
        private final String name;
        private final String className;
        private final long latestSavedAtMillis;
        private int saveCount;
        private int maxLevel;

        private LoadFolder(SaveSystem.SaveSummary summary) {
            this.characterId = summary.characterId();
            this.name = summary.name();
            this.className = summary.className();
            this.latestSavedAtMillis = summary.savedAtMillis();
        }

        private void include(SaveSystem.SaveSummary summary) {
            saveCount++;
            maxLevel = Math.max(maxLevel, summary.level());
        }
    }

    public GamePanel(Path javaRoot) {
        this.javaRoot = javaRoot;
        this.state = new GameState(GameConfig.loadWithSettings(javaRoot));
        this.assets = new AssetStore(javaRoot.resolve("assets"));
        this.battleRenderer = new BattleRenderer(assets);
        this.saves = new SaveSystem(javaRoot);
        MusicManager music = new MusicManager(javaRoot.resolve("assets").resolve("music"));
        SoundManager sounds = new SoundManager(javaRoot.resolve("assets").resolve("sfx"));
        this.audio = new GameAudioController(state, music, sounds);
        this.weather = new WeatherSystem(state);
        this.worldRenderer = new WorldRenderer(assets, new WorldRenderer.TerrainPainter() {
            @Override
            public char visibleTerrainTile(char tile, int wx, int wy) {
                return GamePanel.this.visibleTerrainTile(tile, wx, wy);
            }

            @Override
            public String terrainImageName(char terrainTile, int wx, int wy) {
                return GamePanel.this.terrainImageName(terrainTile, wx, wy);
            }

            @Override
            public void drawTerrainEdges(Graphics2D g, char terrainTile, int wx, int wy, int px, int py) {
                GamePanel.this.drawTerrainEdges(g, terrainTile, wx, wy, px, py);
            }

            @Override
            public void drawWaterAnimation(Graphics2D g, int wx, int wy, int px, int py, int tileSize) {
                GamePanel.this.drawWaterAnimation(g, wx, wy, px, py, tileSize);
            }

            @Override
            public void drawHouseTile(Graphics2D g, char tile, int wx, int wy, int px, int py) {
                GamePanel.this.drawHouseTile(g, tile, wx, wy, px, py);
            }
        }, GamePanel.this::drawWorldProp, new WorldRenderer.LightingPainter() {
            @Override
            public void drawBiomeLightTints(Graphics2D g, int camX, int camY, int visibleCols, int visibleRows, int tileSize) {
                GamePanel.this.drawBiomeLightTints(g, camX, camY, visibleCols, visibleRows, tileSize);
            }

            @Override
            public void drawLightTileSpill(Graphics2D g, int camX, int camY, int visibleCols, int visibleRows, int tileSize) {
                GamePanel.this.drawLightTileSpill(g, camX, camY, visibleCols, visibleRows, tileSize);
            }

            @Override
            public void drawWorldVignette(Graphics2D g) {
                GamePanel.this.drawWorldVignette(g);
            }
        });
        setPreferredSize(new Dimension(GameConfig.WIDTH, GameConfig.HEIGHT));
        setBackground(new Color(15, 17, 24));
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        addKeyListener(new GameKeyboardController(this));
        Mouse mouse = new Mouse();
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);
        timer = new Timer(GameConfig.FPS_MS, event -> {
            frame++;
            state.tickWorld();
            weather.tickTransition();
            tickPlayerPath();
            if (state.mode == GameMode.BATTLE && state.battle != null) {
                state.tickBattle();
            }
            audio.update(frame);
            syncStatusLog();
            repaint();
        });
        timer.start();
        syncStatusLog();
    }

    private void syncStatusLog() {
        String status = state.status == null ? "" : state.status.trim();
        if (status.isBlank() || status.equals(lastLoggedStatus)) {
            return;
        }
        statusLog.add(TravelLogNarrator.narrate(state, status));
        while (statusLog.size() > MAX_STATUS_LOG_ENTRIES) {
            statusLog.remove(0);
        }
        lastLoggedStatus = status;
        statusLogScroll = 0;
    }

    public void setFullscreenToggle(Runnable fullscreenToggle) {
        this.fullscreenToggle = fullscreenToggle == null ? () -> {
        } : fullscreenToggle;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        updateViewportTransform();

        buttons.clear();
        sliders.clear();
        tooltipZones.clear();
        abilityDragZones.clear();
        abilityDropZones.clear();
        partyPortraitZones.clear();
        int renderAttempts = 0;
        do {
            Graphics2D bufferGraphics = backBuffer.createGraphics(this, viewWidth(), viewHeight());
            bufferGraphics.setColor(getBackground());
            bufferGraphics.fillRect(0, 0, viewWidth(), viewHeight());
            long renderStarted = renderMetrics.start();
            renderGame(bufferGraphics);
            renderMetrics.record("renderGame", renderStarted);
            bufferGraphics.dispose();
            renderAttempts++;
        } while (backBuffer.contentsLost() && renderAttempts < 2);

        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        int scaledWidth = (int) Math.round(viewWidth() * renderScaleX);
        int scaledHeight = (int) Math.round(viewHeight() * renderScaleY);
        backBuffer.drawTo(g, renderOffsetX, renderOffsetY, scaledWidth, scaledHeight);
        g.dispose();
        renderMetrics.endFrame(frame);
    }

    private void renderGame(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        if (state.mode == GameMode.MAIN_MENU) {
            drawMainMenu(g);
        } else if (state.mode == GameMode.CLASS_SELECT) {
            drawClassSelect(g);
        } else if (state.mode == GameMode.STORY_INTRO) {
            drawStoryIntro(g);
        } else if (state.mode == GameMode.SETTINGS && state.settingsReturnMode == GameMode.MAIN_MENU) {
            drawTitleBackground(g, false);
            drawSettingsMenu(g, false);
        } else if (state.mode == GameMode.SAVE_MENU && state.saveMenuReturnMode == GameMode.MAIN_MENU) {
            drawTitleBackground(g, false);
            drawSaveMenu(g, false);
        } else {
            GameMode visibleMode = visibleGameplayMode();
            if (visibleMode == GameMode.BATTLE) {
                if (!uiHidden) {
                    drawSidebar(g);
                }
                drawBattle(g);
            } else {
                drawWorld(g);
                drawInteriorPlacementPreview(g);
                if (!uiHidden) {
                    drawSidebar(g);
                }
                if (!uiHidden && visibleMode == GameMode.EXPLORE) {
                    drawTrackedQuestHud(g);
                }
                if (!uiHidden && visibleMode == GameMode.DIALOG) {
                    drawDialog(g);
                } else if (!uiHidden && visibleMode == GameMode.QUEST_LOG) {
                    drawQuestLog(g);
                } else if (!uiHidden && visibleMode == GameMode.SKILLS) {
                    drawPartyOverview(g);
                } else if (!uiHidden && visibleMode == GameMode.INVENTORY) {
                    drawInventory(g);
                } else if (!uiHidden && visibleMode == GameMode.CRAFTING) {
                    drawCrafting(g);
                } else if (!uiHidden && visibleMode == GameMode.PARTY) {
                    drawPartyOverview(g);
                } else if (!uiHidden && visibleMode == GameMode.VILLAGE) {
                    // Village controls live in the standard right sidebar.
                } else if (!uiHidden && visibleMode == GameMode.BUILDING_ASSIGNMENT) {
                    drawBuildingAssignment(g);
                } else if (!uiHidden && visibleMode == GameMode.SETTLEMENT_BOARD) {
                    drawSettlementBoard(g);
                } else if (!uiHidden && visibleMode == GameMode.FAST_TRAVEL) {
                    drawFastTravel(g);
                } else if (!uiHidden && visibleMode == GameMode.SHOP) {
                    drawShop(g);
                } else if (!uiHidden && visibleMode == GameMode.WORLD_MAP) {
                    drawWorldMap(g);
                }
            }
            if (state.mode == GameMode.PAUSE_MENU) {
                drawPauseMenu(g);
            } else if (state.mode == GameMode.SETTINGS) {
                drawSettingsMenu(g, true);
            } else if (state.mode == GameMode.SAVE_MENU) {
                drawSaveMenu(g, true);
            }
        }
        if (!uiHidden) {
            drawWorldContextMenu(g);
        }
        if (drawsGameplayUiToggle()) {
            drawUiVisibilityToggle(g);
        }
        if (!uiHidden && visibleGameplayMode() != GameMode.DIALOG) {
            drawHoverTooltip(g);
        }
        if (state.dialogueVideoActive()) {
            drawDialogueVideoOverlay(g);
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

    private void drawTitleBackground(Graphics2D g, boolean showCast) {
        g.drawImage(assets.cover("battle_forest_backdrop", viewWidth(), viewHeight()), 0, 0, null);
        g.setPaint(new GradientPaint(0, 0, new Color(7, 12, 24, 60), 0, viewHeight(), new Color(5, 8, 13, 238)));
        g.fillRect(0, 0, viewWidth(), viewHeight());
        g.setPaint(new RadialGradientPaint(
                new Point(viewWidth() / 2, 224),
                Math.max(420, viewWidth() / 3),
                new float[]{0.0f, 0.55f, 1.0f},
                new Color[]{new Color(255, 217, 139, 58), new Color(55, 87, 103, 34), new Color(4, 7, 12, 0)}
        ));
        g.fillRect(0, 0, viewWidth(), viewHeight());
        g.setPaint(null);
        g.setColor(new Color(68, 83, 104, 105));
        for (int i = 0; i < 18; i++) {
            int x = (i * 89 + frame * 2) % (viewWidth() + 160) - 80;
            int y = 82 + (i * 37) % 600;
            g.drawLine(x, y, x + 48, y - 5);
        }
        g.setPaint(new GradientPaint(0, viewHeight() - 340, new Color(3, 6, 10, 0), 0, viewHeight(), new Color(2, 4, 7, 220)));
        g.fillRect(0, viewHeight() - 340, viewWidth(), 340);
        g.setPaint(null);
        if (showCast) {
            drawTitleCast(g);
        }
    }

    private void drawTitleCast(Graphics2D g) {
        int center = viewWidth() / 2;
        int groundY = viewHeight() - 174;
        drawTitleCharacter(g, "class_knight_model", center - 812, groundY - 272, 206, 292, 0.98f);
        drawTitleCharacter(g, "class_cleric_model", center - 642, groundY - 236, 164, 238, 0.92f);
        drawTitleCharacter(g, "class_rogue_model", center + 476, groundY - 232, 158, 232, 0.9f);
        drawTitleCharacter(g, "class_ranger_model", center + 616, groundY - 250, 176, 250, 0.94f);
        drawTitleCharacter(g, "class_mage_model", center + 768, groundY - 292, 218, 292, 1.0f);
    }

    private void drawTitleCharacter(Graphics2D g, String sprite, int x, int y, int w, int h, float alpha) {
        Graphics2D cast = (Graphics2D) g.create();
        cast.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        Composite oldComposite = cast.getComposite();
        cast.setComposite(AlphaComposite.SrcOver.derive(0.34f * alpha));
        cast.setColor(new Color(0, 0, 0));
        cast.fillOval(x + w / 8, y + h - 20, w * 3 / 4, 32);
        cast.setComposite(AlphaComposite.SrcOver.derive(alpha));
        cast.drawImage(assets.spriteFit(sprite, w, h), x, y, null);
        cast.setComposite(AlphaComposite.SrcOver.derive(0.18f * alpha));
        cast.setColor(new Color(255, 229, 153));
        cast.drawLine(x + w / 3, y + h - 8, x + w * 2 / 3, y + h - 8);
        cast.setComposite(oldComposite);
        cast.dispose();
    }

    private void drawMainMenu(Graphics2D g) {
        drawTitleBackground(g, true);
        int menuW = 340;
        int menuX = centeredX(menuW);
        int menuY = 286;

        g.setColor(new Color(4, 7, 12, 96));
        g.fillRoundRect(menuX - 28, menuY - 26, menuW + 56, 358, 10, 10);
        g.setColor(new Color(212, 184, 113, 76));
        g.drawRoundRect(menuX - 28, menuY - 26, menuW + 56, 358, 10, 10);

        g.setFont(new Font("Serif", Font.BOLD, 60));
        g.setColor(new Color(3, 5, 10, 190));
        drawCentered(g, "Echoes of Alderfall", 129);
        g.setColor(new Color(244, 213, 141));
        drawCentered(g, "Echoes of Alderfall", 124);
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.setColor(new Color(5, 8, 14, 180));
        drawCentered(g, "Alderfall remembers every name.", 178);
        g.setColor(new Color(215, 228, 239));
        drawCentered(g, "Alderfall remembers every name.", 174);

        int x = menuX;
        int y = menuY;
        int w = menuW;
        actionButton(g, x, y, w, 52, "New Adventure", state::openClassSelect, new Color(66, 93, 49), new Color(126, 176, 95), true);
        boolean hasSave = saves.exists();
        actionButton(g, x, y + 62, w, 52, "Load Adventure", this::openLoadMenu, new Color(57, 71, 102), new Color(110, 127, 160), hasSave);
        actionButton(g, x, y + 124, w, 52, "Import Character", this::openImportMenu, new Color(89, 68, 43), new Color(171, 124, 76), hasSave);
        if (!hasSave) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.setColor(new Color(165, 174, 193));
            drawCenteredIn(g, "No saved adventures yet.", x, y + 196, w);
        }
        actionButton(g, x, y + 196, w, 46, "Settings", state::openSettings, new Color(74, 66, 93), new Color(124, 107, 155), true);
        actionButton(g, x, y + 262, 164, 46, "Fullscreen", fullscreenToggle, new Color(74, 67, 80), new Color(117, 107, 128), true);
        actionButton(g, x + 176, y + 262, 164, 46, "Exit", this::exitGame, new Color(91, 60, 60), new Color(165, 111, 98), true);

        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(174, 184, 199));
        drawCentered(g, "The watchfires are lit beyond the pines.", viewHeight() - 66);
    }

    private void drawClassSelect(Graphics2D g) {
        drawTitleBackground(g, false);
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
        drawClassCard(g, 395, 505, "4", "Cleric", "Protective healing and ward magic", new Color(183, 160, 102), "class_cleric");
        drawClassCard(g, 725, 505, "5", "Rogue", "Quick strikes and poison tricks", new Color(126, 137, 126), "class_rogue");
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
        buttons.add(new UiButton(new java.awt.Rectangle(395, 505, 250, 230), "class:Cleric", () -> {
            state.chooseClass("Cleric");
            syncPlayerAnimationToState();
            repaint();
        }));
        buttons.add(new UiButton(new java.awt.Rectangle(725, 505, 250, 230), "class:Rogue", () -> {
            state.chooseClass("Rogue");
            syncPlayerAnimationToState();
            repaint();
        }));

        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.setColor(new Color(196, 198, 205));
        drawCentered(g, "Type a name, then click a class to start. Esc returns to the main menu.", 780);
        actionButton(g, 32, 32, 110, 34, "Back", state::openMainMenu, new Color(48, 55, 70), new Color(89, 102, 125), true);
    }

    private void drawStoryIntro(Graphics2D g) {
        drawTitleBackground(g, false);
        int w = 760;
        int h = 510;
        int x = centeredX(w);
        int y = 142;
        drawOverlayBase(g, x, y, w, h);
        g.setFont(new Font("Serif", Font.BOLD, 38));
        g.setColor(new Color(244, 213, 141));
        drawCenteredIn(g, "The Five Crowns Break", x, y + 62, w);

        g.setFont(new Font("SansSerif", Font.PLAIN, 19));
        g.setColor(new Color(223, 228, 235));
        wrap(g, state.storyIntroPage(), x + 58, y + 126, w - 116, 32);

        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(new Color(166, 175, 193));
        String page = (state.storyPage + 1) + " / " + GameState.STORY_INTRO.size();
        drawCenteredIn(g, page, x, y + h - 86, w);

        boolean lastPage = state.storyPage >= GameState.STORY_INTRO.size() - 1;
        actionButton(g, x + w - 220, y + h - 58, 150, 36, lastPage ? "Begin" : "Next", state::advanceStoryIntro,
                new Color(66, 93, 49), new Color(126, 176, 95), true);
        actionButton(g, x + 70, y + h - 58, 130, 36, "Back", state::openClassSelect,
                new Color(48, 55, 70), new Color(89, 102, 125), true);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(174, 184, 199));
        drawCentered(g, "Press Enter to continue.", y + h + 36);
    }

    private void drawClassCard(Graphics2D g, int x, int y, String key, String title, String subtitle, Color color) {
        drawClassCard(g, x, y, key, title, subtitle, color, "class_" + title.toLowerCase());
    }

    private void drawClassCard(Graphics2D g, int x, int y, String key, String title, String subtitle, Color color, String sprite) {
        g.setColor(new Color(28, 31, 43));
        g.fillRoundRect(x, y, 250, 230, 8, 8);
        g.setColor(color);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(x, y, 250, 230, 8, 8);
        g.drawImage(assets.sprite(sprite, 96), x + 77, y + 28, null);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(244, 239, 220));
        drawCenteredIn(g, key + " - " + title, x, y + 145, 250);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(205, 208, 216));
        drawCenteredIn(g, subtitle, x, y + 178, 250);
    }

    private void drawWorld(Graphics2D g) {
        long worldStarted = renderMetrics.start();
        int mapWidth = state.world.width(state.currentMapId);
        int mapHeight = state.world.height(state.currentMapId);
        int tileSize = tileSize();
        int worldViewportHeight = worldViewportHeight();
        double viewportTilesX = gameAreaWidth() / (double) tileSize;
        double viewportTilesY = worldViewportHeight / (double) tileSize;
        int visibleCols = Math.max(1, (int) Math.ceil(viewportTilesX) + 2);
        int visibleRows = Math.max(1, (int) Math.ceil(viewportTilesY) + 2);
        CameraView camera = updateCamera(viewportTilesX, viewportTilesY, mapWidth, mapHeight);
        double cameraX = camera.x();
        double cameraY = camera.y();
        int camX = clamp((int) Math.floor(cameraX), 0, Math.max(0, mapWidth - 1));
        int camY = clamp((int) Math.floor(cameraY), 0, Math.max(0, mapHeight - 1));
        double cameraOffsetX = (cameraX - camX) * tileSize;
        double cameraOffsetY = (cameraY - camY) * tileSize;
        String mapKind = state.world.kind(state.currentMapId);
        lastCamX = camX;
        lastCamY = camY;
        lastCameraX = cameraX;
        lastCameraY = cameraY;
        lastVisibleCols = visibleCols;
        lastVisibleRows = visibleRows;
        weather.refreshRenderMetrics();
        renderMetrics.sample("world.visibleTiles", (long) visibleCols * visibleRows);
        WorldRenderer.PropContext propContext = new WorldRenderer.PropContext(
                state,
                camX,
                camY,
                visibleCols,
                visibleRows,
                tileSize,
                state.zoom
        );
        long propsStarted = renderMetrics.start();
        worldRenderer.rebuildNearbyProps(propContext, nearbyWorldProps);
        renderMetrics.record("world.propsInBounds", propsStarted);
        renderMetrics.sample("world.nearbyProps", nearbyWorldProps.size());

        Graphics2D worldGraphics = (Graphics2D) g.create();
        worldGraphics.setClip(0, 0, gameAreaWidth(), worldViewportHeight);
        worldGraphics.translate(-cameraOffsetX, -cameraOffsetY);
        worldRenderer.drawTerrainBase(worldGraphics, new WorldRenderer.TerrainContext(
                state,
                camX,
                camY,
                visibleCols,
                visibleRows,
                mapWidth,
                mapHeight,
                tileSize,
                state.zoom,
                mapKind
        ));

        rebuildWorldLights(camX, camY, visibleCols, visibleRows, tileSize);
        renderMetrics.sample("world.activeLights", activeWorldLights.size());
        shadowWorldOffsetX = camX * (double) tileSize;
        shadowWorldOffsetY = camY * (double) tileSize;
        drawFieldConnectors(worldGraphics, camX, camY);
        worldRenderer.drawGroundPropOverlays(worldGraphics, propContext, nearbyWorldProps);
        drawMountainMassifOverlays(worldGraphics, camX, camY);
        drawRoadConnectors(worldGraphics, camX, camY);
        drawSettlementSurfaceSeams(worldGraphics, camX, camY);
        drawCityBuildingEntities(worldGraphics, camX, camY);
        drawVillageBuildingActionHover(worldGraphics, camX, camY, tileSize);
        drawVillageBuildingPlacementPreview(worldGraphics, camX, camY, tileSize);
        drawSettlementOverlays(worldGraphics, camX, camY);

        worldRenderer.drawVisibleProps(worldGraphics, propContext, nearbyWorldProps, visibleWorldProps);
        renderMetrics.sample("world.visibleProps", visibleWorldProps.size());
        drawFallingGatheredProp(worldGraphics, camX, camY, tileSize);
        drawQuestInteractibles(worldGraphics, camX, camY);
        drawQuestObjectives(worldGraphics, camX, camY);
        drawGatherTargetHighlight(worldGraphics, camX, camY, tileSize);
        for (GameState.DungeonMonsterMotion motion : state.dungeonMonsterMotionsForMap(state.currentMapId)) {
            double mx = renderDungeonMonsterX(motion);
            double my = renderDungeonMonsterY(motion);
            if (mx < camX - 1 || my < camY - 1 || mx >= camX + visibleCols + 1 || my >= camY + visibleRows + 1) {
                continue;
            }
            int px = (int) Math.round((mx - camX) * tileSize);
            int py = (int) Math.round((my - camY) * tileSize);
            int size = motion.boss() ? tileRelative(74, tileSize) : tileRelative(58, tileSize);
            int x = px + (tileSize - size) / 2;
            int y = py + tileSize - size - tileRelative(motion.boss() ? 8 : 5, tileSize);
            drawShadow(worldGraphics, px + tileRelative(6, tileSize), py + tileRelative(38, tileSize), tileRelative(motion.boss() ? 52 : 40, tileSize), tileRelative(10, tileSize));
            worldGraphics.drawImage(assets.spriteFit(motion.spec().sprite(), size, size), x, y, null);
            worldGraphics.setColor(motion.boss() ? new Color(228, 84, 72) : new Color(180, 216, 230));
            int marker = scaled(motion.boss() ? 9 : 6);
            worldGraphics.fillOval(px + tileSize - marker - scaled(7), py + scaled(3), marker, marker);
        }
        drawPlayerPathMarker(worldGraphics, camX, camY);
        drawInteriorPlacementHover(worldGraphics, camX, camY, tileSize);
        drawVillageTilePlacementPreview(worldGraphics, camX, camY, tileSize);
        drawVillagePropPlacementPreview(worldGraphics, camX, camY, tileSize);
        drawPlacementPulses(worldGraphics, camX, camY, tileSize);

        int visibleNpcCount = 0;
        for (GameState.NpcMotion motion : state.npcMotionsForMap(state.currentMapId)) {
            Npc npc = motion.npc();
            double nx = renderNpcX(motion);
            double ny = renderNpcY(motion);
            if (nx < camX - 1 || ny < camY - 1 || nx >= camX + visibleCols + 1 || ny >= camY + visibleRows + 1) {
                continue;
            }
            visibleNpcCount++;
            int px = (int) Math.round((nx - camX) * tileSize);
            int py = (int) Math.round((ny - camY) * tileSize);
            boolean moving = npcMoving(motion);
            String npcWorldSprite = npc.sprite() + "_model";
            CharacterWorldScale npcScale = characterWorldScale(npcWorldSprite, false);
            int npcW = tileRelative(npcScale.width(), tileSize);
            int npcH = tileRelative(npcScale.height(), tileSize);
            int step = moving ? npcWalkAnimationFrame(npcWorldSprite, npcW, npcH, motion) : -1;
            int npcX = px + (tileSize - npcW) / 2;
            int npcY = py + tileSize - npcH - tileRelative(npcScale.footLift(), tileSize);
            drawShadow(worldGraphics, px + tileRelative(24 - npcScale.shadowWidth() / 2, tileSize), py + tileRelative(40, tileSize), tileRelative(npcScale.shadowWidth(), tileSize), tileRelative(9, tileSize));
            drawCharacterSprite(worldGraphics, npcWorldSprite, npcX, npcY, npcW, npcH, motion.facingDx(), motion.facingDy(), step);
            Rectangle npcWorldBounds = new Rectangle(npcX, npcY, npcW, npcH);
            Rectangle npcScreenBounds = new Rectangle(
                    (int) Math.round(npcX - cameraOffsetX),
                    (int) Math.round(npcY - cameraOffsetY),
                    npcW,
                    npcH
            );
            Color npcAccent = currentSettlementAccentColor();
            String npcDisplayName = state.npcDisplayName(npc);
            tooltipZones.add(new TooltipZone(
                    npcScreenBounds,
                    npcDisplayName,
                    npcTooltip(npc),
                    "sprite:" + npc.sprite(),
                    npcAccent
            ));
            if (hoverPoint != null && npcScreenBounds.contains(hoverPoint)) {
                drawNpcHoverHighlight(worldGraphics, npcWorldBounds, npcDisplayName, npcAccent);
            }
            Quest npcQuest = state.questForNpc(npc);
            if (npcQuest != null && !npcQuest.completed) {
                drawNpcQuestMarker(worldGraphics, npcX + npcW / 2, npcY, npcQuest.ready() ? '?' : '!', npcQuest);
            } else {
                worldGraphics.setColor(new Color(255, 245, 174));
                worldGraphics.fillOval(px + scaled(31), py + scaled(2) - (moving ? scaled(2) : 0), scaled(7), scaled(7));
            }
        }
        renderMetrics.sample("world.visibleNpcs", visibleNpcCount);

        drawPartyFollowers(worldGraphics, camX, camY, tileSize, visibleCols, visibleRows, cameraOffsetX, cameraOffsetY);

        double playerX = renderPlayerX();
        double playerY = renderPlayerY();
        int playerPx = (int) Math.round((playerX - camX) * tileSize);
        int playerPy = (int) Math.round((playerY - camY) * tileSize);
        CharacterWorldScale playerScale = characterWorldScale(state.player.worldSprite, true);
        int playerW = tileRelative(playerScale.width(), tileSize);
        int playerH = tileRelative(playerScale.height(), tileSize);
        int step = playerMoving() ? playerWalkAnimationFrame(state.player.worldSprite, playerW, playerH) : -1;
        int bob = playerMoving()
                ? -(int) Math.round(Math.sin(moveProgress(frame - playerMoveStartFrame, playerMoveFrames()) * Math.PI) * tileRelative(2, tileSize))
                : 0;
        int playerXpx = playerPx + (tileSize - playerW) / 2;
        int playerYpx = playerPy + tileSize - playerH - tileRelative(playerScale.footLift(), tileSize) + bob;
        drawPlayerTravelEffects(worldGraphics, playerPx, playerPy);
        int shadowPulse = playerMoving()
                ? (int) Math.round(Math.sin(moveProgress(frame - playerMoveStartFrame, playerMoveFrames()) * Math.PI) * tileRelative(2, tileSize))
                : 0;
        drawShadow(worldGraphics, playerPx + tileRelative(24 - playerScale.shadowWidth() / 2, tileSize) - shadowPulse / 2, playerPy + tileRelative(40, tileSize), tileRelative(playerScale.shadowWidth(), tileSize) + shadowPulse, tileRelative(10, tileSize));
        drawCharacterSprite(worldGraphics, state.player.worldSprite, playerXpx, playerYpx, playerW, playerH, playerFacingDx, playerFacingDy, step);
        drawGatherToolSwing(worldGraphics, camX, camY, tileSize, playerPx, playerPy);
        drawNearbyQuestPrompt(worldGraphics, camX, camY);
        if (weather.effectsVisibleOnCurrentMap()) {
            drawCloudLayer(worldGraphics, camX, camY);
        }
        worldRenderer.drawLighting(worldGraphics, new WorldRenderer.LightingContext(
                camX,
                camY,
                visibleCols,
                visibleRows,
                tileSize,
                gameAreaWidth(),
                viewHeight()
        ));
        worldGraphics.dispose();
        drawTravelBanterPrompt(g, camX, camY, tileSize, cameraOffsetX, cameraOffsetY);
        long atmosphereStarted = renderMetrics.start();
        drawWorldAtmosphere(g);
        renderMetrics.record("atmosphere", atmosphereStarted);
        drawEmissiveWorldLights(g, camX, camY, tileSize, cameraOffsetX, cameraOffsetY);
        activeWorldLights.clear();
        shadowWorldOffsetX = 0.0;
        shadowWorldOffsetY = 0.0;
        renderMetrics.record("world", worldStarted);
    }

    private void drawNpcQuestMarker(Graphics2D g, int markerCenterX, int spriteTopY, char marker, Quest quest) {
        int radius = scaled(10);
        int bob = (int) Math.round(Math.sin(frame * 0.18) * scaled(2));
        int markerY = Math.max(scaled(2), spriteTopY - radius * 2 - scaled(7) - bob);
        int cx = markerCenterX;
        boolean ready = quest != null && quest.ready();
        boolean mainStory = quest != null && quest.mainStoryQuest();
        boolean companion = quest != null && quest.companionQuest();
        Color fill = mainStory
                ? new Color(220, 48, 54)
                : companion
                ? new Color(239, 139, 48)
                : ready ? new Color(93, 198, 255) : new Color(58, 135, 230);
        Color rim = mainStory
                ? new Color(255, 188, 184)
                : companion
                ? new Color(255, 221, 151)
                : ready ? new Color(215, 247, 255) : new Color(178, 219, 255);
        g.setColor(new Color(0, 0, 0, 150));
        g.fillOval(cx - radius - scaled(2), markerY - scaled(2), radius * 2 + scaled(4), radius * 2 + scaled(4));
        g.setColor(fill);
        g.fillOval(cx - radius, markerY, radius * 2, radius * 2);
        g.setColor(rim);
        g.setStroke(new BasicStroke(Math.max(1f, scaledStroke(1.4f))));
        g.drawOval(cx - radius, markerY, radius * 2, radius * 2);
        g.setColor(new Color(0, 0, 0, 130));
        Polygon tail = new Polygon(
                new int[]{cx - scaled(4), cx + scaled(4), cx},
                new int[]{markerY + radius * 2 - scaled(2), markerY + radius * 2 - scaled(2), markerY + radius * 2 + scaled(7)},
                3
        );
        g.fillPolygon(tail);
        g.setColor(fill);
        g.fillPolygon(tail);
        g.setStroke(new BasicStroke(1f));
        g.setColor(mainStory ? new Color(255, 246, 230) : companion ? new Color(57, 32, 10) : new Color(7, 28, 54));
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(11, scaled(13))));
        String text = Character.toString(marker);
        FontMetrics metrics = g.getFontMetrics();
        int textX = cx - metrics.stringWidth(text) / 2;
        int textY = markerY + scaled(14);
        g.drawString(text, textX, textY);
    }

    private void drawPartyFollowers(Graphics2D g, int camX, int camY, int tileSize, int visibleCols, int visibleRows,
                                    double cameraOffsetX, double cameraOffsetY) {
        visiblePartyFollowers.clear();
        List<Actor> followers = state.activeAllies();
        if (followers.isEmpty()) {
            return;
        }
        ensureFollowerTrailMap();
        Set<TilePoint> occupied = new HashSet<>();
        Set<String> activeKeys = new HashSet<>();
        occupied.add(new TilePoint(state.playerX, state.playerY));
        for (int i = Math.min(followers.size(), 4) - 1; i >= 0; i--) {
            Actor follower = followers.get(i);
            activeKeys.add(followerVisualKey(follower));
            FollowerTrailStep trail = followerTrailStep(i, occupied);
            occupied.add(new TilePoint(trail.x(), trail.y()));
            FollowerVisualState visual = updateFollowerVisualState(follower, trail);
            double[] render = renderFollowerPosition(visual);
            double fx = render[0];
            double fy = render[1];
            if (fx < camX - 1 || fy < camY - 1 || fx >= camX + visibleCols + 1 || fy >= camY + visibleRows + 1) {
                continue;
            }
            int px = (int) Math.round((fx - camX) * tileSize);
            int py = (int) Math.round((fy - camY) * tileSize);
            CharacterWorldScale scale = characterWorldScale(follower.worldSprite, false);
            int followerW = tileRelative(scale.width(), tileSize);
            int followerH = tileRelative(scale.height(), tileSize);
            int drawX = px + (tileSize - followerW) / 2;
            int drawY = py + tileSize - followerH - tileRelative(scale.footLift(), tileSize);
            boolean moving = followerMoving(visual);
            int facingDx = visual.facingDx;
            int facingDy = visual.facingDy;
            int step = moving ? followerWalkAnimationFrame(follower.worldSprite, followerW, followerH, facingDx, facingDy, visual) : -1;
            drawShadow(g, px + tileRelative(24 - scale.shadowWidth() / 2, tileSize), py + tileRelative(40, tileSize),
                    tileRelative(scale.shadowWidth(), tileSize), tileRelative(9, tileSize));
            drawCharacterSprite(g, follower.worldSprite, drawX, drawY, followerW, followerH, facingDx, facingDy, step);
            Rectangle worldBounds = new Rectangle(drawX, drawY, followerW, followerH);
            Rectangle screenBounds = new Rectangle(
                    (int) Math.round(drawX - cameraOffsetX),
                    (int) Math.round(drawY - cameraOffsetY),
                    followerW,
                    followerH
            );
            TilePoint tile = new TilePoint(trail.x(), trail.y());
            visiblePartyFollowers.add(new PartyFollowerRender(follower, tile, screenBounds));
            Color accent = playerDialogueAccent();
            tooltipZones.add(new TooltipZone(screenBounds, follower.name,
                    "Party member: " + follower.className + ". Right-click while hovering to talk.", "sprite:" + follower.sprite, accent));
            if (hoverPoint != null && screenBounds.contains(hoverPoint)) {
                drawNpcHoverHighlight(g, worldBounds, follower.name, accent);
            }
        }
        followerVisuals.keySet().removeIf(key -> !activeKeys.contains(key));
    }

    private void ensureFollowerTrailMap() {
        if (!state.currentMapId.equals(followerTrailMapId)) {
            followerTrailMapId = state.currentMapId;
            playerTrail.clear();
            followerVisuals.clear();
        }
    }

    private String followerVisualKey(Actor follower) {
        return follower.sprite + ":" + follower.name;
    }

    private FollowerTrailStep followerTrailStep(int followerIndex, Set<TilePoint> occupied) {
        int trailIndex = followerIndex;
        if (trailIndex < playerTrail.size()) {
            FollowerTrailStep trail = playerTrail.get(trailIndex);
            TilePoint tile = new TilePoint(trail.x(), trail.y());
            if (!tile.equals(new TilePoint(state.playerX, state.playerY)) && !occupied.contains(tile)) {
                return trail;
            }
        }
        TilePoint fallback = fallbackFollowerTile(followerIndex, occupied);
        return new FollowerTrailStep(state.currentMapId, fallback.x(), fallback.y(), fallback.x(), fallback.y(),
                frame - playerMoveFrames(), playerMoveFrames(), playerFacingDx, playerFacingDy);
    }

    private TilePoint fallbackFollowerTile(int followerIndex, Set<TilePoint> occupied) {
        int backDx = playerFacingDx == 0 && playerFacingDy == 0 ? 0 : -playerFacingDx;
        int backDy = playerFacingDx == 0 && playerFacingDy == 0 ? 1 : -playerFacingDy;
        int sideDx = backDy;
        int sideDy = -backDx;
        int[][] offsets = {
                {backDx, backDy},
                {backDx + sideDx, backDy + sideDy},
                {backDx - sideDx, backDy - sideDy},
                {backDx * 2, backDy * 2},
                {backDx * 2 + sideDx, backDy * 2 + sideDy},
                {backDx * 2 - sideDx, backDy * 2 - sideDy},
                {-sideDx, -sideDy},
                {sideDx, sideDy}
        };
        for (int i = 0; i < offsets.length; i++) {
            int index = Math.floorMod(followerIndex + i, offsets.length);
            TilePoint candidate = new TilePoint(state.playerX + offsets[index][0], state.playerY + offsets[index][1]);
            if (!occupied.contains(candidate) && Pathfinder.walkable(state, candidate.x(), candidate.y())) {
                return candidate;
            }
        }
        for (int radius = 1; radius <= 3; radius++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (Math.abs(dx) + Math.abs(dy) != radius) {
                        continue;
                    }
                    TilePoint candidate = new TilePoint(state.playerX + dx, state.playerY + dy);
                    if (!occupied.contains(candidate) && Pathfinder.walkable(state, candidate.x(), candidate.y())) {
                        return candidate;
                    }
                }
            }
        }
        return new TilePoint(playerMoveFromX, playerMoveFromY);
    }

    private FollowerVisualState updateFollowerVisualState(Actor follower, FollowerTrailStep trail) {
        String key = followerVisualKey(follower);
        FollowerVisualState visual = followerVisuals.get(key);
        int facingDx = trail.facingDx() == 0 && trail.facingDy() == 0 ? playerFacingDx : trail.facingDx();
        int facingDy = trail.facingDx() == 0 && trail.facingDy() == 0 ? playerFacingDy : trail.facingDy();
        if (visual == null || !state.currentMapId.equals(visual.mapId)) {
            visual = new FollowerVisualState(state.currentMapId, trail.x(), trail.y(), facingDx, facingDy, frame, trail.durationFrames());
            followerVisuals.put(key, visual);
            return visual;
        }
        if (visual.targetX != trail.x() || visual.targetY != trail.y()) {
            double[] current = renderFollowerPosition(visual);
            visual.fromX = current[0];
            visual.fromY = current[1];
            visual.targetX = trail.x();
            visual.targetY = trail.y();
            visual.startFrame = frame;
            visual.durationFrames = Math.max(1, trail.durationFrames());
            int dx = signumForDirection(visual.targetX - visual.fromX);
            int dy = signumForDirection(visual.targetY - visual.fromY);
            if (Math.abs(visual.targetX - visual.fromX) >= Math.abs(visual.targetY - visual.fromY) && dx != 0) {
                visual.facingDx = dx;
                visual.facingDy = 0;
            } else if (dy != 0) {
                visual.facingDx = 0;
                visual.facingDy = dy;
            } else if (Math.abs(facingDx) + Math.abs(facingDy) == 1) {
                visual.facingDx = facingDx;
                visual.facingDy = facingDy;
            }
            visual.walkTileStart++;
        } else if (!followerMoving(visual) && Math.abs(facingDx) + Math.abs(facingDy) == 1) {
            visual.facingDx = facingDx;
            visual.facingDy = facingDy;
        }
        return visual;
    }

    private int signumForDirection(double value) {
        if (value > 0.01) {
            return 1;
        }
        if (value < -0.01) {
            return -1;
        }
        return 0;
    }

    private boolean followerMoving(FollowerVisualState visual) {
        return visual != null
                && state.currentMapId.equals(visual.mapId)
                && frame - visual.startFrame < visual.durationFrames
                && (Math.abs(visual.targetX - visual.fromX) > 0.01 || Math.abs(visual.targetY - visual.fromY) > 0.01);
    }

    private double[] renderFollowerPosition(FollowerVisualState visual) {
        if (!followerMoving(visual)) {
            return new double[]{visual.targetX, visual.targetY};
        }
        double t = smoothStep(moveProgress(frame - visual.startFrame, visual.durationFrames));
        return new double[]{
                visual.fromX + (visual.targetX - visual.fromX) * t,
                visual.fromY + (visual.targetY - visual.fromY) * t
        };
    }

    private int followerWalkAnimationFrame(String sprite, int width, int height, int facingDx, int facingDy, FollowerVisualState visual) {
        DirectionalSprite directional = directionalSprite(sprite, facingDx, facingDy);
        int animationFrames = Math.max(1, assets.animatedSpriteFrameCount(directional.sprite(), "walk", width, height));
        int framesPerTile = Math.max(1, Math.min(animationFrames, WALK_CYCLE_FRAMES_PER_TILE));
        double progress = moveProgress(frame - visual.startFrame, visual.durationFrames);
        int animationStep = (int) Math.floor((visual.walkTileStart + progress) * framesPerTile);
        return Math.floorMod(animationStep, animationFrames);
    }

    private void recordFollowerTrailStep(String mapId, int fromX, int fromY, int durationFrames, int facingDx, int facingDy) {
        if (!mapId.equals(followerTrailMapId)) {
            followerTrailMapId = mapId;
            playerTrail.clear();
            followerVisuals.clear();
        }
        List<TilePoint> oldTargets = playerTrail.stream()
                .map(step -> new TilePoint(step.x(), step.y()))
                .toList();
        List<TilePoint> targets = new ArrayList<>();
        targets.add(new TilePoint(fromX, fromY));
        targets.addAll(oldTargets);
        playerTrail.clear();
        int limit = Math.min(targets.size(), 72);
        for (int i = 0; i < limit; i++) {
            TilePoint target = targets.get(i);
            TilePoint source = i < oldTargets.size() ? oldTargets.get(i) : target;
            int dx = target.x() - source.x();
            int dy = target.y() - source.y();
            int stepFacingDx = Math.abs(dx) + Math.abs(dy) == 1 ? dx : facingDx;
            int stepFacingDy = Math.abs(dx) + Math.abs(dy) == 1 ? dy : facingDy;
            playerTrail.add(new FollowerTrailStep(mapId, target.x(), target.y(), source.x(), source.y(),
                    frame, durationFrames, stepFacingDx, stepFacingDy));
        }
    }

    private void drawTravelBanterPrompt(Graphics2D g, int camX, int camY, int tileSize, double cameraOffsetX, double cameraOffsetY) {
        GameState.TravelBanterPrompt prompt = state.activeTravelBanter();
        if (prompt == null) {
            return;
        }
        int width = Math.min(460, gameAreaWidth() - 28);
        int lineHeight = 18;
        int optionH = 34;
        int optionGap = 7;
        int height = 78 + prompt.options().size() * (optionH + optionGap);
        int x = clamp(gameAreaWidth() - width - 18, 14, Math.max(14, gameAreaWidth() - width - 14));
        int y = clamp(worldViewportHeight() - height - 18, 14, Math.max(14, worldViewportHeight() - height - 14));

        Graphics2D bubble = (Graphics2D) g.create();
        bubble.setColor(new Color(8, 12, 20, 220));
        bubble.fillRoundRect(x, y, width, height, 8, 8);
        bubble.setColor(new Color(122, 169, 218, 210));
        bubble.setStroke(new BasicStroke(1.2f));
        bubble.drawRoundRect(x, y, width, height, 8, 8);
        bubble.setFont(new Font("SansSerif", Font.BOLD, 13));
        bubble.setColor(new Color(235, 242, 250));
        drawClippedString(bubble, prompt.speaker(), x + 14, y + 20, width - 28);
        bubble.setFont(new Font("SansSerif", Font.PLAIN, 12));
        bubble.setColor(new Color(213, 222, 232));
        drawWrapped(bubble, prompt.line(), x + 14, y + 41, width - 28, lineHeight, 2);

        int optionY = y + 70;
        for (int i = 0; i < prompt.options().size(); i++) {
            int index = i;
            Rectangle bounds = new Rectangle(x + 12, optionY, width - 24, optionH);
            buttons.add(new UiButton(bounds, "banter:" + i, () -> {
                state.replyToTravelBanter(index);
                repaint();
            }));
            boolean hovered = hoverPoint != null && bounds.contains(hoverPoint);
            bubble.setColor(hovered ? new Color(53, 72, 98, 245) : new Color(35, 47, 64, 235));
            bubble.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 6, 6);
            bubble.setColor(hovered ? new Color(152, 184, 226, 235) : new Color(98, 139, 184, 220));
            bubble.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 6, 6);
            bubble.setColor(new Color(239, 243, 248));
            bubble.setFont(new Font("SansSerif", Font.BOLD, 12));
            drawClippedString(bubble, (i + 1) + ". " + prompt.options().get(i), bounds.x + 11, bounds.y + 21, bounds.width - 22);
            String tooltip = index < prompt.optionTooltips().size()
                    ? prompt.optionTooltips().get(index)
                    : "Reply to the travelling companion without opening a full dialogue.";
            tooltipZones.add(new TooltipZone(bounds, prompt.options().get(i), tooltip));
            optionY += optionH + optionGap;
        }
        bubble.dispose();
    }

    private void drawWorldProp(Graphics2D g, WorldProp prop, int camX, int camY, int tileSize) {
        String asset = prop.asset();
        if (isDisallowedSettlementOutdoorProp(asset)) {
            return;
        }
        if (isVillageFenceAsset(asset)) {
            drawVillageFenceProp(g, prop, camX, camY, tileSize);
            return;
        }
        int size = propRenderSize(asset, prop.size(), tileSize);
        int drawW = interiorPropWidth(asset, size);
        int drawH = interiorPropHeight(asset, size);
        int px = (prop.x() - camX) * tileSize + (tileSize - drawW) / 2;
        int py = (prop.y() - camY) * tileSize + tileSize - drawH;
        if (asset.startsWith("interior_")) {
            px += interiorPropOffsetX(asset, tileSize);
            py += interiorPropOffsetY(asset, tileSize);
        }

        if (isSoftGroundProp(asset)) {
            int seed = prop.x() * 928371 + prop.y() * 364479 + asset.hashCode();
            px += scaled(Math.floorMod(seed, 7) - 3);
            py += scaled(Math.floorMod(seed / 13, 5) - 2);
            drawPropGroundBlend(g, prop.x(), prop.y(), px, py, size, 0.32f, true);
            drawAnimatedPropImage(g, prop, asset, px, py, size, 0.90f);
            drawPropGroundVeil(g, prop.x(), prop.y(), px, py, size, 0.16f);
            drawPropAmbientAnimation(g, prop, px, py, size);
            return;
        }

        boolean naturalBlend = isNaturalLowProp(asset);
        if (naturalBlend) {
            drawPropGroundBlend(g, prop.x(), prop.y(), px, py, size, 0.20f, false);
        }
        if (castsPropShadow(asset)) {
            BufferedImage image = propImage(asset, drawW, drawH);
            drawCasterShadow(g, image, "prop:" + asset + ":" + drawW + "x" + drawH,
                    px, py, drawW, drawH, false, 0.36f);
            drawShadow(g, px + drawW / 6, py + drawH - scaled(8), drawW * 2 / 3, scaled(8));
        }
        if (asset.startsWith("interior_")) {
            drawPropImage(g, asset, px, py, drawW, drawH, 1.0f);
            drawPropAmbientAnimation(g, prop, px, py, Math.max(drawW, drawH));
            return;
        }
        drawAnimatedPropImage(g, prop, asset, px, py, size, 1.0f);
        if (naturalBlend) {
            drawPropGroundVeil(g, prop.x(), prop.y(), px, py, size, 0.08f);
        }
        drawPropAmbientAnimation(g, prop, px, py, size);
    }

    private void drawFallingGatheredProp(Graphics2D g, int camX, int camY, int tileSize) {
        WorldProp prop = state.lastGatheredProp;
        if (prop == null || !state.currentMapId.equals(state.lastGatheredPropMapId)) {
            return;
        }
        int age = state.worldTick - state.lastGatheredPropWorldTick;
        if (age < 0 || age > 44) {
            return;
        }
        String asset = prop.asset();
        if (!isTreeGatherAsset(asset)) {
            return;
        }
        if (prop.x() < camX - 1 || prop.y() < camY - 1 || prop.x() > camX + lastVisibleCols || prop.y() > camY + lastVisibleRows) {
            return;
        }
        int size = propRenderSize(asset, prop.size(), tileSize);
        int drawW = interiorPropWidth(asset, size);
        int drawH = interiorPropHeight(asset, size);
        int baseX = (prop.x() - camX) * tileSize + tileSize / 2;
        int baseY = (prop.y() - camY) * tileSize + tileSize;
        double progress = Math.min(1.0, age / 34.0);
        double eased = 1.0 - Math.pow(1.0 - progress, 3.0);
        double direction = Math.floorMod(prop.x() * 31 + prop.y() * 17 + asset.hashCode(), 2) == 0 ? -1.0 : 1.0;
        double angle = direction * eased * 1.36;
        float alpha = (float) Math.max(0.0, 1.0 - Math.max(0.0, progress - 0.72) / 0.28 * 0.55);

        Composite oldComposite = g.getComposite();
        AffineTransform oldTransform = g.getTransform();
        drawShadow(g, baseX - drawW / 3, baseY - scaled(7), drawW * 2 / 3, scaled(9));
        g.setComposite(AlphaComposite.SrcOver.derive(alpha));
        g.translate(baseX, baseY - scaled(3));
        g.rotate(angle);
        g.drawImage(propImage(asset, drawW, drawH), -drawW / 2, -drawH, drawW, drawH, null);
        g.setTransform(oldTransform);
        g.setComposite(oldComposite);
    }

    private void drawGatherToolSwing(Graphics2D g, int camX, int camY, int tileSize, int playerPx, int playerPy) {
        CraftingSystem.GatherCandidate candidate = state.activeGatherCandidate;
        if (!state.crafting.active() || candidate == null || !state.currentMapId.equals(state.activeGatherMapId)) {
            return;
        }
        TilePoint tile = candidate.tile();
        if (tile.x() < camX - 1 || tile.y() < camY - 1 || tile.x() > camX + lastVisibleCols || tile.y() > camY + lastVisibleRows) {
            return;
        }
        String tool = gatherToolKind(candidate);
        double playerToolX = playerPx + tileSize / 2.0;
        double playerToolY = playerPy + tileRelative(24, tileSize);
        double hitX = (tile.x() - camX) * (double) tileSize + tileSize / 2.0;
        double hitY = (tile.y() - camY) * (double) tileSize + gatherToolHitYOffset(tool, tileSize);
        double targetAngle = Math.atan2(hitY - playerToolY, hitX - playerToolX);
        if (Math.abs(hitX - playerToolX) < 0.01 && Math.abs(hitY - playerToolY) < 0.01) {
            targetAngle = Math.atan2(playerFacingDy, playerFacingDx == 0 && playerFacingDy == 0 ? 1 : playerFacingDx);
        }
        double phase = (frame % 24) / 24.0;
        double strike = Math.sin(phase * Math.PI);
        double windup = 1.0 - strike;
        double side = Math.cos(targetAngle) < 0.0 ? -1.0 : 1.0;
        double swingX = hitX
                - Math.cos(targetAngle) * windup * tileRelative(10, tileSize)
                - Math.sin(targetAngle) * side * windup * tileRelative(16, tileSize);
        double swingY = hitY
                - Math.sin(targetAngle) * windup * tileRelative(10, tileSize)
                + Math.cos(targetAngle) * side * windup * tileRelative(16, tileSize);
        int size = tileRelative("sickle".equals(tool) ? 30 : 34, tileSize);
        double[] localHit = gatherToolLocalHitPoint(tool, size);
        double localHitAngle = Math.atan2(localHit[1], localHit[0]);
        double angle = targetAngle - localHitAngle + side * windup * 0.92;
        drawGatherToolIcon(g, tool, swingX, swingY, angle, size, localHit);
        if (strike > 0.86) {
            drawGatherImpact(g, tile, camX, camY, tileSize, tool);
        }
    }

    private void drawGatherToolIcon(Graphics2D g, String tool, double hitX, double hitY, double angle, int size, double[] localHit) {
        String asset = gatherToolAsset(tool);
        BufferedImage image = assets.spriteFit(asset, size, size);
        Graphics2D toolG = (Graphics2D) g.create();
        toolG.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        toolG.translate(hitX, hitY);
        toolG.rotate(angle);
        toolG.drawImage(image,
                (int) Math.round(-size / 2.0 - localHit[0]),
                (int) Math.round(-size / 2.0 - localHit[1]),
                size,
                size,
                null);
        toolG.dispose();
    }

    private double gatherToolHitYOffset(String tool, int tileSize) {
        return tileRelative("sickle".equals(tool) ? 27 : 30, tileSize);
    }

    private double[] gatherToolLocalHitPoint(String tool, int size) {
        return switch (tool) {
            case "pickaxe" -> new double[]{size * 0.32, -size * 0.24};
            case "sickle" -> new double[]{size * 0.30, -size * 0.28};
            default -> new double[]{-size * 0.28, -size * 0.30};
        };
    }

    private String gatherToolAsset(String tool) {
        return switch (tool) {
            case "pickaxe" -> "gather_pickaxe";
            case "sickle" -> "gather_sickle";
            default -> "woodcutter_axe";
        };
    }

    private String gatherSoundName(String tool) {
        return switch (tool) {
            case "pickaxe" -> "gather_stone";
            case "sickle" -> "gather_sickle";
            default -> "gather_wood";
        };
    }

    private void drawGatherImpact(Graphics2D g, TilePoint tile, int camX, int camY, int tileSize, String tool) {
        int px = (tile.x() - camX) * tileSize + tileSize / 2;
        int py = (tile.y() - camY) * tileSize + tileRelative(30, tileSize);
        Composite oldComposite = g.getComposite();
        Stroke oldStroke = g.getStroke();
        g.setComposite(AlphaComposite.SrcOver.derive(0.55f));
        g.setStroke(new BasicStroke(Math.max(1f, scaledStroke(1.5f)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor("pickaxe".equals(tool) ? new Color(224, 219, 196) : "sickle".equals(tool) ? new Color(162, 230, 139) : new Color(236, 214, 134));
        int spark = tileRelative(5, tileSize);
        g.drawLine(px - spark, py, px + spark, py);
        g.drawLine(px, py - spark, px, py + spark);
        g.setStroke(oldStroke);
        g.setComposite(oldComposite);
    }

    private String gatherToolKind(CraftingSystem.GatherCandidate candidate) {
        String asset = candidate.asset() == null ? "" : candidate.asset().toLowerCase();
        String label = candidate.label() == null ? "" : candidate.label().toLowerCase();
        if (candidate.terrain() == 'm' || candidate.terrain() == 'q' || isOreResourceAsset(asset)
                || asset.contains("stone") || asset.contains("rock") || asset.contains("crystal")) {
            return "pickaxe";
        }
        if (candidate.terrain() == 'f' || isTreeGatherAsset(asset) || label.contains("wood") || label.contains("tree")) {
            return "axe";
        }
        return "sickle";
    }

    private boolean isTreeGatherAsset(String asset) {
        String lower = asset == null ? "" : asset.toLowerCase();
        return lower.contains("tree")
                || lower.contains("pine")
                || lower.contains("log")
                || lower.contains("stump")
                || lower.contains("woodpile");
    }

    private void drawPropImage(Graphics2D g, String asset, int x, int y, int size, float opacity) {
        drawPropImage(g, asset, x, y, size, size, opacity);
    }

    private void drawPropImage(Graphics2D g, String asset, int x, int y, int width, int height, float opacity) {
        Composite oldComposite = g.getComposite();
        if (opacity < 1.0f) {
            g.setComposite(AlphaComposite.SrcOver.derive(opacity));
        }
        BufferedImage image = propImage(asset, width, height);
        g.drawImage(image, x, y, null);
        g.setComposite(oldComposite);
    }

    private BufferedImage propImage(String asset, int width, int height) {
        return asset.startsWith("interior_")
                ? assets.image(asset, width, height)
                : assets.spriteFit(asset, width, height);
    }

    private int interiorPropWidth(String asset, int size) {
        if (asset.equals("interior_tavern_bar") || asset.equals("interior_shop_counter")
                || asset.equals("interior_carpenter_table") || asset.equals("interior_alchemy_station")
                || asset.equals("interior_cooking_station") || asset.equals("interior_herb_drying_rack")
                || asset.equals("interior_wall_window_wide") || asset.equals("interior_wall_plant_shelf")
                || asset.equals("interior_wall_herb_rack") || asset.equals("interior_floor_bushy_planter")
                || asset.equals("interior_aquarium_table") || asset.equals("interior_carpenter_workbench")
                || asset.equals("interior_metal_crate") || asset.equals("interior_bakery_counter")
                || asset.equals("interior_tavern_counter") || asset.equals("interior_long_table_benches")
                || asset.equals("interior_sawhorse_planks") || asset.equals("interior_storage_counter")
                || asset.equals("interior_low_cupboard") || asset.equals("interior_table_h_left")
                || asset.equals("interior_table_h_middle") || asset.equals("interior_table_h_right")
                || asset.equals("interior_bench_h") || asset.equals("interior_banquet_table_h")
                || asset.equals("interior_stool_table_h") || asset.equals("interior_study_desk_h")
                || asset.equals("interior_counter_corner_h")) {
            return size * 2;
        }
        return size;
    }

    private int propRenderSize(String asset, int logicalSize, int tileSize) {
        int zoomSize = scaled(logicalSize);
        int tileSizeBased = Math.max(1, Math.round(logicalSize * tileSize / (float) GameConfig.TILE));
        int size = Math.max(zoomSize, tileSizeBased);
        if (!asset.startsWith("interior_")) {
            return outdoorPropRenderSize(asset, size, tileSize);
        }
        if (isInteriorTabletopAsset(asset)) {
            return Math.max(1, Math.round(tileSize * 0.58f));
        }
        if (isInteriorWallDecorAsset(asset)) {
            return Math.max(1, Math.round(tileSize * 0.86f));
        }
        if (asset.equals("interior_chair_north") || asset.equals("interior_chair_south")
                || asset.equals("interior_chair_east") || asset.equals("interior_chair_west")
                || asset.equals("interior_chair_north_alt") || asset.equals("interior_chair_south_alt")
                || asset.equals("interior_chair_east_alt") || asset.equals("interior_chair_west_alt")) {
            return Math.max(1, Math.round(tileSize * 0.84f));
        }
        if (asset.equals("interior_round_table")) {
            return Math.max(1, Math.round(tileSize * 1.02f));
        }
        if (asset.equals("interior_herb_pot") || asset.equals("interior_flower_pot")
                || asset.equals("interior_planting_pot") || asset.equals("interior_cookpot_stand")) {
            return Math.max(1, Math.round(tileSize * 0.90f));
        }
        if (asset.equals("interior_sprout_planter") || asset.equals("interior_herb_planter")) {
            return Math.max(1, Math.round(tileSize * 1.0f));
        }
        if (asset.equals("interior_bookshelf")) {
            return Math.max(1, Math.round(tileSize * 1.08f));
        }
        if (asset.equals("interior_side_table") || asset.equals("interior_anvil") || asset.equals("interior_forge")
                || asset.equals("interior_stove") || asset.equals("interior_oven")
                || asset.equals("interior_cooking_station") || asset.equals("interior_alchemy_station")
                || asset.equals("interior_aquarium_table")) {
            return Math.max(1, Math.round(tileSize * 1.05f));
        }
        if (asset.equals("interior_table_h_left") || asset.equals("interior_table_h_middle")
                || asset.equals("interior_table_h_right") || asset.equals("interior_bench_h")
                || asset.equals("interior_banquet_table_h") || asset.equals("interior_stool_table_h")
                || asset.equals("interior_study_desk_h") || asset.equals("interior_counter_corner_h")
                || asset.equals("interior_table_v_top") || asset.equals("interior_table_v_middle")
                || asset.equals("interior_table_v_bottom") || asset.equals("interior_bench_v")
                || asset.equals("interior_carpenter_workbench") || asset.equals("interior_long_table_benches")
                || asset.equals("interior_metal_crate") || asset.equals("interior_bakery_counter")
                || asset.equals("interior_tavern_counter") || asset.equals("interior_sawhorse_planks")
                || asset.equals("interior_storage_counter") || asset.equals("interior_low_cupboard")) {
            return Math.max(1, Math.round(tileSize * 1.02f));
        }
        if (asset.equals("interior_bakery_oven") || asset.equals("interior_carpenter_workbench")
                || asset.equals("interior_long_table_benches") || asset.equals("interior_metal_crate")
                || asset.equals("interior_traveler_trunk") || asset.equals("interior_bakery_counter")
                || asset.equals("interior_tavern_counter") || asset.equals("interior_sawhorse_planks")
                || asset.equals("interior_storage_counter") || asset.equals("interior_low_cupboard")
                || asset.equals("interior_resident_bed") || asset.equals("interior_herb_drying_rack_v")
                || asset.equals("interior_linen_shelf") || asset.equals("interior_anvil_tool_rack")
                || asset.equals("interior_grain_sacks_v") || asset.equals("interior_inn_screen_chest")
                || asset.equals("interior_table_h_left") || asset.equals("interior_table_h_middle")
                || asset.equals("interior_table_h_right") || asset.equals("interior_bench_h")
                || asset.equals("interior_banquet_table_h") || asset.equals("interior_stool_table_h")
                || asset.equals("interior_study_desk_h") || asset.equals("interior_counter_corner_h")
                || asset.equals("interior_table_v_top") || asset.equals("interior_table_v_middle")
                || asset.equals("interior_table_v_bottom") || asset.equals("interior_bench_v")) {
            return Math.max(1, Math.round(tileSize * 1.0f));
        }
        if (asset.equals("interior_floor_leafy_plant") || asset.equals("interior_floor_sapling_pot")
                || asset.equals("interior_floor_bushy_planter") || asset.equals("interior_floor_reed_pot")
                || asset.equals("interior_floor_flower_planter") || asset.equals("interior_vine_trellis")
                || asset.equals("interior_aquarium_table")) {
            return Math.max(1, Math.round(tileSize * 0.94f));
        }
        return size;
    }

    private int outdoorPropRenderSize(String asset, int size, int tileSize) {
        if (asset.startsWith("town_portal_")) {
            return Math.max(size, Math.round(tileSize * 2.55f));
        }
        if (isVillageFenceAsset(asset)) {
            return Math.max(size, Math.round(tileSize * 1.04f));
        }
        if (asset.startsWith("town_park_accent_")) {
            return Math.max(size, Math.round(tileSize * 1.72f));
        }
        if (!isReadableOutdoorProp(asset)) {
            return size;
        }
        int boosted = Math.round(size * 1.32f);
        int minimum = Math.round(tileSize * 0.94f);
        int maximum = Math.round(tileSize * 1.38f);
        return Math.max(size, Math.min(maximum, Math.max(boosted, minimum)));
    }

    private boolean isReadableOutdoorProp(String asset) {
        return asset.startsWith("village_prop_")
                || isVillageFenceAsset(asset)
                || asset.startsWith("city_prop_")
                || asset.startsWith("town_portal_")
                || asset.equals("city_lantern")
                || asset.startsWith("location_camp_")
                || asset.equals("player_village_quest_board")
                || asset.equals("deco_road_signpost")
                || asset.equals("deco_road_milestone")
                || asset.equals("deco_imagen_signpost")
                || asset.equals("deco_imagen_milestone")
                || asset.equals("deco_imagen_road_camp")
                || isDiscoverabilityOutdoorProp(asset);
    }

    private boolean isDiscoverabilityOutdoorProp(String asset) {
        return asset.equals("deco_imagen_shrine_stone")
                || asset.equals("deco_forest_shrine_stone")
                || asset.equals("deco_imagen_green_rune_stone")
                || asset.equals("deco_imagen_tundra_rune_stone")
                || asset.equals("deco_imagen_stone_stack")
                || asset.equals("deco_mountain_cairn")
                || asset.equals("deco_mountain_pass_way_cairn")
                || asset.equals("location_ruin_standing_stones")
                || asset.equals("deco_tree_elder_harvestable");
    }

    private boolean isInteriorWallDecorAsset(String asset) {
        return asset != null && asset.startsWith("interior_wall_");
    }

    private boolean isInteriorTabletopAsset(String asset) {
        return asset.equals("interior_tabletop_place_setting")
                || asset.equals("interior_tabletop_meal")
                || asset.equals("interior_tabletop_candle")
                || asset.equals("interior_flower_vase")
                || asset.equals("interior_seed_bowl")
                || asset.equals("interior_mortar_pestle");
    }

    private void drawVillageFenceProp(Graphics2D g, WorldProp prop, int camX, int camY, int tileSize) {
        int size = propRenderSize(prop.asset(), prop.size(), tileSize);
        int px = (prop.x() - camX) * tileSize + (tileSize - size) / 2;
        int py = (prop.y() - camY) * tileSize + tileSize - size;
        String asset = "village_fence_" + String.format("%02d", villageFenceBits(prop.x(), prop.y()));
        drawShadow(g, px + size / 6, py + size - scaled(9), size * 2 / 3, scaled(7));
        drawPropImage(g, asset, px, py, size, size, 1.0f);
    }

    private int villageFenceBits(int wx, int wy) {
        int bits = 0;
        if (connectsVillageFence(wx, wy - 1)) {
            bits |= 1;
        }
        if (connectsVillageFence(wx, wy + 1)) {
            bits |= 2;
        }
        if (connectsVillageFence(wx - 1, wy)) {
            bits |= 4;
        }
        if (connectsVillageFence(wx + 1, wy)) {
            bits |= 8;
        }
        return bits;
    }

    private boolean connectsVillageFence(int x, int y) {
        for (WorldProp prop : state.world.propsAt(state.currentMapId, x, y)) {
            if (isVillageFenceAsset(prop.asset())) {
                return true;
            }
        }
        return false;
    }

    private boolean isVillageFenceAsset(String asset) {
        return "village_fence_auto".equals(asset) || (asset != null && asset.startsWith("village_fence_"));
    }

    private boolean isDisallowedSettlementOutdoorProp(String asset) {
        if (asset == null || !isSettlementMapKind(state.currentMapId)) {
            return false;
        }
        String lower = asset.toLowerCase();
        return lower.contains("window")
                || lower.startsWith("interior_wall_")
                || lower.startsWith("city_building_")
                || lower.contains("wall_planter")
                || lower.contains("vine_trellis")
                || lower.contains("herb_planter_narrow")
                || lower.contains("ivy_wall_planter")
                || lower.contains("stone_arch")
                || lower.contains("arched")
                || lower.contains("graveyard_tombstone")
                || lower.contains("dungeon_grave")
                || lower.contains("crypt_sarcophagus");
    }

    private int interiorPropOffsetX(String asset, int tileSize) {
        if (asset.equals("interior_chair_east") || asset.equals("interior_chair_east_alt")) {
            return tileRelative(3, tileSize);
        }
        if (asset.equals("interior_chair_west") || asset.equals("interior_chair_west_alt")) {
            return -tileRelative(3, tileSize);
        }
        return 0;
    }

    private int interiorPropOffsetY(String asset, int tileSize) {
        if (isInteriorTabletopAsset(asset)) {
            return -tileRelative(12, tileSize);
        }
        if (isInteriorWallDecorAsset(asset)) {
            return -tileRelative(asset.contains("sconce") ? 7 : 4, tileSize);
        }
        if (asset.equals("interior_vine_trellis")) {
            return -tileRelative(8, tileSize);
        }
        if (asset.equals("interior_chair_north") || asset.equals("interior_chair_north_alt")) {
            return tileRelative(3, tileSize);
        }
        if (asset.equals("interior_chair_south") || asset.equals("interior_chair_south_alt")) {
            return -tileRelative(4, tileSize);
        }
        if (asset.equals("interior_side_table")) {
            return tileRelative(2, tileSize);
        }
        if (asset.equals("interior_bookshelf")) {
            return -tileRelative(14, tileSize);
        }
        if (asset.equals("interior_bakery_oven") || asset.equals("interior_traveler_trunk")
                || asset.equals("interior_resident_bed") || asset.equals("interior_herb_drying_rack_v")
                || asset.equals("interior_linen_shelf") || asset.equals("interior_anvil_tool_rack")
                || asset.equals("interior_grain_sacks_v") || asset.equals("interior_inn_screen_chest")
                || asset.equals("interior_table_v_top") || asset.equals("interior_table_v_middle")
                || asset.equals("interior_table_v_bottom") || asset.equals("interior_bench_v")) {
            return -tileRelative(8, tileSize);
        }
        return 0;
    }

    private int interiorPropHeight(String asset, int size) {
        if (asset.equals("interior_bookshelf")) {
            return Math.max(size, Math.round(size * 1.36f));
        }
        if (asset.equals("interior_bed_vertical") || asset.equals("interior_vine_trellis")
                || asset.equals("interior_bakery_oven") || asset.equals("interior_traveler_trunk")
                || asset.equals("interior_resident_bed") || asset.equals("interior_herb_drying_rack_v")
                || asset.equals("interior_linen_shelf") || asset.equals("interior_anvil_tool_rack")
                || asset.equals("interior_grain_sacks_v") || asset.equals("interior_inn_screen_chest")
                || asset.equals("interior_table_v_top") || asset.equals("interior_table_v_middle")
                || asset.equals("interior_table_v_bottom") || asset.equals("interior_bench_v")) {
            return size * 2;
        }
        return size;
    }

    private void drawAnimatedPropImage(Graphics2D g, WorldProp prop, String asset, int x, int y, int size, float opacity) {
        double sway = propWindSway(prop, asset);
        if (Math.abs(sway) < 0.003) {
            drawPropImage(g, asset, x, y, size, opacity);
            return;
        }

        AffineTransform oldTransform = g.getTransform();
        Composite oldComposite = g.getComposite();
        if (opacity < 1.0f) {
            g.setComposite(AlphaComposite.SrcOver.derive(opacity));
        }
        double anchorX = x + size / 2.0;
        double anchorY = y + size;
        g.translate(anchorX, anchorY);
        g.shear(sway, 0.0);
        g.drawImage(assets.spriteFit(asset, size, size), (int) Math.round(x - anchorX), (int) Math.round(y - anchorY), null);
        g.setTransform(oldTransform);
        g.setComposite(oldComposite);
    }

    private double propWindSway(WorldProp prop, String asset) {
        if (!isWindReactiveProp(asset) || !isOutdoorPropAnimationMap()) {
            return 0.0;
        }
        double strength = state.windStrength();
        double sideWind = Math.cos(state.windRadians());
        if (Math.abs(sideWind) < 0.08) {
            sideWind = Math.copySign(0.08, sideWind == 0.0 ? 1.0 : sideWind);
        }
        int seed = prop.x() * 928371 + prop.y() * 364479 + asset.hashCode();
        double phase = frame * (0.055 + strength * 0.105) + seed * 0.013;
        double gust = Math.sin(phase) * 0.72 + Math.sin(phase * 1.73 + seed * 0.003) * 0.28;
        return clamp(propWindSwayAmplitude(asset) * strength * sideWind * gust, -0.18, 0.18);
    }

    private double propWindSwayAmplitude(String asset) {
        if (asset.contains("wheat") || asset.contains("grass") || asset.contains("reed")
                || asset.contains("flower") || asset.contains("fern") || asset.contains("bloom")
                || asset.contains("cattail") || asset.contains("plant")) {
            return 0.115;
        }
        if (asset.contains("lily") || asset.contains("duckweed") || asset.contains("floating") || asset.contains("weed")) {
            return 0.070;
        }
        if (asset.contains("bush") || asset.contains("scrub")) {
            return 0.075;
        }
        if (asset.contains("tree") || asset.contains("pine")) {
            return 0.060;
        }
        return 0.045;
    }

    private boolean isWindReactiveProp(String asset) {
        return asset.contains("wheat")
                || asset.contains("tree")
                || asset.contains("pine")
                || asset.contains("grass")
                || asset.contains("reed")
                || asset.contains("flower")
                || asset.contains("fern")
                || asset.contains("bloom")
                || asset.contains("bush")
                || asset.contains("scrub")
                || asset.contains("clover")
                || asset.contains("leaf")
                || asset.contains("plant")
                || asset.contains("cattail")
                || asset.contains("lily")
                || asset.contains("duckweed")
                || asset.contains("floating")
                || asset.contains("weed");
    }

    private boolean isOutdoorPropAnimationMap() {
        String kind = state.world.kind(state.currentMapId);
        return "overworld".equals(kind) || "city".equals(kind) || "village".equals(kind);
    }

    private void drawPropAmbientAnimation(Graphics2D g, WorldProp prop, int x, int y, int size) {
        String asset = prop.asset();
        if (isFireProp(asset)) {
            drawFirePropAnimation(g, prop, x, y, size);
        }
        if (isMagicGlowProp(asset)) {
            drawMagicPropAnimation(g, prop, x, y, size);
        }
        if (isResourceParticleProp(asset)) {
            drawResourcePropParticles(g, prop, x, y, size);
        }
    }

    private boolean isFireProp(String asset) {
        return asset.contains("camp_fire") || asset.contains("campfire") || asset.contains("fire")
                || asset.contains("forge") || asset.contains("oven") || asset.contains("stove")
                || asset.contains("hearth") || asset.contains("cookpot") || asset.contains("cooking_station")
                || asset.contains("sconce") || asset.contains("tabletop_candle");
    }

    private boolean isMagicGlowProp(String asset) {
        return asset.startsWith("town_portal_")
                || asset.contains("crystal")
                || asset.contains("alchemy")
                || asset.contains("ice_crystals")
                || asset.contains("rune")
                || asset.contains("shrine")
                || asset.contains("fairy_pool")
                || asset.contains("bubble_pool")
                || asset.contains("firefly");
    }

    private boolean isResourceParticleProp(String asset) {
        return asset.contains("ore_")
                || asset.contains("_ore")
                || asset.contains("glowing_root")
                || asset.contains("glowroot")
                || asset.contains("magical_harvestable")
                || asset.contains("enchanted")
                || asset.contains("mithril")
                || asset.contains("cobalt")
                || asset.contains("adamantite")
                || asset.contains("gold")
                || asset.contains("silver");
    }

    private void drawFirePropAnimation(Graphics2D g, WorldProp prop, int x, int y, int size) {
        int seed = Math.abs(prop.x() * 928371 + prop.y() * 364479 + prop.asset().hashCode());
        double flicker = Math.sin(frame * 0.42 + seed * 0.01) * 0.5 + Math.sin(frame * 0.77 + seed * 0.03) * 0.5;
        int cx = x + size / 2 + scaled((int) Math.round(flicker));
        int baseY = y + Math.round(size * 0.68f);
        int flameH = Math.max(scaled(11), (int) Math.round(size * (0.34 + flicker * 0.035)));
        int flameW = Math.max(scaled(8), (int) Math.round(size * 0.24));

        drawRadialGlow(g, cx, baseY - flameH / 3, Math.max(scaled(18), size / 2), new Color(255, 143, 44), 0.18f);

        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver.derive(0.82f));
        Polygon outer = new Polygon(
                new int[]{cx - flameW / 2, cx + scaled((int) Math.round(flicker * 2)), cx + flameW / 2},
                new int[]{baseY, baseY - flameH, baseY},
                3
        );
        g.setColor(new Color(255, 92, 34, 220));
        g.fillPolygon(outer);

        g.setComposite(AlphaComposite.SrcOver.derive(0.86f));
        Polygon inner = new Polygon(
                new int[]{cx - flameW / 4, cx - scaled((int) Math.round(flicker)), cx + flameW / 4},
                new int[]{baseY - scaled(1), baseY - flameH * 3 / 4, baseY - scaled(1)},
                3
        );
        g.setColor(new Color(255, 220, 98, 235));
        g.fillPolygon(inner);

        for (int i = 0; i < 5; i++) {
            double life = Math.floorMod(frame * 4 + seed + i * 23, 80) / 80.0;
            int sparkX = cx + scaled(Math.floorMod(seed / (i + 3) + i * 11, 13) - 6);
            int sparkY = baseY - (int) Math.round(life * size * 0.82);
            int sparkSize = Math.max(scaled(2), scaled(4) - (int) Math.round(life * scaled(2)));
            float alpha = (float) ((1.0 - life) * 0.46);
            g.setComposite(AlphaComposite.SrcOver.derive(alpha));
            g.setColor(new Color(255, 214, 112));
            g.fillOval(sparkX - sparkSize / 2, sparkY - sparkSize / 2, sparkSize, sparkSize);
        }
        g.setComposite(oldComposite);
    }

    private void drawMagicPropAnimation(Graphics2D g, WorldProp prop, int x, int y, int size) {
        Color glow = propGlowColor(prop.asset());
        if (glow == null) {
            return;
        }
        int seed = Math.abs(prop.x() * 7349 + prop.y() * 9127 + prop.asset().hashCode());
        int cx = x + size / 2;
        int cy = y + size / 2;
        float visibility = lightVisibilityForAsset(prop.asset());
        float pulse = (float) (0.72 + Math.sin(frame * 0.075 + seed * 0.01) * 0.16);
        drawRadialGlow(g, cx, cy, Math.max(scaled(16), size * 2 / 3), glow, 0.09f * visibility * pulse);

        Composite oldComposite = g.getComposite();
        int motes = prop.asset().contains("firefly") ? 6 : 4;
        for (int i = 0; i < motes; i++) {
            double angle = frame * (0.032 + i * 0.004) + seed * 0.002 + i * 2.399;
            double rise = Math.sin(frame * 0.045 + i * 1.7 + seed * 0.004);
            int moteX = cx + (int) Math.round(Math.cos(angle) * size * 0.34);
            int moteY = cy + (int) Math.round(Math.sin(angle * 1.21) * size * 0.22 - rise * size * 0.10);
            int moteSize = Math.max(scaled(2), scaled(3 + Math.floorMod(seed + i, 3)));
            float alpha = (float) Math.min(0.58, (0.22 + nightFactor() * 0.28) * visibility * (0.76 + Math.sin(angle * 1.8) * 0.18));
            g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0.0f, alpha)));
            g.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 225));
            g.fillOval(moteX - moteSize / 2, moteY - moteSize / 2, moteSize, moteSize);
        }
        g.setComposite(oldComposite);
    }

    private void drawResourcePropParticles(Graphics2D g, WorldProp prop, int x, int y, int size) {
        int seed = Math.abs(prop.x() * 2213 + prop.y() * 4567 + prop.asset().hashCode());
        Color color = resourceParticleColor(prop.asset());
        int count = propParticleCount(prop.asset());
        Composite oldComposite = g.getComposite();
        for (int i = 0; i < count; i++) {
            double life = Math.floorMod(frame * 2 + seed + i * 29, 96) / 96.0;
            double drift = Math.sin(frame * 0.055 + seed * 0.003 + i * 2.1);
            int px = x + size / 2 + scaled(Math.floorMod(seed / (i + 5) + i * 17, 19) - 9)
                    + (int) Math.round(drift * size * 0.08);
            int py = y + (int) Math.round(size * (0.68 - life * 0.48))
                    + scaled(Math.floorMod(seed / (i + 7), 7) - 3);
            int mote = Math.max(scaled(2), scaled(4) - (int) Math.round(life * scaled(2)));
            float alpha = (float) ((1.0 - life) * (0.18 + nightFactor() * 0.20));
            g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0.0f, Math.min(0.42f, alpha))));
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 225));
            g.fillOval(px - mote / 2, py - mote / 2, mote, mote);
        }
        g.setComposite(oldComposite);
    }

    private int propParticleCount(String asset) {
        if (asset.contains("glowing_root") || asset.contains("glowroot") || asset.contains("magical_harvestable")
                || asset.contains("enchanted")) {
            return 5;
        }
        if (asset.contains("mithril") || asset.contains("cobalt") || asset.contains("adamantite")
                || asset.contains("gold") || asset.contains("silver")) {
            return 4;
        }
        return 3;
    }

    private Color resourceParticleColor(String asset) {
        if (asset.contains("gold")) {
            return new Color(255, 215, 106);
        }
        if (asset.contains("mithril") || asset.contains("silver")) {
            return new Color(186, 230, 255);
        }
        if (asset.contains("cobalt") || asset.contains("adamantite")) {
            return new Color(122, 190, 255);
        }
        if (asset.contains("glowroot") || asset.contains("glowing_root") || asset.contains("magical_harvestable")
                || asset.contains("enchanted")) {
            return new Color(139, 244, 168);
        }
        return new Color(244, 194, 122);
    }

    private void drawPropGroundBlend(Graphics2D g, int wx, int wy, int x, int y, int size, float opacity, boolean soft) {
        Color terrain = terrainColorAt(wx, wy);
        int pad = soft ? scaled(3) : scaled(1);
        int ovalW = Math.max(scaled(9), size - pad * 2);
        int ovalH = Math.max(scaled(4), soft ? size / 4 : size / 5);
        int ovalX = x + (size - ovalW) / 2;
        int ovalY = y + size - ovalH - scaled(3);

        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver.derive(opacity));
        g.setColor(terrain);
        g.fillOval(ovalX, ovalY, ovalW, ovalH);

        int seed = wx * 73471 + wy * 19349663 + size;
        int flecks = soft ? 4 : 3;
        for (int i = 0; i < flecks; i++) {
            int fx = ovalX + Math.floorMod(seed + i * 17, Math.max(1, ovalW));
            int fy = ovalY + Math.floorMod(seed / 7 + i * 11, Math.max(1, ovalH));
            int fw = Math.max(scaled(2), ovalW / (soft ? 5 : 6));
            int fh = Math.max(scaled(1), ovalH / 2);
            g.setColor(propBlendFleckColor(terrain, seed + i * 29));
            g.fillOval(fx - fw / 2, fy - fh / 2, fw, fh);
        }
        g.setComposite(oldComposite);
    }

    private void drawPropGroundVeil(Graphics2D g, int wx, int wy, int x, int y, int size, float opacity) {
        Color terrain = terrainColorAt(wx, wy);
        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver.derive(opacity));
        g.setColor(terrain);
        int veilH = Math.max(scaled(3), size / 5);
        g.fillOval(x + scaled(2), y + size - veilH - scaled(2), size - scaled(4), veilH);
        g.setComposite(oldComposite);
    }

    private Color propBlendFleckColor(Color terrain, int seed) {
        int shift = Math.floorMod(seed, 2) == 0 ? 18 : -16;
        return new Color(
                clampColor(terrain.getRed() + shift),
                clampColor(terrain.getGreen() + shift),
                clampColor(terrain.getBlue() + shift)
        );
    }

    private int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private Color terrainColorAt(int wx, int wy) {
        char tile = state.world.tileAt(state.currentMapId, wx, wy);
        return Terrain.color(visibleTerrainTile(tile, wx, wy));
    }

    private void drawQuestInteractibles(Graphics2D g, int camX, int camY) {
        int ts = tileSize();
        for (GameState.QuestInteractible interactible : state.activeQuestInteractibles(state.currentMapId)) {
            if (interactible.x() < camX || interactible.y() < camY || interactible.x() >= camX + lastVisibleCols || interactible.y() >= camY + lastVisibleRows) {
                continue;
            }
            int px = (interactible.x() - camX) * ts;
            int py = (interactible.y() - camY) * ts;
            boolean nearby = Math.abs(interactible.x() - state.playerX) + Math.abs(interactible.y() - state.playerY) <= 1;
            Color color = new Color(112, 220, 128, nearby ? 210 : 126);
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), nearby ? 54 : 28));
            g.fillOval(px + scaled(3), py + scaled(31), ts - scaled(6), scaled(15));
            g.setStroke(new BasicStroke(nearby ? scaledStroke(2.2f) : scaledStroke(1.4f)));
            g.setColor(color);
            g.drawOval(px + scaled(3), py + scaled(31), ts - scaled(6), scaled(15));
            int corner = scaled(11);
            int inset = scaled(5);
            g.drawLine(px + inset, py + inset, px + inset + corner, py + inset);
            g.drawLine(px + inset, py + inset, px + inset, py + inset + corner);
            g.drawLine(px + ts - inset, py + inset, px + ts - inset - corner, py + inset);
            g.drawLine(px + ts - inset, py + inset, px + ts - inset, py + inset + corner);
            g.setStroke(new BasicStroke(1f));
            if (nearby) {
                int glint = (frame / 10) % 3;
                g.setColor(new Color(255, 245, 174, 210));
                g.fillOval(px + scaled(21 + glint * 3), py + scaled(9 - glint), scaled(6), scaled(6));
            }
        }
    }

    private void drawQuestObjectives(Graphics2D g, int camX, int camY) {
        int ts = tileSize();
        for (GameState.QuestObjective objective : state.activeQuestObjectives()) {
            if (!showQuestObjective(objective)) {
                continue;
            }
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
            Color ring = objectiveColor(objective.kind(), 220);
            drawObjectiveGroundMarker(g, px, py, ts, ring);

            if (objective.kind().combatObjective()) {
                int width = scaled(42);
                int height = scaled(54);
                drawShadow(g, px + scaled(8), py + scaled(38), scaled(34), scaled(9));
                drawCharacterSprite(g, objective.asset(), px + (ts - width) / 2, py - scaled(9) + pulse, width, height, 0, 1, (frame / 8) % WALK_ANIMATION_FRAMES);
            } else {
                int size = scaled(42);
                int drawX = px + (ts - size) / 2;
                int drawY = py + ts - size - scaled(3);
                g.drawImage(assets.spriteFit(objective.asset(), size, size), drawX, drawY, null);
            }

            drawObjectivePin(g, px + ts / 2, py + scaled(2) - pulse, objective.kind());
        }
    }

    private boolean showQuestObjective(GameState.QuestObjective objective) {
        Quest quest = state.quests.get(objective.questId());
        return quest != null && quest.accepted && !quest.completed && !quest.ready();
    }

    private void drawGatherTargetHighlight(Graphics2D g, int camX, int camY, int tileSize) {
        if (state.mode != GameMode.EXPLORE || state.crafting.active()) {
            return;
        }
        TilePoint tile = highlightedWorldTile();
        if (tile == null) {
            return;
        }
        if (tile.x() < camX || tile.y() < camY || tile.x() >= camX + lastVisibleCols || tile.y() >= camY + lastVisibleRows) {
            return;
        }
        CraftingSystem.GatherCandidate target = state.gatherTargetAtTile(tile.x(), tile.y());
        Npc npc = state.npcAt(state.currentMapId, tile.x(), tile.y());
        int px = (tile.x() - camX) * tileSize;
        int py = (tile.y() - camY) * tileSize;
        Color color = npc != null ? new Color(242, 208, 101, 235)
                : target != null ? gatherTargetColor(target)
                : new Color(94, 188, 238, 185);
        int pulse = (int) Math.round(Math.sin(frame * 0.18) * scaled(2));
        Composite oldComposite = g.getComposite();
        Stroke oldStroke = g.getStroke();

        drawObjectiveGroundMarker(g, px, py, tileSize, color);
        g.setComposite(AlphaComposite.SrcOver.derive(0.22f));
        g.setColor(color);
        g.fillRoundRect(px + scaled(5), py + scaled(5), tileSize - scaled(10), tileSize - scaled(10), scaled(8), scaled(8));
        g.setComposite(AlphaComposite.SrcOver.derive(0.92f));
        g.setStroke(new BasicStroke(scaledStroke(2.4f)));
        g.drawRoundRect(px + scaled(4), py + scaled(4) - pulse, tileSize - scaled(8), tileSize - scaled(8), scaled(8), scaled(8));
        g.setColor(new Color(255, 250, 205, 230));
        g.setStroke(new BasicStroke(scaledStroke(1.2f)));
        g.drawRoundRect(px + scaled(9), py + scaled(9) - pulse, tileSize - scaled(18), tileSize - scaled(18), scaled(6), scaled(6));
        if (npc != null) {
            g.setComposite(AlphaComposite.SrcOver.derive(0.86f));
            g.setColor(new Color(255, 246, 176, 230));
            g.fillOval(px + tileSize / 2 - scaled(4), py + scaled(7) - pulse, scaled(8), scaled(8));
        }

        g.setStroke(oldStroke);
        g.setComposite(oldComposite);
    }

    private TilePoint highlightedWorldTile() {
        return contextMenuTile != null ? contextMenuTile : hoverWorldTileInViewport();
    }

    private Color gatherTargetColor(CraftingSystem.GatherCandidate target) {
        String asset = target.asset() == null ? "" : target.asset().toLowerCase();
        String label = target.label() == null ? "" : target.label().toLowerCase();
        if (target.terrain() == 'm' || target.terrain() == 'q' || isOreResourceAsset(asset)
                || asset.contains("stone") || asset.contains("rock") || asset.contains("crystal")) {
            return new Color(238, 174, 86, 230);
        }
        if (target.terrain() == 'w' || label.contains("water") || label.contains("shell")) {
            return new Color(94, 188, 238, 230);
        }
        if (target.terrain() == 'f' || asset.contains("tree") || asset.contains("wood") || asset.contains("log")
                || label.contains("wood") || label.contains("trees")) {
            return new Color(121, 222, 126, 230);
        }
        return new Color(144, 224, 159, 230);
    }

    private boolean isOreResourceAsset(String asset) {
        return asset.contains("iron_vein")
                || asset.contains("copper_vein")
                || asset.contains("coal_deposit")
                || asset.contains("tin_vein")
                || asset.contains("silver_vein")
                || asset.contains("gold_vein")
                || asset.contains("mithril_vein")
                || asset.contains("mythril_vein")
                || asset.contains("cobalt_vein")
                || asset.contains("adamantite_vein")
                || asset.contains("crystal_vein")
                || asset.contains("steel_scrap");
    }

    private static final int CONTEXT_MENU_MARGIN = 8;
    private static final int CONTEXT_MENU_RADIUS = 8;
    private static final int CONTEXT_MENU_PADDING_X = 8;
    private static final int CONTEXT_MENU_TITLE_X = 10;
    private static final int CONTEXT_MENU_TITLE_Y = 18;
    private static final int CONTEXT_MENU_BUTTON_TOP = 28;
    private static final int CONTEXT_MENU_BUTTON_HEIGHT = 26;
    private static final int CONTEXT_MENU_ROW_STEP = 30;

    private void drawWorldContextMenu(Graphics2D g) {
        boolean partyOverlayMenu = (state.mode == GameMode.EXPLORE || state.mode == GameMode.PARTY || state.mode == GameMode.SKILLS)
                && contextMenuPartyMember != null
                && contextMenuTile == null
                && contextMenuSettlement == null
                && contextMenuBuilding == null;
        if (contextMenuPoint == null
                || (!partyOverlayMenu && state.mode != GameMode.EXPLORE)
                || (!partyOverlayMenu && contextMenuTile == null && contextMenuSettlement == null && contextMenuBuilding == null)) {
            return;
        }
        Rectangle bounds = contextMenuBounds();
        CraftingSystem.GatherCandidate target = contextMenuTile != null && contextMenuSettlement == null && contextMenuBuilding == null
                ? state.gatherTargetAtTile(contextMenuTile.x(), contextMenuTile.y())
                : null;
        Actor partyMember = contextMenuSettlement == null && contextMenuBuilding == null ? contextMenuPartyMember : null;
        Npc npc = contextMenuTile != null && contextMenuSettlement == null && contextMenuBuilding == null
                && partyMember == null
                ? state.npcAt(state.currentMapId, contextMenuTile.x(), contextMenuTile.y())
                : null;
        WorldProp portal = contextMenuTile != null && contextMenuSettlement == null && contextMenuBuilding == null
                ? townPortalNearTile(contextMenuTile)
                : null;
        WorldProp bookshelf = contextMenuTile != null && contextMenuSettlement == null && contextMenuBuilding == null
                ? state.readableBookshelfAtTile(contextMenuTile.x(), contextMenuTile.y())
                : null;
        CraftingSystem.Workstation workstation = contextMenuTile != null && contextMenuSettlement == null && contextMenuBuilding == null
                ? state.workstationAtTile(contextMenuTile.x(), contextMenuTile.y())
                : null;
        Composite oldComposite = g.getComposite();
        Stroke oldStroke = g.getStroke();
        Font oldFont = g.getFont();

        g.setComposite(AlphaComposite.SrcOver.derive(0.92f));
        g.setColor(new Color(12, 16, 22, 238));
        g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, CONTEXT_MENU_RADIUS, CONTEXT_MENU_RADIUS);
        g.setColor(new Color(220, 210, 170, 220));
        g.setStroke(new BasicStroke(1.4f));
        g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, CONTEXT_MENU_RADIUS, CONTEXT_MENU_RADIUS);

        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(new Color(241, 236, 216));
        String title = contextMenuSettlement != null
                ? contextMenuSettlement.label()
                : contextMenuBuilding != null ? state.generatedBuildingName(contextMenuBuilding)
                : partyMember != null ? partyMember.name
                : npc != null ? state.npcDisplayName(npc)
                : portal != null ? "Town Portal"
                : bookshelf != null ? "Bookshelf"
                : workstation != null ? workstation.label()
                : target == null ? "Location" : "Gather " + target.label();
        drawClippedString(g, title, bounds.x + CONTEXT_MENU_TITLE_X, bounds.y + CONTEXT_MENU_TITLE_Y,
                bounds.width - CONTEXT_MENU_TITLE_X * 2);

        int buttonY = bounds.y + CONTEXT_MENU_BUTTON_TOP;
        if (partyOverlayMenu) {
            Actor selected = contextMenuPartyMember;
            if (state.activeAllies().contains(selected)) {
                actionButton(g, bounds.x + CONTEXT_MENU_PADDING_X, buttonY,
                        bounds.width - CONTEXT_MENU_PADDING_X * 2, CONTEXT_MENU_BUTTON_HEIGHT,
                        "Talk to " + selected.name, () -> {
                            clearContextMenu();
                            if (!state.talkToPartyAlly(selected)) {
                                state.status = selected.name + " is not ready to talk right now.";
                            }
                        }, new Color(52, 56, 78), new Color(126, 154, 220), true);
                buttonY += CONTEXT_MENU_ROW_STEP;
            }
            actionButton(g, bounds.x + CONTEXT_MENU_PADDING_X, buttonY,
                    bounds.width - CONTEXT_MENU_PADDING_X * 2, CONTEXT_MENU_BUTTON_HEIGHT,
                    "Character", () -> openCharacterForPartyMember(selected),
                    new Color(46, 53, 42), new Color(122, 164, 96), true);
            g.setFont(oldFont);
            g.setStroke(oldStroke);
            g.setComposite(oldComposite);
            return;
        }
        if (contextMenuSettlement != null) {
            actionButton(g, bounds.x + CONTEXT_MENU_PADDING_X, buttonY,
                    bounds.width - CONTEXT_MENU_PADDING_X * 2, CONTEXT_MENU_BUTTON_HEIGHT,
                    "Enter " + contextMenuSettlement.label(), () -> {
                        WorldMap.SettlementSite settlement = contextMenuSettlement;
                        clearContextMenu();
                        pendingGatherTile = null;
                        pendingTalkNpc = null;
                        pendingTalkPartyMember = null;
                        pendingTalkPartyTile = null;
                        pendingEnterBuilding = null;
                        pendingPortalTile = null;
                        startNavigateToSettlement(settlement);
                    }, new Color(50, 44, 70), new Color(136, 118, 190), true);
            g.setFont(oldFont);
            g.setStroke(oldStroke);
            g.setComposite(oldComposite);
            return;
        }
        if (contextMenuBuilding != null) {
            if (contextMenuTile != null) {
                actionButton(g, bounds.x + CONTEXT_MENU_PADDING_X, buttonY,
                        bounds.width - CONTEXT_MENU_PADDING_X * 2, CONTEXT_MENU_BUTTON_HEIGHT,
                        "Go to tile", () -> {
                            TilePoint tile = contextMenuTile;
                            clearContextMenu();
                            pendingGatherTile = null;
                            pendingTalkNpc = null;
                            pendingTalkPartyMember = null;
                            pendingTalkPartyTile = null;
                            pendingEnterBuilding = null;
                            pendingPortalTile = null;
                            setPlayerPathDestination(tile.x(), tile.y());
                        }, new Color(35, 39, 54), new Color(86, 98, 128), true);
                buttonY += CONTEXT_MENU_ROW_STEP;
            }
            actionButton(g, bounds.x + CONTEXT_MENU_PADDING_X, buttonY,
                    bounds.width - CONTEXT_MENU_PADDING_X * 2, CONTEXT_MENU_BUTTON_HEIGHT,
                    "Enter " + state.generatedBuildingName(contextMenuBuilding), () -> {
                        CityBuilding building = contextMenuBuilding;
                        clearContextMenu();
                        pendingGatherTile = null;
                        pendingTalkNpc = null;
                        pendingPortalTile = null;
                        startEnterBuilding(building);
                    }, new Color(46, 53, 42), new Color(122, 164, 96), true);
            buttonY += CONTEXT_MENU_ROW_STEP;
            actionButton(g, bounds.x + CONTEXT_MENU_PADDING_X, buttonY,
                    bounds.width - CONTEXT_MENU_PADDING_X * 2, CONTEXT_MENU_BUTTON_HEIGHT,
                    "Inspect", () -> {
                        CityBuilding building = contextMenuBuilding;
                        String mapId = state.currentMapId;
                        clearContextMenu();
                        pendingGatherTile = null;
                        pendingTalkNpc = null;
                        pendingPortalTile = null;
                        state.inspectBuilding(mapId, building);
                    }, new Color(54, 48, 67), new Color(148, 127, 190), true);
            g.setFont(oldFont);
            g.setStroke(oldStroke);
            g.setComposite(oldComposite);
            return;
        }
        actionButton(g, bounds.x + CONTEXT_MENU_PADDING_X, buttonY,
                bounds.width - CONTEXT_MENU_PADDING_X * 2, CONTEXT_MENU_BUTTON_HEIGHT,
                portal == null ? "Go to location" : "Use portal", () -> {
                    TilePoint tile = contextMenuTile;
                    WorldProp selectedPortal = portal;
                    clearContextMenu();
                    pendingGatherTile = null;
                    pendingTalkNpc = null;
                    pendingTalkPartyMember = null;
                    pendingTalkPartyTile = null;
                    pendingEnterBuilding = null;
                    if (selectedPortal == null) {
                        pendingPortalTile = null;
                        setPlayerPathDestination(tile.x(), tile.y());
                    } else {
                        startUsePortal(selectedPortal);
                    }
                }, new Color(35, 39, 54), new Color(86, 98, 128), true);
        buttonY += CONTEXT_MENU_ROW_STEP;
        if (partyMember != null) {
            actionButton(g, bounds.x + CONTEXT_MENU_PADDING_X, buttonY,
                    bounds.width - CONTEXT_MENU_PADDING_X * 2, CONTEXT_MENU_BUTTON_HEIGHT,
                    "Talk to " + partyMember.name, () -> {
                        Actor selected = partyMember;
                        TilePoint selectedTile = contextMenuTile;
                        clearContextMenu();
                        pendingGatherTile = null;
                        pendingEnterBuilding = null;
                        pendingPortalTile = null;
                        pendingReadTile = null;
                        pendingCraftTile = null;
                        startTalkToPartyMember(selected, selectedTile);
                    }, new Color(52, 56, 78), new Color(126, 154, 220), true);
            buttonY += CONTEXT_MENU_ROW_STEP;
            actionButton(g, bounds.x + CONTEXT_MENU_PADDING_X, buttonY,
                    bounds.width - CONTEXT_MENU_PADDING_X * 2, CONTEXT_MENU_BUTTON_HEIGHT,
                    "Character", () -> openCharacterForPartyMember(partyMember),
                    new Color(46, 53, 42), new Color(122, 164, 96), true);
            buttonY += CONTEXT_MENU_ROW_STEP;
        }
        if (npc != null) {
            actionButton(g, bounds.x + CONTEXT_MENU_PADDING_X, buttonY,
                    bounds.width - CONTEXT_MENU_PADDING_X * 2, CONTEXT_MENU_BUTTON_HEIGHT,
                    "Talk to " + state.npcDisplayName(npc), () -> {
                        Npc selectedNpc = npc;
                        clearContextMenu();
                        pendingEnterBuilding = null;
                        pendingPortalTile = null;
                        pendingReadTile = null;
                        pendingCraftTile = null;
                        startTalkToNpc(selectedNpc);
                    }, new Color(62, 50, 31), new Color(174, 135, 66), true);
            buttonY += CONTEXT_MENU_ROW_STEP;
        }
        if (bookshelf != null) {
            actionButton(g, bounds.x + CONTEXT_MENU_PADDING_X, buttonY,
                    bounds.width - CONTEXT_MENU_PADDING_X * 2, CONTEXT_MENU_BUTTON_HEIGHT,
                    "Read", () -> {
                        TilePoint tile = contextMenuTile;
                        clearContextMenu();
                        pendingEnterBuilding = null;
                        pendingPortalTile = null;
                        pendingCraftTile = null;
                        startReadAtLocation(tile);
                    }, new Color(50, 44, 70), new Color(136, 118, 190), true);
            buttonY += CONTEXT_MENU_ROW_STEP;
        }
        if (workstation != null) {
            actionButton(g, bounds.x + CONTEXT_MENU_PADDING_X, buttonY,
                    bounds.width - CONTEXT_MENU_PADDING_X * 2, CONTEXT_MENU_BUTTON_HEIGHT,
                    "Craft at " + workstation.label(), () -> {
                        TilePoint tile = contextMenuTile;
                        clearContextMenu();
                        pendingEnterBuilding = null;
                        pendingPortalTile = null;
                        pendingReadTile = null;
                        startCraftAtLocation(tile);
                    }, new Color(46, 53, 42), new Color(122, 164, 96), true);
            buttonY += CONTEXT_MENU_ROW_STEP;
        }
        actionButton(g, bounds.x + CONTEXT_MENU_PADDING_X, buttonY,
                bounds.width - CONTEXT_MENU_PADDING_X * 2, CONTEXT_MENU_BUTTON_HEIGHT,
                target == null ? "Gather here" : "Gather " + target.label(), () -> {
                    TilePoint tile = contextMenuTile;
                    clearContextMenu();
                    pendingEnterBuilding = null;
                    pendingPortalTile = null;
                    pendingReadTile = null;
                    pendingCraftTile = null;
                    startGatherAtLocation(tile);
                }, new Color(55, 70, 43), new Color(118, 156, 88), target != null);

        g.setFont(oldFont);
        g.setStroke(oldStroke);
        g.setComposite(oldComposite);
    }

    private Rectangle contextMenuBounds() {
        boolean hasBookshelf = contextMenuSettlement == null && contextMenuBuilding == null && contextMenuTile != null
                && state.readableBookshelfAtTile(contextMenuTile.x(), contextMenuTile.y()) != null;
        CraftingSystem.Workstation workstation = contextMenuSettlement == null && contextMenuBuilding == null && contextMenuTile != null
                ? state.workstationAtTile(contextMenuTile.x(), contextMenuTile.y())
                : null;
        boolean hasPartyMember = contextMenuSettlement == null && contextMenuBuilding == null && contextMenuPartyMember != null;
        boolean partyOverlayMenu = contextMenuTile == null && hasPartyMember;
        boolean partyMemberCanTalk = hasPartyMember && state.activeAllies().contains(contextMenuPartyMember);
        boolean hasNpc = contextMenuSettlement == null && contextMenuBuilding == null && contextMenuTile != null
                && contextMenuPartyMember == null
                && state.npcAt(state.currentMapId, contextMenuTile.x(), contextMenuTile.y()) != null;
        int width = contextMenuSettlement != null ? 252
                : contextMenuBuilding != null ? 238
                : workstation != null || hasPartyMember || hasNpc ? 216
                : 174;
        int actionCount = partyOverlayMenu
                ? 1 + (partyMemberCanTalk ? 1 : 0)
                : contextMenuSettlement != null ? 1 : contextMenuBuilding != null ? 2 + (contextMenuTile == null ? 0 : 1) : 2;
        if (hasPartyMember && contextMenuTile != null) {
            actionCount += 2;
        }
        if (hasNpc) {
            actionCount++;
        }
        if (hasBookshelf) {
            actionCount++;
        }
        if (workstation != null) {
            actionCount++;
        }
        int height = 32 + actionCount * CONTEXT_MENU_ROW_STEP;
        int x = contextMenuPoint == null ? 16 : contextMenuPoint.x + CONTEXT_MENU_MARGIN;
        int y = contextMenuPoint == null ? 16 : contextMenuPoint.y + CONTEXT_MENU_MARGIN;
        int clampW = state.mode == GameMode.EXPLORE && !partyOverlayMenu ? gameAreaWidth() : viewWidth();
        int clampH = state.mode == GameMode.EXPLORE && !partyOverlayMenu ? worldViewportHeight() : viewHeight();
        x = clamp(x, CONTEXT_MENU_MARGIN, Math.max(CONTEXT_MENU_MARGIN, clampW - width - CONTEXT_MENU_MARGIN));
        y = clamp(y, CONTEXT_MENU_MARGIN, Math.max(CONTEXT_MENU_MARGIN, clampH - height - CONTEXT_MENU_MARGIN));
        return new Rectangle(x, y, width, height);
    }

    private void openCharacterForPartyMember(Actor actor) {
        if (actor == null) {
            clearContextMenu();
            return;
        }
        List<Actor> members = state.partyMembers();
        int index = members.indexOf(actor);
        if (index < 0) {
            clearContextMenu();
            state.status = actor.name + " is not in the current party.";
            return;
        }
        clearContextMenu();
        state.selectPartyScreenActor(index);
        partySkillScroll = 0;
        partyLoadoutScroll = 0;
        if (state.mode != GameMode.PARTY) {
            state.mode = GameMode.PARTY;
        }
        repaint();
    }

    private void startNavigateToSettlement(WorldMap.SettlementSite settlement) {
        if (settlement == null || !WorldMap.OVERWORLD_ID.equals(state.currentMapId)) {
            return;
        }
        state.status = "Heading for " + settlement.label() + ".";
        setPlayerPathDestination(settlement.x(), settlement.y());
    }

    private void startEnterBuilding(CityBuilding building) {
        if (building == null || !isSettlementMapKind(state.currentMapId)) {
            return;
        }
        TilePoint target = nearestBuildingDoorApproach(building);
        if (target == null) {
            state.status = "No walkable door to " + state.generatedBuildingName(building) + ".";
            return;
        }
        pendingEnterBuilding = building;
        pendingGatherTile = null;
        pendingReadTile = null;
        pendingCraftTile = null;
        pendingTalkNpc = null;
        pendingTalkPartyMember = null;
        pendingTalkPartyTile = null;
        pendingPortalTile = null;
        if (target.x() == state.playerX && target.y() == state.playerY) {
            tryStartPendingEnterBuilding();
            return;
        }
        List<TilePoint> path = Pathfinder.findPath(state, new TilePoint(state.playerX, state.playerY), target);
        if (path.isEmpty()) {
            pendingEnterBuilding = null;
            clearPlayerPath();
            state.status = "No walkable route to " + state.generatedBuildingName(building) + ".";
            return;
        }
        setPlayerPath(path, target);
        tickPlayerPath();
    }

    private TilePoint nearestBuildingDoorApproach(CityBuilding building) {
        TilePoint start = new TilePoint(state.playerX, state.playerY);
        TilePoint best = null;
        int bestPathLength = Integer.MAX_VALUE;
        int bestDistance = Integer.MAX_VALUE;
        for (TilePoint door : state.world.cityBuildingDoorTiles(building)) {
            TilePoint approach = new TilePoint(door.x(), door.y() + 1);
            if (!Pathfinder.walkable(state, approach.x(), approach.y())) {
                continue;
            }
            List<TilePoint> path = approach.equals(start) ? List.of() : Pathfinder.findPath(state, start, approach);
            if (!approach.equals(start) && path.isEmpty()) {
                continue;
            }
            int pathLength = path.size();
            int distance = Math.abs(approach.x() - state.playerX) + Math.abs(approach.y() - state.playerY);
            if (pathLength < bestPathLength || (pathLength == bestPathLength && distance < bestDistance)) {
                best = approach;
                bestPathLength = pathLength;
                bestDistance = distance;
            }
        }
        return best;
    }

    private void startGatherAtLocation(TilePoint tile) {
        if (tile == null) {
            return;
        }
        pendingTalkNpc = null;
        pendingTalkPartyMember = null;
        pendingTalkPartyTile = null;
        pendingEnterBuilding = null;
        pendingPortalTile = null;
        pendingReadTile = null;
        pendingCraftTile = null;
        int distance = Math.abs(tile.x() - state.playerX) + Math.abs(tile.y() - state.playerY);
        if (distance <= 1) {
            pendingGatherTile = null;
            state.gatherAtTile(tile.x(), tile.y());
            return;
        }
        pendingGatherTile = tile;
        setPlayerPathDestination(tile.x(), tile.y());
        if (playerPathDestination == null && Math.abs(tile.x() - state.playerX) + Math.abs(tile.y() - state.playerY) > 1) {
            pendingGatherTile = null;
        }
    }

    private void startReadAtLocation(TilePoint tile) {
        if (tile == null) {
            return;
        }
        pendingGatherTile = null;
        pendingTalkNpc = null;
        pendingTalkPartyMember = null;
        pendingTalkPartyTile = null;
        pendingEnterBuilding = null;
        pendingPortalTile = null;
        pendingCraftTile = null;
        int distance = Math.abs(tile.x() - state.playerX) + Math.abs(tile.y() - state.playerY);
        if (distance <= 1) {
            pendingReadTile = null;
            state.readBookshelfAtTile(tile.x(), tile.y());
            return;
        }
        pendingReadTile = tile;
        setPlayerPathDestination(tile.x(), tile.y());
        if (playerPathDestination == null && Math.abs(tile.x() - state.playerX) + Math.abs(tile.y() - state.playerY) > 1) {
            pendingReadTile = null;
        }
    }

    private void startCraftAtLocation(TilePoint tile) {
        if (tile == null) {
            return;
        }
        pendingGatherTile = null;
        pendingTalkNpc = null;
        pendingTalkPartyMember = null;
        pendingTalkPartyTile = null;
        pendingEnterBuilding = null;
        pendingPortalTile = null;
        pendingReadTile = null;
        int distance = Math.abs(tile.x() - state.playerX) + Math.abs(tile.y() - state.playerY);
        if (distance <= 1) {
            pendingCraftTile = null;
            state.openCraftingAtTile(tile.x(), tile.y());
            return;
        }
        pendingCraftTile = tile;
        setPlayerPathDestination(tile.x(), tile.y());
        if (playerPathDestination == null && Math.abs(tile.x() - state.playerX) + Math.abs(tile.y() - state.playerY) > 1) {
            pendingCraftTile = null;
        }
    }

    private void startUsePortal(WorldProp portal) {
        if (portal == null || state.mode != GameMode.EXPLORE) {
            return;
        }
        pendingGatherTile = null;
        pendingReadTile = null;
        pendingCraftTile = null;
        pendingTalkNpc = null;
        pendingTalkPartyMember = null;
        pendingTalkPartyTile = null;
        pendingEnterBuilding = null;
        TilePoint portalTile = new TilePoint(portal.x(), portal.y());
        if (state.townPortalNearPlayer() != null) {
            pendingPortalTile = null;
            clearPlayerPath();
            state.openFastTravel();
            return;
        }
        TilePoint target = nearestTownPortalApproach(portal);
        if (target == null) {
            pendingPortalTile = null;
            clearPlayerPath();
            state.status = "No walkable route to the town portal.";
            return;
        }
        List<TilePoint> path = target.equals(new TilePoint(state.playerX, state.playerY))
                ? List.of()
                : Pathfinder.findPath(state, new TilePoint(state.playerX, state.playerY), target);
        if (!target.equals(new TilePoint(state.playerX, state.playerY)) && path.isEmpty()) {
            pendingPortalTile = null;
            clearPlayerPath();
            state.status = "No walkable route to the town portal.";
            return;
        }
        pendingPortalTile = portalTile;
        setPlayerPath(path, target);
        tickPlayerPath();
    }

    private void startTalkToPartyMember(Actor ally, TilePoint companionTile) {
        if (ally == null || state.mode != GameMode.EXPLORE) {
            return;
        }
        pendingGatherTile = null;
        pendingReadTile = null;
        pendingCraftTile = null;
        pendingEnterBuilding = null;
        pendingPortalTile = null;
        pendingTalkNpc = null;
        pendingTalkPartyMember = null;
        pendingTalkPartyTile = null;
        TilePoint partyTile = companionTile == null ? partyFollowerTile(ally) : companionTile;
        if (partyTile == null) {
            pendingTalkPartyMember = null;
            pendingTalkPartyTile = null;
            clearPlayerPath();
            state.status = ally.name + " is not close enough to talk.";
            return;
        }
        TilePoint target = nearestPartyTalkTile(partyTile);
        if (target == null) {
            pendingTalkPartyMember = null;
            pendingTalkPartyTile = null;
            clearPlayerPath();
            state.status = "No walkable route to " + ally.name + ".";
            return;
        }
        TilePoint start = new TilePoint(state.playerX, state.playerY);
        List<TilePoint> path = target.equals(start) ? List.of() : Pathfinder.findPath(state, start, target);
        if (!target.equals(start) && path.isEmpty()) {
            pendingTalkPartyMember = null;
            pendingTalkPartyTile = null;
            clearPlayerPath();
            state.status = "No walkable route to " + ally.name + ".";
            return;
        }
        pendingTalkPartyMember = ally;
        pendingTalkPartyTile = partyTile;
        setPlayerPath(path, target);
        tickPlayerPath();
    }

    private TilePoint partyFollowerTile(Actor ally) {
        if (ally == null) {
            return null;
        }
        for (PartyFollowerRender follower : visiblePartyFollowers) {
            if (follower.actor() == ally) {
                return follower.tile();
            }
        }
        return null;
    }

    private TilePoint nearestPartyTalkTile(TilePoint partyTile) {
        if (partyTile == null) {
            return null;
        }
        TilePoint start = new TilePoint(state.playerX, state.playerY);
        TilePoint best = null;
        int bestPathLength = Integer.MAX_VALUE;
        int bestDistance = Integer.MAX_VALUE;
        int[][] directions = {{0, 1}, {1, 0}, {-1, 0}, {0, -1}};
        for (int[] direction : directions) {
            int x = partyTile.x() + direction[0];
            int y = partyTile.y() + direction[1];
            if (!Pathfinder.walkable(state, x, y)) {
                continue;
            }
            TilePoint candidate = new TilePoint(x, y);
            List<TilePoint> path = candidate.equals(start) ? List.of() : Pathfinder.findPath(state, start, candidate);
            if (!candidate.equals(start) && path.isEmpty()) {
                continue;
            }
            int pathLength = path.size();
            int distance = Math.abs(x - state.playerX) + Math.abs(y - state.playerY);
            if (pathLength < bestPathLength || (pathLength == bestPathLength && distance < bestDistance)) {
                best = candidate;
                bestPathLength = pathLength;
                bestDistance = distance;
            }
        }
        return best;
    }

    private TilePoint nearestTownPortalApproach(WorldProp portal) {
        TilePoint start = new TilePoint(state.playerX, state.playerY);
        TilePoint best = null;
        int bestPathLength = Integer.MAX_VALUE;
        int bestDistance = Integer.MAX_VALUE;
        int[][] candidates = {
                {0, 1}, {-1, 1}, {1, 1}, {0, 0}, {-1, 0}, {1, 0}, {0, -1}, {-1, -1}, {1, -1}
        };
        for (int[] candidate : candidates) {
            TilePoint target = new TilePoint(portal.x() + candidate[0], portal.y() + candidate[1]);
            if (!Pathfinder.walkable(state, target.x(), target.y())) {
                continue;
            }
            List<TilePoint> path = target.equals(start) ? List.of() : Pathfinder.findPath(state, start, target);
            if (!target.equals(start) && path.isEmpty()) {
                continue;
            }
            int pathLength = path.size();
            int distance = Math.max(Math.abs(portal.x() - target.x()), Math.abs(portal.y() - target.y()));
            if (pathLength < bestPathLength || (pathLength == bestPathLength && distance < bestDistance)) {
                best = target;
                bestPathLength = pathLength;
                bestDistance = distance;
            }
        }
        return best;
    }

    private WorldProp townPortalNearTile(TilePoint tile) {
        if (tile == null || !isSettlementMapKind(state.currentMapId)) {
            return null;
        }
        WorldProp best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (WorldProp prop : state.world.propsInBounds(state.currentMapId, tile.x() - 1, tile.y() - 1, tile.x() + 1, tile.y() + 1)) {
            if (!state.world.isTownPortalAsset(prop.asset())) {
                continue;
            }
            int distance = Math.max(Math.abs(prop.x() - tile.x()), Math.abs(prop.y() - tile.y()));
            if (distance <= 1 && distance < bestDistance) {
                best = prop;
                bestDistance = distance;
            }
        }
        return best;
    }

    private void startTalkToNpc(Npc npc) {
        if (npc == null || state.mode != GameMode.EXPLORE) {
            return;
        }
        pendingGatherTile = null;
        pendingReadTile = null;
        pendingCraftTile = null;
        pendingEnterBuilding = null;
        pendingPortalTile = null;
        pendingTalkPartyMember = null;
        pendingTalkPartyTile = null;
        if (state.talkToNpc(npc)) {
            pendingTalkNpc = null;
            clearPlayerPath();
            return;
        }
        TilePoint target = nearestTalkTile(npc);
        if (target == null) {
            pendingTalkNpc = null;
            state.status = "No walkable route to " + state.npcDisplayName(npc) + ".";
            return;
        }
        List<TilePoint> path = Pathfinder.findPath(state, new TilePoint(state.playerX, state.playerY), target);
        if (path.isEmpty() && (target.x() != state.playerX || target.y() != state.playerY)) {
            pendingTalkNpc = null;
            clearPlayerPath();
            state.status = "No walkable route to " + state.npcDisplayName(npc) + ".";
            return;
        }
        pendingTalkNpc = npc;
        setPlayerPath(path, target);
        tickPlayerPath();
    }

    private TilePoint nearestTalkTile(Npc npc) {
        if (npc == null || !npc.mapId().equals(state.currentMapId)) {
            return null;
        }
        TilePoint npcPosition = state.npcPosition(npc);
        TilePoint start = new TilePoint(state.playerX, state.playerY);
        TilePoint best = null;
        int bestPathLength = Integer.MAX_VALUE;
        int bestDistance = Integer.MAX_VALUE;
        int[][] directions = {{0, 1}, {1, 0}, {-1, 0}, {0, -1}};
        for (int[] direction : directions) {
            int x = npcPosition.x() + direction[0];
            int y = npcPosition.y() + direction[1];
            if (!Pathfinder.walkable(state, x, y)) {
                continue;
            }
            TilePoint candidate = new TilePoint(x, y);
            List<TilePoint> path = candidate.equals(start) ? List.of() : Pathfinder.findPath(state, start, candidate);
            if (!candidate.equals(start) && path.isEmpty()) {
                continue;
            }
            int pathLength = path.size();
            int distance = Math.abs(x - state.playerX) + Math.abs(y - state.playerY);
            if (pathLength < bestPathLength || (pathLength == bestPathLength && distance < bestDistance)) {
                best = candidate;
                bestPathLength = pathLength;
                bestDistance = distance;
            }
        }
        return best;
    }

    private void drawObjectiveGroundMarker(Graphics2D g, int px, int py, int ts, Color color) {
        Composite oldComposite = g.getComposite();
        Stroke oldStroke = g.getStroke();
        g.setComposite(AlphaComposite.SrcOver.derive(0.20f));
        g.setColor(color);
        g.fillOval(px + scaled(3), py + scaled(30), ts - scaled(6), scaled(17));
        g.setComposite(AlphaComposite.SrcOver.derive(0.86f));
        g.setStroke(new BasicStroke(scaledStroke(2.0f)));
        g.drawOval(px + scaled(3), py + scaled(30), ts - scaled(6), scaled(17));
        g.setComposite(AlphaComposite.SrcOver.derive(0.36f));
        g.setStroke(new BasicStroke(scaledStroke(1.0f)));
        g.setColor(new Color(255, 255, 255));
        g.drawOval(px + scaled(8), py + scaled(33), ts - scaled(16), scaled(10));
        g.setStroke(oldStroke);
        g.setComposite(oldComposite);
    }

    private void drawObjectivePin(Graphics2D g, int cx, int y, Quest.ObjectiveKind kind) {
        Color fill = objectiveColor(kind, 255);
        Color rim = new Color(246, 247, 232, 235);
        int r = scaled(9);
        g.setColor(new Color(0, 0, 0, 145));
        g.fillOval(cx - r - scaled(2), y - scaled(2), r * 2 + scaled(4), r * 2 + scaled(4));
        g.setColor(fill);
        g.fillOval(cx - r, y, r * 2, r * 2);
        Polygon tail = new Polygon(
                new int[]{cx - scaled(5), cx + scaled(5), cx},
                new int[]{y + r * 2 - scaled(2), y + r * 2 - scaled(2), y + r * 2 + scaled(7)},
                3
        );
        g.fillPolygon(tail);
        g.setColor(rim);
        g.setStroke(new BasicStroke(Math.max(1f, scaledStroke(1.2f))));
        g.drawOval(cx - r, y, r * 2, r * 2);
        g.drawPolygon(tail);
        g.setColor(new Color(22, 24, 28, 230));
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(8, scaled(10))));
        String glyph = switch (kind) {
            case GATHER -> "G";
            case DELIVER -> "D";
            case VISIT -> "V";
            case SEARCH -> "S";
            case TALK -> "T";
            case ASK_AROUND -> "?";
            case REPORT -> "R";
            case ESCORT -> "E";
            case RESCUE -> "!";
            case DEFEND -> "P";
            case CHOICE -> "C";
            case DEFEAT -> "B";
        };
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(glyph, cx - metrics.stringWidth(glyph) / 2, y + scaled(12));
        g.setStroke(new BasicStroke(1f));
    }

    private void drawNearbyQuestPrompt(Graphics2D g, int camX, int camY) {
        NearbyPrompt prompt = nearestQuestPrompt();
        if (prompt == null || prompt.x() < camX || prompt.y() < camY || prompt.x() >= camX + lastVisibleCols || prompt.y() >= camY + lastVisibleRows) {
            return;
        }
        int ts = tileSize();
        int px = (prompt.x() - camX) * ts;
        int py = (prompt.y() - camY) * ts;
        int bob = (int) Math.round(Math.sin(frame * 0.18) * scaled(2));
        Color color = objectiveColor(prompt.kind(), 255);
        int cx = px + ts / 2;
        int markerY = py - scaled(20) + bob;
        g.setColor(new Color(0, 0, 0, 150));
        g.fillOval(cx - scaled(17), markerY - scaled(9), scaled(34), scaled(18));
        g.setColor(color);
        g.fillOval(cx - scaled(13), markerY - scaled(13), scaled(26), scaled(26));
        g.setColor(new Color(245, 246, 236));
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        drawCenteredIn(g, "E", cx - scaled(13), markerY + scaled(5), scaled(26));

        String label = prompt.action() + " " + prompt.target();
        FontMetrics metrics = g.getFontMetrics();
        int w = Math.min(scaled(210), Math.max(scaled(92), metrics.stringWidth(label) + scaled(18)));
        int labelX = Math.max(4, Math.min(viewWidth() - w - 4, cx - w / 2));
        int labelY = markerY - scaled(42);
        g.setColor(new Color(8, 10, 16, 202));
        g.fillRoundRect(labelX, labelY, w, scaled(25), 8, 8);
        g.setColor(color);
        g.drawRoundRect(labelX, labelY, w, scaled(25), 8, 8);
        g.setColor(new Color(245, 246, 236));
        drawCenteredIn(g, label, labelX, labelY + scaled(17), w);
    }

    private NearbyPrompt nearestQuestPrompt() {
        NearbyPrompt best = null;
        int bestDistance = Integer.MAX_VALUE;
        WorldProp board = state.settlementBoardNearPlayer();
        if (board != null) {
            best = new NearbyPrompt(board.x(), board.y(), "Board", Quest.ObjectiveKind.VISIT, "Inspect");
            bestDistance = Math.abs(board.x() - state.playerX) + Math.abs(board.y() - state.playerY);
        }
        WorldProp storyPortal = state.storyPortalNearPlayer();
        if (storyPortal != null) {
            int distance = Math.max(Math.abs(storyPortal.x() - state.playerX), Math.abs(storyPortal.y() - state.playerY));
            if (distance < bestDistance) {
                best = new NearbyPrompt(storyPortal.x(), storyPortal.y(), "Old Gate of Alderfall", Quest.ObjectiveKind.VISIT, "Use");
                bestDistance = distance;
            }
        }
        WorldProp portal = state.townPortalNearPlayer();
        if (portal != null) {
            int distance = Math.max(Math.abs(portal.x() - state.playerX), Math.abs(portal.y() - state.playerY));
            if (distance < bestDistance) {
                best = new NearbyPrompt(portal.x(), portal.y(), "Portal", Quest.ObjectiveKind.VISIT, "Use");
                bestDistance = distance;
            }
        }
        CityBuilding entryBuilding = state.world.cityBuildingEntryAt(
                state.currentMapId, state.playerX, state.playerY - 1, state.playerX, state.playerY);
        if (entryBuilding != null && bestDistance > 1) {
            best = new NearbyPrompt(state.playerX, state.playerY - 1,
                    buildingPromptLabel(entryBuilding), Quest.ObjectiveKind.VISIT, "Enter");
            bestDistance = 1;
        }
        for (GameState.QuestInteractible interactible : state.activeQuestInteractibles(state.currentMapId)) {
            int distance = Math.abs(interactible.x() - state.playerX) + Math.abs(interactible.y() - state.playerY);
            if (distance <= 1 && distance < bestDistance) {
                best = new NearbyPrompt(interactible.x(), interactible.y(), interactible.target(), Quest.ObjectiveKind.GATHER, "Harvest");
                bestDistance = distance;
            }
        }
        for (GameState.QuestObjective objective : state.activeQuestObjectives()) {
            if (!objective.mapId().equals(state.currentMapId)) {
                continue;
            }
            int distance = Math.abs(objective.x() - state.playerX) + Math.abs(objective.y() - state.playerY);
            if (distance <= 1 && distance < bestDistance) {
                String action = switch (objective.kind()) {
                    case GATHER -> "Harvest";
                    case VISIT -> "Inspect";
                    case SEARCH -> "Search";
                    case ESCORT -> "Escort";
                    case RESCUE -> "Rescue";
                    case DEFEND -> "Defend";
                    case DELIVER -> "Deliver";
                    case TALK -> "Talk";
                    case ASK_AROUND -> "Ask";
                    case REPORT -> "Report";
                    case CHOICE -> "Decide";
                    case DEFEAT -> "Challenge";
                };
                best = new NearbyPrompt(objective.x(), objective.y(), objective.target(), objective.kind(), action);
                bestDistance = distance;
            }
        }
        return best;
    }

    private String buildingPromptLabel(CityBuilding building) {
        if (building == null || building.style() == null || building.style().isBlank()) {
            return "Building";
        }
        if (VillageManager.isManagedBuildingStyle(building.style())) {
            return VillageManager.buildingLabel(building.style());
        }
        return switch (building.style()) {
            case "arena" -> "Arena";
            case "mage_tower" -> "Mage Tower";
            case "bell_tower" -> "Bell Tower";
            case "sun_shrine" -> "Sun Shrine";
            case "river_hall" -> "River Hall";
            case "hall" -> "Hall";
            case "barracks" -> "Watchtower";
            case "warehouse" -> "Warehouse";
            case "guild" -> "Guildhall";
            case "inn" -> "Inn";
            case "shop" -> "Shop";
            case "row" -> "House";
            default -> "Building";
        };
    }

    private float scaledStroke(float value) {
        return Math.max(1f, (float) (value * tileSize() / (double) GameConfig.TILE));
    }

    private Color objectiveColor(Quest.ObjectiveKind kind, int alpha) {
        return switch (kind) {
            case GATHER -> new Color(112, 220, 128, alpha);
            case DELIVER -> new Color(228, 188, 91, alpha);
            case VISIT -> new Color(114, 191, 255, alpha);
            case SEARCH -> new Color(139, 214, 220, alpha);
            case TALK, ASK_AROUND, REPORT, CHOICE -> new Color(203, 157, 232, alpha);
            case ESCORT -> new Color(102, 205, 170, alpha);
            case RESCUE -> new Color(255, 139, 104, alpha);
            case DEFEND -> new Color(246, 172, 84, alpha);
            case DEFEAT -> new Color(233, 89, 83, alpha);
        };
    }

    private void drawPlayerPathMarker(Graphics2D g, int camX, int camY) {
        if (playerPathDestination == null || playerPathDestination.x() < camX || playerPathDestination.y() < camY
                || playerPathDestination.x() >= camX + lastVisibleCols || playerPathDestination.y() >= camY + lastVisibleRows) {
            return;
        }
        int ts = tileSize();
        int px = (playerPathDestination.x() - camX) * ts;
        int py = (playerPathDestination.y() - camY) * ts;
        double wave = (Math.sin(frame * 0.24) + 1.0) * 0.5;
        int centerX = px + ts / 2;
        int centerY = py + scaled(38);
        int outerW = ts - scaled(8);
        int outerH = scaled(18) + (int) Math.round(wave * scaled(4));
        int outerX = centerX - outerW / 2;
        int outerY = centerY - outerH / 2;
        int innerW = ts - scaled(18) + (int) Math.round(wave * scaled(4));
        int innerH = scaled(10) + (int) Math.round(wave * scaled(2));
        int innerX = centerX - innerW / 2;
        int innerY = centerY - innerH / 2;
        int dot = Math.max(scaled(3), scaled(4) + (int) Math.round(wave * scaled(2)));

        Composite oldComposite = g.getComposite();
        Stroke oldStroke = g.getStroke();
        g.setComposite(AlphaComposite.SrcOver.derive(0.48f));
        g.setColor(new Color(8, 12, 18));
        g.fillOval(outerX - scaled(1), outerY + scaled(2), outerW + scaled(2), outerH + scaled(2));
        g.setComposite(AlphaComposite.SrcOver.derive(0.48f));
        g.setColor(new Color(70, 215, 255));
        g.fillOval(outerX, outerY, outerW, outerH);
        g.setComposite(AlphaComposite.SrcOver.derive(0.42f));
        g.setColor(new Color(255, 229, 88));
        g.fillOval(innerX, innerY, innerW, innerH);
        g.setComposite(AlphaComposite.SrcOver.derive(0.86f));
        g.setStroke(new BasicStroke(Math.max(2f, scaledStroke(3.0f))));
        g.setColor(new Color(8, 12, 18));
        g.drawOval(outerX, outerY, outerW, outerH);
        g.setComposite(AlphaComposite.SrcOver.derive(0.98f));
        g.setStroke(new BasicStroke(Math.max(1.5f, scaledStroke(1.8f))));
        g.setColor(new Color(255, 246, 154));
        g.drawOval(outerX, outerY, outerW, outerH);
        g.setComposite(AlphaComposite.SrcOver.derive(0.90f));
        g.setColor(new Color(255, 252, 222));
        g.fillOval(centerX - dot / 2, centerY - dot / 2, dot, dot);
        g.setStroke(new BasicStroke(Math.max(1f, scaledStroke(1.0f))));
        g.setColor(new Color(24, 34, 46));
        g.drawOval(centerX - dot / 2, centerY - dot / 2, dot, dot);
        g.setStroke(oldStroke);
        g.setComposite(oldComposite);
    }

    private void drawPlayerTravelEffects(Graphics2D g, int playerPx, int playerPy) {
        if (!playerMoving()) {
            return;
        }
        double progress = moveProgress(frame - playerMoveStartFrame, PLAYER_MOVE_FRAMES);
        int driftX = -playerFacingDx * scaled(8);
        int driftY = -playerFacingDy * scaled(8);
        int dustX = playerPx + scaled(20) + driftX;
        int dustY = playerPy + scaled(42) + driftY;
        int width = scaled(10) + (int) Math.round(progress * scaled(8));
        int height = scaled(4);
        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver.derive((float) (0.28 * (1.0 - Math.abs(progress - 0.5)))));
        g.setColor(new Color(244, 223, 172));
        g.fillOval(dustX - width / 2, dustY - height / 2, width, height);
        int sideX = playerFacingDy * scaled(5);
        int sideY = -playerFacingDx * scaled(5);
        g.setComposite(AlphaComposite.SrcOver.derive((float) (0.16 * (1.0 - progress))));
        g.setColor(new Color(255, 246, 202));
        g.fillOval(dustX + sideX - scaled(4), dustY + sideY - scaled(2), scaled(8), scaled(4));
        g.fillOval(dustX - sideX - scaled(3), dustY - sideY - scaled(2), scaled(7), scaled(3));
        g.setComposite(oldComposite);
    }

    private void drawInteriorPlacementHover(Graphics2D g, int camX, int camY, int tileSize) {
        if (!showInteriorPlacementPreview()) {
            return;
        }
        TilePoint tile = hoverWorldTile();
        if (tile == null) {
            return;
        }
        String asset = interiorPlacementPreviewAsset();
        if (asset == null) {
            return;
        }
        int[] footprint = state.world.interiorPlacementSize(asset);
        boolean canPlace = state.world.canPlacePlayerInteriorProp(state.currentMapId, tile.x(), tile.y(), asset);
        int px = (tile.x() - camX) * tileSize;
        int py = (tile.y() - camY) * tileSize;
        int width = footprint[0] * tileSize;
        int height = footprint[1] * tileSize;
        Composite oldComposite = g.getComposite();
        Stroke oldStroke = g.getStroke();
        Color fill = canPlace ? new Color(72, 222, 111) : new Color(212, 72, 72);
        Color edge = canPlace ? new Color(154, 255, 165) : new Color(255, 151, 142);
        g.setComposite(AlphaComposite.SrcOver.derive(0.34f));
        g.setColor(fill);
        g.fillRoundRect(px + scaled(3), py + scaled(3), width - scaled(6), height - scaled(6), scaled(7), scaled(7));
        g.setComposite(AlphaComposite.SrcOver.derive(0.88f));
        g.setStroke(new BasicStroke(Math.max(2f, scaled(2))));
        g.setColor(edge);
        g.drawRoundRect(px + scaled(3), py + scaled(3), width - scaled(6), height - scaled(6), scaled(7), scaled(7));
        g.setStroke(oldStroke);
        g.setComposite(oldComposite);
    }

    private void drawInteriorPlacementPreview(Graphics2D g) {
        if (!showInteriorPlacementPreview() || hoverPoint == null) {
            return;
        }
        String asset = interiorPlacementPreviewAsset();
        if (asset == null) {
            return;
        }
        int baseSize = scaled(VillageManager.interiorAssetSize(asset));
        int drawW = interiorPropWidth(asset, baseSize);
        int drawH = interiorPropHeight(asset, baseSize);
        int bob = (int) Math.round(Math.sin(frame * 0.22) * scaled(2));
        int x = hoverPoint.x - drawW / 2;
        int y = hoverPoint.y - drawH / 2 + bob;
        Composite oldComposite = g.getComposite();
        Shape oldClip = g.getClip();
        g.setClip(0, 0, gameAreaWidth(), worldViewportHeight());
        drawShadow(g, x + drawW / 6, y + drawH - scaled(7), drawW * 2 / 3, scaled(7));
        drawPropImage(g, asset, x, y, drawW, drawH, 0.78f);
        g.setComposite(AlphaComposite.SrcOver.derive(0.70f));
        g.setColor(new Color(255, 255, 255, 90));
        g.drawRoundRect(x - scaled(3), y - scaled(3), drawW + scaled(6), drawH + scaled(6), scaled(8), scaled(8));
        g.setComposite(oldComposite);
        g.setClip(oldClip);
    }

    private void drawVillageTilePlacementPreview(Graphics2D g, int camX, int camY, int tileSize) {
        if (!showVillageTilePlacementPreview()) {
            return;
        }
        TilePoint tile = hoverWorldTile();
        if (tile == null) {
            return;
        }
        char previewTile = "delete".equals(state.villageEditAction) ? 'g' : state.selectedVillageTile;
        boolean canPlace = state.world.canSetPlayerVillageTile(tile.x(), tile.y(), previewTile);
        int px = (tile.x() - camX) * tileSize;
        int py = (tile.y() - camY) * tileSize;
        Composite oldComposite = g.getComposite();
        Stroke oldStroke = g.getStroke();
        g.setComposite(AlphaComposite.SrcOver.derive(canPlace ? 0.62f : 0.35f));
        g.drawImage(assets.tile(previewTile, tileSize), px, py, null);
        g.setComposite(AlphaComposite.SrcOver.derive(0.80f));
        g.setStroke(new BasicStroke(Math.max(2f, scaled(2))));
        g.setColor(canPlace ? new Color(154, 255, 165) : new Color(255, 151, 142));
        g.drawRoundRect(px + scaled(3), py + scaled(3), tileSize - scaled(6), tileSize - scaled(6), scaled(7), scaled(7));
        g.setStroke(oldStroke);
        g.setComposite(oldComposite);
    }

    private boolean showVillageTilePlacementPreview() {
        if (state.mode != GameMode.VILLAGE || !state.isManagedVillageMap() || state.isManagedVillageInterior() || state.villageTab != 1 || hoverPoint == null) {
            return false;
        }
        if (hoverPoint.x < 0 || hoverPoint.x >= gameAreaWidth() || hoverPoint.y < 0 || hoverPoint.y >= worldViewportHeight()) {
            return false;
        }
        return "place".equals(state.villageEditAction) || "delete".equals(state.villageEditAction);
    }

    private void drawVillagePropPlacementPreview(Graphics2D g, int camX, int camY, int tileSize) {
        if (!showVillagePropPlacementPreview()) {
            return;
        }
        TilePoint tile = hoverWorldTile();
        if (tile == null) {
            return;
        }
        WorldProp source = villagePropMoveSource();
        VillageManager.PlaceableAsset selected = VillageManager.outdoorAsset(state.selectedVillageAsset);
        String asset = source == null ? selected.asset() : source.asset();
        int propSize = source == null ? selected.size() : source.size();
        boolean canPlace = state.world.canPlacePlayerVillageProp(tile.x(), tile.y(), source);
        int size = propRenderSize(asset, propSize, tileSize);
        int drawW = interiorPropWidth(asset, size);
        int drawH = interiorPropHeight(asset, size);
        int px = (tile.x() - camX) * tileSize + (tileSize - drawW) / 2;
        int py = (tile.y() - camY) * tileSize + tileSize - drawH;
        Composite oldComposite = g.getComposite();
        Stroke oldStroke = g.getStroke();
        g.setComposite(AlphaComposite.SrcOver.derive(canPlace ? 0.62f : 0.42f));
        drawShadow(g, px + drawW / 6, py + drawH - scaled(7), drawW * 2 / 3, scaled(7));
        drawPropImage(g, asset, px, py, drawW, drawH, 1.0f);
        g.setComposite(AlphaComposite.SrcOver.derive(0.82f));
        g.setStroke(new BasicStroke(Math.max(2f, scaled(2))));
        g.setColor(canPlace ? new Color(154, 255, 165) : new Color(255, 151, 142));
        g.drawRoundRect(px + scaled(2), py + scaled(2), drawW - scaled(4), drawH - scaled(4), scaled(8), scaled(8));
        g.setStroke(oldStroke);
        g.setComposite(oldComposite);
    }

    private boolean showVillagePropPlacementPreview() {
        if (state.mode != GameMode.VILLAGE || !state.isManagedVillageMap() || state.isManagedVillageInterior() || state.villageTab != 2 || hoverPoint == null) {
            return false;
        }
        if (hoverPoint.x < 0 || hoverPoint.x >= gameAreaWidth() || hoverPoint.y < 0 || hoverPoint.y >= worldViewportHeight()) {
            return false;
        }
        return "place".equals(state.villageEditAction)
                || ("move".equals(state.villageEditAction) && villagePropMoveSource() != null);
    }

    private WorldProp villagePropMoveSource() {
        if (!"move".equals(state.villageEditAction) || state.pendingVillageMoveSource == null) {
            return null;
        }
        return state.world.playerVillagePropAt(state.pendingVillageMoveSource.x(), state.pendingVillageMoveSource.y());
    }

    private void drawVillageBuildingPlacementPreview(Graphics2D g, int camX, int camY, int tileSize) {
        if (!showVillageBuildingPlacementPreview()) {
            return;
        }
        TilePoint anchor = hoverWorldTile();
        if (anchor == null) {
            return;
        }
        CityBuilding source = villageBuildingMoveSource();
        String style = source == null ? state.selectedVillageBuildingStyle : source.style();
        VillageManager.BuildingPlan plan = VillageManager.buildingPlan(style);
        int width = source == null ? plan.width() : source.width();
        int depth = source == null ? plan.depth() : source.depth();
        boolean canPlace = state.world.canPlacePlayerVillageBuilding(anchor.x(), anchor.y(), width, depth, source);
        drawVillageBuildingFootprint(g, anchor.x(), anchor.y(), width, depth, camX, camY, tileSize, canPlace);
        CityBuilding preview = new CityBuilding("player_preview_" + style, anchor.x(), anchor.y(),
                anchor.x() + width - 1, anchor.y() + depth - 1, style, source == null ? 0 : source.palette());
        int level = source == null ? 1 : state.world.playerVillageBuildingLevel(source);
        drawVillageBuildingGhost(g, preview, camX, camY, tileSize, canPlace, level);
    }

    private boolean showVillageBuildingPlacementPreview() {
        if (state.mode != GameMode.VILLAGE || !state.isManagedVillageMap() || state.isManagedVillageInterior() || state.villageTab != 0 || hoverPoint == null) {
            return false;
        }
        if (hoverPoint.x < 0 || hoverPoint.x >= gameAreaWidth() || hoverPoint.y < 0 || hoverPoint.y >= worldViewportHeight()) {
            return false;
        }
        if ("place".equals(state.villageEditAction) && (state.selectedVillageBuildingStyle == null || state.selectedVillageBuildingStyle.isBlank())) {
            return false;
        }
        return "place".equals(state.villageEditAction)
                || ("move".equals(state.villageEditAction) && villageBuildingMoveSource() != null);
    }

    private void drawVillageBuildingActionHover(Graphics2D g, int camX, int camY, int tileSize) {
        if (!showVillageBuildingActionHover()) {
            return;
        }
        TilePoint tile = hoverWorldTile();
        if (tile == null) {
            return;
        }
        CityBuilding building = state.world.cityBuildingAt(WorldMap.PLAYER_VILLAGE_ID, tile.x(), tile.y());
        if (building == null || !building.key().startsWith("player_")) {
            return;
        }
        int px = (building.x1() - camX) * tileSize;
        int py = (building.y1() - camY) * tileSize;
        int w = building.width() * tileSize;
        int h = building.depth() * tileSize;
        Color edge = switch (state.villageEditAction) {
            case "upgrade" -> new Color(190, 166, 255);
            case "delete" -> new Color(255, 151, 142);
            default -> new Color(255, 219, 133);
        };
        Composite oldComposite = g.getComposite();
        Stroke oldStroke = g.getStroke();
        g.setComposite(AlphaComposite.SrcOver.derive(0.20f));
        g.setColor(edge);
        g.fillRoundRect(px + scaled(2), py + scaled(2), w - scaled(4), h - scaled(4), scaled(10), scaled(10));
        g.setComposite(AlphaComposite.SrcOver.derive(0.86f));
        g.setStroke(new BasicStroke(Math.max(2f, scaled(2))));
        g.drawRoundRect(px + scaled(2), py + scaled(2), w - scaled(4), h - scaled(4), scaled(10), scaled(10));
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(11, scaled(12))));
        g.setColor(new Color(246, 244, 228));
        String label = switch (state.villageEditAction) {
            case "upgrade" -> "Upgrade";
            case "delete" -> "Delete";
            default -> state.pendingVillageMoveSource == null ? "Move" : "Source";
        };
        drawCenteredIn(g, label, px, py - scaled(6), w);
        g.setStroke(oldStroke);
        g.setComposite(oldComposite);
    }

    private boolean showVillageBuildingActionHover() {
        if (state.mode != GameMode.VILLAGE || !state.isManagedVillageMap() || state.isManagedVillageInterior() || state.villageTab != 0 || hoverPoint == null) {
            return false;
        }
        if (hoverPoint.x < 0 || hoverPoint.x >= gameAreaWidth() || hoverPoint.y < 0 || hoverPoint.y >= worldViewportHeight()) {
            return false;
        }
        return "upgrade".equals(state.villageEditAction)
                || "delete".equals(state.villageEditAction)
                || ("move".equals(state.villageEditAction) && villageBuildingMoveSource() == null);
    }

    private CityBuilding villageBuildingMoveSource() {
        if (!"move".equals(state.villageEditAction) || state.pendingVillageMoveSource == null) {
            return null;
        }
        return state.world.cityBuildingAt(WorldMap.PLAYER_VILLAGE_ID, state.pendingVillageMoveSource.x(), state.pendingVillageMoveSource.y());
    }

    private void drawVillageBuildingFootprint(Graphics2D g, int x, int y, int width, int depth,
                                               int camX, int camY, int tileSize, boolean canPlace) {
        int px = (x - camX) * tileSize;
        int py = (y - camY) * tileSize;
        int w = width * tileSize;
        int h = depth * tileSize;
        Composite oldComposite = g.getComposite();
        Stroke oldStroke = g.getStroke();
        Color fill = canPlace ? new Color(67, 190, 97) : new Color(212, 72, 72);
        Color edge = canPlace ? new Color(158, 255, 168) : new Color(255, 151, 142);
        g.setComposite(AlphaComposite.SrcOver.derive(0.20f));
        g.setColor(fill);
        g.fillRoundRect(px + scaled(2), py + scaled(2), w - scaled(4), h - scaled(4), scaled(10), scaled(10));
        g.setComposite(AlphaComposite.SrcOver.derive(0.68f));
        g.setColor(edge);
        g.setStroke(new BasicStroke(Math.max(2f, scaled(2))));
        g.drawRoundRect(px + scaled(2), py + scaled(2), w - scaled(4), h - scaled(4), scaled(10), scaled(10));
        g.setComposite(AlphaComposite.SrcOver.derive(0.25f));
        g.setStroke(new BasicStroke(Math.max(1f, scaled(1))));
        for (int i = 1; i < width; i++) {
            int lineX = px + i * tileSize;
            g.drawLine(lineX, py + scaled(3), lineX, py + h - scaled(4));
        }
        for (int i = 1; i < depth; i++) {
            int lineY = py + i * tileSize;
            g.drawLine(px + scaled(3), lineY, px + w - scaled(4), lineY);
        }
        g.setStroke(oldStroke);
        g.setComposite(oldComposite);
    }

    private void drawVillageBuildingGhost(Graphics2D g, CityBuilding building, int camX, int camY, int tileSize, boolean canPlace, int level) {
        String asset = villageBuildingSpriteForLevel(building.style(), level, building, 0, 0);
        TilePoint door = state.world.cityBuildingDoorTiles(building).stream()
                .findFirst()
                .orElse(new TilePoint(building.x1() + building.width() / 2, building.y2()));
        int lotX = (building.x1() - camX) * tileSize;
        int lotW = building.width() * tileSize;
        int frontY = (building.y2() - camY + 1) * tileSize;
        int doorCenterX = (door.x() - camX) * tileSize + tileSize / 2;
        int targetW = Math.max(tileRelative(70, tileSize), Math.min(lotW + tileRelative(28, tileSize), tileRelative(176, tileSize)));
        int targetH = Math.max(tileRelative(86, tileSize), Math.min(building.depth() * tileSize + tileRelative(54, tileSize), tileRelative(168, tileSize)));
        int drawX = doorCenterX - targetW / 2;
        int drawY = frontY - targetH - tileRelative(2, tileSize);
        Composite oldComposite = g.getComposite();
        Stroke oldStroke = g.getStroke();
        g.setComposite(AlphaComposite.SrcOver.derive(canPlace ? 0.62f : 0.45f));
        g.setColor(new Color(10, 13, 14, 95));
        g.fillOval(lotX + scaled(6), frontY - scaled(13), Math.max(scaled(24), lotW - scaled(12)), scaled(13));
        g.drawImage(assets.spriteFit(asset, targetW, targetH), drawX, drawY, null);
        g.setComposite(AlphaComposite.SrcOver.derive(0.82f));
        g.setStroke(new BasicStroke(Math.max(2f, scaled(2))));
        g.setColor(canPlace ? new Color(173, 255, 178) : new Color(255, 157, 147));
        g.drawRoundRect(drawX + scaled(2), drawY + scaled(2), targetW - scaled(4), targetH - scaled(4), scaled(10), scaled(10));
        g.setStroke(oldStroke);
        g.setComposite(oldComposite);
    }

    private void drawPlacementPulses(Graphics2D g, int camX, int camY, int tileSize) {
        if (placementPulses.isEmpty()) {
            return;
        }
        placementPulses.removeIf(pulse -> !pulse.mapId().equals(state.currentMapId) || frame - pulse.startFrame() > PlacementPulse.DURATION);
        Composite oldComposite = g.getComposite();
        Stroke oldStroke = g.getStroke();
        for (PlacementPulse pulse : placementPulses) {
            double progress = moveProgress(frame - pulse.startFrame(), PlacementPulse.DURATION);
            float alpha = (float) (0.72 * (1.0 - progress));
            int px = (pulse.x() - camX) * tileSize;
            int py = (pulse.y() - camY) * tileSize;
            int spread = (int) Math.round(progress * scaled(13));
            int lift = (int) Math.round(Math.sin(progress * Math.PI) * scaled(5));
            g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, alpha)));
            g.setColor(new Color(143, 255, 151));
            g.setStroke(new BasicStroke(Math.max(2f, scaled(2))));
            g.drawRoundRect(px + scaled(5) - spread, py + scaled(5) - spread,
                    tileSize - scaled(10) + spread * 2, tileSize - scaled(10) + spread * 2,
                    scaled(10), scaled(10));
            g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, alpha * 0.5f)));
            g.fillOval(px + tileSize / 2 - scaled(10), py + scaled(12) - lift, scaled(20), scaled(8));
        }
        g.setStroke(oldStroke);
        g.setComposite(oldComposite);
    }

    private boolean showInteriorPlacementPreview() {
        if (state.mode != GameMode.VILLAGE || !state.isManagedVillageInterior() || hoverPoint == null) {
            return false;
        }
        if (hoverPoint.x < 0 || hoverPoint.x >= gameAreaWidth() || hoverPoint.y < 0 || hoverPoint.y >= worldViewportHeight()) {
            return false;
        }
        return "place".equals(state.villageEditAction)
                || ("move".equals(state.villageEditAction) && state.pendingVillageMoveSource != null);
    }

    private String interiorPlacementPreviewAsset() {
        if ("move".equals(state.villageEditAction) && state.pendingVillageMoveSource != null) {
            WorldProp prop = state.currentMapId.equals(state.pendingVillageMoveMapId)
                    ? state.world.playerInteriorPropAt(state.currentMapId, state.pendingVillageMoveSource.x(), state.pendingVillageMoveSource.y())
                    : null;
            return prop == null ? null : prop.asset();
        }
        return state.selectedInteriorAsset;
    }

    private TilePoint hoverWorldTile() {
        if (hoverPoint == null) {
            return null;
        }
        int size = tileSize();
        int x = (int) Math.floor(lastCameraX + hoverPoint.x / (double) size);
        int y = (int) Math.floor(lastCameraY + hoverPoint.y / (double) size);
        return new TilePoint(x, y);
    }

    private TilePoint hoverWorldTileInViewport() {
        if (hoverPoint == null || hoverPoint.x < 0 || hoverPoint.x >= gameAreaWidth()
                || hoverPoint.y < 0 || hoverPoint.y >= worldViewportHeight()) {
            return null;
        }
        TilePoint tile = hoverWorldTile();
        if (tile == null || tile.x() < 0 || tile.y() < 0
                || tile.x() >= state.world.width(state.currentMapId)
                || tile.y() >= state.world.height(state.currentMapId)) {
            return null;
        }
        return tile;
    }

    private void triggerInteriorPlacementPulse(int x, int y) {
        placementPulses.add(new PlacementPulse(state.currentMapId, x, y, frame));
    }

    private CharacterWorldScale characterWorldScale(String sprite, boolean player) {
        return switch (sprite) {
            case "class_knight_model", "npc_torin_model" -> new CharacterWorldScale(48, 65, 4, 38);
            case "class_mage_model", "npc_ren_model", "npc_rowan_model", "npc_vexa_model", "npc_elowen_model",
                    "npc_liora_model", "npc_maera_model", "npc_vesper_model" -> new CharacterWorldScale(50, 66, 4, 36);
            case "class_cleric_model", "npc_mira_sunwarden_model", "npc_lyra_model", "npc_samir_model" -> new CharacterWorldScale(47, 64, 4, 36);
            case "class_ranger_model", "class_rogue_model", "npc_nyx_model", "npc_sable_model", "npc_kael_model",
                    "npc_seraphine_model", "npc_aria_model", "npc_rafiq_model" ->
                    new CharacterWorldScale(44, 61, 3, 34);
            case "npc_garruk_model", "npc_cassia_model" -> new CharacterWorldScale(52, 66, 4, 42);
            case "npc_orin_model", "npc_calder_model" -> new CharacterWorldScale(50, 59, 3, 40);
            case "npc_baker_model", "npc_bartender_model", "npc_blacksmith_model", "npc_citizen_man_model",
                    "npc_citizen_woman_model", "npc_innkeeper_model", "npc_merchant_model",
                    "npc_quartermaster_model" -> new CharacterWorldScale(43, 58, 3, 34);
            default -> player ? new CharacterWorldScale(46, 63, 4, 36) : new CharacterWorldScale(43, 59, 3, 34);
        };
    }

    private void drawCharacterSprite(Graphics2D g, String sprite, int x, int y, int width, int height, int facingDx, int facingDy, int step) {
        DirectionalSprite directional = directionalSprite(sprite, facingDx, facingDy);
        boolean hasWalkAnimation = step >= 0 && assets.hasAnimatedSprite(directional.sprite(), "walk");
        BufferedImage image = hasWalkAnimation
                ? assets.animatedSpriteFit(directional.sprite(), "walk", width, height, step)
                : assets.spriteFit(directional.sprite(), width, height);
        String shadowKey = "char:" + directional.sprite() + ":" + width + "x" + height + ":" + (hasWalkAnimation ? step : "idle");
        drawCasterShadow(g, image, shadowKey, x, y, width, height, directional.flipHorizontal(), 0.42f);
        Graphics2D spriteG = (Graphics2D) g.create();
        spriteG.translate(x + width / 2.0, y + height);
        if (directional.flipHorizontal()) {
            spriteG.scale(-1.0, 1.0);
        }
        spriteG.drawImage(image, -width / 2, -height, width, height, null);
        spriteG.dispose();
    }

    private int walkAnimationFrame(int elapsed, int duration) {
        double progress = Math.max(0.0, Math.min(0.999, elapsed / (double) Math.max(1, duration)));
        return Math.min(WALK_ANIMATION_FRAMES - 1, (int) Math.floor(progress * WALK_ANIMATION_FRAMES));
    }

    private int npcWalkAnimationFrame(String sprite, int width, int height, GameState.NpcMotion motion) {
        DirectionalSprite directional = directionalSprite(sprite, motion.facingDx(), motion.facingDy());
        int animationFrames = Math.max(1, assets.animatedSpriteFrameCount(directional.sprite(), "walk", width, height));
        int framesPerTile = Math.max(1, Math.min(animationFrames, WALK_CYCLE_FRAMES_PER_TILE));
        double progress = moveProgress(state.worldTick - motion.moveStartTick(), GameState.NpcMotion.MOVE_TICKS);
        int animationStep = (int) Math.floor(progress * framesPerTile);
        return Math.floorMod(animationStep, animationFrames);
    }

    private int playerWalkAnimationFrame(String sprite, int width, int height) {
        DirectionalSprite directional = directionalSprite(sprite, playerFacingDx, playerFacingDy);
        int animationFrames = Math.max(1, assets.animatedSpriteFrameCount(directional.sprite(), "walk", width, height));
        int framesPerTile = Math.max(1, Math.min(animationFrames, WALK_CYCLE_FRAMES_PER_TILE));
        double progress = moveProgress(frame - playerMoveStartFrame, playerMoveFrames());
        int animationStep = (int) Math.floor((playerWalkAnimationTileStart + progress) * framesPerTile);
        return Math.floorMod(animationStep, animationFrames);
    }

    private DirectionalSprite directionalSprite(String sprite, int facingDx, int facingDy) {
        String direction;
        if (Math.abs(facingDx) > Math.abs(facingDy)) {
            direction = facingDx < 0 ? "left" : "right";
        } else if (facingDy < 0) {
            direction = "up";
        } else {
            direction = "down";
        }
        String directionalName = sprite + "_" + direction;
        if (assets.hasSprite(directionalName)) {
            return new DirectionalSprite(directionalName, false);
        }
        if ("left".equals(direction) && assets.hasSprite(sprite + "_right")) {
            return new DirectionalSprite(sprite + "_right", true);
        }
        if ("right".equals(direction) && assets.hasSprite(sprite + "_left")) {
            return new DirectionalSprite(sprite + "_left", true);
        }
        return new DirectionalSprite(sprite, facingDx < 0);
    }

    private boolean npcMoving(GameState.NpcMotion motion) {
        return state.worldTick - motion.moveStartTick() < GameState.NpcMotion.MOVE_TICKS
                && (motion.x() != motion.fromX() || motion.y() != motion.fromY());
    }

    private double renderNpcX(GameState.NpcMotion motion) {
        if (!npcMoving(motion)) {
            return motion.x();
        }
        double t = smoothStep(moveProgress(state.worldTick - motion.moveStartTick(), GameState.NpcMotion.MOVE_TICKS));
        return motion.fromX() + (motion.x() - motion.fromX()) * t;
    }

    private double renderNpcY(GameState.NpcMotion motion) {
        if (!npcMoving(motion)) {
            return motion.y();
        }
        double t = smoothStep(moveProgress(state.worldTick - motion.moveStartTick(), GameState.NpcMotion.MOVE_TICKS));
        return motion.fromY() + (motion.y() - motion.fromY()) * t;
    }

    private boolean dungeonMonsterMoving(GameState.DungeonMonsterMotion motion) {
        return state.worldTick - motion.moveStartTick() < GameState.DungeonMonsterMotion.MOVE_TICKS
                && (motion.x() != motion.fromX() || motion.y() != motion.fromY());
    }

    private double renderDungeonMonsterX(GameState.DungeonMonsterMotion motion) {
        if (!dungeonMonsterMoving(motion)) {
            return motion.x();
        }
        double t = smoothStep(moveProgress(state.worldTick - motion.moveStartTick(), GameState.DungeonMonsterMotion.MOVE_TICKS));
        return motion.fromX() + (motion.x() - motion.fromX()) * t;
    }

    private double renderDungeonMonsterY(GameState.DungeonMonsterMotion motion) {
        if (!dungeonMonsterMoving(motion)) {
            return motion.y();
        }
        double t = smoothStep(moveProgress(state.worldTick - motion.moveStartTick(), GameState.DungeonMonsterMotion.MOVE_TICKS));
        return motion.fromY() + (motion.y() - motion.fromY()) * t;
    }

    private CameraView updateCamera(double viewportTilesX, double viewportTilesY, int mapWidth, int mapHeight) {
        CameraMode mode = cameraMode();
        double playerX = renderPlayerX();
        double playerY = renderPlayerY();
        int[] heldDirection = heldMoveDirection();
        int lookDx = queuedMoveDx != 0 || queuedMoveDy != 0 ? queuedMoveDx : heldDirection[0] != 0 || heldDirection[1] != 0 ? heldDirection[0] : playerFacingDx;
        int lookDy = queuedMoveDx != 0 || queuedMoveDy != 0 ? queuedMoveDy : heldDirection[0] != 0 || heldDirection[1] != 0 ? heldDirection[1] : playerFacingDy;
        return cameraController.update(
                state.currentMapId,
                playerX,
                playerY,
                lookDx,
                lookDy,
                viewportTilesX,
                viewportTilesY,
                mapWidth,
                mapHeight,
                mode,
                state.config.cameraLookAhead,
                state.config.cameraSmoothing
        );
    }

    private void resetCameraToPlayer() {
        cameraController.resetToPlayer();
    }

    private boolean playerMoving() {
        return state.currentMapId.equals(playerMoveMapId)
                && frame - playerMoveStartFrame < playerMoveFrames()
                && (state.playerX != playerMoveFromX || state.playerY != playerMoveFromY);
    }

    private double renderPlayerX() {
        if (!playerMoving()) {
            return state.playerX;
        }
        double t = smoothStep(moveProgress(frame - playerMoveStartFrame, playerMoveFrames()));
        return playerMoveFromX + (state.playerX - playerMoveFromX) * t;
    }

    private double renderPlayerY() {
        if (!playerMoving()) {
            return state.playerY;
        }
        double t = smoothStep(moveProgress(frame - playerMoveStartFrame, playerMoveFrames()));
        return playerMoveFromY + (state.playerY - playerMoveFromY) * t;
    }

    private double moveProgress(int elapsed, int duration) {
        return Math.max(0.0, Math.min(1.0, (elapsed + 1.0) / Math.max(1.0, duration)));
    }

    private double smoothStep(double value) {
        double t = Math.max(0.0, Math.min(1.0, value));
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }

    private CameraMode cameraMode() {
        return CameraMode.fromKey(state.config.cameraMode);
    }

    private MovementSpeed movementSpeed() {
        for (MovementSpeed speed : MovementSpeed.values()) {
            if (speed.key.equals(state.config.movementSpeed)) {
                return speed;
            }
        }
        return MovementSpeed.NORMAL;
    }

    private WeatherQuality weatherQuality() {
        return WeatherQuality.fromKey(state.config.weatherQuality);
    }

    private int playerMoveFrames() {
        return Math.max(1, playerMoveDurationFrames);
    }

    private int playerMoveFramesForStep(String mapId, int fromX, int fromY, int toX, int toY) {
        int frames = movementSpeed().frames;
        if (Terrain.roadLike(state.world.tileAt(mapId, fromX, fromY))
                || Terrain.roadLike(state.world.tileAt(mapId, toX, toY))) {
            return Math.max(1, Math.max(MIN_ROAD_MOVE_FRAMES, (int) Math.round(frames * ROAD_MOVE_FRAME_MULTIPLIER * state.travelSpeedWorldMultiplier())));
        }
        return Math.max(1, (int) Math.round(frames * state.travelSpeedWorldMultiplier()));
    }

    private int[] heldMoveDirection() {
        if (lastHeldMoveDx != 0 || lastHeldMoveDy != 0) {
            if ((lastHeldMoveDx < 0 && moveLeftHeld)
                    || (lastHeldMoveDx > 0 && moveRightHeld)
                    || (lastHeldMoveDy < 0 && moveUpHeld)
                    || (lastHeldMoveDy > 0 && moveDownHeld)) {
                return new int[]{lastHeldMoveDx, lastHeldMoveDy};
            }
        }
        if (moveRightHeld) {
            return new int[]{1, 0};
        }
        if (moveLeftHeld) {
            return new int[]{-1, 0};
        }
        if (moveDownHeld) {
            return new int[]{0, 1};
        }
        if (moveUpHeld) {
            return new int[]{0, -1};
        }
        return new int[]{0, 0};
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
            Rectangle hoverBounds = buildingScreenBounds(kind, building);
            boolean hovered = hoverPoint != null && hoverBounds.contains(hoverPoint);
            Color accent = currentSettlementAccentColor();
            String portraitAsset = buildingDisplayAsset(kind, building, seed);
            tooltipZones.add(new TooltipZone(
                    hoverBounds,
                    state.generatedBuildingName(building),
                    buildingTooltip(building),
                    portraitAsset.isBlank() ? null : "sprite:" + portraitAsset,
                    accent
            ));
            if (usesStandaloneSettlementBuilding(kind, building)) {
                drawStandaloneVillageBuilding(g, building, camX, camY, ts);
                if (hovered) {
                    drawBuildingHoverHighlight(g, kind, building, accent);
                }
                continue;
            }

            g.setColor(new Color(17, 20, 22, 140));
            g.fillOval(lotX + scaled(4), frontY - scaled(14), Math.max(scaled(18), lotW - scaled(8)), scaled(14));
            g.setColor(new Color(45, 54, 55));
            g.fillRect(lotX + scaled(4), frontY - scaled(8), Math.max(scaled(10), lotW - scaled(8)), scaled(6));
            g.setColor(new Color(115, 122, 116));
            g.drawLine(lotX + scaled(4), frontY - scaled(9), lotX + Math.max(scaled(4), lotW - scaled(4)), frontY - scaled(9));

            int modules = buildingModuleCount(building);
            for (int index = 0; index < modules; index++) {
                String asset = buildingSprite(building, seed, index);
                boolean civic = List.of("hall", "guild", "barracks", "warehouse").contains(building.style());
                boolean compactModule = List.of("guild", "house", "shop", "row").contains(building.style());
                int minH = civic ? 108 : (compactModule ? 102 : 90);
                int maxH = civic ? 166 : (compactModule ? 150 : 134);
                int minW = civic ? 78 : (compactModule ? 72 : 58);
                int maxW = civic ? 128 : (compactModule ? 122 : 104);
                int targetH = Math.max(tileRelative(minH, ts),
                        Math.min(tileRelative(maxH, ts), building.depth() * ts + tileRelative(civic ? 50 : 42, ts)));
                int targetW = Math.max(tileRelative(minW, ts),
                        Math.min(lotW / Math.max(1, modules) + tileRelative(civic ? 42 : 34, ts), tileRelative(maxW, ts)));
                int centerX = lotX + (int) Math.round((index + 0.5) * lotW / modules);
                int jitter = ((seed >> (index * 4)) & 7) - 3;
                int drawX = centerX - targetW / 2 + jitter * ts / GameConfig.TILE;
                int drawY = frontY - targetH - tileRelative(5, ts);
                g.drawImage(assets.spriteFit(asset, targetW, targetH), drawX, drawY, null);
            }

            for (TilePoint door : state.world.cityBuildingDoorTiles(building)) {
                drawBuildingDoorMarker(g, door, camX, camY, ts);
            }

            if (building.width() >= 4) {
                int lanternH = scaled(18);
                int lanternW = scaled(10);
                g.drawImage(assets.spriteFit("city_lantern", lanternW, lanternH), lotX + scaled(4), frontY - lanternH - scaled(20), null);
                g.drawImage(assets.spriteFit("city_lantern", lanternW, lanternH), lotX + lotW - lanternW - scaled(4), frontY - lanternH - scaled(20), null);
            }
            if (hovered) {
                drawBuildingHoverHighlight(g, kind, building, accent);
            }
        }
    }

    private boolean isPlayerVillageBuilding(CityBuilding building) {
        return building.key() != null && building.key().startsWith("player_")
                && VillageManager.isManagedBuildingStyle(building.style());
    }

    private Rectangle buildingScreenBounds(String kind, CityBuilding building) {
        int ts = tileSize();
        if (usesStandaloneSettlementBuilding(kind, building)) {
            return standaloneBuildingSpriteBounds(building, lastCameraX, lastCameraY, ts);
        }
        return buildingVisualBounds(building, lastCameraX, lastCameraY, ts);
    }

    private Rectangle buildingVisualBounds(CityBuilding building, double camX, double camY, int ts) {
        int x = (int) Math.round((building.x1() - camX) * ts);
        int topLift = tileRelative(buildingTopLift(building), ts);
        int y = (int) Math.round((building.y1() - camY) * ts) - topLift;
        int width = Math.max(ts, building.width() * ts);
        int height = Math.max(ts * 2, building.depth() * ts + topLift);
        return new Rectangle(x, y, width, height);
    }

    private int buildingTopLift(CityBuilding building) {
        if (List.of("hall", "guild", "barracks", "warehouse", "inn", "arena", "mage_tower", "bell_tower", "sun_shrine", "river_hall")
                .contains(building.style())) {
            return 54;
        }
        return 30;
    }

    private CityBuilding buildingAtScreenPoint(Point point) {
        if (point == null || !isSettlementMapKind(state.currentMapId)
                || point.x < 0 || point.x >= gameAreaWidth()
                || point.y < 0 || point.y >= worldViewportHeight()) {
            return null;
        }
        List<CityBuilding> buildings = state.world.cityBuildings(state.currentMapId);
        for (int i = buildings.size() - 1; i >= 0; i--) {
            CityBuilding building = buildings.get(i);
            if (buildingScreenBounds(state.world.kind(state.currentMapId), building).contains(point)) {
                return building;
            }
        }
        return null;
    }

    private void drawBuildingHoverHighlight(Graphics2D g, String kind, CityBuilding building, Color accent) {
        Rectangle bounds = buildingWorldVisualBounds(kind, building);
        Composite oldComposite = g.getComposite();
        Stroke oldStroke = g.getStroke();
        Font oldFont = g.getFont();
        int arc = scaled(8);
        g.setComposite(AlphaComposite.SrcOver.derive(0.18f));
        g.setColor(accent);
        g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, arc, arc);
        g.setComposite(AlphaComposite.SrcOver.derive(0.88f));
        g.setStroke(new BasicStroke(Math.max(2f, scaledStroke(1.8f))));
        g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, arc, arc);

        String label = state.generatedBuildingName(building);
        int fontSize = Math.max(10, Math.min(18, settlementHoverNameFontSize()));
        g.setFont(new Font("SansSerif", Font.BOLD, fontSize));
        FontMetrics metrics = g.getFontMetrics();
        int labelW = Math.min(gameAreaWidth() - scaled(24), metrics.stringWidth(label) + scaled(18));
        int labelH = Math.max(scaled(18), fontSize + scaled(8));
        int labelX = clamp(bounds.x + bounds.width / 2 - labelW / 2, scaled(8), Math.max(scaled(8), gameAreaWidth() - labelW - scaled(8)));
        int labelY = Math.max(scaled(4), bounds.y - labelH - scaled(4));
        g.setComposite(AlphaComposite.SrcOver.derive(0.92f));
        g.setColor(new Color(12, 16, 22, 224));
        g.fillRoundRect(labelX, labelY, labelW, labelH, scaled(8), scaled(8));
        g.setColor(accent);
        g.drawRoundRect(labelX, labelY, labelW, labelH, scaled(8), scaled(8));
        g.setColor(new Color(248, 242, 224));
        drawClippedString(g, label, labelX + scaled(8), labelY + labelH / 2 + metrics.getAscent() / 2 - scaled(2), labelW - scaled(16));

        g.setFont(oldFont);
        g.setStroke(oldStroke);
        g.setComposite(oldComposite);
    }

    private void drawNpcHoverHighlight(Graphics2D g, Rectangle bounds, String label, Color accent) {
        Composite oldComposite = g.getComposite();
        Stroke oldStroke = g.getStroke();
        Font oldFont = g.getFont();
        int arc = scaled(8);
        g.setComposite(AlphaComposite.SrcOver.derive(0.16f));
        g.setColor(accent);
        g.fillRoundRect(bounds.x - scaled(3), bounds.y - scaled(3), bounds.width + scaled(6), bounds.height + scaled(6), arc, arc);
        g.setComposite(AlphaComposite.SrcOver.derive(0.88f));
        g.setStroke(new BasicStroke(Math.max(2f, scaledStroke(1.5f))));
        g.drawRoundRect(bounds.x - scaled(3), bounds.y - scaled(3), bounds.width + scaled(6), bounds.height + scaled(6), arc, arc);

        int fontSize = Math.max(10, Math.min(15, settlementHoverNameFontSize()));
        g.setFont(new Font("SansSerif", Font.BOLD, fontSize));
        FontMetrics metrics = g.getFontMetrics();
        int labelW = Math.min(gameAreaWidth() - scaled(24), metrics.stringWidth(label) + scaled(18));
        int labelH = Math.max(scaled(18), fontSize + scaled(8));
        int labelX = clamp(bounds.x + bounds.width / 2 - labelW / 2, scaled(8), Math.max(scaled(8), gameAreaWidth() - labelW - scaled(8)));
        int labelY = Math.max(scaled(4), bounds.y - labelH - scaled(5));
        g.setComposite(AlphaComposite.SrcOver.derive(0.92f));
        g.setColor(new Color(12, 16, 22, 224));
        g.fillRoundRect(labelX, labelY, labelW, labelH, scaled(8), scaled(8));
        g.setColor(accent);
        g.drawRoundRect(labelX, labelY, labelW, labelH, scaled(8), scaled(8));
        g.setColor(new Color(248, 242, 224));
        drawClippedString(g, label, labelX + scaled(8), labelY + labelH / 2 + metrics.getAscent() / 2 - scaled(2), labelW - scaled(16));

        g.setFont(oldFont);
        g.setStroke(oldStroke);
        g.setComposite(oldComposite);
    }

    private Rectangle buildingWorldVisualBounds(String kind, CityBuilding building) {
        int ts = tileSize();
        if (usesStandaloneSettlementBuilding(kind, building)) {
            return standaloneBuildingSpriteBounds(building, lastCamX, lastCamY, ts);
        }
        return buildingVisualBounds(building, lastCamX, lastCamY, ts);
    }

    private Rectangle standaloneBuildingSpriteBounds(CityBuilding building, double camX, double camY, int ts) {
        BuildingVisualLayout layout = standaloneBuildingVisualLayout(building, camX, camY, ts);
        int pad = Math.max(2, tileRelative(3, ts));
        int basePad = Math.max(pad, tileRelative(5, ts));
        return new Rectangle(
                layout.drawX() - pad,
                layout.drawY() - pad,
                layout.drawW() + pad * 2,
                layout.drawH() + pad + basePad
        );
    }

    private String buildingDisplayAsset(String kind, CityBuilding building, int seed) {
        if (usesStandaloneSettlementBuilding(kind, building)) {
            int level = isPlayerVillageBuilding(building) ? state.world.playerVillageBuildingLevel(building) : 1;
            return villageBuildingSpriteForLevel(building.style(), level, building, seed, 0);
        }
        return buildingSprite(building, seed, 0);
    }

    private String buildingTooltip(CityBuilding building) {
        String mapId = state.currentMapId;
        int count = state.buildingKnowledgeCount(mapId, building);
        int level = state.buildingKnowledgeLevel(mapId, building);
        String name = state.generatedBuildingName(building);
        WorldMap.SettlementSite settlement = currentSettlementSite();
        String kingdom = settlement == null ? "Unclaimed" : kingdomName(settlement.kingdomId());
        StringBuilder body = new StringBuilder();
        body.append("Type: ").append(state.buildingTypeLabel(building)).append(".");
        body.append("\nKnowledge: ").append(buildingKnowledgeLabel(level))
                .append(" (").append(count).append(count == 1 ? " clue" : " clues")
                .append(", INT ").append(state.player.intelligence).append(").");
        body.append("\nKingdom: ").append(kingdom).append(".");
        if (level <= 0) {
            body.append("\nOnly the frontage is familiar. Inspect it, step inside, or ask nearby people to learn more.");
            return body.toString();
        }
        body.append("\n").append(buildingExteriorFlavor(building, name));
        if (level >= 2) {
            body.append("\nPurpose: ").append(buildingPurposeFlavor(building)).append(".");
        }
        if (level >= 3) {
            body.append("\nLocal note: ").append(buildingLocalFlavor(building, settlement)).append(".");
        }
        if (level >= 4) {
            body.append("\nInterior read: ").append(buildingInteriorFlavor(building)).append(".");
        }
        if (level >= 5) {
            body.append("\nYou know the likely entrance, staff habits, and which rumors are worth testing.");
        }
        return body.toString();
    }

    private String buildingKnowledgeLabel(int level) {
        return switch (level) {
            case 0 -> "Unknown";
            case 1 -> "Recognized";
            case 2 -> "Studied";
            case 3 -> "Local rumors";
            case 4 -> "Mapped";
            default -> "Well known";
        };
    }

    private String buildingExteriorFlavor(CityBuilding building, String name) {
        return switch (building.style()) {
            case "arena" -> name + " carries the dust and old cheers of public trials.";
            case "mage_tower" -> name + " narrows upward, its windows set like watchful blue-black eyes.";
            case "bell_tower" -> name + " keeps its high frame above the streets, built for sound and warning.";
            case "sun_shrine" -> name + " is faced toward the brightest road and kept cleaner than its neighbors.";
            case "river_hall" -> name + " has broad doors and damp stonework worn by many boots.";
            case "hall" -> name + " is civic stone: practical, old, and meant to outlast arguments.";
            case "barracks" -> name + " shows reinforced lintels and a door polished by patrol traffic.";
            case "warehouse" -> name + " smells faintly of rope, dust, and counted goods.";
            case "blacksmith" -> name + " has a sooty yard, stacked metal, and tools arranged for quick hands.";
            case "bakery" -> name + " carries warm grain smells out into the street before the sign is readable.";
            case "restaurant" -> name + " keeps a busy frontage of ovens, tables, and road-worn appetites.";
            case "apothecary" -> name + " keeps herbs, jars, and careful shade close to its doorway.";
            case "alchemist" -> name + " glints with glass, brass, and a few experiments kept just out of reach.";
            case "forestry_hut" -> name + " gathers cut wood, resin, and saw marks into a working corner.";
            case "farmstead" -> name + " keeps baskets and farm tools where townsfolk can borrow them fast.";
            case "fishing_hut" -> name + " has damp racks and trade goods sorted for the waterside.";
            case "workshop" -> name + " shows sawdust, repair benches, and a front built around daily craft.";
            case "carpenter" -> name + " is all straight edges, plank stacks, and careful joinery.";
            case "guild" -> name + " keeps tidy windows and a threshold marked by careful hands.";
            case "inn" -> name + " leaks warmth, smoke, and the low murmur of travelers.";
            case "shop" -> name + " dresses its front for passing coin and curious eyes.";
            case "row" -> name + " is part home, part witness to daily town life.";
            default -> name + " has the worn look of a place people use more than they praise.";
        };
    }

    private String buildingPurposeFlavor(CityBuilding building) {
        return switch (building.style()) {
            case "arena" -> "training, spectacle, and disputes settled under public rules";
            case "mage_tower" -> "study, ward maintenance, and careful magical observation";
            case "bell_tower" -> "warnings, timekeeping, and messages carried over rooftops";
            case "sun_shrine" -> "prayer, healing customs, and vows made under heat and light";
            case "river_hall", "hall" -> "council work, petitions, records, and local authority";
            case "barracks" -> "watch rotations, arms storage, and patrol orders";
            case "warehouse" -> "storage, ledgers, and goods waiting for safer roads";
            case "blacksmith" -> "metalwork, tool repair, hinges, blades, and practical town maintenance";
            case "bakery" -> "bread, grain stores, road food, and gossip traded before breakfast";
            case "restaurant" -> "meals, travelers, private talks, and work done over shared tables";
            case "apothecary" -> "herbs, tonics, poultices, and quiet medical errands";
            case "alchemist" -> "essences, strange glassware, tonics, and experiments with civic permission";
            case "forestry_hut" -> "woodcutting, timber sorting, repairs, and managed grove work";
            case "farmstead" -> "produce, small livestock, seed keeping, and seasonal errands";
            case "fishing_hut" -> "nets, river food, dock work, and weather-watched trade";
            case "workshop" -> "carpentry, repairs, fittings, and practical craft orders";
            case "carpenter" -> "woodwork, repairs, frames, carts, and the town's practical building needs";
            case "guild" -> "specialist work, apprentices, and closely held methods";
            case "inn" -> "rest, gossip, hiring, and travelers between errands";
            case "shop" -> "trade, repairs, supplies, and small local negotiations";
            case "row" -> "household life and the quieter stories of a street";
            default -> "ordinary work that keeps the settlement alive";
        };
    }

    private String buildingLocalFlavor(CityBuilding building, WorldMap.SettlementSite settlement) {
        String place = settlement == null ? "this settlement" : shortSettlementLabel(settlement.label());
        return switch (building.style()) {
            case "barracks", "arena" -> "guards mention it when routes through " + place + " become tense";
            case "guild", "mage_tower" -> "locals lower their voices around its records and experiments";
            case "blacksmith", "bakery", "restaurant", "apothecary", "alchemist", "forestry_hut", "farmstead",
                    "fishing_hut", "workshop", "carpenter" ->
                    "workers use it as a landmark because its yard changes with the day";
            case "inn", "shop" -> "travelers trade useful scraps here before they become public news";
            case "hall", "river_hall", "bell_tower", "sun_shrine" -> "people point toward it when asked who really keeps order";
            default -> "nearby residents know its routines better than any posted sign";
        };
    }

    private String buildingInteriorFlavor(CityBuilding building) {
        return switch (building.style()) {
            case "barracks", "arena" -> "expect racks, benches, orders, and people who notice loose hands";
            case "guild", "mage_tower", "hall", "river_hall" -> "expect desks, shelves, sealed corners, and names worth remembering";
            case "warehouse" -> "expect stacked goods, narrow lanes, and ledgers that explain more than speeches";
            case "blacksmith" -> "expect heat, iron, unfinished orders, and one tool nobody is allowed to touch";
            case "bakery" -> "expect ovens, grain sacks, and somebody counting loaves by habit";
            case "restaurant" -> "expect tables, heat, and a room where trade follows the food";
            case "apothecary" -> "expect drying herbs, labeled jars, and advice wrapped in caution";
            case "alchemist" -> "expect glassware, sealed cupboards, and labels written by careful hands";
            case "forestry_hut" -> "expect logs, saw marks, resin, and weather notes pinned near the door";
            case "farmstead" -> "expect baskets, seed trays, hand tools, and practical local gossip";
            case "fishing_hut" -> "expect nets, damp wood, salted goods, and talk about water levels";
            case "workshop" -> "expect benches, wood shavings, tools, and repairs waiting their turn";
            case "carpenter" -> "expect plank stacks, clamps, saw marks, and practical work in progress";
            case "inn" -> "expect tables, heat, and conversations that turn when strangers enter";
            case "shop" -> "expect counters, small stock, and someone measuring what you can afford";
            default -> "expect signs of ordinary life, plus one or two details outsiders usually miss";
        };
    }

    private boolean usesStandaloneSettlementBuilding(String kind, CityBuilding building) {
        if (isPlayerVillageBuilding(building)) {
            return true;
        }
        if ("village".equals(kind) && VillageManager.isManagedBuildingStyle(building.style())) {
            return true;
        }
        if (isStandaloneBusinessStyle(building.style())) {
            return true;
        }
        return List.of("hall", "barracks", "inn", "arena", "mage_tower", "bell_tower", "sun_shrine", "river_hall")
                .contains(building.style());
    }

    private boolean isStandaloneBusinessStyle(String style) {
        return List.of("warehouse", "blacksmith", "bakery", "restaurant", "apothecary", "alchemist",
                "forestry_hut", "farmstead", "fishing_hut", "workshop", "carpenter", "mine", "granary",
                "watchtower", "shrine").contains(style);
    }

    private void drawStandaloneVillageBuilding(Graphics2D g, CityBuilding building, int camX, int camY, int ts) {
        int level = isPlayerVillageBuilding(building) ? state.world.playerVillageBuildingLevel(building) : 1;
        String asset = villageBuildingSpriteForLevel(building.style(), level, building, 0, 0);
        BuildingVisualLayout layout = standaloneBuildingVisualLayout(building, camX, camY, ts);

        g.setColor(new Color(10, 13, 14, 105));
        g.fillOval(layout.lotX() + scaled(6), layout.frontY() - scaled(13),
                Math.max(scaled(24), layout.lotW() - scaled(12)), scaled(13));
        g.drawImage(assets.spriteFit(asset, layout.drawW(), layout.drawH()), layout.drawX(), layout.drawY(), null);
        drawBuildingDoorMarker(g, layout.door(), camX, camY, ts);
    }

    private int[] standaloneBuildingTargetSize(CityBuilding building, int lotW, int ts) {
        if ("garden".equals(building.style())) {
            return new int[]{
                    Math.min(lotW + tileRelative(10, ts), tileRelative(144, ts)),
                    Math.min(building.depth() * ts + tileRelative(18, ts), tileRelative(96, ts))
            };
        }
        if (List.of("watchtower", "mage_tower", "bell_tower").contains(building.style())) {
            return new int[]{
                    Math.min(lotW + tileRelative(28, ts), tileRelative(176, ts)),
                    Math.min(building.depth() * ts + tileRelative(112, ts), tileRelative(252, ts))
            };
        }
        if ("barracks".equals(building.style())) {
            return new int[]{
                    Math.min(lotW + tileRelative(28, ts), tileRelative(178, ts)),
                    Math.min(building.depth() * ts + tileRelative(72, ts), tileRelative(190, ts))
            };
        }
        if (List.of("hall", "arena", "river_hall").contains(building.style())) {
            return new int[]{
                    Math.min(lotW + tileRelative(42, ts), tileRelative(232, ts)),
                    Math.min(building.depth() * ts + tileRelative(80, ts), tileRelative(210, ts))
            };
        }
        if (List.of("sun_shrine", "shrine").contains(building.style())) {
            return new int[]{
                    Math.min(lotW + tileRelative(48, ts), tileRelative(236, ts)),
                    Math.min(building.depth() * ts + tileRelative(104, ts), tileRelative(230, ts))
            };
        }
        if ("warehouse".equals(building.style())) {
            return new int[]{
                    Math.min(lotW + tileRelative(42, ts), tileRelative(220, ts)),
                    Math.min(building.depth() * ts + tileRelative(74, ts), tileRelative(198, ts))
            };
        }
        if (List.of("blacksmith", "workshop", "carpenter", "forestry_hut", "farmstead", "fishing_hut").contains(building.style())) {
            return new int[]{
                    Math.min(lotW + tileRelative(34, ts), tileRelative(190, ts)),
                    Math.min(building.depth() * ts + tileRelative(62, ts), tileRelative(176, ts))
            };
        }
        if (List.of("bakery", "restaurant", "apothecary", "alchemist", "granary").contains(building.style())) {
            return new int[]{
                    Math.min(lotW + tileRelative(28, ts), tileRelative(172, ts)),
                    Math.min(building.depth() * ts + tileRelative(58, ts), tileRelative(164, ts))
            };
        }
        if (List.of("inn", "guild").contains(building.style())) {
            return new int[]{
                    Math.min(lotW + tileRelative(32, ts), tileRelative(212, ts)),
                    Math.min(building.depth() * ts + tileRelative(66, ts), tileRelative(188, ts))
            };
        }
        if ("row".equals(building.style())) {
            return new int[]{
                    Math.min(lotW + tileRelative(30, ts), tileRelative(184, ts)),
                    Math.min(building.depth() * ts + tileRelative(52, ts), tileRelative(158, ts))
            };
        }
        return new int[]{
                Math.min(lotW + tileRelative(12, ts), tileRelative(146, ts)),
                Math.min(building.depth() * ts + tileRelative(36, ts), tileRelative(132, ts))
        };
    }

    private BuildingVisualLayout standaloneBuildingVisualLayout(CityBuilding building, double camX, double camY, int ts) {
        int lotX = (int) Math.round((building.x1() - camX) * ts);
        int lotY = (int) Math.round((building.y1() - camY) * ts);
        int lotW = building.width() * ts;
        int lotH = building.depth() * ts;
        int frontY = (int) Math.round((building.y2() - camY + 1) * ts);
        TilePoint door = state.world.cityBuildingDoorTiles(building).stream()
                .findFirst()
                .orElse(new TilePoint(building.x1() + building.width() / 2, building.y2()));
        int doorCenterX = (int) Math.round((door.x() - camX) * ts + ts / 2.0);
        int[] target = standaloneBuildingTargetSize(building, lotW, ts);
        int drawW = Math.max(ts, target[0]);
        int drawH = Math.max(ts, target[1]);
        int drawX = doorCenterX - drawW / 2;
        int drawY = frontY - drawH - tileRelative(1, ts);
        Rectangle spriteBounds = new Rectangle(drawX, drawY, drawW, drawH);
        Rectangle lotBounds = new Rectangle(lotX, lotY, Math.max(ts, lotW), Math.max(ts, lotH));
        return new BuildingVisualLayout(spriteBounds.union(lotBounds), drawX, drawY, drawW, drawH, lotX, lotW, frontY, door);
    }

    private void drawBuildingDoorMarker(Graphics2D g, TilePoint door, int camX, int camY, int ts) {
        if (door.x() < camX || door.x() >= camX + lastVisibleCols || door.y() < camY || door.y() >= camY + lastVisibleRows) {
            return;
        }
        int markerX = (door.x() - camX) * ts;
        int markerY = (door.y() - camY + 1) * ts - tileRelative(7, ts);
        g.setColor(new Color(45, 36, 29));
        g.fillRect(markerX + scaled(10), markerY - scaled(14), ts - scaled(20), scaled(12));
        g.setColor(new Color(122, 96, 65));
        g.drawRect(markerX + scaled(10), markerY - scaled(14), ts - scaled(20), scaled(12));
        g.setColor(new Color(202, 162, 98));
        g.fillRect(markerX + scaled(13), markerY - scaled(3), ts - scaled(26), scaled(3));
    }

    private int buildingModuleCount(CityBuilding building) {
        boolean civic = List.of("hall", "guild", "barracks", "warehouse").contains(building.style());
        if (civic) {
            return Math.max(1, Math.min(3, (building.width() + 2) / 4));
        }
        int modules = Math.max(1, Math.min(building.width(), (building.width() + 1) / 2));
        if ("row".equals(building.style()) && building.width() >= 6) {
            modules = Math.min(4, modules);
        }
        return modules;
    }

    private String buildingSprite(CityBuilding building, int seed, int index) {
        String[] sprites = switch (building.style()) {
                    case "arena" -> new String[]{"city_building_civic_hall_imagegen", "city_building_town_manor"};
                    case "mage_tower" -> new String[]{"city_building_watchtower_imagegen", "city_building_civic_hall_imagegen"};
                    case "bell_tower" -> new String[]{"city_building_watchtower_imagegen", "city_building_civic_hall_imagegen"};
                    case "sun_shrine" -> new String[]{"village_building_shrine", "city_building_stone_hall"};
                    case "river_hall" -> new String[]{"city_building_civic_hall_imagegen", "city_building_house_wide"};
                    case "hall" -> new String[]{"city_building_civic_hall_imagegen", "city_building_town_manor"};
                    case "barracks" -> new String[]{"city_building_watchtower_imagegen", "city_building_town_manor"};
                    case "guild" -> new String[]{"city_building_stone_shop", "city_building_town_manor", "city_building_town_tall"};
                    case "warehouse" -> new String[]{"village_building_warehouse", "city_building_house_wide"};
                    case "blacksmith" -> new String[]{"village_building_blacksmith", "city_building_stone_shop"};
                    case "bakery" -> new String[]{"village_building_bakery", "city_building_town_shop"};
                    case "restaurant" -> new String[]{"city_building_inn_imagegen", "village_building_bakery", "city_building_town_shop"};
                    case "apothecary" -> new String[]{"village_building_apothecary", "city_building_town_shop"};
                    case "alchemist" -> new String[]{"village_building_apothecary", "city_building_stone_shop", "city_building_town_shop"};
                    case "forestry_hut" -> new String[]{"village_building_forestry_hut", "village_building_workshop"};
                    case "farmstead" -> new String[]{"village_building_farmstead", "village_building_garden"};
                    case "fishing_hut" -> new String[]{"village_building_fishing_hut", "city_building_town_small"};
                    case "workshop" -> new String[]{"village_building_workshop", "city_building_town_shop"};
                    case "carpenter" -> new String[]{"village_building_workshop", "city_building_town_shop"};
                    case "watchtower" -> new String[]{"village_building_watchtower", "city_building_watchtower_imagegen"};
                    case "shrine" -> new String[]{"village_building_shrine", "city_building_stone_hall"};
                    case "inn" -> new String[]{"city_building_inn_imagegen", "city_building_town_gabled"};
                    case "row" -> new String[]{"city_building_town_gabled", "city_building_town_shop", "city_building_town_small", "city_building_town_narrow"};
                    case "shop" -> new String[]{"city_building_town_shop", "city_building_town_gabled", "city_building_town_narrow"};
                    case "house" -> new String[]{"city_building_town_small", "city_building_town_gabled", "city_building_town_tall"};
                    default -> new String[]{"city_building_town_gabled", "city_building_town_shop", "city_building_town_small"};
                };
        String themedSprite = themedCityBuildingSprite(building.style(), building, seed, index);
        if (themedSprite != null && assets.hasSprite(themedSprite)) {
            List<String> themed = new ArrayList<>();
            themed.add(themedSprite);
            themed.addAll(List.of(sprites));
            sprites = themed.toArray(String[]::new);
        }
        if (!currentMapAllowsSnowBuildingSprites()) {
            sprites = nonSnowBuildingSprites(sprites, building.style());
        }
        return sprites[Math.floorMod(seed + building.palette() + index * 3, sprites.length)];
    }

    private String themedCityBuildingSprite(String style, CityBuilding building, int seed, int index) {
        String[] sprites = themedCityBuildingSprites(style);
        if (sprites.length == 0 || !shouldUseThemedCityBuildingSprite(style, building, seed, index)) {
            return null;
        }
        return sprites[Math.floorMod(seed + style.hashCode() + index * 5, sprites.length)];
    }

    private String[] themedCityBuildingSprites(String style) {
        if (!"city".equals(state.world.kind(state.currentMapId))) {
            return new String[0];
        }
        WorldMap.SettlementSite settlement = currentSettlementSite();
        if (settlement == null) {
            return new String[0];
        }
        return switch (settlement.kingdomId()) {
            case "riverside" -> List.of("river_hall", "hall", "warehouse", "inn", "restaurant", "fishing_hut").contains(style)
                    ? new String[]{"city_building_riverside_tradehall_imagegen", "city_building_civic_hall_imagegen", "city_building_inn_imagegen"} : new String[0];
            case "highwall" -> List.of("hall", "barracks", "blacksmith", "workshop", "carpenter", "warehouse").contains(style)
                    ? new String[]{"city_building_highwall_forge_keep_imagegen", "city_building_town_manor", "city_building_stone_shop"} : new String[0];
            case "crownlands" -> List.of("hall", "guild", "mage_tower", "apothecary", "alchemist").contains(style)
                    ? new String[]{"city_building_archive_grand_college_imagegen", "city_building_civic_hall_imagegen", "city_building_stone_shop"} : new String[0];
            case "belltower" -> List.of("bell_tower", "workshop", "carpenter", "guild", "hall").contains(style)
                    ? new String[]{"city_building_belltower_chime_workshop_imagegen", "city_building_town_manor", "city_building_stone_shop"} : new String[0];
            case "sanctum" -> List.of("sun_shrine", "shrine", "apothecary", "alchemist", "restaurant", "hall").contains(style)
                    ? new String[]{"city_building_sanctum_sun_alchemist_imagegen", "village_building_shrine", "city_building_civic_hall_imagegen"} : new String[0];
            default -> new String[0];
        };
    }

    private boolean shouldUseThemedCityBuildingSprite(String style, CityBuilding building, int seed, int index) {
        if (building == null) {
            return false;
        }
        boolean landmark = List.of("hall", "river_hall", "arena", "mage_tower", "bell_tower", "sun_shrine", "shrine")
                .contains(style);
        boolean large = building.width() >= 5 || building.depth() >= 4;
        int roll = Math.floorMod(seed + building.key().hashCode() + index * 31, 100);
        if (landmark) {
            return large || roll < 48;
        }
        if (List.of("warehouse", "barracks", "guild", "restaurant", "blacksmith").contains(style)) {
            return roll < 34;
        }
        if (List.of("apothecary", "alchemist", "workshop", "carpenter", "fishing_hut").contains(style)) {
            return roll < 22 && large;
        }
        return false;
    }

    private String[] nonSnowBuildingSprites(String[] sprites, String style) {
        List<String> filtered = new ArrayList<>();
        for (String sprite : sprites) {
            if (!isSnowRoofBuildingSprite(sprite)) {
                filtered.add(sprite);
            }
        }
        if (!filtered.isEmpty()) {
            return filtered.toArray(String[]::new);
        }
        return switch (style) {
            case "mage_tower", "barracks", "bell_tower" -> new String[]{"city_building_watchtower_imagegen"};
            case "warehouse", "row" -> new String[]{"city_building_town_gabled", "city_building_town_shop", "city_building_town_small"};
            default -> new String[]{"city_building_town_gabled", "city_building_town_shop"};
        };
    }

    private boolean currentMapAllowsSnowBuildingSprites() {
        WorldMap.SettlementSite settlement = currentSettlementSite();
        return settlement != null && "snow".equals(settlementBiomeVariant(settlement));
    }

    private boolean isSnowRoofBuildingSprite(String asset) {
        return asset.equals("city_building_house_wide")
                || asset.equals("city_building_house_shop")
                || asset.equals("city_building_house_gabled")
                || asset.equals("city_building_house_tower");
    }

    private String villageBuildingSpriteForLevel(String style, int level, CityBuilding fallbackBuilding, int seed, int index) {
        String themedSprite = themedCityBuildingSprite(style, fallbackBuilding, seed, index);
        if (fallbackBuilding != null && !isPlayerVillageBuilding(fallbackBuilding)
                && themedSprite != null && assets.hasSprite(themedSprite)) {
            return themedSprite;
        }
        if (fallbackBuilding != null && !isPlayerVillageBuilding(fallbackBuilding)
                && "city".equals(state.world.kind(state.currentMapId))) {
            List<String> citySprites = cityManagedBuildingSprites(style);
            if (!citySprites.isEmpty()) {
                return citySprites.get(Math.floorMod(seed + fallbackBuilding.key().hashCode() + index * 7, citySprites.size()));
            }
        }
        if (VillageManager.isManagedBuildingStyle(style)) {
            String candidate = VillageManager.buildingLevelSprite(style, level);
            if (!candidate.isBlank() && assets.hasSprite(candidate)) {
                return candidate;
            }
            List<String> sprites = VillageManager.buildingSprites(style);
            if (!sprites.isEmpty()) {
                return sprites.get(Math.floorMod(seed + index, sprites.size()));
            }
        }
        if (fallbackBuilding != null) {
            return buildingSprite(fallbackBuilding, seed, index);
        }
        List<String> sprites = VillageManager.buildingSprites(style);
        return sprites.isEmpty() ? "" : sprites.get(0);
    }

    private List<String> cityManagedBuildingSprites(String style) {
        return switch (style) {
            case "warehouse" -> List.of("village_building_warehouse", "village_building_granary", "city_building_town_manor");
            case "blacksmith" -> List.of("village_building_blacksmith", "city_building_stone_shop", "city_building_town_manor");
            case "bakery" -> List.of("village_building_bakery", "city_building_town_shop", "city_building_town_gabled");
            case "apothecary" -> List.of("village_building_apothecary", "city_building_stone_shop", "city_building_town_shop");
            case "forestry_hut" -> List.of("village_building_forestry_hut", "village_building_workshop", "city_building_town_small");
            case "farmstead" -> List.of("village_building_farmstead", "village_building_garden", "city_building_town_gabled");
            case "fishing_hut" -> List.of("village_building_fishing_hut", "city_building_town_small", "city_building_town_shop");
            case "granary" -> List.of("village_building_granary", "village_building_warehouse", "city_building_town_small");
            case "shrine" -> List.of("village_building_shrine", "city_building_civic_hall_imagegen");
            default -> List.of();
        };
    }

    private void drawHouseTile(Graphics2D g, char tile, int wx, int wy, int px, int py) {
        if (tile == 'o') {
            drawHouseWallTile(g, wx, wy, px, py);
        } else if (tile == 'e') {
            drawHouseFloorTile(g, 'i', wx, wy, px, py);
            if (doorConnectsWall(wx, wy) || exteriorEntranceDoor(wx, wy)) {
                drawHouseDoorTile(g, wx, wy, px, py);
            }
        } else if (tile == 'k') {
            drawHouseFurnitureTile(g, wx, wy, px, py);
        } else if (tile == 'i' || tile == 'e' || tile == 'z') {
            drawHouseFloorTile(g, tile, wx, wy, px, py);
        } else if (tile == 'x') {
            drawInteriorVoidTile(g, wx, wy, px, py);
        }
    }

    private void drawInteriorVoidTile(Graphics2D g, int wx, int wy, int px, int py) {
        int ts = tileSize();
        int seed = Math.abs(wx * 928371 + wy * 364479 + 127);
        g.drawImage(assets.image("interior_void_edge", ts, ts), px, py, null);
        if (seed % 4 == 0) {
            g.setColor(new Color(8, 8, 12, 76));
            g.fillRect(px, py, ts, ts);
        }
        if (nearInteriorTile(wx, wy, 0, 1) || nearInteriorTile(wx, wy, 1, 0) || nearInteriorTile(wx, wy, -1, 0)) {
            g.setColor(new Color(0, 0, 0, 96));
            g.fillRect(px, py, ts, scaled(5));
        }
    }

    private void drawHouseFloorTile(Graphics2D g, char tile, int wx, int wy, int px, int py) {
        int ts = tileSize();
        int seed = Math.abs(wx * 928371 + wy * 364479 + tile * 71);
        g.drawImage(assets.image(interiorFloorAsset(seed), ts, ts), px, py, null);
        drawInteriorFloorDepth(g, px, py, ts, seed);
        if (tile == 'z') {
            String rug = (seed & 1) == 0 ? "interior_rug_teal" : "interior_rug_red";
            g.drawImage(assets.image(rug, ts, ts), px, py, null);
            g.setColor(new Color(40, 24, 18, 72));
            g.fillRect(px + scaled(5), py + ts - scaled(7), ts - scaled(10), scaled(3));
        }
    }

    private void drawHouseWallTile(Graphics2D g, int wx, int wy, int px, int py) {
        int ts = tileSize();
        int seed = Math.abs(wx * 928371 + wy * 364479 + 79);
        boolean floorNorth = nearInteriorTile(wx, wy, 0, -1);
        boolean floorSouth = nearInteriorTile(wx, wy, 0, 1);
        boolean floorWest = nearInteriorTile(wx, wy, -1, 0);
        boolean floorEast = nearInteriorTile(wx, wy, 1, 0);
        boolean wallNorth = wallConnection(wx, wy, 0, -1);
        boolean wallSouth = wallConnection(wx, wy, 0, 1);
        boolean wallWest = wallConnection(wx, wy, -1, 0);
        boolean wallEast = wallConnection(wx, wy, 1, 0);
        boolean straightVertical = (wallNorth || wallSouth) && !wallWest && !wallEast;
        String asset = connectedInteriorWallAsset(wx, wy, seed, floorNorth, floorSouth, floorWest, floorEast);

        if (drawsTallBackWall(wx, wy, floorNorth, floorSouth)) {
            drawTallBackWallLayer(g, wx, wy, px, py, ts, seed);
        }
        if (straightVertical && (floorWest || floorEast)) {
            drawLinearVerticalWallShade(g, px, py, ts, floorWest, floorEast);
        }
        if (floorSouth) {
            g.setColor(new Color(0, 0, 0, 92));
            g.fillRect(px + scaled(3), py + ts - scaled(2), ts - scaled(3), scaled(5));
        }
        g.drawImage(assets.image(asset, ts, ts), px, py, null);

        g.setColor(new Color(255, 225, 150, 38));
        g.fillRect(px + scaled(3), py + scaled(2), ts - scaled(6), scaled(2));
        g.setColor(new Color(0, 0, 0, 58));
        g.fillRect(px + scaled(3), py + ts - scaled(7), ts - scaled(6), scaled(3));
        drawInteriorWallJoinAccents(g, px, py, ts, wallNorth, wallEast, wallSouth, wallWest);
        if (floorEast) {
            g.setColor(new Color(0, 0, 0, 54));
            g.fillRect(px + ts - scaled(5), py + scaled(6), scaled(4), ts - scaled(11));
        }
        if (floorWest) {
            g.setColor(new Color(255, 226, 156, 36));
            g.fillRect(px + scaled(1), py + scaled(6), scaled(3), ts - scaled(11));
        }
    }

    private void drawLinearVerticalWallShade(Graphics2D g, int px, int py, int ts, boolean floorWest, boolean floorEast) {
        if (floorEast) {
            g.setColor(new Color(0, 0, 0, 50));
            g.fillRect(px + ts - tileRelative(5, ts), py + tileRelative(4, ts), tileRelative(4, ts), ts - tileRelative(8, ts));
        }
        if (floorWest) {
            g.setColor(new Color(255, 226, 156, 30));
            g.fillRect(px + tileRelative(1, ts), py + tileRelative(4, ts), tileRelative(3, ts), ts - tileRelative(8, ts));
        }
    }

    private void drawInteriorWallJoinAccents(Graphics2D g, int px, int py, int ts,
                                             boolean north, boolean east, boolean south, boolean west) {
        if (!((north || south) && (east || west))) {
            return;
        }
        int centerX = px + ts / 2;
        int capY = py + tileRelative(6, ts);
        int trimY = py + tileRelative(24, ts);
        int trimH = Math.max(2, tileRelative(5, ts));
        int postW = Math.max(3, tileRelative(7, ts));
        int postX = centerX - postW / 2;

        g.setColor(new Color(42, 25, 22, 128));
        if (north) {
            g.fillRect(postX, py + tileRelative(2, ts), postW, ts / 2);
        }
        if (south) {
            g.fillRect(postX, py + ts / 2, postW, ts - tileRelative(5, ts) - ts / 2);
        }
        if (west) {
            g.fillRect(px, trimY, ts / 2, trimH);
        }
        if (east) {
            g.fillRect(centerX, trimY, ts / 2, trimH);
        }

        g.setColor(new Color(137, 86, 48, 178));
        if (north || south) {
            g.fillRect(postX + Math.max(1, tileRelative(1, ts)), py + tileRelative(4, ts),
                    Math.max(1, postW - tileRelative(2, ts)), ts - tileRelative(10, ts));
        }
        if (west) {
            g.fillRect(px, capY, ts / 2 + postW / 2, Math.max(2, tileRelative(4, ts)));
        }
        if (east) {
            g.fillRect(centerX - postW / 2, capY, ts / 2 + postW / 2, Math.max(2, tileRelative(4, ts)));
        }

        g.setColor(new Color(255, 229, 167, 46));
        if (west) {
            g.fillRect(px, capY, ts / 2, Math.max(1, tileRelative(1, ts)));
        }
        if (east) {
            g.fillRect(centerX, capY, ts / 2, Math.max(1, tileRelative(1, ts)));
        }
    }

    private boolean drawsTallBackWall(int wx, int wy, boolean floorNorth, boolean floorSouth) {
        return floorSouth && !floorNorth && state.world.tileAt(state.currentMapId, wx, wy - 1) == 'x';
    }

    private void drawTallBackWallLayer(Graphics2D g, int wx, int wy, int px, int py, int ts, int seed) {
        int topY = py - ts;
        g.drawImage(assets.image("interior_wall_horizontal_center", ts, ts), px, topY, null);
        g.setColor(new Color(255, 226, 156, 48));
        g.fillRect(px + scaled(4), topY + scaled(4), ts - scaled(8), scaled(3));
        g.setColor(new Color(24, 13, 11, 96));
        g.fillRect(px + scaled(3), py - scaled(5), ts - scaled(6), scaled(8));
        g.setColor(new Color(0, 0, 0, 48));
        g.fillRect(px + ts - scaled(4), topY + scaled(7), scaled(3), ts - scaled(9));
    }

    private void drawHouseDoorTile(Graphics2D g, int wx, int wy, int px, int py) {
        int ts = tileSize();
        boolean horizontal = wallConnection(wx, wy, -1, 0) || wallConnection(wx, wy, 1, 0)
                || state.world.tileAt(state.currentMapId, wx - 1, wy) == 'e'
                || state.world.tileAt(state.currentMapId, wx + 1, wy) == 'e';
        if (horizontal) {
            g.drawImage(assets.image("interior_wall_horizontal_center", ts, ts), px, py, null);
        } else {
            boolean floorWest = nearInteriorTile(wx, wy, -1, 0);
            boolean floorEast = nearInteriorTile(wx, wy, 1, 0);
            g.drawImage(assets.image(verticalInteriorWallAsset(floorWest, floorEast), ts, ts), px, py, null);
        }
        String asset = horizontal ? "interior_wall_door_h_open" : "interior_wall_door_v_open";
        g.drawImage(assets.image(asset, ts, ts), px, py, null);
        drawInteriorDoorFrameBlend(g, px, py, ts, horizontal);
        g.setColor(new Color(0, 0, 0, 68));
        g.fillRect(px + scaled(4), py + ts - scaled(5), ts - scaled(8), scaled(3));
    }

    private void drawInteriorDoorFrameBlend(Graphics2D g, int px, int py, int ts, boolean horizontal) {
        int side = Math.max(2, tileRelative(5, ts));
        int cap = Math.max(2, tileRelative(6, ts));
        g.setColor(new Color(74, 44, 31, 164));
        if (horizontal) {
            g.fillRect(px, py + tileRelative(2, ts), side, ts - tileRelative(5, ts));
            g.fillRect(px + ts - side, py + tileRelative(2, ts), side, ts - tileRelative(5, ts));
            g.fillRect(px, py, ts, cap);
            g.setColor(new Color(190, 134, 75, 96));
            g.fillRect(px + side, py + tileRelative(2, ts), ts - side * 2, Math.max(1, tileRelative(2, ts)));
        } else {
            g.fillRect(px + tileRelative(2, ts), py, ts - tileRelative(5, ts), cap);
            g.fillRect(px + tileRelative(2, ts), py + ts - cap, ts - tileRelative(5, ts), cap);
            g.fillRect(px + tileRelative(1, ts), py, side, ts);
            g.fillRect(px + ts - side - tileRelative(1, ts), py, side, ts);
            g.setColor(new Color(190, 134, 75, 82));
            g.fillRect(px + tileRelative(3, ts), py + cap, Math.max(1, tileRelative(2, ts)), ts - cap * 2);
        }
    }

    private boolean exteriorEntranceDoor(int wx, int wy) {
        if (state.world.tileAt(state.currentMapId, wx, wy) != 'e') {
            return false;
        }
        boolean floorInside = nearInteriorTile(wx, wy, 0, -1);
        boolean outsideBelow = state.world.tileAt(state.currentMapId, wx, wy + 1) == 'x';
        boolean pairedDoor = state.world.tileAt(state.currentMapId, wx - 1, wy) == 'e'
                || state.world.tileAt(state.currentMapId, wx + 1, wy) == 'e';
        return floorInside && outsideBelow && pairedDoor;
    }

    private String connectedInteriorWallAsset(int wx, int wy, int seed,
                                              boolean floorNorth, boolean floorSouth,
                                              boolean floorWest, boolean floorEast) {
        boolean north = wallConnection(wx, wy, 0, -1);
        boolean east = wallConnection(wx, wy, 1, 0);
        boolean south = wallConnection(wx, wy, 0, 1);
        boolean west = wallConnection(wx, wy, -1, 0);
        int count = (north ? 1 : 0) + (east ? 1 : 0) + (south ? 1 : 0) + (west ? 1 : 0);

        if (count >= 3) {
            return "interior_wall_horizontal_center";
        }
        if ((east && west && (north || south)) || (north && south && (east || west))) {
            return "interior_wall_horizontal_center";
        }
        if (north && south && !east && !west) {
            return verticalInteriorWallAsset(floorWest, floorEast);
        }
        if (east && south && !north && !west) {
            return "interior_wall_outer_tl";
        }
        if (west && south && !north && !east) {
            return "interior_wall_outer_tr";
        }
        if (east && north && !south && !west) {
            return "interior_wall_outer_bl";
        }
        if (west && north && !south && !east) {
            return "interior_wall_outer_br";
        }
        if ((west || east) && !north && !south) {
            if (east && !west) {
                return "interior_wall_horizontal_left";
            }
            if (west && !east) {
                return "interior_wall_horizontal_right";
            }
            return "interior_wall_horizontal_center";
        }
        if ((north || south) && !west && !east) {
            return verticalInteriorWallAsset(floorWest, floorEast);
        }
        if (floorEast && !floorWest) {
            return verticalInteriorWallAsset(false, true);
        }
        if (floorWest && !floorEast) {
            return verticalInteriorWallAsset(true, false);
        }
        return "interior_wall_horizontal_center";
    }

    private String verticalInteriorWallAsset(boolean floorWest, boolean floorEast) {
        if (floorWest && !floorEast) {
            return "interior_wall_side_right";
        }
        if (floorEast && !floorWest) {
            return "interior_wall_side_left";
        }
        return "interior_wall_vertical_side";
    }

    private void drawInteriorFloorDepth(Graphics2D g, int px, int py, int ts, int seed) {
        if (Math.floorMod(seed, 13) == 0) {
            g.setColor(new Color(255, 230, 172, 12));
            g.fillRect(px, py, ts, ts);
        }
    }

    private String interiorFloorAsset(int seed) {
        return "interior_floor_blend";
    }

    private boolean wallConnection(int wx, int wy, int ox, int oy) {
        char tile = state.world.tileAt(state.currentMapId, wx + ox, wy + oy);
        return tile == 'o' || (tile == 'e' && doorConnectsWall(wx + ox, wy + oy));
    }

    private boolean doorConnectsWall(int wx, int wy) {
        char tile = state.world.tileAt(state.currentMapId, wx, wy);
        if (tile != 'e') {
            return false;
        }
        boolean horizontal = state.world.tileAt(state.currentMapId, wx - 1, wy) == 'o'
                || state.world.tileAt(state.currentMapId, wx + 1, wy) == 'o';
        boolean vertical = state.world.tileAt(state.currentMapId, wx, wy - 1) == 'o'
                || state.world.tileAt(state.currentMapId, wx, wy + 1) == 'o';
        return horizontal || vertical;
    }

    private boolean nearInteriorTile(int wx, int wy, int ox, int oy) {
        char tile = state.world.tileAt(state.currentMapId, wx + ox, wy + oy);
        return tile == 'i' || tile == 'e' || tile == 'z' || tile == 'k';
    }

    private void drawHouseFurnitureTile(Graphics2D g, int wx, int wy, int px, int py) {
        drawHouseFloorTile(g, 'i', wx, wy, px, py);
    }

    private void drawTerrainEdges(Graphics2D g, char tile, int wx, int wy, int px, int py) {
        if (!"overworld".equals(state.world.kind(state.currentMapId))) {
            return;
        }
        int[][] dirs = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        for (int i = 0; i < dirs.length; i++) {
            int nx = wx + dirs[i][0];
            int ny = wy + dirs[i][1];
            char other = visibleTerrainTile(state.world.tileAt(state.currentMapId, nx, ny), nx, ny);
                if (other == tile || Terrain.connectingRoad(other) || Terrain.connectingRoad(tile)) {
                    continue;
                }
            if (!shouldBlend(tile, other)) {
                continue;
            }
            drawTerrainTextureBlend(g, tile, other, i, wx, wy, nx, ny, px, py);
        }
        drawDiagonalTerrainBlend(g, tile, wx, wy, px, py);
    }

    private void drawSettlementSurfaceSeams(Graphics2D g, int camX, int camY) {
        String kind = state.world.kind(state.currentMapId);
        if (!"city".equals(kind) && !"village".equals(kind)) {
            return;
        }
        int ts = tileSize();
        int seam = Math.max(2, tileRelative("city".equals(kind) ? 8 : 6, ts));
        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver);
        for (int sy = 0; sy < lastVisibleRows; sy++) {
            for (int sx = 0; sx < lastVisibleCols; sx++) {
                int wx = camX + sx;
                int wy = camY + sy;
                char tile = state.world.tileAt(state.currentMapId, wx, wy);
                if (!isSettlementSurface(tile)) {
                    continue;
                }
                int px = sx * ts;
                int py = sy * ts;
                char east = state.world.tileAt(state.currentMapId, wx + 1, wy);
                if (isSameSettlementSurfaceBand(tile, east)) {
                    g.setColor(settlementSurfaceSeamColor(tile, east));
                    g.fillRect(px + ts - seam / 2, py, seam, ts);
                }
                char south = state.world.tileAt(state.currentMapId, wx, wy + 1);
                if (isSameSettlementSurfaceBand(tile, south)) {
                    g.setColor(settlementSurfaceSeamColor(tile, south));
                    g.fillRect(px, py + ts - seam / 2, ts, seam);
                }
            }
        }
        g.setComposite(oldComposite);
    }

    private boolean isSettlementSurface(char tile) {
        return "pCjlayGV".indexOf(tile) >= 0;
    }

    private boolean isSameSettlementSurfaceBand(char a, char b) {
        if (!isSettlementSurface(a) || !isSettlementSurface(b)) {
            return false;
        }
        return settlementSurfaceBand(a) == settlementSurfaceBand(b);
    }

    private int settlementSurfaceBand(char tile) {
        if (tile == 'G' || tile == 'V' || tile == 'y') {
            return 1;
        }
        if (tile == 'a') {
            return 2;
        }
        return 0;
    }

    private Color settlementSurfaceSeamColor(char a, char b) {
        Color ca = Terrain.color(a);
        Color cb = Terrain.color(b);
        int alpha = settlementSurfaceBand(a) == 0 ? 72 : 46;
        return new Color((ca.getRed() + cb.getRed()) / 2,
                (ca.getGreen() + cb.getGreen()) / 2,
                (ca.getBlue() + cb.getBlue()) / 2,
                alpha);
    }

    private boolean shouldBlend(char tile, char other) {
        return isNatural(tile) && isNatural(other) && tile != other;
    }

    private boolean isNatural(char tile) {
        return "gfsnvbmqwP~".indexOf(tile) >= 0;
    }

    private Color edgeColor(char tile, char other) {
        if (tile == 'w' || other == 'w' || tile == '~' || other == '~') {
            return new Color(122, 191, 139, 170);
        }
        Color a = Terrain.color(tile);
        Color b = Terrain.color(other);
        return new Color((a.getRed() + b.getRed()) / 2, (a.getGreen() + b.getGreen()) / 2, (a.getBlue() + b.getBlue()) / 2, 112);
    }

    private void drawTerrainTextureBlend(Graphics2D g, char tile, char other, int direction,
                                         int wx, int wy, int nx, int ny, int px, int py) {
        int ts = tileSize();
        int seed = terrainTransitionSeed(wx, wy, direction, other);
        Polygon transition = terrainTransitionPolygon(px, py, ts, direction, seed);
        BufferedImage texture = assets.image(terrainImageName(other, nx, ny), ts, ts);

        Graphics2D blend = (Graphics2D) g.create();
        blend.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        blend.setClip(transition);
        blend.setComposite(AlphaComposite.SrcOver.derive(terrainTextureAlpha(tile, other)));
        blend.drawImage(texture, px, py, null);
        blend.setComposite(AlphaComposite.SrcOver.derive(terrainTintAlpha(tile, other)));
        blend.setColor(edgeColor(tile, other));
        blend.fill(transition);
        blend.dispose();

        drawTerrainTransitionFlecks(g, tile, other, direction, wx, wy, px, py, seed);
    }

    private Polygon terrainTransitionPolygon(int px, int py, int ts, int direction, int seed) {
        int shallow = scaled(3 + Math.floorMod(seed, 4));
        int mid = scaled(7 + Math.floorMod(seed / 7, 7));
        int deep = scaled(11 + Math.floorMod(seed / 17, 9));
        int a = Math.floorMod(seed / 31, Math.max(1, ts / 3)) - ts / 6;
        int b = Math.floorMod(seed / 53, Math.max(1, ts / 3)) - ts / 6;
        return switch (direction) {
            case 0 -> new Polygon(
                    new int[]{px, px + ts / 3 + a, px + ts * 2 / 3 + b, px + ts, px + ts, px},
                    new int[]{py, py + mid, py + shallow, py + deep, py, py},
                    6);
            case 1 -> new Polygon(
                    new int[]{px + ts, px + ts, px + ts - deep, px + ts - shallow, px + ts - mid, px + ts},
                    new int[]{py, py + ts, py + ts, py + ts * 2 / 3 + b, py + ts / 3 + a, py},
                    6);
            case 2 -> new Polygon(
                    new int[]{px, px + ts, px + ts, px + ts * 2 / 3 + b, px + ts / 3 + a, px},
                    new int[]{py + ts, py + ts, py + ts - mid, py + ts - deep, py + ts - shallow, py + ts},
                    6);
            case 3 -> new Polygon(
                    new int[]{px, px + mid, px + shallow, px + deep, px, px},
                    new int[]{py, py + ts / 3 + a, py + ts * 2 / 3 + b, py + ts, py + ts, py},
                    6);
            default -> new Polygon();
        };
    }

    private void drawDiagonalTerrainBlend(Graphics2D g, char tile, int wx, int wy, int px, int py) {
        int[][] diagonals = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
        int ts = tileSize();
        for (int i = 0; i < diagonals.length; i++) {
            int nx = wx + diagonals[i][0];
            int ny = wy + diagonals[i][1];
            char other = visibleTerrainTile(state.world.tileAt(state.currentMapId, nx, ny), nx, ny);
            if (!shouldBlend(tile, other)) {
                continue;
            }
            char sideA = visibleTerrainTile(state.world.tileAt(state.currentMapId, wx + diagonals[i][0], wy), wx + diagonals[i][0], wy);
            char sideB = visibleTerrainTile(state.world.tileAt(state.currentMapId, wx, wy + diagonals[i][1]), wx, wy + diagonals[i][1]);
            if (sideA == other || sideB == other) {
                continue;
            }
            int seed = terrainTransitionSeed(wx, wy, i + 8, other);
            int radius = scaled(10 + Math.floorMod(seed, 7));
            Polygon corner = switch (i) {
                case 0 -> new Polygon(new int[]{px, px + radius, px}, new int[]{py, py, py + radius}, 3);
                case 1 -> new Polygon(new int[]{px + ts, px + ts, px + ts - radius}, new int[]{py, py + radius, py}, 3);
                case 2 -> new Polygon(new int[]{px + ts, px + ts - radius, px + ts}, new int[]{py + ts, py + ts, py + ts - radius}, 3);
                default -> new Polygon(new int[]{px, px, px + radius}, new int[]{py + ts, py + ts - radius, py + ts}, 3);
            };
            Graphics2D blend = (Graphics2D) g.create();
            blend.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            blend.setClip(corner);
            blend.setComposite(AlphaComposite.SrcOver.derive(terrainTextureAlpha(tile, other) * 0.72f));
            blend.drawImage(assets.image(terrainImageName(other, nx, ny), ts, ts), px, py, null);
            blend.setComposite(AlphaComposite.SrcOver.derive(terrainTintAlpha(tile, other) * 0.65f));
            blend.setColor(edgeColor(tile, other));
            blend.fill(corner);
            blend.dispose();
        }
    }

    private void drawTerrainTransitionFlecks(Graphics2D g, char tile, char other, int direction,
                                             int wx, int wy, int px, int py, int seed) {
        int ts = tileSize();
        int count = isWaterLike(tile) || isWaterLike(other) ? 7 : 10;
        Graphics2D flecks = (Graphics2D) g.create();
        flecks.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        flecks.setComposite(AlphaComposite.SrcOver.derive(isWaterLike(tile) || isWaterLike(other) ? 0.26f : 0.20f));
        for (int i = 0; i < count; i++) {
            int local = terrainTransitionSeed(wx + i, wy - i, direction + i * 3, other);
            int along = Math.floorMod(local / 11, Math.max(1, ts - scaled(8))) + scaled(4);
            int depth = scaled(2 + Math.floorMod(local / 37, 11));
            int jitter = scaled(Math.floorMod(local / 71, 7) - 3);
            int x = switch (direction) {
                case 1 -> px + ts - depth;
                case 3 -> px + depth;
                default -> px + along;
            };
            int y = switch (direction) {
                case 0 -> py + depth;
                case 2 -> py + ts - depth;
                default -> py + along;
            };
            if (direction == 0 || direction == 2) {
                x += jitter;
            } else {
                y += jitter;
            }
            int w = Math.max(1, scaled(2 + Math.floorMod(local, 4)));
            int h = Math.max(1, scaled(1 + Math.floorMod(local / 5, 3)));
            flecks.setColor(terrainFleckColor(tile, other, local));
            flecks.fillOval(x - w / 2, y - h / 2, w, h);
        }
        flecks.dispose();
    }

    private int terrainTransitionSeed(int wx, int wy, int direction, char other) {
        return Math.abs(wx * 928371 + wy * 364479 + direction * 8191 + other * 131 + state.currentMapId.hashCode());
    }

    private float terrainTextureAlpha(char tile, char other) {
        if (isWaterLike(tile) || isWaterLike(other)) {
            return 0.28f;
        }
        return 0.24f;
    }

    private float terrainTintAlpha(char tile, char other) {
        if (isWaterLike(tile) || isWaterLike(other)) {
            return 0.08f;
        }
        return 0.06f;
    }

    private boolean isWaterLike(char tile) {
        return tile == 'w' || tile == '~';
    }

    private Color terrainFleckColor(char tile, char other, int seed) {
        if (isWaterLike(tile) || isWaterLike(other)) {
            return Math.floorMod(seed, 3) == 0
                    ? new Color(197, 224, 203, 138)
                    : new Color(66, 129, 113, 126);
        }
        Color a = Terrain.color(tile);
        Color b = Terrain.color(other);
        double mix = 0.34 + Math.floorMod(seed, 32) / 100.0;
        int r = clampColor((int) Math.round(a.getRed() * (1.0 - mix) + b.getRed() * mix));
        int gr = clampColor((int) Math.round(a.getGreen() * (1.0 - mix) + b.getGreen() * mix));
        int bl = clampColor((int) Math.round(a.getBlue() * (1.0 - mix) + b.getBlue() * mix));
        return new Color(r, gr, bl, 132);
    }

    private void drawWaterAnimation(Graphics2D g, int wx, int wy, int px, int py, int tileSize) {
        double wave = weather.waterWave();
        int seed = Math.abs(wx * 928371 + wy * 364479 + state.currentMapId.hashCode());
        Composite oldComposite = g.getComposite();
        Stroke oldStroke = g.getStroke();
        Paint oldPaint = g.getPaint();
        Object oldAntialiasing = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        waterTileRenderer.draw(g, wx, wy, px, py, tileSize, frame, state.currentMapId, weather, weatherQuality());
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double rainIntensity = weather.rainIntensity();
        double stormIntensity = weather.stormIntensity();
        double rippleThreshold = weatherQuality().waterRippleThreshold;
        if (rainIntensity > rippleThreshold) {
            drawWaterRainRings(g, wx, wy, px, py, tileSize, seed, false, rainIntensity);
        }
        if (stormIntensity > rippleThreshold) {
            drawWaterRainRings(g, wx, wy, px, py, tileSize, seed, true, stormIntensity);
        }
        drawWaterShoreFoam(g, wx, wy, px, py, tileSize, wave);
        g.setComposite(oldComposite);
        g.setStroke(oldStroke);
        g.setPaint(oldPaint);
        if (oldAntialiasing != null) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialiasing);
        }
    }

    private void drawWaterRainRings(Graphics2D g, int wx, int wy, int px, int py, int tileSize, int seed, boolean heavy, double intensity) {
        int rings = (int) Math.round((heavy ? 4 : 2) * intensity);
        if (rings <= 0) {
            return;
        }
        for (int i = 0; i < rings; i++) {
            int phase = Math.floorMod(frame * (heavy ? 2 : 1) + seed / (i + 3) + i * 13, 32);
            if (phase > 20) {
                continue;
            }
            int cx = px + Math.floorMod(seed / (i + 5) + i * 17, Math.max(1, tileSize));
            int cy = py + Math.floorMod(seed / (i + 7) + i * 23, Math.max(1, tileSize));
            int radiusX = scaled(3) + phase * scaled(9) / 20;
            int radiusY = Math.max(1, radiusX / 3);
            float alpha = (float) ((heavy ? 0.33f : 0.22f) * (1.0f - phase / 22.0f) * intensity);
            g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0.0f, alpha)));
            g.setColor(new Color(207, 235, 246));
            g.drawOval(cx - radiusX, cy - radiusY, radiusX * 2, radiusY * 2);
        }
    }

    private void drawWaterShoreFoam(Graphics2D g, int wx, int wy, int px, int py, int tileSize, double wave) {
        float alpha = (float) (0.16 + wave * 0.25);
        g.setComposite(AlphaComposite.SrcOver.derive(alpha));
        g.setColor(new Color(212, 238, 225));
        g.setStroke(new BasicStroke(Math.max(1f, scaledStroke(1.0f + (float) wave)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int inset = scaled(2);
        int wobble = (int) Math.round(Math.sin(frame * 0.09 + wx * 0.6 + wy * 0.4) * scaled(2));
        if (!isWaterTile(wx, wy - 1)) {
            g.drawLine(px + scaled(5), py + inset + wobble, px + tileSize - scaled(5), py + inset - wobble);
        }
        if (!isWaterTile(wx + 1, wy)) {
            g.drawLine(px + tileSize - inset + wobble, py + scaled(5), px + tileSize - inset - wobble, py + tileSize - scaled(5));
        }
        if (!isWaterTile(wx, wy + 1)) {
            g.drawLine(px + scaled(5), py + tileSize - inset - wobble, px + tileSize - scaled(5), py + tileSize - inset + wobble);
        }
        if (!isWaterTile(wx - 1, wy)) {
            g.drawLine(px + inset - wobble, py + scaled(5), px + inset + wobble, py + tileSize - scaled(5));
        }
    }

    private boolean isWaterTile(int x, int y) {
        char tile = visibleTerrainTile(state.world.tileAt(state.currentMapId, x, y), x, y);
        return tile == 'w' || tile == '~';
    }

    private void drawFieldConnectors(Graphics2D g, int camX, int camY) {
        int ts = tileSize();
        int seam = Math.max(8, tileRelative(32, ts));
        int filler = ts;
        BufferedImage vertical = assets.image("field_connector_vertical", seam, ts);
        BufferedImage horizontal = assets.image("field_connector_horizontal", ts, seam);
        BufferedImage center = assets.image("field_junction_filler", filler, filler);
        for (int sy = 0; sy < lastVisibleRows; sy++) {
            for (int sx = 0; sx < lastVisibleCols; sx++) {
                int wx = camX + sx;
                int wy = camY + sy;
                if (!isFieldTile(wx, wy)) {
                    continue;
                }
                int px = sx * ts;
                int py = sy * ts;
                if (isFieldTile(wx + 1, wy)) {
                    g.drawImage(vertical, px + ts - seam / 2, py, null);
                }
                if (isFieldTile(wx, wy + 1)) {
                    g.drawImage(horizontal, px, py + ts - seam / 2, null);
                }
                if (isFieldTile(wx + 1, wy) && isFieldTile(wx, wy + 1) && isFieldTile(wx + 1, wy + 1)) {
                    g.drawImage(center, px + ts - filler / 2, py + ts - filler / 2, null);
                }
            }
        }
    }

    private boolean isFieldTile(int wx, int wy) {
        return state.world.tileAt(state.currentMapId, wx, wy) == 'A';
    }

    private void drawRoadConnectors(Graphics2D g, int camX, int camY) {
        int ts = tileSize();
        String mapKind = state.world.kind(state.currentMapId);
        boolean texturedRoads = isTexturedRoadMap(mapKind);
        for (int sy = 0; sy < lastVisibleRows; sy++) {
            for (int sx = 0; sx < lastVisibleCols; sx++) {
                int wx = camX + sx;
                int wy = camY + sy;
                char tile = state.world.tileAt(state.currentMapId, wx, wy);
                if (!Terrain.connectingRoad(tile)) {
                    continue;
                }
                if (isSettlementRoadNode(tile)) {
                    continue;
                }
                int px = sx * ts;
                int py = sy * ts;
                if (tile == 'B') {
                    drawBridgeTile(g, wx, wy, px, py);
                    continue;
                }
                if (drawsRoadOverlay(tile, mapKind)) {
                    drawTexturedRoadTile(g, wx, wy, px, py, tile, "village".equals(mapKind) || "city".equals(mapKind));
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
        if (texturedRoads) {
            drawTexturedRoadJunctionFillers(g, camX, camY, "village".equals(mapKind) || "city".equals(mapKind));
        }
    }

    private boolean isSettlementRoadNode(char tile) {
        return tile == 'c' || tile == 'u' || tile == 'd';
    }

    private void drawTexturedRoadTile(Graphics2D g, int wx, int wy, int px, int py, char tile, boolean settlementRoad) {
        int ts = tileSize();
        if (tile == Terrain.VILLAGE_ROAD || tile == Terrain.COBBLESTONE_ROAD) {
            drawRoadMaterialTexture(g, wx, wy, px, py, tile, settlementRoad);
        }
        int bits = roadBits(wx, wy);
        String suffix = "0" + Integer.toHexString(bits);
        String asset = "road_overlay_" + suffix.substring(suffix.length() - 2);
        float alpha = roadOverlayAlpha(tile);
        Composite oldComposite = g.getComposite();
        if (alpha < 1f) {
            g.setComposite(AlphaComposite.SrcOver.derive(alpha));
        }
        g.drawImage(assets.image(asset, ts, ts), px, py, null);
        g.setComposite(oldComposite);
    }

    private float roadOverlayAlpha(char tile) {
        return tile == Terrain.VILLAGE_ROAD || tile == Terrain.COBBLESTONE_ROAD ? 0.34f : 1.0f;
    }

    private void drawRoadMaterialTexture(Graphics2D g, int wx, int wy, int px, int py, char tile, boolean settlementRoad) {
        int ts = tileSize();
        BufferedImage material = assets.image(Terrain.assetName(tile), ts, ts);
        Graphics2D road = (Graphics2D) g.create();
        road.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        road.setPaint(new TexturePaint(material, new Rectangle(px, py, ts, ts)));
        drawRoadShape(road, px, py, wx, wy, tileRelative(settlementRoad ? 34 : 28, ts));
        road.dispose();
    }

    private void drawTexturedRoadJunctionFillers(Graphics2D g, int camX, int camY, boolean settlementRoad) {
        int ts = tileSize();
        int size = Math.max(6, tileRelative(settlementRoad ? 18 : 16, ts));
        BufferedImage filler = assets.image("road_junction_filler", size, size);
        for (int sy = 0; sy < lastVisibleRows - 1; sy++) {
            for (int sx = 0; sx < lastVisibleCols - 1; sx++) {
                int wx = camX + sx;
                int wy = camY + sy;
                if (!isRoadJunctionFill(wx, wy)) {
                    continue;
                }
                int px = (sx + 1) * ts - size / 2;
                int py = (sy + 1) * ts - size / 2;
                Composite oldComposite = g.getComposite();
                float alpha = roadJunctionFillerAlpha(wx, wy);
                if (alpha <= 0f) {
                    continue;
                }
                if (alpha < 1f) {
                    g.setComposite(AlphaComposite.SrcOver.derive(alpha));
                }
                g.drawImage(filler, px, py, null);
                g.setComposite(oldComposite);
            }
        }
    }

    private float roadJunctionFillerAlpha(int wx, int wy) {
        char a = state.world.tileAt(state.currentMapId, wx, wy);
        char b = state.world.tileAt(state.currentMapId, wx + 1, wy);
        char c = state.world.tileAt(state.currentMapId, wx, wy + 1);
        char d = state.world.tileAt(state.currentMapId, wx + 1, wy + 1);
        return a == Terrain.VILLAGE_ROAD || b == Terrain.VILLAGE_ROAD || c == Terrain.VILLAGE_ROAD || d == Terrain.VILLAGE_ROAD
                || a == Terrain.COBBLESTONE_ROAD || b == Terrain.COBBLESTONE_ROAD || c == Terrain.COBBLESTONE_ROAD || d == Terrain.COBBLESTONE_ROAD
                ? 0.0f : 1.0f;
    }

    private boolean isRoadJunctionFill(int wx, int wy) {
        return isTexturedRoadTile(wx, wy)
                && isTexturedRoadTile(wx + 1, wy)
                && isTexturedRoadTile(wx, wy + 1)
                && isTexturedRoadTile(wx + 1, wy + 1);
    }

    private boolean isTexturedRoadTile(int wx, int wy) {
        char tile = state.world.tileAt(state.currentMapId, wx, wy);
        return Terrain.texturedRoad(tile);
    }

    private void drawContinuousRoadUnderlay(Graphics2D g, int wx, int wy, int px, int py, char tile, boolean settlementRoad) {
        int ts = tileSize();
        Graphics2D road = (Graphics2D) g.create();
        road.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color terrain = Terrain.color(roadUnderlayTile(tile, wx, wy));
        Color shoulderBase = roadShoulderColor(tile);
        Color coreBase = roadCoreColor(tile);
        drawRoadStroke(road, wx, wy, px, py,
                Math.min(ts + tileRelative(settlementRoad ? 22 : 16, ts), tileRelative(settlementRoad ? 56 : 50, ts)),
                roadBlendColor(terrain, shoulderBase, 0.28, settlementRoad ? 72 : 52));
        drawRoadStroke(road, wx, wy, px, py,
                Math.min(ts + tileRelative(settlementRoad ? 14 : 10, ts), tileRelative(settlementRoad ? 48 : 44, ts)),
                roadBlendColor(terrain, shoulderBase, 0.58, settlementRoad ? 110 : 82));
        drawRoadStroke(road, wx, wy, px, py,
                Math.min(ts + tileRelative(settlementRoad ? 6 : 2, ts), tileRelative(settlementRoad ? 42 : 38, ts)),
                roadWithAlpha(shoulderBase, settlementRoad ? 145 : 112));
        drawRoadStroke(road, wx, wy, px, py,
                Math.min(ts - tileRelative(settlementRoad ? 2 : 6, ts), tileRelative(settlementRoad ? 36 : 30, ts)),
                roadWithAlpha(coreBase, settlementRoad ? 178 : 142));
        road.dispose();
    }

    private Color roadShoulderColor(char tile) {
        return switch (tile) {
            case 'q' -> new Color(116, 112, 101);
            case 'K' -> new Color(124, 119, 109);
            case 'T' -> new Color(132, 105, 65);
            default -> new Color(156, 118, 70);
        };
    }

    private Color roadCoreColor(char tile) {
        return switch (tile) {
            case 'q' -> new Color(126, 120, 106);
            case 'K' -> new Color(143, 137, 124);
            case 'T' -> new Color(143, 111, 68);
            default -> new Color(166, 126, 75);
        };
    }

    private Color roadHighlightColor(char tile, int alpha) {
        Color color = switch (tile) {
            case 'q' -> new Color(190, 184, 166);
            case 'K' -> new Color(213, 205, 187);
            case 'T' -> new Color(205, 163, 96);
            default -> new Color(222, 181, 111);
        };
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), clamp(alpha, 0, 255));
    }

    private boolean isSoftRoadMaterial(char tile) {
        return tile == Terrain.DIRT_ROAD || tile == Terrain.VILLAGE_ROAD || tile == 'q';
    }

    private boolean isTexturedRoadMap(String mapKind) {
        return "village".equals(mapKind) || "overworld".equals(mapKind) || "city".equals(mapKind);
    }

    private boolean drawsRoadUnderlay(char tile, String mapKind) {
        return Terrain.texturedRoad(tile) && ("overworld".equals(mapKind) || "village".equals(mapKind));
    }

    private boolean drawsRoadOverlay(char tile, String mapKind) {
        return Terrain.texturedRoad(tile) && isTexturedRoadMap(mapKind);
    }

    private boolean usesRoadBaseTexture(char tile, String mapKind) {
        return Terrain.texturedRoad(tile) && !drawsRoadUnderlay(tile, mapKind);
    }

    private boolean blendsRoadEdgeWithUnderlay(char tile, String mapKind) {
        return isSoftRoadMaterial(tile) && ("overworld".equals(mapKind) || "village".equals(mapKind));
    }

    private Color roadBlendColor(Color first, Color second, double amount, int alpha) {
        double t = clamp(amount, 0.0, 1.0);
        int red = (int) Math.round(first.getRed() + (second.getRed() - first.getRed()) * t);
        int green = (int) Math.round(first.getGreen() + (second.getGreen() - first.getGreen()) * t);
        int blue = (int) Math.round(first.getBlue() + (second.getBlue() - first.getBlue()) * t);
        return new Color(clamp(red, 0, 255), clamp(green, 0, 255), clamp(blue, 0, 255), clamp(alpha, 0, 255));
    }

    private Color roadWithAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), clamp(alpha, 0, 255));
    }

    private void drawRoadStroke(Graphics2D g, int wx, int wy, int px, int py, int width, Color color) {
        int ts = tileSize();
        int cx = px + ts / 2;
        int cy = py + ts / 2;
        int bleed = Math.max(2, width / 3);
        boolean north = connectsRoad(wx, wy - 1);
        boolean east = connectsRoad(wx + 1, wy);
        boolean south = connectsRoad(wx, wy + 1);
        boolean west = connectsRoad(wx - 1, wy);
        Stroke oldStroke = g.getStroke();
        g.setColor(color);
        g.setStroke(new BasicStroke(Math.max(1, width), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (!north && !east && !south && !west) {
            g.fillOval(cx - width / 2, cy - width / 2, width, width);
        }
        if (north) {
            g.drawLine(cx, cy, cx, py - bleed);
        }
        if (east) {
            g.drawLine(cx, cy, px + ts + bleed, cy);
        }
        if (south) {
            g.drawLine(cx, cy, cx, py + ts + bleed);
        }
        if (west) {
            g.drawLine(cx, cy, px - bleed, cy);
        }
        g.setStroke(oldStroke);
    }

    private void drawRoadJoinBlend(Graphics2D g, int wx, int wy, int px, int py, char tile, boolean settlementRoad) {
        int ts = tileSize();
        int blend = Math.max(2, tileRelative(settlementRoad ? 12 : 9, ts));
        Color color = roadWithAlpha(roadCoreColor(tile), settlementRoad ? 112 : 88);
        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver);
        g.setColor(color);
        if (connectsRoad(wx + 1, wy)) {
            g.fillRoundRect(px + ts - blend / 2, py + tileRelative(4, ts), blend, ts - tileRelative(8, ts), blend, blend);
        }
        if (connectsRoad(wx, wy + 1)) {
            g.fillRoundRect(px + tileRelative(4, ts), py + ts - blend / 2, ts - tileRelative(8, ts), blend, blend, blend);
        }
        if (connectsRoad(wx + 1, wy) && connectsRoad(wx, wy + 1)) {
            int r = tileRelative(settlementRoad ? 28 : 23, ts);
            g.fillOval(px + ts - r / 2, py + ts - r / 2, r, r);
        }
        if (connectsRoad(wx - 1, wy) && connectsRoad(wx, wy + 1)) {
            int r = tileRelative(settlementRoad ? 28 : 23, ts);
            g.fillOval(px - r / 2, py + ts - r / 2, r, r);
        }
        if (connectsRoad(wx + 1, wy) && connectsRoad(wx, wy - 1)) {
            int r = tileRelative(settlementRoad ? 28 : 23, ts);
            g.fillOval(px + ts - r / 2, py - r / 2, r, r);
        }
        if (connectsRoad(wx - 1, wy) && connectsRoad(wx, wy - 1)) {
            int r = tileRelative(settlementRoad ? 28 : 23, ts);
            g.fillOval(px - r / 2, py - r / 2, r, r);
        }
        g.setComposite(oldComposite);
    }

    private void drawRoadTextureUnifier(Graphics2D g, int wx, int wy, int px, int py, char tile, boolean settlementRoad) {
        int ts = tileSize();
        int width = Math.min(ts - tileRelative(settlementRoad ? 4 : 8, ts), tileRelative(settlementRoad ? 34 : 28, ts));
        Color color = roadWithAlpha(roadShoulderColor(tile), settlementRoad ? 72 : 52);
        Graphics2D road = (Graphics2D) g.create();
        road.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawRoadStroke(road, wx, wy, px, py, width, color);
        road.dispose();
    }

    private void drawRoadBodyTexture(Graphics2D g, int wx, int wy, int px, int py, char tile, boolean settlementRoad) {
        int ts = tileSize();
        int bits = roadBits(wx, wy);
        int half = tileRelative(settlementRoad ? 18 : 15, ts);
        int count = settlementRoad ? 22 : 16;
        int seed = Math.abs(wx * 928371 + wy * 364479 + tile * 97 + 11003);
        Graphics2D texture = (Graphics2D) g.create();
        texture.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int i = 0; i < count; i++) {
            int local = Math.abs(seed + i * 73471 + (seed >> (i % 11)));
            int lx = Math.floorMod(local / 7, Math.max(1, ts));
            int ly = Math.floorMod(local / 31, Math.max(1, ts));
            if (!roadLocalPointInside(lx, ly, bits, half, ts)) {
                int[] adjusted = projectRoadTexturePoint(lx, ly, bits, half, ts);
                lx = adjusted[0];
                ly = adjusted[1];
            }
            int fleckW = Math.max(1, tileRelative(1 + Math.floorMod(local, 3), ts));
            int fleckH = Math.max(1, tileRelative(1 + Math.floorMod(local / 13, 2), ts));
            boolean light = Math.floorMod(local / 17, 4) == 0;
            Color color = roadTextureFleckColor(tile, light);
            texture.setColor(color);
            if (Math.floorMod(local / 23, 3) == 0) {
                texture.setStroke(new BasicStroke(Math.max(1f, scaledStroke(0.8f)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                texture.drawLine(px + lx - fleckW, py + ly, px + lx + fleckW * 2, py + ly + Math.floorMod(local / 53, 3) - 1);
            } else {
                texture.fillOval(px + lx, py + ly, fleckW, fleckH);
            }
        }
        texture.dispose();
    }

    private Color roadTextureFleckColor(char tile, boolean light) {
        if (light) {
            return roadHighlightColor(tile, 70);
        }
        Color core = roadCoreColor(tile);
        return new Color(
                Math.max(0, core.getRed() - 70),
                Math.max(0, core.getGreen() - 62),
                Math.max(0, core.getBlue() - 48),
                tile == Terrain.COBBLESTONE_ROAD ? 44 : 56
        );
    }

    private boolean roadLocalPointInside(int lx, int ly, int bits, int half, int ts) {
        int cx = ts / 2;
        int cy = ts / 2;
        if (bits == 0) {
            int dx = lx - cx;
            int dy = ly - cy;
            return dx * dx + dy * dy <= half * half;
        }
        if (Math.abs(lx - cx) <= half && Math.abs(ly - cy) <= half) {
            return true;
        }
        if ((bits & 1) != 0 && Math.abs(lx - cx) <= half && ly <= cy) {
            return true;
        }
        if ((bits & 2) != 0 && Math.abs(lx - cx) <= half && ly >= cy) {
            return true;
        }
        if ((bits & 4) != 0 && Math.abs(ly - cy) <= half && lx <= cx) {
            return true;
        }
        return (bits & 8) != 0 && Math.abs(ly - cy) <= half && lx >= cx;
    }

    private int[] projectRoadTexturePoint(int lx, int ly, int bits, int half, int ts) {
        int cx = ts / 2;
        int cy = ts / 2;
        boolean horizontal = (bits & 12) != 0;
        boolean vertical = (bits & 3) != 0;
        if (horizontal && (!vertical || Math.abs(ly - cy) < Math.abs(lx - cx))) {
            return new int[]{clamp(lx, 0, ts - 1), clamp(ly, cy - half + 1, cy + half - 1)};
        }
        if (vertical) {
            return new int[]{clamp(lx, cx - half + 1, cx + half - 1), clamp(ly, 0, ts - 1)};
        }
        return new int[]{clamp(lx, cx - half + 1, cx + half - 1), clamp(ly, cy - half + 1, cy + half - 1)};
    }

    private void drawRoadEdgeFeather(Graphics2D g, int wx, int wy, int px, int py, char tile, boolean settlementRoad) {
        int ts = tileSize();
        int bits = roadBits(wx, wy);
        int seed = Math.abs(wx * 374761393 + wy * 668265263 + tile * 41 + 2707);
        Graphics2D edge = (Graphics2D) g.create();
        edge.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color underlay = Terrain.color(roadUnderlayTile(tile, wx, wy));
        Color dust = tile == 'q' ? new Color(136, 126, 101) : new Color(177, 130, 73);
        edge.setColor(roadBlendColor(underlay, dust, 0.55, settlementRoad ? 54 : 42));
        int marks = settlementRoad ? 14 : 10;
        int half = tileRelative(settlementRoad ? 21 : 18, ts);
        for (int i = 0; i < marks; i++) {
            int local = Math.abs(seed + i * 92821 + (seed >> (i % 7)));
            int arm = chooseRoadArm(bits, local);
            int along = Math.floorMod(local / 19, Math.max(1, ts));
            int side = Math.floorMod(local / 43, 2) == 0 ? -1 : 1;
            int jitter = tileRelative(Math.floorMod(local / 71, 7) - 3, ts);
            int lx;
            int ly;
            if (arm == 1 || arm == 2) {
                lx = ts / 2 + side * half + jitter;
                ly = arm == 1 ? along / 2 : ts / 2 + along / 2;
            } else {
                lx = arm == 4 ? along / 2 : ts / 2 + along / 2;
                ly = ts / 2 + side * half + jitter;
            }
            int w = tileRelative(5 + Math.floorMod(local, 7), ts);
            int h = Math.max(1, tileRelative(2 + Math.floorMod(local / 11, 3), ts));
            edge.fillOval(px + lx - w / 2, py + ly - h / 2, w, h);
        }
        edge.dispose();
    }

    private int chooseRoadArm(int bits, int seed) {
        if (bits == 0) {
            return 8;
        }
        int[] arms = {1, 2, 4, 8};
        int available = 0;
        for (int arm : arms) {
            if ((bits & arm) != 0) {
                available++;
            }
        }
        int selected = Math.floorMod(seed, Math.max(1, available));
        for (int arm : arms) {
            if ((bits & arm) == 0) {
                continue;
            }
            if (selected == 0) {
                return arm;
            }
            selected--;
        }
        return 8;
    }

    private void drawVillageRoadTile(Graphics2D g, int wx, int wy, int px, int py, char tile) {
        int ts = tileSize();
        Graphics2D road = (Graphics2D) g.create();
        road.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color shoulder = tile == 'q'
                ? new Color(118, 116, 106, 115)
                : new Color(157, 122, 72, 118);
        Color base = tile == 'q'
                ? new Color(120, 117, 105, 178)
                : new Color(164, 126, 75, 176);
        Color light = tile == 'q'
                ? new Color(190, 184, 166, 58)
                : new Color(222, 181, 111, 58);
        drawVillageRoadStroke(road, wx, wy, px, py, Math.min(ts + tileRelative(8, ts), tileRelative(44, ts)), shoulder);
        drawVillageRoadStroke(road, wx, wy, px, py, Math.min(ts - tileRelative(4, ts), tileRelative(34, ts)), base);
        drawVillageRoadStroke(road, wx, wy, px, py, Math.min(ts - tileRelative(18, ts), tileRelative(18, ts)), light);
        road.dispose();
        drawRoadEdgeFlecking(g, wx, wy, px, py);
    }

    private void drawVillageRoadStroke(Graphics2D g, int wx, int wy, int px, int py, int width, Color color) {
        int ts = tileSize();
        int cx = px + ts / 2;
        int cy = py + ts / 2;
        int bleed = Math.max(2, width / 3);
        Stroke oldStroke = g.getStroke();
        g.setColor(color);
        g.setStroke(new BasicStroke(Math.max(1, width), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        boolean north = connectsRoad(wx, wy - 1);
        boolean east = connectsRoad(wx + 1, wy);
        boolean south = connectsRoad(wx, wy + 1);
        boolean west = connectsRoad(wx - 1, wy);
        if (!north && !east && !south && !west) {
            g.fillOval(cx - width / 2, cy - width / 2, width, width);
        }
        if (north) {
            g.drawLine(cx, cy, cx, py - bleed);
        }
        if (east) {
            g.drawLine(cx, cy, px + ts + bleed, cy);
        }
        if (south) {
            g.drawLine(cx, cy, cx, py + ts + bleed);
        }
        if (west) {
            g.drawLine(cx, cy, px - bleed, cy);
        }
        g.setStroke(oldStroke);
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
        return Terrain.connectingRoad(tile);
    }

    private char visibleTerrainTile(char tile, int wx, int wy) {
        String mapKind = state.world.kind(state.currentMapId);
        if (tile == 'B') {
            return 'w';
        }
        if (drawsRoadUnderlay(tile, mapKind)) {
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
        char[] candidates = {'g', 'f', 's', 'n', 'v', 'b', 'P', 'm', 'q'};
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
            case 'P' -> 7;
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
        char[] candidates = {'g', 'f', 's', 'n', 'v', 'b', 'P', 'm', 'q'};
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

    private void drawBridgeTile(Graphics2D g, int wx, int wy, int px, int py) {
        int ts = tileSize();
        int bits = roadBits(wx, wy);
        if (bits == 0) {
            bits = 3;
        }
        Graphics2D bridgeG = (Graphics2D) g.create();
        bridgeG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int centerX = px + ts / 2;
        int centerY = py + ts / 2;
        int edgeInset = scaled(1);
        drawBridgeStroke(bridgeG, centerX, centerY, px + ts / 2, py - edgeInset, bits, 1);
        drawBridgeStroke(bridgeG, centerX, centerY, px + ts / 2, py + ts + edgeInset, bits, 2);
        drawBridgeStroke(bridgeG, centerX, centerY, px - edgeInset, py + ts / 2, bits, 4);
        drawBridgeStroke(bridgeG, centerX, centerY, px + ts + edgeInset, py + ts / 2, bits, 8);
        if (isBridgeJoint(bits)) {
            bridgeG.setColor(new Color(64, 42, 28, 180));
            bridgeG.fillOval(centerX - scaled(18), centerY - scaled(18), scaled(36), scaled(36));
            bridgeG.setColor(new Color(139, 96, 55));
            bridgeG.fillOval(centerX - scaled(15), centerY - scaled(15), scaled(30), scaled(30));
        }

        drawBridgePlanks(bridgeG, px, py, bits);
        drawBridgeRails(bridgeG, px, py, bits);
        bridgeG.dispose();
    }

    private boolean isBridgeJoint(int bits) {
        return bits != 3 && bits != 12;
    }

    private void drawBridgeStroke(Graphics2D g, int cx, int cy, int ex, int ey, int bits, int bit) {
        if ((bits & bit) == 0) {
            return;
        }
        g.setStroke(new BasicStroke(scaled(36), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(49, 32, 22, 150));
        g.drawLine(cx, cy + scaled(2), ex, ey + scaled(2));
        g.setStroke(new BasicStroke(scaled(32), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(68, 45, 29, 210));
        g.drawLine(cx, cy, ex, ey);
        g.setStroke(new BasicStroke(scaled(26), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(142, 98, 55));
        g.drawLine(cx, cy, ex, ey);
        g.setStroke(new BasicStroke(scaled(18), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(174, 128, 73, 150));
        g.drawLine(cx, cy - scaled(1), ex, ey - scaled(1));
    }

    private void drawBridgePlanks(Graphics2D g, int px, int py, int bits) {
        int ts = tileSize();
        int c = ts / 2;
        g.setStroke(new BasicStroke(Math.max(1, scaled(1)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(73, 48, 31, 175));
        if ((bits & 1) != 0 || (bits & 2) != 0) {
            int startY = (bits & 1) != 0 ? scaled(3) : c - scaled(12);
            int endY = (bits & 2) != 0 ? ts - scaled(3) : c + scaled(12);
            for (int y = startY; y <= endY; y += scaled(7)) {
                g.drawLine(px + c - scaled(13), py + y, px + c + scaled(13), py + y);
            }
        }
        if ((bits & 4) != 0 || (bits & 8) != 0) {
            int startX = (bits & 4) != 0 ? scaled(3) : c - scaled(12);
            int endX = (bits & 8) != 0 ? ts - scaled(3) : c + scaled(12);
            for (int x = startX; x <= endX; x += scaled(7)) {
                g.drawLine(px + x, py + c - scaled(13), px + x, py + c + scaled(13));
            }
        }
    }

    private void drawBridgeRails(Graphics2D g, int px, int py, int bits) {
        int ts = tileSize();
        int c = ts / 2;
        int rail = scaled(16);
        g.setStroke(new BasicStroke(Math.max(2, scaled(3)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(55, 36, 24, 210));
        if ((bits & 1) != 0 || (bits & 2) != 0) {
            int startY = (bits & 1) != 0 ? -scaled(2) : c - scaled(14);
            int endY = (bits & 2) != 0 ? ts + scaled(2) : c + scaled(14);
            g.drawLine(px + c - rail, py + startY, px + c - rail, py + endY);
            g.drawLine(px + c + rail, py + startY, px + c + rail, py + endY);
        }
        if ((bits & 4) != 0 || (bits & 8) != 0) {
            int startX = (bits & 4) != 0 ? -scaled(2) : c - scaled(14);
            int endX = (bits & 8) != 0 ? ts + scaled(2) : c + scaled(14);
            g.drawLine(px + startX, py + c - rail, px + endX, py + c - rail);
            g.drawLine(px + startX, py + c + rail, px + endX, py + c + rail);
        }
        g.setStroke(new BasicStroke(Math.max(1, scaled(1)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(197, 151, 87, 145));
        if ((bits & 1) != 0 || (bits & 2) != 0) {
            int startY = (bits & 1) != 0 ? -scaled(2) : c - scaled(14);
            int endY = (bits & 2) != 0 ? ts + scaled(2) : c + scaled(14);
            g.drawLine(px + c - rail, py + startY, px + c - rail, py + endY);
            g.drawLine(px + c + rail, py + startY, px + c + rail, py + endY);
        }
        if ((bits & 4) != 0 || (bits & 8) != 0) {
            int startX = (bits & 4) != 0 ? -scaled(2) : c - scaled(14);
            int endX = (bits & 8) != 0 ? ts + scaled(2) : c + scaled(14);
            g.drawLine(px + startX, py + c - rail, px + endX, py + c - rail);
            g.drawLine(px + startX, py + c + rail, px + endX, py + c + rail);
        }
    }

    private void drawRoadEdgeFlecking(Graphics2D g, int wx, int wy, int px, int py) {
        int ts = tileSize();
        int seed = Math.abs(wx * 928371 + wy * 364479 + 31337);
        boolean north = connectsRoad(wx, wy - 1);
        boolean east = connectsRoad(wx + 1, wy);
        boolean south = connectsRoad(wx, wy + 1);
        boolean west = connectsRoad(wx - 1, wy);
        g.setColor(new Color(83, 63, 37, 48));
        for (int i = 0; i < 4; i++) {
            int offset = 5 + Math.floorMod(seed >> (i * 3), Math.max(1, ts - 10));
            int fleck = Math.max(1, tileRelative(1 + (seed + i) % 2, ts));
            if ((west || east) && !north) {
                g.fillOval(px + offset, py + tileRelative(5 + i % 2 * 3, ts), fleck, fleck);
            }
            if ((west || east) && !south) {
                g.fillOval(px + offset, py + ts - tileRelative(7 + i % 2 * 3, ts), fleck, fleck);
            }
            if ((north || south) && !west) {
                g.fillOval(px + tileRelative(5 + i % 2 * 3, ts), py + offset, fleck, fleck);
            }
            if ((north || south) && !east) {
                g.fillOval(px + ts - tileRelative(7 + i % 2 * 3, ts), py + offset, fleck, fleck);
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
        for (WorldMap.SettlementSite settlement : state.world.settlementSites()) {
            drawSettlement(
                    g,
                    camX,
                    camY,
                    settlement,
                    settlementOverlayAsset(settlement),
                    settlementOverlaySize(settlement)
            );
        }
    }

    private String settlementOverlayAsset(WorldMap.SettlementSite settlement) {
        if (state.world.isPlayerSettlement(settlement)) {
            return state.world.playerVillageSettlementStage().asset();
        }
        if ("City".equals(settlement.kind()) || "Town".equals(settlement.kind())) {
            return "city_overworld_cluster_large";
        }
        return "city_overworld_village_" + settlementBiomeVariant(settlement);
    }

    private int settlementOverlaySize(WorldMap.SettlementSite settlement) {
        if (state.world.isPlayerSettlement(settlement)) {
            return Math.min(258, 120 + state.world.playerVillageStage() * 16);
        }
        return switch (settlement.kind()) {
            case "City" -> 244;
            case "Town" -> 220;
            case "Camp", "Player Settlement" -> 154;
            default -> 202;
        };
    }

    private int settlementLightRadius(WorldMap.SettlementSite settlement) {
        if (state.world.isPlayerSettlement(settlement)) {
            return Math.min(128, 74 + state.world.playerVillageStage() * 6);
        }
        return switch (settlement.kind()) {
            case "City" -> 128;
            case "Town" -> 112;
            default -> 92;
        };
    }

    private String settlementBiomeVariant(WorldMap.SettlementSite settlement) {
        if ("highwall".equals(settlement.kingdomId()) || containsAny(settlement.id(), "snow", "pine", "frost")) {
            return "snow";
        }
        if ("sanctum".equals(settlement.kingdomId()) || containsAny(settlement.id(), "dune", "sun", "redcairn", "ember")) {
            return "desert";
        }
        if ("belltower".equals(settlement.kingdomId()) || containsAny(settlement.id(), "mire", "fen", "reed", "glimmer")) {
            return "marsh";
        }
        return switch (settlementUnderlayTile(settlement.x(), settlement.y())) {
            case 'n' -> "snow";
            case 's', 'b' -> "desert";
            case 'v', 'w', '~' -> "marsh";
            default -> "green";
        };
    }

    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private void drawSettlement(Graphics2D g, int camX, int camY, WorldMap.SettlementSite settlement, String asset, int size) {
        int wx = settlement.x();
        int wy = settlement.y();
        int ts = tileSize();
        int drawSize = scaled(size);
        int marginTiles = Math.max(4, drawSize / Math.max(1, ts) + 2);
        if (wx < camX - marginTiles || wy < camY - marginTiles
                || wx >= camX + lastVisibleCols + marginTiles || wy >= camY + lastVisibleRows + marginTiles) {
            return;
        }
        int px = (wx - camX) * ts + ts / 2 - drawSize / 2;
        int py = (wy - camY) * ts + ts / 2 - drawSize / 2;
        Rectangle hoverBounds = settlementScreenBounds(settlement, size);
        WorldMap.Kingdom kingdom = kingdomById(settlement.kingdomId());
        Color accent = settlementAccentColor(settlement, kingdom);
        tooltipZones.add(new TooltipZone(
                hoverBounds,
                settlement.label(),
                settlementTooltip(settlement, kingdom),
                "sprite:" + asset,
                accent
        ));
        drawSettlementApron(g, settlement, px, py, drawSize);
        drawShadow(g, px + drawSize / 5, py + drawSize - scaled(18), drawSize * 3 / 5, scaled(16));
        g.drawImage(assets.spriteFit(asset, drawSize, drawSize), px, py, null);
        if (hoverPoint != null && hoverBounds.contains(hoverPoint)) {
            drawSettlementHoverHighlight(g, settlement, px, py, drawSize, accent);
        }
    }

    private Rectangle settlementScreenBounds(WorldMap.SettlementSite settlement, int size) {
        int ts = tileSize();
        int drawSize = scaled(size);
        int x = (int) Math.round((settlement.x() - lastCameraX) * ts + ts / 2.0 - drawSize / 2.0);
        int y = (int) Math.round((settlement.y() - lastCameraY) * ts + ts / 2.0 - drawSize / 2.0);
        return new Rectangle(x, y, drawSize, drawSize);
    }

    private WorldMap.SettlementSite settlementAtScreenPoint(Point point) {
        if (point == null || !WorldMap.OVERWORLD_ID.equals(state.currentMapId)
                || point.x < 0 || point.x >= gameAreaWidth()
                || point.y < 0 || point.y >= worldViewportHeight()) {
            return null;
        }
        List<WorldMap.SettlementSite> settlements = state.world.settlementSites();
        for (int i = settlements.size() - 1; i >= 0; i--) {
            WorldMap.SettlementSite settlement = settlements.get(i);
            if (settlementScreenBounds(settlement, settlementOverlaySize(settlement)).contains(point)) {
                return settlement;
            }
        }
        return null;
    }

    private void drawSettlementHoverHighlight(Graphics2D g, WorldMap.SettlementSite settlement, int px, int py, int drawSize, Color edge) {
        Composite oldComposite = g.getComposite();
        Stroke oldStroke = g.getStroke();
        Font oldFont = g.getFont();

        int inset = Math.max(2, scaled(5));
        int arc = scaled(10);
        int fontSize = settlementHoverNameFontSize();
        int labelH = Math.max(scaled(18), fontSize + scaled(8));
        int labelY = Math.max(py - scaled(8), py + scaled(2));

        g.setComposite(AlphaComposite.SrcOver.derive(0.20f));
        g.setColor(edge);
        g.fillRoundRect(px + inset, py + inset, drawSize - inset * 2, drawSize - inset * 2, arc, arc);
        g.setComposite(AlphaComposite.SrcOver.derive(0.86f));
        g.setStroke(new BasicStroke(Math.max(2f, scaled(2))));
        g.drawRoundRect(px + inset, py + inset, drawSize - inset * 2, drawSize - inset * 2, arc, arc);

        String label = settlement.label();
        g.setFont(new Font("SansSerif", Font.BOLD, fontSize));
        FontMetrics metrics = g.getFontMetrics();
        int labelW = Math.min(gameAreaWidth() - scaled(24), metrics.stringWidth(label) + scaled(18));
        int labelX = clamp(px + drawSize / 2 - labelW / 2, scaled(8), Math.max(scaled(8), gameAreaWidth() - labelW - scaled(8)));
        g.setComposite(AlphaComposite.SrcOver.derive(0.90f));
        g.setColor(new Color(12, 16, 22, 220));
        g.fillRoundRect(labelX, labelY, labelW, labelH, scaled(8), scaled(8));
        g.setColor(edge);
        g.drawRoundRect(labelX, labelY, labelW, labelH, scaled(8), scaled(8));
        g.setColor(new Color(248, 242, 224));
        drawClippedString(g, label, labelX + scaled(8), labelY + labelH / 2 + metrics.getAscent() / 2 - scaled(2), labelW - scaled(16));

        g.setFont(oldFont);
        g.setStroke(oldStroke);
        g.setComposite(oldComposite);
    }

    private int settlementHoverNameFontSize() {
        double zoomScale = Math.max(0.75, Math.min(1.55, state.zoom / 100.0));
        return Math.max(10, (int) Math.round(scaled(12) * zoomScale));
    }

    private Color settlementAccentColor(WorldMap.SettlementSite settlement, WorldMap.Kingdom kingdom) {
        if (kingdom != null) {
            return new Color(kingdom.colorRgb());
        }
        if (state.world.isPlayerSettlement(settlement)) {
            return new Color(255, 226, 128);
        }
        return new Color(194, 174, 255);
    }

    private Color currentSettlementAccentColor() {
        WorldMap.SettlementSite settlement = currentSettlementSite();
        return settlement == null
                ? new Color(194, 174, 255)
                : settlementAccentColor(settlement, kingdomById(settlement.kingdomId()));
    }

    private WorldMap.SettlementSite currentSettlementSite() {
        String mapId = state.currentMapId;
        if (mapId.startsWith("house_")) {
            mapId = sourceMapIdFromHouseMap(mapId);
        }
        for (WorldMap.SettlementSite settlement : state.world.settlementSites()) {
            if (settlement.id().equals(mapId)) {
                return settlement;
            }
        }
        return null;
    }

    private String sourceMapIdFromHouseMap(String mapId) {
        if (mapId == null || !mapId.startsWith("house_")) {
            return mapId == null ? "" : mapId;
        }
        int last = mapId.lastIndexOf('_');
        int previous = last <= 0 ? -1 : mapId.lastIndexOf('_', last - 1);
        return previous <= "house_".length() ? "" : mapId.substring("house_".length(), previous);
    }

    private String kingdomName(String kingdomId) {
        WorldMap.Kingdom kingdom = kingdomById(kingdomId);
        return kingdom == null ? "Unclaimed" : kingdom.name();
    }

    private boolean isSettlementMapKind(String mapId) {
        String kind = state.world.kind(mapId);
        return "city".equals(kind) || "village".equals(kind);
    }

    private void drawSettlementApron(Graphics2D g, WorldMap.SettlementSite settlement, int px, int py, int drawSize) {
        int wx = settlement.x();
        int wy = settlement.y();
        char underlay = settlementUnderlayTile(wx, wy);
        Color terrain = Terrain.color(underlay);
        Graphics2D apron = (Graphics2D) g.create();
        apron.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int cx = px + drawSize / 2;
        int cy = py + drawSize / 2 + scaled(10);
        int radiusX = Math.max(scaled(82), drawSize / 2 + scaled("Town".equals(settlement.kind()) ? 26 : 22));
        int radiusY = Math.max(scaled(64), drawSize / 3 + scaled("Town".equals(settlement.kind()) ? 20 : 16));
        drawSettlementNeighborBlend(apron, wx, wy, cx, cy, radiusX, radiusY);
        apron.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f));
        apron.setColor(new Color(terrain.getRed(), terrain.getGreen(), terrain.getBlue(), 168));
        apron.fillOval(cx - radiusX, cy - radiusY, radiusX * 2, radiusY * 2);

        int seed = Math.abs(wx * 928371 + wy * 364479 + assetLikeHash(underlay));
        apron.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f));
        for (int i = 0; i < 16; i++) {
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

    private void drawSettlementNeighborBlend(Graphics2D g, int wx, int wy, int cx, int cy, int radiusX, int radiusY) {
        Shape oldClip = g.getClip();
        Composite oldComposite = g.getComposite();
        Paint oldPaint = g.getPaint();
        g.clip(new java.awt.geom.Ellipse2D.Double(cx - radiusX, cy - radiusY, radiusX * 2.0, radiusY * 2.0));
        int[][] probes = {
                {0, -1}, {1, 0}, {0, 1}, {-1, 0},
                {1, -1}, {1, 1}, {-1, 1}, {-1, -1}
        };
        for (int i = 0; i < probes.length; i++) {
            int dx = probes[i][0];
            int dy = probes[i][1];
            TilePoint sample = settlementNeighborSample(wx, wy, dx, dy);
            char tile = visibleTerrainTile(state.world.tileAt(state.currentMapId, sample.x(), sample.y()), sample.x(), sample.y());
            if (!isSettlementBlendTerrain(tile)) {
                continue;
            }
            Shape clipBeforeWedge = g.getClip();
            g.clip(settlementBlendWedge(cx, cy, radiusX, radiusY, dx, dy));
            float textureAlpha = i < 4 ? 0.13f : 0.08f;
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, textureAlpha));
            g.drawImage(
                    assets.image(terrainImageName(tile, sample.x(), sample.y()), radiusX * 2, radiusY * 2),
                    cx - radiusX,
                    cy - radiusY,
                    null
            );
            Color terrain = Terrain.color(tile);
            int alpha = i < 4 ? 72 : 48;
            float focusX = (float) (cx + dx * radiusX * 0.66);
            float focusY = (float) (cy + dy * radiusY * 0.66);
            g.setPaint(new RadialGradientPaint(
                    new Point(Math.round(focusX), Math.round(focusY)),
                    Math.max(radiusX, radiusY) * 0.95f,
                    new float[]{0.0f, 0.72f, 1.0f},
                    new Color[]{
                            new Color(terrain.getRed(), terrain.getGreen(), terrain.getBlue(), alpha),
                            new Color(terrain.getRed(), terrain.getGreen(), terrain.getBlue(), alpha / 3),
                            new Color(terrain.getRed(), terrain.getGreen(), terrain.getBlue(), 0)
                    }
            ));
            g.fillOval(cx - radiusX, cy - radiusY, radiusX * 2, radiusY * 2);
            g.setClip(clipBeforeWedge);
        }
        g.setPaint(oldPaint);
        g.setComposite(oldComposite);
        g.setClip(oldClip);
    }

    private Shape settlementBlendWedge(int cx, int cy, int radiusX, int radiusY, int dx, int dy) {
        int x = dx < 0 ? cx - radiusX : dx > 0 ? cx : cx - radiusX;
        int y = dy < 0 ? cy - radiusY : dy > 0 ? cy : cy - radiusY;
        int width = dx == 0 ? radiusX * 2 : radiusX;
        int height = dy == 0 ? radiusY * 2 : radiusY;
        return new Rectangle(x, y, width, height);
    }

    private TilePoint settlementNeighborSample(int wx, int wy, int dx, int dy) {
        TilePoint fallback = new TilePoint(wx, wy);
        for (int distance = 3; distance <= 8; distance++) {
            int sx = wx + dx * distance;
            int sy = wy + dy * distance;
            char raw = state.world.tileAt(state.currentMapId, sx, sy);
            if (raw == 'c' || raw == 'u') {
                continue;
            }
            char tile = visibleTerrainTile(raw, sx, sy);
            if (isSettlementBlendTerrain(tile)) {
                return new TilePoint(sx, sy);
            }
        }
        return fallback;
    }

    private boolean isSettlementBlendTerrain(char tile) {
        return isNatural(tile);
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
        double altitude = sunAltitude();
        double horizon = 1.0 - altitude;
        double direction = shadowCastX();
        double sampleX = x + w / 2.0;
        double sampleY = y + h / 2.0;
        double sampleWorldX = shadowWorldOffsetX + sampleX;
        double sampleWorldY = shadowWorldOffsetY + sampleY;
        double lightInfluence = 0.0;
        double localCastX = 0.0;
        double localCastY = 0.0;
        double red = 0.0;
        double green = 0.0;
        double blue = 0.0;
        double weight = 0.0;
        for (WorldLight light : activeWorldLights) {
            double dx = sampleWorldX - light.x;
            double dy = sampleWorldY - light.y;
            double radius = light.shadowRadius();
            double distanceSq = dx * dx + dy * dy;
            if (distanceSq >= radius * radius) {
                continue;
            }
            double distance = Math.sqrt(distanceSq);
            double falloff = 1.0 - distance / radius;
            double influenceContribution = falloff * falloff * light.shadowStrength();
            double colorContribution = falloff * light.shadowStrength();
            lightInfluence += influenceContribution;
            if (distance > 0.001) {
                localCastX += (dx / distance) * influenceContribution;
                localCastY += (dy / distance) * influenceContribution;
            }
            red += light.color.getRed() * colorContribution;
            green += light.color.getGreen() * colorContribution;
            blue += light.color.getBlue() * colorContribution;
            weight += colorContribution;
        }
        lightInfluence = Math.min(1.0, lightInfluence);
        double localLength = Math.sqrt(localCastX * localCastX + localCastY * localCastY);
        if (localLength > 1.0) {
            localCastX /= localLength;
            localCastY /= localLength;
        }
        double sunBlend = 1.0 - Math.min(0.62, lightInfluence * 0.58);
        int castX = (int) Math.round(w * direction * (0.32 + horizon * 1.55) * sunBlend
                + localCastX * w * Math.min(0.84, lightInfluence * 0.80));
        int castY = (int) Math.round(h * (0.26 + horizon * 0.62) * sunBlend
                + Math.max(-0.55, Math.min(0.85, localCastY)) * h * Math.min(0.58, lightInfluence * 0.54));
        int castW = Math.max(1, (int) Math.round(w * (1.10 + horizon * 0.72)));
        int castH = Math.max(1, (int) Math.round(h * (0.72 + horizon * 0.44)));
        int contactInset = Math.max(1, w / 10);
        float strength = (float) (shadowStrength() * (1.0 - Math.min(0.58, lightInfluence * 0.68)));

        Graphics2D shadow = (Graphics2D) g.create();
        shadow.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Composite oldComposite = shadow.getComposite();

        float castAlpha = (float) (0.08f * strength * (0.35f + (float) state.daylightLevel() * 0.65f) * (1.0 - lightInfluence * 0.35));
        shadow.setComposite(AlphaComposite.SrcOver.derive(castAlpha));
        shadow.setColor(new Color(4, 6, 9));
        shadow.fillOval(x + castX - (castW - w) / 2, y + castY, castW, castH);

        shadow.setComposite(AlphaComposite.SrcOver.derive(0.24f * strength));
        shadow.setColor(new Color(0, 0, 0));
        shadow.fillOval(x + contactInset / 2, y, Math.max(1, w - contactInset), h);

        shadow.setComposite(AlphaComposite.SrcOver.derive(0.07f * strength));
        shadow.setColor(new Color(32, 35, 37));
        shadow.fillOval(x + contactInset, y + Math.max(1, h / 5), Math.max(1, w - contactInset * 2), Math.max(1, h / 2));

        if (lightInfluence > 0.02) {
            Color lightColor = weight <= 0.0
                    ? DEFAULT_SHADOW_LIGHT
                    : new Color(
                            clampColor((int) Math.round(red / weight)),
                            clampColor((int) Math.round(green / weight)),
                            clampColor((int) Math.round(blue / weight))
                    );
            shadow.setComposite(AlphaComposite.SrcOver.derive((float) Math.min(0.20, lightInfluence * 0.18)));
            shadow.setColor(new Color(lightColor.getRed(), lightColor.getGreen(), lightColor.getBlue()));
            shadow.fillOval(x - w / 8, y - h / 3, w + w / 4, h + h / 2);
        }

        shadow.setComposite(oldComposite);
        shadow.dispose();
    }

    private void drawCasterShadow(Graphics2D g, BufferedImage image, String cacheKey,
                                  int x, int y, int width, int height, boolean flipHorizontal, float baseAlpha) {
        if (image == null || width <= scaled(10) || height <= scaled(10)) {
            return;
        }
        double footX = x + width / 2.0;
        double footY = y + height;
        double sampleWorldX = shadowWorldOffsetX + footX;
        double sampleWorldY = shadowWorldOffsetY + footY;
        double altitude = sunAltitude();
        double horizon = 1.0 - altitude;
        double direction = shadowCastX();
        double lightInfluence = 0.0;
        double localCastX = 0.0;
        double localCastY = 0.0;
        for (WorldLight light : activeWorldLights) {
            double dx = sampleWorldX - light.x;
            double dy = sampleWorldY - light.y;
            double radius = light.shadowRadius();
            double distanceSq = dx * dx + dy * dy;
            if (distanceSq >= radius * radius) {
                continue;
            }
            double distance = Math.sqrt(distanceSq);
            double falloff = 1.0 - distance / radius;
            double contribution = falloff * falloff * light.shadowStrength();
            lightInfluence += contribution;
            if (distance > 0.001) {
                localCastX += (dx / distance) * contribution;
                localCastY += (dy / distance) * contribution;
            }
        }
        lightInfluence = Math.min(1.0, lightInfluence);
        double localLength = Math.sqrt(localCastX * localCastX + localCastY * localCastY);
        if (localLength > 1.0) {
            localCastX /= localLength;
            localCastY /= localLength;
        }
        double sunBlend = 1.0 - Math.min(0.68, lightInfluence * 0.62);
        double casterScale = Math.max(0.72, Math.min(1.28, width / (double) Math.max(1, height)));
        double castX = width * direction * (0.18 + horizon * 0.98) * sunBlend
                + localCastX * width * Math.min(0.72, lightInfluence * 0.68);
        double castY = height * (0.06 + horizon * 0.24) * sunBlend
                + Math.max(-0.35, Math.min(0.55, localCastY)) * height * Math.min(0.22, lightInfluence * 0.20);
        double skew = direction * (0.30 + horizon * 0.86) * casterScale * sunBlend
                + localCastX * Math.min(0.58, lightInfluence * 0.52);
        double flatten = 0.20 + horizon * 0.18 + lightInfluence * 0.035;
        float alpha = (float) (baseAlpha * shadowStrength() * (0.55 + horizon * 0.35)
                * (1.0 - Math.min(0.50, lightInfluence * 0.46)));
        if (alpha <= 0.015f) {
            return;
        }

        BufferedImage mask = shadowMask(cacheKey, image);
        Graphics2D shadow = (Graphics2D) g.create();
        shadow.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        shadow.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        shadow.setComposite(AlphaComposite.SrcOver.derive(Math.max(0.0f, Math.min(0.34f, alpha))));
        shadow.translate(footX + castX, footY + castY);
        shadow.shear(skew, 0.0);
        shadow.scale(flipHorizontal ? -1.0 : 1.0, flatten);
        shadow.drawImage(mask, -width / 2, -height, width, height, null);
        shadow.dispose();
    }

    private BufferedImage shadowMask(String cacheKey, BufferedImage image) {
        String key = cacheKey + ":" + image.getWidth() + "x" + image.getHeight();
        BufferedImage cached = shadowMaskCache.get(key);
        if (cached != null) {
            return cached;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage mask = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        for (int i = 0; i < pixels.length; i++) {
            int alpha = (pixels[i] >>> 24) & 0xff;
            if (alpha <= 4) {
                pixels[i] = 0;
            } else {
                pixels[i] = (Math.min(185, alpha) << 24);
            }
        }
        mask.setRGB(0, 0, width, height, pixels, 0, width);
        shadowMaskCache.put(key, mask);
        return mask;
    }

    private double worldLightInfluenceAt(double x, double y) {
        double influence = 0.0;
        for (WorldLight light : activeWorldLights) {
            double dx = x - light.x;
            double dy = y - light.y;
            double radius = light.shadowRadius();
            double distanceSq = dx * dx + dy * dy;
            if (distanceSq >= radius * radius) {
                continue;
            }
            double falloff = 1.0 - Math.sqrt(distanceSq) / radius;
            influence += falloff * falloff * light.shadowStrength();
        }
        return Math.min(1.0, influence);
    }

    private Color worldLightColorAt(double x, double y) {
        double red = 0.0;
        double green = 0.0;
        double blue = 0.0;
        double weight = 0.0;
        for (WorldLight light : activeWorldLights) {
            double dx = x - light.x;
            double dy = y - light.y;
            double radius = light.shadowRadius();
            double distanceSq = dx * dx + dy * dy;
            if (distanceSq >= radius * radius) {
                continue;
            }
            double falloff = 1.0 - Math.sqrt(distanceSq) / radius;
            double contribution = falloff * light.shadowStrength();
            red += light.color.getRed() * contribution;
            green += light.color.getGreen() * contribution;
            blue += light.color.getBlue() * contribution;
            weight += contribution;
        }
        if (weight <= 0.0) {
            return new Color(255, 205, 130);
        }
        return new Color(clampColor((int) Math.round(red / weight)), clampColor((int) Math.round(green / weight)), clampColor((int) Math.round(blue / weight)));
    }

    private double shadowCastX() {
        return (sunProgress() - 0.5) * 2.0;
    }

    private double sunProgress() {
        int minutes = state.timeOfDayMinutes();
        return clamp((minutes - 360) / 720.0, 0.0, 1.0);
    }

    private double sunAltitude() {
        return Math.max(0.12, Math.sin(Math.PI * sunProgress()));
    }

    private float shadowStrength() {
        double daylight = state.daylightLevel();
        double altitude = sunAltitude();
        double horizonBoost = (1.0 - altitude) * 0.28;
        return (float) Math.max(0.38, Math.min(1.0, daylight * 0.62 + horizonBoost));
    }

    private void rebuildWorldLights(int camX, int camY, int visibleCols, int visibleRows, int tileSize) {
        activeWorldLights.clear();
        for (WorldProp prop : nearbyWorldProps) {
            Color glow = propGlowColor(prop.asset());
            if (glow == null) {
                continue;
            }
            int cx = worldTileCenter(prop.x(), tileSize);
            int cy = worldTileCenter(prop.y(), tileSize);
            float pulse = propGlowPulse(prop);
            addWorldLight(cx, cy, propGlowRadius(prop.asset()), glow, propGlowAlpha(prop.asset()) * lightVisibilityForAsset(prop.asset()) * pulse, true);
        }

        float visibility = lightVisibility();
        if (visibility >= 0.28f) {
            if (isSettlementMapKind(state.currentMapId)) {
                addCityBuildingWorldLights(tileSize, camX, camY, visibleCols, visibleRows, visibility);
            } else {
                addTerrainWindowWorldLights(tileSize, camX, camY, visibleCols, visibleRows, visibility);
            }
        }

        addSettlementWorldLights(tileSize, camX, camY, visibleCols, visibleRows);
        addPlayerWorldLight(tileSize);
    }

    private void addTerrainWindowWorldLights(int tileSize, int camX, int camY, int visibleCols, int visibleRows, float visibility) {
        for (int sy = 0; sy < visibleRows; sy++) {
            for (int sx = 0; sx < visibleCols; sx++) {
                int wx = camX + sx;
                int wy = camY + sy;
                char tile = state.world.tileAt(state.currentMapId, wx, wy);
                int seed = Math.abs(wx * 928371 + wy * 364479 + state.currentMapId.hashCode());
                if (!isWindowLightTile(tile, seed)) {
                    continue;
                }
                int cx = worldTileCenter(wx, tileSize) + scaled(Math.floorMod(seed / 13, 13) - 6);
                int cy = worldTileCenter(wy, tileSize) + scaled(Math.floorMod(seed / 31, 11) - 5);
                float pulse = (float) (0.84 + Math.sin(frame * 0.055 + seed * 0.01) * 0.08);
                Color color = tile == 'n' ? new Color(172, 219, 255) : new Color(255, 204, 118);
                addWorldLight(cx, cy, scaled(tile == 'c' || tile == 'u' ? 84 : 48), color, 0.16f * visibility * pulse, true);
            }
        }
    }

    private void addCityBuildingWorldLights(int tileSize, int camX, int camY, int visibleCols, int visibleRows, float visibility) {
        String kind = state.world.kind(state.currentMapId);
        for (CityBuilding building : state.world.cityBuildings(state.currentMapId)) {
            Rectangle bounds = settlementBuildingWorldBounds(kind, building, tileSize);
            if (!worldLightIntersectsView(bounds.getCenterX(), bounds.getCenterY(), Math.max(bounds.width, bounds.height) / 2 + scaled(56),
                    camX, camY, visibleCols, visibleRows, tileSize)) {
                continue;
            }
            addBuildingLanternWorldLights(building, tileSize, visibility);
            addBuildingWindowWorldLights(kind, building, tileSize, visibility);
        }
    }

    private Rectangle settlementBuildingWorldBounds(String kind, CityBuilding building, int tileSize) {
        if (usesStandaloneSettlementBuilding(kind, building)) {
            return standaloneBuildingVisualLayout(building, 0, 0, tileSize).bounds();
        }
        return buildingVisualBounds(building, 0, 0, tileSize);
    }

    private void addBuildingLanternWorldLights(CityBuilding building, int tileSize, float visibility) {
        if (building.width() < 4) {
            return;
        }
        int lotX = building.x1() * tileSize;
        int lotW = building.width() * tileSize;
        int frontY = (building.y2() + 1) * tileSize;
        int lanternW = scaled(10);
        int lanternH = scaled(18);
        int y = frontY - lanternH - scaled(20) + lanternH / 2;
        int leftX = lotX + scaled(4) + lanternW / 2;
        int rightX = lotX + lotW - scaled(4) - lanternW / 2;
        int seed = Math.abs(buildingLightSeed(building));
        float leftPulse = lanternPulse(seed);
        float rightPulse = lanternPulse(seed + 431);
        Color color = new Color(255, 203, 112);
        addWorldLight(leftX, y, scaled(58), color, 0.22f * visibility * leftPulse, true);
        addWorldLight(rightX, y, scaled(58), color, 0.22f * visibility * rightPulse, true);
    }

    private void addBuildingWindowWorldLights(String kind, CityBuilding building, int tileSize, float visibility) {
        int modules = buildingModuleCount(building);
        int lotX = building.x1() * tileSize;
        int lotW = building.width() * tileSize;
        int frontY = (building.y2() + 1) * tileSize;
        int seed = Math.abs(buildingLightSeed(building));
        Color color = buildingWindowLightColor(building);
        for (int index = 0; index < modules; index++) {
            Rectangle module = buildingModuleWorldBounds(kind, building, index, modules, seed, tileSize, lotX, lotW, frontY);
            int lightCount = building.width() >= 6 && module.width >= tileRelative(64, tileSize) ? 2 : 1;
            for (int i = 0; i < lightCount; i++) {
                int lightSeed = seed + index * 919 + i * 337;
                if (!buildingWindowIsLit(building, lightSeed)) {
                    continue;
                }
                double xBias = lightCount == 1 ? 0.50 : (i == 0 ? 0.34 : 0.66);
                int x = module.x + (int) Math.round(module.width * xBias);
                int row = Math.floorMod(lightSeed / 53, module.height > tileRelative(104, tileSize) ? 2 : 1);
                int y = module.y + (int) Math.round(module.height * (row == 0 ? 0.45 : 0.62));
                float pulse = windowPulse(lightSeed);
                int radius = "mage_tower".equals(building.style()) ? scaled(54) : scaled(46);
                addWorldLight(x, y, radius, color, 0.13f * visibility * pulse, false);
            }
        }
    }

    private Rectangle buildingModuleWorldBounds(String kind, CityBuilding building, int index, int modules, int seed,
                                               int tileSize, int lotX, int lotW, int frontY) {
        if (usesStandaloneSettlementBuilding(kind, building)) {
            BuildingVisualLayout layout = standaloneBuildingVisualLayout(building, 0, 0, tileSize);
            int moduleW = Math.max(tileSize, layout.drawW() / Math.max(1, modules));
            return new Rectangle(layout.drawX() + index * moduleW, layout.drawY(), moduleW, layout.drawH());
        }
        boolean civic = List.of("hall", "guild", "barracks", "warehouse").contains(building.style());
        boolean compactModule = List.of("guild", "house", "shop", "row").contains(building.style());
        int minH = civic ? 108 : (compactModule ? 102 : 90);
        int maxH = civic ? 166 : (compactModule ? 150 : 134);
        int minW = civic ? 78 : (compactModule ? 72 : 58);
        int maxW = civic ? 128 : (compactModule ? 122 : 104);
        int targetH = Math.max(tileRelative(minH, tileSize),
                Math.min(tileRelative(maxH, tileSize), building.depth() * tileSize + tileRelative(civic ? 50 : 42, tileSize)));
        int targetW = Math.max(tileRelative(minW, tileSize),
                Math.min(lotW / Math.max(1, modules) + tileRelative(civic ? 42 : 34, tileSize), tileRelative(maxW, tileSize)));
        int centerX = lotX + (int) Math.round((index + 0.5) * lotW / modules);
        int jitter = ((seed >> (index * 4)) & 7) - 3;
        int drawX = centerX - targetW / 2 + jitter * tileSize / GameConfig.TILE;
        int drawY = frontY - targetH - tileRelative(5, tileSize);
        return new Rectangle(drawX, drawY, targetW, targetH);
    }

    private Color buildingWindowLightColor(CityBuilding building) {
        if ("mage_tower".equals(building.style())) {
            return new Color(176, 221, 255);
        }
        if ("sun_shrine".equals(building.style())) {
            return new Color(255, 224, 142);
        }
        return new Color(255, 211, 126);
    }

    private boolean buildingWindowIsLit(CityBuilding building, int seed) {
        int threshold = switch (building.style()) {
            case "warehouse", "barracks" -> 42;
            case "inn", "shop", "guild", "hall", "river_hall" -> 78;
            case "mage_tower", "sun_shrine", "bell_tower" -> 70;
            default -> 58;
        };
        return Math.floorMod(seed / 17, 100) < threshold;
    }

    private float lanternPulse(int seed) {
        return (float) (0.88 + Math.sin(frame * 0.11 + seed * 0.013) * 0.08);
    }

    private float windowPulse(int seed) {
        return (float) (0.90 + Math.sin(frame * 0.045 + seed * 0.007) * 0.05);
    }

    private int buildingLightSeed(CityBuilding building) {
        int keyHash = building.key() == null ? 0 : building.key().hashCode();
        return keyHash ^ building.style().hashCode() ^ building.x1() * 928371 ^ building.y1() * 364479;
    }

    private void addSettlementWorldLights(int tileSize, int camX, int camY, int visibleCols, int visibleRows) {
        if (!WorldMap.OVERWORLD_ID.equals(state.currentMapId) || lightVisibility() < 0.24f) {
            return;
        }
        int[][] settlements = {
                {82, 105, 128}, {152, 145, 128}, {205, 78, 128}, {228, 185, 128}, {150, 230, 128},
                {112, 158, 92}, {83, 62, 92}, {102, 245, 92}, {240, 153, 92}
        };
        float visibility = lightVisibility();
        for (int[] settlement : settlements) {
            int cx = worldTileCenter(settlement[0], tileSize);
            int cy = worldTileCenter(settlement[1], tileSize) + scaled(8);
            int radius = scaled(settlement[2]);
            if (!worldLightIntersectsView(cx, cy, radius + scaled(48), camX, camY, visibleCols, visibleRows, tileSize)) {
                continue;
            }
            addWorldLight(cx, cy, radius, new Color(255, 191, 104), 0.15f * visibility, true);
            for (int i = 0; i < 5; i++) {
                int seed = Math.abs(settlement[0] * 7349 + settlement[1] * 9127 + i * 1451);
                int sx = cx + scaled(Math.floorMod(seed, 70) - 35);
                int sy = cy + scaled(Math.floorMod(seed / 41, 48) - 18);
                float pulse = (float) (0.78 + Math.sin(frame * 0.06 + i * 1.8) * 0.10);
                addWorldLight(sx, sy, scaled(30), new Color(255, 219, 142), 0.08f * visibility * pulse, false);
            }
        }
    }

    private boolean worldLightIntersectsView(double x, double y, int radius, int camX, int camY, int visibleCols, int visibleRows, int tileSize) {
        double minX = camX * (double) tileSize - radius;
        double minY = camY * (double) tileSize - radius;
        double maxX = (camX + visibleCols) * (double) tileSize + radius;
        double maxY = (camY + visibleRows) * (double) tileSize + radius;
        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }

    private void addPlayerWorldLight(int tileSize) {
        int cx = (int) Math.round(renderPlayerX() * tileSize) + tileSize / 2;
        int cy = (int) Math.round(renderPlayerY() * tileSize) + tileSize / 2 + scaled(4);
        addWorldLight(cx, cy, scaled(118), new Color(255, 229, 168), 0.055f + nightFactor() * 0.09f, true);
    }

    private void addWorldLight(double x, double y, int radius, Color color, float alpha, boolean affectsShadows) {
        if (alpha <= 0.005f) {
            return;
        }
        WorldLight candidate = new WorldLight(x, y, Math.max(1, radius), color, Math.min(1.0f, alpha), affectsShadows);
        if (activeWorldLights.size() < MAX_ACTIVE_WORLD_LIGHTS) {
            activeWorldLights.add(candidate);
            return;
        }
        int weakestIndex = 0;
        double weakestImportance = worldLightImportance(activeWorldLights.get(0));
        for (int i = 1; i < activeWorldLights.size(); i++) {
            double importance = worldLightImportance(activeWorldLights.get(i));
            if (importance < weakestImportance) {
                weakestImportance = importance;
                weakestIndex = i;
            }
        }
        if (worldLightImportance(candidate) > weakestImportance * 1.08) {
            activeWorldLights.set(weakestIndex, candidate);
        }
    }

    private double worldLightImportance(WorldLight light) {
        return light.radius * light.alpha * (light.affectsShadows ? 1.2 : 1.0);
    }

    private int worldTileCenter(int coordinate, int tileSize) {
        return coordinate * tileSize + tileSize / 2;
    }

    private void drawLightTileSpill(Graphics2D g, int camX, int camY, int visibleCols, int visibleRows, int tileSize) {
        if (activeWorldLights.isEmpty()) {
            return;
        }
        for (WorldLight light : activeWorldLights) {
            int cx = (int) Math.round(light.x - camX * (double) tileSize);
            int cy = (int) Math.round(light.y - camY * (double) tileSize);
            int coreRadius = Math.max(1, (int) Math.round(light.radius * 1.08));
            int spreadRadius = Math.max(coreRadius + 1, (int) Math.round(light.radius * 1.82));
            if (cx + spreadRadius < -tileSize || cy + spreadRadius < -tileSize
                    || cx - spreadRadius >= visibleCols * tileSize + tileSize
                    || cy - spreadRadius >= visibleRows * tileSize + tileSize) {
                continue;
            }
            drawRadialGlow(g, cx, cy, spreadRadius, light.color, Math.min(0.11f, light.alpha * 0.30f));
            drawRadialGlow(g, cx, cy, coreRadius, light.color, Math.min(0.20f, light.alpha * 0.82f));
        }
    }

    private void drawBiomeLightTints(Graphics2D g, int camX, int camY, int visibleCols, int visibleRows, int tileSize) {
        if (isInteriorAtmosphereMap()) {
            drawInteriorLightTints(g, camX, camY, visibleCols, visibleRows, tileSize);
            return;
        }
        Composite oldComposite = g.getComposite();
        float daylight = (float) state.daylightLevel();
        for (int sy = 0; sy < visibleRows; sy++) {
            for (int sx = 0; sx < visibleCols; sx++) {
                int wx = camX + sx;
                int wy = camY + sy;
                char tile = visibleTerrainTile(state.world.tileAt(state.currentMapId, wx, wy), wx, wy);
                Color tint = biomeLightTint(tile);
                float alpha = biomeLightAlpha(tile) * (0.72f + daylight * 0.55f);
                int px = sx * tileSize;
                int py = sy * tileSize;
                g.setComposite(AlphaComposite.SrcOver.derive(alpha));
                g.setColor(tint);
                g.fillRect(px, py, tileSize, tileSize);
            }
        }
        g.setComposite(oldComposite);
    }

    private Color biomeLightTint(char tile) {
        return switch (tile) {
            case 'n' -> BIOME_TINT_TUNDRA;
            case 's' -> BIOME_TINT_DESERT;
            case 'v' -> BIOME_TINT_MARSH;
            case 'b' -> BIOME_TINT_BADLANDS;
            case 'f' -> BIOME_TINT_FOREST;
            case 'm', 'q' -> BIOME_TINT_MOUNTAIN;
            case 'w' -> BIOME_TINT_WATER;
            case 'r', 'T', 'K', 'c', 'u' -> BIOME_TINT_ROAD;
            default -> BIOME_TINT_GRASS;
        };
    }

    private float biomeLightAlpha(char tile) {
        return switch (tile) {
            case 'f', 'v' -> 0.045f;
            case 's', 'n', 'w' -> 0.036f;
            case 'b', 'm', 'q' -> 0.030f;
            case 'r', 'T', 'K', 'c', 'u' -> 0.026f;
            default -> 0.024f;
        };
    }

    private void drawEmissiveWorldLights(Graphics2D g, int camX, int camY, int tileSize, double cameraOffsetX, double cameraOffsetY) {
        Graphics2D lights = (Graphics2D) g.create();
        lights.setClip(0, 0, gameAreaWidth(), viewHeight());
        lights.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (WorldLight light : activeWorldLights) {
            int cx = (int) Math.round(light.x - camX * tileSize - cameraOffsetX);
            int cy = (int) Math.round(light.y - camY * tileSize - cameraOffsetY);
            if (cx + light.radius < 0 || cy + light.radius < 0
                    || cx - light.radius >= gameAreaWidth() || cy - light.radius >= viewHeight()) {
                continue;
            }
            drawRadialGlow(lights, cx, cy, light.radius, light.color, light.alpha);
            if (light.radius <= scaled(36) || light.alpha > 0.26f) {
                drawSpark(lights, cx, cy, light.color, Math.min(0.48f, light.alpha * 1.35f));
            }
        }
        lights.dispose();
    }

    private void drawPropLightSources(Graphics2D g, int camX, int camY, int tileSize, double cameraOffsetX, double cameraOffsetY) {
        float visibility = lightVisibility();
        for (WorldProp prop : state.world.props(state.currentMapId)) {
            if (prop.x() < camX - 2 || prop.y() < camY - 2 || prop.x() >= camX + lastVisibleCols + 2 || prop.y() >= camY + lastVisibleRows + 2) {
                continue;
            }
            Color glow = propGlowColor(prop.asset());
            if (glow == null) {
                continue;
            }
            int cx = screenTileCenterX(prop.x(), camX, tileSize, cameraOffsetX);
            int cy = screenTileCenterY(prop.y(), camY, tileSize, cameraOffsetY);
            int radius = propGlowRadius(prop.asset());
            float pulse = (float) (0.86 + Math.sin(frame * 0.10 + prop.x() * 0.7 + prop.y() * 0.4) * 0.10);
            drawRadialGlow(g, cx, cy, radius, glow, propGlowAlpha(prop.asset()) * visibility * pulse);
        }
    }

    private void drawTileLightSources(Graphics2D g, int camX, int camY, int tileSize, double cameraOffsetX, double cameraOffsetY) {
        float visibility = lightVisibility();
        if (visibility < 0.28f) {
            return;
        }
        for (int sy = 0; sy < lastVisibleRows; sy++) {
            for (int sx = 0; sx < lastVisibleCols; sx++) {
                int wx = camX + sx;
                int wy = camY + sy;
                char tile = state.world.tileAt(state.currentMapId, wx, wy);
                int seed = Math.abs(wx * 928371 + wy * 364479 + state.currentMapId.hashCode());
                if (!isWindowLightTile(tile, seed)) {
                    continue;
                }
                int cx = screenTileCenterX(wx, camX, tileSize, cameraOffsetX) + scaled(Math.floorMod(seed / 13, 13) - 6);
                int cy = screenTileCenterY(wy, camY, tileSize, cameraOffsetY) + scaled(Math.floorMod(seed / 31, 11) - 5);
                float pulse = (float) (0.84 + Math.sin(frame * 0.055 + seed * 0.01) * 0.08);
                Color color = tile == 'n' ? new Color(172, 219, 255) : new Color(255, 204, 118);
                drawRadialGlow(g, cx, cy, scaled(tile == 'c' || tile == 'u' ? 84 : 48), color, 0.16f * visibility * pulse);
                drawSpark(g, cx, cy, color, 0.28f * visibility * pulse);
            }
        }
    }

    private boolean isWindowLightTile(char tile, int seed) {
        int roll = Math.floorMod(seed, 100);
        return switch (tile) {
            case 'c', 'u' -> roll < 48;
            case 'p', 'a', 'j', 'l', 'y' -> roll < 12;
            case 'h' -> roll < 32;
            default -> false;
        };
    }

    private void drawSettlementLightSources(Graphics2D g, int camX, int camY, int tileSize, double cameraOffsetX, double cameraOffsetY) {
        if (!WorldMap.OVERWORLD_ID.equals(state.currentMapId)) {
            return;
        }
        float visibility = lightVisibility();
        if (visibility < 0.24f) {
            return;
        }
        for (WorldMap.SettlementSite settlement : state.world.settlementSites()) {
            int wx = settlement.x();
            int wy = settlement.y();
            if (wx < camX - 5 || wy < camY - 5 || wx >= camX + lastVisibleCols + 5 || wy >= camY + lastVisibleRows + 5) {
                continue;
            }
            int cx = screenTileCenterX(wx, camX, tileSize, cameraOffsetX);
            int cy = screenTileCenterY(wy, camY, tileSize, cameraOffsetY);
            int radius = scaled(settlementLightRadius(settlement));
            drawRadialGlow(g, cx, cy + scaled(8), radius, new Color(255, 191, 104), 0.15f * visibility);
            for (int i = 0; i < 5; i++) {
                int seed = Math.abs(wx * 7349 + wy * 9127 + i * 1451);
                int sx = cx + scaled(Math.floorMod(seed, 70) - 35);
                int sy = cy + scaled(Math.floorMod(seed / 41, 48) - 18);
                float pulse = (float) (0.78 + Math.sin(frame * 0.06 + i * 1.8) * 0.10);
                drawSpark(g, sx, sy, new Color(255, 219, 142), 0.36f * visibility * pulse);
            }
        }
    }

    private void drawPlayerLight(Graphics2D g, int camX, int camY, int tileSize, double cameraOffsetX, double cameraOffsetY) {
        if (state.playerX >= camX - 2 && state.playerY >= camY - 2 && state.playerX < camX + lastVisibleCols + 2 && state.playerY < camY + lastVisibleRows + 2) {
            int cx = (int) Math.round((renderPlayerX() - camX) * tileSize - cameraOffsetX) + tileSize / 2;
            int cy = (int) Math.round((renderPlayerY() - camY) * tileSize - cameraOffsetY) + tileSize / 2;
            drawRadialGlow(g, cx, cy + scaled(4), scaled(118), new Color(255, 229, 168), 0.055f + nightFactor() * 0.09f);
        }
    }

    private Color propGlowColor(String asset) {
        if (asset.startsWith("town_portal_")) {
            if (asset.contains("snow")) {
                return new Color(112, 220, 255);
            }
            if (asset.contains("desert")) {
                return new Color(93, 238, 226);
            }
            if (asset.contains("marsh")) {
                return new Color(66, 231, 205);
            }
            if (asset.contains("green")) {
                return new Color(174, 255, 105);
            }
            return new Color(116, 178, 255);
        }
        if (asset.contains("camp_fire") || asset.contains("campfire") || asset.contains("fire")) {
            return new Color(255, 151, 58);
        }
        if (asset.contains("forge") || asset.contains("oven") || asset.contains("stove")
                || asset.contains("hearth") || asset.contains("cookpot") || asset.contains("cooking_station")) {
            return new Color(255, 154, 72);
        }
        if (asset.contains("street_lamp") || asset.contains("lantern") || asset.contains("sconce")
                || asset.contains("tabletop_candle")) {
            return new Color(255, 203, 112);
        }
        if (asset.contains("crystal") || asset.contains("ice_crystals")) {
            return new Color(98, 214, 255);
        }
        if (asset.contains("rune") || asset.contains("shrine")) {
            return new Color(117, 236, 205);
        }
        if (asset.contains("alchemy")) {
            return new Color(168, 117, 236);
        }
        if (asset.contains("fairy_pool") || asset.contains("bubble_pool") || asset.contains("firefly")) {
            return new Color(112, 232, 154);
        }
        if (asset.contains("thaw_pond") || asset.contains("spring_pool") || asset.contains("snowmelt_pool")) {
            return new Color(138, 220, 255);
        }
        return null;
    }

    private float propGlowAlpha(String asset) {
        if (asset.contains("camp_fire") || asset.contains("campfire") || asset.contains("fire")) {
            return 0.42f;
        }
        if (asset.contains("forge") || asset.contains("oven") || asset.contains("stove") || asset.contains("hearth")) {
            return 0.34f;
        }
        if (asset.contains("tabletop_candle") || asset.contains("sconce")) {
            return 0.30f;
        }
        if (asset.startsWith("town_portal_")) {
            return 0.46f;
        }
        if (asset.contains("street_lamp") || asset.contains("lantern")) {
            return 0.36f;
        }
        return 0.28f;
    }

    private float propGlowPulse(WorldProp prop) {
        String asset = prop.asset();
        double phase = prop.x() * 0.7 + prop.y() * 0.4;
        if (isFireProp(asset)) {
            return (float) (0.88
                    + Math.sin(frame * 0.26 + phase) * 0.14
                    + Math.sin(frame * 0.57 + phase * 1.7) * 0.08);
        }
        if (isMagicGlowProp(asset)) {
            return (float) (0.90 + Math.sin(frame * 0.085 + phase) * 0.12);
        }
        return (float) (0.90 + Math.sin(frame * 0.10 + phase) * 0.10);
    }

    private float lightVisibilityForAsset(String asset) {
        float night = nightFactor();
        if (asset.startsWith("town_portal_")) {
            return 0.44f + night * 0.94f;
        }
        if (asset.contains("crystal") || asset.contains("rune") || asset.contains("shrine") || asset.contains("alchemy")
                || asset.contains("fairy_pool") || asset.contains("bubble_pool") || asset.contains("firefly")) {
            return 0.34f + night * 0.88f;
        }
        if (asset.contains("camp_fire") || asset.contains("campfire") || asset.contains("fire")
                || asset.contains("cookpot") || asset.contains("cooking_station")
                || asset.contains("forge") || asset.contains("oven") || asset.contains("stove")
                || asset.contains("hearth") || asset.contains("tabletop_candle")
                || asset.contains("street_lamp") || asset.contains("lantern") || asset.contains("sconce")) {
            return 0.12f + night * 1.10f;
        }
        return lightVisibility();
    }

    private int propGlowRadius(String asset) {
        if (asset.contains("camp_fire") || asset.contains("campfire") || asset.contains("fire")) {
            return scaled(96);
        }
        if (asset.contains("forge") || asset.contains("oven") || asset.contains("stove") || asset.contains("hearth")) {
            return scaled(88);
        }
        if (asset.contains("tabletop_candle")) {
            return scaled(42);
        }
        if (asset.contains("street_lamp") || asset.contains("lantern") || asset.contains("sconce")) {
            return scaled(72);
        }
        if (asset.contains("crystal") || asset.contains("rune") || asset.contains("shrine")) {
            return scaled(78);
        }
        if (asset.startsWith("town_portal_")) {
            return scaled(112);
        }
        return scaled(66);
    }

    private float lightVisibility() {
        if (isInteriorAtmosphereMap()) {
            return 0.58f + nightFactor() * 0.42f;
        }
        return (float) Math.min(1.22, 0.20 + nightFactor() * 1.05);
    }

    private float nightFactor() {
        return (float) Math.max(0.0, Math.min(1.0, (0.92 - state.daylightLevel()) / 0.74));
    }

    private int screenTileCenterX(int wx, int camX, int tileSize, double cameraOffsetX) {
        return (int) Math.round((wx - camX) * tileSize + tileSize / 2.0 - cameraOffsetX);
    }

    private int screenTileCenterY(int wy, int camY, int tileSize, double cameraOffsetY) {
        return (int) Math.round((wy - camY) * tileSize + tileSize / 2.0 - cameraOffsetY);
    }

    private void drawSpark(Graphics2D g, int cx, int cy, Color color, float alpha) {
        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0.0f, Math.min(1.0f, alpha))));
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 230));
        int size = scaled(3);
        g.fillOval(cx - size / 2, cy - size / 2, size, size);
        g.setComposite(oldComposite);
    }

    private void drawRadialGlow(Graphics2D g, int cx, int cy, int radius, Color color, float alpha) {
        Composite oldComposite = g.getComposite();
        Paint oldPaint = g.getPaint();
        Color[] colors = {
                new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(255, Math.round(alpha * 255))),
                new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(255, Math.round(alpha * 82))),
                new Color(color.getRed(), color.getGreen(), color.getBlue(), 0)
        };
        g.setComposite(AlphaComposite.SrcOver);
        g.setPaint(new RadialGradientPaint(cx, cy, Math.max(1, radius), RADIAL_GLOW_FRACTIONS, colors));
        g.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
        g.setPaint(oldPaint);
        g.setComposite(oldComposite);
    }

    private void drawWorldVignette(Graphics2D g) {
        int width = gameAreaWidth();
        int height = viewHeight();
        int radius = Math.max(width, height);
        Paint oldPaint = g.getPaint();
        Composite oldComposite = g.getComposite();
        float edgeAlpha = 0.10f + nightFactor() * 0.12f;
        g.setComposite(AlphaComposite.SrcOver);
        g.setPaint(new RadialGradientPaint(
                width * 0.46f,
                height * 0.42f,
                radius * 0.68f,
                new float[]{0.0f, 0.74f, 1.0f},
                new Color[]{
                        new Color(0, 0, 0, 0),
                        new Color(10, 13, 20, Math.round(edgeAlpha * 70)),
                        new Color(8, 10, 18, Math.round(edgeAlpha * 255))
                }
        ));
        g.fillRect(0, 0, width, height);
        g.setPaint(oldPaint);
        g.setComposite(oldComposite);
    }

    private boolean castsPropShadow(String asset) {
        return !asset.equals("location_farmland_tilled")
                && !asset.equals("location_farmland_wheat")
                && !asset.equals("location_graveyard_dirt")
                && !asset.equals("location_graveyard_path")
                && !asset.equals("location_dungeon_approach_path")
                && !asset.startsWith("town_park_accent_")
                && !asset.startsWith("interior_")
                && !isSoftGroundProp(asset)
                && !isFeatheryGroundProp(asset);
    }

    private boolean isSoftGroundProp(String asset) {
        return asset.startsWith("deco_soft_");
    }

    private boolean isNaturalLowProp(String asset) {
        return asset.startsWith("deco_")
                && !asset.contains("tree")
                && !asset.contains("pine")
                && !asset.contains("root")
                && !asset.contains("log")
                && !asset.contains("stump")
                && !asset.contains("totem")
                && !asset.contains("shrine")
                && !asset.contains("rune")
                && !asset.contains("camp")
                && !asset.contains("signpost")
                && !asset.contains("milestone")
                && !asset.contains("jar")
                && (isFeatheryGroundProp(asset)
                || asset.contains("stone")
                || asset.contains("rock")
                || asset.contains("pebble")
                || asset.contains("moss")
                || asset.contains("cairn")
                || asset.contains("pond")
                || asset.contains("pool")
                || asset.contains("lily")
                || asset.contains("oasis"));
    }

    private boolean isFeatheryGroundProp(String asset) {
        return isSoftGroundProp(asset)
                || asset.contains("grass")
                || asset.contains("flower")
                || asset.contains("bloom")
                || asset.contains("fern")
                || asset.contains("bush")
                || asset.contains("mushroom")
                || asset.contains("reed")
                || asset.contains("cattail")
                || asset.contains("plant")
                || asset.contains("lily")
                || asset.contains("duckweed")
                || asset.contains("floating")
                || asset.contains("weed")
                || asset.contains("leaf")
                || asset.contains("scrub");
    }

    private void drawCloudLayer(Graphics2D g, int camX, int camY) {
        if (!WorldMap.OVERWORLD_ID.equals(state.currentMapId)) {
            return;
        }
        int ts = tileSize();
        int density = blendedCloudDensity();
        float opacity = blendedCloudOpacity();
        if (opacity <= WEATHER_VISIBILITY_EPSILON || density <= 0) {
            return;
        }
        Graphics2D cloudG = (Graphics2D) g.create();
        cloudG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        int cloudLimit = Math.min(CLOUD_SEEDS.length, weatherQuality().cloudLayers);
        for (int i = 0; i < cloudLimit; i++) {
            int seed = CLOUD_SEEDS[i];
            if (Math.floorMod(seed, 100) >= density) {
                continue;
            }
            double wx = Math.floorMod(seed / 17, WorldMap.COLS + 34) - 17;
            double wy = Math.floorMod(seed / 53, WorldMap.ROWS + 22) - 11;
            if (wx < camX - 7 || wy < camY - 4 || wx >= camX + lastVisibleCols + 7 || wy >= camY + lastVisibleRows + 4) {
                continue;
            }
            int sizeW = scaled(178 + Math.floorMod(seed / 97, 92));
            int sizeH = scaled(62 + Math.floorMod(seed / 193, 34));
            int x = (int) Math.round((wx - camX) * ts) - sizeW / 2;
            int y = (int) Math.round((wy - camY) * ts) - sizeH / 2;
            cloudG.drawImage(assets.image("cloud_billow_" + Math.floorMod(seed, 3), sizeW, sizeH), x, y, null);
        }
        cloudG.dispose();
    }

    private int cloudDensity(WeatherCondition weather) {
        return switch (weather) {
            case CLEAR -> 18;
            case HEAT_HAZE -> 10;
            case DUST -> 36;
            case CLOUDY, FOG -> 62;
            case RAIN, SNOW -> 78;
            case STORM, BLIZZARD -> 88;
        };
    }

    private float cloudOpacity(WeatherCondition weather) {
        return switch (weather) {
            case CLEAR -> 0.72f;
            case HEAT_HAZE -> 0.45f;
            case DUST -> 0.62f;
            case STORM, BLIZZARD -> 0.94f;
            default -> 0.84f;
        };
    }

    private int blendedCloudDensity() {
        double total = 0.0;
        double density = 0.0;
        for (WeatherCondition condition : WeatherCondition.values()) {
            double intensity = weather.intensity(condition);
            density += cloudDensity(condition) * intensity;
            total += intensity;
        }
        if (total < 1.0) {
            density += cloudDensity(WeatherCondition.CLEAR) * (1.0 - total);
            total = 1.0;
        }
        return (int) Math.round(density / Math.max(1.0, total));
    }

    private float blendedCloudOpacity() {
        double total = 0.0;
        double opacity = 0.0;
        for (WeatherCondition condition : WeatherCondition.values()) {
            double intensity = weather.intensity(condition);
            opacity += cloudOpacity(condition) * intensity;
            total += intensity;
        }
        if (total < 1.0) {
            opacity += cloudOpacity(WeatherCondition.CLEAR) * (1.0 - total);
            total = 1.0;
        }
        return (float) clamp(opacity / Math.max(1.0, total), 0.0, 1.0);
    }

    private void drawWorldAtmosphere(Graphics2D g) {
        int mapW = gameAreaWidth();
        int mapH = viewHeight();
        Graphics2D atmosphere = (Graphics2D) g.create();
        atmosphere.setClip(0, 0, mapW, mapH);
        if (isInteriorAtmosphereMap()) {
            drawInteriorTimeOverlay(atmosphere, mapW, mapH);
            atmosphere.dispose();
            return;
        }
        drawTimeOverlay(atmosphere, mapW, mapH);
        if (weather.effectsVisibleOnCurrentMap()) {
            long weatherStarted = renderMetrics.start();
            drawWeatherOverlay(atmosphere, mapW, mapH);
            renderMetrics.record("weather", weatherStarted);
        }
        atmosphere.dispose();
    }

    private boolean isInteriorAtmosphereMap() {
        String kind = state.world.kind(state.currentMapId);
        return "interior".equals(kind) || "dungeon".equals(kind);
    }

    private void drawInteriorTimeOverlay(Graphics2D g, int width, int height) {
        double daylight = state.daylightLevel();
        float dimAlpha = (float) ((1.0 - daylight) * 0.34);
        if (dimAlpha <= 0.02f) {
            return;
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, dimAlpha));
        g.setColor(new Color(10, 9, 15));
        g.fillRect(0, 0, width, height);
        g.setComposite(AlphaComposite.SrcOver);
    }

    private void drawTimeOverlay(Graphics2D g, int width, int height) {
        double daylight = state.daylightLevel();
        float nightAlpha = (float) ((1.0 - daylight) * 0.68);
        if (nightAlpha > 0.02f) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, nightAlpha));
            g.setColor(new Color(7, 14, 35));
            g.fillRect(0, 0, width, height);
        }
        int minutes = state.timeOfDayMinutes();
        boolean twilight = (minutes >= 300 && minutes < 420) || (minutes >= 1020 && minutes < 1200);
        if (twilight) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.16f));
            g.setPaint(new GradientPaint(0, 0, new Color(241, 155, 84), 0, height, new Color(58, 82, 132)));
            g.fillRect(0, 0, width, height);
        }
        if (daylight < 0.28) {
            drawStars(g, width, height, (float) ((0.28 - daylight) / 0.10 * 0.42));
        }
        g.setComposite(AlphaComposite.SrcOver);
    }

    private void drawStars(Graphics2D g, int width, int height, float alpha) {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.0f, Math.min(0.42f, alpha))));
        BufferedImage star = particleSprite("star");
        for (int i = 0; i < 70; i++) {
            int seed = Math.abs(i * 928371 + 54123);
            int x = Math.floorMod(seed, Math.max(1, width));
            int y = Math.floorMod(seed / 71, Math.max(1, height * 2 / 3));
            int size = 3 + Math.floorMod(seed / 311, 5);
            g.drawImage(star, x, y, size, size, null);
        }
    }

    private void drawWeatherOverlay(Graphics2D g, int width, int height) {
        for (WeatherCondition weather : WeatherCondition.values()) {
            double intensity = this.weather.intensity(weather);
            if (weather == WeatherCondition.CLEAR || intensity <= WEATHER_VISIBILITY_EPSILON) {
                continue;
            }
            drawWeatherCondition(g, width, height, weather, intensity);
        }
        g.setComposite(AlphaComposite.SrcOver);
    }

    private void drawWeatherCondition(Graphics2D g, int width, int height, WeatherCondition weather, double intensity) {
        switch (weather) {
            case RAIN -> {
                drawRainVeil(g, width, height, false, intensity);
                drawCachedPrecipitation(g, width, height, "rain", intensity, layer -> {
                    drawRain(layer, width, height, 420, false, intensity);
                    drawRainSplashes(layer, width, height, 90, false, intensity);
                });
            }
            case STORM -> {
                tint(g, width, height, new Color(22, 30, 42), (float) (0.24f * intensity));
                drawRainVeil(g, width, height, true, intensity);
                drawCachedPrecipitation(g, width, height, "storm", intensity, layer -> {
                    drawRain(layer, width, height, 650, true, intensity);
                    drawRainSplashes(layer, width, height, 150, true, intensity);
                });
                drawLightning(g, width, height, intensity);
            }
            case SNOW -> drawCachedPrecipitation(g, width, height, "snow", intensity,
                    layer -> drawSnow(layer, width, height, 70, false, intensity));
            case BLIZZARD -> {
                tint(g, width, height, new Color(220, 232, 240), (float) (0.18f * intensity));
                drawCachedPrecipitation(g, width, height, "blizzard", intensity,
                        layer -> drawSnow(layer, width, height, 130, true, intensity));
            }
            case FOG -> drawFadedWeatherLayer(g, width, height, "fog", (float) intensity, layer -> drawFog(layer, width, height));
            case DUST -> drawFadedWeatherLayer(g, width, height, "dust", (float) intensity, layer -> drawDust(layer, width, height));
            case HEAT_HAZE -> drawFadedWeatherLayer(g, width, height, "heat_haze", (float) intensity, layer -> drawHeatHaze(layer, width, height));
            case CLOUDY -> tint(g, width, height, new Color(84, 91, 105), (float) (0.10f * intensity));
            case CLEAR -> {
            }
        }
    }

    private void drawFadedWeatherLayer(Graphics2D g, int width, int height, String key, float alpha, WeatherLayerPainter painter) {
        if (alpha <= WEATHER_VISIBILITY_EPSILON) {
            return;
        }
        if (alpha >= 0.999f) {
            drawCachedStableWeatherLayer(g, width, height, key, painter);
            return;
        }
        int interval = dynamicWeatherFrameInterval(key, alpha);
        weatherLayerCaches.drawFaded(g, width, height, weatherLayerKey(key), state.zoom, frame, alpha, interval, painter::paint);
    }

    private void drawCachedPrecipitation(Graphics2D g, int width, int height, String key, double intensity, WeatherLayerPainter painter) {
        int interval = dynamicWeatherFrameInterval(key, intensity);
        if (interval <= 1) {
            painter.paint(g);
            return;
        }
        weatherLayerCaches.drawPrecipitation(g, width, height, weatherLayerKey(key), state.zoom, frame,
                weatherIntensityBucket(intensity), interval, painter::paint,
                frameDelta -> precipitationShiftX(key, frameDelta),
                frameDelta -> precipitationShiftY(key, frameDelta));
    }

    private void drawCachedStableWeatherLayer(Graphics2D g, int width, int height, String key, WeatherLayerPainter painter) {
        weatherLayerCaches.drawStable(g, width, height, weatherLayerKey(key), state.zoom, frame,
                dynamicWeatherFrameInterval(key, 1.0), painter::paint);
    }

    private String weatherLayerKey(String key) {
        return key + ":" + weatherQuality().key;
    }

    private int dynamicWeatherFrameInterval(String key, double intensity) {
        if (intensity <= WEATHER_VISIBILITY_EPSILON) {
            return 1;
        }
        int baseInterval = switch (key) {
            case "rain", "storm" -> 4;
            case "snow", "blizzard", "dust" -> 3;
            default -> 2;
        };
        return baseInterval + weatherQuality().cacheIntervalBonus;
    }

    private int weatherIntensityBucket(double intensity) {
        int steps = weatherQuality().intensityCacheSteps;
        return clamp((int) Math.round(clamp(intensity, 0.0, 1.0) * steps), 0, steps);
    }

    private int precipitationShiftX(String key, int frameDelta) {
        if (frameDelta <= 0) {
            return 0;
        }
        return switch (key) {
            case "rain" -> (int) Math.round(frameDelta * (weather.windX() * 8.5 + 7.0));
            case "storm" -> (int) Math.round(frameDelta * (weather.windX() * 12.0 + 9.0));
            default -> 0;
        };
    }

    private int precipitationShiftY(String key, int frameDelta) {
        if (frameDelta <= 0) {
            return 0;
        }
        return switch (key) {
            case "rain" -> frameDelta * 23;
            case "storm" -> frameDelta * 30;
            case "snow" -> frameDelta * 2;
            case "blizzard" -> frameDelta * 4;
            default -> 0;
        };
    }

    private void tint(Graphics2D g, int width, int height, Color color, float alpha) {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.setColor(color);
        g.fillRect(0, 0, width, height);
    }

    private void drawRain(Graphics2D g, int width, int height, int count, boolean heavy, double intensity) {
        if (count <= 0 || intensity <= WEATHER_VISIBILITY_EPSILON) {
            return;
        }
        BufferedImage field = particleSprite(heavy ? "rain_field_heavy" : "rain_field");
        double gust = Math.sin(frame * 0.055) * 0.16 + Math.sin(frame * 0.019 + 1.7) * 0.10;
        int tileW = Math.max(1, scaled(512));
        int tileH = Math.max(1, scaled(512));
        int offsetX = Math.floorMod((int) Math.round(frame * ((weather.windX() + gust) * (heavy ? 15.0 : 11.0) + (heavy ? 9.0 : 7.0))), tileW);
        int offsetY = Math.floorMod(frame * (heavy ? 30 : 23), tileH);
        drawTiledWeatherSprite(g, field, width, height, tileW, tileH, offsetX, offsetY,
                (float) ((heavy ? 0.74f : 0.62f) * intensity));
        if (heavy) {
            drawTiledWeatherSprite(g, field, width, height, tileW, tileH,
                    offsetX + tileW / 3, offsetY + tileH / 2, (float) (0.32f * intensity));
        }
    }

    private void drawRainVeil(Graphics2D g, int width, int height, boolean heavy, double intensity) {
        BufferedImage sheet = particleSprite(heavy ? "rain_sheet_heavy" : "rain_sheet");
        int tileW = Math.max(1, scaled(192));
        int tileH = Math.max(1, scaled(192));
        int offsetX = Math.floorMod((int) Math.round(frame * (weather.windX() * 9.0 + 2.0)), tileW);
        int offsetY = Math.floorMod(frame * (heavy ? 18 : 13), tileH);
        weatherLayerCaches.drawScrollingTile(g, sheet, width, height, weatherLayerKey(heavy ? "rain_veil_heavy" : "rain_veil"),
                state.zoom, tileW, tileH, offsetX, offsetY, (float) ((heavy ? 0.30f : 0.22f) * intensity));
    }

    private void drawRainSplashes(Graphics2D g, int width, int height, int count, boolean heavy, double intensity) {
        if (count <= 0 || intensity <= WEATHER_VISIBILITY_EPSILON) {
            return;
        }
        BufferedImage splashField = particleSprite(heavy ? "rain_splash_field_heavy" : "rain_splash_field");
        int tileW = Math.max(1, scaled(512));
        int tileH = Math.max(1, scaled(256));
        int offsetX = Math.floorMod((int) Math.round(frame * weather.windX() * 4.0), tileW);
        int offsetY = Math.floorMod(frame * (heavy ? 5 : 3), tileH);
        drawTiledWeatherSprite(g, splashField, width, height, tileW, tileH, offsetX, offsetY,
                (float) ((heavy ? 0.42f : 0.30f) * intensity));
    }

    private void drawSnow(Graphics2D g, int width, int height, int count, boolean heavy, double intensity) {
        if (count <= 0 || intensity <= WEATHER_VISIBILITY_EPSILON) {
            return;
        }
        BufferedImage field = particleSprite(heavy ? "snow_field_heavy" : "snow_field");
        int tileW = Math.max(1, scaled(512));
        int tileH = Math.max(1, scaled(512));
        int speed = heavy ? 4 : 2;
        int drift = (int) Math.round(Math.sin(frame * 0.04) * scaled(heavy ? 18 : 9));
        int offsetX = Math.floorMod(drift + (int) Math.round(frame * weather.windX() * 1.2), tileW);
        int offsetY = Math.floorMod(frame * speed, tileH);
        drawTiledWeatherSprite(g, field, width, height, tileW, tileH, offsetX, offsetY,
                (float) ((heavy ? 0.70f : 0.52f) * intensity));
        drawTiledWeatherSprite(g, field, width, height, tileW, tileH,
                offsetX + tileW / 2, offsetY + tileH / 3, (float) ((heavy ? 0.22f : 0.14f) * intensity));
    }

    private void drawFog(Graphics2D g, int width, int height) {
        int fogRenderScale = weatherQuality().fogRenderScale;
        int fogW = Math.max(1, (width + fogRenderScale - 1) / fogRenderScale);
        int fogH = Math.max(1, (height + fogRenderScale - 1) / fogRenderScale);
        if (fogBuffer == null || fogBuffer.getWidth() != fogW || fogBuffer.getHeight() != fogH) {
            fogBuffer = new BufferedImage(fogW, fogH, BufferedImage.TYPE_INT_ARGB);
        }

        Graphics2D fog = fogBuffer.createGraphics();
        fog.setComposite(AlphaComposite.Clear);
        fog.fillRect(0, 0, fogW, fogH);
        fog.setComposite(AlphaComposite.SrcOver);
        fog.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        fog.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        fog.scale(1.0 / fogRenderScale, 1.0 / fogRenderScale);
        drawFogDirect(fog, width, height);
        fog.dispose();

        Graphics2D fogOut = (Graphics2D) g.create();
        fogOut.setComposite(AlphaComposite.SrcOver);
        fogOut.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        fogOut.drawImage(fogBuffer, 0, 0, width, height, null);
        fogOut.dispose();
    }

    private void drawFogDirect(Graphics2D g, int width, int height) {
        tint(g, width, height, new Color(190, 204, 196), 0.12f);
        tint(g, width, height, new Color(228, 234, 226), 0.04f);
        drawFogWash(g, width, height);
        drawFogBands(g, width, height);
        drawGroundFog(g, width, height);
        drawFogWisps(g, width, height);
        drawFogMotes(g, width, height);
    }

    private void drawDust(Graphics2D g, int width, int height) {
        tint(g, width, height, new Color(175, 122, 64), 0.22f);
        tint(g, width, height, new Color(235, 190, 111), 0.09f);
        drawSandVeil(g, width, height);
        drawSandVortices(g, width, height);
        drawSandStreaks(g, width, height);
        drawSandMotes(g, width, height);
    }

    private void drawFogWash(Graphics2D g, int width, int height) {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.10f));
        g.setPaint(new GradientPaint(0, 0, new Color(214, 224, 214, 22), 0, height, new Color(172, 188, 180, 42)));
        g.fillRect(0, 0, width, height);
        g.setPaint(null);
    }

    private void drawFogBands(Graphics2D g, int width, int height) {
        BufferedImage band = particleSprite("fog_band");
        int spill = scaled(260);
        int drawW = width + spill * 2;
        int drawH = scaled(118);
        for (int i = 0; i < 4; i++) {
            int seed = Math.abs(i * 37321 + 4127);
            int y = Math.floorMod(seed / 31, Math.max(1, height + drawH)) - drawH / 2;
            int drift = (int) Math.round(Math.sin((frame + i * 43) * 0.012) * scaled(70)
                    + frame * (0.28 + weather.windX() * 0.8 + i * 0.03));
            int x = -spill + Math.floorMod(drift + seed, Math.max(1, spill * 2)) - spill;
            drawSpriteAlpha(g, band, x, y, drawW, drawH, 0.12f + Math.floorMod(seed / 97, 6) / 100.0f);
        }
    }

    private void drawGroundFog(Graphics2D g, int width, int height) {
        BufferedImage band = particleSprite("fog_band");
        int spill = scaled(320);
        int drawW = width + spill * 2;
        int drawH = scaled(150);
        int baseY = Math.max(0, height - scaled(245));
        for (int i = 0; i < 2; i++) {
            int y = baseY + i * scaled(56);
            int x = -spill + (int) Math.round(Math.sin((frame + i * 61) * 0.010) * scaled(82));
            drawSpriteAlpha(g, band, x, y, drawW, drawH, 0.16f - i * 0.02f);
        }
    }

    private void drawFogWisps(Graphics2D g, int width, int height) {
        BufferedImage wisp = particleSprite("fog_wisp");
        BufferedImage curl = particleSprite("fog_curl");
        for (int i = 0; i < 10; i++) {
            int seed = Math.abs(i * 77687 + 3319);
            boolean foreground = Math.floorMod(seed / 23, 4) == 0;
            int drawW = scaled((foreground ? 420 : 280) + Math.floorMod(seed, foreground ? 270 : 210));
            int drawH = scaled((foreground ? 72 : 42) + Math.floorMod(seed / 37, foreground ? 52 : 34));
            int yRange = Math.max(1, height - scaled(22));
            int y = Math.floorMod(seed / 73 + (int) Math.round(Math.sin((frame + i * 19) * 0.025) * scaled(13)), yRange) - scaled(10);
            int speed = foreground ? 1 : 0;
            int x = Math.floorMod(frame * speed + (int) Math.round(frame * weather.windX() * 1.2) + seed,
                    width + drawW + scaled(220)) - drawW - scaled(110);
            float alpha = foreground ? 0.24f : 0.17f;
            drawSpriteAlpha(g, Math.floorMod(seed, 5) == 0 ? curl : wisp, x, y, drawW, drawH, alpha);
        }
    }

    private void drawFogMotes(Graphics2D g, int width, int height) {
        BufferedImage mote = particleSprite("dust");
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.08f));
        for (int i = 0; i < 10; i++) {
            int seed = Math.abs(i * 69191 + 1009);
            int x = Math.floorMod(seed + frame / 3, width + 44) - 22;
            int y = Math.floorMod(seed / 43 + (int) Math.round(Math.sin((frame + i * 11) * 0.032) * scaled(9)), height + 30) - 15;
            int size = Math.max(2, scaled(3 + Math.floorMod(seed / 29, 5)));
            g.drawImage(mote, x, y, size * 3, size, null);
        }
    }

    private void drawSandVeil(Graphics2D g, int width, int height) {
        BufferedImage veil = particleSprite("sand_veil");
        int tileW = Math.max(1, scaled(256));
        int tileH = Math.max(1, scaled(192));
        int offsetX = Math.floorMod((int) Math.round(frame * (10.0 + weather.windX() * 18.0)), tileW);
        int offsetY = Math.floorMod(frame * 2, tileH);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.34f));
        for (int y = -offsetY; y < height; y += tileH) {
            for (int x = -offsetX; x < width; x += tileW) {
                g.drawImage(veil, x, y, tileW, tileH, null);
            }
        }
    }

    private void drawSandStreaks(Graphics2D g, int width, int height) {
        BufferedImage streak = particleSprite("sand_streak_field");
        double gust = Math.sin(frame * 0.043) * 0.22 + Math.sin(frame * 0.017 + 2.1) * 0.12;
        int tileW = Math.max(1, scaled(512));
        int tileH = Math.max(1, scaled(384));
        int offsetX = Math.floorMod((int) Math.round(frame * (11.0 + (weather.windX() + gust) * 16.0)), tileW);
        int offsetY = Math.floorMod(frame, tileH);
        drawTiledWeatherSprite(g, streak, width, height, tileW, tileH, offsetX, offsetY, 0.72f);
        drawTiledWeatherSprite(g, streak, width, height, tileW, tileH,
                offsetX + tileW / 2, offsetY + tileH / 3, 0.28f);
    }

    private void drawSandMotes(Graphics2D g, int width, int height) {
        BufferedImage mote = particleSprite("sand_mote_field");
        int tileW = Math.max(1, scaled(512));
        int tileH = Math.max(1, scaled(384));
        int offsetX = Math.floorMod(frame * 5, tileW);
        int offsetY = Math.floorMod(frame, tileH);
        drawTiledWeatherSprite(g, mote, width, height, tileW, tileH, offsetX, offsetY, 0.58f);
    }

    private void drawSandVortices(Graphics2D g, int width, int height) {
        BufferedImage mote = particleSprite("dust");
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.27f));
        for (int i = 0; i < 7; i++) {
            int seed = Math.abs(i * 55291 + 6113);
            int centerX = Math.floorMod(seed + frame * (2 + i % 3), width + scaled(180)) - scaled(90);
            int baseY = Math.floorMod(seed / 53 + frame / 2, Math.max(1, height - scaled(60))) + scaled(25);
            int radius = scaled(18 + Math.floorMod(seed / 97, 32));
            int heightSpan = scaled(46 + Math.floorMod(seed / 131, 58));
            for (int step = 0; step < 7; step++) {
                double phase = (frame * 0.12 + seed * 0.01 + step * 0.9);
                int x = centerX + (int) Math.round(Math.sin(phase) * radius * (1.0 - step * 0.08));
                int y = baseY - step * heightSpan / 7;
                int drawW = Math.max(2, scaled(12 + step * 3));
                int drawH = Math.max(1, scaled(4 + step));
                g.drawImage(mote, x, y, drawW, drawH, null);
            }
        }
    }

    private void drawHeatHaze(Graphics2D g, int width, int height) {
        tint(g, width, height, new Color(230, 176, 96), 0.08f);
        BufferedImage shimmer = particleSprite("heat_haze");
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f));
        for (int i = 0; i < 7; i++) {
            int y = height - scaled(150 + i * 46);
            int x = (int) Math.round(Math.sin((frame + i * 31) * 0.035) * scaled(20));
            g.drawImage(shimmer, x - scaled(40), y, width + scaled(100), scaled(38), null);
        }
    }

    private void drawLightning(Graphics2D g, int width, int height, double intensity) {
        if (intensity < 0.55 || Math.floorMod(frame, 190) > 5) {
            return;
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.24f * intensity)));
        g.setColor(new Color(234, 240, 255));
        g.fillRect(0, 0, width, height);
    }

    private void drawTiledWeatherSprite(Graphics2D g, BufferedImage sprite, int width, int height,
                                        int tileW, int tileH, int offsetX, int offsetY, float alpha) {
        if (alpha <= 0.0f || tileW <= 0 || tileH <= 0) {
            return;
        }
        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1.0f, alpha)));
        int startX = -Math.floorMod(offsetX, tileW);
        int startY = -Math.floorMod(offsetY, tileH);
        for (int y = startY; y < height; y += tileH) {
            for (int x = startX; x < width; x += tileW) {
                g.drawImage(sprite, x, y, tileW, tileH, null);
            }
        }
        g.setComposite(oldComposite);
    }

    private void drawInteriorLightTints(Graphics2D g, int camX, int camY, int visibleCols, int visibleRows, int tileSize) {
        Composite oldComposite = g.getComposite();
        float daylight = (float) state.daylightLevel();
        float warmAlpha = 0.018f + (1.0f - daylight) * 0.028f;
        for (int sy = 0; sy < visibleRows; sy++) {
            for (int sx = 0; sx < visibleCols; sx++) {
                int wx = camX + sx;
                int wy = camY + sy;
                char tile = state.world.tileAt(state.currentMapId, wx, wy);
                if (tile == 'x') {
                    continue;
                }
                int px = sx * tileSize;
                int py = sy * tileSize;
                g.setComposite(AlphaComposite.SrcOver.derive(warmAlpha));
                g.setColor(new Color(255, 208, 142));
                g.fillRect(px, py, tileSize, tileSize);
                if (tile == 'o') {
                    g.setComposite(AlphaComposite.SrcOver.derive(0.035f));
                    g.setColor(new Color(0, 0, 0));
                    g.fillRect(px, py + tileRelative(31, tileSize), tileSize, tileRelative(7, tileSize));
                }
            }
        }
        g.setComposite(oldComposite);
    }

    private void drawSpriteAlpha(Graphics2D g, BufferedImage sprite, int x, int y, int width, int height, float alpha) {
        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.drawImage(sprite, x, y, width, height, null);
        g.setComposite(oldComposite);
    }

    private void drawParticleSprite(Graphics2D g, BufferedImage sprite, int x, int y, int width, int height, double angle, float alpha) {
        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        if (Math.abs(angle) < 0.0001) {
            g.drawImage(sprite, x, y, width, height, null);
            g.setComposite(oldComposite);
            return;
        }
        AffineTransform oldTransform = g.getTransform();
        g.translate(x + width / 2.0, y + height / 2.0);
        g.rotate(angle);
        g.drawImage(sprite, -width / 2, -height / 2, width, height, null);
        g.setTransform(oldTransform);
        g.setComposite(oldComposite);
    }

    @FunctionalInterface
    private interface WeatherLayerPainter {
        void paint(Graphics2D g);
    }

    private BufferedImage particleSprite(String key) {
        return particleSprites.computeIfAbsent(key, this::createParticleSprite);
    }

    private BufferedImage createParticleSprite(String key) {
        return switch (key) {
            case "rain" -> createRainSprite(false);
            case "rain_heavy" -> createRainSprite(true);
            case "rain_field" -> createRainFieldSprite(false);
            case "rain_field_heavy" -> createRainFieldSprite(true);
            case "rain_sheet" -> createRainSheetSprite(false);
            case "rain_sheet_heavy" -> createRainSheetSprite(true);
            case "rain_splash" -> createRainSplashSprite();
            case "rain_splash_field" -> createRainSplashFieldSprite(false);
            case "rain_splash_field_heavy" -> createRainSplashFieldSprite(true);
            case "snow" -> createSnowSprite(false);
            case "snow_heavy" -> createSnowSprite(true);
            case "snow_field" -> createSnowFieldSprite(false);
            case "snow_field_heavy" -> createSnowFieldSprite(true);
            case "fog_wisp" -> createFogWispSprite();
            case "fog_band" -> createFogBandSprite();
            case "fog_curl" -> createFogCurlSprite();
            case "dust" -> createDustSprite();
            case "sand_veil" -> createSandVeilSprite();
            case "sand_streak" -> createSandStreakSprite();
            case "sand_streak_field" -> createSandStreakFieldSprite();
            case "sand_mote_field" -> createSandMoteFieldSprite();
            case "heat_haze" -> createHeatHazeSprite();
            case "star" -> createStarSprite();
            default -> createDustSprite();
        };
    }

    private BufferedImage createRainSprite(boolean heavy) {
        BufferedImage image = new BufferedImage(24, 96, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = spriteGraphics(image);
        g.setStroke(new BasicStroke(heavy ? 4.5f : 2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setPaint(new GradientPaint(12, 2, new Color(226, 242, 255, heavy ? 18 : 12), 12, 92, new Color(176, 211, 238, heavy ? 176 : 138)));
        g.drawLine(12, 4, 12, 92);
        g.setStroke(new BasicStroke(heavy ? 1.8f : 1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setPaint(new GradientPaint(10, 12, new Color(250, 253, 255, heavy ? 120 : 88), 10, 70, new Color(206, 226, 242, 16)));
        g.drawLine(10, 12, 10, 70);
        g.dispose();
        return image;
    }

    private BufferedImage createRainFieldSprite(boolean heavy) {
        int size = 512;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = spriteGraphics(image);
        BufferedImage drop = createRainSprite(heavy);
        int count = heavy ? 250 : 170;
        double baseAngle = heavy ? -0.42 : -0.34;
        for (int i = 0; i < count; i++) {
            int seed = Math.abs(i * 62003 + (heavy ? 18419 : 8419));
            boolean foreground = Math.floorMod(seed / 17, 5) == 0;
            int x = Math.floorMod(seed, size + 120) - 60;
            int y = Math.floorMod(seed / 37, size + 96) - 48;
            int drawW = (foreground ? 5 : 3) + Math.floorMod(seed / 17, heavy ? 4 : 3);
            int drawH = (foreground ? 28 : 18) + Math.floorMod(seed / 31, heavy ? 20 : 13);
            double angle = baseAngle + (Math.floorMod(seed / 101, 9) - 4) * 0.018;
            float alpha = Math.min(0.84f, (heavy ? 0.56f : 0.46f)
                    + (foreground ? 0.18f : 0.0f)
                    + Math.floorMod(seed / 43, 18) / 100.0f);
            drawParticleSprite(g, drop, x, y, drawW, drawH, angle, alpha);
        }
        g.dispose();
        return image;
    }

    private BufferedImage createRainSheetSprite(boolean heavy) {
        BufferedImage image = new BufferedImage(192, 192, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = spriteGraphics(image);
        g.setStroke(new BasicStroke(heavy ? 1.7f : 1.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < (heavy ? 46 : 32); i++) {
            int seed = Math.abs(i * 48271 + 9311);
            int x = Math.floorMod(seed, 230) - 24;
            int y = Math.floorMod(seed / 37, 210) - 16;
            int len = heavy ? 36 + Math.floorMod(seed / 61, 32) : 24 + Math.floorMod(seed / 61, 24);
            int lean = heavy ? 10 + Math.floorMod(seed / 97, 13) : 7 + Math.floorMod(seed / 97, 10);
            int alpha = heavy ? 78 + Math.floorMod(seed / 23, 72) : 50 + Math.floorMod(seed / 23, 58);
            g.setColor(new Color(190, 218, 240, alpha));
            g.drawLine(x, y, x - lean, y + len);
        }
        g.dispose();
        return image;
    }

    private BufferedImage createRainSplashSprite() {
        BufferedImage image = new BufferedImage(48, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = spriteGraphics(image);
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(198, 225, 242, 104));
        g.drawArc(9, 8, 30, 9, 12, 156);
        g.setColor(new Color(230, 243, 252, 132));
        g.drawLine(18, 12, 14, 5);
        g.drawLine(26, 12, 28, 4);
        g.drawLine(32, 12, 37, 7);
        g.setComposite(AlphaComposite.SrcOver.derive(0.34f));
        g.fillOval(12, 12, 26, 4);
        g.dispose();
        return image;
    }

    private BufferedImage createRainSplashFieldSprite(boolean heavy) {
        BufferedImage image = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = spriteGraphics(image);
        BufferedImage splash = createRainSplashSprite();
        int count = heavy ? 100 : 64;
        for (int i = 0; i < count; i++) {
            int seed = Math.abs(i * 51437 + (heavy ? 92719 : 2719));
            int x = Math.floorMod(seed, 552) - 20;
            int y = Math.floorMod(seed / 29, 280) - 12;
            int drawW = 16 + Math.floorMod(seed / 47, 16);
            int drawH = 6 + Math.floorMod(seed / 89, 5);
            float alpha = (heavy ? 0.24f : 0.18f) + Math.floorMod(seed / 31, 12) / 100.0f;
            drawParticleSprite(g, splash, x, y, drawW, drawH, 0.0, alpha);
        }
        g.dispose();
        return image;
    }

    private BufferedImage createSnowSprite(boolean heavy) {
        int size = 40;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = spriteGraphics(image);
        drawSoftBlob(g, size / 2, size / 2, heavy ? 18 : 14, new Color(246, 251, 255), heavy ? 186 : 142);
        g.setComposite(AlphaComposite.SrcOver.derive(heavy ? 0.54f : 0.38f));
        g.setStroke(new BasicStroke(heavy ? 1.8f : 1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(255, 255, 255));
        g.drawLine(size / 2, 7, size / 2, size - 7);
        g.drawLine(7, size / 2, size - 7, size / 2);
        g.drawLine(11, 11, size - 11, size - 11);
        g.drawLine(size - 11, 11, 11, size - 11);
        g.dispose();
        return image;
    }

    private BufferedImage createSnowFieldSprite(boolean heavy) {
        int size = 512;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = spriteGraphics(image);
        BufferedImage flake = createSnowSprite(heavy);
        int count = heavy ? 170 : 100;
        for (int i = 0; i < count; i++) {
            int seed = Math.abs(i * 73241 + (heavy ? 41939 : 11939));
            int x = Math.floorMod(seed, size + 40) - 20;
            int y = Math.floorMod(seed / 29, size + 30) - 15;
            int drawSize = (heavy ? 7 : 5) + Math.floorMod(seed / 43, 5);
            float alpha = (heavy ? 0.56f : 0.42f) + Math.floorMod(seed / 67, 12) / 100.0f;
            drawSpriteAlpha(g, flake, x, y, drawSize, drawSize, Math.min(0.78f, alpha));
        }
        g.dispose();
        return image;
    }

    private BufferedImage createFogWispSprite() {
        BufferedImage image = new BufferedImage(384, 96, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = spriteGraphics(image);
        drawSoftBlob(g, 58, 50, 76, new Color(220, 228, 218), 82);
        drawSoftBlob(g, 132, 42, 92, new Color(232, 237, 229), 92);
        drawSoftBlob(g, 226, 54, 110, new Color(210, 221, 212), 78);
        drawSoftBlob(g, 316, 44, 82, new Color(236, 240, 232), 80);
        g.setComposite(AlphaComposite.SrcOver.derive(0.20f));
        g.setColor(new Color(248, 250, 245));
        g.fillOval(18, 32, 330, 28);
        g.setComposite(AlphaComposite.SrcOver.derive(0.11f));
        g.fillOval(70, 56, 280, 18);
        g.dispose();
        return image;
    }

    private BufferedImage createFogBandSprite() {
        BufferedImage image = new BufferedImage(512, 128, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = spriteGraphics(image);
        drawSoftBlob(g, 92, 66, 86, new Color(231, 238, 231), 52);
        drawSoftBlob(g, 202, 58, 112, new Color(238, 243, 236), 58);
        drawSoftBlob(g, 334, 72, 120, new Color(218, 230, 221), 48);
        drawSoftBlob(g, 438, 56, 80, new Color(239, 242, 236), 42);
        g.setComposite(AlphaComposite.SrcOver.derive(0.09f));
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(245, 248, 242));
        for (int y = 42; y < 96; y += 18) {
            for (int x = 34; x < 456; x += 88) {
                g.drawArc(x, y, 112, 18, 8, 164);
            }
        }
        g.setComposite(AlphaComposite.SrcOver.derive(0.07f));
        g.setColor(new Color(196, 211, 203));
        g.fillOval(64, 72, 380, 24);
        g.dispose();
        return image;
    }

    private BufferedImage createFogCurlSprite() {
        BufferedImage image = new BufferedImage(220, 92, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = spriteGraphics(image);
        drawSoftBlob(g, 58, 48, 54, new Color(230, 236, 228), 86);
        drawSoftBlob(g, 126, 42, 68, new Color(214, 226, 218), 74);
        drawSoftBlob(g, 174, 50, 48, new Color(241, 244, 238), 62);
        g.setComposite(AlphaComposite.SrcOver.derive(0.23f));
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(248, 250, 245));
        g.drawArc(24, 24, 110, 38, 182, -220);
        g.drawArc(88, 34, 100, 28, 178, -190);
        g.dispose();
        return image;
    }

    private BufferedImage createDustSprite() {
        BufferedImage image = new BufferedImage(80, 28, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = spriteGraphics(image);
        drawSoftBlob(g, 18, 14, 20, new Color(218, 171, 104), 92);
        drawSoftBlob(g, 40, 13, 26, new Color(238, 194, 124), 118);
        drawSoftBlob(g, 62, 15, 18, new Color(195, 137, 83), 76);
        g.dispose();
        return image;
    }

    private BufferedImage createSandVeilSprite() {
        BufferedImage image = new BufferedImage(256, 192, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = spriteGraphics(image);
        g.setPaint(new GradientPaint(0, 0, new Color(207, 150, 77, 34), 256, 192, new Color(242, 199, 122, 70)));
        g.fillRect(0, 0, 256, 192);
        g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 58; i++) {
            int seed = Math.abs(i * 71143 + 5021);
            int x = Math.floorMod(seed, 310) - 28;
            int y = Math.floorMod(seed / 37, 220) - 14;
            int len = 28 + Math.floorMod(seed / 61, 76);
            int lift = 4 + Math.floorMod(seed / 89, 20);
            int alpha = 26 + Math.floorMod(seed / 19, 54);
            g.setColor(new Color(255, 223, 154, alpha));
            g.drawLine(x, y, x + len, y - lift);
        }
        for (int i = 0; i < 80; i++) {
            int seed = Math.abs(i * 19073 + 877);
            int x = Math.floorMod(seed, 256);
            int y = Math.floorMod(seed / 41, 192);
            int size = 1 + Math.floorMod(seed / 97, 3);
            g.setColor(new Color(116, 78, 42, 28 + Math.floorMod(seed / 23, 36)));
            g.fillOval(x, y, size, size);
        }
        g.dispose();
        return image;
    }

    private BufferedImage createSandStreakSprite() {
        BufferedImage image = new BufferedImage(180, 28, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = spriteGraphics(image);
        g.setStroke(new BasicStroke(5.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setPaint(new GradientPaint(10, 14, new Color(255, 229, 165, 0), 132, 14, new Color(255, 222, 139, 150)));
        g.drawLine(6, 16, 160, 10);
        g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setPaint(new GradientPaint(28, 21, new Color(134, 85, 43, 0), 178, 10, new Color(126, 78, 37, 88)));
        g.drawLine(24, 21, 178, 12);
        g.dispose();
        return image;
    }

    private BufferedImage createSandStreakFieldSprite() {
        BufferedImage image = new BufferedImage(512, 384, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = spriteGraphics(image);
        BufferedImage streak = createSandStreakSprite();
        for (int i = 0; i < 150; i++) {
            int seed = Math.abs(i * 97561 + 1237);
            boolean foreground = Math.floorMod(seed / 19, 5) == 0;
            int x = Math.floorMod(seed, 672) - 80;
            int y = Math.floorMod(seed / 31, 454) - 35;
            int drawW = (foreground ? 88 : 46) + Math.floorMod(seed / 11, foreground ? 88 : 46);
            int drawH = (foreground ? 8 : 4) + Math.floorMod(seed / 83, foreground ? 9 : 5);
            float alpha = foreground ? 0.42f : 0.25f;
            drawParticleSprite(g, streak, x, y, drawW, drawH, -0.28, alpha);
        }
        g.dispose();
        return image;
    }

    private BufferedImage createSandMoteFieldSprite() {
        BufferedImage image = new BufferedImage(512, 384, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = spriteGraphics(image);
        BufferedImage mote = createDustSprite();
        for (int i = 0; i < 130; i++) {
            int seed = Math.abs(i * 86311 + 4201);
            int x = Math.floorMod(seed, 622) - 55;
            int y = Math.floorMod(seed / 41, 424) - 20;
            int drawW = 14 + Math.floorMod(seed / 23, 34);
            int drawH = 4 + Math.floorMod(seed / 97, 9);
            drawParticleSprite(g, mote, x, y, drawW, drawH, -0.12,
                    0.24f + Math.floorMod(seed / 31, 16) / 100.0f);
        }
        g.dispose();
        return image;
    }

    private BufferedImage createHeatHazeSprite() {
        BufferedImage image = new BufferedImage(384, 42, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = spriteGraphics(image);
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int band = 0; band < 3; band++) {
            g.setComposite(AlphaComposite.SrcOver.derive(0.12f - band * 0.025f));
            g.setColor(new Color(255, 238, 176));
            int y = 10 + band * 10;
            for (int x = -20; x < 384; x += 34) {
                g.drawArc(x, y, 52, 16, 12, 156);
            }
        }
        g.dispose();
        return image;
    }

    private BufferedImage createStarSprite() {
        BufferedImage image = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = spriteGraphics(image);
        drawSoftBlob(g, 12, 12, 11, new Color(252, 245, 202), 150);
        g.setComposite(AlphaComposite.SrcOver.derive(0.65f));
        g.setStroke(new BasicStroke(1.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(255, 255, 238));
        g.drawLine(12, 3, 12, 21);
        g.drawLine(3, 12, 21, 12);
        g.dispose();
        return image;
    }

    private Graphics2D spriteGraphics(BufferedImage image) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return g;
    }

    private void drawSoftBlob(Graphics2D g, int cx, int cy, int radius, Color color, int alpha) {
        Paint oldPaint = g.getPaint();
        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver);
        g.setPaint(new RadialGradientPaint(
                cx,
                cy,
                Math.max(1, radius),
                new float[]{0.0f, 0.42f, 1.0f},
                new Color[]{
                        new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha),
                        new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, alpha / 3)),
                        new Color(color.getRed(), color.getGreen(), color.getBlue(), 0)
                }
        ));
        g.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
        g.setPaint(oldPaint);
        g.setComposite(oldComposite);
    }

    private String terrainImageName(char tile, int wx, int wy) {
        if (tile == 'A') {
            return "field_farmland_tilled_dense";
        }
        String base = Terrain.assetName(tile);
        int count = switch (base) {
            case "grass", "forest", "tundra" -> 8;
            case "water", "road", "desert", "marsh", "badlands", "mountain", "mountain_massif_tile" -> 4;
            case "beach", "submerged_sand" -> 2;
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
        syncStatusLog();
        int left = gameAreaWidth();
        g.setColor(new Color(18, 20, 29, 218));
        g.fillRect(left, 0, GameConfig.SIDEBAR_WIDTH, viewHeight());
        g.setColor(new Color(104, 112, 140, 148));
        g.drawLine(left, 0, left, viewHeight());
        if (state.mode == GameMode.VILLAGE) {
            drawVillageSidebar(g, left);
            return;
        }

        Actor p = state.player;
        int x = left + 28;
        int y = 42;
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.setColor(new Color(243, 238, 219));
        g.drawString(p.className, x, y);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(198, 202, 211));
        g.drawString("Gold " + p.gold, x, y + 28);

        boolean shortSidebar = viewHeight() < 850;
        int contentW = GameConfig.SIDEBAR_WIDTH - 56;
        int gap = shortSidebar ? 8 : 12;
        int buttonH = shortSidebar ? 26 : 30;
        int rowGap = shortSidebar ? 6 : 8;
        int logH = shortSidebar ? 100 : Math.max(132, Math.min(188, viewHeight() - 840));
        int logY = viewHeight() - logH - 24;
        int miniH = shortSidebar ? 104 : 138;
        int envH = shortSidebar ? 62 : 72;
        int posH = shortSidebar ? 44 : 54;
        int fullscreenH = shortSidebar ? 24 : 28;
        int actionRowsH = buttonH * 3 + rowGap * 2;
        int travelY = y + 46;
        int reservedBelowTravel = actionRowsH + miniH + envH + posH + fullscreenH + gap * 6;
        int travelMax = shortSidebar ? 206 : 236;
        int travelMin = Math.min(150, travelMax);
        int travelH = clamp(logY - travelY - reservedBelowTravel, travelMin, travelMax);
        drawTravelPartyHud(g, x, travelY, contentW, travelH);

        int actionY = travelY + travelH + gap;
        int halfW = (contentW - 8) / 2;
        sidebarButton(g, x, actionY, halfW, buttonH, "Quest Log", state::toggleQuestLog);
        sidebarButton(g, x + halfW + 8, actionY, halfW, buttonH, "World Map", state::toggleWorldMap);
        sidebarButton(g, x, actionY + buttonH + rowGap, halfW, buttonH, "Craft", state::toggleCrafting);
        sidebarButton(g, x + halfW + 8, actionY + buttonH + rowGap, halfW, buttonH, "Character (" + characterPointBadge() + ")", state::toggleParty);
        sidebarButton(g, x, actionY + (buttonH + rowGap) * 2, halfW, buttonH, "Inventory", state::toggleInventory);
        sidebarButton(g, x + halfW + 8, actionY + (buttonH + rowGap) * 2, halfW, buttonH, "Village", state::toggleVillage);

        int miniY = actionY + actionRowsH + gap;
        drawMiniMap(g, x, miniY, contentW, miniH);

        int envY = miniY + miniH + gap + (shortSidebar ? 0 : 2);
        g.setFont(new Font("SansSerif", Font.BOLD, shortSidebar ? 14 : 16));
        g.setColor(new Color(230, 225, 206));
        g.drawString("Environment", x, envY + 14);
        g.setFont(new Font("SansSerif", Font.PLAIN, shortSidebar ? 12 : 14));
        g.setColor(new Color(198, 202, 211));
        int lineH = shortSidebar ? 16 : 19;
        drawClippedString(g, "Day " + state.dayNumber() + "  " + state.timeLabel() + "  " + state.dayPhaseLabel(), x, envY + 14 + lineH, contentW);
        drawClippedString(g, "Weather: " + state.weatherLabel() + " over " + Terrain.name(state.currentBiomeTile()), x, envY + 14 + lineH * 2, contentW);
        drawClippedString(g, state.windLabel(), x, envY + 14 + lineH * 3, contentW);

        int posY = envY + envH;
        g.setFont(new Font("SansSerif", Font.BOLD, shortSidebar ? 14 : 16));
        g.setColor(new Color(230, 225, 206));
        g.drawString("Position", x, posY + 14);
        g.setFont(new Font("SansSerif", Font.PLAIN, shortSidebar ? 12 : 14));
        g.setColor(new Color(198, 202, 211));
        drawClippedString(g, state.world.label(state.currentMapId), x, posY + 14 + lineH, contentW);
        drawClippedString(g, state.playerX + ", " + state.playerY + "  " + Terrain.name(state.world.tileAt(state.currentMapId, state.playerX, state.playerY)), x, posY + 14 + lineH * 2, contentW);

        int fullscreenY = posY + posH + gap;
        if (fullscreenY + fullscreenH < logY - gap) {
            sidebarButton(g, x + halfW + 8, fullscreenY, halfW, fullscreenH, "Fullscreen", fullscreenToggle);
        }

        drawStatusLog(g, left + 22, logY, GameConfig.SIDEBAR_WIDTH - 44, logH);
        if (state.crafting.active()) {
            int progressX = left + 34;
            int progressY = logY + logH - 22;
            int progressW = GameConfig.SIDEBAR_WIDTH - 56;
            g.setColor(new Color(35, 39, 54));
            g.fillRoundRect(progressX, progressY, progressW, 12, 8, 8);
            g.setColor(new Color(185, 135, 76));
            g.fillRoundRect(progressX, progressY, (int) Math.round(progressW * state.crafting.progress()), 12, 8, 8);
            g.setColor(new Color(231, 222, 196));
            g.drawRoundRect(progressX, progressY, progressW, 12, 8, 8);
        }
    }

    private int characterPointBadge() {
        int total = 0;
        for (Actor actor : state.partyMembers()) {
            total += actor.skillPoints + actor.statPoints + actor.professionSkillPoints;
        }
        return total;
    }

    private void sidebarButton(Graphics2D g, int x, int y, int w, int h, String label, Runnable action) {
        actionButton(g, x, y, w, h, label, action, new Color(35, 39, 54), new Color(86, 98, 128), true);
    }

    private void drawStatusLog(Graphics2D g, int x, int y, int w, int h) {
        statusLogBounds = new Rectangle(x, y, w, h);
        g.setColor(new Color(11, 13, 20, 224));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(74, 82, 105));
        g.drawRoundRect(x, y, w, h, 8, 8);

        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(new Color(230, 225, 206));
        g.drawString("Log", x + 12, y + 22);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(145, 154, 174));
        g.drawString(statusLog.size() + "/" + MAX_STATUS_LOG_ENTRIES, x + w - 54, y + 22);

        int contentX = x + 12;
        int contentY = y + 45;
        int contentW = w - 34;
        int lineHeight = 16;
        int contentBottomPadding = state.crafting.active() ? 28 : 12;
        int visibleLines = Math.max(1, (h - 38 - contentBottomPadding) / lineHeight);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        FontMetrics metrics = g.getFontMetrics();
        List<String> lines = new ArrayList<>();
        for (String message : statusLog) {
            lines.addAll(wrappedTooltipLines(metrics, message, contentW));
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        int maxScroll = Math.max(0, lines.size() - visibleLines);
        statusLogScroll = clamp(statusLogScroll, 0, maxScroll);
        int start = Math.max(0, lines.size() - visibleLines - statusLogScroll);
        int end = Math.min(lines.size(), start + visibleLines);

        Shape oldClip = g.getClip();
        g.setClip(new Rectangle(contentX, y + 32, contentW, h - 42 - contentBottomPadding));
        for (int i = start; i < end; i++) {
            drawTooltipLine(g, lines.get(i), contentX, contentY + (i - start) * lineHeight, contentW);
        }
        g.setClip(oldClip);

        if (maxScroll > 0) {
            int trackX = x + w - 14;
            int trackY = y + 34;
            int trackH = h - 48 - contentBottomPadding;
            int thumbH = Math.max(18, trackH * visibleLines / Math.max(visibleLines + maxScroll, 1));
            int thumbTravel = Math.max(1, trackH - thumbH);
            int thumbY = trackY + (int) Math.round((maxScroll - statusLogScroll) / (double) maxScroll * thumbTravel);
            g.setColor(new Color(37, 42, 58));
            g.fillRoundRect(trackX, trackY, 4, trackH, 4, 4);
            g.setColor(new Color(126, 141, 184));
            g.fillRoundRect(trackX, thumbY, 4, thumbH, 4, 4);
        }
    }

    private boolean drawsGameplayUiToggle() {
        return switch (state.mode) {
            case MAIN_MENU, CLASS_SELECT, STORY_INTRO -> false;
            default -> true;
        };
    }

    private void drawUiVisibilityToggle(Graphics2D g) {
        int size = 34;
        int x = viewWidth() - size - 18;
        int y = viewHeight() - size - 18;
        Rectangle bounds = new Rectangle(x, y, size, size);
        buttons.add(new UiButton(bounds, "ui-visibility", () -> {
            uiHidden = !uiHidden;
            repaint();
        }));
        tooltipZones.add(new TooltipZone(bounds, uiHidden ? "Show interface" : "Hide interface",
                uiHidden ? "Restores the dialogue and side panels." : "Hides the interface so the scene can be viewed unobstructed."));

        boolean hovered = hoverPoint != null && bounds.contains(hoverPoint);
        Composite oldComposite = g.getComposite();
        Stroke oldStroke = g.getStroke();
        g.setComposite(AlphaComposite.SrcOver.derive(hovered ? 0.84f : 0.58f));
        g.setColor(new Color(10, 12, 18, 210));
        g.fillRoundRect(x, y, size, size, 10, 10);
        g.setColor(hovered ? new Color(224, 232, 246) : new Color(170, 181, 205));
        g.setStroke(new BasicStroke(Math.max(1.2f, scaledStroke(1.4f))));
        int cx = x + size / 2;
        int cy = y + size / 2;
        g.drawOval(cx - 11, cy - 7, 22, 14);
        g.fillOval(cx - 3, cy - 3, 6, 6);
        if (uiHidden) {
            g.drawLine(cx - 13, cy + 11, cx + 13, cy - 11);
        }
        g.setStroke(oldStroke);
        g.setComposite(oldComposite);
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
        Rectangle bounds = new Rectangle(x, y, w, h);
        if (enabled) {
            buttons.add(new UiButton(bounds, label, () -> {
                action.run();
                repaint();
            }));
        }
        boolean hovered = enabled && hoverPoint != null && bounds.contains(hoverPoint);
        boolean pressed = hovered && pressedButtonBounds != null && pressedButtonBounds.equals(bounds)
                && label.equals(pressedButtonLabel);
        Color buttonFill = enabled ? fill : new Color(42, 44, 52);
        Color buttonBorder = enabled ? border : new Color(73, 75, 84);
        if (hovered) {
            buttonFill = blend(buttonFill, Color.WHITE, 0.12);
            buttonBorder = blend(buttonBorder, Color.WHITE, 0.20);
        }
        if (pressed) {
            buttonFill = blend(buttonFill, Color.BLACK, 0.16);
            buttonBorder = blend(buttonBorder, Color.WHITE, 0.34);
        }
        if (pressed) {
            g.setColor(new Color(0, 0, 0, 120));
            g.fillRoundRect(x + 1, y + 2, w, h, 6, 6);
        }
        g.setColor(buttonFill);
        g.fillRoundRect(x, y + (pressed ? 1 : 0), w, h, 6, 6);
        g.setColor(buttonBorder);
        g.drawRoundRect(x, y, w, h, 6, 6);
        g.setFont(new Font("SansSerif", Font.BOLD, h >= 34 ? 13 : 12));
        g.setColor(enabled ? new Color(238, 239, 244) : new Color(142, 146, 156));
        FontMetrics metrics = g.getFontMetrics();
        String display = label;
        while (metrics.stringWidth(display) > w - 12 && display.length() > 4) {
            display = display.substring(0, display.length() - 4) + "...";
        }
        g.drawString(display, x + (w - metrics.stringWidth(display)) / 2, y + h / 2 + 5 + (pressed ? 1 : 0));
    }

    private void drawHoverTooltip(Graphics2D g) {
        if (hoverPoint == null || tooltipZones.isEmpty()) {
            return;
        }
        TooltipZone zone = null;
        for (int i = tooltipZones.size() - 1; i >= 0; i--) {
            TooltipZone candidate = tooltipZones.get(i);
            if (candidate.bounds().contains(hoverPoint)) {
                zone = candidate;
                break;
            }
        }
        if (zone == null) {
            return;
        }
        boolean portraitIcon = zone.icon() != null && zone.icon().startsWith("sprite:");
        int width = portraitIcon ? 500 : 430;
        int iconBox = portraitIcon ? 72 : 46;
        int contentX = zone.icon() == null ? 14 : iconBox + 26;
        int bodyX = xOffsetForTooltipBody(zone, iconBox);
        int bodyWidth = width - bodyX - 14;
        int headerHeight = zone.icon() == null ? 42 : portraitIcon ? 104 : 66;
        Graphics2D measure = (Graphics2D) g.create();
        measure.setFont(new Font("SansSerif", Font.PLAIN, 13));
        int bodyLines = tooltipLineCount(measure, zone.body(), bodyWidth);
        measure.dispose();
        int minHeight = zone.icon() == null ? 126 : portraitIcon ? 174 : 154;
        int height = Math.min(viewHeight() - 24, Math.max(minHeight, headerHeight + bodyLines * 17 + 18));
        int x = Math.min(viewWidth() - width - 18, hoverPoint.x + 18);
        int y = Math.min(viewHeight() - height - 18, hoverPoint.y + 18);
        if (x < 12) {
            x = 12;
        }
        if (y < 12) {
            y = 12;
        }
        Graphics2D tip = (Graphics2D) g.create();
        tip.setColor(new Color(8, 11, 18, 238));
        tip.fillRoundRect(x, y, width, height, 8, 8);
        Color accent = zone.accent() == null ? new Color(125, 136, 172) : zone.accent();
        tip.setColor(accent);
        tip.drawRoundRect(x, y, width, height, 8, 8);
        int titleX = x + contentX;
        if (zone.icon() != null) {
            tip.setColor(new Color(18, 23, 34, 235));
            tip.fillRoundRect(x + 14, y + 14, iconBox, iconBox, 7, 7);
            tip.setColor(new Color(89, 101, 136));
            tip.drawRoundRect(x + 14, y + 14, iconBox, iconBox, 7, 7);
            if (portraitIcon) {
                String sprite = zone.icon().substring("sprite:".length());
                tip.drawImage(assets.spriteFit(sprite, iconBox - 8, iconBox - 8), x + 18, y + 18, null);
            } else {
                tip.drawImage(assets.effectSprite(zone.icon(), 38, 38, frame / 4), x + 18, y + 18, null);
            }
            titleX = x + contentX;
        }
        int titleFontSize = portraitIcon && zone.accent() != null
                ? Math.max(15, Math.min(22, settlementHoverNameFontSize() + 2))
                : 15;
        tip.setFont(new Font("SansSerif", Font.BOLD, titleFontSize));
        tip.setColor(zone.accent() == null ? new Color(246, 224, 151) : accent);
        tip.drawString(zone.title(), titleX, y + 26);
        tip.setFont(new Font("SansSerif", Font.PLAIN, 13));
        int bodyY = y + (zone.icon() == null ? 50 : portraitIcon ? 48 : 76);
        int maxLines = Math.max(1, (height - (bodyY - y) - 14) / 17);
        drawTooltipBody(tip, zone.body(), x + bodyX, bodyY, bodyWidth, 17, maxLines);
        tip.dispose();
    }

    private void drawDialogueVideoOverlay(Graphics2D g) {
        GameState.DialogueVideoPrompt prompt = state.activeDialogueVideo();
        if (prompt == null) {
            return;
        }
        int width = Math.min(720, Math.max(420, gameAreaWidth() - 160));
        int height = 280;
        int x = gameAreaCenteredX(width);
        int y = Math.max(80, (viewHeight() - height) / 2);
        g.setColor(new Color(0, 0, 0, 168));
        g.fillRect(0, 0, viewWidth(), viewHeight());
        g.setColor(new Color(8, 11, 18, 244));
        g.fillRoundRect(x, y, width, height, 8, 8);
        g.setColor(new Color(145, 166, 214, 180));
        g.drawRoundRect(x, y, width, height, 8, 8);

        int frameX = x + 28;
        int frameY = y + 28;
        int frameW = 168;
        int frameH = height - 56;
        g.setColor(new Color(16, 22, 34, 238));
        g.fillRoundRect(frameX, frameY, frameW, frameH, 8, 8);
        g.setColor(new Color(91, 107, 148, 190));
        g.drawRoundRect(frameX, frameY, frameW, frameH, 8, 8);
        g.setFont(new Font("SansSerif", Font.BOLD, 42));
        g.setColor(new Color(238, 239, 244));
        FontMetrics playMetrics = g.getFontMetrics();
        String play = "PLAY";
        g.drawString(play, frameX + (frameW - playMetrics.stringWidth(play)) / 2, frameY + frameH / 2 + 14);

        int textX = frameX + frameW + 28;
        int textW = width - (textX - x) - 28;
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.setColor(new Color(246, 240, 220));
        drawClippedString(g, prompt.title(), textX, y + 54, textW);
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g.setColor(new Color(219, 224, 236));
        drawWrapped(g, prompt.caption(), textX, y + 88, textW, 22, 4);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(145, 153, 174));
        drawClippedString(g, prompt.assetPath(), textX, y + height - 58, textW);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(194, 203, 226));
        drawClippedString(g, "Enter, E, Space, Escape, or click to continue", textX, y + height - 28, textW);
    }

    private int xOffsetForTooltipBody(TooltipZone zone, int iconBox) {
        if (zone.icon() == null) {
            return 14;
        }
        if (zone.icon().startsWith("sprite:")) {
            return iconBox + 26;
        }
        return 14;
    }

    private int tooltipLineCount(Graphics2D g, String text, int width) {
        FontMetrics metrics = g.getFontMetrics();
        int lines = 0;
        for (String paragraph : text.split("\\R", -1)) {
            if (paragraph.isBlank()) {
                lines++;
            } else {
                lines += Math.max(1, wrappedTooltipLines(metrics, paragraph, width).size());
            }
        }
        return Math.max(1, lines);
    }

    private void drawTooltipBody(Graphics2D g, String text, int x, int y, int width, int lineHeight, int maxLines) {
        FontMetrics metrics = g.getFontMetrics();
        List<String> displayLines = new ArrayList<>();
        for (String paragraph : text.split("\\R", -1)) {
            if (paragraph.isBlank()) {
                displayLines.add("");
                continue;
            }
            displayLines.addAll(wrappedTooltipLines(metrics, paragraph, width));
        }
        int count = Math.min(displayLines.size(), maxLines);
        for (int i = 0; i < count; i++) {
            String line = displayLines.get(i);
            if (i == count - 1 && displayLines.size() > count) {
                line = fitWithEllipsis(metrics, line, width);
            }
            drawTooltipLine(g, line, x, y + i * lineHeight, width);
        }
    }

    private List<String> wrappedTooltipLines(FontMetrics metrics, String text, int width) {
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
        return lines;
    }

    private void drawTooltipLine(Graphics2D g, String line, int x, int y, int width) {
        FontMetrics metrics = g.getFontMetrics();
        int cursor = x;
        for (String token : line.split("(?<=\\s)|(?=\\s)")) {
            if (token.isEmpty()) {
                continue;
            }
            g.setColor(statusColorForToken(token, new Color(220, 224, 232)));
            g.drawString(token, cursor, y);
            cursor += metrics.stringWidth(token);
            if (cursor > x + width) {
                return;
            }
        }
    }

    private Color statusColorForToken(String token, Color fallback) {
        String label = token.replaceAll("[^A-Za-z]", "").toLowerCase();
        Color labelColor = switch (label) {
            case "common" -> rarityColor(ItemRarity.COMMON);
            case "uncommon" -> rarityColor(ItemRarity.UNCOMMON);
            case "rare" -> rarityColor(ItemRarity.RARE);
            case "unique" -> rarityColor(ItemRarity.UNIQUE);
            case "legendary" -> rarityColor(ItemRarity.LEGENDARY);
            case "type", "stats", "requirements", "description", "effect", "stack", "equip", "use", "utility", "recovery", "crafting" -> new Color(246, 224, 151);
            case "weapon", "armor", "accessory", "consumable", "material", "property", "affix" -> new Color(194, 203, 226);
            case "atk", "str" -> new Color(255, 190, 124);
            case "def", "con", "wil" -> new Color(176, 205, 222);
            case "int", "spell" -> new Color(194, 169, 255);
            case "dex", "cha" -> new Color(255, 226, 116);
            case "target" -> new Color(134, 196, 255);
            case "strike", "damage", "guard" -> new Color(255, 190, 124);
            case "restores", "healing", "heal", "hp" -> new Color(126, 232, 154);
            case "mp", "cost", "scaling", "power", "attributes", "level" -> new Color(194, 169, 255);
            case "status", "ward", "crit" -> new Color(255, 226, 116);
            case "raw", "defense", "defenses" -> new Color(176, 205, 222);
            default -> null;
        };
        if (labelColor != null) {
            return labelColor;
        }
        String key = statusKeyForToken(token);
        if (key == null) {
            return fallback;
        }
        if (GameData.DAMAGE_TYPES.contains(key)) {
            return switch (key) {
                case "physical" -> new Color(232, 218, 190);
                case "poison" -> new Color(128, 222, 118);
                case "acid" -> new Color(177, 232, 87);
                case "fire" -> new Color(255, 151, 86);
                case "lightning" -> new Color(255, 235, 113);
                case "ice" -> new Color(137, 211, 255);
                case "water" -> new Color(100, 190, 255);
                case "arcane" -> new Color(191, 142, 255);
                case "dark" -> new Color(171, 132, 220);
                case "chaos" -> new Color(255, 130, 205);
                case "holy" -> new Color(255, 236, 160);
                case "nature" -> new Color(112, 233, 164);
                default -> fallback;
            };
        }
        return switch (key) {
            case "burn" -> new Color(255, 151, 86);
            case "poison" -> new Color(128, 222, 118);
            case "weak" -> new Color(137, 211, 255);
            case "vulnerable" -> new Color(255, 122, 134);
            case "shield" -> new Color(130, 183, 255);
            case "fortified" -> new Color(184, 207, 161);
            case "haste" -> new Color(255, 220, 110);
            case "regeneration" -> new Color(112, 233, 164);
            default -> fallback;
        };
    }

    private String statusKeyForToken(String token) {
        String normalized = token.replaceAll("[^A-Za-z]", "").toLowerCase();
        if (normalized.isBlank()) {
            return null;
        }
        String damageType = GameData.normalizeDamageType(normalized);
        if (GameData.DAMAGE_TYPES.contains(damageType)) {
            return damageType;
        }
        if (GameData.STATUS_EFFECTS.containsKey(normalized) || StatusEffects.ALL.containsKey(normalized)) {
            return normalized;
        }
        for (StatusEffect effect : GameData.STATUS_EFFECTS.values()) {
            if (effect.name().replaceAll("[^A-Za-z]", "").equalsIgnoreCase(normalized)) {
                return effect.key();
            }
        }
        for (StatusEffect effect : StatusEffects.ALL.values()) {
            if (effect.name().replaceAll("[^A-Za-z]", "").equalsIgnoreCase(normalized)) {
                return effect.key();
            }
        }
        return null;
    }

    private String attackTooltip(Actor actor, Actor target) {
        int baseAttack = actor.attack + actor.strength / 2 + actor.dexterity / 4;
        int min = Math.max(1, baseAttack - 2);
        int max = baseAttack + 4;
        String targetText = target == null ? "the selected enemy" : target.name + " (DEF " + target.defense + ")";
        int estimatedMin = target == null ? min : Math.max(1, min - target.defense);
        int estimatedMax = target == null ? max : Math.max(1, max - target.defense);
        int crit = (int) Math.round(actor.criticalChanceAgainst(target, null) * 100.0);
        double typeMultiplier = target == null || state.battle == null || !state.battle.enemies().contains(target)
                ? 1.0
                : GameData.monsterDamageMultiplier(state.battle.monsterSpecFor(target), List.of("physical"));
        double knowledgeMultiplier = target == null || state.battle == null || !state.battle.enemies().contains(target)
                ? 1.0
                : GameData.monsterKnowledgeDamageMultiplier(
                        state.monsterInsightCount(state.battle.monsterSpecFor(target).key()),
                        state.player.intelligence
                );
        if (typeMultiplier != 1.0) {
            estimatedMin = Math.max(1, (int) Math.round(estimatedMin * typeMultiplier));
            estimatedMax = Math.max(1, (int) Math.round(estimatedMax * typeMultiplier));
        }
        if (knowledgeMultiplier != 1.0) {
            estimatedMin = Math.max(1, (int) Math.round(estimatedMin * knowledgeMultiplier));
            estimatedMax = Math.max(1, (int) Math.round(estimatedMax * knowledgeMultiplier));
        }
        return "Basic attack uses ATK + STR/2 + DEX/4, then rolls -2 to +4: " + min + "-" + max
                + " raw. Against " + targetText + ", current estimate is " + estimatedMin + "-" + estimatedMax
                + " before status modifiers. Damage: Physical. Crit chance about " + crit
                + "%; targets may dodge from DEX or parry from STR, DEX, and CON.";
    }

    private String abilityTooltip(Actor actor, Ability ability, Battle battle) {
        Actor enemy = battle.selectedEnemy();
        Actor ally = battle.hasExplicitPartyTarget() ? battle.selectedPartyMember() : null;
        int spellcraft = actor == state.player ? state.player.skillRank("spellcraft") * 2 + state.player.skillRank("overchannel") * 3 : 0;
        int volley = actor == state.player && "all_enemies".equals(ability.target()) ? state.player.skillRank("volley_mastery") * 2 : 0;
        int channeling = actor == state.player ? state.player.skillRank("channeling") * 4 : 0;
        List<AbilityStatus> statuses = GameData.statusHintsForAbility(ability);
        return switch (ability.kind()) {
            case DAMAGE -> {
                int attributeBonus = actor.abilityDamageBonus(ability);
                int base = ability.power() + attributeBonus + spellcraft + volley;
                int rawMin = base;
                int rawMax = base + 5;
                List<String> damageTypes = GameData.damageTypesForAbility(ability);
                double typeMultiplier = enemy == null ? 1.0 : GameData.monsterDamageMultiplier(battle.monsterSpecFor(enemy), damageTypes);
                double knowledgeMultiplier = enemy == null ? 1.0 : GameData.monsterKnowledgeDamageMultiplier(
                        state.monsterInsightCount(battle.monsterSpecFor(enemy).key()),
                        state.player.intelligence
                );
                int typedMin = Math.max(1, (int) Math.round(rawMin * typeMultiplier * knowledgeMultiplier));
                int typedMax = Math.max(1, (int) Math.round(rawMax * typeMultiplier * knowledgeMultiplier));
                int shownMin = enemy == null ? typedMin : Math.max(1, typedMin - enemy.defense);
                int shownMax = enemy == null ? typedMax : Math.max(1, typedMax - enemy.defense);
                String targetText = "all_enemies".equals(ability.target()) ? "each enemy" : enemy == null ? "the chosen foe" : enemy.name + " (DEF " + enemy.defense + ")";
                yield abilityFlavor(ability) + "\n"
                        + "Intent: Damage art, " + abilityMood(ability) + ".\n"
                        + "Target: " + targetText + ".\n"
                        + "Strike: " + shownMin + "-" + shownMax + " expected damage"
                        + (shownMin == rawMin && shownMax == rawMax ? "." : " after type and defenses; raw " + rawMin + "-" + rawMax + ".")
                        + "\n"
                        + "Damage: " + GameData.damageTypeListLabel(damageTypes) + ". Scaling: " + scalingLabel(ability)
                        + ", power " + ability.power() + ", attributes +" + attributeBonus
                        + (spellcraft > 0 ? ", spellcraft +" + spellcraft : "")
                        + (volley > 0 ? ", volley +" + volley : "") + "."
                        + cooldownText(ability, battle.abilityCooldownRemaining(actor, ability))
                        + " Crit, dodge, and parry may still turn the edge."
                        + abilityStatusText(statuses)
                        + abilityMechanicsText(ability);
            }
            case HEAL -> {
                int healBonus = actor.abilityScalingBonus(ability);
                int base = ability.power() + actor.level + healBonus + channeling;
                String targetText = "party".equals(ability.target()) ? "the whole party" : "self".equals(ability.target()) ? actor.name : ally == null ? "choose an ally first" : ally.name;
                yield abilityFlavor(ability) + "\n"
                        + "Intent: Restorative art, " + abilityMood(ability) + ".\n"
                        + "Target: " + targetText + ".\n"
                        + "Restores: " + Math.max(1, base - 2) + "-" + (base + 2) + " HP.\n"
                        + "Scaling: " + scalingLabel(ability) + ", power " + ability.power() + ", level +" + actor.level
                        + ", attributes +" + healBonus + (channeling > 0 ? ", channeling +" + channeling : "") + "."
                        + cooldownText(ability, battle.abilityCooldownRemaining(actor, ability))
                        + abilityStatusText(statuses)
                        + abilityMechanicsText(ability);
            }
            case DEFEND -> abilityFlavor(ability) + "\n"
                    + "Intent: Defensive art, " + abilityMood(ability) + ".\n"
                    + "Guard: restores MP and blunts the next incoming strike.\n"
                    + "Scaling: " + scalingLabel(ability) + "."
                    + cooldownText(ability, battle.abilityCooldownRemaining(actor, ability))
                    + "\nWard: Shield and Fortified gather while the stance holds."
                    + abilityStatusText(statuses)
                    + abilityMechanicsText(ability);
        };
    }

    private String cooldownText(Ability ability, int remaining) {
        if (ability.cooldown() <= 0) {
            return "";
        }
        return " Cooldown: " + (remaining > 0 ? remaining + " turn(s) remaining." : ability.cooldown() + " turn(s).");
    }

    private String scalingLabel(Ability ability) {
        return switch (ability.scaling()) {
            case WEAPON -> "weapon STR/DEX/WIL";
            case AGILITY -> "agility DEX/ATK/STR";
            case ARCANE -> "arcane INT/WIL/DEX";
            case DIVINE -> "divine WIL/CHA/INT";
            case NATURE -> "nature WIL/CHA/INT/CON";
            case GUARD -> "guard DEF/CON/WIL";
            case TRIAGE -> "triage WIL/CHA/INT/DEX";
        };
    }

    private String abilityStatusText(List<AbilityStatus> statuses) {
        if (statuses.isEmpty()) {
            return "";
        }
        return "\nStatus: " + String.join(", ", statuses.stream().map(this::abilityStatusLabel).toList()) + ".";
    }

    private String abilityMechanicsText(Ability ability) {
        List<String> mechanics = new ArrayList<>();
        if (ability.hasTag("execute")) {
            mechanics.add("finishes badly wounded enemies");
        }
        if (ability.hasTag("consume_vulnerable")) {
            mechanics.add("consumes Vulnerable for a larger hit");
        }
        if (ability.hasTag("armor_breaker")) {
            mechanics.add("punches through defense");
        }
        if (ability.hasTag("poison_combo")) {
            mechanics.add("hits poisoned foes harder");
        }
        if (ability.hasTag("cleanse")) {
            mechanics.add("cleanses harmful conditions");
        }
        if (ability.hasTag("overheal_shield")) {
            mechanics.add("turns excess healing into shields");
        }
        if (ability.hasTag("revive_party")) {
            mechanics.add("can revive downed allies");
        }
        if (ability.hasTag("party_guard")) {
            mechanics.add("extends guard to the party");
        }
        if (mechanics.isEmpty()) {
            return "";
        }
        return "\nMastery: " + String.join(", ", mechanics) + ".";
    }

    private String abilityStatusLabel(AbilityStatus status) {
        String chance = status.chance() >= 0.999 ? "" : " " + Math.round(status.chance() * 100) + "%";
        String target = switch (status.target()) {
            case "self" -> "self";
            case "target" -> "target";
            case "ally" -> "ally";
            default -> "enemy";
        };
        return statusName(status.key()) + chance + " to " + target;
    }

    private String abilityMood(Ability ability) {
        String effect = abilityEffectKind(ability);
        return switch (effect) {
            case "ember", "fire" -> "Fire pressure and Burn threat";
            case "frost" -> "Ice control and Weak pressure";
            case "water" -> "Water force and steady disruption";
            case "lightning" -> "Lightning chain tempo";
            case "holy", "radiant", "seraphic_hymn" -> "Holy light and exposed foes";
            case "poison" -> "Poison pressure over time";
            case "nature", "grove_hymn", "root_memory" -> "Nature growth and binding roots";
            case "dark", "execution_mark" -> "Dark misdirection and sharp openings";
            case "dust" -> "Smoke cover and evasive footing";
            case "arcane", "ley_detonation", "prismatic", "worldsplitter" -> "Arcane burst and high spectacle";
            case "shield", "heal" -> "Protective momentum";
            case "volley" -> "Piercing lines and ranged reach";
            case "cleave", "claw", "slash", "bash", "impact" -> "Physical force and decisive contact";
            default -> "reliable battle tempo";
        };
    }

    private String abilityFlavor(Ability ability) {
        String lowered = ability.name().toLowerCase();
        if (lowered.contains("worldfire")) {
            return "A sigil of many-colored flame opens under every foe, bright as a stolen dawn.";
        }
        if (lowered.contains("meteor") || lowered.contains("comet")) {
            return "Star-iron answers the caster's call and falls in a clean, terrible arc.";
        }
        if (lowered.contains("chain spark") || lowered.contains("lightning") || lowered.contains("storm")) {
            return "Lightning jumps from mark to mark, looking for the next exposed nerve.";
        }
        if (lowered.contains("water") || lowered.contains("tide") || lowered.contains("current") || lowered.contains("springwater")) {
            return "Cold water coils into a bright ribbon, breaking stance before it breaks bone.";
        }
        if (lowered.contains("frost") || lowered.contains("ice")) {
            return "Winter gathers along the edge of the spell, slowing breath and blood.";
        }
        if (lowered.contains("fire") || lowered.contains("ember") || lowered.contains("flare")) {
            return "A spark is coaxed into a hungry burst of battle-flame.";
        }
        if (lowered.contains("poison") || lowered.contains("venom")) {
            return "Dark tincture rides the strike, quiet and patient in the wound.";
        }
        if (lowered.contains("radiant") || lowered.contains("holy") || lowered.contains("sun")
                || lowered.contains("dawn") || lowered.contains("lantern") || lowered.contains("judgment")
                || lowered.contains("pilgrim")) {
            return "Gold-white light gathers at the hand, then names the enemy's weakness aloud.";
        }
        if (lowered.contains("thorn") || lowered.contains("briar") || lowered.contains("vine")
                || lowered.contains("root") || lowered.contains("grove") || lowered.contains("bloom")
                || lowered.contains("green") || lowered.contains("petal") || lowered.contains("canopy")) {
            return "Living growth answers the call, thorns and blossoms moving with one will.";
        }
        if (lowered.contains("stone") || lowered.contains("granite") || lowered.contains("quake")
                || lowered.contains("faultline") || lowered.contains("avalanche")) {
            return "The ground remembers its weight and lends it to the blow.";
        }
        if (lowered.contains("shield") || lowered.contains("ward") || lowered.contains("aegis") || lowered.contains("guard")) {
            return "Old warding words knit a brief shelter around the faithful.";
        }
        if (lowered.contains("mend") || lowered.contains("salve") || lowered.contains("blessing")
                || lowered.contains("hymn") || lowered.contains("chorus") || lowered.contains("refuge")) {
            return "Warm light settles into bruises and steadies the will to stand.";
        }
        if (lowered.contains("shadow") || lowered.contains("veil") || lowered.contains("night")
                || lowered.contains("eclipse") || lowered.contains("moon") || lowered.contains("fade")
                || lowered.contains("garrote") || lowered.contains("blindside")) {
            return "The air folds like dark cloth, hiding the hand until the moment of need.";
        }
        if (lowered.contains("smoke") || lowered.contains("vanish")) {
            return "Smoke blooms low and fast, turning a clear line of sight into guesswork.";
        }
        if (lowered.contains("arrow") || lowered.contains("shot") || lowered.contains("volley")) {
            return "A practiced shot follows the road between heartbeat and opening.";
        }
        if (lowered.contains("charge") || lowered.contains("rush") || lowered.contains("ram")
                || lowered.contains("slam") || lowered.contains("bash") || lowered.contains("hammer")) {
            return "A full-body surge turns discipline, shield, and momentum into one hard answer.";
        }
        if (lowered.contains("slash") || lowered.contains("cut") || lowered.contains("knife") || lowered.contains("fang")) {
            return "Steel writes a swift lesson across the enemy line.";
        }
        return switch (ability.kind()) {
            case DAMAGE -> "Power is shaped into a battle rite meant to break the enemy's line.";
            case HEAL -> "A restorative charm draws courage and flesh back together.";
            case DEFEND -> "The stance lowers the shoulder and raises an oath against harm.";
        };
    }

    private String abilityIconName(Ability ability) {
        return effectSpriteName(abilityEffectKind(ability));
    }

    private String abilityEffectKind(Ability ability) {
        return GameData.battleEffectForAbility(ability);
    }

    private String statusName(String key) {
        StatusEffect effect = GameData.STATUS_EFFECTS.get(key);
        if (effect == null) {
            effect = StatusEffects.ALL.get(key);
        }
        return effect == null ? key : effect.name();
    }

    private void zoomMiniMapIn() {
        setMiniMapZoom(minimapZoomIndex - 1);
    }

    private void zoomMiniMapOut() {
        setMiniMapZoom(minimapZoomIndex + 1);
    }

    private void setMiniMapZoom(int index) {
        minimapZoomIndex = clamp(index, 0, MINIMAP_COVERAGE.length - 1);
    }

    private int[] minimapCoverage() {
        return MINIMAP_COVERAGE[clamp(minimapZoomIndex, 0, MINIMAP_COVERAGE.length - 1)];
    }

    private void drawMiniMap(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(new Color(10, 13, 20));
        g.fillRoundRect(x, y, w, h, 6, 6);
        int[] coverage = minimapCoverage();
        int cols = coverage[0];
        int rows = coverage[1];
        int pad = 8;
        int mapX = x + pad;
        int mapY = y + 24;
        int cellW = Math.max(1, (w - pad * 2) / cols);
        int cellH = Math.max(1, (h - 32) / rows);
        int mapW = cellW * cols;
        int mapH = cellH * rows;
        int startX = state.playerX - cols / 2;
        int startY = state.playerY - rows / 2;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int wx = startX + col;
                int wy = startY + row;
                char tile = state.world.tileAt(state.currentMapId, wx, wy);
                g.setColor(minimapTileColor(tile, wx, wy));
                g.fillRect(mapX + col * cellW, mapY + row * cellH, cellW, cellH);
            }
        }
        g.setColor(new Color(255, 245, 174));
        int playerX = mapX + cols / 2 * cellW + cellW / 2;
        int playerY = mapY + rows / 2 * cellH + cellH / 2;
        int marker = Math.max(6, Math.min(cellW, cellH) + 2);
        g.fillOval(playerX - marker / 2, playerY - marker / 2, marker, marker);
        g.setColor(new Color(42, 49, 69));
        g.drawOval(playerX - marker / 2 - 2, playerY - marker / 2 - 2, marker + 4, marker + 4);
        for (GameState.QuestObjective objective : state.activeQuestObjectives()) {
            if (!state.currentMapId.equals(objective.mapId())) {
                continue;
            }
            int col = objective.x() - startX;
            int row = objective.y() - startY;
            if (col < 0 || row < 0 || col >= cols || row >= rows) {
                continue;
            }
            int ox = mapX + col * cellW + cellW / 2;
            int oy = mapY + row * cellH + cellH / 2;
            g.setColor(objectiveColor(objective.kind(), 255));
            g.fillRect(ox - 2, oy - 2, 5, 5);
        }
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(new Color(230, 225, 206));
        g.drawString("Vicinity", x + 10, y + 16);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(new Color(165, 174, 193));
        g.drawString(cols + "x" + rows, x + 72, y + 16);
        g.drawString(state.playerX + ", " + state.playerY, x + w - 116, y + 16);
        actionButton(g, x + w - 52, y + 3, 22, 18, "-", this::zoomMiniMapIn,
                new Color(35, 39, 54), new Color(86, 98, 128), minimapZoomIndex > 0);
        actionButton(g, x + w - 26, y + 3, 22, 18, "+", this::zoomMiniMapOut,
                new Color(35, 39, 54), new Color(86, 98, 128), minimapZoomIndex < MINIMAP_COVERAGE.length - 1);
        g.setColor(new Color(81, 88, 108));
        g.drawRoundRect(x, y, w, h, 6, 6);
        if (cellW >= 5 && cellH >= 5) {
            g.setColor(new Color(42, 48, 63, 120));
            for (int col = 1; col < cols; col++) {
                int gx = mapX + col * cellW;
                g.drawLine(gx, mapY, gx, mapY + mapH);
            }
            for (int row = 1; row < rows; row++) {
                int gy = mapY + row * cellH;
                g.drawLine(mapX, gy, mapX + mapW, gy);
            }
        }
    }

    private Color minimapTileColor(char tile, int wx, int wy) {
        Color color = Terrain.color(tile);
        int shade = Math.floorMod(wx * 31 + wy * 17, 18) - 9;
        return new Color(
                clamp(color.getRed() + shade, 0, 255),
                clamp(color.getGreen() + shade, 0, 255),
                clamp(color.getBlue() + shade, 0, 255)
        );
    }

    private void drawBattle(Graphics2D g) {
        Battle battle = state.battle;
        if (battle == null) {
            return;
        }
        int x = 0;
        int y = 0;
        int w = gameAreaWidth();
        int h = viewHeight();
        battleRenderer.drawBackdrop(g, battle, x, y, w, h);
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
            drawBattleEnemySprite(g, battle, foe, x, y, w, h);
        }
        for (Actor foe : battle.enemies()) {
            drawBattleEnemyCard(g, battle, foe, x, y, w, h);
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
            g.drawString(active.name + "'s turn", x + 42, y + h - 288);
        }

        drawBattleLog(g, battle, x + 34, y + h - 268, 620, 142);
        drawBattleActionBar(g, battle, x + 34, y + h - 156, w - 68, 132);
        if (battleVfxDebug) {
            drawBattleVfxDebugOverlay(g, battle, x + w - 368, y + 24, 334, 142);
        }
    }

    private void drawBattleVfxDebugOverlay(Graphics2D g, Battle battle, int x, int y, int w, int h) {
        BattleActionAnimation animation = battle.activeAnimation();
        Graphics2D debugG = (Graphics2D) g.create();
        debugG.setColor(new Color(8, 10, 16, 222));
        debugG.fillRoundRect(x, y, w, h, 8, 8);
        debugG.setColor(new Color(112, 128, 170, 210));
        debugG.drawRoundRect(x, y, w, h, 8, 8);
        debugG.setFont(new Font("SansSerif", Font.BOLD, 13));
        debugG.setColor(new Color(244, 213, 141));
        debugG.drawString("Battle VFX", x + 14, y + 22);
        debugG.setFont(new Font("Monospaced", Font.PLAIN, 12));
        debugG.setColor(new Color(214, 220, 235));
        if (animation == null) {
            debugG.drawString("animation: none", x + 14, y + 46);
            debugG.drawString("fallback effect: " + safeDebugText(battle.effectKind, 24), x + 14, y + 66);
            debugG.dispose();
            return;
        }
        String[] lines = {
                "effect: " + safeDebugText(animation.effectKind, 26),
                "mode:   " + animation.visualMode,
                "stage:  " + animation.stage() + "  frame " + animation.frame() + "/" + animation.totalFrames(),
                "steps:  " + animation.triggeredStepCount() + "/" + animation.targets.size(),
                "target: " + safeDebugText(animation.target == null ? "" : animation.target.name, 24)
        };
        for (int i = 0; i < lines.length; i++) {
            debugG.drawString(lines[i], x + 14, y + 46 + i * 18);
        }
        debugG.dispose();
    }

    private String safeDebugText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.length() <= maxLength ? value : value.substring(0, Math.max(1, maxLength - 1)) + ".";
    }

    private void drawBattleLog(Graphics2D g, Battle battle, int x, int y, int w, int h) {
        battleLogBounds = new Rectangle(x, y, w, h);
        g.setColor(new Color(12, 14, 22, 214));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(98, 108, 142));
        g.drawRoundRect(x, y, w, h, 8, 8);

        int contentX = x + 18;
        int contentY = y + 22;
        int contentW = w - 44;
        int lineHeight = 19;
        int visibleLines = Math.max(1, (h - 30) / lineHeight);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        FontMetrics metrics = g.getFontMetrics();
        List<String> lines = new ArrayList<>();
        for (String message : battle.log) {
            lines.addAll(wrappedTooltipLines(metrics, message, contentW));
        }
        int maxScroll = Math.max(0, lines.size() - visibleLines);
        battleLogScroll = Math.max(0, Math.min(battleLogScroll, maxScroll));
        int start = Math.max(0, lines.size() - visibleLines - battleLogScroll);
        int end = Math.min(lines.size(), start + visibleLines);

        Shape oldClip = g.getClip();
        g.setClip(new Rectangle(contentX, y + 12, contentW, h - 22));
        for (int i = start; i < end; i++) {
            drawTooltipLine(g, lines.get(i), contentX, contentY + (i - start) * lineHeight, contentW);
        }
        g.setClip(oldClip);

        if (maxScroll > 0) {
            int trackX = x + w - 18;
            int trackY = y + 18;
            int trackH = h - 36;
            int thumbH = Math.max(22, trackH * visibleLines / Math.max(visibleLines, lines.size()));
            int thumbTravel = Math.max(1, trackH - thumbH);
            int thumbY = trackY + (int) Math.round((maxScroll - battleLogScroll) / (double) maxScroll * thumbTravel);
            g.setColor(new Color(55, 61, 82, 190));
            g.fillRoundRect(trackX, trackY, 5, trackH, 4, 4);
            g.setColor(new Color(149, 164, 211, 210));
            g.fillRoundRect(trackX, thumbY, 5, thumbH, 4, 4);
        }
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
            g.drawString(battle.victory ? "Victory rewards are ready." : "Return to Oathstead Camp and recover.", x + 180, y + 51);
            return;
        }
        Actor active = battle.activeActor();
        boolean canAct = battle.canAcceptInput() && active != null && active.alive();
        int buttonY = y + 18;
        actionButton(g, x + 18, buttonY, 116, 42, "Attack", state::battleAttack, new Color(92, 79, 50), new Color(156, 132, 76), canAct);
        if (active != null) {
            tooltipZones.add(new TooltipZone(new Rectangle(x + 18, buttonY, 116, 42), "Attack", attackTooltip(active, battle.selectedEnemy())));
        }
        boolean allyTargetReady = battle.hasExplicitPartyTarget();
        actionButton(g, x + 148, buttonY, 104, 42, "Potion", () -> state.useItem("potion_small"), new Color(53, 82, 70), new Color(98, 151, 117), canAct && state.player.hasItem("potion_small") && allyTargetReady);
        tooltipZones.add(new TooltipZone(new Rectangle(x + 148, buttonY, 104, 42), "Small Potion", allyTargetReady ? "Restores HP to the selected ally using the active party member's turn." : "Select an ally before using a restorative item."));
        actionButton(g, x + 266, buttonY, 104, 42, "Ether", () -> state.useItem("ether"), new Color(54, 74, 103), new Color(104, 132, 176), canAct && state.player.hasItem("ether") && allyTargetReady);
        tooltipZones.add(new TooltipZone(new Rectangle(x + 266, buttonY, 104, 42), "Ether", allyTargetReady ? "Restores MP to the selected ally using the active party member's turn." : "Select an ally before using a restorative item."));
        int abilityX = x + 394;
        int abilityWidth = Math.max(112, (w - 426) / 5);
        List<Ability> abilities = active == null ? List.of() : active.activeAbilities();
        int visibleAbilities = Math.min(10, abilities.size());
        for (int i = 0; i < visibleAbilities; i++) {
            Ability ability = abilities.get(i);
            int row = i / 5;
            int col = i % 5;
            int buttonX = abilityX + col * (abilityWidth + 8);
            int rowY = buttonY + row * 48;
            boolean targetReady = !battle.requiresExplicitAllyTarget(ability) || allyTargetReady;
            int cooldown = battle.abilityCooldownRemaining(active, ability);
            boolean enabled = canAct && active.mp >= ability.cost() && targetReady && cooldown <= 0;
            String hotkey = i == 9 ? "0" : Integer.toString(i + 1);
            String label = hotkey + " " + ability.name() + " - " + ability.cost()
                    + (cooldown > 0 ? " CD " + cooldown : "");
            int index = i;
            actionButton(g, buttonX, rowY, abilityWidth, 42, label, () -> state.battleAbility(index), new Color(76, 53, 93), new Color(139, 107, 168), enabled);
            String tooltip = targetReady ? abilityTooltip(active, ability, battle) : abilityTooltip(active, ability, battle) + "\nSelect an ally before using this beneficial ability.";
            tooltipZones.add(new TooltipZone(new Rectangle(buttonX, rowY, abilityWidth, 42),
                    ability.name(), tooltip, abilityIconName(ability)));
        }
        if (active != null && active.abilities.size() > abilities.size()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g.setColor(new Color(170, 176, 191));
            g.drawString("Manage loadout on Character", abilityX, y + h - 12);
        }
    }

    private void drawBattlePartyMember(Graphics2D g, Battle battle, Actor actor, boolean active, int panelX, int panelY, int panelW, int panelH) {
        int[] slot = battlePartyPosition(battle, actor, panelX, panelY, panelW, panelH);
        boolean selected = battle.hasExplicitPartyTarget() && actor == battle.selectedPartyMember();
        int spriteW = 170;
        int spriteH = 220;
        int cardW = 178;
        int cardH = 86;
        String animationAction = battleActorAnimationAction(battle, actor);
        String actorSprite = battleActorSprite(actor);
        boolean stripAnimating = battleActorUsesStrip(actor, actorSprite, animationAction);
        String renderedAction = stripAnimating ? animationAction : null;
        int renderExtraH = stripAnimating && "cast".equals(animationAction) ? 86 : 0;
        int renderH = spriteH + renderExtraH;
        int lunge = active && !stripAnimating ? battle.playerLunge : 0;
        int shake = active && !stripAnimating ? battle.playerOffset : 0;
        int bob = actor.alive() && !stripAnimating ? (int) Math.round(Math.sin((frame + battle.partyMembers().indexOf(actor) * 8) * 0.18) * 3.0) : 0;
        BattleActorPose pose = stripAnimating ? BattleActorPose.REST : battleRenderer.actorPose(battle, actor, false);
        int drawX = slot[0] + lunge + shake + pose.xOffset();
        int drawY = slot[1] + bob - (stripAnimating ? 0 : battle.castOffset(actor)) + pose.yOffset();
        int renderY = drawY - renderExtraH;
        if (actor.alive()) {
            buttons.add(new UiButton(new Rectangle(drawX - 18, drawY - 10, spriteW + 36, spriteH + cardH + 22), "party-target:" + actor.name, () -> state.selectBattlePartyMember(battle.partyMembers().indexOf(actor))));
        }
        drawShadow(g, drawX + 22, drawY + spriteH - 28, spriteW - 44, 24);
        battleRenderer.drawActorSprite(
                g,
                battleActorImage(battle, actor, actorSprite, renderedAction, spriteW, renderH),
                drawX,
                renderY,
                spriteW,
                renderH,
                pose,
                1.0f
        );
        if (selected && actor.alive()) {
            drawBattlePartyTargetMarker(g, drawX, drawY, spriteW, spriteH);
        }
        if (battle.isCasting(actor)) {
            g.setColor(new Color(156, 204, 255, 150));
            g.setStroke(new BasicStroke(3f));
            int pulse = 8 + (frame % 12);
            g.drawOval(drawX + 18 - pulse / 2, drawY + spriteH - 36 - pulse / 3, spriteW - 36 + pulse, 28 + pulse / 2);
        }
        int cardX = drawX + (spriteW - cardW) / 2;
        int cardY = drawY + spriteH - 4;
        g.setColor(new Color(12, 14, 22, 205));
        g.fillRoundRect(cardX, cardY, cardW, cardH, 8, 8);
        g.setColor(selected ? new Color(132, 190, 255) : new Color(86, 98, 128));
        g.setStroke(new BasicStroke(selected ? 3f : 1.5f));
        g.drawRoundRect(cardX, cardY, cardW, cardH, 8, 8);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(238, 239, 244));
        drawCenteredIn(g, actor.name, cardX, cardY + 20, cardW);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(198, 202, 211));
        drawCenteredIn(g, actor.className, cardX, cardY + 38, cardW);
        smallBar(g, cardX + 16, cardY + 48, actor.hp, actor.maxHp, new Color(190, 76, 82), "HP");
        smallBar(g, cardX + 16, cardY + 66, actor.mp, actor.maxMp, new Color(84, 129, 205), "MP");
        drawStatusIcons(g, battle, actor, cardX + cardW + 8, cardY + 10);
        if (!actor.alive()) {
            g.setColor(new Color(9, 10, 16, 150));
            g.fillRoundRect(drawX, drawY, spriteW, spriteH, 8, 8);
            g.setColor(new Color(238, 239, 244));
            drawCenteredIn(g, "Down", drawX, drawY + spriteH / 2, spriteW);
        }
    }

    private void drawBattleEnemySprite(Graphics2D g, Battle battle, Actor foe, int panelX, int panelY, int panelW, int panelH) {
        int[] slot = battleEnemyPosition(battle, foe, panelX, panelY, panelW, panelH);
        int index = Math.max(0, battle.enemies().indexOf(foe));
        boolean selected = foe == battle.selectedEnemy();
        double scale = enemySpriteScale(battle.monsterSpecFor(foe));
        int baseSpriteW = 230;
        int baseSpriteH = 230;
        int spriteW = Math.max(120, (int) Math.round(baseSpriteW * scale));
        int spriteH = Math.max(120, (int) Math.round(baseSpriteH * scale));
        int cardW = 220;
        int cardH = 70;
        String animationAction = battleActorAnimationAction(battle, foe);
        boolean stripAnimating = battleActorUsesStrip(foe, foe.sprite, animationAction);
        String renderedAction = stripAnimating ? animationAction : null;
        int renderExtraH = stripAnimating && "cast".equals(animationAction) ? (int) Math.round(70 * scale) : 0;
        int renderH = spriteH + renderExtraH;
        int lunge = stripAnimating ? 0 : battle.enemyLunge(foe);
        int shake = foe.alive() && !stripAnimating ? battle.monsterOffset : 0;
        int bob = foe.alive() && !stripAnimating ? (int) Math.round(Math.sin((frame + index * 10) * 0.16) * 3.0) : 0;
        BattleActorPose pose = stripAnimating ? BattleActorPose.REST : battleRenderer.actorPose(battle, foe, true);
        int drawX = slot[0] + (baseSpriteW - spriteW) / 2 + shake - lunge + pose.xOffset();
        int drawY = slot[1] + (baseSpriteH - spriteH) + bob - (stripAnimating ? 0 : battle.castOffset(foe)) + pose.yOffset();
        int renderY = drawY - renderExtraH;
        if (foe.alive()) {
            buttons.add(new UiButton(new Rectangle(drawX - 18, drawY - 10, spriteW + 36, spriteH + cardH + 22), "enemy-target:" + foe.name, () -> state.selectBattleEnemy(index)));
        }
        float fade = Math.max(0.0f, Math.min(1.0f, battle.enemyFade(foe) / 255.0f));
        drawShadow(g, drawX + 30, drawY + spriteH - 30, spriteW - 60, 26);
        battleRenderer.drawActorSprite(
                g,
                battleActorImage(battle, foe, foe.sprite, renderedAction, spriteW, renderH),
                drawX,
                renderY,
                spriteW,
                renderH,
                pose,
                fade
        );
        if (selected && foe.alive()) {
            drawBattleEnemyTargetMarker(g, drawX, drawY, spriteW, spriteH);
        } else if (battle.isCasting(foe)) {
            g.setColor(new Color(200, 154, 255, 170));
            g.setStroke(new BasicStroke(3f));
            int pulse = 8 + (frame % 12);
            g.drawOval(drawX + 24 - pulse / 2, drawY + spriteH - 38 - pulse / 3, spriteW - 48 + pulse, 30 + pulse / 2);
        }
    }

    private void drawBattleEnemyCard(Graphics2D g, Battle battle, Actor foe, int panelX, int panelY, int panelW, int panelH) {
        int[] slot = battleEnemyPosition(battle, foe, panelX, panelY, panelW, panelH);
        boolean selected = foe == battle.selectedEnemy();
        double scale = enemySpriteScale(battle.monsterSpecFor(foe));
        int baseSpriteW = 230;
        int baseSpriteH = 230;
        int spriteW = Math.max(120, (int) Math.round(baseSpriteW * scale));
        int spriteH = Math.max(120, (int) Math.round(baseSpriteH * scale));
        int cardW = 220;
        int cardH = 70;
        String animationAction = battleActorAnimationAction(battle, foe);
        boolean stripAnimating = battleActorUsesStrip(foe, foe.sprite, animationAction);
        int renderExtraH = stripAnimating && "cast".equals(animationAction) ? (int) Math.round(70 * scale) : 0;
        int lunge = stripAnimating ? 0 : battle.enemyLunge(foe);
        int shake = foe.alive() && !stripAnimating ? battle.monsterOffset : 0;
        int bob = foe.alive() && !stripAnimating ? (int) Math.round(Math.sin((frame + Math.max(0, battle.enemies().indexOf(foe)) * 10) * 0.16) * 3.0) : 0;
        BattleActorPose pose = stripAnimating ? BattleActorPose.REST : battleRenderer.actorPose(battle, foe, true);
        int drawX = slot[0] + (baseSpriteW - spriteW) / 2 + shake - lunge + pose.xOffset();
        int drawY = slot[1] + (baseSpriteH - spriteH) + bob - (stripAnimating ? 0 : battle.castOffset(foe)) + pose.yOffset();
        drawY -= renderExtraH;
        int cardX = drawX + (spriteW - cardW) / 2;
        int cardY = drawY + renderExtraH + spriteH - 4;
        g.setColor(selected ? new Color(40, 31, 16, 222) : new Color(12, 14, 22, 205));
        g.fillRoundRect(cardX, cardY, cardW, cardH, 8, 8);
        g.setColor(selected ? new Color(255, 220, 150) : new Color(86, 88, 102));
        g.setStroke(new BasicStroke(selected ? 3f : 1.5f));
        g.drawRoundRect(cardX, cardY, cardW, cardH, 8, 8);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(selected ? new Color(255, 244, 202) : new Color(205, 207, 216));
        drawCenteredIn(g, foe.name, cardX, cardY + 22, cardW);
        smallBar(g, cardX + 14, cardY + 40, foe.hp, foe.maxHp, new Color(190, 76, 82), "HP");
        drawStatusIcons(g, battle, foe, cardX + 126, cardY + 40);
        Rectangle hoverBounds = new Rectangle(drawX - 18, drawY - 10, spriteW + 36, spriteH + cardH + 22);
        tooltipZones.add(new TooltipZone(
                hoverBounds,
                foe.name,
                monsterTooltip(battle, foe),
                "sprite:" + battle.monsterSpecFor(foe).sprite()
        ));
    }

    private void drawBattlePartyTargetMarker(Graphics2D g, int drawX, int drawY, int spriteW, int spriteH) {
        Graphics2D marker = (Graphics2D) g.create();
        int pulse = frame % 24;
        int left = drawX + 8;
        int right = drawX + spriteW - 8;
        int top = drawY + 16;
        int bottom = drawY + spriteH - 24;
        int bracket = 20 + pulse / 8;
        marker.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        marker.setColor(new Color(7, 12, 24, 175));
        drawTargetBrackets(marker, left, top, right, bottom, bracket);
        marker.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        marker.setColor(new Color(132, 190, 255, 245));
        drawTargetBrackets(marker, left, top, right, bottom, bracket);
        marker.dispose();
    }

    private void drawBattleEnemyTargetMarker(Graphics2D g, int drawX, int drawY, int spriteW, int spriteH) {
        Graphics2D marker = (Graphics2D) g.create();
        int pulse = frame % 24;
        int left = drawX + 10;
        int right = drawX + spriteW - 10;
        int top = drawY + 18;
        int bottom = drawY + spriteH - 26;
        int bracket = 24 + pulse / 6;
        marker.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        marker.setColor(new Color(10, 8, 4, 175));
        drawTargetBrackets(marker, left, top, right, bottom, bracket);
        marker.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        marker.setColor(new Color(255, 221, 113, 245));
        drawTargetBrackets(marker, left, top, right, bottom, bracket);

        int center = drawX + spriteW / 2;
        int pointerY = Math.max(26, drawY + 6);
        Polygon pointer = new Polygon(
                new int[]{center - 11, center + 11, center},
                new int[]{pointerY - 16, pointerY - 16, pointerY + 2},
                3
        );
        marker.setColor(new Color(10, 8, 4, 185));
        marker.fillPolygon(pointer);
        marker.setColor(new Color(255, 221, 113, 245));
        marker.drawPolygon(pointer);
        marker.dispose();
    }

    private void drawTargetBrackets(Graphics2D g, int left, int top, int right, int bottom, int bracket) {
        g.drawLine(left, top, left + bracket, top);
        g.drawLine(left, top, left, top + bracket);
        g.drawLine(right, top, right - bracket, top);
        g.drawLine(right, top, right, top + bracket);
        g.drawLine(left, bottom, left + bracket, bottom);
        g.drawLine(left, bottom, left, bottom - bracket);
        g.drawLine(right, bottom, right - bracket, bottom);
        g.drawLine(right, bottom, right, bottom - bracket);
    }

    private double enemySpriteScale(GameData.MonsterSpec spec) {
        if (spec == null) {
            return 1.0;
        }
        return switch (spec.key()) {
            case "bat", "crypt_bat" -> 0.70;
            case "slime", "river_eel", "crystal_hare", "doe" -> 0.82;
            case "sheep" -> 0.92;
            case "goblin", "goblin_scout", "goblin_archer", "goblin_trapper", "goblin_skirmisher",
                    "goblin_shaman", "spider", "thornling", "ember_imp", "meadow_wolf",
                    "bandit_cutthroat", "bandit_archer", "bandit_captain" -> 0.94;
            case "wolf", "frost_wolf", "stag", "moss_stag", "mountain_goat", "stoneback_goat",
                    "snow_lynx", "skeleton", "wraith", "sand_stalker", "glass_scorpion", "reed_serpent",
                    "marsh_drake", "mountain_drake", "orc_raider", "orc_shaman" -> 1.08;
            case "orc", "hobgoblin_guard", "ash_scorpion", "bog_beast", "goblin_warlord",
                    "bramble_boar", "ember_tortoise", "orc_berserker", "orc_shieldbearer",
                    "red_dragon", "swamp_troll", "frost_troll" -> 1.20;
            case "ice_golem", "goblin_king", "hill_giant", "stone_giant", "fire_giant", "elder_dragon" -> 1.34;
            default -> 1.0;
        };
    }

    private BufferedImage battleActorImage(Battle battle, Actor actor, String sprite, String action, int width, int height) {
        int frameCount = assets.animatedSpriteFrameCount(sprite, action, width, height);
        int animationFrame = battleActorAnimationFrame(battle, actor, frameCount);
        return assets.animatedSpriteFit(sprite, action, width, height, animationFrame);
    }

    private boolean battleActorUsesStrip(Actor actor, String sprite, String action) {
        if ("class_mage_model".equals(sprite)) {
            return false;
        }
        return actor != null && assets.hasAnimatedSprite(sprite, action);
    }

    private String battleActorAnimationAction(Battle battle, Actor actor) {
        BattleActionAnimation animation = battle.activeAnimation();
        if (animation == null || actor == null || !animation.involves(actor)) {
            return "idle";
        }
        if (actor == animation.target && animation.stage() == BattleActionAnimation.Stage.IMPACT) {
            return "hit";
        }
        if (actor != animation.source) {
            return "idle";
        }
        String kind = animation.effectKind == null ? "strike" : animation.effectKind;
        if ("shield".equals(kind) || "ward".equals(kind)) {
            return "defend";
        }
        if ("item".equals(kind)) {
            return "item";
        }
        if ("volley".equals(kind) || "pierce".equals(kind) || battleRenderer.isRangedClass(actor.className)) {
            return "shoot";
        }
        if (battleRenderer.physicalEffect(kind) || battleRenderer.isPhysicalClass(actor.className)) {
            return "attack";
        }
        return "cast";
    }

    private int battleActorAnimationFrame(Battle battle, Actor actor, int frameCount) {
        frameCount = Math.max(1, frameCount);
        BattleActionAnimation animation = battle.activeAnimation();
        if (animation == null || actor == null || !animation.involves(actor)) {
            return frame / 8;
        }
        double progress = switch (animation.stage()) {
            case CAST -> animation.castProgress();
            case TRAVEL -> animation.travelProgress();
            case IMPACT -> animation.impactProgress();
            case DONE -> 0.0;
        };
        return Math.min(frameCount - 1, Math.max(0, (int) Math.floor(progress * frameCount)));
    }

    private void drawStatusIcons(Graphics2D g, Battle battle, Actor actor, int x, int y) {
        int offset = 0;
        for (EffectStack stack : battle.statusesFor(actor)) {
            Rectangle bounds = new Rectangle(x + offset, y, 28, 20);
            g.setColor("buff".equals(stack.effect.affectType()) ? new Color(63, 91, 69) : new Color(95, 57, 61));
            g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 5, 5);
            g.setColor(new Color(226, 229, 236));
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            g.drawString(StatusEffects.iconLabel(stack.effect.key()), bounds.x + 5, bounds.y + 14);
            tooltipZones.add(new TooltipZone(bounds, stack.effect.name(), statusTooltip(stack)));
            offset += 32;
            if (offset > 160) {
                break;
            }
        }
    }

    private String statusTooltip(EffectStack stack) {
        String type = "buff".equals(stack.effect.affectType()) ? "Buff" : "Debuff";
        String powerText = stack.power == 0 ? "" : " Power: " + stack.power + ".";
        return stack.effect.description() + ". " + type + ". Turns remaining: " + stack.turnsRemaining + "."
                + powerText;
    }

    private String monsterTooltip(Battle battle, Actor foe) {
        GameData.MonsterSpec spec = battle.monsterSpecFor(foe);
        GameData.MonsterLore lore = GameData.loreFor(spec);
        int insightCount = state.monsterInsightCount(spec.key());
        int insightLevel = state.monsterInsightLevel(spec.key());
        int insightPercent = state.monsterInsightPercent(spec.key());
        int knowledgeBonus = (int) Math.round((GameData.monsterKnowledgeDamageMultiplier(insightCount, state.player.intelligence) - 1.0) * 100.0);
        Set<String> rememberedWeaknesses = state.knownMonsterVulnerabilities(spec.key());
        StringBuilder body = new StringBuilder();
        body.append("Knowledge: ").append(insightPercent).append("%")
                .append(" (").append(insightCount).append(insightCount == 1 ? " encounter" : " encounters")
                .append(", INT ").append(state.player.intelligence).append(").")
                .append(" Studied damage +").append(knowledgeBonus).append("%.");
        if (!rememberedWeaknesses.isEmpty()) {
            body.append("\nRemembered weakness: ")
                    .append(GameData.damageTypeListLabel(rememberedWeaknesses.stream().sorted().toList()))
                    .append(".");
        }
        if (insightLevel <= 0) {
            body.append("\nOnly its outline is familiar. Fight it or raise Intelligence to learn more.");
            return body.toString();
        }
        body.append("\n").append(GameData.monsterFlavorText(spec));
        body.append("\nKnown: ").append(capitalize(spec.species())).append(". HP ")
                .append(foe.hp).append("/").append(foe.maxHp).append(".");
        if (insightLevel >= 2) {
            body.append("\nLikely stats: HP ").append(rangeLabel(spec.hp(), 0.12))
                    .append(", ATK ").append(rangeLabel(spec.attack(), 0.12))
                    .append(", DEF ").append(rangeLabel(spec.defense(), 0.15)).append(".");
        }
        if (insightLevel >= 3) {
            List<MonsterAbility> abilities = monsterAbilitiesForTooltip(battle, foe);
            body.append("\nThreat: ").append(GameData.damageTypeListLabel(lore.damageTypes())).append(".");
            body.append(" Abilities: ");
            if (abilities.isEmpty()) {
                body.append("none observed.");
            } else {
                body.append(String.join(", ", abilities.stream().map(MonsterAbility::name).toList())).append(".");
            }
        }
        if (insightLevel >= 4) {
            body.append("\n").append(GameData.monsterModifierSummary(spec));
            List<MonsterAbility> abilities = monsterAbilitiesForTooltip(battle, foe);
            if (!abilities.isEmpty()) {
                body.append("\nAbility details: ");
                body.append(String.join("; ", abilities.stream().map(this::monsterAbilityDetail).toList())).append(".");
            }
        }
        return body.toString();
    }

    private List<MonsterAbility> monsterAbilitiesForTooltip(Battle battle, Actor foe) {
        List<MonsterAbility> abilities = new ArrayList<>(MonsterAbilities.forMonster(battle.monsterSpecFor(foe)));
        if (battle.isEliteEnemy(foe)) {
            abilities.addAll(MonsterAbilities.eliteAbilitiesFor(battle.monsterSpecFor(foe)));
        }
        return abilities;
    }

    private String monsterAbilityDetail(MonsterAbility ability) {
        String kind = ability.kind() == MonsterAbility.Kind.BUFF ? "buff" : GameData.damageTypeListLabel(GameData.damageTypesForEffect(ability.effect()));
        String statusText = ability.statuses().isEmpty()
                ? ""
                : ", " + String.join("/", ability.statuses().stream().map(status -> statusName(status.key())).toList());
        return ability.name() + " " + kind + " PWR " + ability.power() + " " + Math.round(ability.chance() * 100) + "%" + statusText;
    }

    private String rangeLabel(int value, double spread) {
        int min = Math.max(1, (int) Math.floor(value * (1.0 - spread)));
        int max = Math.max(min, (int) Math.ceil(value * (1.0 + spread)));
        return min + "-" + max;
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
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
        BattleActionAnimation animation = battle.activeAnimation();
        if (animation == null && (battle.effectTimer <= 0 || battle.effectTarget == null)) {
            return;
        }
        Actor effectSource = animation == null ? battle.effectSource : animation.source;
        Actor effectTarget = animation == null ? battle.effectTarget : animation.target;
        String kind = animation == null ? battle.effectKind : animation.effectKind;
        kind = kind == null || kind.isBlank() ? "strike" : kind;
        if (effectTarget == null) {
            return;
        }
        int[] source = battleActorCenter(battle, effectSource, panelX, panelY, panelW, panelH);
        int[] target = battleActorCenter(battle, effectTarget, panelX, panelY, panelW, panelH);
        List<int[]> visualTargets = battleEffectTargetCenters(battle, animation, target, panelX, panelY, panelW, panelH);
        BattleActionAnimation.VisualMode visualMode = animation == null
                ? BattleActionAnimation.VisualMode.SINGLE
                : animation.visualMode;
        Graphics2D fx = (Graphics2D) g.create();
        fx.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (animation != null && animation.stage() == BattleActionAnimation.Stage.CAST) {
            drawCastBattleEffect(fx, source, animation.castProgress(), battle.enemies().contains(effectSource));
            fx.dispose();
            return;
        }
        float alpha;
        double phase;
        if (animation == null) {
            alpha = Math.max(0.08f, Math.min(0.85f, battle.effectTimer / 20.0f));
            phase = 1.0 - Math.max(0.0, Math.min(1.0, battle.effectTimer / 20.0));
        } else if (animation.stage() == BattleActionAnimation.Stage.IMPACT) {
            alpha = Math.max(0.08f, (float) (0.85 - animation.impactProgress() * 0.45));
            phase = 1.0;
        } else {
            alpha = 0.85f;
            phase = animation.travelProgress();
        }
        if (drawImageBattleEffect(fx, kind, source, target, visualTargets, visualMode, alpha, phase)) {
            drawBattleEffectParticles(fx, kind, source, target, alpha, phase);
            fx.dispose();
            return;
        }
        fx.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
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
        drawBattleEffectParticles(fx, kind, source, target, alpha, phase);
        fx.dispose();
    }

    private List<int[]> battleEffectTargetCenters(Battle battle, BattleActionAnimation animation, int[] fallbackTarget,
                                                  int panelX, int panelY, int panelW, int panelH) {
        ArrayList<int[]> centers = new ArrayList<>();
        if (animation != null) {
            for (Actor actor : animation.targets) {
                if (actor != null) {
                    centers.add(battleActorCenter(battle, actor, panelX, panelY, panelW, panelH));
                }
            }
        }
        if (centers.isEmpty()) {
            centers.add(fallbackTarget);
        }
        return centers;
    }

    private void drawBattleEffectParticles(Graphics2D g, String kind, int[] source, int[] target, float alpha, double phase) {
        if (!battleEffectHasParticles(kind)) {
            return;
        }
        int count = Math.min(MAX_BATTLE_PARTICLES, battleParticleCount(kind));
        Color color = battleParticleColor(kind);
        boolean travel = battleEffectUsesTravelParticles(kind);
        int seed = Math.abs(kind.hashCode() * 31 + source[0] * 17 + target[1] * 23);
        Composite oldComposite = g.getComposite();
        for (int i = 0; i < count; i++) {
            double life = Math.floorMod(frame * 3 + seed + i * 19, 72) / 72.0;
            double angle = seed * 0.003 + i * 2.399 + frame * (0.045 + i * 0.002);
            int cx;
            int cy;
            if (travel && phase < 0.98) {
                double p = Math.max(0.05, Math.min(0.95, phase + (i - count / 2.0) * 0.014));
                cx = (int) Math.round(source[0] + (target[0] - source[0]) * p);
                cy = (int) Math.round(source[1] + (target[1] - source[1]) * p);
                cx += (int) Math.round(Math.cos(angle) * (7 + i % 3 * 3));
                cy += (int) Math.round(Math.sin(angle * 1.3) * (6 + i % 4 * 2));
            } else {
                double radius = 18 + i % 5 * 7 + life * 26;
                cx = target[0] + (int) Math.round(Math.cos(angle) * radius);
                cy = target[1] + (int) Math.round(Math.sin(angle * 1.18) * radius * 0.62 - life * 12);
            }
            int size = Math.max(2, scaled(3 + i % 3) - (int) Math.round(life * scaled(2)));
            float particleAlpha = (float) Math.max(0.0, Math.min(0.62, alpha * (1.0 - life) * battleParticleAlpha(kind)));
            g.setComposite(AlphaComposite.SrcOver.derive(particleAlpha));
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 230));
            if ("lightning".equals(kind) && i < 5) {
                int ex = cx + (int) Math.round(Math.cos(angle + 1.1) * scaled(10));
                int ey = cy + (int) Math.round(Math.sin(angle + 1.1) * scaled(8));
                g.drawLine(cx, cy, ex, ey);
            } else if ("water".equals(kind)) {
                g.drawArc(cx - size, cy - size / 2, size * 2, size, (int) Math.round(angle * 80), 155);
            } else if ("nature".equals(kind) || "thorn".equals(kind)) {
                g.fillOval(cx - size, cy - size / 2, size + 2, Math.max(2, size / 2 + 1));
                g.drawLine(cx, cy, cx + (int) Math.round(Math.cos(angle) * size * 1.4), cy + (int) Math.round(Math.sin(angle) * size));
            } else if ("slash".equals(kind) || "cleave".equals(kind) || "claw".equals(kind)
                    || "fang".equals(kind) || "volley".equals(kind) || "pierce".equals(kind)) {
                int ex = cx + (int) Math.round(Math.cos(angle) * scaled(12 + i % 4 * 2));
                int ey = cy + (int) Math.round(Math.sin(angle) * scaled(5 + i % 3));
                g.drawLine(cx, cy, ex, ey);
            } else if ("holy".equals(kind) || "radiant".equals(kind) || "prismatic".equals(kind) || "chaos".equals(kind)) {
                int ray = scaled(3 + i % 3);
                g.drawLine(cx - ray, cy, cx + ray, cy);
                g.drawLine(cx, cy - ray, cx, cy + ray);
            } else {
                g.fillOval(cx - size / 2, cy - size / 2, size, size);
            }
        }
        g.setComposite(oldComposite);
    }

    private boolean battleEffectHasParticles(String kind) {
        if (kind != null && kind.endsWith("_unique")) {
            return true;
        }
        return switch (kind) {
            case "heal", "regeneration", "shield", "ward", "fire", "burn", "ember", "spark", "radiant",
                    "holy", "lightning", "arcane", "rune", "void", "dark", "frost", "water", "poison",
                    "acid", "web", "thorn", "nature", "shadow", "dust", "howl", "item", "prismatic",
                    "chaos", "slash", "cleave", "claw", "fang", "volley", "pierce", "strike", "impact",
                    "bash" -> true;
            default -> false;
        };
    }

    private boolean battleEffectUsesTravelParticles(String kind) {
        if (kind != null && kind.endsWith("_unique")) {
            return isGeneratedProjectileEffect(kind);
        }
        return switch (kind) {
            case "fire", "burn", "ember", "spark", "radiant", "holy", "lightning", "arcane", "rune",
                    "void", "dark", "frost", "water", "poison", "acid", "web", "thorn", "nature",
                    "shadow", "item", "prismatic", "chaos", "volley", "pierce" -> true;
            default -> false;
        };
    }

    private int battleParticleCount(String kind) {
        if (kind != null && kind.endsWith("_unique")) {
            return isGeneratedVolleyEffect(kind) ? 18 : 14;
        }
        return switch (kind) {
            case "prismatic", "chaos" -> 18;
            case "lightning", "fire", "burn", "ember", "void", "dark", "arcane", "rune", "shadow",
                    "water", "nature" -> 14;
            case "heal", "regeneration", "holy", "radiant", "spark", "item", "volley", "pierce" -> 12;
            case "frost", "poison", "acid", "web", "thorn", "slash", "cleave", "claw", "fang" -> 10;
            case "strike", "impact", "bash" -> 7;
            default -> 6;
        };
    }

    private float battleParticleAlpha(String kind) {
        if (kind != null && kind.endsWith("_unique")) {
            return 0.58f;
        }
        return switch (kind) {
            case "shadow", "void", "dark", "dust" -> 0.44f;
            case "lightning", "fire", "burn", "ember", "holy", "radiant", "prismatic", "chaos" -> 0.60f;
            case "slash", "cleave", "claw", "fang", "volley", "pierce" -> 0.56f;
            default -> 0.50f;
        };
    }

    private Color battleParticleColor(String kind) {
        if (kind != null && kind.endsWith("_unique")) {
            if (kind.contains("frost") || kind.contains("glacier") || kind.contains("water")
                    || kind.contains("stormline")) {
                return new Color(150, 222, 255);
            }
            if (kind.contains("fire") || kind.contains("inferno") || kind.contains("noon")
                    || kind.contains("phoenix")) {
                return new Color(255, 171, 75);
            }
            if (kind.contains("thorn") || kind.contains("briar") || kind.contains("primeval")) {
                return new Color(130, 220, 108);
            }
            if (kind.contains("shot") || kind.contains("arrow")) {
                return new Color(255, 226, 116);
            }
            if (kind.contains("chaos") || kind.contains("ley")) {
                return new Color(255, 130, 205);
            }
            return new Color(220, 210, 235);
        }
        return switch (kind) {
            case "heal", "regeneration" -> new Color(134, 241, 166);
            case "shield", "ward", "frost" -> new Color(150, 222, 255);
            case "water" -> new Color(100, 190, 255);
            case "fire", "burn", "ember" -> new Color(255, 171, 75);
            case "spark", "radiant", "item" -> new Color(255, 226, 116);
            case "holy" -> new Color(255, 245, 180);
            case "lightning" -> new Color(154, 211, 255);
            case "arcane", "rune" -> new Color(174, 130, 255);
            case "prismatic", "chaos" -> new Color(255, 130, 205);
            case "void", "dark", "shadow" -> new Color(143, 106, 218);
            case "poison", "acid", "web", "thorn", "nature" -> new Color(130, 220, 108);
            case "slash", "cleave", "claw", "fang", "volley", "pierce" -> new Color(255, 240, 206);
            case "strike", "impact", "bash" -> new Color(255, 210, 142);
            default -> new Color(220, 210, 235);
        };
    }

    private void drawCastBattleEffect(Graphics2D g, int[] source, double progress, boolean hostile) {
        float alpha = (float) (0.25 + Math.sin(progress * Math.PI) * 0.5);
        int radius = 30 + (int) Math.round(progress * 18);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.05f, Math.min(0.8f, alpha))));
        Color ring = hostile ? new Color(195, 124, 255) : new Color(126, 205, 255);
        Color spark = hostile ? new Color(255, 130, 205) : new Color(255, 236, 160);
        g.setColor(ring);
        g.drawOval(source[0] - radius, source[1] - radius, radius * 2, radius * 2);
        g.drawOval(source[0] - radius / 2, source[1] - radius / 2, radius, radius);
        g.drawLine(source[0] - radius - 6, source[1], source[0] - radius / 2, source[1]);
        g.drawLine(source[0] + radius / 2, source[1], source[0] + radius + 6, source[1]);
        g.setColor(spark);
        for (int i = 0; i < 8; i++) {
            double angle = frame * 0.08 + i * Math.PI / 4.0;
            int x = source[0] + (int) Math.round(Math.cos(angle) * (radius + 6));
            int y = source[1] + (int) Math.round(Math.sin(angle) * (radius * 0.68 + 4));
            int ray = 3 + i % 2;
            g.drawLine(x - ray, y, x + ray, y);
            g.drawLine(x, y - ray, x, y + ray);
        }
        g.setComposite(AlphaComposite.SrcOver);
    }

    private boolean drawImageBattleEffect(Graphics2D g, String kind, int[] source, int[] target, List<int[]> visualTargets,
                                          BattleActionAnimation.VisualMode visualMode, float alpha, double phase) {
        String sprite = effectSpriteName(kind);
        if (sprite == null) {
            return false;
        }
        double angle = effectTravelAngle(source, target);
        phase = Math.max(0.0, Math.min(1.0, phase));
        double pulse = 0.92 + Math.sin((frame + phase * 20.0) * 0.32) * 0.08;
        if (visualTargets != null && visualTargets.size() > 1) {
            if (visualMode == BattleActionAnimation.VisualMode.CHAIN) {
                drawChainBattleEffect(g, sprite, kind, source, visualTargets, alpha, phase, pulse);
                return true;
            }
            if (visualMode == BattleActionAnimation.VisualMode.MULTI) {
                drawMultiProjectileBattleEffect(g, sprite, kind, source, visualTargets, alpha, phase, pulse);
                return true;
            }
            if (visualMode == BattleActionAnimation.VisualMode.AOE) {
                drawAoeBattleEffect(g, sprite, kind, source, visualTargets, alpha, phase, pulse);
                return true;
            }
        }
        if (kind.endsWith("_unique")) {
            if (isGeneratedHealEffect(kind)) {
                drawEffectSprite(g, sprite, target[0], target[1] - 4, effectSize(142, pulse), effectSize(142, pulse), 0.0, alpha);
                return true;
            }
            if (isGeneratedWardEffect(kind)) {
                drawEffectSprite(g, sprite, target[0] + 6, target[1] - 4, effectSize(148, pulse), effectSize(162, pulse), 0.0, alpha);
                return true;
            }
            if (isGeneratedMeleeEffect(kind)) {
                drawEffectSprite(g, sprite, target[0], target[1], effectSize(164, pulse), effectSize(124, pulse),
                        crescentImpactAngle(source, target, 0.16), alpha);
                return true;
            }
            if (isGeneratedProjectileEffect(kind)) {
                double travelPhase = easedTravelPhase(phase);
                if (isGeneratedVolleyEffect(kind)) {
                    for (int i = -1; i <= 1; i++) {
                        int[] offsetSource = new int[]{source[0], source[1] + i * 12};
                        int[] offsetTarget = new int[]{target[0], target[1] + i * 10};
                        double offsetAngle = effectTravelAngle(offsetSource, offsetTarget);
                        drawTravelingSprite(g, sprite, offsetSource, offsetTarget, offsetAngle, travelPhase,
                                136, 86, alpha * (i == 0 ? 0.92f : 0.66f));
                    }
                } else {
                    drawTravelingSprite(g, sprite, source, target, angle, travelPhase,
                            generatedProjectileWidth(kind), generatedProjectileHeight(kind), alpha * 0.92f);
                }
                if (phase >= 0.98) {
                    drawEffectSprite(g, sprite, target[0], target[1],
                            effectSize(generatedImpactWidth(kind), pulse),
                            effectSize(generatedImpactHeight(kind), pulse),
                            correctedEffectAngle(sprite, angle), alpha * 0.52f);
                }
                return true;
            }
            drawEffectSprite(g, sprite, target[0], target[1], effectSize(154, pulse), effectSize(134, pulse), 0.0, alpha);
            return true;
        }
        switch (kind) {
            case "heal", "regeneration", "grove_hymn", "root_memory", "seraphic_hymn" -> {
                drawEffectSprite(g, sprite, target[0], target[1] - 4, effectSize(134, pulse), effectSize(134, pulse), 0.0, alpha);
                return true;
            }
            case "shield", "ward" -> {
                drawEffectSprite(g, sprite, target[0] + 10, target[1] - 4, effectSize(128, pulse), effectSize(164, pulse), 0.0, alpha);
                return true;
            }
            case "slash", "fang" -> {
                double slashAngle = crescentImpactAngle(source, target, 0.2);
                drawEffectSprite(g, sprite, target[0], target[1], effectSize(154, pulse), effectSize(120, pulse), slashAngle, alpha);
                return true;
            }
            case "claw" -> {
                double clawAngle = crescentImpactAngle(source, target, 0.1);
                drawEffectSprite(g, sprite, target[0], target[1], effectSize(150, pulse), effectSize(136, pulse), clawAngle, alpha);
                return true;
            }
            case "strike", "impact", "bash" -> {
                drawEffectSprite(g, sprite, target[0], target[1] + 6, effectSize(138, pulse), effectSize(118, pulse), 0.0, alpha);
                return true;
            }
            case "cleave" -> {
                double slashAngle = crescentImpactAngle(source, target, 0.1);
                drawEffectSprite(g, sprite, target[0], target[1] - 2, effectSize(178, pulse), effectSize(126, pulse), slashAngle, alpha);
                return true;
            }
            case "shadow" -> {
                drawTravelingSprite(g, sprite, source, target, angle, phase, 116, 116, alpha);
                drawEffectSprite(g, sprite, target[0], target[1], effectSize(132, pulse), effectSize(132, pulse), 0.0, alpha * 0.62f);
                return true;
            }
            case "sonic" -> {
                drawEffectSprite(g, sprite, target[0], target[1], effectSize(176, pulse), effectSize(136, pulse), 0.0, alpha);
                return true;
            }
            case "bone" -> {
                drawEffectSprite(g, sprite, target[0], target[1] + 10, effectSize(142, pulse), effectSize(128, pulse), 0.0, alpha);
                return true;
            }
            case "dust", "howl" -> {
                drawEffectSprite(g, sprite, target[0], target[1] + 12, effectSize(150, pulse), effectSize(116, pulse), 0.0, alpha);
                return true;
            }
            case "spark", "radiant", "item" -> {
                drawTravelingSprite(g, sprite, source, target, angle, phase, 92, 72, alpha * 0.62f);
                drawEffectSprite(g, sprite, target[0], target[1] + 4, effectSize(136, pulse), effectSize(120, pulse), 0.0, alpha);
                return true;
            }
            case "holy" -> {
                drawTravelingSprite(g, sprite, source, target, angle, phase, 132, 76, alpha);
                drawEffectSprite(g, sprite, target[0], target[1] + 2, 112, 96, correctedEffectAngle(sprite, angle), alpha * 0.58f);
                return true;
            }
            case "lightning" -> {
                drawTravelingSprite(g, sprite, source, target, angle, phase, 132, 86, alpha);
                drawEffectSprite(g, sprite, target[0], target[1], 112, 82, correctedEffectAngle(sprite, angle), alpha * 0.62f);
                return true;
            }
            case "arcane", "rune", "ley_detonation" -> {
                drawTravelingSprite(g, sprite, source, target, angle, phase, 108, 82, alpha * 0.68f);
                drawEffectSprite(g, sprite, target[0], target[1] + 6, effectSize(156, pulse), effectSize(112, pulse), 0.0, alpha * 0.86f);
                return true;
            }
            case "prismatic", "chaos", "worldsplitter" -> {
                drawTravelingSprite(g, sprite, source, target, angle, phase, 132, 92, alpha * 0.76f);
                drawEffectSprite(g, sprite, target[0], target[1], effectSize(184, pulse), effectSize(152, pulse), 0.0, alpha);
                drawEffectSprite(g, "fx_spark", target[0], target[1] - 10, effectSize(118, pulse), effectSize(118, pulse), 0.0, alpha * 0.32f);
                return true;
            }
            case "ember" -> {
                drawTravelingSprite(g, sprite, source, target, angle, phase, 132, 88, alpha);
                drawEffectSprite(g, sprite, target[0], target[1], 96, 72, correctedEffectAngle(sprite, angle), alpha * 0.58f);
                return true;
            }
            case "void", "dark", "execution_mark" -> {
                drawTravelingSprite(g, sprite, source, target, angle, phase, 112, 92, alpha * 0.72f);
                drawEffectSprite(g, sprite, target[0], target[1], effectSize(144, pulse), effectSize(128, pulse), 0.0, alpha * 0.88f);
                return true;
            }
            case "water" -> {
                drawTravelingSprite(g, sprite, source, target, angle, phase, 126, 84, alpha * 0.70f);
                drawEffectSprite(g, "fx_heal", target[0], target[1] + 4, effectSize(122, pulse), effectSize(96, pulse), 0.0, alpha * 0.44f);
                drawEffectSprite(g, sprite, target[0], target[1], 104, 76, correctedEffectAngle(sprite, angle), alpha * 0.52f);
                return true;
            }
            case "volley", "pierce" -> {
                for (int i = -1; i <= 1; i++) {
                    int[] offsetSource = new int[]{source[0], source[1] + i * 10};
                    int[] offsetTarget = new int[]{target[0], target[1] + i * 8};
                    double offsetAngle = effectTravelAngle(offsetSource, offsetTarget);
                    drawTravelingSprite(g, sprite, offsetSource, offsetTarget, offsetAngle, phase, 118, 58, alpha * (i == 0 ? 1.0f : 0.72f));
                }
                return true;
            }
            case "nature" -> {
                drawTravelingSprite(g, sprite, source, target, angle, phase, 132, 88, alpha * 0.78f);
                drawEffectSprite(g, sprite, target[0], target[1], effectSize(142, pulse), effectSize(116, pulse), correctedEffectAngle(sprite, angle), alpha * 0.68f);
                drawEffectSprite(g, "fx_web", target[0], target[1] + 4, effectSize(116, pulse), effectSize(96, pulse), 0.0, alpha * 0.30f);
                return true;
            }
            case "fire", "burn", "frost", "poison", "acid", "web", "thorn" -> {
                drawTravelingSprite(g, sprite, source, target, angle, phase, 126, 88, alpha);
                drawEffectSprite(g, sprite, target[0], target[1], 96, 78, correctedEffectAngle(sprite, angle), alpha * 0.54f);
                return true;
            }
            default -> {
                drawTravelingSprite(g, sprite, source, target, angle, phase, 124, 78, alpha);
                drawEffectSprite(g, "fx_slash", target[0], target[1], 112, 82, correctedEffectAngle("fx_slash", angle), alpha * 0.5f);
                return true;
            }
        }
    }

    private int effectSize(int base, double multiplier) {
        return Math.max(1, (int) Math.round(base * multiplier));
    }

    private double crescentImpactAngle(int[] source, int[] target, double lift) {
        return source[0] > target[0] ? lift : Math.PI + lift;
    }

    private void drawChainBattleEffect(Graphics2D g, String sprite, String kind, int[] source, List<int[]> targets,
                                       float alpha, double phase, double pulse) {
        int hops = Math.max(1, targets.size());
        double progress = easedTravelPhase(phase) * hops;
        int[] from = source;
        for (int i = 0; i < targets.size(); i++) {
            int[] to = targets.get(i);
            double segment = Math.max(0.0, Math.min(1.0, progress - i));
            double angle = effectTravelAngle(from, to);
            if (segment > 0.0 && segment < 1.0) {
                drawTravelingSprite(g, sprite, from, to, angle, segment,
                        generatedProjectileWidth(kind), generatedProjectileHeight(kind), alpha * 0.95f);
            } else if (segment >= 1.0) {
                drawEffectSprite(g, sprite, to[0], to[1], effectSize(92, pulse), effectSize(72, pulse),
                        correctedEffectAngle(sprite, angle), alpha * 0.34f);
            }
            from = to;
        }
    }

    private void drawMultiProjectileBattleEffect(Graphics2D g, String sprite, String kind, int[] source, List<int[]> targets,
                                                 float alpha, double phase, double pulse) {
        double travelPhase = easedTravelPhase(phase);
        for (int i = 0; i < targets.size(); i++) {
            int[] target = targets.get(i);
            int offset = i - targets.size() / 2;
            int[] offsetSource = new int[]{source[0], source[1] + offset * 10};
            double angle = effectTravelAngle(offsetSource, target);
            drawTravelingSprite(g, sprite, offsetSource, target, angle, travelPhase,
                    generatedProjectileWidth(kind), generatedProjectileHeight(kind), alpha * 0.88f);
            if (phase >= 0.98) {
                drawEffectSprite(g, sprite, target[0], target[1], effectSize(96, pulse), effectSize(76, pulse),
                        correctedEffectAngle(sprite, angle), alpha * 0.42f);
            }
        }
    }

    private void drawAoeBattleEffect(Graphics2D g, String sprite, String kind, int[] source, List<int[]> targets,
                                     float alpha, double phase, double pulse) {
        int[] center = targetGroupCenter(targets);
        double angle = effectTravelAngle(source, center);
        double travelPhase = easedTravelPhase(Math.min(0.9, phase * 1.25));
        if (phase < 0.92) {
            drawTravelingSprite(g, sprite, source, center, angle, travelPhase,
                    generatedProjectileWidth(kind) + 16, generatedProjectileHeight(kind) + 12, alpha * 0.82f);
        }
        double impactProgress = Math.max(0.0, (phase - 0.58) / 0.42);
        if (impactProgress > 0.0 || phase >= 0.98) {
            double blast = 0.72 + impactProgress * 0.48;
            drawEffectSprite(g, sprite, center[0], center[1], effectSize(220, pulse * blast),
                    effectSize(172, pulse * blast), correctedEffectAngle(sprite, angle), alpha * (float) (0.56 + impactProgress * 0.28));
            drawEffectSprite(g, "fx_impact", center[0], center[1] + 8, effectSize(238, blast),
                    effectSize(152, blast), 0.0, alpha * 0.46f);
        }
    }

    private int[] targetGroupCenter(List<int[]> targets) {
        if (targets == null || targets.isEmpty()) {
            return new int[]{0, 0};
        }
        int x = 0;
        int y = 0;
        for (int[] target : targets) {
            x += target[0];
            y += target[1];
        }
        return new int[]{x / targets.size(), y / targets.size()};
    }

    private void drawTravelingSprite(Graphics2D g, String sprite, int[] source, int[] target, double angle, double phase, int width, int height, float alpha) {
        double travel = 0.14 + phase * 0.74;
        int x = (int) Math.round(source[0] + (target[0] - source[0]) * travel);
        int y = (int) Math.round(source[1] + (target[1] - source[1]) * travel);
        double drawAngle = correctedEffectAngle(sprite, angle);
        drawEffectSprite(g, sprite, x, y, width, height, drawAngle, alpha);
        double distance = Math.hypot(target[0] - source[0], target[1] - source[1]);
        double trailStep = Math.max(0.06, Math.min(0.16, 42.0 / Math.max(160.0, distance)));
        for (int i = 1; i <= 3; i++) {
            double trail = Math.max(0.04, travel - trailStep * i);
            int trailX = (int) Math.round(source[0] + (target[0] - source[0]) * trail);
            int trailY = (int) Math.round(source[1] + (target[1] - source[1]) * trail);
            double scale = 0.78 - i * 0.12;
            drawEffectSprite(g, sprite, trailX, trailY, width * scale, height * scale, drawAngle, alpha * (0.26f / i));
        }
    }

    private double easedTravelPhase(double phase) {
        phase = Math.max(0.0, Math.min(1.0, phase));
        return phase * phase * (3.0 - 2.0 * phase);
    }

    private double correctedEffectAngle(String sprite, double travelAngle) {
        return travelAngle - nativeEffectForwardAngle(sprite);
    }

    private double effectTravelAngle(int[] source, int[] target) {
        return Math.atan2(target[1] - source[1], target[0] - source[0]);
    }

    private double nativeEffectForwardAngle(String sprite) {
        return switch (sprite) {
            // Directional sprites are calibrated to the angle their "head" points at rest.
            case "fx_arrow" -> Math.toRadians(-30);
            case "fx_holy" -> Math.toRadians(-42);
            case "fx_lightning" -> Math.toRadians(-36);
            case "fx_web" -> Math.toRadians(-22);
            case "fx_thorn" -> Math.toRadians(-28);
            case "fx_fire", "fx_ember", "fx_poison" -> Math.toRadians(145);
            case "fx_frost" -> Math.toRadians(-38);
            case "fx_slash" -> Math.PI;
            case "fx_firebolt_unique", "fx_frost_lance_unique", "fx_water_jet_unique",
                    "fx_piercing_shot_unique", "fx_snare_arrow_unique", "fx_radiant_bolt_unique",
                    "fx_hemostatic_strike_unique", "fx_shield_ram_unique", "fx_thorn_lash_unique",
                    "fx_inferno_script_unique", "fx_glacier_prison_unique", "fx_ley_reversal_unique",
                    "fx_chaos_bloom_unique", "fx_heartline_shot_unique", "fx_caustic_flask_unique",
                    "fx_briar_tempest_unique", "fx_noonflare_unique", "fx_titan_hammer_unique" -> Math.toRadians(140);
            case "fx_stormline_volley_unique" -> Math.toRadians(-28);
            default -> 0.0;
        };
    }

    private void drawEffectSprite(Graphics2D g, String sprite, int centerX, int centerY, double width, double height, double angle, float alpha) {
        int drawW = Math.max(1, (int) Math.round(width));
        int drawH = Math.max(1, (int) Math.round(height));
        int frameCount = assets.effectSpriteFrameCount(sprite);
        double animationCursor = frame * 0.62 + Math.floorMod(sprite.hashCode(), 6);
        int animationFrame = (int) Math.floor(animationCursor);
        double frameBlend = animationCursor - Math.floor(animationCursor);
        BufferedImage image = assets.effectSprite(sprite, drawW, drawH, animationFrame);
        Graphics2D spriteG = (Graphics2D) g.create();
        float clampedAlpha = Math.max(0.0f, Math.min(1.0f, alpha));
        spriteG.translate(centerX, centerY);
        spriteG.rotate(angle);
        if (frameCount > 1 && frameBlend > 0.02) {
            float firstAlpha = (float) (clampedAlpha * (1.0 - frameBlend * 0.55));
            spriteG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.0f, Math.min(1.0f, firstAlpha))));
            spriteG.drawImage(image, -drawW / 2, -drawH / 2, drawW, drawH, null);
            BufferedImage nextImage = assets.effectSprite(sprite, drawW, drawH, animationFrame + 1);
            float nextAlpha = (float) (clampedAlpha * frameBlend * 0.55);
            spriteG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.0f, Math.min(1.0f, nextAlpha))));
            spriteG.drawImage(nextImage, -drawW / 2, -drawH / 2, drawW, drawH, null);
            spriteG.dispose();
            return;
        }
        spriteG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clampedAlpha));
        spriteG.drawImage(image, -drawW / 2, -drawH / 2, drawW, drawH, null);
        spriteG.dispose();
    }

    private boolean isGeneratedProjectileEffect(String kind) {
        return switch (kind) {
            case "firebolt_unique", "frost_lance_unique", "water_jet_unique",
                    "piercing_shot_unique", "snare_arrow_unique", "radiant_bolt_unique",
                    "hemostatic_strike_unique", "shield_ram_unique", "thorn_lash_unique",
                    "inferno_script_unique", "glacier_prison_unique", "ley_reversal_unique",
                    "chaos_bloom_unique", "heartline_shot_unique", "stormline_volley_unique",
                    "caustic_flask_unique", "briar_tempest_unique", "noonflare_unique",
                    "titan_hammer_unique" -> true;
            default -> false;
        };
    }

    private boolean isGeneratedVolleyEffect(String kind) {
        return "stormline_volley_unique".equals(kind);
    }

    private boolean isGeneratedMeleeEffect(String kind) {
        return switch (kind) {
            case "dual_cut_unique", "final_challenge_unique" -> true;
            default -> false;
        };
    }

    private int generatedProjectileWidth(String kind) {
        return switch (kind) {
            case "stormline_volley_unique" -> 142;
            case "heartline_shot_unique", "piercing_shot_unique", "snare_arrow_unique" -> 128;
            case "titan_hammer_unique", "chaos_bloom_unique", "briar_tempest_unique" -> 148;
            default -> 136;
        };
    }

    private int generatedProjectileHeight(String kind) {
        return switch (kind) {
            case "heartline_shot_unique", "piercing_shot_unique", "snare_arrow_unique" -> 72;
            case "titan_hammer_unique", "chaos_bloom_unique", "briar_tempest_unique" -> 106;
            default -> 92;
        };
    }

    private int generatedImpactWidth(String kind) {
        return switch (kind) {
            case "titan_hammer_unique", "chaos_bloom_unique", "briar_tempest_unique" -> 178;
            default -> 146;
        };
    }

    private int generatedImpactHeight(String kind) {
        return switch (kind) {
            case "titan_hammer_unique", "chaos_bloom_unique", "briar_tempest_unique" -> 142;
            default -> 116;
        };
    }

    private String effectSpriteName(String kind) {
        if (kind != null && kind.endsWith("_unique")) {
            return "fx_" + kind;
        }
        return switch (kind) {
            case "heal", "regeneration" -> "fx_heal";
            case "grove_hymn" -> "fx_grove_hymn";
            case "root_memory" -> "fx_root_memory";
            case "seraphic_hymn" -> "fx_seraphic_hymn";
            case "shield", "ward" -> "fx_shield";
            case "fire", "burn" -> "fx_fire";
            case "ember" -> "fx_ember";
            case "water" -> "fx_frost";
            case "spark", "radiant", "item" -> "fx_spark";
            case "holy" -> "fx_holy";
            case "lightning" -> "fx_lightning";
            case "arcane", "rune" -> "fx_arcane";
            case "ley_detonation" -> "fx_ley_detonation";
            case "prismatic", "chaos" -> "fx_prismatic";
            case "worldsplitter" -> "fx_worldsplitter";
            case "void", "dark" -> "fx_void_hit";
            case "execution_mark" -> "fx_execution_mark";
            case "frost" -> "fx_frost";
            case "poison", "acid" -> "fx_poison";
            case "web" -> "fx_web";
            case "thorn", "nature" -> "fx_thorn";
            case "volley", "pierce" -> "fx_arrow";
            case "slash" -> "fx_slash";
            case "fang", "claw" -> "fx_claw";
            case "strike", "impact", "bash" -> "fx_impact";
            case "cleave" -> "fx_cleave";
            case "shadow" -> "fx_shadow";
            case "sonic" -> "fx_sonic";
            case "bone" -> "fx_bone";
            case "dust", "howl" -> "fx_dust";
            default -> null;
        };
    }

    private String effectSoundName(String kind) {
        if (kind != null && kind.endsWith("_unique")) {
            if (isGeneratedHealEffect(kind)) {
                return "heal";
            }
            if (isGeneratedWardEffect(kind)) {
                return "shield";
            }
            if (kind.contains("arrow") || kind.contains("shot") || kind.contains("volley")) {
                return "arrow";
            }
            if (kind.contains("fire") || kind.contains("inferno") || kind.contains("noon")) {
                return "fire";
            }
            if (kind.contains("frost") || kind.contains("glacier")) {
                return "frost";
            }
            return "impact";
        }
        return switch (kind) {
            case "heal", "regeneration", "grove_hymn", "root_memory", "seraphic_hymn" -> "heal";
            case "shield", "ward" -> "shield";
            case "fire", "burn", "ember" -> "fire";
            case "frost" -> "frost";
            case "water" -> "heal";
            case "poison", "acid", "web", "thorn", "nature" -> "poison";
            case "volley", "pierce" -> "arrow";
            case "slash", "fang", "strike", "cleave", "claw", "impact", "bash" -> "slash";
            case "shadow", "sonic", "bone", "dust", "howl", "spark", "radiant", "holy", "lightning",
                    "arcane", "rune", "ley_detonation", "void", "dark", "execution_mark",
                    "chaos", "prismatic", "worldsplitter" -> "impact";
            default -> "impact";
        };
    }

    private boolean isGeneratedHealEffect(String kind) {
        return switch (kind) {
            case "mend_unique", "shadow_salve_unique", "mercy_wellspring_unique",
                    "panacea_toss_unique", "primeval_bloom_unique" -> true;
            default -> false;
        };
    }

    private boolean isGeneratedWardEffect(String kind) {
        return switch (kind) {
            case "smoke_veil_unique", "stone_guard_unique", "aegis_circle_unique",
                    "citadel_protocol_unique", "halo_bastion_unique" -> true;
            default -> false;
        };
    }

    private int[] battleActorCenter(Battle battle, Actor actor, int panelX, int panelY, int panelW, int panelH) {
        if (battle.enemies().contains(actor)) {
            int[] slot = battleEnemyPosition(battle, actor, panelX, panelY, panelW, panelH);
            return new int[]{slot[0] + 115 + battle.monsterOffset - battle.enemyLunge(actor), slot[1] + 112};
        }
        int[] slot = battlePartyPosition(battle, actor, panelX, panelY, panelW, panelH);
        int x = slot[0] + 85;
        int y = slot[1] + 108;
        if (actor == battle.activeActor()) {
            x += battle.playerLunge + battle.playerOffset;
        }
        return new int[]{x, y};
    }

    private int[] battlePartyPosition(Battle battle, Actor actor, int panelX, int panelY, int panelW, int panelH) {
        int index = Math.max(0, battle.partyMembers().indexOf(actor));
        index = Math.min(3, index);
        int count = Math.max(1, Math.min(4, battle.partyMembers().size()));
        int left = panelX + 130;
        int right = panelX + 390;
        int high = panelY + 122;
        int mid = panelY + 326;
        int low = panelY + 530;
        int[][] one = {{panelX + 250, mid}};
        int[][] two = {{left, panelY + 206}, {left, panelY + 454}};
        int[][] three = {{left, high}, {left, low - 36}, {right, mid - 6}};
        int[][] four = {{left, high}, {right, high}, {left, low}, {right, low}};
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
        index = Math.min(4, index);
        int count = Math.max(1, Math.min(5, battle.enemies().size()));
        int right = panelX + panelW;
        int enemyLeft = Math.max(panelX + 72, right - 650);
        int enemyRight = Math.max(enemyLeft + 180, Math.min(right - 270, right - 370));
        int enemyMid = enemyLeft + (enemyRight - enemyLeft) / 2;
        int high = panelY + 122;
        int mid = panelY + 326;
        int low = panelY + 530;
        int[][] one = {{Math.max(panelX + 180, right - 500), mid}};
        int[][] two = {{enemyLeft, panelY + 168}, {enemyRight, panelY + 454}};
        int[][] three = {{enemyRight, high}, {enemyRight, low - 36}, {enemyLeft, mid - 6}};
        int[][] four = {{enemyLeft, high}, {enemyRight, high}, {enemyLeft, low}, {enemyRight, low}};
        int[][] five = {{enemyMid, high - 28}, {enemyLeft, mid - 42}, {enemyRight, mid - 42}, {enemyLeft, low - 18}, {enemyRight, low - 18}};
        int[][] positions = switch (count) {
            case 1 -> one;
            case 2 -> two;
            case 3 -> three;
            case 4 -> four;
            default -> five;
        };
        return positions[index];
    }

    private void drawDialog(Graphics2D g) {
        Npc npc = state.activeNpc;
        if (npc == null) {
            return;
        }
        if (companionDialogueSprite(npc) != null) {
            drawCompanionDialog(g, npc);
            return;
        }
        int panelW = Math.min(1120, gameAreaWidth() - 96);
        int panelH = Math.min(560, Math.max(430, viewHeight() - 96));
        int panelX = gameAreaCenteredX(panelW);
        int panelY = viewHeight() - panelH - 76;
        drawOverlayBase(g, panelX, panelY, panelW, panelH);
        drawNpcPortraitCard(g, npc, panelX + 26, panelY + 32, 118, 132);

        int textX = panelX + 166;
        int textW = panelW - 214;
        g.setFont(new Font("SansSerif", Font.BOLD, 25));
        g.setColor(new Color(244, 239, 220));
        String displayName = state.npcDisplayName(npc);
        g.drawString(displayName, textX, panelY + 54);
        FontMetrics nameMetrics = g.getFontMetrics();
        tooltipZones.add(new TooltipZone(new Rectangle(textX, panelY + 30, nameMetrics.stringWidth(displayName), 30),
                displayName, npcTooltip(npc)));
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.setColor(new Color(218, 220, 226));
        String line = state.activeNpcDialogLine();
        drawWrapped(g, line, textX, panelY + 88, textW, 24, 3);
        Quest quest = state.questForNpc(npc);

        List<DialogOption> options = dialogOptions(npc, quest);
        int optionsY = panelY + 172;
        g.setColor(new Color(196, 198, 205));
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.drawString("Conversation options", textX, optionsY - 12);
        int listW = Math.min(760, textW);
        int listH = Math.max(112, panelY + panelH - optionsY - 28);
        drawDialogOptionList(g, options, textX, optionsY, listW, listH);

        if (npc.recruitId() != null && npc.recruitCost() > 0 && !state.isRecruited(npc.recruitId())) {
            g.setColor(new Color(196, 198, 205));
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.drawString("Contract available: " + npc.recruitCost() + "g", textX + listW + 18, optionsY + 22);
        } else if (npc.recruitId() != null && state.isRecruited(npc.recruitId())) {
            g.setColor(new Color(144, 215, 150));
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.drawString("Travels with you", textX + listW + 18, optionsY + 22);
        }
    }

    private void drawCompanionDialog(Graphics2D g, Npc npc) {
        int stageW = gameAreaWidth();
        String npcSprite = companionDialogueSprite(npc);
        Color npcAccent = companionAccent(npc);
        int figureH = Math.max(720, viewHeight() - 52);
        int figureW = Math.max(380, Math.min(540, stageW / 3));
        int figureY = Math.max(8, viewHeight() - figureH - 10);
        int npcX = Math.max(8, stageW / 7 - figureW / 2);
        int playerX = Math.min(stageW - figureW - 8, stageW - stageW / 7 - figureW / 2);
        drawDialogueStandingSprite(g, npcSprite, state.npcDisplayName(npc), npcX, figureY, figureW, figureH, npcAccent, false);
        drawDialogueStandingSprite(g, playerDialogueSprite(), state.player.name, playerX, figureY, figureW, figureH, playerDialogueAccent(), false);

        int panelW = Math.min(1080, stageW - 360);
        panelW = Math.max(720, Math.min(panelW, stageW - 48));
        int panelH = Math.min(444, Math.max(404, viewHeight() - 164));
        int panelX = gameAreaCenteredX(panelW);
        int panelY = viewHeight() - panelH - 38;
        drawOverlayBase(g, panelX, panelY, panelW, panelH);

        g.setColor(new Color(npcAccent.getRed(), npcAccent.getGreen(), npcAccent.getBlue(), 130));
        g.drawRoundRect(panelX + 2, panelY + 2, panelW - 4, panelH - 4, 8, 8);

        int textX = panelX + 32;
        int textW = panelW - 64;
        String displayName = state.npcDisplayName(npc);
        g.setFont(new Font("SansSerif", Font.BOLD, 27));
        g.setColor(new Color(244, 239, 220));
        drawClippedString(g, displayName, textX, panelY + 54, textW);
        FontMetrics nameMetrics = g.getFontMetrics();
        tooltipZones.add(new TooltipZone(new Rectangle(textX, panelY + 28, Math.min(textW, nameMetrics.stringWidth(displayName)), 32),
                displayName, npcTooltip(npc), "sprite:" + npcSprite, npcAccent));

        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(npcAccent);
        drawClippedString(g, companionOriginLine(npc), textX, panelY + 76, textW);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(new Color(207, 211, 222));
        drawWrapped(g, companionPresenceLine(npc), textX, panelY + 100, textW, 18, 1);

        int dialogueBottom = drawCompanionDialogueLine(
                g,
                state.activeNpcDialogLineParts(),
                textX,
                panelY + 124,
                textW,
                npcAccent
        );

        Quest quest = state.questForNpc(npc);

        List<DialogOption> options = dialogOptions(npc, quest);
        int optionsY = Math.max(panelY + 214, dialogueBottom + 18);
        g.setColor(new Color(196, 198, 205));
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.drawString("Conversation options", textX, optionsY - 12);
        int listW = Math.min(textW, 780);
        int listH = Math.max(126, panelY + panelH - optionsY - 28);
        drawDialogOptionList(g, options, textX, optionsY, listW, listH);
    }

    private int drawCompanionDialogueLine(
            Graphics2D g,
            DialogueLibrary.DialogueLine line,
            int x,
            int y,
            int w,
            Color accent
    ) {
        String narration = line == null ? "" : line.narration();
        String speech = line == null ? "" : line.speech();
        if (speech.isBlank() && narration.isBlank()) {
            speech = "...";
        }
        int blockH = 80;
        int gap = 22;
        boolean split = !narration.isBlank() && w >= 720;
        if (split) {
            int narrationW = Math.max(230, Math.min(360, (w - gap) * 38 / 100));
            int speechX = x + narrationW + gap;
            int speechW = Math.max(260, w - narrationW - gap);
            g.setFont(new Font("SansSerif", Font.ITALIC, 14));
            g.setColor(new Color(184, 191, 205));
            drawWrapped(g, narration, x, y + 18, narrationW, 19, 3);
            drawCompanionSpeechBox(g, speech, speechX, y - 6, speechW, blockH, accent);
            return y + blockH;
        }
        if (!narration.isBlank()) {
            g.setFont(new Font("SansSerif", Font.ITALIC, 14));
            g.setColor(new Color(184, 191, 205));
            drawWrapped(g, narration, x, y, w, 18, 2);
            y += 42;
        }
        drawCompanionSpeechBox(g, speech, x, y - 6, w, blockH - 16, accent);
        return y + blockH - 16;
    }

    private void drawCompanionSpeechBox(Graphics2D g, String speech, int x, int y, int w, int h, Color accent) {
        g.setColor(new Color(9, 13, 22, 178));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 150));
        g.drawRoundRect(x, y, w, h, 8, 8);
        g.setFont(new Font("SansSerif", Font.PLAIN, 17));
        g.setColor(new Color(238, 239, 244));
        String spoken = speech == null || speech.isBlank() ? "..." : "\"" + speech + "\"";
        drawWrapped(g, spoken, x + 14, y + 27, w - 28, 22, 3);
    }

    private void drawDialogOptionList(Graphics2D g, List<DialogOption> options, int x, int y, int w, int h) {
        String scrollKey = state.activeNpc == null
                ? ""
                : state.activeNpc.mapId() + ":" + state.activeNpc.sprite() + ":" + state.activeNpc.x() + ":" + state.activeNpc.y() + ":" + options.size();
        if (!scrollKey.equals(dialogOptionScrollKey)) {
            dialogOptionScroll = 0;
            dialogOptionScrollKey = scrollKey;
        }
        dialogOptionScrollBounds = new Rectangle(x, y, w, h);
        g.setColor(new Color(8, 11, 18, 136));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(93, 107, 143, 145));
        g.drawRoundRect(x, y, w, h, 8, 8);
        int rowH = 28;
        int gap = 4;
        int visibleRows = Math.max(1, (h - 14 + gap) / (rowH + gap));
        int maxScroll = Math.max(0, options.size() - visibleRows);
        dialogOptionScroll = clamp(dialogOptionScroll, 0, maxScroll);
        int rowW = maxScroll > 0 ? w - 30 : w - 16;
        int start = dialogOptionScroll;
        int end = Math.min(options.size(), start + visibleRows);
        for (int i = start; i < end; i++) {
            int rowY = y + 7 + (i - start) * (rowH + gap);
            drawDialogOptionRow(g, x + 8, rowY, rowW, rowH, i - start + 1, options.get(i));
        }
        if (maxScroll <= 0) {
            return;
        }
        int trackX = x + w - 16;
        int trackY = y + 8;
        int trackH = h - 16;
        g.setColor(new Color(35, 39, 54, 190));
        g.fillRoundRect(trackX, trackY, 6, trackH, 4, 4);
        int thumbH = Math.max(22, (int) Math.round(trackH * (visibleRows / (double) options.size())));
        int thumbTravel = Math.max(1, trackH - thumbH);
        int thumbY = trackY + (int) Math.round(dialogOptionScroll / (double) maxScroll * thumbTravel);
        g.setColor(new Color(145, 166, 214, 190));
        g.fillRoundRect(trackX, thumbY, 6, thumbH, 4, 4);
    }

    private void drawDialogueStandingSprite(Graphics2D g, String sprite, String label, int x, int y, int w, int h, Color accent, boolean drawLabel) {
        drawDialogueFigureGlow(g, x, y, w, h, accent);
        g.setColor(new Color(7, 9, 14, 90));
        g.fillOval(x + w / 6, y + h - 32, w * 2 / 3, 18);
        g.drawImage(assets.spriteFit(sprite, w, h - 28), x, y, null);
        if (!drawLabel) {
            return;
        }
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        FontMetrics metrics = g.getFontMetrics();
        int labelW = Math.min(w, metrics.stringWidth(label) + 20);
        int labelX = x + (w - labelW) / 2;
        int labelY = y + h - 24;
        g.setColor(new Color(9, 11, 17, 176));
        g.fillRoundRect(labelX, labelY, labelW, 24, 8, 8);
        g.setColor(accent);
        g.drawRoundRect(labelX, labelY, labelW, 24, 8, 8);
        g.setColor(new Color(247, 240, 220));
        drawClippedString(g, label, labelX + 10, labelY + 16, labelW - 20);
    }

    private void drawDialogueFigureGlow(Graphics2D g, int x, int y, int w, int h, Color accent) {
        Paint oldPaint = g.getPaint();
        Composite oldComposite = g.getComposite();
        float[] fractions = {0.0f, 0.58f, 1.0f};
        Color[] colors = {
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 70),
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 24),
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0)
        };
        g.setComposite(AlphaComposite.SrcOver);
        g.setPaint(new RadialGradientPaint(
                new Point(x + w / 2, y + h / 2),
                Math.max(w, h) * 0.56f,
                fractions,
                colors
        ));
        g.fillOval(x - w / 5, y + h / 10, w + w * 2 / 5, h - h / 8);
        g.setPaint(oldPaint);
        g.setComposite(oldComposite);
    }

    private String companionDialogueSprite(Npc npc) {
        if (npc == null || (npc.recruitId() == null && state.activePartyTalkActor == null && !storyDialogueNpc(npc))) {
            return null;
        }
        String sprite = npc.sprite() + "_dialogue_sprite";
        return assets.hasSprite(sprite) ? sprite : null;
    }

    private String playerDialogueSprite() {
        String sprite = state.player.sprite + "_dialogue_sprite";
        return assets.hasSprite(sprite) ? sprite : state.player.worldSprite;
    }

    private String battleActorSprite(Actor actor) {
        if (actor == null) {
            return null;
        }
        String sprite = actor.sprite + "_battle_sprite";
        return assets.hasSprite(sprite) ? sprite : actor.worldSprite;
    }

    private Color playerDialogueAccent() {
        return switch (state.player.className) {
            case "Knight" -> new Color(162, 178, 204);
            case "Mage" -> new Color(126, 111, 218);
            case "Ranger" -> new Color(111, 190, 103);
            case "Cleric" -> new Color(232, 203, 116);
            case "Rogue" -> new Color(92, 170, 120);
            default -> new Color(126, 171, 228);
        };
    }

    private Color companionAccent(Npc npc) {
        return switch (companionKey(npc)) {
            case "story_maelis" -> new Color(218, 75, 72);
            case "story_selene" -> new Color(124, 142, 218);
            case "story_odrick" -> new Color(172, 185, 198);
            case "story_solari" -> new Color(232, 183, 75);
            case "story_ysra" -> new Color(95, 178, 164);
            case "story_mirella" -> new Color(89, 154, 216);
            case "story_elder_rowan" -> new Color(126, 185, 102);
            case "story_gravekeeper_hollis" -> new Color(155, 164, 176);
            case "story_captain_elric_snowrest" -> new Color(144, 190, 220);
            case "story_bellwright_nessa" -> new Color(202, 154, 82);
            case "story_ash_scribe_damar" -> new Color(218, 103, 72);
            case "aria" -> new Color(111, 190, 103);
            case "seraphine" -> new Color(212, 77, 92);
            case "maera" -> new Color(105, 151, 227);
            case "cassia" -> new Color(220, 105, 74);
            case "lyra" -> new Color(121, 205, 185);
            case "samir" -> new Color(235, 185, 76);
            case "vesper" -> new Color(170, 220, 190);
            case "rafiq" -> new Color(188, 122, 218);
            case "calder" -> new Color(164, 133, 82);
            default -> new Color(245, 157, 73);
        };
    }

    private String companionOriginLine(Npc npc) {
        String className = state.activePartyTalkActor != null
                ? state.activePartyTalkActor.className
                : storyDialogueNpc(npc)
                ? "Main Story"
                : npc.recruitId() != null && GameData.RECRUITS.containsKey(npc.recruitId())
                ? GameData.RECRUITS.get(npc.recruitId()).className()
                : "Companion";
        String origin = switch (companionKey(npc)) {
            case "story_maelis" -> "Oathstead settlement leader";
            case "story_selene" -> "Archive City royal archivist";
            case "story_odrick" -> "Highwall roadwatch commander";
            case "story_solari" -> "Sanctum sun priestess";
            case "story_ysra" -> "Belltower Bellwarden";
            case "story_mirella" -> "Riverside noble negotiator";
            case "story_elder_rowan" -> "Oakhaven orchard elder";
            case "story_gravekeeper_hollis" -> "Stonegate gravekeeper";
            case "story_captain_elric_snowrest" -> "Snowrest frost-road captain";
            case "story_bellwright_nessa" -> "Glimmerfen bellwright";
            case "story_ash_scribe_damar" -> "Redcairn ash-scribe";
            case "aria" -> "Oakhaven road scout";
            case "seraphine" -> "Riverside contract-breaker";
            case "maera" -> "Archive star-route scholar";
            case "cassia" -> "Highwall gate veteran";
            case "lyra" -> "Belltower field healer";
            case "samir" -> "Sanctum dawn witness";
            case "vesper" -> "Snowrest winter druid";
            case "rafiq" -> "Dunewick glassstep duelist";
            case "calder" -> "Mireford bridgewright";
            default -> state.world.label(npc.mapId());
        };
        return origin + " | " + className;
    }

    private String companionPresenceLine(Npc npc) {
        return switch (companionKey(npc)) {
            case "story_maelis" -> "Mud on her cloak, counted supplies at her belt, and no patience for heroic speeches.";
            case "story_selene" -> "Dark archive robes and a sealed ledger make every careful word feel like evidence.";
            case "story_odrick" -> "Frost-scored iron and roadwatch discipline turn fear into a problem with a formation.";
            case "story_solari" -> "Gold desert vestments and ember rites make her calm feel severe rather than soft.";
            case "story_ysra" -> "Reed charms and small bronze bells move with her, as if the marsh is listening too.";
            case "story_mirella" -> "River-blue formal travel clothes and a controlled stare make politics look almost honest.";
            case "story_elder_rowan" -> "Old orchard cloth, a carved stick, and practical eyes carry village memory better than books.";
            case "story_gravekeeper_hollis" -> "A dark coat, lantern, and tired hands speak for everyone whose name was cut from stone.";
            case "story_captain_elric_snowrest" -> "Winter gear, road dust, and exhaustion make him look like a commander who counts medicine before glory.";
            case "story_bellwright_nessa" -> "Rope, hooks, and a tool belt mark her as someone who trusts maintenance more than miracles.";
            case "story_ash_scribe_damar" -> "Ash-stained robes, scrolls, and watchful eyes make every ruin feel like a pending appointment.";
            case "aria" -> "Leaf-green leathers, auburn hair, and a ready bow make her look harmless only to people who are not paying attention.";
            case "seraphine" -> "Crimson silk and black leather turn every pause into negotiation; the dagger is simply the honest part.";
            case "maera" -> "Blue moonmarked robes and a heavy satchel make forbidden knowledge look almost ceremonial.";
            case "cassia" -> "Bright hair, polished steel, and a guarded throat-scarf carry Highwall's old breach into every sentence.";
            case "lyra" -> "White cloth, teal trim, and travel-worn boots say healer first, but not helpless.";
            case "samir" -> "Gold sunbursts over white vestments make his doubt feel brighter, not weaker.";
            case "vesper" -> "Pale hair, green winter cloth, and a branch staff make her seem half-buried in snow and half-rooted under it.";
            case "rafiq" -> "Cream desert robes, a purple sash, and a curved blade give his jokes the shape of a duel.";
            case "calder" -> "Moss-brown work leathers, mud boots, and a hammer make every answer sound load-bearing.";
            default -> "Their clothes and posture say as much as their words.";
        };
    }

    private String companionKey(Npc npc) {
        if (npc == null) {
            return "";
        }
        if (npc.recruitId() != null && !npc.recruitId().isBlank()) {
            return npc.recruitId();
        }
        String sprite = npc.sprite();
        return sprite.startsWith("npc_") ? sprite.substring(4) : sprite;
    }

    private boolean storyDialogueNpc(Npc npc) {
        return npc != null && npc.sprite().startsWith("npc_story_");
    }

    private void drawDialogOptionRow(Graphics2D g, int x, int y, int w, int h, int number, DialogOption option) {
        if (option.enabled()) {
            buttons.add(new UiButton(new Rectangle(x, y, w, h), option.label(), () -> {
                option.action().run();
                repaint();
            }));
        }
        g.setColor(option.enabled() ? option.fill() : new Color(42, 44, 52));
        g.fillRoundRect(x, y, w, h, 6, 6);
        g.setColor(option.enabled() ? option.border() : new Color(73, 75, 84));
        g.drawRoundRect(x, y, w, h, 6, 6);
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(11, Math.min(13, h - 12))));
        g.setColor(option.enabled() ? new Color(238, 239, 244) : new Color(142, 146, 156));
        FontMetrics metrics = g.getFontMetrics();
        String numberLabel = number + ".";
        g.drawString(numberLabel, x + 12, y + h / 2 + 5);
        String label = option.label();
        int labelX = x + 42;
        int maxW = w - 56;
        while (metrics.stringWidth(label) > maxW && label.length() > 4) {
            label = label.substring(0, label.length() - 4) + "...";
        }
        g.drawString(label, labelX, y + h / 2 + 5);
    }

    private List<DialogOption> dialogOptions(Npc npc, Quest quest) {
        List<DialogOption> options = new ArrayList<>();
        List<String> lines = state.activeNpcDialogOptions();
        for (int i = 0; i < lines.size(); i++) {
            int index = i;
            Color fill = index == state.dialogIndex ? new Color(66, 80, 111) : new Color(35, 39, 54);
            Color border = index == state.dialogIndex ? new Color(145, 166, 214) : new Color(86, 98, 128);
            options.add(new DialogOption(dialogOptionTopic(lines.get(i), i), () -> state.selectDialogOption(index), fill, border, true));
        }
        if (state.activePartyTalkActor != null) {
            String questActionLabel = questActionLabel(quest);
            if (questActionLabel != null) {
                options.add(new DialogOption(dialogQuestActionLabel(quest, questActionLabel), state::handleActiveNpcQuestAction,
                        new Color(88, 73, 44), new Color(169, 137, 74), true));
            }
            options.add(new DialogOption("Leave", state::closeOverlay,
                    new Color(83, 61, 61), new Color(149, 96, 88), true));
            return options;
        }
        if (state.activeNpcIntroductionPending() || !state.activeNpcDialogueAtRoot()) {
            options.add(new DialogOption("Leave", state::closeOverlay,
                    new Color(83, 61, 61), new Color(149, 96, 88), true));
            return options;
        }
        String questActionLabel = questActionLabel(quest);
        if (questActionLabel != null) {
            options.add(new DialogOption(dialogQuestActionLabel(quest, questActionLabel), state::handleActiveNpcQuestAction,
                    new Color(88, 73, 44), new Color(169, 137, 74), true));
        }
        if (state.activeNpcCanTeachRecipe()) {
            options.add(new DialogOption("Ask for recipe advice", state::learnRecipeFromActiveNpc,
                    new Color(59, 72, 92), new Color(118, 148, 184), true));
        }
        if (activeDialogWillOpenShop(npc)) {
            options.add(new DialogOption("Open shop", state::openActiveShop,
                    new Color(52, 79, 92), new Color(92, 140, 160), true));
        }
        if (npc.recruitId() != null && npc.recruitCost() > 0 && !state.isRecruited(npc.recruitId())) {
            options.add(new DialogOption("Hire " + npc.recruitCost() + "g", state::hireActiveRecruit,
                    new Color(69, 62, 88), new Color(125, 107, 166), true));
        }
        if (state.canRecruitActiveNpcToParty()) {
            options.add(new DialogOption("Join party", state::recruitActiveNpcToParty,
                    new Color(69, 62, 88), new Color(125, 107, 166), true));
        }
        if (npc != null) {
            boolean canSettle = state.canRecruitActiveNpcToVillage();
            String settleLabel = canSettle
                    ? "Join Oathstead"
                    : "Oathstead trust " + state.npcRelationship(npc) + "/" + state.villageRecruitThreshold(npc);
            options.add(new DialogOption(settleLabel, state::recruitActiveNpcToVillage,
                    canSettle ? new Color(49, 84, 64) : new Color(44, 48, 56),
                    canSettle ? new Color(104, 170, 116) : new Color(80, 84, 96),
                    canSettle));
        }
        options.add(new DialogOption("Leave", state::closeOverlay,
                new Color(83, 61, 61), new Color(149, 96, 88), true));
        return options;
    }

    private String dialogOptionTopic(String line, int index) {
        int colon = line.indexOf(':');
        if (colon > 0 && colon <= 18) {
            return line.substring(0, colon);
        }
        String compact = line.replaceAll("\\s+", " ").strip();
        int sentence = compact.indexOf('.');
        if (sentence > 8 && sentence < 34) {
            compact = compact.substring(0, sentence);
        }
        if (compact.length() > 34) {
            compact = compact.substring(0, 31) + "...";
        }
        return compact.isBlank() ? "Talk " + (index + 1) : compact;
    }

    private boolean activeDialogWillOpenShop(Npc npc) {
        return state.activeShop != null || state.activeNpcCanOpenAssignedShop();
    }

    private String npcTooltip(Npc npc) {
        int count = state.npcKnowledgeCount(npc);
        int level = state.npcKnowledgeLevel(npc);
        String knowledge = "Knowledge: " + npcKnowledgeLabel(level) + " (" + count + (count == 1 ? " clue" : " clues")
                + ", INT " + state.player.intelligence + ").";
        if (!state.knowsNpc(npc)) {
            return knowledge + "\nYou have not been introduced. Talk to learn this person's name.";
        }
        List<String> parts = new ArrayList<>();
        parts.add(knowledge);
        String role = npcRoleLabel(npc);
        if (!role.isBlank()) {
            parts.add("Role: " + role + ".");
        }
        parts.add("Skills: " + npcSkillSummary(npc) + ".");
        parts.add("Equipment: " + npcEquipmentSummary(npc) + ".");
        if (npc.recruitId() != null && npc.recruitCost() > 0) {
            parts.add(state.isRecruited(npc.recruitId())
                    ? "Status: travels with you."
                    : "Contract: " + npc.recruitCost() + " gold.");
        }
        return String.join(" ", parts);
    }

    private String npcKnowledgeLabel(int level) {
        return switch (level) {
            case 0 -> "Unknown";
            case 1 -> "Introduced";
            case 2 -> "Acquainted";
            case 3 -> "Personal clues";
            case 4 -> "Trusted";
            default -> "Well known";
        };
    }

    private String npcRoleLabel(Npc npc) {
        if (npc.recruitId() != null) {
            GameData.RecruitSpec spec = GameData.RECRUITS.get(npc.recruitId());
            if (spec != null) {
                return spec.className();
            }
        }
        if (npc.shopId() != null && GameData.SHOPS.containsKey(npc.shopId())) {
            return GameData.SHOPS.get(npc.shopId()).name();
        }
        String text = (npc.name() + " " + npc.sprite() + " " + String.join(" ", npc.dialog())).toLowerCase();
        if (text.contains("smith")) {
            return "Smith";
        }
        if (text.contains("baker") || text.contains("cook")) {
            return "Cook";
        }
        if (text.contains("guard") || text.contains("captain") || text.contains("warden")) {
            return "Guard";
        }
        if (text.contains("scribe") || text.contains("archivist") || text.contains("apprentice")) {
            return "Scholar";
        }
        if (text.contains("farmer") || text.contains("hedge") || text.contains("grove")) {
            return "Forager";
        }
        return "Local resident";
    }

    private String npcSkillSummary(Npc npc) {
        List<String> parts = new ArrayList<>();
        for (Profession profession : Profession.ALL) {
            int level = npc.professionLevel(profession.id());
            if (level > 1) {
                parts.add(profession.label() + " " + level);
            }
        }
        return parts.isEmpty() ? "general survival 1" : String.join(", ", parts);
    }

    private String npcEquipmentSummary(Npc npc) {
        if (npc.recruitId() != null) {
            GameData.RecruitSpec spec = GameData.RECRUITS.get(npc.recruitId());
            if (spec != null && !spec.inventory().isEmpty()) {
                return itemList(spec.inventory(), 3);
            }
        }
        if (npc.shopId() != null) {
            Shop shop = GameData.SHOPS.get(npc.shopId());
            if (shop != null) {
                List<String> stock = shop.availableStock(state.player.level);
                if (!stock.isEmpty()) {
                    List<String> names = new ArrayList<>();
                    for (String key : stock) {
                        names.add(GameData.itemName(key));
                        if (names.size() >= 3) {
                            break;
                        }
                    }
                    return String.join(", ", names);
                }
            }
        }
        String role = npcRoleLabel(npc).toLowerCase();
        if (role.contains("cook")) {
            return "cook's tools, trail rations";
        }
        if (role.contains("smith")) {
            return "hammer, tongs, work apron";
        }
        if (role.contains("guard")) {
            return "watch cloak, sidearm";
        }
        if (role.contains("scholar")) {
            return "ledger, ink kit";
        }
        if (role.contains("forager")) {
            return "field knife, herb satchel";
        }
        return "travel cloak, belt pouch";
    }

    private String itemList(Map<String, Integer> items, int limit) {
        List<String> names = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            String count = entry.getValue() > 1 ? entry.getValue() + " " : "";
            names.add(count + GameData.itemName(entry.getKey()));
            if (names.size() >= limit) {
                break;
            }
        }
        return String.join(", ", names);
    }

    private String questActionLabel(Quest quest) {
        if (quest == null || quest.completed) {
            return null;
        }
        if (!quest.accepted) {
            return null;
        }
        if (quest.ready()) {
            return "Turn In";
        }
        return null;
    }

    private String dialogQuestActionLabel(Quest quest, String fallback) {
        if (quest == null) {
            return fallback;
        }
        if (!quest.accepted) {
            return DialogueLibrary.questCommitLabel(quest);
        }
        if (quest.ready()) {
            return quest.companionQuest() ? "Tell them what happened" : "Report back";
        }
        if (quest.companionQuest()) {
            return DialogueLibrary.questTopicLabel(quest);
        }
        return "Talk through " + quest.title;
    }

    private void drawQuestLog(Graphics2D g) {
        int panelW = Math.min(1120, Math.max(760, gameAreaWidth() - 96));
        int panelX = gameAreaCenteredX(panelW);
        int panelY = 58;
        int panelH = Math.min(780, Math.max(650, viewHeight() - 104));
        drawOverlayBase(g, panelX, panelY, panelW, panelH);

        int activeCount = questLogEntries(false).size();
        int completedCount = questLogEntries(true).size();
        List<Quest> quests = questLogEntries(questLogCompletedTab);
        normalizeSelectedQuestLogId(quests);

        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(244, 239, 220));
        g.drawString("Quest Log", panelX + 36, panelY + 46);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(new Color(190, 196, 211));
        g.drawString("Accepted work, field notes, rewards, and where the next step lives.", panelX + 36, panelY + 72);

        int tabY = panelY + 92;
        drawQuestLogTab(g, panelX + 36, tabY, 148, "Active", activeCount, !questLogCompletedTab);
        drawQuestLogTab(g, panelX + 194, tabY, 166, "Completed", completedCount, questLogCompletedTab);
        actionButton(g, panelX + panelW - 122, tabY, 86, 30, "Close", state::toggleQuestLog,
                new Color(58, 72, 100), new Color(107, 126, 166), true);

        int contentY = panelY + 134;
        int contentH = panelH - 190;
        int listW = Math.min(410, Math.max(340, panelW * 38 / 100));
        int listX = panelX + 36;
        int detailX = listX + listW + 24;
        int detailW = panelX + panelW - 36 - detailX;
        drawQuestLogList(g, quests, listX, contentY, listW, contentH);
        drawQuestLogDetails(g, selectedQuestLogQuest(quests), detailX, contentY, detailW, contentH);

        g.setColor(new Color(196, 198, 205));
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.drawString("Q or Esc close", panelX + 40, panelY + panelH - 24);
        g.drawString("Sorted by tracked, ready, story, companion, progress", detailX, panelY + panelH - 24);
    }

    private void drawQuestLogTab(Graphics2D g, int x, int y, int w, String label, int count, boolean active) {
        actionButton(g, x, y, w, 30, label + " (" + count + ")", () -> {
            questLogCompletedTab = "Completed".equals(label);
            questLogScroll = 0;
            selectedQuestLogId = "";
        }, active ? new Color(79, 90, 60) : new Color(46, 54, 72),
                active ? new Color(139, 154, 96) : new Color(91, 103, 132), true);
    }

    private void drawQuestLogList(Graphics2D g, List<Quest> quests, int x, int y, int w, int h) {
        g.setColor(new Color(9, 12, 18, 210));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(74, 82, 105));
        g.drawRoundRect(x, y, w, h, 8, 8);

        if (quests.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 15));
            g.setColor(new Color(205, 210, 222));
            String empty = questLogCompletedTab ? "No completed quests yet." : "No active quests yet.";
            drawWrapped(g, empty, x + 18, y + 34, w - 36, 18, 2);
            return;
        }

        int rowH = 86;
        int gap = 10;
        int visibleRows = Math.max(1, (h - 28) / (rowH + gap));
        int maxScroll = Math.max(0, quests.size() - visibleRows);
        questLogScroll = clamp(questLogScroll, 0, maxScroll);
        int start = Math.min(questLogScroll, Math.max(0, quests.size() - 1));
        int end = Math.min(quests.size(), start + visibleRows);
        int rowY = y + 14;
        for (int i = start; i < end; i++) {
            drawQuestLogRow(g, quests.get(i), x + 12, rowY, w - 24, rowH);
            rowY += rowH + gap;
        }
        if (maxScroll > 0) {
            int trackX = x + w - 10;
            int trackY = y + 14;
            int trackH = h - 28;
            int thumbH = Math.max(24, trackH * visibleRows / Math.max(visibleRows + maxScroll, 1));
            int thumbTravel = Math.max(1, trackH - thumbH);
            int thumbY = trackY + (int) Math.round(questLogScroll / (double) maxScroll * thumbTravel);
            g.setColor(new Color(38, 43, 58));
            g.fillRoundRect(trackX, trackY, 4, trackH, 4, 4);
            g.setColor(new Color(126, 141, 184));
            g.fillRoundRect(trackX, thumbY, 4, thumbH, 4, 4);
        }
    }

    private void drawQuestLogRow(Graphics2D g, Quest quest, int x, int y, int w, int h) {
        Color accent = questAccent(quest);
        boolean selected = quest.id.equals(selectedQuestLogId);
        Rectangle bounds = new Rectangle(x, y, w, h);
        buttons.add(new UiButton(bounds, "quest-log-row:" + quest.id, () -> {
            selectedQuestLogId = quest.id;
            repaint();
        }));
        boolean hovered = hoverPoint != null && bounds.contains(hoverPoint);

        Color fill = selected ? new Color(28, 34, 46, 238) : new Color(14, 17, 25, 218);
        if (hovered) {
            fill = blend(fill, Color.WHITE, 0.08);
        }
        g.setColor(fill);
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(selected ? accent : new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 145));
        g.drawRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), selected ? 90 : 50));
        g.fillRect(x + 1, y + 1, 5, h - 2);

        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.setColor(accent);
        drawWrapped(g, quest.title, x + 14, y + 22, w - 126, 16, 1);
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.setColor(new Color(226, 229, 238));
        drawRightAligned(g, isTrackedQuest(quest) ? "Tracked" : shortQuestTypeLabel(quest), x + w - 12, y + 21);

        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(191, 198, 212));
        drawWrapped(g, questStatusLine(quest), x + 14, y + 43, w - 28, 14, 1);
        drawQuestProgressBar(g, quest, x + 14, y + h - 24, Math.min(190, w - 28), accent);
        g.setColor(new Color(171, 179, 196));
        drawRightAligned(g, quest.progress + "/" + quest.activeNeeded(), x + w - 14, y + h - 14);
    }

    private void drawQuestLogDetails(Graphics2D g, Quest quest, int x, int y, int w, int h) {
        g.setColor(new Color(9, 12, 18, 222));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(74, 82, 105));
        g.drawRoundRect(x, y, w, h, 8, 8);
        if (quest == null) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 15));
            g.setColor(new Color(205, 210, 222));
            drawWrapped(g, "Select a quest to view field notes, giver context, dialogue, rewards, and next steps.",
                    x + 22, y + 34, w - 44, 18, 4);
            return;
        }

        Shape oldClip = g.getClip();
        g.setClip(new Rectangle(x + 1, y + 1, w - 2, h - 2));
        Color accent = questAccent(quest);
        int contentX = x + 24;
        int contentW = w - 48;
        int cursorY = y + 34;
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.setColor(accent);
        drawWrapped(g, quest.title, contentX, cursorY, contentW - 130, 24, 2);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(new Color(228, 232, 241));
        drawRightAligned(g, questTypeLabel(quest), x + w - 24, cursorY - 2);
        cursorY += 42;

        if (!quest.completed) {
            String trackLabel = isTrackedQuest(quest) ? "Untrack" : "Track";
            actionButton(g, x + w - 116, y + 46, 92, 28, trackLabel, () -> toggleTrackedQuest(quest),
                    isTrackedQuest(quest) ? new Color(86, 69, 50) : new Color(45, 60, 78),
                    isTrackedQuest(quest) ? new Color(188, 147, 74) : new Color(103, 132, 166), true);
        }

        int detailProgressW = Math.min(260, Math.max(150, contentW / 2));
        drawQuestProgressBar(g, quest, contentX, cursorY, detailProgressW, accent);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(232, 235, 242));
        if (contentW >= 430) {
            g.drawString(questStatusLine(quest), contentX + detailProgressW + 18, cursorY + 11);
            cursorY += 42;
        } else {
            drawWrapped(g, questStatusLine(quest), contentX, cursorY + 32, contentW, 16, 2);
            cursorY += 64;
        }

        drawQuestDetailTab(g, contentX, cursorY, 86, "Brief", !questDetailTimelineTab);
        drawQuestDetailTab(g, contentX + 96, cursorY, 108, "Dialogue", questDetailTimelineTab);
        cursorY += 46;

        if (questDetailTimelineTab) {
            drawQuestDialogueTimeline(g, quest, contentX, cursorY, contentW, y + h - 24);
        } else {
            cursorY = drawQuestDetailSection(g, "Giver", questGiverDetail(quest), contentX, cursorY, contentW, 3);
            cursorY = drawQuestDetailSection(g, "Context", questContextDetail(quest), contentX, cursorY, contentW, 5);
            cursorY = drawQuestDetailSection(g, "Current Step", questObjectiveDetail(quest) + " " + questLocationHint(quest),
                    contentX, cursorY, contentW, 4);
            drawQuestDetailSection(g, "Reward", quest.rewardGold + " gold, " + quest.rewardXp + " XP.", contentX, cursorY, contentW, 2);
        }
        g.setClip(oldClip);
    }

    private void drawQuestDetailTab(Graphics2D g, int x, int y, int w, String label, boolean active) {
        actionButton(g, x, y, w, 30, label, () -> {
            questDetailTimelineTab = "Dialogue".equals(label);
            repaint();
        }, active ? new Color(79, 90, 60) : new Color(46, 54, 72),
                active ? new Color(139, 154, 96) : new Color(91, 103, 132), true);
    }

    private void drawQuestDialogueTimeline(Graphics2D g, Quest quest, int x, int y, int w, int bottom) {
        int rowY = y;
        rowY = drawQuestTimelineStep(g, quest, x, rowY, w, bottom, "Accepted",
                cleanQuestDialogueLine(quest, quest.activeStartDialog()), quest.accepted || quest.completed,
                !quest.completed && !quest.ready() && quest.progress == 0);
        rowY = drawQuestTimelineStep(g, quest, x, rowY, w, bottom, "In Progress",
                cleanQuestDialogueLine(quest, quest.activeProgressDialog()), quest.progress > 0 || quest.ready() || quest.completed,
                !quest.completed && !quest.ready() && quest.progress > 0);
        rowY = drawQuestTimelineStep(g, quest, x, rowY, w, bottom, "Ready",
                cleanQuestDialogueLine(quest, quest.activeReadyDialog()), quest.ready() || quest.completed,
                quest.ready());
        drawQuestTimelineStep(g, quest, x, rowY, w, bottom, "Completed",
                cleanQuestDialogueLine(quest, quest.activeCompleteDialog()), quest.completed, quest.completed);
    }

    private int drawQuestTimelineStep(
            Graphics2D g,
            Quest quest,
            int x,
            int y,
            int w,
            int bottom,
            String label,
            String body,
            boolean available,
            boolean current
    ) {
        if (y + 40 > bottom) {
            return y;
        }
        Color accent = current ? questAccent(quest) : available ? new Color(144, 215, 150) : new Color(100, 108, 128);
        int rowH = 84;
        g.setColor(current ? new Color(26, 32, 43, 230) : new Color(13, 16, 23, 205));
        g.fillRoundRect(x, y, w, rowH, 8, 8);
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), current ? 210 : 125));
        g.drawRoundRect(x, y, w, rowH, 8, 8);
        int dotX = x + 16;
        int dotY = y + 22;
        g.setColor(accent);
        g.fillOval(dotX - 5, dotY - 5, 10, 10);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(available ? new Color(238, 240, 246) : new Color(145, 151, 166));
        g.drawString(label + (current ? "  Current" : available ? "  Logged" : "  Pending"), x + 34, y + 26);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(available ? new Color(204, 211, 225) : new Color(126, 133, 150));
        drawQuestDetailText(g, available ? body : "This note will unlock when the quest reaches this state.",
                x + 34, y + 48, w - 50, 15, 2);
        return y + rowH + 10;
    }

    private int drawQuestDetailSection(Graphics2D g, String heading, String body, int x, int y, int w, int maxBodyLines) {
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(245, 214, 117));
        g.drawString(heading, x, y);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(204, 211, 225));
        int bodyY = y + 19;
        int drawn = drawQuestDetailText(g, body, x, bodyY, w, 16, maxBodyLines);
        return bodyY + drawn * 16 + 16;
    }

    private int drawQuestDetailText(Graphics2D g, String text, int x, int y, int width, int lineHeight, int maxLines) {
        FontMetrics metrics = g.getFontMetrics();
        List<String> lines = new ArrayList<>();
        String source = text == null || text.isBlank() ? "No notes recorded." : text.strip();
        for (String paragraph : source.split("\\R", -1)) {
            if (paragraph.isBlank()) {
                lines.add("");
            } else {
                lines.addAll(wrappedTooltipLines(metrics, paragraph, width));
            }
        }
        int count = Math.min(lines.size(), Math.max(1, maxLines));
        for (int i = 0; i < count; i++) {
            String line = lines.get(i);
            if (i == count - 1 && lines.size() > count) {
                line = fitWithEllipsis(metrics, line, width);
            }
            g.drawString(line, x, y + i * lineHeight);
        }
        return count;
    }

    List<Quest> questLogEntries(boolean completed) {
        List<Quest> entries = new ArrayList<>();
        for (Quest quest : state.quests.values()) {
            if (completed) {
                if (quest.completed) {
                    entries.add(quest);
                }
            } else if (quest.accepted && !quest.completed) {
                entries.add(quest);
            }
        }
        entries.sort(completed ? completedQuestComparator() : activeQuestComparator());
        return entries;
    }

    private Comparator<Quest> activeQuestComparator() {
        return Comparator
                .comparing((Quest quest) -> !isTrackedQuest(quest))
                .thenComparing(quest -> !quest.ready())
                .thenComparingInt(this::questTypeSortRank)
                .thenComparing((Quest quest) -> questProgressRatio(quest), Comparator.reverseOrder())
                .thenComparing(quest -> state.questReturnLocation(quest.id))
                .thenComparing(quest -> quest.title);
    }

    private Comparator<Quest> completedQuestComparator() {
        return Comparator
                .comparingInt(this::questTypeSortRank)
                .thenComparing(quest -> state.questReturnLocation(quest.id))
                .thenComparing(quest -> quest.title);
    }

    private int questTypeSortRank(Quest quest) {
        if (quest.mainStoryQuest()) {
            return 0;
        }
        if (quest.companionQuest()) {
            return 1;
        }
        return 2;
    }

    private double questProgressRatio(Quest quest) {
        if (quest.activeNeeded() <= 0) {
            return 1.0;
        }
        return Math.max(0.0, Math.min(1.0, quest.progress / (double) quest.activeNeeded()));
    }

    private void normalizeSelectedQuestLogId(List<Quest> quests) {
        boolean found = false;
        for (Quest quest : quests) {
            if (quest.id.equals(selectedQuestLogId)) {
                found = true;
                break;
            }
        }
        if (!found) {
            selectedQuestLogId = quests.isEmpty() ? "" : quests.get(0).id;
            questLogScroll = 0;
        }
    }

    Quest selectedQuestLogQuest(List<Quest> quests) {
        for (Quest quest : quests) {
            if (quest.id.equals(selectedQuestLogId)) {
                return quest;
            }
        }
        return quests.isEmpty() ? null : quests.get(0);
    }

    private Color questAccent(Quest quest) {
        return quest.completed ? new Color(144, 215, 150)
                : quest.mainStoryQuest() ? new Color(239, 83, 80)
                : quest.companionQuest() ? new Color(245, 157, 73)
                : new Color(164, 211, 255);
    }

    private void drawTrackedQuestHud(Graphics2D g) {
        Quest quest = trackedQuest();
        if (quest == null) {
            return;
        }
        int w = Math.min(380, Math.max(280, gameAreaWidth() - 48));
        int x = 24;
        int y = 24;
        int h = 100;
        Color accent = questAccent(quest);
        g.setColor(new Color(8, 11, 17, 218));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 185));
        g.drawRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 55));
        g.fillRect(x + 1, y + 1, 5, h - 2);

        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(new Color(245, 214, 117));
        g.drawString("Tracked Quest", x + 14, y + 22);
        actionButton(g, x + w - 76, y + 12, 58, 26, "Open", () -> {
            questLogCompletedTab = false;
            selectedQuestLogId = quest.id;
            questLogScroll = 0;
            state.toggleQuestLog();
        }, new Color(45, 60, 78), new Color(103, 132, 166), true);

        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.setColor(accent);
        drawWrapped(g, quest.title, x + 14, y + 44, w - 100, 16, 1);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(204, 211, 225));
        drawWrapped(g, quest.ready() ? questLocationHint(quest) : questStatusLine(quest), x + 14, y + 65, w - 28, 14, 1);
        drawQuestProgressBar(g, quest, x + 14, y + h - 22, Math.min(210, w - 110), accent);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(new Color(220, 225, 236));
        drawRightAligned(g, quest.progress + "/" + quest.activeNeeded(), x + w - 16, y + h - 12);
    }

    private void drawTravelPartyHud(Graphics2D g, int x, int y, int w, int h) {
        List<Actor> party = state.partyMembers();
        if (party.isEmpty()) {
            return;
        }
        g.setColor(new Color(8, 11, 17, 214));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(82, 92, 116, 190));
        g.drawRoundRect(x, y, w, h, 8, 8);

        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(230, 225, 206));
        g.drawString("Travel", x + 10, y + 18);

        List<GameState.WorldAbilityOption> options = state.worldAbilityOptions();
        int buttonY = y + 26;
        int buttonGap = 6;
        int columns = 2;
        int buttonW = (w - 20 - buttonGap) / columns;
        int buttonH = 34;
        int visible = Math.min(4, options.size());
        for (int i = 0; i < visible; i++) {
            GameState.WorldAbilityOption option = options.get(i);
            int col = i % columns;
            int row = i / columns;
            int index = i;
            drawWorldAbilityButton(g, option, index,
                    x + 10 + col * (buttonW + buttonGap),
                    buttonY + row * (buttonH + buttonGap),
                    buttonW,
                    buttonH);
        }
        if (visible == 0) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.setColor(new Color(145, 154, 174));
            drawWrapped(g, "Prepare travel-ready abilities in the loadout tab.", x + 10, buttonY + 16, w - 20, 15, 2);
        }

        int chipX = x + 10;
        int chipY = buttonY + buttonH * 2 + buttonGap + 4;
        for (String effect : List.of("travel_speed", "gather_focus", "encounter_ward", "encounter_lure", "battle_advantage")) {
            int remaining = state.worldAbilityRemaining(effect);
            if (remaining <= 0) {
                continue;
            }
            String label = state.worldAbilityLabel(effect) + " " + remaining;
            int chipW = Math.min(w - 20, Math.max(72, g.getFontMetrics().stringWidth(label) + 18));
            if (chipX + chipW > x + w - 10) {
                break;
            }
            g.setColor(new Color(38, 58, 50, 225));
            g.fillRoundRect(chipX, chipY, chipW, 20, 7, 7);
            g.setColor(new Color(130, 207, 153));
            g.drawRoundRect(chipX, chipY, chipW, 20, 7, 7);
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            g.setColor(new Color(235, 236, 240));
            drawCenteredIn(g, label, chipX, chipY + 14, chipW);
            chipX += chipW + 6;
        }

        int memberY = chipY + 26;
        int memberAreaH = Math.max(72, y + h - memberY - 10);
        int members = Math.max(1, party.size());
        int memberGap = 6;
        int memberCols = members == 1 ? 1 : 2;
        int memberRows = (members + memberCols - 1) / memberCols;
        int memberW = (w - 20 - memberGap * (memberCols - 1)) / memberCols;
        int memberH = Math.max(32, (memberAreaH - memberGap * (memberRows - 1)) / memberRows);
        for (int i = 0; i < party.size(); i++) {
            Actor actor = party.get(i);
            int col = i % memberCols;
            int row = i / memberCols;
            int memberX = x + 10 + col * (memberW + memberGap);
            drawTravelPartyMember(g, actor, memberX, memberY + row * (memberH + memberGap), memberW, memberH, members == 1);
        }
    }

    private void drawWorldAbilityButton(Graphics2D g, GameState.WorldAbilityOption option, int index,
                                        int x, int y, int w, int h) {
        boolean enabled = option.actor().mp >= option.ability().cost();
        Rectangle bounds = new Rectangle(x, y, w, h);
        if (enabled) {
            buttons.add(new UiButton(bounds, "world-ability:" + index, () -> {
                state.useWorldAbility(index);
                repaint();
            }));
        }
        boolean hovered = enabled && hoverPoint != null && bounds.contains(hoverPoint);
        Color accent = worldAbilityAccent(option.effectKey());
        g.setColor(enabled ? new Color(35, 43, 58, 232) : new Color(42, 44, 52, 210));
        if (hovered) {
            g.setColor(blend(new Color(35, 43, 58, 232), accent, 0.22));
        }
        g.fillRoundRect(x, y, w, h, 7, 7);
        g.setColor(enabled ? accent : new Color(73, 75, 84));
        g.drawRoundRect(x, y, w, h, 7, 7);
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.setColor(enabled ? new Color(235, 236, 240) : new Color(145, 154, 174));
        drawClippedString(g, (index + 1) + " " + option.ability().name(), x + 7, y + 15, w - 14);
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.setColor(enabled ? new Color(197, 205, 221) : new Color(118, 124, 138));
        drawClippedString(g, shortWorldAbilityLabel(option.label()) + " - " + shortText(option.actor().name, 8),
                x + 7, y + 30, w - 14);
        tooltipZones.add(new TooltipZone(bounds, option.ability().name(),
                option.label() + "\n" + option.description(), abilityIconName(option.ability()), accent));
    }

    private Color worldAbilityAccent(String effectKey) {
        return switch (effectKey) {
            case "travel_speed" -> new Color(116, 203, 151);
            case "gather_focus" -> new Color(151, 184, 98);
            case "encounter_ward" -> new Color(125, 166, 246);
            case "encounter_lure" -> new Color(232, 126, 92);
            case "battle_advantage" -> new Color(237, 185, 104);
            default -> new Color(103, 132, 166);
        };
    }

    private String shortWorldAbilityLabel(String label) {
        return switch (label) {
            case "Swift Travel" -> "Speed";
            case "Gather Focus" -> "Forage";
            case "Quiet Road" -> "Quiet";
            case "Challenge Call" -> "Lure";
            case "Opening Advantage" -> "Edge";
            default -> label;
        };
    }

    private void drawTravelPartyMember(Graphics2D g, Actor actor, int x, int y, int w, int h, boolean large) {
        Rectangle bounds = new Rectangle(x, y, w, h);
        boolean compact = !large && h < 44;
        g.setColor(new Color(24, 28, 38, 232));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(82, 92, 116));
        g.drawRoundRect(x, y, w, h, 8, 8);
        partyPortraitZones.add(new PartyPortraitZone(bounds, actor));
        int portraitW = large ? Math.min(76, Math.max(50, w / 3)) : compact ? Math.min(42, Math.max(34, w / 3 + 4)) : Math.min(54, Math.max(42, w / 3 + 8));
        int portraitH = Math.max(compact ? 28 : 36, h + (large ? 4 : compact ? -4 : 4));
        int portraitY = y + Math.max(2, h - portraitH - 2);
        g.drawImage(assets.spriteFit(dialoguePortraitSprite(actor), portraitW, portraitH), x + 4, portraitY, null);
        int textX = x + portraitW + 10;
        int textW = Math.max(34, w - portraitW - 16);
        g.setFont(new Font("SansSerif", Font.BOLD, large ? 12 : compact ? 9 : 10));
        g.setColor(new Color(235, 236, 240));
        drawClippedString(g, actor.name, textX, y + (large ? 18 : compact ? 13 : 14), textW);
        int barY = y + (large ? 28 : compact ? 18 : 20);
        int barH = large ? 9 : compact ? 6 : 8;
        int barGap = large ? 14 : compact ? 9 : 13;
        drawMiniResourceBar(g, textX, barY, textW, barH, actor.hp, actor.maxHp, new Color(205, 85, 101));
        drawMiniResourceBar(g, textX, barY + barGap, textW, barH, actor.mp, actor.maxMp, new Color(91, 137, 214));
        tooltipZones.add(new TooltipZone(bounds, actor.name, partyMemberTooltip(actor),
                "sprite:" + dialoguePortraitSprite(actor), new Color(126, 154, 220)));
    }

    private String partyMemberTooltip(Actor actor) {
        actor.sanitizeAbilityLoadout();
        String prepared = actor.activeAbilities().isEmpty()
                ? "none"
                : String.join(", ", actor.activeAbilities().stream().map(Ability::name).toList());
        return "HP " + actor.hp + "/" + actor.maxHp + "   MP " + actor.mp + "/" + actor.maxMp
                + "\nATK " + actor.attack + "   Armor " + actor.defense + "   DR " + actor.damageReductionBonus + "%"
                + "\nSTR " + actor.strength + "   INT " + actor.intelligence + "   DEX " + actor.dexterity
                + "\nCON " + actor.constitution + "   WIL " + actor.willpower + "   CHA " + actor.charisma
                + "\nCrit " + percent(actor.criticalChanceAgainst(null, null))
                + "   Crit Dmg " + Math.round(actor.criticalMultiplier(null) * 100.0) + "%"
                + "\nDodge " + percent(actor.dodgeChanceAgainst(null))
                + "   Parry " + percent(actor.parryChanceAgainst(null))
                + "\nPrepared: " + prepared;
    }

    private String percent(double chance) {
        return Math.round(chance * 100.0) + "%";
    }

    private String dialoguePortraitSprite(Actor actor) {
        if (actor == null) {
            return state.player.worldSprite;
        }
        String sprite = actor.sprite + "_dialogue_sprite";
        return assets.hasSprite(sprite) ? sprite : actor.worldSprite;
    }

    private void drawMiniResourceBar(Graphics2D g, int x, int y, int w, int h, int value, int max, Color fill) {
        g.setColor(new Color(40, 44, 56));
        g.fillRoundRect(x, y, w, h, 4, 4);
        int filled = max <= 0 ? 0 : Math.max(1, Math.min(w, (int) Math.round(w * value / (double) max)));
        g.setColor(fill);
        g.fillRoundRect(x, y, filled, h, 4, 4);
    }

    private Quest trackedQuest() {
        if (trackedQuestId.isBlank()) {
            return null;
        }
        Quest quest = state.quests.get(trackedQuestId);
        if (quest == null || !quest.accepted || quest.completed) {
            trackedQuestId = "";
            return null;
        }
        return quest;
    }

    private boolean isTrackedQuest(Quest quest) {
        return quest != null && quest.id.equals(trackedQuestId) && quest.accepted && !quest.completed;
    }

    void toggleTrackedQuest(Quest quest) {
        if (quest == null || quest.completed) {
            return;
        }
        trackedQuestId = isTrackedQuest(quest) ? "" : quest.id;
        state.status = trackedQuestId.isBlank() ? "Quest tracking cleared." : "Tracking quest: " + quest.title + ".";
    }

    private void drawQuestProgressBar(Graphics2D g, Quest quest, int x, int y, int w, Color fill) {
        int h = 12;
        g.setColor(new Color(35, 39, 54));
        g.fillRoundRect(x, y, w, h, 5, 5);
        int filled = quest.activeNeeded() <= 0 ? w : (int) Math.round(w * Math.max(0.0, Math.min(1.0, quest.progress / (double) quest.activeNeeded())));
        g.setColor(fill);
        g.fillRoundRect(x, y, filled, h, 5, 5);
        g.setColor(new Color(78, 88, 116));
        g.drawRoundRect(x, y, w, h, 5, 5);
    }

    private String questTypeLabel(Quest quest) {
        if (quest.completed) {
            return "Completed";
        }
        if (quest.ready()) {
            return "Ready to turn in";
        }
        if (quest.mainStoryQuest()) {
            return "Main story";
        }
        if (quest.companionQuest()) {
            return "Companion";
        }
        return "Side quest";
    }

    private String shortQuestTypeLabel(Quest quest) {
        if (quest.completed) {
            return "Done";
        }
        if (quest.ready()) {
            return "Ready";
        }
        if (quest.mainStoryQuest()) {
            return "Story";
        }
        if (quest.companionQuest()) {
            return "Ally";
        }
        return "Side";
    }

    private void drawRightAligned(Graphics2D g, String text, int rightX, int baselineY) {
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, rightX - metrics.stringWidth(text), baselineY);
    }

    private String questStatusLine(Quest quest) {
        String stage = quest.stagedQuest() ? "Stage " + (quest.stageIndex + 1) + "/" + quest.stages.size() + " - " : "";
        return stage + quest.objectiveAction() + " " + quest.activeTarget() + "  " + quest.progress + "/" + quest.activeNeeded();
    }

    private String questObjectiveDetail(Quest quest) {
        if (quest.completed) {
            return "Finished with " + state.questGiverName(quest.id) + ". The reward has been claimed.";
        }
        int remaining = Math.max(0, quest.activeNeeded() - quest.progress);
        return switch (quest.activeObjectiveKind()) {
            case DEFEAT -> remaining + " visible " + quest.activeTarget() + " target" + (remaining == 1 ? "" : "s")
                    + " now roam the overworld. Engage them from the map or by walking into them.";
            case RESCUE -> remaining + " threatened " + quest.activeTarget() + " encounter" + (remaining == 1 ? "" : "s")
                    + " remain. Reach the marked danger and win the fight before returning.";
            case DEFEND -> "Hold " + remaining + " more " + quest.activeTarget() + " pressure point" + (remaining == 1 ? "" : "s")
                    + " by defeating the attackers at the marked site.";
            case GATHER -> "Gather " + remaining + " more " + quest.activeTarget() + " from the marked field objects.";
            case DELIVER -> "Deliver " + quest.activeTarget() + " through dialogue with the marked recipient.";
            case VISIT -> "Inspect " + remaining + " marked " + quest.activeTarget() + " point" + (remaining == 1 ? "" : "s") + ".";
            case SEARCH -> "Search " + remaining + " marked " + quest.activeTarget() + " clue" + (remaining == 1 ? "" : "s") + ".";
            case TALK -> "Talk with " + quest.activeTarget() + " and ask the question directly.";
            case ASK_AROUND -> "Ask " + remaining + " more local " + (remaining == 1 ? "person" : "people") + " about " + quest.activeTarget() + ".";
            case REPORT -> "Report what you learned to " + quest.activeTarget() + ".";
            case ESCORT -> "Reach " + remaining + " marked " + quest.activeTarget() + " waypoint" + (remaining == 1 ? "" : "s") + " safely.";
            case CHOICE -> "Resolve the choice about " + quest.activeTarget() + " through dialogue.";
        };
    }

    private String questLocationHint(Quest quest) {
        if (quest.ready()) {
            return "Return to " + state.questGiverName(quest.id) + " in " + state.questReturnLocation(quest.id) + ".";
        }
        if (quest.activeObjectiveKind().combatObjective()) {
            return "Hunting ground: within riding distance of " + state.questReturnLocation(quest.id)
                    + ", favoring the creature's natural biome.";
        }
        if (quest.hasWorldObjective()) {
            return "Marked on the world map and vicinity map.";
        }
        return "Bring the required item back to " + state.questGiverName(quest.id) + ".";
    }

    private String questGiverDetail(Quest quest) {
        String giver = state.questGiverName(quest.id);
        String location = state.questReturnLocation(quest.id);
        String type = quest.mainStoryQuest() ? "Main story contact"
                : quest.companionQuest() ? "Companion request"
                : "Local request";
        String chain = quest.chainOwnerId == null || quest.chainOwnerId.isBlank()
                ? ""
                : " Arc: " + readableId(quest.chainOwnerId) + ".";
        return giver + " in " + location + ". " + type + "." + chain;
    }

    private String questContextDetail(Quest quest) {
        String map = quest.activeObjectiveMapId() == null || quest.activeObjectiveMapId().isBlank()
                ? state.questReturnLocation(quest.id)
                : state.world.label(quest.activeObjectiveMapId());
        String place = quest.activeObjectiveLocationKind() == null || quest.activeObjectiveLocationKind().isBlank()
                ? ""
                : " The work points toward " + readableId(quest.activeObjectiveLocationKind()) + ".";
        String outcome = state.questBranchOutcome(quest);
        String branch = outcome.isBlank()
                ? ""
                : " Remembered choice: " + state.readableQuestOutcome(outcome) + ".";
        return quest.description + " " + questFlavorText(quest) + " Objective area: " + map + "." + place + branch;
    }

    private String questDialogueDetail(Quest quest) {
        if (quest.completed) {
            return cleanQuestDialogueLine(quest, quest.activeCompleteDialog());
        }
        if (quest.ready()) {
            return cleanQuestDialogueLine(quest, quest.activeReadyDialog());
        }
        if (quest.progress > 0) {
            return cleanQuestDialogueLine(quest, quest.activeProgressDialog());
        }
        return cleanQuestDialogueLine(quest, quest.activeStartDialog());
    }

    private String cleanQuestDialogueLine(Quest quest, String line) {
        if (line == null || line.isBlank()) {
            return "No dialogue note recorded.";
        }
        String cleaned = line.strip();
        String giver = state.questGiverName(quest.id);
        String prefix = giver + ":";
        if (cleaned.startsWith(prefix)) {
            cleaned = cleaned.substring(prefix.length()).strip();
        }
        return cleaned;
    }

    private String readableId(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String[] parts = value.replace('-', '_').split("_+");
        List<String> words = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            words.add(part.substring(0, 1).toUpperCase() + part.substring(1).toLowerCase());
        }
        return String.join(" ", words);
    }

    private String questFlavorText(Quest quest) {
        String stake = quest.mainStoryQuest()
                ? "The kingdoms will measure this by what it proves, not by how tidy it looks."
                : quest.companionQuest()
                ? "This is personal work; the reward is only the part written down."
                : "Local trouble grows teeth when everyone waits for someone else.";
        String texture = switch (quest.activeObjectiveKind()) {
            case DEFEAT -> quest.activeTarget() + " sightings have turned from rumor into route planning.";
            case RESCUE -> "Someone is alive only while the road is reached in time.";
            case DEFEND -> "Holding ground can matter as much as taking it.";
            case GATHER -> "Small supplies decide whether people travel, heal, and eat.";
            case DELIVER -> "A carried thing can change hands more cleanly than a rumor can.";
            case VISIT -> "Old marks and quiet places keep better records than frightened witnesses.";
            case SEARCH -> "The truth is present, but it has learned to hide in ordinary details.";
            case TALK -> "Some answers only open when a person is asked plainly.";
            case ASK_AROUND -> "Rumor becomes useful when enough separate mouths point the same way.";
            case REPORT -> "News has weight only when it reaches someone able to act.";
            case ESCORT -> "A safe arrival can be the whole victory.";
            case CHOICE -> "What matters now is not only what happened, but what the player chooses to carry forward.";
        };
        return stake + " " + texture;
    }

    private void drawSkillTree(Graphics2D g) {
        int x = 18;
        int y = 18;
        int w = Math.max(760, gameAreaWidth() - 36);
        int h = Math.max(620, viewHeight() - 36);
        drawOverlayBase(g, x, y, w, h);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(244, 239, 220));
        g.drawString("Skills", x + 36, y + 48);
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g.setColor(new Color(210, 213, 222));
        g.drawString(state.player.className + "  Level " + state.player.level + "  Skill " + state.player.skillPoints
                + "   Stat " + state.player.statPoints + "   Profession " + state.player.professionSkillPoints
                + "   Wheel scrolls the active tree", x + 36, y + 78);

        drawSkillTabs(g, x + 36, y + 94, 148, false);
        drawStatAllocator(g, state.player, x + 598, y + 98);
        if (activeSkillTab == SkillTab.LOADOUT) {
            drawAbilityLoadoutTab(g, state.player, x + 36, y + 142, w - 72, h - 222);
            drawAbilityDragGhost(g);
        } else if (activeSkillTab == SkillTab.PROFESSIONS) {
            drawSkillGraph(g, state.player, activeSkillTitle(state.player), activeSkillTree(state.player),
                    x + 36, y + 142, Math.max(520, w - 478), h - 222, false);
            drawProfessionColumn(g, state.player, x + w - 376, y + 142, 340, h - 222);
        } else {
            drawSkillGraph(g, state.player, activeSkillTitle(state.player), activeSkillTree(state.player),
                    x + 36, y + 142, w - 72, h - 222, false);
        }

        int spent = SkillTrees.spent(state.player.skillAllocations);
        int respecCost = SkillTrees.respecCost(state.player.level, spent);
        boolean canRespec = spent > 0 && state.player.gold >= respecCost;
        actionButton(g, x + w - 330, y + h - 54, 154, 34, "Respec " + respecCost + "g", state::respecSkills, new Color(96, 66, 59), new Color(165, 103, 88), canRespec);
        actionButton(g, x + w - 160, y + h - 54, 120, 34, "Close", state::toggleSkills, new Color(58, 72, 100), new Color(107, 126, 166), true);
    }

    private void drawProfessionColumn(Graphics2D g, Actor actor, int x, int y, int w, int h) {
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.setColor(new Color(246, 224, 151));
        g.drawString("Professions", x, y);
        int rowY = y + 26;
        for (Profession profession : Profession.ALL) {
            boolean selected = profession.id().equals(activeProfessionId);
            int level = actor.professionLevel(profession.id());
            int cap = actor.professionLevelCap(profession.id());
            int xp = actor.professionXp.getOrDefault(profession.id(), 0);
            int current = Math.max(0, xp - Profession.xpForLevel(level, cap));
            int needed = level >= cap ? 1 : Profession.xpToNext(level);
            g.setColor(selected ? new Color(45, 58, 48, 238) : new Color(28, 32, 43, 232));
            g.fillRoundRect(x, rowY, w, 46, 8, 8);
            g.setColor(selected ? new Color(132, 157, 104) : new Color(96, 106, 133));
            g.drawRoundRect(x, rowY, w, 46, 8, 8);
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.setColor(new Color(235, 236, 240));
            g.drawString(profession.label() + "  " + level + "/" + cap, x + 12, rowY + 17);
            int barX = x + 12;
            int barY = rowY + 28;
            int barW = w - 24;
            g.setColor(new Color(45, 50, 63));
            g.fillRoundRect(barX, barY, barW, 8, 6, 6);
            g.setColor(new Color(103, 151, 117));
            int fill = level >= cap ? barW : Math.max(2, Math.min(barW, barW * current / Math.max(1, needed)));
            g.fillRoundRect(barX, barY, fill, 8, 6, 6);
            Rectangle rowBounds = new Rectangle(x, rowY, w, 46);
            String selectedProfession = profession.id();
            buttons.add(new UiButton(rowBounds, "profession-tree:" + selectedProfession, () -> {
                activeProfessionId = selectedProfession;
                skillTreeScroll = 0;
                partySkillScroll = 0;
                repaint();
            }));
            tooltipZones.add(new TooltipZone(rowBounds, profession.label(),
                    "Click to view the " + profession.label() + " skill tree. Current level "
                            + level + "/" + cap + "."));
            rowY += 58;
        }
    }

    private void drawSkillTabs(Graphics2D g, int x, int y, int tabW, boolean party) {
        int tabX = x;
        for (SkillTab tab : SkillTab.values()) {
            boolean active = activeSkillTab == tab;
            SkillTab target = tab;
            actionButton(g, tabX, y, tabW, 30, tab.label, () -> {
                activeSkillTab = target;
                skillTreeScroll = 0;
                partySkillScroll = 0;
                partyLoadoutScroll = 0;
                repaint();
            }, active ? new Color(79, 90, 60) : new Color(46, 54, 72),
                    active ? new Color(139, 154, 96) : new Color(91, 103, 132), true);
            tabX += tabW + (party ? 8 : 12);
        }
    }

    private String activeSkillTitle(Actor actor) {
        return switch (activeSkillTab) {
            case SURVIVAL -> "Survival";
            case CLASS -> actor.className;
            case PROFESSIONS -> Profession.label(activeProfessionId);
            case LOADOUT -> "Loadout";
        };
    }

    private Map<String, SkillNode> activeSkillTree(Actor actor) {
        return switch (activeSkillTab) {
            case SURVIVAL -> SkillTrees.COMMON_SKILL_TREE;
            case CLASS -> SkillTrees.skillTreeForClass(actor.className);
            case PROFESSIONS -> SkillTrees.professionSkillTree(activeProfessionId);
            case LOADOUT -> Map.of();
        };
    }

    private void drawSkillGraph(Graphics2D g, Actor actor, String title, Map<String, SkillNode> tree,
                                int x, int y, int w, int h, boolean party) {
        boolean compact = false;
        g.setFont(new Font("SansSerif", Font.BOLD, party ? 16 : 18));
        g.setColor(new Color(246, 224, 151));
        g.drawString(title, x, y);
        if (tree.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.setColor(new Color(198, 202, 211));
            g.drawString("No skill tree for this class yet.", x, y + 40);
            return;
        }
        int clipY = y + 22;
        int graphH = h - 22;
        int maxGridX = 0;
        int maxGridY = 0;
        for (SkillNode node : tree.values()) {
            maxGridX = Math.max(maxGridX, node.x());
            maxGridY = Math.max(maxGridY, node.y());
        }
        int columns = Math.max(1, maxGridX + 1);
        int gapX = compact ? 18 : 34;
        int naturalW = (w - 24 - gapX * Math.max(0, columns - 1)) / columns;
        int cardW = Math.max(compact ? 150 : 210, Math.min(compact ? 190 : 280, naturalW));
        int cardH = compact ? 58 : 74;
        int rowH = compact ? 94 : 118;
        int contentH = (maxGridY + 1) * rowH + cardH + 12;
        int scrollUnit = 32;
        int maxScroll = Math.max(0, (contentH - graphH + scrollUnit - 1) / scrollUnit);
        int scroll = Math.max(0, Math.min(party ? partySkillScroll : skillTreeScroll, maxScroll));
        int offsetY = scroll * scrollUnit;
        Map<String, Rectangle> nodeRects = new HashMap<>();
        int contentX = x + 12;
        int graphW = w - 24;
        int colStep = columns <= 1 ? 0 : Math.max(1, (graphW - cardW) / Math.max(1, columns - 1));
        for (SkillNode node : tree.values()) {
            int nodeX = contentX + node.x() * colStep;
            int nodeY = clipY + node.y() * rowH - offsetY;
            nodeRects.put(node.id(), new Rectangle(nodeX, nodeY, cardW, cardH));
        }

        Graphics2D graphG = (Graphics2D) g.create();
        graphG.setClip(x, clipY, w, graphH);
        graphG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (SkillNode node : tree.values()) {
            Rectangle to = nodeRects.get(node.id());
            if (to == null) {
                continue;
            }
            for (String required : node.requires()) {
                Rectangle from = nodeRects.get(required);
                if (from == null) {
                    continue;
                }
                boolean unlocked = actor.skillRank(required) > 0;
                boolean canLearn = canShowAllocate(actor, node);
                int fromX = from.x + from.width / 2;
                int fromY = from.y + from.height;
                int toX = to.x + to.width / 2;
                int toY = to.y;
                int midY = fromY + Math.max(10, (toY - fromY) / 2);
                graphG.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                graphG.setColor(new Color(6, 8, 14, 190));
                graphG.drawLine(fromX, fromY, fromX, midY);
                graphG.drawLine(fromX, midY, toX, midY);
                graphG.drawLine(toX, midY, toX, toY);
                graphG.setStroke(new BasicStroke(unlocked || canLearn ? 3f : 2.25f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                graphG.setColor(unlocked ? new Color(128, 206, 154, 235)
                        : canLearn ? new Color(218, 198, 119, 220)
                        : new Color(112, 122, 154, 195));
                graphG.drawLine(fromX, fromY, fromX, midY);
                graphG.drawLine(fromX, midY, toX, midY);
                graphG.drawLine(toX, midY, toX, toY);
            }
        }
        graphG.setStroke(new BasicStroke(1f));
        for (SkillNode node : tree.values()) {
            Rectangle bounds = nodeRects.get(node.id());
            if (bounds != null && bounds.y + bounds.height >= clipY && bounds.y <= clipY + graphH) {
                drawSkillNode(graphG, actor, node, bounds.x, bounds.y, bounds.width, bounds.height,
                        () -> {
                            if (party) {
                                state.allocatePartySkill(node.id());
                            } else {
                                state.allocateSkill(node.id());
                            }
                        }, compact);
            }
        }
        graphG.dispose();
        if (maxScroll > 0) {
            int visibleUnits = Math.max(1, graphH / scrollUnit);
            drawScrollIndicator(g, x + w + 8, clipY, graphH, maxScroll + visibleUnits, scroll, visibleUnits);
        }
    }

    private void drawSkillNode(Graphics2D g, Actor actor, SkillNode node, int x, int y, int w, int h,
                               Runnable allocate, boolean compact) {
        int rank = actor.skillRank(node.id());
        boolean learned = rank > 0;
        boolean canLearn = canShowAllocate(actor, node);
        boolean abilityNode = node.ability() != null;
        g.setColor(learned ? new Color(38, 58, 50, 242)
                : canLearn ? new Color(37, 41, 55, 244)
                : new Color(24, 27, 38, 238));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setStroke(new BasicStroke(learned || canLearn ? 2f : 1.35f));
        g.setColor(learned ? new Color(130, 207, 153)
                : canLearn ? new Color(222, 199, 118)
                : new Color(88, 96, 122));
        g.drawRoundRect(x, y, w, h, 8, 8);
        if (abilityNode) {
            g.setColor(new Color(139, 107, 168, learned ? 110 : 78));
            g.fillRoundRect(x + 4, y + 4, 5, h - 8, 4, 4);
        }
        g.setFont(new Font("SansSerif", Font.BOLD, compact ? 11 : 13));
        g.setColor(new Color(235, 236, 240));
        drawClippedString(g, node.name(), x + (abilityNode ? 14 : 10), y + (compact ? 16 : 18), w - 62);
        g.setFont(new Font("SansSerif", Font.BOLD, compact ? 10 : 11));
        g.setColor(new Color(246, 224, 151));
        g.drawString(rank + "/" + node.maxRank(), x + 10, y + (compact ? 32 : 36));
        g.setFont(new Font("SansSerif", Font.PLAIN, compact ? 10 : 11));
        g.setColor(new Color(176, 182, 196));
        drawClippedString(g, skillNodeStatus(actor, node), x + 38, y + (compact ? 32 : 36), w - 96);
        tooltipZones.add(new TooltipZone(new Rectangle(x, y, w, h), node.name(), skillNodeTooltip(actor, node),
                node.ability() == null ? null : abilityIconName(node.ability())));
        actionButton(g, x + w - (compact ? 42 : 50), y + 8, compact ? 30 : 36, h - 16, "+", allocate, new Color(68, 90, 53), new Color(110, 139, 92), canLearn);
        g.setStroke(new BasicStroke(1f));
    }

    private String skillNodeStatus(Actor actor, SkillNode node) {
        if (actor.skillRank(node.id()) >= node.maxRank()) {
            return "Mastered";
        }
        if (actor.level < node.levelRequirement()) {
            return "Level " + node.levelRequirement();
        }
        for (String required : node.requires()) {
            if (actor.skillRank(required) <= 0) {
                SkillNode requiredNode = SkillTrees.SKILL_TREE.get(required);
                return "Needs " + (requiredNode == null ? required : requiredNode.name());
            }
        }
        if (node.ability() != null) {
            return actor.skillRank(node.id()) > 0 ? "Upgrade " + node.ability().name() : "Unlocks " + node.ability().name();
        }
        return SkillTrees.isProfessionSkill(node.id()) ? "Uses profession point" : "Hover for details";
    }

    private String skillNodeTooltip(Actor actor, SkillNode node) {
        List<String> lines = new ArrayList<>();
        lines.add(skillNodeFlavor(node));
        lines.add(node.description());
        lines.add("Level: " + node.levelRequirement() + " required.");
        if (!node.effects().isEmpty()) {
            lines.add("Innate: " + skillEffectsText(node.effects()) + ".");
        }
        if (node.ability() != null) {
            Ability ability = node.ability();
            int rank = Math.max(1, actor.skillRank(node.id()) + 1);
            int extraRanks = Math.max(0, rank - 1);
            int powerGain = switch (ability.kind()) {
                case DAMAGE -> "all_enemies".equals(ability.target()) ? 6 : 9;
                case HEAL -> "party".equals(ability.target()) ? 5 : 7;
                case DEFEND -> 0;
            };
            int costGain = ability.kind() == Ability.AbilityKind.DEFEND ? 1 : 2;
            int previewPower = ability.power() + extraRanks * powerGain;
            int previewCost = ability.cost() + extraRanks * costGain;
            lines.add("Ability rank " + Math.min(rank, node.maxRank()) + ": " + ability.name() + ".");
            lines.add(skillRankFormula(ability, extraRanks, powerGain, costGain, previewPower, previewCost));
            if (ability.kind() == Ability.AbilityKind.DEFEND) {
                lines.add("Guard cost: " + previewCost + " MP.");
            } else {
                lines.add("Power: " + previewPower + ". Cost: " + previewCost + " MP.");
            }
            if (ability.cooldown() > 0) {
                lines.add("Cooldown: " + ability.cooldown() + " turn(s).");
            }
            lines.add("Scaling: " + scalingLabel(ability) + ".");
            lines.add(skillAbilityFormula(actor, ability, previewPower));
            List<AbilityStatus> statuses = GameData.statusHintsForAbility(ability);
            if (!statuses.isEmpty()) {
                lines.add("Status: " + String.join(", ", statuses.stream().map(this::abilityStatusLabel).toList()) + ".");
            }
        }
        if (!node.requires().isEmpty()) {
            List<String> requirements = new ArrayList<>();
            for (String required : node.requires()) {
                SkillNode requiredNode = SkillTrees.SKILL_TREE.get(required);
                String name = requiredNode == null ? required : requiredNode.name();
                requirements.add(name + (actor.skillRank(required) > 0 ? " learned" : " needed"));
            }
            lines.add("Requires: " + String.join(", ", requirements) + ".");
        }
        lines.add("Rank: " + actor.skillRank(node.id()) + "/" + node.maxRank() + ".");
        lines.add(SkillTrees.isProfessionSkill(node.id()) ? "Costs profession skill points." : "Costs regular skill points.");
        return String.join("\n", lines);
    }

    private String skillEffectsText(Map<String, Integer> effects) {
        return effects.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> signed(entry.getValue()) + " " + skillEffectLabel(entry.getKey()))
                .toList()
                .stream()
                .reduce((first, second) -> first + ", " + second)
                .orElse("no innate stat change");
    }

    private String skillEffectLabel(String key) {
        return switch (key) {
            case "max_hp" -> "max HP";
            case "max_mp" -> "max MP";
            case "attack" -> "ATK";
            case "defense" -> "DEF";
            case "strength" -> "STR";
            case "intelligence" -> "INT";
            case "dexterity" -> "DEX";
            case "charisma" -> "CHA";
            case "constitution" -> "CON";
            case "willpower" -> "WIL";
            default -> key.replace('_', ' ');
        };
    }

    private String signed(int value) {
        return value >= 0 ? "+" + value : Integer.toString(value);
    }

    private String skillRankFormula(Ability ability, int extraRanks, int powerGain, int costGain, int previewPower, int previewCost) {
        if (ability.kind() == Ability.AbilityKind.DEFEND) {
            return "Rank math: cost " + ability.cost() + " + " + extraRanks + " rank(s) x " + costGain
                    + " = " + previewCost + " MP.";
        }
        return "Rank math: power " + ability.power() + " + " + extraRanks + " rank(s) x " + powerGain
                + " = " + previewPower + "; cost " + ability.cost() + " + " + extraRanks
                + " rank(s) x " + costGain + " = " + previewCost + " MP.";
    }

    private String skillAbilityFormula(Actor actor, Ability ability, int previewPower) {
        return switch (ability.kind()) {
            case DAMAGE -> {
                int attributeBonus = actor.abilityScalingBonus(ability);
                int skillBonus = actor == state.player ? state.player.skillRank("spellcraft") * 2 + state.player.skillRank("overchannel") * 3 : 0;
                if (actor == state.player && "all_enemies".equals(ability.target())) {
                    skillBonus += state.player.skillRank("volley_mastery") * 2;
                }
                int total = previewPower + attributeBonus + skillBonus;
                yield "Formula: " + previewPower + " power + " + scalingLabel(ability) + " bonus " + attributeBonus
                        + (skillBonus > 0 ? " + skill bonuses " + skillBonus : "")
                        + " + random 0-5 = " + total + "-" + (total + 5) + " before defenses.";
            }
            case HEAL -> {
                int levelPart = actor.level;
                int attributeBonus = actor.abilityScalingBonus(ability);
                int channeling = actor == state.player ? state.player.skillRank("channeling") * 4 : 0;
                int total = previewPower + levelPart + attributeBonus + channeling;
                yield "Formula: " + previewPower + " power + level " + levelPart
                        + " + " + scalingLabel(ability) + " bonus " + attributeBonus
                        + (channeling > 0 ? " + channeling " + channeling : "")
                        + " + random -2 to +2 = " + Math.max(1, total - 2) + "-" + (total + 2) + " HP.";
            }
            case DEFEND -> {
                int wilPart = actor.willpower / 3;
                int strPart = actor.strength / 5;
                yield "Innate guard: WIL/3 " + wilPart + " + STR/5 " + strPart + " = +" + (wilPart + strPart)
                        + " guard strength before active status effects.";
            }
        };
    }

    private boolean isMentalAbilityForTooltip(Actor actor, Ability ability) {
        String lowerName = ability.name().toLowerCase();
        if (lowerName.contains("fire") || lowerName.contains("frost") || lowerName.contains("arcane")
                || lowerName.contains("radiant") || lowerName.contains("thorn") || lowerName.contains("sun")
                || lowerName.contains("moon") || lowerName.contains("star") || lowerName.contains("bloom")
                || lowerName.contains("mend") || lowerName.contains("heal") || lowerName.contains("ward")
                || lowerName.contains("prayer") || lowerName.contains("chorus") || lowerName.contains("refuge")) {
            return true;
        }
        return switch (actor.className) {
            case "Mage", "Cleric", "Medic", "Battle Medic", "Wildspeaker", "Sunwarden", "Thornbinder", "Grovekeeper" -> true;
            default -> false;
        };
    }

    private String skillNodeFlavor(SkillNode node) {
        if (node.ability() != null) {
            return abilityFlavor(node.ability());
        }
        String description = node.description().toLowerCase();
        if (description.contains("max hp") || description.contains("defense")) {
            return "Hard lessons settle into muscle, mail, and old survival habits.";
        }
        if (description.contains("max mp") || description.contains("healing")) {
            return "The inner well deepens, holding steadier light for darker roads.";
        }
        if (description.contains("attack") || description.contains("damage")) {
            return "Practice turns motion into intent, and intent into a sharper blow.";
        }
        if (SkillTrees.isProfessionSkill(node.id())) {
            return "Trade craft becomes ritual: measured hands, trusted tools, patient work.";
        }
        return "A road-worn lesson, earned one dangerous evening at a time.";
    }

    private boolean canShowAllocate(SkillNode node) {
        return canShowAllocate(state.player, node);
    }

    private boolean canShowAllocate(Actor actor, SkillNode node) {
        boolean professionSkill = SkillTrees.isProfessionSkill(node.id());
        if ((professionSkill ? actor.professionSkillPoints <= 0 : actor.skillPoints <= 0) || actor.skillRank(node.id()) >= node.maxRank()) {
            return false;
        }
        if (actor.level < node.levelRequirement()) {
            return false;
        }
        if (!SkillTrees.availableSkillTree(actor.className).containsKey(node.id())) {
            return false;
        }
        for (String required : node.requires()) {
            if (actor.skillRank(required) <= 0) {
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

    private void drawClippedString(Graphics2D g, String text, int x, int y, int maxWidth) {
        String clipped = text;
        while (clipped.length() > 3 && g.getFontMetrics().stringWidth(clipped) > maxWidth) {
            clipped = clipped.substring(0, clipped.length() - 4) + "...";
        }
        g.drawString(clipped, x, y);
    }

    private String slotLabel(String slot) {
        return switch (slot) {
            case "chestpiece" -> "CHEST";
            case "pauldrons" -> "SHOULDERS";
            case "leggings" -> "LEGS";
            default -> slot.toUpperCase();
        };
    }

    private void drawScrollIndicator(Graphics2D g, int x, int y, int h, int totalRows, int scroll, int visibleRows) {
        if (totalRows <= visibleRows || h <= 0) {
            return;
        }
        g.setColor(new Color(58, 63, 78, 180));
        g.fillRoundRect(x, y, 4, h, 4, 4);
        int thumbH = Math.max(24, h * visibleRows / totalRows);
        int travel = Math.max(1, h - thumbH);
        int maxScroll = Math.max(1, totalRows - visibleRows);
        int thumbY = y + Math.min(travel, scroll * travel / maxScroll);
        g.setColor(new Color(144, 157, 190, 210));
        g.fillRoundRect(x - 1, thumbY, 6, thumbH, 6, 6);
    }

    private void drawInventory(Graphics2D g) {
        int panelW = Math.min(1180, Math.max(1030, gameAreaWidth() - 72));
        int panelX = gameAreaCenteredX(panelW);
        int panelY = 44;
        int panelH = Math.min(790, viewHeight() - 88);
        Actor actor = state.partyScreenActor();
        inventoryDragZones.clear();
        inventoryDropZones.clear();
        drawOverlayBase(g, panelX, panelY, panelW, panelH);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(244, 239, 220));
        g.drawString("Inventory", panelX + 36, panelY + 48);
        int selectorX = panelX + 36;
        int selectorY = panelY + 64;
        int selectorW = panelW - 72;
        int selectorH = inventoryPartySelectorHeight();
        drawInventoryPartySelector(g, selectorX, selectorY, selectorW);

        int modelX = panelX + 36;
        int modelY = selectorY + selectorH + 20;
        int modelW = 430;
        int contentBottom = panelY + panelH - 106;
        int contentH = Math.max(320, contentBottom - modelY);
        drawInventoryCharacter(g, actor, modelX, modelY, modelW, contentH);

        int packX = modelX + modelW + 30;
        int packY = modelY;
        int packW = panelX + panelW - 40 - packX;
        int packH = contentH;
        drawInventoryPackGrid(g, packX, packY, packW, packH);

        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(196, 198, 205));
        g.drawString("Click, drag, or use number keys for visible pack items. Wheel scrolls the grid.", panelX + 36, panelY + panelH - 42);
        g.setColor(new Color(246, 224, 151));
        g.drawString(shortText(state.status, 112), panelX + 36, panelY + panelH - 22);
        actionButton(g, panelX + panelW - 164, panelY + panelH - 62, 124, 34, "Close", state::toggleInventory, new Color(83, 61, 61), new Color(149, 96, 88), true);
        drawInventoryDragGhost(g);
    }

    private void drawInventoryPackGrid(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(new Color(20, 23, 31, 220));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(82, 92, 116));
        g.drawRoundRect(x, y, w, h, 8, 8);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(new Color(246, 224, 151));
        g.drawString("Shared Pack", x + 18, y + 28);

        int gap = 10;
        int cell = 76;
        int gridX = x + 18;
        int gridY = y + 48;
        int gridW = w - 36;
        int gridH = h - 72;
        int cols = Math.max(4, Math.max(1, (gridW + gap) / (cell + gap)));
        int rows = Math.max(3, Math.max(1, (gridH + gap) / (cell + gap)));
        int visible = Math.max(1, cols * rows);
        Rectangle packDrop = new Rectangle(x + 10, y + 42, w - 20, h - 54);
        inventoryDropZones.add(new InventoryDropZone(packDrop, InventoryDropKind.PACK, null));
        drawInventoryDropHint(g, packDrop, "Drop gear here to unequip");

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(state.player.inventory.entrySet());
        int maxScroll = Math.max(0, entries.size() - visible);
        inventoryItemScroll = Math.max(0, Math.min(inventoryItemScroll, maxScroll));
        int end = Math.min(entries.size(), inventoryItemScroll + visible);
        for (int i = inventoryItemScroll; i < end; i++) {
            Map.Entry<String, Integer> entry = entries.get(i);
            int local = i - inventoryItemScroll;
            int col = local % cols;
            int row = local / cols;
            int cellX = gridX + col * (cell + gap);
            int cellY = gridY + row * (cell + gap);
            Rectangle bounds = new Rectangle(cellX, cellY, cell, cell);
            int buttonIndex = i;
            inventoryDragZones.add(new InventoryDragZone(bounds, entry.getKey(), null));
            buttons.add(new UiButton(bounds, "inventory-item:" + entry.getKey(), () -> {
                state.useInventoryItem(buttonIndex);
                repaint();
            }));
            drawInventoryPackCell(g, bounds, entry.getKey(), entry.getValue(), local);
        }
        if (entries.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 16));
            g.setColor(new Color(210, 213, 222));
            g.drawString("Your pack is empty.", gridX, gridY + 32);
        }
        drawScrollIndicator(g, x + w - 12, gridY, Math.min(gridH, rows * (cell + gap) - gap), entries.size(), inventoryItemScroll, visible);
    }

    private String professionSummary(Actor actor) {
        List<String> parts = new ArrayList<>();
        for (Profession profession : Profession.ALL) {
            parts.add(profession.label() + " " + actor.professionLevel(profession.id()));
        }
        return String.join(" | ", parts);
    }

    private void drawInventoryPackCell(Graphics2D g, Rectangle bounds, String itemKey, int count, int visibleIndex) {
        Equipment equipment = GameData.equipment(itemKey);
        Item item = GameData.ITEMS.get(itemKey);
        boolean craftingOnly = CraftingSystem.isCraftingOnlyItem(itemKey);
        boolean hovered = hoverPoint != null && bounds.contains(hoverPoint);
        boolean draggedOver = inventoryDragOver(bounds);
        Color border = equipment != null
                ? rarityColor(equipment.rarity())
                : item != null ? new Color(103, 151, 117) : new Color(125, 136, 172);
        if (item != null) {
            border = rarityColor(item.rarity());
        }
        if (craftingOnly) {
            border = new Color(161, 124, 83);
        }
        if (hovered || draggedOver) {
            border = new Color(151, 177, 112);
        }
        g.setColor(hovered ? new Color(38, 44, 58, 245) : new Color(28, 32, 43, 235));
        g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
        g.setColor(border);
        g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
        g.drawImage(assets.sprite(GameData.itemIcon(itemKey), 46), bounds.x + 15, bounds.y + 11, null);

        String quantity = "x" + count;
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        FontMetrics metrics = g.getFontMetrics();
        int badgeW = Math.max(28, metrics.stringWidth(quantity) + 12);
        int badgeX = bounds.x + bounds.width - badgeW - 7;
        int badgeY = bounds.y + bounds.height - 24;
        g.setColor(new Color(8, 11, 18, 220));
        g.fillRoundRect(badgeX, badgeY, badgeW, 18, 8, 8);
        g.setColor(new Color(238, 239, 244));
        g.drawString(quantity, badgeX + (badgeW - metrics.stringWidth(quantity)) / 2, badgeY + 13);

        if (visibleIndex < 9) {
            String key = Integer.toString(visibleIndex + 1);
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            g.setColor(new Color(176, 182, 196));
            g.drawString(key, bounds.x + 8, bounds.y + 14);
        }
        tooltipZones.add(new TooltipZone(bounds, GameData.itemName(itemKey), inventoryTooltip(itemKey, count),
                null, rarityColorForItem(itemKey)));
    }

    List<CraftingSystem.Recipe> displayedCraftingRecipes() {
        CraftingSystem.Workstation workstation = state.currentWorkstation();
        List<CraftingSystem.Recipe> recipes = state.learnedCraftingRecipes();
        if (workstation != null) {
            recipes = recipes.stream()
                    .filter(recipe -> recipe.workstation() == workstation)
                    .toList();
        }
        CraftingSystem.RecipeCategory[] categories = CraftingSystem.RecipeCategory.values();
        if (craftingRecipeCategoryIndex <= 0 || craftingRecipeCategoryIndex > categories.length) {
            return recipes;
        }
        CraftingSystem.RecipeCategory selected = categories[craftingRecipeCategoryIndex - 1];
        return recipes.stream()
                .filter(recipe -> recipe.category() == selected)
                .toList();
    }

    private int craftingRecipeVisibleSlots() {
        int panelW = 940;
        int panelH = 620;
        int gridW = panelW - 72;
        int gridH = panelH - 228;
        int gap = 10;
        int cell = 76;
        int cols = Math.max(4, Math.max(1, (gridW - 24 + gap) / (cell + gap)));
        int rows = Math.max(3, Math.max(1, (gridH - 34 + gap) / (cell + gap)));
        return Math.max(1, cols * rows);
    }

    private void drawCrafting(Graphics2D g) {
        int panelW = 940;
        int panelX = gameAreaCenteredX(panelW);
        int panelY = 86;
        int panelH = 620;
        drawOverlayBase(g, panelX, panelY, panelW, panelH);
        CraftingSystem.Workstation workstation = state.currentWorkstation();
        CraftingSystem.RecipeCategory[] categories = CraftingSystem.RecipeCategory.values();
        if (craftingRecipeCategoryIndex < 0 || craftingRecipeCategoryIndex > categories.length) {
            craftingRecipeCategoryIndex = 0;
        }
        List<CraftingSystem.Recipe> recipes = displayedCraftingRecipes();

        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(244, 239, 220));
        g.drawString("Recipe Book", panelX + 36, panelY + 48);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(198, 202, 211));
        g.drawString(workstation == null ? "Browsing learned recipes" : "Browsing learned recipes near " + workstation.label(), panelX + 36, panelY + 76);

        int tabX = panelX + 36;
        int tabY = panelY + 92;
        int tabH = 28;
        int tabGap = 6;
        int allW = 54;
        actionButton(g, tabX, tabY, allW, tabH, "All", () -> {
                    craftingRecipeCategoryIndex = 0;
                    craftingRecipeScroll = 0;
                },
                craftingRecipeCategoryIndex == 0 ? new Color(73, 83, 111) : new Color(35, 39, 54),
                craftingRecipeCategoryIndex == 0 ? new Color(145, 166, 214) : new Color(86, 98, 128), true);
        tabX += allW + tabGap;
        for (int i = 0; i < categories.length; i++) {
            CraftingSystem.RecipeCategory category = categories[i];
            int index = i + 1;
            int tabW = Math.max(76, Math.min(118, g.getFontMetrics().stringWidth(category.label()) + 22));
            actionButton(g, tabX, tabY, tabW, tabH, category.label(), () -> {
                        craftingRecipeCategoryIndex = index;
                        craftingRecipeScroll = 0;
                    },
                    craftingRecipeCategoryIndex == index ? new Color(73, 83, 111) : new Color(35, 39, 54),
                    craftingRecipeCategoryIndex == index ? new Color(145, 166, 214) : new Color(86, 98, 128), true);
            tabX += tabW + tabGap;
        }

        int gridX = panelX + 36;
        int gridY = panelY + 146;
        int gridW = panelW - 72;
        int gridH = panelH - 228;
        g.setColor(new Color(20, 23, 31, 220));
        g.fillRoundRect(gridX, gridY, gridW, gridH, 8, 8);
        g.setColor(new Color(82, 92, 116));
        g.drawRoundRect(gridX, gridY, gridW, gridH, 8, 8);

        int gap = 10;
        int cell = 76;
        int cols = Math.max(4, Math.max(1, (gridW - 24 + gap) / (cell + gap)));
        int rows = Math.max(3, Math.max(1, (gridH - 34 + gap) / (cell + gap)));
        int visibleSlots = craftingRecipeVisibleSlots();
        if (recipes.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 17));
            g.setColor(new Color(210, 213, 222));
            g.drawString("No learned recipes in this tab.", gridX + 18, gridY + 38);
        }
        int maxScroll = Math.max(0, recipes.size() - visibleSlots);
        craftingRecipeScroll = Math.min(craftingRecipeScroll, maxScroll);
        int end = Math.min(recipes.size(), craftingRecipeScroll + visibleSlots);
        for (int i = craftingRecipeScroll; i < end; i++) {
            CraftingSystem.Recipe recipe = recipes.get(i);
            int local = i - craftingRecipeScroll;
            int col = local % cols;
            int row = local / cols;
            Rectangle bounds = new Rectangle(gridX + 18 + col * (cell + gap), gridY + 28 + row * (cell + gap), cell, cell);
            drawRecipeBookSlot(g, bounds, recipe, local);
        }
        drawScrollIndicator(g, panelX + panelW - 48, gridY + 28, Math.min(gridH - 44, rows * (cell + gap) - gap),
                recipes.size(), craftingRecipeScroll, visibleSlots);

        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(196, 198, 205));
        String recipeHint = recipes.size() > visibleSlots
                ? "Wheel scrolls the recipe book. Lit slots can be crafted at your current station."
                : "Lit slots can be crafted at your current station. New recipes come from NPCs, books, shops, and ingredients.";
        g.drawString(recipeHint, panelX + 36, panelY + panelH - 42);
        g.setColor(new Color(246, 224, 151));
        g.drawString(shortText(state.status, 92), panelX + 36, panelY + panelH - 22);
        actionButton(g, panelX + panelW - 164, panelY + panelH - 62, 124, 34, "Close", state::toggleCrafting, new Color(83, 61, 61), new Color(149, 96, 88), true);
    }

    private void drawRecipeBookSlot(Graphics2D g, Rectangle bounds, CraftingSystem.Recipe recipe, int visibleIndex) {
        boolean hovered = hoverPoint != null && bounds.contains(hoverPoint);
        boolean stationReady = recipe.workstation() == null || recipe.workstation() == state.currentWorkstation();
        boolean craftable = stationReady && CraftingSystem.canCraft(state.player, recipe) && !state.crafting.active();
        Color border = switch (recipe.category()) {
            case CONSUMABLE -> new Color(103, 151, 117);
            case WEAPON -> new Color(170, 120, 92);
            case ARMOR -> new Color(125, 136, 172);
            case TOOL -> new Color(161, 124, 83);
            case SEED -> new Color(139, 166, 92);
            case ACCESSORY -> new Color(152, 126, 184);
            case MATERIAL -> new Color(151, 142, 116);
            case DECOR -> new Color(116, 151, 164);
        };
        if (craftable) {
            border = new Color(151, 177, 112);
        } else if (!stationReady) {
            border = new Color(86, 98, 128);
        }
        if (hovered) {
            border = blend(border, Color.WHITE, 0.22);
        }
        g.setColor(hovered ? new Color(38, 44, 58, 245) : new Color(28, 32, 43, 235));
        g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
        g.setColor(border);
        g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
        g.drawImage(assets.sprite(GameData.itemIcon(recipe.resultKey()), 42), bounds.x + 17, bounds.y + 9, null);

        if (craftable) {
            buttons.add(new UiButton(bounds, "recipe:" + recipe.key(), () -> {
                state.craftRecipe(recipe.key());
                repaint();
            }));
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            g.setColor(new Color(202, 230, 176));
            drawCenteredIn(g, "Ready", bounds.x, bounds.y + bounds.height - 7, bounds.width);
        } else {
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            g.setColor(new Color(150, 156, 170));
            drawCenteredIn(g, stationReady ? "Needs" : recipeStationLabel(recipe), bounds.x + 3, bounds.y + bounds.height - 7, bounds.width - 6);
        }
        if (visibleIndex < 9) {
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            g.setColor(new Color(176, 182, 196));
            g.drawString(Integer.toString(visibleIndex + 1), bounds.x + 8, bounds.y + 14);
        }
        tooltipZones.add(new TooltipZone(bounds, recipe.name(), recipeTooltip(recipe), GameData.itemIcon(recipe.resultKey()), border));
    }

    private String recipeTooltip(CraftingSystem.Recipe recipe) {
        String output = recipe.resultAmount() + "x " + GameData.itemName(recipe.resultKey());
        String station = "Crafted at: " + recipeStationLabel(recipe) + ".";
        String needs = "Requires: " + CraftingSystem.requirementLabel(recipe) + ".";
        boolean stationReady = recipe.workstation() == null || recipe.workstation() == state.currentWorkstation();
        String readiness = stationReady
                ? CraftingSystem.canCraft(state.player, recipe) ? "Ready to craft here." : "You know this recipe, but need more requirements."
                : "Move to " + recipeStationLabel(recipe) + " to craft it.";
        return recipe.category().label() + ". Output: " + output + ". " + station + " " + needs + " " + recipe.description() + " " + readiness;
    }

    private String recipeStationLabel(CraftingSystem.Recipe recipe) {
        return recipe.workstation() == null ? "Field Crafting" : recipe.workstation().label();
    }

    private void drawInventoryCharacter(Graphics2D g, Actor actor, int x, int y, int w, int h) {
        Rectangle characterDrop = new Rectangle(x + 20, y + 62, w - 40, h - 112);
        inventoryDropZones.add(new InventoryDropZone(characterDrop, InventoryDropKind.CHARACTER, null));

        g.setColor(new Color(20, 23, 31, 220));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(inventoryDragOver(characterDrop) ? new Color(151, 177, 112) : new Color(82, 92, 116));
        g.drawRoundRect(x, y, w, h, 8, 8);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(new Color(246, 224, 151));
        g.drawString(actor.name + "'s Gear", x + 18, y + 28);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(176, 182, 196));
        g.drawString("Drop a potion or ether on the model to use it.", x + 18, y + 48);

        g.setColor(new Color(12, 14, 21, 150));
        int spriteW = 190;
        int spriteH = Math.min(300, Math.max(220, h - 250));
        int spriteX = x + (w - spriteW) / 2;
        int spriteY = y + 96;
        int shadowW = 142;
        int statsH = 106;
        int statsY = y + h - statsH - 18;
        int shadowY = Math.min(statsY - 30, spriteY + spriteH - 30);
        g.fillOval(x + (w - shadowW) / 2, shadowY, shadowW, 34);
        g.drawImage(assets.spriteFit(actor.worldSprite, spriteW, spriteH), spriteX, spriteY, null);

        for (String slot : GameData.EQUIPMENT_SLOTS) {
            drawInventorySlot(g, actor, slot, inventorySlotBounds(slot, x, y, w));
        }
        drawInventoryCharacterStats(g, actor, x + 20, statsY, w - 40, statsH);
    }

    private void drawInventoryCharacterStats(Graphics2D g, Actor actor, int x, int y, int w, int h) {
        g.setColor(new Color(13, 17, 25, 210));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(82, 92, 116, 190));
        g.drawRoundRect(x, y, w, h, 8, 8);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(new Color(246, 224, 151));
        g.drawString("Stats", x + 12, y + 18);

        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        int leftX = x + 12;
        int rightX = x + Math.max(190, w / 2);
        g.setColor(new Color(226, 229, 236));
        drawClippedString(g, "Lv " + actor.level + "   HP " + actor.hp + "/" + actor.maxHp + "   MP " + actor.mp + "/" + actor.maxMp,
                leftX, y + 40, Math.max(120, rightX - leftX - 14));
        drawClippedString(g, "ATK " + actor.attack + "   DEF " + actor.defense,
                rightX, y + 40, Math.max(80, x + w - rightX - 12));
        g.setColor(new Color(186, 192, 205));
        drawClippedString(g, "Skill " + actor.skillPoints + "   Stat " + actor.statPoints + "   Prof " + actor.professionSkillPoints,
                leftX, y + 62, Math.max(120, rightX - leftX - 14));
        drawClippedString(g, actor.className,
                rightX, y + 62, Math.max(80, x + w - rightX - 12));
        g.setColor(new Color(176, 182, 196));
        drawClippedString(g, "Professions: " + professionSummary(actor), leftX, y + 86, w - 24);
    }

    private Rectangle inventorySlotBounds(String slot, int x, int y, int w) {
        int leftX = x + 20;
        int rightX = x + w - 116;
        int topY = y + 72;
        int stepY = 72;
        return switch (slot) {
            case "helmet" -> new Rectangle(leftX, topY, 96, 62);
            case "pauldrons" -> new Rectangle(leftX, topY + stepY, 96, 62);
            case "chestpiece" -> new Rectangle(leftX, topY + stepY * 2, 96, 62);
            case "belt" -> new Rectangle(leftX, topY + stepY * 3, 96, 62);
            case "weapon" -> new Rectangle(leftX, topY + stepY * 4, 96, 62);
            case "necklace" -> new Rectangle(rightX, topY, 96, 62);
            case "gloves" -> new Rectangle(rightX, topY + stepY, 96, 62);
            case "ring" -> new Rectangle(rightX, topY + stepY * 2, 96, 62);
            case "leggings" -> new Rectangle(rightX, topY + stepY * 3, 96, 62);
            case "boots" -> new Rectangle(rightX, topY + stepY * 4, 96, 62);
            default -> new Rectangle(leftX, topY, 96, 62);
        };
    }

    private void drawInventorySlot(Graphics2D g, Actor actor, String slot, Rectangle bounds) {
        Equipment equipment = actor.equippedItem(slot);
        inventoryDropZones.add(new InventoryDropZone(bounds, InventoryDropKind.SLOT, slot));
        if (equipment != null) {
            inventoryDragZones.add(new InventoryDragZone(bounds, equipment.key(), slot));
        }
        boolean hoveredDrop = inventoryDragOver(bounds);
        g.setColor(new Color(28, 32, 43, 230));
        g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
        g.setColor(hoveredDrop ? new Color(151, 177, 112) : new Color(82, 92, 116));
        g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        g.setColor(new Color(246, 224, 151));
        g.drawString(slotLabel(slot), bounds.x + 8, bounds.y + 15);
        if (equipment != null) {
            g.drawImage(assets.sprite(GameData.itemIcon(equipment.key()), 34), bounds.x + 31, bounds.y + 21, null);
        } else {
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g.setColor(new Color(222, 225, 232));
            String empty = "Empty";
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(empty, bounds.x + (bounds.width - metrics.stringWidth(empty)) / 2, bounds.y + 41);
        }
        if (equipment != null) {
            tooltipZones.add(new TooltipZone(bounds, equipment.name(), equipmentTooltip(equipment),
                    null, rarityColor(equipment.rarity())));
        }
    }

    private void drawInventoryDropHint(Graphics2D g, Rectangle bounds, String text) {
        if (inventoryDrag == null || inventoryDrag.sourceSlot() == null) {
            return;
        }
        boolean hovered = inventoryDragOver(bounds);
        g.setColor(hovered ? new Color(151, 177, 112, 130) : new Color(82, 92, 116, 90));
        g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(hovered ? new Color(238, 239, 244) : new Color(176, 182, 196));
        g.drawString(text, bounds.x + 12, bounds.y + bounds.height - 12);
    }

    private boolean inventoryDragOver(Rectangle bounds) {
        return inventoryDrag != null && inventoryDragPoint != null && bounds.contains(inventoryDragPoint);
    }

    private String inventoryTooltip(String itemKey, int count) {
        Equipment equipment = GameData.equipment(itemKey);
        if (equipment != null) {
            return equipmentTooltip(equipment)
                    + "\n\nStack\nCount: " + count
                    + "\n\nEquip\nClick or press the visible number key to equip the selected ally. Drag onto the matching character slot.";
        }
        Item item = GameData.ITEMS.get(itemKey);
        if (CraftingSystem.isCraftingOnlyItem(itemKey)) {
            return "Type\nMaterial\n\nDescription\n" + CraftingSystem.itemDetail(itemKey)
                    + "\n\nStack\nCount: " + count
                    + "\n\nCrafting\nUsed for recipes, upgrades, and workstation projects.";
        }
        if (item == null) {
            return "Type\nField provision\n\nStack\nCount: " + count;
        }
        return itemTooltip(item)
                + "\n\nStack\nCount: " + count
                + "\n\nUse\nClick or press the visible number key to use. Drag onto the character model.";
    }

    private String equipmentTooltip(Equipment equipment) {
        String stats = String.join("  ", equipment.statLines());
        StringBuilder tooltip = new StringBuilder();
        tooltip.append("Type\n")
                .append(equipment.rarityLine()).append(" ").append(slotLabel(equipment.slot()));
        if (!stats.isBlank()) {
            tooltip.append("\n\nStats\n").append(stats);
        }
        tooltip.append("\n\nRequirements\n").append(equipment.levelRangeLine());
        String description = layeredEquipmentDescription(equipment);
        if (!description.isBlank()) {
            tooltip.append("\n\nDescription\n").append(description);
        }
        if (equipment.hasUniqueEffect()) {
            tooltip.append("\n\nEffect\n").append(effectLayerLabel(equipment)).append(": ").append(equipment.uniqueEffect());
        }
        return tooltip.toString();
    }

    private String itemTooltip(Item item) {
        StringBuilder tooltip = new StringBuilder();
        tooltip.append("Type\n").append(item.rarityLine()).append(" Consumable");
        if ("escape_scroll".equals(item.key())) {
            tooltip.append("\n\nUtility\nEscapes a dungeon instantly.");
        } else if (item.heal() > 0 || item.mp() > 0) {
            tooltip.append("\n\nRecovery\n");
            if (item.heal() > 0 && item.mp() > 0) {
                tooltip.append("Restores ").append(item.heal()).append(" HP and ").append(item.mp()).append(" MP.");
            } else if (item.heal() > 0) {
                tooltip.append("Restores ").append(item.heal()).append(" HP.");
            } else {
                tooltip.append("Restores ").append(item.mp()).append(" MP.");
            }
        } else {
            tooltip.append("\n\nUtility\nField provision.");
        }
        if (item.effectDescription() != null && !item.effectDescription().isBlank()) {
            tooltip.append("\n\nEffect\n").append(item.effectDescription());
        }
        return tooltip.toString();
    }

    private String effectLayerLabel(Equipment equipment) {
        if (equipment.uniqueEffect().startsWith("Affix:")) {
            return "Affix";
        }
        if (equipment.rarity().ordinal() >= ItemRarity.UNIQUE.ordinal()) {
            return equipment.rarityLine() + " property";
        }
        return "Effect";
    }

    private String layeredEquipmentDescription(Equipment equipment) {
        String description = equipment.description() == null ? "" : equipment.description().strip();
        description = removeLeadingSentence(description, equipment.rarityLine());
        description = removeLeadingSentence(description, "Requires level");
        return description;
    }

    private String removeLeadingSentence(String text, String prefix) {
        if (text == null || prefix == null || !text.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return text == null ? "" : text;
        }
        int period = text.indexOf('.');
        if (period < 0 || period + 1 >= text.length()) {
            return "";
        }
        return text.substring(period + 1).stripLeading();
    }

    private void drawInventoryDragGhost(Graphics2D g) {
        if (inventoryDrag == null || inventoryDragPoint == null) {
            return;
        }
        Graphics2D ghost = (Graphics2D) g.create();
        ghost.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));
        int x = inventoryDragPoint.x - 22;
        int y = inventoryDragPoint.y - 22;
        ghost.setColor(new Color(8, 11, 18, 230));
        ghost.fillRoundRect(x - 8, y - 8, 190, 56, 8, 8);
        ghost.setColor(new Color(151, 177, 112));
        ghost.drawRoundRect(x - 8, y - 8, 190, 56, 8, 8);
        ghost.drawImage(assets.sprite(GameData.itemIcon(inventoryDrag.itemKey()), 42), x, y, null);
        ghost.setFont(new Font("SansSerif", Font.BOLD, 13));
        ghost.setColor(new Color(238, 239, 244));
        ghost.drawString(shortText(GameData.itemName(inventoryDrag.itemKey()), 18), x + 50, y + 18);
        ghost.setFont(new Font("SansSerif", Font.PLAIN, 11));
        ghost.setColor(new Color(176, 182, 196));
        ghost.drawString(inventoryDrag.sourceSlot() == null ? "From shared pack" : "Equipped " + slotLabel(inventoryDrag.sourceSlot()), x + 50, y + 36);
        ghost.dispose();
    }

    private void drawInventoryPartySelector(Graphics2D g, int x, int y, int w) {
        List<Actor> members = state.partyMembers();
        int cols = Math.min(5, Math.max(1, members.size()));
        int cardW = (w - Math.max(0, cols - 1) * 8) / cols;
        int cardH = members.size() > 5 ? 38 : 52;
        Actor selected = state.partyScreenActor();
        for (int i = 0; i < members.size(); i++) {
            Actor member = members.get(i);
            int col = i % cols;
            int row = i / cols;
            int cardX = x + col * (cardW + 8);
            int cardY = y + row * (cardH + 6);
            boolean active = member == selected;
            g.setColor(active ? new Color(42, 57, 50, 235) : new Color(28, 32, 43, 235));
            g.fillRoundRect(cardX, cardY, cardW, cardH, 8, 8);
            g.setColor(active ? new Color(103, 151, 117) : new Color(82, 92, 116));
            g.drawRoundRect(cardX, cardY, cardW, cardH, 8, 8);
            int spriteW = cardH <= 44 ? 28 : 34;
            int spriteH = cardH <= 44 ? 34 : 44;
            g.drawImage(assets.spriteFit(member.worldSprite, spriteW, spriteH), cardX + 8, cardY + 4, null);
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.setColor(new Color(235, 236, 240));
            g.drawString(shortText(member.name, cardH <= 44 ? 10 : 13), cardX + spriteW + 18, cardY + (cardH <= 44 ? 17 : 21));
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.setColor(new Color(176, 182, 196));
            g.drawString(shortText(member.className + "  Lv " + member.level, 14), cardX + spriteW + 18, cardY + (cardH <= 44 ? 32 : 38));
            int index = i;
            buttons.add(new UiButton(new Rectangle(cardX, cardY, cardW, cardH), "inventory-member:" + member.name, () -> {
                state.selectPartyScreenActor(index);
                inventoryItemScroll = 0;
                partySkillScroll = 0;
                partyLoadoutScroll = 0;
                repaint();
            }));
        }
    }

    private int inventoryPartySelectorHeight() {
        int members = Math.max(1, state.partyMembers().size());
        int cols = Math.min(5, members);
        int rows = (members + cols - 1) / cols;
        int cardH = members > 5 ? 38 : 52;
        return rows * cardH + Math.max(0, rows - 1) * 6;
    }

    private void drawPartyOverview(Graphics2D g) {
        int x = 18;
        int y = 18;
        int w = Math.max(920, gameAreaWidth() - 36);
        int h = Math.max(680, viewHeight() - 36);
        drawOverlayBase(g, x, y, w, h);
        Actor actor = state.partyScreenActor();
        List<Actor> members = state.partyMembers();

        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(244, 239, 220));
        g.drawString("Character", x + 34, y + 48);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(198, 202, 211));
        g.drawString("Gold " + state.player.gold + "   Select an ally to manage stats, skills, and professions.", x + 34, y + 76);

        int memberY = y + 110;
        for (int i = 0; i < members.size(); i++) {
            Actor member = members.get(i);
            boolean selected = member == actor;
            g.setColor(selected ? new Color(42, 57, 50, 235) : new Color(28, 32, 43, 235));
            g.fillRoundRect(x + 34, memberY, 198, 58, 8, 8);
            g.setColor(selected ? new Color(103, 151, 117) : new Color(82, 92, 116));
            g.drawRoundRect(x + 34, memberY, 198, 58, 8, 8);
            Rectangle memberBounds = new Rectangle(x + 34, memberY, 198, 58);
            partyPortraitZones.add(new PartyPortraitZone(memberBounds, member));
            g.drawImage(assets.spriteFit(dialoguePortraitSprite(member), 40, 50), x + 44, memberY + 4, null);
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.setColor(new Color(235, 236, 240));
            g.drawString((i + 1) + ". " + member.name, x + 96, memberY + 23);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.setColor(new Color(176, 182, 196));
            g.drawString(member.className + "  Lv " + member.level, x + 96, memberY + 42);
            int index = i;
            buttons.add(new UiButton(memberBounds, "party-member:" + member.name, () -> {
                state.selectPartyScreenActor(index);
                partySkillScroll = 0;
                partyLoadoutScroll = 0;
                repaint();
            }));
            memberY += 68;
        }

        int detailX = x + 242;
        int contentRight = x + w - 34;
        Rectangle detailPortraitBounds = new Rectangle(detailX, y + 96, 92, 120);
        partyPortraitZones.add(new PartyPortraitZone(detailPortraitBounds, actor));
        g.drawImage(assets.spriteFit(dialoguePortraitSprite(actor), 92, 120), detailX, y + 96, null);
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.setColor(new Color(246, 224, 151));
        g.drawString(actor.name + " the " + actor.className, detailX + 112, y + 122);
        drawPartyCombatSummary(g, actor, detailX + 112, y + 148);
        drawAttributePills(g, actor, detailX + 112, y + 184);
        actor.sanitizeAbilityLoadout();
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(new Color(206, 211, 221));
        g.drawString("Known " + actor.abilities.size() + "   Prepared " + actor.activeAbilityNames.size()
                + "/" + Actor.MAX_ACTIVE_ABILITIES, detailX + 112, y + 232);
        int statY = y + 262;
        drawStatAllocator(g, actor, detailX, statY);

        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(new Color(246, 224, 151));
        int skillHeaderY = statY + 52;
        g.drawString("Skill Trees", detailX, skillHeaderY);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(176, 182, 196));
        g.drawString("Use the mouse wheel to scroll the active tree.", detailX + 94, skillHeaderY);
        drawSkillTabs(g, detailX, skillHeaderY + 14, 132, true);

        int graphY = skillHeaderY + 60;
        int graphH = Math.max(260, y + h - graphY - 86);
        int graphW = contentRight - detailX;
        if (activeSkillTab == SkillTab.LOADOUT) {
            drawAbilityLoadoutTab(g, actor, detailX, graphY, graphW, graphH);
        } else if (activeSkillTab == SkillTab.PROFESSIONS) {
            int columnW = Math.min(320, Math.max(280, graphW / 3));
            drawSkillGraph(g, actor, activeSkillTitle(actor), activeSkillTree(actor), detailX, graphY, graphW - columnW - 28, graphH, true);
            drawProfessionColumn(g, actor, contentRight - columnW, graphY, columnW, graphH);
        } else {
            drawSkillGraph(g, actor, activeSkillTitle(actor), activeSkillTree(actor), detailX, graphY, graphW, graphH, true);
        }
        drawAbilityDragGhost(g);

        int spent = SkillTrees.spent(actor.skillAllocations);
        int respecCost = SkillTrees.respecCost(actor.level, spent);
        boolean canRespec = spent > 0 && state.player.gold >= respecCost;
        actionButton(g, x + w - 330, y + h - 54, 154, 34, "Respec " + respecCost + "g", state::respecPartySkills, new Color(96, 66, 59), new Color(165, 103, 88), canRespec);
        actionButton(g, x + w - 160, y + h - 54, 120, 34, "Close", state::closeOverlay, new Color(58, 72, 100), new Color(107, 126, 166), true);
    }

    private int drawAbilityLoadoutEditor(Graphics2D g, Actor actor, int x, int y, int w) {
        actor.sanitizeAbilityLoadout();
        List<Ability> activeAbilities = actor.activeAbilities();
        List<Ability> known = new ArrayList<>(activeAbilities);
        for (Ability ability : actor.abilities) {
            if (!actor.isAbilityActive(ability.name())) {
                known.add(ability);
            }
        }
        int activeCount = activeAbilities.size();
        int headerH = 22;
        int viewportH = known.size() > 10 ? 86 : Math.max(32, ((Math.max(1, known.size()) + 2) / 3) * 28);
        partyLoadoutBounds = new Rectangle(x, y + headerH, w, viewportH);
        int columns = Math.max(2, Math.min(4, w / 176));
        int gap = 6;
        int rowH = 24;
        int chipW = Math.max(126, (w - (columns - 1) * gap) / columns);
        int rows = Math.max(1, (known.size() + columns - 1) / columns);
        int visibleRows = Math.max(1, viewportH / (rowH + gap));
        int maxScroll = Math.max(0, rows - visibleRows);
        partyLoadoutScroll = Math.max(0, Math.min(partyLoadoutScroll, maxScroll));

        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(new Color(246, 224, 151));
        g.drawString("Battle Loadout " + activeCount + "/" + Actor.MAX_ACTIVE_ABILITIES, x, y + 14);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(176, 182, 196));
        drawClippedString(g, "Click learned abilities to prepare or set aside for combat.", x + 176, y + 14, w - 176);

        Shape previousClip = g.getClip();
        g.setClip(partyLoadoutBounds);
        for (int i = 0; i < known.size(); i++) {
            Ability ability = known.get(i);
            int row = i / columns;
            int col = i % columns;
            int chipX = x + col * (chipW + gap);
            int chipY = y + headerH + (row - partyLoadoutScroll) * (rowH + gap);
            if (chipY + rowH < partyLoadoutBounds.y || chipY > partyLoadoutBounds.y + partyLoadoutBounds.height) {
                continue;
            }
            boolean active = actor.isAbilityActive(ability.name());
            boolean enabled = active || activeCount < Actor.MAX_ACTIVE_ABILITIES;
            Color base = active ? new Color(48, 82, 62, 238) : new Color(28, 32, 43, 226);
            Color edge = active ? new Color(119, 184, 132) : enabled ? new Color(85, 96, 124) : new Color(78, 70, 84);
            g.setColor(base);
            g.fillRoundRect(chipX, chipY, chipW, rowH, 7, 7);
            g.setColor(edge);
            g.drawRoundRect(chipX, chipY, chipW, rowH, 7, 7);
            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            g.setColor(enabled ? new Color(235, 236, 240) : new Color(132, 136, 148));
            drawClippedString(g, (active ? "+ " : "- ") + ability.name(), chipX + 8, chipY + 16, chipW - 44);
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g.setColor(new Color(202, 209, 221));
            String cost = ability.cost() <= 0 ? "free" : ability.cost() + " MP";
            drawClippedString(g, cost, chipX + chipW - 40, chipY + 16, 34);
            Rectangle bounds = new Rectangle(chipX, chipY, chipW, rowH);
            if (enabled) {
                buttons.add(new UiButton(bounds, "loadout:" + actor.name + ":" + ability.name(),
                        () -> state.togglePartyAbilityLoadout(ability.name())));
            }
            tooltipZones.add(new TooltipZone(bounds, ability.name(), abilityLoadoutTooltip(actor, ability),
                    abilityIconName(ability), active ? new Color(119, 184, 132) : edge));
        }
        g.setClip(previousClip);
        drawScrollIndicator(g, x + w + 5, partyLoadoutBounds.y, partyLoadoutBounds.height, rows, partyLoadoutScroll, visibleRows);
        return headerH + viewportH;
    }

    private String abilityLoadoutTooltip(Actor actor, Ability ability) {
        List<String> damageTypes = GameData.damageTypesForAbility(ability);
        List<AbilityStatus> statuses = GameData.statusHintsForAbility(ability);
        int attributeBonus = actor.abilityScalingBonus(ability);
        String target = switch (ability.target()) {
            case "all_enemies" -> "all enemies";
            case "party" -> "party";
            case "self" -> "self";
            default -> "single target";
        };
        String role = switch (ability.kind()) {
            case DAMAGE -> "Damage: " + GameData.damageTypeListLabel(damageTypes)
                    + ", power " + ability.power() + " + attributes " + attributeBonus + ".";
            case HEAL -> "Restore: power " + ability.power() + " + level " + actor.level
                    + " + attributes " + attributeBonus + ".";
            case DEFEND -> "Defense: guard stance using " + scalingLabel(ability) + ".";
        };
        return "Target: " + target + ". Cost: " + ability.cost() + " MP."
                + (ability.cooldown() > 0 ? " Cooldown: " + ability.cooldown() + " turn(s)." : "")
                + "\nScaling: " + scalingLabel(ability) + ".\n"
                + role
                + abilityStatusText(statuses)
                + abilityMechanicsText(ability)
                + (actor.isAbilityActive(ability.name()) ? "\nPrepared in battle." : "\nNot currently prepared.");
    }

    private void drawPartyCombatSummary(Graphics2D g, Actor actor, int x, int y) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(226, 229, 236));
        g.drawString("Lv " + actor.level + "   HP " + actor.hp + "/" + actor.maxHp + "   MP " + actor.mp + "/" + actor.maxMp, x, y);
        g.setColor(new Color(186, 192, 205));
        g.drawString("ATK " + actor.attack + "   DEF " + actor.defense
                + "   Skill " + actor.skillPoints + "   Stat " + actor.statPoints
                + "   Prof " + actor.professionSkillPoints, x, y + 22);
    }

    private void drawAbilityLoadoutTab(Graphics2D g, Actor actor, int x, int y, int w, int h) {
        actor.sanitizeAbilityLoadout();
        actor.sanitizeAbilityLoadoutPresets();
        int presetY = y;
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(new Color(246, 224, 151));
        g.drawString("Battle Loadouts", x, presetY + 18);
        int presetX = x + 170;
        for (String preset : List.of("Travel", "Boss", "Support")) {
            boolean saved = actor.abilityLoadoutPresets.containsKey(preset);
            actionButton(g, presetX, presetY, 78, 26, "Use " + preset, () -> state.applyPartyLoadoutPreset(preset),
                    new Color(46, 54, 72), new Color(91, 103, 132), saved);
            actionButton(g, presetX + 84, presetY, 82, 26, "Save", () -> state.savePartyLoadoutPreset(preset),
                    new Color(68, 74, 48), new Color(139, 154, 96), true);
            presetX += 178;
        }

        int top = y + 42;
        int leftW = Math.max(360, w - 430);
        int rightX = x + leftW + 28;
        int rightW = w - leftW - 28;
        drawLoadoutKnownAbilities(g, actor, x, top, leftW, h - 48);
        drawLoadoutPreparedSlots(g, actor, rightX, top, rightW, h - 48);
    }

    private void drawLoadoutKnownAbilities(Graphics2D g, Actor actor, int x, int y, int w, int h) {
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(new Color(230, 225, 206));
        g.drawString("Known Abilities", x, y + 16);
        int listY = y + 28;
        int rowH = 36;
        int gap = 8;
        int cols = Math.max(1, w / 230);
        int cardW = Math.max(190, (w - gap * (cols - 1)) / cols);
        int rows = Math.max(1, (actor.abilities.size() + cols - 1) / cols);
        int visibleRows = Math.max(1, (h - 34) / (rowH + gap));
        int maxScroll = Math.max(0, rows - visibleRows);
        partyLoadoutScroll = Math.max(0, Math.min(partyLoadoutScroll, maxScroll));
        Rectangle clip = new Rectangle(x, listY, w, h - 34);
        partyLoadoutBounds = clip;
        Shape oldClip = g.getClip();
        g.setClip(clip);
        for (int i = 0; i < actor.abilities.size(); i++) {
            Ability ability = actor.abilities.get(i);
            int row = i / cols;
            int col = i % cols;
            int bx = x + col * (cardW + gap);
            int by = listY + (row - partyLoadoutScroll) * (rowH + gap);
            if (by + rowH < clip.y || by > clip.y + clip.height) {
                continue;
            }
            boolean prepared = actor.isAbilityActive(ability.name());
            g.setColor(prepared ? new Color(38, 58, 50, 238) : new Color(28, 32, 43, 235));
            g.fillRoundRect(bx, by, cardW, rowH, 8, 8);
            g.setColor(prepared ? new Color(130, 207, 153) : new Color(82, 92, 116));
            g.drawRoundRect(bx, by, cardW, rowH, 8, 8);
            g.drawImage(assets.sprite(abilityIconName(ability), 24), bx + 8, by + 6, null);
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.setColor(new Color(235, 236, 240));
            drawClippedString(g, ability.name(), bx + 40, by + 15, cardW - 52);
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g.setColor(new Color(176, 182, 196));
            drawClippedString(g, ability.cost() + " MP  " + scalingLabel(ability), bx + 40, by + 30, cardW - 52);
            Rectangle bounds = new Rectangle(bx, by, cardW, rowH);
            abilityDragZones.add(new AbilityDragZone(bounds, ability.name(), -1));
            tooltipZones.add(new TooltipZone(bounds, ability.name(), abilityLoadoutTooltip(actor, ability),
                    abilityIconName(ability), prepared ? new Color(130, 207, 153) : new Color(82, 92, 116)));
        }
        g.setClip(oldClip);
        drawScrollIndicator(g, x + w + 5, listY, h - 34, rows, partyLoadoutScroll, visibleRows);
    }

    private void drawLoadoutPreparedSlots(Graphics2D g, Actor actor, int x, int y, int w, int h) {
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(new Color(230, 225, 206));
        g.drawString("Prepared Slots", x, y + 16);
        List<Ability> active = actor.activeAbilities();
        int slotY = y + 30;
        int slotH = 40;
        for (int i = 0; i < Actor.MAX_ACTIVE_ABILITIES; i++) {
            Ability ability = i < active.size() ? active.get(i) : null;
            Rectangle bounds = new Rectangle(x, slotY + i * (slotH + 7), w, slotH);
            boolean hovered = abilityDrag != null && abilityDragPoint != null && bounds.contains(abilityDragPoint);
            g.setColor(hovered ? new Color(43, 61, 55, 238) : new Color(22, 26, 36, 232));
            g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
            g.setColor(hovered ? new Color(130, 207, 153) : new Color(82, 92, 116));
            g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.setColor(new Color(246, 224, 151));
            g.drawString((i == 9 ? "0" : Integer.toString(i + 1)) + ".", bounds.x + 10, bounds.y + 25);
            if (ability == null) {
                g.setColor(new Color(145, 154, 174));
                g.drawString("Drop ability here", bounds.x + 38, bounds.y + 25);
            } else {
                g.drawImage(assets.sprite(abilityIconName(ability), 24), bounds.x + 36, bounds.y + 8, null);
                g.setColor(new Color(235, 236, 240));
                drawClippedString(g, ability.name(), bounds.x + 68, bounds.y + 17, bounds.width - 120);
                g.setFont(new Font("SansSerif", Font.PLAIN, 10));
                g.setColor(new Color(176, 182, 196));
                drawClippedString(g, ability.cost() + " MP", bounds.x + 68, bounds.y + 32, bounds.width - 120);
                abilityDragZones.add(new AbilityDragZone(bounds, ability.name(), i));
                actionButton(g, bounds.x + bounds.width - 38, bounds.y + 8, 28, 24, "x",
                        () -> state.removePartyAbilityFromLoadout(ability.name()), new Color(88, 56, 56), new Color(149, 96, 88), true);
            }
            abilityDropZones.add(new AbilityDropZone(bounds, i, false));
        }
        Rectangle remove = new Rectangle(x, y + h - 38, w, 30);
        boolean overRemove = abilityDrag != null && abilityDragPoint != null && remove.contains(abilityDragPoint);
        g.setColor(overRemove ? new Color(88, 50, 50, 238) : new Color(31, 35, 47, 220));
        g.fillRoundRect(remove.x, remove.y, remove.width, remove.height, 8, 8);
        g.setColor(overRemove ? new Color(190, 103, 85) : new Color(82, 92, 116));
        g.drawRoundRect(remove.x, remove.y, remove.width, remove.height, 8, 8);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(new Color(235, 236, 240));
        drawCenteredIn(g, "Drop here to remove from loadout", remove.x, remove.y + 20, remove.width);
        abilityDropZones.add(new AbilityDropZone(remove, -1, true));
    }

    private void drawAbilityDragGhost(Graphics2D g) {
        if (abilityDrag == null || abilityDragPoint == null) {
            return;
        }
        int x = abilityDragPoint.x - 20;
        int y = abilityDragPoint.y - 18;
        Graphics2D ghost = (Graphics2D) g.create();
        ghost.setComposite(AlphaComposite.SrcOver.derive(0.82f));
        ghost.setColor(new Color(12, 14, 22, 230));
        ghost.fillRoundRect(x, y, 210, 34, 8, 8);
        ghost.setColor(new Color(130, 207, 153));
        ghost.drawRoundRect(x, y, 210, 34, 8, 8);
        ghost.setFont(new Font("SansSerif", Font.BOLD, 12));
        ghost.setColor(new Color(235, 236, 240));
        drawClippedString(ghost, abilityDrag.abilityName(), x + 12, y + 22, 186);
        ghost.dispose();
    }

    private void drawAttributePills(Graphics2D g, Actor actor, int x, int y) {
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        int pillX = x;
        for (String[] stat : ATTRIBUTE_STATS) {
            Color color = attributeColor(stat[1]);
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 42));
            g.fillRoundRect(pillX, y, 70, 24, 8, 8);
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 178));
            g.drawRoundRect(pillX, y, 70, 24, 8, 8);
            g.setColor(color);
            g.drawString(stat[0] + " " + attributeValue(actor, stat[1]), pillX + 10, y + 17);
            Rectangle bounds = new Rectangle(pillX, y, 70, 24);
            tooltipZones.add(new TooltipZone(bounds, statTooltipTitle(stat[0], stat[1]),
                    statTooltip(actor, stat[1], false), null, color));
            pillX += 78;
        }
    }

    private int attributeValue(Actor actor, String statKey) {
        return switch (statKey) {
            case "strength" -> actor.strength;
            case "intelligence" -> actor.intelligence;
            case "dexterity" -> actor.dexterity;
            case "charisma" -> actor.charisma;
            case "constitution" -> actor.constitution;
            case "willpower" -> actor.willpower;
            default -> 0;
        };
    }

    private Color attributeColor(String statKey) {
        return switch (statKey) {
            case "strength" -> new Color(232, 126, 92);
            case "intelligence" -> new Color(125, 166, 246);
            case "dexterity" -> new Color(116, 203, 151);
            case "charisma" -> new Color(237, 185, 104);
            case "constitution" -> new Color(223, 104, 118);
            case "willpower" -> new Color(168, 143, 239);
            default -> new Color(206, 211, 221);
        };
    }

    private void drawStatAllocator(Graphics2D g, Actor actor, int x, int y) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(176, 182, 196));
        g.drawString("Stat Points " + actor.statPoints, x, y + 16);
        int buttonX = x + 116;
        for (String[] stat : ATTRIBUTE_STATS) {
            Color color = attributeColor(stat[1]);
            Rectangle bounds = new Rectangle(buttonX, y, 54, 22);
            actionButton(g, buttonX, y, 54, 22, stat[0] + " +", () -> state.allocateStat(actor, stat[1]),
                    new Color(Math.max(24, color.getRed() / 3), Math.max(24, color.getGreen() / 3), Math.max(24, color.getBlue() / 3)),
                    color, actor.statPoints > 0);
            tooltipZones.add(new TooltipZone(bounds, statTooltipTitle(stat[0], stat[1]),
                    statTooltip(actor, stat[1], true), null, color));
            buttonX += 62;
        }
    }

    private String statTooltipTitle(String abbreviation, String statKey) {
        return abbreviation + " - " + switch (statKey) {
            case "strength" -> "Strength";
            case "intelligence" -> "Intelligence";
            case "dexterity" -> "Dexterity";
            case "charisma" -> "Charisma";
            case "constitution" -> "Constitution";
            case "willpower" -> "Willpower";
            default -> statKey;
        };
    }

    private String statTooltip(Actor actor, String statKey, boolean allocationPreview) {
        int value = attributeValue(actor, statKey);
        String current = "Current: " + value + (allocationPreview && actor.statPoints > 0 ? ". Click to spend 1 stat point." : ".");
        String effect = switch (statKey) {
            case "strength" -> "Scales basic attacks by STR/2, physical ability damage by STR/2, guard MP by STR/5, physical crit chance by +0.15% per STR, crit damage by +(STR+DEX)/120 until capped, and parry by +0.25% per STR.";
            case "intelligence" -> "Scales mental ability damage by INT/2, healing by INT/5, mental crit chance by +0.15% per INT, mental crit damage by +(INT+DEX)/120 until capped, and studied-monster damage through lore insight.";
            case "dexterity" -> "Scales basic attacks by DEX/4, all ability damage by DEX/5, crit chance by +0.7% per DEX, dodge by +0.5% per DEX, parry by +0.25% per DEX, and crit damage by +(focus+DEX)/120 until capped.";
            case "charisma" -> "Scales healing by CHA/3. It is the dedicated social/support stat and does not increase direct damage.";
            case "constitution" -> "Adds +" + Actor.HP_PER_CONSTITUTION + " max HP per CON, raises parry by +0.25% per CON, and improves staying power for low-HP defensive skills.";
            case "willpower" -> "Adds +" + Actor.MP_PER_WILLPOWER + " max MP per WIL, scales all ability damage by WIL/6, healing by WIL/2, and guard MP by WIL/3.";
            default -> "No scaling data available yet.";
        };
        return current + "\n" + effect;
    }

    private void drawPartyGearSection(Graphics2D g, Actor actor, int x, int y, int w) {
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(new Color(246, 224, 151));
        g.drawString("Equipment", x, y);
        int rowY = y + 16;
        for (String slot : GameData.EQUIPMENT_SLOTS) {
            Equipment equipment = actor.equippedItem(slot);
            g.setColor(new Color(28, 32, 43, 235));
            g.fillRoundRect(x, rowY, w, 30, 8, 8);
            g.setColor(new Color(82, 92, 116));
            g.drawRoundRect(x, rowY, w, 30, 8, 8);
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.setColor(new Color(246, 224, 151));
            g.drawString(slotLabel(slot), x + 12, rowY + 20);
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.setColor(new Color(222, 225, 232));
            g.drawString(equipment == null ? "None" : shortText(equipment.name(), 44), x + 106, rowY + 20);
            if (equipment != null) {
                actionButton(g, x + w - 72, rowY + 4, 58, 22, "Off", () -> state.unequipPartySlot(slot), new Color(88, 56, 56), new Color(149, 96, 88), true);
            }
            rowY += 36;
        }
    }

    private void drawPartyPackList(Graphics2D g, Actor actor, int x, int y, int w) {
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(new Color(246, 224, 151));
        g.drawString("Pack", x, y);
        int rowY = y + 16;
        int shown = 0;
        java.util.LinkedHashSet<String> keys = new java.util.LinkedHashSet<>();
        keys.addAll(state.player.inventory.keySet());
        if (actor != state.player) {
            keys.addAll(actor.inventory.keySet());
        }
        for (String key : keys) {
            if (!GameData.isEquipment(key) && !GameData.ITEMS.containsKey(key)) {
                continue;
            }
            int count = state.player.inventory.getOrDefault(key, 0) + (actor == state.player ? 0 : actor.inventory.getOrDefault(key, 0));
            if (count <= 0) {
                continue;
            }
            g.setColor(new Color(28, 32, 43, 235));
            g.fillRoundRect(x, rowY, w, 44, 8, 8);
            g.setColor(new Color(82, 92, 116));
            g.drawRoundRect(x, rowY, w, 44, 8, 8);
            g.drawImage(assets.sprite(GameData.itemIcon(key), 28), x + 8, rowY + 8, null);
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.setColor(new Color(235, 236, 240));
            g.drawString(shortText(GameData.itemName(key), 15) + " x" + count, x + 42, rowY + 18);
            String action = GameData.isEquipment(key) ? "Eq" : "Use";
            actionButton(g, x + w - 46, rowY + 12, 36, 22, action, () -> state.usePartyInventoryItem(key), new Color(68, 90, 53), new Color(110, 139, 92), true);
            rowY += 50;
            shown++;
            if (shown >= 4) {
                break;
            }
        }
        if (shown == 0) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.setColor(new Color(198, 202, 211));
            g.drawString("No gear or items.", x, rowY + 18);
        }
    }

    private void drawActorSkillColumn(Graphics2D g, Actor actor, String title, Map<String, SkillNode> tree, int x, int y, int w, int h) {
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(new Color(246, 224, 151));
        g.drawString(title, x, y);
        int clipY = y + 18;
        int rowH = 38;
        int visibleRows = Math.max(1, (h - 18) / rowH);
        int maxScroll = Math.max(0, tree.size() - visibleRows);
        int scroll = Math.max(0, Math.min(partySkillScroll, maxScroll));
        Graphics2D listG = (Graphics2D) g.create();
        listG.setClip(x, clipY, w, h - 18);
        int rowY = clipY - scroll * rowH;
        for (SkillNode node : tree.values()) {
            if (rowY + 32 >= clipY && rowY <= clipY + h - 18) {
                drawActorSkillNode(listG, actor, node, x, rowY, w);
            }
            rowY += 38;
        }
        listG.dispose();
        drawScrollIndicator(g, x + w + 8, clipY, h - 18, tree.size(), scroll, visibleRows);
    }

    private void drawActorSkillNode(Graphics2D g, Actor actor, SkillNode node, int x, int y, int w) {
        int rank = actor.skillRank(node.id());
        boolean learned = rank > 0;
        boolean canLearn = canShowAllocate(actor, node);
        g.setColor(learned ? new Color(38, 50, 45, 232) : new Color(28, 32, 43, 232));
        g.fillRoundRect(x, y, w, 32, 8, 8);
        g.setColor(learned ? new Color(103, 151, 117) : canLearn ? new Color(96, 106, 133) : new Color(65, 68, 82));
        g.drawRoundRect(x, y, w, 32, 8, 8);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(new Color(235, 236, 240));
        g.drawString(shortText(node.name(), 24) + "  " + rank + "/" + node.maxRank(), x + 10, y + 20);
        actionButton(g, x + w - 52, y + 5, 40, 22, "+", () -> state.allocatePartySkill(node.id()), new Color(68, 90, 53), new Color(110, 139, 92), canLearn);
    }

    private void drawVillageSidebar(Graphics2D g, int left) {
        if (lastVillageTab != state.villageTab
                || !lastVillagePropCategory.equals(state.selectedVillagePropCategory)
                || !lastInteriorAssetCategory.equals(state.selectedInteriorAssetCategory)
                || !lastVillageBuildSearch.equals(villageBuildSearch)) {
            villageListScroll = 0;
            lastVillageTab = state.villageTab;
            lastVillagePropCategory = state.selectedVillagePropCategory;
            lastInteriorAssetCategory = state.selectedInteriorAssetCategory;
            lastVillageBuildSearch = villageBuildSearch;
        }
        int x = left + 18;
        int w = GameConfig.SIDEBAR_WIDTH - 36;
        int y = 32;
        g.setFont(new Font("SansSerif", Font.BOLD, 21));
        g.setColor(new Color(243, 238, 219));
        g.drawString("Oathstead", x, y);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(198, 202, 211));
        drawClippedString(g, (state.isManagedVillageInterior() ? "Interior" : "Village")
                + " | Storage " + state.villageStorageUsed() + "/" + state.villageStorageCapacity()
                + " | Gold " + state.player.gold, x, y + 22, w);

        String[] tabs = {"Build", "Tiles", "Props", "Inside", "Workers"};
        int tabY = y + 44;
        int tabW = (w - 8) / 2;
        for (int i = 0; i < tabs.length; i++) {
            int tab = i;
            boolean selected = state.villageTab == i;
            int bx = x + (i % 2) * (tabW + 8);
            int by = tabY + (i / 2) * 30;
            actionButton(g, bx, by, tabW, 24, tabs[i], () -> setVillageTabFromSidebar(tab),
                    selected ? new Color(57, 76, 60) : new Color(35, 39, 54),
                    selected ? new Color(126, 176, 95) : new Color(86, 98, 128), true);
        }

        int actionY = tabY + 94;
        int modeW = (w - 8) / 2;
        sidebarModeButton(g, x, actionY, modeW, "Place", "place", new Color(126, 176, 95));
        sidebarModeButton(g, x + modeW + 8, actionY, modeW, "Move", "move", new Color(169, 137, 74));
        sidebarModeButton(g, x, actionY + 30, modeW, "Upgrade", "upgrade", new Color(125, 107, 166));
        sidebarModeButton(g, x + modeW + 8, actionY + 30, modeW, "Delete", "delete", new Color(149, 96, 88));

        int listY = actionY + 72;
        if (state.config.showMapEditorButton || state.isMapEditorMap()) {
            listY = drawMapEditorSidebarControls(g, x, listY, w) + 10;
        }
        if (state.villageTab == 2) {
            listY = drawVillagePropCategoryButtons(g, x, listY, w) + 10;
        }
        if (state.villageTab == 3) {
            listY = drawInteriorCategoryButtons(g, x, listY, w) + 10;
        }
        if (villageSearchApplies()) {
            listY = drawVillageSearchBox(g, x, listY, w) + 10;
        } else {
            villageSearchFocused = false;
            villageSearchBounds = new Rectangle();
        }
        int footerH = 92;
        int listH = Math.max(120, viewHeight() - listY - footerH);
        if (state.villageTab == 0) {
            drawVillageSidebarBuildings(g, x, listY, w, listH);
        } else if (state.villageTab == 1) {
            drawVillageSidebarTiles(g, x, listY, w, listH);
        } else if (state.villageTab == 2) {
            drawVillageSidebarProps(g, x, listY, w, listH);
        } else if (state.villageTab == 3) {
            drawVillageSidebarInteriors(g, x, listY, w, listH);
        } else {
            drawVillageSidebarWorkers(g, x, listY, w, listH);
        }

        int statusY = viewHeight() - 76;
        g.setColor(new Color(11, 13, 20, 212));
        g.fillRoundRect(x, statusY, w, 44, 8, 8);
        g.setColor(new Color(74, 82, 105));
        g.drawRoundRect(x, statusY, w, 44, 8, 8);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(198, 202, 211));
        drawWrapped(g, state.status, x + 10, statusY + 18, w - 20, 14, 2);
        actionButton(g, x, viewHeight() - 26, w, 22, "Close Village", state::toggleVillage,
                new Color(58, 72, 100), new Color(107, 126, 166), true);
    }

    private int drawMapEditorSidebarControls(Graphics2D g, int x, int y, int w) {
        int h = state.isMapEditorMap() ? 62 : 118;
        g.setColor(new Color(11, 13, 20, 205));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(74, 82, 105));
        g.drawRoundRect(x, y, w, h, 8, 8);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(246, 224, 151));
        drawClippedString(g, state.isMapEditorMap() ? "Map Editor: " + state.world.label(state.currentMapId) : "Map Editor",
                x + 10, y + 19, w - 20);
        if (state.isMapEditorMap()) {
            int buttonW = (w - 28) / 2;
            actionButton(g, x + 10, y + 28, buttonW, 24, "Export", this::exportMapEditorDesign,
                    new Color(57, 71, 102), new Color(110, 127, 160), true);
            actionButton(g, x + 18 + buttonW, y + 28, buttonW, 24, "Exit Editor", state::exitMapEditor,
                    new Color(83, 61, 61), new Color(149, 96, 88), true);
            return y + h;
        }
        int buttonW = 28;
        int valueX = x + 92;
        actionButton(g, x + 10, y + 30, buttonW, 24, "<", state::previousMapEditorKind,
                new Color(35, 39, 54), new Color(86, 98, 128), true);
        drawCenteredIn(g, state.selectedMapEditorKindLabel(), valueX, y + 48, w - 184);
        actionButton(g, x + w - 38, y + 30, buttonW, 24, ">", state::nextMapEditorKind,
                new Color(35, 39, 54), new Color(86, 98, 128), true);
        actionButton(g, x + 10, y + 62, buttonW, 24, "<", state::previousMapEditorSize,
                new Color(35, 39, 54), new Color(86, 98, 128), true);
        drawCenteredIn(g, state.selectedMapEditorSizeLabel(), valueX, y + 80, w - 184);
        actionButton(g, x + w - 38, y + 62, buttonW, 24, ">", state::nextMapEditorSize,
                new Color(35, 39, 54), new Color(86, 98, 128), true);
        actionButton(g, x + 10, y + 92, w - 20, 20, "Enter Editor", state::enterMapEditor,
                new Color(57, 76, 60), new Color(126, 176, 95), true);
        return y + h;
    }

    private void setVillageTabFromSidebar(int tab) {
        state.setVillageTab(tab);
        villageListScroll = 0;
    }

    boolean handleVillageSearchKey(KeyEvent event) {
        int code = event.getKeyCode();
        if (!villageSearchApplies()) {
            villageSearchFocused = false;
            return false;
        }
        if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_ENTER) {
            villageSearchFocused = false;
            return true;
        }
        if (code == KeyEvent.VK_BACK_SPACE) {
            if (!villageBuildSearch.isEmpty()) {
                villageBuildSearch = villageBuildSearch.substring(0, villageBuildSearch.length() - 1);
            }
            return true;
        }
        if (code == KeyEvent.VK_DELETE) {
            villageBuildSearch = "";
            return true;
        }
        char ch = event.getKeyChar();
        if ((Character.isLetterOrDigit(ch) || ch == ' ' || ch == '-' || ch == '_') && villageBuildSearch.length() < 32) {
            villageBuildSearch += ch;
            return true;
        }
        return true;
    }

    private void sidebarModeButton(Graphics2D g, int x, int y, int w, String label, String action, Color border) {
        actionButton(g, x, y, w, 24, label, () -> state.setVillageEditAction(action),
                action.equals(state.villageEditAction) ? new Color(57, 76, 60) : new Color(35, 39, 54),
                border, true);
    }

    private int drawVillagePropCategoryButtons(Graphics2D g, int x, int y, int w) {
        List<String> categories = outdoorAssetCategoriesForCurrentEditor();
        int buttonW = (w - 8) / 2;
        for (int i = 0; i < categories.size(); i++) {
            String category = categories.get(i);
            boolean selected = category.equals(state.selectedVillagePropCategory);
            int bx = x + (i % 2) * (buttonW + 8);
            int by = y + (i / 2) * 26;
            actionButton(g, bx, by, buttonW, 22, category, () -> {
                selectOutdoorAssetCategoryForCurrentEditor(category);
                villageListScroll = 0;
            }, selected ? new Color(57, 76, 60) : new Color(35, 39, 54),
                    selected ? new Color(126, 176, 95) : new Color(86, 98, 128), true);
        }
        return y + Math.max(1, (categories.size() + 1) / 2) * 26;
    }

    private int drawInteriorCategoryButtons(Graphics2D g, int x, int y, int w) {
        List<String> categories = VillageManager.interiorAssetCategories();
        int buttonW = (w - 8) / 2;
        for (int i = 0; i < categories.size(); i++) {
            String category = categories.get(i);
            boolean selected = category.equals(state.selectedInteriorAssetCategory)
                    || ("All".equals(category) && !VillageManager.interiorAssetCategories().contains(state.selectedInteriorAssetCategory));
            int bx = x + (i % 2) * (buttonW + 8);
            int by = y + (i / 2) * 26;
            actionButton(g, bx, by, buttonW, 22, category, () -> {
                state.selectInteriorAssetCategory(category);
                villageListScroll = 0;
            }, selected ? new Color(57, 76, 60) : new Color(35, 39, 54),
                    selected ? new Color(126, 176, 95) : new Color(86, 98, 128), true);
        }
        return y + Math.max(1, (categories.size() + 1) / 2) * 26;
    }

    private int drawVillageSearchBox(Graphics2D g, int x, int y, int w) {
        villageSearchBounds = new Rectangle(x, y, w, 28);
        g.setColor(villageSearchFocused ? new Color(24, 30, 42, 238) : new Color(16, 19, 28, 220));
        g.fillRoundRect(x, y, w, 28, 8, 8);
        g.setColor(villageSearchFocused ? new Color(145, 166, 214) : new Color(74, 82, 105));
        g.drawRoundRect(x, y, w, 28, 8, 8);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        String text = villageBuildSearch.isBlank() ? "Search" : villageBuildSearch;
        g.setColor(villageBuildSearch.isBlank() ? new Color(132, 138, 152) : new Color(228, 231, 238));
        drawClippedString(g, text, x + 10, y + 18, w - 20);
        return y + 28;
    }

    private boolean villageSearchApplies() {
        return state.mode == GameMode.VILLAGE && (state.villageTab == 0 || state.villageTab == 2 || state.villageTab == 3);
    }

    private List<String> outdoorAssetCategoriesForCurrentEditor() {
        if (!state.isMapEditorMap()) {
            return VillageManager.outdoorAssetCategories();
        }
        Map<String, Boolean> categories = new LinkedHashMap<>();
        for (String category : VillageManager.outdoorAssetCategories()) {
            categories.put(category, true);
        }
        for (String asset : assets.assetNames()) {
            if (isEditorOutdoorAsset(asset)) {
                categories.put(editorAssetCategory(asset), true);
            }
        }
        return List.copyOf(categories.keySet());
    }

    private List<VillageManager.PlaceableAsset> outdoorAssetsForCurrentEditor(String category) {
        if (!state.isMapEditorMap()) {
            return VillageManager.outdoorAssets(category);
        }
        Map<String, VillageManager.PlaceableAsset> byAsset = new LinkedHashMap<>();
        for (VillageManager.PlaceableAsset asset : VillageManager.outdoorAssets()) {
            if (asset.category().equals(category)) {
                byAsset.put(asset.asset(), asset);
            }
        }
        for (String asset : assets.assetNames()) {
            if (!isEditorOutdoorAsset(asset) || !editorAssetCategory(asset).equals(category)) {
                continue;
            }
            byAsset.putIfAbsent(asset, new VillageManager.PlaceableAsset(asset, editorAssetLabel(asset), 48,
                    VillageManager.VillageCost.free(), category));
        }
        if (byAsset.isEmpty()) {
            return VillageManager.outdoorAssets(category);
        }
        return List.copyOf(byAsset.values());
    }

    private void selectOutdoorAssetCategoryForCurrentEditor(String category) {
        if (!state.isMapEditorMap()) {
            state.selectVillagePropCategory(category);
            return;
        }
        List<VillageManager.PlaceableAsset> options = outdoorAssetsForCurrentEditor(category);
        if (options.isEmpty()) {
            return;
        }
        state.selectedVillagePropCategory = category;
        state.selectedVillageAsset = options.get(0).asset();
        state.setVillageEditAction("place");
    }

    private boolean isEditorOutdoorAsset(String asset) {
        return asset != null
                && (asset.startsWith("deco_")
                || asset.startsWith("city_")
                || asset.startsWith("village_")
                || asset.startsWith("player_village_")
                || asset.startsWith("location_"))
                && !asset.startsWith("interior_")
                && !asset.endsWith("_anim");
    }

    private String editorAssetCategory(String asset) {
        if (asset.startsWith("city_")) {
            return "City Props";
        }
        if (asset.startsWith("village_") || asset.startsWith("player_village_")) {
            return "Village Props";
        }
        if (asset.startsWith("location_")) {
            return "Location Props";
        }
        if (asset.startsWith("deco_")) {
            return "Nature Props";
        }
        return "Props";
    }

    private String editorAssetLabel(String asset) {
        return assetLabel(asset
                .replace("city_prop_", "")
                .replace("village_prop_", "")
                .replace("player_village_", "")
                .replace("location_", "")
                .replace("deco_", "")
                .replace('_', ' '));
    }

    private List<VillageManager.BuildingPlan> filteredBuildingPlans(List<VillageManager.BuildingPlan> plans) {
        String query = normalizedVillageSearch();
        if (query.isBlank()) {
            return plans;
        }
        return plans.stream()
                .filter(plan -> searchable(plan.label(), plan.style(), plan.description()).contains(query))
                .toList();
    }

    private List<VillageManager.PlaceableAsset> filteredPlaceableAssets(List<VillageManager.PlaceableAsset> items) {
        String query = normalizedVillageSearch();
        if (query.isBlank()) {
            return items;
        }
        return items.stream()
                .filter(item -> searchable(item.label(), item.asset(), item.category()).contains(query))
                .toList();
    }

    private String normalizedVillageSearch() {
        return villageBuildSearch == null ? "" : villageBuildSearch.strip().toLowerCase();
    }

    private String searchable(String... parts) {
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(part.toLowerCase().replace('_', ' '));
            result.append(' ').append(part.toLowerCase());
        }
        return result.toString();
    }

    private void drawVillageSidebarBuildings(Graphics2D g, int x, int y, int w, int h) {
        List<VillageManager.BuildingPlan> plans = filteredBuildingPlans(VillageManager.buildingPlans());
        int rowH = 72;
        int visibleRows = Math.max(1, h / rowH);
        int scroll = clamp(villageListScroll, 0, Math.max(0, plans.size() - visibleRows));
        villageListScroll = scroll;
        Graphics2D listG = (Graphics2D) g.create();
        listG.setClip(x, y, w, h);
        int rowY = y - scroll * rowH;
        for (VillageManager.BuildingPlan plan : plans) {
            if (rowY + rowH > y && rowY < y + h) {
                drawVillageSidebarBuildingRow(listG, plan, x, rowY, w, rowH - 8);
            }
            rowY += rowH;
        }
        listG.dispose();
        drawScrollIndicator(g, x + w + 4, y, h, plans.size(), scroll, visibleRows);
    }

    private void drawVillageSidebarBuildingRow(Graphics2D g, VillageManager.BuildingPlan plan, int x, int y, int w, int h) {
        boolean selected = plan.style().equals(state.selectedVillageBuildingStyle);
        boolean affordable = state.canAffordVillageCost(plan.cost());
        actionButton(g, x, y, w, h, "", () -> state.selectVillageBuildingStyle(plan.style()),
                selected ? new Color(41, 61, 48, 242) : new Color(24, 29, 41, 235),
                selected ? new Color(126, 176, 95) : affordable ? new Color(86, 98, 128) : new Color(92, 74, 74), true);
        g.drawImage(assets.spriteFit(buildingPreviewSprite(plan), 48, 48), x + 8, y + 8, null);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(238, 239, 244));
        drawClippedString(g, plan.label(), x + 64, y + 20, w - 72);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(affordable ? new Color(176, 182, 196) : new Color(222, 143, 132));
        drawClippedString(g, plan.width() + "x" + plan.depth() + " | " + villageCostDisplay(plan.cost()), x + 64, y + 38, w - 72);
        g.setColor(new Color(151, 177, 112));
        drawClippedString(g, plan.storageCapacity() > 0 ? "Storage +" + plan.storageCapacity()
                : plan.workerRole().isBlank() ? "Max Lv " + plan.maxLevel() : VillageManager.workerRole(plan.workerRole()).label(),
                x + 64, y + 55, w - 72);
    }

    private void drawVillageSidebarTiles(Graphics2D g, int x, int y, int w, int h) {
        List<VillageManager.TilePlan> tiles = state.isManagedVillageInterior()
                ? VillageManager.tilePlans().stream()
                .filter(tile -> tile.tile() == 'i' || tile.tile() == 'o')
                .toList()
                : VillageManager.tilePlans();
        int rowH = 58;
        int visibleRows = Math.max(1, h / rowH);
        int scroll = clamp(villageListScroll, 0, Math.max(0, tiles.size() - visibleRows));
        villageListScroll = scroll;
        Graphics2D listG = (Graphics2D) g.create();
        listG.setClip(x, y, w, h);
        int rowY = y - scroll * rowH;
        for (VillageManager.TilePlan tile : tiles) {
            if (rowY + rowH > y && rowY < y + h) {
                boolean selected = tile.tile() == state.selectedVillageTile;
                actionButton(listG, x, rowY, w, rowH - 8, "", () -> state.selectVillageTile(tile.tile()),
                        selected ? new Color(41, 61, 48, 242) : new Color(24, 29, 41, 235),
                        selected ? new Color(126, 176, 95) : new Color(86, 98, 128), true);
                listG.drawImage(assets.tile(tile.tile(), 38), x + 8, rowY + 6, null);
                listG.setFont(new Font("SansSerif", Font.BOLD, 13));
                listG.setColor(new Color(238, 239, 244));
                drawClippedString(listG, tile.label(), x + 56, rowY + 20, w - 64);
                listG.setFont(new Font("SansSerif", Font.PLAIN, 11));
                listG.setColor(new Color(176, 182, 196));
                drawClippedString(listG, villageCostDisplay(tile.cost()), x + 56, rowY + 38, w - 64);
            }
            rowY += rowH;
        }
        listG.dispose();
        drawScrollIndicator(g, x + w + 4, y, h, tiles.size(), scroll, visibleRows);
    }

    private void drawVillageSidebarProps(Graphics2D g, int x, int y, int w, int h) {
        List<VillageManager.PlaceableAsset> props = filteredPlaceableAssets(outdoorAssetsForCurrentEditor(state.selectedVillagePropCategory));
        drawVillageSidebarAssetRows(g, x, y, w, h, props, false);
    }

    private void drawVillageSidebarInteriors(Graphics2D g, int x, int y, int w, int h) {
        drawVillageSidebarAssetRows(g, x, y, w, h,
                filteredPlaceableAssets(VillageManager.interiorAssets(state.selectedInteriorAssetCategory)), true);
    }

    private void drawVillageSidebarAssetRows(Graphics2D g, int x, int y, int w, int h, List<VillageManager.PlaceableAsset> items, boolean interior) {
        int rowH = 58;
        int visibleRows = Math.max(1, h / rowH);
        int scroll = clamp(villageListScroll, 0, Math.max(0, items.size() - visibleRows));
        villageListScroll = scroll;
        Graphics2D listG = (Graphics2D) g.create();
        listG.setClip(x, y, w, h);
        int rowY = y - scroll * rowH;
        for (VillageManager.PlaceableAsset item : items) {
            if (rowY + rowH > y && rowY < y + h) {
                boolean selected = interior ? item.asset().equals(state.selectedInteriorAsset) : item.asset().equals(state.selectedVillageAsset);
                actionButton(listG, x, rowY, w, rowH - 8, "",
                        () -> {
                            if (interior) {
                                state.selectInteriorAsset(item.asset());
                            } else {
                                state.selectVillageAsset(item.asset());
                            }
                        },
                        selected ? new Color(41, 61, 48, 242) : new Color(24, 29, 41, 235),
                        selected ? new Color(126, 176, 95) : new Color(86, 98, 128), true);
                listG.drawImage(assets.spriteFit(item.asset(), 40, 40), x + 8, rowY + 5, null);
                listG.setFont(new Font("SansSerif", Font.BOLD, 13));
                listG.setColor(new Color(238, 239, 244));
                drawClippedString(listG, item.label(), x + 56, rowY + 20, w - 64);
                listG.setFont(new Font("SansSerif", Font.PLAIN, 11));
                listG.setColor(new Color(176, 182, 196));
                drawClippedString(listG, villageCostDisplay(item.cost()), x + 56, rowY + 38, w - 64);
            }
            rowY += rowH;
        }
        listG.dispose();
        drawScrollIndicator(g, x + w + 4, y, h, items.size(), scroll, visibleRows);
    }

    private void drawVillageSidebarWorkers(Graphics2D g, int x, int y, int w, int h) {
        List<Actor> allies = new ArrayList<>();
        allies.addAll(state.stationedAllies());
        allies.addAll(state.activeAllies());
        if (allies.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.setColor(new Color(176, 182, 196));
            drawWrapped(g, "No allies available for village work.", x, y + 22, w, 16, 3);
            return;
        }
        int rowH = 62;
        int visibleRows = Math.max(1, h / rowH);
        int scroll = clamp(villageListScroll, 0, Math.max(0, allies.size() - visibleRows));
        villageListScroll = scroll;
        Graphics2D listG = (Graphics2D) g.create();
        listG.setClip(x, y, w, h);
        int rowY = y - scroll * rowH;
        for (Actor ally : allies) {
            if (rowY + rowH > y && rowY < y + h) {
                boolean stationed = state.villageAllies.contains(ally.name);
                listG.setColor(new Color(24, 29, 41, 235));
                listG.fillRoundRect(x, rowY, w, rowH - 8, 8, 8);
                listG.setColor(stationed ? new Color(126, 176, 95) : new Color(86, 98, 128));
                listG.drawRoundRect(x, rowY, w, rowH - 8, 8, 8);
                listG.drawImage(assets.spriteFit(ally.worldSprite, 34, 42), x + 8, rowY + 5, null);
                listG.setFont(new Font("SansSerif", Font.BOLD, 13));
                listG.setColor(new Color(238, 239, 244));
                drawClippedString(listG, ally.name, x + 50, rowY + 19, w - 112);
                listG.setFont(new Font("SansSerif", Font.PLAIN, 11));
                listG.setColor(new Color(176, 182, 196));
                drawClippedString(listG, stationed ? VillageManager.workerRole(state.villageRoleFor(ally.name)).label() : ally.className,
                        x + 50, rowY + 38, w - 112);
                actionButton(listG, x + w - 58, rowY + 14, 48, 24, stationed ? "Recall" : "Set",
                        () -> {
                            if (stationed) {
                                state.recallAlly(ally.name);
                            } else {
                                state.stationAlly(ally.name);
                            }
                        },
                        stationed ? new Color(57, 76, 60) : new Color(69, 62, 88),
                        stationed ? new Color(126, 176, 95) : new Color(125, 107, 166), true);
            }
            rowY += rowH;
        }
        listG.dispose();
        drawScrollIndicator(g, x + w + 4, y, h, allies.size(), scroll, visibleRows);
    }

    private void drawVillageManager(Graphics2D g) {
        int x = 24;
        int y = villagePanelTop();
        int w = gameAreaWidth() - 48;
        int h = viewHeight() - y - 24;
        drawOverlayBase(g, x, y, w, h);

        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.setColor(new Color(244, 239, 220));
        g.drawString("Oathstead Camp", x + 24, y + 38);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(new Color(198, 202, 211));
        g.drawString((state.isManagedVillageInterior() ? "Interior layout" : "Village layout")
                + " | Storage " + state.villageStorageUsed() + "/" + state.villageStorageCapacity()
                + " | Gold " + state.player.gold, x + 24, y + 62);

        String[] tabs = {"Build", "Tiles", "Props", "Inside", "Workers"};
        for (int i = 0; i < tabs.length; i++) {
            int tab = i;
            boolean selected = state.villageTab == i;
            actionButton(g, x + 214 + i * 88, y + 24, 78, 30, tabs[i], () -> state.setVillageTab(tab),
                    selected ? new Color(57, 76, 60) : new Color(35, 39, 54),
                    selected ? new Color(126, 176, 95) : new Color(86, 98, 128), true);
        }

        actionButton(g, x + w - 434, y + 24, 92, 30, "Place", () -> state.setVillageEditAction("place"),
                "place".equals(state.villageEditAction) ? new Color(57, 76, 60) : new Color(35, 39, 54),
                new Color(126, 176, 95), true);
        actionButton(g, x + w - 334, y + 24, 92, 30, "Move", () -> state.setVillageEditAction("move"),
                "move".equals(state.villageEditAction) ? new Color(71, 63, 42) : new Color(35, 39, 54),
                new Color(169, 137, 74), true);
        actionButton(g, x + w - 234, y + 24, 92, 30, "Upgrade", () -> state.setVillageEditAction("upgrade"),
                "upgrade".equals(state.villageEditAction) ? new Color(69, 62, 88) : new Color(35, 39, 54),
                new Color(125, 107, 166), true);
        actionButton(g, x + w - 134, y + 24, 92, 30, "Delete", () -> state.setVillageEditAction("delete"),
                "delete".equals(state.villageEditAction) ? new Color(84, 50, 50) : new Color(35, 39, 54),
                new Color(149, 96, 88), true);

        if (state.villageTab == 0) {
            drawVillageBuildingTools(g, x + 24, y + 92, w - 48);
        } else if (state.villageTab == 1) {
            drawVillageTileTools(g, x + 24, y + 92, w - 48);
        } else if (state.villageTab == 2) {
            drawVillageAssetTools(g, x + 24, y + 92, w - 48);
        } else if (state.villageTab == 3) {
            drawVillageInteriorTools(g, x + 24, y + 92, w - 48);
        } else {
            drawVillageAllyTools(g, x + 24, y + 88, w - 48);
        }

        actionButton(g, x + w - 118, y + h - 42, 82, 28, "Close", state::toggleVillage,
                new Color(58, 72, 100), new Color(107, 126, 166), true);
    }

    private void drawBuildingAssignment(Graphics2D g) {
        CityBuilding building = state.activeVillageBuilding;
        int panelW = 760;
        int panelH = 560;
        int x = gameAreaCenteredX(panelW);
        int y = Math.max(54, (viewHeight() - panelH) / 2);
        drawOverlayBase(g, x, y, panelW, panelH);
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        g.setColor(new Color(244, 239, 220));
        String title = building == null ? "Building Assignment" : VillageManager.buildingLabel(building.style());
        g.drawString(title, x + 32, y + 46);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(new Color(198, 202, 211));
        VillageManager.WorkerRole buildingRole = building == null
                ? VillageManager.workerRole("idle")
                : VillageManager.workerRole(VillageManager.buildingPlan(building.style()).workerRole());
        String assigned = building == null ? "" : state.assignedAllyForBuilding(building.key());
        String assignment = assigned.isBlank() ? "Unassigned" : "Assigned: " + assigned;
        g.drawString(assignment + " | Work: " + buildingRole.label(), x + 32, y + 74);
        drawWrapped(g, buildingRole.description(), x + 32, y + 94, panelW - 64, 17, 2);
        if (building != null && building.key().startsWith("player_")) {
            g.setColor(new Color(151, 177, 112));
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            drawClippedString(g, state.buildingInteriorSummary(building), x + 32, y + 132, panelW - 64);
        }

        int listX = x + 32;
        int listY = y + 156;
        int rowH = 66;
        List<Actor> workers = state.stationedAllies();
        if (workers.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 15));
            g.setColor(new Color(176, 182, 196));
            drawWrapped(g, "No companions are set to city attendance. Open Village > Workers and station someone first.", listX, listY + 24, panelW - 64, 18, 3);
        }
        int maxRows = 5;
        for (int i = 0; i < Math.min(maxRows, workers.size()); i++) {
            Actor ally = workers.get(i);
            int rowY = listY + i * rowH;
            boolean selected = building != null && ally.name.equals(state.assignedAllyForBuilding(building.key()));
            String otherBuilding = state.buildingAssignmentForAlly(ally.name);
            g.setColor(new Color(28, 32, 43, 235));
            g.fillRoundRect(listX, rowY, panelW - 64, rowH - 10, 8, 8);
            g.setColor(selected ? new Color(126, 176, 95) : new Color(86, 98, 128));
            g.drawRoundRect(listX, rowY, panelW - 64, rowH - 10, 8, 8);
            g.drawImage(assets.spriteFit(ally.worldSprite, 38, 48), listX + 10, rowY + 4, null);
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.setColor(new Color(235, 236, 240));
            drawClippedString(g, ally.name, listX + 60, rowY + 20, panelW - 260);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.setColor(new Color(176, 182, 196));
            String detail = professionSummary(ally);
            if (!otherBuilding.isBlank() && (building == null || !otherBuilding.equals(building.key()))) {
                CityBuilding other = state.playerVillageBuildingByKey(otherBuilding);
                detail = "At " + (other == null ? "another building" : VillageManager.buildingLabel(other.style())) + " | " + detail;
            }
            drawClippedString(g, shortText(detail, 84), listX + 60, rowY + 40, panelW - 260);
            String allyName = ally.name;
            actionButton(g, listX + panelW - 190, rowY + 15, 104, 28, selected ? "Assigned" : "Assign",
                    () -> state.assignAllyToActiveBuilding(allyName),
                    selected ? new Color(57, 76, 60) : new Color(69, 62, 88),
                    selected ? new Color(126, 176, 95) : new Color(125, 107, 166), building != null);
        }
        if (workers.size() > maxRows) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.setColor(new Color(176, 182, 196));
            g.drawString("Open Village > Workers for the full city-attendance roster.", listX, listY + maxRows * rowH + 8);
        }

        actionButton(g, x + 32, y + panelH - 54, 138, 34, "Enter Inside", state::enterActiveVillageBuilding,
                new Color(52, 79, 92), new Color(92, 140, 160), building != null);
        actionButton(g, x + 188, y + panelH - 54, 118, 34, "Open Shop", state::openActiveVillageBuildingShop,
                new Color(57, 76, 60), new Color(126, 176, 95), building != null && !assigned.isBlank());
        actionButton(g, x + panelW - 330, y + panelH - 54, 138, 34, "Clear", state::clearActiveBuildingAssignment,
                new Color(83, 61, 61), new Color(149, 96, 88), building != null && !assigned.isBlank());
        actionButton(g, x + panelW - 172, y + panelH - 54, 138, 34, "Close", state::closeOverlay,
                new Color(58, 72, 100), new Color(107, 126, 166), true);
    }

    private void drawVillageBuildingTools(Graphics2D g, int x, int y, int w) {
        int gap = 12;
        int idealCardW = 150;
        int perRow = Math.max(3, w / (idealCardW + gap));
        int cardW = Math.max(144, (w - gap * (perRow - 1)) / perRow);
        int cardH = 116;
        List<VillageManager.BuildingPlan> plans = VillageManager.buildingPlans();
        for (int i = 0; i < plans.size(); i++) {
            VillageManager.BuildingPlan plan = plans.get(i);
            int cardX = x + (i % perRow) * (cardW + gap);
            int cardY = y + (i / perRow) * (cardH + gap);
            drawVillageBuildingCard(g, plan, cardX, cardY, cardW, cardH);
        }
        int rows = Math.max(1, (plans.size() + perRow - 1) / perRow);
        int infoY = y + rows * (cardH + gap) + 14;
        VillageManager.BuildingPlan selected = VillageManager.buildingPlan(state.selectedVillageBuildingStyle);
        g.setColor(new Color(14, 17, 25, 190));
        g.fillRoundRect(x, infoY - 18, w, 68, 8, 8);
        g.setColor(new Color(82, 92, 116));
        g.drawRoundRect(x, infoY - 18, w, 68, 8, 8);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(new Color(246, 224, 151));
        drawClippedString(g, selected.label() + "  " + selected.width() + "x" + selected.depth()
                + "  " + villageCostDisplay(selected.cost()), x + 14, infoY + 2, w - 28);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(198, 202, 211));
        drawClippedString(g, selected.description(), x + 14, infoY + 23, w - 28);
        drawVillageStorageSummary(g, x + 14, infoY + 44, w - 28);
        drawVillagePlacementStatus(g, x, infoY + 78, w);
    }

    private void drawVillageBuildingCard(Graphics2D g, VillageManager.BuildingPlan plan, int x, int y, int w, int h) {
        boolean selected = plan.style().equals(state.selectedVillageBuildingStyle);
        boolean affordable = state.canAffordVillageCost(plan.cost());
        Rectangle bounds = new Rectangle(x, y, w, h);
        buttons.add(new UiButton(bounds, "village-build:" + plan.style(), () -> state.selectVillageBuildingStyle(plan.style())));

        Color fill = selected ? new Color(41, 61, 48, 242) : new Color(24, 29, 41, 235);
        Color border = selected ? new Color(126, 176, 95) : affordable ? new Color(86, 98, 128) : new Color(92, 74, 74);
        g.setColor(fill);
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(border);
        g.drawRoundRect(x, y, w, h, 8, 8);

        g.setColor(new Color(9, 12, 18, 170));
        int previewX = x + (w - 62) / 2;
        g.fillRoundRect(previewX, y + 9, 62, 54, 7, 7);
        g.setColor(new Color(71, 80, 96, 170));
        g.drawRoundRect(previewX, y + 9, 62, 54, 7, 7);
        g.drawImage(assets.spriteFit(buildingPreviewSprite(plan), 56, 50), previewX + 3, y + 11, null);

        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(238, 239, 244));
        drawCenteredIn(g, plan.label(), x + 8, y + 76, w - 16);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(affordable ? new Color(176, 182, 196) : new Color(222, 143, 132));
        drawCenteredIn(g, plan.width() + "x" + plan.depth() + " | " + villageCostDisplay(plan.cost()), x + 8, y + 93, w - 16);

        String utility = plan.storageCapacity() > 0
                ? "Storage +" + plan.storageCapacity()
                : plan.workerRole().isBlank() ? "Max Lv " + plan.maxLevel() : VillageManager.workerRole(plan.workerRole()).label();
        g.setColor(new Color(151, 177, 112));
        drawCenteredIn(g, utility, x + 8, y + 109, w - 16);

        if (!affordable) {
            g.setColor(new Color(8, 10, 14, 110));
            g.fillRoundRect(x, y, w, h, 8, 8);
            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            g.setColor(new Color(238, 190, 164));
            drawCenteredIn(g, "Need materials", x, y + h - 10, w);
        }
        tooltipZones.add(new TooltipZone(bounds, plan.label(), plan.description() + " " + villageCostDisplay(plan.cost()) + "."));
    }

    private String buildingPreviewSprite(VillageManager.BuildingPlan plan) {
        if (plan.sprites().isEmpty()) {
            return "city_building_town_gabled";
        }
        return plan.sprites().get(0);
    }

    private String villageCostDisplay(VillageManager.VillageCost cost) {
        if (state.config.creativeBuildMode) {
            String planned = VillageManager.costLabel(cost);
            return "Free now" + ("Free".equals(planned) ? "" : " | Plan: " + planned);
        }
        return "Cost: " + VillageManager.costLabel(cost);
    }

    private void drawVillageTileTools(Graphics2D g, int x, int y, int w) {
        int buttonW = 118;
        int buttonH = 64;
        int gap = 14;
        List<VillageManager.TilePlan> tiles = VillageManager.tilePlans();
        for (int i = 0; i < tiles.size(); i++) {
            VillageManager.TilePlan option = tiles.get(i);
            boolean selected = option.tile() == state.selectedVillageTile;
            int buttonX = x + i * (buttonW + gap);
            actionButton(g, buttonX, y, buttonW, buttonH, "", () -> state.selectVillageTile(option.tile()),
                    selected ? new Color(57, 76, 60) : new Color(28, 32, 43),
                    selected ? new Color(126, 176, 95) : new Color(82, 92, 116), true);
            g.drawImage(assets.tile(option.tile(), 34), buttonX + 42, y + 6, null);
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g.setColor(new Color(222, 225, 232));
            drawCenteredIn(g, option.label(), buttonX, y + 54, buttonW);
            g.setColor(new Color(176, 182, 196));
            drawCenteredIn(g, VillageManager.costLabel(option.cost()), buttonX, y + 78, buttonW);
        }
        drawVillagePlacementStatus(g, x, y + buttonH + 42, w);
    }

    private void drawVillageAssetTools(Graphics2D g, int x, int y, int w) {
        int buttonW = 106;
        int buttonH = 66;
        int gap = 12;
        int perRow = Math.max(1, w / (buttonW + gap));
        List<String> categories = VillageManager.outdoorAssetCategories();
        int categoryW = 86;
        int categoryH = 28;
        int categoryPerRow = Math.max(1, w / (categoryW + gap));
        for (int i = 0; i < categories.size(); i++) {
            String category = categories.get(i);
            boolean selected = category.equals(state.selectedVillagePropCategory);
            int buttonX = x + (i % categoryPerRow) * (categoryW + gap);
            int buttonY = y + (i / categoryPerRow) * (categoryH + 8);
            actionButton(g, buttonX, buttonY, categoryW, categoryH, category, () -> state.selectVillagePropCategory(category),
                    selected ? new Color(57, 76, 60) : new Color(28, 32, 43),
                    selected ? new Color(126, 176, 95) : new Color(82, 92, 116), true);
        }
        int categoryRows = Math.max(1, (categories.size() + categoryPerRow - 1) / categoryPerRow);
        int assetY = y + categoryRows * (categoryH + 8) + 10;
        List<VillageManager.PlaceableAsset> assetsList = VillageManager.outdoorAssets(state.selectedVillagePropCategory);
        for (int i = 0; i < assetsList.size(); i++) {
            VillageManager.PlaceableAsset option = assetsList.get(i);
            String asset = option.asset();
            boolean selected = asset.equals(state.selectedVillageAsset);
            int buttonX = x + (i % perRow) * (buttonW + gap);
            int buttonY = assetY + (i / perRow) * (buttonH + 20);
            actionButton(g, buttonX, buttonY, buttonW, buttonH, "", () -> state.selectVillageAsset(asset),
                    selected ? new Color(57, 76, 60) : new Color(28, 32, 43),
                    selected ? new Color(126, 176, 95) : new Color(82, 92, 116), true);
            g.drawImage(assets.spriteFit(asset, 42, 42), buttonX + 32, buttonY + 7, null);
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g.setColor(new Color(222, 225, 232));
            drawCenteredIn(g, assetLabel(option.label()), buttonX, buttonY + 58, buttonW);
        }
        int rows = Math.max(1, (assetsList.size() + perRow - 1) / perRow);
        drawVillageStorageSummary(g, x, assetY + rows * (buttonH + 20) + 10, w);
        drawVillagePlacementStatus(g, x, assetY + rows * (buttonH + 20) + 34, w);
    }

    private void drawVillageInteriorTools(Graphics2D g, int x, int y, int w) {
        int buttonW = 98;
        int buttonH = 64;
        int gap = 14;
        int perRow = Math.max(1, w / (buttonW + gap));
        List<VillageManager.PlaceableAsset> interiors = VillageManager.interiorAssets();
        for (int i = 0; i < interiors.size(); i++) {
            VillageManager.PlaceableAsset option = interiors.get(i);
            String asset = option.asset();
            boolean selected = asset.equals(state.selectedInteriorAsset);
            int buttonX = x + (i % perRow) * (buttonW + gap);
            int buttonY = y + (i / perRow) * (buttonH + 18);
            actionButton(g, buttonX, buttonY, buttonW, buttonH, "", () -> state.selectInteriorAsset(asset),
                    selected ? new Color(57, 76, 60) : new Color(28, 32, 43),
                    selected ? new Color(126, 176, 95) : new Color(82, 92, 116), true);
            g.drawImage(assets.spriteFit(asset, 42, 42), buttonX + 28, buttonY + 8, null);
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g.setColor(new Color(222, 225, 232));
            drawCenteredIn(g, assetLabel(option.label()), buttonX, buttonY + 58, buttonW);
        }
        int rows = Math.max(1, (interiors.size() + perRow - 1) / perRow);
        drawVillagePlacementStatus(g, x, y + rows * (buttonH + 18) + 14, w);
    }

    private void drawVillageAllyTools(Graphics2D g, int x, int y, int w) {
        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.setColor(new Color(246, 224, 151));
        g.drawString("Traveling", x, y);
        g.drawString("City Attendance", x + w / 2, y);
        drawVillageStorageSummary(g, x, y + 232, w);
        List<Actor> stationable = state.allies.stream()
                .filter(ally -> !state.villageAllies.contains(ally.name))
                .toList();
        drawVillageAllyColumn(g, stationable, x, y + 18, w / 2 - 18, true);
        drawVillageAllyColumn(g, state.stationedAllies(), x + w / 2, y + 18, w / 2 - 18, false);
    }

    private void drawVillageAllyColumn(Graphics2D g, List<Actor> allies, int x, int y, int w, boolean station) {
        if (allies.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.setColor(new Color(176, 182, 196));
            g.drawString("No allies here.", x, y + 24);
            return;
        }
        int rowY = y;
        for (Actor ally : allies) {
            g.setColor(new Color(28, 32, 43, 235));
            g.fillRoundRect(x, rowY, w, 46, 8, 8);
            g.setColor(new Color(82, 92, 116));
            g.drawRoundRect(x, rowY, w, 46, 8, 8);
            g.drawImage(assets.spriteFit(ally.worldSprite, 34, 42), x + 8, rowY + 2, null);
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.setColor(new Color(235, 236, 240));
            g.drawString(ally.name, x + 50, rowY + 19);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.setColor(new Color(176, 182, 196));
            String role = station ? ally.className + "  Lv " + ally.level
                    : ally.className + "  Lv " + ally.level + " | " + VillageManager.workerRole(state.villageRoleFor(ally.name)).label();
            g.drawString(role, x + 50, rowY + 35);
            String name = ally.name;
            actionButton(g, x + w - 94, rowY + 9, 78, 28, station ? "Station" : "Recall",
                    () -> {
                        if (station) {
                            state.stationAlly(name);
                        } else {
                            state.recallAlly(name);
                        }
                    },
                    station ? new Color(69, 62, 88) : new Color(57, 76, 60),
                    station ? new Color(125, 107, 166) : new Color(126, 176, 95), true);
            if (!station) {
                drawWorkerRoleButtons(g, name, x + 50, rowY + 50, w - 58);
                rowY += 90;
            } else {
                rowY += 54;
            }
            if (rowY > villagePanelTop() + 222) {
                break;
            }
        }
    }

    private void drawWorkerRoleButtons(Graphics2D g, String allyName, int x, int y, int w) {
        String[][] roles = {
                {"idle", "Idle"},
                {"farmer", "Farm"},
                {"forester", "Wood"},
                {"miner", "Mine"},
                {"hunter", "Hunt"},
                {"hauler", "Haul"},
                {"builder", "Build"}
        };
        int buttonW = Math.max(38, Math.min(52, (w - 6 * 6) / roles.length));
        for (int i = 0; i < roles.length; i++) {
            String roleId = roles[i][0];
            boolean selected = roleId.equals(state.villageRoleFor(allyName));
            actionButton(g, x + i * (buttonW + 6), y, buttonW, 22, roles[i][1],
                    () -> state.assignVillageRole(allyName, roleId),
                    selected ? new Color(57, 76, 60) : new Color(35, 39, 54),
                    selected ? new Color(126, 176, 95) : new Color(86, 98, 128), true);
        }
    }

    private void drawVillageStorageSummary(Graphics2D g, int x, int y, int w) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(198, 202, 211));
        StringBuilder line = new StringBuilder("Stores: ");
        String[] resources = {
                "wood", "stone", "iron_ore", "coal", "flint", "plant_fiber", "herb_seed", "herb_leaf",
                "trail_rations", "garden_vegetables", "wild_meat", "raw_fish", "skin", "bone", "horn",
                "seashell", "shell_lure", "ember_shard"
        };
        boolean any = false;
        for (String resource : resources) {
            int amount = state.villageStoredAmount(resource);
            if (amount <= 0) {
                continue;
            }
            if (any) {
                line.append(" | ");
            }
            line.append(GameData.itemName(resource)).append(" ").append(amount);
            any = true;
        }
        if (!any) {
            line.append("empty");
        }
        drawClippedString(g, line.toString(), x, y, w);
    }

    private void drawVillagePlacementStatus(Graphics2D g, int x, int y, int w) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(new Color(198, 202, 211));
        String target = state.villageTab == 3 ? "interior tiles" : "village tiles";
        String mode = state.villageEditMode ? state.villageEditAction : "place";
        drawClippedString(g, "Mode: " + mode + "   Target: " + target + "   " + state.status, x, y, w);
        if (state.pendingVillageMoveSource != null) {
            g.setColor(new Color(246, 224, 151));
            drawClippedString(g, "Moving from " + state.pendingVillageMoveSource.x() + ", "
                    + state.pendingVillageMoveSource.y(), x, y + 22, w);
        }
    }

    private String assetLabel(String asset) {
        String cleaned = asset.replace("city_prop_", "").replace("interior_", "").replace("deco_", "").replace('_', ' ');
        if (cleaned.length() > 15) {
            cleaned = cleaned.substring(0, 14) + ".";
        }
        return cleaned;
    }

    private void drawSettlementBoard(Graphics2D g) {
        int panelW = Math.min(920, gameAreaWidth() - 96);
        int panelH = 620;
        int x = gameAreaCenteredX(panelW);
        int y = centeredY(panelH);
        drawOverlayBase(g, x, y, panelW, panelH);

        VillageManager.SettlementStage stage = state.villageStage();
        g.drawImage(assets.spriteFit(stage.asset(), 128, 104), x + 34, y + 26, null);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(244, 239, 220));
        g.drawString(stage.title(), x + 184, y + 54);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(new Color(198, 202, 211));
        drawClippedString(g, "Stage " + stage.stage() + "/10 | " + stage.kind()
                + " | Storage " + state.villageStorageUsed() + "/" + state.villageStorageCapacity()
                + " | Workers " + state.villageAllies.size(), x + 184, y + 80, panelW - 240);
        drawWrapped(g, stage.description(), x + 184, y + 104, panelW - 240, 18, 2);

        int tabY = y + 146;
        actionButton(g, x + 34, tabY, 132, 30, "Growth", () -> state.setSettlementBoardTab(0),
                state.settlementBoardTab == 0 ? new Color(57, 76, 60) : new Color(35, 39, 54),
                state.settlementBoardTab == 0 ? new Color(126, 176, 95) : new Color(86, 98, 128), true);
        actionButton(g, x + 174, tabY, 150, 30, "Recruitment", () -> state.setSettlementBoardTab(1),
                state.settlementBoardTab == 1 ? new Color(57, 76, 60) : new Color(35, 39, 54),
                state.settlementBoardTab == 1 ? new Color(126, 176, 95) : new Color(86, 98, 128), true);

        if (state.settlementBoardTab == 0) {
            drawSettlementGrowthTab(g, x + 34, y + 198, panelW - 68, panelH - 260);
        } else {
            drawSettlementRecruitmentTab(g, x + 34, y + 198, panelW - 68, panelH - 260);
        }

        g.setColor(new Color(11, 13, 20, 210));
        g.fillRoundRect(x + 34, y + panelH - 52, panelW - 68, 32, 8, 8);
        g.setColor(new Color(74, 82, 105));
        g.drawRoundRect(x + 34, y + panelH - 52, panelW - 68, 32, 8, 8);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(198, 202, 211));
        drawClippedString(g, state.status, x + 46, y + panelH - 31, panelW - 190);
        actionButton(g, x + panelW - 142, y + panelH - 48, 96, 24, "Close", state::closeOverlay,
                new Color(58, 72, 100), new Color(107, 126, 166), true);
    }

    private void drawFastTravel(Graphics2D g) {
        List<GameState.FastTravelDestination> destinations = state.fastTravelDestinations();
        int panelW = Math.min(980, gameAreaWidth() - 96);
        int panelH = 650;
        int x = gameAreaCenteredX(panelW);
        int y = centeredY(panelH);
        drawOverlayBase(g, x, y, panelW, panelH);

        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(244, 239, 220));
        g.drawString("Town Portal", x + 38, y + 48);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(new Color(198, 202, 211));
        drawClippedString(g, state.world.label(state.currentMapId) + " | Gold " + state.player.gold,
                x + 38, y + 76, panelW - 76);

        int listX = x + 34;
        int listY = y + 108;
        int gap = 14;
        int cols = 2;
        int rowH = 54;
        int colW = (panelW - 68 - gap) / cols;
        for (int i = 0; i < destinations.size(); i++) {
            GameState.FastTravelDestination destination = destinations.get(i);
            int col = i % cols;
            int row = i / cols;
            int bx = listX + col * (colW + gap);
            int by = listY + row * rowH;
            if (by + rowH > y + panelH - 70) {
                break;
            }
            boolean affordable = state.player.gold >= destination.cost();
            actionButton(g, bx, by, colW, rowH - 8, "",
                    () -> state.fastTravelTo(destinationIndex(destination.mapId())),
                    affordable ? new Color(29, 38, 52) : new Color(42, 34, 38),
                    affordable ? new Color(94, 126, 162) : new Color(112, 77, 82), true);
            g.drawImage(assets.spriteFit(destination.portalAsset(), 42, 42), bx + 8, by + 3, null);
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.setColor(affordable ? new Color(239, 241, 229) : new Color(176, 154, 154));
            drawClippedString(g, (i + 1) + ". " + destination.label(), bx + 60, by + 20, colW - 144);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.setColor(new Color(183, 190, 205));
            drawClippedString(g, destination.kind(), bx + 60, by + 38, colW - 144);
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.setColor(affordable ? new Color(246, 224, 151) : new Color(220, 135, 126));
            drawClippedString(g, destination.cost() + "g", bx + colW - 68, by + 28, 52);
        }

        g.setColor(new Color(11, 13, 20, 210));
        g.fillRoundRect(x + 34, y + panelH - 52, panelW - 68, 32, 8, 8);
        g.setColor(new Color(74, 82, 105));
        g.drawRoundRect(x + 34, y + panelH - 52, panelW - 68, 32, 8, 8);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(198, 202, 211));
        drawClippedString(g, state.status, x + 46, y + panelH - 31, panelW - 190);
        actionButton(g, x + panelW - 142, y + panelH - 48, 96, 24, "Close", state::closeOverlay,
                new Color(58, 72, 100), new Color(107, 126, 166), true);
    }

    private int destinationIndex(String mapId) {
        List<GameState.FastTravelDestination> destinations = state.fastTravelDestinations();
        for (int i = 0; i < destinations.size(); i++) {
            if (destinations.get(i).mapId().equals(mapId)) {
                return i;
            }
        }
        return -1;
    }

    private void drawSettlementGrowthTab(Graphics2D g, int x, int y, int w, int h) {
        VillageManager.SettlementStage next = state.nextVillageStage();
        g.setFont(new Font("SansSerif", Font.BOLD, 17));
        g.setColor(new Color(246, 224, 151));
        g.drawString(next == null ? "Oathstead is fully grown" : "Next: " + next.title(), x, y);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(198, 202, 211));
        drawWrapped(g, next == null ? "The board has no larger charter to post yet."
                : next.description(), x, y + 24, w, 17, 2);

        int rowY = y + 78;
        if (next == null) {
            drawSettlementProgressRow(g, x, rowY, w, "Player level", state.player.level, state.player.level);
            drawSettlementProgressRow(g, x, rowY + 44, w, "Buildings", state.villageBuildingCount(), state.villageBuildingCount());
            drawSettlementProgressRow(g, x, rowY + 88, w, "Completed quests", state.completedQuestCount(), state.completedQuestCount());
            return;
        }
        drawSettlementProgressRow(g, x, rowY, w, "Player level", state.player.level, next.requiredPlayerLevel());
        drawSettlementProgressRow(g, x, rowY + 44, w, "Buildings", state.villageBuildingCount(), next.requiredBuildings());
        drawSettlementProgressRow(g, x, rowY + 88, w, "Stationed workers", state.villageAllies.size(), next.requiredAllies());
        drawSettlementProgressRow(g, x, rowY + 132, w, "Completed quests", state.completedQuestCount(), next.requiredCompletedQuests());
        drawSettlementProgressRow(g, x, rowY + 176, w, "Stored resources", state.villageStorageUsed(), next.requiredStorageUsed());
        drawSettlementProgressRow(g, x, rowY + 220, w, "Developed tiles", state.villageDevelopedTileCount(), next.requiredDevelopedTiles());
    }

    private void drawSettlementProgressRow(Graphics2D g, int x, int y, int w, String label, int value, int target) {
        int barX = x + 186;
        int barW = w - 260;
        int clampedTarget = Math.max(1, target);
        double ratio = Math.min(1.0, value / (double) clampedTarget);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(235, 236, 240));
        drawClippedString(g, label, x, y + 18, 170);
        g.setColor(new Color(19, 23, 32, 235));
        g.fillRoundRect(barX, y + 3, barW, 20, 8, 8);
        g.setColor(new Color(79, 124, 84));
        g.fillRoundRect(barX, y + 3, (int) Math.round(barW * ratio), 20, 8, 8);
        g.setColor(new Color(91, 102, 128));
        g.drawRoundRect(barX, y + 3, barW, 20, 8, 8);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(value >= target ? new Color(169, 220, 142) : new Color(198, 202, 211));
        drawClippedString(g, value + "/" + target, barX + barW + 14, y + 18, 60);
    }

    private void drawSettlementRecruitmentTab(Graphics2D g, int x, int y, int w, int h) {
        List<GameState.VillageRecruitOption> options = state.settlementRecruitOptions();
        g.setFont(new Font("SansSerif", Font.BOLD, 17));
        g.setColor(new Color(246, 224, 151));
        g.drawString("Recruitment Notices", x, y);
        actionButton(g, x + w - 120, y - 21, 116, 26, "Refresh", state::rerollSettlementRecruits,
                new Color(58, 72, 100), new Color(107, 126, 166), true);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(198, 202, 211));
        drawClippedString(g, "Cost: free now. Gold prices can be enabled later from the same recruit entries.", x, y + 25, w - 16);

        int rowY = y + 54;
        int rowH = 78;
        for (int i = 0; i < options.size(); i++) {
            GameState.VillageRecruitOption option = options.get(i);
            int ry = rowY + i * (rowH + 12);
            g.setColor(new Color(24, 29, 41, 235));
            g.fillRoundRect(x, ry, w, rowH, 8, 8);
            g.setColor(new Color(86, 98, 128));
            g.drawRoundRect(x, ry, w, rowH, 8, 8);
            g.drawImage(assets.spriteFit(option.sprite(), 54, 62), x + 12, ry + 8, null);
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.setColor(new Color(238, 239, 244));
            drawClippedString(g, option.name(), x + 78, ry + 23, w - 248);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.setColor(new Color(176, 182, 196));
            drawClippedString(g, option.className() + "  Lv " + option.level()
                    + " | HP " + option.hp() + " MP " + option.mp()
                    + " | ATK " + option.attack() + " DEF " + option.defense(), x + 78, ry + 45, w - 248);
            String cost = option.goldCost() <= 0 ? "Free" : option.goldCost() + "g";
            int index = i;
            actionButton(g, x + w - 134, ry + 22, 106, 30, "Recruit " + cost,
                    () -> state.recruitSettlementNpc(index),
                    new Color(57, 76, 60), new Color(126, 176, 95), state.player.gold >= option.goldCost());
        }
    }

    private void drawShop(Graphics2D g) {
        Shop shop = state.activeShop;
        if (shop == null) {
            return;
        }
        int panelW = 1060;
        int panelX = gameAreaCenteredX(panelW);
        int panelY = 72;
        int panelH = 704;
        drawOverlayBase(g, panelX, panelY, panelW, panelH);
        g.setFont(new Font("SansSerif", Font.BOLD, 30));
        g.setColor(new Color(244, 239, 220));
        g.drawString(shop.name(), panelX + 44, panelY + 54);
        g.setFont(new Font("SansSerif", Font.PLAIN, 17));
        g.setColor(new Color(210, 213, 222));
        g.drawString("Gold: " + state.player.gold, panelX + 44, panelY + 88);
        g.setColor(new Color(96, 88, 72, 120));
        g.drawLine(panelX + 44, panelY + 122, panelX + panelW - 44, panelY + 122);

        List<String> stock = shop.availableStock(state.player.level);
        List<String> inventory = new ArrayList<>(state.player.inventory.keySet());
        int columnY = panelY + 158;
        int columnH = panelH - 230;
        int gap = 28;
        int columnW = (panelW - 88 - gap) / 2;
        int shopX = panelX + 44;
        int inventoryX = shopX + columnW + gap;
        int rowH = 58;
        int visibleRows = Math.max(1, columnH / rowH);
        int shopMaxScroll = Math.max(0, stock.size() - visibleRows);
        shopItemScroll = Math.max(0, Math.min(shopItemScroll, shopMaxScroll));

        drawTradeColumnHeader(g, shopX, columnY - 28, columnW, "Shop Stock");
        int shopEnd = Math.min(stock.size(), shopItemScroll + visibleRows);
        for (int i = shopItemScroll; i < shopEnd; i++) {
            int buttonIndex = i;
            String itemKey = stock.get(i);
            Item item = GameData.ITEMS.get(itemKey);
            Equipment equipment = GameData.equipment(itemKey);
            int cost = GameData.itemCost(itemKey);
            if (cost <= 0) {
                continue;
            }
            boolean affordable = state.player.gold >= cost;
            int y = columnY + (i - shopItemScroll) * rowH;
            drawTradeRow(g, shopX, y, columnW, rowH - 8, itemKey,
                    equipment == null ? itemBenefit(item) : equipmentBenefit(equipment),
                    "Buy " + cost + "g", () -> state.buyShopItem(buttonIndex), affordable);
        }
        drawScrollIndicator(g, shopX + columnW + 4, columnY, visibleRows * rowH - 8, stock.size(), shopItemScroll, visibleRows);

        drawTradeColumnHeader(g, inventoryX, columnY - 28, columnW, "Your Inventory");
        int invEnd = Math.min(inventory.size(), shopItemScroll + visibleRows);
        for (int i = shopItemScroll; i < invEnd; i++) {
            int buttonIndex = i;
            String itemKey = inventory.get(i);
            Item item = GameData.ITEMS.get(itemKey);
            Equipment equipment = GameData.equipment(itemKey);
            int value = Math.max(1, GameData.itemCost(itemKey) / 2);
            int count = state.player.inventory.getOrDefault(itemKey, 0);
            int y = columnY + (i - shopItemScroll) * rowH;
            drawTradeRow(g, inventoryX, y, columnW, rowH - 8, itemKey,
                    "x" + count + "  " + (equipment == null ? itemBenefit(item) : equipmentBenefit(equipment)),
                    "Sell " + value + "g", () -> state.sellShopItem(buttonIndex), count > 0);
        }
        drawScrollIndicator(g, inventoryX + columnW + 4, columnY, visibleRows * rowH - 8, inventory.size(), shopItemScroll, visibleRows);

        if (state.activeNpc != null && state.activeNpc.recruitId() != null && state.activeNpc.recruitCost() > 0) {
            if (state.isRecruited(state.activeNpc.recruitId())) {
                g.setColor(new Color(144, 215, 150));
                g.setFont(new Font("SansSerif", Font.BOLD, 15));
                g.drawString(state.npcDisplayName(state.activeNpc) + " travels with you.", panelX + 44, panelY + panelH - 72);
            } else {
                actionButton(g, panelX + 44, panelY + panelH - 88, 258, 34,
                        "Hire " + state.npcDisplayName(state.activeNpc) + " - " + state.activeNpc.recruitCost() + "g",
                        state::hireActiveRecruit, new Color(69, 62, 88), new Color(125, 107, 166), true);
            }
        }
        g.setColor(new Color(196, 198, 205));
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString("Move goods between shop stock and your inventory with Buy/Sell. Wheel scrolls both columns.", panelX + 44, panelY + panelH - 32);
        actionButton(g, panelX + panelW - 176, panelY + panelH - 54, 132, 36, "Leave Shop", state::closeOverlay, new Color(83, 61, 61), new Color(149, 96, 88), true);
    }

    private void drawTradeColumnHeader(Graphics2D g, int x, int y, int w, String label) {
        g.setFont(new Font("SansSerif", Font.BOLD, 17));
        g.setColor(new Color(244, 213, 141));
        g.drawString(label, x + 4, y + 18);
        g.setColor(new Color(96, 88, 72, 120));
        g.drawLine(x, y + 26, x + w, y + 26);
    }

    private void drawTradeRow(Graphics2D g, int x, int y, int w, int h, String itemKey, String detail,
                              String action, Runnable runnable, boolean enabled) {
        drawShopRowBackground(g, x, y, w, h, enabled);
        Color rarity = rarityColorForItem(itemKey);
        g.setColor(new Color(238, 231, 207));
        g.fillRoundRect(x + 10, y + 7, 36, 36, 6, 6);
        g.setColor(rarity);
        g.drawRoundRect(x + 10, y + 7, 36, 36, 6, 6);
        g.drawImage(assets.sprite(GameData.itemIcon(itemKey), 30), x + 13, y + 10, null);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(rarity);
        drawClippedString(g, GameData.itemName(itemKey), x + 56, y + 19, w - 180);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(new Color(176, 182, 196));
        drawClippedString(g, detail, x + 56, y + 38, w - 180);
        actionButton(g, x + w - 112, y + 11, 94, 28, action, runnable,
                enabled ? new Color(68, 90, 53) : new Color(58, 59, 66), new Color(110, 139, 92), enabled);
    }

    private void drawNpcPortraitCard(Graphics2D g, Npc npc, int x, int y, int w, int h) {
        String displayName = state.npcDisplayName(npc);
        tooltipZones.add(new TooltipZone(new Rectangle(x, y, w, h), displayName, npcTooltip(npc)));
        g.setPaint(new GradientPaint(x, y, new Color(31, 36, 50, 238), x, y + h, new Color(13, 16, 24, 238)));
        g.fillRoundRect(x, y, w, h, 10, 10);
        g.setColor(new Color(133, 122, 91, 170));
        g.drawRoundRect(x, y, w, h, 10, 10);
        int imageSize = Math.min(w - 22, h - 38);
        g.drawImage(assets.sprite(npc.sprite(), imageSize), x + (w - imageSize) / 2, y + 10, null);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(new Color(244, 213, 141));
        drawCenteredIn(g, displayName, x, y + h - 14, w);
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
        if ("escape_scroll".equals(item.key())) {
            return item.rarityLine() + "   Escapes a dungeon instantly";
        }
        String effect = item.effectDescription() == null || item.effectDescription().isBlank()
                ? ""
                : " " + item.effectDescription();
        if (item.heal() > 0 && item.mp() > 0) {
            return item.rarityLine() + "   Restores " + item.heal() + " HP and " + item.mp() + " MP." + effect;
        }
        if (item.heal() > 0) {
            return item.rarityLine() + "   Restores " + item.heal() + " HP." + effect;
        }
        if (item.mp() > 0) {
            return item.rarityLine() + "   Restores " + item.mp() + " MP." + effect;
        }
        return item.rarityLine() + "   Field provision." + effect;
    }

    private Color rarityColorForItem(String itemKey) {
        Equipment equipment = GameData.equipment(itemKey);
        if (equipment != null) {
            return rarityColor(equipment.rarity());
        }
        Item item = GameData.ITEMS.get(itemKey);
        return item == null ? new Color(235, 236, 240) : rarityColor(item.rarity());
    }

    private Color rarityColor(ItemRarity rarity) {
        if (rarity == null) {
            return new Color(235, 236, 240);
        }
        return switch (rarity) {
            case COMMON -> new Color(238, 239, 244);
            case UNCOMMON -> new Color(109, 205, 122);
            case RARE -> new Color(94, 154, 255);
            case UNIQUE -> new Color(255, 165, 70);
            case LEGENDARY -> new Color(245, 86, 86);
        };
    }

    private String equipmentBenefit(Equipment equipment) {
        String stats = String.join("  ", equipment.statLines());
        String level = equipment.rarityLine() + "   " + equipment.levelRangeLine();
        return stats.isBlank()
                ? level + "   " + equipment.description()
                : level + "   " + equipment.slot() + "   " + stats;
    }

    private void drawWorldMap(Graphics2D g) {
        long worldMapStarted = renderMetrics.start();
        int x = 0;
        int y = 0;
        int w = gameAreaWidth();
        int h = viewHeight();
        drawOverlayBase(g, x, y, w, h);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(244, 239, 220));
        g.drawString("World Map", x + 32, y + 42);
        int legendW = 184;
        int gap = 20;
        int mapX = x + 24;
        int mapY = y + 64;
        int mapW = Math.max(720, w - legendW - gap - 48);
        int mapH = h - mapY - 24;
        worldMapViewport = updateWorldMapViewport(mapX, mapY, mapW, mapH);
        worldMapBounds = new Rectangle(mapX, mapY, mapW, mapH);
        g.setColor(new Color(8, 10, 16));
        g.fillRect(mapX, mapY, mapW, mapH);
        renderMetrics.sample("worldMap.pixels", (long) mapW * mapH);
        long terrainStarted = renderMetrics.start();
        g.drawImage(worldMapTerrainCache.image(state.world, mapW, mapH, state.worldMapKingdoms,
                worldMapViewport.worldX(), worldMapViewport.worldY(),
                worldMapViewport.worldW(), worldMapViewport.worldH()), mapX, mapY, null);
        renderMetrics.record("worldMap.terrain", terrainStarted);
        Shape oldClip = g.getClip();
        g.setClip(mapX, mapY, mapW, mapH);
        WorldMapOverlayRenderer.drawGrid(g, worldMapViewport);
        if (state.worldMapKingdoms) {
            WorldMapOverlayRenderer.drawKingdomLabels(g, worldMapViewport, state.world.kingdoms());
        }
        for (WorldMap.SettlementSite settlement : state.world.settlementSites()) {
            if (worldMapVisible(worldMapViewport, settlement.x(), settlement.y(), 8.0)) {
                int dx = settlement.x() > worldMapViewport.worldX() + worldMapViewport.worldW() * 0.68 ? -78 : 12;
                WorldMap.Kingdom kingdom = kingdomById(settlement.kingdomId());
                boolean playerSettlement = state.world.isPlayerSettlement(settlement);
                Rectangle markerBounds = WorldMapOverlayRenderer.drawSettlementMarker(
                        g, worldMapViewport, settlement, kingdom, playerSettlement, dx, -9);
                Color markerColor = playerSettlement ? new Color(255, 226, 128)
                        : kingdom == null ? new Color(245, 214, 117) : new Color(kingdom.colorRgb());
                tooltipZones.add(new TooltipZone(markerBounds, settlement.label(),
                        settlementTooltip(settlement, kingdom), null, markerColor));
            }
        }
        long objectivesStarted = renderMetrics.start();
        List<GameState.QuestObjective> activeObjectives = state.activeQuestObjectives();
        renderMetrics.record("questObjectives", objectivesStarted);
        renderMetrics.sample("questObjectives.count", activeObjectives.size());
        for (GameState.QuestObjective objective : activeObjectives) {
            TilePoint marker = worldMapObjectivePosition(objective);
            if (marker != null && showQuestObjective(objective)
                    && worldMapVisible(worldMapViewport, marker.x(), marker.y(), 6.0)) {
                WorldMapOverlayRenderer.drawQuestMarker(g, worldMapViewport, objective.kind(), objective.title(),
                        marker, objectiveColor(objective.kind(), 255));
            }
        }
        if (WorldMap.OVERWORLD_ID.equals(state.currentMapId)) {
            int px = worldMapScreenX(worldMapViewport, state.playerX + 0.5);
            int py = worldMapScreenY(worldMapViewport, state.playerY + 0.5);
            g.setColor(new Color(255, 245, 174));
            g.fillOval(px - 5, py - 5, 10, 10);
            g.setColor(Color.BLACK);
            g.drawOval(px - 5, py - 5, 10, 10);
        }
        g.setClip(oldClip);
        g.setColor(new Color(81, 88, 108));
        g.drawRect(mapX, mapY, mapW, mapH);
        int sideX = mapX + mapW + gap;
        actionButton(g, sideX, y + 18, 136, 30, state.worldMapKingdoms ? "Kingdoms On" : "Kingdoms Off",
                state::toggleWorldMapKingdoms, new Color(57, 71, 102), new Color(110, 127, 160), true);
        drawWorldMapControls(g, sideX, y + 56, legendW);
        WorldMapOverlayRenderer.drawLegend(g, sideX, y + 168, legendW,
                state.worldMapKingdoms, state.world.kingdoms());
        sidebarButton(g, sideX, y + h - 54, 116, 30, "Close", state::toggleWorldMap);
        renderMetrics.record("worldMap", worldMapStarted);
    }

    private WorldMapViewport updateWorldMapViewport(int mapX, int mapY, int mapW, int mapH) {
        worldMapZoom = clamp(worldMapZoom, WORLD_MAP_MIN_ZOOM, WORLD_MAP_MAX_ZOOM);
        double worldW = WorldMap.COLS / worldMapZoom;
        double worldH = WorldMap.ROWS / worldMapZoom;
        worldMapCenterX = clampWorldMapCenter(worldMapCenterX, worldW, WorldMap.COLS);
        worldMapCenterY = clampWorldMapCenter(worldMapCenterY, worldH, WorldMap.ROWS);
        return new WorldMapViewport(mapX, mapY, mapW, mapH,
                worldMapCenterX - worldW / 2.0,
                worldMapCenterY - worldH / 2.0,
                worldW,
                worldH);
    }

    private double clampWorldMapCenter(double center, double viewSize, int worldSize) {
        if (viewSize >= worldSize) {
            return worldSize / 2.0;
        }
        return clamp(center, viewSize / 2.0, worldSize - viewSize / 2.0);
    }

    private int worldMapScreenX(WorldMapViewport viewport, double wx) {
        return WorldMapOverlayRenderer.screenX(viewport, wx);
    }

    private int worldMapScreenY(WorldMapViewport viewport, double wy) {
        return WorldMapOverlayRenderer.screenY(viewport, wy);
    }

    private double worldMapWorldX(WorldMapViewport viewport, int screenX) {
        return WorldMapOverlayRenderer.worldX(viewport, screenX);
    }

    private double worldMapWorldY(WorldMapViewport viewport, int screenY) {
        return WorldMapOverlayRenderer.worldY(viewport, screenY);
    }

    private boolean worldMapVisible(WorldMapViewport viewport, int wx, int wy, double margin) {
        return WorldMapOverlayRenderer.visible(viewport, wx, wy, margin);
    }

    private void drawWorldMapControls(Graphics2D g, int x, int y, int width) {
        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.setColor(new Color(244, 239, 220));
        g.drawString("Zoom " + Math.round(worldMapZoom * 100) + "%", x, y + 13);
        actionButton(g, x, y + 22, 34, 26, "-", () -> adjustWorldMapZoom(-1),
                new Color(35, 39, 54), new Color(86, 98, 128), worldMapZoom > WORLD_MAP_MIN_ZOOM);
        actionButton(g, x + 42, y + 22, 34, 26, "+", () -> adjustWorldMapZoom(1),
                new Color(35, 39, 54), new Color(86, 98, 128), worldMapZoom < WORLD_MAP_MAX_ZOOM);
        actionButton(g, x + 84, y + 22, 68, 26, "Reset", this::resetWorldMapZoom,
                new Color(48, 55, 70), new Color(89, 102, 125), true);
        actionButton(g, x, y + 58, 68, 26, "You", this::focusWorldMapOnPlayer,
                new Color(57, 71, 102), new Color(110, 127, 160), true);
        actionButton(g, x + 76, y + 58, 76, 26, "Quest", this::focusNextWorldMapQuest,
                new Color(68, 90, 53), new Color(110, 139, 92), hasVisibleWorldMapQuest());
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(new Color(178, 184, 204));
        drawClippedString(g, "Wheel zooms, drag pans, click centers.", x, y + 101, width);
    }

    void adjustWorldMapZoom(int direction) {
        zoomWorldMapAt(null, direction > 0 ? WORLD_MAP_ZOOM_STEP : 1.0 / WORLD_MAP_ZOOM_STEP);
    }

    private void zoomWorldMapAt(Point focus, double factor) {
        WorldMapViewport viewport = worldMapViewport;
        double oldZoom = worldMapZoom;
        double nextZoom = clamp(worldMapZoom * factor, WORLD_MAP_MIN_ZOOM, WORLD_MAP_MAX_ZOOM);
        if (Math.abs(nextZoom - oldZoom) < 0.001) {
            return;
        }
        if (focus != null && worldMapBounds.contains(focus)) {
            double focusWorldX = worldMapWorldX(viewport, focus.x);
            double focusWorldY = worldMapWorldY(viewport, focus.y);
            double focusFractionX = clamp((focus.x - viewport.screenX()) / (double) viewport.screenW(), 0.0, 1.0);
            double focusFractionY = clamp((focus.y - viewport.screenY()) / (double) viewport.screenH(), 0.0, 1.0);
            double nextWorldW = WorldMap.COLS / nextZoom;
            double nextWorldH = WorldMap.ROWS / nextZoom;
            worldMapCenterX = focusWorldX + nextWorldW * (0.5 - focusFractionX);
            worldMapCenterY = focusWorldY + nextWorldH * (0.5 - focusFractionY);
        }
        worldMapZoom = nextZoom;
        worldMapCenterX = clampWorldMapCenter(worldMapCenterX, WorldMap.COLS / worldMapZoom, WorldMap.COLS);
        worldMapCenterY = clampWorldMapCenter(worldMapCenterY, WorldMap.ROWS / worldMapZoom, WorldMap.ROWS);
    }

    void resetWorldMapZoom() {
        worldMapZoom = WORLD_MAP_MIN_ZOOM;
        TilePoint position = currentWorldMapPosition();
        centerWorldMapOn(position.x() + 0.5, position.y() + 0.5);
    }

    void focusWorldMapOnPlayer() {
        TilePoint position = currentWorldMapPosition();
        if (worldMapZoom < 2.0) {
            worldMapZoom = 2.0;
        }
        centerWorldMapOn(position.x() + 0.5, position.y() + 0.5);
    }

    private boolean hasVisibleWorldMapQuest() {
        for (GameState.QuestObjective objective : state.activeQuestObjectives()) {
            if (worldMapObjectivePosition(objective) != null && showQuestObjective(objective)) {
                return true;
            }
        }
        return false;
    }

    void focusNextWorldMapQuest() {
        List<GameState.QuestObjective> objectives = state.activeQuestObjectives().stream()
                .filter(this::showQuestObjective)
                .filter(objective -> worldMapObjectivePosition(objective) != null)
                .toList();
        if (objectives.isEmpty()) {
            return;
        }
        GameState.QuestObjective objective = objectives.get(Math.floorMod(worldMapQuestFocusIndex, objectives.size()));
        TilePoint marker = worldMapObjectivePosition(objective);
        if (marker == null) {
            return;
        }
        worldMapQuestFocusIndex++;
        if (worldMapZoom < 2.5) {
            worldMapZoom = 2.5;
        }
        centerWorldMapOn(marker.x() + 0.5, marker.y() + 0.5);
    }

    private void centerWorldMapOn(double wx, double wy) {
        worldMapCenterX = clampWorldMapCenter(wx, WorldMap.COLS / worldMapZoom, WorldMap.COLS);
        worldMapCenterY = clampWorldMapCenter(wy, WorldMap.ROWS / worldMapZoom, WorldMap.ROWS);
    }

    void panWorldMap(double deltaX, double deltaY) {
        worldMapCenterX = clampWorldMapCenter(worldMapCenterX + deltaX, WorldMap.COLS / worldMapZoom, WorldMap.COLS);
        worldMapCenterY = clampWorldMapCenter(worldMapCenterY + deltaY, WorldMap.ROWS / worldMapZoom, WorldMap.ROWS);
    }

    private TilePoint worldMapObjectivePosition(GameState.QuestObjective objective) {
        if (objective == null) {
            return null;
        }
        if (WorldMap.OVERWORLD_ID.equals(objective.mapId())) {
            return new TilePoint(objective.x(), objective.y());
        }
        TilePoint entrance = state.world.overworldEntranceFor(objective.mapId());
        if (entrance != null) {
            return entrance;
        }
        for (WorldMap.SettlementSite settlement : state.world.settlementSites()) {
            if (objective.mapId().equals(settlement.id())
                    || objective.mapId().startsWith("house_" + settlement.id() + "_")) {
                return new TilePoint(settlement.x(), settlement.y());
            }
        }
        return null;
    }

    private String shortSettlementLabel(String label) {
        return WorldMapOverlayRenderer.shortSettlementLabel(label);
    }

    private WorldMap.Kingdom kingdomById(String kingdomId) {
        for (WorldMap.Kingdom kingdom : state.world.kingdoms()) {
            if (kingdom.id().equals(kingdomId)) {
                return kingdom;
            }
        }
        return null;
    }

    private String settlementTooltip(WorldMap.SettlementSite settlement, WorldMap.Kingdom kingdom) {
        TilePoint position = currentWorldMapPosition();
        int distance = Math.abs(settlement.x() - position.x()) + Math.abs(settlement.y() - position.y());
        char terrain = state.world.tileAt(WorldMap.OVERWORLD_ID, settlement.x(), settlement.y());
        String kingdomName = kingdom == null ? "Unclaimed" : kingdom.name();
        int visits = state.settlementVisitCount(settlement.id());
        int knowledge = state.settlementKnowledgeLevel(settlement.id());
        StringBuilder body = new StringBuilder();
        body.append("Knowledge: ").append(settlementKnowledgeLabel(knowledge))
                .append(" (").append(visits).append(visits == 1 ? " visit" : " visits")
                .append(", INT ").append(state.player.intelligence).append(").");
        body.append("\nKingdom: ").append(kingdomName).append(".");
        if (state.world.isPlayerSettlement(settlement)) {
            VillageManager.SettlementStage stage = state.world.playerVillageSettlementStage();
            body.append("\nPlayer settlement, Stage ").append(stage.stage()).append("/10: ").append(stage.title()).append(".");
            body.append("\nDistance from you: ").append(distance).append(" tiles. Terrain: ").append(Terrain.name(terrain)).append(".");
            body.append("\n").append(settlementQuestSummary(settlement, knowledge));
            return body.toString();
        }
        if (knowledge <= 0) {
            body.append("\nOnly the shape of ").append(shortSettlementLabel(settlement.label()))
                    .append(" is known. Visit it or raise Intelligence to learn more.");
            return body.toString();
        }
        body.append("\n").append(settlement.kind()).append(".");
        body.append(" Distance from you: ").append(distance).append(" tiles.");
        if (knowledge >= 2) {
            body.append(" Terrain: ").append(Terrain.name(terrain)).append(".");
        }
        if (knowledge >= 3 && kingdom != null) {
            body.append("\nSeat: ").append(kingdom.seat()).append(".");
        }
        if (knowledge >= 4 && kingdom != null) {
            body.append(" ").append(kingdom.description());
        }
        body.append("\n").append(settlementQuestSummary(settlement, knowledge));
        return body.toString();
    }

    private String settlementKnowledgeLabel(int knowledge) {
        return switch (knowledge) {
            case 0 -> "Unknown";
            case 1 -> "Rumors";
            case 2 -> "Visited";
            case 3 -> "Familiar";
            case 4 -> "Mapped";
            default -> "Well known";
        };
    }

    private String settlementQuestSummary(WorldMap.SettlementSite settlement, int knowledge) {
        List<Quest> quests = settlementLocalQuests(settlement);
        if (quests.isEmpty()) {
            return knowledge <= 0 ? "Quests: no leads known." : "Quests: none known.";
        }
        if (knowledge <= 0) {
            return "Quests: local leads unknown.";
        }
        int revealCount = Math.min(quests.size(), Math.min(3, knowledge));
        StringBuilder body = new StringBuilder("Quests: ");
        for (int i = 0; i < revealCount; i++) {
            if (i > 0) {
                body.append(", ");
            }
            body.append(quests.get(i).title);
        }
        int hidden = quests.size() - revealCount;
        if (hidden > 0) {
            body.append(" (+").append(hidden).append(hidden == 1 ? " more lead" : " more leads").append(")");
        }
        body.append(".");
        return body.toString();
    }

    private List<Quest> settlementLocalQuests(WorldMap.SettlementSite settlement) {
        List<Quest> quests = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Npc npc : GameData.NPCS) {
            if (!settlement.id().equals(npc.mapId()) || npc.questId() == null || !seen.add(npc.questId())) {
                continue;
            }
            Quest quest = state.quests.get(npc.questId());
            if (quest != null && !quest.completed) {
                quests.add(quest);
            }
        }
        return quests;
    }

    private TilePoint currentWorldMapPosition() {
        if (WorldMap.OVERWORLD_ID.equals(state.currentMapId)) {
            return new TilePoint(state.playerX, state.playerY);
        }
        for (WorldMap.SettlementSite settlement : state.world.settlementSites()) {
            if (state.currentMapId.equals(settlement.id()) || state.currentMapId.startsWith("house_" + settlement.id() + "_")) {
                return new TilePoint(settlement.x(), settlement.y());
            }
        }
        return WorldMap.START_POSITION;
    }

    private void drawPauseMenu(Graphics2D g) {
        g.setColor(new Color(8, 11, 16, 170));
        g.fillRect(0, 0, viewWidth(), viewHeight());
        int w = 528;
        int h = 416;
        int x = gameAreaCenteredX(w);
        int y = centeredY(h);
        drawOverlayBase(g, x, y, w, h);
        g.setFont(new Font("Serif", Font.BOLD, 34));
        g.setColor(new Color(244, 213, 141));
        drawCenteredIn(g, "Paused", x, y + 58, w);

        int buttonX = x + 138;
        int buttonW = w - 276;
        actionButton(g, buttonX, y + 96, buttonW, 42, "Resume", state::resumeGame, new Color(66, 93, 49), new Color(126, 176, 95), true);
        actionButton(g, buttonX, y + 150, buttonW, 42, "Save / Load", this::openSaveMenuForSaving, new Color(57, 71, 102), new Color(110, 127, 160), true);
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
        int w = 812;
        int h = 760;
        int x = overlay ? gameAreaCenteredX(w) : centeredX(w);
        int y = centeredY(h);
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

        g.drawString("Monster Difficulty", x + 52, y + 468);
        drawMonsterLevelScalingRow(g, x + 52, y + 504);

        g.drawString("Audio", x + 470, y + 96);
        drawVolumeRow(g, x + 470, y + 132, "Master", state.config.masterVolume, "master");
        drawVolumeRow(g, x + 470, y + 184, "Music", state.config.musicVolume, "music");
        drawVolumeRow(g, x + 470, y + 236, "SFX", state.config.sfxVolume, "sfx");
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(165, 174, 193));
        wrap(g, "Music blends between biome themes over time, with softer ambient variants during longer exploration.", x + 470, y + 294, 260, 18);

        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.setColor(new Color(230, 225, 206));
        g.drawString("Movement & Camera", x + 470, y + 366);
        drawChoiceRow(g, x + 470, y + 394, "Camera", cameraMode().label, this::previousCameraMode, this::nextCameraMode);
        drawChoiceRow(g, x + 470, y + 438, "Speed", movementSpeed().label, this::previousMovementSpeed, this::nextMovementSpeed);
        drawCameraSmoothingRow(g, x + 470, y + 482);
        drawToggleRow(g, x + 470, y + 526, "Look-Ahead", state.config.cameraLookAhead, this::toggleCameraLookAhead);

        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.setColor(new Color(230, 225, 206));
        g.drawString("Graphics", x + 470, y + 588);
        drawChoiceRow(g, x + 470, y + 616, "Weather", weatherQuality().label, this::previousWeatherQuality, this::nextWeatherQuality);

        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.setColor(new Color(230, 225, 206));
        g.drawString("Village", x + 52, y + 588);
        drawToggleRow(g, x + 52, y + 620, "Creative Build", state.config.creativeBuildMode, this::toggleCreativeBuildMode);
        drawToggleRow(g, x + 52, y + 658, "Editor Button", state.config.showMapEditorButton, this::toggleMapEditorButton);

        actionButton(g, x + w - 170, y + h - 58, 120, 34, "Back", state::closeSettings, new Color(48, 55, 70), new Color(89, 102, 125), true);
    }

    private void drawToggleRow(Graphics2D g, int x, int y, String label, boolean enabled, Runnable toggle) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(220, 224, 232));
        g.drawString(label, x, y + 22);
        actionButton(g, x + 188, y, 116, 30, enabled ? "On" : "Off", toggle,
                enabled ? new Color(57, 76, 60) : new Color(62, 49, 49),
                enabled ? new Color(126, 176, 95) : new Color(149, 96, 88), true);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(165, 174, 193));
        g.drawString(enabled ? "Enabled" : "Disabled", x + 318, y + 20);
    }

    private void drawChoiceRow(Graphics2D g, int x, int y, String label, String value, Runnable previous, Runnable next) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(220, 224, 232));
        g.drawString(label, x, y + 22);
        actionButton(g, x + 142, y, 34, 28, "<", previous, new Color(35, 39, 54), new Color(86, 98, 128), true);
        g.setColor(new Color(220, 224, 232));
        drawCenteredIn(g, value, x + 180, y + 20, 104);
        actionButton(g, x + 288, y, 34, 28, ">", next, new Color(35, 39, 54), new Color(86, 98, 128), true);
    }

    private void drawCameraSmoothingRow(Graphics2D g, int x, int y) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(220, 224, 232));
        g.drawString("Smoothing", x, y + 22);
        Rectangle track = new Rectangle(x + 142, y + 8, 116, 10);
        Rectangle bounds = new Rectangle(x + 136, y - 2, 128, 32);
        registerSlider(bounds, track, SliderKind.CAMERA, "camera_smoothing");
        drawSliderTrack(g, track, state.config.cameraSmoothing / 100.0, sliderHovered(bounds), sliderActive(SliderKind.CAMERA, "camera_smoothing"), 11);
        g.setColor(new Color(220, 224, 232));
        g.drawString(state.config.cameraSmoothing + "%", x + 272, y + 22);
    }

    private void drawKeybindRow(Graphics2D g, int x, int y, String scope, String key, String action) {
        g.setColor(new Color(18, 21, 31, 160));
        g.fillRoundRect(x, y - 20, 276, 26, 6, 6);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(155, 164, 184));
        g.drawString(scope, x + 10, y - 3);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(new Color(235, 211, 132));
        g.drawString(key, x + 72, y - 3);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(220, 224, 232));
        g.drawString(action, x + 142, y - 3);
    }

    private void drawChanceRow(Graphics2D g, int x, int y, String label, double value, String key) {
        drawSettingRow(g, x, y, label, Math.round(value * 100) + "%", key, () -> adjustChanceSetting(key, -0.01), () -> adjustChanceSetting(key, 0.01), value);
    }

    private void drawMonsterLevelScalingRow(Graphics2D g, int x, int y) {
        double value = state.config.monsterLevelScaling;
        drawDifficultyRow(g, x, y, "Level Scaling", Math.round(value * 100) + "%",
                () -> adjustMonsterLevelScaling(-0.05), () -> adjustMonsterLevelScaling(0.05), value);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(165, 174, 193));
        g.drawString("Controls extra monster gains from your hero level.", x, y + 48);
    }

    private void drawVolumeRow(Graphics2D g, int x, int y, String label, int value, String key) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(220, 224, 232));
        g.drawString(label, x, y + 22);
        g.drawString(value + "%", x + 76, y + 22);
        actionButton(g, x + 124, y, 34, 28, "-", () -> adjustVolumeSetting(key, -5), new Color(35, 39, 54), new Color(86, 98, 128), true);
        actionButton(g, x + 166, y, 34, 28, "+", () -> adjustVolumeSetting(key, 5), new Color(35, 39, 54), new Color(86, 98, 128), true);
        Rectangle track = new Rectangle(x + 222, y + 8, 116, 10);
        Rectangle bounds = new Rectangle(x + 216, y - 2, 128, 32);
        registerSlider(bounds, track, SliderKind.VOLUME, key);
        drawSliderTrack(g, track, value / 100.0, sliderHovered(bounds), sliderActive(SliderKind.VOLUME, key), 11);
    }

    private void drawSettingRow(Graphics2D g, int x, int y, String label, String value, String key, Runnable decrease, Runnable increase, double fraction) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(220, 224, 232));
        g.drawString(label, x, y + 22);
        Rectangle track = new Rectangle(x + 150, y + 8, 150, 10);
        Rectangle bounds = new Rectangle(x + 144, y - 2, 162, 32);
        registerSlider(bounds, track, SliderKind.CHANCE, key);
        drawSliderTrack(g, track, fraction, sliderHovered(bounds), sliderActive(SliderKind.CHANCE, key), 11);
        g.setColor(new Color(220, 224, 232));
        g.drawString(value, x + 314, y + 22);
        actionButton(g, x + 366, y, 34, 28, "-", decrease, new Color(35, 39, 54), new Color(86, 98, 128), true);
        actionButton(g, x + 408, y, 34, 28, "+", increase, new Color(35, 39, 54), new Color(86, 98, 128), true);
    }

    private void drawDifficultyRow(Graphics2D g, int x, int y, String label, String value, Runnable decrease, Runnable increase, double fraction) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(220, 224, 232));
        g.drawString(label, x, y + 22);
        Rectangle track = new Rectangle(x + 150, y + 8, 150, 10);
        Rectangle bounds = new Rectangle(x + 144, y - 2, 162, 32);
        registerSlider(bounds, track, SliderKind.DIFFICULTY, "monster_level_scaling");
        drawSliderTrack(g, track, fraction, sliderHovered(bounds), sliderActive(SliderKind.DIFFICULTY, "monster_level_scaling"), 11);
        g.setColor(new Color(220, 224, 232));
        g.drawString(value, x + 314, y + 22);
        actionButton(g, x + 366, y, 34, 28, "-", decrease, new Color(35, 39, 54), new Color(86, 98, 128), true);
        actionButton(g, x + 408, y, 34, 28, "+", increase, new Color(35, 39, 54), new Color(86, 98, 128), true);
    }

    private void registerSlider(Rectangle bounds, Rectangle track, SliderKind kind, String key) {
        sliders.add(new UiSlider(bounds, track, kind, key));
    }

    private boolean sliderHovered(Rectangle bounds) {
        return hoverPoint != null && bounds.contains(hoverPoint);
    }

    private boolean sliderActive(SliderKind kind, String key) {
        return activeSlider != null && activeSlider.kind() == kind && activeSlider.key().equals(key);
    }

    private void drawSliderTrack(Graphics2D g, Rectangle track, double fraction, boolean hovered, boolean active, int knobSize) {
        double clamped = clamp(fraction, 0.0, 1.0);
        Color trackColor = hovered || active ? new Color(58, 66, 88) : new Color(42, 47, 62);
        Color fillColor = active ? new Color(255, 226, 142) : hovered ? new Color(248, 220, 135) : new Color(235, 211, 132);
        g.setColor(trackColor);
        g.fillRoundRect(track.x, track.y, track.width, track.height, track.height, track.height);
        int filled = (int) Math.round(track.width * clamped);
        if (filled > 0) {
            g.setColor(fillColor);
            g.fillRoundRect(track.x, track.y, filled, track.height, track.height, track.height);
        }
        int knobX = track.x + (int) Math.round(track.width * clamped);
        int knobY = track.y + track.height / 2;
        int radius = active ? knobSize + 2 : hovered ? knobSize + 1 : knobSize;
        g.setColor(new Color(6, 8, 14, active ? 135 : 90));
        g.fillOval(knobX - radius / 2 + 1, knobY - radius / 2 + 2, radius, radius);
        g.setColor(fillColor);
        g.fillOval(knobX - radius / 2, knobY - radius / 2, radius, radius);
        g.setColor(active ? new Color(255, 247, 203) : new Color(88, 78, 43));
        g.drawOval(knobX - radius / 2, knobY - radius / 2, radius, radius);
    }

    private void drawSaveMenu(Graphics2D g, boolean overlay) {
        if (overlay) {
            g.setColor(new Color(8, 11, 16, 170));
            g.fillRect(0, 0, viewWidth(), viewHeight());
        }
        int w = 756;
        int h = 650;
        int x = overlay ? gameAreaCenteredX(w) : centeredX(w);
        int y = centeredY(h);
        drawOverlayBase(g, x, y, w, h);
        g.setFont(new Font("Serif", Font.BOLD, 32));
        g.setColor(new Color(244, 213, 141));
        String title = state.saveMenuCanSave ? "Save / Load Adventure" : importingCharacter ? "Import Character" : "Load Adventure";
        drawCenteredIn(g, title, x, y + 48, w);

        int rowY = y + 92;
        if (state.saveMenuCanSave) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g.setColor(new Color(198, 202, 211));
            g.drawString("Save Name", x + 52, rowY + 14);
            g.setColor(new Color(22, 27, 38, 235));
            g.fillRoundRect(x + 52, rowY + 24, 376, 40, 7, 7);
            g.setColor(new Color(98, 112, 142));
            g.drawRoundRect(x + 52, rowY + 24, 376, 40, 7, 7);
            String saveName = state.pendingSaveName.isBlank() ? nextDefaultSaveName() : state.pendingSaveName;
            g.setFont(new Font("SansSerif", Font.PLAIN, 18));
            g.setColor(state.pendingSaveName.isBlank() ? new Color(142, 151, 168) : new Color(244, 239, 220));
            g.drawString(shortText(saveName, 34) + (frame % 24 < 12 ? "_" : ""), x + 68, rowY + 50);
            actionButton(g, x + 446, rowY + 24, 210, 40, "Save Adventure", this::saveGame, new Color(66, 93, 49), new Color(126, 176, 95), true);
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.setColor(new Color(170, 178, 194));
            g.drawString("Character: " + state.player.name + " (" + state.player.className + ")   Folder: "
                    + SaveSystem.saveIdFor(state.player.name), x + 52, rowY + 86);
            rowY += 108;
        }

        if (!state.saveMenuCanSave) {
            drawLoadMenuContent(g, x, y, w, h, rowY);
            return;
        }

        boolean filterCurrent = state.saveMenuCanSave && saveListCurrentCharacterOnly;
        List<SaveSystem.SaveSummary> summaries = filterCurrent
                ? saves.listSavesForCharacter(SaveSystem.saveIdFor(state.player.name))
                : saves.listSaves();
        g.setFont(new Font("SansSerif", Font.BOLD, 17));
        g.setColor(new Color(230, 225, 206));
        g.drawString(filterCurrent ? state.player.name + "'s Saves" : "All Saved Adventures", x + 52, rowY + 18);
        if (state.saveMenuCanSave) {
            actionButton(g, x + w - 278, rowY - 6, 100, 28, "This Hero",
                    () -> {
                        saveListCurrentCharacterOnly = true;
                        saveListScroll = 0;
                    },
                    saveListCurrentCharacterOnly ? new Color(66, 93, 49) : new Color(35, 39, 54),
                    saveListCurrentCharacterOnly ? new Color(126, 176, 95) : new Color(86, 98, 128),
                    true);
            actionButton(g, x + w - 166, rowY - 6, 96, 28, "All Saves",
                    () -> {
                        saveListCurrentCharacterOnly = false;
                        saveListScroll = 0;
                    },
                    !saveListCurrentCharacterOnly ? new Color(66, 93, 49) : new Color(35, 39, 54),
                    !saveListCurrentCharacterOnly ? new Color(126, 176, 95) : new Color(86, 98, 128),
                    true);
        }
        rowY += 34;
        if (summaries.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 17));
            g.setColor(new Color(198, 202, 211));
            drawCenteredIn(g, "No saved adventures yet.", x, rowY + 60, w);
        } else {
            int rowH = 78;
            int listBottom = y + h - 82;
            int visibleRows = Math.max(1, (listBottom - rowY) / rowH);
            int maxScroll = Math.max(0, summaries.size() - visibleRows);
            saveListScroll = Math.max(0, Math.min(saveListScroll, maxScroll));
            int end = Math.min(summaries.size(), saveListScroll + visibleRows);
            int listTop = rowY;
            for (int i = saveListScroll; i < end; i++) {
                SaveSystem.SaveSummary summary = summaries.get(i);
                int buttonIndex = i + 1;
                String saveId = summary.saveId();
                g.setColor(new Color(28, 32, 43, 238));
                g.fillRoundRect(x + 52, rowY, w - 104, 68, 8, 8);
                g.setColor(new Color(82, 92, 116));
                g.drawRoundRect(x + 52, rowY, w - 104, 68, 8, 8);
                g.setFont(new Font("SansSerif", Font.BOLD, 16));
                g.setColor(new Color(235, 236, 240));
                g.drawString(buttonIndex + ". " + shortText(summary.saveName(), 34), x + 70, rowY + 24);
                g.setFont(new Font("SansSerif", Font.PLAIN, 13));
                g.setColor(new Color(176, 182, 196));
                g.drawString(summary.name() + " (" + summary.className() + ")  Level " + summary.level() + "  "
                        + state.world.label(summary.mapId()) + "  " + summary.gold() + "g", x + 70, rowY + 43);
                g.setColor(new Color(147, 154, 172));
                g.drawString("Saved " + formatSaveTime(summary.savedAtMillis()) + "  "
                        + summary.completedQuests() + " done  " + summary.activeQuests() + " active  Party "
                        + summary.partySize(), x + 70, rowY + 60);
                boolean canOverwrite = state.saveMenuCanSave
                        && summary.characterId().equals(SaveSystem.saveIdFor(state.player.name));
                if (canOverwrite) {
                    actionButton(g, x + w - 292, rowY + 18, 108, 32, "Overwrite", () -> requestOverwrite(summary), new Color(95, 67, 50), new Color(171, 115, 82), true);
                }
                actionButton(g, x + w - 172, rowY + 18, 104, 32, "Load", () -> loadGame(saveId), new Color(57, 71, 102), new Color(110, 127, 160), true);
                rowY += rowH;
            }
            if (summaries.size() > visibleRows) {
                drawScrollIndicator(g, x + w - 40, listTop, listBottom - listTop, summaries.size(), saveListScroll, visibleRows);
            }
        }
        drawSaveMenuStatus(g, x + 52, y + h - 55, w - 250);
        actionButton(g, x + w - 170, y + h - 58, 120, 34, "Back", this::closeSaveMenuOrLoadFolder, new Color(48, 55, 70), new Color(89, 102, 125), true);
        if (hasPendingOverwrite()) {
            drawOverwriteConfirmation(g, x, y, w, h);
        }
    }

    private void drawLoadMenuContent(Graphics2D g, int x, int y, int w, int h, int rowY) {
        if (selectedLoadCharacterId.isBlank()) {
            drawLoadFolders(g, x, y, w, h, rowY);
        } else {
            drawLoadSavesForSelectedFolder(g, x, y, w, h, rowY);
        }
        drawSaveMenuStatus(g, x + 52, y + h - 55, w - 250);
        actionButton(g, x + w - 170, y + h - 58, 120, 34,
                selectedLoadCharacterId.isBlank() ? "Back" : "Folders",
                this::closeSaveMenuOrLoadFolder, new Color(48, 55, 70), new Color(89, 102, 125), true);
    }

    private void drawLoadFolders(Graphics2D g, int x, int y, int w, int h, int rowY) {
        List<LoadFolder> folders = loadFoldersByLatestSave();
        g.setFont(new Font("SansSerif", Font.BOLD, 17));
        g.setColor(new Color(230, 225, 206));
        g.drawString(importingCharacter ? "Choose Character" : "Character Folders", x + 52, rowY + 18);
        rowY += 34;
        if (folders.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 17));
            g.setColor(new Color(198, 202, 211));
            drawCenteredIn(g, "No saved adventures yet.", x, rowY + 60, w);
            return;
        }
        int rowH = 78;
        int listBottom = y + h - 82;
        int visibleRows = Math.max(1, (listBottom - rowY) / rowH);
        int maxScroll = Math.max(0, folders.size() - visibleRows);
        saveListScroll = Math.max(0, Math.min(saveListScroll, maxScroll));
        int end = Math.min(folders.size(), saveListScroll + visibleRows);
        int listTop = rowY;
        for (int i = saveListScroll; i < end; i++) {
            LoadFolder folder = folders.get(i);
            int buttonIndex = i + 1;
            g.setColor(new Color(28, 32, 43, 238));
            g.fillRoundRect(x + 52, rowY, w - 104, 68, 8, 8);
            g.setColor(new Color(82, 92, 116));
            g.drawRoundRect(x + 52, rowY, w - 104, 68, 8, 8);
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.setColor(new Color(235, 236, 240));
            g.drawString(buttonIndex + ". " + shortText(folder.name, 34), x + 70, rowY + 24);
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.setColor(new Color(176, 182, 196));
            g.drawString(folder.className + "  Level " + folder.maxLevel + "  Folder " + folder.characterId, x + 70, rowY + 43);
            g.setColor(new Color(147, 154, 172));
            g.drawString(folder.saveCount + " saves  Latest " + formatSaveTime(folder.latestSavedAtMillis), x + 70, rowY + 60);
            actionButton(g, x + w - 172, rowY + 18, 104, 32, "Open",
                    () -> selectLoadFolder(folder.characterId), new Color(57, 71, 102), new Color(110, 127, 160), true);
            rowY += rowH;
        }
        if (folders.size() > visibleRows) {
            drawScrollIndicator(g, x + w - 40, listTop, listBottom - listTop, folders.size(), saveListScroll, visibleRows);
        }
    }

    private void drawLoadSavesForSelectedFolder(Graphics2D g, int x, int y, int w, int h, int rowY) {
        List<SaveSystem.SaveSummary> summaries = selectedLoadFolderSaves();
        g.setFont(new Font("SansSerif", Font.BOLD, 17));
        g.setColor(new Color(230, 225, 206));
        g.drawString(selectedLoadFolderTitle(summaries), x + 52, rowY + 18);
        rowY += 34;
        if (summaries.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 17));
            g.setColor(new Color(198, 202, 211));
            drawCenteredIn(g, "No saves in this folder.", x, rowY + 60, w);
            return;
        }
        int rowH = 78;
        int listBottom = y + h - 82;
        int visibleRows = Math.max(1, (listBottom - rowY) / rowH);
        int maxScroll = Math.max(0, summaries.size() - visibleRows);
        saveListScroll = Math.max(0, Math.min(saveListScroll, maxScroll));
        int end = Math.min(summaries.size(), saveListScroll + visibleRows);
        int listTop = rowY;
        for (int i = saveListScroll; i < end; i++) {
            SaveSystem.SaveSummary summary = summaries.get(i);
            int buttonIndex = i + 1;
            String saveId = summary.saveId();
            g.setColor(new Color(28, 32, 43, 238));
            g.fillRoundRect(x + 52, rowY, w - 104, 68, 8, 8);
            g.setColor(new Color(82, 92, 116));
            g.drawRoundRect(x + 52, rowY, w - 104, 68, 8, 8);
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.setColor(new Color(235, 236, 240));
            g.drawString(buttonIndex + ". " + shortText(summary.saveName(), 34), x + 70, rowY + 24);
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.setColor(new Color(176, 182, 196));
            g.drawString(summary.name() + " (" + summary.className() + ")  Level " + summary.level() + "  "
                    + state.world.label(summary.mapId()) + "  " + summary.gold() + "g", x + 70, rowY + 43);
            g.setColor(new Color(147, 154, 172));
            g.drawString("Saved " + formatSaveTime(summary.savedAtMillis()) + "  "
                    + summary.completedQuests() + " done  " + summary.activeQuests() + " active  Party "
                    + summary.partySize(), x + 70, rowY + 60);
            String actionLabel = importingCharacter ? "Import" : "Load";
            actionButton(g, x + w - 172, rowY + 18, 104, 32, actionLabel,
                    () -> loadOrImportGame(saveId), importingCharacter ? new Color(89, 68, 43) : new Color(57, 71, 102),
                    importingCharacter ? new Color(171, 124, 76) : new Color(110, 127, 160), true);
            rowY += rowH;
        }
        if (summaries.size() > visibleRows) {
            drawScrollIndicator(g, x + w - 40, listTop, listBottom - listTop, summaries.size(), saveListScroll, visibleRows);
        }
    }

    List<LoadFolder> loadFoldersByLatestSave() {
        Map<String, LoadFolder> folders = new LinkedHashMap<>();
        for (SaveSystem.SaveSummary summary : saves.listSaves()) {
            folders.computeIfAbsent(summary.characterId(), key -> new LoadFolder(summary)).include(summary);
        }
        return new ArrayList<>(folders.values());
    }

    List<SaveSystem.SaveSummary> selectedLoadFolderSaves() {
        if (selectedLoadCharacterId.isBlank()) {
            return List.of();
        }
        return saves.listSavesForCharacter(selectedLoadCharacterId);
    }

    private String selectedLoadFolderTitle(List<SaveSystem.SaveSummary> summaries) {
        if (summaries.isEmpty()) {
            return selectedLoadCharacterId + "'s Saves";
        }
        SaveSystem.SaveSummary latest = summaries.get(0);
        return latest.name() + "'s Saves";
    }

    void selectLoadFolder(String characterId) {
        selectedLoadCharacterId = characterId == null ? "" : characterId;
        saveListScroll = 0;
    }

    private void drawSaveMenuStatus(Graphics2D g, int x, int y, int w) {
        if (state.status == null || state.status.isBlank()) {
            return;
        }
        boolean failed = state.status.toLowerCase().contains("failed");
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(failed ? new Color(235, 150, 136) : new Color(181, 187, 201));
        drawClippedString(g, state.status, x, y + 21, w);
    }

    private void drawOverwriteConfirmation(Graphics2D g, int x, int y, int w, int h) {
        buttons.add(new UiButton(new Rectangle(x, y, w, h), "overwrite-modal", () -> {
        }));
        g.setColor(new Color(5, 7, 11, 190));
        g.fillRoundRect(x + 34, y + 118, w - 68, 256, 10, 10);
        g.setColor(new Color(171, 115, 82));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(x + 34, y + 118, w - 68, 256, 10, 10);
        g.setFont(new Font("Serif", Font.BOLD, 28));
        g.setColor(new Color(244, 213, 141));
        drawCenteredIn(g, "Overwrite Save?", x + 34, y + 162, w - 68);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(218, 221, 229));
        drawCenteredIn(g, "This will replace \"" + shortText(pendingOverwriteSaveName, 38) + "\" with your current adventure.", x + 64, y + 206, w - 128);
        g.setColor(new Color(181, 187, 201));
        drawCenteredIn(g, "A new copy will not be created.", x + 64, y + 232, w - 128);
        String saveName = state.pendingSaveName.isBlank() ? nextDefaultSaveName() : state.pendingSaveName;
        g.setColor(new Color(147, 154, 172));
        drawCenteredIn(g, "New name: " + shortText(saveName, 42), x + 64, y + 260, w - 128);
        actionButton(g, x + 170, y + 306, 150, 38, "Cancel", this::cancelOverwrite, new Color(48, 55, 70), new Color(89, 102, 125), true);
        actionButton(g, x + w - 320, y + 306, 150, 38, "Overwrite", this::confirmOverwriteSave, new Color(111, 58, 48), new Color(190, 103, 85), true);
    }

    private String formatSaveTime(long savedAtMillis) {
        if (savedAtMillis <= 0) {
            return "Saved recently";
        }
        return SAVE_TIME_FORMAT.format(Instant.ofEpochMilli(savedAtMillis));
    }

    private void drawOverlayBase(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(new Color(9, 10, 16, 208));
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

    private int centeredX(int width) {
        return Math.max(0, (viewWidth() - width) / 2);
    }

    private int gameAreaCenteredX(int width) {
        return Math.max(0, (gameAreaWidth() - width) / 2);
    }

    private int centeredY(int height) {
        return Math.max(0, (viewHeight() - height) / 2);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private Color blend(Color first, Color second, double amount) {
        double t = clamp(amount, 0.0, 1.0);
        int red = (int) Math.round(first.getRed() + (second.getRed() - first.getRed()) * t);
        int green = (int) Math.round(first.getGreen() + (second.getGreen() - first.getGreen()) * t);
        int blue = (int) Math.round(first.getBlue() + (second.getBlue() - first.getBlue()) * t);
        int alpha = (int) Math.round(first.getAlpha() + (second.getAlpha() - first.getAlpha()) * t);
        return new Color(clamp(red, 0, 255), clamp(green, 0, 255), clamp(blue, 0, 255), clamp(alpha, 0, 255));
    }

    private int tileSize() {
        int size = Math.max(24, GameConfig.TILE * state.zoom / 100);
        if (!"overworld".equals(state.world.kind(state.currentMapId))) {
            int mapWidth = Math.max(1, state.world.width(state.currentMapId));
            int mapHeight = Math.max(1, state.world.height(state.currentMapId));
            int fitWidth = (int) Math.ceil(gameAreaWidth() / (double) mapWidth);
            int fitHeight = (int) Math.ceil(viewHeight() / (double) mapHeight);
            size = Math.max(size, Math.max(fitWidth, fitHeight));
        }
        return size;
    }

    private int scaled(int value) {
        return Math.max(1, value * state.zoom / 100);
    }

    private int tileRelative(int value, int tileSize) {
        return Math.max(1, Math.round(value * tileSize / (float) GameConfig.TILE));
    }

    private int viewWidth() {
        return renderWidth;
    }

    private int viewHeight() {
        return GameConfig.HEIGHT;
    }

    private int gameAreaWidth() {
        int sidebarWidth = uiHidden ? 0 : GameConfig.SIDEBAR_WIDTH;
        return Math.max(GameConfig.TILE, viewWidth() - sidebarWidth);
    }

    private int villagePanelTop() {
        return Math.max(150, viewHeight() - 520);
    }

    private int worldViewportHeight() {
        return viewHeight();
    }

    private void updateViewportTransform() {
        double scale = getHeight() / (double) GameConfig.HEIGHT;
        if (!Double.isFinite(scale) || scale <= 0.0) {
            scale = 1.0;
        }
        renderScaleX = scale;
        renderScaleY = scale;
        renderWidth = Math.max(GameConfig.WIDTH, (int) Math.ceil(getWidth() / scale));
        renderOffsetX = 0;
        renderOffsetY = 0;
    }

    private Point logicalPoint(MouseEvent event) {
        updateViewportTransform();
        int scaledWidth = (int) Math.round(viewWidth() * renderScaleX);
        int scaledHeight = (int) Math.round(viewHeight() * renderScaleY);
        if (event.getX() < renderOffsetX || event.getY() < renderOffsetY
                || event.getX() >= renderOffsetX + scaledWidth || event.getY() >= renderOffsetY + scaledHeight) {
            return null;
        }
        int x = clamp((int) Math.floor((event.getX() - renderOffsetX) / renderScaleX), 0, viewWidth() - 1);
        int y = clamp((int) Math.floor((event.getY() - renderOffsetY) / renderScaleY), 0, GameConfig.HEIGHT - 1);
        return new Point(x, y);
    }

    private void clearPlayerPath() {
        playerPath.clear();
        playerPathIndex = 0;
        playerPathDestination = null;
    }

    private void setPlayerPath(List<TilePoint> path, TilePoint destination) {
        playerPath.clear();
        playerPath.addAll(path);
        playerPathIndex = 0;
        playerPathDestination = destination;
    }

    private boolean playerPathExhausted() {
        return playerPathIndex >= playerPath.size();
    }

    private void clearContextMenu() {
        contextMenuTile = null;
        contextMenuSettlement = null;
        contextMenuBuilding = null;
        contextMenuPartyMember = null;
        contextMenuPoint = null;
    }

    private void clearQueuedMove() {
        queuedMoveDx = 0;
        queuedMoveDy = 0;
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
        renderMetrics.sample("path.requestDistance", Math.abs(directDx) + Math.abs(directDy));
        long nearestStarted = renderMetrics.start();
        TilePoint target = Pathfinder.nearestTarget(state, targetX, targetY);
        renderMetrics.record("path.nearestTarget", nearestStarted);
        if (target == null) {
            renderMetrics.sample("path.resultLength", 0);
            clearPlayerPath();
            state.status = "No walkable route there.";
            return;
        }
        if (target.x() == state.playerX && target.y() == state.playerY) {
            clearPlayerPath();
            state.interact();
            return;
        }
        long pathStarted = renderMetrics.start();
        List<TilePoint> path = Pathfinder.findPath(state, new TilePoint(state.playerX, state.playerY), target);
        renderMetrics.record("path.findPath", pathStarted);
        renderMetrics.sample("path.resultLength", path.size());
        if (path.isEmpty()) {
            clearPlayerPath();
            state.status = "No walkable route there.";
            return;
        }
        setPlayerPath(path, target);
        tickPlayerPath();
    }

    private void tickPlayerPath() {
        if (state.mode != GameMode.EXPLORE) {
            clearPlayerPath();
            clearQueuedMove();
            clearHeldMovementKeys();
            pendingGatherTile = null;
            pendingReadTile = null;
            pendingCraftTile = null;
            pendingTalkNpc = null;
            pendingTalkPartyMember = null;
            pendingTalkPartyTile = null;
            pendingEnterBuilding = null;
            pendingPortalTile = null;
            return;
        }
        if (playerMoving()) {
            return;
        }
        if (queuedMoveDx != 0 || queuedMoveDy != 0) {
            int dx = queuedMoveDx;
            int dy = queuedMoveDy;
            clearQueuedMove();
            startPlayerMove(dx, dy, false);
            return;
        }
        if (playerPathExhausted()) {
            if (tryStartPendingPortal()) {
                playerPathDestination = null;
                return;
            }
            if (tryStartPendingEnterBuilding()) {
                playerPathDestination = null;
                return;
            }
            if (tryStartPendingPartyTalk()) {
                playerPathDestination = null;
                return;
            }
            if (tryStartPendingTalk()) {
                playerPathDestination = null;
                return;
            }
            if (tryStartPendingRead()) {
                playerPathDestination = null;
                return;
            }
            if (tryStartPendingCraft()) {
                playerPathDestination = null;
                return;
            }
            if (tryStartPendingGather()) {
                playerPathDestination = null;
                return;
            }
            int[] heldDirection = heldMoveDirection();
            if (heldDirection[0] != 0 || heldDirection[1] != 0) {
                startPlayerMove(heldDirection[0], heldDirection[1], false);
                return;
            }
        }
        if (playerPathExhausted()) {
            playerPathDestination = null;
            return;
        }
        TilePoint next = playerPath.get(playerPathIndex++);
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

    private boolean tryStartPendingEnterBuilding() {
        if (pendingEnterBuilding == null) {
            return false;
        }
        if (state.mode != GameMode.EXPLORE || !isSettlementMapKind(state.currentMapId)) {
            pendingEnterBuilding = null;
            return false;
        }
        if (state.enterBuilding(pendingEnterBuilding)) {
            pendingEnterBuilding = null;
            clearPlayerPath();
            return true;
        }
        if (playerPathDestination == null) {
            pendingEnterBuilding = null;
        }
        return false;
    }

    private boolean tryStartPendingPortal() {
        if (pendingPortalTile == null) {
            return false;
        }
        if (state.mode != GameMode.EXPLORE) {
            pendingPortalTile = null;
            return false;
        }
        if (state.townPortalNearPlayer() != null) {
            pendingPortalTile = null;
            clearPlayerPath();
            state.openFastTravel();
            return true;
        }
        if (playerPathDestination == null) {
            pendingPortalTile = null;
        }
        return false;
    }

    private boolean tryStartPendingGather() {
        if (pendingGatherTile == null) {
            return false;
        }
        if (state.mode != GameMode.EXPLORE || state.crafting.active()) {
            pendingGatherTile = null;
            return false;
        }
        int distance = Math.abs(pendingGatherTile.x() - state.playerX) + Math.abs(pendingGatherTile.y() - state.playerY);
        if (distance <= 1) {
            TilePoint tile = pendingGatherTile;
            pendingGatherTile = null;
            state.gatherAtTile(tile.x(), tile.y());
            return true;
        }
        if (playerPathDestination == null) {
            pendingGatherTile = null;
        }
        return false;
    }

    private boolean tryStartPendingRead() {
        if (pendingReadTile == null) {
            return false;
        }
        if (state.mode != GameMode.EXPLORE || state.crafting.active()) {
            pendingReadTile = null;
            return false;
        }
        int distance = Math.abs(pendingReadTile.x() - state.playerX) + Math.abs(pendingReadTile.y() - state.playerY);
        if (distance <= 1) {
            TilePoint tile = pendingReadTile;
            pendingReadTile = null;
            state.readBookshelfAtTile(tile.x(), tile.y());
            return true;
        }
        if (playerPathDestination == null) {
            pendingReadTile = null;
        }
        return false;
    }

    private boolean tryStartPendingCraft() {
        if (pendingCraftTile == null) {
            return false;
        }
        if (state.mode != GameMode.EXPLORE || state.crafting.active()) {
            pendingCraftTile = null;
            return false;
        }
        int distance = Math.abs(pendingCraftTile.x() - state.playerX) + Math.abs(pendingCraftTile.y() - state.playerY);
        if (distance <= 1) {
            TilePoint tile = pendingCraftTile;
            pendingCraftTile = null;
            state.openCraftingAtTile(tile.x(), tile.y());
            return true;
        }
        if (playerPathDestination == null) {
            pendingCraftTile = null;
        }
        return false;
    }

    private boolean tryStartPendingPartyTalk() {
        if (pendingTalkPartyMember == null) {
            return false;
        }
        if (state.mode != GameMode.EXPLORE || state.crafting.active()) {
            pendingTalkPartyMember = null;
            pendingTalkPartyTile = null;
            return false;
        }
        TilePoint currentPartyTile = partyFollowerTile(pendingTalkPartyMember);
        TilePoint talkTile = currentPartyTile == null ? pendingTalkPartyTile : currentPartyTile;
        if (talkTile != null
                && Math.abs(talkTile.x() - state.playerX) + Math.abs(talkTile.y() - state.playerY) > 1) {
            if (playerPathDestination == null) {
                Actor ally = pendingTalkPartyMember;
                pendingTalkPartyMember = null;
                pendingTalkPartyTile = null;
                startTalkToPartyMember(ally, talkTile);
                return true;
            }
            return false;
        }
        Actor ally = pendingTalkPartyMember;
        pendingTalkPartyMember = null;
        pendingTalkPartyTile = null;
        clearPlayerPath();
        if (!state.talkToPartyAlly(ally)) {
            state.status = ally.name + " is not ready to talk right now.";
        }
        return true;
    }

    private boolean tryStartPendingTalk() {
        if (pendingTalkNpc == null) {
            return false;
        }
        if (state.mode != GameMode.EXPLORE || state.crafting.active()) {
            pendingTalkNpc = null;
            return false;
        }
        if (state.talkToNpc(pendingTalkNpc)) {
            pendingTalkNpc = null;
            return true;
        }
        if (playerPathDestination == null) {
            Npc npc = pendingTalkNpc;
            pendingTalkNpc = null;
            startTalkToNpc(npc);
            return true;
        }
        return false;
    }

    private void startPlayerMove(int dx, int dy) {
        startPlayerMove(dx, dy, false);
    }

    private void startPlayerMove(int dx, int dy, boolean keepPath) {
        if (Math.abs(dx) + Math.abs(dy) != 1) {
            return;
        }
        if (playerMoving()) {
            if (!keepPath && Math.abs(dx) + Math.abs(dy) == 1) {
                clearPlayerPath();
                queuedMoveDx = dx;
                queuedMoveDy = dy;
            }
            return;
        }
        if (!keepPath) {
            pendingGatherTile = null;
            pendingReadTile = null;
            pendingCraftTile = null;
            pendingTalkNpc = null;
            pendingTalkPartyMember = null;
            pendingTalkPartyTile = null;
            pendingPortalTile = null;
            clearPlayerPath();
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
            playerMoveDurationFrames = playerMoveFramesForStep(mapBefore, fromX, fromY, state.playerX, state.playerY);
            recordFollowerTrailStep(mapBefore, fromX, fromY, playerMoveDurationFrames, dx, dy);
            playerMoveFromX = fromX;
            playerMoveFromY = fromY;
            playerMoveStartFrame = frame;
            playerWalkAnimationTileStart = playerWalkAnimationTiles;
            playerWalkAnimationTiles++;
        } else {
            syncPlayerAnimationToState();
            clearQueuedMove();
            if (keepPath) {
                clearPlayerPath();
            }
        }
    }

    boolean handleMovementKeyPressed(int code) {
        int[] direction = movementDirectionForKey(code);
        if (direction == null) {
            return false;
        }
        setMovementKeyHeld(code, true);
        lastHeldMoveDx = direction[0];
        lastHeldMoveDy = direction[1];
        pendingGatherTile = null;
        clearPlayerPath();
        startPlayerMove(direction[0], direction[1]);
        return true;
    }

    boolean handleMovementKeyReleased(int code) {
        if (movementDirectionForKey(code) == null) {
            return false;
        }
        setMovementKeyHeld(code, false);
        if (heldMoveDirection()[0] == 0 && heldMoveDirection()[1] == 0) {
            lastHeldMoveDx = 0;
            lastHeldMoveDy = 0;
        }
        return true;
    }

    private int[] movementDirectionForKey(int code) {
        return switch (code) {
            case KeyEvent.VK_W, KeyEvent.VK_UP -> new int[]{0, -1};
            case KeyEvent.VK_S, KeyEvent.VK_DOWN -> new int[]{0, 1};
            case KeyEvent.VK_A, KeyEvent.VK_LEFT -> new int[]{-1, 0};
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> new int[]{1, 0};
            default -> null;
        };
    }

    private void setMovementKeyHeld(int code, boolean held) {
        switch (code) {
            case KeyEvent.VK_W, KeyEvent.VK_UP -> moveUpHeld = held;
            case KeyEvent.VK_S, KeyEvent.VK_DOWN -> moveDownHeld = held;
            case KeyEvent.VK_A, KeyEvent.VK_LEFT -> moveLeftHeld = held;
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> moveRightHeld = held;
            default -> {
            }
        }
    }

    private void clearHeldMovementKeys() {
        moveUpHeld = false;
        moveDownHeld = false;
        moveLeftHeld = false;
        moveRightHeld = false;
        lastHeldMoveDx = 0;
        lastHeldMoveDy = 0;
    }

    void syncPlayerAnimationToState() {
        clearPlayerPath();
        clearQueuedMove();
        playerMoveMapId = state.currentMapId;
        playerMoveFromX = state.playerX;
        playerMoveFromY = state.playerY;
        playerMoveStartFrame = frame - playerMoveFrames();
        followerTrailMapId = state.currentMapId;
        playerTrail.clear();
        followerVisuals.clear();
        visiblePartyFollowers.clear();
        playerWalkAnimationTileStart = 0;
        playerWalkAnimationTiles = 0;
        resetCameraToPlayer();
    }

    void saveGame() {
        try {
            if (state.pendingSaveName.isBlank() || state.mode != GameMode.SAVE_MENU) {
                state.setPendingSaveName(nextDefaultSaveName());
            }
            String savedName = state.pendingSaveName;
            saves.save(state, savedName);
            state.status = "Saved " + savedName + " for " + state.player.name + ".";
            state.setPendingSaveName(nextDefaultSaveName());
            saveListCurrentCharacterOnly = true;
            saveListScroll = 0;
        } catch (Exception ex) {
            state.status = "Save failed: " + exceptionMessage(ex);
        }
    }

    void openSaveMenuForSaving() {
        state.openSaveMenu(true);
        selectedLoadCharacterId = "";
        importingCharacter = false;
        state.setPendingSaveName(nextDefaultSaveName());
        saveListCurrentCharacterOnly = true;
        saveListScroll = 0;
    }

    void openLoadMenu() {
        state.openSaveMenu(false);
        selectedLoadCharacterId = "";
        importingCharacter = false;
        saveListScroll = 0;
    }

    void openImportMenu() {
        state.openSaveMenu(false);
        selectedLoadCharacterId = "";
        importingCharacter = true;
        state.status = "Choose a saved character to import into a new adventure.";
        saveListScroll = 0;
    }

    void closeSaveMenuOrLoadFolder() {
        if (!state.saveMenuCanSave && !selectedLoadCharacterId.isBlank()) {
            selectedLoadCharacterId = "";
            saveListScroll = 0;
            return;
        }
        if (!state.saveMenuCanSave) {
            importingCharacter = false;
        }
        state.closeSaveMenu();
    }

    private String nextDefaultSaveName() {
        String location = state.world.label(state.currentMapId);
        if (location == null || location.isBlank()) {
            location = "Adventure";
        }
        int next = nextSaveNumberForCurrentHero();
        String suffix = " - Save " + next;
        int maxLocationLength = Math.max(1, 32 - suffix.length());
        if (location.length() > maxLocationLength) {
            location = location.substring(0, maxLocationLength).stripTrailing();
        }
        return location + suffix;
    }

    private int nextSaveNumberForCurrentHero() {
        List<SaveSystem.SaveSummary> heroSaves = saves.listSavesForCharacter(SaveSystem.saveIdFor(state.player.name));
        int next = heroSaves.size() + 1;
        for (SaveSystem.SaveSummary summary : heroSaves) {
            next = Math.max(next, saveNumberFromName(summary.saveName()) + 1);
        }
        return next;
    }

    private int saveNumberFromName(String saveName) {
        if (saveName == null) {
            return 0;
        }
        String marker = " - Save ";
        int markerIndex = saveName.lastIndexOf(marker);
        if (markerIndex < 0) {
            return 0;
        }
        try {
            return Integer.parseInt(saveName.substring(markerIndex + marker.length()).strip());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void requestOverwrite(SaveSystem.SaveSummary summary) {
        pendingOverwriteSaveId = summary.saveId();
        pendingOverwriteSaveName = summary.saveName();
        if (state.pendingSaveName.isBlank()) {
            state.setPendingSaveName(summary.saveName());
        }
        state.status = "Confirm overwrite for " + summary.saveName() + ".";
    }

    void confirmOverwriteSave() {
        if (!hasPendingOverwrite()) {
            return;
        }
        try {
            if (state.pendingSaveName.isBlank()) {
                state.setPendingSaveName(nextDefaultSaveName());
            }
            String overwrittenName = pendingOverwriteSaveName;
            saves.overwrite(state, pendingOverwriteSaveId, state.pendingSaveName);
            state.status = "Overwrote " + overwrittenName + ".";
            cancelOverwrite();
            saveListCurrentCharacterOnly = true;
            saveListScroll = 0;
        } catch (Exception ex) {
            state.status = "Overwrite failed: " + exceptionMessage(ex);
            cancelOverwrite();
        }
    }

    private String exceptionMessage(Exception ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
    }

    void cancelOverwrite() {
        pendingOverwriteSaveId = "";
        pendingOverwriteSaveName = "";
    }

    boolean hasPendingOverwrite() {
        return pendingOverwriteSaveId != null && !pendingOverwriteSaveId.isBlank();
    }

    void loadGame() {
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

    void loadGame(String saveId) {
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

    void loadOrImportGame(String saveId) {
        if (importingCharacter) {
            importCharacter(saveId);
        } else {
            loadGame(saveId);
        }
    }

    private void importCharacter(String saveId) {
        try {
            if (!saves.importCharacter(state, saveId)) {
                state.status = "No Java save found.";
            } else {
                importingCharacter = false;
                selectedLoadCharacterId = "";
                state.resetNpcRuntime();
                syncPlayerAnimationToState();
            }
        } catch (IOException ex) {
            state.status = "Import failed: " + ex.getMessage();
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

    private void adjustMonsterLevelScaling(double delta) {
        setMonsterLevelScaling(state.config.monsterLevelScaling + delta);
    }

    private void setChanceSetting(String key, double value) {
        double rounded = Math.round(clamp(value, 0.0, 1.0) * 100.0) / 100.0;
        double before = chanceSetting(key);
        switch (key) {
            case "wild" -> state.config.wildEncounterChance = rounded;
            case "dungeon" -> state.config.dungeonEncounterChance = rounded;
            case "dungeon_entrance" -> state.config.dungeonEntranceEncounterChance = rounded;
            case "two_monsters" -> state.config.twoMonsterChance = rounded;
            case "three_monsters" -> state.config.threeMonsterChance = rounded;
            default -> {
                return;
            }
        }
        if (Math.abs(before - rounded) > 0.0001) {
            state.status = "Settings updated.";
            saveSettings();
        }
    }

    private double chanceSetting(String key) {
        return switch (key) {
            case "wild" -> state.config.wildEncounterChance;
            case "dungeon" -> state.config.dungeonEncounterChance;
            case "dungeon_entrance" -> state.config.dungeonEntranceEncounterChance;
            case "two_monsters" -> state.config.twoMonsterChance;
            case "three_monsters" -> state.config.threeMonsterChance;
            default -> 0.0;
        };
    }

    private void setMonsterLevelScaling(double value) {
        double rounded = Math.round(clamp(value, 0.0, 1.0) * 100.0) / 100.0;
        double before = state.config.monsterLevelScaling;
        state.config.monsterLevelScaling = rounded;
        if (Math.abs(before - rounded) > 0.0001) {
            state.status = "Monster difficulty scaling updated.";
            saveSettings();
        }
    }

    private void setVolumeSetting(String key, int value) {
        int clamped = clamp(value, 0, 100);
        int before = volumeSetting(key);
        switch (key) {
            case "master" -> state.config.masterVolume = clamped;
            case "music" -> state.config.musicVolume = clamped;
            case "sfx" -> state.config.sfxVolume = clamped;
            default -> {
                return;
            }
        }
        if (before != clamped) {
            state.status = "Audio settings updated.";
            saveSettings();
        }
    }

    private int volumeSetting(String key) {
        return switch (key) {
            case "master" -> state.config.masterVolume;
            case "music" -> state.config.musicVolume;
            case "sfx" -> state.config.sfxVolume;
            default -> 0;
        };
    }

    private void toggleCreativeBuildMode() {
        state.config.creativeBuildMode = !state.config.creativeBuildMode;
        state.status = state.config.creativeBuildMode
                ? "Creative build mode enabled."
                : "Building costs enabled.";
        saveSettings();
    }

    private void toggleMapEditorButton() {
        state.config.showMapEditorButton = !state.config.showMapEditorButton;
        state.status = state.config.showMapEditorButton
                ? "Map editor button shown."
                : "Map editor button hidden.";
        saveSettings();
    }

    private void exportMapEditorDesign() {
        String export = state.exportMapEditorDesign();
        if (export.isBlank()) {
            return;
        }
        try {
            Path folder = javaRoot.resolve("exports");
            Files.createDirectories(folder);
            String kind = state.world.kind(state.currentMapId);
            String fileName = "map-editor-" + kind + "-" + Instant.now().toEpochMilli() + ".properties";
            Path path = folder.resolve(fileName);
            Files.writeString(path, export);
            state.status = "Exported design to exports/" + fileName + ".";
        } catch (IOException ex) {
            state.status = "Export failed: " + ex.getMessage();
        }
    }

    private void previousCameraMode() {
        cycleCameraMode(-1);
    }

    private void nextCameraMode() {
        cycleCameraMode(1);
    }

    private void cycleCameraMode(int delta) {
        CameraMode[] modes = CameraMode.values();
        int index = 0;
        for (int i = 0; i < modes.length; i++) {
            if (modes[i] == cameraMode()) {
                index = i;
                break;
            }
        }
        CameraMode next = modes[Math.floorMod(index + delta, modes.length)];
        state.config.cameraMode = next.key;
        state.status = "Camera mode: " + next.label + ".";
        resetCameraToPlayer();
        saveSettings();
    }

    private void previousMovementSpeed() {
        cycleMovementSpeed(-1);
    }

    private void nextMovementSpeed() {
        cycleMovementSpeed(1);
    }

    private void cycleMovementSpeed(int delta) {
        MovementSpeed[] speeds = MovementSpeed.values();
        int index = 0;
        for (int i = 0; i < speeds.length; i++) {
            if (speeds[i] == movementSpeed()) {
                index = i;
                break;
            }
        }
        MovementSpeed next = speeds[Math.floorMod(index + delta, speeds.length)];
        state.config.movementSpeed = next.key;
        state.status = "Movement speed: " + next.label + ".";
        saveSettings();
    }

    private void previousWeatherQuality() {
        cycleWeatherQuality(-1);
    }

    private void nextWeatherQuality() {
        cycleWeatherQuality(1);
    }

    private void cycleWeatherQuality(int delta) {
        WeatherQuality[] qualities = WeatherQuality.values();
        int index = 0;
        for (int i = 0; i < qualities.length; i++) {
            if (qualities[i] == weatherQuality()) {
                index = i;
                break;
            }
        }
        WeatherQuality next = qualities[Math.floorMod(index + delta, qualities.length)];
        state.config.weatherQuality = next.key;
        state.status = "Weather quality: " + next.label + ".";
        saveSettings();
    }

    private void setCameraSmoothing(int value) {
        int clamped = clamp(value, 0, 100);
        if (state.config.cameraSmoothing != clamped) {
            state.config.cameraSmoothing = clamped;
            state.status = "Camera smoothing updated.";
            saveSettings();
        }
    }

    private void toggleCameraLookAhead() {
        state.config.cameraLookAhead = !state.config.cameraLookAhead;
        state.status = state.config.cameraLookAhead ? "Camera look-ahead enabled." : "Camera look-ahead disabled.";
        saveSettings();
    }

    private void saveSettings() {
        try {
            state.config.save(javaRoot);
        } catch (IOException ex) {
            state.status = "Settings save failed: " + ex.getMessage();
        }
    }

    void exitGame() {
        audio.shutdown();
        java.awt.Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.dispose();
        }
        System.exit(0);
    }

    public void shutdown() {
        audio.shutdown();
    }

    private UiButton buttonAt(Point point) {
        if (point == null) {
            return null;
        }
        for (int i = buttons.size() - 1; i >= 0; i--) {
            UiButton button = buttons.get(i);
            if (button.contains(point.x, point.y)) {
                return button;
            }
        }
        return null;
    }

    private UiSlider sliderAt(Point point) {
        if (point == null) {
            return null;
        }
        for (int i = sliders.size() - 1; i >= 0; i--) {
            UiSlider slider = sliders.get(i);
            if (slider.contains(point.x, point.y)) {
                return slider;
            }
        }
        return null;
    }

    private void updateActiveSlider(Point point) {
        if (activeSlider == null || point == null) {
            return;
        }
        double fraction = clamp((point.x - activeSlider.track().x) / (double) activeSlider.track().width, 0.0, 1.0);
        switch (activeSlider.kind()) {
            case CHANCE -> setChanceSetting(activeSlider.key(), fraction);
            case DIFFICULTY -> setMonsterLevelScaling(fraction);
            case VOLUME -> setVolumeSetting(activeSlider.key(), (int) Math.round(fraction * 100));
            case CAMERA -> setCameraSmoothing((int) Math.round(fraction * 100));
        }
    }

    private void startInventoryDrag(Point point) {
        if (point == null) {
            return;
        }
        for (int i = buttons.size() - 1; i >= 0; i--) {
            UiButton button = buttons.get(i);
            if (button.contains(point.x, point.y) && !button.label().startsWith("inventory-item:")) {
                return;
            }
        }
        for (int i = inventoryDragZones.size() - 1; i >= 0; i--) {
            InventoryDragZone zone = inventoryDragZones.get(i);
            if (zone.bounds().contains(point)) {
                inventoryDrag = new InventoryDrag(zone.itemKey(), zone.sourceSlot());
                inventoryDragStart = point;
                inventoryDragPoint = point;
                inventoryDragMoved = false;
                return;
            }
        }
    }

    private void dropInventoryDrag(Point point) {
        InventoryDropZone target = inventoryDropTarget(point);
        if (target == null || inventoryDrag == null) {
            return;
        }
        if (inventoryDrag.sourceSlot() != null) {
            if (target.kind() == InventoryDropKind.PACK) {
                state.unequipPartySlot(inventoryDrag.sourceSlot());
            } else {
                state.status = "Drop equipped gear into the shared pack to remove it.";
            }
            return;
        }
        if (target.kind() == InventoryDropKind.SLOT) {
            state.equipInventoryItemInSlot(inventoryDrag.itemKey(), target.slot());
        } else if (target.kind() == InventoryDropKind.CHARACTER) {
            state.usePartyInventoryItem(inventoryDrag.itemKey());
        } else {
            state.status = GameData.itemName(inventoryDrag.itemKey()) + " is already in the shared pack.";
        }
    }

    private InventoryDropZone inventoryDropTarget(Point point) {
        for (int i = inventoryDropZones.size() - 1; i >= 0; i--) {
            InventoryDropZone zone = inventoryDropZones.get(i);
            if (zone.kind() == InventoryDropKind.SLOT && zone.bounds().contains(point)) {
                return zone;
            }
        }
        for (int i = inventoryDropZones.size() - 1; i >= 0; i--) {
            InventoryDropZone zone = inventoryDropZones.get(i);
            if (zone.bounds().contains(point)) {
                return zone;
            }
        }
        return null;
    }

    private void startAbilityDrag(Point point) {
        if (point == null || (state.mode != GameMode.PARTY && state.mode != GameMode.SKILLS) || activeSkillTab != SkillTab.LOADOUT) {
            return;
        }
        for (int i = buttons.size() - 1; i >= 0; i--) {
            UiButton button = buttons.get(i);
            if (button.contains(point.x, point.y)) {
                return;
            }
        }
        for (int i = abilityDragZones.size() - 1; i >= 0; i--) {
            AbilityDragZone zone = abilityDragZones.get(i);
            if (zone.bounds().contains(point)) {
                abilityDrag = new AbilityDrag(zone.abilityName(), zone.sourceSlot());
                abilityDragStart = point;
                abilityDragPoint = point;
                abilityDragMoved = false;
                return;
            }
        }
    }

    private void dropAbilityDrag(Point point) {
        if (abilityDrag == null || point == null) {
            return;
        }
        AbilityDropZone target = abilityDropTarget(point);
        if (target == null) {
            return;
        }
        if (target.remove()) {
            state.removePartyAbilityFromLoadout(abilityDrag.abilityName());
        } else {
            state.placePartyAbilityInLoadout(abilityDrag.abilityName(), target.slot());
        }
    }

    private AbilityDropZone abilityDropTarget(Point point) {
        for (int i = abilityDropZones.size() - 1; i >= 0; i--) {
            AbilityDropZone zone = abilityDropZones.get(i);
            if (zone.bounds().contains(point)) {
                return zone;
            }
        }
        return null;
    }

    private boolean openWorldContextMenu(Point point) {
        if (state.mode != GameMode.EXPLORE || point == null) {
            return false;
        }
        hoverPoint = point;
        WorldMap.SettlementSite settlement = settlementAtScreenPoint(point);
        if (settlement != null) {
            contextMenuSettlement = settlement;
            contextMenuBuilding = null;
            contextMenuPartyMember = null;
            contextMenuTile = new TilePoint(settlement.x(), settlement.y());
            contextMenuPoint = new Point(point);
            return true;
        }
        PartyFollowerRender follower = partyFollowerAtPoint(point);
        if (follower != null) {
            contextMenuSettlement = null;
            contextMenuBuilding = null;
            contextMenuPartyMember = follower.actor();
            contextMenuTile = follower.tile();
            contextMenuPoint = new Point(point);
            return true;
        }
        TilePoint tile = hoverWorldTileInViewport();
        if (tile != null && state.npcAt(state.currentMapId, tile.x(), tile.y()) != null) {
            contextMenuSettlement = null;
            contextMenuBuilding = null;
            contextMenuPartyMember = null;
            contextMenuTile = tile;
            contextMenuPoint = new Point(point);
            return true;
        }
        CityBuilding building = buildingAtScreenPoint(point);
        if (building != null) {
            contextMenuBuilding = building;
            contextMenuSettlement = null;
            contextMenuPartyMember = null;
            contextMenuTile = tile;
            contextMenuPoint = new Point(point);
            return true;
        }
        if (tile == null) {
            clearContextMenu();
            return false;
        }
        contextMenuSettlement = null;
        contextMenuBuilding = null;
        contextMenuPartyMember = null;
        contextMenuTile = tile;
        contextMenuPoint = new Point(point);
        return true;
    }

    private boolean openContextMenu(Point point) {
        if (point == null) {
            clearContextMenu();
            return false;
        }
        if ((state.mode == GameMode.EXPLORE || state.mode == GameMode.PARTY || state.mode == GameMode.SKILLS)
                && openPartyPortraitContextMenu(point)) {
            return true;
        }
        if (state.mode == GameMode.PARTY || state.mode == GameMode.SKILLS) {
            return openPartyViewContextMenu(point);
        }
        return openWorldContextMenu(point);
    }

    private boolean openPartyPortraitContextMenu(Point point) {
        hoverPoint = point;
        for (int i = partyPortraitZones.size() - 1; i >= 0; i--) {
            PartyPortraitZone zone = partyPortraitZones.get(i);
            if (!zone.bounds().contains(point)) {
                continue;
            }
            Actor actor = zone.actor();
            if (actor == null || !state.partyMembers().contains(actor)) {
                clearContextMenu();
                return false;
            }
            contextMenuSettlement = null;
            contextMenuBuilding = null;
            contextMenuTile = null;
            contextMenuPartyMember = actor;
            contextMenuPoint = new Point(point);
            return true;
        }
        return false;
    }

    private boolean openPartyViewContextMenu(Point point) {
        hoverPoint = point;
        for (int i = partyPortraitZones.size() - 1; i >= 0; i--) {
            PartyPortraitZone zone = partyPortraitZones.get(i);
            if (!zone.bounds().contains(point)) {
                continue;
            }
            Actor actor = zone.actor();
            if (actor == null || actor == state.player || !state.activeAllies().contains(actor)) {
                clearContextMenu();
                return false;
            }
            contextMenuSettlement = null;
            contextMenuBuilding = null;
            contextMenuTile = null;
            contextMenuPartyMember = actor;
            contextMenuPoint = new Point(point);
            return true;
        }
        clearContextMenu();
        return false;
    }

    private PartyFollowerRender partyFollowerAtPoint(Point point) {
        if (point == null) {
            return null;
        }
        for (int i = visiblePartyFollowers.size() - 1; i >= 0; i--) {
            PartyFollowerRender follower = visiblePartyFollowers.get(i);
            if (follower.screenBounds().contains(point)) {
                return follower;
            }
        }
        return null;
    }

    private final class Mouse extends MouseAdapter {
        @Override
        public void mouseMoved(MouseEvent event) {
            hoverPoint = logicalPoint(event);
            repaint();
        }

        @Override
        public void mousePressed(MouseEvent event) {
            requestFocusInWindow();
            hoverPoint = logicalPoint(event);
            if (SwingUtilities.isRightMouseButton(event) || event.isPopupTrigger()) {
                suppressNextClick = openContextMenu(hoverPoint);
                repaint();
                return;
            }
            if (SwingUtilities.isLeftMouseButton(event)) {
                if (contextMenuPoint != null && hoverPoint != null && !contextMenuBounds().contains(hoverPoint)) {
                    clearContextMenu();
                    suppressNextClick = true;
                    repaint();
                    return;
                }
                UiButton button = buttonAt(hoverPoint);
                if (button != null) {
                    pressedButtonBounds = new Rectangle(button.bounds());
                    pressedButtonLabel = button.label();
                }
                activeSlider = sliderAt(hoverPoint);
                if (activeSlider != null) {
                    updateActiveSlider(hoverPoint);
                    suppressNextClick = true;
                    repaint();
                    return;
                }
                if (state.mode == GameMode.WORLD_MAP && button == null && hoverPoint != null && worldMapBounds.contains(hoverPoint)) {
                    worldMapDragStart = hoverPoint;
                    worldMapDragStartCenterX = worldMapCenterX;
                    worldMapDragStartCenterY = worldMapCenterY;
                    worldMapDragged = false;
                    return;
                }
            }
            if (state.mode == GameMode.INVENTORY && SwingUtilities.isLeftMouseButton(event)) {
                startInventoryDrag(hoverPoint);
            }
            if ((state.mode == GameMode.PARTY || state.mode == GameMode.SKILLS) && SwingUtilities.isLeftMouseButton(event)) {
                startAbilityDrag(hoverPoint);
            }
            repaint();
        }

        @Override
        public void mouseDragged(MouseEvent event) {
            hoverPoint = logicalPoint(event);
            if (activeSlider != null) {
                updateActiveSlider(hoverPoint);
                repaint();
                return;
            }
            if (state.mode == GameMode.WORLD_MAP && worldMapDragStart != null && hoverPoint != null) {
                double dx = hoverPoint.x - worldMapDragStart.x;
                double dy = hoverPoint.y - worldMapDragStart.y;
                worldMapCenterX = worldMapDragStartCenterX - dx * worldMapViewport.worldW() / worldMapViewport.screenW();
                worldMapCenterY = worldMapDragStartCenterY - dy * worldMapViewport.worldH() / worldMapViewport.screenH();
                worldMapCenterX = clampWorldMapCenter(worldMapCenterX, WorldMap.COLS / worldMapZoom, WorldMap.COLS);
                worldMapCenterY = clampWorldMapCenter(worldMapCenterY, WorldMap.ROWS / worldMapZoom, WorldMap.ROWS);
                worldMapDragged = worldMapDragStart.distance(hoverPoint) > 3.0;
                repaint();
                return;
            }
            if (inventoryDrag != null && hoverPoint != null) {
                inventoryDragPoint = hoverPoint;
                inventoryDragMoved = inventoryDragStart != null && inventoryDragStart.distance(hoverPoint) > 4.0;
            }
            if (abilityDrag != null && hoverPoint != null) {
                abilityDragPoint = hoverPoint;
                abilityDragMoved = abilityDragStart != null && abilityDragStart.distance(hoverPoint) > 4.0;
            }
            repaint();
        }

        @Override
        public void mouseReleased(MouseEvent event) {
            Point point = logicalPoint(event);
            hoverPoint = point;
            Rectangle releaseButtonBounds = pressedButtonBounds == null ? null : new Rectangle(pressedButtonBounds);
            String releaseButtonLabel = pressedButtonLabel;
            if (activeSlider != null) {
                updateActiveSlider(point);
                activeSlider = null;
                pressedButtonBounds = null;
                pressedButtonLabel = null;
                suppressNextClick = true;
                repaint();
                return;
            }
            pressedButtonBounds = null;
            pressedButtonLabel = null;
            if (worldMapDragStart != null) {
                if (worldMapDragged) {
                    suppressNextClick = true;
                }
                worldMapDragStart = null;
                worldMapDragged = false;
                repaint();
                return;
            }
            if (inventoryDrag != null) {
                if (inventoryDragMoved && point != null) {
                    dropInventoryDrag(point);
                    suppressNextClick = true;
                }
                inventoryDrag = null;
                inventoryDragStart = null;
                inventoryDragPoint = null;
                inventoryDragMoved = false;
                repaint();
            }
            if (abilityDrag != null) {
                if (abilityDragMoved && point != null) {
                    dropAbilityDrag(point);
                    suppressNextClick = true;
                }
                abilityDrag = null;
                abilityDragStart = null;
                abilityDragPoint = null;
                abilityDragMoved = false;
                repaint();
            }
            if (SwingUtilities.isLeftMouseButton(event) && releaseButtonBounds != null && releaseButtonLabel != null && point != null) {
                UiButton button = buttonAt(point);
                if (button != null && releaseButtonBounds.equals(button.bounds()) && releaseButtonLabel.equals(button.label())) {
                    button.action().run();
                    return;
                }
            }
        }

        @Override
        public void mouseClicked(MouseEvent event) {
            requestFocusInWindow();
            if (suppressNextClick) {
                suppressNextClick = false;
                return;
            }
            if (state.dialogueVideoActive()) {
                state.dismissDialogueVideo();
                repaint();
                return;
            }
            if (SwingUtilities.isRightMouseButton(event) || event.isPopupTrigger()) {
                Point point = logicalPoint(event);
                openContextMenu(point);
                repaint();
                return;
            }
            Point point = logicalPoint(event);
            if (point == null) {
                return;
            }
            if (state.mode == GameMode.VILLAGE) {
                villageSearchFocused = villageSearchApplies() && villageSearchBounds.contains(point);
                if (villageSearchFocused) {
                    repaint();
                    return;
                }
            } else {
                villageSearchFocused = false;
            }
            for (int i = buttons.size() - 1; i >= 0; i--) {
                UiButton button = buttons.get(i);
                if (button.contains(point.x, point.y)) {
                    return;
                }
            }
            if (state.mode == GameMode.WORLD_MAP && worldMapBounds.contains(point)) {
                centerWorldMapOn(worldMapWorldX(worldMapViewport, point.x), worldMapWorldY(worldMapViewport, point.y));
                repaint();
            } else if (state.mode == GameMode.VILLAGE && point.x < gameAreaWidth() && point.y < viewHeight()) {
                clickVillageWorld(point.x, point.y);
            } else if (state.mode == GameMode.EXPLORE && point.x < gameAreaWidth() && point.y < viewHeight()) {
                clickWorld(point.x, point.y);
            }
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent event) {
            Point point = logicalPoint(event);
            if (state.mode == GameMode.DIALOG && point != null && dialogOptionScrollBounds.contains(point)) {
                dialogOptionScroll = Math.max(0, dialogOptionScroll + event.getWheelRotation());
                repaint();
                return;
            }
            if (state.mode == GameMode.BATTLE && state.battle != null && point != null && battleLogBounds.contains(point)) {
                battleLogScroll = Math.max(0, battleLogScroll - event.getWheelRotation() * 3);
                repaint();
                return;
            }
            if (state.mode == GameMode.WORLD_MAP) {
                zoomWorldMapAt(point, event.getWheelRotation() < 0 ? WORLD_MAP_ZOOM_STEP : 1.0 / WORLD_MAP_ZOOM_STEP);
                repaint();
                return;
            }
            if (point != null && statusLogBounds.contains(point)) {
                statusLogScroll = Math.max(0, statusLogScroll - event.getWheelRotation() * 3);
                repaint();
                return;
            }
            if (state.mode == GameMode.INVENTORY) {
                inventoryItemScroll = Math.max(0, inventoryItemScroll + event.getWheelRotation());
                repaint();
                return;
            }
            if (state.mode == GameMode.QUEST_LOG) {
                int rows = questLogEntries(questLogCompletedTab).size();
                questLogScroll = clamp(questLogScroll + event.getWheelRotation(), 0, Math.max(0, rows - 1));
                repaint();
                return;
            }
            if (state.mode == GameMode.CRAFTING) {
                int maxScroll = Math.max(0, displayedCraftingRecipes().size() - craftingRecipeVisibleSlots());
                craftingRecipeScroll = Math.max(0, Math.min(maxScroll, craftingRecipeScroll + event.getWheelRotation()));
                repaint();
                return;
            }
            if (state.mode == GameMode.SKILLS) {
                if (activeSkillTab == SkillTab.LOADOUT && point != null && partyLoadoutBounds.contains(point)) {
                    partyLoadoutScroll = Math.max(0, partyLoadoutScroll + event.getWheelRotation());
                } else {
                    skillTreeScroll = Math.max(0, skillTreeScroll + event.getWheelRotation());
                }
                repaint();
                return;
            }
            if (state.mode == GameMode.PARTY) {
                if (point != null && partyLoadoutBounds.contains(point)) {
                    partyLoadoutScroll = Math.max(0, partyLoadoutScroll + event.getWheelRotation());
                } else {
                    partySkillScroll = Math.max(0, partySkillScroll + event.getWheelRotation());
                }
                repaint();
                return;
            }
            if (state.mode == GameMode.SHOP) {
                shopItemScroll = Math.max(0, shopItemScroll + event.getWheelRotation());
                repaint();
                return;
            }
            if (state.mode == GameMode.SAVE_MENU) {
                saveListScroll = Math.max(0, saveListScroll + event.getWheelRotation());
                repaint();
                return;
            }
            if (state.mode == GameMode.VILLAGE) {
                villageListScroll = Math.max(0, villageListScroll + event.getWheelRotation());
                repaint();
                return;
            }
            if (state.mode != GameMode.MAIN_MENU && state.mode != GameMode.CLASS_SELECT && state.mode != GameMode.PAUSE_MENU) {
                state.adjustZoom(event.getWheelRotation() < 0 ? 10 : -10);
                repaint();
            }
        }

        private void clickWorld(int x, int y) {
            if (settlementAtScreenPoint(new Point(x, y)) != null) {
                clearContextMenu();
                pendingGatherTile = null;
                pendingTalkNpc = null;
                pendingTalkPartyMember = null;
                pendingTalkPartyTile = null;
                pendingEnterBuilding = null;
                pendingPortalTile = null;
                state.status = "Right-click a settlement to choose how to enter.";
                repaint();
                return;
            }
            int tileSize = tileSize();
            int targetX = (int) Math.floor(lastCameraX + x / (double) tileSize);
            int targetY = (int) Math.floor(lastCameraY + y / (double) tileSize);
            CityBuilding clickedBuilding = buildingAtScreenPoint(new Point(x, y));
            if (clickedBuilding != null
                    && clickedBuilding.contains(targetX, targetY)
                    && !state.world.isPassable(state.currentMapId, targetX, targetY)) {
                clearContextMenu();
                pendingGatherTile = null;
                pendingTalkNpc = null;
                pendingTalkPartyMember = null;
                pendingTalkPartyTile = null;
                pendingEnterBuilding = null;
                pendingPortalTile = null;
                state.status = "Right-click a building to enter or inspect it.";
                repaint();
                return;
            }
            WorldTransition transition = state.world.transitionAt(state.currentMapId, targetX, targetY);
            if (WorldMap.OVERWORLD_ID.equals(state.currentMapId) && transition != null && isSettlementMapKind(transition.targetMapId())) {
                clearContextMenu();
                pendingGatherTile = null;
                pendingTalkNpc = null;
                pendingTalkPartyMember = null;
                pendingTalkPartyTile = null;
                pendingEnterBuilding = null;
                pendingPortalTile = null;
                state.status = "Right-click a settlement to choose how to enter.";
                repaint();
                return;
            }
            clearContextMenu();
            pendingGatherTile = null;
            pendingTalkNpc = null;
            pendingTalkPartyMember = null;
            pendingTalkPartyTile = null;
            pendingEnterBuilding = null;
            pendingPortalTile = null;
            setPlayerPathDestination(targetX, targetY);
            repaint();
        }

        private void clickVillageWorld(int x, int y) {
            int tileSize = tileSize();
            int targetX = (int) Math.floor(lastCameraX + x / (double) tileSize);
            int targetY = (int) Math.floor(lastCameraY + y / (double) tileSize);
            boolean interiorEdit = state.isManagedVillageInterior();
            String actionBefore = state.villageEditAction;
            boolean placingInterior = interiorEdit && "place".equals(actionBefore);
            boolean movingInteriorToTarget = interiorEdit && "move".equals(actionBefore) && state.pendingVillageMoveSource != null;
            state.handleVillageWorldClick(targetX, targetY);
            if ((placingInterior || movingInteriorToTarget)
                    && ("Interior asset moved.".equals(state.status) || state.status.endsWith(" placed."))) {
                triggerInteriorPlacementPulse(targetX, targetY);
            }
            repaint();
        }
    }

    void runDialogOption(int index) {
        if (state.activeNpc == null) {
            return;
        }
        Quest quest = state.questForNpc(state.activeNpc);
        List<DialogOption> options = dialogOptions(state.activeNpc, quest);
        if (index >= 0 && index < options.size() && options.get(index).enabled()) {
            options.get(index).action().run();
        }
    }
}
