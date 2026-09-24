package com.alderfall.game;

import java.nio.file.Path;
import java.util.List;

public final class ModularFurnitureTest {
    public static void main(String[] args) {
        AssetStore assets = new AssetStore(Path.of("assets"));
        for (var family : DirectionalSeating.FAMILIES) for (String direction : DirectionalSeating.DIRECTIONS) {
            String asset = family.asset(direction);
            require(assets.hasSprite(asset), "Missing cardinal seating: " + asset);
            var catalog = family.interior() ? VillageManager.interiorAssets() : VillageManager.outdoorAssets();
            require(catalog.stream().anyMatch(p -> p.asset().equals(asset)), "Missing placement choice: " + asset);
            var image = assets.spriteFit(asset, 72, 96);
            int visible = 0, empty = 0;
            for (int y=0;y<96;y++) for (int x=0;x<72;x++) {
                int alpha = image.getRGB(x,y) >>> 24;
                if (alpha > 128) visible++; if (alpha == 0) empty++;
            }
            require(visible > 100 && empty > 100, "Invalid alpha sprite: " + asset);
        }
        GameState state = new GameState(GameConfig.load(Path.of("")));
        String map = state.world.createEditorMap("editor_modular_test", "Modular test", "village", 24, 24);
        var area = state.world.area(map); area.fillTiles(0,0,23,23,'g');
        var left = new WorldProp(4,4,"market_stall_canvas",48);
        var middle = new WorldProp(6,4,"market_stall_blue",48);
        var right = new WorldProp(8,4,"market_stall_green",48);
        var run = List.of(left,middle,right);
        require(ConnectedFurniture.connections(left,run)==2 && ConnectedFurniture.connections(middle,run)==3
                && ConnectedFurniture.connections(right,run)==1, "Mixed-color market joins broken");
        require(state.world.addEditorProp(map,4,4,left.asset(),48), "Market placement failed");
        require(!state.world.addEditorProp(map,5,4,"village_prop_bench",48), "Market second tile accepts overlap");
        require(state.world.editorPropAt(map,5,4).asset().equals(left.asset()), "Cannot select market from second tile");
        require(PropCollision.footprint(state.world,map,left).width==2, "Market collision too narrow");
        var sprites = new ModularFences.Sprites();
        int[][] steps={{0,-1},{0,1},{-1,0},{1,0}};
        for (String asset : List.of("village_fence_auto","location_farmland_fence","location_graveyard_iron_fence")) {
            for (int mask=0;mask<16;mask++) {
                for (var p : new java.util.ArrayList<>(area.propsInBounds(11,11,14,14))) area.removeProp(p);
                var center = new WorldProp(12,12,asset,48); area.addProp(center);
                for (int bit=0;bit<4;bit++) if ((mask & 1<<bit)!=0)
                    area.addProp(new WorldProp(12+steps[bit][0],12+steps[bit][1],asset,48));
                require(ModularFences.connections(state.world,map,center)==mask,"Fence junction mismatch");
                require(sprites.image(assets,asset,48,mask)!=null,"Missing fence junction image");
            }
        }
        area.addProp(new WorldProp(18,18,"location_farmland_fence",48));
        area.addProp(new WorldProp(19,18,"location_graveyard_iron_fence",48));
        require(ModularFences.connections(state.world,map,area.propAt(18,18))==0,"Mixed fence families joined");
        System.out.println("Modular furniture passed: 28 cardinal seating choices, mixed market joins, two-tile placement and 48 fence masks.");
    }
    private static void require(boolean ok,String message) { if (!ok) throw new AssertionError(message); }
}
