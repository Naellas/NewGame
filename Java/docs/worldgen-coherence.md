# Coherent crossings and woodland groves

Generated outdoor maps also use [geographic elevation](world-elevation.md):
walkable stepped foothills, blocked summit cores, regional banks and protected
roads, crossings and settlement foundations. Existing crossing/grove rules below
remain in effect.

This generation pass follows `Story premise/world-framework.md`, especially sections 3, 5, 7, 8, and the Belltower Fenlands subsection of section 12.

- Roads search for connected ground routes. A river crossing is one straight bank-to-bank step, capped at eight water tiles. Nearby routes favor existing crossings. Wide lakes get land detours; water waypoints move to a bank instead of creating artificial landing islands. Settlement gate foundations remain ground. Mountain passes retain their pass tiles when roads are reused or upgraded near cities.
- Decorative forest trees and harvestable forest nodes use the same seed-dependent grove profile. Smooth patches favor oak, birch, or maple in the central basin and pine in northern regions or near snow. Water margins favor willow with ash support; willows are excluded more than three tiles from water. Among trees, the target weights are 75% dominant family, 20% supporting family, 5% other species, subject to existing rarity gates. Existing non-tree decoration proportions remain intact.
- Western crossings receive a Bridge Courts charter marker when there is free natural ground beside an approach. Fenland crossings receive a flood-warning bell. Each span gets at most one marker, placed off the travel lane and only in its associated region. These are scenery, not new quest interactions.

Generated terrain and props change when the world is reconstructed. Existing save files are not rewritten by this implementation. Shoreline rendering, river hydrology, and larger forest density changes are separate future work.

Bridge rendering uses rectangular plank decks with continuous side rails and bank posts. Each tile clips its own pixels, preventing overlapping round stroke caps from producing repeated arches. Adjacent bridge tiles determine the span direction so a nearby road cannot pull the deck sideways. `com.alderfall.game.BridgeRenderingTest` checks both orientations at four zoom levels for continuous decks, stable seams, and independence from tile draw order.

## Verification

After building from `Java/`, run:

```powershell
java -cp temp/checks/classes com.alderfall.game.map.WorldGenerationTest
java '-Djava.awt.headless=true' -cp out com.alderfall.game.WorldGenerationPreview
```

The generation checks exercise straight crossings, crossing reuse, lake detours, unbridgeable water, five seeds, every generated settlement and adventure destination, bridge landings, repeatability, off-road scenery, shared grove dominance, and willow habitat constraints. The preview checks both new asset files and their alpha channels, then writes actual game renders to `Java/exports/worldgen-preview/`.

The broader `SmokeTest` traversal assertions now check generated adventure-site coordinates instead of obsolete preferred coordinates that may be in water. At validation time, the full suite subsequently stopped at its existing Aria road-sign objective expectation; current `GameData` defines that quest as cut trail ribbons.

## Imagegen assets and provenance

Generated with the built-in imagegen tool, not the CLI. Selected transparent PNGs are copied unmodified into the project; the normal sprite renderer crops and scales their alpha bounds.

- `Java/assets/environments/locations/deco_crossing_charter_marker.png`
- `Java/assets/environments/locations/deco_crossing_flood_bell.png`

### Charter marker prompt

Use case: stylized-concept. Asset type: one isolated transparent PNG scenery sprite for Echoes of Alderfall, a top-down orthogonal 2D fantasy RPG with detailed muted pixel art, trees and objects typically displayed at 48-72 pixels tall. Primary request: a single weathered western bridge-charter marker: a squat pale limestone upright marker with a tiny bronze charter plaque inset, shallow abstract carved lines (no readable text), one small faded burgundy cloth tied to its side, a little moss at its foot. This marks maintained river crossings of the fictional Bridge Courts. Grounded humble medieval craftsmanship, no magic glow. Three-quarter overhead RPG view, front face visible, no isometric diamond base. Restrained olive moss, cool gray stone, muted brown bronze, soft light from upper left, crisp hand-painted pixel clusters. Center the complete object with generous transparent padding. Genuine transparent background with alpha, no ground tile, no scenery, no characters, no border, no words, no watermark. Square image. Readable simple silhouette after downsampling. This is a NEW asset, not a scene or screenshot.

### Flood bell prompt

Use case: stylized-concept. Asset type: one isolated transparent PNG scenery sprite for Echoes of Alderfall, top-down orthogonal 2D fantasy RPG with detailed muted pixel art, objects displayed at 48-72 pixels tall. Primary request: a small Fenland flood-warning bell mounted on a single weathered wooden upright with short crossbeam, a modest aged bronze bell hanging under the crossbeam, a few pale flood-level notches carved low on the post, and a tiny tied reed bundle at its base. A humble maintained waterside instrument used by the fictional Belltower Fenlands' flood keepers. No magic glow, no shrine roof, no writing. Three-quarter overhead RPG view, front face visible, no isometric diamond base. Dark weathered brown timber, dull bronze, olive reeds, soft upper-left lighting, crisp hand-painted pixel clusters, realistic modest medieval proportions. Center complete object with generous transparent padding. Genuine transparent background with alpha, no floor tile, no surrounding scene, no characters, no border, no text, no watermark. Square image, easily readable when small. This is a NEW single prop, not a scene or screenshot.
