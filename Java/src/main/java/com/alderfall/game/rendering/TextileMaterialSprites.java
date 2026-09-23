package com.alderfall.game;

import java.awt.image.BufferedImage;
import java.util.List;

/** Individual inventory sprites backed by the generated four-column, three-row atlas. */
final class TextileMaterialSprites {
    static final String ATLAS = "tailoring_leather_materials_atlas";
    static final List<String> NAMES = List.of(
            "material_linen_cloth",
            "material_wool_cloth",
            "material_silk_cloth",
            "material_moonweave_cloth",
            "material_starweave_cloth",
            "material_tanned_leather",
            "material_hardened_leather",
            "material_reinforced_leather",
            "material_frosthide_leather",
            "material_dragonscale_leather",
            "material_spider_silk",
            "material_thick_hide");

    private TextileMaterialSprites() {}

    static BufferedImage extract(String name, BufferedImage atlas) {
        int index = NAMES.indexOf(name);
        if (index < 0) throw new IllegalArgumentException(name);
        int x = index % 4 * atlas.getWidth() / 4;
        int y = index / 4 * atlas.getHeight() / 3;
        int right = (index % 4 + 1) * atlas.getWidth() / 4;
        int bottom = (index / 4 + 1) * atlas.getHeight() / 3;
        return atlas.getSubimage(x, y, right - x, bottom - y);
    }
}
