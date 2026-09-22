package com.alderfall.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Campaign prose and optional conversations. Evidence gates never award quest progress. */
public final class MainStoryContent {
    private MainStoryContent() { }

    public record Topic(String question, String answer, String evidence, List<Topic> replies) { }
    private record Scene(String opening, String purpose, String uncertainty, String report) { }

    public static boolean supports(Quest q) {
        return q != null && q.mainStoryQuest() && scene(q.id) != null;
    }

    public static boolean isSpeaker(Npc npc) {
        return npc != null && npc.questId() != null && npc.questId().startsWith("ms_");
    }

    /** Everyday conversation remains safe before a chapter unlocks and after its completion. */
    public static List<Topic> personal(Npc npc) {
        return switch (npc.questId()) {
            case "ms_wake_ashes" -> List.of(t("Have you eaten today?", "Half a bowl. Someone needed the other half more urgently. I am keeping the spoon, though; we are short of those too.",
                    t("You need to look after yourself as well.", "I know. Bring that argument back when I have sat down and I may even listen.")));
            case "ms_names_dust" -> List.of(t("Why did you become an archivist?", "My father could name every field outside his village. The official map named three. I wanted a place where the smaller names would be kept too.",
                    t("Did you find one?", "I found shelves and people deciding what deserved a shelf. That is work I am still learning to question.")));
            case "ms_watchtower_bells" -> List.of(t("What do you miss when you are on watch?", "The first warm room after a winter patrol. Someone takes your gloves before you can argue and pushes a cup into your hands. Highwall should be that kind of place."));
            case "ms_shrine_shadow" -> List.of(t("What does a guest owe at a well-road shrine?", "A greeting, care with the water, and room for the next traveler. We offer water before asking a person's business. A thirsty stranger should not have to flatter a priest."));
            case "ms_bell_alone" -> List.of(t("Is there a bell you enjoy hearing?", "The ferry coming home on a clear evening. No warning rhythm, no fog. Just someone pulling too hard because they want supper. I never correct that one."));
            case "ms_toll_ledger" -> List.of(t("What would you do with a quiet day?", "Walk across a bridge without somebody handing me an account. Buy a pear. Eat it before the next person finds me. I have modest ambitions and persistent clerks."));
            case "ms_orchard_ward" -> List.of(t("What is the first apple of the season like?", "Usually sour. Every year a child insists it looks ready, and every year I let them discover it. Some lessons are best kept small."));
            case "ms_names_cold_stone" -> List.of(t("Who tends the graves of people with no family?", "I do, when I can. A neighbor often remembers more than they think: a song, a favorite food, the way someone knocked at a door. A name is where we begin."));
            case "ms_cold_road" -> List.of(t("What do people do through a Snowrest winter?", "Mend things, argue over stove space, teach children the road songs. In a whiteout, a familiar voice can bring someone the last few steps home."));
            case "ms_missing_bell_rope" -> List.of(t("What is the best part of making a bell?", "Hearing it from the other bank for the first time. You spend days with your head next to the metal, then remember it was made for someone far away."));
            case "ms_blackvault_mark" -> List.of(t("Do you ever stop working?", "I mend cups. Clay has the courtesy to show most of its cracks. My shelf is full of ugly cups that hold water, and I am fond of them."));
            default -> List.of();
        };
    }

    public static void refine(Map<String, Quest> quests) {
        refineInvestigations(quests);
        for (Quest q : List.copyOf(quests.values())) {
            if (!supports(q)) continue;
            Scene scene = scene(q.id);
            StoryLocationCatalog.Place place = StoryLocationCatalog.forQuest(q.id);
            boolean fixedSite = place != null && place.outdoorSite();
            List<Quest.QuestStage> stages = new ArrayList<>();
            for (Quest.QuestStage s : q.stages) {
                String opening = opening(q, s, scene) + (place == null ? "" : " " + place.route());
                String finding = finding(s);
                String report = stages.size() == q.stages.size() - 1 ? scene.report() : finding;
                String target = s.id().equals("ms_stolen_index_page") ? "Stolen Vault Instructions" : s.target();
                stages.add(new Quest.QuestStage(s.id(), s.title(), target, s.needed(), s.objectiveKind(),
                        s.objectiveMapId(), fixedSite ? "place:" + place.id() : s.objectiveLocationKind(),
                        fixedSite ? 0 : s.objectiveLocationIndex(), s.objectiveAsset(),
                        s.monsterKey(), s.targetNpcId(), s.branchOutcomeKey(), opening,
                        fixedSite ? s.progressDialog() + " " + place.route() : s.progressDialog(), finding, report));
            }
            Quest.QuestStage first = stages.get(0);
            String title = q.id.equals("ms_frosthollow_standard") ? "The Stolen Watch Oath"
                    : q.id.equals("ms_stolen_index") ? "The Missing Vault Instructions" : q.title;
            Quest rewritten = new Quest(q.id, title, scene.purpose(), first.target(), first.needed(), q.rewardGold, q.rewardXp,
                    first.objectiveKind(), first.objectiveMapId(), first.objectiveLocationKind(), first.objectiveLocationIndex(),
                    first.objectiveAsset(), first.monsterKey(), first.startDialog(), first.progressDialog(), first.readyDialog(),
                    stages.get(stages.size() - 1).completeDialog(), q.type, q.chainOwnerId, q.nextQuestId,
                    first.targetNpcId(), first.branchOutcomeKey(), stages);
            // Preserve the revision assigned by the objective pass (or the existing compatible revision).
            rewritten.contentRevision = fixedSite ? 4 : q.contentRevision;
            quests.put(q.id, rewritten);
        }
    }

