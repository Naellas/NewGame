package com.alderfall.game;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Authored context and evidence, shared by journals and conversations. */
public final class QuestNarrative {
    private QuestNarrative() { }

    public static String clean(String line) {
        if (line == null) return "";
        return line.replaceFirst("^[A-Z][A-Za-z ']+: ", "").strip();
    }

    public static String subject(Quest quest) {
        if (MainStoryContent.supports(quest)) return MainStoryContent.subject(quest);
        if (quest.completed) return clean(quest.activeCompleteDialog());
        if (quest.ready()) return clean(quest.activeReadyDialog());
        return clean(quest.activeStartDialog());
    }

    public static String findings(Quest quest) {
        List<String> facts = new ArrayList<>();
        for (Quest.QuestStage stage : quest.stages) {
            if (quest.observedStages.contains(stage.id())) facts.add(clean(stage.readyDialog()));
        }
        if (facts.isEmpty()) return "We have not recorded a finding from this investigation yet. " + instruction(quest);
        // Keep the most recent evidence readable; the journal retains all completed stage IDs.
        return String.join(" ", facts.subList(Math.max(0, facts.size() - 2), facts.size()));
    }

    public static String instruction(Quest quest) {
        if (quest.completed && MainStoryContent.supports(quest)) return MainStoryContent.completedReport(quest);
        if (quest.completed) return clean(quest.activeCompleteDialog());
        if (quest.ready()) return "Report the result before collecting the reward. " + clean(quest.activeReadyDialog());
        return clean(quest.activeProgressDialog()) + CompanionQuestContent.cargoSummary(quest);
    }

    public static String purpose(Quest q) {
        if (MainStoryContent.supports(q)) return MainStoryContent.purpose(q);
        String segment = CompanionQuestContent.purpose(q);
        if (!segment.isBlank()) return segment;
        if (q.companionQuest()) return switch (q.chainOwnerId) {
            case "aria" -> "Someone is using my sister's trail to draw us into an ambush. I need your help checking the signs before I follow them.";
            case "seraphine" -> "Riverside's debt collectors still use my family's name. I know the court, but I need someone outside it to help me test their claims.";
            case "maera" -> "The Archive's maps leave out a road connected to the old wards. I need to compare what we find on the ground with what the records claim.";
            case "cassia" -> "I closed Highwall's gate while people were still outside. I need to establish what happened and help protect the people living with that decision.";
            case "lyra" -> "People in the Fenlands are falling ill despite receiving medicine. I need help checking the supplies and reaching patients while I investigate.";
            case "samir" -> "My family tends a sacred flame, but I no longer trust the seal around it. I need a witness while I test what the keepers taught me.";
            case "vesper" -> "The roots breaking Snowrest's road lead toward my family's sealed grove. We need to find the source without destroying healthy growth that protects the village.";
            case "rafiq" -> "A man died in the duel I fled. I need to find out how it was arranged and face the people I left with the consequences.";
            case "calder" -> "A bridge I approved collapsed. The crossing here needs sound materials and a proper test; I will not call it safe just because the repairs look finished.";
            default -> clean(q.description);
        };
        if (q.mainStoryQuest()) return switch (q.chainOwnerId) {
            case "maelis" -> "People came to Oathstead after the attack on the road shrine. We need supplies and working defenses before Vaelthara reaches the camp.";
            case "selene" -> "The fragment from the road shrine may tell us how Oathstead's wards were built. Recovering the missing instructions gives us a way to investigate their failure.";
            case "odrick" -> "Highwall's pass carries food and medicine to the northern settlements. The broken signals and armed attacks have made that route unsafe.";
            case "solari" -> "The Sunrealm relies on its shrine wards to keep caravan routes open. I need to learn what damaged them before attempting a new rite.";
            case "ysra", "nessa" -> "The Fenland bells guide boats and warn villages of floods. Their failures are cutting people off from medicine and safe passage.";
            case "mirella" -> "Riverside's bridges carry the grain that Oathstead needs. We must recover the stolen supplies and identify what is disrupting the route.";
            case "rowan" -> "The orchard feeds nearby households. Its damaged ward and the Rootmaw Stag now threaten the people tending it.";
            case "hollis" -> "The names on Stonegate's graves help keep its dead at rest. Someone has damaged those protections, and the Warden is attacking visitors.";
            case "elric" -> "Snowrest needs a safe road for its winter supplies. The broodmother and frost wolves are keeping travelers away.";
            case "damar" -> "Blackvault once contained dangerous remnants of the old war. Its new activity could threaten the settlements around it.";
            default -> clean(q.description);
        };
        return clean(q.description);
    }

