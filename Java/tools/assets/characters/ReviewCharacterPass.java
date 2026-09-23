import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Source-atlas overviews for checking grid layout and pose order before accepting art. */
class ReviewCharacterPass {
    public static void main(String[] args) throws Exception {
        String[] names={"knight","mage","ranger","cleric","rogue","aria","calder","cassia","lyra","maera","rafiq","samir","seraphine","vesper"};
        for(String kind:new String[]{"combat","walk"})for(int group=0;group<2;group++){
            BufferedImage sheet=new BufferedImage(1536,1536,BufferedImage.TYPE_INT_RGB);
            Graphics2D g=sheet.createGraphics();g.setColor(new Color(28,34,40));g.fillRect(0,0,1536,1536);
            for(int i=0;i<7;i++){
                String name=names[group*7+i]; BufferedImage source=ImageIO.read(Path.of("assets/characters/shared/animations/character-refresh/pass-v4/sources/"+name+"-"+kind+".png").toFile());
                int x=(i%3)*512,y=(i/3)*512;double scale=Math.min(500.0/source.getWidth(),480.0/source.getHeight());
                g.drawImage(source,x,y+24,(int)(source.getWidth()*scale),(int)(source.getHeight()*scale),null);
                g.setColor(Color.WHITE);g.drawString(name+" / "+source.getWidth()+"x"+source.getHeight(),x+4,y+16);
            }
            g.dispose();ImageIO.write(sheet,"png",Path.of("tools/reviews/characters/source-"+kind+"-"+group+".png").toFile());
        }
    }
}
