# Town architecture
Each of Briarbridge, Ironvale, Moonspire, Reedwatch, Embermarket, Northwatch and
Greyharbor uses twelve distinct town sprites (84 total). The first batch
supplied a signature landmark and house; the variety batch adds ten roles per town.
Each town also uses three cardinal house views. Three shared regional barn variants
bring the current library to 108 sprites.

Runtime PNGs live in assets/environments/settlements/city/buildings/<region>/.
Briarbridge uses river, Ironvale north, Moonspire hearth, Reedwatch fen,
Embermarket sun, and Northwatch/Greyharbor freehold. The latter two have separate
upland and coastal designs despite sharing a region folder.

TownBuildingArt owns town-specific visual routing and sizing.
RegionalSettlementIdentity.townBuildingAsset delegates to it. Complete sprites use
the standalone renderer, preventing repeated miniature houses within a lot.
Ordinary buildings in the seven towns use a visible bounding-box area equivalent
to 180 x 180 pixels at the 48-pixel reference tile size. The seven signature
landmarks use a 300 x 300 equivalent area (two thirds larger in linear scale),
with six-tile frontage reserved during layout. AssetStore measures cached alpha-cropped sprite bounds (alpha
above 8), preserves aspect ratio, and scales the area with zoom. Transparent canvas
padding, source export resolution, lot dimensions and building role cannot change
that apparent size. Tall towers remain tall and narrow; wide houses remain wide.
Unused transparent margins do not count toward the measured size.

Special regional institutions and folklore art retain their dedicated images.
Missing artwork falls back to existing rendering. Artwork stays bottom-anchored to
the entrance. The district layout below relocates town lots; artwork retains its
role-specific apparent size.

Gate placement prefers existing road crossings. The clearance test checks one
tile on either side along the wall and two across it. Gate coordinates follow the
new inset wall ring and street crossings rather than the earlier fixed town grid.
Buildings within the actual passage still reject placement. Settlement diagnostics
verify four continuous-wall gates, passable approaches, reachable entrances and
deterministic regeneration/saved-position recovery across three seeds.

Originals, exact prompts, checksums and safe PowerShell import recipes:
- ../../art-source/towns/2026-09-23-town-architecture/
- ../../art-source/towns/2026-09-23-town-variety/

Review: [interactive gallery](../tools/reviews/town-architecture/index.html).

Validation from Java/:
~~~powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build.ps1 -IncludeTests -OutputDirectory temp/uniform-buildings/classes
java -Djava.awt.headless=true -cp temp/uniform-buildings/classes com.alderfall.game.TownArchitectureTest --render
~~~
TownArchitectureTest verifies twelve base IDs per town plus each town's three
cardinal variants across three seeds,
transparent files, both sprite-selection paths, single-building rendering and
uniform zoom sizing. It also checks alpha padding/export-resolution invariance,
common visible area for institutions, and independence from lot dimensions for
the same selected sprite. The optional render run writes 84 building views and
28 gateway views to asset-review/reviews/town-architecture/2026-09-23-uniform-scale/. Also run RegionalBuildingsTest, RegionalSettlementTest,
BuildingCollisionTest and TownNpcNavigationTest, then scripts/check.ps1.

## Districts, public space and suburbs (2026-09-24)

The seven named towns reserve land before generating roads. TownLayout groups
existing institutions and working buildings into civic, residential, craftsmen's
and market quarters. Scholarly buildings form a cluster in the civic quarter,
with a Scholars' District landmark only where that program exists. Watch and water
courts keep regional government identities; the existing named regional commons,
institutions and residents remain. Capital layouts and villages retain their plans.

Maps are 76 tiles wide, with height determined by the required building rows.
Logical town lots are capped at five by three tiles to match the single-building
art, with seven-tile spacing and room for fronts. Civic plazas, public gardens and
market squares occupy a reserved band with paths joining them to the street
network. Final paving, trees, benches, stalls and house-front planters run after
regional terrain dressing; they protect roads, entrances, props and common-worker
positions. Northern gardens retain snow rather than tropical lawn.

