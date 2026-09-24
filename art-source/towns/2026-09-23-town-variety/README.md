# Town variety, 2026-09-23
70 original transparent building sprites generated individually with the built-in image_gen tool.
Together with the preceding town-architecture batch, each of the seven towns has 12 unique sprites.

Original inputs: exact text prompts in manifest.json; no reference image inputs.
Original output PNG bytes are retained in originals/. The manifest records runtime IDs,
region destinations, dimensions and SHA-256 checksums. No post-generation bitmap edits.

Import from the repository root:
~~~powershell
powershell -NoProfile -ExecutionPolicy Bypass -File art-source/towns/2026-09-23-town-variety/import.ps1
~~~
Requires PowerShell only. Copies originals byte-for-byte; verifies existing identical
assets and refuses to overwrite different content. Source files remain untouched.

Review: production candidate, visually inspected in generation output and game captures.
See Java/tools/reviews/town-architecture/index.html for all 84 sprites and game views.
TownBuildingArt binds authored lots to roles and controls visual sizing. Gameplay lot
keys, doors, collision footprints, interiors and save identifiers are retained.
TownArchitectureTest checks all twelve IDs per town across seeds 0, 42 and 2026.
