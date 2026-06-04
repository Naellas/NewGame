package com.alderfall.game.camera;

public final class CameraController {
    private static final double LOOK_AHEAD_TILES = 0.45;
    private static final double DEAD_ZONE_X = 0.33;
    private static final double DEAD_ZONE_Y = 0.28;

    private boolean initialized;
    private String mapId = "";
    private double x;
    private double y;

    public CameraView update(
            String currentMapId,
            double playerX,
            double playerY,
            int lookDx,
            int lookDy,
            double viewportTilesX,
            double viewportTilesY,
            int mapWidth,
            int mapHeight,
            CameraMode mode,
            boolean lookAheadEnabled,
            int smoothing
    ) {
        double maxX = Math.max(0.0, mapWidth - viewportTilesX);
        double maxY = Math.max(0.0, mapHeight - viewportTilesY);
        double targetX = playerX - viewportTilesX / 2.0;
        double targetY = playerY - viewportTilesY / 2.0;
        if ((lookAheadEnabled || mode == CameraMode.LOOK_AHEAD) && mode != CameraMode.LOCKED) {
            targetX += lookDx * LOOK_AHEAD_TILES;
            targetY += lookDy * LOOK_AHEAD_TILES;
        }
        targetX = clamp(targetX, 0.0, maxX);
        targetY = clamp(targetY, 0.0, maxY);

        if (mode == CameraMode.DEAD_ZONE && initialized && currentMapId.equals(mapId)) {
            targetX = deadZoneTarget(x, playerX, viewportTilesX, DEAD_ZONE_X, maxX);
            targetY = deadZoneTarget(y, playerY, viewportTilesY, DEAD_ZONE_Y, maxY);
        }

        boolean snap = !initialized
                || !currentMapId.equals(mapId)
                || mode == CameraMode.LOCKED
                || mapWidth <= viewportTilesX
                || mapHeight <= viewportTilesY;
        if (snap) {
            x = targetX;
            y = targetY;
            initialized = true;
            mapId = currentMapId;
        } else {
            double follow = 0.08 + clamp(smoothing / 100.0, 0.0, 1.0) * 0.44;
            x += (targetX - x) * follow;
            y += (targetY - y) * follow;
            if (Math.abs(targetX - x) < 0.001) {
                x = targetX;
            }
            if (Math.abs(targetY - y) < 0.001) {
                y = targetY;
            }
        }
        x = clamp(x, 0.0, maxX);
        y = clamp(y, 0.0, maxY);
        return new CameraView(x, y);
    }

    public void resetToPlayer() {
        initialized = false;
        mapId = "";
    }

    private double deadZoneTarget(double currentCamera, double player, double viewportTiles, double deadZoneFraction, double maxCamera) {
        double margin = viewportTiles * deadZoneFraction;
        double min = currentCamera + margin;
        double max = currentCamera + viewportTiles - margin;
        if (player < min) {
            return clamp(player - margin, 0.0, maxCamera);
        }
        if (player > max) {
            return clamp(player + margin - viewportTiles, 0.0, maxCamera);
        }
        return clamp(currentCamera, 0.0, maxCamera);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
