package com.alderfall.game;

import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URISyntaxException;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        Path javaRoot = resolveJavaRoot();
        if (args.length == 1 && "--launcher".equals(args[0])) {
            LauncherWindow.launch(javaRoot);
            return;
        }
        if (args.length > 0 && "--editor".equals(args[0])) {
            com.alderfall.game.editor.MapEditorWindow.launch(javaRoot, args.length > 1 ? Path.of(args[1]) : null);
            return;
        }
        if (args.length == 2 && "--playtest".equals(args[0])) {
            try {
                var document = com.alderfall.game.editor.MapDocumentIO.read(Path.of(args[1]));
                GameWindow.launchPlaytest(javaRoot, document);
            } catch (java.io.IOException ex) {
                throw new IllegalArgumentException("Cannot import map: " + ex.getMessage(), ex);
            }
            return;
        }
        GameWindow.launch(javaRoot);
    }

    private static Path resolveJavaRoot() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        if (isJavaRoot(cwd)) {
            return cwd;
        }
        Path nestedJava = cwd.resolve("Java").normalize();
        if (isJavaRoot(nestedJava)) {
            return nestedJava;
        }
        try {
            Path location = Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath().normalize();
            Path candidate = Files.isRegularFile(location) ? location.getParent() : location;
            if (candidate != null && candidate.getFileName() != null && "out".equals(candidate.getFileName().toString())) {
                candidate = candidate.getParent();
            }
            if (candidate != null && isJavaRoot(candidate)) {
                return candidate;
            }
        } catch (URISyntaxException | SecurityException ignored) {
        }
        return cwd;
    }

    private static boolean isJavaRoot(Path path) {
        return Files.isDirectory(path.resolve("assets"))
                && Files.isDirectory(path.resolve("config"))
                && Files.isDirectory(path.resolve("src").resolve("main").resolve("java"));
    }
}
