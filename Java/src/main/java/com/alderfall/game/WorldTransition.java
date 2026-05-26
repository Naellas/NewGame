package com.alderfall.game;

public record WorldTransition(String targetMapId, int targetX, int targetY, String message) {
}
