# Biome ground cover and nature spaces

Four generated 4×4 RGBA sheets supply 64 sprites (eight per land biome):

- [Meadow / forest](meadow-forest.png): eight grasses and clovers; four leaf-litter variants, fern clumps/fronds, moss and twigs.
- [Desert / badlands](desert-badlands.png): sand hummocks and ripples, grit, baby cacti, dry grass, scrub, shale and roots.
- [Tundra / highlands](tundra-highlands.png): frost grass, sedge, snow hummocks, lichen, alpine tussocks and scree.
- [Marsh / coast](marsh-coast.png): sedge, rushes, wet moss, mud litter, beach grass, sand, shells, seaweed and driftwood.

Generated using the built-in imagegen tool, one call per sheet, with `deco_soft_grass_tuft.png` as the style reference. Prompt specification: ordered four-by-four grid, isolated low ground-hugging vegetation/debris, muted natural colors, overhead three-quarter painted pixel clusters, transparent gutters at least 45 pixels, true RGBA alpha; no text, grid, background, large trees/rocks or collectible-icon treatment. Each sheet requested the subjects listed above in biome order, eight per biome. This is a record of the specification, not a verbatim tool-call transcript.

Runtime sprites live in `Java/assets/environments/props/nature/ground`. `manifest.tsv` maps each asset to its source cell. Import with `javac -d temp tools/assets/environments/ImportCleanFlora.java tools/assets/environments/ImportNatureSheets.java` then `java -cp temp ImportNatureSheets` from `Java/`. The importer finds empty gutters and trims/resizes with the generated alpha intact; it does not chroma-key or recolor pixels.

Ground detail retains four noninteractive visual slots per gameplay tile and the existing overlapping rounded patches. Meadow baseline occupancy is 40% per slot, forest/marsh 28%, other biomes 16%, plus up to 57 percentage points inside patches. Forest leaf piles are less common than ferns/moss/twigs. Mountain-pass ground uses the highland sheet; impassable mountain peaks and open water receive no grass. Boundary tiles remain sparse to keep terrain readable.

The ordinary larger soft-decoration pass runs at 40% of its previous land probability. Random shrine/rune/cairn/stone-stack/fairy-pool/blue-mushroom-ring/blooming-cactus choices are thinned by 65%. Resource generation and authored landmark placement retain their own rules.

Nature sites are overworld scenery, generated after authored content. Jittered 30-tile cells, rejection tests and a 23-tile minimum spacing keep them sparse. A focal object, low crescent and two companion patches form each composition. Existing authored locations, landmark buffers, terrain boundaries and occupied focal positions reject candidates; existing props/resources are never removed. Only generated ground detail is cleared around the focal point and trail.

A bounded cardinal breadth-first search seeks a reachable road within 16 steps. If none exists, it selects a reachable nine-step hint trail. Each segment is checked against swept player collision, including after placing the focal prop. Paths are visual wear, not gameplay roads: layered narrow dabs fade at the outer end and end beside the focal feature. They do not connect the whole map. Search direction varies by location. These are environmental curiosities, not new quests or loot rewards.

Both paths and small flora render into existing terrain chunks. They add generation/cache-fill work and stored detail records, but no separate per-frame sprite sorting or draw calls after chunks warm up. This change does not claim a measured frame-rate improvement.

Validation: `NatureSpacesTest` checks all 64 runtime assets, seed determinism, spacing, trail bounds, authored-location exclusion and actual collision clearance for two seeds. `GroundDetailBatchTest` includes a turning trail across chunk boundaries and compares direct/cached pixels at three zooms and camera offsets. `SubtileCollisionTest` and `SmokeTest` cover movement and startup. `tools/NatureSpaceReview.java` exports actual in-game scenes to this folder.

Scene previews: [meadow](scene-meadow.png), [forest](scene-forest.png), [desert](scene-desert.png), [badlands](scene-badlands.png), [marsh](scene-marsh.png), [tundra](scene-tundra.png).
