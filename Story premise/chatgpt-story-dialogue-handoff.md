# Echoes of Alderfall: Story and Dialogue Handoff

Prepared from the setting bible, narrative rewrite brief, location library, implementation reports, live Java content, and quest catalog on 22 September 2026.

This document is designed to be pasted into ChatGPT as the shared context for future story, quest, NPC, and dialogue work. It separates the intended canon from current playable implementation so that new writing does not promise scenes, choices, locations, or consequences the game does not yet support.

## 1. Status language and source of truth

Use these labels throughout this handoff:

- **LIVE:** present in the current Java game or its generated runtime dialogue.
- **TARGET:** approved creative direction, but not necessarily implemented.
- **AUTHOR TRUTH:** true for writers, but not automatically known by the player or NPCs.
- **OPEN:** deliberately undecided or still requiring design.

For current gameplay behavior, Java code and generated runtime reports are authoritative. For future fiction, the setting bible and narrative rewrite brief are authoritative. A target idea must not be written as a completed player action until its quest state, location, interaction, and aftermath exist.

The most important source documents are listed in section 18.

## 2. One-page creative brief

**Title:** *Echoes of Alderfall*

**Genre:** Party-based fantasy role-playing game with exploration, settlement growth, turn-based combat, quests, recruitable companions, friendship and optional romance.

**Tone:** Adventurous folklore with warmth, wonder, humor, and moments of dread. A frightening shrine, an impossible market, a generous innkeeper, a dangerous royal secret, and an argument over a communal oven should all feel as if they belong in the same world.

**Player:** A customizable traveler and survivor, not a prophesied monarch or bearer of a required bloodline. The player is important because they witnessed Vaelthara's attack, survived it, and can build relationships across divided communities. A magical wound that senses breaches is a possible target feature, not settled canon or a live mechanic.

**Opening hook:** The player follows reports of a failing road shrine. Vaelthara tears open its protection, defeats the player, and leaves them alive. The survivor reaches Oathstead Camp, where Maelis offers shelter and asks for a factual account. Investigation reveals that Oathstead's protection may share the shrine's weakness.

**Home base:** Oathstead, a vulnerable refugee camp growing into a crossroads settlement. It is the campaign's proof that imperfect voluntary cooperation can protect people without turning them into permanent instruments.

**Main antagonist:** Vaelthara, Demon Queen of Mercy. She genuinely recognizes pain and instability, but answers them with compulsory safety under one will. She offers food, wards, and order while denying people the right to leave or dissent.

**Central dramatic question:** Can safety be maintained without binding people, spirits, the dead, or whole communities into permanent service?

**Main campaign device:** Twelve old socket stones once coordinated the defenses of the Demon War. They are functional parts of a ward network, not interchangeable magical trophies. Recovering them reveals what the network did, how emergency cooperation became compulsion, and what a replacement might require.

**Recurring story verbs:** Investigate, witness, shelter, repair, defend, release, negotiate, remember, prepare, and choose. Combat remains important, but folklore stories should not reduce every spirit, animal, or political dispute to a kill target.

**Core motifs:** Roads, thresholds, hearths, bells, names, vessels, crossings, records, gardens, guest law, burial duties, and the difference between a promise and a prison.

## 3. World foundations

### The shared premise

Settlements survive through agreements: between neighbors, political realms, and the powers inhabiting rivers, forests, mountains, homes, roads, and objects. Regional folklore preserves practical warnings, but stories can omit facts, misidentify causes, or protect the interests of those who recorded them.

The world is not divided into mundane humans and uniformly hostile monsters. A returned sailor, territorial wyrm, household presence, predatory animal, human raider, local god, independent fire-being, and servant of Vaelthara are different kinds of encounter. Their appearance alone does not establish motive or morality.

### Supernatural rules

These are author-facing rules. In-world explanations may disagree.

1. **Places and objects can accumulate presence.** Long use, repeated ritual, intense attachment, or contact with a great power may make a place or object supernatural. Age alone does not make everything sentient.
2. **Powers are particular.** A spirit may belong to a river, household, craft, mountain, road, or object. Others travel or maintain societies independent of humans. A river power can affect its waters; it cannot casually end a distant drought.
3. **Wards need anchors and upkeep.** Bells, stones, groves, wells, vessels, carvings, and repeated practices can anchor protection. Failure may come from physical damage, neglect, theft, sabotage, or an agreement that has become exploitative.
4. **A repaired object may not repair the relationship.** Replacing a bell or stone can restore a mechanism while leaving the underlying terms unresolved.
5. **Supernatural oaths require conditions.** A binding oath needs an identifiable witness or power, an intentional act, and discoverable terms. Ordinary promises are not automatically spells.
6. **The dead return for specific reasons.** Attachment, injustice, interrupted rites, binding magic, or unfinished obligations may cause a haunting. There is no rule that all dead return or that only one funeral tradition works.
7. **Spirits are not demons by default.** Southern fire beings, eastern apparitions, northern ancestors, Hearthkin, Briar Court inhabitants, and Fenland water powers remain distinct from Vaelthara's demonic threat.
8. **Magic needs a means and a limit.** A seal needs a vessel and maintenance. A restoration should leave a material or social consequence rather than reset the world at no cost.
9. **Myth is evidence, not a perfect manual.** A tale should provide a clue that can be tested. Mandatory mechanics need dependable evidence from at least two sources when possible.

### Relative history

1. Communities independently developed routes, settlements, local protections, and agreements.
2. Trade and migration spread techniques and stories. Similar customs acquired different meanings in different places.
3. During the Demon War, human communities and supernatural powers formed a coalition against an incursion through the Hollow Throne.
4. The twelve stones coordinated the coalition's defense.
5. **AUTHOR TRUTH:** As defeat approached, coalition leaders altered the system so some powers could no longer withdraw. Temporary emergency service became permanent compulsion.
6. Later rulers inherited safer roads and productive land while records obscured or omitted the cost. Knowledge and responsibility vary; not every present community knowingly chose the abuse.
7. In the present, Vaelthara exploits damaged wards and fractured trust. Other local crises also arise independently from neglect, crime, ecology, grief, and older bargains.

## 4. Geography, political realms, and cultural regions

A temperate central basin opens toward western rivers and a stormy coast. Northern mountains feed its rivers. Southward, grasslands become dry plateaus, irrigated valleys, salt flats, and desert. Eastern wetlands lead to harbors and a sea crossing to the Lantern Isles.

