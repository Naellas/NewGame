# Companion Dialogue QA

Generated: 2026-06-17 02:11

This document samples companion dialogue roots and representative branches so dialogue can be reviewed as conversation, not as disconnected menu text.

Use it to look for repeated labels, vague roots, NPC replies that ignore the player's line, missing quest-stage options, and trust states that do not feel distinct.

## QA Findings Summary

- Total findings: 0
- All sampled companion paths passed the structural QA checks.

## Aria Foxglove

- Recruit id: `aria`
- Starting quest: `Too-Neat Tracks`
- Quest stages: 3

### QA Findings

_No QA findings detected in sampled paths._

### Trust Snapshot Roots

#### Trust 0

- NPC says: An ambush site has tracks too neat to be real. I know bait when it puts on my sister's ribbon.
- Options:
  1. Can we talk about guarded respect?
  2. Tell me about Too-Neat Tracks.
  3. Can we talk, Aria?
  4. What should I avoid answering after dusk?

#### Trust 20

- NPC says: An ambush site has tracks too neat to be real. I know bait when it puts on my sister's ribbon.
- Options:
  1. Can we talk about guarded respect?
  2. Tell me about Too-Neat Tracks.
  3. Can we talk, Aria?
  4. What does home mean to you now?
  5. What should I avoid answering after dusk?

#### Trust 50

- NPC says: The trail is answering. Do not trust it just because it speaks.
- Options:
  1. Can we talk about what changed between us?
  2. Where do things stand with Too-Neat Tracks?
  3. Can we talk, Aria?
  4. What does home mean to you now?
  5. What should I avoid answering after dusk?

#### Trust 100

- NPC says: The roadwatch gives me roads to watch and reasons to come back from them.
- Options:
  1. Can we talk about loyalty?
  2. I found what you needed.
  3. Can we talk, Aria?
  4. How does Oathstead feel to you?
  5. What should I avoid answering after dusk?

#### Trust 150

- NPC says: The roadwatch gives me roads to watch and reasons to come back from them.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Where do things stand with Too-Neat Tracks?
  3. Can we talk, Aria?
  4. How does Oathstead feel to you?
  5. What should I avoid answering after dusk?

#### Trust 180

- NPC says: You found me before I could decide whether I wanted finding. That is becoming familiar.
- Options:
  1. Can we talk about the future?
  2. Where do things stand with Too-Neat Tracks?
  3. Can we talk, Aria?
  4. Stay with me a while.
  5. What should I avoid answering after dusk?

#### Trust 250

- NPC says: There you are. I left the door in my mind open and pretended it was strategy.
- Options:
  1. What changed after Too-Neat Tracks?
  2. Can we talk, Aria?
  3. Stay with me a while.
  4. What should I avoid answering after dusk?

### Quest Stage Branches

#### Too-Neat Tracks / offer / stage 1 of 3

- Stage title: `Read The Marked Briar`
- Objective: `SEARCH` Marked Briar Snare (0/2)

#### Too-Neat Tracks -> What truth are you afraid -> I know the road may be bait

- NPC says: The roadwatch gives me roads to watch and reasons to come back from them.
- Options:
  1. Can we talk about loyalty?
  2. Tell me about Too-Neat Tracks.
  3. Can we talk, Aria?
  4. How does Oathstead feel to you?
  5. What should I avoid answering after dusk?
- Player: Tell me about Too-Neat Tracks.
- NPC narration: Aria reads the road in her head before she gives you the part that matters.
- NPC says: That snare is too tidy. Real panic tangles. This was arranged for us to admire.
- Options:
  1. What truth are you afraid is hidden there?
  2. What happened to your sister?
  3. What am I looking for, exactly?
  4. I know the road may be bait. I am still coming.
  5. Give me the version I can act on.
  6. Back to Aria
- Player: What truth are you afraid is hidden there?
- NPC narration: Aria reads the road in her head before she gives you the part that matters.
- NPC says: This is a road problem first. Road problems kill quietly and call it accident. For now: find 2 Marked Briar Snare at the Farmland near Alderfall.
- Options:
  1. And what does that cost you?
  2. What am I looking for, exactly?
  3. I know the road may be bait. I am still coming.
  4. Back to Too-Neat Tracks
- Player: I know the road may be bait. I am still coming.
- NPC narration: Aria gives you her attention without pretending attention is easy.
- NPC says: Good. The road may be bait, so read it twice. For now: find 2 Marked Briar Snare at the Farmland near Alderfall.
- Options:
  1. Back to Aria

#### Too-Neat Tracks / accepted / stage 1 of 3

- Stage title: `Read The Marked Briar`
- Objective: `SEARCH` Marked Briar Snare (1/2)

#### Where do things stand -> Where do I start looking

- NPC says: The roadwatch gives me roads to watch and reasons to come back from them.
- Options:
  1. Can we talk about loyalty?
  2. Where do things stand with Too-Neat Tracks?
  3. Can we talk, Aria?
  4. How does Oathstead feel to you?
  5. What should I avoid answering after dusk?
- Player: Where do things stand with Too-Neat Tracks?
- NPC narration: Aria reads the road in her head before she gives you the part that matters.
- NPC says: Search the marked briars at the north road. Touch nothing until you know what wanted touching. The second sign matters more than the first; patterns do not lie as easily.
- Options:
  1. What piece is still missing?
  2. Where do I start looking?
  3. What happened to your sister?
  4. What would someone hide in plain sight?
  5. Back to Aria
- Player: Where do I start looking?
- NPC narration: Aria's answer turns spare and useful, the way trail signs are useful.
- NPC says: Start at the Farmland near Alderfall. Find 2 Marked Briar Snare.
- Options:
  1. I know where to start.
  2. What would someone hide in plain sight?
  3. Back to Too-Neat Tracks

#### Too-Neat Tracks / ready / stage 1 of 3

- Stage title: `Read The Marked Briar`
- Objective: `SEARCH` Marked Briar Snare (2/2)

#### I found what you needed

- NPC says: The roadwatch gives me roads to watch and reasons to come back from them.
- Options:
  1. Can we talk about loyalty?
  2. I found what you needed.
  3. Can we talk, Aria?
  4. How does Oathstead feel to you?
  5. What should I avoid answering after dusk?
- Player: I found what you needed.
- NPC narration: Aria reads the road in her head before she gives you the part that matters.
- NPC says: The thorn knots point one way, the drag marks another. The road is being taught to lie. Show me the mark. Slowly. I want to know which trail it opens.
- Options:
  1. I found what someone tried to hide.
  2. What does this prove?
  3. What does it cost you to hear this?
  4. What did the answer disturb?
  5. Back to Aria

#### Too-Neat Tracks / accepted / stage 2 of 3

- Stage title: `Question The Bootprint`
- Objective: `SEARCH` False Bootprint (0/1)

#### Where do things stand -> Where do I start looking

- NPC says: The roadwatch gives me roads to watch and reasons to come back from them.
- Options:
  1. Can we talk about loyalty?
  2. Where do things stand with Too-Neat Tracks?
  3. Can we talk, Aria?
  4. How does Oathstead feel to you?
  5. What should I avoid answering after dusk?
- Player: Where do things stand with Too-Neat Tracks?
- NPC narration: Aria reads the road in her head before she gives you the part that matters.
- NPC says: An ambush site has tracks too neat to be real. I know bait when it puts on my sister's ribbon.
- Options:
  1. What truth are you afraid is hidden there?
  2. Where do I start looking?
  3. What happened to your sister?
  4. What would someone hide in plain sight?
  5. Back to Aria
- Player: Where do I start looking?
- NPC narration: Aria's answer turns spare and useful, the way trail signs are useful.
- NPC says: Start at the Farmland near Alderfall. Find False Bootprint.
- Options:
  1. I know where to start.
  2. What would someone hide in plain sight?
  3. Back to Too-Neat Tracks

#### Too-Neat Tracks / completed / stage 3 of 3

- Stage title: `Lift The Cut Ribbon`
- Objective: `GATHER` Cut Foxglove Ribbon (1/1)

#### What changed after -> What is still unresolved

- NPC says: The roadwatch gives me roads to watch and reasons to come back from them.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. What changed after Too-Neat Tracks?
  3. Can we talk, Aria?
  4. How does Oathstead feel to you?
  5. What should I avoid answering after dusk?
- Player: What changed after Too-Neat Tracks?
- NPC narration: Aria reads the road in her head before she gives you the part that matters. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Now the trail has my attention, which is exactly what worries me. First trail cut free. Now I know the road was lying on purpose.
- Options:
  1. Who gets helped by this first?
  2. Was it enough for the people waiting?
  3. What is still unresolved?
  4. Back to Aria
- Player: What is still unresolved?
- NPC narration: Aria's answer turns spare and useful, the way trail signs are useful. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Do not follow the trail meant for us. Look for the mark that does not want attention.
- Options:
  1. Back to Aria

### Representative Conversation Paths

#### Can we talk, -> How are you holding up? -> How are you feeling?

- NPC says: The roadwatch gives me roads to watch and reasons to come back from them.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Aria?
  3. How does Oathstead feel to you?
  4. What should I avoid answering after dusk?
- Player: Can we talk, Aria?
- NPC says: Aria studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Aria
- Player: How are you holding up?
- NPC narration: Aria lets the silence stretch before deciding not to dodge the question.
- NPC says: I usually say yes before checking. You keep asking before I can turn the answer into a joke about roads.
- Options:
  1. How are you feeling?
  2. What should we do next?
  3. Do you need anything?
  4. Back to personal matters
- Player: How are you feeling?
- NPC narration: Aria lets the silence stretch before deciding not to dodge the question.
- NPC says: Like I have been walking too long and only just noticed I can stop.
- Options:
  1. I will not push.
  2. Tell me the honest version.
  3. We can keep moving.
  4. Back to personal matters

#### Can we talk, -> How do you see me?

- NPC says: The roadwatch gives me roads to watch and reasons to come back from them.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Aria?
  3. How does Oathstead feel to you?
  4. What should I avoid answering after dusk?
- Player: Can we talk, Aria?
- NPC says: Aria studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Aria
- Player: How do you see me?
- NPC narration: Aria measures the truth carefully, then leaves it where you can see it.
- NPC says: I am still deciding what your choices mean. The pattern matters more than one noble moment.
- Options:
  1. What do you trust in me?
  2. What worries you about me?
  3. What should I remember about you?
  4. Back to personal matters

#### Can we talk, -> Do you trust me?

- NPC says: The roadwatch gives me roads to watch and reasons to come back from them.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Can we talk, Aria?
  3. How does Oathstead feel to you?
  4. What should I avoid answering after dusk?
- Player: Can we talk, Aria?
- NPC says: Aria waits without rushing you. Trust has made the quiet less defensive.
- Options:
  1. Do you trust me?
  2. You keep making me look back.
  3. How are you holding up?
  4. How do you see me?
  5. Can we talk about the roadwatch choices?
  6. Do you remember what happened?
  7. What do you think of...?
  8. Back to Aria
- Player: Do you trust me?
- NPC narration: Aria measures the truth carefully, then leaves it where you can see it. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: I keep finding roads back to you before I admit I chose them.
- Options:
  1. Stand with me.
  2. You are free to choose your road.
  3. I am glad you are here.
  4. Good. I need your skills.

#### Can we talk, -> What do you think of -> Me, honestly

- NPC says: The roadwatch gives me roads to watch and reasons to come back from them.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Aria?
  3. How does Oathstead feel to you?
  4. What should I avoid answering after dusk?
- Player: Can we talk, Aria?
- NPC says: Aria studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Aria
- Player: What do you think of...?
- NPC narration: Aria measures the truth carefully, then leaves it where you can see it.
- NPC says: Choose whose name you bring into the space between you.
- Options:
  1. Me, honestly.
  2. Calder
  3. Cassia
  4. Lyra
  5. Maera
  6. Rafiq
  7. Samir
  8. Seraphine
  9. Vesper
  10. Back to personal matters
- Player: Me, honestly.
- NPC narration: Aria measures the truth carefully, then leaves it where you can see it.
- NPC says: You keep walking into trouble like it owes you rent. Annoying. Useful. Occasionally brave.
- Options:
  1. That is fair.
  2. You notice more than I thought.
  3. Back to personal matters

#### Can we talk, -> What do you think of -> Known companion

- NPC says: The roadwatch gives me roads to watch and reasons to come back from them.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Aria?
  3. How does Oathstead feel to you?
  4. What should I avoid answering after dusk?
- Player: Can we talk, Aria?
- NPC says: Aria studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Aria
- Player: What do you think of...?
- NPC narration: Aria measures the truth carefully, then leaves it where you can see it.
- NPC says: Choose whose name you bring into the space between you.
- Options:
  1. Me, honestly.
  2. Calder
  3. Cassia
  4. Lyra
  5. Maera
  6. Rafiq
  7. Samir
  8. Seraphine
  9. Vesper
  10. Back to personal matters
- Player: Calder
- NPC narration: Aria measures the truth carefully, then leaves it where you can see it.
- NPC says: Calder makes roads less likely to kill children. Hard to mock that.
- Options:
  1. Back to conversation

#### How does Oathstead feel -> How does Oathstead feel -> What does Oathstead need next

- NPC says: The roadwatch gives me roads to watch and reasons to come back from them.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Can we talk, Aria?
  3. How does Oathstead feel to you?
  4. What should I avoid answering after dusk?
- Player: How does Oathstead feel to you?
- NPC says: Aria speaks of Oathstead like a place that may yet learn how to hold people gently.
- Options:
  1. How does Oathstead feel to you?
  2. Back to Aria
- Player: How does Oathstead feel to you?
- NPC narration: Aria stays close enough that leaving would have to be a choice. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Staying is not a small word. I know every exit, so the answer matters. Oathstead has roads that come back. I keep noticing that, which is rude of the roads and probably important.
- Options:
  1. Does this place feel like yours?
  2. How is your work here?
  3. What does Oathstead need next?
  4. Back to home and rest
- Player: What does Oathstead need next?
- NPC narration: Aria's answer turns spare and useful, the way trail signs are useful. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Watchers on the roads, quiet signals, and someone checking the paths children use.
- Options:
  1. Back to Aria

#### Can we talk, -> Come here a moment. -> I missed being close to you.

- NPC says: You found me before I could decide whether I wanted finding. That is becoming familiar.
- Options:
  1. Can we talk about the future?
  2. Can we talk, Aria?
  3. Stay with me a while.
  4. What should I avoid answering after dusk?
- Player: Can we talk, Aria?
- NPC says: Aria lets the pause settle between you, careful and warm.
- Options:
  1. What is this between us?
  2. Stay with me here.
  3. Come here a moment.
  4. How are you holding up?
  5. How do you see me?
  6. Can we talk about the roadwatch choices?
  7. Do you remember what happened?
  8. What do you think of...?
  9. Back to Aria
- Player: Come here a moment.
- NPC narration: Aria stays close enough that leaving would have to be a choice. There is warmth in it now, not careless, but no longer disguised as tactics.
- NPC says: Close without tactics. Suspicious. Trouble, then. But the kind I might walk toward on purpose.
- Options:
  1. I missed being close to you.
  2. You make danger look like a place I might stay.
  3. Too much?
  4. Back to personal matters
- Player: I missed being close to you.
- NPC narration: Aria stays close enough that leaving would have to be a choice. There is warmth in it now, not careless, but no longer disguised as tactics.
- NPC says: Close without tactics. Suspicious. Near, but not trapping. You are learning.
- Options:
  1. I wanted you to know.
  2. Back to personal matters

#### Can we talk, -> Stay with me here.

- NPC says: You found me before I could decide whether I wanted finding. That is becoming familiar.
- Options:
  1. Can we talk about the future?
  2. Can we talk, Aria?
  3. Stay with me a while.
  4. What should I avoid answering after dusk?
- Player: Can we talk, Aria?
- NPC says: Aria lets the pause settle between you, careful and warm.
- Options:
  1. What is this between us?
  2. Stay with me here.
  3. Come here a moment.
  4. How are you holding up?
  5. How do you see me?
  6. Can we talk about the roadwatch choices?
  7. Do you remember what happened?
  8. What do you think of...?
  9. Back to Aria
- Player: Stay with me here.
- NPC narration: Aria stays close enough that leaving would have to be a choice. There is warmth in it now, not careless, but no longer disguised as tactics.
- NPC says: Close without tactics. Suspicious. the inn table near Oathstead's road is crowded enough to vanish in and quiet enough to stay. I am choosing the second.
- Options:
  1. Stay close enough to be trouble.
  2. I wanted time with you, not another task.
  3. Stay here with me.
  4. We can keep this gentle.
  5. Back to personal matters

#### Can we talk, -> Can we talk about our promise?

- NPC says: There you are. I left the door in my mind open and pretended it was strategy.
- Options:
  1. Can we talk, Aria?
  2. Stay with me a while.
  3. What should I avoid answering after dusk?
- Player: Can we talk, Aria?
- NPC says: Aria gives you the kind of attention that has learned your silences.
- Options:
  1. Can we talk about our promise?
  2. Stay with me here.
  3. Come here a moment.
  4. How are you holding up?
  5. How do you see me?
  6. Can we talk about the roadwatch choices?
  7. Do you remember what happened?
  8. What do you think of...?
  9. Back to Aria
- Player: Can we talk about our promise?
- NPC narration: Aria measures the truth carefully, then leaves it where you can see it. The answer comes with the ease of someone who has learned your silences.
- NPC says: I have slept under hedges and called it freedom. With you, a door sounds less like a trap.
- Options:
  1. Come home to me when you can.
  2. Tell me what still frightens you.
  3. The work comes first today.
  4. Back to topics

### Repeat-Aware Sample

#### Can we talk, -> How are you holding up? -> How are you feeling?

- NPC says: The roadwatch gives me roads to watch and reasons to come back from them.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Can we talk, Aria?
  3. How does Oathstead feel to you?
  4. What should I avoid answering after dusk?
- Player: Can we talk, Aria?
- NPC says: Aria waits without rushing you. Trust has made the quiet less defensive.
- Options:
  1. Do you trust me?
  2. You keep making me look back.
  3. How are you holding up?
  4. How do you see me?
  5. Can we talk about the roadwatch choices?
  6. Do you remember what happened?
  7. What do you think of...?
  8. Back to Aria
- Player: How are you holding up?
- NPC narration: Aria lets the silence stretch before deciding not to dodge the question. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: You ask like you intend to hear the answer. That still catches me off guard.
- Options:
  1. How are you feeling?
  2. What should we do next?
  3. Do you need anything?
  4. Back to personal matters
- Player: How are you feeling?
- NPC narration: Aria lets the silence stretch before deciding not to dodge the question. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Like I might stay if no one says the word too loudly.
- Options:
  1. I will not push.
  2. Tell me the honest version.
  3. We can keep moving.
  4. Back to personal matters


## Calder Reed

- Recruit id: `calder`
- Starting quest: `The Bridge That Complained`
- Quest stages: 4

### QA Findings

_No QA findings detected in sampled paths._

### Trust Snapshot Roots

#### Trust 0

- NPC says: Bridges fail in pieces first. A loose peg, a lazy inspection, a person pretending water is patient.
- Options:
  1. Can we talk about guarded respect?
  2. Tell me about The Bridge That Complained.
  3. Can we talk, Calder?
  4. What does the forest ask before it lets people pass?

#### Trust 20

- NPC says: Bridges fail in pieces first. A loose peg, a lazy inspection, a person pretending water is patient.
- Options:
  1. Can we talk about guarded respect?
  2. Tell me about The Bridge That Complained.
  3. Can we talk, Calder?
  4. What does home mean to you now?
  5. What does the forest ask before it lets people pass?

#### Trust 50

- NPC says: Bridges fail in pieces first. A loose peg, a lazy inspection, a person pretending water is patient.
- Options:
  1. Can we talk about what changed between us?
  2. Where do things stand with The Bridge That Complained?
  3. Can we talk, Calder?
  4. What does home mean to you now?
  5. What does the forest ask before it lets people pass?

#### Trust 100

- NPC says: The repair yard is uneven, overworked, and worth maintaining. Familiar virtues.
- Options:
  1. Can we talk about loyalty?
  2. I found what you needed.
  3. Can we talk, Calder?
  4. How does Oathstead feel to you?
  5. What does the forest ask before it lets people pass?

#### Trust 150

- NPC says: The repair yard is uneven, overworked, and worth maintaining. Familiar virtues.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Where do things stand with The Bridge That Complained?
  3. Can we talk, Calder?
  4. How does Oathstead feel to you?
  5. What does the forest ask before it lets people pass?

#### Trust 180

- NPC says: You are here. Good. The day was leaning oddly without you.
- Options:
  1. Can we talk about the future?
  2. Where do things stand with The Bridge That Complained?
  3. Can we talk, Calder?
  4. Stay with me a while.
  5. What does the forest ask before it lets people pass?

#### Trust 250

