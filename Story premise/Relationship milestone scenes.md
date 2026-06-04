# Relationship Milestone Scenes

These scenes unlock when a companion reaches a trust threshold and has not already resolved that threshold with the player. The scene appears as a dialogue branch: `Talk about <milestone label>`.

The first choice taken in the milestone scene records the milestone memory, so the scene does not repeat. "Not now" leaves it available.

## Shared Scene Flow

Root option:
- Talk about <milestone label>

Opening choices:
- Tell me the honest version.
- <Warm threshold choice>
- <Boundary threshold choice>
- Not now.

Truth follow-up choices:
- I can carry that.
- That changes things.
- Back to topics.

Warm follow-up choices:
- Stay close, then.
- We keep choosing this.
- Back to topics.

Boundary follow-up choices:
- I will respect that.
- I need time too.
- Back to topics.

## Trust 50: Guarded Respect

Opening:
"I used to measure you by exits. Now I measure you by whether I need one. That is not trust yet, but it is no longer nothing."

Warm choice:
- I respect you too.

Boundary choice:
- Keep your distance if you need it.

Truth lines by companion:
- Seraphine: "Guarded respect, then. I dislike how legal that sounds, but it fits: limited terms, honestly entered."
- Maera: "The honest version is that you have become a source I do not immediately distrust."
- Cassia: "I respect that you bend and return. People who never bend usually break other people first."
- Lyra: "I have begun believing your concern may survive inconvenience. That is rarer than kindness."
- Samir: "You have not demanded faith from me. You have earned attention. That is the first clean step."
- Aria: "You watch the road better than most. You also listen when I say the road is lying."
- Vesper: "You have not trampled every small living thing on the way to large victories. I noticed."
- Rafiq: "You are becoming difficult to dismiss. Very rude. Possibly admirable."
- Calder: "You hold under load. Not perfectly. Nothing good does at first."

Warm response:
"Then we begin there. Respect first. Trust can stop pretending it was born fully armed."

Boundary response:
"Distance is not rejection. Sometimes it is how respect learns not to bruise."

Accept close:
"Then respect stands. Small foundation, real stone."

Change close:
"Changed things are not always broken. Sometimes they are finally named."

## Trust 100: Personal Admission

Opening:
"There is something I have not said because saying it makes it harder to pretend I am only traveling beside you."

Warm choice:
- You can tell me.

Boundary choice:
- Only say what you can bear.

Truth lines by companion:
- Seraphine: "I still expect kindness to hide terms. With you, I sometimes forget to look for the trap first."
- Maera: "I am afraid the truth will cost more than I can pay, and more afraid I will pay it anyway because you are watching."
- Cassia: "I have survived by being useful. I do not know who I am when someone stays after the use is done."
- Lyra: "I know how to hold everyone together. I do not know how to let someone hold me without counting it as failure."
- Samir: "I have preached light while fearing what it would show in me. You make hiding feel less holy."
- Aria: "I learned to leave before anyone could choose it for me. Staying near you is making that habit clumsy."
- Vesper: "I blamed myself for every root that did not bloom. You make blame feel less like truth."
- Rafiq: "I made charm out of panic. It worked so well I forgot where I put the honest man."
- Calder: "I trust structures because people failed me. You are making that distinction inconvenient."

Warm response:
"Good. I will tell it badly at first. Be patient with the parts that limp."

Boundary response:
"Good. A secret forced open becomes another wound."

Accept close:
"Then I will say more when I can breathe around it."

Change close:
"Yes. But not all change is a threat. I am trying to learn that before it learns me."

## Trust 150: Loyalty

Opening:
"Loyalty is a dangerous word. Too many people use it when they mean ownership. I am using it carefully."

Warm choice:
- Choose your road with me.

Boundary choice:
- I will not make loyalty a chain.

Truth line:
This is now companion-specific. Each companion frames loyalty through their own wound:
- Seraphine: chosen freedom without ownership.
- Maera: loyalty as speaking truth aloud.
- Cassia: guarding someone by name, not command.
- Lyra: care chosen beyond crisis.
- Samir: walking beside questioning light.
- Aria: knowing every exit and still staying.
- Vesper: roots that hold through winter.
- Rafiq: choosing to become reliable.
- Calder: bridges and people proven under weight.

Warm response:
"Then I choose this road with open eyes. Not as debt. Not as surrender. As loyalty."

Boundary response:
"That is why I can offer it. A chain would have made me run."

Accept close:
"Then we stand as chosen allies. That word still has weight. Good."

Change close:
"We keep choosing it, then. Loyalty should stay awake."

## Trust 180: Romance Possible

Opening:
"This has crossed the border between useful trust and something less obedient. We should name that before it names us badly."

Warm choice:
- Let us name it gently.

Boundary choice:
- I do not want to rush this.

Truth line:
This is now companion-specific. Each companion admits attraction in their own language:
- Seraphine wants choice without ownership.
- Maera cannot keep filing the player under alliance or gratitude.
- Cassia wants to be looked for after battle.
- Lyra loses her healer's composure around the player.
- Samir finds the feeling warm, difficult, and more honest than certainty.
- Aria wants to stay close without a tactical excuse.
- Vesper turns toward the player like thaw toward sun.
- Rafiq chooses sincerity over charm.
- Calder trusts the feeling because it held through weather.

After this milestone is resolved, a new `Talk personally -> Plan/Share a quiet date` branch unlocks. At an inn, tavern, or Oathstead it becomes an immediate date scene; elsewhere the player can plan one for a quieter place.

Warm response if already romanced:
"We already named it once. I want to keep choosing it when the day is ordinary too."

Warm response if not romanced:
"Gently, then. I would rather grow toward you than fall and call the bruise destiny."

Boundary response:
"Do not rush it. If this is real, it can survive being treated carefully."

Accept close:
"Then we move gently and honestly. I can do one of those easily. I will practice the other."

Change close:
"Time is allowed. Wanting does not become wiser by sprinting."

## Trust 250: Marriage/Future Conversation

Opening:
"The road keeps trying to end us. I have started imagining what remains if it fails."

Warm choice:
- Imagine that future with me.

Boundary choice:
- I cannot promise forever today.

Truth line:
This is now companion-specific. Each companion imagines a concrete future:
- Seraphine: unlocked doors, fair terms, and her name belonging to her.
- Maera: maps on the table, arguments in the margins, no locked drawers.
- Cassia: a gate she does not have to guard alone.
- Lyra: clean water, mended sleeves, and care allowed to rest.
- Samir: a lamp in a window and faith that survives plain speech.
- Aria: a door used more than once and a trail that comes back.
- Vesper: winter stored properly and spring not rushed.
- Rafiq: good wine, fewer creditors, and honest laughter.
- Calder: a checked roof, a scarred table, and maintained promises.

Warm response if already married:
"We have already made one promise. Let us keep making it in smaller, stubborn ways."

Warm response if not married:
"I can imagine a door, a table, a road back, and you not vanishing from any of them."

Boundary response:
"Then promise the honest part: no forever stolen from fear, no future spoken only because the night is warm."

Accept close:
"Then the future remains possible. Not promised into a cage. Possible."

Change close:
"A future earned slowly may hold better than one sworn too loudly."
