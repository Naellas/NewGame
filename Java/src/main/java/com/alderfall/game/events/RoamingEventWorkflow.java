package com.alderfall.game;

import java.util.List;

public final class RoamingEventWorkflow {
    private RoamingEventWorkflow() {
    }

    public static void openLostSatchelPrompt(GameState state, int seed) {
        state.openRoamingEventPrompt("Lost Purse", "event_lost_purse",
                "Lost Purse: A leather purse lies in the road grit. A stamped owner token hangs beside its drawstring.",
                List.of(
                        new GameState.RoamingEventChoice("Inspect the owner token (INT)", () -> resolveSatchelInspect(state, seed)),
                        new GameState.RoamingEventChoice("Recover the loose coins (DEX)", () -> resolveSatchelRecover(state, seed)),
                        new GameState.RoamingEventChoice("Leave a visible marker (CHA + WIL)", () -> resolveSatchelMarker(state, seed)),
                        new GameState.RoamingEventChoice("Ignore it", () -> state.resolveRoamingEventPrompt(
                                "Lost Purse: You leave the purse where its owner might still find it.",
                                "You leave the purse."))
                ));
    }

    public static void openShrinePrompt(GameState state, int seed) {
        String insight = state.player.intelligence >= 12
                ? " Your intelligence catches the shrine's old purpose: this is a road-ward, meant to turn fear into warning rather than obedience."
                : " The glyphs are old enough that guessing wrong could matter.";
        state.openRoamingEventPrompt("Unstable Shrine", "npc_rowan",
                "Unstable Shrine: A cracked roadside idol hums under its own heat." + insight,
                List.of(
                        new GameState.RoamingEventChoice("Pray with steady will (WIL + INT)", () -> resolveShrinePrayer(state, seed)),
                        new GameState.RoamingEventChoice("Desecrate the stone (STR + WIL)", () -> resolveShrineDesecration(state, seed)),
                        new GameState.RoamingEventChoice("Offer 5g and align the rite (INT + CHA)", () -> resolveShrineOffering(state, seed)),
                        new GameState.RoamingEventChoice("Leave the shrine untouched", () -> state.resolveRoamingEventPrompt(
                                "Unstable Shrine: You mark the place in memory and leave its old vow sleeping.",
                                "You leave the shrine untouched."))
                ));
    }

    public static void openWoundedTravelerPrompt(GameState state, int seed) {
        state.openRoamingEventPrompt("Wounded Traveler", "npc_mira_sunwarden",
                "Wounded Traveler: A stranger grips a stained sleeve and tries to look less frightened than they are.",
                List.of(
                        new GameState.RoamingEventChoice("Treat the wound (INT + WIL)", () -> resolveTravelerTreat(state, seed)),
                        new GameState.RoamingEventChoice("Share supplies and calm them (CHA + WIL)", () -> resolveTravelerComfort(state, seed)),
                        new GameState.RoamingEventChoice("Question them about the attack (INT + CHA)", () -> resolveTravelerQuestion(state, seed)),
                        new GameState.RoamingEventChoice("Move on", () -> state.resolveRoamingEventPrompt(
                                "Wounded Traveler: You choose distance. The road keeps the answer.",
                                "You leave the traveler behind."))
                ));
    }

    public static void openFishRunPrompt(GameState state, int seed) {
        state.openRoamingEventPrompt("Fish Run", "npc_ren",
                "Fish Run: The water wrinkles in fast silver lines. Some ripples are fish; some are only the river making jokes.",
                List.of(
                        new GameState.RoamingEventChoice("Read the ripple pattern (INT + DEX)", () -> resolveFishRead(state, seed)),
                        new GameState.RoamingEventChoice("Scoop quickly (DEX)", () -> resolveFishScoop(state, seed)),
                        new GameState.RoamingEventChoice("Wait for the patient catch (WIL)", () -> resolveFishWait(state, seed)),
                        new GameState.RoamingEventChoice("Let them pass", () -> state.resolveRoamingEventPrompt(
                                "Fish Run: You let the silver rush pass untouched.",
                                "You leave the fish run."))
                ));
    }

