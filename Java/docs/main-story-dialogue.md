# Main story: playable dialogue and objectives

Generated from the runtime content by `MainStoryDialogueTest`. Evidence-gated questions appear only after the named observation. Optional questions award no progress or approval. Combat resolution is fixture-driven in the integration checks; inspections use world interactions.

## Wake Among Ashes (`ms_wake_ashes`)

You survived the shrine breach. Oathstead needs to learn what failed before its people put their lives behind the same kind of protection.

### Examine the burned shrine

Opening: Sit down a moment. You reached us alive; we can start there. When you can manage the walk, show me where the road shrine broke. The families here are trusting another ward to keep them safe.

Action: Examine the burned altar in the Road Shrine, reached from Oathstead's shrine path.

Observed result: The altar is scorched, but the damage is concentrated around its ward socket.

### Inspect the broken socket

Opening: The socket took the worst of it. Look inside the break; a mark beneath the soot may survive even where the outer carving is gone.

Action: Inspect the broken socket beside the altar in the Road Shrine.

Observed result: Carved lines continue inside the broken socket. A loose fragment still carries part of the pattern.

Report: The socket was the center of the damage, and part of its carving survived. Recover that fragment. Selene in Archive City may be able to tell us whether our camp has the same weakness.

> Player: Vaelthara let me live. Why?
>
> NPC: You remember her leaving you. I believe you. I cannot tell you why she did it. She may want us frightened enough to obey her.

> Player: Then bringing me here puts you in danger.
>
> NPC: The shrine stood on our road. Whatever broke it was already at our door. You brought us a warning; you did not bring us this war.

> Player: That shrine was supposed to protect travelers.
>
> NPC: My mother left a heel of bread there before every winter journey. I thought she was feeding birds. Now I wish I had asked what the keeper did with the rest.

> Player: Will bread stop Vaelthara?
>
> NPC: No. But someone once knew how to keep that place working. Find what survived the fire and we may learn something useful.

Requires observed evidence: `shrine_damage`.

> Player: The worst damage is around the socket.
>
> NPC: Then start there. A burned roof tells us there was fire. A broken ward socket may tell Selene why the protection failed.

> Player: Does that mean she used the ward against us?
>
> NPC: It means we should ask. I watched you stagger into camp; I did not watch her break the shrine. Keep those two things separate.

## Proof in the Road Dust (`ms_road_dust`)

Carry physical evidence of the attack toward an answer about Oathstead's defenses, instead of facing Vaelthara again with no more knowledge than before.

### Secure the carved fragment

Opening: There is a loose fragment in the shrine's broken socket. Bring it out with its carving intact, then collect ash from beside it. Selene needs something she can examine.

Action: Recover the carved fragment from the marked rubble in the Road Shrine.

Observed result: You secured the fragment. Its inner carving is still readable; this does not yet explain why the ward failed.

### Examine the ash deposit

Opening: The fragment is secured. Collect ash from beside its socket as well; Selene needs to compare the material, not just hear my description.

Action: Collect the marked ash sample beside the shrine's broken socket.

Observed result: You secured an ash sample from beside the socket. Selene still needs to compare it with the Archive's records.

Report: Keep the fragment wrapped; we have little enough of that carving left. You have the ash too. Before you leave for Archive City, help me clear the wolves from our workers' road.

> Player: I want to go after Vaelthara.
>
> NPC: So do I. But you met her once and barely reached us. Bring Selene something she can examine before you face that power again.

> Player: And while I am chasing answers?
>
> NPC: I will keep the camp organized. We still need food, shelter, and a road people can use. That work gives you somewhere to come back to.

Requires observed evidence: `shrine_fragment`.

> Player: I have the carved fragment. Is it enough?
>
> NPC: Enough to put a real question before the Archive. Collect the ash too; Selene may be able to compare the burn with older breaches. I cannot read either one.

## Oathstead Must Stand (`ms_oathstead_stand`)

Give Oathstead's workers room to build while you pursue the knowledge its future defense will need.

### Oathstead Must Stand

Opening: Six wolves are threatening the road our timber workers use. They cannot build shelter while watching the trees for teeth. Deal with that danger, then take your shrine evidence to Selene.

Action: Defeat 6 marked Grey Wolf opponents. Follow the quest markers to their encounter.

Observed result: Six wolves have been dealt with. Maelis can send the next work party along the camp road; the palisade still needs building.

Report: Good. Our workers can use the road again. Take the fragment and ash to Selene in Archive City. Tell her there are people sleeping behind the ward you need her to understand.

> Player: Why wolves, when Vaelthara is out there?
>
> NPC: Because the people cutting timber have to reach the trees alive. A grand plan will not keep a wolf off a hungry worker.

> Player: Are you asking me to stay?
>
> NPC: Long enough to clear this danger. Then take the shrine evidence to Selene. I need both a usable road and an answer about our wards.

> Player: What kind of place are we building?
>
> NPC: One where a person can ask for shelter without first proving useful. Once they have eaten, we can ask what work they can manage. I would like that much to survive the winter.

## Names Under Dust (`ms_names_dust`)

Establish whether the road shrine and Oathstead share a ward design, then find the builders' instructions needed to investigate its weakness.

### Read the ward maintenance record

Opening: You brought a piece of the shrine Vaelthara broke. The carving resembles our ward diagrams. Before I send you back to Oathstead with a comforting guess, compare it with the maintenance record here.

Action: Read the marked ward maintenance record in Archive City.

Observed result: The record depicts the same carving as the shrine fragment and lists Oathstead among the protected sites.

### Check the missing instructions

Opening: There it is: Oathstead, under the same ward pattern. Now check the builders' index. We need the instructions for that pattern, not another reassurance from me.

