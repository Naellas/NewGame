package com.alderfall.game;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

public final class Battle {
    private final Random random;
    private final Map<Actor, Map<String, EffectStack>> statuses = new IdentityHashMap<>();
    private final Map<Actor, Map<String, Integer>> monsterCooldowns = new IdentityHashMap<>();
    private final Map<Actor, Boolean> guards = new IdentityHashMap<>();
    private final List<Actor> partyMembers = new ArrayList<>();
    private final Map<Actor, GameData.MonsterSpec> enemySpecs = new IdentityHashMap<>();
    private final Map<Actor, Integer> enemyFades = new IdentityHashMap<>();
    private final Map<Actor, Integer> enemyLunges = new IdentityHashMap<>();
    private final List<Actor> defeatedEnemies = new ArrayList<>();
    private final List<String> defeatedMonsterNames = new ArrayList<>();
    private int activePartyIndex;
    private int selectedEnemyIndex;
    private int selectedPartyIndex;

    public final Actor player;
    public final Actor enemy;
    public final List<Actor> enemies = new ArrayList<>();
    public final GameData.MonsterSpec monsterSpec;
    public final List<GameData.MonsterSpec> monsterSpecs;
    public final Queue<String> log = new ArrayDeque<>();
    public final List<FloatingText> floaters = new ArrayList<>();
    public final String backdrop;
    public boolean finished;
    public boolean victory;
    public boolean questRecorded;
    public int shake;
    public int monsterOffset;
    public int playerOffset;
    public int playerLunge;
    public int monsterLunge;
    public int flashTimer;
    public int monsterFade = 255;
    public int effectTimer;
    public String effectKind = "";
    public Actor effectSource;
    public Actor effectTarget;

    public Battle(Actor player, GameData.MonsterSpec monsterSpec, Random random) {
        this(player, List.of(), monsterSpec, random, 'g', "overworld");
    }

    public Battle(Actor player, List<Actor> allies, GameData.MonsterSpec monsterSpec, Random random) {
        this(player, allies, monsterSpec, random, 'g', "overworld");
    }

    public Battle(Actor player, GameData.MonsterSpec monsterSpec, Random random, char terrain, String mapKind) {
        this(player, List.of(), monsterSpec, random, terrain, mapKind);
    }

    public Battle(Actor player, List<Actor> allies, GameData.MonsterSpec monsterSpec, Random random, char terrain, String mapKind) {
        this(player, allies, List.of(monsterSpec), random, terrain, mapKind);
    }

    public Battle(Actor player, List<Actor> allies, List<GameData.MonsterSpec> monsterSpecs, Random random, char terrain, String mapKind) {
        this.player = player;
        this.random = random;
        this.backdrop = chooseBackdrop(terrain, mapKind);
        List<GameData.MonsterSpec> specs = monsterSpecs == null || monsterSpecs.isEmpty()
                ? List.of(GameData.MONSTERS.get("slime"))
                : monsterSpecs.stream().filter(spec -> spec != null).toList();
        if (specs.isEmpty()) {
            specs = List.of(GameData.MONSTERS.get("slime"));
        }
        this.monsterSpecs = List.copyOf(specs);
        this.monsterSpec = specs.get(0);
        Actor firstEnemy = null;
        for (GameData.MonsterSpec spec : specs) {
            Actor foe = spec.createActor();
            if (firstEnemy == null) {
                firstEnemy = foe;
            }
            enemies.add(foe);
            enemySpecs.put(foe, spec);
            enemyFades.put(foe, 255);
            enemyLunges.put(foe, 0);
        }
        this.enemy = firstEnemy;
        partyMembers.add(player);
        for (Actor ally : allies) {
            if (ally != null && !partyMembers.contains(ally)) {
                partyMembers.add(ally);
            }
        }
        if (enemies.size() == 1) {
            addLog(this.monsterSpec.name() + " appears!");
        } else {
            addLog(enemies.size() + " enemies appear!");
        }
        beginActivePartyTurn();
    }

