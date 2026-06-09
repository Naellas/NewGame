package com.alderfall.game;

public record Kingdom(
        String id,
        String name,
        String capital,
        int centerX,
        int centerY,
        String description,
        int color
) {
}
