# Settlement building and terrain

Open Village (`V`) at Oathstead. **Build** places buildings; **Manage** lists
placed buildings with their tier, assigned worker, storage and next upgrade
cost. Management cards and details use the same individual building names as
the map; the details subtitle retains the building type. Search by building
name, type or assigned worker. Manage opens the existing
worker/interior panel; Upgrade applies the displayed cost, drawing from village
stores and the player's inventory. Creative mode remains free.

All 18 building types have six visually distinct levels: Camp, Small Village,
Village, Small Town, Large Town and Metropolis. Each profession retains its own
identity at every stage. The reference's progression is explicit: canvas camp,
timber hut, thatched/plastered village building, taller framing and slate,
substantial multi-storey stone, ornate metropolitan stone and copper roofs.
There are 108 playable sprites; footprints and interior/save identities stay stable.

Camp costs now start at roughly half their previous gold/material costs. Each
subsequent tier uses the camp gold price multiplied by tier squared / 2, and
camp materials multiplied by tier. Tier 3 adds stone; tiers 4-6 add iron.
For example, cottages cost 10g to place, then 20/45/80/125/180g to improve.
Existing saves keep their buildings and levels; no retroactive charge applies.

Build offers a larger, paginated **building catalogue** with Housing, Production
and Civic categories. Select a plan to return to map placement. Escape closes
just the catalogue. The sidebar retains quick placement, move and removal.
Manage retains upgrade cards and now shows potential daily resource output,
expected gold over the next game day, and the last production cycle's actual
stored resources. Rare quantities are averages, not guaranteed drops. Resource
forecasts assume room in storage; gold continues when stores fill.

**Workers:** click **Set**, then click the workplace sprite on the map. Cancel
or Escape abandons selection. Set stations a traveling companion first; Recall
returns them to the party. Building details can assign/remove individual staff
or clear all slots. Tiers 1-2 have one worker slot, 3-4 two, and 5-6 three.

**Housing:** cottages supply 2 beds per tier, longhouses 4 per tier. Beds are
allocated automatically in resident roster order across placed houses. The
Housing tab shows residents, total beds, unhoused residents and each person's
home. NPCs may join without housing, but cannot take a workstation or produce
until housed. Removing housing pauses surplus workers without losing their
workplace assignments. At night residents appear in their home interiors.

Production occurs every 900 ticks (eight cycles per 7200-tick game day).
The assigned building determines the resource and relevant profession:
foresters gather wood, mines stone/ore, hunters meat/hide, fishers fish,
farms/granaries rations, bakeries stew, gardens herbs, apothecaries salves,
blacksmiths ingots, workshops wood/construction supplies and warehouses fibers.
Staffed civic buildings provide income. Houses do not act as workplaces.
Skill, tier and interior equipment improve quantity and rare-find chance.
Tier 3+, skill 6+ and tool score 2+ unlock richer finds for mines, forestry,
blacksmiths and apothecaries. Tool score includes tier equipment and placed
interior work surfaces. The same production rules drive forecasts and payouts.

The town board uses a newly generated, resized notice-board sprite and styled
panel. Stage counts now reflect all 12 milestones. Props include refreshed
carts, benches, barrels, crates and lamps. Props/Inside offer X/Y offsets in
four-pixel steps up to 24 reference pixels each way. Preview, rendering, movement,
expansion and saves preserve offsets; old saves default to zero. Footprints
use the existing shared prop-offset representation.

**Village editor tools:** Props and Inside have **Grid snap** and **Collision**
toggles. Grid snap Off places or moves the prop root at the cursor's sub-tile
position, at one-reference-pixel precision. Grid snap On retains the manual
offset controls. Collision Off permits multiple props on one tile and overlapping
furniture. It bypasses placement occupancy checks; it does not disable walking
collision. Map bounds, solid interior walls and exits remain protected.

Right-click the village or a managed interior to open the editor menu. It offers
placement at that point, both toggles, Move/Remove for the prop under the cursor,
and one-pixel nudges in four directions. Move then click the destination. Nudges
carry across tile boundaries using the same position helper as the map editor.
The last matching placed prop is selected when sprites overlap. Move or remove
the top prop to reach one underneath. Escape or an outside click closes the menu.
Overlapping props and their offsets survive save/load and settlement expansion;
removing an interior layer preserves the collision footprint of surviving furniture.

Source sprites already remain cached across edits. Settlement terrain now uses
local material and raw elevation signatures: only changed chunks and their
blending/relief neighbors rebuild. Ordinary prop placement and tier sprite
upgrades reuse terrain. Actual settlement expansion changes map coordinates
and still requires rebuilding the expanded view.

