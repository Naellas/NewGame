package com.alderfall.game;

import com.alderfall.game.ui.DialogueFigureAnimation;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Headless timing checks plus a visual contact sheet of the actual render path. */
public final class DialogueAnimationTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        check(DialogueFigureAnimation.mouthPose("", false, 150) == 0, "Narration must stay silent");
        check(DialogueFigureAnimation.mouthPose("Hello", true, 150) == 0, "Completed/skipped text must stay silent");
        check(DialogueFigureAnimation.mouthPose("Hello,", false, 150) == 0, "Punctuation must pause speech");
        check(DialogueFigureAnimation.mouthPose("Hello ", false, 150) == 0, "Spaces must pause speech");
        check(DialogueFigureAnimation.mouthPose("Hello", false, 150) == 2, "Revealing speech must open mouth");
        check(DialogueFigureAnimation.mouthPose("Hello", false, 300) == 0, "Speech must also close mouth");
        AssetStore assets = new AssetStore(Path.of("assets"));
        var files = Files.walk(Path.of("assets"));
        java.util.List<Path> roster;
        try (files) { roster = files.filter(p -> p.toString().endsWith("_dialogue_sprite.png")).sorted().toList(); }
        BufferedImage sheet = new BufferedImage(900, roster.size() * 180, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(new Color(75,75,75)); g.fillRect(0,0,sheet.getWidth(),sheet.getHeight());
        DialogueFigureAnimation animation = new DialogueFigureAnimation();
        BufferedImage portraits=new BufferedImage(26*110,160,BufferedImage.TYPE_INT_ARGB);
        Graphics2D portraitGraphics=portraits.createGraphics();
        for (int i = 0; i < roster.size(); i++) {
            String sprite = roster.get(i).getFileName().toString().replace(".png", "");
            BufferedImage source = assets.spriteFit(sprite, 700, 1000);
            int before = java.util.Arrays.hashCode(source.getRGB(0,0,700,1000,null,0,700));
            BufferedImage disabled=new BufferedImage(700,1000,BufferedImage.TYPE_INT_ARGB);
            Graphics2D disabledGraphics=disabled.createGraphics();
            new DialogueFigureAnimation(new com.alderfall.game.ui.DialogueAnimationLayers.Settings(false,false,false,false,false,false,false))
                    .draw(disabledGraphics,source,sprite,0,0,1234,0,true,true,1);
            disabledGraphics.dispose();
            check(before==java.util.Arrays.hashCode(disabled.getRGB(0,0,700,1000,null,0,700)),"Disabled layers changed artwork");
            long closedAt = Math.floorMod(-Math.floorMod(sprite.hashCode(), 4100) + 70, 4100);
            check(DialogueFigureAnimation.blinkPose(sprite, closedAt) == 2, "Blink timing");
            for (int pose = 0; pose < 3; pose++) {
                BufferedImage rendered = new BufferedImage(700,1000,BufferedImage.TYPE_INT_ARGB);
                Graphics2D rg = rendered.createGraphics();
                animation.draw(rg,source,sprite,0,0,pose == 1 ? closedAt : closedAt+300,pose == 2 ? 2 : 0);
                rg.dispose();
                // Fit leaves vertical padding for unusually wide art; these full figures all fit by height.
                g.drawImage(rendered,pose*300,i*180,pose*300+300,i*180+150,200,0,500,190,null);
                g.setColor(Color.WHITE); g.drawString(sprite.replace("_dialogue_sprite", "") + " / " + pose,pose*300+3,i*180+167);
            }
            check(before == java.util.Arrays.hashCode(source.getRGB(0,0,700,1000,null,0,700)), "Animation mutated source art");
            BufferedImage portrait=new BufferedImage(112,140,BufferedImage.TYPE_INT_ARGB);
            Graphics2D pg=portrait.createGraphics();
            animation.drawPortrait(pg,source,sprite,new Rectangle(10,10,92,120),1400,1,true,false,1);pg.dispose();
            int visible=0;
            for(int py=0;py<140;py++)for(int px=0;px<112;px++) {
                int alpha=portrait.getRGB(px,py)>>>24;
                if(px<10||px>=102||py<10||py>=130)check(alpha==0,"Portrait escaped its clip");
                else if(alpha>0)visible++;
            }
            check(visible>1000,"Portrait crop missed character");
            portraitGraphics.drawImage(portrait,i*110,0,null);
        }
        portraitGraphics.dispose();
        g.dispose();
        Path output = Path.of("temp/dialogue-animation-review.png"); Files.createDirectories(output.getParent());
        ImageIO.write(sheet,"png",output.toFile());
        ImageIO.write(portraits,"png",Path.of("temp/dialogue-animation/party-portraits.png").toFile());
        System.out.println("DialogueAnimationTest passed; rendered " + roster.size() + " characters: " + output);
    }
}
