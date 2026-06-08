"""
Procedural music generator for the RPG.

Creates short loop-friendly WAV tracks for zones, battles, and stingers.
The synth is intentionally simple and dependency-free: square/triangle/sine
voices, light percussion, and deterministic presets.

Usage:
    python tools/musicgen.py
    python tools/musicgen.py --list
    python tools/musicgen.py --only zone_forest battle_boss --bars 24
    python tools/musicgen.py --sample-pack
"""

from __future__ import annotations

import argparse
import math
import random
import struct
import wave
from dataclasses import dataclass, replace
from pathlib import Path
from typing import Callable, Iterable


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "assets" / "music"
SAMPLE_RATE = 22_050
TAU = math.pi * 2
DEFAULT_BARS = 24

NOTE_INDEX = {
    "C": 0,
    "C#": 1,
    "Db": 1,
    "D": 2,
    "D#": 3,
    "Eb": 3,
    "E": 4,
    "F": 5,
    "F#": 6,
    "Gb": 6,
    "G": 7,
    "G#": 8,
    "Ab": 8,
    "A": 9,
    "A#": 10,
    "Bb": 10,
    "B": 11,
}

SCALES = {
    "C_major": ["C", "D", "E", "F", "G", "A", "B"],
    "A_minor": ["A", "B", "C", "D", "E", "F", "G"],
    "D_dorian": ["D", "E", "F", "G", "A", "B", "C"],
    "D_minor": ["D", "E", "F", "G", "A", "Bb", "C"],
    "E_minor": ["E", "F#", "G", "A", "B", "C", "D"],
    "F_minor": ["F", "G", "Ab", "Bb", "C", "Db", "Eb"],
    "G_minor": ["G", "A", "Bb", "C", "D", "Eb", "F"],
}


@dataclass(frozen=True)
class TrackPreset:
    tempo: int
    scale: str
    progression: tuple[tuple[str, ...], ...]
    lead: tuple[int, ...]
    arp: tuple[int, ...]
    mood: str
    bass_octave: int = 2
    lead_octave: int = 5
    drum: str = "soft"
    pad_wave: str = "triangle"
    lead_wave: str = "square"
    density: float = 1.0
    pad_volume: float = 0.055
    arp_volume: float = 0.045
    bass_volume: float = 0.09
    lead_volume: float = 0.105
    drum_volume: float = 1.0
    ceiling: float = 0.86
    long_pads: bool = False
    counterline: bool = False
    shimmer: bool = False
    detune_cents: float = 3.5
    vibrato_depth: float = 0.0018
    delay_mix: float = 0.08
    reverb_mix: float = 0.14
    filter_warmth: float = 0.10
    saturation: float = 1.12


@dataclass(frozen=True)
class SampleOption:
    suffix: str
    label: str
    description: str
    tempo_scale: float = 1.0
    drum: str | None = None
    pad_wave: str | None = None
    lead_wave: str | None = None
    density: float | None = None
    pad_volume: float = 1.0
    arp_volume: float = 1.0
    bass_volume: float = 1.0
    lead_volume: float = 1.0
    drum_volume: float = 1.0
    ceiling: float | None = None
    long_pads: bool | None = None
    counterline: bool | None = None
    shimmer: bool | None = None
    bass_octave_delta: int = 0
    lead_octave_delta: int = 0
    detune_cents: float | None = None
    vibrato_depth: float | None = None
    delay_mix: float | None = None
    reverb_mix: float | None = None
    filter_warmth: float | None = None
    saturation: float | None = None