- NPC says: There you are. Roof held, table held, promise held. Good start.
- Options:
  1. What changed after The Bridge That Complained?
  2. Can we talk, Calder?
  3. Stay with me a while.
  4. What does the forest ask before it lets people pass?

### Quest Stage Branches

#### The Bridge That Complained / offer / stage 1 of 4

- Stage title: `Inspect The Cracked Beam`
- Objective: `SEARCH` Cracked Bridge Beam (0/1)

#### The Bridge That Complained -> What truth are you afraid -> I will show up with both hands

- NPC says: The repair yard is uneven, overworked, and worth maintaining. Familiar virtues.
- Options:
  1. Can we talk about loyalty?
  2. Tell me about The Bridge That Complained.
  3. Can we talk, Calder?
  4. How does Oathstead feel to you?
  5. What does the forest ask before it lets people pass?
- Player: Tell me about The Bridge That Complained.
- NPC narration: Calder sets the truth down carefully, like weight on tested timber.
- NPC says: Bridge is talking. Mostly insults. Means it is close to falling.
- Options:
  1. What truth are you afraid is hidden there?
  2. Who did the bridge take from you?
  3. What am I looking for, exactly?
  4. I will show up with both hands.
  5. Give me the version I can act on.
  6. Back to Calder
- Player: What truth are you afraid is hidden there?
- NPC narration: Calder sets the truth down carefully, like weight on tested timber.
- NPC says: Something failed before the collapse became visible. Good craft listens early. For now: find Cracked Bridge Beam at the Farmland near Alderfall.
- Options:
  1. And what does that cost you?
  2. What am I looking for, exactly?
  3. I will show up with both hands.
  4. Back to The Bridge That Complained
- Player: I will show up with both hands.
- NPC narration: Calder sets the answer down carefully, like weight on tested timber.
- NPC says: Good. Bring your hands, not just your agreement. For now: find Cracked Bridge Beam at the Farmland near Alderfall.
- Options:
  1. Back to Calder

#### The Bridge That Complained / accepted / stage 1 of 4

- Stage title: `Inspect The Cracked Beam`
- Objective: `SEARCH` Cracked Bridge Beam (0/1)

#### Where do things stand -> Where do I start looking

- NPC says: The repair yard is uneven, overworked, and worth maintaining. Familiar virtues.
- Options:
  1. Can we talk about loyalty?
  2. Where do things stand with The Bridge That Complained?
  3. Can we talk, Calder?
  4. How does Oathstead feel to you?
  5. What does the forest ask before it lets people pass?
- Player: Where do things stand with The Bridge That Complained?
- NPC narration: Calder sets the truth down carefully, like weight on tested timber.
- NPC says: Bridges fail in pieces first. A loose peg, a lazy inspection, a person pretending water is patient.
- Options:
  1. What truth are you afraid is hidden there?
  2. Where do I start looking?
  3. Who did the bridge take from you?
  4. What would someone hide in plain sight?
  5. Back to Calder
- Player: Where do I start looking?
- NPC narration: Calder's answer becomes a repair plan, plain enough to trust.
- NPC says: Start at the Farmland near Alderfall. Find Cracked Bridge Beam.
- Options:
  1. I know where to start.
  2. What would someone hide in plain sight?
  3. Back to The Bridge That Complained

#### The Bridge That Complained / ready / stage 1 of 4

- Stage title: `Inspect The Cracked Beam`
- Objective: `SEARCH` Cracked Bridge Beam (1/1)

#### I found what you needed

- NPC says: The repair yard is uneven, overworked, and worth maintaining. Familiar virtues.
- Options:
  1. Can we talk about loyalty?
  2. I found what you needed.
  3. Can we talk, Calder?
  4. How does Oathstead feel to you?
  5. What does the forest ask before it lets people pass?
- Player: I found what you needed.
- NPC narration: Calder sets the truth down carefully, like weight on tested timber.
- NPC says: Crack runs along old rot, then fresh strain. Bad repair on worse patience. Set it down. We test the weight before deciding what it means.
- Options:
  1. I found what someone tried to hide.
  2. What does this prove?
  3. What does it cost you to hear this?
  4. What did the answer disturb?
  5. Back to Calder

#### The Bridge That Complained / accepted / stage 2 of 4

- Stage title: `Check The Loose Footing`
- Objective: `SEARCH` Loose Stone Footing (0/1)

#### Where do things stand -> Where do I start looking

- NPC says: The repair yard is uneven, overworked, and worth maintaining. Familiar virtues.
- Options:
  1. Can we talk about loyalty?
  2. Where do things stand with The Bridge That Complained?
  3. Can we talk, Calder?
  4. How does Oathstead feel to you?
  5. What does the forest ask before it lets people pass?
- Player: Where do things stand with The Bridge That Complained?
- NPC narration: Calder sets the truth down carefully, like weight on tested timber.
- NPC says: Bridges fail in pieces first. A loose peg, a lazy inspection, a person pretending water is patient.
- Options:
  1. What truth are you afraid is hidden there?
  2. Where do I start looking?
  3. Who did the bridge take from you?
  4. What would someone hide in plain sight?
  5. Back to Calder
- Player: Where do I start looking?
- NPC narration: Calder's answer becomes a repair plan, plain enough to trust.
- NPC says: Start at the Farmland near Alderfall. Find Loose Stone Footing.
- Options:
  1. I know where to start.
  2. What would someone hide in plain sight?
  3. Back to The Bridge That Complained

#### The Bridge That Complained / completed / stage 4 of 4

- Stage title: `Choose What The Bridge Says`
- Objective: `CHOICE` Bridge Warning (1/1)

#### What changed after -> What is still unresolved

- NPC says: The repair yard is uneven, overworked, and worth maintaining. Familiar virtues.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. What changed after The Bridge That Complained?
  3. Can we talk, Calder?
  4. How does Oathstead feel to you?
  5. What does the forest ask before it lets people pass?
- Player: What changed after The Bridge That Complained?
- NPC narration: Calder sets the truth down carefully, like weight on tested timber. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Good. Now we fix before we explain. First crack found. The structure has begun telling the truth.
- Options:
  1. Who carries the consequence with me?
  2. Do you think I chose well?
  3. What is still unresolved?
  4. Back to Calder
- Player: What is still unresolved?
- NPC narration: Calder's answer becomes a repair plan, plain enough to trust. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Do not fix only the visible crack. Find where the weight is really shifting.
- Options:
  1. Back to Calder

### Representative Conversation Paths

#### Can we talk, -> How are you holding up? -> How are you feeling?

- NPC says: The repair yard is uneven, overworked, and worth maintaining. Familiar virtues.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Calder?
  3. How does Oathstead feel to you?
  4. What does the forest ask before it lets people pass?
- Player: Can we talk, Calder?
- NPC says: Calder studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Calder
- Player: How are you holding up?
- NPC narration: Calder sets the answer down carefully, like weight on tested timber.
- NPC says: You ask like a person checking the supports after rain. I respect that.
- Options:
  1. How are you feeling?
  2. What should we do next?
  3. Do you need anything?
  4. Back to personal matters
- Player: How are you feeling?
- NPC narration: Calder sets the answer down carefully, like weight on tested timber.
- NPC says: A little strained. Still holding.
- Options:
  1. I will not push.
  2. Tell me the honest version.
  3. We can keep moving.
  4. Back to personal matters

#### Can we talk, -> How do you see me?

- NPC says: The repair yard is uneven, overworked, and worth maintaining. Familiar virtues.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Calder?
  3. How does Oathstead feel to you?
  4. What does the forest ask before it lets people pass?
- Player: Can we talk, Calder?
- NPC says: Calder studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Calder
- Player: How do you see me?
- NPC narration: Calder inspects the question like a brace that has to hold.
- NPC says: I am still deciding what your choices mean. The pattern matters more than one noble moment.
- Options:
  1. What do you trust in me?
  2. What worries you about me?
  3. What should I remember about you?
  4. Back to personal matters

#### Can we talk, -> Do you trust me?

- NPC says: The repair yard is uneven, overworked, and worth maintaining. Familiar virtues.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Can we talk, Calder?
  3. How does Oathstead feel to you?
  4. What does the forest ask before it lets people pass?
- Player: Can we talk, Calder?
- NPC says: Calder waits without rushing you. Trust has made the quiet less defensive.
- Options:
  1. Do you trust me?
  2. You feel like somewhere solid.
  3. How are you holding up?
  4. How do you see me?
  5. Do you remember what happened?
  6. What do you think of...?
  7. Back to Calder
- Player: Do you trust me?
- NPC narration: Calder inspects the question like a brace that has to hold. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Trust is load-bearing. Best to inspect it honestly. I trust your weight on the beam. That is higher praise than it sounds.
- Options:
  1. Stand with me.
  2. You are free to choose your road.
  3. I am glad you are here.
  4. Good. I need your skills.

#### Can we talk, -> What do you think of -> Me, honestly

- NPC says: The repair yard is uneven, overworked, and worth maintaining. Familiar virtues.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Calder?
  3. How does Oathstead feel to you?
  4. What does the forest ask before it lets people pass?
- Player: Can we talk, Calder?
- NPC says: Calder studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Calder
- Player: What do you think of...?
- NPC narration: Calder inspects the question like a brace that has to hold.
- NPC says: Choose whose name you bring into the space between you.
- Options:
  1. Me, honestly.
  2. Aria
  3. Cassia
  4. Lyra
  5. Maera
  6. Rafiq
  7. Samir
  8. Seraphine
  9. Vesper
  10. Back to personal matters
- Player: Me, honestly.
- NPC narration: Calder inspects the question like a brace that has to hold.
- NPC says: You are not finished. Good. Finished things cannot be repaired.
- Options:
  1. That is fair.
  2. You notice more than I thought.
  3. Back to personal matters

#### Can we talk, -> What do you think of -> Known companion

- NPC says: The repair yard is uneven, overworked, and worth maintaining. Familiar virtues.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Calder?
  3. How does Oathstead feel to you?
  4. What does the forest ask before it lets people pass?
- Player: Can we talk, Calder?
- NPC says: Calder studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Calder
- Player: What do you think of...?
- NPC narration: Calder inspects the question like a brace that has to hold.
- NPC says: Choose whose name you bring into the space between you.
- Options:
  1. Me, honestly.
  2. Aria
  3. Cassia
  4. Lyra
  5. Maera
  6. Rafiq
  7. Samir
  8. Seraphine
  9. Vesper
  10. Back to personal matters
- Player: Aria
- NPC narration: Calder inspects the question like a brace that has to hold.
- NPC says: Aria sees bad footing before the rest of us find mud.
- Options:
  1. Back to conversation

#### How does Oathstead feel -> How does Oathstead feel -> What does Oathstead need next

- NPC says: The repair yard is uneven, overworked, and worth maintaining. Familiar virtues.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Can we talk, Calder?
  3. How does Oathstead feel to you?
  4. What does the forest ask before it lets people pass?
- Player: How does Oathstead feel to you?
- NPC says: Calder speaks of Oathstead like a place that may yet learn how to hold people gently.
- Options:
  1. How does Oathstead feel to you?
  2. Back to Calder
- Player: How does Oathstead feel to you?
- NPC narration: Calder's voice roughens around the feeling, but it does not move away. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: I thought a camp would feel temporary. This one keeps asking people to become less temporary with it.
- Options:
  1. Does this place feel like yours?
  2. How is your work here?
  3. What does Oathstead need next?
  4. Back to home and rest
- Player: What does Oathstead need next?
- NPC narration: Calder's answer becomes a repair plan, plain enough to trust. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Foundations, braces, nails, and people willing to maintain what they love.
- Options:
  1. Back to Calder

#### Can we talk, -> Come here a moment. -> I missed being close to you.

- NPC says: You are here. Good. The day was leaning oddly without you.
- Options:
  1. Can we talk about the future?
  2. Can we talk, Calder?
  3. Stay with me a while.
  4. What does the forest ask before it lets people pass?
- Player: Can we talk, Calder?
- NPC says: Calder lets the pause settle between you, careful and warm.
- Options:
  1. What is this between us?
  2. Stay with me here.
  3. Come here a moment.
  4. How are you holding up?
  5. How do you see me?
  6. Do you remember what happened?
  7. What do you think of...?
  8. Back to Calder
- Player: Come here a moment.
- NPC narration: Calder's voice roughens around the feeling, but it does not move away. There is warmth in it now, not careless, but no longer disguised as tactics.
- NPC says: That was not subtle. Good. Subtle things get missed in bad weather.
- Options:
  1. I missed being close to you.
  2. I keep thinking about what it would mean to come home to you.
  3. Too much?
  4. Back to personal matters
- Player: I missed being close to you.
- NPC narration: Calder's voice roughens around the feeling, but it does not move away. There is warmth in it now, not careless, but no longer disguised as tactics.
- NPC says: Near is honest. It puts weight where words cannot dodge it.
- Options:
  1. I wanted you to know.
  2. Back to personal matters

#### Can we talk, -> Stay with me here.

- NPC says: You are here. Good. The day was leaning oddly without you.
- Options:
  1. Can we talk about the future?
  2. Can we talk, Calder?
  3. Stay with me a while.
  4. What does the forest ask before it lets people pass?
- Player: Can we talk, Calder?
- NPC says: Calder lets the pause settle between you, careful and warm.
- Options:
  1. What is this between us?
  2. Stay with me here.
  3. Come here a moment.
  4. How are you holding up?
  5. How do you see me?
  6. Do you remember what happened?
  7. What do you think of...?
  8. Back to Calder
- Player: Stay with me here.
- NPC narration: Calder's voice roughens around the feeling, but it does not move away. There is warmth in it now, not careless, but no longer disguised as tactics.
- NPC says: the inn table near Oathstead's road has bad chairs and a sound roof. Good enough for truth if we sit carefully.
- Options:
  1. You are becoming part of the foundation.
  2. I wanted time with you, not another task.
  3. Stay here with me.
  4. We can keep this gentle.
  5. Back to personal matters

#### Can we talk, -> Can we talk about our promise?

- NPC says: There you are. Roof held, table held, promise held. Good start.
- Options:
  1. Can we talk, Calder?
  2. Stay with me a while.
  3. What does the forest ask before it lets people pass?
- Player: Can we talk, Calder?
- NPC says: Calder gives you the kind of attention that has learned your silences.
- Options:
  1. Can we talk about our promise?
  2. Stay with me here.
  3. Come here a moment.
  4. How are you holding up?
  5. How do you see me?
  6. Do you remember what happened?
  7. What do you think of...?
  8. Back to Calder
- Player: Can we talk about our promise?
- NPC narration: Calder inspects the question like a brace that has to hold. The answer comes with the ease of someone who has learned your silences.
- NPC says: Trust is load-bearing. Best to inspect it honestly. I trust stone because it tells you when it cracks. I trust you because you stayed long enough to hear it.
- Options:
  1. Come home to me when you can.
  2. Tell me what still frightens you.
  3. The work comes first today.
  4. Back to topics

### Repeat-Aware Sample

#### Can we talk, -> How are you holding up? -> How are you feeling?

- NPC says: The repair yard is uneven, overworked, and worth maintaining. Familiar virtues.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Can we talk, Calder?
  3. How does Oathstead feel to you?
  4. What does the forest ask before it lets people pass?
- Player: Can we talk, Calder?
- NPC says: Calder waits without rushing you. Trust has made the quiet less defensive.
- Options:
  1. Do you trust me?
  2. You feel like somewhere solid.
  3. How are you holding up?
  4. How do you see me?
  5. Do you remember what happened?
  6. What do you think of...?
  7. Back to Calder
- Player: How are you holding up?
- NPC narration: Calder sets the answer down carefully, like weight on tested timber. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: You ask like you intend to hear the answer. That still catches me off guard.
- Options:
  1. How are you feeling?
  2. What should we do next?
  3. Do you need anything?
  4. Back to personal matters
- Player: How are you feeling?
- NPC narration: Calder sets the answer down carefully, like weight on tested timber. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Sound enough. Some cracks. None spreading today.
- Options:
  1. I will not push.
  2. Tell me the honest version.
  3. We can keep moving.
  4. Back to personal matters


## Cassia Flint

- Recruit id: `cassia`
- Starting quest: `The Woman at the Gate`
- Quest stages: 4

### QA Findings

_No QA findings detected in sampled paths._

### Trust Snapshot Roots

#### Trust 0

- NPC says: The gate should have held. It did not. I have been arguing with that fact longer than is dignified.
- Options:
  1. Can we talk about guarded respect?
  2. Tell me about The Woman at the Gate.
  3. Can we talk, Cassia?
  4. What does the forest ask before it lets people pass?

#### Trust 20

- NPC says: The gate should have held. It did not. I have been arguing with that fact longer than is dignified.
- Options:
  1. Can we talk about guarded respect?
  2. Tell me about The Woman at the Gate.
  3. Can we talk, Cassia?
  4. What does home mean to you now?
  5. What does the forest ask before it lets people pass?

#### Trust 50

- NPC says: The gate should have held. It did not. I have been arguing with that fact longer than is dignified.
- Options:
  1. Can we talk about what changed between us?
  2. Where do things stand with The Woman at the Gate?
  3. Can we talk, Cassia?
  4. What does home mean to you now?
  5. What does the forest ask before it lets people pass?

#### Trust 100

- NPC says: The watch gate is not a wall, but people lean on it. That is enough for my attention.
- Options:
  1. Can we talk about loyalty?
  2. I found what you needed.
  3. Can we talk, Cassia?
  4. How does Oathstead feel to you?
  5. What does the forest ask before it lets people pass?

#### Trust 150

- NPC says: The watch gate is not a wall, but people lean on it. That is enough for my attention.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Where do things stand with The Woman at the Gate?
  3. Can we talk, Cassia?
  4. How does Oathstead feel to you?
  5. What does the forest ask before it lets people pass?

#### Trust 180

- NPC says: You are here. Good. I will pretend that did not change my breathing.
- Options:
  1. Can we talk about the future?
  2. Where do things stand with The Woman at the Gate?
  3. Can we talk, Cassia?
  4. Stay with me a while.
  5. What does the forest ask before it lets people pass?

#### Trust 250

- NPC says: There you are. The line holds better when I know where you stand.
- Options:
  1. What changed after The Woman at the Gate?
  2. Can we talk, Cassia?
  3. Stay with me a while.
  4. What does the forest ask before it lets people pass?

### Quest Stage Branches

#### The Woman at the Gate / offer / stage 1 of 4

- Stage title: `Inspect The Dented Shield`
- Objective: `SEARCH` Highwall Gate Shield (0/1)

#### The Woman at the Gate -> What truth are you afraid -> I will stand where this breaks

- NPC says: The watch gate is not a wall, but people lean on it. That is enough for my attention.
- Options:
  1. Can we talk about loyalty?
  2. Tell me about The Woman at the Gate.
  3. Can we talk, Cassia?
  4. How does Oathstead feel to you?
  5. What does the forest ask before it lets people pass?
- Player: Tell me about The Woman at the Gate.
- NPC narration: Cassia answers like someone holding a line others once abandoned.
- NPC says: Say what you came to say. Gates do not open wider because people mumble at them.
- Options:
  1. What truth are you afraid is hidden there?
  2. Whose order broke you?
  3. What am I looking for, exactly?
  4. I will stand where this breaks.
  5. Give me the version I can act on.
  6. Back to Cassia
- Player: What truth are you afraid is hidden there?
- NPC narration: Cassia answers like someone holding a line others once abandoned.
- NPC says: This is about who was protected, who was abandoned, and who gave the order. For now: find Highwall Gate Shield at the Graveyard near Alderfall.
- Options:
  1. And what does that cost you?
  2. What am I looking for, exactly?
  3. I will stand where this breaks.
  4. Back to The Woman at the Gate
- Player: I will stand where this breaks.
- NPC narration: Cassia steadies her shoulders, but her answer is for you rather than the room.
- NPC says: Good. Judgment first, noise never. For now: find Highwall Gate Shield at the Graveyard near Alderfall.
- Options:
  1. Back to Cassia

#### The Woman at the Gate / accepted / stage 1 of 4

- Stage title: `Inspect The Dented Shield`
- Objective: `SEARCH` Highwall Gate Shield (0/1)

#### Where do things stand -> Where do I start looking

- NPC says: The watch gate is not a wall, but people lean on it. That is enough for my attention.
- Options:
  1. Can we talk about loyalty?
  2. Where do things stand with The Woman at the Gate?
  3. Can we talk, Cassia?
  4. How does Oathstead feel to you?
  5. What does the forest ask before it lets people pass?
- Player: Where do things stand with The Woman at the Gate?
- NPC narration: Cassia answers like someone holding a line others once abandoned.
- NPC says: The gate should have held. It did not. I have been arguing with that fact longer than is dignified.
- Options:
  1. What truth are you afraid is hidden there?
  2. Where do I start looking?
  3. Whose order broke you?
  4. What would someone hide in plain sight?
  5. Back to Cassia
