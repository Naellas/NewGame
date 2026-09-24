# Generated lake crests, 2026-09-24

Generated with the built-in image_gen tool. Exact generation and refinement
prompts are in `prompt.md`. `initial-sheet.png` is the first, taller-wave study;
`crest-sheet.png` is the selected flatter overhead crest sequence. Both originals
retain generated RGBA data unchanged. Runtime uses only the refined sheet.

Import from the repository root:

```powershell
python Java/tools/run.py import_animation_grid ../art-source/water/2026-09-24-wave-crests/crest-sheet.png effects/weather/water_crest_cycle_anim.png --columns 4 --rows 3 --width 192 --height 64
```

Output: `Java/assets/effects/weather/water_crest_cycle_anim.png`, `.frames`, and
`.framebounds`. Logical animation ID: `water_crest` / action `cycle`, 12 frames.
The importer finds alpha bounds above 2/255 (ignoring invisible stray pixels),
preserves retained alpha, uses one shared scale, and bottom-aligns the poses.
It does not recolor or paint the generated frames. Standard Pillow resampling
packs each pose into a 192x64 canvas with transparent margins.

Review status: source and runtime frames inspected; accepted for in-game trial.
Runtime crossfades adjacent poses and fades lifecycle endpoints rather than
jumping from the dissipated final crest back to the first frame. Existing base
water and depth colors are unchanged. Final art approval remains with the user.

[Animation comparison](../../../asset-review/reviews/water/2026-09-24-generated-crests/README.md).