The main wall ring sits nine tiles from the north/west edges and ten from the south/east
edges. A four-tile projection creates a stepped outline: northern watch towns and
Moonspire have a northern court, while trading and river towns have an eastern bay.
Gates move with any affected wall segment and retain their roads. Two existing homes move into the southern suburbs, reached through the
gates on the same map. All city wall rendering now uses 2.5-tile horizontal faces
and 1.5-tile vertical screen width, with larger gates and towers. Logical wall
collision remains one tile; the larger silhouette is visual, like building roofs.

Town building keys, palettes and original interior IDs/seeds are retained. The
interior anchor table decouples saved furniture identity from new exterior lots;
return transitions use the current door. Outdoor saved coordinates can now refer
to a different part of town; normal safe-position recovery handles blocked tiles.
Restart the running game to regenerate its cached maps.

Validation: TownLayoutTest (three seeds, public-space clearance, frontages,
suburbs and wall setbacks), RegionalSettlementTest, TownArchitectureTest,
RegionalBuildingsTest, BuildingCollisionTest and TownNpcNavigationTest.
RegionalSettlementTest checks one continuous eight-corner town ring, four cardinal
gates and all approaches (capitals retain their four-corner rings), and limits straight
avenues to half the expanded map rather than the old fixed 28-tile threshold.
RegionalBuildingsTest derives furniture seeds from the retained interior ID.
Run TownLayoutTest --render to export town and suburb views to
temp/town-layout/review; curated evidence belongs under asset-review/reviews.

## Cardinal house entrances (2026-09-24)

All seven towns have west, east and north house views; existing art supplies south.
Side views use horizontal ridges and edge-on vestibules, with steps projecting
left/right. The north door is behind the roof, approached from above. Three
residences in each town use these views; side-facing lots reserve at least four
tiles of width and directional lots reserve three tiles of depth.

Regional materials match each town: river plaster/timber and slate, snowy northern
granite/timber, Hearthlands red tiles and scholarly details, Fenland reed roofs
and stilts, Sunrealm sandstone/parapets, and coastal timber/slate. Northwatch
retains its first three authored placements; other towns deterministically select
ordinary homes inside the walls. Named institutions and original interior anchors
are preserved; south-facing homes and suburbs remain represented.

CityBuilding.Facing defaults to SOUTH for existing constructors. Its outside()
transform drives street approaches, NPC standing spots, click-to-enter and
interaction. Interior IDs remain stable and exits return to the actual approach.
Directional bases have solid collision and clear thresholds; entries accept only
the outward adjacent tile. Furniture reserves the matching approach corridor.
Side artwork is anchored to its doorstep rather than centered on a south door.

[Northwatch originals](../../art-source/towns/2026-09-24-cardinal-houses/README.md)
and [regional originals, prompts and import recipe](../../art-source/towns/2026-09-24-regional-cardinal-houses/README.md).
CardinalBuildingTest checks all seven towns over three seeds, direction, collision, clear approaches,
interior identity and three entry methods with correct return positions.
TownLayoutTest --render adds twenty-one full-zoom directional views.

## Furnished outdoor areas

TownExteriorRooms runs after the public surfaces and regional commons. Markets
have three-piece stall rows with stock behind them. Parks and residential fronts
use clipped hedge borders and continuous planter beds, with openings at paths and
doors. Civic areas have joined benches and small reading courts. Craftsmen and
traders receive L-shaped or stepped courts, falling back to compact rectangular
yards, with rail fencing, storage, working props, seating and open gateways.
Candidates are selected near their owning buildings;
limits per district and clearance around each yard prevent filling every gap.

Placement reserves streets, doors and their approaches, roofs, NPC homes, common
work/gathering positions, transitions and portals. A group is placed only if every
piece fits. Gates are passable; hedge and fence modules have tile collision.
Three-tile entrances connect to nearby streets through clear footpaths. Existing props
and regional features outside the composed areas retain their placement.

ConnectedFurniture now also joins identical outdoor benches, planters, stalls,
horizontal hedges and rails. Modules have stable tile anchors, avoiding decorative
jitter between joined pieces. The renderer queries neighboring tiles rather than
scanning every town prop for every connection. Interior counters and shelves keep
their existing footprint sizes and behavior. ConnectedBoundary uses a shared
ground anchor and all sixteen cardinal neighbor masks for hedge and rail ends,
corners, T-junctions and crossings. Horizontal and vertical asset names can mix.
Gate shoulders join adjacent boundaries across the three-tile opening.

