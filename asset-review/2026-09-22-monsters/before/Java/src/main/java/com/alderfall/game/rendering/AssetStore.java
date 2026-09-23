package com.alderfall.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;

public final class AssetStore {
    private final Path assetsRoot;
    private final AssetCatalog catalog;
    private final Map<String, BufferedImage> cache = new HashMap<>();
    private final Map<String, BufferedImage> sourceCache = new HashMap<>();
    private final Map<String, BufferedImage> croppedSourceCache = new HashMap<>();
    private final Map<String, Integer> animationMetadataCache = new HashMap<>();

    public AssetStore(Path assetsRoot) {
        this.assetsRoot = assetsRoot;
        this.catalog = new AssetCatalog(assetsRoot);
    }

    public BufferedImage tile(char tile, int size) {
        return image(Terrain.assetName(tile), size, size);
    }

    public BufferedImage sprite(String name, int size) {
        return image(name, size, size);
    }

    public BufferedImage spriteFit(String name, int width, int height) {
        String key = "fit:" + name + ":" + width + "x" + height;
        BufferedImage cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        BufferedImage source = croppedSource(name);
        BufferedImage fitted = fit(source, width, height, pixelated(name));
        cache.put(key, fitted);
        return fitted;
    }

    public boolean hasSprite(String name) {
        if (TextileMaterialSprites.NAMES.contains(name))
            return catalog.findAsset(TextileMaterialSprites.ATLAS) != null;
        if (name.startsWith(ItemAppearance.PREFIX)) return catalog.findAsset(ItemAppearance.atlasName(name)) != null;
        return catalog.findAsset(name) != null;
    }

    public Set<String> assetNames() {
        Set<String> names = new java.util.HashSet<>(catalog.allAssetNames());
        if (catalog.findAsset(TextileMaterialSprites.ATLAS) != null) names.addAll(TextileMaterialSprites.NAMES);
        return Set.copyOf(names);
    }

    public BufferedImage effectSprite(String name, int width, int height, int frame) {
        String sheetName = name + "_anim";
        if (catalog.findAsset(sheetName) == null) {
            return spriteFit(name, width, height);
        }
        BufferedImage sheet = loadSource(sheetName);
        int frameWidth = sheet.getHeight();
        int frames = Math.max(1, sheet.getWidth() / Math.max(1, frameWidth));
        int selectedFrame = Math.floorMod(frame, frames);
        String key = "effect:" + sheetName + ":" + width + "x" + height + ":" + selectedFrame;
        BufferedImage cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        int x = selectedFrame * frameWidth;
        int sourceWidth = Math.min(frameWidth, sheet.getWidth() - x);
        BufferedImage source = cropTransparent(sheet.getSubimage(x, 0, sourceWidth, sheet.getHeight()));
        BufferedImage fitted = fit(source, width, height, false);
        cache.put(key, fitted);
        return fitted;
    }

    public BufferedImage animatedSpriteFit(String name, String action, int width, int height, int frame) {
        if (action == null || action.isBlank()) {
            return spriteFit(name, width, height);
        }
        String sheetName = name + "_" + action + "_anim";
        if (catalog.findAsset(sheetName) == null) {
            return spriteFit(name, width, height);
        }
        BufferedImage sheet = loadSource(sheetName);
        int frames = animationFrameCount(sheetName, sheet, width, height);
        int frameWidth = Math.max(1, sheet.getWidth() / frames);
        int selectedFrame = Math.floorMod(frame, frames);
        String key = "anim:" + sheetName + ":" + width + "x" + height + ":" + selectedFrame;
        BufferedImage cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        int x = selectedFrame * frameWidth;
        int sourceWidth = Math.min(frameWidth, sheet.getWidth() - x);
        BufferedImage source = sheet.getSubimage(x, 0, sourceWidth, sheet.getHeight());
        if (usesMovementFit(action)) {
            source = cropTransparent(source);
        }
        BufferedImage fitted = fit(source, width, height, pixelated(name));
        cache.put(key, fitted);
        return fitted;
    }

    public int effectSpriteFrameCount(String name) {
        String sheetName = name + "_anim";
        if (catalog.findAsset(sheetName) == null) {
            return 1;
        }
        BufferedImage sheet = loadSource(sheetName);
        int frameWidth = Math.max(1, sheet.getHeight());
        return Math.max(1, sheet.getWidth() / frameWidth);
    }

    /** Fixed-grid imagegen atlas cells, extracted without trimming the atlas itself. */
    public BufferedImage effectAtlasCell(String name, int cell, int columns, int rows) {
        String key = "vfx-atlas:" + name + ":" + cell;
        BufferedImage existing = cache.get(key);
        if (existing != null) return existing;
        BufferedImage atlas = loadSource(name);
        int col = Math.floorMod(cell, columns), row = cell / columns;
        int x = col * atlas.getWidth() / columns, y = row * atlas.getHeight() / rows;
        int right = (col + 1) * atlas.getWidth() / columns, bottom = (row + 1) * atlas.getHeight() / rows;
        BufferedImage image = fit(atlas.getSubimage(x, y, right - x, bottom - y), 512, 512, true);
        cache.put(key, image);
        return image;
    }

