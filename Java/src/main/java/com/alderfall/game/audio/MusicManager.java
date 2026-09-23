package com.alderfall.game;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public final class MusicManager {
    private static final float FADE_SECONDS = 7.5f;
    private float fadeSeconds = FADE_SECONDS;
    private static final float CLOSE_EPSILON = 0.002f;
    private final Path musicDir;
    private final Map<String, PlayingTrack> activeTracks = new HashMap<>();
    private float volume;
    private long lastUpdateNanos;
    private boolean available = true;

    public MusicManager(Path musicDir) {
        this(musicDir, 0.42f);
    }

    public MusicManager(Path musicDir, float volume) {
        this.musicDir = musicDir;
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
    }

    public void play(String track) {
        blend(track, null, 1.0f);
    }

    public void blend(String track, String bedTrack, float trackPresence) {
        blend(track, bedTrack, trackPresence, FADE_SECONDS);
    }

    public void blend(String track, String bedTrack, float trackPresence, float fadeSeconds) {
        this.fadeSeconds = Math.max(0.1f, fadeSeconds);
        if (track == null || track.isBlank()) {
            stop();
            return;
        }
        if (!available) {
            return;
        }
        float presence = Math.max(0.0f, Math.min(1.0f, trackPresence));
        if (track.equals(bedTrack)) {
            bedTrack = null;
            presence = 1.0f;
        }
        if (!ensureTrack(track)) {
            stop();
            return;
        }
        if (bedTrack != null && !bedTrack.isBlank()) {
            ensureTrack(bedTrack);
        }
        for (Map.Entry<String, PlayingTrack> entry : activeTracks.entrySet()) {
            String name = entry.getKey();
            PlayingTrack playing = entry.getValue();
            if (name.equals(track)) {
                playing.targetGain = presence;
            } else if (bedTrack != null && name.equals(bedTrack)) {
                playing.targetGain = 1.0f - presence;
            } else {
                playing.targetGain = 0.0f;
            }
        }
        update();
    }

    public void update() {
        if (activeTracks.isEmpty()) {
            lastUpdateNanos = System.nanoTime();
            return;
        }
        long now = System.nanoTime();
        float elapsed = lastUpdateNanos == 0L ? 0.05f : Math.min(0.25f, (now - lastUpdateNanos) / 1_000_000_000.0f);
        lastUpdateNanos = now;
        float step = elapsed / fadeSeconds;
        Iterator<Map.Entry<String, PlayingTrack>> iterator = activeTracks.entrySet().iterator();
        while (iterator.hasNext()) {
            PlayingTrack playing = iterator.next().getValue();
            if (playing.gain < playing.targetGain) {
                playing.gain = Math.min(playing.targetGain, playing.gain + step);
            } else if (playing.gain > playing.targetGain) {
                playing.gain = Math.max(playing.targetGain, playing.gain - step);
            }
            applyVolume(playing.clip, playing.gain);
            if (playing.gain <= CLOSE_EPSILON && playing.targetGain <= CLOSE_EPSILON) {
                playing.clip.stop();
                playing.clip.close();
                iterator.remove();
            }
        }
    }

    private boolean ensureTrack(String track) {
        if (activeTracks.containsKey(track)) {
            return true;
        }
        Path path = musicDir.resolve(track + ".wav");
        if (!Files.isRegularFile(path)) {
            return false;
        }
        Clip nextClip;
        try {
            nextClip = loadClip(path);
        } catch (IOException | LineUnavailableException | UnsupportedAudioFileException | RuntimeException ex) {
            available = false;
            stop();
            return false;
        }
        PlayingTrack playing = new PlayingTrack(nextClip);
        activeTracks.put(track, playing);
        nextClip.loop(Clip.LOOP_CONTINUOUSLY);
        nextClip.start();
        return true;
    }

    public void stop() {
        for (PlayingTrack playing : activeTracks.values()) {
            playing.clip.stop();
            playing.clip.close();
        }
        activeTracks.clear();
        lastUpdateNanos = 0L;
    }

    public void shutdown() {
        stop();
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
        for (PlayingTrack playing : activeTracks.values()) {
            applyVolume(playing.clip, playing.gain);
        }
    }

    private Clip loadClip(Path path) throws IOException, LineUnavailableException, UnsupportedAudioFileException {
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(path.toFile())) {
            Clip clip = AudioSystem.getClip();
            clip.open(stream);
            applyVolume(clip, 0.0f);
            return clip;
        }
    }

    private void applyVolume(Clip clip, float gainScale) {
        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            return;
        }
        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        float scaledVolume = volume * Math.max(0.0f, Math.min(1.0f, gainScale));
        float decibels = scaledVolume <= 0.0f ? gain.getMinimum() : (float) (20.0 * Math.log10(scaledVolume));
        gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), decibels)));
    }

    private static final class PlayingTrack {
        private final Clip clip;
        private float gain;
        private float targetGain;

        private PlayingTrack(Clip clip) {
            this.clip = clip;
        }
    }
}
