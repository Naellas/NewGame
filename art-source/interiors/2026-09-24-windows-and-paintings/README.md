# Windows and paintings — 2026-09-24

Generated with the built-in imagegen tool for the supplied interior art direction.
The four PNGs here are preserved original outputs, with generated alpha intact.
`prompts.md` records the art briefs and final transparency edit prompt.
`recipe.json` records SHA-256 inputs, explicit crops and runtime output paths.

## Import

From the repository root:

```powershell
python Java/tools/run.py extract_interior_seamless_tiles --recipe ../art-source/interiors/2026-09-24-windows-and-paintings/recipe.json
```

Uses Python/Pillow and the existing hashed crop importer. It overwrites only the
four declared runtime PNGs in `Java/assets/environments/interiors/props/wall-decor/`.
Crops enclose alpha >= 16 with two runtime pixels of transparent padding;
generated alpha inside the crops is preserved. No matte or painted background is
added. The Gothic and river originals are the final imagegen transparency edits.

| Runtime ID | Use |
| --- | --- |
| `interior_wall_window_oak_segment` | Segmented casement; identical touching modules join horizontally |
| `interior_wall_window_gothic` | Individual stone tracery and stained-glass window |
| `interior_wall_painting_river` | Two-cell lantern bridge and willow landscape |
| `interior_wall_painting_harvest` | One-cell hearth, seed bowl and grain still life |

Review: inspected through the game renderer in the wall-art, home and blacksmith
samples. Joining follows furniture adjacency and preserves outer frames; moved,
misaligned or removed modules restore exposed ends. Gothic windows stay separate.
Artistic acceptance remains for the user. Review evidence:
[flooring and decor](../../../asset-review/reviews/interiors/2026-09-24-flooring-and-decor/README.md).