- Player: Where do I start looking?
- NPC narration: Cassia makes the answer practical before fear can start giving orders.
- NPC says: Start at the Graveyard near Alderfall. Find Highwall Gate Shield.
- Options:
  1. I know where to start.
  2. What would someone hide in plain sight?
  3. Back to The Woman at the Gate

#### The Woman at the Gate / ready / stage 1 of 4

- Stage title: `Inspect The Dented Shield`
- Objective: `SEARCH` Highwall Gate Shield (1/1)

#### I found what you needed

- NPC says: The watch gate is not a wall, but people lean on it. That is enough for my attention.
- Options:
  1. Can we talk about loyalty?
  2. I found what you needed.
  3. Can we talk, Cassia?
  4. How does Oathstead feel to you?
  5. What does the forest ask before it lets people pass?
- Player: I found what you needed.
- NPC narration: Cassia answers like someone holding a line others once abandoned.
- NPC says: The dent came from the outside. A civilian tool, not a raider axe. Show me. I will not salute the truth until I have looked it in the face.
- Options:
  1. I found what someone tried to hide.
  2. What does this prove?
  3. What does it cost you to hear this?
  4. What did the answer disturb?
  5. Back to Cassia

#### The Woman at the Gate / accepted / stage 2 of 4

- Stage title: `Test The Old Gate Winch`
- Objective: `SEARCH` Old Gate Winch (0/1)

#### Where do things stand -> Where do I start looking

- NPC says: The watch gate is not a wall, but people lean on it. That is enough for my attention.
- Options:
  1. Can we talk about loyalty?
  2. Where do things stand with The Woman at the Gate?
  3. Can we talk, Cassia?
  4. How does Oathstead feel to you?
  5. What does the forest ask before it lets people pass?
- Player: Where do things stand with The Woman at the Gate?
- NPC narration: Cassia answers like someone holding a line others once abandoned.
- NPC says: The gate should have held. It did not. I have been arguing with that fact longer than is dignified.
- Options:
  1. What truth are you afraid is hidden there?
  2. Where do I start looking?
  3. Whose order broke you?
  4. What would someone hide in plain sight?
  5. Back to Cassia
- Player: Where do I start looking?
- NPC narration: Cassia makes the answer practical before fear can start giving orders.
- NPC says: Start at the Graveyard near Alderfall. Find Old Gate Winch.
- Options:
  1. I know where to start.
  2. What would someone hide in plain sight?
  3. Back to The Woman at the Gate

#### The Woman at the Gate / completed / stage 4 of 4

- Stage title: `Name What The Gate Kept`
- Objective: `CHOICE` Highwall Gate Memory (1/1)

#### What changed after -> What is still unresolved

- NPC says: The watch gate is not a wall, but people lean on it. That is enough for my attention.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. What changed after The Woman at the Gate?
  3. Can we talk, Cassia?
  4. How does Oathstead feel to you?
  5. What does the forest ask before it lets people pass?
- Player: What changed after The Woman at the Gate?
- NPC narration: Cassia answers like someone holding a line others once abandoned. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Good. The gate has spoken enough for one day. First breach named. That matters more than a clean report.
- Options:
  1. Who carries the consequence with me?
  2. Do you think I chose well?
  3. What is still unresolved?
  4. Back to Cassia
- Player: What is still unresolved?
- NPC narration: Cassia makes the answer practical before fear can start giving orders. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Do not let discipline become a hiding place. Orders still need judgment.
- Options:
  1. Back to Cassia

### Representative Conversation Paths

#### Can we talk, -> How are you holding up? -> How are you feeling?

- NPC says: The watch gate is not a wall, but people lean on it. That is enough for my attention.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Cassia?
  3. How does Oathstead feel to you?
  4. What does the forest ask before it lets people pass?
- Player: Can we talk, Cassia?
- NPC says: Cassia studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Cassia
- Player: How are you holding up?
- NPC narration: Cassia steadies her shoulders, but her answer is for you rather than the room.
- NPC says: You ask after the person inside the armor. That is still not standard procedure.
- Options:
  1. How are you feeling?
  2. What should we do next?
  3. Do you need anything?
  4. Back to personal matters
- Player: How are you feeling?
- NPC narration: Cassia steadies her shoulders, but her answer is for you rather than the room.
- NPC says: You want the answer under the armor. Fine. Bruised under the discipline. Still standing.
- Options:
  1. I will not push.
  2. Tell me the honest version.
  3. We can keep moving.
  4. Back to personal matters

#### Can we talk, -> How do you see me?

- NPC says: The watch gate is not a wall, but people lean on it. That is enough for my attention.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Cassia?
  3. How does Oathstead feel to you?
  4. What does the forest ask before it lets people pass?
- Player: Can we talk, Cassia?
- NPC says: Cassia studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Cassia
- Player: How do you see me?
- NPC narration: Cassia meets the question squarely, no salute to hide behind.
- NPC says: I am still deciding what your choices mean. The pattern matters more than one noble moment.
- Options:
  1. What do you trust in me?
  2. What worries you about me?
  3. What should I remember about you?
  4. Back to personal matters

#### Can we talk, -> Do you trust me?

- NPC says: The watch gate is not a wall, but people lean on it. That is enough for my attention.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Can we talk, Cassia?
  3. How does Oathstead feel to you?
  4. What does the forest ask before it lets people pass?
- Player: Can we talk, Cassia?
- NPC says: Cassia waits without rushing you. Trust has made the quiet less defensive.
- Options:
  1. Do you trust me?
  2. You are ruining my discipline.
  3. How are you holding up?
  4. How do you see me?
  5. Can we talk about the duty choices?
  6. Do you remember what happened?
  7. What do you think of...?
  8. Back to Cassia
- Player: Do you trust me?
- NPC narration: Cassia meets the question squarely, no salute to hide behind. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Trust is not a speech. It is where I stand when the line moves. If the line moves, I look for you before I look for orders.
- Options:
  1. Stand with me.
  2. You are free to choose your road.
  3. I am glad you are here.
  4. Good. I need your skills.

#### Can we talk, -> What do you think of -> Me, honestly

- NPC says: The watch gate is not a wall, but people lean on it. That is enough for my attention.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Cassia?
  3. How does Oathstead feel to you?
  4. What does the forest ask before it lets people pass?
- Player: Can we talk, Cassia?
- NPC says: Cassia studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Cassia
- Player: What do you think of...?
- NPC narration: Cassia meets the question squarely, no salute to hide behind.
- NPC says: Choose whose name you bring into the space between you.
- Options:
  1. Me, honestly.
  2. Aria
  3. Calder
  4. Lyra
  5. Maera
  6. Rafiq
  7. Samir
  8. Seraphine
  9. Vesper
  10. Back to personal matters
- Player: Me, honestly.
- NPC narration: Cassia meets the question squarely, no salute to hide behind.
- NPC says: You bend under weight, then stand again. I trust that more than speeches.
- Options:
  1. That is fair.
  2. You notice more than I thought.
  3. Back to personal matters

#### Can we talk, -> What do you think of -> Known companion

- NPC says: The watch gate is not a wall, but people lean on it. That is enough for my attention.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Cassia?
  3. How does Oathstead feel to you?
  4. What does the forest ask before it lets people pass?
- Player: Can we talk, Cassia?
- NPC says: Cassia studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Cassia
- Player: What do you think of...?
- NPC narration: Cassia meets the question squarely, no salute to hide behind.
- NPC says: Choose whose name you bring into the space between you.
- Options:
  1. Me, honestly.
  2. Aria
  3. Calder
  4. Lyra
  5. Maera
  6. Rafiq
  7. Samir
  8. Seraphine
  9. Vesper
  10. Back to personal matters
- Player: Aria
- NPC narration: Cassia meets the question squarely, no salute to hide behind.
- NPC says: Aria moves like someone who expects betrayal. I hope she learns otherwise.
- Options:
  1. Back to conversation

#### How does Oathstead feel -> How does Oathstead feel -> What does Oathstead need next

- NPC says: The watch gate is not a wall, but people lean on it. That is enough for my attention.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Can we talk, Cassia?
  3. How does Oathstead feel to you?
  4. What does the forest ask before it lets people pass?
- Player: How does Oathstead feel to you?
- NPC says: Cassia speaks of Oathstead like a place that may yet learn how to hold people gently.
- Options:
  1. How does Oathstead feel to you?
  2. Back to Cassia
- Player: How does Oathstead feel to you?
- NPC narration: Cassia's posture stays disciplined, but her voice does not. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Oathstead asks for duty without demanding I disappear inside it. I still do not quite trust how much I need that.
- Options:
  1. Does this place feel like yours?
  2. How is your work here?
  3. What does Oathstead need next?
  4. Back to home and rest
- Player: What does Oathstead need next?
- NPC narration: Cassia makes the answer practical before fear can start giving orders. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: A gate that opens for the right people and closes for the right reasons.
- Options:
  1. Back to Cassia

#### Can we talk, -> Come here a moment. -> I missed being close to you.

- NPC says: You are here. Good. I will pretend that did not change my breathing.
- Options:
  1. Can we talk about the future?
  2. Can we talk, Cassia?
  3. Stay with me a while.
  4. What does the forest ask before it lets people pass?
- Player: Can we talk, Cassia?
- NPC says: Cassia lets the pause settle between you, careful and warm.
- Options:
  1. What is this between us?
  2. Stay with me here.
  3. Come here a moment.
  4. How are you holding up?
  5. How do you see me?
  6. Can we talk about the duty choices?
  7. Do you remember what happened?
  8. What do you think of...?
  9. Back to Cassia
- Player: Come here a moment.
- NPC narration: Cassia's posture stays disciplined, but her voice does not. There is warmth in it now, not careless, but no longer disguised as tactics.
- NPC says: You are making my pulse tactically unhelpful. I am choosing not to object.
- Options:
  1. I missed being close to you.
  2. Permission to be a terrible distraction?
  3. Too much?
  4. Back to personal matters
- Player: I missed being close to you.
- NPC narration: Cassia's posture stays disciplined, but her voice does not. There is warmth in it now, not careless, but no longer disguised as tactics.
- NPC says: Then stand there a moment. I can guard the world badly for one breath.
- Options:
  1. I wanted you to know.
  2. Back to personal matters

#### Can we talk, -> Stay with me here.

- NPC says: You are here. Good. I will pretend that did not change my breathing.
- Options:
  1. Can we talk about the future?
  2. Can we talk, Cassia?
  3. Stay with me a while.
  4. What does the forest ask before it lets people pass?
- Player: Can we talk, Cassia?
- NPC says: Cassia lets the pause settle between you, careful and warm.
- Options:
  1. What is this between us?
  2. Stay with me here.
  3. Come here a moment.
  4. How are you holding up?
  5. How do you see me?
  6. Can we talk about the duty choices?
  7. Do you remember what happened?
  8. What do you think of...?
  9. Back to Cassia
- Player: Stay with me here.
- NPC narration: Cassia's posture stays disciplined, but her voice does not. There is warmth in it now, not careless, but no longer disguised as tactics.
- NPC says: This corner of the inn table near Oathstead's road has two exits, a tolerable sightline, and you. I am trying to notice the third thing most.
- Options:
  1. You make standing guard difficult.
  2. I wanted time with you, not another task.
  3. Stay here with me.
  4. We can keep this gentle.
  5. Back to personal matters

#### Can we talk, -> Can we talk about our promise?

- NPC says: There you are. The line holds better when I know where you stand.
- Options:
  1. Can we talk, Cassia?
  2. Stay with me a while.
  3. What does the forest ask before it lets people pass?
- Player: Can we talk, Cassia?
- NPC says: Cassia gives you the kind of attention that has learned your silences.
- Options:
  1. Can we talk about our promise?
  2. Stay with me here.
  3. Come here a moment.
  4. How are you holding up?
  5. How do you see me?
  6. Can we talk about the duty choices?
  7. Do you remember what happened?
  8. What do you think of...?
  9. Back to Cassia
- Player: Can we talk about our promise?
- NPC narration: Cassia meets the question squarely, no salute to hide behind. The answer comes with the ease of someone who has learned your silences.
- NPC says: Trust is not a speech. It is where I stand when the line moves. I used to believe vows were walls. With you, they feel more like a hand at my back.
- Options:
  1. Come home to me when you can.
  2. Tell me what still frightens you.
  3. The work comes first today.
  4. Back to topics

### Repeat-Aware Sample

#### Can we talk, -> How are you holding up? -> How are you feeling?

- NPC says: The watch gate is not a wall, but people lean on it. That is enough for my attention.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Can we talk, Cassia?
  3. How does Oathstead feel to you?
  4. What does the forest ask before it lets people pass?
- Player: Can we talk, Cassia?
- NPC says: Cassia waits without rushing you. Trust has made the quiet less defensive.
- Options:
  1. Do you trust me?
  2. You are ruining my discipline.
  3. How are you holding up?
  4. How do you see me?
  5. Can we talk about the duty choices?
  6. Do you remember what happened?
  7. What do you think of...?
  8. Back to Cassia
- Player: How are you holding up?
- NPC narration: Cassia steadies her shoulders, but her answer is for you rather than the room. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: You ask like you intend to hear the answer. That still catches me off guard.
- Options:
  1. How are you feeling?
  2. What should we do next?
  3. Do you need anything?
  4. Back to personal matters
- Player: How are you feeling?
- NPC narration: Cassia steadies her shoulders, but her answer is for you rather than the room. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Steady enough to stand. Honest enough to admit standing is not the same as healed.
- Options:
  1. I will not push.
  2. Tell me the honest version.
  3. We can keep moving.
  4. Back to personal matters


## Lyra Bell

- Recruit id: `lyra`
- Starting quest: `The Same Fever Twice`
- Quest stages: 4

### QA Findings

_No QA findings detected in sampled paths._

### Trust Snapshot Roots

#### Trust 0

- NPC says: The clinic is out of clean cloth, good sleep, and patient gods. Bring any two and I will improvise the third.
- Options:
  1. Can we talk about guarded respect?
  2. Tell me about The Same Fever Twice.
  3. Can we talk, Lyra?
  4. What should I avoid answering after dusk?

#### Trust 20

- NPC says: The clinic is out of clean cloth, good sleep, and patient gods. Bring any two and I will improvise the third.
- Options:
  1. Can we talk about guarded respect?
  2. Tell me about The Same Fever Twice.
  3. Can we talk, Lyra?
  4. What does home mean to you now?
  5. What should I avoid answering after dusk?

#### Trust 50

- NPC says: The clinic is out of clean cloth, good sleep, and patient gods. Bring any two and I will improvise the third.
- Options:
  1. Can we talk about what changed between us?
  2. Where do things stand with The Same Fever Twice?
  3. Can we talk, Lyra?
  4. What does home mean to you now?
  5. What should I avoid answering after dusk?

#### Trust 100

- NPC says: The clinic corner needs clean hands, calmer voices, and fewer heroic entrances. I am working on two.
- Options:
  1. Can we talk about loyalty?
  2. I found what you needed.
  3. Can we talk, Lyra?
  4. How does Oathstead feel to you?
  5. What should I avoid answering after dusk?

#### Trust 150

- NPC says: The clinic corner needs clean hands, calmer voices, and fewer heroic entrances. I am working on two.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Where do things stand with The Same Fever Twice?
  3. Can we talk, Lyra?
  4. How does Oathstead feel to you?
  5. What should I avoid answering after dusk?

#### Trust 180

- NPC says: There you are. I was not waiting. I was... pausing with intent.
- Options:
  1. Can we talk about the future?
  2. Where do things stand with The Same Fever Twice?
  3. Can we talk, Lyra?
  4. Stay with me a while.
  5. What should I avoid answering after dusk?

#### Trust 250

- NPC says: There you are. Sit if you can. I am trying to learn that welcome is also care.
- Options:
  1. What changed after The Same Fever Twice?
  2. Can we talk, Lyra?
  3. Stay with me a while.
  4. What should I avoid answering after dusk?

### Quest Stage Branches

#### The Same Fever Twice / offer / stage 1 of 4

- Stage title: `Inspect The Fever Cot`
- Objective: `VISIT` Roadside Fever Cot (0/1)

#### The Same Fever Twice -> What made this place worth checking -> I will help before more people get hurt

- NPC says: The clinic corner needs clean hands, calmer voices, and fewer heroic entrances. I am working on two.
- Options:
  1. Can we talk about loyalty?
  2. Tell me about The Same Fever Twice.
  3. Can we talk, Lyra?
  4. How does Oathstead feel to you?
  5. What should I avoid answering after dusk?
- Player: Tell me about The Same Fever Twice.
- NPC narration: Lyra listens first, the way a healer listens for breath under pain.
- NPC says: That cough is not new. I heard it in Belltower three nights ago.
- Options:
  1. What made this place worth checking?
  2. Who taught you mercy costs this much?
  3. What am I supposed to notice there?
  4. I will help before more people get hurt.
  5. Give me the version I can act on.
  6. Back to Lyra
- Player: What made this place worth checking?
- NPC narration: Lyra listens first, the way a healer listens for breath under pain.
- NPC says: People die while heroes debate symbols. We start with care. For now: inspect Roadside Fever Cot at the Farmland near Alderfall.
- Options:
  1. And what does that cost you?
  2. What am I supposed to notice there?
  3. I will help before more people get hurt.
  4. Back to The Same Fever Twice
- Player: I will help before more people get hurt.
- NPC narration: Lyra studies your face first, gentle as a healer checking an old wound.
- NPC says: Good. We move before care arrives too late. For now: inspect Roadside Fever Cot at the Farmland near Alderfall.
- Options:
  1. Back to Lyra

#### The Same Fever Twice / accepted / stage 1 of 4

- Stage title: `Inspect The Fever Cot`
- Objective: `VISIT` Roadside Fever Cot (0/1)

#### Where do things stand -> Show me the place

- NPC says: The clinic corner needs clean hands, calmer voices, and fewer heroic entrances. I am working on two.
- Options:
  1. Can we talk about loyalty?
  2. Where do things stand with The Same Fever Twice?
  3. Can we talk, Lyra?
  4. How does Oathstead feel to you?
  5. What should I avoid answering after dusk?
- Player: Where do things stand with The Same Fever Twice?
- NPC narration: Lyra listens first, the way a healer listens for breath under pain.
- NPC says: The clinic is out of clean cloth, good sleep, and patient gods. Bring any two and I will improvise the third.
- Options:
  1. What made this place worth checking?
  2. Show me the place I should inspect.
  3. Who taught you mercy costs this much?
  4. What would staged evidence look like?
  5. Back to Lyra
- Player: Show me the place I should inspect.
- NPC narration: Lyra counts what can be done before grief can make the room too large.
- NPC says: Go to the Farmland near Alderfall and inspect Roadside Fever Cot.
- Options:
  1. I know where to start.
  2. What would staged evidence look like?
  3. Back to The Same Fever Twice

#### The Same Fever Twice / ready / stage 1 of 4

- Stage title: `Inspect The Fever Cot`
- Objective: `VISIT` Roadside Fever Cot (1/1)

#### I found what you needed

- NPC says: The clinic corner needs clean hands, calmer voices, and fewer heroic entrances. I am working on two.
- Options:
  1. Can we talk about loyalty?
  2. I found what you needed.
  3. Can we talk, Lyra?
  4. How does Oathstead feel to you?
  5. What should I avoid answering after dusk?
- Player: I found what you needed.
- NPC narration: Lyra listens first, the way a healer listens for breath under pain.
- NPC says: Sweat at the pillow, blue at the nails, no rash. This fever has a pattern. Show me carefully. Proof can bruise too, if handled like a victory.
- Options:
  1. I saw enough to answer you.
  2. What does this prove?
  3. What does it cost you to hear this?
  4. What did the answer disturb?
  5. Back to Lyra

#### The Same Fever Twice / accepted / stage 2 of 4

- Stage title: `Take A Well Sample`
- Objective: `GATHER` Clean Well Sample (0/1)

#### Where do things stand -> Where do I find what you need

- NPC says: The clinic corner needs clean hands, calmer voices, and fewer heroic entrances. I am working on two.
- Options:
  1. Can we talk about loyalty?
  2. Where do things stand with The Same Fever Twice?
  3. Can we talk, Lyra?
  4. How does Oathstead feel to you?
  5. What should I avoid answering after dusk?
- Player: Where do things stand with The Same Fever Twice?
- NPC narration: Lyra listens first, the way a healer listens for breath under pain.
- NPC says: The clinic is out of clean cloth, good sleep, and patient gods. Bring any two and I will improvise the third.
- Options:
  1. Why does this need to come back?
  2. Where do I find what you need?
  3. Who taught you mercy costs this much?
  4. What should I not disturb?
  5. Back to Lyra
- Player: Where do I find what you need?
- NPC narration: Lyra counts what can be done before grief can make the room too large.
- NPC says: Start at the Farmland near Alderfall and bring back Clean Well Sample.
- Options:
  1. I know where to start.
  2. What should I not disturb?
  3. Back to The Same Fever Twice

#### The Same Fever Twice / completed / stage 4 of 4

- Stage title: `Choose The First Triage`
- Objective: `CHOICE` Roadside Triage Rule (1/1)

