package com.alderfall.game;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public final class GameState {
    public final GameConfig config;
    public final WorldMap world;
    public final Random random = new Random();
    public final Map<String, Quest> quests = new LinkedHashMap<>();
    public final List<Actor> allies = new ArrayList<>();
    public final List<String> recruitedIds = new ArrayList<>();
    public GameMode mode = GameMode.CLASS_SELECT;
    public Actor player = GameData.createPlayer("Knight");
    public String currentMapId = WorldMap.OVERWORLD_ID;
    public int playerX = WorldMap.START_POSITION.x();
    public int playerY = WorldMap.START_POSITION.y();
    public Battle battle;
    public Npc activeNpc;
    public Shop activeShop;
    public int dialogIndex;
    public int zoom = 100;
    public String status = "Choose a class.";

    public GameState(GameConfig config) {
        this.config = config;
        this.world = new WorldMap(0);
        for (Map.Entry<String, Quest> entry : GameData.QUESTS.entrySet()) {
            quests.put(entry.getKey(), entry.getValue().copy());
        }
    }

    public void chooseClass(String className) {
        player = GameData.createPlayer(className);
        syncSkillAbilities();
        currentMapId = WorldMap.OVERWORLD_ID;
        playerX = WorldMap.START_POSITION.x();
        playerY = WorldMap.START_POSITION.y();
        allies.clear();
        recruitedIds.clear();
        battle = null;
        mode = GameMode.EXPLORE;
        status = className + " begins in Oakhaven.";
    }

    public Npc npcAtPlayer() {
        for (Npc npc : GameData.NPCS) {
            if (npc.mapId().equals(currentMapId) && Math.abs(npc.x() - playerX) + Math.abs(npc.y() - playerY) <= 1) {
                return npc;
            }
        }
        return null;
    }

    public void interact() {
        if (mode != GameMode.EXPLORE) {
            return;
        }
        WorldTransition transition = world.transitionAt(currentMapId, playerX, playerY);
        if (transition != null) {
            applyTransition(transition);
            return;
        }
        TilePoint buildingEntry = adjacentBuildingEntry();
        if (buildingEntry != null && ("city".equals(world.kind(currentMapId)) || "village".equals(world.kind(currentMapId)))) {
            String houseMapId = world.ensureHouseInterior(currentMapId, buildingEntry.x(), buildingEntry.y(), playerX, playerY);
            currentMapId = houseMapId;
            playerX = 11;
            playerY = 14;
            status = "You step inside.";
            return;
        }
        Npc npc = npcAtPlayer();
        if (npc == null) {
            status = "No one is close enough to talk.";
            return;
        }
        activeNpc = npc;
        activeShop = npc.shopId() == null ? null : GameData.SHOPS.get(npc.shopId());
        dialogIndex = 0;
        mode = GameMode.DIALOG;
        status = "Talking to " + npc.name() + ".";
    }

    public void advanceDialog() {
        if (mode != GameMode.DIALOG || activeNpc == null) {
            return;
        }
        if (dialogIndex < activeNpc.dialog().size() - 1) {
            dialogIndex++;
            return;
        }
        handleNpcQuest();
        if (activeShop != null) {
            mode = GameMode.SHOP;
            status = activeShop.name() + ". Press 1-" + activeShop.stock().size() + " to buy, Esc to leave.";
        } else {
            if (activeNpc.recruitId() != null && activeNpc.recruitCost() > 0 && !isRecruited(activeNpc.recruitId())) {
                status = activeNpc.name() + "'s companion contract costs " + activeNpc.recruitCost() + " gold. Press H to hire.";
                return;
            }
            closeOverlay();
        }
    }

    private void handleNpcQuest() {
        if (activeNpc.questId() == null) {
            return;
        }
        Quest quest = quests.get(activeNpc.questId());
        if (quest == null) {
            return;
        }
        if (!quest.accepted) {
            quest.accepted = true;
            status = "Quest accepted: " + quest.title + ".";
        } else if (quest.ready()) {
            quest.completed = true;
            player.gold += quest.rewardGold;
            List<String> notes = player.gainXp(quest.rewardXp);
            status = "Quest complete: " + quest.title + ".";
            String recruitNote = recruitAlly(activeNpc.recruitId());
            if (recruitNote != null) {
                status += " " + recruitNote;
            }
            if (!notes.isEmpty()) {
                status += " " + notes.get(notes.size() - 1);
            }
        } else if (!quest.completed) {
            status = quest.title + ": " + quest.progress + "/" + quest.needed + ".";
        } else {
            status = quest.title + " is already complete.";
        }
    }

    public void closeOverlay() {
        activeNpc = null;
        activeShop = null;
        dialogIndex = 0;
        mode = GameMode.EXPLORE;
    }

    public void toggleQuestLog() {
        if (mode == GameMode.QUEST_LOG) {
            closeOverlay();
        } else if (mode == GameMode.EXPLORE) {
            mode = GameMode.QUEST_LOG;
        }
    }

    public void toggleInventory() {
        if (mode == GameMode.INVENTORY) {
            closeOverlay();
        } else if (mode == GameMode.EXPLORE) {
            mode = GameMode.INVENTORY;
        }
    }

    public void toggleSkills() {
        if (mode == GameMode.SKILLS) {
            closeOverlay();
        } else if (mode == GameMode.EXPLORE) {
            mode = GameMode.SKILLS;
        }
    }

    public void toggleWorldMap() {
        if (mode == GameMode.WORLD_MAP) {
            closeOverlay();
        } else if (mode == GameMode.EXPLORE) {
            mode = GameMode.WORLD_MAP;
        }
    }

    public void setZoom(int zoom) {
        this.zoom = Math.max(70, Math.min(150, zoom));
    }

    public void adjustZoom(int delta) {
        setZoom(zoom + delta);
    }

    public void buyShopItem(int index) {
        if (mode != GameMode.SHOP || activeShop == null || index < 0 || index >= activeShop.stock().size()) {
            return;
        }
        String itemKey = activeShop.stock().get(index);
        int cost = GameData.itemCost(itemKey);
        if (cost <= 0) {
            return;
        }
        if (player.gold < cost) {
            status = "Not enough gold for " + GameData.itemName(itemKey) + ".";
            return;
        }
        player.gold -= cost;
        player.addItem(itemKey, 1);
        status = "Bought " + GameData.itemName(itemKey) + ".";
    }

    public boolean isRecruited(String recruitId) {
        return recruitId != null && recruitedIds.contains(recruitId);
    }

    public String recruitAlly(String recruitId) {
        if (recruitId == null || isRecruited(recruitId)) {
            return null;
        }
        GameData.RecruitSpec spec = GameData.RECRUITS.get(recruitId);
        if (spec == null) {
            return null;
        }
        Actor ally = spec.createActor();
        allies.add(ally);
        recruitedIds.add(recruitId);
        return ally.name + " joins your party.";
    }

    public void hireActiveRecruit() {
        if ((mode != GameMode.DIALOG && mode != GameMode.SHOP) || activeNpc == null) {
            return;
        }
        if (activeNpc.recruitId() == null || activeNpc.recruitCost() <= 0) {
            return;
        }
        if (isRecruited(activeNpc.recruitId())) {
            status = activeNpc.name() + " already travels with you.";
            return;
        }
        if (player.gold < activeNpc.recruitCost()) {
            status = activeNpc.name() + "'s contract costs " + activeNpc.recruitCost() + " gold.";
            return;
        }
        player.gold -= activeNpc.recruitCost();
        String recruitNote = recruitAlly(activeNpc.recruitId());
        if (recruitNote == null) {
            player.gold += activeNpc.recruitCost();
            status = activeNpc.name() + " cannot join right now.";
            return;
        }
        status = "Paid " + activeNpc.recruitCost() + " gold. " + recruitNote;
    }

    public void useInventoryItem(int index) {
        if (mode != GameMode.INVENTORY || index < 0 || index >= player.inventory.size()) {
            return;
        }
        String itemKey = player.inventory.keySet().stream().toList().get(index);
        useItem(itemKey);
    }

    public void useItem(String itemKey) {
        Equipment equipment = GameData.EQUIPMENT.get(itemKey);
        if (equipment != null) {
            status = player.equipItem(itemKey);
            return;
        }
        Item item = GameData.ITEMS.get(itemKey);
        if (item == null || !player.hasItem(itemKey)) {
            status = "No usable " + GameData.itemName(itemKey) + ".";
            return;
        }
        if (player.hp >= player.maxHp && player.mp >= player.maxMp) {
            status = "You are already refreshed.";
            return;
        }
        int heal = item.heal();
        int mp = item.mp();
        if (heal > 0) {
            heal += player.skillRank("merchant_sense") * 3;
            if (player.skillRank("battle_medic") > 0) {
                heal += 8;
            }
        }
        if (mp > 0) {
            mp += player.skillRank("merchant_sense") * 2;
        }
        if (battle != null && mode == GameMode.BATTLE && !battle.finished) {
            if (!battle.playerUseItem(item.name(), heal, mp)) {
                status = "Could not use " + item.name() + " right now.";
                return;
            }
            player.consumeItem(itemKey);
            updateAfterBattleAction();
        } else {
            player.consumeItem(itemKey);
            player.hp = Math.min(player.maxHp, player.hp + heal);
            player.mp = Math.min(player.maxMp, player.mp + mp);
        }
        status = "Used " + item.name() + ".";
    }

    public void unequipSlot(String slot) {
        status = player.unequipSlot(slot);
    }

    public boolean canAllocateSkill(String skillKey) {
        if (player.skillPoints <= 0) {
            status = "No skill points available.";
            return false;
        }
        SkillNode node = SkillTrees.availableSkillTree(player.className).get(skillKey);
        if (node == null) {
            status = "That skill belongs to a different class.";
            return false;
        }
        if (player.skillRank(skillKey) >= node.maxRank()) {
            status = node.name() + " is already mastered.";
            return false;
        }
        for (String required : node.requires()) {
            if (player.skillRank(required) <= 0) {
                SkillNode requiredNode = SkillTrees.SKILL_TREE.get(required);
                status = "Requires " + (requiredNode == null ? required : requiredNode.name()) + ".";
                return false;
            }
        }
        return true;
    }

    public void allocateSkill(String skillKey) {
        SkillNode node = SkillTrees.SKILL_TREE.get(skillKey);
        if (node == null || !canAllocateSkill(skillKey)) {
            return;
        }
        player.skillPoints--;
        player.skillAllocations.put(skillKey, player.skillRank(skillKey) + 1);
        applySkillEffects(node, 1);
        syncSkillAbilities();
        status = "Skill learned: " + node.name() + ".";
    }

    public void respecSkills() {
        int spent = SkillTrees.spent(player.skillAllocations);
        if (spent <= 0) {
            status = "No allocated skills to respec.";
            return;
        }
        int cost = SkillTrees.respecCost(player.level, spent);
        if (player.gold < cost) {
            status = "Respec costs " + cost + " gold.";
            return;
        }
        for (var entry : player.skillAllocations.entrySet()) {
            SkillNode node = SkillTrees.SKILL_TREE.get(entry.getKey());
            if (node != null) {
                for (int i = 0; i < entry.getValue(); i++) {
                    applySkillEffects(node, -1);
                }
            }
        }
        player.gold -= cost;
        player.skillPoints += spent;
        player.skillAllocations.clear();
        syncSkillAbilities();
        status = "Skills reset for " + cost + " gold.";
    }

    public void move(int dx, int dy) {
        if (mode != GameMode.EXPLORE) {
            return;
        }
        int nx = playerX + dx;
        int ny = playerY + dy;
        if (!world.isPassable(currentMapId, nx, ny)) {
            status = "Blocked by " + Terrain.name(world.tileAt(currentMapId, nx, ny)) + ".";
            return;
        }
        playerX = nx;
        playerY = ny;
        WorldTransition transition = world.transitionAt(currentMapId, playerX, playerY);
        if (transition != null) {
            applyTransition(transition);
            return;
        }
        status = world.describe(currentMapId, playerX, playerY);
        maybeStartEncounter();
    }

    public void maybeStartEncounter() {
        char tile = world.tileAt(currentMapId, playerX, playerY);
        String kind = world.kind(currentMapId);
        if (tile == 'r' || tile == 'c' || tile == 'u' || "city".equals(kind) || "village".equals(kind) || "interior".equals(kind)) {
            return;
        }
        double chance = "dungeon".equals(kind) ? config.dungeonEncounterChance : tile == 'd' ? config.dungeonEntranceEncounterChance : config.wildEncounterChance;
        if (random.nextDouble() >= chance) {
            return;
        }
        String key = chooseMonster(tile);
        GameData.MonsterSpec spec = GameData.MONSTERS.getOrDefault(key, GameData.MONSTERS.get("slime"));
        battle = new Battle(player, allies, spec, random, tile, kind);
        mode = GameMode.BATTLE;
        status = "Battle!";
    }

    public void battleAttack() {
        if (battle == null) {
            return;
        }
        battle.playerAttack();
        updateAfterBattleAction();
    }

    public void battleAbility(int index) {
        if (battle == null) {
            return;
        }
        battle.useAbility(index);
        updateAfterBattleAction();
    }

    public void revive() {
        player.healFull();
        for (Actor ally : allies) {
            ally.healFull();
        }
        currentMapId = WorldMap.OVERWORLD_ID;
        playerX = WorldMap.START_POSITION.x();
        playerY = WorldMap.START_POSITION.y();
        battle = null;
        mode = GameMode.EXPLORE;
        status = "Revived at Oakhaven.";
    }

    public void leaveFinishedBattle() {
        if (battle == null || !battle.finished || !battle.victory) {
            return;
        }
        battle = null;
        mode = GameMode.EXPLORE;
        status = world.describe(currentMapId, playerX, playerY);
    }

    public void applyTransition(WorldTransition transition) {
        currentMapId = transition.targetMapId();
        playerX = transition.targetX();
        playerY = transition.targetY();
        status = transition.message();
    }

    private TilePoint adjacentBuildingEntry() {
        if ("city".equals(world.kind(currentMapId))) {
            CityBuilding building = world.cityBuildingEntryAt(currentMapId, playerX, playerY - 1, playerX, playerY);
            if (building != null) {
                return new TilePoint(playerX, playerY - 1);
            }
            return null;
        }
        int[][] directions = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        for (int[] direction : directions) {
            int x = playerX + direction[0];
            int y = playerY + direction[1];
            char tile = world.tileAt(currentMapId, x, y);
            if (tile == 'h') {
                return new TilePoint(x, y);
            }
        }
        return null;
    }

    private void updateAfterBattleAction() {
        if (battle != null && battle.finished && battle.victory) {
            if (!battle.questRecorded) {
                recordQuestProgress(battle.enemy.name);
                battle.questRecorded = true;
            }
            status = "Victory. Press Enter to continue.";
        } else if (battle != null && battle.finished) {
            status = "Defeated. Press R to revive.";
        } else if (battle != null) {
            Actor actor = battle.activeActor();
            status = actor == null ? "Battle!" : actor.name + "'s turn.";
        }
    }

    public void syncSkillAbilities() {
        player.abilities.removeIf(ability -> SkillTrees.SKILL_ABILITY_NAMES.contains(ability.name()));
        Map<String, SkillNode> availableTree = SkillTrees.availableSkillTree(player.className);
        for (SkillNode node : availableTree.values()) {
            Ability ability = node.ability();
            if (ability != null && player.skillRank(node.id()) > 0 && !player.hasAbility(ability.name())) {
                player.abilities.add(ability);
            }
        }
    }

    private void applySkillEffects(SkillNode node, int direction) {
        double hpRatio = player.hp / (double) Math.max(1, player.maxHp);
        double mpRatio = player.maxMp <= 0 ? 0.0 : player.mp / (double) player.maxMp;
        for (var entry : node.effects().entrySet()) {
            int delta = entry.getValue() * direction;
            switch (entry.getKey()) {
                case "max_hp" -> player.maxHp = Math.max(1, player.maxHp + delta);
                case "max_mp" -> player.maxMp = Math.max(0, player.maxMp + delta);
                case "attack" -> player.attack = Math.max(1, player.attack + delta);
                case "defense" -> player.defense = Math.max(0, player.defense + delta);
                default -> {
                }
            }
        }
        player.hp = Math.max(1, Math.min(player.maxHp, (int) Math.round(player.maxHp * hpRatio)));
        player.mp = Math.max(0, Math.min(player.maxMp, (int) Math.round(player.maxMp * mpRatio)));
    }

    private void recordQuestProgress(String defeatedTarget) {
        for (Quest quest : quests.values()) {
            int oldProgress = quest.progress;
            quest.record(defeatedTarget);
            if (quest.progress > oldProgress) {
                status = quest.title + ": " + quest.progress + "/" + quest.needed + ".";
            }
        }
    }

    private String chooseMonster(char tile) {
        List<String> early = switch (tile) {
            case 'f' -> List.of("wolf", "goblin", "spider", "thornling");
            case 's' -> List.of("slime", "goblin", "orc", "sand_stalker");
            case 'n' -> List.of("wolf", "bat", "ice_golem");
            case 'v' -> List.of("slime", "spider", "bog_beast");
            case 'b' -> List.of("goblin", "skeleton", "wraith", "ember_imp");
            case 'q', 'm' -> List.of("wolf", "bat", "ice_golem");
            case 'd' -> List.of("bat", "skeleton", "wraith", "orc");
            default -> List.of("slime", "wolf", "thornling");
        };
        int cap = Math.min(early.size(), player.level < 4 ? 2 : player.level < 7 ? 3 : early.size());
        return early.get(random.nextInt(cap));
    }
}
