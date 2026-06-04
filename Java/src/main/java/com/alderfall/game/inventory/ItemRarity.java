package com.alderfall.game.inventory;

public enum ItemRarity {
    COMMON("Common"),
    UNCOMMON("Uncommon"),
    RARE("Rare"),
    UNIQUE("Unique"),
    LEGENDARY("Legendary");

    private final String label;

    ItemRarity(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
