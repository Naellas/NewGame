package com.alderfall.game;

import java.util.List;
import java.util.Map;

public record SkillNode(
        String id,
        String name,
        String description,
        int maxRank,
        int levelRequirement,
        int x,
        int y,
        List<String> requires,
        Map<String, Integer> effects,
        Ability ability,
        String tree
) {
    public SkillNode(
            String id,
            String name,
            String description,
            int maxRank,
            int x,
            int y,
            List<String> requires,
            Map<String, Integer> effects,
            Ability ability,
            String tree
    ) {
        this(id, name, description, maxRank, Math.max(1, 1 + y * 2), x, y, requires, effects, ability, tree);
    }
}
