# Connected trade interiors — 2026-09-24

Original art generated with the built-in Imagegen tool from the exact prompts in
`manifest.json`, `smith-manifest.json`, and `accents-manifest.json`. No external
reference art was used for the original generation. The smith transparency edit
uses this batch's `interior_smith_storage.png` as its input; its exact prompt and
accepted output are in `alpha-correction.json`. The original with faint alpha at
the image corner is retained here for provenance, not used at runtime.
`pillar-correction.json` records the final pillar cutout. Its background alpha was
checked directly: colored RGB outside the silhouette has zero alpha and is invisible.

Accepted outputs: five storage fronts, five worktops, a forge hearth, a library
lectern and an oak support pillar. Runtime IDs and destinations are recorded as
`asset_file` in the manifests. All runtime originals belong in
`Java/assets/environments/interiors/props/trades/`. Prompts and originals stay here.

Reimport from repository root, copying PNG bytes and retaining alpha:

```powershell
$batch = 'art-source/trade-interiors/2026-09-24-connected-workrooms'
foreach ($manifest in @('manifest.json', 'smith-manifest.json', 'accents-manifest.json')) {
    foreach ($entry in (Get-Content "$batch/$manifest" -Raw | ConvertFrom-Json)) {
        Copy-Item -LiteralPath "$batch/$($entry.source)" -Destination $entry.asset_file
    }
}
$entry = Get-Content "$batch/alpha-correction.json" -Raw | ConvertFrom-Json
Copy-Item -LiteralPath "$batch/$($entry.source)" -Destination $entry.asset_file
$entry = Get-Content "$batch/pillar-correction.json" -Raw | ConvertFrom-Json
Copy-Item -LiteralPath "$batch/$($entry.source)" -Destination $entry.asset_file
```

Connection variants are selected and cached at runtime by ConnectedFurniture.
Side-by-side matching bays trim their inner posts, share the footprint boundary,
and retain only exposed end caps. Hearth, lectern and pillars remain standalone.
No generated sheets or source directories were added under runtime assets.

Review: straight frontal perspective, readable contents at game scale, transparent
backgrounds, continuous tops/bases across joined fronts and worktops. Game-render
captures and reproduction instructions are in the
[trade-interior review](../../../asset-review/reviews/trade-interiors/2026-09-24-workrooms/README.md).
