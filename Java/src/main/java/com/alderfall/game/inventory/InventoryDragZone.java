package com.alderfall.game.inventory;

import java.awt.Rectangle;

public record InventoryDragZone(Rectangle bounds, String itemKey, String sourceSlot) {
}
