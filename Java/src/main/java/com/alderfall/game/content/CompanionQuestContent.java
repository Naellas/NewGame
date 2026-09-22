package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Complete companion segments: observations, arrangements, handovers, and aftermath. */
public final class CompanionQuestContent {
    private CompanionQuestContent() { }

    public static void refine(Map<String, Quest> quests) {
        replace(quests, "seraphine_chain_2",
                "A former Thorn Hall servant may know why the Vale invitations keep renewing. Find his account and arrange a refuge before asking him to testify further.", List.of(
                local("seraphine_invitations", "Collect the renewed invitations", "Renewed Thorn Hall Invitation", 3, Quest.ObjectiveKind.GATHER, "city_riverside", 20, "quest_document_bundle",
                        "Seraphine: Three invitations arrived bearing my dead uncle's name. Find the marked copies in Riverside. A former servant has offered to compare them with the host's original promise.",
                        "Collect three marked invitations in Riverside, then meet former servant Iven.",
                        "All three invitations carry the same guest seal and a later date. That proves renewal, not that the named guest agreed."),
                local("seraphine_iven_account", "Ask Iven about the host's promise", "Former Servant Iven", 1, Quest.ObjectiveKind.TALK, "city_riverside", 23, "npc_citizen_man",
                        "Iven knows the household's old terms. Let him state what he remembers before asking him to risk a public accusation.",
                        "Speak to Iven in Riverside and ask what he personally heard the host promise.",
                        "Iven: I heard the host promise one winter of service. The steward renewed my invitation without asking. I will meet you at the refuge if its threshold is sound; I am not safe there yet."),
                local("seraphine_refuge_threshold", "Secure the refuge threshold", "Refuge Guest Charm", 1, Quest.ObjectiveKind.SEARCH, "city_riverside", 24, "quest_ward_marker",
                        "Seraphine: This guest charm bars an uninvited collector. Set it against the refuge threshold and check that it answers before Iven comes.",
                        "Set and test the marked refuge charm in Riverside, then meet Iven at the refuge.",
                        "The charm lights across the threshold. The refuge is prepared; Iven's arrival remains unconfirmed."),
                local("seraphine_iven_arrival", "Confirm Iven reached the refuge", "Former Servant Iven", 1, Quest.ObjectiveKind.TALK, "city_riverside", 25, "npc_citizen_man",
                        "The threshold is prepared. Iven agreed to make his own way here; check that he arrived before reporting him sheltered.",
                        "Meet Iven at the marked Riverside refuge and confirm his arrival.",
                        "Iven: I reached the refuge. The collector stopped at the guest charm. I will stay here tonight. It shelters me; it has not released me from Thorn Hall's service.")));

        replace(quests, "aria_chain_2",
                "A charcoal burner saw someone wearing Aria's sister's ribbon enter a road through the hedge. Record his account and arrange a protected meeting place.", List.of(
                local("aria_cut_ribbons", "Collect the cut trail ribbons", "Cut Trail Ribbon", 3, Quest.ObjectiveKind.GATHER, "village_oakhaven", 20, "quest_broken_road_signs",
                        "Aria: Someone cut the ribbons along Oakhaven's hedge. Collect the three marked pieces. The charcoal burner saw who passed, but he will not wait on that road after dusk.",
                        "Collect the three marked ribbon pieces in Oakhaven. Then find charcoal burner Bren.",
                        "The three pieces were cut cleanly rather than torn by thorns. The cuts do not identify who made them."),
                local("aria_bren_account", "Hear the charcoal burner's account", "Charcoal Burner Bren", 1, Quest.ObjectiveKind.TALK, "village_oakhaven", 23, "npc_citizen_man",
                        "Bren saw the hedge path open. Ask what he saw, not whether he can confirm everything Aria hopes.",
                        "Ask Bren which traveler he saw enter the hedge road.",
                        "Bren: At dusk I saw a woman with that ribbon walk through the hedge. I did not see her face. A rider came after her. Mark the kiln refuge and I will meet you there."),
                local("aria_kiln_countermark", "Mark the kiln refuge", "Kiln Refuge Countermark", 1, Quest.ObjectiveKind.SEARCH, "village_oakhaven", 24, "quest_roadwatch_warning_marks",
                        "Aria: A reversed trail knot turns the hedge riders from a doorway. Set this one at the kiln refuge; Bren will travel there on his own.",
                        "Set the marked counterknot at Oakhaven's kiln refuge, then check for Bren.",
                        "The counterknot holds its shape when the hedge wind reaches it. The refuge is marked; Bren has not yet confirmed arrival."),
                local("aria_bren_arrival", "Meet Bren at the kiln refuge", "Charcoal Burner Bren", 1, Quest.ObjectiveKind.TALK, "village_oakhaven", 25, "npc_citizen_man",
                        "The refuge is marked. Meet Bren there before telling Aria her witness is sheltered.",
                        "Speak to Bren at the marked kiln refuge in Oakhaven.",
                        "Bren: I reached the kiln. The rider passed its door twice without turning in. I can wait here while you check the hedge. I still cannot swear that the woman was your sister.")));

        Quest patient = quests.get("lyra_chain_3");
        List<Quest.QuestStage> care = new ArrayList<>(List.of(
                local("lyra_eda_assessment", "Ask Eda what happened", "Mireford Patient Eda", 1, Quest.ObjectiveKind.TALK, "village_mireford", 20, "npc_citizen_woman",
                        "Lyra: Eda is feverish beside the Mireford cot station. Ask if she can speak and what touched her skin. We need to treat her before taking the story back to the supplier.",
                        "Speak to Eda at Mireford's marked cot station.",
                        "Eda: The shaking started after they tied the new bell-rope around my cot. Please cut it away. I can hear someone calling from under the floor."),
                local("lyra_clean_dressing", "Collect the sealed care packet", "Sealed Care Packet", 1, Quest.ObjectiveKind.GATHER, "village_mireford", 21, "quest_medical_supplies",
                        "Lyra: The marked packet contains clean cloth and water. Keep it sealed until the old rope is away from Eda's cot.",
                        "Collect the marked sealed care packet in Mireford.",
                        "You secured a sealed packet of clean cloth and water for Eda. It has not been used yet."),
                local("lyra_contaminated_rope", "Cut and contain the cot rope", "Contaminated Cot Rope", 1, Quest.ObjectiveKind.SEARCH, "village_mireford", 22, "quest_ward_marker",
                        "Eda consented to removing the rope. Use the cot station's shears and lidded box; keep the fibers away from the clean packet.",
                        "Cut and contain the marked rope at Eda's cot in Mireford.",
                        "The rope is cut and its loose fibers sealed in the cot station's box. The call beneath the cot has stopped. Eda still needs the clean dressing and water."),
                local("lyra_eda_treatment", "Give Eda the clean dressing and water", "Mireford Patient Eda", 1, Quest.ObjectiveKind.DELIVER, "village_mireford", 20, "npc_citizen_woman",
                        "The contaminated rope is contained. Give Eda the sealed care packet and help replace the dirty dressing.",
                        "Return to Eda with the sealed care packet and explicitly apply the clean dressing and water.",
                        "Eda: The clean cloth is on and I can drink again. The shaking is easing. I need rest; I am not ready to leave the cot."),
                local("lyra_eda_testimony", "Ask Eda about the rope shipment", "Mireford Patient Eda", 1, Quest.ObjectiveKind.TALK, "village_mireford", 20, "npc_citizen_woman",
                        "Eda is stable enough for a short conversation. Ask about the delivery without treating her account as proof of a curse.",
                        "Ask Eda who brought the new rope. Then discuss the next response with Lyra.",
                        "Eda: The rope came with the medicine crate, under the supplier's seal. The porter called it a blessing from the old infirmary. I saw the seal; I do not know where he got the rope.")));
        care.add(choice(patient, "We removed the rope and Eda accepted care. Decide what we do next for the other households, without treating a supplier's seal as proof of intent.",
                "Lyra: Eda has received care. The rope is contained, and her account gives us a lead to check. The other patients and the drowned infirmary remain unresolved."));
        replace(quests, patient.id, "Treat Eda's immediate illness, contain the suspect bell-rope, and hear her account before pursuing the supplier.", care);

        Quest rafiq = quests.get("rafiq_chain_2");
        List<Quest.QuestStage> restitution = new ArrayList<>(List.of(
                local("rafiq_household_water", "Collect the household's water", "Household Water Skin", 3, Quest.ObjectiveKind.GATHER, "village_dunewick", 20, "quest_water_skins",
                        "Rafiq: The family harmed by my duel needs water. Elder Safa will speak with us after the supplies arrive. Gather three filled skins at Dunewick's marked water station.",
                        "Collect three marked filled water skins in Dunewick for Safa's household.",
                        "Three filled water skins are secured for the household. Safa has not received them yet."),
                local("rafiq_household_herbs", "Collect the household's herb bundles", "Glassstep Herb Bundle", 2, Quest.ObjectiveKind.GATHER, "village_dunewick", 23, "quest_salve_herbs",
                        "Rafiq: The marked herb bundles were set aside for the household. Bring both with the water. An apology can wait until their needs are met.",
                        "Collect two marked glassstep herb bundles in Dunewick.",
                        "Both herb bundles are secured. The water and herbs still need to be handed to Safa."),
                local("rafiq_safa_handover", "Give Safa the household supplies", "Elder Safa", 1, Quest.ObjectiveKind.DELIVER, "village_dunewick", 25, "npc_citizen_woman",
                        "Safa has agreed to receive the supplies. Give them to her yourself; handing them to Rafiq does not deliver them to the household.",
                        "Give Elder Safa three filled water skins and two herb bundles at the marked Dunewick household.",
                        "Safa: Three skins and two bundles, all here. Thank you. This helps the household; it does not settle what happened in that duel."),
                local("rafiq_safa_testimony", "Hear Safa's account of the duel", "Elder Safa", 1, Quest.ObjectiveKind.TALK, "village_dunewick", 25, "npc_citizen_woman",
                        "The supplies have been delivered. Safa has offered an account of what she saw in the dueling mirror.",
                        "Ask Safa what she saw in the mirror, then return to Rafiq to discuss restitution.",
                        "Safa: I saw Rafiq turn toward the gate. The figure in the mirror still faced his opponent, with its sword raised. I cannot tell you how it happened. Ask the glassmaker before calling that an excuse.")));
        restitution.add(choice(rafiq, "Safa received the supplies and described the mirror. Choose Rafiq's next commitment to the household; her account does not erase his decision to flee.",
                "Rafiq: Safa has the supplies. We have her account of the mirror and a commitment to follow through on. I still owe them more than a convincing story."));
        replace(quests, rafiq.id, "Deliver water and herbs to the household harmed by Rafiq's duel, then hear what its elder saw in the enchanted mirror.", restitution);

        Quest clinic = quests.get("lyra_chain_6");
        List<Quest.QuestStage> supplies = new ArrayList<>();
        supplies.add(local("lyra_mobile_supplies", "Collect the sealed clinic bundles", "Sealed Clinic Bundle", 3, Quest.ObjectiveKind.GATHER, "city_belltower", 20, "quest_supply_cache",
                "Lyra: Mireford's bell warnings are failing, and its clinic needs supplies before the next flood cuts the route. Three sealed bundles are ready in Belltower.",
                "Collect three marked clinic bundles in Belltower, then clear the marked raiders on the supply route.",
                "Three sealed clinic bundles are secured. The route and the handover remain unfinished."));
        Quest.QuestStage road = clinic.stages.get(1);
        supplies.add(new Quest.QuestStage("lyra_mobile_road", "Defeat the supply-route raiders", road.target(), road.needed(), Quest.ObjectiveKind.DEFEAT,
                road.objectiveMapId(), road.objectiveLocationKind(), road.objectiveLocationIndex(), road.objectiveAsset(), road.monsterKey(), "", "",
                "Lyra: These raiders block the supply route. Deal with the three marked guards so the clinic bundles can reach Mireford.",
                "Defeat the three marked raiders on the clinic supply route.", "The three marked raiders are defeated. The clinic bundles still need to reach Mireford.", "The marked raiders are defeated."));
        supplies.add(local("lyra_mireford_handover", "Deliver the bundles to the clinic", "Mireford Attendant Sen", 1, Quest.ObjectiveKind.DELIVER, "village_mireford", 26, "npc_citizen_man",
                "Sen is keeping Mireford's cot station open. Hand the three bundles to him before reporting the clinic supplied.",
                "Give Sen all three sealed clinic bundles at Mireford's marked cot station.",
                "Sen: Three bundles received. Clean dressings, water vessels, and spare straps. We can restock the traveling kit. That will not stop the flooding or repair the warning bells."));
        supplies.add(choice(clinic, "The clinic has received its supplies. Agree with Lyra which calls the traveling team should answer first; the plan does not mean those visits have happened.",
                "Lyra: Sen has the supplies, and we have agreed how to prioritize the next visits. Mireford can prepare its kit while we investigate the drowned infirmary."));
        replace(quests, clinic.id, "Secure clinic supplies, defeat the marked raiders, and deliver the bundles to Mireford before planning further visits.", supplies);
    }