Political borders and cultural regions are not identical. Trade, migration, marriage, study, military service, and displacement create mixed communities. Culture is learned and shared; ancestry does not determine morality, class, profession, or magical ability.

### Political structure

The current mainland map contains six territories:

- Highwall March
- Northroad Freeholds
- Sanctum Sunrealm
- Riverside Reach
- Crownlands of Alderfall
- Belltower Fenlands

**TARGET continuity:** There are five mainland treaty signatories, the independent Northroad Freeholds, and the eastern Lantern Isles. Refer to their joint action as a gathering of realms, not the "five kingdoms." The current live campaign still collects commitments from five mainland representatives and does not yet include Freehold or eastern representatives in that scene.

### Regional reference

| Region | Identity and everyday life | Supernatural focus | Faith and institutions | Key live places | Signature question |
| --- | --- | --- | --- | --- | --- |
| **North: Skeldreach and the Stormbound Holds** | Fjords, pine valleys, mountain passes, fishing, grazing, weaving, ship repair, winter stores, rescue shelters, and assemblies. Prestige culture is in tension with collective survival. | The Wake Road, burial cairns, ancestral ships, returned defenders, Rimewrights, giants, and dangerous winters. Accurate remembrance matters. | Hearthhouses of Hedra; Hearth Assemblies; Keel Houses; Cairn Keepers. | Highwall, Ironvale, Snowrest, Pineward; Freehold links through Northwatch and Cairnvale. | What do the living owe the dead? |
| **South: Serevan and the Sunrealm** | Caravan cities, well towns, ports, date gardens, irrigated valleys, observatories, water engineering, glasswork, and guesthouses. | Independent beings of fire and wind, dangerous vessels, sealed guests, ruin deceivers, rocs, ghouls, and great serpents. | Courts of the Open Sun; Avar; Well Councils; Caravan Houses; Keepers of the Open Door. | Sanctum, Embermarket, Dunewick, Sunmere, Redcairn. | When does a promise become a prison? |
| **West: Thornmere and the River Courts** | Navigable rivers, orchards, wet meadows, bridge towns, ferries, mills, old roads, castles, market charters, and contested tolls. | Briar Courts, formal invitations, enchanted beasts, reflected roads, supernatural hunts, wyrms, and inherited compacts. | Sanctuaries of the Open Bough; Bridge Courts; Thorn Compacts; Road Orders. | Riverside, Briarbridge, Foxbarrow, Redcap Supply Camp. | What makes a ruler's claim legitimate? |
| **Center: Alderfall and the Hearthlands** | Craft towns, royal bureaucracy, grain fields, orchards, forests, shared ovens, seed stores, household transitions, and boundary work. | Hearthkin, the Greenward, household wards, forest powers, restless dead, and protections preserved in ordinary work. | Seedhouses and Hearth Chapels; Keeper of the Returning Seed; Royal Archive; Hearth Guilds; Boundary Stewards. | Archive City, Moonspire, Oakhaven, Elderford, Oathstead. | Who protects a home when official protections fail? |
| **Belltower Fenlands** | Wet ground, stilt homes, plank walks, floating gardens, ferries, clinics, reed work, flood expertise, and warning networks. | Deep Listeners, the Drowned Choir, submerged passages, contaminated bell-rope, channel predators, and drowned memory. | Houses of the Listening Bell; navigators, bellwrights, healers, and salvage crews. | Belltower, Reedwatch, Mireford, Glimmerfen, Stormfen. | When does answering the dead become keeping them at work? |
| **Northroad Freeholds** | Independent inland and harbor communities with rescue yards, crew stores, councils, cairns, and eastern shipping ties. | Northern remembrance mixed with harbor crossings and imported techniques. | Harbor councils and rescue lodges; no single ruler speaks for every freehold. | Northwatch, Cairnvale, Greyharbor. | Who has authority when survival is shared but rule is not? |
| **East: Lantern Isles and Kasen Courts** | Competing provinces, free ports, terraced fields, mountain sanctuaries, fishing towns, performance traditions, and storm relief. | Near Shore and Further Shore crossings, Remembered Things, local mountain and river powers, apparitions, and serpents. | Sanctuaries of the Returning Tide; Harbor League; Mountain Sanctuaries; Provincial Houses. | **TARGET:** Ninth Landing and other island sites. No live eastern campaign map yet. | How should people live beside powers they cannot own? |

### Live settlement footprint

There are 24 generated NPC settlements with regional visual identity, ordinary working commons, daily-life dialogue, and 38 enterable regional institution buildings. Oathstead is managed separately.

- Hearthlands: Archive City, Moonspire, Oakhaven, Elderford.
- Western Reach: Riverside, Briarbridge, Foxbarrow.
- Stormbound Holds: Highwall, Ironvale, Snowrest, Pineward.
- Sunrealm: Sanctum, Embermarket, Dunewick, Sunmere, Redcairn.
- Fenlands: Belltower, Reedwatch, Mireford, Glimmerfen, Stormfen.
- Northroad Freeholds: Northwatch, Cairnvale, Greyharbor.

Regional working buildings include granaries, ferry lodges, halls of names, winter smokehouses, cistern houses, caravanserais, flood bellhouses, reedworkers' houses, and rescue lodges. They are explorable social spaces, but they do not yet simulate production, lodging, or transport economies.

## 5. Oathstead

Oathstead begins as the refuge that receives the wounded player after the shrine attack. It grows into the emotional and practical center of the campaign. Its importance comes from the people who choose to maintain it, not from a royal claim.

**Maelis's purpose:** Turn emergency shelter into a durable community without pretending she is an expert in ancient magic.

**Target institutions:** Public hearth, rescue store, clinic, workshop, guest court, garden, landing place, map room, refuge, and training or maintenance spaces established through actual resident needs.

**Useful local conflicts:**

- Who maintains winter stores and who may draw from them?
- Can a spirit own or occupy a room?
- When should a warning bell ring?
- Should a gate remain open during an attack to admit late arrivals?
- How much water may visitors take during shortage?
- Who is responsible for rescue, repair, and challenging a dangerous command?

Regional contributions should create lived shared spaces, not a row of collectible national pavilions. Oathstead is not utopia: compromises fail, resources remain limited, and some decisions require the player to choose.

## 6. Folklore, faith, and hostile factions

### Regional faiths

Faith supports meals, teaching, shelter, medicine, mourning, festivals, craft, and arguments as well as magical protection. A working ward does not prove that its keeper is right about every theological or political claim.

