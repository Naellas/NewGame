# Roaming world objects

## Ambush bushes

Ambushes are stationary bushes centered on passable overworld road tiles. They do
not show a dialogue marker or react to E from a neighboring tile. Stepping onto
the bush opens a Roadside Ambush popup through the normal movement path, before
the ordinary random encounter roll. Fight starts combat with the already selected
nearest-dungeon group; closing the popup also proceeds to combat. The bush is
consumed before the popup opens, so
fleeing or revisiting that tile cannot trigger the same ambush twice.

Spawning excludes the player's immediate vicinity, obstructed tiles, existing
props, NPCs, transitions, quest objectives and nearby adventure entrances. Dirt,
cobblestone and packed roads qualify; bridges, rocky terrain and settlement maps
do not. If no qualifying road is found, that spawn attempt is skipped.

`WorldMap.nearestDungeonInBiome` compares squared entrance distances after
filtering to dungeons whose stored exterior biome equals the biome surrounding
the road. Road biome inference reuses the dungeon entrance's existing weighted
surface-biome resolver. Ties use map ID. With no same-biome dungeon, no ambush is
spawned. There is no extra distance cap beyond selecting the nearest matching
entrance. `GameState.roadAmbushers` draws two or three enemies from that dungeon's
existing first-floor roamer pool using the event seed; no separate copied monster
catalog or dungeon boss placement is introduced.

## Purses and shrine events

A lost purse is a small leather world object, not a moving dust marker or an NPC.
Approaching or stepping onto it does not open anything. Deliberately pressing E
opens the existing event choices in a compact event card without an NPC portrait,
relationship information or conversation options. Result text remains in that
card with one Continue action, preventing repeated rewards. This retains choices
only on intentional interaction; discovery itself has no popup.

Shrine events attach only to existing `deco_imagen_shrine_stone`,
`deco_forest_shrine_stone` or `folklore_boundary_shrine` props near the player,
with a passable adjacent approach. No phantom shrine is spawned at a random
coordinate. Three cached runtime palette variants (blue, amber and violet) tint
the existing transparent sprite. A soft foot glow and rising particles identify
an active shrine. Effects render after depth-sorted props. Deliberate interaction
runs the existing shrine rites, and removes the active tint/glow. A resolved shrine
does not immediately recur during the session.

The bush and purse use generated transparent sprites in
`assets/environments/locations/quest/events`. Two additional sprites show supply
caches and document bundles for existing quests: presentation maps
`quest_supply_cache`/`quest_inspection_cache` and `quest_document_bundle` to the
new artwork without changing logical IDs or saved progress. Imagegen originals,
exact prompts and the import recipe are in
`art-source/quest-objects/2026-09-24-world-events` at the repository root.
Shrine colors remain runtime variants. Wounded travelers now recline on a
transparent bedroll with a pack, reusing the Mira world character that matches
their event dialogue. Hidden traps have a visible jaw/pressure plate mechanism;
fish runs show three swimming fish with small wakes, confined to a water tile
with a reachable neighboring bank or wading tile. These events remain stationary
and fully visible from their first frame until interaction or local retirement.
Travelers and traps require clear dry ground. Their visuals use the world depth
pass, and their existing deliberate interaction / trap entry behavior is retained.
Ambient gusts, dust and fireflies remain environmental effects only.
Menus and combat suspend event aging rather than erase
nearby events. Map changes or traveling more than 24 tiles away retire local
objects. These are transient events: their placement and resolved-shrine set are
not saved, and new-game/load resets clear them.

## Ownership and checks

GameState owns the shared RoamingWorldEventLayer instance. GamePanel ticks and
draws it, while movement calls its entry trigger directly. This avoids requiring
an extra UI frame to fire an ambush or letting a random encounter replace it.

From the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/build.ps1 -IncludeTests -IncludeReviews -OutputDirectory temp/roaming-events/classes
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/check.ps1
```

From Java with Java 21:

```powershell
java -Djava.awt.headless=true -cp temp/roaming-events/classes com.alderfall.game.RoamingWorldEventTest
java -Djava.awt.headless=true -cp temp/roaming-events/classes com.alderfall.game.BattleDefeatTest
java -Djava.awt.headless=true -cp temp/roaming-events/classes com.alderfall.game.RenderCacheTest
java -Djava.awt.headless=true -cp temp/roaming-events/classes com.alderfall.game.WorldGenerationPreview temp/roaming-events/captures roaming
```

All three tests passed. Coverage includes nearest matching entrance, valid seeded
groups, actual movement into the bush, no remote or repeated trigger, real shrine
bindings, palette alpha preservation/cache reuse, pause/reset behavior, deliberate
purse interaction and reward replay. The retained trap defeat/revival test passes.
[Reviewed visual evidence](../../asset-review/reviews/roaming-events/2026-09-23-world-objects/README.md).

[Generated artwork and popup review](../../asset-review/reviews/quest-objects/2026-09-24-generated-events/README.md).
