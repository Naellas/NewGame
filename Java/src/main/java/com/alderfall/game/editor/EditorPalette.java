package com.alderfall.game.editor;

import com.alderfall.game.*;
import java.util.*;

/** Every runtime image ID is available; grouping never acts as an inclusion filter. */
public final class EditorPalette {
    public record Entry(String label,String category,String asset,String style,int size,char tile) {
        @Override public String toString() { return label; }
        public boolean building() { return !style.isEmpty(); }
        public boolean terrain() { return tile != 0; }
    }
    public static List<Entry> entries(AssetStore assets) {
        List<Entry> result=new ArrayList<>();
        for (char tile:MapDocument.TILES.toCharArray()) result.add(new Entry(Terrain.name(tile)+" ["+tile+"]","Terrain paint",Terrain.assetName(tile),"",48,tile));
        for (var b:VillageManager.buildingPlans()) result.add(new Entry(b.label(),"Buildings",VillageManager.primaryBuildingSprite(b.style()),b.style(),180,(char)0));
        Map<String,VillageManager.PlaceableAsset> curated=new LinkedHashMap<>();
        VillageManager.outdoorAssets().forEach(p -> curated.putIfAbsent(p.asset(),p));
        VillageManager.interiorAssets().forEach(p -> curated.putIfAbsent(p.asset(),p));
        Set<String> names=new TreeSet<>(assets.assetNames());
        names.addAll(curated.keySet()); // Includes virtual directional furniture and tinted-stall IDs.
        for (String name:names) {
            String source=assets.assetRelativePath(name);
            var p=curated.get(name);
            result.add(new Entry(p==null?title(name):p.label(),category(name,source),name,"",
                    p==null?defaultSize(name,source):p.size(),(char)0));
        }
        return List.copyOf(result);
    }

    private static String category(String name,String source) {
        String family=family(name,source);
        if(name.endsWith("_anim")||source.contains("/animations/"))return "Animation poses / "+family;
        if(name.contains("atlas")||name.contains("sheet"))return "Atlases and sheets / "+family;
        return family;
    }
    private static String family(String name,String source) {
        String buildingRoot="environments/settlements/city/buildings/";
        if(source.startsWith(buildingRoot))return "Building sprites / "+title(segment(source.substring(buildingRoot.length())));
        if(source.startsWith("characters/monsters/"))return "Fauna and monsters";
        if(source.startsWith("characters/player/"))return "Player characters";
        if(source.startsWith("characters/companions/"))return "Companions";
        if(source.startsWith("characters/npcs/"))return "NPCs / "+title(segment(source.substring("characters/npcs/".length())));
        if(source.startsWith("characters/"))return "Characters / Shared";
        if(source.startsWith("items/"))return "Items / "+title(segment(source.substring("items/".length())));
        if(source.startsWith("effects/"))return "Effects";
        if(source.startsWith("environments/interiors/"))return "Interiors and furniture";
        if(source.startsWith("environments/props/nature/"))return "Flora and nature";
        if(source.contains("/biomes/")) {
            String biome=segment(source.substring(source.indexOf("/biomes/")+8));
            return (name.startsWith("deco_")?"Flora and nature / ":"Terrain sprites / ")+title(biome);
        }
        if(source.startsWith("environments/settlements/player_village/"))return "Village sprites";
        if(source.startsWith("environments/settlements/"))return "Settlement scenery";
        if(source.startsWith("environments/terrain/"))return "Terrain sprites / "+title(segment(source.substring("environments/terrain/".length())));
        if(source.startsWith("environments/locations/"))return "Location scenery";
        if(source.startsWith("environments/battle/"))return "Battle backgrounds";
        return "Other sprites";
    }
    private static String segment(String path) {
        int slash=path.indexOf('/');return slash<0?"shared":path.substring(0,slash);
    }
    private static int defaultSize(String name,String source) {
        if(source.contains("/buildings/"))return 180;
        if(source.startsWith("characters/"))return 64;
        if(source.startsWith("items/"))return 32;
        if(name.contains("tree")||name.contains("pine"))return 96;
        return 48;
    }
    private static String title(String id) {
        String text=id.replace('_',' ');return text.isEmpty()?"Other":Character.toUpperCase(text.charAt(0))+text.substring(1);
    }
}
