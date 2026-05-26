package com.alderfall.game;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public final class MusicManager {
    private final Path musicDir;
    private float volume;
    private Clip currentClip;
    private String currentTrack;
    private boolean available = true;

    public MusicManager(Path musicDir) {
        this(musicDir, 0.42f);
    }

    public MusicManager(Path musicDir, float volume) {
        this.musicDir = musicDir;
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
    }

    public void play(String track) {
        if (track == null || track.isBlank()) {
            stop();
            return;
        }
        if (!available || track.equals(currentTrack)) {
            return;
        }
        Path path = musicDir.resolve(track + ".wav");
        if (!Files.isRegularFile(path)) {
            stop();
            return;
        }
        Clip nextClip;
        try {
            nextClip = loadClip(path);
        } catch (IOException | LineUnavailableException | UnsupportedAudioFileException | RuntimeException ex) {
            available = false;
            stop();
            return;
        }
        stop();
        currentClip = nextClip;
        currentTrack = track;
        currentClip.loop(Clip.LOOP_CONTINUOUSLY);
        currentClip.start();
    }

    public void stop() {
        if (currentClip != null) {
            currentClip.stop();
            currentClip.close();
        }
        currentClip = null;
        currentTrack = null;
    }

    public void shutdown() {
        stop();
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
        if (currentClip != null) {
            applyVolume(currentClip);
        }
    }

    private Clip loadClip(Path path) throws IOException, LineUnavailableException, UnsupportedAudioFileException {
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(path.toFile())) {
            Clip clip = AudioSystem.getClip();
            clip.open(stream);
            applyVolume(clip);
            return clip;
        }
    }

    private void applyVolume(Clip clip) {
        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            return;
        }
        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        float decibels = volume <= 0.0f ? gain.getMinimum() : (float) (20.0 * Math.log10(volume));
        gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), decibels)));
    }
}
