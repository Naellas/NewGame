package com.alderfall.game;

import com.alderfall.game.map.MapArea;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Headless layout/timing regression checks and reproducible visual review captures. */
public final class CombatGraphicsTest {
    public static void main(String[] args) throws Exception {
        checkScenery();
        checkFormations();
        checkStatusVfx();
        checkHealingRecipients();
        checkImpactArt();
        checkClassImpactArt();
        checkAnimationSpeedSettings();
        Actor source = GameData.createPlayer("Mage");
        List<Actor> targets = List.of(GameData.createPlayer("Knight"), GameData.createPlayer("Ranger"), GameData.createPlayer("Rogue"));
        for (var mode : BattleActionAnimation.VisualMode.values()) {
          int previousDuration = Integer.MAX_VALUE;
          for (double speed : new double[]{0.5, 1.0, 2.0, 3.0}) {
            BattleActionAnimation animation = new BattleActionAnimation(source, targets.get(0), targets, mode, "fire", 10, 20, 18, speed);
            animation.configureVisual("Signal Flare", Ability.AbilityKind.DAMAGE);
            require(animation.totalFrames() < previousDuration, "Higher playback speed must shorten the action");
            previousDuration = animation.totalFrames();
            while (animation.stage() != BattleActionAnimation.Stage.DONE) {
                for (var step : animation.pendingImpactSteps()) {
                    require(animation.collisionProgress(step.index()) == 0.0, "Visual collision must coincide with damage: " + mode);
                    animation.markStepTriggered(step.index());
                }
                animation.tick();
            }
            require(animation.triggeredStepCount() == targets.size(), "Every target must resolve");
          }
        }
        Path output = Path.of("exports/combat-graphics");
        Files.createDirectories(output);
        SwingUtilities.invokeAndWait(() -> capture(output));
        writeMotionPreview(output);
        writeMotionPreview(output, true);
        System.out.println("Combat graphics checks passed; previews: " + output.toAbsolutePath());
    }

    private static void checkAnimationSpeedSettings() throws Exception {
        Path root = Files.createTempDirectory("alderfall-animation-speed-");
        try {
            require(GameConfig.loadWithSettings(root).combatAnimationSpeed == 1.0, "Default animation speed");
            GameConfig config = GameConfig.load(root);
            config.combatAnimationSpeed = 2.3;
            config.save(root);
            require(GameConfig.loadWithSettings(root).combatAnimationSpeed == 2.3, "Animation speed must persist");
            Path settings = root.resolve("config/settings.properties");
            for (String invalid : List.of("NaN", "Infinity", "oops")) {
                Files.writeString(settings, "combatAnimationSpeed=" + invalid);
                require(GameConfig.loadWithSettings(root).combatAnimationSpeed == 1.0, "Invalid animation speed fallback");
            }
            Files.writeString(settings, "combatAnimationSpeed=99");
            require(GameConfig.loadWithSettings(root).combatAnimationSpeed == 3.0, "Maximum animation speed");
            Files.writeString(settings, "combatAnimationSpeed=-1");
            require(GameConfig.loadWithSettings(root).combatAnimationSpeed == 0.5, "Minimum animation speed");
        } finally {
            Files.deleteIfExists(root.resolve("config/settings.properties"));
            Files.deleteIfExists(root.resolve("config"));
            Files.deleteIfExists(root);
        }
    }

