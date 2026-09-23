# Modular dungeon assets

Twenty small transparent sprites extend the existing orthogonal, tile-based scenery. The built-in image generation tool used `assets/source/imagegen-dungeon-overworld-support-props-orthogonal-alpha.png` as a visual reference. Exact prompts, source paths, and ordered sprite names are recorded in `assets/source/modular-dungeons/prompts.json`.

Runtime PNGs: `assets/deco/dungeon_details/dungeon_detail_<name>.png`. Each is a 128×128 RGBA master, bottom-centered with padding. Dungeon placement uses 42-pixel props; overworld dressing uses the existing 42–49-pixel sizing. Existing terrain, entrances, camp shelters, directional perimeter fences, and room layouts provide the structure.

| Theme | Four new pieces |
| --- | --- |
| Cave | root arch, mossy rocks, stalagmites, mineral cluster |
| Vampire castle | banner, gargoyle, coffin, candelabrum |
| Crypt | grave marker, urn, ossuary, offering slab |
| Bandits | barricade, supply sacks, weapon rack, bedroll |
| Goblins | spike fence, totem, cookpot, scrap heap |

`OverworldLocationBlueprints` uses themed dressing in existing site slots. `DungeonGenerator` adds room-appropriate details alongside existing functional and folklore props, retaining its route and stair exclusions. Castle details are applied to existing abandoned castles; this asset pass does not introduce a separate playable vampire-castle dungeon type. The root arch is available as an optional small entrance asset; existing entrance landmarks remain active.

Reimport the source sheets with `powershell -NoProfile -ExecutionPolicy Bypass -File tools/import_modular_dungeon_assets.ps1` from `Java`. The importer slices equal quadrants, ignores alpha values at or below 16 when computing bounds, retains the generated alpha in the crop, fits to the standard canvas, and rejects empty or clipped cells.

Validation: full Java compilation, `ModularDungeonAssetsTest` (20 sprites and 180 generated floors), and `OverworldLocationBlueprintTest` passed. `ModularDungeonPreview` captured exterior and interior views for all five themes using the actual renderer. Review `../../asset-review/modular-dungeons/index.html` for the gallery and ten full-size captures.