    public static void openAmbushPrompt(GameState state, int seed) {
        String clue = state.player.intelligence >= 11
                ? " The spacing says two watchers and one impatient blade, probably close."
                : " The tracks are recent enough to feel like a held breath.";
        state.openRoamingEventPrompt("Ambush Tracks", "npc_quartermaster",
                "Ambush Tracks: Heel marks, dragged brush, and one careless boot tell the road's secret." + clue,
                List.of(
                        new GameState.RoamingEventChoice("Read the tracks (INT)", () -> resolveAmbushRead(state, seed)),
                        new GameState.RoamingEventChoice("Set a counter-ambush (DEX + WIL)", () -> resolveAmbushCounter(state, seed)),
                        new GameState.RoamingEventChoice("Charge through before they set (STR + DEX)", () -> resolveAmbushCharge(state, seed)),
                        new GameState.RoamingEventChoice("Take a longer route", () -> state.resolveRoamingEventPrompt(
                                "Ambush Tracks: You take the ugly route around the kill-zone. Pride survives worse losses than time.",
                                "You avoid the ambush."))
                ));
    }

    public static void openTrapPrompt(GameState state, int seed) {
        String clue = state.player.intelligence >= 10
                ? " Your intelligence spots the trigger line: waist-high for panic, ankle-high for pursuit."
                : " Something about the dust pattern is too neat.";
        state.openRoamingEventPrompt("Hidden Trap", "npc_vexa",
                "Hidden Trap: A disturbed patch of ground waits beside the path." + clue,
                List.of(
                        new GameState.RoamingEventChoice("Disarm the mechanism (DEX + INT)", () -> resolveTrapDisarm(state, seed)),
                        new GameState.RoamingEventChoice("Avoid with careful footwork (DEX)", () -> resolveTrapAvoid(state, seed)),
                        new GameState.RoamingEventChoice("Trigger it from a distance (INT + WIL)", () -> resolveTrapTrigger(state, seed)),
                        new GameState.RoamingEventChoice("Back away", () -> state.resolveRoamingEventPrompt(
                                "Hidden Trap: You give the mechanism more respect than curiosity and leave it behind.",
                                "You avoid the trap."))
                ));
    }

    public static void triggerTrapTile(GameState state, int seed) {
        int notice = eventRoll(seed, 131, state.player.intelligence, state.player.dexterity);
        if (notice >= 28) {
            state.openRoamingEventPrompt("Hidden Trap", "npc_vexa",
                    "Hidden Trap: Your foot stops just before the trigger takes your weight. The mechanism is live beneath the dust.",
                    List.of(
                            new GameState.RoamingEventChoice("Freeze and disarm it (DEX + INT)", () -> resolveTrapDisarm(state, seed)),
                            new GameState.RoamingEventChoice("Step away carefully (DEX)", () -> resolveTrapAvoid(state, seed)),
                            new GameState.RoamingEventChoice("Kick it loose and brace (INT + WIL)", () -> resolveTrapTrigger(state, seed))
                    ));
            return;
        }
        int damage = 6 + Math.floorMod(seed, 7);
        state.player.takeDamage(damage);
        startTrapAmbush(state, "Hidden Trap: The trigger snaps underfoot. " + damage
                + " damage, then figures break cover.");
    }

    private static void resolveShrinePrayer(GameState state, int seed) {
        int roll = eventRoll(seed, 11, state.player.willpower, state.player.intelligence);
        int xp = 10 + roll % 14;
        if (roll >= 28) {
            state.player.gainXp(xp);
            int heal = 6 + state.player.willpower / 2;
            state.player.hp = Math.min(state.player.maxHp, state.player.hp + heal);
            state.worldAbilityTimers.merge("battle_advantage", 90 + roll % 60, Math::max);
            state.resolveRoamingEventPrompt(
                    "Unstable Shrine: You name the fear without kneeling to it. The ward answers cleanly. Result: +" + xp
                            + " XP, +" + heal + " HP, and battle advantage for the next fight.",
                    "The shrine steadies you.");
            return;
        }
        int damage = Math.max(1, 5 - state.player.defense / 2);
        state.player.takeDamage(damage);
        state.player.gainXp(Math.max(4, xp / 2));
        state.startRoamingEventBattle(List.of("wraith"),
                "Unstable Shrine: The prayer catches wrong, deals " + damage + " damage, and wakes an ash wraith.");
    }

