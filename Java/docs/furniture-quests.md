# Furniture quest interactions

## Playing Missing Provisions

Talk to **Innkeeper Senn in Oakhaven's inn** and accept **Missing Provisions**.
Inspect the long service counter, read the marked pantry ledger, explicitly ask
Senn about its reserve entry, collect the released reserve parcel, and place it
on the service counter. Return to Senn for 35 gold and the usual scaled quest XP.
Stand beside the highlighted furniture and press E (or Talk / Enter).
The prompt names the current action. Wide counters can be used from any exposed
side of their footprint. Other inns and similar shelves do not count.

The ledger is shown during its investigation. The parcel appears on the pantry
shelf once Senn releases it, disappears when collected, and appears on the counter
when delivered. Discoveries, carried quest cargo and completion survive saving;
the existing save policy resumes interior saves outside the building. Re-entering
reconstructs the visual state. The parcel is quest cargo, not a sellable item.

## Implementation and extension

`FurnitureQuestContent` declares stage-to-furniture role bindings, action labels,
and optional required/granted/consumed cargo. The first quest uses the generated
Oakhaven inn and its actual props. Bindings select matching furniture in a stable
row/column order, excluding props with no passable adjacent tile. The existing
quest stage IDs and observed-stage set own progress and visual state. No new save
format or runtime image files were introduced.

`GameState` resolves these bindings before generic objective spawning, validates
the current stage, map, furniture and reach at interaction time, and applies cargo
changes only after successful quest progress. Senn's conversation must be selected
explicitly. Existing reward handling prevents repeat rewards. The renderer draws
small existing item sprites on the furniture, and queues objective highlights after
depth-sorted props so furniture cannot hide its own marker.

This first implementation covers inspect/read, collect, place and a keeper
conversation. It does not add locks, workstation puzzles, branching outcomes,
a universal furniture inventory, or persistent identities for arbitrarily moved
editor furniture. Bindings are authored room roles, not IDs attached to movable
WorldProp instances. Future movable quest objects need instance identity and
binding invalidation in the editor/save path before those behaviors are promised.

## Verification

From the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/build.ps1 -IncludeTests -IncludeReviews -OutputDirectory temp/furniture-quests/classes
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/check.ps1
```

From Java, using Java 21:

```powershell
java -Djava.awt.headless=true -cp temp/furniture-quests/classes com.alderfall.game.FurnitureQuestTest
java -Djava.awt.headless=true -cp temp/furniture-quests/classes com.alderfall.game.RenderCacheTest
java -Djava.awt.headless=true -cp temp/furniture-quests/classes com.alderfall.game.InteriorDesignPreview temp/furniture-quests/captures furniture-quest
```

The new quest test exercises three world seeds, actual E interactions, explicit
keeper testimony, wrong-map and stale interactions, missing-cargo rejection,
one-time consumption and reward, and active/completed save round trips. Generated
saves stay under ignored temp/furniture-quests. The broader QuestNarrativeTest
passes 2,052 checks. Run legacy narrative diagnostics from an isolated scratch
working directory with assets/config available: they write out-story-refinement.
CompanionQuestSegmentTest still fails at seraphine_refuge_threshold because its
interaction enters Orin's Cookshop. The same failure was reproduced with the
pre-change GameState and QuestNarrative compiled into an isolated scratch output.

[Reviewed captures](../../asset-review/reviews/furniture-quests/2026-09-23-missing-provisions/README.md)
show the ledger and delivered parcel through the actual world renderer.
