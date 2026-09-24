package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import com.alderfall.game.camera.CameraController;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Actual game captures of geographically selected relief, plus the shared material swatches. */
public final class WorldElevationPreview {
    public static void main(String[] args)throws Exception {
        Path output=Path.of(args.length>0?args[0]:"temp/world-elevation/preview");Files.createDirectories(output);
        materials(output);
        SwingUtilities.invokeAndWait(()->{
            GamePanel panel=new GamePanel(Path.of("").toAbsolutePath());
            try {
                ((Timer)get(panel,"timer")).stop();GameState state=(GameState)get(panel,"state");
                state.chooseClass("Knight");while(state.mode==GameMode.STORY_INTRO)state.advanceStoryIntro();
                state.currentMapId=WorldMap.OVERWORLD_ID;state.zoom=125;state.config.renderQuality="high";
                state.worldTick=GameState.TICKS_PER_GAME_DAY/2;
                panel.setSize(GameConfig.WIDTH,GameConfig.HEIGHT);
                char[] types={'g','f','m','n','s','b','v','P'};
                String[] names={"meadow","forest","mountains","frost","sandstone","badlands","fen","coast"};
                StringBuilder locations=new StringBuilder();
                for(int i=0;i<types.length;i++) {
                    var spot=spot(state.world,types[i]);state.playerX=spot.x();state.playerY=spot.y();
                    capture(panel,output,names[i]);
                    locations.append(names[i]).append(": ").append(spot).append("; ")
                        .append(state.world.kingdomAt(spot.x(),spot.y()).id()).append('\n');
                }
                var trails=state.world.elevation(WorldMap.OVERWORLD_ID).trails();
                var chosen=trails.stream().filter(t->t.size()>=8&&t.size()<=25)
                    .max(java.util.Comparator.comparingDouble(t->{
                        var top=t.getLast();return t.size()+
                            (state.world.kingdomAt(top.x(),top.y()).id().equals("sanctum")?100:0);
                    })).orElse(trails.getFirst());
                var trailCenter=chosen.get(chosen.size()/2);
                state.playerX=trailCenter.x();state.playerY=trailCenter.y();capture(panel,output,"mountain-route");
                locations.append("mountain-route: ").append(chosen).append('\n');
                for(String map:new String[]{"city_highwall","city_sanctum","city_belltower"}) {
                    state.currentMapId=map;var field=state.world.elevation(map);
                    TilePoint best=new TilePoint(state.world.width(map)/2,state.world.height(map)/2);double score=-1;
                    for(int y=8;y<state.world.height(map)-8;y++)for(int x=8;x<state.world.width(map)-8;x++) {
                        if(!state.world.isPassable(map,x,y))continue;
                        double s=0;for(int dy=-4;dy<=4;dy+=2)for(int dx=-4;dx<=4;dx+=2)
                            s+=Math.abs(field.heightAt(x+dx+.5,y+dy+.5)-field.heightAt(x+dx+.5,y+dy+1.5));
                        if(s>score){score=s;best=new TilePoint(x,y);}
                    }
                    state.playerX=best.x();state.playerY=best.y();
                    capture(panel,output,map);
                    locations.append(map).append(": ").append(best).append('\n');
                }
                Files.writeString(output.resolve("locations.txt"),locations);
            }catch(Exception e){throw new IllegalStateException(e);}finally{panel.shutdown();}
        });
        System.out.println("World elevation captures: "+output.toAbsolutePath());
    }
    private static void capture(GamePanel panel,Path output,String name)throws Exception {
        ((CameraController)get(panel,"cameraController")).resetToPlayer();
        var image=new BufferedImage(GameConfig.WIDTH,GameConfig.HEIGHT,BufferedImage.TYPE_INT_RGB);
        var g=image.createGraphics();panel.paint(g);g.dispose();
        ImageIO.write(image,"png",output.resolve(name+".png").toFile());
    }
    private static TilePoint spot(WorldMap world,char terrain) {
        String map=WorldMap.OVERWORLD_ID;var field=world.elevation(map);
        TilePoint best=WorldMap.START_POSITION;double score=-1;
        for(int y=10;y<world.height(map)-10;y+=2)for(int x=10;x<world.width(map)-10;x+=2) {
            if(world.tileAt(map,x,y)!=terrain || !world.isGroundPassable(map,x,y))continue;
            double variety=0;int matching=0;
            for(int dy=-4;dy<=4;dy+=2)for(int dx=-5;dx<=5;dx+=2) {
                double a=field.heightAt(x+dx+.5,y+dy+.5),b=field.heightAt(x+dx+.5,y+dy+1.5);
                variety+=Math.abs(a-b);if(world.tileAt(map,x+dx,y+dy)==terrain)matching++;
            }
            double s=variety+matching*.03;
            if(matching<20)continue;
            if(s>score){score=s;best=new TilePoint(x,y);}
        }
        return best;
    }
    private static void materials(Path output)throws Exception {
        var image=new BufferedImage(960,540,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();
        g.setColor(new Color(22,29,26));g.fillRect(0,0,960,540);
        var values=ElevationMaterial.values();
        for(int i=0;i<values.length;i++) {
            int left=i%3*320,top=i/3*180;g.setColor(new Color(234,227,204));
            g.setFont(new Font("SansSerif",Font.BOLD,17));g.drawString(values[i].name(),left+18,top+28);
            for(int y=0;y<110;y++)for(int x=0;x<284;x++)
                image.setRGB(left+18+x,top+45+y,values[i].pixel(x/48.0,12,y/150.0,.75));
        }
        g.dispose();ImageIO.write(image,"png",output.resolve("materials.png").toFile());
    }
    private static Object get(Object target,String name)throws Exception {
        var f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);
    }
}
