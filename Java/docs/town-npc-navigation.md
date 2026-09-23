# Town NPC navigation

Outdoor city and village residents follow cached paths to their existing daily activity targets. Street and plaza tiles cost less than open ground, so residents prefer reasonable road detours. Cardinal steps avoid cutting building corners. Assigned destinations are no longer restricted by the old wandering radius around home.

Guards visit up to four stable checkpoints near gates, lamps, banners, notice boards and wells, with street checkpoints as a fallback. Other residents alternate between their activity target and up to two nearby clear spots. Sleeping residents stay at home. Each completed stop has a pause based on the existing activity timing; travel continues smoothly between stops.

Routes avoid terrain obstacles, map transitions, quest blockers, the player and other residents. Moving residents reserve both their origin and destination until the current step finishes. A blocked next step triggers a short yield followed by replanning. After repeated failures, residents try a nearby arrival spot or skip the unavailable checkpoint and revisit it on a later circuit.

Schedules and weather are checked every 90 world ticks after any current movement step finishes. A changed routine replaces the route and checkpoints. Route searches are capped at 4,096 expanded tiles. Navigation is transient and resets with NPC runtime; no save migration is needed. Overworld commuters and indoor movement retain their existing behaviour.

## Player priority at entrances

Residents in cities, villages and interiors are soft obstacles for the player. Walking into a resident's tile prompts a safe sideways step, followed by a brief pause. Residents never yield into walls, transitions, quest blockers, other residents or the player's previous tile, and never retreat forward along the player's direction of travel. If there is no safe sidestep, the player can pass through. Scripted quest NPCs remain in position; blocking quest objectives still prevent movement.

Click-to-move adds a cost for occupied tiles, preferring a clear route when practical but allowing travel through crowded corridors and building approaches. Keyboard movement and click-to-move share the same yielding behavior. NPC interaction and companion placement retain their existing occupancy checks.

Run from `Java` after building:

```powershell
java -cp temp/checks/classes com.alderfall.game.TownNpcNavigationTest
java -cp temp/checks/classes com.alderfall.game.NpcYieldTest
java -cp temp/checks/classes com.alderfall.game.SmokeTest
```

The navigation diagnostic covers wall detours, unreachable destinations, corner safety, street preference, occupied paths, and 1,200 movement ticks in each of Elderford, Briarbridge and Riverside.
