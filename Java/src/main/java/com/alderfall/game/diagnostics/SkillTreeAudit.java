package com.alderfall.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SkillTreeAudit {
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public static void main(String[] args) {
        SkillTreeAudit audit = new SkillTreeAudit();
        audit.run();
        audit.printReport();
        if (!audit.errors.isEmpty()) {
            System.exit(1);
        }
    }

    private void run() {
        Map<String, Map<String, SkillNode>> classTrees = classTrees();
        Map<String, Map<String, SkillNode>> supportTrees = supportTrees();
        Set<String> knownIds = new LinkedHashSet<>();
        for (Map<String, SkillNode> tree : supportTrees.values()) {
            knownIds.addAll(tree.keySet());
        }
        for (Map<String, SkillNode> tree : classTrees.values()) {
            knownIds.addAll(tree.keySet());
        }

        for (var entry : supportTrees.entrySet()) {
            auditTree(entry.getKey(), entry.getValue(), knownIds, false);
        }
        for (var entry : classTrees.entrySet()) {
            auditTree(entry.getKey(), entry.getValue(), knownIds, true);
            auditClassProgression(entry.getKey(), entry.getValue());
            auditClassCapacity(entry.getKey(), entry.getValue());
        }
        auditGlobalAbilityNames(classTrees);
    }

    private void auditTree(String title, Map<String, SkillNode> tree, Set<String> knownIds, boolean classTree) {
        if (tree.isEmpty()) {
            errors.add(title + ": tree is empty.");
            return;
        }
        for (SkillNode node : tree.values()) {
            if (node.id() == null || node.id().isBlank()) {
                errors.add(title + ": found a node with a blank id.");
            }
            if (node.name() == null || node.name().isBlank()) {
                errors.add(title + "/" + node.id() + ": blank display name.");
            }
            if (node.maxRank() <= 0) {
                errors.add(title + "/" + node.id() + ": maxRank must be positive.");
            }
            if (node.levelRequirement() < 1 || node.levelRequirement() > 60) {
                errors.add(title + "/" + node.id() + ": level requirement " + node.levelRequirement() + " is outside 1-60.");
            }
            for (String required : node.requires()) {
                if (!knownIds.contains(required)) {
                    errors.add(title + "/" + node.id() + ": missing prerequisite " + required + ".");
                    continue;
                }
                SkillNode requiredNode = tree.get(required);
                if (requiredNode != null && requiredNode.levelRequirement() > node.levelRequirement()) {
                    errors.add(title + "/" + node.id() + ": requires later node " + required + ".");
                }
            }
            if (node.ability() != null) {
                Ability ability = node.ability();
                if (ability.cost() < 0 || ability.power() < 0) {
                    errors.add(title + "/" + node.id() + ": ability has negative cost or power.");
                }
                if (ability.cooldown() < 0) {
                    errors.add(title + "/" + node.id() + ": ability cooldown is negative.");
                }
            }
        }
        auditReachability(title, tree);
        if (classTree) {
            int sideNodes = countEnding(tree, "_technique") + countEnding(tree, "_doctrine");
            if (countEnding(tree, "_initiate") < 4 || sideNodes < 8 || countEnding(tree, "_apex") < 4) {
                errors.add(title + ": expected four branching mastery lanes with side nodes and apex choices.");
            }
        }
    }

    private void auditReachability(String title, Map<String, SkillNode> tree) {
        Set<String> reached = new LinkedHashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        for (SkillNode node : tree.values()) {
            boolean localPrereqs = node.requires().stream().anyMatch(tree::containsKey);
            if (node.requires().isEmpty() || !localPrereqs) {
                queue.add(node.id());
            }
        }
        while (!queue.isEmpty()) {
            String id = queue.removeFirst();
            if (!reached.add(id)) {
                continue;
            }
            for (SkillNode node : tree.values()) {
                if (!reached.contains(node.id()) && node.requires().stream().filter(tree::containsKey).allMatch(reached::contains)) {
                    queue.add(node.id());
                }
            }
        }
        for (String id : tree.keySet()) {
            if (!reached.contains(id)) {
                errors.add(title + "/" + id + ": cannot be reached from any root node.");
            }
        }
    }

    private void auditClassProgression(String title, Map<String, SkillNode> tree) {
        List<Integer> levels = tree.values().stream()
                .map(SkillNode::levelRequirement)
                .distinct()
                .sorted()
                .toList();
        int maxLevel = levels.isEmpty() ? 0 : levels.get(levels.size() - 1);
        if (maxLevel < 60) {
            errors.add(title + ": highest unlock is level " + maxLevel + ", expected a level-60 capstone.");
        }
        if (levels.stream().noneMatch(level -> level >= 58)) {
            errors.add(title + ": missing late apex unlocks at level 58+.");
        }
        int previous = 1;
        for (int level : levels) {
            if (level - previous > 12) {
                errors.add(title + ": unlock gap from level " + previous + " to " + level + " is too large.");
            }
            previous = level;
        }
    }

    private void auditClassCapacity(String title, Map<String, SkillNode> tree) {
        long skillAbilities = tree.values().stream().filter(node -> node.ability() != null).count();
        int baseAbilities = GameData.classAbilities(title).size();
        if (baseAbilities + skillAbilities > Actor.MAX_ABILITIES) {
            errors.add(title + ": can learn " + (baseAbilities + skillAbilities) + " abilities, exceeding Actor.MAX_ABILITIES " + Actor.MAX_ABILITIES + ".");
        }
    }

    private void auditGlobalAbilityNames(Map<String, Map<String, SkillNode>> classTrees) {
        Map<String, List<String>> locations = new LinkedHashMap<>();
        for (var treeEntry : classTrees.entrySet()) {
            for (SkillNode node : treeEntry.getValue().values()) {
                if (node.ability() != null) {
                    locations.computeIfAbsent(node.ability().name(), key -> new ArrayList<>())
                            .add(treeEntry.getKey() + "/" + node.id());
                }
            }
        }
        for (var entry : locations.entrySet()) {
            if (entry.getValue().size() > 1) {
                warnings.add("Ability name reused: " + entry.getKey() + " at " + String.join(", ", entry.getValue()) + ".");
            }
        }
    }

    private int countEnding(Map<String, SkillNode> tree, String suffix) {
        return (int) tree.keySet().stream().filter(id -> id.endsWith(suffix)).count();
    }

    private Map<String, Map<String, SkillNode>> classTrees() {
        Map<String, Map<String, SkillNode>> trees = new LinkedHashMap<>();
        for (String className : List.of(
                "Knight", "Mage", "Ranger", "Cleric", "Rogue",
                "Battle Medic", "Ironwall", "Bladedancer", "Veilrunner", "Wildspeaker",
                "Sunwarden", "Thornbinder", "Stonebreaker", "Nightblade", "Grovekeeper")) {
            trees.put(className, SkillTrees.skillTreeForClass(className));
        }
        return trees;
    }

    private Map<String, Map<String, SkillNode>> supportTrees() {
        Map<String, Map<String, SkillNode>> trees = new LinkedHashMap<>();
        trees.put("Common", SkillTrees.COMMON_SKILL_TREE);
        trees.put("Professions", SkillTrees.PROFESSION_SKILL_TREE);
        SkillTrees.PROFESSION_SKILL_TREES.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .forEach(entry -> trees.put("Profession/" + entry.getKey(), entry.getValue()));
        return trees;
    }

    private void printReport() {
        System.out.println("Skill Tree Audit");
        System.out.println("Errors: " + errors.size());
        for (String error : errors) {
            System.out.println("ERROR " + error);
        }
        System.out.println("Warnings: " + warnings.size());
        for (String warning : warnings) {
            System.out.println("WARN " + warning);
        }
        if (errors.isEmpty()) {
            System.out.println("OK skill trees are connected, level-gated to 60, and fit learned ability capacity.");
        }
    }
}