PRESETS: dict[str, TrackPreset] = {
    "zone_grasslands": TrackPreset(
        tempo=96,
        scale="C_major",
        progression=(("C", "E", "G"), ("G", "B", "D"), ("A", "C", "E"), ("F", "A", "C")),
        lead=(0, 2, 4, 2, 3, 5, 4, 2),
        arp=(0, 1, 2, 1),
        mood="open fields and early adventure",
        drum="soft",
    ),
    "zone_forest": TrackPreset(
        tempo=86,
        scale="D_dorian",
        progression=(("D", "F", "A"), ("F", "A", "C"), ("G", "B", "D"), ("C", "E", "G")),
        lead=(0, 2, 3, 5, 4, 3, 2, 0),
        arp=(0, 2, 1, 2),
        mood="mysterious forest",
        drum="wood",
        lead_wave="triangle",
    ),
    "zone_desert": TrackPreset(
        tempo=88,
        scale="A_minor",
        progression=(("A", "C", "E"), ("G", "B", "D"), ("F", "A", "C"), ("E", "G", "B")),
        lead=(0, 1, 4, 3, 2, 5, 4, 1),
        arp=(0, 1, 0, 2),
        mood="dry desert travel",
        drum="soft",
        lead_wave="sine",
    ),
    "zone_marsh": TrackPreset(
        tempo=74,
        scale="F_minor",
        progression=(("F", "Ab", "C"), ("Db", "F", "Ab"), ("Eb", "G", "Bb"), ("C", "Eb", "G")),
        lead=(0, 1, 0, 3, 2, 1, 4, 2),
        arp=(0, 1, 2, 1),
        mood="slow marsh and fireflies",
        drum="drip",
        density=0.85,
    ),
    "zone_mountains": TrackPreset(
        tempo=82,
        scale="E_minor",
        progression=(("E", "G", "B"), ("C", "E", "G"), ("D", "F#", "A"), ("B", "D", "F#")),
        lead=(0, 4, 5, 4, 2, 4, 3, 1),
        arp=(0, 2, 1, 2),
        mood="cold mountain passes",
        drum="soft",
        pad_wave="sine",
    ),
    "zone_tundra": TrackPreset(
        tempo=70,
        scale="G_minor",
        progression=(("G", "Bb", "D"), ("Eb", "G", "Bb"), ("F", "A", "C"), ("D", "F", "A")),
        lead=(0, 2, 4, 6, 5, 4, 2, 1),
        arp=(0, 1, 2, 1),
        mood="snow and long horizons",
        drum="none",
        pad_wave="sine",
        lead_wave="triangle",
        density=0.7,
    ),
    "zone_badlands": TrackPreset(
        tempo=102,
        scale="D_minor",
        progression=(("D", "F", "A"), ("Bb", "D", "F"), ("C", "E", "G"), ("A", "C", "E")),
        lead=(0, 4, 3, 1, 0, 5, 4, 2),
        arp=(0, 0, 2, 1),
        mood="dust, rock, and danger",
        drum="heavy",
    ),
    "zone_water": TrackPreset(
        tempo=78,
        scale="C_major",
        progression=(("F", "A", "C"), ("C", "E", "G"), ("G", "B", "D"), ("A", "C", "E")),
        lead=(4, 5, 4, 2, 0, 2, 3, 5),
        arp=(0, 1, 2, 1),
        mood="riverbanks and shorelines",
        drum="drip",
        pad_wave="sine",
        lead_wave="triangle",
        density=0.75,
    ),
    "town_village": TrackPreset(
        tempo=92,
        scale="C_major",
        progression=(("C", "E", "G"), ("F", "A", "C"), ("G", "B", "D"), ("C", "E", "G")),
        lead=(0, 2, 4, 2, 5, 4, 3, 2),
        arp=(0, 1, 2, 1),
        mood="warm village",
        drum="soft",
        lead_wave="triangle",
    ),
    "dungeon_crypt": TrackPreset(
        tempo=76,
        scale="C_major",
        progression=(("C", "Eb", "G"), ("Ab", "C", "Eb"), ("Bb", "D", "F"), ("G", "Bb", "D")),
        lead=(0, 1, 0, 3, 2, 1, 4, 2),
        arp=(0, 1, 0, 2),
        mood="dark dungeon",
        drum="drip",
        bass_octave=1,
        density=0.7,
    ),
    "battle_standard": TrackPreset(
        tempo=138,
        scale="G_minor",
        progression=(("G", "Bb", "D"), ("Eb", "G", "Bb"), ("F", "A", "C"), ("D", "F", "A")),
        lead=(0, 4, 3, 4, 5, 4, 3, 1),
        arp=(0, 1, 2, 1),
        mood="standard battle",
        drum="battle",
        bass_octave=2,
        density=1.25,
    ),
    "battle_boss": TrackPreset(
        tempo=156,
        scale="F_minor",
        progression=(("F", "Ab", "C"), ("Db", "F", "Ab"), ("Eb", "G", "Bb"), ("C", "Eb", "G")),
        lead=(0, 4, 6, 5, 4, 3, 4, 1),
        arp=(0, 2, 1, 2),
        mood="boss battle",
        drum="battle",
        bass_octave=1,
        density=1.45,
    ),
    "stinger_victory": TrackPreset(
        tempo=124,
        scale="C_major",
        progression=(("C", "E", "G"), ("F", "A", "C"), ("G", "B", "D"), ("C", "E", "G")),
        lead=(0, 2, 4, 5, 4, 2, 0, 4),
        arp=(0, 1, 2, 1),
        mood="victory fanfare",
        drum="soft",
        lead_wave="triangle",
    ),
}