Action: Inspect the damaged ward index beside the Archive's reading station.

Observed result: The index lists northern and southern ward instructions, but its vault-access page has been removed.

Report: The record names Oathstead under the shrine's pattern. The index points to northern and southern instructions, but the vault-access page is gone. Our theft report names Crowhook. That is our next lead.

> Player: Can Oathstead survive the attack I saw?
>
> NPC: I cannot promise it. Your fragment resembles our ward diagrams. Read the maintenance record with me; resemblance is not enough to send you home reassured.

> Player: Can you at least repair the stone?
>
> NPC: The masonry, yes. But if the protection failed because of how it was commanded, a fresh block would leave the same weakness.

Requires observed evidence: `archive_record`.

> Player: Oathstead is listed under the same ward pattern.
>
> NPC: Then your fear has a basis. The camp and shrine belong to the same design. We need the builders' instructions before we decide how to defend it.

> Player: You are the archivist. Why don't you have them?
>
> NPC: I have what the Archive kept. That is an answer I used to give with pride. Look at the builders' index; we need to establish what is missing.

Requires observed evidence: `archive_index`.

> Player: The vault page was removed. Was that deliberate?
>
> NPC: A page is missing from a working index. That establishes a loss, not the thief's reason. Crowhook is our lead for recovering it; the older omissions will take longer to explain.

## The Stolen Index (`ms_stolen_index`)

Recover the missing vault instructions so Selene can investigate the network protecting Oathstead.

### Clear the page cache

Opening: The Archive's theft report says the missing page was taken toward Crowhook. Three cutthroats guard the marked cache. Clear them, then search the papers; killing the guards will not open our vault.

Action: Defeat the three marked cutthroats guarding the page cache at Crowhook.

Observed result: The marked guards are defeated. The page still needs to be recovered.

### Recover the vault page

Opening: You have cleared the guards. Search the document cache before leaving Crowhook; the vault is still closed without its instructions.

Action: Search the marked document cache at Crowhook for the vault page.

Observed result: You recovered the page describing the Old Oath Vault's entry seal. It identifies the Stone of Memory inside.

Report: This is the vault page. It names a Stone of Memory and gives the entry seal beneath the Archive. We can finally compare an instruction with the place it was written for.

> Player: How do you know to look at Crowhook?
>
> NPC: The Archive's theft report names Crowhook as the place the stolen papers were taken. I did not see the theft. Until you search that cache, it is a lead.

> Player: Why would raiders want ward instructions?
>
> NPC: They may know what the page opens, or simply know that we will pay for it. Recover it before we decide whose plan this serves.

Requires observed evidence: `ms_stolen_index_guards`.

> Player: The guards are dead. Do we have what we need?
>
> NPC: The cache is accessible. We still need the page itself. Do not leave the answer behind because the fighting is over.

Requires observed evidence: `ms_stolen_index_page`.

> Player: What does the Stone of Memory remember?
>
> NPC: The vault page calls it a keeper of names and terms. If that description is accurate, it may preserve what the wards were instructed to recognize. Let us compare the actual seal before touching it.

## The First Socket (`ms_first_socket`)

Recover Memory, the network's keeper of names and terms, and discover why reaching Vaelthara requires the other eleven stones.

### Examine the vault entry seal

Opening: The Old Oath Vault is beneath this city. Compare its entry seal with the page you recovered, then examine the Memory pedestal. I want you coming back with an answer, not becoming another reason to seal the stairs.

Action: Enter the Old Oath Vault from the marked entrance in Archive City and inspect its seal.

Observed result: The entry seal matches the recovered instructions. The chamber contains a twelve-socket mural.

### Examine the Memory pedestal

Opening: The entry seal matches. Now examine the Memory pedestal. Its instructions should tell us what the stone can be asked to preserve.

Action: Inspect the marked Memory pedestal, then report to Selene to complete the recovery.

Observed result: The Memory pedestal responds to the recovered instructions. Its inscription describes preserving the names and terms of ward service. Report to Selene to complete recovery of the stone.

Report: Take the Stone of Memory. The mural places it among twelve functions of one network. Highwall keeps boundary lore; Sanctum tends the fire vessels; the western and Fenland routes preserve other pieces. We need those stones to reach the command site, and their keepers to understand what we are carrying.

> Player: Will this stone tell us how to defeat Vaelthara?
>
> NPC: It may tell us how the network recognized its participants. That could explain how she entered a protected shrine. I will not promise a weapon before we understand what we have.

> Player: What am I looking for in the vault?
>
> NPC: First, a seal matching the recovered page. Then the pedestal marked Memory. If the page and the chamber disagree, the disagreement matters.

Requires observed evidence: `vault_seal`.

> Player: Why does the vault show twelve sockets?
>
> NPC: The page names Memory; the mural places it beside eleven other functions. We have been maintaining pieces of a larger defense. Reaching its command site will take more than this one stone.

> Player: Were all twelve stones weapons?
>
> NPC: The old vocabulary includes boundaries, crossings, warnings, and the release of spent power. People needed to live behind the defense. The stones served that life too.

Requires observed evidence: `vault_memory`.

> Player: The pedestal remembers names. Can it judge who was right?
>
> NPC: No inscription can do that for us. Memory can preserve a promise and who made it. Whether someone was forced to make that promise is a question we must still ask.

> Player: Could the old defenders have done that?
>
> NPC: People defending their homes can still harm others. I will not accuse particular builders without evidence. I will not assume their victory excuses everything either.

## Watchtower Without Bells (`ms_watchtower_bells`)

Trace the failure of Highwall's warning chain on the road carrying food and refugees between the northern holds and the rest of Alderfall.

### Inspect the split signal bell

