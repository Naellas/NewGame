# Desert surfaces and shared roads

Overworld and village roads now use the same material compositor, feathered boundaries, earth/gravel grain, and regional palette. Bridge construction is still drawn separately. Sand-biome roads use packed sand, including cobblestone road codes; gameplay tile codes and movement remain unchanged. Location paving stamps are suppressed over these travel surfaces.

Two new full-resolution assets were generated with the built-in imagegen tool (not the CLI) and copied unmodified into `Java/assets/terrain/`:

- `desert_sand_wind.png`
- `desert_path_packed.png`

The renderer samples each across eight tiles in world coordinates, with mirrored wrapping to avoid abrupt image edges. Runtime tint reduces sand ripple contrast and separates compacted paths. Original images remain intact. Existing sand options were either repeated pebble clusters, busy ripple tiles, or bright beach sand. Retired sand variants are kept under a timestamped `asset-review` folder. Existing road textures remain available for city/interior use.

Preview command from Java: `java -Djava.awt.headless=true -cp out com.alderfall.game.VillageTerrainPreview exports/desert-roads`.

Validation: Java 21 compilation; RoadSurfaceTest (four regional palettes, three zooms, large-texture wrap); LayeredTerrainTest (water masks, chunk/scroll equivalence, cache edits); VillageTerrainTest; BridgeRenderingTest. Actual GamePanel previews cover four villages, desert travel, green roads, and shoreline. Isolated decorative ground stamps away from roads retain their existing rendering.

Retired originals: `asset-review/2026-09-22_05-35-57-desert-surfaces/`, with original paths and SHA-256 hashes in `manifest.json`.

## Exact generation prompts

### desert_sand_wind.png

Use case: stylized-concept. Asset type: seamless square ground material for Echoes of Alderfall, a top-down orthogonal detailed pixel-art fantasy RPG. Generate ONE full-bleed texture of quiet, pale muted ochre desert sand, designed as a large continuous patch spanning eight game tiles rather than a tiny repeating tile. Straight overhead orthographic view; uniform diffuse lighting; entirely opaque. Finely stippled sand grains with soft shallow irregular wind ripples and restrained broad natural tonal variation; mostly open calm sand. Warm dusty beige and subdued honey ochre, approximate average #BDA16F, narrow contrast range. Hand-painted pixel clusters, crisp grain at game scale, tasteful low contrast. Seamless left/right and top/bottom edges, no border or vignette. No stones, pebbles, dot grids, footprints, debris, vegetation, roads, dunes with deep shadows, terrain edges, horizon, perspective, words, labels, watermark, panels, tile grid or objects. This is a flat reusable sand albedo texture, not a scene or a map. Square 1024 by 1024 composition.

### desert_path_packed.png

Use case: stylized-concept. Asset type: seamless square compacted desert earth ground material for Echoes of Alderfall, a top-down orthogonal detailed pixel-art fantasy RPG. Generate ONE full-bleed texture of dry compacted sandy earth worn smooth by travel, matching muted warm beige desert sand. Intended as the interior fill material of paths; the engine draws their outlines. Straight overhead orthographic view, uniform diffuse lighting, entirely opaque. Fine sandy grit, tiny irregular shallow compacted patches and restrained broad tonal variation. Warm dusty tan with subtle warm brown, approximate average #A58A60, slightly darker and firmer than sand. Hand-painted pixel clusters, crisp grain at game scale, low contrast and visually quiet. Seamless left/right and top/bottom edges. No explicit road stripe, path borders or geometry, no stones, pebbles, repeated dots, footprints, wheel ruts, brick, paving, vegetation, mud, cracks, deep shadows, vignette, horizon, perspective, words, labels, watermark, panels or grid. Designed as a large continuous patch covering eight game tiles, not one small tile. Square 1024 by 1024 composition.
