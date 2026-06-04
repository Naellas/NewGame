package com.alderfall.game;

enum WeatherQuality {
    HIGH("high", "High", 96, 64, 0, 0.18, 3),
    BALANCED("balanced", "Balanced", 72, 48, 1, 0.26, 4),
    PERFORMANCE("performance", "Performance", 48, 32, 2, 0.34, 5);

    final String key;
    final String label;
    final int cloudLayers;
    final int intensityCacheSteps;
    final int cacheIntervalBonus;
    final double waterRippleThreshold;
    final int fogRenderScale;

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

    static WeatherQuality fromKey(String key) {
        for (WeatherQuality quality : values()) {
            if (quality.key.equals(key)) {
                return quality;
            }
        }
        return HIGH;
    }
}
