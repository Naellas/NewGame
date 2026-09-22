# Companion Quest Relevance Rubric

Date: 2026-06-19

This note is for writing companion quest dialogue so quests feel like shared stakes between player and companion, not random requests.

## Core Rule

Every companion quest offer should answer these across the conversation:

- Why now?
- Why you?
- What happens if we ignore it?
- Where do we start?

If all four answers are not present, the quest will usually feel detached from the player.

## Single-Prompt Budget

Do not try to fit the whole quest pitch into the first speech bubble.

Use this budget:

- Root offer line: 1 concrete hook sentence.
- Clarify follow-up: the factual situation.
- Personal follow-up: why it matters to the companion and why they trust the player.
- Warning follow-up: the cost of delay, failure, or misreading the evidence.
- Practical follow-up: the first actionable step.

The current companion dialogue panel only comfortably holds a few wrapped lines. Writing should assume short, high-signal beats rather than one long monologue.

## Recommended Offer Structure

Use this pattern for `QuestDialogueStage.OFFER`:

1. Root:
   One concrete sentence that names the wound, clue, threat, or contradiction.
2. Clarify:
   Explain what is physically happening.
3. Personal:
   Explain why the companion cares and why the player is involved.
4. Warning:
   Explain what gets worse if the player waits or misunderstands the situation.
5. Practical:
   Name the first place, person, clue, or fight.
6. Commit:
   Let the player accept with enough context to feel ownership.

## Writing Pattern

Prefer:

```text
Concrete fact. Consequence. Character meaning.
```

Example:

```text
The ledger still charges dead families. That means someone living is collecting in their name. I need you because you read the numbers before deciding who deserves fire.
```

Avoid:

```text
The river remembers old cruelties.
```

That line may work later as flavor, but not as the main quest hook.

## Relevance Checks

Before shipping a companion quest stage, check:

- Does the companion have a personal stake beyond generic concern?
- Does the player have a specific role beyond "helping"?
- Is there visible evidence in the world?
- Is there pressure if the player delays?
- Does the quest change a relationship, belief, settlement role, road, or future conversation?

If the answer is "no" to more than one of these, the stage probably needs rewriting.

## Preferred Player Role

The player should usually be one of these:

- Witness
- Protector
- Investigator
- Moral counterweight
- Tactical partner
- Trusted pair of hands

Try not to make the player just a courier unless the courier role is itself emotionally or politically meaningful.

## Presentation Recommendation

Default to staged prompts before adding timed text reveal.

Why:

- Staged prompts improve clarity and agency immediately.
- They work with the current conversation tree.
- They scale better than longer root monologues.

Timed reveal can still help later, but it should be used to pace short beats, not to hide oversized paragraphs.
