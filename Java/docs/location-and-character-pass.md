# Physical places and conversations about people

The location catalog now creates actual surface sites. The dialogue entry points now introduce an incident and the person affected by it before offering a task. This pass follows the [story premise](../../Story%20premise/world-framework.md) and [rewrite brief](../../Story%20premise/narrative-rewrite-brief.md).

## Places in the world

All 18 outdoor campaign destinations have permanent world-map icons, names and hover descriptions. They are present without an accepted quest. Labels avoid settlement and other site labels; a crowded label can be read by hovering or zooming. The [generated library](../../Story%20premise/location-library.md) records each template and its seed-0 marker coordinates.

Surface generation reuses the existing camp, graveyard, road-marker and watchpost placement rules. New arrangements use those same clearance, edge and prop-cluster helpers for a caravan halt, toll station, orchard, reed beds, bell landing and guest shrine. The shrine and forge share one compound with separate interaction areas. Fruit trees distinguish the orchard; reeds distinguish the medicine beds; bells and burial stones identify the watch sites.

Crowhook, Miredepth, Stonegate and Blackvault use their existing adventure entrances and layouts. The other new surface sites do not imply additional dungeon interiors. New patches are excluded from legacy numbered objective lookup, preserving its ordering and modulo behavior. Named campaign objectives and their enemies use reachable ground inside their site, and those enemies remain inside the site's walkable area.

## Conversation structure

The shared quest node no longer says “For [title], our current task is [title].” It uses the authored scene. Long passages advance through **Continue** before showing response choices. The player can leave during narration. Continuing has no quest effect or approval reward; acceptance, testimony, decisions and reports remain explicit actions. Returning from a question goes directly to the response menu.

The 16 weekly request scenarios now identify an incident, a source, personal stakes and a concrete commitment. Directions describe the actual generated destination relative to a settlement. A charcoal carrier reports the trapped traveler; she has seen the wolf, while the giver has only heard her account. Farmer Joss promised hay because his daughter uses the patrol road, but his broken cart prevents collection. His field-spirit custom explains his view of responsibility. See the [generated local conversations](local-quest-conversations.md).

An actual job supplies the resident's work introduction; regional beliefs have optional follow-ups. Weekly follow-ups from an authored quest giver acknowledge the completed request. The four grain-theft advice options distinguish informing households, withholding an identity while checking the account, proposing repayment and seeking a hearing. None claims those future actions have already happened.

## Companions and the main cast

All nine principal companions and eleven main-story quest givers have authored introductions, public backgrounds and reasons to ask an unfamiliar player for help. The initial quest conversation presents that background before its task. The [character transcript](npc-background-conversations.md) records the implemented wording.

Companions do not begin by presuming friendship. Their more personal background topic requires 20 relationship or prior work completed together; reading it grants neither approval nor evidence. Existing recruitment, romance and quest-choice requirements remain separate. Background topics remain available to reread after leaving or loading a save.

Within a quest, the next scene recalls the preceding observation only when its stage ID is recorded. Between quests from the same giver, the runtime supplies the completed predecessor's report before the new offer and offers a recap question. An unfinished predecessor cannot supply this history. Maelis treats the player as a survivor she is sheltering; other authorities introduce their own responsibilities instead of assuming the player already knows them.

## Verification and boundaries

`StoryLocationTest` checks templates, recognizable props, unique reachable interaction points, retained dungeon transitions, non-overlapping compounds and deterministic regeneration across six seeds. `WorldGenerationTest` checks road crossings and destination access across five seeds. `CampaignPlacePreview` captures the actual site layouts, the world map and Joss's paged conversation.

`NpcQuestStoryTest` reads every weekly scenario across six regional contexts, checks narration effects and stale options, round-trips an unfinished weekly quest, and checks the twenty cast profiles and companion disclosure gates. The main-story and companion quest integration tests continue to check objective progression and saves; the companion dialogue export checks response paths.

Validation on Java 21: 7,036 location checks, 2,906 local-story/background checks, 1,683 main-story checks, 2,037 quest-narrative checks and 269 companion-segment checks pass. The companion-segment test reads the new introduction pages before selecting an outcome and verifies that reading cannot commit a decision. The companion dialogue export reports zero findings; world generation passes its five-seed crossing/access suite. The broader `SmokeTest` still stops at the pre-existing Snowrest village-building road connectivity assertion (`Connected=6 buildings=8`), so this pass does not claim a clean full-game smoke run.

This does not migrate every later companion or authored side quest to a fixed location. The [remaining location audit](quest-location-audit.md) still identifies the numbered bindings requiring individual migration. Older optional relationship branches and later quest prose also need individual editorial review; the new introductions do not make every existing branch a finished rewrite.
