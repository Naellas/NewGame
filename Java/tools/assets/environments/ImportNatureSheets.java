import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Run from Java/, alongside ImportCleanFlora. Keeps the generated alpha intact. */
public final class ImportNatureSheets {
    public static void main(String[] args) throws Exception {
        Path review = Path.of("../asset-review/nature-spaces");
        Path output = Path.of("assets/environments/props/nature/ground");
        Files.createDirectories(output);
        List<String> manifest = new ArrayList<>(List.of("asset\tsource\tcell"));
        for (String pair : List.of("meadow-forest", "desert-badlands", "tundra-highlands", "marsh-coast")) {
            var image = ImageIO.read(review.resolve(pair + ".png").toFile());
            if (!image.getColorModel().hasAlpha()) throw new IllegalStateException("Missing alpha: " + pair);
            int[] rows = ImportCleanFlora.seams(image, 4, true, 0, image.getWidth());
            for (int i = 0; i < 16; i++) {
                int y = rows[i / 4], bottom = rows[i / 4 + 1];
                int[] columns = ImportCleanFlora.seams(image, 4, false, y, bottom);
                var sprite = ImportCleanFlora.trim(image.getSubimage(columns[i % 4], y,
                        columns[i % 4 + 1] - columns[i % 4], bottom - y));
                String name = "deco_ground_" + pair.split("-")[i / 8] + "_" + (i % 8 + 1);
                ImageIO.write(sprite, "png", output.resolve(name + ".png").toFile());
                manifest.add(name + "\t" + pair + ".png\t" + i);
            }
        }
        Files.write(review.resolve("manifest.tsv"), manifest);
        System.out.println("Imported " + (manifest.size() - 1) + " ground-cover sprites.");
    }
}
