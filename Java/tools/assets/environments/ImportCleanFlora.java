import java.awt.AlphaComposite;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Import generated alpha sheets without chroma keying or recoloring their edge pixels. Run from Java/. */
public final class ImportCleanFlora {
    static final Path ASSETS = Path.of("assets").toAbsolutePath();
    static final Path REVIEW = Path.of("../asset-review/flora-refresh").toAbsolutePath().normalize();
    static final String[] SOFT = {
        "grass_tuft", "clover_white", "daisy_patch", "blue_flowers", "yellow_flowers", "purple_flowers",
        "grass_blades", "clover_pink", "daisy_small", "alpine_mix", "clover_low", "blue_wildflowers",
        "stone_large", "pebble_cluster", "mossy_rock", "flat_stones", "pebbles", "mossy_boulder",
        "moss_stones", "leaf_litter", "dry_grass", "frost_grass", "reeds", "purple_rockflowers"
    };
    static final String[] DENSE = {
        "dense_snow_mound", "dense_snow_boulder", "dense_snow_grass", "dense_frost_reeds",
        "dense_meadow_daisies", "dense_meadow_flowers", "dense_leafy_plant", "dense_tall_grass",
        "dense_berry_bush", "dense_leaf_bush", "dense_white_blossom_plant", "dense_blue_flower_bush",
        "water_cattails", "water_reeds_gold", "water_lily_reeds", "water_lily_reeds_white",
        "water_lily_white", "water_lily_pink", "water_duckweed", "water_floating_leaves",
        "water_wet_stones", "water_mossy_boulder", "water_shore_grass", "water_flower_reeds",
        "dense_calla_plant", "dense_marsh_grass", "water_sparkle_patch", "water_weed_patch"
    };
    static final Map<String, String> ALIASES = Map.of(
        "dense_meadow_flowers", "grass/deco_flowers.png", "dense_tall_grass", "grass/deco_grass_clump.png",
        "dense_snow_mound", "tundra/deco_snow_mound.png", "reeds", "marsh/deco_reeds.png");
    static final List<String> manifest = new ArrayList<>(List.of("asset\tsource\tcell\twidth\theight"));

    public static void main(String[] args) throws Exception {
        sheet("soft-clean-sheet.png", 6, 4, SOFT);
        sheet("dense-clean-sheet.png", 4, 7, DENSE);
        single("mushrooms-clean.png", "forest/deco_forest_mushrooms.png");
        single("mushrooms-clean.png", "marsh/deco_mushrooms.png");
        single("blue-ring-clean.png", "forest/deco_forest_blue_mushroom_ring.png");
        single("fern-clean.png", "forest/deco_forest_fern.png");
        Files.write(REVIEW.resolve("manifest.tsv"), manifest);
        System.out.println("Imported " + (manifest.size() - 1) + " clean-alpha sprites; original assets backed up in " + REVIEW.resolve("before"));
    }

    static void sheet(String source, int columns, int rows, String[] names) throws Exception {
        if (names.length != columns * rows) throw new IllegalArgumentException("Grid mapping mismatch");
        BufferedImage image = ImageIO.read(REVIEW.resolve(source).toFile());
        if (!image.getColorModel().hasAlpha()) throw new IllegalArgumentException("Source has no alpha: " + source);
        int[] rowEdges = seams(image, rows, true, 0, image.getWidth());
        for (int i = 0; i < names.length; i++) {
            int y = rowEdges[i / columns], bottom = rowEdges[i / columns + 1];
            int[] columnEdges = seams(image, columns, false, y, bottom);
            int x = columnEdges[i % columns], right = columnEdges[i % columns + 1];
            BufferedImage sprite = trim(image.getSubimage(x, y, right - x, bottom - y));
            write(sprite, "deco/deco_soft_" + names[i] + ".png", source, i);
            if (ALIASES.containsKey(names[i])) write(sprite, ALIASES.get(names[i]), source, i);
        }
    }

    // Generated sheets keep their ordered grid but can shift a row slightly. Locate empty gutters before slicing.
    static int[] seams(BufferedImage image, int count, boolean horizontal, int from, int to) {
        int length = horizontal ? image.getHeight() : image.getWidth();
        int[] result = new int[count + 1]; result[count] = length;
        for (int i = 1; i < count; i++) {
            int ideal = i * length / count, radius = length / count / 3;
            int best = ideal, bestScore = Integer.MAX_VALUE;
            for (int position = ideal - radius; position <= ideal + radius; position++) {
                int score = 0;
                for (int cross = from; cross < to; cross++) {
                    int pixel = horizontal ? image.getRGB(cross, position) : image.getRGB(position, cross);
                    if ((pixel >>> 24) > 100) score++;
                }
                if (score < bestScore || (score == bestScore && Math.abs(position - ideal) < Math.abs(best - ideal))) {
                    best = position; bestScore = score;
                }
            }
            if (bestScore != 0) throw new IllegalArgumentException("No clear gutter at row/column " + i);
            result[i] = best;
        }
        return result;
    }

    static void single(String source, String destination) throws Exception {
        write(trim(ImageIO.read(REVIEW.resolve(source).toFile())), destination, source, 0);
    }

    static BufferedImage trim(BufferedImage image) {
        int left = image.getWidth(), top = image.getHeight(), right = -1, bottom = -1;
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
            if ((image.getRGB(x, y) >>> 24) < 24) continue;
            left = Math.min(left, x); top = Math.min(top, y); right = Math.max(right, x); bottom = Math.max(bottom, y);
        }
        if (right < left) throw new IllegalArgumentException("Empty sprite cell");
        left = Math.max(0, left - 2); top = Math.max(0, top - 2);
        right = Math.min(image.getWidth() - 1, right + 2); bottom = Math.min(image.getHeight() - 1, bottom + 2);
        int width = right - left + 1, height = bottom - top + 1;
        double scale = Math.min(1, 256.0 / Math.max(width, height));
        var result = new BufferedImage(Math.max(1, (int) Math.round(width * scale)),
                Math.max(1, (int) Math.round(height * scale)), BufferedImage.TYPE_INT_ARGB);
        var g = result.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(image, 0, 0, result.getWidth(), result.getHeight(), left, top, right + 1, bottom + 1, null);
        g.dispose(); return result;
    }

    static void write(BufferedImage image, String relative, String source, int cell) throws Exception {
        Path destination = ASSETS.resolve(relative), backup = REVIEW.resolve("before").resolve(relative);
        if (!Files.isRegularFile(destination)) throw new IllegalArgumentException("Unknown runtime asset: " + destination);
        Files.createDirectories(backup.getParent());
        if (!Files.exists(backup)) Files.copy(destination, backup);
        ImageIO.write(image, "png", destination.toFile());
        manifest.add(relative + "\t" + source + "\t" + cell + "\t" + image.getWidth() + "\t" + image.getHeight());
    }
}
