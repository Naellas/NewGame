package com.alderfall.game;

import com.alderfall.game.map.MapArea;
import com.alderfall.game.render.world.TerrainFeatureRenderer;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Arrays;

/** Checks tile seams and draw-order independence without relying on a screenshot baseline. */
public final class BridgeRenderingTest {
    public static void main(String[] args) throws Exception {
        GameState state = new GameState(GameConfig.load(Path.of("").toAbsolutePath().normalize()));
        state.currentMapId = state.world.createEditorMap("editor_bridge_check", "Bridge check", "village", 10, 10);
        MapArea area = state.world.area(state.currentMapId);
        TerrainFeatureRenderer renderer = new TerrainFeatureRenderer(null, state, null, null, null);
        Method draw = TerrainFeatureRenderer.class.getDeclaredMethod("drawBridgeTile", Graphics2D.class,
                int.class, int.class, int.class, int.class);
        draw.setAccessible(true);
        for (int zoom : new int[]{50, 75, 100, 150}) {
            state.zoom = zoom;
            int ts = GameConfig.TILE * zoom / 100;
            renderer.useContext(new TerrainFeatureRenderer.RenderContext(ts, 10, 10, 0));
            for (boolean vertical : new boolean[]{true, false}) {
                for (char[] row : area.tiles) Arrays.fill(row, 'w');
                for (int along = 1; along <= 8; along++) {
                    int x = vertical ? 5 : along, y = vertical ? along : 5;
                    area.tiles[y][x] = along == 1 || along == 8 ? 'r' : 'B';
                }
                // A parallel bank road must not pull the deck sideways.
                area.tiles[4][4] = 'r';
                BufferedImage forward = render(renderer, draw, ts, vertical, false);
                BufferedImage backward = render(renderer, draw, ts, vertical, true);
                for (int y = 0; y < forward.getHeight(); y++) {
                    for (int x = 0; x < forward.getWidth(); x++) {
                        require(forward.getRGB(x, y) == backward.getRGB(x, y), "Tile order changes bridge pixels");
                        int along = vertical ? y : x;
                        if (along < ts * 2 || along >= ts * 8) {
                            require((forward.getRGB(x, y) >>> 24) == 0, "Bridge paints an end cap outside its span");
                        }
                    }
                }
                int center = ts * 5 + ts / 2;
                int innerHalf = Math.max(1, ts / 5);
                for (int along = ts * 2; along < ts * 8; along++) {
                    for (int across = center - innerHalf; across <= center + innerHalf; across++) {
                        int pixel = vertical ? forward.getRGB(across, along) : forward.getRGB(along, across);
                        require((pixel >>> 24) == 255, "Gap or narrowing in bridge deck");
                    }
                }
            }
        }
        System.out.println("Bridge rendering passed: both orientations at four zoom levels, continuous decks and stable tile seams.");
    }

    private static BufferedImage render(TerrainFeatureRenderer renderer, Method draw, int ts,
                                        boolean vertical, boolean reverse) throws Exception {
        BufferedImage image = new BufferedImage(ts * 10, ts * 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            for (int i = 0; i < 6; i++) {
                int along = reverse ? 7 - i : 2 + i;
                int x = vertical ? 5 : along, y = vertical ? along : 5;
                draw.invoke(renderer, g, x, y, x * ts, y * ts);
            }
        } finally {
            g.dispose();
        }
        return image;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
