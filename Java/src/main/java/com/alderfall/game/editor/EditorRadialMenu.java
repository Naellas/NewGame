package com.alderfall.game.editor;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;

/** A translucent canvas overlay: alpha blends over the map rather than an opaque popup window. */
final class EditorRadialMenu extends JPanel {
    private final MapEditorWindow editor;
    private final String[] labels;
    private final Runnable[] actions;
    private final int count;
    private final Shape[] segments=new Shape[8];
    private int centerX=165,centerY=165,hovered=-1;

    EditorRadialMenu(MapEditorWindow editor){
        this.editor=editor;count=editor.propSelection().size()+editor.buildingSelection().size();
        setOpaque(false);setFocusable(true);setPreferredSize(new Dimension(330,330));
        labels=new String[]{"Select","Offset drag","Draw copies",count>0?"Delete":"Erase","Undo","Redo","Patch","Single"};
        actions=new Runnable[]{
            () -> editor.tool.setSelectedItem(MapEditorWindow.Tool.SELECT),
            () -> {editor.dragMode.setSelectedItem(MapEditorWindow.DragMode.OFFSET);editor.tool.setSelectedItem(count>0?MapEditorWindow.Tool.SELECT:MapEditorWindow.Tool.PLACE);},
            () -> {editor.clearSelection();editor.dragMode.setSelectedItem(MapEditorWindow.DragMode.COPIES);editor.tool.setSelectedItem(MapEditorWindow.Tool.PLACE);},
            () -> {if(count>0)editor.deleteSelection();else editor.tool.setSelectedItem(MapEditorWindow.Tool.ERASE);},
            editor::undo,editor::redo,
            () -> editor.tool.setSelectedItem(MapEditorWindow.Tool.PATCH),
            () -> {editor.clearSelection();editor.dragMode.setSelectedItem(MapEditorWindow.DragMode.SINGLE);editor.tool.setSelectedItem(MapEditorWindow.Tool.PLACE);}
        };
        for(int i=0;i<8;i++){
            Area wedge=new Area(new Arc2D.Double(-155,-155,310,310,111.5-i*45,-43,Arc2D.PIE));
            wedge.subtract(new Area(new Ellipse2D.Double(-64,-64,128,128)));segments[i]=wedge;
        }
        MouseAdapter mouse=new MouseAdapter(){
            public void mouseMoved(MouseEvent e){setHovered(segmentAt(e.getX(),e.getY()));}
            public void mouseDragged(MouseEvent e){mouseMoved(e);}
            public void mouseExited(MouseEvent e){setHovered(-1);}
            public void mousePressed(MouseEvent e){
                if(SwingUtilities.isRightMouseButton(e)){dismiss();return;}
                if(SwingUtilities.isLeftMouseButton(e)){int index=segmentAt(e.getX(),e.getY());dismiss();if(index>=0)actions[index].run();}
            }
            public void mouseWheelMoved(MouseWheelEvent e){e.consume();}
        };
        addMouseListener(mouse);addMouseMotionListener(mouse);addMouseWheelListener(mouse);
        bind("ESCAPE",this::dismiss);
        bind("RIGHT",() -> setHovered((hovered+1+8)%8));bind("DOWN",() -> setHovered((hovered+1+8)%8));
        bind("LEFT",() -> setHovered((hovered+7+8)%8));bind("UP",() -> setHovered((hovered+7+8)%8));
        bind("ENTER",() -> {int index=hovered;dismiss();if(index>=0)actions[index].run();});
        addFocusListener(new FocusAdapter(){public void focusLost(FocusEvent e){if(!e.isTemporary())dismiss();}});
        getAccessibleContext().setAccessibleName("Editor tool wheel");
    }
    private void bind(String key,Runnable action){getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key),key);getActionMap().put(key,new AbstractAction(){public void actionPerformed(ActionEvent e){action.run();}});}
    void showAt(EditorCanvas canvas,Point pointer){
        setBounds(0,0,canvas.getWidth(),canvas.getHeight());
        centerX=Math.max(165,Math.min(getWidth()-165,pointer.x));centerY=Math.max(165,Math.min(getHeight()-165,pointer.y));
        canvas.add(this);canvas.setComponentZOrder(this,0);setVisible(true);requestFocusInWindow();canvas.repaint();
    }
    void dismiss(){
        Container parent=getParent();setVisible(false);if(parent!=null){parent.remove(this);parent.repaint();editor.canvas.requestFocusInWindow();}
    }
    int segmentAt(int x,int y){for(int i=0;i<segments.length;i++)if(segments[i].contains(x-centerX,y-centerY))return i;return -1;}
    private void setHovered(int index){if(hovered==index)return;hovered=index;setCursor(Cursor.getPredefinedCursor(index>=0?Cursor.HAND_CURSOR:Cursor.DEFAULT_CURSOR));getAccessibleContext().setAccessibleDescription(index<0?"Choose a tool; Escape closes":labels[index]);repaint();}
    protected void paintComponent(Graphics graphics){
        Graphics2D g=(Graphics2D)graphics.create();g.translate(centerX,centerY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        for(int i=0;i<segments.length;i++){
            boolean active=i==hovered;g.setColor(active?new Color(151,117,47,215):new Color(28,44,53,175));g.fill(segments[i]);
            g.setStroke(new BasicStroke(active?2.5f:1f));g.setColor(active?new Color(255,217,128):new Color(151,180,191,130));g.draw(segments[i]);
            double angle=-Math.PI/2+i*Math.PI/4;int x=(int)Math.round(Math.cos(angle)*109),y=(int)Math.round(Math.sin(angle)*109);
            g.setFont(getFont().deriveFont(active?Font.BOLD:Font.PLAIN,13f));g.setColor(active?new Color(255,240,203):Color.WHITE);
            String[] lines=labels[i].split(" ");for(int line=0;line<lines.length;line++)centerText(g,lines[line],x,y-(lines.length-1)*8+line*16+5);
        }
        g.setColor(new Color(19,28,34,150));g.fillOval(-59,-59,118,118);
        g.setFont(getFont().deriveFont(Font.BOLD,12f));g.setColor(new Color(255,226,163));centerText(g,hovered<0?count+" selected":labels[hovered],0,-10);
        g.setFont(getFont().deriveFont(Font.PLAIN,10f));g.setColor(Color.WHITE);centerText(g,hovered<0?"Choose a tool":"Click to choose",0,10);centerText(g,"Right-click / Esc closes",0,28);g.dispose();
    }
    private static void centerText(Graphics2D g,String text,int x,int y){g.drawString(text,x-g.getFontMetrics().stringWidth(text)/2,y);}
}
