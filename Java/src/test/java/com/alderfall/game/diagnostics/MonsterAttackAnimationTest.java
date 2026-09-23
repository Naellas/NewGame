package com.alderfall.game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Asset integrity, actual battle selection, timing, and visual review for monster attacks. */
public final class MonsterAttackAnimationTest {
    static final Path OUTPUT = Path.of("tools/reviews/monster-attacks-2026-09-23");
    static final List<String> NAMES = MonsterAttackAnimations.SPRITES.stream().sorted().toList();

    public static void main(String[] args) throws Exception {
        Files.createDirectories(OUTPUT);
        AssetStore assets = new AssetStore(Path.of("assets"));
        int catalogCount = 0;
        for (var spec : GameData.MONSTERS.values()) {
            require(MonsterAttackAnimations.supports(spec.sprite()), "Missing family member " + spec.key());
            catalogCount++;
        }
        for (String name : NAMES) {
            require(assets.hasAuthoredAnimation(name, "attack"), "Missing " + name);
            BufferedImage strip = ImageIO.read(Path.of("assets/characters/monsters/animations/" + MonsterAttackAnimations.sheetName(name,"attack") + ".png").toFile());
            require(strip.getWidth() == 3072 && strip.getHeight() == 256, "Strip size " + name);
            Set<Long> unique = new HashSet<>();
            for (int f = 0; f < 12; f++) {
                int pixels = 0; long hash = 1;
                for (int y = 0; y < 256; y++) for (int x = 0; x < 256; x++) {
                    int rgba = strip.getRGB(f * 256 + x, y); hash = hash * 31 + rgba;
                    if ((rgba >>> 24) > 0) {
                        pixels++;
                        require(x > 0 && x < 255 && y > 0 && y < 255, "Clipped frame " + name + ":" + f);
                    }
                }
                require(pixels > 1000 && pixels < 60000, "Blank or opaque frame " + name + ":" + f);
                unique.add(hash);
            }
            require(unique.size() >= 10, "Insufficient distinct poses " + name);
            for (int[] size : new int[][]{{120,120},{240,240},{200,300}}) {
                require(assets.animatedSpriteFrameCount(name,"attack",size[0],size[1]) == 12, "Metadata " + name);
                for (int f = 0; f < 12; f++) require(assets.animatedSpriteFit(name,"attack",size[0],size[1],f) != null, "Frame load");
            }
        }
        writeContactSheets(assets);
        SwingUtilities.invokeAndWait(MonsterAttackAnimationTest::checkBattle);
        String result = "PASS: " + NAMES.size() + " strips, " + NAMES.size() * 12 + " frames, " + catalogCount
                + " catalog entries; transparent margins, varied poses, three display sizes, runtime attack selection, buff exclusion and playback timing.\n";
        Files.writeString(OUTPUT.resolve("verification.txt"), result);
        System.out.print(result);
    }

