package com.alderfall.game;

import java.awt.Rectangle;

public record UiButton(Rectangle bounds, String label, Runnable action) {
    public boolean contains(int x, int y) {
        return bounds.contains(x, y);
    }
}
