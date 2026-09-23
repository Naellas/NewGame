package com.alderfall.game;

import com.alderfall.game.map.WorldMap;

/** Weekly work is selected from the giver's trade and placed near their home settlement. */
public final class RegionalNpcQuests {
    private RegionalNpcQuests() { }
    private static final java.util.Set<String> TITLES = java.util.Set.of("Feed Before Frost", "The Shared Grain Order",
            "Coal for the Tool Forge", "An Honest Ore Assay", "Crates Owed to the Market", "Bandits on the Delivery Trail",
            "Stock for the Workshop", "Tools Taken from the Bench", "Salve for the Ward", "Herbs for the Fever Beds",
            "A Record Out of Place", "Marks Worth Preserving", "A Patrol Route Reopened", "The Watch's Missing Supplies",
            "The Counterfeit Charter", "Evidence Before Judgment", "Grain for the Shared Oven", "Supplies for the Guest Table");

    public static NpcQuestStories.Story story(Quest q) {
        if (!q.id.startsWith("weekly_") || !TITLES.contains(q.title)) return null;
        String background = switch (q.title) {
            case "Feed Before Frost", "The Shared Grain Order", "Grain for the Shared Oven", "Supplies for the Guest Table" -> "The marked bundles are from my own order. Leave the standing crop and other households' stores alone. I know exactly what I promised and who is waiting for it.";
            case "Coal for the Tool Forge", "An Honest Ore Assay" -> "These are test samples, not permission to reopen a sealed shaft. I check the material myself before sending miners underground or asking a smith to pay for it.";
            case "Crates Owed to the Market", "Bandits on the Delivery Trail" -> "Drivers reported the loss, and I checked the delivery records. The marked cargo or attackers are tied to that route; this is about getting paid-for supplies to their owners.";
            case "Stock for the Workshop", "Tools Taken from the Bench" -> "The workshop has a repair order waiting. The marked bundles belong to that delivery; recover those supplies and I can put my apprentices back to work.";
            case "Salve for the Ward", "Herbs for the Fever Beds" -> "I marked mature herbs we can harvest without stripping the grove. Bring those bundles back for preparation; an unfamiliar leaf should never go straight into a patient's cup.";
            case "A Record Out of Place", "Marks Worth Preserving" -> "Copy what the two records say, including any gaps. A damaged line is a reason to leave a question open, not fill it with a convenient answer.";
            case "A Patrol Route Reopened", "The Watch's Missing Supplies" -> "The watch identified armed attackers at the marked positions. Deal with that threat and report back; travelers and local workers are not our targets.";
            default -> "Compare the marked evidence before naming anyone. A forged seal proves that a document is false; it does not by itself tell us who made it.";
        };
        String accept = q.objectiveKind == Quest.ObjectiveKind.DEFEAT ? "I'll deal with the marked attackers and report back."
                : q.objectiveKind == Quest.ObjectiveKind.SEARCH ? "I'll examine both marked records and bring you the findings."
                : "I'll collect the three marked bundles and bring them back.";
        return new NpcQuestStories.Story(q.startDialog, "What exactly have you established?", background,
                "People here are relying on my work. I can pay for your time, but I cannot replace careful work with a reassuring promise.",
                accept, q.progressDialog, q.readyDialog, q.completeDialog);
    }
    public static Quest create(Npc npc, String id, int block, WorldMap world) {
        int seed = npc.name().hashCode() * 31 + npc.mapId().hashCode() + block * 7919;
        boolean alternate = Math.floorMod(seed, 2) == 1;
        String title, target, site, asset, reason, monster = null;
        Quest.ObjectiveKind kind = Quest.ObjectiveKind.GATHER;
        int count = 3;
        switch (NpcIdentity.role(npc)) {
            case COOK -> {
                title = alternate ? "Grain for the Shared Oven" : "Supplies for the Guest Table";
                target = "Wheat Sheaf"; site = "farmland"; asset = "location_farmland_wheat";
                reason = "The kitchen has promised bread to the next travelers. Bring the three marked sheaves from our grain order while I keep the oven and guest table ready.";
            }
            case FARMER -> {
                title = alternate ? "Feed Before Frost" : "The Shared Grain Order";
                target = alternate ? "Dry Hay Bale" : "Wheat Sheaf";
                site = "farmland"; asset = alternate ? "location_farmland_hay_bales" : "location_farmland_wheat";
                reason = alternate ? "My livestock need dry feed. I set aside three marked bales, but the cart axle broke before I could bring them in."
                        : "Three marked sheaves from my crop are promised to the shared oven. Help me bring them in without touching the neighboring grower's bundles.";
            }
            case MINER -> {
                title = alternate ? "Coal for the Tool Forge" : "An Honest Ore Assay";
                target = alternate ? "Coal Sample" : "Iron Ore Sample";
                site = "cave_mouth"; asset = alternate ? "deco_ore_coal_deposit" : "deco_ore_iron_vein";
                reason = "The tool forge needs reliable stock. Bring me three marked " + (alternate ? "coal" : "iron")
                        + " samples from the mine approach so I can check them before the crew commits to the order.";
            }
            case TRADER -> {
                title = alternate ? "Crates Owed to the Market" : "Bandits on the Delivery Trail";
                site = "bandit_camp";
                if (alternate) {
                    target = "Stolen Trade Crate"; asset = "location_camp_crates";
                    reason = "Raiders took three crates marked with my trade cord. Recover those marked crates so I can fill the orders my customers already paid for.";
                } else {
                    target = "Bandit Cutthroat"; monster = "bandit_cutthroat"; asset = monster; count = 2;
                    kind = Quest.ObjectiveKind.DEFEAT;
                    reason = "Two bandits are stopping my supply carts on the trail. Deal with the marked attackers so the drivers can deliver our market stock.";
                }
            }
            case ARTISAN -> {
                title = alternate ? "Stock for the Workshop" : "Tools Taken from the Bench";
                target = alternate ? "Workshop Timber Bundle" : "Stolen Tool Crate";
                site = alternate ? "old_road_marker" : "bandit_camp"; asset = "location_camp_crates";
                reason = alternate ? "The bridge crew needs repairs, and my timber delivery is waiting beside the marked road. Bring in three bundles so I can finish the work."
                        : "Raiders stole the tool crates from our workshop delivery. Recover the three marked crates; my apprentices cannot work with empty hands.";
            }
            case HEALER -> {
                title = alternate ? "Salve for the Ward" : "Herbs for the Fever Beds";
                target = "Salve Herb"; site = "hidden_grove"; asset = "quest_salve_herbs";
                reason = "I am staying with the patients. Gather three marked herb bundles for the next batch of salve, and leave the young growth for another season.";
            }
            case SCHOLAR -> {
                title = alternate ? "A Record Out of Place" : "Marks Worth Preserving";
                target = alternate ? "Misfiled Record" : "Weathered Inscription";
                site = alternate ? "ruined_watchpost" : "graveyard";
                asset = alternate ? "quest_document_bundle" : "location_graveyard_tombstones";
                kind = Quest.ObjectiveKind.SEARCH; count = 2;
                reason = "Two marked records may explain a gap in our town ledger. Examine both and report what they actually say; guesses will not repair the archive.";
            }
            case GUARD -> {
                title = alternate ? "A Patrol Route Reopened" : "The Watch's Missing Supplies";
                target = alternate ? "Bandit Cutthroat" : "Goblin Scout";
                monster = alternate ? "bandit_cutthroat" : "goblin_scout";
                site = alternate ? "bandit_camp" : "goblin_camp"; asset = monster;
                kind = Quest.ObjectiveKind.DEFEAT; count = 2;
                reason = "The patrol identified two armed attackers along the marked route. Remove that threat so the relief watch and its supplies can get through.";
            }
            case NOBLE -> {
                title = alternate ? "The Counterfeit Charter" : "Evidence Before Judgment";
                target = alternate ? "Disputed Charter" : "Witness Record";
                site = alternate ? "abandoned_castle" : "ruined_watchpost"; asset = "quest_document_bundle";
                kind = Quest.ObjectiveKind.SEARCH; count = 2;
                reason = "Someone is using our charter to claim payments that never reach the town. Examine the two marked records before I name a suspect. Rank is no substitute for evidence.";
            }
            default -> throw new IllegalStateException("Unhandled NPC occupation");
        }
        int index = nearestSite(world, npc.mapId(), site);
        String instruction = (kind == Quest.ObjectiveKind.DEFEAT ? "Defeat " : kind == Quest.ObjectiveKind.SEARCH ? "Examine " : "Collect ")
                + count + " marked " + target.toLowerCase(java.util.Locale.ROOT) + " objectives, then return to " + npc.name() + ".";
        return new Quest(id, title, reason + " " + instruction, target, count,
                28 + Math.floorMod(seed / 7, 55), 22 + Math.floorMod(seed / 11, 48), kind,
                WorldMap.OVERWORLD_ID, site, index, asset, monster,
                reason + " I have marked the nearest known site on your map.", instruction,
                "The work is done. Return to me so we can check the result and settle your payment.",
                "Thank you. That is the work I asked for, and here is the agreed payment.");
    }