    private static void resolveShrineDesecration(GameState state, int seed) {
        int roll = eventRoll(seed, 23, state.player.strength, state.player.willpower);
        int gold = 10 + roll % 22;
        if (roll >= 30) {
            state.player.gold += gold;
            state.player.addItem("arcane_dust", 1);
            state.resolveRoamingEventPrompt(
                    "Unstable Shrine: You break only the hungry part of the rite. A false relic-shell crumbles open. Recovered: "
                            + gold + "g and arcane dust.",
                    "You strip power from the shrine.");
            return;
        }
        int damage = 4 + roll % 6;
        state.player.takeDamage(damage);
        state.startRoamingEventBattle(List.of("wraith", "skeleton"),
                "Unstable Shrine: The stone splits, deals " + damage + " damage, and an angry wraith rises from the ward.");
    }

    private static void resolveShrineOffering(GameState state, int seed) {
        if (state.player.gold < 5) {
            state.resolveRoamingEventPrompt(
                    "Unstable Shrine: You search for a worthy offering and find only empty pockets. The glyphs dim without anger.",
                    "You need 5g to make that offering.");
            return;
        }
        state.player.gold -= 5;
        int roll = eventRoll(seed, 37, state.player.intelligence, state.player.charisma);
        int xp = 8 + roll % 12;
        state.player.gainXp(xp);
        if (roll >= 26) {
            state.worldAbilityTimers.merge("foragers_luck", 110 + roll % 70, Math::max);
            state.resolveRoamingEventPrompt(
                    "Unstable Shrine: The offering completes the missing gesture. The ward shows a safer road through the next stretch. Result: +"
                            + xp + " XP and forager's luck.",
                    "The offering pleases the old ward.");
            return;
        }
        if (roll <= 16) {
            state.startRoamingEventBattle(List.of("wraith"),
                    "Unstable Shrine: The offering is accepted by the wrong presence. An ash wraith answers.");
            return;
        }
        state.resolveRoamingEventPrompt(
                "Unstable Shrine: The offering is accepted, but your reading of the rite stays incomplete. Result: +" + xp
                        + " XP.",
                "The shrine accepts a modest offering.");
    }

    private static void resolveTrapDisarm(GameState state, int seed) {
        int roll = eventRoll(seed, 41, state.player.dexterity, state.player.intelligence);
        if (roll >= 27) {
            int xp = 12 + roll % 12;
            state.player.gainXp(xp);
            state.player.addItem("iron_scrap", 1);
            state.resolveRoamingEventPrompt(
                    "Hidden Trap: You pin the spring, cut the loop, and keep the useful metal. Result: +" + xp
                            + " XP and iron scrap.",
                    "Trap disarmed.");
            return;
        }
        int damage = 5 + roll % 8;
        state.player.takeDamage(damage);
        startTrapAmbush(state, "Hidden Trap: The mechanism accepts your first move and rejects the second. "
                + damage + " damage, then the trap-setters arrive.");
    }

    private static void resolveTrapAvoid(GameState state, int seed) {
        int roll = eventRoll(seed, 43, state.player.dexterity, 0);
        if (roll >= 20) {
            int xp = 7 + roll % 8;
            state.player.gainXp(xp);
            state.resolveRoamingEventPrompt(
                    "Hidden Trap: You move with the boring precision that keeps people alive. Result: +" + xp + " XP.",
                    "You slip past the trap.");
            return;
        }
        int damage = 3 + roll % 5;
        state.player.takeDamage(damage);
        startTrapAmbush(state, "Hidden Trap: Your heel finds the one honest board in a field of liars. "
                + damage + " damage, then the ambush closes.");
    }

    private static void resolveTrapTrigger(GameState state, int seed) {
        int roll = eventRoll(seed, 47, state.player.intelligence, state.player.willpower);
        int xp = 6 + roll % 10;
        state.player.gainXp(xp);
        if (roll >= 24) {
            state.worldAbilityTimers.merge("battle_advantage", 55 + roll % 45, Math::max);
            state.resolveRoamingEventPrompt(
                    "Hidden Trap: You set it off with a thrown stone and study the violence from a polite distance. Result: +"
                            + xp + " XP and battle advantage.",
                    "Trap spent safely.");
            return;
        }
        state.startRoamingEventBattle(trapAmbushers(state),
                "Hidden Trap: The trigger fires loudly. You avoid the teeth, but the noise pulls enemies out of cover.");
    }

