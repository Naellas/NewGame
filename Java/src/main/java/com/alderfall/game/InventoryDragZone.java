package com.alderfall.game;

import java.awt.Rectangle;

record InventoryDragZone(Rectangle bounds, String itemKey, String sourceSlot) {
}
