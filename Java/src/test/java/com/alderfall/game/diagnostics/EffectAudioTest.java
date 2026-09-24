package com.alderfall.game;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.sound.sampled.AudioSystem;

public final class EffectAudioTest {
    public static void main(String[] args) throws Exception {
        for (double speed : new double[]{.5, 1, 3}) {
            for (BattleActionAnimation.VisualMode mode : BattleActionAnimation.VisualMode.values()) {
                Actor a = new Actor("caster", "mage", "Mage", 50, 50, 5, 5);
                Actor b = new Actor("target", "knight", "Knight", 50, 50, 5, 5);
                BattleActionAnimation action = new BattleActionAnimation(a, b, List.of(a, b), mode,
                        "firebolt_unique", 20, 40, 50, speed);
                BattleSoundTimeline timeline = new BattleSoundTimeline();
                int cues = 0;
                while (action.stage() != BattleActionAnimation.Stage.DONE) {
                    List<String> sounds = timeline.impacts(action);
                    if (!sounds.isEmpty()) {
                        require(action.frame() == action.collisionFrame(0) || action.frame() == action.collisionFrame(1), "Early/late impact");
                        require(sounds.equals(List.of("fire_explosion")), "Wrong impact sample");
                        cues++;
                    }
                    require(timeline.impacts(action).isEmpty(), "Duplicate impact on repeated update/resume");
                    require(BattleSoundTimeline.flight(action).isEmpty() == (action.stage() != BattleActionAnimation.Stage.TRAVEL), "Flight escaped travel phase");
                    action.tick();
                }
                require(cues == (mode == BattleActionAnimation.VisualMode.CHAIN || mode == BattleActionAnimation.VisualMode.MULTI ? 2 : 1), "Lost collision cue");
            }
        }
        require(BattleSoundTimeline.family("glacier_prison_unique").equals("ice"), "Ice routing");
        require(BattleSoundTimeline.family("sacrifice_unique").isEmpty(), "Unrelated ability treated as ice");
        FootstepTracker steps = new FootstepTracker();
        require(!steps.advance(0, 0, 1), "Spawn made a step");
        require(!steps.advance(0, 0, 1), "Idle made a step");
        require(steps.advance(.4, 0, 1), "Travel missed contact");
        require(!steps.advance(20, 20, 1), "Teleport made a step");
        for (int i = 0; i < 100; i++) require(!steps.advance(20, 20, 1), "Blocked actor made a step");
        require(FootstepTracker.surface('B').equals("wood"), "Bridge must override water");
        require(FootstepTracker.surface('7').equals("wood"), "Plank road surface");
        require(FootstepTracker.surface('K').equals("stone"), "Cobblestone surface");
        require(FootstepTracker.surface('n').equals("snow"), "Snow surface");
        require(FootstepTracker.surface('z').equals("cloth"), "Rug surface");
        String previous = "";
        java.util.Set<String> variants = new java.util.HashSet<>();
        for (int i = 0; i < 256; i++) {
            String sample = steps.sample('K');
            require(!sample.equals(previous), "Footstep immediately repeated");
            variants.add(sample); previous = sample;
        }
        require(variants.size() == 8, "Footsteps did not use all eight variants");
        FootstepTracker fast = new FootstepTracker();
        fast.advance(0, 0, 2);
        int contacts = 0;
        for (int i = 1; i <= 20; i++) if (fast.advance(0, i * .2, 2)) contacts++;
        require(contacts <= 7, "Fast walking floods audio with contacts");
        checkCatalog();
        Path scratch = Files.createTempDirectory(Path.of("temp"), "audio-settings-");
        GameConfig config = GameConfig.loadWithSettings(scratch);
        require(config.footstepVolume == 55, "Old settings need a default");
        config.footstepVolume = 23; config.save(scratch);
        require(GameConfig.loadWithSettings(scratch).footstepVolume == 23, "Volume did not persist");
        Files.writeString(scratch.resolve("config/settings.properties"), "footstepVolume=900\n");
        require(GameConfig.loadWithSettings(scratch).footstepVolume == 100, "Saved volume not clamped");
        Files.writeString(scratch.resolve("config/settings.properties"), "footstepVolume=oops\n");
        require(GameConfig.loadWithSettings(scratch).footstepVolume == 55, "Invalid volume lost default");
        config.adjustVolume("footsteps", -200);
        require(config.footstepVolume == 0 && config.sfxVolume == 75, "Independent mute failed");
        for (String surface : List.of("grass", "dirt", "stone", "wood", "sand", "snow", "water", "cloth")) {
            for (int variant = 1; variant <= 8; variant++) checkWave("foot_" + surface + "_" + variant);
        }
        for (String name : List.of("fire_flight", "fire_explosion", "ice_flight", "ice_shatter", "lightning_flight", "lightning_impact", "arcane_flight", "arcane_impact")) checkWave(name);
        System.out.println("Audio passed: collision/release timing, class and monster catalog coverage, varied contacts, surfaces, settings and PCM decoding.");
    }

