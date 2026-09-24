package com.alderfall.game.editor;

import com.alderfall.game.*;
import java.util.*;

/** Four authored cover anchors per tile. Repainting a slot replaces it instead of piling up copies. */
final class EditorPatchBrush {
    static boolean paint(MapDocument d,int cx,int cy,int diameter,int density,int variation,
                         int size,int offsetX,int offsetY,List<String> assets,boolean protect,Set<TilePoint> visited){
        if(assets.isEmpty())return false;
        boolean changed=false;
        Map<TilePoint,List<WorldProp>> existing=new HashMap<>();
        for(WorldProp p:d.props)existing.computeIfAbsent(new TilePoint(p.x(),p.y()),ignored -> new ArrayList<>()).add(p);
        Set<WorldProp> removed=new HashSet<>();List<WorldProp> added=new ArrayList<>();
        double radius=diameter/2.0;
        for(int y=cy-diameter/2;y<cy-diameter/2+diameter;y++)for(int x=cx-diameter/2;x<cx-diameter/2+diameter;x++){
            double distance=Math.hypot(x-cx,y-cy);TilePoint point=new TilePoint(x,y);
            if(!d.contains(x,y)||distance>radius||!visited.add(point))continue;
            var occupants=existing.getOrDefault(point,List.of());
            if(protect&&(d.buildingIndex(x,y)>=0||d.landmarks.containsKey(point)||"gfsnvb".indexOf(d.tiles[y][x])<0
                    ||occupants.stream().anyMatch(p -> p.visualSlot()<0)))continue;
            for(int slot=0;slot<4;slot++){
                Random random=new Random(d.seed ^ x*73428767L ^ y*912931L ^ slot*1709L);
                double edge=diameter<=2?1:Math.min(1,(radius-distance+0.5)/Math.max(1,radius*.35));
                if(random.nextInt(100)>=density*edge)continue;
                int visualSlot=128+slot;
                for(WorldProp p:occupants)if(p.visualSlot()==visualSlot)removed.add(p);
                String asset=assets.get(random.nextInt(assets.size()));
                int scaled=Math.max(4,Math.min(28,(int)Math.round(Math.min(28,size)*(1+(random.nextDouble()*2-1)*variation/100.0))));
                int jitter=variation/5;
                int ox=Math.max(-12,Math.min(12,offsetX+(jitter==0?0:random.nextInt(jitter*2+1)-jitter)));
                int oy=Math.max(-12,Math.min(12,offsetY+(jitter==0?0:random.nextInt(jitter*2+1)-jitter)));
                added.add(new WorldProp(x,y,asset,scaled,visualSlot,ox,oy));changed=true;
            }
        }
        d.props.removeAll(removed);d.props.addAll(added);return changed;
    }
}