    public Actor activeActor() {
        if (finished || partyMembers.isEmpty()) {
            return null;
        }
        activePartyIndex = Math.max(0, Math.min(activePartyIndex, partyMembers.size() - 1));
        if (partyMembers.get(activePartyIndex).alive()) {
            return partyMembers.get(activePartyIndex);
        }
        for (int i = 0; i < partyMembers.size(); i++) {
            if (partyMembers.get(i).alive()) {
                activePartyIndex = i;
                return partyMembers.get(i);
            }
        }
        return null;
    }

    public List<Actor> livingParty() {
        return partyMembers.stream().filter(Actor::alive).toList();
    }

    public List<Actor> partyMembers() {
        return List.copyOf(partyMembers);
    }

    public List<Actor> livingEnemies() {
        return enemies.stream().filter(Actor::alive).toList();
    }

    public List<Actor> enemies() {
        return List.copyOf(enemies);
    }

    public GameData.MonsterSpec monsterSpecFor(Actor actor) {
        return enemySpecs.getOrDefault(actor, monsterSpec);
    }

    public List<String> defeatedMonsterNames() {
        return List.copyOf(defeatedMonsterNames);
    }

    public int enemyFade(Actor actor) {
        return enemyFades.getOrDefault(actor, 255);
    }

    public int enemyLunge(Actor actor) {
        return enemyLunges.getOrDefault(actor, 0);
    }

    public Actor selectedEnemy() {
        if (enemies.isEmpty()) {
            return null;
        }
        selectedEnemyIndex = clampSelectedIndex(selectedEnemyIndex, enemies);
        Actor selected = enemies.get(selectedEnemyIndex);
        if (selected.alive()) {
            return selected;
        }
        for (int i = 0; i < enemies.size(); i++) {
            if (enemies.get(i).alive()) {
                selectedEnemyIndex = i;
                return enemies.get(i);
            }
        }
        return null;
    }

    public Actor selectedPartyMember() {
        if (partyMembers.isEmpty()) {
            return null;
        }
        selectedPartyIndex = clampSelectedIndex(selectedPartyIndex, partyMembers);
        Actor selected = partyMembers.get(selectedPartyIndex);
        if (selected.alive()) {
            return selected;
        }
        for (int i = 0; i < partyMembers.size(); i++) {
            if (partyMembers.get(i).alive()) {
                selectedPartyIndex = i;
                return partyMembers.get(i);
            }
        }
        return null;
    }

    public void selectEnemy(int index) {
        if (index >= 0 && index < enemies.size() && enemies.get(index).alive()) {
            selectedEnemyIndex = index;
        }
    }

    public void selectPartyMember(int index) {
        if (index >= 0 && index < partyMembers.size() && partyMembers.get(index).alive()) {
            selectedPartyIndex = index;
        }
    }

    public void cycleEnemyTarget(int direction) {
        selectedEnemyIndex = cycleLivingIndex(enemies, selectedEnemyIndex, direction);
    }

    public void cyclePartyTarget(int direction) {
        selectedPartyIndex = cycleLivingIndex(partyMembers, selectedPartyIndex, direction);
    }

    public List<EffectStack> statusesFor(Actor actor) {
        Map<String, EffectStack> bucket = statuses.get(actor);
        if (bucket == null) {
            return List.of();
        }
        return bucket.values().stream()
                .sorted(Comparator.comparing(stack -> stack.effect.name()))
                .toList();
    }

    public boolean isGuarding(Actor actor) {
        return guards.containsKey(actor);
    }

    public void tick() {
        shake = Math.max(0, shake - 1);
        monsterOffset = shake * (shake % 2 == 0 ? 1 : -1);
        playerOffset = (shake / 2) * (shake % 2 == 0 ? -1 : 1);
        playerLunge = Math.max(0, playerLunge - 2);
        monsterLunge = Math.max(0, monsterLunge - 2);
        for (Actor foe : enemies) {
            enemyLunges.put(foe, Math.max(0, enemyLunges.getOrDefault(foe, 0) - 2));
        }
        flashTimer = Math.max(0, flashTimer - 1);
        effectTimer = Math.max(0, effectTimer - 1);
        for (Actor foe : enemies) {
            if (!foe.alive()) {
                int nextFade = Math.max(0, enemyFades.getOrDefault(foe, 255) - 16);
                enemyFades.put(foe, nextFade);
                if (foe == enemy) {
                    monsterFade = nextFade;
                }
            }
        }
        for (int i = floaters.size() - 1; i >= 0; i--) {
            FloatingText floater = floaters.get(i);
            floater.life--;
            floater.y--;
            if (floater.life <= 0) {
                floaters.remove(i);
            }
        }
    }

