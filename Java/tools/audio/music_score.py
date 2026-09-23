"""Deterministic stereo chamber-score renderer used by musicgen.py.

Synthesized instruments, not recordings: requires NumPy only at asset-build time.
Notes and room tails wrap around the loop instead of being faded to silence.
"""
from __future__ import annotations

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[2]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT

import hashlib
from html import escape
import wave
from pathlib import Path

import numpy as np

RATE = 22050
REGIONS = {
    "riverside": ("zone_water", 88, "flute", "harp", "River dances: flowing harp and woodwind"),
    "highwall": ("zone_mountains", 84, "horn", "dulcimer", "Watchfires: low strings, horns and deep drums"),
    "crownlands": ("zone_grasslands", 94, "oboe", "harp", "Old oaths: lyrical woodwind and chamber strings"),
    "belltower": ("zone_marsh", 72, "flute", "bell", "Mist bells: suspended strings and distant chimes"),
    "sanctum": ("zone_desert", 104, "oboe", "lute", "Sun procession: plucked strings and frame drums"),
    "northroad": ("zone_tundra", 80, "horn", "dulcimer", "Cairn song: broad horns and crystalline strings"),
}

# base harmony, tempo, lead, accompaniment, rhythmic identity, original motif, title
ENCOUNTER_SCORES = {
    "battle_standard": ("battle_standard", 138, "horn", "dulcimer", "drive", (0, 4, 3, 1, 2, 5, 4, 1), "Crossed Blades"),
    "battle_pursuit": ("zone_forest", 146, "oboe", "lute", "gallop", (0, 2, 3, 4, 6, 4, 3, 1), "Through Thorn and Steel"),
    "battle_skirmish": ("zone_desert", 132, "flute", "lute", "syncopated", (4, 1, 0, 2, 5, 3, 4, 0), "Dust on the Blade"),
    "battle_depths": ("dungeon_crypt", 124, "cello", "dulcimer", "march", (0, 1, 4, 3, 2, 0, 6, 4), "Below the Last Torch"),
    "battle_elite_hunt": ("zone_mountains", 148, "cello", "lute", "gallop", (0, 4, 6, 5, 3, 4, 1, 2), "The Hunter Closes In"),
    "battle_elite_iron": ("battle_standard", 128, "horn", "dulcimer", "march", (0, 0, 4, 5, 3, 1, 2, 4), "Iron Resolve"),
    "battle_elite_arcane": ("zone_marsh", 140, "oboe", "bell", "syncopated", (0, 1, 4, 6, 5, 2, 3, 1), "A Dangerous Art"),
    "battle_boss": ("battle_boss", 156, "horn", "dulcimer", "drive", (0, 4, 6, 5, 4, 1, 3, 0), "Oathbreaker"),
    "battle_boss_requiem": ("dungeon_crypt", 126, "organ", "bell", "march", (0, 6, 5, 3, 4, 1, 2, 0), "Crown of Ash"),
    "battle_boss_tempest": ("zone_mountains", 164, "horn", "lute", "gallop", (0, 2, 4, 6, 5, 4, 1, 3), "Against the Storm"),
    "battle_boss_eclipse": ("zone_marsh", 144, "choir", "dulcimer", "syncopated", (4, 5, 6, 3, 1, 0, 2, 4), "The Sun Goes Dark"),
    "dungeon_crypt": ("dungeon_crypt", 68, "choir", "bell", "hush", (0, 1, 4, 2, 0, 6, 3, 1), "Names Beneath the Stone"),
    "dungeon_crypt_depths": ("zone_marsh", 76, "organ", "bell", "pulse", (0, 4, 1, 0, 6, 5, 3, 1), "The Ossuary Answers"),
    "dungeon_cave": ("zone_forest", 72, "flute", "harp", "hush", (4, 2, 0, 3, 2, 5, 4, 0), "Mineral Light"),
    "dungeon_cave_depths": ("zone_mountains", 80, "cello", "dulcimer", "pulse", (0, 1, 2, 4, 3, 1, 0, 6), "Under the Mountain's Heart"),
    "dungeon_castle": ("zone_tundra", 78, "organ", "harp", "hush", (0, 2, 4, 3, 5, 4, 1, 0), "An Empty Throne"),
    "dungeon_castle_depths": ("battle_boss", 86, "horn", "bell", "march", (0, 4, 3, 6, 5, 1, 2, 0), "Banners of the Fallen"),
    "dungeon_prison": ("zone_marsh", 74, "cello", "dulcimer", "pulse", (0, 1, 0, 4, 3, 1, 6, 5), "Behind Iron Doors"),
    "dungeon_prison_depths": ("dungeon_crypt", 84, "choir", "bell", "syncopated", (0, 6, 4, 1, 2, 3, 1, 0), "The Warden's Footsteps"),
    "dungeon_bandit": ("zone_desert", 92, "oboe", "lute", "syncopated", (0, 2, 1, 4, 3, 5, 2, 0), "Embers in the Hideout"),
    "dungeon_bandit_depths": ("zone_badlands", 104, "cello", "lute", "march", (0, 4, 2, 1, 5, 3, 4, 0), "The Captain's Watch"),
}


