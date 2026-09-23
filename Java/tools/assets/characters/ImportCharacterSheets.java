import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Packs imagegen's 3x2 chroma-key exports into the game's fixed-baseline PNG strips.
 * Run from Java/: java tools/assets/characters/ImportCharacterSheets.java
 */
class ImportCharacterSheets {
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("--walk")) {
            for (String name : new String[]{"knight", "mage", "ranger", "cleric", "rogue"}) packWalk(name);
            return;
        }
        if (args.length > 0 && args[0].equals("--expanded")) {
            String[][] roster = {{"knight", "class_knight", "attack"}, {"mage", "class_mage", "cast"},
                    {"ranger", "class_ranger", "shoot"}, {"cleric", "class_cleric", "cast"}, {"rogue", "class_rogue", "attack"},
                    {"aria", "npc_aria", "shoot"}, {"calder", "npc_calder", "attack"}, {"cassia", "npc_cassia", "attack"},
                    {"lyra", "npc_lyra", "cast"}, {"maera", "npc_maera", "cast"}, {"rafiq", "npc_rafiq", "attack"},
                    {"samir", "npc_samir", "cast"}, {"seraphine", "npc_seraphine", "attack"}, {"vesper", "npc_vesper", "cast"}};
            for (String[] character : roster) pack(character[0], character[1], character[2], true);
            return;
        }
        pack("knight", "class_knight", "attack");
        pack("mage", "class_mage", "cast");
        pack("aria", "npc_aria", "shoot");
    }

    private static void pack(String source, String actor, String action) throws Exception {
        pack(source, actor, action, false);
    }

    private static void pack(String source, String actor, String action, boolean expanded) throws Exception {
        Path root = Path.of("assets/characters/shared/animations/character-refresh");
        BufferedImage atlas = ImageIO.read(root.resolve(expanded ? "sources-v3/" + source + ".png" : source + "_combat_v2.png").toFile());
        int columns = expanded ? 4 : 3, rows = expanded ? 3 : 2, count = columns * rows;
        BufferedImage strip = new BufferedImage(256 * count, 256, BufferedImage.TYPE_INT_ARGB);
        BufferedImage first = null;
        for (int i = 0; i < count; i++) {
            int left = (i % columns) * atlas.getWidth() / columns, top = (i / columns) * atlas.getHeight() / rows;
            int right = (i % columns + 1) * atlas.getWidth() / columns, bottomEdge = (i / columns + 1) * atlas.getHeight() / rows;
            int w = right - left, h = bottomEdge - top;
            BufferedImage cell = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                int rgba = atlas.getRGB(left + x, top + y);
                int r = rgba >> 16 & 255, g = rgba >> 8 & 255, b = rgba & 255;
                // Some transparent exports retain pure marker colors outside the cutout.
                // Only remove near-pure markers, never the muted red cloth/gold of the model.
                if (expanded && ((r > 245 && g < 12 && b < 12) || (g > 245 && r < 12 && b < 12)
                        || (r > 245 && g > 245 && b < 12))) continue;
                int key = Math.min(r, b) - g;
                if (key > 65 && r > 140 && b > 140) continue;
                // Despill only strong magenta edge contamination, retaining robe purples.
                if (key > 45 && r > 160 && b > 160) {
                    r = Math.min(r, g + 45); b = Math.min(b, g + 45);
                    rgba = (rgba & 0xff000000) | r << 16 | g << 8 | b;
                }
                cell.setRGB(x, y, rgba);
            }
            removeSpecks(cell);
            int bottom = 0;
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++)
                if ((cell.getRGB(x, y) >>> 24) > 12) bottom = Math.max(bottom, y);
            BufferedImage normalized = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = normalized.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            // Same scale for all frames; only baseline translation, never independent pose scaling.
            double scale = 240.0 / Math.max(atlas.getWidth() / (double) columns, atlas.getHeight() / (double) rows);
            int drawW = (int) Math.round(w * scale), drawH = (int) Math.round(h * scale);
            g.drawImage(cell, (256 - drawW) / 2, 247 - (int) Math.round(bottom * scale), drawW, drawH, null);
            g.dispose();
            if (first == null) first = normalized;
            Graphics2D sg = strip.createGraphics();
            sg.drawImage(normalized, i * 256, 0, null); sg.dispose();
        }
        Path output = root.resolve(expanded ? "ready-v3" : "ready");
        Files.createDirectories(output);
        String stem = actor + (expanded ? "_combat_v3" : "_combat_v2");
        ImageIO.write(first, "png", output.resolve(stem + ".png").toFile());
        ImageIO.write(strip, "png", output.resolve(stem + "_" + action + "_anim.png").toFile());
        Files.writeString(output.resolve(stem + "_" + action + "_anim.frames"), count + "\n");
        System.out.println("Packed " + stem + " / " + action);
    }

    private static void removeSpecks(BufferedImage image) {
        removeSpecks(image, 48);
    }

    private static void removeSpecks(BufferedImage image, int minimumArea) {
        int w = image.getWidth(), h = image.getHeight();
        boolean[] seen = new boolean[w * h];
        int[] queue = new int[w * h];
        for (int start = 0; start < seen.length; start++) {
            if (seen[start] || (image.getRGB(start % w, start / w) >>> 24) == 0) continue;
            int head = 0, tail = 1; queue[0] = start; seen[start] = true;
            int minX = w, maxX = -1;
            while (head < tail) {
                int pixel = queue[head++], x = pixel % w, y = pixel / w;
                minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                    int nx = x + dx, ny = y + dy;
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                    int next = ny * w + nx;
                    if (!seen[next] && (image.getRGB(nx, ny) >>> 24) != 0) {
                        seen[next] = true; queue[tail++] = next;
                    }
                }
            }
            boolean edgeDebris = minimumArea > 48 && maxX - minX < w / 12 && (minX < 3 || maxX >= w - 3);
            if (tail < minimumArea || edgeDebris) for (int i = 0; i < tail; i++) image.setRGB(queue[i] % w, queue[i] / w, 0);
        }
    }

    private static void packWalk(String name) throws Exception {
        Path root = Path.of("assets/characters/shared/animations/character-refresh");
        BufferedImage atlas = ImageIO.read(root.resolve("walk-sources/" + name + ".png").toFile());
        Path output = root.resolve("walk-ready"); Files.createDirectories(output);
        String[] directions = {"down", "left", "right", "up"};
        int[] rowEdges = new int[9]; rowEdges[8] = atlas.getHeight();
        for (int row = 1; row < 8; row++) rowEdges[row] = quietBoundary(atlas, false,
                row * atlas.getHeight() / 8, atlas.getHeight() / 16 - 1, 0, atlas.getWidth());
        if (name.equals("mage")) {
            // This export also uses taller front rows and shorter rear rows.
            double[] gutters = {0, .158, .303, .435, .564, .691, .816, .909, 1};
            for (int row = 1; row < 8; row++) rowEdges[row] = quietBoundary(atlas, false,
                    (int) Math.round(gutters[row] * atlas.getHeight()), 12, 0, atlas.getWidth());
        }
        for (int direction = 0; direction < 4; direction++) {
            // Mage's front-facing export contains four columns; preserve its eight complete
            // poses rather than slicing bodies to pretend it contains twelve.
            int columns = name.equals("mage") && direction == 0 ? 4 : 6;
            int count = columns * 2;
            BufferedImage strip = new BufferedImage(192 * count, 192, BufferedImage.TYPE_INT_ARGB);
            double scale = 180.0 / Math.max(atlas.getWidth() / 6.0, atlas.getHeight() / 8.0);
            for (int frame = 0; frame < count; frame++) {
                int col = frame % columns, row = direction * 2 + frame / columns;
                int top = rowEdges[row], h = rowEdges[row + 1] - top;
                int left = col == 0 ? 0 : quietBoundary(atlas, true, col * atlas.getWidth() / columns,
                        atlas.getWidth() / columns / 3, top, top + h);
                int right = col + 1 == columns ? atlas.getWidth() : quietBoundary(atlas, true,
                        (col + 1) * atlas.getWidth() / columns, atlas.getWidth() / columns / 3, top, top + h);
                int w = right - left;
                BufferedImage cell = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                    int rgba = atlas.getRGB(left + x, top + y), r = rgba >> 16 & 255, g = rgba >> 8 & 255, b = rgba & 255;
                    if ((Math.min(r, b) - g > 65 && r > 140 && b > 140)
                            || (r > 245 && g < 12 && b < 12) || (g > 245 && r < 12 && b < 12)
                            || (r > 245 && g > 245 && b < 12)) continue;
                    cell.setRGB(x, y, rgba);
                }
                removeSpecks(cell, Math.max(48, w * h / 50));
                int bottom = -1, minX = w, maxX = -1;
                for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) if ((cell.getRGB(x, y) >>> 24) > 32) {
                    bottom = Math.max(bottom, y); minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                }
                if (bottom < 0) throw new IllegalStateException("Blank walk cell: " + name + "/" + directions[direction] + "/" + frame);
                Graphics2D g = strip.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(cell, frame * 192 + 96 - (int) Math.round((minX + maxX + 1) * .5 * scale),
                        186 - (int) Math.round(bottom * scale), (int) Math.round(w * scale), (int) Math.round(h * scale), null);
                g.dispose();
            }
            String stem = "class_" + name + "_model_" + directions[direction] + "_walk_v3_anim";
            ImageIO.write(strip, "png", output.resolve(stem + ".png").toFile());
            Files.writeString(output.resolve(stem + ".frames"), count + "\n");
            System.out.println("Packed " + stem);
        }
    }

    /** Generated atlases have uneven gutters: cut in their actual transparent valleys. */
    private static int quietBoundary(BufferedImage image, boolean vertical, int expected, int radius, int from, int to) {
        int best = expected, bestScore = Integer.MAX_VALUE;
        int limit = vertical ? image.getWidth() : image.getHeight();
        for (int position = Math.max(1, expected - radius); position < Math.min(limit - 1, expected + radius); position++) {
            int ink = 0;
            for (int offset = -1; offset <= 1; offset++) for (int other = from; other < to; other++) {
                int pixel = vertical ? image.getRGB(position + offset, other) : image.getRGB(other, position + offset);
                if ((pixel >>> 24) > 100) ink++;
            }
            int score = ink * limit + Math.abs(position - expected);
            if (score < bestScore) { bestScore = score; best = position; }
        }
        return best;
    }
}
