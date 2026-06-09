package com.alderfall.game;

record NearbyPrompt(int x, int y, String target, Quest.ObjectiveKind kind, String action) {
}
