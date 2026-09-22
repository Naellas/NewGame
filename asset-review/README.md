# Asset cleanup review

437 candidates are moved into `files/`, preserving their original paths. Nothing is deleted. This directory is outside the game's asset catalog.

- 384 raw city-sheet grid slices with no references in game code, configuration, saved games, or map exports.
- 9 old building cutouts, trial images, and named mountain backups with no such references.
- 44 duplicate images, including 10 old terrain backups. The currently selected runtime image is retained. For the 9 identical companion battle sprites, the canonical per-companion copy is retained instead of the flat copy.

`manifest.json` records each original path, reason, retained replacement where applicable, byte size, and SHA-256 checksum. Source artwork, assets referenced through dynamic naming, and pre-existing modified/untracked files remain in place. These are cleanup candidates, not a judgment that every image is visually defective.

To restore every moved file from the repository root:

```powershell
powershell -ExecutionPolicy Bypass -File .\asset-review\restore.ps1
```

The restore script checks all paths and checksums first and refuses to overwrite existing files.

## Animation finding

616 animation metadata files were scanned. One has a UTF-8 BOM before its frame count: `Java/assets/player/classes/mage/animations/class_mage_model_cast_anim.frames`. Java's integer parser rejects that marker and falls back to frame-count inference. This active animation and its metadata remain in place for now; removing the marker is the appropriate repair. The other 615 strips have valid frame counts, divisible widths, and no fully transparent frames. This does not assess visual motion quality.