    private static void refineInvestigations(Map<String, Quest> quests) {
        Quest north = quests.get("ms_watchtower_bells");
        Quest.QuestStage n = north.stages.get(0);
        revise(quests, north, List.of(
                inspect(n, "north_bell_clapper", "Inspect the split signal bell", "Split Signal Bell",
                        "Inspect the marked signal bell on the northern watch route.",
                        "A strip of banner cloth jams the bell's clapper. Its stitching reads 'Fenrik household: pass watch'. The obstruction prevented this bell from warning the supply carts."),
                inspect(n, "north_watch_roster", "Read the cairn watch names", "Cairn Watch Names",
                        "Read the marked watch inscription beside the northern cairns.",
                        "The burial stone names the Fenrik household, matching the cloth in the bell. Its oath reads: 'Guard the supply road until the spring thaw.' These dead watchmen were promised an end to their duty."),
                inspect(n, "north_stolen_signal", "Compare the raiders' signal", "Copied Watch Signal",
                        "Inspect the marked signal scratched beside the watch route, then report to Odrick.",
                        "The roadside post shows the watch's all-clear pattern: two short bell strokes. A newer scratch copies that pattern beside a raider mark. Someone learned how to signal a safe road while the warning bell was jammed.")));
        Quest south = quests.get("ms_shrine_shadow");
        Quest.QuestStage s = south.stages.get(0);
        revise(quests, south, List.of(
                inspect(s, "south_guest_cup", "Inspect the guest cup", "Cracked Guest Cup",
                        "Inspect the marked guest cup at the damaged southern shrine.",
                        "The cup beside the altar bears a greeting to a fire guest. Its rim is scorched on the inside, beneath a later iron collar."),
                inspect(s, "south_welcome_words", "Read the welcome beneath the collar", "Old Welcome Inscription",
                        "Read the marked welcome inscription at the southern shrine.",
                        "The old welcome says the guest gives warmth until moonset, then may depart. A newer command cut across it demands warmth until the keeper releases the vessel."),
                inspect(s, "south_closed_outlet", "Examine the sealed outlet", "Sealed Vessel Outlet",
                        "Inspect the marked outlet of the southern fire vessel, then report to Solari.",
                        "The outlet named in the welcome has been plugged with iron. Soot has collected behind the plug. This vessel's departure route was physically closed; the survey does not identify who ordered it.")));
        Quest standard = quests.get("ms_frosthollow_standard");
        Quest.QuestStage battle = standard.stages.get(0);
        revise(quests, standard, List.of(new Quest.QuestStage("north_banner_battle", battle.title(), battle.target(), battle.needed(),
                battle.objectiveKind(), battle.objectiveMapId(), battle.objectiveLocationKind(), battle.objectiveLocationIndex(),
                battle.objectiveAsset(), battle.monsterKey(), battle.targetNpcId(), battle.branchOutcomeKey(),
                battle.startDialog(), battle.progressDialog(),
                "Kharvok is defeated. His fallen standard can now be examined; the names sewn into it still need to be read.",
                "Kharvok is defeated. Examine the fallen standard before reporting to Odrick."),
                inspect(battle, "north_fallen_standard", "Read the fallen standard", "Kharvok's Fallen Standard",
                        "Examine the marked fallen standard at Kharvok's encounter site, then return to Odrick.",
                        "Kharvok's standard names the Fenrik household, the dead roadwatchmen named on the Highwall burial stone. New thread covers 'until the spring thaw' with 'until Kharvok grants release'. The commander changed their seasonal duty into service only he could end.")));
        Quest ember = quests.get("ms_ember_socket_rite");
        Quest.QuestStage forge = ember.stages.get(0);
        revise(quests, ember, List.of(
                inspect(forge, "ember_cradle", "Inspect Ember's transfer cradle", "Ember Transfer Cradle",
                        "Inspect the marked transfer cradle at the forge shrine.",
                        "Ember's cradle is linked to a guest vessel by an iron collar. The directions say to lift the stone clear before opening the guest's outlet; otherwise the cradle keeps drawing heat."),
                inspect(forge, "ember_disconnect", "Lift Ember clear of the cradle", "Ember Cradle Release",
                        "Use the marked cradle release to lift Ember clear of the transfer channel.",
                        "You lift Ember clear of its transfer channel. The channel stops glowing, but a small flame still presses against the vessel's closed outlet. The guest has not yet been released."),
                inspect(forge, "ember_open_outlet", "Open the guest's outlet", "Guest Vessel Outlet",
                        "Open the marked outlet of the disconnected guest vessel, then report to Solari.",
                        "You unfasten the outlet. A thin flame rises through it, pauses above the guest cup, and vanishes into the daylight. The vessel is empty and cool. This guest has left; the other shrines still need attention.")));
    }

    private static Quest.QuestStage inspect(Quest.QuestStage source, String id, String title, String target,
                                             String instruction, String finding) {
        // Separate stages use the existing regional encounter's reachable objective anchors.
        // Location index names the site, not an offset into unrelated camps.
        return new Quest.QuestStage(id, title, target, 1, Quest.ObjectiveKind.SEARCH,
                source.objectiveMapId(), source.objectiveLocationKind(), source.objectiveLocationIndex(),
                "quest_ward_marker", "", "", "", instruction, instruction, finding, finding);
    }

    public static int objectiveVariant(Quest q, int variant) {
        return q.mainStoryQuest() && q.contentRevision >= 3 ? variant + q.stageIndex : variant;
    }

    private static void revise(Map<String, Quest> quests, Quest q, List<Quest.QuestStage> stages) {
        Quest.QuestStage first = stages.get(0);
        Quest rewritten = new Quest(q.id, q.title, q.description, first.target(), first.needed(), q.rewardGold, q.rewardXp,
                first.objectiveKind(), first.objectiveMapId(), first.objectiveLocationKind(), first.objectiveLocationIndex(),
                first.objectiveAsset(), first.monsterKey(), first.startDialog(), first.progressDialog(), first.readyDialog(),
                stages.get(stages.size() - 1).completeDialog(), q.type, q.chainOwnerId, q.nextQuestId,
                first.targetNpcId(), first.branchOutcomeKey(), stages);
        rewritten.contentRevision = 3;
        quests.put(q.id, rewritten);
    }

    public static String purpose(Quest q) { return supports(q) ? scene(q.id).purpose() : ""; }
    public static String uncertainty(Quest q) {
        if (q.observedStages.contains("south_closed_outlet"))
            return "This vessel was confined: the changed welcome and the iron plug establish that. We still need to learn who ordered the change and how widely it was used.";
        if (q.observedStages.contains("north_fallen_standard"))
            return "Kharvok's stitching changed the watch oath's ending. The cairn keepers still need to establish which disturbed names can now be released; the battle and the inspection did not perform their burial work.";
        if (q.observedStages.contains("ember_open_outlet"))
            return "This guest left after you disconnected Ember and opened the outlet. Other vessels may still be confined, and this cold vessel no longer supplies the road wards. Repair and replacement are still work to do.";
        if (q.observedStages.contains("north_stolen_signal"))
            return "Someone obstructed the bell and copied the safe-passage signal. We have not established who ordered that interference or how Kharvok uses the names on his standard.";
        return supports(q) ? scene(q.id).uncertainty() : "";
    }

    public static String subject(Quest q) {
        if (q.completed) return completedReport(q);
        if (q.ready()) return "Tell me what happened. " + QuestNarrative.clean(q.activeReadyDialog());
        return QuestNarrative.clean(q.activeStartDialog());
    }

    public static String completedReport(Quest q) {
        if (q.contentRevision >= 3 && !q.observedStages.contains(q.stages.get(q.stages.size() - 1).id())) {
            return "Your earlier completion of " + q.title + " remains recorded. We have no detailed findings from the revised investigation to review.";
        }
        return QuestNarrative.clean(q.activeCompleteDialog());
    }

    /** Coalition testimony must be spoken by its contact, not by Maelis or the next representative. */
    public static String subject(Quest q, Npc npc) {
        if (!"ms_kingdoms_answer".equals(q.id)) return subject(q);
        if ("Maelis".equals(npc.name())) return q.ready() || q.completed
                ? "You have five answers. Tell me what each person actually agreed to; I need to plan for the help we can expect."
                : scene(q.id).opening();
        if (q.activeTargetNpcId().equals(npc.name()) && !q.ready()) return q.activeStartDialog();
        return "You have my answer for Oathstead. Take it to Maelis with the other commitments.";
    }

