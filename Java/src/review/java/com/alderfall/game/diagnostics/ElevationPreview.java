package com.alderfall.game;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.*;
import javax.imageio.metadata.IIOMetadataNode;
import javax.swing.*;

/** Uses the actual game ground renderer/assets, with an experimental elevation projection. */
public final class ElevationPreview {
    private static final int HEADER = 78, FOOTER = 54;
    private final ElevationStudy field = new ElevationStudy();
    private final AssetStore assets = new AssetStore(Path.of("assets"));
    private final BufferedImage ground;
    private final BufferedImage scenery;
    private final BufferedImage surface;
    private final int[][] depth = new int[768][1056];
    private final List<TilePoint> route;
    private record Prop(double x, double y, String asset, int width, int height) { }
    private final List<Prop> props = List.of(
        new Prop(6.7,6.2,"deco_tree_oak",120,140),
        new Prop(16.5,6.1,"deco_tree_oak",115,135),
        new Prop(12.0,5.5,"deco_soft_mossy_rock",48,40),
        new Prop(10,10.4,"deco_grass_wildflowers",50,35),
        new Prop(16,10.6,"deco_soft_mossy_rock",55,42),
        new Prop(3,10,"deco_tree_oak",120,145),
        new Prop(19.5,12,"deco_tree_oak",125,145),
        new Prop(5,14,"deco_grass_wildflowers",60,38),
        new Prop(14.7,6.8,"deco_grass_wildflowers",45,32));

