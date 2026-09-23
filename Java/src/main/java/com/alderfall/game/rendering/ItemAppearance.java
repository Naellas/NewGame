package com.alderfall.game;

import com.alderfall.game.inventory.Equipment;
import com.alderfall.game.inventory.ItemRarity;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;

/** Material channels in the imagegen atlas: neutral = body, red = fittings, gold = ornament. */
final class ItemAppearance {
    static final String PREFIX = "equipment-look|";
    private static final List<String> SHAPES = List.of("sword", "dagger", "axe", "spear", "staff",
            "bow", "mail", "robe", "leather_armor", "ring",
            "helmet", "pauldrons", "gloves", "belt", "leggings", "boots", "necklace", "shield");

    static String icon(Equipment equipment) {
        if (equipment.rarity() == ItemRarity.UNIQUE || !equipment.uniqueEffect().isBlank()) return equipment.icon();
        String key = equipment.key();
        boolean assembled = key.startsWith("gear1~");
        String shape, main, secondary = "skin", ornament = "gold_ingot";
        int quality = 0;
        if (assembled) {
            String[] fields = key.split("~", 4);
            shape = fields[1];
            quality = visualTier(AssemblyCrafting.Quality.valueOf(fields[2]));
            String[] parts = fields[3].split("\\+");
            main = parts[0].split("~")[0];
            secondary = parts[1].split("~")[0];
            if (!parts[2].equals("-")) ornament = parts[2].split("~")[0];
        } else {
            String base = key.split("__", 2)[0];
            shape = shape(base);
            if (shape == null) return equipment.icon();
            main = legacyMaterial(base, shape);
        }
        int materialTier = switch (equipment.rarity()) {
            case COMMON -> 0;
            case UNCOMMON -> 1;
            case RARE -> 2;
            case LEGENDARY -> 4;
            default -> 0;
        };
        // Crafted quality defines the silhouette; material supplies the independent recolor.
        // Legacy equipment has no component quality, so its rarity remains the visual tier.
        int tier = assembled ? quality : materialTier;
        return PREFIX + shape + "|" + tier + "|" + main + "|" + secondary + "|" + ornament;
    }

    private static int visualTier(AssemblyCrafting.Quality quality) {
        return switch (quality) {
            case STANDARD -> 0;
            case FINE -> 1;
            case RARE -> 2;
            case MASTERWORK -> 3;
            case LEGENDARY -> 4;
        };
    }

    static String atlasName(String descriptor) {
        String[] fields = descriptor.split("\\|", -1);
        if (fields.length > 1 && SHAPES.indexOf(fields[1]) >= 10) {
            return "crafting_slots_atlas";
        }
        return fields.length > 1 && SHAPES.indexOf(fields[1]) < 5 ? "equipment_weapons_tiers" : "equipment_gear_tiers";
    }

    private static String shape(String key) {
        if (key.contains("dagger") || key.contains("knife") || key.contains("dirk") || key.contains("kris")) return "dagger";
        if (key.contains("sword") || key.contains("blade") || key.contains("saber") || key.contains("cutlass")
                || key.contains("rapier") || key.contains("cleaver") || key.contains("falchion") || key.equals("stormbrand")) return "sword";
        if (key.contains("bow")) return "bow";
        if (key.contains("staff") || key.contains("wand") || key.contains("scepter")) return "staff";
        if (key.contains("axe") || key.contains("mace") || key.contains("hammer") || key.contains("flail") || key.contains("sickle")) return "axe";
        if (key.contains("spear") || key.contains("pike") || key.contains("halberd") || key.contains("glaive")
                || key.contains("trident") || key.contains("lance")) return "spear";
        if (key.contains("robe") || key.contains("cloak") || key.contains("mantle") || key.contains("cowl")) return "robe";
        if (key.contains("leather") || key.contains("vest") || key.contains("coat") || key.contains("jacket") || key.contains("jerkin")) return "leather_armor";
        if (key.contains("plate") || key.contains("mail") || key.contains("cuirass")) return "mail";
        if (key.contains("ring") || key.contains("signet") || key.contains("band")) return "ring";
        return null;
    }

