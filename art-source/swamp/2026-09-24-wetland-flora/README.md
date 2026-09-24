# Wetland flora, 2026-09-24

Generated with the built-in imagegen tool. Original input: `sedge-original.png` (generated transparent PNG); no reference image was supplied. Exact prompt: [prompt.txt](prompt.txt).

Import from the repository root (Python and Pillow required):

```powershell
python art-source/swamp/2026-09-24-wetland-flora/import.py
```

The recipe reuses `fit_to_canvas` from the existing water-prop importer. It trims transparent margins, fits to a 64px RGBA canvas with a 58px subject, and overwrites `Java/assets/environments/terrain/biomes/marsh/deco_marsh_sedge_clump.png`. Runtime ID: `deco_marsh_sedge_clump`. Original alpha is retained; no background removal or repainting.

Review: original inspected; accepted for integration at 48-68px prop sizes. In-game review shows dark cattails and broad sedge leaves with no opaque rectangular background. Awaiting player aesthetic feedback.

The rejected `quest_supply_cache` artwork was separately archived under `Java/archive/pending-deletion/swamp-supply-art-2026-09-24`; its runtime ID now reuses the detailed `location_camp_crates.png` image. This keeps existing quest lookups valid.