Opening: The Highwall pass is losing its warning signals. Families used to leave ale at the cairns and trust the watch to answer the bells. Now the road is dangerous and the bells are broken. Inspect the three marked sites before I send more people through.

Action: Inspect the marked signal bell on the northern watch route.

Observed result: The bell's clapper is wedged with a strip of banner cloth. It could not ring freely. The cloth carries a stitched household name.

### Read the cairn watch names

Opening: Read the marked watch inscription beside the northern cairns.

Action: Read the marked watch inscription beside the northern cairns.

Observed result: The cairn inscription names the same household as the cloth. Its watch oath ends at the spring thaw; the name belongs to a duty with an ending.

### Compare the raiders' signal

Opening: Inspect the marked signal scratched beside the watch route, then report to Odrick.

Action: Inspect the marked signal scratched beside the watch route, then report to Odrick.

Observed result: A newer scratch repeats the watch's safe-passage signal beside a raider mark. The bell was obstructed and the signal copied. Neither finding proves Vaelthara ordered it.

Report: The bell was obstructed, a household's watch name used, and our safe-passage signal copied. This was interference, not just winter damage. Three brutes still hold the marked route. Break their force before I send repair crews through.

> Player: What do northern bells have to do with the shrine?
>
> NPC: A ward needs warning before it needs strength. Our pass bells used to carry an alarm from cairn to watchtower. If they fail, food carts and fleeing families reach the danger before my soldiers hear of it.

> Player: Do the ancestors really answer them?
>
> NPC: That is what the cairn keepers teach. I know living watchmen used the same signals. Inspect the three sites; I need to know where the chain broke.

> Player: Why are there burial cairns beside a military road?
>
> NPC: The holds bury their watchkeepers facing the pass. Families bring warm ale at the first snow and ask for another winter's warning. Kharvok has made people afraid to visit their own dead.

Requires observed evidence: `north_watch_roster`.

> Player: The watch oath was supposed to end at spring thaw.
>
> NPC: Then the ending mattered to the people who swore it. The cloth in the bell carries one of their names. I want to know who is using that name now.

> Player: Does that prove Kharvok commands the dead?
>
> NPC: It proves someone obstructed our bell with named banner cloth. We need his actual standard before we can say what he changed.

## Raiders at the Pass (`ms_raiders_pass`)

Remove the force preventing Highwall from restoring its road and responding to Kharvok's command over the northern boundary.

### Raiders at the Pass

Opening: Three orc brutes hold the marked route. While they stay there, neither carts nor repair crews can use it. Break that force; afterward we face the standard Kharvok has raised over the pass.

Action: Defeat 3 marked Orc Brute opponents. Follow the quest markers to their encounter.

Observed result: The three marked orc brutes are defeated. Odrick still needs to organize patrols before he can promise travelers a safe pass.

Report: The brutes are down. I still need patrols on that road. Your next enemy is Kharvok, the Banner-Bound: the keepers fear he has twisted the names sewn into surrendered standards into commands.

> Player: Are these raiders serving Vaelthara?
>
> NPC: I can place the brutes on the pass. I cannot place Vaelthara in their camp. Kharvok benefits while our road is closed; that gives us an enemy we can reach.

> Player: Will you reopen the road when they fall?
>
> NPC: I will need patrols first. You can break this force. I must answer for the families I send behind you.

> Player: What is wrong with Kharvok's banners?
>
> NPC: Northern standards carry the names of the households that raised them. Kharvok stitches surrendered flags together. The keepers believe he is using those names to command service. I know he has a force behind him.

## Frosthollow Standard (`ms_frosthollow_standard`)

Break Kharvok's hold on the northern pass and recover Iron, which strengthens a boundary but cannot decide whom it should protect.

### Frosthollow Standard

Opening: Kharvok has raised his stolen standards at Frosthollow. Defeat him, then examine the fallen standard. We have seen names used to obstruct our warning bell; now we need to know what he has sewn into those names.

Action: Defeat 1 marked Kharvok the Banner-Bound opponents. Follow the quest markers to their encounter.

Observed result: Kharvok is defeated. His fallen standard can now be examined; the names sewn into it still need to be read.

### Read the fallen standard

Opening: Examine the marked fallen standard at Kharvok's encounter site, then return to Odrick.

Action: Examine the marked fallen standard at Kharvok's encounter site, then return to Odrick.

Observed result: Beneath the added stitching, the standard promises a watch only until the spring thaw. New thread covers that ending and names Kharvok as the one who may release the watch. You have evidence of an altered obligation, not merely a defeated commander.

Report: Kharvok has fallen, and his standard shows how he changed the oath: the watch could end only when he released it. Take Iron. It holds a boundary under pressure. I will ask the cairn keepers to tend the names we found; his defeat alone does not finish their work.

> Player: Why should the dead obey Kharvok?
>
> NPC: The cairn keepers say a watch oath binds a name to a boundary. They fear his sewn banners have turned that duty into obedience to him. Their reading may explain the dead on the pass; it does not make him their rightful commander.

> Player: Did Highwall swear that kind of oath?
>
> NPC: Our watch still names the households it protects. If those words can be twisted, I owe the living an answer as much as I owe the dead.

> Player: What will the Stone of Iron do for Oathstead?
>
> NPC: The keepers describe it as strength lent to a boundary. It cannot choose whom a wall should shelter. Defeat Kharvok, examine his standard, and return to me for the stone.

Requires observed evidence: `north_fallen_standard`.

> Player: He covered the ending of their oath with his own name.
>
> NPC: Then we have more than a keeper's fear. His stitching made release depend on him. We can show the households exactly what was changed.

> Player: Are the dead free now?
>
> NPC: We have stopped him and read the standard. I still need the cairn keepers to attend to the disturbed oaths. I will not announce peace at their graves before that work is done.