Garden entrances include vine-covered timber arbors, open iron gates and living
hedge arches. Regional civic, northern granite and sandstone fountains furnish
plazas and larger reading courts; their basins block a two-by-two-tile footprint.
Source sheets and import recipes are in
[the garden feature batch](../../art-source/towns/2026-09-24-garden-features/README.md).

Eight imagegen hedge/fence sprites and their original sheets, exact prompts and
import command are recorded in
[the source batch](../../art-source/towns/2026-09-24-exterior-modules/README.md).
Runtime art lives under environments/settlements/city/props/garden.
TownExteriorRoomsTest verifies alpha, joined edges, generated furnishing density,
street reservations and boundary collision; ConnectedFurnitureTest checks the
existing interior behavior. ConnectedBoundaryTest verifies every cardinal mask
at three scales, gate openings and fountain collision. TownLayoutTest --render
also captures each wall bay and garden details at full zoom.


## Regional planting and lived-in gaps (2026-09-24)

TownGardenArt selects garden soil, trees, water features and local goods by town.
Greyharbor is temperate despite sharing Freehold culture with snowy Northwatch:
its gardens use grass and an unfrozen civic fountain. Embermarket uses sand,
Reedwatch marsh soil, and only Ironvale/Northwatch retain snowy garden terrain.
The misleading legacy scrub-pine asset depicts crystals; town planting now uses
verified evergreen art instead. Greyharbor's common uses nets, barrels and a
notice board instead of a tundra rune prop.

TownExteriorRooms composes up to eighteen small planted pockets in unused land
near buildings and suburban homes. Fruit trees, wildflowers and produce baskets
alternate with two tilled seedling beds and a soil aisle in temperate towns.
Cold towns use evergreen groves and frost shrubs; Reedwatch uses cypress and
sedge; Embermarket uses palms, dry grasses and gardens with water troughs.
Whole plot footprints must clear roads, public squares, doors, buildings, NPC
positions, common workers, portals and existing furniture. No new random seed
is introduced. Market stock and workshop pairs follow each town's trade.
Coastal and marsh work courts use plank surfaces, desert courts packed ground.

Validation: TownExteriorRoomsTest checks all seven towns over three seeds for
missing art, new planted pockets and snow/frozen props in non-snowy towns;
TownLayoutTest --render, RegionalSettlementTest --layout-only,
CardinalBuildingTest and TownNpcNavigationTest cover circulation and entrances.
Curated screenshots and reproduction instructions are in
[regional planting review](../../asset-review/reviews/town-architecture/2026-09-24-regional-planting/README.md).


## Working plots and modest infill

One new barn/stable occupies the gap between the lower quarters of each town,
adding seven buildings without expanding maps. Its key is separate from existing
interior anchors. Three new imagegen sprites use temperate oak/slate, northern
stone/pine/snow and desert sandstone/reed materials; there are now 108 distinct
building sprites across sixteen views per town. Barn entrances open feed-storage
interiors with grain reserves, a seed store, tally desk and distribution area.

Specialized buildings receive yards before generic civic/workshop decoration:
barns have fenced paddocks, hay and troughs; apothecaries/alchemists have tilled
herb beds and drying supplies; blacksmiths have outdoor forges, anvils and sales
stalls. Existing building roles determine the furnishings. Compact yards fill
smaller spaces while their gates and street connections remain walkable. Yard
exits must already be passable, so footpaths cannot cut through town walls.
Paddocks are furnished scenery; this change does not add animal simulation.

[Originals, exact imagegen prompts and import manifest](../../art-source/towns/2026-09-24-working-plots/README.md).
TownLayoutTest verifies the barn interior and role-specific yards in all seven
towns over three seeds. TownArchitectureTest includes all three regional barns.

## Streets and public-space continuity

