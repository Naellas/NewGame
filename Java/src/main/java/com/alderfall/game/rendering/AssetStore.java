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
    private final Map<String,BufferedImage> groundedFrames=new java.util.LinkedHashMap<>(256,.75f,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String,BufferedImage> e){return size()>512;}
    };
    private final Map<String, BufferedImage> sourceCache = new HashMap<>();
    private final Map<String, BufferedImage> croppedSourceCache = new HashMap<>();
    private final Map<String, Integer> animationMetadataCache = new HashMap<>();
    private final Map<String, java.awt.Rectangle> animationViewportCache = new HashMap<>();
    private final Map<String, WorldCharacterAnimation> worldAnimations = new java.util.LinkedHashMap<>(128,.75f,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String,WorldCharacterAnimation> e){return size()>128;}
    };

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
        if (WorldCharacterAnimation.supports(name) && hasSprite(name)) return worldFrame(name, null, width, height, 0);
        String key = "fit:" + name + ":" + width + "x" + height;
        BufferedImage cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        BufferedImage source = name.matches(".*_combat_v[234]") ? loadSource(name) : croppedSource(name);
        BufferedImage fitted = fit(source, width, height, pixelated(name));
        cache.put(key, fitted);
        return fitted;
    }

    /**
     * Aspect-preserving dimensions with a common visible bounding-box area.
     * Uses the same cached alpha crop as spriteFit, so transparent canvas padding
     * and export resolution cannot change apparent scale.
     */
    public int[] spriteAreaSize(String name, double equivalentSide) {
        BufferedImage source = croppedSource(name);
        double scale = equivalentSide / Math.sqrt((double) source.getWidth() * source.getHeight());
        return new int[]{Math.max(1, (int) Math.round(source.getWidth() * scale)),
                Math.max(1, (int) Math.round(source.getHeight() * scale))};
    }

    public boolean hasSprite(String name) {
        name = DirectionalSeating.source(name);
        if (ModularMarket.supports(name)) return catalog.findAsset(ModularMarket.BASE) != null;
        if (TextileMaterialSprites.NAMES.contains(name))
            return catalog.findAsset(TextileMaterialSprites.ATLAS) != null;
        if (name.startsWith(ItemAppearance.PREFIX)) return catalog.findAsset(ItemAppearance.atlasName(name)) != null;
        return catalog.findAsset(name) != null;
    }

    /** Tight head-and-shoulders crop, preserving the character artwork's pixel edges. */
    public BufferedImage portrait(String name, int width, int height) {
        String identity = name.replaceFirst("_(?:model|dialogue|combat).*$", "");
        if (hasSprite(identity + "_portrait")) return spriteFit(identity + "_portrait", width, height);
        String key = "portrait:" + name + ":" + width + "x" + height;
        return cache.computeIfAbsent(key, ignored -> {
            BufferedImage source = croppedSource(name);
            int cropH = Math.max(1, (int) Math.round(source.getHeight() * 0.28));
            int cropW = Math.min(source.getWidth(), Math.max(1, (int) Math.round(cropH * width / (double) height)));
            BufferedImage upperBody = source.getSubimage((source.getWidth() - cropW) / 2, 0, cropW, cropH);
            return fit(upperBody, width, height, true);
        });
    }

    public Set<String> assetNames() {
        Set<String> names = new java.util.HashSet<>(catalog.allAssetNames());
        if (catalog.findAsset(TextileMaterialSprites.ATLAS) != null) names.addAll(TextileMaterialSprites.NAMES);
        return Set.copyOf(names);
    }

    /** Canonical source family for browsing; follows the same aliases as runtime lookup. */
    public String assetRelativePath(String name) {
        name = DirectionalSeating.source(name);
        if (ModularMarket.supports(name)) name = ModularMarket.BASE;
        if (TextileMaterialSprites.NAMES.contains(name)) name = TextileMaterialSprites.ATLAS;
        if (name.startsWith(ItemAppearance.PREFIX)) name = ItemAppearance.atlasName(name);
        Path path = catalog.findAsset(name);
        return path == null ? "" : assetsRoot.relativize(path).toString().replace('\\', '/');
    }

    /** Static placement pose for arbitrary library assets, including authored animation strips. */
    public BufferedImage placementSpriteFit(String name, int width, int height) {
        Path path = catalog.findAsset(name);
        if (path == null) return spriteFit(name, width, height);
        Integer authoredFrames = animationFrameCountFromMetadata(name);
        if (authoredFrames == null && !name.endsWith("_anim")) return spriteFit(name, width, height);
        String key = "placement-pose:" + name + ":" + width + "x" + height;
        BufferedImage cached = cache.get(key);
        if (cached != null) return cached;
        // Decode just the first frame: browsing strips must not retain every full-resolution sheet.
        try (var input = ImageIO.createImageInputStream(path.toFile())) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) return spriteFit(name, width, height);
            var reader = readers.next();
            try {
                reader.setInput(input);
                int sourceWidth = reader.getWidth(0), sourceHeight = reader.getHeight(0);
                int count = authoredFrames != null ? authoredFrames : Math.max(1, sourceWidth / sourceHeight);
                int frameWidth = Math.max(1, sourceWidth / Math.min(count, sourceWidth));
                var parameters = reader.getDefaultReadParam();
                parameters.setSourceRegion(new java.awt.Rectangle(0, 0, frameWidth, sourceHeight));
                BufferedImage pose = fit(cropTransparent(reader.read(0, parameters)), width, height, pixelated(name));
                cache.put(key, pose);
                return pose;
            } finally { reader.dispose(); }
        } catch (IOException ex) {
            return spriteFit(name, width, height);
        }
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
        if (WorldCharacterAnimation.supports(name) && WorldCharacterAnimation.action(action) && hasSprite(name))
            return worldFrame(name, action, width, height, frame);
        if (action == null || action.isBlank()) {
            return spriteFit(name, width, height);
        }
        String sheetName = MonsterAttackAnimations.sheetName(name, action);
        if (catalog.findAsset(sheetName) == null) {
            if (CharacterMotion.supports(name, action) && hasSprite(name)) {
                int selected = Math.floorMod(frame, CharacterMotion.FRAMES);
                String key = "motion:" + name + ":" + action + ":" + width + "x" + height + ":" + selected;
                BufferedImage cached = cache.get(key);
                if (cached != null) return cached;
                BufferedImage motion = CharacterMotion.frame(spriteFit(name, width, height), action, selected);
                cache.put(key, motion);
                return motion;
            }
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
        java.awt.Rectangle viewport = animationViewport(sheetName, frameWidth, sheet.getHeight());
        if (viewport.width <= sourceWidth && viewport.x + viewport.width <= sourceWidth) {
            source = source.getSubimage(viewport.x, viewport.y, viewport.width, viewport.height);
        }
        // All poses use one viewport. Per-frame trimming erased foot lift and changed body scale.
        if (WorldCharacterAnimation.supports(name)) source = WorldCharacterAnimation.actionPose(name, source);
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
        if (WorldCharacterAnimation.supports(name) && WorldCharacterAnimation.action(action) && hasSprite(name))
            return WorldCharacterAnimation.frameCount(action);
        String sheetName = MonsterAttackAnimations.sheetName(name, action);
        if (catalog.findAsset(sheetName) == null) {
            return CharacterMotion.supports(name, action) && hasSprite(name) ? CharacterMotion.FRAMES : 1;
        }
        BufferedImage sheet = loadSource(sheetName);
        return animationFrameCount(sheetName, sheet, width, height);
    }

    public boolean hasAnimatedSprite(String name, String action) {
        if (action == null || action.isBlank()) {
            return false;
        }
        return (WorldCharacterAnimation.supports(name) && WorldCharacterAnimation.action(action) && hasSprite(name))
                || catalog.findAsset(MonsterAttackAnimations.sheetName(name, action)) != null
                || (CharacterMotion.supports(name, action) && hasSprite(name));
    }

    public boolean hasAuthoredAnimation(String name, String action) {
        return action != null && !action.isBlank() && catalog.findAsset(MonsterAttackAnimations.sheetName(name, action)) != null;
    }

    private BufferedImage worldFrame(String name, String action, int width, int height, int frame) {
        int selected = Math.floorMod(frame, WorldCharacterAnimation.frameCount(action));
        Map<String,BufferedImage> frameCache=action!=null&&action.contains("grounded")?groundedFrames:cache;
        String key = "world-motion:" + name + ":" + action + ":" + width + "x" + height + ":" + selected;
        BufferedImage existing = frameCache.get(key);
        if (existing != null) return existing;
        WorldCharacterAnimation sequence = worldAnimations.get(name);
        if (sequence == null) {
            String sourceName = worldDirectionSource(name);
            BufferedImage still = loadSource(sourceName);
            String sheetName = sourceName + "_walk_anim";
            // Use the original adult standing model for players and town residents.
            // The v3 player walk atlas introduced oversized heads and a permanent stride pose.
            boolean hasSheet = WorldCharacterAnimation.companion(sourceName) && catalog.findAsset(sheetName) != null;
            boolean side = name.endsWith("_left") || name.endsWith("_right");
            String expanded = name.replaceFirst("_model_.*$", "_model_right_walk_v4_anim");
            boolean authoredSide = side && catalog.findAsset(expanded) != null;
            if (authoredSide) { sheetName = expanded; hasSheet = true; }
            BufferedImage sheet = hasSheet ? loadSource(sheetName) : still;
            int count = hasSheet ? animationFrameCount(sheetName, sheet, still.getWidth(), still.getHeight()) : 1;
            if (mirroredWorldDirection(name)) still = flipCells(still, 1);
            if (authoredSide) {
                if (name.endsWith("_left")) sheet = flipCells(sheet, count);
            } else if (mirroredWorldDirection(name)) {
                sheet = flipCells(sheet, count);
            }
            sequence = new WorldCharacterAnimation(name, still, sheet, count, sheetName.endsWith("_walk_v4_anim"));
            worldAnimations.put(name, sequence);
        }
        BufferedImage fitted = fit(sequence.frame(action, selected), width, height, true);
        frameCache.put(key, fitted);
        return fitted;
    }

    private String worldDirectionSource(String name) {
        // Reviewed diagonal stills: these rear quarter views were labelled backwards.
        if (name.matches("(?:class_(?:knight|mage|ranger|cleric|rogue)|npc_(?:aria|cassia))_model_up_(?:left|right)"))
            return name.endsWith("_left") ? name.substring(0, name.length() - 4) + "right"
                    : name.substring(0, name.length() - 5) + "left";
        // These standing side views were labelled backwards. The v4 walking atlas
        // is already correct and is selected independently above.
        for (String actor : new String[]{"npc_rafiq", "npc_samir", "npc_vesper"}) {
            if (name.equals(actor + "_model_right")) return actor + "_model_left";
            if (name.equals(actor + "_model_left")) return actor + "_model_right";
        }
        if (name.equals("npc_rafiq_model_down_right")) return "npc_rafiq_model_down_left";
        if (name.equals("npc_vesper_model_up_right")) return "npc_vesper_model_up_left";
        // These imports have reversed/inconsistent side-facing rows. Use the verified
        // left-facing 'right' row and mirror its cells for screen-right (not the whole strip).
        for (String actor : new String[]{"npc_calder", "npc_lyra", "npc_maera"}) {
            if (name.equals(actor + "_model_left")) return actor + "_model_right";
        }
        return name;
    }

    private boolean mirroredWorldDirection(String name) {
        return name.matches("npc_(?:calder|lyra|maera)_model_right")
                || name.matches("(?:class_(?:knight|mage|ranger|cleric|rogue)|npc_(?:aria|cassia|seraphine))_model_down_right")
                || name.equals("npc_rafiq_model_down_right") || name.equals("npc_vesper_model_up_right");
    }

    private BufferedImage flipCells(BufferedImage source, int count) {
        BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics(); int w = source.getWidth() / count, h = source.getHeight();
        for (int i = 0; i < count; i++) g.drawImage(source, (i + 1) * w, 0, i * w, h, i * w, 0, (i + 1) * w, h, null);
        g.dispose(); return result;
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
        name = DirectionalSeating.source(name);
        BufferedImage cached = sourceCache.get(name);
        if (cached != null) {
            return cached;
        }
        if (ModularMarket.supports(name) && !name.equals(ModularMarket.BASE)) {
            BufferedImage image = ModularMarket.tint(loadSource(ModularMarket.BASE), name);
            sourceCache.put(name, image);
            return image;
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

    // Optional shared viewport keeps padded animation poses at one stable scale and baseline.
    // Unlike trimming each frame separately, this preserves intentional attack movement.
    private java.awt.Rectangle animationViewport(String sheetName, int width, int height) {
        return animationViewportCache.computeIfAbsent(sheetName + ":" + width + "x" + height, key -> {
            java.awt.Rectangle full = new java.awt.Rectangle(0, 0, width, height);
            Path asset = catalog.findAsset(sheetName);
            if (asset == null) return full;
            Path metadata = asset.resolveSibling(sheetName + ".framebounds");
            if (!Files.exists(metadata)) {
                boolean movement = sheetName.matches(".*_(walk|start_walk|stop_walk|idle)_anim");
                if (!movement) return full;
                BufferedImage sheet = loadSource(sheetName);
                int minX = width, minY = height, maxX = -1, maxY = -1;
                for (int x = 0; x + width <= sheet.getWidth(); x += width) {
                    for (int py = 0; py < height; py++) {
                        for (int px = 0; px < width; px++) {
                            if ((sheet.getRGB(x + px, py) >>> 24) > 12) {
                                minX = Math.min(minX, px); minY = Math.min(minY, py);
                                maxX = Math.max(maxX, px); maxY = Math.max(maxY, py);
                            }
                        }
                    }
                }
                return maxX < minX ? full : new java.awt.Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
            }
            try {
                String[] parts = Files.readString(metadata).strip().split("\\s+");
                if (parts.length != 4) return full;
                java.awt.Rectangle bounds = new java.awt.Rectangle(Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                return bounds.width > 0 && bounds.height > 0 && full.contains(bounds) ? bounds : full;
            } catch (IOException | NumberFormatException ignored) {
                return full;
            }
        });
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
            path = assetsRoot.resolve("characters/shared/animations").resolve(sheetName + ".frames");
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
