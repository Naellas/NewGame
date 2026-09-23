package com.alderfall.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Arrays;
import javax.imageio.ImageIO;

public final class ArticulatedWalkingTest {
    public static void main(String[] args) throws Exception {
        AssetStore assets = new AssetStore(Path.of("assets"));
        LocomotionClock clock = new LocomotionClock("articulated");
        clock.selectAt(0, 0, 0);
        require(clock.selectAt(1, .1, 0).action().equals("walk_articulated"), "Wrong walking style");
        require(clock.selectAt(2, .1, 0).action().startsWith("settle_articulated_"), "Wrong stopping style");
        require(clock.selectAt(11, .1, 0).action().equals("idle"), "Never returns to standing");
        String[] actors = {"class_mage", "class_cleric", "npc_seraphine", "class_knight", "npc_lyra", "npc_samir"};
        BufferedImage review = new BufferedImage(192 * 8, 164 * actors.length, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = review.createGraphics();
        g.setColor(new Color(28, 34, 40)); g.fillRect(0, 0, review.getWidth(), review.getHeight());
        for (int row = 0; row < actors.length; row++) {
            String name = actors[row] + "_model_right";
            BufferedImage still = assets.spriteFit(name, 192, 144);
            BufferedImage settled = assets.animatedSpriteFit(name, "settle_articulated_8", 192, 144, 11);
            require(Arrays.equals(still.getRGB(0,0,192,144,null,0,192), settled.getRGB(0,0,192,144,null,0,192)), "Stop changes standing pose");
            for (int facing : new int[]{-1, 1}) for (boolean robes : new boolean[]{false, true}) {
                ArticulatedWalkingMotion rig = new ArticulatedWalkingMotion(still, facing, robes);
                for (int which = 0; which < 2; which++) {
                    double upper = -1, lower = -1;
                    for (int f = 0; f < 32; f++) {
                        var p = rig.leg(f, which);
                        double u = Math.hypot(p.kneeX() - p.hipX(), p.kneeY() - p.hipY());
                        double l = Math.hypot(p.ankleX() - p.kneeX(), p.ankleY() - p.kneeY());
                        if (f == 0) { upper = u; lower = l; }
                        require(Math.abs(u - upper) < .01 && Math.abs(l - lower) < .01, "Leg shortened during stride: " + name);
                        require((p.kneeX() - (p.hipX() + p.ankleX()) / 2) * facing >= 0, "Knee bends backwards");
                    }
                }
                require(Math.abs(rig.leg(0, 0).ankleX() - rig.leg(16, 0).ankleX()) > 20, "Near leg does not change lead");
                require(Math.abs(rig.leg(0, 1).ankleX() - rig.leg(16, 1).ankleX()) > 20, "Far leg does not change lead");
                require(rig.leg(8, 1).ankleY() < rig.leg(0, 1).ankleY() - 5, "Far leg does not lift");
                require(rig.leg(24, 0).ankleY() < rig.leg(0, 0).ankleY() - 5, "Near leg does not lift");
            }
            ArticulatedWalkingMotion front = new ArticulatedWalkingMotion(still, 0, false);
            for (int f = 0; f < 32; f++) require(front.leg(f, 0).ankleX() < front.leg(f, 1).ankleX(), "Frontal feet cross lanes");
            g.setColor(Color.WHITE); g.drawString(actors[row] + " jointed legs / independent clothing", 4, row * 164 + 14);
            for (int f = 0; f < 8; f++) g.drawImage(assets.animatedSpriteFit(name, "walk_articulated", 192, 144, f * 4), f * 192, row * 164 + 20, null);
        }
        g.dispose(); ImageIO.write(review, "png", Path.of("tools/reviews/characters/articulated-review.png").toFile());
        System.out.println("Jointed gait passed: fixed leg lengths, forward knees, two alternating feet and separate frontal lanes.");
    }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