SAMPLE_OPTIONS = (
    SampleOption(
        suffix="hush",
        label="Hush",
        description="very soft ambient bed with long pads and almost no rhythmic edge",
        tempo_scale=0.86,
        drum="none",
        pad_wave="sine",
        lead_wave="bell",
        density=0.42,
        pad_volume=0.72,
        arp_volume=0.28,
        bass_volume=0.42,
        lead_volume=0.30,
        drum_volume=0.0,
        ceiling=0.42,
        long_pads=True,
        counterline=False,
        shimmer=True,
        lead_octave_delta=-1,
        detune_cents=6.0,
        vibrato_depth=0.0024,
        delay_mix=0.12,
        reverb_mix=0.28,
        filter_warmth=0.22,
        saturation=1.04,
    ),
    SampleOption(
        suffix="lyrical",
        label="Lyrical",
        description="soft but more melodic, with a clear tune and gentle answer phrase",
        tempo_scale=0.94,
        drum="soft",
        pad_wave="sine",
        lead_wave="triangle",
        density=0.82,
        pad_volume=0.88,
        arp_volume=0.70,
        bass_volume=0.72,
        lead_volume=0.70,
        drum_volume=0.35,
        ceiling=0.58,
        long_pads=True,
        counterline=True,
        shimmer=True,
        detune_cents=4.5,
        vibrato_depth=0.0020,
        delay_mix=0.10,
        reverb_mix=0.20,
        filter_warmth=0.15,
        saturation=1.08,
    ),
    SampleOption(
        suffix="pulse",
        label="Pulse",
        description="travel groove with a steadier bass and brighter arpeggio motion",
        tempo_scale=1.08,
        pad_wave="triangle",
        lead_wave="soft_square",
        density=1.08,
        pad_volume=0.78,
        arp_volume=1.10,
        bass_volume=0.92,
        lead_volume=0.62,
        drum_volume=0.70,
        ceiling=0.68,
        long_pads=False,
        counterline=True,
        shimmer=False,
        detune_cents=2.5,
        vibrato_depth=0.0012,
        delay_mix=0.06,
        reverb_mix=0.11,
        filter_warmth=0.08,
        saturation=1.18,
    ),
    SampleOption(
        suffix="glass",
        label="Glass",
        description="glassy, spacious variation with bell tones and slow shimmer",
        tempo_scale=0.78,
        drum="drip",
        pad_wave="sine",
        lead_wave="bell",
        density=0.56,
        pad_volume=0.82,
        arp_volume=0.34,
        bass_volume=0.50,
        lead_volume=0.42,
        drum_volume=0.25,
        ceiling=0.50,
        long_pads=True,
        counterline=True,
        shimmer=True,
        lead_octave_delta=1,
        detune_cents=7.0,
        vibrato_depth=0.0028,
        delay_mix=0.14,
        reverb_mix=0.30,
        filter_warmth=0.20,
        saturation=1.02,
    ),
)


INSPIRED_SAMPLE_OPTIONS = (
    SampleOption(
        suffix="emberglass",
        label="Emberglass",
        description="bright bell ostinato, soft low pulse, and a wide dusk-like wash",
        tempo_scale=1.02,
        drum="soft",
        pad_wave="sine",
        lead_wave="bell",
        density=0.92,
        pad_volume=0.92,
        arp_volume=0.92,
        bass_volume=0.70,
        lead_volume=0.62,
        drum_volume=0.32,
        ceiling=0.56,
        long_pads=True,
        counterline=True,
        shimmer=True,
        lead_octave_delta=1,
        detune_cents=8.0,
        vibrato_depth=0.0026,
        delay_mix=0.15,
        reverb_mix=0.31,
        filter_warmth=0.18,
        saturation=1.06,
    ),
    SampleOption(
        suffix="nightdrive",
        label="Nightdrive",
        description="steady mid-tempo movement with rounded square lead and restrained percussion",
        tempo_scale=1.22,
        drum="soft",
        pad_wave="triangle",
        lead_wave="soft_square",
        density=1.22,
        pad_volume=0.70,
        arp_volume=1.24,
        bass_volume=1.05,
        lead_volume=0.72,
        drum_volume=0.62,
        ceiling=0.66,
        long_pads=False,
        counterline=True,
        shimmer=True,
        bass_octave_delta=0,
        detune_cents=3.0,
        vibrato_depth=0.0014,
        delay_mix=0.08,
        reverb_mix=0.14,
        filter_warmth=0.10,
        saturation=1.18,
    ),
    SampleOption(
        suffix="lowtide",
        label="Lowtide",
        description="slow submerged pads, sparse bell fragments, and almost no attack",
        tempo_scale=0.72,
        drum="none",
        pad_wave="sine",
        lead_wave="bell",
        density=0.38,
        pad_volume=0.84,
        arp_volume=0.22,
        bass_volume=0.36,
        lead_volume=0.28,
        drum_volume=0.0,
        ceiling=0.40,
        long_pads=True,
        counterline=True,
        shimmer=True,
        lead_octave_delta=0,
        detune_cents=9.0,
        vibrato_depth=0.0030,
        delay_mix=0.18,
        reverb_mix=0.36,
        filter_warmth=0.26,
        saturation=1.02,
    ),
    SampleOption(
        suffix="prismarch",
        label="Prismarch",
        description="more urgent arpeggio motion with luminous high accents and a heroic lift",
        tempo_scale=1.34,
        drum=None,
        pad_wave="triangle",
        lead_wave="bell",
        density=1.38,
        pad_volume=0.66,
        arp_volume=1.36,
        bass_volume=1.12,
        lead_volume=0.66,
        drum_volume=0.78,
        ceiling=0.70,
        long_pads=False,
        counterline=True,
        shimmer=True,
        lead_octave_delta=1,
        detune_cents=4.0,
        vibrato_depth=0.0016,
        delay_mix=0.09,
        reverb_mix=0.18,
        filter_warmth=0.08,
        saturation=1.16,
    ),
)


