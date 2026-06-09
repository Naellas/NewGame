package com.alderfall.game;

import java.awt.Color;
import java.awt.Rectangle;

public record TooltipZone(Rectangle bounds, String title, String body, String icon, Color accent) {
    public TooltipZone(Rectangle bounds, String title, String body) {
        this(bounds, title, body, null, null);
    }

    public TooltipZone(Rectangle bounds, String title, String body, String icon) {
        this(bounds, title, body, icon, null);
    }
}
