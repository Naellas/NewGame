package com.alderfall.game;

import com.alderfall.game.ui.PartyPortraitRenderer;
import com.alderfall.game.ui.QuestLogRenderer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Exercises the actual sidebar and banter hit targets, including paged speakers. */
public final class PartyPortraitHudTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static Field field(Class<?> type,String name)throws Exception {Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    private static Object value(Object object,String name)throws Exception {return field(object.getClass(),name).get(object);}
    private static void portraitSource()throws Exception {
        Path assets=Path.of("temp/party-portraits/fake-assets");Files.createDirectories(assets);
        for(String suffix:List.of("_portrait","_dialogue_sprite")) {
            BufferedImage image=new BufferedImage(500,1000,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();
            g.setColor(suffix.equals("_portrait")?Color.RED:Color.BLUE);g.fillRect(0,0,500,1000);g.dispose();
            ImageIO.write(image,"png",assets.resolve("npc_seraphine"+suffix+".png").toFile());
        }
        AssetStore store=new AssetStore(assets);Actor actor=GameData.RECRUITS.get("seraphine").createActor();
        BufferedImage image=new BufferedImage(100,140,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();
        new PartyPortraitRenderer(store).draw(g,actor,new Rectangle(0,0,100,140),null);g.dispose();
        check((image.getRGB(50,115)&0xffffff)==0x0000ff,"Legacy portrait substituted for dialogue art");
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args)throws Exception {
        portraitSource();
        SwingUtilities.invokeAndWait(()->{
            GamePanel panel=new GamePanel(Path.of("").toAbsolutePath());
            try {
                ((Timer)value(panel,"timer")).stop();GameState state=(GameState)value(panel,"state");
                state.chooseClass("Mage");while(state.mode==GameMode.STORY_INTRO)state.advanceStoryIntro();
                for(String id:List.of("aria","calder","seraphine")) {
                    state.recruitedIds.add(id);state.allies.add(GameData.RECRUITS.get(id).createActor());
                }
                panel.setSize(1280,720);
                for(String speaker:List.of("Seraphine Vale","Aria Foxglove")) {
                    check(state.partyMembers().stream().anyMatch(a->a.name.equals(speaker)),"Missing test speaker");
                    var prompt=new GameState.TravelBanterPrompt(speaker,"The road is quiet. Shall we keep moving?",List.of("Keep moving.","Wait a moment."),state.worldTick+900);
                    field(GameState.class,"activeTravelBanter").set(state,prompt);
                    BufferedImage image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();panel.paint(g);g.dispose();
                    Rectangle anchor=null;
                    for(Object zone:(List<?>)value(panel,"partyPortraitZones"))if(((Actor)value(zone,"actor")).name.equals(speaker)){anchor=(Rectangle)value(zone,"bounds");break;}
                    check(anchor!=null,"Paged speaking portrait is hidden");
                    int options=0;
                    for(UiButton button:(List<UiButton>)value(panel,"buttons"))if(button.label().startsWith("banter:")) {
                        options++;Rectangle b=button.bounds();
                        check(b.x+b.width<anchor.x,"Banter is not beside its speaking portrait");
                        check(anchor.x-b.x-b.width<60,"Banter tail detached by viewport scaling");
                        check(new Rectangle(0,0,1920,1080).contains(b),"Banter response offscreen");
                        Method hit=GamePanel.class.getDeclaredMethod("buttonAt",Point.class);hit.setAccessible(true);
                        check(hit.invoke(panel,new Point(b.x+b.width/2,b.y+b.height/2))==button,"Banter button occluded by another hit target");
                    }
                    check(options==2,"Missing or duplicate banter controls");
                    ImageIO.write(image,"png",Path.of("temp/party-portraits/"+speaker.split(" ")[0]+"-banter.png").toFile());
                    ((List<?>)value(panel,"partyPortraitZones")).clear();((List<?>)value(panel,"buttons")).clear();
                    Graphics2D compact=image.createGraphics();
                    ((QuestLogRenderer)value(panel,"questLogRenderer")).drawTravelPartyHud(compact,1540,90,350,230);
                    Method drawPrompt=GamePanel.class.getDeclaredMethod("drawTravelBanterPrompt",Graphics2D.class);drawPrompt.setAccessible(true);drawPrompt.invoke(panel,compact);compact.dispose();
                    check((int)value(value(panel,"questLogRenderer"),"travelPartyPage")>0,"Compact sidebar failed to follow speaker's page");
                    check(((List<?>)value(panel,"partyPortraitZones")).stream().anyMatch(zone->{try{return ((Actor)value(zone,"actor")).name.equals(speaker);}catch(Exception e){throw new RuntimeException(e);}}),"Compact sidebar omitted speaking actor");
                    ((List<UiButton>)value(panel,"buttons")).stream().filter(b->b.label().equals("banter:0")).findFirst().orElseThrow().action().run();
                    check(state.activeTravelBanter()!=prompt,"Banter reply action did not advance the prompt");
                }
                field(GameState.class,"activeTravelBanter").set(state,null);
                BufferedImage image=new BufferedImage(1920,1080,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();panel.paint(g);g.dispose();
                check(((List<UiButton>)value(panel,"buttons")).stream().noneMatch(b->b.label().startsWith("banter:")),"Expired prompt left clickable options");
            }catch(Exception e){throw new RuntimeException(e);}finally{panel.shutdown();}
        });
        System.out.println("PartyPortraitHudTest passed: dialogue artwork, speaker paging, anchored options, hit targets and prompt cleanup.");
    }
}
