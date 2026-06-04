package com.alderfall.game.inventory;

public record Item(
        String key,
        String name,
        String icon,
        int cost,
        int heal,
        int mp,
        ItemRarity rarity,
        int minLevel,
        String effectDescription
) {
    public Item(String key, String name, String icon, int cost, int heal, int mp) {
        this(key, name, icon, cost, heal, mp, ItemRarity.COMMON, 1, "");
    }

    public String rarityLine() {
        return rarity.label();
    }
}
