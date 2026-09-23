package com.alderfall.game.map;

import com.alderfall.game.*;
import java.nio.file.Path;
import java.util.*;

public final class BuiltDungeonEcologyTest {
    public static void main(String[] args) throws Exception {
        AssetStore assets = new AssetStore(Path.of("assets"));
        int floors=0, nodes=0;
        for(String theme : List.of("crypt","prison","sewer","bandit_camp","abandoned_castle"))
            for(int seed=0; seed<20; seed++) for(int floor=1; floor<=4; floor++) {
                var profile=DungeonGenerator.profile("built-"+seed,"Built",theme,"highwall",'n',seed);
                var plan=DungeonGenerator.generate(profile,floor,4);
                var repeat=DungeonGenerator.generate(profile,floor,4);
                require(plan.props().equals(repeat.props()) && plan.encounters().equals(repeat.encounters()),"Unstable population");
                var resources=BuiltDungeonStyle.resources(theme,"highwall",'n',floor);
                int count=0;
                for(var prop:plan.props()) {
                    require(assets.hasSprite(prop.asset()),"Missing prop "+prop.asset());
                    if(!resources.contains(prop.asset())) continue;
                    count++; var point=new TilePoint(prop.x(),prop.y());
                    require(!plan.criticalRoute().contains(point),"Deposit blocks route");
                    require(plan.encounters().stream().noneMatch(e->e.x()==prop.x() && e.y()==prop.y()),"Deposit overlaps monster");
                    require(CraftingSystem.isDepletableResourceNode(prop.asset()),"Deposit cannot deplete");
                }
                require(count==6,"Expected six deposits: "+theme+" "+seed+" "+floor+" got "+count);
                floors++; nodes+=count;
            }
        GameState state=new GameState(GameConfig.load(Path.of("").toAbsolutePath().normalize()));
        var choose=GameState.class.getDeclaredMethod("chooseDungeonRoamer",String.class,String.class,Random.class);
        choose.setAccessible(true);
        int harvested=0;
        for(var marker:state.world.adventureMarkers()) {
            var context=state.world.dungeonContext(marker.mapId());
            if(context==null || !BuiltDungeonStyle.supports(context.theme())) continue;
            Set<String> variety=new HashSet<>();
            Random rolls=new Random(251);
            for(int i=0;i<100;i++) {
                String monster=(String)choose.invoke(state,marker.mapId(),"guard",rolls);
                require(GameData.MONSTERS.containsKey(monster),"Unknown dungeon monster "+monster);
                variety.add(monster);
            }
            require(variety.size()>=4,"Dungeon monster variety too narrow");
            var resources=BuiltDungeonStyle.resources(context.theme(),context.region(),context.exterior(),context.floor());
            for(WorldProp prop:state.world.props(marker.mapId())) {
                if(!resources.contains(prop.asset())) continue;
                var crafting=new CraftingSystem();
                var candidate=crafting.findGatherTargetAt(state.world,marker.mapId(),prop.x(),prop.y());
                require(candidate!=null && candidate.asset().equals(prop.asset()),"Cannot target deposit");
                Actor actor=new Actor("Salvager","player","Warrior",100,30,8,5);
                crafting.beginGather(actor,List.of(),candidate,new Random(42));
                for(int i=0;i<1000;i++) crafting.tick(actor);
                require(!crafting.lastCompletedOutput().isEmpty(),"No resource output");
                harvested++;
            }
        }
        require(harvested>=30,"Missing world resources");
        System.out.println("Built dungeon ecology passed: "+floors+" floors, "+nodes+" deposits; "+harvested+" real nodes harvested.");
    }
    private static void require(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
}
