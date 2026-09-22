package com.alderfall.game;

import java.util.Map;

final class TravelLogNarrator {
    private static final Map<String, String[]> TERRAIN_LINES = Map.ofEntries(
            Map.entry("Meadow", new String[]{
                    "The meadow grass bends back after your boots pass.",
                    "Wind combs the meadow into green waves.",
                    "Small flowers vanish and return between the blades."
            }),
            Map.entry("Oldwood", new String[]{
                    "Oldwood branches knit the road noise into hush.",
                    "Something small moves where the roots cross.",
                    "Leaves trade whispers above the party."
            }),
            Map.entry("Road", new String[]{
                    "The road softens under old rain.",
                    "Cart ruts guide your feet through the dark.",
                    "Loose stones click under the party's pace."
            }),
            Map.entry("Unmaintained Road", new String[]{
                    "The old road breaks into weeds and stubborn stones.",
                    "Grass claims the road one crack at a time.",
                    "The road forgets its edges here."
            }),
            Map.entry("Cobblestone Road", new String[]{
                    "Cobblestones hold the day's warmth a little longer.",
                    "The road rings faintly beneath your steps.",
                    "Old paving points toward safer walls."
            }),
            Map.entry("Mountain Pass", new String[]{
                    "The pass narrows and the wind finds every gap.",
                    "Loose shale ticks down the mountain wall.",
                    "Cold air spills through the pass."
            }),
            Map.entry("Frostfield", new String[]{
                    "Frost silvering the field catches each footfall.",
                    "Cold grass rasps against your boots.",
                    "The field lies pale and listening."
            }),
            Map.entry("Sunsteppe", new String[]{
                    "Dry grass hisses under the sunsteppe wind.",
                    "Heat lifts the horizon into a shimmer.",
                    "Dust gathers at the hem of every cloak."
            }),
            Map.entry("Marsh", new String[]{
                    "Marsh water answers each step with a soft gulp.",
                    "Reeds lean together as if keeping counsel.",
                    "The air tastes of peat and slow water."
            }),
            Map.entry("Badlands", new String[]{
                    "Red dust scuffs up around your heels.",
                    "The badlands keep their silence sharp.",
                    "Stone ribs show through the dry earth."
            }),
            Map.entry("Beach", new String[]{
                    "Sand loosens around each footprint.",
                    "The shore wind carries salt and distant birds.",
                    "Foam threads itself along the beach."
            }),
            Map.entry("Bridge", new String[]{
                    "The bridge complains softly under the crossing.",
                    "Water flashes below the planks.",
                    "The crossing narrows the party into single file."
            }),
            Map.entry("Farmland", new String[]{
                    "Something moves beyond the wheat.",
                    "The fields rustle with work left unfinished.",
                    "Fence posts watch over the furrows."
            }),
            Map.entry("Water", new String[]{
                    "Water darkens in slow rings nearby.",
                    "The water carries broken sky in its surface.",
                    "A cold ripple worries the bank."
            }),
            Map.entry("Village", new String[]{
                    "Lantern smoke curls above the village roofs.",
                    "Low voices travel between the cottages.",
                    "The village keeps its doors half-lit."
            }),
            Map.entry("City Gate", new String[]{
                    "The city gate gathers road dust and rumor.",
                    "Stonework rises ahead, crowded with old promises.",
                    "Lanterns burn where the road meets the gate."
            }),
            Map.entry("Dungeon", new String[]{
                    "The entrance waits with its breath held.",
                    "Cold stone marks the way down.",
                    "The dark ahead feels older than the path."
            })
    );

    private TravelLogNarrator() {
    }

    static String narrate(GameState state, String status) {
        if (status == null || status.isBlank()) {
            return "";
        }
        String trimmed = status.trim();
        String transition = transitionLine(state, trimmed);
        if (transition != null) {
            return transition;
        }
        String[] terrainLines = TERRAIN_LINES.get(trimmed);
        if (terrainLines != null) {
            return pick(state, trimmed, terrainLines);
        }
        return trimmed;
    }

    private static String transitionLine(GameState state, String status) {
        if (status.startsWith("You enter ") && status.endsWith(".")) {
            String place = status.substring("You enter ".length(), status.length() - 1);
            String local = state == null ? "" : RegionalSettlementIdentity.arrival(state.currentMapId);
            if (!local.isEmpty()) return "You enter " + place + ". " + local;
            return "You enter " + place + "; lantern light and local voices draw near.";
        }
        if (status.startsWith("You leave ") && status.endsWith(".")) {
            String place = status.substring("You leave ".length(), status.length() - 1);
            return "You leave " + place + "; the road gathers quietly behind you.";
        }
        return null;
    }

    private static String pick(GameState state, String key, String[] lines) {
        int x = state == null ? 0 : state.playerX;
        int y = state == null ? 0 : state.playerY;
        int tick = state == null ? 0 : state.worldTick;
        String map = state == null ? "" : state.currentMapId;
        int seed = key.hashCode() ^ map.hashCode() ^ x * 928371 ^ y * 364479 ^ tick / 31;
        return lines[Math.floorMod(seed, lines.length)];
    }
}
