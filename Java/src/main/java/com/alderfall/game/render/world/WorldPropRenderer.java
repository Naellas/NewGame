package com.alderfall.game.render.world;

import com.alderfall.game.*;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public final class WorldPropRenderer {
    private final AssetStore assets;
    private final GameState state;
    private final Effects effects;

    public WorldPropRenderer(AssetStore assets, GameState state, Effects effects) {
        this.assets = assets;
        this.state = state;
        this.effects = effects;
    }

    public void drawWorldProp(Graphics2D g, WorldProp prop, WorldRenderer.PropContext context) {
        String asset = prop.asset();
        if (isDisallowedSettlementOutdoorProp(asset)) {
            return;
        }
        if (isVillageFenceAsset(asset)) {
            drawVillageFenceProp(g, prop, context);
            return;
        }
        int size = propRenderSize(asset, prop.size(), context.tileSize());
        int drawW = interiorPropWidth(asset, size);
        int drawH = interiorPropHeight(asset, size);
        int px = (prop.x() - context.camX()) * context.tileSize() + (context.tileSize() - drawW) / 2;
        int py = (prop.y() - context.camY()) * context.tileSize() + context.tileSize() - drawH;
        if (asset.startsWith("interior_")) {
            px += interiorPropOffsetX(asset, context.tileSize());
            py += interiorPropOffsetY(asset, context.tileSize());
        }

        if (isSoftGroundProp(asset)) {
            int seed = prop.x() * 928371 + prop.y() * 364479 + asset.hashCode();
            px += scaled(Math.floorMod(seed, 7) - 3);
            py += scaled(Math.floorMod(seed / 13, 5) - 2);
            drawPropGroundBlend(g, prop.x(), prop.y(), px, py, size, 0.32f, true);
            drawAnimatedPropImage(g, prop, asset, px, py, size, 0.90f, context.frame());
            drawPropGroundVeil(g, prop.x(), prop.y(), px, py, size, 0.16f);
            drawPropAmbientAnimation(g, prop, px, py, size, context.frame());
            return;
        }

        boolean naturalBlend = isNaturalLowProp(asset);
        if (naturalBlend) {
            drawPropGroundBlend(g, prop.x(), prop.y(), px, py, size, 0.20f, false);
        }
        if (castsPropShadow(asset)) {
            BufferedImage image = propImage(asset, drawW, drawH);
            effects.drawCasterShadow(g, image, "prop:" + asset + ":" + drawW + "x" + drawH,
                    px, py, drawW, drawH, false, 0.36f);
            effects.drawShadow(g, px + drawW / 6, py + drawH - scaled(8), drawW * 2 / 3, scaled(8));
        }
        if (asset.startsWith("interior_")) {
            drawPropImage(g, asset, px, py, drawW, drawH, 1.0f);
            drawPropAmbientAnimation(g, prop, px, py, Math.max(drawW, drawH), context.frame());
            return;
        }
        drawAnimatedPropImage(g, prop, asset, px, py, size, 1.0f, context.frame());
        if (naturalBlend) {
            drawPropGroundVeil(g, prop.x(), prop.y(), px, py, size, 0.08f);
        }
        drawPropAmbientAnimation(g, prop, px, py, size, context.frame());
    }

    public void drawFallingGatheredProp(Graphics2D g, WorldRenderer.PropContext context) {
        WorldProp prop = state.lastGatheredProp;
        if (prop == null || !state.currentMapId.equals(state.lastGatheredPropMapId)) {
            return;
        }
        int age = state.worldTick - state.lastGatheredPropWorldTick;
        if (age < 0 || age > 44) {
            return;
        }
        String asset = prop.asset();
        if (!isTreeGatherAsset(asset)) {
            return;
        }
        if (prop.x() < context.camX() - 1 || prop.y() < context.camY() - 1
                || prop.x() > context.camX() + context.visibleCols()
                || prop.y() > context.camY() + context.visibleRows()) {
            return;
        }
        int size = propRenderSize(asset, prop.size(), context.tileSize());
        int drawW = interiorPropWidth(asset, size);
        int drawH = interiorPropHeight(asset, size);
        int baseX = (prop.x() - context.camX()) * context.tileSize() + context.tileSize() / 2;
        int baseY = (prop.y() - context.camY()) * context.tileSize() + context.tileSize();
        double progress = Math.min(1.0, age / 34.0);
        double eased = 1.0 - Math.pow(1.0 - progress, 3.0);
        double direction = Math.floorMod(prop.x() * 31 + prop.y() * 17 + asset.hashCode(), 2) == 0 ? -1.0 : 1.0;
        double angle = direction * eased * 1.36;
        float alpha = (float) Math.max(0.0, 1.0 - Math.max(0.0, progress - 0.72) / 0.28 * 0.55);

        Composite oldComposite = g.getComposite();
        AffineTransform oldTransform = g.getTransform();
        effects.drawShadow(g, baseX - drawW / 3, baseY - scaled(7), drawW * 2 / 3, scaled(9));
        g.setComposite(AlphaComposite.SrcOver.derive(alpha));
        g.translate(baseX, baseY - scaled(3));
        g.rotate(angle);
        g.drawImage(propImage(asset, drawW, drawH), -drawW / 2, -drawH, drawW, drawH, null);
        g.setTransform(oldTransform);
        g.setComposite(oldComposite);
    }

    public void drawPropImage(Graphics2D g, String asset, int x, int y, int width, int height, float opacity) {
        Composite oldComposite = g.getComposite();
        if (opacity < 1.0f) {
            g.setComposite(AlphaComposite.SrcOver.derive(opacity));
        }
        BufferedImage image = propImage(asset, width, height);
        g.drawImage(image, x, y, null);
        g.setComposite(oldComposite);
    }

    public BufferedImage propImage(String asset, int width, int height) {
        return asset.startsWith("interior_")
                ? assets.image(asset, width, height)
                : assets.spriteFit(asset, width, height);
    }

    public int interiorPropWidth(String asset, int size) {
        if (asset.equals("interior_tavern_bar") || asset.equals("interior_shop_counter")
                || asset.equals("interior_carpenter_table") || asset.equals("interior_alchemy_station")
                || asset.equals("interior_cooking_station") || asset.equals("interior_herb_drying_rack")
                || asset.equals("interior_wall_window_wide") || asset.equals("interior_wall_plant_shelf")
                || asset.equals("interior_wall_herb_rack") || asset.equals("interior_floor_bushy_planter")
                || asset.equals("interior_aquarium_table") || asset.equals("interior_carpenter_workbench")
                || asset.equals("interior_metal_crate") || asset.equals("interior_bakery_counter")
                || asset.equals("interior_tavern_counter") || asset.equals("interior_long_table_benches")
                || asset.equals("interior_sawhorse_planks") || asset.equals("interior_storage_counter")
                || asset.equals("interior_low_cupboard") || asset.equals("interior_table_h_left")
                || asset.equals("interior_table_h_middle") || asset.equals("interior_table_h_right")
                || asset.equals("interior_bench_h") || asset.equals("interior_banquet_table_h")
                || asset.equals("interior_stool_table_h") || asset.equals("interior_study_desk_h")
                || asset.equals("interior_counter_corner_h")) {
            return size * 2;
        }
        return size;
    }

    public int propRenderSize(String asset, int logicalSize, int tileSize) {
        int zoomSize = scaled(logicalSize);
        int tileSizeBased = Math.max(1, Math.round(logicalSize * tileSize / (float) GameConfig.TILE));
        int size = Math.max(zoomSize, tileSizeBased);
        if (!asset.startsWith("interior_")) {
            return outdoorPropRenderSize(asset, size, tileSize);
        }
        if (isInteriorTabletopAsset(asset)) {
            return Math.max(1, Math.round(tileSize * 0.58f));
        }
        if (isInteriorWallDecorAsset(asset)) {
            return Math.max(1, Math.round(tileSize * 0.86f));
        }
        if (asset.equals("interior_chair_north") || asset.equals("interior_chair_south")
                || asset.equals("interior_chair_east") || asset.equals("interior_chair_west")
                || asset.equals("interior_chair_north_alt") || asset.equals("interior_chair_south_alt")
                || asset.equals("interior_chair_east_alt") || asset.equals("interior_chair_west_alt")) {
            return Math.max(1, Math.round(tileSize * 0.84f));
        }
        if (asset.equals("interior_round_table")) {
            return Math.max(1, Math.round(tileSize * 1.02f));
        }
        if (asset.equals("interior_herb_pot") || asset.equals("interior_flower_pot")
                || asset.equals("interior_planting_pot") || asset.equals("interior_cookpot_stand")) {
            return Math.max(1, Math.round(tileSize * 0.90f));
        }
        if (asset.equals("interior_sprout_planter") || asset.equals("interior_herb_planter")) {
            return Math.max(1, Math.round(tileSize * 1.0f));
        }
        if (asset.equals("interior_bookshelf")) {
            return Math.max(1, Math.round(tileSize * 1.08f));
        }
        if (asset.equals("interior_side_table") || asset.equals("interior_anvil") || asset.equals("interior_forge")
                || asset.equals("interior_stove") || asset.equals("interior_oven")
                || asset.equals("interior_cooking_station") || asset.equals("interior_alchemy_station")
                || asset.equals("interior_aquarium_table")) {
            return Math.max(1, Math.round(tileSize * 1.05f));
        }
        if (asset.equals("interior_table_h_left") || asset.equals("interior_table_h_middle")
                || asset.equals("interior_table_h_right") || asset.equals("interior_bench_h")
                || asset.equals("interior_banquet_table_h") || asset.equals("interior_stool_table_h")
                || asset.equals("interior_study_desk_h") || asset.equals("interior_counter_corner_h")
                || asset.equals("interior_table_v_top") || asset.equals("interior_table_v_middle")
                || asset.equals("interior_table_v_bottom") || asset.equals("interior_bench_v")
                || asset.equals("interior_carpenter_workbench") || asset.equals("interior_long_table_benches")
                || asset.equals("interior_metal_crate") || asset.equals("interior_bakery_counter")
                || asset.equals("interior_tavern_counter") || asset.equals("interior_sawhorse_planks")
                || asset.equals("interior_storage_counter") || asset.equals("interior_low_cupboard")) {
            return Math.max(1, Math.round(tileSize * 1.02f));
        }
        if (asset.equals("interior_bakery_oven") || asset.equals("interior_carpenter_workbench")
                || asset.equals("interior_long_table_benches") || asset.equals("interior_metal_crate")
                || asset.equals("interior_traveler_trunk") || asset.equals("interior_bakery_counter")
                || asset.equals("interior_tavern_counter") || asset.equals("interior_sawhorse_planks")
                || asset.equals("interior_storage_counter") || asset.equals("interior_low_cupboard")
                || asset.equals("interior_resident_bed") || asset.equals("interior_herb_drying_rack_v")
                || asset.equals("interior_linen_shelf") || asset.equals("interior_anvil_tool_rack")
                || asset.equals("interior_grain_sacks_v") || asset.equals("interior_inn_screen_chest")
                || asset.equals("interior_table_h_left") || asset.equals("interior_table_h_middle")
                || asset.equals("interior_table_h_right") || asset.equals("interior_bench_h")
                || asset.equals("interior_banquet_table_h") || asset.equals("interior_stool_table_h")
                || asset.equals("interior_study_desk_h") || asset.equals("interior_counter_corner_h")
                || asset.equals("interior_table_v_top") || asset.equals("interior_table_v_middle")
                || asset.equals("interior_table_v_bottom") || asset.equals("interior_bench_v")) {
            return Math.max(1, Math.round(tileSize * 1.0f));
        }
        if (asset.equals("interior_floor_leafy_plant") || asset.equals("interior_floor_sapling_pot")
                || asset.equals("interior_floor_bushy_planter") || asset.equals("interior_floor_reed_pot")
                || asset.equals("interior_floor_flower_planter") || asset.equals("interior_vine_trellis")
                || asset.equals("interior_aquarium_table")) {
            return Math.max(1, Math.round(tileSize * 0.94f));
        }
        return size;
    }

    private int outdoorPropRenderSize(String asset, int size, int tileSize) {
        if (asset.startsWith("town_portal_")) {
            return Math.max(size, Math.round(tileSize * 2.55f));
        }
        if (isVillageFenceAsset(asset)) {
            return Math.max(size, Math.round(tileSize * 1.04f));
        }
        if (asset.startsWith("town_park_accent_")) {
            return Math.max(size, Math.round(tileSize * 1.72f));
        }
        if (!isReadableOutdoorProp(asset)) {
            return size;
        }
        int boosted = Math.round(size * 1.32f);
        int minimum = Math.round(tileSize * 0.94f);
        int maximum = Math.round(tileSize * 1.38f);
        return Math.max(size, Math.min(maximum, Math.max(boosted, minimum)));
    }

    private boolean isReadableOutdoorProp(String asset) {
        return asset.startsWith("village_prop_")
                || isVillageFenceAsset(asset)
                || asset.startsWith("city_prop_")
                || asset.startsWith("town_portal_")
                || asset.equals("city_lantern")
                || asset.startsWith("location_camp_")
                || asset.equals("player_village_quest_board")
                || asset.equals("deco_road_signpost")
                || asset.equals("deco_road_milestone")
                || asset.equals("deco_imagen_signpost")
                || asset.equals("deco_imagen_milestone")
                || asset.equals("deco_imagen_road_camp")
                || isDiscoverabilityOutdoorProp(asset);
    }

    private boolean isDiscoverabilityOutdoorProp(String asset) {
        return asset.equals("deco_imagen_shrine_stone")
                || asset.equals("deco_forest_shrine_stone")
                || asset.equals("deco_imagen_green_rune_stone")
                || asset.equals("deco_imagen_tundra_rune_stone")
                || asset.equals("deco_imagen_stone_stack")
                || asset.equals("deco_mountain_cairn")
                || asset.equals("deco_mountain_pass_way_cairn")
                || asset.equals("location_ruin_standing_stones")
                || asset.equals("deco_tree_elder_harvestable");
    }

    private boolean isInteriorWallDecorAsset(String asset) {
        return asset != null && asset.startsWith("interior_wall_");
    }

    private boolean isInteriorTabletopAsset(String asset) {
        return asset.equals("interior_tabletop_place_setting")
                || asset.equals("interior_tabletop_meal")
                || asset.equals("interior_tabletop_candle")
                || asset.equals("interior_flower_vase")
                || asset.equals("interior_seed_bowl")
                || asset.equals("interior_mortar_pestle");
    }

    private void drawVillageFenceProp(Graphics2D g, WorldProp prop, WorldRenderer.PropContext context) {
        int size = propRenderSize(prop.asset(), prop.size(), context.tileSize());
        int px = (prop.x() - context.camX()) * context.tileSize() + (context.tileSize() - size) / 2;
        int py = (prop.y() - context.camY()) * context.tileSize() + context.tileSize() - size;
        String asset = "village_fence_" + String.format("%02d", villageFenceBits(prop.x(), prop.y()));
        effects.drawShadow(g, px + size / 6, py + size - scaled(9), size * 2 / 3, scaled(7));
        drawPropImage(g, asset, px, py, size, size, 1.0f);
    }

    private int villageFenceBits(int wx, int wy) {
        int bits = 0;
        if (connectsVillageFence(wx, wy - 1)) {
            bits |= 1;
        }
        if (connectsVillageFence(wx, wy + 1)) {
            bits |= 2;
        }
        if (connectsVillageFence(wx - 1, wy)) {
            bits |= 4;
        }
        if (connectsVillageFence(wx + 1, wy)) {
            bits |= 8;
        }
        return bits;
    }

    private boolean connectsVillageFence(int x, int y) {
        for (WorldProp prop : state.world.propsAt(state.currentMapId, x, y)) {
            if (isVillageFenceAsset(prop.asset())) {
                return true;
            }
        }
        return false;
    }

    private boolean isVillageFenceAsset(String asset) {
        return "village_fence_auto".equals(asset) || (asset != null && asset.startsWith("village_fence_"));
    }

    private boolean isDisallowedSettlementOutdoorProp(String asset) {
        if (asset == null || !effects.isSettlementMapKind(state.currentMapId)) {
            return false;
        }
        String lower = asset.toLowerCase();
        return lower.contains("window")
                || lower.startsWith("interior_wall_")
                || lower.startsWith("city_building_")
                || lower.contains("wall_planter")
                || lower.contains("vine_trellis")
                || lower.contains("herb_planter_narrow")
                || lower.contains("ivy_wall_planter")
                || lower.contains("stone_arch")
                || lower.contains("arched")
                || lower.contains("graveyard_tombstone")
                || lower.contains("dungeon_grave")
                || lower.contains("crypt_sarcophagus");
    }

    private int interiorPropOffsetX(String asset, int tileSize) {
        if (asset.equals("interior_chair_east") || asset.equals("interior_chair_east_alt")) {
            return tileRelative(3, tileSize);
        }
        if (asset.equals("interior_chair_west") || asset.equals("interior_chair_west_alt")) {
            return -tileRelative(3, tileSize);
        }
        return 0;
    }

    private int interiorPropOffsetY(String asset, int tileSize) {
        if (isInteriorTabletopAsset(asset)) {
            return -tileRelative(12, tileSize);
        }
        if (isInteriorWallDecorAsset(asset)) {
            return -tileRelative(asset.contains("sconce") ? 7 : 4, tileSize);
        }
        if (asset.equals("interior_vine_trellis")) {
            return -tileRelative(8, tileSize);
        }
        if (asset.equals("interior_chair_north") || asset.equals("interior_chair_north_alt")) {
            return tileRelative(3, tileSize);
        }
        if (asset.equals("interior_chair_south") || asset.equals("interior_chair_south_alt")) {
            return -tileRelative(4, tileSize);
        }
        if (asset.equals("interior_side_table")) {
            return tileRelative(2, tileSize);
        }
        if (asset.equals("interior_bookshelf")) {
            return -tileRelative(14, tileSize);
        }
        if (asset.equals("interior_bakery_oven") || asset.equals("interior_traveler_trunk")
                || asset.equals("interior_resident_bed") || asset.equals("interior_herb_drying_rack_v")
                || asset.equals("interior_linen_shelf") || asset.equals("interior_anvil_tool_rack")
                || asset.equals("interior_grain_sacks_v") || asset.equals("interior_inn_screen_chest")
                || asset.equals("interior_table_v_top") || asset.equals("interior_table_v_middle")
                || asset.equals("interior_table_v_bottom") || asset.equals("interior_bench_v")) {
            return -tileRelative(8, tileSize);
        }
        return 0;
    }

    public int interiorPropHeight(String asset, int size) {
        if (asset.equals("interior_bookshelf")) {
            return Math.max(size, Math.round(size * 1.36f));
        }
        if (asset.equals("interior_bed_vertical") || asset.equals("interior_vine_trellis")
                || asset.equals("interior_bakery_oven") || asset.equals("interior_traveler_trunk")
                || asset.equals("interior_resident_bed") || asset.equals("interior_herb_drying_rack_v")
                || asset.equals("interior_linen_shelf") || asset.equals("interior_anvil_tool_rack")
                || asset.equals("interior_grain_sacks_v") || asset.equals("interior_inn_screen_chest")
                || asset.equals("interior_table_v_top") || asset.equals("interior_table_v_middle")
                || asset.equals("interior_table_v_bottom") || asset.equals("interior_bench_v")) {
            return size * 2;
        }
        return size;
    }

    private void drawAnimatedPropImage(Graphics2D g, WorldProp prop, String asset, int x, int y, int size, float opacity, int frame) {
        double sway = propWindSway(prop, asset, frame);
        if (Math.abs(sway) < 0.003) {
            drawPropImage(g, asset, x, y, size, size, opacity);
            return;
        }

        AffineTransform oldTransform = g.getTransform();
        Composite oldComposite = g.getComposite();
        if (opacity < 1.0f) {
            g.setComposite(AlphaComposite.SrcOver.derive(opacity));
        }
        double anchorX = x + size / 2.0;
        double anchorY = y + size;
        g.translate(anchorX, anchorY);
        g.shear(sway, 0.0);
        g.drawImage(assets.spriteFit(asset, size, size), (int) Math.round(x - anchorX), (int) Math.round(y - anchorY), null);
        g.setTransform(oldTransform);
        g.setComposite(oldComposite);
    }

    private double propWindSway(WorldProp prop, String asset, int frame) {
        if (!isWindReactiveProp(asset) || !isOutdoorPropAnimationMap()) {
            return 0.0;
        }
        double strength = state.windStrength();
        double sideWind = Math.cos(state.windRadians());
        if (Math.abs(sideWind) < 0.08) {
            sideWind = Math.copySign(0.08, sideWind == 0.0 ? 1.0 : sideWind);
        }
        int seed = prop.x() * 928371 + prop.y() * 364479 + asset.hashCode();
        double phase = frame * (0.055 + strength * 0.105) + seed * 0.013;
        double gust = Math.sin(phase) * 0.72 + Math.sin(phase * 1.73 + seed * 0.003) * 0.28;
        return clamp(propWindSwayAmplitude(asset) * strength * sideWind * gust, -0.18, 0.18);
    }

    private double propWindSwayAmplitude(String asset) {
        if (asset.contains("wheat") || asset.contains("grass") || asset.contains("reed")
                || asset.contains("flower") || asset.contains("fern") || asset.contains("bloom")
                || asset.contains("cattail") || asset.contains("plant")) {
            return 0.115;
        }
        if (asset.contains("lily") || asset.contains("duckweed") || asset.contains("floating") || asset.contains("weed")) {
            return 0.070;
        }
        if (asset.contains("bush") || asset.contains("scrub")) {
            return 0.075;
        }
        if (asset.contains("tree") || asset.contains("pine")) {
            return 0.060;
        }
        return 0.045;
    }

    private boolean isWindReactiveProp(String asset) {
        return asset.contains("wheat")
                || asset.contains("tree")
                || asset.contains("pine")
                || asset.contains("grass")
                || asset.contains("reed")
                || asset.contains("flower")
                || asset.contains("fern")
                || asset.contains("bloom")
                || asset.contains("bush")
                || asset.contains("scrub")
                || asset.contains("clover")
                || asset.contains("leaf")
                || asset.contains("plant")
                || asset.contains("cattail")
                || asset.contains("lily")
                || asset.contains("duckweed")
                || asset.contains("floating")
                || asset.contains("weed");
    }

    private boolean isOutdoorPropAnimationMap() {
        String kind = state.world.kind(state.currentMapId);
        return "overworld".equals(kind) || "city".equals(kind) || "village".equals(kind);
    }

    private void drawPropAmbientAnimation(Graphics2D g, WorldProp prop, int x, int y, int size, int frame) {
        String asset = prop.asset();
        if (isFireProp(asset)) {
            drawFirePropAnimation(g, prop, x, y, size, frame);
        }
        if (isMagicGlowProp(asset)) {
            drawMagicPropAnimation(g, prop, x, y, size, frame);
        }
        if (isResourceParticleProp(asset)) {
            drawResourcePropParticles(g, prop, x, y, size, frame);
        }
    }

    static boolean isFireProp(String asset) {
        return asset.contains("camp_fire") || asset.contains("campfire") || asset.contains("fire")
                || asset.contains("forge") || asset.contains("oven") || asset.contains("stove")
                || asset.contains("hearth") || asset.contains("cookpot") || asset.contains("cooking_station")
                || asset.contains("sconce") || asset.contains("tabletop_candle");
    }

    static boolean isMagicGlowProp(String asset) {
        return asset.startsWith("town_portal_")
                || asset.contains("crystal")
                || asset.contains("alchemy")
                || asset.contains("ice_crystals")
                || asset.contains("rune")
                || asset.contains("shrine")
                || asset.contains("fairy_pool")
                || asset.contains("bubble_pool")
                || asset.contains("firefly");
    }

    private boolean isResourceParticleProp(String asset) {
        return asset.contains("ore_")
                || asset.contains("_ore")
                || asset.contains("glowing_root")
                || asset.contains("glowroot")
                || asset.contains("magical_harvestable")
                || asset.contains("enchanted")
                || asset.contains("mithril")
                || asset.contains("cobalt")
                || asset.contains("adamantite")
                || asset.contains("gold")
                || asset.contains("silver");
    }

    private void drawFirePropAnimation(Graphics2D g, WorldProp prop, int x, int y, int size, int frame) {
        int seed = Math.abs(prop.x() * 928371 + prop.y() * 364479 + prop.asset().hashCode());
        double flicker = Math.sin(frame * 0.42 + seed * 0.01) * 0.5 + Math.sin(frame * 0.77 + seed * 0.03) * 0.5;
        int cx = x + size / 2 + scaled((int) Math.round(flicker));
        int baseY = y + Math.round(size * 0.68f);
        int flameH = Math.max(scaled(11), (int) Math.round(size * (0.34 + flicker * 0.035)));
        int flameW = Math.max(scaled(8), (int) Math.round(size * 0.24));

        effects.drawRadialGlow(g, cx, baseY - flameH / 3, Math.max(scaled(18), size / 2), new Color(255, 143, 44), 0.18f);

        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver.derive(0.82f));
        Polygon outer = new Polygon(
                new int[]{cx - flameW / 2, cx + scaled((int) Math.round(flicker * 2)), cx + flameW / 2},
                new int[]{baseY, baseY - flameH, baseY},
                3
        );
        g.setColor(new Color(255, 92, 34, 220));
        g.fillPolygon(outer);

        g.setComposite(AlphaComposite.SrcOver.derive(0.86f));
        Polygon inner = new Polygon(
                new int[]{cx - flameW / 4, cx - scaled((int) Math.round(flicker)), cx + flameW / 4},
                new int[]{baseY - scaled(1), baseY - flameH * 3 / 4, baseY - scaled(1)},
                3
        );
        g.setColor(new Color(255, 220, 98, 235));
        g.fillPolygon(inner);

        for (int i = 0; i < 5; i++) {
            double life = Math.floorMod(frame * 4 + seed + i * 23, 80) / 80.0;
            int sparkX = cx + scaled(Math.floorMod(seed / (i + 3) + i * 11, 13) - 6);
            int sparkY = baseY - (int) Math.round(life * size * 0.82);
            int sparkSize = Math.max(scaled(2), scaled(4) - (int) Math.round(life * scaled(2)));
            float alpha = (float) ((1.0 - life) * 0.46);
            g.setComposite(AlphaComposite.SrcOver.derive(alpha));
            g.setColor(new Color(255, 214, 112));
            g.fillOval(sparkX - sparkSize / 2, sparkY - sparkSize / 2, sparkSize, sparkSize);
        }
        g.setComposite(oldComposite);
    }

    private void drawMagicPropAnimation(Graphics2D g, WorldProp prop, int x, int y, int size, int frame) {
        Color glow = effects.propGlowColor(prop.asset());
        if (glow == null) {
            return;
        }
        int seed = Math.abs(prop.x() * 7349 + prop.y() * 9127 + prop.asset().hashCode());
        int cx = x + size / 2;
        int cy = y + size / 2;
        float visibility = effects.lightVisibilityForAsset(prop.asset());
        float pulse = (float) (0.72 + Math.sin(frame * 0.075 + seed * 0.01) * 0.16);
        effects.drawRadialGlow(g, cx, cy, Math.max(scaled(16), size * 2 / 3), glow, 0.09f * visibility * pulse);

        Composite oldComposite = g.getComposite();
        int motes = prop.asset().contains("firefly") ? 6 : 4;
        for (int i = 0; i < motes; i++) {
            double angle = frame * (0.032 + i * 0.004) + seed * 0.002 + i * 2.399;
            double rise = Math.sin(frame * 0.045 + i * 1.7 + seed * 0.004);
            int moteX = cx + (int) Math.round(Math.cos(angle) * size * 0.34);
            int moteY = cy + (int) Math.round(Math.sin(angle * 1.21) * size * 0.22 - rise * size * 0.10);
            int moteSize = Math.max(scaled(2), scaled(3 + Math.floorMod(seed + i, 3)));
            float alpha = (float) Math.min(0.58, (0.22 + effects.nightFactor() * 0.28) * visibility * (0.76 + Math.sin(angle * 1.8) * 0.18));
            g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0.0f, alpha)));
            g.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 225));
            g.fillOval(moteX - moteSize / 2, moteY - moteSize / 2, moteSize, moteSize);
        }
        g.setComposite(oldComposite);
    }

    private void drawResourcePropParticles(Graphics2D g, WorldProp prop, int x, int y, int size, int frame) {
        int seed = Math.abs(prop.x() * 2213 + prop.y() * 4567 + prop.asset().hashCode());
        Color color = resourceParticleColor(prop.asset());
        int count = propParticleCount(prop.asset());
        Composite oldComposite = g.getComposite();
        for (int i = 0; i < count; i++) {
            double life = Math.floorMod(frame * 2 + seed + i * 29, 96) / 96.0;
            double drift = Math.sin(frame * 0.055 + seed * 0.003 + i * 2.1);
            int px = x + size / 2 + scaled(Math.floorMod(seed / (i + 5) + i * 17, 19) - 9)
                    + (int) Math.round(drift * size * 0.08);
            int py = y + (int) Math.round(size * (0.68 - life * 0.48))
                    + scaled(Math.floorMod(seed / (i + 7), 7) - 3);
            int mote = Math.max(scaled(2), scaled(4) - (int) Math.round(life * scaled(2)));
            float alpha = (float) ((1.0 - life) * (0.18 + effects.nightFactor() * 0.20));
            g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0.0f, Math.min(0.42f, alpha))));
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 225));
            g.fillOval(px - mote / 2, py - mote / 2, mote, mote);
        }
        g.setComposite(oldComposite);
    }

    private int propParticleCount(String asset) {
        if (asset.contains("glowing_root") || asset.contains("glowroot") || asset.contains("magical_harvestable")
                || asset.contains("enchanted")) {
            return 5;
        }
        if (asset.contains("mithril") || asset.contains("cobalt") || asset.contains("adamantite")
                || asset.contains("gold") || asset.contains("silver")) {
            return 4;
        }
        return 3;
    }

    private Color resourceParticleColor(String asset) {
        if (asset.contains("gold")) {
            return new Color(255, 215, 106);
        }
        if (asset.contains("mithril") || asset.contains("silver")) {
            return new Color(186, 230, 255);
        }
        if (asset.contains("cobalt") || asset.contains("adamantite")) {
            return new Color(122, 190, 255);
        }
        if (asset.contains("glowroot") || asset.contains("glowing_root") || asset.contains("magical_harvestable")
                || asset.contains("enchanted")) {
            return new Color(139, 244, 168);
        }
        return new Color(244, 194, 122);
    }

    private void drawPropGroundBlend(Graphics2D g, int wx, int wy, int x, int y, int size, float opacity, boolean soft) {
        Color terrain = terrainColorAt(wx, wy);
        int pad = soft ? scaled(3) : scaled(1);
        int ovalW = Math.max(scaled(9), size - pad * 2);
        int ovalH = Math.max(scaled(4), soft ? size / 4 : size / 5);
        int ovalX = x + (size - ovalW) / 2;
        int ovalY = y + size - ovalH - scaled(3);

        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver.derive(opacity));
        g.setColor(terrain);
        g.fillOval(ovalX, ovalY, ovalW, ovalH);

        int seed = wx * 73471 + wy * 19349663 + size;
        int flecks = soft ? 4 : 3;
        for (int i = 0; i < flecks; i++) {
            int fx = ovalX + Math.floorMod(seed + i * 17, Math.max(1, ovalW));
            int fy = ovalY + Math.floorMod(seed / 7 + i * 11, Math.max(1, ovalH));
            int fw = Math.max(scaled(2), ovalW / (soft ? 5 : 6));
            int fh = Math.max(scaled(1), ovalH / 2);
            g.setColor(propBlendFleckColor(terrain, seed + i * 29));
            g.fillOval(fx - fw / 2, fy - fh / 2, fw, fh);
        }
        g.setComposite(oldComposite);
    }

    private void drawPropGroundVeil(Graphics2D g, int wx, int wy, int x, int y, int size, float opacity) {
        Color terrain = terrainColorAt(wx, wy);
        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver.derive(opacity));
        g.setColor(terrain);
        int veilH = Math.max(scaled(3), size / 5);
        g.fillOval(x + scaled(2), y + size - veilH - scaled(2), size - scaled(4), veilH);
        g.setComposite(oldComposite);
    }

    private Color propBlendFleckColor(Color terrain, int seed) {
        int shift = Math.floorMod(seed, 2) == 0 ? 18 : -16;
        return new Color(
                clampColor(terrain.getRed() + shift),
                clampColor(terrain.getGreen() + shift),
                clampColor(terrain.getBlue() + shift)
        );
    }

    private int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private Color terrainColorAt(int wx, int wy) {
        char tile = state.world.tileAt(state.currentMapId, wx, wy);
        return Terrain.color(effects.visibleTerrainTile(tile, wx, wy));
    }

    private boolean castsPropShadow(String asset) {
        return !asset.equals("location_farmland_tilled")
                && !asset.equals("location_farmland_wheat")
                && !asset.equals("location_graveyard_dirt")
                && !asset.equals("location_graveyard_path")
                && !asset.equals("location_dungeon_approach_path")
                && !asset.startsWith("town_park_accent_")
                && !asset.startsWith("interior_")
                && !isSoftGroundProp(asset)
                && !isFeatheryGroundProp(asset);
    }

    private boolean isSoftGroundProp(String asset) {
        return asset.startsWith("deco_soft_");
    }

    private boolean isNaturalLowProp(String asset) {
        return asset.startsWith("deco_")
                && !asset.contains("tree")
                && !asset.contains("pine")
                && !asset.contains("root")
                && !asset.contains("log")
                && !asset.contains("stump")
                && !asset.contains("totem")
                && !asset.contains("shrine")
                && !asset.contains("rune")
                && !asset.contains("camp")
                && !asset.contains("signpost")
                && !asset.contains("milestone")
                && !asset.contains("jar")
                && (isFeatheryGroundProp(asset)
                || asset.contains("stone")
                || asset.contains("rock")
                || asset.contains("pebble")
                || asset.contains("moss")
                || asset.contains("cairn")
                || asset.contains("pond")
                || asset.contains("pool")
                || asset.contains("lily")
                || asset.contains("oasis"));
    }

    private boolean isFeatheryGroundProp(String asset) {
        return isSoftGroundProp(asset)
                || asset.contains("grass")
                || asset.contains("flower")
                || asset.contains("bloom")
                || asset.contains("fern")
                || asset.contains("bush")
                || asset.contains("mushroom")
                || asset.contains("reed")
                || asset.contains("cattail")
                || asset.contains("plant")
                || asset.contains("lily")
                || asset.contains("duckweed")
                || asset.contains("floating")
                || asset.contains("weed")
                || asset.contains("leaf")
                || asset.contains("scrub");
    }

    private boolean isTreeGatherAsset(String asset) {
        String lower = asset == null ? "" : asset.toLowerCase();
        return lower.contains("tree")
                || lower.contains("pine")
                || lower.contains("log")
                || lower.contains("stump")
                || lower.contains("woodpile");
    }

    private int scaled(int value) {
        return Math.max(1, value * state.zoom / 100);
    }

    private int tileRelative(int value, int tileSize) {
        return Math.max(1, Math.round(value * tileSize / (float) GameConfig.TILE));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public interface Effects {
        public void drawShadow(Graphics2D g, int x, int y, int w, int h);

        public void drawCasterShadow(Graphics2D g, BufferedImage image, String cacheKey,
                              int x, int y, int width, int height, boolean flipHorizontal, float baseAlpha);

        public void drawRadialGlow(Graphics2D g, int cx, int cy, int radius, Color color, float alpha);

        char visibleTerrainTile(char tile, int wx, int wy);

        public Color propGlowColor(String asset);

        public float lightVisibilityForAsset(String asset);

        public float nightFactor();

        boolean isSettlementMapKind(String mapId);
    }
}
