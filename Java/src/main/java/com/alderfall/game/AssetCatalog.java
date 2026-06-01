package com.alderfall.game;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

final class AssetCatalog {
    private static final String[] FOLDERS = {
            "terrain", "player", "monsters", "npcs", "items", "city", "road", "battle", "weather",
            "player_village",
            "grass", "forest", "desert", "marsh", "mountain", "tundra", "badlands", "beach", "water", "locations",
            "deco", "effects", "interiors", "animations"
    };

    private final Path assetsRoot;
    private final Map<String, Path> assetPaths = new HashMap<>();
    private final Set<String> missingAssets = new HashSet<>();

    AssetCatalog(Path assetsRoot) {
        this.assetsRoot = assetsRoot;
    }

    Path findAsset(String name) {
        Path cached = assetPaths.get(name);
        if (cached != null) {
            return cached;
        }
        if (missingAssets.contains(name)) {
            return null;
        }

        Path path = preferredAsset(name);
        if (path == null) {
            path = searchAssetFolders(name);
        }
        if (path == null) {
            path = rootAsset(name);
        }

        if (path == null) {
            missingAssets.add(name);
        } else {
            assetPaths.put(name, path);
        }
        return path;
    }

    private Path preferredAsset(String name) {
        if ("mountain_massif".equals(name)) {
            Path path = assetsRoot.resolve("mountain").resolve(name + ".png");
            if (Files.exists(path)) {
                return path;
            }
        }
        return null;
    }

    private Path searchAssetFolders(String name) {
        for (String folder : FOLDERS) {
            Path folderPath = assetsRoot.resolve(folder);
            Path path = folderPath.resolve(name + ".png");
            if (Files.exists(path)) {
                return path;
            }
            path = searchAssetFolderRecursively(folderPath, name);
            if (path != null) {
                return path;
            }
        }
        return null;
    }

    private Path searchAssetFolderRecursively(Path folder, String name) {
        if (!Files.isDirectory(folder)) {
            return null;
        }
        String fileName = name + ".png";
        try (Stream<Path> paths = Files.walk(folder)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals(fileName))
                    .findFirst()
                    .orElse(null);
        } catch (IOException ignored) {
            return null;
        }
    }

    private Path rootAsset(String name) {
        Path path = assetsRoot.resolve(name + ".png");
        return Files.exists(path) ? path : null;
    }
}