## Shrine Without Shadow (`ms_shrine_shadow`)

Investigate southern fire wards whose failures threaten caravan hospitality and the routes that keep the Sunrealm supplied.

### Inspect the guest cup

Opening: A well-road shrine should offer shade and a safe lamp. This vessel has become dangerous to approach. Examine its guest cup, the words of welcome, and the outlet. Before we condemn the guest, I want to know what its keepers built around it.

Action: Inspect the marked guest cup at the damaged southern shrine.

Observed result: The cup beside the altar bears a greeting to a fire guest. Its rim is scorched on the inside, beneath a later iron collar.

### Read the welcome beneath the collar

Opening: Read the marked welcome inscription at the southern shrine.

Action: Read the marked welcome inscription at the southern shrine.

Observed result: The old welcome says the guest gives warmth until moonset, then may depart. A newer command cut across it demands warmth until the keeper releases the vessel.

### Examine the sealed outlet

Opening: Inspect the marked outlet of the southern fire vessel, then report to Solari.

Action: Inspect the marked outlet of the southern fire vessel, then report to Solari.

Observed result: The outlet named in the welcome has been plugged with iron. Soot has collected behind the plug. This vessel's departure route was physically closed; the survey does not identify who ordered it.

Report: The welcome allowed departure at moonset. Someone changed that command and plugged the outlet with iron. This vessel held a captive. We must reach the forge and disconnect its draw before opening it. First, clear the imps on the caravan route.

> Player: Is the sacred fire a god?
>
> NPC: Our temples honor the sun. The presence welcomed into a fire vessel is a guest, with duties of its own. Worship and hospitality are different obligations.

> Player: Then why would a guest burn its keepers?
>
> NPC: It might be injured, trapped, or no longer the presence the keepers welcomed. I will not name its anger wicked before we examine the shrines.

> Player: Could this be the same failure I saw?
>
> NPC: Possibly. Your shrine lost its protection; ours have become dangerous to approach. Examine the guest cup, its welcome, and its outlet. We need to know how this vessel was meant to work.

Requires observed evidence: `south_welcome_words`.

> Player: Someone changed 'until moonset' to 'until released'.
>
> NPC: Then someone changed the terms of the welcome. That is written into this vessel. We still need to examine whether the guest had a way out.

> Player: Does your temple teach that command?
>
> NPC: I was taught that the fire is a guest. If our keepers used this altered command, they betrayed the rite I learned. I need to find who used it and when.

Requires observed evidence: `south_closed_outlet`.

> Player: The guest's way out was plugged with iron.
>
> NPC: Then this was confinement, whatever name the keeper gave it. We have the changed words and the blocked outlet. At the forge, we must disconnect the draw before opening the vessel.

## Caravan of Glass (`ms_caravan_glass`)

Reach the southern forge by removing the creatures that prevent recovery along the caravan route.

### Caravan of Glass

Opening: Six ember imps threaten the shrine road. The caravan carries vessels made for guest flames, and supplies we need to recover. Clear the imps; afterward I can arrange the work of bringing those stores back.

Action: Defeat 6 marked Ember Imp opponents. Follow the quest markers to their encounter.

Observed result: Six ember imps are defeated. Solari can arrange the caravan's recovery; the fight itself did not deliver its oil or supplies.

Report: The imps are dealt with. I will arrange recovery of the caravan stores. Go to the marked forge shrine: examine the cradle, disconnect Ember, then open the vessel. I authorize the guest's release. We will have to replace the warmth it was forced to give.

> Player: Are the ember imps the shrine's guests?
>
> NPC: No keeper has identified them as such. They are dangerous creatures on a route we need. Driving them off will let us reach the forge; it will not explain every burned vessel.

> Player: Why does the caravan carry glass?
>
> NPC: A sealed lamp carries a guest flame between wells. Our glassmakers leave room for the heat to breathe. Cheap vessels crack; an impatient keeper can lose a caravan's protection in one night.

> Player: Will clearing the road finish the rite?
>
> NPC: It makes recovery possible. The caravan stores still need collecting, and the forge must be examined. I will not ask you to pretend a battle delivered our oil.

## The Ember Socket Rite (`ms_ember_socket_rite`)

Recover Ember with an understanding of the difference between sharing protective power and extracting it from a captive source.

### Inspect Ember's transfer cradle

Opening: We found a welcome changed into confinement. At the forge, inspect the transfer cradle, lift Ember clear, then open the guest's outlet. Follow that order. I authorize the release and will answer for the repair work it leaves us.

Action: Inspect the marked transfer cradle at the forge shrine.

Observed result: Ember's cradle is linked to a guest vessel by an iron collar. The directions say to lift the stone clear before opening the guest's outlet; otherwise the cradle keeps drawing heat.

### Lift Ember clear of the cradle

Opening: Use the marked cradle release to lift Ember clear of the transfer channel.

Action: Use the marked cradle release to lift Ember clear of the transfer channel.

Observed result: You lift Ember clear of its transfer channel. The channel stops glowing, but a small flame still presses against the vessel's closed outlet. The guest has not yet been released.

### Open the guest's outlet

Opening: Open the marked outlet of the disconnected guest vessel, then report to Solari.

Action: Open the marked outlet of the disconnected guest vessel, then report to Solari.

Observed result: You unfasten the outlet. A thin flame rises through it, pauses above the guest cup, and vanishes into the daylight. The vessel is empty and cool. This guest has left; the other shrines still need attention.

Report: You disconnected the draw and opened the outlet. The guest left. Take Ember: it transfers power, but is not itself the fire we imprisoned. That vessel is cold now. I will answer for finding willing sources and repairing the shrines; releasing one guest has not done that work for us.

