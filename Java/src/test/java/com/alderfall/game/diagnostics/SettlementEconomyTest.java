package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.nio.file.*;
import java.util.*;

public final class SettlementEconomyTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static CityBuilding place(GameState s,String style){
        for(int y=3;y<s.world.height(WorldMap.PLAYER_VILLAGE_ID)-7;y++)for(int x=3;x<s.world.width(WorldMap.PLAYER_VILLAGE_ID)-7;x++){
            CityBuilding b=s.world.placePlayerVillageBuilding(style,x,y);if(b!=null)return b;
        }
        throw new AssertionError("No plot for "+style);
    }
    public static void main(String[] args)throws Exception{
        GameState s=new GameState(GameConfig.load(Path.of(".")));s.chooseClass("Knight");
        while(s.mode==GameMode.STORY_INTRO)s.advanceStoryIntro();
        s.world.clearPlayerVillageCustomizations();s.world.setPlayerVillageStage(3);
        s.currentMapId=WorldMap.PLAYER_VILLAGE_ID;s.mode=GameMode.VILLAGE;
        Actor a=new Actor("Artisan A","npc_blacksmith","Knight",50,10,5,5);
        Actor b=new Actor("Artisan B","npc_blacksmith","Knight",50,10,5,5);
        Actor c=new Actor("Artisan C","npc_blacksmith","Knight",50,10,5,5);
        s.allies.addAll(List.of(a,b,c));s.villageAllies.addAll(List.of(a.name,b.name,c.name));
        CityBuilding mine=place(s,"mine");s.activeVillageBuilding=mine;
        s.assignAllyToActiveBuilding(a.name);check(s.workersForBuilding(mine.key()).isEmpty(),"Housing required");
        CityBuilding house=place(s,"house");check(s.villageHousingCapacity()==2,"Two camp beds");
        s.assignAllyToActiveBuilding(a.name);s.assignAllyToActiveBuilding(b.name);
        check(s.workersForBuilding(mine.key()).equals(List.of(a.name)),"Tier one has one slot");
        s.world.upgradePlayerVillageBuilding(mine);s.world.upgradePlayerVillageBuilding(mine);
        s.assignAllyToActiveBuilding(b.name);check(s.workersForBuilding(mine.key()).size()==2,"Tier three has two slots");
        check(s.housingForAlly(c.name)==null,"Surplus resident remains unhoused");
        var forecast=s.villageForecast(mine);check(forecast.resources().get("stone")>0,"Mine produces stone");
        a.professionXp.put(Profession.MINING.id(),Profession.xpForLevel(8));
        check(s.villageForecast(mine).resources().get("stone")>forecast.resources().get("stone"),"Skill improves output");
        double rareBefore=s.villageForecast(mine).resources().get("iron_ore");
        s.villageBuildingDecorations.put(mine.key(),new ArrayList<>(List.of("interior_anvil","interior_workbench")));
        check(s.villageForecast(mine).resources().getOrDefault("silver_ore",0.0)>0,"Skill and tools unlock rarer ore");
        check(s.villageForecast(mine).resources().entrySet().stream().filter(e->!e.getKey().equals("stone")).mapToDouble(Map.Entry::getValue).sum()>rareBefore,"Tools improve rare yields");
        var tick=GameState.class.getDeclaredMethod("tickVillageProduction");tick.setAccessible(true);s.villageStorage.clear();
        tick.invoke(s);check(s.villageStorage.getOrDefault("stone",0)>0,"Staffed mine stores output");
        s.world.removePlayerVillageBuilding(house);
        check(s.villageForecast(mine).resources().isEmpty(),"Loss of housing pauses production");
        house=place(s,"house");s.world.upgradePlayerVillageBuilding(house);
        check(s.villageHousingCapacity()==4 && s.housingForAlly(c.name)!=null,"House tier expands beds");
        WorldProp prop=null;
        for(int y=3;y<30&&prop==null;y++)for(int x=3;x<30;x++)if(s.world.addPlayerVillageProp(x,y,"city_prop_refresh_barrel",40,12,-8)){prop=s.world.playerVillagePropAt(x,y);break;}
        check(prop!=null,"Offset prop placement");
        Path dir=Files.createTempDirectory(Files.createDirectories(Path.of("temp/settlement-identity")),"economy-save-");
        SaveSystem saves=new SaveSystem(dir);saves.save(s,"Settlement economy");String id=s.currentSaveId;
        check(saves.load(s,id),"Load save");
        check(s.workersForBuilding(mine.key()).size()==2,"Multiple slots persist");
        check(s.world.playerVillageProps().stream().anyMatch(p->p.offsetX()==12&&p.offsetY()==-8),"Prop offsets persist");
        int shift=s.world.setPlayerVillageStage(4);
        check(shift>0&&s.world.playerVillageProps().stream().anyMatch(p->p.offsetX()==12&&p.offsetY()==-8),"Expansion preserves offsets");
        for(var plan:VillageManager.buildingPlans()){
            int gold=plan.cost().gold();
            for(int tier=1;tier<6;tier++){int next=VillageManager.upgradeCost(plan.style(),tier).gold();check(next>gold,"Increasing costs");gold=next;}
        }
        System.out.println("SettlementEconomyTest passed: housing, slots, skill/tools, production, costs and save compatibility");
    }
}