    public void playerAttack() {
        Actor actor = activeActor();
        if (!canPartyAct(actor)) {
            return;
        }
        Actor target = selectedEnemy();
        if (target == null) {
            checkFinished();
            return;
        }
        setGuard(actor, false);
        int raw = modifyOutgoingDamage(actor, actor.basicDamage(random));
        int damage = dealDamage(actor, target, raw);
        floaters.add(new FloatingText("-" + damage, floaterX(target), floaterY(target), 34, new Color(255, 221, 221)));
        addLog(actor == player ? "You hit " + target.name + " for " + damage + "." : actor.name + " hits " + target.name + " for " + damage + ".");
        shake = 10;
        playerLunge = 18;
        flashTimer = 4;
        playEffect("strike", actor, target, 12);
        if (!target.alive()) {
            addLog(target.name + " falls.");
            clearStatuses(target);
            rememberDefeated(target);
        }
        afterPartyAction(actor);
    }

    public void useAbility(int index) {
        Actor actor = activeActor();
        if (!canPartyAct(actor) || index < 0 || index >= actor.abilities.size()) {
            return;
        }
        Ability ability = actor.abilities.get(index);
        if (actor.mp < ability.cost()) {
            addLog(actor.name + " does not have enough MP.");
            return;
        }
        actor.mp -= ability.cost();
        switch (ability.kind()) {
            case HEAL -> useHealAbility(actor, ability);
            case DEFEND -> defend(actor, ability.name(), 4 + (actor == player ? player.skillRank("stalwart_guard") : 0));
            case DAMAGE -> useDamageAbility(actor, ability);
        }
        if (actor == player && player.skillRank("ether_flow") > 0) {
            player.mp = Math.min(player.maxMp, player.mp + player.skillRank("ether_flow"));
        }
        afterPartyAction(actor);
    }

    public boolean playerUseItem(Item item) {
        return item != null && playerUseItem(item.name(), item.heal(), item.mp());
    }

    public boolean playerUseItem(String itemName, int heal, int mp) {
        Actor actor = activeActor();
        if (!canPartyAct(actor) || (heal <= 0 && mp <= 0)) {
            return false;
        }
        Actor target = selectedPartyMember();
        if (target == null) {
            return false;
        }
        if (target.hp >= target.maxHp && target.mp >= target.maxMp) {
            addLog(target.name + " is already refreshed.");
            return false;
        }
        setGuard(actor, false);
        if (heal > 0) {
            int before = target.hp;
            target.hp = Math.min(target.maxHp, target.hp + heal);
            int healed = target.hp - before;
            if (healed > 0) {
                floaters.add(new FloatingText("+" + healed, floaterX(target), floaterY(target), 32, new Color(185, 247, 195)));
            }
        }
        if (mp > 0) {
            int before = target.mp;
            target.mp = Math.min(target.maxMp, target.mp + mp);
            int gained = target.mp - before;
            if (gained > 0) {
                floaters.add(new FloatingText("+" + gained + " MP", floaterX(target), floaterY(target), 32, new Color(189, 210, 255)));
            }
        }
        addLog(actor.name + " used " + itemName + " on " + target.name + ".");
        playEffect("item", actor, target, 14);
        afterPartyAction(actor);
        return true;
    }

    private boolean canPartyAct(Actor actor) {
        return !finished && actor != null && actor.alive() && !enemies.contains(actor);
    }

