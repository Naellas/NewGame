package com.alderfall.game;

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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
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
            Map.entry('P', "zone_water"),
            Map.entry('~', "zone_water"),
            Map.entry('m', "zone_mountains"),
            Map.entry('q', "zone_mountains"),
            Map.entry('B', "zone_water"),
            Map.entry('w', "zone_water"),
            Map.entry('c', "town_village"),
            Map.entry('u', "town_village"),
            Map.entry('d', "dungeon_crypt")
    );
    private static final Map<String, List<String>> MUSIC_PALETTES = Map.ofEntries(
            Map.entry("zone_grasslands", List.of("zone_grasslands", "zone_grasslands_ambient")),
            Map.entry("zone_forest", List.of("zone_forest", "zone_forest_ambient")),
            Map.entry("zone_desert", List.of("zone_desert", "zone_desert_ambient")),
            Map.entry("zone_marsh", List.of("zone_marsh", "zone_marsh_ambient")),
            Map.entry("zone_mountains", List.of("zone_mountains", "zone_mountains_ambient")),
            Map.entry("zone_tundra", List.of("zone_tundra", "zone_tundra_ambient")),
            Map.entry("zone_badlands", List.of("zone_badlands", "zone_badlands_ambient")),
            Map.entry("zone_water", List.of("zone_water", "zone_water_ambient")),
            Map.entry("town_village", List.of("town_village")),
            Map.entry("dungeon_crypt", List.of("dungeon_crypt")),
            Map.entry("battle_standard", List.of("battle_standard")),
            Map.entry("battle_boss", List.of("battle_boss"))
    );
    private static final Set<GameMode> WORLD_MUSIC_MODES = Set.of(
            GameMode.EXPLORE,
            GameMode.DIALOG,
            GameMode.QUEST_LOG,
            GameMode.SKILLS,
            GameMode.INVENTORY,
            GameMode.CRAFTING,
            GameMode.PARTY,
            GameMode.VILLAGE,
            GameMode.BUILDING_ASSIGNMENT,
            GameMode.SETTLEMENT_BOARD,
            GameMode.SHOP,
            GameMode.WORLD_MAP
    );
    private static final Set<String> BOSS_MUSIC_KEYS = new HashSet<>(Set.of(
            "acid_broodmother",
            "bandit_captain",
            "bone_knight",
            "crypt_revenant",
            "elder_wraith",
            "elder_dragon",
            "fire_giant",
            "frost_troll",
            "goblin_king",
            "goblin_warlord",
            "hill_giant",
            "ice_golem",
            "orc_champion",
            "stone_giant",
            "swamp_troll"
    ));
    private static final Set<String> PHYSICAL_CLASS_NAMES = Set.of(
            "Knight",
            "Rogue",
            "Ironwall",
            "Bladedancer",
            "Veilrunner",
            "Sunwarden",
            "Stonebreaker",
            "Nightblade"
    );
    private static final Set<String> RANGED_CLASS_NAMES = Set.of("Ranger");
    private static final Set<String> MAGIC_CLASS_NAMES = Set.of(
            "Mage",
            "Cleric",
            "Battle Medic",
            "Wildspeaker",
            "Thornbinder",
            "Grovekeeper"
    );
    private static final int PLAYER_MOVE_FRAMES = 6;
    private static final int PLAYER_WALK_FRAMES_PER_TILE = 9;
    private static final int WALK_ANIMATION_FRAMES = 18;
    private static final int BIOME_MUSIC_FULL_BLEND_TICKS = Math.max(1, 18_000 / GameConfig.FPS_MS);
    private static final int BIOME_TUNE_CROSSFADE_TICKS = Math.max(1, 12_000 / GameConfig.FPS_MS);
    private static final int BIOME_TUNE_ROTATION_TICKS = Math.max(1, 50_000 / GameConfig.FPS_MS);
    private static final int FOG_RENDER_SCALE = 3;
    private static final double WEATHER_TRANSITION_STEP = Math.min(1.0, GameConfig.FPS_MS / 8000.0);
    private static final double WEATHER_VISIBILITY_EPSILON = 0.01;
    private static final Color WORLD_VOID_COLOR = new Color(5, 6, 9);
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
    private static final Comparator<WorldProp> WORLD_PROP_DRAW_ORDER = Comparator
            .comparingInt(WorldProp::y)
            .thenComparingInt(WorldProp::x);
    private static final DateTimeFormatter SAVE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());
    private final GameState state;
    private final AssetStore assets;
    private final SaveSystem saves;
    private final MusicManager music;
    private final SoundManager sounds;
    private final Map<String, BufferedImage> particleSprites = new HashMap<>();
    private final EnumMap<WeatherCondition, Double> weatherIntensities = new EnumMap<>(WeatherCondition.class);
    private final Path javaRoot;
    private final Timer timer;
    private final List<UiButton> buttons = new ArrayList<>();
    private final List<UiSlider> sliders = new ArrayList<>();
    private final List<TooltipZone> tooltipZones = new ArrayList<>();
    private int frame;
    private int lastCamX;
    private int lastCamY;
    private double lastCameraX;
    private double lastCameraY;
    private int lastVisibleCols;
    private int lastVisibleRows;
    private BufferedImage backBuffer;
    private BufferedImage fogBuffer;
    private BufferedImage weatherLayerBuffer;
    private double renderScaleX = 1.0;
    private double renderScaleY = 1.0;
    private int renderWidth = GameConfig.WIDTH;
    private int renderOffsetX;
    private int renderOffsetY;
    private String playerMoveMapId = WorldMap.OVERWORLD_ID;
    private int playerMoveStartFrame = -PLAYER_MOVE_FRAMES;
    private int playerMoveFromX = WorldMap.START_POSITION.x();
    private int playerMoveFromY = WorldMap.START_POSITION.y();
    private int playerWalkAnimationTileStart;
    private int playerWalkAnimationTiles;
    private int playerFacingDx;
    private int playerFacingDy = 1;
    private int queuedMoveDx;
    private int queuedMoveDy;
    private final List<TilePoint> playerPath = new ArrayList<>();
    private final List<WorldLight> activeWorldLights = new ArrayList<>();
    private final List<WorldProp> nearbyWorldProps = new ArrayList<>();
    private final List<WorldProp> visibleWorldProps = new ArrayList<>();
    private TilePoint playerPathDestination;
    private int lastBattleEffectTimer;
    private String lastBattleEffectSignature = "";
    private String worldMusicKey = "";
    private String worldMusicTrack = "";
    private String previousWorldMusicTrack = "";
    private int worldMusicDwellTicks;
    private int worldMusicTrackTicks;
    private int worldMusicPaletteIndex;
    private boolean worldMusicBiomeChanging;
    private Runnable fullscreenToggle = () -> {
    };
    private Point hoverPoint;
    private int inventoryItemScroll;
    private int craftingRecipeScroll;
    private int shopItemScroll;
    private int saveListScroll;
    private int villageListScroll;
    private int lastVillageTab = -1;
    private String lastVillagePropCategory = "";
    private boolean saveListCurrentCharacterOnly = true;
    private String pendingOverwriteSaveId = "";
    private String pendingOverwriteSaveName = "";
    private final List<InventoryDragZone> inventoryDragZones = new ArrayList<>();
    private final List<InventoryDropZone> inventoryDropZones = new ArrayList<>();
    private InventoryDrag inventoryDrag;
    private Point inventoryDragStart;
    private Point inventoryDragPoint;
    private boolean inventoryDragMoved;
    private boolean suppressNextClick;
    private Rectangle pressedButtonBounds;
    private String pressedButtonLabel;
    private UiSlider activeSlider;
    private int partySkillScroll;
    private int skillTreeScroll;
    private SkillTab activeSkillTab = SkillTab.SURVIVAL;
    private String activeProfessionId = Profession.WOODCUTTING.id();
    private final List<PlacementPulse> placementPulses = new ArrayList<>();

    private enum SliderKind {
        CHANCE,
        VOLUME,
        ZOOM
    }

    private enum SkillTab {
        SURVIVAL("Survival"),
        CLASS("Class"),
        PROFESSIONS("Professions");

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

    public GamePanel(Path javaRoot) {
        this.javaRoot = javaRoot;
        this.state = new GameState(GameConfig.loadWithSettings(javaRoot));
        this.assets = new AssetStore(javaRoot.resolve("assets"));
        this.saves = new SaveSystem(javaRoot);
        this.music = new MusicManager(javaRoot.resolve("assets").resolve("music"));
        this.sounds = new SoundManager(javaRoot.resolve("assets").resolve("sfx"));
        this.weatherIntensities.put(WeatherCondition.CLEAR, 1.0);
        setPreferredSize(new Dimension(GameConfig.WIDTH, GameConfig.HEIGHT));
        setBackground(new Color(15, 17, 24));
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        addKeyListener(new Keys());
        Mouse mouse = new Mouse();
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);
        timer = new Timer(GameConfig.FPS_MS, event -> {
            frame++;
            state.tickWorld();
            tickWeatherTransition();
            tickPlayerPath();
            if (state.mode == GameMode.BATTLE && state.battle != null) {
                state.tickBattle();
            }
            updateMusic();
            updateBattleSoundEffects();
            repaint();
        });
        timer.start();
    }

    private void tickWeatherTransition() {
        WeatherCondition target = state.currentWeather();
        for (WeatherCondition condition : WeatherCondition.values()) {
            double current = weatherIntensities.getOrDefault(condition, 0.0);
            double desired = condition == target ? 1.0 : 0.0;
            if (Math.abs(current - desired) <= WEATHER_TRANSITION_STEP) {
                current = desired;
            } else if (current < desired) {
                current += WEATHER_TRANSITION_STEP;
            } else {
                current -= WEATHER_TRANSITION_STEP;
            }

            if (current <= 0.0001) {
                weatherIntensities.remove(condition);
            } else {
                weatherIntensities.put(condition, current);
            }
        }
    }

    private double weatherIntensity(WeatherCondition condition) {
        return smoothStep(weatherIntensities.getOrDefault(condition, 0.0));
    }

    public void setFullscreenToggle(Runnable fullscreenToggle) {
        this.fullscreenToggle = fullscreenToggle == null ? () -> {
        } : fullscreenToggle;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        updateViewportTransform();
        if (backBuffer == null || backBuffer.getWidth() != viewWidth() || backBuffer.getHeight() != viewHeight()) {
            backBuffer = new BufferedImage(viewWidth(), viewHeight(), BufferedImage.TYPE_INT_ARGB);
        }

        buttons.clear();
        sliders.clear();
        tooltipZones.clear();
        Graphics2D bufferGraphics = backBuffer.createGraphics();
        bufferGraphics.setColor(getBackground());
        bufferGraphics.fillRect(0, 0, viewWidth(), viewHeight());
        renderGame(bufferGraphics);
        bufferGraphics.dispose();

        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        int scaledWidth = (int) Math.round(viewWidth() * renderScaleX);
        int scaledHeight = (int) Math.round(viewHeight() * renderScaleY);
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
        } else if (state.mode == GameMode.STORY_INTRO) {
            drawStoryIntro(g);
        } else if (state.mode == GameMode.SETTINGS && state.settingsReturnMode == GameMode.MAIN_MENU) {
            drawTitleBackground(g, false);
            drawSettingsMenu(g, false);
        } else if (state.mode == GameMode.SAVE_MENU && state.saveMenuReturnMode == GameMode.MAIN_MENU) {
            drawTitleBackground(g, false);
            drawSaveMenu(g, false);
        } else {
            drawWorld(g);
            drawInteriorPlacementPreview(g);
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
            } else if (visibleMode == GameMode.CRAFTING) {
                drawCrafting(g);
            } else if (visibleMode == GameMode.PARTY) {
                drawPartyOverview(g);
            } else if (visibleMode == GameMode.VILLAGE) {
                // Village controls live in the standard right sidebar.
            } else if (visibleMode == GameMode.BUILDING_ASSIGNMENT) {
                drawBuildingAssignment(g);
            } else if (visibleMode == GameMode.SETTLEMENT_BOARD) {
                drawSettlementBoard(g);
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
        drawHoverTooltip(g);
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

    private record MusicCue(String track, String bedTrack, float presence) {
    }

    private void updateMusic() {
        music.setVolume(effectiveMusicVolume());
        MusicCue cue = desiredMusicCue();
        if (cue == null || cue.track() == null) {
            music.stop();
        } else {
            music.blend(cue.track(), cue.bedTrack(), cue.presence());
        }
        music.update();
    }

    private MusicCue desiredMusicCue() {
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
            return new MusicCue(battleMusicTrack(), null, 1.0f);
        }
        if (WORLD_MUSIC_MODES.contains(state.mode)) {
            return worldMusicCue();
        }
        return null;
    }

    private MusicCue trackForMode(GameMode mode) {
        if (mode == GameMode.BATTLE && state.battle != null) {
            return new MusicCue(battleMusicTrack(), null, 1.0f);
        }
        if (mode == GameMode.MAIN_MENU || mode == GameMode.CLASS_SELECT) {
            return null;
        }
        return worldMusicCue();
    }

    private float effectiveMusicVolume() {
        return (state.config.masterVolume / 100.0f) * (state.config.musicVolume / 100.0f);
    }

    private float effectiveSfxVolume() {
        return (state.config.masterVolume / 100.0f) * (state.config.sfxVolume / 100.0f);
    }

    private void updateBattleSoundEffects() {
        sounds.setVolume(effectiveSfxVolume());
        if (state.mode != GameMode.BATTLE || state.battle == null || state.battle.effectTimer <= 0 || !state.battle.effectReleased()) {
            lastBattleEffectTimer = 0;
            lastBattleEffectSignature = "";
            return;
        }
        Battle battle = state.battle;
        String kind = battle.effectKind == null ? "strike" : battle.effectKind;
        String source = battle.effectSource == null ? "" : battle.effectSource.name;
        String target = battle.effectTarget == null ? "" : battle.effectTarget.name;
        String signature = kind + ":" + source + ":" + target;
        if (battle.effectTimer > lastBattleEffectTimer || !signature.equals(lastBattleEffectSignature)) {
            sounds.play(effectSoundName(kind));
        }
        lastBattleEffectTimer = battle.effectTimer;
        lastBattleEffectSignature = signature;
    }

    private MusicCue worldMusicCue() {
        String key = worldMusicKeyForCurrentPosition();
        List<String> palette = MUSIC_PALETTES.getOrDefault(key, List.of(key));
        if (!key.equals(worldMusicKey)) {
            previousWorldMusicTrack = worldMusicTrack;
            worldMusicKey = key;
            worldMusicPaletteIndex = Math.floorMod(key.hashCode() + state.dayNumber(), palette.size());
            worldMusicTrack = palette.get(worldMusicPaletteIndex);
            worldMusicDwellTicks = 0;
            worldMusicTrackTicks = 0;
            worldMusicBiomeChanging = previousWorldMusicTrack != null && !previousWorldMusicTrack.isBlank();
        } else {
            worldMusicDwellTicks++;
            worldMusicTrackTicks++;
            if (palette.size() > 1 && worldMusicTrackTicks >= BIOME_TUNE_ROTATION_TICKS) {
                previousWorldMusicTrack = worldMusicTrack;
                worldMusicPaletteIndex = (worldMusicPaletteIndex + 1) % palette.size();
                worldMusicTrack = palette.get(worldMusicPaletteIndex);
                worldMusicTrackTicks = 0;
                worldMusicBiomeChanging = false;
            }
        }

        float presence = 1.0f;
        if (previousWorldMusicTrack != null && !previousWorldMusicTrack.isBlank()) {
            int fadeTicks = worldMusicBiomeChanging ? BIOME_MUSIC_FULL_BLEND_TICKS : BIOME_TUNE_CROSSFADE_TICKS;
            int elapsedTicks = worldMusicBiomeChanging ? worldMusicDwellTicks : worldMusicTrackTicks;
            presence = Math.max(0.0f, Math.min(1.0f, elapsedTicks / (float) fadeTicks));
            if (presence >= 1.0f) {
                previousWorldMusicTrack = "";
                worldMusicBiomeChanging = false;
            }
        }
        return new MusicCue(worldMusicTrack, previousWorldMusicTrack, presence);
    }

    private String worldMusicKeyForCurrentPosition() {
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
        int menuY = 302;

        g.setColor(new Color(4, 7, 12, 96));
        g.fillRoundRect(menuX - 28, menuY - 26, menuW + 56, 318, 10, 10);
        g.setColor(new Color(212, 184, 113, 76));
        g.drawRoundRect(menuX - 28, menuY - 26, menuW + 56, 318, 10, 10);

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
        actionButton(g, x, y + 72, w, 52, "Load Adventure", () -> state.openSaveMenu(false), new Color(57, 71, 102), new Color(110, 127, 160), hasSave);
        if (!hasSave) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.setColor(new Color(165, 174, 193));
            drawCenteredIn(g, "No saved adventures yet.", x, y + 146, w);
        }
        actionButton(g, x, y + 164, w, 46, "Settings", state::openSettings, new Color(74, 66, 93), new Color(124, 107, 155), true);
        actionButton(g, x, y + 230, 164, 46, "Fullscreen", fullscreenToggle, new Color(74, 67, 80), new Color(117, 107, 128), true);
        actionButton(g, x + 176, y + 230, 164, 46, "Exit", this::exitGame, new Color(91, 60, 60), new Color(165, 111, 98), true);

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
        int mapWidth = state.world.width(state.currentMapId);
        int mapHeight = state.world.height(state.currentMapId);
        int tileSize = tileSize();
        int worldViewportHeight = worldViewportHeight();
        double viewportTilesX = gameAreaWidth() / (double) tileSize;
        double viewportTilesY = worldViewportHeight / (double) tileSize;
        int visibleCols = Math.max(1, (int) Math.ceil(viewportTilesX) + 2);
        int visibleRows = Math.max(1, (int) Math.ceil(viewportTilesY) + 2);
        double cameraX = clamp(renderPlayerX() - viewportTilesX / 2.0, 0.0, Math.max(0.0, mapWidth - viewportTilesX));
        double cameraY = clamp(renderPlayerY() - viewportTilesY / 2.0, 0.0, Math.max(0.0, mapHeight - viewportTilesY));
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
        rebuildVisibleWorldProps(camX, camY, visibleCols, visibleRows);

        Graphics2D worldGraphics = (Graphics2D) g.create();
        worldGraphics.setClip(0, 0, gameAreaWidth(), worldViewportHeight);
        worldGraphics.translate(-cameraOffsetX, -cameraOffsetY);
        for (int sy = 0; sy < visibleRows; sy++) {
            for (int sx = 0; sx < visibleCols; sx++) {
                int wx = camX + sx;
                int wy = camY + sy;
                int px = sx * tileSize;
                int py = sy * tileSize;
                if (wx < 0 || wy < 0 || wx >= mapWidth || wy >= mapHeight) {
                    worldGraphics.setColor(WORLD_VOID_COLOR);
                    worldGraphics.fillRect(px, py, tileSize, tileSize);
                    continue;
                }
                char tile = state.world.tileAt(state.currentMapId, wx, wy);
                char terrainTile = visibleTerrainTile(tile, wx, wy);
                worldGraphics.drawImage(assets.image(terrainImageName(terrainTile, wx, wy), tileSize, tileSize), px, py, null);
                drawTerrainEdges(worldGraphics, terrainTile, wx, wy, px, py);
                if (terrainTile == 'w' || terrainTile == '~') {
                    drawWaterAnimation(worldGraphics, wx, wy, px, py, tileSize);
                }
                if ("interior".equals(mapKind)) {
                    drawHouseTile(worldGraphics, tile, wx, wy, px, py);
                } else if (tile == 'h') {
                    worldGraphics.setColor(new Color(72, 47, 36, 96));
                    worldGraphics.fillRect(px + scaled(6), py + scaled(8), tileSize - scaled(12), tileSize - scaled(10));
                } else if (tile == 'x' || tile == 'o') {
                    worldGraphics.setColor(new Color(40, 38, 45, 120));
                    worldGraphics.fillRect(px, py, tileSize, tileSize);
                }
                String landmark = state.world.landmarkAt(state.currentMapId, wx, wy);
                if (landmark != null) {
                    worldGraphics.setColor(new Color(255, 245, 174));
                    worldGraphics.fillOval(px + scaled(19), py + scaled(4), scaled(10), scaled(10));
                }
            }
        }

        rebuildWorldLights(camX, camY, visibleCols, visibleRows, tileSize);
        drawFieldConnectors(worldGraphics, camX, camY);
        drawGroundPropOverlays(worldGraphics, camX, camY);
        drawMountainMassifOverlays(worldGraphics, camX, camY);
        drawRoadConnectors(worldGraphics, camX, camY);
        drawSettlementSurfaceSeams(worldGraphics, camX, camY);
        drawCityBuildingEntities(worldGraphics, camX, camY);
        drawVillageBuildingActionHover(worldGraphics, camX, camY, tileSize);
        drawVillageBuildingPlacementPreview(worldGraphics, camX, camY, tileSize);
        drawSettlementOverlays(worldGraphics, camX, camY);

        visibleWorldProps.clear();
        for (WorldProp prop : nearbyWorldProps) {
            if (!isGroundProp(prop.asset()) && isWorldPropVisible(prop, camX, camY, visibleCols, visibleRows)) {
                visibleWorldProps.add(prop);
            }
        }
        visibleWorldProps.sort(WORLD_PROP_DRAW_ORDER);
        for (WorldProp prop : visibleWorldProps) {
            drawWorldProp(worldGraphics, prop, camX, camY, tileSize);
        }
        drawQuestInteractibles(worldGraphics, camX, camY);
        drawQuestObjectives(worldGraphics, camX, camY);
        for (GameState.DungeonMonsterMotion motion : state.dungeonMonsterMotionsForMap(state.currentMapId)) {
            double mx = renderDungeonMonsterX(motion);
            double my = renderDungeonMonsterY(motion);
            if (mx < camX - 1 || my < camY - 1 || mx >= camX + visibleCols + 1 || my >= camY + visibleRows + 1) {
                continue;
            }
            int px = (int) Math.round((mx - camX) * tileSize);
            int py = (int) Math.round((my - camY) * tileSize);
            int size = motion.boss() ? tileRelative(58, tileSize) : tileRelative(44, tileSize);
            int x = px + (tileSize - size) / 2;
            int y = py + tileSize - size - tileRelative(motion.boss() ? 7 : 4, tileSize);
            drawShadow(worldGraphics, px + tileRelative(10, tileSize), py + tileRelative(37, tileSize), tileRelative(motion.boss() ? 38 : 30, tileSize), tileRelative(9, tileSize));
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

        for (GameState.NpcMotion motion : state.npcMotionsForMap(state.currentMapId)) {
            Npc npc = motion.npc();
            double nx = renderNpcX(motion);
            double ny = renderNpcY(motion);
            if (nx < camX - 1 || ny < camY - 1 || nx >= camX + visibleCols + 1 || ny >= camY + visibleRows + 1) {
                continue;
            }
            int px = (int) Math.round((nx - camX) * tileSize);
            int py = (int) Math.round((ny - camY) * tileSize);
            boolean moving = npcMoving(motion);
            int step = moving ? walkAnimationFrame(state.worldTick - motion.moveStartTick(), GameState.NpcMotion.MOVE_TICKS) : -1;
            String npcWorldSprite = npc.sprite() + "_model";
            CharacterWorldScale npcScale = characterWorldScale(npcWorldSprite, false);
            int npcW = tileRelative(npcScale.width(), tileSize);
            int npcH = tileRelative(npcScale.height(), tileSize);
            int npcX = px + (tileSize - npcW) / 2;
            int npcY = py + tileSize - npcH - tileRelative(npcScale.footLift(), tileSize);
            drawShadow(worldGraphics, px + tileRelative(24 - npcScale.shadowWidth() / 2, tileSize), py + tileRelative(40, tileSize), tileRelative(npcScale.shadowWidth(), tileSize), tileRelative(9, tileSize));
            drawCharacterSprite(worldGraphics, npcWorldSprite, npcX, npcY, npcW, npcH, motion.facingDx(), motion.facingDy(), step);
            Quest npcQuest = state.questForNpc(npc);
            if (npcQuest != null && !npcQuest.completed) {
                drawNpcQuestMarker(worldGraphics, px, py, npcQuest.ready() ? '?' : '!', npcQuest.ready());
            } else {
                worldGraphics.setColor(new Color(255, 245, 174));
                worldGraphics.fillOval(px + scaled(31), py + scaled(2) - (moving ? scaled(2) : 0), scaled(7), scaled(7));
            }
        }

        double playerX = renderPlayerX();
        double playerY = renderPlayerY();
        int playerPx = (int) Math.round((playerX - camX) * tileSize);
        int playerPy = (int) Math.round((playerY - camY) * tileSize);
        int step = playerMoving() ? playerWalkAnimationFrame() : -1;
        int bob = 0;
        CharacterWorldScale playerScale = characterWorldScale(state.player.worldSprite, true);
        int playerW = tileRelative(playerScale.width(), tileSize);
        int playerH = tileRelative(playerScale.height(), tileSize);
        int playerXpx = playerPx + (tileSize - playerW) / 2;
        int playerYpx = playerPy + tileSize - playerH - tileRelative(playerScale.footLift(), tileSize) + bob;
        drawPlayerTravelEffects(worldGraphics, playerPx, playerPy);
        drawShadow(worldGraphics, playerPx + tileRelative(24 - playerScale.shadowWidth() / 2, tileSize), playerPy + tileRelative(40, tileSize), tileRelative(playerScale.shadowWidth(), tileSize), tileRelative(10, tileSize));
        drawCharacterSprite(worldGraphics, state.player.worldSprite, playerXpx, playerYpx, playerW, playerH, playerFacingDx, playerFacingDy, step);
        drawNearbyQuestPrompt(worldGraphics, camX, camY);
        drawCloudLayer(worldGraphics, camX, camY);
        drawWorldLighting(worldGraphics, camX, camY, visibleCols, visibleRows, tileSize);
        worldGraphics.dispose();
        drawWorldAtmosphere(g);
        drawEmissiveWorldLights(g, camX, camY, tileSize, cameraOffsetX, cameraOffsetY);
        activeWorldLights.clear();
    }

    private void rebuildVisibleWorldProps(int camX, int camY, int visibleCols, int visibleRows) {
        nearbyWorldProps.clear();
        for (WorldProp prop : state.world.props(state.currentMapId)) {
            if (prop.x() < camX - 4 || prop.y() < camY - 4
                    || prop.x() >= camX + visibleCols + 4 || prop.y() >= camY + visibleRows + 4) {
                continue;
            }
            nearbyWorldProps.add(prop);
        }
    }

    private boolean isWorldPropVisible(WorldProp prop, int camX, int camY, int visibleCols, int visibleRows) {
        return prop.x() >= camX && prop.y() >= camY
                && prop.x() < camX + visibleCols && prop.y() < camY + visibleRows;
    }

    private void drawNpcQuestMarker(Graphics2D g, int px, int py, char marker, boolean ready) {
        int markerY = py - scaled(13) - (int) Math.round(Math.sin(frame * 0.18) * scaled(2));
        int cx = px + scaled(34);
        int radius = scaled(10);
        Color fill = ready ? new Color(91, 184, 255) : new Color(246, 190, 75);
        Color rim = ready ? new Color(210, 239, 255) : new Color(255, 242, 169);
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
        g.setColor(ready ? new Color(8, 32, 54) : new Color(48, 35, 13));
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(11, scaled(13))));
        String text = Character.toString(marker);
        FontMetrics metrics = g.getFontMetrics();
        int textX = cx - metrics.stringWidth(text) / 2;
        int textY = markerY + scaled(14);
        g.drawString(text, textX, textY);
    }

    private void drawWorldProp(Graphics2D g, WorldProp prop, int camX, int camY, int tileSize) {
        String asset = prop.asset();
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

    private void drawPropImage(Graphics2D g, String asset, int x, int y, int size, float opacity) {
        drawPropImage(g, asset, x, y, size, size, opacity);
    }

    private void drawPropImage(Graphics2D g, String asset, int x, int y, int width, int height, float opacity) {
        Composite oldComposite = g.getComposite();
        if (opacity < 1.0f) {
            g.setComposite(AlphaComposite.SrcOver.derive(opacity));
        }
        BufferedImage image = asset.startsWith("interior_")
                ? assets.image(asset, width, height)
                : assets.spriteFit(asset, width, height);
        g.drawImage(image, x, y, null);
        g.setComposite(oldComposite);
    }

    private int interiorPropWidth(String asset, int size) {
        if (asset.equals("interior_tavern_bar") || asset.equals("interior_shop_counter")
                || asset.equals("interior_carpenter_table") || asset.equals("interior_alchemy_station")
                || asset.equals("interior_cooking_station") || asset.equals("interior_herb_drying_rack")
                || asset.equals("interior_wall_window_wide") || asset.equals("interior_wall_plant_shelf")
                || asset.equals("interior_wall_herb_rack") || asset.equals("interior_floor_bushy_planter")
                || asset.equals("interior_aquarium_table")) {
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
                || asset.equals("interior_chair_east") || asset.equals("interior_chair_west")) {
            return Math.max(1, Math.round(tileSize * 0.78f));
        }
        if (asset.equals("interior_round_table")) {
            return Math.max(1, Math.round(tileSize * 0.94f));
        }
        if (asset.equals("interior_herb_pot") || asset.equals("interior_flower_pot")
                || asset.equals("interior_planting_pot") || asset.equals("interior_cookpot_stand")) {
            return Math.max(1, Math.round(tileSize * 0.82f));
        }
        if (asset.equals("interior_sprout_planter") || asset.equals("interior_herb_planter")) {
            return Math.max(1, Math.round(tileSize * 0.92f));
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
                || asset.startsWith("city_prop_")
                || asset.equals("city_lantern")
                || asset.startsWith("location_camp_")
                || asset.equals("player_village_quest_board")
                || asset.equals("deco_road_signpost")
                || asset.equals("deco_road_milestone")
                || asset.equals("deco_imagen_signpost")
                || asset.equals("deco_imagen_milestone")
                || asset.equals("deco_imagen_road_camp");
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

    private int interiorPropOffsetX(String asset, int tileSize) {
        if (asset.equals("interior_chair_east")) {
            return tileRelative(3, tileSize);
        }
        if (asset.equals("interior_chair_west")) {
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
        if (asset.equals("interior_chair_north")) {
            return tileRelative(3, tileSize);
        }
        if (asset.equals("interior_chair_south")) {
            return -tileRelative(4, tileSize);
        }
        if (asset.equals("interior_side_table")) {
            return tileRelative(2, tileSize);
        }
        return 0;
    }

    private int interiorPropHeight(String asset, int size) {
        if (asset.equals("interior_bed_vertical") || asset.equals("interior_vine_trellis")) {
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
        } else if (isMagicGlowProp(asset)) {
            drawMagicPropAnimation(g, prop, x, y, size);
        }
    }

    private boolean isFireProp(String asset) {
        return asset.contains("camp_fire") || asset.contains("campfire") || asset.contains("fire")
                || asset.contains("forge") || asset.contains("oven") || asset.contains("stove")
                || asset.contains("hearth") || asset.contains("cookpot") || asset.contains("cooking_station")
                || asset.contains("sconce");
    }

    private boolean isMagicGlowProp(String asset) {
        return asset.contains("crystal")
                || asset.contains("alchemy")
                || asset.contains("ice_crystals")
                || asset.contains("rune")
                || asset.contains("shrine")
                || asset.contains("fairy_pool")
                || asset.contains("bubble_pool")
                || asset.contains("firefly");
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

            if (objective.kind() == Quest.ObjectiveKind.DEFEAT) {
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
            case VISIT -> "V";
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
            case VISIT -> new Color(114, 191, 255, alpha);
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

    private void triggerInteriorPlacementPulse(int x, int y) {
        placementPulses.add(new PlacementPulse(state.currentMapId, x, y, frame));
    }

    private CharacterWorldScale characterWorldScale(String sprite, boolean player) {
        return switch (sprite) {
            case "class_knight_model", "npc_torin_model" -> new CharacterWorldScale(48, 65, 4, 38);
            case "class_mage_model", "npc_ren_model", "npc_rowan_model", "npc_vexa_model", "npc_elowen_model",
                    "npc_liora_model" -> new CharacterWorldScale(50, 66, 4, 36);
            case "class_cleric_model", "npc_mira_sunwarden_model" -> new CharacterWorldScale(47, 64, 4, 36);
            case "class_ranger_model", "class_rogue_model", "npc_nyx_model", "npc_sable_model", "npc_kael_model" ->
                    new CharacterWorldScale(44, 61, 3, 34);
            case "npc_garruk_model" -> new CharacterWorldScale(52, 66, 4, 42);
            case "npc_orin_model" -> new CharacterWorldScale(50, 59, 3, 40);
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

    private int playerWalkAnimationFrame() {
        double progress = moveProgress(frame - playerMoveStartFrame, PLAYER_MOVE_FRAMES);
        int animationStep = (int) Math.floor((playerWalkAnimationTileStart + progress) * PLAYER_WALK_FRAMES_PER_TILE);
        return Math.floorMod(animationStep, WALK_ANIMATION_FRAMES);
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

    private boolean playerMoving() {
        return state.currentMapId.equals(playerMoveMapId)
                && frame - playerMoveStartFrame < PLAYER_MOVE_FRAMES
                && (state.playerX != playerMoveFromX || state.playerY != playerMoveFromY);
    }

    private double renderPlayerX() {
        if (!playerMoving()) {
            return state.playerX;
        }
        double t = moveProgress(frame - playerMoveStartFrame, PLAYER_MOVE_FRAMES);
        return playerMoveFromX + (state.playerX - playerMoveFromX) * t;
    }

    private double renderPlayerY() {
        if (!playerMoving()) {
            return state.playerY;
        }
        double t = moveProgress(frame - playerMoveStartFrame, PLAYER_MOVE_FRAMES);
        return playerMoveFromY + (state.playerY - playerMoveFromY) * t;
    }

    private double moveProgress(int elapsed, int duration) {
        return Math.max(0.0, Math.min(1.0, (elapsed + 1.0) / Math.max(1.0, duration)));
    }

    private double smoothStep(double value) {
        double t = Math.max(0.0, Math.min(1.0, value));
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
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
            if (usesStandaloneSettlementBuilding(kind, building)) {
                drawStandaloneVillageBuilding(g, building, camX, camY, ts, lotX, lotW, frontY);
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
                int targetH = Math.max(tileRelative(civic ? 88 : 78, ts),
                        Math.min(tileRelative(civic ? 130 : 116, ts), building.depth() * ts + tileRelative(civic ? 34 : 26, ts)));
                int targetW = Math.max(tileRelative(civic ? 58 : 46, ts),
                        Math.min(lotW / Math.max(1, modules) + tileRelative(civic ? 30 : 22, ts), tileRelative(civic ? 104 : 88, ts)));
                int centerX = lotX + (int) Math.round((index + 0.5) * lotW / modules);
                int jitter = ((seed >> (index * 4)) & 7) - 3;
                int drawX = centerX - targetW / 2 + jitter * ts / GameConfig.TILE;
                int drawY = frontY - targetH - tileRelative(3, ts);
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
        }
    }

    private boolean isPlayerVillageBuilding(CityBuilding building) {
        return building.key() != null && building.key().startsWith("player_")
                && VillageManager.isManagedBuildingStyle(building.style());
    }

    private boolean usesStandaloneSettlementBuilding(String kind, CityBuilding building) {
        if (isPlayerVillageBuilding(building)) {
            return true;
        }
        if (VillageManager.isManagedBuildingStyle(building.style())) {
            return true;
        }
        return List.of("hall", "barracks", "arena", "mage_tower", "bell_tower", "sun_shrine", "river_hall")
                .contains(building.style());
    }

    private void drawStandaloneVillageBuilding(Graphics2D g, CityBuilding building, int camX, int camY,
                                                int ts, int lotX, int lotW, int frontY) {
        int level = isPlayerVillageBuilding(building) ? state.world.playerVillageBuildingLevel(building) : 1;
        String asset = villageBuildingSpriteForLevel(building.style(), level, building, 0, 0);
        TilePoint door = state.world.cityBuildingDoorTiles(building).stream()
                .findFirst()
                .orElse(new TilePoint(building.x1() + building.width() / 2, building.y2()));
        int doorCenterX = (door.x() - camX) * ts + ts / 2;
        int[] target = standaloneBuildingTargetSize(building, lotW, ts);
        int targetW = target[0];
        int targetH = target[1];
        int drawX = doorCenterX - targetW / 2;
        int drawY = frontY - targetH - tileRelative(2, ts);

        g.setColor(new Color(10, 13, 14, 105));
        g.fillOval(lotX + scaled(6), frontY - scaled(13), Math.max(scaled(24), lotW - scaled(12)), scaled(13));
        g.drawImage(assets.spriteFit(asset, targetW, targetH), drawX, drawY, null);
        drawBuildingDoorMarker(g, door, camX, camY, ts);
    }

    private int[] standaloneBuildingTargetSize(CityBuilding building, int lotW, int ts) {
        if ("garden".equals(building.style())) {
            return new int[]{
                    Math.max(tileRelative(104, ts), Math.min(lotW + tileRelative(20, ts), tileRelative(148, ts))),
                    Math.max(tileRelative(74, ts), Math.min(building.depth() * ts + tileRelative(34, ts), tileRelative(104, ts)))
            };
        }
        if (List.of("barracks", "watchtower", "mage_tower", "bell_tower").contains(building.style())) {
            return new int[]{
                    Math.max(tileRelative(82, ts), Math.min(lotW + tileRelative(12, ts), tileRelative(118, ts))),
                    Math.max(tileRelative(126, ts), Math.min(building.depth() * ts + tileRelative(72, ts), tileRelative(166, ts)))
            };
        }
        if (List.of("hall", "inn", "arena", "sun_shrine", "river_hall").contains(building.style())) {
            return new int[]{
                    Math.max(tileRelative(126, ts), Math.min(lotW + tileRelative(24, ts), tileRelative(172, ts))),
                    Math.max(tileRelative(112, ts), Math.min(building.depth() * ts + tileRelative(56, ts), tileRelative(148, ts)))
            };
        }
        return new int[]{
                Math.max(tileRelative(96, ts), Math.min(lotW + tileRelative(18, ts), tileRelative(146, ts))),
                Math.max(tileRelative(106, ts), Math.min(building.depth() * ts + tileRelative(46, ts), tileRelative(136, ts)))
        };
    }

    private void drawBuildingDoorMarker(Graphics2D g, TilePoint door, int camX, int camY, int ts) {
        if (door.x() < camX || door.x() >= camX + lastVisibleCols || door.y() < camY || door.y() >= camY + lastVisibleRows) {
            return;
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
        String[] sprites = VillageManager.isManagedBuildingStyle(building.style())
                ? VillageManager.buildingSprites(building.style()).toArray(String[]::new)
                : switch (building.style()) {
                    case "arena" -> new String[]{"city_building_civic_hall_imagegen", "city_building_town_manor"};
                    case "mage_tower" -> new String[]{"city_building_stone_tower", "city_building_house_tower"};
                    case "bell_tower" -> new String[]{"city_building_watchtower_imagegen", "city_building_stone_tower"};
                    case "sun_shrine" -> new String[]{"village_building_shrine", "city_building_stone_hall"};
                    case "river_hall" -> new String[]{"city_building_civic_hall_imagegen", "city_building_house_wide"};
                    case "hall" -> new String[]{"city_building_civic_hall_imagegen", "city_building_town_manor"};
                    case "barracks" -> new String[]{"city_building_stone_tower", "city_building_watchtower_imagegen"};
                    case "warehouse" -> new String[]{"village_building_warehouse", "city_building_house_wide"};
                    default -> new String[]{"city_building_town_gabled", "city_building_house_shop", "city_building_town_shop"};
                };
        return sprites[Math.floorMod(seed + building.palette() + index * 3, sprites.length)];
    }

    private String villageBuildingSpriteForLevel(String style, int level, CityBuilding fallbackBuilding, int seed, int index) {
        if (VillageManager.isManagedBuildingStyle(style)) {
            String candidate = VillageManager.buildingLevelSprite(style, level);
            if (!candidate.isBlank() && assets.hasSprite(candidate)) {
                return candidate;
            }
        }
        if (fallbackBuilding != null) {
            return buildingSprite(fallbackBuilding, seed, index);
        }
        List<String> sprites = VillageManager.buildingSprites(style);
        return sprites.isEmpty() ? "" : sprites.get(0);
    }

    private void drawHouseTile(Graphics2D g, char tile, int wx, int wy, int px, int py) {
        if (tile == 'o') {
            drawHouseWallTile(g, wx, wy, px, py);
        } else if (tile == 'e' && doorConnectsWall(wx, wy)) {
            drawHouseFloorTile(g, 'i', wx, wy, px, py);
            drawHouseDoorTile(g, wx, wy, px, py);
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
        } else if (tile == 'e') {
            String door = nearInteriorWall(wx, wy, 0, -1) || nearInteriorWall(wx, wy, 0, 1)
                    ? "interior_door_open"
                    : "interior_door_closed";
            g.drawImage(assets.image(door, ts, ts), px, py, null);
            g.setColor(new Color(0, 0, 0, 70));
            g.fillRect(px + scaled(5), py + ts - scaled(6), ts - scaled(10), scaled(3));
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
        boolean straightHorizontal = (wallWest || wallEast) && !wallNorth && !wallSouth;
        String asset = connectedInteriorWallAsset(wx, wy, seed, floorNorth, floorSouth, floorWest, floorEast);

        if (drawsTallBackWall(wx, wy, floorNorth, floorSouth)) {
            drawTallBackWallLayer(g, wx, wy, px, py, ts, seed);
        }
        if (straightHorizontal) {
            drawLinearHorizontalWallTile(g, px, py, ts, floorNorth, floorSouth);
            return;
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
        if (floorEast) {
            g.setColor(new Color(0, 0, 0, 54));
            g.fillRect(px + ts - scaled(5), py + scaled(6), scaled(4), ts - scaled(11));
        }
        if (floorWest) {
            g.setColor(new Color(255, 226, 156, 36));
            g.fillRect(px + scaled(1), py + scaled(6), scaled(3), ts - scaled(11));
        }
    }

    private void drawLinearHorizontalWallTile(Graphics2D g, int px, int py, int ts, boolean floorNorth, boolean floorSouth) {
        int capH = Math.max(2, tileRelative(12, ts));
        int shadowH = Math.max(2, tileRelative(9, ts));
        int trimY = floorSouth && !floorNorth ? py + ts - shadowH - tileRelative(4, ts) : py + tileRelative(21, ts);
        g.drawImage(assets.image("interior_wall_horizontal_cap", ts, capH), px, py + tileRelative(2, ts), null);
        g.drawImage(assets.image("interior_wall_horizontal_shadow", ts, shadowH), px, trimY, null);
        g.setColor(new Color(255, 229, 167, 34));
        g.fillRect(px, py + tileRelative(3, ts), ts, Math.max(1, tileRelative(1, ts)));
        g.setColor(new Color(0, 0, 0, floorSouth ? 86 : 56));
        g.fillRect(px, trimY + shadowH - Math.max(1, tileRelative(2, ts)), ts, Math.max(1, tileRelative(3, ts)));
        if (floorSouth) {
            g.setColor(new Color(0, 0, 0, 54));
            g.fillRect(px, py + ts - tileRelative(5, ts), ts, tileRelative(4, ts));
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
        boolean horizontal = wallConnection(wx, wy, -1, 0) || wallConnection(wx, wy, 1, 0);
        String asset = horizontal ? "interior_wall_door_h_open" : "interior_wall_door_v_open";
        g.drawImage(assets.image(asset, ts, ts), px, py, null);
        g.setColor(new Color(0, 0, 0, 68));
        g.fillRect(px + scaled(4), py + ts - scaled(5), ts - scaled(8), scaled(3));
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
            if (!west) {
                return "interior_wall_inner_nw";
            }
            if (!east) {
                return "interior_wall_inner_ne";
            }
            if (!north) {
                return "interior_wall_inner_sw";
            }
            return "interior_wall_inner_se";
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
            return "interior_wall_vertical_side";
        }
        if (floorEast && !floorWest) {
            return "interior_wall_vertical_side";
        }
        if (floorWest && !floorEast) {
            return "interior_wall_vertical_side";
        }
        return "interior_wall_horizontal_center";
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

    private boolean nearInteriorWall(int wx, int wy, int ox, int oy) {
        return state.world.tileAt(state.currentMapId, wx + ox, wy + oy) == 'o';
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
            if (other == tile || other == 'r' || tile == 'r' || other == 'c' || other == 'u' || tile == 'c' || tile == 'u') {
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
        WeatherCondition weather = state.currentWeather();
        double wind = state.windStrength();
        double wave = waterWaveStrength(weather, wind);
        int seed = Math.abs(wx * 928371 + wy * 364479 + state.currentMapId.hashCode());
        double windX = Math.cos(state.windRadians());
        double windY = Math.sin(state.windRadians());
        double drift = frame * (0.028 + wave * 0.060) + seed * 0.009;

        Graphics2D water = (Graphics2D) g.create();
        water.setClip(px, py, tileSize, tileSize);
        water.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Composite oldComposite = water.getComposite();
        water.setComposite(AlphaComposite.SrcOver.derive((float) (0.08 + wave * 0.10)));
        water.setColor(new Color(60, 122, 174));
        int shadowOffsetX = scaled((int) Math.round(windX * wave * 4.0));
        int shadowOffsetY = scaled((int) Math.round(windY * wave * 3.0));
        water.fillOval(px + scaled(4) + shadowOffsetX, py + scaled(6) + shadowOffsetY, tileSize - scaled(8), tileSize - scaled(10));

        int bands = 3 + (int) Math.round(wave * 4.0);
        for (int i = 0; i < bands; i++) {
            double local = drift + i * 1.83;
            int y = py + scaled(7) + Math.floorMod((int) Math.round(i * tileSize / (double) Math.max(1, bands) + frame * (1.2 + wave * 2.8) + seed / 19), tileSize + scaled(8)) - scaled(4);
            int startX = px - scaled(8) + (int) Math.round(Math.sin(local) * scaled(4) + windX * scaled(3));
            int length = scaled(18 + Math.floorMod(seed / (i + 5), 17)) + (int) Math.round(wave * scaled(18));
            int amplitude = Math.max(1, scaled(1) + (int) Math.round(wave * scaled(3)));
            int alpha = clampColor((int) Math.round(50 + wave * 88 + Math.sin(local * 1.4) * 18));
            water.setComposite(AlphaComposite.SrcOver.derive(alpha / 255.0f));
            water.setColor(new Color(188, 234, 245));
            water.setStroke(new BasicStroke(Math.max(1f, scaledStroke(0.8f + (float) wave * 0.8f)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            drawWaveStroke(water, startX, y, length, amplitude, local);
        }

        double rainIntensity = weatherIntensity(WeatherCondition.RAIN);
        double stormIntensity = weatherIntensity(WeatherCondition.STORM);
        if (rainIntensity > WEATHER_VISIBILITY_EPSILON) {
            drawWaterRainRings(water, wx, wy, px, py, tileSize, seed, false, rainIntensity);
        }
        if (stormIntensity > WEATHER_VISIBILITY_EPSILON) {
            drawWaterRainRings(water, wx, wy, px, py, tileSize, seed, true, stormIntensity);
        }
        drawWaterShoreFoam(water, wx, wy, px, py, tileSize, wave);
        water.setComposite(oldComposite);
        water.dispose();
    }

    private void drawWaveStroke(Graphics2D g, int x, int y, int length, int amplitude, double phase) {
        int segments = 4;
        int prevX = x;
        int prevY = y + (int) Math.round(Math.sin(phase) * amplitude);
        for (int i = 1; i <= segments; i++) {
            int nextX = x + length * i / segments;
            int nextY = y + (int) Math.round(Math.sin(phase + i * 0.85) * amplitude);
            g.drawLine(prevX, prevY, nextX, nextY);
            prevX = nextX;
            prevY = nextY;
        }
    }

    private void drawWaterRainRings(Graphics2D g, int wx, int wy, int px, int py, int tileSize, int seed, boolean heavy, double intensity) {
        int rings = Math.max(1, (int) Math.round((heavy ? 4 : 2) * intensity));
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

    private double waterWaveStrength(WeatherCondition weather, double wind) {
        double weatherPush = blendedWeatherPush(weather);
        return clamp(weatherPush * 0.72 + wind * 0.42, 0.18, 1.0);
    }

    private double blendedWeatherPush(WeatherCondition fallbackWeather) {
        double push = 0.0;
        double total = 0.0;
        for (WeatherCondition condition : WeatherCondition.values()) {
            double intensity = weatherIntensity(condition);
            if (intensity <= 0.0) {
                continue;
            }
            push += weatherWavePush(condition) * intensity;
            total += intensity;
        }
        if (total <= 0.001) {
            return weatherWavePush(fallbackWeather);
        }
        return push / total;
    }

    private double weatherWavePush(WeatherCondition weather) {
        return switch (weather) {
            case STORM, BLIZZARD -> 0.90;
            case RAIN, SNOW, DUST -> 0.58;
            case CLOUDY, FOG, HEAT_HAZE -> 0.34;
            case CLEAR -> 0.20;
        };
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
        boolean texturedRoads = "village".equals(mapKind) || "overworld".equals(mapKind);
        for (int sy = 0; sy < lastVisibleRows; sy++) {
            for (int sx = 0; sx < lastVisibleCols; sx++) {
                int wx = camX + sx;
                int wy = camY + sy;
                char tile = state.world.tileAt(state.currentMapId, wx, wy);
                if (tile != 'r' && tile != 'q' && tile != 'B') {
                    continue;
                }
                int px = sx * ts;
                int py = sy * ts;
                if (tile == 'B') {
                    drawBridgeTile(g, wx, wy, px, py);
                    continue;
                }
                if ("village".equals(mapKind)) {
                    drawTexturedRoadTile(g, wx, wy, px, py, tile, true);
                    continue;
                }
                if ("overworld".equals(mapKind)) {
                    drawTexturedRoadTile(g, wx, wy, px, py, tile, false);
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
            drawTexturedRoadJunctionFillers(g, camX, camY, "village".equals(mapKind));
        }
    }

    private void drawTexturedRoadTile(Graphics2D g, int wx, int wy, int px, int py, char tile, boolean settlementRoad) {
        int ts = tileSize();
        int bits = roadBits(wx, wy);
        String suffix = "0" + Integer.toHexString(bits);
        String asset = "road_overlay_" + suffix.substring(suffix.length() - 2);
        g.drawImage(assets.image(asset, ts, ts), px, py, null);
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
                g.drawImage(filler, px, py, null);
            }
        }
    }

    private boolean isRoadJunctionFill(int wx, int wy) {
        return isTexturedRoadTile(wx, wy)
                && isTexturedRoadTile(wx + 1, wy)
                && isTexturedRoadTile(wx, wy + 1)
                && isTexturedRoadTile(wx + 1, wy + 1);
    }

    private boolean isTexturedRoadTile(int wx, int wy) {
        char tile = state.world.tileAt(state.currentMapId, wx, wy);
        return tile == 'r' || tile == 'q';
    }

    private void drawContinuousRoadUnderlay(Graphics2D g, int wx, int wy, int px, int py, char tile, boolean settlementRoad) {
        int ts = tileSize();
        Graphics2D road = (Graphics2D) g.create();
        road.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color terrain = Terrain.color(roadUnderlayTile(tile, wx, wy));
        Color shoulderBase = tile == 'q'
                ? new Color(116, 112, 101)
                : new Color(156, 118, 70);
        Color coreBase = tile == 'q'
                ? new Color(126, 120, 106)
                : new Color(166, 126, 75);
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
        Color color = tile == 'q'
                ? new Color(126, 119, 104, settlementRoad ? 110 : 82)
                : new Color(162, 123, 73, settlementRoad ? 112 : 88);
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
        Color color = tile == 'q'
                ? new Color(119, 113, 98, settlementRoad ? 70 : 48)
                : new Color(157, 119, 69, settlementRoad ? 72 : 52);
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
            Color color = tile == 'q'
                    ? (light ? new Color(178, 172, 150, 72) : new Color(74, 70, 58, 58))
                    : (light ? new Color(220, 173, 105, 70) : new Color(88, 61, 35, 56));
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
        return tile == 'r' || tile == 'q' || tile == 'B' || tile == 'c' || tile == 'u' || tile == 'd';
    }

    private char visibleTerrainTile(char tile, int wx, int wy) {
        String mapKind = state.world.kind(state.currentMapId);
        if (tile == 'B') {
            return 'w';
        }
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
                    settlement.x(),
                    settlement.y(),
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
            return Math.min(230, 96 + state.world.playerVillageStage() * 14);
        }
        return switch (settlement.kind()) {
            case "City" -> 230;
            case "Town" -> 188;
            case "Camp", "Player Settlement" -> 132;
            default -> 176;
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
        double altitude = sunAltitude();
        double horizon = 1.0 - altitude;
        double direction = shadowCastX();
        double sampleX = x + w / 2.0;
        double sampleY = y + h / 2.0;
        double lightInfluence = 0.0;
        double red = 0.0;
        double green = 0.0;
        double blue = 0.0;
        double weight = 0.0;
        for (WorldLight light : activeWorldLights) {
            double dx = sampleX - light.x;
            double dy = sampleY - light.y;
            double radius = light.shadowRadius();
            double distanceSq = dx * dx + dy * dy;
            if (distanceSq >= radius * radius) {
                continue;
            }
            double falloff = 1.0 - Math.sqrt(distanceSq) / radius;
            double influenceContribution = falloff * falloff * light.shadowStrength();
            double colorContribution = falloff * light.shadowStrength();
            lightInfluence += influenceContribution;
            red += light.color.getRed() * colorContribution;
            green += light.color.getGreen() * colorContribution;
            blue += light.color.getBlue() * colorContribution;
            weight += colorContribution;
        }
        lightInfluence = Math.min(1.0, lightInfluence);
        int castX = (int) Math.round(w * direction * (0.32 + horizon * 1.55));
        int castY = (int) Math.round(h * (0.26 + horizon * 0.62));
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

    private void drawWorldLighting(Graphics2D g, int camX, int camY, int visibleCols, int visibleRows, int tileSize) {
        Graphics2D light = (Graphics2D) g.create();
        light.setClip(0, 0, gameAreaWidth(), viewHeight());
        light.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawBiomeLightTints(light, camX, camY, visibleCols, visibleRows, tileSize);
        drawLightTileSpill(light, camX, camY, visibleCols, visibleRows, tileSize);
        drawWorldVignette(light);
        light.dispose();
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

        addSettlementWorldLights(tileSize, camX, camY, visibleCols, visibleRows);
        addPlayerWorldLight(tileSize);
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
        activeWorldLights.add(new WorldLight(x, y, Math.max(1, radius), color, Math.min(1.0f, alpha), affectsShadows));
    }

    private int worldTileCenter(int coordinate, int tileSize) {
        return coordinate * tileSize + tileSize / 2;
    }

    private void drawLightTileSpill(Graphics2D g, int camX, int camY, int visibleCols, int visibleRows, int tileSize) {
        if (activeWorldLights.isEmpty()) {
            return;
        }
        Composite oldComposite = g.getComposite();
        for (WorldLight light : activeWorldLights) {
            int minX = Math.max(camX, (int) Math.floor((light.x - light.radius) / tileSize));
            int maxX = Math.min(camX + visibleCols - 1, (int) Math.ceil((light.x + light.radius) / tileSize));
            int minY = Math.max(camY, (int) Math.floor((light.y - light.radius) / tileSize));
            int maxY = Math.min(camY + visibleRows - 1, (int) Math.ceil((light.y + light.radius) / tileSize));
            for (int wy = minY; wy <= maxY; wy++) {
                for (int wx = minX; wx <= maxX; wx++) {
                    double cx = worldTileCenter(wx, tileSize);
                    double cy = worldTileCenter(wy, tileSize);
                    double dx = cx - light.x;
                    double dy = cy - light.y;
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    if (distance >= light.radius) {
                        continue;
                    }
                    double falloff = 1.0 - distance / light.radius;
                    float alpha = (float) Math.min(0.18, light.alpha * falloff * falloff * 0.72);
                    if (alpha <= 0.004f) {
                        continue;
                    }
                    int px = (wx - camX) * tileSize;
                    int py = (wy - camY) * tileSize;
                    g.setComposite(AlphaComposite.SrcOver.derive(alpha));
                    g.setColor(light.color);
                    g.fillRect(px, py, tileSize, tileSize);
                }
            }
        }
        g.setComposite(oldComposite);
    }

    private void drawBiomeLightTints(Graphics2D g, int camX, int camY, int visibleCols, int visibleRows, int tileSize) {
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
            case 'r', 'c', 'u' -> BIOME_TINT_ROAD;
            default -> BIOME_TINT_GRASS;
        };
    }

    private float biomeLightAlpha(char tile) {
        return switch (tile) {
            case 'f', 'v' -> 0.045f;
            case 's', 'n', 'w' -> 0.036f;
            case 'b', 'm', 'q' -> 0.030f;
            case 'r', 'c', 'u' -> 0.026f;
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
        if (asset.contains("camp_fire") || asset.contains("campfire") || asset.contains("fire")) {
            return new Color(255, 151, 58);
        }
        if (asset.contains("street_lamp") || asset.contains("lantern") || asset.contains("sconce")) {
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
        if (asset.contains("crystal") || asset.contains("rune") || asset.contains("shrine") || asset.contains("alchemy")
                || asset.contains("fairy_pool") || asset.contains("bubble_pool") || asset.contains("firefly")) {
            return 0.34f + night * 0.88f;
        }
        if (asset.contains("camp_fire") || asset.contains("campfire") || asset.contains("fire")
                || asset.contains("cookpot") || asset.contains("cooking_station")
                || asset.contains("street_lamp") || asset.contains("lantern")) {
            return 0.12f + night * 1.10f;
        }
        return lightVisibility();
    }

    private int propGlowRadius(String asset) {
        if (asset.contains("camp_fire") || asset.contains("campfire") || asset.contains("fire")) {
            return scaled(96);
        }
        if (asset.contains("street_lamp") || asset.contains("lantern")) {
            return scaled(72);
        }
        if (asset.contains("crystal") || asset.contains("rune") || asset.contains("shrine")) {
            return scaled(78);
        }
        return scaled(66);
    }

    private float lightVisibility() {
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

    private void drawGroundPropOverlays(Graphics2D g, int camX, int camY) {
        int ts = tileSize();
        int inset = scaled(2);
        for (WorldProp prop : nearbyWorldProps) {
            if (!isGroundProp(prop.asset())) {
                continue;
            }
            if (!isWorldPropVisible(prop, camX, camY, lastVisibleCols, lastVisibleRows)) {
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
                || asset.equals("location_graveyard_path")
                || asset.equals("location_dungeon_approach_path");
    }

    private boolean castsPropShadow(String asset) {
        return !asset.equals("location_farmland_tilled")
                && !asset.equals("location_farmland_wheat")
                && !asset.equals("location_graveyard_dirt")
                && !asset.equals("location_graveyard_path")
                && !asset.equals("location_dungeon_approach_path")
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
        Graphics2D cloudG = (Graphics2D) g.create();
        int ts = tileSize();
        int density = blendedCloudDensity();
        float opacity = blendedCloudOpacity();
        if (opacity <= WEATHER_VISIBILITY_EPSILON || density <= 0) {
            cloudG.dispose();
            return;
        }
        cloudG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        for (int i = 0; i < 96; i++) {
            int seed = cloudSeed(i);
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

    private int cloudSeed(int index) {
        return Math.abs(index * 734287 + 19349663);
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
            double intensity = weatherIntensity(condition);
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
            double intensity = weatherIntensity(condition);
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
        drawTimeOverlay(atmosphere, mapW, mapH);
        drawWeatherOverlay(atmosphere, mapW, mapH);
        atmosphere.dispose();
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
            double intensity = weatherIntensity(weather);
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
                drawRain(g, width, height, 420, false, intensity);
                drawRainSplashes(g, width, height, 90, false, intensity);
            }
            case STORM -> {
                tint(g, width, height, new Color(22, 30, 42), (float) (0.24f * intensity));
                drawRainVeil(g, width, height, true, intensity);
                drawRain(g, width, height, 650, true, intensity);
                drawRainSplashes(g, width, height, 150, true, intensity);
                drawLightning(g, width, height, intensity);
            }
            case SNOW -> drawSnow(g, width, height, 70, false, intensity);
            case BLIZZARD -> {
                tint(g, width, height, new Color(220, 232, 240), (float) (0.18f * intensity));
                drawSnow(g, width, height, 130, true, intensity);
            }
            case FOG -> drawFadedWeatherLayer(g, width, height, (float) intensity, layer -> drawFog(layer, width, height));
            case DUST -> drawFadedWeatherLayer(g, width, height, (float) intensity, layer -> drawDust(layer, width, height));
            case HEAT_HAZE -> drawFadedWeatherLayer(g, width, height, (float) intensity, layer -> drawHeatHaze(layer, width, height));
            case CLOUDY -> tint(g, width, height, new Color(84, 91, 105), (float) (0.10f * intensity));
            case CLEAR -> {
            }
        }
    }

    private void drawFadedWeatherLayer(Graphics2D g, int width, int height, float alpha, WeatherLayerPainter painter) {
        if (alpha <= WEATHER_VISIBILITY_EPSILON) {
            return;
        }
        if (weatherLayerBuffer == null || weatherLayerBuffer.getWidth() != width || weatherLayerBuffer.getHeight() != height) {
            weatherLayerBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }

        Graphics2D layer = weatherLayerBuffer.createGraphics();
        layer.setComposite(AlphaComposite.Clear);
        layer.fillRect(0, 0, width, height);
        layer.setComposite(AlphaComposite.SrcOver);
        layer.setClip(0, 0, width, height);
        painter.paint(layer);
        layer.dispose();

        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) clamp(alpha, 0.0, 1.0)));
        g.drawImage(weatherLayerBuffer, 0, 0, null);
        g.setComposite(oldComposite);
    }

    private void tint(Graphics2D g, int width, int height, Color color, float alpha) {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.setColor(color);
        g.fillRect(0, 0, width, height);
    }

    private void drawRain(Graphics2D g, int width, int height, int count, boolean heavy, double intensity) {
        int activeCount = (int) Math.round(count * intensity);
        if (activeCount <= 0) {
            return;
        }
        BufferedImage drop = particleSprite(heavy ? "rain_heavy" : "rain");
        double wind = state.windRadians();
        double strength = state.windStrength();
        double windX = Math.cos(wind) * strength;
        double gust = Math.sin(frame * 0.055) * 0.16 + Math.sin(frame * 0.019 + 1.7) * 0.10;
        int speed = heavy ? 26 : 20;
        int windOffset = (int) Math.round(frame * (windX + gust) * (heavy ? 12.0 : 8.5));
        double baseAngle = Math.max(-0.78, Math.min(0.78, -(windX + gust) * (heavy ? 0.72 : 0.58)));
        for (int i = 0; i < activeCount; i++) {
            int seed = Math.abs(i * 62003 + 8419);
            boolean foreground = Math.floorMod(seed / 17, 5) == 0;
            int localSpeed = speed + Math.floorMod(seed / 71, heavy ? 11 : 8);
            int x = Math.floorMod(seed + windOffset + frame * (localSpeed / 3), width + 120) - 60;
            int y = Math.floorMod(seed / 37 + frame * localSpeed, height + 96) - 48;
            int drawW = Math.max(2, scaled((foreground ? 5 : 3) + Math.floorMod(seed / 17, heavy ? 4 : 3)));
            int drawH = Math.max(11, scaled((foreground ? 28 : 18) + Math.floorMod(seed / 31, heavy ? 20 : 13)));
            double angle = baseAngle + (Math.floorMod(seed / 101, 9) - 4) * 0.012;
            float alpha = (heavy ? 0.52f : 0.42f)
                    + (foreground ? 0.18f : 0.0f)
                    + Math.floorMod(seed / 43, 18) / 100.0f;
            drawParticleSprite(g, drop, x, y, drawW, drawH, angle, (float) (Math.min(0.78f, alpha) * intensity));
        }
    }

    private void drawRainVeil(Graphics2D g, int width, int height, boolean heavy, double intensity) {
        BufferedImage sheet = particleSprite(heavy ? "rain_sheet_heavy" : "rain_sheet");
        double windX = Math.cos(state.windRadians()) * state.windStrength();
        int tileW = scaled(192);
        int tileH = scaled(192);
        int offsetX = Math.floorMod((int) Math.round(frame * (windX * 9.0 + 2.0)), tileW);
        int offsetY = Math.floorMod(frame * (heavy ? 18 : 13), tileH);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) ((heavy ? 0.30f : 0.22f) * intensity)));
        for (int y = -tileH - offsetY; y < height + tileH; y += tileH) {
            for (int x = -tileW - offsetX; x < width + tileW; x += tileW) {
                g.drawImage(sheet, x, y, tileW, tileH, null);
            }
        }
    }

    private void drawRainSplashes(Graphics2D g, int width, int height, int count, boolean heavy, double intensity) {
        int activeCount = (int) Math.round(count * intensity);
        if (activeCount <= 0) {
            return;
        }
        BufferedImage splash = particleSprite("rain_splash");
        double windX = Math.cos(state.windRadians()) * state.windStrength();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) ((heavy ? 0.36f : 0.26f) * intensity)));
        for (int i = 0; i < activeCount; i++) {
            int seed = Math.abs(i * 51437 + 2719);
            int x = Math.floorMod(seed + (int) Math.round(frame * windX * 4.0), width + 50) - 25;
            int y = Math.floorMod(seed / 29 + frame * (heavy ? 5 : 3), height + 40) - 20;
            int phase = Math.floorMod(frame + seed, 18);
            if (phase > 8) {
                continue;
            }
            int drawW = scaled(16 + Math.floorMod(seed / 47, 16));
            int drawH = scaled(6 + phase / 2);
            float alpha = (float) ((heavy ? 0.34f : 0.24f) * (1.0f - phase / 10.0f) * intensity);
            drawParticleSprite(g, splash, x, y, drawW, drawH, 0.0, alpha);
        }
    }

    private void drawSnow(Graphics2D g, int width, int height, int count, boolean heavy, double intensity) {
        int activeCount = (int) Math.round(count * intensity);
        if (activeCount <= 0) {
            return;
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) ((heavy ? 0.64f : 0.46f) * intensity)));
        BufferedImage flake = particleSprite(heavy ? "snow_heavy" : "snow");
        int speed = heavy ? 4 : 2;
        for (int i = 0; i < activeCount; i++) {
            int seed = Math.abs(i * 73241 + 11939);
            int drift = (int) Math.round(Math.sin((frame + i * 17) * 0.04) * scaled(heavy ? 18 : 9));
            int x = Math.floorMod(seed + drift, width + 40) - 20;
            int y = Math.floorMod(seed / 29 + frame * speed, height + 30) - 15;
            int size = Math.max(2, scaled((heavy ? 7 : 5) + Math.floorMod(seed / 43, 5)));
            g.drawImage(flake, x, y, size, size, null);
        }
    }

    private void drawFog(Graphics2D g, int width, int height) {
        int fogW = Math.max(1, (width + FOG_RENDER_SCALE - 1) / FOG_RENDER_SCALE);
        int fogH = Math.max(1, (height + FOG_RENDER_SCALE - 1) / FOG_RENDER_SCALE);
        if (fogBuffer == null || fogBuffer.getWidth() != fogW || fogBuffer.getHeight() != fogH) {
            fogBuffer = new BufferedImage(fogW, fogH, BufferedImage.TYPE_INT_ARGB);
        }

        Graphics2D fog = fogBuffer.createGraphics();
        fog.setComposite(AlphaComposite.Clear);
        fog.fillRect(0, 0, fogW, fogH);
        fog.setComposite(AlphaComposite.SrcOver);
        fog.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        fog.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        fog.scale(1.0 / FOG_RENDER_SCALE, 1.0 / FOG_RENDER_SCALE);
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
        double windX = Math.cos(state.windRadians()) * state.windStrength();
        int spill = scaled(260);
        int drawW = width + spill * 2;
        int drawH = scaled(118);
        for (int i = 0; i < 4; i++) {
            int seed = Math.abs(i * 37321 + 4127);
            int y = Math.floorMod(seed / 31, Math.max(1, height + drawH)) - drawH / 2;
            int drift = (int) Math.round(Math.sin((frame + i * 43) * 0.012) * scaled(70)
                    + frame * (0.28 + windX * 0.8 + i * 0.03));
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
        double windX = Math.cos(state.windRadians()) * state.windStrength();
        for (int i = 0; i < 10; i++) {
            int seed = Math.abs(i * 77687 + 3319);
            boolean foreground = Math.floorMod(seed / 23, 4) == 0;
            int drawW = scaled((foreground ? 420 : 280) + Math.floorMod(seed, foreground ? 270 : 210));
            int drawH = scaled((foreground ? 72 : 42) + Math.floorMod(seed / 37, foreground ? 52 : 34));
            int yRange = Math.max(1, height - scaled(22));
            int y = Math.floorMod(seed / 73 + (int) Math.round(Math.sin((frame + i * 19) * 0.025) * scaled(13)), yRange) - scaled(10);
            int speed = foreground ? 1 : 0;
            int x = Math.floorMod(frame * speed + (int) Math.round(frame * windX * 1.2) + seed,
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
        double windX = Math.cos(state.windRadians()) * state.windStrength();
        int tileW = Math.max(1, scaled(256));
        int tileH = Math.max(1, scaled(192));
        int offsetX = Math.floorMod((int) Math.round(frame * (10.0 + windX * 18.0)), tileW);
        int offsetY = Math.floorMod(frame * 2, tileH);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.34f));
        for (int y = -tileH - offsetY; y < height + tileH; y += tileH) {
            for (int x = -tileW - offsetX; x < width + tileW; x += tileW) {
                g.drawImage(veil, x, y, tileW, tileH, null);
            }
        }
    }

    private void drawSandStreaks(Graphics2D g, int width, int height) {
        BufferedImage streak = particleSprite("sand_streak");
        double windX = Math.cos(state.windRadians()) * state.windStrength();
        double gust = Math.sin(frame * 0.043) * 0.22 + Math.sin(frame * 0.017 + 2.1) * 0.12;
        double lean = -0.22 - (windX + gust) * 0.18;
        for (int i = 0; i < 125; i++) {
            int seed = Math.abs(i * 97561 + 1237);
            boolean foreground = Math.floorMod(seed / 19, 5) == 0;
            int speed = foreground ? 15 : 10;
            int drawW = scaled((foreground ? 88 : 46) + Math.floorMod(seed / 11, foreground ? 88 : 46));
            int drawH = scaled((foreground ? 8 : 4) + Math.floorMod(seed / 83, foreground ? 9 : 5));
            int x = Math.floorMod(seed + frame * speed + (int) Math.round(frame * (windX + gust) * 16.0),
                    width + drawW + scaled(160)) - drawW - scaled(80);
            int y = Math.floorMod(seed / 31 + frame * (foreground ? 2 : 1), height + scaled(70)) - scaled(35);
            float alpha = foreground ? 0.42f : 0.25f;
            drawParticleSprite(g, streak, x, y, drawW, drawH, lean, alpha);
        }
    }

    private void drawSandMotes(Graphics2D g, int width, int height) {
        BufferedImage mote = particleSprite("dust");
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.42f));
        for (int i = 0; i < 108; i++) {
            int seed = Math.abs(i * 86311 + 4201);
            int speed = 3 + Math.floorMod(seed / 71, 5);
            int x = Math.floorMod(seed + frame * speed, width + 110) - 55;
            int y = Math.floorMod(seed / 41 + frame + (int) Math.round(Math.sin((frame + i * 13) * 0.035) * scaled(12)), height + 40) - 20;
            int drawW = scaled(14 + Math.floorMod(seed / 23, 34));
            int drawH = scaled(4 + Math.floorMod(seed / 97, 9));
            drawParticleSprite(g, mote, x, y, drawW, drawH, -0.12, 0.24f + Math.floorMod(seed / 31, 16) / 100.0f);
        }
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

    private void drawSpriteAlpha(Graphics2D g, BufferedImage sprite, int x, int y, int width, int height, float alpha) {
        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.drawImage(sprite, x, y, width, height, null);
        g.setComposite(oldComposite);
    }

    private void drawParticleSprite(Graphics2D g, BufferedImage sprite, int x, int y, int width, int height, double angle, float alpha) {
        Composite oldComposite = g.getComposite();
        AffineTransform oldTransform = g.getTransform();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
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
            case "rain_sheet" -> createRainSheetSprite(false);
            case "rain_sheet_heavy" -> createRainSheetSprite(true);
            case "rain_splash" -> createRainSplashSprite();
            case "snow" -> createSnowSprite(false);
            case "snow_heavy" -> createSnowSprite(true);
            case "fog_wisp" -> createFogWispSprite();
            case "fog_band" -> createFogBandSprite();
            case "fog_curl" -> createFogCurlSprite();
            case "dust" -> createDustSprite();
            case "sand_veil" -> createSandVeilSprite();
            case "sand_streak" -> createSandStreakSprite();
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
        int left = gameAreaWidth();
        g.setColor(new Color(18, 20, 29));
        g.fillRect(left, 0, GameConfig.SIDEBAR_WIDTH, viewHeight());
        g.setColor(new Color(59, 64, 82));
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
        g.drawString("Level " + p.level + "   Gold " + p.gold, x, y + 28);
        bar(g, x, y + 52, p.hp, p.maxHp, new Color(190, 76, 82), "HP");
        bar(g, x, y + 86, p.mp, p.maxMp, new Color(84, 129, 205), "MP");
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(198, 202, 211));
        String partyLine = state.activeAllies().isEmpty()
                ? "Party: no allies"
                : "Party: " + String.join(", ", state.activeAllies().stream().map(ally -> ally.name).toList());
        if (!state.stationedAllies().isEmpty()) {
            partyLine += " | Village: " + state.stationedAllies().size();
        }
        wrap(g, partyLine, x, y + 122, GameConfig.SIDEBAR_WIDTH - 56, 17);

        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(new Color(230, 225, 206));
        g.drawString("Environment", x, y + 158);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(198, 202, 211));
        g.drawString("Day " + state.dayNumber() + "  " + state.timeLabel() + "  " + state.dayPhaseLabel(), x, y + 183);
        g.drawString("Weather: " + state.weatherLabel() + " over " + Terrain.name(state.currentBiomeTile()), x, y + 205);
        g.drawString(state.windLabel(), x, y + 227);

        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(new Color(230, 225, 206));
        g.drawString("Position", x, y + 252);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(198, 202, 211));
        g.drawString(state.world.label(state.currentMapId), x, y + 277);
        g.drawString(state.playerX + ", " + state.playerY + "  " + Terrain.name(state.world.tileAt(state.currentMapId, state.playerX, state.playerY)), x, y + 299);

        int logY = 324;
        g.setColor(new Color(11, 13, 20, 212));
        g.fillRoundRect(left + 22, logY, GameConfig.SIDEBAR_WIDTH - 44, 92, 8, 8);
        g.setColor(new Color(74, 82, 105));
        g.drawRoundRect(left + 22, logY, GameConfig.SIDEBAR_WIDTH - 44, 92, 8, 8);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(new Color(230, 225, 206));
        g.drawString("Log", x, logY + 22);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(198, 202, 211));
        drawWrapped(g, state.status, x, logY + 45, GameConfig.SIDEBAR_WIDTH - 56, 18, 3);
        if (state.crafting.active()) {
            int progressX = left + 28;
            int progressY = logY + 74;
            int progressW = GameConfig.SIDEBAR_WIDTH - 56;
            g.setColor(new Color(35, 39, 54));
            g.fillRoundRect(progressX, progressY, progressW, 12, 8, 8);
            g.setColor(new Color(185, 135, 76));
            g.fillRoundRect(progressX, progressY, (int) Math.round(progressW * state.crafting.progress()), 12, 8, 8);
            g.setColor(new Color(231, 222, 196));
            g.drawRoundRect(progressX, progressY, progressW, 12, 8, 8);
        }

        int actionY = 432;
        sidebarButton(g, left + 28, actionY, 246, 30, "Talk / Enter", state::interact);
        sidebarButton(g, left + 28, actionY + 38, 246, 30, "Quest Log", state::toggleQuestLog);
        sidebarButton(g, left + 28, actionY + 76, 246, 30, "World Map", state::toggleWorldMap);
        sidebarButton(g, left + 28, actionY + 114, 116, 30, "Gather", state::gatherNearby);
        sidebarButton(g, left + 158, actionY + 114, 116, 30, "Craft", state::toggleCrafting);
        sidebarButton(g, left + 28, actionY + 152, 116, 30, "Skills (" + state.player.skillPoints + ")", state::toggleSkills);
        sidebarButton(g, left + 158, actionY + 152, 116, 30, "Inventory", state::toggleInventory);
        sidebarButton(g, left + 28, actionY + 190, 116, 30, "Party", state::toggleParty);
        sidebarButton(g, left + 158, actionY + 190, 116, 30, "Village", state::toggleVillage);

        int miniY = actionY + 242;
        drawMiniMap(g, left + 28, miniY, 112, 70);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(230, 225, 206));
        g.drawString("Zoom", left + 158, miniY + 12);
        sidebarButton(g, left + 158, miniY + 24, 34, 26, "-", () -> state.adjustZoom(-10));
        sidebarButton(g, left + 240, miniY + 24, 34, 26, "+", () -> state.adjustZoom(10));
        Rectangle zoomTrack = new Rectangle(left + 196, miniY + 33, 38, 8);
        Rectangle zoomBounds = new Rectangle(left + 188, miniY + 22, 54, 30);
        registerSlider(zoomBounds, zoomTrack, SliderKind.ZOOM, "zoom");
        drawSliderTrack(g, zoomTrack, (state.zoom - 70) / 80.0, sliderHovered(zoomBounds), sliderActive(SliderKind.ZOOM, "zoom"), 9);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(198, 202, 211));
        g.drawString(state.zoom + "%", left + 158, miniY + 66);
        sidebarButton(g, left + 158, miniY + 78, 116, 28, "Fullscreen", fullscreenToggle);
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
        int width = 390;
        int height = 150;
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
        tip.setColor(new Color(125, 136, 172));
        tip.drawRoundRect(x, y, width, height, 8, 8);
        tip.setFont(new Font("SansSerif", Font.BOLD, 15));
        tip.setColor(new Color(246, 224, 151));
        tip.drawString(zone.title(), x + 14, y + 26);
        tip.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tip.setColor(new Color(220, 224, 232));
        drawWrapped(tip, zone.body(), x + 14, y + 50, width - 28, 18, 5);
        tip.dispose();
    }

    private String attackTooltip(Actor actor, Actor target) {
        int min = Math.max(1, actor.attack - 2);
        int max = actor.attack + 4;
        String targetText = target == null ? "the selected enemy" : target.name + " (DEF " + target.defense + ")";
        int estimatedMin = target == null ? min : Math.max(1, min - target.defense);
        int estimatedMax = target == null ? max : Math.max(1, max - target.defense);
        return "Basic attack rolls ATK-2 to ATK+4: " + min + "-" + max
                + " raw. Against " + targetText + ", current estimate is " + estimatedMin + "-" + estimatedMax
                + " before status modifiers such as Weak, Vulnerable, Fortified, or Shield.";
    }

    private String abilityTooltip(Actor actor, Ability ability, Battle battle) {
        Actor enemy = battle.selectedEnemy();
        Actor ally = battle.selectedPartyMember();
        int spellcraft = actor == state.player ? state.player.skillRank("spellcraft") * 2 + state.player.skillRank("overchannel") * 3 : 0;
        int volley = actor == state.player && "all_enemies".equals(ability.target()) ? state.player.skillRank("volley_mastery") * 2 : 0;
        int channeling = actor == state.player ? state.player.skillRank("channeling") * 4 : 0;
        List<AbilityStatus> statuses = GameData.statusHintsForAbility(ability.name(), ability.kind());
        String statusText = statuses.isEmpty()
                ? ""
                : " Applies: " + String.join(", ", statuses.stream().map(status -> statusName(status.key())).toList()) + ".";
        return switch (ability.kind()) {
            case DAMAGE -> {
                int base = ability.power() + actor.attack / 2 + spellcraft + volley;
                int rawMin = base;
                int rawMax = base + 5;
                int shownMin = enemy == null ? rawMin : Math.max(1, rawMin - enemy.defense);
                int shownMax = enemy == null ? rawMax : Math.max(1, rawMax - enemy.defense);
                String targetText = "all_enemies".equals(ability.target()) ? "each enemy" : enemy == null ? "selected enemy" : enemy.name + " DEF " + enemy.defense;
                yield "Damage spell. Formula: power " + ability.power() + " + ATK/2 " + (actor.attack / 2)
                        + (spellcraft > 0 ? " + skill power " + spellcraft : "")
                        + (volley > 0 ? " + Volley " + volley : "") + " + random 0-5. Raw "
                        + rawMin + "-" + rawMax + "; vs " + targetText + " about " + shownMin + "-" + shownMax + "." + statusText;
            }
            case HEAL -> {
                int base = ability.power() + actor.level * 2 + channeling;
                String targetText = "party".equals(ability.target()) ? "the party" : "self".equals(ability.target()) ? actor.name : ally == null ? "selected ally" : ally.name;
                yield "Healing ability for " + targetText + ". Formula: power " + ability.power() + " + level*2 "
                        + (actor.level * 2) + (channeling > 0 ? " + Channeling " + channeling : "")
                        + " + random -2 to +2. Current range " + Math.max(1, base - 2) + "-" + (base + 2) + "." + statusText;
            }
            case DEFEND -> "Defensive stance. Restores MP, applies Shield and Fortified, and reduces the next incoming strike while guard is active." + statusText;
        };
    }

    private String statusName(String key) {
        StatusEffect effect = GameData.STATUS_EFFECTS.get(key);
        if (effect == null) {
            effect = StatusEffects.ALL.get(key);
        }
        return effect == null ? key : effect.name();
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
            g.setColor(objectiveColor(objective.kind(), 255));
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
        int w = gameAreaWidth();
        int h = viewHeight();
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
        }

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
            case 'g', 'r', 'B' -> "battle_plains_backdrop";
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
            g.drawString(battle.victory ? "Victory rewards are ready." : "Return to Oathstead Camp and recover.", x + 180, y + 51);
            return;
        }
        Actor active = battle.activeActor();
        boolean canAct = battle.canAcceptInput() && active != null && active.alive();
        int buttonY = y + 22;
        actionButton(g, x + 18, buttonY, 116, 42, "Attack", state::battleAttack, new Color(92, 79, 50), new Color(156, 132, 76), canAct);
        if (active != null) {
            tooltipZones.add(new TooltipZone(new Rectangle(x + 18, buttonY, 116, 42), "Attack", attackTooltip(active, battle.selectedEnemy())));
        }
        actionButton(g, x + 148, buttonY, 104, 42, "Potion", () -> state.useItem("potion_small"), new Color(53, 82, 70), new Color(98, 151, 117), canAct && state.player.hasItem("potion_small"));
        tooltipZones.add(new TooltipZone(new Rectangle(x + 148, buttonY, 104, 42), "Small Potion", "Restores HP to the selected ally using the active party member's turn."));
        actionButton(g, x + 266, buttonY, 104, 42, "Ether", () -> state.useItem("ether"), new Color(54, 74, 103), new Color(104, 132, 176), canAct && state.player.hasItem("ether"));
        tooltipZones.add(new TooltipZone(new Rectangle(x + 266, buttonY, 104, 42), "Ether", "Restores MP to the selected ally using the active party member's turn."));
        int abilityX = x + 394;
        int abilityWidth = Math.max(112, (w - 426) / 5);
        List<Ability> abilities = active == null ? List.of() : active.abilities;
        for (int i = 0; i < Math.min(5, abilities.size()); i++) {
            Ability ability = abilities.get(i);
            int buttonX = abilityX + i * (abilityWidth + 8);
            boolean enabled = canAct && active.mp >= ability.cost();
            String label = (i + 1) + " " + ability.name() + " - " + ability.cost() + " MP";
            int index = i;
            actionButton(g, buttonX, buttonY, abilityWidth, 42, label, () -> state.battleAbility(index), new Color(76, 53, 93), new Color(139, 107, 168), enabled);
            tooltipZones.add(new TooltipZone(new Rectangle(buttonX, buttonY, abilityWidth, 42), ability.name(), abilityTooltip(active, ability, battle)));
        }
    }

    private void drawBattlePartyMember(Graphics2D g, Battle battle, Actor actor, boolean active, int panelX, int panelY, int panelW, int panelH) {
        int[] slot = battlePartyPosition(battle, actor, panelX, panelY, panelW, panelH);
        boolean selected = actor == battle.selectedPartyMember();
        int spriteW = 170;
        int spriteH = 220;
        int cardW = 178;
        int cardH = 86;
        String animationAction = battleActorAnimationAction(battle, actor);
        boolean stripAnimating = battleActorUsesStrip(actor, actor.worldSprite, animationAction);
        String renderedAction = stripAnimating ? animationAction : null;
        int renderExtraH = stripAnimating && "cast".equals(animationAction) ? 86 : 0;
        int renderH = spriteH + renderExtraH;
        int lunge = active && !stripAnimating ? battle.playerLunge : 0;
        int shake = active && !stripAnimating ? battle.playerOffset : 0;
        int bob = actor.alive() && !stripAnimating ? (int) Math.round(Math.sin((frame + battle.partyMembers().indexOf(actor) * 8) * 0.18) * 3.0) : 0;
        BattleActorPose pose = stripAnimating ? BattleActorPose.REST : battleActorPose(battle, actor, false);
        int drawX = slot[0] + lunge + shake + pose.xOffset();
        int drawY = slot[1] + bob - (stripAnimating ? 0 : battle.castOffset(actor)) + pose.yOffset();
        int renderY = drawY - renderExtraH;
        if (actor.alive()) {
            buttons.add(new UiButton(new Rectangle(drawX - 18, drawY - 10, spriteW + 36, spriteH + cardH + 22), "party-target:" + actor.name, () -> state.selectBattlePartyMember(battle.partyMembers().indexOf(actor))));
        }
        drawShadow(g, drawX + 22, drawY + spriteH - 28, spriteW - 44, 24);
        drawBattleActorSprite(
                g,
                battleActorImage(battle, actor, actor.worldSprite, renderedAction, spriteW, renderH),
                drawX,
                renderY,
                spriteW,
                renderH,
                pose,
                1.0f
        );
        if (selected && actor.alive()) {
            g.setColor(new Color(132, 190, 255, 190));
            g.setStroke(new BasicStroke(3f));
            g.drawOval(drawX + 10, drawY + spriteH - 38, spriteW - 20, 34);
        }
        if (battle.isCasting(actor)) {
            g.setColor(new Color(156, 204, 255, 150));
            g.setStroke(new BasicStroke(3f));
            int pulse = 8 + (frame % 12);
            g.drawOval(drawX + 18 - pulse / 2, drawY + spriteH - 36 - pulse / 3, spriteW - 36 + pulse, 28 + pulse / 2);
        } else if (active && actor.alive()) {
            g.setColor(new Color(246, 224, 151, 170));
            g.setStroke(new BasicStroke(2f));
            g.drawOval(drawX + 18, drawY + spriteH - 34, spriteW - 36, 26);
        }
        int cardX = drawX + (spriteW - cardW) / 2;
        int cardY = drawY + spriteH - 4;
        g.setColor(new Color(12, 14, 22, 205));
        g.fillRoundRect(cardX, cardY, cardW, cardH, 8, 8);
        g.setColor(selected ? new Color(132, 190, 255) : active ? new Color(246, 224, 151) : new Color(86, 98, 128));
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

    private void drawBattleEnemy(Graphics2D g, Battle battle, Actor foe, int panelX, int panelY, int panelW, int panelH) {
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
        BattleActorPose pose = stripAnimating ? BattleActorPose.REST : battleActorPose(battle, foe, true);
        int drawX = slot[0] + (baseSpriteW - spriteW) / 2 + shake - lunge + pose.xOffset();
        int drawY = slot[1] + (baseSpriteH - spriteH) + bob - (stripAnimating ? 0 : battle.castOffset(foe)) + pose.yOffset();
        int renderY = drawY - renderExtraH;
        if (foe.alive()) {
            buttons.add(new UiButton(new Rectangle(drawX - 18, drawY - 10, spriteW + 36, spriteH + cardH + 22), "enemy-target:" + foe.name, () -> state.selectBattleEnemy(index)));
        }
        float fade = Math.max(0.0f, Math.min(1.0f, battle.enemyFade(foe) / 255.0f));
        drawShadow(g, drawX + 30, drawY + spriteH - 30, spriteW - 60, 26);
        drawBattleActorSprite(
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
            g.setColor(new Color(255, 220, 150, 210));
            g.setStroke(new BasicStroke(3f));
            g.drawOval(drawX + 18, drawY + spriteH - 40, spriteW - 36, 34);
        } else if (battle.isCasting(foe)) {
            g.setColor(new Color(200, 154, 255, 170));
            g.setStroke(new BasicStroke(3f));
            int pulse = 8 + (frame % 12);
            g.drawOval(drawX + 24 - pulse / 2, drawY + spriteH - 38 - pulse / 3, spriteW - 48 + pulse, 30 + pulse / 2);
        } else if (foe.alive() && foe == battle.livingEnemies().stream().findFirst().orElse(null)) {
            g.setColor(new Color(255, 220, 150, 180));
            g.setStroke(new BasicStroke(2f));
            g.drawOval(drawX + 26, drawY + spriteH - 36, spriteW - 52, 26);
        }
        int cardX = drawX + (spriteW - cardW) / 2;
        int cardY = drawY + spriteH - 4;
        g.setColor(new Color(12, 14, 22, 205));
        g.fillRoundRect(cardX, cardY, cardW, cardH, 8, 8);
        g.setColor(selected ? new Color(255, 220, 150) : new Color(100, 76, 76));
        g.drawRoundRect(cardX, cardY, cardW, cardH, 8, 8);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(new Color(238, 239, 244));
        drawCenteredIn(g, foe.name, cardX, cardY + 22, cardW);
        smallBar(g, cardX + 14, cardY + 40, foe.hp, foe.maxHp, new Color(190, 76, 82), "HP");
        drawStatusIcons(g, battle, foe, cardX + 126, cardY + 40);
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

    private BattleActorPose battleActorPose(Battle battle, Actor actor, boolean enemySide) {
        BattleActionAnimation animation = battle.activeAnimation();
        if (animation == null || actor == null || !animation.involves(actor)) {
            return BattleActorPose.REST;
        }
        int direction = enemySide ? -1 : 1;
        boolean source = actor == animation.source;
        String kind = animation.effectKind == null ? "strike" : animation.effectKind;
        String className = actor.className == null ? "" : actor.className;
        if (source) {
            return sourceBattlePose(animation, className, kind, direction);
        }
        if (animation.stage() != BattleActionAnimation.Stage.IMPACT) {
            return BattleActorPose.REST;
        }
        double impact = Math.sin(animation.impactProgress() * Math.PI);
        return new BattleActorPose(
                (int) Math.round(-direction * impact * 14.0),
                (int) Math.round(-impact * 4.0),
                direction * impact * 0.045,
                1.0 + impact * 0.035,
                1.0 - impact * 0.025
        );
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
        if ("volley".equals(kind) || "pierce".equals(kind) || isRangedClass(actor.className)) {
            return "shoot";
        }
        if (physicalBattleEffect(kind) || isPhysicalClass(actor.className)) {
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

    private BattleActorPose sourceBattlePose(BattleActionAnimation animation, String className, String kind, int direction) {
        boolean physical = physicalBattleEffect(kind) || isPhysicalClass(className);
        boolean ranged = isRangedClass(className) || "volley".equals(kind) || "pierce".equals(kind);
        boolean magic = !physical || isMagicClass(className) || magicalBattleEffect(kind);
        return switch (animation.stage()) {
            case CAST -> {
                double p = animation.castProgress();
                if (ranged) {
                    yield new BattleActorPose(
                            (int) Math.round(-direction * p * 10.0),
                            (int) Math.round(-Math.sin(p * Math.PI) * 5.0),
                            -direction * (0.08 + p * 0.05),
                            0.98,
                            1.04
                    );
                }
                if (physical) {
                    double coil = Math.sin(p * Math.PI);
                    yield new BattleActorPose(
                            (int) Math.round(-direction * coil * 12.0),
                            (int) Math.round(coil * 4.0),
                            -direction * coil * 0.085,
                            1.03,
                            0.98
                    );
                }
                double weave = Math.sin(p * Math.PI * 4.0);
                yield new BattleActorPose(
                        (int) Math.round(weave * 4.0),
                        (int) Math.round(-Math.sin(p * Math.PI) * 12.0),
                        weave * 0.06,
                        1.0 - Math.sin(p * Math.PI) * 0.02,
                        1.0 + Math.sin(p * Math.PI) * 0.05
                );
            }
            case TRAVEL -> {
                double p = animation.travelProgress();
                if (physical) {
                    double thrust = Math.sin(p * Math.PI);
                    yield new BattleActorPose(
                            (int) Math.round(direction * (10.0 + thrust * 18.0)),
                            (int) Math.round(-thrust * 3.0),
                            direction * thrust * 0.08,
                            1.04,
                            0.98
                    );
                }
                double settle = 1.0 - p;
                yield new BattleActorPose(
                        (int) Math.round(Math.sin(p * Math.PI * 2.0) * 3.0),
                        (int) Math.round(-settle * 6.0),
                        Math.sin(p * Math.PI * 2.0) * 0.025,
                        1.0,
                        1.0 + settle * 0.025
                );
            }
            case IMPACT -> {
                double p = 1.0 - animation.impactProgress();
                if (physical) {
                    yield new BattleActorPose(
                            (int) Math.round(direction * p * 12.0),
                            0,
                            direction * p * 0.04,
                            1.0,
                            1.0
                    );
                }
                yield new BattleActorPose(0, (int) Math.round(-p * 4.0), 0.0, 1.0, 1.0);
            }
            case DONE -> BattleActorPose.REST;
        };
    }

    private boolean physicalBattleEffect(String kind) {
        return switch (kind) {
            case "strike", "impact", "bash", "slash", "cleave", "fang", "claw", "volley", "pierce" -> true;
            default -> false;
        };
    }

    private boolean isPhysicalClass(String className) {
        return PHYSICAL_CLASS_NAMES.contains(className);
    }

    private boolean isRangedClass(String className) {
        return RANGED_CLASS_NAMES.contains(className);
    }

    private boolean isMagicClass(String className) {
        return MAGIC_CLASS_NAMES.contains(className);
    }

    private boolean magicalBattleEffect(String kind) {
        return switch (kind) {
            case "heal", "regeneration", "shield", "ward", "fire", "burn", "ember", "spark", "radiant",
                    "holy", "lightning", "arcane", "rune", "void", "frost", "poison", "acid", "web",
                    "thorn", "shadow", "sonic", "bone", "dust", "howl", "item" -> true;
            default -> false;
        };
    }

    private void drawBattleActorSprite(Graphics2D g, BufferedImage image, int x, int y, int width, int height, BattleActorPose pose, float alpha) {
        Graphics2D spriteG = (Graphics2D) g.create();
        spriteG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.0f, Math.min(1.0f, alpha))));
        double pivotX = x + width / 2.0;
        double pivotY = y + height * 0.86;
        spriteG.translate(pivotX, pivotY);
        spriteG.rotate(pose.rotation());
        spriteG.scale(pose.scaleX(), pose.scaleY());
        spriteG.drawImage(image, (int) Math.round(-width / 2.0), (int) Math.round(-height * 0.86), width, height, null);
        spriteG.dispose();
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
        if (drawImageBattleEffect(fx, kind, source, target, alpha, phase)) {
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
        fx.dispose();
    }

    private void drawCastBattleEffect(Graphics2D g, int[] source, double progress, boolean hostile) {
        float alpha = (float) (0.25 + Math.sin(progress * Math.PI) * 0.5);
        int radius = 30 + (int) Math.round(progress * 18);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.05f, Math.min(0.8f, alpha))));
        g.setColor(hostile ? new Color(195, 124, 255) : new Color(126, 205, 255));
        g.drawOval(source[0] - radius, source[1] - radius, radius * 2, radius * 2);
        g.drawOval(source[0] - radius / 2, source[1] - radius / 2, radius, radius);
        g.drawLine(source[0] - radius - 6, source[1], source[0] - radius / 2, source[1]);
        g.drawLine(source[0] + radius / 2, source[1], source[0] + radius + 6, source[1]);
        g.setComposite(AlphaComposite.SrcOver);
    }

    private boolean drawImageBattleEffect(Graphics2D g, String kind, int[] source, int[] target, float alpha, double phase) {
        String sprite = effectSpriteName(kind);
        if (sprite == null) {
            return false;
        }
        double angle = effectTravelAngle(source, target);
        phase = Math.max(0.0, Math.min(1.0, phase));
        double pulse = 0.92 + Math.sin((frame + phase * 20.0) * 0.32) * 0.08;
        switch (kind) {
            case "heal", "regeneration" -> {
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
            case "arcane", "rune" -> {
                drawTravelingSprite(g, sprite, source, target, angle, phase, 108, 82, alpha * 0.68f);
                drawEffectSprite(g, sprite, target[0], target[1] + 6, effectSize(156, pulse), effectSize(112, pulse), 0.0, alpha * 0.86f);
                return true;
            }
            case "ember" -> {
                drawTravelingSprite(g, sprite, source, target, angle, phase, 132, 88, alpha);
                drawEffectSprite(g, sprite, target[0], target[1], 96, 72, correctedEffectAngle(sprite, angle), alpha * 0.58f);
                return true;
            }
            case "void" -> {
                drawTravelingSprite(g, sprite, source, target, angle, phase, 112, 92, alpha * 0.72f);
                drawEffectSprite(g, sprite, target[0], target[1], effectSize(144, pulse), effectSize(128, pulse), 0.0, alpha * 0.88f);
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

    private void drawTravelingSprite(Graphics2D g, String sprite, int[] source, int[] target, double angle, double phase, int width, int height, float alpha) {
        double travel = 0.18 + phase * 0.68;
        int x = (int) Math.round(source[0] + (target[0] - source[0]) * travel);
        int y = (int) Math.round(source[1] + (target[1] - source[1]) * travel);
        double drawAngle = correctedEffectAngle(sprite, angle);
        drawEffectSprite(g, sprite, x, y, width, height, drawAngle, alpha);
        int trailX = (int) Math.round(source[0] + (target[0] - source[0]) * Math.max(0.05, travel - 0.12));
        int trailY = (int) Math.round(source[1] + (target[1] - source[1]) * Math.max(0.05, travel - 0.12));
        drawEffectSprite(g, sprite, trailX, trailY, (int) (width * 0.72), (int) (height * 0.72), drawAngle, alpha * 0.28f);
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
            default -> 0.0;
        };
    }

    private void drawEffectSprite(Graphics2D g, String sprite, int centerX, int centerY, double width, double height, double angle, float alpha) {
        int drawW = Math.max(1, (int) Math.round(width));
        int drawH = Math.max(1, (int) Math.round(height));
        int animationFrame = frame / 2 + Math.floorMod(sprite.hashCode(), 6);
        BufferedImage image = assets.effectSprite(sprite, drawW, drawH, animationFrame);
        Graphics2D spriteG = (Graphics2D) g.create();
        spriteG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.0f, Math.min(1.0f, alpha))));
        spriteG.translate(centerX, centerY);
        spriteG.rotate(angle);
        spriteG.drawImage(image, -drawW / 2, -drawH / 2, drawW, drawH, null);
        spriteG.dispose();
    }

    private String effectSpriteName(String kind) {
        return switch (kind) {
            case "heal", "regeneration" -> "fx_heal";
            case "shield", "ward" -> "fx_shield";
            case "fire", "burn" -> "fx_fire";
            case "ember" -> "fx_ember";
            case "spark", "radiant", "item" -> "fx_spark";
            case "holy" -> "fx_holy";
            case "lightning" -> "fx_lightning";
            case "arcane", "rune" -> "fx_arcane";
            case "void" -> "fx_void_hit";
            case "frost" -> "fx_frost";
            case "poison", "acid" -> "fx_poison";
            case "web" -> "fx_web";
            case "thorn" -> "fx_thorn";
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
        return switch (kind) {
            case "heal", "regeneration" -> "heal";
            case "shield", "ward" -> "shield";
            case "fire", "burn", "ember" -> "fire";
            case "frost" -> "frost";
            case "poison", "acid", "web", "thorn" -> "poison";
            case "volley", "pierce" -> "arrow";
            case "slash", "fang", "strike", "cleave", "claw", "impact", "bash" -> "slash";
            case "shadow", "sonic", "bone", "dust", "howl", "spark", "radiant", "holy", "lightning",
                    "arcane", "rune", "void" -> "impact";
            default -> "impact";
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
        index = Math.min(3, index);
        int count = Math.max(1, Math.min(4, battle.enemies().size()));
        int right = panelX + panelW;
        int enemyLeft = right - 650;
        int enemyRight = right - 370;
        int high = panelY + 122;
        int mid = panelY + 326;
        int low = panelY + 530;
        int[][] one = {{right - 500, mid}};
        int[][] two = {{enemyRight, panelY + 206}, {enemyRight, panelY + 454}};
        int[][] three = {{enemyRight, high}, {enemyRight, low - 36}, {enemyLeft, mid - 6}};
        int[][] four = {{enemyLeft, high}, {enemyRight, high}, {enemyLeft, low}, {enemyRight, low}};
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
        int panelW = Math.min(1120, gameAreaWidth() - 96);
        int panelH = 430;
        int panelX = gameAreaCenteredX(panelW);
        int panelY = viewHeight() - panelH - 76;
        drawOverlayBase(g, panelX, panelY, panelW, panelH);
        drawNpcPortraitCard(g, npc, panelX + 26, panelY + 32, 118, 132);

        int textX = panelX + 166;
        int textW = panelW - 214;
        g.setFont(new Font("SansSerif", Font.BOLD, 25));
        g.setColor(new Color(244, 239, 220));
        g.drawString(npc.name(), textX, panelY + 54);
        FontMetrics nameMetrics = g.getFontMetrics();
        tooltipZones.add(new TooltipZone(new Rectangle(textX, panelY + 30, nameMetrics.stringWidth(npc.name()), 30),
                npc.name(), npcTooltip(npc)));
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.setColor(new Color(218, 220, 226));
        String line = state.activeNpcDialogLine();
        drawWrapped(g, line, textX, panelY + 88, textW, 24, 3);
        Quest quest = npc.questId() == null ? null : state.quests.get(npc.questId());
        int questY = panelY + 166;
        if (quest != null) {
            g.setColor(new Color(14, 17, 25, 190));
            g.fillRoundRect(textX - 2, questY - 22, textW, 52, 8, 8);
            Color questColor = quest.completed
                    ? new Color(144, 215, 150)
                    : quest.ready()
                    ? new Color(245, 214, 117)
                    : quest.accepted ? new Color(164, 211, 255) : new Color(246, 224, 151);
            g.setColor(questColor);
            g.drawRoundRect(textX - 2, questY - 22, textW, 52, 8, 8);
            String questText = quest.completed
                    ? "Quest complete"
                    : quest.ready()
                    ? "Ready to turn in: " + quest.title
                    : quest.accepted
                    ? quest.title + " " + quest.progress + "/" + quest.needed + " - " + quest.objectiveAction() + " " + quest.target
                    : "Quest available: " + quest.title;
            g.setFont(new Font("SansSerif", Font.BOLD, 15));
            g.setColor(questColor);
            drawWrapped(g, questText, textX + 12, questY, textW - 24, 18, 1);
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.setColor(new Color(198, 202, 211));
            String helper = quest.completed
                    ? "Completed with " + npc.name() + "."
                    : quest.ready()
                    ? "Return complete. Claim the reward here."
                    : quest.accepted
                    ? "Progress is saved. Return here when ready."
                    : "Accept to add it to the quest log.";
            drawWrapped(g, helper, textX + 12, questY + 20, textW - 24, 16, 1);
        }

        List<DialogOption> options = dialogOptions(npc, quest);
        int optionsY = quest == null ? panelY + 198 : panelY + 240;
        int actionY = panelY + panelH - 48;
        g.setColor(new Color(196, 198, 205));
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.drawString("Conversation options", textX, optionsY - 12);
        int listW = Math.min(760, textW);
        int listH = Math.max(92, actionY - optionsY - 14);
        g.setColor(new Color(8, 11, 18, 145));
        g.fillRoundRect(textX, optionsY, listW, listH, 8, 8);
        g.setColor(new Color(86, 98, 128, 150));
        g.drawRoundRect(textX, optionsY, listW, listH, 8, 8);
        int gap = 4;
        int columns = options.size() > 5 ? 2 : 1;
        int rows = Math.max(1, (options.size() + columns - 1) / columns);
        int buttonH = Math.max(22, Math.min(30, (listH - 14 - gap * Math.max(0, rows - 1)) / rows));
        int rowW = (listW - 16 - gap * Math.max(0, columns - 1)) / columns;
        for (int i = 0; i < options.size(); i++) {
            DialogOption option = options.get(i);
            int col = i / rows;
            int row = i % rows;
            int rowX = textX + 8 + col * (rowW + gap);
            int rowY = optionsY + 7 + row * (buttonH + gap);
            drawDialogOptionRow(g, rowX, rowY, rowW, buttonH, i + 1, option);
        }

        String questActionLabel = questActionLabel(quest);
        if (npc.recruitId() != null && npc.recruitCost() > 0 && !state.isRecruited(npc.recruitId())) {
            g.setColor(new Color(196, 198, 205));
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.drawString("Contract available: " + npc.recruitCost() + "g", textX, actionY + 21);
        } else if (npc.recruitId() != null && state.isRecruited(npc.recruitId())) {
            g.setColor(new Color(144, 215, 150));
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.drawString("Travels with you", textX, actionY + 21);
        }
        g.setColor(new Color(196, 198, 205));
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        String helper = questActionLabel == null
                ? "Enter/E advances the current topic. Number keys select options."
                : "Enter/E advances, or choose " + questActionLabel + " from the options.";
        g.drawString(helper, panelX + panelW - 430, actionY + 21);
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
        String questActionLabel = questActionLabel(quest);
        if (questActionLabel != null) {
            options.add(new DialogOption(questActionLabel + " quest", state::handleActiveNpcQuestAction,
                    new Color(88, 73, 44), new Color(169, 137, 74), true));
        }
        if (activeDialogWillOpenShop(npc)) {
            options.add(new DialogOption("Open shop", state::openActiveShop,
                    new Color(52, 79, 92), new Color(92, 140, 160), true));
        }
        if (npc.recruitId() != null && npc.recruitCost() > 0 && !state.isRecruited(npc.recruitId())) {
            options.add(new DialogOption("Hire " + npc.recruitCost() + "g", state::hireActiveRecruit,
                    new Color(69, 62, 88), new Color(125, 107, 166), true));
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
        return state.activeShop != null;
    }

    private String npcTooltip(Npc npc) {
        List<String> parts = new ArrayList<>();
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
        return "Townsperson";
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
            return "Accept";
        }
        if (quest.ready()) {
            return "Turn In";
        }
        return "Ask";
    }

    private void drawQuestLog(Graphics2D g) {
        int panelW = 740;
        int panelX = gameAreaCenteredX(panelW);
        drawOverlayBase(g, panelX, 110, panelW, 650);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(244, 239, 220));
        g.drawString("Quest Log", panelX + 40, 160);
        int y = 210;
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        for (Quest quest : state.quests.values()) {
            if (!quest.accepted && !quest.completed) {
                continue;
            }
            g.setColor(quest.completed ? new Color(144, 215, 150) : new Color(246, 224, 151));
            g.setFont(new Font("SansSerif", Font.BOLD, 17));
            g.drawString(quest.title + "  " + quest.progress + "/" + quest.needed, panelX + 40, y);
            y += 24;
            g.setFont(new Font("SansSerif", Font.PLAIN, 15));
            g.setColor(new Color(210, 213, 222));
            wrap(g, quest.description, panelX + 40, y, 620, 20);
            y += 42;
            if (quest.ready()) {
                g.setColor(new Color(245, 214, 117));
                String returnLine = "Return to " + state.questGiverName(quest.id) + " in " + state.questReturnLocation(quest.id) + ".";
                wrap(g, returnLine, panelX + 40, y, 620, 18);
                y += 28;
            } else if (quest.hasWorldObjective()) {
                g.setColor(new Color(164, 211, 255));
                String objectiveLine = quest.objectiveAction() + " marked objective: " + quest.target;
                wrap(g, objectiveLine, panelX + 40, y, 620, 18);
                y += 28;
            } else {
                y += 12;
            }
        }
        if (y == 210) {
            g.setColor(new Color(210, 213, 222));
            g.drawString("No accepted quests yet.", panelX + 40, y);
        }
        g.setColor(new Color(196, 198, 205));
        g.drawString("Q or Esc close", panelX + 40, 725);
    }

    private void drawSkillTree(Graphics2D g) {
        int x = gameAreaCenteredX(1180);
        int y = 72;
        int w = 1180;
        int h = 760;
        drawOverlayBase(g, x, y, w, h);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(244, 239, 220));
        g.drawString("Skills", x + 36, y + 48);
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g.setColor(new Color(210, 213, 222));
        g.drawString(state.player.className + "  Level " + state.player.level + "  Points " + state.player.skillPoints
                + "   Wheel scrolls the active tree", x + 36, y + 78);

        drawSkillTabs(g, x + 36, y + 94, 148, false);
        if (activeSkillTab == SkillTab.PROFESSIONS) {
            drawSkillGraph(g, state.player, activeSkillTitle(state.player), activeSkillTree(state.player),
                    x + 36, y + 142, 710, 568, false);
            drawProfessionColumn(g, state.player, x + 792, y + 142, 340, 568);
        } else {
            drawSkillGraph(g, state.player, activeSkillTitle(state.player), activeSkillTree(state.player),
                    x + 36, y + 142, 1096, 568, false);
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
        };
    }

    private Map<String, SkillNode> activeSkillTree(Actor actor) {
        return switch (activeSkillTab) {
            case SURVIVAL -> SkillTrees.COMMON_SKILL_TREE;
            case CLASS -> SkillTrees.skillTreeForClass(actor.className);
            case PROFESSIONS -> SkillTrees.professionSkillTree(activeProfessionId);
        };
    }

    private void drawSkillGraph(Graphics2D g, Actor actor, String title, Map<String, SkillNode> tree,
                                int x, int y, int w, int h, boolean party) {
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
        int gapX = party ? 14 : 24;
        int cardW = Math.max(party ? 104 : 130, (w - 24 - gapX * Math.max(0, columns - 1)) / columns);
        int cardH = party ? 52 : 64;
        int rowH = party ? 84 : 102;
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
        graphG.setStroke(new BasicStroke(2f));
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
                graphG.setColor(unlocked ? new Color(103, 151, 117, 180) : new Color(74, 80, 100, 150));
                int fromX = from.x + from.width / 2;
                int fromY = from.y + from.height;
                int toX = to.x + to.width / 2;
                int toY = to.y;
                int midY = fromY + Math.max(10, (toY - fromY) / 2);
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
                        }, party);
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
        g.setColor(learned ? new Color(38, 50, 45, 232) : new Color(28, 32, 43, 232));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(learned ? new Color(103, 151, 117) : canLearn ? new Color(96, 106, 133) : new Color(65, 68, 82));
        g.drawRoundRect(x, y, w, h, 8, 8);
        g.setFont(new Font("SansSerif", Font.BOLD, compact ? 11 : 13));
        g.setColor(new Color(235, 236, 240));
        drawClippedString(g, node.name(), x + 10, y + (compact ? 16 : 18), w - 58);
        g.setFont(new Font("SansSerif", Font.BOLD, compact ? 10 : 11));
        g.setColor(new Color(246, 224, 151));
        g.drawString(rank + "/" + node.maxRank(), x + 10, y + (compact ? 32 : 36));
        g.setFont(new Font("SansSerif", Font.PLAIN, compact ? 10 : 11));
        g.setColor(new Color(176, 182, 196));
        drawClippedString(g, skillNodeStatus(actor, node), x + 38, y + (compact ? 32 : 36), w - 96);
        tooltipZones.add(new TooltipZone(new Rectangle(x, y, w, h), node.name(), skillNodeTooltip(actor, node)));
        actionButton(g, x + w - (compact ? 42 : 50), y + 8, compact ? 30 : 36, h - 16, "+", allocate, new Color(68, 90, 53), new Color(110, 139, 92), canLearn);
    }

    private String skillNodeStatus(Actor actor, SkillNode node) {
        if (actor.skillRank(node.id()) >= node.maxRank()) {
            return "Mastered";
        }
        for (String required : node.requires()) {
            if (actor.skillRank(required) <= 0) {
                SkillNode requiredNode = SkillTrees.SKILL_TREE.get(required);
                return "Needs " + (requiredNode == null ? required : requiredNode.name());
            }
        }
        if (node.ability() != null) {
            return "Unlocks " + node.ability().name();
        }
        return "Hover for details";
    }

    private String skillNodeTooltip(Actor actor, SkillNode node) {
        List<String> lines = new ArrayList<>();
        lines.add(node.description());
        if (node.ability() != null) {
            Ability ability = node.ability();
            lines.add("Unlocks: " + ability.name() + " (" + ability.kind() + ", cost " + ability.cost() + ").");
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
        return String.join(" ", lines);
    }

    private boolean canShowAllocate(SkillNode node) {
        return canShowAllocate(state.player, node);
    }

    private boolean canShowAllocate(Actor actor, SkillNode node) {
        if (actor.skillPoints <= 0 || actor.skillRank(node.id()) >= node.maxRank()) {
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
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(198, 202, 211));
        g.drawString(actor.className + "  Lv " + actor.level + "   ATK " + actor.attack + "   DEF " + actor.defense
                + "   Skill Points " + actor.skillPoints, panelX + 36, panelY + 76);
        g.drawString("Professions: " + shortText(professionSummary(actor), 96), panelX + 36, panelY + 98);
        drawInventoryPartySelector(g, panelX + 286, panelY + 24, panelW - 326);

        int modelX = panelX + 36;
        int modelY = panelY + 124;
        int modelW = 430;
        drawInventoryCharacter(g, actor, modelX, modelY, modelW, panelH - 230);

        int packX = modelX + modelW + 30;
        int packY = modelY;
        int packW = panelX + panelW - 40 - packX;
        int packH = panelH - 230;
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
        Equipment equipment = GameData.EQUIPMENT.get(itemKey);
        Item item = GameData.ITEMS.get(itemKey);
        boolean craftingOnly = CraftingSystem.isCraftingOnlyItem(itemKey);
        boolean hovered = hoverPoint != null && bounds.contains(hoverPoint);
        boolean draggedOver = inventoryDragOver(bounds);
        Color border = equipment != null
                ? new Color(132, 118, 190)
                : item != null ? new Color(103, 151, 117) : new Color(125, 136, 172);
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
        tooltipZones.add(new TooltipZone(bounds, GameData.itemName(itemKey), inventoryTooltip(itemKey, count)));
    }

    private void drawCrafting(Graphics2D g) {
        int panelW = 860;
        int panelX = gameAreaCenteredX(panelW);
        int panelY = 86;
        int panelH = 620;
        drawOverlayBase(g, panelX, panelY, panelW, panelH);
        CraftingSystem.Workstation workstation = state.currentWorkstation();
        List<CraftingSystem.Recipe> recipes = state.availableCraftingRecipes();

        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(244, 239, 220));
        g.drawString("Crafting", panelX + 36, panelY + 48);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(198, 202, 211));
        g.drawString(workstation == null ? "Field Crafting" : workstation.label() + " + field recipes", panelX + 36, panelY + 76);

        int y = panelY + 124;
        int rowH = 82;
        if (recipes.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 17));
            g.setColor(new Color(210, 213, 222));
            g.drawString("No recipes available.", panelX + 36, y);
        }
        int visibleRows = 6;
        int maxScroll = Math.max(0, recipes.size() - visibleRows);
        craftingRecipeScroll = Math.min(craftingRecipeScroll, maxScroll);
        int end = Math.min(recipes.size(), craftingRecipeScroll + visibleRows);
        for (int i = craftingRecipeScroll; i < end; i++) {
            CraftingSystem.Recipe recipe = recipes.get(i);
            Rectangle row = new Rectangle(panelX + 36, y - 30, panelW - 72, 70);
            boolean affordable = CraftingSystem.canCraft(state.player, recipe) && !state.crafting.active();
            g.setColor(new Color(30, 34, 45));
            g.fillRoundRect(row.x, row.y, row.width, row.height, 6, 6);
            g.setColor(affordable ? new Color(95, 111, 82) : new Color(82, 92, 116));
            g.drawRoundRect(row.x, row.y, row.width, row.height, 6, 6);
            g.drawImage(assets.sprite(GameData.itemIcon(recipe.resultKey()), 40), row.x + 12, row.y + 14, null);
            g.setFont(new Font("SansSerif", Font.BOLD, 17));
            g.setColor(new Color(222, 225, 232));
            g.drawString((i + 1) + ". " + recipe.name(), row.x + 64, y - 4);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.setColor(new Color(176, 182, 196));
            g.drawString(shortText(recipe.description(), 68), row.x + 64, y + 14);
            g.drawString("Needs: " + shortText(CraftingSystem.requirementLabel(recipe), 58), row.x + 64, y + 30);
            actionButton(g, row.x + row.width - 102, row.y + 20, 82, 30, "Craft",
                    () -> state.craftRecipe(recipe.key()), new Color(68, 90, 53), new Color(110, 139, 92), affordable);
            tooltipZones.add(new TooltipZone(row, recipe.name(), recipe.description() + " Needs: " + CraftingSystem.requirementLabel(recipe) + "."));
            y += rowH;
        }

        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(196, 198, 205));
        String recipeHint = recipes.size() > visibleRows
                ? "Wheel scrolls recipes. Recipes consume ingredients when work begins."
                : "Recipes consume ingredients when work begins. Stay put until the progress finishes.";
        g.drawString(recipeHint, panelX + 36, panelY + panelH - 42);
        g.setColor(new Color(246, 224, 151));
        g.drawString(shortText(state.status, 92), panelX + 36, panelY + panelH - 22);
        actionButton(g, panelX + panelW - 164, panelY + panelH - 62, 124, 34, "Close", state::toggleCrafting, new Color(83, 61, 61), new Color(149, 96, 88), true);
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
        g.fillOval(x + 128, y + 404, 142, 34);
        g.drawImage(assets.spriteFit(actor.worldSprite, 190, 330), x + 104, y + 104, null);

        for (String slot : GameData.EQUIPMENT_SLOTS) {
            drawInventorySlot(g, actor, slot, inventorySlotBounds(slot, x, y));
        }
    }

    private Rectangle inventorySlotBounds(String slot, int x, int y) {
        return switch (slot) {
            case "weapon" -> new Rectangle(x + 20, y + 292, 96, 62);
            case "helmet" -> new Rectangle(x + 167, y + 70, 96, 62);
            case "pauldrons" -> new Rectangle(x + 20, y + 146, 96, 62);
            case "chestpiece" -> new Rectangle(x + 167, y + 198, 96, 62);
            case "gloves" -> new Rectangle(x + 314, y + 238, 96, 62);
            case "belt" -> new Rectangle(x + 167, y + 282, 96, 62);
            case "leggings" -> new Rectangle(x + 167, y + 350, 96, 62);
            case "boots" -> new Rectangle(x + 167, y + 428, 96, 62);
            case "ring" -> new Rectangle(x + 314, y + 338, 96, 62);
            case "necklace" -> new Rectangle(x + 314, y + 128, 96, 62);
            default -> new Rectangle(x + 20, y + 70, 96, 62);
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
            tooltipZones.add(new TooltipZone(bounds, equipment.name(), equipmentTooltip(equipment)));
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
        Equipment equipment = GameData.EQUIPMENT.get(itemKey);
        if (equipment != null) {
            return equipmentTooltip(equipment) + " Count: " + count
                    + ". Click or press the visible number key to equip the selected ally. Drag onto the matching character slot to equip.";
        }
        Item item = GameData.ITEMS.get(itemKey);
        String benefit = item == null ? "" : itemBenefit(item);
        if (CraftingSystem.isCraftingOnlyItem(itemKey)) {
            benefit = CraftingSystem.itemDetail(itemKey);
        }
        if (item == null && CraftingSystem.isCraftingOnlyItem(itemKey)) {
            return benefit + " Count: " + count + ". Used for crafting.";
        }
        return benefit + " Count: " + count
                + ". Click or press the visible number key to use. Drag onto the character model to use.";
    }

    private String equipmentTooltip(Equipment equipment) {
        String stats = String.join("  ", equipment.statLines());
        String line = stats.isBlank() ? equipment.description() : slotLabel(equipment.slot()) + "   " + stats;
        return line + " " + equipment.description();
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
        int cardH = members.size() > 5 ? 40 : 64;
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
            int spriteW = cardH <= 44 ? 28 : 38;
            int spriteH = cardH <= 44 ? 34 : 50;
            g.drawImage(assets.spriteFit(member.worldSprite, spriteW, spriteH), cardX + 8, cardY + 4, null);
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.setColor(new Color(235, 236, 240));
            g.drawString(shortText(member.name, cardH <= 44 ? 10 : 13), cardX + spriteW + 18, cardY + (cardH <= 44 ? 17 : 25));
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.setColor(new Color(176, 182, 196));
            g.drawString(shortText(member.className, 10), cardX + spriteW + 18, cardY + (cardH <= 44 ? 32 : 43));
            int index = i;
            buttons.add(new UiButton(new Rectangle(cardX, cardY, cardW, cardH), "inventory-member:" + member.name, () -> {
                state.selectPartyScreenActor(index);
                inventoryItemScroll = 0;
                partySkillScroll = 0;
                repaint();
            }));
        }
    }

    private void drawPartyOverview(Graphics2D g) {
        int w = 1020;
        int x = gameAreaCenteredX(w);
        int y = 66;
        int h = 768;
        drawOverlayBase(g, x, y, w, h);
        Actor actor = state.partyScreenActor();
        List<Actor> members = state.partyMembers();

        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(244, 239, 220));
        g.drawString("Party", x + 34, y + 48);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(198, 202, 211));
        g.drawString("Gold " + state.player.gold + "   Select an ally to manage skills.", x + 34, y + 76);

        int memberY = y + 110;
        for (int i = 0; i < members.size(); i++) {
            Actor member = members.get(i);
            boolean selected = member == actor;
            g.setColor(selected ? new Color(42, 57, 50, 235) : new Color(28, 32, 43, 235));
            g.fillRoundRect(x + 34, memberY, 176, 54, 8, 8);
            g.setColor(selected ? new Color(103, 151, 117) : new Color(82, 92, 116));
            g.drawRoundRect(x + 34, memberY, 176, 54, 8, 8);
            g.drawImage(assets.spriteFit(member.worldSprite, 38, 46), x + 44, memberY + 4, null);
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.setColor(new Color(235, 236, 240));
            g.drawString((i + 1) + ". " + member.name, x + 90, memberY + 22);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.setColor(new Color(176, 182, 196));
            g.drawString(member.className + "  Lv " + member.level, x + 90, memberY + 40);
            int index = i;
            buttons.add(new UiButton(new Rectangle(x + 34, memberY, 176, 54), "party-member:" + member.name, () -> {
                state.selectPartyScreenActor(index);
                partySkillScroll = 0;
                repaint();
            }));
            memberY += 62;
        }

        int detailX = x + 238;
        g.drawImage(assets.spriteFit(actor.worldSprite, 96, 124), detailX, y + 102, null);
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.setColor(new Color(246, 224, 151));
        g.drawString(actor.name + " the " + actor.className, detailX + 116, y + 126);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(222, 225, 232));
        g.drawString("HP " + actor.hp + "/" + actor.maxHp + "   MP " + actor.mp + "/" + actor.maxMp
                + "   ATK " + actor.attack + "   DEF " + actor.defense + "   Points " + actor.skillPoints, detailX + 116, y + 154);
        String abilities = actor.abilities.isEmpty() ? "No abilities" : String.join(", ", actor.abilities.stream().map(Ability::name).toList());
        wrap(g, "Abilities: " + abilities, detailX + 116, y + 180, 610, 18);

        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(new Color(246, 224, 151));
        g.drawString("Skill Trees", detailX, y + 248);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(176, 182, 196));
        g.drawString("Use the mouse wheel to scroll the active tree.", detailX + 94, y + 248);
        drawSkillTabs(g, detailX, y + 262, 116, true);

        drawSkillGraph(g, actor, activeSkillTitle(actor), activeSkillTree(actor), detailX, y + 306, 734, 396, true);

        int spent = SkillTrees.spent(actor.skillAllocations);
        int respecCost = SkillTrees.respecCost(actor.level, spent);
        boolean canRespec = spent > 0 && state.player.gold >= respecCost;
        actionButton(g, x + w - 330, y + h - 54, 154, 34, "Respec " + respecCost + "g", state::respecPartySkills, new Color(96, 66, 59), new Color(165, 103, 88), canRespec);
        actionButton(g, x + w - 160, y + h - 54, 120, 34, "Close", state::toggleParty, new Color(58, 72, 100), new Color(107, 126, 166), true);
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
            if (!GameData.EQUIPMENT.containsKey(key) && !GameData.ITEMS.containsKey(key)) {
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
            String action = GameData.EQUIPMENT.containsKey(key) ? "Eq" : "Use";
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
        if (lastVillageTab != state.villageTab || !lastVillagePropCategory.equals(state.selectedVillagePropCategory)) {
            villageListScroll = 0;
            lastVillageTab = state.villageTab;
            lastVillagePropCategory = state.selectedVillagePropCategory;
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
        if (state.villageTab == 2) {
            listY = drawVillagePropCategoryButtons(g, x, listY, w) + 10;
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

    private void setVillageTabFromSidebar(int tab) {
        state.setVillageTab(tab);
        villageListScroll = 0;
    }

    private void sidebarModeButton(Graphics2D g, int x, int y, int w, String label, String action, Color border) {
        actionButton(g, x, y, w, 24, label, () -> state.setVillageEditAction(action),
                action.equals(state.villageEditAction) ? new Color(57, 76, 60) : new Color(35, 39, 54),
                border, true);
    }

    private int drawVillagePropCategoryButtons(Graphics2D g, int x, int y, int w) {
        List<String> categories = VillageManager.outdoorAssetCategories();
        int buttonW = (w - 8) / 2;
        for (int i = 0; i < categories.size(); i++) {
            String category = categories.get(i);
            boolean selected = category.equals(state.selectedVillagePropCategory);
            int bx = x + (i % 2) * (buttonW + 8);
            int by = y + (i / 2) * 26;
            actionButton(g, bx, by, buttonW, 22, category, () -> {
                state.selectVillagePropCategory(category);
                villageListScroll = 0;
            }, selected ? new Color(57, 76, 60) : new Color(35, 39, 54),
                    selected ? new Color(126, 176, 95) : new Color(86, 98, 128), true);
        }
        return y + Math.max(1, (categories.size() + 1) / 2) * 26;
    }

    private void drawVillageSidebarBuildings(Graphics2D g, int x, int y, int w, int h) {
        List<VillageManager.BuildingPlan> plans = VillageManager.buildingPlans();
        int rowH = 72;
        int visibleRows = Math.max(1, h / rowH);
        int scroll = clamp(villageListScroll, 0, Math.max(0, plans.size() - visibleRows));
        villageListScroll = scroll;
        Graphics2D listG = (Graphics2D) g.create();
        listG.setClip(x, y, w, h);
        int rowY = y - scroll * rowH;
        for (VillageManager.BuildingPlan plan : plans) {
            if (rowY + rowH >= y && rowY <= y + h) {
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
        List<VillageManager.TilePlan> tiles = VillageManager.tilePlans();
        int rowH = 58;
        int visibleRows = Math.max(1, h / rowH);
        int scroll = clamp(villageListScroll, 0, Math.max(0, tiles.size() - visibleRows));
        villageListScroll = scroll;
        Graphics2D listG = (Graphics2D) g.create();
        listG.setClip(x, y, w, h);
        int rowY = y - scroll * rowH;
        for (VillageManager.TilePlan tile : tiles) {
            if (rowY + rowH >= y && rowY <= y + h) {
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
        List<VillageManager.PlaceableAsset> props = VillageManager.outdoorAssets(state.selectedVillagePropCategory);
        drawVillageSidebarAssetRows(g, x, y, w, h, props, false);
    }

    private void drawVillageSidebarInteriors(Graphics2D g, int x, int y, int w, int h) {
        drawVillageSidebarAssetRows(g, x, y, w, h, VillageManager.interiorAssets(), true);
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
            if (rowY + rowH >= y && rowY <= y + h) {
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
            if (rowY + rowH >= y && rowY <= y + h) {
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

        int listX = x + 32;
        int listY = y + 146;
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
        drawVillageAllyColumn(g, state.activeAllies(), x, y + 18, w / 2 - 18, true);
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
        int panelW = 840;
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
        if (state.activeNpc != null) {
            drawNpcPortraitCard(g, state.activeNpc, panelX + panelW - 164, panelY + 24, 116, 132);
        }

        g.setColor(new Color(96, 88, 72, 120));
        g.drawLine(panelX + 44, panelY + 122, panelX + panelW - 204, panelY + 122);

        List<String> stock = shop.availableStock(state.player.level);
        int maxRows = Math.max(1, (panelH - 220) / 74);
        int maxScroll = Math.max(0, stock.size() - maxRows);
        shopItemScroll = Math.max(0, Math.min(shopItemScroll, maxScroll));
        int end = Math.min(stock.size(), shopItemScroll + maxRows);
        int y = panelY + 168;
        for (int i = shopItemScroll; i < end; i++) {
            int buttonIndex = i;
            String itemKey = stock.get(i);
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
            g.drawString((i - shopItemScroll + 1) + ". " + GameData.itemName(itemKey), rowX + 78, y + 25);
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g.setColor(new Color(176, 182, 196));
            g.drawString(equipment == null ? itemBenefit(item) : equipmentBenefit(equipment), rowX + 78, y + 48);
            Color buyFill = affordable ? new Color(68, 90, 53) : new Color(58, 59, 66);
            actionButton(g, rowX + rowW - 154, y + 14, 118, 36, "Buy " + cost + "g", () -> state.buyShopItem(buttonIndex), buyFill, new Color(110, 139, 92), affordable);
            y += 74;
        }
        drawScrollIndicator(g, panelX + panelW - 24, panelY + 168, maxRows * 74 - 10, stock.size(), shopItemScroll, maxRows);
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
        g.drawString("Number keys buy visible items. Wheel scrolls stock. H hires when available.", panelX + 44, panelY + panelH - 32);
        actionButton(g, panelX + panelW - 176, panelY + panelH - 54, 132, 36, "Leave Shop", state::closeOverlay, new Color(83, 61, 61), new Color(149, 96, 88), true);
    }

    private void drawNpcPortraitCard(Graphics2D g, Npc npc, int x, int y, int w, int h) {
        tooltipZones.add(new TooltipZone(new Rectangle(x, y, w, h), npc.name(), npcTooltip(npc)));
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
        String level = equipment.levelRangeLine();
        return stats.isBlank()
                ? level + "   " + equipment.description()
                : level + "   " + equipment.slot() + "   " + stats;
    }

    private void drawWorldMap(Graphics2D g) {
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
        g.setColor(new Color(8, 10, 16));
        g.fillRect(mapX, mapY, mapW, mapH);
        for (int py = 0; py < mapH; py++) {
            int wy = py * WorldMap.ROWS / mapH;
            for (int px = 0; px < mapW; px++) {
                int wx = px * WorldMap.COLS / mapW;
                char tile = state.world.tileAt(WorldMap.OVERWORLD_ID, wx, wy);
                Color color = Terrain.color(tile);
                if (state.worldMapKingdoms) {
                    color = kingdomMapColor(color, state.world.kingdomAt(wx, wy));
                }
                g.setColor(color);
                g.fillRect(mapX + px, mapY + py, 1, 1);
            }
        }
        if (state.worldMapKingdoms) {
            drawKingdomLabels(g, mapX, mapY, mapW, mapH);
        }
        for (WorldMap.SettlementSite settlement : state.world.settlementSites()) {
            int dx = settlement.x() > 220 ? -78 : 12;
            drawSettlementMapMarker(g, mapX, mapY, mapW, mapH, settlement, dx, -9);
        }
        for (GameState.QuestObjective objective : state.activeQuestObjectives()) {
            if (WorldMap.OVERWORLD_ID.equals(objective.mapId()) && showQuestObjective(objective)) {
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
        int sideX = mapX + mapW + gap;
        actionButton(g, sideX, y + 18, 136, 30, state.worldMapKingdoms ? "Kingdoms On" : "Kingdoms Off",
                state::toggleWorldMapKingdoms, new Color(57, 71, 102), new Color(110, 127, 160), true);
        drawWorldMapLegend(g, sideX, y + 66, legendW);
        sidebarButton(g, sideX, y + h - 54, 116, 30, "Close", state::toggleWorldMap);
    }

    private Color kingdomMapColor(Color terrain, WorldMap.Kingdom kingdom) {
        Color kingdomColor = new Color(kingdom.colorRgb());
        double blend = 0.42;
        int r = (int) Math.round(terrain.getRed() * (1.0 - blend) + kingdomColor.getRed() * blend);
        int g = (int) Math.round(terrain.getGreen() * (1.0 - blend) + kingdomColor.getGreen() * blend);
        int b = (int) Math.round(terrain.getBlue() * (1.0 - blend) + kingdomColor.getBlue() * blend);
        return new Color(r, g, b);
    }

    private void drawKingdomLabels(Graphics2D g, int mapX, int mapY, int mapW, int mapH) {
        for (WorldMap.Kingdom kingdom : state.world.kingdoms()) {
            int px = mapX + kingdom.centerX() * mapW / WorldMap.COLS;
            int py = mapY + kingdom.centerY() * mapH / WorldMap.ROWS;
            drawMapLabel(g, kingdom.name(), px - 36, py, new Color(245, 246, 236));
        }
    }

    private String shortSettlementLabel(String label) {
        return label.replace(" City", "").replace(" Town", "").replace(" Village", "");
    }

    private void drawSettlementMapMarker(Graphics2D g, int mapX, int mapY, int mapW, int mapH,
                                         WorldMap.SettlementSite settlement, int labelDx, int labelDy) {
        int px = mapX + settlement.x() * mapW / WorldMap.COLS;
        int py = mapY + settlement.y() * mapH / WorldMap.ROWS;
        WorldMap.Kingdom kingdom = kingdomById(settlement.kingdomId());
        boolean playerSettlement = state.world.isPlayerSettlement(settlement);
        Color color = playerSettlement ? new Color(255, 226, 128)
                : kingdom == null ? new Color(245, 214, 117) : new Color(kingdom.colorRgb());
        g.setColor(new Color(8, 10, 16, 166));
        g.fillOval(px - 7, py - 7, 15, 15);
        g.setColor(color);
        if (playerSettlement) {
            g.fillOval(px - 5, py - 5, 11, 11);
        } else {
            g.fillRect(px - 4, py - 4, 9, 9);
        }
        g.setColor(new Color(245, 246, 236, 220));
        if (playerSettlement) {
            g.drawOval(px - 6, py - 6, 13, 13);
        } else {
            g.drawRect(px - 5, py - 5, 11, 11);
        }
        String label = shortSettlementLabel(settlement.label());
        drawMapLabel(g, label, px + labelDx, py + labelDy, color);
        Rectangle markerBounds = new Rectangle(Math.min(px - 8, px + labelDx - 8), Math.min(py - 16, py + labelDy - 18), 150, 28);
        tooltipZones.add(new TooltipZone(markerBounds, settlement.label(), settlementTooltip(settlement, kingdom)));
    }

    private void drawMapMarker(Graphics2D g, int mapX, int mapY, int mapW, int mapH, int wx, int wy, String label, int labelDx, int labelDy) {
        int px = mapX + wx * mapW / WorldMap.COLS;
        int py = mapY + wy * mapH / WorldMap.ROWS;
        g.setColor(new Color(245, 214, 117));
        g.fillRect(px - 3, py - 3, 7, 7);
        drawMapLabel(g, label, px + labelDx, py + labelDy, new Color(245, 214, 117));
    }

    private void drawQuestMapMarker(Graphics2D g, int mapX, int mapY, int mapW, int mapH, GameState.QuestObjective objective) {
        int px = mapX + objective.x() * mapW / WorldMap.COLS;
        int py = mapY + objective.y() * mapH / WorldMap.ROWS;
        Color color = objectiveColor(objective.kind(), 255);
        g.setColor(new Color(0, 0, 0, 155));
        g.fillOval(px - 8, py - 9, 17, 17);
        g.setColor(color);
        g.fillOval(px - 6, py - 7, 13, 13);
        Polygon tail = new Polygon(new int[]{px - 4, px + 4, px}, new int[]{py + 2, py + 2, py + 10}, 3);
        g.fillPolygon(tail);
        g.setColor(new Color(245, 246, 236));
        g.drawOval(px - 7, py - 8, 15, 15);
        g.drawPolygon(tail);
        g.setColor(new Color(18, 20, 24));
        g.setFont(new Font("SansSerif", Font.BOLD, 9));
        String glyph = switch (objective.kind()) {
            case GATHER -> "G";
            case VISIT -> "V";
            case DEFEAT -> "B";
        };
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(glyph, px - metrics.stringWidth(glyph) / 2, py + 3);
        drawMapLabel(g, objective.title(), px + 13, py - 10, new Color(245, 246, 236));
    }

    private void drawMapLabel(Graphics2D g, String label, int x, int y, Color color) {
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        FontMetrics metrics = g.getFontMetrics();
        int textW = metrics.stringWidth(label);
        int textH = metrics.getHeight();
        g.setColor(new Color(8, 10, 16, 188));
        g.fillRoundRect(x - 4, y - textH + 4, textW + 8, textH + 2, 6, 6);
        g.setColor(color);
        g.drawString(label, x, y);
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
        String seat = kingdom == null ? "" : " Seat: " + kingdom.seat() + ".";
        if (state.world.isPlayerSettlement(settlement)) {
            VillageManager.SettlementStage stage = state.world.playerVillageSettlementStage();
            return "Player settlement, Stage " + stage.stage() + "/10: " + stage.title()
                    + ". Distance from you: " + distance + " tiles. Terrain: " + Terrain.name(terrain) + ".";
        }
        return settlement.kind() + " of " + kingdomName + ". Distance from you: " + distance
                + " tiles. Terrain: " + Terrain.name(terrain) + "." + seat;
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

    private void drawWorldMapLegend(Graphics2D g, int x, int y, int width) {
        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.setColor(new Color(244, 239, 220));
        g.drawString("Legend", x, y);
        int rowY = y + 24;
        drawLegendSwatch(g, x, rowY, width, Terrain.color('g'), "Meadow");
        rowY += 24;
        drawLegendSwatch(g, x, rowY, width, Terrain.color('f'), "Oldwood");
        rowY += 24;
        drawLegendSwatch(g, x, rowY, width, Terrain.color('s'), "Sunsteppe");
        rowY += 24;
        drawLegendSwatch(g, x, rowY, width, Terrain.color('n'), "Frostfield");
        rowY += 24;
        drawLegendSwatch(g, x, rowY, width, Terrain.color('v'), "Marsh");
        rowY += 24;
        drawLegendSwatch(g, x, rowY, width, Terrain.color('b'), "Badlands");
        rowY += 24;
        drawLegendSwatch(g, x, rowY, width, Terrain.color('P'), "Beach");
        rowY += 24;
        drawLegendSwatch(g, x, rowY, width, Terrain.color('m'), "Mountain");
        rowY += 24;
        drawLegendSwatch(g, x, rowY, width, Terrain.color('w'), "Water");
        rowY += 34;
        drawLegendSettlement(g, x, rowY, "Settlement");
        rowY += 26;
        drawLegendQuest(g, x, rowY, new Color(112, 220, 128), "Gather");
        rowY += 26;
        drawLegendQuest(g, x, rowY, new Color(114, 191, 255), "Visit");
        rowY += 26;
        drawLegendQuest(g, x, rowY, new Color(233, 89, 83), "Battle");
        rowY += 26;
        drawLegendPlayer(g, x, rowY, "You");
        if (state.worldMapKingdoms) {
            rowY += 34;
            g.setFont(new Font("SansSerif", Font.BOLD, 15));
            g.setColor(new Color(244, 239, 220));
            g.drawString("Kingdoms", x, rowY);
            rowY += 24;
            for (WorldMap.Kingdom kingdom : state.world.kingdoms()) {
                drawLegendSwatch(g, x, rowY, width, new Color(kingdom.colorRgb()), kingdom.name());
                rowY += 22;
            }
        }
    }

    private void drawLegendSwatch(Graphics2D g, int x, int y, int width, Color color, String label) {
        g.setColor(color);
        g.fillRect(x, y - 12, 16, 16);
        g.setColor(new Color(18, 23, 32));
        g.drawRect(x, y - 12, 16, 16);
        drawLegendText(g, x + 24, y + 1, width - 24, label);
    }

    private void drawLegendSettlement(Graphics2D g, int x, int y, String label) {
        g.setColor(new Color(245, 214, 117));
        g.fillRect(x + 5, y - 9, 8, 8);
        drawLegendText(g, x + 24, y, 92, label);
    }

    private void drawLegendQuest(Graphics2D g, int x, int y, Color color, String label) {
        g.setColor(new Color(0, 0, 0, 155));
        g.fillOval(x + 2, y - 16, 16, 16);
        g.setColor(color);
        g.fillOval(x + 4, y - 14, 12, 12);
        g.fillPolygon(new Polygon(new int[]{x + 7, x + 13, x + 10}, new int[]{y - 5, y - 5, y + 1}, 3));
        g.setColor(new Color(245, 246, 236));
        g.drawOval(x + 3, y - 15, 14, 14);
        drawLegendText(g, x + 24, y, 92, label);
    }

    private void drawLegendPlayer(Graphics2D g, int x, int y, String label) {
        g.setColor(new Color(255, 245, 174));
        g.fillOval(x + 4, y - 12, 11, 11);
        g.setColor(Color.BLACK);
        g.drawOval(x + 4, y - 12, 11, 11);
        drawLegendText(g, x + 24, y, 92, label);
    }

    private void drawLegendText(Graphics2D g, int x, int y, int width, String label) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(210, 213, 222));
        FontMetrics metrics = g.getFontMetrics();
        String display = label;
        while (metrics.stringWidth(display) > width && display.length() > 4) {
            display = display.substring(0, display.length() - 4) + "...";
        }
        g.drawString(display, x, y);
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
        int h = 704;
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

        g.drawString("Audio", x + 470, y + 96);
        drawVolumeRow(g, x + 470, y + 132, "Master", state.config.masterVolume, "master");
        drawVolumeRow(g, x + 470, y + 184, "Music", state.config.musicVolume, "music");
        drawVolumeRow(g, x + 470, y + 236, "SFX", state.config.sfxVolume, "sfx");
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(165, 174, 193));
        wrap(g, "Music blends between biome themes over time, with softer ambient variants during longer exploration.", x + 470, y + 294, 260, 18);

        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.setColor(new Color(230, 225, 206));
        g.drawString("Keybind Map", x + 470, y + 366);
        drawKeybindRow(g, x + 470, y + 394, "Battle", "A / Space", "Attack");
        drawKeybindRow(g, x + 470, y + 426, "Battle", "1-3", "Abilities");
        drawKeybindRow(g, x + 470, y + 458, "Battle", "E / Tab", "Enemy target");
        drawKeybindRow(g, x + 470, y + 490, "Battle", "Q", "Ally target");
        drawKeybindRow(g, x + 470, y + 522, "Battle", "H / J", "Potion / Ether");
        drawKeybindRow(g, x + 470, y + 554, "Game", "Esc", "Pause or close");

        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.setColor(new Color(230, 225, 206));
        g.drawString("Village", x + 52, y + 492);
        drawToggleRow(g, x + 52, y + 524, "Creative Build", state.config.creativeBuildMode, this::toggleCreativeBuildMode);

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
        g.drawString(enabled ? "Building costs disabled" : "Building costs enabled", x + 318, y + 20);
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
        drawCenteredIn(g, state.saveMenuCanSave ? "Save / Load Adventure" : "Load Adventure", x, y + 48, w);

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
        actionButton(g, x + w - 170, y + h - 58, 120, 34, "Back", state::closeSaveMenu, new Color(48, 55, 70), new Color(89, 102, 125), true);
        if (hasPendingOverwrite()) {
            drawOverwriteConfirmation(g, x, y, w, h);
        }
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
        return Math.max(GameConfig.TILE, viewWidth() - GameConfig.SIDEBAR_WIDTH);
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
        playerPathDestination = null;
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
            clearQueuedMove();
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
        if (playerPath.isEmpty()) {
            playerPathDestination = null;
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
        if (playerMoving()) {
            if (!keepPath && Math.abs(dx) + Math.abs(dy) == 1) {
                clearPlayerPath();
                queuedMoveDx = dx;
                queuedMoveDy = dy;
            }
            return;
        }
        if (!keepPath) {
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

    private void syncPlayerAnimationToState() {
        clearPlayerPath();
        clearQueuedMove();
        playerMoveMapId = state.currentMapId;
        playerMoveFromX = state.playerX;
        playerMoveFromY = state.playerY;
        playerMoveStartFrame = frame - PLAYER_MOVE_FRAMES;
        playerWalkAnimationTileStart = 0;
        playerWalkAnimationTiles = 0;
    }

    private void saveGame() {
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

    private void openSaveMenuForSaving() {
        state.openSaveMenu(true);
        state.setPendingSaveName(nextDefaultSaveName());
        saveListCurrentCharacterOnly = true;
        saveListScroll = 0;
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

    private void confirmOverwriteSave() {
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

    private void cancelOverwrite() {
        pendingOverwriteSaveId = "";
        pendingOverwriteSaveName = "";
    }

    private boolean hasPendingOverwrite() {
        return pendingOverwriteSaveId != null && !pendingOverwriteSaveId.isBlank();
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

    private void saveSettings() {
        try {
            state.config.save(javaRoot);
        } catch (IOException ex) {
            state.status = "Settings save failed: " + ex.getMessage();
        }
    }

    private void exitGame() {
        music.shutdown();
        sounds.shutdown();
        java.awt.Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.dispose();
        }
        System.exit(0);
    }

    public void shutdown() {
        music.shutdown();
        sounds.shutdown();
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
                if (handleClassSelectTextInput(event)) {
                    repaint();
                    return;
                }
                if (code == KeyEvent.VK_1) {
                    state.chooseClass("Knight");
                    syncPlayerAnimationToState();
                } else if (code == KeyEvent.VK_2) {
                    state.chooseClass("Mage");
                    syncPlayerAnimationToState();
                } else if (code == KeyEvent.VK_3) {
                    state.chooseClass("Ranger");
                    syncPlayerAnimationToState();
                } else if (code == KeyEvent.VK_4) {
                    state.chooseClass("Cleric");
                    syncPlayerAnimationToState();
                } else if (code == KeyEvent.VK_5) {
                    state.chooseClass("Rogue");
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
            if (state.mode == GameMode.STORY_INTRO) {
                if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE || code == KeyEvent.VK_E) {
                    state.advanceStoryIntro();
                } else if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_BACK_SPACE) {
                    state.openClassSelect();
                }
                repaint();
                return;
            }
            if (state.mode == GameMode.PAUSE_MENU) {
                if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_ENTER) {
                    state.resumeGame();
                } else if (code == KeyEvent.VK_F5 || code == KeyEvent.VK_S) {
                    openSaveMenuForSaving();
                } else if ((code == KeyEvent.VK_F9 || code == KeyEvent.VK_L) && saves.exists()) {
                    openSaveMenuForSaving();
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
                if (hasPendingOverwrite()) {
                    if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_F5) {
                        confirmOverwriteSave();
                    } else if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_BACK_SPACE) {
                        cancelOverwrite();
                    }
                    repaint();
                    return;
                }
                if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_BACK_SPACE) {
                    if (state.saveMenuCanSave && code == KeyEvent.VK_BACK_SPACE && !state.pendingSaveName.isEmpty()) {
                        state.setPendingSaveName(state.pendingSaveName.substring(0, state.pendingSaveName.length() - 1));
                    } else {
                        state.closeSaveMenu();
                    }
                } else if ((code == KeyEvent.VK_ENTER || code == KeyEvent.VK_F5) && state.saveMenuCanSave) {
                    saveGame();
                } else if (state.saveMenuCanSave && code == KeyEvent.VK_DELETE) {
                    state.setPendingSaveName("");
                } else if (state.saveMenuCanSave && code == KeyEvent.VK_TAB) {
                    saveListCurrentCharacterOnly = !saveListCurrentCharacterOnly;
                    saveListScroll = 0;
                } else if (!state.saveMenuCanSave && code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
                    int index = code - KeyEvent.VK_1;
                    List<SaveSystem.SaveSummary> summaries = saves.listSaves();
                    if (index < summaries.size()) {
                        loadGame(summaries.get(index).saveId());
                    }
                } else if (code == KeyEvent.VK_UP) {
                    saveListScroll = Math.max(0, saveListScroll - 1);
                } else if (code == KeyEvent.VK_DOWN) {
                    saveListScroll = Math.max(0, saveListScroll + 1);
                } else if (state.saveMenuCanSave) {
                    char ch = event.getKeyChar();
                    if ((Character.isLetterOrDigit(ch) || ch == ' ' || ch == '-' || ch == '_' || ch == '\'')
                            && state.pendingSaveName.length() < 32) {
                        state.setPendingSaveName(state.pendingSaveName + ch);
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
                } else if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
                    runDialogOption(code - KeyEvent.VK_1);
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
                } else if (code == KeyEvent.VK_K) {
                    state.toggleWorldMapKingdoms();
                }
            } else if (state.mode == GameMode.INVENTORY) {
                if (code == KeyEvent.VK_I) {
                    state.toggleInventory();
                } else if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
                    state.useInventoryItem(inventoryItemScroll + code - KeyEvent.VK_1);
                }
            } else if (state.mode == GameMode.CRAFTING) {
                if (code == KeyEvent.VK_C || code == KeyEvent.VK_Q) {
                    state.toggleCrafting();
                } else if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
                    state.craftRecipeAt(craftingRecipeScroll + code - KeyEvent.VK_1);
                }
            } else if (state.mode == GameMode.PARTY) {
                if (code == KeyEvent.VK_O || code == KeyEvent.VK_Q) {
                    state.toggleParty();
                } else if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
                    state.selectPartyScreenActor(code - KeyEvent.VK_1);
                }
            } else if (state.mode == GameMode.VILLAGE) {
                if (code == KeyEvent.VK_V || code == KeyEvent.VK_Q) {
                    state.toggleVillage();
                } else if (code == KeyEvent.VK_1) {
                    state.setVillageTab(0);
                } else if (code == KeyEvent.VK_2) {
                    state.setVillageTab(1);
                } else if (code == KeyEvent.VK_3) {
                    state.setVillageTab(2);
                } else if (code == KeyEvent.VK_4) {
                    state.setVillageTab(3);
                } else if (code == KeyEvent.VK_5) {
                    state.setVillageTab(4);
                } else if (code == KeyEvent.VK_DELETE || code == KeyEvent.VK_BACK_SPACE) {
                    state.setVillageEditAction("delete");
                } else if (code == KeyEvent.VK_M) {
                    state.setVillageEditAction("move");
                } else if (code == KeyEvent.VK_U) {
                    state.setVillageEditAction("upgrade");
                } else if (code == KeyEvent.VK_P) {
                    state.setVillageEditAction("place");
                }
            } else if (state.mode == GameMode.BUILDING_ASSIGNMENT) {
                if (code == KeyEvent.VK_Q || code == KeyEvent.VK_ESCAPE) {
                    state.closeOverlay();
                } else if (code == KeyEvent.VK_E || code == KeyEvent.VK_ENTER) {
                    state.enterActiveVillageBuilding();
                } else if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
                    int index = code - KeyEvent.VK_1;
                    List<Actor> workers = state.stationedAllies();
                    if (index >= 0 && index < workers.size()) {
                        state.assignAllyToActiveBuilding(workers.get(index).name);
                    }
                }
            } else if (state.mode == GameMode.SETTLEMENT_BOARD) {
                if (code == KeyEvent.VK_Q || code == KeyEvent.VK_E) {
                    state.closeOverlay();
                } else if (code == KeyEvent.VK_1) {
                    state.setSettlementBoardTab(0);
                } else if (code == KeyEvent.VK_2) {
                    state.setSettlementBoardTab(1);
                } else if (code == KeyEvent.VK_R) {
                    state.rerollSettlementRecruits();
                }
            } else if (state.mode == GameMode.SHOP) {
                if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
                    state.buyShopItem(shopItemScroll + code - KeyEvent.VK_1);
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
            } else if (code == KeyEvent.VK_G) {
                state.gatherNearby();
            } else if (code == KeyEvent.VK_C) {
                state.toggleCrafting();
            } else if (code == KeyEvent.VK_O) {
                state.toggleParty();
            } else if (code == KeyEvent.VK_V) {
                state.toggleVillage();
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
            } else if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
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

        private boolean handleClassSelectTextInput(KeyEvent event) {
            int code = event.getKeyCode();
            if (code == KeyEvent.VK_BACK_SPACE) {
                if (!state.pendingPlayerName.isEmpty()) {
                    state.setPendingPlayerName(state.pendingPlayerName.substring(0, state.pendingPlayerName.length() - 1));
                }
                return true;
            }
            if (event.isControlDown() || event.isAltDown() || event.isMetaDown()) {
                return false;
            }
            char ch = event.getKeyChar();
            if (isNameCharacter(ch) && state.pendingPlayerName.length() < 24) {
                state.setPendingPlayerName(state.pendingPlayerName + ch);
                return true;
            }
            return false;
        }

        private boolean isNameCharacter(char ch) {
            return Character.isLetterOrDigit(ch) || ch == ' ' || ch == '-' || ch == '_';
        }

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
            case VOLUME -> setVolumeSetting(activeSlider.key(), (int) Math.round(fraction * 100));
            case ZOOM -> state.setZoom(70 + (int) Math.round(fraction * 80));
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
            if (SwingUtilities.isLeftMouseButton(event)) {
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
            }
            if (state.mode == GameMode.INVENTORY && SwingUtilities.isLeftMouseButton(event)) {
                startInventoryDrag(hoverPoint);
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
            if (inventoryDrag != null && hoverPoint != null) {
                inventoryDragPoint = hoverPoint;
                inventoryDragMoved = inventoryDragStart != null && inventoryDragStart.distance(hoverPoint) > 4.0;
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
            Point point = logicalPoint(event);
            if (point == null) {
                return;
            }
            for (int i = buttons.size() - 1; i >= 0; i--) {
                UiButton button = buttons.get(i);
                if (button.contains(point.x, point.y)) {
                    return;
                }
            }
            if (state.mode == GameMode.VILLAGE && point.x < gameAreaWidth() && point.y < viewHeight()) {
                clickVillageWorld(point.x, point.y);
            } else if (state.mode == GameMode.EXPLORE && point.x < gameAreaWidth() && point.y < viewHeight()) {
                clickWorld(point.x, point.y);
            }
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent event) {
            if (state.mode == GameMode.INVENTORY) {
                inventoryItemScroll = Math.max(0, inventoryItemScroll + event.getWheelRotation());
                repaint();
                return;
            }
            if (state.mode == GameMode.CRAFTING) {
                int maxScroll = Math.max(0, state.availableCraftingRecipes().size() - 6);
                craftingRecipeScroll = Math.max(0, Math.min(maxScroll, craftingRecipeScroll + event.getWheelRotation()));
                repaint();
                return;
            }
            if (state.mode == GameMode.SKILLS) {
                skillTreeScroll = Math.max(0, skillTreeScroll + event.getWheelRotation());
                repaint();
                return;
            }
            if (state.mode == GameMode.PARTY) {
                partySkillScroll = Math.max(0, partySkillScroll + event.getWheelRotation());
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
            int tileSize = tileSize();
            int targetX = (int) Math.floor(lastCameraX + x / (double) tileSize);
            int targetY = (int) Math.floor(lastCameraY + y / (double) tileSize);
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

    private void runDialogOption(int index) {
        if (state.activeNpc == null) {
            return;
        }
        Quest quest = state.activeNpc.questId() == null ? null : state.quests.get(state.activeNpc.questId());
        List<DialogOption> options = dialogOptions(state.activeNpc, quest);
        if (index >= 0 && index < options.size() && options.get(index).enabled()) {
            options.get(index).action().run();
        }
    }
}
