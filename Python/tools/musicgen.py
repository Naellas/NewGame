"""
Procedural music generator for the RPG.

Creates short loop-friendly WAV tracks for zones, battles, and stingers.
The synth is intentionally simple and dependency-free: square/triangle/sine
voices, light percussion, and deterministic presets.

Usage:
    python tools/musicgen.py
    python tools/musicgen.py --list
    python tools/musicgen.py --only zone_forest battle_boss --bars 24
"""

from __future__ import annotations

import argparse
import math
import random
import struct
import wave
from dataclasses import dataclass
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


def note_frequency(note: str, octave: int) -> float:
    semitone = NOTE_INDEX[note] - NOTE_INDEX["A"] + (octave - 4) * 12
    return 440.0 * (2 ** (semitone / 12))


def sine(phase: float) -> float:
    return math.sin(TAU * phase)


def square(phase: float) -> float:
    return 1.0 if (phase % 1.0) < 0.5 else -1.0


def triangle(phase: float) -> float:
    return 4.0 * abs((phase % 1.0) - 0.5) - 1.0


WAVES: dict[str, Callable[[float], float]] = {
    "sine": sine,
    "square": square,
    "triangle": triangle,
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
) -> None:
    if not audio:
        return
    start = max(0, int(start_sec * SAMPLE_RATE))
    length = max(1, int(duration_sec * SAMPLE_RATE))
    wave_fn = WAVES[wave_name]
    for local in range(length):
        out_idx = (start + local) % len(audio)
        phase = (local / SAMPLE_RATE) * frequency
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


def mix_drums(audio: list[float], bar_start: float, beat: float, style: str, rng: random.Random) -> None:
    if style == "none":
        return

    for b in range(4):
        t = bar_start + b * beat
        if style == "battle":
            mix_kick(audio, t, 0.34)
            mix_noise(audio, t + beat * 0.5, 0.035, 0.05, rng)
            if b in (1, 3):
                mix_noise(audio, t, 0.07, 0.17, rng)
        elif style == "heavy":
            if b in (0, 2):
                mix_kick(audio, t, 0.25)
            mix_noise(audio, t + beat * 0.5, 0.04, 0.045, rng)
        elif style == "wood":
            if b in (0, 2):
                mix_tone(audio, t, 0.045, 180, 0.08, "triangle", release=0.02)
        elif style == "drip":
            if b in (1, 3):
                mix_tone(audio, t + beat * 0.25, 0.08, 680 + b * 45, 0.045, "sine", release=0.04)
        elif b in (0, 2):
            mix_kick(audio, t, 0.09)


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
                4.15 * beat,
                note_frequency(note, 4),
                0.055,
                preset.pad_wave,
                attack=0.05,
                release=0.1,
            )

        for step, arp_index in enumerate(preset.arp * 2):
            note = chord[arp_index % len(chord)]
            mix_tone(
                audio,
                bar_start + step * 0.5 * beat,
                0.38 * beat,
                note_frequency(note, 4),
                0.045,
                "triangle",
                attack=0.006,
                release=0.04,
            )

        bass_steps = 8 if preset.drum == "battle" else 4
        for step in range(bass_steps):
            start = bar_start + step * (4 / bass_steps) * beat
            mix_tone(
                audio,
                start,
                0.28 * beat,
                note_frequency(root, preset.bass_octave),
                0.12 if preset.drum == "battle" else 0.09,
                "square",
                attack=0.004,
                release=0.04,
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
                0.105,
                preset.lead_wave,
                attack=0.004,
                release=0.035,
            )

        mix_drums(audio, bar_start, beat, preset.drum, rng)

    return normalize(seal_loop_boundary(audio))


def make_manifest(generated: Iterable[tuple[str, Path, TrackPreset, int]]) -> str:
    lines = ["# Music Pack", ""]
    for name, path, preset, bars in generated:
        length = bars * 4 * 60 / preset.tempo
        lines.append(f"- `{path.name}` - {preset.mood}, {preset.tempo} BPM, {length:.1f}s loop")
    lines.append("")
    lines.append("Generated with `python tools/musicgen.py`.")
    return "\n".join(lines)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate procedural RPG music loops.")
    parser.add_argument("--list", action="store_true", help="List available track presets and exit.")
    parser.add_argument("--only", nargs="+", choices=sorted(PRESETS), help="Only generate these presets.")
    parser.add_argument("--bars", type=int, default=DEFAULT_BARS, help=f"Bars per loop. Default: {DEFAULT_BARS}.")
    parser.add_argument("--out", type=Path, default=OUT, help="Output directory. Default: assets/music.")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    if args.list:
        for name, preset in PRESETS.items():
            print(f"{name}: {preset.mood} ({preset.tempo} BPM)")
        return

    if args.bars < 2:
        raise SystemExit("--bars must be at least 2")

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
