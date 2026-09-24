package com.alderfall.game;

import com.alderfall.game.editor.MapEditorWindow;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.Path;
import javax.swing.*;

/** Lightweight start screen; the world is constructed only after a mode is chosen. */
public final class LauncherWindow extends JFrame {
    private final LauncherPanel panel;

    public static void launch(Path root) {
        SwingUtilities.invokeLater(() -> new LauncherWindow(root).setVisible(true));
    }

    private LauncherWindow(Path root) {
        super("Echoes of Alderfall");
        panel=new LauncherPanel(
                () -> start("Starting your adventure…",() -> GameWindow.openOnEventThread(root)),
                () -> start("Opening the Location Workshop…",() -> MapEditorWindow.openOnEventThread(root,null)));
        setContentPane(panel);setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(760,520));setSize(940,650);setLocationRelativeTo(null);
        getRootPane().setDefaultButton(panel.play);
    }

    private void start(String message,Runnable open) {
        panel.busy(message);
        // Give Swing a chance to paint the loading state before constructing the selected application.
        SwingUtilities.invokeLater(() -> {
            try {open.run();dispose();}
            catch(RuntimeException ex) {
                panel.ready();
                JOptionPane.showMessageDialog(this,"Could not start: "+ex.getMessage(),"Alderfall",JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    static final class LauncherPanel extends JPanel {
        private static final Color GOLD=new Color(218,189,127),TEXT=new Color(228,230,224);
        final JButton play,editor;
        private final JLabel status=new JLabel("Choose where to begin",SwingConstants.CENTER);

        LauncherPanel(Runnable playAction,Runnable editorAction) {
            setLayout(new GridBagLayout());setBorder(BorderFactory.createEmptyBorder(35,45,35,45));
            JPanel content=new JPanel();content.setOpaque(false);content.setLayout(new BoxLayout(content,BoxLayout.Y_AXIS));
            JLabel overline=label("E C H O E S   O F",new Font(Font.SANS_SERIF,Font.PLAIN,16),GOLD);
            JLabel title=label("ALDERFALL",new Font(Font.SERIF,Font.BOLD,64),TEXT);
            JLabel subtitle=label("Explore the world. Create your own locations.",new Font(Font.SANS_SERIF,Font.PLAIN,16),new Color(168,183,183));
            content.add(overline);content.add(title);content.add(Box.createVerticalStrut(12));content.add(subtitle);content.add(Box.createVerticalStrut(38));
            JPanel choices=new JPanel(new GridLayout(1,2,22,0));choices.setOpaque(false);
            play=new ModeButton("Play Game","Begin or continue an adventure","Quests, exploration and companions",false);
            editor=new ModeButton("Map Editor","Open the Location Workshop","Maps, assets and reusable prefabs",true);
            play.addActionListener(e -> playAction.run());editor.addActionListener(e -> editorAction.run());
            choices.add(play);choices.add(editor);choices.setPreferredSize(new Dimension(700,205));
            choices.setMaximumSize(new Dimension(700,205));content.add(choices);content.add(Box.createVerticalStrut(28));
            status.setForeground(new Color(168,183,183));status.setAlignmentX(.5f);content.add(status);add(content);
        }
        private static JLabel label(String text,Font font,Color color){JLabel label=new JLabel(text,SwingConstants.CENTER);label.setFont(font);label.setForeground(color);label.setAlignmentX(.5f);return label;}
        void busy(String text){play.setEnabled(false);editor.setEnabled(false);status.setText(text);paintImmediately(0,0,getWidth(),getHeight());}
        void ready(){play.setEnabled(true);editor.setEnabled(true);status.setText("Choose where to begin");}
        @Override protected void paintComponent(Graphics graphics){
            super.paintComponent(graphics);Graphics2D g=(Graphics2D)graphics.create();int w=getWidth(),h=getHeight();
            g.setPaint(new GradientPaint(0,0,new Color(14,30,38),w,h,new Color(8,16,23)));g.fillRect(0,0,w,h);
            g.setColor(new Color(25,45,48));g.fillPolygon(new int[]{0,w/5,w/3,w/2,w*3/4,w,w},new int[]{h*3/4,h/2,h*2/3,h*2/5,h*3/5,h/2,h},7);
            g.setColor(new Color(10,27,30));g.fillPolygon(new int[]{0,w/4,w/2,w*4/5,w,w,0},new int[]{h*4/5,h*3/5,h*4/5,h/2,h*3/4,h,h},7);
            g.setColor(new Color(218,189,127,45));g.drawRect(17,17,w-35,h-35);g.dispose();
        }
        private static final class ModeButton extends JButton {
            private final String title,description,detail;
            private final boolean map;
            ModeButton(String title,String description,String detail,boolean map){
                super(title);this.title=title;this.description=description;this.detail=detail;this.map=map;
                setContentAreaFilled(false);setBorderPainted(false);setFocusPainted(false);setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                getAccessibleContext().setAccessibleDescription(description+". "+detail);
                addMouseListener(new MouseAdapter(){public void mouseEntered(MouseEvent e){repaint();}public void mouseExited(MouseEvent e){repaint();}});
            }
            @Override protected void paintComponent(Graphics graphics){
                Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                boolean active=getModel().isRollover()||isFocusOwner();
                g.setColor(active?new Color(34,56,59):new Color(20,38,45));g.fillRoundRect(1,1,getWidth()-3,getHeight()-3,18,18);
                g.setColor(isEnabled()?GOLD:new Color(92,107,108));g.setStroke(new BasicStroke(active?2:1));g.drawRoundRect(2,2,getWidth()-5,getHeight()-5,18,18);
                int cx=getWidth()/2;
                if(map){for(int y=0;y<3;y++)for(int x=0;x<3;x++)g.drawRect(cx-23+x*16,26+y*13,12,9);}
                else{g.drawOval(cx-22,22,44,44);g.drawLine(cx,16,cx,72);g.drawLine(cx-28,44,cx+28,44);g.fillPolygon(new int[]{cx,cx-7,cx+7},new int[]{25,53,53},3);}
                drawCentered(g,title,new Font(Font.SERIF,Font.BOLD,29),TEXT,109);
                drawCentered(g,description,new Font(Font.SANS_SERIF,Font.PLAIN,14),TEXT,143);
                drawCentered(g,detail,new Font(Font.SANS_SERIF,Font.PLAIN,12),new Color(168,183,183),166);g.dispose();
            }
            private void drawCentered(Graphics2D g,String text,Font font,Color color,int y){g.setFont(font);g.setColor(color);g.drawString(text,(getWidth()-g.getFontMetrics().stringWidth(text))/2,y);}
        }
    }
}