| Tradition | Public purpose | Local supernatural rule or dispute |
| --- | --- | --- |
| **Hearthhouses of Hedra** | Winter shelter, food stores, rescue work, memorial meals. | At the proposed Last Fire Hall, a formally received guest gains protection while they remain peaceful. The dispute is whom a community must shelter during scarcity, not whether kindness is naive. |
| **Courts of the Open Sun** | Guest water, teaching, engineering, songs, lamps, and sanctuary. | Avar's revelation can support truth or oppressive surveillance. Cooling basins and flame behavior reveal whether a local vessel is transferring power or extracting it. Unbanked Flame beings are independent, not Avar's servants. |
| **Sanctuaries of the Open Bough** | Hospitality, hearings, ferries, gardens, journey remembrance, and refuge. | In a particular feast hall, eating after accepting a named host's invitation establishes a guest relationship. Ordinary food has no universal hidden trap. The guest must be allowed to know and refuse the terms. |
| **Seedhouses and Hearth Chapels** | Shared ovens, seed libraries, gardens, household rites, and seasonal work. | A local Hearthkin responds to care and ordinary use, not expensive offerings. Debate centers on common ownership, damaged buildings, and the rights of inhabitants of root beds and groves. |
| **Houses of the Listening Bell** | Navigation, clinics, warning calls, funerals, rope work, and flood memory. | A three-note rescue call can open a submerged route. Repeating it without an answering interval becomes a summons that may keep the dead attending. |
| **Sanctuaries of the Returning Tide** | Crossing care, boat and object repair, remembrance of the missing, and harbor assembly. | A particular old sail remembers a lost route. Repairing its pattern and using the correct tide reveals the crossing; access is not a vague purity test. |

### Hostile factions

These groups have specific abuses, internal differences, and recruitment appeals. A recruit, paid guard, captive, doubter, and informed leader should not be written as morally or factually identical.

| Faction | Appeal | Abuse | Vaelthara relationship |
| --- | --- | --- | --- |
| **Ashen Keel** | Returned defenders will protect abandoned settlements. | Binds ancestral names to command banners; many recruits believe service is voluntary. | Kharvok supplies techniques, but local ambition predates him. |
| **Closed Dawn** | Reliable light, food, shelter, and protection through discipline. | Alters fire rites to extract vitality from unwilling participants. | Receives help from a herald while claiming independent authority. |
| **Antler's Due** | Escape from debt and humiliation through a glorious hunt. | Marks unwilling people as quarry to win Briar Court favor. | Independent movement courting a Briar noble. |
| **Still Harvest** | No child should hunger or die before their time. | Sustains arrested growth using exhausted living anchors. Most supporters see only miraculous fruit. | Independent; Vaelthara may later offer protection. |
| **Unending Vigil** | The drowned can return and keep protecting their families. | Maintains a summons that traps the dead and contaminates bell-rope. | Velmora exploits the local grief; Vaelthara did not create it. |
| **Unbroken Mask** | Preserve voices, skills, and loved ones forever. | Copies living people into memorial objects and conceals the originals. | Independent regional power seeking crossing control. |
| **Merciful Hand** | Relief stores, safe roads, and an end to uncertainty. | Aid is paired with compulsory pledges, confinement, and armed enforcement. | Directly serves Vaelthara. |

Not every secret group is hostile. The Society of the Last Lamp quietly sits with dying strangers and pays funeral costs; secrecy protects privacy rather than concealing abuse.

### Live environmental folklore

Several pieces of regional folklore already exist as explorable scenery and peaceful social space:

- Oakhaven's House of Returning Seed and Elderford's Seed Exchange.
- Communal ovens, household wards, boundary markers, and local paired conversations in the Hearthlands.
- Briarbridge Guest Abbey, public orchard, petition benches, journey trees, undercroft, and a physically sealed mirror-door.
- Riverside Charter Hall, Foxbarrow Orchard House, western woodland markers, and a lived-in Redcap supply shelter.
- Regional commons and working institutions throughout all 24 NPC settlements.

These spaces establish vocabulary and atmosphere. Their larger disputes are not automatically playable quests. The sealed Briarbridge mirror has no open portal, and the proposed fugitive hearing is still future content.

## 7. Creatures and bosses

### Encounter ecology

- North: frost wolves, snow lynxes, mountain drakes, cairn sentries, ice scavengers, giants, ship revenants, winter beasts.
- South: glass scorpions, sand stalkers, ember tortoises, ruin scavengers, fire beings, rocs, ghouls, hostile fire lords, great serpents.
- West: bramble boars, woodland deceivers, river predators, hunting hounds, enchanted sentries, wyrms, Briar nobles, hunt beasts.
- Center: thornlings, moss stags, household spirits, displaced animals, grave sentries, root guardians, forest powers, revenants.
- Fenlands: river eels, marsh drakes, bog beasts, drowned remnants, channel predators, and entities caught in bell networks.
- East: river and cliff creatures, Remembered Things, apparitions, sanctuary guardians, Further Shore predators, and great serpents.

An ecological threat does not need a hidden cult. A beast can be hungry, displaced, nesting, territorial, or responding to changed waterworks. An intelligent power can have a legitimate grievance and still use unacceptable violence.

### Target signature bosses

| Region | Boss | Story function and possible outcomes |
| --- | --- | --- |
| North | **Hrold, Jotunn of the Broken Pass** | Miners diverted his warm stream and damaged his hall; his retaliation closes a vital route. Defeat him, restore the stream and negotiate withdrawal, or rescue workers and force a limited retreat. |
| North | **Eydra, Captain of the Unmoored** | Raiders stole a burial anchor and compelled a returned ship's crew. Return the anchor and defeat the binding, or destroy the vessel at the cost of its remaining memory. |
| South | **Whitewing, Roc of the Salt Cliffs** | Smugglers stole an egg, provoking attacks on caravans. Kill the roc, return the egg and survive the retreat, or temporarily reroute traffic. |
| South | **Vahrun, Lord of the Sealed Furnace** | An independent fire lord diverts Dunewick's water to arm his court. Destroy his anchored form or force a truce after ending military control and releasing captives. |
| South | **The Borrowed Host** | A predatory ghoul imitates remembered voices in a deserted caravanserai. Kill or permanently trap it and recover victims; no compulsory sympathy twist. |
| West | **Thornmaw, the Orchard Wyrm** | Thieves manipulate a territorial dragon's lure route. Kill it or rebuild the lure system and drive it to an unoccupied valley. |
| Center | **Rootmaw, Stag Beneath the Orchard** | A living guardian carries invasive roots and a ward fragment. Future design may permit restoration or lethal victory. **LIVE currently:** the main quest requires defeating Rootmaw. |
| Fenlands | **Old Reedjaw, Keeper of the Sluice** | A large predator occupies a spillway after floodworks changed its feeding grounds. Drive it into open marsh or kill it. |
| East | **Eight-Flood Serpent** | Compressed into a reservoir by embankments and a failing network. Slay it or reopen an old sea outlet and drive it through; this makes the Tides crossing reachable. |
| East | **Pale Coil of the Ninth Landing** | Hunts Remembered Things through reflected surfaces. Defeat it or sever its access without stranding normal travelers. |

