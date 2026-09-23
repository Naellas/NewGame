package com.alderfall.game;

public record WorldProp(int x, int y, String asset, int size, int visualSlot) {
    public WorldProp(int x, int y, String asset, int size) {
        this(x, y, asset, size, -1);
    }
}