Gate-to-civic routes and public garden/market connections form the cobbled main
network, preserving existing circulation and organically bent streets;
feeder streets fade through worn stone into packed dirt on quieter branches.
These remain ordinary navigation tiles. The layered renderer blends adjacent
road materials and treats roads crossing a public square as continuous paving.
Greyharbor uses temperate ground beneath its paths and gardens.

Disconnected hedge fragments are removed after furnishing. Connected boundaries
retain their shared anchors; street lamps use existing left/right/front artwork
selected toward nearby roads, with small shared render/collision anchor offsets.
This does not add freely rotated sprites or new lamp artwork.

## Residential infill and street population (2026-09-24)

Each town gains four ordinary regional homes without increasing map dimensions:
two occupy the upper central gap, and two extend the southgate residential suburb.
Stable building keys create separate normal house interiors and retain existing
building/interior identities. Roads are generated around these footprints.

Twelve additional residents are placed after exterior furnishing: four market
porters/shoppers, two messengers, two garden tenders and four nearby household
residents. Placement rejects occupied tiles, props, buildings, transitions and
immediate door approaches. They use existing citizen art, local regional dialogue
and the existing ambient routine/navigation system; no new commuting jobs or
save fields are introduced. Placement is deterministic. The town's common,
work yards and gardens remain reserved; fountains may use a three-tile clear
footprint when their larger apron cannot fit.

TownLayoutTest checks infill count, twelve distinct safe resident positions,
clear entrances, retained fountains, public spaces and barn/paddock proximity
across seven towns and three seeds. RegionalSettlementTest and
TownNpcNavigationTest cover connected entrances and moving crowds.


## Grand landmarks and civic grounds (2026-09-24)

Briarbridge, Ironvale and Embermarket now use new cardinal charter-hall,
forge-keep and public-water-court artwork. Moonspire, Reedwatch, Northwatch and
Greyharbor retain their distinctive towers at the larger landmark scale. These
signature buildings receive the first civic plot and a six-tile frontage.
Original building keys and existing interior anchors remain intact.

Landmark approaches receive named forecourts, regional paving and safe side
planting/seating. Reedwatch and Greyharbor use plank courts; Embermarket uses
packed desert paving. Reflecting ponds remain deferred: no complete basin and bank fit these plots
without compromising circulation. Existing regional fountains remain in place.
Decoration respects other buildings and movement reservations. Moonspire's first added
central infill building is now a botanical glasshouse with an alchemist interior
and herb-garden program, reusing its stable building key.

The old river court is used by Foxbarrow's ferry institution; the old Sun Court
is used by Sunmere's cistern house. Their existing institutional function,
interiors and residents remain. No originals are deleted or overwritten.

[Originals and repeatable import](../../art-source/towns/2026-09-24-grand-landmarks/README.md).
TownArchitectureTest verifies landmark scale, ordinary-building scale, alpha and
both renderer paths; RegionalBuildingsTest verifies the two explicit village
reuse routes. TownLayoutTest --render includes close-ups named *-landmark.png.

Embermarket formerly had two lots sharing the Sun Court key. The secondary
lot now has a separate Water Ledger Annex identity; the principal court retains
its prior key and interior anchor. Layout diagnostics require unique building
keys and exactly one signature landmark per town.


## Context-aware asset placement and offsets (2026-09-24)

The earlier two-by-two roadside bench/tree groups are replaced. They could pass
clearance checks while leaving a bench pressed against a wall or facing lawn.
TownExteriorRooms now applies the following rules before placing a composition:

| Feature | Placement rule | If it does not fit |
| --- | --- | --- |
| Square seating | Two joined benches on existing court paving, facing two clear paved rows to the south; two-tile wall/tower setback. Search the northern edge of civic squares and the sides of landmark approaches. | Leave the space open; do not put the seat in a grass scrap. |
| Garden and house-front hedges | Evaluate the entire proposed border, retain connected stretches of at least three modules (including corners), and place each stretch together. Keep door and road gaps. | Omit short fragments; never close a path just to connect hedges. |
| Street groves | Three-by-five or five-by-three natural-ground strips with continuous paved frontage on opposite sides, an open walking rim, and no reserved square or work site. Two aligned regional trees sit between two complete five-module hedges; short ends stay open. | Skip the site. There is no requirement to force a grove into every town. |
| Offsets | Use existing WorldProp offset fields, in reference pixels (48 per tile). Both rails shift outward by 12 pixels; all modules in a rail share its offset. Tree roots shift up 12 pixels. Paired public-garden trees move inward 12 pixels. Square seats move back 12 pixels from their apron. | Reject a composition if any shifted solid footprint reaches a road, reserved approach or existing object. |

