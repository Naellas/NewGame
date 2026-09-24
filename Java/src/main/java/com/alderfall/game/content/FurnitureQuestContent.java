package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Authored furniture roles bind quest stages to existing room props, not spawned objective copies. */
public final class FurnitureQuestContent {
    private FurnitureQuestContent() {}
    public static final String ID = "missing_provisions";
    public static final String SETTLEMENT = "village_oakhaven";
    private static final String CARGO = "senn_provisions";
    public record Binding(String id, String asset, int ordinal, String action, String requiresCargo,
                          String grantsCargo, String consumesCargo) {}
    private static final Map<String, Binding> BINDINGS = Map.of(
            "provisions_counter", new Binding("senn_service_counter", "interior_storage_counter", 1, "Inspect", "", "", ""),
            "provisions_ledger", new Binding("senn_pantry", "interior_pantry_shelf", 0, "Read ledger", "", "", ""),
            "provisions_recover", new Binding("senn_pantry", "interior_pantry_shelf", 0, "Collect supplies", "", CARGO, ""),
            "provisions_return", new Binding("senn_service_counter", "interior_storage_counter", 1, "Place supplies", CARGO, "", CARGO));

    public static Binding binding(String stageId) { return BINDINGS.get(stageId); }
    public static boolean isFurniture(GameState.QuestObjective objective) {
        return ID.equals(objective.questId()) && binding(objective.stageId()) != null;
    }
    public static Quest create() {
        List<Quest.QuestStage> stages = List.of(
                stage("provisions_counter", "Inspect the inn's service counter", "Service Counter", Quest.ObjectiveKind.SEARCH,
                        "Senn: The supper stores are short, but the delivery bill says everything arrived. Would you check the counter and pantry before I accuse the carter?",
                        "Inspect the long service counter at Oakhaven's inn. Stand beside it and press E.",
                        "An empty space remains between the flour sacks. No torn wrapping or spilled grain suggests a theft."),
                stage("provisions_ledger", "Read the pantry ledger", "Pantry Ledger", Quest.ObjectiveKind.SEARCH,
                        "Read the ledger tucked into the pantry shelf beside the service area.",
                        "The pantry shelf holds the receiving ledger. Read its latest entry.",
                        "The delivery was received in full. A second entry says: 'Guest-room reserve, lower shelf.' It bears Senn's initials."),
                stage("provisions_explain", "Ask Senn about the reserve", "Reserve Entry", Quest.ObjectiveKind.TALK,
                        "Ask Innkeeper Senn about the signed reserve entry.",
                        "Speak to Senn and explicitly ask about the reserve entry.",
                        "Senn: Those are my initials. I put the supplies aside for a late coach, then counted only the counter stock. The coach never arrived. Please bring the parcel from the lower pantry shelf back to the counter."),
                stage("provisions_recover", "Recover the reserve parcel", "Reserve Parcel", Quest.ObjectiveKind.SEARCH,
                        "Collect the reserve parcel from the pantry shelf.",
                        "Search the lower pantry shelf; Senn has released the guest-room reserve.",
                        "You collect the sealed reserve parcel. The wrapping and contents are intact."),
                stage("provisions_return", "Return the supplies to the counter", "Service Counter", Quest.ObjectiveKind.VISIT,
                        "Place the reserve parcel on the service counter, then report to Senn.",
                        "Carry the reserve parcel to the long service counter and press E to place it.",
                        "The reserve parcel is back on the counter. Tell Senn the supper supplies are ready."));
        var first = stages.getFirst();
        Quest quest = new Quest(ID, "Missing Provisions", "Help Innkeeper Senn at Oakhaven reconcile the inn's missing supper supplies.",
                first.target(), 1, 35, 30, first.objectiveKind(), SETTLEMENT, "", 0, first.objectiveAsset(), "",
                first.startDialog(), first.progressDialog(), first.readyDialog(),
                "Senn: All accounted for. I will keep reserve stock on its own page from now on. Thank you for checking before blaming anyone.",
                Quest.QuestType.SIDE, "", "", "", "", stages);
        quest.contentRevision = 1;
        return quest;
    }
    private static Quest.QuestStage stage(String id, String title, String target, Quest.ObjectiveKind kind,
                                           String start, String progress, String ready) {
        Binding binding = binding(id);
        return new Quest.QuestStage(id, title, target, 1, kind, SETTLEMENT, "", 0,
                binding == null ? "" : binding.asset(), "", kind == Quest.ObjectiveKind.TALK ? "Innkeeper Senn" : "", "", start, progress, ready,
                "Senn: All accounted for. I will keep reserve stock on its own page from now on. Thank you for checking before blaming anyone.");
    }

