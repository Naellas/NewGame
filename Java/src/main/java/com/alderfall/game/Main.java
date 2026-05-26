package com.alderfall.game;

import java.nio.file.Path;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        Path javaRoot = Path.of("").toAbsolutePath().normalize();
        Path pythonRoot = args.length > 0
                ? Path.of(args[0]).toAbsolutePath().normalize()
                : javaRoot.getParent().resolve("Python").normalize();
        GameWindow.launch(javaRoot, pythonRoot);
    }
}