    private static void checkScenery() {
        char[][] tiles = new char[9][9];
        for (char[] row : tiles) java.util.Arrays.fill(row, 's');
        MapArea area = new MapArea("test", "Test", "overworld", tiles);
        require(BattleScenery.choose(area, 0, 0, 's', "base").equals("base"), "Map boundary must not imply mountains");
        area.setTile(5, 4, 'm');
        require(BattleScenery.choose(area, 4, 4, 's', "base").equals("battle_desert_foothills_backdrop"), "Desert mountain transition");
        area.setTile(4, 5, 'P');
        require(BattleScenery.choose(area, 4, 4, 'g', "base").equals("battle_coastal_meadow_backdrop"), "Coastal transition");
        MapArea dungeon = new MapArea("dungeon", "Dungeon", "dungeon", tiles);
        require(BattleScenery.choose(dungeon, 4, 4, 'g', "crypt").equals("crypt"), "Indoor scenery must remain intact");
        AssetStore assets = new AssetStore(Path.of("assets"));
        for (String name : List.of("coastal_meadow", "desert_foothills", "forest_edge", "meadow_hills"))
            require(assets.hasSprite("battle_" + name + "_backdrop"), "Missing background " + name);
    }

    private static void checkHealingRecipients() {
        Actor caster = GameData.createPlayer("Mage");
        Ability heal = new Ability("Grove Hymn", 10, 0, Ability.AbilityKind.HEAL, "party").withEffect("grove_hymn");
        caster.abilities.clear(); caster.abilities.add(heal); caster.setAbilityLoadout(List.of(heal.name()));
        List<Actor> allies = List.of(GameData.createPlayer("Knight"), GameData.createPlayer("Ranger"));
        Battle battle = new Battle(caster, allies, GameData.MONSTERS.get("skeleton"), new Random(7));
        for (Actor actor : battle.partyMembers()) { actor.maxHp = 1000; actor.hp = 1; }
        battle.useAbility(0);
        BattleActionAnimation animation = battle.activeAnimation();
        require(animation != null && animation.effectKind.equals("grove_hymn"), "Healing must preserve its own effect identity");
        require(animation.targets.size() == battle.partyMembers().size(), "Party healing must animate every recipient");
        while (animation.frame() < animation.collisionFrame(0) - 1) battle.tick();
        require(battle.partyMembers().stream().allMatch(actor -> actor.hp == 1), "Healing must wait for visual collision");
        battle.tick();
        require(battle.partyMembers().stream().allMatch(actor -> actor.hp > 1), "Each party recipient must recover HP");
        require(battle.log.stream().filter(line -> line.contains("party recovers")).count() == 1,
                "A party heal must resolve once, not once per visual recipient");
    }

    private static void checkImpactArt() throws Exception {
        var sprites = com.alderfall.game.render.battle.AbilityImpactArt.SPRITES;
        require(new java.util.HashSet<>(sprites.values()).size() == sprites.size(), "Named impacts must have distinct art");
        for (String sprite : sprites.values()) {
            BufferedImage image = ImageIO.read(Path.of("assets/effects", sprite + ".png").toFile());
            require(image.getColorModel().hasAlpha(), "Impact must preserve transparency: " + sprite);
            require((image.getRGB(0, 0) >>> 24) < 10, "Impact must not have an opaque rectangular backdrop: " + sprite);
        }
        int[] bounds = com.alderfall.game.render.battle.AbilityImpactArt.groupBounds(List.of(new int[]{200, 200}, new int[]{600, 400}));
        require(bounds[2] >= 660 && bounds[3] >= 430, "Area art must cover the affected formation");
        var art = new com.alderfall.game.render.battle.AbilityImpactArt(new AssetStore(Path.of("assets")));
        for (String kind : sprites.keySet()) {
            long early = impactFingerprint(art, kind, 0.15);
            long peak = impactFingerprint(art, kind, 0.48);
            long late = impactFingerprint(art, kind, 0.82);
            require(early != peak && peak != late && early != late, "Impact must evolve through distinct phases: " + kind);
            require(impactFingerprint(art, kind, -0.1) == impactFingerprint(art, kind, 1), "No impact before collision or after completion");
        }
    }