    private static void resolveAmbushRead(GameState state, int seed) {
        int roll = eventRoll(seed, 53, state.player.intelligence, 0);
        int xp = 10 + roll % 12;
        state.player.gainXp(xp);
        if (roll >= 20) {
            state.worldAbilityTimers.merge("battle_advantage", 65 + roll % 55, Math::max);
            state.resolveRoamingEventPrompt(
                    "Ambush Tracks: You understand the trap before it becomes a fight. Result: +" + xp
                            + " XP and battle advantage.",
                    "You read the ambush.");
            return;
        }
        state.startRoamingEventBattle(ambushers(state),
                "Ambush Tracks: You read the signs too late. The hidden attackers spring first.");
    }

    private static void resolveAmbushCounter(GameState state, int seed) {
        int roll = eventRoll(seed, 59, state.player.dexterity, state.player.willpower);
        int xp = 12 + roll % 14;
        state.player.gainXp(xp);
        if (roll >= 28) {
            int gold = 6 + roll % 16;
            state.player.gold += gold;
            state.worldAbilityTimers.merge("battle_advantage", 90 + roll % 50, Math::max);
            state.resolveRoamingEventPrompt(
                    "Ambush Tracks: You wait where their patience runs out first. They scatter and leave coin behind. Result: +"
                            + xp + " XP, " + gold + "g, and battle advantage.",
                    "Counter-ambush successful.");
            return;
        }
        state.worldAbilityTimers.merge("battle_advantage", 45 + roll % 35, Math::max);
        state.startRoamingEventBattle(ambushers(state),
                "Ambush Tracks: Your trap is imperfect, but it ruins theirs. The fight begins with your side ready.");
    }

    private static void resolveAmbushCharge(GameState state, int seed) {
        int roll = eventRoll(seed, 61, state.player.strength, state.player.dexterity);
        int xp = 9 + roll % 13;
        state.player.gainXp(xp);
        if (roll >= 27) {
            state.resolveRoamingEventPrompt(
                    "Ambush Tracks: Speed makes the ambush useless. You crash through before courage finishes forming. Result: +"
                            + xp + " XP.",
                    "You break the ambush line.");
            return;
        }
        int damage = 4 + roll % 7;
        state.player.takeDamage(damage);
        state.startRoamingEventBattle(ambushers(state),
                "Ambush Tracks: You move fast, but one hidden angle was faster. " + damage
                        + " damage, then steel follows.");
    }

    private static void resolveSatchelInspect(GameState state, int seed) {
        int roll = eventRoll(seed, 67, state.player.intelligence, 0);
        if (roll <= 15) {
            state.startRoamingEventBattle(ambushers(state),
                    "Lost Purse: The owner token is bait. A trip-line jerks loose and bandits rush the road.");
            return;
        }
        int gold = 6 + roll % 14;
        state.player.gold += gold;
        state.player.gainXp(6 + roll % 8);
        state.resolveRoamingEventPrompt(
                "Lost Purse: The owner token points to a trade road cache. You keep only the unclaimed coin and note the mark. Recovered: "
                        + gold + "g.",
                "You identify the satchel mark.");
    }

    private static void resolveSatchelRecover(GameState state, int seed) {
        int roll = eventRoll(seed, 71, state.player.dexterity, 0);
        int gold = 8 + roll % 18;
        state.player.gold += gold;
        state.resolveRoamingEventPrompt(
                "Lost Purse: You gather the coins before they spill through the split lining. Recovered: " + gold
                        + "g.",
                "You recover the coins.");
    }

    private static void resolveSatchelMarker(GameState state, int seed) {
        int roll = eventRoll(seed, 73, state.player.charisma, state.player.willpower);
        int xp = 8 + roll % 10;
        state.player.gainXp(xp);
        state.worldAbilityTimers.merge("foragers_luck", 70 + roll % 50, Math::max);
        state.resolveRoamingEventPrompt(
                "Lost Purse: You raise a marker visible to roadfolk but dull to thieves. Result: +" + xp
                        + " XP and forager's luck.",
                "You mark the lost purse.");
    }