    public static String uncertainty(Quest q) {
        if (MainStoryContent.supports(q)) return MainStoryContent.uncertainty(q);
        if (q.completed) return "This request is finished. It does not settle every problem in the region. " + clean(q.activeCompleteDialog());
        if (q.ready()) return "This result establishes only what you observed or did here. We should not treat it as proof of who caused every failure in the wards.";
        return "We still need to complete this step: " + clean(q.activeProgressDialog())
                + " I will not count a proposed solution as finished work.";
    }

    public static Map<String, Quest> refine(Map<String, Quest> source) {
        Map<String, Quest> result = new LinkedHashMap<>(source);
        replace(result, "ms_wake_ashes", "Examine the burned road shrine and its broken ward before reporting to Maelis.", List.of(
                inspect("shrine_damage", "Examine the burned shrine", "Burned Road Shrine", "story_road_shrine", 0, "quest_ward_marker",
                        "Maelis: You survived the attack at the road shrine. Show us what was damaged so we can decide how to protect Oathstead.",
                        "Examine the burned altar in the Road Shrine, reached from Oathstead's shrine path.",
                        "The altar is scorched, but the damage is concentrated around its ward socket."),
                inspect("shrine_socket", "Inspect the broken socket", "Broken Ward Socket", "story_road_shrine", 1, "quest_ward_marker",
                        "The socket is cracked. Inspect its inner face before deciding what broke it.",
                        "Inspect the broken socket beside the altar in the Road Shrine.",
                        "Carved lines continue inside the broken socket. A loose fragment still carries part of the pattern.")));
        replace(result, "ms_road_dust", "Recover a readable fragment and an ash sample for the investigation into Oathstead's wards.", List.of(
                inspect("shrine_fragment", "Secure the carved fragment", "Carved Ward Fragment", "story_road_shrine", 2, "quest_ward_marker",
                        "Maelis: Selene in Archive City may recognize these marks. Recover the loose fragment without scraping off the carving.",
                        "Recover the carved fragment from the marked rubble in the Road Shrine.",
                        "You secured the fragment. Its inner carving is still readable; this does not yet explain why the ward failed."),
                inspect("shrine_ash", "Examine the ash deposit", "Shrine Ash Sample", "story_road_shrine", 3, "location_camp_fire",
                        "The ash beside the socket may help Selene compare the attack with earlier breaches.",
                        "Collect the marked ash sample beside the shrine's broken socket.",
                        "You secured an ash sample from beside the socket. Selene still needs to compare it with the Archive's records.")));
        replace(result, "ms_names_dust", "Compare the shrine evidence with the ward records in Archive City.", List.of(
                inspect("archive_record", "Read the ward maintenance record", "Ward Maintenance Record", "city_archive", 0, "quest_document_bundle",
                        "Selene: You brought evidence from the shrine. The maintenance record in this square may show whether Oathstead uses the same ward pattern.",
                        "Read the marked ward maintenance record in Archive City.",
                        "The record depicts the same carving as the shrine fragment and lists Oathstead among the protected sites."),
                inspect("archive_index", "Check the missing instructions", "Damaged Ward Index", "city_archive", 1, "quest_document_bundle",
                        "The maintenance record refers to a builders' index. Check which instructions remain.",
                        "Inspect the damaged ward index beside the Archive's reading station.",
                        "The index lists northern and southern ward instructions, but its vault-access page has been removed.")));
        Quest stolen = result.get("ms_stolen_index");
        List<Quest.QuestStage> stolenStages = new ArrayList<>();
        stolenStages.add(stage(stolen.id + "_guards", "Clear the page cache", "Bandit Cutthroat", 3, Quest.ObjectiveKind.DEFEAT,
                "overworld", "bandit_camp", 0, "bandit_cutthroat", "bandit_cutthroat", "",
                "Selene: The missing vault page was taken toward Crowhook. Clear the guards at the marked cache, then search it.",
                "Defeat the three marked cutthroats guarding the page cache at Crowhook.",
                "The marked guards are defeated. The page still needs to be recovered."));
        stolenStages.add(stage(stolen.id + "_page", "Recover the vault page", "Stolen Vault Page", 1, Quest.ObjectiveKind.SEARCH,
                "overworld", "bandit_camp", 0, "quest_document_bundle", null, "",
                "The cache is accessible. Search for the builders' vault instructions.",
                "Search the marked document cache at Crowhook for the vault page.",
                "You recovered the page describing the Old Oath Vault's entry seal. It identifies the Stone of Memory inside."));
        replace(result, stolen.id, "Recover the stolen vault-access page from Crowhook, after securing its cache.", stolenStages);
        replace(result, "ms_first_socket", "Use the recovered instructions to inspect the Old Oath Vault and recover the Stone of Memory.", List.of(
                inspect("vault_seal", "Examine the vault entry seal", "Vault Entry Seal", "story_oath_vault", 0, "quest_ward_marker",
                        "Selene: The recovered page describes the seal beneath the Archive. Compare it with the vault before touching the stone.",
                        "Enter the Old Oath Vault from the marked entrance in Archive City and inspect its seal.",
                        "The entry seal matches the recovered instructions. The chamber contains a twelve-socket mural."),
                inspect("vault_memory", "Examine the Memory pedestal", "Memory Pedestal", "story_oath_vault", 1, "quest_ward_marker",
                        "The pedestal bears the same mark as the socket labeled Memory. Check that the stone can be removed safely.",
                        "Inspect the marked Memory pedestal, then report to Selene to complete the recovery.",
                        "The Memory pedestal responds to the recovered instructions. Report to Selene to receive the stone and plan the next journey.")));

        String[][] commitments = {
                {"Mirella", "city_riverside", "Riverside will supply grain for Oathstead's defenders. The barges still need an escort before they can sail."},
                {"Odrick", "city_highwall", "Highwall will send a watch detail to Oathstead. The soldiers have been promised; they have not arrived yet."},
                {"Selene", "city_archive", "The Archive will share its surviving ward instructions. Selene warns that a complete record of the first binding has not survived."},
                {"Ysra", "city_belltower", "The bellkeepers will relay warnings between the settlements. Ysra cannot promise that every damaged bell will answer."},
                {"Solari", "city_sanctum", "Sanctum will support the attempt to open the Old Gate. Solari gives no assurance that the twelve stones can control what lies beyond it."}
        };
        List<Quest.QuestStage> answers = new ArrayList<>();
        for (String[] commitment : commitments) {
            String name = commitment[0];
            answers.add(stage("kingdom_answer_" + name.toLowerCase(java.util.Locale.ROOT), "Request " + name + "'s commitment",
                    "Support for Oathstead", 1, Quest.ObjectiveKind.TALK, commitment[1], "", 0, "", null, name,
                    "Maelis: We have the stones and we survived the raid. Ask " + name + " what help their people can actually promise.",
                    "Speak with " + name + " and explicitly request support for Oathstead and the Old Gate expedition.",
                    commitment[2]));
        }
        replace(result, "ms_kingdoms_answer", "Ask the five mainland representatives for specific commitments, then report their answers to Maelis.", answers);

        replace(result, "skull_wards", "Check the two graveyard charms so Cal can request the right replacements from Sanctum.", List.of(
                stage("skull_ward_warm", "Test the humming skull ward", "Humming Skull Ward", 1, Quest.ObjectiveKind.SEARCH,
                        "overworld", "graveyard", 1, "location_graveyard_skull_marker", null, "",
                        "Cal: The skull charms keep the dead inside this burial ground. One has stopped humming. Test both before I ask Sanctum for replacements.",
                        "Inspect the marked humming skull on the graveyard fence.",
                        "The first skull hums when you approach. Its charm still responds; that does not establish the condition of the rest of the fence."),
                stage("skull_ward_cold", "Test the silent skull ward", "Silent Skull Ward", 1, Quest.ObjectiveKind.SEARCH,
                        "overworld", "graveyard", 1, "location_graveyard_skull_marker", null, "",
                        "The first charm responds. Test the silent skull before giving Cal your report.",
                        "Inspect the marked silent skull on the graveyard fence, then return to Cal.",
                        "The second skull stays cold and silent. Cal needs one replacement charm; inspecting it has not repaired the ward.")));

        // Honest regional context for every remaining main-story offer; retain supported objectives.
        for (Quest q : List.copyOf(result.values())) {
            if (q.mainStoryQuest() && q.contentRevision == 0) {
                List<Quest.QuestStage> stages = new ArrayList<>();
                for (Quest.QuestStage s : q.stages) {
                    String instruction = supportedInstruction(s);
                    stages.add(new Quest.QuestStage(s.id(), s.title(), s.target(), s.needed(), s.objectiveKind(),
                            s.objectiveMapId(), s.objectiveLocationKind(), s.objectiveLocationIndex(), s.objectiveAsset(),
                            s.monsterKey(), s.targetNpcId(), s.branchOutcomeKey(), purpose(q) + " " + instruction,
                            instruction, honestResult(q, s), honestCompletion(q, s)));
                }
                result.put(q.id, copy(q, purpose(q) + " " + supportedInstruction(q.activeStage()), stages, 0));
            }
        }

        // Distinct objects replace side-quest claims previously inferred from a kill counter.
        addRecovery(result, "peddler_ledger", "Recover Orren's account book from the scouts' cache.", "Peddler's Ledger",
                "goblin_camp", 0, "Orren: Scouts took my account book. Search their marked cache; I need the book, not a report of dead scouts.",
                "You recovered Orren's account book from the marked cache. The entries are still legible.");
        refineSide(result, "wolf_pelt_order", "Clear ten Grey Wolves threatening the winter roadwatch.",
                "Sori: Wolves are attacking roadwatch patrols. Deal with ten of them while the tanner arranges winter clothing.",
                "Defeat ten Grey Wolves. This request covers the patrol threat, not collecting or delivering pelts.",
                "Ten Grey Wolves have been defeated. Report the result to Sori.",
                "Sori: That removes ten wolves from the patrol routes. The tanner still has work to do on the cloaks.");
        refineSide(result, "goat_bell_roundup", "Stop three Mountain Goats damaging the warning-bell posts.",
                "Una: Three goats keep breaking the posts on the ridge. Stop them before the warning bells fall.",
                "Defeat three Mountain Goats. Una will arrange repairs to the damaged bell posts.",
                "Three Mountain Goats have been defeated. The damaged posts still need repairs.",
                "Una: The goats are dealt with. I can send workers to repair the posts now.");
        for (Quest q : List.copyOf(result.values())) {
            if (q.companionQuest() && "vesper".equals(q.chainOwnerId)
                    && (q.id.equals("vesper_chain_1") || q.id.equals("vesper_chain_2"))) {
                List<Quest.QuestStage> stages = new ArrayList<>();
                for (Quest.QuestStage s : q.stages) {
                    boolean inspect = s.objectiveKind().inspectObjective();
                    boolean elder = s.id().equals("vesper_chain_2_patient");
                    stages.add(new Quest.QuestStage(s.id(), s.title(), s.target(), s.needed(), s.objectiveKind(),
                            inspect || elder ? "village_snowrest" : s.objectiveMapId(), inspect ? "story" : elder ? "" : s.objectiveLocationKind(),
                            stages.size() + 3, s.objectiveAsset(), s.monsterKey(), elder ? "Goatkeeper Una" : s.targetNpcId(), s.branchOutcomeKey(),
                            s.startDialog(), inspect ? "Inspect the marked " + s.target() + " on Snowrest's village paths."
                                    : elder ? "Ask Goatkeeper Una in Snowrest where she saw the first crack appear." : s.progressDialog(),
                            elder ? "Una: The first crack opened under the winter shrine. I saw it before the roots reached the road. It ran toward the grove your family sealed."
                                    : s.readyDialog(), s.completeDialog()));
                }
                result.put(q.id, copy(q, q.description, stages, 0));
            }
        }
        CompanionQuestContent.refine(result);
        MainStoryContent.refine(result);
        result.replaceAll((id, quest) -> NpcQuestStories.refine(quest));
        result.put(FurnitureQuestContent.ID, FurnitureQuestContent.create());
        return Map.copyOf(result);
    }

