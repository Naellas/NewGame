package com.alderfall.game;

import java.nio.file.Files;
import java.nio.file.Path;

public final class WalkAnimationSpeedTest {
    public static void main(String[] args) throws Exception {
        require(frameAt(.5) == 7 && frameAt(1) == 8 && frameAt(2) == 16,
                "Grounded cadence must respect reach at low settings and preserve 1x/2x");
        LocomotionClock original=new LocomotionClock("original");original.selectAt(0,0,0,.5);
        require(original.selectAt(1,.1875,0,.5).frame()==4,"Changed legacy cadence");
        require(LocomotionClock.groundedCadence(.5,1,0)==.9&&LocomotionClock.groundedCadence(.5,1,1)==.5,"Direction-specific reach limit failed");
        LocomotionClock clock = new LocomotionClock();
        clock.selectAt(0, 0, 0, 1);
        require(clock.selectAt(1, .09375, 0, 1).frame() == 4, "Unexpected initial phase");
        require(clock.selectAt(2, .140625, 0, .5).frame() == 5, "Changing speed reset the stride");
        require(clock.selectAt(2, .140625, 0, 2).frame() == 5, "Repaint advanced the stride");
        require(clock.selectAt(3, .140625, 0, 2).action().startsWith("settle_"), "Stopped actor kept walking");
        require(clock.selectAt(12, .140625, 0, 2).action().equals("idle"), "Speed control changed standing behavior");
        require(frameAt(Double.NaN) == 8 && frameAt(Double.POSITIVE_INFINITY) == 8, "Invalid speed should use default");
        require(frameAt(-5) == 7 && frameAt(50) == 16, "Speed limits not enforced");
        Path root = Files.createTempDirectory("alderfall-walk-speed-");
        Path settings = root.resolve("config/settings.properties");
        try {
            GameConfig config = GameConfig.loadWithSettings(root);
            require(config.walkAnimationSpeed == 1, "Changed the existing default");
            config.walkAnimationSpeed = 1.6; config.save(root);
            GameConfig loaded = GameConfig.loadWithSettings(root);
            require(loaded.walkAnimationSpeed == 1.6, "Speed did not persist");
            require(loaded.movementSpeed.equals("normal") && loaded.combatAnimationSpeed == 1,
                    "Walk setting affected travel or combat speed");
            Files.writeString(settings, "walkAnimationSpeed=bad\n");
            require(GameConfig.loadWithSettings(root).walkAnimationSpeed == 1, "Malformed saved setting broke default");
            Files.writeString(settings, "walkAnimationSpeed=9\n");
            require(GameConfig.loadWithSettings(root).walkAnimationSpeed == 2, "Saved setting bypassed limit");
        } finally {
            Files.deleteIfExists(settings); Files.deleteIfExists(root.resolve("config")); Files.deleteIfExists(root);
        }
        System.out.println("Walk speed passed: reach-limited cadence, live phase continuity, stationary behavior, defaults, limits and persistence.");
    }
    private static int frameAt(double speed) {
        LocomotionClock gait = new LocomotionClock();
        gait.selectAt(0, 0, 0, speed);
        return gait.selectAt(1, .1875, 0, speed).frame();
    }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
