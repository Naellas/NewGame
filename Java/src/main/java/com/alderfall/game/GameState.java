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
    public GameMode mode = GameMode.MAIN_MENU;
    public GameMode pauseReturnMode = GameMode.EXPLORE;
    public GameMode settingsReturnMode = GameMode.MAIN_MENU;
    public GameMode saveMenuReturnMode = GameMode.MAIN_MENU;
    public boolean saveMenuCanSave;
    public Actor player = GameData.createPlayer("Knight");
    public String pendingPlayerName = "Arin";
    public String currentSaveId = "";
    public String currentMapId = WorldMap.OVERWORLD_ID;
    public int playerX = WorldMap.START_POSITION.x();
    public int playerY = WorldMap.START_POSITION.y();
    public Battle battle;
    public Npc activeNpc;
    public Shop activeShop;
    public int dialogIndex;
    public int zoom = 100;
    public int worldTick;
    public String status = "Choose New Adventure or load an existing save.";
    private final Map<Npc, NpcRuntime> npcRuntime = new LinkedHashMap<>();

    public GameState(GameConfig config) {
        this.config = config;
        this.world = new WorldMap(0);
        for (Map.Entry<String, Quest> entry : GameData.QUESTS.entrySet()) {
            quests.put(entry.getKey(), entry.getValue().copy());
        }
    }

    public void openMainMenu() {
        activeNpc = null;
        activeShop = null;
        battle = null;
        dialogIndex = 0;
        mode = GameMode.MAIN_MENU;
        status = "Choose New Adventure or load an existing save.";
    }

    public void openClassSelect() {
        activeNpc = null;
        activeShop = null;
        battle = null;
        dialogIndex = 0;
        mode = GameMode.CLASS_SELECT;
        status = "Choose a class.";
    }

    public void setPendingPlayerName(String name) {
        pendingPlayerName = cleanName(name);
    }

    public void chooseClass(String className) {
        setPendingPlayerName(pendingPlayerName);
        if (pendingPlayerName.isBlank()) {
            pendingPlayerName = "Arin";
        }
        player = GameData.createPlayer(className, pendingPlayerName);
        syncSkillAbilities();
        currentSaveId = SaveSystem.saveIdFor(player.name);
        currentMapId = WorldMap.OVERWORLD_ID;
        playerX = WorldMap.START_POSITION.x();
        playerY = WorldMap.START_POSITION.y();
        worldTick = 0;
        resetNpcRuntime();
        allies.clear();
        recruitedIds.clear();
        battle = null;
        mode = GameMode.EXPLORE;
        status = className + " begins in Oakhaven.";
    }

    public void openPauseMenu() {
        if (mode == GameMode.MAIN_MENU || mode == GameMode.CLASS_SELECT || mode == GameMode.PAUSE_MENU) {
            return;
        }
        pauseReturnMode = mode;
        mode = GameMode.PAUSE_MENU;
        status = "Paused.";
    }

    public void resumeGame() {
        mode = pauseReturnMode == GameMode.PAUSE_MENU ? GameMode.EXPLORE : pauseReturnMode;
        if (mode == GameMode.BATTLE) {
            status = "Battle!";
        } else if (mode == GameMode.DIALOG && activeNpc != null) {
            status = "Talking to " + activeNpc.name() + ".";
        } else {
            status = world.describe(currentMapId, playerX, playerY);
        }
    }

    public void openSettings() {
        settingsReturnMode = mode == GameMode.SETTINGS ? GameMode.MAIN_MENU : mode;
        mode = GameMode.SETTINGS;
    }

    public void closeSettings() {
        mode = settingsReturnMode == GameMode.SETTINGS ? GameMode.MAIN_MENU : settingsReturnMode;
    }

    public void openSaveMenu(boolean canSave) {
        saveMenuReturnMode = mode == GameMode.SAVE_MENU ? GameMode.MAIN_MENU : mode;
        saveMenuCanSave = canSave;
        mode = GameMode.SAVE_MENU;
    }

    public void closeSaveMenu() {
        mode = saveMenuReturnMode == GameMode.SAVE_MENU ? GameMode.MAIN_MENU : saveMenuReturnMode;
    }

    public void adjustChanceSetting(String key, double delta) {
        config.adjustChance(key, delta);
        status = "Settings updated.";
    }

    public void adjustVolumeSetting(String key, int delta) {
        config.adjustVolume(key, delta);
        status = "Audio settings updated.";
    }

    public Npc npcAtPlayer() {
        for (Npc npc : GameData.NPCS) {
            TilePoint position = npcPosition(npc);
            if (npc.mapId().equals(currentMapId) && Math.abs(position.x() - playerX) + Math.abs(position.y() - playerY) <= 1) {
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
            if (player.hp >= player.maxHp && player.mp >= player.maxMp) {
                status = "You are already refreshed.";
                return;
            }
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

    public boolean move(int dx, int dy) {
        if (mode != GameMode.EXPLORE) {
            return false;
        }
        int nx = playerX + dx;
        int ny = playerY + dy;
        if (!world.isPassable(currentMapId, nx, ny)) {
            status = "Blocked by " + Terrain.name(world.tileAt(currentMapId, nx, ny)) + ".";
            return false;
        }
        Npc npc = npcAt(currentMapId, nx, ny);
        if (npc != null) {
            status = npc.name() + " is there. Press E to talk.";
            return false;
        }
        playerX = nx;
        playerY = ny;
        WorldTransition transition = world.transitionAt(currentMapId, playerX, playerY);
        if (transition != null) {
            applyTransition(transition);
            return true;
        }
        status = world.describe(currentMapId, playerX, playerY);
        maybeStartEncounter();
        return true;
    }

    public void tickWorld() {
        worldTick++;
        if (mode != GameMode.EXPLORE) {
            return;
        }
        updateNpcMovement();
    }

    public void resetNpcRuntime() {
        npcRuntime.clear();
    }

    public TilePoint npcPosition(Npc npc) {
        NpcRuntime runtime = runtimeFor(npc);
        return new TilePoint(runtime.x, runtime.y);
    }

    public List<NpcMotion> npcMotionsForMap(String mapId) {
        List<NpcMotion> motions = new ArrayList<>();
        for (Npc npc : GameData.NPCS) {
            if (!npc.mapId().equals(mapId)) {
                continue;
            }
            NpcRuntime runtime = runtimeFor(npc);
            motions.add(new NpcMotion(npc, runtime.x, runtime.y, runtime.fromX, runtime.fromY, runtime.moveStartTick, runtime.facingDx, runtime.facingDy));
        }
        return motions;
    }

    public Npc npcAt(String mapId, int x, int y) {
        for (Npc npc : GameData.NPCS) {
            if (!npc.mapId().equals(mapId)) {
                continue;
            }
            TilePoint position = npcPosition(npc);
            if (position.x() == x && position.y() == y) {
                return npc;
            }
        }
        return null;
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
        List<GameData.MonsterSpec> specs = chooseMonsterGroup(tile).stream()
                .map(key -> GameData.MONSTERS.getOrDefault(key, GameData.MONSTERS.get("slime")))
                .toList();
        battle = new Battle(player, allies, specs, random, tile, kind);
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

    public void selectBattleEnemy(int index) {
        if (battle == null || battle.finished) {
            return;
        }
        battle.selectEnemy(index);
        Actor target = battle.selectedEnemy();
        status = target == null ? "No enemy target." : "Targeting " + target.name + ".";
    }

    public void selectBattlePartyMember(int index) {
        if (battle == null || battle.finished) {
            return;
        }
        battle.selectPartyMember(index);
        Actor target = battle.selectedPartyMember();
        status = target == null ? "No ally target." : "Ally target: " + target.name + ".";
    }

    public void cycleBattleEnemyTarget(int direction) {
        if (battle == null || battle.finished) {
            return;
        }
        battle.cycleEnemyTarget(direction);
        Actor target = battle.selectedEnemy();
        status = target == null ? "No enemy target." : "Targeting " + target.name + ".";
    }

    public void cycleBattlePartyTarget(int direction) {
        if (battle == null || battle.finished) {
            return;
        }
        battle.cyclePartyTarget(direction);
        Actor target = battle.selectedPartyMember();
        status = target == null ? "No ally target." : "Ally target: " + target.name + ".";
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
                for (String defeatedName : battle.defeatedMonsterNames()) {
                    recordQuestProgress(defeatedName);
                }
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

    private void updateNpcMovement() {
        for (Npc npc : GameData.NPCS) {
            if (!npc.mapId().equals(currentMapId)) {
                continue;
            }
            NpcRuntime runtime = runtimeFor(npc);
            if (worldTick - runtime.moveStartTick < NpcMotion.MOVE_TICKS || worldTick < runtime.nextThinkTick) {
                continue;
            }
            runtime.nextThinkTick = worldTick + 70 + random.nextInt(100);
            if (random.nextDouble() < 0.45) {
                continue;
            }
            int[][] directions = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
            int start = random.nextInt(directions.length);
            for (int i = 0; i < directions.length; i++) {
                int[] direction = directions[(start + i) % directions.length];
                int nx = runtime.x + direction[0];
                int ny = runtime.y + direction[1];
                if (!canNpcMoveTo(npc, runtime, nx, ny)) {
                    continue;
                }
                runtime.fromX = runtime.x;
                runtime.fromY = runtime.y;
                runtime.x = nx;
                runtime.y = ny;
                runtime.facingDx = direction[0];
                runtime.facingDy = direction[1];
                runtime.moveStartTick = worldTick;
                break;
            }
        }
    }

    private boolean canNpcMoveTo(Npc npc, NpcRuntime runtime, int x, int y) {
        if (!world.isPassable(npc.mapId(), x, y)) {
            return false;
        }
        if (Math.abs(x - runtime.homeX) + Math.abs(y - runtime.homeY) > 3) {
            return false;
        }
        if (npc.mapId().equals(currentMapId) && x == playerX && y == playerY) {
            return false;
        }
        for (Npc otherNpc : GameData.NPCS) {
            if (otherNpc.equals(npc) || !otherNpc.mapId().equals(npc.mapId())) {
                continue;
            }
            TilePoint otherPosition = npcPosition(otherNpc);
            if (otherPosition.x() == x && otherPosition.y() == y) {
                return false;
            }
        }
        return true;
    }

    private NpcRuntime runtimeFor(Npc npc) {
        return npcRuntime.computeIfAbsent(npc, ignored -> new NpcRuntime(npc.x(), npc.y(), Math.max(0, random.nextInt(90))));
    }

    private String cleanName(String name) {
        if (name == null) {
            return "";
        }
        String cleaned = String.join(" ", name.strip().split("\\s+"));
        return cleaned.substring(0, Math.min(24, cleaned.length()));
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

    private List<String> chooseMonsterGroup(char tile) {
        int count = 1;
        double roll = random.nextDouble();
        if (roll < config.threeMonsterChance && player.level >= 3) {
            count = 3;
        } else if (roll < config.threeMonsterChance + config.twoMonsterChance) {
            count = 2;
        }
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            keys.add(chooseMonster(tile));
        }
        return keys;
    }

    public record NpcMotion(Npc npc, int x, int y, int fromX, int fromY, int moveStartTick, int facingDx, int facingDy) {
        public static final int MOVE_TICKS = 14;
    }

    private static final class NpcRuntime {
        final int homeX;
        final int homeY;
        int x;
        int y;
        int fromX;
        int fromY;
        int moveStartTick = -NpcMotion.MOVE_TICKS;
        int facingDx;
        int facingDy = 1;
        int nextThinkTick;

        NpcRuntime(int x, int y, int nextThinkTick) {
            this.homeX = x;
            this.homeY = y;
            this.x = x;
            this.y = y;
            this.fromX = x;
            this.fromY = y;
            this.nextThinkTick = nextThinkTick;
        }
    }
}
