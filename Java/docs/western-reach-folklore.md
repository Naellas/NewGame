# Western reach: outward worldbuilding pass

This working document tracks Briarbridge, Riverside, Foxbarrow and Redcap. The concurrent [Hearthlands work](hearthlands-worldbuilding.md) owns Oathstead, Oakhaven, Elderford, Archive City and Moonspire. Western content is isolated in `WesternReachFolklore`, `WesternReachPropGenerator`, `WesternAbbeyGrounds` and `WesternReachWorldTest`.

Use [world-framework.md](../../Story%20premise/world-framework.md), sections 7 and 12 (River and Thorn Kingdoms), and [faith-cults-and-dungeons.md](../../Story%20premise/faith-cults-and-dungeons.md), Sanctuaries of the Open Bough, as the source of truth. Coordinate approach placement with [worldgen-coherence.md](worldgen-coherence.md).

| Place | Implemented |
| --- | --- |
| Briarbridge | Guest Abbey exterior on the existing `bridge_inn` lot, named entrance and traveler interior, Open Bough Host and Abbey Gardener, petition bench, bread oven and journey-tree seedlings. |
| Abbey grounds | Open-bough gate connects Briarbridge to a public orchard with journey trees, shared fruit, petition benches, Orchard Tender and Petition Steward. Steps lead to a stone undercroft with Threshold Keeper and a physically sealed mirror-door. |
| Riverside | Charter Hall on the existing `river_hall` lot, study interior with Crossing Clerk and River Advocate, petition bench and grain cart. Dialogue explains obligations on both sides of a crossing charter. |
| Foxbarrow | Orchard House on `west_cottage`, seed-and-bread exterior and bakery interior, orchard residents, shared produce, pruning tools and maintained woodland marker. |
| Redcap | New patched supply shelter inside the first-floor warren and beside its actual generated overworld location. Observations describe provisions and repairs without inventing a cult allegiance. |
| Western approaches | Woodland markers near Briarbridge, Riverside and Foxbarrow. |

Nearby objects produce a short observation during exploration and can be read again with Talk / Enter when no higher-priority interaction applies. Named buildings retain their lot keys, collision geometry and interior IDs. Building interiors reuse the existing inn, study and bakery layouts and have local names and residents. The orchard and undercroft are separate authored maps. No new quest gates, rewards or automatic guest-binding effects are introduced. The passage beyond the mirror and the fugitive hearing remain future story work.

## Latest update: public orchard and sealed threshold

Enter the open-bough gate along the Guest Abbey's street in Briarbridge. The gate's location follows the generated settlement layout and uses an unoccupied, reachable approach after regional population has finished. Walk north through `garden_briarbridge_abbey` to the stairs into `undercroft_briarbridge_abbey`. Both routes have return exits and arrivals off transition tiles.

Dialogue explains journey-tree care, gifts freely given, the public petition bench, and the right to refuse a formal invitation while requesting protection. The mirror is barred, has blocking stone behind it, and has no portal transition. The orchard and threshold are inhabited, peaceful places. Undercroft saves follow the game's existing interior policy: loading resumes outside its stairs in the orchard; orchard saves preserve the player's position.

## Next outward updates

- Riverside: develop the bridge-charter dispute from the existing hall and river workers, with the actual crossing agreements visible before any quest outcome is added.
- Foxbarrow: deepen orchard labor and the maintained woodland boundary; distinguish ordinary neighborly gifts from a Briar Court bargain.
- Redcap: extend lived-in camp detail around generated supply routes, using the existing provisions and repair shelter as the starting point.
- Later abbey story: implement the fugitive hearing and the explicit invitation conditions before opening any mirror passage. The current benches and keeper do not resolve that dispute.

Building art and scenery use transparent PNGs, the existing sprite fitting, depth sorting, shadows and environmental lighting. Decorative placement avoids existing structures, NPCs, transitions and occupied props; the garden gate intentionally marks its own new transition. Only small grass, flower and leaf decorations may be cleared by the western prop pass. Route tests cover three seeds.

## Assets and review

Built-in imagegen assets live in `Java/assets/environments/settlements/city/folklore/`. Western files are `folklore_briarbridge_guest_abbey.png`, `folklore_redcap_supply_shelter.png`, [folklore_open_bough_gate.png](../assets/environments/settlements/city/folklore/folklore_open_bough_gate.png) and [folklore_sealed_mirror_door.png](../assets/environments/settlements/city/folklore/folklore_sealed_mirror_door.png). The new gate and mirror use the abbey as their visual reference. `folklore_returning_seedhouse.png` and `folklore_boundary_shrine.png` are shared with the initial artwork pass. Exact prompts and the abbey finial correction are in [western-prompts.md](../assets/environments/settlements/city/folklore/western-prompts.md). Earlier asset prompts are in [prompts.md](../assets/environments/settlements/city/folklore/prompts.md).

In-game review captures are under `asset-review/western-reach/` at repository root.

## Validation

Compile all sources with JDK 21, then from `Java/` run:

```powershell
java '-Djava.awt.headless=true' -cp out-western-check com.alderfall.game.WesternReachWorldTest --render
```

The checks cover PNG alpha and asset lookup, required placements, spatial indexing, approach reachability, building entrances, local dialogue, reachable interior residents, orchard/undercroft return routes and the sealed mirror for seeds 0, 42 and 1024. Save/load checks use an isolated temporary directory and remove their own files. Rendering uses the real game panel and writes six review captures without saving an adventure.

Latest validation (2026-09-22): full JDK 21 compilation and `WesternReachWorldTest --render` pass, including all six renders and the isolated save/load checks. The broader `SmokeTest`, run from `temp/western-abbey-smoke/` to keep test saves separate, stops at `SmokeTest.java:1035`: `Dunewick Village has disconnected village building roads. Connected=0 buildings=5.` The full smoke suite is not passing; this pass does not change Dunewick's layout. An earlier run reached a separate Aria road-sign objective failure, also documented in the world-generation report; concurrent changes mean that earlier result is not the current stopping point.