#### What changed after -> What is still unresolved

- NPC says: The clinic corner needs clean hands, calmer voices, and fewer heroic entrances. I am working on two.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. What changed after The Same Fever Twice?
  3. Can we talk, Lyra?
  4. How does Oathstead feel to you?
  5. What should I avoid answering after dusk?
- Player: What changed after The Same Fever Twice?
- NPC narration: Lyra listens first, the way a healer listens for breath under pain. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Good. Now we have care with a spine. First wound traced. Now we keep it from becoming someone else's.
- Options:
  1. Who carries the consequence with me?
  2. Do you think I chose well?
  3. What is still unresolved?
  4. Back to Lyra
- Player: What is still unresolved?
- NPC narration: Lyra studies your face first, gentle as a healer checking an old wound. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Do not move so fast that the living become background. Check who still needs help.
- Options:
  1. Back to Lyra

### Representative Conversation Paths

#### Can we talk, -> How are you holding up? -> How are you feeling?

- NPC says: The clinic corner needs clean hands, calmer voices, and fewer heroic entrances. I am working on two.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Lyra?
  3. How does Oathstead feel to you?
  4. What should I avoid answering after dusk?
- Player: Can we talk, Lyra?
- NPC says: Lyra studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Lyra
- Player: How are you holding up?
- NPC narration: Lyra lets the gentleness stay, even when the answer does not.
- NPC says: You are checking the healer for wounds. That is irritatingly fair. You remembered that healers bruise too. Sensible. Inconveniently kind.
- Options:
  1. How are you feeling?
  2. What should we do next?
  3. Do you need anything?
  4. Back to personal matters
- Player: How are you feeling?
- NPC narration: Lyra lets the gentleness stay, even when the answer does not.
- NPC says: Tired in the hands. Better because someone noticed.
- Options:
  1. I will not push.
  2. Tell me the honest version.
  3. We can keep moving.
  4. Back to personal matters

#### Can we talk, -> How do you see me?

- NPC says: The clinic corner needs clean hands, calmer voices, and fewer heroic entrances. I am working on two.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Lyra?
  3. How does Oathstead feel to you?
  4. What should I avoid answering after dusk?
- Player: Can we talk, Lyra?
- NPC says: Lyra studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Lyra
- Player: How do you see me?
- NPC narration: Lyra studies your face first, gentle as a healer checking an old wound.
- NPC says: I am still deciding what your choices mean. The pattern matters more than one noble moment.
- Options:
  1. What do you trust in me?
  2. What worries you about me?
  3. What should I remember about you?
  4. Back to personal matters

#### Can we talk, -> Do you trust me?

- NPC says: The clinic corner needs clean hands, calmer voices, and fewer heroic entrances. I am working on two.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Can we talk, Lyra?
  3. How does Oathstead feel to you?
  4. What should I avoid answering after dusk?
- Player: Can we talk, Lyra?
- NPC says: Lyra waits without rushing you. Trust has made the quiet less defensive.
- Options:
  1. Do you trust me?
  2. I like seeing you smile.
  3. How are you holding up?
  4. How do you see me?
  5. Can we talk about the healing choices?
  6. Do you remember what happened?
  7. What do you think of...?
  8. Back to Lyra
- Player: Do you trust me?
- NPC narration: Lyra studies your face first, gentle as a healer checking an old wound. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: I trust you with the part of care that keeps working after the room goes quiet.
- Options:
  1. Stand with me.
  2. You are free to choose your road.
  3. I am glad you are here.
  4. Good. I need your skills.

#### Can we talk, -> What do you think of -> Me, honestly

- NPC says: The clinic corner needs clean hands, calmer voices, and fewer heroic entrances. I am working on two.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Lyra?
  3. How does Oathstead feel to you?
  4. What should I avoid answering after dusk?
- Player: Can we talk, Lyra?
- NPC says: Lyra studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Lyra
- Player: What do you think of...?
- NPC narration: Lyra studies your face first, gentle as a healer checking an old wound.
- NPC says: Choose whose name you bring into the space between you.
- Options:
  1. Me, honestly.
  2. Aria
  3. Calder
  4. Cassia
  5. Maera
  6. Rafiq
  7. Samir
  8. Seraphine
  9. Vesper
  10. Back to personal matters
- Player: Me, honestly.
- NPC narration: Lyra studies your face first, gentle as a healer checking an old wound.
- NPC says: You get hurt often enough to worry me and keep helping often enough to make the worry worthwhile.
- Options:
  1. That is fair.
  2. You notice more than I thought.
  3. Back to personal matters

#### Can we talk, -> What do you think of -> Known companion

- NPC says: The clinic corner needs clean hands, calmer voices, and fewer heroic entrances. I am working on two.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Lyra?
  3. How does Oathstead feel to you?
  4. What should I avoid answering after dusk?
- Player: Can we talk, Lyra?
- NPC says: Lyra studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Lyra
- Player: What do you think of...?
- NPC narration: Lyra studies your face first, gentle as a healer checking an old wound.
- NPC says: Choose whose name you bring into the space between you.
- Options:
  1. Me, honestly.
  2. Aria
  3. Calder
  4. Cassia
  5. Maera
  6. Rafiq
  7. Samir
  8. Seraphine
  9. Vesper
  10. Back to personal matters
- Player: Aria
- NPC narration: Lyra studies your face first, gentle as a healer checking an old wound.
- NPC says: Aria says she is fine too quickly. That is a symptom.
- Options:
  1. Back to conversation

#### How does Oathstead feel -> How does Oathstead feel -> What does Oathstead need next

- NPC says: The clinic corner needs clean hands, calmer voices, and fewer heroic entrances. I am working on two.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Can we talk, Lyra?
  3. How does Oathstead feel to you?
  4. What should I avoid answering after dusk?
- Player: How does Oathstead feel to you?
- NPC says: Lyra speaks of Oathstead like a place that may yet learn how to hold people gently.
- Options:
  1. How does Oathstead feel to you?
  2. Back to Lyra
- Player: How does Oathstead feel to you?
- NPC narration: Lyra's expression warms with the relief of being asked to rest, not only mend. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Oathstead's clinic is becoming the kind of mercy that arrives before panic. That may be the first cure I ever helped build.
- Options:
  1. Does this place feel like yours?
  2. How is your work here?
  3. What does Oathstead need next?
  4. Back to home and rest
- Player: What does Oathstead need next?
- NPC narration: Lyra counts what can be done before grief can make the room too large. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Clean water, spare cloth, and fewer heroes pretending infection respects bravery.
- Options:
  1. Back to Lyra

#### Can we talk, -> Come here a moment. -> I missed being close to you.

- NPC says: There you are. I was not waiting. I was... pausing with intent.
- Options:
  1. Can we talk about the future?
  2. Can we talk, Lyra?
  3. Stay with me a while.
  4. What should I avoid answering after dusk?
- Player: Can we talk, Lyra?
- NPC says: Lyra lets the pause settle between you, careful and warm.
- Options:
  1. What is this between us?
  2. Stay with me here.
  3. Come here a moment.
  4. How are you holding up?
  5. How do you see me?
  6. Can we talk about the healing choices?
  7. Do you remember what happened?
  8. What do you think of...?
  9. Back to Lyra
- Player: Come here a moment.
- NPC narration: Lyra's expression warms with the relief of being asked to rest, not only mend. There is warmth in it now, not careless, but no longer disguised as tactics.
- NPC says: That is unfair. I know several remedies for fever and none for you.
- Options:
  1. I missed being close to you.
  2. I think you are bad for my pulse.
  3. Too much?
  4. Back to personal matters
- Player: I missed being close to you.
- NPC narration: Lyra's expression warms with the relief of being asked to rest, not only mend. There is warmth in it now, not careless, but no longer disguised as tactics.
- NPC says: Near is a very good medicine when administered by someone I trust.
- Options:
  1. I wanted you to know.
  2. Back to personal matters

#### Can we talk, -> Stay with me here.

- NPC says: There you are. I was not waiting. I was... pausing with intent.
- Options:
  1. Can we talk about the future?
  2. Can we talk, Lyra?
  3. Stay with me a while.
  4. What should I avoid answering after dusk?
- Player: Can we talk, Lyra?
- NPC says: Lyra lets the pause settle between you, careful and warm.
- Options:
  1. What is this between us?
  2. Stay with me here.
  3. Come here a moment.
  4. How are you holding up?
  5. How do you see me?
  6. Can we talk about the healing choices?
  7. Do you remember what happened?
  8. What do you think of...?
  9. Back to Lyra
- Player: Stay with me here.
- NPC narration: Lyra's expression warms with the relief of being asked to rest, not only mend. There is warmth in it now, not careless, but no longer disguised as tactics.
- NPC says: the inn table near Oathstead's road is warm enough, quiet enough, and not currently bleeding. A rare medical recommendation.
- Options:
  1. Your smile is terrible for my discipline.
  2. I wanted time with you, not another task.
  3. Stay here with me.
  4. We can keep this gentle.
  5. Back to personal matters

#### Can we talk, -> Can we talk about our promise?

- NPC says: There you are. Sit if you can. I am trying to learn that welcome is also care.
- Options:
  1. Can we talk, Lyra?
  2. Stay with me a while.
  3. What should I avoid answering after dusk?
- Player: Can we talk, Lyra?
- NPC says: Lyra gives you the kind of attention that has learned your silences.
- Options:
  1. Can we talk about our promise?
  2. Stay with me here.
  3. Come here a moment.
  4. How are you holding up?
  5. How do you see me?
  6. Can we talk about the healing choices?
  7. Do you remember what happened?
  8. What do you think of...?
  9. Back to Lyra
- Player: Can we talk about our promise?
- NPC narration: Lyra studies your face first, gentle as a healer checking an old wound. The answer comes with the ease of someone who has learned your silences.
- NPC says: I have held too many hands at endings. I want yours at beginnings too.
- Options:
  1. Come home to me when you can.
  2. Tell me what still frightens you.
  3. The work comes first today.
  4. Back to topics

### Repeat-Aware Sample

#### Can we talk, -> How are you holding up? -> How are you feeling?

- NPC says: The clinic corner needs clean hands, calmer voices, and fewer heroic entrances. I am working on two.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Can we talk, Lyra?
  3. How does Oathstead feel to you?
  4. What should I avoid answering after dusk?
- Player: Can we talk, Lyra?
- NPC says: Lyra waits without rushing you. Trust has made the quiet less defensive.
- Options:
  1. Do you trust me?
  2. I like seeing you smile.
  3. How are you holding up?
  4. How do you see me?
  5. Can we talk about the healing choices?
  6. Do you remember what happened?
  7. What do you think of...?
  8. Back to Lyra
- Player: How are you holding up?
- NPC narration: Lyra lets the gentleness stay, even when the answer does not. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: You ask like you intend to hear the answer. That still catches me off guard.
- Options:
  1. How are you feeling?
  2. What should we do next?
  3. Do you need anything?
  4. Back to personal matters
- Player: How are you feeling?
- NPC narration: Lyra lets the gentleness stay, even when the answer does not. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Tired in the hands. Better in the heart than I expected.
- Options:
  1. I will not push.
  2. Tell me the honest version.
  3. We can keep moving.
  4. Back to personal matters


## Maera Quill

- Recruit id: `maera`
- Starting quest: `The Wrong Star`
- Quest stages: 4

### QA Findings

_No QA findings detected in sampled paths._

### Trust Snapshot Roots

#### Trust 0

- NPC says: The star map shows a road the royal record says never existed. Someone cut out the star that proves the road was there. I need the map, the route entry, and Miri's testimony before the Archive files the lie as policy.
- Options:
  1. Can we talk about guarded respect?
  2. Tell me about The Wrong Star.
  3. Can we talk, Maera?
  4. What does the forest ask before it lets people pass?

#### Trust 20

- NPC says: The star map shows a road the royal record says never existed. Someone cut out the star that proves the road was there. I need the map, the route entry, and Miri's testimony before the Archive files the lie as policy.
- Options:
  1. Can we talk about guarded respect?
  2. Tell me about The Wrong Star.
  3. Can we talk, Maera?
  4. What does home mean to you now?
  5. What does the forest ask before it lets people pass?

#### Trust 50

- NPC says: The star map shows a road the royal record says never existed. Someone cut out the star that proves the road was there. I need the map, the route entry, and Miri's testimony before the Archive files the lie as policy.
- Options:
  1. Can we talk about what changed between us?
  2. Where do things stand with The Wrong Star?
  3. Can we talk, Maera?
  4. What does home mean to you now?
  5. What does the forest ask before it lets people pass?

#### Trust 100

- NPC says: The map desk keeps producing questions. I have claimed a corner for the dangerous ones.
- Options:
  1. Can we talk about loyalty?
  2. I found what you needed.
  3. Can we talk, Maera?
  4. How does Oathstead feel to you?
  5. What does the forest ask before it lets people pass?

#### Trust 150

- NPC says: The map desk keeps producing questions. I have claimed a corner for the dangerous ones.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Where do things stand with The Wrong Star?
  3. Can we talk, Maera?
  4. How does Oathstead feel to you?
  5. What does the forest ask before it lets people pass?

#### Trust 180

- NPC says: You arrived before I finished sorting my thoughts. Annoyingly, that may improve them.
- Options:
  1. Can we talk about the future?
  2. Where do things stand with The Wrong Star?
  3. Can we talk, Maera?
  4. Stay with me a while.
  5. What does the forest ask before it lets people pass?

#### Trust 250

- NPC says: There you are. I was about to annotate the silence, which would have been undignified.
- Options:
  1. What changed after The Wrong Star?
  2. Can we talk, Maera?
  3. Stay with me a while.
  4. What does the forest ask before it lets people pass?

### Quest Stage Branches

#### The Wrong Star / offer / stage 1 of 4

- Stage title: `Inspect The Broken Star-Route Map`
- Objective: `SEARCH` Broken Star-Route Map (0/1)

#### The Wrong Star -> What truth are you afraid -> I will follow the evidence

- NPC says: The map desk keeps producing questions. I have claimed a corner for the dangerous ones.
- Options:
  1. Can we talk about loyalty?
  2. Tell me about The Wrong Star.
  3. Can we talk, Maera?
  4. How does Oathstead feel to you?
  5. What does the forest ask before it lets people pass?
- Player: Tell me about The Wrong Star.
- NPC narration: Maera follows the thought like a line of ink across a dangerous margin.
- NPC says: This star map shows a road north of the old grave markers. The royal record says that road never existed. Someone scraped out the star that proves otherwise.
- Options:
  1. What truth are you afraid is hidden there?
  2. What did your mother hide from you?
  3. What am I looking for, exactly?
  4. I will follow the evidence with you.
  5. Give me the version I can act on.
  6. Back to Maera
- Player: What truth are you afraid is hidden there?
- NPC narration: Maera follows the thought like a line of ink across a dangerous margin.
- NPC says: The map, the route record, and the witness cannot all be mistakes. Someone made the official version contradict the physical evidence. For now: find Broken Star-Route Map at the Graveyard near Alderfall.
- Options:
  1. And what does that cost you?
  2. What am I looking for, exactly?
  3. I will follow the evidence with you.
  4. Back to The Wrong Star
- Player: I will follow the evidence with you.
- NPC narration: Maera sorts the thought as if it were a page with dangerous margins.
- NPC says: Good. Keep the evidence in one piece. For now: find Broken Star-Route Map at the Graveyard near Alderfall.
- Options:
  1. Back to Maera

#### The Wrong Star / accepted / stage 1 of 4

- Stage title: `Inspect The Broken Star-Route Map`
- Objective: `SEARCH` Broken Star-Route Map (0/1)

#### Where do things stand -> Where do I start looking

- NPC says: The map desk keeps producing questions. I have claimed a corner for the dangerous ones.
- Options:
  1. Can we talk about loyalty?
  2. Where do things stand with The Wrong Star?
  3. Can we talk, Maera?
  4. How does Oathstead feel to you?
  5. What does the forest ask before it lets people pass?
- Player: Where do things stand with The Wrong Star?
- NPC narration: Maera follows the thought like a line of ink across a dangerous margin.
- NPC says: The star map shows a road the royal record says never existed. Someone cut out the star that proves the road was there. I need the map, the route entry, and Miri's testimony before the Archive files the lie as policy.
- Options:
  1. What truth are you afraid is hidden there?
  2. Where do I start looking?
  3. What did your mother hide from you?
  4. What would someone hide in plain sight?
  5. Back to Maera
- Player: Where do I start looking?
- NPC narration: Maera organizes the answer into steps before emotion can scatter it.
- NPC says: Start at the Graveyard near Alderfall. Find Broken Star-Route Map.
- Options:
  1. I know where to start.
  2. What would someone hide in plain sight?
  3. Back to The Wrong Star

#### The Wrong Star / ready / stage 1 of 4

- Stage title: `Inspect The Broken Star-Route Map`
- Objective: `SEARCH` Broken Star-Route Map (1/1)

#### I found what you needed

- NPC says: The map desk keeps producing questions. I have claimed a corner for the dangerous ones.
- Options:
  1. Can we talk about loyalty?
  2. I found what you needed.
  3. Can we talk, Maera?
  4. How does Oathstead feel to you?
  5. What does the forest ask before it lets people pass?
- Player: I found what you needed.
- NPC narration: Maera follows the thought like a line of ink across a dangerous margin.
- NPC says: The missing star was cut out after the ink dried. Damage does not leave neat knife edges. Set it beside the record. I want the lie and the correction in the same light.
- Options:
  1. I found what someone tried to hide.
  2. What does this prove?
  3. What does it cost you to hear this?
  4. What did the answer disturb?
  5. Back to Maera

#### The Wrong Star / accepted / stage 2 of 4

- Stage title: `Compare The Altered Royal Route Record`
- Objective: `SEARCH` Altered Royal Route Record (0/1)

#### Where do things stand -> Where do I start looking

- NPC says: The map desk keeps producing questions. I have claimed a corner for the dangerous ones.
- Options:
  1. Can we talk about loyalty?
  2. Where do things stand with The Wrong Star?
  3. Can we talk, Maera?
  4. How does Oathstead feel to you?
  5. What does the forest ask before it lets people pass?
- Player: Where do things stand with The Wrong Star?
- NPC narration: Maera follows the thought like a line of ink across a dangerous margin.
- NPC says: The star map shows a road the royal record says never existed. Someone cut out the star that proves the road was there. I need the map, the route entry, and Miri's testimony before the Archive files the lie as policy.
- Options:
  1. What truth are you afraid is hidden there?
  2. Where do I start looking?
  3. What did your mother hide from you?
  4. What would someone hide in plain sight?
  5. Back to Maera
- Player: Where do I start looking?
- NPC narration: Maera organizes the answer into steps before emotion can scatter it.
- NPC says: Start at the Graveyard near Alderfall. Find Altered Royal Route Record.
- Options:
  1. I know where to start.
  2. What would someone hide in plain sight?
  3. Back to The Wrong Star

#### The Wrong Star / completed / stage 4 of 4

- Stage title: `Choose The First Citation`
- Objective: `CHOICE` Forbidden Footnote (1/1)

#### What changed after -> What is still unresolved

- NPC says: The map desk keeps producing questions. I have claimed a corner for the dangerous ones.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. What changed after The Wrong Star?
  3. Can we talk, Maera?
  4. How does Oathstead feel to you?
  5. What does the forest ask before it lets people pass?
- Player: What changed after The Wrong Star?
- NPC narration: Maera follows the thought like a line of ink across a dangerous margin. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Good. The first citation now names the map, the route record, and the witness who kept the star from vanishing alone. You kept the scraped star, the altered route entry, and Miri's testimony together. That was the first time this lie had to answer all three at once.
- Options:
  1. Who carries the consequence with me?
  2. Do you think I chose well?
  3. What is still unresolved?
  4. Back to Maera
- Player: What is still unresolved?
- NPC narration: Maera sorts the thought as if it were a page with dangerous margins. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Do not accept the clean version because it is easier to cite. Check the cut mark, the date, and the witness before anyone decides which truth is convenient.
- Options:
  1. Back to Maera

### Representative Conversation Paths

#### Can we talk, -> How are you holding up? -> How are you feeling?

- NPC says: The map desk keeps producing questions. I have claimed a corner for the dangerous ones.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Maera?
  3. How does Oathstead feel to you?
  4. What does the forest ask before it lets people pass?
- Player: Can we talk, Maera?
- NPC says: Maera studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Maera
- Player: How are you holding up?
- NPC narration: Maera sorts the thought as if it were a page with dangerous margins.
- NPC says: You ask as if the answer belongs in the record. I am not sure whether to be annoyed or relieved.
- Options:
  1. How are you feeling?
  2. What should we do next?
  3. Do you need anything?
  4. Back to personal matters
- Player: How are you feeling?
- NPC narration: Maera sorts the thought as if it were a page with dangerous margins.
- NPC says: Like I need another shelf for thoughts I refuse to misfile.
- Options:
  1. I will not push.
  2. Tell me the honest version.
  3. We can keep moving.
  4. Back to personal matters

