package com.alderfall.game;

import com.alderfall.game.map.WorldMap;

import java.nio.file.Path;
import java.util.List;
import java.util.Random;

public final class BattleDefeatTest {
    public static void main(String[] args) {
        Actor player = GameData.createPlayer("Mage");
        player.hp = 0;
        Battle battle = battle(player, List.of());
        check(battle.finished && !battle.victory, "A downed party must immediately lose");
        battle.configureIntro("Ambush", "Bandits appear", "");
        check(!battle.introActive(), "Defeat must not be hidden behind an intro");
        check(!battle.canAcceptInput(), "A defeated party cannot act");

        Actor ally = GameData.createPlayer("Knight");
        Battle supported = battle(player, List.of(ally));
        check(!supported.finished && supported.activeActor() == ally,
                "A living companion must still be able to fight");
        supported.configureIntro("Ambush", "Bandits appear", "");
        ally.hp = 0;
        supported.tick();
        check(supported.finished && !supported.victory && !supported.introActive(),
                "Tick must resolve a party wiped out outside an action");
        int logSize = supported.log.size();
        for (int i = 0; i < 100; i++) supported.tick();
        check(supported.log.size() == logSize, "Defeat must only be logged once");

        GameState state = new GameState(GameConfig.load(Path.of("").toAbsolutePath()));
        state.chooseClass("Mage");
        state.mode = GameMode.EXPLORE;
        state.player.hp = 1;
        state.player.intelligence = 0;
        state.player.dexterity = 0;
        RoamingEventWorkflow.triggerTrapTile(state, 0);
        check(state.player.hp == 0 && state.mode == GameMode.BATTLE,
                "The trap must reproduce lethal damage before the ambush");
        state.tickBattle();
        check(state.battle.finished && !state.battle.victory && state.status.contains("Press R"),
                "Lethal trap ambush must offer revival");
        state.revive();
        check(state.mode == GameMode.EXPLORE && state.battle == null && state.player.hp == state.player.maxHp
                        && WorldMap.PLAYER_VILLAGE_ID.equals(state.currentMapId),
                "Revival must return a healthy player to camp");
        System.out.println("BattleDefeatTest passed: lethal trap, defeat, companion turn, idle recovery and revival.");
    }

    private static Battle battle(Actor player, List<Actor> allies) {
        return new Battle(player, allies, List.of(GameData.MONSTERS.get("bandit_cutthroat"),
                GameData.MONSTERS.get("bandit_archer")), new Random(42), 'g', "city");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
