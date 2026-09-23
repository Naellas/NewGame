# Main story: the ward network and Oathstead

Implementation checkpoint: 22 September 2026. This pass applies the [story premise](../../Story%20premise/world-framework.md#14-revised-central-premise) and [narrative rewrite brief](../../Story%20premise/narrative-rewrite-brief.md) to all 26 current main quests. It also changes four quests' objective sequences. The [runtime dialogue transcript](main-story-dialogue.md) contains the actual offers, actions, findings, reports, and optional exchanges.

The subsequent comprehension review adds a [35-place runtime location library](../../Story%20premise/location-library.md). All 19 outdoor main quests now bind to 18 named sites rather than generic camp, field, or graveyard indices. Their labels persist without an active quest. See [the clarity review](../../Story%20premise/dialogue-clarity-and-place-review.md) for the specific problems corrected and the first-use explanations required in future writing.

## Dramatic throughline

The shrine attack gives the player a practical reason to investigate: Oathstead sheltered them, and its protection may share the shrine's weakness. Maelis does not know Vaelthara's private reasons for leaving a survivor. Selene connects the surviving carving to the camp's ward design, then directs the search for missing instructions and Memory.

Regional journeys explain why twelve different stones matter. Iron holds boundaries; Ember transfers power; Tides governs crossings; Hunger absorbs excess force; Roots distributes protection through living ground; Graves recognizes the end of service; Frost buys time; Bells coordinates warnings; Ash releases spent power; Oaths recognizes voluntary commitments; Dawn enables the attempt to restart the whole system. These are their narrative functions. This pass does not add twelve new combat abilities.

The northern and southern investigations now demonstrate a specific abuse of the network: an obligation with an ending was changed into service that only a commander or keeper could end. That gives the player evidence to bring into conversations about Oathstead and Vaelthara. It does not make every historical defender guilty or prove that every local threat serves her.

Morvane's raid threatens the place that took the player in. The mainland gathering asks actual representatives for limited commitments rather than treating the regions as conquered assets. At the Gate, Vaelthara offers food and protection while demanding the stones and the right to decide when Oathstead's people may leave her protection. This is an antagonist's demand at the start of the existing battle, not a selectable surrender branch. Maelis no longer speaks from the battle scene while claiming elsewhere to remain at Oathstead.

## Changed objective contracts

| Quest | Playable sequence | What completion establishes |
| --- | --- | --- |
| Watchtower Without Bells | Inspect the obstructed clapper → read the cairn watch names → compare the copied signal → report to Odrick. | Banner cloth obstructed the bell; its household name matches an oath ending at spring thaw; the watch signal was copied beside a raider mark. The culprit's orders remain unproven. |
| The Stolen Watch Oath (`ms_frosthollow_standard`) | Defeat Kharvok at Banner Cairn → examine his fallen standard → report to Odrick for Iron. | Added stitching covers the Fenrik household's oath ending and makes release depend on Kharvok. Killing him alone does not read the standard or finish the burial keepers' work. |
| Shrine Without Shadow | Inspect the guest cup → read the altered welcome → examine the plugged outlet → report to Solari. | The old welcome allowed departure at moonset. A later command and iron plug converted hospitality into confinement. The survey does not identify who ordered it. |
| The Ember Socket Rite | Inspect the transfer cradle → lift Ember clear → open the guest's outlet → report to Solari for Ember. | The draw stops before the outlet opens. A flame remains captive between those actions; only the final interaction releases this guest. The vessel is then empty and cool. Other shrines remain unfinished work. |

These are sequential world interactions at named campaign sites. Each has a stable stage ID and a separate recorded result. Only the currently active stage's object can execute. The sites have persistent landmarks and existing prop art; bespoke northern cairn and southern temple interiors remain future work. Kharvok's main encounter and standard inspection now belong to Banner Cairn north of Highwall, rather than the southern Frosthollow cave. Sunken Shrine Forge is the workshop behind the investigated shrine, connected to the same guest vessel.

Solari explicitly authorizes the release and accepts responsibility for finding willing replacement sources. Opening the vessel changes the recorded quest state and removes the completed interaction. It does not install repaired wards, summon a persistent spirit actor, change settlement resource production, or simulate a new source of heat. Those systems must precede any stronger aftermath claims.

## Conversation behavior

All 26 quests have specific openings, motives, limits, and reports. Optional questions address the player's survival, the camp, local beliefs, each stone's function, or the immediate task. Follow-ups answer the selected question. Reading a question neither accepts a quest nor registers a coalition commitment.

Observed discoveries unlock additional questions. For example:

> Player: Someone changed 'until moonset' to 'until released'.
>
> Solari: Then someone changed the terms of the welcome. That is written into this vessel. We still need to examine whether the guest had a way out.
>
> Player: Does your temple teach that command?
>
> Solari: I was taught that the fire is a guest. If our keepers used this altered command, they betrayed the rite I learned. I need to find who used it and when.

After disconnecting Ember, the player can ask whether the guest is free. Solari says the flame is still inside and directs them to open the outlet. After release, the question changes to the cost of losing the vessel's heat. His uncertainty response acknowledges that this guest left instead of reverting to speculation about whether it was confined.

The shared findings and journal views retain earlier observations. Generic main-NPC work, personality, and local branches no longer compete with the authored campaign conversation. Eleven main speakers receive everyday conversations about food, work, hospitality, names, and home, which are safe before a chapter unlocks. Their ambient greetings have also been rewritten; Selene no longer announces an unsupported secret history before the investigation.

## Saves and rewards

All 19 relocated outdoor main quests use content revision 4, including the four restructured objective sequences. Unfinished older versions restart their evidence sequence while retaining acceptance; loading identifies the quests that need revisiting. The revised Kharvok battle has a new stable stage ID so an older handled encounter cannot consume its new objective.

Completed older quests remain completed, and their stones remain earned. They do not gain fabricated observations of the new standard or released guest. Their review dialogue states that the earlier completion is recorded but detailed findings are unavailable. Compatible saves of the new sequences preserve every observation, including the important interval between disconnecting Ember and opening the outlet.

The seven main quests outside those outdoor bindings retain their prior objective revisions and progress. Every existing stone reward remains with its quest, including Tides in the Fenlands. Final reports still own rewards; optional questions and repeated interactions cannot award them. No persuasion success, companion recruitment, or romance is required.

## Verification

Compile with the installed Java 21 toolchain. Run:

```text
java -cp out-story-refinement com.alderfall.game.MainStoryDialogueTest
java -cp out-story-refinement com.alderfall.game.QuestNarrativeTest
java -cp out-story-refinement com.alderfall.game.CompanionQuestSegmentTest
java -cp out-story-refinement com.alderfall.game.StoryLocationTest
```

The main-story suite checks authored branches across every main quest stage, evidence-gated questions, zero-effect information branches, correct coalition speakers, the actual Maelis dialogue menu, four revised sequences through world interactions, stale interactions, save/load between actions, old completion handling, and reward replay. Combat results are driven through the existing encounter/progress hooks; this is not an automated tactical battle playthrough. Passing `docs/main-story-dialogue.md` as its argument regenerates the checked-in transcript from runtime content.

## Remaining targets

The eastern voyage, Tides migration, dedicated folklore boss replacements, full temple/dungeon layouts, post-raid war-brazier conversation, and selectable network endings remain target work. Vaelthara's history as a bound power remains author-facing truth; the player no longer asks Damar about it before discovering it. Western and Fenland main objectives now have named destinations but still need deeper action revisions beyond their current survey, gather, and combat contracts. Later companion and side-quest site bindings require their own location review.
