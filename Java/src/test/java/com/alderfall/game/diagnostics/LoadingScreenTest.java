package com.alderfall.game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Real modal-event-loop check: worker preparation must not stop Swing animation. */
public final class LoadingScreenTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        Path scratch=Path.of("temp/loading/tests");Files.createDirectories(scratch);
        SwingUtilities.invokeAndWait(() -> {
            CountDownLatch ticks=new CountDownLatch(6);AtomicBoolean installed=new AtomicBoolean();AtomicInteger heartbeat=new AtomicInteger();
            Timer timer=new Timer(80,e -> {ticks.countDown();heartbeat.incrementAndGet();});timer.start();
            try{
                LoadingScreen.run(null,"Loading test",progress -> {
                    check(!SwingUtilities.isEventDispatchThread(),"Preparation must leave the event thread");
                    progress.accept("Restoring settlement buildings and terrain...");
                    check(ticks.await(10,TimeUnit.SECONDS),"UI timer froze while preparation was running");return 42;
                },result -> {check(SwingUtilities.isEventDispatchThread()&&result==42,"Install must run on the event thread");installed.set(true);});
                check(installed.get()&&heartbeat.get()>=6,"Animation heartbeat remained active");
                try{LoadingScreen.run(null,"Failure test",progress -> {throw new java.io.IOException("Unreadable save");},result -> {throw new AssertionError("Failed loads must not install");});throw new AssertionError("Failure was swallowed");}
                catch(IllegalStateException expected){check(expected.getMessage().contains("Unreadable save"),"Failure preserved its cause");}
                for(Window window:Window.getWindows())if(window instanceof JDialog dialog)check(!dialog.isVisible(),"Loading dialog leaked after completion/failure");
                LoadingScreen.View view=new LoadingScreen.View("Loading saved adventure");
                try{view.stage("Restoring settlement buildings and terrain...");view.setSize(view.getPreferredSize());view.doLayout();BufferedImage capture=new BufferedImage(view.getWidth(),view.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=capture.createGraphics();view.printAll(g);g.dispose();ImageIO.write(capture,"png",scratch.resolve("loading.png").toFile());}
                catch(Exception ex){throw new RuntimeException(ex);}finally{view.stop();}
            }finally{timer.stop();}
        });
        var stages=new ArrayList<String>();GamePanel.Prepared original=GamePanel.prepare(Path.of("."),stages::add);
        check(stages.stream().anyMatch(s -> s.contains("terrain"))&&stages.stream().anyMatch(s -> s.contains("Indexing")),"Startup reports real stages");
        original.state().chooseClass("Mage");original.state().player.gold=123;
        SaveSystem saves=new SaveSystem(Files.createTempDirectory(scratch,"isolated-save-"));saves.save(original.state(),"Loading regression");String id=original.state().currentSaveId;
        original.state().player.gold=456;stages.clear();
        GamePanel.Prepared restored=GamePanel.prepareSave(Path.of("."),saves,id,original.assets(),stages::add);
        check(restored.state()!=original.state()&&original.state().player.gold==456&&restored.state().player.gold==123,"Save restoration is detached and preserves the running state");
        check(stages.stream().anyMatch(s -> s.startsWith("Reading save"))&&stages.stream().anyMatch(s -> s.startsWith("Restoring companions")),"Save stages describe restoration work");
        SwingUtilities.invokeAndWait(() -> {
            GamePanel panel=new GamePanel(Path.of("."),false,original);JFrame frame=new JFrame("Save loading integration");frame.setContentPane(panel);
            AtomicReference<GamePanel> replacement=new AtomicReference<>();
            try{
                var saveField=GamePanel.class.getDeclaredField("saves");saveField.setAccessible(true);saveField.set(panel,saves);
                panel.setReplacementHandler(next -> {check(SwingUtilities.isEventDispatchThread(),"Panel replacement uses EDT");replacement.set(next);frame.setContentPane(next);});
                panel.loadGame(id);
                check(replacement.get()!=null&&replacement.get().state.player.gold==123&&panel.state.player.gold==456,"Save selection installs the restored panel without mutating the old adventure");
            }catch(Exception ex){throw new RuntimeException(ex);}finally{panel.shutdown();if(replacement.get()!=null)replacement.get().shutdown();frame.dispose();}
        });
        System.out.println("PASS: responsive loading UI, EDT installation, error recovery, startup stages and detached save restoration");
    }
}
