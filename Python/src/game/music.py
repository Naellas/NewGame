from __future__ import annotations

import sys
from pathlib import Path


class MusicManager:
    def __init__(self, music_dir: Path, volume: float = 0.42, fade_ms: int = 700) -> None:
        self.music_dir = music_dir
        self.current_track: str | None = None
        self.enabled = True
        self.volume = max(0.0, min(1.0, volume))
        self.fade_ms = max(0, fade_ms)
        self.backend = "none"
        self.available = False
        self._pygame = None
        self._channels = []
        self._channel_index = 0
        self._sounds = {}
        self._winsound = None
        self._init_pygame()
        if not self.available:
            self._init_winsound()

    def _init_pygame(self) -> None:
        try:
            import pygame

            pygame.mixer.pre_init(frequency=22_050, size=-16, channels=1, buffer=1024)
            pygame.mixer.init()
            pygame.mixer.set_num_channels(max(4, pygame.mixer.get_num_channels()))
            self._pygame = pygame
            self._channels = [pygame.mixer.Channel(0), pygame.mixer.Channel(1)]
            for channel in self._channels:
                channel.set_volume(self.volume)
            self.backend = "pygame"
            self.available = True
        except Exception:
            self._pygame = None
            self._channels = []
            self.backend = "none"
            self.available = False

    def _init_winsound(self) -> None:
        if sys.platform != "win32":
            return
        try:
            import winsound

            self._winsound = winsound
            self.backend = "winsound"
            self.available = True
        except ImportError:
            self._winsound = None

    def play(self, track: str | None, loop: bool = True) -> None:
        if not track:
            self.stop()
            return
        if not self.enabled or not self.available:
            return
        if self.current_track == track:
            return
        path = self.music_dir / f"{track}.wav"
        if not path.exists():
            return
        if self.backend == "pygame":
            self._play_pygame(track, path, loop)
        elif self.backend == "winsound":
            self._play_winsound(track, path, loop)

    def _play_pygame(self, track: str, path: Path, loop: bool) -> None:
        if self._pygame is None or not self._channels:
            return
        try:
            sound = self._sounds.get(track)
            if sound is None:
                sound = self._pygame.mixer.Sound(str(path))
                sound.set_volume(self.volume)
                self._sounds[track] = sound
            old_channel = self._channels[self._channel_index]
            self._channel_index = (self._channel_index + 1) % len(self._channels)
            new_channel = self._channels[self._channel_index]
            old_channel.fadeout(self.fade_ms)
            new_channel.set_volume(self.volume)
            new_channel.play(sound, loops=-1 if loop else 0, fade_ms=self.fade_ms)
            self.current_track = track
        except Exception:
            self.available = False
            self.current_track = None
            self._init_winsound()

    def _play_winsound(self, track: str, path: Path, loop: bool) -> None:
        if self._winsound is None:
            return
        flags = self._winsound.SND_FILENAME | self._winsound.SND_ASYNC
        if loop:
            flags |= self._winsound.SND_LOOP
        try:
            self._winsound.PlaySound(str(path), flags)
            self.current_track = track
        except RuntimeError:
            self.available = False
            self.current_track = None

    def stop(self) -> None:
        if self.current_track is None:
            return
        if self.backend == "pygame":
            for channel in self._channels:
                channel.fadeout(self.fade_ms)
        elif self.backend == "winsound" and self._winsound is not None:
            try:
                self._winsound.PlaySound(None, self._winsound.SND_ASYNC)
            except RuntimeError:
                self.available = False
        self.current_track = None

    def shutdown(self) -> None:
        self.stop()
        if self._pygame is not None:
            try:
                self._pygame.mixer.quit()
            except Exception:
                pass
