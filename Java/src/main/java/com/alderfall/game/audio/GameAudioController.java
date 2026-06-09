package com.alderfall.game;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class GameAudioController {
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
            GameMode.FAST_TRAVEL,
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
    private static final int BIOME_MUSIC_FULL_BLEND_TICKS = Math.max(1, 18_000 / GameConfig.FPS_MS);
    private static final int BIOME_TUNE_CROSSFADE_TICKS = Math.max(1, 12_000 / GameConfig.FPS_MS);
    private static final int BIOME_TUNE_ROTATION_TICKS = Math.max(1, 50_000 / GameConfig.FPS_MS);

    private final GameState state;
    private final MusicManager music;
    private final SoundManager sounds;
    private int lastBattleEffectTimer;
    private String lastBattleEffectSignature = "";
    private int lastGatherSoundCycle = -1;
    private String lastGatherSoundSignature = "";
    private String worldMusicKey = "";
    private String worldMusicTrack = "";
    private String previousWorldMusicTrack = "";
    private int worldMusicDwellTicks;
    private int worldMusicTrackTicks;
    private int worldMusicPaletteIndex;
    private boolean worldMusicBiomeChanging;

    GameAudioController(GameState state, MusicManager music, SoundManager sounds) {
        this.state = state;
        this.music = music;
        this.sounds = sounds;
    }

    void update(int frame) {
        updateMusic();
        updateBattleSoundEffects();
        updateGatherSoundEffects(frame);
    }

    void shutdown() {
        music.shutdown();
        sounds.shutdown();
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

    private void updateGatherSoundEffects(int frame) {
        sounds.setVolume(effectiveSfxVolume());
        CraftingSystem.GatherCandidate candidate = state.activeGatherCandidate;
        if (!state.crafting.active() || candidate == null || !state.currentMapId.equals(state.activeGatherMapId)) {
            lastGatherSoundCycle = -1;
            lastGatherSoundSignature = "";
            return;
        }
        String tool = gatherToolKind(candidate);
        TilePoint tile = candidate.tile();
        String signature = state.activeGatherMapId + ":" + tile.x() + ":" + tile.y() + ":" + tool;
        int cycle = frame / 24;
        double phase = (frame % 24) / 24.0;
        double strike = Math.sin(phase * Math.PI);
        if (strike > 0.90 && (cycle != lastGatherSoundCycle || !signature.equals(lastGatherSoundSignature))) {
            sounds.play(gatherSoundName(tool));
            lastGatherSoundCycle = cycle;
            lastGatherSoundSignature = signature;
        }
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
            case "ward_prayer_unique", "bulwark_unique", "citadel_protocol_unique",
                    "hold_fast_unique", "stone_guard_unique", "brave_stand_unique" -> true;
            default -> false;
        };
    }

    private String gatherSoundName(String tool) {
        return switch (tool) {
            case "pickaxe" -> "gather_stone";
            case "sickle" -> "gather_sickle";
            default -> "gather_wood";
        };
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

    private boolean isTreeGatherAsset(String asset) {
        String lower = asset == null ? "" : asset.toLowerCase();
        return lower.contains("tree")
                || lower.contains("pine")
                || lower.contains("log")
                || lower.contains("stump")
                || lower.contains("woodpile");
    }
}