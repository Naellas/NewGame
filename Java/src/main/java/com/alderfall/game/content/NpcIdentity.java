package com.alderfall.game;

import java.util.Locale;
import java.util.Set;

/** Presentation identity is separate from stable NPC/save/quest identifiers. */
public final class NpcIdentity {
    private NpcIdentity() { }
    public enum Role {
        FARMER("Farmer", "work clothes, seed pouch"), MINER("Miner", "mining helmet, pickaxe"),
        TRADER("Trader", "travel coat, account book"), ARTISAN("Artisan", "work apron, hand tools"),
        SCHOLAR("Scholar", "ledger, ink kit"), GUARD("Guard", "watch cloak, sidearm"),
        HEALER("Herbalist", "herb satchel, bandages"), NOBLE("Magistrate", "embroidered mantle, signet"),
        COOK("Cook / host", "clean apron, kitchen tools");
        public final String label, equipment;
        Role(String label, String equipment) { this.label = label; this.equipment = equipment; }
    }

    public static String region(Npc npc) {
        String map = npc.mapId().replaceFirst("^house_", "").replaceFirst("_\\d+_\\d+$", "");
        if (map.equals("town_greyharbor")) return "marsh";
        return switch (RegionalSettlementIdentity.region(map)) {
            case NORTH -> "north";
            case SUN -> "desert";
            case FEN -> "marsh";
            case FREEHOLDS -> "highland";
            default -> "temperate";
        };
    }

    public static Role role(Npc npc) {
        if (npc.job() != null) return switch (npc.job().kind()) {
            case FARMER -> Role.FARMER;
            case WOODCUTTER -> Role.ARTISAN;
            case HERBALIST -> Role.HEALER;
        };
        // Authored exceptions use the character's occupation, never incidental words in dialogue.
        return switch (npc.name()) {
            case "Rowan", "Goatkeeper Una", "Seed Keeper", "Hearth Apprentice" -> Role.FARMER;
            case "Hedgewise Lin", "Grove Tender Talla", "Rain-Seer Imani" -> Role.HEALER;
            case "Rune Delver Saela" -> Role.SCHOLAR;
            case "Edda" -> Role.COOK;
            case "Old Noll", "Reed Captain Lio", "Net-Mender Corso", "Trapmaster Yaro", "Reedcutter Vell" -> Role.ARTISAN;
            case "Toma", "Brother Cal", "Cairnwatch Asta", "Bellkeeper Ilya" -> Role.SCHOLAR;
            case "Pass Guide Olin", "Mountaineer Pela", "Fur-Tracker Minn", "Ash Watcher Kera", "Warden Sol" -> Role.GUARD;
            default -> inferredRole(npc);
        };
    }

    private static Role inferredRole(Npc npc) {
        String name = npc.name().toLowerCase(Locale.ROOT);
        for (String line : npc.dialog()) {
            if (line.startsWith("Local Clerk:") || line.startsWith("Town Crier:")) return Role.SCHOLAR;
            if (line.startsWith("Doorward:") || line.startsWith("Street Guide:")) return Role.GUARD;
        }
        if (name.matches(".*(baker|cook|innkeeper|bartender|guest host).*")) return Role.COOK;
        if (name.matches(".*(magistrate|lady |lord |baron|noble).*")) return Role.NOBLE;
        if (name.matches(".*(miner|ore |coal runner|quarry).*")) return Role.MINER;
        if (name.matches(".*(farmer|goatkeeper|seed|grain|grove tender|orchard).*")) return Role.FARMER;
        if (name.matches(".*(herbal|healer|hedgewise|wellkeeper).*")) return Role.HEALER;
        if (name.matches(".*(scribe|scholar|clerk|cartographer|archiv|apprentice|name keeper).*")) return Role.SCHOLAR;
        if (name.matches(".*(guard|captain|marshal|scout|warden|signal).*")) return Role.GUARD;
        if (name.matches(".*(smith|carpenter|weaver|mender|cutter|basket|dockhand|baker|cook|innkeeper|bartender|seamstress|tanner|bellwright).*")) return Role.ARTISAN;
        if (npc.shopId() != null || name.matches(".*(merchant|peddler|seller|trader|cachemaster|furrier).*")) return Role.TRADER;
        return switch (npc.sprite()) {
            case "npc_merchant", "npc_quartermaster" -> Role.TRADER;
            case "npc_torin", "npc_garruk" -> Role.GUARD;
            case "npc_ren", "npc_maera" -> Role.SCHOLAR;
            case "npc_marla", "npc_elowen", "npc_liora", "npc_rowan", "npc_mira_sunwarden" -> Role.HEALER;
            case "npc_blacksmith" -> Role.ARTISAN;
            case "npc_baker", "npc_bartender", "npc_innkeeper" -> Role.COOK;
            default -> Role.FARMER;
        };
    }

    private static final Set<String> FEMALE = Set.of("Peddler Nessa", "Edda", "Hedgewise Lin", "Goatkeeper Una",
            "Lantern Seller Pella", "Cairnwatch Asta", "Rain-Seer Imani", "Mira", "Rune Delver Saela",
            "Grove Tender Talla", "Ash Watcher Kera", "Mountaineer Pela");
    public static boolean female(Npc npc) {
        if (FEMALE.contains(npc.name())) return true;
        return Set.of("npc_baker", "npc_citizen_woman", "npc_marla", "npc_elowen", "npc_liora", "npc_maera", "npc_vexa",
                "npc_mira_sunwarden").contains(npc.sprite());
    }

    public static boolean regional(Npc npc) {
        return npc.recruitId() == null && !npc.sprite().startsWith("npc_story_")
                && role(npc) != Role.COOK
                && !npc.name().equals("Finch") && !npc.mapId().equals("overworld");
    }
    public static String appearance(Npc npc) {
        return regional(npc) ? "npc_regional_" + region(npc) + "_" + role(npc).name().toLowerCase(Locale.ROOT)
                + (female(npc) ? "_f" : "_m") : npc.sprite();
    }
    public static String portrait(Npc npc) { return appearance(npc) + "_portrait"; }
}