    private static String supportedInstruction(Quest.QuestStage s) {
        return switch (s.objectiveKind()) {
            case DEFEAT, RESCUE, DEFEND -> "Defeat " + s.needed() + " marked " + s.target() + " opponents. Follow the quest markers to their encounter.";
            case GATHER -> "Gather " + s.needed() + " " + s.target() + " samples from the marked sources.";
            case RAID_DEFENSE -> "Use the marked Oathstead defense point and repel the raid.";
            case TALK, REPORT, DELIVER, ASK_AROUND -> "Discuss " + s.target() + " with the marked contact and explicitly give your report.";
            case CHOICE -> "Discuss " + s.target() + " and choose how to proceed. Asking questions does not make the decision.";
            default -> "Inspect " + s.needed() + " marked " + s.target() + " locations. Report only what those inspections establish.";
        };
    }

    private static String honestResult(Quest q, Quest.QuestStage s) {
        String result = switch (q.id) {
            case "ms_oathstead_stand" -> "Six wolves have been dealt with. Maelis can send the next work party along the camp road; the palisade still needs building.";
            case "ms_raiders_pass" -> "The three marked orc brutes are defeated. Odrick still needs to organize patrols before he can promise travelers a safe pass.";
            case "ms_frosthollow_standard" -> "Kharvok is defeated. Report his fall to Odrick so he can release the Stone of Iron for the Gate expedition.";
            case "ms_watchtower_bells" -> "You examined the three marked signal-bell sites. Odrick now has a survey to work from; the bells have not been repaired.";
            case "ms_shrine_shadow" -> "The three marked altar sites have been examined. Give Solari the survey before attempting a new ward rite.";
            case "ms_caravan_glass" -> "Six ember imps are defeated. Solari can arrange the caravan's recovery; the fight itself did not deliver its oil or supplies.";
            case "ms_ember_socket_rite" -> "You reached the marked forge shrine. Return to Solari to complete recovery of the Stone of Ember.";
            case "ms_bell_alone" -> "You checked the three marked bell-rope sites. Ysra can use the survey to investigate the failing warnings; the cause is still unproven.";
            case "ms_medicine_mireford" -> "Six fever-reed samples are secured. Give Ysra your report so the medicine can be prepared; the patients have not yet been treated.";
            case "ms_miredepth_below" -> "Velmora is defeated. Report to Ysra for the recovered stone. The Fenlands still need their damaged warning bells restored.";
            case "ms_toll_ledger" -> "The three marked toll records have been examined. Report to Mirella; reading them has not returned the missing grain to Riverside.";
            case "ms_redcap_trade" -> "Eight marked raiders are defeated. The stolen supply route can now be searched; this does not mean its crates have reached Riverside.";
            case "ms_glowing_mud" -> "The marked scavenger is defeated. Report to Mirella to complete recovery of the Stone of Hunger.";
            case "ms_orchard_ward" -> "Rootmaw is defeated. Rowan can begin tending the damaged orchard; its ward still needs attention.";
            case "ms_names_cold_stone" -> "The Nameless Warden is defeated. Hollis can return to the burial ground, but the scratched names still need restoring.";
            case "ms_cold_road" -> "The Hailback Broodmother is defeated. Elric still needs to arrange winter supplies and patrols; this encounter did not deliver firewood or medicine.";
            case "ms_missing_bell_rope" -> "Both marked bell-foundation sites have been inspected. Nessa can use the survey for repairs; no new rope or hooks have been installed.";
            case "ms_blackvault_mark" -> "Sareth is defeated. Report to Damar for the Stone of Ash. The other wards in Blackvault have not been cleared by this fight.";
            default -> "";
        };
        if (!result.isBlank()) return result;
        return switch (s.objectiveKind()) {
            case DEFEAT, RESCUE, DEFEND -> "You defeated the required " + s.target() + " opponents. That resolves this encounter, not every danger on the road.";
            case GATHER -> "You gathered the requested " + s.target() + " samples. Their meaning still needs to be discussed.";
            case RAID_DEFENSE -> "Oathstead held against this raid. Its people still need working defenses.";
            default -> "You completed the required checks for " + s.target() + ". Report these findings before deciding what follows.";
        };
    }

