package com.alderfall.game;

import java.util.Map;

public final class NpcChoreAnimationLayer {
    private static final Map<String, String> ACTION_BY_SPRITE = Map.of(
            "npc_baker", "knead",
            "npc_blacksmith", "hammer",
            "npc_merchant", "inspect"
    );

    public String actionFor(GameState state, Npc npc, boolean moving, AssetStore assets) {
        if (moving || state.mode != GameMode.EXPLORE || !activeOnCurrentMap(state)) {
            return "";
        }
        String action = ACTION_BY_SPRITE.getOrDefault(npc.sprite(), "");
        if (action.isBlank()) {
            return "";
        }
        String downSprite = NpcIdentity.appearance(npc) + "_model_down";
        if (!assets.hasAnimatedSprite(downSprite, action)) {
            return "";
        }
        int seed = Math.abs(npc.name().hashCode() * 31 + npc.sprite().hashCode() + state.currentMapId.hashCode());
        int cycle = Math.floorMod(state.worldTick / 120 + seed, 6);
        return cycle < 4 ? action : "";
    }

    private boolean activeOnCurrentMap(GameState state) {
        String kind = state.world.kind(state.currentMapId);
        return "city".equals(kind) || "village".equals(kind) || "interior".equals(kind);
    }
}
