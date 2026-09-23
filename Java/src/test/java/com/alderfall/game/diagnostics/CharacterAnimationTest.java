package com.alderfall.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;

/** Checks the rendering regressions that make a walk slide or an attack restart. */
public final class CharacterAnimationTest {
    public static void main(String[] args) throws Exception {
        checkTimeline();
        checkStableViewport();
        AssetStore assets = new AssetStore(Path.of("assets"));
        String[] actors = CharacterAnimationAudit.ACTORS.stream().map(actor -> actor + "_combat_v4").toArray(String[]::new);
        String[] actions = {"attack", "cast", "shoot", "cast", "attack", "shoot", "attack", "attack", "cast", "cast", "attack", "cast", "attack", "cast"};
        BufferedImage preview = new BufferedImage(3456, actors.length * 168, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = preview.createGraphics();
        g.setColor(new Color(28, 34, 40)); g.fillRect(0, 0, preview.getWidth(), preview.getHeight());
        for (int row = 0; row < actors.length; row++) {
            require(assets.hasSprite(actors[row]), "Missing refreshed model " + actors[row]);
            require(assets.hasAuthoredAnimation(actors[row], actions[row]), "Missing authored action " + actors[row]);
            require(assets.animatedSpriteFrameCount(actors[row], actions[row], 210, 194) == 24, "Explicit twenty-four-frame sheet");
            long previousHash = 0;
            for (int frame = 0; frame < 24; frame++) {
                BufferedImage image = assets.animatedSpriteFit(actors[row], actions[row], 210, 194, frame);
                require(image == assets.animatedSpriteFit(actors[row], actions[row], 210, 194, frame), "Frames must be cached");
                require((image.getRGB(0, 0) >>> 24) == 0, "Chroma background must be transparent");
                long hash = hash(image);
                require(frame == 0 || hash != previousHash, "Authored poses must change");
                previousHash = hash;
                g.drawImage(image, frame * 144, row * 168 + 22, 144, 144, null);
            }
            g.setColor(Color.WHITE); g.drawString(actors[row] + " / " + actions[row] + " / 24 poses", 12, row * 168 + 17);
        }
        for (String actor : List.of("npc_calder_battle_sprite", "npc_cassia_battle_sprite", "npc_lyra_battle_sprite",
                "npc_maera_battle_sprite", "npc_rafiq_battle_sprite", "npc_samir_battle_sprite",
                "npc_seraphine_battle_sprite", "npc_vesper_battle_sprite", "class_ranger_model", "class_cleric_model", "class_rogue_model")) {
            require(assets.hasSprite(actor), "Missing party model " + actor);
            for (String action : List.of("idle", "attack", "cast", "shoot", "defend", "item", "hit")) {
                require(assets.hasAnimatedSprite(actor, action), "Missing action " + actor + "/" + action);
                assets.animatedSpriteFit(actor, action, 150, 194, 7);
            }
            BufferedImage rest = assets.animatedSpriteFit(actor, "idle", 150, 194, 0);
            BufferedImage breath = assets.animatedSpriteFit(actor, "idle", 150, 194, 6);
            require(hash(rest) != hash(breath), "Idle motion must be visible " + actor);
        }
        g.dispose();
        Path output = Path.of("tools/reviews/characters");
        Files.createDirectories(output);
        ImageIO.write(preview, "png", output.resolve("combat-poses.png").toFile());
        System.out.println("Character animation checks passed; preview: " + output.resolve("combat-poses.png"));
    }

    private static void checkTimeline() {
        Actor source = GameData.createPlayer("Mage"), a = GameData.createPlayer("Knight"), b = GameData.createPlayer("Ranger");
        for (double speed : new double[]{.5, 1, 3}) for (var mode : BattleActionAnimation.VisualMode.values()) {
            BattleActionAnimation animation = new BattleActionAnimation(source, a, List.of(a, b), mode, "fire", 10, 20, 18, speed);
            double previous = -1;
            while (animation.stage() != BattleActionAnimation.Stage.DONE) {
                double progress = animation.actorPoseProgress(source);
                require(progress >= previous, "Action strip restarted at a phase boundary");
                previous = progress;
                if (animation.frame() == animation.castFrames) require((int) (progress * 12) == 7, "Release pose must coincide with projectile release");
                for (Actor recipient : List.of(a, b)) {
                    int hit = animation.collisionFrame(animation.targets.indexOf(recipient));
                    require((animation.actorPoseProgress(recipient) < 0) == (animation.frame() < hit), "Recoil before collision");
                }
                animation.tick();
            }
            require(animation.actorPoseProgress(source) == 1, "Action must finish recovery");
        }
    }

    private static void checkStableViewport() throws Exception {
        Path root = Files.createTempDirectory("alderfall-pose-test-");
        // Direct root PNG discovery is also part of the catalog contract.
        Path dir = root;
        Path png = dir.resolve("test_walk_anim.png"), metadata = dir.resolve("test_walk_anim.frames");
        try {
            BufferedImage sheet = new BufferedImage(40, 20, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = sheet.createGraphics();
            g.setColor(Color.WHITE); g.fillRect(5, 4, 6, 12); g.fillRect(25, 2, 6, 12); g.dispose();
            ImageIO.write(sheet, "png", png.toFile()); Files.writeString(metadata, "2");
            AssetStore store = new AssetStore(root);
            BufferedImage first = store.animatedSpriteFit("test", "walk", 20, 20, 0);
            BufferedImage lifted = store.animatedSpriteFit("test", "walk", 20, 20, 1);
            require(hash(first) != hash(lifted), "Per-frame crop erased the authored lift");
            require(opaqueCount(first) == opaqueCount(lifted), "Walking changed body scale");
        } finally {
            Files.deleteIfExists(png); Files.deleteIfExists(metadata); Files.deleteIfExists(root);
        }
    }

    private static long hash(BufferedImage image) {
        long hash = 1;
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) hash = hash * 31 + image.getRGB(x, y);
        return hash;
    }
    private static int opaqueCount(BufferedImage image) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) if ((image.getRGB(x, y) >>> 24) > 0) count++;
        return count;
    }
    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
