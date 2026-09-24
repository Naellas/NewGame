package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import com.alderfall.game.render.world.WorldPropRenderer;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/** Anchor safety, zoom stability, sprite depth, and persistence without save-format changes. */
public final class PropPlacementTest {
    public static void main(String[] args) {
        GameState state = new GameState(GameConfig.load(Path.of("").toAbsolutePath().normalize()));
        state.currentMapId = WorldMap.OVERWORLD_ID;
        var effects = (WorldPropRenderer.Effects) Proxy.newProxyInstance(WorldPropRenderer.Effects.class.getClassLoader(),
                new Class<?>[]{WorldPropRenderer.Effects.class}, (proxy, method, values) -> {
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == float.class) return 0f;
                    if (method.getReturnType() == char.class) return values[0];
                    return null;
                });
        WorldPropRenderer props = new WorldPropRenderer(new AssetStore(Path.of("assets").toAbsolutePath()), state, effects);
        int changed = 0;
        var sizes = new HashSet<Double>();
        for (WorldProp prop : state.world.props(state.currentMapId)) {
            var p = PropPlacement.at(state.world, state.currentMapId, prop);
            require(p.equals(PropPlacement.at(state.world, state.currentMapId,
                    new WorldProp(prop.x(), prop.y(), prop.asset(), prop.size(), prop.visualSlot()))), "Reload changes placement");
            if (p.kind() == PropPlacement.Kind.FIXED) {
                require(p.x() == 0.5 && p.y() == 1 && p.scale() == 1, "Fixed landmark moved");
                continue;
            }
            changed++;
            sizes.add(p.scale());
            if (prop.visualSlot() >= 0) {
                require(p.x() >= 0.06 && p.x() <= 0.94 && p.y() >= 0.12 && p.y() <= 0.92,
                        "Ground detail leaves its owner tile");
                require(PropCollision.footprint(state.world, state.currentMapId, prop) == null, "Ground detail blocks movement");
                continue;
            }
            require(p.x() >= 0.14 && p.x() <= 0.86 && p.y() >= 0.32 && p.y() <= 0.86, "Anchor leaves tile");
            require(p.scale() >= 0.76 && p.scale() <= 1.15, "Scale outside family range");
        }
        // Candidate IDs must no longer lock plants to quarter-tile rows or depend on artwork.
        var anchorBins = new HashSet<Integer>();
        var moduleScales = new HashSet<Double>();
        for (int x = 10; x < 80; x++) for (int slot = 0; slot < 4; slot++) {
            var a = PropPlacement.at(state.world, state.currentMapId, new WorldProp(x, 44, "deco_ground_tundra_1", 20, slot));
            var b = PropPlacement.at(state.world, state.currentMapId, new WorldProp(x, 44, "deco_ground_grass_2", 20, slot));
            require(a.equals(b), "Changing cover module moves its root");
            moduleScales.add(a.scale());
            anchorBins.add((int) (a.x() * 10) + 10 * (int) (a.y() * 10));
        }
        require(moduleScales.size() == 5 && java.util.Collections.max(moduleScales) / java.util.Collections.min(moduleScales) > 2.5, "Module scale range too narrow");
        require(anchorBins.size() > 45, "Ground cover still follows fixed sub-tile rows");
        double minimum = 1, maximum = 0;
        for (int y = 0; y < 40; y++) for (int x = 0; x < 40; x++) {
            var patch = GroundCoverPattern.patch(x, y + .3, 714);
            minimum = Math.min(minimum, patch.coverage()); maximum = Math.max(maximum, patch.coverage());
            require(Math.abs(GroundCoverPattern.patch(x - .0001, y + .3, 714).coverage()
                    - GroundCoverPattern.patch(x + .0001, y + .3, 714).coverage()) < .001,
                    "Cover discontinuity at tile or patch boundary");
        }
        require(minimum < .01 && maximum > .95, "Missing bare gaps or full patch centers");
        require(changed > 100 && sizes.size() >= 5, "Missing natural variation");
        WorldProp tree = new WorldProp(123, 99, "deco_tree_oak_harvestable", 80);
        var placement = PropPlacement.at(state.world, state.currentMapId, tree);
        for (int zoom : new int[]{50, 75, 100, 150}) {
            state.zoom = zoom;
            int tile = GameConfig.TILE * zoom / 100;
            Rectangle a = props.propBounds(tree, tile, 120, 96);
            Rectangle b = props.propBounds(tree, tile, 121, 97);
            require(a.x - b.x == tile && a.y - b.y == tile, "Camera moved anchor within tile");
            require(Math.abs(a.getCenterX() / tile - 3 - placement.x()) <= 1.0 / tile, "Zoom moves root X");
            require(Math.abs(a.getMaxY() / tile - 3 - placement.y()) <= 1.0 / tile, "Zoom moves root Y");
        }
        // Terrain constraints update immediately; placement does not depend on prop-list membership.
        var area = state.world.area(state.currentMapId);
        char old = area.tiles[tree.y()][tree.x() + 1];
        area.setTile(tree.x() + 1, tree.y(), 'r');
        require(PropPlacement.at(state.world, state.currentMapId, tree).x() <= 0.58, "Root encroaches on road");
        area.setTile(tree.x() + 1, tree.y(), old);
        require(placement.equals(PropPlacement.at(state.world, state.currentMapId, tree)), "Placement drift after terrain restore");
        require(PropPlacement.kind("location_camp_palisade") == PropPlacement.Kind.FIXED, "Connected walls jitter");
        require(PropPlacement.kind("deco_tree_elder_harvestable") == PropPlacement.Kind.FIXED, "Landmark tree loses identity");
        require(PropPlacement.kind("deco_tree_enchanted_stump") == PropPlacement.Kind.ROCK, "Stump treated as tall tree");
        require(PropPlacement.at(state.world, "player_village", tree).scale() == 1, "Editor placement changed");
        state.zoom = 100;
        WorldRenderer renderer = new WorldRenderer(null, null, props, null);
        List<WorldProp> scene = new ArrayList<>(List.of(tree,
                new WorldProp(123, 98, "deco_tree_birch_harvestable", 90),
                new WorldProp(124, 99, "deco_tree_pine_harvestable", 76),
                new WorldProp(123, 99, "deco_grass_wildflowers", 24)));
        BufferedImage forward = render(renderer, state, scene);
        Collections.reverse(scene);
        BufferedImage reverse = render(renderer, state, scene);
        for (int y = 0; y < 288; y++) for (int x = 0; x < 288; x++) {
            require(forward.getRGB(x, y) == reverse.getRGB(x, y), "Prop input order changes depth");
        }
        System.out.println("Prop placement passed: " + changed + " natural props; bounded anchors/scales, fixed landmarks, road clearance, four zooms and stable depth.");
    }

    private static BufferedImage render(WorldRenderer renderer, GameState state, List<WorldProp> scene) {
        BufferedImage image = new BufferedImage(288, 288, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        renderer.drawVisibleProps(g, new WorldRenderer.PropContext(state, 120, 96, 6, 6, 48, 100, 0), scene, new ArrayList<>());
        g.dispose();
        return image;
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
