package com.alderfall.game;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

final class AssetCatalog {
    private static final String[] FOLDERS = {
            "environments/terrain/common", "characters/player", "characters/monsters",
            "characters/npcs/townsfolk", "characters/companions", "items",
            "environments/settlements/city", "environments/terrain/roads", "environments/battle",
            "environments/settlements/player_village",
            "environments/terrain/biomes/grass", "environments/terrain/biomes/forest",
            "environments/terrain/biomes/desert", "environments/terrain/biomes/marsh",
            "environments/terrain/biomes/mountain", "environments/terrain/biomes/tundra",
            "environments/terrain/biomes/badlands", "environments/terrain/biomes/beach",
            "environments/terrain/biomes/water", "environments/locations",
            "environments/props/nature", "effects", "environments/interiors", "characters/npcs/story", "characters/shared/animations"
    };

    private final Path assetsRoot;
    private final Map<String, Path> assetPaths = new HashMap<>();
    private final Set<String> missingAssets = new HashSet<>();
    private boolean indexed;
    private int duplicateAssetStems;

    AssetCatalog(Path assetsRoot) {
        this.assetsRoot = assetsRoot;
    }

    Path findAsset(String name) {
        if (missingAssets.contains(name)) {
            return null;
        }

        Path preferred = preferredAsset(name);
        if (preferred != null) {
            assetPaths.put(name, preferred);
            return preferred;
        }

        ensureIndexed();
        Path path = assetPaths.get(name);
        if (path == null) {
            missingAssets.add(name);
        }
        return path;
    }

    String summary() {
        ensureIndexed();
        return "indexed=" + assetPaths.size()
                + ", missing=" + missingAssets.size()
                + ", duplicateStems=" + duplicateAssetStems;
    }

    Set<String> allAssetNames() {
        ensureIndexed();
        return new TreeSet<>(assetPaths.keySet());
    }

    private Path preferredAsset(String name) {
        if ("mountain_massif".equals(name)) {
            Path path = assetsRoot.resolve("environments/terrain/biomes/mountain").resolve(name + ".png");
            if (Files.exists(path)) {
                return path;
            }
        }
        return null;
    }

    private void ensureIndexed() {
        if (indexed) {
            return;
        }
        indexed = true;
        for (String folder : FOLDERS) {
            Path folderPath = assetsRoot.resolve(folder);
            indexDirectPngs(folderPath);
            indexNestedPngs(folderPath);
        }
        indexDirectPngs(assetsRoot);
    }

    private void indexDirectPngs(Path folder) {
        if (!Files.isDirectory(folder)) {
            return;
        }
        try (Stream<Path> paths = Files.list(folder)) {
            paths
                    .filter(Files::isRegularFile)
                    .filter(this::isPng)
                    .forEach(this::registerAsset);
        } catch (IOException ignored) {
        }
    }

    private void indexNestedPngs(Path folder) {
        if (!Files.isDirectory(folder)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(folder)) {
            paths
                    .filter(Files::isRegularFile)
                    .filter(this::isPng)
                    .filter(path -> !folder.equals(path.getParent()))
                    .forEach(this::registerAsset);
        } catch (IOException ignored) {
        }
    }

    private boolean isPng(Path path) {
        return path.getFileName().toString().endsWith(".png");
    }

    private void registerAsset(Path path) {
        String fileName = path.getFileName().toString();
        String stem = fileName.substring(0, fileName.length() - ".png".length());
        if (assetPaths.putIfAbsent(stem, path) != null) {
            duplicateAssetStems++;
        }
    }
}
