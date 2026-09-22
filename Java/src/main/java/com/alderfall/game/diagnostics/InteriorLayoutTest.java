package com.alderfall.game;

import com.alderfall.game.map.InteriorLayout;
import com.alderfall.game.map.MapArea;
import com.alderfall.game.map.WorldMap;
import java.lang.reflect.Method;
import java.util.List;

/** Evaluate the complete composition, including silent placement rejection and navigation repair. */
public final class InteriorLayoutTest {
    public static void main(String[] args) throws Exception {
        WorldMap world = new WorldMap(42);
        Method place = WorldMap.class.getDeclaredMethod("addFurniture", MapArea.class, int.class, int.class, String.class);
        Method repair = WorldMap.class.getDeclaredMethod("ensureInteriorNavigable", MapArea.class);
        place.setAccessible(true); repair.setAccessible(true);
        int checked = 0;
        for (String theme : List.of("home", "inn", "tavern", "bakery", "shop", "study", "blacksmith", "carpenter")) {
            for (InteriorStyle style : InteriorStyle.values()) {
                for (int seed = 0; seed < 6; seed++) {
                    InteriorLayout plan = InteriorLayout.compose(theme, seed, style);
                    String id = world.createEditorMap("editor_composition", theme, "interior",
                            plan.tiles()[0].length, plan.tiles().length);
                    MapArea area = world.area(id);
                    for (int y = 0; y < area.height(); y++) {
                        area.tiles[y] = plan.tiles()[y].clone();
                        for (int x = 0; x < area.width(); x++) {
                            if (area.tiles[y][x] == 'z') area.interiorRugs.add(new TilePoint(x, y));
                        }
                    }
                    String context = theme + "/" + style + "/" + seed;
                    for (WorldProp prop : plan.props()) {
                        place.invoke(world, area, prop.x(), prop.y(), prop.asset());
                        require(area.props.contains(prop), "Rejected planned prop " + context + " " + prop);
                        if (!prop.asset().startsWith("interior_wall_")) {
                            int[] size = world.interiorVisualFootprint(prop.asset());
                            for (int y = prop.y(); y < prop.y() + size[1]; y++) {
                                for (int x = prop.x(); x < prop.x() + size[0]; x++) {
                                    require(!plan.circulation().contains(new TilePoint(x, y)), "Prop on main aisle " + context + " " + prop);
                                }
                            }
                            require(plan.zones().stream().anyMatch(z -> z.contains(prop.x(), prop.y())),
                                    "Prop has no functional zone " + context + " " + prop);
                        }
                        if (prop.asset().contains("bed")) {
                            require(plan.zones().stream().anyMatch(z -> (z.purpose().contains("room") || z.purpose().equals("sleeping"))
                                    && z.contains(prop.x(), prop.y())), "Bed outside private room");
                        }
                    }
                    int before = area.props.size();
                    repair.invoke(world, area);
                    require(area.props.size() == before, "Navigation repair dismantled a furniture group " + context);
                    for (TilePoint rug : area.interiorRugs) {
                        require(world.interiorRugAt(id, rug.x(), rug.y()), "Furniture punched a hole in a rug " + context);
                    }
                    for (TilePoint position : plan.residents()) {
                        require(world.isPassable(id, position.x(), position.y()), "Resident standing in furniture " + context + " " + position);
                    }
                    for (WorldProp prop : area.props) {
                        if (prop.asset().contains("bench_h")) {
                            require(area.props.stream().anyMatch(table -> table.x() == prop.x()
                                    && Math.abs(table.y() - prop.y()) == 1 && table.asset().equals("interior_banquet_table_h"))
                                    || plan.zones().stream().anyMatch(z -> (z.purpose().equals("waiting") || z.purpose().equals("welcome"))
                                    && z.contains(prop.x(), prop.y())), "Unpaired dining bench " + context);
                        }
                    }
                    checked++;
                }
            }
        }
        System.out.println("Composed layout checks passed: " + checked + " plans, 8 uses, 6 styles, 6 seeds.");
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