    private ElevationPreview() throws Exception {
        ground = captureGround();
        surface = projectSurface();
        route = new ArrayList<>();
        route.add(new TilePoint(13,14));
        route.addAll(field.path(route.getFirst(),new TilePoint(14,5)));
        scenery = renderWorld(-1,-1,false);
    }
    private BufferedImage captureGround() throws Exception {
        GamePanel panel = new GamePanel(Path.of("").toAbsolutePath());
        try {
            ((Timer)get(panel,"timer")).stop();
            GameState state = (GameState)get(panel,"state");
            state.currentMapId = state.world.createEditorMap("editor_elevation_study","Elevation study","village",22,16);
            var area = state.world.area(state.currentMapId);
            area.fillTiles(0,0,21,15,'g');
            // Quiet earth paths make the two ramps readable without a grid overlay.
            area.fillTiles(8,11,8,15,'r');
            area.fillTiles(8,10,13,10,'r');
            area.fillTiles(13,6,13,10,'r');
            var result = new BufferedImage(22*48,16*48,BufferedImage.TYPE_INT_RGB);
            var g = result.createGraphics();
            ((WorldRenderer)get(panel,"worldRenderer")).drawTerrainBase(g,
                new WorldRenderer.TerrainContext(state,0,0,22,16,22,16,48,100,"village"));
            g.dispose(); return result;
        } finally { panel.shutdown(); }
    }
    private static Object get(Object target,String name) throws Exception {
        var f = target.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(target);
    }
    private static int rgb(double r,double g,double b) {
        return 0xff000000 | ((int)Math.max(0,Math.min(255,r))<<16)
            | ((int)Math.max(0,Math.min(255,g))<<8) | (int)Math.max(0,Math.min(255,b));
    }
    private static double grain(int x,int y) {
        int n=x*374761393+y*668265263;n=(n^(n>>>13))*1274126177;
        return ((n^(n>>>16))&255)/255.0;
    }
    private static int shade(int color,double factor) {
        return rgb(((color>>16)&255)*factor,((color>>8)&255)*factor,(color&255)*factor);
    }
    private BufferedImage projectSurface() {
        var image=new BufferedImage(1056,768,BufferedImage.TYPE_INT_RGB);
        // Project each continuous world-space scanline. No tile faces or repeated cliff sprites.
        for(int x=0;x<1056;x++) {
            double previous=0;int last=-1,shadowEnd=-1;
            for(int y=0;y<768;y++) {
                double h=field.height((x+.5)/48,(y+.5)/48);
                int sy=(int)Math.round(y-h*ElevationStudy.RISE);
                int color=ground.getRGB(x,y);
                boolean cliff=previous-h>.1;
                if(cliff) {
                    int length=sy-last;
                    for(int v=last+1;v<sy;v++) if(v>=0 && v<768) {
                        double t=(v-last)/(double)length;
                        double strata=Math.sin(t*19+Math.sin(x*.028)*2)*3;
                        double noise=(grain(x,v)-.5)*15+(grain(x/4,v/3)-.5)*10;
                        double light=1-.24*t;
                        int bank=rgb((116+strata+noise)*light,(96+strata+noise)*light,(64+strata+noise)*light);
                        // Sparse embedded stones, not a repeated rock-wall pattern.
                        int gx=x/11,gy=(v+(int)(Math.sin(x*.08)*3))/7;
                        if(grain(gx,gy)>.77 && Math.pow((x%11-5)/4.0,2)+Math.pow((v%7-3)/2.2,2)<1)
                            bank=rgb(133+noise-20*t,125+noise-20*t,98+noise-20*t);
                        int fringe=Math.min(Math.max(1,length/3),2+(int)(grain(x/3,y)*4));
                        if(v-last<=fringe) bank=shade(ground.getRGB(x,Math.max(0,y-1)),.67+.1*grain(x,v));
                        image.setRGB(x,v,bank);depth[v][x]=y;
                    }
                    shadowEnd=sy+(int)Math.max(3,9*(previous-h));
                }
                double lighting=1+h*.025;
                if(sy<shadowEnd) lighting*=.76+.24*(1-(shadowEnd-sy)/9.0);
                // A soft grass bevel follows the contour, including the rear and side edges.
                if(h>0 && (field.height((x-4.0)/48,(y+.5)/48)<h-.1
                    ||field.height((x+4.0)/48,(y+.5)/48)<h-.1)) lighting*=.86;
                if(h-previous>.1) lighting*=.88;
                color=shade(color,lighting);
                int begin=cliff || sy<=last ? sy : last+1;
                for(int v=begin;v<=sy;v++) if(v>=0 && v<768) {
                    image.setRGB(x,v,color);depth[v][x]=y;
                }
                previous=h;last=sy;
            }
        }
        return image;
    }
    private void drawProp(Graphics2D g,Prop p) {
        int sx=(int)(p.x*48),sy=(int)(p.asset.startsWith("class_")
            ?field.actorScreenY(p.x,p.y):field.screenY(p.x,p.y));
        int left=sx-p.width/2,top=sy-p.height;
        var sprite=new BufferedImage(p.width,p.height+8,BufferedImage.TYPE_INT_ARGB);
        var sg=sprite.createGraphics();
        sg.setColor(new Color(12,22,12,75));sg.fillOval(p.width/4,p.height-5,p.width/2,10);
        sg.drawImage(assets.spriteFit(p.asset,p.width,p.height),0,0,p.width,p.height,null);sg.dispose();
        // Terrain in front of a sprite must still occlude it after continuous projection.
        for(int y=0;y<sprite.getHeight();y++)for(int x=0;x<sprite.getWidth();x++) {
            int wx=left+x,wy=top+y;
            if(wx>=0 && wx<1056 && wy>=0 && wy<768 && depth[wy][wx]>p.y*48+6)
                sprite.setRGB(x,y,0);
        }
        g.drawImage(sprite,left,top,null);
    }
    private BufferedImage renderWorld(double actorX,double actorY,boolean showRoute) {
        var image=new BufferedImage(1056,768,BufferedImage.TYPE_INT_RGB);
        var g=image.createGraphics();g.drawImage(surface,0,0,null);
        if(showRoute) for(var p:route) {
            int sx=p.x()*48+24,sy=(int)field.screenY(p.x()+.5,p.y()+.5);
            g.setColor(new Color(246,219,134,170));g.fillOval(sx-3,sy-3,6,6);
        }
        var ordered=new ArrayList<>(props);
        if(actorY>=0)ordered.add(new Prop(actorX,actorY,"class_knight_model_down",60,86));
        ordered.sort(java.util.Comparator.comparingDouble(Prop::y));
        for(var p:ordered)drawProp(g,p);
        g.dispose();return image;
    }
    private BufferedImage frame(double x,double y,boolean routeVisible) {
        var image = new BufferedImage(1056,768+HEADER+FOOTER,BufferedImage.TYPE_INT_RGB);
        var g=image.createGraphics();g.setColor(new Color(22,29,26));g.fillRect(0,0,image.getWidth(),image.getHeight());
        g.drawImage(x<0 ? scenery : renderWorld(x,y,routeVisible),0,HEADER,null);
        g.setColor(new Color(234,227,204));g.setFont(new Font("SansSerif",Font.BOLD,25));
        g.drawString("ECHOES OF ALDERFALL  /  Raised ground",28,34);
        g.setFont(new Font("SansSerif",Font.PLAIN,14));g.setColor(new Color(170,183,163));
        g.drawString("Terrain experiment     /     Quarter-height steps     /     Ramps and tall cliffs",28,59);
        g.drawString("Small grass ledges can be stepped up or down. Full-height banks still require a ramp or stepped route.",28,HEADER+796);
        g.dispose();return image;
    }
    private void export(Path output) throws Exception {
        Files.createDirectories(output);
        ImageIO.write(frame(13.5,14.5,false),"png",output.resolve("plateau.png").toFile());
        ImageIO.write(frame(14.5,5.5,true),"png",output.resolve("route.png").toFile());
        var detail=new BufferedImage(800,490,BufferedImage.TYPE_INT_RGB);
        var dg=detail.createGraphics();
        dg.drawImage(renderWorld(13.5,12.8,false),0,0,800,490,470,475,870,720,null);dg.dispose();
        ImageIO.write(detail,"png",output.resolve("steps.png").toFile());
        var writer=ImageIO.getImageWritersByFormatName("gif").next();
        try(var stream=ImageIO.createImageOutputStream(output.resolve("ascent.gif").toFile())) {
            writer.setOutput(stream);writer.prepareWriteSequence(null);
            int frames=(route.size()-1)*5+15;
            for(int i=0;i<frames;i++) {
                double step=Math.min(route.size()-1,i/5.0);
                int a=(int)step,b=Math.min(route.size()-1,a+1);double t=step-a;
                var p=route.get(a);var q=route.get(b);
                var full=frame(p.x()+.5+(q.x()-p.x())*t,p.y()+.5+(q.y()-p.y())*t,true);
                var img=new BufferedImage(528,450,BufferedImage.TYPE_INT_RGB);
                var scaled=img.createGraphics();
                scaled.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                scaled.drawImage(full,0,0,528,450,null);scaled.dispose();
                var meta=writer.getDefaultImageMetadata(new ImageTypeSpecifier(img),null);
                String format=meta.getNativeMetadataFormatName();
                var root=(IIOMetadataNode)meta.getAsTree(format);
                var control=(IIOMetadataNode)root.getElementsByTagName("GraphicControlExtension").item(0);
                control.setAttribute("delayTime","10");control.setAttribute("disposalMethod","none");
                if(i==0) {
                    var extensions=new IIOMetadataNode("ApplicationExtensions");
                    var loop=new IIOMetadataNode("ApplicationExtension");
                    loop.setAttribute("applicationID","NETSCAPE");loop.setAttribute("authenticationCode","2.0");
                    loop.setUserObject(new byte[]{1,0,0});extensions.appendChild(loop);root.appendChild(extensions);
                }
                meta.setFromTree(format,root);writer.writeToSequence(new IIOImage(img,null,meta),null);
            }
            writer.endWriteSequence();
        } finally { writer.dispose(); }
    }
    private void openWindow() {
        JFrame window=new JFrame("Elevation study — click to walk; arrows / WASD to step");
        JPanel panel=new JPanel() {
            TilePoint player=new TilePoint(13,14);
            List<TilePoint> path=List.of(); double progress=0;
            final Timer timer=new Timer(40,e->{
                if(!path.isEmpty()) {
                    progress+=.13;
                    if(progress>=1) {player=path.getFirst();path=path.subList(1,path.size());progress=0;}
                    repaint();
                }
            });
            {
                setPreferredSize(new Dimension(1056,900));setFocusable(true);
                addMouseListener(new MouseAdapter(){@Override public void mousePressed(MouseEvent e){
                    var target=field.pick(e.getX(),e.getY()-HEADER);
                    if(target!=null && progress==0) path=field.path(player,target);
                    requestFocusInWindow();
                }});
                addKeyListener(new KeyAdapter(){@Override public void keyPressed(KeyEvent e){
                    if(!path.isEmpty())return;
                    int dx=0,dy=0;
                    switch(e.getKeyCode()) {
                        case KeyEvent.VK_LEFT,KeyEvent.VK_A -> dx=-1;
                        case KeyEvent.VK_RIGHT,KeyEvent.VK_D -> dx=1;
                        case KeyEvent.VK_UP,KeyEvent.VK_W -> dy=-1;
                        case KeyEvent.VK_DOWN,KeyEvent.VK_S -> dy=1;
                        default -> {return;}
                    }
                    var target=new TilePoint(player.x()+dx,player.y()+dy);
                    if(field.canStep(player,target))path=List.of(target);
                }});
                timer.start();
                window.addWindowListener(new WindowAdapter(){@Override public void windowClosed(WindowEvent e){timer.stop();}});
            }
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);var next=path.isEmpty()?player:path.getFirst();
                g.drawImage(frame(player.x()+.5+(next.x()-player.x())*progress,
                    player.y()+.5+(next.y()-player.y())*progress,false),0,0,null);
            }
        };
        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);window.setContentPane(panel);
        window.pack();window.setLocationRelativeTo(null);window.setVisible(true);panel.requestFocusInWindow();
    }
    public static void main(String[] args) throws Exception {
        final ElevationPreview[] preview=new ElevationPreview[1];
        SwingUtilities.invokeAndWait(()->{try{preview[0]=new ElevationPreview();}catch(Exception e){throw new IllegalStateException(e);}});
        if(args.length>0 && args[0].equals("--play")) SwingUtilities.invokeLater(preview[0]::openWindow);
        else {
            Path output=Path.of(args.length>0?args[0]:"temp/elevation/preview");
            preview[0].export(output);System.out.println("Elevation preview written to "+output.toAbsolutePath());
        }
    }
}
