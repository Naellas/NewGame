package com.alderfall.game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.List;
import javax.imageio.ImageIO;

/** Assets, role constraints, indoor culture inheritance, and real quest interaction/turn-in. */
public final class RegionalNpcTest {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        AssetStore assets = new AssetStore(Path.of("assets"));
        GameState state = new GameState(GameConfig.load(Path.of("")));
        state.chooseClass("Mage");
        while (state.mode == GameMode.STORY_INTRO) state.advanceStoryIntro();
        int residents=0, weekly=0;
        java.util.ArrayList<Npc> roster = new java.util.ArrayList<>(GameData.NPCS);
        for (var site : state.world.settlementSites()) roster.addAll(state.world.npcs(site.id()));
        for (Npc npc : roster) {
            check(assets.hasSprite(NpcIdentity.portrait(npc)), "Missing assigned portrait: " + npc.name());
            if (!NpcIdentity.regional(npc)) continue;
            residents++;
            String stem=NpcIdentity.appearance(npc);
            check(assets.hasSprite(stem+"_portrait"), "Missing portrait: "+npc.name()+" "+stem);
            for(String direction:List.of("down","left","right","up"))
                check(assets.hasSprite(stem+"_model_"+direction), "Missing direction "+stem+" "+direction);
            for(int block=0;block<4;block++) {
                Quest q=RegionalNpcQuests.create(npc,"weekly_audit_"+weekly++,block,state.world);
                check(assets.hasSprite(q.objectiveAsset),"Missing quest asset "+q.objectiveAsset);
                if(q.monsterKey!=null&&!q.monsterKey.isBlank())check(GameData.MONSTERS.containsKey(q.monsterKey),"Unknown monster");
                check(RegionalNpcQuests.story(q)!=null,"Missing occupational conversation");
                switch(NpcIdentity.role(npc)) {
                    case FARMER -> check(q.objectiveLocationKind.equals("farmland") && q.objectiveKind==Quest.ObjectiveKind.GATHER,"Farmer assigned unrelated work");
                    case MINER -> check(q.target.contains("Ore") || q.target.contains("Coal"),"Miner assigned unrelated work");
                    case TRADER -> check(q.target.contains("Trade") || q.target.contains("Bandit"),"Trader assigned unrelated work");
                    case NOBLE -> check(q.objectiveKind==Quest.ObjectiveKind.SEARCH,"Magistrate assigned unrelated work");
                    default -> { }
                }
            }
        }
        check(NpcIdentity.region(new Npc("house_town_greyharbor_4_9","Host","npc_citizen_man",1,1,List.of(),null,null)).equals("marsh"),"Harbor interior lost its coastal culture");
        for(String id:List.of("cairnvale_ore_assay","briarbridge_forged_seal")) {
            Quest q=state.quests.get(id);
            Npc giver=GameData.NPCS.stream().filter(n->id.equals(n.questId())).findFirst().orElseThrow();
            state.activeNpc=giver;state.currentMapId=giver.mapId();
            check(state.handleActiveNpcQuestAction() && q.accepted,"Could not accept "+id);
            var interact=GameState.class.getDeclaredMethod("interactQuestObjective",GameState.QuestObjective.class);interact.setAccessible(true);
            int interactions=0;
            while(!q.ready() && interactions++<10) {
                var objective=state.activeQuestObjectives().stream().filter(o->o.questId().equals(id)).findFirst().orElseThrow();
                boolean accessible=state.world.isPassable(objective.mapId(),objective.x(),objective.y());
                for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)accessible|=state.world.isPassable(objective.mapId(),objective.x()+dx,objective.y()+dy);
                check(accessible,"Unreachable quest interaction "+id+" "+objective);
                state.currentMapId=objective.mapId();state.playerX=objective.x();state.playerY=objective.y();
                interact.invoke(state,objective);
            }
            check(q.ready(),"Quest did not become ready: "+id);
            state.currentMapId=giver.mapId();state.activeNpc=giver;
            state.handleActiveNpcQuestAction();check(q.completed,"Quest failed turn-in: "+id);
        }
        preview(assets);
        System.out.println("Regional NPC checks passed: "+residents+" residents, "+weekly+" occupational requests, both authored quests completed.");
    }

    private static void preview(AssetStore assets) throws Exception {
        String[] regions={"temperate","north","desert","marsh","highland"};
        String[] roles={"farmer_m","farmer_f","miner_m","trader_f","artisan_f","scholar_m","guard_f","healer_m","noble_m"};
        BufferedImage image=new BufferedImage(1250,roles.length*175+40,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();g.setColor(new Color(22,28,34));g.fillRect(0,0,image.getWidth(),image.getHeight());
        for(int c=0;c<regions.length;c++) {
            g.setColor(new Color(226,208,169));g.setFont(new Font("SansSerif",Font.BOLD,16));g.drawString(regions[c].toUpperCase(),c*250+18,25);
            for(int r=0;r<roles.length;r++) {
                int x=c*250,y=40+r*175;String stem="npc_regional_"+regions[c]+"_"+roles[r];
                g.setColor(new Color(44,52,59));g.drawRect(x+4,y+4,242,167);
                g.drawImage(assets.spriteFit(stem+"_portrait",112,128),x+8,y+7,null);
                g.drawImage(assets.spriteFit(stem+"_model_down",65,118),x+125,y+15,null);
                g.drawImage(assets.spriteFit(stem+"_model_right",52,108),x+191,y+25,null);
                g.setColor(Color.LIGHT_GRAY);g.setFont(new Font("SansSerif",Font.PLAIN,12));g.drawString(roles[r],x+12,y+156);
            }
        }
        g.dispose();Path output=Path.of("../asset-review/regional-npcs");Files.createDirectories(output);
        ImageIO.write(image,"png",output.resolve("regional-portrait-review.png").toFile());
    }
}
