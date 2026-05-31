package com.alderfall.game;

import java.awt.Rectangle;

record InventoryDropZone(Rectangle bounds, InventoryDropKind kind, String slot) {
}