**Status warning:** These are mostly TARGET designs, not live boss fights. Current main-story opponents include Kharvok, Velmora, Rootmaw, the Nameless Warden, the Hailback Broodmother, Sareth, Morvane's raiders, and Vaelthara. Do not write a target boss as previously defeated unless the implementation is added.

## 8. Main cast and antagonists

### Campaign cast

| Character | Role | Voice | Knowledge boundary and useful tension |
| --- | --- | --- | --- |
| **Maelis** | Organizer of Oathstead; receives the player and turns shelter into community. | Direct, humane, attentive to immediate needs. Asks for events in order. | Understands people and logistics better than ancient magic. She should not state Vaelthara's private motive as fact. |
| **Selene** | Archive custodian who compares shrine evidence, locates missing instructions, and guides the Memory recovery. | Precise, curious, separates documentation from inference. | Knows surviving records are incomplete; must not reveal the entire concealed history before evidence supports it. |
| **Odrick** | Highwall commander responsible to the roadwatch and communities beyond the gate. | Brief, practical, command-aware. | Must answer whose survival his orders prioritize. Cairn keepers possess knowledge he lacks. |
| **Solari** | Southern religious authority working with engineers and independent fire powers. | Deliberate and exact about rites; capable of admitting uncertainty. | Knows the rite he was taught, not every alteration in every vessel. Authorizes the live release of one guest and accepts the material cost. |
| **Mirella** | Western political broker managing bridge economy, grain movement, and obligations. | Persuasive, observant, concrete about roads and cargo. | Sometimes overestimates what negotiation or charter authority can control. Briar paths have their own keepers. |
| **Ysra** | Fenland navigator and bell authority coordinating warnings and ferries. | Concrete about sound, weather, water, and routes. | Does not know every drowned voice or command every Deep Listener. |
| **Damar** | Ash-scribe and Blackvault investigator. | Methodical, evidence-driven, impatient with unsupported claims. | Understands spent magic and maintenance records, not Vaelthara's entire hidden history. |
| **Elder Rowan** | Oakhaven orchard keeper and main-story contact for Roots. | Patient, practical, attentive to generations of labor and nonhuman passage. | Knows orchard custom and care, not a universal account of forest beings. |
| **Gravekeeper Hollis** | Archive burial custodian and contact for Graves. | Gentle, exact about names and release from duty. | Remembrance should not keep the dead working. |
| **Captain Elric Snowrest** | Organizes winter supplies and roads; contact for Frost. | Warm but logistical, grounded in household effects of delayed supplies. | A defeated predator does not itself deliver medicine or repair the road. |
| **Bellwright Nessa** | Glimmerfen craftsperson and contact for Bells. | Practical, attentive, craft-specific. | A sound bell still needs sound rope, foundations, and people to maintain it. |
| **Tovan Keelward** | **TARGET:** Elected Freehold pilot representing participating harbors. | Sociable and frank. | Has a community mandate, not power over all northern ports. |
| **Nao of the Ninth Landing** | **TARGET:** Eastern crossing keeper leading restoration of missing ferry routes. | Patient with travelers, exact about routes. | Authority ends at another sanctuary's responsibility. |

### Vaelthara

**Title:** Demon Queen of Mercy.

**AUTHOR TRUTH:** Vaelthara was a defeated power bound into the ward network after the Demon War and compelled to absorb destructive forces. She escaped part of that confinement and now seeks central command.

**Goal:** End uncertainty, hunger, dangerous crossings, and uncontrolled grief by placing settlements and powers under a single permanent authority: hers.

**Contradiction:** Her protected enclaves can provide real relief, but obedience becomes compulsory and dissent is suppressed. Her mercy is not fake; it is inseparable from domination.

**Voice:** Controlled, explicit, and able to name material suffering. She should make comprehensible offers with unacceptable terms, not speak only in riddles. She can acknowledge that Oathstead needs food and defenses while insisting that only submission can guarantee them.

**Limits:** She did not engineer every companion tragedy, invent every cult, or control every beast. She overestimates fear and underestimates voluntary cooperation.

**LIVE Gate demand:** Food and protection in exchange for the stones and authority over Oathstead, including the right to decide when protected residents may leave. This leads to the existing battle; there is no functional surrender branch.

### Other current threats

- **Kharvok:** Northern hostile commander who overwrote an oath's ending on a banner so release depended on him.
- **Velmora, Bell-Drowned:** Hostile power in Miredepth associated with bells calling the drowned against travelers. Ysra does not claim a complete account of Velmora's former life.
- **Rootmaw:** Orchard guardian attacking workers while carrying damaged or invasive growth. The live quest defeats it; a restorative resolution is only a target.
- **Nameless Warden:** Burial threat associated with damaged names and continued service.
- **Hailback Broodmother:** Mountain predator blocking Snowrest's road, not a proven servant of Vaelthara.
- **Sareth:** Blocks recovery of Ash at Blackvault.
- **Morvane:** Leads the raid on Oathstead that tests whether the settlement is worth and capable of defending.
- **Crowhook and Redcap raiders:** Human or goblin camps with supply, labor, command, and possible internal differences. Species does not prove cult allegiance.

## 9. Principal companions

There are nine principal companions, each with an eight-quest chain in the live catalog. Friendship, optional romance, memory, trust, recruitment, battle participation, and post-quest conversation systems exist. Recruitment requires relationship 20; companion trust can reach 300. Essential campaign information must never require romance, recruitment, persuasion, or a particular companion.

