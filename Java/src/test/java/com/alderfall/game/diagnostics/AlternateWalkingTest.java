package com.alderfall.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Arrays;

/** Test individual limb identities, loop continuity and isolation from the original walk. */
public final class AlternateWalkingTest {
    public static void main(String[] args) throws Exception {
        BufferedImage actor = new BufferedImage(192, 144, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = actor.createGraphics();
        g.setColor(Color.GRAY); g.fillRect(83, 6, 26, 75);
        g.setColor(Color.RED); g.fillRect(77, 82, 13, 55);
        g.setColor(Color.BLUE); g.fillRect(102, 82, 13, 55);
        g.setColor(Color.GREEN); g.fillRect(70, 43, 10, 28);
        g.setColor(Color.YELLOW); g.fillRect(114, 43, 10, 28); g.dispose();
        for (int facing : new int[]{-1, 1}) {
            BufferedImage first = AlternateWalkingMotion.frame(actor, 0, facing, false);
            BufferedImage opposite = AlternateWalkingMotion.frame(actor, 16, facing, false);
            require((point(first, Color.RED)[0] - point(opposite, Color.RED)[0]) * facing > 20, "First leg never changes lead");
            require((point(opposite, Color.BLUE)[0] - point(first, Color.BLUE)[0]) * facing > 20, "Second leg never changes lead");
            BufferedImage quarter = AlternateWalkingMotion.frame(actor, 8, facing, false);
            BufferedImage threeQuarter = AlternateWalkingMotion.frame(actor, 24, facing, false);
            require(point(quarter, Color.BLUE)[1] < point(first, Color.BLUE)[1] - 7, "Second leg has no return lift");
            require(point(threeQuarter, Color.RED)[1] < point(first, Color.RED)[1] - 7, "First leg has no return lift");
            require(Math.abs(point(quarter, Color.RED)[1] - point(first, Color.RED)[1]) <= 1, "Planted foot lifts");
            require(point(quarter, Color.GREEN)[0] < point(threeQuarter, Color.GREEN)[0] - 3, "Left arm has no swing");
            require(point(quarter, Color.YELLOW)[0] > point(threeQuarter, Color.YELLOW)[0] + 3, "Right arm has no opposite swing");
            for (int frame = 0; frame < 32; frame++) {
                BufferedImage a = AlternateWalkingMotion.frame(actor, frame, facing, false);
                BufferedImage b = AlternateWalkingMotion.frame(actor, frame + 1, facing, false);
                for (Color limb : new Color[]{Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW})
                    require(Math.abs(point(a, limb)[0] - point(b, limb)[0]) < 6, "Sudden limb jump, including loop seam");
            }
        }
        LocomotionClock clock = new LocomotionClock(true);
        clock.selectAt(0, 0, 0);
        require(clock.selectAt(1, .1, 0).action().equals("walk_alternate"), "Alternate launcher not routed");
        require(clock.selectAt(2, .1, 0).action().startsWith("settle_alternate_"), "Wrong stop transition");
        require(clock.selectAt(11, .1, 0).action().equals("idle"), "Alternate never settles");
        AssetStore assets = new AssetStore(Path.of("assets"));
        for (String actorName : CharacterAnimationAudit.ACTORS) {
            String name = actorName + "_model_right";
            int[] before = pixels(assets.animatedSpriteFit(name, "walk", 192, 144, 8));
            BufferedImage alt = assets.animatedSpriteFit(name, "walk_alternate", 192, 144, 8);
            require(!Arrays.equals(before, pixels(alt)), "Alternate equals original: " + name);
            require(Arrays.equals(before, pixels(assets.animatedSpriteFit(name, "walk", 192, 144, 8))), "Original was replaced");
            require(Arrays.equals(pixels(assets.spriteFit(name, 192, 144)),
                    pixels(assets.animatedSpriteFit(name, "settle_alternate_8", 192, 144, 11))), "Alternate stop does not reach idle");
        }
        BufferedImage contact = new BufferedImage(8 * 192, 5 * 164, BufferedImage.TYPE_INT_RGB);
        Graphics2D review = contact.createGraphics();
        review.setColor(new Color(28, 34, 40)); review.fillRect(0, 0, contact.getWidth(), contact.getHeight());
        for (int row = 0; row < 5; row++) {
            String name = CharacterAnimationAudit.ACTORS.get(row);
            review.setColor(Color.WHITE); review.drawString(name + " alternate", 4, row * 164 + 14);
            for (int f = 0; f < 8; f++) review.drawImage(assets.animatedSpriteFit(name + "_model_right",
                    "walk_alternate", 192, 144, f * 4), f * 192, row * 164 + 20, null);
        }
        review.dispose();
        javax.imageio.ImageIO.write(contact, "png", Path.of("tools/reviews/characters/alternate-limb-review.png").toFile());
        System.out.println("Alternate walk passed: both legs change lead and lift, opposing arms, continuous loop, separate caches and settling.");
    }

    private static int[] pixels(BufferedImage image) { return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()); }
    private static double[] point(BufferedImage image, Color color) {
        int bottom = -1, count = 0; double total = 0;
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++)
            if (image.getRGB(x, y) == color.getRGB()) { bottom = Math.max(bottom, y); total += x; count++; }
        require(count > 0, "Limb disappeared: " + color);
        return new double[]{total / count, bottom};
    }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
