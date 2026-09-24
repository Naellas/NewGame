package com.alderfall.game;

import com.alderfall.game.ui.SaveMenuRenderer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.Properties;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Save previews round trip independently of gameplay and remain optional for old saves. */
public final class SaveSnapshotTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static Object field(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(target);
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GamePanel panel = new GamePanel(Path.of("").toAbsolutePath());
            try {
                ((Timer) field(panel, "timer")).stop();
                GameState state = (GameState) field(panel, "state");
                state.chooseClass("Mage");
                while (state.mode == GameMode.STORY_INTRO) state.advanceStoryIntro();
                Method capture = GamePanel.class.getDeclaredMethod("captureSaveSnapshot");
                capture.setAccessible(true);
                BufferedImage image = (BufferedImage) capture.invoke(panel);
                check(image.getWidth() == 640 && image.getHeight() > 0, "Snapshot dimensions");
                Path root = Files.createTempDirectory(Path.of("temp"), "save-snapshot-");
                SaveSystem saves = new SaveSystem(root);
                saves.save(state, "Snapshot test", image);
                String id = state.currentSaveId;
                BufferedImage restored = saves.readSnapshot(id);
                check(restored != null && restored.getRGB(100, 100) == image.getRGB(100, 100), "PNG round trip");
                image.setRGB(100, 100, Color.MAGENTA.getRGB());
                check(saves.listSaves().get(0).saveName().equals("Snapshot test"), "Initial summary");
                saves.overwrite(state, id, "Updated", image);
                check(saves.listSaves().get(0).saveName().equals("Updated"), "Summary cache invalidation");
                check(saves.readSnapshot(id).getRGB(100, 100) == Color.MAGENTA.getRGB(), "Overwrite must refresh image");
                check(saves.load(state, id), "Snapshot save loads");
                saves.save(state, "Legacy");
                check(saves.readSnapshot(state.currentSaveId) == null, "Old save placeholder");
                Path file = root.resolve("saves").resolve(id + ".properties");
                Properties props = new Properties();
                try (var in = Files.newInputStream(file)) { props.load(in); }
                props.setProperty("snapshot.png", "corrupted");
                try (var out = Files.newOutputStream(file)) { props.store(out, "test"); }
                check(saves.readSnapshot(id) == null && saves.load(state, id), "Broken preview must not break load");
                SaveMenuRenderer renderer = (SaveMenuRenderer) field(panel, "saveMenuRenderer");
                renderer = new SaveMenuRenderer(state, saves, (SaveMenuRenderer.Effects) field(renderer, "effects"));
                renderer.setCurrentSnapshot(image);
                state.openSaveMenu(true);
                BufferedImage menu = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = menu.createGraphics();
                renderer.drawSaveMenu(g, true); g.dispose();
                ImageIO.write(menu, "png", root.resolve("save-menu.png").toFile());
                for (UiButton button : (java.util.List<UiButton>) field(panel, "buttons")) {
                    check(new Rectangle(0, 0, 1920, 1080).contains(button.bounds()), "Offscreen save control");
                }
                java.util.List<UiButton> buttons = (java.util.List<UiButton>) field(panel, "buttons");
                UiButton preview = buttons.stream().filter(b -> b.label().startsWith("Preview ")).findFirst().orElseThrow();
                preview.action().run();
                check(field(renderer, "previewSave") != null, "Row selection updates preview");
                check(state.mode == GameMode.SAVE_MENU, "Preview must not load");
                state.closeSaveMenu();
                state.openSaveMenu(false);
                renderer.resetPreview();
                Field width = GamePanel.class.getDeclaredField("renderWidth"); width.setAccessible(true); width.setInt(panel, 1280);
                buttons.clear();
                g = menu.createGraphics(); renderer.drawSaveMenu(g, false); g.dispose();
                check(field(renderer, "previewSave") != null, "Folders preview latest save");
                for (UiButton button : buttons) check(new Rectangle(0, 0, 1280, 1080).contains(button.bounds()), "Offscreen load control");
                ImageIO.write(menu, "png", root.resolve("load-menu.png").toFile());
                System.out.println("SaveSnapshotTest passed; capture: " + root.resolve("save-menu.png"));
            } catch (Exception ex) { throw new RuntimeException(ex); }
        });
        System.exit(0);
    }
}