| Companion | Home, role, and class | Voice and ordinary desire | Target personal arc | Important lasting question |
| --- | --- | --- | --- | --- |
| **Aria Foxglove** | Western border guide based in Oakhaven; Ranger. | Quick, observant, often brief; playful about tracking and competition. | Follow her missing sister through a Briar Court hunt. The sister is alive and made choices Aria dislikes; the objective is not to treat her as property to retrieve. | Release, limited renegotiation, or chosen continued service with safeguards; how will the sisters maintain contact? |
| **Seraphine Vale** | Western court insider in Riverside; Veilrunner. | Controlled, socially perceptive, sincerely enjoys music, clothes, and hosting. | End her family's inherited service as living sureties to Thorn Hall without transferring the cost to another family. | Public exposure or negotiated release; what do freed servants choose for themselves? |
| **Maera Quill** | Central scholar in Archive City; Mage. | Enthusiastic, specific, evidence-focused; humor from observation rather than contempt. | Reconstruct erased routes through star observation, moving roads, witness knowledge, and omitted eastern navigation. | Publish routes with protections or restrict vulnerable paths without repeating the Archive's secrecy? |
| **Cassia Flint** | Northern shield captain in Highwall; Ironwall. | Concise in danger, slower personally; enjoys board games and teaching skills. | Confront the dead and survivors left outside a gate, expose Captain Varran's order, and create accountable rescue duties. | Punishment, restitution, and responsibility are distinct; confession does not force forgiveness. |
| **Vesper Snowroot** | Northern grovekeeper in Snowrest; Grovekeeper. | Precise about living things, guarded about family; delighted by unfamiliar plants and animals. | Free a spring trapped by a family seal while preventing its blight from reaching the dependent village. | Slow managed thaw or rapid release with evacuation and material loss; who maintains the living cutting afterward? |
| **Samir Dawn** | Southern reliquary keeper in Sanctum; Sunwarden. | Sincere and thoughtful, occasionally funny; likes song and teaching. | Discover a fire guest imprisoned in his family vessel, recover his mother's work, and offer a genuine choice to leave or form a new agreement. | Can his faith survive honesty, and can Oathstead maintain light if the being leaves? |
| **Rafiq Glass** | Southern performer and duelist in Dunewick; Bladedancer. | Charming and theatrical in public, quiet when not managing an audience; genuinely enjoys performance. | Expose a stolen reflection that helped rig a fatal duel while owning his concealment and flight. The double is becoming a fearful person. | Willing reintegration or separate existence with safeguards; restitution remains necessary either way. |
| **Lyra Bell** | Fenland healer based in Belltower; Battle Medic. | Attentive and direct while working, impatient and funny when appropriate; wants rest and hobbies too. | Treat sickness carried by contaminated enchanted bell-rope and release a trapped call from a drowned infirmary. | How are care, supplies, responsibility, remembrance, and training distributed afterward? |
| **Calder Reed** | Freehold-born bridgewright in Mireford, trained in eastern harbor workshops; Stonebreaker. | Patient with craft explanations, stubborn about shortcuts, admits ignorance; makes toys and useful gifts. | Repair a bridge whose material failure also interrupted a spirit passage. Officials denied materials; he still approved the design. | Shared crossing hours or separate routes; how will knowledge and maintenance survive his departure? |

### Implemented companion-story coverage

The general conversation system supports all nine companions, including staged quest topics, evidence, decisions, memory, friendship, and optional romance. Most target eight-part arcs are not yet fully rewritten.

Five chapters have bespoke live action sequences aligned with the new direction:

1. **Seraphine 2:** Collect renewed invitations, hear former servant Iven's account, prepare a refuge charm, and confirm his arrival. His service remains unresolved.
2. **Aria 2:** Collect cut ribbons, question charcoal burner Bren, place a counterknot, and confirm his arrival. He saw a traveler, not the sister's face.
3. **Lyra 3:** Obtain Eda's consent, collect a care packet, contain suspect rope, treat her, hear her account, and agree on the next response.
4. **Rafiq 2:** Gather and hand over water and herbs to Elder Safa, hear her mirror testimony, and decide Rafiq's next commitment.
5. **Lyra 6:** Collect sealed supplies, defeat marked route raiders, deliver to attendant Sen, and plan later visits without claiming they already occurred.

Vesper's first two investigations also have improved Snowrest evidence and direct testimony from Goatkeeper Una. Other companion stages retain a mixture of older content and new conversation framing. Do not assume later target outcomes have happened.

## 10. Current NPC directory

This is a compact name-and-role reference for recurring live NPCs. It is intended to prevent accidental renaming, duplication, or biography collisions. Procedural residents and building keepers also exist beyond this list.

| Place | Main-story and companion anchors | Other named residents, quest givers, merchants, and recruits |
| --- | --- | --- |
| **Oathstead** | Maelis | Player-recruited residents may later be assigned to the settlement. |
| **Riverside** | Mirella; Seraphine Vale | Marla, Peddler Nessa, Dockhand Hobb, Seamstress Vala. |
| **Archive City** | Selene; Gravekeeper Hollis; Maera Quill | Archivist Ren, Map-Seller Dain, Apprentice Miri, Scribe Pela. |
| **Highwall** | Odrick; Cassia Flint | Captain Torin, Canteen Cook Berta, Gate Clerk Halen, Signal Keeper Lysa, Scout Rook, Trapmaster Yaro. |
| **Sanctum** | Solari; Samir Dawn | Warden Sol, Brother Cal, Quartermaster Vesh, Eira, Ash Watcher Kera. |
| **Belltower** | Ysra; Lyra Bell | Bellkeeper Ilya, Net-Mender Corso. |
| **Oakhaven** | Elder Rowan; Aria Foxglove | Edda, Hedgewise Lin, Tanner Sori, Finch, Farmer Joss, Rowan, merchant Mira, Bran, Liora, Rowan Wildspeaker. |
| **Snowrest** | Captain Elric Snowrest; Vesper Snowroot | Niva, Garruk Ironwall, Elowen, Cairnwatch Asta, Goatkeeper Una, Furrier Pem, Pass Guide Olin. |
| **Dunewick** | Rafiq Glass | Sela, Rain-Seer Imani, Orren the Peddler, Wellkeeper Safa, Spice Peddler Rafi, Kael, Nyx, Orin Stonebreaker, Toma. |
| **Mireford** | Calder Reed | Fen, Reedcutter Vell, Basketmaker Jun, Lantern Seller Pella, Mira Sunwarden, Vexa, Sable, Old Noll. |
| **Glimmerfen** | Bellwright Nessa | Regional workers and generated residents. |
| **Redcairn** | Ash-Scribe Damar | Regional workers and generated residents. |
| **Northwatch** | — | Signal Marshal Edrin, Mountaineer Pela, Cold Cartographer Ro. |
| **Cairnvale** | — | Rune Delver Saela, Fur-Tracker Minn, Grove Tender Talla. |
| **Greyharbor** | — | Cachemaster Orric, Reed Captain Lio, Mist Clerk Vessa. |