ZONE_TRACKS = (
    "zone_grasslands",
    "zone_forest",
    "zone_desert",
    "zone_marsh",
    "zone_mountains",
    "zone_tundra",
    "zone_badlands",
    "zone_water",
)

for zone_name in ZONE_TRACKS:
    base = PRESETS[zone_name]
    PRESETS[zone_name] = replace(
        base,
        lead_volume=base.lead_volume * 0.86,
        arp_volume=base.arp_volume * 0.82,
        ceiling=0.78 if zone_name != "zone_badlands" else 0.82,
        counterline=True,
        shimmer=zone_name in {"zone_grasslands", "zone_forest", "zone_mountains", "zone_tundra", "zone_water"},
    )


AMBIENT_VARIANTS = {
    "zone_grasslands_ambient": ("zone_grasslands", "soft grassland air and distant bells", 84),
    "zone_forest_ambient": ("zone_forest", "mossy canopy ambience", 72),
    "zone_desert_ambient": ("zone_desert", "warm desert dusk ambience", 76),
    "zone_marsh_ambient": ("zone_marsh", "misty marsh hush", 66),
    "zone_mountains_ambient": ("zone_mountains", "thin mountain wind and low echoes", 68),
    "zone_tundra_ambient": ("zone_tundra", "snowfield quiet and glassy tones", 62),
    "zone_badlands_ambient": ("zone_badlands", "soft red-rock dusk", 78),
    "zone_water_ambient": ("zone_water", "slow water shimmer", 66),
}

for ambient_name, (base_name, mood, tempo) in AMBIENT_VARIANTS.items():
    base = PRESETS[base_name]
    PRESETS[ambient_name] = replace(
        base,
        tempo=tempo,
        mood=mood,
        drum="none",
        pad_wave="sine",
        lead_wave="triangle",
        density=min(base.density, 0.55),
        pad_volume=0.042,
        arp_volume=0.018,
        bass_volume=0.038,
        lead_volume=0.035,
        drum_volume=0.0,
        ceiling=0.48,
        long_pads=True,
        counterline=True,
        shimmer=True,
        detune_cents=6.0,
        vibrato_depth=0.0024,
        delay_mix=0.12,
        reverb_mix=0.26,
        filter_warmth=0.22,
        saturation=1.04,
    )


def note_frequency(note: str, octave: int) -> float:
    semitone = NOTE_INDEX[note] - NOTE_INDEX["A"] + (octave - 4) * 12
    return 440.0 * (2 ** (semitone / 12))


def sine(phase: float) -> float:
    return math.sin(TAU * phase)


def square(phase: float) -> float:
    return 1.0 if (phase % 1.0) < 0.5 else -1.0


def triangle(phase: float) -> float:
    return 4.0 * abs((phase % 1.0) - 0.5) - 1.0


def soft_square(phase: float) -> float:
    return math.tanh(math.sin(TAU * phase) * 2.2)


def saw(phase: float) -> float:
    return 2.0 * (phase % 1.0) - 1.0


def bell(phase: float) -> float:
    return (
        math.sin(TAU * phase) * 0.72
        + math.sin(TAU * phase * 2.01) * 0.20
        + math.sin(TAU * phase * 3.02) * 0.08
    )


WAVES: dict[str, Callable[[float], float]] = {
    "sine": sine,
    "square": square,
    "soft_square": soft_square,
    "saw": saw,
    "triangle": triangle,
    "bell": bell,
}


