package com.alderfall.game.editor;

import com.alderfall.game.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.List;

public final class EditorResponsivenessTest {
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static BufferedImage render(GamePanel panel){
        BufferedImage image=new BufferedImage(768,576,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
        panel.renderEditorScene(g,0,0,32,24,24,true,false,true);g.dispose();return image;
    }
    public static void main(String[] args)throws Exception{
        MapDocument d=new MapDocument("Cache study","village",32,24,'g');
        GamePanel panel=new GamePanel(Path.of("."),false);
        try{
            panel.editorState().currentMapId="editor_workshop";d.install(panel.editorState().world,"editor_workshop");render(panel);
            long warm=panel.editorTerrainBuildCount();render(panel);check(warm==panel.editorTerrainBuildCount(),"Warm frame must reuse chunks");
            d.tiles[10][10]='w';d.install(panel.editorState().world,"editor_workshop");
            long started=System.nanoTime();BufferedImage edited=render(panel);long changed=panel.editorTerrainBuildCount()-warm;
            check(changed>0&&changed<=9,"Single tile must rebuild only adjacent chunks: "+changed);
            System.out.println("Single tile: "+changed+" / "+warm+" chunks rebuilt, "+(System.nanoTime()-started)/1_000_000+" ms");
            GamePanel fresh=new GamePanel(Path.of("."),false);
            try{fresh.editorState().currentMapId="editor_workshop";d.install(fresh.editorState().world,"editor_workshop");BufferedImage expected=render(fresh);
                for(int y=0;y<edited.getHeight();y++)for(int x=0;x<edited.getWidth();x++)check(edited.getRGB(x,y)==expected.getRGB(x,y),"Local invalidation differs from fresh render at "+x+","+y);
                EditorPatchBrush.paint(d,20,16,3,100,20,24,0,0,List.of("deco_ground_meadow_1"),true,new HashSet<>());
                long patchBefore=panel.editorTerrainBuildCount();d.install(panel.editorState().world,"editor_workshop");BufferedImage patchImage=render(panel);
                check(panel.editorTerrainBuildCount()-patchBefore<=9,"Patch only rebuilds nearby chunks");
                d.install(fresh.editorState().world,"editor_workshop");
                BufferedImage hidden=new BufferedImage(768,576,BufferedImage.TYPE_INT_RGB);Graphics2D hg=hidden.createGraphics();
                fresh.renderEditorScene(hg,0,0,32,24,24,true,false,false);hg.dispose(); // Force a complete rebuild on visibility change.
                BufferedImage expectedPatch=render(fresh);boolean visibleDifference=false;
                for(int y=0;y<patchImage.getHeight();y++)for(int x=0;x<patchImage.getWidth();x++){
                    check(patchImage.getRGB(x,y)==expectedPatch.getRGB(x,y),"Patch chunk seam/cache mismatch at "+x+","+y);
                    visibleDifference|=hidden.getRGB(x,y)!=expectedPatch.getRGB(x,y);
                }
                check(visibleDifference,"Prop visibility controls baked patches");
                WorldProp nw=new WorldProp(4,4,"deco_ground_meadow_1",24,128,0,0);
                var anchor=PropPlacement.at(panel.editorState().world,"editor_workshop",nw);
                check(anchor.x()==.25&&anchor.y()==.25,"Explicit NW quarter anchor");
            }finally{fresh.shutdown();}
            long before=panel.editorTerrainBuildCount();d.props.add(new WorldProp(8,8,"deco_flower",24,-1,12,-8));d.install(panel.editorState().world,"editor_workshop");render(panel);
            check(panel.editorTerrainBuildCount()==before,"Ordinary placement must not rebuild terrain");
        }finally{panel.shutdown();}
        MapDocument patch=new MapDocument("Patch","village",16,16,'g');
        EditorPatchBrush.paint(patch,8,8,3,100,0,24,0,0,List.of("deco_ground_meadow_1"),true,new HashSet<>());
        check(patch.props.size()>4&&patch.props.size()<=36,"Round patch tapers toward its boundary");
        check(patch.props.stream().filter(p -> p.x()==8&&p.y()==8).count()==4,"Patch center has four anchors per tile");
        int count=patch.props.size();EditorPatchBrush.paint(patch,8,8,3,100,0,24,0,0,List.of("deco_ground_meadow_1"),true,new HashSet<>());
        check(patch.props.size()==count,"Repainting must not pile up slots");
        patch.props.add(new WorldProp(2,3,"deco_flower",24,-1,12,-8));
        Path file=Path.of("temp/editor-responsive/offsets.aldermap");MapDocumentIO.write(file,patch);
        check(MapDocumentIO.read(file).props.equals(patch.props),"Offsets and patch slots roundtrip");
        check(patch.rotate().rotate().rotate().rotate().props.equals(patch.props),"Four rotations preserve offsets and quadrants");
        MapDocument part=patch.extract(new Rectangle(1,2,3,3));MapDocument target=new MapDocument("Stamp","village",16,16,'g');target.stamp(part,5,5,false);
        check(target.props.get(0).offsetX()==12&&target.props.get(0).offsetY()==-8,"Prefab preserves offsets");
        var a=new EditorPalette.Entry("Alpha","Nature / Grass","a","",24,(char)0);var b=new EditorPalette.Entry("Beta","Nature / Flowers","b","",24,(char)0);
        var info=Map.of("a",new EditorAssetBrowser.Info("a",10),"b",new EditorAssetBrowser.Info("b",20));
        check(EditorAssetBrowser.comparator("Name A-Z",info).compare(a,b)<0,"Alphabetical ordering");
        check(EditorAssetBrowser.comparator("Newest modified",info).compare(a,b)>0,"Modification ordering");
        check(EditorAssetBrowser.category(a).equals("Nature")&&EditorAssetBrowser.subcategory(a).equals("Grass"),"Category hierarchy");
        System.out.println("PASS: local rendering cache, offsets, patch density/deduplication, prefab rotation and browser ordering");
    }
}
