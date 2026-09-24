package com.alderfall.game;

import java.awt.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.*;

/** Modal input guard with background preparation and EDT-only presentation/installation. */
public final class LoadingScreen {
    @FunctionalInterface public interface Task<T>{T prepare(Consumer<String> progress)throws Exception;}
    private LoadingScreen(){}
    public static <T> void run(Window owner,String title,Task<T> task,Consumer<T> install){
        if(!SwingUtilities.isEventDispatchThread())throw new IllegalStateException("Loading screens must open on the event thread.");
        JDialog dialog=new JDialog(owner,title,Dialog.ModalityType.APPLICATION_MODAL);
        View view=new View(title);dialog.setContentPane(view);dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dialog.setResizable(false);dialog.pack();dialog.setLocationRelativeTo(owner);
        Throwable[] failure=new Throwable[1];
        SwingWorker<T,String> worker=new SwingWorker<>(){
            protected T doInBackground()throws Exception{return task.prepare(this::publish);}
            protected void process(List<String> messages){if(!messages.isEmpty())view.stage(messages.get(messages.size()-1));}
            protected void done(){
                try{T result=get();view.stage("Preparing the interface...");view.paintImmediately(0,0,view.getWidth(),view.getHeight());install.accept(result);}
                catch(ExecutionException ex){failure[0]=ex.getCause();}
                catch(Exception ex){failure[0]=ex;}
                finally{view.stop();dialog.dispose();}
            }
        };
        worker.execute();dialog.setVisible(true);
        if(failure[0]!=null)throw new IllegalStateException("Could not finish loading: "+failure[0].getMessage(),failure[0]);
    }
    static final class View extends JPanel {
        private final JLabel stage=new JLabel("Starting...",SwingConstants.CENTER),elapsed=new JLabel("Working...",SwingConstants.CENTER);
        private final Timer timer;
        private final JProgressBar progress=new JProgressBar();
        private final long started=System.nanoTime();
        View(String title){
            setBackground(new Color(15,29,37));setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));setBorder(BorderFactory.createEmptyBorder(34,32,30,32));setPreferredSize(new Dimension(580,245));
            JLabel heading=new JLabel(title,SwingConstants.CENTER);heading.setFont(new Font(Font.SERIF,Font.BOLD,27));heading.setForeground(new Color(234,204,143));
            stage.setForeground(new Color(229,235,231));elapsed.setForeground(new Color(155,179,180));
            progress.setIndeterminate(true);progress.setMaximumSize(new Dimension(510,12));progress.setForeground(new Color(196,162,88));
            for(JComponent c:new JComponent[]{heading,stage,elapsed,progress})c.setAlignmentX(.5f);
            add(heading);add(Box.createVerticalStrut(23));add(stage);add(Box.createVerticalStrut(21));add(progress);add(Box.createVerticalStrut(17));add(elapsed);
            timer=new Timer(250,e -> elapsed.setText("Working - "+((System.nanoTime()-started)/1_000_000_000)+" seconds elapsed"));timer.start();
        }
        void stage(String message){stage.setText(message);stage.getAccessibleContext().setAccessibleDescription(message);}
        void stop(){timer.stop();progress.setIndeterminate(false);}
    }
}
