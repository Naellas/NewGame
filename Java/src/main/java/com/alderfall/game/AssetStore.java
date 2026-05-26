package com.alderfall.game;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public final class AssetStore {
    private final Path assetsRoot;
    private final Map<String, BufferedImage> cache = new HashMap<>();

    public AssetStore(Path assetsRoot) {
        this.assetsRoot = assetsRoot;
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
        BufferedImage source = cropTransparent(loadSource(name));
        BufferedImage fitted = fit(source, width, height);
        cache.put(key, fitted);
        return fitted;
    }

    public BufferedImage image(String name, int width, int height) {
        String key = name + ":" + width + "x" + height;
        BufferedImage cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        BufferedImage source = loadSource(name);
        BufferedImage scaled = scale(source, width, height);
        cache.put(key, scaled);
        return scaled;
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
        Path path = findAsset(name);
        if (path != null) {
            try {
                return ImageIO.read(path.toFile());
            } catch (IOException ignored) {
            }
        }
        return fallback(name);
    }

    private Path findAsset(String name) {
        String[] folders = {
                "terrain", "player", "monsters", "npcs", "items", "city", "deco", "road", "battle", "weather",
                "grass", "forest", "desert", "marsh", "mountain", "tundra", "badlands", "water", "locations"
        };
        for (String folder : folders) {
            Path path = assetsRoot.resolve(folder).resolve(name + ".png");
            if (Files.exists(path)) {
                return path;
            }
        }
        Path rootPath = assetsRoot.resolve(name + ".png");
        return Files.exists(rootPath) ? rootPath : null;
    }

    private BufferedImage scale(BufferedImage source, int width, int height) {
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        Image scaled = source.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        g.drawImage(scaled, 0, 0, null);
        g.dispose();
        return out;
    }

    private BufferedImage fit(BufferedImage source, int width, int height) {
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        double scale = Math.min(width / (double) source.getWidth(), height / (double) source.getHeight());
        int drawWidth = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int drawHeight = Math.max(1, (int) Math.round(source.getHeight() * scale));
        int x = (width - drawWidth) / 2;
        int y = height - drawHeight;
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(source.getScaledInstance(drawWidth, drawHeight, Image.SCALE_SMOOTH), x, y, null);
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
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(source.getScaledInstance(drawWidth, drawHeight, Image.SCALE_SMOOTH), x, y, null);
        g.dispose();
        return out;
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
