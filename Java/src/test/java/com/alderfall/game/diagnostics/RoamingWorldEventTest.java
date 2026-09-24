package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.nio.file.Path;
import java.util.List;

public final class RoamingWorldEventTest {
    public static void main(String[] args) {
        GameState state = new GameState(GameConfig.load(Path.of("")));
        state.chooseClass("Mage");
        while (state.mode == GameMode.STORY_INTRO) state.advanceStoryIntro();
        state.currentMapId = WorldMap.OVERWORLD_ID;
        state.playerX = 1; state.playerY = 1;
        int matched = 0;
        for (int y = 15; y < 290; y += 29) for (int x = 15; x < 290; x += 31) {
            var nearest = state.world.nearestDungeonInBiome(x, y);
            char biome = state.world.dungeonExteriorBiome(x, y);
            long minimum = Long.MAX_VALUE;
            for (var marker : state.world.adventureMarkers()) {
                var context = state.world.dungeonContext(marker.mapId());
                if (context == null || context.exterior() != biome) continue;
                long dx = marker.x() - x, dy = marker.y() - y;
                minimum = Math.min(minimum, dx * dx + dy * dy);
            }
            if (nearest == null) { require(minimum == Long.MAX_VALUE, "Missed matching dungeon"); continue; }
            long dx = nearest.x() - x, dy = nearest.y() - y;
            require(dx * dx + dy * dy == minimum, "Selected farther dungeon");
            require(state.world.dungeonContext(nearest.mapId()).exterior() == biome, "Cross-biome attackers");
            List<String> enemies = state.roadAmbushers(x, y, 123);
            require(enemies.size() >= 2 && enemies.size() <= 3 && enemies.stream().allMatch(GameData.MONSTERS::containsKey), "Invalid party");
            require(enemies.equals(state.roadAmbushers(x, y, 123)), "Unstable encounter seed");
            matched++;
        }
        require(matched > 10, "Insufficient biome coverage");
        TilePoint bush = null;
        for (int y = 2; y < 298 && bush == null; y++) for (int x = 2; x < 298; x++) {
            if (!state.world.isPassable(state.currentMapId, x - 1, y)) continue;
            if (state.roamingEvents.spawnAmbushAt(state, x, y, 77)) { bush = new TilePoint(x, y); break; }
        }
        require(bush != null, "No valid road bush");
        state.playerX = bush.x() - 1; state.playerY = bush.y();
        require(state.roamingEvents.nearestPrompt(state) == null, "Ambush exposed as dialogue prompt");
        require(!state.roamingEvents.interact(state), "Ambush activated remotely");
        require(!state.roamingEvents.triggerEnteredEvent(state), "Ambush fired beside bush");
        List<String> expectedEnemies = state.roadAmbushers(bush.x(), bush.y(), 77);
        require(state.move(1, 0) && state.mode == GameMode.DIALOG, "Step did not open ambush popup");
        require(state.roamingEventPrompt().title().equals("Roadside Ambush"), "Wrong encounter popup");
        require(state.roamingEventPrompt().choices().size() == 1, "Ambush fight choice missing");
        state.revealActiveDialogueLineInstantly();
        state.selectDialogOption(0);
        require(state.mode == GameMode.BATTLE, "Fight did not start battle");
        require(state.battle.enemies.size() == expectedEnemies.size(), "Popup changed encounter group");
        require(state.battle.enemies.size() >= 2 && state.battle.enemies.size() <= 3, "Wrong battle size");
        state.mode = GameMode.EXPLORE;
        require(!state.roamingEvents.triggerEnteredEvent(state), "Ambush fired twice");
        require(!state.roamingEvents.spawnAmbushAt(state, state.playerX, state.playerY, 1), "Spawned beneath player");
        state.roamingEvents.reset();
        WorldProp shrine = null;
        for (WorldProp prop : state.world.props(state.currentMapId)) {
            if (state.roamingEvents.spawnShrineAt(state, prop, 5)) { shrine = prop; break; }
        }
        require(shrine != null, "No existing shrine bound");
        require(state.roamingEvents.shrineVariant(state.currentMapId, shrine) == 2, "Wrong shrine palette");
        require(!state.roamingEvents.spawnShrineAt(state, new WorldProp(-50, -50, shrine.asset(), 48), 2), "Created phantom shrine");
        state.mode = GameMode.PAUSE_MENU;
        state.roamingEvents.tick(state);
        require(state.roamingEvents.shrineVariant(state.currentMapId, shrine) == 2, "Pause removed shrine event");
        state.mode = GameMode.EXPLORE;
        state.playerX = shrine.x() + 1; state.playerY = shrine.y();
        require(state.roamingEvents.interact(state), "Shrine interaction missing");
        require(state.roamingEvents.shrineVariant(state.currentMapId, shrine) == -1, "Resolved shrine still glowing");
        require(!state.roamingEvents.spawnShrineAt(state, shrine, 5), "Shrine immediately repeated");
        AssetStore assets = new AssetStore(Path.of("assets"));
        var source = assets.spriteFit(shrine.asset(), 48, 48);
        var sprites = new ShrineEventVisuals.Sprites();
        for (int variant = 0; variant < 3; variant++) {
            var tinted = sprites.tint(source, variant);
            require(tinted == sprites.tint(source, variant), "Uncached tint");
            int changed = 0;
            for (int y = 0; y < 48; y++) for (int x = 0; x < 48; x++) {
                require((source.getRGB(x, y) >>> 24) == (tinted.getRGB(x, y) >>> 24), "Tint changed transparency");
                if (source.getRGB(x, y) != tinted.getRGB(x, y)) changed++;
            }
            require(changed > 20, "Invisible shrine color variant");
        }
        state.closeOverlay();
        state.roamingEvents.reset();
        state.playerX = bush.x() - 3; state.playerY = bush.y();
        require(state.roamingEvents.spawnPurseAt(state, bush.x(), bush.y(), 77), "Purse placement failed");
        state.playerX = bush.x(); state.playerY = bush.y();
        require(!state.roamingEvents.triggerEnteredEvent(state) && state.mode == GameMode.EXPLORE, "Purse opened automatically");
        require(state.roamingEvents.interact(state) && state.roamingEventPromptActive(), "Explicit purse interaction failed");
        int gold = state.player.gold;
        state.revealActiveDialogueLineInstantly();
        state.selectDialogOption(1);
        require(state.player.gold > gold && state.roamingEventPrompt().choices().size() == 1, "Purse result still offers rewards");
        int rewarded = state.player.gold;
        state.revealActiveDialogueLineInstantly();
        state.selectDialogOption(1);
        require(state.player.gold == rewarded, "Purse reward repeated");
        state.selectDialogOption(0);
        require(state.mode == GameMode.EXPLORE && !state.roamingEventPromptActive(), "Result did not close");
        state.openRoadAmbushPrompt(expectedEnemies);
        state.closeOverlay();
        require(state.mode == GameMode.BATTLE, "Escape bypassed the triggered ambush");
        for (String asset : List.of("event_lost_purse", "event_ambush_brush", "event_supply_cache", "event_evidence_bundle")) {
            require(assets.hasSprite(asset), "Missing world-object art: " + asset);
            var art = assets.spriteFit(asset, 48, 48);
            int visible = 0, transparent = 0;
            for (int y = 0; y < 48; y++) for (int x = 0; x < 48; x++) {
                int alpha = art.getRGB(x, y) >>> 24;
                if (alpha > 128) visible++;
                if (alpha == 0) transparent++;
            }
            require(visible > 150 && transparent > 150, "Unreadable or opaque-background art: " + asset);
        }
        state.resetDungeonMonsterRuntime();
        require(state.roamingEvents.shrineVariant(state.currentMapId, shrine) == -1, "Reset leaked events");
        state.mode = GameMode.EXPLORE;
        state.playerX = 1; state.playerY = 1;
        TilePoint traveler = null;
        for (int y = 12; y < 288 && traveler == null; y++) for (int x = 12; x < 288; x++) {
            if (state.roamingEvents.spawnTravelerAt(state, x, y, 99)) { traveler = new TilePoint(x, y); break; }
        }
        require(traveler != null, "No safe traveler placement");
        state.playerX = traveler.x(); state.playerY = traveler.y();
        require(!state.roamingEvents.triggerEnteredEvent(state), "Traveler opened automatically");
        var context = new WorldRenderer.PropContext(state, traveler.x() - 2, traveler.y() - 2, 5, 5, 48, 100, 0);
        var canvas = new java.awt.image.BufferedImage(240, 240, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var graphics = canvas.createGraphics();
        var depth = new WorldDepthRenderer();
        depth.begin(48);
        state.roamingEvents.draw(graphics, context, assets, depth);
        depth.draw(graphics);
        graphics.dispose();
        int opaque = 0;
        for (int y = 96; y < 144; y++) for (int x = 96; x < 144; x++)
            if ((canvas.getRGB(x, y) >>> 24) > 200) opaque++;
        require(opaque > 250, "Traveler missing solid world art at age zero");
        for (int tick = 0; tick < 1000; tick++) state.roamingEvents.tick(state);
        var prompt = state.roamingEvents.nearestPrompt(state);
        require(prompt != null && prompt.x() == traveler.x() && prompt.y() == traveler.y(), "Traveler drifted or expired");
        require(state.roamingEvents.interact(state), "Traveler interaction missing");
        require(state.roamingEventPrompt().title().equals("Wounded Traveler"), "Wrong traveler choices");
        require(!state.roamingEvents.interact(state), "Traveler interaction repeated");
        state.closeOverlay();
        state.roamingEvents.reset();
        state.playerX = 1; state.playerY = 1;
        require(state.roamingEvents.spawnTrapAt(state, traveler.x(), traveler.y(), 99), "Trap placement failed");
        require(!state.roamingEvents.spawnFishAt(state, traveler.x(), traveler.y(), 99), "Fish spawned on land");
        state.playerX = traveler.x(); state.playerY = traveler.y();
        require(state.roamingEvents.triggerEnteredEvent(state), "Stationary trap did not trigger on entry");
        require(!state.roamingEvents.triggerEnteredEvent(state), "Trap triggered twice");
        System.out.println("Roaming events passed: nearest same-biome dungeon, seeded groups, real movement trigger, shrine binding and three palettes.");
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