#### Can we talk, -> How do you see me?

- NPC says: The map desk keeps producing questions. I have claimed a corner for the dangerous ones.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Maera?
  3. How does Oathstead feel to you?
  4. What does the forest ask before it lets people pass?
- Player: Can we talk, Maera?
- NPC says: Maera studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Maera
- Player: How do you see me?
- NPC narration: Maera looks up from the inner record she keeps of you.
- NPC says: My assessment, as honest as a living source permits. I am still deciding what your choices mean. The pattern matters more than one noble moment.
- Options:
  1. What do you trust in me?
  2. What worries you about me?
  3. What should I remember about you?
  4. Back to personal matters

#### Can we talk, -> Do you trust me?

- NPC says: The map desk keeps producing questions. I have claimed a corner for the dangerous ones.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Can we talk, Maera?
  3. How does Oathstead feel to you?
  4. What does the forest ask before it lets people pass?
- Player: Can we talk, Maera?
- NPC says: Maera waits without rushing you. Trust has made the quiet less defensive.
- Options:
  1. Do you trust me?
  2. You keep stealing my attention.
  3. How are you holding up?
  4. How do you see me?
  5. Can we talk about the archive choices?
  6. Do you remember what happened?
  7. What do you think of...?
  8. Back to Maera
- Player: Do you trust me?
- NPC narration: Maera looks up from the inner record she keeps of you. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: I have begun leaving space for you in plans I pretend are only theoretical.
- Options:
  1. Stand with me.
  2. You are free to choose your road.
  3. I am glad you are here.
  4. Good. I need your skills.

#### Can we talk, -> What do you think of -> Me, honestly

- NPC says: The map desk keeps producing questions. I have claimed a corner for the dangerous ones.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Maera?
  3. How does Oathstead feel to you?
  4. What does the forest ask before it lets people pass?
- Player: Can we talk, Maera?
- NPC says: Maera studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Maera
- Player: What do you think of...?
- NPC narration: Maera looks up from the inner record she keeps of you.
- NPC says: Choose whose name you bring into the space between you.
- Options:
  1. Me, honestly.
  2. Aria
  3. Calder
  4. Cassia
  5. Lyra
  6. Rafiq
  7. Samir
  8. Seraphine
  9. Vesper
  10. Back to personal matters
- Player: Me, honestly.
- NPC narration: Maera looks up from the inner record she keeps of you.
- NPC says: You are evidence with boots. Occasionally muddy evidence, but persuasive.
- Options:
  1. That is fair.
  2. You notice more than I thought.
  3. Back to personal matters

#### Can we talk, -> What do you think of -> Known companion

- NPC says: The map desk keeps producing questions. I have claimed a corner for the dangerous ones.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Maera?
  3. How does Oathstead feel to you?
  4. What does the forest ask before it lets people pass?
- Player: Can we talk, Maera?
- NPC says: Maera studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Maera
- Player: What do you think of...?
- NPC narration: Maera looks up from the inner record she keeps of you.
- NPC says: Choose whose name you bring into the space between you.
- Options:
  1. Me, honestly.
  2. Aria
  3. Calder
  4. Cassia
  5. Lyra
  6. Rafiq
  7. Samir
  8. Seraphine
  9. Vesper
  10. Back to personal matters
- Player: Aria
- NPC narration: Maera looks up from the inner record she keeps of you.
- NPC says: Aria reads tracks the way I read margins.
- Options:
  1. Back to conversation

#### How does Oathstead feel -> How does Oathstead feel -> What does Oathstead need next

- NPC says: The map desk keeps producing questions. I have claimed a corner for the dangerous ones.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Can we talk, Maera?
  3. How does Oathstead feel to you?
  4. What does the forest ask before it lets people pass?
- Player: How does Oathstead feel to you?
- NPC says: Maera speaks of Oathstead like a place that may yet learn how to hold people gently.
- Options:
  1. How does Oathstead feel to you?
  2. Back to Maera
- Player: How does Oathstead feel to you?
- NPC narration: Maera softens around the question, as if surprised the page is still blank. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Oathstead's archive is young enough to be honest on purpose. I am trying not to frighten it with standards too quickly.
- Options:
  1. Does this place feel like yours?
  2. How is your work here?
  3. What does Oathstead need next?
  4. Back to home and rest
- Player: What does Oathstead need next?
- NPC narration: Maera organizes the answer into steps before emotion can scatter it. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: A map table, good light, and the courage to correct old routes.
- Options:
  1. Back to Maera

#### Can we talk, -> Come here a moment. -> I missed being close to you.

- NPC says: You arrived before I finished sorting my thoughts. Annoyingly, that may improve them.
- Options:
  1. Can we talk about the future?
  2. Can we talk, Maera?
  3. Stay with me a while.
  4. What does the forest ask before it lets people pass?
- Player: Can we talk, Maera?
- NPC says: Maera lets the pause settle between you, careful and warm.
- Options:
  1. What is this between us?
  2. Stay with me here.
  3. Come here a moment.
  4. How are you holding up?
  5. How do you see me?
  6. Can we talk about the archive choices?
  7. Do you remember what happened?
  8. What do you think of...?
  9. Back to Maera
- Player: Come here a moment.
- NPC narration: Maera softens around the question, as if surprised the page is still blank. There is warmth in it now, not careless, but no longer disguised as tactics.
- NPC says: If you keep looking at me like that, I will lose my place in three separate arguments.
- Options:
  1. I missed being close to you.
  2. I am trying very hard not to memorize your mouth.
  3. Too much?
  4. Back to personal matters
- Player: I missed being close to you.
- NPC narration: Maera softens around the question, as if surprised the page is still blank. There is warmth in it now, not careless, but no longer disguised as tactics.
- NPC says: Good. I have been pretending the same thing was merely tactical positioning.
- Options:
  1. I wanted you to know.
  2. Back to personal matters

#### Can we talk, -> Stay with me here.

- NPC says: You arrived before I finished sorting my thoughts. Annoyingly, that may improve them.
- Options:
  1. Can we talk about the future?
  2. Can we talk, Maera?
  3. Stay with me a while.
  4. What does the forest ask before it lets people pass?
- Player: Can we talk, Maera?
- NPC says: Maera lets the pause settle between you, careful and warm.
- Options:
  1. What is this between us?
  2. Stay with me here.
  3. Come here a moment.
  4. How are you holding up?
  5. How do you see me?
  6. Can we talk about the archive choices?
  7. Do you remember what happened?
  8. What do you think of...?
  9. Back to Maera
- Player: Stay with me here.
- NPC narration: Maera softens around the question, as if surprised the page is still blank. There is warmth in it now, not careless, but no longer disguised as tactics.
- NPC says: the inn table near Oathstead's road has poor archival discipline and excellent potential for being interrupted by honesty.
- Options:
  1. Your footnotes are distracting me.
  2. I wanted time with you, not another task.
  3. Stay here with me.
  4. We can keep this gentle.
  5. Back to personal matters

#### Can we talk, -> Can we talk about our promise?

- NPC says: There you are. I was about to annotate the silence, which would have been undignified.
- Options:
  1. Can we talk, Maera?
  2. Stay with me a while.
  3. What does the forest ask before it lets people pass?
- Player: Can we talk, Maera?
- NPC says: Maera gives you the kind of attention that has learned your silences.
- Options:
  1. Can we talk about our promise?
  2. Stay with me here.
  3. Come here a moment.
  4. How are you holding up?
  5. How do you see me?
  6. Can we talk about the archive choices?
  7. Do you remember what happened?
  8. What do you think of...?
  9. Back to Maera
- Player: Can we talk about our promise?
- NPC narration: Maera looks up from the inner record she keeps of you. The answer comes with the ease of someone who has learned your silences.
- NPC says: My whole life was footnotes and locked shelves. You made a road through them. I would like to keep walking it with you.
- Options:
  1. Come home to me when you can.
  2. Tell me what still frightens you.
  3. The work comes first today.
  4. Back to topics

### Repeat-Aware Sample

#### Can we talk, -> How are you holding up? -> How are you feeling?

- NPC says: The map desk keeps producing questions. I have claimed a corner for the dangerous ones.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Can we talk, Maera?
  3. How does Oathstead feel to you?
  4. What does the forest ask before it lets people pass?
- Player: Can we talk, Maera?
- NPC says: Maera waits without rushing you. Trust has made the quiet less defensive.
- Options:
  1. Do you trust me?
  2. You keep stealing my attention.
  3. How are you holding up?
  4. How do you see me?
  5. Can we talk about the archive choices?
  6. Do you remember what happened?
  7. What do you think of...?
  8. Back to Maera
- Player: How are you holding up?
- NPC narration: Maera sorts the thought as if it were a page with dangerous margins. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: You ask like you intend to hear the answer. That still catches me off guard.
- Options:
  1. How are you feeling?
  2. What should we do next?
  3. Do you need anything?
  4. Back to personal matters
- Player: How are you feeling?
- NPC narration: Maera sorts the thought as if it were a page with dangerous margins. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Overfull. Evidence, fear, fondness. My mental shelves are badly arranged.
- Options:
  1. I will not push.
  2. Tell me the honest version.
  3. We can keep moving.
  4. Back to personal matters


## Rafiq Glass

- Recruit id: `rafiq`
- Starting quest: `The Duelist in Debt`
- Quest stages: 4

### QA Findings

_No QA findings detected in sampled paths._

### Trust Snapshot Roots

#### Trust 0

- NPC says: If anyone asks, I am handling a debt. If anyone armed asks, you have never met me.
- Options:
  1. Can we talk about guarded respect?
  2. Tell me about The Duelist in Debt.
  3. Can we talk, Rafiq?
  4. What do the trees make outsiders learn?

#### Trust 20

- NPC says: If anyone asks, I am handling a debt. If anyone armed asks, you have never met me.
- Options:
  1. Can we talk about guarded respect?
  2. Tell me about The Duelist in Debt.
  3. Can we talk, Rafiq?
  4. What does home mean to you now?
  5. What do the trees make outsiders learn?

#### Trust 50

- NPC says: If anyone asks, I am handling a debt. If anyone armed asks, you have never met me.
- Options:
  1. Can we talk about what changed between us?
  2. Where do things stand with The Duelist in Debt?
  3. Can we talk, Rafiq?
  4. What does home mean to you now?
  5. What do the trees make outsiders learn?

#### Trust 100

- NPC says: The practice yard has survived my standards so far. Heroic little structure.
- Options:
  1. Can we talk about loyalty?
  2. I found what you needed.
  3. Can we talk, Rafiq?
  4. How does Oathstead feel to you?
  5. What do the trees make outsiders learn?

#### Trust 150

- NPC says: The practice yard has survived my standards so far. Heroic little structure.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Where do things stand with The Duelist in Debt?
  3. Can we talk, Rafiq?
  4. How does Oathstead feel to you?
  5. What do the trees make outsiders learn?

#### Trust 180

- NPC says: Ah. My favorite complication has arrived.
- Options:
  1. Can we talk about the future?
  2. Where do things stand with The Duelist in Debt?
  3. Can we talk, Rafiq?
  4. Stay with me a while.
  5. What do the trees make outsiders learn?

#### Trust 250

- NPC says: There you are. I have been terribly composed in your absence. A tragedy.
- Options:
  1. What changed after The Duelist in Debt?
  2. Can we talk, Rafiq?
  3. Stay with me a while.
  4. What do the trees make outsiders learn?

### Quest Stage Branches

#### The Duelist in Debt / offer / stage 1 of 4

- Stage title: `Inspect The Duel Notice`
- Objective: `SEARCH` Duel Notice (0/1)

#### The Duelist in Debt -> What truth are you afraid -> I will help you survive the truth

- NPC says: The practice yard has survived my standards so far. Heroic little structure.
- Options:
  1. Can we talk about loyalty?
  2. Tell me about The Duelist in Debt.
  3. Can we talk, Rafiq?
  4. How does Oathstead feel to you?
  5. What do the trees make outsiders learn?
- Player: Tell me about The Duelist in Debt.
- NPC narration: Rafiq tests a smile, then lets the dangerous truth go first.
- NPC says: If you are here to collect, take a number. If you are here to admire, stand closer.
- Options:
  1. What truth are you afraid is hidden there?
  2. Who wants you dead?
  3. What am I looking for, exactly?
  4. I will help you survive the truth.
  5. Give me the version I can act on.
  6. Back to Rafiq
- Player: What truth are you afraid is hidden there?
- NPC narration: Rafiq tests a smile, then lets the dangerous truth go first.
- NPC says: Charm stopped paying the debt. Now the receipt is ugly. For now: find Duel Notice at the Farmland near Alderfall.
- Options:
  1. And what does that cost you?
  2. What am I looking for, exactly?
  3. I will help you survive the truth.
  4. Back to The Duelist in Debt
- Player: I will help you survive the truth.
- NPC narration: Rafiq almost reaches for a joke, then thinks better of hiding there.
- NPC says: Good. Keep me honest if I start making it charming. For now: find Duel Notice at the Farmland near Alderfall.
- Options:
  1. Back to Rafiq

#### The Duelist in Debt / accepted / stage 1 of 4

- Stage title: `Inspect The Duel Notice`
- Objective: `SEARCH` Duel Notice (0/1)

#### Where do things stand -> Where do I start looking

- NPC says: The practice yard has survived my standards so far. Heroic little structure.
- Options:
  1. Can we talk about loyalty?
  2. Where do things stand with The Duelist in Debt?
  3. Can we talk, Rafiq?
  4. How does Oathstead feel to you?
  5. What do the trees make outsiders learn?
- Player: Where do things stand with The Duelist in Debt?
- NPC narration: Rafiq tests a smile, then lets the dangerous truth go first.
- NPC says: If anyone asks, I am handling a debt. If anyone armed asks, you have never met me.
- Options:
  1. What truth are you afraid is hidden there?
  2. Where do I start looking?
  3. Who wants you dead?
  4. What would someone hide in plain sight?
  5. Back to Rafiq
- Player: Where do I start looking?
- NPC narration: Rafiq gives the answer quickly, before style can get in the way of survival.
- NPC says: Start at the Farmland near Alderfall. Find Duel Notice.
- Options:
  1. I know where to start.
  2. What would someone hide in plain sight?
  3. Back to The Duelist in Debt

#### The Duelist in Debt / ready / stage 1 of 4

- Stage title: `Inspect The Duel Notice`
- Objective: `SEARCH` Duel Notice (1/1)

#### I found what you needed

- NPC says: The practice yard has survived my standards so far. Heroic little structure.
- Options:
  1. Can we talk about loyalty?
  2. I found what you needed.
  3. Can we talk, Rafiq?
  4. How does Oathstead feel to you?
  5. What do the trees make outsiders learn?
- Player: I found what you needed.
- NPC narration: Rafiq tests a smile, then lets the dangerous truth go first.
- NPC says: The notice was posted after the duel was already decided. Show me before I pretend I am ready. I may even tell the truth first.
- Options:
  1. I found what someone tried to hide.
  2. What does this prove?
  3. What does it cost you to hear this?
  4. What did the answer disturb?
  5. Back to Rafiq

#### The Duelist in Debt / accepted / stage 2 of 4

- Stage title: `Inspect The Cracked Glass Token`
- Objective: `SEARCH` Cracked Glass Token (0/1)

#### Where do things stand -> Where do I start looking

- NPC says: The practice yard has survived my standards so far. Heroic little structure.
- Options:
  1. Can we talk about loyalty?
  2. Where do things stand with The Duelist in Debt?
  3. Can we talk, Rafiq?
  4. How does Oathstead feel to you?
  5. What do the trees make outsiders learn?
- Player: Where do things stand with The Duelist in Debt?
- NPC narration: Rafiq tests a smile, then lets the dangerous truth go first.
- NPC says: If anyone asks, I am handling a debt. If anyone armed asks, you have never met me.
- Options:
  1. What truth are you afraid is hidden there?
  2. Where do I start looking?
  3. Who wants you dead?
  4. What would someone hide in plain sight?
  5. Back to Rafiq
- Player: Where do I start looking?
- NPC narration: Rafiq gives the answer quickly, before style can get in the way of survival.
- NPC says: Start at the Farmland near Alderfall. Find Cracked Glass Token.
- Options:
  1. I know where to start.
  2. What would someone hide in plain sight?
  3. Back to The Duelist in Debt

#### The Duelist in Debt / completed / stage 4 of 4

- Stage title: `Choose How The Debt Is Named`
- Objective: `CHOICE` Rafiq's First Debt (1/1)

#### What changed after -> What is still unresolved

- NPC says: The practice yard has survived my standards so far. Heroic little structure.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. What changed after The Duelist in Debt?
  3. Can we talk, Rafiq?
  4. How does Oathstead feel to you?
  5. What do the trees make outsiders learn?
- Player: What changed after The Duelist in Debt?
- NPC narration: Rafiq tests a smile, then lets the dangerous truth go first. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Good. Now I suppose I should stop performing too. Briefly. First debt marker exposed. I dislike how relieved I am.
- Options:
  1. Who carries the consequence with me?
  2. Do you think I chose well?
  3. What is still unresolved?
  4. Back to Rafiq
- Player: What is still unresolved?
- NPC narration: Rafiq gives the answer quickly, before style can get in the way of survival. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Beware the elegant answer. It usually arrives well dressed and carrying a knife.
- Options:
  1. Back to Rafiq

### Representative Conversation Paths

#### Can we talk, -> How are you holding up? -> How are you feeling?

- NPC says: The practice yard has survived my standards so far. Heroic little structure.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Rafiq?
  3. How does Oathstead feel to you?
  4. What do the trees make outsiders learn?
- Player: Can we talk, Rafiq?
- NPC says: Rafiq studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Rafiq
- Player: How are you holding up?
- NPC narration: Rafiq almost reaches for a joke, then thinks better of hiding there.
- NPC says: You are inviting sincerity. Reckless. I will attempt not to ruin it immediately. You ask with the tragic confidence of someone expecting honesty from me.
- Options:
  1. How are you feeling?
  2. What should we do next?
  3. Do you need anything?
  4. Back to personal matters
- Player: How are you feeling?
- NPC narration: Rafiq almost reaches for a joke, then thinks better of hiding there.
- NPC says: Dramatically fine. Which is to say: not fine, but well dressed.
- Options:
  1. I will not push.
  2. Tell me the honest version.
  3. We can keep moving.
  4. Back to personal matters

#### Can we talk, -> How do you see me?

- NPC says: The practice yard has survived my standards so far. Heroic little structure.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Rafiq?
  3. How does Oathstead feel to you?
  4. What do the trees make outsiders learn?
- Player: Can we talk, Rafiq?
- NPC says: Rafiq studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Rafiq
- Player: How do you see me?
- NPC narration: Rafiq lets charm stand aside long enough for honesty to be seen.
- NPC says: I am still deciding what your choices mean. The pattern matters more than one noble moment.
- Options:
  1. What do you trust in me?
  2. What worries you about me?
  3. What should I remember about you?
  4. Back to personal matters

#### Can we talk, -> Do you trust me?

- NPC says: The practice yard has survived my standards so far. Heroic little structure.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Can we talk, Rafiq?
  3. How does Oathstead feel to you?
  4. What do the trees make outsiders learn?
- Player: Can we talk, Rafiq?
- NPC says: Rafiq waits without rushing you. Trust has made the quiet less defensive.
- Options:
  1. Do you trust me?
  2. Flirt with me before you behave.
  3. How are you holding up?
  4. How do you see me?
  5. Do you remember what happened?
  6. What do you think of...?
  7. Back to Rafiq
- Player: Do you trust me?
- NPC narration: Rafiq lets charm stand aside long enough for honesty to be seen. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: I have stopped rehearsing my exit whenever you speak. Alarming. Meaningful.
- Options:
  1. Stand with me.
  2. You are free to choose your road.
  3. I am glad you are here.
  4. Good. I need your skills.

#### Can we talk, -> What do you think of -> Me, honestly

- NPC says: The practice yard has survived my standards so far. Heroic little structure.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Rafiq?
  3. How does Oathstead feel to you?
  4. What do the trees make outsiders learn?
- Player: Can we talk, Rafiq?
- NPC says: Rafiq studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Rafiq
- Player: What do you think of...?
- NPC narration: Rafiq lets charm stand aside long enough for honesty to be seen.
- NPC says: If you want my opinion, I can make it charming or true. Today I will risk true. Choose whose name you bring into the space between you.
- Options:
  1. Me, honestly.
  2. Aria
  3. Calder
  4. Cassia
  5. Lyra
  6. Maera
  7. Samir
  8. Seraphine
  9. Vesper
  10. Back to personal matters
- Player: Me, honestly.
- NPC narration: Rafiq lets charm stand aside long enough for honesty to be seen.
- NPC says: If you want my opinion, I can make it charming or true. Today I will risk true. You make sincerity look survivable. I resent the example and may follow it.
- Options:
  1. That is fair.
  2. You notice more than I thought.
  3. Back to personal matters

