package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public final class DialogueLibrary {
    private DialogueLibrary() {
    }

    public static DialogueSession startSession(Npc npc, Quest quest, String biomeContext, int relationship, Random random) {
        return startSession(npc, quest, biomeContext, relationship, random, DialogueContext.empty());
    }

    public static DialogueSession startSession(
            Npc npc,
            Quest quest,
            String biomeContext,
            int relationship,
            Random random,
            DialogueContext context
    ) {
        return startSession(npc, quest, biomeContext, relationship, random, context, "");
    }

    public static DialogueSession startSession(Npc npc, Quest quest, String biomeContext, int relationship,
                                                Random random, DialogueContext context, String precedingReport) {
        DialogueContext dialogueContext = context == null ? DialogueContext.empty() : context;
        Map<String, DialogueNode> nodes = new LinkedHashMap<>();
        String greeting = greetingQuote(npc, random);
        Personality personality = personalityFor(npc);

        List<DialogueChoice> rootChoices = new ArrayList<>();
        CompanionVoice companionVoice = companionVoice(npc);
        boolean campaignSpeaker = MainStoryContent.isSpeaker(npc);
        if (companionVoice != null) {
            greeting = companionGreeting(companionVoice, quest, relationship, dialogueContext);
            List<DialogueChoice> personalChoices = new ArrayList<>();
            List<DialogueChoice> homeChoices = new ArrayList<>();
            addCompanionMilestoneTree(nodes, rootChoices, companionVoice, relationship, dialogueContext);
            if (quest != null && quest.companionQuest()) {
                addCompanionQuestTree(nodes, rootChoices, npc, companionVoice, quest, relationship, dialogueContext, precedingReport);
            }
            addCompanionRecruitmentTree(nodes, personalChoices, companionVoice, relationship, dialogueContext);
            addCompanionRelationshipTree(nodes, personalChoices, companionVoice, relationship, dialogueContext);
            addCompanionRomanceTree(nodes, personalChoices, companionVoice, relationship, dialogueContext);
            addCompanionFlirtTree(nodes, personalChoices, companionVoice, relationship, dialogueContext);
            addCompanionCheckInTree(nodes, personalChoices, companionVoice, relationship, dialogueContext);
            addCompanionValuesTree(nodes, personalChoices, companionVoice, relationship, dialogueContext);
            addCompanionSharedTableTree(nodes, homeChoices, companionVoice, relationship, dialogueContext);
            addCompanionOutcomeTree(nodes, personalChoices, companionVoice, relationship, dialogueContext);
            addCompanionMemoryTree(nodes, personalChoices, companionVoice, relationship, dialogueContext);
            addCompanionOpinionTree(nodes, personalChoices, companionVoice, relationship, dialogueContext);
            if (dialogueContext.recruited() || dialogueContext.stationedAtOathstead()) {
                addCompanionOathsteadTree(nodes, homeChoices, companionVoice, relationship, dialogueContext);
            }
            addCompanionPersonalHub(nodes, rootChoices, personalChoices, companionVoice, relationship, dialogueContext);
            addCompanionHomeHub(nodes, rootChoices, homeChoices, companionVoice, relationship, dialogueContext);
        }

        if (campaignSpeaker) {
            addCampaignTopics(nodes, rootChoices, "campaign_personal", MainStoryContent.personal(npc), quest,
                    List.of(new DialogueChoice("Back to topics", "root")));
            if (quest != null) addQuestOfferNodes(nodes, npc, quest, "quest", precedingReport);
        } else if (companionVoice == null) {
            String personal = pick(npcSpecificDialog(npc), random);
            if (!personal.isBlank()) {
                rootChoices.add(new DialogueChoice(personalTopicLabel(npc), "personal"));
                addNode(nodes, "personal", personal, List.of(
                        new DialogueChoice(personalWarmLabel(npc), "personal_more", 2),
                        new DialogueChoice(personalPushLabel(npc), "personal_push", -2),
                        new DialogueChoice("Back to topics", "root")
                ));
                addNode(nodes, "personal_more", personalFollowDialog(npc, random), backChoices());
                addNode(nodes, "personal_push", personalBoundaryResponse(npc), backChoices());
            }

            rootChoices.add(new DialogueChoice(personalityTopicLabel(npc), "personality"));
            addNode(nodes, "personality",
                    personalityLine(npc, personality),
                    List.of(
                            new DialogueChoice(personalityWarmLabel(personality), "personality_warm", 1),
                            new DialogueChoice(personalityChallengeLabel(personality), "personality_challenge", -1),
                            new DialogueChoice("Back to topics", "root")
                    ));
            addNode(nodes, "personality_warm", personalityWarmResponse(npc, personality), backChoices());
            addNode(nodes, "personality_challenge", personalityChallengeResponse(npc, personality), backChoices());

            String questLine = questDialog(quest, false);
            rootChoices.add(new DialogueChoice(workTopicLabel(npc, quest), "work"));
            if (!questLine.isBlank()) {
                addNode(nodes, "work", professionDialog(npc, random), List.of(
                        new DialogueChoice(questTopicLabel(quest), "quest", 0),
                        new DialogueChoice(craftRespectLabel(npc), "work_more", 1),
                        new DialogueChoice(craftDoubtLabel(npc), "work_doubt", -1),
                        new DialogueChoice("Back to topics", "root")
                ));
                addQuestOfferNodes(nodes, npc, quest, "quest", precedingReport);
            } else {
                addNode(nodes, "work", professionDialog(npc, random), List.of(
                        new DialogueChoice(craftRespectLabel(npc), "work_more", 1),
                        new DialogueChoice(craftDoubtLabel(npc), "work_doubt", -1),
                        new DialogueChoice("Back to topics", "root")
                ));
                rootChoices.add(new DialogueChoice(rumorTopicLabel(npc, biomeContext), "rumor"));
                addNode(nodes, "rumor", rumorDialog(biomeContext, random), List.of(
                        new DialogueChoice(rumorFollowLabel(npc, biomeContext), "rumor_more", 1),
                        new DialogueChoice(rumorDoubtLabel(npc, biomeContext), "rumor_doubt", -1),
                        new DialogueChoice("Back to topics", "root")
                ));
                addNode(nodes, "rumor_more", rumorFollowDialog(biomeContext, random), backChoices());
                addNode(nodes, "rumor_doubt", npc.name() + " shrugs, but the next story will not be offered so freely.", backChoices());
            }
            addNode(nodes, "work_more", professionFollowDialog(npc, random), backChoices());
            addNode(nodes, "work_doubt", professionDoubtResponse(npc), backChoices());
        }

        if (!campaignSpeaker) rootChoices.add(new DialogueChoice(localTopicLabel(npc, biomeContext), "local"));
        addNode(nodes, "local", biomeDialog(biomeContext, random), List.of(
                new DialogueChoice(localRespectLabel(npc, biomeContext), "local_more", 1),
                new DialogueChoice(localDismissLabel(npc, biomeContext), "local_dismiss", -1),
                new DialogueChoice("Back to topics", "root")
        ));
        addNode(nodes, "local_more", biomeFollowDialog(biomeContext, random), backChoices());
        addNode(nodes, "local_dismiss", "Local: " + npc.name() + " lets the warning drop. The land will make its own argument.", backChoices());

        if (quest != null) {
            if (companionVoice == null) {
                rootChoices.add(0, new DialogueChoice("Discuss " + quest.title + ".", "quest"));
            }
        }
        NpcBackstories.Profile history = NpcBackstories.profile(npc);
        if (history != null) {
            boolean unfamiliar = !dialogueContext.recruited() && relationship < 20
                    && (quest == null || !quest.completed && quest.id.equals(npc.questId()));
            if (companionVoice == null || unfamiliar) greeting = history.greeting();
            addCampaignTopics(nodes, rootChoices, "background", NpcBackstories.topics(npc, quest, relationship), quest,
                    List.of(new DialogueChoice("Back to topics", "root")));
        }
        addNode(nodes, "root", "\"" + greeting + "\"", rootChoices);
        return new DialogueSession(nodes, quest);
    }

    private static void addStoryBranchChoices(
            Map<String, DialogueNode> nodes,
            List<DialogueChoice> rootChoices,
            String prefix,
            StoryBranch storyBranch,
            Quest quest
    ) {
        for (int i = 0; i < storyBranch.options().size(); i++) {
            StoryOption option = storyBranch.options().get(i);
            String nodeId = prefix + "_" + i;
            rootChoices.add(new DialogueChoice(option.playerLine(), nodeId, option.relationshipDelta(), option.effect()));
            List<DialogueChoice> choices = new ArrayList<>();
            if (quest != null && !quest.accepted && !quest.completed) {
                choices.add(new DialogueChoice(questCommitLabel(quest), "story_branch_accept", 1, questAcceptEffect(quest)));
                choices.add(new DialogueChoice("I need more context first.", "story_branch_context", 0));
            }
            choices.addAll(backChoices());
            addNode(nodes, nodeId, option.npcResponse(), choices);
        }
        if (quest != null && !quest.accepted && !quest.completed) {
            addNode(nodes, "story_branch_accept", questDialog(quest, true), backChoices());
            addNode(nodes, "story_branch_context", questDialog(quest, false), List.of(
                    new DialogueChoice(questCommitLabel(quest), "story_branch_accept", 1, questAcceptEffect(quest)),
                    new DialogueChoice("Back to conversation", "root")
            ));
        }
    }

    private static StoryBranch branch(String opening, StoryOption... options) {
        return new StoryBranch(opening, List.of(options));
    }

    private static StoryOption option(String playerLine, String npcResponse, int relationshipDelta) {
        return option(playerLine, npcResponse, relationshipDelta, "");
    }

    private static StoryOption option(String playerLine, String npcResponse, int relationshipDelta, String effect) {
        return new StoryOption(playerLine, npcResponse, relationshipDelta, effect);
    }

    private static String companionGreeting(CompanionVoice voice, Quest quest, int relationship, DialogueContext context) {
        if (context.married()) {
            return voice.marriedGreeting();
        }
        if (context.romanced()) {
            return voice.romancedGreeting();
        }
        if (quest != null && quest.companionQuest() && !quest.completed) {
            return voice.questGreeting(quest);
        }
        if (context.stationedAtOathstead()) {
            return voice.oathsteadGreeting(context);
        }
        if (relationship >= 150) {
            return voice.loyalOpening();
        }
        if (relationship >= 90) {
            return voice.friendOpening();
        }
        if (relationship >= 50) {
            return voice.friendlyOpening();
        }
        if (relationship >= 20) {
            return voice.acquaintanceOpening();
        }
        return voice.guardedOpening();
    }

    private static List<CompanionChoiceOption> companionChoiceOptions(CompanionVoice voice, Quest quest) {
        if (quest != null && quest.id.equals("rafiq_chain_2")) return List.of(
                new CompanionChoiceOption("Give the glassmaker Safa's account, including your flight.", "truth", 0, DialogueIntent.QUEST_SUPPORT,
                        "Rafiq: I will tell the glassmaker what Safa saw and that I ran. A strange reflection does not excuse the part I know I played.", true),
                new CompanionChoiceOption("Ask the household what restitution would help them.", "mercy", 0, DialogueIntent.QUEST_SUPPORT,
                        "Rafiq: I will ask before deciding what they need. Today's supplies are delivered; any further restitution remains a promise.", true),
                new CompanionChoiceOption("Accept responsibility before seeking an explanation for the mirror.", "accountability", 0, DialogueIntent.QUEST_CHALLENGE,
                        "Rafiq: Agreed. I left them with the consequences. I will face that openly while we investigate the mirror.", false));
        if (quest != null && quest.id.equals("lyra_chain_3")) return List.of(
                new CompanionChoiceOption("Warn the cot attendants about the suspect rope.", "truth", 0, DialogueIntent.QUEST_SUPPORT,
                        "Lyra: I will warn them about contact with the fibers. We can state what happened to Eda without claiming we have proved the supplier intended it.", true),
                new CompanionChoiceOption("Prioritize clean supplies for the other patients.", "mercy", 0, DialogueIntent.QUEST_SUPPORT,
                        "Lyra: Eda has care. I will make the other patients our first priority. Their treatment has not happened merely because we agreed on it.", true),
                new CompanionChoiceOption("Trace the sealed shipment and preserve Eda's account.", "accountability", 0, DialogueIntent.QUEST_CHALLENGE,
                        "Lyra: We will question the supplier with Eda's account recorded. Care continues while we trace the shipment.", false));
        if (quest != null && quest.id.equals("lyra_chain_6")) return List.of(
                new CompanionChoiceOption("Check the bell warnings before choosing the clinic's route.", "truth", 0, DialogueIntent.QUEST_SUPPORT,
                        "Lyra: Sen has the bundles. We will verify which bells still carry reliable calls before planning visits from them.", true),
                new CompanionChoiceOption("Send the clinic toward the most seriously ill first.", "mercy", 0, DialogueIntent.QUEST_SUPPORT,
                        "Lyra: I will ask for current patient reports and prioritize the worst cases. Receiving the kit has prepared us; those visits are still ahead.", true),
                new CompanionChoiceOption("Keep a public list so isolated households are not skipped.", "accountability", 0, DialogueIntent.QUEST_CHALLENGE,
                        "Lyra: I will keep a list of requests and completed visits. The team should be answerable for who is still waiting.", false));
        if (voice == CompanionVoice.VESPER) {
            return vesperChoiceOptions(quest);
        }
        if (voice == CompanionVoice.SERAPHINE) {
            return seraphineChoiceOptions(quest);
        }
        if (voice == CompanionVoice.ARIA) {
            return ariaChoiceOptions(quest);
        }
        return List.of(
                new CompanionChoiceOption("The honest answer comes first.", "truth", 2, DialogueIntent.QUEST_SUPPORT,
                        "You choose truth, even where it will bruise.", true),
                new CompanionChoiceOption("Mercy comes first.", "mercy", 2, DialogueIntent.QUEST_SUPPORT,
                        "You choose mercy, but not forgetfulness.", true),
                new CompanionChoiceOption("There has to be accountability.", "accountability", 1, DialogueIntent.QUEST_CHALLENGE,
                        "You choose accountability before comfort.", false)
        );
    }

    private static String companionOfferReasonQuestion(CompanionVoice voice, Quest quest) {
        if (voice == CompanionVoice.VESPER) {
            return VesperDialogue.offerReasonQuestion();
        }
        return questReasonQuestion(quest);
    }

    private static String companionOfferPersonalQuestion(CompanionVoice voice, Quest quest) {
        if (voice == CompanionVoice.VESPER) {
            return VesperDialogue.offerPersonalQuestion();
        }
        return questPersonalQuestion(quest);
    }

    private static String companionOfferPracticalQuestion(CompanionVoice voice, Quest quest) {
        if (voice == CompanionVoice.VESPER) {
            return VesperDialogue.offerPracticalQuestion();
        }
        return practicalQuestLabel(quest);
    }

    private static String companionOfferPushQuestion(CompanionVoice voice) {
        if (voice == CompanionVoice.VESPER) {
            return VesperDialogue.offerPushQuestion();
        }
        return "Give me the version I can act on.";
    }

    private static String companionOfferClarifyFollowup(CompanionVoice voice) {
        if (voice == CompanionVoice.VESPER) {
            return VesperDialogue.offerClarifyFollowup();
        }
        return "And what does that cost you?";
    }

    private static String companionOfferPracticalFollowup(CompanionVoice voice, Quest quest) {
        if (voice == CompanionVoice.VESPER) {
            return VesperDialogue.offerPracticalFollowup();
        }
        return voice.questPracticalCommitLabel(quest);
    }

    private static List<CompanionChoiceOption> vesperChoiceOptions(Quest quest) {
        String outcomeKey = quest == null ? "" : quest.outcomeKey();
        return switch (outcomeKey) {
            case "vesper_first_root" -> List.of(
                    new CompanionChoiceOption("Name it as living, but dangerous.", "truth", 2, DialogueIntent.QUEST_SUPPORT,
                            "You name the waking root without softening it. Vesper nods, as if the truth has finally been given enough air.", true),
                    new CompanionChoiceOption("Protect Snowrest before the grove.", "protect", 1, DialogueIntent.QUEST_SUPPORT,
                            "You put the village first. Vesper accepts it without flinching, though her hand tightens around the seed bowl.", true),
                    new CompanionChoiceOption("Do not cut until we understand it.", "mercy", 2, DialogueIntent.QUEST_SUPPORT,
                            "You choose patience before steel. Vesper watches you like someone measuring whether hope has hands.", true)
            );
            case "vesper_practical_care" -> List.of(
                    new CompanionChoiceOption("Keep Snowrest's road open first.", "protect", 1, DialogueIntent.QUEST_SUPPORT,
                            "You choose the road and the people who need it. Vesper looks relieved that care can still have priorities.", true),
                    new CompanionChoiceOption("Cut only the black-sapped roots.", "accountability", 1, DialogueIntent.QUEST_CHALLENGE,
                            "You choose the cleanest harm: rot cut away, living roots spared. Vesper accepts the severity because it has limits.", true),
                    new CompanionChoiceOption("Follow the roots to the grove before deciding.", "truth", 2, DialogueIntent.QUEST_SUPPORT,
                            "You choose evidence before comfort. Vesper's silence warms by a degree.", true)
            );
            case "vesper_family_grove" -> List.of(
                    new CompanionChoiceOption("Call it a sacrifice, not innocence.", "truth", 1, DialogueIntent.QUEST_SUPPORT,
                            "You name the cost without polishing it clean. Vesper lets the word sacrifice hurt instead of hiding behind it.", true),
                    new CompanionChoiceOption("Snowrest was owed the truth.", "accountability", 1, DialogueIntent.QUEST_CHALLENGE,
                            "You choose accountability for the lie. Vesper does not defend her family, and that restraint costs her.", false),
                    new CompanionChoiceOption("Someone stayed so others could live.", "mercy", 2, DialogueIntent.QUEST_SUPPORT,
                            "You leave room for mercy without erasing the lock. Vesper breathes like grief has found a less cruel shape.", true)
            );
            case "vesper_buried_spring" -> List.of(
                    new CompanionChoiceOption("Halwen had a reason, not absolution.", "truth", 1, DialogueIntent.QUEST_SUPPORT,
                            "You separate reason from forgiveness. Vesper holds onto that distinction like a tool she needed years ago.", true),
                    new CompanionChoiceOption("He chose victims without asking them.", "accountability", 1, DialogueIntent.QUEST_CHALLENGE,
                            "You choose the people buried inside the decision. Vesper's agreement is quiet and sharp.", false),
                    new CompanionChoiceOption("Let the old keeper help repair it.", "mercy", 2, DialogueIntent.QUEST_SUPPORT,
                            "You choose mercy with work attached. Vesper accepts that kind; it does not ask memory to disappear.", true)
            );
            case "vesper_spring_return" -> List.of(
                    new CompanionChoiceOption("Let the grove set the pace.", "mercy", 2, DialogueIntent.QUEST_SUPPORT,
                            "You choose a slow return. Vesper watches the water as if patience has become visible.", true),
                    new CompanionChoiceOption("Open the spring to Snowrest carefully.", "protect", 2, DialogueIntent.QUEST_SUPPORT,
                            "You choose shared protection. Vesper hears the village inside the choice, not just the grove.", true),
                    new CompanionChoiceOption("Cut away every black root first.", "accountability", 1, DialogueIntent.QUEST_CHALLENGE,
                            "You choose clean boundaries before bloom. Vesper accepts the knife because you named where it must stop.", false)
            );
            case "vesper_oathstead_growth" -> List.of(
                    new CompanionChoiceOption("Stay only while staying remains a choice.", "truth", 2, DialogueIntent.QUEST_SUPPORT,
                            "You choose a home without chains. Vesper looks at Oathstead as if it has finally asked properly.", true),
                    new CompanionChoiceOption("Tend Oathstead before making symbols.", "protect", 2, DialogueIntent.QUEST_SUPPORT,
                            "You choose food, shade, and watering before banners. Vesper smiles almost unwillingly.", true),
                    new CompanionChoiceOption("Teach Snowrest what the root survived.", "mercy", 1, DialogueIntent.QUEST_SUPPORT,
                            "You choose memory that can be shared without becoming a sermon. Vesper lets that future stand.", true)
            );
            default -> List.of(
                    new CompanionChoiceOption("Read the evidence before judging.", "truth", 2, DialogueIntent.QUEST_SUPPORT,
                            "You choose evidence before comfort. Vesper trusts that more than certainty.", true),
                    new CompanionChoiceOption("Protect the people first.", "protect", 1, DialogueIntent.QUEST_SUPPORT,
                            "You choose the living people nearest the danger. Vesper accepts that care needs order.", true),
                    new CompanionChoiceOption("Cut only what is clearly rotten.", "accountability", 1, DialogueIntent.QUEST_CHALLENGE,
                            "You choose limits, not panic. Vesper respects the difference.", false)
            );
        };
    }

    private static List<CompanionChoiceOption> seraphineChoiceOptions(Quest quest) {
        String outcomeKey = quest == null ? "" : quest.outcomeKey();
        return switch (outcomeKey) {
            case "seraphine_first_lie" -> List.of(
                    new CompanionChoiceOption("Copy the clause before anyone sees it.", "clause_copied", 2, DialogueIntent.QUEST_SUPPORT,
                            "You choose leverage before theater. Seraphine watches the duplicate dry like it might finally tell the truth.", true),
                    new CompanionChoiceOption("Expose the false seal in court.", "public_record", 1, DialogueIntent.QUEST_SUPPORT,
                            "You choose the public record, loud enough that even Riverside has to pretend to hear it.", true),
                    new CompanionChoiceOption("Hide the evidence until the witness is safe.", "witness_first", 2, DialogueIntent.QUEST_SUPPORT,
                            "You choose the living witness before the beautiful proof. Seraphine's smile goes quiet for a breath.", true)
            );
            case "seraphine_clerk_truth" -> List.of(
                    new CompanionChoiceOption("Hide the clerk until his testimony can stand.", "witness_protected", 2, DialogueIntent.QUEST_SUPPORT,
                            "You choose shelter first, with locks, witnesses, and no praise for silence.", true),
                    new CompanionChoiceOption("Make his testimony public now.", "testimony_public", 1, DialogueIntent.QUEST_SUPPORT,
                            "You choose daylight before the court can buy another shadow.", true),
                    new CompanionChoiceOption("Trade his name for court access.", "leverage_traded", -1, DialogueIntent.QUEST_CHALLENGE,
                            "You choose leverage with a living person attached to it. Seraphine hears the usefulness and the danger.", false)
            );
            case "seraphine_family_debt" -> List.of(
                    new CompanionChoiceOption("Name desperation without calling it consent.", "survival_named", 2, DialogueIntent.QUEST_SUPPORT,
                            "You choose to name the trap without putting shame on the person caught in it.", true),
                    new CompanionChoiceOption("Call the contract legal, and still false.", "legal_lie_named", 2, DialogueIntent.QUEST_SUPPORT,
                            "You choose the hard sentence: lawful enough to wound, false enough to fight.", true),
                    new CompanionChoiceOption("Blame the family for signing.", "blame_signed", -2, DialogueIntent.QUEST_CHALLENGE,
                            "You choose blame where Seraphine expected understanding. The room gets colder than the old ink deserves.", false)
            );
            case "seraphine_red_notary" -> List.of(
                    new CompanionChoiceOption("Burn her contract and publish the ledger.", "ledger_published", 2, DialogueIntent.QUEST_SUPPORT,
                            "You choose smoke for the chain and daylight for the names still trapped in its links.", true),
                    new CompanionChoiceOption("Keep the records so victims can reclaim names.", "names_reclaimed", 3, DialogueIntent.QUEST_SUPPORT,
                            "You choose the messiest freedom: records guarded for the people they can still release.", true),
                    new CompanionChoiceOption("Trade the records for immediate safety.", "safety_bargain", -1, DialogueIntent.QUEST_CHALLENGE,
                            "You choose safety bought with leverage. Seraphine does not dismiss it, but she counts the cost twice.", false),
                    new CompanionChoiceOption("Burn everything before anyone else can use it.", "records_burned", -1, DialogueIntent.QUEST_CHALLENGE,
                            "You choose a clean fire. Seraphine watches the ash and worries about every name that still needed proof.", false)
            );
            case "seraphine_oathstead_promise" -> List.of(
                    new CompanionChoiceOption("Build the public ledger desk.", "refuge_ledger", 2, DialogueIntent.QUEST_SUPPORT,
                            "You choose a desk that answers chains with records people can actually use.", true),
                    new CompanionChoiceOption("Hold witness days before any oath is signed.", "witness_bench", 2, DialogueIntent.QUEST_SUPPORT,
                            "You choose witnesses before promises. Seraphine looks almost relieved by the order of operations.", true),
                    new CompanionChoiceOption("Leave the promise open; she chooses daily.", "chosen_daily", 3, DialogueIntent.QUEST_SUPPORT,
                            "You choose an unlocked promise, renewed only when Seraphine wants to renew it.", true)
            );
            default -> List.of(
                    new CompanionChoiceOption("The honest answer comes first.", "truth", 2, DialogueIntent.QUEST_SUPPORT,
                            "You choose truth, even where it will bruise.", true),
                    new CompanionChoiceOption("Mercy comes first.", "mercy", 2, DialogueIntent.QUEST_SUPPORT,
                            "You choose mercy, but not forgetfulness.", true),
                    new CompanionChoiceOption("There has to be accountability.", "accountability", 1, DialogueIntent.QUEST_CHALLENGE,
                            "You choose accountability before comfort.", false)
            );
        };
    }

    private static List<CompanionChoiceOption> ariaChoiceOptions(Quest quest) {
        String outcomeKey = quest == null ? "" : quest.outcomeKey();
        return switch (outcomeKey) {
            case "aria_false_trail_choice" -> List.of(
                    new CompanionChoiceOption("Chase the sister lead carefully.", "truth", 1, DialogueIntent.QUEST_SUPPORT,
                            "You choose the painful lead, but you do not let hope outrun proof.", true),
                    new CompanionChoiceOption("Hide the witness first.", "protect", 2, DialogueIntent.QUEST_SUPPORT,
                            "You choose the living witness before the answer Aria wants most. She flinches, then nods.", true),
                    new CompanionChoiceOption("Turn the false trail into a counter-ambush.", "accountability", 2, DialogueIntent.QUEST_CHALLENGE,
                            "You choose teeth and patience. Aria approves before she remembers to hide it.", false)
            );
            case "aria_oathstead_roadmarks" -> List.of(
                    new CompanionChoiceOption("Build a quiet road-scout post.", "road_scouts", 2, DialogueIntent.QUEST_SUPPORT,
                            "You choose watchers with patience instead of banners.", true),
                    new CompanionChoiceOption("Mark hidden trails for locals.", "hidden_markers", 2, DialogueIntent.QUEST_SUPPORT,
                            "You choose signs that help people who know to look and teach the rest to look twice.", true),
                    new CompanionChoiceOption("Prepare a trapline for false guides.", "counter_traps", 1, DialogueIntent.QUEST_CHALLENGE,
                            "You choose a road with teeth. Aria respects it, even while measuring the cost.", false)
            );
            case "aria_sister_truth" -> List.of(
                    new CompanionChoiceOption("Name the hope without forcing it.", "hope_named", 2, DialogueIntent.QUEST_SUPPORT,
                            "You choose hope with its hands open, not wrapped around Aria's throat.", true),
                    new CompanionChoiceOption("Protect the scout who survived.", "witness_protected", 2, DialogueIntent.QUEST_SUPPORT,
                            "You choose the survivor before the story. Aria looks hurt, then grateful enough to resent it.", true),
                    new CompanionChoiceOption("Follow the debt trail next.", "debt_pursued", 1, DialogueIntent.QUEST_CHALLENGE,
                            "You choose the harder road instead of the neat ending. Aria's mouth goes sharp and honest.", false)
            );
            default -> List.of(
                    new CompanionChoiceOption("Tell the truth plainly.", "truth", 2, DialogueIntent.QUEST_SUPPORT,
                            "You choose truth before comfort. Aria watches whether you protect anyone standing under it.", true),
                    new CompanionChoiceOption("Protect them first.", "protect", 2, DialogueIntent.QUEST_SUPPORT,
                            "You choose protection before the clean answer. Aria hears the cost and the care.", true),
                    new CompanionChoiceOption("Make the liar accountable.", "accountability", 1, DialogueIntent.QUEST_CHALLENGE,
                            "You choose consequence over quiet. Aria respects the teeth and checks for collateral.", false)
            );
        };
    }

    private static String companionQuestRootLabel(Quest quest) {
        if (quest == null) {
            return "What is happening here?";
        }
        return switch (questDialogueStage(quest)) {
            case OFFER -> "Tell me about " + quest.title + ".";
            case READY -> "I found what you needed.";
            case COMPLETED -> "What changed after " + quest.title + "?";
            case ACCEPTED, IN_PROGRESS -> "Where do things stand with " + quest.title + "?";
        };
    }

    private static String optionVariant(String seed, int salt, List<String> options) {
        if (options == null || options.isEmpty()) {
            return "";
        }
        int hash = seed == null || seed.isBlank() ? 0 : seed.hashCode();
        return options.get(Math.floorMod(hash + salt * 3571, options.size()));
    }

    private static String npcOptionSeed(Npc npc, String context) {
        String key = npc == null || npc.name() == null || npc.name().isBlank()
                ? "unknown"
                : npcDialogKey(npc);
        return key + "|" + context;
    }

    private static String biomeOptionSeed(Npc npc, String biomeContext, String context) {
        return npcOptionSeed(npc, context + "|" + (biomeContext == null ? "" : biomeContext));
    }

    private static String personalTopicLabel(Npc npc) {
        String name = shortNpcName(npc);
        return switch (npcProfession(npc)) {
            case "bartender" -> optionVariant(npcOptionSeed(npc, "personal_topic"), 1, List.of(
                    "What is your story, " + name + "?",
                    "What did this room teach you about people?",
                    "Who were you before everyone brought you their secrets?"));
            case "blacksmith" -> optionVariant(npcOptionSeed(npc, "personal_topic"), 2, List.of(
                    "What put the hammer in your hand?",
                    "What did the forge make of you?",
                    "Who taught you to trust heat and steel?"));
            case "baker" -> optionVariant(npcOptionSeed(npc, "personal_topic"), 3, List.of(
                    "What keeps your oven lit these days?",
                    "Who taught you that feeding people matters?",
                    "What did hunger teach you, " + name + "?"));
            case "merchant" -> optionVariant(npcOptionSeed(npc, "personal_topic"), 4, List.of(
                    "What did the road teach you, " + name + "?",
                    "What bargain changed you?",
                    "What price do you still remember?"));
            case "archivist" -> optionVariant(npcOptionSeed(npc, "personal_topic"), 5, List.of(
                    "What would your own record say?",
                    "Which page of yourself do you keep sealed?",
                    "What truth first made you careful?"));
            case "guard" -> optionVariant(npcOptionSeed(npc, "personal_topic"), 6, List.of(
                    "What keeps you standing watch?",
                    "Who taught you not to look away?",
                    "What does the watch cost you?"));
            case "healer" -> optionVariant(npcOptionSeed(npc, "personal_topic"), 7, List.of(
                    "Who taught you to keep people alive?",
                    "Which patient still follows you?",
                    "What does care take from you?"));
            case "farmer" -> optionVariant(npcOptionSeed(npc, "personal_topic"), 8, List.of(
                    "What keeps you rooted here?",
                    "What did this soil ask of you?",
                    "Who are you when the fields go quiet?"));
            case "scout" -> optionVariant(npcOptionSeed(npc, "personal_topic"), 9, List.of(
                    "What did the road do to you?",
                    "Which trail made you cautious?",
                    "What did you learn by coming back alive?"));
            case "quartermaster" -> optionVariant(npcOptionSeed(npc, "personal_topic"), 10, List.of(
                    "What do your ledgers not say?",
                    "What did supplies teach you about courage?",
                    "Which missing number still troubles you?"));
            default -> optionVariant(npcOptionSeed(npc, "personal_topic"), 11, List.of(
                    "Who are you when no one needs anything?",
                    "What should I know before I ask more of you?",
                    "What has this place made of you?"));
        };
    }

    private static String personalWarmLabel(Npc npc) {
        return switch (npcProfession(npc)) {
            case "guard" -> optionVariant(npcOptionSeed(npc, "personal_warm"), 1, List.of(
                    "I can listen without making it a report.",
                    "No orders. Just tell me what it was like.",
                    "You can set the pace. I will listen."));
            case "healer" -> optionVariant(npcOptionSeed(npc, "personal_warm"), 2, List.of(
                    "Tell me the part people forget to ask.",
                    "I can listen without asking you to be useful.",
                    "Who cares for you after all that care?"));
            case "archivist" -> optionVariant(npcOptionSeed(npc, "personal_warm"), 3, List.of(
                    "I want the version that did not fit the ledger.",
                    "No citations needed. Just you.",
                    "Tell me what the record could not hold."));
            case "scout" -> optionVariant(npcOptionSeed(npc, "personal_warm"), 4, List.of(
                    "I am listening. No trail signs, just you.",
                    "You came back. Start there.",
                    "Tell me the part no map kept."));
            case "merchant" -> optionVariant(npcOptionSeed(npc, "personal_warm"), 5, List.of(
                    "Tell me the honest price of that life.",
                    "No bargaining. What did it cost you?",
                    "I am not here to haggle with your grief."));
            default -> optionVariant(npcOptionSeed(npc, "personal_warm"), 6, List.of(
                    "I am listening. Take your time.",
                    "Say only what you want heard.",
                    "Start wherever the story lets you."));
        };
    }

    private static String personalPushLabel(Npc npc) {
        return switch (npcProfession(npc)) {
            case "guard" -> optionVariant(npcOptionSeed(npc, "personal_push"), 1, List.of(
                    "That sounds like the official version.",
                    "Who are you when the watch ends?",
                    "What are you not putting in the report?"));
            case "healer" -> optionVariant(npcOptionSeed(npc, "personal_push"), 2, List.of(
                    "You are leaving yourself out of the story.",
                    "That answer tends everyone except you.",
                    "What wound are you stepping around?"));
            case "archivist" -> optionVariant(npcOptionSeed(npc, "personal_push"), 3, List.of(
                    "What did the record leave out?",
                    "Which line did you refuse to write?",
                    "What did the official page protect?"));
            case "scout" -> optionVariant(npcOptionSeed(npc, "personal_push"), 4, List.of(
                    "That sounds like the safe trail.",
                    "Where does the map stop telling the truth?",
                    "What did you see and not mark?"));
            case "merchant" -> optionVariant(npcOptionSeed(npc, "personal_push"), 5, List.of(
                    "And what did it really cost you?",
                    "That sounds like the price, not the debt.",
                    "What did you sell that was not on the table?"));
            default -> optionVariant(npcOptionSeed(npc, "personal_push"), 6, List.of(
                    "There is more under that answer, isn't there?",
                    "That sounds like the version for strangers.",
                    "What are you protecting by saying it that way?"));
        };
    }

    private static String personalBoundaryResponse(Npc npc) {
        return switch (npcProfession(npc)) {
            case "guard" -> npc.name() + ": I know the difference between a question and an inspection. That one leaned close to inspection.";
            case "healer" -> npc.name() + ": Some wounds close better when people stop poking to see whether they are interesting.";
            case "archivist" -> npc.name() + ": Not every sealed page is hiding scandal. Some are simply waiting for permission.";
            case "scout" -> npc.name() + ": I mark safe paths for a living. That answer was one of them.";
            case "merchant" -> npc.name() + ": Bold bargaining. Unfortunately, that price is not posted.";
            case "farmer" -> npc.name() + ": Let a thing stay buried too long and you call it soil. Dig anyway and you may not like the roots.";
            case "quartermaster" -> npc.name() + ": That column stays closed for now. Even trust needs inventory control.";
            default -> npc.name() + ": Some doors do not open just because someone leans on them. Ask again when the hinges trust you.";
        };
    }

    private static String personalityTopicLabel(Npc npc) {
        return switch (npcProfession(npc)) {
            case "guard" -> optionVariant(npcOptionSeed(npc, "personality_topic"), 1, List.of(
                    "What do you need people to understand about duty?",
                    "What keeps duty from becoming cruelty?",
                    "What do you watch for in people?"));
            case "healer" -> optionVariant(npcOptionSeed(npc, "personality_topic"), 2, List.of(
                    "What does care cost you?",
                    "What makes mercy practical?",
                    "How do you keep caring without breaking?"));
            case "archivist" -> optionVariant(npcOptionSeed(npc, "personality_topic"), 3, List.of(
                    "What kind of truth earns your trust?",
                    "What makes a record worth keeping?",
                    "When does history become a warning?"));
            case "scout" -> optionVariant(npcOptionSeed(npc, "personality_topic"), 4, List.of(
                    "What makes you trust a road, or a person?",
                    "What does the trail reveal first?",
                    "What makes you turn back?"));
            case "merchant" -> optionVariant(npcOptionSeed(npc, "personality_topic"), 5, List.of(
                    "What makes a bargain feel honest to you?",
                    "What do people reveal when they trade?",
                    "What is worth more than coin?"));
            case "farmer" -> optionVariant(npcOptionSeed(npc, "personality_topic"), 6, List.of(
                    "What matters after the speeches end?",
                    "What has the land taught you about patience?",
                    "What do people forget until harvest?"));
            default -> optionVariant(npcOptionSeed(npc, "personality_topic"), 7, List.of(
                    "What matters most when things get ugly?",
                    "What do you trust when fear gets loud?",
                    "What should I understand about you?"));
        };
    }

    private static String personalityLine(Npc npc, Personality personality) {
        String key = personality == null ? "" : personality.name().toLowerCase(Locale.ROOT);
        if (key.contains("compassion") || key.contains("gentle")) {
            return npc.name() + ": People mistake care for softness because they only see it after danger passes. Care is deciding someone still matters while the room is on fire.";
        }
        if (key.contains("disciplined") || key.contains("steady") || key.contains("iron")) {
            return npc.name() + ": Duty is not bravery. Bravery comes and goes. Duty is what remains after fear has made a very reasonable argument.";
        }
        if (key.contains("curious") || key.contains("star")) {
            return npc.name() + ": Truth earns trust by surviving contact with ugly details. Anything too polished makes me check for fingerprints.";
        }
        if (key.contains("guarded") || key.contains("reserved") || key.contains("velvet")) {
            return npc.name() + ": I trust slowly because fast trust is how people put handles on you. Earned trust at least has the decency to leave marks.";
        }
        if (key.contains("practical") || key.contains("mud")) {
            return npc.name() + ": When things get ugly, I look for what still works. A warm meal, a dry bandage, a fixed hinge. Hope needs tools.";
        }
        if (key.contains("wild")) {
            return npc.name() + ": The land does not care whether a person sounds clever. It listens for patience, respect, and whether you step where you were warned not to.";
        }
        if (key.contains("watchful")) {
            return npc.name() + ": I trust the second answer more than the first. The first is often for strangers. The second forgets to pose.";
        }
        if (key.contains("exacting")) {
            return npc.name() + ": Clear numbers, clear promises, clear exits. People call that cold until the supplies run out.";
        }
        return switch (npcProfession(npc)) {
            case "bartender" -> npc.name() + ": People tell the truth sideways when their hands are warm and their cup is nearly empty. I have learned to listen sideways too.";
            case "blacksmith" -> npc.name() + ": Metal complains honestly. Heat, pressure, weakness; all of it shows. People would be easier if they sparked first.";
            case "baker" -> npc.name() + ": Bread teaches patience. Rush it and everyone tastes the mistake. People are not so different, though they deny it louder.";
            case "merchant" -> npc.name() + ": A bargain is only honest if both sides can still look at each other afterward. Anything else is robbery with nicer shoes.";
            case "archivist" -> npc.name() + ": Records do not make truth clean. They only stop it from changing clothes every time someone powerful enters the room.";
            case "guard" -> npc.name() + ": A quiet road is not a peaceful road until someone has watched it long enough to know the difference.";
            case "healer" -> npc.name() + ": Care is work. It has laundry, bad smells, shaking hands, and very little poetry when it matters most.";
            case "farmer" -> npc.name() + ": The ground does not applaud. It answers later, honestly, and usually after you have already worried yourself thin.";
            case "scout" -> npc.name() + ": I trust signs that do not know they are signs. Bent grass, fresh ash, silence in the wrong place.";
            case "quartermaster" -> npc.name() + ": The heroic part comes after someone counted rope, water, medicine, and how many fools forgot socks.";
            default -> npc.name() + ": I judge people by what they do when nobody is naming it courage.";
        };
    }

    private static String personalityWarmLabel(Personality personality) {
        String key = personality == null ? "" : personality.name().toLowerCase(Locale.ROOT);
        if (key.contains("disciplined") || key.contains("steady")) {
            return "I respect the weight you carry.";
        }
        if (key.contains("compassion") || key.contains("gentle")) {
            return "That kind of care takes strength.";
        }
        if (key.contains("curious") || key.contains("star")) {
            return "Careful questions are worth protecting.";
        }
        if (key.contains("guarded") || key.contains("reserved")) {
            return "I will not force the door open.";
        }
        return "I understand why that matters to you.";
    }

    private static String personalityWarmResponse(Npc npc, Personality personality) {
        String key = personality == null ? "" : personality.name().toLowerCase(Locale.ROOT);
        if (key.contains("disciplined") || key.contains("steady") || key.contains("iron")) {
            return npc.name() + ": Respect is useful if it keeps you listening after orders stop sounding clean.";
        }
        if (key.contains("compassion") || key.contains("gentle")) {
            return npc.name() + ": Then remember it when care looks ordinary. Ordinary is where most of the saving happens.";
        }
        if (key.contains("curious") || key.contains("star")) {
            return npc.name() + ": Good. A careful question can keep more people alive than a loud answer.";
        }
        if (key.contains("guarded") || key.contains("reserved") || key.contains("velvet")) {
            return npc.name() + ": That is a better beginning than most people offer. Doors prefer knocking to kicking.";
        }
        if (key.contains("practical") || key.contains("mud")) {
            return npc.name() + ": Understanding is pleasant. Showing up with both hands is better. I will accept both, in that order.";
        }
        return switch (npcProfession(npc)) {
            case "merchant" -> npc.name() + ": Fairly said. Words do not settle accounts, but they can keep the ink honest.";
            case "healer" -> npc.name() + ": Then help me make it practical. Kindness with clean hands and enough cloth.";
            case "guard" -> npc.name() + ": Good. Respect the watch before the road teaches you why it exists.";
            case "archivist" -> npc.name() + ": Careful. Understanding is the first draft. Evidence is the copy that survives.";
            default -> npc.name() + " lets the answer settle, guarded but not untouched by it.";
        };
    }

    private static String personalityChallengeLabel(Personality personality) {
        String key = personality == null ? "" : personality.name().toLowerCase(Locale.ROOT);
        if (key.contains("disciplined") || key.contains("steady")) {
            return "Duty can hide fear if you let it.";
        }
        if (key.contains("compassion") || key.contains("gentle")) {
            return "Care can become a chain too.";
        }
        if (key.contains("curious") || key.contains("star")) {
            return "Truth can still hurt people.";
        }
        if (key.contains("guarded") || key.contains("reserved")) {
            return "Distance can become its own lie.";
        }
        return "I am not sure that answer is enough.";
    }

    private static String personalityChallengeResponse(Npc npc, Personality personality) {
        String key = personality == null ? "" : personality.name().toLowerCase(Locale.ROOT);
        if (key.contains("disciplined") || key.contains("steady") || key.contains("iron")) {
            return npc.name() + ": Yes. That is why honest duty needs witnesses. Alone, it starts calling fear discipline.";
        }
        if (key.contains("compassion") || key.contains("gentle")) {
            return npc.name() + ": It can. That is why care has to include the person giving it, even when they are inconveniently still standing.";
        }
        if (key.contains("curious") || key.contains("star")) {
            return npc.name() + ": Truth hurts people most when it arrives late and proud. Better to bring it early and carry water.";
        }
        if (key.contains("guarded") || key.contains("reserved") || key.contains("velvet")) {
            return npc.name() + ": Distance can lie. So can closeness. I prefer the lie with fewer fingerprints on my throat.";
        }
        if (key.contains("practical") || key.contains("mud")) {
            return npc.name() + ": Enough? No. But it is a handle. You need a handle before you move weight.";
        }
        return switch (npcProfession(npc)) {
            case "merchant" -> npc.name() + ": Sharp question. Keep it sharp, then. Dull suspicion is just bad manners.";
            case "healer" -> npc.name() + ": Maybe not. But if we wait for perfect answers, people bleed while we admire the problem.";
            case "guard" -> npc.name() + ": Good. Do not trust any answer so much that you stop watching it.";
            case "archivist" -> npc.name() + ": Enough is a dangerous word. I prefer accurate, then useful, then survivable.";
            default -> npc.name() + " takes the challenge in, but their expression cools. The answer mattered enough to bruise.";
        };
    }

    private static String workTopicLabel(Npc npc, Quest quest) {
        if (quest != null && !quest.completed) {
            return optionVariant(npcOptionSeed(npc, "work_quest_" + quest.id), 1, List.of(
                    "What is happening with " + shortQuestTitle(quest) + "?",
                    "What does " + shortQuestTitle(quest) + " need from us?",
                    "Where does " + shortQuestTitle(quest) + " begin?"));
        }
        return switch (npcProfession(npc)) {
            case "bartender" -> optionVariant(npcOptionSeed(npc, "work_topic"), 1, List.of(
                    "What are people bringing to your counter?",
                    "What trouble came in thirsty?",
                    "What has the room been trying not to say?"));
            case "blacksmith" -> optionVariant(npcOptionSeed(npc, "work_topic"), 2, List.of(
                    "What is the forge telling you?",
                    "What keeps coming back broken?",
                    "What does the metal say about the road?"));
            case "baker" -> optionVariant(npcOptionSeed(npc, "work_topic"), 3, List.of(
                    "What does the bread know before we do?",
                    "What are people hungry enough to admit?",
                    "What keeps the ovens uneasy?"));
            case "merchant" -> optionVariant(npcOptionSeed(npc, "work_topic"), 4, List.of(
                    "What has trade made obvious lately?",
                    "What are people buying out of fear?",
                    "Which road has become too expensive?"));
            case "archivist" -> optionVariant(npcOptionSeed(npc, "work_topic"), 5, List.of(
                    "What are the records refusing to settle?",
                    "Which page has started troubling you?",
                    "What has history put back on the table?"));
            case "guard" -> optionVariant(npcOptionSeed(npc, "work_topic"), 6, List.of(
                    "What has the watch worried?",
                    "Which road is too quiet?",
                    "What are people pretending is normal?"));
            case "healer" -> optionVariant(npcOptionSeed(npc, "work_topic"), 7, List.of(
                    "What wounds keep coming back?",
                    "Who is arriving hurt in the same way?",
                    "What sickness has a pattern?"));
            case "farmer" -> optionVariant(npcOptionSeed(npc, "work_topic"), 8, List.of(
                    "What is the land warning you about?",
                    "What changed in the fields?",
                    "What did the animals notice first?"));
            case "scout" -> optionVariant(npcOptionSeed(npc, "work_topic"), 9, List.of(
                    "What did the trail show you?",
                    "Which route stopped feeling honest?",
                    "What did the road leave behind?"));
            case "quartermaster" -> optionVariant(npcOptionSeed(npc, "work_topic"), 10, List.of(
                    "What are the supplies saying?",
                    "Which shortage worries you most?",
                    "What does the ledger know before command does?"));
            default -> optionVariant(npcOptionSeed(npc, "work_topic"), 11, List.of(
                    "What work has your hands full?",
                    "What needs doing before it gets worse?",
                    "What has this place asked of you?"));
        };
    }

    private static String craftRespectLabel(Npc npc) {
        return switch (npcProfession(npc)) {
            case "bartender" -> optionVariant(npcOptionSeed(npc, "craft_respect"), 1, List.of(
                    "You hear more truth than most people admit.",
                    "A counter can become a witness stand.",
                    "People survive because someone remembers what they said."));
            case "blacksmith" -> optionVariant(npcOptionSeed(npc, "craft_respect"), 2, List.of(
                    "Good craft deserves respect.",
                    "A sound blade can be the difference between tale and tomb.",
                    "You make courage less likely to break."));
            case "baker" -> optionVariant(npcOptionSeed(npc, "craft_respect"), 3, List.of(
                    "Feeding people is not small work.",
                    "Bread can hold a village together.",
                    "A warm oven is a kind of promise."));
            case "merchant" -> optionVariant(npcOptionSeed(npc, "craft_respect"), 4, List.of(
                    "A fair trade can hold a town together.",
                    "Honest measures matter when fear is buying.",
                    "The road needs bargains people can survive."));
            case "archivist" -> optionVariant(npcOptionSeed(npc, "craft_respect"), 5, List.of(
                    "Records matter when memory gets frightened.",
                    "A kept name can be a rescue.",
                    "Truth needs someone stubborn enough to store it."));
            case "guard" -> optionVariant(npcOptionSeed(npc, "craft_respect"), 6, List.of(
                    "Keeping watch is harder than it looks.",
                    "A quiet night still costs someone sleep.",
                    "Standing there matters before anyone thanks you."));
            case "healer" -> optionVariant(npcOptionSeed(npc, "craft_respect"), 7, List.of(
                    "That work keeps people from becoming stories.",
                    "Care is courage with clean hands.",
                    "You fight endings most people only mourn."));
            case "farmer" -> optionVariant(npcOptionSeed(npc, "craft_respect"), 8, List.of(
                    "Useful work rarely gets enough songs.",
                    "Harvest is hope made practical.",
                    "Keeping people fed is its own kind of defense."));
            case "scout" -> optionVariant(npcOptionSeed(npc, "craft_respect"), 9, List.of(
                    "A good route can save more than a blade.",
                    "Coming back with warning is its own victory.",
                    "You make danger arrive with a name."));
            case "quartermaster" -> optionVariant(npcOptionSeed(npc, "craft_respect"), 10, List.of(
                    "Someone has to count what keeps us alive.",
                    "Supplies are hope with numbers attached.",
                    "You keep bravery from starving."));
            default -> optionVariant(npcOptionSeed(npc, "craft_respect"), 11, List.of(
                    "That work matters more than people notice.",
                    "Someone will survive because of that.",
                    "Ordinary work carries more than it shows."));
        };
    }

    private static String craftDoubtLabel(Npc npc) {
        return switch (npcProfession(npc)) {
            case "bartender" -> optionVariant(npcOptionSeed(npc, "craft_doubt"), 1, List.of(
                    "How do you know which stories to trust?",
                    "What if the room is lying to you?",
                    "Who pays for the rumors you repeat?"));
            case "blacksmith" -> optionVariant(npcOptionSeed(npc, "craft_doubt"), 2, List.of(
                    "Why work the metal that way?",
                    "What if the flaw is deeper than the blade?",
                    "What breaks first when fear takes hold?"));
            case "baker" -> optionVariant(npcOptionSeed(npc, "craft_doubt"), 3, List.of(
                    "What happens when the ovens go cold?",
                    "What if bread is not enough?",
                    "Who goes hungry first when trouble comes?"));
            case "merchant" -> optionVariant(npcOptionSeed(npc, "craft_doubt"), 4, List.of(
                    "Where does fair trade end and fear begin?",
                    "What if the price is hiding the threat?",
                    "Who cannot afford your truth?"));
            case "archivist" -> optionVariant(npcOptionSeed(npc, "craft_doubt"), 5, List.of(
                    "What if the record is protecting a lie?",
                    "Who benefits from that version?",
                    "What truth did the archive learn too late?"));
            case "guard" -> optionVariant(npcOptionSeed(npc, "craft_doubt"), 6, List.of(
                    "What if the watch is looking the wrong way?",
                    "Who watches the watchers when fear gets useful?",
                    "What if quiet has already become bait?"));
            case "healer" -> optionVariant(npcOptionSeed(npc, "craft_doubt"), 7, List.of(
                    "What if care is not enough this time?",
                    "What wound are you afraid to name?",
                    "Who do you lose when supplies run out?"));
            case "farmer" -> optionVariant(npcOptionSeed(npc, "craft_doubt"), 8, List.of(
                    "What if the land is past patience?",
                    "What did the fields stop forgiving?",
                    "What happens when harvest fails twice?"));
            case "scout" -> optionVariant(npcOptionSeed(npc, "craft_doubt"), 9, List.of(
                    "What if the safe route is the bait?",
                    "What sign do people ignore until it kills them?",
                    "What trail would you refuse today?"));
            case "quartermaster" -> optionVariant(npcOptionSeed(npc, "craft_doubt"), 10, List.of(
                    "What are your numbers not showing?",
                    "Where does the ledger lie by being tidy?",
                    "What shortage would break us first?"));
            default -> optionVariant(npcOptionSeed(npc, "craft_doubt"), 11, List.of(
                    "What worries you about doing it this way?",
                    "What are people refusing to notice?",
                    "What part of this work hurts most?"));
        };
    }

    private static String professionDoubtResponse(Npc npc) {
        return switch (npcProfession(npc)) {
            case "bartender" -> npc.name() + ": I do not trust stories. I trust who repeats them, who avoids them, and who suddenly pays in exact change.";
            case "blacksmith" -> npc.name() + ": Because metal remembers insult. Rush the heat and the blade forgives you right up until someone needs it.";
            case "baker" -> npc.name() + ": Then people get mean before they get hungry. An oven going cold is a town losing patience by the loaf.";
            case "merchant" -> npc.name() + ": Fair trade ends the moment one side cannot walk away. Fear is very good at blocking exits.";
            case "archivist" -> npc.name() + ": Then the record needs witnesses, not worship. Ink can lie. Margins usually tell on it.";
            case "guard" -> npc.name() + ": Then someone has to say it aloud before the wrong road becomes policy.";
            case "healer" -> npc.name() + ": Sometimes care is not enough. That is why I keep learning, keep asking, and keep hating easy comfort.";
            case "farmer" -> npc.name() + ": If the land is past patience, it tells you plainly. The trick is listening before it has to shout.";
            case "scout" -> npc.name() + ": If the safe route is bait, the bait will be too clean. Real roads are messier than traps.";
            case "quartermaster" -> npc.name() + ": Numbers miss panic, pride, and who will give away their last ration. So I count faces too.";
            default -> npc.name() + ": I can answer that, but not kindly. People mistake unfamiliar work for simple work all the time.";
        };
    }

    private static String rumorTopicLabel(Npc npc, String biomeContext) {
        return switch (biomeContext) {
            case "mountain" -> optionVariant(biomeOptionSeed(npc, biomeContext, "rumor_topic"), 1, List.of(
                    "What are people whispering about the pass?",
                    "What came down from the snow besides travelers?",
                    "Which mountain story has people counting doors?"));
            case "desert" -> optionVariant(biomeOptionSeed(npc, biomeContext, "rumor_topic"), 2, List.of(
                    "What rumor is crossing the dunes?",
                    "What story survived the heat?",
                    "What are the wells repeating?"));
            case "marsh" -> optionVariant(biomeOptionSeed(npc, biomeContext, "rumor_topic"), 3, List.of(
                    "What are the reeds repeating?",
                    "What story came in with wet boots?",
                    "What rumor keeps floating back?"));
            case "forest" -> optionVariant(biomeOptionSeed(npc, biomeContext, "rumor_topic"), 4, List.of(
                    "What story followed the trees into town?",
                    "What did the forest let someone overhear?",
                    "Which tale keeps returning at dusk?"));
            case "grave" -> optionVariant(biomeOptionSeed(npc, biomeContext, "rumor_topic"), 5, List.of(
                    "What are people afraid to say near the stones?",
                    "What rumor lowered its voice near the graves?",
                    "Which name keeps coming back wrong?"));
            default -> optionVariant(biomeOptionSeed(npc, biomeContext, "rumor_topic"), 6, List.of(
                    "What have people been afraid to say aloud?",
                    "What story has been moving faster than the road?",
                    "What rumor is everyone pretending not to hear?"));
        };
    }

    private static String rumorFollowLabel(Npc npc, String biomeContext) {
        return switch (biomeContext) {
            case "mountain" -> optionVariant(biomeOptionSeed(npc, biomeContext, "rumor_follow"), 1, List.of(
                    "Who came down from the pass with that?",
                    "Who heard it before the snow buried the tracks?",
                    "Did the story arrive alone?"));
            case "desert" -> optionVariant(biomeOptionSeed(npc, biomeContext, "rumor_follow"), 2, List.of(
                    "Who carried that story through the heat?",
                    "Which well heard it first?",
                    "Who spent water to tell it?"));
            case "marsh" -> optionVariant(biomeOptionSeed(npc, biomeContext, "rumor_follow"), 3, List.of(
                    "Where did the reeds first hear it?",
                    "Who came back muddy enough to know?",
                    "What part of it refuses to sink?"));
            case "forest" -> optionVariant(biomeOptionSeed(npc, biomeContext, "rumor_follow"), 4, List.of(
                    "Who heard it before dusk?",
                    "Did the trees repeat it, or did a person?",
                    "Where did the story leave the path?"));
            case "grave" -> optionVariant(biomeOptionSeed(npc, biomeContext, "rumor_follow"), 5, List.of(
                    "Who was brave enough to repeat it?",
                    "Which grave heard it first?",
                    "Did anyone check whether the number of names changed?"));
            default -> optionVariant(biomeOptionSeed(npc, biomeContext, "rumor_follow"), 6, List.of(
                    "Where did that story start?",
                    "Who gains if people believe it?",
                    "What detail stayed the same each time?"));
        };
    }

    private static String rumorDoubtLabel(Npc npc, String biomeContext) {
        return switch (biomeContext) {
            case "mountain" -> optionVariant(biomeOptionSeed(npc, biomeContext, "rumor_doubt"), 1, List.of(
                    "Snow makes liars dramatic.",
                    "Whiteouts can turn any fear into a witness.",
                    "That sounds like frost talking."));
            case "desert" -> optionVariant(biomeOptionSeed(npc, biomeContext, "rumor_doubt"), 2, List.of(
                    "Heat can make any story shimmer.",
                    "Mirages do not become truth by being thirsty.",
                    "The dunes love improving a tale."));
            case "marsh" -> optionVariant(biomeOptionSeed(npc, biomeContext, "rumor_doubt"), 3, List.of(
                    "The marsh fattens every rumor.",
                    "Mud makes everything sound deeper.",
                    "That sounds like fog with a witness."));
            case "forest" -> optionVariant(biomeOptionSeed(npc, biomeContext, "rumor_doubt"), 4, List.of(
                    "The woods add teeth to harmless stories.",
                    "Leaves whisper too much at dusk.",
                    "That sounds like branches learning drama."));
            case "grave" -> optionVariant(biomeOptionSeed(npc, biomeContext, "rumor_doubt"), 5, List.of(
                    "People invent voices around graves.",
                    "Grief can teach any bell to speak.",
                    "The dead get blamed for many living lies."));
            default -> optionVariant(biomeOptionSeed(npc, biomeContext, "rumor_doubt"), 6, List.of(
                    "That sounds like fear wearing a better coat.",
                    "People polish panic until it looks like news.",
                    "I need more than a frightened retelling."));
        };
    }

    private static String localTopicLabel(Npc npc, String biomeContext) {
        return switch (biomeContext) {
            case "mountain" -> optionVariant(biomeOptionSeed(npc, biomeContext, "local_topic"), 1, List.of(
                    "What should I respect before the pass kills me?",
                    "What does the snow punish first?",
                    "How do people survive this height?"));
            case "desert" -> optionVariant(biomeOptionSeed(npc, biomeContext, "local_topic"), 2, List.of(
                    "What keeps people alive in this heat?",
                    "What does the desert take from fools first?",
                    "What should I learn before the dunes teach me?"));
            case "marsh" -> optionVariant(biomeOptionSeed(npc, biomeContext, "local_topic"), 3, List.of(
                    "What does the marsh punish first?",
                    "Where does the mire trick outsiders?",
                    "What should I trust when the path moves?"));
            case "forest" -> optionVariant(biomeOptionSeed(npc, biomeContext, "local_topic"), 4, List.of(
                    "What should I know before I go deeper into the forest?",
                    "What mistakes do outsiders make here?",
                    "What should I watch for after dusk?"));
            case "grave" -> optionVariant(biomeOptionSeed(npc, biomeContext, "local_topic"), 5, List.of(
                    "What manners do the old stones expect?",
                    "What should I not say near the graves?",
                    "How do the living avoid insulting the dead?"));
            default -> optionVariant(biomeOptionSeed(npc, biomeContext, "local_topic"), 6, List.of(
                    "What does this place punish in strangers?",
                    "What warning do locals learn first?",
                    "What should I notice before the road notices me?"));
        };
    }

    private static String localRespectLabel(Npc npc, String biomeContext) {
        return switch (biomeContext) {
            case "mountain" -> optionVariant(biomeOptionSeed(npc, biomeContext, "local_respect"), 1, List.of(
                    "I will treat the pass like it has teeth.",
                    "I will listen before the snow has to shout.",
                    "I will keep pride below the treeline."));
            case "desert" -> optionVariant(biomeOptionSeed(npc, biomeContext, "local_respect"), 2, List.of(
                    "I will save bravado for somewhere cooler.",
                    "I will count water before courage.",
                    "I will distrust easy shade."));
            case "marsh" -> optionVariant(biomeOptionSeed(npc, biomeContext, "local_respect"), 3, List.of(
                    "I will watch the roots before the path.",
                    "I will step where the marsh has already forgiven weight.",
                    "I will trust quiet less than mud."));
            case "forest" -> optionVariant(biomeOptionSeed(npc, biomeContext, "local_respect"), 4, List.of(
                    "I will listen before I answer the trees.",
                    "I will not mistake green for harmless.",
                    "I will let the forest speak first."));
            case "grave" -> optionVariant(biomeOptionSeed(npc, biomeContext, "local_respect"), 5, List.of(
                    "I will keep my voice and my pride low.",
                    "I will bring manners before steel.",
                    "I will let the names remain names."));
            default -> optionVariant(biomeOptionSeed(npc, biomeContext, "local_respect"), 6, List.of(
                    "I will pay attention before I pay in blood.",
                    "I will learn the warning before testing it.",
                    "I will treat local fear as earned knowledge."));
        };
    }

    private static String localDismissLabel(Npc npc, String biomeContext) {
        return switch (biomeContext) {
            case "mountain" -> optionVariant(biomeOptionSeed(npc, biomeContext, "local_dismiss"), 1, List.of(
                    "I have crossed worse snow.",
                    "A pass is still only a road.",
                    "Cold does not make me cautious by itself."));
            case "desert" -> optionVariant(biomeOptionSeed(npc, biomeContext, "local_dismiss"), 2, List.of(
                    "Heat and mirages do not scare me.",
                    "Sand is not wisdom.",
                    "I will not bow to weather."));
            case "marsh" -> optionVariant(biomeOptionSeed(npc, biomeContext, "local_dismiss"), 3, List.of(
                    "Mud is mud. I can manage.",
                    "The mire can keep its tricks.",
                    "I have no patience for puddle omens."));
            case "forest" -> optionVariant(biomeOptionSeed(npc, biomeContext, "local_dismiss"), 4, List.of(
                    "Trees do not frighten me.",
                    "I am not here to argue with branches.",
                    "The woods can whisper to someone else."));
            case "grave" -> optionVariant(biomeOptionSeed(npc, biomeContext, "local_dismiss"), 5, List.of(
                    "Dead stones can keep their manners.",
                    "I do not take orders from graves.",
                    "The dead have had their turn."));
            default -> optionVariant(biomeOptionSeed(npc, biomeContext, "local_dismiss"), 6, List.of(
                    "I can handle the road.",
                    "Warnings are not always wisdom.",
                    "I will learn it my own way."));
        };
    }

    private static String companionQuestOutcomeLine(CompanionVoice voice, Quest quest, DialogueContext context) {
        String label = context.questOutcomeLabel(quest.outcomeKey());
        return voice.shortName() + " remembers that you chose " + label
                + ". That choice still affects how they understand what happened.";
    }

    private static void addCompanionRelationshipTree(
            Map<String, DialogueNode> nodes,
            List<DialogueChoice> rootChoices,
            CompanionVoice voice,
            int relationship,
            DialogueContext context
    ) {
        String rootId = "companion_trust";
        rootChoices.add(new DialogueChoice(relationshipTopicLabel(relationship, context), rootId));
        if (context.married()) {
            addNode(nodes, rootId, voice.reply(DialogueIntent.TRUST, relationship, context, voice.marriageOpening()), List.of(
                    new DialogueChoice("Come home to me when you can.", "companion_trust_warm", 2),
                    new DialogueChoice("Tell me what still frightens you.", "companion_trust_truth", 2),
                    new DialogueChoice("The work comes first today.", "companion_trust_mission", 0),
                    new DialogueChoice("Back to topics", "root")
            ));
        } else if (relationship >= 250) {
            addNode(nodes, rootId, voice.reply(DialogueIntent.FUTURE, relationship, context, voice.marriageOpening()), List.of(
                    new DialogueChoice("Marry me, " + voice.shortName() + ".", "companion_trust_proposal", 5, "marriage:accept"),
                    new DialogueChoice("Would you want a future with me?", "companion_trust_future", 2),
                    new DialogueChoice("One day, when the road is quieter.", "companion_trust_later", 1),
                    new DialogueChoice("I cannot promise forever.", "companion_trust_no", -1)
            ));
        } else if (context.romanced() || relationship >= 180) {
            addNode(nodes, rootId, voice.reply(DialogueIntent.ROMANCE, relationship, context, voice.lovingOpening()), List.of(
                    new DialogueChoice("I feel it too.", "companion_trust_romance", 3, "romance:start"),
                    new DialogueChoice("Stay with me a while.", "companion_trust_stay", 3, "romance:start"),
                    new DialogueChoice("This matters to me.", "companion_trust_warm", 2),
                    new DialogueChoice("We should keep this professional.", "companion_trust_no", -2, "romance:end")
            ));
        } else if (relationship >= 150) {
            addNode(nodes, rootId, voice.reply(DialogueIntent.TRUST, relationship, context, voice.loyalOpening()), List.of(
                    new DialogueChoice("I want you with me.", "companion_trust_stand", 2),
                    new DialogueChoice("Stay only if you choose to.", "companion_trust_choose", 2),
                    new DialogueChoice("I am glad you are here.", "companion_trust_warm", 1),
                    new DialogueChoice("I need your skills today.", "companion_trust_cold", -1)
            ));
        } else if (relationship >= 120) {
            addNode(nodes, rootId, voice.reply(DialogueIntent.TRUST, relationship, context, voice.trustedOpening()), List.of(
                    new DialogueChoice("Tell me the full truth.", "companion_trust_truth", 2),
                    new DialogueChoice("I will be careful with what you tell me.", "companion_trust_care", 2),
                    new DialogueChoice("You trust me that much?", "companion_trust_ask", 1),
                    new DialogueChoice("Would someone else be safer?", "companion_trust_doubt", -1)
            ));
        } else if (relationship >= 90) {
            addNode(nodes, rootId, voice.reply(DialogueIntent.TRUST, relationship, context, voice.friendOpening()), List.of(
                    new DialogueChoice("I am listening.", "companion_trust_listen", 2),
                    new DialogueChoice("Take your time.", "companion_trust_patient", 2),
                    new DialogueChoice("Does this change what we do next?", "companion_trust_mission", 1),
                    new DialogueChoice("Make it quick.", "companion_trust_rush", -1)
            ));
        } else if (relationship >= 50) {
            addNode(nodes, rootId, voice.reply(DialogueIntent.TRUST, relationship, context, voice.friendlyOpening()), List.of(
                    new DialogueChoice("Good to see you too.", "companion_trust_friendly", 2),
                    new DialogueChoice("You sound almost comfortable.", "companion_trust_play", 1),
                    new DialogueChoice("What changed?", "companion_trust_changed", 1),
                    new DialogueChoice("Stay alert.", "companion_trust_cold", -1)
            ));
        } else if (relationship >= 20) {
            addNode(nodes, rootId, voice.reply(DialogueIntent.TRUST, relationship, context, voice.acquaintanceOpening()), List.of(
                    new DialogueChoice("I will take that for now.", "companion_trust_useful", 1),
                    new DialogueChoice("I would rather earn trust.", "companion_trust_earn", 2),
                    new DialogueChoice("You keep your distance. Why?", "companion_trust_distance", 0),
                    new DialogueChoice("I do not need your trust.", "companion_trust_cold", -1)
            ));
        } else {
            addNode(nodes, rootId, voice.reply(DialogueIntent.TRUST, relationship, context, voice.guardedOpening()), List.of(
                    new DialogueChoice("Then tell me how to help.", "companion_trust_teach", 2),
                    new DialogueChoice("I am listening.", "companion_trust_listen", 1),
                    new DialogueChoice("I can handle myself.", "companion_trust_neutral", 0),
                    new DialogueChoice("I do not have time for this.", "companion_trust_cold", -1)
            ));
        }
        addNode(nodes, "companion_trust_proposal", voice.reply(DialogueIntent.FUTURE, relationship, context, voice.marriageAccept()), backChoices());
        addNode(nodes, "companion_trust_future", voice.reply(DialogueIntent.FUTURE, relationship, context, voice.marriageAsk()), backChoices());
        addNode(nodes, "companion_trust_later", voice.reply(DialogueIntent.FUTURE, relationship, context, voice.marriageLater()), backChoices());
        addNode(nodes, "companion_trust_romance", voice.reply(DialogueIntent.ROMANCE, relationship, context, voice.romanceAccept()), backChoices());
        addNode(nodes, "companion_trust_stay", voice.reply(DialogueIntent.ROMANCE, relationship, context, voice.romanceStay()), backChoices());
        addNode(nodes, "companion_trust_stand", voice.reply(DialogueIntent.TRUST, relationship, context, voice.loyalStand()), backChoices());
        addNode(nodes, "companion_trust_choose", voice.reply(DialogueIntent.TRUST, relationship, context, voice.loyalChoose()), backChoices());
        addNode(nodes, "companion_trust_truth", voice.reply(DialogueIntent.FEELING, relationship, context, voice.trustedTruth()), backChoices());
        addNode(nodes, "companion_trust_care", voice.reply(DialogueIntent.TRUST, relationship, context, voice.trustedCare()), backChoices());
        addNode(nodes, "companion_trust_ask", voice.reply(DialogueIntent.TRUST, relationship, context, voice.trustedAsk()), backChoices());
        addNode(nodes, "companion_trust_doubt", voice.reply(DialogueIntent.TRUST, relationship, context, voice.trustedDoubt()), backChoices());
        addNode(nodes, "companion_trust_listen", voice.reply(DialogueIntent.FEELING, relationship, context, voice.friendListen()), backChoices());
        addNode(nodes, "companion_trust_patient", voice.reply(DialogueIntent.FEELING, relationship, context, voice.friendPatient()), backChoices());
        addNode(nodes, "companion_trust_mission", voice.reply(DialogueIntent.NEXT_STEP, relationship, context, voice.friendMission()), backChoices());
        addNode(nodes, "companion_trust_rush", voice.reply(DialogueIntent.QUEST_CHALLENGE, relationship, context, voice.friendRush()), backChoices());
        addNode(nodes, "companion_trust_friendly", voice.reply(DialogueIntent.TRUST, relationship, context, voice.friendlyWarm()), backChoices());
        addNode(nodes, "companion_trust_play", voice.reply(DialogueIntent.TRUST, relationship, context, voice.friendlyPlay()), backChoices());
        addNode(nodes, "companion_trust_changed", voice.reply(DialogueIntent.TRUST, relationship, context, voice.friendlyAsk()), backChoices());
        addNode(nodes, "companion_trust_useful", voice.reply(DialogueIntent.TRUST, relationship, context, voice.acquaintUseful()), backChoices());
        addNode(nodes, "companion_trust_earn", voice.reply(DialogueIntent.TRUST, relationship, context, voice.acquaintTrust()), backChoices());
        addNode(nodes, "companion_trust_distance", voice.reply(DialogueIntent.TRUST, relationship, context, voice.acquaintDistance()), backChoices());
        addNode(nodes, "companion_trust_teach", voice.reply(DialogueIntent.TRUST, relationship, context, voice.guardedTeach()), backChoices());
        addNode(nodes, "companion_trust_neutral", voice.reply(DialogueIntent.TRUST, relationship, context, voice.guardedNeutral()), backChoices());
        addNode(nodes, "companion_trust_warm", voice.reply(DialogueIntent.ROMANCE, relationship, context, voice.romanceWarm()), backChoices());
        addNode(nodes, "companion_trust_cold", voice.reply(DialogueIntent.QUEST_CHALLENGE, relationship, context, voice.loyalCold()), backChoices());
        addNode(nodes, "companion_trust_no", voice.reply(DialogueIntent.ROMANCE, relationship, context, context.romanced() ? voice.romanceNo() : voice.marriageNo()), backChoices());
    }

    private static void addCompanionRecruitmentTree(
            Map<String, DialogueNode> nodes,
            List<DialogueChoice> personalChoices,
            CompanionVoice voice,
            int relationship,
            DialogueContext context
    ) {
        if (context.recruited() || !context.recruitmentAvailable()) {
            return;
        }
        String rootId = "companion_recruit";
        personalChoices.add(new DialogueChoice(voice.recruitmentRootLabel(), rootId));
        addNode(nodes, rootId, voice.reply(DialogueIntent.TRUST, relationship, context, voice.recruitmentOpening()), List.of(
                new DialogueChoice(voice.recruitmentRoadChoice(), "companion_recruit_road", 3, "recruit:" + voice.recruitId()),
                new DialogueChoice(voice.recruitmentOathsteadChoice(), "companion_recruit_oathstead", 2, "recruit:" + voice.recruitId()),
                new DialogueChoice("Not yet. I wanted to ask, not pressure you.", "companion_recruit_wait", 1),
                backToPersonal()
        ));
        addNode(nodes, "companion_recruit_road", voice.reply(DialogueIntent.TRUST, relationship, context, voice.recruitmentRoadAcceptLine()), companionBackChoices(voice));
        addNode(nodes, "companion_recruit_oathstead", voice.reply(DialogueIntent.HOME, relationship, context, voice.recruitmentOathsteadAcceptLine()), companionBackChoices(voice));
        addNode(nodes, "companion_recruit_wait", voice.reply(DialogueIntent.TRUST, relationship, context, voice.recruitmentWaitLine()), companionBackChoices(voice));
    }

    private static void addCompanionMilestoneTree(
            Map<String, DialogueNode> nodes,
            List<DialogueChoice> rootChoices,
            CompanionVoice voice,
            int relationship,
            DialogueContext context
    ) {
        if (context.pendingMilestoneTrust() <= 0) {
            return;
        }
        int threshold = context.pendingMilestoneTrust();
        String effect = "milestone:" + threshold;
        rootChoices.add(new DialogueChoice(milestoneRootPrompt(threshold), "companion_milestone"));
        addNode(nodes, "companion_milestone", voice.reply(milestoneIntent(threshold), relationship, context,
                voice.milestoneOpening(threshold, relationship, context)), List.of(
                new DialogueChoice("Tell me the honest version.", "companion_milestone_truth", 2, effect),
                new DialogueChoice(voice.milestoneWarmChoice(threshold), "companion_milestone_warm", 2, effect),
                new DialogueChoice(voice.milestoneBoundaryChoice(threshold), "companion_milestone_boundary", threshold >= 180 ? -1 : 0, effect),
                new DialogueChoice("Not now.", "root")
        ));
        addNode(nodes, "companion_milestone_truth", voice.reply(milestoneIntent(threshold), relationship, context,
                voice.milestoneTruthLine(threshold, relationship, context)), List.of(
                new DialogueChoice("I can carry that.", "companion_milestone_accept", 2),
                new DialogueChoice("That changes things.", "companion_milestone_change", 1),
                backToCompanion(voice)
        ));
        addNode(nodes, "companion_milestone_warm", voice.reply(milestoneIntent(threshold), relationship, context,
                voice.milestoneWarmLine(threshold, relationship, context)), List.of(
                new DialogueChoice("Stay close, then.", "companion_milestone_accept", 2),
                new DialogueChoice("We keep choosing this.", "companion_milestone_change", 1),
                backToCompanion(voice)
        ));
        addNode(nodes, "companion_milestone_boundary", voice.reply(milestoneIntent(threshold), relationship, context,
                voice.milestoneBoundaryLine(threshold, relationship, context)), List.of(
                new DialogueChoice("I will respect that.", "companion_milestone_accept", 1),
                new DialogueChoice("I need time too.", "companion_milestone_change", 0),
                backToCompanion(voice)
        ));
        addNode(nodes, "companion_milestone_accept", voice.reply(milestoneIntent(threshold), relationship, context,
                voice.milestoneAcceptLine(threshold)), companionBackChoices(voice));
        addNode(nodes, "companion_milestone_change", voice.reply(milestoneIntent(threshold), relationship, context,
                voice.milestoneChangeLine(threshold)), companionBackChoices(voice));
    }

    private static DialogueIntent milestoneIntent(int threshold) {
        if (threshold >= 250) {
            return DialogueIntent.FUTURE;
        }
        if (threshold >= 180) {
            return DialogueIntent.ROMANCE;
        }
        if (threshold >= 150) {
            return DialogueIntent.TRUST;
        }
        return DialogueIntent.FEELING;
    }

    private static void addCompanionPersonalHub(
            Map<String, DialogueNode> nodes,
            List<DialogueChoice> rootChoices,
            List<DialogueChoice> personalChoices,
            CompanionVoice voice,
            int relationship,
            DialogueContext context
    ) {
        if (personalChoices.isEmpty()) {
            return;
        }
        List<DialogueChoice> choices = new ArrayList<>(personalChoices);
        choices.add(backToCompanion(voice));
        rootChoices.add(new DialogueChoice("Can we talk, " + voice.shortName() + "?", "companion_personal"));
        addNode(nodes, "companion_personal", personalHubLine(voice, relationship, context), choices);
    }

    private static void addCompanionHomeHub(
            Map<String, DialogueNode> nodes,
            List<DialogueChoice> rootChoices,
            List<DialogueChoice> homeChoices,
            CompanionVoice voice,
            int relationship,
            DialogueContext context
    ) {
        if (homeChoices.isEmpty()) {
            return;
        }
        List<DialogueChoice> choices = new ArrayList<>(homeChoices);
        choices.add(backToCompanion(voice));
        rootChoices.add(new DialogueChoice(homeHubLabel(context), "companion_home"));
        addNode(nodes, "companion_home", homeHubLine(voice, relationship, context), choices);
    }

    private static String homeHubLabel(DialogueContext context) {
        if (context.sharedTableConversation()) {
            return "Stay with me a while.";
        }
        if (context.stationedAtOathstead()) {
            return "How does Oathstead feel to you?";
        }
        return "What does home mean to you now?";
    }

    private static String homeHubLine(CompanionVoice voice, int relationship, DialogueContext context) {
        if (context.sharedTableConversation() && (context.stationedAtOathstead() || context.recruited())) {
            return voice.shortName() + " talks with you about home, work, and what comes after the road.";
        }
        if (context.sharedTableConversation()) {
            return voice.shortName() + " settles into the quiet for a moment.";
        }
        if (relationship >= 120) {
            return voice.shortName() + " talks about Oathstead with cautious hope.";
        }
        return voice.shortName() + " considers Oathstead carefully before answering.";
    }

    private static String personalHubLine(CompanionVoice voice, int relationship, DialogueContext context) {
        if (context.married()) {
            return voice.shortName() + " gives you their full attention.";
        }
        if (context.romanced() || relationship >= 180) {
            return voice.shortName() + " waits for you to speak, careful but warm.";
        }
        if (relationship >= 120) {
            return voice.shortName() + " waits without rushing you.";
        }
        if (relationship >= 50) {
            return voice.shortName() + " studies your face, less guarded than before but not careless.";
        }
        return voice.shortName() + " keeps some distance, but does not turn away.";
    }

    private static void addCompanionRomanceTree(
            Map<String, DialogueNode> nodes,
            List<DialogueChoice> personalChoices,
            CompanionVoice voice,
            int relationship,
            DialogueContext context
    ) {
        if (!context.romanceMilestoneResolved() && !context.romanced() && relationship < 200) {
            return;
        }
        String rootId = "companion_romance";
        String label = context.sharedTableConversation() ? "Stay with me here." : "Could we find somewhere quiet together?";
        personalChoices.add(new DialogueChoice(label, rootId));
        String place = context.sharedTableLabel().isBlank() ? "somewhere quieter" : context.sharedTableLabel();
        addNode(nodes, rootId, voice.reply(DialogueIntent.ROMANCE, relationship, context,
                voice.romanceDateOpening(context, place)), List.of(
                new DialogueChoice(voice.flirtChoice(), "companion_romance_flirt", 2, context.romanced() ? "" : "romance:start"),
                new DialogueChoice("I wanted time with you, not another task.", "companion_romance_sincere", 2),
                new DialogueChoice(context.sharedTableConversation() ? "Stay here with me." : "Let us find an inn or Oathstead.", "companion_romance_place", 1,
                        context.sharedTableConversation() ? "romance:date" : "romance:date_plan"),
                new DialogueChoice("We can keep this gentle.", "companion_romance_boundary", 1),
                backToPersonal()
        ));
        addNode(nodes, "companion_romance_flirt", voice.reply(DialogueIntent.ROMANCE, relationship, context,
                voice.romanceDateFlirtLine(context)), List.of(
                new DialogueChoice(context.sharedTableConversation() ? "Then stay a while." : "Then hold that thought.", "companion_romance_place", 1,
                        context.sharedTableConversation() ? "romance:date" : "romance:date_plan"),
                backToPersonal()
        ));
        addNode(nodes, "companion_romance_sincere", voice.reply(DialogueIntent.ROMANCE, relationship, context,
                voice.romanceDateSincereLine(context)), List.of(
                new DialogueChoice("I mean it.", "companion_romance_place", 2,
                        context.sharedTableConversation() ? "romance:date" : "romance:date_plan"),
                backToPersonal()
        ));
        addNode(nodes, "companion_romance_place", voice.reply(DialogueIntent.ROMANCE, relationship, context,
                voice.romanceDatePlaceLine(context, place)), companionBackChoices(voice));
        addNode(nodes, "companion_romance_boundary", voice.reply(DialogueIntent.ROMANCE, relationship, context,
                voice.romanceDateBoundaryLine(context)), companionBackChoices(voice));
    }

    private static void addCompanionFlirtTree(
            Map<String, DialogueNode> nodes,
            List<DialogueChoice> personalChoices,
            CompanionVoice voice,
            int relationship,
            DialogueContext context
    ) {
        if (!context.romanced() && !context.romanceMilestoneResolved() && relationship < 150) {
            return;
        }
        String rootId = "companion_flirt";
        personalChoices.add(new DialogueChoice(voice.flirtRootLabel(context), rootId));
        addNode(nodes, rootId, voice.reply(DialogueIntent.ROMANCE, relationship, context,
                voice.flirtOpening(relationship, context)), List.of(
                new DialogueChoice(voice.flirtSoftChoice(context), "companion_flirt_soft", 2,
                        "dialogue_video:flirt:" + voice.recruitId()),
                new DialogueChoice(voice.flirtBoldChoice(context), "companion_flirt_bold", 2),
                new DialogueChoice("Too much?", "companion_flirt_boundary", 1),
                backToPersonal()
        ));
        addNode(nodes, "companion_flirt_soft", voice.reply(DialogueIntent.ROMANCE, relationship, context,
                voice.flirtSoftLine(context)), List.of(
                new DialogueChoice("I wanted you to know.", "companion_flirt_after", 1),
                backToPersonal()
        ));
        addNode(nodes, "companion_flirt_bold", voice.reply(DialogueIntent.ROMANCE, relationship, context,
                voice.flirtBoldLine(context)), List.of(
                new DialogueChoice("I can behave. Mostly.", "companion_flirt_after", 1),
                backToPersonal()
        ));
        addNode(nodes, "companion_flirt_boundary", voice.reply(DialogueIntent.ROMANCE, relationship, context,
                voice.flirtBoundaryLine(context)), companionBackChoices(voice));
        addNode(nodes, "companion_flirt_after", voice.reply(DialogueIntent.ROMANCE, relationship, context,
                voice.flirtAfterLine(context)), companionBackChoices(voice));
    }

    private static void addCompanionCheckInTree(
            Map<String, DialogueNode> nodes,
            List<DialogueChoice> rootChoices,
            CompanionVoice voice,
            int relationship,
            DialogueContext context
    ) {
        rootChoices.add(new DialogueChoice("How are you holding up?", "companion_checkin"));
        addNode(nodes, "companion_checkin", voice.reply(DialogueIntent.CONCERN, relationship, context,
                voice.checkInOpening(relationship, context)), List.of(
                new DialogueChoice("How are you feeling?", "companion_feeling", 1),
                new DialogueChoice("What should we do next?", "companion_next", 1),
                new DialogueChoice("Do you need anything?", "companion_need", 1),
                backToPersonal()
        ));
        addNode(nodes, "companion_feeling", voice.reply(DialogueIntent.FEELING, relationship, context,
                voice.feelingLine(relationship, context)), List.of(
                new DialogueChoice("I will not push.", "companion_feeling_respect", 1),
                new DialogueChoice("Tell me the honest version.", "companion_feeling_honest", 2),
                new DialogueChoice("We can keep moving.", "companion_feeling_mission", 0),
                backToPersonal()
        ));
        addNode(nodes, "companion_next", voice.reply(DialogueIntent.NEXT_STEP, relationship, context,
                voice.nextStepLine(relationship, context)), List.of(
                new DialogueChoice("That sounds right.", "companion_next_accept", 1),
                new DialogueChoice("What worries you about it?", "companion_next_worry", 2),
                new DialogueChoice("I had another plan.", "companion_next_disagree", -1),
                backToPersonal()
        ));
        addNode(nodes, "companion_need", voice.reply(DialogueIntent.NEED, relationship, context,
                voice.needLine(relationship, context)), List.of(
                new DialogueChoice("I can do that.", "companion_need_support", 1),
                new DialogueChoice("Ask me directly next time.", "companion_need_direct", 2),
                new DialogueChoice("We do not have time.", "companion_need_refuse", -1),
                backToPersonal()
        ));
        addNode(nodes, "companion_feeling_respect", voice.reply(DialogueIntent.FEELING, relationship, context, voice.feelingRespectResponse()), companionBackChoices(voice));
        addNode(nodes, "companion_feeling_honest", voice.reply(DialogueIntent.FEELING, relationship, context, voice.feelingHonestResponse(relationship)), companionBackChoices(voice));
        addNode(nodes, "companion_feeling_mission", voice.reply(DialogueIntent.NEXT_STEP, relationship, context, voice.feelingMissionResponse()), companionBackChoices(voice));
        addNode(nodes, "companion_next_accept", voice.reply(DialogueIntent.NEXT_STEP, relationship, context, voice.nextAcceptResponse()), companionBackChoices(voice));
        addNode(nodes, "companion_next_worry", voice.reply(DialogueIntent.QUEST_WARNING, relationship, context, voice.nextWorryResponse()), companionBackChoices(voice));
        addNode(nodes, "companion_next_disagree", voice.reply(DialogueIntent.QUEST_CHALLENGE, relationship, context, voice.nextDisagreeResponse()), companionBackChoices(voice));
        addNode(nodes, "companion_need_support", voice.reply(DialogueIntent.NEED, relationship, context, voice.needSupportResponse()), companionBackChoices(voice));
        addNode(nodes, "companion_need_direct", voice.reply(DialogueIntent.NEED, relationship, context, voice.needDirectResponse()), companionBackChoices(voice));
        addNode(nodes, "companion_need_refuse", voice.reply(DialogueIntent.NEED, relationship, context, voice.needRefuseResponse()), companionBackChoices(voice));
    }

    private static void addCompanionValuesTree(
            Map<String, DialogueNode> nodes,
            List<DialogueChoice> rootChoices,
            CompanionVoice voice,
            int relationship,
            DialogueContext context
    ) {
        rootChoices.add(new DialogueChoice("How do you see me?", "companion_values"));
        addNode(nodes, "companion_values", voice.reply(DialogueIntent.VALUES, relationship, context,
                voice.valuesOpening(relationship, context)), List.of(
                new DialogueChoice("What do you trust in me?", "companion_values_trust", 2),
                new DialogueChoice("What worries you about me?", "companion_values_worry", 1),
                new DialogueChoice("What should I remember about you?", "companion_values_request", 1),
                backToPersonal()
        ));
        addNode(nodes, "companion_values_trust", voice.reply(DialogueIntent.VALUES, relationship, context, voice.valuesTrustLine(relationship, context)), companionBackChoices(voice));
        addNode(nodes, "companion_values_worry", voice.reply(DialogueIntent.QUEST_WARNING, relationship, context, voice.valuesWorryLine(relationship, context)), companionBackChoices(voice));
        addNode(nodes, "companion_values_request", voice.reply(DialogueIntent.REQUEST, relationship, context, voice.valuesRequestLine(relationship, context)), List.of(
                new DialogueChoice("I will remember that.", "companion_values_request_accept", 2, "companion_request:accept"),
                new DialogueChoice("I cannot promise perfectly.", "companion_values_request_honest", 1),
                new DialogueChoice("That is too much to ask.", "companion_values_request_refuse", -1)
        ));
        addNode(nodes, "companion_values_request_accept", voice.reply(DialogueIntent.REQUEST, relationship, context, voice.requestAcceptedLine(context)), companionBackChoices(voice));
        addNode(nodes, "companion_values_request_honest", voice.reply(DialogueIntent.REQUEST, relationship, context, voice.requestHonestLine()), companionBackChoices(voice));
        addNode(nodes, "companion_values_request_refuse", voice.reply(DialogueIntent.REQUEST, relationship, context, voice.requestRefusedLine()), companionBackChoices(voice));
    }

    private static void addCompanionSharedTableTree(
            Map<String, DialogueNode> nodes,
            List<DialogueChoice> rootChoices,
            CompanionVoice voice,
            int relationship,
            DialogueContext context
    ) {
        if (!context.sharedTableConversation()) {
            return;
        }
        rootChoices.add(new DialogueChoice("Sit with me for a while.", "companion_table"));
        addNode(nodes, "companion_table", voice.reply(DialogueIntent.HOME, relationship, context,
                voice.sharedTableOpening(relationship, context)), List.of(
                new DialogueChoice("Stay with me a while.", "companion_table_stay", 2),
                new DialogueChoice("Do you need something from me?", "companion_table_request", 1),
                new DialogueChoice("What comes after all this?", "companion_table_after", 1),
                backToHome()
        ));
        addNode(nodes, "companion_table_stay", voice.reply(DialogueIntent.HOME, relationship, context,
                voice.sharedTableStayLine(relationship, context)), companionBackChoices(voice));
        addNode(nodes, "companion_table_request", voice.reply(DialogueIntent.REQUEST, relationship, context,
                voice.sharedTableRequestLine(relationship, context)), List.of(
                new DialogueChoice("I can do that.", "companion_table_request_yes", 2, "companion_request:accept"),
                new DialogueChoice("Remind me when it matters.", "companion_table_request_later", 1),
                new DialogueChoice("Not tonight.", "companion_table_request_no", -1)
        ));
        addNode(nodes, "companion_table_after", voice.reply(DialogueIntent.FUTURE, relationship, context,
                voice.epilogueLine(relationship, context)), List.of(
                new DialogueChoice("I want you there for it.", "companion_table_after_together", 2),
                new DialogueChoice("We will earn that future.", "companion_table_after_earn", 1),
                new DialogueChoice("Survive first.", "companion_table_after_survive", 0)
        ));
        addNode(nodes, "companion_table_request_yes", voice.reply(DialogueIntent.REQUEST, relationship, context, voice.requestAcceptedLine(context)), companionBackChoices(voice));
        addNode(nodes, "companion_table_request_later", voice.reply(DialogueIntent.REQUEST, relationship, context, voice.requestHonestLine()), companionBackChoices(voice));
        addNode(nodes, "companion_table_request_no", voice.reply(DialogueIntent.REQUEST, relationship, context, voice.requestRefusedLine()), companionBackChoices(voice));
        addNode(nodes, "companion_table_after_together", voice.reply(DialogueIntent.FUTURE, relationship, context, voice.epilogueTogetherLine(context)), companionBackChoices(voice));
        addNode(nodes, "companion_table_after_earn", voice.reply(DialogueIntent.FUTURE, relationship, context, voice.epilogueEarnLine()), companionBackChoices(voice));
        addNode(nodes, "companion_table_after_survive", voice.reply(DialogueIntent.FUTURE, relationship, context, voice.epilogueSurviveLine()), companionBackChoices(voice));
    }

    private static void addCompanionOpinionTree(
            Map<String, DialogueNode> nodes,
            List<DialogueChoice> rootChoices,
            CompanionVoice voice,
            int relationship,
            DialogueContext context
    ) {
        rootChoices.add(new DialogueChoice("What do you think of...?", "companion_opinion"));
        List<DialogueChoice> choices = new ArrayList<>();
        choices.add(new DialogueChoice("Me, honestly.", "companion_opinion_player", 1));
        int index = 0;
        for (String knownName : context.knownCompanionNames()) {
            CompanionVoice subject = companionVoice(knownName);
            if (subject == null || subject == voice) {
                continue;
            }
            String nodeId = "companion_opinion_known_" + index++;
            choices.add(new DialogueChoice(subject.shortName(), nodeId, 0));
            addNode(nodes, nodeId, voice.reply(DialogueIntent.OPINION, relationship, context,
                    voice.opinionAbout(subject)), backChoices());
        }
        choices.add(backToPersonal());
        addNode(nodes, "companion_opinion", voice.reply(DialogueIntent.OPINION, relationship, context,
                "Choose whose name you bring into the space between you."), choices);
        addNode(nodes, "companion_opinion_player", voice.reply(DialogueIntent.OPINION, relationship, context,
                voice.opinionAboutPlayer(context)), List.of(
                new DialogueChoice("That is fair.", "companion_opinion_player_accept", 1),
                new DialogueChoice("You notice more than I thought.", "companion_opinion_player_warm", 2),
                backToPersonal()
        ));
        addNode(nodes, "companion_opinion_player_accept", voice.reply(DialogueIntent.OPINION, relationship, context,
                "That is fair because it was meant as a true answer, not a flattering one."), companionBackChoices(voice));
        addNode(nodes, "companion_opinion_player_warm", voice.reply(DialogueIntent.OPINION, relationship, context,
                "You notice the noticing. That makes the road feel less like errands and more like company."), companionBackChoices(voice));
    }

    private static void addCompanionMemoryTree(
            Map<String, DialogueNode> nodes,
            List<DialogueChoice> rootChoices,
            CompanionVoice voice,
            int relationship,
            DialogueContext context
    ) {
        if (context.recentMemoryTexts().isEmpty()) {
            return;
        }
        rootChoices.add(new DialogueChoice("Do you remember what happened?", "companion_memory"));
        List<DialogueChoice> choices = new ArrayList<>();
        for (int i = 0; i < context.recentMemoryTexts().size(); i++) {
            String memory = context.recentMemoryTexts().get(i);
            String nodeId = "companion_memory_" + i;
            choices.add(new DialogueChoice(memoryTopicLabel(memory), nodeId, 1));
            addNode(nodes, nodeId, voice.reply(DialogueIntent.MEMORY, relationship, context,
                    voice.memoryReflection(memory)), List.of(
                    new DialogueChoice("I remember it too.", "companion_memory_shared", 2),
                    new DialogueChoice("We keep moving from there.", "companion_memory_forward", 1),
                    backToPersonal()
            ));
        }
        choices.add(backToPersonal());
        addNode(nodes, "companion_memory", voice.reply(DialogueIntent.MEMORY, relationship, context,
                voice.memoryOpening()), choices);
        addNode(nodes, "companion_memory_shared", voice.reply(DialogueIntent.MEMORY, relationship, context,
                voice.memorySharedResponse()), companionBackChoices(voice));
        addNode(nodes, "companion_memory_forward", voice.reply(DialogueIntent.MEMORY, relationship, context,
                voice.memoryForwardResponse()), companionBackChoices(voice));
    }

    private static void addCompanionOutcomeTree(
            Map<String, DialogueNode> nodes,
            List<DialogueChoice> personalChoices,
            CompanionVoice voice,
            int relationship,
            DialogueContext context
    ) {
        List<CompanionOutcomeTopic> topics = companionOutcomeTopics(voice, context);
        if (topics.isEmpty()) {
            return;
        }
        String rootId = "companion_outcomes";
        personalChoices.add(new DialogueChoice(voice.outcomeMemoryRootLabel(), rootId));
        List<DialogueChoice> choices = new ArrayList<>();
        for (int i = 0; i < topics.size(); i++) {
            CompanionOutcomeTopic topic = topics.get(i);
            String nodeId = "companion_outcome_" + i;
            choices.add(new DialogueChoice(topic.label(), nodeId, 1));
            addNode(nodes, nodeId, voice.reply(DialogueIntent.MEMORY, relationship, context,
                    voice.outcomeMemoryLine(topic.outcomeKey(), context)), List.of(
                    new DialogueChoice("I remember why we chose that.", nodeId + "_shared", 2),
                    new DialogueChoice("Would you choose differently now?", nodeId + "_question", 1),
                    new DialogueChoice("Back to remembered choices", rootId)
            ));
            addNode(nodes, nodeId + "_shared", voice.reply(DialogueIntent.MEMORY, relationship, context,
                    voice.outcomeMemorySharedLine(topic.outcomeKey(), context)), companionBackChoices(voice));
            addNode(nodes, nodeId + "_question", voice.reply(DialogueIntent.MEMORY, relationship, context,
                    voice.outcomeMemoryQuestionLine(topic.outcomeKey(), context)), companionBackChoices(voice));
        }
        choices.add(backToPersonal());
        addNode(nodes, rootId, voice.reply(DialogueIntent.MEMORY, relationship, context,
                voice.outcomeMemoryOpening(context)), choices);
    }

    private static List<CompanionOutcomeTopic> companionOutcomeTopics(CompanionVoice voice, DialogueContext context) {
        List<CompanionOutcomeTopic> topics = new ArrayList<>();
        switch (voice) {
            case SERAPHINE -> {
                addOutcomeTopic(topics, context, "seraphine_first_lie", "The first lie");
                addOutcomeTopic(topics, context, "seraphine_clerk_truth", "The hidden clerk");
                addOutcomeTopic(topics, context, "seraphine_family_debt", "Vale family debt");
                addOutcomeTopic(topics, context, "seraphine_red_notary", "The Red Notary");
                addOutcomeTopic(topics, context, "seraphine_oathstead_promise", "Her chosen promise");
            }
            case MAERA -> {
                addOutcomeTopic(topics, context, "maera_first_truth", "The first citation");
                addOutcomeTopic(topics, context, "maera_moving_map", "The moving map");
                addOutcomeTopic(topics, context, "maera_archive_warning", "The archive warning");
                addOutcomeTopic(topics, context, "maera_cult_pages", "The cult pages");
                addOutcomeTopic(topics, context, "maera_family_correction", "The family correction");
                addOutcomeTopic(topics, context, "maera_vault_truth", "The vault truth");
                addOutcomeTopic(topics, context, "maera_oathstead_archive", "Oathstead's archive");
            }
            case CASSIA -> {
                addOutcomeTopic(topics, context, "cassia_gate_memory", "The gate memory");
                addOutcomeTopic(topics, context, "cassia_frost_road", "The frost road");
                addOutcomeTopic(topics, context, "cassia_survivor_truth", "The survivor's truth");
                addOutcomeTopic(topics, context, "cassia_blue_steel_orders", "Blue Steel orders");
                addOutcomeTopic(topics, context, "cassia_varran_truth", "Varran's truth");
                addOutcomeTopic(topics, context, "cassia_old_gate", "The old gate");
                addOutcomeTopic(topics, context, "cassia_ironwall_trial", "The Ironwall trial");
                addOutcomeTopic(topics, context, "cassia_oathstead_duty", "Oathstead duty");
            }
            case LYRA -> {
                addOutcomeTopic(topics, context, "lyra_first_triage", "The first triage");
                addOutcomeTopic(topics, context, "lyra_names_on_cot", "Clean-labeled crates");
                addOutcomeTopic(topics, context, "lyra_false_medicine", "False medicine");
                addOutcomeTopic(topics, context, "lyra_antidote_rule", "The antidote rule");
                addOutcomeTopic(topics, context, "lyra_first_patient", "The first patient");
                addOutcomeTopic(topics, context, "lyra_moving_care", "Moving care");
                addOutcomeTopic(topics, context, "lyra_fever_cure", "The fever cure");
                addOutcomeTopic(topics, context, "lyra_oathstead_clinic", "Oathstead clinic");
            }
            case ARIA -> {
                addOutcomeTopic(topics, context, "aria_ambush_lesson", "The ambush lesson");
                addOutcomeTopic(topics, context, "aria_false_trail_choice", "The false trail choice");
                addOutcomeTopic(topics, context, "aria_oathstead_roadmarks", "Oathstead road marks");
                addOutcomeTopic(topics, context, "aria_sister_truth", "Her sister's trail");
                addOutcomeTopic(topics, context, "aria_oathstead_future", "The road ahead");
            }
            case VESPER -> {
                addOutcomeTopics(topics, context, VesperDialogue.outcomeTopics());
            }
            default -> {
            }
        }
        return topics;
    }

    private static void addOutcomeTopic(
            List<CompanionOutcomeTopic> topics,
            DialogueContext context,
            String outcomeKey,
            String label
    ) {
        if (!context.questOutcome(outcomeKey).isBlank()) {
            topics.add(new CompanionOutcomeTopic(outcomeKey, label));
        }
    }

    private static void addOutcomeTopics(
            List<CompanionOutcomeTopic> topics,
            DialogueContext context,
            String[][] outcomeTopics
    ) {
        for (String[] topic : outcomeTopics) {
            addOutcomeTopic(topics, context, topic[0], topic[1]);
        }
    }

    private static void addCompanionOathsteadTree(
            Map<String, DialogueNode> nodes,
            List<DialogueChoice> rootChoices,
            CompanionVoice voice,
            int relationship,
            DialogueContext context
    ) {
        rootChoices.add(new DialogueChoice("How does Oathstead feel to you?", "companion_oathstead"));
        addNode(nodes, "companion_oathstead", voice.reply(DialogueIntent.HOME, relationship, context,
                voice.oathsteadLine(context)), List.of(
                new DialogueChoice("Does this place feel like yours?", "companion_oathstead_home", 2),
                new DialogueChoice("How is your work here?", "companion_oathstead_work", 1),
                new DialogueChoice("What does Oathstead need next?", "companion_oathstead_need", 1),
                backToHome()
        ));
        addNode(nodes, "companion_oathstead_home", voice.reply(DialogueIntent.HOME, relationship, context,
                voice.oathsteadHomeLine(context)), companionBackChoices(voice));
        addNode(nodes, "companion_oathstead_work", voice.reply(DialogueIntent.HOME, relationship, context,
                voice.assignedWorkLine(context)), companionBackChoices(voice));
        addNode(nodes, "companion_oathstead_need", voice.reply(DialogueIntent.NEXT_STEP, relationship, context,
                voice.oathsteadNeedLine()), companionBackChoices(voice));
    }

    private static String practicalQuestLabel(Quest quest) {
        if (quest == null) {
            return "What am I looking for, exactly?";
        }
        return switch (quest.activeObjectiveKind()) {
            case DEFEAT -> "What are we up against?";
            case RESCUE -> "Who are we trying to get out alive?";
            case DEFEND -> "What has to hold?";
            case RAID_DEFENSE -> "How do we survive the raid?";
            case GATHER -> "What am I bringing back?";
            case DELIVER -> "Who needs this in their hands?";
            case VISIT -> "What am I supposed to notice there?";
            case SEARCH -> "What am I looking for, exactly?";
            case TALK -> "Who needs to hear this from me?";
            case ASK_AROUND -> "Whose version should I listen for?";
            case REPORT -> "Who needs the truth carried back?";
            case ESCORT -> "Where do we need to get them safely?";
            case CHOICE -> "What choice are you putting in my hands?";
        };
    }

    private static String relationshipTopicLabel(int relationship, DialogueContext context) {
        if (context.married()) {
            return "Can we talk about our promise?";
        }
        if (context.romanced() || relationship >= 180) {
            return "What is this between us?";
        }
        if (relationship >= 120) {
            return "Do you trust me?";
        }
        if (relationship >= 50) {
            return "Can we talk more personally?";
        }
        return "Can I earn your trust?";
    }

    private static String cleanQuestLine(String line, String speaker) {
        if (line == null || line.isBlank()) {
            return "";
        }
        String cleaned = line.replaceAll("\\s+", " ").strip();
        String prefix = speaker + ":";
        if (cleaned.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return cleaned.substring(prefix.length()).strip();
        }
        int colon = cleaned.indexOf(':');
        if (colon > 0 && colon <= 24) {
            return cleaned.substring(colon + 1).strip();
        }
        return cleaned;
    }

    private static String compactQuestOfferLine(String line) {
        if (line == null || line.isBlank()) {
            return "";
        }
        String cleaned = line.replaceAll("\\s+", " ").strip();
        if (cleaned.length() <= 140) {
            return cleaned;
        }
        for (char stop : new char[] {'.', '!', '?'}) {
            int index = cleaned.indexOf(stop);
            if (index >= 0 && index < 140) {
                return cleaned.substring(0, index + 1).strip();
            }
        }
        int split = cleaned.lastIndexOf(' ', 136);
        if (split <= 0) {
            split = Math.min(cleaned.length(), 136);
        }
        return cleaned.substring(0, split).strip() + "...";
    }

    private static StoryBranch companionQuestBranch(Npc npc, Quest quest) {
        if (quest == null || !quest.companionQuest()) {
            return null;
        }
        CompanionVoice voice = companionVoice(npc);
        if (voice == null) {
            return null;
        }
        int stage = companionQuestStage(quest);
        return switch (stage) {
            case 1 -> branch(voice.firstQuestOpening(),
                    option(voice.helpLine(), voice.helpResponse(), 2),
                    option(voice.personalQuestion(), voice.personalResponse(), 1),
                    option("I need the practical version.", voice.practicalResponse(quest), 1),
                    option(voice.impatientLine(), voice.impatientResponse(), -1));
            case 7 -> branch(voice.bossQuestOpening(),
                    option("Tell me what this fight really means.", voice.bossMeaning(), 2),
                    option("How do we win without losing you to it?", voice.bossTactical(), 2),
                    option("Then we end this cleanly.", voice.bossClean(), 1),
                    option("It is just another fight.", voice.bossDismiss(), -2));
            case 8 -> branch(voice.finalQuestOpening(),
                    option("You choose where you stand now.", voice.finalChoice(), 2),
                    option("Oathstead has room for the truth.", voice.finalHome(), 2),
                    option("What do you need from me?", voice.finalNeed(), 1),
                    option("At least this is finally over.", voice.finalDismiss(), -1));
            default -> branch(voice.middleQuestOpening(quest),
                    option(voice.helpLine(), voice.helpResponse(), 2),
                    option(voice.personalQuestion(), voice.personalResponse(), 1),
                    option("What should I look for?", voice.practicalResponse(quest), 1),
                    option(voice.impatientLine(), voice.impatientResponse(), -1));
        };
    }

    private static StoryBranch companionRelationshipBranch(Npc npc, int relationship) {
        CompanionVoice voice = companionVoice(npc);
        if (voice == null) {
            return null;
        }
        if (relationship >= 250) {
            return branch(voice.marriageOpening(),
                    option("Marry me, " + voice.shortName() + ".", voice.marriageAccept(), 5, "marriage:accept"),
                    option("Would you want a future with me?", voice.marriageAsk(), 2),
                    option("One day, when the road is quieter.", voice.marriageLater(), 1),
                    option("I cannot promise forever.", voice.marriageNo(), -1));
        }
        if (relationship >= 180) {
            return branch(voice.lovingOpening(),
                    option("I feel it too.", voice.romanceAccept(), 3, "romance:start"),
                    option("Stay with me a while.", voice.romanceStay(), 3, "romance:start"),
                    option("This matters to me.", voice.romanceWarm(), 2),
                    option("We should keep this professional.", voice.romanceNo(), -2, "romance:end"));
        }
        if (relationship >= 150) {
            return branch(voice.loyalOpening(),
                    option("Stand with me.", voice.loyalStand(), 2),
                    option("You are free to choose your road.", voice.loyalChoose(), 2),
                    option("I am glad you are here.", voice.loyalWarm(), 1),
                    option("Good. I need your skills.", voice.loyalCold(), -1));
        }
        if (relationship >= 120) {
            return branch(voice.trustedOpening(),
                    option("Tell me the full truth.", voice.trustedTruth(), 2),
                    option("I will carry this carefully.", voice.trustedCare(), 2),
                    option("You trust me that much?", voice.trustedAsk(), 1),
                    option("Would someone else be safer?", voice.trustedDoubt(), -1));
        }
        if (relationship >= 90) {
            return branch(voice.friendOpening(),
                    option("I am listening.", voice.friendListen(), 2),
                    option("Take your time.", voice.friendPatient(), 2),
                    option("Can it help us?", voice.friendMission(), 1),
                    option("Make it quick.", voice.friendRush(), -1));
        }
        if (relationship >= 50) {
            return branch(voice.friendlyOpening(),
                    option("Good to see you too.", voice.friendlyWarm(), 2),
                    option("You sound almost comfortable.", voice.friendlyPlay(), 1),
                    option("What changed?", voice.friendlyAsk(), 1),
                    option("Stay alert.", voice.friendlyCold(), -1));
        }
        if (relationship >= 20) {
            return branch(voice.acquaintanceOpening(),
                    option("I will take useful for now.", voice.acquaintUseful(), 1),
                    option("I would rather earn trust.", voice.acquaintTrust(), 2),
                    option("You keep your distance.", voice.acquaintDistance(), 0),
                    option("I do not need your trust.", voice.acquaintCold(), -1));
        }
        return branch(voice.guardedOpening(),
                option("Then teach me how to help.", voice.guardedTeach(), 2),
                option("I am listening.", voice.guardedListen(), 1),
                option("I survive well enough.", voice.guardedNeutral(), 0),
                option("I do not have time for this.", voice.guardedCold(), -1));
    }

    public static String questTopicLabel(Quest quest) {
        CompanionVoice voice = companionVoiceForQuest(quest);
        if (voice != null) {
            return voice.questTopic(quest);
        }
        if (quest == null) {
            return "What work needs doing?";
        }
        if (quest.companionQuest()) {
            return "What does this mean to you?";
        }
        return switch (questDialogueStage(quest)) {
            case OFFER -> switch (quest.activeObjectiveKind()) {
                case DEFEAT -> "Tell me what needs stopping.";
                case RESCUE -> "Tell me who needs saving.";
                case DEFEND -> "Tell me what has to hold.";
                case RAID_DEFENSE -> "Tell me how the raid starts.";
                case GATHER -> "Tell me what you need brought back.";
                case DELIVER -> "Tell me what needs carrying.";
                case VISIT -> "Tell me what I should inspect.";
                case SEARCH -> "Tell me what went missing.";
                case TALK -> "Tell me who needs answers.";
                case ASK_AROUND -> "Tell me what rumor matters.";
                case REPORT -> "Tell me what truth needs reporting.";
                case ESCORT -> "Tell me who needs the road.";
                case CHOICE -> "Tell me what choice is waiting.";
            };
            case READY -> questTurnInLabel(quest);
            case COMPLETED -> "What changed after " + shortQuestTitle(quest) + "?";
            case ACCEPTED, IN_PROGRESS -> "Where does " + shortQuestTitle(quest) + " stand?";
        };
    }

    public static String questCommitLabel(Quest quest) {
        CompanionVoice voice = companionVoiceForQuest(quest);
        if (voice != null) {
            return voice.helpLine();
        }
        if (quest != null && quest.activeObjectiveKind().defenseMinigameObjective()) {
            return "Start the raid defense now";
        }
        if (quest == null) {
            return "I will help.";
        }
        if (quest.companionQuest()) {
            return "How can I help?";
        }
        return switch (quest.activeObjectiveKind()) {
            case DEFEAT -> "I will stop it.";
            case RESCUE -> "I will get them out.";
            case DEFEND -> "I will help hold the line.";
            case RAID_DEFENSE -> "Start the raid defense now";
            case GATHER -> "I will bring it back.";
            case DELIVER -> "I will carry it carefully.";
            case VISIT -> "I will inspect it.";
            case SEARCH -> "I will find what is hidden.";
            case TALK -> "I will speak with them.";
            case ASK_AROUND -> "I will ask around quietly.";
            case REPORT -> "I will report the truth.";
            case ESCORT -> "I will get them there safely.";
            case CHOICE -> "I will make the choice.";
        };
    }

    private static String questDoubtLabel(Quest quest) {
        CompanionVoice voice = companionVoiceForQuest(quest);
        if (voice != null) {
            return voice.personalQuestion();
        }
        if (quest == null) {
            return "Why does this need doing?";
        }
        if (quest.companionQuest()) {
            return "Why does this matter to you?";
        }
        return switch (quest.activeObjectiveKind()) {
            case DEFEAT -> "Is killing it the only answer?";
            case RESCUE -> "What if I am already too late?";
            case DEFEND -> "What if this place cannot hold?";
            case RAID_DEFENSE -> "What if we are not ready?";
            case GATHER -> "Why does this object matter so much?";
            case DELIVER -> "Why trust a stranger with this?";
            case VISIT -> "What if there is nothing there?";
            case SEARCH -> "What if the truth is worse?";
            case TALK -> "What if they refuse to talk?";
            case ASK_AROUND -> "What if the rumors are only fear?";
            case REPORT -> "What if they do not want the truth?";
            case ESCORT -> "What if the road is already watched?";
            case CHOICE -> "Why should I be the one to decide?";
        };
    }

    private static CompanionVoice companionVoiceForQuest(Quest quest) {
        if (quest == null || !quest.companionQuest() || quest.chainOwnerId == null) {
            return null;
        }
        return companionVoice(quest.chainOwnerId);
    }

    private static CompanionVoice companionVoice(Npc npc) {
        if (npc == null) {
            return null;
        }
        if (npc.recruitId() != null && !npc.recruitId().isBlank()) {
            return companionVoice(npc.recruitId());
        }
        return companionVoice(npcDialogKey(npc));
    }

    private static CompanionVoice companionVoice(String key) {
        if (key == null) {
            return null;
        }
        return switch (key.strip().toLowerCase()) {
            case "seraphine", "seraphine vale" -> CompanionVoice.SERAPHINE;
            case "maera", "maera quill" -> CompanionVoice.MAERA;
            case "cassia", "cassia flint" -> CompanionVoice.CASSIA;
            case "lyra", "lyra bell" -> CompanionVoice.LYRA;
            case "samir", "samir dawn" -> CompanionVoice.SAMIR;
            case "aria", "aria foxglove" -> CompanionVoice.ARIA;
            case "vesper", "vesper snowroot" -> CompanionVoice.VESPER;
            case "rafiq", "rafiq glass" -> CompanionVoice.RAFIQ;
            case "calder", "calder reed" -> CompanionVoice.CALDER;
            default -> null;
        };
    }

    private static int companionQuestStage(Quest quest) {
        if (quest == null || quest.id == null) {
            return 0;
        }
        int marker = quest.id.lastIndexOf("_chain_");
        if (marker < 0) {
            return 0;
        }
        try {
            return Integer.parseInt(quest.id.substring(marker + "_chain_".length()));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    enum DialogueIntent {
        QUEST_ROOT,
        QUEST_CLARIFY,
        QUEST_PERSONAL,
        QUEST_PRACTICAL,
        QUEST_WARNING,
        QUEST_SUPPORT,
        QUEST_CHALLENGE,
        CONCERN,
        FEELING,
        NEXT_STEP,
        NEED,
        VALUES,
        MEMORY,
        OPINION,
        HOME,
        ROMANCE,
        REQUEST,
        FUTURE,
        TRUST
    }

    private enum CompanionVoice {
        SERAPHINE(SeraphineDialogue.profile()),
        MAERA(MaeraDialogue.profile()),
        CASSIA(CassiaDialogue.profile()),
        LYRA(LyraDialogue.profile()),
        SAMIR(SamirDialogue.profile()),
        ARIA(AriaDialogue.profile()),
        VESPER(VesperDialogue.profile()),
        RAFIQ(RafiqDialogue.profile()),
        CALDER(CalderDialogue.profile());

        private final String shortName;
        private final String concern;
        private final String questTopic;
        private final String firstQuestOpening;
        private final String personalQuestion;
        private final String personalResponse;
        private final String marriageOpening;
        private final String marriageAccept;
        private final String lovingOpening;

        CompanionVoice(CompanionDialogueProfile profile) {
            this.shortName = profile.shortName();
            this.concern = profile.concern();
            this.questTopic = profile.questTopic();
            this.firstQuestOpening = profile.firstQuestOpening();
            this.personalQuestion = profile.personalQuestion();
            this.personalResponse = profile.personalResponse();
            this.marriageOpening = profile.marriageOpening();
            this.marriageAccept = profile.marriageAccept();
            this.lovingOpening = profile.lovingOpening();
        }

        String shortName() {
            return shortName;
        }

        String reply(DialogueIntent intent, int relationship, DialogueContext context, String answer) {
            String cleaned = answer == null ? "" : answer.replaceAll("\\s+", " ").strip();
            StringBuilder line = new StringBuilder();
            String narration = conversationBeat(intent, relationship, context);
            if (!narration.isBlank()) {
                line.append(narration).append(' ');
            }
            line.append('"');
            String lead = replyLead(intent, relationship, context, cleaned);
            if (!lead.isBlank()) {
                line.append(lead);
            }
            if (!cleaned.isBlank()) {
                if (!lead.isBlank()) {
                    line.append(' ');
                }
                line.append(cleaned);
            }
            String thread = relationshipThread(intent, relationship, context);
            if (!thread.isBlank()) {
                line.append(' ').append(thread);
            }
            line.append('"');
            return line.toString();
        }

        private String replyLead(DialogueIntent intent, int relationship, DialogueContext context, String answer) {
            if (intent == null) {
                return "";
            }
            if (questIntent(intent)) {
                return "";
            }
            int visits = context.topicVisitCount(topicKey(intent));
            if (visits > 0) {
                return repeatAcknowledgement(intent, context);
            }
            String characterLine = characterIntentAcknowledgement(intent);
            if (!characterLine.isBlank()) {
                return characterLine;
            }
            return answer == null || answer.isBlank() ? intentAcknowledgement(intent, relationship, context) : "";
        }

        private boolean questIntent(DialogueIntent intent) {
            return switch (intent) {
                case QUEST_ROOT, QUEST_CLARIFY, QUEST_PERSONAL, QUEST_PRACTICAL, QUEST_WARNING,
                     QUEST_SUPPORT, QUEST_CHALLENGE -> true;
                default -> false;
            };
        }

        private String repeatAcknowledgement(DialogueIntent intent, DialogueContext context) {
            return "";
        }

        private String topicKey(DialogueIntent intent) {
            if (intent == null) {
                return "";
            }
            return switch (intent) {
                case QUEST_ROOT -> "quest";
                case QUEST_CLARIFY -> "quest.clarify";
                case QUEST_PERSONAL -> "quest.personal";
                case QUEST_PRACTICAL -> "quest.practical";
                case QUEST_WARNING -> "quest.warning";
                case QUEST_SUPPORT -> "quest.support";
                case QUEST_CHALLENGE -> "quest.challenge";
                case CONCERN -> "checkin";
                case FEELING -> "feeling";
                case NEXT_STEP -> "next";
                case NEED -> "need";
                case VALUES -> "values";
                case MEMORY -> "memory";
                case OPINION -> "opinion";
                case HOME -> "home";
                case ROMANCE -> "romance";
                case REQUEST -> "request";
                case FUTURE -> "future";
                case TRUST -> "trust";
            };
        }

        private String conversationBeat(DialogueIntent intent, int relationship, DialogueContext context) {
            String beat = characterBeat(intent);
            String relationshipBeat = relationshipBeat(relationship, context);
            return relationshipBeat.isBlank() ? beat : beat + " " + relationshipBeat;
        }

        private String characterBeat(DialogueIntent intent) {
            DialogueIntent safeIntent = intent == null ? DialogueIntent.TRUST : intent;
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.characterBeat(safeIntent);
                case MAERA -> MaeraDialogue.characterBeat(safeIntent);
                case CASSIA -> CassiaDialogue.characterBeat(safeIntent);
                case LYRA -> LyraDialogue.characterBeat(safeIntent);
                case SAMIR -> SamirDialogue.characterBeat(safeIntent);
                case ARIA -> AriaDialogue.characterBeat(safeIntent);
                case VESPER -> VesperDialogue.characterBeat(safeIntent);
                case RAFIQ -> RafiqDialogue.characterBeat(safeIntent);
                case CALDER -> CalderDialogue.characterBeat(safeIntent);
            };
        }

        private String relationshipBeat(int relationship, DialogueContext context) {
            if (context.married()) {
                return "The answer comes with the ease of someone who has learned your silences.";
            }
            if (context.romanced()) {
                return "There is warmth in it now, not careless, but no longer disguised as tactics.";
            }
            if (relationship >= 150) {
                return "Trust has taken enough root that the guarded part no longer speaks first.";
            }
            return "";
        }

        private String intentAcknowledgement(DialogueIntent intent, int relationship, DialogueContext context) {
            String characterLine = characterIntentAcknowledgement(intent);
            if (!characterLine.isBlank()) {
                return characterLine;
            }
            return switch (intent) {
                case QUEST_ROOT -> "You are asking where this stands, so I will not hand you a loose errand.";
                case QUEST_CLARIFY -> "You asked what is really happening. Here is the thread that matters.";
                case QUEST_PERSONAL -> "You asked why it matters to me, so I will not pretend it is only practical.";
                case QUEST_PRACTICAL -> "You asked for the work. Here is the work, tied to the reason.";
                case QUEST_WARNING -> "You asked what can go wrong. Good. That question keeps people alive.";
                case QUEST_SUPPORT -> "You offered to carry this, so I will name what you are carrying.";
                case QUEST_CHALLENGE -> "You are challenging the shape of this. I can answer that.";
                case CONCERN -> "You asked how I am holding up, so I will answer the part that is not just strategy.";
                case FEELING -> "You want the feeling, not the report. That is harder, and more useful.";
                case NEXT_STEP -> "Since you asked what comes next, I will give you the road and the reason for it.";
                case NEED -> "You asked what I need. I will risk being specific.";
                case VALUES -> "You asked how I see you. I will not make it abstract.";
                case MEMORY -> "You brought up the memory. I will not treat it like a trophy.";
                case OPINION -> "Since you asked what I think, I will be precise.";
                case HOME -> "You asked about home. That is not a small question anymore.";
                case ROMANCE -> "You asked for time with me, not another task. I heard that.";
                case REQUEST -> "You asked what I need from you. This is the honest shape of it.";
                case FUTURE -> "You asked about after. I will answer as if after is allowed.";
                case TRUST -> "You are asking where trust lives between us. I will answer plainly.";
            };
        }

        private String characterIntentAcknowledgement(DialogueIntent intent) {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.intentAcknowledgement(intent);
                case MAERA -> MaeraDialogue.intentAcknowledgement(intent);
                case CASSIA -> CassiaDialogue.intentAcknowledgement(intent);
                case LYRA -> LyraDialogue.intentAcknowledgement(intent);
                case SAMIR -> SamirDialogue.intentAcknowledgement(intent);
                case ARIA -> AriaDialogue.intentAcknowledgement(intent);
                case VESPER -> VesperDialogue.intentAcknowledgement(intent);
                case RAFIQ -> RafiqDialogue.intentAcknowledgement(intent);
                case CALDER -> CalderDialogue.intentAcknowledgement(intent);
            };
        }

        private String relationshipThread(DialogueIntent intent, int relationship, DialogueContext context) {
            return "";
        }

        String firstQuestOpening() {
            return firstQuestOpening;
        }

        String questGreeting(Quest quest) {
            int stage = companionQuestStage(quest);
            if (quest.ready()) {
                return readyRootGreeting(stage);
            }
            if (quest.accepted && quest.progress > 0) {
                return progressRootGreeting(stage);
            }
            if (stage == 1) {
                return firstQuestOpening;
            }
            if (stage == 7) {
                return bossQuestOpening();
            }
            if (stage == 8) {
                return finalQuestOpening();
            }
            return middleQuestOpening(quest);
        }

        String questOpening(Quest quest) {
            String line;
            if (quest.completed) {
                line = cleanQuestLine(quest.activeCompleteDialog(), shortName);
                return line.isBlank() ? afterQuestLine(companionQuestStage(quest)) : line + " " + afterQuestLine(companionQuestStage(quest));
            }
            if (!quest.accepted) {
                line = cleanQuestLine(quest.activeStartDialog(), shortName);
                return line.isBlank() ? questGreeting(quest) : line;
            }
            if (quest.ready()) {
                line = cleanQuestLine(quest.activeReadyDialog(), shortName);
                return (line.isBlank() ? readyRootGreeting(companionQuestStage(quest)) : line) + " " + readyLine(companionQuestStage(quest));
            }
            if (quest.progress > 0) {
                line = cleanQuestLine(quest.activeProgressDialog(), shortName);
                return (line.isBlank() ? progressRootGreeting(companionQuestStage(quest)) : line) + " " + progressLine(companionQuestStage(quest));
            }
            return questGreeting(quest);
        }

        String helpLine() {
            return "How can I help?";
        }

        String helpResponse() {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.helpResponse();
                case MAERA -> MaeraDialogue.helpResponse();
                case CASSIA -> CassiaDialogue.helpResponse();
                case LYRA -> LyraDialogue.helpResponse();
                case SAMIR -> SamirDialogue.helpResponse();
                case ARIA -> AriaDialogue.helpResponse();
                case VESPER -> VesperDialogue.helpResponse();
                case RAFIQ -> RafiqDialogue.helpResponse();
                case CALDER -> CalderDialogue.helpResponse();
            };
        }

        String questCommitLabel(Quest quest) {
            if (quest != null && quest.activeObjectiveKind().defenseMinigameObjective()) {
                return "Start the raid defense now";
            }
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.questCommitLabel();
                case MAERA -> MaeraDialogue.questCommitLabel();
                case CASSIA -> CassiaDialogue.questCommitLabel();
                case LYRA -> LyraDialogue.questCommitLabel();
                case SAMIR -> SamirDialogue.questCommitLabel();
                case ARIA -> AriaDialogue.questCommitLabel();
                case VESPER -> VesperDialogue.questCommitLabel(quest);
                case RAFIQ -> RafiqDialogue.questCommitLabel();
                case CALDER -> CalderDialogue.questCommitLabel();
            };
        }

        String questPersonalCommitLabel(Quest quest) {
            if (quest != null && quest.activeObjectiveKind().defenseMinigameObjective()) {
                return "I understand. Start the raid defense.";
            }
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.questPersonalCommitLabel();
                case MAERA -> MaeraDialogue.questPersonalCommitLabel();
                case CASSIA -> CassiaDialogue.questPersonalCommitLabel();
                case LYRA -> LyraDialogue.questPersonalCommitLabel();
                case SAMIR -> SamirDialogue.questPersonalCommitLabel();
                case ARIA -> AriaDialogue.questPersonalCommitLabel();
                case VESPER -> VesperDialogue.questPersonalCommitLabel();
                case RAFIQ -> RafiqDialogue.questPersonalCommitLabel();
                case CALDER -> CalderDialogue.questPersonalCommitLabel();
            };
        }

        String questPracticalCommitLabel(Quest quest) {
            if (quest != null && quest.activeObjectiveKind().defenseMinigameObjective()) {
                return "Take me to the raid line now.";
            }
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.questPracticalCommitLabel();
                case MAERA -> MaeraDialogue.questPracticalCommitLabel();
                case CASSIA -> CassiaDialogue.questPracticalCommitLabel();
                case LYRA -> LyraDialogue.questPracticalCommitLabel();
                case SAMIR -> SamirDialogue.questPracticalCommitLabel();
                case ARIA -> AriaDialogue.questPracticalCommitLabel();
                case VESPER -> VesperDialogue.questPracticalCommitLabel();
                case RAFIQ -> RafiqDialogue.questPracticalCommitLabel();
                case CALDER -> CalderDialogue.questPracticalCommitLabel();
            };
        }

        String questAcceptedLine(int stage, Quest quest) {
            if (quest != null) {
                return chapterAcceptanceLine(stage, quest);
            }
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.questAcceptedLine();
                case MAERA -> MaeraDialogue.questAcceptedLine();
                case CASSIA -> CassiaDialogue.questAcceptedLine();
                case LYRA -> LyraDialogue.questAcceptedLine();
                case SAMIR -> SamirDialogue.questAcceptedLine();
                case ARIA -> AriaDialogue.questAcceptedLine();
                case VESPER -> VesperDialogue.questAcceptedLine();
                case RAFIQ -> RafiqDialogue.questAcceptedLine();
                case CALDER -> CalderDialogue.questAcceptedLine();
            };
        }

        String personalQuestion() {
            return personalQuestion;
        }

        String personalResponse() {
            return personalResponse;
        }

        String practicalResponse(Quest quest) {
            return objectiveDialogueLine(quest);
        }

        String stageClarify(int stage, Quest quest) {
            if (quest != null) {
                return chapterClarifyLine(stage, quest);
            }
            return switch (stage) {
                case 1 -> firstQuestOpening + " " + originLine();
                case 2 -> "This is the practical part, where pretty motives become sore hands. " + concernLine();
                case 3 -> "Investigation is where the past stops being polite. Objects remember what people edit.";
                case 4 -> "The conflict has names, weapons, and timing. That means it can be faced, but it is no longer only mine.";
                case 5 -> "This is the backstory part. I dislike that phrase. It makes old pain sound decorative. " + woundLine();
                case 6 -> "A trial is not a test of whether I hurt. It is a test of whether hurt still gets to command me.";
                case 7 -> bossMeaning();
                case 8 -> finalChoice();
                default -> "This is one branch of the same tree. " + quest.title + " keeps leading back to " + concern + ".";
            };
        }

        String stagePersonal(int stage, Quest quest) {
            if (quest != null) {
                return chapterPersonalLine(stage, quest);
            }
            return switch (stage) {
                case 1 -> personalResponse;
                case 2 -> "I am asking for grounded help because trust has to survive ordinary weight before it survives danger.";
                case 3 -> "I need a witness because I know how easily one person can talk themselves around an inconvenient truth.";
                case 4 -> "The fight matters because enemies connected to the old wound are trying to decide my future for me.";
                case 5 -> woundLine();
                case 6 -> "If I face this alone, I will call it independence and mean fear. I would rather be more honest than that.";
                case 7 -> bossMeaning();
                case 8 -> finalNeed();
                default -> personalResponse;
            };
        }

        private String chapterAcceptanceLine(int stage, Quest quest) {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.chapterAcceptanceLine(activeStageFocusLine(quest));
                case MAERA -> MaeraDialogue.chapterAcceptanceLine(activeStageFocusLine(quest));
                case CASSIA -> CassiaDialogue.chapterAcceptanceLine(activeStageFocusLine(quest));
                case LYRA -> LyraDialogue.chapterAcceptanceLine(activeStageFocusLine(quest));
                case SAMIR -> SamirDialogue.chapterAcceptanceLine(activeStageFocusLine(quest));
                case ARIA -> AriaDialogue.chapterAcceptanceLine(activeStageFocusLine(quest));
                case VESPER -> VesperDialogue.chapterAcceptanceLine(activeStageFocusLine(quest));
                case RAFIQ -> RafiqDialogue.chapterAcceptanceLine(activeStageFocusLine(quest));
                case CALDER -> CalderDialogue.chapterAcceptanceLine(activeStageFocusLine(quest));
            };
        }

        private String chapterClarifyLine(int stage, Quest quest) {
            String focus = activeStageFocusLine(quest);
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.chapterClarifyLine(focus);
                case MAERA -> MaeraDialogue.chapterClarifyLine(focus);
                case CASSIA -> CassiaDialogue.chapterClarifyLine(focus);
                case LYRA -> LyraDialogue.chapterClarifyLine(focus);
                case SAMIR -> SamirDialogue.chapterClarifyLine(focus);
                case ARIA -> AriaDialogue.chapterClarifyLine(focus);
                case VESPER -> VesperDialogue.chapterClarifyLine(focus);
                case RAFIQ -> RafiqDialogue.chapterClarifyLine(focus);
                case CALDER -> CalderDialogue.chapterClarifyLine(focus);
            };
        }

        private String chapterPersonalLine(int stage, Quest quest) {
            String title = quest.title;
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.chapterPersonalLine(title);
                case MAERA -> MaeraDialogue.chapterPersonalLine(title);
                case CASSIA -> CassiaDialogue.chapterPersonalLine(title);
                case LYRA -> LyraDialogue.chapterPersonalLine(title);
                case SAMIR -> SamirDialogue.chapterPersonalLine(title);
                case ARIA -> AriaDialogue.chapterPersonalLine(title);
                case VESPER -> VesperDialogue.chapterPersonalLine(title);
                case RAFIQ -> RafiqDialogue.chapterPersonalLine(title);
                case CALDER -> CalderDialogue.chapterPersonalLine(title);
            };
        }

        String stagePractical(int stage, Quest quest) {
            return objectiveDialogueLine(quest);
        }

        private String objectiveDialogueLine(Quest quest) {
            if (quest == null) {
                return defaultObjectiveLine();
            }
            String place = objectiveLocationLine(quest);
            String target = objectiveTargetLine(quest);
            String instruction = switch (quest.activeObjectiveKind()) {
                case DEFEAT -> "Find " + target + " near " + place + " and stop it.";
                case RESCUE -> "Reach " + target + " near " + place + ".";
                case DEFEND -> "Hold " + target + " at " + place + ".";
                case RAID_DEFENSE -> "Repel " + target + " at " + place + ".";
                case GATHER -> "Start at " + place + " and bring back " + target + ".";
                case DELIVER -> "Carry " + target + " to " + place + ".";
                case VISIT -> "Go to " + place + " and inspect " + target + ".";
                case SEARCH -> "Start at " + place + ". Find " + target + ".";
                case TALK -> "Find " + target + " at " + place + " and ask plainly.";
                case ASK_AROUND -> "Ask around " + place + " about " + target + ".";
                case REPORT -> "Bring the truth about " + target + " back to " + place + ".";
                case ESCORT -> "Get " + target + " through " + place + " safely.";
                case CHOICE -> "Decide what to do about " + target + ".";
            };
            return instruction;
        }

        private String defaultObjectiveLine() {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.defaultObjectiveLine();
                case MAERA -> MaeraDialogue.defaultObjectiveLine();
                case CASSIA -> CassiaDialogue.defaultObjectiveLine();
                case LYRA -> LyraDialogue.defaultObjectiveLine();
                case SAMIR -> SamirDialogue.defaultObjectiveLine();
                case ARIA -> AriaDialogue.defaultObjectiveLine();
                case VESPER -> VesperDialogue.defaultObjectiveLine();
                case RAFIQ -> RafiqDialogue.defaultObjectiveLine();
                case CALDER -> CalderDialogue.defaultObjectiveLine();
            };
        }

        private String objectiveTargetLine(Quest quest) {
            String target = quest.activeTarget();
            int needed = quest.activeNeeded();
            return switch (quest.activeObjectiveKind()) {
                case GATHER, SEARCH, VISIT -> needed > 1 ? needed + " " + target : target;
                default -> target;
            };
        }

        private String objectiveVoiceWarning(Quest quest) {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.objectiveVoiceWarning();
                case MAERA -> MaeraDialogue.objectiveVoiceWarning();
                case CASSIA -> CassiaDialogue.objectiveVoiceWarning();
                case LYRA -> LyraDialogue.objectiveVoiceWarning();
                case SAMIR -> SamirDialogue.objectiveVoiceWarning();
                case ARIA -> AriaDialogue.objectiveVoiceWarning();
                case VESPER -> VesperDialogue.objectiveVoiceWarning();
                case RAFIQ -> RafiqDialogue.objectiveVoiceWarning();
                case CALDER -> CalderDialogue.objectiveVoiceWarning();
            };
        }

        String supportResponse(int stage, Quest quest) {
            return switch (stage) {
                case 5, 6, 7 -> "Good. I do not need rescuing from the truth. I need someone beside me while I stop obeying it.";
                case 8 -> finalHome();
                default -> switch (this) {
                    case SERAPHINE -> SeraphineDialogue.supportResponse();
                    case MAERA -> MaeraDialogue.supportResponse();
                    case CASSIA -> CassiaDialogue.supportResponse();
                    case LYRA -> LyraDialogue.supportResponse();
                    case SAMIR -> SamirDialogue.supportResponse();
                    case ARIA -> AriaDialogue.supportResponse();
                    case VESPER -> VesperDialogue.supportResponse();
                    case RAFIQ -> RafiqDialogue.supportResponse();
                    case CALDER -> CalderDialogue.supportResponse();
                };
            };
        }

        String challengeResponse(int stage, Quest quest) {
            return switch (stage) {
                case 5, 6 -> "Maybe. But pain can be true and still not be an excuse. I am trying to learn the difference.";
                case 7 -> bossDismiss();
                default -> "You can doubt me. Just do it with open eyes, not impatience wearing armor.";
            };
        }

        String warningResponse(int stage, Quest quest) {
            if (quest != null) {
                return chapterWarningLine(stage, quest);
            }
            return switch (stage) {
                case 3 -> "Evidence may be planted. Look for what is too neat, too clean, or too eager to be found.";
                case 4, 7 -> bossTactical();
                case 8 -> "Old endings sometimes pretend to be peace. Watch for what still asks to be carried.";
                default -> switch (this) {
                    case SERAPHINE -> SeraphineDialogue.warningResponse();
                    case MAERA -> MaeraDialogue.warningResponse();
                    case CASSIA -> CassiaDialogue.warningResponse();
                    case LYRA -> LyraDialogue.warningResponse();
                    case SAMIR -> SamirDialogue.warningResponse();
                    case ARIA -> AriaDialogue.warningResponse();
                    case VESPER -> VesperDialogue.warningResponse();
                    case RAFIQ -> RafiqDialogue.warningResponse();
                    case CALDER -> CalderDialogue.warningResponse();
                };
            };
        }

        private String chapterWarningLine(int stage, Quest quest) {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.chapterWarningLine();
                case MAERA -> MaeraDialogue.chapterWarningLine();
                case CASSIA -> CassiaDialogue.chapterWarningLine();
                case LYRA -> LyraDialogue.chapterWarningLine();
                case SAMIR -> SamirDialogue.chapterWarningLine();
                case ARIA -> AriaDialogue.chapterWarningLine();
                case VESPER -> VesperDialogue.chapterWarningLine();
                case RAFIQ -> RafiqDialogue.chapterWarningLine();
                case CALDER -> CalderDialogue.chapterWarningLine();
            };
        }

        private String activeStageFocusLine(Quest quest) {
            Quest.QuestStage active = quest.activeStage();
            String target = objectiveTargetLine(quest);
            String place = objectiveLocationLine(quest);
            return switch (active.objectiveKind()) {
                case DEFEAT -> "For now: face " + target + " near " + place + ".";
                case RESCUE -> "For now: reach " + target + " near " + place + ".";
                case DEFEND -> "For now: hold " + target + " at " + place + ".";
                case RAID_DEFENSE -> "For now: repel " + target + " at " + place + ".";
                case GATHER -> "For now: bring back " + target + " from " + place + ".";
                case DELIVER -> "For now: carry " + target + " to " + place + ".";
                case VISIT -> "For now: inspect " + target + " at " + place + ".";
                case SEARCH -> "For now: find " + target + " at " + place + ".";
                case TALK -> "For now: speak with " + target + " at " + place + ".";
                case ASK_AROUND -> "For now: ask around " + place + " about " + target + ".";
                case REPORT -> "For now: report about " + target + " at " + place + ".";
                case ESCORT -> "For now: escort " + target + " through " + place + ".";
                case CHOICE -> "For now: decide what " + target + " should mean.";
            };
        }

        private String searchFocusClose() {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.searchFocusClose();
                case MAERA -> MaeraDialogue.searchFocusClose();
                case CASSIA -> CassiaDialogue.searchFocusClose();
                case LYRA -> LyraDialogue.searchFocusClose();
                case SAMIR -> SamirDialogue.searchFocusClose();
                case ARIA -> AriaDialogue.searchFocusClose();
                case VESPER -> VesperDialogue.searchFocusClose();
                case RAFIQ -> RafiqDialogue.searchFocusClose();
                case CALDER -> CalderDialogue.searchFocusClose();
            };
        }

        private String gatherFocusClose() {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.gatherFocusClose();
                case MAERA -> MaeraDialogue.gatherFocusClose();
                case CASSIA -> CassiaDialogue.gatherFocusClose();
                case LYRA -> LyraDialogue.gatherFocusClose();
                case SAMIR -> SamirDialogue.gatherFocusClose();
                case ARIA -> AriaDialogue.gatherFocusClose();
                case VESPER -> VesperDialogue.gatherFocusClose();
                case RAFIQ -> RafiqDialogue.gatherFocusClose();
                case CALDER -> CalderDialogue.gatherFocusClose();
            };
        }

        private String defeatFocusClose() {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.defeatFocusClose();
                case MAERA -> MaeraDialogue.defeatFocusClose();
                case CASSIA -> CassiaDialogue.defeatFocusClose();
                case LYRA -> LyraDialogue.defeatFocusClose();
                case SAMIR -> SamirDialogue.defeatFocusClose();
                case ARIA -> AriaDialogue.defeatFocusClose();
                case VESPER -> VesperDialogue.defeatFocusClose();
                case RAFIQ -> RafiqDialogue.defeatFocusClose();
                case CALDER -> CalderDialogue.defeatFocusClose();
            };
        }

        String progressRootGreeting(int stage) {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.progressRootGreeting();
                case MAERA -> MaeraDialogue.progressRootGreeting();
                case CASSIA -> CassiaDialogue.progressRootGreeting();
                case LYRA -> LyraDialogue.progressRootGreeting();
                case SAMIR -> SamirDialogue.progressRootGreeting();
                case ARIA -> AriaDialogue.progressRootGreeting();
                case VESPER -> VesperDialogue.progressRootGreeting();
                case RAFIQ -> RafiqDialogue.progressRootGreeting();
                case CALDER -> CalderDialogue.progressRootGreeting();
            };
        }

        String readyRootGreeting(int stage) {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.readyRootGreeting();
                case MAERA -> MaeraDialogue.readyRootGreeting();
                case CASSIA -> CassiaDialogue.readyRootGreeting();
                case LYRA -> LyraDialogue.readyRootGreeting();
                case SAMIR -> SamirDialogue.readyRootGreeting();
                case ARIA -> AriaDialogue.readyRootGreeting();
                case VESPER -> VesperDialogue.readyRootGreeting();
                case RAFIQ -> RafiqDialogue.readyRootGreeting();
                case CALDER -> CalderDialogue.readyRootGreeting();
            };
        }

        String progressLine(int stage) {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.progressLine();
                case MAERA -> MaeraDialogue.progressLine();
                case CASSIA -> CassiaDialogue.progressLine();
                case LYRA -> LyraDialogue.progressLine();
                case SAMIR -> SamirDialogue.progressLine();
                case ARIA -> AriaDialogue.progressLine();
                case VESPER -> VesperDialogue.progressLine();
                case RAFIQ -> RafiqDialogue.progressLine();
                case CALDER -> CalderDialogue.progressLine();
            };
        }

        String originLine() {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.originLine();
                case MAERA -> MaeraDialogue.originLine();
                case CASSIA -> CassiaDialogue.originLine();
                case LYRA -> LyraDialogue.originLine();
                case SAMIR -> SamirDialogue.originLine();
                case ARIA -> AriaDialogue.originLine();
                case VESPER -> VesperDialogue.originLine();
                case RAFIQ -> RafiqDialogue.originLine();
                case CALDER -> CalderDialogue.originLine();
            };
        }

        String concernLine() {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.concernLine();
                case MAERA -> MaeraDialogue.concernLine();
                case CASSIA -> CassiaDialogue.concernLine();
                case LYRA -> LyraDialogue.concernLine();
                case SAMIR -> SamirDialogue.concernLine();
                case ARIA -> AriaDialogue.concernLine();
                case VESPER -> VesperDialogue.concernLine();
                case RAFIQ -> RafiqDialogue.concernLine();
                case CALDER -> CalderDialogue.concernLine();
            };
        }

        String woundLine() {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.woundLine();
                case MAERA -> MaeraDialogue.woundLine();
                case CASSIA -> CassiaDialogue.woundLine();
                case LYRA -> LyraDialogue.woundLine();
                case SAMIR -> SamirDialogue.woundLine();
                case ARIA -> AriaDialogue.woundLine();
                case VESPER -> VesperDialogue.woundLine();
                case RAFIQ -> RafiqDialogue.woundLine();
                case CALDER -> CalderDialogue.woundLine();
            };
        }

        String practicalDetail(int stage) {
            return switch (stage) {
                case 1 -> switch (this) {
                    case SERAPHINE -> SeraphineDialogue.practicalDetailStageOne();
                    case MAERA -> MaeraDialogue.practicalDetailStageOne();
                    case CASSIA -> CassiaDialogue.practicalDetailStageOne();
                    case LYRA -> LyraDialogue.practicalDetailStageOne();
                    case SAMIR -> SamirDialogue.practicalDetailStageOne();
                    case ARIA -> AriaDialogue.practicalDetailStageOne();
                    case VESPER -> VesperDialogue.practicalDetailStageOne();
                    case RAFIQ -> RafiqDialogue.practicalDetailStageOne();
                    case CALDER -> CalderDialogue.practicalDetailStageOne();
                };
                case 2 -> "Small things matter because someone is counting on us to overlook them.";
                case 3 -> "Let objects contradict people. Objects are less ambitious.";
                case 4 -> "Enemies protect the weak point. Notice what they guard hardest.";
                case 5 -> "Do not polish what you find. I need the ugly version.";
                case 6 -> "If I hesitate, do not mistake it for surrender.";
                case 7 -> "Win the opening, not the argument.";
                case 8 -> "We are not erasing the past. We are choosing where it stops.";
                default -> "Move carefully and come back with something the road cannot deny.";
            };
        }

        String practicalClose(int stage) {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.practicalClose();
                case MAERA -> MaeraDialogue.practicalClose();
                case CASSIA -> CassiaDialogue.practicalClose();
                case LYRA -> LyraDialogue.practicalClose();
                case SAMIR -> SamirDialogue.practicalClose();
                case ARIA -> AriaDialogue.practicalClose();
                case VESPER -> VesperDialogue.practicalClose();
                case RAFIQ -> RafiqDialogue.practicalClose();
                case CALDER -> CalderDialogue.practicalClose();
            };
        }

        String readyLine(int stage) {
            return switch (stage) {
                case 5, 6 -> "I am ready to hear the part I avoided.";
                case 7 -> "Now the fight has to become an ending.";
                case 8 -> "Now I choose what comes after.";
                default -> switch (this) {
                    case SERAPHINE -> SeraphineDialogue.readyLine();
                    case MAERA -> MaeraDialogue.readyLine();
                    case CASSIA -> CassiaDialogue.readyLine();
                    case LYRA -> LyraDialogue.readyLine();
                    case SAMIR -> SamirDialogue.readyLine();
                    case ARIA -> AriaDialogue.readyLine();
                    case VESPER -> VesperDialogue.readyLine();
                    case RAFIQ -> RafiqDialogue.readyLine();
                    case CALDER -> CalderDialogue.readyLine();
                };
            };
        }

        String afterQuestLine(int stage) {
            return switch (stage) {
                case 1 -> switch (this) {
                    case SERAPHINE -> SeraphineDialogue.afterQuestStageOne();
                    case MAERA -> MaeraDialogue.afterQuestStageOne();
                    case CASSIA -> CassiaDialogue.afterQuestStageOne();
                    case LYRA -> LyraDialogue.afterQuestStageOne();
                    case SAMIR -> SamirDialogue.afterQuestStageOne();
                    case ARIA -> AriaDialogue.afterQuestStageOne();
                    case VESPER -> VesperDialogue.afterQuestStageOne();
                    case RAFIQ -> RafiqDialogue.afterQuestStageOne();
                    case CALDER -> CalderDialogue.afterQuestStageOne();
                };
                case 2 -> "Useful help is how trust learns to stand.";
                case 3 -> "Evidence has a colder kindness than comfort. It does not look away.";
                case 4 -> "The old wound has enemies now. That means it can be fought.";
                case 5 -> "You heard the truth and did not try to make it prettier. I noticed.";
                case 6 -> "I am still afraid. I am also still here. That distinction matters.";
                case 7 -> "The thing that hunted me has an ending now.";
                case 8 -> "Loyalty is not a chain. It is a road I am choosing with both feet.";
                default -> "This part is done. The trust remains.";
            };
        }

        String marriedGreeting() {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.marriedGreeting();
                case MAERA -> MaeraDialogue.marriedGreeting();
                case CASSIA -> CassiaDialogue.marriedGreeting();
                case LYRA -> LyraDialogue.marriedGreeting();
                case SAMIR -> SamirDialogue.marriedGreeting();
                case ARIA -> AriaDialogue.marriedGreeting();
                case VESPER -> VesperDialogue.marriedGreeting();
                case RAFIQ -> RafiqDialogue.marriedGreeting();
                case CALDER -> CalderDialogue.marriedGreeting();
            };
        }

        String romancedGreeting() {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.romancedGreeting();
                case MAERA -> MaeraDialogue.romancedGreeting();
                case CASSIA -> CassiaDialogue.romancedGreeting();
                case LYRA -> LyraDialogue.romancedGreeting();
                case SAMIR -> SamirDialogue.romancedGreeting();
                case ARIA -> AriaDialogue.romancedGreeting();
                case VESPER -> VesperDialogue.romancedGreeting();
                case RAFIQ -> RafiqDialogue.romancedGreeting();
                case CALDER -> CalderDialogue.romancedGreeting();
            };
        }

        String oathsteadGreeting(DialogueContext context) {
            if (!context.assignedBuildingLabel().isBlank()) {
                String station = context.assignedBuildingLabel()
                        .replaceFirst("(?i)^oathstead\\s+", "")
                        .toLowerCase(Locale.ROOT);
                return switch (this) {
                    case SERAPHINE -> SeraphineDialogue.oathsteadAssignedGreeting(station);
                    case MAERA -> MaeraDialogue.oathsteadAssignedGreeting(station);
                    case CASSIA -> CassiaDialogue.oathsteadAssignedGreeting(station);
                    case LYRA -> LyraDialogue.oathsteadAssignedGreeting(station);
                    case SAMIR -> SamirDialogue.oathsteadAssignedGreeting(station);
                    case ARIA -> AriaDialogue.oathsteadAssignedGreeting(station);
                    case VESPER -> VesperDialogue.oathsteadAssignedGreeting(station);
                    case RAFIQ -> RafiqDialogue.oathsteadAssignedGreeting(station);
                    case CALDER -> CalderDialogue.oathsteadAssignedGreeting(station);
                };
            }
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.oathsteadGreeting();
                case MAERA -> MaeraDialogue.oathsteadGreeting();
                case CASSIA -> CassiaDialogue.oathsteadGreeting();
                case LYRA -> LyraDialogue.oathsteadGreeting();
                case SAMIR -> SamirDialogue.oathsteadGreeting();
                case ARIA -> AriaDialogue.oathsteadGreeting();
                case VESPER -> VesperDialogue.oathsteadGreeting();
                case RAFIQ -> RafiqDialogue.oathsteadGreeting();
                case CALDER -> CalderDialogue.oathsteadGreeting();
            };
        }

        String oathsteadLine(DialogueContext context) {
            if (hasFemaleOutcomeVoice()) {
                return femaleOathsteadLine(context);
            }
            if (context.stationedAtOathstead()) {
                return "I thought a camp would feel temporary. This one keeps asking people to become less temporary with it.";
            }
            return "Oathstead is not polished enough to lie well. I find that almost comforting.";
        }

        String oathsteadHomeLine(DialogueContext context) {
            return switch (this) {
                case SERAPHINE -> femaleOathsteadHomeLine(context);
                case MAERA -> femaleOathsteadHomeLine(context);
                case CASSIA -> femaleOathsteadHomeLine(context);
                case LYRA -> femaleOathsteadHomeLine(context);
                case SAMIR -> SamirDialogue.oathsteadHomeLine(context);
                case ARIA -> femaleOathsteadHomeLine(context);
                case VESPER -> VesperDialogue.oathsteadHomeLine(context);
                case RAFIQ -> RafiqDialogue.oathsteadHomeLine(context);
                case CALDER -> CalderDialogue.oathsteadHomeLine(context);
            };
        }

        String assignedWorkLine(DialogueContext context) {
            if (context.assignedBuildingLabel().isBlank()) {
                return "Give me a place to work and I will make it earn its roof.";
            }
            String role = context.workerRoleLabel().isBlank() ? "work" : context.workerRoleLabel().toLowerCase();
            return "The " + context.assignedBuildingLabel().toLowerCase() + " gives me " + role
                    + ". It is good to have hands busy with something that builds instead of only survives.";
        }

        String oathsteadNeedLine() {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.oathsteadNeedLine();
                case MAERA -> MaeraDialogue.oathsteadNeedLine();
                case CASSIA -> CassiaDialogue.oathsteadNeedLine();
                case LYRA -> LyraDialogue.oathsteadNeedLine();
                case SAMIR -> SamirDialogue.oathsteadNeedLine();
                case ARIA -> AriaDialogue.oathsteadNeedLine();
                case VESPER -> VesperDialogue.oathsteadNeedLine();
                case RAFIQ -> RafiqDialogue.oathsteadNeedLine();
                case CALDER -> CalderDialogue.oathsteadNeedLine();
            };
        }

        private String femaleOathsteadLine(DialogueContext context) {
            if (this == VESPER) {
                return VesperDialogue.oathsteadLine(context);
            }
            if (this == SERAPHINE && !context.questOutcome("seraphine_oathstead_promise").isBlank()) {
                return "Oathstead has not asked me to sign away a single part of myself. Suspicious. Generous. I am learning the difference.";
            }
            if (this == MAERA && !context.questOutcome("maera_oathstead_archive").isBlank()) {
                return "Oathstead's archive is young enough to be honest on purpose. I am trying not to frighten it with standards too quickly.";
            }
            if (this == CASSIA && !context.questOutcome("cassia_oathstead_duty").isBlank()) {
                return "Oathstead asks for duty without demanding I disappear inside it. I still do not quite trust how much I need that.";
            }
            if (this == LYRA && !context.questOutcome("lyra_oathstead_clinic").isBlank()) {
                return "Oathstead's clinic is becoming the kind of mercy that arrives before panic. That may be the first cure I ever helped build.";
            }
            if (this == ARIA && !context.questOutcome("aria_oathstead_future").isBlank()) {
                return "Oathstead has roads that come back. I keep noticing that, which is rude of the roads and probably important.";
            }
            return "Oathstead is not polished enough to lie well. I find that almost comforting.";
        }

        private String femaleOathsteadHomeLine(DialogueContext context) {
            if (this == VESPER) {
                return VesperDialogue.oathsteadHomeLine(context);
            }
            if (this == SERAPHINE) {
                return !context.questOutcome("seraphine_oathstead_promise").isBlank()
                        ? "It feels like a promise I can leave and still choose again tomorrow. I did not know a place could be that impolite to old fear."
                        : "It feels like a promise without hidden ink. I keep checking anyway.";
            }
            if (this == MAERA) {
                return !context.questOutcome("maera_oathstead_archive").isBlank()
                        ? "It feels like a first draft with room for correction. That is almost my definition of hope."
                        : "It feels like a map table before the first honest line is drawn.";
            }
            if (this == CASSIA) {
                return !context.questOutcome("cassia_oathstead_duty").isBlank()
                        ? "It feels worth defending without asking me to become stone. I am still learning that difference."
                        : "It feels badly defended and worth defending. That is enough to start.";
            }
            if (this == LYRA) {
                return !context.questOutcome("lyra_oathstead_clinic").isBlank()
                        ? "It feels like care allowed to stop running long enough to become a place."
                        : "It feels wounded, which means it is still alive and asking for care.";
            }
            if (this == ARIA) {
                return !context.questOutcome("aria_oathstead_future").isBlank()
                        ? "It feels like a door I can use twice and a road that will not mock me for returning."
                        : "It feels like a door I might use twice. Do not make a ceremony of that.";
            }
            return "It feels like soil that has not decided what it can grow. I respect that.";
        }

        String checkInOpening(int relationship, DialogueContext context) {
            if (relationship >= 150) {
                return "You ask like you intend to hear the answer. That still catches me off guard.";
            }
            if (relationship >= 50) {
                return switch (this) {
                    case SERAPHINE -> SeraphineDialogue.checkInOpeningTrusted();
                    case MAERA -> MaeraDialogue.checkInOpeningTrusted();
                    case CASSIA -> CassiaDialogue.checkInOpeningTrusted();
                    case LYRA -> LyraDialogue.checkInOpeningTrusted();
                    case SAMIR -> SamirDialogue.checkInOpeningTrusted();
                    case ARIA -> AriaDialogue.checkInOpeningTrusted();
                    case VESPER -> VesperDialogue.checkInOpeningTrusted();
                    case RAFIQ -> RafiqDialogue.checkInOpeningTrusted();
                    case CALDER -> CalderDialogue.checkInOpeningTrusted();
                };
            }
            return "I am not used to being asked that directly, but go on.";
        }

        String feelingLine(int relationship, DialogueContext context) {
            if (relationship >= 150) {
                return switch (this) {
                    case SERAPHINE -> SeraphineDialogue.feelingLineCommitted();
                    case MAERA -> MaeraDialogue.feelingLineCommitted();
                    case CASSIA -> CassiaDialogue.feelingLineCommitted();
                    case LYRA -> LyraDialogue.feelingLineCommitted();
                    case SAMIR -> SamirDialogue.feelingLineCommitted();
                    case ARIA -> AriaDialogue.feelingLineCommitted();
                    case VESPER -> VesperDialogue.feelingLineCommitted();
                    case RAFIQ -> RafiqDialogue.feelingLineCommitted();
                    case CALDER -> CalderDialogue.feelingLineCommitted();
                };
            }
            if (relationship >= 50) {
                return switch (this) {
                    case SERAPHINE -> SeraphineDialogue.feelingLineTrusted();
                    case MAERA -> MaeraDialogue.feelingLineTrusted();
                    case CASSIA -> CassiaDialogue.feelingLineTrusted();
                    case LYRA -> LyraDialogue.feelingLineTrusted();
                    case SAMIR -> SamirDialogue.feelingLineTrusted();
                    case ARIA -> AriaDialogue.feelingLineTrusted();
                    case VESPER -> VesperDialogue.feelingLineTrusted();
                    case RAFIQ -> RafiqDialogue.feelingLineTrusted();
                    case CALDER -> CalderDialogue.feelingLineTrusted();
                };
            }
            return "Alert. Tired. Not ready to answer fully.";
        }

        String nextStepLine(int relationship, DialogueContext context) {
            if (!context.assignedBuildingLabel().isBlank()) {
                return "Keep Oathstead useful. The " + context.assignedBuildingLabel().toLowerCase()
                        + " is not just a building if people rely on it.";
            }
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.nextStepLine();
                case MAERA -> MaeraDialogue.nextStepLine();
                case CASSIA -> CassiaDialogue.nextStepLine();
                case LYRA -> LyraDialogue.nextStepLine();
                case SAMIR -> SamirDialogue.nextStepLine();
                case ARIA -> AriaDialogue.nextStepLine();
                case VESPER -> VesperDialogue.nextStepLine();
                case RAFIQ -> RafiqDialogue.nextStepLine();
                case CALDER -> CalderDialogue.nextStepLine();
            };
        }

        String needLine(int relationship, DialogueContext context) {
            if (relationship >= 120) {
                return "Patience when I repeat myself. Honesty when I pretend I am fine. It is an annoying list, but it is the honest one.";
            }
            if (relationship >= 50) {
                return "Clear roads, fewer assumptions, and maybe dry socks. I will settle for one.";
            }
            return "Space, for now. Useful help later.";
        }

        String feelingRespectResponse() {
            return "Thank you. Sometimes I need silence, not because I distrust you, but because I am still thinking.";
        }

        String feelingHonestResponse(int relationship) {
            if (relationship >= 120) {
                return "The honest version is that I am better with you near and irritated that I cannot make that sound tactical.";
            }
            return "The honest version is that I am tired, and I did not plan to admit even that much.";
        }

        String feelingMissionResponse() {
            return "We can keep moving. Just do not mistake that for me being all right.";
        }

        String nextAcceptResponse() {
            return "Good. Then we have a plan, at least for the next step.";
        }

        String nextWorryResponse() {
            return "I worry we will move too fast because the danger feels urgent. Urgent is not always wise.";
        }

        String nextDisagreeResponse() {
            return "Then make it a plan. I can argue with a plan. I cannot follow a mood.";
        }

        String needSupportResponse() {
            return "Then I will try to ask before I go quiet and make you guess.";
        }

        String needDirectResponse() {
            return "Directly. Uncomfortable, but probably healthier. I will try.";
        }

        String needRefuseResponse() {
            return "All right. I do not like it, but I heard you.";
        }

        String milestoneOpening(int threshold, int relationship, DialogueContext context) {
            if (hasFemaleOutcomeVoice()) {
                return femaleMilestoneOpening(threshold, context);
            }
            return switch (threshold) {
                case 50 -> "I used to measure you by exits. Now I measure you by whether I need one. That is not trust yet, but it is no longer nothing.";
                case 100 -> "There is something I have not said because saying it makes it harder to pretend I am only traveling beside you.";
                case 150 -> "Loyalty is a dangerous word. Too many people use it when they mean ownership. I am using it carefully.";
                case 180 -> "This has crossed the border between useful trust and something less obedient. We should name that before it names us badly.";
                case 250 -> "The road keeps trying to end us. I have started imagining what remains if it fails.";
                default -> "Something has changed between us. It deserves better than being stepped around.";
            };
        }

        String milestoneWarmChoice(int threshold) {
            if (hasFemaleOutcomeVoice()) {
                return femaleMilestoneWarmChoice(threshold);
            }
            return switch (threshold) {
                case 50 -> "I respect you too.";
                case 100 -> "You can tell me.";
                case 150 -> "Choose your road with me.";
                case 180 -> "Let us name it gently.";
                case 250 -> "Imagine that future with me.";
                default -> "Let us talk about it.";
            };
        }

        String milestoneBoundaryChoice(int threshold) {
            if (hasFemaleOutcomeVoice()) {
                return femaleMilestoneBoundaryChoice(threshold);
            }
            return switch (threshold) {
                case 50 -> "Keep your distance if you need it.";
                case 100 -> "Only say what you can bear.";
                case 150 -> "I will not make loyalty a chain.";
                case 180 -> "I do not want to rush this.";
                case 250 -> "I cannot promise forever today.";
                default -> "We can move carefully.";
            };
        }

        String milestoneTruthLine(int threshold, int relationship, DialogueContext context) {
            return switch (threshold) {
                case 50 -> switch (this) {
                    case SERAPHINE -> SeraphineDialogue.milestoneTruthLine(threshold);
                    case MAERA -> MaeraDialogue.milestoneTruthLine(threshold);
                    case CASSIA -> CassiaDialogue.milestoneTruthLine(threshold);
                    case LYRA -> LyraDialogue.milestoneTruthLine(threshold);
                    case SAMIR -> SamirDialogue.milestoneTruthLine(threshold);
                    case ARIA -> AriaDialogue.milestoneTruthLine(threshold);
                    case VESPER -> VesperDialogue.milestoneTruthLine(threshold);
                    case RAFIQ -> RafiqDialogue.milestoneTruthLine(threshold);
                    case CALDER -> CalderDialogue.milestoneTruthLine(threshold);
                };
                case 100 -> switch (this) {
                    case SERAPHINE -> SeraphineDialogue.milestoneTruthLine(threshold);
                    case MAERA -> MaeraDialogue.milestoneTruthLine(threshold);
                    case CASSIA -> CassiaDialogue.milestoneTruthLine(threshold);
                    case LYRA -> LyraDialogue.milestoneTruthLine(threshold);
                    case SAMIR -> SamirDialogue.milestoneTruthLine(threshold);
                    case ARIA -> AriaDialogue.milestoneTruthLine(threshold);
                    case VESPER -> VesperDialogue.milestoneTruthLine(threshold);
                    case RAFIQ -> RafiqDialogue.milestoneTruthLine(threshold);
                    case CALDER -> CalderDialogue.milestoneTruthLine(threshold);
                };
                case 150 -> switch (this) {
                    case SERAPHINE -> SeraphineDialogue.milestoneTruthLine(threshold);
                    case MAERA -> MaeraDialogue.milestoneTruthLine(threshold);
                    case CASSIA -> CassiaDialogue.milestoneTruthLine(threshold);
                    case LYRA -> LyraDialogue.milestoneTruthLine(threshold);
                    case SAMIR -> SamirDialogue.milestoneTruthLine(threshold);
                    case ARIA -> AriaDialogue.milestoneTruthLine(threshold);
                    case VESPER -> VesperDialogue.milestoneTruthLine(threshold);
                    case RAFIQ -> RafiqDialogue.milestoneTruthLine(threshold);
                    case CALDER -> CalderDialogue.milestoneTruthLine(threshold);
                };
                case 180 -> switch (this) {
                    case SERAPHINE -> SeraphineDialogue.milestoneTruthLine(threshold);
                    case MAERA -> MaeraDialogue.milestoneTruthLine(threshold);
                    case CASSIA -> CassiaDialogue.milestoneTruthLine(threshold);
                    case LYRA -> LyraDialogue.milestoneTruthLine(threshold);
                    case SAMIR -> SamirDialogue.milestoneTruthLine(threshold);
                    case ARIA -> AriaDialogue.milestoneTruthLine(threshold);
                    case VESPER -> VesperDialogue.milestoneTruthLine(threshold);
                    case RAFIQ -> RafiqDialogue.milestoneTruthLine(threshold);
                    case CALDER -> CalderDialogue.milestoneTruthLine(threshold);
                };
                case 250 -> switch (this) {
                    case SERAPHINE -> SeraphineDialogue.milestoneTruthLine(threshold);
                    case MAERA -> MaeraDialogue.milestoneTruthLine(threshold);
                    case CASSIA -> CassiaDialogue.milestoneTruthLine(threshold);
                    case LYRA -> LyraDialogue.milestoneTruthLine(threshold);
                    case SAMIR -> SamirDialogue.milestoneTruthLine(threshold);
                    case ARIA -> AriaDialogue.milestoneTruthLine(threshold);
                    case VESPER -> VesperDialogue.milestoneTruthLine(threshold);
                    case RAFIQ -> RafiqDialogue.milestoneTruthLine(threshold);
                    case CALDER -> CalderDialogue.milestoneTruthLine(threshold);
                };
                default -> "The honest version is simple and therefore terrifying: you matter.";
            };
        }

        String milestoneWarmLine(int threshold, int relationship, DialogueContext context) {
            if (hasFemaleOutcomeVoice()) {
                return femaleMilestoneWarmLine(threshold, context);
            }
            return switch (threshold) {
                case 50 -> "Then we begin there. Respect first. Trust can stop pretending it was born fully armed.";
                case 100 -> "Good. I will tell it badly at first. Be patient with the parts that limp.";
                case 150 -> switch (this) {
                    case SERAPHINE -> SeraphineDialogue.milestoneWarmLine(threshold);
                    case MAERA -> MaeraDialogue.milestoneWarmLine(threshold);
                    case CASSIA -> CassiaDialogue.milestoneWarmLine(threshold);
                    case LYRA -> LyraDialogue.milestoneWarmLine(threshold);
                    case SAMIR -> SamirDialogue.milestoneWarmLine(threshold);
                    case ARIA -> AriaDialogue.milestoneWarmLine(threshold);
                    case VESPER -> VesperDialogue.milestoneWarmLine(threshold);
                    case RAFIQ -> RafiqDialogue.milestoneWarmLine(threshold);
                    case CALDER -> CalderDialogue.milestoneWarmLine(threshold);
                };
                case 180 -> context.romanced()
                        ? romanceDateSincereLine(context)
                        : romanceDateFlirtLine(context);
                case 250 -> context.married()
                        ? "We have already made one promise. Let us keep making it in smaller, stubborn ways."
                        : switch (this) {
                            case SERAPHINE -> SeraphineDialogue.milestoneWarmLine(threshold);
                            case MAERA -> MaeraDialogue.milestoneWarmLine(threshold);
                            case CASSIA -> CassiaDialogue.milestoneWarmLine(threshold);
                            case LYRA -> LyraDialogue.milestoneWarmLine(threshold);
                            case SAMIR -> SamirDialogue.milestoneWarmLine(threshold);
                            case ARIA -> AriaDialogue.milestoneWarmLine(threshold);
                            case VESPER -> VesperDialogue.milestoneWarmLine(threshold);
                            case RAFIQ -> RafiqDialogue.milestoneWarmLine(threshold);
                            case CALDER -> CalderDialogue.milestoneWarmLine(threshold);
                        };
                default -> "Then let this be spoken plainly enough to survive tomorrow.";
            };
        }

        String milestoneBoundaryLine(int threshold, int relationship, DialogueContext context) {
            if (hasFemaleOutcomeVoice()) {
                return femaleMilestoneBoundaryLine(threshold, context);
            }
            return switch (threshold) {
                case 50 -> "Distance is not rejection. Sometimes it is how respect learns not to bruise.";
                case 100 -> "Good. A secret forced open becomes another wound.";
                case 150 -> "That is why I can offer it. A chain would have made me run. Choice lets me stay.";
                case 180 -> romanceDateBoundaryLine(context);
                case 250 -> "Then promise the honest part: no forever stolen from fear, no future spoken only because the night is warm.";
                default -> "Careful is not cowardly. Careless courage has killed better people than us.";
            };
        }

        String milestoneAcceptLine(int threshold) {
            if (hasFemaleOutcomeVoice()) {
                return femaleMilestoneAcceptLine(threshold);
            }
            return switch (threshold) {
                case 50 -> "Then respect stands. Small foundation, real stone.";
                case 100 -> "Then I will say more when I can breathe around it.";
                case 150 -> "Then we stand as chosen allies. That word still has weight. Good.";
                case 180 -> "Then we move gently and honestly. I can do one of those easily. I will practice the other.";
                case 250 -> "Then the future remains possible. Not promised into a cage. Possible.";
                default -> "Then this moment has somewhere to live.";
            };
        }

        String milestoneChangeLine(int threshold) {
            if (hasFemaleOutcomeVoice()) {
                return femaleMilestoneChangeLine(threshold);
            }
            return switch (threshold) {
                case 50 -> "Changed things are not always broken. Sometimes they are finally named.";
                case 100 -> "Yes. But not all change is a threat. I am trying to learn that before it learns me.";
                case 150 -> "We keep choosing it, then. Loyalty should stay awake.";
                case 180 -> "Time is allowed. Wanting does not become wiser by sprinting.";
                case 250 -> "A future earned slowly may hold better than one sworn too loudly.";
                default -> "Then we let the change breathe.";
            };
        }

        private String femaleMilestoneOpening(int threshold, DialogueContext context) {
            if (this == VESPER) {
                return VesperDialogue.milestoneOpening(threshold, context);
            }
            return switch (threshold) {
                case 50 -> switch (this) {
                    case SERAPHINE -> SeraphineDialogue.milestoneOpening(threshold, context);
                    case MAERA -> MaeraDialogue.milestoneOpening(threshold, context);
                    case CASSIA -> CassiaDialogue.milestoneOpening(threshold, context);
                    case LYRA -> LyraDialogue.milestoneOpening(threshold, context);
                    case ARIA -> AriaDialogue.milestoneOpening(threshold, context);
                    default -> "I have begun respecting the shape of your choices.";
                };
                case 100 -> switch (this) {
                    case SERAPHINE -> SeraphineDialogue.milestoneOpening(threshold, context);
                    case MAERA -> MaeraDialogue.milestoneOpening(threshold, context);
                    case CASSIA -> CassiaDialogue.milestoneOpening(threshold, context);
                    case LYRA -> LyraDialogue.milestoneOpening(threshold, context);
                    case ARIA -> AriaDialogue.milestoneOpening(threshold, context);
                    default -> "There is something I have not said because saying it makes it harder to keep distance.";
                };
                case 150 -> switch (this) {
                    case SERAPHINE -> SeraphineDialogue.milestoneOpening(threshold, context);
                    case MAERA -> MaeraDialogue.milestoneOpening(threshold, context);
                    case CASSIA -> CassiaDialogue.milestoneOpening(threshold, context);
                    case LYRA -> LyraDialogue.milestoneOpening(threshold, context);
                    case ARIA -> AriaDialogue.milestoneOpening(threshold, context);
                    default -> "Loyalty is a choice I am making with my eyes open.";
                };
                case 180 -> switch (this) {
                    case SERAPHINE -> SeraphineDialogue.milestoneOpening(threshold, context);
                    case MAERA -> MaeraDialogue.milestoneOpening(threshold, context);
                    case CASSIA -> CassiaDialogue.milestoneOpening(threshold, context);
                    case LYRA -> LyraDialogue.milestoneOpening(threshold, context);
                    case ARIA -> AriaDialogue.milestoneOpening(threshold, context);
                    default -> "This has crossed into something warmer than tactics. We should name it gently.";
                };
                case 250 -> switch (this) {
                    case SERAPHINE -> SeraphineDialogue.milestoneOpening(threshold, context);
                    case MAERA -> MaeraDialogue.milestoneOpening(threshold, context);
                    case CASSIA -> CassiaDialogue.milestoneOpening(threshold, context);
                    case LYRA -> LyraDialogue.milestoneOpening(threshold, context);
                    case ARIA -> AriaDialogue.milestoneOpening(threshold, context);
                    default -> "The road keeps trying to end us. I have started imagining what remains if it fails.";
                };
                default -> "Something has changed between us. It deserves better than being stepped around.";
            };
        }

        private String femaleMilestoneWarmChoice(int threshold) {
            if (this == VESPER) {
                return VesperDialogue.milestoneWarmChoice(threshold);
            }
            return switch (threshold) {
                case 50 -> "I respect you too.";
                case 100 -> "You can tell me.";
                case 150 -> "Choose this road with me.";
                case 180 -> "Let me ask properly.";
                case 250 -> "Imagine that future with me.";
                default -> "Let us talk about it.";
            };
        }

        private String femaleMilestoneBoundaryChoice(int threshold) {
            if (this == VESPER) {
                return VesperDialogue.milestoneBoundaryChoice(threshold);
            }
            return switch (threshold) {
                case 50 -> "Keep your distance if you need it.";
                case 100 -> "Only say what you can bear.";
                case 150 -> "I will not make loyalty a chain.";
                case 180 -> "We can let this grow slowly.";
                case 250 -> "Do not promise forever out of fear.";
                default -> "We can move carefully.";
            };
        }

        private String femaleMilestoneWarmLine(int threshold, DialogueContext context) {
            if (this == VESPER) {
                return VesperDialogue.milestoneWarmLine(threshold, context);
            }
            return switch (threshold) {
                case 50 -> "Then let respect be the first honest plank. Not enough for a bridge, but enough to stop pretending the river is not there.";
                case 100 -> switch (this) {
                    case SERAPHINE -> SeraphineDialogue.milestoneWarmLine(threshold, context);
                    case MAERA -> MaeraDialogue.milestoneWarmLine(threshold, context);
                    case CASSIA -> CassiaDialogue.milestoneWarmLine(threshold, context);
                    case LYRA -> LyraDialogue.milestoneWarmLine(threshold, context);
                    case ARIA -> AriaDialogue.milestoneWarmLine(threshold, context);
                    default -> "Good. I will tell it badly at first. Be patient with the parts that limp.";
                };
                case 150 -> switch (this) {
                    case SERAPHINE -> SeraphineDialogue.milestoneWarmLine(threshold, context);
                    case MAERA -> MaeraDialogue.milestoneWarmLine(threshold, context);
                    case CASSIA -> CassiaDialogue.milestoneWarmLine(threshold, context);
                    case LYRA -> LyraDialogue.milestoneWarmLine(threshold, context);
                    case ARIA -> AriaDialogue.milestoneWarmLine(threshold, context);
                    default -> "Then we stand as chosen allies.";
                };
                case 180 -> context.romanced()
                        ? romanceDateSincereLine(context)
                        : romanceDateFlirtLine(context);
                case 250 -> context.married()
                        ? "We have already made one promise. Let us keep making it in smaller, stubborn ways."
                        : switch (this) {
                            case SERAPHINE -> SeraphineDialogue.milestoneWarmLine(threshold, context);
                            case MAERA -> MaeraDialogue.milestoneWarmLine(threshold, context);
                            case CASSIA -> CassiaDialogue.milestoneWarmLine(threshold, context);
                            case LYRA -> LyraDialogue.milestoneWarmLine(threshold, context);
                            case ARIA -> AriaDialogue.milestoneWarmLine(threshold, context);
                            default -> "Then we let the future stay possible and chosen.";
                        };
                default -> "Then let this be spoken plainly enough to survive tomorrow.";
            };
        }

        private String femaleMilestoneBoundaryLine(int threshold, DialogueContext context) {
            if (this == VESPER) {
                return VesperDialogue.milestoneBoundaryLine(threshold);
            }
            return switch (threshold) {
                case 50 -> "Distance is not rejection. Sometimes it is how respect learns not to bruise.";
                case 100 -> "Good. A secret forced open becomes another wound.";
                case 150 -> "That is why I can offer it. A chain would have made me run or harden. Choice lets me stay.";
                case 180 -> romanceDateBoundaryLine(context);
                case 250 -> "Then promise the honest part: no forever stolen from fear, no future spoken only because the night is warm.";
                default -> "Careful is not cowardly. Careless courage has killed better people than us.";
            };
        }

        private String femaleMilestoneAcceptLine(int threshold) {
            if (this == VESPER) {
                return VesperDialogue.milestoneAcceptLine(threshold);
            }
            return switch (threshold) {
                case 50 -> "Then respect stands. Small foundation, real stone.";
                case 100 -> "Then I will say more when I can breathe around it.";
                case 150 -> "Then we stand as chosen allies. That word still has weight. Good.";
                case 180 -> "Then we move gently and honestly. I can do one of those easily. I will practice the other.";
                case 250 -> "Then the future remains possible. Not promised into a cage. Possible.";
                default -> "Then this moment has somewhere to live.";
            };
        }

        private String femaleMilestoneChangeLine(int threshold) {
            if (this == VESPER) {
                return VesperDialogue.milestoneChangeLine(threshold);
            }
            return switch (threshold) {
                case 50 -> "Changed things are not always broken. Sometimes they are finally named.";
                case 100 -> "Yes. But not all change is a threat. I am trying to learn that before it learns me.";
                case 150 -> "We keep choosing it, then. Loyalty should stay awake.";
                case 180 -> "Time is allowed. Wanting does not become wiser by sprinting.";
                case 250 -> "A future earned slowly may hold better than one sworn too loudly.";
                default -> "Then we let the change breathe.";
            };
        }

        String valuesOpening(int relationship, DialogueContext context) {
            if (relationship >= 150) {
                return "You want the honest answer? I judge you by " + context.approvalMotive()
                        + ", and lately I have had fewer reasons to doubt you.";
            }
            if (relationship >= 60) {
                return "I am still deciding what your choices mean. One good moment is not a pattern.";
            }
            return "It is too early for certainty. I can tell you what I have noticed so far.";
        }

        String valuesTrustLine(int relationship, DialogueContext context) {
            if (relationship >= 150) {
                return switch (this) {
                    case SERAPHINE -> SeraphineDialogue.valuesTrustLine();
                    case MAERA -> MaeraDialogue.valuesTrustLine();
                    case CASSIA -> CassiaDialogue.valuesTrustLine();
                    case LYRA -> LyraDialogue.valuesTrustLine();
                    case SAMIR -> SamirDialogue.valuesTrustLine();
                    case ARIA -> AriaDialogue.valuesTrustLine();
                    case VESPER -> VesperDialogue.valuesTrustLine(context);
                    case RAFIQ -> RafiqDialogue.valuesTrustLine();
                    case CALDER -> CalderDialogue.valuesTrustLine();
                };
            }
            return "I trust you most when what you do matches what you said you would do.";
        }

        String valuesWorryLine(int relationship, DialogueContext context) {
            if (relationship >= 120) {
                return switch (this) {
                    case SERAPHINE -> SeraphineDialogue.valuesWorryLine();
                    case MAERA -> MaeraDialogue.valuesWorryLine();
                    case CASSIA -> CassiaDialogue.valuesWorryLine();
                    case LYRA -> LyraDialogue.valuesWorryLine();
                    case SAMIR -> SamirDialogue.valuesWorryLine();
                    case ARIA -> AriaDialogue.valuesWorryLine();
                    case VESPER -> VesperDialogue.valuesWorryLine();
                    case RAFIQ -> RafiqDialogue.valuesWorryLine();
                    case CALDER -> CalderDialogue.valuesWorryLine();
                };
            }
            return "That you will act quickly and call it judgment. Most people do that when they are afraid.";
        }

        String valuesRequestLine(int relationship, DialogueContext context) {
            if (!context.activeSoftRequestText().isBlank()) {
                return "You already carry this from me: " + context.activeSoftRequestText()
                        + " I do not need a speech about it. I need you to remember it when it becomes inconvenient.";
            }
            if (relationship >= 120) {
                return switch (this) {
                    case SERAPHINE -> SeraphineDialogue.valuesRequestLine();
                    case MAERA -> MaeraDialogue.valuesRequestLine();
                    case CASSIA -> CassiaDialogue.valuesRequestLine();
                    case LYRA -> LyraDialogue.valuesRequestLine();
                    case SAMIR -> SamirDialogue.valuesRequestLine();
                    case ARIA -> AriaDialogue.valuesRequestLine();
                    case VESPER -> VesperDialogue.valuesRequestLine();
                    case RAFIQ -> RafiqDialogue.valuesRequestLine();
                    case CALDER -> CalderDialogue.valuesRequestLine();
                };
            }
            return "Remember that " + context.approvalMotive() + " matters to me. It is how I decide whether to stand near someone.";
        }

        String requestAcceptedLine(DialogueContext context) {
            return "Good. I will hold you to that.";
        }

        String requestHonestLine() {
            return "That may be the healthier promise. Perfect promises usually break loudly.";
        }

        String requestRefusedLine() {
            return "Then I know where the line is. I do not like it, but I prefer honesty.";
        }

        String sharedTableOpening(int relationship, DialogueContext context) {
            String place = context.sharedTableLabel().isBlank() ? "this table" : context.sharedTableLabel();
            if (hasFemaleOutcomeVoice()) {
                return femaleSharedTableOpening(relationship, context, place);
            }
            if (relationship >= 150) {
                return "At " + place + ", even quiet has room to sit down. Stay if you mean to stay.";
            }
            if (relationship >= 60) {
                return place + " makes people talk like the road cannot overhear. I am not sure I trust that. I may use it anyway.";
            }
            return place + " is warmer than the road. That does not automatically make it safe.";
        }

        String sharedTableStayLine(int relationship, DialogueContext context) {
            if (hasFemaleOutcomeVoice()) {
                return femaleSharedTableStayLine(relationship, context);
            }
            if (relationship >= 150) {
                return switch (this) {
                    case SERAPHINE -> SeraphineDialogue.sharedTableStayLineCommitted();
                    case MAERA -> MaeraDialogue.sharedTableStayLineCommitted();
                    case CASSIA -> CassiaDialogue.sharedTableStayLineCommitted();
                    case LYRA -> LyraDialogue.sharedTableStayLineCommitted();
                    case SAMIR -> SamirDialogue.sharedTableStayLineCommitted();
                    case ARIA -> AriaDialogue.sharedTableStayLineCommitted();
                    case VESPER -> VesperDialogue.sharedTableStayLineCommitted();
                    case RAFIQ -> RafiqDialogue.sharedTableStayLineCommitted();
                    case CALDER -> CalderDialogue.sharedTableStayLineCommitted();
                };
            }
            return "A while, then. No grand confessions. Just enough quiet to prove we can survive it.";
        }

        String sharedTableRequestLine(int relationship, DialogueContext context) {
            if (this == SERAPHINE && !context.questOutcome("seraphine_oathstead_promise").isBlank()) {
                return "Tonight I ask you not to turn my freedom into a rescue story. Sit with me like someone who trusts I chose the chair myself.";
            }
            if (this == MAERA && !context.questOutcome("maera_oathstead_archive").isBlank()) {
                return "Tonight I ask for one honest record: what we meant, what we feared, and what we refused to edit out.";
            }
            if (this == CASSIA && !context.questOutcome("cassia_oathstead_duty").isBlank()) {
                return "Tonight I ask you to notice when I turn duty into a wall. I am trying to stand without disappearing behind it.";
            }
            if (this == LYRA && !context.questOutcome("lyra_oathstead_clinic").isBlank()) {
                return "Tonight I ask you to remind me that a clinic is not a shrine to exhaustion. Care must survive the healer too.";
            }
            if (this == ARIA && !context.questOutcome("aria_oathstead_future").isBlank()) {
                return "Tonight I ask you not to call it running when I need the sky, or leaving when I come back by choice.";
            }
            if (this == VESPER) {
                String vesperLine = VesperDialogue.sharedTableRequestLine(context);
                if (!vesperLine.isBlank()) {
                    return vesperLine;
                }
            }
            if (!context.activeSoftRequestText().isBlank()) {
                return "Tonight I only ask that you keep carrying what I already asked: " + context.activeSoftRequestText();
            }
            if (relationship >= 120) {
                return valuesRequestLine(relationship, context);
            }
            return "Ask before assuming. Listen before fixing. If that sounds small, you have not tried it under pressure.";
        }

        String epilogueLine(int relationship, DialogueContext context) {
            if (hasFemaleOutcomeVoice()) {
                return femaleEpilogueLine(relationship, context);
            }
            if (context.epilogueReady()) {
                if (relationship >= 150) {
                    return "After came. Somehow. Now the question is not whether we survive the ending, but what kind of people we remain after it.";
                }
                return "The ending happened and left work behind. Endings do that when they are honest.";
            }
            if (relationship >= 150) {
                return "After all this? I try not to name it too often. Named futures can frighten easily. But I have one.";
            }
            return "After all this, we count who is breathing and do not insult the dead by pretending it was simple.";
        }

        String epilogueTogetherLine(DialogueContext context) {
            if (context.epilogueReady()) {
                return "Then we begin there. Not as a reward. As a choice made after seeing the cost.";
            }
            return "Say that again when the road is quiet. I want to know whether it still sounds true.";
        }

        String epilogueEarnLine() {
            return "Earned futures hold better. Less shine, more weight. I can work with that.";
        }

        String epilogueSurviveLine() {
            return "Survive first. Yes. But do not let survival become the only language you remember.";
        }

        private String femaleSharedTableOpening(int relationship, DialogueContext context, String place) {
            if (this == SERAPHINE && !context.questOutcome("seraphine_oathstead_promise").isBlank() && relationship >= 150) {
                return "At " + place + ", nobody is pricing the chair, the hour, or my name. Sit down before I make a joke and ruin how much that matters.";
            }
            if (this == MAERA && !context.questOutcome("maera_oathstead_archive").isBlank() && relationship >= 150) {
                return place + " is a terrible archive and an excellent witness. Stay. I want a record that smells faintly of bad ale and honesty.";
            }
            if (this == CASSIA && !context.questOutcome("cassia_oathstead_duty").isBlank() && relationship >= 150) {
                return place + " has two exits, three weak chairs, and no orders waiting for me. I am trying to understand why that feels like a victory.";
            }
            if (this == LYRA && !context.questOutcome("lyra_oathstead_clinic").isBlank() && relationship >= 150) {
                return place + " is warm, crowded, and no one has bled on the table yet. An unusually persuasive setting for staying.";
            }
            if (this == ARIA && !context.questOutcome("aria_oathstead_future").isBlank() && relationship >= 150) {
                return place + " is crowded enough to vanish in and quiet enough to choose not to. Sit with me before I overthink that.";
            }
            return VesperDialogue.sharedTableOpening(relationship, context, place);
        }

        private String femaleSharedTableStayLine(int relationship, DialogueContext context) {
            if (this == SERAPHINE && !context.questOutcome("seraphine_oathstead_promise").isBlank()) {
                return "Good. I am practicing being present without owing anyone a performance. Do not look too impressed. It will become a habit.";
            }
            if (this == MAERA && !context.questOutcome("maera_vault_truth").isBlank()) {
                return "Good. Some truths are easier to shelve after someone else has heard the sound they made falling.";
            }
            if (this == CASSIA && !context.questOutcome("cassia_ironwall_trial").isBlank()) {
                return "Good. The trial is over, but my shoulders keep waiting for the next verdict. Sitting may confuse them into peace.";
            }
            if (this == LYRA && !context.questOutcome("lyra_fever_cure").isBlank()) {
                return "Good. I have watched enough people breathe badly. Tonight I would like to hear breathing that does not need counting.";
            }
            if (this == ARIA && !context.questOutcome("aria_sister_truth").isBlank()) {
                return "Good. The unplanted trail kept going. I am still deciding whether that is comforting or insulting.";
            }
            return VesperDialogue.sharedTableStayLine(relationship, context);
        }

        private String femaleEpilogueLine(int relationship, DialogueContext context) {
            if (this == SERAPHINE && context.epilogueReady()) {
                return !context.questOutcome("seraphine_oathstead_promise").isBlank()
                        ? "After came, and no one owned it. I keep checking the fine print. There is none. Horrifyingly intimate."
                        : "After came with fewer contracts than I expected. I am trying to take that personally in a healthy way.";
            }
            if (this == MAERA && context.epilogueReady()) {
                return !context.questOutcome("maera_oathstead_archive").isBlank()
                        ? "After came, and the first honest archive is still unfinished. Good. Finished records become smug too quickly."
                        : "After came with loose pages everywhere. History remains inconsiderate.";
            }
            if (this == CASSIA && context.epilogueReady()) {
                return !context.questOutcome("cassia_oathstead_duty").isBlank()
                        ? "After came, and duty did not vanish. It became smaller, named, possible to carry without becoming it."
                        : "After came. I am still standing, but no longer only because someone ordered me to.";
            }
            if (this == LYRA && context.epilogueReady()) {
                return !context.questOutcome("lyra_oathstead_clinic").isBlank()
                        ? "After came, and the clinic opened before the next disaster. That is not a miracle. That is better: people chose it."
                        : "After came with wounds that still need changing. Endings rarely wash their own bandages.";
            }
            if (this == ARIA && context.epilogueReady()) {
                return !context.questOutcome("aria_oathstead_future").isBlank()
                        ? "After came, and the road with my name on it still has exits. That may be why I can call it mine."
                        : "After came. I still know the exits. I also know which road leads back.";
            }
            return VesperDialogue.epilogueLine(relationship, context);
        }

        String memoryOpening() {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.memoryOpening();
                case MAERA -> MaeraDialogue.memoryOpening();
                case CASSIA -> CassiaDialogue.memoryOpening();
                case LYRA -> LyraDialogue.memoryOpening();
                case SAMIR -> SamirDialogue.memoryOpening();
                case ARIA -> AriaDialogue.memoryOpening();
                case VESPER -> VesperDialogue.memoryOpening();
                case RAFIQ -> RafiqDialogue.memoryOpening();
                case CALDER -> CalderDialogue.memoryOpening();
            };
        }

        String memoryReflection(String memory) {
            String remembered = memory == null || memory.isBlank() ? "that moment" : memory.strip();
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.memoryReflection(remembered);
                case MAERA -> MaeraDialogue.memoryReflection(remembered);
                case CASSIA -> CassiaDialogue.memoryReflection(remembered);
                case LYRA -> LyraDialogue.memoryReflection(remembered);
                case SAMIR -> SamirDialogue.memoryReflection(remembered);
                case ARIA -> AriaDialogue.memoryReflection(remembered);
                case VESPER -> VesperDialogue.memoryReflection(remembered);
                case RAFIQ -> RafiqDialogue.memoryReflection(remembered);
                case CALDER -> CalderDialogue.memoryReflection(remembered);
            };
        }

        String memorySharedResponse() {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.memorySharedResponse();
                case MAERA -> MaeraDialogue.memorySharedResponse();
                case CASSIA -> CassiaDialogue.memorySharedResponse();
                case LYRA -> LyraDialogue.memorySharedResponse();
                case SAMIR -> SamirDialogue.memorySharedResponse();
                case ARIA -> AriaDialogue.memorySharedResponse();
                case VESPER -> VesperDialogue.memorySharedResponse();
                case RAFIQ -> RafiqDialogue.memorySharedResponse();
                case CALDER -> CalderDialogue.memorySharedResponse();
            };
        }

        String memoryForwardResponse() {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.memoryForwardResponse();
                case MAERA -> MaeraDialogue.memoryForwardResponse();
                case CASSIA -> CassiaDialogue.memoryForwardResponse();
                case LYRA -> LyraDialogue.memoryForwardResponse();
                case SAMIR -> SamirDialogue.memoryForwardResponse();
                case ARIA -> AriaDialogue.memoryForwardResponse();
                case VESPER -> VesperDialogue.memoryForwardResponse();
                case RAFIQ -> RafiqDialogue.memoryForwardResponse();
                case CALDER -> CalderDialogue.memoryForwardResponse();
            };
        }

        String outcomeMemoryRootLabel() {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.outcomeMemoryRootLabel();
                case MAERA -> MaeraDialogue.outcomeMemoryRootLabel();
                case CASSIA -> CassiaDialogue.outcomeMemoryRootLabel();
                case LYRA -> LyraDialogue.outcomeMemoryRootLabel();
                case ARIA -> AriaDialogue.outcomeMemoryRootLabel();
                case VESPER -> VesperDialogue.outcomeMemoryRootLabel();
                default -> "Can we talk about our choices?";
            };
        }

        String outcomeMemoryOpening(DialogueContext context) {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.outcomeMemoryOpening();
                case MAERA -> MaeraDialogue.outcomeMemoryOpening();
                case CASSIA -> CassiaDialogue.outcomeMemoryOpening();
                case LYRA -> LyraDialogue.outcomeMemoryOpening();
                case ARIA -> AriaDialogue.outcomeMemoryOpening();
                case VESPER -> VesperDialogue.outcomeMemoryOpening();
                default -> "Some choices keep walking beside us after the quest calls them finished.";
            };
        }

        String outcomeMemoryLine(String outcomeKey, DialogueContext context) {
            if (hasFemaleOutcomeVoice()) {
                return femaleOutcomeMemoryLine(outcomeKey, context);
            }
            return "We chose " + context.questOutcomeLabel(outcomeKey).toLowerCase(Locale.ROOT)
                    + " there. The choice still deserves to be spoken plainly.";
        }

        String outcomeMemorySharedLine(String outcomeKey, DialogueContext context) {
            if (hasFemaleOutcomeVoice()) {
                return femaleOutcomeSharedLine(outcomeKey, context);
            }
            return "Then we remember it honestly, without sanding off the parts that made the choice matter.";
        }

        String outcomeMemoryQuestionLine(String outcomeKey, DialogueContext context) {
            if (hasFemaleOutcomeVoice()) {
                return femaleOutcomeQuestionLine(outcomeKey, context);
            }
            return "Different, perhaps. Easier, no. Easy answers rarely survive the road.";
        }

        private boolean hasFemaleOutcomeVoice() {
            return switch (this) {
                case SERAPHINE, MAERA, CASSIA, LYRA, ARIA, VESPER -> true;
                default -> false;
            };
        }

        private String femaleOutcomeMemoryLine(String outcomeKey, DialogueContext context) {
            if (this == VESPER) {
                return VesperDialogue.outcomeMemoryLine(outcomeKey, context);
            }
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.outcomeMemoryLine(outcomeKey, context);
                case MAERA -> MaeraDialogue.outcomeMemoryLine(outcomeKey, context);
                case CASSIA -> CassiaDialogue.outcomeMemoryLine(outcomeKey, context);
                case LYRA -> LyraDialogue.outcomeMemoryLine(outcomeKey, context);
                case ARIA -> AriaDialogue.outcomeMemoryLine(outcomeKey, context);
                default -> "I remember that choice. It kept speaking after the road moved on.";
            };
        }

        private String femaleOutcomeSharedLine(String outcomeKey, DialogueContext context) {
            if (this == VESPER) {
                return VesperDialogue.outcomeMemorySharedLine(outcomeKey);
            }
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.outcomeMemorySharedLine(outcomeKey);
                case MAERA -> MaeraDialogue.outcomeMemorySharedLine(outcomeKey);
                case CASSIA -> CassiaDialogue.outcomeMemorySharedLine(outcomeKey);
                case LYRA -> LyraDialogue.outcomeMemorySharedLine(outcomeKey);
                case ARIA -> AriaDialogue.outcomeMemorySharedLine(outcomeKey);
                default -> "Then we remember it honestly.";
            };
        }

        private String femaleOutcomeQuestionLine(String outcomeKey, DialogueContext context) {
            if (this == VESPER) {
                return VesperDialogue.outcomeMemoryQuestionLine(context.questOutcome(outcomeKey));
            }
            String outcome = context.questOutcome(outcomeKey);
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.outcomeMemoryQuestionLine(outcome);
                case MAERA -> MaeraDialogue.outcomeMemoryQuestionLine(outcome);
                case CASSIA -> CassiaDialogue.outcomeMemoryQuestionLine(outcome);
                case LYRA -> LyraDialogue.outcomeMemoryQuestionLine(outcome);
                case ARIA -> AriaDialogue.outcomeMemoryQuestionLine(outcome);
                default -> "Different, perhaps. Easier, no. Easy answers rarely survive the road.";
            };
        }

        String opinionAboutPlayer(DialogueContext context) {
            if (this == VESPER) {
                return VesperDialogue.opinionAboutPlayer(context);
            }
            if (context.married()) {
                return "You are home and road at once. Inconvenient. Necessary. Mine by choice, never by claim.";
            }
            if (context.romanced()) {
                return "You keep making danger feel less lonely. I am still deciding whether to forgive you for that.";
            }
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.opinionAboutPlayer(context);
                case MAERA -> MaeraDialogue.opinionAboutPlayer(context);
                case CASSIA -> CassiaDialogue.opinionAboutPlayer(context);
                case LYRA -> LyraDialogue.opinionAboutPlayer(context);
                case SAMIR -> SamirDialogue.opinionAboutPlayer(context);
                case ARIA -> AriaDialogue.opinionAboutPlayer(context);
                case VESPER -> VesperDialogue.opinionAboutPlayer(context);
                case RAFIQ -> RafiqDialogue.opinionAboutPlayer(context);
                case CALDER -> CalderDialogue.opinionAboutPlayer(context);
            };
        }

        String opinionAbout(CompanionVoice subject) {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.opinionAbout(subject.name());
                case MAERA -> MaeraDialogue.opinionAbout(subject.name());
                case CASSIA -> CassiaDialogue.opinionAbout(subject.name());
                case LYRA -> LyraDialogue.opinionAbout(subject.name());
                case SAMIR -> SamirDialogue.opinionAbout(subject.name());
                case ARIA -> AriaDialogue.opinionAbout(subject.name());
                case VESPER -> VesperDialogue.opinionAbout(subject.name());
                case RAFIQ -> RafiqDialogue.opinionAbout(subject.name());
                case CALDER -> CalderDialogue.opinionAbout(subject.name());
            };
        }

        String impatientLine() {
            return "Can we skip the story and do the work?";
        }

        String impatientResponse() {
            return "You can. But then you will only know where to swing, not why the wound matters.";
        }

        String bossQuestOpening() {
            return "This is the part of the road that stops pretending it is only personal. Whatever waits ahead is tied to " + concern + ".";
        }

        String bossMeaning() {
            return "It means the thing I tried to outrun has learned your name too. I hate that. I also trust you with it.";
        }

        String bossTactical() {
            return "Do not fight for my pride. Fight for the opening. If I freeze, call my name. If I rush, stop me.";
        }

        String bossClean() {
            return "Cleanly would be merciful. I will settle for finished.";
        }

        String bossDismiss() {
            return "If it were just another fight, I would not have asked you to stand beside me.";
        }

        String finalQuestOpening() {
            return "The old road ends where I thought my past had the last word. I am no longer sure it gets that privilege.";
        }

        String finalChoice() {
            return "Then I choose this. Not because it is easy, and not because you asked. Because I am done letting old fear vote for me.";
        }

        String finalHome() {
            return "A home that can hold the truth is rarer than a fortress. I did not expect to find one muddy and half-built.";
        }

        String finalNeed() {
            return "Stay honest with me. If I start speaking like the old wound, remind me I am not required to obey it.";
        }

        String finalDismiss() {
            return "Over is a dangerous word. Say survived. Say changed. Those are harder to misuse.";
        }

        String middleQuestOpening(Quest quest) {
            return "This thread keeps pulling. " + (quest == null ? "We follow it carefully." : quest.title + " is not separate from " + concern + ".");
        }

        String marriageOpening() {
            return marriageOpening;
        }

        String marriageAccept() {
            return marriageAccept;
        }

        String marriageAsk() {
            return "I want it. I want to be brave enough to say that without making a joke or a doctrine out of it.";
        }

        String marriageLater() {
            return "Later, then. But not never. Do not offer me a future and vanish into grammar.";
        }

        String marriageNo() {
            return "Then do not dress uncertainty as kindness. I would rather have the truth than a beautiful trap.";
        }

        String lovingOpening() {
            return lovingOpening;
        }

        String romanceAccept() {
            return "Then say it plainly when the world is not ending too. I want the quiet version as much as the brave one.";
        }

        String romanceStay() {
            return "A while is how honest things begin when forever is too large to hold.";
        }

        String romanceWarm() {
            return "It matters to me too. More than I planned, which is rude of it.";
        }

        String romanceNo() {
            return "Professional. Of course. I can do professional. I may even forgive the word eventually.";
        }

        String flirtChoice() {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.flirtChoice();
                case MAERA -> MaeraDialogue.flirtChoice();
                case CASSIA -> CassiaDialogue.flirtChoice();
                case LYRA -> LyraDialogue.flirtChoice();
                case SAMIR -> SamirDialogue.flirtChoice();
                case ARIA -> AriaDialogue.flirtChoice();
                case VESPER -> VesperDialogue.flirtChoice();
                case RAFIQ -> RafiqDialogue.flirtChoice();
                case CALDER -> CalderDialogue.flirtChoice();
            };
        }

        String flirtRootLabel(DialogueContext context) {
            if (context.romanced() || context.married()) {
                return "Come here a moment.";
            }
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.flirtRootLabel();
                case MAERA -> MaeraDialogue.flirtRootLabel();
                case CASSIA -> CassiaDialogue.flirtRootLabel();
                case LYRA -> LyraDialogue.flirtRootLabel();
                case SAMIR -> SamirDialogue.flirtRootLabel();
                case ARIA -> AriaDialogue.flirtRootLabel();
                case VESPER -> VesperDialogue.flirtRootLabel();
                case RAFIQ -> RafiqDialogue.flirtRootLabel();
                case CALDER -> CalderDialogue.flirtRootLabel();
            };
        }

        String flirtOpening(int relationship, DialogueContext context) {
            if (context.married()) {
                return switch (this) {
                    case SERAPHINE -> SeraphineDialogue.marriedFlirtOpening();
                    case MAERA -> MaeraDialogue.marriedFlirtOpening();
                    case CASSIA -> CassiaDialogue.marriedFlirtOpening();
                    case LYRA -> LyraDialogue.marriedFlirtOpening();
                    case SAMIR -> SamirDialogue.marriedFlirtOpening();
                    case ARIA -> AriaDialogue.marriedFlirtOpening();
                    case VESPER -> VesperDialogue.marriedFlirtOpening();
                    case RAFIQ -> RafiqDialogue.marriedFlirtOpening();
                    case CALDER -> CalderDialogue.marriedFlirtOpening();
                };
            }
            if (context.romanced()) {
                return romanceDateFlirtLine(context);
            }
            if (relationship >= 180) {
                return "Careful. If you say it softly enough, I might believe you are not only teasing.";
            }
            return "That is a warmer road than we usually walk. I am listening, but do not make it careless.";
        }

        String flirtSoftChoice(DialogueContext context) {
            return context.romanced() || context.married()
                    ? "I missed being close to you."
                    : "I like being near you.";
        }

        String flirtBoldChoice(DialogueContext context) {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.flirtBoldChoice();
                case MAERA -> MaeraDialogue.flirtBoldChoice();
                case CASSIA -> CassiaDialogue.flirtBoldChoice();
                case LYRA -> LyraDialogue.flirtBoldChoice();
                case SAMIR -> SamirDialogue.flirtBoldChoice();
                case ARIA -> AriaDialogue.flirtBoldChoice();
                case VESPER -> VesperDialogue.flirtBoldChoice();
                case RAFIQ -> RafiqDialogue.flirtBoldChoice();
                case CALDER -> CalderDialogue.flirtBoldChoice();
            };
        }

        String flirtSoftLine(DialogueContext context) {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.flirtSoftLine();
                case MAERA -> MaeraDialogue.flirtSoftLine();
                case CASSIA -> CassiaDialogue.flirtSoftLine();
                case LYRA -> LyraDialogue.flirtSoftLine();
                case SAMIR -> SamirDialogue.flirtSoftLine();
                case ARIA -> AriaDialogue.flirtSoftLine();
                case VESPER -> VesperDialogue.flirtSoftLine();
                case RAFIQ -> RafiqDialogue.flirtSoftLine();
                case CALDER -> CalderDialogue.flirtSoftLine();
            };
        }

        String flirtBoldLine(DialogueContext context) {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.flirtBoldLine();
                case MAERA -> MaeraDialogue.flirtBoldLine();
                case CASSIA -> CassiaDialogue.flirtBoldLine();
                case LYRA -> LyraDialogue.flirtBoldLine();
                case SAMIR -> SamirDialogue.flirtBoldLine();
                case ARIA -> AriaDialogue.flirtBoldLine();
                case VESPER -> VesperDialogue.flirtBoldLine();
                case RAFIQ -> RafiqDialogue.flirtBoldLine();
                case CALDER -> CalderDialogue.flirtBoldLine();
            };
        }

        String flirtBoundaryLine(DialogueContext context) {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.flirtBoundaryLine();
                case MAERA -> MaeraDialogue.flirtBoundaryLine();
                case CASSIA -> CassiaDialogue.flirtBoundaryLine();
                case LYRA -> LyraDialogue.flirtBoundaryLine();
                case SAMIR -> SamirDialogue.flirtBoundaryLine();
                case ARIA -> AriaDialogue.flirtBoundaryLine();
                case VESPER -> VesperDialogue.flirtBoundaryLine();
                case RAFIQ -> RafiqDialogue.flirtBoundaryLine();
                case CALDER -> CalderDialogue.flirtBoundaryLine();
            };
        }

        String flirtAfterLine(DialogueContext context) {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.flirtAfterLine();
                case MAERA -> MaeraDialogue.flirtAfterLine();
                case CASSIA -> CassiaDialogue.flirtAfterLine();
                case LYRA -> LyraDialogue.flirtAfterLine();
                case SAMIR -> SamirDialogue.flirtAfterLine();
                case ARIA -> AriaDialogue.flirtAfterLine();
                case VESPER -> VesperDialogue.flirtAfterLine();
                case RAFIQ -> RafiqDialogue.flirtAfterLine();
                case CALDER -> CalderDialogue.flirtAfterLine();
            };
        }

        String romanceDateOpening(DialogueContext context, String place) {
            if (context.sharedTableConversation()) {
                return switch (this) {
                    case SERAPHINE -> SeraphineDialogue.romanceDateOpeningAtPlace(place);
                    case MAERA -> MaeraDialogue.romanceDateOpeningAtPlace(place);
                    case CASSIA -> CassiaDialogue.romanceDateOpeningAtPlace(place);
                    case LYRA -> LyraDialogue.romanceDateOpeningAtPlace(place);
                    case SAMIR -> SamirDialogue.romanceDateOpeningAtPlace(place);
                    case ARIA -> AriaDialogue.romanceDateOpeningAtPlace(place);
                    case VESPER -> VesperDialogue.romanceDateOpeningAtPlace(place);
                    case RAFIQ -> RafiqDialogue.romanceDateOpeningAtPlace(place);
                    case CALDER -> CalderDialogue.romanceDateOpeningAtPlace(place);
                };
            }
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.romanceDateOpeningInvite();
                case MAERA -> MaeraDialogue.romanceDateOpeningInvite();
                case CASSIA -> CassiaDialogue.romanceDateOpeningInvite();
                case LYRA -> LyraDialogue.romanceDateOpeningInvite();
                case SAMIR -> SamirDialogue.romanceDateOpeningInvite();
                case ARIA -> AriaDialogue.romanceDateOpeningInvite();
                case VESPER -> VesperDialogue.romanceDateOpeningInvite();
                case RAFIQ -> RafiqDialogue.romanceDateOpeningInvite();
                case CALDER -> CalderDialogue.romanceDateOpeningInvite();
            };
        }

        String romanceDateFlirtLine(DialogueContext context) {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.romanceDateFlirtLine();
                case MAERA -> MaeraDialogue.romanceDateFlirtLine();
                case CASSIA -> CassiaDialogue.romanceDateFlirtLine();
                case LYRA -> LyraDialogue.romanceDateFlirtLine();
                case SAMIR -> SamirDialogue.romanceDateFlirtLine();
                case ARIA -> AriaDialogue.romanceDateFlirtLine();
                case VESPER -> VesperDialogue.romanceDateFlirtLine();
                case RAFIQ -> RafiqDialogue.romanceDateFlirtLine();
                case CALDER -> CalderDialogue.romanceDateFlirtLine();
            };
        }

        String romanceDateSincereLine(DialogueContext context) {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.romanceDateSincereLine();
                case MAERA -> MaeraDialogue.romanceDateSincereLine();
                case CASSIA -> CassiaDialogue.romanceDateSincereLine();
                case LYRA -> LyraDialogue.romanceDateSincereLine();
                case SAMIR -> SamirDialogue.romanceDateSincereLine();
                case ARIA -> AriaDialogue.romanceDateSincereLine();
                case VESPER -> VesperDialogue.romanceDateSincereLine();
                case RAFIQ -> RafiqDialogue.romanceDateSincereLine();
                case CALDER -> CalderDialogue.romanceDateSincereLine();
            };
        }

        String romanceDatePlaceLine(DialogueContext context, String place) {
            if (context.sharedTableConversation()) {
                return switch (this) {
                    case SERAPHINE -> SeraphineDialogue.romanceDatePlaceLine(place);
                    case MAERA -> MaeraDialogue.romanceDatePlaceLine(place);
                    case CASSIA -> CassiaDialogue.romanceDatePlaceLine(place);
                    case LYRA -> LyraDialogue.romanceDatePlaceLine(place);
                    case SAMIR -> SamirDialogue.romanceDatePlaceLine(place);
                    case ARIA -> AriaDialogue.romanceDatePlaceLine(place);
                    case VESPER -> VesperDialogue.romanceDatePlaceLine(place);
                    case RAFIQ -> RafiqDialogue.romanceDatePlaceLine(place);
                    case CALDER -> CalderDialogue.romanceDatePlaceLine(place);
                };
            }
            return "Then we will find the right place. Not a grand gesture. A quiet hour chosen on purpose.";
        }

        String romanceDateBoundaryLine(DialogueContext context) {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.romanceDateBoundaryLine();
                case MAERA -> MaeraDialogue.romanceDateBoundaryLine();
                case CASSIA -> CassiaDialogue.romanceDateBoundaryLine();
                case LYRA -> LyraDialogue.romanceDateBoundaryLine();
                case SAMIR -> SamirDialogue.romanceDateBoundaryLine();
                case ARIA -> AriaDialogue.romanceDateBoundaryLine();
                case VESPER -> VesperDialogue.romanceDateBoundaryLine();
                case RAFIQ -> RafiqDialogue.romanceDateBoundaryLine();
                case CALDER -> CalderDialogue.romanceDateBoundaryLine();
            };
        }

        String loyalOpening() {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.loyalOpening();
                case MAERA -> MaeraDialogue.loyalOpening();
                case CASSIA -> CassiaDialogue.loyalOpening();
                case LYRA -> LyraDialogue.loyalOpening();
                case SAMIR -> SamirDialogue.loyalOpening();
                case ARIA -> AriaDialogue.loyalOpening();
                case VESPER -> VesperDialogue.loyalOpening();
                case RAFIQ -> RafiqDialogue.loyalOpening();
                case CALDER -> CalderDialogue.loyalOpening();
            };
        }

        String loyalStand() {
            return "I already am. I just wanted to know whether you noticed the cost.";
        }

        String loyalChoose() {
            return "Choice is the part that makes loyalty worth keeping. Thank you for remembering.";
        }

        String loyalWarm() {
            return "Good. I was beginning to suspect you kept me around for my charming ability to create problems.";
        }

        String loyalCold() {
            return "Skills I can offer. Warmth is not a tool you get to inventory.";
        }

        String trustedOpening() {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.trustedOpening();
                case MAERA -> MaeraDialogue.trustedOpening();
                case CASSIA -> CassiaDialogue.trustedOpening();
                case LYRA -> LyraDialogue.trustedOpening();
                case SAMIR -> SamirDialogue.trustedOpening();
                case ARIA -> AriaDialogue.trustedOpening();
                case VESPER -> VesperDialogue.trustedOpening();
                case RAFIQ -> RafiqDialogue.trustedOpening();
                case CALDER -> CalderDialogue.trustedOpening();
            };
        }

        String trustedTruth() {
            return personalResponse;
        }

        String trustedCare() {
            return "Carefully is all I ask. Some truths break if carried like trophies.";
        }

        String trustedAsk() {
            return "Apparently. Try not to look too pleased. It will make me revise the decision.";
        }

        String trustedDoubt() {
            return "Safer, perhaps. Truer, no.";
        }

        String friendOpening() {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.friendOpening();
                case MAERA -> MaeraDialogue.friendOpening();
                case CASSIA -> CassiaDialogue.friendOpening();
                case LYRA -> LyraDialogue.friendOpening();
                case SAMIR -> SamirDialogue.friendOpening();
                case ARIA -> AriaDialogue.friendOpening();
                case VESPER -> VesperDialogue.friendOpening();
                case RAFIQ -> RafiqDialogue.friendOpening();
                case CALDER -> CalderDialogue.friendOpening();
            };
        }

        String friendListen() {
            return "Then listen without sharpening an answer. That is rarer than advice.";
        }

        String friendPatient() {
            return "Time offered without a leash is a gift. I know what that costs on this road.";
        }

        String friendMission() {
            return "Everything can help the mission if you grind it small enough. I am trying not to live that way.";
        }

        String friendRush() {
            return "Quick stories are for taverns. Mine has teeth.";
        }

        String friendlyOpening() {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.friendlyOpening();
                case MAERA -> MaeraDialogue.friendlyOpening();
                case CASSIA -> CassiaDialogue.friendlyOpening();
                case LYRA -> LyraDialogue.friendlyOpening();
                case SAMIR -> SamirDialogue.friendlyOpening();
                case ARIA -> AriaDialogue.friendlyOpening();
                case VESPER -> VesperDialogue.friendlyOpening();
                case RAFIQ -> RafiqDialogue.friendlyOpening();
                case CALDER -> CalderDialogue.friendlyOpening();
            };
        }

        String friendlyWarm() {
            return "Careful. I may grow accustomed to being greeted like a person.";
        }

        String friendlyPlay() {
            return "Almost. Do not startle it. Comfort is a skittish thing.";
        }

        String friendlyAsk() {
            return "You kept showing up when showing up was inconvenient. That tends to alter the ledger.";
        }

        String friendlyCold() {
            return "Always. But alert and alone are not the same thing.";
        }

        String acquaintanceOpening() {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.acquaintanceOpening();
                case MAERA -> MaeraDialogue.acquaintanceOpening();
                case CASSIA -> CassiaDialogue.acquaintanceOpening();
                case LYRA -> LyraDialogue.acquaintanceOpening();
                case SAMIR -> SamirDialogue.acquaintanceOpening();
                case ARIA -> AriaDialogue.acquaintanceOpening();
                case VESPER -> VesperDialogue.acquaintanceOpening();
                case RAFIQ -> RafiqDialogue.acquaintanceOpening();
                case CALDER -> CalderDialogue.acquaintanceOpening();
            };
        }

        String acquaintUseful() {
            return "Useful is a respectable beginning. It has fed better people than affection has.";
        }

        String acquaintTrust() {
            return "Earned trust lasts longer than demanded trust. Sensible. Annoyingly attractive, in a civic way.";
        }

        String acquaintDistance() {
            return "Distance lets people decide whether they are approaching honestly.";
        }

        String acquaintCold() {
            return "Then you will get my work, not my trust. There is a difference.";
        }

        String guardedOpening() {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.guardedOpening();
                case MAERA -> MaeraDialogue.guardedOpening();
                case CASSIA -> CassiaDialogue.guardedOpening();
                case LYRA -> LyraDialogue.guardedOpening();
                case SAMIR -> SamirDialogue.guardedOpening();
                case ARIA -> AriaDialogue.guardedOpening();
                case VESPER -> VesperDialogue.guardedOpening();
                case RAFIQ -> RafiqDialogue.guardedOpening();
                case CALDER -> CalderDialogue.guardedOpening();
            };
        }

        String guardedTeach() {
            return "Good. Learning before swinging prevents expensive apologies.";
        }

        String guardedListen() {
            return "Listening is a start. Continuing to listen after the answer becomes inconvenient is the test.";
        }

        String guardedNeutral() {
            return "Survival is not the same as judgment. It is only proof you got another chance.";
        }

        String guardedCold() {
            return "Then spend your haste elsewhere. I have buried enough people who confused urgency with wisdom.";
        }

        String questTopic(Quest quest) {
            int stage = companionQuestStage(quest);
            if (stage == 7) {
                return "Why does this fight matter?";
            }
            if (stage == 8) {
                return "What will you choose now?";
            }
            return questTopic;
        }

        String recruitId() {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.recruitId();
                case MAERA -> MaeraDialogue.recruitId();
                case CASSIA -> CassiaDialogue.recruitId();
                case LYRA -> LyraDialogue.recruitId();
                case SAMIR -> SamirDialogue.recruitId();
                case ARIA -> AriaDialogue.recruitId();
                case VESPER -> VesperDialogue.recruitId();
                case RAFIQ -> RafiqDialogue.recruitId();
                case CALDER -> CalderDialogue.recruitId();
            };
        }

        String recruitmentRootLabel() {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.recruitmentRootLabel();
                case MAERA -> MaeraDialogue.recruitmentRootLabel();
                case CASSIA -> CassiaDialogue.recruitmentRootLabel();
                case LYRA -> LyraDialogue.recruitmentRootLabel();
                case SAMIR -> SamirDialogue.recruitmentRootLabel();
                case ARIA -> AriaDialogue.recruitmentRootLabel();
                case VESPER -> VesperDialogue.recruitmentRootLabel();
                case RAFIQ -> RafiqDialogue.recruitmentRootLabel();
                case CALDER -> CalderDialogue.recruitmentRootLabel();
            };
        }

        String recruitmentOpening() {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.recruitmentOpening();
                case MAERA -> MaeraDialogue.recruitmentOpening();
                case CASSIA -> CassiaDialogue.recruitmentOpening();
                case LYRA -> LyraDialogue.recruitmentOpening();
                case SAMIR -> SamirDialogue.recruitmentOpening();
                case ARIA -> AriaDialogue.recruitmentOpening();
                case VESPER -> VesperDialogue.recruitmentOpening();
                case RAFIQ -> RafiqDialogue.recruitmentOpening();
                case CALDER -> CalderDialogue.recruitmentOpening();
            };
        }

        String recruitmentRoadChoice() {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.recruitmentRoadChoice();
                case MAERA -> MaeraDialogue.recruitmentRoadChoice();
                case CASSIA -> CassiaDialogue.recruitmentRoadChoice();
                case LYRA -> LyraDialogue.recruitmentRoadChoice();
                case SAMIR -> SamirDialogue.recruitmentRoadChoice();
                case ARIA -> AriaDialogue.recruitmentRoadChoice();
                case VESPER -> VesperDialogue.recruitmentRoadChoice();
                case RAFIQ -> RafiqDialogue.recruitmentRoadChoice();
                case CALDER -> CalderDialogue.recruitmentRoadChoice();
            };
        }

        String recruitmentOathsteadChoice() {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.recruitmentOathsteadChoice();
                case MAERA -> MaeraDialogue.recruitmentOathsteadChoice();
                case CASSIA -> CassiaDialogue.recruitmentOathsteadChoice();
                case LYRA -> LyraDialogue.recruitmentOathsteadChoice();
                case SAMIR -> SamirDialogue.recruitmentOathsteadChoice();
                case ARIA -> AriaDialogue.recruitmentOathsteadChoice();
                case VESPER -> VesperDialogue.recruitmentOathsteadChoice();
                case RAFIQ -> RafiqDialogue.recruitmentOathsteadChoice();
                case CALDER -> CalderDialogue.recruitmentOathsteadChoice();
            };
        }

        String recruitmentRoadAcceptLine() {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.recruitmentRoadAcceptLine();
                case MAERA -> MaeraDialogue.recruitmentRoadAcceptLine();
                case CASSIA -> CassiaDialogue.recruitmentRoadAcceptLine();
                case LYRA -> LyraDialogue.recruitmentRoadAcceptLine();
                case SAMIR -> SamirDialogue.recruitmentRoadAcceptLine();
                case ARIA -> AriaDialogue.recruitmentRoadAcceptLine();
                case VESPER -> VesperDialogue.recruitmentRoadAcceptLine();
                case RAFIQ -> RafiqDialogue.recruitmentRoadAcceptLine();
                case CALDER -> CalderDialogue.recruitmentRoadAcceptLine();
            };
        }

        String recruitmentOathsteadAcceptLine() {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.recruitmentOathsteadAcceptLine();
                case MAERA -> MaeraDialogue.recruitmentOathsteadAcceptLine();
                case CASSIA -> CassiaDialogue.recruitmentOathsteadAcceptLine();
                case LYRA -> LyraDialogue.recruitmentOathsteadAcceptLine();
                case SAMIR -> SamirDialogue.recruitmentOathsteadAcceptLine();
                case ARIA -> AriaDialogue.recruitmentOathsteadAcceptLine();
                case VESPER -> VesperDialogue.recruitmentOathsteadAcceptLine();
                case RAFIQ -> RafiqDialogue.recruitmentOathsteadAcceptLine();
                case CALDER -> CalderDialogue.recruitmentOathsteadAcceptLine();
            };
        }

        String recruitmentWaitLine() {
            return switch (this) {
                case SERAPHINE -> SeraphineDialogue.recruitmentWaitLine();
                case MAERA -> MaeraDialogue.recruitmentWaitLine();
                case CASSIA -> CassiaDialogue.recruitmentWaitLine();
                case LYRA -> LyraDialogue.recruitmentWaitLine();
                case SAMIR -> SamirDialogue.recruitmentWaitLine();
                case ARIA -> AriaDialogue.recruitmentWaitLine();
                case VESPER -> VesperDialogue.recruitmentWaitLine();
                case RAFIQ -> RafiqDialogue.recruitmentWaitLine();
                case CALDER -> CalderDialogue.recruitmentWaitLine();
            };
        }
    }

    private static String greetingQuote(Npc npc, Random random) {
        List<String> candidates = new ArrayList<>();
        for (String line : npc.dialog()) {
            String cleaned = cleanGreetingLine(line);
            if (!cleaned.isBlank()) {
                candidates.add(cleaned);
            }
        }
        if (!candidates.isEmpty()) {
            return pick(candidates, random);
        }
        String work = professionDialog(npc, random);
        int colon = work.indexOf(':');
        if (colon > 0 && colon + 1 < work.length()) {
            work = work.substring(colon + 1).strip();
        }
        if (!work.isBlank()) {
            return work;
        }
        return npc.name() + " nods, measuring the road dust on your boots.";
    }

    private static String cleanGreetingLine(String line) {
        if (line == null) {
            return "";
        }
        String cleaned = line.replaceAll("\\s+", " ").strip();
        if (cleaned.isBlank()) {
            return "";
        }
        String lower = cleaned.toLowerCase();
        if (lower.startsWith("skills:")
                || lower.startsWith("abilities:")
                || lower.startsWith("pack:")
                || lower.startsWith("work:")) {
            return "";
        }
        int colon = cleaned.indexOf(':');
        if (colon > 0 && colon <= 24) {
            cleaned = cleaned.substring(colon + 1).strip();
        }
        return cleaned;
    }

    public static Personality personalityFor(Npc npc) {
        String key = npcDialogKey(npc);
        return switch (key) {
            case "seraphine vale" -> new Personality("Velvet-Guarded", "She responds to discretion, elegant leverage, and promises that do not sound like ownership.");
            case "maera quill" -> new Personality("Star-Curious", "She warms to evidence, forbidden routes, and people who let wonder remain sharp.");
            case "cassia flint" -> new Personality("Iron-Contrite", "She values direct speech, battlefield accountability, and help that respects old losses without pity.");
            case "lyra bell" -> new Personality("Gentle-Fierce", "She trusts practical mercy, careful listening, and anyone who treats harm as something to interrupt.");
            case "samir dawn" -> new Personality("Radiant-Questioning", "He respects faith that can survive doubt, witness, and choices made in open light.");
            case "aria foxglove" -> new Personality("Fox-Quick", "She enjoys dry wit, quiet competence, and people who notice traps before stepping into them.");
            case "vesper snowroot" -> new Personality("Winter-Rooted", "She opens to patience, old growth, and respect for living things hidden under hard weather.");
            case "rafiq glass" -> new Personality("Mirage-Charming", "He responds to courage with humor, graceful honesty, and second chances earned without applause.");
            case "calder reed" -> new Personality("Mud-Steady", "He trusts repairs, plain truth, and people who know when a bridge matters more than pride.");
            case "marla", "liora", "elowen" -> new Personality("Compassionate", "They respond well to patience, honest concern, and choices that protect vulnerable people.");
            case "captain torin", "bran", "garruk ironwall" -> new Personality("Disciplined", "They value duty, direct promises, and respect for difficult work.");
            case "archivist ren", "scribe pela", "toma" -> new Personality("Curious", "They warm to careful questions, evidence, and people who treat stories as useful truths.");
            case "sela", "nyx", "sable" -> new Personality("Guarded", "They dislike pressure, but respect discretion, competence, and quiet honesty.");
            case "edda", "farmer joss", "old noll" -> new Personality("Practical", "They trust useful help more than grand speeches and prefer grounded answers.");
            case "hedgewise lin", "rowan wildspeaker", "vexa", "fen" -> new Personality("Wild-Touched", "They listen for respect toward land, weather, roots, and things polite folk ignore.");
            case "rain-seer imani", "bellkeeper ilya", "cairnwatch asta" -> new Personality("Watchful", "They notice tone quickly and reward careful listening over bravado.");
            case "quartermaster vesh" -> new Personality("Exacting", "They appreciate preparation, clear numbers, and people who do not waste supplies.");
            default -> switch (npcProfession(npc)) {
                case "bartender" -> new Personality("Gregarious", "They like curiosity, humor, and travelers who know a rumor is a kind of currency.");
                case "blacksmith" -> new Personality("Proud", "They respect craft, maintenance, and people who understand that tools deserve care.");
                case "merchant" -> new Personality("Transactional", "They respond to fair dealing, sharp observation, and reliable follow-through.");
                case "guard" -> new Personality("Steady", "They value respect, vigilance, and people who do not make danger louder.");
                case "scout" -> new Personality("Observant", "They appreciate restraint, route sense, and questions about what others missed.");
                default -> new Personality("Reserved", "They open up slowly and judge travelers by tone as much as action.");
            };
        };
    }

    private static String relationshipLabel(int relationship) {
        if (relationship >= 250) {
            return "marriage-ready";
        }
        if (relationship >= 180) {
            return "loving";
        }
        if (relationship >= 150) {
            return "loyal";
        }
        if (relationship >= 120) {
            return "trusted";
        }
        if (relationship >= 90) {
            return "friend";
        }
        if (relationship >= 50) {
            return "friendly";
        }
        if (relationship >= 20) {
            return "acquaintance";
        }
        if (relationship <= -60) {
            return "hostile";
        }
        if (relationship <= -25) {
            return "wary";
        }
        return "neutral";
    }

    public static String biomeContext(Npc npc, char npcTile, char fallbackTile) {
        String mapId = npc.mapId();
        if (mapId.startsWith("house_")) {
            mapId = settlementMapFromHouse(mapId);
        }
        if (WorldMap.OVERWORLD_ID.equals(mapId)) {
            return biomeContextForTile(npcTile);
        }
        if (mapId.contains("snowrest") || mapId.contains("pineward") || mapId.contains("highwall") || mapId.contains("ironvale")) {
            return "mountain";
        }
        if (mapId.contains("dunewick") || mapId.contains("sunmere") || mapId.contains("redcairn") || mapId.contains("sanctum") || mapId.contains("embermarket")) {
            return "desert";
        }
        if (mapId.contains("mireford") || mapId.contains("glimmerfen") || mapId.contains("reedwatch") || mapId.contains("belltower") || mapId.contains("riverside")) {
            return "marsh";
        }
        if (mapId.contains("archive") || mapId.contains("moonspire")) {
            return "grave";
        }
        if (mapId.contains("oakhaven") || mapId.contains("foxbarrow") || mapId.contains("elderford") || mapId.contains("briarbridge")) {
            return "forest";
        }
        return biomeContextForTile(fallbackTile);
    }

    private static void addNode(Map<String, DialogueNode> nodes, String id, String line, List<DialogueChoice> choices) {
        nodes.put(id, new DialogueNode(id, line, choices, inferNodeIntent(id), inferRepeatKey(id)));
    }

    private static DialogueIntent inferNodeIntent(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        if ("companion_quest".equals(id)) {
            return DialogueIntent.QUEST_ROOT;
        }
        if ("companion_outcomes".equals(id)) {
            return DialogueIntent.MEMORY;
        }
        if (id.contains("quest_practical") || id.contains("_where")) {
            return DialogueIntent.QUEST_PRACTICAL;
        }
        if (id.contains("quest_warning") || id.contains("_warning") || id.contains("worry")) {
            return DialogueIntent.QUEST_WARNING;
        }
        if (id.contains("quest_personal") || id.contains("_opinion")) {
            return DialogueIntent.QUEST_PERSONAL;
        }
        if (id.contains("quest_clarify") || id.contains("_reason") || id.contains("_meaning") || id.contains("_progress")) {
            return DialogueIntent.QUEST_CLARIFY;
        }
        if (id.contains("quest_challenge") || id.contains("push") || id.contains("doubt")) {
            return DialogueIntent.QUEST_CHALLENGE;
        }
        if (id.contains("quest_support") || id.contains("quest_accept") || id.contains("quest_turnin") || id.contains("_choice_")) {
            return DialogueIntent.QUEST_SUPPORT;
        }
        if (id.contains("checkin")) {
            return DialogueIntent.CONCERN;
        }
        if (id.contains("feeling") || id.contains("milestone")) {
            return DialogueIntent.FEELING;
        }
        if (id.contains("next") || id.contains("need_next") || id.contains("oathstead_need")) {
            return DialogueIntent.NEXT_STEP;
        }
        if (id.contains("need") || id.contains("request")) {
            return DialogueIntent.NEED;
        }
        if (id.contains("values")) {
            return DialogueIntent.VALUES;
        }
        if (id.contains("memory") || id.contains("outcome")) {
            return DialogueIntent.MEMORY;
        }
        if (id.contains("opinion")) {
            return DialogueIntent.OPINION;
        }
        if (id.contains("oathstead") || id.contains("table") || id.contains("home")) {
            return DialogueIntent.HOME;
        }
        if (id.contains("romance")) {
            return DialogueIntent.ROMANCE;
        }
        if (id.contains("trust") || id.contains("recruit")) {
            return DialogueIntent.TRUST;
        }
        return null;
    }

    private static String inferRepeatKey(String id) {
        DialogueIntent intent = inferNodeIntent(id);
        if (intent == null || id == null || id.equals("root") || id.startsWith("story_branch")) {
            return "";
        }
        if (!id.startsWith("companion_")) {
            return "";
        }
        if (id.startsWith("companion_memory_") && !id.equals("companion_memory")) {
            return "memory.detail";
        }
        if (id.startsWith("companion_outcome_") && !id.equals("companion_outcomes")) {
            return "outcome.detail";
        }
        if (id.startsWith("companion_opinion_known_")) {
            return "opinion.companion";
        }
        return switch (intent) {
            case QUEST_ROOT -> "quest";
            case QUEST_CLARIFY -> "quest.clarify";
            case QUEST_PERSONAL -> "quest.personal";
            case QUEST_PRACTICAL -> "quest.practical";
            case QUEST_WARNING -> "quest.warning";
            case QUEST_SUPPORT -> "quest.support";
            case QUEST_CHALLENGE -> "quest.challenge";
            case CONCERN -> "checkin";
            case FEELING -> "feeling";
            case NEXT_STEP -> "next";
            case NEED -> "need";
            case VALUES -> "values";
            case MEMORY -> "memory";
            case OPINION -> "opinion";
            case HOME -> "home";
            case ROMANCE -> "romance";
            case REQUEST -> "request";
            case FUTURE -> "future";
            case TRUST -> "trust";
        };
    }

    private static List<DialogueChoice> backChoices() {
        return List.of(
                new DialogueChoice("Back to conversation", "root")
        );
    }

    private static List<DialogueChoice> companionBackChoices(CompanionVoice voice) {
        return List.of(backToCompanion(voice));
    }

    private static DialogueChoice backToCompanion(CompanionVoice voice) {
        String name = voice == null ? "conversation" : voice.shortName();
        return new DialogueChoice("Back to " + name, "root");
    }

    private static DialogueChoice backToQuest(Quest quest) {
        String title = quest == null || quest.title == null || quest.title.isBlank() ? "this trouble" : quest.title;
        return new DialogueChoice("Back to " + title, "companion_quest");
    }

    private static DialogueChoice backToPersonal() {
        return new DialogueChoice("Back to personal matters", "companion_personal");
    }

    private static DialogueChoice backToHome() {
        return new DialogueChoice("Back to home and rest", "companion_home");
    }

    private static String memoryTopicLabel(String memory) {
        String cleaned = memory == null ? "" : memory.replaceAll("\\s+", " ").strip();
        String lower = cleaned.toLowerCase(Locale.ROOT);
        if (lower.startsWith("fought beside the player against ")) {
            return "Do you remember the last fight?";
        }
        if (lower.startsWith("survived a hard fight against ")) {
            return "Do you remember that hard fight?";
        }
        if (lower.startsWith("survived a decisive battle against ")) {
            return "Do you remember that decisive battle?";
        }
        if (lower.startsWith("started ") && lower.endsWith(" together.")) {
            return "Do you remember starting " + shortenTopic(cleaned.substring("Started ".length(), cleaned.length() - " together.".length())) + "?";
        }
        if (lower.startsWith("completed ") && lower.endsWith(" together.")) {
            return "Do you remember finishing " + shortenTopic(cleaned.substring("Completed ".length(), cleaned.length() - " together.".length())) + "?";
        }
        if (lower.startsWith("saw the player complete ")) {
            String topic = cleaned.substring("Saw the player complete ".length()).replaceAll("\\.$", "");
            return "Do you remember finishing " + shortenTopic(topic) + "?";
        }
        if (lower.startsWith("accepted ") && lower.contains("'s request")) {
            return "Do you remember your request?";
        }
        if (lower.startsWith("kept ") && lower.contains("'s request")) {
            return "Did I keep that promise well?";
        }
        if (lower.startsWith("reached ")) {
            return "This trust between us... what does it mean to you?";
        }
        if (cleaned.length() <= 62) {
            return cleaned;
        }
        return cleaned.substring(0, 59).strip() + "...";
    }

    private static String shortenTopic(String topic) {
        String cleaned = topic == null ? "" : topic.replaceAll("\\s+", " ").strip();
        if (cleaned.length() <= 32) {
            return cleaned;
        }
        return cleaned.substring(0, 29).strip() + "...";
    }

    private static String shortQuestTitle(Quest quest) {
        String title = quest == null || quest.title == null || quest.title.isBlank() ? "this" : quest.title.strip();
        if (title.length() <= 30) {
            return title;
        }
        return title.substring(0, 27).strip() + "...";
    }

    private static String shortNpcName(Npc npc) {
        if (npc == null || npc.name() == null || npc.name().isBlank()) {
            return "friend";
        }
        String[] parts = npc.name().strip().split("\\s+");
        if (parts.length == 0 || parts[0].isBlank()) {
            return "friend";
        }
        return parts[0];
    }

    private static String pick(List<String> pool, Random random) {
        if (pool == null || pool.isEmpty()) {
            return "";
        }
        return pool.get(random.nextInt(pool.size()));
    }

    private static String questDialog(Quest quest, boolean followUp) {
        if (quest == null) {
            return "";
        }
        return switch (questDialogueStage(quest)) {
            case OFFER -> followUp
                    ? questOfferDetailLine(quest)
                    : cleanQuestLine(quest.activeStartDialog(), "");
            case ACCEPTED -> followUp
                    ? questWhereLine(quest)
                    : questAcceptedLine(quest);
            case IN_PROGRESS -> followUp
                    ? questProgressLine(quest)
                    : cleanQuestLine(quest.activeProgressDialog(), "") + " " + questProgressLine(quest);
            case READY -> followUp
                    ? questReadyMeaningLine(quest)
                    : cleanQuestLine(quest.activeReadyDialog(), "");
            case COMPLETED -> followUp
                    ? questAftermathLine(quest)
                    : cleanQuestLine(quest.activeCompleteDialog(), "");
        };
    }

    private static String questAcceptEffect(Quest quest) {
        return quest == null ? "" : "quest:accept:" + quest.id;
    }

    private static String questTurnInEffect(Quest quest) {
        return quest == null ? "" : "quest:turnin:" + quest.id;
    }

    private static String questOutcomeEffect(Quest quest, String outcome) {
        return quest == null ? "" : "quest:outcome:" + quest.id + ":" + quest.activeStage().id() + ":" + outcome;
    }

    private static void addCompanionQuestTree(Map<String, DialogueNode> nodes, List<DialogueChoice> root, Npc speaker,
                                               CompanionVoice voice, Quest quest, int relationship, DialogueContext context, String precedingReport) {
        root.add(new DialogueChoice("Discuss " + quest.title + ".", "companion_quest"));
        addGroundedQuestTree(nodes, quest, "companion_quest", voice, context, speaker, precedingReport);
    }

    private static void addQuestOfferNodes(Map<String, DialogueNode> nodes, Npc npc, Quest quest, String prefix, String precedingReport) {
        if (quest != null) addGroundedQuestTree(nodes, quest, prefix, companionVoice(npc), DialogueContext.empty(), npc, precedingReport);
    }

    private static void addGroundedQuestTree(Map<String, DialogueNode> nodes, Quest quest, String prefix,
                                             CompanionVoice voice, DialogueContext context, Npc speaker, String precedingReport) {
        List<DialogueChoice> choices = new ArrayList<>();
        List<DialogueChoice> back = List.of(new DialogueChoice("About helping you...", prefix + "_responses"),
                new DialogueChoice("Back to topics", "root"));
        NpcQuestStories.Story localStory = NpcQuestStories.story(quest);
        if (MainStoryContent.supports(quest)) {
            addCampaignTopics(nodes, choices, prefix + "_story", MainStoryContent.topics(quest, speaker), quest, back);
        } else if (localStory != null) {
            addCampaignTopics(nodes, choices, prefix + "_story", NpcQuestStories.topics(quest, speaker), quest, back);
        } else {
            choices.add(new DialogueChoice("How did this start?", prefix + "_purpose"));
        }
        if (!precedingReport.isBlank()) {
            choices.add(new DialogueChoice("How did our earlier work lead to this?", prefix + "_earlier"));
            addPagedNode(nodes, prefix + "_earlier", dialoguePassages(precedingReport + " "
                    + QuestNarrative.clean(quest.stages.getFirst().startDialog())), back);
        }
        if (quest.accepted || quest.completed)
            choices.add(new DialogueChoice("What do we know so far?", prefix + "_facts"));
        choices.add(new DialogueChoice(quest.completed ? "What happens here now?" : "Where should I begin?", prefix + "_next"));
        addNode(nodes, prefix + "_purpose", voice == null && quest.companionQuest()
                ? "You are helping with " + quest.title + ". " + quest.description : QuestNarrative.purpose(quest), back);
        List<DialogueChoice> evidenceChoices = new ArrayList<>();
        for (Quest.QuestStage stage : quest.stages) {
            if (!quest.observedStages.contains(stage.id())) continue;
            String evidenceNode = prefix + "_evidence_" + stage.id();
            evidenceChoices.add(new DialogueChoice("Review: " + stage.title(), evidenceNode));
            addNode(nodes, evidenceNode, "Your notes record: " + QuestNarrative.clean(stage.readyDialog()),
                    List.of(new DialogueChoice("Back to recorded findings", prefix + "_facts"),
                            new DialogueChoice("Back to topics", "root")));
        }
        evidenceChoices.addAll(back);
        addNode(nodes, prefix + "_facts", QuestNarrative.findings(quest), evidenceChoices);
        addNode(nodes, prefix + "_next", QuestNarrative.instruction(quest), back);
        addNode(nodes, prefix + "_unknown", QuestNarrative.uncertainty(quest), back);
        if (!quest.accepted && !quest.completed) {
            choices.add(new DialogueChoice(localStory == null ? "I'll help with " + quest.activeStage().title() + "." : localStory.accept(),
                    prefix + "_accept", 0, questAcceptEffect(quest)));
            addNode(nodes, prefix + "_accept", "Agreed. " + QuestNarrative.instruction(quest), back);
        } else if (quest.ready() && !quest.completed) {
            if (!quest.companionQuest() || voice != null) {
                choices.add(new DialogueChoice("Report the completed work.", prefix + "_report", 0, questTurnInEffect(quest)));
            }
            addNode(nodes, prefix + "_report", QuestNarrative.clean(quest.activeCompleteDialog()), back);
        } else if (quest.accepted && !quest.completed && quest.activeObjectiveKind() == Quest.ObjectiveKind.CHOICE
                && !context.questOutcome(quest.outcomeKey()).isBlank()) {
            choices.add(new DialogueChoice("Keep our recorded decision.", prefix + "_retained", 0,
                    questOutcomeEffect(quest, context.questOutcome(quest.outcomeKey()))));
            addNode(nodes, prefix + "_retained", "Our earlier decision stands. The new findings do not change the commitment we already made.", back);
        } else if (quest.accepted && !quest.completed && quest.activeObjectiveKind() == Quest.ObjectiveKind.CHOICE) {
            List<CompanionChoiceOption> options = voice == null ? List.of() : companionChoiceOptions(voice, quest);
            for (int i = 0; i < options.size(); i++) {
                CompanionChoiceOption option = options.get(i);
                String node = prefix + "_decision_" + i;
                choices.add(new DialogueChoice(option.label(), node, 0, questOutcomeEffect(quest, option.outcome())));
                addNode(nodes, node, option.response(), back);
            }
            if (options.isEmpty()) {
                for (String outcome : List.of("truth", "protect", "mercy", "accountability")) {
                    if (localStory != null && quest.title.equals("Hard Truth")) {
                        String node = prefix + "_decision_" + outcome;
                        choices.add(new DialogueChoice(NpcQuestStories.grainDecision(outcome, false), node, 0, questOutcomeEffect(quest, outcome)));
                        addNode(nodes, node, NpcQuestStories.grainDecision(outcome, true), back);
                        continue;
                    }
                    String label = switch (outcome) {
                        case "truth" -> "Give the people involved the findings we have verified.";
                        case "protect" -> "Keep the witness's identity private while we investigate.";
                        case "mercy" -> "Ask for restitution before seeking punishment.";
                        default -> "Take the verified findings to the local authority.";
                    };
                    choices.add(new DialogueChoice(label, prefix + "_decision", 0, questOutcomeEffect(quest, outcome)));
                }
                addNode(nodes, prefix + "_decision", "That records the approach you chose. Any remaining action must still be completed.", back);
            }
        } else if (quest.accepted && !quest.completed && quest.activeObjectiveKind().conversationObjective()) {
            choices.add(new DialogueChoice(CompanionQuestContent.exchangeLabel(quest), prefix + "_exchange", 0,
                    "quest:discuss:" + quest.id + ":" + quest.activeStage().id()));
            addNode(nodes, prefix + "_exchange", QuestNarrative.clean(quest.activeReadyDialog()), back);
        }
        List<String> decisions = new ArrayList<>();
        Quest earlier = quest.copy();
        for (int i = 0; i < quest.stages.size(); i++) {
            earlier.stageIndex = i;
            String outcome = context.questOutcome(earlier.outcomeKey());
            if (earlier.activeObjectiveKind() != Quest.ObjectiveKind.CHOICE || outcome.isBlank()) continue;
            String chosen = voice == null ? context.questOutcomeLabel(earlier.outcomeKey())
                    : companionChoiceOptions(voice, earlier).stream().filter(o -> o.outcome().equals(outcome))
                    .map(CompanionChoiceOption::label).findFirst().orElse(context.questOutcomeLabel(earlier.outcomeKey()));
            decisions.add("For " + earlier.activeStage().title() + ", you chose: " + chosen);
        }
        if (!decisions.isEmpty()) {
            choices.add(new DialogueChoice("Which approach did we agree on?", prefix + "_chosen"));
            addNode(nodes, prefix + "_chosen", String.join(" ", decisions) + " " + QuestNarrative.instruction(quest), back);
        }
        choices.add(new DialogueChoice("I need to check something first.", "root"));
        String topic = NpcQuestStories.subject(quest);
        if (MainStoryContent.supports(quest)) topic = MainStoryContent.subject(quest, speaker);
        List<String> passages = new ArrayList<>();
        if (!precedingReport.isBlank() && !quest.accepted && !quest.completed)
            passages.addAll(dialoguePassages(precedingReport));
        if (localStory != null && !quest.accepted && !quest.completed) {
            if (speaker != null) passages.add(NpcQuestStories.occupation(speaker));
            passages.addAll(dialoguePassages(quest.activeStartDialog()));
            passages.add(localStory.concern());
        } else {
            NpcBackstories.Profile history = NpcBackstories.profile(speaker);
            if (history != null && !quest.accepted && !quest.completed && quest.stageIndex == 0
                    && quest.id.equals(speaker.questId())) {
                passages.addAll(dialoguePassages(history.background()));
                passages.addAll(dialoguePassages(history.reasonToAsk()));
            }
            // Repeat the observation that caused the next step, but only if the player recorded it.
            if (quest.stageIndex > 0 && !quest.completed && !quest.ready()) {
                Quest.QuestStage previous = quest.stages.get(quest.stageIndex - 1);
                if (quest.observedStages.contains(previous.id()))
                    passages.addAll(dialoguePassages(QuestNarrative.clean(previous.readyDialog())));
            }
            passages.addAll(dialoguePassages(topic));
        }
        addPagedNode(nodes, prefix, passages, choices);
    }

    private static void addCampaignTopics(Map<String, DialogueNode> nodes, List<DialogueChoice> choices,
                                          String prefix, List<MainStoryContent.Topic> topics, Quest quest,
                                          List<DialogueChoice> back) {
        for (int i = 0; i < topics.size(); i++) {
            MainStoryContent.Topic topic = topics.get(i);
            if (!topic.evidence().isEmpty() && (quest == null || !MainStoryContent.available(quest, topic))) continue;
            String id = prefix + "_" + i;
            choices.add(new DialogueChoice(topic.question(), id));
            List<DialogueChoice> replies = new ArrayList<>();
            addCampaignTopics(nodes, replies, id, topic.replies(), quest, back);
            replies.addAll(back);
            addPagedNode(nodes, id, dialoguePassages(topic.answer()), replies);
        }
    }

    private static List<String> dialoguePassages(String text) {
        List<String> passages = new ArrayList<>();
        StringBuilder passage = new StringBuilder();
        for (String sentence : text.split("(?<=[.!?])\\s+|\\n\\n")) {
            if (!passage.isEmpty() && passage.length() + sentence.length() > 290) {
                passages.add(passage.toString());
                passage.setLength(0);
            }
            if (!passage.isEmpty()) passage.append(' ');
            passage.append(sentence);
        }
        if (!passage.isEmpty()) passages.add(passage.toString());
        return passages.isEmpty() ? List.of(text) : passages;
    }

    /** Advancing narration never accepts a quest, awards approval, or changes an objective. */
    private static void addPagedNode(Map<String, DialogueNode> nodes, String id, List<String> passages,
                                     List<DialogueChoice> responses) {
        for (int i = 0; i < passages.size(); i++) {
            String current = i == 0 ? id : id + "_page_" + i;
            boolean last = i == passages.size() - 1;
            addNode(nodes, current, passages.get(i), last ? responses
                    : List.of(new DialogueChoice("Continue", id + "_page_" + (i + 1))));
        }
        addNode(nodes, id + "_responses", passages.getLast(), responses);
    }

    static boolean allowedQuestOutcome(Npc npc, Quest quest, String outcome) {
        CompanionVoice voice = companionVoice(npc);
        List<CompanionChoiceOption> options = voice == null ? List.of() : companionChoiceOptions(voice, quest);
        return options.isEmpty() ? List.of("truth", "protect", "mercy", "accountability").contains(outcome)
                : options.stream().anyMatch(option -> option.outcome().equals(outcome));
    }

    private enum QuestDialogueStage {
        OFFER,
        ACCEPTED,
        IN_PROGRESS,
        READY,
        COMPLETED
    }

    private static QuestDialogueStage questDialogueStage(Quest quest) {
        if (quest == null) {
            return QuestDialogueStage.OFFER;
        }
        if (quest.completed) {
            return QuestDialogueStage.COMPLETED;
        }
        if (quest.ready()) {
            return QuestDialogueStage.READY;
        }
        if (!quest.accepted) {
            return QuestDialogueStage.OFFER;
        }
        if (quest.progress > 0) {
            return QuestDialogueStage.IN_PROGRESS;
        }
        return QuestDialogueStage.ACCEPTED;
    }

    private static String questReasonQuestion(Npc npc, Quest quest) {
        return npcQuestOption(npc, quest, "quest_reason", 1, questReasonQuestion(quest), switch (npcProfession(npc)) {
            case "guard" -> "What happens if no one answers this?";
            case "healer" -> "Who suffers first if we delay?";
            case "archivist" -> "What truth is this trying to uncover?";
            case "merchant" -> "Who pays the price if this goes unanswered?";
            case "scout" -> "What did the road show you?";
            case "farmer" -> "What does this grow into if ignored?";
            case "quartermaster" -> "What breaks first if we leave it alone?";
            default -> "Why does this matter here and now?";
        });
    }

    private static String questWhereQuestion(Npc npc, Quest quest) {
        return npcQuestOption(npc, quest, "quest_where", 2, questWhereQuestion(quest), switch (npcProfession(npc)) {
            case "guard" -> "Where do you need boots on the ground?";
            case "healer" -> "Where is help needed first?";
            case "archivist" -> "Where does the evidence begin?";
            case "merchant" -> "Which road carries the risk?";
            case "scout" -> "Where does the trail turn honest?";
            case "farmer" -> "Where did the trouble first take root?";
            case "quartermaster" -> "Where do the supplies or people need to move?";
            default -> "Point me at the first true step.";
        });
    }

    private static String questCommitLabel(Npc npc, Quest quest) {
        return npcQuestOption(npc, quest, "quest_commit", 3, questCommitLabel(quest), switch (npcProfession(npc)) {
            case "guard" -> "I will stand where you need me.";
            case "healer" -> "I will get help where it matters.";
            case "archivist" -> "I will bring back something the truth can stand on.";
            case "merchant" -> "I will see the risk paid properly.";
            case "scout" -> "I will follow the trail and return with an answer.";
            case "farmer" -> "I will pull this up before it roots deeper.";
            case "quartermaster" -> "I will make the plan hold in the real world.";
            default -> "I will see it done.";
        });
    }

    private static String questDoubtLabel(Npc npc, Quest quest) {
        return npcQuestOption(npc, quest, "quest_doubt", 4, questDoubtLabel(quest), switch (npcProfession(npc)) {
            case "guard" -> "What if this is the wrong fight?";
            case "healer" -> "What if helping one person hurts another?";
            case "archivist" -> "What if the answer has been edited already?";
            case "merchant" -> "What if someone profits from my saying yes?";
            case "scout" -> "What if the trail is showing us bait?";
            case "farmer" -> "What if this trouble is already too deep?";
            case "quartermaster" -> "What if we do not have enough to do this right?";
            default -> "What if this is not as simple as it sounds?";
        });
    }

    private static String questReasonCommitLabel(Npc npc, Quest quest) {
        return npcQuestOption(npc, quest, "quest_reason_commit", 5, questReasonCommitLabel(quest), switch (npcProfession(npc)) {
            case "guard" -> "Then I will answer before fear does.";
            case "healer" -> "Then I move before pain becomes permanent.";
            case "archivist" -> "Then I will bring back proof, not guesses.";
            case "merchant" -> "Then I will pay the road its due and return.";
            case "scout" -> "Then I will follow the sign that matters.";
            case "farmer" -> "Then I will stop it before it seeds.";
            case "quartermaster" -> "Then I will make the practical part hold.";
            default -> "Then I understand enough to act.";
        });
    }

    private static String questOpinionCommitLabel(Npc npc, Quest quest) {
        return npcQuestOption(npc, quest, "quest_opinion_commit", 6, questOpinionCommitLabel(quest), switch (npcProfession(npc)) {
            case "guard" -> "I hear you. I will not look away.";
            case "healer" -> "I hear you. I will treat this like a life, not an errand.";
            case "archivist" -> "I hear you. The record will not be left blank.";
            case "merchant" -> "I hear you. I will keep the bargain honest.";
            case "scout" -> "I hear you. I will trust the trail and my eyes.";
            case "farmer" -> "I hear you. I will tend to the root, not the rumor.";
            case "quartermaster" -> "I hear you. I will bring back what the plan needs.";
            default -> "I hear you. I will carry that with me.";
        });
    }

    private static String questDelayLabel(Npc npc, Quest quest) {
        return npcQuestOption(npc, quest, "quest_delay", 7, questDelayLabel(quest), switch (npcProfession(npc)) {
            case "guard" -> "Not yet. I need to check my footing.";
            case "healer" -> "Not yet. I need to make sure help does not arrive careless.";
            case "archivist" -> "Not yet. I need the question clear first.";
            case "merchant" -> "Not yet. I need to know what I am really carrying.";
            case "scout" -> "Not yet. I need to read the approach.";
            case "farmer" -> "Not yet. I need to see how deep this goes.";
            case "quartermaster" -> "Not yet. I need the supplies to match the promise.";
            default -> "Not yet. I need to choose carefully.";
        });
    }

    private static String questTurnInLabel(Npc npc, Quest quest) {
        return npcQuestOption(npc, quest, "quest_turnin", 8, questTurnInLabel(quest), switch (npcProfession(npc)) {
            case "guard" -> "The watch has an answer.";
            case "healer" -> "Help reached where it was needed.";
            case "archivist" -> "I brought back proof for the record.";
            case "merchant" -> "The risk has been paid and answered.";
            case "scout" -> "The trail has given up its truth.";
            case "farmer" -> "I dealt with what had taken root.";
            case "quartermaster" -> "The practical work is accounted for.";
            default -> "I brought back an answer.";
        });
    }

    private static String questReadyMeaningQuestion(Npc npc, Quest quest) {
        return npcQuestOption(npc, quest, "quest_ready_meaning", 9, questReadyMeaningQuestion(quest), switch (npcProfession(npc)) {
            case "guard" -> "What does this let the watch do now?";
            case "healer" -> "Who gets to breathe easier now?";
            case "archivist" -> "What does this prove clearly enough to keep?";
            case "merchant" -> "Who can act now that the risk is known?";
            case "scout" -> "What changes on the road after this?";
            case "farmer" -> "What grows differently because of this?";
            case "quartermaster" -> "What does this make possible now?";
            default -> "What does this change for the people here?";
        });
    }

    private static String questAfterQuestion(Npc npc, Quest quest) {
        return npcQuestOption(npc, quest, "quest_after", 10, questAfterQuestion(quest), switch (npcProfession(npc)) {
            case "guard" -> "How does the watch remember this?";
            case "healer" -> "Who still needs tending after the danger passes?";
            case "archivist" -> "What line gets written after this?";
            case "merchant" -> "Who benefits now that the road is clearer?";
            case "scout" -> "What does the trail look like tomorrow?";
            case "farmer" -> "What is left to mend after the field is quiet?";
            case "quartermaster" -> "What do we restock before the next trouble?";
            default -> "Where does this leave everyone?";
        });
    }

    private static String questCompletedCostQuestion(Npc npc, Quest quest) {
        return npcQuestOption(npc, quest, "quest_completed_cost", 11, questCompletedCostQuestion(quest), switch (npcProfession(npc)) {
            case "guard" -> "What did holding the line cost?";
            case "healer" -> "Who still hurts because of it?";
            case "archivist" -> "What did the truth fail to fix?";
            case "merchant" -> "What debt remains after the work is done?";
            case "scout" -> "What danger learned from us?";
            case "farmer" -> "What scar stays in the ground?";
            case "quartermaster" -> "What did we spend that cannot be replaced?";
            default -> "What did it cost more than people admit?";
        });
    }

    private static String questProgressQuestion(Npc npc, Quest quest) {
        return npcQuestOption(npc, quest, "quest_progress", 12, questProgressQuestion(quest), switch (npcProfession(npc)) {
            case "guard" -> "What still stands between us and safe?";
            case "healer" -> "How much hurt is still unanswered?";
            case "archivist" -> "Which piece of the account is missing?";
            case "merchant" -> "What part of the bargain is unfinished?";
            case "scout" -> "Where does the trail still run cold?";
            case "farmer" -> "How much of the trouble is still rooted?";
            case "quartermaster" -> "What is still short of enough?";
            default -> "What is still unfinished?";
        });
    }

    private static String questWarningQuestion(Npc npc, Quest quest) {
        return npcQuestOption(npc, quest, "quest_warning", 13, questWarningQuestion(quest), switch (npcProfession(npc)) {
            case "guard" -> "Where would you expect the first mistake?";
            case "healer" -> "What mistake gets someone hurt?";
            case "archivist" -> "What detail would a liar polish smooth?";
            case "merchant" -> "Who would gain from stopping this?";
            case "scout" -> "What sign would make you turn back?";
            case "farmer" -> "What warning does the land give first?";
            case "quartermaster" -> "What practical thing will fail first?";
            default -> "What should I be afraid of missing?";
        });
    }

    private static String npcQuestOption(Npc npc, Quest quest, String context, int salt, String baseLabel, String roleLabel) {
        if (npc == null || quest == null || companionVoiceForQuest(quest) != null) {
            return baseLabel;
        }
        return optionVariant(npcOptionSeed(npc, context + "_" + quest.activeObjectiveKind()), salt, List.of(
                baseLabel,
                roleLabel
        ));
    }

    private static String questReasonQuestion(Quest quest) {
        if (quest == null) {
            return "Why does this matter?";
        }
        return switch (quest.activeObjectiveKind()) {
            case DEFEAT -> "Who gets hurt if this keeps hunting?";
            case RESCUE -> "Who are we trying not to lose?";
            case DEFEND -> "What breaks if this place falls?";
            case RAID_DEFENSE -> "What are they trying to take from us?";
            case GATHER -> "Why does this need to come back?";
            case DELIVER -> "Why can this not pass through other hands?";
            case VISIT -> "What made this place worth checking?";
            case SEARCH -> "What are we looking for?";
            case TALK -> "Why does this conversation matter?";
            case ASK_AROUND -> "Which rumor could get someone killed?";
            case REPORT -> "Who needs the unpolished truth?";
            case ESCORT -> "Why does this road need protection?";
            case CHOICE -> "Why put the choice in my hands?";
        };
    }

    private static String questWhereQuestion(Quest quest) {
        if (quest == null) {
            return "Point me at the first step.";
        }
        return switch (quest.activeObjectiveKind()) {
            case DEFEAT -> "Where was it last seen?";
            case RESCUE -> "Where do I reach them?";
            case DEFEND -> "Where does the line need to hold?";
            case RAID_DEFENSE -> "Where do we make our stand?";
            case GATHER -> "Where do I find what you need?";
            case DELIVER -> "Where does this need to land?";
            case VISIT -> "Show me the place I should inspect.";
            case SEARCH -> "Where do I start looking?";
            case TALK -> "Where do I find the right person?";
            case ASK_AROUND -> "Where should I start asking?";
            case REPORT -> "Where do I bring the answer?";
            case ESCORT -> "Which road are we taking?";
            case CHOICE -> "Where does the decision happen?";
        };
    }

    private static String questPersonalQuestion(Quest quest) {
        if (quest == null) {
            return "What does this mean to you?";
        }
        return switch (quest.activeObjectiveKind()) {
            case DEFEAT -> "How close has this come to you?";
            case RESCUE -> "Who are we trying not to lose for you?";
            case DEFEND -> "What are you afraid this place becomes?";
            case RAID_DEFENSE -> "What do you need to see survive?";
            case GATHER -> "Why bring me into this instead of doing it alone?";
            case DELIVER -> "Why trust me with this?";
            case VISIT -> "What are you hoping I notice?";
            case SEARCH -> "What answer are you bracing for?";
            case TALK -> "What should I listen for under their words?";
            case ASK_AROUND -> "Which version do you already doubt?";
            case REPORT -> "What answer would change your mind?";
            case ESCORT -> "Who are you trying to get home?";
            case CHOICE -> "Why put the choice in my hands?";
        };
    }

    private static String questPersonalQuestion(Npc npc, Quest quest) {
        String name = shortNpcName(npc);
        if (quest == null) {
            return "What does this mean to you?";
        }
        return switch (quest.activeObjectiveKind()) {
            case DEFEAT -> "How close has this come to you, " + name + "?";
            case RESCUE -> "Who are they to you?";
            case DEFEND -> "What are you afraid this place becomes?";
            case RAID_DEFENSE -> "What do you need to see survive?";
            case GATHER -> "What happens if I bring too little back?";
            case DELIVER -> "Why trust me with this?";
            case VISIT -> "What are you hoping I notice?";
            case SEARCH -> "What answer are you bracing for?";
            case TALK -> "What should I listen for under their words?";
            case ASK_AROUND -> "Which version do you already doubt?";
            case REPORT -> "What answer would change your mind?";
            case ESCORT -> "Who are you trying to get home?";
            case CHOICE -> "Which outcome scares you most?";
        };
    }

    private static String questReasonCommitLabel(Quest quest) {
        if (quest == null) {
            return "That is enough reason.";
        }
        return switch (quest.activeObjectiveKind()) {
            case DEFEAT -> "Then I stop it before it chooses another victim.";
            case RESCUE -> "Then I move before rescue becomes mourning.";
            case DEFEND -> "Then I will help hold it.";
            case RAID_DEFENSE -> "Then we hold the line now.";
            case GATHER -> "Then I will bring back enough to matter.";
            case DELIVER -> "Then I will carry it carefully.";
            case VISIT -> "Then I will make the place answer.";
            case SEARCH -> "Then I will look for the thing someone hid.";
            case TALK -> "Then I will ask plainly.";
            case ASK_AROUND -> "Then I will sort rumor from truth.";
            case REPORT -> "Then I will bring the truth back clean.";
            case ESCORT -> "Then I get them there alive.";
            case CHOICE -> "Then I will choose with open eyes.";
        };
    }

    private static String questOpinionCommitLabel(Quest quest) {
        if (quest == null) {
            return "I understand the stakes.";
        }
        return switch (quest.activeObjectiveKind()) {
            case DEFEAT -> "I understand. Leaving it alive is the crueler choice.";
            case RESCUE -> "I understand. We go before hope runs out.";
            case DEFEND -> "I understand. Some places have to stand.";
            case RAID_DEFENSE -> "I understand. Start the defense.";
            case GATHER -> "I understand. The proof matters.";
            case DELIVER -> "I understand. I will keep it out of careless hands.";
            case VISIT -> "I understand. I will trust the details.";
            case SEARCH -> "I understand. Hidden things still leave edges.";
            case TALK -> "I understand. The first answer may not be the true one.";
            case ASK_AROUND -> "I understand. Quiet details first.";
            case REPORT -> "I understand. No comfort-polishing.";
            case ESCORT -> "I understand. Boring and alive.";
            case CHOICE -> "I understand. I own the consequence.";
        };
    }

    private static String questDelayLabel(Quest quest) {
        if (quest == null) {
            return "I need to think first.";
        }
        return switch (quest.activeObjectiveKind()) {
            case RAID_DEFENSE -> "Not yet. I need to prepare the defense.";
            case DEFEAT, DEFEND, RESCUE -> "Not yet. I need to prepare properly.";
            case GATHER, DELIVER, VISIT, SEARCH -> "I need to think before I touch this.";
            case TALK, ASK_AROUND, REPORT, ESCORT, CHOICE -> "I need to choose my words first.";
        };
    }

    private static String questTurnInLabel(Quest quest) {
        if (quest == null) {
            return "I brought what you needed.";
        }
        return switch (quest.activeObjectiveKind()) {
            case DEFEAT -> "The threat is down.";
            case RESCUE -> "They are alive. I got them out.";
            case DEFEND -> "The place still stands.";
            case RAID_DEFENSE -> "The raid broke against us.";
            case GATHER -> "I brought back what you asked for.";
            case DELIVER -> "It reached the right hands.";
            case VISIT -> "I saw enough to answer you.";
            case SEARCH -> "I found what someone tried to hide.";
            case TALK -> "I had the conversation.";
            case ASK_AROUND -> "I heard the versions that matter.";
            case REPORT -> "I brought the truth back.";
            case ESCORT -> "They made it through.";
            case CHOICE -> "I made the choice.";
        };
    }

    private static String questReadyMeaningQuestion(Quest quest) {
        if (quest == null) {
            return "What does this change?";
        }
        return switch (quest.activeObjectiveKind()) {
            case DEFEAT -> "What changes now that it cannot hurt anyone?";
            case RESCUE -> "What happens for them now?";
            case DEFEND, RAID_DEFENSE -> "What does surviving buy us?";
            case GATHER -> "What will you do with what I brought?";
            case DELIVER -> "What changes now that it arrived?";
            case VISIT, SEARCH -> "What does this prove?";
            case TALK, ASK_AROUND, REPORT -> "What do we do with the truth now?";
            case ESCORT -> "Where does safety lead next?";
            case CHOICE -> "What does that choice cost?";
        };
    }

    private static String questAfterQuestion(Quest quest) {
        if (quest == null) {
            return "Where does this leave us?";
        }
        return switch (quest.activeObjectiveKind()) {
            case DEFEAT -> "What grows back after the fear leaves?";
            case RESCUE -> "How do they live after this?";
            case DEFEND, RAID_DEFENSE -> "How do people breathe after holding?";
            case GATHER -> "Who gets helped by this first?";
            case DELIVER -> "Who can act now?";
            case VISIT, SEARCH -> "What does this change now?";
            case TALK, ASK_AROUND, REPORT -> "Who believes us now?";
            case ESCORT -> "Who sleeps easier tonight?";
            case CHOICE -> "Who carries the consequence with me?";
        };
    }

    private static String questCompletedCostQuestion(Quest quest) {
        if (quest == null) {
            return "Did it cost more than expected?";
        }
        return switch (quest.activeObjectiveKind()) {
            case DEFEAT, DEFEND, RAID_DEFENSE -> "Did winning cost more than people will admit?";
            case RESCUE, ESCORT -> "Are they really safe now?";
            case GATHER, DELIVER -> "Was it enough for the people waiting?";
            case VISIT, SEARCH -> "Did the answer leave a worse question?";
            case TALK, ASK_AROUND, REPORT -> "Did the truth make things cleaner or harder?";
            case CHOICE -> "Do you think I chose well?";
        };
    }

    private static String questProgressQuestion(Quest quest) {
        if (quest == null) {
            return "Let me make sure I have this right.";
        }
        return switch (quest.activeObjectiveKind()) {
            case DEFEAT -> "How much danger is still walking?";
            case RESCUE -> "How much time do we have left?";
            case DEFEND -> "What still threatens the line?";
            case RAID_DEFENSE -> "What remains before the raid breaks?";
            case GATHER -> "How much more do you need?";
            case DELIVER -> "What still has to reach them?";
            case VISIT -> "What have I not checked yet?";
            case SEARCH -> "What piece is still missing?";
            case TALK -> "Who still needs to be asked?";
            case ASK_AROUND -> "Which version have I not heard?";
            case REPORT -> "What truth is still missing?";
            case ESCORT -> "How far until everyone is safe?";
            case CHOICE -> "What is still undecided?";
        };
    }

    private static String questWarningQuestion(Quest quest) {
        if (quest == null) {
            return "What would make you nervous out there?";
        }
        return switch (quest.activeObjectiveKind()) {
            case DEFEAT -> "What am I likely to miss while hunting it?";
            case RESCUE -> "What mistake gets them killed?";
            case DEFEND -> "Where does the line fail first?";
            case RAID_DEFENSE -> "What will the raiders try first?";
            case GATHER -> "What should I not disturb?";
            case DELIVER -> "Who might want this intercepted?";
            case VISIT -> "What would staged evidence look like?";
            case SEARCH -> "What should I be careful not to miss?";
            case TALK -> "What lie should I expect first?";
            case ASK_AROUND -> "Which rumor sounds useful but isn't?";
            case REPORT -> "What truth will they not want to hear?";
            case ESCORT -> "Where does the road turn ugly?";
            case CHOICE -> "Which choice only looks merciful?";
        };
    }

    private static String questOfferDetailLine(Quest quest) {
        if (quest.activeObjectiveKind().defenseMinigameObjective()) {
            return "If you say yes, the next sound is not talk but alarm. We choose a raid loadout, then hold "
                    + objectiveLocationLine(quest) + " against " + quest.activeTarget()
                    + ", so the people behind the wall can keep believing walls mean something. I can pay "
                    + quest.rewardGold + " gold, and the fight will be worth " + quest.rewardXp + " experience.";
        }
        if (weeklySideQuest(quest)) {
            return switch (questVariant(quest, 3, 3)) {
                case 0 -> "This is local trouble, close enough to bruise real people and near enough to answer before it spreads. "
                        + questActionMeaningLine(quest) + " I need you to "
                        + questActionPhrase(quest).toLowerCase(Locale.ROOT) + ". I can pay "
                        + quest.rewardGold + " gold, and the work is worth " + quest.rewardXp + " experience.";
                case 1 -> "Take it only if this week has room for one more promise, because promises become shelter or shame. "
                        + questActionMeaningLine(quest) + " The task is to "
                        + questActionPhrase(quest).toLowerCase(Locale.ROOT) + ". I can pay "
                        + quest.rewardGold + " gold, and the work is worth " + quest.rewardXp + " experience.";
                default -> "The settlement needs a practical answer before fear starts making its own laws. "
                        + questActionMeaningLine(quest) + " The work is to "
                        + questActionPhrase(quest).toLowerCase(Locale.ROOT) + ". I can pay "
                        + quest.rewardGold + " gold, and the work is worth " + quest.rewardXp + " experience.";
            };
        }
        return "Take it only if you mean to leave a mark on the matter, not merely a bootprint near it. "
                + questActionMeaningLine(quest) + " I need you to "
                + questActionPhrase(quest).toLowerCase(Locale.ROOT) + ". I can pay "
                + quest.rewardGold + " gold, and the work is worth " + quest.rewardXp + " experience.";
    }

    private static String questAcceptedLine(Quest quest) {
        if (weeklySideQuest(quest)) {
            return switch (questVariant(quest, 5, 3)) {
                case 0 -> "Good. Then this stops being a worry passed from mouth to mouth and becomes work with a witness. "
                        + questReasonLine(quest);
                case 1 -> "Good. Someone can stop rehearsing the worst ending for a while. This one has your name on it now. "
                        + questReasonLine(quest);
                default -> "All right. The request has crossed from hoping into doing, and that gives people room to breathe. "
                        + questReasonLine(quest);
            };
        }
        return "All right. Then the promise has a shape, and the shape is yours to carry. " + questReasonLine(quest);
    }

    private static String questReasonLine(Quest quest) {
        return questWorldStakeLine(quest) + " " + questActionMeaningLine(quest)
                + " What we know is this: " + quest.description;
    }

    private static String questWhereLine(Quest quest) {
        if (quest.activeObjectiveKind().defenseMinigameObjective()) {
            return "Say yes when your hands are steady. The raid defense starts immediately at "
                    + objectiveLocationLine(quest) + ", and first we choose the loadout.";
        }
        return spokenQuestObjectiveLine(quest) + " " + questLocationMeaningLine(quest)
                + " Then come back when you have something solid enough to answer for.";
    }

    private static String questProgressLine(Quest quest) {
        if (quest.activeObjectiveKind().defenseMinigameObjective()) {
            return "The raid is not settled yet. Return to " + objectiveLocationLine(quest)
                    + " and start the raid defense when you are ready to hold the line.";
        }
        int remaining = Math.max(0, quest.activeNeeded() - quest.progress);
        if (remaining <= 0) {
            if (weeklySideQuest(quest)) {
                return switch (questVariant(quest, 7, 3)) {
                    case 0 -> "That is enough for this week. Bring the proof back while it still feels immediate.";
                    case 1 -> "You have what was asked for. Return before the request turns into a town meeting.";
                    default -> "The practical part is done. Now carry the answer back to the person waiting on it.";
                };
            }
            return "You have enough. Bring the proof back while it can still change what people dare to do next.";
        }
        if (weeklySideQuest(quest)) {
            return switch (questVariant(quest, 11, 3)) {
                case 0 -> "You have " + quest.progress + "/" + quest.activeNeeded() + ". "
                        + remaining + " more " + quest.activeTarget() + (remaining == 1 ? "" : "s")
                        + " should keep this week's trouble from becoming next week's emergency.";
                case 1 -> "Progress is real but unfinished: " + quest.progress + "/" + quest.activeNeeded() + ". "
                        + remaining + " more " + quest.activeTarget() + (remaining == 1 ? "" : "s")
                        + " will make the answer sturdy.";
                default -> quest.progress + "/" + quest.activeNeeded() + " handled. Finish the last "
                        + remaining + " " + quest.activeTarget() + (remaining == 1 ? "" : "s")
                        + " and the settlement can stop guessing.";
            };
        }
        return "So far, it is " + quest.progress + "/" + quest.activeNeeded() + ". "
                + remaining + " more " + quest.activeTarget() + (remaining == 1 ? "" : "s")
                + " will turn suspicion into something harder to dismiss.";
    }

    private static String questWarningLine(Quest quest) {
        return switch (quest.activeObjectiveKind()) {
            case DEFEAT -> "Do not hunt the target so narrowly that you miss who sent it, fed it, or benefits from its fear.";
            case RESCUE -> "Reach them quickly, but do not mistake panic for a plan. A rescue still needs eyes.";
            case DEFEND -> "A defended place teaches you what the enemy wants badly enough to risk taking.";
            case RAID_DEFENSE -> "A raid tests more than strength. Watch what the attackers target when the settlement refuses to break.";
            case GATHER -> "Take what proves the work. Do not strip the place bare just because a ledger would look cleaner.";
            case DELIVER -> "Carry the thing carefully. Delivery is trust made physical.";
            case VISIT -> "Inspect the place, but distrust anything arranged too neatly. Staged evidence loves obedient eyes.";
            case SEARCH -> "Search for what refuses to be obvious. The hidden detail is usually carrying the honest weight.";
            case TALK -> "Ask plainly and listen past the first answer. People often rehearse the version that hurts least.";
            case ASK_AROUND -> "Do not take the loudest rumor as truth. Look for the detail repeated by people who did not speak together.";
            case REPORT -> "Say what happened without polishing it into comfort. A report loses value when it flatters the listener.";
            case ESCORT -> "Keep the road boring. Boring means everyone arrived alive.";
            case CHOICE -> "Make the choice with open eyes. Mercy, truth, and convenience are wearing similar coats today.";
        };
    }

    private static String questOpinionLine(Npc npc, Quest quest) {
        String name = npc == null ? "They" : npc.name();
        return switch (quest.activeObjectiveKind()) {
            case DEFEAT -> name + " does not like asking for blood, but likes leaving threats unanswered even less.";
            case RESCUE -> name + " is trying not to sound afraid for someone who may still be saved.";
            case DEFEND -> name + " knows a wall is only as strong as the people willing to stand at it.";
            case RAID_DEFENSE -> name + " knows a defended place is only real if it survives being tested.";
            case GATHER -> name + " thinks the object matters because it will make rumor measurable, and measurable things are harder to ignore.";
            case DELIVER -> name + " is trusting the player with something that should not pass through careless hands.";
            case VISIT -> name + " believes places tell the truth differently than people. This one needs someone willing to listen.";
            case SEARCH -> name + " suspects the truth survived, but only as a small thing someone hoped would be missed.";
            case TALK -> name + " thinks the right question, asked to the right person, may do more than a drawn blade.";
            case ASK_AROUND -> name + " wants rumor sorted into testimony before anyone is asked to bleed for it.";
            case REPORT -> name + " needs an answer clean enough to act on and honest enough to trust.";
            case ESCORT -> name + " cares less about heroic speed than about everyone reaching the other side.";
            case CHOICE -> name + " is asking for judgment, not obedience.";
        };
    }

    private static String questReadyMeaningLine(Quest quest) {
        if (weeklySideQuest(quest)) {
            return switch (questVariant(quest, 13, 3)) {
                case 0 -> "This is where a small task becomes an answer someone can use before fear invents a worse one. "
                        + cleanQuestLine(quest.activeReadyDialog(), "");
                case 1 -> "The work is done enough to stand in daylight, which is more than rumor ever manages. "
                        + cleanQuestLine(quest.activeReadyDialog(), "");
                default -> "Now the request has weight instead of worry, and weight can be carried to a decision. "
                        + cleanQuestLine(quest.activeReadyDialog(), "");
            };
        }
        return "This is the moment where work becomes testimony. " + cleanQuestLine(quest.activeReadyDialog(), "")
                + " If the proof is true, the next choice cannot hide behind ignorance.";
    }

    private static String questAftermathHintLine(Quest quest) {
        if (weeklySideQuest(quest)) {
            return switch (questVariant(quest, 17, 3)) {
                case 0 -> "After this, the week becomes a little easier to survive. That is not small to the people living it.";
                case 1 -> "The settlement will move on quickly, which is usually how you know the work mattered.";
                default -> "No one may sing about it, but fewer people will have to worry about this particular thing tomorrow.";
            };
        }
        return "After this, the road may not become safer all at once, but it becomes less ignorant. That is often how real victories begin.";
    }

    private static String questAftermathLine(Quest quest) {
        if (weeklySideQuest(quest)) {
            return switch (questVariant(quest, 19, 3)) {
                case 0 -> "The weekly work is finished. " + cleanQuestLine(quest.activeCompleteDialog(), "")
                        + " Small answers keep Alderfall stitched together.";
                case 1 -> "That task can leave the worry list. " + cleanQuestLine(quest.activeCompleteDialog(), "")
                        + " The settlement will feel the difference before it names it.";
                default -> "Finished, and in time to matter. " + cleanQuestLine(quest.activeCompleteDialog(), "")
                        + " Another ordinary problem failed to become a disaster.";
            };
        }
        return "The task is finished, but finished does not mean erased. "
                + cleanQuestLine(quest.activeCompleteDialog(), "") + " " + questCompletionMeaningLine(quest);
    }

    private static String questCompletedOpinionLine(Npc npc, Quest quest) {
        String name = npc == null ? "They" : npc.name();
        if (weeklySideQuest(quest)) {
            return switch (questVariant(quest, 23, 3)) {
                case 0 -> name + " looks relieved in the practical way of someone whose week has one less sharp edge.";
                case 1 -> name + " seems ready to call it ordinary, which is often how local gratitude protects itself.";
                default -> name + " treats the answer as useful first and kind second. Both seem true.";
            };
        }
        return name + " seems changed by the answer. Not healed exactly, but less alone with the question.";
    }

    private static String questDoubtResponse(Npc npc, Quest quest) {
        String name = npc == null ? "They" : npc.name();
        if (weeklySideQuest(quest)) {
            return switch (questVariant(quest, 29, 3)) {
                case 0 -> name + " accepts the hesitation, but the week will not pause for it. " + questReasonLine(quest);
                case 1 -> name + " does not press, only waits with the tired patience of someone who has asked because asking was necessary. "
                        + questReasonLine(quest);
                default -> name + " lets the refusal remain possible. The problem remains possible too. " + questReasonLine(quest);
            };
        }
        return name + " does not soften the request. " + questReasonLine(quest)
                + " Refusing is allowed. Pretending it is weightless is not.";
    }

    private static boolean weeklySideQuest(Quest quest) {
        return quest != null
                && quest.id != null
                && quest.id.startsWith("weekly_")
                && !quest.mainStoryQuest()
                && !quest.companionQuest();
    }

    private static int questVariant(Quest quest, int salt, int count) {
        if (count <= 1) {
            return 0;
        }
        int hash = quest == null || quest.id == null ? 0 : quest.id.hashCode();
        return Math.floorMod(hash + salt * 7919, count);
    }

    private static String questWorldStakeLine(Quest quest) {
        if (quest.mainStoryQuest()) {
            return "This is not a loose errand; it is one place where the larger dark can still be argued with.";
        }
        if (quest.companionQuest()) {
            return "This matters because someone has trusted you with the part of the story they cannot solve alone.";
        }
        if (weeklySideQuest(quest)) {
            return weeklyQuestStakesLine(quest);
        }
        return "Alderfall is changed less by speeches than by the moments when someone refuses to let trouble become weather.";
    }

    private static String questActionMeaningLine(Quest quest) {
        return switch (quest.activeObjectiveKind()) {
            case DEFEAT -> "Stopping " + quest.activeTarget()
                    + " means proving fear has a body, and bodies can be faced.";
            case RESCUE -> "Rescuing " + quest.activeTarget()
                    + " means returning a name to the living before the road learns to speak of them in past tense.";
            case DEFEND -> "Defending " + quest.activeTarget()
                    + " means telling the enemy there are still places people will not surrender politely.";
            case RAID_DEFENSE -> "Holding the line means the settlement remembers itself as a home, not a target.";
            case GATHER -> "Gathering " + spokenQuestTarget(quest)
                    + " means replacing guesswork with proof someone can hold in their hand.";
            case DELIVER -> "Delivering " + quest.activeTarget()
                    + " means keeping trust intact across a road built to break it.";
            case VISIT -> "Inspecting " + spokenQuestTarget(quest)
                    + " means letting a place testify when people have learned to lie carefully.";
            case SEARCH -> "Finding " + spokenQuestTarget(quest)
                    + " means pulling the hidden piece back into the light where decisions have to answer for it.";
            case TALK -> "Speaking with " + quest.activeTarget()
                    + " means giving truth one more chance before steel becomes the only language left.";
            case ASK_AROUND -> "Asking around about " + quest.activeTarget()
                    + " means sorting fear from fact before either one gets someone killed.";
            case REPORT -> "Reporting to " + quest.activeTarget()
                    + " means making sure power hears what happened, not merely what would flatter it.";
            case ESCORT -> "Escorting " + quest.activeTarget()
                    + " means making the road keep its oldest promise: passage, not punishment.";
            case CHOICE -> "Choosing what happens to " + quest.activeTarget()
                    + " means admitting that mercy, justice, and convenience do not always walk together.";
        };
    }

    private static String questLocationMeaningLine(Quest quest) {
        String place = objectiveLocationLine(quest);
        return switch (quest.activeObjectiveKind()) {
            case DEFEAT, RAID_DEFENSE, DEFEND -> "If " + place
                    + " breaks, the story people tell about safety breaks with it.";
            case RESCUE, ESCORT -> "The road near " + place
                    + " will remember whether it carried someone home or swallowed them quietly.";
            case GATHER, SEARCH, VISIT -> "That place matters because the truth is no longer in a mouth; it is in marks, objects, and what was left behind.";
            case DELIVER, REPORT -> "The distance matters because every mile gives fear a chance to edit the message.";
            case TALK, ASK_AROUND -> "The place matters because people speak differently when the walls know their names.";
            case CHOICE -> "The place matters because whatever you decide there will begin as action, not opinion.";
        };
    }

    private static String questCompletionMeaningLine(Quest quest) {
        return switch (quest.activeObjectiveKind()) {
            case DEFEAT -> "The threat is no longer a rumor large enough to command the room.";
            case RESCUE -> "Someone's absence has been answered with a return, and that changes every silence around them.";
            case DEFEND, RAID_DEFENSE -> "The defended place becomes a memory people can stand inside.";
            case GATHER -> "What was scattered is now evidence, and evidence gives frightened people a spine.";
            case DELIVER -> "The trust arrived whole, which is sometimes the rarest victory on the road.";
            case VISIT -> "The place has spoken. Now no one gets to pretend it was empty of meaning.";
            case SEARCH -> "The hidden thing has lost its shelter, and so has the lie built around it.";
            case TALK -> "Words did what blades would have done more crudely: they changed what can happen next.";
            case ASK_AROUND -> "Rumor has been forced into shape, and shaped truth can be carried.";
            case REPORT -> "The answer is in the right hands now. What they do with it will show who they are.";
            case ESCORT -> "A life crossed the dangerous stretch and remained a life, not a warning story.";
            case CHOICE -> "The choice has entered the world. Alderfall will answer it in time.";
        };
    }

    private static String weeklyQuestStakesLine(Quest quest) {
        return switch (questVariant(quest, 31, 4)) {
            case 0 -> "This is weekly work, which means it is the kind of trouble people survive only if someone notices it early.";
            case 1 -> "This is local trouble, and local trouble becomes law when everyone decides it is someone else's burden.";
            case 2 -> "No crown will hear about this, which is exactly why the people within reach have to answer it.";
            default -> "Alderfall survives on ordinary favors done before they harden into extraordinary grief.";
        };
    }

    private static String questActionPhrase(Quest quest) {
        return switch (quest.activeObjectiveKind()) {
            case DEFEAT -> "Hunt " + quest.activeTarget();
            case RESCUE -> "Rescue " + quest.activeTarget();
            case DEFEND -> "Defend " + quest.activeTarget();
            case RAID_DEFENSE -> "Repel " + quest.activeTarget();
            case GATHER -> "Gather " + quest.activeNeeded() + " " + quest.activeTarget();
            case DELIVER -> "Deliver " + quest.activeTarget();
            case VISIT -> "Inspect " + quest.activeNeeded() + " " + quest.activeTarget();
            case SEARCH -> "Search for " + quest.activeNeeded() + " " + quest.activeTarget();
            case TALK -> "Talk to " + quest.activeTarget();
            case ASK_AROUND -> "Ask around about " + quest.activeTarget();
            case REPORT -> "Report to " + quest.activeTarget();
            case ESCORT -> "Escort " + quest.activeTarget();
            case CHOICE -> "Decide what to do about " + quest.activeTarget();
        };
    }

    private static String spokenQuestObjectiveLine(Quest quest) {
        String place = objectiveLocationLine(quest);
        String target = spokenQuestTarget(quest);
        return switch (quest.activeObjectiveKind()) {
            case DEFEAT -> "Find " + target + " near " + place + ", and stop the threat before it grows bolder.";
            case RESCUE -> "Reach " + target + " near " + place + " before fear or injury gets the final word.";
            case DEFEND -> "Hold " + target + " at " + place + " and watch what the attackers reveal about themselves.";
            case RAID_DEFENSE -> "Repel " + target + " at " + place + " before the settlement learns fear as a habit.";
            case GATHER -> "Start at " + place + " and bring back " + target + ", not as cargo but as proof.";
            case DELIVER -> "Take " + target + " to " + place + " and keep it out of careless hands.";
            case VISIT -> "Go to " + place + " and inspect " + target + " closely enough that the place has to answer.";
            case SEARCH -> "Start at " + place + ". Look for " + target + ", especially whatever someone tried to make ordinary.";
            case TALK -> "Find " + target + " at " + place + ", ask plainly, and listen past the prepared answer.";
            case ASK_AROUND -> "Ask around " + place + " about " + target + ", then compare the quiet details.";
            case REPORT -> "Bring what you learned about " + target + " back to " + place + " without polishing it into comfort.";
            case ESCORT -> "Guide " + target + " through " + place + " safely; a boring road is a successful one.";
            case CHOICE -> "At " + place + ", decide what should happen with " + target + " and own the consequence.";
        };
    }

    private static String spokenQuestTarget(Quest quest) {
        int needed = quest.activeNeeded();
        String target = quest.activeTarget();
        return switch (quest.activeObjectiveKind()) {
            case GATHER, SEARCH, VISIT -> needed > 1 ? needed + " " + target : target;
            default -> target;
        };
    }

    private static String objectiveLocationLine(Quest quest) {
        String map = readableId(quest.activeObjectiveMapId() == null || quest.activeObjectiveMapId().isBlank()
                ? WorldMap.OVERWORLD_ID
                : quest.activeObjectiveMapId());
        if (quest.activeObjectiveLocationKind() != null && !quest.activeObjectiveLocationKind().isBlank()) {
            return "the " + readableId(quest.activeObjectiveLocationKind()) + " near " + map;
        }
        return map;
    }

    private static String readableId(String id) {
        if (id == null || id.isBlank()) {
            return "the road";
        }
        String cleaned = id.replace("village_", "")
                .replace("city_", "")
                .replace("town_", "")
                .replace(WorldMap.OVERWORLD_ID, "Alderfall")
                .replace('_', ' ')
                .strip();
        if (cleaned.isBlank()) {
            return "the road";
        }
        StringBuilder result = new StringBuilder();
        for (String word : cleaned.split("\\s+")) {
            if (word.isBlank()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                result.append(word.substring(1));
            }
        }
        return result.toString();
    }

    private static String professionDialog(Npc npc, Random random) {
        String profession = npcProfession(npc);
        List<String> pool = switch (profession) {
            case "bartender" -> List.of(
                    "Rumor: People joke at the bar until the roads start taking names. Then every cup becomes a confession about who they are afraid will not come home.",
                    "Rumor: Three miners came through with frost in their beards and sand in their packs. That is not getting lost; that is two parts of Alderfall touching where they should not.",
                    "Rumor: Someone is buying empty bottles by the crate. Empty vessels mean messages, medicine, or mourning, and I dislike all three in bulk."
            );
            case "blacksmith" -> List.of(
                    "Forge: Metal remembers every choice its owner made. A nick near the guard says panic; a worn grip says someone kept standing anyway.",
                    "Forge: Snow, sand, marshwater, road dust; every place damages a blade differently, the same way every fear damages a person differently.",
                    "Forge: Bring me ore, old hinges, or enemy buckles. I turn broken things into tools because Alderfall cannot afford to waste survival."
            );
            case "baker" -> List.of(
                    "Oven: Bread is how a village tells itself there will be a morning. When flour runs short, courage starts running short beside it.",
                    "Oven: People confess more over warm bread than under threat. Hunger strips the theater from a person.",
                    "Oven: If the crust cracks too wide, rain is coming. If nobody reaches for seconds, trouble is already sitting among us."
            );
            case "merchant" -> List.of(
                    "Trade: I price goods by distance and dread. A caravan carries more than wares; it carries proof that towns still believe in each other.",
                    "Trade: The best cargo is boring. The worst cargo makes people lie about why it needs moving before dawn.",
                    "Trade: Desperate customers spend like the future is imaginary. I would rather sell them a plan than a miracle."
            );
            case "archivist" -> List.of(
                    "Records: A missing page is not absence. It is permission for someone powerful to decide what happened.",
                    "Records: History is not dead. It waits until the living repeat themselves, then opens the drawer with the receipt.",
                    "Records: Any ruin with perfect silence is lying. Stone always remembers who asked it to keep quiet."
            );
            case "guard" -> List.of(
                    "Watch: Trouble changes uniforms by region, but the first casualty is usually the same: ordinary people stop trusting the road.",
                    "Watch: We rotate the night posts so fear does not learn anyone's favorite hiding place. Fear is lazy when you let it study you.",
                    "Watch: A quiet road is either safe, watched, or bait. The difference decides whether children are allowed outside tomorrow."
            );
            case "healer" -> List.of(
                    "Remedy: Desert folk need water, mountain folk need warmth, marsh folk need clean cuts. Most of healing is learning what a place takes first.",
                    "Remedy: Half of medicine is asking where it hurts. The other half is noticing what answer shame is trying to bury.",
                    "Remedy: If a wound smells sweet, the body is negotiating badly. If a town smells afraid, the wound is larger than one patient."
            );
            case "farmer" -> List.of(
                    "Fieldwork: Good soil tells the truth. When the birds go quiet, the field is not peaceful; it is warning whoever still listens.",
                    "Fieldwork: You learn patience from crops, suspicion from crows, and how quickly hunger turns neighbors into judges.",
                    "Fieldwork: A fence is a polite argument with the wilderness. When it falls, everyone learns how thin politeness was."
            );
            case "scout" -> List.of(
                    "Trailcraft: The safest path is rarely the shortest. A shortcut is just a story the road tells to impatient people.",
                    "Trailcraft: If the road looks empty, check what the birds are avoiding. The living world votes with its feet first.",
                    "Trailcraft: I mark routes by what can go wrong there. A good map is not pretty; it is honest about where hope gets expensive."
            );
            case "quartermaster" -> List.of(
                    "Supplies: Count arrows twice, medicine three times, and socks every time it rains. Bravery fails quickly when logistics lose interest.",
                    "Supplies: A heroic charge eats rations like anyone else. Songs skip that part because songs never had to carry the pot.",
                    "Supplies: People remember who swung the sword. I remember who packed the bandages, because victory is useless if nobody survives to need them."
            );
            default -> List.of(
                    "Local talk: Everyone here watches the horizon because the horizon is where trouble first becomes everyone's business.",
                    "Local talk: You can tell who is new by who trusts a quiet morning. The rest of us ask what made it quiet.",
                    "Local talk: If people seem jumpy, it is because Alderfall has taught them that ignoring a warning is also a choice."
            );
        };
        return pick(pool, random);
    }

    private static List<String> npcSpecificDialog(Npc npc) {
        return switch (npcDialogKey(npc)) {
            case "seraphine vale" -> List.of(
                    "Personal: The red silk is not vanity. It taught creditors to look at the dress while my hands found the contract knife.",
                    "Personal: Riverside taught me that a smile can be armor, a hem can hide a ledger, and a beautiful room can still be a cage.",
                    "Personal: If I sound polished, listen harder. Polished things are usually hiding the scratches."
            );
            case "maera quill" -> List.of(
                    "Personal: These blue robes were archive issue before I stitched the moonmarks in myself. The Archive dislikes unauthorized constellations.",
                    "Personal: I carry books because paper is lighter than regret, though my satchel has been testing that theory.",
                    "Personal: Stonegate's old scholars looked up and called it navigation. Modern scholars look down and call it funding."
            );
            case "cassia flint" -> List.of(
                    "Personal: I keep the armor bright because rust is too close to forgetting. Highwall soldiers notice both.",
                    "Personal: The red scarf marks the gate I failed to hold. I wear it where a throat guard should be, because some lessons should stay uncomfortable.",
                    "Personal: I do not need comfort. I need clean orders, honest reports, and a road where standing firm helps someone."
            );
            case "lyra bell" -> List.of(
                    "Personal: White cloth shows blood quickly. That is not pretty. That is useful.",
                    "Personal: Belltower taught me every warning has a pitch. Sickness, sabotage, fear; they all ring differently if you listen.",
                    "Personal: I dress like a healer because people run toward healers. I learned to carry enough steel for what follows them."
            );
            case "samir dawn" -> List.of(
                    "Personal: Gold on white looks obedient from a distance. Up close, every sunburst is a question I was told not to ask.",
                    "Personal: Sanctum raises lanterns high so nobody notices which corners stay dark.",
                    "Personal: If I speak softly, it is not submission. Dawn does not shout either."
            );
            case "aria foxglove" -> List.of(
                    "Personal: Green hides well in Oakhaven until the leaves betray you by being prettier.",
                    "Personal: The bow is honest. The harmless look is practical. Roads are kinder when danger underestimates you first.",
                    "Personal: Foxglove is medicine, poison, and a flower. I try to be all three on schedule."
            );
            case "vesper snowroot" -> List.of(
                    "Personal: White hair makes people assume winter claimed me. The green says it failed to finish.",
                    "Personal: Snowrest buries roads, seeds, and inconvenient family truths. Roots remember the shape of all three.",
                    "Personal: My staff is not for drama. Branches know where the ground is lying."
            );
            case "rafiq glass" -> List.of(
                    "Personal: The purple sash was a wager, then a warning, then an apology. Desert fashion is efficient like that.",
                    "Personal: Dunewick taught me to smile before duels and count water after them. I am improving the order.",
                    "Personal: I look expensive because cheap-looking travelers are searched more roughly."
            );
            case "calder reed" -> List.of(
                    "Personal: Mud on boots is honest. Mud in foundations is a negotiation you are already losing.",
                    "Personal: Mireford folk dress for work because the marsh respects neither ceremony nor clean trousers.",
                    "Personal: This hammer has fixed doors, bridges, carts, and one noble's opinion of himself."
            );
            case "marla" -> List.of(
                    "Personal: I keep a list of patients who lived because someone came back with muddy boots and useful herbs.",
                    "Personal: Riverside calls me stern. Fever calls me worse."
            );
            case "archivist ren" -> List.of(
                    "Personal: I trust ink more than memory, but less than scars.",
                    "Personal: If I join you, I am bringing notebooks. Heroics without citations are just shouting."
            );
            case "scribe pela" -> List.of(
                    "Personal: I sharpen charcoal before knives. A copied name can keep a ghost quieter than steel.",
                    "Personal: The Archive smells of dust, wax, and people pretending not to be afraid."
            );
            case "captain torin" -> List.of(
                    "Personal: Command is mostly counting who did not come back and still giving the next order.",
                    "Personal: Highwall taught me to sleep in armor. I do not recommend it, but it works."
            );
            case "signal keeper lysa" -> List.of(
                    "Personal: Smoke has grammar. Raiders are learning it badly, which makes them dangerous.",
                    "Personal: My worst fear is a clear sky on the wrong day."
            );
            case "scout rook" -> List.of(
                    "Personal: I name every ambush site after what I wish I had eaten instead.",
                    "Personal: A scout's pride is coming back with boring news. I miss pride."
            );
            case "trapmaster yaro" -> List.of(
                    "Personal: I respect a tidy snare. I hate finding one with my shin.",
                    "Personal: Give me wire, patience, and bad intentions, and I can make a road nervous."
            );
            case "bellkeeper ilya" -> List.of(
                    "Personal: Bells do not lie. People hear whatever guilt teaches them.",
                    "Personal: When Belltower goes quiet, even the gulls look over their shoulders."
            );
            case "net-mender corso" -> List.of(
                    "Personal: I can mend rope in the dark. I prefer not to discover what else is in the dark.",
                    "Personal: Spider silk pays well, if it is not still attached to opinions."
            );
            case "warden sol" -> List.of(
                    "Personal: Wards are promises carved deeply enough that stone feels obliged.",
                    "Personal: Sanctum survives because someone always stays awake under the floor."
            );
            case "brother cal" -> List.of(
                    "Personal: I pray politely and carry a mallet for the things that answer impolitely.",
                    "Personal: A charm that hums is comforting. A charm that stops suddenly is paperwork."
            );
            case "quartermaster vesh" -> List.of(
                    "Personal: I can forgive panic. I cannot forgive an empty inventory ledger.",
                    "Personal: If you return with fewer supplies than wounds, I will have questions."
            );
            case "eira" -> List.of(
                    "Personal: I build cairns because the snow tries to make every road a rumor.",
                    "Personal: Wolves respect warmth, hunger, and nothing else."
            );
            case "ash watcher kera" -> List.of(
                    "Personal: I listen to ashes. They are less dramatic than prophets and usually more accurate.",
                    "Personal: If a cold hearth laughs, leave first and investigate second."
            );
            case "edda" -> List.of(
                    "Personal: I have fed cowards, heroes, and people smart enough to be both.",
                    "Personal: Oakhaven survives because nobody here lets an empty bowl stay empty."
            );
            case "hedgewise lin" -> List.of(
                    "Personal: Hedges gossip through roots. Most of it is weather, some of it is murder.",
                    "Personal: I used to prune roses. Now I negotiate with brambles carrying grudges."
            );
            case "farmer joss" -> List.of(
                    "Personal: Horses complain honestly. People could learn from them.",
                    "Personal: If the watch mounts are hungry, everyone suddenly discovers how far walking is."
            );
            case "rowan" -> List.of(
                    "Personal: Straw should stay where you leave it. That is the foundation of civilization.",
                    "Personal: I am trying very hard not to lose an argument with a scarecrow."
            );
            case "mira" -> List.of(
                    "Personal: I tie blue cord on my crates because raiders remember color worse than guilt.",
                    "Personal: A merchant who cannot track losses is just a bard with heavier bags."
            );
            case "bran" -> List.of(
                    "Personal: I charge for my shield because people value what costs them.",
                    "Personal: Roadwatch discipline is simple: stand up, stay awake, do not romanticize dying."
            );
            case "liora" -> List.of(
                    "Personal: I lecture after I stitch. People listen better when their blood is staying put.",
                    "Personal: Tea, bandages, and blunt honesty have saved more camps than speeches."
            );
            case "rowan wildspeaker" -> List.of(
                    "Personal: The wilds do not obey me. We have an understanding and mutual doubts.",
                    "Personal: A friendly hedge is still a hedge. Ask before leaning."
            );
            case "niva" -> List.of(
                    "Personal: Snow writes everything down. My work is reading before the wind edits.",
                    "Personal: Warm hands are holy in Snowrest, whatever the priests say."
            );
            case "garruk ironwall" -> List.of(
                    "Personal: A shield is a door you bring to an argument.",
                    "Personal: I have been called stubborn by avalanches. I consider that professional respect."
            );
            case "elowen" -> List.of(
                    "Personal: Frost remembers spring. Wounds remember gentleness too, given time.",
                    "Personal: I carry seeds because even battlefields eventually need a better idea."
            );
            case "cairnwatch asta" -> List.of(
                    "Personal: My job is proving the dead were here before snow says otherwise.",
                    "Personal: A cairn is just stones until someone depends on it."
            );
            case "pass guide olin" -> List.of(
                    "Personal: I have cursed the same snowdrift for six winters. We are practically kin.",
                    "Personal: The pass does not hate travelers. It simply forgets we are fragile."
            );
            case "sela" -> List.of(
                    "Personal: Shade is a currency in Dunewick. I spend mine carefully.",
                    "Personal: A well that lies is still useful if you know what it wants."
            );
            case "rain-seer imani" -> List.of(
                    "Personal: I inherited one rainstorm, three arguments about it, and a very dry sense of humor.",
                    "Personal: In Dunewick, hope sounds like a bucket hitting water."
            );
            case "kael" -> List.of(
                    "Personal: Two blades are not showing off. Showing off is surviving with one.",
                    "Personal: I like clean exits, sharp steel, and employers who can count."
            );
            case "nyx" -> List.of(
                    "Personal: If I wanted to be found, I would wear brighter boots.",
                    "Personal: Secrets are lighter than armor and usually block more damage."
            );
            case "orin stonebreaker" -> List.of(
                    "Personal: The trick to a hammer is knowing when the world is asking to be simpler.",
                    "Personal: Stone argues slowly. I answer quickly."
            );
            case "toma" -> List.of(
                    "Personal: Ledgers are just confessions with columns.",
                    "Personal: Raiders keeping books is either progress or doom wearing spectacles."
            );
            case "fen" -> List.of(
                    "Personal: The marsh answers questions eventually. The price is usually socks.",
                    "Personal: Boglight is useful because it shows you what wishes it had stayed hidden."
            );
            case "mira sunwarden" -> List.of(
                    "Personal: A lantern is not brave. It simply makes bravery easier to locate.",
                    "Personal: Darkness is less frightening when you give it edges."
            );
            case "vexa" -> List.of(
                    "Personal: Thorns are honest. They announce the terms and keep them.",
                    "Personal: I cultivate plants that make enemies reconsider shortcuts."
            );
            case "sable" -> List.of(
                    "Personal: I dislike noise, loose buckles, and people who call murder messy.",
                    "Personal: Locks are conversations. Most surrender to patience or embarrassment."
            );
            case "old noll" -> List.of(
                    "Personal: A fence is a wall with humility. Raiders never understand humility.",
                    "Personal: I have trusted mud more than men and been disappointed less often."
            );
            default -> List.of();
        };
    }

    private static String rumorDialog(String biomeContext, Random random) {
        List<String> pool = switch (biomeContext) {
            case "mountain" -> List.of(
                    "Rumor: A mule came down from the pass alone with blue candle wax on its saddle. Someone sent a beast home because they could not send themselves.",
                    "Rumor: Lanterns moved inside a whiteout, all in a perfect line. Either the lost have found discipline, or something is marching with borrowed light."
            );
            case "desert" -> List.of(
                    "Rumor: A dune near the old ruins rings like a bell when the moon is thin. Bells call people, and I want to know who that one thinks it owns.",
                    "Rumor: Dunewick children trade stories about rain the way city children trade sweets. Hope becomes currency when water does."
            );
            case "marsh" -> List.of(
                    "Rumor: The reeds near the black pools have started repeating names in the wrong voices. Names are doors; something there is testing handles.",
                    "Rumor: A fisher pulled up a boot full of clean, dry ash. In a marsh, dryness is not comfort. It is evidence."
            );
            case "forest" -> List.of(
                    "Rumor: Oakhaven's oldest tree dropped green leaves during frost. When an elder breaks season, the young start wondering what law failed.",
                    "Rumor: There is a deer on the north road that watches campfires until they go out. I dislike witnesses that never blink."
            );
            case "grave" -> List.of(
                    "Rumor: One grave bell rang underground for a full minute, then apologized in a child's voice. That is not haunting; that is a message with manners.",
                    "Rumor: The Archive locked a map away because it kept adding fresh graves. A map that predicts grief is either precious or guilty."
            );
            default -> List.of(
                    "Rumor: A trader paid double for broken charms and refused to say why. Broken protections are still protections to someone who knows what broke them.",
                    "Rumor: Travelers keep finding the same black feather in different inns. Either a bird is impossible, or a warning has learned to travel."
            );
        };
        return pick(pool, random);
    }

    private static String personalFollowDialog(Npc npc, Random random) {
        List<String> pool = npcSpecificDialog(npc);
        if (pool.size() > 1) {
            return pool.get(random.nextInt(pool.size()));
        }
        return "Personal: " + npc.name() + " studies you for a moment, deciding how much truth the road has earned today.";
    }

    private static String professionFollowDialog(Npc npc, Random random) {
        String profession = npcProfession(npc);
        List<String> pool = switch (profession) {
            case "bartender" -> List.of(
                    "Advice: If three strangers lower their voices at once, refill their cups and remember their boots. Quiet people often leave loud consequences.",
                    "Advice: Good rumors arrive thirsty. Bad rumors arrive already paid for, and paid stories usually want someone blamed."
            );
            case "blacksmith" -> List.of(
                    "Advice: Oil your blade after marsh work, warm it slowly after snow, and never trust desert grit near a hinge. Care is cheaper than regret.",
                    "Advice: If armor pinches in the shop, it will betray you on the road. Small discomforts become large decisions under fear."
            );
            case "baker" -> List.of(
                    "Advice: Eat before a hard road. Empty stomachs make cowards out of sensible people, and cowards make fast promises.",
                    "Advice: A village with flour left still believes in tomorrow. Guard the flour if you want tomorrow to believe back."
            );
            case "merchant" -> List.of(
                    "Advice: Count payment in the shade, promises in daylight, and enemies twice. Numbers reveal what pride edits out.",
                    "Advice: The road tax nobody names is fear. Every caravan pays it, and every safe arrival argues the rate down."
            );
            case "archivist" -> List.of(
                    "Advice: Read inscriptions from the bottom up if the stone is cursed. Old wards love dramatic openings and careless readers.",
                    "Advice: Never trust a clean ruin. Someone cleaned it for a reason, and history is rarely scrubbed for kindness."
            );
            case "guard" -> List.of(
                    "Advice: Keep your back to stone, your fire low, and your exit boring. Heroic exits are for people who failed to plan.",
                    "Advice: The first sound in an ambush is usually the least honest one. Listen for what the trap is trying to hide."
            );
            case "healer" -> List.of(
                    "Advice: Wash bites, warm frost, cool burns, and do not let proud people sleep with a fever. Pride is a poor nurse.",
                    "Advice: The patient who says they are fine is either brave, foolish, or bleeding internally. Treat the silence too."
            );
            case "farmer" -> List.of(
                    "Advice: Watch animals before weather. They complain early and without politics, which makes them better witnesses than most councils.",
                    "Advice: If weeds all lean away from a path, pick another path. Even roots know when to refuse."
            );
            case "scout" -> List.of(
                    "Advice: When tracks vanish, look up. When birds vanish, leave. The sky often notices an ambush before the road admits it.",
                    "Advice: A trail that looks too easy has been prepared by someone who dislikes you. Convenience is sometimes bait with manners."
            );
            case "quartermaster" -> List.of(
                    "Advice: Spare rope, dry socks, clean water. Everything else is a debate, and debates get quiet when thirst arrives.",
                    "Advice: Never split supplies evenly. Split them by need, route, and who is most likely to panic."
            );
            default -> List.of(
                    "Advice: Ask two locals the same question. If both answer quickly, worry; fear rehearses better than truth.",
                    "Advice: Keep one hand free and one promise unpaid. The first saves your body, the second saves your choices."
            );
        };
        return pick(pool, random);
    }

    private static String rumorFollowDialog(String biomeContext, Random random) {
        List<String> pool = switch (biomeContext) {
            case "mountain" -> List.of(
                    "Rumor: They heard it from a courier who would not remove their mittens indoors. Some people keep gloves on because their hands have told the story already.",
                    "Rumor: Snow muffles lies poorly. Mountain rumors arrive half true because the false half freezes first."
            );
            case "desert" -> List.of(
                    "Rumor: A well-keeper told it first, and well-keepers do not waste breath in the heat. If they spent words, count them.",
                    "Rumor: In the dunes, a story surviving until morning is already suspicious. Heat kills weak lies and weak travelers."
            );
            case "marsh" -> List.of(
                    "Rumor: It came from reedcutters, which means it is either exact truth or a joke with boots. Either way, someone stood close enough to risk sinking.",
                    "Rumor: Marsh stories grow in the telling, but the roots are usually real. Pull gently or the whole bank comes with it."
            );
            case "forest" -> List.of(
                    "Rumor: Oakhaven children heard it from the old road, and children are better listeners than adults admit. They have not learned which truths to ignore.",
                    "Rumor: The forest changes details, not endings. If the ending stays the same, respect it."
            );
            default -> List.of(
                    "Rumor: It passed through three towns and lost only one corpse in the retelling. That kind of consistency deserves attention.",
                    "Rumor: I do not believe all of it. That is why I believe part of it; lies usually grow around a hard seed."
            );
        };
        return pick(pool, random);
    }

    private static String biomeFollowDialog(String biomeContext, Random random) {
        List<String> pool = switch (biomeContext) {
            case "mountain" -> List.of(
                    "Local: Watch for snow that falls sideways but leaves no drift. Something large has passed through weather and made it lie.",
                    "Local: Keep metal wrapped at night. Cold makes honest tools spiteful, and spiteful tools make cowards of careful hands."
            );
            case "desert" -> List.of(
                    "Local: If you see rain in the desert, check whether anything else can see it too. Miracles with witnesses become politics quickly.",
                    "Local: The hottest hours belong to lizards, spirits, and fools. Travel around all three unless you mean to join one of them."
            );
            case "marsh" -> List.of(
                    "Local: Step on roots, not shine. Shiny mud has plans, and plans in the marsh usually begin with your ankles.",
                    "Local: If your reflection moves late, stop looking and back away. Some places learn your shape before they ask permission."
            );
            case "forest" -> List.of(
                    "Local: Do not answer voices from the trees unless they use your name correctly twice. The forest knows many sounds and fewer loyalties.",
                    "Local: Moss grows thickest where people stopped hurrying. Sometimes that means peace. Sometimes it means they never left."
            );
            case "badlands" -> List.of(
                    "Local: Red dust hides tracks until sunset, then gives them back in gold light. The land delays testimony, but it does testify.",
                    "Local: Caves out there breathe warm before storms. Useful warnings are not obliged to be pleasant."
            );
            case "grave" -> List.of(
                    "Local: Bring charcoal, salt, and manners near old stones. The dead forgive ignorance less often than stories claim.",
                    "Local: Never count graves aloud unless you know whether the number changed. Arithmetic is rude in a restless place."
            );
            default -> List.of(
                    "Local: Watch what locals avoid stepping on. They paid for that knowledge before you arrived.",
                    "Local: Every place has a warning sound. Survive long enough to learn this one, then respect it before it has to repeat itself."
            );
        };
        return pick(pool, random);
    }

    private static String biomeDialog(String biomeContext, Random random) {
        List<String> pool = switch (biomeContext) {
            case "mountain" -> List.of(
                    "Local: The snow never really stops up here. It covers roads, then memories, then excuses, unless someone keeps digging.",
                    "Local: Mountain folk complain about snow because complaining is how they count losses without naming them.",
                    "Local: If the wind drops suddenly in the pass, people stop talking. Silence that arrives all at once is usually carrying orders."
            );
            case "desert" -> List.of(
                    "Local: My grandmother said she saw rain on the dunes once. People still argue whether she meant water, mercy, or a mirage that wanted followers.",
                    "Local: Desert stories begin with a well, a shadow, or someone trusting the wrong shimmer. Scarcity makes every choice look holy.",
                    "Local: You can hear heat here. It ticks in roof beams and shortens tempers until truth comes out cracked but useful."
            );
            case "marsh" -> List.of(
                    "Local: The marsh keeps every secret twice: once under water, once under fog. Secrets kept that carefully are rarely harmless.",
                    "Local: Mire paths move after heavy rain. People call it mud because blaming the land is easier than admitting the map is afraid.",
                    "Local: If the frogs go quiet, check your boots, your purse, and your courage. One of the three is already being tested."
            );
            case "forest" -> List.of(
                    "Local: The trees are friendly by daylight. At dusk they repeat sounds they should not know, as if practicing how to call us back.",
                    "Local: Forest people judge weather by leaf backs, birdsong, and whether the old roots ache. Roots ache before villages admit danger.",
                    "Local: Oakhaven calls this green country. The green country calls us temporary, which keeps our manners sharp."
            );
            case "badlands" -> List.of(
                    "Local: Red dust gets into bread, bandages, letters, prayers. After a week every message tastes like the road it survived.",
                    "Local: Badlands wind polishes bones and secrets with equal patience. It leaves only what was stubborn enough to remain.",
                    "Local: There are places out there where your echo comes back tired. Even sound spends itself crossing that ground."
            );
            case "water" -> List.of(
                    "Local: River people can tell depth by color, current by smell, and bad weather by how quiet the gulls get. Survival begins as attention.",
                    "Local: Water roads remember every mistake. They do not judge, which is worse; they simply keep the body of the lesson.",
                    "Local: If the river looks still, it is either deep, cold, or thinking. Respect all three."
            );
            case "grave" -> List.of(
                    "Local: Around the old graves, people lower their voices even when no one taught them to. Reverence sometimes survives where memory failed.",
                    "Local: Graveyard weather feels colder because names keep shade. Every name is a small roof over a finished life.",
                    "Local: The old stones lean like listeners. I try not to give them fresh gossip; the dead already know too much."
            );
            default -> List.of(
                    "Local: This place has its own weather, its own warnings, and its own way of deciding whether travelers are paying attention.",
                    "Local: Every road in Alderfall has a favorite lie. Learning it is how you survive; challenging it is how you change anything.",
                    "Local: People here read the sky before they read letters because the sky has killed more fools than bad grammar ever did."
            );
        };
        return pick(pool, random);
    }

    private static String npcProfession(Npc npc) {
        String text = (npc.name() + " " + npc.sprite() + " " + (npc.shopId() == null ? "" : npc.shopId())).toLowerCase();
        if (text.contains("bartender") || text.contains("tavern") || text.contains("innkeeper")) {
            return "bartender";
        }
        if (text.contains("trapmaster") || text.contains("guide") || text.contains("rain-seer") || text.contains("scout") || text.contains("rook") || text.contains("rowan") || text.contains("fen")) {
            return "scout";
        }
        if (text.contains("blacksmith") || text.contains("forge")) {
            return "blacksmith";
        }
        if (text.contains("baker") || text.contains("edda")) {
            return "baker";
        }
        if (text.contains("merchant") || text.contains("market") || text.contains("sela") || text.contains("mira")) {
            return "merchant";
        }
        if (text.contains("archivist") || text.contains("scribe") || text.contains("ren") || text.contains("pela")) {
            return "archivist";
        }
        if (text.contains("captain") || text.contains("warden") || text.contains("watcher") || text.contains("guard") || text.contains("torin") || text.contains("bran")) {
            return "guard";
        }
        if (text.contains("medic") || text.contains("healer") || text.contains("marla") || text.contains("brother") || text.contains("eira") || text.contains("niva")) {
            return "healer";
        }
        if (text.contains("farmer") || text.contains("joss") || text.contains("hedgewise") || text.contains("reedcutter") || text.contains("net-mender")) {
            return "farmer";
        }
        if (text.contains("quartermaster")) {
            return "quartermaster";
        }
        return "citizen";
    }

    private static String npcDialogKey(Npc npc) {
        return npc.name().strip().toLowerCase();
    }

    private static String settlementMapFromHouse(String mapId) {
        int last = mapId.lastIndexOf('_');
        if (last <= "house_".length()) {
            return mapId;
        }
        int previous = mapId.lastIndexOf('_', last - 1);
        if (previous <= "house_".length()) {
            return mapId;
        }
        return mapId.substring("house_".length(), previous);
    }

    private static String biomeContextForTile(char tile) {
        return switch (tile) {
            case 'n', 'q', 'm' -> "mountain";
            case 's' -> "desert";
            case 'v' -> "marsh";
            case 'f', 'g' -> "forest";
            case 'b' -> "badlands";
            case 'w' -> "water";
            case 'd' -> "grave";
            default -> "road";
        };
    }

    public static final class DialogueSession {
        private final Map<String, DialogueNode> nodes;
        private final Map<String, Integer> nodeVisits = new LinkedHashMap<>();
        private String currentNodeId = "root";
        private DialogueIntent previousIntent;
        private String lastRepeatKey = "";
        private final Quest quest;
        private final String questSnapshot;

        private DialogueSession(Map<String, DialogueNode> nodes) {
            this(nodes, null);
        }

        private DialogueSession(Map<String, DialogueNode> nodes, Quest quest) {
            this.nodes = nodes;
            this.quest = quest;
            this.questSnapshot = snapshot(quest);
        }

        private static String snapshot(Quest q) {
            return q == null ? "" : q.id + ":" + q.activeStage().id() + ":" + q.accepted + ":" + q.completed + ":" + q.progress
                    + ":" + q.cargo + ":" + q.observedStages;
        }

        public boolean matches(Quest current) {
            return quest == current && questSnapshot.equals(snapshot(current));
        }

        public String line() {
            return line(0);
        }

        public String line(int relationship) {
            String line = currentNode().line();
            if (line.contains("{{relationship}}")) {
                line = line.replace("{{relationship}}", relationshipLabel(relationship) + " (" + relationship + ")");
            }
            return line;
        }

        public DialogueLine structuredLine(int relationship) {
            return DialogueLine.from(line(relationship));
        }

        public List<String> optionLabels() {
            return currentNode().choices().stream()
                    .map(DialogueChoice::label)
                    .toList();
        }

        public List<DialogueChoicePreview> optionPreviews() {
            return currentNode().choices().stream()
                    .map(choice -> new DialogueChoicePreview(choice.label(), choice.relationshipDelta(), choice.effect()))
                    .toList();
        }

        public DialogueChoiceResult choose(int index) {
            if (!questSnapshot.equals(snapshot(quest))) return new DialogueChoiceResult(0);
            List<DialogueChoice> choices = currentNode().choices();
            if (index < 0 || index >= choices.size()) {
                return new DialogueChoiceResult(0);
            }
            DialogueChoice choice = choices.get(index);
            String nextNodeId = choice.nextNodeId();
            if (!nodes.containsKey(nextNodeId)) {
                return new DialogueChoiceResult(0);
            }
            DialogueNode current = currentNode();
            previousIntent = current.intent();
            currentNodeId = nextNodeId;
            DialogueNode next = currentNode();
            nodeVisits.merge(next.id(), 1, Integer::sum);
            lastRepeatKey = next.repeatKey();
            return new DialogueChoiceResult(choice.relationshipDelta(), choice.effect(), next.repeatKey());
        }

        public boolean hasOptions() {
            return !currentNode().choices().isEmpty();
        }

        public boolean atRoot() {
            return "root".equals(currentNodeId);
        }

        public String lastRepeatKey() {
            return lastRepeatKey;
        }

        private DialogueNode currentNode() {
            return nodes.getOrDefault(currentNodeId, nodes.get("root"));
        }

    }

    private record DialogueNode(String id, String line, List<DialogueChoice> choices, DialogueIntent intent, String repeatKey) {
    }

    public record DialogueChoiceResult(int relationshipDelta, String effect, String repeatKey) {
        public DialogueChoiceResult(int relationshipDelta) {
            this(relationshipDelta, "", "");
        }
    }

    public record DialogueChoicePreview(String label, int relationshipDelta, String effect) {
        public DialogueChoicePreview {
            label = label == null ? "" : label.strip();
            effect = effect == null ? "" : effect.strip();
        }
    }

    public record DialogueLine(String narration, String speech) {
        public DialogueLine {
            narration = narration == null ? "" : narration.replaceAll("\\s+", " ").strip();
            speech = speech == null ? "" : speech.replaceAll("\\s+", " ").strip();
        }

        public static DialogueLine from(String rawLine) {
            String cleaned = rawLine == null ? "" : rawLine.replaceAll("\\s+", " ").strip();
            if (cleaned.isBlank()) {
                return new DialogueLine("", "");
            }
            int firstQuote = cleaned.indexOf('"');
            int lastQuote = cleaned.lastIndexOf('"');
            if (firstQuote >= 0 && lastQuote > firstQuote) {
                String narration = cleaned.substring(0, firstQuote).strip();
                String speech = cleaned.substring(firstQuote + 1, lastQuote).strip();
                String trailing = cleaned.substring(lastQuote + 1).strip();
                if (!trailing.isBlank()) {
                    speech = speech.isBlank() ? trailing : speech + " " + trailing;
                }
                return new DialogueLine(narration, speech);
            }
            return new DialogueLine("", cleaned);
        }

        public String combined() {
            if (narration.isBlank()) {
                return speech;
            }
            if (speech.isBlank()) {
                return narration;
            }
            return narration + " \"" + speech + "\"";
        }
    }

    public record Personality(String name, String description) {
    }

    private record StoryBranch(String opening, List<StoryOption> options) {
    }

    private record StoryOption(String playerLine, String npcResponse, int relationshipDelta, String effect) {
    }

    private record CompanionOutcomeTopic(String outcomeKey, String label) {
    }

    private static String milestoneRootPrompt(int threshold) {
        return switch (threshold) {
            case 50 -> "Can we talk about where we stand?";
            case 100 -> "Can we talk about what changed between us?";
            case 150 -> "Can we talk about staying together by choice?";
            case 180 -> "Can we talk about what this feeling is becoming?";
            case 250 -> "Can we talk about the future?";
            default -> "Can we talk about us?";
        };
    }

    private record CompanionChoiceOption(
            String label,
            String outcome,
            int relationshipDelta,
            DialogueIntent intent,
            String response,
            boolean supportiveFollowup
    ) {
        private String followup(CompanionVoice voice, int stage, Quest quest) {
            return supportiveFollowup
                    ? voice.supportResponse(stage, quest)
                    : voice.challengeResponse(stage, quest);
        }
    }

    private record DialogueChoice(String label, String nextNodeId, int relationshipDelta, String effect) {
        private DialogueChoice(String label, String nextNodeId) {
            this(label, nextNodeId, 0, "");
        }

        private DialogueChoice(String label, String nextNodeId, int relationshipDelta) {
            this(label, nextNodeId, relationshipDelta, "");
        }
    }

    public record DialogueContext(
            boolean recruited,
            boolean recruitmentAvailable,
            boolean stationedAtOathstead,
            boolean romanced,
            boolean married,
            String assignedBuildingLabel,
            String workerRoleLabel,
            List<String> knownCompanionNames,
            List<String> recentMemoryTexts,
            boolean sharedTableConversation,
            String sharedTableLabel,
            String approvalMotive,
            boolean epilogueReady,
            String activeSoftRequestText,
            boolean loyaltyMilestoneResolved,
            boolean romanceMilestoneResolved,
            boolean futureMilestoneResolved,
            int pendingMilestoneTrust,
            String pendingMilestoneLabel,
            Map<String, Integer> dialogueTopicCounts,
            Map<String, String> questBranchOutcomes
    ) {
        public DialogueContext {
            assignedBuildingLabel = assignedBuildingLabel == null ? "" : assignedBuildingLabel.strip();
            workerRoleLabel = workerRoleLabel == null ? "" : workerRoleLabel.strip();
            knownCompanionNames = knownCompanionNames == null ? List.of() : List.copyOf(knownCompanionNames);
            recentMemoryTexts = recentMemoryTexts == null ? List.of() : List.copyOf(recentMemoryTexts);
            sharedTableLabel = sharedTableLabel == null ? "" : sharedTableLabel.strip();
            approvalMotive = approvalMotive == null || approvalMotive.isBlank() ? "trust" : approvalMotive.strip();
            activeSoftRequestText = activeSoftRequestText == null ? "" : activeSoftRequestText.strip();
            pendingMilestoneTrust = Math.max(0, pendingMilestoneTrust);
            pendingMilestoneLabel = pendingMilestoneLabel == null ? "" : pendingMilestoneLabel.strip();
            dialogueTopicCounts = dialogueTopicCounts == null ? Map.of() : Map.copyOf(dialogueTopicCounts);
            questBranchOutcomes = questBranchOutcomes == null ? Map.of() : Map.copyOf(questBranchOutcomes);
        }

        public static DialogueContext empty() {
            return new DialogueContext(false, false, false, false, false, "", "", List.of(), List.of(), false, "", "trust", false, "", false, false, false, 0, "", Map.of(), Map.of());
        }

        public int topicVisitCount(String topicKey) {
            if (topicKey == null || topicKey.isBlank()) {
                return 0;
            }
            return Math.max(0, dialogueTopicCounts.getOrDefault(topicKey, 0));
        }

        public String questOutcome(String outcomeKey) {
            if (outcomeKey == null || outcomeKey.isBlank()) {
                return "";
            }
            return questBranchOutcomes.getOrDefault(outcomeKey, "");
        }

        public boolean hasQuestOutcome(String outcomeKey, String expectedOutcome) {
            if (expectedOutcome == null || expectedOutcome.isBlank()) {
                return false;
            }
            return expectedOutcome.equals(questOutcome(outcomeKey));
        }

        public String questOutcomeLabel(String outcomeKey) {
            String outcome = questOutcome(outcomeKey);
            if (outcome.isBlank()) {
                return "undecided";
            }
            String[] parts = outcome.replace('-', '_').split("_+");
            List<String> words = new ArrayList<>();
            for (String part : parts) {
                if (part.isBlank()) {
                    continue;
                }
                words.add(part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1).toLowerCase(Locale.ROOT));
            }
            return words.isEmpty() ? outcome : String.join(" ", words);
        }
    }
}
