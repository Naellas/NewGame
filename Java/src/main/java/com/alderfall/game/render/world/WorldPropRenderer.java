package com.alderfall.game.render.world;

import com.alderfall.game.*;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public final class WorldPropRenderer {
    private final AssetStore assets;
    private final ModularFences.Sprites fenceSprites = new ModularFences.Sprites();
    private final ShrineEventVisuals.Sprites shrineSprites = new ShrineEventVisuals.Sprites();
    private final ConnectedFurniture.Sprites furnitureSprites = new ConnectedFurniture.Sprites();
    private final ConnectedBoundary.Sprites boundarySprites = new ConnectedBoundary.Sprites();
    private final GameState state;
    private final Effects effects;
    private final java.util.Map<BufferedImage, java.awt.Rectangle> surfaceInkBounds = new java.util.WeakHashMap<>();

    public WorldPropRenderer(AssetStore assets, GameState state, Effects effects) {
        this.assets = assets;
        this.state = state;
        this.effects = effects;
    }

    public void drawWorldProp(Graphics2D g, WorldProp prop, WorldRenderer.PropContext context) {
        drawWorldProp(g, prepare(prop, context), context);
    }

    public PropRenderData prepare(WorldProp prop, WorldRenderer.PropContext context) {
        String asset = prop.asset();
        PropPlacement.Placement placement = PropPlacement.at(state.world, state.currentMapId, prop);
        int size = Math.max(1, (int) Math.round(propRenderSize(asset, prop.size(), context.tileSize())
                * placement.scale()));
        int drawW = interiorPropWidth(asset, size);
        int drawH = interiorPropHeight(asset, size);
        java.awt.Rectangle bounds;
        double depthY;
        double depthX;
        int depthLayer;
        if (ModularMarket.supports(asset)) {
            int tile = context.tileSize();
            drawW = tile * 2;
            drawH = Math.round(tile * 1.5f);
            bounds = new java.awt.Rectangle((prop.x() - context.camX()) * tile,
                    (prop.y() - context.camY() + 1) * tile - drawH, drawW, drawH);
            depthY = prop.y() + 1;
            depthX = prop.x();
            depthLayer = 1;
        } else if (ConnectedBoundary.supports(asset)) {
            int tile = context.tileSize();
            drawW = tile; drawH = tile * 2;
            bounds = new java.awt.Rectangle((prop.x() - context.camX()) * tile,
                    (prop.y() - context.camY() - 1) * tile, drawW, drawH);
            depthY = prop.y() + .75;
            depthX = prop.x();
            depthLayer = 1;
        } else if (TownGardenArt.gate(asset) || TownGardenArt.fountain(asset)) {
            int tile = context.tileSize();
            boolean gate = TownGardenArt.gate(asset);
            drawW = tile * 3; drawH = Math.round(tile * (gate ? 2.4f : 2.8f));
            bounds = new java.awt.Rectangle(Math.round((prop.x() - context.camX() - (gate ? 1f : .5f)) * tile),
                    (prop.y() - context.camY() + (gate ? 1 : 2)) * tile - drawH, drawW, drawH);
            depthY = prop.y() + (gate ? 1 : 2);
            depthX = prop.x();
            depthLayer = 1;
        } else if (ConnectedFurniture.outdoor(asset)) {
            int tile = context.tileSize();
            boolean gate = asset.equals("town_fence_gate");
            boolean vertical = asset.endsWith("_v");
            drawW = gate ? tile * 2 : vertical ? Math.round(tile * .55f) : tile;
            drawH = Math.round(tile * (gate ? 1.35f : vertical ? 1.45f
                    : asset.contains("stall") ? 1.6f : asset.contains("bench") ? .65f : .85f));
            bounds = new java.awt.Rectangle((prop.x() - context.camX()) * tile + (tile - drawW) / 2,
                    Math.round((prop.y() - context.camY() + (vertical ? 1.5f : 1f)) * tile) - drawH, drawW, drawH);
            depthY = prop.y() + 1;
            depthX = prop.x();
            depthLayer = 1;
        } else if (asset.startsWith("interior_")) {
            int[] footprint = state.world.interiorVisualFootprint(asset);
            bounds = interiorBounds(prop, asset, context.tileSize(), context.camX(), context.camY(),
                    drawW, drawH, footprint);
            depthY = prop.y() + footprint[1] + (isInteriorTabletopAsset(asset) ? 0.1
                    : isInteriorWallDecorAsset(asset) ? 0.02 : 0.0);
            depthX = prop.x();
            depthLayer = 0;
        } else {
            bounds = propBounds(prop, context.tileSize(), context.camX(), context.camY(),
                    placement, drawW, drawH);
            depthY = placement.footY(prop);
            depthX = prop.x() + placement.x();
            depthLayer = placement.kind() == PropPlacement.Kind.COVER
                    || asset.equals("location_overgrown_landing") ? 0 : 1;
        }
        bounds.translate((int)Math.round(prop.offsetX()*context.tileSize()/48.0),
                (int)Math.round(prop.offsetY()*context.tileSize()/48.0));
        depthX+=prop.offsetX()/48.0;
        // Nudging a mounted picture changes its height, not which side of the wall it occupies.
        if (!isInteriorWallDecorAsset(asset)) depthY+=prop.offsetY()/48.0;
        return new PropRenderData(prop, asset, placement, size, bounds.width, bounds.height, bounds, depthLayer, depthY, depthX);
    }

    public void drawWorldProp(Graphics2D g, PropRenderData render, WorldRenderer.PropContext context) {
        WorldProp prop = render.prop();
        String asset = render.asset();
        if (isDisallowedSettlementOutdoorProp(asset)) {
            return;
        }
        if (ModularMarket.supports(asset)) {
            g.drawImage(placedImage(prop, render.width(), render.height()), render.bounds().x, render.bounds().y, null);
            return;
        }
        if (isVillageFenceAsset(asset)) {
            Graphics2D shifted=(Graphics2D)g.create();
            shifted.translate(prop.offsetX()*context.tileSize()/48.0,prop.offsetY()*context.tileSize()/48.0);
            drawVillageFenceProp(shifted, prop, context);shifted.dispose();
            return;
        }
        int size = render.size();
        int drawW = render.width();
        int drawH = render.height();
        java.awt.Rectangle placed = render.bounds();
        int px = placed.x;
        int py = placed.y;
        if (prop.visualSlot() >= 0) {
            // Tiny ground details reuse cached sprites and do not need per-prop shadows, veils or animation.
            drawPropImage(g, asset, px, py, drawW, drawH, 1.0f);
            return;
        }
        if (isSoftGroundProp(asset)) {
            if (!PropPlacement.usesOffsets(state.world, state.currentMapId)) {
                int seed = prop.x() * 928371 + prop.y() * 364479 + asset.hashCode();
                px += scaled(Math.floorMod(seed, 7) - 3);
                py += scaled(Math.floorMod(seed / 13, 5) - 2);
            }
            drawPropGroundBlend(g, prop.x(), prop.y(), px, py, size, 0.32f, true);
            drawAnimatedPropImage(g, prop, asset, px, py, size, 0.90f, context);
            drawPropGroundVeil(g, prop.x(), prop.y(), px, py, size, 0.16f);
            drawPropAmbientAnimation(g, prop, px, py, size, context.frame());
            return;
        }

        boolean naturalBlend = isNaturalLowProp(asset);
        if (naturalBlend) {
            drawPropGroundBlend(g, prop.x(), prop.y(), px, py, size, 0.20f, false);
        }
        int joins = ConnectedBoundary.supports(asset)
                ? ConnectedBoundary.connections(state.world, state.currentMapId, prop) : furnitureConnections(prop);
        if (castsPropShadow(asset)) {
            BufferedImage image = placedImage(prop, drawW, drawH);
            effects.drawCasterShadow(g, image, "prop:" + asset + ":join=" + joins + ":" + drawW + "x" + drawH,
                    px, py, drawW, drawH, false, 0.36f);
            // Boundary canvases include transparent space below their ground node.
            // A generic canvas-bottom ellipse makes continuous hedges look suspended.
            if (!ConnectedBoundary.supports(asset))
                effects.drawShadow(g, px + drawW / 6, py + drawH - scaled(8), drawW * 2 / 3, scaled(8));
        }
        if (asset.startsWith("interior_")) {
            g.drawImage(placedImage(prop, drawW, drawH), px, py, null);
            String detail = FurnitureQuestContent.decoration(state, prop);
            if (!detail.isEmpty()) {
                int detailSize = Math.max(8, context.tileSize() / 2);
                g.drawImage(assets.spriteFit(detail, detailSize, detailSize),
                        px + (drawW - detailSize) / 2, py + drawH / 3 - detailSize / 2, null);
            }
            drawInteriorEmbers(g, prop, px, py, drawW, drawH, context.frame());
            return;
        }
        if (ConnectedFurniture.outdoor(asset) || TownGardenArt.fountain(asset) || state.roamingEvents.shrineVariant(state.currentMapId, prop) >= 0)
            g.drawImage(placedImage(prop, drawW, drawH), px, py, null);
        else drawAnimatedPropImage(g, prop, asset, px, py, size, 1.0f, context);
        if (naturalBlend) {
            drawPropGroundVeil(g, prop.x(), prop.y(), px, py, size, 0.08f);
        }
        drawPropAmbientAnimation(g, prop, px, py, size, context.frame());
    }

    public java.awt.Rectangle interiorBounds(WorldProp prop, int tileSize, int camX, int camY) {
        String asset = prop.asset();
        int size = propRenderSize(asset, prop.size(), tileSize);
        int width = interiorPropWidth(asset, size), height = interiorPropHeight(asset, size);
        int[] footprint = state.world.interiorVisualFootprint(asset);
        return interiorBounds(prop, asset, tileSize, camX, camY, width, height, footprint);
    }

    private java.awt.Rectangle interiorBounds(WorldProp prop, String asset, int tileSize, int camX, int camY,
                                               int width, int height, int[] footprint) {
        if (isInteriorWallDecorAsset(asset)) {
            var face = ConnectedInteriorWalls.bounds(state.world, state.currentMapId, prop.x(), prop.y(),
                    (prop.x() - camX) * tileSize, (prop.y() - camY) * tileSize, tileSize);
            int available = Math.max(1, Math.round(face.height * .78f));
            if (height > available) { width = Math.max(1, width * available / height); height = available; }
            // Center on plaster above the dado; the saved anchor stays on the wall cell.
            int centerY = face.y + Math.round(face.height * .43f);
            return new java.awt.Rectangle((prop.x() - camX) * tileSize + (footprint[0] * tileSize - width) / 2,
                    centerY - height / 2, width, height);
        }
        if (isInteriorTabletopAsset(asset)) {
            WorldProp surface = state.world.interiorSurfaceAt(state.currentMapId, prop.x(), prop.y());
            if (surface != null) {
                java.awt.Rectangle support = interiorBounds(surface, tileSize, camX, camY);
                BufferedImage sprite = placedImage(surface, support.width, support.height);
                java.awt.Rectangle ink = surfaceInkBounds.computeIfAbsent(sprite, WorldPropRenderer::visibleBounds);
                int[] supportFootprint = state.world.interiorVisualFootprint(surface.asset());
                float slot = (prop.x() - surface.x() + .5f) / supportFootprint[0];
                int centerX = support.x + ink.x + Math.round(ink.width * slot);
                int baseY = support.y + ink.y + Math.round(ink.height * .30f);
                return new java.awt.Rectangle(centerX - width / 2, baseY - Math.round(height * .75f), width, height);
            }
        }
        if (furnitureConnections(prop) != 0) {
            return new java.awt.Rectangle((prop.x() - camX) * tileSize,
                    (prop.y() - camY + footprint[1]) * tileSize - height, footprint[0] * tileSize, height);
        }
        return new java.awt.Rectangle((prop.x() - camX) * tileSize + (footprint[0] * tileSize - width) / 2
                + interiorPropOffsetX(asset, tileSize),
                (prop.y() - camY + footprint[1]) * tileSize - height + interiorPropOffsetY(asset, tileSize), width, height);
    }

    private static java.awt.Rectangle visibleBounds(BufferedImage image) {
        int left = image.getWidth(), top = image.getHeight(), right = -1, bottom = -1;
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
            if ((image.getRGB(x, y) >>> 24) < 16) continue;
            left = Math.min(left, x); top = Math.min(top, y);
            right = Math.max(right, x); bottom = Math.max(bottom, y);
        }
        return right < left ? new java.awt.Rectangle(0, 0, image.getWidth(), image.getHeight())
                : new java.awt.Rectangle(left, top, right - left + 1, bottom - top + 1);
    }

    public int placedSize(WorldProp prop, int tileSize) {
        return Math.max(1, (int) Math.round(propRenderSize(prop.asset(), prop.size(), tileSize)
                * PropPlacement.at(state.world, state.currentMapId, prop).scale()));
    }

    public java.awt.Rectangle propBounds(WorldProp prop, int tileSize, int camX, int camY) {
        if (ModularMarket.supports(prop.asset())) return new java.awt.Rectangle((prop.x()-camX)*tileSize,
                (prop.y()-camY+1)*tileSize-Math.round(tileSize*1.5f), tileSize*2, Math.round(tileSize*1.5f));
        if (prop.asset().startsWith("interior_")) return interiorBounds(prop, tileSize, camX, camY);
        PropPlacement.Placement placement = PropPlacement.at(state.world, state.currentMapId, prop);
        int size = Math.max(1, (int) Math.round(propRenderSize(prop.asset(), prop.size(), tileSize) * placement.scale()));
        int width = interiorPropWidth(prop.asset(), size), height = interiorPropHeight(prop.asset(), size);
        return propBounds(prop, tileSize, camX, camY, placement, width, height);
    }

    private java.awt.Rectangle propBounds(WorldProp prop, int tileSize, int camX, int camY,
                                           PropPlacement.Placement placement, int width, int height) {
        return new java.awt.Rectangle((prop.x() - camX) * tileSize + (int) Math.round(placement.x() * tileSize - width / 2.0),
                (prop.y() - camY) * tileSize + (int) Math.round(placement.y() * tileSize) - height, width, height);
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
        int size = placedSize(prop, context.tileSize());
        int drawW = interiorPropWidth(asset, size);
        int drawH = interiorPropHeight(asset, size);
        java.awt.Rectangle bounds = propBounds(prop, context.tileSize(), context.camX(), context.camY());
        int baseX = bounds.x + bounds.width / 2;
        int baseY = bounds.y + bounds.height;
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

    private int furnitureConnections(WorldProp prop) {
        if (!ConnectedFurniture.supports(prop.asset())) return 0;
        int width = ConnectedFurniture.connectionWidth(prop.asset());
        var neighbors = new java.util.ArrayList<WorldProp>();
        neighbors.addAll(state.world.propsAt(state.currentMapId, prop.x() - width, prop.y()));
        neighbors.addAll(state.world.propsAt(state.currentMapId, prop.x() + width, prop.y()));
        return ConnectedFurniture.connections(prop, neighbors);
    }

    private BufferedImage placedImage(WorldProp prop, int width, int height) {
        if (ConnectedBoundary.supports(prop.asset())) return boundarySprites.image(assets, prop.asset(), width,
                ConnectedBoundary.connections(state.world, state.currentMapId, prop));
        int mask = furnitureConnections(prop);
        BufferedImage image = mask == 0 ? propImage(prop.asset(), width, height)
                : furnitureSprites.image(assets, prop.asset(), width, height, mask);
        return shrineSprites.tint(image, state.roamingEvents.shrineVariant(state.currentMapId, prop));
    }

    public BufferedImage propImage(String asset, int width, int height) {
        return asset.startsWith("interior_wall_") && InteriorFurnishings.find(asset) == null
                ? assets.image(asset, width, height)
                : assets.placementSpriteFit(asset, width, height);
    }

    public int interiorPropWidth(String asset, int size) {
        InteriorFurnishings.Furnishing furnishing = InteriorFurnishings.find(asset);
        if (furnishing != null) return Math.max(1, Math.round(size * (furnishing.placement()
                == InteriorFurnishings.Placement.TABLETOP ? .6f : furnishing.width())));
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
        if (SettlementSpriteScale.matches(asset))
            return Math.max(1, Math.round(SettlementSpriteScale.size(asset) * tileSize / (float) GameConfig.TILE));
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
        boolean settlement = "city".equals(state.world.kind(state.currentMapId)) || "village".equals(state.world.kind(state.currentMapId));
        if (settlement && !state.currentMapId.equals(com.alderfall.game.map.WorldMap.PLAYER_VILLAGE_ID)) {
            if (asset.startsWith("city_prop_market_")) return Math.min(size, Math.round(tileSize * 1.05f));
            if ((asset.startsWith("city_prop_") || asset.startsWith("village_prop_"))
                    && (asset.contains("barrel") || asset.contains("crate") || asset.contains("basket")
                    || asset.contains("flower_pot") || asset.contains("seedling") || asset.contains("grain_sack")))
                return Math.min(size, Math.round(tileSize * .72f));
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
        if (InteriorFurnishings.is(asset, InteriorFurnishings.Placement.TABLETOP)) return true;
        return asset.equals("interior_tabletop_place_setting")
                || asset.equals("interior_tabletop_meal")
                || asset.equals("interior_tabletop_candle")
                || asset.equals("interior_flower_vase")
                || asset.equals("interior_seed_bowl")
                || asset.equals("interior_mortar_pestle");
    }

    private void drawVillageFenceProp(Graphics2D g, WorldProp prop, WorldRenderer.PropContext context) {
        int size = context.tileSize();
        int x = (prop.x() - context.camX()) * size, y = (prop.y() - context.camY()) * size;
        g.drawImage(fenceSprites.image(assets, prop.asset(), size,
                ModularFences.connections(state.world, state.currentMapId, prop)), x, y, null);
    }

    private boolean isVillageFenceAsset(String asset) {
        return asset != null && !ModularFences.family(asset).isEmpty();
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
            return 0;
        }
        if (asset.equals("interior_bakery_oven") || asset.equals("interior_traveler_trunk")
                || asset.equals("interior_resident_bed") || asset.equals("interior_herb_drying_rack_v")
                || asset.equals("interior_linen_shelf") || asset.equals("interior_anvil_tool_rack")
                || asset.equals("interior_grain_sacks_v") || asset.equals("interior_inn_screen_chest")
                || asset.equals("interior_table_v_top") || asset.equals("interior_table_v_middle")
                || asset.equals("interior_table_v_bottom") || asset.equals("interior_bench_v")) {
            return 0;
        }
        return 0;
    }

    public int interiorPropHeight(String asset, int size) {
        InteriorFurnishings.Furnishing furnishing = InteriorFurnishings.find(asset);
        if (furnishing != null) return Math.max(1, Math.round(size * furnishing.drawHeight()));
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

    private void drawAnimatedPropImage(Graphics2D g, WorldProp prop, String asset, int x, int y, int size,
                                       float opacity, WorldRenderer.PropContext context) {
        double sway = propWindSway(prop, asset, context.frame(), context.windStrength(), context.windRadians());
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

    private double propWindSway(WorldProp prop, String asset, int frame, double strength, double windRadians) {
        if (!isWindReactiveProp(asset) || !isOutdoorPropAnimationMap()) {
            return 0.0;
        }
        double sideWind = Math.cos(windRadians);
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
        return asset.contains("camp_fire") || asset.contains("campfire")
                || asset.contains("forge") || asset.contains("oven") || asset.contains("stove")
                || asset.contains("hearth") || asset.equals("interior_fireplace")
                || asset.contains("sconce") || asset.contains("tabletop_candle");
    }

    private void drawInteriorEmbers(Graphics2D g, WorldProp prop, int x, int y, int width, int height, int frame) {
        if (!isFireProp(prop.asset())) return;
        boolean enclosed = prop.asset().contains("sconce") || prop.asset().contains("oven")
                || prop.asset().equals("interior_fireplace");
        double phase = frame * 0.19 + prop.x() * 2.7 + prop.y();
        int cx = x + width / 2;
        int cy = y + Math.round(height * (prop.asset().contains("candle") ? 0.24f : 0.62f));
        // The painted fire is already in the sprite. Animate its light and a few
        // tiny embers, never paste a second geometric flame over the furniture.
        int seed = prop.asset().hashCode() ^ prop.x() * 928371 ^ prop.y() * 364479;
        effects.drawRadialGlow(g, cx, cy, Math.max(4, Math.min(width, height) / 3),
                new Color(255, 171, 64), stableLightState(frame, seed, 0.10f, 0.008f, 14));
        if (enclosed || prop.asset().contains("candle")) return;
        Graphics2D ember = (Graphics2D) g.create();
        int pixel = Math.max(1, width / 32);
        for (int i = 0; i < 3; i++) {
            double life = Math.floorMod(frame + prop.x() * 13 + i * 19, 61) / 61.0;
            ember.setColor(new Color(255, 197, 96, (int) ((1 - life) * 150)));
            int ex = cx + (int) (Math.sin(phase * 0.4 + i) * width * 0.07);
            int ey = cy - (int) (life * height * 0.25);
            ember.fillRect(ex, ey, pixel, pixel);
        }
        ember.dispose();
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
        drawInteriorEmbers(g, prop, x, y, size, size, frame);
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
        float pulse = stableLightState(frame, seed, 0.72f, 0.03f, 32);
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

    private float stableLightState(int frame, int seed, float base, float variation, int holdFrames) {
        int step = Math.floorDiv(frame, holdFrames);
        int mixed = seed ^ step * 0x9e3779b9;
        mixed ^= mixed >>> 16;
        mixed *= 0x7feb352d;
        mixed ^= mixed >>> 15;
        int level = Math.floorMod(mixed, 5) - 2;
        return base + variation * level * 0.5f;
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
        if (asset.contains("froststeel") || asset.contains("rock_salt")) return new Color(199, 230, 244);
        if (asset.contains("verdant")) return new Color(113, 196, 139);
        if (asset.contains("emberite")) return new Color(237, 132, 75);
        if (asset.contains("obsidian")) return new Color(154, 136, 177);
        if (asset.contains("bog_iron")) return new Color(171, 130, 88);
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
        if (asset.startsWith("interior_")) {
            return !isInteriorWallDecorAsset(asset) && !isInteriorTabletopAsset(asset)
                    && !asset.startsWith("interior_rug_");
        }
        return !asset.equals("location_farmland_tilled")
                && !asset.equals("location_farmland_wheat")
                && !asset.equals("location_graveyard_dirt")
                && !asset.equals("location_graveyard_path")
                && !asset.equals("location_dungeon_approach_path")
                && !asset.startsWith("town_park_accent_")
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

    public record PropRenderData(
            WorldProp prop,
            String asset,
            PropPlacement.Placement placement,
            int size,
            int width,
            int height,
            java.awt.Rectangle bounds,
            int depthLayer,
            double depthY,
            double depthX
    ) {
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
