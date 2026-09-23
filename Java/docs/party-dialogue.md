# Party conversations and shared expeditions

Implemented 23 September 2026 in `PartyDialogue.java` and the existing travel speech bubbles.

All 36 pairings of the nine named companions have an authored conversation. These play during overworld travel, one speaker at a time, with **Listen** and **Continue traveling** controls. A pair must be traveling with the player; stationed companions cannot speak. Each scene has a cooldown, and leaving a conversation has no relationship penalty. Offers expire without accepting anything. Individual companion reactions remain available between pair scenes.

Companion quest acceptance and completion can also queue a conversation between the quest owner and a present partner. These comments acknowledge the actual event without inventing findings, knowledge of private history, or completed future actions. Queued scenes recheck party membership before playing.

Five pairs can propose an optional shared expedition after their ordinary travel conversation:

| Companions | Quest | Shared concern |
| --- | --- | --- |
| Cassia and Lyra | Room to Breathe | Holding space for care and being willing to retreat. |
| Maera and Samir | Faith under Pressure | Letting another person question one's judgment. |
| Aria and Vesper | A Way Back Out | Watching the retreat route and avoiding unnecessary fights. |
| Seraphine and Rafiq | No Convenient Exit | Staying with a partner despite fear. |
| Calder and Cassia | Hold Together | Accepting help rather than holding alone. |

The final offer explicitly allows acceptance or **Not now**. Accepted expeditions appear in the normal quest journal. Each requires victory over one actual dungeon boss with both named companions in the battle roster. Any dungeon boss qualifies; ordinary enemies, unrelated quest encounters, fleeing, and victories without the required pair do not count. An incapacitated companion who participated still counts. These are cooperative expeditions, not new resolutions of personal quest arcs or new boss mechanics.

Completion automatically awards 60 gold and 80 player XP once, records a memory for both companions, and queues a three-turn aftermath exchange. Accepted/completed status and memories use the existing save system. Older saves begin with the new expeditions unaccepted. Transient conversations and cooldowns reset on loading; no offer accepts itself on reload.

The journal describes the party requirement and automatic completion rather than inventing a quest giver or map marker. The conversation bubble allows five text rows for the longer pair dialogue.

Validation: compile all sources, then run from `Java/`:

```text
java -cp out-party-dialogue com.alderfall.game.PartyDialogueTest
java -cp out-party-dialogue com.alderfall.game.QuestNarrativeTest
java -cp out-party-dialogue com.alderfall.game.CompanionQuestSegmentTest
```

The party suite checks all pairings, both directions of quest commentary, absent/stationed speakers, dialogue turns, acceptance/decline, cooldowns, encounter attribution, repeat rewards, memories, and save round trips. It also uses a generated dungeon boss and the real battle-result and travel-reply hooks. It sets the battle outcome directly; it does not automate tactical combat or visually inspect the rendered UI.
