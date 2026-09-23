package com.alderfall.game;

import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Files;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Exercises the real world renderer and its cadence/character-scale action routing. */
public final class GroundedGameRenderTest {
    public static void main(String[] args)throws Exception {
        SwingUtilities.invokeAndWait(()->{
            GamePanel panel=new GamePanel(Path.of("").toAbsolutePath());
            try {
                ((Timer)field("timer").get(panel)).stop();
                GameState state=(GameState)field("state").get(panel);
                state.chooseClass("Mage");while(state.mode==GameMode.STORY_INTRO)state.advanceStoryIntro();
                state.config.walkAnimationSpeed=1;state.zoom=100;
                panel.setSize(GameConfig.WIDTH,GameConfig.HEIGHT);
                field("freePlayerMapId").set(panel,state.currentMapId);
                field("playerFacingDx").setInt(panel,1);field("playerFacingDy").setInt(panel,0);
                field("freePlayerY").setDouble(panel,state.playerY+.5);
                BufferedImage image=new BufferedImage(GameConfig.WIDTH,GameConfig.HEIGHT,BufferedImage.TYPE_INT_RGB);
                var g=image.createGraphics();
                for(int i=0;i<4;i++) {
                    if(i==3)state.config.walkAnimationSpeed=1.5;
                    field("frame").setInt(panel,100+i);
                    field("freePlayerX").setDouble(panel,state.playerX+.5+i*.08);
                    panel.paint(g);
                }
                g.dispose();
                AssetStore store=(AssetStore)field("assets").get(panel);
                Field cache=AssetStore.class.getDeclaredField("groundedFrames");cache.setAccessible(true);
                String keys=((java.util.Map<?,?>)cache.get(store)).keySet().toString();
                if(!keys.contains("walk_grounded_10_66")||!keys.contains("walk_grounded_15_66"))throw new IllegalStateException("Game renderer did not use grounded profiles: "+keys);
                Files.createDirectories(Path.of("exports"));ImageIO.write(image,"png",Path.of("exports/grounded-movement-game.png").toFile());
                System.out.println("Game render passed: grounded default, actual actor scale and live cadence changes.");
            }catch(Exception e){throw new IllegalStateException(e);}finally{panel.shutdown();}
        });
    }
    private static Field field(String name)throws Exception{Field f=GamePanel.class.getDeclaredField(name);f.setAccessible(true);return f;}
}
