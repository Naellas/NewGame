package com.alderfall.game;

import com.alderfall.game.map.MapArea;
import java.util.*;

/** Local terrain invalidation for the static authoring view, isolated from gameplay revisions. */
final class EditorTerrainRevisions {
    private MapArea area;
    private String id,kind;
    private char[][] tiles;
    private long[][] revisions;
    private long serial=Long.MIN_VALUE,base;
    private boolean active,showGround=true;
    private Map<TilePoint,String> landmarks=Map.of();
    private Set<TilePoint> rugs=Set.of();
    private Map<TilePoint,String> interiorFloors=Map.of();
    private List<WorldProp> groundProps=List.of();
    private TerrainElevation.Field villageField;
    private long[][] villageHeights;
    private MapArea observedVillage;
    private long observedVillageRevision=Long.MIN_VALUE;

    /** Share the editor's local material cache with settlement play, including relief changes. */
    void prepareVillage(GameState state) {
        MapArea next=state.world.area(state.currentMapId);
        if(area==next && observedVillage==next && observedVillageRevision==next.visualRevision() && showGround) {
            active=true;return;
        }
        boolean reset=area!=next || villageHeights==null || villageHeights.length!=next.height()
                || villageHeights[0].length!=next.width();
        prepare(next,true);
        TerrainElevation.Field field=state.world.elevation(state.currentMapId);
        if(reset) villageHeights=new long[next.height()][next.width()];
        if(reset || field!=villageField) {
            long revision=++serial;
            for(int y=0;y<next.height();y++)for(int x=0;x<next.width();x++) {
                long height=field.surfaceSignature(x,y);
                // Relief interpolates raw samples, then draws a southward 2.5-tile halo.
                if(reset || villageHeights[y][x]!=height)dirty(x,y,4,revision);
                villageHeights[y][x]=height;
            }
            villageField=field;
        }
        observedVillage=next;observedVillageRevision=next.visualRevision();
    }

    void prepare(MapArea next,boolean groundVisible) {
        active=true;
        boolean reset=showGround!=groundVisible||tiles==null||!next.id.equals(id)||!next.kind.equals(kind)
                ||next.height()!=tiles.length||next.width()!=tiles[0].length;
        area=next;showGround=groundVisible;
        if(reset){
            id=next.id;kind=next.kind;base=++serial;
            tiles=new char[next.height()][];revisions=new long[next.height()][next.width()];
            for(int y=0;y<tiles.length;y++){tiles[y]=next.tiles[y].clone();Arrays.fill(revisions[y],base);}
        }else{
            long revision=++serial;
            for(int y=0;y<tiles.length;y++)for(int x=0;x<tiles[y].length;x++)if(tiles[y][x]!=next.tiles[y][x]){
                // Material blends sample immediate neighbors; bank depth reaches 3 cells and marsh tint 7.
                char old=tiles[y][x],tile=next.tiles[y][x];
                int radius=old=='v'||tile=='v'?7:3;
                dirty(x,y,radius,revision);tiles[y][x]=tile;
            }
        }
        if(!landmarks.equals(next.landmarks)){
            long revision=++serial;
            for(var p:landmarks.keySet())if(!Objects.equals(landmarks.get(p),next.landmarks.get(p)))dirty(p.x(),p.y(),1,revision);
            for(var p:next.landmarks.keySet())if(!Objects.equals(landmarks.get(p),next.landmarks.get(p)))dirty(p.x(),p.y(),1,revision);
            landmarks=new HashMap<>(next.landmarks);
        }
        List<WorldProp> ground=next.props.stream().filter(p -> p.visualSlot()>=0
                || com.alderfall.game.map.InteriorFlooring.affectsFloor(p.asset())).toList();
        if(!groundProps.equals(ground)){
            long revision=++serial;Set<WorldProp> old=new HashSet<>(groundProps),current=new HashSet<>(ground);
            for(WorldProp p:old)if(!current.contains(p))dirty(p.x(),p.y(),4,revision);
            for(WorldProp p:current)if(!old.contains(p))dirty(p.x(),p.y(),4,revision);
            groundProps=ground;
        }
        if(!rugs.equals(next.interiorRugs)){
            rugs=Set.copyOf(next.interiorRugs);long revision=++serial;for(long[] row:revisions)Arrays.fill(row,revision);
        }
        if(!interiorFloors.equals(next.interiorFloorMaterials)){
            interiorFloors=Map.copyOf(next.interiorFloorMaterials);
            long revision=++serial;for(long[] row:revisions)Arrays.fill(row,revision);
        }
    }
    private void dirty(int x,int y,int radius,long revision){
        if(x+radius<0||y+radius<0||x-radius>=revisions[0].length||y-radius>=revisions.length)return;
        for(int yy=Math.max(0,y-radius);yy<Math.min(revisions.length,y+radius+1);yy++)
            Arrays.fill(revisions[yy],Math.max(0,x-radius),Math.min(revisions[yy].length,x+radius+1),revision);
    }
    long revision(MapArea target,int x,int y,int width,int height,long fallback){
        if(!active||target!=area)return fallback;
        long result=base;
        for(int yy=Math.max(0,y);yy<Math.min(revisions.length,y+height);yy++)
            for(int xx=Math.max(0,x);xx<Math.min(revisions[yy].length,x+width);xx++)result=Math.max(result,revisions[yy][xx]);
        return result;
    }
    void finish(){active=false;}
}