    static void writeContactSheets(AssetStore assets) throws Exception {
        for (int group = 0; group * 8 < NAMES.size(); group++) {
            int count = Math.min(8, NAMES.size() - group * 8);
            BufferedImage image = new BufferedImage(1150, count * 155, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics(); g.setColor(new Color(41,46,54)); g.fillRect(0,0,image.getWidth(),image.getHeight());
            for (int row = 0; row < count; row++) {
                String name = NAMES.get(group * 8 + row); g.setColor(Color.WHITE); g.drawString(name, 12, row * 155 + 24);
                for (int f = 0; f < 6; f++) g.drawImage(assets.animatedSpriteFit(name,"attack",150,150,new int[]{0,3,6,8,9,11}[f]),230+f*150,row*155,null);
            }
            g.dispose(); ImageIO.write(image,"png",OUTPUT.resolve("poses-"+group+".png").toFile());
        }
    }

    static void checkBattle() {
        GamePanel panel = new GamePanel(Path.of("").toAbsolutePath());
        try {
            ((javax.swing.Timer)field(GamePanel.class,"timer").get(panel)).stop();
            GameState state = (GameState)field(GamePanel.class,"state").get(panel);
            state.chooseClass("Mage"); while(state.mode==GameMode.STORY_INTRO) state.advanceStoryIntro();
            panel.setSize(GameConfig.WIDTH,GameConfig.HEIGHT);
            Method action = GamePanel.class.getDeclaredMethod("battleActorAnimationAction",Battle.class,Actor.class); action.setAccessible(true);
            Method render = GamePanel.class.getDeclaredMethod("battleActorImage",Battle.class,Actor.class,String.class,String.class,int.class,int.class); render.setAccessible(true);
            for (String name : NAMES) {
                var spec = GameData.MONSTERS.values().stream().filter(s->s.sprite().equals(name)).findFirst().orElseThrow();
                Battle battle = new Battle(state.player,List.of(),List.of(spec),new Random(1),'g',"overworld");
                Actor enemy = battle.enemies().get(0);
                state.battle = battle; state.mode = GameMode.BATTLE; battle.backdrop="battle_meadow_hills_backdrop";
                for (String effect : List.of("strike","pierce","frost","shadow","shield")) {
                    BattleActionAnimation animation = new BattleActionAnimation(enemy,state.player,effect,24,12,18);
                    animation.configureVisual("Monster attack review",Ability.AbilityKind.DAMAGE);
                    field(Battle.class,"actionAnimation").set(battle,animation);
                    require("attack".equals(action.invoke(panel,battle,enemy)),"Wrong runtime action " + name + "/" + effect);
                }
                BattleActionAnimation buff = new BattleActionAnimation(enemy,enemy,"ward",24,12,18);
                buff.configureVisual("Monster guard review",Ability.AbilityKind.DEFEND);
                require(!MonsterAttackAnimations.playsAttack(buff,enemy),"Buff played attack " + name);
                field(Battle.class,"actionAnimation").set(battle,null);
                BufferedImage rest = (BufferedImage)render.invoke(panel,battle,enemy,name,null,240,240);
                BufferedImage ready = new AssetStore(Path.of("assets")).animatedSpriteFit(name,"attack",240,240,0);
                require(Arrays.equals(rest.getRGB(0,0,240,240,null,0,240),ready.getRGB(0,0,240,240,null,0,240)),"Rest changed facing " + name);
                for (double speed : new double[]{0.5,1,3}) {
                    BattleActionAnimation animation = new BattleActionAnimation(enemy,state.player,List.of(state.player),BattleActionAnimation.VisualMode.SINGLE,"strike",24,12,18,speed);
                    double previous = -1;
                    while(animation.stage()!=BattleActionAnimation.Stage.DONE) {
                        double p = animation.actorPoseProgress(enemy); require(p>=previous,"Animation restarted"); previous=p; animation.tick();
                    }
                    require(animation.actorPoseProgress(enemy)==1,"Missing recovery");
                }
                BattleActionAnimation animation = new BattleActionAnimation(enemy,state.player,"strike",24,12,18);
                field(Battle.class,"actionAnimation").set(battle,animation);
                while(animation.actorPoseProgress(enemy)<8.0/12) animation.tick();
                BufferedImage scene = new BufferedImage(GameConfig.WIDTH,GameConfig.HEIGHT,BufferedImage.TYPE_INT_RGB);
                Graphics2D g = scene.createGraphics(); panel.paint(g); g.dispose();
                ImageIO.write(scene,"png",OUTPUT.resolve("battle-"+name+".png").toFile());
            }
        } catch(Exception e) { throw new IllegalStateException(e); }
        finally { panel.shutdown(); }
    }

    static Field field(Class<?> type,String name) throws Exception { Field f=type.getDeclaredField(name);f.setAccessible(true);return f; }
    static void require(boolean pass,String message) { if(!pass)throw new IllegalStateException(message); }
}
