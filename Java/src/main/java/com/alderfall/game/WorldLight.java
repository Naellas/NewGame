package com.alderfall.game;

import java.awt.Color;

final class WorldLight {
    final double x;
    final double y;
    final int radius;
    final Color color;
    final float alpha;
    final boolean affectsShadows;

    WorldLight(double x, double y, int radius, Color color, float alpha, boolean affectsShadows) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.color = color;
        this.alpha = alpha;
        this.affectsShadows = affectsShadows;
    }

    double shadowRadius() {
        return radius * 0.78;
    }

    double shadowStrength() {
        return affectsShadows ? alpha * 1.45 : alpha * 0.35;
    }
}