def envelope(position: int, length: int, attack: float = 0.015, release: float = 0.06) -> float:
    if length <= 0:
        return 0.0
    attack_samples = max(1, int(SAMPLE_RATE * attack))
    release_samples = max(1, int(SAMPLE_RATE * release))
    if position < attack_samples:
        return position / attack_samples
    if position > length - release_samples:
        return max(0.0, (length - position) / release_samples)
    return 1.0


def mix_tone(
    audio: list[float],
    start_sec: float,
    duration_sec: float,
    frequency: float,
    volume: float,
    wave_name: str,
    attack: float = 0.01,
    release: float = 0.05,
    detune_cents: float = 0.0,
    vibrato_depth: float = 0.0,
) -> None:
    if not audio:
        return
    if detune_cents > 0.0:
        spread = 2 ** (detune_cents / 1200.0)
        mix_tone(audio, start_sec, duration_sec, frequency, volume * 0.68, wave_name, attack, release, 0.0, vibrato_depth)
        mix_tone(audio, start_sec, duration_sec, frequency * spread, volume * 0.16, wave_name, attack, release, 0.0, vibrato_depth * 0.72)
        mix_tone(audio, start_sec, duration_sec, frequency / spread, volume * 0.16, wave_name, attack, release, 0.0, vibrato_depth * 0.72)
        return
    start = max(0, int(start_sec * SAMPLE_RATE))
    length = max(1, int(duration_sec * SAMPLE_RATE))
    wave_fn = WAVES[wave_name]
    for local in range(length):
        out_idx = (start + local) % len(audio)
        seconds = local / SAMPLE_RATE
        phase = seconds * frequency
        if vibrato_depth > 0.0:
            phase += math.sin(TAU * 5.2 * seconds) * vibrato_depth
        env = envelope(local, length, attack, release)
        audio[out_idx] += wave_fn(phase) * volume * env


def mix_noise(audio: list[float], start_sec: float, duration_sec: float, volume: float, rng: random.Random) -> None:
    if not audio:
        return
    start = max(0, int(start_sec * SAMPLE_RATE))
    length = max(1, int(duration_sec * SAMPLE_RATE))
    for local in range(length):
        out_idx = (start + local) % len(audio)
        decay = 1.0 - (local / length)
        audio[out_idx] += rng.uniform(-1.0, 1.0) * volume * decay


def mix_kick(audio: list[float], start_sec: float, volume: float = 0.38) -> None:
    if not audio:
        return
    start = max(0, int(start_sec * SAMPLE_RATE))
    length = int(0.11 * SAMPLE_RATE)
    for local in range(length):
        out_idx = (start + local) % len(audio)
        pos = local / max(1, length)
        freq = 92 - 48 * pos
        decay = (1.0 - pos) ** 2
        audio[out_idx] += math.sin(TAU * freq * (local / SAMPLE_RATE)) * volume * decay


def mix_drums(audio: list[float], bar_start: float, beat: float, style: str, rng: random.Random, volume_scale: float = 1.0) -> None:
    if style == "none":
        return

    for b in range(4):
        t = bar_start + b * beat
        if style == "battle":
            mix_kick(audio, t, 0.34 * volume_scale)
            mix_noise(audio, t + beat * 0.5, 0.035, 0.05 * volume_scale, rng)
            if b in (1, 3):
                mix_noise(audio, t, 0.07, 0.17 * volume_scale, rng)
        elif style == "heavy":
            if b in (0, 2):
                mix_kick(audio, t, 0.25 * volume_scale)
            mix_noise(audio, t + beat * 0.5, 0.04, 0.045 * volume_scale, rng)
        elif style == "wood":
            if b in (0, 2):
                mix_tone(audio, t, 0.045, 180, 0.08 * volume_scale, "triangle", release=0.02)
        elif style == "drip":
            if b in (1, 3):
                mix_tone(audio, t + beat * 0.25, 0.08, 680 + b * 45, 0.045 * volume_scale, "sine", release=0.04)
        elif b in (0, 2):
            mix_kick(audio, t, 0.09 * volume_scale)


def normalize(audio: list[float], ceiling: float = 0.86) -> list[float]:
    peak = max((abs(sample) for sample in audio), default=0.0)
    if peak <= 0:
        return audio
    gain = ceiling / peak
    return [sample * gain for sample in audio]


def seal_loop_boundary(audio: list[float]) -> list[float]:
    if len(audio) < 2:
        return audio
    delta = audio[-1] - audio[0]
    span = len(audio) - 1
    return [sample - delta * (index / span) for index, sample in enumerate(audio)]


