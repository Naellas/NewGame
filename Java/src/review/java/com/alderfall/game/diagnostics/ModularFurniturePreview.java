package com.alderfall.game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.lang.reflect.Field;
import com.alderfall.game.camera.CameraController;

public final class ModularFurniturePreview {
    public static void main(String[] args) throws Exception {
        Path output = Path.of(args.length>0 ? args[0] : "temp/modular-marketplace/captures"); Files.createDirectories(output);
        AssetStore assets = new AssetStore(Path.of("assets"));
        BufferedImage sheet = new BufferedImage(1000,1000,BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics(); g.setColor(new Color(47,54,49));g.fillRect(0,0,1000,1000);
        g.setFont(new Font("SansSerif",Font.BOLD,18));g.setColor(Color.WHITE);
        for (int d=0;d<4;d++) g.drawString(DirectionalSeating.DIRECTIONS.get(d),250+d*180,35);
        for (int row=0;row<DirectionalSeating.FAMILIES.size();row++) {
            var f=DirectionalSeating.FAMILIES.get(row);g.setColor(Color.WHITE);g.drawString(f.label(),15,110+row*125);
            for (int d=0;d<4;d++) g.drawImage(assets.spriteFit(f.asset(DirectionalSeating.DIRECTIONS.get(d)),112,104),225+d*180,50+row*125,null);
        }
        g.dispose();ImageIO.write(sheet,"png",output.resolve("seating-directions.png").toFile());
        SwingUtilities.invokeAndWait(() -> {
            GamePanel panel=new GamePanel(Path.of("").toAbsolutePath());
            try {
                ((Timer)field("timer").get(panel)).stop();GameState state=(GameState)field("state").get(panel);
                state.chooseClass("Mage");while(state.mode==GameMode.STORY_INTRO)state.advanceStoryIntro();
                state.currentMapId=state.world.createEditorMap("editor_market_review","Marketplace", "village",36,28);
                var area=state.world.area(state.currentMapId);area.fillTiles(0,0,35,27,'g');
                String[] colors={"canvas","blue","green","gold"};
                for(int i=0;i<4;i++) area.addProp(new WorldProp(10+i*2,9,"market_stall_"+colors[i],48));
                String[] fences={"village_fence_auto","location_farmland_fence","location_graveyard_iron_fence"};
                for(int f=0;f<3;f++) for(int y=13;y<=17;y++) for(int x=7+f*7;x<=12+f*7;x++)
                    if(y==13 || y==17 || x==7+f*7 || x==12+f*7)
                        area.addProp(new WorldProp(x,y,fences[f],48));
                for(int i=0;i<4;i++) area.addProp(new WorldProp(11+i*3,20,"interior_armchair_green_"+DirectionalSeating.DIRECTIONS.get(i),48));
                state.playerX=17;state.playerY=12;state.zoom=125;state.config.renderQuality="high";
                state.worldTick=GameState.TICKS_PER_GAME_DAY/2;
                ((CameraController)field("cameraController").get(panel)).resetToPlayer();panel.setSize(GameConfig.WIDTH,GameConfig.HEIGHT);
                BufferedImage scene=new BufferedImage(GameConfig.WIDTH,GameConfig.HEIGHT,BufferedImage.TYPE_INT_RGB);
                Graphics2D world=scene.createGraphics();panel.paint(world);world.dispose();ImageIO.write(scene,"png",output.resolve("market-and-fences.png").toFile());
            }catch(Exception ex){throw new IllegalStateException(ex);}finally{panel.shutdown();}
        });
    }
    private static Field field(String name)throws Exception{Field f=GamePanel.class.getDeclaredField(name);f.setAccessible(true);return f;}
}
