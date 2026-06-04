package com.alderfall.game.inventory;

import java.awt.Rectangle;

public record InventoryDropZone(Rectangle bounds, InventoryDropKind kind, String slot) {
}
