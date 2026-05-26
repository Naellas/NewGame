package com.alderfall.game;

import java.awt.Color;

public final class FloatingText {
    public final String text;
    public int x;
    public int y;
    public int life;
    public final Color color;

    public FloatingText(String text, int x, int y, int life, Color color) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.life = life;
        this.color = color;
    }
}
