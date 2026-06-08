package com.alderfall.game.map;

public record WorldTransition(String targetMapId, int targetX, int targetY, String message) {
}
