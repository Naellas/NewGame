package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.util.*;

public final class WorldElevationTest {
    public static void main(String[] args) {
        for(long seed:new long[]{0,42,2026}) {
            var world=new WorldMap(seed);String map=WorldMap.OVERWORLD_ID;
            var field=world.elevation(map);
            int rim=0,core=0,steps=0,cliffs=0;double max=0;
            for(int y=1;y<world.height(map)-1;y++)for(int x=1;x<world.width(map)-1;x++) {
                char t=world.tileAt(map,x,y);double h=field.heightAt(x+.5,y+.5);max=Math.max(max,h);
                if("w~B".indexOf(t)>=0)require(h==0,"Water and bridges stay at datum");
                if(Terrain.ROAD_LIKE.contains(t))require(h==0,"Existing road grades protected");
                if(field.mountainWalkable(x,y))rim++;
                if(field.summit(x,y)){core++;require(!world.isGroundPassable(map,x,y),"Summits remain blocked");}
                if(h>0 && h<1)steps++;
                if(h>=2 && Math.abs(h-field.heightAt(x+1.5,y+.5))>.5
                    &&field.rampAt(x+.5,y+.5)==0&&field.rampAt(x+1.5,y+.5)==0
                    &&!field.canTravel(x+.5,y+.5,x+1.5,y+.5))cliffs++;
                if(Terrain.passable(t))for(int[] d:new int[][]{{1,0},{0,1}}) {
                    if(!Terrain.passable(world.tileAt(map,x+d[0],y+d[1])))continue;
                    require(field.canTravel(x+.5,y+.5,x+d[0]+.5,y+d[1]+.5),"Elevation cannot sever ground edge "+seed+" "+x+","+y+" "+t+" -> "+world.tileAt(map,x+d[0],y+d[1])+" "+d[0]+","+d[1]);
                }
            }
            require(rim>100&&core>100&&steps>1000&&max>=2,"Geographic relief and mountain hierarchy");
            require(cliffs>0,"Full-height scarps remain impassable outside ramps");
            require(field.trails().size()>=5,"Mountain terraces have deliberate access routes");
            for(var trail:field.trails()) {
                var first=trail.getFirst();var last=trail.getLast();
                require(Terrain.ROAD_LIKE.contains(world.tileAt(map,first.x(),first.y())),"Mountain trail joins an existing pass/road");
                require(field.heightAt(last.x()+.5,last.y()+.5)>=1,"Mountain trail reaches an upper terrace");
                for(int i=1;i<trail.size();i++) {
                    var a=trail.get(i-1);var b=trail.get(i);
                    require(field.canTravel(a.x()+.5,a.y()+.5,b.x()+.5,b.y()+.5)
                        &&field.canTravel(b.x()+.5,b.y()+.5,a.x()+.5,a.y()+.5),"Ramps support uphill and downhill traversal");
                    require(PropCollision.canTravel(world,map,a.x()+.5,a.y()+.5,b.x()+.5,b.y()+.5),"Ramp collision "+seed+" "+a+" -> "+b+" passable "+world.isGroundPassable(map,a.x(),a.y())+","+world.isGroundPassable(map,b.x(),b.y()));
                }
            }
            for(var site:world.settlementSites()) {
                var local=world.elevation(site.id());require(local.active(),"Settlement relief enabled");
                for(var b:world.cityBuildings(site.id()))
                    require(local.heightAt(b.x1()+.5,b.y2()+.5)==0,"Level building foundation");
            }
            var repeat=new WorldMap(seed).elevation(map);
            for(int y=0;y<world.height(map);y+=7)for(int x=0;x<world.width(map);x+=7)
                require(field.heightAt(x+.31,y+.73)==repeat.heightAt(x+.31,y+.73),"Seed repeatability");
            var area=world.area(map);char old=area.tileAt(120,120);area.setTile(120,120,'w');
            require(world.elevation(map)!=field && world.elevation(map).heightAt(120.5,120.5)==0,"Edits invalidate relief");
            require(world.elevation(map).trails().equals(field.trails()),"Map edits cannot relocate established mountain access");
            area.setTile(120,120,old);
            require(world.elevation(world.createEditorMap("editor_relief_test","Test","village",20,20))==TerrainElevation.FLAT,"Authored editor maps opt out");
            System.out.println("Seed "+seed+": "+rim+" foothill tiles, "+core+" summit tiles, "+steps+" low shelves, "+field.trails().size()+" clear mountain routes, max height "+max);
        }
        var colors=new HashSet<Integer>();
        for(var material:ElevationMaterial.values())colors.add(material.pixel(3.5,4.25,.2,.6));
        require(colors.size()==ElevationMaterial.values().length,"Distinct bank materials");
        System.out.println("World elevation passed: geography, preserved routes/foundations, water, edits and materials.");
    }
    private static void require(boolean okay,String message){if(!okay)throw new AssertionError(message);}
}