> Player: Why does the forge have a guest's cup?
>
> NPC: A vessel is offered shelter before its fire is asked to give warmth. That is how I was taught the rite. The Stone of Ember transfers power; our prayers insist that the giving must be willing.

> Player: What if the guest cannot refuse?
>
> NPC: We found the changed welcome and the plugged outlet. That vessel was a prison. I will not call its heat a gift. Disconnect the draw before opening the outlet; afterward we must find willing sources.

> Player: Can Ember replace the broken shrine ward?
>
> NPC: It transfers power within the network. Power alone will not repair its instructions. Carry it with Memory; we need to understand both what the ward is told and what feeds it.

Requires observed evidence: `ember_cradle`.

> Player: Why lift the stone before opening the outlet?
>
> NPC: The cradle's own directions say it continues drawing while Ember is seated. Disconnect that draw first. Opening an exit means little if something still holds the guest inside.

Requires observed evidence: `ember_disconnect`.

> Player: The channel is dark. Have I freed the guest?
>
> NPC: You have stopped this cradle taking heat. The flame is still inside. Open the outlet; then it can leave.

Requires observed evidence: `ember_open_outlet`.

> Player: The flame left. Have we weakened the road wards?
>
> NPC: That vessel no longer supplies them. We need willing sources and repaired shrines to replace what was taken. I authorized the release; I will answer for the repair work too.

> Player: And the stone still works?
>
> NPC: It is a means of transfer, not the fire itself. Take Ember for the Gate. We must find power that can be offered without trapping its source.

## The Bell That Rang Alone (`ms_bell_alone`)

Investigate Fenland signals that may guide displaced families toward danger instead of toward shelter.

### The Bell That Rang Alone

Opening: A landing bell is sounding where no keeper should be pulling the rope. A boat can follow that sound straight into deep water. Inspect the three marked rope sites; I need to know what our warning chain can still be trusted to do.

Action: Inspect 3 marked Bell Rope locations. Report only what those inspections establish.

Observed result: You checked the three marked bell-rope sites. Ysra can use the survey to investigate the failing warnings; the cause is still unproven.

Report: Your survey gives the bellkeepers somewhere to begin. Mireford's sick cannot wait for the whole marsh to make sense. Gather fever reed before we face the danger below Miredepth.

> Player: Why listen to a bell no one rang?
>
> NPC: Because a boatman may turn toward it. In fog, a false landing bell can kill a whole ferry. Check the three rope sites before we decide whether we are hearing a warning or a lure.

> Player: Who rings from beneath the water?
>
> NPC: Fenland keepers call them Deep Listeners. Some say they remember the drowned roads. I can teach you our warning rhythms; I cannot tell you who answers every bell.

> Player: What does this mean for Oathstead?
>
> NPC: Your camp needs travelers to reach it alive. Our bells connect the waterways to the roads. A ward behind a palisade will not save a family led into deep water on the way there.

## Medicine for Mireford (`ms_medicine_mireford`)

Keep the Fenland investigation connected to living patients who cannot wait for the campaign's mysteries to be solved.

### Medicine for Mireford

Opening: Mireford needs fever reed. Gather six samples from the marked sources. We can investigate the drowned bells and still make time for people who need help tonight.

Action: Gather 6 Fever Reed samples from the marked sources.

Observed result: Six fever-reed samples are secured. Give Ysra your report so the medicine can be prepared; the patients have not yet been treated.

Report: Six samples. That gives the medicine work a start; the patients still need care. Now we can turn to Velmora below Miredepth, where the keepers say drowned bells are calling the dead.

> Player: Shouldn't we be hunting the thing below the marsh?
>
> NPC: We will. First, gather six fever-reed samples. Mireford has sick people now, and the remedy cannot wait for us to settle the marsh's history.

> Player: Did the bells cause their fever?
>
> NPC: I do not know. A frightening sound and an illness arriving together are a reason to investigate, not a diagnosis.

> Player: What do the reeds mean in the village shrines?
>
> NPC: A fresh bundle marks a household that will shelter a stranded traveler. When the water rises, you can see it above the door. I would like Mireford to be able to keep offering that welcome.

## Miredepth Below (`ms_miredepth_below`)

Defeat the power threatening the marsh crossings and recover Tides, while leaving the identity of every drowned voice an open question.

### Miredepth Below

Opening: Velmora holds the depths below Miredepth. The keepers call her Bell-Drowned, but none can give me a trustworthy account of her first life. We know she is calling the dead against the living. Stop her, then return for Tides.

Action: Defeat 1 marked Velmora, the Bell-Drowned opponents. Follow the quest markers to their encounter.

Observed result: Velmora is defeated. Report to Ysra for the recovered stone. The Fenlands still need their damaged warning bells restored.

Report: Velmora is defeated. Take the Stone of Tides from this recovery. It governs crossings through shifting boundaries. Remember the boat following a bell in fog: opening a route is a responsibility, not merely a way through.

> Player: Is Velmora one of the people the bells drowned?
>
> NPC: The keepers call her Bell-Drowned. Their accounts place her below Miredepth with bells that call the dead. They do not tell me whether she was once a ferryman, a captive, or something older.

> Player: Then why kill her?
>
> NPC: Because she is attacking the living and calling more dead to the surface. I can admit what I do not know without sending another boat into her reach.

> Player: Why is the Stone of Tides here?
>
> NPC: These marshes cover old crossings. The keepers associate Tides with opening and closing a route through changing water. Recover it here; that does not give us command of every river or sea.

## The Toll Ledger (`ms_toll_ledger`)

Trace the missing supplies on Riverside's bridges, where ordinary trade meets the paths and obligations of the Briar Courts.

### The Toll Ledger

