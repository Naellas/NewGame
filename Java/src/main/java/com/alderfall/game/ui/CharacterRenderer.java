package com.alderfall.game.ui;

import com.alderfall.game.*;

import com.alderfall.game.inventory.Equipment;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CharacterRenderer {
    private final AssetStore assets;
    private final GameState state;
    private final Effects effects;

    public CharacterRenderer(AssetStore assets, GameState state, Effects effects) {
        this.assets = assets;
        this.state = state;
        this.effects = effects;
    }

    private static final String[][] ATTRIBUTE_STATS = {
            {"STR", "strength"},
            {"INT", "intelligence"},
            {"DEX", "dexterity"},
            {"CHA", "charisma"},
            {"CON", "constitution"},
            {"WIL", "willpower"}
    };

    private static String shortText(String text, int maxChars) {
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, Math.max(0, maxChars - 3)) + "...";
    }

    private static String slotLabel(String slot) {
        return switch (slot) {
            case "chestpiece" -> "CHEST";
            case "pauldrons" -> "SHOULDERS";
            case "leggings" -> "LEGS";
            default -> slot.toUpperCase();
        };
    }

    public void drawSkillTree(Graphics2D g) {
        int x = 18;
        int y = 18;
        int w = Math.max(760, effects.gameAreaWidth() - 36);
        int h = Math.max(620, effects.viewHeight() - 36);
        effects.drawOverlayBase(g, x, y, w, h);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(244, 239, 220));
        g.drawString("Skills", x + 36, y + 48);
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g.setColor(new Color(210, 213, 222));
        g.drawString(state.player.className + "  Level " + state.player.level + "  Skill " + state.player.skillPoints
                + "   Stat " + state.player.statPoints + "   Profession " + state.player.professionSkillPoints
                + "   Wheel scrolls the active tree", x + 36, y + 78);

        drawSkillTabs(g, x + 36, y + 94, 148, false);
        drawStatAllocator(g, state.player, x + 598, y + 98);
        if (effects.activeSkillTab() == SkillTab.LOADOUT) {
            drawAbilityLoadoutTab(g, state.player, x + 36, y + 142, w - 72, h - 222);
            drawAbilityDragGhost(g);
        } else if (effects.activeSkillTab() == SkillTab.PROFESSIONS) {
            drawSkillGraph(g, state.player, activeSkillTitle(state.player), activeSkillTree(state.player),
                    x + 36, y + 142, Math.max(520, w - 478), h - 222, false);
            drawProfessionColumn(g, state.player, x + w - 376, y + 142, 340, h - 222);
        } else {
            drawSkillGraph(g, state.player, activeSkillTitle(state.player), activeSkillTree(state.player),
                    x + 36, y + 142, w - 72, h - 222, false);
        }

        int spent = SkillTrees.spent(state.player.skillAllocations);
        int respecCost = SkillTrees.respecCost(state.player.level, spent);
        boolean canRespec = spent > 0 && state.player.gold >= respecCost;
        effects.actionButton(g, x + w - 330, y + h - 54, 154, 34, "Respec " + respecCost + "g", state::respecSkills, new Color(96, 66, 59), new Color(165, 103, 88), canRespec);
        effects.actionButton(g, x + w - 160, y + h - 54, 120, 34, "Close", state::toggleSkills, new Color(58, 72, 100), new Color(107, 126, 166), true);
    }

    private void drawProfessionColumn(Graphics2D g, Actor actor, int x, int y, int w, int h) {
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.setColor(new Color(246, 224, 151));
        g.drawString("Professions", x, y);
        int originalX = x;
        int index = 0;
        int rows = (Profession.ALL.size() + 1) / 2;
        int step = Math.min(58, Math.max(42, (h - 26) / rows));
        w = (w - 10) / 2;
        for (Profession profession : Profession.ALL) {
            x = originalX + (index / rows) * (w + 10);
            int rowY = y + 26 + (index % rows) * step;
            index++;
            boolean selected = profession.id().equals(effects.activeProfessionId());
            int level = actor.professionLevel(profession.id());
            int cap = actor.professionLevelCap(profession.id());
            int xp = actor.professionXp.getOrDefault(profession.id(), 0);
            int current = Math.max(0, xp - Profession.xpForLevel(level, cap));
            int needed = level >= cap ? 1 : Profession.xpToNext(level);
            g.setColor(selected ? new Color(45, 58, 48, 238) : new Color(28, 32, 43, 232));
            g.fillRoundRect(x, rowY, w, 46, 8, 8);
            g.setColor(selected ? new Color(132, 157, 104) : new Color(96, 106, 133));
            g.drawRoundRect(x, rowY, w, 46, 8, 8);
            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            g.setColor(new Color(235, 236, 240));
            g.drawString(profession.label() + "  " + level + "/" + cap, x + 12, rowY + 17);
            int barX = x + 12;
            int barY = rowY + 28;
            int barW = w - 24;
            g.setColor(new Color(45, 50, 63));
            g.fillRoundRect(barX, barY, barW, 8, 6, 6);
            g.setColor(new Color(103, 151, 117));
            int fill = level >= cap ? barW : Math.max(2, Math.min(barW, barW * current / Math.max(1, needed)));
            g.fillRoundRect(barX, barY, fill, 8, 6, 6);
            Rectangle rowBounds = new Rectangle(x, rowY, w, 46);
            String selectedProfession = profession.id();
            effects.addButton(rowBounds, "profession-tree:" + selectedProfession, () -> {
                effects.setActiveProfessionId(selectedProfession);
                effects.setSkillTreeScroll(0);
                effects.setPartySkillScroll(0);
                effects.repaintPanel();
            });
            effects.addTooltip(rowBounds, profession.label(),
                    "Click to view the " + profession.label() + " skill tree. Current level "
                            + level + "/" + cap + ".");

        }
    }

    private void drawSkillTabs(Graphics2D g, int x, int y, int tabW, boolean party) {
        int tabX = x;
        for (SkillTab tab : SkillTab.values()) {
            boolean active = effects.activeSkillTab() == tab;
            SkillTab target = tab;
            effects.actionButton(g, tabX, y, tabW, 30, tab.label(), () -> {
                effects.setActiveSkillTab(target);
                effects.setSkillTreeScroll(0);
                effects.setPartySkillScroll(0);
                effects.setPartyLoadoutScroll(0);
                effects.repaintPanel();
            }, active ? new Color(79, 90, 60) : new Color(46, 54, 72),
                    active ? new Color(139, 154, 96) : new Color(91, 103, 132), true);
            tabX += tabW + (party ? 8 : 12);
        }
    }

    private String activeSkillTitle(Actor actor) {
        return switch (effects.activeSkillTab()) {
            case SURVIVAL -> "Survival";
            case CLASS -> actor.className;
            case PROFESSIONS -> Profession.label(effects.activeProfessionId());
            case LOADOUT -> "Loadout";
        };
    }

    private Map<String, SkillNode> activeSkillTree(Actor actor) {
        return switch (effects.activeSkillTab()) {
            case SURVIVAL -> SkillTrees.COMMON_SKILL_TREE;
            case CLASS -> SkillTrees.skillTreeForClass(actor.className);
            case PROFESSIONS -> SkillTrees.professionSkillTree(effects.activeProfessionId());
            case LOADOUT -> Map.of();
        };
    }

    private void drawSkillGraph(Graphics2D g, Actor actor, String title, Map<String, SkillNode> tree,
                                int x, int y, int w, int h, boolean party) {
        boolean compact = false;
        g.setFont(new Font("SansSerif", Font.BOLD, party ? 16 : 18));
        g.setColor(new Color(246, 224, 151));
        g.drawString(title, x, y);
        if (tree.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.setColor(new Color(198, 202, 211));
            g.drawString("No skill tree for this class yet.", x, y + 40);
            return;
        }
        int clipY = y + 22;
        int graphH = h - 22;
        int maxGridX = 0;
        int maxGridY = 0;
        for (SkillNode node : tree.values()) {
            maxGridX = Math.max(maxGridX, node.x());
            maxGridY = Math.max(maxGridY, node.y());
        }
        int columns = Math.max(1, maxGridX + 1);
        int gapX = compact ? 18 : 34;
        int naturalW = (w - 24 - gapX * Math.max(0, columns - 1)) / columns;
        int cardW = Math.max(compact ? 150 : 210, Math.min(compact ? 190 : 280, naturalW));
        int cardH = compact ? 58 : 74;
        int rowH = compact ? 94 : 118;
        int contentH = (maxGridY + 1) * rowH + cardH + 12;
        int scrollUnit = 32;
        int maxScroll = Math.max(0, (contentH - graphH + scrollUnit - 1) / scrollUnit);
        int scroll = Math.max(0, Math.min(party ? effects.partySkillScroll() : effects.skillTreeScroll(), maxScroll));
        int offsetY = scroll * scrollUnit;
        Map<String, Rectangle> nodeRects = new HashMap<>();
        int contentX = x + 12;
        int graphW = w - 24;
        int colStep = columns <= 1 ? 0 : Math.max(1, (graphW - cardW) / Math.max(1, columns - 1));
        for (SkillNode node : tree.values()) {
            int nodeX = contentX + node.x() * colStep;
            int nodeY = clipY + node.y() * rowH - offsetY;
            nodeRects.put(node.id(), new Rectangle(nodeX, nodeY, cardW, cardH));
        }

        Graphics2D graphG = (Graphics2D) g.create();
        graphG.setClip(x, clipY, w, graphH);
        graphG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (SkillNode node : tree.values()) {
            Rectangle to = nodeRects.get(node.id());
            if (to == null) {
                continue;
            }
            for (String required : node.requires()) {
                Rectangle from = nodeRects.get(required);
                if (from == null) {
                    continue;
                }
                boolean unlocked = actor.skillRank(required) > 0;
                boolean canLearn = canShowAllocate(actor, node);
                int fromX = from.x + from.width / 2;
                int fromY = from.y + from.height;
                int toX = to.x + to.width / 2;
                int toY = to.y;
                int midY = fromY + Math.max(10, (toY - fromY) / 2);
                graphG.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                graphG.setColor(new Color(6, 8, 14, 190));
                graphG.drawLine(fromX, fromY, fromX, midY);
                graphG.drawLine(fromX, midY, toX, midY);
                graphG.drawLine(toX, midY, toX, toY);
                graphG.setStroke(new BasicStroke(unlocked || canLearn ? 3f : 2.25f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                graphG.setColor(unlocked ? new Color(128, 206, 154, 235)
                        : canLearn ? new Color(218, 198, 119, 220)
                        : new Color(112, 122, 154, 195));
                graphG.drawLine(fromX, fromY, fromX, midY);
                graphG.drawLine(fromX, midY, toX, midY);
                graphG.drawLine(toX, midY, toX, toY);
            }
        }
        graphG.setStroke(new BasicStroke(1f));
        for (SkillNode node : tree.values()) {
            Rectangle bounds = nodeRects.get(node.id());
            if (bounds != null && bounds.y + bounds.height >= clipY && bounds.y <= clipY + graphH) {
                drawSkillNode(graphG, actor, node, bounds.x, bounds.y, bounds.width, bounds.height,
                        () -> {
                            if (party) {
                                state.allocatePartySkill(node.id());
                            } else {
                                state.allocateSkill(node.id());
                            }
                        }, compact);
            }
        }
        graphG.dispose();
        if (maxScroll > 0) {
            int visibleUnits = Math.max(1, graphH / scrollUnit);
            effects.drawScrollIndicator(g, x + w + 8, clipY, graphH, maxScroll + visibleUnits, scroll, visibleUnits);
        }
    }

    private void drawSkillNode(Graphics2D g, Actor actor, SkillNode node, int x, int y, int w, int h,
                               Runnable allocate, boolean compact) {
        int rank = actor.skillRank(node.id());
        boolean learned = rank > 0;
        boolean canLearn = canShowAllocate(actor, node);
        boolean abilityNode = node.ability() != null;
        g.setColor(learned ? new Color(38, 58, 50, 242)
                : canLearn ? new Color(37, 41, 55, 244)
                : new Color(24, 27, 38, 238));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setStroke(new BasicStroke(learned || canLearn ? 2f : 1.35f));
        g.setColor(learned ? new Color(130, 207, 153)
                : canLearn ? new Color(222, 199, 118)
                : new Color(88, 96, 122));
        g.drawRoundRect(x, y, w, h, 8, 8);
        if (abilityNode) {
            g.setColor(new Color(139, 107, 168, learned ? 110 : 78));
            g.fillRoundRect(x + 4, y + 4, 5, h - 8, 4, 4);
        }
        g.setFont(new Font("SansSerif", Font.BOLD, compact ? 11 : 13));
        g.setColor(new Color(235, 236, 240));
        effects.drawClippedString(g, node.name(), x + (abilityNode ? 14 : 10), y + (compact ? 16 : 18), w - 62);
        g.setFont(new Font("SansSerif", Font.BOLD, compact ? 10 : 11));
        g.setColor(new Color(246, 224, 151));
        g.drawString(rank + "/" + node.maxRank(), x + 10, y + (compact ? 32 : 36));
        g.setFont(new Font("SansSerif", Font.PLAIN, compact ? 10 : 11));
        g.setColor(new Color(176, 182, 196));
        effects.drawClippedString(g, skillNodeStatus(actor, node), x + 38, y + (compact ? 32 : 36), w - 96);
        effects.addTooltip(new Rectangle(x, y, w, h), node.name(), skillNodeTooltip(actor, node),
                node.ability() == null ? null : effects.abilityIconName(node.ability()));
        effects.actionButton(g, x + w - (compact ? 42 : 50), y + 8, compact ? 30 : 36, h - 16, "+", allocate, new Color(68, 90, 53), new Color(110, 139, 92), canLearn);
        g.setStroke(new BasicStroke(1f));
    }

    private String skillNodeStatus(Actor actor, SkillNode node) {
        if (actor.skillRank(node.id()) >= node.maxRank()) {
            return "Mastered";
        }
        if (actor.level < node.levelRequirement()) {
            return "Level " + node.levelRequirement();
        }
        for (String required : node.requires()) {
            if (actor.skillRank(required) <= 0) {
                SkillNode requiredNode = SkillTrees.SKILL_TREE.get(required);
                return "Needs " + (requiredNode == null ? required : requiredNode.name());
            }
        }
        if (node.ability() != null) {
            return actor.skillRank(node.id()) > 0 ? "Upgrade " + node.ability().name() : "Unlocks " + node.ability().name();
        }
        return SkillTrees.isProfessionSkill(node.id()) ? "Uses profession point" : "Hover for details";
    }

    private String skillNodeTooltip(Actor actor, SkillNode node) {
        List<String> lines = new ArrayList<>();
        lines.add(skillNodeFlavor(node));
        lines.add(node.description());
        lines.add("Level: " + node.levelRequirement() + " required.");
        if (!node.effects().isEmpty()) {
            lines.add("Innate: " + skillEffectsText(node.effects()) + ".");
        }
        if (node.ability() != null) {
            Ability ability = node.ability();
            int rank = Math.max(1, actor.skillRank(node.id()) + 1);
            int extraRanks = Math.max(0, rank - 1);
            int powerGain = switch (ability.kind()) {
                case DAMAGE -> "all_enemies".equals(ability.target()) ? 6 : 9;
                case HEAL -> "party".equals(ability.target()) ? 5 : 7;
                case DEFEND -> 0;
            };
            int costGain = ability.kind() == Ability.AbilityKind.DEFEND ? 1 : 2;
            int previewPower = ability.power() + extraRanks * powerGain;
            int previewCost = ability.cost() + extraRanks * costGain;
            lines.add("Ability rank " + Math.min(rank, node.maxRank()) + ": " + ability.name() + ".");
            lines.add(skillRankFormula(ability, extraRanks, powerGain, costGain, previewPower, previewCost));
            if (ability.kind() == Ability.AbilityKind.DEFEND) {
                lines.add("Guard cost: " + previewCost + " MP.");
            } else {
                lines.add("Power: " + previewPower + ". Cost: " + previewCost + " MP.");
            }
            if (ability.cooldown() > 0) {
                lines.add("Cooldown: " + ability.cooldown() + " turn(s).");
            }
            lines.add("Scaling: " + effects.scalingLabel(ability) + ".");
            lines.add(skillAbilityFormula(actor, ability, previewPower));
            List<AbilityStatus> statuses = GameData.statusHintsForAbility(ability);
            if (!statuses.isEmpty()) {
                lines.add("Status: " + String.join(", ", statuses.stream().map(effects::abilityStatusLabel).toList()) + ".");
            }
        }
        if (!node.requires().isEmpty()) {
            List<String> requirements = new ArrayList<>();
            for (String required : node.requires()) {
                SkillNode requiredNode = SkillTrees.SKILL_TREE.get(required);
                String name = requiredNode == null ? required : requiredNode.name();
                requirements.add(name + (actor.skillRank(required) > 0 ? " learned" : " needed"));
            }
            lines.add("Requires: " + String.join(", ", requirements) + ".");
        }
        lines.add("Rank: " + actor.skillRank(node.id()) + "/" + node.maxRank() + ".");
        lines.add(SkillTrees.isProfessionSkill(node.id()) ? "Costs profession skill points." : "Costs regular skill points.");
        return String.join("\n", lines);
    }

    private String skillEffectsText(Map<String, Integer> effects) {
        return effects.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> signed(entry.getValue()) + " " + skillEffectLabel(entry.getKey()))
                .toList()
                .stream()
                .reduce((first, second) -> first + ", " + second)
                .orElse("no innate stat change");
    }

    private String skillEffectLabel(String key) {
        return switch (key) {
            case "max_hp" -> "max HP";
            case "max_mp" -> "max MP";
            case "attack" -> "ATK";
            case "defense" -> "DEF";
            case "strength" -> "STR";
            case "intelligence" -> "INT";
            case "dexterity" -> "DEX";
            case "charisma" -> "CHA";
            case "constitution" -> "CON";
            case "willpower" -> "WIL";
            default -> key.replace('_', ' ');
        };
    }

    private String signed(int value) {
        return value >= 0 ? "+" + value : Integer.toString(value);
    }

    private String skillRankFormula(Ability ability, int extraRanks, int powerGain, int costGain, int previewPower, int previewCost) {
        if (ability.kind() == Ability.AbilityKind.DEFEND) {
            return "Rank math: cost " + ability.cost() + " + " + extraRanks + " rank(s) x " + costGain
                    + " = " + previewCost + " MP.";
        }
        return "Rank math: power " + ability.power() + " + " + extraRanks + " rank(s) x " + powerGain
                + " = " + previewPower + "; cost " + ability.cost() + " + " + extraRanks
                + " rank(s) x " + costGain + " = " + previewCost + " MP.";
    }

    private String skillAbilityFormula(Actor actor, Ability ability, int previewPower) {
        return switch (ability.kind()) {
            case DAMAGE -> {
                int attributeBonus = actor.abilityScalingBonus(ability);
                int skillBonus = actor == state.player ? state.player.skillRank("spellcraft") * 2 + state.player.skillRank("overchannel") * 3 : 0;
                if (actor == state.player && "all_enemies".equals(ability.target())) {
                    skillBonus += state.player.skillRank("volley_mastery") * 2;
                }
                int total = previewPower + attributeBonus + skillBonus;
                yield "Formula: " + previewPower + " power + " + effects.scalingLabel(ability) + " bonus " + attributeBonus
                        + (skillBonus > 0 ? " + skill bonuses " + skillBonus : "")
                        + " + random 0-5 = " + total + "-" + (total + 5) + " before defenses.";
            }
            case HEAL -> {
                int levelPart = actor.level;
                int attributeBonus = actor.abilityScalingBonus(ability);
                int channeling = actor == state.player ? state.player.skillRank("channeling") * 4 : 0;
                int total = previewPower + levelPart + attributeBonus + channeling;
                yield "Formula: " + previewPower + " power + level " + levelPart
                        + " + " + effects.scalingLabel(ability) + " bonus " + attributeBonus
                        + (channeling > 0 ? " + channeling " + channeling : "")
                        + " + random -2 to +2 = " + Math.max(1, total - 2) + "-" + (total + 2) + " HP.";
            }
            case DEFEND -> {
                int wilPart = actor.willpower / 3;
                int strPart = actor.strength / 5;
                yield "Innate guard: WIL/3 " + wilPart + " + STR/5 " + strPart + " = +" + (wilPart + strPart)
                        + " guard strength before active status effects.";
            }
        };
    }

    private boolean isMentalAbilityForTooltip(Actor actor, Ability ability) {
        String lowerName = ability.name().toLowerCase();
        if (lowerName.contains("fire") || lowerName.contains("frost") || lowerName.contains("arcane")
                || lowerName.contains("radiant") || lowerName.contains("thorn") || lowerName.contains("sun")
                || lowerName.contains("moon") || lowerName.contains("star") || lowerName.contains("bloom")
                || lowerName.contains("mend") || lowerName.contains("heal") || lowerName.contains("ward")
                || lowerName.contains("prayer") || lowerName.contains("chorus") || lowerName.contains("refuge")) {
            return true;
        }
        return switch (actor.className) {
            case "Mage", "Cleric", "Medic", "Battle Medic", "Wildspeaker", "Sunwarden", "Thornbinder", "Grovekeeper" -> true;
            default -> false;
        };
    }

    private String skillNodeFlavor(SkillNode node) {
        if (node.ability() != null) {
            return effects.abilityFlavor(node.ability());
        }
        String description = node.description().toLowerCase();
        if (description.contains("max hp") || description.contains("defense")) {
            return "Hard lessons settle into muscle, mail, and old survival habits.";
        }
        if (description.contains("max mp") || description.contains("healing")) {
            return "The inner well deepens, holding steadier light for darker roads.";
        }
        if (description.contains("attack") || description.contains("damage")) {
            return "Practice turns motion into intent, and intent into a sharper blow.";
        }
        if (SkillTrees.isProfessionSkill(node.id())) {
            return "Trade craft becomes ritual: measured hands, trusted tools, patient work.";
        }
        return "A road-worn lesson, earned one dangerous evening at a time.";
    }

    private boolean canShowAllocate(SkillNode node) {
        return canShowAllocate(state.player, node);
    }

    private boolean canShowAllocate(Actor actor, SkillNode node) {
        boolean professionSkill = SkillTrees.isProfessionSkill(node.id());
        if ((professionSkill ? actor.professionSkillPoints <= 0 : actor.skillPoints <= 0) || actor.skillRank(node.id()) >= node.maxRank()) {
            return false;
        }
        if (actor.level < node.levelRequirement()) {
            return false;
        }
        if (!SkillTrees.availableSkillTree(actor.className).containsKey(node.id())) {
            return false;
        }
        for (String required : node.requires()) {
            if (actor.skillRank(required) <= 0) {
                return false;
            }
        }
        return true;
    }

    public void drawPartyOverview(Graphics2D g) {
        int x = 18;
        int y = 18;
        int w = Math.max(920, effects.gameAreaWidth() - 36);
        int h = Math.max(680, effects.viewHeight() - 36);
        effects.drawOverlayBase(g, x, y, w, h);
        Actor actor = state.partyScreenActor();
        List<Actor> members = state.partyMembers();

        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(244, 239, 220));
        g.drawString("Character", x + 34, y + 48);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(198, 202, 211));
        g.drawString("Gold " + state.player.gold + "   Select an ally to manage stats, skills, and professions.", x + 34, y + 76);

        int memberY = y + 110;
        for (int i = 0; i < members.size(); i++) {
            Actor member = members.get(i);
            boolean selected = member == actor;
            g.setColor(selected ? new Color(42, 57, 50, 235) : new Color(28, 32, 43, 235));
            g.fillRoundRect(x + 34, memberY, 198, 58, 8, 8);
            g.setColor(selected ? new Color(103, 151, 117) : new Color(82, 92, 116));
            g.drawRoundRect(x + 34, memberY, 198, 58, 8, 8);
            Rectangle memberBounds = new Rectangle(x + 34, memberY, 198, 58);
            effects.addPartyPortraitZone(memberBounds, member);
            g.drawImage(assets.spriteFit(effects.dialoguePortraitSprite(member), 40, 50), x + 44, memberY + 4, null);
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.setColor(new Color(235, 236, 240));
            g.drawString((i + 1) + ". " + member.name, x + 96, memberY + 23);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.setColor(new Color(176, 182, 196));
            g.drawString(member.className + "  Lv " + member.level, x + 96, memberY + 42);
            int index = i;
            effects.addButton(memberBounds, "party-member:" + member.name, () -> {
                state.selectPartyScreenActor(index);
                effects.setPartySkillScroll(0);
                effects.setPartyLoadoutScroll(0);
                effects.repaintPanel();
            });
            memberY += 68;
        }

        int detailX = x + 242;
        int contentRight = x + w - 34;
        Rectangle detailPortraitBounds = new Rectangle(detailX, y + 96, 92, 120);
        effects.addPartyPortraitZone(detailPortraitBounds, actor);
        g.drawImage(assets.spriteFit(effects.dialoguePortraitSprite(actor), 92, 120), detailX, y + 96, null);
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.setColor(new Color(246, 224, 151));
        g.drawString(actor.name + " the " + actor.className, detailX + 112, y + 122);
        drawPartyCombatSummary(g, actor, detailX + 112, y + 148);
        drawAttributePills(g, actor, detailX + 112, y + 184);
        actor.sanitizeAbilityLoadout();
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(new Color(206, 211, 221));
        g.drawString("Known " + actor.abilities.size() + "   Prepared " + actor.activeAbilityNames.size()
                + "/" + Actor.MAX_ACTIVE_ABILITIES, detailX + 112, y + 232);
        int statY = y + 262;
        drawStatAllocator(g, actor, detailX, statY);

        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(new Color(246, 224, 151));
        int skillHeaderY = statY + 52;
        g.drawString("Skill Trees", detailX, skillHeaderY);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(176, 182, 196));
        g.drawString("Use the mouse wheel to scroll the active tree.", detailX + 94, skillHeaderY);
        drawSkillTabs(g, detailX, skillHeaderY + 14, 132, true);

        int graphY = skillHeaderY + 60;
        int graphH = Math.max(260, y + h - graphY - 86);
        int graphW = contentRight - detailX;
        if (effects.activeSkillTab() == SkillTab.LOADOUT) {
            drawAbilityLoadoutTab(g, actor, detailX, graphY, graphW, graphH);
        } else if (effects.activeSkillTab() == SkillTab.PROFESSIONS) {
            int columnW = Math.min(320, Math.max(280, graphW / 3));
            drawSkillGraph(g, actor, activeSkillTitle(actor), activeSkillTree(actor), detailX, graphY, graphW - columnW - 28, graphH, true);
            drawProfessionColumn(g, actor, contentRight - columnW, graphY, columnW, graphH);
        } else {
            drawSkillGraph(g, actor, activeSkillTitle(actor), activeSkillTree(actor), detailX, graphY, graphW, graphH, true);
        }
        drawAbilityDragGhost(g);

        int spent = SkillTrees.spent(actor.skillAllocations);
        int respecCost = SkillTrees.respecCost(actor.level, spent);
        boolean canRespec = spent > 0 && state.player.gold >= respecCost;
        effects.actionButton(g, x + w - 330, y + h - 54, 154, 34, "Respec " + respecCost + "g", state::respecPartySkills, new Color(96, 66, 59), new Color(165, 103, 88), canRespec);
        effects.actionButton(g, x + w - 160, y + h - 54, 120, 34, "Close", state::closeOverlay, new Color(58, 72, 100), new Color(107, 126, 166), true);
    }

    private int drawAbilityLoadoutEditor(Graphics2D g, Actor actor, int x, int y, int w) {
        actor.sanitizeAbilityLoadout();
        List<Ability> activeAbilities = actor.activeAbilities();
        List<Ability> known = new ArrayList<>(activeAbilities);
        for (Ability ability : actor.abilities) {
            if (!actor.isAbilityActive(ability.name())) {
                known.add(ability);
            }
        }
        int activeCount = activeAbilities.size();
        int headerH = 22;
        int viewportH = known.size() > 10 ? 86 : Math.max(32, ((Math.max(1, known.size()) + 2) / 3) * 28);
        effects.setPartyLoadoutBounds(new Rectangle(x, y + headerH, w, viewportH));
        int columns = Math.max(2, Math.min(4, w / 176));
        int gap = 6;
        int rowH = 24;
        int chipW = Math.max(126, (w - (columns - 1) * gap) / columns);
        int rows = Math.max(1, (known.size() + columns - 1) / columns);
        int visibleRows = Math.max(1, viewportH / (rowH + gap));
        int maxScroll = Math.max(0, rows - visibleRows);
        effects.setPartyLoadoutScroll(Math.max(0, Math.min(effects.partyLoadoutScroll(), maxScroll)));

        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(new Color(246, 224, 151));
        g.drawString("Battle Loadout " + activeCount + "/" + Actor.MAX_ACTIVE_ABILITIES, x, y + 14);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(176, 182, 196));
        effects.drawClippedString(g, "Click learned abilities to prepare or set aside for combat.", x + 176, y + 14, w - 176);

        Shape previousClip = g.getClip();
        g.setClip(new Rectangle(x, y + headerH, w, viewportH));
        for (int i = 0; i < known.size(); i++) {
            Ability ability = known.get(i);
            int row = i / columns;
            int col = i % columns;
            int chipX = x + col * (chipW + gap);
            int chipY = y + headerH + (row - effects.partyLoadoutScroll()) * (rowH + gap);
            if (chipY + rowH < (y + headerH) || chipY > (y + headerH) + viewportH) {
                continue;
            }
            boolean active = actor.isAbilityActive(ability.name());
            boolean enabled = active || activeCount < Actor.MAX_ACTIVE_ABILITIES;
            Color base = active ? new Color(48, 82, 62, 238) : new Color(28, 32, 43, 226);
            Color edge = active ? new Color(119, 184, 132) : enabled ? new Color(85, 96, 124) : new Color(78, 70, 84);
            g.setColor(base);
            g.fillRoundRect(chipX, chipY, chipW, rowH, 7, 7);
            g.setColor(edge);
            g.drawRoundRect(chipX, chipY, chipW, rowH, 7, 7);
            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            g.setColor(enabled ? new Color(235, 236, 240) : new Color(132, 136, 148));
            effects.drawClippedString(g, (active ? "+ " : "- ") + ability.name(), chipX + 8, chipY + 16, chipW - 44);
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g.setColor(new Color(202, 209, 221));
            String cost = ability.cost() <= 0 ? "free" : ability.cost() + " MP";
            effects.drawClippedString(g, cost, chipX + chipW - 40, chipY + 16, 34);
            Rectangle bounds = new Rectangle(chipX, chipY, chipW, rowH);
            if (enabled) {
                effects.addButton(bounds, "loadout:" + actor.name + ":" + ability.name(),
                        () -> state.togglePartyAbilityLoadout(ability.name()));
            }
            effects.addTooltip(bounds, ability.name(), abilityLoadoutTooltip(actor, ability),
                    effects.abilityIconName(ability), active ? new Color(119, 184, 132) : edge);
        }
        g.setClip(previousClip);
        effects.drawScrollIndicator(g, x + w + 5, (y + headerH), viewportH, rows, effects.partyLoadoutScroll(), visibleRows);
        return headerH + viewportH;
    }

    private String abilityLoadoutTooltip(Actor actor, Ability ability) {
        List<String> damageTypes = GameData.damageTypesForAbility(ability);
        List<AbilityStatus> statuses = GameData.statusHintsForAbility(ability);
        int attributeBonus = actor.abilityScalingBonus(ability);
        String target = switch (ability.target()) {
            case "all_enemies" -> "all enemies";
            case "party" -> "party";
            case "self" -> "self";
            default -> "single target";
        };
        String role = switch (ability.kind()) {
            case DAMAGE -> "Damage: " + GameData.damageTypeListLabel(damageTypes)
                    + ", power " + ability.power() + " + attributes " + attributeBonus + ".";
            case HEAL -> "Restore: power " + ability.power() + " + level " + actor.level
                    + " + attributes " + attributeBonus + ".";
            case DEFEND -> "Defense: guard stance using " + effects.scalingLabel(ability) + ".";
        };
        return "Target: " + target + ". Cost: " + ability.cost() + " MP."
                + (ability.cooldown() > 0 ? " Cooldown: " + ability.cooldown() + " turn(s)." : "")
                + "\nScaling: " + effects.scalingLabel(ability) + ".\n"
                + role
                + effects.abilityStatusText(statuses)
                + effects.abilityMechanicsText(ability)
                + (actor.isAbilityActive(ability.name()) ? "\nPrepared in battle." : "\nNot currently prepared.");
    }

    private void drawPartyCombatSummary(Graphics2D g, Actor actor, int x, int y) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(226, 229, 236));
        g.drawString("Lv " + actor.level + "   HP " + actor.hp + "/" + actor.maxHp + "   MP " + actor.mp + "/" + actor.maxMp, x, y);
        g.setColor(new Color(186, 192, 205));
        g.drawString("ATK " + actor.attack + "   DEF " + actor.defense
                + "   Skill " + actor.skillPoints + "   Stat " + actor.statPoints
                + "   Prof " + actor.professionSkillPoints, x, y + 22);
    }

    private void drawAbilityLoadoutTab(Graphics2D g, Actor actor, int x, int y, int w, int h) {
        actor.sanitizeAbilityLoadout();
        actor.sanitizeAbilityLoadoutPresets();
        int presetY = y;
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(new Color(246, 224, 151));
        g.drawString("Battle Loadouts", x, presetY + 18);
        int presetX = x + 170;
        for (String preset : List.of("Travel", "Boss", "Support")) {
            boolean saved = actor.abilityLoadoutPresets.containsKey(preset);
            effects.actionButton(g, presetX, presetY, 78, 26, "Use " + preset, () -> state.applyPartyLoadoutPreset(preset),
                    new Color(46, 54, 72), new Color(91, 103, 132), saved);
            effects.actionButton(g, presetX + 84, presetY, 82, 26, "Save", () -> state.savePartyLoadoutPreset(preset),
                    new Color(68, 74, 48), new Color(139, 154, 96), true);
            presetX += 178;
        }

        int top = y + 42;
        int leftW = Math.max(360, w - 430);
        int rightX = x + leftW + 28;
        int rightW = w - leftW - 28;
        drawLoadoutKnownAbilities(g, actor, x, top, leftW, h - 48);
        drawLoadoutPreparedSlots(g, actor, rightX, top, rightW, h - 48);
    }

    private void drawLoadoutKnownAbilities(Graphics2D g, Actor actor, int x, int y, int w, int h) {
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(new Color(230, 225, 206));
        g.drawString("Known Abilities", x, y + 16);
        int listY = y + 28;
        int rowH = 36;
        int gap = 8;
        int cols = Math.max(1, w / 230);
        int cardW = Math.max(190, (w - gap * (cols - 1)) / cols);
        int rows = Math.max(1, (actor.abilities.size() + cols - 1) / cols);
        int visibleRows = Math.max(1, (h - 34) / (rowH + gap));
        int maxScroll = Math.max(0, rows - visibleRows);
        effects.setPartyLoadoutScroll(Math.max(0, Math.min(effects.partyLoadoutScroll(), maxScroll)));
        Rectangle clip = new Rectangle(x, listY, w, h - 34);
        effects.setPartyLoadoutBounds(clip);
        Shape oldClip = g.getClip();
        g.setClip(clip);
        for (int i = 0; i < actor.abilities.size(); i++) {
            Ability ability = actor.abilities.get(i);
            int row = i / cols;
            int col = i % cols;
            int bx = x + col * (cardW + gap);
            int by = listY + (row - effects.partyLoadoutScroll()) * (rowH + gap);
            if (by + rowH < clip.y || by > clip.y + clip.height) {
                continue;
            }
            boolean prepared = actor.isAbilityActive(ability.name());
            g.setColor(prepared ? new Color(38, 58, 50, 238) : new Color(28, 32, 43, 235));
            g.fillRoundRect(bx, by, cardW, rowH, 8, 8);
            g.setColor(prepared ? new Color(130, 207, 153) : new Color(82, 92, 116));
            g.drawRoundRect(bx, by, cardW, rowH, 8, 8);
            g.drawImage(assets.sprite(effects.abilityIconName(ability), 24), bx + 8, by + 6, null);
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.setColor(new Color(235, 236, 240));
            effects.drawClippedString(g, ability.name(), bx + 40, by + 15, cardW - 52);
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g.setColor(new Color(176, 182, 196));
            effects.drawClippedString(g, ability.cost() + " MP  " + effects.scalingLabel(ability), bx + 40, by + 30, cardW - 52);
            Rectangle bounds = new Rectangle(bx, by, cardW, rowH);
            effects.addAbilityDragZone(bounds, ability.name(), -1);
            effects.addTooltip(bounds, ability.name(), abilityLoadoutTooltip(actor, ability),
                    effects.abilityIconName(ability), prepared ? new Color(130, 207, 153) : new Color(82, 92, 116));
        }
        g.setClip(oldClip);
        effects.drawScrollIndicator(g, x + w + 5, listY, h - 34, rows, effects.partyLoadoutScroll(), visibleRows);
    }

    private void drawLoadoutPreparedSlots(Graphics2D g, Actor actor, int x, int y, int w, int h) {
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(new Color(230, 225, 206));
        g.drawString("Prepared Slots", x, y + 16);
        List<Ability> active = actor.activeAbilities();
        int slotY = y + 30;
        int slotH = 40;
        for (int i = 0; i < Actor.MAX_ACTIVE_ABILITIES; i++) {
            Ability ability = i < active.size() ? active.get(i) : null;
            Rectangle bounds = new Rectangle(x, slotY + i * (slotH + 7), w, slotH);
            boolean hovered = effects.abilityDragActive() && effects.abilityDragPoint() != null && bounds.contains(effects.abilityDragPoint());
            g.setColor(hovered ? new Color(43, 61, 55, 238) : new Color(22, 26, 36, 232));
            g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
            g.setColor(hovered ? new Color(130, 207, 153) : new Color(82, 92, 116));
            g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.setColor(new Color(246, 224, 151));
            g.drawString((i == 9 ? "0" : Integer.toString(i + 1)) + ".", bounds.x + 10, bounds.y + 25);
            if (ability == null) {
                g.setColor(new Color(145, 154, 174));
                g.drawString("Drop ability here", bounds.x + 38, bounds.y + 25);
            } else {
                g.drawImage(assets.sprite(effects.abilityIconName(ability), 24), bounds.x + 36, bounds.y + 8, null);
                g.setColor(new Color(235, 236, 240));
                effects.drawClippedString(g, ability.name(), bounds.x + 68, bounds.y + 17, bounds.width - 120);
                g.setFont(new Font("SansSerif", Font.PLAIN, 10));
                g.setColor(new Color(176, 182, 196));
                effects.drawClippedString(g, ability.cost() + " MP", bounds.x + 68, bounds.y + 32, bounds.width - 120);
                effects.addAbilityDragZone(bounds, ability.name(), i);
                effects.actionButton(g, bounds.x + bounds.width - 38, bounds.y + 8, 28, 24, "x",
                        () -> state.removePartyAbilityFromLoadout(ability.name()), new Color(88, 56, 56), new Color(149, 96, 88), true);
            }
            effects.addAbilityDropZone(bounds, i, false);
        }
        Rectangle remove = new Rectangle(x, y + h - 38, w, 30);
        boolean overRemove = effects.abilityDragActive() && effects.abilityDragPoint() != null && remove.contains(effects.abilityDragPoint());
        g.setColor(overRemove ? new Color(88, 50, 50, 238) : new Color(31, 35, 47, 220));
        g.fillRoundRect(remove.x, remove.y, remove.width, remove.height, 8, 8);
        g.setColor(overRemove ? new Color(190, 103, 85) : new Color(82, 92, 116));
        g.drawRoundRect(remove.x, remove.y, remove.width, remove.height, 8, 8);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(new Color(235, 236, 240));
        effects.drawCenteredIn(g, "Drop here to remove from loadout", remove.x, remove.y + 20, remove.width);
        effects.addAbilityDropZone(remove, -1, true);
    }

    private void drawAbilityDragGhost(Graphics2D g) {
        if (!effects.abilityDragActive() || effects.abilityDragPoint() == null) {
            return;
        }
        int x = effects.abilityDragPoint().x - 20;
        int y = effects.abilityDragPoint().y - 18;
        Graphics2D ghost = (Graphics2D) g.create();
        ghost.setComposite(AlphaComposite.SrcOver.derive(0.82f));
        ghost.setColor(new Color(12, 14, 22, 230));
        ghost.fillRoundRect(x, y, 210, 34, 8, 8);
        ghost.setColor(new Color(130, 207, 153));
        ghost.drawRoundRect(x, y, 210, 34, 8, 8);
        ghost.setFont(new Font("SansSerif", Font.BOLD, 12));
        ghost.setColor(new Color(235, 236, 240));
        effects.drawClippedString(ghost, effects.abilityDragName(), x + 12, y + 22, 186);
        ghost.dispose();
    }

    private void drawAttributePills(Graphics2D g, Actor actor, int x, int y) {
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        int pillX = x;
        for (String[] stat : ATTRIBUTE_STATS) {
            Color color = attributeColor(stat[1]);
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 42));
            g.fillRoundRect(pillX, y, 70, 24, 8, 8);
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 178));
            g.drawRoundRect(pillX, y, 70, 24, 8, 8);
            g.setColor(color);
            g.drawString(stat[0] + " " + attributeValue(actor, stat[1]), pillX + 10, y + 17);
            Rectangle bounds = new Rectangle(pillX, y, 70, 24);
            effects.addTooltip(bounds, statTooltipTitle(stat[0], stat[1]),
                    statTooltip(actor, stat[1], false), null, color);
            pillX += 78;
        }
    }

    private int attributeValue(Actor actor, String statKey) {
        return switch (statKey) {
            case "strength" -> actor.strength;
            case "intelligence" -> actor.intelligence;
            case "dexterity" -> actor.dexterity;
            case "charisma" -> actor.charisma;
            case "constitution" -> actor.constitution;
            case "willpower" -> actor.willpower;
            default -> 0;
        };
    }

    private Color attributeColor(String statKey) {
        return switch (statKey) {
            case "strength" -> new Color(232, 126, 92);
            case "intelligence" -> new Color(125, 166, 246);
            case "dexterity" -> new Color(116, 203, 151);
            case "charisma" -> new Color(237, 185, 104);
            case "constitution" -> new Color(223, 104, 118);
            case "willpower" -> new Color(168, 143, 239);
            default -> new Color(206, 211, 221);
        };
    }

    private void drawStatAllocator(Graphics2D g, Actor actor, int x, int y) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(176, 182, 196));
        g.drawString("Stat Points " + actor.statPoints, x, y + 16);
        int buttonX = x + 116;
        for (String[] stat : ATTRIBUTE_STATS) {
            Color color = attributeColor(stat[1]);
            Rectangle bounds = new Rectangle(buttonX, y, 54, 22);
            effects.actionButton(g, buttonX, y, 54, 22, stat[0] + " +", () -> state.allocateStat(actor, stat[1]),
                    new Color(Math.max(24, color.getRed() / 3), Math.max(24, color.getGreen() / 3), Math.max(24, color.getBlue() / 3)),
                    color, actor.statPoints > 0);
            effects.addTooltip(bounds, statTooltipTitle(stat[0], stat[1]),
                    statTooltip(actor, stat[1], true), null, color);
            buttonX += 62;
        }
    }

    private String statTooltipTitle(String abbreviation, String statKey) {
        return abbreviation + " - " + switch (statKey) {
            case "strength" -> "Strength";
            case "intelligence" -> "Intelligence";
            case "dexterity" -> "Dexterity";
            case "charisma" -> "Charisma";
            case "constitution" -> "Constitution";
            case "willpower" -> "Willpower";
            default -> statKey;
        };
    }

    private String statTooltip(Actor actor, String statKey, boolean allocationPreview) {
        int value = attributeValue(actor, statKey);
        String current = "Current: " + value + (allocationPreview && actor.statPoints > 0 ? ". Click to spend 1 stat point." : ".");
        String effect = switch (statKey) {
            case "strength" -> "Scales basic attacks by STR/2, physical ability damage by STR/2, guard MP by STR/5, physical crit chance by +0.15% per STR, crit damage by +(STR+DEX)/120 until capped, and parry by +0.25% per STR.";
            case "intelligence" -> "Scales mental ability damage by INT/2, healing by INT/5, mental crit chance by +0.15% per INT, mental crit damage by +(INT+DEX)/120 until capped, and studied-monster damage through lore insight.";
            case "dexterity" -> "Scales basic attacks by DEX/4, all ability damage by DEX/5, crit chance by +0.7% per DEX, dodge by +0.5% per DEX, parry by +0.25% per DEX, and crit damage by +(focus+DEX)/120 until capped.";
            case "charisma" -> "Scales healing by CHA/3. It is the dedicated social/support stat and does not increase direct damage.";
            case "constitution" -> "Adds +" + Actor.HP_PER_CONSTITUTION + " max HP per CON, raises parry by +0.25% per CON, and improves staying power for low-HP defensive skills.";
            case "willpower" -> "Adds +" + Actor.MP_PER_WILLPOWER + " max MP per WIL, scales all ability damage by WIL/6, healing by WIL/2, and guard MP by WIL/3.";
            default -> "No scaling data available yet.";
        };
        return current + "\n" + effect;
    }

    private void drawPartyGearSection(Graphics2D g, Actor actor, int x, int y, int w) {
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(new Color(246, 224, 151));
        g.drawString("Equipment", x, y);
        int rowY = y + 16;
        for (String slot : GameData.EQUIPMENT_SLOTS) {
            Equipment equipment = actor.equippedItem(slot);
            g.setColor(new Color(28, 32, 43, 235));
            g.fillRoundRect(x, rowY, w, 30, 8, 8);
            g.setColor(new Color(82, 92, 116));
            g.drawRoundRect(x, rowY, w, 30, 8, 8);
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.setColor(new Color(246, 224, 151));
            g.drawString(slotLabel(slot), x + 12, rowY + 20);
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.setColor(new Color(222, 225, 232));
            g.drawString(equipment == null ? "None" : shortText(equipment.name(), 44), x + 106, rowY + 20);
            if (equipment != null) {
                effects.actionButton(g, x + w - 72, rowY + 4, 58, 22, "Off", () -> state.unequipPartySlot(slot), new Color(88, 56, 56), new Color(149, 96, 88), true);
            }
            rowY += 36;
        }
    }

    private void drawPartyPackList(Graphics2D g, Actor actor, int x, int y, int w) {
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(new Color(246, 224, 151));
        g.drawString("Pack", x, y);
        int rowY = y + 16;
        int shown = 0;
        java.util.LinkedHashSet<String> keys = new java.util.LinkedHashSet<>();
        keys.addAll(state.player.inventory.keySet());
        if (actor != state.player) {
            keys.addAll(actor.inventory.keySet());
        }
        for (String key : keys) {
            if (!GameData.isEquipment(key) && !GameData.ITEMS.containsKey(key)) {
                continue;
            }
            int count = state.player.inventory.getOrDefault(key, 0) + (actor == state.player ? 0 : actor.inventory.getOrDefault(key, 0));
            if (count <= 0) {
                continue;
            }
            g.setColor(new Color(28, 32, 43, 235));
            g.fillRoundRect(x, rowY, w, 44, 8, 8);
            g.setColor(new Color(82, 92, 116));
            g.drawRoundRect(x, rowY, w, 44, 8, 8);
            g.drawImage(assets.sprite(GameData.itemIcon(key), 28), x + 8, rowY + 8, null);
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.setColor(new Color(235, 236, 240));
            g.drawString(shortText(GameData.itemName(key), 15) + " x" + count, x + 42, rowY + 18);
            String action = GameData.isEquipment(key) ? "Eq" : "Use";
            effects.actionButton(g, x + w - 46, rowY + 12, 36, 22, action, () -> state.usePartyInventoryItem(key), new Color(68, 90, 53), new Color(110, 139, 92), true);
            rowY += 50;
            shown++;
            if (shown >= 4) {
                break;
            }
        }
        if (shown == 0) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.setColor(new Color(198, 202, 211));
            g.drawString("No gear or items.", x, rowY + 18);
        }
    }

    private void drawActorSkillColumn(Graphics2D g, Actor actor, String title, Map<String, SkillNode> tree, int x, int y, int w, int h) {
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(new Color(246, 224, 151));
        g.drawString(title, x, y);
        int clipY = y + 18;
        int rowH = 38;
        int visibleRows = Math.max(1, (h - 18) / rowH);
        int maxScroll = Math.max(0, tree.size() - visibleRows);
        int scroll = Math.max(0, Math.min(effects.partySkillScroll(), maxScroll));
        Graphics2D listG = (Graphics2D) g.create();
        listG.setClip(x, clipY, w, h - 18);
        int rowY = clipY - scroll * rowH;
        for (SkillNode node : tree.values()) {
            if (rowY + 32 >= clipY && rowY <= clipY + h - 18) {
                drawActorSkillNode(listG, actor, node, x, rowY, w);
            }
            rowY += 38;
        }
        listG.dispose();
        effects.drawScrollIndicator(g, x + w + 8, clipY, h - 18, tree.size(), scroll, visibleRows);
    }

    private void drawActorSkillNode(Graphics2D g, Actor actor, SkillNode node, int x, int y, int w) {
        int rank = actor.skillRank(node.id());
        boolean learned = rank > 0;
        boolean canLearn = canShowAllocate(actor, node);
        g.setColor(learned ? new Color(38, 50, 45, 232) : new Color(28, 32, 43, 232));
        g.fillRoundRect(x, y, w, 32, 8, 8);
        g.setColor(learned ? new Color(103, 151, 117) : canLearn ? new Color(96, 106, 133) : new Color(65, 68, 82));
        g.drawRoundRect(x, y, w, 32, 8, 8);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(new Color(235, 236, 240));
        g.drawString(shortText(node.name(), 24) + "  " + rank + "/" + node.maxRank(), x + 10, y + 20);
        effects.actionButton(g, x + w - 52, y + 5, 40, 22, "+", () -> state.allocatePartySkill(node.id()), new Color(68, 90, 53), new Color(110, 139, 92), canLearn);
    }


    public interface Effects {
        int gameAreaWidth();

        int viewHeight();

        SkillTab activeSkillTab();

        void setActiveSkillTab(SkillTab tab);

        String activeProfessionId();

        void setActiveProfessionId(String id);

        int skillTreeScroll();

        void setSkillTreeScroll(int value);

        int partySkillScroll();

        void setPartySkillScroll(int value);

        int partyLoadoutScroll();

        void setPartyLoadoutScroll(int value);

        void setPartyLoadoutBounds(Rectangle bounds);

        boolean abilityDragActive();

        Point abilityDragPoint();

        String abilityDragName();

        void addAbilityDragZone(Rectangle bounds, String abilityName, int sourceSlot);

        void addAbilityDropZone(Rectangle bounds, int slot, boolean remove);

        void addPartyPortraitZone(Rectangle bounds, Actor actor);

        void addButton(Rectangle bounds, String label, Runnable action);

        void addTooltip(Rectangle bounds, String title, String body);

        void addTooltip(Rectangle bounds, String title, String body, String icon);

        void addTooltip(Rectangle bounds, String title, String body, String icon, Color accent);

        void repaintPanel();

        public void drawOverlayBase(Graphics2D g, int x, int y, int w, int h);

        void actionButton(Graphics2D g, int x, int y, int w, int h, String label, Runnable action, Color fill, Color border, boolean enabled);

        public void drawClippedString(Graphics2D g, String text, int x, int y, int maxWidth);

        public void drawCenteredIn(Graphics2D g, String text, int x, int y, int width);

        public void drawScrollIndicator(Graphics2D g, int x, int y, int h, int totalRows, int scroll, int visibleRows);

        String abilityIconName(Ability ability);

        String scalingLabel(Ability ability);

        String abilityStatusLabel(AbilityStatus status);

        String abilityStatusText(List<AbilityStatus> statuses);

        String abilityMechanicsText(Ability ability);

        String abilityFlavor(Ability ability);

        String dialoguePortraitSprite(Actor actor);
    }
}
