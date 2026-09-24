package com.alderfall.game;

import com.alderfall.game.editor.*;
import com.alderfall.game.render.world.WorldPropRenderer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Covers catalogue completeness, regional families, placement poses and saved logical IDs. */
public final class EditorAssetLibraryTest {
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    public static void main(String[] args) throws Exception {
        AssetStore assets=new AssetStore(Path.of("assets"));
        var entries=EditorPalette.entries(assets);
        Map<String,EditorPalette.Entry> raw=new HashMap<>();
        for(var entry:entries)if(!entry.terrain()&&!entry.building())check(raw.put(entry.asset(),entry)==null,"Duplicate raw sprite: "+entry.asset());
        for(String name:assets.assetNames())check(raw.containsKey(name),"Runtime image excluded from editor: "+name);
        for(var prop:VillageManager.outdoorAssets())check(raw.containsKey(prop.asset()),"Outdoor alias excluded: "+prop.asset());
        for(var prop:VillageManager.interiorAssets())check(raw.containsKey(prop.asset()),"Interior alias excluded: "+prop.asset());
        int regional=0,animations=0,flora=0;
        Set<String> families=new TreeSet<>();
        for(var entry:raw.values()) {
            String path=assets.assetRelativePath(entry.asset());families.add(entry.category());
            if(path.contains("/city/buildings/")){regional++;check(entry.category().startsWith("Building sprites / "),"Regional building grouping: "+entry.asset());check(entry.size()==180,"Regional building size");}
            if(entry.asset().endsWith("_anim")){animations++;check(entry.category().startsWith("Animation poses / "),"Animation grouping");}
            if(path.contains("/props/nature/")){flora++;check(entry.category().contains("nature"),"Nature grouping");}
        }
        check(regional>100&&animations>100&&flora>0,"Expected regional, animation and nature libraries");
        for(String animal:new String[]{"wolf","stag","sheep","bat","crystal_hare"})check(raw.containsKey(animal)&&raw.get(animal).category().equals("Fauna and monsters"),"Fauna missing: "+animal);
        check(families.contains("Player characters")&&families.contains("Companions"),"Character families missing");
        check(families.stream().anyMatch(s -> s.startsWith("Items / ")),"Items missing");
        check(families.stream().anyMatch(s -> s.startsWith("Atlases and sheets / ")),"Atlases missing");
        Path scratch=Path.of("temp/editor-asset-library");Files.createDirectories(scratch);
        MapDocument all=new MapDocument("All runtime sprite IDs","village",12,12,'g');
        for(var entry:raw.values())all.props.add(new WorldProp(2,2,entry.asset(),entry.size()));
        Path export=scratch.resolve("all-assets.aldermap");MapDocumentIO.write(export,all);
        check(all.sameContent(MapDocumentIO.read(export)),"All sprite IDs survive map persistence");

        // A two-frame authored strip must place the red pose, not a squeezed red+blue sheet.
        Path fixtureRoot=scratch.resolve("fixture/assets"),effects=fixtureRoot.resolve("effects");Files.createDirectories(effects);
        BufferedImage strip=new BufferedImage(32,16,BufferedImage.TYPE_INT_ARGB);Graphics2D g=strip.createGraphics();
        g.setColor(Color.RED);g.fillRect(0,0,16,16);g.setColor(Color.BLUE);g.fillRect(16,0,16,16);g.dispose();
        ImageIO.write(strip,"png",effects.resolve("test_pose_anim.png").toFile());Files.writeString(effects.resolve("test_pose_anim.frames"),"2");
        AssetStore fixture=new AssetStore(fixtureRoot);
        BufferedImage pose=fixture.placementSpriteFit("test_pose_anim",48,48);
        for(int x=0;x<48;x++)check(pose.getRGB(x,24)==Color.RED.getRGB(),"Animation sheet leaked into placement pose");
        check(pose==fixture.placementSpriteFit("test_pose_anim",48,48),"Placement pose cache reused");
        WorldPropRenderer renderer=new WorldPropRenderer(fixture,null,null);
        check(renderer.propImage("test_pose_anim",48,48).getRGB(40,24)==Color.RED.getRGB(),"Gameplay prop renderer uses the same first pose");
        check(fixture.spriteFit("test_pose_anim",48,48).getRGB(40,24)==Color.BLUE.getRGB(),"Raw sprite API still exposes the original sheet");

        BufferedImage board=new BufferedImage(840,480,BufferedImage.TYPE_INT_RGB);g=board.createGraphics();g.setColor(new Color(28,33,39));g.fillRect(0,0,840,480);g.setColor(Color.WHITE);
        String[] sample={"wolf","stag","sheep","bat","crystal_hare","npc_innkeeper",
            raw.keySet().stream().filter(n -> assets.assetRelativePath(n).contains("/city/buildings/north/")).sorted().findFirst().orElseThrow(),
            raw.keySet().stream().filter(n -> assets.assetRelativePath(n).contains("/city/buildings/sun/")).sorted().findFirst().orElseThrow(),
            raw.keySet().stream().filter(n -> n.startsWith("deco_tree")&&!n.endsWith("_anim")).sorted().findFirst().orElseThrow(),
            raw.keySet().stream().filter(n -> n.endsWith("_anim")&&assets.assetRelativePath(n).startsWith("characters/monsters/")).sorted().findFirst().orElseThrow()};
        for(int i=0;i<sample.length;i++){
            if(!assets.hasSprite(sample[i]))continue;
            int x=(i%5)*168,y=(i/5)*240;
            g.drawImage(assets.placementSpriteFit(sample[i],150,190),x+9,y+5,null);
            g.drawString(sample[i],x+5,y+215);
        }
        g.dispose();ImageIO.write(board,"png",scratch.resolve("library-samples.png").toFile());
        System.out.println("PASS: "+assets.assetNames().size()+" runtime IDs, "+raw.size()+" placeable IDs, "+regional+" regional building sprites, "+animations+" animation poses; no catalogue exclusions");
    }
}
