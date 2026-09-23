# Flora sprite refresh

Regenerated with the built-in `image_gen` tool to remove magenta background contamination while retaining natural colors, intentional flower colors, and the game's painted pixel-art style.

Replaced 60 runtime PNGs: 24 soft ground props, 28 dense foliage/water props, four matching aliases, and four mushroom/fern files. Mushroom files previously containing logs or clipped rock artwork now contain mushrooms; the fern file now contains a complete fern.

- [Exact prompts](prompts.json), [runtime asset manifest](manifest.tsv), and [edge-color audit](edge-audit.tsv).
- Generated source sheets: [soft props](soft-clean-sheet.png) and [dense foliage/water](dense-clean-sheet.png).
- Standalone sources: [mushrooms](mushrooms-clean.png), [blue mushroom ring](blue-ring-clean.png), and [fern](fern-clean.png).
- Original runtime sprites are preserved under `before/`, retaining their asset-directory paths.
- [Updated in-game previews](../subtile-placement/README.md).

The importer preserves generated alpha, locates transparent gutters before slicing, and crops/resizes sprites for runtime use. It does not chroma-key or recolor the generated art. Run from `Java/`:

```powershell
javac -d out tools/assets/environments/ImportCleanFlora.java
java -cp out ImportCleanFlora
python tools/checks/audit_flora_edges.py
```

Across the 60 replacements, the audit found 34,252 magenta-like boundary pixels before and 771 after. This color metric also counts intentional purple/pink petals; it is not a rule for stripping those colors. Visual review on game terrain confirmed clean transparent backgrounds. The runtime filenames and logical prop sizes remain unchanged.