    private static void checkClassImpactArt() throws Exception {
        var art = new com.alderfall.game.render.battle.AbilityImpactArt(new AssetStore(Path.of("assets")));
        var cells = new java.util.HashSet<String>();
        for (var profile : ClassAbilityVfx.profiles().values()) {
            require(art.has(profile), "Missing class VFX sheet: " + profile.name());
            require(cells.add(profile.sheet() + ":" + profile.cell()), "Reused ability artwork: " + profile.name());
            BufferedImage texture = art.texture(profile);
            int visible = 0, transparent = 0;
            for (int y = 0; y < texture.getHeight(); y++) for (int x = 0; x < texture.getWidth(); x++) {
                int alpha = texture.getRGB(x, y) >>> 24;
                if (alpha > 32) visible++;
                if (alpha == 0) transparent++;
            }
            require(visible > 200, "Empty ability cell: " + profile.name());
            require(transparent > 200, "Ability cell needs transparency: " + profile.name());
        }
        for (String cls : ClassAbilityVfx.CLASSES) {
            var abilities = new ArrayList<Ability>(GameData.classAbilities(cls));
            for (var node : SkillTrees.skillTreeForClass(cls).values())
                if (node.ability() != null) abilities.add(node.ability());
            for (Ability ability : abilities)
                require(ClassAbilityVfx.forName(ability.name()) != null || ClassAbilityVfx.RETAINED.containsKey(ability.name()),
                        "Unmapped ability: " + cls + "/" + ability.name());
            for (String mode : List.of("basic", "heavy", "cleave", "guard", "dodge"))
                require(ClassAbilityVfx.forName(ClassAbilityVfx.attackName(cls, mode)) != null, "Unmapped class action");
        }
        for (var node : SkillTrees.COMMON_SKILL_TREE.values())
            if (node.ability() != null)
                require(ClassAbilityVfx.forName(node.ability().name()) != null, "Unmapped common ability: " + node.ability().name());
        System.out.println("Class VFX coverage: " + cells.size() + " distinct ability/action cells");
        for (String name : List.of("Worldsplitter", "Primeval Bloom", "Moonless Verdict", "Signal Flare",
                "Canopy Ward", "Vine Flick", "Fade")) {
            var profile = ClassAbilityVfx.forName(name);
            long start = classImpactAlpha(art, profile, 0);
            long early = classImpactAlpha(art, profile, 0.08);
            long peak = classImpactAlpha(art, profile, 0.40);
            require(start == 0, "Effect must begin without a full-image pop: " + name);
            require(peak > early * 2, "Effect must build gradually: " + name);
            require(classImpactAlpha(art, profile, 0.94) < peak, "Effect must dissipate: " + name);
            require(classImpactAlpha(art, profile, -0.1) == 0 && classImpactAlpha(art, profile, 1) == 0,
                    "Effect must remain within collision lifetime: " + name);
        }
    }

