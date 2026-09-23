package com.alderfall.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
public class SpriteIntegrityPreview {
    public static void main(String[] args) throws Exception {
        Path review=Path.of("asset-review/2026-09-22-sprite-integrity");
        AssetStore current=new AssetStore(Path.of("Java/assets"));
        AssetStore before=new AssetStore(review.resolve("before/Java/assets"));
        String[] names={"city_cobble","city_building_house_wide","city_prop_barrel_stack","city_prop_crate","city_building_town_manor","city_building_stone_hall","village_prop_well"};
        BufferedImage result=new BufferedImage(1060, names.length*220+40,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=result.createGraphics();g.setColor(new Color(28,32,38));g.fillRect(0,0,result.getWidth(),result.getHeight());
        g.setFont(new Font("SansSerif",Font.PLAIN,14));g.setColor(Color.WHITE);g.drawString("Original / repaired on dark / repaired on light / 48 px game-size fit (actual AssetStore)",20,24);
        for(int i=0;i<names.length;i++){
            String n=names[i]; if(!current.hasSprite(n)||!before.hasSprite(n)) throw new IllegalStateException("Missing "+n);
            int y=40+i*220;
            g.setColor(Color.WHITE);g.drawString(n,12,y+18);
            Color[] backgrounds={new Color(74,77,80),new Color(24,35,28),new Color(227,218,198)};
            for(int j=0;j<3;j++){
                g.setColor(backgrounds[j]);g.fillRect(12+j*280,y+25,268,184);
                BufferedImage sprite=(j==0?before:current).spriteFit(n,240,172);
                g.drawImage(sprite,25+j*280,y+31,null);
            }
            g.drawImage(current.spriteFit(n,48,48),900,y+110,null);
            BufferedImage cached=current.spriteFit(n,48,48);boolean visible=false;
            for(int sy=0;sy<48;sy++)for(int sx=0;sx<48;sx++)if((cached.getRGB(sx,sy)>>>24)>8)visible=true;
            if(!visible)throw new IllegalStateException("Blank runtime sprite: "+n);
        }
        g.dispose();ImageIO.write(result,"png",review.resolve("runtime-before-after.png").toFile());
        System.out.println("Verified 7 active assets through AssetCatalog and AssetStore, including 48px rendering.");
    }
}