    public int animatedSpriteFrameCount(String name, String action, int width, int height) {
        if (action == null || action.isBlank()) {
            return 1;
        }
        String sheetName = name + "_" + action + "_anim";
        if (catalog.findAsset(sheetName) == null) {
            return 1;
        }
        BufferedImage sheet = loadSource(sheetName);
        return animationFrameCount(sheetName, sheet, width, height);
    }

    public boolean hasAnimatedSprite(String name, String action) {
        if (action == null || action.isBlank()) {
            return false;
        }
        return catalog.findAsset(name + "_" + action + "_anim") != null;
    }

    public BufferedImage image(String name, int width, int height) {
        String key = name + ":" + width + "x" + height;
        BufferedImage cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        BufferedImage source = loadSource(name);
        BufferedImage scaled = scale(source, width, height, pixelated(name));
        cache.put(key, scaled);
        return scaled;
    }

    public BufferedImage imageWithoutBorder(String name, int width, int height) {
        String key = "inner:" + name + ":" + width + "x" + height;
        BufferedImage cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        BufferedImage source = loadSource(name);
        int inset = Math.max(1, Math.min(6, Math.min(source.getWidth(), source.getHeight()) / 24));
        if (source.getWidth() <= inset * 2 || source.getHeight() <= inset * 2) {
            inset = 0;
        }
        BufferedImage cropped = inset == 0
                ? source
                : source.getSubimage(inset, inset, source.getWidth() - inset * 2, source.getHeight() - inset * 2);
        BufferedImage scaled = scale(cropped, width, height, pixelated(name));
        cache.put(key, scaled);
        return scaled;
    }

    public String cacheSummary() {
        return "scaled=" + cache.size()
                + ", source=" + sourceCache.size()
                + ", cropped=" + croppedSourceCache.size()
                + ", animationMetadata=" + animationMetadataCache.size()
                + ", catalog={" + catalog.summary() + "}";
    }

    public BufferedImage cover(String name, int width, int height) {
        String key = "cover:" + name + ":" + width + "x" + height;
        BufferedImage cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        BufferedImage source = loadSource(name);
        BufferedImage scaled = coverScale(source, width, height);
        cache.put(key, scaled);
        return scaled;
    }

    private BufferedImage loadSource(String name) {
        BufferedImage cached = sourceCache.get(name);
        if (cached != null) {
            return cached;
        }
        if (TextileMaterialSprites.NAMES.contains(name)) {
            BufferedImage image = TextileMaterialSprites.extract(name, loadSource(TextileMaterialSprites.ATLAS));
            sourceCache.put(name, image);
            return image;
        }
        if (name.startsWith(ItemAppearance.PREFIX)) {
            BufferedImage image = ItemAppearance.render(name, loadSource(ItemAppearance.atlasName(name)));
            sourceCache.put(name, image);
            return image;
        }
        Path path = catalog.findAsset(name);
        if (path != null) {
            try {
                BufferedImage image = ImageIO.read(path.toFile());
                if (image != null) {
                    sourceCache.put(name, image);
                    return image;
                }
            } catch (IOException ignored) {
            }
        }
        BufferedImage image = fallback(name);
        sourceCache.put(name, image);
        return image;
    }

    private BufferedImage croppedSource(String name) {
        BufferedImage cached = croppedSourceCache.get(name);
        if (cached != null) {
            return cached;
        }
        BufferedImage cropped = cropTransparent(loadSource(name));
        croppedSourceCache.put(name, cropped);
        return cropped;
    }

    private boolean pixelated(String name) {
        if (name.endsWith("_dialogue_sprite")) {
            return false;
        }
        return name.startsWith("dungeon")
                || name.startsWith("interior_")
                || name.startsWith("npc_")
                || name.startsWith("class_")
                || name.startsWith("player")
                || isMonsterSprite(name);
    }

    private boolean isMonsterSprite(String name) {
        return switch (name) {
            case "slime", "wolf", "bat", "skeleton", "goblin", "spider", "orc", "wraith",
                    "thornling", "sand_stalker", "ice_golem", "bog_beast", "ember_imp",
                    "sheep", "mountain_goat", "doe", "stag",
                    "crystal_hare", "bramble_boar", "snow_lynx", "ember_tortoise",
                    "frost_wolf", "reed_serpent", "glass_scorpion", "stoneback_goat",
                    "moss_stag", "river_eel", "ash_scorpion", "crypt_bat",
                    "goblin_scout", "goblin_archer", "goblin_trapper", "goblin_skirmisher",
                    "goblin_shaman", "hobgoblin_guard", "goblin_warlord", "goblin_king",
                    "red_dragon", "elder_dragon", "marsh_drake", "mountain_drake",
                    "bandit_cutthroat", "bandit_archer", "bandit_captain",
                    "orc_raider", "orc_berserker", "orc_shaman", "orc_shieldbearer",
                    "swamp_troll", "frost_troll", "hill_giant", "stone_giant", "fire_giant",
                    "void_knight", "flame_herald", "frost_witch", "shadow_beast", "demon_queen" -> true;
            default -> false;
        };
    }

