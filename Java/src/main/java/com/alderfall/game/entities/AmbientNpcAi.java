package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class AmbientNpcAi {
    private AmbientNpcAi() {
    }

    enum Activity {
        SLEEPING,
        SHELTERING,
        OPENING_SHOP,
        WORKING,
        PATROLLING,
        GATHERING,
        FISHING,
        STUDYING,
        SOCIALIZING,
        ERRAND,
        RETURNING_HOME,
        RESTING
    }

    enum Archetype {
        MERCHANT,
        COOK,
        CRAFTER,
        SCHOLAR,
        GUARD,
        SCOUT,
        HEALER,
        FORAGER,
        FISHER,
        MINER,
        TAILOR,
        LEATHERWORKER,
        NIGHT_WATCH,
        CITIZEN
    }

    private record DayPlan(
            int dawnStartMinutes,
            int workStartMinutes,
            int middayStartMinutes,
            int middayEndMinutes,
            int duskStartMinutes,
            int nightStartMinutes
    ) {
        DayPlan {
            dawnStartMinutes = Math.max(0, dawnStartMinutes);
            workStartMinutes = Math.max(dawnStartMinutes + 20, workStartMinutes);
            middayStartMinutes = Math.max(workStartMinutes + 120, middayStartMinutes);
            middayEndMinutes = Math.max(middayStartMinutes + 30, middayEndMinutes);
            duskStartMinutes = Math.max(middayEndMinutes + 90, duskStartMinutes);
            nightStartMinutes = Math.max(duskStartMinutes + 60, nightStartMinutes);
        }
    }

    private record ScoredTarget(TilePoint target, int score) {
    }

    record Routine(
            Activity activity,
            TilePoint target,
            int roamRadius,
            int maxDistanceFromHome,
            int thinkMinTicks,
            int thinkJitterTicks,
            double idleChance
    ) {
        Routine {
            roamRadius = Math.max(0, roamRadius);
            maxDistanceFromHome = Math.max(roamRadius, maxDistanceFromHome);
            thinkMinTicks = Math.max(1, thinkMinTicks);
            thinkJitterTicks = Math.max(0, thinkJitterTicks);
            idleChance = Math.max(0.0, Math.min(1.0, idleChance));
        }

        int nextThinkDelay(Random random) {
            return thinkMinTicks + (thinkJitterTicks <= 0 ? 0 : random.nextInt(thinkJitterTicks + 1));
        }
    }

    static Routine routineFor(Npc npc, WorldMap world, String mapId, int minutes, WeatherCondition weather, int dayNumber) {
        Archetype archetype = archetypeFor(npc);
        String mapKind = world.kind(mapId);
        boolean outdoorTown = "city".equals(mapKind) || "village".equals(mapKind);
        boolean interior = "interior".equals(mapKind);
        if (interior) {
            return routine(Activity.RESTING, home(npc, world), 1, 2, 105, 90, 0.72);
        }
        if (outdoorTown && severeWeather(weather) && !weatherHardy(archetype)) {
            return routine(Activity.SHELTERING, home(npc, world), 1, 2, 135, 120, 0.82);
        }
        if (outdoorTown && weather == WeatherCondition.HEAT_HAZE && minutes >= 720 && minutes < 960 && !weatherHardy(archetype)) {
            return routine(Activity.RESTING, home(npc, world), 1, 3, 125, 100, 0.78);
        }
        if (npc.job() != null && WorldMap.OVERWORLD_ID.equals(mapId) && npc.job().activeAt(minutes)) {
            return commuterJobRoutine(npc, world, weather);
        }
        if (outdoorTown) {
            for (RegionalSettlementIdentity.District district : world.area(mapId).districts) {
                if (!district.keeperName().equals(npc.name())) continue;
                if (minutes < 360 || minutes >= 1260) {
                    return routine(Activity.SLEEPING, home(npc, world), 1, 48, 140, 80, 0.85);
                }
                boolean middayShade = RegionalSettlementIdentity.region(mapId) == RegionalSettlementIdentity.Region.SUN
                        && minutes >= 720 && minutes < 960;
                if (middayShade || minutes >= 1080) {
                    return routine(middayShade ? Activity.RESTING : Activity.SOCIALIZING,
                            district.gathering(), 1, 48, 80, 60, 0.55);
                }
                return routine(minutes < 480 ? Activity.ERRAND : Activity.WORKING,
                        district.work(), 1, 48, 70, 50, 0.38);
            }
        }

        if (archetype == Archetype.NIGHT_WATCH) {
            return nightWatchRoutine(npc, world, mapId, dayNumber, minutes, weather);
        }

        DayPlan dayPlan = dayPlanFor(npc, mapId, archetype, dayNumber);
        if (minutes < dayPlan.dawnStartMinutes()) {
            return nightRoutine(npc, world, mapId, archetype, dayNumber, true);
        }
        if (minutes < dayPlan.workStartMinutes()) {
            return dawnRoutine(npc, world, mapId, archetype, dayNumber, weather);
        }
        if (minutes < dayPlan.middayStartMinutes()) {
            return workRoutine(npc, world, mapId, archetype, dayNumber, weather, true);
        }
        if (minutes < dayPlan.middayEndMinutes()) {
            return middayRoutine(npc, world, mapId, archetype, dayNumber, weather);
        }
        if (minutes < dayPlan.duskStartMinutes()) {
            return workRoutine(npc, world, mapId, archetype, dayNumber + 7, weather, false);
        }
        if (minutes < dayPlan.nightStartMinutes()) {
            return duskRoutine(npc, world, mapId, archetype, dayNumber, weather);
        }
        return nightRoutine(npc, world, mapId, archetype, dayNumber, false);
    }

    private static Routine nightWatchRoutine(Npc npc, WorldMap world, String mapId, int dayNumber, int minutes,
                                             WeatherCondition weather) {
        int hash = stableHash(npc.name(), mapId, dayNumber + 200);
        int patrolStart = 17 * 60 + signed(hash, 30);
        int latePatrol = 21 * 60 + signed(hash / 3, 35);
        int sleepStart = 5 * 60 + signed(hash / 5, 20);
        int sleepEnd = 12 * 60 + signed(hash / 7, 35);
        if (minutes >= patrolStart || minutes < sleepStart) {
            boolean deepNight = minutes < 4 * 60 || minutes >= latePatrol;
            return routine(Activity.PATROLLING, patrolTarget(npc, world, mapId, dayNumber + 19), 1, 9, 55, 75,
                    deepNight ? 0.26 : 0.18);
        }
        if (minutes < sleepEnd) {
            return routine(Activity.SLEEPING, home(npc, world), 0, 2, 150, 140, 0.90);
        }
        if (severeWeather(weather)) {
            return routine(Activity.SHELTERING, home(npc, world), 1, 2, 125, 105, 0.80);
        }
        if (minutes < 15 * 60) {
            return routine(Activity.RESTING, home(npc, world), 1, 3, 110, 90, 0.76);
        }
        return routine(Activity.ERRAND, errandTarget(npc, world, mapId, dayNumber + 23), 1, 4, 90, 80, 0.54);
    }

    private static DayPlan dayPlanFor(Npc npc, String mapId, Archetype archetype, int dayNumber) {
        int hash = stableHash(npc.name(), mapId, dayNumber + 500);
        int dawnStart = switch (archetype) {
            case GUARD, SCOUT -> 5 * 60 + signed(hash, 25);
            case FORAGER, FISHER, COOK -> 5 * 60 + 30 + signed(hash, 30);
            case SCHOLAR, HEALER -> 6 * 60 + 20 + signed(hash, 25);
            case MERCHANT, CRAFTER, MINER, TAILOR, LEATHERWORKER -> 6 * 60 + signed(hash, 35);
            default -> 6 * 60 + 10 + signed(hash, 35);
        };
        int workStart = dawnStart + 40 + Math.floorMod(hash / 3, 35);
        int middayStart = 11 * 60 + 15 + signed(hash / 5, 30);
        int middayEnd = middayStart + 50 + Math.floorMod(hash / 7, 35);
        int duskStart = 17 * 60 + signed(hash / 11, 35);
        int nightStart = 20 * 60 + 15 + signed(hash / 13, 35);
        if (lighterWorkDay(archetype, hash)) {
            middayStart -= 20;
            middayEnd += 15;
            duskStart -= 25;
        }
        if (marketDay(archetype, hash)) {
            middayEnd += 20;
            duskStart += 15;
            nightStart += 10;
        }
        return new DayPlan(dawnStart, workStart, middayStart, middayEnd, duskStart, nightStart);
    }

    private static Routine dawnRoutine(Npc npc, WorldMap world, String mapId, Archetype archetype, int dayNumber,
                                       WeatherCondition weather) {
        int hash = stableHash(npc.name(), mapId, dayNumber + 41);
        if (marketDay(archetype, hash) && marketFriendly(archetype)) {
            return routine(Activity.ERRAND, marketTarget(npc, world, mapId, dayNumber), 1, 5, 85, 70, 0.36);
        }
        return switch (archetype) {
            case COOK, FISHER, FORAGER -> workRoutine(npc, world, mapId, archetype, dayNumber, WeatherCondition.CLEAR, true);
            case MERCHANT -> routine(Activity.OPENING_SHOP, workTarget(npc, world, mapId, archetype, dayNumber), 1, 4, 80, 70, 0.42);
            case GUARD, SCOUT, NIGHT_WATCH -> routine(Activity.PATROLLING, patrolTarget(npc, world, mapId, dayNumber), 1, 8, 55, 60, 0.20);
            default -> routine(Activity.ERRAND, errandTarget(npc, world, mapId, dayNumber), 1, 4, 90, 80,
                    wetWeather(weather) ? 0.62 : 0.55);
        };
    }

    private static Routine workRoutine(Npc npc, WorldMap world, String mapId, Archetype archetype, int dayNumber, WeatherCondition weather, boolean morning) {
        int hash = stableHash(npc.name(), mapId, dayNumber + (morning ? 61 : 67));
        if (!morning && lighterWorkDay(archetype, hash) && socialArchetype(archetype)) {
            return routine(Activity.ERRAND, errandTarget(npc, world, mapId, dayNumber + 7), 1, 5, 90, 85, 0.44);
        }
        if (!morning && marketDay(archetype, hash) && marketFriendly(archetype)) {
            return routine(Activity.WORKING, marketTarget(npc, world, mapId, dayNumber + 7), 1, 6, 80, 85, 0.46);
        }
        if (wetWeather(weather) && (archetype == Archetype.TAILOR || archetype == Archetype.MERCHANT || archetype == Archetype.SCHOLAR)) {
            return routine(Activity.WORKING, workTarget(npc, world, mapId, archetype, dayNumber), 1, 3, 105, 90, 0.66);
        }
        return switch (archetype) {
            case GUARD, SCOUT, NIGHT_WATCH -> routine(Activity.PATROLLING, patrolTarget(npc, world, mapId, dayNumber), 1, 9, 50, 55, 0.18);
            case FISHER -> routine(Activity.FISHING, workTarget(npc, world, mapId, archetype, dayNumber), 1, 8, 65, 75, 0.35);
            case FORAGER -> routine(Activity.GATHERING, workTarget(npc, world, mapId, archetype, dayNumber), 2, 8, 70, 90, 0.38);
            case MERCHANT -> routine(morning ? Activity.OPENING_SHOP : Activity.WORKING, workTarget(npc, world, mapId, archetype, dayNumber), 1, 5, 85, 85, 0.56);
            case SCHOLAR, HEALER -> routine(Activity.STUDYING, workTarget(npc, world, mapId, archetype, dayNumber), 1, 4, 95, 85, 0.62);
            default -> routine(Activity.WORKING, workTarget(npc, world, mapId, archetype, dayNumber), 1, 5, 85, 90, 0.50);
        };
    }

    private static Routine middayRoutine(Npc npc, WorldMap world, String mapId, Archetype archetype, int dayNumber, WeatherCondition weather) {
        int hash = stableHash(npc.name(), mapId, dayNumber + 89);
        if (severeWeather(weather)) {
            return routine(Activity.SHELTERING, home(npc, world), 1, weatherHardy(archetype) ? 4 : 2, 130, 110, 0.78);
        }
        if (archetype == Archetype.GUARD || archetype == Archetype.SCOUT || archetype == Archetype.NIGHT_WATCH) {
            return routine(Activity.PATROLLING, patrolTarget(npc, world, mapId, dayNumber + 3), 1, 8, 60, 70, 0.24);
        }
        if (marketDay(archetype, hash) && marketFriendly(archetype)) {
            return routine(Activity.SOCIALIZING, marketTarget(npc, world, mapId, dayNumber + 3), 2, 6, 75, 85, 0.38);
        }
        if (lighterWorkDay(archetype, hash) && !weatherHardy(archetype)) {
            return routine(Activity.RETURNING_HOME, home(npc, world), 1, 4, 95, 90, 0.60);
        }
        if (archetype == Archetype.FISHER || archetype == Archetype.FORAGER) {
            return routine(Activity.RESTING, mealTarget(npc, world, mapId, dayNumber + 5), 1, 5, 95, 95, 0.58);
        }
        return routine(Activity.SOCIALIZING, socialTarget(npc, world, mapId, dayNumber), 2, 6, 80, 90, 0.48);
    }

    private static Routine duskRoutine(Npc npc, WorldMap world, String mapId, Archetype archetype, int dayNumber,
                                       WeatherCondition weather) {
        int hash = stableHash(npc.name(), mapId, dayNumber + 113);
        if (archetype == Archetype.GUARD || archetype == Archetype.SCOUT || archetype == Archetype.NIGHT_WATCH) {
            return routine(Activity.PATROLLING, patrolTarget(npc, world, mapId, dayNumber + 5), 1, 8, 55, 70, 0.22);
        }
        if (severeWeather(weather) && !weatherHardy(archetype)) {
            return routine(Activity.RETURNING_HOME, home(npc, world), 1, 3, 105, 95, 0.68);
        }
        if (archetype == Archetype.MERCHANT || archetype == Archetype.COOK || socialEvening(archetype, hash)) {
            return routine(Activity.SOCIALIZING, socialTarget(npc, world, mapId, dayNumber + 2), 1, 5, 90, 90, 0.55);
        }
        if (lighterWorkDay(archetype, hash)) {
            return routine(Activity.ERRAND, errandTarget(npc, world, mapId, dayNumber + 13), 1, 4, 92, 90, 0.50);
        }
        return routine(Activity.RETURNING_HOME, home(npc, world), 1, 4, 95, 95, 0.62);
    }

    private static Routine nightRoutine(Npc npc, WorldMap world, String mapId, Archetype archetype, int dayNumber, boolean deepNight) {
        if (archetype == Archetype.GUARD || archetype == Archetype.NIGHT_WATCH || (archetype == Archetype.SCOUT && !deepNight)) {
            return routine(Activity.PATROLLING, patrolTarget(npc, world, mapId, dayNumber + 11), 1, 8, 65, 80, deepNight ? 0.32 : 0.24);
        }
        return routine(Activity.SLEEPING, home(npc, world), 0, 2, 150, 140, 0.88);
    }

    private static Routine routine(Activity activity, TilePoint target, int roamRadius, int maxDistance, int thinkMin, int thinkJitter, double idleChance) {
        return new Routine(activity, target, roamRadius, maxDistance, thinkMin, thinkJitter, idleChance);
    }

    private static Routine commuterJobRoutine(Npc npc, WorldMap world, WeatherCondition weather) {
        TilePoint workSite = home(npc, world);
        return switch (npc.job().kind()) {
            case FARMER -> wetWeather(weather)
                    ? routine(Activity.RESTING, workSite, 1, 3, 110, 90, 0.60)
                    : routine(Activity.WORKING, workSite, 2, 6, 85, 85, 0.40);
            case WOODCUTTER -> wetWeather(weather)
                    ? routine(Activity.RESTING, workSite, 1, 3, 105, 90, 0.58)
                    : routine(Activity.GATHERING, workSite, 2, 7, 70, 80, 0.30);
            case HERBALIST -> severeWeather(weather)
                    ? routine(Activity.RESTING, workSite, 1, 3, 115, 95, 0.62)
                    : routine(Activity.GATHERING, workSite, 2, 6, 78, 82, 0.34);
        };
    }

    private static Archetype archetypeFor(Npc npc) {
        if (npc.job() != null) {
            return switch (npc.job().kind()) {
                case FARMER -> Archetype.FORAGER;
                case WOODCUTTER -> Archetype.CRAFTER;
                case HERBALIST -> Archetype.HEALER;
            };
        }
        String text = ((npc.name() == null ? "" : npc.name()) + " "
                + (npc.sprite() == null ? "" : npc.sprite()) + " "
                + (npc.shopId() == null ? "" : npc.shopId()) + " "
                + String.join(" ", npc.dialog())).toLowerCase();
        if (containsAny(text, "night", "nyx", "sable", "veil")) {
            return Archetype.NIGHT_WATCH;
        }
        if (containsAny(text, "captain", "guard", "gate", "watch", "warden", "scout", "knight", "torin", "garruk", "sunwarden")) {
            return containsAny(text, "scout", "rook") ? Archetype.SCOUT : Archetype.GUARD;
        }
        if (containsAny(text, "merchant", "peddler", "seller", "shop", "quartermaster")) {
            return Archetype.MERCHANT;
        }
        if (containsAny(text, "cook", "baker", "canteen", "hearth")) {
            return Archetype.COOK;
        }
        if (containsAny(text, "dock", "fish", "net", "river", "wellkeeper")) {
            return Archetype.FISHER;
        }
        if (containsAny(text, "farmer", "hedge", "goat", "grove", "rowan", "reedcutter", "wildspeaker")) {
            return Archetype.FORAGER;
        }
        if (containsAny(text, "smith", "carpenter", "trapmaster", "builder")) {
            return Archetype.CRAFTER;
        }
        if (containsAny(text, "stone", "miner")) {
            return Archetype.MINER;
        }
        if (containsAny(text, "archivist", "scribe", "apprentice", "clerk", "map", "aide", "ren")) {
            return Archetype.SCHOLAR;
        }
        if (containsAny(text, "cleric", "brother", "medic", "healer", "marla", "liora", "niva", "sol")) {
            return Archetype.HEALER;
        }
        if (containsAny(text, "seam", "weav", "basket")) {
            return Archetype.TAILOR;
        }
        if (containsAny(text, "tanner", "furrier", "hunter", "leather")) {
            return Archetype.LEATHERWORKER;
        }
        return archetypeFromProfession(npc.professionXp());
    }

    private static Archetype archetypeFromProfession(Map<String, Integer> professionXp) {
        String bestId = "";
        int bestLevel = 1;
        for (Profession profession : Profession.ALL) {
            int level = Profession.levelForXp(professionXp.getOrDefault(profession.id(), 0));
            if (level > bestLevel) {
                bestLevel = level;
                bestId = profession.id();
            }
        }
        return switch (bestId) {
            case "woodcutting", "survival" -> Archetype.FORAGER;
            case "fishing" -> Archetype.FISHER;
            case "mining" -> Archetype.MINER;
            case "crafting" -> Archetype.CRAFTER;
            case "weaving" -> Archetype.TAILOR;
            case "leatherworking" -> Archetype.LEATHERWORKER;
            case "cooking" -> Archetype.COOK;
            default -> Archetype.CITIZEN;
        };
    }

    private static TilePoint workTarget(Npc npc, WorldMap world, String mapId, Archetype archetype, int dayNumber) {
        int hash = stableHash(npc.name(), mapId, dayNumber);
        TilePoint assignedWork = world.npcWorkTarget(npc, mapId);
        TilePoint prop = propTarget(npc, world, mapId, assignedWork == null ? home(npc, world) : assignedWork,
                hash, workPropKeywords(archetype));
        if (prop != null && (assignedWork == null || Math.floorMod(hash, 4) != 0)) {
            return prop;
        }
        if (assignedWork != null && world.isPassable(mapId, assignedWork.x(), assignedWork.y())) {
            return assignedWork;
        }
        return switch (archetype) {
            case GUARD, SCOUT, NIGHT_WATCH -> patrolTarget(npc, world, mapId, dayNumber);
            case FISHER -> waterTarget(npc, world, mapId, hash);
            case FORAGER -> offsetTarget(npc, world, mapId, hash, signed(hash, 4), signed(hash / 5, 3), 8);
            case MERCHANT -> offsetTarget(npc, world, mapId, hash, signed(hash, 2), 1 + Math.floorMod(hash, 2), 5);
            case COOK -> offsetTarget(npc, world, mapId, hash, 1, 1, 4);
            case CRAFTER, MINER -> offsetTarget(npc, world, mapId, hash, signed(hash, 2), 2, 5);
            case SCHOLAR, HEALER -> offsetTarget(npc, world, mapId, hash, signed(hash, 2), -2, 4);
            case TAILOR, LEATHERWORKER -> offsetTarget(npc, world, mapId, hash, -1, 1 + Math.floorMod(hash, 2), 4);
            default -> offsetTarget(npc, world, mapId, hash, signed(hash, 3), signed(hash / 3, 2), 5);
        };
    }

    private static TilePoint errandTarget(Npc npc, WorldMap world, String mapId, int dayNumber) {
        int hash = stableHash(npc.name(), mapId, dayNumber + 131);
        TilePoint home = home(npc, world);
        TilePoint errandProp = propTarget(npc, world, mapId, home, hash,
                "well", "fountain", "notice", "market", "cart", "barrel", "wash_line", "bench", "street_lamp");
        if (errandProp != null) {
            return errandProp;
        }
        TilePoint errandBuilding = buildingTarget(world, mapId, home, hash,
                "shop", "warehouse", "inn", "hall", "row", "house");
        if (errandBuilding != null) {
            return errandBuilding;
        }
        return offsetTarget(npc, world, mapId, hash, signed(hash, 3), 1 + signed(hash / 5, 2), 5);
    }

    private static TilePoint mealTarget(Npc npc, WorldMap world, String mapId, int dayNumber) {
        int hash = stableHash(npc.name(), mapId, dayNumber + 149);
        TilePoint home = home(npc, world);
        TilePoint prop = propTarget(npc, world, mapId, home, hash,
                "bench", "fountain", "well", "flower", "table", "barrel", "tree", "street_lamp");
        if (prop != null && Math.floorMod(hash, 4) != 0) {
            return prop;
        }
        TilePoint building = buildingTarget(world, mapId, home, hash, "inn", "restaurant", "house", "row");
        return building != null ? building : home;
    }

    private static TilePoint marketTarget(Npc npc, WorldMap world, String mapId, int dayNumber) {
        int hash = stableHash(npc.name(), mapId, dayNumber + 173);
        TilePoint center = world.npcWorkTarget(npc, mapId);
        if (center == null) {
            center = home(npc, world);
        }
        TilePoint marketProp = propTarget(npc, world, mapId, center, hash,
                "market", "kiosk", "cart", "crate", "barrel", "notice_board", "produce", "wagon");
        if (marketProp != null) {
            return marketProp;
        }
        TilePoint marketBuilding = buildingTarget(world, mapId, center, hash,
                "shop", "warehouse", "inn", "restaurant", "hall", "guild");
        return marketBuilding != null ? marketBuilding : socialTarget(npc, world, mapId, dayNumber + 1);
    }

    private static TilePoint patrolTarget(Npc npc, WorldMap world, String mapId, int dayNumber) {
        int hash = stableHash(npc.name(), mapId, dayNumber);
        TilePoint patrolProp = propTarget(npc, world, mapId, home(npc, world), hash,
                "signpost", "notice", "banner", "watch", "gate", "portal", "training_dummy", "street_lamp");
        if (patrolProp != null) {
            return patrolProp;
        }
        TilePoint patrolBuilding = buildingTarget(world, mapId, home(npc, world), hash,
                "barracks", "watchtower", "bell_tower", "hall", "arena");
        if (patrolBuilding != null) {
            return patrolBuilding;
        }
        int step = Math.floorMod(hash, 4);
        int distance = 4 + Math.floorMod(hash / 7, 4);
        int dx = switch (step) {
            case 0 -> distance;
            case 1 -> -distance;
            default -> signed(hash / 11, 2);
        };
        int dy = switch (step) {
            case 2 -> distance;
            case 3 -> -distance;
            default -> signed(hash / 13, 2);
        };
        return offsetTarget(npc, world, mapId, hash, dx, dy, 8);
    }

    private static TilePoint socialTarget(Npc npc, WorldMap world, String mapId, int dayNumber) {
        int hash = stableHash(npc.name(), mapId, dayNumber);
        TilePoint socialProp = propTarget(npc, world, mapId, home(npc, world), hash,
                "fountain", "bench", "market", "kiosk", "signpost", "notice", "flower", "well", "street_lamp", "town_portal");
        if (socialProp != null && Math.floorMod(hash, 3) != 0) {
            return socialProp;
        }
        TilePoint socialBuilding = buildingTarget(world, mapId, home(npc, world), hash,
                "inn", "restaurant", "shop", "hall", "guild", "house", "row");
        if (socialBuilding != null) {
            return socialBuilding;
        }
        int dx = signed(hash, 3);
        int dy = signed(hash / 7, 3);
        if (Math.abs(dx) + Math.abs(dy) < 2) {
            dx += hash % 2 == 0 ? 2 : -2;
        }
        return offsetTarget(npc, world, mapId, hash, dx, dy, 6);
    }

    private static TilePoint waterTarget(Npc npc, WorldMap world, String mapId, int hash) {
        TilePoint home = home(npc, world);
        for (int radius = 1; radius <= 10; radius++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (Math.abs(dx) + Math.abs(dy) != radius) {
                        continue;
                    }
                    int x = home.x() + dx;
                    int y = home.y() + dy;
                    if (world.isPassable(mapId, x, y) && adjacentToTile(world, mapId, x, y, 'w')) {
                        return new TilePoint(x, y);
                    }
                }
            }
        }
        return offsetTarget(npc, world, mapId, hash, signed(hash, 5), signed(hash / 3, 2), 8);
    }

    private static String[] workPropKeywords(Archetype archetype) {
        return switch (archetype) {
            case MERCHANT -> new String[]{"market", "cart", "crate", "barrel", "kiosk", "wagon", "produce", "notice_board"};
            case COOK -> new String[]{"oven", "clay_oven", "cook", "bakery", "produce", "grain", "table", "barrel"};
            case CRAFTER -> new String[]{"anvil", "tool", "sawhorse", "woodpile", "log", "workbench", "forge", "crate"};
            case SCHOLAR -> new String[]{"kiosk", "notice", "signpost", "books", "archive", "crystal", "table"};
            case GUARD, SCOUT, NIGHT_WATCH ->
                    new String[]{"signpost", "banner", "watch", "gate", "portal", "training_dummy", "street_lamp"};
            case HEALER -> new String[]{"fountain", "flower", "herb", "plant", "shrine", "sun", "well"};
            case FORAGER -> new String[]{"seedling", "compost", "flower", "bush", "reeds", "mushroom", "woodpile", "herb"};
            case FISHER -> new String[]{"fish", "net", "water", "reed", "lily", "dock", "barrel"};
            case MINER -> new String[]{"ore", "anvil", "crate", "barrel", "lantern", "stone", "tool"};
            case TAILOR, LEATHERWORKER -> new String[]{"wash_line", "basket", "crate", "barrel", "rack", "cloth"};
            case CITIZEN -> new String[]{"fountain", "bench", "flower", "market", "signpost", "notice", "well"};
        };
    }

    private static TilePoint propTarget(Npc npc, WorldMap world, String mapId, TilePoint center, int hash, String... keywords) {
        TilePoint home = home(npc, world);
        List<ScoredTarget> candidates = new ArrayList<>();
        for (WorldProp prop : world.props(mapId)) {
            if (!propMatches(prop.asset(), keywords)) {
                continue;
            }
            TilePoint spot = propInteractionSpot(world, mapId, prop, hash);
            if (spot == null) {
                continue;
            }
            int centerDistance = Math.abs(spot.x() - center.x()) + Math.abs(spot.y() - center.y());
            int homeDistance = Math.abs(spot.x() - home.x()) + Math.abs(spot.y() - home.y());
            if (centerDistance > 14 && homeDistance > 16) {
                continue;
            }
            int score = 120 - centerDistance * 5 - homeDistance * 2
                    + Math.floorMod(hash + prop.asset().hashCode() + prop.x() * 17 + prop.y() * 31, 29);
            addScoredCandidate(candidates, spot, score);
        }
        return pickCandidate(candidates, hash, 3);
    }

    private static boolean propMatches(String asset, String... keywords) {
        if (asset == null) {
            return false;
        }
        String lower = asset.toLowerCase();
        for (String keyword : keywords) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static TilePoint propInteractionSpot(WorldMap world, String mapId, WorldProp prop, int hash) {
        int[][] spots = {{0, 1}, {-1, 0}, {1, 0}, {0, -1}, {-1, 1}, {1, 1}, {-1, -1}, {1, -1}};
        int start = Math.floorMod(hash + prop.x() * 3 + prop.y() * 5, spots.length);
        for (int i = 0; i < spots.length; i++) {
            int[] spot = spots[(start + i) % spots.length];
            int x = prop.x() + spot[0];
            int y = prop.y() + spot[1];
            if (world.isPassable(mapId, x, y) && world.propAt(mapId, x, y) == null) {
                return new TilePoint(x, y);
            }
        }
        return world.isPassable(mapId, prop.x(), prop.y()) ? new TilePoint(prop.x(), prop.y()) : null;
    }

    private static TilePoint buildingTarget(WorldMap world, String mapId, TilePoint center, int hash, String... styles) {
        List<ScoredTarget> candidates = new ArrayList<>();
        for (CityBuilding building : world.cityBuildings(mapId)) {
            if (!styleMatches(building.style(), styles)) {
                continue;
            }
            TilePoint spot = buildingDoorSpot(world, mapId, building, hash);
            if (spot == null) {
                continue;
            }
            int distance = Math.abs(spot.x() - center.x()) + Math.abs(spot.y() - center.y());
            if (distance > 18) {
                continue;
            }
            int score = 80 - distance * 4 + Math.floorMod(hash + building.key().hashCode(), 31);
            addScoredCandidate(candidates, spot, score);
        }
        return pickCandidate(candidates, hash, 2);
    }

    private static boolean styleMatches(String style, String... styles) {
        for (String wanted : styles) {
            if (wanted.equals(style)) {
                return true;
            }
        }
        return false;
    }

    private static TilePoint buildingDoorSpot(WorldMap world, String mapId, CityBuilding building, int hash) {
        List<TilePoint> doors = world.cityBuildingDoorTiles(building);
        int start = doors.isEmpty() ? 0 : Math.floorMod(hash, doors.size());
        for (int i = 0; i < doors.size(); i++) {
            TilePoint door = doors.get((start + i) % doors.size());
            int[][] spots = {{0, 1}, {-1, 1}, {1, 1}, {0, 2}, {-1, 2}, {1, 2}};
            for (int[] spot : spots) {
                int x = door.x() + spot[0];
                int y = door.y() + spot[1];
                if (world.isPassable(mapId, x, y)) {
                    return new TilePoint(x, y);
                }
            }
        }
        return null;
    }

    private static TilePoint offsetTarget(Npc npc, WorldMap world, String mapId, int hash, int dx, int dy, int maxDistance) {
        TilePoint home = home(npc, world);
        int distance = Math.max(1, Math.abs(dx) + Math.abs(dy));
        if (distance > maxDistance) {
            dx = Math.round(dx * (maxDistance / (float) distance));
            dy = Math.round(dy * (maxDistance / (float) distance));
        }
        return closestPassableNear(world, mapId, home.x() + dx, home.y() + dy, home, Math.max(2, maxDistance), hash);
    }

    private static TilePoint home(Npc npc, WorldMap world) {
        TilePoint assignedHome = world.npcHome(npc);
        if (world.isPassable(npc.mapId(), assignedHome.x(), assignedHome.y())) {
            return assignedHome;
        }
        TilePoint fallback = new TilePoint(npc.x(), npc.y());
        return closestPassableNear(world, npc.mapId(), assignedHome.x(), assignedHome.y(), fallback, 5, stableHash(npc.name(), npc.mapId(), 0));
    }

    private static TilePoint closestPassableNear(WorldMap world, String mapId, int targetX, int targetY, TilePoint fallback, int radius, int hash) {
        if (world.isPassable(mapId, targetX, targetY)) {
            return new TilePoint(targetX, targetY);
        }
        int start = Math.floorMod(hash, 8);
        int[][] ring = {{0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}};
        for (int r = 1; r <= radius; r++) {
            for (int i = 0; i < ring.length; i++) {
                int[] direction = ring[(start + i) % ring.length];
                for (int step = 1; step <= r; step++) {
                    int x = targetX + direction[0] * step;
                    int y = targetY + direction[1] * step;
                    if (world.isPassable(mapId, x, y)) {
                        return new TilePoint(x, y);
                    }
                }
            }
        }
        return fallback;
    }

    private static boolean adjacentToTile(WorldMap world, String mapId, int x, int y, char tile) {
        return world.tileAt(mapId, x, y - 1) == tile
                || world.tileAt(mapId, x + 1, y) == tile
                || world.tileAt(mapId, x, y + 1) == tile
                || world.tileAt(mapId, x - 1, y) == tile;
    }

    private static boolean weatherHardy(Archetype archetype) {
        return archetype == Archetype.GUARD
                || archetype == Archetype.SCOUT
                || archetype == Archetype.FISHER
                || archetype == Archetype.FORAGER
                || archetype == Archetype.NIGHT_WATCH;
    }

    private static boolean severeWeather(WeatherCondition weather) {
        return weather == WeatherCondition.STORM
                || weather == WeatherCondition.BLIZZARD
                || weather == WeatherCondition.DUST;
    }

    private static boolean wetWeather(WeatherCondition weather) {
        return List.of(WeatherCondition.RAIN, WeatherCondition.STORM, WeatherCondition.SNOW, WeatherCondition.BLIZZARD).contains(weather);
    }

    private static boolean marketDay(Archetype archetype, int hash) {
        if (!marketFriendly(archetype)) {
            return false;
        }
        int cadence = archetype == Archetype.MERCHANT ? 4 : 6;
        return Math.floorMod(hash, cadence) == 0;
    }

    private static boolean lighterWorkDay(Archetype archetype, int hash) {
        if (archetype == Archetype.GUARD || archetype == Archetype.SCOUT || archetype == Archetype.NIGHT_WATCH) {
            return false;
        }
        if (archetype == Archetype.FISHER || archetype == Archetype.FORAGER) {
            return Math.floorMod(hash, 7) == 0;
        }
        return Math.floorMod(hash, 6) == 0;
    }

    private static boolean socialEvening(Archetype archetype, int hash) {
        if (archetype == Archetype.GUARD || archetype == Archetype.SCOUT || archetype == Archetype.NIGHT_WATCH) {
            return false;
        }
        return socialArchetype(archetype) && Math.floorMod(hash, 3) == 0;
    }

    private static boolean socialArchetype(Archetype archetype) {
        return archetype == Archetype.CITIZEN
                || archetype == Archetype.SCHOLAR
                || archetype == Archetype.HEALER
                || archetype == Archetype.TAILOR
                || archetype == Archetype.LEATHERWORKER
                || archetype == Archetype.MERCHANT
                || archetype == Archetype.COOK;
    }

    private static boolean marketFriendly(Archetype archetype) {
        return archetype == Archetype.MERCHANT
                || archetype == Archetype.COOK
                || archetype == Archetype.TAILOR
                || archetype == Archetype.LEATHERWORKER
                || archetype == Archetype.CRAFTER
                || archetype == Archetype.CITIZEN;
    }

    private static void addScoredCandidate(List<ScoredTarget> candidates, TilePoint target, int score) {
        for (int i = 0; i < candidates.size(); i++) {
            ScoredTarget existing = candidates.get(i);
            if (existing.target().equals(target)) {
                if (score > existing.score()) {
                    candidates.set(i, new ScoredTarget(target, score));
                }
                return;
            }
        }
        candidates.add(new ScoredTarget(target, score));
    }

    private static TilePoint pickCandidate(List<ScoredTarget> candidates, int hash, int topChoices) {
        if (candidates.isEmpty()) {
            return null;
        }
        candidates.sort(Comparator.comparingInt(ScoredTarget::score).reversed());
        int limit = Math.min(Math.max(1, topChoices), candidates.size());
        int index = Math.floorMod(hash / 17, limit);
        return candidates.get(index).target();
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static int signed(int seed, int magnitude) {
        if (magnitude <= 0) {
            return 0;
        }
        return Math.floorMod(seed, magnitude * 2 + 1) - magnitude;
    }

    private static int stableHash(String name, String mapId, int salt) {
        int hash = 17;
        hash = hash * 31 + (name == null ? 0 : name.hashCode());
        hash = hash * 31 + (mapId == null ? 0 : mapId.hashCode());
        hash = hash * 31 + salt;
        return hash == Integer.MIN_VALUE ? 0 : Math.abs(hash);
    }
}
