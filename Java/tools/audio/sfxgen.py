"""Deterministic, original spell and foot-contact synthesis (standard library only)."""
import argparse
import math
import random
from pathlib import Path

from tools.audio.musicgen import SAMPLE_RATE, write_wav
from tools.project_paths import JAVA_ROOT

SURFACES = ("grass", "dirt", "stone", "wood", "sand", "snow", "water", "cloth")
SPELLS = ("fire_flight", "fire_explosion", "ice_flight", "ice_shatter",
          "lightning_flight", "lightning_impact", "arcane_flight", "arcane_impact")
FOOT_VARIANTS = 8
IMPACT_FAMILIES = ("blade", "blunt", "arrow", "shadow", "poison", "root", "holy",
                   "heal", "ward", "water", "wind", "bone", "bite", "claw", "heavy")
RELEASES = ("blade_swing", "blunt_swing", "arrow_release", "monster_growl",
            "monster_roar", "monster_hiss", "monster_rattle")
FLIGHTS = ("shadow", "poison", "root", "holy", "water", "wind")


def sample_names():
    return (list(SPELLS) + [f"{f}_flight" for f in FLIGHTS]
            + [f"{f}_impact_{v}" for f in IMPACT_FAMILIES for v in range(1, 4)]
            + [f"{f}_{v}" for f in RELEASES for v in range(1, 4)]
            + [f"foot_{s}_{v}" for s in SURFACES for v in range(1, FOOT_VARIANTS + 1)])


def expanded_sample(name, rng):
    """Distinct material/creature recipes, with decorrelated pitch and transient timing."""
    family = name.split("_")[0]
    flight = name.endswith("flight")
    vocal = family == "monster"
    swing = "swing" in name or "release" in name
    duration = .65 if flight else rng.uniform(.45, .7) if vocal else .23 if swing else .55
    pitch = rng.uniform(.86, 1.15)
    low = band = phase = 0.0
    samples = []
    for i in range(int(SAMPLE_RATE * duration)):
        t = i / SAMPLE_RATE
        noise = rng.uniform(-1, 1)
        low += .045 * (noise - low)
        band += .3 * (noise - band)
        grain = band - low
        fade = min(1, t / .012, (duration - t) / .065)
        env = fade * (1 if flight else math.exp(-t * (2.8 if vocal else 6)))
        tone = lambda hz: math.sin(2 * math.pi * hz * pitch * t)
        if vocal:
            if "hiss" in name:
                value = grain * (.6 + .25 * math.sin(t * 45))
            elif "rattle" in name:
                pulse = max(0, math.sin(t * 92)) ** 10
                value = pulse * (.45 * grain + .12 * tone(960) + .12 * tone(1480))
            else:
                frequency = 58 if "roar" in name else 103
                phase += 2 * math.pi * frequency * pitch * (1 + .1 * math.sin(t * 33)) / SAMPLE_RATE
                value = (.21 * math.sin(phase) + .13 * math.sin(phase * 2) + low * 1.3) * (.6 + .4 * math.sin(t * 46) ** 2)
        elif swing:
            sweep = math.sin(math.pi * t / duration) ** 2
            value = (.6 * grain + .7 * low) * sweep
            if family == "arrow": value += .22 * tone(320 * math.exp(-t * 4)) * math.exp(-t * 25)
        elif family == "blade":
            value = .38 * grain * math.exp(-t * 24) + (.18 * tone(1350) + .1 * tone(2287)) * math.exp(-t * 17) + .25 * low
        elif family in ("blunt", "heavy"):
            phase += 2 * math.pi * (55 + 110 * math.exp(-t * 22)) * pitch / SAMPLE_RATE
            value = .48 * math.sin(phase) * math.exp(-t * 12) + low * 1.3 + .2 * grain * math.exp(-t * 50)
            if family == "heavy": value += .45 * low * math.exp(-((t - .13) / .08) ** 2)
        elif family in ("arrow", "bite", "claw", "bone"):
            pulse = math.exp(-t * 35) + .5 * math.exp(-((t - .045 * pitch) / .01) ** 2)
            value = .55 * grain * pulse + .65 * low + .12 * tone(170) * math.exp(-t * 28)
            if family == "bone": value += (.12 * tone(820) + .1 * tone(1370)) * math.exp(-t * 24)
            if family == "claw": value += .4 * grain * math.exp(-((t - .11) / .05) ** 2)
            if family == "bite": value += .2 * tone(240 + 120 * math.sin(t * 13)) * math.exp(-t * 22)
        elif family in ("heal", "holy", "ward"):
            base = {"heal": 523, "holy": 659, "ward": 220}[family]
            value = sum(.13 * tone(base * ratio) * math.exp(-t * (2 + j)) for j, ratio in enumerate((1, 1.5, 2)))
            if family == "ward": value += .22 * low + .07 * tone(1137)
        elif family == "shadow":
            phase += 2 * math.pi * (160 - t * 120) * pitch / SAMPLE_RATE
            value = .18 * math.sin(phase) + low * 1.1 + grain * .08 * math.sin(t * 40)
        elif family in ("water", "poison"):
            phase += 2 * math.pi * (550 + 180 * math.sin(t * 27)) * pitch / SAMPLE_RATE
            bubbles = max(0, math.sin(t * 39)) ** 8
            value = .85 * low + .18 * grain + .18 * math.sin(phase) * bubbles
            if family == "poison": value += .3 * grain * max(0, math.sin(t * 135))
        elif family == "root":
            value = low * 1.25 + grain * .4 * max(0, math.sin(t * 89)) ** 8 + .12 * tone(130) * math.exp(-t * 20)
        else:  # wind
            value = low * 1.2 + grain * .16 * (1 + math.sin(t * 18))
        samples.append(value * env * .65)
    return samples


