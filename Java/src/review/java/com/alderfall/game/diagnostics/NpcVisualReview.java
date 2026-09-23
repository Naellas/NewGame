package com.alderfall.game;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Capture actual dialogue cards and regional town models without opening a window. */
public final class NpcVisualReview {
    public static void main(String[] args) throws Exception {
        Path output=Path.of("../asset-review/regional-npcs");Files.createDirectories(output);
        SwingUtilities.invokeAndWait(()->{
            try {
                GamePanel panel=new GamePanel(Path.of("").toAbsolutePath());
                var timer=GamePanel.class.getDeclaredField("timer");timer.setAccessible(true);((Timer)timer.get(panel)).stop();
                var field=GamePanel.class.getDeclaredField("state");field.setAccessible(true);GameState state=(GameState)field.get(panel);
                state.chooseClass("Mage");while(state.mode==GameMode.STORY_INTRO)state.advanceStoryIntro();
                panel.setSize(1440,900);
                for(String name:new String[]{"Farmer Joss","Miner Dorran","Magistrate Halven","Spice Peddler Rafi","Goatkeeper Una"}) {
                    Npc npc=GameData.NPCS.stream().filter(n->n.name().equals(name)).findFirst().orElseThrow();
                    state.currentMapId=npc.mapId();TilePoint home=state.world.npcHome(npc);state.playerX=home.x();state.playerY=home.y()+1;
                    state.mode=GameMode.EXPLORE;
                    TilePoint position=state.npcPosition(npc);state.playerX=position.x();state.playerY=position.y();
                    if(!state.talkToNpc(npc))throw new IllegalStateException("Cannot talk to "+name);
                    state.revealActiveDialogueLineInstantly();
                    BufferedImage image=new BufferedImage(1440,900,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();panel.paint(g);g.dispose();
                    ImageIO.write(image,"png",output.resolve(name.toLowerCase().replace(' ','-')+"-dialogue.png").toFile());
                }
            }catch(Exception e){throw new RuntimeException(e);}
        });
        System.out.println("NPC dialogue review saved to "+output);System.exit(0);
    }
}
