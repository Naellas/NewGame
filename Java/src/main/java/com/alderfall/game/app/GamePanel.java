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
import com.alderfall.game.render.battle.BattleVfxRenderer;
import com.alderfall.game.render.world.TerrainFeatureRenderer;
import com.alderfall.game.render.world.WorldAtmosphereRenderer;
import com.alderfall.game.render.world.WorldLightingRenderer;
import com.alderfall.game.render.world.WorldPropRenderer;
import com.alderfall.game.ui.CharacterRenderer;
import com.alderfall.game.ui.DialogueRenderer;
import com.alderfall.game.ui.QuestLogRenderer;
import com.alderfall.game.ui.InventoryRenderer;
import com.alderfall.game.ui.SaveMenuRenderer;
import com.alderfall.game.ui.ShopRenderer;
import com.alderfall.game.ui.SkillTab;
import com.alderfall.game.ui.VillageOverlayRenderer;
import com.alderfall.game.ui.VillageSidebarRenderer;
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
    private static final double EXPLORE_FREE_SPEED_MULTIPLIER = 1.18;
    private static final int WALK_CYCLE_FRAMES_PER_TILE = 4;
    private static final int WALK_ANIMATION_FRAMES = 18;
    private static final int IDLE_ANIMATION_TRIGGER_FRAMES = 18;
    private static final int IDLE_ANIMATION_FRAME_DIVISOR = 10;
    private static final int MAX_BATTLE_PARTICLES = 22;
    private static final int MAX_STATUS_LOG_ENTRIES = 250;
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
    private static final DateTimeFormatter SAVE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());
    final GameState state;
    private final AssetStore assets;
    private final BattleRenderer battleRenderer;
    private final BattleVfxRenderer battleVfxRenderer;
    final SaveSystem saves;
    private final SaveMenuRenderer saveMenuRenderer;
    private final GameAudioController audio;
    private final WeatherSystem weather;
    private final WorldRenderer worldRenderer;
    private final AmbientMotionLayer ambientMotionLayer = new AmbientMotionLayer();
    private final ReactivePropLayer reactivePropLayer = new ReactivePropLayer();
    private final RoamingWorldEventLayer roamingWorldEventLayer = new RoamingWorldEventLayer();
    private final NpcChoreAnimationLayer npcChoreAnimationLayer = new NpcChoreAnimationLayer();
    private final TerrainFootstepLayer terrainFootstepLayer = new TerrainFootstepLayer();

    private final CameraController cameraController = new CameraController();
    private final RenderMetrics renderMetrics = RenderMetrics.create();
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
    private final WaterTileRenderer waterTileRenderer = new WaterTileRenderer();
    private final TerrainFeatureRenderer terrainFeatureRenderer;
    private final WorldAtmosphereRenderer worldAtmosphereRenderer;
    private final WorldLightingRenderer worldLightingRenderer;
    private final VillageOverlayRenderer villageOverlayRenderer;
    final ShopRenderer shopRenderer;
    final InventoryRenderer inventoryRenderer;
    private final CharacterRenderer characterRenderer;
    private final DialogueRenderer dialogueRenderer;
    private final QuestLogRenderer questLogRenderer;
    private final VillageSidebarRenderer villageSidebarRenderer;
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
    private String freePlayerMapId = WorldMap.OVERWORLD_ID;
    private double freePlayerX = Double.NaN;
    private double freePlayerY = Double.NaN;
    private String followerTrailMapId = WorldMap.OVERWORLD_ID;
    private boolean playerLocomotionActive;
    private int playerLocomotionStartFrame = Integer.MIN_VALUE / 4;
    private int playerLocomotionStopFrame = Integer.MIN_VALUE / 4;
    private int playerIdleStartFrame;
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
    private final List<NpcRender> visibleNpcRenders = new ArrayList<>();
    private final List<WorldProp> nearbyWorldProps = new ArrayList<>();
    private final List<WorldProp> visibleWorldProps = new ArrayList<>();
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
    private boolean battleItemPickerOpen;
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

    private SettingsCategory settingsCategory = SettingsCategory.GRAPHICS;

    private enum SettingsCategory {
        GRAPHICS("Graphics"), COMBAT("Combat"), GAMEPLAY("Gameplay"), AUDIO("Audio"), DEBUG("Debug");

        private final String label;

        SettingsCategory(String label) {
            this.label = label;
        }
    }

    private enum SliderKind {
        CHANCE,
        DIFFICULTY,
        VOLUME,
        ANIMATION,
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

    private record NpcRender(
            String key,
            Npc npc,
            Rectangle screenBounds
    ) {
    }

    private record BubblePlacement(
            int x,
            int y,
            int tailX,
            boolean aboveAnchor,
            int tailHeight
    ) {
        private Rectangle bounds(int width, int height) {
            return new Rectangle(x, y, width, height);
        }

        private BubblePlacement shifted(int dx, int dy) {
            return new BubblePlacement(x + dx, y + dy, tailX + dx, aboveAnchor, tailHeight);
        }
    }

    private record CharacterAnimationSelection(
            String action,
            int frame
    ) {
        private static CharacterAnimationSelection staticPose() {
            return new CharacterAnimationSelection("", -1);
        }

        private boolean animated() {
            return action != null && !action.isBlank() && frame >= 0;
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
        private boolean locomotionActive;
        private int locomotionStartFrame = Integer.MIN_VALUE / 4;
        private int locomotionStopFrame = Integer.MIN_VALUE / 4;
        private int idleStartFrame;
        private final int idlePhaseSeed;

        private FollowerVisualState(String mapId, int x, int y, int facingDx, int facingDy, int frame, int durationFrames,
                                    int idlePhaseSeed) {
            this.mapId = mapId;
            this.fromX = x;
            this.fromY = y;
            this.targetX = x;
            this.targetY = y;
            this.startFrame = frame - Math.max(1, durationFrames);
            this.durationFrames = Math.max(1, durationFrames);
            this.idleStartFrame = frame;
            this.idlePhaseSeed = idlePhaseSeed;
            if (Math.abs(facingDx) + Math.abs(facingDy) == 1) {
                this.facingDx = facingDx;
                this.facingDy = facingDy;
            }
        }
    }

    public GamePanel(Path javaRoot) {
        this.javaRoot = javaRoot;
        this.state = new GameState(GameConfig.loadWithSettings(javaRoot));
        this.assets = new AssetStore(javaRoot.resolve("assets"));
        this.battleRenderer = new BattleRenderer(assets);
        this.battleVfxRenderer = new BattleVfxRenderer(assets,
                new BattleVfxRenderer.Effects() {
                    @Override
                    public int frame() {
                        return GamePanel.this.frame;
                    }

                    @Override
                    public int scaled(int value) {
                        return GamePanel.this.scaled(value);
                    }

                    @Override
                    public int[] battleActorCenter(Battle battle, Actor actor, int panelX, int panelY, int panelW, int panelH) {
                        return GamePanel.this.battleActorCenter(battle, actor, panelX, panelY, panelW, panelH);
                    }
                });
        this.saves = new SaveSystem(javaRoot);
        this.saveMenuRenderer = new SaveMenuRenderer(state, saves,
                new SaveMenuRenderer.Effects() {
                    @Override
                    public int viewWidth() {
                        return GamePanel.this.viewWidth();
                    }

                    @Override
                    public int viewHeight() {
                        return GamePanel.this.viewHeight();
                    }

                    @Override
                    public int centeredX(int width) {
                        return GamePanel.this.centeredX(width);
                    }

                    @Override
                    public int centeredY(int height) {
                        return GamePanel.this.centeredY(height);
                    }

                    @Override
                    public int gameAreaCenteredX(int width) {
                        return GamePanel.this.gameAreaCenteredX(width);
                    }

                    @Override
                    public int frame() {
                        return GamePanel.this.frame;
                    }

                    @Override
                    public boolean importingCharacter() {
                        return GamePanel.this.importingCharacter;
                    }

                    @Override
                    public boolean saveListCurrentCharacterOnly() {
                        return GamePanel.this.saveListCurrentCharacterOnly;
                    }

                    @Override
                    public void setSaveListCurrentCharacterOnly(boolean value) {
                        GamePanel.this.saveListCurrentCharacterOnly = value;
                    }

                    @Override
                    public int saveListScroll() {
                        return GamePanel.this.saveListScroll;
                    }

                    @Override
                    public void setSaveListScroll(int value) {
                        GamePanel.this.saveListScroll = value;
                    }

                    @Override
                    public String selectedLoadCharacterId() {
                        return GamePanel.this.selectedLoadCharacterId;
                    }

                    @Override
                    public void setSelectedLoadCharacterId(String value) {
                        GamePanel.this.selectedLoadCharacterId = value == null ? "" : value;
                    }

                    @Override
                    public String pendingOverwriteSaveName() {
                        return GamePanel.this.pendingOverwriteSaveName;
                    }

                    @Override
                    public void drawOverlayBase(Graphics2D g, int x, int y, int w, int h) {
                        GamePanel.this.drawOverlayBase(g, x, y, w, h);
                    }

                    @Override
                    public void actionButton(Graphics2D g, int x, int y, int w, int h, String label, Runnable action, Color fill, Color border, boolean enabled) {
                        GamePanel.this.actionButton(g, x, y, w, h, label, action, fill, border, enabled);
                    }

                    @Override
                    public void drawCenteredIn(Graphics2D g, String text, int x, int y, int width) {
                        GamePanel.this.drawCenteredIn(g, text, x, y, width);
                    }

                    @Override
                    public void drawClippedString(Graphics2D g, String text, int x, int y, int maxWidth) {
                        GamePanel.this.drawClippedString(g, text, x, y, maxWidth);
                    }

                    @Override
                    public void drawScrollIndicator(Graphics2D g, int x, int y, int h, int totalRows, int scroll, int visibleRows) {
                        GamePanel.this.drawScrollIndicator(g, x, y, h, totalRows, scroll, visibleRows);
                    }

                    @Override
                    public void addButton(Rectangle bounds, String label, Runnable action) {
                        buttons.add(new UiButton(bounds, label, action));
                    }

                    @Override
                    public String nextDefaultSaveName() {
                        return GamePanel.this.nextDefaultSaveName();
                    }

                    @Override
                    public void saveGame() {
                        GamePanel.this.saveGame();
                    }

                    @Override
                    public void requestOverwrite(SaveSystem.SaveSummary summary) {
                        GamePanel.this.requestOverwrite(summary);
                    }

                    @Override
                    public void loadGame(String saveId) {
                        GamePanel.this.loadGame(saveId);
                    }

                    @Override
                    public void loadOrImportGame(String saveId) {
                        GamePanel.this.loadOrImportGame(saveId);
                    }

                    @Override
                    public void closeSaveMenuOrLoadFolder() {
                        GamePanel.this.closeSaveMenuOrLoadFolder();
                    }

                    @Override
                    public boolean hasPendingOverwrite() {
                        return GamePanel.this.hasPendingOverwrite();
                    }

                    @Override
                    public void cancelOverwrite() {
                        GamePanel.this.cancelOverwrite();
                    }

                    @Override
                    public void confirmOverwriteSave() {
                        GamePanel.this.confirmOverwriteSave();
                    }
                });
        MusicManager music = new MusicManager(javaRoot.resolve("assets").resolve("music"));
        SoundManager sounds = new SoundManager(javaRoot.resolve("assets").resolve("sfx"));
        this.audio = new GameAudioController(state, music, sounds);
        this.weather = new WeatherSystem(state);
        this.worldLightingRenderer = new WorldLightingRenderer(state,
                new WorldLightingRenderer.Effects() {
                    @Override
                    public int scaled(int value) {
                        return GamePanel.this.scaled(value);
                    }

                    @Override
                    public int tileRelative(int value, int tileSize) {
                        return GamePanel.this.tileRelative(value, tileSize);
                    }

                    @Override
                    public int gameAreaWidth() {
                        return GamePanel.this.gameAreaWidth();
                    }

                    @Override
                    public int viewHeight() {
                        return GamePanel.this.viewHeight();
                    }

                    @Override
                    public boolean isInteriorAtmosphereMap() {
                        return GamePanel.this.isInteriorAtmosphereMap();
                    }

                    @Override
                    public void drawInteriorLightTints(Graphics2D g, int camX, int camY, int visibleCols, int visibleRows, int tileSize) {
                        GamePanel.this.drawInteriorLightTints(g, camX, camY, visibleCols, visibleRows, tileSize);
                    }

                    @Override
                    public char visibleTerrainTile(char tile, int wx, int wy) {
                        return GamePanel.this.visibleTerrainTile(tile, wx, wy);
                    }

                    @Override
                    public boolean isSettlementMapKind(String mapId) {
                        return GamePanel.this.isSettlementMapKind(mapId);
                    }

                    @Override
                    public int buildingModuleCount(CityBuilding building) {
                        return GamePanel.this.buildingModuleCount(building);
                    }

                    @Override
                    public Rectangle settlementBuildingWorldBounds(String kind, CityBuilding building, int tileSize) {
                        if (GamePanel.this.usesStandaloneSettlementBuilding(kind, building)) {
                            return GamePanel.this.standaloneBuildingVisualLayout(building, 0, 0, tileSize).bounds();
                        }
                        return GamePanel.this.buildingVisualBounds(building, 0, 0, tileSize);
                    }

                    @Override
                    public Rectangle buildingModuleWorldBounds(String kind, CityBuilding building, int index, int modules, int seed,
                                                               int tileSize, int lotX, int lotW, int frontY) {
                        if (GamePanel.this.usesStandaloneSettlementBuilding(kind, building)) {
                            BuildingVisualLayout layout = GamePanel.this.standaloneBuildingVisualLayout(building, 0, 0, tileSize);
                            int moduleW = Math.max(tileSize, layout.drawW() / Math.max(1, modules));
                            return new Rectangle(layout.drawX() + index * moduleW, layout.drawY(), moduleW, layout.drawH());
                        }
                        boolean civic = java.util.List.of("hall", "guild", "barracks", "warehouse").contains(building.style());
                        boolean compactModule = java.util.List.of("guild", "house", "shop", "row").contains(building.style());
                        int minH = civic ? 108 : (compactModule ? 102 : 90);
                        int maxH = civic ? 166 : (compactModule ? 150 : 134);
                        int minW = civic ? 78 : (compactModule ? 72 : 58);
                        int maxW = civic ? 128 : (compactModule ? 122 : 104);
                        int targetH = Math.max(GamePanel.this.tileRelative(minH, tileSize),
                                Math.min(GamePanel.this.tileRelative(maxH, tileSize), building.depth() * tileSize + GamePanel.this.tileRelative(civic ? 50 : 42, tileSize)));
                        int targetW = Math.max(GamePanel.this.tileRelative(minW, tileSize),
                                Math.min(lotW / Math.max(1, modules) + GamePanel.this.tileRelative(civic ? 42 : 34, tileSize), GamePanel.this.tileRelative(maxW, tileSize)));
                        int centerX = lotX + (int) Math.round((index + 0.5) * lotW / modules);
                        int jitter = ((seed >> (index * 4)) & 7) - 3;
                        int drawX = centerX - targetW / 2 + jitter * tileSize / GameConfig.TILE;
                        int drawY = frontY - targetH - GamePanel.this.tileRelative(5, tileSize);
                        return new Rectangle(drawX, drawY, targetW, targetH);
                    }
                });
        this.terrainFeatureRenderer = new TerrainFeatureRenderer(assets, state, weather, waterTileRenderer,
                new TerrainFeatureRenderer.Effects() {
                    @Override
                    public WeatherQuality weatherQuality() {
                        return GamePanel.this.effectiveWeatherQuality();
                    }

                    @Override
                    public String terrainImageName(char tile, int wx, int wy) {
                        return GamePanel.this.terrainImageName(tile, wx, wy);
                    }

                    @Override
                    public void drawShadow(Graphics2D g, int x, int y, int w, int h) {
                        GamePanel.this.drawShadow(g, x, y, w, h);
                    }
                });
        this.worldAtmosphereRenderer = new WorldAtmosphereRenderer(assets, state, weather, renderMetrics,
                new WorldAtmosphereRenderer.Effects() {
                    @Override
                    public WeatherQuality weatherQuality() {
                        return GamePanel.this.effectiveWeatherQuality();
                    }

                    @Override
                    public int scaled(int value) {
                        return GamePanel.this.scaled(value);
                    }

                    @Override
                    public int tileRelative(int value, int tileSize) {
                        return GamePanel.this.tileRelative(value, tileSize);
                    }
                });
        this.shopRenderer = new ShopRenderer(assets, state,
                new ShopRenderer.Effects() {
                    public List<UiButton> buttons() { return GamePanel.this.buttons; }
                    public List<TooltipZone> tooltipZones() { return GamePanel.this.tooltipZones; }
                    @Override
                    public int shopItemScroll() {
                        return GamePanel.this.shopItemScroll;
                    }

                    @Override
                    public void setShopItemScroll(int value) {
                        GamePanel.this.shopItemScroll = value;
                    }

                    @Override
                    public int gameAreaCenteredX(int width) {
                        return GamePanel.this.gameAreaCenteredX(width);
                    }

                    @Override
                    public void drawOverlayBase(Graphics2D g, int x, int y, int w, int h) {
                        GamePanel.this.drawOverlayBase(g, x, y, w, h);
                    }

                    @Override
                    public void actionButton(Graphics2D g, int x, int y, int w, int h, String label, Runnable action, Color fill, Color border, boolean enabled) {
                        GamePanel.this.actionButton(g, x, y, w, h, label, action, fill, border, enabled);
                    }

                    @Override
                    public void drawClippedString(Graphics2D g, String text, int x, int y, int maxWidth) {
                        GamePanel.this.drawClippedString(g, text, x, y, maxWidth);
                    }

                    @Override
                    public void drawScrollIndicator(Graphics2D g, int x, int y, int h, int totalRows, int scroll, int visibleRows) {
                        GamePanel.this.drawScrollIndicator(g, x, y, h, totalRows, scroll, visibleRows);
                    }
                });
        this.inventoryRenderer = new InventoryRenderer(assets, state,
                new InventoryRenderer.Effects() {
                    @Override
                    public int inventoryItemScroll() {
                        return GamePanel.this.inventoryItemScroll;
                    }

                    @Override
                    public void setInventoryItemScroll(int value) {
                        GamePanel.this.inventoryItemScroll = value;
                    }

                    @Override
                    public int craftingRecipeScroll() {
                        return GamePanel.this.craftingRecipeScroll;
                    }

                    @Override
                    public void setCraftingRecipeScroll(int value) {
                        GamePanel.this.craftingRecipeScroll = value;
                    }

                    @Override
                    public int craftingRecipeCategoryIndex() {
                        return GamePanel.this.craftingRecipeCategoryIndex;
                    }

                    @Override
                    public void setCraftingRecipeCategoryIndex(int value) {
                        GamePanel.this.craftingRecipeCategoryIndex = value;
                    }

                    @Override
                    public int gameAreaWidth() {
                        return GamePanel.this.gameAreaWidth();
                    }

                    @Override
                    public int gameAreaCenteredX(int width) {
                        return GamePanel.this.gameAreaCenteredX(width);
                    }

                    @Override
                    public int viewHeight() {
                        return GamePanel.this.viewHeight();
                    }

                    @Override
                    public Point hoverPoint() {
                        return GamePanel.this.hoverPoint;
                    }

                    @Override
                    public InventoryDrag inventoryDrag() {
                        return GamePanel.this.inventoryDrag;
                    }

                    @Override
                    public Point inventoryDragPoint() {
                        return GamePanel.this.inventoryDragPoint;
                    }

                    @Override
                    public List<UiButton> buttons() {
                        return GamePanel.this.buttons;
                    }

                    @Override
                    public List<TooltipZone> tooltipZones() {
                        return GamePanel.this.tooltipZones;
                    }

                    @Override
                    public List<InventoryDragZone> inventoryDragZones() {
                        return GamePanel.this.inventoryDragZones;
                    }

                    @Override
                    public List<InventoryDropZone> inventoryDropZones() {
                        return GamePanel.this.inventoryDropZones;
                    }

                    @Override
                    public void resetPartyScreenScroll() {
                        GamePanel.this.partySkillScroll = 0;
                        GamePanel.this.partyLoadoutScroll = 0;
                    }

                    @Override
                    public void repaintPanel() {
                        GamePanel.this.repaint();
                    }

                    @Override
                    public void drawOverlayBase(Graphics2D g, int x, int y, int w, int h) {
                        GamePanel.this.drawOverlayBase(g, x, y, w, h);
                    }

                    @Override
                    public void actionButton(Graphics2D g, int x, int y, int w, int h, String label, Runnable action, Color fill, Color border, boolean enabled) {
                        GamePanel.this.actionButton(g, x, y, w, h, label, action, fill, border, enabled);
                    }

                    @Override
                    public void drawClippedString(Graphics2D g, String text, int x, int y, int maxWidth) {
                        GamePanel.this.drawClippedString(g, text, x, y, maxWidth);
                    }

                    @Override
                    public void drawCenteredIn(Graphics2D g, String text, int x, int y, int width) {
                        GamePanel.this.drawCenteredIn(g, text, x, y, width);
                    }

                    @Override
                    public void drawScrollIndicator(Graphics2D g, int x, int y, int h, int totalRows, int scroll, int visibleRows) {
                        GamePanel.this.drawScrollIndicator(g, x, y, h, totalRows, scroll, visibleRows);
                    }

                    @Override
                    public Color blend(Color a, Color b, double t) {
                        return GamePanel.this.blend(a, b, t);
                    }
                });
        this.characterRenderer = new CharacterRenderer(assets, state,
                new CharacterRenderer.Effects() {
                    @Override
                    public int gameAreaWidth() {
                        return GamePanel.this.gameAreaWidth();
                    }

                    @Override
                    public int viewHeight() {
                        return GamePanel.this.viewHeight();
                    }

                    @Override
                    public SkillTab activeSkillTab() {
                        return GamePanel.this.activeSkillTab;
                    }

                    @Override
                    public void setActiveSkillTab(SkillTab tab) {
                        GamePanel.this.activeSkillTab = tab;
                    }

                    @Override
                    public String activeProfessionId() {
                        return GamePanel.this.activeProfessionId;
                    }

                    @Override
                    public void setActiveProfessionId(String id) {
                        GamePanel.this.activeProfessionId = id;
                    }

                    @Override
                    public int skillTreeScroll() {
                        return GamePanel.this.skillTreeScroll;
                    }

                    @Override
                    public void setSkillTreeScroll(int value) {
                        GamePanel.this.skillTreeScroll = value;
                    }

                    @Override
                    public int partySkillScroll() {
                        return GamePanel.this.partySkillScroll;
                    }

                    @Override
                    public void setPartySkillScroll(int value) {
                        GamePanel.this.partySkillScroll = value;
                    }

                    @Override
                    public int partyLoadoutScroll() {
                        return GamePanel.this.partyLoadoutScroll;
                    }

                    @Override
                    public void setPartyLoadoutScroll(int value) {
                        GamePanel.this.partyLoadoutScroll = value;
                    }

                    @Override
                    public void setPartyLoadoutBounds(Rectangle bounds) {
                        GamePanel.this.partyLoadoutBounds = bounds;
                    }

                    @Override
                    public boolean abilityDragActive() {
                        return GamePanel.this.abilityDrag != null;
                    }

                    @Override
                    public Point abilityDragPoint() {
                        return GamePanel.this.abilityDragPoint;
                    }

                    @Override
                    public String abilityDragName() {
                        return GamePanel.this.abilityDrag == null ? "" : GamePanel.this.abilityDrag.abilityName();
                    }

                    @Override
                    public void addAbilityDragZone(Rectangle bounds, String abilityName, int sourceSlot) {
                        abilityDragZones.add(new AbilityDragZone(bounds, abilityName, sourceSlot));
                    }

                    @Override
                    public void addAbilityDropZone(Rectangle bounds, int slot, boolean remove) {
                        abilityDropZones.add(new AbilityDropZone(bounds, slot, remove));
                    }

                    @Override
                    public void addPartyPortraitZone(Rectangle bounds, Actor actor) {
                        partyPortraitZones.add(new PartyPortraitZone(bounds, actor));
                    }

                    @Override
                    public void addButton(Rectangle bounds, String label, Runnable action) {
                        buttons.add(new UiButton(bounds, label, action));
                    }

                    @Override
                    public void addTooltip(Rectangle bounds, String title, String body) {
                        tooltipZones.add(new TooltipZone(bounds, title, body));
                    }

                    @Override
                    public void addTooltip(Rectangle bounds, String title, String body, String icon) {
                        tooltipZones.add(new TooltipZone(bounds, title, body, icon));
                    }

                    @Override
                    public void addTooltip(Rectangle bounds, String title, String body, String icon, Color accent) {
                        tooltipZones.add(new TooltipZone(bounds, title, body, icon, accent));
                    }

                    @Override
                    public void repaintPanel() {
                        GamePanel.this.repaint();
                    }

                    @Override
                    public void drawOverlayBase(Graphics2D g, int x, int y, int w, int h) {
                        GamePanel.this.drawOverlayBase(g, x, y, w, h);
                    }

                    @Override
                    public void actionButton(Graphics2D g, int x, int y, int w, int h, String label, Runnable action, Color fill, Color border, boolean enabled) {
                        GamePanel.this.actionButton(g, x, y, w, h, label, action, fill, border, enabled);
                    }

                    @Override
                    public void drawClippedString(Graphics2D g, String text, int x, int y, int maxWidth) {
                        GamePanel.this.drawClippedString(g, text, x, y, maxWidth);
                    }

                    @Override
                    public void drawCenteredIn(Graphics2D g, String text, int x, int y, int width) {
                        GamePanel.this.drawCenteredIn(g, text, x, y, width);
                    }

                    @Override
                    public void drawScrollIndicator(Graphics2D g, int x, int y, int h, int totalRows, int scroll, int visibleRows) {
                        GamePanel.this.drawScrollIndicator(g, x, y, h, totalRows, scroll, visibleRows);
                    }

                    @Override
                    public String abilityIconName(Ability ability) {
                        return GamePanel.this.abilityIconName(ability);
                    }

                    @Override
                    public String scalingLabel(Ability ability) {
                        return GamePanel.this.scalingLabel(ability);
                    }

                    @Override
                    public String abilityStatusLabel(AbilityStatus status) {
                        return GamePanel.this.abilityStatusLabel(status);
                    }

                    @Override
                    public String abilityStatusText(List<AbilityStatus> statuses) {
                        return GamePanel.this.abilityStatusText(statuses);
                    }

                    @Override
                    public String abilityMechanicsText(Ability ability) {
                        return GamePanel.this.abilityMechanicsText(ability);
                    }

                    @Override
                    public String abilityFlavor(Ability ability) {
                        return GamePanel.this.abilityFlavor(ability);
                    }

                    @Override
                    public String dialoguePortraitSprite(Actor actor) {
                        return GamePanel.this.dialoguePortraitSprite(actor);
                    }
                });
        this.dialogueRenderer = new DialogueRenderer(assets, state,
                new DialogueRenderer.Effects() {
                    @Override
                    public int gameAreaWidth() {
                        return GamePanel.this.gameAreaWidth();
                    }

                    @Override
                    public int gameAreaCenteredX(int width) {
                        return GamePanel.this.gameAreaCenteredX(width);
                    }

                    @Override
                    public int viewHeight() {
                        return GamePanel.this.viewHeight();
                    }

                    @Override
                    public int dialogOptionScroll() {
                        return GamePanel.this.dialogOptionScroll;
                    }

                    @Override
                    public void setDialogOptionScroll(int value) {
                        GamePanel.this.dialogOptionScroll = value;
                    }

                    @Override
                    public Rectangle dialogOptionScrollBounds() {
                        return GamePanel.this.dialogOptionScrollBounds;
                    }

                    @Override
                    public void setDialogOptionScrollBounds(Rectangle bounds) {
                        GamePanel.this.dialogOptionScrollBounds = bounds;
                    }

                    @Override
                    public String dialogOptionScrollKey() {
                        return GamePanel.this.dialogOptionScrollKey;
                    }

                    @Override
                    public void setDialogOptionScrollKey(String key) {
                        GamePanel.this.dialogOptionScrollKey = key == null ? "" : key;
                    }

                    @Override
                    public void drawOverlayBase(Graphics2D g, int x, int y, int w, int h) {
                        GamePanel.this.drawOverlayBase(g, x, y, w, h);
                    }

                    @Override
                    public void drawWrapped(Graphics2D g, String text, int x, int y, int width, int lineHeight, int maxLines) {
                        GamePanel.this.drawWrapped(g, text, x, y, width, lineHeight, maxLines);
                    }

                    @Override
                    public void drawClippedString(Graphics2D g, String text, int x, int y, int maxWidth) {
                        GamePanel.this.drawClippedString(g, text, x, y, maxWidth);
                    }

                    @Override
                    public void addTooltip(Rectangle bounds, String title, String body) {
                        tooltipZones.add(new TooltipZone(bounds, title, body));
                    }

                    @Override
                    public void addTooltip(Rectangle bounds, String title, String body, String icon, Color accent) {
                        tooltipZones.add(new TooltipZone(bounds, title, body, icon, accent));
                    }

                    @Override
                    public void addButton(Rectangle bounds, String label, Runnable action) {
                        buttons.add(new UiButton(bounds, label, action));
                    }

                    @Override
                    public void repaintPanel() {
                        GamePanel.this.repaint();
                    }

                    @Override
                    public Point hoverPoint() {
                        return GamePanel.this.hoverPoint;
                    }

                    @Override
                    public int clamp(int value, int min, int max) {
                        return GamePanel.this.clamp(value, min, max);
                    }

                    @Override
                    public void drawNpcPortraitCard(Graphics2D g, Npc npc, int x, int y, int w, int h) {
                        GamePanel.this.drawNpcPortraitCard(g, npc, x, y, w, h);
                    }
                });
        this.questLogRenderer = new QuestLogRenderer(assets, state,
                new QuestLogRenderer.Effects() {
                    @Override
                    public int gameAreaWidth() {
                        return GamePanel.this.gameAreaWidth();
                    }

                    @Override
                    public int gameAreaCenteredX(int width) {
                        return GamePanel.this.gameAreaCenteredX(width);
                    }

                    @Override
                    public int viewHeight() {
                        return GamePanel.this.viewHeight();
                    }

                    @Override
                    public boolean questLogCompletedTab() {
                        return GamePanel.this.questLogCompletedTab;
                    }

                    @Override
                    public void setQuestLogCompletedTab(boolean value) {
                        GamePanel.this.questLogCompletedTab = value;
                    }

                    @Override
                    public int questLogScroll() {
                        return GamePanel.this.questLogScroll;
                    }

                    @Override
                    public void setQuestLogScroll(int value) {
                        GamePanel.this.questLogScroll = value;
                    }

                    @Override
                    public String selectedQuestLogId() {
                        return GamePanel.this.selectedQuestLogId;
                    }

                    @Override
                    public void setSelectedQuestLogId(String value) {
                        GamePanel.this.selectedQuestLogId = value == null ? "" : value;
                    }

                    @Override
                    public boolean questDetailTimelineTab() {
                        return GamePanel.this.questDetailTimelineTab;
                    }

                    @Override
                    public void setQuestDetailTimelineTab(boolean value) {
                        GamePanel.this.questDetailTimelineTab = value;
                    }

                    @Override
                    public String trackedQuestId() {
                        return GamePanel.this.trackedQuestId;
                    }

                    @Override
                    public void setTrackedQuestId(String value) {
                        GamePanel.this.trackedQuestId = value == null ? "" : value;
                    }

                    @Override
                    public Point hoverPoint() {
                        return GamePanel.this.hoverPoint;
                    }

                    @Override
                    public void drawOverlayBase(Graphics2D g, int x, int y, int w, int h) {
                        GamePanel.this.drawOverlayBase(g, x, y, w, h);
                    }

                    @Override
                    public void actionButton(Graphics2D g, int x, int y, int w, int h, String label, Runnable action, Color fill, Color border, boolean enabled) {
                        GamePanel.this.actionButton(g, x, y, w, h, label, action, fill, border, enabled);
                    }

                    @Override
                    public void drawWrapped(Graphics2D g, String text, int x, int y, int width, int lineHeight, int maxLines) {
                        GamePanel.this.drawWrapped(g, text, x, y, width, lineHeight, maxLines);
                    }

                    @Override
                    public void drawClippedString(Graphics2D g, String text, int x, int y, int maxWidth) {
                        GamePanel.this.drawClippedString(g, text, x, y, maxWidth);
                    }

                    @Override
                    public void drawCenteredIn(Graphics2D g, String text, int x, int y, int width) {
                        GamePanel.this.drawCenteredIn(g, text, x, y, width);
                    }

                    @Override
                    public void drawScrollIndicator(Graphics2D g, int x, int y, int h, int totalRows, int scroll, int visibleRows) {
                        GamePanel.this.drawScrollIndicator(g, x, y, h, totalRows, scroll, visibleRows);
                    }

                    @Override
                    public void addButton(Rectangle bounds, String label, Runnable action) {
                        buttons.add(new UiButton(bounds, label, action));
                    }

                    @Override
                    public void addTooltip(Rectangle bounds, String title, String body, String icon, Color accent) {
                        tooltipZones.add(new TooltipZone(bounds, title, body, icon, accent));
                    }

                    @Override
                    public void repaintPanel() {
                        GamePanel.this.repaint();
                    }

                    @Override
                    public int clamp(int value, int min, int max) {
                        return GamePanel.this.clamp(value, min, max);
                    }

                    @Override
                    public Color blend(Color first, Color second, double amount) {
                        return GamePanel.this.blend(first, second, amount);
                    }

                    @Override
                    public void addPartyPortraitZone(Rectangle bounds, Actor actor) {
                        partyPortraitZones.add(new PartyPortraitZone(bounds, actor));
                    }

                    @Override
                    public String abilityIconName(Ability ability) {
                        return GamePanel.this.abilityIconName(ability);
                    }
                });
        this.villageOverlayRenderer = new VillageOverlayRenderer(assets, state,
                new VillageOverlayRenderer.Effects() {
                    @Override
                    public int gameAreaWidth() {
                        return GamePanel.this.gameAreaWidth();
                    }

                    @Override
                    public int viewHeight() {
                        return GamePanel.this.viewHeight();
                    }

                    @Override
                    public int villagePanelTop() {
                        return GamePanel.this.villagePanelTop();
                    }

                    @Override
                    public int gameAreaCenteredX(int width) {
                        return GamePanel.this.gameAreaCenteredX(width);
                    }

                    @Override
                    public int centeredY(int height) {
                        return GamePanel.this.centeredY(height);
                    }

                    @Override
                    public void drawOverlayBase(Graphics2D g, int x, int y, int w, int h) {
                        GamePanel.this.drawOverlayBase(g, x, y, w, h);
                    }

                    @Override
                    public void actionButton(Graphics2D g, int x, int y, int w, int h, String label, Runnable action, Color fill, Color border, boolean enabled) {
                        GamePanel.this.actionButton(g, x, y, w, h, label, action, fill, border, enabled);
                    }

                    @Override
                    public void drawClippedString(Graphics2D g, String text, int x, int y, int maxWidth) {
                        GamePanel.this.drawClippedString(g, text, x, y, maxWidth);
                    }

                    @Override
                    public void drawWrapped(Graphics2D g, String text, int x, int y, int width, int lineHeight, int maxLines) {
                        GamePanel.this.drawWrapped(g, text, x, y, width, lineHeight, maxLines);
                    }

                    @Override
                    public void drawCenteredIn(Graphics2D g, String text, int x, int y, int width) {
                        GamePanel.this.drawCenteredIn(g, text, x, y, width);
                    }

                    @Override
                    public void addButton(Rectangle bounds, String label, Runnable action) {
                        buttons.add(new UiButton(bounds, label, action));
                    }

                    @Override
                    public void addTooltip(Rectangle bounds, String title, String body) {
                        tooltipZones.add(new TooltipZone(bounds, title, body));
                    }

                    @Override
                    public String professionSummary(Actor actor) {
                        return GamePanel.this.professionSummary(actor);
                    }

                    @Override
                    public String shortText(String text, int maxChars) {
                        return GamePanel.this.shortText(text, maxChars);
                    }
                });
        this.villageSidebarRenderer = new VillageSidebarRenderer(assets, state,
                new VillageSidebarRenderer.Effects() {
                    @Override
                    public int viewHeight() {
                        return GamePanel.this.viewHeight();
                    }

                    @Override
                    public void actionButton(Graphics2D g, int x, int y, int w, int h, String label, Runnable action, Color fill, Color border, boolean enabled) {
                        GamePanel.this.actionButton(g, x, y, w, h, label, action, fill, border, enabled);
                    }

                    @Override
                    public void drawClippedString(Graphics2D g, String text, int x, int y, int maxWidth) {
                        GamePanel.this.drawClippedString(g, text, x, y, maxWidth);
                    }

                    @Override
                    public void drawWrapped(Graphics2D g, String text, int x, int y, int width, int lineHeight, int maxLines) {
                        GamePanel.this.drawWrapped(g, text, x, y, width, lineHeight, maxLines);
                    }

                    @Override
                    public void drawCenteredIn(Graphics2D g, String text, int x, int y, int width) {
                        GamePanel.this.drawCenteredIn(g, text, x, y, width);
                    }

                    @Override
                    public void drawScrollIndicator(Graphics2D g, int x, int y, int h, int totalRows, int scroll, int visibleRows) {
                        GamePanel.this.drawScrollIndicator(g, x, y, h, totalRows, scroll, visibleRows);
                    }

                    @Override
                    public String buildingPreviewSprite(VillageManager.BuildingPlan plan) {
                        return GamePanel.this.buildingPreviewSprite(plan);
                    }

                    @Override
                    public String villageCostDisplay(VillageManager.VillageCost cost) {
                        return GamePanel.this.villageCostDisplay(cost);
                    }

                    @Override
                    public void exportMapEditorDesign() {
                        GamePanel.this.exportMapEditorDesign();
                    }
                });
        WorldRenderer.TerrainPainter terrainPainter = new WorldRenderer.TerrainPainter() {
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
        };
        WorldPropRenderer propRenderer = new WorldPropRenderer(assets, state, new WorldPropRenderer.Effects() {
            @Override
            public void drawShadow(Graphics2D g, int x, int y, int w, int h) {
                GamePanel.this.drawShadow(g, x, y, w, h);
            }

            @Override
            public void drawCasterShadow(Graphics2D g, BufferedImage image, String cacheKey,
                                         int x, int y, int width, int height, boolean flipHorizontal, float baseAlpha) {
                GamePanel.this.drawCasterShadow(g, image, cacheKey, x, y, width, height, flipHorizontal, baseAlpha);
            }

            @Override
            public void drawRadialGlow(Graphics2D g, int cx, int cy, int radius, Color color, float alpha) {
                GamePanel.this.drawRadialGlow(g, cx, cy, radius, color, alpha);
            }

            @Override
            public char visibleTerrainTile(char tile, int wx, int wy) {
                return GamePanel.this.visibleTerrainTile(tile, wx, wy);
            }

            @Override
            public Color propGlowColor(String asset) {
                return GamePanel.this.propGlowColor(asset);
            }

            @Override
            public float lightVisibilityForAsset(String asset) {
                return GamePanel.this.lightVisibilityForAsset(asset);
            }

            @Override
            public float nightFactor() {
                return GamePanel.this.nightFactor();
            }

            @Override
            public boolean isSettlementMapKind(String mapId) {
                return GamePanel.this.isSettlementMapKind(mapId);
            }
        });
        this.worldRenderer = new WorldRenderer(assets, terrainPainter, propRenderer, new WorldRenderer.LightingPainter() {
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
            long updateStarted = renderMetrics.start();
            frame++;
            state.tickWorld();
            ambientMotionLayer.tick(state);
            roamingWorldEventLayer.tick(state);
            weather.tickTransition();
            if (state.mode == GameMode.DEFENSE && state.defenseRaid != null) {
                tickDefensePlayerMovement();
                state.tickDefenseRaid();
            } else {
                tickPlayerPath();
                tickExploreFreeMovement();
            }
            if (state.mode == GameMode.BATTLE && state.battle != null) {
                state.tickBattle();
            }
            audio.update(frame);
            syncStatusLog();
            renderMetrics.record("update", updateStarted);
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

    void interactWithRoamingWorldEvent() {
        if (roamingWorldEventLayer.interact(state)) {
            return;
        }
        state.interact();
    }

    public void setFullscreenToggle(Runnable fullscreenToggle) {
        this.fullscreenToggle = fullscreenToggle == null ? () -> {
        } : fullscreenToggle;
    }

    void toggleBattleItemPicker() {
        if (state.mode == GameMode.BATTLE && state.battle != null && !state.battle.finished) {
            battleItemPickerOpen = !battleItemPickerOpen;
        }
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
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, renderQuality().qualityRendering
                ? RenderingHints.VALUE_INTERPOLATION_BILINEAR
                : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        int scaledWidth = (int) Math.round(viewWidth() * renderScaleX);
        int scaledHeight = (int) Math.round(viewHeight() * renderScaleY);
        backBuffer.drawTo(g, renderOffsetX, renderOffsetY, scaledWidth, scaledHeight);
        g.dispose();
        renderMetrics.endFrame(frame);
    }

    private void renderGame(Graphics2D g) {
        applyRenderQualityHints(g);
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
                if (!uiHidden && visibleMode == GameMode.DEFENSE) {
                    drawDefenseRaidHud(g);
                }
                if (!uiHidden && visibleMode == GameMode.DIALOG) {
                    drawDialog(g);
                } else if (!uiHidden && visibleMode == GameMode.QUEST_LOG) {
                    drawQuestLog(g);
                } else if (!uiHidden && visibleMode == GameMode.SKILLS) {
                    drawPartyOverview(g);
                } else if (!uiHidden && visibleMode == GameMode.CHEST) {
                    inventoryRenderer.drawChest(g);
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
        if (!uiHidden && visibleGameplayMode() != GameMode.DIALOG && !battleIntroBlocksTooltips()) {
            drawHoverTooltip(g);
        }
        if (state.dialogueVideoActive()) {
            drawDialogueVideoOverlay(g);
        }
    }

    private boolean battleIntroBlocksTooltips() {
        return visibleGameplayMode() == GameMode.BATTLE
                && state.battle != null
                && state.battle.introActive();
    }

    private boolean skipBattleIntroOnClick() {
        if (!battleIntroBlocksTooltips()) {
            return false;
        }
        state.battle.skipIntro();
        pressedButtonBounds = null;
        pressedButtonLabel = null;
        suppressNextClick = true;
        repaint();
        return true;
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
        terrainFeatureRenderer.useContext(new TerrainFeatureRenderer.RenderContext(tileSize, visibleCols, visibleRows, renderAnimationFrame()));
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
                state.zoom,
                frame
        );
        long propsStarted = renderMetrics.start();
        worldRenderer.rebuildNearbyProps(propContext, nearbyWorldProps);
        reactivePropLayer.tick(state, nearbyWorldProps);
        renderMetrics.record("world.propsInBounds", propsStarted);
        renderMetrics.sample("world.nearbyProps", nearbyWorldProps.size());

        Graphics2D worldGraphics = (Graphics2D) g.create();
        worldGraphics.setClip(0, 0, gameAreaWidth(), worldViewportHeight);
        worldGraphics.translate(-cameraOffsetX, -cameraOffsetY);
        WorldRenderer.TerrainContext terrainContext = new WorldRenderer.TerrainContext(
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
        );
        long terrainStarted = renderMetrics.start();
        worldRenderer.drawCachedTerrainBase(worldGraphics, terrainContext);
        renderMetrics.record("world.terrain", terrainStarted);
        long terrainAnimationsStarted = renderMetrics.start();
        worldRenderer.drawTerrainAnimations(worldGraphics, terrainContext);
        renderMetrics.record("world.terrainAnimations", terrainAnimationsStarted);
        if (renderQuality().ambientWorldEffects) {
            ambientMotionLayer.draw(worldGraphics, propContext);
        }

        rebuildWorldLights(camX, camY, visibleCols, visibleRows, tileSize);
        renderMetrics.sample("world.activeLights", activeWorldLightCount());
        setShadowWorldOffset(camX * (double) tileSize, camY * (double) tileSize);
        long sceneryStarted = renderMetrics.start();
        drawFieldConnectors(worldGraphics, camX, camY);
        worldRenderer.drawGroundPropOverlays(worldGraphics, propContext, nearbyWorldProps);
        drawMountainMassifOverlays(worldGraphics, camX, camY);
        drawRoadConnectors(worldGraphics, camX, camY);
        drawSettlementSurfaceSeams(worldGraphics, camX, camY);
        drawCityBuildingEntities(worldGraphics, camX, camY);
        drawVillageBuildingActionHover(worldGraphics, camX, camY, tileSize);
        drawVillageBuildingPlacementPreview(worldGraphics, camX, camY, tileSize);
        drawSettlementOverlays(worldGraphics, camX, camY);
        renderMetrics.record("world.scenery", sceneryStarted);

        long propDrawStarted = renderMetrics.start();
        worldRenderer.drawVisibleProps(worldGraphics, propContext, nearbyWorldProps, visibleWorldProps);
        renderMetrics.record("world.props", propDrawStarted);
        renderMetrics.sample("world.visibleProps", visibleWorldProps.size());
        roamingWorldEventLayer.draw(worldGraphics, propContext);
        reactivePropLayer.draw(worldGraphics, propContext, visibleWorldProps);
        worldRenderer.drawFallingGatheredProp(worldGraphics, propContext);
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
        visibleNpcRenders.clear();
        if (state.mode != GameMode.DEFENSE) {
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
                String choreAction = npcChoreAnimationLayer.actionFor(state, npc, moving, assets);
                if (choreAction.isBlank()) {
                    drawCharacterSprite(worldGraphics, npcWorldSprite, npcX, npcY, npcW, npcH, motion.facingDx(), motion.facingDy(), step);
                } else {
                    drawCharacterActionSprite(worldGraphics, npcWorldSprite, choreAction, npcX, npcY, npcW, npcH);
                }
                drawNpcGatherToolSwing(worldGraphics, npc, motion, camX, camY, tileSize, px, py, moving);
                Rectangle npcWorldBounds = new Rectangle(npcX, npcY, npcW, npcH);
                Rectangle npcScreenBounds = new Rectangle(
                        (int) Math.round(npcX - cameraOffsetX),
                        (int) Math.round(npcY - cameraOffsetY),
                        npcW,
                        npcH
                );
                visibleNpcRenders.add(new NpcRender(state.npcRenderKey(npc), npc, npcScreenBounds));
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
        }
        renderMetrics.sample("world.visibleNpcs", visibleNpcCount);

        if (state.mode != GameMode.DEFENSE) {
            drawPartyFollowers(worldGraphics, camX, camY, tileSize, visibleCols, visibleRows, cameraOffsetX, cameraOffsetY);
        }
        drawDefenseRaidEntities(worldGraphics, camX, camY, tileSize, visibleCols, visibleRows);

        double playerX = renderPlayerX();
        double playerY = renderPlayerY();
        int playerPx = (int) Math.round((playerX - camX) * tileSize);
        int playerPy = (int) Math.round((playerY - camY) * tileSize);
        CharacterWorldScale playerScale = characterWorldScale(state.player.worldSprite, true);
        int playerW = tileRelative(playerScale.width(), tileSize);
        int playerH = tileRelative(playerScale.height(), tileSize);
        int[] heldDirection = heldMoveDirection();
        boolean defenseMoving = state.mode == GameMode.DEFENSE && state.defenseRaid != null
                && state.defenseRaid.started()
                && (heldDirection[0] != 0 || heldDirection[1] != 0);
        boolean freeMoving = exploreFreeMoving();
        boolean pathMoving = explorePathMoving();
        boolean playerTileMoving = playerMoving();
        boolean locomotionActive = defenseMoving || playerTileMoving || freeMoving || pathMoving;
        updatePlayerAnimationState(locomotionActive);
        CharacterAnimationSelection playerAnimation = playerAnimationSelection(
                state.player.worldSprite,
                playerW,
                playerH,
                defenseMoving,
                playerTileMoving,
                freeMoving || pathMoving
        );
        int bob = playerTileMoving
                ? -(int) Math.round(Math.sin(moveProgress(frame - playerMoveStartFrame, playerMoveFrames()) * Math.PI) * tileRelative(2, tileSize))
                : freeMoving || pathMoving
                ? -(int) Math.round(Math.sin(frame * 0.42) * tileRelative(2, tileSize))
                : 0;
        int playerXpx = playerPx + (tileSize - playerW) / 2;
        int playerYpx = playerPy + tileSize - playerH - tileRelative(playerScale.footLift(), tileSize) + bob;
        terrainFootstepLayer.tick(state, playerX, playerY, locomotionActive, playerFacingDx, playerFacingDy);
        terrainFootstepLayer.draw(worldGraphics, propContext);
        drawPlayerTravelEffects(worldGraphics, playerPx, playerPy);
        int shadowPulse = playerTileMoving
                ? (int) Math.round(Math.sin(moveProgress(frame - playerMoveStartFrame, playerMoveFrames()) * Math.PI) * tileRelative(2, tileSize))
                : freeMoving || pathMoving
                ? (int) Math.round(Math.sin(frame * 0.42) * tileRelative(2, tileSize))
                : 0;
        drawShadow(worldGraphics, playerPx + tileRelative(24 - playerScale.shadowWidth() / 2, tileSize) - shadowPulse / 2, playerPy + tileRelative(40, tileSize), tileRelative(playerScale.shadowWidth(), tileSize) + shadowPulse, tileRelative(10, tileSize));
        drawCharacterSprite(worldGraphics, state.player.worldSprite, playerXpx, playerYpx, playerW, playerH,
                playerFacingDx, playerFacingDy, playerAnimation);
        drawGatherToolSwing(worldGraphics, camX, camY, tileSize, playerPx, playerPy);
        drawNearbyQuestPrompt(worldGraphics, camX, camY);
        if (weather.effectsVisibleOnCurrentMap()) {
            drawCloudLayer(worldGraphics, camX, camY);
        }
        long lightingStarted = renderMetrics.start();
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
        // The vignette belongs to the viewport, so camera movement must not resample it.
        worldRenderer.drawVignette(g, gameAreaWidth(), viewHeight());
        renderMetrics.record("world.lighting", lightingStarted);
        long atmosphereStarted = renderMetrics.start();
        drawWorldAtmosphere(g);
        renderMetrics.record("atmosphere", atmosphereStarted);
        drawEmissiveWorldLights(g, camX, camY, tileSize, cameraOffsetX, cameraOffsetY);
        drawAmbientTownConversationBubbles(g);
        drawTravelBanterPrompt(g, camX, camY, tileSize, cameraOffsetX, cameraOffsetY);
        clearWorldLights();
        renderMetrics.record("world", worldStarted);
    }

    private void drawDefenseRaidEntities(Graphics2D g, int camX, int camY, int tileSize, int visibleCols, int visibleRows) {
        CampDefenseMinigame raid = state.defenseRaid;
        if (raid == null || !raid.mapId().equals(state.currentMapId)) {
            return;
        }
        for (CampDefenseMinigame.AttackPulse pulse : raid.attackPulses()) {
            float alpha = (float) (1.0 - pulse.progress());
            Graphics2D beam = (Graphics2D) g.create();
            beam.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.08f, alpha * 0.78f)));
            beam.setColor(pulse.color());
            int x1 = (int) Math.round((pulse.fromX() - camX) * tileSize);
            int y1 = (int) Math.round((pulse.fromY() - camY) * tileSize);
            int effectX = x1;
            int effectY = y1;
            if (pulse.radius() > 0.0) {
                int radius = (int) Math.round(pulse.radius() * tileSize * (0.35 + pulse.progress() * 0.75));
                beam.setStroke(new BasicStroke(Math.max(2f, scaledStroke(3.4f)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                beam.drawOval(x1 - radius, y1 - radius, radius * 2, radius * 2);
                beam.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.04f, alpha * 0.18f)));
                beam.fillOval(x1 - radius, y1 - radius, radius * 2, radius * 2);
            } else {
                beam.setStroke(new BasicStroke(Math.max(2f, scaledStroke(3.2f)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int x2 = (int) Math.round((pulse.toX() - camX) * tileSize);
                int y2 = (int) Math.round((pulse.toY() - camY) * tileSize);
                beam.drawLine(x1, y1, x2, y2);
                double travel = smoothStep(pulse.progress());
                effectX = (int) Math.round(x1 + (x2 - x1) * travel);
                effectY = (int) Math.round(y1 + (y2 - y1) * travel);
            }
            String effectSprite = effectSpriteName(pulse.effectKey());
            if (effectSprite != null && (assets.hasSprite(effectSprite) || assets.effectSpriteFrameCount(effectSprite) > 1)) {
                int effectSize = tileRelative(pulse.radius() > 0.0 ? 96 : 62, tileSize);
                beam.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.12f, alpha * 0.84f)));
                beam.drawImage(assets.effectSprite(effectSprite, effectSize, effectSize, frame),
                        effectX - effectSize / 2, effectY - effectSize / 2, null);
            } else if (pulse.radius() <= 0.0) {
                int spark = Math.max(5, scaled(8));
                beam.fillOval(effectX - spark / 2, effectY - spark / 2, spark, spark);
            }
            beam.dispose();
        }
        for (CampDefenseMinigame.RaidEnemy enemy : raid.enemies()) {
            double ex = enemy.x();
            double ey = enemy.y();
            if (ex < camX - 1 || ey < camY - 1 || ex >= camX + visibleCols + 1 || ey >= camY + visibleRows + 1) {
                continue;
            }
            int tilePx = (int) Math.round((ex - 0.5 - camX) * tileSize);
            int tilePy = (int) Math.round((ey - 0.5 - camY) * tileSize);
            int size = tileRelative(enemy.brute() ? 74 : 58, tileSize);
            int x = tilePx + (tileSize - size) / 2;
            int y = tilePy + tileSize - size - tileRelative(enemy.brute() ? 8 : 5, tileSize);
            drawShadow(g, tilePx + tileRelative(6, tileSize), tilePy + tileRelative(38, tileSize), tileRelative(enemy.brute() ? 52 : 40, tileSize), tileRelative(10, tileSize));
            g.drawImage(assets.spriteFit(enemy.spec().sprite(), size, size), x, y, null);
            int barW = Math.max(22, tileRelative(enemy.brute() ? 56 : 42, tileSize));
            int barH = Math.max(3, scaled(4));
            int barX = tilePx + tileSize / 2 - barW / 2;
            int barY = y - scaled(8);
            g.setColor(new Color(18, 18, 22, 185));
            g.fillRoundRect(barX, barY, barW, barH, barH, barH);
            g.setColor(enemy.brute() ? new Color(233, 91, 76) : new Color(238, 177, 93));
            g.fillRoundRect(barX, barY, (int) Math.round(barW * (enemy.hp() / (double) Math.max(1, enemy.maxHp()))), barH, barH, barH);
        }
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
            updateFollowerAnimationState(visual, moving);
            int facingDx = visual.facingDx;
            int facingDy = visual.facingDy;
            CharacterAnimationSelection followerAnimation = followerAnimationSelection(
                    follower.worldSprite,
                    followerW,
                    followerH,
                    facingDx,
                    facingDy,
                    visual
            );
            drawShadow(g, px + tileRelative(24 - scale.shadowWidth() / 2, tileSize), py + tileRelative(40, tileSize),
                    tileRelative(scale.shadowWidth(), tileSize), tileRelative(9, tileSize));
            drawCharacterSprite(g, follower.worldSprite, drawX, drawY, followerW, followerH, facingDx, facingDy,
                    followerAnimation);
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
            visual = new FollowerVisualState(state.currentMapId, trail.x(), trail.y(), facingDx, facingDy, frame,
                    trail.durationFrames(), Math.floorMod(key.hashCode(), 97));
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
        double progress = moveProgress(frame - visual.startFrame, visual.durationFrames);
        int animationStep = (int) Math.floor((visual.walkTileStart + progress) * animationFrames);
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
        Rectangle anchor = partyFollowerBounds(prompt.speaker());
        int width = Math.min(scaled(380), gameAreaWidth() - scaled(24));
        int lineHeight = scaled(17);
        int optionH = scaled(31);
        int optionGap = scaled(6);
        int tailH = scaled(14);
        int height = scaled(58) + prompt.options().size() * optionH + Math.max(0, prompt.options().size() - 1) * optionGap;
        BubblePlacement placement = bubblePlacement(anchor, width, height, tailH);

        Graphics2D bubble = (Graphics2D) g.create();
        Color fill = new Color(8, 12, 20, 186);
        Color border = new Color(122, 169, 218, 178);
        bubble.setColor(fill);
        bubble.fillRoundRect(placement.x(), placement.y(), width, height, scaled(10), scaled(10));
        drawSpeechBubbleTail(bubble, placement, height, fill);
        bubble.setColor(border);
        bubble.setStroke(new BasicStroke(1.2f));
        bubble.drawRoundRect(placement.x(), placement.y(), width, height, scaled(10), scaled(10));
        drawSpeechBubbleTailOutline(bubble, placement, height, border);
        bubble.setFont(new Font("SansSerif", Font.BOLD, scaled(12)));
        bubble.setColor(new Color(235, 242, 250));
        drawClippedString(bubble, prompt.speaker(), placement.x() + scaled(14), placement.y() + scaled(18), width - scaled(28));
        bubble.setFont(new Font("SansSerif", Font.PLAIN, scaled(12)));
        bubble.setColor(new Color(213, 222, 232));
        drawWrapped(bubble, prompt.line(), placement.x() + scaled(14), placement.y() + scaled(38), width - scaled(28), lineHeight, 2);

        int optionY = placement.y() + scaled(60);
        for (int i = 0; i < prompt.options().size(); i++) {
            int index = i;
            Rectangle bounds = new Rectangle(placement.x() + scaled(12), optionY, width - scaled(24), optionH);
            buttons.add(new UiButton(bounds, "banter:" + i, () -> {
                state.replyToTravelBanter(index);
                repaint();
            }));
            boolean hovered = hoverPoint != null && bounds.contains(hoverPoint);
            bubble.setColor(hovered ? new Color(53, 72, 98, 212) : new Color(35, 47, 64, 188));
            bubble.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 6, 6);
            bubble.setColor(hovered ? new Color(152, 184, 226, 198) : new Color(98, 139, 184, 172));
            bubble.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 6, 6);
            bubble.setColor(new Color(239, 243, 248));
            bubble.setFont(new Font("SansSerif", Font.BOLD, scaled(11)));
            drawClippedString(bubble, (i + 1) + ". " + prompt.options().get(i), bounds.x + scaled(11), bounds.y + scaled(20), bounds.width - scaled(22));
            String tooltip = index < prompt.optionTooltips().size()
                    ? prompt.optionTooltips().get(index)
                    : "Reply to the travelling companion without opening a full dialogue.";
            tooltipZones.add(new TooltipZone(bounds, prompt.options().get(i), tooltip));
            optionY += optionH + optionGap;
        }
        bubble.dispose();
    }

    private void drawAmbientTownConversationBubbles(Graphics2D g) {
        GameState.AmbientNpcConversation conversation = state.activeTownConversation();
        if (conversation == null) {
            return;
        }
        Rectangle speakerBounds = npcRenderBounds(conversation.speakerNpcKey());
        Rectangle listenerBounds = npcRenderBounds(conversation.listenerNpcKey());
        if (speakerBounds == null || listenerBounds == null) {
            return;
        }
        int width = Math.min(scaled(250), Math.max(scaled(180), gameAreaWidth() / 4));
        int height = scaled(60);
        BubblePlacement speakerPlacement = bubblePlacement(speakerBounds, width, height, scaled(12));
        BubblePlacement listenerPlacement = bubblePlacement(listenerBounds, width, height, scaled(12));
        if (speakerPlacement.bounds(width, height).intersects(listenerPlacement.bounds(width, height))) {
            int offset = scaled(34);
            if (speakerPlacement.x() <= listenerPlacement.x()) {
                speakerPlacement = speakerPlacement.shifted(-offset, 0);
                listenerPlacement = listenerPlacement.shifted(offset, 0);
            } else {
                speakerPlacement = speakerPlacement.shifted(offset, 0);
                listenerPlacement = listenerPlacement.shifted(-offset, 0);
            }
        }
        drawAmbientSpeechBubble(g, speakerPlacement, width, height, conversation.speakerLine(), new Color(255, 244, 214, 188));
        drawAmbientSpeechBubble(g, listenerPlacement, width, height, conversation.listenerLine(), new Color(221, 239, 255, 182));
    }

    private void drawAmbientSpeechBubble(Graphics2D g, BubblePlacement placement, int width, int height, String line, Color border) {
        Graphics2D bubble = (Graphics2D) g.create();
        Color fill = new Color(10, 14, 22, 172);
        bubble.setColor(fill);
        bubble.fillRoundRect(placement.x(), placement.y(), width, height, scaled(10), scaled(10));
        drawSpeechBubbleTail(bubble, placement, height, fill);
        bubble.setColor(border);
        bubble.setStroke(new BasicStroke(1.1f));
        bubble.drawRoundRect(placement.x(), placement.y(), width, height, scaled(10), scaled(10));
        drawSpeechBubbleTailOutline(bubble, placement, height, border);
        bubble.setFont(new Font("SansSerif", Font.PLAIN, scaled(11)));
        bubble.setColor(new Color(241, 244, 249));
        drawWrapped(bubble, line, placement.x() + scaled(12), placement.y() + scaled(22), width - scaled(24), scaled(16), 2);
        bubble.dispose();
    }

    private Rectangle partyFollowerBounds(String speaker) {
        if (speaker == null || speaker.isBlank()) {
            return fallbackSpeechAnchor();
        }
        for (int i = visiblePartyFollowers.size() - 1; i >= 0; i--) {
            PartyFollowerRender follower = visiblePartyFollowers.get(i);
            if (speaker.equals(follower.actor().name)) {
                return follower.screenBounds();
            }
        }
        return fallbackSpeechAnchor();
    }

    private Rectangle npcRenderBounds(String npcKey) {
        if (npcKey == null || npcKey.isBlank()) {
            return null;
        }
        for (int i = visibleNpcRenders.size() - 1; i >= 0; i--) {
            NpcRender render = visibleNpcRenders.get(i);
            if (npcKey.equals(render.key())) {
                return render.screenBounds();
            }
        }
        return null;
    }

    private Rectangle fallbackSpeechAnchor() {
        int x = gameAreaWidth() / 2 - scaled(18);
        int y = Math.max(scaled(40), worldViewportHeight() - scaled(136));
        return new Rectangle(x, y, scaled(36), scaled(54));
    }

    private BubblePlacement bubblePlacement(Rectangle anchor, int width, int height, int tailH) {
        Rectangle resolvedAnchor = anchor == null ? fallbackSpeechAnchor() : anchor;
        int anchorX = resolvedAnchor.x + resolvedAnchor.width / 2;
        int x = clamp(anchorX - width / 2, scaled(8), Math.max(scaled(8), gameAreaWidth() - width - scaled(8)));
        int aboveY = resolvedAnchor.y - height - tailH - scaled(4);
        boolean above = aboveY >= scaled(8);
        int y = above
                ? aboveY
                : clamp(resolvedAnchor.y + resolvedAnchor.height + tailH + scaled(4),
                scaled(8),
                Math.max(scaled(8), worldViewportHeight() - height - scaled(8)));
        int tailX = clamp(anchorX, x + scaled(18), x + width - scaled(18));
        return new BubblePlacement(x, y, tailX, above, tailH);
    }

    private void drawSpeechBubbleTail(Graphics2D g, BubblePlacement placement, int height, Color fill) {
        g.setColor(fill);
        g.fillPolygon(speechBubbleTail(placement, height));
    }

    private void drawSpeechBubbleTailOutline(Graphics2D g, BubblePlacement placement, int height, Color border) {
        g.setColor(border);
        g.drawPolygon(speechBubbleTail(placement, height));
    }

    private Polygon speechBubbleTail(BubblePlacement placement, int height) {
        int halfWidth = Math.max(scaled(8), placement.tailHeight());
        int baseY = placement.aboveAnchor() ? placement.y() + height - 1 : placement.y() + 1;
        int tipY = placement.aboveAnchor()
                ? placement.y() + height + placement.tailHeight()
                : placement.y() - placement.tailHeight();
        return new Polygon(
                new int[]{placement.tailX() - halfWidth, placement.tailX() + halfWidth, placement.tailX()},
                new int[]{baseY, baseY, tipY},
                3
        );
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
        WorldProp targetProp = gatherVisualProp(candidate);
        if (targetProp != null && PropPlacement.usesOffsets(state.world, state.currentMapId)) {
            Rectangle bounds = worldRenderer.propBounds(targetProp, tileSize, camX, camY);
            hitX = bounds.getCenterX();
            hitY = bounds.getMaxY() - bounds.height * 0.22;
        }
        drawGatherToolSwingOverlay(g, tool, playerToolX, playerToolY, hitX, hitY, tileSize,
                frame % 24, playerFacingDx, playerFacingDy, tile, camX, camY);
    }

    private void drawNpcGatherToolSwing(Graphics2D g, Npc npc, GameState.NpcMotion motion, int camX, int camY,
                                        int tileSize, int px, int py, boolean moving) {
        if (moving
                || state.mode != GameMode.EXPLORE
                || npc.job() == null
                || !WorldMap.OVERWORLD_ID.equals(state.currentMapId)
                || !npc.job().activeAt(state.timeOfDayMinutes())) {
            return;
        }
        int phaseFrame = npcGatherSwingFrame(npc);
        if (phaseFrame < 0) {
            return;
        }
        int facingDx = motion.facingDx();
        int facingDy = motion.facingDy();
        if (facingDx == 0 && facingDy == 0) {
            facingDy = 1;
        }
        String tool = gatherToolKindForNpc(npc);
        TilePoint targetTile = npcGatherTargetTile(npc, facingDx, facingDy);
        double workerToolX = px + tileSize / 2.0;
        double workerToolY = py + tileRelative(24, tileSize);
        double hitX = (targetTile.x() - camX) * (double) tileSize + tileSize / 2.0;
        double hitY = (targetTile.y() - camY) * (double) tileSize + gatherToolHitYOffset(tool, tileSize);
        WorldProp targetProp = state.world.propAt(state.currentMapId, targetTile.x(), targetTile.y());
        if (targetProp != null && PropPlacement.kind(targetProp.asset()) != PropPlacement.Kind.FIXED) {
            Rectangle bounds = worldRenderer.propBounds(targetProp, tileSize, camX, camY);
            hitX = bounds.getCenterX();
            hitY = bounds.getMaxY() - bounds.height * 0.22;
        }
        drawGatherToolSwingOverlay(g, tool, workerToolX, workerToolY, hitX, hitY, tileSize,
                phaseFrame, facingDx, facingDy, targetTile, camX, camY);
    }

    private int npcGatherSwingFrame(Npc npc) {
        int seed = Math.abs((npc.name() == null ? 0 : npc.name().hashCode()) * 31
                + (npc.sprite() == null ? 0 : npc.sprite().hashCode()) * 17
                + npc.job().kind().ordinal() * 53);
        int cycleFrame = Math.floorMod(frame + seed, 84);
        return cycleFrame < 24 ? cycleFrame : -1;
    }

    private TilePoint npcGatherTargetTile(Npc npc, int facingDx, int facingDy) {
        TilePoint best = null;
        int bestScore = Integer.MIN_VALUE;
        for (int y = npc.y() - 1; y <= npc.y() + 1; y++) {
            for (int x = npc.x() - 1; x <= npc.x() + 1; x++) {
                WorldProp prop = state.world.propAt(state.currentMapId, x, y);
                if (prop == null || !matchesNpcGatherProp(npc.job().kind(), prop.asset())) {
                    continue;
                }
                int dx = prop.x() - npc.x();
                int dy = prop.y() - npc.y();
                int distance = Math.abs(dx) + Math.abs(dy);
                int facingScore = facingDx * dx + facingDy * dy;
                int score = 30 - distance * 10 + facingScore * 4;
                if (score > bestScore) {
                    bestScore = score;
                    best = new TilePoint(prop.x(), prop.y());
                }
            }
        }
        if (best != null) {
            return best;
        }
        return new TilePoint(npc.x() + facingDx, npc.y() + facingDy);
    }

    private boolean matchesNpcGatherProp(NpcJob.Kind kind, String asset) {
        if (asset == null) {
            return false;
        }
        String lower = asset.toLowerCase();
        return switch (kind) {
            case FARMER -> lower.contains("farmland_wheat") || lower.contains("hay") || lower.contains("tilled");
            case WOODCUTTER -> (lower.contains("tree") || lower.contains("log") || lower.contains("stump"))
                    && (lower.contains("harvestable") || lower.contains("fallen") || lower.contains("wood"));
            case HERBALIST -> lower.contains("herb") || lower.contains("flower") || lower.contains("mushroom") || lower.contains("root");
        };
    }

    private String gatherToolKindForNpc(Npc npc) {
        return switch (npc.job().kind()) {
            case FARMER, HERBALIST -> "sickle";
            case WOODCUTTER -> "axe";
        };
    }

    private void drawGatherToolSwingOverlay(Graphics2D g, String tool, double actorToolX, double actorToolY,
                                            double hitX, double hitY, int tileSize, int phaseFrame,
                                            int fallbackFacingDx, int fallbackFacingDy, TilePoint impactTile,
                                            int camX, int camY) {
        double targetAngle = Math.atan2(hitY - actorToolY, hitX - actorToolX);
        if (Math.abs(hitX - actorToolX) < 0.01 && Math.abs(hitY - actorToolY) < 0.01) {
            targetAngle = Math.atan2(fallbackFacingDy, fallbackFacingDx == 0 && fallbackFacingDy == 0 ? 1 : fallbackFacingDx);
        }
        double phase = Math.floorMod(phaseFrame, 24) / 24.0;
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
        if (impactTile != null && strike > 0.86) {
            drawGatherImpact(g, impactTile, camX, camY, tileSize, tool);
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
            Color ring = objectiveColor(objective, 220);
            drawObjectiveGroundMarker(g, px, py, ts, ring);

            if (objective.kind().combatObjective()) {
                int width = scaled(42);
                int height = scaled(54);
                drawShadow(g, px + scaled(8), py + scaled(38), scaled(34), scaled(9));
                drawCharacterSprite(g, objective.asset(), px + (ts - width) / 2, py - scaled(9) + pulse, width, height, 0, 1, (frame / 8) % WALK_ANIMATION_FRAMES);
            } else if (!objective.markerOnly()) {
                int size = scaled(42);
                int drawX = px + (ts - size) / 2;
                int drawY = py + ts - size - scaled(3);
                g.drawImage(assets.spriteFit(objective.asset(), size, size), drawX, drawY, null);
            }

            drawObjectivePin(g, px + ts / 2, py + scaled(2) - pulse, objective);
        }
    }

    private boolean showQuestObjective(GameState.QuestObjective objective) {
        Quest quest = state.quests.get(objective.questId());
        return quest != null && quest.accepted && !quest.completed && (!quest.ready() || objective.markerOnly());
    }

    private WorldProp gatherVisualProp(CraftingSystem.GatherCandidate target) {
        if (target == null || target.asset() == null) return null;
        for (WorldProp prop : state.world.propsAt(state.currentMapId, target.tile().x(), target.tile().y())) {
            if (prop.asset().equals(target.asset())) return prop;
        }
        return null;
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

        WorldProp prop = gatherVisualProp(target);
        if (npc == null && prop != null && PropPlacement.usesOffsets(state.world, state.currentMapId)
                && PropPlacement.kind(prop.asset()) != PropPlacement.Kind.FIXED) {
            Rectangle bounds = worldRenderer.propBounds(prop, tileSize, camX, camY);
            int width = Math.max(tileSize / 3, Math.min(tileSize, bounds.width * 2 / 3));
            int height = Math.max(6, width / 3);
            int x = (int) bounds.getCenterX() - width / 2, y = (int) bounds.getMaxY() - height / 2;
            g.setColor(color);
            g.setComposite(AlphaComposite.SrcOver.derive(0.25f));
            g.fillOval(x, y, width, height);
            g.setComposite(AlphaComposite.SrcOver.derive(0.92f));
            g.setStroke(new BasicStroke(scaledStroke(2.0f)));
            g.drawOval(x - pulse, y - pulse / 2, width + pulse * 2, height + pulse);
            g.setStroke(oldStroke);
            g.setComposite(oldComposite);
            return;
        }
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
        return asset.startsWith("deco_ore_") || asset.contains("iron_vein")
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

    private void drawObjectivePin(Graphics2D g, int cx, int y, GameState.QuestObjective objective) {
        Quest.ObjectiveKind kind = objective.kind();
        Color fill = objectiveColor(objective, 255);
        Color rim = objective.mainStory() ? new Color(255, 212, 82, 245) : new Color(246, 247, 232, 235);
        int r = scaled(9);
        if (objective.dungeonHazard()) {
            drawObjectiveSkull(g, cx, y - scaled(8));
        }
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
            case RAID_DEFENSE -> "R";
            case CHOICE -> "C";
            case DEFEAT -> "B";
        };
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(glyph, cx - metrics.stringWidth(glyph) / 2, y + scaled(12));
        g.setStroke(new BasicStroke(1f));
    }

    private void drawObjectiveSkull(Graphics2D g, int cx, int cy) {
        int r = scaled(6);
        g.setColor(new Color(0, 0, 0, 150));
        g.fillOval(cx - r - scaled(1), cy - r - scaled(1), r * 2 + scaled(2), r * 2 + scaled(2));
        g.setColor(new Color(246, 247, 232, 235));
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
        g.fillRect(cx - scaled(4), cy + scaled(1), scaled(8), scaled(5));
        g.setColor(new Color(28, 24, 26, 230));
        int eye = Math.max(1, scaled(2));
        g.fillOval(cx - scaled(4), cy - scaled(2), eye, eye);
        g.fillOval(cx + scaled(2), cy - scaled(2), eye, eye);
        g.drawLine(cx - scaled(3), cy + scaled(5), cx + scaled(3), cy + scaled(5));
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
        WorldProp chest = state.chestNearPlayer();
        if (chest != null) return new NearbyPrompt(chest.x(), chest.y(), "Chest", Quest.ObjectiveKind.SEARCH, "Open");
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
        NearbyPrompt roamingPrompt = roamingWorldEventLayer.nearestPrompt(state);
        if (roamingPrompt != null) {
            int distance = Math.abs(roamingPrompt.x() - state.playerX) + Math.abs(roamingPrompt.y() - state.playerY);
            if (distance <= 1 && distance < bestDistance) {
                best = roamingPrompt;
                bestDistance = distance;
            }
        }
        for (GameState.QuestInteractible interactible : state.activeQuestInteractibles(state.currentMapId)) {
            int distance = Math.abs(interactible.x() - state.playerX) + Math.abs(interactible.y() - state.playerY);
            if (distance <= 1 && distance < bestDistance) {
                best = new NearbyPrompt(interactible.x(), interactible.y(), interactible.target(), Quest.ObjectiveKind.GATHER, "Harvest");
                bestDistance = distance;
            }
        }
        for (GameState.QuestObjective objective : state.activeQuestObjectives()) {
            if (objective.markerOnly()) {
                continue;
            }
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
                    case RAID_DEFENSE -> "Repel";
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
        String western = WesternReachFolklore.buildingName(state.currentMapId, building);
        if (!western.isEmpty()) return western;
        String regional = RegionalBuildingTypes.name(state.currentMapId, building);
        if (!regional.isEmpty()) return regional;
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

    private Color objectiveColor(GameState.QuestObjective objective, int alpha) {
        if (objective.mainStory()) {
            return new Color(220, 50, 47, alpha);
        }
        Quest.ObjectiveKind kind = objective.kind();
        if (kind == Quest.ObjectiveKind.GATHER || kind == Quest.ObjectiveKind.DELIVER) {
            return new Color(112, 220, 128, alpha);
        }
        if (kind.conversationObjective()) {
            return new Color(203, 157, 232, alpha);
        }
        if (kind.combatObjective() || kind.defenseMinigameObjective()) {
            return new Color(233, 89, 83, alpha);
        }
        return objectiveColor(kind, alpha);
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
            case RAID_DEFENSE -> new Color(255, 206, 96, alpha);
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
        TilePoint tile = hoverWorldTile();
        if (tile == null) return;
        int ts = tileSize();
        Rectangle bounds = worldRenderer.interiorPropBounds(
                new WorldProp(tile.x(), tile.y(), asset, VillageManager.interiorAssetSize(asset)), ts, 0, 0);
        int drawW = bounds.width, drawH = bounds.height;
        int x = bounds.x - (int) Math.round(lastCameraX * ts);
        int y = bounds.y - (int) Math.round(lastCameraY * ts);
        Composite oldComposite = g.getComposite();
        Shape oldClip = g.getClip();
        g.setClip(0, 0, gameAreaWidth(), worldViewportHeight());
        worldRenderer.drawPropImage(g, asset, x, y, drawW, drawH, 0.78f);
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
        int size = worldRenderer.propRenderSize(asset, propSize, tileSize);
        int drawW = worldRenderer.propWidth(asset, size);
        int drawH = worldRenderer.propHeight(asset, size);
        int px = (tile.x() - camX) * tileSize + (tileSize - drawW) / 2;
        int py = (tile.y() - camY) * tileSize + tileSize - drawH;
        Composite oldComposite = g.getComposite();
        Stroke oldStroke = g.getStroke();
        g.setComposite(AlphaComposite.SrcOver.derive(canPlace ? 0.62f : 0.42f));
        drawShadow(g, px + drawW / 6, py + drawH - scaled(7), drawW * 2 / 3, scaled(7));
        worldRenderer.drawPropImage(g, asset, px, py, drawW, drawH, 1.0f);
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

    private void drawCharacterSprite(Graphics2D g, String sprite, int x, int y, int width, int height, int facingDx, int facingDy,
                                     CharacterAnimationSelection selection) {
        DirectionalSprite directional = directionalSprite(sprite, facingDx, facingDy);
        boolean animated = selection != null
                && selection.animated()
                && assets.hasAnimatedSprite(directional.sprite(), selection.action());
        BufferedImage image = animated
                ? assets.animatedSpriteFit(directional.sprite(), selection.action(), width, height, selection.frame())
                : assets.spriteFit(directional.sprite(), width, height);
        String shadowKey = "char:" + directional.sprite() + ":" + width + "x" + height + ":"
                + (animated ? selection.action() + ":" + selection.frame() : "idle");
        drawCasterShadow(g, image, shadowKey, x, y, width, height, directional.flipHorizontal(), 0.42f);
        Graphics2D spriteG = (Graphics2D) g.create();
        spriteG.translate(x + width / 2.0, y + height);
        if (directional.flipHorizontal()) {
            spriteG.scale(-1.0, 1.0);
        }
        spriteG.drawImage(image, -width / 2, -height, width, height, null);
        spriteG.dispose();
    }

    private void drawCharacterSprite(Graphics2D g, String sprite, int x, int y, int width, int height, int facingDx, int facingDy, int step) {
        CharacterAnimationSelection selection = step >= 0
                ? new CharacterAnimationSelection("walk", step)
                : CharacterAnimationSelection.staticPose();
        drawCharacterSprite(g, sprite, x, y, width, height, facingDx, facingDy, selection);
    }

    private void drawCharacterActionSprite(Graphics2D g, String sprite, String action, int x, int y, int width, int height) {
        DirectionalSprite directional = directionalSprite(sprite, 0, 1);
        int actionW = Math.max(width, Math.round(width * 1.28f));
        int actionH = Math.max(height, Math.round(height * 1.18f));
        int frames = Math.max(1, assets.animatedSpriteFrameCount(directional.sprite(), action, actionW, actionH));
        int actionStep = Math.floorMod(frame / 8, frames);
        BufferedImage image = assets.animatedSpriteFit(directional.sprite(), action, actionW, actionH, actionStep);
        String shadowKey = "char:" + directional.sprite() + ":" + action + ":" + actionW + "x" + actionH + ":" + actionStep;
        int drawX = x + width / 2 - actionW / 2;
        int drawY = y + height - actionH;
        drawCasterShadow(g, image, shadowKey, drawX, drawY, actionW, actionH, directional.flipHorizontal(), 0.42f);
        g.drawImage(image, drawX, drawY, actionW, actionH, null);
    }

    private int walkAnimationFrame(int elapsed, int duration) {
        double progress = Math.max(0.0, Math.min(0.999, elapsed / (double) Math.max(1, duration)));
        return Math.min(WALK_ANIMATION_FRAMES - 1, (int) Math.floor(progress * WALK_ANIMATION_FRAMES));
    }

    private int npcWalkAnimationFrame(String sprite, int width, int height, GameState.NpcMotion motion) {
        DirectionalSprite directional = directionalSprite(sprite, motion.facingDx(), motion.facingDy());
        int animationFrames = Math.max(1, assets.animatedSpriteFrameCount(directional.sprite(), "walk", width, height));
        double progress = moveProgress(state.worldTick - motion.moveStartTick(), GameState.NpcMotion.MOVE_TICKS);
        int animationStep = (int) Math.floor(progress * animationFrames);
        return Math.floorMod(animationStep, animationFrames);
    }

    private int playerWalkAnimationFrame(String sprite, int width, int height) {
        DirectionalSprite directional = directionalSprite(sprite, playerFacingDx, playerFacingDy);
        int animationFrames = Math.max(1, assets.animatedSpriteFrameCount(directional.sprite(), "walk", width, height));
        double progress = moveProgress(frame - playerMoveStartFrame, playerMoveFrames());
        int animationStep = (int) Math.floor((playerWalkAnimationTileStart + progress) * animationFrames);
        return Math.floorMod(animationStep, animationFrames);
    }

    private int freePlayerWalkAnimationFrame(String sprite, int width, int height) {
        DirectionalSprite directional = directionalSprite(sprite, playerFacingDx, playerFacingDy);
        int animationFrames = Math.max(1, assets.animatedSpriteFrameCount(directional.sprite(), "walk", width, height));
        return Math.floorMod(frame / 3, animationFrames);
    }

    private void updatePlayerAnimationState(boolean locomotionActive) {
        if (locomotionActive) {
            if (!playerLocomotionActive) {
                playerLocomotionStartFrame = frame;
            }
        } else if (playerLocomotionActive) {
            playerLocomotionStopFrame = frame;
            playerIdleStartFrame = frame;
        }
        playerLocomotionActive = locomotionActive;
    }

    private CharacterAnimationSelection playerAnimationSelection(String sprite, int width, int height,
                                                                 boolean defenseMoving, boolean tileMoving, boolean freeMoving) {
        if (playerLocomotionActive) {
            CharacterAnimationSelection start = transitionAnimationSelection(
                    sprite, width, height, playerFacingDx, playerFacingDy, "start_walk", playerLocomotionStartFrame
            );
            if (start.animated()) {
                return start;
            }
            int walkFrame = defenseMoving ? (frame / 4) % WALK_ANIMATION_FRAMES
                    : tileMoving ? playerWalkAnimationFrame(sprite, width, height)
                    : freeMoving ? freePlayerWalkAnimationFrame(sprite, width, height)
                    : 0;
            return new CharacterAnimationSelection("walk", walkFrame);
        }
        CharacterAnimationSelection stop = transitionAnimationSelection(
                sprite, width, height, playerFacingDx, playerFacingDy, "stop_walk", playerLocomotionStopFrame
        );
        if (stop.animated()) {
            return stop;
        }
        CharacterAnimationSelection idle = idleAnimationSelection(
                sprite, width, height, playerFacingDx, playerFacingDy, playerIdleStartFrame, 0
        );
        return idle.animated() ? idle : CharacterAnimationSelection.staticPose();
    }

    private void updateFollowerAnimationState(FollowerVisualState visual, boolean moving) {
        if (moving) {
            if (!visual.locomotionActive) {
                visual.locomotionStartFrame = frame;
            }
        } else if (visual.locomotionActive) {
            visual.locomotionStopFrame = frame;
            visual.idleStartFrame = frame;
        }
        visual.locomotionActive = moving;
    }

    private CharacterAnimationSelection followerAnimationSelection(String sprite, int width, int height,
                                                                   int facingDx, int facingDy, FollowerVisualState visual) {
        if (visual.locomotionActive) {
            CharacterAnimationSelection start = transitionAnimationSelection(
                    sprite, width, height, facingDx, facingDy, "start_walk", visual.locomotionStartFrame
            );
            if (start.animated()) {
                return start;
            }
            return new CharacterAnimationSelection(
                    "walk",
                    followerWalkAnimationFrame(sprite, width, height, facingDx, facingDy, visual)
            );
        }
        CharacterAnimationSelection stop = transitionAnimationSelection(
                sprite, width, height, facingDx, facingDy, "stop_walk", visual.locomotionStopFrame
        );
        if (stop.animated()) {
            return stop;
        }
        CharacterAnimationSelection idle = idleAnimationSelection(
                sprite, width, height, facingDx, facingDy, visual.idleStartFrame, visual.idlePhaseSeed
        );
        return idle.animated() ? idle : CharacterAnimationSelection.staticPose();
    }

    private CharacterAnimationSelection transitionAnimationSelection(String sprite, int width, int height,
                                                                     int facingDx, int facingDy, String action,
                                                                     int actionStartFrame) {
        int frames = directionalActionFrameCount(sprite, action, width, height, facingDx, facingDy);
        if (frames <= 0 || actionStartFrame <= Integer.MIN_VALUE / 8) {
            return CharacterAnimationSelection.staticPose();
        }
        int elapsed = frame - actionStartFrame;
        if (elapsed < 0 || elapsed >= frames) {
            return CharacterAnimationSelection.staticPose();
        }
        return new CharacterAnimationSelection(action, elapsed);
    }

    private CharacterAnimationSelection idleAnimationSelection(String sprite, int width, int height,
                                                               int facingDx, int facingDy, int idleStartFrame, int seed) {
        int frames = directionalActionFrameCount(sprite, "idle", width, height, facingDx, facingDy);
        if (frames <= 0 || frame - idleStartFrame < IDLE_ANIMATION_TRIGGER_FRAMES) {
            return CharacterAnimationSelection.staticPose();
        }
        int idleFrame = Math.floorMod(((frame - idleStartFrame) / IDLE_ANIMATION_FRAME_DIVISOR) + seed, frames);
        return new CharacterAnimationSelection("idle", idleFrame);
    }

    private int directionalActionFrameCount(String sprite, String action, int width, int height, int facingDx, int facingDy) {
        DirectionalSprite directional = directionalSprite(sprite, facingDx, facingDy);
        if (!assets.hasAnimatedSprite(directional.sprite(), action)) {
            return 0;
        }
        return Math.max(1, assets.animatedSpriteFrameCount(directional.sprite(), action, width, height));
    }

    private DirectionalSprite directionalSprite(String sprite, int facingDx, int facingDy) {
        String direction;
        if (facingDx != 0 && facingDy != 0) {
            direction = facingDy < 0
                    ? facingDx < 0 ? "up_left" : "up_right"
                    : facingDx < 0 ? "down_left" : "down_right";
        } else if (Math.abs(facingDx) > Math.abs(facingDy)) {
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
        if ("up_left".equals(direction) || "down_left".equals(direction)) {
            String horizontalName = sprite + "_left";
            if (assets.hasSprite(horizontalName)) {
                return new DirectionalSprite(horizontalName, false);
            }
            horizontalName = sprite + "_right";
            if (assets.hasSprite(horizontalName)) {
                return new DirectionalSprite(horizontalName, true);
            }
        }
        if ("up_right".equals(direction) || "down_right".equals(direction)) {
            String horizontalName = sprite + "_right";
            if (assets.hasSprite(horizontalName)) {
                return new DirectionalSprite(horizontalName, false);
            }
            horizontalName = sprite + "_left";
            if (assets.hasSprite(horizontalName)) {
                return new DirectionalSprite(horizontalName, true);
            }
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

    private boolean freePlayerPositionValid() {
        return state.currentMapId.equals(freePlayerMapId)
                && Double.isFinite(freePlayerX)
                && Double.isFinite(freePlayerY);
    }

    private void ensureFreePlayerPosition() {
        if (!freePlayerPositionValid()) {
            syncFreePlayerPositionToState();
        }
    }

    private void syncFreePlayerPositionToState() {
        freePlayerMapId = state.currentMapId;
        freePlayerX = state.playerX + 0.5;
        freePlayerY = state.playerY + 0.5;
    }

    private boolean exploreFreeMoving() {
        if (state.mode != GameMode.EXPLORE || state.crafting.active() || playerMoving()
                || hasActivePlayerPath()) {
            return false;
        }
        int[] direction = heldMoveDirection();
        return direction[0] != 0 || direction[1] != 0;
    }

    private boolean explorePathMoving() {
        if (state.mode != GameMode.EXPLORE || state.crafting.active() || playerMoving()
                || !hasActivePlayerPath() || !freePlayerPositionValid()) {
            return false;
        }
        TilePoint target = playerPath.get(playerPathIndex);
        double targetX = target.x() + 0.5;
        double targetY = target.y() + 0.5;
        return Math.hypot(targetX - freePlayerX, targetY - freePlayerY) > 0.035;
    }

    private boolean hasActivePlayerPath() {
        return playerPathDestination != null && !playerPathExhausted();
    }

    private double exploreFreeStepSize() {
        int frames = playerMoveFramesForStep(state.currentMapId, state.playerX, state.playerY, state.playerX, state.playerY);
        return Math.min(0.24, EXPLORE_FREE_SPEED_MULTIPLIER / Math.max(1.0, frames));
    }

    private void tickExploreFreeMovement() {
        if (tickExplorePathMovement()) {
            return;
        }
        if (!exploreFreeMoving()) {
            return;
        }
        ensureFreePlayerPosition();
        int[] direction = heldMoveDirection();
        double length = Math.hypot(direction[0], direction[1]);
        if (length <= 0.0) {
            return;
        }
        setPlayerFacing(direction[0], direction[1]);
        double step = exploreFreeStepSize();
        double stepX = direction[0] / length * step;
        double stepY = direction[1] / length * step;
        if (stepX != 0.0 && tryMoveFreePlayer(freePlayerX + stepX, freePlayerY) && state.mode != GameMode.EXPLORE) {
            return;
        }
        if (stepY != 0.0) {
            tryMoveFreePlayer(freePlayerX, freePlayerY + stepY);
        }
    }

    private boolean tickExplorePathMovement() {
        if (state.mode != GameMode.EXPLORE || state.crafting.active() || playerMoving() || !hasActivePlayerPath()) {
            return false;
        }
        ensureFreePlayerPosition();
        TilePoint target = playerPath.get(playerPathIndex);
        double targetX = target.x() + 0.5;
        double targetY = target.y() + 0.5;
        double dx = targetX - freePlayerX;
        double dy = targetY - freePlayerY;
        double distance = Math.hypot(dx, dy);
        if (distance <= 0.035) {
            freePlayerX = targetX;
            freePlayerY = targetY;
            playerPathIndex++;
            return true;
        }
        updatePlayerFacingFromVector(dx, dy);
        double step = Math.min(exploreFreeStepSize(), distance);
        boolean moved = tryMoveFreePlayer(freePlayerX + dx / distance * step, freePlayerY + dy / distance * step);
        if (!moved && state.mode == GameMode.EXPLORE) {
            clearPlayerPath();
            state.status = "Path blocked.";
        }
        return true;
    }

    private boolean tryMoveFreePlayer(double nextX, double nextY) {
        String mapBefore = state.currentMapId;
        int tileBeforeX = state.playerX;
        int tileBeforeY = state.playerY;
        double previousX = freePlayerX;
        double previousY = freePlayerY;
        if (!state.moveFreeExploreTo(nextX, nextY)) {
            return false;
        }
        if (state.mode != GameMode.EXPLORE || !state.currentMapId.equals(mapBefore)) {
            syncPlayerAnimationToState();
            return true;
        }
        freePlayerX = nextX;
        freePlayerY = nextY;
        if (state.playerX != tileBeforeX || state.playerY != tileBeforeY) {
            int facingDx = Integer.compare(state.playerX, tileBeforeX);
            int facingDy = Integer.compare(state.playerY, tileBeforeY);
            recordFollowerTrailStep(mapBefore, tileBeforeX, tileBeforeY,
                    playerMoveFramesForStep(mapBefore, tileBeforeX, tileBeforeY, state.playerX, state.playerY),
                    facingDx,
                    facingDy);
            playerWalkAnimationTiles++;
        }
        return previousX != freePlayerX || previousY != freePlayerY;
    }

    private void updatePlayerFacingFromVector(double dx, double dy) {
        int facingDx = Math.abs(dx) < 0.01 ? 0 : dx < 0.0 ? -1 : 1;
        int facingDy = Math.abs(dy) < 0.01 ? 0 : dy < 0.0 ? -1 : 1;
        if (Math.abs(dx) >= Math.abs(dy) && facingDx != 0) {
            setPlayerFacing(facingDx, 0);
        } else if (facingDy != 0) {
            setPlayerFacing(0, facingDy);
        }
    }

    private void setPlayerFacing(int dx, int dy) {
        if (dx == 0 && dy == 0) {
            return;
        }
        playerFacingDx = Integer.compare(dx, 0);
        playerFacingDy = Integer.compare(dy, 0);
    }

    private double renderPlayerX() {
        if (state.mode == GameMode.DEFENSE && state.defenseRaid != null) {
            return state.defenseRaid.playerX() - 0.5;
        }
        if (state.mode == GameMode.EXPLORE && !playerMoving() && freePlayerPositionValid()) {
            return freePlayerX - 0.5;
        }
        if (!playerMoving()) {
            return state.playerX;
        }
        double t = smoothStep(moveProgress(frame - playerMoveStartFrame, playerMoveFrames()));
        return playerMoveFromX + (state.playerX - playerMoveFromX) * t;
    }

    private double renderPlayerY() {
        if (state.mode == GameMode.DEFENSE && state.defenseRaid != null) {
            return state.defenseRaid.playerY() - 0.5;
        }
        if (state.mode == GameMode.EXPLORE && !playerMoving() && freePlayerPositionValid()) {
            return freePlayerY - 0.5;
        }
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

    private WeatherQuality effectiveWeatherQuality() {
        return renderQuality().effectiveWeatherQuality(weatherQuality());
    }

    private RenderQuality renderQuality() {
        return RenderQuality.fromKey(state.config.renderQuality);
    }

    private int renderAnimationFrame() {
        return frame / renderQuality().terrainAnimationFrameStride;
    }

    private void applyRenderQualityHints(Graphics2D g) {
        RenderQuality quality = renderQuality();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, quality.antialiasing
                ? RenderingHints.VALUE_ANTIALIAS_ON
                : RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, quality.qualityRendering
                ? RenderingHints.VALUE_RENDER_QUALITY
                : RenderingHints.VALUE_RENDER_SPEED);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, quality.qualityRendering
                ? RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY
                : RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, quality.qualityRendering
                ? RenderingHints.VALUE_COLOR_RENDER_QUALITY
                : RenderingHints.VALUE_COLOR_RENDER_SPEED);
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
        if (state.mode == GameMode.DEFENSE || state.mode == GameMode.EXPLORE) {
            int dx = (moveRightHeld ? 1 : 0) - (moveLeftHeld ? 1 : 0);
            int dy = (moveDownHeld ? 1 : 0) - (moveUpHeld ? 1 : 0);
            return new int[]{dx, dy};
        }
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
        RegionalBuildingTypes.Type regional = RegionalBuildingTypes.type(state.currentMapId, building);
        if (regional != null) return regional.description;
        String western = WesternReachFolklore.buildingDescription(state.currentMapId, building);
        if (!western.isEmpty()) return western;
        String local = HearthlandsFolklore.buildingDescription(state.currentMapId, building);
        if (!local.isEmpty()) return local;
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
        RegionalBuildingTypes.Type regional = RegionalBuildingTypes.type(state.currentMapId, building);
        if (regional != null) return regional.purpose;
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
        RegionalBuildingTypes.Type regional = RegionalBuildingTypes.type(state.currentMapId, building);
        if (regional != null) return regional.dialogue.get(0);
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
        RegionalBuildingTypes.Type regional = RegionalBuildingTypes.type(state.currentMapId, building);
        if (regional != null) return regional.interior;
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
        if (!RegionalSettlementIdentity.buildingAsset(state.currentMapId, building).isEmpty()) return true;
        if (!WesternReachFolklore.buildingAsset(state.currentMapId, building).isEmpty()) return true;
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
        RegionalBuildingTypes.Type regional = RegionalBuildingTypes.type(state.currentMapId, building);
        if (regional != null) {
            int height = switch (regional) {
                case REMEMBRANCE_HALL, BELLHOUSE -> 232;
                case CARAVANSERAI, RESCUE_LODGE -> 210;
                case GRANARY -> 188;
                default -> 168;
            };
            return new int[]{Math.min(lotW + tileRelative(12, ts), tileRelative(204, ts)),
                    Math.min(building.depth() * ts + tileRelative(136, ts), tileRelative(height, ts))};
        }
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
        String western = WesternReachFolklore.buildingAsset(state.currentMapId, building);
        if (!western.isEmpty()) return western;
        String local = HearthlandsFolklore.buildingAsset(state.currentMapId, building);
        if (!local.isEmpty()) return local;
        String regional = RegionalSettlementIdentity.buildingAsset(state.currentMapId, building);
        if (!regional.isEmpty()) return regional;
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
        String western = WesternReachFolklore.buildingAsset(state.currentMapId, fallbackBuilding);
        if (!western.isEmpty()) return western;
        String local = HearthlandsFolklore.buildingAsset(state.currentMapId, fallbackBuilding);
        if (!local.isEmpty()) return local;
        String regional = RegionalSettlementIdentity.buildingAsset(state.currentMapId, fallbackBuilding);
        if (!regional.isEmpty()) return regional;
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
        g.setColor(new Color(12, 13, 18));
        g.fillRect(px, py, tileSize(), tileSize());
    }

    private void drawHouseFloorTile(Graphics2D g, char tile, int wx, int wy, int px, int py) {
        int ts = tileSize();
        int seed = Math.abs(wx * 928371 + wy * 364479 + tile * 71);
        g.drawImage(assets.image(interiorFloorAsset(seed), ts, ts), px, py, null);
        drawInteriorFloorDepth(g, px, py, ts, seed);
        g.setColor(InteriorStyle.forMap(state.currentMapId).materialTint);
        g.fillRect(px, py, ts, ts);
        // Contact occlusion belongs to the floor tile so chunk draw order cannot erase it.
        int depth = Math.max(2, ts / 5);
        for (int d = depth; d > 0; d--) {
            g.setColor(new Color(20, 15, 21, 4 + (depth - d) * 2));
            if (state.world.tileAt(state.currentMapId, wx, wy - 1) == 'o') g.fillRect(px, py, ts, d);
            if (state.world.tileAt(state.currentMapId, wx - 1, wy) == 'o') g.fillRect(px, py, d, ts);
            if (state.world.tileAt(state.currentMapId, wx + 1, wy) == 'o') g.fillRect(px + ts - d, py, d, ts);
        }
        if (state.world.interiorRugAt(state.currentMapId, wx, wy)) {
            InteriorStyle style = InteriorStyle.forMap(state.currentMapId);
            String rug = style == InteriorStyle.FENLANDS || style == InteriorStyle.THORNMERE
                    ? "interior_rug_teal" : "interior_rug_red";
            int border = Math.max(1, ts / 6);
            int left = state.world.interiorRugAt(state.currentMapId, wx - 1, wy) ? border : 0;
            int top = state.world.interiorRugAt(state.currentMapId, wx, wy - 1) ? border : 0;
            int right = state.world.interiorRugAt(state.currentMapId, wx + 1, wy) ? ts - border : ts;
            int bottom = state.world.interiorRugAt(state.currentMapId, wx, wy + 1) ? ts - border : ts;
            g.drawImage(assets.image(rug, ts, ts), px, py, px + ts, py + ts, left, top, right, bottom, null);
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

        if (straightVertical && (floorWest || floorEast)) {
            drawLinearVerticalWallShade(g, px, py, ts, floorWest, floorEast);
        }
        if (floorSouth) {
            g.setColor(new Color(0, 0, 0, 92));
            g.fillRect(px + scaled(3), py + ts - scaled(2), ts - scaled(3), scaled(5));
        }
        if (straightVertical) {
            drawInteriorVerticalCap(g, px, py, ts);
            return;
        }
        InteriorStyle style = InteriorStyle.forMap(state.currentMapId);
        String face = style == InteriorStyle.STORMBOUND ? "interior_wall_timber"
                : style == InteriorStyle.SUNREALM ? "interior_wall_plaster" : asset;
        g.drawImage(assets.image(face, ts, ts), px, py, null);
        g.setColor(style.materialTint);
        g.fillRect(px, py, ts, ts);

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

    private void drawInteriorVerticalCap(Graphics2D g, int px, int py, int ts) {
        g.setColor(new Color(49, 34, 29));
        g.fillRect(px, py, ts, ts);
        // Sample only the timber rail, continuously along the wall axis.
        BufferedImage timber = assets.image("interior_wall_timber", ts, ts);
        g.drawImage(timber, px + ts / 6, py, px + ts * 5 / 6, py + ts,
                ts / 4, ts / 4, ts * 3 / 4, ts * 3 / 4, null);
        g.setColor(new Color(181, 130, 78, 100));
        g.fillRect(px + ts / 6, py, Math.max(1, ts / 24), ts);
        g.setColor(new Color(0, 0, 0, 95));
        g.fillRect(px + ts * 5 / 6, py, ts / 6, ts);
        g.setColor(InteriorStyle.forMap(state.currentMapId).materialTint);
        g.fillRect(px, py, ts, ts);
    }

    private void drawHouseDoorTile(Graphics2D g, int wx, int wy, int px, int py) {
        int ts = tileSize();
        boolean horizontal = wallConnection(wx, wy, -1, 0) || wallConnection(wx, wy, 1, 0)
                || state.world.tileAt(state.currentMapId, wx - 1, wy) == 'e'
                || state.world.tileAt(state.currentMapId, wx + 1, wy) == 'e';
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
            return "interior_wall_horizontal_center";
        }
        if (west && south && !north && !east) {
            return "interior_wall_horizontal_center";
        }
        if (east && north && !south && !west) {
            return "interior_wall_horizontal_center";
        }
        if (west && north && !south && !east) {
            return "interior_wall_horizontal_center";
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
        return InteriorStyle.forMap(state.currentMapId).floor;
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
        terrainFeatureRenderer.drawTerrainEdges(g, tile, wx, wy, px, py);
    }

    private void drawSettlementSurfaceSeams(Graphics2D g, int camX, int camY) {
        terrainFeatureRenderer.drawSettlementSurfaceSeams(g, camX, camY);
    }

    private void drawWaterAnimation(Graphics2D g, int wx, int wy, int px, int py, int tileSize) {
        terrainFeatureRenderer.drawWaterAnimation(g, wx, wy, px, py, tileSize);
    }

    private boolean isWaterTile(int x, int y) {
        return terrainFeatureRenderer.isWaterTile(x, y);
    }

    private void drawFieldConnectors(Graphics2D g, int camX, int camY) {
        terrainFeatureRenderer.drawFieldConnectors(g, camX, camY);
    }

    private void drawRoadConnectors(Graphics2D g, int camX, int camY) {
        terrainFeatureRenderer.drawRoadConnectors(g, camX, camY);
    }

    private boolean drawsRoadUnderlay(char tile, String mapKind) {
        return terrainFeatureRenderer.drawsRoadUnderlay(tile, mapKind);
    }

    private char visibleTerrainTile(char tile, int wx, int wy) {
        return terrainFeatureRenderer.visibleTerrainTile(tile, wx, wy);
    }

    private char settlementUnderlayTile(int wx, int wy) {
        return terrainFeatureRenderer.settlementUnderlayTile(wx, wy);
    }

    private boolean isNatural(char tile) {
        return terrainFeatureRenderer.isNatural(tile);
    }

    private void drawMountainMassifOverlays(Graphics2D g, int camX, int camY) {
        terrainFeatureRenderer.drawMountainMassifOverlays(g, camX, camY);
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
        String authoredAsset = RegionalSettlementIdentity.overworldAsset(settlement.id());
        if (!authoredAsset.isEmpty()) {
            return authoredAsset;
        }
        if ("City".equals(settlement.kind()) || "Town".equals(settlement.kind())) {
            return switch (settlement.id()) {
                case "city_highwall" -> "city_overworld_village_snow";
                case "city_sanctum" -> "city_overworld_village_desert";
                case "city_belltower" -> "city_overworld_village_marsh";
                default -> "city_overworld_cluster_large";
            };
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
        drawSettlementRoadApproaches(g, settlement, px, py, drawSize);
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

    /** Carries the world road material under a settlement's authored gates instead of ending at its apron. */
    private void drawSettlementRoadApproaches(Graphics2D g, WorldMap.SettlementSite settlement,
                                               int px, int py, int drawSize) {
        int centerX = px + drawSize / 2;
        int centerY = py + drawSize / 2 + scaled(10);
        int reachX = drawSize / 2 + scaled("Town".equals(settlement.kind()) ? 30 : 24);
        int reachY = drawSize / 3 + scaled("Town".equals(settlement.kind()) ? 24 : 20);
        int[][] directions = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        Graphics2D road = (Graphics2D) g.create();
        road.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int[] direction : directions) {
            char material = settlementApproachRoad(settlement.x(), settlement.y(), direction[0], direction[1]);
            if (!Terrain.connectingRoad(material)) {
                continue;
            }
            int innerX = centerX + direction[0] * drawSize / 5;
            int innerY = centerY + direction[1] * drawSize / 6;
            int outerX = centerX + direction[0] * reachX;
            int outerY = centerY + direction[1] * reachY;
            Color shoulder = material == Terrain.COBBLESTONE_ROAD
                    ? new Color(124, 119, 109, 104)
                    : material == Terrain.PACKED_ROAD
                    ? new Color(132, 105, 65, 104)
                    : new Color(156, 118, 70, 98);
            Color core = material == Terrain.COBBLESTONE_ROAD
                    ? new Color(143, 137, 124, 132)
                    : material == Terrain.PACKED_ROAD
                    ? new Color(143, 111, 68, 132)
                    : new Color(166, 126, 75, 126);
            road.setColor(shoulder);
            road.setStroke(new BasicStroke(Math.max(2, scaled(28)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            road.drawLine(innerX, innerY, outerX, outerY);
            road.setColor(core);
            road.setStroke(new BasicStroke(Math.max(2, scaled(18)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            road.drawLine(innerX, innerY, outerX, outerY);
            int seed = settlement.x() * 928371 + settlement.y() * 364479 + direction[0] * 97 + direction[1] * 193;
            road.setColor(new Color(225, 200, 149, 42));
            road.setStroke(new BasicStroke(Math.max(1, scaled(2)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 1; i <= 4; i++) {
                double t = i / 5.0;
                int fx = (int) Math.round(innerX + (outerX - innerX) * t);
                int fy = (int) Math.round(innerY + (outerY - innerY) * t);
                int jitter = Math.floorMod(seed + i * 31, Math.max(2, scaled(10))) - scaled(5);
                if (direction[0] == 0) fx += jitter; else fy += jitter;
                road.fillOval(fx - scaled(2), fy - scaled(1), scaled(4), scaled(2));
            }
        }
        road.dispose();
    }

    private char settlementApproachRoad(int wx, int wy, int dx, int dy) {
        for (int distance = 3; distance <= 8; distance++) {
            char tile = state.world.tileAt(WorldMap.OVERWORLD_ID, wx + dx * distance, wy + dy * distance);
            if (Terrain.connectingRoad(tile)) {
                return tile;
            }
        }
        return 0;
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
        worldLightingRenderer.drawShadow(g, x, y, w, h);
    }

    private void drawCasterShadow(Graphics2D g, BufferedImage image, String cacheKey,
                                  int x, int y, int width, int height, boolean flipHorizontal, float baseAlpha) {
        worldLightingRenderer.drawCasterShadow(g, image, cacheKey, x, y, width, height, flipHorizontal, baseAlpha);
    }

    private void rebuildWorldLights(int camX, int camY, int visibleCols, int visibleRows, int tileSize) {
        worldLightingRenderer.useContext(lightingRenderContext(camX, camY, visibleCols, visibleRows, tileSize));
        RenderQuality quality = renderQuality();
        worldLightingRenderer.configurePerformance(
                quality.maxWorldLights,
                quality.ambientWorldEffects,
                quality.ambientWorldEffects
        );
        worldLightingRenderer.rebuildWorldLights(nearbyWorldProps, camX, camY, visibleCols, visibleRows, tileSize);
    }

    private int activeWorldLightCount() {
        return worldLightingRenderer.activeLightCount();
    }

    private void setShadowWorldOffset(double x, double y) {
        worldLightingRenderer.setShadowWorldOffset(x, y);
    }

    private void clearWorldLights() {
        worldLightingRenderer.clearLights();
    }

    private void drawLightTileSpill(Graphics2D g, int camX, int camY, int visibleCols, int visibleRows, int tileSize) {
        worldLightingRenderer.drawLightTileSpill(g, camX, camY, visibleCols, visibleRows, tileSize);
    }

    private void drawBiomeLightTints(Graphics2D g, int camX, int camY, int visibleCols, int visibleRows, int tileSize) {
        worldLightingRenderer.drawBiomeLightTints(g, camX, camY, visibleCols, visibleRows, tileSize);
    }

    private void drawEmissiveWorldLights(Graphics2D g, int camX, int camY, int tileSize, double cameraOffsetX, double cameraOffsetY) {
        worldLightingRenderer.drawEmissiveWorldLights(g, camX, camY, tileSize, cameraOffsetX, cameraOffsetY);
    }

    private Color propGlowColor(String asset) {
        return worldLightingRenderer.propGlowColor(asset);
    }

    private float lightVisibilityForAsset(String asset) {
        return worldLightingRenderer.lightVisibilityForAsset(asset);
    }

    private float nightFactor() {
        return worldLightingRenderer.nightFactor();
    }

    private void drawRadialGlow(Graphics2D g, int cx, int cy, int radius, Color color, float alpha) {
        worldLightingRenderer.drawRadialGlow(g, cx, cy, radius, color, alpha);
    }

    private void drawWorldVignette(Graphics2D g) {
        worldLightingRenderer.drawWorldVignette(g);
    }

    private WorldLightingRenderer.RenderContext lightingRenderContext(int camX, int camY, int visibleCols, int visibleRows, int tileSize) {
        return new WorldLightingRenderer.RenderContext(
                frame,
                camX,
                camY,
                visibleCols,
                visibleRows,
                tileSize,
                gameAreaWidth(),
                viewHeight(),
                renderPlayerX(),
                renderPlayerY());
    }

    private void drawCloudLayer(Graphics2D g, int camX, int camY) {
        worldAtmosphereRenderer.useContext(atmosphereRenderContext());
        worldAtmosphereRenderer.drawCloudLayer(g, camX, camY);
    }

    private void drawWorldAtmosphere(Graphics2D g) {
        worldAtmosphereRenderer.useContext(atmosphereRenderContext());
        worldAtmosphereRenderer.drawWorldAtmosphere(g);
    }

    private boolean isInteriorAtmosphereMap() {
        return worldAtmosphereRenderer.isInteriorAtmosphereMap();
    }

    private void drawInteriorLightTints(Graphics2D g, int camX, int camY, int visibleCols, int visibleRows, int tileSize) {
        worldAtmosphereRenderer.useContext(atmosphereRenderContext());
        worldAtmosphereRenderer.drawInteriorLightTints(g, camX, camY, visibleCols, visibleRows, tileSize);
    }

    private WorldAtmosphereRenderer.RenderContext atmosphereRenderContext() {
        return new WorldAtmosphereRenderer.RenderContext(
                renderAnimationFrame(),
                lastVisibleCols,
                lastVisibleRows,
                tileSize(),
                gameAreaWidth(),
                viewHeight());
    }

    private String terrainImageName(char tile, int wx, int wy) {
        if (tile == 'A') {
            return "field_farmland_tilled_dense";
        }
        String base = Terrain.assetName(tile);
        int count = switch (base) {
            case "grass", "forest", "tundra" -> 8;
            case "water", "road", "desert", "marsh", "badlands", "mountain", "mountain_massif_tile" -> 4;
            case "dungeon_cave_floor", "dungeon_castle_floor" -> 3;
            case "beach" -> 2;
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
        int actionRowsH = buttonH * 4 + rowGap * 3;
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
        sidebarButton(g, x, actionY + (buttonH + rowGap) * 3, halfW, buttonH, "Raid", state::startDefenseRaid);

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

    private void drawDefenseRaidHud(Graphics2D g) {
        CampDefenseMinigame raid = state.defenseRaid;
        if (raid == null) {
            return;
        }
        if (!raid.started()) {
            drawDefenseRaidLoadout(g, raid);
            return;
        }
        int x = scaled(28);
        int y = scaled(28);
        int w = scaled(470);
        int h = raid.finished() ? scaled(218) : scaled(192);
        g.setColor(new Color(12, 16, 22, 216));
        g.fillRoundRect(x, y, w, h, scaled(8), scaled(8));
        g.setColor(new Color(190, 162, 107, 170));
        g.drawRoundRect(x, y, w, h, scaled(8), scaled(8));
        g.setFont(new Font("SansSerif", Font.BOLD, scaled(18)));
        g.setColor(new Color(248, 230, 181));
        g.drawString("Camp Raid", x + scaled(18), y + scaled(28));
        g.setFont(new Font("SansSerif", Font.PLAIN, scaled(13)));
        g.setColor(new Color(207, 213, 221));
        g.drawString("Raid Lv " + raid.raidLevel() + "  XP " + raid.raidXp() + "/" + raid.raidXpToNext()
                + "  Wave " + raid.wave() + "/" + raid.maxWaves()
                + "  Enemies " + raid.enemies().size()
                + "  Kills " + raid.kills(), x + scaled(18), y + scaled(52));
        int barX = x + scaled(18);
        int barY = y + scaled(66);
        int barW = w - scaled(36);
        int barH = scaled(14);
        g.setColor(new Color(38, 42, 50));
        g.fillRoundRect(barX, barY, barW, barH, scaled(8), scaled(8));
        g.setColor(raid.campIntegrity() > 35 ? new Color(104, 194, 121) : new Color(222, 91, 74));
        g.fillRoundRect(barX, barY, (int) Math.round(barW * raid.campIntegrity() / 100.0), barH, scaled(8), scaled(8));
        g.setColor(new Color(235, 232, 215));
        g.drawRoundRect(barX, barY, barW, barH, scaled(8), scaled(8));
        g.drawString("Camp integrity " + raid.campIntegrity() + "%", barX, barY + scaled(31));
        if (raid.guardTicks() > 0) {
            g.setColor(new Color(158, 190, 246));
            g.drawString("Guard " + Math.max(1, raid.guardTicks() / 20) + "s", x + scaled(346), barY + scaled(31));
        }
        int abilityY = y + scaled(114);
        List<Ability> selected = raid.selectedAbilities();
        for (int i = 0; i < selected.size(); i++) {
            Ability ability = selected.get(i);
            int col = i % 4;
            int row = i / 4;
            int chipX = x + scaled(18) + col * scaled(108);
            int chipW = scaled(98);
            int chipY = abilityY + row * scaled(30);
            g.setColor(new Color(29, 34, 47, 226));
            g.fillRoundRect(chipX, chipY, chipW, scaled(24), scaled(7), scaled(7));
            g.setColor(abilityKindColor(ability));
            int cooldown = raid.abilityCooldown(i);
            int fillW = cooldown <= 0 ? chipW : (int) Math.round(chipW * Math.max(0.0, 1.0 - cooldown / 120.0));
            g.fillRoundRect(chipX, chipY, fillW, scaled(24), scaled(7), scaled(7));
            g.setColor(new Color(234, 236, 229));
            g.setFont(new Font("SansSerif", Font.BOLD, scaled(11)));
            drawClippedString(g, ability.name(), chipX + scaled(8), chipY + scaled(16), chipW - scaled(16));
        }
        if (raid.finished()) {
            g.setFont(new Font("SansSerif", Font.BOLD, scaled(14)));
            g.setColor(raid.victory() ? new Color(168, 224, 151) : new Color(238, 137, 111));
            drawClippedString(g, raid.resultText(), x + scaled(18), y + h - scaled(24), w - scaled(170));
            sidebarButton(g, x + w - scaled(136), y + h - scaled(38), scaled(112), scaled(26), "Close", state::closeDefenseRaid);
        } else if (raid.nextWaveCountdown() > 0) {
            g.setColor(new Color(190, 198, 208));
            g.drawString("Next wave in " + Math.max(1, raid.nextWaveCountdown() / 20) + "s", x + scaled(230), barY + scaled(31));
        }
        if (!raid.pendingLevelChoices().isEmpty()) {
            drawDefenseRaidLevelChoices(g, raid);
        }
    }

    private void drawDefenseRaidLevelChoices(Graphics2D g, CampDefenseMinigame raid) {
        List<Ability> choices = raid.pendingLevelChoices();
        int w = scaled(520);
        int h = scaled(214);
        int x = gameAreaCenteredX(w);
        int y = centeredY(h);
        g.setColor(new Color(10, 14, 22, 236));
        g.fillRoundRect(x, y, w, h, scaled(8), scaled(8));
        g.setColor(new Color(216, 183, 104, 190));
        g.drawRoundRect(x, y, w, h, scaled(8), scaled(8));
        g.setFont(new Font("SansSerif", Font.BOLD, scaled(21)));
        g.setColor(new Color(250, 229, 168));
        g.drawString("Raid Level " + raid.raidLevel(), x + scaled(24), y + scaled(34));
        g.setFont(new Font("SansSerif", Font.PLAIN, scaled(13)));
        g.setColor(new Color(198, 206, 219));
        g.drawString("Choose another learned ability for this raid rotation.", x + scaled(24), y + scaled(58));
        int rowY = y + scaled(78);
        for (int i = 0; i < choices.size(); i++) {
            Ability ability = choices.get(i);
            int choiceIndex = i;
            actionButton(g, x + scaled(24), rowY, w - scaled(48), scaled(36), "",
                    () -> state.chooseDefenseRaidLevelAbility(choiceIndex),
                    new Color(30, 38, 54, 238), new Color(101, 121, 165), true);
            g.setFont(new Font("SansSerif", Font.BOLD, scaled(13)));
            g.setColor(new Color(238, 241, 230));
            drawClippedString(g, (i + 1) + ". " + ability.name(), x + scaled(38), rowY + scaled(16), scaled(220));
            g.setFont(new Font("SansSerif", Font.PLAIN, scaled(12)));
            g.setColor(new Color(174, 184, 201));
            drawClippedString(g, ability.kind() + " | Power " + ability.power() + " | " + ability.target(),
                    x + scaled(270), rowY + scaled(16), w - scaled(308));
            rowY += scaled(44);
        }
    }

    private void drawDefenseRaidLoadout(Graphics2D g, CampDefenseMinigame raid) {
        int w = scaled(620);
        int x = gameAreaCenteredX(w);
        int y = scaled(76);
        List<Ability> abilities = raid.availableAbilities();
        int visible = Math.min(8, abilities.size());
        int h = scaled(132 + visible * 46);
        g.setColor(new Color(12, 16, 22, 230));
        g.fillRoundRect(x, y, w, h, scaled(8), scaled(8));
        g.setColor(new Color(190, 162, 107, 170));
        g.drawRoundRect(x, y, w, h, scaled(8), scaled(8));
        g.setFont(new Font("SansSerif", Font.BOLD, scaled(22)));
        g.setColor(new Color(248, 230, 181));
        g.drawString("Choose Raid Abilities", x + scaled(24), y + scaled(34));
        g.setFont(new Font("SansSerif", Font.PLAIN, scaled(13)));
        g.setColor(new Color(198, 204, 214));
        drawClippedString(g, "Pick up to three learned abilities. They fire automatically with effects based on their battle behavior.",
                x + scaled(24), y + scaled(60), w - scaled(48));
        if (abilities.isEmpty()) {
            g.setColor(new Color(218, 197, 151));
            g.drawString("No learned abilities yet. The raid can still begin with basic attacks disabled.", x + scaled(24), y + scaled(100));
        }
        int rowY = y + scaled(82);
        List<Ability> selected = raid.selectedAbilities();
        for (int i = 0; i < visible; i++) {
            Ability ability = abilities.get(i);
            int abilityIndex = i;
            boolean active = selected.stream().anyMatch(chosen -> chosen.name().equals(ability.name()));
            int rowX = x + scaled(24);
            int rowW = w - scaled(48);
            int rowH = scaled(38);
            actionButton(g, rowX, rowY, rowW, rowH, "", () -> state.toggleDefenseRaidAbility(abilityIndex),
                    active ? new Color(42, 66, 57, 238) : new Color(25, 30, 43, 232),
                    active ? new Color(122, 190, 127) : new Color(84, 94, 122), true);
            g.setFont(new Font("SansSerif", Font.BOLD, scaled(13)));
            g.setColor(active ? new Color(230, 247, 216) : new Color(235, 236, 240));
            drawClippedString(g, (i + 1) + ". " + ability.name(), rowX + scaled(12), rowY + scaled(17), scaled(230));
            g.setFont(new Font("SansSerif", Font.PLAIN, scaled(12)));
            g.setColor(new Color(178, 187, 201));
            String detail = ability.kind() + " | Power " + ability.power()
                    + (ability.target().contains("all") ? " | area" : "")
                    + (ability.visualResolution() == Ability.VisualResolution.MULTI_PROJECTILE ? " | multi" : "")
                    + (ability.visualResolution() == Ability.VisualResolution.CHAIN ? " | chain" : "");
            drawClippedString(g, detail, rowX + scaled(256), rowY + scaled(17), rowW - scaled(276));
            rowY += scaled(46);
        }
        if (abilities.size() > visible) {
            g.setFont(new Font("SansSerif", Font.PLAIN, scaled(12)));
            g.setColor(new Color(158, 166, 180));
            g.drawString("Showing first " + visible + " abilities from your learned set.", x + scaled(24), rowY + scaled(6));
        }
        actionButton(g, x + w - scaled(172), y + h - scaled(42), scaled(148), scaled(28), "Begin Raid", state::beginDefenseRaid,
                new Color(63, 78, 48), new Color(142, 184, 94), true);
    }

    private Color abilityKindColor(Ability ability) {
        return switch (ability.kind()) {
            case HEAL -> new Color(58, 128, 86, 212);
            case DEFEND -> new Color(65, 94, 154, 212);
            default -> new Color(126, 82, 47, 212);
        };
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

        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(230, 225, 206));
        g.drawString("ACTIVITY", x + 12, y + 21);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(new Color(145, 154, 174));
        String countLabel = statusLog.size() + " / " + MAX_STATUS_LOG_ENTRIES;
        g.drawString(countLabel, x + w - 12 - g.getFontMetrics().stringWidth(countLabel), y + 21);

        g.setColor(new Color(48, 54, 72, 190));
        g.drawLine(x + 12, y + 30, x + w - 12, y + 30);

        int contentX = x + 12;
        int contentY = y + 42;
        int contentW = w - 34;
        int lineHeight = 15;
        int contentBottomPadding = state.crafting.active() ? 28 : 12;
        int visibleLines = Math.max(1, (h - 33 - contentBottomPadding) / lineHeight);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        FontMetrics metrics = g.getFontMetrics();
        List<StatusLogLine> lines = new ArrayList<>();
        for (int messageIndex = 0; messageIndex < statusLog.size(); messageIndex++) {
            String message = statusLog.get(messageIndex);
            List<String> wrapped = wrappedTooltipLines(metrics, message, contentW);
            StatusLogTone tone = statusLogTone(message);
            boolean latest = messageIndex == statusLog.size() - 1;
            for (int lineIndex = 0; lineIndex < wrapped.size(); lineIndex++) {
                lines.add(new StatusLogLine(wrapped.get(lineIndex), tone, lineIndex == 0, latest));
            }
        }
        if (lines.isEmpty()) {
            lines.add(new StatusLogLine("No recent activity.", StatusLogTone.NARRATIVE, true, false));
        }
        int maxScroll = Math.max(0, lines.size() - visibleLines);
        statusLogScroll = clamp(statusLogScroll, 0, maxScroll);
        int start = Math.max(0, lines.size() - visibleLines - statusLogScroll);
        int end = Math.min(lines.size(), start + visibleLines);

        Shape oldClip = g.getClip();
        g.setClip(new Rectangle(contentX - 8, y + 31, contentW + 8, h - 34 - contentBottomPadding));
        for (int i = start; i < end; i++) {
            StatusLogLine line = lines.get(i);
            int lineY = contentY + (i - start) * lineHeight;
            drawStatusLogLine(g, line, contentX, lineY, contentW, lineHeight);
        }
        g.setClip(oldClip);

        if (maxScroll > 0) {
            int trackX = x + w - 14;
            int trackY = y + 32;
            int trackH = h - 42 - contentBottomPadding;
            int thumbH = Math.max(18, trackH * visibleLines / Math.max(visibleLines + maxScroll, 1));
            int thumbTravel = Math.max(1, trackH - thumbH);
            int thumbY = trackY + (int) Math.round((maxScroll - statusLogScroll) / (double) maxScroll * thumbTravel);
            g.setColor(new Color(37, 42, 58));
            g.fillRoundRect(trackX, trackY, 4, trackH, 4, 4);
            g.setColor(new Color(126, 141, 184));
            g.fillRoundRect(trackX, thumbY, 4, thumbH, 4, 4);
        }
    }

    private void drawStatusLogLine(Graphics2D g, StatusLogLine line, int x, int y, int width, int lineHeight) {
        Color accent = statusLogAccent(line.tone());
        if (line.latest()) {
            g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 18));
            g.fillRoundRect(x - 3, y - 11, width + 3, lineHeight, 4, 4);
        }

        g.setColor(line.firstLine() ? accent : new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 72));
        if (line.firstLine()) {
            g.fillRoundRect(x - 7, y - 9, 3, 10, 3, 3);
        } else {
            g.fillRect(x - 6, y - 11, 1, lineHeight);
        }

        FontMetrics metrics = g.getFontMetrics();
        int cursor = x;
        int tokenStart = 0;
        Color base = statusLogTextColor(line.tone());
        for (String token : line.text().split("(?<=\\s)|(?=\\s)")) {
            if (token.isEmpty()) {
                continue;
            }
            g.setColor(statusLogTokenColor(line.text(), token, tokenStart, line.tone(), base));
            g.drawString(token, cursor, y);
            cursor += metrics.stringWidth(token);
            tokenStart += token.length();
            if (cursor > x + width) {
                return;
            }
        }
    }

    private StatusLogTone statusLogTone(String message) {
        String normalized = message == null ? "" : message.strip().toLowerCase();
        if (normalized.contains("cannot ") || normalized.contains("can't ")
                || normalized.contains("not enough") || normalized.contains("nothing useful")
                || normalized.contains("already busy") || normalized.contains("failed")
                || normalized.contains("blocked") || normalized.startsWith("no ")
                || normalized.contains("requires level") || normalized.contains("does not ")) {
            return StatusLogTone.WARNING;
        }
        if (normalized.startsWith("quest ") || normalized.startsWith("tracking quest:")
                || normalized.contains("quest stage") || normalized.contains("objective")) {
            return StatusLogTone.QUEST;
        }
        if (normalized.startsWith("battle") || normalized.startsWith("victory")
                || normalized.startsWith("defeated") || normalized.contains(" attacks!")
                || normalized.contains("'s turn") || normalized.startsWith("targeting ")
                || normalized.startsWith("ally target:") || normalized.startsWith("escaped from battle")) {
            return StatusLogTone.COMBAT;
        }
        if (normalized.startsWith("finished:") || normalized.startsWith("gathered ")
                || normalized.startsWith("crafted ") || normalized.startsWith("caught ")
                || normalized.startsWith("mined ") || normalized.startsWith("loot:")
                || normalized.startsWith("bought ") || normalized.startsWith("sold ")
                || normalized.startsWith("paid ") || normalized.startsWith("used ")
                || normalized.startsWith("revived ") || normalized.contains(" unlocked:")
                || normalized.contains(" learned ") || normalized.contains(" equipped ")
                || normalized.contains(" improved ") || normalized.contains(" gained ")
                || normalized.contains(" earned ") || normalized.contains("level up")) {
            return StatusLogTone.REWARD;
        }
        if (normalized.startsWith("gathering") || normalized.startsWith("fishing")
                || normalized.startsWith("mining") || normalized.startsWith("chipping")
                || normalized.startsWith("chopping") || normalized.startsWith("hewing")
                || normalized.startsWith("crafting") || normalized.startsWith("cooking")
                || normalized.startsWith("reading") || normalized.endsWith("...")) {
            return StatusLogTone.ACTION;
        }
        if (normalized.startsWith("you enter ") || normalized.startsWith("you leave ")
                || normalized.startsWith("heading for ") || normalized.startsWith("saved ")
                || normalized.startsWith("loaded ") || normalized.endsWith(" updated.")) {
            return StatusLogTone.SYSTEM;
        }
        return StatusLogTone.NARRATIVE;
    }

    private Color statusLogAccent(StatusLogTone tone) {
        return switch (tone) {
            case ACTION -> new Color(91, 196, 190);
            case REWARD -> new Color(133, 207, 116);
            case QUEST -> new Color(221, 184, 91);
            case COMBAT -> new Color(222, 128, 96);
            case WARNING -> new Color(236, 132, 112);
            case SYSTEM -> new Color(118, 166, 226);
            case NARRATIVE -> new Color(116, 125, 151);
        };
    }

    private Color statusLogTextColor(StatusLogTone tone) {
        return switch (tone) {
            case ACTION -> new Color(197, 225, 221);
            case REWARD -> new Color(204, 230, 194);
            case QUEST -> new Color(232, 216, 171);
            case COMBAT -> new Color(236, 196, 180);
            case WARNING -> new Color(241, 194, 181);
            case SYSTEM -> new Color(198, 216, 239);
            case NARRATIVE -> new Color(184, 191, 207);
        };
    }

    private Color statusLogTokenColor(String line, String token, int tokenStart, StatusLogTone tone, Color fallback) {
        if (token.isBlank()) {
            return fallback;
        }
        String cleaned = token.replaceAll("^[^A-Za-z0-9+%-]+|[^A-Za-z0-9/%-]+$", "");
        String normalized = cleaned.toLowerCase();
        if (cleaned.matches("[+-]?\\d+(?:/\\d+)?(?:%|g)?")) {
            return new Color(247, 211, 117);
        }
        if (normalized.equals("xp") || normalized.equals("level") || normalized.equals("skills")
                || normalized.equals("skill") || normalized.equals("mp")) {
            return new Color(193, 166, 247);
        }
        if (normalized.equals("gold") || normalized.endsWith("g") && normalized.matches("\\d+g")) {
            return new Color(247, 211, 117);
        }
        if (isStatusLogKeyword(normalized)) {
            return statusLogAccent(tone);
        }
        int colon = line.indexOf(':');
        if (colon >= 0 && colon < 34 && tokenStart <= colon) {
            return statusLogAccent(tone);
        }
        if (colon >= 0 && tokenStart > colon
                && (line.substring(0, colon).toLowerCase().contains("unlocked")
                || line.substring(0, colon).toLowerCase().contains("learned"))) {
            return new Color(201, 178, 245);
        }
        if (insideQuantityPhrase(line, tokenStart)) {
            return new Color(158, 222, 164);
        }
        return statusColorForToken(token, fallback);
    }

    private boolean isStatusLogKeyword(String token) {
        return switch (token) {
            case "finished", "gathered", "crafted", "caught", "mined", "earned", "gained",
                    "unlocked", "learned", "completed", "victory", "bought", "sold", "paid",
                    "equipped", "improved", "saved", "loaded", "failed", "blocked", "requires" -> true;
            default -> false;
        };
    }

    private boolean insideQuantityPhrase(String line, int tokenStart) {
        int clauseStart = 0;
        for (char delimiter : new char[]{',', '.', ':', ';'}) {
            clauseStart = Math.max(clauseStart, line.lastIndexOf(delimiter, Math.max(0, tokenStart - 1)) + 1);
        }
        String clause = line.substring(clauseStart, Math.min(tokenStart + 1, line.length())).stripLeading();
        return clause.matches("[+]?\\d+[x×]?\\s+.*");
    }

    private enum StatusLogTone {
        ACTION,
        REWARD,
        QUEST,
        COMBAT,
        WARNING,
        SYSTEM,
        NARRATIVE
    }

    private record StatusLogLine(String text, StatusLogTone tone, boolean firstLine, boolean latest) {
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

    private void battleIconButton(
            Graphics2D g,
            int x,
            int y,
            int w,
            int h,
            String label,
            String hotkey,
            String sprite,
            String badge,
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
        Color buttonFill = enabled ? fill : new Color(36, 39, 49);
        Color buttonBorder = enabled ? border : new Color(70, 73, 84);
        if (hovered) {
            buttonFill = blend(buttonFill, Color.WHITE, 0.13);
            buttonBorder = blend(buttonBorder, Color.WHITE, 0.22);
        }
        if (pressed) {
            buttonFill = blend(buttonFill, Color.BLACK, 0.16);
            buttonBorder = blend(buttonBorder, Color.WHITE, 0.34);
        }
        if (pressed) {
            g.setColor(new Color(0, 0, 0, 130));
            g.fillRoundRect(x + 1, y + 2, w, h, 7, 7);
        }
        g.setColor(buttonFill);
        g.fillRoundRect(x, y + (pressed ? 1 : 0), w, h, 7, 7);
        g.setColor(buttonBorder);
        g.drawRoundRect(x, y, w, h, 7, 7);

        int iconSize = Math.max(24, Math.min(h - 14, w - 18));
        int iconX = x + (w - iconSize) / 2;
        int iconY = y + (h - iconSize) / 2 + (pressed ? 1 : 0);
        g.drawImage(assets.spriteFit(sprite, iconSize, iconSize), iconX, iconY, null);

        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.setColor(enabled ? new Color(244, 238, 212) : new Color(142, 146, 156));
        if (hotkey != null && !hotkey.isBlank()) {
            g.drawString(hotkey, x + 7, y + 14 + (pressed ? 1 : 0));
        }
        if (badge != null && !badge.isBlank()) {
            FontMetrics metrics = g.getFontMetrics();
            int badgeW = metrics.stringWidth(badge) + 8;
            int badgeH = 15;
            int badgeX = x + w - badgeW - 5;
            int badgeY = y + h - badgeH - 5 + (pressed ? 1 : 0);
            g.setColor(enabled ? new Color(12, 15, 23, 190) : new Color(22, 24, 31, 180));
            g.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 6, 6);
            g.setColor(enabled ? new Color(233, 236, 244) : new Color(144, 148, 158));
            g.drawString(badge, badgeX + 4, badgeY + 11);
        }
    }

    private void battleUseItemButton(
            Graphics2D g,
            int x,
            int y,
            int w,
            int h,
            boolean enabled
    ) {
        Rectangle bounds = new Rectangle(x, y, w, h);
        if (enabled) {
            buttons.add(new UiButton(bounds, "battle-use-item", () -> {
                battleItemPickerOpen = !battleItemPickerOpen;
                repaint();
            }));
        }
        boolean hovered = enabled && hoverPoint != null && bounds.contains(hoverPoint);
        boolean pressed = hovered && pressedButtonBounds != null && pressedButtonBounds.equals(bounds)
                && "battle-use-item".equals(pressedButtonLabel);
        Color fill = enabled ? new Color(55, 72, 79) : new Color(36, 39, 49);
        Color border = battleItemPickerOpen ? new Color(205, 187, 113) : new Color(98, 142, 151);
        if (hovered) {
            fill = blend(fill, Color.WHITE, 0.13);
            border = blend(border, Color.WHITE, 0.22);
        }
        if (pressed) {
            fill = blend(fill, Color.BLACK, 0.16);
            border = blend(border, Color.WHITE, 0.34);
        }
        g.setColor(fill);
        g.fillRoundRect(x, y + (pressed ? 1 : 0), w, h, 7, 7);
        g.setColor(border);
        g.drawRoundRect(x, y, w, h, 7, 7);
        int iconSize = h - 21;
        g.drawImage(assets.spriteFit("icon_chest", iconSize, iconSize), x + (w - iconSize) / 2, y + 5 + (pressed ? 1 : 0), null);
        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        g.setColor(enabled ? new Color(235, 239, 244) : new Color(142, 146, 156));
        FontMetrics metrics = g.getFontMetrics();
        String text = "Use Item";
        g.drawString(text, x + (w - metrics.stringWidth(text)) / 2, y + h - 7 + (pressed ? 1 : 0));
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
            case "common" -> ShopRenderer.rarityColor(ItemRarity.COMMON);
            case "uncommon" -> ShopRenderer.rarityColor(ItemRarity.UNCOMMON);
            case "rare" -> ShopRenderer.rarityColor(ItemRarity.RARE);
            case "unique" -> ShopRenderer.rarityColor(ItemRarity.UNIQUE);
            case "legendary" -> ShopRenderer.rarityColor(ItemRarity.LEGENDARY);
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
            case "evasive" -> new Color(157, 220, 255);
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
            g.setColor(objectiveColor(objective, 255));
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
        drawBattleEffect(g, battle, x, y, w, h);
        // Combat information stays readable over even the largest area impacts.
        for (int i = 0; i < visibleParty; i++) {
            drawBattlePartyCard(g, battle, party.get(i), x, y, w, h);
        }
        for (Actor foe : battle.enemies()) {
            drawBattleEnemyCard(g, battle, foe, x, y, w, h);
        }
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
            g.drawString(active.name + "'s turn", x + 42, y + h - 174);
        }

        drawBattleLog(g, battle, x + w - 454, y + h - 146, 420, 112);
        drawBattleActionBar(g, battle, x + 34, y + h - 156, w - 502, 132);
        if (battleVfxDebug) {
            drawBattleVfxDebugOverlay(g, battle, x + w - 368, y + 24, 334, 142);
        }
        drawBattleIntroOverlay(g, battle, x, y, w, h);
    }

    private void drawBattleIntroOverlay(Graphics2D g, Battle battle, int x, int y, int w, int h) {
        if (battle == null || !battle.introActive()) {
            return;
        }
        Graphics2D overlay = (Graphics2D) g.create();
        overlay.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double rawProgress = battle.introProgress();
        double progress = smoothStep(rawProgress);
        double shutterProgress = smoothStep(clamp(rawProgress / 0.3, 0.0, 1.0));
        double textIn = smoothStep(clamp((rawProgress - 0.08) / 0.14, 0.0, 1.0));
        double textOut = 1.0 - smoothStep(clamp((rawProgress - 0.92) / 0.08, 0.0, 1.0));
        float shadeAlpha = (float) (0.86 * (1.0 - smoothStep(clamp((rawProgress - 0.68) / 0.32, 0.0, 1.0))));
        Color accent = battleIntroAccent(battle);

        overlay.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, shadeAlpha));
        overlay.setPaint(new GradientPaint(x, y, new Color(8, 10, 15, 246), x, y + h, new Color(18, 22, 32, 236)));
        overlay.fillRect(x, y, w, h);
        overlay.setPaint(null);

        int barHeight = (int) Math.round(h * 0.14 * (1.0 - smoothStep(clamp(rawProgress / 0.34, 0.0, 1.0))));
        if (barHeight > 0) {
            overlay.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.92f));
            overlay.setColor(new Color(4, 5, 8, 246));
            overlay.fillRect(x, y, w, barHeight);
            overlay.fillRect(x, y + h - barHeight, w, barHeight);
        }

        int stripCount = 11;
        int stripH = Math.max(24, (int) Math.ceil(h / (double) stripCount));
        for (int i = 0; i < stripCount; i++) {
            double delay = i * 0.028;
            double local = smoothStep(clamp((shutterProgress - delay) / 0.72, 0.0, 1.0));
            int currentH = (int) Math.round(stripH * (1.0 - local));
            if (currentH <= 1) {
                continue;
            }
            int centerY = y + i * stripH + stripH / 2;
            int top = centerY - currentH / 2;
            int bottom = top + currentH;
            int skew = (int) Math.round((1.0 - local) * 24.0);
            int sideShift = (int) Math.round((1.0 - local) * 18.0) * (i % 2 == 0 ? 1 : -1);
            Polygon strip = new Polygon(
                    new int[]{x - skew + sideShift, x + w + skew + sideShift, x + w - sideShift, x + sideShift},
                    new int[]{top, top + skew / 3, bottom, bottom - skew / 3},
                    4
            );
            float stripAlpha = (float) (0.84 * (1.0 - local));
            overlay.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, stripAlpha));
            overlay.setColor(new Color(7, 8, 11, 248));
            overlay.fillPolygon(strip);
            overlay.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(0.65f, stripAlpha + 0.12f)));
            overlay.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 220));
            overlay.drawLine(x, top, x + w, top);
        }

        float glowAlpha = (float) (0.16 * (1.0 - smoothStep(clamp((rawProgress - 0.8) / 0.2, 0.0, 1.0))));
        if (glowAlpha > 0.01f) {
            overlay.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, glowAlpha));
            overlay.setPaint(new GradientPaint(x, y, new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0),
                    x + w, y + h, new Color(255, 245, 214, 110)));
            overlay.fillRoundRect(x + 24, y + h / 2 - 96, w - 48, 192, 24, 24);
            overlay.setPaint(null);
        }

        float textAlpha = (float) Math.min(1.0, textIn * textOut);
        if (textAlpha > 0.01f) {
            drawBattleIntroText(overlay, battle, x, y, w, h, accent, textAlpha, progress);
        }
        overlay.dispose();
    }

    private void drawBattleIntroText(Graphics2D g, Battle battle, int x, int y, int w, int h, Color accent, float alpha, double progress) {
        Graphics2D text = (Graphics2D) g.create();
        text.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        int centerX = x + w / 2;
        int centerY = y + h / 2 - 46 + (int) Math.round((1.0 - smoothStep(clamp((progress - 0.08) / 0.14, 0.0, 1.0))) * 14.0);
        int textWidth = Math.min(780, w - 180);
        int bandX = centerX - textWidth / 2 - 30;
        int bandY = centerY - 88;
        int bandH = 188;
        text.setColor(new Color(6, 8, 14, 184));
        text.fillRoundRect(bandX, bandY, textWidth + 60, bandH, 24, 24);
        text.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 210));
        text.setStroke(new BasicStroke(2f));
        text.drawRoundRect(bandX, bandY, textWidth + 60, bandH, 24, 24);

        String styleLabel = battle.introStyleLabel();
        if (!styleLabel.isBlank()) {
            text.setFont(new Font("SansSerif", Font.BOLD, 16));
            FontMetrics styleMetrics = text.getFontMetrics();
            int styleWidth = styleMetrics.stringWidth(styleLabel);
            int pillW = styleWidth + 30;
            int pillX = centerX - pillW / 2;
            int pillY = bandY - 18;
            text.setColor(new Color(12, 14, 22, 220));
            text.fillRoundRect(pillX, pillY, pillW, 28, 14, 14);
            text.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 235));
            text.drawRoundRect(pillX, pillY, pillW, 28, 14, 14);
            text.setColor(new Color(252, 238, 204));
            text.drawString(styleLabel, centerX - styleWidth / 2, pillY + 19);
        }

        String encounterLine = battle.introEncounterLine();
        if (!encounterLine.isBlank()) {
            text.setFont(new Font("SansSerif", Font.BOLD, 32));
            FontMetrics encounterMetrics = text.getFontMetrics();
            List<String> lines = wrappedTooltipLines(encounterMetrics, encounterLine, textWidth);
            int baseY = centerY - (lines.size() - 1) * 20;
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                int lineWidth = encounterMetrics.stringWidth(line);
                int lineX = centerX - lineWidth / 2;
                int lineY = baseY + i * 38;
                text.setColor(new Color(6, 7, 10, 210));
                text.drawString(line, lineX + 3, lineY + 3);
                text.setColor(new Color(246, 238, 220));
                text.drawString(line, lineX, lineY);
            }
        }

        String bark = battle.introPartyBark();
        if (!bark.isBlank()) {
            text.setFont(new Font("SansSerif", Font.PLAIN, 17));
            FontMetrics barkMetrics = text.getFontMetrics();
            List<String> barkLines = wrappedTooltipLines(barkMetrics, bark, textWidth - 60);
            int barkStartY = centerY + 50;
            for (int i = 0; i < Math.min(3, barkLines.size()); i++) {
                String line = barkLines.get(i);
                int lineWidth = barkMetrics.stringWidth(line);
                int lineX = centerX - lineWidth / 2;
                int lineY = barkStartY + i * 22;
                text.setColor(new Color(8, 10, 14, 210));
                text.drawString(line, lineX + 2, lineY + 2);
                text.setColor(new Color(218, 226, 236));
                text.drawString(line, lineX, lineY);
            }
        }
        text.dispose();
    }

    private Color battleIntroAccent(Battle battle) {
        if (battle == null) {
            return new Color(186, 164, 94);
        }
        if ("dungeon".equals(battle.mapKind)) {
            return switch (battle.terrain) {
                case 'n', 'i' -> new Color(135, 180, 224);
                case 'v', 'q' -> new Color(84, 162, 152);
                case 'm', 'r' -> new Color(180, 126, 92);
                default -> new Color(171, 138, 212);
            };
        }
        return switch (battle.terrain) {
            case 'f', 't' -> new Color(118, 180, 102);
            case 'v', 'q' -> new Color(84, 170, 156);
            case 'n', 'i' -> new Color(146, 186, 230);
            case 'm', 'r' -> new Color(191, 132, 92);
            case 'd', 's' -> new Color(215, 177, 96);
            case 'b', 'u' -> new Color(170, 104, 82);
            default -> new Color(197, 169, 102);
        };
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
        if (!canAct) {
            battleItemPickerOpen = false;
        }
        int buttonY = y + 14;
        int slot = 50;
        int gap = 8;
        int cursorX = x + 18;
        battleIconButton(g, cursorX, buttonY, slot, slot, "battle-attack", "A", "icon_sword", "",
                state::battleAttack, new Color(92, 79, 50), new Color(156, 132, 76), canAct);
        if (active != null) {
            tooltipZones.add(new TooltipZone(new Rectangle(cursorX, buttonY, slot, slot), "Attack", attackTooltip(active, battle.selectedEnemy()), "icon_sword"));
        }
        cursorX += slot + gap;
        battleIconButton(g, cursorX, buttonY, slot, slot, "battle-heavy", "S", "fx_impact", "3",
                state::battleHeavyAttack, new Color(105, 68, 48), new Color(177, 104, 72), canAct && active != null && active.mp >= 3);
        tooltipZones.add(new TooltipZone(new Rectangle(cursorX, buttonY, slot, slot), "Heavy Attack", "Costs 3 MP. Slower, harder hit that applies Weak when it connects.", "fx_impact"));
        cursorX += slot + gap;
        battleIconButton(g, cursorX, buttonY, slot, slot, "battle-cleave", "F", "fx_cleave", "2",
                state::battleCleaveAttack, new Color(84, 71, 52), new Color(157, 126, 79), canAct && active != null && active.mp >= 2);
        tooltipZones.add(new TooltipZone(new Rectangle(cursorX, buttonY, slot, slot), "Cleave", "Costs 2 MP. Hits up to three living enemies for reduced physical damage.", "fx_cleave"));
        cursorX += slot + gap;
        battleIconButton(g, cursorX, buttonY, slot, slot, "battle-evade", "D", "fx_dust", "+2",
                state::battleDodge, new Color(48, 77, 88), new Color(87, 145, 164), canAct);
        tooltipZones.add(new TooltipZone(new Rectangle(cursorX, buttonY, slot, slot), "Evade", "Uses the turn to gain Evasive, Haste, and +2 MP. Evasive boosts the next dodge or softens the next hit.", "fx_dust"));
        boolean allyTargetReady = battle.hasExplicitPartyTarget();
        cursorX += slot + gap;
        battleIconButton(g, cursorX, buttonY, slot, slot, "battle-run", "R", "fx_smoke_veil_unique", "",
                state::battleRun, new Color(80, 70, 87), new Color(143, 125, 164), canAct);
        tooltipZones.add(new TooltipZone(new Rectangle(cursorX, buttonY, slot, slot), "Run", "Attempts to escape combat. Dexterity helps most; constitution helps keep moving. Failing spends the turn.", "fx_smoke_veil_unique"));
        cursorX += slot + gap;
        int itemW = 76;
        battleUseItemButton(g, cursorX, buttonY, itemW, slot, canAct);
        tooltipZones.add(new TooltipZone(new Rectangle(cursorX, buttonY, itemW, slot), "Use Item",
                allyTargetReady ? "Open consumables that can restore the selected ally." : "Select an ally, then open consumables.", "icon_chest"));
        cursorX += itemW + 18;

        List<Ability> abilities = active == null ? List.of() : active.activeAbilities();
        int visibleAbilities = Math.min(10, abilities.size());
        for (int i = 0; i < visibleAbilities; i++) {
            Ability ability = abilities.get(i);
            int buttonX = cursorX + i * (slot + gap);
            int rowY = buttonY;
            boolean targetReady = !battle.requiresExplicitAllyTarget(ability) || allyTargetReady;
            int cooldown = battle.abilityCooldownRemaining(active, ability);
            boolean enabled = canAct && active.mp >= ability.cost() && targetReady && cooldown <= 0;
            String hotkey = i == 9 ? "0" : Integer.toString(i + 1);
            int index = i;
            String badge = cooldown > 0 ? "CD " + cooldown : Integer.toString(ability.cost());
            battleIconButton(g, buttonX, rowY, slot, slot, "battle-ability:" + index, hotkey, abilityIconName(ability), badge,
                    () -> state.battleAbility(index), new Color(76, 53, 93), new Color(139, 107, 168), enabled);
            String tooltip = targetReady ? abilityTooltip(active, ability, battle) : abilityTooltip(active, ability, battle) + "\nSelect an ally before using this beneficial ability.";
            tooltipZones.add(new TooltipZone(new Rectangle(buttonX, rowY, slot, slot),
                    ability.name(), tooltip, abilityIconName(ability)));
        }
        if (active != null && active.abilities.size() > abilities.size()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g.setColor(new Color(170, 176, 191));
            g.drawString("Manage loadout on Character", cursorX, y + h - 12);
        }
        if (battleItemPickerOpen) {
            drawBattleItemPicker(g, battle, x + 18, y + 75, w - 36, h - 88, canAct, allyTargetReady);
        }
    }

    private void drawBattleItemPicker(Graphics2D g, Battle battle, int x, int y, int w, int h, boolean canAct, boolean allyTargetReady) {
        List<String> consumables = battleConsumableKeys(battle, allyTargetReady);
        g.setColor(new Color(17, 20, 30, 230));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(82, 91, 115));
        g.drawRoundRect(x, y, w, h, 8, 8);
        if (!allyTargetReady) {
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.setColor(new Color(207, 211, 224));
            g.drawString("Select an ally target to use consumables.", x + 14, y + 25);
            return;
        }
        if (consumables.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.setColor(new Color(207, 211, 224));
            g.drawString("No usable consumables for this ally.", x + 14, y + 25);
            return;
        }
        int slot = Math.min(42, h - 8);
        int gap = 8;
        int maxVisible = Math.max(1, (w - 20) / (slot + gap));
        int visible = Math.min(consumables.size(), maxVisible);
        for (int i = 0; i < visible; i++) {
            String itemKey = consumables.get(i);
            Item item = GameData.ITEMS.get(itemKey);
            int itemX = x + 10 + i * (slot + gap);
            int count = state.player.inventory.getOrDefault(itemKey, 0);
            boolean enabled = canAct && canUseConsumableOnSelectedAlly(battle, item);
            battleIconButton(g, itemX, y + 4, slot, slot, "battle-item:" + itemKey, i < 9 ? Integer.toString(i + 1) : "",
                    GameData.itemIcon(itemKey), "x" + count, () -> {
                        state.useItem(itemKey);
                        battleItemPickerOpen = false;
                    }, new Color(52, 82, 70), new Color(100, 151, 117), enabled);
            tooltipZones.add(new TooltipZone(new Rectangle(itemX, y + 4, slot, slot),
                    item.name(), itemTooltip(item, count), GameData.itemIcon(itemKey)));
        }
        if (consumables.size() > visible) {
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.setColor(new Color(194, 199, 214));
            g.drawString("+" + (consumables.size() - visible), x + 10 + visible * (slot + gap), y + 29);
        }
    }

    private List<String> battleConsumableKeys(Battle battle, boolean allyTargetReady) {
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : state.player.inventory.entrySet()) {
            if (entry.getValue() <= 0) {
                continue;
            }
            Item item = GameData.ITEMS.get(entry.getKey());
            if (item == null || (item.heal() <= 0 && item.mp() <= 0) || "escape_scroll".equals(entry.getKey())) {
                continue;
            }
            if (state.player.level < item.minLevel()) {
                continue;
            }
            if (allyTargetReady && !canUseConsumableOnSelectedAlly(battle, item)) {
                continue;
            }
            keys.add(entry.getKey());
        }
        return keys;
    }

    private boolean canUseConsumableOnSelectedAlly(Battle battle, Item item) {
        Actor target = battle.selectedPartyMember();
        if (target == null || item == null) {
            return false;
        }
        boolean restoresHp = item.heal() > 0 && target.hp < target.maxHp;
        boolean restoresMp = item.mp() > 0 && target.mp < target.maxMp;
        return restoresHp || restoresMp;
    }

    private String itemTooltip(Item item, int count) {
        List<String> parts = new ArrayList<>();
        parts.add("Owned: " + count);
        if (item.heal() > 0) {
            parts.add("Restores " + item.heal() + " HP.");
        }
        if (item.mp() > 0) {
            parts.add("Restores " + item.mp() + " MP.");
        }
        if (item.minLevel() > 1) {
            parts.add("Requires level " + item.minLevel() + ".");
        }
        if (!item.effectDescription().isBlank()) {
            parts.add(item.effectDescription());
        }
        parts.add("Uses the active party member's turn.");
        return String.join("\n", parts);
    }

    private void drawBattlePartyMember(Graphics2D g, Battle battle, Actor actor, boolean active, int panelX, int panelY, int panelW, int panelH) {
        int[] slot = battlePartyPosition(battle, actor, panelX, panelY, panelW, panelH);
        boolean selected = battle.hasExplicitPartyTarget() && actor == battle.selectedPartyMember();
        int spriteW = 150;
        int spriteH = 194;
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
        BattleActorPose pose = battleRenderer.actorPose(battle, actor, false);
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
        battleRenderer.drawHitFlash(g, battleActorImage(battle, actor, actorSprite, renderedAction, spriteW, renderH),
                drawX, renderY, spriteW, renderH, pose, battle.hitFlash(actor));
        battleVfxRenderer.drawActorStatuses(g, battle, actor, drawX, drawY, spriteW, spriteH);
        if (selected && actor.alive()) {
            drawBattlePartyTargetMarker(g, drawX, drawY, spriteW, spriteH);
        }
        if (battle.hasIncomingTelegraph(actor)) {
            drawBattleIncomingTelegraph(g, battle.incomingTelegraphLabel(actor), battle.incomingTelegraphDanger(actor), drawX, drawY, spriteW);
        }
        if (battle.isCasting(actor)) {
            g.setColor(new Color(156, 204, 255, 150));
            g.setStroke(new BasicStroke(3f));
            int pulse = 8 + (frame % 12);
            g.drawOval(drawX + 18 - pulse / 2, drawY + spriteH - 36 - pulse / 3, spriteW - 36 + pulse, 28 + pulse / 2);
        }

        if (!actor.alive()) {
            g.setColor(new Color(9, 10, 16, 150));
            g.fillRoundRect(drawX, drawY, spriteW, spriteH, 8, 8);
            g.setColor(new Color(238, 239, 244));
            drawCenteredIn(g, "Down", drawX, drawY + spriteH / 2, spriteW);
        }
    }

    private void drawBattlePartyCard(Graphics2D g, Battle battle, Actor actor, int panelX, int panelY, int panelW, int panelH) {
        int[] slot = battlePartyPosition(battle, actor, panelX, panelY, panelW, panelH);
        boolean active = actor == battle.activeActor();
        boolean selected = battle.hasExplicitPartyTarget() && actor == battle.selectedPartyMember();
        int spriteW = 150, spriteH = 194, cardW = 178, cardH = 86;
        int cardX = slot[0] + (spriteW - cardW) / 2;
        int cardY = slot[1] + spriteH - 4;
        g.setColor(new Color(12, 14, 22, 205));
        g.fillRoundRect(cardX, cardY, cardW, cardH, 8, 8);
        g.setColor(selected ? new Color(132, 190, 255) : active ? new Color(242, 208, 124) : new Color(86, 98, 128));
        g.setStroke(new BasicStroke(selected || active ? 3f : 1.5f));
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
    }

    private void drawBattleEnemySprite(Graphics2D g, Battle battle, Actor foe, int panelX, int panelY, int panelW, int panelH) {
        int[] slot = battleEnemyPosition(battle, foe, panelX, panelY, panelW, panelH);
        int index = Math.max(0, battle.enemies().indexOf(foe));
        boolean selected = foe == battle.selectedEnemy();
        double scale = battleEnemyScale(battle, foe);
        int baseSpriteW = BattleFormation.enemySize(battle.enemies().size());
        int baseSpriteH = baseSpriteW;
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
        BattleActorPose pose = battleRenderer.actorPose(battle, foe, true);
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
        battleRenderer.drawHitFlash(g, battleActorImage(battle, foe, foe.sprite, renderedAction, spriteW, renderH),
                drawX, renderY, spriteW, renderH, pose, battle.hitFlash(foe));
        battleVfxRenderer.drawActorStatuses(g, battle, foe, drawX, drawY, spriteW, spriteH);
        if (selected && foe.alive()) {
            drawBattleEnemyTargetMarker(g, drawX, drawY, spriteW, spriteH);
        } else if (battle.isCasting(foe)) {
            g.setColor(new Color(200, 154, 255, 170));
            g.setStroke(new BasicStroke(3f));
            int pulse = 8 + (frame % 12);
            g.drawOval(drawX + 24 - pulse / 2, drawY + spriteH - 38 - pulse / 3, spriteW - 48 + pulse, 30 + pulse / 2);
        }
        String telegraph = battle.sourceTelegraphLabel(foe);
        if (!telegraph.isBlank()) {
            drawBattleIncomingTelegraph(g, telegraph, battle.sourceTelegraphDanger(foe), drawX, drawY, spriteW);
        }
    }

    private void drawBattleEnemyCard(Graphics2D g, Battle battle, Actor foe, int panelX, int panelY, int panelW, int panelH) {
        int[] slot = battleEnemyPosition(battle, foe, panelX, panelY, panelW, panelH);
        boolean selected = foe == battle.selectedEnemy();
        double scale = battleEnemyScale(battle, foe);
        int baseSpriteW = BattleFormation.enemySize(battle.enemies().size());
        int baseSpriteH = baseSpriteW;
        int spriteW = Math.max(120, (int) Math.round(baseSpriteW * scale));
        int spriteH = Math.max(120, (int) Math.round(baseSpriteH * scale));
        int cardW = 220;
        int cardH = 78;
        String animationAction = battleActorAnimationAction(battle, foe);
        boolean stripAnimating = battleActorUsesStrip(foe, foe.sprite, animationAction);
        int renderExtraH = stripAnimating && "cast".equals(animationAction) ? (int) Math.round(70 * scale) : 0;
        int lunge = stripAnimating ? 0 : battle.enemyLunge(foe);
        int shake = foe.alive() && !stripAnimating ? battle.monsterOffset : 0;
        int bob = foe.alive() && !stripAnimating ? (int) Math.round(Math.sin((frame + Math.max(0, battle.enemies().indexOf(foe)) * 10) * 0.16) * 3.0) : 0;
        BattleActorPose pose = battleRenderer.actorPose(battle, foe, true);
        int drawX = slot[0] + (baseSpriteW - spriteW) / 2 + shake - lunge + pose.xOffset();
        int drawY = slot[1] + (baseSpriteH - spriteH) + bob - (stripAnimating ? 0 : battle.castOffset(foe)) + pose.yOffset();
        drawY -= renderExtraH;
        int cardX = slot[0] + (baseSpriteW - cardW) / 2;
        int cardY = slot[1] + baseSpriteH - 4;
        g.setColor(selected ? new Color(40, 31, 16, 222) : new Color(12, 14, 22, 205));
        g.fillRoundRect(cardX, cardY, cardW, cardH, 8, 8);
        g.setColor(selected ? new Color(255, 220, 150) : new Color(86, 88, 102));
        g.setStroke(new BasicStroke(selected ? 3f : 1.5f));
        g.drawRoundRect(cardX, cardY, cardW, cardH, 8, 8);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(selected ? new Color(255, 244, 202) : new Color(205, 207, 216));
        drawCenteredIn(g, foe.name, cardX, cardY + 22, cardW);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(selected ? new Color(255, 220, 150) : new Color(178, 182, 194));
        drawCenteredIn(g, battle.enemyTraitLabel(foe), cardX, cardY + 38, cardW);
        smallBar(g, cardX + 14, cardY + 51, foe.hp, foe.maxHp, new Color(190, 76, 82), "HP");
        drawStatusIcons(g, battle, foe, cardX + 126, cardY + 51);
        Rectangle hoverBounds = new Rectangle(drawX - 18, drawY - 10, spriteW + 36, spriteH + cardH + 22);
        tooltipZones.add(new TooltipZone(
                hoverBounds,
                foe.name,
                monsterTooltip(battle, foe),
                "sprite:" + battle.monsterSpecFor(foe).sprite()
        ));
    }

    private void drawBattleIncomingTelegraph(Graphics2D g, String label, int danger, int drawX, int drawY, int spriteW) {
        if (label == null || label.isBlank()) {
            return;
        }
        Graphics2D warn = (Graphics2D) g.create();
        int boxW = Math.max(96, Math.min(180, spriteW));
        int boxH = 26;
        int boxX = drawX + (spriteW - boxW) / 2;
        int boxY = Math.max(20, drawY - 34);
        Color edge = danger >= 2 ? new Color(255, 118, 94) : new Color(255, 201, 92);
        warn.setColor(new Color(18, 12, 10, 218));
        warn.fillRoundRect(boxX, boxY, boxW, boxH, 8, 8);
        warn.setStroke(new BasicStroke(2f));
        warn.setColor(edge);
        warn.drawRoundRect(boxX, boxY, boxW, boxH, 8, 8);
        warn.setFont(new Font("SansSerif", Font.BOLD, 12));
        warn.setColor(new Color(255, 242, 211));
        drawCenteredIn(warn, label, boxX, boxY + 17, boxW);
        warn.dispose();
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
        boolean damage = animation.actionKind() == null || animation.actionKind() == Ability.AbilityKind.DAMAGE;
        int recipient = animation.targets.indexOf(actor);
        if (actor != animation.source && recipient >= 0 && damage && animation.collisionProgress(recipient) >= 0) {
            return "hit";
        }
        if (actor != animation.source) {
            return "idle";
        }
        String kind = animation.effectKind == null ? "strike" : animation.effectKind;
        ClassAbilityVfx.Profile profile = animation.visualProfile();
        if (profile != null) {
            if (profile.kind() == Ability.AbilityKind.DEFEND) return "defend";
            if (profile.kind() == Ability.AbilityKind.HEAL) return "cast";
            if (profile.family() == ClassAbilityVfx.Family.PIERCE) return "shoot";
            return profile.melee() ? "attack" : "cast";
        }
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
        if (battle.isEliteEnemy(foe) || battle.isBossEnemy(foe)) {
            body.append("\nTrait: ").append(battle.enemyTraitLabel(foe)).append(".");
        }
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
        if (battle.isBossEnemy(foe)) {
            abilities.addAll(MonsterAbilities.bossAbilitiesFor(battle.monsterSpecFor(foe), battle.bossPhase(foe)));
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
        battleVfxRenderer.drawBattleEffect(g, battle, panelX, panelY, panelW, panelH);
    }

    private String effectSpriteName(String kind) {
        return battleVfxRenderer.effectSpriteName(kind);
    }

    private String effectSoundName(String kind) {
        return battleVfxRenderer.effectSoundName(kind);
    }

    private double battleEnemyScale(Battle battle, Actor actor) {
        double scale = enemySpriteScale(battle.monsterSpecFor(actor));
        return battle.enemies().size() >= 3 ? Math.min(1.08, scale) : scale;
    }

    private int[] battleActorCenter(Battle battle, Actor actor, int panelX, int panelY, int panelW, int panelH) {
        boolean enemy = battle.enemies().contains(actor);
        int[] slot = enemy ? battleEnemyPosition(battle, actor, panelX, panelY, panelW, panelH)
                : battlePartyPosition(battle, actor, panelX, panelY, panelW, panelH);
        int base = enemy ? BattleFormation.enemySize(battle.enemies().size()) : 150;
        int height = enemy ? Math.max(120, (int) Math.round(base * battleEnemyScale(battle, actor))) : 194;
        int foot = slot[1] + (enemy ? base : height);
        if (actor == null) return new int[]{slot[0] + base / 2, foot - height / 2};
        String action = battleActorAnimationAction(battle, actor);
        boolean strip = battleActorUsesStrip(actor, enemy ? actor.sprite : battleActorSprite(actor), action);
        BattleActorPose pose = battleRenderer.actorPose(battle, actor, enemy);
        int index = Math.max(0, (enemy ? battle.enemies() : battle.partyMembers()).indexOf(actor));
        int bob = actor.alive() && !strip ? (int) Math.round(Math.sin((frame + index * (enemy ? 10 : 8)) * (enemy ? 0.16 : 0.18)) * 3.0) : 0;
        int dx = 0;
        if (!strip) {
            if (enemy) dx = (actor.alive() ? battle.monsterOffset : 0) - battle.enemyLunge(actor);
            else if (actor == battle.activeActor()) dx = battle.playerLunge + battle.playerOffset;
        }
        return new int[]{slot[0] + base / 2 + dx + pose.xOffset(),
                foot - (int) Math.round(height * 0.48) + bob + pose.yOffset() - (strip ? 0 : battle.castOffset(actor))};
    }

    private int[] battlePartyPosition(Battle battle, Actor actor, int panelX, int panelY, int panelW, int panelH) {
        return BattleFormation.position(false, battle.partyMembers().indexOf(actor), battle.partyMembers().size(),
                panelX, panelY, panelW, panelH);
    }

    private int[] battleEnemyPosition(Battle battle, Actor actor, int panelX, int panelY, int panelW, int panelH) {
        return BattleFormation.position(true, battle.enemies().indexOf(actor), battle.enemies().size(),
                panelX, panelY, panelW, panelH);
    }

    private void drawDialog(Graphics2D g) {
        dialogueRenderer.drawDialog(g);
    }

    private List<DialogOption> dialogOptions(Npc npc, Quest quest) {
        return dialogueRenderer.dialogOptions(npc, quest);
    }

    private boolean activeDialogWillOpenShop(Npc npc) {
        return dialogueRenderer.activeDialogWillOpenShop(npc);
    }

    private String npcTooltip(Npc npc) {
        return dialogueRenderer.npcTooltip(npc);
    }

    private String battleActorSprite(Actor actor) {
        return dialogueRenderer.battleActorSprite(actor);
    }

    private Color playerDialogueAccent() {
        return dialogueRenderer.playerDialogueAccent();
    }

    private void drawQuestLog(Graphics2D g) {
        questLogRenderer.drawQuestLog(g);
    }

    List<Quest> questLogEntries(boolean completed) {
        return questLogRenderer.questLogEntries(completed);
    }

    Quest selectedQuestLogQuest(List<Quest> quests) {
        return questLogRenderer.selectedQuestLogQuest(quests);
    }

    void toggleTrackedQuest(Quest quest) {
        questLogRenderer.toggleTrackedQuest(quest);
    }

    private void drawTrackedQuestHud(Graphics2D g) {
        questLogRenderer.drawTrackedQuestHud(g);
    }

    private void drawTravelPartyHud(Graphics2D g, int x, int y, int w, int h) {
        questLogRenderer.drawTravelPartyHud(g, x, y, w, h);
    }

    private String dialoguePortraitSprite(Actor actor) {
        return questLogRenderer.dialoguePortraitSprite(actor);
    }

    private void drawSkillTree(Graphics2D g) {
        characterRenderer.drawSkillTree(g);
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
        inventoryRenderer.drawInventory(g);
    }

    List<CraftingSystem.Recipe> displayedCraftingRecipes() {
        return inventoryRenderer.displayedCraftingRecipes();
    }

    private int craftingRecipeVisibleSlots() {
        return inventoryRenderer.craftingRecipeVisibleSlots();
    }

    private void drawCrafting(Graphics2D g) {
        inventoryRenderer.drawCrafting(g);
    }

    private String professionSummary(Actor actor) {
        List<String> parts = new ArrayList<>();
        for (Profession profession : Profession.ALL) {
            parts.add(profession.label() + " " + actor.professionLevel(profession.id()));
        }
        return String.join(" | ", parts);
    }

    private void drawPartyOverview(Graphics2D g) {
        characterRenderer.drawPartyOverview(g);
    }

    private void drawVillageSidebar(Graphics2D g, int left) {
        villageSidebarRenderer.drawVillageSidebar(g, left);
    }

    boolean handleVillageSearchKey(KeyEvent event) {
        return villageSidebarRenderer.handleVillageSearchKey(event);
    }

    boolean villageSearchFocused() {
        return villageSidebarRenderer.searchFocused();
    }

    private void drawVillageManager(Graphics2D g) {
        villageOverlayRenderer.drawVillageManager(g);
    }

    private void drawBuildingAssignment(Graphics2D g) {
        villageOverlayRenderer.drawBuildingAssignment(g);
    }

    private String buildingPreviewSprite(VillageManager.BuildingPlan plan) {
        return villageOverlayRenderer.buildingPreviewSprite(plan);
    }

    private String villageCostDisplay(VillageManager.VillageCost cost) {
        return villageOverlayRenderer.villageCostDisplay(cost);
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
        shopRenderer.drawShop(g);
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
        List<Rectangle> placeLabels = new ArrayList<>();
        for (WorldMap.SettlementSite settlement : state.world.settlementSites()) {
            if (worldMapVisible(worldMapViewport, settlement.x(), settlement.y(), 8.0)) {
                int dx = settlement.x() > worldMapViewport.worldX() + worldMapViewport.worldW() * 0.68 ? -78 : 12;
                WorldMap.Kingdom kingdom = kingdomById(settlement.kingdomId());
                boolean playerSettlement = state.world.isPlayerSettlement(settlement);
                Rectangle markerBounds = WorldMapOverlayRenderer.drawSettlementMarker(
                        g, worldMapViewport, settlement, kingdom, playerSettlement, dx, -9);
                placeLabels.add(markerBounds);
                Color markerColor = playerSettlement ? new Color(255, 226, 128)
                        : kingdom == null ? new Color(245, 214, 117) : new Color(kingdom.colorRgb());
                tooltipZones.add(new TooltipZone(markerBounds, settlement.label(),
                        settlementTooltip(settlement, kingdom), null, markerColor));
            }
        }
        for (WorldMap.CampaignMarker site : state.world.campaignMarkers()) {
            if (!worldMapVisible(worldMapViewport, site.x(), site.y(), 0.0)) continue;
            Rectangle bounds = WorldMapOverlayRenderer.drawCampaignMarker(g, worldMapViewport, site, placeLabels);
            var place = StoryLocationCatalog.byId(site.id());
            tooltipZones.add(new TooltipZone(bounds, site.label(),
                    place.purpose() + "\n" + place.landmark() + "\n" + place.route()));
        }
        long objectivesStarted = renderMetrics.start();
        List<GameState.QuestObjective> activeObjectives = state.activeQuestObjectives();
        renderMetrics.record("questObjectives", objectivesStarted);
        renderMetrics.sample("questObjectives.count", activeObjectives.size());
        for (GameState.QuestObjective objective : activeObjectives) {
            TilePoint marker = worldMapObjectivePosition(objective);
            if (marker != null && showQuestObjective(objective)
                    && worldMapVisible(worldMapViewport, marker.x(), marker.y(), 6.0)) {
                WorldMapOverlayRenderer.drawQuestMarker(g, worldMapViewport, objective.kind(), objective.markerLabel(),
                        marker, objectiveColor(objective, 255), objective.mainStory(), objective.dungeonHazard());
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
        if (objective.worldMapX() >= 0 && objective.worldMapY() >= 0) {
            return new TilePoint(objective.worldMapX(), objective.worldMapY());
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
        int w = 940;
        int h = 640;
        int x = overlay ? gameAreaCenteredX(w) : centeredX(w);
        int y = centeredY(h);
        drawOverlayBase(g, x, y, w, h);
        g.setFont(new Font("Serif", Font.BOLD, 32));
        g.setColor(new Color(244, 213, 141));
        drawCenteredIn(g, "Settings", x, y + 46, w);

        int tabX = x + 52;
        for (SettingsCategory category : SettingsCategory.values()) {
            boolean selected = settingsCategory == category;
            actionButton(g, tabX, y + 72, 160, 38, category.label, () -> {
                settingsCategory = category;
                activeSlider = null;
            }, selected ? new Color(74, 66, 93) : new Color(35, 39, 54),
                    selected ? new Color(164, 143, 197) : new Color(86, 98, 128), true);
            tabX += 168;
        }

        int left = x + 52;
        switch (settingsCategory) {
            case GRAPHICS -> {
                drawSettingsHeading(g, left, y + 154, "Graphics Quality");
                drawChoiceRow(g, left, y + 178, "Render", renderQuality().label, this::previousRenderQuality, this::nextRenderQuality);
                drawChoiceRow(g, left, y + 230, "Weather", weatherQuality().label, this::previousWeatherQuality, this::nextWeatherQuality);
            }
            case COMBAT -> {
                drawSettingsHeading(g, left, y + 154, "Monster Spawn Ratios");
                drawChanceRow(g, left, y + 178, "Wild", state.config.wildEncounterChance, "wild");
                drawChanceRow(g, left, y + 222, "Dungeon", state.config.dungeonEncounterChance, "dungeon");
                drawChanceRow(g, left, y + 266, "Dungeon Entrance", state.config.dungeonEntranceEncounterChance, "dungeon_entrance");

                drawSettingsHeading(g, left, y + 342, "Monster Group Ratios");
                drawChanceRow(g, left, y + 366, "Two Monsters", state.config.twoMonsterChance, "two_monsters");
                drawChanceRow(g, left, y + 410, "Three Monsters", state.config.threeMonsterChance, "three_monsters");

                drawSettingsHeading(g, left, y + 486, "Monster Difficulty");
                drawMonsterLevelScalingRow(g, left, y + 510);
                drawSettingsHeading(g, x + 540, y + 154, "Battle Animations");
                drawAnimationSpeedRow(g, x + 540, y + 178);
            }
            case GAMEPLAY -> {
                drawSettingsHeading(g, left, y + 154, "Movement & Camera");
                drawChoiceRow(g, left, y + 178, "Camera", cameraMode().label, this::previousCameraMode, this::nextCameraMode);
                drawChoiceRow(g, left, y + 230, "Speed", movementSpeed().label, this::previousMovementSpeed, this::nextMovementSpeed);
                drawCameraSmoothingRow(g, left, y + 282);
                drawToggleRow(g, left, y + 334, "Look-Ahead", state.config.cameraLookAhead, this::toggleCameraLookAhead);
            }
            case AUDIO -> {
                drawSettingsHeading(g, left, y + 154, "Volume");
                drawVolumeRow(g, left, y + 178, "Master", state.config.masterVolume, "master");
                drawVolumeRow(g, left, y + 230, "Music", state.config.musicVolume, "music");
                drawVolumeRow(g, left, y + 282, "SFX", state.config.sfxVolume, "sfx");
                g.setFont(new Font("SansSerif", Font.PLAIN, 13));
                g.setColor(new Color(165, 174, 193));
                wrap(g, "Music blends between biome themes over time, with softer ambient variants during longer exploration.", left, y + 354, 490, 18);
            }
            case DEBUG -> {
                drawSettingsHeading(g, left, y + 154, "Creative Tools");
                drawToggleRow(g, left, y + 178, "Creative Build", state.config.creativeBuildMode, this::toggleCreativeBuildMode);
                drawToggleRow(g, left, y + 230, "Editor Button", state.config.showMapEditorButton, this::toggleMapEditorButton);
                drawToggleRow(g, left, y + 282, "Creative Crafting", state.config.creativeCraftingMode, this::toggleCreativeCraftingMode);
            }
        }

        actionButton(g, x + w - 170, y + h - 58, 120, 34, "Back", state::closeSettings, new Color(48, 55, 70), new Color(89, 102, 125), true);
    }

    private void drawSettingsHeading(Graphics2D g, int x, int y, String label) {
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.setColor(new Color(230, 225, 206));
        g.drawString(label, x, y);
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

    private void drawAnimationSpeedRow(Graphics2D g, int x, int y) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(220, 224, 232));
        g.drawString("Animation speed", x, y + 22);
        Rectangle track = new Rectangle(x + 142, y + 8, 150, 10);
        Rectangle bounds = new Rectangle(x + 136, y - 2, 162, 32);
        registerSlider(bounds, track, SliderKind.ANIMATION, "combat_speed");
        drawSliderTrack(g, track, (state.config.combatAnimationSpeed - 0.5) / 2.5,
                sliderHovered(bounds), sliderActive(SliderKind.ANIMATION, "combat_speed"), 11);
        g.setColor(new Color(220, 224, 232));
        g.drawString(String.format(java.util.Locale.ROOT, "%.1fx", state.config.combatAnimationSpeed), x + 306, y + 22);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(165, 174, 193));
        g.drawString("0.5x slow  /  1x normal  /  3x fast", x, y + 43);
    }

    private void setAnimationSpeed(double speed) {
        double value = GameConfig.clampCombatAnimationSpeed(Math.round(speed * 10) / 10.0);
        if (value == state.config.combatAnimationSpeed) return;
        state.config.combatAnimationSpeed = value;
        if (state.battle != null) state.battle.setAnimationSpeed(value);
        saveSettings();
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
        saveMenuRenderer.drawSaveMenu(g, overlay);
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

    void selectLoadFolder(String characterId) {
        selectedLoadCharacterId = characterId == null ? "" : characterId;
        saveListScroll = 0;
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

    private int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
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
        ensureFreePlayerPosition();
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
        if (state.mode != GameMode.EXPLORE && state.mode != GameMode.DEFENSE) {
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
                ensureFreePlayerPosition();
                playerPathDestination = null;
                return;
            }
        }
        if (playerPathExhausted()) {
            playerPathDestination = null;
            return;
        }
        ensureFreePlayerPosition();
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
        setPlayerFacing(dx, dy);
        boolean moved = state.move(dx, dy);
        if (moved && state.currentMapId.equals(mapBefore)
                && Math.abs(state.playerX - fromX) + Math.abs(state.playerY - fromY) == 1) {
            syncFreePlayerPositionToState();
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
        pendingReadTile = null;
        pendingCraftTile = null;
        pendingTalkNpc = null;
        pendingTalkPartyMember = null;
        pendingTalkPartyTile = null;
        pendingPortalTile = null;
        pendingEnterBuilding = null;
        clearPlayerPath();
        clearQueuedMove();
        if (state.mode == GameMode.EXPLORE) {
            ensureFreePlayerPosition();
            int[] heldDirection = heldMoveDirection();
            setPlayerFacing(heldDirection[0] == 0 && heldDirection[1] == 0 ? direction[0] : heldDirection[0],
                    heldDirection[0] == 0 && heldDirection[1] == 0 ? direction[1] : heldDirection[1]);
            return true;
        }
        startPlayerMove(direction[0], direction[1]);
        return true;
    }

    boolean handleDefenseMovementKeyPressed(int code) {
        int[] direction = movementDirectionForKey(code);
        if (direction == null) {
            return false;
        }
        setMovementKeyHeld(code, true);
        lastHeldMoveDx = direction[0];
        lastHeldMoveDy = direction[1];
        int[] heldDirection = heldMoveDirection();
        setPlayerFacing(heldDirection[0] == 0 && heldDirection[1] == 0 ? direction[0] : heldDirection[0],
                heldDirection[0] == 0 && heldDirection[1] == 0 ? direction[1] : heldDirection[1]);
        clearPlayerPath();
        clearQueuedMove();
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

    boolean handleDefenseMovementKeyReleased(int code) {
        return handleMovementKeyReleased(code);
    }

    private void tickDefensePlayerMovement() {
        if (state.defenseRaid == null || !state.defenseRaid.started() || state.defenseRaid.finished()) {
            clearPlayerPath();
            clearQueuedMove();
            return;
        }
        int[] direction = heldMoveDirection();
        if (direction[0] == 0 && direction[1] == 0) {
            return;
        }
        setPlayerFacing(direction[0], direction[1]);
        state.moveDefensePlayer(direction[0], direction[1]);
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
        syncFreePlayerPositionToState();
        followerTrailMapId = state.currentMapId;
        playerTrail.clear();
        followerVisuals.clear();
        visiblePartyFollowers.clear();
        playerLocomotionActive = false;
        playerLocomotionStartFrame = Integer.MIN_VALUE / 4;
        playerLocomotionStopFrame = Integer.MIN_VALUE / 4;
        playerIdleStartFrame = frame;
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

    private void toggleCreativeCraftingMode() {
        state.config.creativeCraftingMode = !state.config.creativeCraftingMode;
        state.status = state.config.creativeCraftingMode
                ? "Creative crafting enabled: instant, free crafts anywhere, no profession gates or XP."
                : "Creative crafting disabled. Normal crafting requirements restored.";
        saveSettings();
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

    private void previousRenderQuality() {
        cycleRenderQuality(-1);
    }

    private void nextRenderQuality() {
        cycleRenderQuality(1);
    }

    private void cycleRenderQuality(int delta) {
        RenderQuality[] qualities = RenderQuality.values();
        int index = 0;
        for (int i = 0; i < qualities.length; i++) {
            if (qualities[i] == renderQuality()) {
                index = i;
                break;
            }
        }
        RenderQuality next = qualities[Math.floorMod(index + delta, qualities.length)];
        state.config.renderQuality = next.key;
        state.status = "Render quality: " + next.label + ".";
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
        timer.stop();
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
            case ANIMATION -> setAnimationSpeed(0.5 + fraction * 2.5);
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
            if (SwingUtilities.isLeftMouseButton(event) && skipBattleIntroOnClick()) {
                return;
            }
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
            if (SwingUtilities.isLeftMouseButton(event)) {
                if (state.mode == GameMode.SHOP) shopRenderer.press(hoverPoint);
                if (state.mode == GameMode.INVENTORY) inventoryRenderer.browser.focusAt(hoverPoint);
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
            if (state.mode == GameMode.SHOP) shopRenderer.move(hoverPoint);
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
            if (shopRenderer.release(point)) {
                pressedButtonBounds = null; pressedButtonLabel = null;
                suppressNextClick = true; repaint(); return;
            }
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
                if (villageSidebarRenderer.updateSearchFocus(point)) {
                    repaint();
                    return;
                }
            } else {
                villageSidebarRenderer.clearSearchFocus();
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
                inventoryRenderer.browser.wheel(logicalPoint(event), event.getWheelRotation());
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
                shopRenderer.stockBrowser.wheel(logicalPoint(event), event.getWheelRotation());
                shopRenderer.packBrowser.wheel(logicalPoint(event), event.getWheelRotation());
                repaint();
                return;
            }
            if (state.mode == GameMode.CHEST) return;
            if (state.mode == GameMode.SAVE_MENU) {
                saveListScroll = Math.max(0, saveListScroll + event.getWheelRotation());
                repaint();
                return;
            }
            if (state.mode == GameMode.VILLAGE) {
                villageSidebarRenderer.scrollList(event.getWheelRotation());
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
            for (WorldProp prop : state.world.propsAt(state.currentMapId, targetX, targetY)) {
                if (ChestSystem.isChest(prop)
                        && Math.max(Math.abs(prop.x() - state.playerX), Math.abs(prop.y() - state.playerY)) <= 1) {
                    clearPlayerPath();
                    clearQueuedMove();
                    state.openChest(prop);
                    repaint();
                    return;
                }
            }
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
