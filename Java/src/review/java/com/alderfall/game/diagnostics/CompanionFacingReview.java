package com.alderfall.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Rebuild affected preview strips and compare facing in standing and both walk modes. */
public final class CompanionFacingReview {
    public static void main(String[] args) throws Exception {
        AssetStore assets = new AssetStore(Path.of("assets"));
        Path root = Path.of("tools/reviews/characters");
        String[] actors = {"rafiq", "samir", "vesper", "aria", "lyra"};
        String[] actions = {"idle", "walk_alternate", "walk", "walk_articulated"};
        BufferedImage review = new BufferedImage(8 * 192, actors.length * actions.length * 164, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = review.createGraphics();
        g.setColor(new Color(28, 34, 40)); g.fillRect(0, 0, review.getWidth(), review.getHeight());
        for (int actor = 0; actor < actors.length; actor++) for (int action = 0; action < actions.length; action++) {
            int row = actor * actions.length + action;
            for (int column = 0; column < CharacterAnimationAudit.DIRECTIONS.size(); column++) {
                String direction = CharacterAnimationAudit.DIRECTIONS.get(column);
                String name = "npc_" + actors[actor] + "_model_" + direction;
                int count = assets.animatedSpriteFrameCount(name, actions[action], 192, 144);
                BufferedImage strip = new BufferedImage(count * 96, 72, BufferedImage.TYPE_INT_ARGB);
                Graphics2D sg = strip.createGraphics();
                sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                for (int f = 0; f < count; f++) sg.drawImage(assets.animatedSpriteFit(name, actions[action], 192, 144, f), f * 96, 0, 96, 72, null);
                sg.dispose();
                ImageIO.write(strip, "png", root.resolve(actions[action]).resolve("npc_" + actors[actor] + "_" + direction + ".png").toFile());
                g.setColor(Color.WHITE); g.drawString(actors[actor] + " " + actions[action] + " " + direction, column * 192 + 3, row * 164 + 14);
                g.drawImage(assets.animatedSpriteFit(name, actions[action], 192, 144, 0), column * 192, row * 164 + 20, null);
            }
        }
        g.dispose();
        ImageIO.write(review, "png", root.resolve("companion-facing.png").toFile());
        System.out.println("Updated five companions: eight directions, idle and all three walking modes.");
    }
}
