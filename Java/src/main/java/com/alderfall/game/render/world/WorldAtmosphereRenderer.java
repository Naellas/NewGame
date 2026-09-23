package com.alderfall.game.render.world;

import com.alderfall.game.*;

import com.alderfall.game.map.WorldMap;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public final class WorldAtmosphereRenderer {
    private static final int CLOUD_LAYER_COUNT = 96;
    private static final double WEATHER_VISIBILITY_EPSILON = 0.01;
    private static final int[] CLOUD_SEEDS = createCloudSeeds();

    private final AssetStore assets;
    private final GameState state;
    private final WeatherSystem weather;
    private final RenderMetrics renderMetrics;
    private final Effects effects;
    private final Map<String, BufferedImage> particleSprites = new HashMap<>();
    private final WeatherLayerCacheStore weatherLayerCaches = new WeatherLayerCacheStore();
    private BufferedImage fogBuffer;
    private int cameraX, cameraY;
    private double cameraOffsetX, cameraOffsetY;
    private RenderContext context = new RenderContext(0, 1, 1, GameConfig.TILE, GameConfig.WIDTH - GameConfig.SIDEBAR_WIDTH, GameConfig.HEIGHT);

    public WorldAtmosphereRenderer(AssetStore assets, GameState state, WeatherSystem weather, RenderMetrics renderMetrics, Effects effects) {
        this.assets = assets;
        this.state = state;
        this.weather = weather;
        this.renderMetrics = renderMetrics;
        this.effects = effects;
    }

    public void useContext(RenderContext context) {
        this.context = context;
    }

    public void useCamera(int tileX, int tileY, double offsetX, double offsetY) {
        cameraX = tileX;
        cameraY = tileY;
        cameraOffsetX = offsetX;
        cameraOffsetY = offsetY;
    }

    private WeatherQuality weatherQuality() {
        return effects.weatherQuality();
    }

    private int scaled(int value) {
        return effects.scaled(value);
    }

    private int tileRelative(int value, int tileSize) {
        return effects.tileRelative(value, tileSize);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int[] createCloudSeeds() {
        int[] seeds = new int[CLOUD_LAYER_COUNT];
        for (int i = 0; i < seeds.length; i++) {
            seeds[i] = Math.abs(i * 734287 + 19349663);
        }
        return seeds;
    }

    public void drawCloudLayer(Graphics2D g, int camX, int camY) {
        if (!WorldMap.OVERWORLD_ID.equals(state.currentMapId)) {
            return;
        }
        int ts = context.tileSize();
        int density = blendedCloudDensity();
        float opacity = blendedCloudOpacity();
        if (opacity <= WEATHER_VISIBILITY_EPSILON || density <= 0) {
            return;
        }
        Graphics2D cloudG = (Graphics2D) g.create();
        cloudG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        int cloudLimit = Math.min(CLOUD_SEEDS.length, weatherQuality().cloudLayers);
        for (int i = 0; i < cloudLimit; i++) {
            int seed = CLOUD_SEEDS[i];
            if (Math.floorMod(seed, 100) >= density) {
                continue;
            }
            double wx = Math.floorMod(seed / 17, WorldMap.COLS + 34) - 17;
            double wy = Math.floorMod(seed / 53, WorldMap.ROWS + 22) - 11;
            if (wx < camX - 7 || wy < camY - 4 || wx >= camX + context.visibleCols() + 7 || wy >= camY + context.visibleRows() + 4) {
                continue;
            }
            int sizeW = scaled(178 + Math.floorMod(seed / 97, 92));
            int sizeH = scaled(62 + Math.floorMod(seed / 193, 34));
            int x = (int) Math.round((wx - camX) * ts) - sizeW / 2;
            int y = (int) Math.round((wy - camY) * ts) - sizeH / 2;
            cloudG.drawImage(assets.image("cloud_billow_" + Math.floorMod(seed, 3), sizeW, sizeH), x, y, null);
        }
        cloudG.dispose();
    }

    int cloudDensity(WeatherCondition weather) {
        return switch (weather) {
            case CLEAR -> 18;
            case HEAT_HAZE -> 10;
            case DUST -> 36;
            case CLOUDY, FOG -> 62;
            case RAIN, SNOW -> 78;
            case STORM, BLIZZARD -> 88;
        };
    }

    float cloudOpacity(WeatherCondition weather) {
        return switch (weather) {
            case CLEAR -> 0.72f;
            case HEAT_HAZE -> 0.45f;
            case DUST -> 0.62f;
            case STORM, BLIZZARD -> 0.94f;
            default -> 0.84f;
        };
    }

    int blendedCloudDensity() {
        double total = 0.0;
        double density = 0.0;
        for (WeatherCondition condition : WeatherCondition.values()) {
            double intensity = weather.intensity(condition);
            density += cloudDensity(condition) * intensity;
            total += intensity;
        }
        if (total < 1.0) {
            density += cloudDensity(WeatherCondition.CLEAR) * (1.0 - total);
            total = 1.0;
        }
        return (int) Math.round(density / Math.max(1.0, total));
    }

    float blendedCloudOpacity() {
        double total = 0.0;
        double opacity = 0.0;
        for (WeatherCondition condition : WeatherCondition.values()) {
            double intensity = weather.intensity(condition);
            opacity += cloudOpacity(condition) * intensity;
            total += intensity;
        }
        if (total < 1.0) {
            opacity += cloudOpacity(WeatherCondition.CLEAR) * (1.0 - total);
            total = 1.0;
        }
        return (float) clamp(opacity / Math.max(1.0, total), 0.0, 1.0);
    }

    public void drawWorldAtmosphere(Graphics2D g) {
        int mapW = context.gameAreaWidth();
        int mapH = context.viewHeight();
        Graphics2D atmosphere = (Graphics2D) g.create();
        atmosphere.setClip(0, 0, mapW, mapH);
        if (isInteriorAtmosphereMap()) {
            drawInteriorTimeOverlay(atmosphere, mapW, mapH);
            atmosphere.dispose();
            return;
        }
        drawTimeOverlay(atmosphere, mapW, mapH);
        if (weather.effectsVisibleOnCurrentMap()) {
            long weatherStarted = renderMetrics.start();
            drawWeatherOverlay(atmosphere, mapW, mapH);
            renderMetrics.record("weather", weatherStarted);
        }
        atmosphere.dispose();
    }

    public boolean isInteriorAtmosphereMap() {
        String kind = state.world.kind(state.currentMapId);
        return "interior".equals(kind) || "dungeon".equals(kind);
    }

    public void drawInteriorTimeOverlay(Graphics2D g, int width, int height) {
        double daylight = state.daylightLevel();
        float dimAlpha = "interior".equals(state.world.kind(state.currentMapId))
                ? (float) (0.14 + (1.0 - daylight) * 0.22)
                : (float) ((1.0 - daylight) * 0.34);
        if (dimAlpha <= 0.02f) {
            return;
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, dimAlpha));
        g.setColor(new Color(10, 9, 15));
        g.fillRect(0, 0, width, height);
        g.setComposite(AlphaComposite.SrcOver);
    }

    public void drawTimeOverlay(Graphics2D g, int width, int height) {
        double daylight = state.daylightLevel();
        float nightAlpha = (float) ((1.0 - daylight) * 0.68);
        if (nightAlpha > 0.02f) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, nightAlpha));
            g.setColor(new Color(7, 14, 35));
            g.fillRect(0, 0, width, height);
        }
        int minutes = state.timeOfDayMinutes();
        boolean twilight = (minutes >= 300 && minutes < 420) || (minutes >= 1020 && minutes < 1200);
        if (twilight) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.16f));
            g.setPaint(new GradientPaint(0, 0, new Color(241, 155, 84), 0, height, new Color(58, 82, 132)));
            g.fillRect(0, 0, width, height);
        }
        if (daylight < 0.28) {
            drawStars(g, width, height, (float) ((0.28 - daylight) / 0.10 * 0.42));
        }
        g.setComposite(AlphaComposite.SrcOver);
    }

    public void drawStars(Graphics2D g, int width, int height, float alpha) {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.0f, Math.min(0.42f, alpha))));
        BufferedImage star = particleSprite("star");
        for (int i = 0; i < 70; i++) {
            int seed = Math.abs(i * 928371 + 54123);
            int x = Math.floorMod(seed, Math.max(1, width));
            int y = Math.floorMod(seed / 71, Math.max(1, height * 2 / 3));
            int size = 3 + Math.floorMod(seed / 311, 5);
            g.drawImage(star, x, y, size, size, null);
        }
    }

    public void drawWeatherOverlay(Graphics2D g, int width, int height) {
        for (WeatherCondition weather : WeatherCondition.values()) {
            double intensity = this.weather.intensity(weather);
            if (weather == WeatherCondition.CLEAR || intensity <= WEATHER_VISIBILITY_EPSILON) {
                continue;
            }
            drawWeatherCondition(g, width, height, weather, intensity);
        }
        g.setComposite(AlphaComposite.SrcOver);
    }

    public void drawWeatherCondition(Graphics2D g, int width, int height, WeatherCondition weather, double intensity) {
        switch (weather) {
            case RAIN -> {
                drawRainVeil(g, width, height, false, intensity);
                drawCachedPrecipitation(g, width, height, "rain", intensity, layer -> {
                    drawRain(layer, width, height, 420, false, intensity);
                    drawRainSplashes(layer, width, height, 90, false, intensity);
                });
            }
            case STORM -> {
                tint(g, width, height, new Color(22, 30, 42), (float) (0.14f * intensity));
                drawRainVeil(g, width, height, true, intensity);
                drawCachedPrecipitation(g, width, height, "storm", intensity, layer -> {
                    drawRain(layer, width, height, 650, true, intensity);
                    drawRainSplashes(layer, width, height, 150, true, intensity);
                });
                drawLightning(g, width, height, intensity);
            }
            case SNOW -> drawCachedPrecipitation(g, width, height, "snow", intensity,
                    layer -> drawSnow(layer, width, height, 70, false, intensity));
            case BLIZZARD -> {
                tint(g, width, height, new Color(220, 232, 240), (float) (0.065f * intensity));
                drawCachedPrecipitation(g, width, height, "blizzard", intensity,
                        layer -> drawSnow(layer, width, height, 130, true, intensity));
            }
            case FOG -> drawFadedWeatherLayer(g, width, height, "fog", (float) intensity, layer -> drawFog(layer, width, height));
            case DUST -> drawFadedWeatherLayer(g, width, height, "dust", (float) intensity, layer -> drawDust(layer, width, height));
            case HEAT_HAZE -> drawFadedWeatherLayer(g, width, height, "heat_haze", (float) intensity, layer -> drawHeatHaze(layer, width, height));
            case CLOUDY -> tint(g, width, height, new Color(84, 91, 105), (float) (0.10f * intensity));
            case CLEAR -> {
            }
        }
    }

    public void drawFadedWeatherLayer(Graphics2D g, int width, int height, String key, float alpha, WeatherLayerPainter painter) {
        if (alpha <= WEATHER_VISIBILITY_EPSILON) {
            return;
        }
        if (alpha >= 0.999f) {
            drawCachedStableWeatherLayer(g, width, height, key, painter);
            return;
        }
        int interval = dynamicWeatherFrameInterval(key, alpha);
        weatherLayerCaches.drawFaded(g, width, height, weatherLayerKey(key), state.zoom, context.frame(), alpha, interval, painter::paint);
    }

    public void drawCachedPrecipitation(Graphics2D g, int width, int height, String key, double intensity, WeatherLayerPainter painter) {
        int interval = dynamicWeatherFrameInterval(key, intensity);
        if (interval <= 1) {
            painter.paint(g);
            return;
        }
        weatherLayerCaches.drawPrecipitation(g, width, height, weatherLayerKey(key), state.zoom, context.frame(),
                weatherIntensityBucket(intensity), interval, painter::paint,
                frameDelta -> precipitationShiftX(key, frameDelta),
                frameDelta -> precipitationShiftY(key, frameDelta));
    }

    public void drawCachedStableWeatherLayer(Graphics2D g, int width, int height, String key, WeatherLayerPainter painter) {
        weatherLayerCaches.drawStable(g, width, height, weatherLayerKey(key), state.zoom, context.frame(),
                dynamicWeatherFrameInterval(key, 1.0), painter::paint);
    }

    String weatherLayerKey(String key) {
        return key + ":" + weatherQuality().key;
    }

    int dynamicWeatherFrameInterval(String key, double intensity) {
        if (intensity <= WEATHER_VISIBILITY_EPSILON) {
            return 1;
        }
        // Independent particles must advance every frame. Translating a cached field
        // makes every particle share one velocity and also moves ground impacts.
        if (key.equals("rain") || key.equals("storm") || key.equals("snow") || key.equals("blizzard")) {
            return 1;
        }
        int baseInterval = switch (key) {
            case "rain", "storm" -> 4;
            case "snow", "blizzard", "dust" -> 3;
            default -> 2;
        };
        return baseInterval + weatherQuality().cacheIntervalBonus;
    }

    int weatherIntensityBucket(double intensity) {
        int steps = weatherQuality().intensityCacheSteps;
        return clamp((int) Math.round(clamp(intensity, 0.0, 1.0) * steps), 0, steps);
    }

    int precipitationShiftX(String key, int frameDelta) {
        if (frameDelta <= 0) {
            return 0;
        }
        return switch (key) {
            case "rain" -> (int) Math.round(frameDelta * (weather.windX() * 8.5 + 7.0));
            case "storm" -> (int) Math.round(frameDelta * (weather.windX() * 12.0 + 9.0));
            default -> 0;
        };
    }

    int precipitationShiftY(String key, int frameDelta) {
        if (frameDelta <= 0) {
            return 0;
        }
        return switch (key) {
            case "rain" -> frameDelta * 23;
            case "storm" -> frameDelta * 30;
            case "snow" -> frameDelta * 2;
            case "blizzard" -> frameDelta * 4;
            default -> 0;
        };
    }

    void tint(Graphics2D g, int width, int height, Color color, float alpha) {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.setColor(color);
        g.fillRect(0, 0, width, height);
    }

    // Hash each attribute independently: linear seeds produce visible diagonal grids.
    private static double particleRandom(int index, int salt) {
        int value = index * 0x9e3779b9 + salt * 0x85ebca6b;
        value = (value ^ (value >>> 16)) * 0x7feb352d;
        value = (value ^ (value >>> 15)) * 0x846ca68b;
        return Integer.toUnsignedLong(value ^ (value >>> 16)) / 4294967296.0;
    }

    private int particleCount(int width, int height, int count) {
        double quality = switch (weatherQuality()) {
            case HIGH -> 1.0;
            case BALANCED -> 0.8;
            case PERFORMANCE -> 0.6;
            case LOW_SPEC -> 0.4;
        };
        return Math.max(1, (int) Math.round(count * (width * (double) height / (1280.0 * 720.0)) * quality));
    }

    private static double wrapParticle(double position, double span) {
        return position - Math.floor(position / span) * span;
    }

    public void drawRain(Graphics2D g, int width, int height, int count, boolean heavy, double intensity) {
        if (count <= 0 || intensity <= WEATHER_VISIBILITY_EPSILON) return;
        Graphics2D rain = (Graphics2D) g.create();
        rain.setComposite(AlphaComposite.SrcOver);
        rain.setStroke(new BasicStroke(1.0f));
        double time = context.frame();
        double wind = weather.windX() * (heavy ? 4.5 : 2.5);
        for (int i = 0, n = particleCount(width, height, count); i < n; i++) {
            double depth = particleRandom(i, 3);
            double speed = (heavy ? 12 : 9) + depth * 9;
            double vx = wind + (heavy ? 2.0 : 0.8);
            double length = 6 + depth * (heavy ? 17 : 11);
            int x = (int) Math.round(wrapParticle(particleRandom(i, 1) * (width + 64)
                    + time * vx, width + 64) - 32);
            int y = (int) Math.round(wrapParticle(particleRandom(i, 2) * (height + 64)
                    + time * speed, height + 64) - 32);
            rain.setColor(new Color(184, 207, 222, (int) ((35 + depth * 85) * intensity)));
            rain.drawLine(x, y, x - (int) Math.round(vx * length / speed), y - (int) length);
        }
        rain.dispose();
    }

    public void drawRainVeil(Graphics2D g, int width, int height, boolean heavy, double intensity) {
        // A light atmospheric tint preserves terrain contrast without a tiled sheet.
        tint(g, width, height, new Color(102, 126, 145), (float) ((heavy ? 0.045 : 0.018) * intensity));
    }

    public void drawRainSplashes(Graphics2D g, int width, int height, int count, boolean heavy, double intensity) {
        if (count <= 0 || intensity <= WEATHER_VISIBILITY_EPSILON) return;
        Graphics2D splash = (Graphics2D) g.create();
        splash.setComposite(AlphaComposite.SrcOver);
        splash.setStroke(new BasicStroke(1.0f));
        int tileSize = context.tileSize();
        for (int sy = -1; sy <= height / tileSize + 1; sy++) {
            for (int sx = -1; sx <= width / tileSize + 1; sx++) {
                int wx = cameraX + sx, wy = cameraY + sy;
                int seed = wx * 734287 ^ wy * 912931 ^ state.currentMapId.hashCode();
                if (particleRandom(seed, 4) > (heavy ? 0.48 : 0.28)) continue;
                if (!state.world.isPassable(state.currentMapId, wx, wy)) continue;
                int cycle = 42 + (int) (particleRandom(seed, 5) * 55);
                int age = Math.floorMod(context.frame() + (int) (particleRandom(seed, 6) * cycle), cycle);
                if (age >= 9) continue;
                int x = (int) Math.round((sx + 0.15 + particleRandom(seed, 7) * 0.7) * tileSize + cameraOffsetX);
                int y = (int) Math.round((sy + 0.15 + particleRandom(seed, 8) * 0.7) * tileSize + cameraOffsetY);
                int radius = 1 + age / 3;
                splash.setColor(new Color(192, 213, 225, (int) ((1.0 - age / 9.0) * 80 * intensity)));
                splash.drawLine(x - radius, y, x - 1, y + 1);
                splash.drawLine(x + 1, y + 1, x + radius, y);
            }
        }
        splash.dispose();
    }

    public void drawSnow(Graphics2D g, int width, int height, int count, boolean heavy, double intensity) {
        if (count <= 0 || intensity <= WEATHER_VISIBILITY_EPSILON) return;
        Graphics2D snow = (Graphics2D) g.create();
        snow.setComposite(AlphaComposite.SrcOver);
        double time = context.frame();
        for (int i = 0, n = particleCount(width, height, count * 2); i < n; i++) {
            double depth = particleRandom(i, 13);
            double phase = particleRandom(i, 14) * Math.PI * 2;
            double speed = (heavy ? 1.6 : 0.45) + depth * (heavy ? 2.5 : 1.1);
            double drift = weather.windX() * (heavy ? 3.2 : 0.8) + (heavy ? 1.4 : 0.15);
            double sway = Math.sin(time * (0.012 + depth * 0.013) + phase) * (5 + depth * 13);
            int x = (int) Math.round(wrapParticle(particleRandom(i, 11) * (width + 32)
                    + time * drift * (0.6 + depth * 0.4) + sway, width + 32) - 16);
            int y = (int) Math.round(wrapParticle(particleRandom(i, 12) * (height + 32)
                    + time * speed, height + 32) - 16);
            int size = depth < 0.5 ? 1 : depth < 0.92 ? 2 : 3;
            snow.setColor(new Color(225, 234, 242, (int) ((70 + depth * 115) * intensity)));
            snow.fillRect(x, y, size, size == 3 ? 2 : size);
        }
        snow.dispose();
    }

    public void drawFog(Graphics2D g, int width, int height) {
        int fogRenderScale = weatherQuality().fogRenderScale;
        int fogW = Math.max(1, (width + fogRenderScale - 1) / fogRenderScale);
        int fogH = Math.max(1, (height + fogRenderScale - 1) / fogRenderScale);
        if (fogBuffer == null || fogBuffer.getWidth() != fogW || fogBuffer.getHeight() != fogH) {
            fogBuffer = new BufferedImage(fogW, fogH, BufferedImage.TYPE_INT_ARGB);
        }

        Graphics2D fog = fogBuffer.createGraphics();
        fog.setComposite(AlphaComposite.Clear);
        fog.fillRect(0, 0, fogW, fogH);
        fog.setComposite(AlphaComposite.SrcOver);
        fog.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        fog.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        fog.scale(1.0 / fogRenderScale, 1.0 / fogRenderScale);
        drawFogDirect(fog, width, height);
        fog.dispose();

        Graphics2D fogOut = (Graphics2D) g.create();
        fogOut.setComposite(AlphaComposite.SrcOver);
        fogOut.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        fogOut.drawImage(fogBuffer, 0, 0, width, height, null);
        fogOut.dispose();
    }

    public void drawFogDirect(Graphics2D g, int width, int height) {
        tint(g, width, height, new Color(190, 204, 206), 0.045f);
        drawFogWash(g, width, height);
        drawFogBands(g, width, height);
        drawGroundFog(g, width, height);
        drawFogWisps(g, width, height);

    }

    public void drawDust(Graphics2D g, int width, int height) {
        tint(g, width, height, new Color(175, 122, 64), 0.12f);
        tint(g, width, height, new Color(235, 190, 111), 0.035f);
        drawSandVeil(g, width, height);
        drawSandVortices(g, width, height);
        drawSandStreaks(g, width, height);
        drawSandMotes(g, width, height);
    }

    public void drawFogWash(Graphics2D g, int width, int height) {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.10f));
        g.setPaint(new GradientPaint(0, 0, new Color(214, 224, 214, 22), 0, height, new Color(172, 188, 180, 42)));
        g.fillRect(0, 0, width, height);
        g.setPaint(null);
    }

    public void drawFogBands(Graphics2D g, int width, int height) {
        BufferedImage band = particleSprite("fog_band");
        int spill = scaled(260);
        int drawW = width + spill * 2;
        int drawH = scaled(118);
        for (int i = 0; i < 4; i++) {
            int seed = Math.abs(i * 37321 + 4127);
            int y = Math.floorMod(seed / 31, Math.max(1, height + drawH)) - drawH / 2;
            int drift = (int) Math.round(Math.sin((context.frame() + i * 43) * 0.012) * scaled(70)
                    + context.frame() * (0.28 + weather.windX() * 0.8 + i * 0.03));
            int x = -spill + Math.floorMod(drift + seed, Math.max(1, spill * 2)) - spill;
            drawSpriteAlpha(g, band, x, y, drawW, drawH, 0.12f + Math.floorMod(seed / 97, 6) / 100.0f);
        }
    }

    public void drawGroundFog(Graphics2D g, int width, int height) {
        BufferedImage band = particleSprite("fog_band");
        int spill = scaled(320);
        int drawW = width + spill * 2;
        int drawH = scaled(150);
        int baseY = Math.max(0, height - scaled(245));
        for (int i = 0; i < 2; i++) {
            int y = baseY + i * scaled(56);
            int x = -spill + (int) Math.round(Math.sin((context.frame() + i * 61) * 0.010) * scaled(82));
            drawSpriteAlpha(g, band, x, y, drawW, drawH, 0.16f - i * 0.02f);
        }
    }

    public void drawFogWisps(Graphics2D g, int width, int height) {
        BufferedImage wisp = particleSprite("fog_wisp");
        BufferedImage curl = particleSprite("fog_curl");
        for (int i = 0; i < 10; i++) {
            int seed = Math.abs(i * 77687 + 3319);
            boolean foreground = Math.floorMod(seed / 23, 4) == 0;
            int drawW = scaled((foreground ? 420 : 280) + Math.floorMod(seed, foreground ? 270 : 210));
            int drawH = scaled((foreground ? 72 : 42) + Math.floorMod(seed / 37, foreground ? 52 : 34));
            int yRange = Math.max(1, height - scaled(22));
            int y = Math.floorMod(seed / 73 + (int) Math.round(Math.sin((context.frame() + i * 19) * 0.025) * scaled(13)), yRange) - scaled(10);
            int speed = foreground ? 1 : 0;
            int x = Math.floorMod(context.frame() * speed + (int) Math.round(context.frame() * weather.windX() * 1.2) + seed,
                    width + drawW + scaled(220)) - drawW - scaled(110);
            float alpha = foreground ? 0.24f : 0.17f;
            drawSpriteAlpha(g, Math.floorMod(seed, 5) == 0 ? curl : wisp, x, y, drawW, drawH, alpha);
        }
    }

    public void drawFogMotes(Graphics2D g, int width, int height) {
        BufferedImage mote = particleSprite("dust");
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.08f));
        for (int i = 0; i < 10; i++) {
            int seed = Math.abs(i * 69191 + 1009);
            int x = Math.floorMod(seed + context.frame() / 3, width + 44) - 22;
            int y = Math.floorMod(seed / 43 + (int) Math.round(Math.sin((context.frame() + i * 11) * 0.032) * scaled(9)), height + 30) - 15;
            int size = Math.max(2, scaled(3 + Math.floorMod(seed / 29, 5)));
            g.drawImage(mote, x, y, size * 3, size, null);
        }
    }

    public void drawSandVeil(Graphics2D g, int width, int height) {
        BufferedImage veil = particleSprite("sand_veil");
        int tileW = Math.max(1, scaled(256));
        int tileH = Math.max(1, scaled(192));
        int offsetX = Math.floorMod((int) Math.round(context.frame() * (10.0 + weather.windX() * 18.0)), tileW);
        int offsetY = Math.floorMod(context.frame() * 2, tileH);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.34f));
        for (int y = -offsetY; y < height; y += tileH) {
            for (int x = -offsetX; x < width; x += tileW) {
                g.drawImage(veil, x, y, tileW, tileH, null);
            }
        }
    }

    public void drawSandStreaks(Graphics2D g, int width, int height) {
        BufferedImage streak = particleSprite("sand_streak_field");
        double gust = Math.sin(context.frame() * 0.043) * 0.22 + Math.sin(context.frame() * 0.017 + 2.1) * 0.12;
        int tileW = Math.max(1, scaled(512));
        int tileH = Math.max(1, scaled(384));
        int offsetX = Math.floorMod((int) Math.round(context.frame() * (11.0 + (weather.windX() + gust) * 16.0)), tileW);
        int offsetY = Math.floorMod(context.frame(), tileH);
        drawTiledWeatherSprite(g, streak, width, height, tileW, tileH, offsetX, offsetY, 0.72f);
        drawTiledWeatherSprite(g, streak, width, height, tileW, tileH,
                offsetX + tileW / 2, offsetY + tileH / 3, 0.28f);
    }

    public void drawSandMotes(Graphics2D g, int width, int height) {
        BufferedImage mote = particleSprite("sand_mote_field");
        int tileW = Math.max(1, scaled(512));
        int tileH = Math.max(1, scaled(384));
        int offsetX = Math.floorMod(context.frame() * 5, tileW);
        int offsetY = Math.floorMod(context.frame(), tileH);
        drawTiledWeatherSprite(g, mote, width, height, tileW, tileH, offsetX, offsetY, 0.58f);
    }

    public void drawSandVortices(Graphics2D g, int width, int height) {
        BufferedImage mote = particleSprite("dust");
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.27f));
        for (int i = 0; i < 7; i++) {
            int seed = Math.abs(i * 55291 + 6113);
            int centerX = Math.floorMod(seed + context.frame() * (2 + i % 3), width + scaled(180)) - scaled(90);
            int baseY = Math.floorMod(seed / 53 + context.frame() / 2, Math.max(1, height - scaled(60))) + scaled(25);
            int radius = scaled(18 + Math.floorMod(seed / 97, 32));
            int heightSpan = scaled(46 + Math.floorMod(seed / 131, 58));
            for (int step = 0; step < 7; step++) {
                double phase = (context.frame() * 0.12 + seed * 0.01 + step * 0.9);
                int x = centerX + (int) Math.round(Math.sin(phase) * radius * (1.0 - step * 0.08));
                int y = baseY - step * heightSpan / 7;
                int drawW = Math.max(2, scaled(12 + step * 3));
                int drawH = Math.max(1, scaled(4 + step));
                g.drawImage(mote, x, y, drawW, drawH, null);
            }
        }
    }

    public void drawHeatHaze(Graphics2D g, int width, int height) {
        tint(g, width, height, new Color(230, 176, 96), 0.08f);
        BufferedImage shimmer = particleSprite("heat_haze");
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f));
        for (int i = 0; i < 7; i++) {
            int y = height - scaled(150 + i * 46);
            int x = (int) Math.round(Math.sin((context.frame() + i * 31) * 0.035) * scaled(20));
            g.drawImage(shimmer, x - scaled(40), y, width + scaled(100), scaled(38), null);
        }
    }

    public void drawLightning(Graphics2D g, int width, int height, double intensity) {
        if (intensity < 0.55 || Math.floorMod(context.frame(), 190) > 5) {
            return;
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.24f * intensity)));
        g.setColor(new Color(234, 240, 255));
        g.fillRect(0, 0, width, height);
    }

    public void drawTiledWeatherSprite(Graphics2D g, BufferedImage sprite, int width, int height,
                                        int tileW, int tileH, int offsetX, int offsetY, float alpha) {
        if (alpha <= 0.0f || tileW <= 0 || tileH <= 0) {
            return;
        }
        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1.0f, alpha)));
        int startX = -Math.floorMod(offsetX, tileW);
        int startY = -Math.floorMod(offsetY, tileH);
        for (int y = startY; y < height; y += tileH) {
            for (int x = startX; x < width; x += tileW) {
                g.drawImage(sprite, x, y, tileW, tileH, null);
            }
        }
        g.setComposite(oldComposite);
    }

    public void drawInteriorLightTints(Graphics2D g, int camX, int camY, int visibleCols, int visibleRows, int tileSize) {
        Composite oldComposite = g.getComposite();
        float daylight = (float) state.daylightLevel();
        float warmAlpha = 0.018f + (1.0f - daylight) * 0.028f;
        for (int sy = 0; sy < visibleRows; sy++) {
            for (int sx = 0; sx < visibleCols; sx++) {
                int wx = camX + sx;
                int wy = camY + sy;
                char tile = state.world.tileAt(state.currentMapId, wx, wy);
                if (tile == 'x') {
                    continue;
                }
                int px = sx * tileSize;
                int py = sy * tileSize;
                g.setComposite(AlphaComposite.SrcOver.derive(warmAlpha));
                g.setColor(new Color(255, 208, 142));
                g.fillRect(px, py, tileSize, tileSize);
                if (tile == 'o') {
                    g.setComposite(AlphaComposite.SrcOver.derive(0.035f));
                    g.setColor(new Color(0, 0, 0));
                    g.fillRect(px, py + tileRelative(31, tileSize), tileSize, tileRelative(7, tileSize));
                }
            }
        }
        g.setComposite(oldComposite);
    }

    public void drawSpriteAlpha(Graphics2D g, BufferedImage sprite, int x, int y, int width, int height, float alpha) {
        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.drawImage(sprite, x, y, width, height, null);
        g.setComposite(oldComposite);
    }

    public void drawParticleSprite(Graphics2D g, BufferedImage sprite, int x, int y, int width, int height, double angle, float alpha) {
        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        if (Math.abs(angle) < 0.0001) {
            g.drawImage(sprite, x, y, width, height, null);
            g.setComposite(oldComposite);
            return;
        }
        AffineTransform oldTransform = g.getTransform();
        g.translate(x + width / 2.0, y + height / 2.0);
        g.rotate(angle);
        g.drawImage(sprite, -width / 2, -height / 2, width, height, null);
        g.setTransform(oldTransform);
        g.setComposite(oldComposite);
    }

    @FunctionalInterface
    interface WeatherLayerPainter {
        void paint(Graphics2D g);
    }

    BufferedImage particleSprite(String key) {
        return particleSprites.computeIfAbsent(key, this::createParticleSprite);
    }

    BufferedImage createParticleSprite(String key) {
        return switch (key) {
            case "rain" -> createRainSprite(false);
            case "rain_heavy" -> createRainSprite(true);
            case "rain_field" -> createRainFieldSprite(false);
            case "rain_field_heavy" -> createRainFieldSprite(true);
            case "rain_sheet" -> createRainSheetSprite(false);
            case "rain_sheet_heavy" -> createRainSheetSprite(true);
            case "rain_splash" -> createRainSplashSprite();
            case "rain_splash_field" -> createRainSplashFieldSprite(false);
            case "rain_splash_field_heavy" -> createRainSplashFieldSprite(true);
            case "snow" -> createSnowSprite(false);
            case "snow_heavy" -> createSnowSprite(true);
            case "snow_field" -> createSnowFieldSprite(false);
            case "snow_field_heavy" -> createSnowFieldSprite(true);
            case "fog_wisp" -> createFogWispSprite();
            case "fog_band" -> createFogBandSprite();
            case "fog_curl" -> createFogCurlSprite();
            case "dust" -> createDustSprite();
            case "sand_veil" -> createSandVeilSprite();
            case "sand_streak" -> createSandStreakSprite();
            case "sand_streak_field" -> createSandStreakFieldSprite();
            case "sand_mote_field" -> createSandMoteFieldSprite();
            case "heat_haze" -> createHeatHazeSprite();
            case "star" -> createStarSprite();
            default -> createDustSprite();
        };
    }

    BufferedImage createRainSprite(boolean heavy) {
        BufferedImage image = new BufferedImage(24, 96, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = spriteGraphics(image);
        g.setStroke(new BasicStroke(heavy ? 4.5f : 2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setPaint(new GradientPaint(12, 2, new Color(226, 242, 255, heavy ? 18 : 12), 12, 92, new Color(176, 211, 238, heavy ? 176 : 138)));
        g.drawLine(12, 4, 12, 92);
        g.setStroke(new BasicStroke(heavy ? 1.8f : 1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setPaint(new GradientPaint(10, 12, new Color(250, 253, 255, heavy ? 120 : 88), 10, 70, new Color(206, 226, 242, 16)));
        g.drawLine(10, 12, 10, 70);
        g.dispose();
        return image;
    }

    BufferedImage createRainFieldSprite(boolean heavy) {
        int size = 512;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = spriteGraphics(image);
        BufferedImage drop = createRainSprite(heavy);
        int count = heavy ? 250 : 170;
        double baseAngle = heavy ? -0.42 : -0.34;
        for (int i = 0; i < count; i++) {
            int seed = Math.abs(i * 62003 + (heavy ? 18419 : 8419));
            boolean foreground = Math.floorMod(seed / 17, 5) == 0;
            int x = Math.floorMod(seed, size + 120) - 60;
            int y = Math.floorMod(seed / 37, size + 96) - 48;
            int drawW = (foreground ? 5 : 3) + Math.floorMod(seed / 17, heavy ? 4 : 3);
            int drawH = (foreground ? 28 : 18) + Math.floorMod(seed / 31, heavy ? 20 : 13);
            double angle = baseAngle + (Math.floorMod(seed / 101, 9) - 4) * 0.018;
            float alpha = Math.min(0.84f, (heavy ? 0.56f : 0.46f)
                    + (foreground ? 0.18f : 0.0f)
                    + Math.floorMod(seed / 43, 18) / 100.0f);
            drawParticleSprite(g, drop, x, y, drawW, drawH, angle, alpha);
        }
        g.dispose();
        return image;
    }

    BufferedImage createRainSheetSprite(boolean heavy) {
        BufferedImage image = new BufferedImage(192, 192, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = spriteGraphics(image);
        g.setStroke(new BasicStroke(heavy ? 1.7f : 1.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < (heavy ? 46 : 32); i++) {
            int seed = Math.abs(i * 48271 + 9311);
            int x = Math.floorMod(seed, 230) - 24;
            int y = Math.floorMod(seed / 37, 210) - 16;
            int len = heavy ? 36 + Math.floorMod(seed / 61, 32) : 24 + Math.floorMod(seed / 61, 24);
            int lean = heavy ? 10 + Math.floorMod(seed / 97, 13) : 7 + Math.floorMod(seed / 97, 10);
            int alpha = heavy ? 78 + Math.floorMod(seed / 23, 72) : 50 + Math.floorMod(seed / 23, 58);
            g.setColor(new Color(190, 218, 240, alpha));
            g.drawLine(x, y, x - lean, y + len);
        }
        g.dispose();
        return image;
    }

    BufferedImage createRainSplashSprite() {
        BufferedImage image = new BufferedImage(48, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = spriteGraphics(image);
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(198, 225, 242, 104));
        g.drawArc(9, 8, 30, 9, 12, 156);
        g.setColor(new Color(230, 243, 252, 132));
        g.drawLine(18, 12, 14, 5);
        g.drawLine(26, 12, 28, 4);
        g.drawLine(32, 12, 37, 7);
        g.setComposite(AlphaComposite.SrcOver.derive(0.34f));
        g.fillOval(12, 12, 26, 4);
        g.dispose();
        return image;
    }

    BufferedImage createRainSplashFieldSprite(boolean heavy) {
        BufferedImage image = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = spriteGraphics(image);
        BufferedImage splash = createRainSplashSprite();
        int count = heavy ? 100 : 64;
        for (int i = 0; i < count; i++) {
            int seed = Math.abs(i * 51437 + (heavy ? 92719 : 2719));
            int x = Math.floorMod(seed, 552) - 20;
            int y = Math.floorMod(seed / 29, 280) - 12;
            int drawW = 16 + Math.floorMod(seed / 47, 16);
            int drawH = 6 + Math.floorMod(seed / 89, 5);
            float alpha = (heavy ? 0.24f : 0.18f) + Math.floorMod(seed / 31, 12) / 100.0f;
            drawParticleSprite(g, splash, x, y, drawW, drawH, 0.0, alpha);
        }
        g.dispose();
        return image;
    }

    BufferedImage createSnowSprite(boolean heavy) {
        int size = 40;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = spriteGraphics(image);
        drawSoftBlob(g, size / 2, size / 2, heavy ? 18 : 14, new Color(246, 251, 255), heavy ? 186 : 142);
        g.setComposite(AlphaComposite.SrcOver.derive(heavy ? 0.54f : 0.38f));
        g.setStroke(new BasicStroke(heavy ? 1.8f : 1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(255, 255, 255));
        g.drawLine(size / 2, 7, size / 2, size - 7);
        g.drawLine(7, size / 2, size - 7, size / 2);
        g.drawLine(11, 11, size - 11, size - 11);
        g.drawLine(size - 11, 11, 11, size - 11);
        g.dispose();
        return image;
    }

    BufferedImage createSnowFieldSprite(boolean heavy) {
        int size = 512;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = spriteGraphics(image);
        BufferedImage flake = createSnowSprite(heavy);
        int count = heavy ? 170 : 100;
        for (int i = 0; i < count; i++) {
            int seed = Math.abs(i * 73241 + (heavy ? 41939 : 11939));
            int x = Math.floorMod(seed, size + 40) - 20;
            int y = Math.floorMod(seed / 29, size + 30) - 15;
            int drawSize = (heavy ? 7 : 5) + Math.floorMod(seed / 43, 5);
            float alpha = (heavy ? 0.56f : 0.42f) + Math.floorMod(seed / 67, 12) / 100.0f;
            drawSpriteAlpha(g, flake, x, y, drawSize, drawSize, Math.min(0.78f, alpha));
        }
        g.dispose();
        return image;
    }

    BufferedImage createFogWispSprite() {
        BufferedImage image = new BufferedImage(384, 96, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = spriteGraphics(image);
        drawSoftBlob(g, 58, 50, 76, new Color(220, 228, 218), 82);
        drawSoftBlob(g, 132, 42, 92, new Color(232, 237, 229), 92);
        drawSoftBlob(g, 226, 54, 110, new Color(210, 221, 212), 78);
        drawSoftBlob(g, 316, 44, 82, new Color(236, 240, 232), 80);
        g.setComposite(AlphaComposite.SrcOver.derive(0.20f));
        g.setColor(new Color(248, 250, 245));
        g.fillOval(18, 32, 330, 28);
        g.setComposite(AlphaComposite.SrcOver.derive(0.11f));
        g.fillOval(70, 56, 280, 18);
        g.dispose();
        return image;
    }

    BufferedImage createFogBandSprite() {
        BufferedImage image = new BufferedImage(512, 128, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = spriteGraphics(image);
        drawSoftBlob(g, 92, 66, 86, new Color(231, 238, 231), 52);
        drawSoftBlob(g, 202, 58, 112, new Color(238, 243, 236), 58);
        drawSoftBlob(g, 334, 72, 120, new Color(218, 230, 221), 48);
        drawSoftBlob(g, 438, 56, 80, new Color(239, 242, 236), 42);
        g.setComposite(AlphaComposite.SrcOver.derive(0.09f));
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(245, 248, 242));
        for (int y = 42; y < 96; y += 18) {
            for (int x = 34; x < 456; x += 88) {
                g.drawArc(x, y, 112, 18, 8, 164);
            }
        }
        g.setComposite(AlphaComposite.SrcOver.derive(0.07f));
        g.setColor(new Color(196, 211, 203));
        g.fillOval(64, 72, 380, 24);
        g.dispose();
        return image;
    }

    BufferedImage createFogCurlSprite() {
        BufferedImage image = new BufferedImage(220, 92, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = spriteGraphics(image);
        drawSoftBlob(g, 58, 48, 54, new Color(230, 236, 228), 86);
        drawSoftBlob(g, 126, 42, 68, new Color(214, 226, 218), 74);
        drawSoftBlob(g, 174, 50, 48, new Color(241, 244, 238), 62);
        g.setComposite(AlphaComposite.SrcOver.derive(0.23f));
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(248, 250, 245));
        g.drawArc(24, 24, 110, 38, 182, -220);
        g.drawArc(88, 34, 100, 28, 178, -190);
        g.dispose();
        return image;
    }

    BufferedImage createDustSprite() {
        BufferedImage image = new BufferedImage(80, 28, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = spriteGraphics(image);
        drawSoftBlob(g, 18, 14, 20, new Color(218, 171, 104), 92);
        drawSoftBlob(g, 40, 13, 26, new Color(238, 194, 124), 118);
        drawSoftBlob(g, 62, 15, 18, new Color(195, 137, 83), 76);
        g.dispose();
        return image;
    }

    BufferedImage createSandVeilSprite() {
        BufferedImage image = new BufferedImage(256, 192, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = spriteGraphics(image);
        g.setPaint(new GradientPaint(0, 0, new Color(207, 150, 77, 34), 256, 192, new Color(242, 199, 122, 70)));
        g.fillRect(0, 0, 256, 192);
        g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 58; i++) {
            int seed = Math.abs(i * 71143 + 5021);
            int x = Math.floorMod(seed, 310) - 28;
            int y = Math.floorMod(seed / 37, 220) - 14;
            int len = 28 + Math.floorMod(seed / 61, 76);
            int lift = 4 + Math.floorMod(seed / 89, 20);
            int alpha = 26 + Math.floorMod(seed / 19, 54);
            g.setColor(new Color(255, 223, 154, alpha));
            g.drawLine(x, y, x + len, y - lift);
        }
        for (int i = 0; i < 80; i++) {
            int seed = Math.abs(i * 19073 + 877);
            int x = Math.floorMod(seed, 256);
            int y = Math.floorMod(seed / 41, 192);
            int size = 1 + Math.floorMod(seed / 97, 3);
            g.setColor(new Color(116, 78, 42, 28 + Math.floorMod(seed / 23, 36)));
            g.fillOval(x, y, size, size);
        }
        g.dispose();
        return image;
    }

    BufferedImage createSandStreakSprite() {
        BufferedImage image = new BufferedImage(180, 28, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = spriteGraphics(image);
        g.setStroke(new BasicStroke(5.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setPaint(new GradientPaint(10, 14, new Color(255, 229, 165, 0), 132, 14, new Color(255, 222, 139, 150)));
        g.drawLine(6, 16, 160, 10);
        g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setPaint(new GradientPaint(28, 21, new Color(134, 85, 43, 0), 178, 10, new Color(126, 78, 37, 88)));
        g.drawLine(24, 21, 178, 12);
        g.dispose();
        return image;
    }

    BufferedImage createSandStreakFieldSprite() {
        BufferedImage image = new BufferedImage(512, 384, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = spriteGraphics(image);
        BufferedImage streak = createSandStreakSprite();
        for (int i = 0; i < 150; i++) {
            int seed = Math.abs(i * 97561 + 1237);
            boolean foreground = Math.floorMod(seed / 19, 5) == 0;
            int x = Math.floorMod(seed, 672) - 80;
            int y = Math.floorMod(seed / 31, 454) - 35;
            int drawW = (foreground ? 88 : 46) + Math.floorMod(seed / 11, foreground ? 88 : 46);
            int drawH = (foreground ? 8 : 4) + Math.floorMod(seed / 83, foreground ? 9 : 5);
            float alpha = foreground ? 0.42f : 0.25f;
            drawParticleSprite(g, streak, x, y, drawW, drawH, -0.28, alpha);
        }
        g.dispose();
        return image;
    }

    BufferedImage createSandMoteFieldSprite() {
        BufferedImage image = new BufferedImage(512, 384, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = spriteGraphics(image);
        BufferedImage mote = createDustSprite();
        for (int i = 0; i < 130; i++) {
            int seed = Math.abs(i * 86311 + 4201);
            int x = Math.floorMod(seed, 622) - 55;
            int y = Math.floorMod(seed / 41, 424) - 20;
            int drawW = 14 + Math.floorMod(seed / 23, 34);
            int drawH = 4 + Math.floorMod(seed / 97, 9);
            drawParticleSprite(g, mote, x, y, drawW, drawH, -0.12,
                    0.24f + Math.floorMod(seed / 31, 16) / 100.0f);
        }
        g.dispose();
        return image;
    }

    BufferedImage createHeatHazeSprite() {
        BufferedImage image = new BufferedImage(384, 42, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = spriteGraphics(image);
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int band = 0; band < 3; band++) {
            g.setComposite(AlphaComposite.SrcOver.derive(0.12f - band * 0.025f));
            g.setColor(new Color(255, 238, 176));
            int y = 10 + band * 10;
            for (int x = -20; x < 384; x += 34) {
                g.drawArc(x, y, 52, 16, 12, 156);
            }
        }
        g.dispose();
        return image;
    }

    BufferedImage createStarSprite() {
        BufferedImage image = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = spriteGraphics(image);
        drawSoftBlob(g, 12, 12, 11, new Color(252, 245, 202), 150);
        g.setComposite(AlphaComposite.SrcOver.derive(0.65f));
        g.setStroke(new BasicStroke(1.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(255, 255, 238));
        g.drawLine(12, 3, 12, 21);
        g.drawLine(3, 12, 21, 12);
        g.dispose();
        return image;
    }

    Graphics2D spriteGraphics(BufferedImage image) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return g;
    }

    public void drawSoftBlob(Graphics2D g, int cx, int cy, int radius, Color color, int alpha) {
        Paint oldPaint = g.getPaint();
        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver);
        g.setPaint(new RadialGradientPaint(
                cx,
                cy,
                Math.max(1, radius),
                new float[]{0.0f, 0.42f, 1.0f},
                new Color[]{
                        new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha),
                        new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, alpha / 3)),
                        new Color(color.getRed(), color.getGreen(), color.getBlue(), 0)
                }
        ));
        g.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
        g.setPaint(oldPaint);
        g.setComposite(oldComposite);
    }


    public record RenderContext(int frame, int visibleCols, int visibleRows, int tileSize, int gameAreaWidth, int viewHeight) {
    }

    public interface Effects {
        WeatherQuality weatherQuality();

        int scaled(int value);

        int tileRelative(int value, int tileSize);
    }
}
