package com.alderfall.game;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/** Review sheet rendered with the same sprite fitting and scaling as the game. */
public final class ResourceArtworkPreview {
    private record Entry(String asset, String label) { }

    public static void main(String[] args) throws Exception {
        AssetStore assets = new AssetStore(Path.of("assets"));
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry("deco_ore_copper_vein", "Existing copper (reference)"));
        for (String material : List.of("bog_iron", "froststeel", "sunmetal", "emberite", "verdant",
                "obsidian", "amber", "rock_salt")) {
            entries.add(new Entry("deco_ore_" + material + "_vein", material.replace('_', ' ') + " deposit"));
        }
        entries.add(new Entry("deco_tree_cypress_harvestable", "Marsh cypress tree"));
        for (String material : List.of("bog_iron_ore", "froststeel_ore", "sunmetal_ore", "emberite_ore",
                "verdant_ore", "ancient_wood", "palm_wood", "cypress_wood", "obsidian_glass",
                "polished_amber", "refined_salt")) {
            entries.add(new Entry(CraftingSystem.itemIcon(material), CraftingSystem.itemName(material)));
        }
        CraftingSystem.CRAFTING_ITEMS.entrySet().stream().filter(e -> e.getKey().endsWith("_ingot"))
                .sorted(java.util.Map.Entry.comparingByKey())
                .forEach(e -> entries.add(new Entry(e.getValue().icon(), e.getValue().name())));
        int columns = 6, cellWidth = 200, cellHeight = 152;
        int rows = (entries.size() + columns - 1) / columns;
        BufferedImage sheet = new BufferedImage(columns * cellWidth, rows * cellHeight + 60, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = sheet.createGraphics();
        try {
            graphics.setColor(new Color(24, 34, 30));
            graphics.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
            graphics.setColor(new Color(229, 234, 215));
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 21));
            graphics.drawString("Alderfall resource sprites — 96 px preview / 40 px game scale", 18, 36);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            for (int i = 0; i < entries.size(); i++) {
                Entry entry = entries.get(i);
                if (!assets.hasSprite(entry.asset())) throw new IllegalStateException("Missing artwork: " + entry.asset());
                int x = (i % columns) * cellWidth, y = (i / columns) * cellHeight + 60;
                graphics.setColor(new Color(44, 61, 49));
                graphics.fillRect(x + 5, y + 4, cellWidth - 10, cellHeight - 8);
                graphics.drawImage(assets.spriteFit(entry.asset(), 96, 96), x + 12, y + 8, null);
                graphics.drawImage(assets.spriteFit(entry.asset(), 40, 40), x + 134, y + 54, null);
                graphics.setColor(new Color(229, 234, 215));
                graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
                graphics.drawString(entry.label(), x + 12, y + 127);
            }
        } finally {
            graphics.dispose();
        }
        Path output = Path.of("exports/resource-art-preview.png");
        Files.createDirectories(output.getParent());
        ImageIO.write(sheet, "png", output.toFile());
        System.out.println("Rendered " + entries.size() + " assets to " + output);
    }
}
