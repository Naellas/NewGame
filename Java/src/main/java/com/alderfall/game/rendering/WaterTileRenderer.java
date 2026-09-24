package com.alderfall.game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.AlphaComposite;
import java.awt.image.BufferedImage;

/** Continuous world-space water. Terrain caches the bed; wavelets and generated crest cycles animate. */
public final class WaterTileRenderer {
    private final BufferedImage[] crests = new BufferedImage[12];

    public WaterTileRenderer(AssetStore assets) {
        for (int i = 0; i < crests.length; i++)
            crests[i] = assets.animatedSpriteFit("water_crest", "cycle", 192, 64, i);
    }

    static int surfaceColor(double x, double y, double waterDepth) {
        double depth = noise(x / 4.7, y / 4.7, 311);
        double cloud = noise(x / 1.3, y / 0.85, 733);
        double grain = noise(x * 23, y * 31, 199) - 0.5;
        double detail = noise(x * 4.3, y * 8.1, 571) - 0.5;
        double filaments = Math.sin(y * 95 + Math.sin(x * 12 + y * 7) * 2 + detail * 5);
        double light = depth * 16 + cloud * 9 + detail * 4 + grain * 3 + filaments * 1.7;
        double shelf = Math.max(0, Math.min(1, (3 - waterDepth) / 2));
        int r = (int) (22 + light + shelf * 33);
        int g = (int) (64 + light * 1.45 + shelf * 44);
        int b = (int) (91 + light * 1.55 + shelf * 25);
        return 0xff000000 | r << 16 | g << 8 | b;
    }

