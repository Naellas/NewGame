package com.alderfall.game;

/** Foot anchors shared by sprites, target hitboxes and effects. HUD occupies the bottom 180px. */
final class BattleFormation {
    static int enemySize(int count) { return count >= 5 ? 164 : count >= 3 ? 196 : 220; }

    static int[] position(boolean enemy, int index, int count, int x, int y, int w, int h) {
        count = Math.max(1, Math.min(enemy ? 5 : 4, count));
        index = Math.max(0, Math.min(count - 1, index));
        int spriteW = enemy ? enemySize(count) : 150;
        int spriteH = enemy ? spriteW : 194;
        double cx;
        double feet;
        if (count == 1) {
            cx = enemy ? 0.75 : 0.25;
            feet = 0.66;
        } else if (enemy && count == 5) {
            cx = index < 2 ? 0.66 + index * 0.20 : 0.60 + (index - 2) * 0.16;
            feet = index < 2 ? 0.48 : 0.75;
        } else {
            int column = index % 2;
            int row = index / 2;
            cx = enemy ? 0.64 + column * 0.20 : 0.13 + column * 0.20;
            feet = count == 2 ? 0.59 + column * 0.10 : 0.44 + row * 0.29 + column * 0.025;
        }
        int footY = Math.min(h - 280, (int) Math.round(h * feet));
        return new int[]{x + (int) Math.round(w * cx) - spriteW / 2, y + footY - spriteH};
    }
}
