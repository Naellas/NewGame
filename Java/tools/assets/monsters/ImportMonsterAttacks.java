import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

/** Import generated 4x3 pose atlases without cutting weapons at cell boundaries.
 * Run from Java: java tools/assets/monsters/ImportMonsterAttacks.java [sprite ...]
 */
class ImportMonsterAttacks {
    static final Path ROOT = Path.of("assets/source/monster-attacks-2026-09-23");
    static final Path OUT = Path.of("assets/characters/monsters/animations");
    static final int SIZE = 256;
    static boolean replace;

    record Component(int[] pixels, Rectangle bounds, double cx, double cy) {}

    public static void main(String[] args) throws Exception {
        Files.createDirectories(OUT);
        for (String name : args) {
            if (name.equals("--replace")) replace = true;
            else pack(name);
        }
    }

    static void pack(String name) throws Exception {
        BufferedImage atlas = ImageIO.read(ROOT.resolve(name + ".png").toFile());
        // Some alpha exports retain pure segmentation marker colors just outside the cutout.
        // These narrow thresholds do not key out the shaded green skin or red costumes.
        for (int y = 0; y < atlas.getHeight(); y++) for (int x = 0; x < atlas.getWidth(); x++) {
            int c = atlas.getRGB(x, y), r = c >> 16 & 255, gr = c >> 8 & 255, b = c & 255;
            if ((r > 245 && gr < 12 && b < 12) || (gr > 245 && r < 12 && b < 12)
                    || (r > 245 && gr > 245 && b < 12) || (r > 245 && b > 245 && gr < 12)
                    || (b > 245 && r < 12 && gr < 12) || (b > 245 && gr > 245 && r < 12)) atlas.setRGB(x, y, 0);
        }
        List<Component> components = components(atlas);
        components.sort(Comparator.comparingInt((Component c) -> c.pixels.length).reversed());
        if (components.size() < 12) throw new IllegalStateException(name + ": fewer than 12 separate poses");
        Component[] poses = new Component[12];
        for (Component c : components.subList(0, 12)) {
            int col = Math.min(3, (int) (c.cx * 4 / atlas.getWidth()));
            int row = Math.min(2, (int) (c.cy * 3 / atlas.getHeight()));
            int slot = row * 4 + col;
            if (poses[slot] != null) throw new IllegalStateException(name + ": overlapping/missing poses at " + slot);
            poses[slot] = c;
        }
        int maxW = 0, maxH = 0;
        for (Component c : poses) { maxW = Math.max(maxW, c.bounds.width); maxH = Math.max(maxH, c.bounds.height); }
        double scale = Math.min(240.0 / maxW, 240.0 / maxH);
        BufferedImage strip = new BufferedImage(SIZE * 12, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = strip.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for (int i = 0; i < poses.length; i++) {
            Component c = poses[i]; Rectangle b = c.bounds;
            BufferedImage pose = new BufferedImage(b.width, b.height, BufferedImage.TYPE_INT_ARGB);
            for (int p : c.pixels) pose.setRGB(p % atlas.getWidth() - b.x, p / atlas.getWidth() - b.y, atlas.getRGB(p % atlas.getWidth(), p / atlas.getWidth()));
            int w = (int) Math.round(b.width * scale), h = (int) Math.round(b.height * scale);
            // Preserve a single scale across the cycle; anchor feet to a fixed baseline.
            boolean flip = name.equals("bandit_archer");
            int x = i * SIZE + (SIZE - w) / 2;
            g.drawImage(pose, flip ? x + w : x, 248 - h, flip ? -w : w, h, null);
        }
        g.dispose();
        String stem = name + ((name.equals("skeleton") || name.equals("spider")) ? "_attack_v2_anim" : "_attack_anim");
        Path output = OUT.resolve(stem + ".png");
        if (Files.exists(output) && !replace) throw new IllegalStateException("Refusing to replace existing " + output);
        ImageIO.write(strip, "png", output.toFile());
        Files.writeString(OUT.resolve(stem + ".frames"), "12\n");
        System.out.println(name + ": 12 poses, " + maxW + "x" + maxH + " shared scale " + scale);
    }

    static List<Component> components(BufferedImage image) {
        int w = image.getWidth(), h = image.getHeight();
        boolean[] seen = new boolean[w * h]; int[] queue = new int[w * h];
        List<Component> result = new ArrayList<>();
        for (int start = 0; start < seen.length; start++) {
            if (seen[start] || (image.getRGB(start % w, start / w) >>> 24) < 16) continue;
            int head = 0, tail = 1; queue[0] = start; seen[start] = true;
            int minX = w, minY = h, maxX = 0, maxY = 0; long sx = 0, sy = 0;
            while (head < tail) {
                int p = queue[head++], x = p % w, y = p / w;
                minX = Math.min(minX, x); minY = Math.min(minY, y); maxX = Math.max(maxX, x); maxY = Math.max(maxY, y); sx += x; sy += y;
                for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                    int nx = x + dx, ny = y + dy;
                    if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue;
                    int q = ny * w + nx;
                    if (!seen[q] && (image.getRGB(nx, ny) >>> 24) >= 16) { seen[q] = true; queue[tail++] = q; }
                }
            }
            if (tail > 100) result.add(new Component(Arrays.copyOf(queue, tail), new Rectangle(minX,minY,maxX-minX+1,maxY-minY+1), sx/(double)tail, sy/(double)tail));
        }
        return result;
    }
}