    public static List<Topic> topics(Quest q, Npc npc) {
        if (!supports(q)) return List.of();
        if ("ms_kingdoms_answer".equals(q.id)) return coalitionTopics(q, npc);
        List<Topic> result = new ArrayList<>(switch (q.id) {
            case "ms_wake_ashes" -> List.of(
                    t("Vaelthara let me live. Why?", "You remember her leaving you. I believe you. I cannot tell you why she did it. She may want us frightened enough to obey her.",
                            t("Then bringing me here puts you in danger.", "The shrine stood on our road. Whatever broke it was already at our door. You brought us a warning; you did not bring us this war.")),
                    t("That shrine was supposed to protect travelers.", "My mother left a heel of bread there before every winter journey. I thought she was feeding birds. Now I wish I had asked what the keeper did with the rest.",
                            t("Will bread stop Vaelthara?", "No. But someone once knew how to keep that place working. Find what survived the fire and we may learn something useful.")),
                    evidence("shrine_damage", "The worst damage is around the socket.", "Then start there. A burned roof tells us there was fire. A broken ward socket may tell Selene why the protection failed.",
                            t("Does that mean she used the ward against us?", "It means we should ask. I watched you stagger into camp; I did not watch her break the shrine. Keep those two things separate.")));
            case "ms_road_dust" -> List.of(
                    t("I want to go after Vaelthara.", "So do I. But you met her once and barely reached us. Bring Selene something she can examine before you face that power again.",
                            t("And while I am chasing answers?", "I will keep the camp organized. We still need food, shelter, and a road people can use. That work gives you somewhere to come back to.")),
                    evidence("shrine_fragment", "I have the carved fragment. Is it enough?", "Enough to put a real question before the Archive. Collect the ash too; Selene may be able to compare the burn with older breaches. I cannot read either one."));
            case "ms_oathstead_stand" -> List.of(
                    t("Why wolves, when Vaelthara is out there?", "Because the people cutting timber have to reach the trees alive. A grand plan will not keep a wolf off a hungry worker.",
                            t("Are you asking me to stay?", "Long enough to clear this danger. Then take the shrine evidence to Selene. I need both a usable road and an answer about our wards.")),
                    t("What kind of place are we building?", "One where a person can ask for shelter without first proving useful. Once they have eaten, we can ask what work they can manage. I would like that much to survive the winter."));
            case "ms_names_dust" -> List.of(
                    t("Can Oathstead survive the attack I saw?", "I cannot promise it. Your fragment resembles our ward diagrams. Read the maintenance record with me; resemblance is not enough to send you home reassured.",
                            t("Can you at least repair the stone?", "The masonry, yes. But if the protection failed because of how it was commanded, a fresh block would leave the same weakness.")),
                    evidence("archive_record", "Oathstead is listed under the same ward pattern.", "Then your fear has a basis. The camp and shrine belong to the same design. We need the builders' instructions before we decide how to defend it.",
                            t("You are the archivist. Why don't you have them?", "I have what the Archive kept. That is an answer I used to give with pride. Look at the builders' index; we need to establish what is missing.")),
                    evidence("archive_index", "What exactly did the bandits take?", "The leaf titled 'Old Oath Vault: Keeper's Instructions'. It describes the seal protecting the Stone of Memory beneath Archive City. The ransom demand tucked into this index names Crowhook Bandit Camp as the place to pay.",
                            t("Why do we need those instructions?", "We need to examine the Memory pedestal without damaging its protective seal. Guessing at the carvings could destroy the very record that might explain the shrine attack.")));
            case "ms_stolen_index" -> List.of(
                    t("Which stolen document am I looking for?", "A single leaf headed 'Old Oath Vault: Keeper's Instructions'. It explains the carvings around the Stone of Memory's pedestal beneath Archive City. Look for that heading in the bandits' document cache; I need the instructions intact."),
                    t("Why are you sending me to Crowhook Bandit Camp?", "The ransom demand left in the builders' index names Crowhook as the place to pay for the stolen Archive papers. Crowhook is a bandit camp on Belltower's southern approach. I have marked the camp and its document cache on your map.",
                            t("Why would bandits steal instructions for an old vault?", "They stole Archive papers and want us to buy them back. The missing keeper's instructions were among those papers. We need that particular document because the Stone of Memory may help us understand the ward Vaelthara broke.")),
                    evidence("ms_stolen_index_guards", "The three cache guards are dead. What should I search?", "Search the marked Stolen Vault Instructions cache at Crowhook Bandit Camp. Find the leaf headed 'Old Oath Vault: Keeper's Instructions', then bring it back to me in Archive City."),
                    evidence("ms_stolen_index_page", "What does the Stone of Memory remember?", "The instructions describe a stone that preserves the names of people serving the protective wards and the promises they made. If those promises were changed, Memory may preserve enough for us to compare the versions. First we must inspect its pedestal beneath Archive City."));
            case "ms_first_socket" -> List.of(
                    t("Will this stone tell us how to defeat Vaelthara?", "It may tell us how the network recognized its participants. That could explain how she entered a protected shrine. I will not promise a weapon before we understand what we have.",
                            t("What am I looking for in the vault?", "First, a seal matching the recovered page. Then the pedestal marked Memory. If the page and the chamber disagree, the disagreement matters.")),
                    evidence("vault_seal", "Why does the vault show twelve sockets?", "The page names Memory; the mural places it beside eleven other functions. We have been maintaining pieces of a larger defense. Reaching its command site will take more than this one stone.",
                            t("Were all twelve stones weapons?", "The old vocabulary includes boundaries, crossings, warnings, and the release of spent power. People needed to live behind the defense. The stones served that life too.")),
                    evidence("vault_memory", "The pedestal remembers names. Can it judge who was right?", "No inscription can do that for us. Memory can preserve a promise and who made it. Whether someone was forced to make that promise is a question we must still ask.",
                            t("Could the old defenders have done that?", "People defending their homes can still harm others. I will not accuse particular builders without evidence. I will not assume their victory excuses everything either.")));
            case "ms_watchtower_bells" -> List.of(
                    t("Who used the bell at Highwall Cairn Watch?", "My roadwatch soldiers. Two short strokes meant the supply road was open; three slow strokes warned cart drivers to stop. The bell stands beside the burial mounds of watchmen who died defending that road.",
                            t("Why put the warning bell beside those graves?", "The watchmen's families believe the dead can carry a warning when fog hides the living patrols. The families who tend those stone mounds are called cairn keepers. They maintain the graves and recite the dead watchmen's names; they are not another military order.")),
                    t("What exactly should I inspect at Highwall Cairn Watch?", "Start with the Split Signal Bell. Then read the Cairn Watch Names cut into the burial stone beside it. Finally, examine the Copied Watch Signal scratched beside the road. Each object has its own red quest marker.",
                            t("How does that help Oathstead?", "Highwall's carts carry food toward the central settlements, including your camp. If a raider can imitate our all-clear signal while the warning bell is silent, those carts can be led straight into an ambush.")),
                    evidence("north_watch_roster", "The watch oath was supposed to end at spring thaw.", "Then the ending mattered to the people who swore it. The cloth in the bell carries one of their names. I want to know who is using that name now.",
                            t("Does that prove Kharvok commands the dead?", "It proves someone obstructed our bell with named banner cloth. We need his actual standard before we can say what he changed.")));
            case "ms_raiders_pass" -> List.of(
                    t("Are these raiders serving Vaelthara?", "I can place the brutes on the pass. I cannot place Vaelthara in their camp. Kharvok benefits while our road is closed; that gives us an enemy we can reach.",
                            t("Will you reopen the road when they fall?", "I will need patrols first. You can break this force. I must answer for the families I send behind you.")),
                    t("What is wrong with Kharvok's banners?", "Northern standards carry the names of the households that raised them. Kharvok stitches surrendered flags together. The keepers believe he is using those names to command service. I know he has a force behind him."));
            case "ms_frosthollow_standard" -> List.of(
                    t("Why should the dead obey Kharvok?", "The cairn keepers say a watch oath binds a name to a boundary. They fear his sewn banners have turned that duty into obedience to him. Their reading may explain the dead on the pass; it does not make him their rightful commander.",
                            t("Did Highwall swear that kind of oath?", "Our watch still names the households it protects. If those words can be twisted, I owe the living an answer as much as I owe the dead.")),
                    t("What will the Stone of Iron do for Oathstead?", "The keepers describe it as strength lent to a boundary. It cannot choose whom a wall should shelter. Defeat Kharvok, examine his standard, and return to me for the stone."),
                    evidence("north_fallen_standard", "He covered the ending of their oath with his own name.", "Then we have more than a keeper's fear. His stitching made release depend on him. We can show the households exactly what was changed.",
                            t("Are the dead free now?", "We have stopped him and read the standard. I still need the cairn keepers to attend to the disturbed oaths. I will not announce peace at their graves before that work is done.")));
            case "ms_shrine_shadow" -> List.of(
                    t("What do you mean by a guest inside the fire vessel?", "A fire spirit. Shrine keepers invite one into a heatproof cup and ask it to warm travelers and power the shrine's protective magic. We call the spirit a guest because it is supposed to be free to leave. Our temple worships the sun; the spirit in the cup is not our god.",
                            t("Then why would a guest burn its keepers?", "It might be injured, trapped, or no longer the presence the keepers welcomed. I will not name its anger wicked before we examine the shrines.")),
                    t("Could this be the same failure I saw?", "Possibly. Your shrine lost its protection; ours have become dangerous to approach. Examine the guest cup, its welcome, and its outlet. We need to know how this vessel was meant to work."),
                    evidence("south_welcome_words", "Someone changed 'until moonset' to 'until released'.", "Then someone changed the terms of the welcome. That is written into this vessel. We still need to examine whether the guest had a way out.",
                            t("Does your temple teach that command?", "I was taught that the fire is a guest. If our keepers used this altered command, they betrayed the rite I learned. I need to find who used it and when.")),
                    evidence("south_closed_outlet", "The guest's way out was plugged with iron.", "Then this was confinement, whatever name the keeper gave it. We have the changed words and the blocked outlet. At the forge, we must disconnect the draw before opening the vessel."));
            case "ms_caravan_glass" -> List.of(
                    t("Are the ember imps the shrine's guests?", "No keeper has identified them as such. They are dangerous creatures on a route we need. Driving them off will let us reach the forge; it will not explain every burned vessel.",
                            t("Why does the caravan carry glass?", "A sealed lamp carries a guest flame between wells. Our glassmakers leave room for the heat to breathe. Cheap vessels crack; an impatient keeper can lose a caravan's protection in one night.")),
                    t("Will clearing the road finish the rite?", "It makes recovery possible. The caravan stores still need collecting, and the forge must be examined. I will not ask you to pretend a battle delivered our oil."));
            case "ms_ember_socket_rite" -> List.of(
                    t("What connects the fire spirit to the Stone of Ember?", "The forge behind Sunken Guest Shrine's altar contains Ember's transfer cradle. An iron channel connects it to the same guest cup you examined. While seated, Ember draws that spirit's heat into the road wards. Lift the stone out before opening the vessel's outlet, or the cradle will keep pulling on the spirit.",
                            t("What if the guest cannot refuse?", "We found the changed welcome and the plugged outlet. That vessel was a prison. I will not call its heat a gift. Disconnect the draw before opening the outlet; afterward we must find willing sources.")),
                    t("Can Ember replace the broken shrine ward?", "It transfers power within the network. Power alone will not repair its instructions. Carry it with Memory; we need to understand both what the ward is told and what feeds it."),
                    evidence("ember_cradle", "Why lift the stone before opening the outlet?", "The cradle's own directions say it continues drawing while Ember is seated. Disconnect that draw first. Opening an exit means little if something still holds the guest inside."),
                    evidence("ember_disconnect", "The channel is dark. Have I freed the guest?", "You have stopped this cradle taking heat. The flame is still inside. Open the outlet; then it can leave."),
                    evidence("ember_open_outlet", "The flame left. Have we weakened the road wards?", "That vessel no longer supplies them. We need willing sources and repaired shrines to replace what was taken. I authorized the release; I will answer for the repair work too.",
                            t("And the stone still works?", "It is a means of transfer, not the fire itself. Take Ember for the Gate. We must find power that can be offered without trapping its source.")));
            case "ms_bell_alone" -> List.of(
                    t("Why listen to a bell no one rang?", "Because a boatman may turn toward it. In fog, a false landing bell can kill a whole ferry. Check the three rope sites before we decide whether we are hearing a warning or a lure.",
                            t("Who rings from beneath the water?", "Ferry families tell stories of water spirits listening from the drowned riverbed. They call those spirits Deep Listeners. Some families leave a little bread by the landing bell and ask them to guide lost boats. That belief does not tell us who is ringing the unattended bell at Reedbank.")),
                    t("What does this mean for Oathstead?", "Your camp needs travelers to reach it alive. Our bells connect the waterways to the roads. A ward behind a palisade will not save a family led into deep water on the way there."));
            case "ms_medicine_mireford" -> List.of(
                    t("Shouldn't we be hunting the thing below the marsh?", "We will. First, gather six fever-reed samples. Mireford has sick people now, and the remedy cannot wait for us to settle the marsh's history.",
                            t("Did the bells cause their fever?", "I do not know. A frightening sound and an illness arriving together are a reason to investigate, not a diagnosis.")),
                    t("What do the reeds mean in the village shrines?", "A fresh bundle marks a household that will shelter a stranded traveler. When the water rises, you can see it above the door. I would like Mireford to be able to keep offering that welcome."));
            case "ms_miredepth_below" -> List.of(
                    t("Is Velmora one of the people the bells drowned?", "The keepers call her Bell-Drowned. Their accounts place her below Miredepth with bells that call the dead. They do not tell me whether she was once a ferryman, a captive, or something older.",
                            t("Then why kill her?", "Because she is attacking the living and calling more dead to the surface. I can admit what I do not know without sending another boat into her reach.")),
                    t("Why is the Stone of Tides here?", "These marshes cover old crossings. The keepers associate Tides with opening and closing a route through changing water. Recover it here; that does not give us command of every river or sea."));
            case "ms_toll_ledger" -> List.of(
                    t("How can grain disappear at a guarded bridge?", "The toll keepers count what is declared. Something can cross under a false name, or leave by a road our guards cannot follow. Read the three records before I accuse either a keeper or a court.",
                            t("A court? Whose court?", "The Briar Courts are supernatural households said to rule the forest paths west of our river towns. Travelers describe antlered riders and roads that appear only at dusk. Riverside governs the human bridges. A toll paid to my clerk may mean nothing to a rider on one of those paths.")),
                    t("Oathstead cannot eat an explanation.", "No. That is why I need the route cleared as well as the loss understood. Otherwise the next grain barge follows the first into somebody else's store."));
            case "ms_redcap_trade" -> List.of(
                    t("Are these raiders part of a Briar hunt?", "Redcaps trade stolen goods along its edges. That makes the connection worth pursuing. It does not make every goblin a sworn servant of a court.",
                            t("So whose orders are we stopping?", "We are stopping eight raiders on the supply route. If we find who pays them, I will happily add a name. I will not invent one to make the fight sound grander.")),
                    t("What happens to the grain after the fight?", "It still has to be recovered and moved. Clearing the route gives Riverside a chance to do that. I cannot put bread in Oathstead's ovens by declaring the road safe."));
            case "ms_glowing_mud" -> List.of(
                    t("Why call a stone Hunger?", "Old river accounts describe a stone that swallowed excess force when wards were struck. That sounds useful until someone teaches it to draw from a harvest instead.",
                            t("Is that what happened to our grain?", "It is a possibility, not an excuse to forget ordinary theft. We have a marked scavenger to stop and a stone to recover. Selene will have more to work with once it is out of the mud.")),
                    t("Could we use it against Vaelthara?", "Perhaps it can absorb something she sends at us. First learn what it takes and where that power goes. I would rather owe you a barge of grain than discover we fed the stone another village's winter."));
            case "ms_orchard_ward" -> List.of(
                    t("Why would the orchard's guardian attack its keepers?", "We leave the first fallen apple for the stag. We keep an opening in the hedge. Those are the customs I learned. Something has hurt Rootmaw badly enough that it now attacks anyone approaching; I cannot tell you which protection failed.",
                            t("Do you want me to kill your guardian?", "I want the people tending this orchard to live. Rootmaw is attacking them. Stop it, and I will take responsibility for tending what remains.")),
                    t("What does Roots have to do with a stone gate?", "Our ward reaches from tree to tree. The old name for the stone describes protection shared through living ground. If you rebuild the network, remember that its roads run through places people eat from."));
            case "ms_names_cold_stone" -> List.of(
                    t("How does scratching out a name wake the dead?", "Our burial rite names the person and ends the duties they held in life. The Graves stone served that boundary. I believe the damaged names are leaving the Warden with a duty it cannot finish.",
                            t("So the dead are still being ordered to serve?", "That is my reading, not a confession from the builders. Stop the Warden so we can approach the graves again. Restoring the names is work that comes after.")),
                    t("Why should this matter to someone still alive?", "Because a promise to protect a road should not outlast the person who made it. If you reach the network's command, remember that some of its servants may have been waiting centuries to be allowed to die."));
            case "ms_cold_road" -> List.of(
                    t("Is the Broodmother one of Vaelthara's beasts?", "I have no evidence of that. Hailbacks lived above the road before this war. This one is blocking our winter route, and Snowrest needs that route open.",
                            t("Will killing it end this winter?", "No. It removes the creature keeping us from the pass. We still need supplies, patrols, and whatever work the frost ward requires.")),
                    t("Why keep a stone that preserves the cold?", "The old keepers used Frost to slow a failing ward until help arrived. In these mountains, a little time can save a settlement. A delay that never ends can bury one."));
            case "ms_missing_bell_rope" -> List.of(
                    t("Can the bells carry a warning all the way to Oathstead?", "That is what this network was built for. The Bells stone coordinates the signals, but it still needs working bells. Inspect both old foundations so I know what the repair can use.",
                            t("Can I ring it now?", "You can inspect the foundations now. A stone is not a new rope. I need sound fittings before I promise anyone a working warning line.")),
                    t("What do you say before casting a bell?", "We name the landing it must guide people home to. My teacher made me say it clearly, even when no one else was in the workshop. It kept me thinking about the person listening in the rain."));
            case "ms_blackvault_mark" -> List.of(
                    t("What was Blackvault built to contain?", "The magical force left after the old protective wards stopped an attack. The keepers sent that force into storage chambers at Blackvault, west of Redcairn. Their maintenance books say the Stone of Ash was used to drain the chambers safely.",
                            t("Why has that become dangerous?", "The books list chambers that were filled but never emptied. I cannot tell you how much power remains in them. Sareth blocks access to the Ash stone, so recovering it is the first task.")),
                    t("Why is Sareth guarding the waste?", "I can tell you Sareth blocks access to the Ash stone. I cannot tell you what bargain put him there. Defeat him, and we can recover the means to release spent power instead of letting it build up."));
            case "ms_camp_defending" -> List.of(
                    t("Did I bring Morvane here by collecting the stones?", "He is coming to a camp that shelters people and refuses Vaelthara's rule. The stones may give him another reason. Sending you away will not make these people safe.",
                            t("You could give me up.", "You came to us needing shelter. If that promise lasts only until sheltering you becomes dangerous, it was never worth much.")),
                    t("What does Vaelthara mean by mercy?", "I know what you survived at the shrine, and I know a force bearing her cause is coming here. Whatever protection she claims to offer, Morvane's raid is how that claim reaches us.",
                            t("If she could keep everyone safe, would you accept?", "I would ask who can leave, who can refuse an order, and what happens to those who do. A full storehouse matters. So does the person holding its key.")),
                    t("Can we hold without every companion here?", "We defend with the people and defenses we have. No single missing friend makes the rest of us helpless. Use the defense point when you are ready to face the raid."));
            case "ms_twelve_stones_gate" -> List.of(
                    t("What am I going to the Hollow Throne to do?", "Stop Vaelthara from making every protected road answer to her. You began by asking why you survived. Now there are people behind you who need a future beyond waiting for her next army.",
                            t("Does defeating her mean the old wards were right?", "No. We can defend the living without pretending every old promise was freely made. Stopping her is the danger in front of us; what we build afterward still needs work.")),
                    t("What happens to Oathstead while I am gone?", "I stay. The representatives have promised help, and I will plan around what actually arrives. You are allowed to be afraid. You do not have to pretend you are the only person keeping this place alive.",
                            t("When I first arrived, you barely knew me.", "You needed a blanket. I had one. We did not need the whole future settled before beginning.")));
            default -> List.of();
        });
        StoryLocationCatalog.Place place = StoryLocationCatalog.forQuest(q.id);
        if (place != null) result.add(t("Where is " + place.name() + ", and what am I looking for?",
                place.route() + " Look for: " + place.landmark()));
        result.add(t("What is a ward?", "A protective enchantment fixed to a place or an object. A shrine might keep hostile spirits off a road; a burial ward might keep the dead at rest. Someone must maintain the carving, vessel, bell, or other object carrying the enchantment."));
        return List.copyOf(result);
    }