    private static long classImpactAlpha(com.alderfall.game.render.battle.AbilityImpactArt art,
                                         ClassAbilityVfx.Profile profile, double progress) {
        BufferedImage image = new BufferedImage(300, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        art.draw(g, profile, 150, 150, 240, 240, progress);
        g.dispose();
        long alpha = 0;
        for (int y = 0; y < 300; y++) for (int x = 0; x < 300; x++) alpha += image.getRGB(x, y) >>> 24;
        return alpha;
    }

    private static long impactFingerprint(com.alderfall.game.render.battle.AbilityImpactArt art, String kind, double progress) {
        BufferedImage image = new BufferedImage(300, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        art.draw(g, kind, 150, 150, 240, 240, progress);
        g.dispose();
        long hash = 1;
        for (int y = 0; y < 300; y++) for (int x = 0; x < 300; x++) hash = 31 * hash + image.getRGB(x, y);
        return hash;
    }

    private static void writeMotionPreview(Path output) throws Exception {
        writeMotionPreview(output, false);
    }

    private static void writeMotionPreview(Path output, boolean classEffects) throws Exception {
        AssetStore assets = new AssetStore(Path.of("assets"));
        var art = new com.alderfall.game.render.battle.AbilityImpactArt(assets);
        String[] kinds = {"root_memory", "mend_unique", "firebolt_unique", "glacier_prison_unique"};
        String[] labels = {"Roots emerge", "Healing gathers and rises", "Fire bursts into embers", "Ice grows and shatters"};
        if (classEffects) {
            kinds = new String[]{"Worldsplitter", "Primeval Bloom", "Moonless Verdict", "Signal Flare"};
            labels = new String[]{"Worldsplitter: stone eruption", "Primeval Bloom: rising petals", "Moonless Verdict: shadow sweep", "Signal Flare: area burst"};
        }
        var writer = ImageIO.getImageWritersByFormatName("gif").next();
        try (var stream = ImageIO.createImageOutputStream(output.resolve(classEffects ? "class-ability-process.gif" : "ability-process.gif").toFile())) {
            writer.setOutput(stream);
            writer.prepareWriteSequence(null);
            for (int frame = 0; frame < 52; frame++) {
                BufferedImage image = new BufferedImage(960, 640, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = image.createGraphics();
                double p = (frame - 5) / 38.0;
                for (int cell = 0; cell < 4; cell++) {
                    Graphics2D scene = (Graphics2D) g.create(cell % 2 * 480, cell / 2 * 320, 480, 320);
                    scene.drawImage(assets.cover("battle_coastal_meadow_backdrop", 480, 320), 0, 0, null);
                    scene.setColor(new java.awt.Color(10, 15, 20, 100)); scene.fillRect(0, 0, 480, 320);
                    String actor = cell == 1 ? GameData.createPlayer("Mage").sprite : "bramble_boar";
                    scene.drawImage(assets.spriteFit(actor, 160, 180), cell == 3 ? 45 : 160, 78, null);
                    if (cell == 3) scene.drawImage(assets.spriteFit("bandit_archer", 140, 180), 290, 85, null);
                    if (classEffects) art.draw(scene, ClassAbilityVfx.forName(kinds[cell]), 240, 160, cell == 3 ? 440 : 230, 240, p);
                    else art.draw(scene, kinds[cell], 240, 160, cell == 3 ? 440 : 230, 240, p);
                    scene.setColor(new java.awt.Color(14, 20, 25, 225)); scene.fillRect(0, 0, 480, 40);
                    scene.setColor(new java.awt.Color(241, 225, 185));
                    scene.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 17));
                    scene.drawString(labels[cell], 16, 26);
                    scene.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12));
                    scene.drawString("Motion review (slowed)", 16, 306);
                    scene.dispose();
                }
                g.dispose();
                if (frame == 8 || frame == 12 || frame == 24 || frame == 36)
                    ImageIO.write(image, "png", output.resolve((classEffects ? "class-process-phase-" : "process-phase-") + frame + ".png").toFile());
                var metadata = writer.getDefaultImageMetadata(javax.imageio.ImageTypeSpecifier.createFromRenderedImage(image), null);
                var root = (javax.imageio.metadata.IIOMetadataNode) metadata.getAsTree("javax_imageio_gif_image_1.0");
                var control = (javax.imageio.metadata.IIOMetadataNode) root.getElementsByTagName("GraphicControlExtension").item(0);
                control.setAttribute("delayTime", "3"); control.setAttribute("disposalMethod", "none");
                control.setAttribute("userInputFlag", "FALSE"); control.setAttribute("transparentColorFlag", "FALSE");
                if (frame == 0) {
                    var extensions = new javax.imageio.metadata.IIOMetadataNode("ApplicationExtensions");
                    var loop = new javax.imageio.metadata.IIOMetadataNode("ApplicationExtension");
                    loop.setAttribute("applicationID", "NETSCAPE"); loop.setAttribute("authenticationCode", "2.0");
                    loop.setUserObject(new byte[]{1, 0, 0}); extensions.appendChild(loop); root.appendChild(extensions);
                }
                metadata.setFromTree("javax_imageio_gif_image_1.0", root);
                writer.writeToSequence(new javax.imageio.IIOImage(image, null, metadata), null);
            }
            writer.endWriteSequence();
        } finally { writer.dispose(); }
    }

    private static void checkStatusVfx() throws Exception {
        Actor player = GameData.createPlayer("Mage");
        Battle battle = new Battle(player, GameData.MONSTERS.get("skeleton"), new Random(1));
        var apply = Battle.class.getDeclaredMethod("applyStatus", Actor.class, String.class, Integer.class);
        apply.setAccessible(true);
        var renderer = new com.alderfall.game.render.battle.BattleVfxRenderer(new AssetStore(Path.of("assets")),
                new com.alderfall.game.render.battle.BattleVfxRenderer.Effects() {
                    public int frame() { return 25; }
                    public int scaled(int value) { return value; }
                    public int[] battleActorCenter(Battle b, Actor a, int x, int y, int w, int h) { return new int[]{100, 100}; }
                });
        for (String key : List.of("burn", "frozen", "poison", "regeneration", "shield")) {
            apply.invoke(battle, battle.enemy, key, null);
            require(statusPixels(renderer, battle) > 0, "Active status must render: " + key);
            for (EffectStack stack : battle.statusesFor(battle.enemy)) stack.turnsRemaining = 0;
            require(statusPixels(renderer, battle) == 0, "Expired status must not render");
        }
        apply.invoke(battle, battle.enemy, "frozen", null);
        var cleanse = Battle.class.getDeclaredMethod("cleanseNegativeStatuses", Actor.class);
        cleanse.setAccessible(true);
        cleanse.invoke(battle, battle.enemy);
        require(battle.statusesFor(battle.enemy).stream().noneMatch(s -> s.effect.key().equals("frozen")), "Cleanse must remove frost");
        apply.invoke(battle, battle.enemy, "burn", null);
        battle.enemy.hp = 0;
        require(statusPixels(renderer, battle) == 0, "Defeated actors must not retain status VFX");
    }

    private static int statusPixels(com.alderfall.game.render.battle.BattleVfxRenderer renderer, Battle battle) {
        BufferedImage image = new BufferedImage(220, 240, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        renderer.drawActorStatuses(g, battle, battle.enemy, 20, 20, 160, 190);
        g.dispose();
        int pixels = 0;
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++)
            if ((image.getRGB(x, y) >>> 24) != 0) pixels++;
        return pixels;
    }

    private static void checkFormations() {
        for (boolean enemy : new boolean[]{false, true}) for (int count = 1; count <= (enemy ? 5 : 4); count++) {
            List<Rectangle> bounds = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                int[] pos = BattleFormation.position(enemy, i, count, 0, 0, 1536, 1080);
                int base = enemy ? BattleFormation.enemySize(count) : 150;
                int height = enemy ? (int) Math.round(base * (count >= 3 ? 1.08 : 1.34)) : 194;
                int width = enemy ? Math.max(220, height) : 178;
                Rectangle box = new Rectangle(pos[0] + base / 2 - width / 2, pos[1] + (enemy ? base : height) - height,
                        width, height + (enemy ? 78 : 86));
                require(box.y > 70 && box.y + box.height < 900 && box.x >= 0 && box.x + box.width < 1536, "Actor outside battlefield");
                for (Rectangle other : bounds) require(!box.intersects(other), "Formation overlap: " + enemy + "/" + count);
                bounds.add(box);
            }
        }
    }

    private static void capture(Path output) {
        GamePanel panel = new GamePanel(Path.of("").toAbsolutePath());
        try {
            ((Timer) field(GamePanel.class, "timer").get(panel)).stop();
            GameState state = (GameState) field(GamePanel.class, "state").get(panel);
            state.chooseClass("Mage");
            while (state.mode == GameMode.STORY_INTRO) state.advanceStoryIntro();
            List<Actor> allies = List.of(new Actor("Seraphine Vale", "npc_seraphine", "Veilrunner", 80, 40, 12, 8),
                    new Actor("Maera Quill", "npc_maera", "Mage", 70, 60, 10, 6),
                    new Actor("Aria Foxglove", "npc_aria", "Ranger", 85, 40, 12, 7));
            for (int count : new int[]{1, 2, 5}) {
                List<GameData.MonsterSpec> specs = new ArrayList<>();
                for (int i = 0; i < count; i++) specs.add(GameData.MONSTERS.get(i % 2 == 0 ? "bramble_boar" : "bandit_archer"));
                Battle battle = new Battle(state.player, allies, specs, new Random(1), 'g', "overworld");
                battle.backdrop = count == 1 ? "battle_meadow_hills_backdrop" : count == 2 ? "battle_coastal_meadow_backdrop" : "battle_desert_foothills_backdrop";
                state.battle = battle;
                state.mode = GameMode.BATTLE;
                panel.setSize(GameConfig.WIDTH, GameConfig.HEIGHT);
                save(panel, output.resolve("formation-" + count + ".png"));
                if (count == 2) {
                    var applyStatus = Battle.class.getDeclaredMethod("applyStatus", Actor.class, String.class, Integer.class);
                    applyStatus.setAccessible(true);
                    applyStatus.invoke(battle, battle.enemies().get(0), "burn", null);
                    applyStatus.invoke(battle, battle.enemies().get(1), "frozen", null);
                    save(panel, output.resolve("burn-and-frozen.png"));
                    BattleActionAnimation animation = new BattleActionAnimation(state.player, battle.enemies().get(0), "firebolt_unique", 10, 20, 20);
                    field(Battle.class, "actionAnimation").set(battle, animation);
                    for (int frame = 0; frame < animation.castFrames - 1; frame++) animation.tick();
                    save(panel, output.resolve("fire-cast.png"));
                    while (animation.frame() < animation.castFrames + animation.travelFrames + 4) animation.tick();
                    save(panel, output.resolve("fire-impact.png"));
                    animation = new BattleActionAnimation(state.player, battle.enemies().get(1), "frost_lance_unique", 10, 20, 20);
                    field(Battle.class, "actionAnimation").set(battle, animation);
                    while (animation.frame() < animation.castFrames + animation.travelFrames + 5) animation.tick();
                    save(panel, output.resolve("frost-impact.png"));
                    for (String kind : com.alderfall.game.render.battle.AbilityImpactArt.SPRITES.keySet()) {
                        boolean heal = List.of("root_memory", "mend_unique", "grove_hymn", "seraphic_hymn").contains(kind);
                        boolean single = kind.equals("mend_unique") || kind.equals("thorn_lash_unique") || kind.equals("firebolt_unique");
                        List<Actor> recipients = heal ? battle.partyMembers() : battle.enemies();
                        if (single) recipients = List.of(recipients.get(0));
                        animation = new BattleActionAnimation(state.player, recipients.get(0), recipients,
                                single ? BattleActionAnimation.VisualMode.SINGLE : BattleActionAnimation.VisualMode.AOE, kind, 10, 20, 36);
                        field(Battle.class, "actionAnimation").set(battle, animation);
                        while (animation.frame() < animation.collisionFrame(0) + animation.impactFrames * 0.48) animation.tick();
                        save(panel, output.resolve("impact-" + kind + ".png"));
                    }
                }
            }
            state.openSettings();
            save(panel, output.resolve("settings-animation-speed.png"));
        } catch (Exception ex) { throw new IllegalStateException(ex); }
        finally { panel.shutdown(); }
    }

    private static void save(GamePanel panel, Path path) throws Exception {
        BufferedImage image = new BufferedImage(GameConfig.WIDTH, GameConfig.HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        panel.paint(g);
        g.dispose();
        ImageIO.write(image, "png", path.toFile());
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