    public static String purpose(Quest q) {
        return switch (q.id) {
            case "seraphine_chain_2" -> "The renewed invitations may bind my family to Thorn Hall again. Iven remembers the original promise. I need your help giving him somewhere to speak without a collector at his door.";
            case "aria_chain_2" -> "Bren saw a traveler enter the hedge road. Her ribbon may be my sister's. Help me hear what he actually saw and arrange a place where the rider cannot corner him.";
            case "lyra_chain_3" -> "Eda is ill beside a cot tied with the suspect bell-rope. Help her first. Her symptoms and her account may tell us what needs checking at the drowned infirmary.";
            case "rafiq_chain_2" -> "I fled the duel and left this household with the consequences. Bring the supplies they need. Then we can ask Safa about the mirror without treating her help as something I am owed.";
            case "lyra_chain_6" -> "Mireford needs clean supplies before flooding closes the route. Deliver the bundles to its attendant so the traveling clinic has something to work with.";
            default -> "";
        };
    }

    public static String gatheredCargo(String stage) {
        return switch (stage) {
            case "lyra_clean_dressing" -> "care_packet";
            case "rafiq_household_water" -> "water_skin";
            case "rafiq_household_herbs" -> "herb_bundle";
            case "lyra_mobile_supplies" -> "clinic_bundle";
            default -> "";
        };
    }

