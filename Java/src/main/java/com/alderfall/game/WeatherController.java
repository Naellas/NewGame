package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
final class WeatherController {
    private static final int WEATHER_BLOCK_TICKS = 1200;
    private static final int WIND_BLOCK_TICKS = 900;
    private static final String[] WIND_LABELS = {"E", "SE", "S", "SW", "W", "NW", "N", "NE"};

    private final WorldMap world;

    WeatherController(WorldMap world) {
        this.world = world;
    }

    WeatherCondition currentWeather(String mapId, char biome, int worldTick, int dayNumber) {
        if (!isOutdoorWeatherMap(mapId)) {
            return WeatherCondition.CLEAR;
        }
        int block = Math.floorDiv(worldTick, WEATHER_BLOCK_TICKS);
        int seed = Math.abs(biome * 7349 + block * 9127 + dayNumber * 1711);
        int roll = Math.floorMod(seed, 100);
        return switch (biome) {
            case 'f' -> weightedWeather(roll, new WeatherCondition[]{
                    WeatherCondition.CLEAR, WeatherCondition.CLOUDY, WeatherCondition.CLOUDY, WeatherCondition.RAIN,
                    WeatherCondition.RAIN, WeatherCondition.FOG, WeatherCondition.STORM
            });
            case 's' -> weightedWeather(roll, new WeatherCondition[]{
                    WeatherCondition.CLEAR, WeatherCondition.CLEAR, WeatherCondition.CLEAR, WeatherCondition.HEAT_HAZE,
                    WeatherCondition.HEAT_HAZE, WeatherCondition.DUST, WeatherCondition.CLOUDY
            });
            case 'n' -> weightedWeather(roll, new WeatherCondition[]{
                    WeatherCondition.CLEAR, WeatherCondition.CLOUDY, WeatherCondition.SNOW, WeatherCondition.SNOW,
                    WeatherCondition.SNOW, WeatherCondition.FOG, WeatherCondition.BLIZZARD
            });
            case 'v' -> weightedWeather(roll, new WeatherCondition[]{
                    WeatherCondition.FOG, WeatherCondition.FOG, WeatherCondition.CLOUDY, WeatherCondition.RAIN,
                    WeatherCondition.RAIN, WeatherCondition.STORM, WeatherCondition.CLEAR
            });
            case 'b' -> weightedWeather(roll, new WeatherCondition[]{
                    WeatherCondition.CLEAR, WeatherCondition.CLEAR, WeatherCondition.DUST, WeatherCondition.DUST,
                    WeatherCondition.HEAT_HAZE, WeatherCondition.CLOUDY, WeatherCondition.STORM
            });
            case 'm', 'q' -> weightedWeather(roll, new WeatherCondition[]{
                    WeatherCondition.CLEAR, WeatherCondition.CLOUDY, WeatherCondition.CLOUDY, WeatherCondition.FOG,
                    WeatherCondition.RAIN, WeatherCondition.SNOW, WeatherCondition.STORM
            });
            case 'w' -> weightedWeather(roll, new WeatherCondition[]{
                    WeatherCondition.CLOUDY, WeatherCondition.RAIN, WeatherCondition.RAIN, WeatherCondition.FOG,
                    WeatherCondition.FOG, WeatherCondition.STORM, WeatherCondition.CLEAR
            });
            default -> weightedWeather(roll, new WeatherCondition[]{
                    WeatherCondition.CLEAR, WeatherCondition.CLEAR, WeatherCondition.CLOUDY, WeatherCondition.CLOUDY,
                    WeatherCondition.RAIN, WeatherCondition.FOG, WeatherCondition.STORM
            });
        };
    }

    String weatherLabel(WeatherCondition weather) {
        return weather.label();
    }

    double windRadians(char biome, int worldTick, int dayNumber) {
        int block = Math.floorDiv(worldTick, WIND_BLOCK_TICKS);
        int seed = Math.abs(block * 48121 + biome * 1697 + dayNumber * 337);
        return (Math.floorMod(seed, 360) - 180) * Math.PI / 180.0;
    }

    double windStrength(WeatherCondition weather, char biome, int worldTick, int dayNumber) {
        int block = Math.floorDiv(worldTick, WIND_BLOCK_TICKS);
        int seed = Math.abs(block * 7349 + biome * 251 + dayNumber * 97);
        double gust = Math.floorMod(seed, 100) / 100.0;
        double base = switch (weather) {
            case STORM, BLIZZARD -> 0.72;
            case RAIN, SNOW, DUST -> 0.42;
            case CLOUDY, FOG, HEAT_HAZE -> 0.24;
            case CLEAR -> 0.16;
        };
        return Math.min(1.0, base + gust * 0.28);
    }

    String windLabel(double windRadians) {
        int index = Math.floorMod((int) Math.round(windRadians / (Math.PI / 4.0)), WIND_LABELS.length);
        return WIND_LABELS[index] + " wind";
    }

    private WeatherCondition weightedWeather(int roll, WeatherCondition[] conditions) {
        int index = Math.min(conditions.length - 1, roll * conditions.length / 100);
        return conditions[index];
    }

    private boolean isOutdoorWeatherMap(String mapId) {
        String kind = world.kind(mapId);
        return "overworld".equals(kind) || "city".equals(kind) || "village".equals(kind);
    }
}
