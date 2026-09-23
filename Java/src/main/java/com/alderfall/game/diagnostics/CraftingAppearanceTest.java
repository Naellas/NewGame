package com.alderfall.game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Arrays;
import javax.imageio.ImageIO;

/** Verify functional material color channels on the additional equipment categories. */
public final class CraftingAppearanceTest {
    public static void main(String[] args) throws Exception {
        AssetStore assets = new AssetStore(Path.of("assets"));
        BufferedImage sheet = new BufferedImage(400, 8 * 118, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(new Color(20, 25, 34)); g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        int row = 0;
        for (var blueprint : AssemblyCrafting.BLUEPRINTS.subList(10, AssemblyCrafting.BLUEPRINTS.size())) {
            var primary = blueprint.slots().get(0); var secondary = blueprint.slots().get(1);
            String main = material(primary, false), otherMain = material(primary, true);
            String fitting = material(secondary, false), otherFitting = material(secondary, true);
            BufferedImage original = sprite(assets, blueprint.id(), main, fitting);
            BufferedImage bodyChanged = sprite(assets, blueprint.id(), otherMain, fitting);
            BufferedImage fittingChanged = sprite(assets, blueprint.id(), main, otherFitting);
            require(!Arrays.equals(pixels(original), pixels(bodyChanged)), blueprint.id() + " body material must change pixels");
            require(!Arrays.equals(pixels(original), pixels(fittingChanged)), blueprint.id() + " fitting/lining material must change pixels");
            g.drawImage(original, 4, row * 118, null); g.drawImage(bodyChanged, 134, row * 118, null);
            g.drawImage(fittingChanged, 264, row * 118, null);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12)); g.setColor(Color.WHITE);
            g.drawString(blueprint.label(), 8, row * 118 + 110);
            g.drawString("Body changed", 134, row * 118 + 110);
            g.drawString("Fittings changed", 264, row * 118 + 110);
            row++;
        }
        g.dispose();
        if (args.length > 0) ImageIO.write(sheet, "png", Path.of(args[0]).toFile());
        System.out.println("Crafting appearance passed: body and secondary materials independently change all eight new categories.");
    }
    private static String material(AssemblyCrafting.Slot slot, boolean alternate) {
        return switch (slot) {
            case PLATES, SETTING -> alternate ? "mithril_ingot" : "copper_ingot";
            case LEATHER -> alternate ? "scale" : "skin";
            case LINING -> alternate ? "plant_fiber" : "wool";
            case GEM -> alternate ? "ember_shard" : "crystal_dust";
            case HILT -> alternate ? "birch_wood" : "wood";
            default -> throw new AssertionError("Unhandled test slot: " + slot);
        };
    }
    private static BufferedImage sprite(AssetStore assets, String shape, String main, String fitting) {
        String key = "gear1~" + shape + "~STANDARD~" + main + "~STANDARD+" + fitting + "~STANDARD+-+-";
        require(GameData.equipment(key) != null, "Valid gear key");
        require(assets.hasSprite(GameData.itemIcon(key)), "Category artwork available: " + shape);
        return assets.sprite(GameData.itemIcon(key), 96);
    }
    private static int[] pixels(BufferedImage image) { return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