def add_encounter_presets(presets, replace):
    originals = dict(presets)
    for name, (base, tempo, lead, pluck, rhythm, motif, title) in ENCOUNTER_SCORES.items():
        source = originals[base]
        # A contrasting middle progression gives each new motif its own harmonic arc.
        chords = source.progression
        progression = chords + (chords[2], chords[0], chords[3], chords[1])
        presets[name] = replace(source, tempo=tempo, lead=motif, progression=progression,
                                mood=f"{title} â€” {lead}, {pluck}, {rhythm}")


def add_regional_presets(presets, replace):
    for region, (base, tempo, lead, pluck, mood) in REGIONS.items():
        presets["region_" + region] = replace(presets[base], tempo=tempo, mood=mood)
        presets["town_" + region] = replace(presets[base], tempo=tempo - 8,
                                            mood=mood + " (chamber town arrangement)")


class Score:
    def __init__(self, seconds, seed):
        self.audio = np.zeros((round(seconds * RATE), 2), dtype=np.float32)
        self.rng = np.random.default_rng(seed)

    def mix(self, signal, start, gain, pan):
        signal = signal * gain
        stereo = signal[:, None] * np.array([np.cos(pan * np.pi / 2),
                                             np.sin(pan * np.pi / 2)])
        offset = round(start * RATE) % len(self.audio)
        # Split wrapped notes without allocating a full-track index array.
        for begin in range(0, len(signal), len(self.audio)):
            part = stereo[begin:begin + len(self.audio)]
            first = min(len(part), len(self.audio) - offset)
            self.audio[offset:offset + first] += part[:first]
            self.audio[:len(part) - first] += part[first:]

    def note(self, midi, start, duration, instrument, gain, pan=0.5):
        t = np.arange(max(2, round((duration + 0.35) * RATE))) / RATE
        freq = 440 * 2 ** ((midi - 69) / 12)
        phase = 2 * np.pi * freq * t
        vibrato = 0.025 * np.sin(2 * np.pi * 5.1 * t) * np.minimum(t / 0.3, 1)
        p = phase + vibrato
        attack = 0.045
        if instrument == "strings":
            signal = np.zeros_like(t)
            for detune in (-0.004, 0.0, 0.003):
                for harmonic in range(1, 9):
                    if harmonic * freq < RATE * 0.44:
                        signal += np.sin(harmonic * (p + phase * detune) + harmonic * 0.3) / harmonic ** 1.65 / 3
            attack = 0.24
        elif instrument in ("harp", "lute", "dulcimer", "bell"):
            signal = np.zeros_like(t)
            partials = (1, 2.01, 2.76, 4.07) if instrument == "bell" else (1, 2, 3, 4, 5)
            decay = {"harp": 1.8, "lute": 3.2, "dulcimer": 2.3, "bell": 0.9}[instrument]
            for h in partials:
                if freq * h < RATE * 0.44:
                    signal += np.sin(phase * h) * np.exp(-t * decay * (1 + h * 0.22)) / h ** 1.3
            attack = 0.006
        elif instrument == "cello":
            signal = sum(np.sin(h * p + .15 * h) / h ** 1.35 for h in range(1, 7))
            attack = .09
        elif instrument == "organ":
            signal = np.sin(p) + .48 * np.sin(2 * p) + .23 * np.sin(3 * p) + .16 * np.sin(4 * p)
            attack = .06
        elif instrument == "choir":
            signal = np.zeros_like(t)
            for detune in (-.003, .002):
                for h in range(1, 9):
                    weight = (np.exp(-((h * freq - 750) / 400) ** 2) + .3) / h
                    signal += weight * np.sin(h * (p + phase * detune)) * .5
            attack = .3
        elif instrument == "horn":
            signal = (np.sin(p) + 0.42 * np.sin(2 * p) + 0.23 * np.sin(3 * p)
                      + 0.10 * np.sin(4 * p)) * (0.8 + 0.2 * np.sin(np.pi * np.minimum(t / duration, 1)))
            attack = 0.12
        elif instrument == "oboe":
            signal = np.sin(p) + 0.3 * np.sin(3 * p) + 0.12 * np.sin(5 * p)
            attack = 0.065
        elif instrument == "bass":
            signal = (np.sin(p) + 0.24 * np.sin(2 * p)) * np.exp(-t * 0.5)
        else:
            breath = self.rng.normal(0, 0.025, len(t))
            signal = np.sin(p) + 0.12 * np.sin(2 * p) + breath
            attack = 0.085
        env = (1 - np.exp(-t / attack)) * np.clip((duration + 0.35 - t) / 0.35, 0, 1)
        self.mix(signal * env, start, gain, pan)

    def drum(self, start, gain, bright=False):
        t = np.arange(round(RATE * (0.7 if bright else 1.1))) / RATE
        noise = self.rng.normal(0, 1, len(t))
        if bright:
            signal = np.diff(noise, prepend=0) * np.exp(-t * 13) * 0.24
        else:
            signal = np.sin(2 * np.pi * (58 * t + 3.5 * (1 - np.exp(-t * 24)))) * np.exp(-t * 6)
            signal += noise * np.exp(-t * 60) * 0.13
        self.mix(signal * np.minimum(t * 1000, 1), start, gain, 0.55 if bright else 0.45)

    def finish(self):
        dry = self.audio.copy()
        # Circular early reflections preserve room tails at the loop seam.
        for seconds, gain in ((0.071, .14), (0.113, .11), (.193, .09), (.307, .065), (.449, .04)):
            self.audio += np.roll(dry[:, ::-1], round(seconds * RATE), axis=0) * gain
        peak = float(np.max(np.abs(self.audio)))
        rms = float(np.sqrt(np.mean(self.audio ** 2)))
        self.audio *= min(0.88 / max(peak, 1e-9), 0.15 / max(rms, 1e-9))
        return self.audio