    public static Npc keeper(WorldMap world) {
        for (CityBuilding building : world.cityBuildings(SETTLEMENT)) {
            if (!(building.key().contains("inn") || building.style().equals("inn") || building.style().equals("restaurant"))) continue;
            String map = "house_" + SETTLEMENT + "_" + building.x1() + "_" + building.y1();
            if (!world.hasMap(map)) {
                TilePoint door = world.cityBuildingDoorTiles(building).getFirst();
                TilePoint outside = world.safePassablePoint(SETTLEMENT, door.x(), door.y() + 1);
                world.ensureHouseInterior(SETTLEMENT, building.x1(), building.y1(), outside.x(), outside.y());
            }
            for (Npc npc : world.npcs(map)) if (npc.name().equals("Innkeeper Senn")) return npc;
        }
        return null;
    }
    public static boolean isKeeper(WorldMap world, Npc npc) {
        if (npc == null || !npc.name().equals("Innkeeper Senn") || !npc.mapId().startsWith("house_" + SETTLEMENT + "_")) return false;
        Npc keeper = keeper(world);
        return keeper != null && keeper.mapId().equals(npc.mapId());
    }
    public static WorldProp furniture(WorldMap world, String map, Binding binding) {
        return world.props(map).stream().filter(p -> p.asset().equals(binding.asset()))
                .filter(p -> hasApproach(world, map, p))
                .sorted(Comparator.comparingInt(WorldProp::y).thenComparingInt(WorldProp::x))
                .skip(binding.ordinal()).findFirst().orElse(null);
    }
    private static boolean hasApproach(WorldMap world, String map, WorldProp prop) {
        int[] size = WorldMap.interiorVisualFootprint(prop.asset());
        for (int x = prop.x(); x < prop.x() + size[0]; x++)
            if (world.isPassable(map, x, prop.y() - 1) || world.isPassable(map, x, prop.y() + size[1])) return true;
        for (int y = prop.y(); y < prop.y() + size[1]; y++)
            if (world.isPassable(map, prop.x() - 1, y) || world.isPassable(map, prop.x() + size[0], y)) return true;
        return false;
    }

    public static boolean canUse(Quest quest, Binding binding) {
        return binding.requiresCargo().isEmpty() || quest.cargo.getOrDefault(binding.requiresCargo(), 0) > 0;
    }
    public static void apply(Quest quest, Binding binding) {
        if (!binding.grantsCargo().isEmpty()) quest.cargo.merge(binding.grantsCargo(), 1, Integer::sum);
        if (!binding.consumesCargo().isEmpty()) {
            String key = binding.consumesCargo();
            int count = quest.cargo.getOrDefault(key, 0);
            if (count <= 1) quest.cargo.remove(key); else quest.cargo.put(key, count - 1);
        }
    }
    public static String decoration(GameState state, WorldProp prop) {
        if (!prop.asset().equals("interior_storage_counter") && !prop.asset().equals("interior_pantry_shelf")) return "";
        Quest quest = state.quests.get(ID);
        if (quest == null || !quest.accepted || !state.currentMapId.startsWith("house_" + SETTLEMENT + "_")) return "";
        Npc keeper = keeper(state.world);
        if (keeper == null || !keeper.mapId().equals(state.currentMapId)) return "";
        if (!quest.observedStages.contains("provisions_ledger")
                && prop.equals(furniture(state.world, state.currentMapId, binding("provisions_ledger")))) return "interior_tabletop_scrolls";
        if (quest.observedStages.contains("provisions_explain") && !quest.observedStages.contains("provisions_recover")
                && prop.equals(furniture(state.world, state.currentMapId, binding("provisions_recover")))) return "interior_tied_bales";
        if (quest.observedStages.contains("provisions_return")
                && prop.equals(furniture(state.world, state.currentMapId, binding("provisions_return")))) return "interior_tied_bales";
        return "";
    }
}
