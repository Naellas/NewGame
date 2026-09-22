package com.alderfall.game;

import java.util.List;

/** Working institutions on stable, ordinary dwelling lots; authored landmarks take precedence. */
public final class RegionalBuildingTypes {
    public enum Type {
        GRANARY("regional_hearth_granary", "Common Granary", "granary", "Grain Keeper",
                "Raised timber stores and a grain hoist keep the village's seed above damp ground. A carved leaf shelters the door.",
                "shared grain reserves, seed keeping and the lending of planting stock",
                "grain bays, seed counters and a keeper's tally desk",
                "We keep planting seed apart from the grain for meals. Emptying both stores would leave us hungry next year too.",
                "The leaf over the door names our responsibility. Every household that borrows seed helps refill these bins."),
        FERRY_LODGE("regional_river_ferry_lodge", "Ferry Lodge", "ferry_lodge", "Ferry Keeper",
                "A long slate awning shelters the waiting bench. Oars, spare rope and a small brass bell mark a river worker's lodge.",
                "crossing records, waiting passengers and repairs to ferry equipment",
                "a dispatch desk, waiting benches and a rope-and-provisions store",
                "We check the water and the ropes before taking passengers. A lord's seal cannot make a damaged ferry safe.",
                "The charter records our duties as well as the toll. Ask to see it before anyone charges you twice."),
        REMEMBRANCE_HALL("regional_north_remembrance_hall", "Hall of Names", "remembrance_hall", "Name Keeper",
                "Tiered timber roofs rise above a carved porch. Small name stones line the foundation where the living can reach them.",
                "keeping names, hearing corrections and remembering those lost in winter",
                "family records, a reading desk and benches for a public hearing",
                "A name can be added without a battle to its credit. We remember the people who kept others alive as well.",
                "If the record is wrong, bring a witness. We read corrections aloud so the mistake does not survive in every telling."),
        SMOKEHOUSE("regional_north_smokehouse", "Winter Smokehouse", "smokehouse", "Store Tender",
                "Two stout vents break a low turf roof. Split logs wait under the lean-to, kept dry for a long winter's work.",
                "drying herbs, preserving provisions and maintaining the winter food reserve",
                "separate warming and drying bays with packed stores beyond them",
                "Slow warmth and moving air do most of the work. A hot fire can ruin what a week's gathering brought in.",
                "We set aside provisions for people caught beyond the pass. Winter hospitality starts before the stranger knocks."),
        CISTERN_HOUSE("regional_sun_cistern_house", "Public Cistern House", "cistern_house", "Cistern Keeper",
                "A low glazed dome shades the water rooms. Deep grilles and recessed basins keep sun and wind away from the stored water.",
                "public water storage, channel maintenance and the traveller's first cup",
                "covered water troughs, jars, a public bench and the allocation ledger",
                "Your first cup comes before any bargain. We account for guest water in the same book as the gardens.",
                "The ward may hold and a channel may still be blocked. We inspect the stonework before blaming a spirit."),
        CARAVANSERAI("regional_sun_caravanserai", "Roadside Caravanserai", "caravanserai", "Guest Host",
                "A broad shaded arch opens beneath a flat roof and an upper gallery. Travel chests wait beside the welcome porch.",
                "shelter for travellers, shared meals and a place to hear the terms of a journey",
                "guest rooms, a shaded common room, a kitchen and space for travel stores",
                "A place at the table does not bind you to tomorrow's caravan. We speak the price and the route before taking your agreement.",
                "At noon we leave the road to its heat. You can compare travellers' accounts here while the animals rest."),
        BELLHOUSE("regional_fen_bellhouse", "Flood Bellhouse", "bellhouse", "Flood Listener",
                "An open timber belfry rises above reed thatch. Short stilts and a marked post keep the warning house tied to the changing water.",
                "flood watches, warning signals and the preservation of water-level records",
                "a warning bell, repair bench and shelves of flood observations",
                "The landing signal means move your loads uphill. The second signal calls the crews who check the walks.",
                "A bell heard below water goes into the record too. We compare it with the flood marks before calling it a Deep Listener's answer."),
        REEDWORKS("regional_fen_reedworks", "Reedworkers' House", "reedworks", "Reed Weaver",
                "A broad reed roof covers an open work bay. Rolled mats and drying bundles rest above the wet ground on short timber legs.",
                "weaving mats, preparing thatch and repairing the village's wet-weather shelters",
                "workbenches, drying reeds, a cutting bay and finished bundles",
                "Cut reeds need air before they become a roof. We leave the channel beside the drying beds open for that reason.",
                "The first sound of a leaking roof usually reaches us before the bellkeeper. Bring the damaged piece; it tells us where to look."),
        RESCUE_LODGE("regional_freehold_rescue_lodge", "Freehold Rescue Lodge", "rescue_lodge", "Rescue Warden",
                "Heavy stone footings carry a timber lookout. Ropes, a rescue sled and spare blankets sit under the deep porch.",
                "maintaining rescue supplies, sheltering stranded travellers and assembling crews",
                "a dispatch desk, equipment stores, spare beds and a common table",
                "The council counts these supplies, but a rescue does not wait for its next meeting. Tell us where help is needed.",
                "We record the whole crew when they return. Knowing who repaired a rope matters as much as knowing who gave the order.");

        public final String asset, label, theme, keeper, description, purpose, interior;
        public final List<String> dialogue;

        Type(String asset, String label, String theme, String keeper, String description,
             String purpose, String interior, String first, String second) {
            this.asset = asset; this.label = label; this.theme = theme; this.keeper = keeper;
            this.description = description; this.purpose = purpose; this.interior = interior;
            this.dialogue = List.of(first, second);
        }
    }

    private RegionalBuildingTypes() { }

    public static Type type(String mapId, CityBuilding building) {
        if (building == null || !List.of("house", "row").contains(building.style())
                || !WesternReachFolklore.buildingName(mapId, building).isEmpty()
                || !HearthlandsFolklore.buildingName(mapId, building).isEmpty()) return null;
        String key = building.key();
        boolean first = key.equals("inner_west_row") || key.equals("east_cottage");
        boolean second = key.equals("inner_east_row") || key.equals("south_cottage");
        if (!first && !second) return null;
        return switch (RegionalSettlementIdentity.region(mapId)) {
            case HEARTH -> first ? Type.GRANARY : null;
            case RIVER -> first ? Type.FERRY_LODGE : null;
            case NORTH -> first ? Type.REMEMBRANCE_HALL : Type.SMOKEHOUSE;
            case SUN -> first ? Type.CISTERN_HOUSE : Type.CARAVANSERAI;
            case FEN -> first ? Type.BELLHOUSE : Type.REEDWORKS;
            case FREEHOLDS -> first ? Type.RESCUE_LODGE : null;
            default -> null;
        };
    }

    public static String name(String mapId, CityBuilding building) {
        Type type = type(mapId, building);
        if (type == null) return "";
        String place = mapId.substring(mapId.indexOf('_') + 1);
        return Character.toUpperCase(place.charAt(0)) + place.substring(1) + " " + type.label;
    }
}
