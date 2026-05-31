package com.alderfall.game;

import java.awt.Color;

record DialogOption(String label, Runnable action, Color fill, Color border, boolean enabled) {
}
