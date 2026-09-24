package com.alderfall.game;

final class DebugMetrics {
    private DebugMetrics() {
    }

    static long timeNanos(Runnable action) {
        long started = System.nanoTime();
        action.run();
        return System.nanoTime() - started;
    }

    static String millis(long nanos) {
        return String.format("%.2f ms", nanos / 1_000_000.0);
    }
}
