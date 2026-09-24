package com.alderfall.game;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** Presentation-only randomness; never consumes battle/world RNG. */
final class SoundVariations {
    private final Map<String, Integer> previous = new HashMap<>();

    String next(String base, int count) {
        int last = previous.getOrDefault(base, 0);
        int selected = ThreadLocalRandom.current().nextInt(1, last == 0 ? count + 1 : count);
        if (last != 0 && selected >= last) selected++;
        previous.put(base, selected);
        return base + "_" + selected;
    }
}
