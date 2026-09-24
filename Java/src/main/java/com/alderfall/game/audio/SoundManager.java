package com.alderfall.game;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public final class SoundManager {
    private record Voice(String name, boolean footstep, float presence, boolean loop) { }
    private record Sample(AudioFormat format, byte[] pcm) { }
    private final Path soundDir;
    private final Map<Clip, Voice> activeClips = new HashMap<>();
    private final Map<String, Sample> samples = new HashMap<>();
    private long retryAfter;
    private float volume;
    private float footstepVolume = .55f;

    public SoundManager(Path soundDir) { this(soundDir, .6f); }
    public SoundManager(Path soundDir, float volume) {
        this.soundDir = soundDir;
        this.volume = clamp(volume);
    }

    public void play(String sound) { play(sound, false, 1, false); }
    void playFootstep(String sound, float presence) { play(sound, true, presence, false); }
    void playLoop(String sound) { play(sound, false, 1, true); }

    private synchronized void play(String sound, boolean footstep, float presence, boolean loop) {
        if (System.nanoTime() < retryAfter) return;
        if (sound == null || sound.isBlank() || (footstep ? footstepVolume : volume) <= 0 || presence <= 0) return;
        if (loop && activeClips.values().stream().anyMatch(v -> v.loop() && v.name().equals(sound))) return;
        // Keep incidental footsteps from using all available mixer voices.
        if (footstep && activeClips.values().stream().filter(Voice::footstep).count() >= 4) return;
        if (activeClips.size() >= (footstep ? 12 : 24)) return;
        Path path = soundDir.resolve(sound + ".wav");
        if (!Files.isRegularFile(path)) return;
        Clip clip = null;
        try {
            Sample sample = samples.get(sound);
            if (sample == null) {
                try (AudioInputStream stream = AudioSystem.getAudioInputStream(path.toFile())) {
                    sample = new Sample(stream.getFormat(), stream.readAllBytes());
                }
                samples.put(sound, sample);
            }
            clip = AudioSystem.getClip();
            clip.open(sample.format(), sample.pcm(), 0, sample.pcm().length);
            Voice voice = new Voice(sound, footstep, clamp(presence), loop);
            applyVolume(clip, voice);
            activeClips.put(clip, voice);
            Clip playing = clip;
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) release(playing);
            });
            if (loop) clip.loop(Clip.LOOP_CONTINUOUSLY);
            else clip.start();
        } catch (IOException | LineUnavailableException | UnsupportedAudioFileException | RuntimeException ex) {
            retryAfter = System.nanoTime() + 2_000_000_000L;
            // A missing device or exhausted mixer must not permanently disable every later cue.
            if (clip != null) { activeClips.remove(clip); clip.close(); }
        }
    }

    private synchronized void release(Clip clip) {
        if (activeClips.remove(clip) != null) clip.close();
    }

    synchronized void stop(String sound) {
        for (var entry : new HashMap<>(activeClips).entrySet()) {
            if (entry.getValue().name().equals(sound)) {
                activeClips.remove(entry.getKey());
                entry.getKey().stop();
                entry.getKey().close();
            }
        }
    }

    public synchronized void setVolume(float volume) {
        float next = clamp(volume);
        if (next == this.volume) return;
        this.volume = next;
        refreshVolumes();
    }

    synchronized void setFootstepVolume(float volume) {
        float next = clamp(volume);
        if (next == footstepVolume) return;
        footstepVolume = next;
        refreshVolumes();
    }

    private void refreshVolumes() {
        for (var entry : new HashMap<>(activeClips).entrySet()) {
            Clip clip = entry.getKey();
            Voice voice = entry.getValue();
            if ((voice.footstep() ? footstepVolume : volume) == 0) {
                activeClips.remove(clip);
                clip.stop(); clip.close();
            } else applyVolume(clip, voice);
        }
    }

    public synchronized void shutdown() {
        var clips = new java.util.ArrayList<>(activeClips.keySet());
        activeClips.clear();
        for (Clip clip : clips) { clip.stop(); clip.close(); }
    }

    private void applyVolume(Clip clip, Voice voice) {
        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) return;
        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        float level = (voice.footstep() ? footstepVolume : volume) * voice.presence();
        float decibels = level <= 0 ? gain.getMinimum() : (float) (20 * Math.log10(level));
        gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), decibels)));
    }

    private static float clamp(float value) { return Float.isFinite(value) ? Math.max(0, Math.min(1, value)) : 0; }
}