    private void useHealAbility(Actor actor, Ability ability) {
        setGuard(actor, false);
        Actor enemyTarget = selectedEnemy();
        int bonus = actor == player ? player.skillRank("channeling") * 4 : 0;
        int amount = Math.max(1, ability.power() + actor.level * 2 + bonus + random.nextInt(5) - 2);
        Actor target = selectedPartyMember();
        if (target == null) {
            target = lowestPartyMember();
        }
        int before = target.hp;
        target.hp = Math.min(target.maxHp, target.hp + amount);
        int healed = target.hp - before;
        applyAbilityStatuses(actor, ability, target, enemyTarget);
        floaters.add(new FloatingText("+" + healed, floaterX(target), floaterY(target), 34, new Color(185, 247, 195)));
        addLog(castMessage(actor, ability.name(), target.name + " recovers " + healed + "."));
        playEffect("heal", actor, target, 18);
    }

    private void useDamageAbility(Actor actor, Ability ability) {
        setGuard(actor, false);
        Actor target = selectedEnemy();
        if (target == null) {
            return;
        }
        int bonus = actor == player ? player.skillRank("spellcraft") * 2 : 0;
        int raw = ability.power() + bonus + actor.attack / 2 + random.nextInt(6);
        raw = modifyOutgoingDamage(actor, raw);
        int damage = dealDamage(actor, target, raw);
        floaters.add(new FloatingText("-" + damage, floaterX(target), floaterY(target), 34, new Color(255, 224, 166)));
        addLog(castMessage(actor, ability.name(), "deals " + damage + " to " + target.name + "."));
        shake = 14;
        playerLunge = 22;
        flashTimer = 5;
        playEffect(effectForAbility(ability.name()), actor, target, 18);
        applyAbilityStatuses(actor, ability, actor, target);
        if (!target.alive()) {
            addLog(target.name + " falls.");
            clearStatuses(target);
            rememberDefeated(target);
        }
    }

    private void defend(Actor actor, String label, int mpGain) {
        setGuard(actor, true);
        actor.mp = Math.min(actor.maxMp, actor.mp + mpGain);
        applyStatus(actor, "shield", null);
        applyStatus(actor, "fortified", null);
        floaters.add(new FloatingText("Guard", floaterX(actor), floaterY(actor), 32, new Color(214, 232, 255)));
        addLog(actor.name + " uses " + label + ": guard stance up, +" + mpGain + " MP.");
        playEffect("shield", actor, actor, 16);
    }

    private void afterPartyAction(Actor actor) {
        advanceStatusTurn(actor);
        if (checkFinished()) {
            return;
        }
        if (advanceToNextPartyActor()) {
            beginActivePartyTurn();
            return;
        }
        enemyTurn();
        if (!finished) {
            activePartyIndex = firstLivingPartyIndex();
            beginActivePartyTurn();
        }
    }

    private void enemyTurn() {
        if (finished || livingEnemies().isEmpty()) {
            return;
        }
        for (Actor foe : new ArrayList<>(livingEnemies())) {
            if (finished || !foe.alive()) {
                continue;
            }
            triggerTurnStatuses(foe);
            if (checkFinished() || !foe.alive()) {
                continue;
            }
            addLog(foe.name + "'s turn.");
            tickMonsterCooldowns(foe);
            MonsterAbility ability = chooseMonsterAbility(foe);
            if (ability != null) {
                useMonsterAbility(foe, ability);
            } else {
                Actor target = randomLivingPartyMember();
                int raw = guardReducedRaw(target, modifyOutgoingDamage(foe, foe.basicDamage(random)));
                int damage = applyGuardMastery(target, dealDamage(foe, target, raw));
                floaters.add(new FloatingText("-" + damage, floaterX(target), floaterY(target), 34, new Color(255, 187, 187)));
                addLog(foe.name + " strikes " + target.name + " for " + damage + ".");
                shake = Math.max(shake, 8);
                setEnemyLunge(foe, 20);
                playEffect("strike", foe, target, 12);
                if (!target.alive()) {
                    addLog(target.name + " falls.");
                    clearStatuses(target);
                }
            }
            advanceStatusTurn(foe);
            if (checkFinished()) {
                return;
            }
        }
    }

