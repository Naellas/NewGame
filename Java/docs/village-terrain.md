# Village terrain coherence

Village outdoor maps now use the same world-space coverage and cached terrain compositor as the overworld. Ground, road edges, snow, marsh, and water blend across tile boundaries. Village borders clamp their material samples to the map edge, avoiding accidental mountain fringes. Camp ground regions apply only to the overworld.

Village paths use a small material palette: regional packed earth, subdued gravel for courts, and existing timber for wetland walks. Dirt, unmaintained road, packed-road, and packed-court tile codes share one continuous earth material. Desert roads use warm dust colors, northern roads use cool gravel, and grassland roads use earth tones. The old per-tile road overlays, junction fillers, seam strips, and shoreline foam are disabled for villages; real bridge decks retain their renderer.

Generation no longer adds random whole road tiles to roughen a village shoulder. Desert village paths no longer switch material along an arbitrary coordinate cross, and the civic clearing follows the village hub. Natural decoration uses stable within-tile offsets and size variation. Village building footprints and the rear roof strip are cleared of incidental scenery, while landmarks, transitions, and fences are preserved.

Submerged sand now uses the water material with a restrained tint instead of a baked circular sand stamp. Beach sand has a quieter palette. Three retired textures were moved, unmodified, to `asset-review/2026-09-22_05-13-30-village-terrain/` at the workspace root. That directory includes reasons, replacements, original paths, and SHA-256 hashes. Runtime fallback mappings and the old extraction/generation scripts were updated so the archived assets are not required or silently recreated.

Validation from `Java/`:

```powershell
java '-Djava.awt.headless=true' -cp out com.alderfall.game.VillageTerrainTest
java '-Djava.awt.headless=true' -cp out com.alderfall.game.RegionalSettlementTest
java '-Djava.awt.headless=true' -cp out com.alderfall.game.VillageTerrainPreview
```

The terrain test checks four regional villages at two zoom levels, exact direct/cached rendering agreement, unchanged gameplay tiles during rendering, consistent earth materials, shallow-water masks, asset fallbacks, and building clearance. The regional suite checks commons, inhabitants, access, deterministic generation, and saved-position recovery for three seeds. Actual game previews are in `exports/village-terrain/`.
