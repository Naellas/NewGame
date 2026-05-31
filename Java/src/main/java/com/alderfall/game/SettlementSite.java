package com.alderfall.game;

public record SettlementSite(
        String id,
        String label,
        String kind,
        int x,
        int y,
        String kingdomId
) {
}