def synthesize(name: str) -> list[float]:
    rng = random.Random(name)
    foot = name.startswith("foot_")
    if not foot and name not in SPELLS:
        return expanded_sample(name, rng)
    flight = name.endswith("flight")
    duration = rng.uniform(.17, .24) if foot else .6 if flight else 1.05
    heel = rng.uniform(72, 108) if foot else 90
    toe_time, toe_width = (rng.uniform(.045, .095), rng.uniform(.025, .045)) if foot else (.075, .035)
    weight, decay = (rng.uniform(.8, 1), rng.uniform(32, 52)) if foot else (1, 40)
    low = 0.0
    soft = 0.0
    phase = 0.0
    samples = []
    material = name.split("_")[1] if foot else ""
    for i in range(int(SAMPLE_RATE * duration)):
        t = i / SAMPLE_RATE
        noise = rng.uniform(-1, 1)
        low += .065 * (noise - low)
        high = noise - low
        soft += .23 * (noise - soft)
        fade = min(1, t / .006, (duration - t) / .04)
        if foot:
            # Heel body followed by a softer toe/scuff; variants change grain and pitch.
            body = math.exp(-t * decay) * math.sin(2 * math.pi * heel * t)
            scuff = math.exp(-((t - toe_time) / toe_width) ** 2)
            high = soft - low  # Roll off the sharp, repetitive heel click.
            if material == "stone":
                value = .5 * body + .45 * high * math.exp(-t * 75) + .1 * low * scuff
            elif material == "wood":
                value = .5 * body + .23 * math.sin(2 * math.pi * 390 * t) * math.exp(-t * 38) + .15 * high * math.exp(-t * 70)
            elif material == "water":
                value = .65 * low * scuff + .18 * high * scuff + .18 * math.sin(2 * math.pi * (780 * t - 950 * t * t)) * math.exp(-t * 30)
            elif material == "cloth":
                value = .27 * body + .2 * low * scuff
            else:
                grain = {"grass": .18, "dirt": .1, "sand": .07, "snow": .28}[material]
                value = .3 * body + .6 * low * scuff + grain * high * scuff
            value *= min(1, t / .014, (duration - t) / .055) * .22 * weight
        else:
            env = fade * (1 if flight else math.exp(-t * 4.5))
            if name.startswith("fire"):
                phase += 2 * math.pi * (95 + 70 * math.exp(-t * 10)) / SAMPLE_RATE
                value = low * 1.9 + high * .1 + math.sin(phase) * (.12 if flight else .5)
                value *= .8 + .2 * math.sin(t * 53)
            elif name.startswith("ice"):
                # Inharmonic shards with staggered, rapidly decaying resonances.
                value = high * (.09 if flight else .24)
                for j, frequency in enumerate((1120, 1783, 2617, 3911)):
                    age = t - j * .035
                    if age >= 0:
                        value += .16 * math.sin(2 * math.pi * frequency * age) * math.exp(-age * (4 if flight else 12))
            elif name.startswith("lightning"):
                value = high * .5 * (1 if math.sin(t * 370) > .6 else .12) + low * .8
                value += .2 * math.sin(2 * math.pi * 60 * t)
            else:
                phase += 2 * math.pi * (280 + 460 * t / duration) / SAMPLE_RATE
                value = .25 * math.sin(phase) + .15 * math.sin(phase * 1.503) + low * .65
            value *= env * .65
        samples.append(value)
    peak = max(abs(v) for v in samples)
    return [v * min(1, .85 / peak) for v in samples]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=JAVA_ROOT / "assets/sfx")
    args = parser.parse_args()
    names = sample_names()
    for name in names:
        write_wav(args.output / f"{name}.wav", synthesize(name))
    print(f"Wrote {len(names)} original PCM samples to {args.output}")


if __name__ == "__main__":
    main()