    private static void checkCatalog() throws Exception {
        Actor target = new Actor("Target", "knight", "Knight", 50, 50, 5, 5);
        for (var profile : ClassAbilityVfx.profiles().values()) {
            BattleActionAnimation action = new BattleActionAnimation(target, target, profile.baseEffect(), 5, 8, 10);
            action.configureVisual(profile.name(), profile.kind());
            require(!BattleSoundTimeline.family(action).isEmpty(), "Unrouted class ability: " + profile.name());
            exercise(action);
        }
        for (var spec : GameData.MONSTERS.values()) {
            Actor source = new Actor(spec.name(), spec.sprite(), "Monster", 50, 50, 5, 5);
            BattleActionAnimation basic = new BattleActionAnimation(source, target, "strike", 5, 8, 10);
            basic.configureMonsterVisual("Basic attack", MonsterAbilityVfx.basic(spec.sprite()));
            exercise(basic);
            var abilities = new java.util.ArrayList<>(MonsterAbilities.forMonster(spec));
            abilities.addAll(MonsterAbilities.eliteAbilitiesFor(spec));
            abilities.addAll(MonsterAbilities.bossAbilitiesFor(spec, 3));
            for (var ability : abilities) {
                BattleActionAnimation action = new BattleActionAnimation(source, target, ability.effect(), 5, 8, 10);
                action.configureMonsterVisual(ability.name(), MonsterAbilityVfx.forAbility(spec.sprite(), ability));
                require(!BattleSoundTimeline.family(action).isEmpty(), "Unrouted monster ability: " + ability.name());
                exercise(action);
            }
        }
        Actor wolf = new Actor("Wolf", "wolf", "Monster", 50, 0, 5, 5);
        BattleActionAnimation bite = new BattleActionAnimation(wolf, target, "fang", 5, 8, 10);
        bite.configureMonsterVisual("Hamstring Bite", new MonsterAbilityVfx.Profile("fx_claw", MonsterAbilityVfx.Motion.MELEE));
        require(BattleSoundTimeline.family(bite).equals("bite"), "Bite became generic slash");
        require(BattleSoundTimeline.monsterVoice(bite).equals("monster_growl"), "Wolf lost growl");
        BattleActionAnimation named = new BattleActionAnimation(target, target, "prismatic", 5, 8, 10);
        named.configureVisual("Crown Splitter", Ability.AbilityKind.DAMAGE);
        require(BattleSoundTimeline.family(named).equals("blunt"), "Gameplay key overrode actual visual family");
    }

    private static final java.util.Set<String> checked = new java.util.HashSet<>();
    private static void exercise(BattleActionAnimation action) throws Exception {
        BattleSoundTimeline timeline = new BattleSoundTimeline();
        int hits = 0;
        while (action.stage() != BattleActionAnimation.Stage.DONE) {
            for (String cue : timeline.releases(action)) {
                require(action.frame() == action.castFrames, "Release did not coincide with release frame");
                if (checked.add(cue)) checkWave(cue);
            }
            require(timeline.releases(action).isEmpty(), "Release repeated on same frame");
            for (String cue : timeline.impacts(action)) {
                hits++;
                if (checked.add(cue)) checkWave(cue);
            }
            String flight = BattleSoundTimeline.flight(action);
            if (!flight.isEmpty() && checked.add(flight)) checkWave(flight);
            if (action.monsterVisual() != null && action.monsterVisual().motion() != MonsterAbilityVfx.Motion.PROJECTILE)
                require(flight.isEmpty(), "Melee/self action played a flight loop");
            action.tick();
        }
        require(hits == 1, "Missing or duplicated impact for " + action.visualName());
    }

    private static void checkWave(String name) throws Exception {
        try (var stream = AudioSystem.getAudioInputStream(Path.of("assets/sfx", name + ".wav").toFile())) {
            require(stream.getFormat().getSampleSizeInBits() == 16 && stream.getFrameLength() > 1000, "Invalid PCM: " + name);
        }
    }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
