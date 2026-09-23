package com.alderfall.game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.imageio.ImageIO;

/** Headless integration checks and a contact sheet of the actual cached UI sprites. */
public final class ItemAppearanceTest {
    public static void main(String[] args) throws Exception {
        AssetStore assets = new AssetStore(Path.of("assets"));
        String copper = gear("sword", "STANDARD", "copper_ingot", "oak_wood");
        String steel = gear("sword", "STANDARD", "steel_ingot", "oak_wood");
        String hilt = gear("sword", "STANDARD", "copper_ingot", "birch_wood");
        BufferedImage a = icon(assets, copper), b = icon(assets, steel), c = icon(assets, hilt);
        require(!Arrays.equals(pixels(a), pixels(b)), "Material must change rendered pixels");
        require(!Arrays.equals(pixels(a), pixels(c)), "Hilt must change rendered pixels independently");
        require(a == icon(assets, copper), "Repeated icons must use cache");
        String uncommon = gear("sword", "FINE", "steel_ingot", "oak_wood");
        String rare = gear("sword", "RARE", "steel_ingot", "oak_wood");
        String masterwork = gear("sword", "MASTERWORK", "steel_ingot", "oak_wood");
        String legendary = gear("sword", "LEGENDARY", "steel_ingot", "oak_wood");
        require(!Arrays.equals(pixels(b), pixels(icon(assets, uncommon))), "Uncommon silhouette differs from Common");
        require(!Arrays.equals(pixels(icon(assets, rare)), pixels(icon(assets, masterwork))), "Masterwork silhouette differs from Rare");
        require(!Arrays.equals(pixels(icon(assets, masterwork)), pixels(icon(assets, legendary))), "Legendary silhouette differs from Masterwork");
        require(EquipmentCatalog.equipment(copper).name().startsWith("Common"), "Standard save key displays Common");
        require(EquipmentCatalog.equipment(uncommon).name().startsWith("Uncommon"), "Fine save key displays Uncommon");
        String unique = "new_weapon_the_bellringer";
        require(GameData.itemIcon(unique).equals(EquipmentCatalog.equipment(unique).icon()), "Unique keeps bespoke art");
        require(!GameData.itemIcon("iron_sword").equals(GameData.itemIcon("rusty_sword")), "Legacy material appearance");
        for (String atlasName : new String[]{"equipment_weapons_tiers.png", "equipment_gear_tiers.png"}) {
            BufferedImage atlas = ImageIO.read(Path.of("assets/items", atlasName).toFile());
            require(atlas.getColorModel().hasAlpha(), atlasName + " requires alpha");
            long transparent = Arrays.stream(pixels(atlas)).filter(p -> (p >>> 24) == 0).count();
            require(transparent > atlas.getWidth() * atlas.getHeight() / 4, atlasName + " has no opaque icon background");
        }
        for (AssemblyCrafting.Blueprint blueprint : AssemblyCrafting.BLUEPRINTS) {
            String main = sampleMaterial(blueprint.slots().get(0));
            String secondary = sampleMaterial(blueprint.slots().get(1));
            for (String quality : new String[]{"STANDARD", "FINE", "MASTERWORK", "RARE", "LEGENDARY"}) {
                String key = gear(blueprint.id(), quality, main, secondary);
                require(EquipmentCatalog.equipment(key) != null, "Valid saved assembly key: " + key);
                require(assets.hasSprite(GameData.itemIcon(key)), "Resolved atlas");
                require(Arrays.stream(pixels(icon(assets, key))).anyMatch(p -> (p >>> 24) > 0), "Visible sprite");
            }
        }
        for (String shopId : GameData.SHOPS.keySet()) {
            for (String key : AssemblyCrafting.vendorStock(shopId)) {
                require(GameData.equipment(key) != null, shopId + " vendor item must resolve");
                require(assets.hasSprite(GameData.itemIcon(key)), shopId + " vendor item must have art");
            }
        }
        BufferedImage preview = new BufferedImage(1200, 660, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = preview.createGraphics();
        g.setColor(new Color(22, 25, 33)); g.fillRect(0, 0, 840, 420);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        String[] shapes = {"sword", "dagger", "axe", "spear", "staff", "bow", "mail", "robe", "leather_armor", "ring"};
        String[] qualities = {"STANDARD", "FINE", "RARE", "MASTERWORK", "LEGENDARY"};
        for (int row = 0; row < qualities.length; row++) for (int col = 0; col < shapes.length; col++) {
            String shape = shapes[col];
            String main = switch (shape) {
                case "staff", "bow" -> "oak_wood";
                case "robe" -> "wool";
                case "leather_armor" -> "skin";
                default -> "steel_ingot";
            };
            String secondary = switch (shape) {
                case "staff", "ring" -> "ember_shard";
                case "mail", "robe", "leather_armor", "bow" -> "wool";
                default -> "oak_wood";
            };
            g.drawImage(icon(assets, gear(shape, qualities[row], main, secondary)), col * 120 + 12, row * 130, null);
            g.setColor(Color.WHITE); g.drawString(shape, col * 120 + 12, row * 130 + 104);
            g.setColor(Color.LIGHT_GRAY); g.drawString(EquipmentCatalog.equipment(gear(shape, qualities[row], main, secondary)).name().split(" ")[0], col * 120 + 12, row * 130 + 122);
        }
        g.dispose();
        Path output = Path.of("../asset-review/item-appearance/material-quality-preview.png");
        Files.createDirectories(output.getParent()); ImageIO.write(preview, "png", output.toFile());
        System.out.println("ItemAppearanceTest passed; preview: " + output);
    }
    private static String sampleMaterial(AssemblyCrafting.Slot slot) {
        return switch (slot) {
            case BLADE, HEAD, PLATES, SETTING -> "steel_ingot";
            case HILT, SHAFT -> "oak_wood";
            case GEM -> "frost_shard";
            case LEATHER -> "skin";
            default -> "wool";
        };
    }
    private static String gear(String shape, String quality, String main, String secondary) {
        return "gear1~" + shape + "~" + quality + "~" + main + "~" + quality + "+" + secondary + "~" + quality + "+-+-";
    }
    private static BufferedImage icon(AssetStore assets, String key) { return assets.sprite(GameData.itemIcon(key), 96); }
    private static int[] pixels(BufferedImage image) { return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
