package com.alderfall.game;

import java.util.Map;
import java.util.List;

public record Npc(
        String mapId,
        String name,
        String sprite,
        int x,
        int y,
        List<String> dialog,
        String questId,
        String shopId,
        String recruitId,
        int recruitCost,
        Map<String, Integer> professionXp
) {
    public Npc {
        professionXp = Map.copyOf(professionXp == null || professionXp.isEmpty()
                ? Profession.seedXp(name, sprite, "NPC")
                : professionXp);
    }

    public Npc(
            String mapId,
            String name,
            String sprite,
            int x,
            int y,
            List<String> dialog,
            String questId,
            String shopId,
            String recruitId,
            int recruitCost
    ) {
        this(mapId, name, sprite, x, y, dialog, questId, shopId, recruitId, recruitCost,
                Profession.seedXp(name, sprite, "NPC"));
    }

    public Npc(
            String mapId,
            String name,
            String sprite,
            int x,
            int y,
            List<String> dialog,
            String questId,
            String shopId
    ) {
        this(mapId, name, sprite, x, y, dialog, questId, shopId, null, 0);
    }

    public int professionLevel(String professionId) {
        return Profession.levelForXp(professionXp.getOrDefault(professionId, 0));
    }
}
