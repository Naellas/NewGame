# Generated quest objects ? 2026-09-24

Built-in imagegen produced four individual transparent raster sprites. Original
outputs are preserved here byte-for-byte; manifest.json records generation IDs,
source files, briefs and runtime asset_file paths. Shared direction: detailed
medieval fantasy pixel art, three-quarter overhead view, south-facing front,
upper-left lighting, earthy palette, isolated alpha, readable at 40?64 pixels.
The evidence bundle received an imagegen edit requesting removal of outer haze.
Runtime uses the existing AssetStore alpha crop and cached fit; no offline image
processing or new dependency is needed.

Repeat import from the repository root (overwrites only the four listed assets):

```powershell
$batch = 'art-source/quest-objects/2026-09-24-world-events'
Get-Content "$batch/manifest.json" | ConvertFrom-Json | ForEach-Object {
    Copy-Item -LiteralPath (Join-Path $batch $_.source) -Destination $_.asset_file
}
```

Usage: purse and bush are roaming world objects. Supply cache and evidence bundle
replace presentation of existing quest objectives while retaining logical quest
asset IDs. Review: accepted after in-game captures at 100% zoom. The document image retains
a faint soft edge at source resolution, but no rectangular background is visible
in its game capture. `evidence-input.png` is the original edit input;
`letters-alternative.png` is an unused alternate, not installed at runtime. Exact
prompts are in prompts.json.