    public static String cargoSummary(Quest q) {
        List<String> items = new ArrayList<>();
        q.cargo.forEach((key, count) -> items.add(count + " " + switch (key) {
            case "care_packet" -> "sealed care packet";
            case "water_skin" -> "filled water skins";
            case "herb_bundle" -> "glassstep herb bundles";
            case "clinic_bundle" -> "sealed clinic bundles";
            default -> key.replace('_', ' ');
        }));
        return items.isEmpty() ? "" : " Supplies held for this request: " + String.join(", ", items) + ".";
    }

    public static Map<String, Integer> requiredCargo(String stage) {
        return switch (stage) {
            case "lyra_eda_treatment" -> Map.of("care_packet", 1);
            case "rafiq_safa_handover" -> Map.of("water_skin", 3, "herb_bundle", 2);
            case "lyra_mireford_handover" -> Map.of("clinic_bundle", 3);
            default -> Map.of();
        };
    }

    public static boolean canHandOver(Quest q) {
        if (q.activeStage().id().equals("lyra_eda_treatment") && !q.observedStages.contains("lyra_contaminated_rope")) return false;
        return requiredCargo(q.activeStage().id()).entrySet().stream()
                .allMatch(e -> q.cargo.getOrDefault(e.getKey(), 0) >= e.getValue());
    }

