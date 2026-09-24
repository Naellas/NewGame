package com.alderfall.game.ui;

import com.alderfall.game.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/** Shared dialogue-art framing and idle/speech timing for party cards. */
public final class PartyPortraitRenderer {
    private final AssetStore assets;
    private final long started=System.nanoTime();
    private GameState.TravelBanterPrompt lastPrompt;
    private long promptStarted;
    private final Map<String,DialogueFigureAnimation> animations=new LinkedHashMap<>(12,.75f,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String,DialogueFigureAnimation> e){return size()>12;}
    };
    public PartyPortraitRenderer(AssetStore assets){this.assets=assets;}
    public static String sprite(AssetStore assets,Actor actor) {
        String dialogue=actor.sprite+"_dialogue_sprite";
        return assets.hasSprite(dialogue)?dialogue:actor.worldSprite;
    }
    public void draw(Graphics2D g,Actor actor,Rectangle bounds,GameState.TravelBanterPrompt prompt) {
        long now=(System.nanoTime()-started)/1_000_000;
        if(prompt!=lastPrompt){lastPrompt=prompt;promptStarted=now;}
        String sprite=sprite(assets,actor);
        if(!sprite.endsWith("_dialogue_sprite")) {
            g.drawImage(assets.portrait(sprite,bounds.width,bounds.height),bounds.x,bounds.y,null);return;
        }
        // Banter is shown as a complete line. Give its speaker a bounded speaking
        // gesture, then return to idle while the player considers the responses.
        boolean speaking=prompt!=null && actor.name.equals(prompt.speaker())
                && now-promptStarted<Math.min(12000,Math.max(1500,prompt.line().length()*38L));
        int mouth=DialogueFigureAnimation.mouthPose("Speaking",!speaking,now-promptStarted);
        animations.computeIfAbsent(sprite,ignored->new DialogueFigureAnimation()).drawPortrait(g,
                assets.spriteFit(sprite,384,768),sprite,bounds,now,mouth,speaking,false,-1);
    }
}
