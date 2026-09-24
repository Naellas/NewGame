package com.alderfall.game.render.world;

import com.alderfall.game.*;

import java.awt.Color;

public final class WorldLight {
    final double x;
    final double y;
    final int radius;
    final Color color;
    final float alpha;
    final boolean affectsShadows;
    final double sourceX, sourceY;
    final java.awt.Rectangle mountingFace;

    public WorldLight(double x, double y, int radius, Color color, float alpha, boolean affectsShadows) {
        this(x, y, radius, color, alpha, affectsShadows, x, y, null);
    }

    WorldLight(double x, double y, int radius, Color color, float alpha, boolean affectsShadows,
               double sourceX, double sourceY, java.awt.Rectangle mountingFace) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.color = color;
        this.alpha = alpha;
        this.affectsShadows = affectsShadows;
        this.sourceX = sourceX;
        this.sourceY = sourceY;
        this.mountingFace = mountingFace;
    }

    double shadowRadius() {
        return radius * 0.78;
    }

    double shadowStrength() {
        return affectsShadows ? alpha * 1.45 : alpha * 0.35;
    }
}