Opening: Oathstead needs grain, and our supply route is losing it. Read the three marked toll records. Before I accuse a bridge keeper or a Briar Court, I need to know what the accounts actually say.

Action: Inspect 3 marked Toll Ledger locations. Report only what those inspections establish.

Observed result: The three marked toll records have been examined. Report to Mirella; reading them has not returned the missing grain to Riverside.

Report: We have the toll survey. Eight raiders still hold the marked supply route. Clear them so Riverside can begin looking for its stores; the records alone will not feed your camp.

> Player: How can grain disappear at a guarded bridge?
>
> NPC: The toll keepers count what is declared. Something can cross under a false name, or leave by a road our guards cannot follow. Read the three records before I accuse either a keeper or a court.

> Player: A court? Whose court?
>
> NPC: The Briar Courts hold paths beyond our river roads. Their hunts have rules older than my office. I can collect a bridge toll; I cannot assume the hunt recognizes my seal.

> Player: Oathstead cannot eat an explanation.
>
> NPC: No. That is why I need the route cleared as well as the loss understood. Otherwise the next grain barge follows the first into somebody else's store.

## Redcap Trade (`ms_redcap_trade`)

Disrupt the raiders blocking Riverside's supplies without treating every western creature as a servant of Vaelthara.

### Redcap Trade

Opening: Eight raiders are holding the supply route. Redcaps trade at the edges of Briar hunts, but I cannot tell you who bought this grain. Break their hold first. Then we can pursue what their scavengers carried away.

Action: Defeat 8 marked Goblin Raider opponents. Follow the quest markers to their encounter.

Observed result: Eight marked raiders are defeated. The stolen supply route can now be searched; this does not mean its crates have reached Riverside.

Report: The raiders are dealt with. The stores still need recovering. There is a marked scavenger to stop next; the stone among those stolen goods may explain why this route drew more than ordinary thieves.

> Player: Are these raiders part of a Briar hunt?
>
> NPC: Redcaps trade stolen goods along its edges. That makes the connection worth pursuing. It does not make every goblin a sworn servant of a court.

> Player: So whose orders are we stopping?
>
> NPC: We are stopping eight raiders on the supply route. If we find who pays them, I will happily add a name. I will not invent one to make the fight sound grander.

> Player: What happens to the grain after the fight?
>
> NPC: It still has to be recovered and moved. Clearing the route gives Riverside a chance to do that. I cannot put bread in Oathstead's ovens by declaring the road safe.

## Glowing Thing in the Mud (`ms_glowing_mud`)

Recover Hunger from the stolen supply route and distinguish the network's use of excess force from its possible abuse of living harvests.

### Glowing Thing in the Mud

Opening: The marked scavenger is our next target. We need the stone recovered from the stolen goods. The old accounts call it Hunger: a useful name if you remember to ask what it feeds on.

Action: Defeat 1 marked Goblin Skirmisher opponents. Follow the quest markers to their encounter.

Observed result: The marked scavenger is defeated. Report to Mirella to complete recovery of the Stone of Hunger.

Report: Take the Stone of Hunger. The river accounts say it once drew in surplus force when wards were struck. Find out where that force goes before you feed it more. Riverside still has grain to recover and mouths to fill.

> Player: Why call a stone Hunger?
>
> NPC: Old river accounts describe a stone that swallowed excess force when wards were struck. That sounds useful until someone teaches it to draw from a harvest instead.

> Player: Is that what happened to our grain?
>
> NPC: It is a possibility, not an excuse to forget ordinary theft. We have a marked scavenger to stop and a stone to recover. Selene will have more to work with once it is out of the mud.

> Player: Could we use it against Vaelthara?
>
> NPC: Perhaps it can absorb something she sends at us. First learn what it takes and where that power goes. I would rather owe you a barge of grain than discover we fed the stone another village's winter.

## The Orchard Ward (`ms_orchard_ward`)

Face a wounded guardian of a living ward and recover Roots without confusing the creature's defeat with the orchard's healing.

### The Orchard Ward

Opening: We used to leave the first fallen apple for the stag. Now Rootmaw attacks the people tending the trees. Stop it before another household loses someone to the orchard that feeds us.

Action: Defeat 1 marked Rootmaw Stag opponents. Follow the quest markers to their encounter.

Observed result: Rootmaw is defeated. Rowan can begin tending the damaged orchard; its ward still needs attention.

Report: Rootmaw is down. I will tend the orchard, but that will take more than a fight. Take Roots. Protection once passed through these trees to the households around them; remember those households when you work on the greater network.

> Player: Why would the orchard's guardian attack its keepers?
>
> NPC: We leave the first fallen apple for the stag. We keep an opening in the hedge. Those are the customs I learned. Something has hurt Rootmaw badly enough that it now attacks anyone approaching; I cannot tell you which protection failed.

> Player: Do you want me to kill your guardian?
>
> NPC: I want the people tending this orchard to live. Rootmaw is attacking them. Stop it, and I will take responsibility for tending what remains.

> Player: What does Roots have to do with a stone gate?
>
> NPC: Our ward reaches from tree to tree. The old name for the stone describes protection shared through living ground. If you rebuild the network, remember that its roads run through places people eat from.

## Names on Cold Stone (`ms_names_cold_stone`)

Recover Graves while confronting the possibility that the old defense continues to demand service from people who should be at rest.

### Names on Cold Stone

Opening: The names on Stonegate's graves are damaged, and the Nameless Warden attacks those who approach. Our burial words release a person from duties held in life. I fear that release is failing. Stop the Warden so the graves can be reached again.

Action: Defeat 1 marked The Nameless Warden opponents. Follow the quest markers to their encounter.

Observed result: The Nameless Warden is defeated. Hollis can return to the burial ground, but the scratched names still need restoring.