def apply_warm_filter(audio: list[float], warmth: float) -> list[float]:
    if not audio or warmth <= 0.0:
        return audio
    alpha = max(0.02, min(0.55, warmth))
    filtered: list[float] = []
    previous = audio[-1]
    for sample in audio:
        previous += (sample - previous) * alpha
        filtered.append(previous)
    previous = filtered[0]
    for index in range(len(filtered) - 1, -1, -1):
        previous += (filtered[index] - previous) * alpha
        filtered[index] = previous
    return filtered


def apply_saturation(audio: list[float], drive: float) -> list[float]:
    if not audio or drive <= 1.0:
        return audio
    ceiling = math.tanh(drive)
    return [math.tanh(sample * drive) / ceiling for sample in audio]


def apply_tap_delay(audio: list[float], delay_sec: float, mix: float) -> list[float]:
    if not audio or mix <= 0.0:
        return audio
    delay = max(1, int(delay_sec * SAMPLE_RATE))
    return [sample + audio[(index - delay) % len(audio)] * mix for index, sample in enumerate(audio)]


def apply_room(audio: list[float], mix: float) -> list[float]:
    if not audio or mix <= 0.0:
        return audio
    room = audio
    for delay_sec, amount in ((0.073, 0.34), (0.137, 0.24), (0.211, 0.16), (0.319, 0.11)):
        room = apply_tap_delay(room, delay_sec, mix * amount)
    return room


def post_process(audio: list[float], preset: TrackPreset) -> list[float]:
    processed = apply_saturation(audio, preset.saturation)
    processed = apply_tap_delay(processed, 0.23, preset.delay_mix)
    processed = apply_room(processed, preset.reverb_mix)
    processed = apply_warm_filter(processed, preset.filter_warmth)
    return processed


def write_wav(path: Path, audio: list[float]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(path), "wb") as wav_file:
        wav_file.setnchannels(1)
        wav_file.setsampwidth(2)
        wav_file.setframerate(SAMPLE_RATE)
        frames = bytearray()
        for sample in audio:
            clamped = max(-1.0, min(1.0, sample))
            frames.extend(struct.pack("<h", int(clamped * 32767)))
        wav_file.writeframes(frames)


def scale_note(preset: TrackPreset, index: int) -> str:
    scale = SCALES[preset.scale]
    return scale[index % len(scale)]


def arranged_preset(base: TrackPreset, option: SampleOption) -> TrackPreset:
    return replace(
        base,
        tempo=max(40, round(base.tempo * option.tempo_scale)),
        mood=f"{base.mood}; {option.description}",
        bass_octave=max(1, base.bass_octave + option.bass_octave_delta),
        lead_octave=max(3, base.lead_octave + option.lead_octave_delta),
        drum=option.drum or base.drum,
        pad_wave=option.pad_wave or base.pad_wave,
        lead_wave=option.lead_wave or base.lead_wave,
        density=option.density if option.density is not None else base.density,
        pad_volume=base.pad_volume * option.pad_volume,
        arp_volume=base.arp_volume * option.arp_volume,
        bass_volume=base.bass_volume * option.bass_volume,
        lead_volume=base.lead_volume * option.lead_volume,
        drum_volume=base.drum_volume * option.drum_volume,
        ceiling=option.ceiling if option.ceiling is not None else base.ceiling,
        long_pads=option.long_pads if option.long_pads is not None else base.long_pads,
        counterline=option.counterline if option.counterline is not None else base.counterline,
        shimmer=option.shimmer if option.shimmer is not None else base.shimmer,
        detune_cents=option.detune_cents if option.detune_cents is not None else base.detune_cents,
        vibrato_depth=option.vibrato_depth if option.vibrato_depth is not None else base.vibrato_depth,
        delay_mix=option.delay_mix if option.delay_mix is not None else base.delay_mix,
        reverb_mix=option.reverb_mix if option.reverb_mix is not None else base.reverb_mix,
        filter_warmth=option.filter_warmth if option.filter_warmth is not None else base.filter_warmth,
        saturation=option.saturation if option.saturation is not None else base.saturation,
    )


