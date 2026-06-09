package com.alderfall.game.ui;

import com.alderfall.game.*;

public enum SkillTab {
    SURVIVAL("Survival"),
    CLASS("Class"),
    PROFESSIONS("Professions"),
    LOADOUT("Loadout");

    private final String label;

    SkillTab(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
