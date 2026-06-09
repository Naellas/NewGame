package com.alderfall.game;

public enum WeatherCondition {
    CLEAR("Clear"),
    CLOUDY("Cloudy"),
    RAIN("Rain"),
    STORM("Storm"),
    FOG("Fog"),
    SNOW("Snow"),
    BLIZZARD("Blizzard"),
    DUST("Sandstorm"),
    HEAT_HAZE("Dry Heat");

    private final String label;

    WeatherCondition(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
