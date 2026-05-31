package com.alderfall.game;

import java.util.List;

public record Shop(String id, String name, List<String> stock) {
    public List<String> availableStock(int level) {
        return stock.stream()
                .filter(key -> {
                    Equipment equipment = GameData.EQUIPMENT.get(key);
                    return equipment == null || equipment.isAvailableAt(level);
                })
                .toList();
    }
}
