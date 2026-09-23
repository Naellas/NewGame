import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Extract authored portraits and poses; no recolouring or synthetic costume variants. Run from Java. */
public class ImportRegionalNpcs {
    static final String[] MAIN = {"farmer_m","farmer_f","miner_m","trader_f","artisan_m","scholar_f","guard_m","healer_f","noble_m"};
    static final String[] EXTRA = {"miner_f","trader_m","artisan_f","scholar_m","guard_f","healer_m","noble_f"};
    public static void main(String[] args) throws Exception {
        Path output = Path.of("assets/characters/npcs/townsfolk/regional"); Files.createDirectories(output);
        for (String region : new String[]{"temperate","north","desert","marsh","highland"}) {
            for (boolean extra : new boolean[]{false,true}) {
                Path source = Path.of("assets/source/regional-npcs/"+region+(extra?"-extra":"")+".png");
                if (!Files.exists(source)) continue;
                BufferedImage atlas = ImageIO.read(source.toFile());
                String[] roles = extra ? EXTRA : MAIN;
                int[] ys = new int[roles.length+1]; ys[roles.length]=atlas.getHeight();
                for(int r=1;r<roles.length;r++) ys[r]=gutter(atlas,false,r*atlas.getHeight()/roles.length,atlas.getHeight()/roles.length/5,0,atlas.getWidth());
                for(int row=0;row<roles.length;row++) {
                    int top=ys[row], bottom=ys[row+1];
                    int[] xs=new int[6]; xs[5]=atlas.getWidth();
                    // A portrait is wider than a pose. Find the transparent gutters in each row.
                    for(int col=1;col<5;col++) xs[col]=gutter(atlas,true,(int)(atlas.getWidth()*(.28+(col-1)*.16)),(int)(atlas.getWidth()*.065),top,bottom);
                    for(int col=0;col<5;col++) {
                        BufferedImage cell=atlas.getSubimage(xs[col],top,xs[col+1]-xs[col],bottom-top);
                        Rectangle ink=bounds(cell);
                        if(ink.width<8 || ink.height<20) throw new IllegalStateException("Empty cell: "+source+" "+row+":"+col);
                        BufferedImage cropped=new BufferedImage(ink.width,ink.height,BufferedImage.TYPE_INT_ARGB);
                        Graphics2D cg=cropped.createGraphics(); cg.drawImage(cell,-ink.x,-ink.y,null);cg.dispose();
                        String stem="npc_regional_"+region+"_"+roles[row];
                        // Reviewed sheets present the right profile before the left.
                        boolean reverse = true;
                        String direction=col==1?"down":col==2?(reverse?"right":"left"):col==3?(reverse?"left":"right"):"up";
                        String name=stem+(col==0?"_portrait":"_model_"+direction);
                        ImageIO.write(cropped,"png",output.resolve(name+".png").toFile());
                        if(col==1) {
                            ImageIO.write(cropped,"png",output.resolve(stem+"_model.png").toFile());
                        }
                    }
                }
                System.out.println("Imported "+source+": "+roles.length+" portraits and four-direction models");
            }
        }
    }
    static Rectangle bounds(BufferedImage image) {
        int x0=image.getWidth(),y0=image.getHeight(),x1=-1,y1=-1;
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)if((image.getRGB(x,y)>>>24)>48){x0=Math.min(x0,x);y0=Math.min(y0,y);x1=Math.max(x1,x);y1=Math.max(y1,y);}
        return x1<0?new Rectangle():new Rectangle(x0,y0,x1-x0+1,y1-y0+1);
    }
    static int gutter(BufferedImage image,boolean vertical,int expected,int radius,int from,int to) {
        int best=expected;long score=Long.MAX_VALUE;
        for(int p=Math.max(1,expected-radius);p<Math.min((vertical?image.getWidth():image.getHeight())-1,expected+radius);p++) {
            int ink=0;for(int a=from;a<to;a++)if(((vertical?image.getRGB(p,a):image.getRGB(a,p))>>>24)>48)ink++;
            long candidate=ink*10000L+Math.abs(p-expected);if(candidate<score){score=candidate;best=p;}
        }return best;
    }
}
