package com.alderfall.game;

import java.awt.image.BufferedImage;
import java.util.Set;

public final class ModularMarket {
    private ModularMarket() { }
    public static final String BASE = "market_stall_canvas";
    public static final Set<String> ASSETS = Set.of(BASE, "market_stall_blue", "market_stall_green", "market_stall_gold");
    public static boolean supports(String asset) { return ASSETS.contains(asset); }
    public static BufferedImage tint(BufferedImage source, String asset) {
        var image = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int color = asset.endsWith("blue") ? 0x477da5 : asset.endsWith("green") ? 0x67864a : 0xc39345;
        for (int y = 0; y < source.getHeight(); y++) for (int x = 0; x < source.getWidth(); x++) {
            int pixel = source.getRGB(x, y), r = pixel >> 16 & 255, g = pixel >> 8 & 255, b = pixel & 255;
            if (y < source.getHeight() * .46 && r > g * 1.3 && r > b * 1.1) {
                double light = Math.min(1.5, r / 145.0);
                pixel = (pixel & 0xff000000) | (Math.min(255, (int) ((color >> 16 & 255) * light)) << 16)
                        | (Math.min(255, (int) ((color >> 8 & 255) * light)) << 8) | Math.min(255, (int) ((color & 255) * light));
            }
            image.setRGB(x, y, pixel);
        }
        return image;
    }
}
