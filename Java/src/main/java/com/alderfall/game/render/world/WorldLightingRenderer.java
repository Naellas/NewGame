package com.alderfall.game.render.world;

import com.alderfall.game.*;

import com.alderfall.game.map.WorldMap;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WorldLightingRenderer {
    private static final Color DEFAULT_SHADOW_LIGHT = new Color(255, 205, 130);
    private static final Color BIOME_TINT_TUNDRA = new Color(212, 235, 255);
    private static final Color BIOME_TINT_DESERT = new Color(255, 215, 145);
    private static final Color BIOME_TINT_MARSH = new Color(92, 150, 128);
    private static final Color BIOME_TINT_BADLANDS = new Color(190, 123, 92);
    private static final Color BIOME_TINT_FOREST = new Color(45, 93, 66);
    private static final Color BIOME_TINT_MOUNTAIN = new Color(185, 196, 205);
    private static final Color BIOME_TINT_WATER = new Color(70, 165, 215);
    private static final Color BIOME_TINT_ROAD = new Color(230, 196, 130);
    private static final Color BIOME_TINT_GRASS = new Color(176, 205, 127);
    private static final float[] RADIAL_GLOW_FRACTIONS = {0.0f, 0.42f, 1.0f};
    private static final float[] LAYERED_GLOW_FRACTIONS = {0.0f, 0.25f, 0.42f, 0.593f, 1.0f};
    private static final float[] LAYERED_CORE_FACTORS = {1.0f, 0.321f, 0.162f, 0.0f, 0.0f};
    private static final float[] LAYERED_SPREAD_FACTORS = {1.0f, 0.596f, 0.322f, 0.226f, 0.0f};
    private static final int MAX_ACTIVE_WORLD_LIGHTS = 48;
    private static final int LIGHT_STATE_HOLD_FRAMES = 24;

    private final GameState state;
    private final Effects effects;
    private final List<WorldLight> activeWorldLights = new ArrayList<>();
    private final InteriorLightField interiorLightField = new InteriorLightField();
    private final Map<String, BufferedImage> shadowMaskCache = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> eldest) {
            return size() > 96;
        }
    };
    private RenderContext context = new RenderContext(0, 0, 0, 1, 1, GameConfig.TILE, GameConfig.WIDTH - GameConfig.SIDEBAR_WIDTH, GameConfig.HEIGHT, 0.0, 0.0);
    private double shadowWorldOffsetX;
    private double shadowWorldOffsetY;
    private int maxActiveWorldLights = MAX_ACTIVE_WORLD_LIGHTS;
    private boolean casterShadowsEnabled = true;
    private boolean glowEffectsEnabled = true;
    private BufferedImage vignetteCache;
    private int vignetteInnerAlpha = -1;
    private int vignetteOuterAlpha = -1;
    private int cachedLightingTick = Integer.MIN_VALUE;
    private int cachedDaylightState;
    private float cachedLightingDaylight = 0.18f;
    private int cachedLightingMinutes;

    public WorldLightingRenderer(GameState state, Effects effects) {
        this.state = state;
        this.effects = effects;
    }

    public void useContext(RenderContext context) {
        this.context = context;
    }

    public void configurePerformance(int maxActiveWorldLights, boolean casterShadowsEnabled, boolean glowEffectsEnabled) {
        this.maxActiveWorldLights = Math.max(1, Math.min(MAX_ACTIVE_WORLD_LIGHTS, maxActiveWorldLights));
        this.casterShadowsEnabled = casterShadowsEnabled;
        this.glowEffectsEnabled = glowEffectsEnabled;
    }

    public int activeLightCount() {
        return activeWorldLights.size();
    }

    public void setShadowWorldOffset(double x, double y) {
        this.shadowWorldOffsetX = x;
        this.shadowWorldOffsetY = y;
    }

    public void clearLights() {
        activeWorldLights.clear();
        shadowWorldOffsetX = 0.0;
        shadowWorldOffsetY = 0.0;
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

    private int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }

    public void drawShadow(Graphics2D g, int x, int y, int w, int h) {
        if (isHouseInterior()) {
            Graphics2D contact = (Graphics2D) g.create();
            clipInteriorFloor(contact);
            drawSimpleShadow(contact, x, y, w, h);
            contact.dispose();
            return;
        }
        if (!casterShadowsEnabled) {
            drawSimpleShadow(g, x, y, w, h);
            return;
        }
        double altitude = sunAltitude();
        double horizon = 1.0 - altitude;
        double direction = shadowCastX();
        double sampleX = x + w / 2.0;
        double sampleY = y + h / 2.0;
        double sampleWorldX = shadowWorldOffsetX + sampleX;
        double sampleWorldY = shadowWorldOffsetY + sampleY;
        double lightInfluence = 0.0;
        double localCastX = 0.0;
        double localCastY = 0.0;
        double red = 0.0;
        double green = 0.0;
        double blue = 0.0;
        double weight = 0.0;
        for (WorldLight light : activeWorldLights) {
            double dx = sampleWorldX - light.x;
            double dy = sampleWorldY - light.y;
            double radius = light.shadowRadius();
            double distanceSq = dx * dx + dy * dy;
            if (distanceSq >= radius * radius) {
                continue;
            }
            double distance = Math.sqrt(distanceSq);
            double falloff = 1.0 - distance / radius;
            double influenceContribution = falloff * falloff * light.shadowStrength();
            double colorContribution = falloff * light.shadowStrength();
            lightInfluence += influenceContribution;
            if (distance > 0.001) {
                localCastX += (dx / distance) * influenceContribution;
                localCastY += (dy / distance) * influenceContribution;
            }
            red += light.color.getRed() * colorContribution;
            green += light.color.getGreen() * colorContribution;
            blue += light.color.getBlue() * colorContribution;
            weight += colorContribution;
        }
        lightInfluence = Math.min(1.0, lightInfluence);
        double localLength = Math.sqrt(localCastX * localCastX + localCastY * localCastY);
        if (localLength > 1.0) {
            localCastX /= localLength;
            localCastY /= localLength;
        }
        double sunBlend = 1.0 - Math.min(0.62, lightInfluence * 0.58);
        int castX = (int) Math.round(w * direction * (0.32 + horizon * 1.55) * sunBlend
                + localCastX * w * Math.min(0.84, lightInfluence * 0.80));
        int castY = (int) Math.round(h * (0.26 + horizon * 0.62) * sunBlend
                + Math.max(-0.55, Math.min(0.85, localCastY)) * h * Math.min(0.58, lightInfluence * 0.54));
        int castW = Math.max(1, (int) Math.round(w * (1.10 + horizon * 0.72)));
        int castH = Math.max(1, (int) Math.round(h * (0.72 + horizon * 0.44)));
        int contactInset = Math.max(1, w / 10);
        float strength = (float) (shadowStrength() * (1.0 - Math.min(0.58, lightInfluence * 0.68)));

        Graphics2D shadow = (Graphics2D) g.create();
        shadow.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Composite oldComposite = shadow.getComposite();

        float castAlpha = (float) (0.08f * strength * (0.35f + lightingDaylight() * 0.65f) * (1.0 - lightInfluence * 0.35));
        shadow.setComposite(AlphaComposite.SrcOver.derive(castAlpha));
        shadow.setColor(new Color(4, 6, 9));
        shadow.fillOval(x + castX - (castW - w) / 2, y + castY, castW, castH);

        shadow.setComposite(AlphaComposite.SrcOver.derive(0.24f * strength));
        shadow.setColor(new Color(0, 0, 0));
        shadow.fillOval(x + contactInset / 2, y, Math.max(1, w - contactInset), h);

        shadow.setComposite(AlphaComposite.SrcOver.derive(0.07f * strength));
        shadow.setColor(new Color(32, 35, 37));
        shadow.fillOval(x + contactInset, y + Math.max(1, h / 5), Math.max(1, w - contactInset * 2), Math.max(1, h / 2));

        if (lightInfluence > 0.02) {
            Color lightColor = weight <= 0.0
                    ? DEFAULT_SHADOW_LIGHT
                    : new Color(
                            clampColor((int) Math.round(red / weight)),
                            clampColor((int) Math.round(green / weight)),
                            clampColor((int) Math.round(blue / weight))
                    );
            shadow.setComposite(AlphaComposite.SrcOver.derive((float) Math.min(0.20, lightInfluence * 0.18)));
            shadow.setColor(new Color(lightColor.getRed(), lightColor.getGreen(), lightColor.getBlue()));
            shadow.fillOval(x - w / 8, y - h / 3, w + w / 4, h + h / 2);
        }

        shadow.setComposite(oldComposite);
        shadow.dispose();
    }

    public void drawCasterShadow(Graphics2D g, BufferedImage image, String cacheKey,
                                  int x, int y, int width, int height, boolean flipHorizontal, float baseAlpha) {
        if (!casterShadowsEnabled) {
            return;
        }
        if (image == null || width <= scaled(10) || height <= scaled(10)) {
            return;
        }
        if (isHouseInterior()) {
            drawInteriorCasterShadow(g, image, cacheKey, x, y, width, height, flipHorizontal, baseAlpha);
            return;
        }
        double footX = x + width / 2.0;
        double footY = y + height;
        double sampleWorldX = shadowWorldOffsetX + footX;
        double sampleWorldY = shadowWorldOffsetY + footY;
        double altitude = sunAltitude();
        double horizon = 1.0 - altitude;
        double direction = shadowCastX();
        double lightInfluence = 0.0;
        double localCastX = 0.0;
        double localCastY = 0.0;
        for (WorldLight light : activeWorldLights) {
            double dx = sampleWorldX - light.x;
            double dy = sampleWorldY - light.y;
            double radius = light.shadowRadius();
            double distanceSq = dx * dx + dy * dy;
            if (distanceSq >= radius * radius) {
                continue;
            }
            double distance = Math.sqrt(distanceSq);
            double falloff = 1.0 - distance / radius;
            double contribution = falloff * falloff * light.shadowStrength();
            lightInfluence += contribution;
            if (distance > 0.001) {
                localCastX += (dx / distance) * contribution;
                localCastY += (dy / distance) * contribution;
            }
        }
        lightInfluence = Math.min(1.0, lightInfluence);
        double localLength = Math.sqrt(localCastX * localCastX + localCastY * localCastY);
        if (localLength > 1.0) {
            localCastX /= localLength;
            localCastY /= localLength;
        }
        double sunBlend = 1.0 - Math.min(0.68, lightInfluence * 0.62);
        double casterScale = Math.max(0.72, Math.min(1.28, width / (double) Math.max(1, height)));
        double castX = width * direction * (0.18 + horizon * 0.98) * sunBlend
                + localCastX * width * Math.min(0.72, lightInfluence * 0.68);
        double castY = height * (0.06 + horizon * 0.24) * sunBlend
                + Math.max(-0.35, Math.min(0.55, localCastY)) * height * Math.min(0.22, lightInfluence * 0.20);
        double skew = direction * (0.30 + horizon * 0.86) * casterScale * sunBlend
                + localCastX * Math.min(0.58, lightInfluence * 0.52);
        double flatten = 0.20 + horizon * 0.18 + lightInfluence * 0.035;
        float alpha = (float) (baseAlpha * shadowStrength() * (0.55 + horizon * 0.35)
                * (1.0 - Math.min(0.50, lightInfluence * 0.46)));
        if (alpha <= 0.015f) {
            return;
        }

        BufferedImage mask = shadowMask(cacheKey, image);
        Graphics2D shadow = (Graphics2D) g.create();
        shadow.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        shadow.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        shadow.setComposite(AlphaComposite.SrcOver.derive(Math.max(0.0f, Math.min(0.34f, alpha))));
        shadow.translate(footX + castX, footY + castY);
        shadow.shear(skew, 0.0);
        shadow.scale(flipHorizontal ? -1.0 : 1.0, flatten);
        shadow.drawImage(mask, -width / 2, -height, width, height, null);
        shadow.dispose();
    }

    private BufferedImage shadowMask(String cacheKey, BufferedImage image) {
        String key = cacheKey + ":" + image.getWidth() + "x" + image.getHeight();
        BufferedImage cached = shadowMaskCache.get(key);
        if (cached != null) {
            return cached;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage mask = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        for (int i = 0; i < pixels.length; i++) {
            int alpha = (pixels[i] >>> 24) & 0xff;
            if (alpha <= 4) {
                pixels[i] = 0;
            } else {
                pixels[i] = (Math.min(185, alpha) << 24);
            }
        }
        mask.setRGB(0, 0, width, height, pixels, 0, width);
        shadowMaskCache.put(key, mask);
        return mask;
    }

    private boolean isHouseInterior() {
        return "interior".equals(state.world.kind(state.currentMapId));
    }

    private void clipInteriorFloor(Graphics2D g) {
        if (interiorLightField.floor() != null) {
            g.clip(java.awt.geom.AffineTransform.getTranslateInstance(-shadowWorldOffsetX, -shadowWorldOffsetY)
                    .createTransformedShape(interiorLightField.floor()));
        }
    }

    private void drawInteriorCasterShadow(Graphics2D g, BufferedImage image, String key,
                                          int x, int y, int width, int height, boolean flip, float alpha) {
        double fx = x + width / 2.0, fy = y + height - 2;
        double wx = fx + shadowWorldOffsetX, wy = fy + shadowWorldOffsetY;
        WorldLight strongest = null;
        double strength = 0;
        for (WorldLight light : activeWorldLights) {
            double distance = Math.hypot(wx - light.x, wy - light.y);
            if (!light.affectsShadows || distance < context.tileSize() * 0.4 || distance >= light.radius * 1.8) continue;
            if (!interiorLightField.visible(state.world, light.sourceX, light.sourceY, light.radius * 2, context.tileSize())
                    .contains(wx, wy)) continue;
            double influence = light.alpha * (1 - distance / (light.radius * 1.8));
            if (influence > strength) { strength = influence; strongest = light; }
        }
        if (strongest == null) return;
        double distance = Math.max(1, Math.hypot(wx - strongest.x, wy - strongest.y));
        double length = Math.min(height * 0.6, context.tileSize() * 0.85);
        double dx = (wx - strongest.x) / distance * length;
        double dy = (wy - strongest.y) / distance * length;
        Graphics2D shadow = (Graphics2D) g.create();
        clipInteriorFloor(shadow);
        shadow.setComposite(AlphaComposite.SrcOver.derive(Math.min(0.30f, alpha * (float) (0.4 + strength))));
        // Keep the silhouette's foot fixed; project its top away from the emitter.
        shadow.translate(fx, fy);
        shadow.transform(new java.awt.geom.AffineTransform(flip ? -1 : 1, 0, -dx / height, -dy / height, 0, 0));
        shadow.drawImage(shadowMask(key, image), -width / 2, -height, width, height, null);
        shadow.dispose();
    }

    private void drawSimpleShadow(Graphics2D g, int x, int y, int w, int h) {
        Graphics2D shadow = (Graphics2D) g.create();
        Composite oldComposite = shadow.getComposite();
        shadow.setComposite(AlphaComposite.SrcOver.derive(0.20f * shadowStrength()));
        shadow.setColor(new Color(0, 0, 0));
        int inset = Math.max(1, w / 10);
        shadow.fillOval(x + inset / 2, y, Math.max(1, w - inset), h);
        shadow.setComposite(oldComposite);
        shadow.dispose();
    }



    private double shadowCastX() {
        return (sunProgress() - 0.5) * 2.0;
    }

    private double sunProgress() {
        refreshLightingState();
        return clamp((cachedLightingMinutes - 360) / 720.0, 0.0, 1.0);
    }

    private double sunAltitude() {
        return Math.max(0.12, Math.sin(Math.PI * sunProgress()));
    }

    private float shadowStrength() {
        double daylight = lightingDaylight();
        double altitude = sunAltitude();
        double horizonBoost = (1.0 - altitude) * 0.28;
        return (float) Math.max(0.38, Math.min(1.0, daylight * 0.62 + horizonBoost));
    }

    public void rebuildWorldLights(List<WorldProp> nearbyWorldProps, int camX, int camY, int visibleCols, int visibleRows, int tileSize) {
        activeWorldLights.clear();
        if (isHouseInterior()) interiorLightField.update(state.world, state.currentMapId, tileSize);
        for (WorldProp prop : nearbyWorldProps) {
            Color glow = propGlowColor(prop.asset());
            if (glow == null) {
                continue;
            }
            int cx = worldTileCenter(prop.x(), tileSize);
            int cy = worldTileCenter(prop.y(), tileSize);
            PropPlacement.Placement placement = PropPlacement.at(state.world, state.currentMapId, prop);
            if (placement.kind() != PropPlacement.Kind.FIXED) {
                cx += (int) Math.round((placement.x() - 0.5) * tileSize);
                cy += (int) Math.round((placement.y() - 1.0) * tileSize);
            }
            if (prop.asset().startsWith("interior_")) {
                int[] footprint = state.world.interiorVisualFootprint(prop.asset());
                cx = Math.round((prop.x() + footprint[0] * 0.5f) * tileSize);
                cy = Math.round((prop.y() + footprint[1] * 0.62f) * tileSize);
                if (prop.asset().contains("sconce")) cy = Math.round((prop.y() + 0.4f) * tileSize);
                if (prop.asset().contains("tabletop_candle")) cy = Math.round((prop.y() + 0.32f) * tileSize);
            }
            float pulse = propGlowPulse(prop);
            float alpha = propGlowAlpha(prop.asset()) * lightVisibilityForAsset(prop.asset()) * pulse;
            if (isHouseInterior() && prop.asset().startsWith("interior_wall_")) {
                var face = ConnectedInteriorWalls.bounds(state.world, state.currentMapId, prop.x(), prop.y(),
                        prop.x() * tileSize, prop.y() * tileSize, tileSize);
                face.width = state.world.interiorVisualFootprint(prop.asset())[0] * tileSize;
                addWorldLight(new WorldLight(cx + prop.offsetX() * tileSize / 48.0,
                        face.y + face.height * .43 + prop.offsetY() * tileSize / 48.0,
                        propGlowRadius(prop.asset()), glow, alpha, true, cx, cy, face));
            } else addWorldLight(cx, cy, propGlowRadius(prop.asset()), glow, alpha, true);
        }

        float visibility = lightVisibility();
        if (visibility >= 0.28f) {
            if (effects.isSettlementMapKind(state.currentMapId)) {
                addCityBuildingWorldLights(tileSize, camX, camY, visibleCols, visibleRows, visibility);
            } else {
                addTerrainWindowWorldLights(tileSize, camX, camY, visibleCols, visibleRows, visibility);
            }
        }

        addSettlementWorldLights(tileSize, camX, camY, visibleCols, visibleRows);
        if (!isHouseInterior()) addPlayerWorldLight(tileSize);
    }

    private void addTerrainWindowWorldLights(int tileSize, int camX, int camY, int visibleCols, int visibleRows, float visibility) {
        for (int sy = 0; sy < visibleRows; sy++) {
            for (int sx = 0; sx < visibleCols; sx++) {
                int wx = camX + sx;
                int wy = camY + sy;
                char tile = state.world.tileAt(state.currentMapId, wx, wy);
                int seed = Math.abs(wx * 928371 + wy * 364479 + state.currentMapId.hashCode());
                if (!isWindowLightTile(tile, seed)) {
                    continue;
                }
                int cx = worldTileCenter(wx, tileSize) + scaled(Math.floorMod(seed / 13, 13) - 6);
                int cy = worldTileCenter(wy, tileSize) + scaled(Math.floorMod(seed / 31, 11) - 5);
                float pulse = lightState(seed, 0.84f, 0.015f);
                Color color = tile == 'n' ? new Color(172, 219, 255) : new Color(255, 204, 118);
                addWorldLight(cx, cy, scaled(tile == 'c' || tile == 'u' ? 84 : 48), color, 0.16f * visibility * pulse, true);
            }
        }
    }

    private void addCityBuildingWorldLights(int tileSize, int camX, int camY, int visibleCols, int visibleRows, float visibility) {
        String kind = state.world.kind(state.currentMapId);
        for (CityBuilding building : state.world.cityBuildings(state.currentMapId)) {
            Rectangle bounds = effects.settlementBuildingWorldBounds(kind, building, tileSize);
            if (!worldLightIntersectsView(bounds.getCenterX(), bounds.getCenterY(), Math.max(bounds.width, bounds.height) / 2 + scaled(56),
                    camX, camY, visibleCols, visibleRows, tileSize)) {
                continue;
            }
            addBuildingLanternWorldLights(building, tileSize, visibility);
            addBuildingWindowWorldLights(kind, building, tileSize, visibility);
        }
    }


    private void addBuildingLanternWorldLights(CityBuilding building, int tileSize, float visibility) {
        if (building.width() < 4) {
            return;
        }
        int lotX = building.x1() * tileSize;
        int lotW = building.width() * tileSize;
        int frontY = (building.y2() + 1) * tileSize;
        int lanternW = scaled(10);
        int lanternH = scaled(18);
        int y = frontY - lanternH - scaled(20) + lanternH / 2;
        int leftX = lotX + scaled(4) + lanternW / 2;
        int rightX = lotX + lotW - scaled(4) - lanternW / 2;
        int seed = Math.abs(buildingLightSeed(building));
        float leftPulse = lanternPulse(seed);
        float rightPulse = lanternPulse(seed + 431);
        Color color = new Color(255, 203, 112);
        addWorldLight(leftX, y, scaled(58), color, 0.22f * visibility * leftPulse, true);
        addWorldLight(rightX, y, scaled(58), color, 0.22f * visibility * rightPulse, true);
    }

    private void addBuildingWindowWorldLights(String kind, CityBuilding building, int tileSize, float visibility) {
        int modules = effects.buildingModuleCount(building);
        int lotX = building.x1() * tileSize;
        int lotW = building.width() * tileSize;
        int frontY = (building.y2() + 1) * tileSize;
        int seed = Math.abs(buildingLightSeed(building));
        Color color = buildingWindowLightColor(building);
        for (int index = 0; index < modules; index++) {
            Rectangle module = effects.buildingModuleWorldBounds(kind, building, index, modules, seed, tileSize, lotX, lotW, frontY);
            int lightCount = building.width() >= 6 && module.width >= tileRelative(64, tileSize) ? 2 : 1;
            for (int i = 0; i < lightCount; i++) {
                int lightSeed = seed + index * 919 + i * 337;
                if (!buildingWindowIsLit(building, lightSeed)) {
                    continue;
                }
                double xBias = lightCount == 1 ? 0.50 : (i == 0 ? 0.34 : 0.66);
                int x = module.x + (int) Math.round(module.width * xBias);
                int row = Math.floorMod(lightSeed / 53, module.height > tileRelative(104, tileSize) ? 2 : 1);
                int y = module.y + (int) Math.round(module.height * (row == 0 ? 0.45 : 0.62));
                float pulse = windowPulse(lightSeed);
                int radius = "mage_tower".equals(building.style()) ? scaled(54) : scaled(46);
                addWorldLight(x, y, radius, color, 0.13f * visibility * pulse, false);
            }
        }
    }


    private Color buildingWindowLightColor(CityBuilding building) {
        if ("mage_tower".equals(building.style())) {
            return new Color(176, 221, 255);
        }
        if ("sun_shrine".equals(building.style())) {
            return new Color(255, 224, 142);
        }
        return new Color(255, 211, 126);
    }

    private boolean buildingWindowIsLit(CityBuilding building, int seed) {
        int threshold = switch (building.style()) {
            case "warehouse", "barracks" -> 42;
            case "inn", "shop", "guild", "hall", "river_hall" -> 78;
            case "mage_tower", "sun_shrine", "bell_tower" -> 70;
            default -> 58;
        };
        return Math.floorMod(seed / 17, 100) < threshold;
    }

    private float lanternPulse(int seed) {
        return lightState(seed, 0.88f, 0.025f);
    }

    private float windowPulse(int seed) {
        return lightState(seed, 0.90f, 0.015f);
    }

    private int buildingLightSeed(CityBuilding building) {
        int keyHash = building.key() == null ? 0 : building.key().hashCode();
        return keyHash ^ building.style().hashCode() ^ building.x1() * 928371 ^ building.y1() * 364479;
    }

    private void addSettlementWorldLights(int tileSize, int camX, int camY, int visibleCols, int visibleRows) {
        if (!WorldMap.OVERWORLD_ID.equals(state.currentMapId) || lightVisibility() < 0.24f) {
            return;
        }
        int[][] settlements = {
                {82, 105, 128}, {152, 145, 128}, {205, 78, 128}, {228, 185, 128}, {150, 230, 128},
                {112, 158, 92}, {83, 62, 92}, {102, 245, 92}, {240, 153, 92}
        };
        float visibility = lightVisibility();
        for (int[] settlement : settlements) {
            int cx = worldTileCenter(settlement[0], tileSize);
            int cy = worldTileCenter(settlement[1], tileSize) + scaled(8);
            int radius = scaled(settlement[2]);
            if (!worldLightIntersectsView(cx, cy, radius + scaled(48), camX, camY, visibleCols, visibleRows, tileSize)) {
                continue;
            }
            addWorldLight(cx, cy, radius, new Color(255, 191, 104), 0.15f * visibility, true);
            for (int i = 0; i < 5; i++) {
                int seed = Math.abs(settlement[0] * 7349 + settlement[1] * 9127 + i * 1451);
                int sx = cx + scaled(Math.floorMod(seed, 70) - 35);
                int sy = cy + scaled(Math.floorMod(seed / 41, 48) - 18);
                float pulse = lightState(seed, 0.78f, 0.02f);
                addWorldLight(sx, sy, scaled(30), new Color(255, 219, 142), 0.08f * visibility * pulse, false);
            }
        }
    }

    private boolean worldLightIntersectsView(double x, double y, int radius, int camX, int camY, int visibleCols, int visibleRows, int tileSize) {
        double minX = camX * (double) tileSize - radius;
        double minY = camY * (double) tileSize - radius;
        double maxX = (camX + visibleCols) * (double) tileSize + radius;
        double maxY = (camY + visibleRows) * (double) tileSize + radius;
        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }

    private void addPlayerWorldLight(int tileSize) {
        int cx = (int) Math.round(context.playerX() * tileSize) + tileSize / 2;
        int cy = (int) Math.round(context.playerY() * tileSize) + tileSize / 2 + scaled(4);
        addWorldLight(cx, cy, scaled(118), new Color(255, 229, 168), 0.055f + nightFactor() * 0.09f, true);
    }

    private void addWorldLight(double x, double y, int radius, Color color, float alpha, boolean affectsShadows) {
        addWorldLight(new WorldLight(x, y, Math.max(1, radius), color, Math.min(1.0f, alpha), affectsShadows));
    }

    private void addWorldLight(WorldLight candidate) {
        if (candidate.alpha <= 0.005f) {
            return;
        }
        if (activeWorldLights.size() < maxActiveWorldLights) {
            activeWorldLights.add(candidate);
            return;
        }
        int weakestIndex = 0;
        double weakestImportance = worldLightImportance(activeWorldLights.get(0));
        for (int i = 1; i < activeWorldLights.size(); i++) {
            double importance = worldLightImportance(activeWorldLights.get(i));
            if (importance < weakestImportance) {
                weakestImportance = importance;
                weakestIndex = i;
            }
        }
        if (worldLightImportance(candidate) > weakestImportance * 1.08) {
            activeWorldLights.set(weakestIndex, candidate);
        }
    }

    private double worldLightImportance(WorldLight light) {
        return light.radius * light.alpha * (light.affectsShadows ? 1.2 : 1.0);
    }

    private int worldTileCenter(int coordinate, int tileSize) {
        return coordinate * tileSize + tileSize / 2;
    }

    public void drawLightTileSpill(Graphics2D g, int camX, int camY, int visibleCols, int visibleRows, int tileSize) {
        if (activeWorldLights.isEmpty()) {
            return;
        }
        for (WorldLight light : activeWorldLights) {
            int cx = (int) Math.round(light.x - camX * (double) tileSize);
            int cy = (int) Math.round(light.y - camY * (double) tileSize);
            int spreadRadius = Math.max(2, (int) Math.round(light.radius * 1.82));
            if (cx + spreadRadius < -tileSize || cy + spreadRadius < -tileSize
                    || cx - spreadRadius >= visibleCols * tileSize + tileSize
                    || cy - spreadRadius >= visibleRows * tileSize + tileSize) {
                continue;
            }
            Graphics2D spill = (Graphics2D) g.create();
            clipInteriorLight(spill, light, camX * (double) tileSize, camY * (double) tileSize, tileSize);
            drawLayeredRadialGlow(spill, cx, cy, spreadRadius, light.color,
                    Math.min(0.20f, light.alpha * 0.82f), Math.min(0.11f, light.alpha * 0.30f));
            spill.dispose();
        }
    }

    private void drawLayeredRadialGlow(Graphics2D g, int cx, int cy, int spreadRadius,
                                        Color color, float coreAlpha, float spreadAlpha) {
        Color[] colors = new Color[LAYERED_GLOW_FRACTIONS.length];
        for (int i = 0; i < LAYERED_GLOW_FRACTIONS.length; i++) {
            float core = coreAlpha * LAYERED_CORE_FACTORS[i];
            float spread = spreadAlpha * LAYERED_SPREAD_FACTORS[i];
            float combined = 1.0f - (1.0f - core) * (1.0f - spread);
            colors[i] = new Color(color.getRed(), color.getGreen(), color.getBlue(),
                    clamp(Math.round(combined * 255), 0, 255));
        }
        Composite oldComposite = g.getComposite();
        Paint oldPaint = g.getPaint();
        g.setComposite(AlphaComposite.SrcOver);
        g.setPaint(new RadialGradientPaint(cx, cy, spreadRadius, LAYERED_GLOW_FRACTIONS, colors));
        g.fillOval(cx - spreadRadius, cy - spreadRadius, spreadRadius * 2, spreadRadius * 2);
        g.setPaint(oldPaint);
        g.setComposite(oldComposite);
    }

    public void drawBiomeLightTints(Graphics2D g, int camX, int camY, int visibleCols, int visibleRows, int tileSize) {
        if (effects.isInteriorAtmosphereMap()) {
            effects.drawInteriorLightTints(g, camX, camY, visibleCols, visibleRows, tileSize);
            return;
        }
        Composite oldComposite = g.getComposite();
        float daylight = lightingDaylight();
        for (int sy = 0; sy < visibleRows; sy++) {
            int runStart = 0;
            char runTile = 0;
            for (int sx = 0; sx <= visibleCols; sx++) {
                char tile = 0;
                if (sx < visibleCols) {
                    int wx = camX + sx;
                    int wy = camY + sy;
                    tile = biomeLightGroup(effects.visibleTerrainTile(
                            state.world.tileAt(state.currentMapId, wx, wy), wx, wy));
                }
                if (sx == 0) {
                    runTile = tile;
                    continue;
                }
                if (sx == visibleCols || tile != runTile) {
                    float alpha = biomeLightAlpha(runTile) * (0.72f + daylight * 0.55f);
                    g.setComposite(AlphaComposite.SrcOver.derive(alpha));
                    g.setColor(biomeLightTint(runTile));
                    g.fillRect(runStart * tileSize, sy * tileSize, (sx - runStart) * tileSize, tileSize);
                    runStart = sx;
                    runTile = tile;
                }
            }
        }
        g.setComposite(oldComposite);
    }

    private char biomeLightGroup(char tile) {
        return switch (tile) {
            case 'n', 's', 'v', 'b', 'f', 'w' -> tile;
            case 'm', 'q' -> 'm';
            case 'r', 'T', 'K', 'c', 'u' -> 'r';
            default -> 'g';
        };
    }

    private Color biomeLightTint(char tile) {
        return switch (tile) {
            case 'n' -> BIOME_TINT_TUNDRA;
            case 's' -> BIOME_TINT_DESERT;
            case 'v' -> BIOME_TINT_MARSH;
            case 'b' -> BIOME_TINT_BADLANDS;
            case 'f' -> BIOME_TINT_FOREST;
            case 'm', 'q' -> BIOME_TINT_MOUNTAIN;
            case 'w' -> BIOME_TINT_WATER;
            case 'r', 'T', 'K', 'c', 'u' -> BIOME_TINT_ROAD;
            default -> BIOME_TINT_GRASS;
        };
    }

    private float biomeLightAlpha(char tile) {
        return switch (tile) {
            case 'f', 'v' -> 0.045f;
            case 's', 'n', 'w' -> 0.036f;
            case 'b', 'm', 'q' -> 0.030f;
            case 'r', 'T', 'K', 'c', 'u' -> 0.026f;
            default -> 0.024f;
        };
    }

    public void drawEmissiveWorldLights(Graphics2D g, int camX, int camY, int tileSize, double cameraOffsetX, double cameraOffsetY) {
        Graphics2D lights = (Graphics2D) g.create();
        lights.setClip(0, 0, effects.gameAreaWidth(), effects.viewHeight());
        lights.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (WorldLight light : activeWorldLights) {
            int cx = (int) Math.round(light.x - camX * tileSize - cameraOffsetX);
            int cy = (int) Math.round(light.y - camY * tileSize - cameraOffsetY);
            if (cx + light.radius < 0 || cy + light.radius < 0
                    || cx - light.radius >= effects.gameAreaWidth() || cy - light.radius >= effects.viewHeight()) {
                continue;
            }
            Graphics2D glow = (Graphics2D) lights.create();
            clipInteriorLight(glow, light, camX * (double) tileSize + cameraOffsetX,
                    camY * (double) tileSize + cameraOffsetY, tileSize);
            drawRadialGlow(glow, cx, cy, light.radius, light.color, light.alpha);
            glow.dispose();
            if (light.radius <= scaled(36) || light.alpha > 0.26f) {
                drawSpark(lights, cx, cy, light.color, Math.min(0.48f, light.alpha * 1.35f));
            }
        }
        lights.dispose();
    }

    private void clipInteriorLight(Graphics2D g, WorldLight light, double ox, double oy, int tileSize) {
        if (!isHouseInterior()) return;
        java.awt.Shape visible = interiorLightField.visible(state.world, light.sourceX, light.sourceY, light.radius * 2, tileSize);
        if (light.mountingFace != null) {
            var combined = new java.awt.geom.Area(visible);
            int row = (int) Math.floor(light.sourceY / tileSize);
            for (int x = (int) Math.floor((light.x - light.radius) / tileSize);
                 x <= (int) Math.floor((light.x + light.radius) / tileSize); x++) {
                if (state.world.tileAt(state.currentMapId, x, row) == 'o')
                    combined.add(new java.awt.geom.Area(ConnectedInteriorWalls.bounds(state.world, state.currentMapId,
                            x, row, x * tileSize, row * tileSize, tileSize)));
            }
            visible = combined;
        }
        g.clip(java.awt.geom.AffineTransform.getTranslateInstance(-ox, -oy).createTransformedShape(visible));
    }



    private boolean isWindowLightTile(char tile, int seed) {
        int roll = Math.floorMod(seed, 100);
        return switch (tile) {
            case 'c', 'u' -> roll < 48;
            case 'p', 'a', 'j', 'l', 'y' -> roll < 12;
            case 'h' -> roll < 32;
            default -> false;
        };
    }



    public Color propGlowColor(String asset) {
        if (asset.startsWith("interior_wall_window")) return new Color(187, 214, 239);
        if (asset.startsWith("town_portal_")) {
            if (asset.contains("snow")) {
                return new Color(112, 220, 255);
            }
            if (asset.contains("desert")) {
                return new Color(93, 238, 226);
            }
            if (asset.contains("marsh")) {
                return new Color(66, 231, 205);
            }
            if (asset.contains("green")) {
                return new Color(174, 255, 105);
            }
            return new Color(116, 178, 255);
        }
        if (asset.contains("camp_fire") || asset.contains("campfire")) {
            return new Color(255, 151, 58);
        }
        if (asset.contains("forge") || asset.contains("oven") || asset.contains("stove")
                || asset.contains("hearth") || asset.equals("interior_fireplace")) {
            return new Color(255, 154, 72);
        }
        if (asset.contains("street_lamp") || asset.contains("lantern") || asset.contains("sconce")
                || asset.contains("tabletop_candle")) {
            return new Color(255, 203, 112);
        }
        if (asset.contains("crystal") || asset.contains("ice_crystals")) {
            return new Color(98, 214, 255);
        }
        if (asset.contains("rune") || asset.contains("shrine")) {
            return new Color(117, 236, 205);
        }
        if (asset.contains("alchemy")) {
            return new Color(168, 117, 236);
        }
        if (asset.contains("fairy_pool") || asset.contains("bubble_pool") || asset.contains("firefly")) {
            return new Color(112, 232, 154);
        }
        if (asset.contains("thaw_pond") || asset.contains("spring_pool") || asset.contains("snowmelt_pool")) {
            return new Color(138, 220, 255);
        }
        return null;
    }

    private float propGlowAlpha(String asset) {
        if (asset.contains("camp_fire") || asset.contains("campfire") || asset.contains("fire")) {
            return 0.42f;
        }
        if (asset.contains("forge") || asset.contains("oven") || asset.contains("stove") || asset.contains("hearth")) {
            return 0.34f;
        }
        if (asset.contains("tabletop_candle") || asset.contains("sconce")) {
            return 0.30f;
        }
        if (asset.startsWith("town_portal_")) {
            return 0.46f;
        }
        if (asset.contains("street_lamp") || asset.contains("lantern")) {
            return 0.36f;
        }
        return 0.28f;
    }

    private float propGlowPulse(WorldProp prop) {
        String asset = prop.asset();
        int seed = prop.asset().hashCode() ^ prop.x() * 928371 ^ prop.y() * 364479;
        if (WorldPropRenderer.isFireProp(asset)) {
            return lightState(seed, 0.88f, 0.035f);
        }
        if (WorldPropRenderer.isMagicGlowProp(asset)) {
            return lightState(seed, 0.90f, 0.03f);
        }
        return lightState(seed, 0.90f, 0.02f);
    }

    private float lightState(int seed, float base, float variation) {
        int step = Math.floorDiv(context.frame(), LIGHT_STATE_HOLD_FRAMES);
        int mixed = seed ^ step * 0x9e3779b9;
        mixed ^= mixed >>> 16;
        mixed *= 0x7feb352d;
        mixed ^= mixed >>> 15;
        int level = Math.floorMod(mixed, 5) - 2;
        return base + variation * level * 0.5f;
    }

    public float lightVisibilityForAsset(String asset) {
        if (isHouseInterior()) {
            if (asset.startsWith("interior_wall_window")) return lightingDaylight();
            return 0.88f + nightFactor() * 0.12f;
        }
        float night = nightFactor();
        if (asset.startsWith("town_portal_")) {
            return 0.44f + night * 0.94f;
        }
        if (asset.contains("crystal") || asset.contains("rune") || asset.contains("shrine") || asset.contains("alchemy")
                || asset.contains("fairy_pool") || asset.contains("bubble_pool") || asset.contains("firefly")) {
            return 0.34f + night * 0.88f;
        }
        if (asset.contains("camp_fire") || asset.contains("campfire") || asset.contains("fire")
                || asset.contains("cookpot") || asset.contains("cooking_station")
                || asset.contains("forge") || asset.contains("oven") || asset.contains("stove")
                || asset.contains("hearth") || asset.contains("tabletop_candle")
                || asset.contains("street_lamp") || asset.contains("lantern") || asset.contains("sconce")) {
            return 0.12f + night * 1.10f;
        }
        return lightVisibility();
    }

    private int propGlowRadius(String asset) {
        if (asset.startsWith("interior_wall_window")) return scaled(130);
        if (asset.contains("camp_fire") || asset.contains("campfire") || asset.contains("fire")) {
            return scaled(96);
        }
        if (asset.contains("forge") || asset.contains("oven") || asset.contains("stove") || asset.contains("hearth")) {
            return scaled(88);
        }
        if (asset.contains("tabletop_candle")) {
            return scaled(42);
        }
        if (asset.contains("street_lamp") || asset.contains("lantern") || asset.contains("sconce")) {
            return scaled(72);
        }
        if (asset.contains("crystal") || asset.contains("rune") || asset.contains("shrine")) {
            return scaled(78);
        }
        if (asset.startsWith("town_portal_")) {
            return scaled(112);
        }
        return scaled(66);
    }

    private float lightVisibility() {
        if (effects.isInteriorAtmosphereMap()) {
            return 0.58f + nightFactor() * 0.42f;
        }
        return (float) Math.min(1.22, 0.20 + nightFactor() * 1.05);
    }

    public float nightFactor() {
        return (float) Math.max(0.0, Math.min(1.0, (0.92 - lightingDaylight()) / 0.74));
    }

    private int daylightState() {
        refreshLightingState();
        return cachedDaylightState;
    }

    private float lightingDaylight() {
        refreshLightingState();
        return cachedLightingDaylight;
    }

    private void refreshLightingState() {
        if (cachedLightingTick == state.worldTick) {
            return;
        }
        cachedLightingTick = state.worldTick;
        double normalized = (state.daylightLevel() - 0.18) / 0.82;
        cachedDaylightState = clamp((int) Math.round(normalized * 24.0), 0, 24);
        cachedLightingDaylight = (float) (0.18 + cachedDaylightState * (0.82 / 24.0));
        cachedLightingMinutes = state.timeOfDayMinutes() / 10 * 10;
    }



    private void drawSpark(Graphics2D g, int cx, int cy, Color color, float alpha) {
        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0.0f, Math.min(1.0f, alpha))));
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 230));
        int size = scaled(3);
        g.fillOval(cx - size / 2, cy - size / 2, size, size);
        g.setComposite(oldComposite);
    }

    public void drawRadialGlow(Graphics2D g, int cx, int cy, int radius, Color color, float alpha) {
        if (!glowEffectsEnabled) {
            Composite oldComposite = g.getComposite();
            g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0.0f, Math.min(0.18f, alpha * 0.7f))));
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 150));
            int simpleRadius = Math.max(1, radius / 3);
            g.fillOval(cx - simpleRadius, cy - simpleRadius, simpleRadius * 2, simpleRadius * 2);
            g.setComposite(oldComposite);
            return;
        }
        Composite oldComposite = g.getComposite();
        Paint oldPaint = g.getPaint();
        Color[] colors = {
                new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(255, Math.round(alpha * 255))),
                new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(255, Math.round(alpha * 82))),
                new Color(color.getRed(), color.getGreen(), color.getBlue(), 0)
        };
        g.setComposite(AlphaComposite.SrcOver);
        g.setPaint(new RadialGradientPaint(cx, cy, Math.max(1, radius), RADIAL_GLOW_FRACTIONS, colors));
        g.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
        g.setPaint(oldPaint);
        g.setComposite(oldComposite);
    }

    public void drawWorldVignette(Graphics2D g) {
        int width = effects.gameAreaWidth();
        int height = effects.viewHeight();
        float edgeAlpha = 0.10f + nightFactor() * 0.12f;
        int innerAlpha = Math.round(edgeAlpha * 70);
        int outerAlpha = Math.round(edgeAlpha * 255);
        if (vignetteCache == null || vignetteCache.getWidth() != width || vignetteCache.getHeight() != height
                || vignetteInnerAlpha != innerAlpha || vignetteOuterAlpha != outerAlpha) {
            vignetteCache = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D vignette = vignetteCache.createGraphics();
            vignette.setPaint(new RadialGradientPaint(
                width * 0.46f,
                height * 0.42f,
                Math.max(width, height) * 0.68f,
                new float[]{0.0f, 0.74f, 1.0f},
                new Color[]{
                        new Color(0, 0, 0, 0),
                        new Color(10, 13, 20, innerAlpha),
                        new Color(8, 10, 18, outerAlpha)
                }
            ));
            vignette.fillRect(0, 0, width, height);
            vignette.dispose();
            vignetteInnerAlpha = innerAlpha;
            vignetteOuterAlpha = outerAlpha;
        }
        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver);
        g.drawImage(vignetteCache, 0, 0, null);
        g.setComposite(oldComposite);
    }


    public record RenderContext(int frame, int camX, int camY, int visibleCols, int visibleRows, int tileSize,
                         int gameAreaWidth, int viewHeight, double playerX, double playerY) {
    }

    public interface Effects {
        int scaled(int value);

        int tileRelative(int value, int tileSize);

        int gameAreaWidth();

        int viewHeight();

        boolean isInteriorAtmosphereMap();

        public void drawInteriorLightTints(Graphics2D g, int camX, int camY, int visibleCols, int visibleRows, int tileSize);

        char visibleTerrainTile(char tile, int wx, int wy);

        boolean isSettlementMapKind(String mapId);

        int buildingModuleCount(CityBuilding building);

        Rectangle settlementBuildingWorldBounds(String kind, CityBuilding building, int tileSize);

        Rectangle buildingModuleWorldBounds(String kind, CityBuilding building, int index, int modules, int seed,
                                            int tileSize, int lotX, int lotW, int frontY);

        default int settlementLightRadius(WorldMap.SettlementSite settlement) {
            return switch (settlement.kind()) {
                case "Capital" -> 146;
                case "City" -> 126;
                case "Town" -> 96;
                default -> 74;
            };
        }
    }
}