### Name-collision warnings

- Elder Rowan, the side-quest NPC Rowan, and recruit Rowan Wildspeaker are separate people.
- Peddler Nessa in Riverside and Bellwright Nessa in Glimmerfen are separate people.
- Merchant Mira in Oakhaven and Mira Sunwarden in Mireford are separate people.
- Scribe Pela in Archive City and Mountaineer Pela in Northwatch are separate people.
- Spice Peddler Rafi and companion Rafiq Glass are separate people.

The smaller recruit roster includes Marla, Ren, Torin, Eira, Bran, Niva, Sela, Fen, Liora, Garruk, Kael, Nyx, Rowan Wildspeaker, Mira Sunwarden, Vexa, Orin, Sable, Elowen, Orren, Sori, Berta, and Safa in addition to the nine principal companions. Their gameplay roles can be retained, but most still need deeper individual biographies and location-aware recruitment writing.

## 11. Quest system and quest types

### Catalog size

The live catalog contains **134 quests and 355 stages**:

- **26 main-story quests**
- **72 companion quests**: eight chapters for each of nine principal companions
- **36 side quests**

### Objective types in the live catalog

| Objective | Stages | Narrative use |
| --- | ---: | --- |
| Search | 140 | Inspect evidence, recover an object, read a mark, test a mechanism. The dominant current format. |
| Choice | 63 | Make or confirm a supported decision in dialogue. |
| Defeat | 55 | Resolve a marked combat encounter. Combat only proves the stated encounter result. |
| Gather | 29 | Collect marked samples or quest-bound supplies. |
| Visit | 29 | Reach and inspect a marked place. |
| Talk | 23 | Hold an explicit exchange with a named contact. |
| Defend | 10 | Protect or hold a location through a combat objective. |
| Deliver | 3 | Hand quest-bound cargo to a validated recipient. |
| Ask around | 1 | Collect testimony from eligible witnesses. |
| Rescue | 1 | Resolve a marked rescue combat. |
| Raid defense | 1 | Use Oathstead's defense point and complete the camp-defense sequence. |
| Escort / Report | 0 as distinct current stage kinds | Escort language must not claim follower simulation. Reports are generally handled through quest-giver dialogue rather than a stage tagged REPORT. |

### Narrative quest families

When designing future quests, think beyond the enum and choose the story activity first:

- **Evidence investigation:** Inspect multiple concrete clues, separate observation from inference, then report.
- **Care and relief:** Gather or secure supplies, obtain consent, make a validated delivery or treatment, and acknowledge what remains undone.
- **Witness and testimony:** Identify what a person actually saw, why they are credible or limited, and how to protect them.
- **Defense and rescue:** State who or what is threatened, what holding the line achieves, and what repairs or care still follow.
- **Ecological conflict:** Read signs, learn an animal or guardian's rule, prepare, then kill, redirect, restore, or retreat where supported.
- **Ritual and mechanism:** Learn the terms, test the mechanism safely, act in the correct order, and show the material cost.
- **Negotiation and public decision:** Define participants, authority, available alternatives, enforcement, and concrete aftermath.
- **Settlement-building story:** Meet a need, create a facility or practice, assign maintenance, and let residents disagree about it.
- **Companion story:** Give the companion a personal stake and the player a role as witness, protector, investigator, moral counterweight, tactical partner, or trusted pair of hands.

### Quest authoring rules

Every refined stage should answer:

1. Why is this action needed now?
2. Why is this player involved?
3. Where exactly does it happen?
4. What visible evidence or event completes it?
5. What does completion prove?
6. What does it explicitly not prove or repair?
7. Who must receive the report or make the decision?
8. What changes afterward?

Do not let a kill counter imply that cargo was recovered, patients were treated, a road was repaired, or a community agreed. Keep encounter, recovery, delivery, report, decision, and aftermath as separate beats when they are separate facts.

### Current location limitation

All 26 main quests use named destinations. Across the full catalog, **84 companion or side quests with 202 stages still use generic indexed camps, fields, graveyards, or similar sites**. Their prose may contain place-like wording that does not correspond to a real named map marker. These stages require relocation decisions before dialogue gives precise directions.

## 12. Current main campaign

The 26 live quests already have authored offers, questions, evidence, reports, and completion reactions. After the Memory chapter, regional branches can open in flexible order, subject to the current prerequisite graph.

### Act I: Ashes and Oathstead

1. **Wake Among Ashes** — Examine the burned shrine and establish what was damaged.
2. **Proof in the Road Dust** — Recover the carved fragment and ash evidence.
3. **Oathstead Must Stand** — Remove six wolves from the timber road so workers can use it; this does not build the palisade.

### Act II: Archive and Memory

4. **Names Under Dust** — Read ward records and identify missing instructions.
5. **The Missing Vault Instructions** — Defeat Crowhook cache guards, then separately recover the keeper's page.
6. **The First Socket** — Inspect the Old Oath Vault seal and Memory pedestal; recover the Stone of Memory.

### Act III: The mainland regional chapters

**North / Iron**

7. **Watchtower Without Bells** — Inspect a split bell, read watch names, and compare a copied signal.
8. **Raiders at the Pass** — Defeat marked brutes blocking the supply route.
9. **The Stolen Watch Oath** — Defeat Kharvok, inspect his fallen standard, and show how an oath ending was overwritten before receiving Iron.

**South / Ember**

10. **Shrine Without Shadow** — Inspect the guest cup, altered welcome, and plugged outlet.
11. **Caravan of Glass** — Defeat ember imps so a recovery party can reach the caravan; the fight does not itself deliver the cargo.
12. **The Ember Socket Rite** — Inspect the cradle, disconnect Ember, then open the vessel outlet and release one captive guest.

**Fenlands / currently Tides**

13. **The Bell That Rang Alone** — Survey Reedbank's bell-rope fittings.
14. **Medicine for Mireford** — Gather fever reed; preparation and treatment happen after the report.
15. **Miredepth Below** — Defeat Velmora and currently receive the Stone of Tides.