Whole grove footprints and margins are checked before changing anything. Only
ordinary non-harvestable tree/grass/bush scenery inside an accepted plot may be
recomposed; existing goods, harvestables, named features, workers and transitions
remain protected. Later infill planting respects the grove's walking margin.
Regional ground and tree selection remain in TownGardenArt. Up to six groves may
fit a town, with ten-tile spacing between their centers. The current seven-town
layout yields groves in six towns; Briarbridge has no eligible complete strip.

This follows the editor's authored-offset system rather than adding renderer-only
jitter. Tile-based NPC navigation now tests shifted structural footprints at tile
centers, while swept player collision retains the full rectangles. Continuous
boundaries no longer receive detached canvas-bottom contact-shadow ellipses.
The garden renderer and connected boundary assets otherwise retain their sizing.

Validation: TownPlacementRulesTest (package com.alderfall.game.map) covers invalid
wall seats, blocked/unpaved aprons, broken hedge runs, corners, road crossings and
reserved gardens. TownExteriorRoomsTest checks square seating, complete groves,
shifted road clearance and collision across three seeds. TownLayoutTest --render
exports *-square-seating.png and *-street-garden.png; Briarbridge's latter view
falls back to its civic square. RegionalSettlementTest, TownNpcNavigationTest
(all seven towns), EditorPropCollisionTest and ConnectedBoundaryTest cover access,
determinism, NPC routines, offset structures and joined boundaries.

[Placement-rule review captures](../../asset-review/reviews/town-architecture/2026-09-24-placement-rules/README.md).


## Road-shaped land plots (2026-09-24)

TownLandPlots replaces the small rectangular infill planting pass. Town roads,
existing public spaces, walls, buildings, door approaches, working yards and
street groves cut the remaining usable natural ground into connected components.
Large components are divided by competing flood fills, preserving road bends and
notches rather than clipping them to a rectangle. Each accepted parcel contains
14-64 cells, a usable interior and direct street frontage; narrow scraps remain
open. Up to fourteen parcels are furnished per town. Current layouts yield 6-14.

Each plot records its nearby building owner and one purpose: household garden,
herb garden, quiet civic garden or trade yard. This metadata is available through
MapArea.townPlots(). Nearest-building ownership selects purpose; it does not move
buildings, create additional interiors, or grant land ownership to the player.
Generation is deterministic and does not add save data.

A paved access spine runs from street frontage to a usable point near the plot's
centroid. Planting and borders reserve that spine. Hedges or trade-yard fences
trace the perimeter of the broadest connected inset core on half-tile vertices.
Thin fingers and ambiguous junctions stay open, avoiding small hedge cages and
lattices at stepped corners. Shared offsets keep the modules connected; shifted
collision is checked before placing each segment. Border fragments shorter than
three modules are omitted. The inset leaves clearance from roads and adjoining
plots. Existing multi-tile props, workers, landmarks and harvestables are retained.

Household plots use fruit trees where appropriate, cold gardens use evergreen
shelter and frost shrubs, wet gardens use cypress and sedge, and herb plots use
seedling beds. Trade plots receive regional paving and local goods. Ordinary
non-harvestable scenery inside accepted plots may be recomposed. Furnishings and
beds fit the actual cell mask, so irregular land produces irregular gardens.
Household and civic ground cover uses flowers; seedling trays belong to herb plots.

TownLandPlotsTest verifies a bent-road fixture, complete/disjoint subdivision,
connectivity, bounded size, stable ordering, clean contours, ownership and clear
access across all seven towns and three seeds. TownLayoutTest --render includes
*-plots.png. Use --render-plots-only for just the seven plot captures, without
rerunning layout assertions. Existing regional, furnishing and NPC checks apply.

[Road-shaped plot review](../../asset-review/reviews/town-architecture/2026-09-24-road-plots/README.md).
