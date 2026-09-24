package com.alderfall.game.editor;

import com.alderfall.game.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Native Swing interaction check; uses an undisplayed window, never desktop input. */
public final class MapEditorInteractionTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args) throws Exception {
        if(GraphicsEnvironment.isHeadless())throw new IllegalStateException("Run with -Djava.awt.headless=false on a desktop.");
        SwingUtilities.invokeAndWait(() -> {
            MapEditorWindow window=new MapEditorWindow(Path.of("."));
            try {
                window.pack();window.setSize(1380,900);layout(window);
                check(window.category.getSelectedItem().equals("All assets"),"Full library is the default");
                check(window.palette.getFixedCellWidth()>0,"Library layout must not decode every thumbnail");
                window.search.setText("wolf");check(window.palette.getModel().getSize()>0,"Fauna found by global search");
                window.search.setText("environments/settlements/city/buildings/north");check(window.palette.getModel().getSize()>0,"Tileset found by source path");
                window.search.setText("");
                window.document=new MapDocument("Interaction study","village",32,24,'g');window.history=new EditorHistory(window.document);window.sync();window.canvas.fit();
                window.entry=new EditorPalette.Entry("Water","Terrain","water","",48,'w');window.tool.setSelectedItem(MapEditorWindow.Tool.PAINT);
                gesture(window,5,5,9,5);
                for(int x=5;x<=9;x++)check(window.document.tiles[5][x]=='w',"Continuous drag must not leave gaps");
                window.undo();check(window.document.tiles[5][5]=='g'&&window.document.tiles[5][9]=='g',"Single undo removes entire gesture");
                window.redo();check(window.document.tiles[5][7]=='w',"Redo restores gesture");
                window.tool.setSelectedItem(MapEditorWindow.Tool.RECTANGLE);gesture(window,2,2,4,4);check(window.document.tiles[3][3]=='w',"Rectangle painting");
                window.tool.setSelectedItem(MapEditorWindow.Tool.SPAWN);gesture(window,12,12,12,12);check(window.document.spawnX==12&&window.document.spawnY==12,"Spawn placement");
                var prop=VillageManager.outdoorAssets().get(0);
                window.entry=new EditorPalette.Entry(prop.label(),prop.category(),prop.asset(),"",prop.size(),(char)0);window.tool.setSelectedItem(MapEditorWindow.Tool.PLACE);gesture(window,15,10,15,10);
                check(window.document.props.size()==1,"Asset placement");
                window.tool.setSelectedItem(MapEditorWindow.Tool.SELECT);gesture(window,15,10,17,11);check(window.document.props.get(0).x()==17&&window.document.props.get(0).y()==11,"Drag object move");
                gesture(window,16,10,18,12);window.copy();check(window.clipboard!=null&&window.clipboard.props.size()==1,"Region copy");
                window.tool.setSelectedItem(MapEditorWindow.Tool.STAMP);gesture(window,23,16,23,16);check(window.document.props.size()==2,"Prefab stamping");
                window.tool.setSelectedItem(MapEditorWindow.Tool.ERASE);gesture(window,17,11,17,11);check(window.document.props.size()==1,"Erase object");
                window.undo();check(window.document.props.size()==2,"Undo erase");
                window.document=new MapDocument("Bulk study","village",32,24,'g');window.history=new EditorHistory(window.document);window.clearSelection();window.sync();window.canvas.fit();
                window.tool.setSelectedItem(MapEditorWindow.Tool.PLACE);window.spacing.setValue(2);window.offsetX.setValue(12);window.offsetY.setValue(-6);
                gesture(window,3,3,9,3);check(window.document.props.size()==4,"Drag spacing places four copies");
                check(window.document.props.stream().allMatch(p -> p.offsetX()==12&&p.offsetY()==-6),"Drag copies keep offsets");
                window.undo();check(window.document.props.isEmpty(),"Whole object stroke is one undo");window.redo();
                window.tool.setSelectedItem(MapEditorWindow.Tool.SELECT);gesture(window,3,3,3,3);window.nudge(1,0);
                check(window.document.props.get(0).offsetX()==13,"Nudge selected prop");window.undo();check(window.document.props.get(0).offsetX()==12,"Undo nudge");
                window.clearSelection();gesture(window,0,0,10,4);gesture(window,0,0,2,2);
                check(window.document.props.get(0).x()==5&&window.document.props.get(3).x()==11,"Drag rectangle moves group");
                window.category.setSelectedItem("Building sprites");check(window.subcategory.getItemCount()>2,"Regional subcategories populated");
                window.subcategory.setSelectedItem("North");check(window.palette.getModel().getSize()>0,"North filter has assets");
                for(int j=0;j<window.palette.getModel().getSize();j++)check(window.palette.getModel().getElementAt(j).category().equals("Building sprites / North"),"Subcategory excludes other regions");
                window.category.setSelectedItem("All assets");window.sort.setSelectedItem("Newest modified");
                check(!window.modifiedInfo.getText().contains("Unknown"),"Source modification time is available");
                window.document=new MapDocument("Controls study","village",32,24,'g');window.history=new EditorHistory(window.document);window.clearSelection();window.sync();window.canvas.fit();
                window.entry=new EditorPalette.Entry("Rock","Nature","deco_rock","",48,(char)0);window.propSize.setValue(48);window.setOffsets(0,0);
                window.tool.setSelectedItem(MapEditorWindow.Tool.PLACE);window.dragMode.setSelectedItem(MapEditorWindow.DragMode.OFFSET);
                Point origin=pixel(window,5,5),right=pixel(window,6,5);int tile=right.x-origin.x;
                gesturePixels(window,origin,new Point(origin.x+tile/4,origin.y-tile/4),0);
                check(window.document.props.size()==1,"Offset drag places one prop, never copies");
                WorldProp shifted=window.document.props.get(0);check(shifted.offsetX()>0&&shifted.offsetY()<0,"Drag direction sets both offsets");
                window.undo();check(window.document.props.isEmpty(),"Offset placement is one undo");window.redo();
                window.document.props.clear();window.document.props.add(new WorldProp(5,5,"deco_rock",48));window.document.props.add(new WorldProp(12,5,"deco_rock",64));window.history=new EditorHistory(window.document);window.clearSelection();window.sync();
                window.tool.setSelectedItem(MapEditorWindow.Tool.SELECT);window.dragMode.setSelectedItem(MapEditorWindow.DragMode.SINGLE);
                gesture(window,5,5,5,5);gesturePixels(window,pixel(window,12,5),pixel(window,12,5),MouseEvent.SHIFT_DOWN_MASK);
                check(window.propSelection().size()==2,"Shift click selects two props");
                check(window.document.props.get(0).size()==48,"Selecting another object never edits the first");
                window.propSize.setValue(96);check(window.document.props.stream().allMatch(p -> p.size()==96),"Live size edits apply to all selected props");
                window.undo();check(window.document.props.get(0).size()==48&&window.document.props.get(1).size()==64,"Undo restores each original property");
                gesture(window,5,5,5,5);gesturePixels(window,pixel(window,12,5),pixel(window,12,5),MouseEvent.SHIFT_DOWN_MASK);
                window.canvas.getActionMap().get("deleteObjects").actionPerformed(new ActionEvent(window.canvas,0,"delete"));
                check(window.document.props.isEmpty(),"Delete removes multiple selected objects");window.undo();check(window.document.props.size()==2,"One undo restores deleted selection");
                window.clearSelection();window.selectedProps.add(0);window.selectedProps.add(1);window.selectedProp=0;window.showProp(window.document.props.get(0));
                window.offsetX.setValue(8);check(window.document.props.stream().allMatch(p -> p.offsetX()==8),"Multi-selection offsets update together");
                window.document.props.set(1,new WorldProp(12,5,"deco_rock",64,-1,-4,0));window.sync();
                window.dragMode.setSelectedItem(MapEditorWindow.DragMode.OFFSET);window.tool.setSelectedItem(MapEditorWindow.Tool.SELECT);
                Point groupStart=pixel(window,5,5);gesturePixels(window,groupStart,new Point(groupStart.x+tile/4,groupStart.y),0);
                WorldProp first=window.document.props.get(0),second=window.document.props.get(1);
                check(first.offsetX()>8&&second.offsetX()>-4,"Offset drag moves every selected prop");
                check(first.offsetX()-second.offsetX()==12,"Group drag preserves relative offsets");
                window.undo();check(window.document.props.get(0).offsetX()==8&&window.document.props.get(1).offsetX()==-4,"Group offset gesture is one undo");
                window.selectedProps.add(0);window.selectedProps.add(1);window.selectedProp=0;
                EditorRadialMenu radial=new EditorRadialMenu(window);radial.setSize(radial.getPreferredSize());layout(radial);
                BufferedImage radialImage=new BufferedImage(radial.getWidth(),radial.getHeight(),BufferedImage.TYPE_INT_ARGB);Graphics2D rg=radialImage.createGraphics();radial.printAll(rg);rg.dispose();
                check(radial.segmentAt(165,55)==0&&radial.segmentAt(275,165)==2&&radial.segmentAt(165,165)==-1,"Wheel segments match visible wedges and exclude center");
                int normal=radialImage.getRGB(285,170);check((normal>>>24)>0&&(normal>>>24)<255,"Wheel segments are translucent");
                check(radialImage.getRGB(0,0)==0,"Wheel corners leave the map visible");
                radial.dispatchEvent(new MouseEvent(radial,MouseEvent.MOUSE_MOVED,System.currentTimeMillis(),0,275,165,0,false));
                radialImage=new BufferedImage(radial.getWidth(),radial.getHeight(),BufferedImage.TYPE_INT_ARGB);rg=radialImage.createGraphics();radial.printAll(rg);rg.dispose();
                check(normal!=radialImage.getRGB(285,170),"Hovered segment is visibly highlighted");
                radial.dispatchEvent(new MouseEvent(radial,MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),MouseEvent.BUTTON1_DOWN_MASK,275,165,1,false,MouseEvent.BUTTON1));
                check(!radial.isVisible()&&window.dragMode.getSelectedItem()==MapEditorWindow.DragMode.COPIES&&window.tool.getSelectedItem()==MapEditorWindow.Tool.PLACE,"Segment selects mode and closes wheel");
                EditorRadialMenu toggle=new EditorRadialMenu(window);toggle.showAt(window.canvas,new Point(200,200));
                int propCount=window.document.props.size();toggle.dispatchEvent(new MouseEvent(toggle,MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),MouseEvent.BUTTON3_DOWN_MASK,200,200,1,false,MouseEvent.BUTTON3));
                check(!toggle.isVisible()&&toggle.getParent()==null&&window.document.props.size()==propCount,"Right click dismisses overlay without placing objects");
                try{Path radialPath=Path.of("temp/editor-controls/radial-menu.png");Files.createDirectories(radialPath.getParent());ImageIO.write(radialImage,"png",radialPath.toFile());}catch(Exception ex){throw new RuntimeException(ex);}
                window.document=EditorLayouts.create("Alderfall City Workshop","city",64,48,42,true);window.history=new EditorHistory(window.document);window.clearSelection();window.palette.clearSelection();window.palette.setSelectedIndex(0);window.tool.setSelectedItem(MapEditorWindow.Tool.SELECT);window.sync();window.canvas.fit();
                BufferedImage image=new BufferedImage(window.getRootPane().getWidth(),window.getRootPane().getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D graphics=image.createGraphics();window.getRootPane().printAll(graphics);graphics.dispose();
                check(image.getRGB(20,50)!=Color.BLACK.getRGB(),"Workshop capture contains the rendered interface");
                try{Path imagePath=Path.of("temp/map-editor/tests/workshop.png");Files.createDirectories(imagePath.getParent());ImageIO.write(image,"png",imagePath.toFile());}catch(Exception ex){throw new RuntimeException(ex);}
                System.out.println("PASS: native Swing painting, brush gestures, undo/redo, selection, object drag, prefab stamping and erase");
            } finally {window.renderer.shutdown();window.dispose();}
        });
    }
    private static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container nested)layout(nested);}
    private static Point pixel(MapEditorWindow w,int x,int y){int tile=Math.max(12,Math.min(64,Math.min(Math.max(1,w.canvas.getWidth()-48)/w.document.width(),Math.max(1,w.canvas.getHeight()-48)/w.document.height())));return new Point((w.canvas.getWidth()-w.document.width()*tile)/2+x*tile+tile/2,(w.canvas.getHeight()-w.document.height()*tile)/2+y*tile+tile/2);}
    private static void gesture(MapEditorWindow w,int x1,int y1,int x2,int y2){
        gesturePixels(w,pixel(w,x1,y1),pixel(w,x2,y2),0);
    }
    private static void gesturePixels(MapEditorWindow w,Point a,Point b,int modifiers){
        long time=System.currentTimeMillis();
        w.canvas.dispatchEvent(new MouseEvent(w.canvas,MouseEvent.MOUSE_PRESSED,time,modifiers|MouseEvent.BUTTON1_DOWN_MASK,a.x,a.y,1,false,MouseEvent.BUTTON1));
        w.canvas.dispatchEvent(new MouseEvent(w.canvas,MouseEvent.MOUSE_DRAGGED,time+10,modifiers|MouseEvent.BUTTON1_DOWN_MASK,b.x,b.y,0,false,MouseEvent.NOBUTTON));
        w.canvas.dispatchEvent(new MouseEvent(w.canvas,MouseEvent.MOUSE_RELEASED,time+20,modifiers,b.x,b.y,1,false,MouseEvent.BUTTON1));
    }
}