    private static String honestCompletion(Quest q, Quest.QuestStage s) {
        return switch (q.id) {
            case "ms_oathstead_stand" -> "Maelis: With those wolves gone, our workers can use the road again. Take the shrine evidence to Selene in Archive City while we keep building the camp.";
            case "ms_watchtower_bells" -> "Odrick: Your survey tells me where the signals failed. The next task is to break the marked raiders threatening the pass.";
            case "ms_raiders_pass" -> "Odrick: The brutes are defeated. Kharvok remains at the marked stronghold; his banner gives the raiders something to rally around.";
            case "ms_shrine_shadow" -> "Solari: I have your shrine survey. Clear the imps threatening the caravan route before we attempt the next rite.";
            case "ms_caravan_glass" -> "Solari: The imps are dealt with. I will arrange recovery of the caravan stores. Find the marked forge shrine for the next step.";
            case "ms_bell_alone" -> "Ysra: I have your bell survey. Before we follow the disturbance below ground, Mireford needs fever reed for its sick.";
            case "ms_medicine_mireford" -> "Ysra: The reed is accounted for. I will see to its preparation. You can now follow the marked route to Velmora's encounter.";
            case "ms_toll_ledger" -> "Mirella: Your report gives me a place to start. Break the marked raiders holding the supply route, and I can organize a search for the grain.";
            case "ms_redcap_trade" -> "Mirella: Those raiders are defeated. One marked scavenger still holds the lead to the missing socket stone. Deal with that encounter next.";
            case "ms_kingdoms_answer" -> "The mainland representatives have committed their support. With the other eleven stones secured, the Stone of Dawn completes the Gate's key.";
            case "ms_twelve_stones_gate" -> "The twelve stones have opened the Old Gate. The Hollow Throne is reachable; Vaelthara has not yet been defeated.";
            case "ms_orchard_ward" -> "Rootmaw has been defeated. The orchard still needs tending; the recovered Stone of Roots can help us understand its protection.";
            default -> {
                String stone = GameData.mainStoryStoneReward(q.id);
                yield stone.isBlank() ? honestResult(q, s) + " We can now follow the next lead."
                        : "Your report is accepted, and the recovered socket stone is now yours to carry. " + purpose(q);
            }
        };
    }