    public static boolean available(Quest q, Topic topic) {
        return topic.evidence().isEmpty() || q.observedStages.contains(topic.evidence());
    }

    private static Topic t(String question, String answer, Topic... replies) {
        return new Topic(question, answer, "", List.of(replies));
    }

    private static Topic evidence(String stage, String question, String answer, Topic... replies) {
        return new Topic(question, answer, stage, List.of(replies));
    }

    private static List<Topic> coalitionTopics(Quest q, Npc npc) {
        if ("Maelis".equals(npc.name())) return List.of(t("What are we asking the realms to agree to?",
                "Help Oathstead hold while we open the Gate. Ask each representative what they can commit, and let them state the limits. I will not turn their help into an oath they cannot leave.",
                t("Isn't one commander stronger?", "Sometimes one order is faster. We have also seen what happens when nobody is allowed to question an order. I need partners who can tell me when my plan will get their people killed.")));
        return switch (npc.name()) {
            case "Mirella" -> List.of(t("What will your grain cost Oathstead?", "This is support for the defense, not a claim on your settlement. The barges still need an escort. Do not tell Maelis I have delivered what I have only promised."));
            case "Odrick" -> List.of(t("Who commands the watch detail you are promising?", "They remain responsible for their own people. Maelis must agree their duties with their captain. I am offering soldiers for a defense, not giving away their right to question an order."));
            case "Selene" -> List.of(t("Will you share the records that embarrass the Archive?", "The surviving ward instructions, including their gaps. You cannot make a sound decision from a flattering selection. I cannot give you pages that no longer exist."));
            case "Ysra" -> List.of(t("Can you guarantee the warning will reach us?", "I can commit our bellkeepers to relaying it. A broken bell may still leave a gap. Maelis needs to know that before she relies on hearing us."));
            case "Solari" -> List.of(t("Are you blessing whatever comes through the Gate?", "No. I support the attempt to reach Vaelthara, knowing that the stones may open a way we cannot fully control. Tell Maelis that plainly when you carry my answer."));
            default -> List.of();
        };
    }

