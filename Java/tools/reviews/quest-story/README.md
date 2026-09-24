# Story Workshop

Open `index.html` directly or select **Quest, character & dialogue editor** in
the Preview Workshop. The page uses browser `localStorage`; it needs no server or
dependencies. Use **Export JSON** to save/share a draft and **Import JSON** to
continue editing that export in another browser. The exported `quests` and
`characters` collections are a planning format, not runtime game data.

`catalog.js` contains a catalog snapshot read from the Java runtime content tables:
all 137 quests in `GameData.QUESTS` plus five dynamically installed pair expedition
quests (142 total), and 94 named NPC/recruit characters. Quest stage records carry
the current stage title, objective instructions, offer/progress/ready/completion
dialogue, objective metadata, chain ownership and follow-up links. Quest chains
group the main campaign, companion storylines, quest-giver side stories and the
five pair expeditions. The chain name can be edited per quest to create custom
groups. Each character has their current NPC dialogue or a source-based profile;
quest and companion links are shown in that character's editor. The character
editor includes a read-only tree of current `DialogueLibrary` choices and
responses (up to three choice levels) and an editable response-block board. Copy
the runtime branches into blocks, add standalone/parallel/adjacent blocks, and
connect any two blocks. Each block supports response text, consequences, quest
stage triggers, objectives and a quest link; each connection supports an option
label, layout and consequence. These authored graph changes export as planning
data and do not alter Java runtime behavior automatically.

On open, browser-saved drafts are upgraded by merging in missing catalog records
and source content. Existing draft-only records, notes and stage state-change notes
are retained. The **Load / merge full game catalog** button repeats that merge;
**Restore full game catalog** replaces local edits after a warning.

This is a content snapshot and summary, not a substitute for source review. Eight
generic recruitable companions have no authored standalone dialogue profile in
the source; their personality/story fields describe their existing recruitment
and combat data, while the runtime builds their conversations dynamically. Before
implementing edits, inspect the source of truth:

- Main quest definitions and objective/dialogue refinements:
  `src/main/java/com/alderfall/game/content/MainStoryContent.java` and
  `QuestNarrative.java`.
- Companion stories: `src/main/java/com/alderfall/game/content/CompanionQuestContent.java`
  and the character files under `src/main/java/com/alderfall/game/dialogues/`.
- Side quests and assembly: `src/main/java/com/alderfall/game/content/Quest.java`,
  `NpcQuestStories.java`, and `FurnitureQuestContent.java`.
- Named character/recruit inventory: `GameData.NPCS` and `GameData.RECRUITS`.
- Pair expedition dialogue and dynamic shared quests: `PartyDialogue.java`.

Each stage accepts an objective, dialogue summary, intended state change and
conditions. Character records accept personality, voice and story guidance, plus
conversation scenes with lines, player choices, replies, next-node references
and consequences. Notes and drafts persist locally until browser data is cleared;
export JSON for durable storage and handoff. This tool does not change runtime
dialogue or quest behavior.
