package com.alderfall.game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.List;
import javax.imageio.ImageIO;

/** Read-only old/new furniture comparison at runtime-sized, aspect-preserving scale. */
public final class InteriorAssetReview {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("before-directory after-directory output-directory");
        Path beforeRoot = Path.of(args[0]), afterRoot = Path.of(args[1]), output = Path.of(args[2]);
        Files.createDirectories(output);
        List<Path> files;
        try (var paths = Files.list(beforeRoot)) {
            files = paths.filter(p -> p.toString().endsWith(".png")).sorted().toList();
        }
        AssetStore before = new AssetStore(beforeRoot), after = new AssetStore(afterRoot);
        for (int start = 0; start < files.size(); start += 18) {
            BufferedImage sheet = new BufferedImage(1200, 960, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = sheet.createGraphics();
            g.setColor(new Color(51, 62, 55)); g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            for (int i = start; i < Math.min(start + 18, files.size()); i++) {
                String id = files.get(i).getFileName().toString().replace(".png", "");
                int x = ((i - start) % 3) * 400, y = ((i - start) / 3) * 160;
                g.setColor(Color.WHITE); g.drawString(id.replace("interior_", ""), x + 12, y + 18);
                g.setColor(Color.LIGHT_GRAY); g.drawString("before", x + 42, y + 148); g.drawString("after", x + 240, y + 148);
                // Both sides receive identical fitting bounds. No sharpening or recoloring.
                g.drawImage(before.spriteFit(id, 160, 112), x + 15, y + 25, null);
                if (after.hasSprite(id)) g.drawImage(after.spriteFit(id, 160, 112), x + 215, y + 25, null);
            }
            g.dispose(); ImageIO.write(sheet, "png", output.resolve("comparison-" + start / 18 + ".png").toFile());
        }
        System.out.println("Compared " + files.size() + " furniture assets.");
    }
}