    public void draw(Graphics2D target, int wx, int wy, int px, int py, int tileSize, int frame,
                     String mapId, WeatherSystem weather, WeatherQuality quality) {
        Graphics2D g = (Graphics2D) target.create();
        g.clipRect(px, py, tileSize, tileSize);
        g.translate(px - wx * (double) tileSize, py - wy * (double) tileSize);
        g.scale(tileSize / 48.0, tileSize / 48.0);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double time = frame * (GameConfig.FPS_MS / 1000.0);
        double wave = weather.waterWave();
        int layers = switch (quality) {
            case HIGH -> 3;
            case BALANCED -> 2;
            case PERFORMANCE, LOW_SPEC -> 1;
        };
        // Include neighboring anchors: each tile clips the same uninterrupted world paths.
        for (int layer = 0; layer < layers; layer++) {
            double cellW = layer == 0 ? 43 : 29;
            double cellH = layer == 0 ? 19 : 13;
            int seed = mapId.hashCode() + layer * 9013;
            for (int cy = (int) Math.floor(wy * 48 / cellH) - 2;
                 cy <= (int) Math.floor((wy + 1) * 48 / cellH) + 2; cy++) {
                for (int cx = (int) Math.floor(wx * 48 / cellW) - 2;
                     cx <= (int) Math.floor((wx + 1) * 48 / cellW) + 2; cx++) {
                    double a = random(cx, cy, seed);
                    double b = random(cx, cy, seed + 41);
                    if (b < 0.23) continue;
                    double phase = time * (0.55 + a * 0.35) + b * Math.PI * 2;
                    double pulse = 0.5 + 0.5 * Math.sin(phase);
                    double visibility = pulse * pulse;
                    double x = cx * cellW + a * cellW + Math.sin(phase * 0.73) * (3 + wave * 6) * weather.windX();
                    double y = cy * cellH + b * cellH + Math.sin(phase) * (1.0 + wave * 4);
                    if (x + 38 < wx * 48 || x - 8 > (wx + 1) * 48
                            || y + 8 < wy * 48 || y - 8 > (wy + 1) * 48) continue;
                    double length = (7 + a * 23) * (layer == 0 ? 1 : 0.65);
                    double bend = Math.sin(phase + a * 6) * (1.0 + wave);
                    Path2D.Double path = new Path2D.Double();
                    path.moveTo(x, y);
                    path.curveTo(x + length * .32, y - 1.2 + bend,
                            x + length * .7, y + bend, x + length, y - 0.5 + weather.windUnitY() * wave * length * 0.2);
                    int alpha = (int) ((layer == 0 ? 74 : 45) * visibility * (0.7 + wave * 0.6));
                    g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.setColor(new Color(15, 57, 77, alpha / 2));
                    g.draw(path);
                    g.setStroke(new BasicStroke(layer == 0 ? 0.7f : 0.45f,
                            BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.setColor(new Color(166, 217, 221, alpha));
                    g.draw(path);
                    // Whitecaps emerge gradually with the existing weather transition.
                    double foam = weather.stormIntensity() * Math.pow(pulse, 5);
                    if (foam > 0.02 && a > 0.55) {
                        g.setColor(new Color(216, 238, 233, (int) (foam * 115)));
                        g.setStroke(new BasicStroke(1.1f));
                        g.draw(path);
                    }
                    if (layer == 0 && a > 0.82) {
                        g.setColor(new Color(219, 244, 239, (int) (visibility * visibility * 58)));
                        g.draw(new java.awt.geom.Line2D.Double(x + 3, y - .8, x + 5, y - .8));
                    }
                }
            }
        }
        drawCrests(g, wx, wy, time, mapId.hashCode(), weather, quality);
        g.dispose();
    }

    private void drawCrests(Graphics2D g, int wx, int wy, double time, int seed,
                            WeatherSystem weather, WeatherQuality quality) {
        double intensity = Math.max(0, Math.min(1, (weather.waterWave() - .18) / .77));
        double[] weights = {1, smoothRange(.12, .65, intensity), smoothRange(.55, 1, intensity)};
        // Fixed clocks prevent phase jumps while weather crossfades between wave populations.
        double[] periods = {9.5, 6.7, 4.6};
        int populations = quality == WeatherQuality.LOW_SPEC ? 2 : 3;
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        for (int layer = 0; layer < populations; layer++) {
            double strength = weights[layer];
            if (strength < .005) continue;
            double cellW = 108 - layer * 12, cellH = 64 - layer * 8;
            for (int cy = (int) Math.floor(wy * 48 / cellH) - 2;
                 cy <= (int) Math.floor((wy + 1) * 48 / cellH) + 2; cy++) {
                for (int cx = (int) Math.floor(wx * 48 / cellW) - 2;
                     cx <= (int) Math.floor((wx + 1) * 48 / cellW) + 2; cx++) {
                    double a = random(cx, cy, seed + layer * 1301);
                    double b = random(cx, cy, seed + layer * 1301 + 67);
                    double phase = (time / (periods[layer] + a * 2) + b) % 1;
                    double envelope = Math.sin(Math.PI * phase);
                    envelope *= envelope;
                    double travel = (phase - .5) * (25 + layer * 30);
                    double x = (cx + a) * cellW + travel * weather.windUnitX();
                    double y = (cy + b) * cellH + travel * (.4 + weather.windUnitY() * .4);
                    double width = (53 + layer * 23) * (.8 + a * .4);
                    double height = width * (.17 + layer * .025);
                    // Conservative bounds include the maximum .30 rad rotation and the
                    // asymmetric (-48..16) vertical anchor, avoiding off-tile raster work.
                    double extentX = width * .5 + height * .23;
                    double extentY = height * .75 + width * .15;
                    if (x + extentX < wx * 48 || x - extentX > (wx + 1) * 48
                            || y + extentY < wy * 48 || y - extentY > (wy + 1) * 48) continue;
                    // Calm swells use the unbroken crest poses; rough populations reach foam breakup.
                    double pose = phase * (layer == 0 ? 2.8 : 11);
                    int first = Math.min(10, (int) pose);
                    double blend = pose - first;
                    double alpha = strength * envelope * (layer == 0 ? .26 : layer == 1 ? .46 : .72);
                    Graphics2D crest = (Graphics2D) g.create();
                    crest.translate(x, y);
                    crest.rotate(weather.windUnitY() * .22 + (a - .5) * .16);
                    crest.scale(width / 192, height / 64);
                    crest.setComposite(AlphaComposite.SrcOver.derive((float) (alpha * (1 - blend))));
                    crest.drawImage(crests[first], -96, -48, null);
                    crest.setComposite(AlphaComposite.SrcOver.derive((float) (alpha * blend)));
                    crest.drawImage(crests[first + 1], -96, -48, null);
                    crest.dispose();
                }
            }
        }
    }

    private static double smoothRange(double low, double high, double value) {
        double t = Math.max(0, Math.min(1, (value - low) / (high - low)));
        return t * t * (3 - 2 * t);
    }

    private static double noise(double x, double y, int seed) {
        int ix = (int) Math.floor(x), iy = (int) Math.floor(y);
        double fx = x - ix, fy = y - iy;
        fx = fx * fx * (3 - 2 * fx); fy = fy * fy * (3 - 2 * fy);
        double top = random(ix, iy, seed) * (1 - fx) + random(ix + 1, iy, seed) * fx;
        double bottom = random(ix, iy + 1, seed) * (1 - fx) + random(ix + 1, iy + 1, seed) * fx;
        return top * (1 - fy) + bottom * fy;
    }

    private static double random(int x, int y, int seed) {
        int value = x * 374761393 + y * 668265263 + seed * 1274126177;
        value = (value ^ (value >>> 13)) * 1274126177;
        return ((value ^ (value >>> 16)) & 65535) / 65535.0;
    }
}