Report: The Warden has fallen. The names still need restoring. Take Graves: its place in the network is to recognize that a life, and its service, can end. Do not build a new safety that refuses people that release.

> Player: How does scratching out a name wake the dead?
>
> NPC: Our burial rite names the person and ends the duties they held in life. The Graves stone served that boundary. I believe the damaged names are leaving the Warden with a duty it cannot finish.

> Player: So the dead are still being ordered to serve?
>
> NPC: That is my reading, not a confession from the builders. Stop the Warden so we can approach the graves again. Restoring the names is work that comes after.

> Player: Why should this matter to someone still alive?
>
> NPC: Because a promise to protect a road should not outlast the person who made it. If you reach the network's command, remember that some of its servants may have been waiting centuries to be allowed to die.

## The Cold Road (`ms_cold_road`)

Recover Frost from the northern road and recognize the difference between buying time against disaster and trapping a place in endless suspension.

### The Cold Road

Opening: The Hailback Broodmother blocks Snowrest's winter road. We have families waiting on supplies that must come through that pass. Stop her; the road crews will still have work after the fighting.

Action: Defeat 1 marked Hailback Broodmother opponents. Follow the quest markers to their encounter.

Observed result: The Hailback Broodmother is defeated. Elric still needs to arrange winter supplies and patrols; this encounter did not deliver firewood or medicine.

Report: The Broodmother is defeated. We still need supply runs and patrols. Take Frost. The old keepers used it to slow a failing protection until help came. A delay is only mercy if help eventually arrives.

> Player: Is the Broodmother one of Vaelthara's beasts?
>
> NPC: I have no evidence of that. Hailbacks lived above the road before this war. This one is blocking our winter route, and Snowrest needs that route open.

> Player: Will killing it end this winter?
>
> NPC: No. It removes the creature keeping us from the pass. We still need supplies, patrols, and whatever work the frost ward requires.

> Player: Why keep a stone that preserves the cold?
>
> NPC: The old keepers used Frost to slow a failing ward until help arrived. In these mountains, a little time can save a settlement. A delay that never ends can bury one.

## The Missing Bell Rope (`ms_missing_bell_rope`)

Recover Bells while establishing the physical work still needed to carry warnings between the Fenlands and Oathstead.

### The Missing Bell Rope

Opening: A bell line needs a sound foundation before it needs a heroic speech. Inspect both marked foundations. The Bells stone belongs to this network, and I need to know what we can build around it.

Action: Inspect 2 marked Old Bell Foundation locations. Report only what those inspections establish.

Observed result: Both marked bell-foundation sites have been inspected. Nessa can use the survey for repairs; no new rope or hooks have been installed.

Report: That gives me a repair survey. Take Bells. It coordinates warnings across distance, but it still needs keepers, rope, and metal that holds. A promise to warn someone is work, every day.

> Player: Can the bells carry a warning all the way to Oathstead?
>
> NPC: That is what this network was built for. The Bells stone coordinates the signals, but it still needs working bells. Inspect both old foundations so I know what the repair can use.

> Player: Can I ring it now?
>
> NPC: You can inspect the foundations now. A stone is not a new rope. I need sound fittings before I promise anyone a working warning line.

> Player: What do you say before casting a bell?
>
> NPC: We name the landing it must guide people home to. My teacher made me say it clearly, even when no one else was in the workshop. It kept me thinking about the person listening in the rain.

## The Blackvault Mark (`ms_blackvault_mark`)

Recover Ash and confront the accumulated cost of a defense whose spent power was stored where later generations could ignore it.

### The Blackvault Mark

Opening: Blackvault held the residue of old ward workings. Its maintenance books describe stores that should have been discharged. Sareth now blocks access to the Ash stone. Defeat him so that recovery can begin.

Action: Defeat 1 marked Sareth, the Cinder Knife opponents. Follow the quest markers to their encounter.

Observed result: Sareth is defeated. Report to Damar for the Stone of Ash. The other wards in Blackvault have not been cleared by this fight.

Report: Sareth is defeated. Take Ash. It gives spent power a way out of the network; sealing waste away forever only leaves the danger to somebody else. Blackvault still needs work beyond this one chamber.

> Player: What was Blackvault built to contain?
>
> NPC: Spent ward power. The maintenance books call it residue; the old keepers called it ash. Both names let people imagine it had become harmless. The stores still need somewhere safe to discharge.

> Player: Was Vaelthara made to contain it too?
>
> NPC: That is a claim we would need evidence for. Blackvault gives us a place to examine what the network discarded. It does not give me access to Vaelthara's memories.

> Player: Why is Sareth guarding the waste?
>
> NPC: I can tell you Sareth blocks access to the Ash stone. I cannot tell you what bargain put him there. Defeat him, and we can recover the means to release spent power instead of letting it build up.

## A Camp Worth Defending (`ms_camp_defending`)

Defend the home built after the shrine attack and make voluntary cooperation, rather than Vaelthara's obedience, the basis of its survival.

### A Camp Worth Defending

Opening: Morvane's raid is coming for Oathstead. This is the camp that took you in after the shrine; now it is home to people with nowhere else to stand. Use the defense point when you are ready. We have to hold.

Action: Use the marked Oathstead defense point and repel the raid.

Observed result: Oathstead held against this raid. Its people still need working defenses.

Report: We held. You are still here, and so are the people who stood with you. Take Oaths for the Gate expedition. It recognizes a commitment freely made; after a day like this, I understand why that needed a stone of its own.

> Player: Did I bring Morvane here by collecting the stones?
>
> NPC: He is coming to a camp that shelters people and refuses Vaelthara's rule. The stones may give him another reason. Sending you away will not make these people safe.

