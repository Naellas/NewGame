package com.alderfall.game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

public final class LauncherTest {
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicInteger games=new AtomicInteger(),editors=new AtomicInteger();
            LauncherWindow.LauncherPanel panel=new LauncherWindow.LauncherPanel(games::incrementAndGet,editors::incrementAndGet);
            panel.setSize(940,650);layout(panel);
            panel.play.doClick();check(games.get()==1&&editors.get()==0,"Play button routes only to game");
            panel.editor.doClick();check(games.get()==1&&editors.get()==1,"Editor button routes only to workshop");
            panel.busy("Starting…");panel.play.doClick();panel.editor.doClick();
            check(!panel.play.isEnabled()&&!panel.editor.isEnabled()&&games.get()==1&&editors.get()==1,"Prevent duplicate starts while loading");
            panel.ready();check(panel.play.isEnabled()&&panel.editor.isEnabled(),"Startup failure can re-enable both choices");
            BufferedImage image=new BufferedImage(940,650,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();panel.printAll(g);g.dispose();
            try{Path path=Path.of("temp/editor-asset-library/launcher.png");Files.createDirectories(path.getParent());ImageIO.write(image,"png",path.toFile());}catch(Exception ex){throw new RuntimeException(ex);}
        });
        System.out.println("PASS: launcher mode routing, loading guard, recovery and start-screen rendering");
    }
    private static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container nested)layout(nested);}
}