Settlement growth remains separate from individual building upgrades. Existing
milestones 1â€“10 retain their requirements and save IDs; stages 11 and 12 add
Large Town and Metropolis. Stage 11 requires level 19, 20 buildings, six stationed
allies, 14 completed quests, 140 stored items and 130 developed tiles. Stage 12
requires level 24, 28 buildings, six allies, 16 quests, 200 stored items and 190
developed tiles. Growth preserves and shifts custom terrain and height edits.

## Terrain

**Terrain** searches ground types, including six generated settlement surfaces,
dry sand, marsh, tundra, badlands, herringbone and brick paving. Lake / River
uses the existing shore-derived water depth: wider bodies develop deep, blocked
centres. Shallow Water stays wadeable. Bridge creates a walkable crossing.
Water placement protects nearby buildings, props, entrances and the player's
immediate surroundings. These tools do not simulate flowing water or flooding.

Select **raise**, **lower**, **level**, **smooth** or **restore**, then click the
map. Height brush cycles between 1x1, 3x3 and 5x5. Level cycles the target height
from 0 through 4 in quarter steps. Raise/lower apply one quarter step per click;
smooth averages neighbours and snaps to a quarter step. Restore removes authored
height at the affected cells, returning to generated relief. The hover grid
shows editable and protected cells and the central height. Height brushes skip
water, foundations, entrances, props and the player's immediate surroundings.

Ground material painting affects one tile and preserves authored land height.
Water/bridge painting removes that tile's authored height to retain the common
water datum. Height uses the shared relief and collision field, so steep edits
can block travel; lower, smooth or restore to reopen a route. Height editing is
available on Oathstead's outdoor map, not interiors or the separate map editor.

Height overrides are saved in the optional `villageHeights` property. Older
saves default to generated heights. Building upgrades retain their original
keys; explicit grass repainting now survives save/load as well.

## Art and validation

[Full progression gallery](../tools/reviews/player-settlement/index.html).
[Specialist atlases, prompts and relocation record](../../art-source/settlement-growth/2026-09-24-specialist-tiers/README.md).
Player building art lives in `assets/environments/settlements/player_village/buildings/`,
grouped by building style; legacy IDs remain available to NPC settlements.
The reference's residential/workshop/civic sprites are in `shared/`. The other
15 styles use `settlement_<style>_tier1` through `tier6` in their own folders.
Ground textures remain in terrain/common and are unchanged by this revision.

Build and run from the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/build.ps1 -IncludeTests -OutputDirectory temp/settlement-identity/classes
```

Then from `Java/`:

```powershell
java '-XX:TieredStopAtLevel=1' '-Djava.awt.headless=true' -cp temp/settlement-identity/classes com.alderfall.game.SettlementBuildTest
java '-XX:TieredStopAtLevel=1' '-Djava.awt.headless=true' -cp temp/settlement-identity/classes com.alderfall.game.SettlementPanelTest
java '-XX:TieredStopAtLevel=1' '-Djava.awt.headless=true' -cp temp/settlement-identity/classes com.alderfall.game.SettlementEconomyTest
java '-XX:TieredStopAtLevel=1' '-Djava.awt.headless=true' -cp temp/settlement-identity/classes com.alderfall.game.SettlementCacheTest
```

The first checks upgrades, assets, terrain protection, cache invalidation,
expansion and save/load using private scratch saves. The second renders the
actual game panel, invokes sidebar buttons, and writes captures under
`temp/settlement-identity/`. WaterDepthTest and WorldElevationTest cover the shared
movement and height systems.

Validation on 2026-09-24: SettlementBuildTest, SettlementPanelTest,
WaterDepthTest and WorldElevationTest pass. VillageTerrainTest reports an exact
cached/direct pixel mismatch at `(1,0)`, also with elevation disabled. Matching
animation passes did not resolve it; its original assertion remains unchanged.
The repository check stops at the existing `.vscode/settings.json` root-policy
finding; review links, archive checksums and structure checks pass separately.
Use the project's Java 21 runtime for diagnostics; the system PATH can select
Java 26, which crashed in one diagnostic run. WaterDepthTest also needed the
existing conservative-JIT workaround (`-XX:TieredStopAtLevel=1`) on Java 21;
with that flag its collision, animation, path and immersion checks pass.

Management revision validation: SettlementEconomyTest, SettlementCacheTest and
SettlementPanelTest pass, alongside EditorResponsivenessTest and
WorldElevationTest. The production build passes. The new cache regression
checks exact fresh-render equality and confirms zero terrain rebuilds for
ordinary prop placement and building sprite upgrades. The older
VillageTerrainTest finding above is historical and was not re-run here.
[Reviewed interface captures](../../asset-review/reviews/settlement-building/2026-09-24-management/README.md).
[Town-board source, exact prompt and import](../../art-source/settlement-management/2026-09-24-town-board/README.md).