    private BufferedImage scale(BufferedImage source, int width, int height, boolean pixelated) {
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        configureScaling(g, pixelated);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return out;
    }

    private BufferedImage fit(BufferedImage source, int width, int height, boolean pixelated) {
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        double scale = Math.min(width / (double) source.getWidth(), height / (double) source.getHeight());
        int drawWidth = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int drawHeight = Math.max(1, (int) Math.round(source.getHeight() * scale));
        int x = (width - drawWidth) / 2;
        int y = height - drawHeight;
        Graphics2D g = out.createGraphics();
        configureScaling(g, pixelated);
        g.drawImage(source, x, y, drawWidth, drawHeight, null);
        g.dispose();
        return out;
    }

    private BufferedImage coverScale(BufferedImage source, int width, int height) {
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        double scale = Math.max(width / (double) source.getWidth(), height / (double) source.getHeight());
        int drawWidth = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int drawHeight = Math.max(1, (int) Math.round(source.getHeight() * scale));
        int x = (width - drawWidth) / 2;
        int y = (height - drawHeight) / 2;
        Graphics2D g = out.createGraphics();
        configureScaling(g, false);
        g.drawImage(source, x, y, drawWidth, drawHeight, null);
        g.dispose();
        return out;
    }

    private void configureScaling(Graphics2D g, boolean pixelated) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, pixelated
                ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
                : RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
    }

    private BufferedImage cropTransparent(BufferedImage source) {
        int minX = source.getWidth();
        int minY = source.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                if (((source.getRGB(x, y) >>> 24) & 0xff) > 8) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < minX || maxY < minY) {
            return source;
        }
        return source.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private boolean usesMovementFit(String action) {
        return switch (action) {
            case "walk", "start_walk", "stop_walk", "idle" -> true;
            default -> false;
        };
    }

    private int inferHorizontalFrameCount(BufferedImage sheet, double targetAspect) {
        int best = 1;
        double bestScore = Double.MAX_VALUE;
        int[] candidates = {12, 10, 8, 6, 5, 4, 3, 2, 1};
        for (int frames : candidates) {
            if (sheet.getWidth() % frames != 0) {
                continue;
            }
            int frameWidth = sheet.getWidth() / frames;
            double frameAspect = frameWidth / (double) Math.max(1, sheet.getHeight());
            double score = Math.abs(frameAspect - targetAspect);
            if (frames > 1) {
                score *= 0.92;
            }
            if (score < bestScore) {
                best = frames;
                bestScore = score;
            }
        }
        return Math.max(1, best);
    }

    private int animationFrameCount(String sheetName, BufferedImage sheet, int width, int height) {
        Integer metadataCount = animationFrameCountFromMetadata(sheetName);
        if (metadataCount != null) {
            return metadataCount;
        }
        return inferHorizontalFrameCount(sheet, width / (double) Math.max(1, height));
    }

    private Integer animationFrameCountFromMetadata(String sheetName) {
        Integer cached = animationMetadataCache.get(sheetName);
        if (cached != null) {
            return cached > 0 ? cached : null;
        }
        Path asset = catalog.findAsset(sheetName);
        Path path = asset == null ? null : asset.resolveSibling(sheetName + ".frames");
        if (path == null || !Files.exists(path)) {
            path = assetsRoot.resolve("animations").resolve(sheetName + ".frames");
        }
        if (!Files.exists(path)) {
            animationMetadataCache.put(sheetName, -1);
            return null;
        }
        try {
            int frames = Integer.parseInt(Files.readString(path).strip());
            animationMetadataCache.put(sheetName, frames > 0 ? frames : -1);
            return frames > 0 ? frames : null;
        } catch (IOException | NumberFormatException ignored) {
            animationMetadataCache.put(sheetName, -1);
            return null;
        }
    }

    private BufferedImage fallback(String name) {
        BufferedImage image = new BufferedImage(GameConfig.TILE, GameConfig.TILE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        int hash = Math.abs(name.hashCode());
        g.setColor(new java.awt.Color(70 + hash % 120, 60 + (hash / 7) % 120, 80 + (hash / 13) % 120));
        g.fillRoundRect(3, 3, GameConfig.TILE - 6, GameConfig.TILE - 6, 8, 8);
        g.setColor(java.awt.Color.BLACK);
        g.drawRoundRect(3, 3, GameConfig.TILE - 6, GameConfig.TILE - 6, 8, 8);
        g.dispose();
        return image;
    }
}
