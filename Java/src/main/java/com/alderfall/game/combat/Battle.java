package com.alderfall.game;

import com.alderfall.game.inventory.Item;
import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

public final class Battle {
    public static final int MAX_ENEMIES = 5;
    public static final int MAX_PLAYER_COMPANIONS = 3;
    private static final int INTRO_TOTAL_TICKS = 84;
    private final Random random;
    private final Map<Actor, Map<String, EffectStack>> statuses = new IdentityHashMap<>();
    private final Map<Actor, Map<String, Integer>> monsterCooldowns = new IdentityHashMap<>();
    private final Map<Actor, Map<String, Integer>> partyCooldowns = new IdentityHashMap<>();
    private final Map<Actor, Map<String, Double>> monsterHpThresholds = new IdentityHashMap<>();
    private final Set<Actor> firstCriticalSpent = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Actor> firstPotionSpent = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Actor> lethalSaveSpent = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Actor> woundedFocusSpent = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Actor> eliteSecondWindSpent = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<Actor, String> alternatingDamageMode = new IdentityHashMap<>();
    private final Map<Actor, Boolean> guards = new IdentityHashMap<>();
    private final Map<Actor, Boolean> eliteEnemies = new IdentityHashMap<>();
    private final Map<Actor, EnemyRole> enemyRoles = new IdentityHashMap<>();
    private final Map<Actor, EliteTrait> eliteTraits = new IdentityHashMap<>();
    private final Map<Actor, Integer> bossPhases = new IdentityHashMap<>();
    private final List<Actor> partyMembers = new ArrayList<>();
    private final Map<Actor, GameData.MonsterSpec> enemySpecs = new IdentityHashMap<>();
    private final Map<Actor, Integer> enemyFades = new IdentityHashMap<>();
    private final Map<Actor, Integer> enemyLunges = new IdentityHashMap<>();
    private final Map<Actor, Integer> hitFlashes = new IdentityHashMap<>();
    private final Map<Actor, Integer> comboCounters = new IdentityHashMap<>();
    private final List<Actor> defeatedEnemies = new ArrayList<>();
    private final List<String> defeatedMonsterNames = new ArrayList<>();
    private final List<String> defeatedMonsterKeys = new ArrayList<>();
    private final Map<String, List<String>> discoveredMonsterVulnerabilities = new HashMap<>();
    private final Map<String, Integer> knownMonsterInsights = new HashMap<>();
    private final Map<String, Set<String>> knownMonsterVulnerabilities = new HashMap<>();
    private int activePartyIndex;
    private int selectedEnemyIndex;
    private int selectedPartyIndex;
    private boolean partyTargetExplicit;
    private int enemyTurnIndex;
    private boolean enemyPhase;
    private BattleActionAnimation actionAnimation;
    private Consumer<AbilityResolutionStep> animationImpact;
    private Runnable animationComplete;
    private String actionTelegraphLabel = "";
    private int actionTelegraphDanger;