def render_track(name: str, preset: TrackPreset, bars: int) -> list[float]:
    rng = random.Random(name)
    beat = 60 / preset.tempo
    total_sec = bars * 4 * beat
    audio = [0.0] * int(SAMPLE_RATE * total_sec)

    for bar in range(bars):
        bar_start = bar * 4 * beat
        chord = preset.progression[bar % len(preset.progression)]
        root = chord[0]

        for i, note in enumerate(chord):
            mix_tone(
                audio,
                bar_start + i * 0.13 * beat,
                (7.65 if preset.long_pads else 4.15) * beat,
                note_frequency(note, 4),
                preset.pad_volume,
                preset.pad_wave,
                attack=0.12 if preset.long_pads else 0.05,
                release=0.22 if preset.long_pads else 0.1,
                detune_cents=preset.detune_cents,
                vibrato_depth=preset.vibrato_depth * 0.7,
            )
            if preset.long_pads and i == 0:
                mix_tone(
                    audio,
                    bar_start + beat * 0.5,
                    6.5 * beat,
                    note_frequency(note, 3),
                    preset.pad_volume * 0.58,
                    "sine",
                    attack=0.2,
                    release=0.32,
                    detune_cents=preset.detune_cents * 0.7,
                    vibrato_depth=preset.vibrato_depth * 0.5,
                )

        for step, arp_index in enumerate(preset.arp * 2):
            note = chord[arp_index % len(chord)]
            mix_tone(
                audio,
                bar_start + step * 0.5 * beat,
                0.38 * beat,
                note_frequency(note, 4),
                preset.arp_volume,
                "triangle",
                attack=0.006,
                release=0.04,
                detune_cents=preset.detune_cents * 0.25,
                vibrato_depth=preset.vibrato_depth * 0.25,
            )

        bass_steps = 8 if preset.drum == "battle" else 4
        for step in range(bass_steps):
            start = bar_start + step * (4 / bass_steps) * beat
            mix_tone(
                audio,
                start,
                0.28 * beat,
                note_frequency(root, preset.bass_octave),
                0.12 if preset.drum == "battle" else preset.bass_volume,
                "square",
                attack=0.004,
                release=0.04,
                detune_cents=preset.detune_cents * 0.15,
            )

        lead_steps = 8 if preset.density >= 0.9 else 4
        for step in range(lead_steps):
            if preset.density < 1.0 and (bar + step) % 3 == 0:
                continue
            idx = preset.lead[(bar + step) % len(preset.lead)]
            note = scale_note(preset, idx)
            octave = preset.lead_octave if step % 4 != 3 else preset.lead_octave - 1
            start = bar_start + step * (4 / lead_steps) * beat
            mix_tone(
                audio,
                start,
                0.34 * beat,
                note_frequency(note, octave),
                preset.lead_volume,
                preset.lead_wave,
                attack=0.004,
                release=0.035,
                detune_cents=preset.detune_cents * 0.6,
                vibrato_depth=preset.vibrato_depth,
            )

        if preset.counterline and bar % 2 == 1:
            for step, idx in enumerate(reversed(preset.lead[:4])):
                note = scale_note(preset, idx + 2)
                mix_tone(
                    audio,
                    bar_start + (step + 0.5) * beat,
                    0.55 * beat,
                    note_frequency(note, preset.lead_octave - 1),
                    preset.lead_volume * 0.42,
                    "sine",
                    attack=0.025,
                    release=0.09,
                    detune_cents=preset.detune_cents * 0.45,
                    vibrato_depth=preset.vibrato_depth * 0.8,
                )

        if preset.shimmer and bar % 4 in (1, 3):
            for step in range(3):
                note = chord[(step + bar) % len(chord)]
                mix_tone(
                    audio,
                    bar_start + (1.1 + step * 0.72) * beat,
                    0.45 * beat,
                    note_frequency(note, preset.lead_octave + 1),
                    preset.lead_volume * 0.22,
                    "sine",
                    attack=0.04,
                    release=0.16,
                    detune_cents=preset.detune_cents * 0.5,
                    vibrato_depth=preset.vibrato_depth * 0.6,
                )

        mix_drums(audio, bar_start, beat, preset.drum, rng, preset.drum_volume)

    return normalize(seal_loop_boundary(post_process(audio, preset)), preset.ceiling)


def make_manifest(generated: Iterable[tuple[str, Path, TrackPreset, int]]) -> str:
    lines = ["# Music Pack", ""]
    for name, path, preset, bars in generated:
        length = bars * 4 * 60 / preset.tempo
        lines.append(f"- `{path.name}` - {preset.mood}, {preset.tempo} BPM, {length:.1f}s loop")
    lines.append("")
    lines.append("Generated with `python tools/musicgen.py`.")
    return "\n".join(lines)


def make_sample_manifest(
    generated: Iterable[tuple[str, Path, TrackPreset, int, SampleOption]],
    title: str = "Music Audition Samples",
    note: str = "Short samples generated for choosing biome music direction. These do not replace the in-game loops.",
    command: str = "python tools/musicgen.py --sample-pack",
) -> str:
    lines = [
        f"# {title}",
        "",
        note,
        "",
    ]
    for name, path, preset, bars, option in generated:
        base_name = name.split("__", 1)[0]
        length = bars * 4 * 60 / preset.tempo
        lines.append(
            f"- `{path.name}` - {base_name}, {option.label}: {option.description}; "
            f"{preset.tempo} BPM, {length:.1f}s sample"
        )
    lines.append("")
    lines.append(f"Generated with `{command}`.")
    return "\n".join(lines)