    private static String legacyMaterial(String key, String shape) {
        if (key.startsWith("rusty_")) return "rust";
        // Longest matching name wins (bog iron before iron, froststeel before steel).
        String match = "";
        for (MaterialCatalog.Material material : MaterialCatalog.all()) {
            String stem = material.key().replace("_ingot", "").replace("_ore", "").replace("_wood", "");
            if (key.contains(stem) && stem.length() > match.length()) match = stem;
        }
        if (!match.isEmpty()) {
            if (MaterialCatalog.get(match + "_ingot") != null) return match + "_ingot";
            if (MaterialCatalog.get(match + "_wood") != null) return match + "_wood";
            return match;
        }
        return switch (shape) {
            case "bow", "staff" -> "oak_wood";
            case "robe" -> "wool";
            case "leather_armor" -> "skin";
            default -> "steel_ingot";
        };
    }

    static Color materialColor(String key) {
        return new Color(switch (key) {
            case "linen_cloth" -> 0xe2d1ac;
            case "wool_cloth" -> 0x85906b;
            case "silk_cloth" -> 0xbccfe8;
            case "moonweave_cloth" -> 0x3d468c;
            case "starweave_cloth" -> 0x8257bd;
            case "tanned_leather" -> 0xbe834b;
            case "hardened_leather" -> 0x71503e;
            case "reinforced_leather" -> 0x9c5038;
            case "frosthide_leather" -> 0x7894b3;
            case "dragonscale_leather" -> 0xb53f35;
            case "spider_silk" -> 0xdfd6bd;
            case "thick_hide" -> 0x765840;
            case "obsidian_shard", "obsidian_glass" -> 0x59526f;
            case "raw_amber", "polished_amber" -> 0xeab44c;
            case "rock_salt", "refined_salt" -> 0xebe3d3;
            case "cypress_wood" -> 0xa8654a;
            case "rust" -> 0x996044;
            case "copper_ingot" -> 0xd48a57;
            case "tin_ingot" -> 0xc5c9be;
            case "iron_ingot" -> 0x929ba5;
            case "bog_iron_ingot" -> 0x79765a;
            case "bronze_ingot" -> 0xb59650;
            case "steel_ingot", "steel_scrap" -> 0xc2d0dc;
            case "silver_ingot" -> 0xe5e8f1;
            case "gold_ingot", "sunmetal_ingot" -> 0xf4ca59;
            case "cobalt_ingot" -> 0x587aca;
            case "mithril_ingot" -> 0x9cdde1;
            case "froststeel_ingot", "frost_shard" -> 0x7ebeee;
            case "emberite_ingot", "ember_shard" -> 0xe77348;
            case "verdant_ingot" -> 0x79c493;
            case "adamantite_ingot" -> 0x9382b8;
            case "oak_wood", "wood" -> 0xaa7843;
            case "pine_wood", "palm_wood" -> 0xc59b60;
            case "birch_wood", "ash_wood" -> 0xd7c8a2;
            case "maple_wood", "fruitwood" -> 0xb7724c;
            case "willow_wood" -> 0x9aa477;
            case "deadwood", "ironwood" -> 0x686263;
            case "elder_wood", "magic_wood", "enchanted_bark", "ancient_wood" -> 0x9a86c1;
            case "skin" -> 0xa9734c;
            case "scale" -> 0x6eaa95;
            case "wool", "bone", "seashell" -> 0xe0d5b7;
            case "horn" -> 0x9c8870;
            case "plant_fiber", "palm_frond" -> 0xb3ba79;
            case "crystal_dust" -> 0xc1a8e8;
            case "venom_sac", "glowroot", "herb_leaf" -> 0x90bd5d;
            default -> 0xb7a894;
        });
    }

