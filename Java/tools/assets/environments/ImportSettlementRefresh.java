import java.nio.file.*;
import java.util.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/** Atlas slicing only: transparency comes from the reviewed imagegen cutout. */
public final class ImportSettlementRefresh {
    public static void main(String[] args) throws Exception {
        Path review = Path.of("../asset-review/settlement-refresh");
        Path output = Path.of("assets/environments/settlements/city/props/refresh");
        Files.createDirectories(output);
        var sheet = ImageIO.read(review.resolve("street-alpha.png").toFile());
        if (!sheet.getColorModel().hasAlpha()) throw new IllegalStateException("Missing alpha");
        var rows = Files.readAllLines(review.resolve("crops.tsv"));
        var preview = new BufferedImage(840, 480, BufferedImage.TYPE_INT_RGB);
        var g = preview.createGraphics();
        g.setColor(new Color(91, 108, 70)); g.fillRect(0, 0, 840, 480);
        int index = 0;
        for (String row : rows.subList(1, rows.size())) {
            String[] c = row.split("\t");
            var sprite = ImportCleanFlora.trim(sheet.getSubimage(Integer.parseInt(c[1]), Integer.parseInt(c[2]),
                    Integer.parseInt(c[3]), Integer.parseInt(c[4])));
            ImageIO.write(sprite, "png", output.resolve("city_prop_refresh_" + c[0] + ".png").toFile());
            int size = Integer.parseInt(c[5]), x = index % 7 * 120, y = index / 7 * 160;
            double scale = size / (double) Math.max(sprite.getWidth(), sprite.getHeight());
            int w = (int)Math.round(sprite.getWidth()*scale), h = (int)Math.round(sprite.getHeight()*scale);
            g.drawImage(sprite, x+60-w/2, y+95-h, w, h, null);
            g.setColor(Color.WHITE); g.drawString(c[0], x+3, y+120);
            g.drawString(size+" px maximum", x+3, y+138); index++;
        }
        g.dispose(); ImageIO.write(preview, "png", review.resolve("scale-preview.png").toFile());
        System.out.println("Imported " + index + " reviewed street sprites");
    }
}