    public static String exchangeLabel(Quest q) {
        return switch (q.activeStage().id()) {
            case "lyra_eda_treatment" -> "Apply Eda's clean dressing and give her the water.";
            case "rafiq_safa_handover" -> "Give Safa the three water skins and two herb bundles.";
            case "lyra_mireford_handover" -> "Give Sen the three sealed clinic bundles.";
            case "seraphine_iven_arrival", "aria_bren_arrival" -> "Confirm that you reached the refuge safely.";
            default -> "Discuss " + q.activeTarget() + " and record this exchange.";
        };
    }

    public static List<Npc> aftermath(Map<String, Quest> quests, WorldMap world, String mapId) {
        List<Npc> result = new ArrayList<>();
        addAftermath(result, quests, world, mapId, "seraphine_chain_2", "seraphine_iven_arrival", "city_riverside", 25, "Former Servant Iven",
                "Iven: I am still using the refuge. The guest charm keeps the collector outside; Thorn Hall's claim on my service remains unresolved.");
        addAftermath(result, quests, world, mapId, "aria_chain_2", "aria_bren_arrival", "village_oakhaven", 25, "Charcoal Burner Bren",
                "Bren: The kiln refuge has held. My account has not changed: I saw the ribbon, not the woman's face.");
        addAftermath(result, quests, world, mapId, "lyra_chain_3", "lyra_eda_testimony", "village_mireford", 20, "Mireford Patient Eda",
                "Eda: I am resting with clean dressings. The rope is contained. I have told you what I remember about its delivery.");
        addAftermath(result, quests, world, mapId, "rafiq_chain_2", "rafiq_safa_testimony", "village_dunewick", 25, "Elder Safa",
                "Safa: The water and herbs reached us. That matters. The family is still waiting to see how Rafiq follows through.");
        addAftermath(result, quests, world, mapId, "lyra_chain_6", "lyra_mireford_handover", "village_mireford", 26, "Mireford Attendant Sen",
                "Sen: The three bundles are here. We are preparing the traveling kit; the flood and the damaged warning bells still need attention.");
        return result;
    }

