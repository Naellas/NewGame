package com.alderfall.game;

import java.awt.Color;
import java.awt.Rectangle;

record TooltipZone(Rectangle bounds, String title, String body, String icon, Color accent) {
    TooltipZone(Rectangle bounds, String title, String body) {
        this(bounds, title, body, null, null);
    }

    TooltipZone(Rectangle bounds, String title, String body, String icon) {
        this(bounds, title, body, icon, null);
    }
}
