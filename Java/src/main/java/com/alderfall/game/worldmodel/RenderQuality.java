package com.alderfall.game;

public enum RenderQuality {
    HIGH("high", "High", WeatherQuality.HIGH, true, true, 1, true, 48),
    BALANCED("balanced", "Balanced", WeatherQuality.BALANCED, true, true, 2, true, 32),
    PERFORMANCE("performance", "Performance", WeatherQuality.PERFORMANCE, false, false, 3, false, 18),
    LOW_SPEC("low_spec", "Low Spec", WeatherQuality.LOW_SPEC, false, false, 6, false, 8);

    public final String key;
    public final String label;
    public final WeatherQuality maxWeatherQuality;
    public final boolean antialiasing;
    public final boolean qualityRendering;
    public final int terrainAnimationFrameStride;
    public final boolean ambientWorldEffects;
    public final int maxWorldLights;

    RenderQuality(
            String key,
            String label,
            WeatherQuality maxWeatherQuality,
            boolean antialiasing,
            boolean qualityRendering,
            int terrainAnimationFrameStride,
            boolean ambientWorldEffects,
            int maxWorldLights
    ) {
        this.key = key;
        this.label = label;
        this.maxWeatherQuality = maxWeatherQuality;
        this.antialiasing = antialiasing;
        this.qualityRendering = qualityRendering;
        this.terrainAnimationFrameStride = Math.max(1, terrainAnimationFrameStride);
        this.ambientWorldEffects = ambientWorldEffects;
        this.maxWorldLights = Math.max(1, maxWorldLights);
    }

    public WeatherQuality effectiveWeatherQuality(WeatherQuality configured) {
        WeatherQuality safe = configured == null ? WeatherQuality.HIGH : configured;
        return safe.ordinal() < maxWeatherQuality.ordinal() ? maxWeatherQuality : safe;
    }

    public static RenderQuality fromKey(String key) {
        for (RenderQuality quality : values()) {
            if (quality.key.equals(key)) {
                return quality;
            }
        }
        return HIGH;
    }
}