**West / Hunger**

16. **The Toll Ledger** — Inspect grain-route toll entries.
17. **Redcap Trade** — Defeat supply-route raiders.
18. **Glowing Thing in the Mud** — Defeat the marked scavenger and receive Hunger after reporting.

**Other mainland stones**

19. **The Orchard Ward** — Defeat Rootmaw and recover Roots; the orchard still needs tending.
20. **Names on Cold Stone** — Defeat the Nameless Warden and recover Graves; damaged names still need restoration.
21. **The Cold Road** — Defeat the Hailback Broodmother and recover Frost; Elric still must arrange supplies and patrols.
22. **The Missing Bell Rope** — Inspect Glimmerfen's foundations and recover Bells through Nessa's report; inspection does not install rope or hooks.
23. **The Blackvault Mark** — Defeat Sareth and recover Ash; this does not clear every Blackvault ward.

### Act IV: Defend and gather support

24. **A Camp Worth Defending** — Repel Morvane's raid and receive Oaths through Oathstead's voluntary defense.
25. **The Kingdoms Answer** — Obtain limited commitments from Mirella, Odrick, Selene, Ysra, and Solari, then receive Dawn. Promised forces and supplies are preparations, not already delivered assets.

### Act V: The Gate

26. **The Twelve Stones of the Gate** — Use the portal, hear Vaelthara's coercive terms, and begin the existing Hollow Throne confrontation. Opening the Gate does not mean Vaelthara has been defeated or the network has been reformed.

## 13. The twelve stones

| Stone | Narrative function | Current live recovery | Target change or custody |
| --- | --- | --- | --- |
| **Memory** | Preserves identities and terms recognized by the network. | Old Oath Vault, through Selene. | Keep in Archive chapter; make omissions demonstrable. |
| **Iron** | Holds a boundary under physical or magical pressure. | Kharvok / Banner Cairn, through Odrick. | Keep northern role; connect to compelled ancestral defenders. |
| **Ember** | Transfers power between willing sources. | Sunken Shrine Forge, through Solari. | Live sequence already exposes transfer changed into extraction. |
| **Tides** | Opens and closes routes across shifting boundaries. | **Currently Miredepth in the Fenlands.** | **TARGET:** Move to a Lantern Isles crossing after the Eight-Flood Serpent chapter. |
| **Hunger** | Draws in surplus force during a crisis. | Redcap scavenger, through Mirella. | Target western use consumes harvests and sustains an unnatural hunt. |
| **Roots** | Distributes protection through living places. | Rootmaw / Oakhaven orchard. | Expand toward restoration and the Greenward without duplicating the stone. |
| **Graves** | Stops the network treating dead participants as living servants. | Nameless Warden / Stonegate, through Hollis. | Restore names and end broken burial service. |
| **Frost** | Slows a failing process so people can intervene. | Hailback Broodmother / Snowrest, through Elric. | Expand into a winter suspension trapping people and danger. |
| **Bells** | Coordinates warnings and responses over distance. | Glimmerfen foundations, through Nessa. | Consolidate the Fenlands chapter around Bells and the Deep Listeners. |
| **Ash** | Safely releases spent magical residue. | Sareth / Blackvault, through Damar. | Confront accumulated effects of suppressed discharge. |
| **Oaths** | Recognizes freely committed participants. | Oathstead defense, through Maelis. | Preserve voluntary defense as the point. |
| **Dawn** | Restarts the system under an agreed configuration. | Five mainland commitments, through Maelis. | Target gathering also recognizes Freehold and eastern participation. |

There must remain exactly twelve campaign stones. A folklore boss may open a route or grant equipment, but must not introduce a thirteenth stone.

## 14. Target campaign ending

The current game does not yet implement the final political choice. The target endings follow a necessary defeat of Vaelthara:

- **Renewal:** Build a distributed network whose participants can renegotiate or withdraw. Regional outcomes determine how difficult maintenance becomes.
- **Custodianship:** Retain more central structure under limited, supervised authority. It offers immediate stability but preserves dependence and difficult reform work.
- **Severance:** Remove central command. Local protections survive where the player prepared them; other communities face hard rebuilding.

No ending should be labeled perfectly good. Each must explain concrete local consequences, remaining costs, and which promises the player can realistically keep.

## 15. Dialogue writing guide

### Basic style

- Lead with the concrete situation before metaphor or cultural vocabulary.
- Use readable nouns and verbs: name the person, object, place, danger, and next action.
- Introduce unfamiliar terms on first use. A listener should understand the practical problem without having read the setting bible.
- Use short, high-signal exchanges suited to the dialogue panel. Reveal the full offer through staged questions rather than one long speech.
- Let tired workers, children, cooks, craftspeople, skeptics, and officials sound different. Not everyone speaks in proverbs.
- Use humor, interruptions, preferences, and ordinary work. A culture cannot exist only through ceremonies and crises.
- Avoid simulated real-world accents or random foreign words as shorthand for culture.
- Regional surnames can be translated bynames; a name does not determine birthplace.

### Evidence and knowledge

- Separate **what happened**, **what a speaker observed**, **what they infer**, **what their tradition teaches**, and **what the institution wants**.
- A rumor must have a source: who heard it, saw it, or repeated it?
- An NPC may be wrong, but the player needs enough dependable evidence to act fairly.
- NPCs react only to events they can plausibly know.
- Asking a question does not accept a quest, make a political commitment, or choose an outcome.
- A companion's useful practical information should not be locked behind intimacy.

### Quest-offer structure

Use this progression:

1. One concrete hook.
2. What is physically happening.
3. Why it matters to this speaker.
4. Why the player is needed.
5. What worsens if nothing is done.
6. The first named place, person, clue, or encounter.
7. A clear acceptance option with known scope.

For companion quests, always answer: why now, why the player, what happens if ignored, and where to begin.

### Voice guardrails

- Maelis: care first, then factual sequence and practical work.
- Selene: distinguish records, observations, and hypotheses.
- Odrick: command economy and survival, with accountability under the brevity.
- Solari: ritual precision, hospitality, and willingness to name uncertainty or institutional failure.
- Mirella: agreements, routes, cargo, authority, and the limits of a deal.
- Ysra: sound, water, weather, navigation, and local limits.
- Damar: physical evidence, maintenance, residue, and skepticism.
- Vaelthara: comprehensible material offer plus explicit loss of freedom; never mere theatrical evil.
- Companions: preserve the individual voice notes in section 9; cultural identity is context, not a complete personality.