    private static void refineSide(Map<String, Quest> map, String id, String description, String offer, String instruction, String ready, String done) {
        Quest q = map.get(id);
        Quest.QuestStage s = q.activeStage();
        var replacement = new Quest.QuestStage(s.id(), s.title(), s.target(), s.needed(), s.objectiveKind(), s.objectiveMapId(),
                s.objectiveLocationKind(), s.objectiveLocationIndex(), s.objectiveAsset(), s.monsterKey(), s.targetNpcId(), s.branchOutcomeKey(),
                offer, instruction, ready, done);
        map.put(id, copy(q, description, List.of(replacement), 0));
    }

    private static void addRecovery(Map<String, Quest> map, String id, String description, String target, String site, int index, String offer, String result) {
        replace(map, id, description, List.of(stage(id + "_recover", "Recover " + target, target, 1, Quest.ObjectiveKind.SEARCH,
                "overworld", site, index, "quest_document_bundle", null, "", offer,
                "Search the marked cache for " + target + ", then return to its owner.", result)));
    }

    private static Quest.QuestStage inspect(String id, String title, String target, String map, int anchor, String asset,
                                           String offer, String instruction, String result) {
        return stage(id, title, target, 1, Quest.ObjectiveKind.SEARCH, map, "story", anchor, asset, null, "", offer, instruction, result);
    }

