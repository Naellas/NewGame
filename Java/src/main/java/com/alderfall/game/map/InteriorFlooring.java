package com.alderfall.game.map;

import com.alderfall.game.TilePoint;
import com.alderfall.game.WorldProp;
import java.util.Set;

/** Functional floor finishes. Furniture and rugs retain their own placement layers. */
public final class InteriorFlooring {
    private InteriorFlooring() { }
    private static final Set<String> STONE_ROOMS = Set.of("forge_and_quench", "finishing_bench",
            "oven_and_flour", "kitchen", "cooking", "distillation", "bottling_and_wash",
            "covered_water", "channel_maintenance", "warming_bay");
    private static final Set<String> HEAT = Set.of("interior_anvil", "interior_anvil_tool_rack",
            "interior_forge", "interior_smith_hearth", "interior_stove", "interior_oven",
            "interior_bakery_oven", "interior_fireplace", "interior_hearth_pot");

    public static boolean affectsFloor(String asset) { return HEAT.contains(asset); }

    public static void apply(MapArea area, InteriorLayout layout) {
        area.interiorFloorMaterials.clear();
        for (var zone : layout.zones()) {
            String purpose = zone.purpose();
            String material = STONE_ROOMS.contains(purpose) ? "stone"
                    : purpose.contains("sleep") || purpose.contains("bunk") || purpose.contains("bedroom") || purpose.contains("guest_room")
                    || purpose.equals("keeper_room") || purpose.contains("dressing") ? "wood" : null;
            if (material == null) continue;
            for (int y = zone.y(); y < zone.y() + zone.height(); y++)
                for (int x = zone.x(); x < zone.x() + zone.width(); x++)
                    if (floor(area.tileAt(x, y))) area.interiorFloorMaterials.put(new TilePoint(x, y), material);
        }
    }

    public static String material(WorldMap world, String map, int x, int y, String fallback) {
        MapArea area = world.area(map);
        if (area == null || !floor(area.tileAt(x, y))) return fallback;
        // A small hearth pad also covers newly placed equipment and saved custom interiors.
        for (WorldProp prop : area.props) {
            if (!HEAT.contains(prop.asset())) continue;
            int[] size = WorldMap.interiorVisualFootprint(prop.asset());
            int nearX = Math.max(prop.x(), Math.min(x, prop.x() + size[0] - 1));
            int nearY = Math.max(prop.y(), Math.min(y, prop.y() + size[1] - 1));
            if (Math.abs(x - nearX) <= 1 && Math.abs(y - nearY) <= 1
                    && floor(area.tileAt(nearX, y)) && floor(area.tileAt(x, nearY))) return "stone";
        }
        String finish = area.interiorFloorMaterials.get(new TilePoint(x, y));
        if ("wood".equals(finish)) return fallback.equals("rustic") ? "rustic" : "timber";
        return finish == null ? fallback : finish;
    }

    private static boolean floor(char tile) { return tile == 'i' || tile == 'z' || tile == 'k' || tile == 'e'; }
}
