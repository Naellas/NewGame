package com.alderfall.game;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/** Regressions for side-facing scale changes, skipped strides and truncated stop transitions. */
public final class LocomotionTest {
    public static void main(String[] args) {
        LocomotionClock clock = new LocomotionClock();
        Set<Integer> visited = new HashSet<>();
        for (int tick = 0; tick < 120; tick++) {
            var selection = clock.select(tick, true);
            require(selection.equals(clock.select(tick, true)), "Repeated paint advances gait");
            if (selection.action().equals("walk")) visited.add(selection.frame());
        }
        require(visited.size() == WorldCharacterAnimation.FRAMES, "Continuous motion must show the full stride");
        clock.reset();
        for (int tick = 0; tick <= 13; tick++) clock.select(tick, true);
        require(!clock.select(14, false).action().equals("idle"), "Releasing movement cut the stride short");
        boolean completedStop = false;
        for (int tick = 15; tick < 70; tick++) {
            var selection = clock.select(tick, false);
            require(selection.equals(clock.select(tick, false)), "Repeated paint advances settling");
            if (selection.action().equals("stop_walk") && selection.frame() % 12 == 11) completedStop = true;
        }
        require(completedStop, "Final settling frame was skipped");
        require(clock.select(71, false).action().equals("idle"), "Gait did not settle");
        int total = 0;
        for (String actor : CharacterAnimationAudit.ACTORS) {
            AssetStore assets = new AssetStore(Path.of("assets"));
            int minIdle = Integer.MAX_VALUE, maxIdle = 0;
            for (String direction : CharacterAnimationAudit.DIRECTIONS) {
                String name = actor + "_model_" + direction;
                require(assets.animatedSpriteFrameCount(name, "walk", 192, 144) == WorldCharacterAnimation.FRAMES, "Walk not expanded: " + name);
                Rectangle idle = bounds(assets.spriteFit(name, 192, 144));
                minIdle = Math.min(minIdle, idle.height); maxIdle = Math.max(maxIdle, idle.height);
                Set<Long> poses = new HashSet<>();
                for (int frame = 0; frame < WorldCharacterAnimation.FRAMES; frame++) {
                    BufferedImage image = assets.animatedSpriteFit(name, "walk", 192, 144, frame);
                    Rectangle box = bounds(image);
                    require(box.width > 0 && box.height > 0, "Blank walk pose: " + name);
                    require(box.x > 0 && box.y > 0 && box.x + box.width < 192 && box.y + box.height < 144, "Clipped render: " + name);
                    require(Math.abs(box.height - idle.height) <= 20, "Body resized between idle and walk: " + name + " " + idle.height + " -> " + box.height);
                    poses.add(hash(image));
                }
                require(poses.size() >= 20, "Padded frame count or frozen gait: " + name + " / " + poses.size());
                total++;
            }
            require(maxIdle - minIdle <= 4, "Changing direction resizes " + actor + ": " + minIdle + ".." + maxIdle);
        }
        System.out.println("Locomotion checks passed: " + total + " directions, 32-frame strides, fixed scale and complete settling.");
    }

    private static Rectangle bounds(BufferedImage image) {
        Rectangle result = null;
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) if ((image.getRGB(x, y) >>> 24) > 32) {
            Rectangle p = new Rectangle(x, y, 1, 1); result = result == null ? p : result.union(p);
        }
        return result == null ? new Rectangle() : result;
    }
    private static long hash(BufferedImage image) {
        long result = 1;
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) result = result * 31 + image.getRGB(x, y);
        return result;
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}

