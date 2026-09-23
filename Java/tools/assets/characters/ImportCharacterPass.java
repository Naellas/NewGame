import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Import reviewed 24-pose combat and 16-pose side-walk atlases. Run from Java/. */
class ImportCharacterPass {
    static final String[][] ROSTER = {{"knight","class_knight","attack"},{"mage","class_mage","cast"},
        {"ranger","class_ranger","shoot"},{"cleric","class_cleric","cast"},{"rogue","class_rogue","attack"},
        {"aria","npc_aria","shoot"},{"calder","npc_calder","attack"},{"cassia","npc_cassia","attack"},
        {"lyra","npc_lyra","cast"},{"maera","npc_maera","cast"},{"rafiq","npc_rafiq","attack"},
        {"samir","npc_samir","cast"},{"seraphine","npc_seraphine","attack"},{"vesper","npc_vesper","cast"}};
    static final Path ROOT = Path.of("assets/characters/shared/animations/character-refresh");

    public static void main(String[] args) throws Exception {
        boolean complete = args.length > 0 && args[0].equals("--complete");
        for (String[] actor : ROSTER) for (boolean walk : new boolean[]{false,true}) {
            Path file = ROOT.resolve("pass-v4/sources/"+actor[0]+(walk?"-walk.png":"-combat.png"));
            if (!Files.exists(file)) { if (complete) throw new IllegalStateException("Missing "+file); continue; }
            pack(file, actor[1], actor[2], walk);
        }
    }

    static void pack(Path file, String actor, String action, boolean walk) throws Exception {
        BufferedImage source = ImageIO.read(file.toFile());
        int columns = walk ? 4 : 6, rows = 4, count = columns * rows, size = walk ? 192 : 256;
        BufferedImage atlas = new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_ARGB);
        for (int y=0;y<source.getHeight();y++) for(int x=0;x<source.getWidth();x++) {
            int p=source.getRGB(x,y), r=p>>16&255,g=p>>8&255,b=p&255;
            if ((p>>>24)<12 || (Math.min(r,b)-g>55 && r>140 && b>140)
                    || (r>245&&g<12&&b<12) || (g>245&&r<12&&b<12) || (r>245&&g>245&&b<12)) continue;
            atlas.setRGB(x,y,p);
        }
        int[] ys = new int[rows+1]; ys[rows]=atlas.getHeight();
        for(int row=1;row<rows;row++) ys[row]=gutter(atlas,false,row*atlas.getHeight()/rows,atlas.getHeight()/rows/5,0,atlas.getWidth());
        BufferedImage strip=new BufferedImage(size*count,size,BufferedImage.TYPE_INT_ARGB);
        double scale=(size-16.0)/Math.max(atlas.getWidth()/(double)columns,atlas.getHeight()/(double)rows);
        for(int frame=0;frame<count;frame++) {
            int row=frame/columns,col=frame%columns,top=ys[row],bottomEdge=ys[row+1];
            int left=col==0?0:gutter(atlas,true,col*atlas.getWidth()/columns,atlas.getWidth()/columns/6,top,bottomEdge);
            int right=col+1==columns?atlas.getWidth():gutter(atlas,true,(col+1)*atlas.getWidth()/columns,atlas.getWidth()/columns/6,top,bottomEdge);
            BufferedImage cell=new BufferedImage(right-left,bottomEdge-top,BufferedImage.TYPE_INT_ARGB);
            Graphics2D cg=cell.createGraphics();cg.drawImage(atlas,-left,-top,null);cg.dispose();clean(cell);
            int bottom=-1,ink=0;
            for(int y=0;y<cell.getHeight();y++) for(int x=0;x<cell.getWidth();x++) if((cell.getRGB(x,y)>>>24)>32){bottom=Math.max(bottom,y);ink++;}
            if(ink<80) throw new IllegalStateException("Empty atlas pose "+file+" frame "+frame);
            Graphics2D g=strip.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            // Preserve the source grid's body anchor and one scale across all poses.
            double gridCenter=(col+.5)*atlas.getWidth()/columns;
            g.translate(frame*size+size*.5+(left-gridCenter)*scale,size-7-bottom*scale);g.scale(scale,scale);g.drawImage(cell,0,0,null);g.dispose();
        }
        Path output=ROOT.resolve(walk?"walk-ready-v4":"ready-v4");Files.createDirectories(output);
        String model=actor+"_combat_v4";
        String stem=walk?actor+"_model_right_walk_v4_anim":model+"_"+action+"_anim";
        ImageIO.write(strip,"png",output.resolve(stem+".png").toFile());Files.writeString(output.resolve(stem+".frames"),count+"\n");
        if(!walk) ImageIO.write(strip.getSubimage(0,0,size,size),"png",output.resolve(model+".png").toFile());
        System.out.println("Packed "+stem+" / "+count+" drawn poses");
    }

    static int gutter(BufferedImage image,boolean vertical,int expected,int radius,int from,int to) {
        int best=expected;long score=Long.MAX_VALUE;
        for(int p=Math.max(1,expected-radius);p<Math.min((vertical?image.getWidth():image.getHeight())-1,expected+radius);p++) {
            int ink=0;for(int a=from;a<to;a++) if(((vertical?image.getRGB(p,a):image.getRGB(a,p))>>>24)>100)ink++;
            long candidate=ink*10000L+Math.abs(p-expected);if(candidate<score){score=candidate;best=p;}
        }return best;
    }

    static void clean(BufferedImage image) {
        int w=image.getWidth(),h=image.getHeight();boolean[] seen=new boolean[w*h];int[] queue=new int[w*h];
        java.util.ArrayList<int[]> components=new java.util.ArrayList<>();int largest=0;
        for(int p=0;p<w*h;p++) {
            if(seen[p]||(image.getRGB(p%w,p/w)>>>24)==0)continue;
            int head=0,tail=1;queue[0]=p;seen[p]=true;
            while(head<tail){int v=queue[head++],x=v%w,y=v/w;
                for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){
                    int nx=x+dx,ny=y+dy;if(nx<0||nx>=w||ny<0||ny>=h)continue;int n=ny*w+nx;
                    if(!seen[n]&&(image.getRGB(nx,ny)>>>24)!=0){seen[n]=true;queue[tail++]=n;}
                }
            }
            largest=Math.max(largest,tail);components.add(java.util.Arrays.copyOf(queue,tail));
        }
        // Labels and grid outlines are detached from the character. Keep substantial
        // connected sprite parts, rather than allowing atlas annotations into the model.
        for(int[] component:components)if(component.length<Math.max(32,largest/8))
            for(int pixel:component)image.setRGB(pixel%w,pixel/w,0);
    }
}
