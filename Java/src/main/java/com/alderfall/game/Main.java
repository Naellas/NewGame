package com.alderfall.game;

import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URISyntaxException;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        Path javaRoot = resolveJavaRoot();
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