#### Can we talk, -> What do you think of -> Known companion

- NPC says: The practice yard has survived my standards so far. Heroic little structure.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Rafiq?
  3. How does Oathstead feel to you?
  4. What do the trees make outsiders learn?
- Player: Can we talk, Rafiq?
- NPC says: Rafiq studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Rafiq
- Player: What do you think of...?
- NPC narration: Rafiq lets charm stand aside long enough for honesty to be seen.
- NPC says: If you want my opinion, I can make it charming or true. Today I will risk true. Choose whose name you bring into the space between you.
- Options:
  1. Me, honestly.
  2. Aria
  3. Calder
  4. Cassia
  5. Lyra
  6. Maera
  7. Samir
  8. Seraphine
  9. Vesper
  10. Back to personal matters
- Player: Aria
- NPC narration: Rafiq lets charm stand aside long enough for honesty to be seen.
- NPC says: If you want my opinion, I can make it charming or true. Today I will risk true. Aria distrusts applause. I am trying to understand this medical condition.
- Options:
  1. Back to conversation

#### How does Oathstead feel -> How does Oathstead feel -> What does Oathstead need next

- NPC says: The practice yard has survived my standards so far. Heroic little structure.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Can we talk, Rafiq?
  3. How does Oathstead feel to you?
  4. What do the trees make outsiders learn?
- Player: How does Oathstead feel to you?
- NPC says: Rafiq speaks of Oathstead like a place that may yet learn how to hold people gently.
- Options:
  1. How does Oathstead feel to you?
  2. Back to Rafiq
- Player: How does Oathstead feel to you?
- NPC narration: Rafiq's humor thins into something warmer and far less practiced. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: I thought a camp would feel temporary. This one keeps asking people to become less temporary with it.
- Options:
  1. Does this place feel like yours?
  2. How is your work here?
  3. What does Oathstead need next?
  4. Back to home and rest
- Player: What does Oathstead need next?
- NPC narration: Rafiq gives the answer quickly, before style can get in the way of survival. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Better drainage, worse music, and a corner where second chances are not announced too loudly.
- Options:
  1. Back to Rafiq

#### Can we talk, -> Come here a moment. -> I missed being close to you.

- NPC says: Ah. My favorite complication has arrived.
- Options:
  1. Can we talk about the future?
  2. Can we talk, Rafiq?
  3. Stay with me a while.
  4. What do the trees make outsiders learn?
- Player: Can we talk, Rafiq?
- NPC says: Rafiq lets the pause settle between you, careful and warm.
- Options:
  1. What is this between us?
  2. Stay with me here.
  3. Come here a moment.
  4. How are you holding up?
  5. How do you see me?
  6. Do you remember what happened?
  7. What do you think of...?
  8. Back to Rafiq
- Player: Come here a moment.
- NPC narration: Rafiq's humor thins into something warmer and far less practiced. There is warmth in it now, not careless, but no longer disguised as tactics.
- NPC says: The honest version of my attention. Bold, dangerous, well dressed. At last, a battlefield suited to my talents. Continue.
- Options:
  1. I missed being close to you.
  2. You are unfairly pretty when you pretend to be sensible.
  3. Too much?
  4. Back to personal matters
- Player: I missed being close to you.
- NPC narration: Rafiq's humor thins into something warmer and far less practiced. There is warmth in it now, not careless, but no longer disguised as tactics.
- NPC says: The honest version of my attention. Bold, dangerous, well dressed. Near is dangerous. Luckily, I have always believed in useful danger.
- Options:
  1. I wanted you to know.
  2. Back to personal matters

#### Can we talk, -> Stay with me here.

- NPC says: Ah. My favorite complication has arrived.
- Options:
  1. Can we talk about the future?
  2. Can we talk, Rafiq?
  3. Stay with me a while.
  4. What do the trees make outsiders learn?
- Player: Can we talk, Rafiq?
- NPC says: Rafiq lets the pause settle between you, careful and warm.
- Options:
  1. What is this between us?
  2. Stay with me here.
  3. Come here a moment.
  4. How are you holding up?
  5. How do you see me?
  6. Do you remember what happened?
  7. What do you think of...?
  8. Back to Rafiq
- Player: Stay with me here.
- NPC narration: Rafiq's humor thins into something warmer and far less practiced. There is warmth in it now, not careless, but no longer disguised as tactics.
- NPC says: The honest version of my attention. Bold, dangerous, well dressed. the inn table near Oathstead's road is almost worthy of us. I will forgive its flaws if you sit close enough.
- Options:
  1. Flirt with me before I become responsible.
  2. I wanted time with you, not another task.
  3. Stay here with me.
  4. We can keep this gentle.
  5. Back to personal matters

#### Can we talk, -> Can we talk about our promise?

- NPC says: There you are. I have been terribly composed in your absence. A tragedy.
- Options:
  1. Can we talk, Rafiq?
  2. Stay with me a while.
  3. What do the trees make outsiders learn?
- Player: Can we talk, Rafiq?
- NPC says: Rafiq gives you the kind of attention that has learned your silences.
- Options:
  1. Can we talk about our promise?
  2. Stay with me here.
  3. Come here a moment.
  4. How are you holding up?
  5. How do you see me?
  6. Do you remember what happened?
  7. What do you think of...?
  8. Back to Rafiq
- Player: Can we talk about our promise?
- NPC narration: Rafiq lets charm stand aside long enough for honesty to be seen. The answer comes with the ease of someone who has learned your silences.
- NPC says: I have run from creditors, duelists, and my own better sense. I am tired. Ask me to stay.
- Options:
  1. Come home to me when you can.
  2. Tell me what still frightens you.
  3. The work comes first today.
  4. Back to topics

### Repeat-Aware Sample

#### Can we talk, -> How are you holding up? -> How are you feeling?

- NPC says: The practice yard has survived my standards so far. Heroic little structure.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Can we talk, Rafiq?
  3. How does Oathstead feel to you?
  4. What do the trees make outsiders learn?
- Player: Can we talk, Rafiq?
- NPC says: Rafiq waits without rushing you. Trust has made the quiet less defensive.
- Options:
  1. Do you trust me?
  2. Flirt with me before you behave.
  3. How are you holding up?
  4. How do you see me?
  5. Do you remember what happened?
  6. What do you think of...?
  7. Back to Rafiq
- Player: How are you holding up?
- NPC narration: Rafiq almost reaches for a joke, then thinks better of hiding there. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: You ask like you intend to hear the answer. That still catches me off guard.
- Options:
  1. How are you feeling?
  2. What should we do next?
  3. Do you need anything?
  4. Back to personal matters
- Player: How are you feeling?
- NPC narration: Rafiq almost reaches for a joke, then thinks better of hiding there. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Tragically sincere. I am enduring it with style.
- Options:
  1. I will not push.
  2. Tell me the honest version.
  3. We can keep moving.
  4. Back to personal matters


## Samir Dawn

- Recruit id: `samir`
- Starting quest: `Cinders in the Reliquary`
- Quest stages: 4

### QA Findings

_No QA findings detected in sampled paths._

### Trust Snapshot Roots

#### Trust 0

- NPC says: The reliquary flame is eating its own shadow. That is either a miracle with poor manners or a warning with teeth.
- Options:
  1. Can we talk about guarded respect?
  2. Tell me about Cinders in the Reliquary.
  3. Can we talk, Samir?
  4. What does the forest ask before it lets people pass?

#### Trust 20

- NPC says: The reliquary flame is eating its own shadow. That is either a miracle with poor manners or a warning with teeth.
- Options:
  1. Can we talk about guarded respect?
  2. Tell me about Cinders in the Reliquary.
  3. Can we talk, Samir?
  4. What does home mean to you now?
  5. What does the forest ask before it lets people pass?

#### Trust 50

- NPC says: The light has moved, and so has the shadow behind it.
- Options:
  1. Can we talk about what changed between us?
  2. Where do things stand with Cinders in the Reliquary?
  3. Can we talk, Samir?
  4. What does home mean to you now?
  5. What does the forest ask before it lets people pass?

#### Trust 100

- NPC says: The lantern post has ordinary work and honest shadows. I find that useful.
- Options:
  1. Can we talk about loyalty?
  2. I found what you needed.
  3. Can we talk, Samir?
  4. How does Oathstead feel to you?
  5. What does the forest ask before it lets people pass?

#### Trust 150

- NPC says: The lantern post has ordinary work and honest shadows. I find that useful.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Where do things stand with Cinders in the Reliquary?
  3. Can we talk, Samir?
  4. How does Oathstead feel to you?
  5. What does the forest ask before it lets people pass?

#### Trust 180

- NPC says: You come in like ordinary light. That is harder to resist than miracles.
- Options:
  1. Can we talk about the future?
  2. Where do things stand with Cinders in the Reliquary?
  3. Can we talk, Samir?
  4. Stay with me a while.
  5. What does the forest ask before it lets people pass?

#### Trust 250

- NPC says: There you are. The room feels less like waiting when you enter it.
- Options:
  1. What changed after Cinders in the Reliquary?
  2. Can we talk, Samir?
  3. Stay with me a while.
  4. What does the forest ask before it lets people pass?

### Quest Stage Branches

#### Cinders in the Reliquary / offer / stage 1 of 4

- Stage title: `Snuff The Reliquary Imps`
- Objective: `DEFEAT` Ember Imp (0/3)

#### Cinders in the Reliquary -> Who gets hurt if this keeps hunting -> I will carry the question

- NPC says: The lantern post has ordinary work and honest shadows. I find that useful.
- Options:
  1. Can we talk about loyalty?
  2. Tell me about Cinders in the Reliquary.
  3. Can we talk, Samir?
  4. How does Oathstead feel to you?
  5. What does the forest ask before it lets people pass?
- Player: Tell me about Cinders in the Reliquary.
- NPC narration: Samir lets the question pass through silence before giving it shape.
- NPC says: My family guarded a reliquary and called it duty. I called it a locked door with hymns.
- Options:
  1. Who gets hurt if this keeps hunting?
  2. What did the light take from you?
  3. What are we up against?
  4. I will carry the question with you.
  5. Give me the version I can act on.
  6. Back to Samir
- Player: Who gets hurt if this keeps hunting?
- NPC narration: Samir lets the question pass through silence before giving it shape.
- NPC says: The light was taught to command instead of reveal. I need to see where it turns wrong. For now: face Ember Imp near the Goblin Camp near Alderfall.
- Options:
  1. And what does that cost you?
  2. What are we up against?
  3. I will carry the question with you.
  4. Back to Cinders in the Reliquary
- Player: I will carry the question with you.
- NPC narration: Samir lets the silence breathe before he answers.
- NPC says: Good. Carry the question with me. For now: face Ember Imp near the Goblin Camp near Alderfall.
- Options:
  1. Back to Samir

#### Cinders in the Reliquary / accepted / stage 1 of 4

- Stage title: `Snuff The Reliquary Imps`
- Objective: `DEFEAT` Ember Imp (1/3)

#### Where do things stand -> Where was it last seen

- NPC says: The lantern post has ordinary work and honest shadows. I find that useful.
- Options:
  1. Can we talk about loyalty?
  2. Where do things stand with Cinders in the Reliquary?
  3. Can we talk, Samir?
  4. How does Oathstead feel to you?
  5. What does the forest ask before it lets people pass?
- Player: Where do things stand with Cinders in the Reliquary?
- NPC narration: Samir lets the question pass through silence before giving it shape.
- NPC says: Snuff the imps before the past becomes smoke. If the evidence troubles the old lesson, trust the trouble.
- Options:
  1. How much danger is still walking?
  2. Where was it last seen?
  3. What did the light take from you?
  4. What am I likely to miss while hunting it?
  5. Back to Samir
- Player: Where was it last seen?
- NPC narration: Samir's voice steadies, light turned toward the work instead of the wound.
- NPC says: Find Ember Imp near the Goblin Camp near Alderfall and stop it.
- Options:
  1. I know where to start.
  2. What am I likely to miss while hunting it?
  3. Back to Cinders in the Reliquary

#### Cinders in the Reliquary / ready / stage 1 of 4

- Stage title: `Snuff The Reliquary Imps`
- Objective: `DEFEAT` Ember Imp (3/3)

#### I found what you needed

- NPC says: The lantern post has ordinary work and honest shadows. I find that useful.
- Options:
  1. Can we talk about loyalty?
  2. I found what you needed.
  3. Can we talk, Samir?
  4. How does Oathstead feel to you?
  5. What does the forest ask before it lets people pass?
- Player: I found what you needed.
- NPC narration: Samir lets the question pass through silence before giving it shape.
- NPC says: The imps are quiet. The reliquary is not. Let me see it plainly. If it burns away certainty, so be it.
- Options:
  1. The threat is down.
  2. What changes now that it cannot hurt anyone?
  3. What does it cost you to hear this?
  4. What grows back after the fear leaves?
  5. Back to Samir

#### Cinders in the Reliquary / accepted / stage 2 of 4

- Stage title: `Inspect The Family Reliquary`
- Objective: `SEARCH` Family Reliquary (0/1)

#### Where do things stand -> Where do I start looking

- NPC says: The lantern post has ordinary work and honest shadows. I find that useful.
- Options:
  1. Can we talk about loyalty?
  2. Where do things stand with Cinders in the Reliquary?
  3. Can we talk, Samir?
  4. How does Oathstead feel to you?
  5. What does the forest ask before it lets people pass?
- Player: Where do things stand with Cinders in the Reliquary?
- NPC narration: Samir lets the question pass through silence before giving it shape.
- NPC says: The reliquary flame is eating its own shadow. That is either a miracle with poor manners or a warning with teeth.
- Options:
  1. What truth are you afraid is hidden there?
  2. Where do I start looking?
  3. What did the light take from you?
  4. What would someone hide in plain sight?
  5. Back to Samir
- Player: Where do I start looking?
- NPC narration: Samir's voice steadies, light turned toward the work instead of the wound.
- NPC says: Start at the Goblin Camp near Alderfall. Find Family Reliquary.
- Options:
  1. I know where to start.
  2. What would someone hide in plain sight?
  3. Back to Cinders in the Reliquary

#### Cinders in the Reliquary / completed / stage 4 of 4

- Stage title: `Choose What The Seal Means`
- Objective: `CHOICE` Reliquary Truth (1/1)

#### What changed after -> What is still unresolved

- NPC says: The lantern post has ordinary work and honest shadows. I find that useful.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. What changed after Cinders in the Reliquary?
  3. Can we talk, Samir?
  4. How does Oathstead feel to you?
  5. What does the forest ask before it lets people pass?
- Player: What changed after Cinders in the Reliquary?
- NPC narration: Samir lets the question pass through silence before giving it shape. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Good. A locked door with hymns is still a locked door. First shadow named. The light can stop pretending it stands alone.
- Options:
  1. Who carries the consequence with me?
  2. Do you think I chose well?
  3. What is still unresolved?
  4. Back to Samir
- Player: What is still unresolved?
- NPC narration: Samir's voice steadies, light turned toward the work instead of the wound. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Do not mistake brightness for honesty. Bad light loves familiar shapes.
- Options:
  1. Back to Samir

### Representative Conversation Paths

#### Can we talk, -> How are you holding up? -> How are you feeling?

- NPC says: The lantern post has ordinary work and honest shadows. I find that useful.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Samir?
  3. How does Oathstead feel to you?
  4. What does the forest ask before it lets people pass?
- Player: Can we talk, Samir?
- NPC says: Samir studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Samir
- Player: How are you holding up?
- NPC narration: Samir lets the silence breathe before he answers.
- NPC says: You ask gently enough that I cannot hide behind doctrine.
- Options:
  1. How are you feeling?
  2. What should we do next?
  3. Do you need anything?
  4. Back to personal matters
- Player: How are you feeling?
- NPC narration: Samir lets the silence breathe before he answers.
- NPC says: Unsteady, but not dimmed.
- Options:
  1. I will not push.
  2. Tell me the honest version.
  3. We can keep moving.
  4. Back to personal matters

#### Can we talk, -> How do you see me?

- NPC says: The lantern post has ordinary work and honest shadows. I find that useful.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Samir?
  3. How does Oathstead feel to you?
  4. What does the forest ask before it lets people pass?
- Player: Can we talk, Samir?
- NPC says: Samir studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Samir
- Player: How do you see me?
- NPC narration: Samir answers without making faith do the thinking for him.
- NPC says: You ask how I see you. I will answer without making faith do the work for me. I am still deciding what your choices mean. The pattern matters more than one noble moment.
- Options:
  1. What do you trust in me?
  2. What worries you about me?
  3. What should I remember about you?
  4. Back to personal matters

#### Can we talk, -> Do you trust me?

- NPC says: The lantern post has ordinary work and honest shadows. I find that useful.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Can we talk, Samir?
  3. How does Oathstead feel to you?
  4. What does the forest ask before it lets people pass?
- Player: Can we talk, Samir?
- NPC says: Samir waits without rushing you. Trust has made the quiet less defensive.
- Options:
  1. Do you trust me?
  2. You make ordinary light difficult.
  3. How are you holding up?
  4. How do you see me?
  5. Do you remember what happened?
  6. What do you think of...?
  7. Back to Samir
- Player: Do you trust me?
- NPC narration: Samir answers without making faith do the thinking for him. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Trust should not kneel. It should look up and choose. I trust your questions near my faith. That is not a small permission.
- Options:
  1. Stand with me.
  2. You are free to choose your road.
  3. I am glad you are here.
  4. Good. I need your skills.

#### Can we talk, -> What do you think of -> Me, honestly

- NPC says: The lantern post has ordinary work and honest shadows. I find that useful.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Samir?
  3. How does Oathstead feel to you?
  4. What does the forest ask before it lets people pass?
- Player: Can we talk, Samir?
- NPC says: Samir studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Samir
- Player: What do you think of...?
- NPC narration: Samir answers without making faith do the thinking for him.
- NPC says: Choose whose name you bring into the space between you.
- Options:
  1. Me, honestly.
  2. Aria
  3. Calder
  4. Cassia
  5. Lyra
  6. Maera
  7. Rafiq
  8. Seraphine
  9. Vesper
  10. Back to personal matters
- Player: Me, honestly.
- NPC narration: Samir answers without making faith do the thinking for him.
- NPC says: You ask for light without demanding obedience from it. That is rarer than doctrine admits.
- Options:
  1. That is fair.
  2. You notice more than I thought.
  3. Back to personal matters

#### Can we talk, -> What do you think of -> Known companion

- NPC says: The lantern post has ordinary work and honest shadows. I find that useful.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Samir?
  3. How does Oathstead feel to you?
  4. What does the forest ask before it lets people pass?
- Player: Can we talk, Samir?
- NPC says: Samir studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Samir
- Player: What do you think of...?
- NPC narration: Samir answers without making faith do the thinking for him.
- NPC says: Choose whose name you bring into the space between you.
- Options:
  1. Me, honestly.
  2. Aria
  3. Calder
  4. Cassia
  5. Lyra
  6. Maera
  7. Rafiq
  8. Seraphine
  9. Vesper
  10. Back to personal matters
- Player: Aria
- NPC narration: Samir answers without making faith do the thinking for him.
- NPC says: Aria survives by reading shadows. I hope one day she trusts dawn too.
- Options:
  1. Back to conversation

#### How does Oathstead feel -> How does Oathstead feel -> What does Oathstead need next

- NPC says: The lantern post has ordinary work and honest shadows. I find that useful.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Can we talk, Samir?
  3. How does Oathstead feel to you?
  4. What does the forest ask before it lets people pass?
- Player: How does Oathstead feel to you?
- NPC says: Samir speaks of Oathstead like a place that may yet learn how to hold people gently.
- Options:
  1. How does Oathstead feel to you?
  2. Back to Samir
- Player: How does Oathstead feel to you?
- NPC narration: Samir looks at you as if warmth itself has become a question worth honoring. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: I thought a camp would feel temporary. This one keeps asking people to become less temporary with it.
- Options:
  1. Does this place feel like yours?
  2. How is your work here?
  3. What does Oathstead need next?
  4. Back to home and rest
- Player: What does Oathstead need next?
- NPC narration: Samir's voice steadies, light turned toward the work instead of the wound. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: A light people can gather around without being ordered to kneel.
- Options:
  1. Back to Samir

#### Can we talk, -> Come here a moment. -> I missed being close to you.

- NPC says: You come in like ordinary light. That is harder to resist than miracles.
- Options:
  1. Can we talk about the future?
  2. Can we talk, Samir?
  3. Stay with me a while.
  4. What does the forest ask before it lets people pass?
- Player: Can we talk, Samir?
- NPC says: Samir lets the pause settle between you, careful and warm.
- Options:
  1. What is this between us?
  2. Stay with me here.
  3. Come here a moment.
  4. How are you holding up?
  5. How do you see me?
  6. Do you remember what happened?
  7. What do you think of...?
  8. Back to Samir
