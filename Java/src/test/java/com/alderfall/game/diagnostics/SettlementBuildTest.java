package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.nio.file.*;

/** Build-mode progression, terrain invalidation, protections and save compatibility. */
public final class SettlementBuildTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        GameState state = new GameState(GameConfig.load(Path.of("")));
        state.chooseClass("Knight");
        while (state.mode == GameMode.STORY_INTRO) state.advanceStoryIntro();
        WorldMap world = state.world;
        world.clearPlayerVillageCustomizations();
        world.setPlayerVillageStage(2);
        String map = WorldMap.PLAYER_VILLAGE_ID;
        state.currentMapId=map;
        AssetStore assets=new AssetStore(Path.of("assets"));
        AssetCatalog catalog=new AssetCatalog(Path.of("assets"));
        java.util.Set<String> identities=new java.util.HashSet<>();
        for (var plan:VillageManager.buildingPlans()) {
            check(plan.maxLevel()==6,"Six building tiers");
            for(int level=1;level<=6;level++) {
                check(assets.hasSprite(VillageManager.buildingLevelSprite(plan.style(),level)),"Missing building: "+plan.style()+" tier "+level);
                String sprite=VillageManager.buildingLevelSprite(plan.style(),level);
                check(identities.add(sprite),"Every building and tier must retain its own visual identity: "+sprite);
                check(catalog.findAsset(sprite).toString().replace('\\','/').contains("settlements/player_village/buildings/"),"Player building ownership");
                if(level<6)check(VillageManager.upgradeCost(plan.style(),level).gold()>0,"Paid upgrade");
            }
        }
        check(VillageManager.settlementStage(12).kind().equals("Metropolis"),"Metropolis milestone");
        for(char tile:"!$%&()w~BsvnbCG".toCharArray()) {
            check(VillageManager.tilePlan(tile).tile()==tile,"Terrain palette "+tile);
            check(assets.hasSprite(Terrain.assetName(tile)),"Terrain asset "+tile);
        }
        TilePoint point=null;
        for(int y=4;y<world.height(map)-4 && point==null;y++)for(int x=4;x<world.width(map)-4;x++) {
            if(world.canEditVillageHeight(x,y) && world.canSetPlayerVillageTile(x,y,'w')) {point=new TilePoint(x,y);break;}
        }
        check(point!=null,"Editable terrain");
        int x=point.x(),y=point.y();
        long revision=world.visualRevision(map);
        var previous=world.elevation(map);
        check(world.setPlayerVillageHeight(x,y,1.75),"Height edit");
        check(world.visualRevision(map)>revision && world.elevation(map)!=previous,"Cache invalidation");
        check(world.elevation(map).heightAt(x+.5,y+.5)==1.75,"Height used by collision/render field");
        check(!world.setPlayerVillageHeight(x,y,Double.NaN),"Reject nonfinite");
        check(!world.setPlayerVillageHeight(0,0,2),"Protect boundary");
        check(world.setPlayerVillageTile(x,y,'w'),"Place water");
        check(!world.setPlayerVillageHeight(x,y,2),"Protect water datum");
        check(world.setPlayerVillageTile(x,y,'~'),"Place shallow water");
        check(world.isPassable(map,x,y),"Shallow water walkable");
        check(world.setPlayerVillageTile(x,y,'!'),"Place new ground");
        check(world.setPlayerVillageHeight(x,y,2.75),"Quarter steps above two");
        check(world.elevation(map).heightAt(x+.5,y+.5)==2.75,"High quarter step");
        check(world.setPlayerVillageTile(x,y,')'),"Repaint raised ground");
        check(world.elevation(map).heightAt(x+.5,y+.5)==2.75,"Land paint preserves height");
        world.setPlayerVillageTile(x,y,'!');
        int shift=world.setPlayerVillageStage(3);
        x+=shift;y+=shift;
        check(world.playerVillageHeights().get(new TilePoint(x,y))==2.75,"Expansion shifts heights");
        CityBuilding building=null;
        for(int by=4;by<world.height(map)-6 && building==null;by++)for(int bx=4;bx<world.width(map)-6;bx++) {
            if(Math.abs(bx-x)<6 && Math.abs(by-y)<6)continue;
            building=world.placePlayerVillageBuilding("warehouse",bx,by);
            if(building!=null)break;
        }
        check(building!=null,"Building placement");
        for(int i=1;i<6;i++)check(world.upgradePlayerVillageBuilding(building),"Building upgrade "+i);
        check(!world.upgradePlayerVillageBuilding(building),"Tier cap");
        check(!world.setPlayerVillageHeight(building.x1(),building.y1(),2),"Protect foundation");
        Path saveRoot=Files.createTempDirectory(Files.createDirectories(Path.of("temp/settlement-identity")),"save-");
        SaveSystem saves=new SaveSystem(saveRoot);
        saves.save(state,"Settlement build test");
        String id=state.currentSaveId;
        world.resetPlayerVillageHeight(x,y);
        check(saves.load(state,id),"Save reload");
        check(state.world.playerVillageHeights().get(new TilePoint(x,y))==2.75,"Saved elevation");
        check(state.world.tileAt(map,x,y)=='!',"Saved ground");
        check(state.world.playerVillageBuildingLevels().containsValue(6),"Saved tier six");
        CityBuilding saved=state.world.playerVillageBuildings().get(0);
        check(state.buildingToolScore(saved)>=2,"Advanced tiers unlock shop quality");
        Actor worker=new Actor("Tier worker","npc_blacksmith","Knight",50,10,5,5);
        state.allies.add(worker);state.villageAllies.add(worker.name);
        state.villageBuildingAssignments.put(saved.key(),worker.name);
        state.villageStorage.put("wood",state.villageStorageCapacity());
        var production=GameState.class.getDeclaredMethod("tickVillageProduction");production.setAccessible(true);
        int gold=state.player.gold;
        production.invoke(state);
        check(state.player.gold==gold,"Unhoused workers cannot produce");
        CityBuilding house=null;
        for(int by=4;by<state.world.height(map)-6 && house==null;by++)for(int bx=4;bx<state.world.width(map)-6;bx++) {
            house=state.world.placePlayerVillageBuilding("house",bx,by);if(house!=null)break;
        }
        check(house!=null,"Housing fixture");
        production.invoke(state);
        check(state.player.gold-gold==VillageManager.tierRevenue(6),"Tier trade pays even when storage is full");
        System.out.println("SettlementBuildTest passed: six tiers, assets, terrain, protection, expansion, save/load");
    }
}
