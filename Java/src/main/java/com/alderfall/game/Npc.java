package com.alderfall.game;

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
        int recruitCost
) {
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
}
