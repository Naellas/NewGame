package com.alderfall.game;

import com.alderfall.game.map.InteriorLayout;
import com.alderfall.game.map.MapArea;
import com.alderfall.game.map.WorldMap;
import com.alderfall.game.render.world.WorldPropRenderer;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;

/** Real editor placement, asset resolution, and regional furnishing composition. */
public final class InteriorFurnishingsTest {
    public static void main(String[] args) throws Exception {
        WorldMap world = new WorldMap(42);
        AssetCatalog catalog = new AssetCatalog(Path.of("assets"));
        Set<String> ids = new HashSet<>();
        for (VillageManager.PlaceableAsset entry : VillageManager.interiorAssets()) {
            require(ids.add(entry.asset()), "Duplicate editor identity " + entry.asset());
        }
        for (InteriorFurnishings.Furnishing item : InteriorFurnishings.all()) {
            require(ids.contains(item.asset()), "Missing editor asset " + item.asset());
            require(VillageManager.interiorAssets(item.category()).stream().anyMatch(a -> a.asset().equals(item.asset())),
                    "Missing category entry " + item.asset());
            Path path = catalog.findAsset(DirectionalSeating.source(item.asset()));
            require(path != null, "Missing sprite " + item.asset());
            var image = ImageIO.read(path.toFile());
            require(image.getColorModel().hasAlpha() && image.getRGB(0, 0) >>> 24 == 0,
                    "Opaque sprite background " + item.asset());
            String id = world.createEditorMap("editor_furnishing", "Furniture", "interior", 18, 14);
            MapArea area = world.area(id);
            for (int y = 1; y < area.height() - 1; y++) for (int x = 1; x < area.width() - 1; x++) {
                area.tiles[y][x] = y == 1 ? 'o' : 'i';
            }
            int y = item.placement() == InteriorFurnishings.Placement.WALL ? 1 : 5;
            if (item.placement() == InteriorFurnishings.Placement.TABLETOP) {
                require(!world.addEditorInteriorProp(id, 5, y, item.asset(), 48), "Floating tabletop accepted");
                require(world.addEditorInteriorProp(id, 5, y, "interior_low_cupboard", 48), "Missing support");
                require(world.interiorSurfaceAt(id, 5, y) != null, "Support lookup failed");
            } else if (item.placement() == InteriorFurnishings.Placement.WALL) {
                require(!world.addEditorInteriorProp(id, 5, 5, item.asset(), 48), "Wall art accepted on floor");
            }
            require(world.addEditorInteriorProp(id, 5, y, item.asset(), 48), "Rejected editor item " + item.asset());
            require(!world.addEditorInteriorProp(id, 5, y, item.asset(), 48), "Overlapping duplicate accepted");
            WorldProp placed = world.editorInteriorPropAt(id, 5 + item.width() - 1, y);
            require(placed != null && placed.asset().equals(item.asset()), "Bad editor hitbox " + item.asset());
            if (item.placement() == InteriorFurnishings.Placement.FLOOR || item.placement() == InteriorFurnishings.Placement.SURFACE) {
                for (int x = 5; x < 5 + item.width(); x++) require(!world.isPassable(id, x, y), "Incomplete collision");
                require(!world.moveEditorInteriorProp(id, placed, 0, 0), "Invalid move accepted");
                require(world.editorInteriorPropAt(id, 5, y) != null, "Failed move lost item");
                require(world.moveEditorInteriorProp(id, placed, 9, 5), "Valid move rejected");
                placed = world.editorInteriorPropAt(id, 9, 5);
                require(world.isPassable(id, 5, y), "Old collision remains after move");
            }
            require(world.exportEditorMap(id).contains("," + item.asset() + ",48"), "Export lost asset identity");
            require(world.removeEditorInteriorProp(id, placed), "Removal failed");
        }
        Method place = WorldMap.class.getDeclaredMethod("addFurniture", MapArea.class, int.class, int.class, String.class);
        Method repair = WorldMap.class.getDeclaredMethod("ensureInteriorNavigable", MapArea.class);
        place.setAccessible(true); repair.setAccessible(true);
        int checked = 0;
        for (String theme : List.of("home", "inn", "tavern", "shop", "bakery", "study", "alchemy", "blacksmith", "carpenter",
                "granary", "smokehouse", "ferry_lodge", "remembrance_hall", "cistern_house", "bellhouse", "reedworks",
                "caravanserai", "rescue_lodge")) {
            for (InteriorStyle style : InteriorStyle.values()) for (int seed : new int[]{0, 42, -1024}) {
                InteriorLayout plan = InteriorLayout.compose(theme, seed, style);
                require(plan.props().equals(InteriorLayout.compose(theme, seed, style).props()), "Nondeterministic furnishings");
                String id = world.createEditorMap("editor_furnishing_layout", theme, "interior", plan.tiles()[0].length, plan.tiles().length);
                MapArea area = world.area(id);
                for (int y = 0; y < area.height(); y++) area.tiles[y] = plan.tiles()[y].clone();
                for (WorldProp prop : plan.props()) {
                    place.invoke(world, area, prop.x(), prop.y(), prop.asset());
                    require(area.props.contains(prop), "Rejected prop " + theme + "/" + style + ": " + prop);
                    if (!prop.asset().startsWith("interior_wall_")) {
                        int[] footprint = world.interiorPlacementSize(prop.asset());
                        for (int y = prop.y(); y < prop.y() + footprint[1]; y++) for (int x = prop.x(); x < prop.x() + footprint[0]; x++) {
                            require(!plan.circulation().contains(new TilePoint(x, y)), "Furniture on circulation route");
                        }
                    }
                }
                int count = area.props.size();
                repair.invoke(world, area);
                require(area.props.size() == count, "Navigation repair removed furniture: " + theme + "/" + style);
                for (TilePoint resident : plan.residents()) require(world.isPassable(id, resident.x(), resident.y()), "Blocked resident");
                checked++;
            }
        }
        checkSurfaceRendering();
        System.out.println("Household furnishings passed: " + InteriorFurnishings.all().size()
                + " editor assets; " + checked + " plans across 18 building uses.");
    }
    private static void checkSurfaceRendering() {
        GameState state = new GameState(GameConfig.load(Path.of("").toAbsolutePath().normalize()));
        AssetStore assets = new AssetStore(Path.of("assets"));
        WorldPropRenderer renderer = new WorldPropRenderer(assets, state, null);
        for (String support : List.of("interior_low_cupboard", "interior_chest_of_drawers", "interior_round_table")) {
            state.currentMapId = state.world.createEditorMap("editor_surface_render", "Surface", "interior", 18, 14);
            require(state.world.addEditorInteriorProp(state.currentMapId, 5, 5, support, 48), "Support setup failed");
            require(state.world.addEditorInteriorProp(state.currentMapId, 5, 5, "interior_tabletop_jug", 48), "Detail setup failed");
            WorldProp surface = state.world.interiorSurfaceAt(state.currentMapId, 5, 5);
            WorldProp detail = state.world.editorInteriorPropAt(state.currentMapId, 5, 5);
            for (int tile : new int[]{24, 48, 72, 96}) {
                var surfaceBounds = renderer.interiorBounds(surface, tile, 0, 0);
                var detailBounds = renderer.interiorBounds(detail, tile, 0, 0);
                var sprite = renderer.propImage(support, surfaceBounds.width, surfaceBounds.height);
                int left = sprite.getWidth(), right = -1;
                for (int y = 0; y < sprite.getHeight(); y++) for (int x = 0; x < sprite.getWidth(); x++) {
                    if ((sprite.getRGB(x, y) >>> 24) >= 16) { left = Math.min(left, x); right = Math.max(right, x); }
                }
                require(detailBounds.getCenterX() >= surfaceBounds.x + left
                        && detailBounds.getCenterX() <= surfaceBounds.x + right, "Detail floats beside visible surface");
                require(detailBounds.getCenterY() < surfaceBounds.getCenterY(), "Detail overlaps cabinet front");
                var scrolled = renderer.interiorBounds(detail, tile, 2, 3);
                require(scrolled.x == detailBounds.x - 2 * tile && scrolled.y == detailBounds.y - 3 * tile,
                        "Surface detail drifts with camera");
            }
        }
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