- Player: Come here a moment.
- NPC narration: Samir looks at you as if warmth itself has become a question worth honoring. There is warmth in it now, not careless, but no longer disguised as tactics.
- NPC says: Warmth without worship. That is rarer than it should be. Do not make me call this temptation. I am trying to have a better theology about your eyes.
- Options:
  1. I missed being close to you.
  2. If this is temptation, I am becoming fond of the theology.
  3. Too much?
  4. Back to personal matters
- Player: I missed being close to you.
- NPC narration: Samir looks at you as if warmth itself has become a question worth honoring. There is warmth in it now, not careless, but no longer disguised as tactics.
- NPC says: Warmth without worship. That is rarer than it should be. Then stay near without making it a sermon. I would like that better.
- Options:
  1. I wanted you to know.
  2. Back to personal matters

#### Can we talk, -> Stay with me here.

- NPC says: You come in like ordinary light. That is harder to resist than miracles.
- Options:
  1. Can we talk about the future?
  2. Can we talk, Samir?
  3. Stay with me a while.
  4. What does the forest ask before it lets people pass?
- Player: Can we talk, Samir?
- NPC says: Samir lets the pause settle between you, careful and warm.
- Options:
  1. What is this between us?
  2. Stay with me here.
  3. Come here a moment.
  4. How are you holding up?
  5. How do you see me?
  6. Do you remember what happened?
  7. What do you think of...?
  8. Back to Samir
- Player: Stay with me here.
- NPC narration: Samir looks at you as if warmth itself has become a question worth honoring. There is warmth in it now, not careless, but no longer disguised as tactics.
- NPC says: Warmth without worship. That is rarer than it should be. The light in the inn table near Oathstead's road is ordinary. That makes what I feel near you harder to blame on miracles.
- Options:
  1. You are brighter than my better judgment.
  2. I wanted time with you, not another task.
  3. Stay here with me.
  4. We can keep this gentle.
  5. Back to personal matters

#### Can we talk, -> Can we talk about our promise?

- NPC says: There you are. The room feels less like waiting when you enter it.
- Options:
  1. Can we talk, Samir?
  2. Stay with me a while.
  3. What does the forest ask before it lets people pass?
- Player: Can we talk, Samir?
- NPC says: Samir gives you the kind of attention that has learned your silences.
- Options:
  1. Can we talk about our promise?
  2. Stay with me here.
  3. Come here a moment.
  4. How are you holding up?
  5. How do you see me?
  6. Do you remember what happened?
  7. What do you think of...?
  8. Back to Samir
- Player: Can we talk about our promise?
- NPC narration: Samir answers without making faith do the thinking for him. The answer comes with the ease of someone who has learned your silences.
- NPC says: Trust should not kneel. It should look up and choose. I once mistook devotion for surrender. With you, devotion feels awake.
- Options:
  1. Come home to me when you can.
  2. Tell me what still frightens you.
  3. The work comes first today.
  4. Back to topics

### Repeat-Aware Sample

#### Can we talk, -> How are you holding up? -> How are you feeling?

- NPC says: The lantern post has ordinary work and honest shadows. I find that useful.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Can we talk, Samir?
  3. How does Oathstead feel to you?
  4. What does the forest ask before it lets people pass?
- Player: Can we talk, Samir?
- NPC says: Samir waits without rushing you. Trust has made the quiet less defensive.
- Options:
  1. Do you trust me?
  2. You make ordinary light difficult.
  3. How are you holding up?
  4. How do you see me?
  5. Do you remember what happened?
  6. What do you think of...?
  7. Back to Samir
- Player: How are you holding up?
- NPC narration: Samir lets the silence breathe before he answers. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: You ask like you intend to hear the answer. That still catches me off guard.
- Options:
  1. How are you feeling?
  2. What should we do next?
  3. Do you need anything?
  4. Back to personal matters
- Player: How are you feeling?
- NPC narration: Samir lets the silence breathe before he answers. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Bright in places I used to keep locked. Frightening, but not unwelcome.
- Options:
  1. I will not push.
  2. Tell me the honest version.
  3. We can keep moving.
  4. Back to personal matters


## Seraphine Vale

- Recruit id: `seraphine`
- Starting quest: `The Clause In Red Silk`
- Quest stages: 3

### QA Findings

_No QA findings detected in sampled paths._

### Trust Snapshot Roots

#### Trust 0

- NPC says: Before you do anything heroic, know this: the drawer was already open when I found it. Probably.
- Options:
  1. Can we talk about guarded respect?
  2. Tell me about The Clause In Red Silk.
  3. Can we talk, Seraphine?
  4. What do the trees make outsiders learn?

#### Trust 20

- NPC says: Before you do anything heroic, know this: the drawer was already open when I found it. Probably.
- Options:
  1. Can we talk about guarded respect?
  2. Tell me about The Clause In Red Silk.
  3. Can we talk, Seraphine?
  4. What does home mean to you now?
  5. What do the trees make outsiders learn?

#### Trust 50

- NPC says: Before you do anything heroic, know this: the drawer was already open when I found it. Probably.
- Options:
  1. Can we talk about what changed between us?
  2. Where do things stand with The Clause In Red Silk?
  3. Can we talk, Seraphine?
  4. What does home mean to you now?
  5. What do the trees make outsiders learn?

#### Trust 100

- NPC says: The ledger room has fewer hidden clauses than I expected. I am improving it anyway.
- Options:
  1. Can we talk about loyalty?
  2. I found what you needed.
  3. Can we talk, Seraphine?
  4. How does Oathstead feel to you?
  5. What do the trees make outsiders learn?

#### Trust 150

- NPC says: The ledger room has fewer hidden clauses than I expected. I am improving it anyway.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Where do things stand with The Clause In Red Silk?
  3. Can we talk, Seraphine?
  4. How does Oathstead feel to you?
  5. What do the trees make outsiders learn?

#### Trust 180

- NPC says: You have terrible timing. I am beginning to suspect I like being interrupted by you.
- Options:
  1. Can we talk about the future?
  2. Where do things stand with The Clause In Red Silk?
  3. Can we talk, Seraphine?
  4. Stay with me a while.
  5. What do the trees make outsiders learn?

#### Trust 250

- NPC says: There you are. No summons, no contract, and still you came back. I remain suspiciously fond of that.
- Options:
  1. What changed after The Clause In Red Silk?
  2. Can we talk, Seraphine?
  3. Stay with me a while.
  4. What do the trees make outsiders learn?

### Quest Stage Branches

#### The Clause In Red Silk / offer / stage 1 of 3

- Stage title: `Read The Riverside Toll Ledger`
- Objective: `SEARCH` Riverside Court Toll Ledger (0/1)

#### The Clause In Red Silk -> What truth are you afraid -> I will help, but no pretty promises

- NPC says: The ledger room has fewer hidden clauses than I expected. I am improving it anyway.
- Options:
  1. Can we talk about loyalty?
  2. Tell me about The Clause In Red Silk.
  3. Can we talk, Seraphine?
  4. How does Oathstead feel to you?
  5. What do the trees make outsiders learn?
- Player: Tell me about The Clause In Red Silk.
- NPC narration: Seraphine studies the wording as if the lie might be hiding in the punctuation.
- NPC says: Riverside court keeps its cruelties near the river windows. Better light for lying.
- Options:
  1. What truth are you afraid is hidden there?
  2. What are you stealing?
  3. What am I looking for, exactly?
  4. I will help, but no pretty promises.
  5. Give me the version I can act on.
  6. Back to Seraphine
- Player: What truth are you afraid is hidden there?
- NPC narration: Seraphine studies the wording as if the lie might be hiding in the punctuation.
- NPC says: Someone made harm look properly filed. I want the first lie exposed. For now: find Riverside Court Toll Ledger at the Farmland near Alderfall.
- Options:
  1. And what does that cost you?
  2. What am I looking for, exactly?
  3. I will help, but no pretty promises.
  4. Back to The Clause In Red Silk
- Player: I will help, but no pretty promises.
- NPC narration: Seraphine weighs your wording before she lets it pass.
- NPC says: Good. No pretty promises. For now: find Riverside Court Toll Ledger at the Farmland near Alderfall.
- Options:
  1. Back to Seraphine

#### The Clause In Red Silk / accepted / stage 1 of 3

- Stage title: `Read The Riverside Toll Ledger`
- Objective: `SEARCH` Riverside Court Toll Ledger (0/1)

#### Where do things stand -> Where do I start looking

- NPC says: The ledger room has fewer hidden clauses than I expected. I am improving it anyway.
- Options:
  1. Can we talk about loyalty?
  2. Where do things stand with The Clause In Red Silk?
  3. Can we talk, Seraphine?
  4. How does Oathstead feel to you?
  5. What do the trees make outsiders learn?
- Player: Where do things stand with The Clause In Red Silk?
- NPC narration: Seraphine studies the wording as if the lie might be hiding in the punctuation.
- NPC says: Before you do anything heroic, know this: the drawer was already open when I found it. Probably.
- Options:
  1. What truth are you afraid is hidden there?
  2. Where do I start looking?
  3. What are you stealing?
  4. What would someone hide in plain sight?
  5. Back to Seraphine
- Player: Where do I start looking?
- NPC narration: Seraphine's answer sharpens into instructions, all velvet stripped from the edge.
- NPC says: Start at the Farmland near Alderfall. Find Riverside Court Toll Ledger.
- Options:
  1. I know where to start.
  2. What would someone hide in plain sight?
  3. Back to The Clause In Red Silk

#### The Clause In Red Silk / ready / stage 1 of 3

- Stage title: `Read The Riverside Toll Ledger`
- Objective: `SEARCH` Riverside Court Toll Ledger (1/1)

#### I found what you needed

- NPC says: The ledger room has fewer hidden clauses than I expected. I am improving it anyway.
- Options:
  1. Can we talk about loyalty?
  2. I found what you needed.
  3. Can we talk, Seraphine?
  4. How does Oathstead feel to you?
  5. What do the trees make outsiders learn?
- Player: I found what you needed.
- NPC narration: Seraphine studies the wording as if the lie might be hiding in the punctuation.
- NPC says: The ledger charges dead accounts for river crossings they can no longer make. Put it where I can see the fraud without letting rage edit the evidence.
- Options:
  1. I found what someone tried to hide.
  2. What does this prove?
  3. What does it cost you to hear this?
  4. What did the answer disturb?
  5. Back to Seraphine

#### The Clause In Red Silk / accepted / stage 2 of 3

- Stage title: `Find The Red-Silk Clause`
- Objective: `SEARCH` Red-Silk Contract Clause (0/1)

#### Where do things stand -> Where do I start looking

- NPC says: The ledger room has fewer hidden clauses than I expected. I am improving it anyway.
- Options:
  1. Can we talk about loyalty?
  2. Where do things stand with The Clause In Red Silk?
  3. Can we talk, Seraphine?
  4. How does Oathstead feel to you?
  5. What do the trees make outsiders learn?
- Player: Where do things stand with The Clause In Red Silk?
- NPC narration: Seraphine studies the wording as if the lie might be hiding in the punctuation.
- NPC says: Before you do anything heroic, know this: the drawer was already open when I found it. Probably.
- Options:
  1. What truth are you afraid is hidden there?
  2. Where do I start looking?
  3. What are you stealing?
  4. What would someone hide in plain sight?
  5. Back to Seraphine
- Player: Where do I start looking?
- NPC narration: Seraphine's answer sharpens into instructions, all velvet stripped from the edge.
- NPC says: Start at the Farmland near Alderfall. Find Red-Silk Contract Clause.
- Options:
  1. I know where to start.
  2. What would someone hide in plain sight?
  3. Back to The Clause In Red Silk

#### The Clause In Red Silk / completed / stage 3 of 3

- Stage title: `Choose The First Lie`
- Objective: `CHOICE` Counting House Evidence (1/1)

#### What changed after -> What is still unresolved

- NPC says: The ledger room has fewer hidden clauses than I expected. I am improving it anyway.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. What changed after The Clause In Red Silk?
  3. Can we talk, Seraphine?
  4. How does Oathstead feel to you?
  5. What do the trees make outsiders learn?
- Player: What changed after The Clause In Red Silk?
- NPC narration: Seraphine studies the wording as if the lie might be hiding in the punctuation. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: You read the clause before reaching for the match. I noticed. First page found. Now the contract knows someone is reading back.
- Options:
  1. That choice still follows us, doesn't it?
  2. Who carries the consequence with me?
  3. Do you think I chose well?
  4. What is still unresolved?
  5. Back to Seraphine
- Player: What is still unresolved?
- NPC narration: Seraphine weighs your wording before she lets it pass. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Do not trust the official shape of the harm. If it looks too clean, someone paid for the polish.
- Options:
  1. Back to Seraphine

### Representative Conversation Paths

#### Can we talk, -> How are you holding up? -> How are you feeling?

- NPC says: The ledger room has fewer hidden clauses than I expected. I am improving it anyway.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Seraphine?
  3. How does Oathstead feel to you?
  4. What do the trees make outsiders learn?
- Player: Can we talk, Seraphine?
- NPC says: Seraphine studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Seraphine
- Player: How are you holding up?
- NPC narration: Seraphine weighs your wording before she lets it pass.
- NPC says: Most people ask that when they want leverage. You keep forgetting the leverage.
- Options:
  1. How are you feeling?
  2. What should we do next?
  3. Do you need anything?
  4. Back to personal matters
- Player: How are you feeling?
- NPC narration: Seraphine weighs your wording before she lets it pass.
- NPC says: Threadbare in places I prefer to keep expensive-looking.
- Options:
  1. I will not push.
  2. Tell me the honest version.
  3. We can keep moving.
  4. Back to personal matters

#### Can we talk, -> How do you see me?

- NPC says: The ledger room has fewer hidden clauses than I expected. I am improving it anyway.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Seraphine?
  3. How does Oathstead feel to you?
  4. What do the trees make outsiders learn?
- Player: Can we talk, Seraphine?
- NPC says: Seraphine studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Seraphine
- Player: How do you see me?
- NPC narration: Seraphine lets the truth sit on the table without dressing it up.
- NPC says: If you want my honest measure of you, I will not hide it under pretty language. I am still deciding what your choices mean. The pattern matters more than one noble moment.
- Options:
  1. What do you trust in me?
  2. What worries you about me?
  3. What should I remember about you?
  4. Back to personal matters

#### Can we talk, -> Do you trust me?

- NPC says: The ledger room has fewer hidden clauses than I expected. I am improving it anyway.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Can we talk, Seraphine?
  3. How does Oathstead feel to you?
  4. What do the trees make outsiders learn?
- Player: Can we talk, Seraphine?
- NPC says: Seraphine waits without rushing you. Trust has made the quiet less defensive.
- Options:
  1. Do you trust me?
  2. No contracts. Just you and me.
  3. How are you holding up?
  4. How do you see me?
  5. Can we talk about the Vale choices?
  6. Do you remember what happened?
  7. What do you think of...?
  8. Back to Seraphine
- Player: Do you trust me?
- NPC narration: Seraphine lets the truth sit on the table without dressing it up. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Trust, then. No contracts, no ownership, no hidden ink. I keep choosing your side without checking for a loophole first. Disturbing progress.
- Options:
  1. Stand with me.
  2. You are free to choose your road.
  3. I am glad you are here.
  4. Good. I need your skills.

#### Can we talk, -> What do you think of -> Me, honestly

- NPC says: The ledger room has fewer hidden clauses than I expected. I am improving it anyway.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Seraphine?
  3. How does Oathstead feel to you?
  4. What do the trees make outsiders learn?
- Player: Can we talk, Seraphine?
- NPC says: Seraphine studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Seraphine
- Player: What do you think of...?
- NPC narration: Seraphine lets the truth sit on the table without dressing it up.
- NPC says: Since you brought a name to the table, I will not pretend neutrality is the same as truth. Choose whose name you bring into the space between you.
- Options:
  1. Me, honestly.
  2. Aria
  3. Calder
  4. Cassia
  5. Lyra
  6. Maera
  7. Rafiq
  8. Samir
  9. Vesper
  10. Back to personal matters
- Player: Me, honestly.
- NPC narration: Seraphine lets the truth sit on the table without dressing it up.
- NPC says: Since you brought a name to the table, I will not pretend neutrality is the same as truth. You walk into old contracts like they are doors. I like that you keep checking for locks.
- Options:
  1. That is fair.
  2. You notice more than I thought.
  3. Back to personal matters

#### Can we talk, -> What do you think of -> Known companion

- NPC says: The ledger room has fewer hidden clauses than I expected. I am improving it anyway.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Seraphine?
  3. How does Oathstead feel to you?
  4. What do the trees make outsiders learn?
- Player: Can we talk, Seraphine?
- NPC says: Seraphine studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Seraphine
- Player: What do you think of...?
- NPC narration: Seraphine lets the truth sit on the table without dressing it up.
- NPC says: Since you brought a name to the table, I will not pretend neutrality is the same as truth. Choose whose name you bring into the space between you.
- Options:
  1. Me, honestly.
  2. Aria
  3. Calder
  4. Cassia
  5. Lyra
  6. Maera
  7. Rafiq
  8. Samir
  9. Vesper
  10. Back to personal matters
- Player: Aria
- NPC narration: Seraphine lets the truth sit on the table without dressing it up.
- NPC says: Since you brought a name to the table, I will not pretend neutrality is the same as truth. Aria notices exits first. I respect a woman with priorities.
- Options:
  1. Back to conversation

#### How does Oathstead feel -> How does Oathstead feel -> What does Oathstead need next

- NPC says: The ledger room has fewer hidden clauses than I expected. I am improving it anyway.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Can we talk, Seraphine?
  3. How does Oathstead feel to you?
  4. What do the trees make outsiders learn?
- Player: How does Oathstead feel to you?
- NPC says: Seraphine speaks of Oathstead like a place that may yet learn how to hold people gently.
- Options:
  1. How does Oathstead feel to you?
  2. Back to Seraphine
- Player: How does Oathstead feel to you?
- NPC narration: Seraphine allows the pause to become almost tender before she speaks. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Oathstead has not asked me to sign away a single part of myself. Suspicious. Generous. I am learning the difference.
- Options:
  1. Does this place feel like yours?
  2. How is your work here?
  3. What does Oathstead need next?
  4. Back to home and rest
- Player: What does Oathstead need next?
- NPC narration: Seraphine's answer sharpens into instructions, all velvet stripped from the edge. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Clear agreements. Fair shares. No paper that owns a person.
- Options:
  1. Back to Seraphine

#### Can we talk, -> Come here a moment. -> I missed being close to you.

- NPC says: You have terrible timing. I am beginning to suspect I like being interrupted by you.
- Options:
  1. Can we talk about the future?
  2. Can we talk, Seraphine?
  3. Stay with me a while.
  4. What do the trees make outsiders learn?
- Player: Can we talk, Seraphine?
- NPC says: Seraphine lets the pause settle between you, careful and warm.
- Options:
  1. What is this between us?
  2. Stay with me here.
  3. Come here a moment.
  4. How are you holding up?
  5. How do you see me?
  6. Can we talk about the Vale choices?
  7. Do you remember what happened?
  8. What do you think of...?
  9. Back to Seraphine
- Player: Come here a moment.
- NPC narration: Seraphine allows the pause to become almost tender before she speaks. There is warmth in it now, not careless, but no longer disguised as tactics.
- NPC says: The uncontracted part of me, then. Careful. That part has teeth. Careful. That sounded almost like desire without paperwork. I may need to hear it again.
- Options:
  1. I missed being close to you.
  2. You are impossible to negotiate with when you look at me like that.
  3. Too much?
  4. Back to personal matters
- Player: I missed being close to you.
- NPC narration: Seraphine allows the pause to become almost tender before she speaks. There is warmth in it now, not careless, but no longer disguised as tactics.
- NPC says: The uncontracted part of me, then. Careful. That part has teeth. Near is acceptable. Near does not own. Near chooses, and I like choices.
- Options:
  1. I wanted you to know.
  2. Back to personal matters

#### Can we talk, -> Stay with me here.

- NPC says: You have terrible timing. I am beginning to suspect I like being interrupted by you.
- Options:
  1. Can we talk about the future?
  2. Can we talk, Seraphine?
  3. Stay with me a while.
  4. What do the trees make outsiders learn?
- Player: Can we talk, Seraphine?
- NPC says: Seraphine lets the pause settle between you, careful and warm.
- Options:
  1. What is this between us?
  2. Stay with me here.
  3. Come here a moment.
  4. How are you holding up?
  5. How do you see me?
  6. Can we talk about the Vale choices?
  7. Do you remember what happened?
  8. What do you think of...?
  9. Back to Seraphine
- Player: Stay with me here.
- NPC narration: Seraphine allows the pause to become almost tender before she speaks. There is warmth in it now, not careless, but no longer disguised as tactics.
- NPC says: The uncontracted part of me, then. Careful. That part has teeth. A quiet hour at the inn table near Oathstead's road, with no one selling, signing, or owning anything. Suspiciously luxurious.
- Options:
  1. No hidden clauses. I just want you.
  2. I wanted time with you, not another task.
  3. Stay here with me.
  4. We can keep this gentle.
  5. Back to personal matters

