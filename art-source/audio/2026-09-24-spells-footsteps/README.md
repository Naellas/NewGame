# Spell and footstep audio, 2026-09-24

Original inputs: deterministic Python synthesis, no recordings or third-party samples.
The complete recipe is `Java/tools/audio/sfxgen.py`, using the existing PCM writer
in `musicgen.py`. Seeds are the logical sample IDs. No external audio service was used.

Regenerate from repository root:

```powershell
python Java/tools/run.py sfxgen
```

Outputs: `Java/assets/sfx/<ID>.wav` (16-bit mono PCM, 22,050 Hz).
This overwrites only the 32 named batch outputs. Use `--output` for a scratch destination.

Review status: integrated; automated signal/format and gameplay timing checks.
Subjective listening and in-game mix acceptance remain pending.
Listen at `Java/tools/reviews/audio/index.html`.

IDs:

- fire_flight
- fire_explosion
- ice_flight
- ice_shatter
- lightning_flight
- lightning_impact
- arcane_flight
- arcane_impact
- foot_grass_1
- foot_grass_2
- foot_grass_3
- foot_dirt_1
- foot_dirt_2
- foot_dirt_3
- foot_stone_1
- foot_stone_2
- foot_stone_3
- foot_wood_1
- foot_wood_2
- foot_wood_3
- foot_sand_1
- foot_sand_2
- foot_sand_3
- foot_snow_1
- foot_snow_2
- foot_snow_3
- foot_water_1
- foot_water_2
- foot_water_3
- foot_cloth_1
- foot_cloth_2
- foot_cloth_3
