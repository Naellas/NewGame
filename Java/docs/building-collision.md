# Building ground footprints

The seven towns' cardinal house variants use direction-aware solid bases with clear
side/rear thresholds. CityBuilding.outside() supplies the approach; the movement
and interaction paths enter from that side. CardinalBuildingTest verifies these
twenty-one variants, including continuous movement and returning from interiors.

Buildings reserve their original lots for placement, ownership, and interior links. Movement collision uses smaller bases near the front of those lots instead. Roofs and lot padding leave room for characters to walk behind the art and use the scenery cutaway.

`BuildingGeometry` shares standalone sprite sizing with the renderer. Its ground profiles use inset widths and shallow depths (at most 1.45 tiles; towers at most 1.10). Door positions remain unchanged. `WorldMap` caches these footprints for grid navigation; `PropCollision` checks the actual rectangles and swept player radius for continuous movement. Terrain, props, and building bases are checked separately so a partly occupied tile does not become an oversized obstacle.

Building hover and click detection use the bases too, so roof space accepts movement clicks. The blue hover rectangle shows the base, rather than the full roof and reserved lot. The bounds follow camera movement and zoom.

Validation: `BuildingCollisionTest` checks 77 authored buildings, solid bases, rear/side clearance, swept collision, continuous movement, and door entry. `SubtileCollisionTest`, `TownNpcNavigationTest`, and `SmokeTest` cover existing movement and interactions. Actual GamePanel hover checks passed at 75%, 100%, and 150% zoom; the capture is in `asset-review/building-hitboxes/preview.png` at the repository root.