#### Can we talk, -> Can we talk about our promise?

- NPC says: There you are. No summons, no contract, and still you came back. I remain suspiciously fond of that.
- Options:
  1. Can we talk, Seraphine?
  2. Stay with me a while.
  3. What do the trees make outsiders learn?
- Player: Can we talk, Seraphine?
- NPC says: Seraphine gives you the kind of attention that has learned your silences.
- Options:
  1. Can we talk about our promise?
  2. Stay with me here.
  3. Come here a moment.
  4. How are you holding up?
  5. How do you see me?
  6. Can we talk about the Vale choices?
  7. Do you remember what happened?
  8. What do you think of...?
  9. Back to Seraphine
- Player: Can we talk about our promise?
- NPC narration: Seraphine lets the truth sit on the table without dressing it up. The answer comes with the ease of someone who has learned your silences.
- NPC says: Trust, then. No contracts, no ownership, no hidden ink. I spent years burning contracts that called themselves promises. If you ask for one now, make it ours and make it honest.
- Options:
  1. Come home to me when you can.
  2. Tell me what still frightens you.
  3. The work comes first today.
  4. Back to topics

### Repeat-Aware Sample

#### Can we talk, -> How are you holding up? -> How are you feeling?

- NPC says: The ledger room has fewer hidden clauses than I expected. I am improving it anyway.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Can we talk, Seraphine?
  3. How does Oathstead feel to you?
  4. What do the trees make outsiders learn?
- Player: Can we talk, Seraphine?
- NPC says: Seraphine waits without rushing you. Trust has made the quiet less defensive.
- Options:
  1. Do you trust me?
  2. No contracts. Just you and me.
  3. How are you holding up?
  4. How do you see me?
  5. Can we talk about the Vale choices?
  6. Do you remember what happened?
  7. What do you think of...?
  8. Back to Seraphine
- Player: How are you holding up?
- NPC narration: Seraphine weighs your wording before she lets it pass. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: You ask like you intend to hear the answer. That still catches me off guard.
- Options:
  1. How are you feeling?
  2. What should we do next?
  3. Do you need anything?
  4. Back to personal matters
- Player: How are you feeling?
- NPC narration: Seraphine weighs your wording before she lets it pass. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Less owned by old ink. More annoyed by how much that matters.
- Options:
  1. I will not push.
  2. Tell me the honest version.
  3. We can keep moving.
  4. Back to personal matters


## Vesper Snowroot

- Recruit id: `vesper`
- Starting quest: `The Green Under White`
- Quest stages: 3

### QA Findings

_No QA findings detected in sampled paths._

### Trust Snapshot Roots

#### Trust 0

- NPC says: Snow does not hide things. People hide things and then act surprised when spring has questions.
- Options:
  1. Can we talk about guarded respect?
  2. Tell me about The Green Under White.
  3. Can we talk, Vesper?
  4. What should I avoid answering after dusk?

#### Trust 20

- NPC says: Snow does not hide things. People hide things and then act surprised when spring has questions.
- Options:
  1. Can we talk about guarded respect?
  2. Tell me about The Green Under White.
  3. Can we talk, Vesper?
  4. What does home mean to you now?
  5. What should I avoid answering after dusk?

#### Trust 50

- NPC says: Snow does not hide things. People hide things and then act surprised when spring has questions.
- Options:
  1. Can we talk about what changed between us?
  2. Where do things stand with The Green Under White?
  3. Can we talk, Vesper?
  4. What does home mean to you now?
  5. What should I avoid answering after dusk?

#### Trust 100

- NPC says: The winter garden is learning what grows when people stop mistaking shelter for ownership.
- Options:
  1. Can we talk about loyalty?
  2. I found what you needed.
  3. Can we talk, Vesper?
  4. How does Oathstead feel to you?
  5. What should I avoid answering after dusk?

#### Trust 150

- NPC says: The winter garden is learning what grows when people stop mistaking shelter for ownership.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Where do things stand with The Green Under White?
  3. Can we talk, Vesper?
  4. How does Oathstead feel to you?
  5. What should I avoid answering after dusk?

#### Trust 180

- NPC says: You are early. Or I am glad. I have not decided which is safer.
- Options:
  1. Can we talk about the future?
  2. Where do things stand with The Green Under White?
  3. Can we talk, Vesper?
  4. Stay with me a while.
  5. What should I avoid answering after dusk?

#### Trust 250

- NPC says: There you are. I kept the warm place beside me without naming why.
- Options:
  1. What changed after The Green Under White?
  2. Can we talk, Vesper?
  3. Stay with me a while.
  4. What should I avoid answering after dusk?

### Quest Stage Branches

#### The Green Under White / offer / stage 1 of 3

- Stage title: `Read The Root Circle`
- Objective: `SEARCH` Snowroot Circle (0/1)

#### The Green Under White -> What truth are you afraid -> I will inspect the road breaks before cutting roots

- NPC says: The winter garden is learning what grows when people stop mistaking shelter for ownership.
- Options:
  1. Can we talk about loyalty?
  2. Tell me about The Green Under White.
  3. Can we talk, Vesper?
  4. How does Oathstead feel to you?
  5. What should I avoid answering after dusk?
- Player: Tell me about The Green Under White.
- NPC narration: Vesper listens as if the ground beneath the words has shifted.
- NPC says: Step lightly. Snow is only quiet because it is listening.
- Options:
  1. What truth are you afraid is hidden there?
  2. What did your family bury?
  3. What am I looking for, exactly?
  4. I will inspect the road breaks before cutting roots.
  5. Give me the version I can act on.
  6. Back to Vesper
- Player: What truth are you afraid is hidden there?
- NPC narration: Vesper listens as if the ground beneath the words has shifted.
- NPC says: Something survived under the cold, but survival does not make it simple. For now: find Snowroot Circle at the Graveyard near Alderfall.
- Options:
  1. And what does that cost you?
  2. What am I looking for, exactly?
  3. I will inspect the road breaks before cutting roots.
  4. Back to The Green Under White
- Player: I will inspect the road breaks before cutting roots.
- NPC narration: Vesper grows still, as if listening for the root under the words.
- NPC says: Good. Step lightly around the sealed grove and the roots it sent into the road. For now: find Snowroot Circle at the Graveyard near Alderfall.
- Options:
  1. Back to Vesper

#### The Green Under White / accepted / stage 1 of 3

- Stage title: `Read The Root Circle`
- Objective: `SEARCH` Snowroot Circle (0/1)

#### Where do things stand -> Where do I start looking

- NPC says: The winter garden is learning what grows when people stop mistaking shelter for ownership.
- Options:
  1. Can we talk about loyalty?
  2. Where do things stand with The Green Under White?
  3. Can we talk, Vesper?
  4. How does Oathstead feel to you?
  5. What should I avoid answering after dusk?
- Player: Where do things stand with The Green Under White?
- NPC narration: Vesper listens as if the ground beneath the words has shifted.
- NPC says: Snow does not hide things. People hide things and then act surprised when spring has questions.
- Options:
  1. What truth are you afraid is hidden there?
  2. Where do I start looking?
  3. What did your family bury?
  4. What would someone hide in plain sight?
  5. Back to Vesper
- Player: Where do I start looking?
- NPC narration: Vesper trims the answer down to what can survive winter.
- NPC says: Start at the Graveyard near Alderfall. Find Snowroot Circle.
- Options:
  1. I know where to start.
  2. What would someone hide in plain sight?
  3. Back to The Green Under White

#### The Green Under White / ready / stage 1 of 3

- Stage title: `Read The Root Circle`
- Objective: `SEARCH` Snowroot Circle (1/1)

#### I found what you needed

- NPC says: The winter garden is learning what grows when people stop mistaking shelter for ownership.
- Options:
  1. Can we talk about loyalty?
  2. I found what you needed.
  3. Can we talk, Vesper?
  4. How does Oathstead feel to you?
  5. What should I avoid answering after dusk?
- Player: I found what you needed.
- NPC narration: Vesper listens as if the ground beneath the words has shifted.
- NPC says: The roots woke too early. That is rarely joy. Lay the evidence down plainly. If we make it pretty too soon, we will miss where it points.
- Options:
  1. I found what someone tried to hide.
  2. What does this prove?
  3. What does it cost you to hear this?
  4. What did the answer disturb?
  5. Back to Vesper

#### The Green Under White / accepted / stage 2 of 3

- Stage title: `Inspect The Frozen Seed Bowl`
- Objective: `SEARCH` Frozen Seed Bowl (0/1)

#### Where do things stand -> Where do I start looking

- NPC says: The winter garden is learning what grows when people stop mistaking shelter for ownership.
- Options:
  1. Can we talk about loyalty?
  2. Where do things stand with The Green Under White?
  3. Can we talk, Vesper?
  4. How does Oathstead feel to you?
  5. What should I avoid answering after dusk?
- Player: Where do things stand with The Green Under White?
- NPC narration: Vesper listens as if the ground beneath the words has shifted.
- NPC says: Snow does not hide things. People hide things and then act surprised when spring has questions.
- Options:
  1. What truth are you afraid is hidden there?
  2. Where do I start looking?
  3. What did your family bury?
  4. What would someone hide in plain sight?
  5. Back to Vesper
- Player: Where do I start looking?
- NPC narration: Vesper trims the answer down to what can survive winter.
- NPC says: Start at the Graveyard near Alderfall. Find Frozen Seed Bowl.
- Options:
  1. I know where to start.
  2. What would someone hide in plain sight?
  3. Back to The Green Under White

#### The Green Under White / completed / stage 3 of 3

- Stage title: `Name The Waking`
- Objective: `CHOICE` The Waking Root (1/1)

#### What changed after -> What is still unresolved

- NPC says: The winter garden is learning what grows when people stop mistaking shelter for ownership.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. What changed after The Green Under White?
  3. Can we talk, Vesper?
  4. How does Oathstead feel to you?
  5. What should I avoid answering after dusk?
- Player: What changed after The Green Under White?
- NPC narration: Vesper listens as if the ground beneath the words has shifted. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Good. The first kindness is not lying to the wound. The first root still lives, and the black sap points toward my family grove. Mercy has become work.
- Options:
  1. That choice still follows us, doesn't it?
  2. Who carries the consequence with me?
  3. Do you think I chose well?
  4. What is still unresolved?
  5. Back to Vesper
- Player: What is still unresolved?
- NPC narration: Vesper trims the answer down to what can survive winter. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Do not force the sealed grove to answer too quickly. Roots tear when pulled for comfort.
- Options:
  1. Back to Vesper

### Representative Conversation Paths

#### Can we talk, -> How are you holding up? -> How are you feeling?

- NPC says: The winter garden is learning what grows when people stop mistaking shelter for ownership.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Vesper?
  3. How does Oathstead feel to you?
  4. What should I avoid answering after dusk?
- Player: Can we talk, Vesper?
- NPC says: Vesper studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Vesper
- Player: How are you holding up?
- NPC narration: Vesper's stillness deepens, protective rather than distant.
- NPC says: After the root, not just the branch. Few people do. You ask softly enough that the root does not flinch.
- Options:
  1. How are you feeling?
  2. What should we do next?
  3. Do you need anything?
  4. Back to personal matters
- Player: How are you feeling?
- NPC narration: Vesper's stillness deepens, protective rather than distant.
- NPC says: Cold at the edges. Alive at the center.
- Options:
  1. I will not push.
  2. Tell me the honest version.
  3. We can keep moving.
  4. Back to personal matters

#### Can we talk, -> How do you see me?

- NPC says: The winter garden is learning what grows when people stop mistaking shelter for ownership.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Vesper?
  3. How does Oathstead feel to you?
  4. What should I avoid answering after dusk?
- Player: Can we talk, Vesper?
- NPC says: Vesper studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Vesper
- Player: How do you see me?
- NPC narration: Vesper grows still, as if listening for the root under the words.
- NPC says: I am still deciding what your choices mean. The pattern matters more than one noble moment.
- Options:
  1. What do you trust in me?
  2. What worries you about me?
  3. What should I remember about you?
  4. Back to personal matters

#### Can we talk, -> Do you trust me?

- NPC says: The winter garden is learning what grows when people stop mistaking shelter for ownership.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Can we talk, Vesper?
  3. How does Oathstead feel to you?
  4. What should I avoid answering after dusk?
- Player: Can we talk, Vesper?
- NPC says: Vesper waits without rushing you. Trust has made the quiet less defensive.
- Options:
  1. Do you trust me?
  2. You make the cold feel less certain.
  3. How are you holding up?
  4. How do you see me?
  5. Can we talk about the Snowroot choices?
  6. Do you remember what happened?
  7. What do you think of...?
  8. Back to Vesper
- Player: Do you trust me?
- NPC narration: Vesper grows still, as if listening for the root under the words. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: I do not uproot easily. Somehow you have become part of where I stand.
- Options:
  1. Stand with me.
  2. You are free to choose your road.
  3. I am glad you are here.
  4. Good. I need your skills.

#### Can we talk, -> What do you think of -> Me, honestly

- NPC says: The winter garden is learning what grows when people stop mistaking shelter for ownership.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Vesper?
  3. How does Oathstead feel to you?
  4. What should I avoid answering after dusk?
- Player: Can we talk, Vesper?
- NPC says: Vesper studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Vesper
- Player: What do you think of...?
- NPC narration: Vesper grows still, as if listening for the root under the words.
- NPC says: Choose whose name you bring into the space between you.
- Options:
  1. Me, honestly.
  2. Aria
  3. Calder
  4. Cassia
  5. Lyra
  6. Maera
  7. Rafiq
  8. Samir
  9. Seraphine
  10. Back to personal matters
- Player: Me, honestly.
- NPC narration: Vesper grows still, as if listening for the root under the words.
- NPC says: You are not gentle, exactly. But you return to what needs care.
- Options:
  1. That is fair.
  2. You notice more than I thought.
  3. Back to personal matters

#### Can we talk, -> What do you think of -> Known companion

- NPC says: The winter garden is learning what grows when people stop mistaking shelter for ownership.
- Options:
  1. Can we talk about loyalty?
  2. Can we talk, Vesper?
  3. How does Oathstead feel to you?
  4. What should I avoid answering after dusk?
- Player: Can we talk, Vesper?
- NPC says: Vesper studies your face, less guarded than before but not careless.
- Options:
  1. Can we talk more personally?
  2. How are you holding up?
  3. How do you see me?
  4. Do you remember what happened?
  5. What do you think of...?
  6. Back to Vesper
- Player: What do you think of...?
- NPC narration: Vesper grows still, as if listening for the root under the words.
- NPC says: Choose whose name you bring into the space between you.
- Options:
  1. Me, honestly.
  2. Aria
  3. Calder
  4. Cassia
  5. Lyra
  6. Maera
  7. Rafiq
  8. Samir
  9. Seraphine
  10. Back to personal matters
- Player: Aria
- NPC narration: Vesper grows still, as if listening for the root under the words.
- NPC says: Aria knows paths that avoid attention. Roots approve.
- Options:
  1. Back to conversation

#### How does Oathstead feel -> How does Oathstead feel -> What does Oathstead need next

- NPC says: The winter garden is learning what grows when people stop mistaking shelter for ownership.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Can we talk, Vesper?
  3. How does Oathstead feel to you?
  4. What should I avoid answering after dusk?
- Player: How does Oathstead feel to you?
- NPC says: Vesper speaks of Oathstead like a place that may yet learn how to hold people gently.
- Options:
  1. How does Oathstead feel to you?
  2. Back to Vesper
- Player: How does Oathstead feel to you?
- NPC narration: Vesper lets warmth into the silence without rushing it into bloom. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: You ask about home as if it can grow instead of trap. I am listening. Oathstead has seen the snowroot take. I still check it too often, as if hope might vanish if I stop watching, but it keeps growing without permission.
- Options:
  1. Does this place feel like yours?
  2. How is your work here?
  3. What does Oathstead need next?
  4. Back to home and rest
- Player: What does Oathstead need next?
- NPC narration: Vesper trims the answer down to what can survive winter. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: A garden that grows food before symbols. Symbols can wait their turn.
- Options:
  1. Back to Vesper

#### Can we talk, -> Come here a moment. -> I missed being close to you.

- NPC says: You are early. Or I am glad. I have not decided which is safer.
- Options:
  1. Can we talk about the future?
  2. Can we talk, Vesper?
  3. Stay with me a while.
  4. What should I avoid answering after dusk?
- Player: Can we talk, Vesper?
- NPC says: Vesper lets the pause settle between you, careful and warm.
- Options:
  1. What is this between us?
  2. Stay with me here.
  3. Come here a moment.
  4. How are you holding up?
  5. How do you see me?
  6. Can we talk about the Snowroot choices?
  7. Do you remember what happened?
  8. What do you think of...?
  9. Back to Vesper
- Player: Come here a moment.
- NPC narration: Vesper lets warmth into the silence without rushing it into bloom. There is warmth in it now, not careless, but no longer disguised as tactics.
- NPC says: Say that softly. Some things grow crooked if everyone stares.
- Options:
  1. I missed being close to you.
  2. I keep wanting to stand where you are warmest.
  3. Too much?
  4. Back to personal matters
- Player: I missed being close to you.
- NPC narration: Vesper lets warmth into the silence without rushing it into bloom. There is warmth in it now, not careless, but no longer disguised as tactics.
- NPC says: Near can be enough. Some roots grow toward warmth without being ordered.
- Options:
  1. I wanted you to know.
  2. Back to personal matters

#### Can we talk, -> Stay with me here.

- NPC says: You are early. Or I am glad. I have not decided which is safer.
- Options:
  1. Can we talk about the future?
  2. Can we talk, Vesper?
  3. Stay with me a while.
  4. What should I avoid answering after dusk?
- Player: Can we talk, Vesper?
- NPC says: Vesper lets the pause settle between you, careful and warm.
- Options:
  1. What is this between us?
  2. Stay with me here.
  3. Come here a moment.
  4. How are you holding up?
  5. How do you see me?
  6. Can we talk about the Snowroot choices?
  7. Do you remember what happened?
  8. What do you think of...?
  9. Back to Vesper
- Player: Stay with me here.
- NPC narration: Vesper lets warmth into the silence without rushing it into bloom. There is warmth in it now, not careless, but no longer disguised as tactics.
- NPC says: At the inn table near Oathstead's road, even the noise has roots. Sit with me before I start pretending I only came for shelter.
- Options:
  1. I think I am thawing near you.
  2. I wanted time with you, not another task.
  3. Stay here with me.
  4. We can keep this gentle.
  5. Back to personal matters

#### Can we talk, -> Can we talk about our promise?

- NPC says: There you are. I kept the warm place beside me without naming why.
- Options:
  1. Can we talk, Vesper?
  2. Stay with me a while.
  3. What should I avoid answering after dusk?
- Player: Can we talk, Vesper?
- NPC says: Vesper gives you the kind of attention that has learned your silences.
- Options:
  1. Can we talk about our promise?
  2. Stay with me here.
  3. Come here a moment.
  4. How are you holding up?
  5. How do you see me?
  6. Can we talk about the Snowroot choices?
  7. Do you remember what happened?
  8. What do you think of...?
  9. Back to Vesper
- Player: Can we talk about our promise?
- NPC narration: Vesper grows still, as if listening for the root under the words. The answer comes with the ease of someone who has learned your silences.
- NPC says: I was raised to survive winters alone. You made me consider that warmth can be a vow, not a weakness.
- Options:
  1. Come home to me when you can.
  2. Tell me what still frightens you.
  3. The work comes first today.
  4. Back to topics

### Repeat-Aware Sample

#### Can we talk, -> How are you holding up? -> How are you feeling?

- NPC says: The winter garden is learning what grows when people stop mistaking shelter for ownership.
- Options:
  1. Can we talk about what this feeling is becoming?
  2. Can we talk, Vesper?
  3. How does Oathstead feel to you?
  4. What should I avoid answering after dusk?
- Player: Can we talk, Vesper?
- NPC says: Vesper waits without rushing you. Trust has made the quiet less defensive.
- Options:
  1. Do you trust me?
  2. You make the cold feel less certain.
  3. How are you holding up?
  4. How do you see me?
  5. Can we talk about the Snowroot choices?
  6. Do you remember what happened?
  7. What do you think of...?
  8. Back to Vesper
- Player: How are you holding up?
- NPC narration: Vesper's stillness deepens, protective rather than distant. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: You ask like you intend to hear the answer. That still catches me off guard.
- Options:
  1. How are you feeling?
  2. What should we do next?
  3. Do you need anything?
  4. Back to personal matters
- Player: How are you feeling?
- NPC narration: Vesper's stillness deepens, protective rather than distant. Trust has taken enough root that the guarded part no longer speaks first.
- NPC says: Like thaw. Muddy, exposed, necessary.
- Options:
  1. I will not push.
  2. Tell me the honest version.
  3. We can keep moving.
  4. Back to personal matters


