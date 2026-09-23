package com.alderfall.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import javax.imageio.ImageIO;

/** Actual rendered-frame coverage, including repeated poses, for the complete core party. */
public final class CharacterAnimationAudit {
    public static final List<String> ACTORS = List.of("class_knight", "class_mage", "class_ranger", "class_cleric", "class_rogue",
            "npc_aria", "npc_calder", "npc_cassia", "npc_lyra", "npc_maera", "npc_rafiq", "npc_samir", "npc_seraphine", "npc_vesper");
    public static final List<String> DIRECTIONS = List.of("down", "down_left", "left", "up_left", "up", "up_right", "right", "down_right");

    public static void main(String[] args) throws Exception {
        String tag = args.length > 0 ? args[0] : "current";
        boolean alternate = tag.equals("alternate") || tag.equals("articulated") || tag.equals("grounded");
        String action = alternate ? "walk_" + tag : "walk";
        Path root = Path.of("tools/reviews/characters"); Files.createDirectories(root);
        Path walkPreview = root.resolve(action); Files.createDirectories(walkPreview);
        Path idlePreview = root.resolve("idle"); Files.createDirectories(idlePreview);
        java.util.ArrayList<String> actors = new java.util.ArrayList<>(ACTORS);
        AssetStore catalog = new AssetStore(Path.of("assets"));
        catalog.assetNames().stream().filter(n -> n.startsWith("npc_") && n.endsWith("_model_down"))
                .map(n -> n.substring(0, n.length() - "_model_down".length())).sorted()
                .filter(n -> !actors.contains(n)).forEach(actors::add);
        Files.writeString(root.resolve("world-roster.js"), "window.worldActors=[" + actors.stream()
                .map(n -> "\"" + n + "\"").collect(java.util.stream.Collectors.joining(",")) + "];\n");
        StringBuilder report = new StringBuilder("actor\tdirection\tframes\tunique_rendered_frames\n");
        BufferedImage preview = new BufferedImage(8 * 176, actors.size() * 156, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = preview.createGraphics(); g.setColor(new Color(28, 34, 40)); g.fillRect(0, 0, preview.getWidth(), preview.getHeight());
        int row = 0;
        for (String actor : actors) {
            AssetStore assets = new AssetStore(Path.of("assets"));
            for (String direction : DIRECTIONS) {
                String sprite = actor + "_model_" + direction;
                if (!assets.hasSprite(sprite)) sprite = actor + "_model_" + (direction.endsWith("left") ? "left" : direction.endsWith("right") ? "right" : direction);
                if (!assets.hasSprite(sprite)) sprite = actor + "_model_down";
                if (!assets.hasSprite(sprite)) { report.append(actor + "\t" + direction + "\t0\t0\n"); continue; }
                String renderedAction=action;
                if(tag.equals("grounded")&&direction.contains("_")&&!sprite.matches(".*_model_(?:up|down)_(?:left|right)"))
                    renderedAction="walk_grounded_10_66_"+DIRECTIONS.indexOf(direction);
                int frames = assets.animatedSpriteFrameCount(sprite, renderedAction, 192, 144);
                HashSet<Integer> hashes = new HashSet<>();
                BufferedImage strip = new BufferedImage(frames * 96, 72, BufferedImage.TYPE_INT_ARGB);
                BufferedImage idleStrip = new BufferedImage(24 * 96, 72, BufferedImage.TYPE_INT_ARGB);
                Graphics2D idleG = idleStrip.createGraphics();
                idleG.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                Graphics2D stripG = strip.createGraphics();
                stripG.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                for (int frame = 0; frame < frames; frame++) {
                    BufferedImage image = assets.animatedSpriteFit(sprite, renderedAction, 192, 144, frame);
                    hashes.add(Arrays.hashCode(image.getRGB(0, 0, 192, 144, null, 0, 192)));
                    stripG.drawImage(image, frame * 96, 0, 96, 72, null);
                    if (!alternate && frame < 24) idleG.drawImage(assets.animatedSpriteFit(sprite, "idle", 192, 144, frame), frame * 96, 0, 96, 72, null);
                }
                stripG.dispose();
                idleG.dispose();
                if (!alternate) ImageIO.write(idleStrip, "png", idlePreview.resolve(actor + "_" + direction + ".png").toFile());
                ImageIO.write(strip, "png", walkPreview.resolve(actor + "_" + direction + ".png").toFile());
                report.append(actor + "\t" + direction + "\t" + frames + "\t" + hashes.size() + "\n");
                if (direction.equals("right")) {
                    for (int i = 0; i < 8; i++) g.drawImage(assets.animatedSpriteFit(sprite, action, 192, 144, i * frames / 8), i * 176, row * 156 + 22, 171, 128, null);
                    g.setColor(Color.WHITE); g.drawString(actor + ": " + frames + " frames / " + hashes.size() + " distinct", 4, row * 156 + 16);
                }
            }
            row++;
        }
        g.dispose();
        Files.writeString(root.resolve("walking-" + tag + ".tsv"), report);
        ImageIO.write(preview, "png", root.resolve("walking-" + tag + ".png").toFile());
        if (alternate) ImageIO.write(preview.getSubimage(0, 0, preview.getWidth(), 5 * 156), "png",
                root.resolve("walking-" + tag + "-classes.png").toFile());
        System.out.println(report);
    }
}
