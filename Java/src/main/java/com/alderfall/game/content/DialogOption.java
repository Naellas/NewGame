package com.alderfall.game;

import java.awt.Color;

public record DialogOption(String label, Runnable action, Color fill, Color border, boolean enabled) {
}
