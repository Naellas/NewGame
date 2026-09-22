package com.alderfall.game;

public enum WeatherQuality {
    HIGH("high", "High", 96, 64, 0, 0.18, 3),
    BALANCED("balanced", "Balanced", 72, 48, 1, 0.26, 4),
    PERFORMANCE("performance", "Performance", 48, 32, 2, 0.34, 5),
    LOW_SPEC("low_spec", "Low Spec", 24, 16, 5, 0.52, 8);

    public final String key;
    public final String label;
    public final int cloudLayers;
    public final int intensityCacheSteps;
    public final int cacheIntervalBonus;
    public final double waterRippleThreshold;
    public final int fogRenderScale;

    WeatherQuality(String key, String label, int cloudLayers, int intensityCacheSteps,
                   int cacheIntervalBonus, double waterRippleThreshold, int fogRenderScale) {
        this.key = key;
        this.label = label;
        this.cloudLayers = cloudLayers;
        this.intensityCacheSteps = intensityCacheSteps;
        this.cacheIntervalBonus = cacheIntervalBonus;
        this.waterRippleThreshold = waterRippleThreshold;
        this.fogRenderScale = fogRenderScale;
    }

    public static WeatherQuality fromKey(String key) {
        for (WeatherQuality quality : values()) {
            if (quality.key.equals(key)) {
                return quality;
            }
        }
        return HIGH;
    }
}