def render_score(name, preset, bars, note_index, scales):
    seed = int.from_bytes(hashlib.sha256(name.encode()).digest()[:8], "little")
    beat = 60 / preset.tempo
    score = Score(bars * 4 * beat, seed)
    region = name.removeprefix("region_").removeprefix("town_")
    palette = REGIONS.get(region)
    lead, pluck = (palette[2:4] if palette else ("flute", "harp"))
    battle = name.startswith("battle")
    boss = name.startswith("battle_boss")
    elite = name.startswith("battle_elite")
    town = name.startswith("town")
    dark = name.startswith("dungeon")
    ambient = name.endswith("ambient")
    if battle:
        lead, pluck = "horn", "dulcimer"
    if dark:
        lead, pluck = "oboe", "bell"
    encounter = ENCOUNTER_SCORES.get(name)
    groove = "drive"
    if encounter:
        lead, pluck, groove = encounter[2:5]
    scale = [note_index[n] for n in scales[preset.scale]]
    # Keep modal degrees ascending even for scales whose tonic is not C.
    scale = [pc + (12 if pc < scale[0] else 0) for pc in scale]
    for bar in range(bars):
        section = min(3, bar * 4 // bars)  # statement, development, release, culmination
        energy = ((.83, 1.0, .7, 1.15) if battle else (.62, .88, .48, 1.0))[section]
        if town or ambient:
            energy *= .72
        if dark:
            energy *= .7 if groove == "hush" else .86
        chord_index = (bar // 2) % len(preset.progression)
        if section == 2:
            chord_index = (chord_index + 2) % len(preset.progression)
        chord = preset.progression[chord_index]
        tones = [48 + note_index[n] for n in chord]
        for i in range(1, len(tones)):
            while tones[i] <= tones[i - 1]:
                tones[i] += 12
        start = bar * 4 * beat
        # Inversions, upper extensions, and a sustained bass replace block triads.
        voicing = [tones[0] - 12, tones[1], tones[2], tones[0] + 14]
        for i, midi in enumerate(voicing):
            score.note(midi, start + i * .018, 4.05 * beat, "strings", .041 * energy, .15 + i * .22)
        for step in range(2 if not battle else 4):
            score.note(tones[0] - 12 + (7 if step % 2 else 0), start + step * (1 if battle else 2) * beat,
                       1.3 * beat, "bass", .10 * energy, .48)
        pattern = (0, 2, 1, 3, 2, 1, 0, 2) if region != "sanctum" else (0, 1, 2, 1, 3, 2, 1, 2)
        if encounter:
            pattern = {"gallop": (0, 0, 2, 1, 0, 2, 3, 2), "march": (0, 2, 0, 1, 2, 3, 2, 1),
                       "syncopated": (0, 3, 1, 2, 0, 2, 1, 3)}.get(groove, pattern)
        for step, degree in enumerate(pattern):
            if section == 2 and step % 2 or ambient and step % 3:
                continue
            if dark and step % (4 if groove == "hush" else 2):
                continue
            midi = tones[degree % 3] + (12 if degree == 3 else 0)
            score.note(midi, start + step * .5 * beat + score.rng.uniform(0, .012),
                       .7 * beat, pluck, .063 * energy * score.rng.uniform(.85, 1.1), .28)
        # Four-bar question/answer phrases with rests and longer cadences.
        rhythm = ((0, 1.4), (1.5, .45), (2.5, 1.1)) if bar % 4 < 2 else ((.5, .9), (2, 1.8))
        if section == 2:
            rhythm = ((1, 2.6),) if bar % 2 == 0 else ()
        elif battle and encounter:
            rhythm = {"gallop": ((0, .65), (.75, .2), (1.5, .4), (2, .7), (3, .8)),
                      "march": ((0, .8), (1, .8), (2, 1.7)),
                      "syncopated": ((.5, .65), (1.5, .9), (3, .7)),
                      "drive": ((0, .9), (1, .4), (2, .4), (2.5, 1.2))}[groove]
        if dark:
            rhythm = ((.5, 2.8),) if bar % 2 == 0 else ((2, 1.6),)
        for step, (offset, length) in enumerate(rhythm):
            degree = preset.lead[(bar % 8 * 2 + step) % len(preset.lead)]
            if section == 1:
                degree += 2
            midi = 60 + scale[degree % 7] + 12 * (degree // 7)
            if bar % 4 == 3:
                midi = tones[0 if section != 1 else 2] + 12
            if lead in ("cello", "organ", "choir"):
                midi -= 12
            score.note(midi, start + offset * beat, length * beat, lead, .09 * energy, .62)
        if section in (1, 3) and bar % 2:
            for step in range(2):
                score.note(tones[2 - step] + 12, start + (step * 2 + .5) * beat,
                           1.6 * beat, "flute" if lead != "flute" else "oboe", .035 * energy, .78)
        if not ambient and groove != "hush" and (section != 2 or battle) and (not town or bar % 2 == 0):
            hits = {"gallop": (0, .75, 1.5, 2, 2.75, 3.5), "march": (0, 1, 2, 3),
                    "syncopated": (0, 1.5, 2.75), "pulse": (0, 2)}.get(groove, (0, 1.5, 2, 3.5) if battle else (0, 2.5))
            for offset in hits:
                score.drum(start + offset * beat, (.19 if battle else .085) * energy)
            for offset in (1, 3):
                score.drum(start + offset * beat, .08 * energy, True)
            if bar % 8 == 7:
                for offset in (3, 3.25, 3.5, 3.75):
                    score.drum(start + offset * beat, .08 * energy, bright=not boss)
        if boss or section == 3 and bar % 4 == 0:
            score.note(tones[0] + 24, start, 2 * beat, "bell", .045 * energy, .8)
        if elite or boss:
            # Bowed rhythmic pedal under the melody; boss choir broadens the later sections.
            for step in range(8):
                score.note(tones[(step // 2) % 3] - 12, start + step * .5 * beat,
                           .24 * beat, "cello", .044 * energy, .18)
            if boss and section != 2:
                for midi in tones:
                    score.note(midi, start, 3.7 * beat, "choir", .027 * energy, .72)
    return score.finish()


def write_score(path: Path, audio):
    path.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(path), "wb") as stream:
        stream.setnchannels(2)
        stream.setsampwidth(2)
        stream.setframerate(RATE)
        stream.writeframes((np.clip(audio, -1, 1) * 32767).astype("<i2").tobytes())


def write_encounter_player(directory: Path):
    """A local listening page; audio is loaded only when the user plays a cue."""
    cards = []
    for name, (_, tempo, lead, pluck, groove, _, title) in ENCOUNTER_SCORES.items():
        if not (directory / (name + ".wav")).is_file():
            continue
        category = ("Dungeon" if name.startswith("dungeon") else "Boss" if name.startswith("battle_boss")
                    else "Elite" if name.startswith("battle_elite") else "Regular combat")
        cards.append(f'<article data-category="{category}"><small>{category}</small>'
                     f'<h2>{escape(title)}</h2><p>{tempo} BPM Â· {lead} Â· {pluck} Â· {groove}</p>'
                     f'<audio controls loop preload="none" aria-label="{escape(title, quote=True)}" '
                     f'src="{name}.wav"></audio></article>')
    page = '''<!doctype html><html lang="en"><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Alderfall â€” combat and dungeon music</title>
<style>
body{background:#111820;color:#e8e3d6;font:16px/1.5 system-ui;margin:0;padding:32px;max-width:1200px;margin:auto}
h1{font-size:32px}h2{font-size:20px;margin:8px 0}p{color:#bdc6cc}small{color:#e4ba78}
main{display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:16px;margin-top:24px}
article{background:#1d2935;padding:20px;border-radius:12px}audio{width:100%}
select{font:inherit;background:#1d2935;color:#fff;padding:8px;border:1px solid #8797a6;border-radius:6px}
[hidden]{display:none}
</style><h1>Combat &amp; dungeon scores</h1>
<p>Original synthesized arrangements for Alderfall. Each piece develops across four sections.
Use the playback controls to explore the full arrangement; tracks loop so you can check the seam.</p>
<label>Listen to <select id="filter"><option>All tracks</option><option>Regular combat</option>
<option>Elite</option><option>Boss</option><option>Dungeon</option></select></label><main>
''' + "\n".join(cards) + '''</main><script>
document.addEventListener('play',e=>{if(e.target.tagName==='AUDIO')
document.querySelectorAll('audio').forEach(a=>{if(a!==e.target)a.pause()})},true);
document.getElementById('filter').addEventListener('change',e=>{
document.querySelectorAll('article').forEach(card=>{
card.hidden=e.target.value!=='All tracks'&&card.dataset.category!==e.target.value;
if(card.hidden)card.querySelector('audio').pause();});});
</script></html>'''
    (directory / "encounter-music.html").write_text(page, encoding="utf-8")
