package com.alderfall.game.editor;

import com.alderfall.game.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/** Tile-space interaction; every mouse gesture is one undo operation. */
final class EditorCanvas extends JPanel {
    private final MapEditorWindow editor;
    private double offsetX=24,offsetY=24;
    private int tileSize=24;
    private Point pressPixel,previousPixel,hover=new Point(-1,-1),start,lastPaint;
    private boolean panning,dragging,additiveSelection;
    private boolean pendingEdits, movingRegion, erasePatch;
    private Point lastPlaced;
    private int offsetProp=-1;
    private WorldProp dragOrigin;
    private final Map<Integer,WorldProp> dragOrigins=new LinkedHashMap<>();
    private final Set<Point> visited=new HashSet<>();
    private final Set<TilePoint> patchVisited=new HashSet<>();
    private EditorRadialMenu radialMenu;
    final JPanel minimap;

    EditorCanvas(MapEditorWindow editor) {
        this.editor=editor;setLayout(null);addComponentListener(new ComponentAdapter(){public void componentResized(ComponentEvent e){if(radialMenu!=null)radialMenu.dismiss();}});setBackground(new Color(26,32,37));setFocusable(true);
        minimap=new JPanel(){
            {setMinimumSize(new Dimension(220,150));setPreferredSize(new Dimension(220,150));setMaximumSize(new Dimension(260,170));addMouseListener(new MouseAdapter(){@Override public void mousePressed(MouseEvent e){
                double scale=Math.min(getWidth()/(double)editor.document.width(),getHeight()/(double)editor.document.height());
                offsetX=EditorCanvas.this.getWidth()/2.0-e.getX()/scale*tileSize;offsetY=EditorCanvas.this.getHeight()/2.0-e.getY()/scale*tileSize;repaintAll();
            }});}
            @Override protected void paintComponent(Graphics graphics){
                super.paintComponent(graphics);MapDocument d=editor.document;Graphics2D g=(Graphics2D)graphics.create();
                double scale=Math.min(getWidth()/(double)d.width(),getHeight()/(double)d.height());g.scale(scale,scale);
                for(int y=0;y<d.height();y++)for(int x=0;x<d.width();x++){g.setColor(Terrain.color(d.tiles[y][x]));g.fillRect(x,y,1,1);}
                g.setColor(new Color(104,75,53));for(CityBuilding b:d.buildings)g.fill(MapDocument.rect(b));
                g.setColor(Color.WHITE);g.setStroke(new BasicStroke((float)(1/scale)));g.draw(new java.awt.geom.Rectangle2D.Double(-offsetX/tileSize,-offsetY/tileSize,EditorCanvas.this.getWidth()/(double)tileSize,EditorCanvas.this.getHeight()/(double)tileSize));g.dispose();
            }
        };
        MouseAdapter mouse=new MouseAdapter(){
            @Override public void mouseMoved(MouseEvent e){hover=tile(e.getPoint());repaint();coordinates();}
            @Override public void mouseExited(MouseEvent e){if(!dragging){hover=new Point(-1,-1);repaint();}}
            @Override public void mousePressed(MouseEvent e){
                requestFocusInWindow();pressPixel=previousPixel=e.getPoint();panning=SwingUtilities.isRightMouseButton(e)||SwingUtilities.isMiddleMouseButton(e);
                if(panning)return;
                erasePatch=e.isShiftDown();additiveSelection=e.isShiftDown();start=tile(e.getPoint());if(!inside(start))return;dragging=true;visited.clear();patchVisited.clear();lastPaint=null;lastPlaced=null;
                movingRegion=!additiveSelection&&tool()==MapEditorWindow.Tool.SELECT&&editor.selection!=null&&editor.selection.contains(start);
                editor.history.begin(editor.document);
                offsetProp=-1;dragOrigin=null;dragOrigins.clear();int oldCount=editor.document.props.size();
                if(tool()==MapEditorWindow.Tool.SELECT){if(!movingRegion)select(start);}else if(tool()!=MapEditorWindow.Tool.RECTANGLE&&tool()!=MapEditorWindow.Tool.LINE)apply(start);
                if(editor.dragMode.getSelectedItem()==MapEditorWindow.DragMode.OFFSET){
                    if(tool()==MapEditorWindow.Tool.PLACE&&editor.document.props.size()>oldCount)offsetProp=oldCount;
                    else if(tool()==MapEditorWindow.Tool.SELECT){
                        var selected=editor.propSelection();if(!selected.isEmpty()){
                            offsetProp=selected.iterator().next();editor.selectedProps.addAll(selected);
                            editor.selectedBuildings.addAll(editor.buildingSelection());editor.selection=null;movingRegion=false;
                        }
                    }
                    if(offsetProp>=0){dragOrigin=editor.document.props.get(offsetProp);dragOrigins.put(offsetProp,dragOrigin);
                        if(tool()==MapEditorWindow.Tool.SELECT)for(int i:editor.propSelection())dragOrigins.put(i,editor.document.props.get(i));}
                }
                flushEdits();
                repaintAll();
            }
            @Override public void mouseDragged(MouseEvent e){
                if(panning){offsetX+=e.getX()-previousPixel.x;offsetY+=e.getY()-previousPixel.y;previousPixel=e.getPoint();repaintAll();return;}
                if(!dragging)return;hover=bounded(tile(e.getPoint()));
                if(offsetProp>=0)dragOffset(e.getPoint());
                else if(tool()==MapEditorWindow.Tool.SELECT&&editor.selectedProp<0&&editor.selectedBuilding<0&&!movingRegion&&!additiveSelection)editor.selection=rectangle(start,hover);
                else if(tool()==MapEditorWindow.Tool.PAINT||tool()==MapEditorWindow.Tool.ERASE||tool()==MapEditorWindow.Tool.PATCH||tool()==MapEditorWindow.Tool.PLACE&&editor.dragMode.getSelectedItem()==MapEditorWindow.DragMode.COPIES){if(lastPaint!=null)line(lastPaint,hover,p -> apply(p));else apply(hover);lastPaint=hover;}
                flushEdits();repaintAll();coordinates();
            }
            @Override public void mouseReleased(MouseEvent e){
                if(panning){panning=false;if(SwingUtilities.isRightMouseButton(e)&&pressPixel.distance(e.getPoint())<4)contextMenu(e);return;}if(!dragging)return;
                Point end=bounded(tile(e.getPoint()));
                try {
                    if(offsetProp>=0)dragOffset(e.getPoint());
                    else if(tool()==MapEditorWindow.Tool.RECTANGLE&&terrainEntry()&&editor.terrainLayer.isSelected()){
                        Rectangle r=rectangle(start,end);for(int y=r.y;y<r.y+r.height;y++)for(int x=r.x;x<r.x+r.width;x++)editor.document.tiles[y][x]=editor.entry.tile();
                    }else if(tool()==MapEditorWindow.Tool.LINE&&terrainEntry()&&editor.terrainLayer.isSelected())line(start,end,p -> editor.document.paint(p.x,p.y,(int)editor.brush.getValue(),editor.entry.tile()));
                    else if(tool()==MapEditorWindow.Tool.SELECT&&(movingRegion||editor.selection==null)&&pressPixel.distance(e.getPoint())>4)moveSelection(end.x-start.x,end.y-start.y);
                }catch(IllegalArgumentException ex){editor.info(ex.getMessage());}
                dragging=false;offsetProp=-1;dragOrigin=null;pendingEdits=false;editor.changed();repaintAll();
            }
            @Override public void mouseWheelMoved(MouseWheelEvent e){
                if(dragging)return;
                int next=Math.max(12,Math.min(96,tileSize+(e.getWheelRotation()<0?4:-4)));double ratio=next/(double)tileSize;
                offsetX=e.getX()-(e.getX()-offsetX)*ratio;offsetY=e.getY()-(e.getY()-offsetY)*ratio;tileSize=next;repaintAll();
            }
        };
        for(int[] delta:new int[][]{{-1,0},{1,0},{0,-1},{0,1}}){
            String direction=delta[0]<0?"LEFT":delta[0]>0?"RIGHT":delta[1]<0?"UP":"DOWN";
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("alt "+direction),"nudge"+direction);
            getActionMap().put("nudge"+direction,new AbstractAction(){public void actionPerformed(ActionEvent e){editor.nudge(delta[0],delta[1]);}});
        }
        setTransferHandler(new TransferHandler(){
            public boolean canImport(TransferSupport support){return support.isDrop()&&support.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.stringFlavor);}
            public boolean importData(TransferSupport support){
                if(!canImport(support))return false;
                try{
                    String ids=(String)support.getTransferable().getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor);
                    Point dropPixel=support.getDropLocation().getDropPoint();Point target=tile(dropPixel);if(!inside(target))return false;
                    var dropped=Arrays.stream(ids.split("\n")).map(id -> editor.entries.stream().filter(a -> a.asset().equals(id)).findFirst().orElse(null)).filter(a -> a!=null&&!a.terrain()).toList();
                    if(dropped.isEmpty())return false;
                    editor.history.begin(editor.document);visited.clear();lastPlaced=null;int x=target.x;
                    editor.tool.setSelectedItem(MapEditorWindow.Tool.PLACE);
                    if(editor.dragMode.getSelectedItem()==MapEditorWindow.DragMode.OFFSET){
                        editor.offsetX.setValue((int)Math.round(((dropPixel.x-offsetX)/tileSize-target.x-.5)*48));
                        editor.offsetY.setValue((int)Math.round(((dropPixel.y-offsetY)/tileSize-target.y-1)*48));
                    }
                    for(var entry:dropped){editor.entry=entry;editor.propSize.setValue(entry.size());apply(new Point(x,target.y));
                        x+=(int)editor.spacing.getValue()+(entry.building()?VillageManager.buildingPlan(entry.style()).width():0);}
                    pendingEdits=false;editor.changed();return true;
                }catch(Exception ex){editor.info("Could not drop asset: "+ex.getMessage());return false;}
            }
        });
        for(String key:new String[]{"DELETE","BACK_SPACE"}){getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key),"deleteObjects");}
        getActionMap().put("deleteObjects",new AbstractAction(){public void actionPerformed(ActionEvent e){editor.deleteSelection();}});
        addMouseListener(mouse);addMouseMotionListener(mouse);addMouseWheelListener(mouse);
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ESCAPE"),"cancel");getActionMap().put("cancel",new AbstractAction(){public void actionPerformed(ActionEvent e){editor.clearSelection();editor.tool.setSelectedItem(MapEditorWindow.Tool.SELECT);repaint();}});
    }
    private MapEditorWindow.Tool tool(){return (MapEditorWindow.Tool)editor.tool.getSelectedItem();}
    private boolean terrainEntry(){return editor.entry!=null&&editor.entry.terrain();}
    private Point tile(Point p){
        double x=(p.x-offsetX)/tileSize,y=(p.y-offsetY)/tileSize;
        if(tool()==MapEditorWindow.Tool.PLACE && editor.entry!=null && editor.entry.asset()!=null
                && editor.entry.asset().startsWith("interior_wall_") && "interior".equals(editor.document.kind)) {
            var state=editor.renderer.editorState();
            TilePoint mount=com.alderfall.game.render.world.ConnectedInteriorWalls.mountAt(state.world,"editor_workshop",x,y);
            return new Point(mount.x(),mount.y());
        }
        return new Point((int)Math.floor(x),(int)Math.floor(y));
    }
    private Point bounded(Point p){return new Point(Math.max(0,Math.min(editor.document.width()-1,p.x)),Math.max(0,Math.min(editor.document.height()-1,p.y)));}
    private boolean inside(Point p){return editor.document.contains(p.x,p.y);}
    private static Rectangle rectangle(Point a,Point b){return new Rectangle(Math.min(a.x,b.x),Math.min(a.y,b.y),Math.abs(a.x-b.x)+1,Math.abs(a.y-b.y)+1);}
    void fit(){tileSize=Math.max(12,Math.min(64,Math.min(Math.max(1,getWidth()-48)/editor.document.width(),Math.max(1,getHeight()-48)/editor.document.height())));offsetX=(getWidth()-editor.document.width()*tileSize)/2.0;offsetY=(getHeight()-editor.document.height()*tileSize)/2.0;repaintAll();}
    private void repaintAll(){repaint();minimap.repaint();}
    private void flushEdits(){if(pendingEdits){pendingEdits=false;editor.sync();}}
    private void coordinates(){if(inside(hover))editor.info(tool()+"  |  "+hover.x+", "+hover.y+"  |  "+Terrain.name(editor.document.tiles[hover.y][hover.x])+"  |  "+tileSize+" px/tile  |  Right drag to pan · Wheel to zoom");}
    private void select(Point p){
        boolean retain=!additiveSelection&&editor.propSelection().size()+editor.buildingSelection().size()>1
                &&(editor.propSelection().contains(editor.document.propIndex(p.x,p.y))||editor.buildingSelection().contains(editor.document.buildingIndex(p.x,p.y)));
        if(!additiveSelection&&!retain)editor.clearSelection();else{
            if(editor.selectedProp>=0)editor.selectedProps.add(editor.selectedProp);
            if(editor.selectedBuilding>=0)editor.selectedBuildings.add(editor.selectedBuilding);
            editor.selectedProp=-1;editor.selectedBuilding=-1;editor.selection=null;
        }
        MapDocument d=editor.document;
        if(editor.propLayer.isSelected()){
            editor.selectedProp=d.propIndex(p.x,p.y);
            if(editor.selectedProp<0||editor.dragMode.getSelectedItem()==MapEditorWindow.DragMode.OFFSET){
                for(int i=d.props.size()-1;i>=0;i--){Rectangle bounds=editor.renderer.editorPropBounds(d.props.get(i),tileSize);
                    if(bounds.contains(pressPixel.x-offsetX,pressPixel.y-offsetY)){editor.selectedProp=i;break;}}
            }
        }
        if(editor.selectedProp<0&&editor.buildingLayer.isSelected())editor.selectedBuilding=d.buildingIndex(p.x,p.y);
        if(additiveSelection){
            if(editor.selectedProp>=0&&!editor.selectedProps.add(editor.selectedProp)){editor.selectedProps.remove(editor.selectedProp);editor.selectedProp=-1;}
            if(editor.selectedBuilding>=0&&!editor.selectedBuildings.add(editor.selectedBuilding)){editor.selectedBuildings.remove(editor.selectedBuilding);editor.selectedBuilding=-1;}
        }
        if(editor.selectedProp>=0){WorldProp prop=d.props.get(editor.selectedProp);var metadata=editor.assetInfo.get(prop.asset());editor.modifiedInfo.setText("Modified: "+(metadata==null?"Unknown":metadata.date()));editor.showProp(prop);editor.selectionInfo.setText("Prop at "+prop.x()+", "+prop.y());editor.preview.setIcon(new ImageIcon(editor.assets.placementSpriteFit(prop.asset(),180,150)));}
        else if(editor.selectedBuilding>=0){CityBuilding b=d.buildings.get(editor.selectedBuilding);editor.loadingInspector=true;try{editor.facing.setSelectedItem(b.facing());}finally{editor.loadingInspector=false;}editor.selectionInfo.setText(VillageManager.buildingLabel(b.style())+" at "+b.x1()+", "+b.y1());}
        else if(!additiveSelection)editor.selection=new Rectangle(p.x,p.y,1,1);
        int count=editor.propSelection().size()+editor.buildingSelection().size();if(count>1)editor.selectionInfo.setText(count+" objects selected");
    }
    private void contextMenu(MouseEvent e){
        Point point=tile(e.getPoint());
        if(inside(point)&&editor.propSelection().isEmpty()&&editor.buildingSelection().isEmpty()){additiveSelection=false;pressPixel=e.getPoint();select(point);}
        if(radialMenu!=null&&radialMenu.isVisible()){radialMenu.dismiss();return;}
        radialMenu=new EditorRadialMenu(editor);radialMenu.showAt(this,e.getPoint());
    }

    private void dragOffset(Point pixel){
        if(dragOrigin==null)return;
        int dx=(int)Math.round((pixel.x-pressPixel.x)*48.0/tileSize),dy=(int)Math.round((pixel.y-pressPixel.y)*48.0/tileSize);
        for(WorldProp p:dragOrigins.values()){WorldProp next=EditorPropPosition.shift(p,dx,dy);if(!editor.document.contains(next.x(),next.y()))return;}
        for(var item:dragOrigins.entrySet())editor.document.props.set(item.getKey(),EditorPropPosition.shift(item.getValue(),dx,dy));
        WorldProp next=editor.document.props.get(offsetProp);editor.setOffsets(next.offsetX(),next.offsetY());
        editor.selectionInfo.setText("Offset "+next.offsetX()+", "+next.offsetY()+" at "+next.x()+", "+next.y());pendingEdits=true;
    }

    private void moveSelection(int dx,int dy){
        MapDocument d=editor.document;
        if(movingRegion&&editor.selection!=null||editor.propSelection().size()+editor.buildingSelection().size()>1){
            var props=editor.propSelection();var buildings=editor.buildingSelection();
            for(int i:props){WorldProp p=d.props.get(i);if(!d.contains(p.x()+dx,p.y()+dy))throw new IllegalArgumentException("Selection must remain inside the map.");}
            for(int i:buildings){var next=MapDocument.translate(d.buildings.get(i),dx,dy);
                if(!d.contains(next.x1(),next.y1())||!d.contains(next.x2(),next.y2()))throw new IllegalArgumentException("Selection must remain inside the map.");
                for(int j=0;j<d.buildings.size();j++)if(!buildings.contains(j)&&MapDocument.rect(next).intersects(MapDocument.rect(d.buildings.get(j))))throw new IllegalArgumentException("Buildings cannot overlap.");}
            for(int i:props){WorldProp p=d.props.get(i);d.props.set(i,new WorldProp(p.x()+dx,p.y()+dy,p.asset(),p.size(),p.visualSlot(),p.offsetX(),p.offsetY()));}
            for(int i:buildings)d.buildings.set(i,MapDocument.translate(d.buildings.get(i),dx,dy));
            if(editor.selection!=null)editor.selection.translate(dx,dy);return;
        }
        if(editor.selectedProp>=0){WorldProp p=d.props.get(editor.selectedProp);if(d.contains(p.x()+dx,p.y()+dy))d.props.set(editor.selectedProp,new WorldProp(p.x()+dx,p.y()+dy,p.asset(),p.size(),p.visualSlot(),p.offsetX(),p.offsetY()));}
        else if(editor.selectedBuilding>=0){
            CityBuilding b=d.buildings.get(editor.selectedBuilding),next=MapDocument.translate(b,dx,dy);
            if(!d.contains(next.x1(),next.y1())||!d.contains(next.x2(),next.y2()))throw new IllegalArgumentException("Building must remain inside the map.");
            for(int i=0;i<d.buildings.size();i++)if(i!=editor.selectedBuilding&&MapDocument.rect(next).intersects(MapDocument.rect(d.buildings.get(i))))throw new IllegalArgumentException("Buildings cannot overlap.");
            d.buildings.set(editor.selectedBuilding,next);
        }
    }
    private void apply(Point p){
        if(!inside(p)||!visited.add(new Point(p)))return;MapDocument d=editor.document;boolean changed=false;
        try {
            switch(tool()) {
                case PAINT -> {if(terrainEntry()&&editor.terrainLayer.isSelected()){d.paint(p.x,p.y,(int)editor.brush.getValue(),editor.entry.tile());changed=true;}lastPaint=p;}
                case FILL -> {if(terrainEntry()&&editor.terrainLayer.isSelected()){d.fill(p.x,p.y,editor.entry.tile());changed=true;}}
                case PLACE -> {
                    var e=editor.entry;if(e==null||e.terrain())break;
                    if(lastPlaced!=null&&Math.max(Math.abs(p.x-lastPlaced.x),Math.abs(p.y-lastPlaced.y))<(int)editor.spacing.getValue())break;
                    if(e.building()&&editor.buildingLayer.isSelected()){
                        changed=d.placeBuilding(e.style(),p.x,p.y,(CityBuilding.Facing)editor.facing.getSelectedItem());if(!changed)editor.info("Building overlaps a lot or extends outside the map.");
                    }else if(!e.building()&&editor.propLayer.isSelected()){d.props.add(new WorldProp(p.x,p.y,e.asset(),(int)editor.propSize.getValue(),-1,(int)editor.offsetX.getValue(),(int)editor.offsetY.getValue()));changed=true;}
                    if(changed)lastPlaced=new Point(p);
                }
                case PATCH -> {
                    if(!editor.propLayer.isSelected())break;
                    var selected=editor.palette.getSelectedValuesList().stream().filter(a -> !a.terrain()&&!a.building()&&PropPlacement.kind(a.asset())==PropPlacement.Kind.COVER).map(EditorPalette.Entry::asset).toList();
                    if(selected.isEmpty()){editor.info("Select grass, flowers, ferns or other ground cover for PATCH.");break;}
                    changed=EditorPatchBrush.paint(d,p.x,p.y,(int)editor.brush.getValue(),(int)editor.density.getValue(),(int)editor.variation.getValue(),(int)editor.propSize.getValue(),(int)editor.offsetX.getValue(),(int)editor.offsetY.getValue(),selected,editor.protectGround.isSelected(),patchVisited);
                }
                case ERASE -> {
                    if(erasePatch&&editor.propLayer.isSelected()){changed=d.props.removeIf(a -> a.x()==p.x&&a.y()==p.y&&a.visualSlot()>=128&&a.visualSlot()<=131);editor.clearSelection();break;}
                    int prop=d.propIndex(p.x,p.y),building=d.buildingIndex(p.x,p.y);
                    if(prop>=0&&editor.propLayer.isSelected())d.props.remove(prop);
                    else if(building>=0&&editor.buildingLayer.isSelected())d.buildings.remove(building);
                    else if(editor.markerLayer.isSelected())d.landmarks.remove(new TilePoint(p.x,p.y));
                    editor.clearSelection();changed=true;
                }
                case SPAWN -> {d.spawnX=p.x;d.spawnY=p.y;changed=true;}
                case LANDMARK -> {if(editor.markerLayer.isSelected()){String label=editor.markerText.getText().trim();if(!label.isEmpty()){d.landmarks.put(new TilePoint(p.x,p.y),label);changed=true;}}}
                case STAMP -> {if(editor.clipboard!=null){d.stamp(editor.clipboard,p.x,p.y,editor.stampTerrain.isSelected());changed=true;}}
                default -> { }
            }
        }catch(IllegalArgumentException ex){editor.info(ex.getMessage());}
        if(tool()==MapEditorWindow.Tool.PAINT||tool()==MapEditorWindow.Tool.ERASE||tool()==MapEditorWindow.Tool.PATCH||tool()==MapEditorWindow.Tool.PLACE)lastPaint=p;
        pendingEdits|=changed;
    }
    private static void line(Point a,Point b,java.util.function.Consumer<Point> apply){
        int dx=Math.abs(b.x-a.x),dy=Math.abs(b.y-a.y),sx=a.x<b.x?1:-1,sy=a.y<b.y?1:-1,err=dx-dy,x=a.x,y=a.y;
        while(true){apply.accept(new Point(x,y));if(x==b.x&&y==b.y)break;int e2=err*2;if(e2>-dy){err-=dy;x+=sx;}if(e2<dx){err+=dx;y+=sy;}}
    }
    @Override protected void paintComponent(Graphics graphics){
        super.paintComponent(graphics);Graphics2D g=(Graphics2D)graphics.create();MapDocument d=editor.document;
        int minX=Math.max(0,(int)Math.floor(-offsetX/tileSize)),minY=Math.max(0,(int)Math.floor(-offsetY/tileSize));
        int maxX=Math.min(d.width(),(int)Math.ceil((getWidth()-offsetX)/tileSize)),maxY=Math.min(d.height(),(int)Math.ceil((getHeight()-offsetY)/tileSize));
        Graphics2D world=(Graphics2D)g.create();world.translate(offsetX+minX*tileSize,offsetY+minY*tileSize);
        if(maxX>minX&&maxY>minY)editor.renderer.renderEditorScene(world,minX,minY,maxX-minX,maxY-minY,tileSize,editor.terrainLayer.isSelected(),editor.buildingLayer.isSelected(),editor.propLayer.isSelected());world.dispose();
        g.translate(offsetX,offsetY);
        if(editor.collision.isSelected()){
            g.setColor(new Color(230,68,73,90));for(int y=minY;y<maxY;y++)for(int x=minX;x<maxX;x++)if(!editor.renderer.editorState().world.isPassable("editor_workshop",x,y))g.fillRect(x*tileSize,y*tileSize,tileSize,tileSize);
        }
        if(editor.collision.isSelected()){
            var worldState=editor.renderer.editorState();g.setStroke(new BasicStroke(1.5f));
            for(WorldProp p:d.props){var bounds=PropCollision.footprint(worldState.world,"editor_workshop",p);if(bounds==null)continue;
                var shape=new java.awt.geom.Rectangle2D.Double(bounds.x*tileSize,bounds.y*tileSize,bounds.width*tileSize,bounds.height*tileSize);
                g.setColor(new Color(255,123,55,85));g.fill(shape);g.setColor(new Color(255,164,70));g.draw(shape);}
        }
        if(editor.grid.isSelected()){
            g.setColor(new Color(255,255,255,35));for(int x=minX;x<=maxX;x++)g.drawLine(x*tileSize,minY*tileSize,x*tileSize,maxY*tileSize);for(int y=minY;y<=maxY;y++)g.drawLine(minX*tileSize,y*tileSize,maxX*tileSize,y*tileSize);
        }
        if(tool()==MapEditorWindow.Tool.PATCH&&tileSize>=24){
            g.setColor(new Color(175,220,160,50));
            for(int x=minX;x<maxX;x++)g.drawLine(x*tileSize+tileSize/2,minY*tileSize,x*tileSize+tileSize/2,maxY*tileSize);
            for(int y=minY;y<maxY;y++)g.drawLine(minX*tileSize,y*tileSize+tileSize/2,maxX*tileSize,y*tileSize+tileSize/2);
        }
        if(editor.markerLayer.isSelected()){
            for(var entry:d.landmarks.entrySet()){int x=entry.getKey().x()*tileSize,y=entry.getKey().y()*tileSize;g.setColor(new Color(239,199,98));g.fillOval(x+tileSize/3,y+tileSize/3,Math.max(4,tileSize/3),Math.max(4,tileSize/3));if(tileSize>=24){g.setColor(new Color(15,20,25,210));g.fillRect(x,y-16,g.getFontMetrics().stringWidth(entry.getValue())+8,17);g.setColor(Color.WHITE);g.drawString(entry.getValue(),x+4,y-3);}}
            g.setColor(new Color(71,227,180));g.setStroke(new BasicStroke(3));g.drawOval(d.spawnX*tileSize+2,d.spawnY*tileSize+2,tileSize-4,tileSize-4);g.drawString("S",d.spawnX*tileSize+tileSize/3,d.spawnY*tileSize+tileSize*3/4);
        }
        if(editor.selection!=null)outline(g,editor.selection,new Color(87,191,248));
        if(editor.selectedBuilding>=0&&editor.selectedBuilding<d.buildings.size())outline(g,MapDocument.rect(d.buildings.get(editor.selectedBuilding)),new Color(87,191,248));
        for(int index:editor.propSelection())if(index<d.props.size()){
            Rectangle bounds=editor.renderer.editorPropBounds(d.props.get(index),tileSize);g.setColor(new Color(87,191,248));g.setStroke(new BasicStroke(2));g.draw(bounds);
        }
        for(int index:editor.buildingSelection())if(index<d.buildings.size())outline(g,MapDocument.rect(d.buildings.get(index)),new Color(87,191,248));
        if(inside(hover)) {
            Rectangle box=new Rectangle(hover.x,hover.y,1,1);
            if(tool()==MapEditorWindow.Tool.PAINT||tool()==MapEditorWindow.Tool.PATCH){int size=(int)editor.brush.getValue();box=new Rectangle(hover.x-size/2,hover.y-size/2,size,size);}
            else if(tool()==MapEditorWindow.Tool.STAMP&&editor.clipboard!=null)box=new Rectangle(hover.x,hover.y,editor.clipboard.width(),editor.clipboard.height());
            else if(tool()==MapEditorWindow.Tool.PLACE&&editor.entry!=null&&editor.entry.building()) {var b=VillageManager.buildingPlan(editor.entry.style());boolean sideways=editor.facing.getSelectedItem()==CityBuilding.Facing.EAST||editor.facing.getSelectedItem()==CityBuilding.Facing.WEST;box=new Rectangle(hover.x,hover.y,sideways?b.depth():b.width(),sideways?b.width():b.depth());}
            if(dragging&&tool()==MapEditorWindow.Tool.RECTANGLE)box=rectangle(start,hover);
            outline(g,box,new Color(255,217,117));
            if(tool()==MapEditorWindow.Tool.PLACE&&editor.entry!=null&&!editor.entry.terrain()&&offsetProp<0){
                int size=editor.entry.building()?Math.max(box.width,box.height)*tileSize:(int)editor.propSize.getValue()*tileSize/48;
                Composite old=g.getComposite();g.setComposite(AlphaComposite.SrcOver.derive(.55f));g.drawImage(editor.assets.placementSpriteFit(editor.entry.asset(),Math.max(1,size),Math.max(1,size)),hover.x*tileSize+(tileSize-size)/2+(editor.entry.building()?0:(int)editor.offsetX.getValue()*tileSize/48),hover.y*tileSize+tileSize-size+(editor.entry.building()?0:(int)editor.offsetY.getValue()*tileSize/48),null);g.setComposite(old);
            }
            if(dragging&&tool()==MapEditorWindow.Tool.LINE){g.setColor(new Color(255,217,117));g.setStroke(new BasicStroke(3));g.drawLine(start.x*tileSize+tileSize/2,start.y*tileSize+tileSize/2,hover.x*tileSize+tileSize/2,hover.y*tileSize+tileSize/2);}
        }
        g.dispose();
    }
    private void outline(Graphics2D g,Rectangle box,Color color){g.setColor(new Color(color.getRed(),color.getGreen(),color.getBlue(),35));g.fillRect(box.x*tileSize,box.y*tileSize,box.width*tileSize,box.height*tileSize);g.setColor(color);g.setStroke(new BasicStroke(2));g.drawRect(box.x*tileSize,box.y*tileSize,box.width*tileSize,box.height*tileSize);}
}