    private static void resolveTravelerTreat(GameState state, int seed) {
        int roll = eventRoll(seed, 79, state.player.intelligence, state.player.willpower);
        int xp = 8 + roll % 12;
        int heal = 6 + roll % 8;
        state.player.gainXp(xp);
        state.player.hp = Math.min(state.player.maxHp, state.player.hp + heal);
        state.resolveRoamingEventPrompt(
                "Wounded Traveler: Your hands stay useful while fear argues. Result: +" + xp + " XP and +" + heal + " HP.",
                "You aid the traveler.");
    }

    private static void resolveTravelerComfort(GameState state, int seed) {
        int roll = eventRoll(seed, 83, state.player.charisma, state.player.willpower);
        int xp = 6 + roll % 10;
        state.player.gainXp(xp);
        state.worldAbilityTimers.merge("foragers_luck", 75 + roll % 55, Math::max);
        state.resolveRoamingEventPrompt(
                "Wounded Traveler: Food, water, and a steady voice do what medicine cannot. Result: +" + xp
                        + " XP and forager's luck.",
                "The traveler steadies.");
    }

    private static void resolveTravelerQuestion(GameState state, int seed) {
        int roll = eventRoll(seed, 89, state.player.intelligence, state.player.charisma);
        int xp = 7 + roll % 11;
        state.player.gainXp(xp);
        if (roll <= 16) {
            state.startRoamingEventBattle(ambushers(state),
                    "Wounded Traveler: The story has too many rehearsed parts. The bait drops the act and attackers rush in.");
            return;
        }
        state.worldAbilityTimers.merge("battle_advantage", 55 + roll % 40, Math::max);
        state.resolveRoamingEventPrompt(
                "Wounded Traveler: Their scattered story becomes a map of danger when you ask the right questions. Result: +"
                        + xp + " XP and battle advantage.",
                "You learn from the attack.");
    }

    private static void resolveFishRead(GameState state, int seed) {
        int roll = eventRoll(seed, 97, state.player.intelligence, state.player.dexterity);
        state.player.addItem("quest_clean_water_skin", 1);
        state.player.addItem("trail_rations", roll >= 25 ? 2 : 1);
        state.player.gainXp(5 + roll % 8);
        state.resolveRoamingEventPrompt(
                "Fish Run: You wait for the honest ripple and scoop where the river forgets to lie. Recovered: clean water and trail rations.",
                "You read the fish run.");
    }

    private static void resolveFishScoop(GameState state, int seed) {
        int roll = eventRoll(seed, 101, state.player.dexterity, 0);
        if (roll >= 19) {
            state.player.addItem("trail_rations", 2);
            state.resolveRoamingEventPrompt(
                    "Fish Run: Fast hands beat fast water. Recovered: two trail rations.",
                    "Quick catch.");
            return;
        }
        if (state.world.kind(state.currentMapId).equals("dungeon")) {
            state.startRoamingEventBattle(List.of("river_eel"),
                    "Fish Run: The splash echoes wrong. Something hungry follows it.");
            return;
        }
        state.player.addItem("trail_rations", 1);
        state.resolveRoamingEventPrompt(
                "Fish Run: You catch one useful meal and donate your dignity to the river. Recovered: trail rations.",
                "Messy catch.");
    }

    private static void resolveFishWait(GameState state, int seed) {
        int roll = eventRoll(seed, 103, state.player.willpower, 0);
        state.player.addItem("quest_clean_water_skin", 1);
        state.player.gainXp(4 + roll % 7);
        state.resolveRoamingEventPrompt(
                "Fish Run: Patience finds the slow pool beneath the rush. Recovered: clean water.",
                "Patient catch.");
    }

    private static void startTrapAmbush(GameState state, String status) {
        state.startRoamingEventBattle(trapAmbushers(state), status);
    }

    private static List<String> trapAmbushers(GameState state) {
        return "dungeon".equals(state.world.kind(state.currentMapId))
                ? List.of("goblin_trapper", "goblin_skirmisher")
                : List.of("bandit_cutthroat", "bandit_archer");
    }

    private static List<String> ambushers(GameState state) {
        return "dungeon".equals(state.world.kind(state.currentMapId))
                ? List.of("goblin_trapper", "skeleton")
                : List.of("bandit_cutthroat", "bandit_archer");
    }

    private static int eventRoll(int seed, int salt, int primaryStat, int secondaryStat) {
        return 1
                + Math.floorMod(seed * 31 + salt * 997, 20)
                + Math.max(0, primaryStat)
                + Math.max(0, secondaryStat) / 2;
    }
}
