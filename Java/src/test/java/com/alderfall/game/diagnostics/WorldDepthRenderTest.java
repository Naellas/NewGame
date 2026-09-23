package com.alderfall.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Pixel assertions for depth ordering, feathering, alpha holes, and camera transforms. */
public final class WorldDepthRenderTest {
    public static void main(String[] args) throws Exception {
        WorldDepthRenderer renderer = new WorldDepthRenderer();
        BufferedImage behind = render(renderer, 170, false, false, 0);
        require(red(behind, 100, 76) > 220, "Hidden character must show through the center");
        require(red(behind, 145, 76) > 0 && red(behind, 145, 76) < 160, "Reveal must feather into scenery");
        require(red(behind, 180, 76) == 0, "Scenery outside the circle must remain opaque");
        BufferedImage front = render(renderer, 100, false, false, 0);
        require(red(front, 100, 76) == 255, "Character in front must cover scenery");
        require(red(front, 145, 76) == 0, "Character in front must not fade scenery");
        BufferedImage empty = render(renderer, 170, false, true, 0);
        require(red(empty, 100, 76) == 0, "Previous frame's circle must not persist");
        BufferedImage hole = render(renderer, 170, true, false, 0);
        require(red(hole, 100, 76) == 255, "Transparent source pixels must not hide a character");
        BufferedImage shifted = render(renderer, 170, false, false, 17);
        require(shifted.getRGB(83, 59) == behind.getRGB(100, 76), "Camera translation must move the mask with the sprite");
        // A fractional camera offset, viewport clipping and a second character exercise scratch reuse.
        BufferedImage target = new BufferedImage(220, 220, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = target.createGraphics();
        g.setClip(0, 0, 220, 220);
        g.translate(-0.5, -0.5);
        renderer.begin(96);
        renderer.scenery(g, 200, new Rectangle(-50, -50, 400, 400), layer -> {
            layer.setColor(Color.BLUE); layer.fillRect(-50, -50, 400, 400);
        });
        for (int x : new int[]{60, 140}) {
            Rectangle actor = new Rectangle(x, 40, 25, 70);
            renderer.character(g, actor, layer -> { layer.setColor(Color.RED); layer.fill(actor); });
        }
        renderer.overlay(g, layer -> { layer.setColor(Color.YELLOW); layer.fillRect(0, 0, 8, 8); });
        renderer.draw(g);
        g.dispose();
        require(red(target, 70, 73) > 220 && red(target, 150, 73) > 220, "Both characters need a reveal");
        require((target.getRGB(3, 3) & 0xffffff) == 0xffff00, "Markers must stay above the scene");
        if (args.length > 0) preview(Path.of(args[0]));
        System.out.println("WorldDepthRenderTest passed: depth, feathering, alpha, reset, camera, zoom, multiple characters, overlays");
    }

    private static BufferedImage render(WorldDepthRenderer renderer, int depth, boolean hole, boolean noCharacter, int offset) {
        BufferedImage target = new BufferedImage(220, 220, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = target.createGraphics();
        g.setColor(Color.RED); g.fillRect(0, 0, 220, 220);
        g.setClip(0, 0, 220, 220); g.translate(-offset, -offset);
        renderer.begin(48);
        renderer.scenery(g, depth, new Rectangle(10, 10, 200, 200), layer -> {
            layer.setColor(Color.BLUE);
            if (hole) { layer.fillRect(10, 10, 70, 200); layer.fillRect(120, 10, 90, 200); }
            else layer.fillRect(10, 10, 200, 200);
        });
        if (!noCharacter) renderer.character(g, new Rectangle(85, 40, 30, 75), layer -> {
            layer.setColor(Color.RED); layer.fillRect(85, 40, 30, 75);
        });
        renderer.draw(g); g.dispose();
        return target;
    }

    private static void preview(Path path) throws Exception {
        AssetStore assets = new AssetStore(Path.of("assets"));
        BufferedImage sheet = new BufferedImage(1200, 700, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        String[] sprites = {"city_building_house_wide", "deco_tree_oak", "mountain_massif"};
        for (int row = 0; row < 2; row++) for (int col = 0; col < 3; col++) {
            Graphics2D cell = (Graphics2D) g.create(col * 400, row * 350, 400, 350);
            cell.setColor(new Color(77, 94, 57)); cell.fillRect(0, 0, 400, 350);
            for (int y = 0; y < 350; y += 48) for (int x = 0; x < 400; x += 48) {
                cell.setColor(new Color(82 + (x / 48 % 2) * 4, 101, 61)); cell.fillRect(x, y, 47, 47);
            }
            WorldDepthRenderer renderer = new WorldDepthRenderer();
            renderer.begin(72);
            String sprite = sprites[col];
            renderer.scenery(cell, 284, new Rectangle(45, 20, 310, 280), layer ->
                    layer.drawImage(assets.spriteFit(sprite, 310, 280), 45, 20, null));
            int y = row == 0 ? 118 : 230;
            renderer.character(cell, new Rectangle(164, y, 72, 94), layer ->
                    layer.drawImage(assets.spriteFit("class_mage_model", 72, 94), 164, y, null));
            renderer.draw(cell);
            cell.setColor(Color.WHITE);
            cell.drawString(sprite + (row == 0 ? " / behind" : " / in front"), 12, 332);
            cell.dispose();
        }
        g.dispose();
        Files.createDirectories(path.toAbsolutePath().getParent());
        ImageIO.write(sheet, "png", path.toFile());
    }

    private static int red(BufferedImage image, int x, int y) { return (image.getRGB(x, y) >>> 16) & 255; }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