    private static Quest.QuestStage stage(String id, String title, String target, int count, Quest.ObjectiveKind kind,
                                          String map, String location, int index, String asset, String monster, String npc,
                                          String offer, String instruction, String result) {
        String completion = switch (id) {
            case "shrine_socket" -> "Maelis: The damage centers on the socket, and part of its carving survived. Recover that fragment so Selene can compare it with Oathstead's ward records.";
            case "shrine_ash" -> "Maelis: You have the carved fragment and the ash sample. Take this evidence to Selene in Archive City; we still do not know what broke the ward.";
            case "vault_memory" -> "Selene: The Stone of Memory is yours to carry. Its socket is one of twelve in the vault mural. The other regions may preserve instructions that our Archive has lost.";
            case "kingdom_answer_solari" -> "Maelis: Five commitments, with their limits stated plainly. Take the Stone of Dawn. We have all twelve stones for the Gate; the promised reinforcements are still preparations for what follows.";
            case "skull_ward_cold" -> "Cal: One working charm and one spent. I will request a replacement from Sanctum. Until it arrives, the silent skull is still a gap in our protection.";
            default -> result;
        };
        return new Quest.QuestStage(id, title, target, count, kind, map, location, index, asset, monster, npc, "", offer, instruction, result, completion);
    }

    private static void replace(Map<String, Quest> map, String id, String description, List<Quest.QuestStage> stages) {
        map.put(id, copy(map.get(id), description, stages, 1));
    }

    private static Quest copy(Quest q, String description, List<Quest.QuestStage> stages, int revision) {
        Quest.QuestStage first = stages.get(0);
        Quest rewritten = new Quest(q.id, q.title, description, first.target(), first.needed(), q.rewardGold, q.rewardXp,
                first.objectiveKind(), first.objectiveMapId(), first.objectiveLocationKind(), first.objectiveLocationIndex(),
                first.objectiveAsset(), first.monsterKey(), first.startDialog(), first.progressDialog(), first.readyDialog(),
                stages.get(stages.size() - 1).completeDialog(), q.type, q.chainOwnerId, q.nextQuestId,
                first.targetNpcId(), first.branchOutcomeKey(), stages);
        rewritten.contentRevision = revision;
        return rewritten;
    }
}
