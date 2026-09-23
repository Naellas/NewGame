package com.alderfall.game;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Renders an environment with real actors and HUD, at the game's logical resolution. */
public final class BattleBackdropPreview {
    public static void main(String[] args) throws Exception {
        String backdrop = args.length == 0 ? "battle_forest_edge_backdrop" : args[0];
        Path output = Path.of("exports/combat-graphics", backdrop + "-preview.png");
        Files.createDirectories(output.getParent());
        SwingUtilities.invokeAndWait(() -> {
            GamePanel panel = new GamePanel(Path.of("").toAbsolutePath());
            try {
                ((Timer) field("timer").get(panel)).stop();
                GameState state = (GameState) field("state").get(panel);
                state.chooseClass("Mage");
                while (state.mode == GameMode.STORY_INTRO) state.advanceStoryIntro();
                List<Actor> allies = List.of(
                        new Actor("Seraphine Vale", "npc_seraphine", "Veilrunner", 80, 40, 12, 8),
                        new Actor("Maera Quill", "npc_maera", "Mage", 70, 60, 10, 6),
                        new Actor("Aria Foxglove", "npc_aria", "Ranger", 85, 40, 12, 7));
                Battle battle = new Battle(state.player, allies, List.of(GameData.MONSTERS.get("goblin_archer"),
                        GameData.MONSTERS.get("goblin_scout"), GameData.MONSTERS.get("goblin_trapper")), new Random(7), 'f', "overworld");
                battle.backdrop = backdrop;
                state.battle = battle;
                state.mode = GameMode.BATTLE;
                panel.setSize(GameConfig.WIDTH, GameConfig.HEIGHT);
                BufferedImage image = new BufferedImage(GameConfig.WIDTH, GameConfig.HEIGHT, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = image.createGraphics();
                panel.paint(g);
                g.dispose();
                ImageIO.write(image, "png", output.toFile());
                System.out.println(output.toAbsolutePath());
            } catch (Exception ex) { throw new IllegalStateException(ex); }
            finally { panel.shutdown(); }
        });
    }

    private static Field field(String name) throws Exception {
        Field field = GamePanel.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