def generate_sample_pack(
    output_dir: Path,
    bars: int,
    options: tuple[SampleOption, ...] = SAMPLE_OPTIONS,
    title: str = "Music Audition Samples",
    note: str = "Short samples generated for choosing biome music direction. These do not replace the in-game loops.",
    command: str = "python tools/musicgen.py --sample-pack",
) -> None:
    generated: list[tuple[str, Path, TrackPreset, int, SampleOption]] = []
    print(f"Generating {len(ZONE_TRACKS) * len(options)} audition sample(s) into {output_dir}...")
    for base_name in ZONE_TRACKS:
        base = PRESETS[base_name]
        for option in options:
            name = f"{base_name}__{option.suffix}"
            preset = arranged_preset(base, option)
            audio = render_track(name, preset, bars)
            path = output_dir / f"{name}.wav"
            write_wav(path, audio)
            generated.append((name, path, preset, bars, option))
            print(f"- {name}: {path}")

    manifest = output_dir / "README.md"
    manifest.write_text(make_sample_manifest(generated, title, note, command), encoding="utf-8")
    print(f"Done. Audition manifest: {manifest}")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate procedural RPG music loops.")
    parser.add_argument("--list", action="store_true", help="List available track presets and exit.")
    parser.add_argument("--only", nargs="+", choices=sorted(PRESETS), help="Only generate these presets.")
    parser.add_argument("--bars", type=int, default=DEFAULT_BARS, help=f"Bars per loop. Default: {DEFAULT_BARS}.")
    parser.add_argument("--out", type=Path, default=OUT, help="Output directory. Default: assets/music.")
    parser.add_argument("--sample-pack", action="store_true", help="Generate short biome audition variants without replacing game tracks.")
    parser.add_argument("--sample-bars", type=int, default=4, help="Bars per audition sample. Default: 4.")
    parser.add_argument("--sample-out", type=Path, default=OUT / "samples", help="Audition output directory. Default: assets/music/samples.")
    parser.add_argument("--inspired-pack", action="store_true", help="Generate original reference-inspired audition variants without replacing game tracks.")
    parser.add_argument("--inspired-bars", type=int, default=6, help="Bars per inspired sample. Default: 6.")
    parser.add_argument("--inspired-out", type=Path, default=OUT / "samples" / "inspired", help="Inspired audition output directory. Default: assets/music/samples/inspired.")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    if args.list:
        for name, preset in PRESETS.items():
            print(f"{name}: {preset.mood} ({preset.tempo} BPM)")
        print("")
        print("Sample options:")
        for option in SAMPLE_OPTIONS:
            print(f"{option.suffix}: {option.label} - {option.description}")
        print("")
        print("Inspired sample options:")
        for option in INSPIRED_SAMPLE_OPTIONS:
            print(f"{option.suffix}: {option.label} - {option.description}")
        return

    if args.bars < 2:
        raise SystemExit("--bars must be at least 2")

    if args.inspired_pack:
        if args.inspired_bars < 2:
            raise SystemExit("--inspired-bars must be at least 2")
        inspired_output_dir = args.inspired_out if args.inspired_out.is_absolute() else ROOT / args.inspired_out
        generate_sample_pack(
            inspired_output_dir,
            args.inspired_bars,
            INSPIRED_SAMPLE_OPTIONS,
            "Reference-Inspired Music Audition Samples",
            "Original samples using broad atmospheric traits from the reference as direction. These do not quote or recreate melodies, rhythms, or recordings, and they do not replace the in-game loops.",
            "python tools/musicgen.py --inspired-pack",
        )
        return

    if args.sample_pack:
        if args.sample_bars < 2:
            raise SystemExit("--sample-bars must be at least 2")
        sample_output_dir = args.sample_out if args.sample_out.is_absolute() else ROOT / args.sample_out
        generate_sample_pack(sample_output_dir, args.sample_bars)
        return

    output_dir = args.out if args.out.is_absolute() else ROOT / args.out
    names = args.only or list(PRESETS)
    generated: list[tuple[str, Path, TrackPreset, int]] = []

    print(f"Generating {len(names)} music loop(s) into {output_dir}...")
    for name in names:
        preset = PRESETS[name]
        audio = render_track(name, preset, args.bars)
        path = output_dir / f"{name}.wav"
        write_wav(path, audio)
        generated.append((name, path, preset, args.bars))
        print(f"- {name}: {path}")

    manifest = output_dir / "README.md"
    manifest.write_text(make_manifest(generated), encoding="utf-8")
    print(f"Done. Manifest: {manifest}")


if __name__ == "__main__":
    main()
