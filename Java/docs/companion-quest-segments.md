# Companion care, witnesses, and deliveries

Second narrative implementation pass, 22 September 2026. Source: [Narrative Rewrite Brief](../../Story%20premise/narrative-rewrite-brief.md). This is implemented content, with the boundaries below.

## Playable segments

| Quest | Actions and story connection | What completion establishes |
| --- | --- | --- |
| Seraphine, `seraphine_chain_2` | Collect three renewed Thorn Hall invitations in Riverside; hear former servant Iven's account of the original one-winter promise; set a refuge guest charm; meet Iven at the refuge. | Iven confirms arrival and temporary shelter. His binding service remains unresolved. Seraphine receives the final report. |
| Aria, `aria_chain_2` | Collect three cut ribbons in Oakhaven; ask charcoal burner Bren what he saw at the dusk hedge path; set a counterknot at the kiln refuge; confirm Bren's arrival. | A witness is sheltered. He saw a ribbon and a traveler, not her face: this does not prove Aria's sister's location or wishes. |
| Lyra, `lyra_chain_3` | Ask Eda about her symptoms and obtain consent; collect a sealed care packet; cut and contain the cot's suspect bell-rope; apply the dressing and water; hear Eda's account of the shipment; agree on the next response with Lyra. | Eda has received care and is resting. The suspect rope is contained. Her observation of a supplier's seal is a lead, not proof of intent or a complete explanation of the haunting. |
| Rafiq, `rafiq_chain_2` | Collect three filled skins and two herb bundles in Dunewick; hand them to Elder Safa; hear her account of the figure in the dueling mirror; choose Rafiq's next commitment. | The household received supplies. Safa describes the discrepancy she witnessed. Rafiq's flight and responsibility remain part of the story. |
| Lyra, `lyra_chain_6` | Collect three sealed bundles in Belltower; defeat the three marked supply-route raiders; deliver the bundles to attendant Sen in Mireford; plan the clinic's next visits with Lyra. | The supplies reached the clinic. Planned visits, flood protection, and bell repairs have not happened simply because a route was agreed. |

The former escort stages are now explicit refuge arrangements: Iven and Bren agree to travel independently after the player prepares the meeting place. The player must find them and hear their arrival confirmation. This pass does not simulate a following traveler or escort survival combat, and the dialogue does not promise either.

These chapters do not require hiring the companion, passing persuasion, or achieving a relationship threshold. Essential testimony comes from its witness. Distinct purpose, action, findings, and uncertainty topics remain available throughout the quest.

## Supplies and completion

Supplies are quest-bound cargo, shown in quest instructions and the journal. A marked collection adds a precise quantity once. A validated handover checks the whole requested bundle before consuming anything; wrong recipients, missing quantities, stale dialogue, and repeated actions do not advance it. Eda's treatment additionally requires the contaminated rope to be contained.

Cargo is separate from sellable player inventory. Ordinary water or medicine does not silently substitute for these sealed or assigned supplies. This prevents a shop transaction from destroying the only route through a required handover.

Final rewards and companion report reactions require the quest giver. A witness can confirm their experience but cannot award the companion's reward. Completing a decision preserves the specific reply the companion gives to the selected option, even when the underlying quest conversation refreshes. After testimony or handover, the relevant NPC remains in the settlement with a saved-outcome response: Iven, Bren, Eda, Safa, or Sen. Loading reconstructs these contacts from established facts without creating duplicate NPCs or changing their appearance.

## Save migration

The five quests use content revision 2 and new action-stage IDs. Existing quest IDs, reward amounts, chain links, and decision keys remain stable.

- Unfinished older versions remain accepted but restart this chapter's revised sequence. The load notice names the affected chapter. Old kill/visit counts do not become new testimony, treatment, or deliveries.
- Completed older chapters and earned rewards remain completed. Detailed new observations are not inferred from their old counters.
- Existing decision values survive. When a revised chapter reaches its decision, the player can retain that recorded commitment, including a legacy value no longer offered as a fresh option. This does not replay the decision's relationship or memory effects.
- New saves preserve supply quantities, consumed handovers, stage IDs, observed facts, and outcomes. Repeated loads neither restore delivered supplies nor duplicate rewards.

## Verification and review

Compile with Java 21, then run from `Java/`:

```text
java -cp out-story-refinement com.alderfall.game.CompanionQuestSegmentTest
java -cp out-story-refinement com.alderfall.game.QuestNarrativeTest
java -cp out-story-refinement com.alderfall.game.CompanionDialogueQaExport out-story-refinement/dialogue-qa.md
```

The segment suite passes 257 checks. It uses public world interactions for collections and preparations, validates actual quest contacts for exchanges, and checks every revised chapter through completion. It exercises partial-cargo saves, missing supplies, wrong recipients, treatment prerequisites, stale actions, retained legacy choices, final report ownership, persistent aftermath, and duplicate rewards. Decisions are also exercised through the actual dialogue menu to verify that the selected reply remains visible. Combat checks exercise encounter attribution and progress; they do not automate tactical battle play. The broader narrative regression suite passes 2,017 checks.

It also writes `out-story-refinement/companion-segments.md`, a readable record of each chapter's instructions, discoveries, chosen reply, final report, and response after reloading. The broader structural dialogue export reports zero findings. The wider smoke suite is currently blocked by Dunewick's generated building-road connectivity assertion; see the [implementation checkpoint](quest-narrative-implementation.md).

## Boundaries and follow-through

This is a five-chapter content pass, not a complete rewrite of every companion arc. Thorn Hall's later confrontation, Aria's sister's eventual decision, Rafiq's mirror encounter, and the drowned infirmary still require later implementations. Most surrounding chapter text remains the earlier catalog and will need to be reconciled as those segments are rewritten.

The new local evidence and witness anchors are in the named settlements. Lyra's supply-route combat retains the existing marked raider encounter. No general escort AI, timed patient deterioration, medical simulation, new boss mechanics, or eastern campaign migration was introduced.
