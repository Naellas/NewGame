# Asset Tools

Run tools from the `Java` project root:

```powershell
python tools/regenerate_universal_assets.py --verify
```

`regenerate_universal_assets.py` is the preferred entrypoint for sheet cutouts. It routes sprites, props, items, monsters, NPCs, class models, directional sprites, and walk strips through `universal_cutout.py`, then can run the proof report.

Use individual extraction scripts only when iterating on one source sheet's grid, names, or sizing. New chroma/background removal behavior belongs in `universal_cutout.py`, not in sheet-specific scripts.

Some extraction and fidelity-refresh scripts require Pillow. The Java runtime does not use these scripts.