    private static String opening(Quest q, Quest.QuestStage s, Scene scene) {
        return switch (s.id()) {
            case "shrine_socket" -> "The socket took the worst of it. Look inside the break; a mark beneath the soot may survive even where the outer carving is gone.";
            case "shrine_ash" -> "The fragment is secured. Collect ash from beside its socket as well; Selene needs to compare the material, not just hear my description.";
            case "archive_index" -> "There it is: Oathstead, under the same ward pattern. Now check the builders' index. We need the instructions for that pattern, not another reassurance from me.";
            case "ms_stolen_index_page" -> "You have defeated Crowhook's three cache guards. Search the Stolen Vault Instructions marker for the leaf headed 'Old Oath Vault: Keeper's Instructions'. Bring that document to Selene in Archive City.";
            case "vault_memory" -> "The entry seal matches. Now examine the Memory pedestal. Its instructions should tell us what the stone can be asked to preserve.";
            case "kingdom_answer_mirella" -> "Oathstead held. Now you want grain for a longer fight. I can make a commitment, but the barges will still need an escort.";
            case "kingdom_answer_odrick" -> "Maelis needs a watch that can hold while you are at the Gate. Let us be precise about the detail I can promise.";
            case "kingdom_answer_selene" -> "If you are going to open the Gate, you need the surviving instructions in your hands. The Archive must stop treating access as a favor.";
            case "kingdom_answer_ysra" -> "I can ask our bellkeepers to carry warnings for Oathstead. Before you rely on them, hear where our promise ends.";
            case "kingdom_answer_solari" -> "I will answer for Sanctum's support. I cannot answer for every power beyond the Gate.";
            default -> s.id().equals(q.stages.get(0).id()) ? scene.opening() : s.startDialog();
        };
    }