    private void useMonsterAbility(Actor foe, MonsterAbility ability) {
        cooldownsFor(foe).put(ability.name(), Math.max(1, ability.cooldown()));
        Actor target = ability.kind() == MonsterAbility.Kind.BUFF ? foe : randomLivingPartyMember();
        setEnemyLunge(foe, ability.kind() == MonsterAbility.Kind.DAMAGE ? 16 : 8);
        playEffect(ability.effect(), foe, target, 20);
        if (ability.kind() == MonsterAbility.Kind.BUFF) {
            boolean applied = applyMonsterAbilityStatuses(foe, ability, target);
            floaters.add(new FloatingText("Focus", floaterX(foe), floaterY(foe), 30, new Color(215, 230, 255)));
            addLog(foe.name + " uses " + ability.name() + "; " + (applied ? "power gathers." : "draws inward."));
            return;
        }

        int raw = randomBetween(Math.max(1, ability.power() - 3), ability.power() + 5);
        raw = guardReducedRaw(target, modifyOutgoingDamage(foe, raw));
        int damage = applyGuardMastery(target, dealDamage(foe, target, raw));
        floaters.add(new FloatingText("-" + damage, floaterX(target), floaterY(target), 34, new Color(255, 187, 187)));
        addLog(foe.name + " uses " + ability.name() + "; " + target.name + " takes " + damage + ".");
        shake = Math.max(shake, 10);
        flashTimer = Math.max(flashTimer, 2);
        applyMonsterAbilityStatuses(foe, ability, target);
        if (!target.alive()) {
            addLog(target.name + " falls.");
            clearStatuses(target);
        }
    }

    private MonsterAbility chooseMonsterAbility(Actor foe) {
        List<MonsterAbility> abilities = MonsterAbilities.forMonster(monsterSpecFor(foe));
        if (abilities.isEmpty()) {
            return null;
        }
        double hpRatio = foe.hp / (double) Math.max(1, foe.maxHp);
        Map<String, EffectStack> bucket = statusBucket(foe);
        Map<String, Integer> cooldowns = cooldownsFor(foe);
        for (MonsterAbility ability : abilities) {
            if (cooldowns.getOrDefault(ability.name(), 0) > 0) {
                continue;
            }
            if (ability.hpBelow() != null && hpRatio > ability.hpBelow()) {
                continue;
            }
            if (ability.kind() == MonsterAbility.Kind.BUFF) {
                boolean allSelfStatusesPresent = ability.statuses().stream()
                        .filter(status -> "self".equals(status.target()))
                        .map(MonsterAbilityStatus::key)
                        .allMatch(bucket::containsKey);
                if (allSelfStatusesPresent) {
                    continue;
                }
            }
            double chance = ability.chance();
            if (ability.hpBelow() != null && hpRatio <= ability.hpBelow()) {
                chance += 0.14;
            }
            if (random.nextDouble() < Math.min(0.92, chance)) {
                return ability;
            }
        }
        return null;
    }

    private boolean applyMonsterAbilityStatuses(Actor caster, MonsterAbility ability, Actor chosenTarget) {
        boolean applied = false;
        for (MonsterAbilityStatus status : ability.statuses()) {
            if (status.chance() < 1.0 && random.nextDouble() > status.chance()) {
                continue;
            }
            Actor target = "self".equals(status.target()) ? caster : chosenTarget;
            if (applyStatus(target, status.key(), status.power())) {
                applied = true;
                addLog(target.name + " is affected by " + statusName(status.key()) + ".");
            }
        }
        return applied;
    }

    private void applyAbilityStatuses(Actor caster, Ability ability, Actor alliedTarget, Actor enemyTarget) {
        for (AbilityStatus hint : GameData.statusHintsForAbility(ability.name(), ability.kind())) {
            if (hint.chance() < 1.0 && random.nextDouble() > hint.chance()) {
                continue;
            }
            Actor target = switch (hint.target()) {
                case "self" -> caster;
                case "target", "ally" -> ability.kind() == Ability.AbilityKind.HEAL ? alliedTarget : enemyTarget;
                default -> enemyTarget;
            };
            if (target == null) {
                continue;
            }
            if (applyStatus(target, hint.key(), null)) {
                addLog(target.name + " is affected by " + statusName(hint.key()) + ".");
            }
        }
    }

