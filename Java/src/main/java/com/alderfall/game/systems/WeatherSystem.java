package com.alderfall.game;

import java.util.EnumMap;

public final class WeatherSystem {
    private static final double TRANSITION_STEP = Math.min(1.0, GameConfig.FPS_MS / 8000.0);

    private final GameState state;
    private final EnumMap<WeatherCondition, Double> intensities = new EnumMap<>(WeatherCondition.class);
    private WeatherCondition renderWeather = WeatherCondition.CLEAR;
    private double windUnitX = 1.0;
    private double windUnitY;
    private double windX = 1.0;
    private double windStrength;
    private double waterWave = 0.18;
    private double rainIntensity;
    private double stormIntensity;

    public WeatherSystem(GameState state) {
        this.state = state;
        intensities.put(WeatherCondition.CLEAR, 1.0);
    }

    void tickTransition() {
        if (!effectsVisibleOnCurrentMap()) {
            intensities.clear();
            intensities.put(WeatherCondition.CLEAR, 1.0);
            return;
        }
        WeatherCondition target = state.currentWeather();
        for (WeatherCondition condition : WeatherCondition.values()) {
            double current = intensities.getOrDefault(condition, 0.0);
            double desired = condition == target ? 1.0 : 0.0;
            if (Math.abs(current - desired) <= TRANSITION_STEP) {
                current = desired;
            } else if (current < desired) {
                current += TRANSITION_STEP;
            } else {
                current -= TRANSITION_STEP;
            }

            if (current <= 0.0001) {
                intensities.remove(condition);
            } else {
                intensities.put(condition, current);
            }
        }
    }

    void refreshRenderMetrics() {
        if (!effectsVisibleOnCurrentMap()) {
            renderWeather = WeatherCondition.CLEAR;
            windStrength = 0.0;
            windUnitX = 1.0;
            windUnitY = 0.0;
            windX = 0.0;
            waterWave = 0.18;
            rainIntensity = 0.0;
            stormIntensity = 0.0;
            return;
        }
        renderWeather = state.currentWeather();
        windStrength = state.windStrength();
        double windRadians = state.windRadians();
        windUnitX = Math.cos(windRadians);
        windUnitY = Math.sin(windRadians);
        windX = windUnitX * windStrength;
        waterWave = clamp(blendedWeatherPush(renderWeather) * 0.72 + windStrength * 0.42, 0.18, 1.0);
        rainIntensity = intensity(WeatherCondition.RAIN);
        stormIntensity = intensity(WeatherCondition.STORM);
    }

    public double intensity(WeatherCondition condition) {
        return smoothStep(intensities.getOrDefault(condition, 0.0));
    }

    public boolean effectsVisibleOnCurrentMap() {
        String kind = state.world.kind(state.currentMapId);
        return !"interior".equals(kind) && !"dungeon".equals(kind);
    }

    double windUnitX() {
        return windUnitX;
    }

    double windUnitY() {
        return windUnitY;
    }

    public double windX() {
        return windX;
    }

    public double waterWave() {
        return waterWave;
    }

    public double rainIntensity() {
        return rainIntensity;
    }

    public double stormIntensity() {
        return stormIntensity;
    }

    private double blendedWeatherPush(WeatherCondition fallbackWeather) {
        double push = 0.0;
        double total = 0.0;
        for (WeatherCondition condition : WeatherCondition.values()) {
            double conditionIntensity = intensity(condition);
            if (conditionIntensity <= 0.0) {
                continue;
            }
            push += weatherWavePush(condition) * conditionIntensity;
            total += conditionIntensity;
        }
        if (total <= 0.001) {
            return weatherWavePush(fallbackWeather);
        }
        return push / total;
    }

    private double weatherWavePush(WeatherCondition weather) {
        return switch (weather) {
            case STORM, BLIZZARD -> 0.90;
            case RAIN, SNOW, DUST -> 0.58;
            case CLOUDY, FOG, HEAT_HAZE -> 0.34;
            case CLEAR -> 0.20;
        };
    }

    private double smoothStep(double value) {
        double clamped = clamp(value, 0.0, 1.0);
        return clamped * clamped * (3.0 - 2.0 * clamped);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
