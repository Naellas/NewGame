package com.alderfall.game;

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
    private static final List<String> REGULAR_BATTLE_TRACKS = List.of(
            "battle_standard", "battle_pursuit", "battle_skirmish", "battle_depths");
    private static final List<String> ELITE_BATTLE_TRACKS = List.of(
            "battle_elite_hunt", "battle_elite_iron", "battle_elite_arcane");
    private static final List<String> BOSS_BATTLE_TRACKS = List.of(
            "battle_boss", "battle_boss_requiem", "battle_boss_tempest", "battle_boss_eclipse");
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
    private String pendingWorldMusicKey = "";
    private int pendingWorldMusicTicks;
    private String entranceMapId = "";
    private TilePoint musicEntrance;
    private Battle scoredBattle;
    private String scoredBattleTrack;
    private int regularBattleIndex;
    private int eliteBattleIndex;
    private int bossBattleIndex;

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
            music.blend(cue.track(), cue.bedTrack(), cue.presence(),
                    cue.track().startsWith("battle_") ? 0.65f : 3.0f);
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
        // Brief border crossings should not restart two long regional scores.
        if (key.startsWith("region_") && worldMusicKey.startsWith("region_") && !key.equals(worldMusicKey)) {
            if (!key.equals(pendingWorldMusicKey)) {
                pendingWorldMusicKey = key;
                pendingWorldMusicTicks = 0;
            }
            if (++pendingWorldMusicTicks < Math.max(1, 2_000 / GameConfig.FPS_MS)) {
                key = worldMusicKey;
            }
        } else {
            pendingWorldMusicKey = "";
            pendingWorldMusicTicks = 0;
        }
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
            if (worldMusicBiomeChanging && worldMusicKey.startsWith("dungeon_")) {
                fadeTicks = Math.max(1, 4_000 / GameConfig.FPS_MS);
            }
            int elapsedTicks = worldMusicBiomeChanging ? worldMusicDwellTicks : worldMusicTrackTicks;
            presence = Math.max(0.0f, Math.min(1.0f, elapsedTicks / (float) fadeTicks));
            if (presence >= 1.0f) {
                previousWorldMusicTrack = "";
                worldMusicBiomeChanging = false;
            }
        }
        return new MusicCue(worldMusicTrack, previousWorldMusicTrack, presence);
    }

    String worldMusicKeyForCurrentPosition() {
        String kind = state.world.kind(state.currentMapId);
        if ("dungeon".equals(kind)) {
            var context = state.world.dungeonContext(state.currentMapId);
            if (context == null) return "dungeon_crypt";
            String family = switch (context.theme()) {
                case "cave" -> "cave";
                case "abandoned_castle" -> "castle";
                case "prison" -> "prison";
                case "bandit_camp" -> "bandit";
                default -> "crypt";
            };
            // The last two floors build tension, including ascending strongholds.
            boolean deep = context.floor() >= Math.max(2, context.floors() - 1);
            return "dungeon_" + family + (deep ? "_depths" : "");
        }
        if (!state.currentMapId.equals(entranceMapId)) {
            entranceMapId = state.currentMapId;
            musicEntrance = state.world.overworldEntranceFor(state.currentMapId);
        }
        TilePoint entrance = musicEntrance;
        String region = null;
        if ("overworld".equals(state.currentMapId)) {
            region = state.world.kingdomAt(state.playerX, state.playerY).id();
        } else if (entrance != null) {
            region = state.world.kingdomAt(entrance.x(), entrance.y()).id();
        }
        if ("city".equals(kind) || "village".equals(kind) || "interior".equals(kind)) {
            return region == null ? "town_village" : "town_" + region;
        }
        if (region != null) {
            return "region_" + region;
        }
        char tile = state.world.tileAt(state.currentMapId, state.playerX, state.playerY);
        return MUSIC_BY_TERRAIN.getOrDefault(tile, "zone_grasslands");
    }

    String battleMusicTrack() {
        Battle battle = state.battle;
        if (battle == null) return "battle_standard";
        if (battle != scoredBattle) {
            scoredBattle = battle;
            // Use actual combat flags, including story bosses and randomly rolled elites.
            // Lock the cue for the encounter so deaths, menus and phase changes cannot restart it.
            if (battle.enemies.stream().anyMatch(battle::isBossEnemy)) {
                scoredBattleTrack = BOSS_BATTLE_TRACKS.get(Math.floorMod(bossBattleIndex++, BOSS_BATTLE_TRACKS.size()));
            } else if (battle.enemies.stream().anyMatch(battle::isEliteEnemy)) {
                scoredBattleTrack = ELITE_BATTLE_TRACKS.get(Math.floorMod(eliteBattleIndex++, ELITE_BATTLE_TRACKS.size()));
            } else {
                scoredBattleTrack = REGULAR_BATTLE_TRACKS.get(Math.floorMod(regularBattleIndex++, REGULAR_BATTLE_TRACKS.size()));
            }
        }
        return scoredBattleTrack;
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

    private boolean isTreeGatherAsset(String asset) {
        String lower = asset == null ? "" : asset.toLowerCase();
        return lower.contains("tree")
                || lower.contains("pine")
                || lower.contains("log")
                || lower.contains("stump")
                || lower.contains("woodpile");
    }
}