    private static void addAftermath(List<Npc> into, Map<String, Quest> quests, WorldMap world, String current,
                                     String id, String fact, String map, int slot, String name, String line) {
        Quest q = quests.get(id);
        if (!map.equals(current) || q == null || !q.observedStages.contains(fact)) return;
        // While a last report awaits turn-in, its temporary witness already occupies this spot.
        if (!q.completed && !q.ready() && q.activeObjectiveKind().conversationObjective() && q.activeTarget().equals(name)) return;
        TilePoint p = world.storyObjectivePoint(map, slot);
        String sprite = q.stages.stream().filter(stage -> stage.id().equals(fact))
                .map(Quest.QuestStage::objectiveAsset).findFirst().orElse("npc_citizen_woman");
        into.add(new Npc(map, name, sprite, p.x(), p.y(), List.of(line), null, null));
    }

    private static Quest.QuestStage local(String id, String title, String target, int needed, Quest.ObjectiveKind kind,
                                          String map, int slot, String asset, String offer, String instruction, String result) {
        String completed = switch (id) {
            case "seraphine_iven_arrival" -> "Seraphine: Iven reached the refuge and gave us his account of the one-winter promise. The guest charm shelters him. We still need to confront the terms that keep renewing his service.";
            case "aria_bren_arrival" -> "Aria: Bren reached the kiln refuge. He saw the ribbon, but not the woman's face. We have a reason to test the hedge path; we do not yet have proof of where my sister is.";
            default -> result;
        };
        return new Quest.QuestStage(id, title, target, needed, kind, map, "story", slot, asset, null, "", "", offer, instruction, result, completed);
    }

    private static Quest.QuestStage choice(Quest original, String prompt, String completed) {
        Quest.QuestStage c = original.stages.get(original.stages.size() - 1);
        return new Quest.QuestStage(c.id(), c.title(), c.target(), 1, Quest.ObjectiveKind.CHOICE,
                c.objectiveMapId(), c.objectiveLocationKind(), c.objectiveLocationIndex(), c.objectiveAsset(), c.monsterKey(), c.targetNpcId(), c.branchOutcomeKey(),
                prompt, prompt, "The next approach is agreed. It remains a commitment to act.", completed);
    }

    private static void replace(Map<String, Quest> quests, String id, String description, List<Quest.QuestStage> stages) {
        Quest q = quests.get(id);
        Quest.QuestStage first = stages.get(0);
        Quest replacement = new Quest(q.id, q.title, description, first.target(), first.needed(), q.rewardGold, q.rewardXp,
                first.objectiveKind(), first.objectiveMapId(), first.objectiveLocationKind(), first.objectiveLocationIndex(), first.objectiveAsset(), first.monsterKey(),
                first.startDialog(), first.progressDialog(), first.readyDialog(), stages.get(stages.size() - 1).completeDialog(),
                q.type, q.chainOwnerId, q.nextQuestId, first.targetNpcId(), first.branchOutcomeKey(), stages);
        replacement.contentRevision = 2;
        quests.put(id, replacement);
    }
}
