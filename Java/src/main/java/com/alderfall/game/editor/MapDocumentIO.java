package com.alderfall.game.editor;

import com.alderfall.game.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Versioned UTF-8 properties; also reads the village editor's original text exports. */
public final class MapDocumentIO {
    private MapDocumentIO() {}
    public static void write(Path path, MapDocument d) throws IOException {
        Properties p = new Properties();
        p.setProperty("format", "alderfall-map"); p.setProperty("version", "2");
        p.setProperty("label", d.label); p.setProperty("kind", d.kind);
        p.setProperty("width", ""+d.width()); p.setProperty("height", ""+d.height());
        p.setProperty("spawn", d.spawnX+","+d.spawnY); p.setProperty("seed", ""+d.seed);
        p.setProperty("prefab", ""+d.prefab);
        for (int y=0; y<d.height(); y++) p.setProperty("tile."+y, new String(d.tiles[y]));
        for (int i=0; i<d.props.size(); i++) {
            WorldProp a = d.props.get(i);
            p.setProperty("prop."+i, a.x()+","+a.y()+","+a.asset()+","+a.size()+","+a.visualSlot()+","+a.offsetX()+","+a.offsetY());
        }
        for (int i=0; i<d.buildings.size(); i++) {
            CityBuilding b = d.buildings.get(i);
            p.setProperty("building."+i, b.style()+","+b.x1()+","+b.y1()+","+b.width()+","+b.depth()+","+b.palette()+","+b.facing());
            p.setProperty("buildingKey."+i, b.key());
        }
        int i=0;
        for (var e : d.landmarks.entrySet()) {
            p.setProperty("marker."+i, e.getKey().x()+","+e.getKey().y());
            p.setProperty("markerLabel."+i++, e.getValue());
        }
        Path target = path.toAbsolutePath();
        Files.createDirectories(target.getParent());
        Path temp = Files.createTempFile(target.getParent(), ".map-write-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) { p.store(writer, "Echoes of Alderfall map / prefab"); }
            try { Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
    public static MapDocument read(Path path) throws IOException {
        if (Files.size(path)>8_000_000) throw new IOException("Map file exceeds 8 MB.");
        Properties p = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { p.load(reader); }
        catch (IllegalArgumentException ex) { throw new IOException("Invalid map text: " + ex.getMessage(), ex); }
        try {
            if (p.containsKey("version") && !Set.of("1","2").contains(p.getProperty("version"))) throw new IllegalArgumentException("Unsupported map version.");
            if (p.containsKey("format") && !"alderfall-map".equals(p.getProperty("format"))) throw new IllegalArgumentException("Not an Alderfall map.");
            MapDocument d = new MapDocument(p.getProperty("label", "Imported map"), required(p,"kind"), integer(required(p,"width")), integer(required(p,"height")), 'g');
            for (int y=0; y<d.height(); y++) {
                String row=required(p,"tile."+y);
                if (row.length()!=d.width()) throw new IllegalArgumentException("Wrong width in tile row " + y);
                for (char tile : row.toCharArray()) if (MapDocument.TILES.indexOf(tile)<0) throw new IllegalArgumentException("Unknown tile: " + tile);
                d.tiles[y]=row.toCharArray();
            }
            d.seed=Long.parseLong(p.getProperty("seed","0")); d.prefab=Boolean.parseBoolean(p.getProperty("prefab","false"));
            if (p.containsKey("spawn")) { String[] s=parts(p.getProperty("spawn"),2,2); d.spawnX=integer(s[0]); d.spawnY=integer(s[1]); }
            point(d,d.spawnX,d.spawnY);
            for (String key : indexedKeys(p,"prop.")) {
                String[] s=parts(p.getProperty(key),4,7);
                int x=integer(s[0]), y=integer(s[1]), size=integer(s[3]), slot=s.length>=5?integer(s[4]):-1;
                point(d,x,y); if (!s[2].matches("[A-Za-z0-9_-]{1,180}") || size<1 || size>512 || slot < -1 || slot > 255) throw new IllegalArgumentException("Invalid prop: " + key);
                if(s.length==6)throw new IllegalArgumentException("Both prop offsets are required.");
                d.props.add(new WorldProp(x,y,s[2],size,slot,s.length==7?integer(s[5]):0,s.length==7?integer(s[6]):0));
            }
            for (String key : indexedKeys(p,"building.")) {
                if (!d.kind.equals("city") && !d.kind.equals("village")) throw new IllegalArgumentException("Building lots require a city or village map.");
                String[] s=parts(p.getProperty(key),5,7);
                if (VillageManager.buildingPlans().stream().noneMatch(b -> b.style().equals(s[0]))) throw new IllegalArgumentException("Unknown building style: " + s[0]);
                int x=integer(s[1]), y=integer(s[2]), w=integer(s[3]), h=integer(s[4]);
                if (w<1 || h<1 || w>MapDocument.MAX_SIZE || h>MapDocument.MAX_SIZE) throw new IllegalArgumentException("Invalid building dimensions.");
                point(d,x,y); point(d,x+w-1,y+h-1);
                d.buildings.add(new CityBuilding(p.getProperty("buildingKey."+key.substring(9),key),x,y,x+w-1,y+h-1,s[0],s.length>=6?integer(s[5]):0,s.length==7?CityBuilding.Facing.valueOf(s[6]):CityBuilding.Facing.SOUTH));
            }
            for (String key : indexedKeys(p,"marker.")) {
                String[] s=parts(p.getProperty(key),2,2); int x=integer(s[0]), y=integer(s[1]); point(d,x,y);
                d.landmarks.put(new TilePoint(x,y),required(p,"markerLabel."+key.substring(7)));
            }
            return d;
        } catch (IllegalArgumentException ex) { throw new IOException("Invalid map: " + ex.getMessage(), ex); }
    }
    private static List<String> indexedKeys(Properties p, String prefix) {
        return p.stringPropertyNames().stream().filter(k -> k.startsWith(prefix)).sorted(Comparator.comparingInt(k -> integer(k.substring(prefix.length())))).toList();
    }
    private static String required(Properties p,String key) { String s=p.getProperty(key); if (s==null) throw new IllegalArgumentException("Missing " + key); return s; }
    private static int integer(String s) { return Integer.parseInt(s); }
    private static String[] parts(String s,int min,int max) { String[] a=s.split(",",-1); if (a.length<min || a.length>max) throw new IllegalArgumentException("Malformed record: " + s); return a; }
    private static void point(MapDocument d,int x,int y) { if (!d.contains(x,y)) throw new IllegalArgumentException("Coordinate outside map: " + x + "," + y); }
}
