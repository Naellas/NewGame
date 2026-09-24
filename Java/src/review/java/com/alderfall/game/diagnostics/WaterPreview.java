package com.alderfall.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Surface-only weather comparison. Writes frames to a caller-selected scratch directory. */
public final class WaterPreview {
    public static void main(String[] args) throws Exception {
        Path output = Path.of(args.length > 0 ? args[0] : "temp/water/surface-frames");
        Files.createDirectories(output);
        GameState state = new GameState(GameConfig.load(Path.of("").toAbsolutePath()));
        WaterTileRenderer renderer = new WaterTileRenderer(new AssetStore(Path.of("assets")));
        WeatherSystem[] weather = {new WeatherSystem(state), new WeatherSystem(state), new WeatherSystem(state)};
        set(weather[1], "waterWave", .58); set(weather[1], "windX", .8);
        set(weather[2], "waterWave", .95); set(weather[2], "stormIntensity", 1);
        set(weather[2], "windX", 1);
        String[] labels = {"CALM", "WIND", "STORM"};
        BufferedImage bed = new BufferedImage(672, 144, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 144; y++) for (int x = 0; x < 672; x++) {
            double depth = Math.max(1, Math.min(3, 1 + x / 160.0));
            bed.setRGB(x, y, WaterTileRenderer.surfaceColor(x / 48.0, y / 48.0, depth));
        }
        for (int frame = 0; frame < 80; frame++) {
            BufferedImage image = new BufferedImage(672, 504, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            for (int row = 0; row < 3; row++) {
                int top = row * 168;
                g.setColor(new Color(20, 29, 36)); g.fillRect(0, top, 672, 24);
                g.setColor(Color.WHITE); g.drawString(labels[row] + "     Shallows  >  Wading  >  Deep water", 12, top + 17);
                g.drawImage(bed, 0, top + 24, null);
                for (int y = 0; y < 3; y++) for (int x = 0; x < 14; x++)
                    renderer.draw(g, x, y, x * 48, top + 24 + y * 48, 48, frame * 2,
                            "surface-review", weather[row], WeatherQuality.HIGH);
            }
            g.dispose();
            ImageIO.write(image, "png", output.resolve(String.format("water-%03d.png", frame)).toFile());
        }
        System.out.println("Wrote 80 surface frames at 10 fps to " + output);
    }
    private static void set(WeatherSystem weather, String name, double value) throws Exception {
        var field = WeatherSystem.class.getDeclaredField(name);
        field.setAccessible(true); field.setDouble(weather, value);
    }
}
