# Player settlement identity and six-stage progression

The user supplied the earlier six-tier building atlas as the progression
reference: `../2026-09-24-six-tiers/buildings.png`. The accepted direction has
pronounced architectural changes at every tier: canvas, timber, thatch/plaster,
tall framing/slate, substantial stone, ornate metropolitan stone/copper.

Generated with the built-in image generator. Exact prompts are the adjacent
`strong-*-prompt.txt` files. `strong-industry.png`, `strong-food.png`,
`strong-civic.png`, `strong-specialists.png` and `strong-provisions.png` contain
the 15 specialist types across six tiers. Cottage, Workshop and Hall retain the
18 reference sprites imported by the previous batch. Together these provide
108 distinct playable building/tier identities.

`repaired-industry.png` and `repaired-provisions.png` are the final imported
versions of those two sheets. The adjacent `repaired-*-prompt.txt` files record
the layout-only edits to provide clear gutters around the tallest buildings.
All 18 silhouettes in each repaired sheet are separate alpha components;
the original strong sheets remain here for provenance.

`industry.png`, `civic.png`, `trades.png` and their prompts are preserved initial
drafts of only the last three tiers. The user rejected their overly subtle
progression; they have never been imported into runtime assets.

From the repository root:

```powershell
python art-source/settlement-growth/2026-09-24-specialist-tiers/import_recipe.py
```

Requires Pillow and reuses the existing `universal_cutout` alpha extraction.
The fixed batch recipe overwrites only its 90 named PNG outputs and `imports.json`.
Recorded row gutters and quiet column divisions avoid cutting wide facades;
connected alpha extraction drops detached neighboring fragments. Output IDs,
source crop rectangles and exact paths are recorded in `imports.json`.

Runtime outputs: `Java/assets/environments/settlements/player_village/buildings/<style>/settlement_<style>_tier<N>.png`.
All are transparent 256x256 canvases with grounded, padded silhouettes.
`relocation.json` records the original and current paths and exact SHA-256
checksums of the 72 existing assets moved into player ownership. No pixels were
changed during relocation. Historical importer aliases and live review links
were updated together with the move.

Review: full atlases and representative imports inspected; distinct IDs and
player-owned paths checked for all 108 playable sprites. The actual game panel
test exercises category filtering, upgrades from both panels, returning to the
town panel, and terrain controls. Current visual comparison:
[building gallery](../../../Java/tools/reviews/player-settlement/index.html).
