package com.alderfall.game;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public final class SoundManager {
    private final Path soundDir;
    private final Set<Clip> activeClips = Collections.synchronizedSet(new HashSet<>());
    private float volume;
    private boolean available = true;

    public SoundManager(Path soundDir) {
        this(soundDir, 0.6f);
    }

    public SoundManager(Path soundDir, float volume) {
        this.soundDir = soundDir;
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
    }

    public void play(String sound) {
        if (!available || sound == null || sound.isBlank() || volume <= 0.0f) {
            return;
        }
        Path path = soundDir.resolve(sound + ".wav");
        if (!Files.isRegularFile(path)) {
            return;
        }
        cleanupFinishedClips();
        Clip clip;
        try {
            clip = loadClip(path);
        } catch (IOException | LineUnavailableException | UnsupportedAudioFileException | RuntimeException ex) {
            available = false;
            shutdown();
            return;
        }
        activeClips.add(clip);
        clip.addLineListener(event -> {
            if (event.getType() == LineEvent.Type.STOP || event.getType() == LineEvent.Type.CLOSE) {
                activeClips.remove(clip);
                clip.close();
            }
        });
        clip.start();
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
        synchronized (activeClips) {
            for (Clip clip : activeClips) {
                applyVolume(clip);
            }
        }
    }

    public void shutdown() {
        synchronized (activeClips) {
            for (Clip clip : new HashSet<>(activeClips)) {
                clip.stop();
                clip.close();
            }
            activeClips.clear();
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

    private void cleanupFinishedClips() {
        synchronized (activeClips) {
            Iterator<Clip> iterator = activeClips.iterator();
            while (iterator.hasNext()) {
                Clip clip = iterator.next();
                if (!clip.isRunning() && !clip.isActive()) {
                    clip.close();
                    iterator.remove();
                }
            }
        }
    }
}
