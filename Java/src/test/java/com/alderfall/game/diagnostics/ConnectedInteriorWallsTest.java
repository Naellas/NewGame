package com.alderfall.game;

import com.alderfall.game.map.MapArea;
import com.alderfall.game.render.world.ConnectedInteriorWalls;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Arrays;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Visible seams and cached tall-wall pixels, including chunk boundaries and edits. */
public final class ConnectedInteriorWallsTest {
    public static void main(String[] args) throws Exception {
        AssetStore assets = new AssetStore(Path.of("assets"));
        BufferedImage doorway = new BufferedImage(96, 48, BufferedImage.TYPE_INT_ARGB);
        Graphics2D doorGraphics = doorway.createGraphics();
        ConnectedInteriorWalls.door(doorGraphics, assets, "timber", 0, 0, 48, true, true, false);
        ConnectedInteriorWalls.door(doorGraphics, assets, "timber", 48, 0, 48, true, false, true);
        doorGraphics.dispose();
        for (int y = 8; y < 48; y++)
            require((doorway.getRGB(47, y) >>> 24) == 0 && (doorway.getRGB(48, y) >>> 24) == 0,
                    "Paired door cells must share an opening without a middle jamb");
        for (String material : new String[]{"timber", "paneled", "eastern", "stone", "rustic"}) {
            for (String piece : new String[]{"floor", "face", "cap", "side", "base", "post"})
                require(assets.hasSprite("interior_module_" + (material.equals("rustic")
                        && !piece.equals("floor") && !piece.equals("face") ? "timber" : material) + "_" + piece), "Missing material piece");
            for (int size : new int[]{24, 48, 73}) {
                BufferedImage image = new BufferedImage(size * 2, size * 2, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = image.createGraphics();
                for (int y = 0; y < 2; y++) for (int x = 0; x < 2; x++)
                    ConnectedInteriorWalls.floor(g, assets, material, x - 1, y - 1, x * size, y * size, size);
                g.dispose();
                for (int p = 0; p < size * 2; p++) {
                    require(image.getRGB(size - 1, p) == image.getRGB(size, p), "Horizontal floor seam");
                    require(image.getRGB(p, size - 1) == image.getRGB(p, size), "Vertical floor seam");
                }
            }
        }
        SwingUtilities.invokeAndWait(() -> {
            GamePanel panel = new GamePanel(Path.of("").toAbsolutePath());
            try {
                ((Timer) field("timer").get(panel)).stop();
                GameState state = (GameState) field("state").get(panel);
                WorldRenderer renderer = (WorldRenderer) field("worldRenderer").get(panel);
                state.currentMapId = state.world.createEditorMap("editor_interior_material_check", "Materials", "interior", 12, 12);
                MapArea area = state.world.area(state.currentMapId);
                for (char[] row : area.tiles) Arrays.fill(row, 'i');
                for (int x = 0; x < 12; x++) { area.setTile(x, 4, 'o'); area.setTile(x, 8, 'o'); }
                for (int y = 0; y < 12; y++) area.setTile(2, y, 'o');
                for (int x = 3; x <= 5; x++) area.interiorFloorMaterials.put(new TilePoint(x, 5), "stone");
                for (int size : new int[]{24, 48, 73}) {
                    field("editorTileSize").setInt(panel, size);
                    compareTerrain(renderer, state, size);
                }
                area.setTile(6, 4, 'e');
                field("editorTileSize").setInt(panel, 48);
                compareTerrain(renderer, state, 48);
                checkMountsAndOcclusion(renderer, state, assets);
                checkSlimJunctions(state, assets);
                System.out.println("Connected interior materials, floor seams, chunk boundaries and edited doors passed.");
            } catch (Exception e) { throw new IllegalStateException(e); }
            finally { panel.shutdown(); }
        });
    }

    private static void compareTerrain(WorldRenderer renderer, GameState state, int size) {
        var context = new WorldRenderer.TerrainContext(state, 0, 0, 12, 12, 12, 12, size, 100, "interior");
        BufferedImage direct = new BufferedImage(size * 12, size * 12, BufferedImage.TYPE_INT_RGB);
        BufferedImage cached = new BufferedImage(size * 12, size * 12, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = direct.createGraphics(); renderer.drawTerrainBase(g, context); g.dispose();
        g = cached.createGraphics(); renderer.drawCachedTerrainBase(g, context); g.dispose();
        // Different zoom levels intentionally reuse scaled terrain caches; compare
        // at native 24px and after edits (which force a new native 48px render).
        if (size != 24 && state.world.tileAt(state.currentMapId, 6, 4) != 'e') return;
        for (int y = 0; y < size * 12; y++) for (int x = 0; x < size * 12; x++)
            require(direct.getRGB(x, y) == cached.getRGB(x, y), "Chunk mismatch at " + x + "," + y);
    }

    private static Field field(String name) throws Exception {
        Field field = GamePanel.class.getDeclaredField(name); field.setAccessible(true); return field;
    }

    private static void checkSlimJunctions(GameState state, AssetStore assets) {
        var area = state.world.area(state.currentMapId);
        for (int ts : new int[]{24, 48, 73}) {
            require(ConnectedInteriorWalls.sideWidth(ts) <= ts / 5.0, "Side wall must be slimmer than a fifth of a tile");
            // Left/right turns, T and cross junctions share their south rail's
            // exact edge pixels; this catches width, grain and double-post seams.
            for (int mask = 1; mask <= 7; mask++) {
                if ((mask & 3) == 0) continue;
                for (char[] row : area.tiles) Arrays.fill(row, 'i');
                area.setTile(4, 4, 'o'); area.setTile(4, 5, 'o'); area.setTile(4, 6, 'o');
                if ((mask & 1) != 0) area.setTile(3, 4, 'o');
                if ((mask & 2) != 0) area.setTile(5, 4, 'o');
                if ((mask & 4) != 0) area.setTile(4, 3, 'o');
                BufferedImage image = new BufferedImage(ts * 3, ts * 3, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = image.createGraphics();
                ConnectedInteriorWalls.draw(g, assets, state.world, state.currentMapId, 4, 4, ts, ts, ts);
                ConnectedInteriorWalls.draw(g, assets, state.world, state.currentMapId, 4, 5, ts, ts * 2, ts);
                g.dispose();
                int width = ConnectedInteriorWalls.sideWidth(ts), left = ts + (ts - width) / 2;
                for (int x = left; x < left + width; x++)
                    require(image.getRGB(x, ts * 2 - 1) == image.getRGB(x, ts * 2), "Junction/side seam at scale " + ts + ", mask " + mask);
                var face = ConnectedInteriorWalls.bounds(state.world, state.currentMapId, 4, 4, ts, ts, ts);
                if ((mask & 1) == 0) require(face.x == left, "Left corner edge must align with rail");
                if ((mask & 2) == 0) require(face.x + face.width == left + width, "Right corner edge must align with rail");
            }
        }
        System.out.println("Slim side walls and continuous corner/T/cross junctions passed at three scales.");
    }

    private static void checkMountsAndOcclusion(WorldRenderer renderer, GameState state, AssetStore assets) {
        var area = state.world.area(state.currentMapId);
        for (char[] row : area.tiles) Arrays.fill(row, 'i');
        for (int x = 1; x < 11; x++) area.setTile(x, 4, 'o');
        area.setTile(1, 5, 'o'); area.setTile(1, 6, 'o');
        var corner = ConnectedInteriorWalls.bounds(state.world, state.currentMapId, 1, 4, 48, 192, 48);
        var side = ConnectedInteriorWalls.bounds(state.world, state.currentMapId, 1, 5, 48, 240, 48);
        require(corner.x == side.x, "Corner must end flush with the side rail");
        require(ConnectedInteriorWalls.mountAt(state.world, state.currentMapId, 5.5, 3.5).equals(new TilePoint(5, 4)),
                "Clicking the raised face must resolve to the wall anchor");
        for (String asset : new String[]{"interior_wall_window_leaded", "interior_wall_window_curtained"}) {
            require(assets.hasSprite(asset), "Missing imported window " + asset);
            require(InteriorFurnishings.is(asset, InteriorFurnishings.Placement.WALL), "Window must be placeable on a wall");
            var mount = renderer.interiorPropBounds(new WorldProp(5, 4, asset, 48), 48, 0, 0);
            require(mount.y > 144 && mount.getMaxY() < 240, "Window must fit inside the tall face");
            area.setTile(5, 5, 'x');
            var shortMount = renderer.interiorPropBounds(new WorldProp(5, 4, asset, 48), 48, 0, 0);
            require(shortMount.y >= 192 && shortMount.getMaxY() < 240, "Window must fit a short foreground face");
            area.setTile(5, 5, 'i');
        }
        var props = java.util.List.of(new WorldProp(5, 3, "interior_washstand", 48));
        var context = new WorldRenderer.PropContext(state, 0, 0, 12, 12, 48, 100, 0);
        BufferedImage wallOnly = occlusionScene(renderer, state, context, java.util.List.of(), false);
        BufferedImage hidden = occlusionScene(renderer, state, context, props, false);
        BufferedImage revealed = occlusionScene(renderer, state, context, props, true);
        BufferedImage emptyReveal = occlusionScene(renderer, state, context, java.util.List.of(), true);
        int furniturePixels = 0;
        for (int y = 144; y < 192; y++) for (int x = 240; x < 288; x++) {
            require(wallOnly.getRGB(x, y) == hidden.getRGB(x, y), "Furniture painted on partition at " + x + "," + y);
            if (revealed.getRGB(x, y) != emptyReveal.getRGB(x, y)) furniturePixels++;
        }
        require(furniturePixels > 50, "Player cutaway must reveal the furniture behind the partition");
        System.out.println("Wall mounts, window fit, flush corners, furniture occlusion and player cutaway passed.");
    }

    private static BufferedImage occlusionScene(WorldRenderer renderer, GameState state, WorldRenderer.PropContext context,
                                                java.util.List<WorldProp> props, boolean reveal) {
        BufferedImage image = new BufferedImage(576, 576, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        renderer.drawTerrainBase(g, new WorldRenderer.TerrainContext(state, 0, 0, 12, 12, 12, 12, 48, 100, "interior"));
        WorldDepthRenderer depth = new WorldDepthRenderer(); depth.begin(48);
        renderer.drawVisibleProps(g, context, props, new java.util.ArrayList<>(), depth);
        if (reveal) depth.character(g, new java.awt.Rectangle(240, 144, 48, 48), ignored -> { });
        depth.draw(g); g.dispose();
        return image;
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
