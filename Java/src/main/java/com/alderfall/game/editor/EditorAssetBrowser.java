package com.alderfall.game.editor;

import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Metadata is read once, never from a Swing cell renderer. */
final class EditorAssetBrowser {
    record Info(String source,long modified) {
        String date(){return modified==0?"Unknown":DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .format(Instant.ofEpochMilli(modified).atZone(ZoneId.systemDefault()));}
    }
    static Info inspect(Path assets,String source){
        long modified=0;
        if(source!=null&&!source.isBlank())try{modified=Files.getLastModifiedTime(assets.resolve(source)).toMillis();}catch(java.io.IOException ignored){}
        return new Info(source==null?"":source,modified);
    }
    static String category(EditorPalette.Entry e){return e.category().split(" / ",2)[0];}
    static String subcategory(EditorPalette.Entry e){String[] p=e.category().split(" / ",2);return p.length==2?p[1]:"General";}
    static Comparator<EditorPalette.Entry> comparator(String sort,Map<String,Info> info){
        Comparator<EditorPalette.Entry> name=Comparator.comparing(EditorPalette.Entry::label,String.CASE_INSENSITIVE_ORDER).thenComparing(EditorPalette.Entry::asset);
        Comparator<EditorPalette.Entry> date=Comparator.comparingLong(e -> info.get(e.asset()).modified());
        return switch(sort){
            case "Name Z-A" -> name.reversed();
            case "Newest modified" -> date.reversed().thenComparing(name);
            case "Oldest modified" -> Comparator.<EditorPalette.Entry>comparingInt(e -> info.get(e.asset()).modified()==0?1:0).thenComparing(date).thenComparing(name);
            case "Category" -> Comparator.comparing(EditorPalette.Entry::category).thenComparing(name);
            default -> name;
        };
    }
}