    private void triggerTurnStatuses(Actor actor) {
        Map<String, EffectStack> bucket = statusBucket(actor);
        if (bucket.isEmpty()) {
            return;
        }
        if (bucket.containsKey("poison") && actor.alive()) {
            int dealt = applyDirectDamage(actor, Math.max(1, bucket.get("poison").power));
            addLog(actor.name + " suffers " + dealt + " poison damage.");
            floaters.add(new FloatingText("-" + dealt, floaterX(actor), floaterY(actor), 30, new Color(156, 224, 132)));
        }
        if (bucket.containsKey("burn") && actor.alive()) {
            int dealt = applyDirectDamage(actor, Math.max(1, bucket.get("burn").power + 2));
            addLog(actor.name + " burns for " + dealt + " damage.");
            floaters.add(new FloatingText("-" + dealt, floaterX(actor), floaterY(actor), 30, new Color(255, 196, 148)));
        }
        if (bucket.containsKey("regeneration") && actor.alive()) {
            int before = actor.hp;
            actor.hp = Math.min(actor.maxHp, actor.hp + Math.max(1, bucket.get("regeneration").power));
            int gained = actor.hp - before;
            if (gained > 0) {
                addLog(actor.name + " regenerates " + gained + " HP.");
                floaters.add(new FloatingText("+" + gained, floaterX(actor), floaterY(actor), 30, new Color(185, 247, 195)));
            }
        }
    }

    private void advanceStatusTurn(Actor actor) {
        Map<String, EffectStack> bucket = statusBucket(actor);
        for (String key : new ArrayList<>(bucket.keySet())) {
            EffectStack stack = bucket.get(key);
            if (!stack.tick()) {
                addLog(actor.name + " is no longer " + stack.effect.name() + ".");
                bucket.remove(key);
            }
        }
        if (bucket.isEmpty()) {
            statuses.remove(actor);
        }
    }

    private boolean applyStatus(Actor actor, String key, Integer power) {
        StatusEffect effect = GameData.STATUS_EFFECTS.getOrDefault(key, StatusEffects.ALL.get(key));
        if (effect == null) {
            return false;
        }
        Map<String, EffectStack> bucket = statusBucket(actor);
        EffectStack stack = bucket.get(key);
        if (stack != null) {
            stack.refresh(power);
            return true;
        }
        bucket.put(key, new EffectStack(effect, effect.duration(), power == null ? effect.power() : power));
        return true;
    }

    private void clearStatuses(Actor actor) {
        statuses.remove(actor);
    }

    private Map<String, EffectStack> statusBucket(Actor actor) {
        return statuses.computeIfAbsent(actor, key -> new HashMap<>());
    }

    private int modifyOutgoingDamage(Actor actor, int raw) {
        Map<String, EffectStack> bucket = statusBucket(actor);
        int modified = raw;
        if (bucket.containsKey("weak")) {
            modified = (int) (modified * 0.70);
        }
        if (bucket.containsKey("haste")) {
            modified = (int) (modified * 1.55);
        }
        return Math.max(1, modified);
    }

    private int dealDamage(Actor attacker, Actor target, int raw) {
        Map<String, EffectStack> bucket = statusBucket(target);
        int modified = raw;
        if (bucket.containsKey("vulnerable")) {
            modified = (int) (modified * 1.25);
        }
        if (bucket.containsKey("fortified")) {
            modified = (int) (modified * 0.75);
        }
        if (bucket.containsKey("shield")) {
            modified = Math.max(1, modified - bucket.get("shield").power);
        }
        return target.takeDamage(modified);
    }

    private int guardReducedRaw(Actor target, int raw) {
        if (isGuarding(target)) {
            addLog(target.name + "'s guard absorbs part of the strike.");
            setGuard(target, false);
            return Math.max(1, raw - 8);
        }
        return raw;
    }

