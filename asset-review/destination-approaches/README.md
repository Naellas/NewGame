# Trails to playable destinations

Destination approaches reuse the four new biome sheets alongside existing signs, milestones, supply crates, fences, buildings, camp layouts and dungeon entrances. No additional image generation was needed.

`DestinationApproaches.populate` runs after the authored world and quiet nature clearings exist. It targets the registered adventure entrances and campaign place coordinates, so the endpoint leads to an actual location with existing encounters/objectives instead of a decorative placeholder. A small seeded subset of quiet clearings becomes persistent loot caches. This adds environmental presentation and cache rewards; it does not add new quest scripts, enemy species or an elite-enemy system.

The route finder uses bounded weighted search (at most 2,400 expansions / 28 steps), prefers natural ground, and seeks an existing road. If a road is unavailable, a reachable nearby trail mouth serves as the hint. Every segment uses swept player collision. Locations without a viable approach are skipped. Water, blocked terrain, unrelated transitions and other location kinds are excluded.

Worn paths are about one-third of a tile wide, with irregular dabs, fading mouths and occasional two-segment gaps. A sign near the mouth and sparse milestones or camp supplies indicate the destination. Their hover labels name it. Ground-cover patches soften the edges. Some routes receive short fence sections with an opening; fence assets retain actual movement collision. All route tiles are reserved before dressing, including earlier quiet trails.

Only the final local dirt-road spur can receive a narrower visual ground treatment; junctions, cobblestone roads and underlying gameplay tile types are preserved. Routes are baked into the existing terrain chunk cache. No additional per-frame flora sorting is required.

Caches use `ChestSystem` and its existing save format. Looting a chest leaves a persistent empty entry, preventing rewards from refilling on load. Linked dungeons retain their current floors, loot chests, resident enemies and bosses; campaign locations retain their current quest hooks.

Validation:

- `DestinationApproachesTest`: two seeds, deterministic routes, real endpoints, route length bounds, clear collision after dressing, sprite availability, dungeon bosses/loot, and empty-cache persistence across save/load.
- `GroundDetailBatchTest`: rough and quiet trails, worn gaps and narrowed road surfaces across cached chunk boundaries, three camera offsets and three zoom levels.
- `NatureSpacesTest`: the remaining quiet trails stay clear.
- `LocationEncounterTest`: 363 actual location encounters, including 324 on paths.

Seed 42 produced 18 dungeon/camp approaches, 13 campaign-place approaches and two caches. Seed 1337 produced 17, 13 and one respectively. Coverage is deliberately sparse; not every registered location receives a trail.

Actual game previews from `Java/tools/DestinationApproachReview.java`:

- [Bandit camp approach](scene-bandit_camp.png)
- [Crypt approach](scene-crypt.png)
- [Campaign location approach](scene-old_road_marker.png)
- [Wayfarer cache](scene-cache.png)
