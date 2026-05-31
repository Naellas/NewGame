package com.alderfall.game;

record BattleActorPose(int xOffset, int yOffset, double rotation, double scaleX, double scaleY) {
    static final BattleActorPose REST = new BattleActorPose(0, 0, 0.0, 1.0, 1.0);
}
