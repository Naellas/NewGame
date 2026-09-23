package com.alderfall.game;

/** Maximum visible dimensions at base zoom; tightly trimmed sprites keep their aspect ratios. */
public final class SettlementSpriteScale {
    private SettlementSpriteScale() { }
    public static boolean matches(String asset) { return asset.startsWith("city_prop_refresh_"); }
    public static int size(String asset) {
        return switch (asset.replace("city_prop_refresh_", "")) {
            case "lamp_right", "lamp_left", "lamp_iron" -> 90;
            case "stall_blue", "stall_red", "stall_green", "stall_canvas" -> 78;
            case "bench" -> 42;
            case "cart" -> 48;
            case "planter_cypress" -> 44;
            case "planter_herbs", "planter_pink" -> 34;
            case "planter_white", "barrel" -> 30;
            case "planter_blue", "pottery" -> 32;
            case "crate_green", "crate_red" -> 28;
            default -> 26;
        };
    }
}