    private int applyGuardMastery(Actor target, int damage) {
        if (target != player) {
            return damage;
        }
        int reduction = Math.min(Math.max(0, damage - 1), player.skillRank("guard_mastery"));
        if (reduction <= 0) {
            return damage;
        }
        player.hp = Math.min(player.maxHp, player.hp + reduction);
        return damage - reduction;
    }

    private int applyDirectDamage(Actor actor, int amount) {
        int before = actor.hp;
        actor.hp = Math.max(0, actor.hp - Math.max(0, amount));
        return Math.max(0, before - actor.hp);
    }

    private void tickMonsterCooldowns(Actor foe) {
        Map<String, Integer> cooldowns = cooldownsFor(foe);
        for (String name : new ArrayList<>(cooldowns.keySet())) {
            int next = cooldowns.get(name) - 1;
            if (next <= 0) {
                cooldowns.remove(name);
            } else {
                cooldowns.put(name, next);
            }
        }
    }

    private boolean checkFinished() {
        if (finished) {
            return true;
        }
        if (livingEnemies().isEmpty()) {
            win();
            return true;
        }
        if (livingParty().isEmpty()) {
            finished = true;
            victory = false;
            addLog("Your party falls. Press R to revive at Oakhaven.");
            return true;
        }
        return false;
    }

    private void win() {
        if (finished) {
            return;
        }
        finished = true;
        victory = true;
        for (Actor foe : enemies) {
            rememberDefeated(foe);
        }
        int rawGold = monsterSpecs.stream().mapToInt(GameData.MonsterSpec::gold).sum();
        int xp = monsterSpecs.stream().mapToInt(GameData.MonsterSpec::xp).sum();
        int gold = (int) Math.round(rawGold * (1.0 + player.skillRank("scavenger") * 0.10));
        player.gold += gold;
        List<String> recovered = partyMembers.stream().filter(actor -> !actor.alive()).map(actor -> actor.name).toList();
        for (Actor actor : partyMembers) {
            if (!actor.alive()) {
                actor.hp = Math.max(1, actor.maxHp / 4);
            }
        }
        List<String> notes = new ArrayList<>(player.gainXp(xp));
        addLog("Victory! +" + xp + " XP, +" + gold + " gold.");
        if (!recovered.isEmpty()) {
            addLog(String.join(", ", recovered) + " recover after the fight.");
        }
        for (String note : notes) {
            addLog(note.startsWith("Level ") ? "Level up! " + note : note);
        }
    }

    public void addLog(String message) {
        log.add(message);
        while (log.size() > 8) {
            log.remove();
        }
    }

    private void beginActivePartyTurn() {
        Actor actor = activeActor();
        if (actor == null || checkFinished()) {
            return;
        }
        triggerTurnStatuses(actor);
        if (checkFinished()) {
            return;
        }
        if (!actor.alive()) {
            addLog(actor.name + " falls before acting.");
            clearStatuses(actor);
            if (advanceToNextPartyActor()) {
                beginActivePartyTurn();
            } else {
                enemyTurn();
            }
            return;
        }
        addLog(actor.name + "'s turn.");
    }

    private boolean advanceToNextPartyActor() {
        int start = activePartyIndex + 1;
        for (int i = start; i < partyMembers.size(); i++) {
            if (partyMembers.get(i).alive()) {
                activePartyIndex = i;
                return true;
            }
        }
        return false;
    }

    private int firstLivingPartyIndex() {
        for (int i = 0; i < partyMembers.size(); i++) {
            if (partyMembers.get(i).alive()) {
                return i;
            }
        }
        return 0;
    }

    private Actor lowestPartyMember() {
        return livingParty().stream()
                .min(Comparator.comparingDouble(actor -> actor.hp / (double) Math.max(1, actor.maxHp)))
                .orElse(player);
    }

    private Actor randomLivingPartyMember() {
        List<Actor> living = livingParty();
        if (living.isEmpty()) {
            return player;
        }
        return living.get(random.nextInt(living.size()));
    }

    private void setGuard(Actor actor, boolean enabled) {
        if (enabled) {
            guards.put(actor, true);
        } else {
            guards.remove(actor);
        }
    }

