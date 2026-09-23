package com.alderfall.game;
import com.alderfall.game.map.MapArea;
import com.alderfall.game.map.WorldMap;
import java.nio.file.*;
import java.lang.reflect.Field;
import java.util.*;
public class SpriteUsageCheck {
    public static void main(String[] args) throws Exception {
        Path review=Path.of("../asset-review/2026-09-22-sprite-integrity");
        AssetStore assets=new AssetStore(Path.of("assets"));
        int required=0;
        for(String name:Files.readAllLines(review.resolve("required-runtime-stems.txt"))){
            if(!assets.hasSprite(name))throw new IllegalStateException("Required asset missing: "+name);required++;
        }
        Set<String> archived=new HashSet<>();
        try(var paths=Files.walk(review.resolve("unused/Java/assets"))){paths.filter(p->p.toString().endsWith(".png")).forEach(p->archived.add(p.getFileName().toString().replace(".png","")));}
        GameState state=new GameState(GameConfig.load(Path.of("").toAbsolutePath().normalize()));
        Field field=WorldMap.class.getDeclaredField("maps");field.setAccessible(true);
        Map<?,?> maps=(Map<?,?>)field.get(state.world);int props=0;
        Set<String> missing=new TreeSet<>();
        for(Object value:maps.values()){
            MapArea area=(MapArea)value;
            for(WorldProp prop:area.props){
                props++;if(archived.contains(prop.asset()))throw new IllegalStateException("Archived live prop: "+prop.asset()+" in "+area.id);
                if(!assets.hasSprite(prop.asset()))missing.add(prop.asset());
            }
        }
        for(String name:archived)if(assets.hasSprite(name))throw new IllegalStateException("Archived asset still indexed: "+name);
        String result="Required assets retained: "+required+"\nGenerated maps checked: "+maps.size()+"\nPlaced props checked: "+props+"\nArchived assets absent from runtime catalog: "+archived.size()+"\nUnresolved world prop stems: "+missing+"\n";
        Files.writeString(review.resolve("runtime-usage-verification.txt"),result);System.out.print(result);
    }
}
