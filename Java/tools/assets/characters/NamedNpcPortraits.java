import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import javax.imageio.ImageIO;

public class NamedNpcPortraits {
    public static void main(String[] args) throws Exception {
        Path dir=Path.of("assets/source/regional-npcs");Files.createDirectories(dir);
        String data=Files.readString(Path.of("src/main/java/com/alderfall/game/content/GameData.java"));
        data=data.substring(data.indexOf("public static final List<Npc> NPCS"),data.indexOf("private static Map.Entry<String, Quest> quest("));
        Set<String> stems=new TreeSet<>();Matcher m=Pattern.compile("\"(npc_[a-z_]+)\"").matcher(data);
        while(m.find())stems.add(m.group(1));
        var names=new ArrayList<>(stems);
        Files.write(dir.resolve("named-roster.txt"),names);
        int cols=6,rows=(names.size()+cols-1)/cols;
        if(args.length==0) {
            BufferedImage sheet=new BufferedImage(cols*220,rows*240,BufferedImage.TYPE_INT_RGB);
            Graphics2D g=sheet.createGraphics();g.setColor(new Color(29,34,39));g.fillRect(0,0,sheet.getWidth(),sheet.getHeight());
            for(int i=0;i<names.size();i++) {
                String name=names.get(i);Path p;
                try(var files=Files.walk(Path.of("assets"))) {p=files.filter(f->f.getFileName().toString().equals(name+"_model_down.png")).findFirst().orElseThrow();}
                BufferedImage img=ImageIO.read(p.toFile());Rectangle b=ImportRegionalNpcs.bounds(img);img=img.getSubimage(b.x,b.y,b.width,b.height);
                double scale=Math.min(180.0/img.getWidth(),200.0/img.getHeight());int w=(int)(img.getWidth()*scale),h=(int)(img.getHeight()*scale);
                int x=i%cols*220,y=i/cols*240;g.drawImage(img,x+(220-w)/2,y+200-h,w,h,null);
                g.setColor(Color.WHITE);g.setFont(new Font("SansSerif",Font.PLAIN,12));g.drawString((i+1)+" "+name.replace("npc_",""),x+5,y+225);
            }
            g.dispose();ImageIO.write(sheet,"png",dir.resolve("named-reference.png").toFile());
            System.out.println(names.size()+" characters; "+cols+" columns x "+rows+" rows");
        } else {
            BufferedImage sheet=ImageIO.read(Path.of(args[0]).toFile());Path output=Path.of("assets/characters/npcs/townsfolk/portraits");Files.createDirectories(output);
            for(int i=0;i<names.size();i++) {
                int x=i%cols*sheet.getWidth()/cols,y=i/cols*sheet.getHeight()/rows;
                BufferedImage cell=sheet.getSubimage(x,y,sheet.getWidth()/cols,sheet.getHeight()/rows);
                ImageIO.write(cell,"png",output.resolve(names.get(i)+"_portrait.png").toFile());
            }
        }
    }
}