    private static String finding(Quest.QuestStage s) {
        return switch (s.id()) {
            case "archive_index" -> "The builders' index lists northern and southern ward instructions. The leaf titled 'Old Oath Vault: Keeper's Instructions' is missing. A ransom demand tucked into the index says to pay for the stolen Archive papers at Crowhook Bandit Camp.";
            case "ms_stolen_index_page" -> "You recovered the leaf headed 'Old Oath Vault: Keeper's Instructions'. It describes the entry seal and Memory pedestal beneath Archive City, including how to release the Stone of Memory safely.";
            case "vault_memory" -> "The Memory pedestal responds to the recovered instructions. Its inscription describes preserving the names and terms of ward service. Report to Selene to complete recovery of the stone.";
            default -> s.readyDialog();
        };
    }

    private static Scene scene(String id) {
        return switch (id) {
            case "ms_wake_ashes" -> new Scene(
                    "Sit down a moment. You reached us alive; we can start there. When you can manage the walk, show me where the road shrine broke. The families here are trusting another ward to keep them safe.",
                    "You survived the shrine breach. Oathstead needs to learn what failed before its people put their lives behind the same kind of protection.",
                    "You saw Vaelthara at the shrine. We still do not know how she broke its protection or why she left you alive.",
                    "The socket was the center of the damage, and part of its carving survived. Recover that fragment. Selene in Archive City may be able to tell us whether our camp has the same weakness.");
            case "ms_road_dust" -> new Scene(
                    "There is a loose fragment in the shrine's broken socket. Bring it out with its carving intact, then collect ash from beside it. Selene needs something she can examine.",
                    "Carry physical evidence of the attack toward an answer about Oathstead's defenses, instead of facing Vaelthara again with no more knowledge than before.",
                    "I can recognize a burn. I cannot read a ward. Recovering the fragment gives us a question for Selene, not an explanation of the breach.",
                    "Keep the fragment wrapped; we have little enough of that carving left. You have the ash too. Before you leave for Archive City, help me clear the wolves from our workers' road.");
            case "ms_oathstead_stand" -> new Scene(
                    "Six wolves are threatening the road our timber workers use. They cannot build shelter while watching the trees for teeth. Deal with that danger, then take your shrine evidence to Selene.",
                    "Give Oathstead's workers room to build while you pursue the knowledge its future defense will need.",
                    "Clearing the wolves makes this work possible. Our palisade and our ward still need separate attention.",
                    "Good. Our workers can use the road again. Take the fragment and ash to Selene in Archive City. Tell her there are people sleeping behind the ward you need her to understand.");
            case "ms_names_dust" -> new Scene(
                    "You brought a piece of the shrine Vaelthara broke. The carving resembles our ward diagrams. Before I send you back to Oathstead with a comforting guess, compare it with the maintenance record here.",
                    "Establish whether the road shrine and Oathstead share a ward design, then find the builders' instructions needed to investigate its weakness.",
                    "A shared design would establish a shared risk worth investigating. It would not tell us how Vaelthara opened the shrine, or whether she can open every ward.",
                    "The shrine and Oathstead use the same ward pattern. We need to compare the instructions kept with the Stone of Memory beneath Archive City. The keeper's instructions were stolen, and the ransom demand in the index names Crowhook Bandit Camp. Recover that document so we can examine the stone safely.");
            case "ms_stolen_index" -> new Scene(
                    "The bandits took a leaf headed 'Old Oath Vault: Keeper's Instructions'. It describes the protective seal around the Stone of Memory beneath Archive City. Their ransom demand names Crowhook Bandit Camp. Defeat the three cutthroats guarding the papers, then search the cache for that heading.",
                    "Recover the missing vault instructions so Selene can investigate the network protecting Oathstead.",
                    "The ransom demand identifies Crowhook Bandit Camp. It does not prove the bandits serve Vaelthara. We need the keeper's instructions recovered before investigating the people behind the theft.",
                    "This is the missing keeper's document. It describes the seal and Memory pedestal inside the Old Oath Vault beneath Archive City. Enter the vault, compare the carved seal with these instructions, then examine the pedestal before removing the stone.");
            case "ms_first_socket" -> new Scene(
                    "The Old Oath Vault is beneath this city. Compare its entry seal with the page you recovered, then examine the Memory pedestal. I want you coming back with an answer, not becoming another reason to seal the stairs.",
                    "Recover Memory, the network's keeper of names and terms, and discover why reaching Vaelthara requires the other eleven stones.",
                    "The vault can preserve instructions. It cannot tell us whether everyone named in them agreed freely, or whether every surviving copy is complete.",
                    "Take the Stone of Memory. The mural places it among twelve functions of one network. Highwall keeps boundary lore; Sanctum tends the fire vessels; the western and Fenland routes preserve other pieces. We need those stones to reach the command site, and their keepers to understand what we are carrying.");
            case "ms_watchtower_bells" -> new Scene(
                    "Highwall Cairn Watch is the bell post beside our old roadwatch graves, southwest of the city. Its bell should warn supply carts of danger. It has fallen silent. Inspect the Split Signal Bell, the Cairn Watch Names beside it, and the Copied Watch Signal by the road before I send another cart through.",
                    "Trace the failure of Highwall's warning chain on the road carrying food and refugees between the northern holds and the rest of Alderfall.",
                    "The families tending the roadwatch graves think their dead relatives are being called back to duty. First I need to know why the living patrol's warning bell stopped working. The bell, burial inscription, and roadside signal give us something to examine.",
                    "The bell was obstructed, a household's watch name used, and our safe-passage signal copied. This was interference, not just winter damage. Three brutes still hold the marked route. Break their force before I send repair crews through.");
            case "ms_raiders_pass" -> new Scene(
                    "Three orc brutes hold the marked route. While they stay there, neither carts nor repair crews can use it. Break that force; afterward we face the standard Kharvok has raised over the pass.",
                    "Remove the force preventing Highwall from restoring its road and responding to Kharvok's command over the northern boundary.",
                    "The brutes are a threat we can locate. Their defeat will not tell us who first damaged the signals or prove that every raid comes from Vaelthara.",
                    "The brutes are down. I still need patrols on that road. Your next enemy is Kharvok, the Banner-Bound: the keepers fear he has twisted the names sewn into surrendered standards into commands.");
            case "ms_frosthollow_standard" -> new Scene(
                    "Kharvok, the commander occupying Banner Cairn north of Highwall, has sewn the names of our dead roadwatchmen into his standard. Defeat him, then read that banner. We found a household's name on the cloth jamming our warning bell; we need to compare it with the names he is using.",
                    "Break Kharvok's hold on the northern pass and recover Iron, which strengthens a boundary but cannot decide whom it should protect.",
                    "His defeat can break this commander's hold. Ending every disturbed burial oath will take the keepers' work afterward.",
                    "Kharvok has fallen, and his standard shows how he changed the oath: the watch could end only when he released it. Take Iron. It holds a boundary under pressure. I will ask the cairn keepers to tend the names we found; his defeat alone does not finish their work.");
            case "ms_shrine_shadow" -> new Scene(
                    "At Sunken Guest Shrine, southwest of Sanctum, a fire spirit lives inside a shrine vessel. The keepers invited it to warm travelers and power the road's protective magic. The vessel has begun burning its keepers. Examine the spirit's cup, the welcome carved beneath it, and the outlet it should be able to leave through.",
                    "Investigate southern fire wards whose failures threaten caravan hospitality and the routes that keep the Sunrealm supplied.",
                    "I do not yet know whether the fire presences are injured, confined, or replaced. Calling all of them demons would settle nothing.",
                    "The welcome allowed departure at moonset. Someone changed that command and plugged the outlet with iron. This vessel held a captive. We must reach the forge and disconnect its draw before opening it. First, clear the imps on the caravan route.");
            case "ms_caravan_glass" -> new Scene(
                    "Six ember imps threaten the shrine road. The caravan carries vessels made for guest flames, and supplies we need to recover. Clear the imps; afterward I can arrange the work of bringing those stores back.",
                    "Reach the southern forge by removing the creatures that prevent recovery along the caravan route.",
                    "We have not established that the imps are the shrine's guests or that killing them repairs a vessel. The caravan recovery remains work to arrange.",
                    "The imps are dealt with. I will arrange recovery of the caravan stores. Go to the marked forge shrine: examine the cradle, disconnect Ember, then open the vessel. I authorize the guest's release. We will have to replace the warmth it was forced to give.");
            case "ms_ember_socket_rite" -> new Scene(
                    "Return to the fire vessel at Sunken Guest Shrine. Its forge controls the iron channel drawing heat from that captive spirit. Inspect Ember's cradle in the workshop, lift the stone clear, then open the vessel's outlet. I authorize the release and will answer for replacing the heat the road wards will lose.",
                    "Recover Ember with an understanding of the difference between sharing protective power and extracting it from a captive source.",
                    "We know this shrine tradition was altered to confine a guest. We do not yet know who ordered the alteration, or how many other vessels were changed.",
                    "You disconnected the draw and opened the outlet. The guest left. Take Ember: it transfers power, but is not itself the fire we imprisoned. That vessel is cold now. I will answer for finding willing sources and repairing the shrines; releasing one guest has not done that work for us.");
            case "ms_bell_alone" -> new Scene(
                    "A landing bell is sounding where no keeper should be pulling the rope. A boat can follow that sound straight into deep water. Inspect the three marked rope sites; I need to know what our warning chain can still be trusted to do.",
                    "Investigate Fenland signals that may guide displaced families toward danger instead of toward shelter.",
                    "We can inspect the bell sites. We cannot identify every voice beneath the marsh from the sound alone.",
                    "Your survey gives the bellkeepers somewhere to begin. Mireford's sick cannot wait for the whole marsh to make sense. Gather fever reed before we face the danger below Miredepth.");
            case "ms_medicine_mireford" -> new Scene(
                    "Mireford needs fever reed. Gather six samples from the marked sources. We can investigate the drowned bells and still make time for people who need help tonight.",
                    "Keep the Fenland investigation connected to living patients who cannot wait for the campaign's mysteries to be solved.",
                    "The samples must still be prepared into medicine. We have not established that the bells caused the illness or that anyone has been treated.",
                    "Six samples. That gives the medicine work a start; the patients still need care. Now we can turn to Velmora below Miredepth, where the keepers say drowned bells are calling the dead.");
            case "ms_miredepth_below" -> new Scene(
                    "Velmora holds the depths below Miredepth. The keepers call her Bell-Drowned, but none can give me a trustworthy account of her first life. We know she is calling the dead against the living. Stop her, then return for Tides.",
                    "Defeat the power threatening the marsh crossings and recover Tides, while leaving the identity of every drowned voice an open question.",
                    "Velmora's defeat will not repair the warning bells or explain every lost ferry. Tides opens routes; it does not make every destination safe.",
                    "Velmora is defeated. Take the Stone of Tides from this recovery. It governs crossings through shifting boundaries. Remember the boat following a bell in fog: opening a route is a responsibility, not merely a way through.");
            case "ms_toll_ledger" -> new Scene(
                    "Oathstead needs grain, and our supply route is losing it. Read the three marked toll records. Before I accuse a bridge keeper or a Briar Court, I need to know what the accounts actually say.",
                    "Trace the missing supplies on Riverside's bridges, where ordinary trade meets the paths and obligations of the Briar Courts.",
                    "Missing grain may mean theft, false accounts, or a route beyond our guards' reach. A court's reputation is not proof that it took this cargo.",
                    "We have the toll survey. Eight raiders still hold the marked supply route. Clear them so Riverside can begin looking for its stores; the records alone will not feed your camp.");
            case "ms_redcap_trade" -> new Scene(
                    "Eight raiders are holding the supply route. Redcaps trade at the edges of Briar hunts, but I cannot tell you who bought this grain. Break their hold first. Then we can pursue what their scavengers carried away.",
                    "Disrupt the raiders blocking Riverside's supplies without treating every western creature as a servant of Vaelthara.",
                    "Clearing the raiders does not recover the crates. We still do not know whether the hunt, ordinary buyers, or both profited from them.",
                    "The raiders are dealt with. The stores still need recovering. There is a marked scavenger to stop next; the stone among those stolen goods may explain why this route drew more than ordinary thieves.");
            case "ms_glowing_mud" -> new Scene(
                    "The marked scavenger is our next target. We need the stone recovered from the stolen goods. The old accounts call it Hunger: a useful name if you remember to ask what it feeds on.",
                    "Recover Hunger from the stolen supply route and distinguish the network's use of excess force from its possible abuse of living harvests.",
                    "The river accounts describe absorbing excess ward power. They do not prove what the stolen stone has consumed on this road.",
                    "Take the Stone of Hunger. The river accounts say it once drew in surplus force when wards were struck. Find out where that force goes before you feed it more. Riverside still has grain to recover and mouths to fill.");
            case "ms_orchard_ward" -> new Scene(
                    "We used to leave the first fallen apple for the stag. Now Rootmaw attacks the people tending the trees. Stop it before another household loses someone to the orchard that feeds us.",
                    "Face a wounded guardian of a living ward and recover Roots without confusing the creature's defeat with the orchard's healing.",
                    "I know our orchard customs and I know Rootmaw is attacking. I do not know which injury or broken protection drove it to this.",
                    "Rootmaw is down. I will tend the orchard, but that will take more than a fight. Take Roots. Protection once passed through these trees to the households around them; remember those households when you work on the greater network.");
            case "ms_names_cold_stone" -> new Scene(
                    "The names on Stonegate's graves are damaged, and the Nameless Warden attacks those who approach. Our burial words release a person from duties held in life. I fear that release is failing. Stop the Warden so the graves can be reached again.",
                    "Recover Graves while confronting the possibility that the old defense continues to demand service from people who should be at rest.",
                    "The damaged names and the Warden's behavior support my fear. They do not tell us who damaged the graves or prove that every burial binding is broken.",
                    "The Warden has fallen. The names still need restoring. Take Graves: its place in the network is to recognize that a life, and its service, can end. Do not build a new safety that refuses people that release.");
            case "ms_cold_road" -> new Scene(
                    "The Hailback Broodmother blocks Snowrest's winter road. We have families waiting on supplies that must come through that pass. Stop her; the road crews will still have work after the fighting.",
                    "Recover Frost from the northern road and recognize the difference between buying time against disaster and trapping a place in endless suspension.",
                    "Hailbacks belong to these mountains. Their presence is not evidence of Vaelthara's orders. Removing this one will not end winter or deliver our stores.",
                    "The Broodmother is defeated. We still need supply runs and patrols. Take Frost. The old keepers used it to slow a failing protection until help came. A delay is only mercy if help eventually arrives.");
            case "ms_missing_bell_rope" -> new Scene(
                    "A bell line needs a sound foundation before it needs a heroic speech. Inspect both marked foundations. The Bells stone belongs to this network, and I need to know what we can build around it.",
                    "Recover Bells while establishing the physical work still needed to carry warnings between the Fenlands and Oathstead.",
                    "The foundation survey will guide repairs. Until rope and fittings are replaced, I cannot promise a warning will pass along the whole line.",
                    "That gives me a repair survey. Take Bells. It coordinates warnings across distance, but it still needs keepers, rope, and metal that holds. A promise to warn someone is work, every day.");
            case "ms_blackvault_mark" -> new Scene(
                    "Blackvault held the residue of old ward workings. Its maintenance books describe stores that should have been discharged. Sareth now blocks access to the Ash stone. Defeat him so that recovery can begin.",
                    "Recover Ash and confront the accumulated cost of a defense whose spent power was stored where later generations could ignore it.",
                    "The maintenance books describe Blackvault's purpose. They do not establish who ordered its neglect, or prove a personal history for Vaelthara.",
                    "Sareth is defeated. Take Ash. It gives spent power a way out of the network; sealing waste away forever only leaves the danger to somebody else. Blackvault still needs work beyond this one chamber.");
            case "ms_camp_defending" -> new Scene(
                    "Morvane's raid is coming for Oathstead. This is the camp that took you in after the shrine; now it is home to people with nowhere else to stand. Use the defense point when you are ready. We have to hold.",
                    "Defend the home built after the shrine attack and make voluntary cooperation, rather than Vaelthara's obedience, the basis of its survival.",
                    "Holding against Morvane will stop this raid. It will not end Vaelthara's campaign or guarantee that another army cannot reach us.",
                    "We held. You are still here, and so are the people who stood with you. Take Oaths for the Gate expedition. It recognizes a commitment freely made; after a day like this, I understand why that needed a stone of its own.");
            case "ms_kingdoms_answer" -> new Scene(
                    "Oathstead held, but we cannot sustain the next fight alone. Ask Mirella, Odrick, Selene, Ysra, and Solari what help they will commit while we reach the Gate. Bring back their conditions as carefully as their promises.",
                    "Gather five explicit mainland commitments for Oathstead and the Gate expedition, while allowing each participant to state the limits of their help.",
                    "An agreement is not an arrived grain barge, a repaired bell, or a full account of the old binding. We must plan around those limits.",
                    "Five commitments, each with its limits. Take Dawn, the twelfth stone. We can attempt to open the Gate. The promised help still needs to reach us, and these five voices do not speak for every coast or every power the network touches.");
            case "ms_twelve_stones_gate" -> new Scene(
                    "You have all twelve stones. Take them to the Old Gate and open the way to the Hollow Throne. Vaelthara left a survivor at the shrine. She is about to face someone who has learned what her rule would cost.",
                    "Open the route to Vaelthara's command site with the twelve stones, carrying the needs of Oathstead and the regions into the confrontation.",
                    "Opening the Gate gives us a way to reach her. It does not defeat her or settle the future of every ward. The realms' promises remain preparations for the struggle ahead.",
                    "The Gate is open. Beyond it is the power that broke the road shrine. Here, there are people who chose to help you reach it. Go when you are ready; I will keep working for the home you are coming back to.");
            default -> null;
        };
    }
}
