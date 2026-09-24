# Modular interior materials, 2026-09-24

Original inputs are the user's four 1254 x 1254 uploads, copied byte-for-byte:

| Preserved original | Uploaded filename |
| --- | --- |
| `timber.png` | `8c721c17-a823-4fc8-b296-ec3ee914ddb7.png` |
| `paneled.png` | `67e9b5d9-ce2d-4a18-b20d-a8aa85d63839.png` |
| `eastern.png` | `7dfa3ba3-34a3-45d4-b13d-8fb50df9541c.png` |
| `stone.png` | `cef887f2-8530-4b85-8840-dbff5dbd350d.png` |

[recipe.json](recipe.json) records SHA-256 hashes, exclusive crop rectangles,
rotation, output size and every exact runtime path. Architectural crops stay
inside opaque fields. The curtained window removes only border-connected navy
atlas background; the leaded window has a one-pixel transparent canvas margin.
The vertical cap is a rotated horizontal trim, so both orientations share a
material. Floor crops exclude atlas separators; runtime mirrored repetition
matches their edge pixels. Two window cutouts extend the architectural set.

Run from repository root (Python and Pillow required):

```powershell
python Java/tools/run.py extract_interior_seamless_tiles --recipe ../art-source/interiors/2026-09-24-modular-materials/recipe.json
```

This overwrites the 26 named outputs under
`Java/assets/environments/interiors/modular/`. Each material has IDs
`interior_module_<material>_floor`, `_face`, `_cap`, `_side`, `_base`, `_post`.
The window IDs are `interior_wall_window_leaded` and `interior_wall_window_curtained`.
They are placeable wall decorations; generated small windows use the leaded frame,
and Archive wide windows use the curtained frame. Window crops and in-game mounts
have been visually inspected; player acceptance remains pending.
Legacy runtime images are retained for existing named consumers and restoration.
Use `--output-root temp/modular-interiors/reimport` for a scratch import.

Review status: architectural crops inspected against all originals; timber walls
and floors reviewed in actual blacksmith renderer captures; all four material
sets pass presence and seam checks. Player acceptance is pending. The runtime
uses timber by default, paneled for Archive interiors and stone for cistern
houses. An editor map ID containing `interior_eastern` or `interior_stone` selects
that profile for architecture review; no existing region is reassigned to the
eastern style. Source crops and runtime assembly can be tuned independently.

Regression command from `Java/`:
`python -m unittest tools.tests.test_interior_architecture_import`.
Java renderer checks: `com.alderfall.game.ConnectedInteriorWallsTest` using the
documented development build switches. Full-room captures use
`com.alderfall.game.InteriorDesignPreview`.
