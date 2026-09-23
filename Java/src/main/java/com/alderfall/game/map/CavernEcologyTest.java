package com.alderfall.game.map;

import com.alderfall.game.*;
import java.nio.file.Path;
import java.util.*;

/** Regional deposits are useful, sparse, reachable and deterministic. */
public final class CavernEcologyTest {
    public static void main(String[] args) throws Exception {
        AssetStore assets = new AssetStore(Path.of("assets"));
        int floors = 0;
        int minNodes = 100, maxNodes = 0;
        String[] regions = {"crownlands", "highwall", "belltower", "sanctum"};
        char[] exteriors = {'m','n','v','s'};
        for (String theme : List.of("cave", "goblin_camp")) for (int biome = 0; biome < 4; biome++)
            for (int seed = 0; seed < 20; seed++) for (int floor = 1; floor <= 4; floor++) {
                var profile = DungeonGenerator.profile("ecology-"+seed, "Ecology", theme, regions[biome], exteriors[biome], seed);
                var plan = DungeonGenerator.generate(profile, floor, 4);
                var repeated = DungeonGenerator.generate(profile, floor, 4);
                require(plan.props().equals(repeated.props()) && plan.encounters().equals(repeated.encounters()), "Unstable generation");
                var nodes = CavernStyle.resources(CavernStyle.biome(regions[biome], exteriors[biome]), floor);
                var occupied = new HashSet<TilePoint>();
                int count = 0;
                for (var prop : plan.props()) {
                    var point = new TilePoint(prop.x(),prop.y());
                    require(occupied.add(point), "Stacked prop");
                    require(assets.hasSprite(prop.asset()), "Missing art: " + prop.asset());
                    if (!nodes.contains(prop.asset())) continue;
                    count++;
                    require(!plan.criticalRoute().contains(point), "Resource on route");
                    require(plan.encounters().stream().noneMatch(e -> e.x()==point.x() && e.y()==point.y()), "Resource on monster");
                    require(distance(point, plan.up()) > 1 && (plan.down()==null || distance(point,plan.down())>1), "Resource at stair");
                    require(CraftingSystem.isDepletableResourceNode(prop.asset()), "Unlimited resource node: " + prop.asset());
                }
                require(count >= 3 && count <= 12, "Unexpected resource density " + count);
                minNodes=Math.min(minNodes,count); maxNodes=Math.max(maxNodes,count); floors++;
            }

        GameState state = new GameState(GameConfig.load(Path.of("").toAbsolutePath().normalize()));
        var choose = GameState.class.getDeclaredMethod("chooseDungeonRoamer", String.class, Random.class);
        choose.setAccessible(true);
        int harvested = 0;
        for (var marker : state.world.adventureMarkers()) {
            var context=state.world.dungeonContext(marker.mapId());
            if (context==null || !CavernStyle.natural(context.theme())) continue;
            String biome=CavernStyle.biome(context.region(),context.exterior());
            Set<String> monsters=new HashSet<>();
            Random random=new Random(9182);
            for (int roll=0;roll<100;roll++) {
                String key=(String) choose.invoke(state,marker.mapId(),random);
                require(GameData.MONSTERS.containsKey(key), "Unknown monster " + key);
                if (context.theme().equals("cave") && biome.equals("sand"))
                    require(!key.contains("frost") && !key.contains("ice_"), "Winter monster in sandstone cavern");
                monsters.add(key);
            }
            require(monsters.size()>=4,"Insufficient monster variety");
            for (WorldProp prop : state.world.props(marker.mapId())) {
                if (!CavernStyle.resources(biome,1).contains(prop.asset())) continue;
                CraftingSystem crafting = new CraftingSystem();
                var candidate=crafting.findGatherTargetAt(state.world,marker.mapId(),prop.x(),prop.y());
                require(candidate!=null && candidate.asset().equals(prop.asset()), "Resource cannot be targeted");
                Actor actor=new Actor("Miner", "player", "Warrior",100,30,8,5);
                crafting.beginGather(actor,List.of(),candidate,new Random(42));
                for(int tick=0;tick<1000;tick++) crafting.tick(actor);
                require(!crafting.lastCompletedOutput().isEmpty(), "Empty resource yield");
                crafting.lastCompletedOutput().forEach((key,amount) -> require(amount>0 && CraftingSystem.CRAFTING_ITEMS.containsKey(key), "Invalid yield " + key));
                harvested++;
            }
        }
        require(harvested>=15, "Too few harvestable nodes in real caverns");
        System.out.println("Cavern ecology passed: " + floors + " floors, " + minNodes + "-" + maxNodes
                + " resource nodes/floor; " + harvested + " real nodes targeted and harvested; regional monster variety verified.");
    }
    private static int distance(TilePoint a, TilePoint b) { return Math.abs(a.x()-b.x())+Math.abs(a.y()-b.y()); }
    private static void require(boolean ok,String message) { if(!ok) throw new AssertionError(message); }
}