> Player: You could give me up.
>
> NPC: You came to us needing shelter. If that promise lasts only until sheltering you becomes dangerous, it was never worth much.

> Player: What does Vaelthara mean by mercy?
>
> NPC: I know what you survived at the shrine, and I know a force bearing her cause is coming here. Whatever protection she claims to offer, Morvane's raid is how that claim reaches us.

> Player: If she could keep everyone safe, would you accept?
>
> NPC: I would ask who can leave, who can refuse an order, and what happens to those who do. A full storehouse matters. So does the person holding its key.

> Player: Can we hold without every companion here?
>
> NPC: We defend with the people and defenses we have. No single missing friend makes the rest of us helpless. Use the defense point when you are ready to face the raid.

## The Kingdoms Answer (`ms_kingdoms_answer`)

Gather five explicit mainland commitments for Oathstead and the Gate expedition, while allowing each participant to state the limits of their help.

### Request Mirella's commitment

Opening: Oathstead held. Now you want grain for a longer fight. I can make a commitment, but the barges will still need an escort.

Action: Speak with Mirella and explicitly request support for Oathstead and the Old Gate expedition.

Observed result: Riverside will supply grain for Oathstead's defenders. The barges still need an escort before they can sail.

### Request Odrick's commitment

Opening: Maelis needs a watch that can hold while you are at the Gate. Let us be precise about the detail I can promise.

Action: Speak with Odrick and explicitly request support for Oathstead and the Old Gate expedition.

Observed result: Highwall will send a watch detail to Oathstead. The soldiers have been promised; they have not arrived yet.

### Request Selene's commitment

Opening: If you are going to open the Gate, you need the surviving instructions in your hands. The Archive must stop treating access as a favor.

Action: Speak with Selene and explicitly request support for Oathstead and the Old Gate expedition.

Observed result: The Archive will share its surviving ward instructions. Selene warns that a complete record of the first binding has not survived.

### Request Ysra's commitment

Opening: I can ask our bellkeepers to carry warnings for Oathstead. Before you rely on them, hear where our promise ends.

Action: Speak with Ysra and explicitly request support for Oathstead and the Old Gate expedition.

Observed result: The bellkeepers will relay warnings between the settlements. Ysra cannot promise that every damaged bell will answer.

### Request Solari's commitment

Opening: I will answer for Sanctum's support. I cannot answer for every power beyond the Gate.

Action: Speak with Solari and explicitly request support for Oathstead and the Old Gate expedition.

Observed result: Sanctum will support the attempt to open the Old Gate. Solari gives no assurance that the twelve stones can control what lies beyond it.

Report: Five commitments, each with its limits. Take Dawn, the twelfth stone. We can attempt to open the Gate. The promised help still needs to reach us, and these five voices do not speak for every coast or every power the network touches.

> Player: What are we asking the realms to agree to?
>
> NPC: Help Oathstead hold while we open the Gate. Ask each representative what they can commit, and let them state the limits. I will not turn their help into an oath they cannot leave.

> Player: Isn't one commander stronger?
>
> NPC: Sometimes one order is faster. We have also seen what happens when nobody is allowed to question an order. I need partners who can tell me when my plan will get their people killed.

Speaker: Mirella

> Player: What will your grain cost Oathstead?
>
> NPC: This is support for the defense, not a claim on your settlement. The barges still need an escort. Do not tell Maelis I have delivered what I have only promised.

Speaker: Odrick

> Player: Who commands the watch detail you are promising?
>
> NPC: They remain responsible for their own people. Maelis must agree their duties with their captain. I am offering soldiers for a defense, not giving away their right to question an order.

Speaker: Selene

> Player: Will you share the records that embarrass the Archive?
>
> NPC: The surviving ward instructions, including their gaps. You cannot make a sound decision from a flattering selection. I cannot give you pages that no longer exist.

Speaker: Ysra

> Player: Can you guarantee the warning will reach us?
>
> NPC: I can commit our bellkeepers to relaying it. A broken bell may still leave a gap. Maelis needs to know that before she relies on hearing us.

Speaker: Solari

> Player: Are you blessing whatever comes through the Gate?
>
> NPC: No. I support the attempt to reach Vaelthara, knowing that the stones may open a way we cannot fully control. Tell Maelis that plainly when you carry my answer.

## The Twelve Stones of the Gate (`ms_twelve_stones_gate`)

Open the route to Vaelthara's command site with the twelve stones, carrying the needs of Oathstead and the regions into the confrontation.

### The Twelve Stones of the Gate

Opening: You have all twelve stones. Take them to the Old Gate and open the way to the Hollow Throne. Vaelthara left a survivor at the shrine. She is about to face someone who has learned what her rule would cost.

Action: Inspect 1 marked Old Gate of Alderfall locations. Report only what those inspections establish.

Observed result: You completed the required checks for Old Gate of Alderfall. Report these findings before deciding what follows.

Report: The Gate is open. Beyond it is the power that broke the road shrine. Here, there are people who chose to help you reach it. Go when you are ready; I will keep working for the home you are coming back to.

> Player: What am I going to the Hollow Throne to do?
>
> NPC: Stop Vaelthara from making every protected road answer to her. You began by asking why you survived. Now there are people behind you who need a future beyond waiting for her next army.

> Player: Does defeating her mean the old wards were right?
>
> NPC: No. We can defend the living without pretending every old promise was freely made. Stopping her is the danger in front of us; what we build afterward still needs work.

> Player: What happens to Oathstead while I am gone?
>
> NPC: I stay. The representatives have promised help, and I will plan around what actually arrives. You are allowed to be afraid. You do not have to pretend you are the only person keeping this place alive.

> Player: When I first arrived, you barely knew me.
>
> NPC: You needed a blanket. I had one. We did not need the whole future settled before beginning.

