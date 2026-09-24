package com.alderfall.game;

import com.alderfall.game.editor.*;
import com.alderfall.game.map.WorldMap;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

public final class MapEditorTest {
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    public static void main(String[] args) throws Exception {
        Path scratch=Path.of("temp/map-editor/tests");Files.createDirectories(scratch);
        MapDocument d=EditorLayouts.create("Workshop Łódź = city","city",64,48,872,true);
        var plan=VillageManager.buildingPlans().get(0);
        MapDocument small=new MapDocument("Room","city",12,12,'i');
        small.props.add(new WorldProp(2,3,"interior_chest",48,2));small.landmarks.put(new TilePoint(2,3),"A = B\nsecond line");
        small.placeBuilding(plan.style(),3,4,CityBuilding.Facing.EAST);
        Path map=scratch.resolve("roundtrip.aldermap");MapDocumentIO.write(map,small);
        check(small.sameContent(MapDocumentIO.read(map)),"Map roundtrip must preserve all data, including Unicode, slots and building facing");
        MapDocumentIO.write(map,d);check(d.sameContent(MapDocumentIO.read(map)),"City roundtrip");
        check(d.sameContent(EditorLayouts.create(d.label,"city",64,48,872,true)),"Seeded city is reproducible");
        MapDocument rotated=small;
        for(int i=0;i<4;i++)rotated=rotated.rotate();
        check(rotated.sameContent(small),"Four rotations restore tiles, props, buildings, spawn and markers");
        MapDocument extracted=small.extract(new Rectangle(0,0,12,12));
        MapDocument target=new MapDocument("Target","city",40,30,'g');target.stamp(extracted,10,8,true);
        check(target.props.get(0).x()==12&&target.props.get(0).y()==11,"Stamp translates props");
        check(target.landmarks.get(new TilePoint(12,11)).equals("A = B\nsecond line"),"Stamp translates landmarks");
        MapDocument unchanged=target.copy();
        try{target.stamp(extracted,39,29,true);throw new AssertionError("Out of bounds stamp accepted");}catch(IllegalArgumentException expected){}
        check(target.sameContent(unchanged),"Rejected stamp is atomic");
        try{target.stamp(extracted,10,8,true);throw new AssertionError("Overlapping stamp accepted");}catch(IllegalArgumentException expected){}
        check(target.sameContent(unchanged),"Overlap rejection must not paint terrain");
        EditorHistory history=new EditorHistory(target);
        history.begin(target);target.paint(0,0,3,'w');target.paint(1,0,3,'w');history.commit(target);
        check(history.dirty(target),"Stroke makes document dirty");target=history.undo(target);check(target.sameContent(unchanged),"Whole stroke undone");check(!history.dirty(target),"Undo returns to clean checkpoint");
        target=history.redo(target);check(target.tiles[0][0]=='w',"Redo restores stroke");
        history.saved(target);history.begin(target);target.fill(0,0,'n');history.commit(target);check(target.tiles[0][0]=='n'&&target.tiles[20][30]=='g',"Flood fill stays in connected material");
        MapDocument dungeon=EditorLayouts.create("Dungeon","dungeon",64,48,15,true);
        check(Arrays.deepEquals(dungeon.tiles,EditorLayouts.create("Dungeon","dungeon",64,48,15,true).tiles),"Seeded dungeon deterministic");
        WorldMap world=new WorldMap(0);dungeon.install(world,"editor_test");
        check(world.isPassable("editor_test",dungeon.spawnX,dungeon.spawnY),"Generated spawn walkable");
        Set<TilePoint> reachable=new HashSet<>();ArrayDeque<TilePoint> queue=new ArrayDeque<>();queue.add(new TilePoint(dungeon.spawnX,dungeon.spawnY));
        while(!queue.isEmpty()){TilePoint p=queue.remove();if(!reachable.add(p))continue;for(int[] delta:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){int x=p.x()+delta[0],y=p.y()+delta[1];if(world.isPassable("editor_test",x,y)&&!reachable.contains(new TilePoint(x,y)))queue.add(new TilePoint(x,y));}}
        for(int y=0;y<dungeon.height();y++)for(int x=0;x<dungeon.width();x++)if(Terrain.passable(dungeon.tiles[y][x]))check(reachable.contains(new TilePoint(x,y)),"All generated dungeon rooms connected");
        long revision=world.visualRevision("editor_test");dungeon.install(world,"editor_test");check(world.visualRevision("editor_test")>revision,"Replacing a map invalidates render caches");
        String legacy="id=editor_old\nlabel=Old export\nkind=village\nwidth=2\nheight=2\ntile.0=gg\ntile.1=rr\nprop.0=0,0,city_prop_crate,48\n";
        Files.writeString(map,legacy);check(MapDocumentIO.read(map).props.size()==1,"Legacy editor export import");
        for(String bad:new String[]{legacy.replace("width=2","width=99999999"),legacy.replace("tile.1=rr","tile.1=r"),legacy.replace("0,0,city_prop_crate","9,0,city_prop_crate"),legacy+"version=900\n"}){
            Files.writeString(map,bad);try{MapDocumentIO.read(map);throw new AssertionError("Malformed map accepted");}catch(java.io.IOException expected){}
        }
        SwingUtilities.invokeAndWait(() -> {
            GamePanel panel=new GamePanel(Path.of("."),false);
            try {
                panel.setSize(1100,800);
                for(String kind:new String[]{"city","village","dungeon","interior"}){
                    MapDocument scene=EditorLayouts.create("Render "+kind,kind,48,36,42,true);
                    scene.install(panel.editorState().world,"editor_render");panel.editorState().currentMapId="editor_render";
                    BufferedImage image=new BufferedImage(960,720,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
                    panel.renderEditorScene(g,0,0,40,30,24,true,true,true);g.dispose();
                    try{ImageIO.write(image,"png",scratch.resolve(kind+".png").toFile());}catch(Exception ex){throw new RuntimeException(ex);}
                    Set<Integer> colors=new HashSet<>();for(int y=0;y<720;y+=7)for(int x=0;x<960;x+=11)colors.add(image.getRGB(x,y));
                    check(colors.size()>12,"Renderer draws textured "+kind);
                }
                MapDocument cacheScene=new MapDocument("Cache test","village",20,20,'g');cacheScene.install(panel.editorState().world,"editor_render");
                BufferedImage before=new BufferedImage(240,240,BufferedImage.TYPE_INT_RGB);Graphics2D bg=before.createGraphics();panel.renderEditorScene(bg,0,0,10,10,24,true,false,false);bg.dispose();
                cacheScene.fill(0,0,'n');cacheScene.install(panel.editorState().world,"editor_render");
                BufferedImage after=new BufferedImage(240,240,BufferedImage.TYPE_INT_RGB);Graphics2D ag=after.createGraphics();panel.renderEditorScene(ag,0,0,10,10,24,true,false,false);ag.dispose();
                check(before.getRGB(120,120)!=after.getRGB(120,120),"Replacing document refreshes rendered terrain");
                panel.startEditorPlaytest(dungeon);
                check(panel.editorState().mode==GameMode.EXPLORE,"Playtest enters gameplay");
                check(panel.editorState().playerX==dungeon.spawnX,"Playtest uses authored spawn");
                for(int i=0;i<5;i++)panel.editorState().tickWorld();
                BufferedImage gameplay=new BufferedImage(1100,800,BufferedImage.TYPE_INT_RGB);Graphics2D gg=gameplay.createGraphics();panel.paint(gg);gg.dispose();
                panel.editorState().world.area("editor_playtest").setTile(0,0,'w');check(dungeon.tiles[0][0]=='x',"Playtest cannot mutate source document");
            }finally{panel.shutdown();}
        });
        System.out.println("PASS: Map editor documents, persistence, prefabs, history, procedural layouts, rendering and game import");
    }
}
