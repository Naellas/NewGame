package com.alderfall.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import javax.imageio.ImageIO;

/** World-space cadence, true standing idle and town-NPC coverage. */
public final class WorldPoseTest {
    public static void main(String[] args) throws Exception {
        LocomotionClock slow = new LocomotionClock(), fast = new LocomotionClock(), blocked = new LocomotionClock();
        slow.selectAt(0, 0, 0); fast.selectAt(0, 0, 0);
        LocomotionClock.Selection a = null, b = null;
        for (int i = 1; i <= 20; i++) a = slow.selectAt(i, i * .02, 0);
        for (int i = 1; i <= 10; i++) b = fast.selectAt(i, i * .04, 0);
        require(a.frame() == b.frame(), "Same travel distance produced different stride phases");
        for (int i = 0; i < 80; i++) require(blocked.selectAt(i, 4, 5).action().equals("idle"), "Standing character walks in place");
        require(slow.selectAt(21, .4, 0).action().startsWith("settle_"), "Stopped position continued walking");
        require(slow.selectAt(30, .4, 0).action().equals("idle"), "Stopped character never stands idle");
        require(slow.selectAt(31, 20, 20).action().equals("idle"), "Teleport was treated as a stride");

        AssetStore assets = new AssetStore(Path.of("assets"));
        ArrayList<String> actors = new ArrayList<>(CharacterAnimationAudit.ACTORS);
        assets.assetNames().stream().filter(n -> n.startsWith("npc_") && n.endsWith("_model_down"))
                .map(n -> n.substring(0, n.length() - "_model_down".length())).sorted()
                .filter(n -> !actors.contains(n)).forEach(actors::add);
        BufferedImage preview = new BufferedImage(6 * 144, actors.size() * 160, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = preview.createGraphics(); g.setColor(new Color(28, 34, 40)); g.fillRect(0, 0, preview.getWidth(), preview.getHeight());
        int row = 0;
        for (String actor : actors) {
            String down = actor + "_model_down", side = actor + "_model_right";
            if (!assets.hasSprite(side)) side = down;
            HashSet<Integer> poses = new HashSet<>();
            int ground = -1;
            for (int f = 0; f < 24; f++) {
                BufferedImage pose = assets.animatedSpriteFit(down, "idle", 88, 66, f);
                poses.add(Arrays.hashCode(pose.getRGB(0, 0, 88, 66, null, 0, 88)));
                int bottom = bottom(pose);
                if (ground < 0) ground = bottom;
                require(bottom == ground, "Idle feet drift: " + actor);
            }
            require(poses.size() >= 3, "Idle invisible at gameplay scale: " + actor);
            BufferedImage stand = assets.spriteFit(side, 192, 144);
            BufferedImage settle = assets.animatedSpriteFit(side, "settle_6", 192, 144, 11);
            require(Arrays.equals(stand.getRGB(0,0,192,144,null,0,192), settle.getRGB(0,0,192,144,null,0,192)), "Settling never returns to standing: " + actor);
            g.setColor(Color.WHITE); g.drawString(actor + " | standing front / side / breathe / walk contact / passing / opposite", 4, row * 160 + 14);
            String[] names = {down, side, down, side, side, side};
            String[] actions = {"idle", "idle", "idle", "walk", "walk", "walk"};
            int[] frames = {0, 0, 6, 0, 8, 16};
            for (int i = 0; i < 6; i++) g.drawImage(assets.animatedSpriteFit(names[i], actions[i], 192, 144, frames[i]), i * 144, row * 160 + 18, 144, 108, null);
            row++;
        }
        g.dispose(); ImageIO.write(preview, "png", Path.of("tools/reviews/characters/world-poses.png").toFile());
        ImageIO.write(preview.getSubimage(0, 0, preview.getWidth(), 5 * 160), "png", Path.of("tools/reviews/characters/player-proportions.png").toFile());
        ImageIO.write(preview.getSubimage(0, 14 * 160, preview.getWidth(), Math.min(10, actors.size() - 14) * 160), "png", Path.of("tools/reviews/characters/npc-proportions.png").toFile());
        System.out.println("World pose checks passed: distance-based stride, blocked movement, stops, teleports and visible planted idle for " + actors.size() + " actors.");
    }
    private static int bottom(BufferedImage image) {
        for (int y = image.getHeight() - 1; y >= 0; y--) for (int x = 0; x < image.getWidth(); x++)
            if ((image.getRGB(x,y) >>> 24) > 32) return y;
        return -1;
    }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}

