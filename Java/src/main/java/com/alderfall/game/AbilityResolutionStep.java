package com.alderfall.game;

public record AbilityResolutionStep(
        int index,
        int count,
        Actor source,
        Actor target,
        BattleActionAnimation.VisualMode visualMode,
        BattleActionAnimation.Stage stage,
        double progress
) {
}