### Relationships

- Friendship must have its own satisfying scenes and future plans.
- Romance is optional and depends on individual consent, not completion of a regional rite.
- Higher trust changes what a companion shares, asks, admits, or challenges; it should do more than add warmer adjectives.
- Include competence, pleasure, disagreement, vulnerability, and ordinary shared activity for every principal companion.
- A quest decision can leave a companion in disagreement without converting the scene into a simple approval score.

## 16. Continuity rules: do not contradict these

1. Alderfall primarily names the central realm and the historical alliance, not necessarily the whole planet.
2. The target coalition is five mainland signatories plus independent Freeholds and the Lantern Isles. The live commitment quest still uses five mainland representatives.
3. There are exactly twelve socket stones.
4. Tides is still a Fenland reward in the live game; moving it east is target work.
5. Vaelthara is an active tyrant with a history of coercion against her; that history does not excuse her domination.
6. Vaelthara did not cause every personal tragedy, cult, monster, broken bridge, or failed harvest.
7. A spirit, ancestor, apparition, intelligent giant, remembered object, or fire-being is not automatically a demon.
8. Folklore rules are local and discoverable, not universal superstition puzzles.
9. The player has no required royal bloodline, ancestry, gender, personality, romance, or fixed class for narrative purposes.
10. No companion is the definitive representative of a culture, and no companion is required to finish the main story.
11. Oathstead is a shared settlement, not a museum of regional stereotypes.
12. Killing an enemy proves only that encounter's result. It does not automatically recover cargo, treat patients, repair infrastructure, settle a haunting, or secure political consent.
13. Never advertise a nonlethal route, surrender branch, escort, persistent NPC survival, new building, or world-state change unless the game supports it.
14. Completed target scenes cannot be referenced as past events until implemented. This especially applies to the Lantern Isles, new bosses, Thorn Hall resolution, drowned infirmary, final network choice, and Vaelthara war-brazier audience.
15. Use the fixed names in the location library for main-story directions. Do not disguise a generic indexed field or camp with invented proper nouns.

## 17. Recommended next writing packages

These are the highest-value self-contained tasks for future story and dialogue work:

1. **Eastern campaign packet:** Nao, local gate tenders and sanctuary residents, the Eight-Flood Serpent approach, the Tides crossing, Freehold-to-Isles travel, and consequences of moving Tides out of Miredepth.
2. **Fenland consolidation:** Rebuild the regional spine around Bells, the Deep Listeners, drowned calls, care routes, and Velmora without duplicating Tides.
3. **Post-raid Vaelthara contact:** Implement the war-brazier audience or another actual communication method, with question, refusal, and time-to-consult branches but no fake surrender option.
4. **Final network resolution:** Define evidence requirements, representatives, supported outcomes, Vaelthara confrontation, local epilogues, and save-state consequences for Renewal, Custodianship, and Severance.
5. **One companion at a time:** Finish each eight-part target arc with named places, supported actions, testimony, decisions, aftermath, ordinary conversations, friendship, and optional romance. Do not rewrite all nine superficially in parallel.
6. **Named-site migration:** Replace the remaining 202 generic companion and side stages with actual locations before writing precise travel directions.
7. **Target boss vertical slice:** Rootmaw is the most practical prototype because it already exists in the live campaign. Add evidence, preparation, alternate resolution only when the mechanics and world state support them.
8. **Ambient regional voices:** Expand workers, families, visitors, doubters, mixed communities, festivals, hobbies, and reactions to campaign changes. Keep paired NPC conversations responsive rather than two unrelated monologues.
9. **Smaller recruit pass:** Give existing hireable allies individual origins, current needs, ordinary preferences, and location-aware recruitment lines without assigning each an eight-chapter arc.

## 18. Workspace references

- [Setting and Story Bible](world-framework.md): target world, regions, supernatural rules, campaign, cast, stones, faith, and bosses.
- [Narrative Rewrite Brief](narrative-rewrite-brief.md): NPC specification, companion packets, sample scenes, and implementation guidance.
- [Location Library](location-library.md): fixed names, identities, inhabitants, directions, and current main-story bindings.
- [Dialogue Clarity and Place Review](dialogue-clarity-and-place-review.md): first-use explanations and comprehension rules.
- [Faith, Cults, Camps, and Dungeons](faith-cults-and-dungeons.md): religious institutions, factions, site logic, and proposed story slice.
- [Regional Bestiary and Bosses](regional-bestiary-and-bosses.md): creature ecology, boss designs, preparation, outcomes, and combat limits.
- [Quest Narrative Implementation](../Java/docs/quest-narrative-implementation.md): exact live coverage, persistence rules, verification, and remaining work.
- [Main Story Rewrite Pass](../Java/docs/main-story-rewrite-pass.md): live dramatic throughline and revised objective contracts.
- [Main Story Runtime Dialogue](../Java/docs/main-story-dialogue.md): current playable wording for all 26 campaign quests.
- [Companion Quest Segments](../Java/docs/companion-quest-segments.md): the five bespoke implemented companion chapters.
- [Companion Quest Relevance Rubric](../Java/docs/companion-quest-relevance-rubric.md): quest-offer structure and player-role checks.
- [Whole-Catalog Quest Location Audit](../Java/docs/quest-location-audit.md): all generic location bindings still needing migration.

## 19. Ready-to-paste instruction for ChatGPT

Use the following after attaching or pasting this handoff:

> You are helping develop the story and dialogue for *Echoes of Alderfall*. Treat LIVE material as current game continuity, TARGET material as approved future direction, AUTHOR TRUTH as information that characters may not yet know, and OPEN material as requiring proposals rather than assumptions. Preserve the supernatural rules, regional identities, character voices, twelve-stone continuity, and implementation boundaries in this handoff. Write concrete, staged conversations that identify the current problem, evidence, stakes, location, player role, and limits of what an action proves. Do not invent a supported branch, location, companion requirement, or aftermath unless I explicitly ask you to design it as new content. When proposing new content, state required quest states, locations, NPC knowledge, player choices, and lasting consequences separately from dialogue.

For a specific task, add:

> Task: [name the quest, character, region, or scene]. Output: [for example, a beat sheet; dialogue tree; quest-stage table; ambient lines; companion scene; or implementation-ready content packet]. Status: [rewrite existing LIVE content / design new TARGET content]. Constraints: [word count, number of branches, location, required evidence, supported outcomes, and any mechanics already available].
