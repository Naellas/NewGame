package com.alderfall.game;

import java.nio.file.Path;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        Path javaRoot = Path.of("").toAbsolutePath().normalize();
        GameWindow.launch(javaRoot);
    }
}