    static BufferedImage render(String descriptor, BufferedImage atlas) {
        String[] fields = descriptor.split("\\|");
        int shape = SHAPES.indexOf(fields[1]);
        int tier = Integer.parseInt(fields[2]);
        int col = shape % 5, row = tier;
        boolean individual = shape >= 10;
        BufferedImage source = individual ? extract(atlas, (shape - 10) % 4, (shape - 10) / 4, 4, 2) : extract(atlas, col, row);
        int width = source.getWidth(), height = source.getHeight();
        Color body = materialColor(fields[3]), fitting = materialColor(fields[4]), trim = materialColor(fields[5]);
        BufferedImage cell = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            int pixel = source.getRGB(x, y);
            int a = pixel >>> 24, r = pixel >> 16 & 255, g = pixel >> 8 & 255, b = pixel & 255;
            if (a == 0) continue;
            boolean red = r > g * 1.35 && r > b * 1.35;
            boolean gold = !red && r > b * 1.3 && g > b * 1.2;
            Color target = red ? fitting : gold ? trim : body;
            double shade = Math.max(r, Math.max(g, b)) / 255.0;
            shade = Math.min(1.15, shade * (individual ? 0.92 + tier * 0.04 : 1.0));
            int rr = Math.min(255, (int) (target.getRed() * shade));
            int gg = Math.min(255, (int) (target.getGreen() * shade));
            int bb = Math.min(255, (int) (target.getBlue() * shade));
            cell.setRGB(x, y, a << 24 | rr << 16 | gg << 8 | bb);
        }
        BufferedImage result = new BufferedImage(96, 96, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        double scale = Math.min(86.0 / width, 86.0 / height);
        int w = (int) (width * scale), h = (int) (height * scale);
        graphics.drawImage(cell, (96 - w) / 2, (96 - h) / 2, w, h, null);
        graphics.dispose();
        return result;
    }

    /** Generated sheets have approximate cell boundaries. Keep the largest connected object
     * in an expanded cell, excluding fragments from adjacent icons, then fit its actual bounds. */
    private static BufferedImage extract(BufferedImage atlas, int col, int row) {
        return extract(atlas, col, row, 5, 5);
    }
    private static BufferedImage extract(BufferedImage atlas, int col, int row, int columns, int rows) {
        int cw = atlas.getWidth() / columns, ch = atlas.getHeight() / rows;
        int x0 = col * cw, y0 = row * ch;
        int w = Math.min(cw, atlas.getWidth() - x0), h = Math.min(ch, atlas.getHeight() - y0);
        boolean[] visited = new boolean[w * h];
        ArrayList<Integer> largest = new ArrayList<>();
        ArrayList<Integer> paired = new ArrayList<>();
        for (int start = 0; start < visited.length; start++) {
            if (visited[start] || (atlas.getRGB(x0 + start % w, y0 + start / w) >>> 24) < 24) continue;
            ArrayList<Integer> component = new ArrayList<>();
            component.add(start); visited[start] = true;
            for (int i = 0; i < component.size(); i++) {
                int p = component.get(i), px = p % w, py = p / w;
                for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                    int nx = px + dx, ny = py + dy;
                    if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue;
                    int next = ny * w + nx;
                    if (visited[next]) continue;
                    visited[next] = true;
                    if ((atlas.getRGB(x0 + nx, y0 + ny) >>> 24) >= 24) component.add(next);
                }
            }
            if (component.size() > largest.size()) largest = component;
            if (columns == 4 && component.size() >= Math.max(12, w * h / 1000)) paired.addAll(component);
        }
        if (columns == 4 && !paired.isEmpty()) largest = paired;
        if (largest.isEmpty()) return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        int left = w, top = h, right = 0, bottom = 0;
        for (int p : largest) {
            left = Math.min(left, p % w); right = Math.max(right, p % w);
            top = Math.min(top, p / w); bottom = Math.max(bottom, p / w);
        }
        BufferedImage result = new BufferedImage(right - left + 1, bottom - top + 1, BufferedImage.TYPE_INT_ARGB);
        for (int p : largest) result.setRGB(p % w - left, p / w - top, atlas.getRGB(x0 + p % w, y0 + p / w));
        return result;
    }
}