    public final Actor player;
    public final Actor enemy;
    public final List<Actor> enemies = new ArrayList<>();
    public final GameData.MonsterSpec monsterSpec;
    public final List<GameData.MonsterSpec> monsterSpecs;
    private final double monsterLevelScaling;
    public final Queue<String> log = new ArrayDeque<>();
    public final List<FloatingText> floaters = new ArrayList<>();
    public final String backdrop;
    public final char terrain;
    public final String mapKind;
    public boolean finished;
    public boolean victory;
    public boolean fled;
    public boolean questRecorded;
    public boolean lootGranted;
    public boolean companionTrustGranted;
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
    private int introTimer;
    private int introTotalTicks;
    private String introStyleLabel = "";
    private String introEncounterLine = "";
    private String introPartyBark = "";

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
        this(player, allies, monsterSpecs, random, terrain, mapKind, 1.0);
    }

    public Battle(Actor player, List<Actor> allies, List<GameData.MonsterSpec> monsterSpecs, Random random, char terrain, String mapKind, double monsterLevelScaling) {
        this.player = player;
        this.random = random == null ? new Random() : random;
        this.backdrop = chooseBackdrop(terrain, mapKind);
        this.terrain = terrain;
        this.mapKind = mapKind == null ? "overworld" : mapKind;
        this.monsterLevelScaling = Math.max(0.0, Math.min(1.0, monsterLevelScaling));
        List<GameData.MonsterSpec> specs = monsterSpecs == null || monsterSpecs.isEmpty()
                ? List.of(GameData.MONSTERS.get("slime"))
                : monsterSpecs.stream().filter(spec -> spec != null).limit(MAX_ENEMIES).toList();
        if (specs.isEmpty()) {
            specs = List.of(GameData.MONSTERS.get("slime"));
        }
        this.monsterSpecs = List.copyOf(specs);
        this.monsterSpec = specs.get(0);
        Actor firstEnemy = null;
        int partySize = 1 + (int) allies.stream()
                .filter(ally -> ally != null && ally != player)
                .distinct()
                .limit(MAX_PLAYER_COMPANIONS)
                .count();
        boolean eliteAssigned = false;
        for (GameData.MonsterSpec spec : specs) {
            boolean boss = isBossSpec(spec);
            boolean elite = !boss && !eliteAssigned && rollEliteEnemy(specs.size());
            Actor foe = elite ? createEliteActor(spec) : spec.createActor();
            scaleEnemyForBattle(foe, spec, partySize, specs.size());
            if (elite) {
                scaleEliteEnemy(foe);
                eliteEnemies.put(foe, true);
                eliteAssigned = true;
            }
            if (firstEnemy == null) {
                firstEnemy = foe;
            }
            enemies.add(foe);
            enemySpecs.put(foe, spec);
            enemyFades.put(foe, 255);
            enemyLunges.put(foe, 0);
            EnemyRole role = chooseEnemyRole(spec, foe);
            enemyRoles.put(foe, role);
            applyEnemyRoleStats(foe, role);
            if (elite) {
                EliteTrait trait = chooseEliteTrait(spec, role);
                eliteTraits.put(foe, trait);
                applyEliteTraitOpening(foe, trait);
            }
            if (boss) {
                bossPhases.put(foe, 1);
                applyBossOpeningPresence(foe);
            }
        }
        this.enemy = firstEnemy;
        partyMembers.add(player);
        for (Actor ally : allies) {
            if (ally != null && !partyMembers.contains(ally) && partyMembers.size() <= MAX_PLAYER_COMPANIONS) {
                partyMembers.add(ally);
            }
        }
        if (enemies.size() == 1) {
            addLog(this.monsterSpec.name() + " appears!");
        } else {
            addLog(enemies.size() + " enemies appear!");
        }
        for (Actor foe : enemies) {
            if (isEliteEnemy(foe)) {
                addLog(foe.name + " steps forward as " + eliteTraitLabel(foe) + ".");
                break;
            }
        }
        for (Actor foe : enemies) {
            if (isBossEnemy(foe)) {
                addLog(foe.name + " enters as a boss with phase breaks at 70% and 35% HP.");
                break;
            }
        }
        addLog(enemyRoleSummary());
        beginActivePartyTurn();
    }

    public enum EnemyRole {
        BRUISER("Bruiser"),
        GUARD("Guard"),
        SKIRMISHER("Skirmisher"),
        HEXER("Hexer"),
        SWARMER("Swarmer");

        private final String label;

        EnemyRole(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public enum EliteTrait {
        FRENZIED("Frenzied"),
        BULWARK("Bulwark"),
        VAMPIRIC("Vampiric"),
        VOLATILE("Volatile"),
        HEXBOUND("Hexbound"),
        COMMANDER("Commander"),
        EXECUTIONER("Executioner"),
        MIRROR("Mirror");

        private final String label;

        EliteTrait(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private enum AttackStyle {
        BASIC,
        HEAVY,
        CLEAVE
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

    public boolean isEliteEnemy(Actor actor) {
        return eliteEnemies.getOrDefault(actor, false);
    }

    public EliteTrait eliteTrait(Actor actor) {
        return eliteTraits.get(actor);
    }

    public String eliteTraitLabel(Actor actor) {
        EliteTrait trait = eliteTrait(actor);
        return trait == null ? "Elite" : trait.label() + " Elite";
    }

    public boolean isBossEnemy(Actor actor) {
        return bossPhases.containsKey(actor);
    }

    public int bossPhase(Actor actor) {
        return bossPhases.getOrDefault(actor, 0);
    }

    public String enemyTraitLabel(Actor actor) {
        if (isBossEnemy(actor)) {
            return "Boss Phase " + bossPhase(actor);
        }
        return isEliteEnemy(actor) ? eliteTraitLabel(actor) : enemyRoleLabel(actor);
    }

    public EnemyRole enemyRole(Actor actor) {
        return enemyRoles.getOrDefault(actor, EnemyRole.BRUISER);
    }

    public String enemyRoleLabel(Actor actor) {
        return enemyRole(actor).label();
    }

    public List<String> defeatedMonsterNames() {
        return List.copyOf(defeatedMonsterNames);
    }

    public List<String> defeatedMonsterKeys() {
        return List.copyOf(defeatedMonsterKeys);
    }

    public Map<String, List<String>> discoveredMonsterVulnerabilities() {
        Map<String, List<String>> copy = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : discoveredMonsterVulnerabilities.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return copy;
    }

    public void setMonsterInsightCounts(Map<String, Integer> insights) {
        knownMonsterInsights.clear();
        if (insights == null) {
            return;
        }
        for (Map.Entry<String, Integer> entry : insights.entrySet()) {
            if (entry.getValue() != null && entry.getValue() > 0) {
                knownMonsterInsights.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public void setKnownMonsterVulnerabilities(Map<String, Set<String>> vulnerabilities) {
        knownMonsterVulnerabilities.clear();
        if (vulnerabilities == null) {
            return;
        }
        for (Map.Entry<String, Set<String>> entry : vulnerabilities.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                knownMonsterVulnerabilities.put(entry.getKey(), Set.copyOf(entry.getValue()));
            }
        }
    }

    public int enemyFade(Actor actor) {
        return enemyFades.getOrDefault(actor, 255);
    }

    public int enemyLunge(Actor actor) {
        return enemyLunges.getOrDefault(actor, 0);
    }

    public int hitFlash(Actor actor) {
        return hitFlashes.getOrDefault(actor, 0);
    }

    public boolean hasIncomingTelegraph(Actor actor) {
        return actor != null
                && actionAnimation != null
                && actionAnimation.stage() == BattleActionAnimation.Stage.CAST
                && actionAnimation.target == actor
                && !actionTelegraphLabel.isBlank();
    }

    public String incomingTelegraphLabel(Actor actor) {
        return hasIncomingTelegraph(actor) ? actionTelegraphLabel : "";
    }

    public int incomingTelegraphDanger(Actor actor) {
        return hasIncomingTelegraph(actor) ? actionTelegraphDanger : 0;
    }

    public String sourceTelegraphLabel(Actor actor) {
        return actor != null
                && actionAnimation != null
                && actionAnimation.stage() == BattleActionAnimation.Stage.CAST
                && actionAnimation.source == actor
                && !actionTelegraphLabel.isBlank()
                ? actionTelegraphLabel
                : "";
    }

    public int sourceTelegraphDanger(Actor actor) {
        return sourceTelegraphLabel(actor).isBlank() ? 0 : actionTelegraphDanger;
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

    public boolean hasExplicitPartyTarget() {
        Actor target = selectedPartyMember();
        return partyTargetExplicit && target != null && target.alive();
    }

    public void selectEnemy(int index) {
        if (index >= 0 && index < enemies.size() && enemies.get(index).alive()) {
            selectedEnemyIndex = index;
            partyTargetExplicit = false;
        }
    }

    public void selectPartyMember(int index) {
        if (index >= 0 && index < partyMembers.size() && partyMembers.get(index).alive()) {
            selectedPartyIndex = index;
            partyTargetExplicit = true;
        }
    }

    public void cycleEnemyTarget(int direction) {
        selectedEnemyIndex = cycleLivingIndex(enemies, selectedEnemyIndex, direction);
        partyTargetExplicit = false;
    }

    public void cyclePartyTarget(int direction) {
        selectedPartyIndex = cycleLivingIndex(partyMembers, selectedPartyIndex, direction);
        partyTargetExplicit = selectedPartyMember() != null;
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

    public void grantPartyStartingAdvantage(boolean strong) {
        for (Actor actor : livingParty()) {
            applyStatus(actor, "haste", strong ? 3 : 2);
            applyStatus(actor, "shield", strong ? 10 : 6);
            if (strong) {
                applyStatus(actor, "regeneration", 4);
            }
        }
        addLog(strong ? "The party enters with a decisive advantage." : "The party enters ready.");
    }

    public boolean isGuarding(Actor actor) {
        return guards.containsKey(actor);
    }

    public boolean canAcceptInput() {
        return !finished && actionAnimation == null && !enemyPhase && !introActive();
    }

    public boolean isAnimating() {
        return actionAnimation != null;
    }

    public BattleActionAnimation activeAnimation() {
        return actionAnimation;
    }

    public void configureIntro(String styleLabel, String encounterLine, String partyBark) {
        introStyleLabel = styleLabel == null ? "" : styleLabel.trim();
        introEncounterLine = encounterLine == null ? "" : encounterLine.trim();
        introPartyBark = partyBark == null ? "" : partyBark.trim();
        boolean hasIntroText = !introStyleLabel.isBlank() || !introEncounterLine.isBlank() || !introPartyBark.isBlank();
        introTotalTicks = hasIntroText ? INTRO_TOTAL_TICKS : 0;
        introTimer = introTotalTicks;
    }

    public boolean introActive() {
        return introTimer > 0;
    }

    public void skipIntro() {
        introTimer = 0;
    }

    public double introProgress() {
        if (introTotalTicks <= 0) {
            return 1.0;
        }
        return 1.0 - introTimer / (double) introTotalTicks;
    }

    public String introStyleLabel() {
        return introStyleLabel;
    }

    public String introEncounterLine() {
        return introEncounterLine;
    }

    public String introPartyBark() {
        return introPartyBark;
    }

    public boolean effectReleased() {
        return actionAnimation == null || actionAnimation.released();
    }

    public boolean isCasting(Actor actor) {
        return actionAnimation != null
                && actionAnimation.source == actor
                && actionAnimation.stage() == BattleActionAnimation.Stage.CAST;
    }

    public int castOffset(Actor actor) {
        if (!isCasting(actor)) {
            return 0;
        }
        return (int) Math.round(Math.sin(actionAnimation.castProgress() * Math.PI) * 12.0);
    }

    public String animationStatus() {
        if (actionAnimation == null) {
            return "";
        }
        return switch (actionAnimation.stage()) {
            case CAST -> actionTelegraphLabel.isBlank()
                    ? actionAnimation.source.name + " is casting."
                    : actionAnimation.source.name + " readies " + actionTelegraphLabel + " against " + actionAnimation.target.name + ".";
            case TRAVEL -> actionAnimation.effectKind + " flies toward " + actionAnimation.target.name + ".";
            case IMPACT -> actionAnimation.target.name + " is hit.";
            case DONE -> "";
        };
    }

    public void tick() {
        if (introTimer > 0) {
            introTimer--;
            return;
        }
        shake = Math.max(0, shake - 1);
        monsterOffset = shake * (shake % 2 == 0 ? 1 : -1);
        playerOffset = (shake / 2) * (shake % 2 == 0 ? -1 : 1);
        playerLunge = Math.max(0, playerLunge - 2);
        monsterLunge = Math.max(0, monsterLunge - 2);
        for (Actor foe : enemies) {
            enemyLunges.put(foe, Math.max(0, enemyLunges.getOrDefault(foe, 0) - 2));
        }
        for (Actor actor : new ArrayList<>(hitFlashes.keySet())) {
            int next = Math.max(0, hitFlashes.getOrDefault(actor, 0) - 1);
            if (next <= 0) {
                hitFlashes.remove(actor);
            } else {
                hitFlashes.put(actor, next);
            }
        }
        flashTimer = Math.max(0, flashTimer - 1);
        if (actionAnimation == null) {
            effectTimer = Math.max(0, effectTimer - 1);
        }
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
        tickActionAnimation();
    }

    public void playerAttack() {
        playerBasicAttack();
    }

    public void playerBasicAttack() {
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
        beginAnimatedAction(
                "strike",
                actor,
                target,
                5,
                8,
                8,
                () -> resolvePartyAttack(actor, target, AttackStyle.BASIC),
                () -> afterPartyAction(actor)
        );
    }

    public void playerHeavyAttack() {
        Actor actor = activeActor();
        if (!canPartyAct(actor)) {
            return;
        }
        if (actor.mp < 3) {
            addLog(actor.name + " needs 3 MP for a heavy attack.");
            return;
        }
        Actor target = selectedEnemy();
        if (target == null) {
            checkFinished();
            return;
        }
        actor.mp -= 3;
        setGuard(actor, false);
        beginTelegraphedAnimatedAction(
                "Heavy",
                2,
                "bash",
                actor,
                target,
                9,
                10,
                12,
                () -> resolvePartyAttack(actor, target, AttackStyle.HEAVY),
                () -> afterPartyAction(actor)
        );
    }

    public void playerCleaveAttack() {
        Actor actor = activeActor();
        if (!canPartyAct(actor)) {
            return;
        }
        if (actor.mp < 2) {
            addLog(actor.name + " needs 2 MP for cleave.");
            return;
        }
        List<Actor> targets = livingEnemies().stream().limit(3).toList();
        if (targets.isEmpty()) {
            checkFinished();
            return;
        }
        actor.mp -= 2;
        setGuard(actor, false);
        int[] totalDamage = {0};
        int[] hitCount = {0};
        beginAnimatedAction(
                "cleave",
                actor,
                targets.get(0),
                targets,
                BattleActionAnimation.VisualMode.AOE,
                7,
                10,
                10,
                step -> {
                    DamageStepResult result = resolvePartyAttackStep(actor, step.target(), AttackStyle.CLEAVE);
                    totalDamage[0] += result.damage();
                    if (result.hit()) {
                        hitCount[0]++;
                    }
                },
                () -> {
                    addLog(actor.name + " cleaves " + hitCount[0] + " foe" + (hitCount[0] == 1 ? "" : "s")
                            + " for " + totalDamage[0] + " total.");
                    shake = Math.max(shake, 12);
                    playerLunge = Math.max(playerLunge, 20);
                    afterPartyAction(actor);
                }
        );
    }

    public void playerDodge() {
        Actor actor = activeActor();
        if (!canPartyAct(actor)) {
            return;
        }
        setGuard(actor, false);
        beginAnimatedAction(
                "dust",
                actor,
                actor,
                6,
                1,
                8,
                () -> {
                    applyStatus(actor, "evasive", Math.max(16, 18 + actor.dexterity / 2));
                    applyStatus(actor, "haste", null);
                    actor.mp = Math.min(actor.maxMp, actor.mp + 2);
                    floaters.add(new FloatingText("Evade", floaterX(actor), floaterY(actor), 32, new Color(200, 225, 255)));
                    addLog(actor.name + " slips into evasive footwork, gaining +2 MP.");
                },
                () -> afterPartyAction(actor)
        );
    }

    public void playerRun() {
        Actor actor = activeActor();
        if (!canPartyAct(actor)) {
            return;
        }
        if (hasBossEnemy()) {
            addLog(actor.name + " cannot escape this fight.");
            return;
        }
        setGuard(actor, false);
        beginAnimatedAction(
                "dust",
                actor,
                actor,
                5,
                1,
                6,
                () -> resolveRunAttempt(actor),
                () -> {
                    if (!finished) {
                        afterPartyAction(actor);
                    }
                }
        );
    }

    public void useAbility(int index) {
        Actor actor = activeActor();
        List<Ability> abilities = actor == null ? List.of() : actor.activeAbilities();
        if (!canPartyAct(actor) || index < 0 || index >= abilities.size()) {
            return;
        }
        Ability ability = abilities.get(index);
        if (actor.mp < ability.cost()) {
            addLog(actor.name + " does not have enough MP.");
            return;
        }
        int remainingCooldown = abilityCooldownRemaining(actor, ability);
        if (remainingCooldown > 0) {
            addLog(ability.name() + " is cooling down for " + remainingCooldown + " more turn"
                    + (remainingCooldown == 1 ? "." : "s."));
            return;
        }
        Actor enemyTarget = selectedEnemy();
        List<Actor> healTargets = List.of();
        List<Actor> damageTargets = List.of();
        Actor effectTarget = actor;
        String effectKind = "shield";
        if (ability.kind() == Ability.AbilityKind.HEAL) {
            if (requiresExplicitAllyTarget(ability) && !hasExplicitPartyTarget()) {
                addLog("Choose an ally target before using " + ability.name() + ".");
                return;
            }
            healTargets = healTargetsFor(actor, ability);
            if (healTargets.isEmpty()) {
                return;
            }
            effectTarget = healTargets.get(0);
            effectKind = "heal";
        } else if (ability.kind() == Ability.AbilityKind.DAMAGE) {
            damageTargets = damageTargetsFor(ability);
            if (damageTargets.isEmpty()) {
                return;
            }
            effectTarget = damageTargets.get(0);
            effectKind = effectForAbility(ability);
        }
        List<Actor> finalHealTargets = healTargets;
        List<Actor> finalDamageTargets = damageTargets;
        Actor finalEnemyTarget = enemyTarget;
        Actor finalEffectTarget = effectTarget;
        String finalEffectKind = effectKind;
        BattleActionAnimation.VisualMode finalVisualMode = visualModeForAbility(ability);
        int[] finalTotalDamage = {0};
        int[] finalHitCount = {0};
        Actor[] finalLastHitTarget = {null};
        actor.mp -= ability.cost();
        startAbilityCooldown(actor, ability);
        switch (ability.kind()) {
            case HEAL -> beginAnimatedAction(
                    finalEffectKind,
                    actor,
                    finalEffectTarget,
                    10,
                    8,
                    12,
                    () -> {
                        resolveHealAbility(actor, ability, finalHealTargets, finalEnemyTarget);
                        applyMpFlow(actor);
                        applyPostCastMomentum(actor);
                    },
                    () -> afterPartyAction(actor)
            );
            case DEFEND -> beginAnimatedAction(
                    finalEffectKind,
                    actor,
                    actor,
                    8,
                    1,
                    12,
                    () -> {
                        defend(actor, ability, 4 + actor.skillRank("stalwart_guard"));
                        applyAbilityStatuses(actor, ability, actor, finalEnemyTarget);
                        applyMpFlow(actor);
                        applyPostCastMomentum(actor);
                    },
                    () -> afterPartyAction(actor)
            );
            case DAMAGE -> beginAnimatedAction(
                    finalEffectKind,
                    actor,
                    finalEffectTarget,
                    finalDamageTargets,
                    finalVisualMode,
                    10,
                    12,
                    10,
                    step -> {
                        DamageStepResult result = resolveDamageAbilityStep(actor, ability, step.target(), finalDamageTargets.size());
                        finalTotalDamage[0] += result.damage();
                        if (result.hit()) {
                            finalHitCount[0]++;
                            finalLastHitTarget[0] = step.target();
                        }
                    },
                    () -> {
                        finishDamageAbility(actor, ability, finalLastHitTarget[0], finalTotalDamage[0], finalHitCount[0]);
                        applyMpFlow(actor);
                        applyPostCastMomentum(actor);
                        afterPartyAction(actor);
                    }
            );
        }
    }

    public boolean playerUseItem(Item item) {
        return item != null && playerUseItem(item.name(), item.heal(), item.mp());
    }

    public boolean playerUseItem(String itemName, int heal, int mp) {
        Actor actor = activeActor();
        if (!canPartyAct(actor) || (heal <= 0 && mp <= 0)) {
            return false;
        }
        if (!hasExplicitPartyTarget()) {
            addLog("Choose an ally target before using " + itemName + ".");
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
        beginAnimatedAction(
                "item",
                actor,
                target,
                7,
                8,
                10,
                () -> resolveItemUse(actor, target, itemName, heal, mp),
                () -> afterPartyAction(actor)
        );
        return true;
    }

    private boolean canPartyAct(Actor actor) {
        return canAcceptInput() && actor != null && actor.alive() && !enemies.contains(actor);
    }

    public boolean requiresExplicitAllyTarget(Ability ability) {
        return ability != null
                && ability.kind() == Ability.AbilityKind.HEAL
                && !"self".equals(ability.target())
                && !"party".equals(ability.target());
    }

    private List<Actor> healTargetsFor(Actor actor, Ability ability) {
        setGuard(actor, false);
        if ("self".equals(ability.target())) {
            return List.of(actor);
        }
        if ("party".equals(ability.target())) {
            if (ability.hasTag("revive_party")) {
                return new ArrayList<>(partyMembers);
            }
            return new ArrayList<>(livingParty());
        }
        Actor target = selectedPartyMember();
        if (target == null) {
            target = lowestPartyMember();
        }
        return target == null ? List.of() : List.of(target);
    }

    private void resolveHealAbility(Actor actor, Ability ability, List<Actor> targets, Actor enemyTarget) {
        int bonus = actor.abilityScalingBonus(ability) + actor.healingBonus() + actor.skillRank("channeling") * 4;
        int totalHealed = 0;
        Actor effectTarget = targets.isEmpty() ? actor : targets.get(0);
        for (Actor target : targets) {
            if (!target.alive() && ability.hasTag("revive_party")) {
                target.hp = 1;
                addLog(target.name + " answers " + ability.name() + " and stands again.");
            }
            if (!target.alive()) {
                continue;
            }
            int amount = Math.max(1, ability.power() + actor.level + bonus + random.nextInt(5) - 2);
            if ("party".equals(ability.target())) {
                amount = Math.max(1, (int) Math.round(amount * 0.70));
            }
            int before = target.hp;
            target.hp = Math.min(target.maxHp, target.hp + amount);
            int healed = target.hp - before;
            totalHealed += healed;
            if (ability.hasTag("cleanse") && cleanseNegativeStatuses(target)) {
                addLog(target.name + " is cleansed by " + ability.name() + ".");
            }
            int overheal = Math.max(0, before + amount - target.maxHp);
            if ((ability.hasTag("overheal_shield") || hasEquipmentEffect(actor, EquipmentEffectHooks.OVERHEAL_WARD)) && overheal > 0) {
                applyStatus(target, "shield", Math.max(3, overheal / 2));
                if (hasEquipmentEffect(actor, EquipmentEffectHooks.OVERHEAL_PARTY_PULSE)) {
                    int pulse = Math.max(3, overheal / 3);
                    for (Actor ally : livingParty()) {
                        applyStatus(ally, "shield", pulse);
                    }
                    addLog(actor.name + "'s legendary overheal pulses across the party.");
                }
            }
            applyAbilityStatuses(actor, ability, target, enemyTarget);
            floaters.add(new FloatingText("+" + healed, floaterX(target), floaterY(target), 34, new Color(185, 247, 195)));
        }
        if (totalHealed > 0 && hasEquipmentEffect(actor, EquipmentEffectHooks.CRITICAL_HEAL_RIPPLE)
                && random.nextDouble() < actor.criticalChanceAgainst(null, ability)) {
            Actor rippleTarget = livingParty().stream()
                    .filter(candidate -> !targets.contains(candidate))
                    .min(Comparator.comparingDouble(candidate -> candidate.hp / (double) Math.max(1, candidate.maxHp)))
                    .orElse(null);
            if (rippleTarget != null && rippleTarget.hp < rippleTarget.maxHp) {
                int ripple = Math.max(1, totalHealed / 4);
                int before = rippleTarget.hp;
                rippleTarget.hp = Math.min(rippleTarget.maxHp, rippleTarget.hp + ripple);
                int healed = rippleTarget.hp - before;
                if (healed > 0) {
                    totalHealed += healed;
                    floaters.add(new FloatingText("+" + healed, floaterX(rippleTarget), floaterY(rippleTarget), 30, new Color(185, 247, 195)));
                    addLog(actor.name + "'s critical healing ripples to " + rippleTarget.name + ".");
                }
            }
        }
        String detail = "party recovers " + totalHealed + " total.";
        if (targets.size() == 1) {
            detail = effectTarget.name + " recovers " + totalHealed + ".";
        }
        addLog(castMessage(actor, ability.name(), detail));
    }

    private List<Actor> damageTargetsFor(Ability ability) {
        if ("all_enemies".equals(ability.target())) {
            return new ArrayList<>(livingEnemies());
        }
        Actor selected = selectedEnemy();
        return selected == null || !selected.alive() ? List.of() : List.of(selected);
    }

    private BattleActionAnimation.VisualMode visualModeForAbility(Ability ability) {
        if (ability == null) {
            return BattleActionAnimation.VisualMode.SINGLE;
        }
        return switch (ability.visualResolution()) {
            case CHAIN -> BattleActionAnimation.VisualMode.CHAIN;
            case MULTI_PROJECTILE -> BattleActionAnimation.VisualMode.MULTI;
            case AOE -> BattleActionAnimation.VisualMode.AOE;
            case SINGLE -> BattleActionAnimation.VisualMode.SINGLE;
        };
    }

    private DamageStepResult resolveDamageAbilityStep(Actor actor, Ability ability, Actor target, int originalTargetCount) {
        if (actor == null || ability == null || target == null || !target.alive()) {
            return new DamageStepResult(0, false);
        }
        int bonus = actor.skillRank("spellcraft") * 2 + actor.skillRank("overchannel") * 3;
        if ("all_enemies".equals(ability.target())) {
            bonus += actor.skillRank("volley_mastery") * 2;
        }
        int raw = ability.power() + bonus + actor.abilityDamageBonus(ability) + random.nextInt(6);
        if (originalTargetCount > 1) {
            raw = Math.max(1, (int) Math.round(raw * 0.78));
        }
        raw = modifyOutgoingDamage(actor, raw);
        raw = modifyAbilityDamage(actor, target, ability, raw);
        DamageResult result = dealDamage(actor, target, raw, ability, true);
        int totalDamage = result.damage();
        addDamageFloater(target, result, new Color(255, 224, 166));
        applyEliteDamagedTrait(target, actor, ability, result);
        if (totalDamage > 0 && target.alive() && isSpellAbility(ability)
                && hasEquipmentEffect(actor, EquipmentEffectHooks.SPELL_ECHO)) {
            int echo = applyDirectDamage(target, Math.max(1, totalDamage / 5));
            if (echo > 0) {
                totalDamage += echo;
                floaters.add(new FloatingText("Echo " + echo, floaterX(target), floaterY(target) - 10, 28, new Color(190, 220, 255)));
                addLog(actor.name + "'s spell leaves a glass echo in " + target.name + ".");
            }
        }
        applyAbilityStatuses(actor, ability, actor, target);
        checkBossPhaseTransition(target);
        if (!target.alive()) {
            addLog(target.name + " falls.");
            clearStatuses(target);
            rememberDefeated(target);
        }
        return new DamageStepResult(totalDamage, true);
    }

    private void finishDamageAbility(Actor actor, Ability ability, Actor displayTarget, int totalDamage, int hitCount) {
        setGuard(actor, false);
        if (hitCount <= 0) {
            addLog(actor.name + "'s " + ability.name() + " finds no target.");
            return;
        }
        String detail = hitCount == 1 && displayTarget != null
                ? "deals " + totalDamage + " to " + displayTarget.name + "."
                : "hits " + hitCount + " enemies for " + totalDamage + " total.";
        addLog(castMessage(actor, ability.name(), detail));
        shake = 14;
        playerLunge = 22;
        flashTimer = 5;
    }

    private record DamageStepResult(int damage, boolean hit) {
    }

    private void defend(Actor actor, Ability ability, int mpGain) {
        setGuard(actor, true);
        mpGain += actor.guardBonus();
        actor.mp = Math.min(actor.maxMp, actor.mp + mpGain);
        applyStatus(actor, "shield", null);
        applyStatus(actor, "fortified", null);
        if (ability.hasTag("party_guard")) {
            int shieldPower = Math.max(4, actor.abilityScalingBonus(ability) / 3 + actor.defense);
            for (Actor ally : livingParty()) {
                applyStatus(ally, "shield", shieldPower);
                applyStatus(ally, "fortified", null);
            }
            addLog(actor.name + " extends the guard across the party.");
        } else if (hasEquipmentEffect(actor, EquipmentEffectHooks.DEFEND_STEADIES_ALLIES)) {
            int shieldPower = Math.max(3, actor.defense / 3 + actor.willpower / 5);
            for (Actor ally : livingParty()) {
                if (ally != actor) {
                    applyStatus(ally, "shield", shieldPower);
                }
            }
            addLog(actor.name + "'s oath steadies nearby allies.");
        }
        floaters.add(new FloatingText("Guard", floaterX(actor), floaterY(actor), 32, new Color(214, 232, 255)));
        addLog(actor.name + " uses " + ability.name() + ": guard stance up, +" + mpGain + " MP.");
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
        beginEnemyPhase();
    }

    private void enemyTurn() {
        beginEnemyPhase();
    }

    private void beginEnemyPhase() {
        if (finished || livingEnemies().isEmpty()) {
            return;
        }
        enemyPhase = true;
        enemyTurnIndex = 0;
        continueEnemyPhase();
    }

    private void continueEnemyPhase() {
        if (finished) {
            enemyPhase = false;
            return;
        }
        while (enemyTurnIndex < enemies.size()) {
            Actor foe = enemies.get(enemyTurnIndex++);
            if (!foe.alive()) {
                continue;
            }
            triggerTurnStatuses(foe);
            if (!foe.alive()) {
                addLog(foe.name + " falls.");
                clearStatuses(foe);
                rememberDefeated(foe);
            }
            if (checkFinished()) {
                enemyPhase = false;
                return;
            }
            if (!foe.alive()) {
                continue;
            }
            addLog(foe.name + "'s turn.");
            applyEliteTurnPressure(foe);
            applyBossTurnPressure(foe);
            tickMonsterCooldowns(foe);
            MonsterAbility ability = chooseMonsterAbility(foe);
            if (ability != null) {
                scheduleMonsterAbility(foe, ability);
            } else {
                scheduleMonsterAttack(foe);
            }
            return;
        }
        enemyPhase = false;
        if (!finished) {
            activePartyIndex = firstLivingPartyIndex();
            beginActivePartyTurn();
        }
    }

    private void afterEnemyAction(Actor foe) {
        advanceStatusTurn(foe);
        if (checkFinished()) {
            enemyPhase = false;
            return;
        }
        continueEnemyPhase();
    }

    private void scheduleMonsterAttack(Actor foe) {
        Actor target = chooseMonsterTarget(foe, null);
        beginTelegraphedAnimatedAction(
                isBossEnemy(foe) ? "Boss Strike" : enemyRole(foe) == EnemyRole.SKIRMISHER ? "Quick Strike" : enemyRole(foe) == EnemyRole.BRUISER ? "Crushing Blow" : "Strike",
                isBossEnemy(foe) ? Math.max(2, bossPhase(foe)) : enemyRole(foe) == EnemyRole.BRUISER ? 2 : 1,
                "strike",
                foe,
                target,
                isBossEnemy(foe) ? 10 + bossPhase(foe) : enemyRole(foe) == EnemyRole.BRUISER ? 10 : 6,
                8,
                8,
                () -> resolveMonsterAttack(foe, target),
                () -> afterEnemyAction(foe)
        );
    }

    private void scheduleMonsterAbility(Actor foe, MonsterAbility ability) {
        cooldownsFor(foe).put(ability.name(), Math.max(1, ability.cooldown()));
        Actor target = ability.kind() == MonsterAbility.Kind.BUFF ? foe : chooseMonsterTarget(foe, ability);
        beginTelegraphedAnimatedAction(
                ability.name(),
                ability.kind() == MonsterAbility.Kind.BUFF ? 0 : isBossEnemy(foe) ? Math.max(2, bossPhase(foe)) : enemyRole(foe) == EnemyRole.HEXER ? 2 : 1,
                ability.effect(),
                foe,
                target,
                isBossEnemy(foe) ? 10 + bossPhase(foe) : enemyRole(foe) == EnemyRole.HEXER ? 11 : 8,
                ability.kind() == MonsterAbility.Kind.BUFF ? 1 : 11,
                10,
                () -> resolveMonsterAbility(foe, ability, target),
                () -> afterEnemyAction(foe)
        );
    }

    private void resolvePartyAttack(Actor actor, Actor target, AttackStyle style) {
        DamageStepResult step = resolvePartyAttackStep(actor, target, style);
        if (step.hit()) {
            if (style == AttackStyle.BASIC) {
                comboCounters.put(actor, Math.min(3, comboCounters.getOrDefault(actor, 0) + 1));
            } else {
                comboCounters.put(actor, 0);
            }
        }
    }

    private DamageStepResult resolvePartyAttackStep(Actor actor, Actor target, AttackStyle style) {
        if (!target.alive()) {
            return new DamageStepResult(0, false);
        }
        int raw = modifyOutgoingDamage(actor, actor.basicDamage(random));
        int combo = comboCounters.getOrDefault(actor, 0);
        if (style == AttackStyle.BASIC && combo >= 2) {
            raw = Math.max(1, (int) Math.round(raw * 1.28));
            applyStatus(target, "vulnerable", null);
        } else if (style == AttackStyle.HEAVY) {
            raw = Math.max(1, (int) Math.round(raw * 1.55)) + actor.strength / 3;
        } else if (style == AttackStyle.CLEAVE) {
            raw = Math.max(1, (int) Math.round(raw * 0.72));
        }
        DamageResult result = dealDamage(actor, target, raw, List.of("physical"), null, true);
        int damage = result.damage();
        addDamageFloater(target, result, new Color(255, 221, 221));
        applyEliteDamagedTrait(target, actor, null, result);
        addLog(attackMessage(actor, target, result));
        if (style == AttackStyle.HEAVY && damage > 0 && target.alive()) {
            applyStatus(target, "weak", null);
            addLog(target.name + " is staggered by the heavy blow.");
        }
        if (style == AttackStyle.BASIC && combo >= 2 && damage > 0) {
            comboCounters.put(actor, 0);
            floaters.add(new FloatingText("Combo", floaterX(target), floaterY(target) - 14, 30, new Color(255, 232, 150)));
            addLog(actor.name + " completes a three-hit combo.");
        }
        int force = style == AttackStyle.HEAVY ? 16 : style == AttackStyle.CLEAVE ? 12 : 10;
        triggerHitFeedback(target, result, force);
        playerLunge = Math.max(playerLunge, style == AttackStyle.HEAVY ? 24 : 18);
        flashTimer = Math.max(flashTimer, style == AttackStyle.HEAVY ? 6 : 4);
        checkBossPhaseTransition(target);
        if (!target.alive()) {
            addLog(target.name + " falls.");
            clearStatuses(target);
            rememberDefeated(target);
        }
        return new DamageStepResult(damage, !result.dodged());
    }

    private void resolveMonsterAttack(Actor foe, Actor target) {
        if (!foe.alive() || !target.alive()) {
            return;
        }
        int raw = guardReducedRaw(target, modifyOutgoingDamage(foe, foe.basicDamage(random)));
        DamageResult result = dealDamage(foe, target, raw, List.of("physical"), null, true);
        int damage = applyGuardMastery(target, result.damage());
        result = result.withDamage(damage);
        addDamageFloater(target, result, new Color(255, 187, 187));
        addLog(attackMessage(foe, target, result));
        applyEliteDamageDealtTrait(foe, target, result);
        triggerHitFeedback(target, result, enemyRole(foe) == EnemyRole.BRUISER ? 13 : 8);
        setEnemyLunge(foe, 20);
        if (!target.alive()) {
            addLog(target.name + " falls.");
            clearStatuses(target);
        }
    }

    private void resolveMonsterAbility(Actor foe, MonsterAbility ability, Actor target) {
        if (!foe.alive() || target == null || !target.alive()) {
            return;
        }
        setEnemyLunge(foe, ability.kind() == MonsterAbility.Kind.DAMAGE ? 16 : 8);
        if (ability.kind() == MonsterAbility.Kind.BUFF) {
            boolean applied = applyMonsterAbilityStatuses(foe, ability, target);
            floaters.add(new FloatingText("Focus", floaterX(foe), floaterY(foe), 30, new Color(215, 230, 255)));
            addLog(foe.name + " uses " + ability.name() + "; " + (applied ? "power gathers." : "draws inward."));
            return;
        }
        int statBonus = Math.max(0, foe.strength / 3 + foe.intelligence / 4 + foe.willpower / 6);
        int raw = randomBetween(Math.max(1, ability.power() - 3), ability.power() + 5) + statBonus;
        raw = guardReducedRaw(target, modifyOutgoingDamage(foe, raw));
        DamageResult result = dealDamage(foe, target, raw, GameData.damageTypesForEffect(ability.effect()), null, true);
        int damage = applyGuardMastery(target, result.damage());
        result = result.withDamage(damage);
        addDamageFloater(target, result, new Color(255, 187, 187));
        addLog(foe.name + " uses " + ability.name() + "; " + damageDetail(target, result) + ".");
        applyEliteDamageDealtTrait(foe, target, result);
        triggerHitFeedback(target, result, enemyRole(foe) == EnemyRole.HEXER ? 12 : 10);
        flashTimer = Math.max(flashTimer, 2);
        applyMonsterAbilityStatuses(foe, ability, target);
        if (!target.alive()) {
            addLog(target.name + " falls.");
            clearStatuses(target);
        }
    }

    private void resolveItemUse(Actor actor, Actor target, String itemName, int heal, int mp) {
        if (!target.alive()) {
            return;
        }
        if (hasEquipmentEffect(actor, EquipmentEffectHooks.FIRST_POTION) && firstPotionSpent.add(actor)) {
            heal = (int) Math.round(heal * 1.35);
            mp = (int) Math.round(mp * 1.35);
            addLog(actor.name + "'s mercy charm strengthens the potion.");
        }
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
    }

    private void applyMpFlow(Actor actor) {
        int mpFlow = actor.skillRank("ether_flow") + actor.skillRank("grace_flow");
        if (mpFlow > 0) {
            actor.mp = Math.min(actor.maxMp, actor.mp + mpFlow);
        }
    }

    private void applyPostCastMomentum(Actor actor) {
        if (hasEquipmentEffect(actor, EquipmentEffectHooks.CAST_MOVEMENT)) {
            applyStatus(actor, "haste", null);
            addLog(actor.name + " moves lighter after the cast.");
        }
    }

    private MonsterAbility chooseMonsterAbility(Actor foe) {
        List<MonsterAbility> abilities = new ArrayList<>(MonsterAbilities.forMonster(monsterSpecFor(foe)));
        if (isEliteEnemy(foe)) {
            abilities.addAll(MonsterAbilities.eliteAbilitiesFor(monsterSpecFor(foe)));
        }
        if (isBossEnemy(foe)) {
            abilities.addAll(MonsterAbilities.bossAbilitiesFor(monsterSpecFor(foe), bossPhase(foe)));
        }
        if (abilities.isEmpty()) {
            return null;
        }
        double hpRatio = foe.hp / (double) Math.max(1, foe.maxHp);
        Map<String, EffectStack> bucket = statusBucket(foe);
        Map<String, Integer> cooldowns = cooldownsFor(foe);
        MonsterAbility bestAbility = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (MonsterAbility ability : abilities) {
            if (cooldowns.getOrDefault(ability.name(), 0) > 0) {
                continue;
            }
            Double hpThreshold = monsterHpThreshold(foe, ability);
            if (hpThreshold != null && hpRatio > hpThreshold) {
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
            if (hpThreshold != null && hpRatio <= hpThreshold) {
                chance += 0.18 + (hpThreshold - hpRatio) * 0.40;
            } else if (ability.kind() == MonsterAbility.Kind.BUFF && hpRatio <= 0.35) {
                chance += 0.10;
            }
            if (random.nextDouble() < Math.min(0.95, chance)) {
                double score = monsterAbilityScore(foe, ability, hpRatio, hpThreshold);
                if (score > bestScore) {
                    bestScore = score;
                    bestAbility = ability;
                }
            }
        }
        return bestAbility;
    }

    private double monsterAbilityScore(Actor foe, MonsterAbility ability, double hpRatio, Double hpThreshold) {
        double score = ability.chance() * 2.0 + random.nextDouble() * 0.35;
        EnemyRole role = enemyRole(foe);
        if (hpThreshold != null) {
            score += Math.max(0.0, hpThreshold - hpRatio) * 4.0 + 0.65;
        }
        if (ability.kind() == MonsterAbility.Kind.BUFF) {
            if (role == EnemyRole.GUARD) {
                score += 0.9;
            }
            for (MonsterAbilityStatus status : ability.statuses()) {
                score += switch (status.key()) {
                    case "shield" -> 1.4 + (1.0 - hpRatio) * 2.2;
                    case "regeneration" -> 1.2 + (1.0 - hpRatio) * 2.0;
                    case "fortified" -> 1.25;
                    case "haste" -> 1.1 + (hpRatio > 0.55 ? 0.45 : 0.0);
                    default -> 0.6;
                };
            }
            return score;
        }
        Actor target = chooseMonsterTarget(foe, ability);
        score += ability.power() / 15.0;
        if (role == EnemyRole.HEXER && !ability.statuses().isEmpty()) {
            score += 1.0;
        } else if (role == EnemyRole.BRUISER && ability.power() >= 10) {
            score += 0.7;
        }
        if (target != null) {
            score += monsterTargetScore(foe, target, ability);
            if (target.hp <= likelyMonsterDamage(foe, ability, target)) {
                score += 3.0;
            }
        }
        return score;
    }

    private Actor chooseMonsterTarget(Actor foe, MonsterAbility ability) {
        List<Actor> living = livingParty();
        if (living.isEmpty()) {
            return null;
        }
        return living.stream()
                .max(Comparator.comparingDouble(target -> monsterTargetScore(foe, target, ability) + random.nextDouble() * 0.25))
                .orElse(living.get(0));
    }

    private double monsterTargetScore(Actor foe, Actor target, MonsterAbility ability) {
        double hpRatio = target.hp / (double) Math.max(1, target.maxHp);
        double score = (1.0 - hpRatio) * 3.0;
        if (target == player) {
            score += 0.35;
        }
        Map<String, EffectStack> targetStatuses = statusBucket(target);
        EnemyRole role = enemyRole(foe);
        if (role == EnemyRole.SKIRMISHER) {
            score += (1.0 - hpRatio) * 1.2;
            if (!isGuarding(target)) {
                score += 0.55;
            }
        } else if (role == EnemyRole.BRUISER && target.maxHp >= player.maxHp) {
            score += 0.35;
        } else if (role == EnemyRole.HEXER && ability != null && !ability.statuses().isEmpty()) {
            score += targetStatusesMissingValue(target, ability);
        }
        EliteTrait trait = eliteTrait(foe);
        if (trait == EliteTrait.EXECUTIONER) {
            score += (1.0 - hpRatio) * 2.0;
        } else if (trait == EliteTrait.HEXBOUND && ability != null && !ability.statuses().isEmpty()) {
            score += targetStatusesMissingValue(target, ability) * 0.8;
        }
        if (targetStatuses.containsKey("vulnerable")) {
            score += 0.9;
        }
        if (targetStatuses.containsKey("weak")) {
            score -= 0.35;
        }
        if (isGuarding(target)) {
            score -= ability == null ? 0.65 : 0.25;
        }
        if (target.hp <= likelyMonsterDamage(foe, ability, target)) {
            score += 4.0;
        }
        if (ability != null) {
            for (MonsterAbilityStatus status : ability.statuses()) {
                if (!"self".equals(status.target()) && !targetStatuses.containsKey(status.key())) {
                    score += switch (status.key()) {
                        case "weak", "vulnerable" -> 0.85;
                        case "poison", "burn" -> 0.7;
                        default -> 0.35;
                    };
                }
            }
        }
        return score;
    }

    private double targetStatusesMissingValue(Actor target, MonsterAbility ability) {
        Map<String, EffectStack> targetStatuses = statusBucket(target);
        double score = 0.0;
        for (MonsterAbilityStatus status : ability.statuses()) {
            if (!"self".equals(status.target()) && !targetStatuses.containsKey(status.key())) {
                score += 0.35;
            }
        }
        return score;
    }

    private int likelyMonsterDamage(Actor foe, MonsterAbility ability, Actor target) {
        int raw = ability == null ? foe.attack + 2 : ability.power();
        raw = modifyOutgoingDamage(foe, Math.max(1, raw));
        if (isGuarding(target)) {
            raw = Math.max(1, raw - 8);
        }
        Map<String, EffectStack> targetStatuses = statusBucket(target);
        if (targetStatuses.containsKey("vulnerable")) {
            raw = (int) (raw * 1.25);
        }
        if (targetStatuses.containsKey("fortified")) {
            raw = (int) (raw * 0.75);
        }
        if (targetStatuses.containsKey("shield")) {
            raw = Math.max(1, raw - targetStatuses.get("shield").power);
        }
        return Math.max(1, raw - target.defense);
    }

    private Double monsterHpThreshold(Actor foe, MonsterAbility ability) {
        if (ability.hpBelow() == null) {
            return null;
        }
        Map<String, Double> thresholds = monsterHpThresholds.computeIfAbsent(foe, key -> new HashMap<>());
        return thresholds.computeIfAbsent(ability.name(), key -> {
            double spread = 0.07 + Math.min(0.06, monsterSpecFor(foe).attack() / 250.0);
            double rolled = ability.hpBelow() + (random.nextDouble() * spread * 2.0) - spread;
            return Math.max(0.18, Math.min(0.85, rolled));
        });
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
        for (AbilityStatus hint : GameData.statusHintsForAbility(ability)) {
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
        if (actor != player && partyMembers.contains(actor)) {
            modified += player.skillRank("shared_training") + player.skillRank("pack_coordination");
        }
        if (bucket.containsKey("weak")) {
            modified = (int) (modified * 0.70);
        }
        if (bucket.containsKey("haste")) {
            modified = (int) (modified * 1.55);
        }
        if (enemies.contains(actor)) {
            EnemyRole role = enemyRole(actor);
            if (role == EnemyRole.BRUISER) {
                modified = (int) Math.round(modified * 1.14);
            } else if (role == EnemyRole.SWARMER && livingEnemies().size() >= 3) {
                modified = (int) Math.round(modified * 1.10);
            } else if (role == EnemyRole.GUARD) {
                modified = (int) Math.round(modified * 0.92);
            }
            if (isBossEnemy(actor)) {
                modified = (int) Math.round(modified * (1.0 + Math.max(0, bossPhase(actor) - 1) * 0.08));
            }
            EliteTrait trait = eliteTrait(actor);
            if (trait == EliteTrait.FRENZIED && actor.hp * 100 <= actor.maxHp * 70) {
                modified = (int) Math.round(modified * 1.18);
            } else if (trait == EliteTrait.EXECUTIONER && livingParty().stream().anyMatch(target -> target.hp * 2 <= target.maxHp)) {
                modified = (int) Math.round(modified * 1.12);
            }
        }
        return Math.max(1, modified);
    }

    private int modifyAbilityDamage(Actor actor, Actor target, Ability ability, int raw) {
        Map<String, EffectStack> bucket = statusBucket(target);
        int modified = raw;
        if (ability.hasTag("armor_breaker")) {
            modified += Math.max(1, target.defense * 2);
        }
        if (ability.hasTag("execute") && target.hp <= Math.max(1, target.maxHp * 35 / 100)) {
            modified = (int) Math.round(modified * 1.45);
        }
        if (ability.hasTag("poison_combo") && bucket.containsKey("poison")) {
            modified = (int) Math.round(modified * 1.22);
        }
        if (ability.hasTag("consume_vulnerable") && bucket.remove("vulnerable") != null) {
            modified = (int) Math.round(modified * 1.35);
            addLog(actor.name + " consumes the exposed opening.");
        }
        if (bucket.isEmpty()) {
            statuses.remove(target);
        }
        return Math.max(1, modified);
    }

    private boolean cleanseNegativeStatuses(Actor actor) {
        Map<String, EffectStack> bucket = statuses.get(actor);
        if (bucket == null || bucket.isEmpty()) {
            return false;
        }
        boolean removed = false;
        for (String key : List.of("poison", "burn", "weak", "vulnerable")) {
            removed |= bucket.remove(key) != null;
        }
        if (bucket.isEmpty()) {
            statuses.remove(actor);
        }
        return removed;
    }

    private void addDamageFloater(Actor target, DamageResult result, Color damageColor) {
        String text = result.dodged() ? "Dodge" : result.parried() ? "Parry -" + result.damage() : "-" + result.damage();
        if (!result.dodged() && !result.parried()) {
            if (result.typeMultiplier() > 1.05) {
                text = "Weak " + text;
            } else if (result.typeMultiplier() < 0.95) {
                text = "Resist " + text;
            }
        }
        if (result.critical() && !result.dodged()) {
            text = "Crit " + text;
        }
        Color color = result.dodged() || result.parried() ? new Color(200, 225, 255) : damageColor;
        floaters.add(new FloatingText(text, floaterX(target), floaterY(target), 34, color));
    }

    private void triggerHitFeedback(Actor target, DamageResult result, int force) {
        if (result.dodged()) {
            shake = Math.max(shake, Math.max(3, force / 3));
            return;
        }
        int scaledForce = result.critical() ? force + 6 : result.parried() ? Math.max(4, force - 3) : force;
        shake = Math.max(shake, scaledForce);
        hitFlashes.put(target, result.critical() ? 10 : 7);
        if (result.critical()) {
            floaters.add(new FloatingText("CRIT", floaterX(target), floaterY(target) - 18, 30, new Color(255, 237, 150)));
        }
    }

    private String attackMessage(Actor attacker, Actor target, DamageResult result) {
        String source = attacker == player ? "You" : attacker.name;
        if (result.dodged()) {
            return target.name + " dodges " + (attacker == player ? "your strike" : attacker.name + "'s strike") + ".";
        }
        return source + " " + (attacker == player ? "hit " : "strikes ") + target.name + " for "
                + result.damage() + (result.critical() ? " critical" : "") + (result.parried() ? " after a parry" : "")
                + damageTypeNote(result) + ".";
    }

    private String damageDetail(Actor target, DamageResult result) {
        if (result.dodged()) {
            return target.name + " dodges";
        }
        return target.name + " takes " + result.damage()
                + (result.critical() ? " critical" : "")
                + (result.parried() ? " after a parry" : "")
                + damageTypeNote(result);
    }

    private DamageResult dealDamage(Actor attacker, Actor target, int raw, Ability ability, boolean allowAvoidance) {
        return dealDamage(attacker, target, raw, GameData.damageTypesForAbility(ability), ability, allowAvoidance);
    }

    private DamageResult dealDamage(Actor attacker, Actor target, int raw, List<String> damageTypes, Ability ability, boolean allowAvoidance) {
        Map<String, EffectStack> bucket = statusBucket(target);
        if (allowAvoidance) {
            double dodgeChance = target.dodgeChanceAgainst(attacker);
            if (bucket.containsKey("evasive")) {
                dodgeChance += Math.min(0.45, bucket.get("evasive").power / 100.0);
            }
            if (enemies.contains(target) && enemyRole(target) == EnemyRole.SKIRMISHER) {
                dodgeChance += 0.08;
            }
            if (random.nextDouble() < Math.min(0.70, dodgeChance)) {
                bucket.remove("evasive");
                return new DamageResult(0, true, false, false, GameData.normalizeDamageTypes(damageTypes), 1.0, 1.0);
            }
        }
        int modified = raw;
        if (bucket.containsKey("evasive")) {
            modified = Math.max(1, (int) Math.round(modified * 0.65));
            bucket.remove("evasive");
        }
        boolean parried = false;
        if (allowAvoidance && random.nextDouble() < target.parryChanceAgainst(attacker)) {
            modified = Math.max(1, (int) Math.round(modified * 0.55));
            parried = true;
        }
        if (ability == null && bucket.containsKey("weak") && hasEquipmentEffect(attacker, EquipmentEffectHooks.BASIC_ATTACK_WEAK)) {
            modified = Math.max(1, (int) Math.round(modified * 1.18));
        }
        String damageMode = damageModeFor(ability);
        if (hasEquipmentEffect(attacker, EquipmentEffectHooks.ALTERNATING_DAMAGE)) {
            String previousMode = alternatingDamageMode.put(attacker, damageMode);
            if (previousMode != null && !previousMode.equals(damageMode)) {
                modified = Math.max(1, (int) Math.round(modified * 1.15));
                addLog(attacker.name + "'s worldroot rhythm empowers the " + damageMode + " strike.");
            }
        }
        boolean critical = false;
        double criticalChance = attacker.criticalChanceAgainst(target, ability);
        if (bucket.containsKey("vulnerable") && hasEquipmentEffect(attacker, EquipmentEffectHooks.VULNERABLE_CRIT)) {
            criticalChance += 0.10;
        }
        if (random.nextDouble() < criticalChance) {
            modified = Math.max(1, (int) Math.round(modified * attacker.criticalMultiplier(ability)));
            critical = true;
            if (hasEquipmentEffect(attacker, EquipmentEffectHooks.FIRST_CRITICAL) && firstCriticalSpent.add(attacker)) {
                modified = Math.max(1, (int) Math.round(modified * 1.35));
                addLog(attacker.name + "'s bell answers the first critical strike.");
            }
            if (hasEquipmentEffect(attacker, EquipmentEffectHooks.CRITICAL_SLOW)) {
                applyStatus(target, "weak", null);
                addLog(target.name + " is staggered by the legendary critical hit.");
            }
        }
        if (bucket.containsKey("vulnerable")) {
            modified = (int) (modified * 1.25);
        }
        if (bucket.containsKey("weak") && attacker.skillRank("frostbite") > 0) {
            modified = (int) (modified * (1.0 + attacker.skillRank("frostbite") * 0.05));
        }
        double typeMultiplier = enemies.contains(target)
                ? GameData.monsterDamageMultiplier(monsterSpecFor(target), damageTypes)
                : 1.0;
        modified = Math.max(1, (int) Math.round(modified * typeMultiplier));
        double knowledgeMultiplier = knowledgeDamageMultiplier(attacker, target);
        modified = Math.max(1, (int) Math.round(modified * knowledgeMultiplier));
        if (bucket.containsKey("fortified")) {
            modified = (int) (modified * 0.75);
        }
        if (enemies.contains(target) && enemyRole(target) == EnemyRole.GUARD) {
            modified = (int) Math.round(modified * 0.88);
        }
        if (bucket.containsKey("shield")) {
            modified = Math.max(1, modified - bucket.get("shield").power);
        }
        if (target.skillRank("unyielding") > 0 && target.hp * 2 <= target.maxHp) {
            modified = Math.max(1, modified - target.skillRank("unyielding") * 2);
        }
        if (enemies.contains(target) && !enemies.contains(attacker)) {
            rememberDiscoveredVulnerabilities(target, damageTypes);
        }
        if (target.previewDamage(modified) >= target.hp
                && (hasEquipmentEffect(target, EquipmentEffectHooks.LETHAL_SAVE) || hasEquipmentEffect(target, EquipmentEffectHooks.DESPERATE_VICTORY))
                && lethalSaveSpent.add(target)) {
            int before = target.hp;
            target.hp = 1;
            addLog(target.name + "'s legendary charm refuses the killing blow.");
            return new DamageResult(Math.max(0, before - target.hp), false, parried, critical,
                    GameData.normalizeDamageTypes(damageTypes), typeMultiplier, knowledgeMultiplier);
        }
        return new DamageResult(target.takeDamage(modified), false, parried, critical,
                GameData.normalizeDamageTypes(damageTypes), typeMultiplier, knowledgeMultiplier);
    }

    private String damageModeFor(Ability ability) {
        return isSpellAbility(ability) ? "spell" : "weapon";
    }

    private boolean isSpellAbility(Ability ability) {
        if (ability == null) {
            return false;
        }
        return switch (ability.scaling()) {
            case ARCANE, DIVINE, NATURE, TRIAGE -> true;
            case WEAPON, AGILITY, GUARD -> false;
        };
    }

    private boolean hasEquipmentEffect(Actor actor, String fragment) {
        return actor != null && actor.hasEquipmentEffect(fragment);
    }

    private double knowledgeDamageMultiplier(Actor attacker, Actor target) {
        if (!enemies.contains(target) || enemies.contains(attacker)) {
            return 1.0;
        }
        GameData.MonsterSpec spec = monsterSpecFor(target);
        int defeatedCount = knownMonsterInsights.getOrDefault(spec.key(), 0);
        return GameData.monsterKnowledgeDamageMultiplier(defeatedCount, player.intelligence);
    }

    private void rememberDiscoveredVulnerabilities(Actor target, List<String> damageTypes) {
        GameData.MonsterSpec spec = monsterSpecFor(target);
        List<String> matches = GameData.matchingVulnerabilities(spec, damageTypes);
        if (matches.isEmpty()) {
            return;
        }
        Set<String> alreadyKnown = knownMonsterVulnerabilities.getOrDefault(spec.key(), Set.of());
        List<String> known = discoveredMonsterVulnerabilities.computeIfAbsent(spec.key(), key -> new ArrayList<>());
        List<String> newlyLearned = new ArrayList<>();
        for (String type : matches) {
            if (alreadyKnown.contains(type)) {
                continue;
            }
            if (!known.contains(type)) {
                known.add(type);
                newlyLearned.add(type);
            }
        }
        if (!newlyLearned.isEmpty()) {
            addLog("You note " + spec.name() + " is vulnerable to " + GameData.damageTypeListLabel(newlyLearned) + ".");
        }
    }

    private String damageTypeNote(DamageResult result) {
        if (result.dodged() || result.damageTypes().isEmpty()) {
            return "";
        }
        String label = GameData.damageTypeListLabel(result.damageTypes());
        if (result.typeMultiplier() > 1.05) {
            return " (" + label + " bites deep)";
        }
        if (result.typeMultiplier() < 0.95) {
            return " (" + label + " is resisted)";
        }
        return "";
    }

    private record DamageResult(
            int damage,
            boolean dodged,
            boolean parried,
            boolean critical,
            List<String> damageTypes,
            double typeMultiplier,
            double knowledgeMultiplier
    ) {
        DamageResult withDamage(int nextDamage) {
            return new DamageResult(nextDamage, dodged, parried, critical, damageTypes, typeMultiplier, knowledgeMultiplier);
        }
    }

    private int guardReducedRaw(Actor target, int raw) {
        if (isGuarding(target)) {
            addLog(target.name + "'s guard absorbs part of the strike.");
            setGuard(target, false);
            if (hasEquipmentEffect(target, EquipmentEffectHooks.GUARD_RESOLVE)) {
                int resolve = Math.max(1, raw / 6);
                int before = target.mp;
                target.mp = Math.min(target.maxMp, target.mp + resolve);
                if (target.mp > before) {
                    addLog(target.name + " turns guard pressure into +" + (target.mp - before) + " MP.");
                }
            }
            return Math.max(1, raw - 8);
        }
        return raw;
    }

    private int applyGuardMastery(Actor target, int damage) {
        int reduction = Math.min(Math.max(0, damage - 1), target.skillRank("guard_mastery"));
        if (reduction <= 0) {
            return damage;
        }
        target.hp = Math.min(target.maxHp, target.hp + reduction);
        return damage - reduction;
    }

    private int applyDirectDamage(Actor actor, int amount) {
        int before = actor.hp;
        actor.hp = Math.max(0, actor.hp - Math.max(0, amount));
        return Math.max(0, before - actor.hp);
    }

    private void scaleEnemyForBattle(Actor foe, GameData.MonsterSpec spec, int partySize, int encounterSize) {
        int baseLevelPressure = Math.max(0, player.level - 1);
        double levelPressure = baseLevelPressure * monsterLevelScaling;
        int partyPressure = Math.max(0, partySize - 1);
        if (levelPressure <= 0.0 && partyPressure <= 0) {
            return;
        }
        int encounterPressure = Math.max(0, encounterSize - 1);
        double hpScale = 1.0 + levelPressure * 0.075 + partyPressure * 0.18 + Math.max(0, encounterPressure - 1) * 0.035;
        double attackScale = 1.0 + levelPressure * 0.045 + partyPressure * 0.10;
        double defenseScale = 1.0 + levelPressure * 0.035 + partyPressure * 0.08;
        int hpBonus = (int) Math.round(levelPressure * (3 + Math.max(1, spec.defense()))) + partyPressure * Math.max(4, spec.hp() / 8);
        foe.maxHp = Math.max(1, (int) Math.round(foe.maxHp * hpScale) + hpBonus);
        foe.hp = foe.maxHp;
        foe.attack = Math.max(1, (int) Math.round(foe.attack * attackScale) + (int) Math.floor(levelPressure / 3.0) + partyPressure);
        foe.defense = Math.max(0, (int) Math.round(foe.defense * defenseScale) + (int) Math.floor(levelPressure / 5.0));
        foe.strength = Math.max(1, (int) Math.round(foe.strength * attackScale) + (int) Math.floor(levelPressure / 4.0));
        foe.intelligence = Math.max(1, foe.intelligence + (int) Math.floor(levelPressure / 5.0));
        foe.dexterity = Math.max(1, (int) Math.round(foe.dexterity * (1.0 + levelPressure * 0.025)));
        foe.charisma = Math.max(1, foe.charisma + (int) Math.floor(levelPressure / 6.0));
        foe.increaseStat("constitution", Math.max(0, (int) Math.floor(levelPressure / 5.0)));
        int scaledWillpower = Math.max(1, (int) Math.round(foe.willpower * defenseScale) + (int) Math.floor(levelPressure / 5.0));
        foe.increaseStat("willpower", scaledWillpower - foe.willpower);
    }

    private boolean rollEliteEnemy(int encounterSize) {
        double levelPressure = Math.max(0, player.level - 1) * monsterLevelScaling;
        double chance = 0.10 + levelPressure * 0.012 + Math.max(0, encounterSize - 1) * 0.015;
        return random.nextDouble() < Math.min(0.22, chance);
    }

    private Actor createEliteActor(GameData.MonsterSpec spec) {
        return new Actor("Elite " + spec.name(), spec.sprite(), "Monster", spec.hp(), 0, spec.attack(), spec.defense());
    }

    private void scaleEliteEnemy(Actor foe) {
        foe.maxHp = Math.max(1, (int) Math.round(foe.maxHp * 1.45) + 8);
        foe.hp = foe.maxHp;
        foe.attack = Math.max(1, (int) Math.round(foe.attack * 1.22) + 2);
        foe.defense = Math.max(0, (int) Math.round(foe.defense * 1.18) + 1);
        foe.strength = Math.max(1, (int) Math.round(foe.strength * 1.18) + 1);
        foe.intelligence = Math.max(1, (int) Math.round(foe.intelligence * 1.12));
        foe.dexterity = Math.max(1, (int) Math.round(foe.dexterity * 1.12));
        foe.charisma = Math.max(1, (int) Math.round(foe.charisma * 1.10));
        foe.increaseStat("constitution", Math.max(1, (int) Math.round(foe.constitution * 0.16)));
        int eliteWillpower = Math.max(1, (int) Math.round(foe.willpower * 1.16) + 1);
        foe.increaseStat("willpower", eliteWillpower - foe.willpower);
    }

    private EnemyRole chooseEnemyRole(GameData.MonsterSpec spec, Actor foe) {
        String key = spec.key();
        String name = spec.name().toLowerCase();
        if (key.contains("shield") || key.contains("guard") || key.contains("golem")
                || key.contains("tortoise") || key.contains("giant")) {
            return EnemyRole.GUARD;
        }
        if (key.contains("shaman") || key.contains("wraith") || key.contains("imp")
                || key.contains("drake") || name.contains("caster")) {
            return EnemyRole.HEXER;
        }
        if (key.contains("archer") || key.contains("scout") || key.contains("stalker")
                || key.contains("lynx") || key.contains("wolf") || key.contains("bat")
                || key.contains("cutthroat")) {
            return EnemyRole.SKIRMISHER;
        }
        if (key.contains("slime") || key.contains("spider") || key.contains("skeleton")
                || key.contains("thornling") || foe.maxHp <= 28) {
            return EnemyRole.SWARMER;
        }
        return EnemyRole.BRUISER;
    }

    private EliteTrait chooseEliteTrait(GameData.MonsterSpec spec, EnemyRole role) {
        List<EliteTrait> weighted = new ArrayList<>();
        weighted.add(EliteTrait.FRENZIED);
        weighted.add(EliteTrait.VAMPIRIC);
        weighted.add(EliteTrait.VOLATILE);
        weighted.add(EliteTrait.HEXBOUND);
        weighted.add(EliteTrait.EXECUTIONER);
        weighted.add(EliteTrait.MIRROR);
        switch (role) {
            case GUARD -> {
                weighted.add(EliteTrait.BULWARK);
                weighted.add(EliteTrait.BULWARK);
                weighted.add(EliteTrait.COMMANDER);
            }
            case HEXER -> {
                weighted.add(EliteTrait.HEXBOUND);
                weighted.add(EliteTrait.HEXBOUND);
                weighted.add(EliteTrait.MIRROR);
            }
            case SKIRMISHER -> {
                weighted.add(EliteTrait.EXECUTIONER);
                weighted.add(EliteTrait.FRENZIED);
            }
            case SWARMER -> {
                weighted.add(EliteTrait.COMMANDER);
                weighted.add(EliteTrait.VOLATILE);
            }
            case BRUISER -> {
                weighted.add(EliteTrait.FRENZIED);
                weighted.add(EliteTrait.VAMPIRIC);
            }
        }
        if ("undead".equals(spec.species()) || "demon".equals(spec.species())) {
            weighted.add(EliteTrait.HEXBOUND);
            weighted.add(EliteTrait.VAMPIRIC);
        }
        if ("dragon".equals(spec.species()) || "elemental".equals(spec.species())) {
            weighted.add(EliteTrait.VOLATILE);
            weighted.add(EliteTrait.MIRROR);
        }
        return weighted.get(random.nextInt(weighted.size()));
    }

    private void applyEliteTraitOpening(Actor foe, EliteTrait trait) {
        switch (trait) {
            case BULWARK -> {
                applyStatus(foe, "shield", Math.max(14, foe.defense + 12));
                applyStatus(foe, "fortified", null);
            }
            case FRENZIED -> applyStatus(foe, "haste", null);
            case HEXBOUND -> applyStatus(foe, "shield", Math.max(8, foe.willpower + 4));
            case MIRROR -> applyStatus(foe, "shield", Math.max(10, foe.intelligence + 5));
            case COMMANDER -> applyStatus(foe, "fortified", null);
            case VAMPIRIC, VOLATILE, EXECUTIONER -> {
            }
        }
    }

    private void applyEnemyRoleStats(Actor foe, EnemyRole role) {
        switch (role) {
            case BRUISER -> {
                foe.attack = Math.max(1, foe.attack + 2);
                foe.strength = Math.max(1, foe.strength + 3);
                foe.maxHp += 6;
                foe.hp = foe.maxHp;
            }
            case GUARD -> {
                foe.defense = Math.max(0, foe.defense + 3);
                foe.increaseStat("constitution", 2);
                applyStatus(foe, "shield", Math.max(8, foe.defense + 5));
                applyStatus(foe, "fortified", null);
            }
            case SKIRMISHER -> {
                foe.dexterity = Math.max(1, foe.dexterity + 5);
                foe.attack = Math.max(1, foe.attack + 1);
            }
            case HEXER -> {
                foe.intelligence = Math.max(1, foe.intelligence + 4);
                foe.increaseStat("willpower", 3);
            }
            case SWARMER -> {
                foe.dexterity = Math.max(1, foe.dexterity + 2);
                foe.attack = Math.max(1, foe.attack + Math.max(1, livingEnemies().size() / 2));
            }
        }
    }

    private boolean isBossSpec(GameData.MonsterSpec spec) {
        String key = spec.key();
        return key.equals("goblin_king")
                || key.equals("red_dragon")
                || key.equals("elder_dragon")
                || key.equals("demon_queen")
                || key.equals("vaelthara")
                || key.contains("_banner_bound")
                || key.contains("_bell_drowned")
                || key.contains("_cinder_knife")
                || key.contains("_mercy_taker")
                || spec.name().contains(",")
                || "demon".equals(spec.species()) && spec.hp() >= 165;
    }

    private void applyBossOpeningPresence(Actor foe) {
        foe.maxHp = Math.max(1, (int) Math.round(foe.maxHp * 1.12) + 8);
        foe.hp = foe.maxHp;
        applyStatus(foe, "shield", Math.max(10, foe.defense + 8));
    }

    private void checkBossPhaseTransition(Actor foe) {
        if (!isBossEnemy(foe) || !foe.alive()) {
            return;
        }
        int current = bossPhase(foe);
        double hpRatio = foe.hp / (double) Math.max(1, foe.maxHp);
        int next = hpRatio <= 0.35 ? 3 : hpRatio <= 0.70 ? 2 : 1;
        if (next <= current) {
            return;
        }
        bossPhases.put(foe, next);
        cleanseNegativeStatuses(foe);
        applyStatus(foe, "shield", Math.max(12, foe.defense + next * 7));
        applyStatus(foe, "fortified", null);
        if (next >= 2) {
            applyStatus(foe, "haste", null);
        }
        if (next >= 3) {
            applyStatus(foe, "regeneration", Math.max(5, foe.maxHp / 32));
            Actor target = chooseMonsterTarget(foe, null);
            if (target != null) {
                applyStatus(target, "vulnerable", null);
                floaters.add(new FloatingText("Marked", floaterX(target), floaterY(target), 30, new Color(255, 162, 150)));
            }
        }
        hitFlashes.put(foe, 14);
        shake = Math.max(shake, next >= 3 ? 20 : 15);
        flashTimer = Math.max(flashTimer, 7);
        floaters.add(new FloatingText("Phase " + next, floaterX(foe), floaterY(foe) - 18, 36, new Color(255, 222, 132)));
        addLog(foe.name + " enters Phase " + next + ".");
    }

    private void applyEliteTurnPressure(Actor foe) {
        if (!isEliteEnemy(foe) || !foe.alive()) {
            return;
        }
        applyEliteTraitTurnPressure(foe);
        if (foe.hp * 2 <= foe.maxHp && eliteSecondWindSpent.add(foe)) {
            cleanseNegativeStatuses(foe);
            applyStatus(foe, "shield", Math.max(10, foe.defense + 8));
            applyStatus(foe, "haste", null);
            hitFlashes.put(foe, 10);
            floaters.add(new FloatingText("Second Wind", floaterX(foe), floaterY(foe), 30, new Color(255, 224, 156)));
            addLog(foe.name + " digs in with elite second wind.");
            return;
        }
        if (random.nextDouble() < 0.28 && statusBucket(foe).get("shield") == null) {
            applyStatus(foe, "shield", Math.max(6, foe.defense + 4));
            addLog(foe.name + " reads the field and tightens guard.");
        }
    }

    private void applyEliteTraitTurnPressure(Actor foe) {
        EliteTrait trait = eliteTrait(foe);
        if (trait == null) {
            return;
        }
        switch (trait) {
            case FRENZIED -> {
                if (foe.hp * 100 <= foe.maxHp * 70) {
                    applyStatus(foe, "haste", null);
                    if (random.nextDouble() < 0.35) {
                        addLog(foe.name + "'s frenzy sharpens.");
                    }
                }
            }
            case BULWARK -> {
                for (Actor ally : livingEnemies()) {
                    if (ally != foe && random.nextDouble() < 0.55) {
                        applyStatus(ally, "shield", Math.max(5, foe.defense + 3));
                    }
                }
            }
            case HEXBOUND -> {
                Actor target = chooseMonsterTarget(foe, null);
                if (target != null && random.nextDouble() < 0.45) {
                    applyStatus(target, random.nextBoolean() ? "weak" : "vulnerable", null);
                    addLog(foe.name + "'s hex clings to " + target.name + ".");
                }
            }
            case COMMANDER -> {
                for (Actor ally : livingEnemies()) {
                    if (ally != foe) {
                        applyStatus(ally, random.nextBoolean() ? "haste" : "fortified", null);
                    }
                }
                if (livingEnemies().size() > 1) {
                    addLog(foe.name + " commands the enemy line.");
                }
            }
            case EXECUTIONER -> {
                Actor target = livingParty().stream()
                        .min(Comparator.comparingInt(actor -> actor.hp))
                        .orElse(null);
                if (target != null && target.hp * 2 <= target.maxHp && random.nextDouble() < 0.45) {
                    applyStatus(target, "vulnerable", null);
                    addLog(foe.name + " marks wounded " + target.name + " for execution.");
                }
            }
            case MIRROR -> {
                if (statusBucket(foe).get("shield") == null && random.nextDouble() < 0.40) {
                    applyStatus(foe, "shield", Math.max(7, foe.intelligence + 4));
                }
            }
            case VAMPIRIC, VOLATILE -> {
            }
        }
    }

    private void applyEliteDamageDealtTrait(Actor foe, Actor target, DamageResult result) {
        EliteTrait trait = eliteTrait(foe);
        if (trait == EliteTrait.VAMPIRIC && result.damage() > 0 && foe.alive()) {
            int healed = Math.max(1, result.damage() / 4);
            int before = foe.hp;
            foe.hp = Math.min(foe.maxHp, foe.hp + healed);
            int gained = foe.hp - before;
            if (gained > 0) {
                floaters.add(new FloatingText("Leech +" + gained, floaterX(foe), floaterY(foe), 30, new Color(205, 102, 132)));
                addLog(foe.name + " drinks strength from the hit.");
            }
        }
        if (trait == EliteTrait.EXECUTIONER && result.damage() > 0 && target.alive() && target.hp * 2 <= target.maxHp) {
            applyStatus(target, "weak", null);
        }
    }

    private void applyEliteDamagedTrait(Actor target, Actor attacker, Ability ability, DamageResult result) {
        EliteTrait trait = eliteTrait(target);
        if (trait == null || result.dodged() || result.damage() <= 0 || attacker == null || !attacker.alive()) {
            return;
        }
        if (trait == EliteTrait.MIRROR && ability != null && isSpellAbility(ability)) {
            int reflected = applyDirectDamage(attacker, Math.max(1, result.damage() / 5));
            if (reflected > 0) {
                floaters.add(new FloatingText("Mirror " + reflected, floaterX(attacker), floaterY(attacker), 30, new Color(190, 220, 255)));
                addLog(target.name + "'s mirror trait reflects " + reflected + " damage.");
            }
        } else if (trait == EliteTrait.BULWARK && random.nextDouble() < 0.28) {
            applyStatus(target, "fortified", null);
        } else if (trait == EliteTrait.FRENZIED && target.alive() && target.hp * 100 <= target.maxHp * 70) {
            applyStatus(target, "haste", null);
        }
    }

    private void applyBossTurnPressure(Actor foe) {
        if (!isBossEnemy(foe) || !foe.alive()) {
            return;
        }
        int phase = bossPhase(foe);
        if (phase >= 2 && statusBucket(foe).get("shield") == null) {
            applyStatus(foe, "shield", Math.max(8, foe.defense + phase * 4));
        }
        if (phase >= 3) {
            applyStatus(foe, "haste", null);
            Actor target = chooseMonsterTarget(foe, null);
            if (target != null && random.nextDouble() < 0.38) {
                applyStatus(target, "weak", null);
                floaters.add(new FloatingText("Dread", floaterX(target), floaterY(target) - 12, 30, new Color(205, 184, 255)));
                addLog(foe.name + "'s final phase pressure rattles " + target.name + ".");
            }
        }
    }

    private String enemyRoleSummary() {
        Map<EnemyRole, Integer> counts = new java.util.EnumMap<>(EnemyRole.class);
        for (Actor foe : enemies) {
            counts.merge(enemyRole(foe), 1, Integer::sum);
        }
        List<String> labels = new ArrayList<>();
        for (Map.Entry<EnemyRole, Integer> entry : counts.entrySet()) {
            labels.add(entry.getValue() == 1 ? entry.getKey().label() : entry.getValue() + " " + entry.getKey().label() + "s");
        }
        return "Enemy roles: " + String.join(", ", labels) + ".";
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

    public int abilityCooldownRemaining(Actor actor, Ability ability) {
        if (actor == null || ability == null || ability.cooldown() <= 0) {
            return 0;
        }
        return partyCooldowns.getOrDefault(actor, Map.of()).getOrDefault(ability.name(), 0);
    }

    private void startAbilityCooldown(Actor actor, Ability ability) {
        if (actor == null || ability == null || ability.cooldown() <= 0) {
            return;
        }
        partyCooldowns.computeIfAbsent(actor, key -> new HashMap<>()).put(ability.name(), ability.cooldown());
    }

    private void tickPartyCooldowns(Actor actor) {
        Map<String, Integer> cooldowns = partyCooldowns.get(actor);
        if (cooldowns == null || cooldowns.isEmpty()) {
            return;
        }
        for (String name : new ArrayList<>(cooldowns.keySet())) {
            int next = cooldowns.get(name) - 1;
            if (next <= 0) {
                cooldowns.remove(name);
            } else {
                cooldowns.put(name, next);
            }
        }
        if (cooldowns.isEmpty()) {
            partyCooldowns.remove(actor);
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

    private double rewardMultiplier(Actor foe) {
        if (isBossEnemy(foe)) {
            return 2.25;
        }
        return isEliteEnemy(foe) ? 1.75 : 1.0;
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
        int rawGold = enemies.stream()
                .mapToInt(foe -> (int) Math.round(monsterSpecFor(foe).gold() * rewardMultiplier(foe)))
                .sum();
        int rawXp = enemies.stream()
                .mapToInt(foe -> (int) Math.round(monsterSpecFor(foe).xp() * rewardMultiplier(foe)))
                .sum();
        int xp = (int) Math.round(rawXp * 0.80 * (1.0 + player.skillRank("hard_won_lessons") * 0.08));
        int gold = (int) Math.round(rawGold * (1.0 + player.skillRank("scavenger") * 0.10));
        player.gold += gold;
        List<String> recovered = partyMembers.stream().filter(actor -> !actor.alive()).map(actor -> actor.name).toList();
        for (Actor actor : partyMembers) {
            if (!actor.alive()) {
                actor.hp = Math.max(1, actor.maxHp / 4);
            }
        }
        List<String> notes = new ArrayList<>();
        for (Actor actor : partyMembers) {
            for (String note : actor.gainXp(xp)) {
                notes.add(actor.name + ": " + note);
            }
        }
        addLog("Victory! +" + xp + " XP, +" + gold + " gold.");
        if (!recovered.isEmpty()) {
            addLog(String.join(", ", recovered) + " recover after the fight.");
        }
        for (String note : notes) {
            addLog(note.contains("Level ") ? "Level up! " + note : note);
        }
    }

    private void resolveRunAttempt(Actor actor) {
        double chance = runChance(actor);
        int percent = (int) Math.round(chance * 100.0);
        if (random.nextDouble() < chance) {
            finished = true;
            victory = false;
            fled = true;
            floaters.add(new FloatingText("Escaped", floaterX(actor), floaterY(actor), 34, new Color(200, 225, 255)));
            addLog(actor.name + " breaks away from combat. Escape chance: " + percent + "%.");
            return;
        }
        applyStatus(actor, "evasive", Math.max(8, 8 + actor.dexterity / 3));
        floaters.add(new FloatingText("Failed", floaterX(actor), floaterY(actor), 28, new Color(231, 196, 119)));
        addLog(actor.name + " tries to run, but the enemy keeps pace. Escape chance: " + percent + "%.");
    }

    public double runChance(Actor actor) {
        if (actor == null) {
            return 0.0;
        }
        double enemyPressure = livingEnemies().stream()
                .mapToDouble(foe -> foe.dexterity * 0.65 + foe.attack * 0.22 + foe.defense * 0.12)
                .max()
                .orElse(8.0);
        double escapeScore = actor.dexterity * 1.25 + actor.constitution * 0.55 + actor.level * 0.45;
        double chance = 0.46 + (escapeScore - enemyPressure) * 0.018;
        chance -= Math.max(0, livingEnemies().size() - livingParty().size()) * 0.055;
        if (statusBucket(actor).containsKey("haste")) {
            chance += 0.08;
        }
        if (statusBucket(actor).containsKey("evasive")) {
            chance += 0.10;
        }
        return Math.max(0.15, Math.min(0.85, chance));
    }

    private boolean hasBossEnemy() {
        return livingEnemies().stream().anyMatch(this::isBossEnemy);
    }

    public void addLog(String message) {
        log.add(message);
        while (log.size() > 36) {
            log.remove();
        }
    }

    private void beginActivePartyTurn() {
        Actor actor = activeActor();
        if (actor == null || checkFinished()) {
            return;
        }
        partyTargetExplicit = false;
        tickPartyCooldowns(actor);
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
        if (actor.hp * 2 <= actor.maxHp
                && hasEquipmentEffect(actor, EquipmentEffectHooks.WOUNDED_FOCUS)
                && woundedFocusSpent.add(actor)) {
            int focus = Math.max(3, actor.willpower / 3);
            int before = actor.mp;
            actor.mp = Math.min(actor.maxMp, actor.mp + focus);
            applyStatus(actor, "haste", null);
            addLog(actor.name + " sharpens wounded focus for +" + (actor.mp - before) + " MP.");
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

    private void tickActionAnimation() {
        if (actionAnimation == null) {
            return;
        }
        actionAnimation.tick();
        effectTimer = actionAnimation.releaseRemainingFrames();
        for (AbilityResolutionStep step : actionAnimation.pendingImpactSteps()) {
            actionAnimation.markStepTriggered(step.index());
            if (animationImpact != null) {
                animationImpact.accept(step);
            }
        }
        if (actionAnimation.stage() == BattleActionAnimation.Stage.DONE) {
            Runnable complete = animationComplete;
            clearActionAnimation();
            if (complete != null) {
                complete.run();
            }
        }
    }

    private boolean beginTelegraphedAnimatedAction(
            String telegraphLabel,
            int telegraphDanger,
            String kind,
            Actor source,
            Actor target,
            int castFrames,
            int travelFrames,
            int impactFrames,
            Runnable onImpact,
            Runnable onComplete
    ) {
        boolean started = beginAnimatedAction(kind, source, target, castFrames, travelFrames, impactFrames, onImpact, onComplete);
        if (started) {
            actionTelegraphLabel = telegraphLabel == null ? "" : telegraphLabel;
            actionTelegraphDanger = Math.max(0, telegraphDanger);
        }
        return started;
    }

    private boolean beginAnimatedAction(
            String kind,
            Actor source,
            Actor target,
            int castFrames,
            int travelFrames,
            int impactFrames,
            Runnable onImpact,
            Runnable onComplete
    ) {
        return beginAnimatedAction(kind, source, target, List.of(target), BattleActionAnimation.VisualMode.SINGLE,
                castFrames, travelFrames, impactFrames, step -> onImpact.run(), onComplete);
    }

    private boolean beginAnimatedAction(
            String kind,
            Actor source,
            Actor target,
            List<Actor> visualTargets,
            BattleActionAnimation.VisualMode visualMode,
            int castFrames,
            int travelFrames,
            int impactFrames,
            Runnable onImpact,
            Runnable onComplete
    ) {
        return beginAnimatedAction(kind, source, target, visualTargets, visualMode,
                castFrames, travelFrames, impactFrames, step -> onImpact.run(), onComplete);
    }

    private boolean beginAnimatedAction(
            String kind,
            Actor source,
            Actor target,
            List<Actor> visualTargets,
            BattleActionAnimation.VisualMode visualMode,
            int castFrames,
            int travelFrames,
            int impactFrames,
            Consumer<AbilityResolutionStep> onImpact,
            Runnable onComplete
    ) {
        if (finished || actionAnimation != null || source == null || target == null) {
            return false;
        }
        actionAnimation = new BattleActionAnimation(source, target, visualTargets, visualMode, kind, castFrames, travelFrames, impactFrames);
        animationImpact = onImpact;
        animationComplete = onComplete;
        effectKind = actionAnimation.effectKind;
        effectSource = source;
        effectTarget = target;
        effectTimer = 0;
        if (enemies.contains(source)) {
            setEnemyLunge(source, 8);
        } else {
            playerLunge = Math.max(playerLunge, 8);
        }
        return true;
    }

    private void clearActionAnimation() {
        actionAnimation = null;
        animationImpact = null;
        animationComplete = null;
        actionTelegraphLabel = "";
        actionTelegraphDanger = 0;
        effectTimer = 0;
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
            return GameConfig.MAP_COLS * GameConfig.TILE - 440 - (enemyIndex % 2) * 220;
        }
        int partyIndex = Math.max(0, partyMembers.indexOf(actor));
        return 215 + (partyIndex % 2) * 260;
    }

    private int floaterY(Actor actor) {
        int enemyIndex = enemies.indexOf(actor);
        if (enemyIndex >= 0) {
            return 220 + (enemyIndex / 2) * 300 + (enemyIndex % 2) * 120;
        }
        int partyIndex = Math.max(0, partyMembers.indexOf(actor));
        return 230 + (partyIndex / 2) * 320 + (partyIndex % 2) * 108;
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
            applyEliteDeathTrait(foe);
            defeatedEnemies.add(foe);
            defeatedMonsterNames.add(monsterSpecFor(foe).name());
            defeatedMonsterKeys.add(monsterSpecFor(foe).key());
        }
    }

    private void applyEliteDeathTrait(Actor foe) {
        if (eliteTrait(foe) != EliteTrait.VOLATILE || foe.alive()) {
            return;
        }
        int blast = Math.max(4, monsterSpecFor(foe).attack() / 3 + 4);
        int total = 0;
        for (Actor actor : livingParty()) {
            int dealt = applyDirectDamage(actor, blast);
            total += dealt;
            if (dealt > 0) {
                floaters.add(new FloatingText("-" + dealt, floaterX(actor), floaterY(actor), 30, new Color(255, 174, 114)));
                applyStatus(actor, "burn", null);
            }
        }
        if (total > 0) {
            shake = Math.max(shake, 16);
            flashTimer = Math.max(flashTimer, 6);
            addLog(foe.name + " erupts on defeat for " + total + " total damage.");
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

    private String effectForAbility(Ability ability) {
        return GameData.battleEffectForAbility(ability);
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