    private void playEffect(String kind, Actor source, Actor target, int timer) {
        effectKind = kind == null || kind.isBlank() ? "strike" : kind;
        effectSource = source;
        effectTarget = target;
        effectTimer = timer;
    }

    private int randomBetween(int min, int max) {
        if (max <= min) {
            return min;
        }
        return min + random.nextInt(max - min + 1);
    }

    private int floaterX(Actor actor) {
        int enemyIndex = enemies.indexOf(actor);
        if (enemyIndex >= 0) {
            return GameConfig.MAP_COLS * GameConfig.TILE - 310 - (enemyIndex % 2) * 72;
        }
        int partyIndex = Math.max(0, partyMembers.indexOf(actor));
        return 165 + (partyIndex % 2) * 46;
    }

    private int floaterY(Actor actor) {
        int enemyIndex = enemies.indexOf(actor);
        if (enemyIndex >= 0) {
            return 210 + enemyIndex * 116;
        }
        int partyIndex = Math.max(0, partyMembers.indexOf(actor));
        return 252 + partyIndex * 86;
    }

    private void setEnemyLunge(Actor foe, int amount) {
        enemyLunges.put(foe, Math.max(enemyLunges.getOrDefault(foe, 0), amount));
        if (foe == enemy) {
            monsterLunge = enemyLunges.get(foe);
        }
    }

    private Map<String, Integer> cooldownsFor(Actor foe) {
        return monsterCooldowns.computeIfAbsent(foe, key -> new HashMap<>());
    }

    private void rememberDefeated(Actor foe) {
        if (foe != null && enemies.contains(foe) && !defeatedEnemies.contains(foe)) {
            defeatedEnemies.add(foe);
            defeatedMonsterNames.add(foe.name);
        }
    }

    private int clampSelectedIndex(int index, List<Actor> actors) {
        if (actors.isEmpty()) {
            return 0;
        }
        return Math.max(0, Math.min(index, actors.size() - 1));
    }

    private int cycleLivingIndex(List<Actor> actors, int current, int direction) {
        if (actors.isEmpty()) {
            return 0;
        }
        int step = direction < 0 ? -1 : 1;
        int index = clampSelectedIndex(current, actors);
        for (int i = 0; i < actors.size(); i++) {
            index = Math.floorMod(index + step, actors.size());
            if (actors.get(index).alive()) {
                return index;
            }
        }
        return current;
    }

    private String castMessage(Actor actor, String abilityName, String detail) {
        if (actor == player) {
            return "You cast " + abilityName + "; " + detail;
        }
        return actor.name + " casts " + abilityName + "; " + detail;
    }

    private String statusName(String key) {
        StatusEffect effect = GameData.STATUS_EFFECTS.getOrDefault(key, StatusEffects.ALL.get(key));
        return effect == null ? key : effect.name();
    }

    private String effectForAbility(String abilityName) {
        String lowered = abilityName.toLowerCase();
        if (lowered.contains("fire") || lowered.contains("arc")) {
            return "fire";
        }
        if (lowered.contains("frost") || lowered.contains("ice")) {
            return "frost";
        }
        if (lowered.contains("poison") || lowered.contains("venom")) {
            return "poison";
        }
        if (lowered.contains("volley") || lowered.contains("shot") || lowered.contains("arrow")) {
            return "volley";
        }
        if (lowered.contains("slash") || lowered.contains("rush") || lowered.contains("bash")) {
            return "slash";
        }
        return "strike";
    }

    private String chooseBackdrop(char terrain, String mapKind) {
        if ("dungeon".equals(mapKind)) {
            return "battle_dungeon_hall_backdrop";
        }
        return switch (terrain) {
            case 'f' -> "battle_forest_backdrop";
            case 's', 'b' -> "battle_desert_backdrop";
            case 'n' -> "battle_snow_backdrop";
            case 'q', 'm' -> "battle_mountain_backdrop";
            case 'd' -> "battle_dungeon_cavern_backdrop";
            default -> "battle_plains_backdrop";
        };
    }
}
