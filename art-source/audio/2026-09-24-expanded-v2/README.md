# Expanded audio v2, 2026-09-24

Original inputs: deterministic synthesis in `Java/tools/audio/sfxgen.py`, using
its named sample IDs as random seeds and the existing `musicgen.py` PCM writer.
No recordings, third-party samples, external services or additional dependencies.
`sfxgen-v1.txt` preserves the previous recipe so the replaced footsteps can be
reproduced. The eight original elemental samples retain their original synthesis.

From repository root:

```powershell
python Java/tools/run.py sfxgen
```

Outputs: 144 mono 16-bit, 22,050 Hz WAVs under `Java/assets/sfx`. The exact IDs,
paths and SHA-256 hashes are in `outputs.json`. Regeneration overwrites only those
named outputs. To audition a separate batch, pass `--output temp/audio-v2` (runner-relative paths start in Java).

Changes: 24 original footsteps softened; 40 additional footstep variants; 72 new
combat samples (15 impact families x 3, 7 release/creature families x 3, 6 flight
loops). Footstep transients are low-pass filtered, shorter and lower in level,
with independently varied heel pitch, toe timing, weight and decay. First-variant
RMS comparisons against v1 measured about 10-15 dB quieter by surface.

Review: integrated and automatically validated, with subjective listening pending.
Listen at `Java/tools/reviews/audio/index.html`; gameplay behavior and verification
are documented in `Java/docs/audio-effects.md`.
