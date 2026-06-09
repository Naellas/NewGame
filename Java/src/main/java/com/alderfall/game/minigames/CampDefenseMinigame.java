package com.alderfall.game;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class CampDefenseMinigame {
    public static final int STARTING_LOADOUT_ABILITIES = 3;
    public static final int MAX_LOADOUT_ABILITIES = 8;
    private static final int MAX_WAVES = 5;
    private static final int WAVE_INTERVAL_TICKS = 170;
    private static final double PLAYER_SPEED = 0.19;
    private static final int PULSE_LIFETIME = 14;
    private static final List<String> RAID_MONSTERS = List.of(
            "goblin", "goblin_scout", "goblin_archer", "goblin_trapper",
            "bandit_cutthroat", "bandit_archer", "wolf", "orc_raider"
    );
    private static final List<String> BRUTE_MONSTERS = List.of(
            "hobgoblin_guard", "orc", "orc_shieldbearer", "bandit_captain", "goblin_warlord"
    );

    private final String mapId;
    private final List<Ability> availableAbilities;
    private final List<Ability> selectedAbilities = new ArrayList<>();
    private final List<Ability> pendingLevelChoices = new ArrayList<>();
    private final List<RaidEnemy> enemies = new ArrayList<>();
    private final List<AttackPulse> attackPulses = new ArrayList<>();
    private final int[] abilityCooldowns = new int[MAX_LOADOUT_ABILITIES];
    private double playerX;
    private double playerY;
    private int tick;
    private int nextEnemyId;
    private int wave;
    private int nextWaveTick = 1;
    private int campIntegrity = 100;
    private int guardTicks;
    private int raidLevel = 1;
    private int raidXp;
    private int raidXpToNext = 4;
    private int kills;
    private int escaped;
    private boolean started;
    private boolean finished;
    private boolean victory;
    private boolean resolved;
    private String resultText = "";

    public CampDefenseMinigame(GameState state) {
        this.mapId = state.currentMapId;
        this.playerX = state.playerX + 0.5;
        this.playerY = state.playerY + 0.5;
        List<Ability> learned = new ArrayList<>(state.player.activeAbilities());
        for (Ability ability : state.player.abilities) {
            if (learned.stream().noneMatch(existing -> existing.name().equals(ability.name()))) {
                learned.add(ability);
            }
        }
        this.availableAbilities = List.copyOf(learned);
        for (int i = 0; i < Math.min(STARTING_LOADOUT_ABILITIES, learned.size()); i++) {
            selectedAbilities.add(learned.get(i));
        }
    }

    public void tick(GameState state) {
        syncTileAnchor(state);
        if (!started || finished) {
            tickAttackPulses();
            return;
        }
        if (!pendingLevelChoices.isEmpty()) {
            tickAttackPulses();
            return;
        }
        tick++;
        if (guardTicks > 0) {
            guardTicks--;
        }
        if (wave < MAX_WAVES && tick >= nextWaveTick) {
            spawnWave(state);
            nextWaveTick = tick + WAVE_INTERVAL_TICKS;
        }
        moveEnemies(state);
        tickAbilities(state);
        tickAttackPulses();
        if (campIntegrity <= 0) {
            finish(false, "The raiders overran the settlement stores.");
        } else if (wave >= MAX_WAVES && enemies.isEmpty()) {
            finish(true, "The last raider breaks and runs.");
        }
    }

    public void begin(GameState state) {
        if (started) {
            return;
        }
        if (selectedAbilities.isEmpty() && !availableAbilities.isEmpty()) {
            selectedAbilities.add(availableAbilities.get(0));
        }
        started = true;
        state.status = selectedAbilities.isEmpty()
                ? "Raid started. No learned abilities are slotted, so the settlement must hold on raw steel."
                : "Raid started. Your chosen techniques begin cycling automatically.";
    }

    public boolean toggleAbility(int index) {
        if (started || index < 0 || index >= availableAbilities.size()) {
            return false;
        }
        Ability ability = availableAbilities.get(index);
        for (Iterator<Ability> iterator = selectedAbilities.iterator(); iterator.hasNext(); ) {
            if (iterator.next().name().equals(ability.name())) {
                iterator.remove();
                return true;
            }
        }
        if (selectedAbilities.size() >= STARTING_LOADOUT_ABILITIES) {
            selectedAbilities.remove(0);
        }
        selectedAbilities.add(ability);
        return true;
    }

    public boolean chooseLevelAbility(int index) {
        if (!started || finished || index < 0 || index >= pendingLevelChoices.size()) {
            return false;
        }
        Ability ability = pendingLevelChoices.get(index);
        pendingLevelChoices.clear();
        if (selectedAbilities.stream().noneMatch(existing -> existing.name().equals(ability.name()))) {
            if (selectedAbilities.size() >= MAX_LOADOUT_ABILITIES) {
                selectedAbilities.remove(0);
            }
            if (selectedAbilities.size() < MAX_LOADOUT_ABILITIES) {
                selectedAbilities.add(ability);
            }
        } else {
            refreshMatchingAbilityCooldown(ability);
        }
        return true;
    }

    public void movePlayer(GameState state, double dx, double dy) {
        if (!started || finished) {
            return;
        }
        double length = Math.hypot(dx, dy);
        if (length <= 0.001) {
            return;
        }
        double stepX = dx / length * PLAYER_SPEED;
        double stepY = dy / length * PLAYER_SPEED;
        double nextX = playerX + stepX;
        double nextY = playerY + stepY;
        if (playerPassable(state, nextX, nextY)) {
            playerX = nextX;
            playerY = nextY;
        } else {
            if (playerPassable(state, playerX + stepX, playerY)) {
                playerX += stepX;
            }
            if (playerPassable(state, playerX, playerY + stepY)) {
                playerY += stepY;
            }
        }
        syncTileAnchor(state);
    }

    private boolean playerPassable(GameState state, double x, double y) {
        int tx = (int) Math.floor(x);
        int ty = (int) Math.floor(y);
        return state.world.isPassable(mapId, tx, ty);
    }

    private void syncTileAnchor(GameState state) {
        state.playerX = Math.max(0, Math.min(state.world.width(mapId) - 1, (int) Math.floor(playerX)));
        state.playerY = Math.max(0, Math.min(state.world.height(mapId) - 1, (int) Math.floor(playerY)));
    }

    private void spawnWave(GameState state) {
        wave++;
        int stage = Math.max(0, state.world.playerVillageStage());
        int count = 4 + wave * 2 + Math.min(5, stage + state.villageAllies.size() / 2);
        for (int i = 0; i < count; i++) {
            boolean brute = wave >= 4 && i % 5 == 0;
            enemies.add(spawnEnemy(state, brute));
        }
        state.status = "Raid wave " + wave + " is spilling into " + state.world.label(mapId) + ".";
    }

    private RaidEnemy spawnEnemy(GameState state, boolean brute) {
        Random random = state.random;
        int width = state.world.width(mapId);
        int height = state.world.height(mapId);
        GameData.MonsterSpec spec = monsterSpec(random, brute);
        for (int attempt = 0; attempt < 80; attempt++) {
            int side = random.nextInt(4);
            int x = switch (side) {
                case 0 -> 1;
                case 1 -> width - 2;
                default -> 1 + random.nextInt(Math.max(1, width - 2));
            };
            int y = switch (side) {
                case 2 -> 1;
                case 3 -> height - 2;
                default -> 1 + random.nextInt(Math.max(1, height - 2));
            };
            if (state.world.isPassable(mapId, x, y)) {
                return enemyFromSpec(spec, x + 0.5, y + 0.5, brute);
            }
        }
        return enemyFromSpec(spec,
                Math.max(1, Math.min(width - 2, state.playerX + 6)) + 0.5,
                Math.max(1, Math.min(height - 2, state.playerY)) + 0.5,
                brute);
    }

    private GameData.MonsterSpec monsterSpec(Random random, boolean brute) {
        List<String> keys = brute ? BRUTE_MONSTERS : RAID_MONSTERS;
        String key = keys.get(random.nextInt(keys.size()));
        GameData.MonsterSpec spec = GameData.MONSTERS.get(key);
        return spec == null ? GameData.MONSTERS.get("goblin") : spec;
    }

    private RaidEnemy enemyFromSpec(GameData.MonsterSpec spec, double x, double y, boolean brute) {
        int hp = Math.max(10, spec.hp() / 2 + wave * (brute ? 7 : 4));
        double speed = brute ? 0.034 : spec.species().equals("beast") ? 0.066 : 0.052;
        return new RaidEnemy(
                nextEnemyId++,
                spec,
                x,
                y,
                hp,
                speed,
                Math.max(2, spec.attack() / (brute ? 2 : 3)),
                brute ? 0.48 : 0.36
        );
    }

    private void moveEnemies(GameState state) {
        for (RaidEnemy enemy : enemies) {
            double dx = playerX - enemy.x;
            double dy = playerY - enemy.y;
            double distance = Math.max(0.001, Math.hypot(dx, dy));
            if (distance <= enemy.radius + 0.52) {
                enemy.attackCooldown--;
                if (enemy.attackCooldown <= 0) {
                    int damage = guardTicks > 0 ? Math.max(1, enemy.damage / 2) : enemy.damage;
                    campIntegrity = Math.max(0, campIntegrity - damage);
                    enemy.attackCooldown = enemy.brute() ? 22 : 16;
                }
                continue;
            }
            double nx = enemy.x + dx / distance * enemy.speed;
            double ny = enemy.y + dy / distance * enemy.speed;
            if (enemyPassable(state, nx, ny)) {
                enemy.x = nx;
                enemy.y = ny;
            } else if (Math.abs(dx) > Math.abs(dy) && enemyPassable(state, enemy.x + Math.signum(dx) * enemy.speed, enemy.y)) {
                enemy.x += Math.signum(dx) * enemy.speed;
            } else if (enemyPassable(state, enemy.x, enemy.y + Math.signum(dy) * enemy.speed)) {
                enemy.y += Math.signum(dy) * enemy.speed;
            }
        }
    }

    private boolean enemyPassable(GameState state, double x, double y) {
        int tx = (int) Math.floor(x);
        int ty = (int) Math.floor(y);
        return state.world.isPassable(mapId, tx, ty);
    }

    private void tickAbilities(GameState state) {
        for (int i = 0; i < selectedAbilities.size(); i++) {
            if (abilityCooldowns[i] > 0) {
                abilityCooldowns[i]--;
                continue;
            }
            Ability ability = selectedAbilities.get(i);
            if (castAbility(state, ability)) {
                abilityCooldowns[i] = cooldownTicks(ability);
            }
        }
    }

    private boolean castAbility(GameState state, Ability ability) {
        if (ability.kind() == Ability.AbilityKind.HEAL) {
            int amount = Math.max(4, ability.power() / 3 + state.player.abilityScalingBonus(ability) / 2);
            campIntegrity = Math.min(100, campIntegrity + amount);
            attackPulses.add(AttackPulse.nova(playerX, playerY, 1.4, abilityColor(ability), effectKeyFor(ability), PULSE_LIFETIME));
            return true;
        }
        if (ability.kind() == Ability.AbilityKind.DEFEND) {
            guardTicks = Math.max(guardTicks, 75 + ability.power());
            attackPulses.add(AttackPulse.nova(playerX, playerY, 1.8, abilityColor(ability), effectKeyFor(ability), PULSE_LIFETIME + 8));
            return true;
        }
        if (enemies.isEmpty()) {
            return false;
        }
        int hits = hitCount(ability);
        boolean aoe = isAoe(ability);
        RaidEnemy first = nearestEnemy(playerX, playerY, abilityRange(ability));
        if (first == null) {
            return false;
        }
        if (aoe) {
            double radius = 1.35 + Math.min(1.15, ability.power() / 70.0);
            int damage = abilityDamage(state, ability, first, 0.76);
            for (RaidEnemy enemy : new ArrayList<>(enemies)) {
                if (Math.hypot(enemy.x - first.x, enemy.y - first.y) <= radius) {
                    damageEnemy(enemy, damage, ability);
                }
            }
            attackPulses.add(AttackPulse.nova(first.x, first.y, radius, abilityColor(ability), effectKeyFor(ability), PULSE_LIFETIME + 5));
            return true;
        }
        List<RaidEnemy> targets = nearestEnemies(playerX, playerY, abilityRange(ability), hits);
        for (int i = 0; i < targets.size(); i++) {
            RaidEnemy target = targets.get(i);
            int damage = abilityDamage(state, ability, target, i == 0 ? 1.0 : 0.72);
            damageEnemy(target, damage, ability);
            attackPulses.add(AttackPulse.beam(playerX, playerY, target.x, target.y, abilityColor(ability), effectKeyFor(ability), PULSE_LIFETIME));
        }
        return !targets.isEmpty();
    }

    private int cooldownTicks(Ability ability) {
        int base = 22 + ability.cost() * 2 + ability.cooldown() * 18;
        if (ability.kind() == Ability.AbilityKind.HEAL || ability.kind() == Ability.AbilityKind.DEFEND) {
            base += 28;
        }
        if (isAoe(ability)) {
            base += 18;
        }
        return Math.max(18, base);
    }

    private double abilityRange(Ability ability) {
        String lowered = ability.name().toLowerCase();
        if (lowered.contains("bow") || lowered.contains("shot") || lowered.contains("arrow") || lowered.contains("lance")) {
            return 6.8;
        }
        if (isAoe(ability)) {
            return 5.5;
        }
        return 4.9;
    }

    private boolean isAoe(Ability ability) {
        return ability.target().contains("all")
                || ability.visualResolution() == Ability.VisualResolution.AOE
                || ability.name().toLowerCase().contains("sweep")
                || ability.name().toLowerCase().contains("storm")
                || ability.name().toLowerCase().contains("nova");
    }

    private int hitCount(Ability ability) {
        if (ability.visualResolution() == Ability.VisualResolution.MULTI_PROJECTILE) {
            return 3;
        }
        if (ability.visualResolution() == Ability.VisualResolution.CHAIN) {
            return 4;
        }
        return 1;
    }

    private int abilityDamage(GameState state, Ability ability, RaidEnemy enemy, double multiplier) {
        int raw = Math.max(4, ability.power() / 2 + state.player.abilityDamageBonus(ability) + state.player.attack / 2);
        double typed = raw * GameData.monsterDamageMultiplier(enemy.spec, ability.damageTypes());
        return Math.max(1, (int) Math.round((typed - enemy.spec.defense() / 2.0) * multiplier));
    }

    private void damageEnemy(RaidEnemy enemy, int damage, Ability ability) {
        enemy.hp -= damage;
        for (AbilityStatus status : ability.statusHints()) {
            if ("weak".equals(status.key()) || "vulnerable".equals(status.key())) {
                enemy.hp -= Math.max(1, damage / 5);
            }
        }
        if (enemy.hp <= 0 && enemies.remove(enemy)) {
            kills++;
            addRaidXp(enemy.brute() ? 2 : 1);
        }
    }

    private RaidEnemy nearestEnemy(double x, double y, double range) {
        RaidEnemy best = null;
        double bestDistance = range;
        for (RaidEnemy enemy : enemies) {
            double distance = Math.hypot(enemy.x - x, enemy.y - y);
            if (distance <= bestDistance) {
                best = enemy;
                bestDistance = distance;
            }
        }
        return best;
    }

    private List<RaidEnemy> nearestEnemies(double x, double y, double range, int count) {
        List<RaidEnemy> sorted = new ArrayList<>(enemies);
        sorted.sort((a, b) -> Double.compare(Math.hypot(a.x - x, a.y - y), Math.hypot(b.x - x, b.y - y)));
        List<RaidEnemy> result = new ArrayList<>();
        for (RaidEnemy enemy : sorted) {
            if (result.size() >= count) {
                break;
            }
            if (Math.hypot(enemy.x - x, enemy.y - y) <= range) {
                result.add(enemy);
            }
        }
        return result;
    }

    private Color abilityColor(Ability ability) {
        if (ability.kind() == Ability.AbilityKind.HEAL) {
            return new Color(106, 223, 151);
        }
        if (ability.kind() == Ability.AbilityKind.DEFEND) {
            return new Color(145, 184, 246);
        }
        String type = ability.damageTypes().isEmpty() ? "" : ability.damageTypes().get(0);
        return switch (type) {
            case "fire" -> new Color(248, 116, 72);
            case "ice", "cold" -> new Color(135, 218, 255);
            case "lightning" -> new Color(244, 229, 92);
            case "nature" -> new Color(119, 217, 110);
            case "dark" -> new Color(171, 112, 231);
            case "radiant", "holy" -> new Color(255, 235, 155);
            default -> new Color(255, 196, 105);
        };
    }

    private String effectKeyFor(Ability ability) {
        if (ability.effectKey() != null && !ability.effectKey().isBlank()) {
            return ability.effectKey();
        }
        if (ability.kind() == Ability.AbilityKind.HEAL) {
            return "heal";
        }
        if (ability.kind() == Ability.AbilityKind.DEFEND) {
            return "shield";
        }
        String type = ability.damageTypes().isEmpty() ? "" : ability.damageTypes().get(0);
        return switch (type) {
            case "fire" -> "fire";
            case "ice", "cold" -> "frost";
            case "lightning" -> "lightning";
            case "nature" -> "nature";
            case "dark" -> "shadow";
            case "radiant", "holy" -> "radiant";
            case "poison" -> "poison";
            case "acid" -> "acid";
            default -> "strike";
        };
    }

    private void addRaidXp(int amount) {
        if (finished || !pendingLevelChoices.isEmpty() || availableAbilities.isEmpty()) {
            return;
        }
        raidXp += Math.max(1, amount);
        while (raidXp >= raidXpToNext && pendingLevelChoices.isEmpty()) {
            raidXp -= raidXpToNext;
            raidLevel++;
            raidXpToNext = Math.min(18, 4 + raidLevel * 2);
            populateLevelChoices();
        }
    }

    private void populateLevelChoices() {
        pendingLevelChoices.clear();
        for (Ability ability : availableAbilities) {
            if (selectedAbilities.stream().noneMatch(existing -> existing.name().equals(ability.name()))) {
                pendingLevelChoices.add(ability);
            }
            if (pendingLevelChoices.size() >= 3) {
                return;
            }
        }
        for (Ability ability : availableAbilities) {
            if (pendingLevelChoices.stream().noneMatch(existing -> existing.name().equals(ability.name()))) {
                pendingLevelChoices.add(ability);
            }
            if (pendingLevelChoices.size() >= 3) {
                return;
            }
        }
    }

    private void refreshMatchingAbilityCooldown(Ability ability) {
        for (int i = 0; i < selectedAbilities.size() && i < abilityCooldowns.length; i++) {
            if (selectedAbilities.get(i).name().equals(ability.name())) {
                abilityCooldowns[i] = Math.min(abilityCooldowns[i], Math.max(8, cooldownTicks(ability) / 2));
                return;
            }
        }
    }

    private void tickAttackPulses() {
        Iterator<AttackPulse> iterator = attackPulses.iterator();
        while (iterator.hasNext()) {
            AttackPulse pulse = iterator.next();
            pulse.age++;
            if (pulse.age >= pulse.lifetime) {
                iterator.remove();
            }
        }
    }

    private void finish(boolean won, String text) {
        finished = true;
        victory = won;
        resultText = text;
        if (!won) {
            escaped = enemies.size();
            enemies.clear();
        }
    }

    public String resolveRewards(GameState state) {
        if (!finished || resolved) {
            return "";
        }
        resolved = true;
        if (!victory) {
            return "Raid failed. The camp lost supplies, but the villagers survived.";
        }
        int gold = 12 + kills * 2 + wave * 6;
        int xp = 8 + kills + wave * 4;
        state.player.gold += gold;
        List<String> notes = state.player.gainXp(xp);
        String levelText = notes.isEmpty() ? "" : " " + notes.get(notes.size() - 1);
        return "Raid held: +" + gold + "g, +" + xp + " XP." + levelText;
    }

    public String mapId() {
        return mapId;
    }

    public List<Ability> availableAbilities() {
        return availableAbilities;
    }

    public List<Ability> selectedAbilities() {
        return List.copyOf(selectedAbilities);
    }

    public List<Ability> pendingLevelChoices() {
        return List.copyOf(pendingLevelChoices);
    }

    public int abilityCooldown(int index) {
        return index < 0 || index >= abilityCooldowns.length ? 0 : abilityCooldowns[index];
    }

    public boolean started() {
        return started;
    }

    public int raidLevel() {
        return raidLevel;
    }

    public int raidXp() {
        return raidXp;
    }

    public int raidXpToNext() {
        return raidXpToNext;
    }

    public double playerX() {
        return playerX;
    }

    public double playerY() {
        return playerY;
    }

    public List<RaidEnemy> enemies() {
        return enemies;
    }

    public List<AttackPulse> attackPulses() {
        return attackPulses;
    }

    public int wave() {
        return wave;
    }

    public int maxWaves() {
        return MAX_WAVES;
    }

    public int campIntegrity() {
        return campIntegrity;
    }

    public int guardTicks() {
        return guardTicks;
    }

    public int kills() {
        return kills;
    }

    public int escaped() {
        return escaped;
    }

    public int nextWaveCountdown() {
        return wave >= MAX_WAVES ? 0 : Math.max(0, nextWaveTick - tick);
    }

    public boolean finished() {
        return finished;
    }

    public boolean victory() {
        return victory;
    }

    public boolean resolved() {
        return resolved;
    }

    public String resultText() {
        return resultText;
    }

    public static final class RaidEnemy {
        private final int id;
        private final GameData.MonsterSpec spec;
        private final int maxHp;
        private final double speed;
        private final int damage;
        private final double radius;
        private double x;
        private double y;
        private int hp;
        private int attackCooldown;

        private RaidEnemy(int id, GameData.MonsterSpec spec, double x, double y, int hp, double speed, int damage, double radius) {
            this.id = id;
            this.spec = spec;
            this.x = x;
            this.y = y;
            this.hp = hp;
            this.maxHp = hp;
            this.speed = speed;
            this.damage = damage;
            this.radius = radius;
        }

        public int id() {
            return id;
        }

        public GameData.MonsterSpec spec() {
            return spec;
        }

        public boolean brute() {
            return maxHp >= 45 || spec.key().contains("captain") || spec.key().contains("guard") || spec.key().contains("warlord");
        }

        public double x() {
            return x;
        }

        public double y() {
            return y;
        }

        public int hp() {
            return hp;
        }

        public int maxHp() {
            return maxHp;
        }
    }

    public static final class AttackPulse {
        private final double fromX;
        private final double fromY;
        private final double toX;
        private final double toY;
        private final double radius;
        private final Color color;
        private final String effectKey;
        private final int lifetime;
        private int age;

        private AttackPulse(double fromX, double fromY, double toX, double toY, double radius, Color color, String effectKey, int lifetime) {
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
            this.radius = radius;
            this.color = color;
            this.effectKey = effectKey == null || effectKey.isBlank() ? "strike" : effectKey;
            this.lifetime = lifetime;
        }

        private static AttackPulse beam(double fromX, double fromY, double toX, double toY, Color color, String effectKey, int lifetime) {
            return new AttackPulse(fromX, fromY, toX, toY, 0.0, color, effectKey, lifetime);
        }

        private static AttackPulse nova(double x, double y, double radius, Color color, String effectKey, int lifetime) {
            return new AttackPulse(x, y, x, y, radius, color, effectKey, lifetime);
        }

        public double fromX() {
            return fromX;
        }

        public double fromY() {
            return fromY;
        }

        public double toX() {
            return toX;
        }

        public double toY() {
            return toY;
        }

        public double radius() {
            return radius;
        }

        public Color color() {
            return color;
        }

        public String effectKey() {
            return effectKey;
        }

        public double progress() {
            return Math.min(1.0, age / (double) Math.max(1, lifetime));
        }
    }
}