    static int nearestSite(WorldMap world, String mapId, String kind) {
        WorldMap.SettlementSite home = world.settlementSites().stream().filter(s -> s.id().equals(mapId)).findFirst().orElse(null);
        if (home == null) return 0;
        int best = 0; long distance = Long.MAX_VALUE;
        for (int i = 0; i < world.locationSites(kind).size(); i++) {
            TilePoint point = world.objectivePoint(kind, i, 0);
            long dx = point.x() - home.x(), dy = point.y() - home.y(), candidate = dx * dx + dy * dy;
            if (candidate < distance) { distance = candidate; best = i; }
        }
        return best;
    }

    public static TilePoint accessiblePoint(WorldMap world, Quest quest, TilePoint origin, java.util.Set<TilePoint> used) {
        if (!TITLES.contains(quest.title) && !quest.id.equals("cairnvale_ore_assay")) return origin;
        for (int radius = 0; radius <= 24; radius++) {
            for (int dy = -radius; dy <= radius; dy++) for (int dx = -radius; dx <= radius; dx++) {
                if (Math.abs(dx) + Math.abs(dy) != radius) continue;
                TilePoint p = new TilePoint(origin.x() + dx, origin.y() + dy);
                if (!used.contains(p) && world.isPassable(quest.activeObjectiveMapId(), p.x(), p.y())) return p;
            }
        }
        return origin;
    }
}
