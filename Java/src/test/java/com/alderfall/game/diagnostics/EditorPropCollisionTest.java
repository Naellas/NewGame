package com.alderfall.game;

import com.alderfall.game.editor.MapDocument;
import java.nio.file.Path;

public final class EditorPropCollisionTest {
    private static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    public static void main(String[] args)throws Exception{
        GameState state=new GameState(GameConfig.load(Path.of(".").toAbsolutePath()));
        String id="editor_collision";MapDocument map=new MapDocument("Collision","village",32,24,'g');map.install(state.world,id);
        WorldProp normal=new WorldProp(12,12,"deco_rock",48),large=new WorldProp(12,12,"deco_rock",192,-1,36,-24);
        var a=PropCollision.footprint(state.world,id,normal);var b=PropCollision.footprint(state.world,id,large);
        check(Math.abs(b.width/a.width-4)<1e-9,"Hitbox must grow beyond the old 2x cap");
        check(Math.abs(b.getCenterX()-a.getCenterX()-.75)<1e-9,"Hitbox follows horizontal offset");
        WorldProp shifted=new WorldProp(12,12,"deco_rock",48,-1,48,-48);
        var shiftedBounds=PropCollision.footprint(state.world,id,shifted);
        check(Math.abs(shiftedBounds.y-a.y+1)<1e-9,"Hitbox follows vertical offset");
        state.world.area(id).props.add(shifted);
        check(PropCollision.clear(state.world,id,a.getCenterX(),a.getCenterY()),"Old visual position does not retain collision");
        check(!PropCollision.clear(state.world,id,shiftedBounds.getCenterX(),shiftedBounds.getCenterY()),"Offset prop blocks its visible base");
        state.world.area(id).props.clear();
        WorldProp huge=new WorldProp(12,12,"deco_dense_bush",512,-1,48,48);state.world.area(id).props.add(huge);
        var hugeBounds=PropCollision.footprint(state.world,id,huge);double farX=hugeBounds.getMaxX()-.05,farY=hugeBounds.getCenterY();
        check(!PropCollision.clear(state.world,id,farX,farY),"Large offset footprint found beyond old neighbor query");
        check(!PropCollision.canTravel(state.world,id,farX,farY-4,farX,farY+4),"Swept movement finds large footprint");
        check(PropCollision.footprint(state.world,id,new WorldProp(1,1,"deco_ground_meadow_1",28,128))==null,"Ground-cover patches remain passable");
        state.world.area(id).props.clear();WorldProp fence=new WorldProp(8,8,"village_fence_auto",48,-1,48,0);state.world.area(id).props.add(fence);
        check(state.world.isGroundPassable(id,8,8),"Shifted structure must not retain its old tile blocker");
        check(PropCollision.clear(state.world,id,8.5,8.5),"Former fence tile is clear");
        check(!PropCollision.clear(state.world,id,9.5,8.5),"Fence footprint follows its artwork");
        check(state.world.area(id).moveProp(fence,10,10).offsetX()==48,"Runtime movement retains authored offsets");
        check(!state.world.isGroundPassable(id,11,10),"NPC tile navigation must see shifted structural collision");
        state.world.area(id).props.clear();
        WorldProp hedge=new WorldProp(8,8,"town_hedge_h",48,-1,-12,0);state.world.area(id).props.add(hedge);
        check(!state.world.isGroundPassable(id,8,8),"Quarter-tile hedge still blocks its occupied tile center");
        check(state.world.isGroundPassable(id,7,8),"Quarter-tile hedge leaves adjacent walking center clear");
        state.world.area(id).props.clear();
        WorldProp basin=new WorldProp(8,8,"town_fountain_civic",144,-1,48,48);state.world.area(id).props.add(basin);
        check(state.world.isGroundPassable(id,8,8),"Shifted fountain releases original corner");
        for(int y=9;y<=10;y++)for(int x=9;x<=10;x++)
            check(!state.world.isGroundPassable(id,x,y),"Shifted fountain retains its entire two-tile basin");
        System.out.println("PASS: scalable hitboxes, offset alignment, broad-phase lookup, swept collision and passable cover");
    }
}
