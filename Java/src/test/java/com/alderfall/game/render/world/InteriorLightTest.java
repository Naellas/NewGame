package com.alderfall.game.render.world;

import com.alderfall.game.map.WorldMap;
import java.awt.Shape;

/** Wall, doorway, zoom and geometry invalidation regressions for room lighting. */
public final class InteriorLightTest {
    public static void main(String[] args) {
        WorldMap world = new WorldMap(42);
        String id = world.createEditorMap("editor_light_test", "Light test", "interior", 12, 10);
        for (int y = 1; y < 9; y++) world.area(id).tiles[y][6] = 'o';
        InteriorLightField field = new InteriorLightField();
        for (int tile : new int[]{24, 48, 72, 96}) {
            field.update(world, id, tile);
            Shape blocked = field.visible(world, tile * 4.5, tile * 4.5, tile * 7, tile);
            require(blocked.contains(tile * 5.5, tile * 4.5), "Light failed on emitter side");
            require(!blocked.contains(tile * 7.5, tile * 4.5), "Light crossed solid wall");
            require(!field.floor().contains(tile * 6.5, tile * 4.5), "Wall accepted floor shadow");
            require(blocked == field.visible(world, tile * 4.5, tile * 4.5, tile * 7, tile), "Visibility not cached");
            world.area(id).tiles[4][6] = 'e';
            field.update(world, id, tile);
            Shape open = field.visible(world, tile * 4.5, tile * 4.5, tile * 7, tile);
            require(open.contains(tile * 7.5, tile * 4.5), "Door failed to transmit light");
            require(open != blocked, "Wall edit failed to invalidate light cache");
            world.area(id).tiles[4][6] = 'o';
        }
        require(!WorldPropRenderer.isFireProp("interior_cooking_station"), "Cold counter emits fire");
        require(!WorldPropRenderer.isFireProp("deco_marsh_firefly_reeds"), "Fireflies emit flame");
        require(WorldPropRenderer.isFireProp("interior_hearth_pot"), "Hearth missing embers");
        require(WorldPropRenderer.isFireProp("interior_fireplace"), "Fireplace missing fire classification");
        require(!WorldPropRenderer.isFireProp("interior_firewood_basket"), "Stored wood must not emit flame");
        System.out.println("Interior lighting checks passed at 4 tile scales.");
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
