package com.alderfall.game;

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
        DialogueContext dialogueContext = context == null ? DialogueContext.empty() : context;
        Map<String, DialogueNode> nodes = new LinkedHashMap<>();
        String greeting = greetingQuote(npc, random);
        Personality personality = personalityFor(npc);

        List<DialogueChoice> rootChoices = new ArrayList<>();
        StoryBranch storyBranch = storyBranch(npc, quest);
        CompanionVoice companionVoice = companionVoice(npc);
        if (storyBranch != null) {
            greeting = storyBranch.opening();
            addStoryBranchChoices(nodes, rootChoices, "story_branch", storyBranch, quest);
        } else if (companionVoice != null) {
            greeting = companionGreeting(companionVoice, quest, relationship, dialogueContext);
            List<DialogueChoice> personalChoices = new ArrayList<>();
            List<DialogueChoice> homeChoices = new ArrayList<>();
            addCompanionMilestoneTree(nodes, rootChoices, companionVoice, relationship, dialogueContext);
            if (quest != null && quest.companionQuest()) {
                addCompanionQuestTree(nodes, rootChoices, companionVoice, quest, relationship, dialogueContext);
            }
            addCompanionRecruitmentTree(nodes, personalChoices, companionVoice, relationship, dialogueContext);
            addCompanionRelationshipTree(nodes, personalChoices, companionVoice, relationship, dialogueContext);
            addCompanionRomanceTree(nodes, personalChoices, companionVoice, relationship, dialogueContext);
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

        if (companionVoice == null) {
            String personal = pick(npcSpecificDialog(npc), random);
            if (!personal.isBlank()) {
                rootChoices.add(new DialogueChoice("Tell me about yourself.", "personal"));
                addNode(nodes, "personal", personal, List.of(
                        new DialogueChoice("I am listening.", "personal_more", 2),
                        new DialogueChoice("There is more, isn't there?", "personal_push", -2),
                        new DialogueChoice("Back to topics", "root")
                ));
                addNode(nodes, "personal_more", personalFollowDialog(npc, random), backChoices());
                addNode(nodes, "personal_push", npc.name() + " closes part of the story behind their eyes. Some doors do not open to force.", backChoices());
            }

            rootChoices.add(new DialogueChoice("What matters most to you?", "personality"));
            addNode(nodes, "personality",
                    personality.name() + ": " + personality.description() + " Relationship: {{relationship}}.",
                    List.of(
                            new DialogueChoice("I respect that.", "personality_warm", 1),
                            new DialogueChoice("I am not sure I agree.", "personality_challenge", -1),
                            new DialogueChoice("Back to topics", "root")
                    ));
            addNode(nodes, "personality_warm", npc.name() + " seems to appreciate being understood instead of merely questioned.", backChoices());
            addNode(nodes, "personality_challenge", npc.name() + " takes the challenge in, but their expression cools.", backChoices());

            rootChoices.add(new DialogueChoice("What work do you do here?", "work"));
            String questLine = questDialog(quest, false);
            if (!questLine.isBlank()) {
                addNode(nodes, "work", professionDialog(npc, random), List.of(
                        new DialogueChoice("What work needs doing?", "quest", 0),
                        new DialogueChoice("Your craft deserves respect.", "work_more", 1),
                        new DialogueChoice("Why do it that way?", "work_doubt", -1),
                        new DialogueChoice("Back to topics", "root")
                ));
                addQuestOfferNodes(nodes, npc, quest, "quest");
            } else {
                addNode(nodes, "work", professionDialog(npc, random), List.of(
                        new DialogueChoice("Your craft deserves respect.", "work_more", 1),
                        new DialogueChoice("Why do it that way?", "work_doubt", -1),
                        new DialogueChoice("Back to topics", "root")
                ));
                rootChoices.add(new DialogueChoice("What have people been saying?", "rumor"));
                addNode(nodes, "rumor", rumorDialog(biomeContext, random), List.of(
                        new DialogueChoice("Go on.", "rumor_more", 1),
                        new DialogueChoice("That sounds like nonsense.", "rumor_doubt", -1),
                        new DialogueChoice("Back to topics", "root")
                ));
                addNode(nodes, "rumor_more", rumorFollowDialog(biomeContext, random), backChoices());
                addNode(nodes, "rumor_doubt", npc.name() + " shrugs, but the next story will not be offered so freely.", backChoices());
            }
            addNode(nodes, "work_more", professionFollowDialog(npc, random), backChoices());
            addNode(nodes, "work_doubt", npc.name() + " gives a thinner answer. Professional pride bruises quietly.", backChoices());
        }

        rootChoices.add(new DialogueChoice("What should I know about this area?", "local"));
        addNode(nodes, "local", biomeDialog(biomeContext, random), List.of(
                new DialogueChoice("Thank you for the warning.", "local_more", 1),
                new DialogueChoice("I can handle the road.", "local_dismiss", -1),
                new DialogueChoice("Back to topics", "root")
        ));
        addNode(nodes, "local_more", biomeFollowDialog(biomeContext, random), backChoices());
        addNode(nodes, "local_dismiss", "Local: " + npc.name() + " lets the warning drop. The land will make its own argument.", backChoices());

        addNode(nodes, "root", "\"" + greeting + "\"", rootChoices);
        return new DialogueSession(nodes);
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

    private static StoryBranch storyBranch(Npc npc, Quest quest) {
        String key = npcDialogKey(npc);
        String questId = quest == null ? "" : quest.id;
        if ("maelis".equals(key) && "ms_wake_ashes".equals(questId)) {
            return branch("You are awake. That is inconvenient for death, and useful for me. Can you stand?",
                    option("I saw her. The Demon Queen. She let me live.",
                            "Do not call that mercy. Mercy leaves a person whole. Whatever she did to you, it was meant to travel. Tell me where it happened. Then we look for proof.", 2),
                    option("I do not know what happened. There was fire, a voice, then nothing.",
                            "That sounds honest enough to be useless. Good. Useless truth is still better than polished lies. We start with the road shrine.", 1),
                    option("I do not need help.",
                            "You collapsed in our mud and bled on our blankets. You needed help before you had an opinion. Stand if you like. Fall if you must.", -1),
                    option("I was hoping to sleep longer. Maybe through the end of the world.",
                            "If the world ends, I will wake you for cleanup. Come on. The broken ward-stone is waiting, and unlike you, it cannot complain.", 1));
        }
        if ("maelis".equals(key) && "ms_road_dust".equals(questId)) {
            return branch("The ash clings, the ward is broken from the inside, and that banner carried more than one kingdom's colors. This was not a raid.",
                    option("Then we warn everyone. Now.",
                            "And say what? A half-dead traveler saw a queen from old nightmares? Panic is easy to start and hard to feed. We need records.", 0),
                    option("We need proof strong enough for nobles, priests, and soldiers.",
                            "There you are. Thinking like someone who wants to be believed, not just heard. Archive City keeps the old lies. With luck, it kept the truth too.", 2),
                    option("What if no one listens?",
                            "Then we make them listen by being annoyingly correct. You keep moving. I will keep Oathstead standing.", 1),
                    option("She should have killed me.",
                            "No. That thought is hers, not yours. Do not carry it for her. She spared you to make fear useful. We will make you useful instead.", 2));
        }
        if ("maelis".equals(key) && "ms_twelve_stones_gate".equals(questId)) {
            return branch("Twelve stones. Five kingdoms. One road into the Queen's own dark. This is the part where someone usually makes a speech.",
                    option("I do not have a speech. I have people who kept their promises.",
                            "Good. Speeches travel poorly in bad weather. That answer will do.", 2),
                    option("She spared me to spread fear. I brought her Alderfall instead.",
                            "Nearly a speech. Not a bad one. Just remember to breathe after saying it.", 2),
                    option("We go in, stop her, and come back. Alderfall still needs roads repaired.",
                            "Now that is a plan with maintenance after. Calder would approve, and so do I.", 1),
                    option("I am afraid.",
                            "Good. Fear that tells the truth is not weakness. Hold the line anyway.", 2));
        }
        if ("selene".equals(key) && "ms_names_dust".equals(questId)) {
            return branch("You are the survivor from Oathstead. Half the city calls you a warning. The other half calls you a rumor. Which should I file you under?",
                    option("File me under evidence. I brought Demon Ash and ward fragments.",
                            "Evidence. Good. The only language that survives politics with most of its bones intact. Show me what you carried out of that road.", 2),
                    option("I think your records are lying.",
                            "Records do not lie. People lie, then records are forced to dress the wound. You may be more useful than polite.", 1),
                    option("The Crownlands hid the truth, did they not?",
                            "Careful. Accusations are arrows. Loose too many and you forget which ones matter. Inspect first. Conclusions after dust.", -1),
                    option("I do not know what I am looking for. I only know people will die if I stop.",
                            "An honest ignorance. Rare in Archive City. Come. The missing shelves will tell you more than the full ones.", 2));
        }
        if ("selene".equals(key) && "ms_first_socket".equals(questId)) {
            return branch("Twelve socket stones. Five kingdoms. One portal no crown was allowed to own. The old histories did not forget this. They buried it.",
                    option("Why hide the only way to reach her?",
                            "Because a weapon shared by five kingdoms is also a reason for five kingdoms to betray one another.", 1),
                    option("People are dying because your archives protected royal shame.",
                            "Yes. They are. And I will not insult them by pretending ink is innocent.", 2),
                    option("Then we collect the stones before anyone else does.",
                            "Practical. Frighteningly so. You are beginning to sound like history under pressure.", 1),
                    option("How do I know you are not still hiding something?",
                            "You do not. That is the correct answer. Trust should never be blind in a room full of edited history.", 2));
        }
        if ("odrick".equals(key) && "ms_watchtower_bells".equals(questId)) {
            return branch("I hear you saw a demon queen. I have soldiers who saw wolves become dragons after two cups of frostwine. Why are you different?",
                    option("Because your watchtower went silent, and I can help find out why.",
                            "Good. You brought a problem I can point a patrol at. Check the tower. Bring facts, not fear.", 2),
                    option("She is coming for every wall you command.",
                            "Everything is always coming for the wall. The wall remains unimpressed. Prove this threat has boots on my road.", 0),
                    option("You will believe me when your gates burn.",
                            "Speak like that again and you can warn the snowdrifts instead. You want my attention? Earn it at the watchtower.", -2),
                    option("I do not need belief. I need a chance to show you evidence.",
                            "That is nearly soldier talk. Nearly. Take the north road and find out what killed the tower.", 1));
        }
        if ("odrick".equals(key) && "ms_frosthollow_standard".equals(questId)) {
            return branch("Kharvok carries a banner stitched from surrendered flags. That is not raider work. That is war.",
                    option("Then Vaelthara is organizing the borders before she strikes the cities.",
                            "Correct. Fear on the roads, pressure at the walls, hunger in the towns. She understands campaigns.", 2),
                    option("He made dead soldiers march.",
                            "There are few things I hate more than poor treatment of the dead. Poor treatment of soldiers is one of them.", 1),
                    option("He will fall like anything else.",
                            "Careful. Victories get people killed when they start sounding easy.", -1),
                    option("I am tired of carrying proof made of corpses.",
                            "That means you are still fit to carry it. The ones who enjoy proof like this become monsters or officers.", 2));
        }
        if ("solari".equals(key) && "ms_shrine_shadow".equals(questId)) {
            return branch("You carry demon ash and road fear into a city of shrines. Tell me, survivor: do you seek light, or permission to use it?",
                    option("I seek a way to stop Vaelthara.",
                            "Stopping is not the same as understanding. But it is a beginning with clean edges.", 1),
                    option("I do not know what I seek. I only know darkness is spreading.",
                            "Darkness is not the enemy. False light is. Learn that, and you may survive Sanctum wisdom.", 2),
                    option("I need your rites. Teach me.",
                            "Need is not worthiness. Thirst does not make every cup clean.", -1),
                    option("Can sunlight really matter against a demon queen?",
                            "Sunlight matters against rot, lies, fever, and kings. Why should demons be special?", 1));
        }
        if ("solari".equals(key) && "ms_ember_socket_rite".equals(questId)) {
            return branch("Ember remembers shape. Glass remembers heat. Ash remembers what ended. What do you remember, survivor?",
                    option("I remember being beaten and left alive.",
                            "Then let the stone hold what you survived, not what broke you.", 2),
                    option("I remember Oathstead. People sharing food they barely had.",
                            "Good. Fire kept only for itself becomes hunger. Fire shared becomes a hearth.", 2),
                    option("I remember her face. I want to see it fall.",
                            "Revenge burns hot and stupid. Useful for starting flame. Poor for carrying it.", -1),
                    option("I try not to remember.",
                            "Then the ash remembers for you. Be careful what memories you leave unattended.", 0));
        }
        if ("ysra".equals(key) && "ms_bell_alone".equals(questId)) {
            return branch("The bells spoke of you before you arrived. That is either important or rude. I have not decided.",
                    option("I came because something is wrong in the marsh.",
                            "Something is always wrong in the marsh. The question is whether it is wrong in a new voice.", 1),
                    option("What did the bells say?",
                            "Not words. Bells are kinder than people. They warn without pretending to explain.", 2),
                    option("They are just bells.",
                            "And roads are just dirt until you are lost. Try not to speak proudly about what you do not understand.", -2),
                    option("Give me the place. I will inspect it.",
                            "Good. Mud respects feet more than speeches.", 1));
        }
        if ("ysra".equals(key) && "ms_miredepth_below".equals(questId)) {
            return branch("Miredepth does not kill quickly. It convinces you to step lower until the sky becomes a rumor.",
                    option("I have survived worse than mud.",
                            "Mud has killed better people than pride. But confidence floats, briefly.", -1),
                    option("What should I listen for?",
                            "Bells that ring underwater. Voices that sound dry. Your own name spoken from below.", 2),
                    option("Velmora was a bellkeeper once, was she not?",
                            "Yes. Remember that when she tries to drown you. Pity is useful only if it keeps its boots tied.", 2),
                    option("If she guards the stone, she dies.",
                            "Likely. But the marsh remembers how you say necessary things.", -1));
        }
        if ("mirella".equals(key) && "ms_toll_ledger".equals(questId)) {
            return branch("You arrive with ash, rumors, and urgency. Riverside already has all three. Why should yours be placed on my desk?",
                    option("Because if trade fails, your river barons will tear each other apart before Vaelthara arrives.",
                            "Unpleasantly accurate. I prefer guests who flatter first, but accuracy has its uses.", 2),
                    option("People are being hurt while nobles argue over tolls.",
                            "People are always hurt while nobles argue. The trick is making the nobles notice before the bodies block the road.", 1),
                    option("I do not have time for politics.",
                            "Then you do not have time for kingdoms. They are mostly politics with walls.", -1),
                    option("Give me a task. If I solve it, you give me what you know.",
                            "At last, a language the river understands.", 2));
        }
        if ("mirella".equals(key) && "ms_glowing_mud".equals(questId)) {
            return branch("A socket stone in goblin hands. That means old battlefields are being picked clean by desperate creatures and cleverer masters.",
                    option("Vaelthara's agents are letting scavengers move pieces for them.",
                            "Yes. Cheap hands, deniable losses, no letters to intercept. Hideous and efficient.", 2),
                    option("Then the poor are being used as tools again.",
                            "That is the oldest policy in every kingdom. Demons did not invent it. They merely admire it.", 1),
                    option("We kill the goblins and take it.",
                            "Do not mistake a simple step for a simple problem.", -1),
                    option("Tell me where the mud trail ends.",
                            "Practical. The trail ends where greed stops walking and starts digging.", 1));
        }
        if ("elder rowan".equals(key) && "ms_orchard_ward".equals(questId)) {
            return branch("The orchard ward was here before my grandmother had teeth. Now the roots are black and learning bad manners.",
                    option("What hurt the orchard?",
                            "Demon ash, most likely. Roots remember where blood fell, and this soil has started remembering too loudly.", 1),
                    option("Tell me how to help without making it worse.",
                            "Good. City folk poke old wards like soup. Inspect the trees first. Fight only what cannot be soothed.", 2),
                    option("If the stag is corrupted, I will put it down.",
                            "Necessary may be necessary, but do not make it sound clean.", -1),
                    option("Old village rites still matter.",
                            "Aye. Laughable things matter right up until they stop keeping us alive.", 2));
        }
        if ("gravekeeper hollis".equals(key) && "ms_names_cold_stone".equals(questId)) {
            return branch("Some fool has been cutting names from the old graves. Now the dead are walking around confused and offended.",
                    option("Names matter. Tell me which stones to check.",
                            "Good. Scratch a name from stone, and you do not erase a person. You untie them.", 2),
                    option("Can the dead be calmed?",
                            "Sometimes. If the living stop making their grief someone else's problem.", 1),
                    option("Undead are undead. I will clear them.",
                            "Clear them if you must. But do not call them the problem when someone made them this way.", -1),
                    option("I do not know what to say near graves.",
                            "That is often best. Silence has better manners than most mourners.", 1));
        }
        if ("captain elric snowrest".equals(key) && "ms_cold_road".equals(questId)) {
            return branch("I have three sick children, two missing sleds, and one road full of wolves. Put your prophecy in line.",
                    option("What do you need first?",
                            "Firewood, medicine crates, and the road quiet enough to move both. Good answer.", 2),
                    option("The Demon Queen threat is bigger than one road.",
                            "Fever does not care. Neither does snow. Big threats still need small roads cleared.", -1),
                    option("I can clear the wolves.",
                            "Then start there. Useful work earns better hearing than urgent speech.", 1),
                    option("You sound exhausted.",
                            "I am. Notice later. Work now.", 1));
        }
        if ("bellwright nessa".equals(key) && "ms_missing_bell_rope".equals(questId)) {
            return branch("The village bell stopped ringing because no one listened when the rope started fraying. That is how most disasters begin.",
                    option("Show me where the repair starts.",
                            "At the winch. Then the hooks. Then the foundation. Magic loves people who maintain things.", 2),
                    option("A bell rope can hide a socket stone?",
                            "A foundation can hide anything if enough generations forget to ask why it was built.", 1),
                    option("This feels small for a demon war.",
                            "Bad rope kills more travelers than bad monsters. Start learning scale properly.", -1),
                    option("You trust craft more than prophecy.",
                            "Craft answers when pulled correctly. Prophecy mostly hums and waits to be misunderstood.", 2));
        }
        if ("ash-scribe damar".equals(key) && "ms_blackvault_mark".equals(questId)) {
            return branch("Blackvault has been dead for a century. Yesterday, it started making new shadows.",
                    option("What did the ash tell you?",
                            "That the mark was burned from the other side of the stone. Ash is honest about what endured.", 2),
                    option("I will inspect the wards before drawing steel.",
                            "Good. A survivor who reads first may remain a survivor longer.", 1),
                    option("If Sareth wants witnesses gone, he can try me.",
                            "He will. The Cinder Knife dislikes loose witnesses and confident ones most of all.", 1),
                    option("Ruins are ruins. Dead places make noise.",
                            "I do not fear ruins. I fear ruins that begin keeping appointments.", -1));
        }
        return null;
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
        if (context.stationedAtOathstead()) {
            return voice.oathsteadGreeting(context);
        }
        if (quest != null && quest.companionQuest() && !quest.completed) {
            return voice.questGreeting(quest);
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

    private static void addCompanionQuestTree(
            Map<String, DialogueNode> nodes,
            List<DialogueChoice> rootChoices,
            CompanionVoice voice,
            Quest quest,
            int relationship,
            DialogueContext context
    ) {
        int stage = companionQuestStage(quest);
        QuestDialogueStage questStage = questDialogueStage(quest);
        boolean hasRememberedOutcome = !context.questOutcome(quest.outcomeKey()).isBlank();
        rootChoices.add(new DialogueChoice(companionQuestRootLabel(quest), "companion_quest"));
        if (questStage == QuestDialogueStage.OFFER) {
            addNode(nodes, "companion_quest", voice.reply(DialogueIntent.QUEST_ROOT, relationship, context, voice.questOpening(quest)), List.of(
                    new DialogueChoice("Tell me what is really happening.", "companion_quest_clarify", 1),
                    new DialogueChoice(voice.personalQuestion(), "companion_quest_personal", 1),
                    new DialogueChoice(practicalQuestLabel(quest), "companion_quest_practical", 1),
                    new DialogueChoice(questCommitLabel(quest), "companion_quest_accept", 2, questAcceptEffect(quest)),
                    new DialogueChoice("Give me the short version.", "companion_quest_push", -1),
                    backToCompanion(voice)
            ));
        } else if (questStage == QuestDialogueStage.READY) {
            addNode(nodes, "companion_quest", voice.reply(DialogueIntent.QUEST_ROOT, relationship, context, voice.questOpening(quest)), List.of(
                    new DialogueChoice("Here is what I found.", "companion_quest_turnin", 2, questTurnInEffect(quest)),
                    new DialogueChoice("What does this prove?", "companion_quest_clarify", 1),
                    new DialogueChoice("What does it cost you to hear this?", "companion_quest_personal", 1),
                    new DialogueChoice("What happens after this?", "companion_quest_warning", 1),
                    backToCompanion(voice)
            ));
        } else if (questStage == QuestDialogueStage.COMPLETED) {
            List<DialogueChoice> choices = new ArrayList<>();
            if (hasRememberedOutcome) {
                choices.add(new DialogueChoice("Can we talk about the choice we made?", "companion_quest_outcome", 2));
            }
            choices.addAll(List.of(
                    new DialogueChoice("What changed after this?", "companion_quest_clarify", 1),
                    new DialogueChoice("Was it worth the cost?", "companion_quest_personal", 1),
                    new DialogueChoice("What remains unfinished?", "companion_quest_warning", 1),
                    backToCompanion(voice)
            ));
            addNode(nodes, "companion_quest", voice.reply(DialogueIntent.QUEST_ROOT, relationship, context, voice.questOpening(quest)), choices);
        } else {
            String progressLabel = questStage == QuestDialogueStage.IN_PROGRESS ? "Let me make sure I understand." : "Remind me why this matters.";
            List<DialogueChoice> choices = new ArrayList<>();
            if (quest.activeObjectiveKind() == Quest.ObjectiveKind.CHOICE && !quest.ready()) {
                choices.add(new DialogueChoice("The honest answer comes first.", "companion_quest_choice_truth", 2, questOutcomeEffect(quest, "truth")));
                choices.add(new DialogueChoice("Mercy comes first.", "companion_quest_choice_mercy", 2, questOutcomeEffect(quest, "mercy")));
                choices.add(new DialogueChoice("There has to be accountability.", "companion_quest_choice_accountability", 1, questOutcomeEffect(quest, "accountability")));
            }
            if (hasRememberedOutcome) {
                choices.add(new DialogueChoice("Can we talk about the choice we made?", "companion_quest_outcome", 2));
            }
            choices.addAll(List.of(
                    new DialogueChoice(progressLabel, "companion_quest_clarify", 1),
                    new DialogueChoice("Where should I go next?", "companion_quest_practical", 1),
                    new DialogueChoice(voice.personalQuestion(), "companion_quest_personal", 1),
                    new DialogueChoice("What are you afraid will happen?", "companion_quest_warning", 1),
                    backToCompanion(voice)
            ));
            addNode(nodes, "companion_quest", voice.reply(DialogueIntent.QUEST_ROOT, relationship, context, voice.questOpening(quest)), choices);
        }
        addNode(nodes, "companion_quest_clarify", voice.reply(DialogueIntent.QUEST_CLARIFY, relationship, context, voice.stageClarify(stage, quest)), List.of(
                new DialogueChoice("And what does that cost you?", "companion_quest_personal", 1),
                new DialogueChoice(practicalQuestLabel(quest), "companion_quest_practical", 1),
                questStage == QuestDialogueStage.OFFER
                        ? new DialogueChoice(questCommitLabel(quest), "companion_quest_accept", 2, questAcceptEffect(quest))
                        : new DialogueChoice("I will keep the thread straight.", "companion_quest_support", 2),
                backToQuest(quest)
        ));
        addNode(nodes, "companion_quest_personal", voice.reply(DialogueIntent.QUEST_PERSONAL, relationship, context, voice.stagePersonal(stage, quest)), List.of(
                questStage == QuestDialogueStage.OFFER
                        ? new DialogueChoice("I will carry that carefully.", "companion_quest_accept", 2, questAcceptEffect(quest))
                        : new DialogueChoice("I will carry that carefully.", "companion_quest_support", 2),
                new DialogueChoice("Tell me only what helps the work.", "companion_quest_practical", 0),
                new DialogueChoice("That sounds like an excuse.", "companion_quest_challenge", -2),
                backToQuest(quest)
        ));
        addNode(nodes, "companion_quest_practical", voice.reply(DialogueIntent.QUEST_PRACTICAL, relationship, context, voice.stagePractical(stage, quest)), List.of(
                questStage == QuestDialogueStage.OFFER
                        ? new DialogueChoice("I will bring proof, not guesses.", "companion_quest_accept", 2, questAcceptEffect(quest))
                        : new DialogueChoice("I know where to go.", "companion_quest_support", 1),
                new DialogueChoice("What should I watch for?", "companion_quest_warning", 1),
                backToQuest(quest)
        ));
        addNode(nodes, "companion_quest_push", voice.reply(DialogueIntent.QUEST_CHALLENGE, relationship, context, voice.impatientResponse()), List.of(
                new DialogueChoice("You are right. Tell me the why.", "companion_quest_personal", 1),
                new DialogueChoice("Then give me the clean task.", "companion_quest_practical", 0),
                backToQuest(quest)
        ));
        addNode(nodes, "companion_quest_support", voice.reply(DialogueIntent.QUEST_SUPPORT, relationship, context, voice.supportResponse(stage, quest)), companionBackChoices(voice));
        addNode(nodes, "companion_quest_accept", voice.reply(DialogueIntent.QUEST_SUPPORT, relationship, context, voice.supportResponse(stage, quest)), companionBackChoices(voice));
        addNode(nodes, "companion_quest_turnin", voice.reply(DialogueIntent.QUEST_SUPPORT, relationship, context, voice.readyLine(stage)), companionBackChoices(voice));
        addNode(nodes, "companion_quest_choice_truth", voice.reply(DialogueIntent.QUEST_SUPPORT, relationship, context,
                "You choose truth, even where it will bruise. " + voice.supportResponse(stage, quest)), companionBackChoices(voice));
        addNode(nodes, "companion_quest_choice_mercy", voice.reply(DialogueIntent.QUEST_SUPPORT, relationship, context,
                "You choose mercy, but not forgetfulness. " + voice.supportResponse(stage, quest)), companionBackChoices(voice));
        addNode(nodes, "companion_quest_choice_accountability", voice.reply(DialogueIntent.QUEST_CHALLENGE, relationship, context,
                "You choose accountability before comfort. " + voice.challengeResponse(stage, quest)), companionBackChoices(voice));
        if (hasRememberedOutcome) {
            addNode(nodes, "companion_quest_outcome", voice.reply(DialogueIntent.MEMORY, relationship, context,
                    companionQuestOutcomeLine(voice, quest, context)), companionBackChoices(voice));
        }
        addNode(nodes, "companion_quest_challenge", voice.reply(DialogueIntent.QUEST_CHALLENGE, relationship, context, voice.challengeResponse(stage, quest)), companionBackChoices(voice));
        addNode(nodes, "companion_quest_warning", voice.reply(DialogueIntent.QUEST_WARNING, relationship, context, voice.warningResponse(stage, quest)), companionBackChoices(voice));
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

    private static String companionQuestOutcomeLine(CompanionVoice voice, Quest quest, DialogueContext context) {
        String label = context.questOutcomeLabel(quest.outcomeKey());
        return voice.shortName() + " remembers that you chose " + label
                + ". The work does not become smaller because it is remembered; it becomes harder to lie about.";
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
                    new DialogueChoice("Stand with me.", "companion_trust_stand", 2),
                    new DialogueChoice("You are free to choose your road.", "companion_trust_choose", 2),
                    new DialogueChoice("I am glad you are here.", "companion_trust_warm", 1),
                    new DialogueChoice("Good. I need your skills.", "companion_trust_cold", -1)
            ));
        } else if (relationship >= 120) {
            addNode(nodes, rootId, voice.reply(DialogueIntent.TRUST, relationship, context, voice.trustedOpening()), List.of(
                    new DialogueChoice("Tell me the full truth.", "companion_trust_truth", 2),
                    new DialogueChoice("I will carry this carefully.", "companion_trust_care", 2),
                    new DialogueChoice("You trust me that much?", "companion_trust_ask", 1),
                    new DialogueChoice("Would someone else be safer?", "companion_trust_doubt", -1)
            ));
        } else if (relationship >= 90) {
            addNode(nodes, rootId, voice.reply(DialogueIntent.TRUST, relationship, context, voice.friendOpening()), List.of(
                    new DialogueChoice("I am listening.", "companion_trust_listen", 2),
                    new DialogueChoice("Take your time.", "companion_trust_patient", 2),
                    new DialogueChoice("Can it help us?", "companion_trust_mission", 1),
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
                    new DialogueChoice("I will take useful for now.", "companion_trust_useful", 1),
                    new DialogueChoice("I would rather earn trust.", "companion_trust_earn", 2),
                    new DialogueChoice("You keep your distance.", "companion_trust_distance", 0),
                    new DialogueChoice("I do not need your trust.", "companion_trust_cold", -1)
            ));
        } else {
            addNode(nodes, rootId, voice.reply(DialogueIntent.TRUST, relationship, context, voice.guardedOpening()), List.of(
                    new DialogueChoice("Then teach me how to help.", "companion_trust_teach", 2),
                    new DialogueChoice("I am listening.", "companion_trust_listen", 1),
                    new DialogueChoice("I survive well enough.", "companion_trust_neutral", 0),
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
        rootChoices.add(new DialogueChoice("Can we talk about " + context.pendingMilestoneLabel() + "?", "companion_milestone"));
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
            return voice.shortName() + " lets the conversation turn toward home, work, and the rare quiet between roads.";
        }
        if (context.sharedTableConversation()) {
            return voice.shortName() + " settles into the moment as if the road has briefly stopped asking for blood.";
        }
        if (relationship >= 120) {
            return voice.shortName() + " speaks of Oathstead like a place that may yet learn how to hold people gently.";
        }
        return voice.shortName() + " considers Oathstead with cautious interest, as if trust were a map still being drawn.";
    }

    private static String personalHubLine(CompanionVoice voice, int relationship, DialogueContext context) {
        if (context.married()) {
            return voice.shortName() + " gives you the kind of attention that has learned your silences.";
        }
        if (context.romanced() || relationship >= 180) {
            return voice.shortName() + " lets the pause settle between you, careful and warm.";
        }
        if (relationship >= 120) {
            return voice.shortName() + " waits without rushing you. Trust has made the quiet less defensive.";
        }
        if (relationship >= 50) {
            return voice.shortName() + " studies your face, less guarded than before but not careless.";
        }
        return voice.shortName() + " keeps an honest distance, but does not turn away.";
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
                addOutcomeTopic(topics, context, "lyra_names_on_cot", "Names on the cot");
                addOutcomeTopic(topics, context, "lyra_false_medicine", "False medicine");
                addOutcomeTopic(topics, context, "lyra_antidote_rule", "The antidote rule");
                addOutcomeTopic(topics, context, "lyra_first_patient", "The first patient");
                addOutcomeTopic(topics, context, "lyra_moving_care", "Moving care");
                addOutcomeTopic(topics, context, "lyra_fever_cure", "The fever cure");
                addOutcomeTopic(topics, context, "lyra_oathstead_clinic", "Oathstead clinic");
            }
            case ARIA -> {
                addOutcomeTopic(topics, context, "aria_ambush_lesson", "The ambush lesson");
                addOutcomeTopic(topics, context, "aria_mother_truth", "Her mother's trail");
                addOutcomeTopic(topics, context, "aria_crossing_truth", "Rootmaw Crossing");
                addOutcomeTopic(topics, context, "aria_oathstead_future", "The road ahead");
            }
            case VESPER -> {
                addOutcomeTopic(topics, context, "vesper_first_root", "The waking root");
                addOutcomeTopic(topics, context, "vesper_practical_care", "Firewood and feverroot");
                addOutcomeTopic(topics, context, "vesper_family_grove", "The family grove");
                addOutcomeTopic(topics, context, "vesper_buried_spring", "The buried spring");
                addOutcomeTopic(topics, context, "vesper_spring_return", "How spring returned");
                addOutcomeTopic(topics, context, "vesper_oathstead_growth", "The Oathstead bloom");
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
            return "What should I look for?";
        }
        return switch (quest.activeObjectiveKind()) {
            case DEFEAT -> "What are we fighting?";
            case RESCUE -> "Who needs rescuing?";
            case DEFEND -> "What are we defending?";
            case GATHER -> "What should I gather?";
            case DELIVER -> "What should I deliver?";
            case VISIT -> "What should I inspect?";
            case SEARCH -> "What am I searching for?";
            case TALK -> "Who should I talk to?";
            case ASK_AROUND -> "Who should I ask?";
            case REPORT -> "Who needs the report?";
            case ESCORT -> "Where are we going?";
            case CHOICE -> "What choice are you asking of me?";
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
        return quest.companionQuest() ? "What does this mean to you?" : "Tell me about " + quest.title + ".";
    }

    public static String questCommitLabel(Quest quest) {
        CompanionVoice voice = companionVoiceForQuest(quest);
        if (voice != null) {
            return voice.helpLine();
        }
        return quest != null && quest.companionQuest() ? "How can I help?" : "I will handle it";
    }

    private static String questDoubtLabel(Quest quest) {
        CompanionVoice voice = companionVoiceForQuest(quest);
        if (voice != null) {
            return voice.personalQuestion();
        }
        return quest != null && quest.companionQuest() ? "Why does this matter to you?" : "Why does this need doing?";
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

    private enum DialogueIntent {
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
        SERAPHINE("Seraphine", "a stolen contract", "What are you really stealing back?",
                "Before you do anything heroic, know this: the drawer was already open when I found it. Probably.",
                "What are you stealing?",
                "Technically? My own name. Less technically, a paper trail that says I belong to someone who forgot I learned knives before etiquette.",
                "I spent years burning contracts that called themselves promises. If you ask for one now, make it ours and make it honest.",
                "No ownership. No chains. No clever clauses hidden under candle wax. Yes, I will marry you.",
                "I think about you when you are gone. It is inconvenient, obvious, and apparently terminal."),
        MAERA("Maera", "forbidden maps", "Why is this map forbidden?",
                "There are three laws against copying this map and one against admitting it exists. Naturally, I brought charcoal.",
                "What did your mother hide from you?",
                "A route, a warning, and the shape of a lie old enough to have royal punctuation. I need the missing edge before it gets someone killed.",
                "My whole life was footnotes and locked shelves. You made a road through them. I would like to keep walking it with you.",
                "Yes. But our vows will include at least one clause about never hiding maps from each other.",
                "I have indexed every reason this is reckless. None of them made me want you less."),
        CASSIA("Cassia", "old orders", "What happened at the gate?",
                "The gate should have held. It did not. I have been arguing with that fact longer than is dignified.",
                "Whose order broke you?",
                "Mine, partly. Someone gave the command, but I obeyed it. That is the part honor keeps returning to me.",
                "I used to believe vows were walls. With you, they feel more like a hand at my back.",
                "Yes. I will stand beside you by choice, not command.",
                "I do not say this lightly: when the line breaks, I look for you first."),
        LYRA("Lyra", "mercy work", "Who needs help at the clinic?",
                "The clinic is out of clean cloth, good sleep, and patient gods. Bring any two and I will improvise the third.",
                "Who taught you mercy costs this much?",
                "Every fever bed. Every person I could not save. Mercy is not soft. It is work that keeps standing after hope sits down.",
                "I have held too many hands at endings. I want yours at beginnings too.",
                "Yes. No grand cure, no perfect promise. Just us, choosing care every day.",
                "When you leave, I count supplies twice. When you return, I breathe once."),
        SAMIR("Samir", "difficult light", "Why is the reliquary burning?",
                "The reliquary flame is eating its own shadow. That is either a miracle with poor manners or a warning with teeth.",
                "What did the light take from you?",
                "Certainty. Then pride. Then the comfort of thinking faith excuses fear. I am better without some of those things.",
                "I once mistook devotion for surrender. With you, devotion feels awake.",
                "Yes. Let the vow be light enough to carry and strong enough to return to.",
                "I have prayed for guidance. Annoyingly, the answer keeps wearing your face."),
        ARIA("Aria", "a family trail", "What happened to your sister?",
                "I know how to look harmless. The road taught me that harmless people get underestimated.",
                "What happened to your sister?",
                "She left through briars and debt, and every false trail I found afterward was too neat. Someone wanted me chasing the wrong pain.",
                "I have slept under hedges and called it freedom. With you, a door sounds less like a trap.",
                "Yes. But if anyone calls me tamed, I am borrowing your best knife.",
                "I keep pretending this is just another trail. It is not. It is you."),
        VESPER("Vesper", "buried roots", "What is hidden under the snow?",
                "Snow does not hide things. People hide things and then act surprised when spring has questions.",
                "What did your family bury?",
                "Roots, names, and one promise that should have been allowed to die. The ground remembers all of it.",
                "I was raised to survive winters alone. You made me consider the usefulness of a shared fire.",
                "Yes. Quietly, stubbornly, and with enough firewood.",
                "I do not melt for people. I remain. Somehow, that became yours."),
        RAFIQ("Rafiq", "a debt with a blade", "Who wants you dead?",
                "If anyone asks, I am handling a debt. If anyone armed asks, you have never met me.",
                "Who wants you dead?",
                "A patron with silk gloves, a duelist with better cheekbones than judgment, and one ledger I should have burned.",
                "I have run from creditors, duelists, and my own better sense. I am tired. Ask me to stay.",
                "Yes. Magnificently unwise, emotionally ruinous, and yes.",
                "I usually flirt to avoid honesty. This is the part where honesty wins, inconveniently."),
        CALDER("Calder", "a failing bridge", "Why is the bridge failing?",
                "Bridges fail in pieces first. A loose peg, a lazy inspection, a person pretending water is patient.",
                "Who did the bridge take from you?",
                "Someone I should have reached faster. Since then I count ropes, beams, and regrets in the same voice.",
                "I trust stone because it tells you when it cracks. I trust you because you stayed long enough to hear it.",
                "Aye. We build it daily, then. No pretty nonsense. Just a promise with foundations.",
                "Love is a structure. I am terrified by how sound this one feels.");

        private final String shortName;
        private final String concern;
        private final String questTopic;
        private final String firstQuestOpening;
        private final String personalQuestion;
        private final String personalResponse;
        private final String marriageOpening;
        private final String marriageAccept;
        private final String lovingOpening;

        CompanionVoice(String shortName, String concern, String questTopic, String firstQuestOpening,
                       String personalQuestion, String personalResponse, String marriageOpening,
                       String marriageAccept, String lovingOpening) {
            this.shortName = shortName;
            this.concern = concern;
            this.questTopic = questTopic;
            this.firstQuestOpening = firstQuestOpening;
            this.personalQuestion = personalQuestion;
            this.personalResponse = personalResponse;
            this.marriageOpening = marriageOpening;
            this.marriageAccept = marriageAccept;
            this.lovingOpening = lovingOpening;
        }

        String shortName() {
            return shortName;
        }

        String reply(DialogueIntent intent, int relationship, DialogueContext context, String answer) {
            String cleaned = answer == null ? "" : answer.replaceAll("\\s+", " ").strip();
            StringBuilder line = new StringBuilder();
            line.append(conversationBeat(relationship, context)).append(' ');
            line.append('"').append(repeatAcknowledgement(intent, context));
            if (!cleaned.isBlank()) {
                line.append(' ').append(cleaned);
            }
            String thread = relationshipThread(intent, relationship, context);
            if (!thread.isBlank()) {
                line.append(' ').append(thread);
            }
            line.append('"');
            return line.toString();
        }

        private String repeatAcknowledgement(DialogueIntent intent, DialogueContext context) {
            if (intent == null) {
                return "";
            }
            int visits = context.topicVisitCount(topicKey(intent));
            if (visits <= 0) {
                return intentAcknowledgement(intent, 0, context);
            }
            if (visits == 1) {
                return switch (this) {
                    case SERAPHINE -> "Again, then. Good. Repeated questions reveal which clauses still matter.";
                    case MAERA -> "We are returning to the same entry. That usually means the margin was not wide enough.";
                    case CASSIA -> "Back to this. Fine. Some things deserve a second inspection.";
                    case LYRA -> "Still this wound, then. We will look carefully and not pretend it closed.";
                    case SAMIR -> "You return to the question. Perhaps it is still giving off light.";
                    case ARIA -> "Still tracking this? Good. The second pass catches what the first missed.";
                    case VESPER -> "You come back to the same root. That is often where the water is.";
                    case RAFIQ -> "Again? Either you are thorough or I was devastatingly unclear. Let us be generous and say thorough.";
                    case CALDER -> "Back to the same joint. Sensible. Load-bearing things should be checked twice.";
                };
            }
            return switch (this) {
                case SERAPHINE -> "You keep returning to this. I will answer, but I will not perform certainty just because repetition asks nicely.";
                case MAERA -> "Third pass and counting. The record is not changing, but perhaps we are.";
                case CASSIA -> "We have named this before. I can stand here again, but not forever.";
                case LYRA -> "We can reopen it, gently. But even honest care needs breath between dressings.";
                case SAMIR -> "The question remains lit. Let us not stare so long that we mistake brightness for understanding.";
                case ARIA -> "We have circled this trail enough to know it is not random. Ask, then.";
                case VESPER -> "Roots can be tended. They can also be worried loose. Carefully, then.";
                case RAFIQ -> "Again. I am beginning to suspect this question has rented space in both our heads.";
                case CALDER -> "We have checked this brace before. I will check it again, but we should also keep building.";
            };
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

        private String conversationBeat(int relationship, DialogueContext context) {
            String beat = switch (this) {
                case SERAPHINE -> "Seraphine weighs your wording before she lets it pass.";
                case MAERA -> "Maera sorts the thought as if it were a page with dangerous margins.";
                case CASSIA -> "Cassia steadies her shoulders, but her answer is for you rather than the room.";
                case LYRA -> "Lyra studies your face first, gentle as a healer checking an old wound.";
                case SAMIR -> "Samir lets the silence breathe before he answers.";
                case ARIA -> "Aria checks the nearest exit by habit, then gives her attention back to you.";
                case VESPER -> "Vesper grows still, as if listening for the root under the words.";
                case RAFIQ -> "Rafiq almost reaches for a joke, then thinks better of hiding there.";
                case CALDER -> "Calder sets the answer down carefully, like weight on tested timber.";
            };
            return beat + " " + relationshipBeat(relationship, context);
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
            if (relationship >= 90) {
                return "The guard remains, but it no longer fills the whole doorway.";
            }
            if (relationship >= 50) {
                return "They let you see the answer forming before they decide how much to give.";
            }
            if (relationship >= 20 || context.recruited()) {
                return "They answer cautiously, measuring whether concern and usefulness can share a road.";
            }
            return "The answer is careful, offered from just beyond arm's reach.";
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
                case SERAPHINE -> switch (intent) {
                    case VALUES -> "If you want my honest measure of you, I will not hide it under pretty language.";
                    case OPINION -> "Since you brought a name to the table, I will not pretend neutrality is the same as truth.";
                    case REQUEST -> "You asked what I need without asking what it buys you. That changes the answer.";
                    case TRUST -> "Trust, then. No contracts, no ownership, no hidden ink.";
                    case ROMANCE -> "You are asking for the uncontracted part of me. Careful. That part has teeth.";
                    default -> "";
                };
                case MAERA -> switch (intent) {
                    case QUEST_CLARIFY -> "You want the underlying record, not the summary. Sensible.";
                    case VALUES -> "You asked for my assessment. I will make it as honest as a living source permits.";
                    case MEMORY -> "If we are reopening the record, we should do it deliberately.";
                    case FUTURE -> "You are asking me to speculate about after. I have notes, unfortunately.";
                    default -> "";
                };
                case CASSIA -> switch (intent) {
                    case QUEST_PRACTICAL -> "You asked for the practical line. Good. It keeps fear from giving orders.";
                    case QUEST_WARNING -> "You asked what breaks first. That is the soldier's question.";
                    case TRUST -> "Trust is not a speech. It is where I stand when the line moves.";
                    case FEELING -> "You want the answer under the armor. Fine.";
                    default -> "";
                };
                case LYRA -> switch (intent) {
                    case CONCERN -> "You are checking the healer for wounds. That is irritatingly fair.";
                    case NEED -> "You asked what I need before I collapsed into being useful. I heard that.";
                    case FUTURE -> "You are asking about a future where care is allowed to rest. I want that answer too.";
                    case MEMORY -> "If we touch that memory, we do it gently and all the way awake.";
                    default -> "";
                };
                case SAMIR -> switch (intent) {
                    case QUEST_CLARIFY -> "You are asking for the shadow behind the light. That is where truth usually waits.";
                    case VALUES -> "You ask how I see you. I will answer without making faith do the work for me.";
                    case TRUST -> "Trust should not kneel. It should look up and choose.";
                    case ROMANCE -> "You are asking for warmth without worship. That is rarer than it should be.";
                    default -> "";
                };
                case ARIA -> switch (intent) {
                    case CONCERN -> "You are asking whether I am fine. I usually say yes before checking.";
                    case QUEST_PERSONAL -> "You want the part of the trail I usually cover with leaves.";
                    case QUEST_WARNING -> "You asked what can go wrong. Finally, a question with survival instincts.";
                    case HOME -> "You are asking about staying. I know every exit, so the answer matters.";
                    case ROMANCE -> "You are asking me to stay close without calling it a tactic. Suspicious.";
                    default -> "";
                };
                case VESPER -> switch (intent) {
                    case CONCERN -> "You are asking after the root, not just the branch. Few people do.";
                    case QUEST_CLARIFY -> "You want the buried part named before the work begins. Good.";
                    case MEMORY -> "Spoken memory changes the soil. Let us be careful what we plant.";
                    case HOME -> "You ask about home as if it can grow instead of trap. I am listening.";
                    case REQUEST -> "You ask what I need from you. Needs are roots; pulled too roughly, they tear.";
                    default -> "";
                };
                case RAFIQ -> switch (intent) {
                    case CONCERN -> "You are inviting sincerity. Reckless. I will attempt not to ruin it immediately.";
                    case OPINION -> "If you want my opinion, I can make it charming or true. Today I will risk true.";
                    case ROMANCE -> "You are asking for the honest version of my attention. Bold, dangerous, well dressed.";
                    case QUEST_CHALLENGE -> "You are calling the bluff. Excellent. I was getting tired of holding it alone.";
                    default -> "";
                };
                case CALDER -> switch (intent) {
                    case QUEST_PRACTICAL -> "You asked what needs doing. Good. A sound answer starts there.";
                    case NEED -> "You asked what I need. Needs are easier to meet when named before the beam cracks.";
                    case TRUST -> "Trust is load-bearing. Best to inspect it honestly.";
                    case FUTURE -> "You ask about after. That means we plan for weather, not just sunlight.";
                    default -> "";
                };
            };
        }

        private String relationshipThread(DialogueIntent intent, int relationship, DialogueContext context) {
            if (intent == DialogueIntent.QUEST_PRACTICAL || intent == DialogueIntent.QUEST_WARNING) {
                return "";
            }
            if (context.married()) {
                return "I can say that because the promise between us has room for truth, not only comfort.";
            }
            if (context.romanced() && (intent == DialogueIntent.CONCERN || intent == DialogueIntent.FEELING || intent == DialogueIntent.FUTURE)) {
                return "It changes the answer, knowing you are not asking from a distance anymore.";
            }
            if (relationship >= 150 && (intent == DialogueIntent.MEMORY || intent == DialogueIntent.VALUES || intent == DialogueIntent.REQUEST)) {
                return "At this point, pretending not to trust you would be less honest than the fear.";
            }
            if (relationship < 50 && (intent == DialogueIntent.CONCERN || intent == DialogueIntent.FEELING || intent == DialogueIntent.NEED)) {
                return "Do not mistake the answer for full trust yet, but it is an answer.";
            }
            return "";
        }

        String firstQuestOpening() {
            return firstQuestOpening;
        }

        String questGreeting(Quest quest) {
            int stage = companionQuestStage(quest);
            if (quest.ready()) {
                return "You found enough of the truth. Now we decide what it means.";
            }
            if (quest.accepted && quest.progress > 0) {
                return "The thread is moving. Do not let it knot around your wrist.";
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
                return (line.isBlank() ? "That should be enough." : line) + " " + readyLine(companionQuestStage(quest));
            }
            if (quest.progress > 0) {
                line = cleanQuestLine(quest.activeProgressDialog(), shortName);
                return (line.isBlank() ? "Stay with it." : line) + " Progress is proof that the story can still be changed.";
            }
            return questGreeting(quest);
        }

        String helpLine() {
            return "How can I help?";
        }

        String helpResponse() {
            return "By treating this like more than an errand. " + shortName + " watches you a little more steadily. The work is about " + concern + ", and the trust behind it.";
        }

        String personalQuestion() {
            return personalQuestion;
        }

        String personalResponse() {
            return personalResponse;
        }

        String practicalResponse(Quest quest) {
            String target = quest == null ? "the next sign" : quest.activeTarget();
            return "Start with " + target + ". Bring back proof, not guesses. If something looks staged, assume it was staged for someone less careful than you.";
        }

        String stageClarify(int stage, Quest quest) {
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

        String stagePractical(int stage, Quest quest) {
            String target = quest == null ? "the next sign" : quest.activeTarget();
            String action = quest == null ? "Follow" : switch (quest.activeObjectiveKind()) {
                case DEFEAT -> "Break";
                case RESCUE -> "Rescue";
                case DEFEND -> "Defend";
                case GATHER -> "Gather";
                case DELIVER -> "Deliver";
                case VISIT -> "Inspect";
                case SEARCH -> "Search";
                case TALK -> "Speak with";
                case ASK_AROUND -> "Ask around about";
                case REPORT -> "Report to";
                case ESCORT -> "Escort";
                case CHOICE -> "Decide about";
            };
            String where = quest == null ? "where the trail points" : objectiveLocationLine(quest);
            return action + " " + target + " at " + where + ". " + practicalDetail(stage) + " Bring back proof, not guesses.";
        }

        String supportResponse(int stage, Quest quest) {
            return switch (stage) {
                case 5, 6, 7 -> "Good. I do not need rescuing from the truth. I need someone beside me while I stop obeying it.";
                case 8 -> finalHome();
                default -> "That is the right kind of help. Not loud. Not possessive. Present.";
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
            return switch (stage) {
                case 3 -> "Evidence may be planted. Look for what is too neat, too clean, or too eager to be found.";
                case 4, 7 -> bossTactical();
                case 8 -> "Old endings sometimes pretend to be peace. Watch for what still asks to be carried.";
                default -> "The danger is not only the task. It is believing the task is small because the first step is simple.";
            };
        }

        String originLine() {
            return switch (this) {
                case SERAPHINE -> "A contract can become a cage if enough rich people agree to call it law.";
                case MAERA -> "A forbidden map is usually a confession folded into geography.";
                case CASSIA -> "A gate remembers every order that made it open or close.";
                case LYRA -> "A clinic teaches you that mercy without movement becomes furniture.";
                case SAMIR -> "A holy flame can warm a room or teach people to kneel. I am learning the difference.";
                case ARIA -> "A trail can save you, trap you, or lead you back to the person you failed to follow.";
                case VESPER -> "Roots do not forget burial. They decide what to do with it.";
                case RAFIQ -> "A debt with a blade is still a debt, just more honest about the ending.";
                case CALDER -> "A bridge fails long before it falls. People do too.";
            };
        }

        String concernLine() {
            return switch (this) {
                case SERAPHINE -> "Ink is tidy because the cruelty happened before it dried.";
                case MAERA -> "If the map is wrong on purpose, every road built from it is a lie with mile markers.";
                case CASSIA -> "Duty without judgment is just obedience hoping no one counts the dead.";
                case LYRA -> "Supplies are not small. A clean bandage can be the difference between grief and tomorrow.";
                case SAMIR -> "Light that forbids questions has already begun casting the wrong shadow.";
                case ARIA -> "False trails are made by people who know what real grief will chase.";
                case VESPER -> "Winter hides many things, but it does not make them harmless.";
                case RAFIQ -> "Charm can buy time. It cannot pay the debt forever.";
                case CALDER -> "Maintenance is care with a hammer. Neglect is harm with better excuses.";
            };
        }

        String woundLine() {
            return switch (this) {
                case SERAPHINE -> "My family was taught to call survival a debt. I am trying to stop speaking that language.";
                case MAERA -> "My mother hid truth where only stubborn love would find it. I am angry enough to be grateful.";
                case CASSIA -> "I obeyed an order and people paid for it. Context matters. So does blame.";
                case LYRA -> "I waited for permission once. Someone died while obedience kept its hands clean.";
                case SAMIR -> "My family grief was dressed as doctrine until I almost mistook silence for faith.";
                case ARIA -> "I followed the wrong trail because it hurt less than admitting someone had shaped my fear.";
                case VESPER -> "I thought the grove died because I was too young to save it. Grief likes simple lies.";
                case RAFIQ -> "I bought time for someone I loved and let everyone call it vanity. It was easier to wear.";
                case CALDER -> "I signed off on work that failed. The river did the killing, but I gave it a place to start.";
            };
        }

        String practicalDetail(int stage) {
            return switch (stage) {
                case 1 -> "Start where the first lie touches the ground.";
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

        String readyLine(int stage) {
            return switch (stage) {
                case 5, 6 -> "I am ready to hear the part I avoided.";
                case 7 -> "Now the fight has to become an ending.";
                case 8 -> "Now I choose what comes after.";
                default -> "Bring it here. We will make the next choice with eyes open.";
            };
        }

        String afterQuestLine(int stage) {
            return switch (stage) {
                case 1 -> "That was the first honest mark on a very dishonest road.";
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
            return "There you are. I was beginning to think the road had grown selfish again.";
        }

        String romancedGreeting() {
            return "You have terrible timing. Somehow I have grown fond of that.";
        }

        String oathsteadGreeting(DialogueContext context) {
            if (!context.assignedBuildingLabel().isBlank()) {
                return "Oathstead has put me at the " + context.assignedBuildingLabel().toLowerCase() + ". I am making it useful in my own way.";
            }
            return "Oathstead still leans toward standing. That counts for more than it sounds.";
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
                case SAMIR -> "It feels like a lamp someone is allowed to question.";
                case ARIA -> femaleOathsteadHomeLine(context);
                case VESPER -> femaleOathsteadHomeLine(context);
                case RAFIQ -> "It feels muddy, earnest, and dangerously sincere. I am adapting with heroic grace.";
                case CALDER -> "It feels uneven. Repairable. Most worthwhile things are.";
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
                case SERAPHINE -> "Clear agreements. Fair shares. No paper that owns a person.";
                case MAERA -> "A map table, good light, and the courage to correct old routes.";
                case CASSIA -> "A gate that opens for the right people and closes for the right reasons.";
                case LYRA -> "Clean water, spare cloth, and fewer heroes pretending infection respects bravery.";
                case SAMIR -> "A light people can gather around without being ordered to kneel.";
                case ARIA -> "Watchers on the roads, quiet signals, and someone checking the paths children use.";
                case VESPER -> "A garden that grows food before symbols. Symbols can wait their turn.";
                case RAFIQ -> "Better drainage, worse music, and a corner where second chances are not announced too loudly.";
                case CALDER -> "Foundations, braces, nails, and people willing to maintain what they love.";
            };
        }

        private String femaleOathsteadLine(DialogueContext context) {
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
            if (!context.questOutcome("vesper_oathstead_growth").isBlank()) {
                return "Oathstead has seen the snowroot take. I still check it too often, as if hope might vanish if I stop watching, but it keeps growing without permission.";
            }
            if (!context.questOutcome("vesper_spring_return").isBlank()) {
                return "After the buried spring, Oathstead feels like a question with soil in its hands: what do we plant now that we know not every grave is finished?";
            }
            if (context.stationedAtOathstead()) {
                return "I thought a camp would feel temporary. This one keeps asking roots to decide whether temporary is only another word for afraid.";
            }
            return "Oathstead is not polished enough to lie well. I find that almost comforting.";
        }

        private String femaleOathsteadHomeLine(DialogueContext context) {
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
            if (!context.questOutcome("vesper_oathstead_growth").isBlank()) {
                return "It feels like soil that listened before receiving a root. That matters. I can stay near a place that asks instead of taking.";
            }
            if (!context.questOutcome("vesper_family_grove").isBlank()) {
                return "It does not feel like my family grove. Good. I need it to become itself, not a replacement shaped like grief.";
            }
            return "It feels like soil that has not decided what it can grow. I respect that.";
        }

        String checkInOpening(int relationship, DialogueContext context) {
            if (relationship >= 150) {
                return "You ask like you intend to hear the answer. That still catches me off guard.";
            }
            if (relationship >= 50) {
                return "You are making a habit of checking whether I am a person. Dangerous habit.";
            }
            return "If this is concern, it is early. If this is strategy, it is unusually gentle.";
        }

        String feelingLine(int relationship, DialogueContext context) {
            if (relationship >= 150) {
                return switch (this) {
                    case SERAPHINE -> "Less owned by old ink. More annoyed by how much that matters.";
                    case MAERA -> "Overfull. Evidence, fear, fondness. My mental shelves are badly arranged.";
                    case CASSIA -> "Steady enough to stand. Honest enough to admit standing is not the same as healed.";
                    case LYRA -> "Tired in the hands. Better in the heart than I expected.";
                    case SAMIR -> "Bright in places I used to keep locked. Frightening, but not unwelcome.";
                    case ARIA -> "Like I might stay if no one says the word too loudly.";
                    case VESPER -> "Like thaw. Muddy, exposed, necessary.";
                    case RAFIQ -> "Tragically sincere. I am enduring it with style.";
                    case CALDER -> "Sound enough. Some cracks. None spreading today.";
                };
            }
            if (relationship >= 50) {
                return "Tired. Not the sleepy kind. The road kind.";
            }
            return "Aware. Armed. Not answering that fully.";
        }

        String nextStepLine(int relationship, DialogueContext context) {
            if (!context.assignedBuildingLabel().isBlank()) {
                return "Keep Oathstead useful. The " + context.assignedBuildingLabel().toLowerCase()
                        + " is not just a building if people rely on it.";
            }
            return switch (this) {
                case SERAPHINE -> "Follow the paper until it points at someone powerful enough to hate being named.";
                case MAERA -> "Trust the contradiction. That is usually where truth failed to stay buried.";
                case CASSIA -> "Secure the road behind us before chasing glory ahead.";
                case LYRA -> "Supplies first. Heroics after clean water and bandages.";
                case SAMIR -> "Find the light that can survive questions. Leave the obedient flames behind.";
                case ARIA -> "Watch the trails people hope we are too hurried to notice.";
                case VESPER -> "Look for what is still alive under what everyone called dead.";
                case RAFIQ -> "Choose the useful danger over the dramatic one. I am trying this new discipline.";
                case CALDER -> "Check what holds. Roads, gates, promises. Same principle.";
            };
        }

        String needLine(int relationship, DialogueContext context) {
            if (relationship >= 120) {
                return "Patience when I circle the old wound. Honesty when I pretend I am fine. The usual impossible list.";
            }
            if (relationship >= 50) {
                return "Dry socks, clear roads, and fewer assumptions. I will settle for one.";
            }
            return "Space, for now. Useful help later.";
        }

        String feelingRespectResponse() {
            return "Thank you. Not every closed door is a locked one. Some are simply resting on the hinge.";
        }

        String feelingHonestResponse(int relationship) {
            if (relationship >= 120) {
                return "The honest version is that I am better with you near and irritated that I cannot make that sound tactical.";
            }
            return "The honest version is smaller than you want and larger than I planned to share.";
        }

        String feelingMissionResponse() {
            return "We can. Just do not mistake motion for being all right.";
        }

        String nextAcceptResponse() {
            return "Good. Agreement is useful when it still leaves room for judgment.";
        }

        String nextWorryResponse() {
            return "That we mistake urgency for wisdom. That we win the next step and lose the reason for taking it.";
        }

        String nextDisagreeResponse() {
            return "Then make it a plan, not a mood. I can follow a plan. I do not trust moods with maps.";
        }

        String needSupportResponse() {
            return "Then I will try to ask before the need turns into a wall.";
        }

        String needDirectResponse() {
            return "Directly. Terrible idea. Probably healthier. I will consider it.";
        }

        String needRefuseResponse() {
            return "Time is always the first thing people deny each other. I noticed.";
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
                    case SERAPHINE -> "Guarded respect, then. I dislike how legal that sounds, but it fits: limited terms, honestly entered.";
                    case MAERA -> "The honest version is that you have become a source I do not immediately distrust.";
                    case CASSIA -> "I respect that you bend and return. People who never bend usually break other people first.";
                    case LYRA -> "I have begun believing your concern may survive inconvenience. That is rarer than kindness.";
                    case SAMIR -> "You have not demanded faith from me. You have earned attention. That is the first clean step.";
                    case ARIA -> "You watch the road better than most. You also listen when I say the road is lying.";
                    case VESPER -> "You have not trampled every small living thing on the way to large victories. I noticed.";
                    case RAFIQ -> "You are becoming difficult to dismiss. Very rude. Possibly admirable.";
                    case CALDER -> "You hold under load. Not perfectly. Nothing good does at first.";
                };
                case 100 -> switch (this) {
                    case SERAPHINE -> "I still expect kindness to hide terms. With you, I sometimes forget to look for the trap first.";
                    case MAERA -> "I am afraid the truth will cost more than I can pay, and more afraid I will pay it anyway because you are watching.";
                    case CASSIA -> "I have survived by being useful. I do not know who I am when someone stays after the use is done.";
                    case LYRA -> "I know how to hold everyone together. I do not know how to let someone hold me without counting it as failure.";
                    case SAMIR -> "I have preached light while fearing what it would show in me. You make hiding feel less holy.";
                    case ARIA -> "I learned to leave before anyone could choose it for me. Staying near you is making that habit clumsy.";
                    case VESPER -> "I blamed myself for every root that did not bloom. You make blame feel less like truth.";
                    case RAFIQ -> "I made charm out of panic. It worked so well I forgot where I put the honest man.";
                    case CALDER -> "I trust structures because people failed me. You are making that distinction inconvenient.";
                };
                case 150 -> switch (this) {
                    case SERAPHINE -> "If I stay, it is not because you own the clause I signed. It is because you keep leaving the door open and I keep choosing not to use it.";
                    case MAERA -> "I have corrected kings in margins and feared my own name in ink. With you, loyalty feels like citing the truth aloud.";
                    case CASSIA -> "I was trained to hold lines for people who never learned my face. This time I know exactly whose shoulder I am guarding.";
                    case LYRA -> "I have spent my life staying because people hurt. I stay with you because care can be more than triage.";
                    case SAMIR -> "I used to think loyalty meant kneeling to the brightest thing in the room. Now I think it means walking beside a light that asks questions.";
                    case ARIA -> "I know every exit from here. That is why it matters that I am still standing close enough for you to reach me.";
                    case VESPER -> "Roots do not swear loyalty. They hold, feed, and return after winter. That is the kind I can offer.";
                    case RAFIQ -> "I have escaped finer rooms than this feeling. Somehow I am still here, making the alarming choice to be reliable.";
                    case CALDER -> "A bridge proves itself under weight. So do people. You have weight, and I am still here.";
                };
                case 180 -> switch (this) {
                    case SERAPHINE -> "I want you without turning wanting into ownership. That is the frightening part: no contract, no cage, just choice.";
                    case MAERA -> "I keep trying to file this under alliance, gratitude, shared evidence. The index refuses. It keeps writing your name.";
                    case CASSIA -> "I do not have a soldier's word for wanting to be looked for after the battle. I only know I want it from you.";
                    case LYRA -> "When you are hurt, my hands know what to do. When you smile at me, I lose the whole medical tradition.";
                    case SAMIR -> "I have prayed for clean answers. This is not clean. It is warm, difficult, and more honest than certainty.";
                    case ARIA -> "I want to stay near you when there is no tactical reason. Do you understand how suspicious that is?";
                    case VESPER -> "I do not bloom on command. But something in me keeps turning toward you like thaw toward sun.";
                    case RAFIQ -> "I could make a joke. A devastating one. Instead I will say the disastrous truth: I want you to choose me when no one is watching.";
                    case CALDER -> "I trust what holds after weather. This feeling has taken weather. I am beginning to respect its joinery.";
                };
                case 250 -> switch (this) {
                    case SERAPHINE -> "A future with you looks like unlocked doors, fair terms, and my name belonging to me while I choose to answer when you say it.";
                    case MAERA -> "A future with you has maps on the table, arguments in the margins, and no locked drawer between us.";
                    case CASSIA -> "A future with you is a gate I do not have to guard alone, and a home that does not require orders to be worth defending.";
                    case LYRA -> "A future with you is clean water, mended sleeves, laughter from the next room, and care that is allowed to rest.";
                    case SAMIR -> "A future with you is a lamp in a window, questions at breakfast, and faith that survives being spoken plainly.";
                    case ARIA -> "A future with you is a door I use more than once, a trail that comes back, and someone who knows I may still need the sky.";
                    case VESPER -> "A future with you is winter stored properly, spring not rushed, and two people learning what can grow after damage.";
                    case RAFIQ -> "A future with you is scandalously domestic: good wine, fewer creditors, and someone laughing before I finish the lie.";
                    case CALDER -> "A future with you is a roof checked before rain, a table scarred by use, and promises maintained before they crack.";
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
                    case SERAPHINE -> "Then I choose the open door and the person beside it. That may be the freest thing I have done.";
                    case MAERA -> "Then let the record show this was chosen under no false citation.";
                    case CASSIA -> "Then I stand with you by name, not by command.";
                    case LYRA -> "Then let care be something we choose, not only something crisis demands.";
                    case SAMIR -> "Then I walk beside you, eyes open, light unforced.";
                    case ARIA -> "Then do not block the exits. I will stay because I can leave.";
                    case VESPER -> "Then I hold. Quietly. Stubbornly. Alive.";
                    case RAFIQ -> "Then witness my finest trick: sincerity without an escape route.";
                    case CALDER -> "Then we brace it properly and trust it under load.";
                };
                case 180 -> context.romanced()
                        ? romanceDateSincereLine(context)
                        : romanceDateFlirtLine(context);
                case 250 -> context.married()
                        ? "We have already made one promise. Let us keep making it in smaller, stubborn ways."
                        : switch (this) {
                            case SERAPHINE -> "No hidden ink, then. Just the terrifying plain text of wanting a life.";
                            case MAERA -> "Then we draft the future in pencil first, and trust each other enough to revise.";
                            case CASSIA -> "Then we build somewhere worth returning to after the line holds.";
                            case LYRA -> "Then we make room for beginnings, not only recoveries.";
                            case SAMIR -> "Then let it be a vow that can stand in daylight.";
                            case ARIA -> "Then promise me windows. I can learn doors if there are windows.";
                            case VESPER -> "Then we do not force the season. We prepare the soil and remain.";
                            case RAFIQ -> "Then I will try domestic courage. I expect applause.";
                            case CALDER -> "Then we maintain it. Daily. No pretty ruin, no neglected vow.";
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

        private String vesperMilestoneOpening(int threshold, DialogueContext context) {
            return switch (threshold) {
                case 50 -> "I used to watch your boots more than your face, to see what you crushed without noticing. Lately I watch your hands too. They are learning care.";
                case 100 -> "There is a thing grief taught me that I mistook for wisdom. If I tell you, I cannot keep using it as armor.";
                case 150 -> "Roots do not kneel, but they do choose where to hold. I have been choosing beside you often enough that the soil has noticed.";
                case 180 -> context.sharedTableConversation()
                        ? "This table is too warm for evasions. Something in me keeps turning toward you, and I am tired of pretending it is only weather."
                        : "If you ask me somewhere quiet, an inn table or Oathstead after rain, I may say the thing I have been growing too carefully to name here.";
                case 250 -> "I brought spring back from a grave and still feared asking it to stay. With you, I have begun wondering what a future looks like when nobody buries it for safety.";
                default -> "Something has changed between us. I would rather tend it than step around it.";
            };
        }

        private String femaleMilestoneOpening(int threshold, DialogueContext context) {
            if (this == VESPER) {
                return vesperMilestoneOpening(threshold, context);
            }
            return switch (threshold) {
                case 50 -> switch (this) {
                    case SERAPHINE -> "I have stopped counting exits before your sentences finish. Do not look too pleased. It means guarded respect, not a parade.";
                    case MAERA -> "I have begun treating you as a source worth preserving, which is more intimate than it sounds and less flattering than you hope.";
                    case CASSIA -> "I respect that you stand after bending. People who refuse to bend usually make everyone else pay for it.";
                    case LYRA -> "I have started believing your concern can survive inconvenience. That is rarer than kindness in a crisis.";
                    case ARIA -> "You read warnings better now. More importantly, you listen when I say the road is lying.";
                    default -> "I have begun respecting the shape of your choices.";
                };
                case 100 -> switch (this) {
                    case SERAPHINE -> "There is a difference between being saved and being owned. I know it in theory. With you, I am starting to know it in my body.";
                    case MAERA -> "There is a correction I keep avoiding because it is written in me, not in a book. I dislike that filing category.";
                    case CASSIA -> "I survived by becoming useful. I do not know who I am when someone stays after the use is done.";
                    case LYRA -> "I know how to hold everyone together. I do not know how to let someone hold me without counting it as failure.";
                    case ARIA -> "I learned to leave before anyone could choose it for me. Staying near you is making that habit clumsy.";
                    default -> "There is something I have not said because saying it makes it harder to keep distance.";
                };
                case 150 -> switch (this) {
                    case SERAPHINE -> "If I stay, it is not because you own the clause I signed. It is because the door stayed open and I still chose the room.";
                    case MAERA -> "Loyalty, properly cited: I have seen enough evidence to choose this road without calling it an error.";
                    case CASSIA -> "I was trained to hold lines for people who never learned my face. This time I know whose shoulder I am guarding.";
                    case LYRA -> "I have spent my life staying because people hurt. I stay with you because care can be more than triage.";
                    case ARIA -> "I know every exit from here. That is why it matters that I am still close enough for you to reach me.";
                    default -> "Loyalty is a choice I am making with my eyes open.";
                };
                case 180 -> switch (this) {
                    case SERAPHINE -> context.sharedTableConversation()
                            ? "This table has no contract between us, which makes what I want both simpler and more dangerous."
                            : "Ask me somewhere with poor witnesses and no ledgers. If I flirt, understand that I mean it more than is convenient.";
                    case MAERA -> context.sharedTableConversation()
                            ? "I keep trying to classify this as alliance, gratitude, shared evidence. The index refuses and keeps writing your name."
                            : "Find me a table, a bad candle, and an hour the archives cannot claim. I have a dangerously honest question.";
                    case CASSIA -> context.sharedTableConversation()
                            ? "No orders, no line to hold, and I still want you near. I do not have a soldier's word for that."
                            : "Ask me somewhere defensible and quiet. If I stop watching the door, you may take that as significant.";
                    case LYRA -> context.sharedTableConversation()
                            ? "When you are hurt, my hands know what to do. When you smile at me here, I lose the whole medical tradition."
                            : "Ask me somewhere no one needs saving for one hour. I want to learn what care says when it is not working.";
                    case ARIA -> context.sharedTableConversation()
                            ? "I want to stay near you when there is no tactical reason. Do you understand how suspicious that is?"
                            : "Ask me somewhere with exits and no speeches. If I stay anyway, you may take that as an answer.";
                    default -> "This has crossed into something warmer than tactics. We should name it gently.";
                };
                case 250 -> switch (this) {
                    case SERAPHINE -> "A future with you looks like unlocked doors, fair terms, and my name belonging to me while I choose to answer when you say it.";
                    case MAERA -> "A future with you has maps on the table, arguments in the margins, and no locked drawer between us.";
                    case CASSIA -> "A future with you is a gate I do not have to guard alone, and a home that does not require orders to be worth defending.";
                    case LYRA -> "A future with you is clean water, mended sleeves, laughter from the next room, and care that is allowed to rest.";
                    case ARIA -> "A future with you is a door I use more than once, a trail that comes back, and someone who knows I may still need the sky.";
                    default -> "The road keeps trying to end us. I have started imagining what remains if it fails.";
                };
                default -> "Something has changed between us. It deserves better than being stepped around.";
            };
        }

        private String vesperMilestoneWarmChoice(int threshold) {
            return switch (threshold) {
                case 50 -> "I will step more carefully.";
                case 100 -> "Tell me what grief taught you.";
                case 150 -> "Choose to hold beside me.";
                case 180 -> "Let me take you somewhere quiet.";
                case 250 -> "Imagine the future with me.";
                default -> "Let us tend this honestly.";
            };
        }

        private String femaleMilestoneWarmChoice(int threshold) {
            if (this == VESPER) {
                return vesperMilestoneWarmChoice(threshold);
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

        private String vesperMilestoneBoundaryChoice(int threshold) {
            return switch (threshold) {
                case 50 -> "Keep watching until you are sure.";
                case 100 -> "Only open what will not wound you again.";
                case 150 -> "No roots should become chains.";
                case 180 -> "We can let this grow slowly.";
                case 250 -> "Do not promise a future out of fear.";
                default -> "Careful is allowed.";
            };
        }

        private String femaleMilestoneBoundaryChoice(int threshold) {
            if (this == VESPER) {
                return vesperMilestoneBoundaryChoice(threshold);
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

        private String vesperMilestoneWarmLine(int threshold, DialogueContext context) {
            return switch (threshold) {
                case 50 -> "Then begin with small mercies. Leave living things enough room to survive your good intentions. I will notice.";
                case 100 -> "Grief told me every dead thing was my verdict. You make that lie harder to water.";
                case 150 -> "Then I hold. Not because I cannot leave, and not because you asked prettily. Because staying has become a living choice.";
                case 180 -> context.sharedTableConversation()
                        ? "Then stay close enough that I can stop pretending the warmth is from the room."
                        : "Then ask me again where there is rain on a roof or Oathstead mud under our boots. I want the quiet version to be true too.";
                case 250 -> context.married()
                        ? "Then we keep making the promise in useful ways: watered roots, honest mornings, no graves dug for living hopes."
                        : "Then we prepare the soil and remain. No forced bloom, no pretty cage, just a future given enough care to risk itself.";
                default -> "Then we tend this gently and see what survives in daylight.";
            };
        }

        private String femaleMilestoneWarmLine(int threshold, DialogueContext context) {
            if (this == VESPER) {
                return vesperMilestoneWarmLine(threshold, context);
            }
            return switch (threshold) {
                case 50 -> "Then let respect be the first honest plank. Not enough for a bridge, but enough to stop pretending the river is not there.";
                case 100 -> switch (this) {
                    case SERAPHINE -> "Then I will tell you the parts that do not flatter me. Kindly refrain from looking like someone who can be trusted. It is distracting.";
                    case MAERA -> "Then I will cite the wound directly. Do not interrupt unless I start footnoting my own panic.";
                    case CASSIA -> "Then I will say it plainly. I am tired of being useful and calling that a self.";
                    case LYRA -> "Then I will let the care point toward me for once. I may complain. Continue anyway.";
                    case ARIA -> "Then do not block the exits. I will stay because I can leave.";
                    default -> "Good. I will tell it badly at first. Be patient with the parts that limp.";
                };
                case 150 -> switch (this) {
                    case SERAPHINE -> "Then I choose the open door and the person beside it. That may be the freest thing I have done.";
                    case MAERA -> "Then let the record show this was chosen under no false citation.";
                    case CASSIA -> "Then I stand with you by name, not by command.";
                    case LYRA -> "Then let care be something we choose, not only something crisis demands.";
                    case ARIA -> "Then do not make a ceremony of it. I am staying, which is ceremony enough.";
                    default -> "Then we stand as chosen allies.";
                };
                case 180 -> context.romanced()
                        ? romanceDateSincereLine(context)
                        : romanceDateFlirtLine(context);
                case 250 -> context.married()
                        ? "We have already made one promise. Let us keep making it in smaller, stubborn ways."
                        : switch (this) {
                            case SERAPHINE -> "No hidden ink, then. Just the terrifying plain text of wanting a life.";
                            case MAERA -> "Then we draft the future in pencil first, and trust each other enough to revise.";
                            case CASSIA -> "Then we build somewhere worth returning to after the line holds.";
                            case LYRA -> "Then we make room for beginnings, not only recoveries.";
                            case ARIA -> "Then promise me windows. I can learn doors if there are windows.";
                            default -> "Then we let the future stay possible and chosen.";
                        };
                default -> "Then let this be spoken plainly enough to survive tomorrow.";
            };
        }

        private String vesperMilestoneBoundaryLine(int threshold, DialogueContext context) {
            return switch (threshold) {
                case 50 -> "Good. Trust that grows too fast often borrows strength from somewhere else.";
                case 100 -> "Thank you. Some truths need air, but not a crowd. Not even a kind crowd.";
                case 150 -> "That is why I can offer it. A chain would make me go still. Choice lets me root.";
                case 180 -> "Slow is not refusal. Slow is how living things prove they are not being dragged by fear.";
                case 250 -> "Then let the future remain a seed tonight. Seeds are not failures because they are not trees yet.";
                default -> "Careful is not cold. It is one way of staying kind.";
            };
        }

        private String femaleMilestoneBoundaryLine(int threshold, DialogueContext context) {
            if (this == VESPER) {
                return vesperMilestoneBoundaryLine(threshold, context);
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

        private String vesperMilestoneAcceptLine(int threshold) {
            return switch (threshold) {
                case 50 -> "Then respect has soil. Not deep yet. Real enough to plant in.";
                case 100 -> "Then I will speak more when the old lie loosens its roots.";
                case 150 -> "Then we hold by choice. That is the only loyalty I trust.";
                case 180 -> "Then we let this grow honestly, with light and weather and no hands forcing the stem.";
                case 250 -> "Then the future stays alive. Not trapped. Not buried for its own good. Alive.";
                default -> "Then this moment has somewhere to grow.";
            };
        }

        private String femaleMilestoneAcceptLine(int threshold) {
            if (this == VESPER) {
                return vesperMilestoneAcceptLine(threshold);
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

        private String vesperMilestoneChangeLine(int threshold) {
            return switch (threshold) {
                case 50 -> "Named things change. Sometimes that is the first mercy.";
                case 100 -> "Yes. It changes the shape of the grief. I am learning that change is not always theft.";
                case 150 -> "Then we keep choosing it awake. Roots sleep, but loyalty should not.";
                case 180 -> "Time is allowed. Wanting does not become wiser because someone pulls at it.";
                case 250 -> "A future grown slowly may hold through winter better than one sworn in summer heat.";
                default -> "Then we give the change air.";
            };
        }

        private String femaleMilestoneChangeLine(int threshold) {
            if (this == VESPER) {
                return vesperMilestoneChangeLine(threshold);
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
                return "You want the honest ledger? Fine. I measure you by " + context.approvalMotive()
                        + ", and lately I have had fewer reasons to flinch while doing it.";
            }
            if (relationship >= 60) {
                return "I am still deciding what your choices mean. The pattern matters more than one noble moment.";
            }
            return "Too early for certainty. But even early choices leave footprints.";
        }

        String valuesTrustLine(int relationship, DialogueContext context) {
            if (relationship >= 150) {
                return switch (this) {
                    case SERAPHINE -> "You keep checking whether a choice is actually free. I trust that more than any oath dressed in gold.";
                    case MAERA -> "You let evidence inconvenience you. That is rarer than courage and more useful.";
                    case CASSIA -> "You stand where standing costs something. I trust weight better than speeches.";
                    case LYRA -> "You return to people after the danger passes. That is when care becomes real.";
                    case SAMIR -> "You do not demand that light obey you. You ask what it reveals.";
                    case ARIA -> "You notice who gets left behind on the road. You do not always say it, but you notice.";
                    case VESPER -> "You come back to living things even when dead things shout louder.";
                    case RAFIQ -> "You make second chances look less ridiculous. I remain offended and grateful.";
                    case CALDER -> "You maintain what you claim to love. Not perfectly. Enough to matter.";
                };
            }
            return "I trust the moments when your action and your explanation point in the same direction.";
        }

        String valuesWorryLine(int relationship, DialogueContext context) {
            if (relationship >= 120) {
                return switch (this) {
                    case SERAPHINE -> "That you will let duty become a prettier word for ownership if enough people praise you for it.";
                    case MAERA -> "That urgency will teach you to accept simple answers because they move faster.";
                    case CASSIA -> "That you will mistake endurance for healing and call the wound useful.";
                    case LYRA -> "That you will spend yourself like supplies no one has to replace.";
                    case SAMIR -> "That victory will tempt you to stop asking whether the light is honest.";
                    case ARIA -> "That you will walk into danger because people have started expecting you to.";
                    case VESPER -> "That you will harvest hope too quickly and wonder why the soil thins.";
                    case RAFIQ -> "That you will become necessary to everyone and never ask who holds you.";
                    case CALDER -> "That you will build too fast and forget every promise needs maintenance.";
                };
            }
            return "That you confuse motion with judgment. Most people do, right before the road teaches them.";
        }

        String valuesRequestLine(int relationship, DialogueContext context) {
            if (!context.activeSoftRequestText().isBlank()) {
                return "You already carry this from me: " + context.activeSoftRequestText()
                        + " I do not need it performed loudly. I need it remembered when the road makes forgetting convenient.";
            }
            if (relationship >= 120) {
                return switch (this) {
                    case SERAPHINE -> "When someone asks you to save them, make sure they are not handing you a chain with flowers on it.";
                    case MAERA -> "When truth becomes inconvenient, do not make it lonely.";
                    case CASSIA -> "When I say I can hold, ask whether I should have to.";
                    case LYRA -> "When I start caring for everyone except myself, interrupt me.";
                    case SAMIR -> "When faith sounds too clean, ask what it had to wash away.";
                    case ARIA -> "When I joke about leaving, hear the part that is checking whether I can stay.";
                    case VESPER -> "When something looks dead, give it one honest look before burial.";
                    case RAFIQ -> "When I make pain charming, do not applaud too quickly.";
                    case CALDER -> "When I call something sound, ask when we inspect it again.";
                };
            }
            return "Remember that " + context.approvalMotive() + " is not a decoration for me. It is how I know where to stand.";
        }

        String requestAcceptedLine(DialogueContext context) {
            return "Good. I will hold you to that gently at first, and less gently if the world requires it.";
        }

        String requestHonestLine() {
            return "That may be the healthier promise. Perfect promises crack loudly.";
        }

        String requestRefusedLine() {
            return "Then I know where the line is tonight. I do not like it, but I prefer a named line to a false door.";
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
                    case SERAPHINE -> "Then sit close enough that I do not have to perform being fine.";
                    case MAERA -> "Good. I have spent too long with records that cannot answer back.";
                    case CASSIA -> "A while is acceptable. Longer if no one calls it rest too loudly.";
                    case LYRA -> "Stay, then. I am tired of every kindness having a departure planned inside it.";
                    case SAMIR -> "A shared silence can be prayer if no one tries to own it.";
                    case ARIA -> "Fine. But if I relax, you are legally required not to mention it.";
                    case VESPER -> "Good. Some roots only move when no one is watching.";
                    case RAFIQ -> "At last, an audience for my restraint. I will be magnificent and mostly quiet.";
                    case CALDER -> "Good. Sit. Even beams need to stop bearing weight sometimes.";
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
            if (this == VESPER && !context.questOutcome("vesper_oathstead_growth").isBlank()) {
                return "Tonight I only ask this: do not let Oathstead make the snowroot a symbol before it has become food, shade, and proof that patience works.";
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
            if (!context.questOutcome("vesper_oathstead_growth").isBlank() && relationship >= 150) {
                return "At " + place + ", I keep expecting the snowroot to need me elsewhere. It does not. Sit with me while I learn that not every living thing is a summons.";
            }
            if (!context.questOutcome("vesper_spring_return").isBlank() && relationship >= 120) {
                return place + " is loud with ordinary life. After the buried spring, ordinary feels less small than it used to.";
            }
            if (relationship >= 150) {
                return "At " + place + ", even the noise has roots. Sit with me before I pretend I only came for shelter.";
            }
            if (relationship >= 60) {
                return place + " makes people talk like the road cannot overhear. I am not sure I trust that. I may use it anyway.";
            }
            return place + " is warmer than the road. That does not automatically make it safe.";
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
            if (this == ARIA && !context.questOutcome("aria_crossing_truth").isBlank()) {
                return "Good. Rootmaw ended and the road kept going. I am still deciding whether that is comforting or insulting.";
            }
            if (!context.questOutcome("vesper_oathstead_growth").isBlank()) {
                return "Good. The root can stand one evening without my shadow over it. I would like to find out whether I can do the same.";
            }
            if (!context.questOutcome("vesper_family_grove").isBlank() && relationship >= 120) {
                return "Good. I spent years listening for names under snow. Tonight I can listen to someone breathing beside me.";
            }
            if (relationship >= 150) {
                return "Good. Some roots only move when no one is watching.";
            }
            return "A while, then. No grand confessions. Just enough quiet to prove we can survive it.";
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
            if (context.epilogueReady()) {
                if (!context.questOutcome("vesper_oathstead_growth").isBlank()) {
                    return "After came. The snowroot did not turn into a banner or a miracle. It turned into work, shade, and people remembering to water it. That is better.";
                }
                return "The ending happened and left soil behind. Endings do that when they are honest.";
            }
            if (!context.questOutcome("vesper_spring_return").isBlank() && relationship >= 150) {
                return "After all this, I imagine a place where spring is not hurried and grief is not given the shovel. It frightens me less when you are in the thought.";
            }
            if (relationship >= 150) {
                return "After all this? I try not to name it too often. Named futures can frighten easily. But I have one.";
            }
            return "After all this, we count who is breathing and do not insult the dead by pretending it was simple.";
        }

        String memoryOpening() {
            return switch (this) {
                case SERAPHINE -> "Memory is a witness. Unreliable, yes, but rarely useless.";
                case MAERA -> "Good. Let us compare the record in your head with the one in mine.";
                case CASSIA -> "Some moments should be named. Otherwise they become weight with no handle.";
                case LYRA -> "Careful. Remembering can heal, but it can also reopen what was only resting.";
                case SAMIR -> "The past is not sacred because it happened. It matters because it still asks things of us.";
                case ARIA -> "If this is sentiment, walk lightly. If it is useful, I am listening.";
                case VESPER -> "Old moments root deeper when spoken aloud.";
                case RAFIQ -> "Ah, memory. My most theatrical and least obedient colleague.";
                case CALDER -> "A remembered thing can hold a person steady if it was set straight.";
            };
        }

        String memoryReflection(String memory) {
            String remembered = memory == null || memory.isBlank() ? "that moment" : memory.strip();
            return switch (this) {
                case SERAPHINE -> "I remember: " + remembered + " It matters because choice has to be witnessed, not merely claimed.";
                case MAERA -> "I remember: " + remembered + " The detail still refuses to become simple, which usually means it is true.";
                case CASSIA -> "I remember: " + remembered + " It did not fix everything. Good memories rarely do. They give us footing.";
                case LYRA -> "I remember: " + remembered + " I keep it gently. Some proof belongs beside the heart, not in a ledger.";
                case SAMIR -> "I remember: " + remembered + " It changed the shape of the light around us, whether we admitted it or not.";
                case ARIA -> "I remember: " + remembered + " I noticed who stayed, who flinched, and who pretended not to care.";
                case VESPER -> "I remember: " + remembered + " Something living took root there, even if it looked like trouble at first.";
                case RAFIQ -> "I remember: " + remembered + " I made several excellent remarks internally and at least one useful decision.";
                case CALDER -> "I remember: " + remembered + " It became part of the structure. Small brace, real weight.";
            };
        }

        String memorySharedResponse() {
            return switch (this) {
                case SERAPHINE -> "Then we are both bound to the better version of what happened.";
                case MAERA -> "Shared testimony. Stronger than either account alone.";
                case CASSIA -> "Good. I would rather not carry that one by myself.";
                case LYRA -> "Then it is a little less lonely in the remembering.";
                case SAMIR -> "Two witnesses make a steadier flame.";
                case ARIA -> "Fine. But if you make it ceremonial, I am leaving first.";
                case VESPER -> "Shared roots hold better in bad weather.";
                case RAFIQ -> "Excellent. If memory must misbehave, at least it has company.";
                case CALDER -> "Good. Things held by two hands are harder to drop.";
            };
        }

        String memoryForwardResponse() {
            return switch (this) {
                case SERAPHINE -> "Forward, then. But not forgetfully.";
                case MAERA -> "Forward with annotations. I can tolerate that.";
                case CASSIA -> "Forward is allowed. Running is not required.";
                case LYRA -> "Forward, and we check the wound before calling it healed.";
                case SAMIR -> "Forward, carrying enough light to know why.";
                case ARIA -> "Forward works. It keeps people from staring too long.";
                case VESPER -> "Forward. Growth is not stillness with better poetry.";
                case RAFIQ -> "Forward, preferably with fewer dramatic injuries. I am flexible.";
                case CALDER -> "Forward, with maintenance. Always maintenance.";
            };
        }

        String outcomeMemoryRootLabel() {
            return switch (this) {
                case SERAPHINE -> "Can we talk about the Vale choices?";
                case MAERA -> "Can we talk about the archive choices?";
                case CASSIA -> "Can we talk about the duty choices?";
                case LYRA -> "Can we talk about the healing choices?";
                case ARIA -> "Can we talk about the roadwatch choices?";
                case VESPER -> "Can we talk about the Snowroot choices?";
                default -> "Can we talk about our choices?";
            };
        }

        String outcomeMemoryOpening(DialogueContext context) {
            return switch (this) {
                case SERAPHINE -> "Vale did not become free because one paper burned. It became free because we kept asking who benefited from every beautiful chain.";
                case MAERA -> "The archive road was never about finding one true page. It was about deciding what truth deserved once we had proof.";
                case CASSIA -> "Duty kept changing shape under us. I want to remember where it protected people and where it only protected orders.";
                case LYRA -> "Healing was never only salves and stitches. It was choosing who gets care before panic makes the decision for us.";
                case ARIA -> "Roads remember choices. So do scouts, even when they pretend they only remember tracks.";
                case VESPER -> "The Snowroot road did not change because we found one answer. It changed because we kept choosing what kind of answer we could live beside.";
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
                return vesperOutcomeMemoryLine(outcomeKey, context);
            }
            String tone = outcomeTone(context.questOutcome(outcomeKey));
            return switch (this) {
                case SERAPHINE -> seraphineOutcomeMemoryLine(outcomeKey, tone);
                case MAERA -> maeraOutcomeMemoryLine(outcomeKey, tone);
                case CASSIA -> cassiaOutcomeMemoryLine(outcomeKey, tone);
                case LYRA -> lyraOutcomeMemoryLine(outcomeKey, tone);
                case ARIA -> ariaOutcomeMemoryLine(outcomeKey, tone);
                default -> "I remember that choice. It kept speaking after the road moved on.";
            };
        }

        private String vesperOutcomeMemoryLine(String outcomeKey, DialogueContext context) {
            String tone = outcomeTone(context.questOutcome(outcomeKey));
            return switch (outcomeKey) {
                case "vesper_first_root" -> "When the first root woke, " + tone
                        + ". I still think that named the whole road: new life can be frightening and still deserve patience.";
                case "vesper_practical_care" -> "With firewood, feverroot, and the sick child, " + tone
                        + ". You learned the lesson I trust most: omens mean little if people freeze beside them.";
                case "vesper_family_grove" -> "In my family grove, " + tone
                        + ". That choice kept me from turning every grave into a simple accusation.";
                case "vesper_buried_spring" -> "When the old druid's burial came into the light, " + tone
                        + ". You helped me see that survival can be wise and wrong at the same time.";
                case "vesper_spring_return" -> "At the sealed spring, " + tone
                        + ". The grove heard that. So did I.";
                case "vesper_oathstead_growth" -> "At Oathstead, " + tone
                        + ". That was when staying stopped feeling like a trap and started feeling like soil.";
                default -> "I remember that choice. It grew quietly afterward, which is often how important things behave.";
            };
        }

        private String seraphineOutcomeMemoryLine(String outcomeKey, String tone) {
            return switch (outcomeKey) {
                case "seraphine_first_lie" -> "In the counting house, " + tone
                        + ". That was when I began suspecting you understood evidence is not freedom until someone can use it.";
                case "seraphine_clerk_truth" -> "With the hidden clerk, " + tone
                        + ". Mercy is easy to announce and difficult to keep alive under paperwork.";
                case "seraphine_family_debt" -> "At the Vale counting room, " + tone
                        + ". You did not mistake my father's desperation for consent. I remember that more than I admit.";
                case "seraphine_red_notary" -> "After the Red Notary fell, " + tone
                        + ". Freedom became messy there, which is how I knew it was probably real.";
                case "seraphine_oathstead_promise" -> "At Oathstead, " + tone
                        + ". That was the first promise in a long time that did not feel like someone else's handwriting.";
                default -> "I remember that choice. It had clauses nobody wrote down and consequences nobody could fully audit.";
            };
        }

        private String maeraOutcomeMemoryLine(String outcomeKey, String tone) {
            return switch (outcomeKey) {
                case "maera_first_truth" -> "With the forbidden footnote, " + tone
                        + ". That was the first time the official record looked nervous in front of us.";
                case "maera_moving_map" -> "When the map moved, " + tone
                        + ". I still think about how truth behaves when a page finally has somewhere to go.";
                case "maera_archive_warning" -> "At the archive warning, " + tone
                        + ". Old institutions fear correction more than fire, which is useful information.";
                case "maera_cult_pages" -> "With the cult pages, " + tone
                        + ". A record can be dangerous and still deserve better than silence.";
                case "maera_family_correction" -> "When my family correction surfaced, " + tone
                        + ". You helped me stop treating inheritance like an argument I had already lost.";
                case "maera_vault_truth" -> "In the vault, " + tone
                        + ". The truth did not become cleaner because we reached the locked room. It became harder to abandon.";
                case "maera_oathstead_archive" -> "At Oathstead, " + tone
                        + ". That archive is small, badly lit, and therefore still capable of becoming honest.";
                default -> "I remember that choice. It remains an excellent footnote with poor manners.";
            };
        }

        private String cassiaOutcomeMemoryLine(String outcomeKey, String tone) {
            return switch (outcomeKey) {
                case "cassia_gate_memory" -> "At the first gate memory, " + tone
                        + ". That was when I stopped letting orders stand between me and the faces they cost.";
                case "cassia_frost_road" -> "On the frost road, " + tone
                        + ". Survival looked less like glory there and more like keeping people moving.";
                case "cassia_survivor_truth" -> "With the survivor's truth, " + tone
                        + ". I needed to hear someone living contradict the version duty preferred.";
                case "cassia_blue_steel_orders" -> "With the Blue Steel orders, " + tone
                        + ". Written command stopped feeling sacred once we measured it against the dead.";
                case "cassia_varran_truth" -> "When Varran's truth came out, " + tone
                        + ". I hated how much of it I understood. Understanding is not absolution.";
                case "cassia_old_gate" -> "At the old gate, " + tone
                        + ". I learned a held line can still be wrong if nobody asks who it crushes.";
                case "cassia_ironwall_trial" -> "At Ironwall's trial, " + tone
                        + ". That choice made judgment heavier, and cleaner for being heavy.";
                case "cassia_oathstead_duty" -> "At Oathstead, " + tone
                        + ". Duty became something I could choose without disappearing inside it.";
                default -> "I remember that choice. It still has weight, but weight can become footing.";
            };
        }

        private String lyraOutcomeMemoryLine(String outcomeKey, String tone) {
            return switch (outcomeKey) {
                case "lyra_first_triage" -> "At the first triage, " + tone
                        + ". Care had to become judgment there. I still dislike that, which is probably healthy.";
                case "lyra_names_on_cot" -> "With the names on the cot, " + tone
                        + ". Patients become easier to neglect when someone steals their names first.";
                case "lyra_false_medicine" -> "When the false medicine surfaced, " + tone
                        + ". I wanted rage to be enough. It was not. It rarely is.";
                case "lyra_antidote_rule" -> "With the antidote rule, " + tone
                        + ". A cure that teaches no lesson invites the next fever to be cleverer.";
                case "lyra_first_patient" -> "With my first patient, " + tone
                        + ". I had carried that memory like a splint on the wrong bone.";
                case "lyra_moving_care" -> "When care had to move, " + tone
                        + ". Mercy cannot wait for everyone to reach the same room.";
                case "lyra_fever_cure" -> "With the fever cure, " + tone
                        + ". That was when healing stopped being a frantic response and started becoming a plan.";
                case "lyra_oathstead_clinic" -> "At Oathstead, " + tone
                        + ". The clinic means care can arrive before someone has to beg for it.";
                default -> "I remember that choice. Some wounds heal around decisions like that.";
            };
        }

        private String ariaOutcomeMemoryLine(String outcomeKey, String tone) {
            return switch (outcomeKey) {
                case "aria_ambush_lesson" -> "After the ambush, " + tone
                        + ". You noticed the lesson without making me teach it twice. That is rarer than good aim.";
                case "aria_mother_truth" -> "At my mother's trail, " + tone
                        + ". You let hope and proof stand in the same clearing without forcing one to shoot the other.";
                case "aria_crossing_truth" -> "At Rootmaw Crossing, " + tone
                        + ". The road kept something from her alive. I am still deciding how to thank it.";
                case "aria_oathstead_future" -> "At Oathstead, " + tone
                        + ". That was when a road back stopped sounding like surrender.";
                default -> "I remember that choice. Trails do not end just because people stop looking.";
            };
        }

        private String femaleOutcomeSharedLine(String outcomeKey, DialogueContext context) {
            if (this == VESPER) {
                return vesperOutcomeSharedLine(outcomeKey, context);
            }
            return switch (this) {
                case SERAPHINE -> "Then remember it without making me grateful for the wound. Witness is enough. Ownership is not invited.";
                case MAERA -> "Then remember it with the margins intact. The uncomfortable part is usually where the record begins telling the truth.";
                case CASSIA -> "Then remember that we chose with weight on us. That does not excuse everything. It does mean the choice was real.";
                case LYRA -> "Then remember the cost and the care together. I do not trust mercy that edits out either one.";
                case ARIA -> "Then remember where we stood and who got to walk away. Roads are made of those details.";
                default -> "Then we remember it honestly.";
            };
        }

        private String vesperOutcomeSharedLine(String outcomeKey, DialogueContext context) {
            return switch (outcomeKey) {
                case "vesper_family_grove" -> "Then remember the names with me, not as proof I failed them, but as proof they kept trying to protect what could live.";
                case "vesper_buried_spring" -> "Then remember that mercy without truth becomes another burial. I do not want to confuse those again.";
                case "vesper_oathstead_growth" -> "Then remember the first bloom as work, not decoration. I can live beside that kind of hope.";
                default -> "Then remember it with mud on it. Clean memories are usually edited by someone afraid of roots.";
            };
        }

        private String femaleOutcomeQuestionLine(String outcomeKey, DialogueContext context) {
            if (this == VESPER) {
                return vesperOutcomeQuestionLine(outcomeKey, context);
            }
            String outcome = context.questOutcome(outcomeKey);
            return switch (this) {
                case SERAPHINE -> seraphineOutcomeQuestionLine(outcome);
                case MAERA -> maeraOutcomeQuestionLine(outcome);
                case CASSIA -> cassiaOutcomeQuestionLine(outcome);
                case LYRA -> lyraOutcomeQuestionLine(outcome);
                case ARIA -> ariaOutcomeQuestionLine(outcome);
                default -> "Different, perhaps. Easier, no. Easy answers rarely survive the road.";
            };
        }

        private String seraphineOutcomeQuestionLine(String outcome) {
            if ("protect".equals(outcome)) {
                return "Perhaps I would protect less quietly. Hidden people become easy to misplace in noble systems.";
            }
            if ("accountability".equals(outcome)) {
                return "Perhaps I would leave the blade sheathed half a breath longer. Consequences are cleaner when nobody can call them revenge by another name.";
            }
            if ("mercy".equals(outcome)) {
                return "Perhaps I would make the mercy sign its own receipt. Mercy without boundaries becomes someone's new debt.";
            }
            if ("truth".equals(outcome)) {
                return "Perhaps I would make the truth harder to steal afterward. Spoken truth still needs locks, witnesses, and very rude friends.";
            }
            return "Different, perhaps. But not tidier. Tidy freedom usually has a trapdoor.";
        }

        private String maeraOutcomeQuestionLine(String outcome) {
            if ("protect".equals(outcome)) {
                return "Perhaps I would annotate the protection more clearly. Hidden truth grows mildew if nobody knows where it sleeps.";
            }
            if ("accountability".equals(outcome)) {
                return "Perhaps I would ask one more question before judgment. Scholarship and justice both rot when they become impatient.";
            }
            if ("mercy".equals(outcome)) {
                return "Perhaps I would attach a sharper footnote. Mercy is not an eraser, however popular that misuse becomes.";
            }
            if ("truth".equals(outcome)) {
                return "Perhaps I would publish it with better safeguards. Truth deserves daylight, but daylight attracts knives.";
            }
            return "Different evidence might change my method. It would not change the need to record what happened.";
        }

        private String cassiaOutcomeQuestionLine(String outcome) {
            if ("protect".equals(outcome)) {
                return "Perhaps I would ask who protection silenced. A shield can become a wall if held too long.";
            }
            if ("accountability".equals(outcome)) {
                return "Perhaps I would make room for remorse before sentence. Not instead of sentence. Before.";
            }
            if ("mercy".equals(outcome)) {
                return "Perhaps I would make mercy stand inspection. Mercy that cannot bear witness is only retreat.";
            }
            if ("truth".equals(outcome)) {
                return "Perhaps I would speak it with less steel in my mouth. Truth does not need to wound twice to count.";
            }
            return "I might choose with steadier hands. That is not the same as choosing against what we did.";
        }

        private String lyraOutcomeQuestionLine(String outcome) {
            if ("protect".equals(outcome)) {
                return "Perhaps I would ask whether protection was keeping someone alive or keeping them quiet. Both can look gentle from a distance.";
            }
            if ("accountability".equals(outcome)) {
                return "Perhaps I would check the wound before tightening the bandage. Accountability must stop bleeding, not just make a point.";
            }
            if ("mercy".equals(outcome)) {
                return "Perhaps I would make the mercy report back for follow-up. Healing without follow-up is optimism in a clean apron.";
            }
            if ("truth".equals(outcome)) {
                return "Perhaps I would give the truth water first. People swallow hard things better when they are not already choking.";
            }
            return "I might choose with better supplies now. That does not make the old triage careless.";
        }

        private String ariaOutcomeQuestionLine(String outcome) {
            if ("protect".equals(outcome)) {
                return "Perhaps I would leave a clearer mark for the people following us. Protection should not require guessing the scout's mind.";
            }
            if ("accountability".equals(outcome)) {
                return "Perhaps I would let the confession breathe before turning it into a warning sign. Useful is not the same as fair.";
            }
            if ("mercy".equals(outcome)) {
                return "Perhaps I would put teeth in the mercy. Some people hear softness and start looking for the next victim.";
            }
            if ("truth".equals(outcome)) {
                return "Perhaps I would say it with fewer thorns. Maybe. Do not write that down.";
            }
            return "Maybe. Roads look different from the far side. That does not mean the old tracks were false.";
        }

        private String vesperOutcomeQuestionLine(String outcomeKey, DialogueContext context) {
            String outcome = context.questOutcome(outcomeKey);
            if ("protect".equals(outcome)) {
                return "Perhaps I would protect less silently. Protection can become another sealed door if nobody is allowed to ask why it is locked.";
            }
            if ("accountability".equals(outcome)) {
                return "Perhaps I would leave more room for grief to explain itself. Consequences matter, but so does hearing why someone chose badly.";
            }
            if ("mercy".equals(outcome)) {
                return "Perhaps I would make the mercy carry a sharper memory. Forgiveness that forgets too quickly feeds the same rot twice.";
            }
            if ("truth".equals(outcome)) {
                return "Perhaps I would speak it softer. Truth does not become false because it is kind.";
            }
            return "Perhaps. Growth changes the gardener too. That does not make the old choice a lie.";
        }

        private String outcomeTone(String outcome) {
            return switch (outcome) {
                case "truth" -> "you chose to name the truth before comfort";
                case "protect" -> "you chose to shield the living thing until it could stand";
                case "mercy" -> "you chose mercy without pretending the wound had vanished";
                case "accountability" -> "you chose consequences before easy softness";
                default -> "you chose " + (outcome == null || outcome.isBlank() ? "patience" : outcome.replace('_', ' '));
            };
        }

        String opinionAboutPlayer(DialogueContext context) {
            if (context.married()) {
                return "You are home and road at once. Inconvenient. Necessary. Mine by choice, never by claim.";
            }
            if (context.romanced()) {
                return "You keep making danger feel less lonely. I am still deciding whether to forgive you for that.";
            }
            return switch (this) {
                case SERAPHINE -> "You walk into old contracts like they are doors. I like that you keep checking for locks.";
                case MAERA -> "You are evidence with boots. Occasionally muddy evidence, but persuasive.";
                case CASSIA -> "You bend under weight, then stand again. I trust that more than speeches.";
                case LYRA -> "You get hurt often enough to worry me and keep helping often enough to make the worry worthwhile.";
                case SAMIR -> "You ask for light without demanding obedience from it. That is rarer than doctrine admits.";
                case ARIA -> "You keep walking into trouble like it owes you rent. Annoying. Useful. Occasionally brave.";
                case VESPER -> "You are not gentle, exactly. But you return to what needs care.";
                case RAFIQ -> "You make sincerity look survivable. I resent the example and may follow it.";
                case CALDER -> "You are not finished. Good. Finished things cannot be repaired.";
            };
        }

        String opinionAbout(CompanionVoice subject) {
            return switch (this) {
                case SERAPHINE -> switch (subject) {
                    case MAERA -> "Maera treats truth like contraband. Excellent instincts, terrible hiding posture.";
                    case CASSIA -> "Cassia is what happens when guilt learns shield discipline.";
                    case LYRA -> "Lyra makes mercy sound like logistics. That is why I trust it.";
                    case SAMIR -> "Samir questions light as if it might answer honestly. Brave, or beautifully doomed.";
                    case ARIA -> "Aria notices exits first. I respect a woman with priorities.";
                    case VESPER -> "Vesper is quiet the way winter is quiet: full of decisions.";
                    case RAFIQ -> "Rafiq jokes like a man checking whether the floor will hold.";
                    case CALDER -> "Calder could make a bridge out of bad news and shame the river into behaving.";
                    default -> "They are useful. That is not the same as simple.";
                };
                case MAERA -> switch (subject) {
                    case SERAPHINE -> "Seraphine is a legal argument with knives. I would cite her carefully.";
                    case CASSIA -> "Cassia is a primary source on duty hurting people. She hates that, which makes her valuable.";
                    case LYRA -> "Lyra records pain because forgetting is another wound.";
                    case SAMIR -> "Samir is revising faith in real time. Fascinating. Painful footnotes.";
                    case ARIA -> "Aria reads tracks the way I read margins.";
                    case VESPER -> "Vesper understands buried things better than most archivists.";
                    case RAFIQ -> "Rafiq is unreliable only when pretending not to care.";
                    case CALDER -> "Calder believes structures tell the truth. I am fond of that hypothesis.";
                    default -> "They are a useful contradiction.";
                };
                case CASSIA -> switch (subject) {
                    case SERAPHINE -> "Seraphine hides wounds behind elegance. The blade is honest, at least.";
                    case MAERA -> "Maera questions orders written by the dead. Good.";
                    case LYRA -> "Lyra holds the line after the line breaks.";
                    case SAMIR -> "Samir has courage. Not loud courage. Better kind.";
                    case ARIA -> "Aria moves like someone who expects betrayal. I hope she learns otherwise.";
                    case VESPER -> "Vesper is patient with roots and less patient with fools. Sensible.";
                    case RAFIQ -> "Rafiq jokes when cornered. That is either courage or bad armor. Possibly both.";
                    case CALDER -> "Calder understands that strength without maintenance fails.";
                    default -> "They can stand if the day asks it.";
                };
                case LYRA -> switch (subject) {
                    case SERAPHINE -> "Seraphine has old scars dressed beautifully. I do not confuse that for healed.";
                    case MAERA -> "Maera forgets to eat when truth is nearby. Someone should supervise scholarship.";
                    case CASSIA -> "Cassia carries blame like armor. Heavy. Familiar.";
                    case SAMIR -> "Samir is learning that light can comfort without commanding.";
                    case ARIA -> "Aria says she is fine too quickly. That is a symptom.";
                    case VESPER -> "Vesper knows living things survive in ugly shapes.";
                    case RAFIQ -> "Rafiq laughs near pain. I keep bandages ready anyway.";
                    case CALDER -> "Calder treats broken things with respect. People included, when he remembers.";
                    default -> "They need rest more than advice.";
                };
                case SAMIR -> switch (subject) {
                    case SERAPHINE -> "Seraphine taught herself freedom with sharp tools. There is holiness in refusing chains.";
                    case MAERA -> "Maera seeks truth without asking whether it is permitted. I admire that sin.";
                    case CASSIA -> "Cassia knows obedience can wound. That knowledge may yet become light.";
                    case LYRA -> "Lyra's mercy has calluses. That makes it trustworthy.";
                    case ARIA -> "Aria survives by reading shadows. I hope one day she trusts dawn too.";
                    case VESPER -> "Vesper listens to buried life. A sacred practice, though she would dislike the phrase.";
                    case RAFIQ -> "Rafiq makes confession wear perfume. Still confession.";
                    case CALDER -> "Calder's faith is in foundations. Mine could learn from it.";
                    default -> "They carry a light I do not fully understand.";
                };
                case ARIA -> switch (subject) {
                    case SERAPHINE -> "Seraphine smiles like a locked drawer. I like her.";
                    case MAERA -> "Maera would chase a forbidden map through a burning library and complain about the smoke damaging notes.";
                    case CASSIA -> "Cassia watches gates like they might apologize. I like her. From a distance.";
                    case LYRA -> "Lyra sees when people limp emotionally. Inconvenient talent.";
                    case SAMIR -> "Samir glows with questions. Hard to hide with, easy to trust.";
                    case VESPER -> "Vesper understands quiet. Proper quiet, not awkward silence.";
                    case RAFIQ -> "Rafiq is what happens when trouble learns choreography.";
                    case CALDER -> "Calder makes roads less likely to kill children. Hard to mock that.";
                    default -> "They are less predictable than tracks.";
                };
                case VESPER -> switch (subject) {
                    case SERAPHINE -> "Seraphine is thorn and silk. Both come from living things that learned defense.";
                    case MAERA -> "Maera digs with ink instead of hands. Still digging.";
                    case CASSIA -> "Cassia is winter iron. Cold because it had to hold shape.";
                    case LYRA -> "Lyra tends wounds like seedlings. Some survive because she refuses otherwise.";
                    case SAMIR -> "Samir carries dawn carefully, as if it might break. Perhaps it can.";
                    case ARIA -> "Aria knows paths that avoid attention. Roots approve.";
                    case RAFIQ -> "Rafiq blooms loudly to distract from where he was cut.";
                    case CALDER -> "Calder respects weight. That is close to wisdom.";
                    default -> "They are growing in difficult soil.";
                };
                case RAFIQ -> switch (subject) {
                    case SERAPHINE -> "Seraphine could rob a courthouse and make the courthouse apologize.";
                    case MAERA -> "Maera makes treason sound well organized. Attractive quality in a scholar.";
                    case CASSIA -> "Cassia's glare could reinforce a gate. I am careful where I admire it.";
                    case LYRA -> "Lyra saves lives with terrifying competence and no appreciation for dramatic bleeding.";
                    case SAMIR -> "Samir is proof that holiness can have excellent posture and troubling questions.";
                    case ARIA -> "Aria distrusts applause. I am trying to understand this medical condition.";
                    case VESPER -> "Vesper could shame a winter into introspection.";
                    case CALDER -> "Calder is what stone would be if it learned disappointment and loyalty.";
                    default -> "They improve the room, usually by making me look subtler.";
                };
                case CALDER -> switch (subject) {
                    case SERAPHINE -> "Seraphine knows where a thing is weak before anyone admits it is load-bearing.";
                    case MAERA -> "Maera measures roads in truth. Good material, if brittle.";
                    case CASSIA -> "Cassia holds. Sometimes too hard. Still, holding matters.";
                    case LYRA -> "Lyra repairs people who pretend they are not structures.";
                    case SAMIR -> "Samir keeps checking whether the light is honest. Good habit.";
                    case ARIA -> "Aria sees bad footing before the rest of us find mud.";
                    case VESPER -> "Vesper knows roots can break stone. Respectful kind of warning.";
                    case RAFIQ -> "Rafiq is a bridge with too much paint. Still might hold.";
                    default -> "They have weight. Worth accounting for.";
                };
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
                case SERAPHINE -> "No hidden clauses. I just want you.";
                case MAERA -> "Your footnotes are distracting me.";
                case CASSIA -> "You make standing guard difficult.";
                case LYRA -> "Your smile is terrible for my discipline.";
                case SAMIR -> "You are brighter than my better judgment.";
                case ARIA -> "Stay close enough to be trouble.";
                case VESPER -> "I think I am thawing near you.";
                case RAFIQ -> "Flirt with me before I become responsible.";
                case CALDER -> "You are becoming part of the foundation.";
            };
        }

        String romanceDateOpening(DialogueContext context, String place) {
            if (context.sharedTableConversation()) {
                return switch (this) {
                    case SERAPHINE -> "A quiet hour at " + place + ", with no one selling, signing, or owning anything. Suspiciously luxurious.";
                    case MAERA -> place + " has poor archival discipline and excellent potential for being interrupted by honesty.";
                    case CASSIA -> "This corner of " + place + " has two exits, a tolerable sightline, and you. I am trying to notice the third thing most.";
                    case LYRA -> place + " is warm enough, quiet enough, and not currently bleeding. A rare medical recommendation.";
                    case SAMIR -> "The light in " + place + " is ordinary. That makes what I feel near you harder to blame on miracles.";
                    case ARIA -> place + " is crowded enough to vanish in and quiet enough to stay. I am choosing the second.";
                    case VESPER -> "At " + place + ", even the noise has roots. Sit with me before I start pretending I only came for shelter.";
                    case RAFIQ -> place + " is almost worthy of us. I will forgive its flaws if you sit close enough.";
                    case CALDER -> place + " has bad chairs and a sound roof. Good enough for truth if we sit carefully.";
                };
            }
            return switch (this) {
                case SERAPHINE -> "Ask me somewhere with no ledgers. I want to learn what I say when nothing is being negotiated.";
                case MAERA -> "Find me a table, a bad candle, and an hour the archives cannot claim. I will bring a dangerously honest question.";
                case CASSIA -> "Somewhere defensible, quiet, and not pretending to be a battlefield. I am told such places exist.";
                case LYRA -> "An inn. A bench. Any place where no one asks me to triage the evening.";
                case SAMIR -> "Take me somewhere with ordinary light. I want to see whether this still feels holy without ceremony.";
                case ARIA -> "Somewhere with exits and no speeches. If I stay anyway, you may take that as significant.";
                case VESPER -> "Find a quiet table or Oathstead after rain. I trust places better when they smell alive.";
                case RAFIQ -> "Invite me properly at an inn, and I will pretend not to have been waiting for it.";
                case CALDER -> "A roof, a table, and time enough that neither of us calls it maintenance. That would do.";
            };
        }

        String romanceDateFlirtLine(DialogueContext context) {
            return switch (this) {
                case SERAPHINE -> "Careful. That sounded almost like desire without paperwork. I may need to hear it again.";
                case MAERA -> "If you keep looking at me like that, I will lose my place in three separate arguments.";
                case CASSIA -> "You are making my pulse tactically unhelpful. I am choosing not to object.";
                case LYRA -> "That is unfair. I know several remedies for fever and none for you.";
                case SAMIR -> "Do not make me call this temptation. I am trying to have a better theology about your eyes.";
                case ARIA -> "Trouble, then. But the kind I might walk toward on purpose.";
                case VESPER -> "Say that softly. Some things grow crooked if everyone stares.";
                case RAFIQ -> "At last, a battlefield suited to my talents. Continue.";
                case CALDER -> "That was not subtle. Good. Subtle things get missed in bad weather.";
            };
        }

        String romanceDateSincereLine(DialogueContext context) {
            return switch (this) {
                case SERAPHINE -> "Then I will sit here as myself. Not borrowed, not bought, not fleeing. With you.";
                case MAERA -> "Then let the record be incomplete for once. I would rather live this hour than annotate it.";
                case CASSIA -> "Then I can stop scanning the horizon for one breath and let myself be found.";
                case LYRA -> "Then I will put down the work for a moment. Not because it is done. Because you asked me to rest with you.";
                case SAMIR -> "Then I will let the quiet be enough. No sermon. No sign. Just your hand near mine.";
                case ARIA -> "Then do not make a cage of it. Let it be a trail we both choose again tomorrow.";
                case VESPER -> "Then I will not rush to name it. I will sit with it until it roots properly.";
                case RAFIQ -> "Then I will be sincere for the length of one drink. Possibly two, if you are devastating.";
                case CALDER -> "Then we give the hour a foundation: truth, patience, and no pretending this is nothing.";
            };
        }

        String romanceDatePlaceLine(DialogueContext context, String place) {
            if (context.sharedTableConversation()) {
                return switch (this) {
                    case SERAPHINE -> "Then this is ours for an hour. No witnesses with claims, no ink with teeth.";
                    case MAERA -> "Then I will stop cataloging exits and start remembering the shape of your voice in " + place + ".";
                    case CASSIA -> "Then I stand down. Briefly. Do not make me regret discovering I can.";
                    case LYRA -> "Then we rest here. If the world needs us, it can wait until the cup is empty.";
                    case SAMIR -> "Then let " + place + " hold the ordinary miracle of two people choosing not to hurry.";
                    case ARIA -> "Then I stay. Not trapped. Not cornered. Here.";
                    case VESPER -> "Then we let the hour grow without pulling at its roots.";
                    case RAFIQ -> "Then " + place + " becomes historic. I recommend a plaque and another drink.";
                    case CALDER -> "Then we sit, and we let the roof do its job while we do ours.";
                };
            }
            return "Then we will find the right place. Not a grand gesture. A quiet hour chosen on purpose.";
        }

        String romanceDateBoundaryLine(DialogueContext context) {
            return switch (this) {
                case SERAPHINE -> "Good. Wanting without pressure is the only version I trust.";
                case MAERA -> "Careful is acceptable. Some truths deserve proper handling.";
                case CASSIA -> "Gentle does not mean uncertain. It means disciplined.";
                case LYRA -> "Good. Tender things bruise when hurried, and I am tired of treating preventable wounds.";
                case SAMIR -> "Then we let it be honest before we ask it to be holy.";
                case ARIA -> "Gentle, with exits. That I can understand.";
                case VESPER -> "Good. Nothing living grows better because someone shouted at it.";
                case RAFIQ -> "Gentle. Terrifyingly mature. I will attempt not to ruin it with charm.";
                case CALDER -> "Slow is not weak. Slow is how foundations learn the ground.";
            };
        }

        String loyalOpening() {
            return "You have become harder to leave behind. That is either trust or poor tactical judgment.";
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
            return "There is a truth I would not hand to a stranger. You are no longer convenient enough to be one.";
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
            return "You have a habit of being present before I decide whether I need anyone present.";
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
            return "I am beginning to believe you are useful on purpose.";
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
            return "We are not friends yet. We are two people with overlapping problems and tolerable timing.";
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
            return "You are alive, armed, and asking questions. Only one of those is automatically useful.";
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
                case SERAPHINE -> "seraphine";
                case MAERA -> "maera";
                case CASSIA -> "cassia";
                case LYRA -> "lyra";
                case SAMIR -> "samir";
                case ARIA -> "aria";
                case VESPER -> "vesper";
                case RAFIQ -> "rafiq";
                case CALDER -> "calder";
            };
        }

        String recruitmentRootLabel() {
            return switch (this) {
                case SERAPHINE -> "Ask Seraphine to choose the road";
                case MAERA -> "Ask Maera to join the search";
                case CASSIA -> "Ask Cassia to stand with you";
                case LYRA -> "Ask Lyra to travel with you";
                case SAMIR -> "Ask Samir to walk beside you";
                case ARIA -> "Ask Aria to scout with you";
                case VESPER -> "Ask Vesper to come with you";
                case RAFIQ -> "Ask Rafiq to join the party";
                case CALDER -> "Ask Calder to help build the road";
            };
        }

        String recruitmentOpening() {
            return switch (this) {
                case SERAPHINE -> "You are about to ask for a promise. I can hear the hinges creak. Say it plainly, and do not make it sound like ownership.";
                case MAERA -> "If this is an invitation, be precise. I have followed enough badly labeled roads for one lifetime.";
                case CASSIA -> "You want me beside you beyond this one task. Say so directly. Orders ruined enough things; clarity will do better.";
                case LYRA -> "Traveling with you means more wounds, worse weather, and probably fewer sensible rest breaks. I am listening anyway.";
                case SAMIR -> "If you ask me to come, do not ask for obedience. Ask for witness, questions, and light that argues back.";
                case ARIA -> "Careful. Ask a scout to stay near you and she starts checking whether you understand exits.";
                case VESPER -> "If you ask me to travel, do not make it sound like uprooting. Some things come with you because the soil changed.";
                case RAFIQ -> "Ah. The formal invitation. Try to make it sound daring. I am allergic to administrative affection.";
                case CALDER -> "You are asking whether I will put my weight behind your road. Good. Ask it like you know weight matters.";
            };
        }

        String recruitmentRoadChoice() {
            return switch (this) {
                case SERAPHINE -> "Travel with me by choice, no clauses.";
                case MAERA -> "Walk with me. We will follow the truth together.";
                case CASSIA -> "Stand with me on the road.";
                case LYRA -> "Come with me. We will keep people alive together.";
                case SAMIR -> "Walk beside me, questions and all.";
                case ARIA -> "Scout with me. Keep the road honest.";
                case VESPER -> "Come with me. We will tend what still lives.";
                case RAFIQ -> "Join me. I could use your trouble.";
                case CALDER -> "Join me. Help me make the road hold.";
            };
        }

        String recruitmentOathsteadChoice() {
            return switch (this) {
                case SERAPHINE -> "Oathstead has room without owning you.";
                case MAERA -> "Oathstead needs your maps and questions.";
                case CASSIA -> "Oathstead needs someone who understands duty.";
                case LYRA -> "Oathstead needs care before crisis.";
                case SAMIR -> "Oathstead needs light that can be questioned.";
                case ARIA -> "Oathstead needs roads watched by someone sharp.";
                case VESPER -> "Oathstead has soil worth tending.";
                case RAFIQ -> "Oathstead needs a second chance with style.";
                case CALDER -> "Oathstead needs hands that know repairs.";
            };
        }

        String recruitmentRoadAcceptLine() {
            return switch (this) {
                case SERAPHINE -> "Then I choose the road, not the debt. If you ever forget the difference, I will remind you with elegance and possibly knives.";
                case MAERA -> "Then I am coming. Bring spare ink, patience for corrections, and the humility to admit when the map insults us correctly.";
                case CASSIA -> "Then I stand with you. Not by command. By name, by judgment, and because the road still has people worth defending.";
                case LYRA -> "Then I come with you. Someone has to count bandages, interrupt heroics, and make sure mercy arrives before the funeral.";
                case SAMIR -> "Then I walk beside you. I will bring light, doubt, and the habit of asking whether victory still has clean hands.";
                case ARIA -> "Then I scout with you. I will find the snares, mock the obvious paths, and come back when I said I would. Mostly.";
                case VESPER -> "Then I come. Not uprooted. Transplanted, perhaps. I will know the difference if we start dying dramatically.";
                case RAFIQ -> "Then I join you. History will say you needed brilliance. I will allow a footnote about mutual trust.";
                case CALDER -> "Then I am with you. Roads fail where people stop maintaining them. I will bring the hammer and the uncomfortable reminders.";
            };
        }

        String recruitmentOathsteadAcceptLine() {
            return switch (this) {
                case SERAPHINE -> "Oathstead without ownership. A suspiciously decent offer. I accept, and reserve the right to inspect every promise.";
                case MAERA -> "A young settlement with editable records and no royal censor in the rafters. Fine. I am interested.";
                case CASSIA -> "If Oathstead needs a line held for the right reasons, I will stand there. I may even learn to rest between alarms.";
                case LYRA -> "Then I will help make care arrive before panic. Build me shelves, clean water, and a place where heroes wash their hands.";
                case SAMIR -> "Then I will keep a lamp there, but not a throne. Let people gather around light without kneeling to it.";
                case ARIA -> "Then I will watch the roads in and out. Do not call it settling. Call it knowing where to return.";
                case VESPER -> "Then I will tend the soil. Food first, symbols later. A living place should earn its poetry.";
                case RAFIQ -> "A corner for second chances, fewer creditors, and people too busy surviving to ask about my best lies. Excellent.";
                case CALDER -> "Then give me tools, bad plans to correct, and a roof that admits it needs work. I can do something with that.";
            };
        }

        String recruitmentWaitLine() {
            return switch (this) {
                case SERAPHINE -> "Good. Asking without pressing is a rare talent. Practice it. I may reward consistency.";
                case MAERA -> "Deferred, not rejected. I appreciate accurate labels.";
                case CASSIA -> "Good. A choice made under pressure is too close to an order.";
                case LYRA -> "Thank you. Consent is healthier when it has room to breathe.";
                case SAMIR -> "Then the question waits in honest light. That is better than forcing an answer in a shadow.";
                case ARIA -> "Fine. I will keep pretending I did not appreciate the way you asked.";
                case VESPER -> "Good. Living things answer better when no one yanks at the stem.";
                case RAFIQ -> "Wise. Let anticipation improve my entrance.";
                case CALDER -> "Good. Measure twice, ask once, and do not rush the join.";
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

    private static void addQuestOfferNodes(Map<String, DialogueNode> nodes, Npc npc, Quest quest, String prefix) {
        if (quest == null) {
            return;
        }
        QuestDialogueStage stage = questDialogueStage(quest);
        if (stage == QuestDialogueStage.OFFER) {
            addNode(nodes, prefix, questDialog(quest, false), List.of(
                    new DialogueChoice("Why does this need doing?", prefix + "_reason", 1),
                    new DialogueChoice("Where should I start?", prefix + "_where", 1),
                    new DialogueChoice("What do you think of this task?", prefix + "_opinion", 1),
                    new DialogueChoice(questCommitLabel(quest), prefix + "_accept", 1, questAcceptEffect(quest)),
                    new DialogueChoice(questDoubtLabel(quest), prefix + "_doubt", -1),
                    new DialogueChoice("Back to topics", "root")
            ));
            addNode(nodes, prefix + "_reason", questReasonLine(quest), List.of(
                    new DialogueChoice("That is enough reason.", prefix + "_accept", 1, questAcceptEffect(quest)),
                    new DialogueChoice("Where should I start?", prefix + "_where", 0),
                    new DialogueChoice("Back to topics", "root")
            ));
            addNode(nodes, prefix + "_where", questWhereLine(quest), List.of(
                    new DialogueChoice(questCommitLabel(quest), prefix + "_accept", 1, questAcceptEffect(quest)),
                    new DialogueChoice("Why does this matter?", prefix + "_reason", 0),
                    new DialogueChoice("I need to think first.", "root", 0),
                    new DialogueChoice("Back to topics", "root")
            ));
            addNode(nodes, prefix + "_opinion", questOpinionLine(npc, quest), List.of(
                    new DialogueChoice("I understand the stakes.", prefix + "_accept", 1, questAcceptEffect(quest)),
                    new DialogueChoice("Where should I start?", prefix + "_where", 0),
                    new DialogueChoice("Back to topics", "root")
            ));
            addNode(nodes, prefix + "_accept", questOfferDetailLine(quest), backChoices());
            addNode(nodes, prefix + "_doubt", questDoubtResponse(npc, quest), backChoices());
            return;
        }
        if (stage == QuestDialogueStage.READY) {
            addNode(nodes, prefix, questDialog(quest, false), List.of(
                    new DialogueChoice("Here is what I found.", prefix + "_turnin", 2, questTurnInEffect(quest)),
                    new DialogueChoice("What does this prove?", prefix + "_meaning", 1),
                    new DialogueChoice("What happens after this?", prefix + "_after", 1),
                    new DialogueChoice("Back to topics", "root")
            ));
            addNode(nodes, prefix + "_meaning", questReadyMeaningLine(quest), backChoices());
            addNode(nodes, prefix + "_after", questAftermathHintLine(quest), backChoices());
            addNode(nodes, prefix + "_turnin", questReadyMeaningLine(quest), backChoices());
            return;
        }
        if (stage == QuestDialogueStage.COMPLETED) {
            addNode(nodes, prefix, questDialog(quest, false), List.of(
                    new DialogueChoice("What changed after this?", prefix + "_after", 1),
                    new DialogueChoice("Was it worth the cost?", prefix + "_opinion", 1),
                    new DialogueChoice("Back to topics", "root")
            ));
            addNode(nodes, prefix + "_after", questAftermathLine(quest), backChoices());
            addNode(nodes, prefix + "_opinion", questCompletedOpinionLine(npc, quest), backChoices());
            return;
        }
        if (quest.activeObjectiveKind() == Quest.ObjectiveKind.CHOICE && !quest.ready()) {
            addNode(nodes, prefix, questDialog(quest, false), List.of(
                    new DialogueChoice("Tell the truth plainly.", prefix + "_choice_truth", 2, questOutcomeEffect(quest, "truth")),
                    new DialogueChoice("Protect them for now.", prefix + "_choice_protect", 1, questOutcomeEffect(quest, "protect")),
                    new DialogueChoice("Show mercy, but ask for honesty.", prefix + "_choice_mercy", 2, questOutcomeEffect(quest, "mercy")),
                    new DialogueChoice("There has to be accountability.", prefix + "_choice_accountability", 1, questOutcomeEffect(quest, "accountability")),
                    new DialogueChoice("Back to topics", "root")
            ));
            addNode(nodes, prefix + "_choice_truth", "You choose truth over comfort. The branch will remember that.", backChoices());
            addNode(nodes, prefix + "_choice_protect", "You choose protection, at least until the whole truth can stand without breaking someone.", backChoices());
            addNode(nodes, prefix + "_choice_mercy", "You choose mercy without pretending the harm did not happen.", backChoices());
            addNode(nodes, prefix + "_choice_accountability", "You choose accountability. Kindness can wait; consequences cannot.", backChoices());
            return;
        }
        addNode(nodes, prefix, questDialog(quest, false), List.of(
                new DialogueChoice(stage == QuestDialogueStage.IN_PROGRESS ? "This is what I have so far." : "Remind me why this matters.", prefix + "_progress", 1),
                new DialogueChoice("Where do I finish this?", prefix + "_where", 1),
                new DialogueChoice("What should I watch for?", prefix + "_warning", 1),
                new DialogueChoice("What do you think of the task now?", prefix + "_opinion", 1),
                new DialogueChoice("Back to topics", "root")
        ));
        addNode(nodes, prefix + "_progress", questProgressLine(quest), backChoices());
        addNode(nodes, prefix + "_where", questWhereLine(quest), backChoices());
        addNode(nodes, prefix + "_warning", questWarningLine(quest), backChoices());
        addNode(nodes, prefix + "_opinion", questOpinionLine(npc, quest), backChoices());
    }

    private static String questAcceptEffect(Quest quest) {
        return quest == null ? "" : "quest:accept:" + quest.id;
    }

    private static String questTurnInEffect(Quest quest) {
        return quest == null ? "" : "quest:turnin:" + quest.id;
    }

    private static String questOutcomeEffect(Quest quest, String outcome) {
        return quest == null ? "" : "quest:outcome:" + quest.id + ":" + outcome;
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

    private static String questOfferDetailLine(Quest quest) {
        return "Take it only if you mean to finish it. The work asks you to "
                + questActionPhrase(quest).toLowerCase(Locale.ROOT) + ". Reward: "
                + quest.rewardGold + " gold and " + quest.rewardXp + " experience.";
    }

    private static String questAcceptedLine(Quest quest) {
        return "The work is accepted, but not yet proven. " + questReasonLine(quest);
    }

    private static String questReasonLine(Quest quest) {
        String stakes = quest.mainStoryQuest()
                ? "This is not local trouble. It is one loose thread in the larger dark moving across Alderfall."
                : quest.companionQuest()
                ? "This matters because it touches someone who chose to trust you with more than a chore."
                : "Small trouble becomes large trouble when decent people learn to step around it.";
        return stakes + " " + quest.description;
    }

    private static String questWhereLine(Quest quest) {
        return "Start with " + objectiveLocationLine(quest) + ". " + questActionPhrase(quest)
                + ", then return when the work can be answered for.";
    }

    private static String questProgressLine(Quest quest) {
        int remaining = Math.max(0, quest.activeNeeded() - quest.progress);
        if (remaining <= 0) {
            return "You have enough. Bring the proof back before the trail cools.";
        }
        return "You have " + quest.progress + "/" + quest.activeNeeded() + ". "
                + remaining + " more " + quest.activeTarget() + (remaining == 1 ? "" : "s")
                + " will make the claim harder to dismiss.";
    }

    private static String questWarningLine(Quest quest) {
        return switch (quest.activeObjectiveKind()) {
            case DEFEAT -> "Do not hunt the target so narrowly that you miss who sent it, fed it, or benefits from its fear.";
            case RESCUE -> "Reach them quickly, but do not mistake panic for a plan. A rescue still needs eyes.";
            case DEFEND -> "A defended place teaches you what the enemy wants badly enough to risk taking.";
            case GATHER -> "Take what proves the work. Do not strip the place bare just because a ledger would look cleaner.";
            case DELIVER -> "Carry the thing carefully. Delivery is trust made physical.";
            case VISIT -> "Inspect the place, but distrust anything arranged too neatly. Staged evidence loves obedient eyes.";
            case SEARCH -> "Search for what refuses to be obvious. The hidden detail is usually carrying the honest weight.";
            case TALK -> "Ask plainly and listen past the first answer. People often rehearse the version that hurts least.";
            case ASK_AROUND -> "Do not take the loudest rumor as truth. Look for the detail repeated by people who did not speak together.";
            case REPORT -> "Say what happened without polishing it into comfort. A report loses value when it flatters the listener.";
            case ESCORT -> "Keep the road boring. Boring means everyone arrived alive.";
            case CHOICE -> "Make the choice with open eyes. Mercy, truth, and convenience are not the same thing.";
        };
    }

    private static String questOpinionLine(Npc npc, Quest quest) {
        String name = npc == null ? "They" : npc.name();
        return switch (quest.activeObjectiveKind()) {
            case DEFEAT -> name + " does not like asking for blood, but likes leaving threats unanswered even less.";
            case RESCUE -> name + " is trying not to sound afraid for someone who may still be saved.";
            case DEFEND -> name + " knows a wall is only as strong as the people willing to stand at it.";
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
        return "This is the moment where work becomes testimony. " + cleanQuestLine(quest.activeReadyDialog(), "")
                + " If the proof is true, the next choice belongs to the person who asked.";
    }

    private static String questAftermathHintLine(Quest quest) {
        return "After this, the road may not become safer, but it becomes less ignorant. That is often how real victories begin.";
    }

    private static String questAftermathLine(Quest quest) {
        return "The task is finished, but finished does not mean erased. "
                + cleanQuestLine(quest.activeCompleteDialog(), "") + " Alderfall keeps the consequence, not just the reward.";
    }

    private static String questCompletedOpinionLine(Npc npc, Quest quest) {
        String name = npc == null ? "They" : npc.name();
        return name + " seems changed by the answer. Not healed exactly, but less alone with the question.";
    }

    private static String questDoubtResponse(Npc npc, Quest quest) {
        String name = npc == null ? "They" : npc.name();
        return name + " does not soften the request. " + questReasonLine(quest)
                + " Refusing is allowed. Pretending it is weightless is not.";
    }

    private static String questActionPhrase(Quest quest) {
        return switch (quest.activeObjectiveKind()) {
            case DEFEAT -> "Hunt " + quest.activeTarget();
            case RESCUE -> "Rescue " + quest.activeTarget();
            case DEFEND -> "Defend " + quest.activeTarget();
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
                    "Rumor: A roadwarden swore the old milestones moved overnight. I told them milestones drink less than roadwardens, but I wrote it down anyway.",
                    "Rumor: Three miners came through with frost in their beards and sand in their packs. Nobody gets that lost by accident.",
                    "Rumor: Someone is buying empty bottles by the crate. Either alchemy is booming or grief is."
            );
            case "blacksmith" -> List.of(
                    "Forge: Snow, sand, marshwater, road dust; every biome ruins metal in its own smug little way.",
                    "Forge: A clean blade tells me its owner has not used it. A nicked blade tells me where they panic.",
                    "Forge: Bring me ore, old hinges, or enemy buckles. I am not romantic about metal."
            );
            case "baker" -> List.of(
                    "Oven: Bread listens to weather. Desert dough dries fast, marsh loaves sulk, and mountain yeast needs more patience than a priest.",
                    "Oven: People confess more over warm bread than under threat. Hunger is honest that way.",
                    "Oven: If the crust cracks too wide, rain is coming. If it stays pale, trouble is already here."
            );
            case "merchant" -> List.of(
                    "Trade: I price goods by distance and dread. Anything hauled through snow or dunes earns its markup honestly.",
                    "Trade: The best cargo is boring. The worst cargo whispers from inside the crate.",
                    "Trade: I can smell counterfeit coin, wet wool, and desperate customers. Only one of those gets a discount."
            );
            case "archivist" -> List.of(
                    "Records: The oldest maps describe Alderfall by smell first: pine resin, wet reeds, hot stone, grave dust.",
                    "Records: History is not dead. It is merely badly indexed and waiting to embarrass the living.",
                    "Records: Any ruin with perfect silence is lying. Stone always remembers something."
            );
            case "guard" -> List.of(
                    "Watch: Trouble changes uniforms by region, but it always leaves tracks if you look before it looks at you.",
                    "Watch: We rotate the night posts so fear does not learn anyone's favorite hiding place.",
                    "Watch: A quiet road is either safe, watched, or bait. I prefer knowing which."
            );
            case "healer" -> List.of(
                    "Remedy: Desert folk need water, mountain folk need warmth, marsh folk need clean cuts. The rest is listening.",
                    "Remedy: Half of medicine is asking where it hurts. The other half is noticing where they avoid answering.",
                    "Remedy: If a wound smells like copper, work quickly. If it smells sweet, work faster."
            );
            case "farmer" -> List.of(
                    "Fieldwork: Good soil tells the truth. If the birds go quiet or the wind changes taste, bring the tools inside.",
                    "Fieldwork: You learn patience from crops, suspicion from crows, and bargaining from weather.",
                    "Fieldwork: A fence is just a polite argument with the wilderness."
            );
            case "scout" -> List.of(
                    "Trailcraft: The safest path is rarely the shortest. I trust bent grass, loose pebbles, and old camp ash more than signs.",
                    "Trailcraft: If the road looks empty, check what the birds are avoiding.",
                    "Trailcraft: I mark routes by what can go wrong there. It makes my maps unpopular and useful."
            );
            case "quartermaster" -> List.of(
                    "Supplies: Count arrows twice, medicine three times, and socks every time it rains. Armies have failed for less.",
                    "Supplies: A heroic charge eats rations like anyone else.",
                    "Supplies: People remember who swung the sword. I remember who packed the bandages."
            );
            default -> List.of(
                    "Local talk: Everyone here watches the horizon. In Alderfall, the horizon usually watches back.",
                    "Local talk: You can tell who is new by who trusts a quiet morning.",
                    "Local talk: If people seem jumpy, it is because the land has started answering back."
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
                    "Rumor: A mule came down from the pass alone with blue candle wax on its saddle.",
                    "Rumor: Someone saw lanterns moving inside a whiteout, all in a perfect line."
            );
            case "desert" -> List.of(
                    "Rumor: A dune near the old ruins rings like a bell when the moon is thin.",
                    "Rumor: Dunewick children trade stories about rain the way city children trade sweets."
            );
            case "marsh" -> List.of(
                    "Rumor: The reeds near the black pools have started repeating names in the wrong voices.",
                    "Rumor: A fisher pulled up a boot full of clean, dry ash. Nobody liked that."
            );
            case "forest" -> List.of(
                    "Rumor: Oakhaven's oldest tree dropped green leaves during frost and nobody slept well after.",
                    "Rumor: There is a deer on the north road that watches campfires until they go out."
            );
            case "grave" -> List.of(
                    "Rumor: One grave bell rang underground for a full minute, then apologized in a child's voice.",
                    "Rumor: The Archive locked a map away because it kept adding fresh graves."
            );
            default -> List.of(
                    "Rumor: A trader paid double for broken charms and refused to say why.",
                    "Rumor: Travelers keep finding the same black feather in different inns."
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
                    "Advice: If three strangers lower their voices at once, refill their cups and remember their boots.",
                    "Advice: Good rumors arrive thirsty. Bad rumors arrive already paid for."
            );
            case "blacksmith" -> List.of(
                    "Advice: Oil your blade after marsh work, warm it slowly after snow, and never trust desert grit near a hinge.",
                    "Advice: If armor pinches in the shop, it will betray you on the road."
            );
            case "baker" -> List.of(
                    "Advice: Eat before a hard road. Empty stomachs make cowards out of sensible people.",
                    "Advice: A village with flour left still believes in tomorrow."
            );
            case "merchant" -> List.of(
                    "Advice: Count payment in the shade, promises in daylight, and enemies twice.",
                    "Advice: The road tax nobody names is fear. Every caravan pays it."
            );
            case "archivist" -> List.of(
                    "Advice: Read inscriptions from the bottom up if the stone is cursed. Old wards love dramatic openings.",
                    "Advice: Never trust a clean ruin. Someone cleaned it for a reason."
            );
            case "guard" -> List.of(
                    "Advice: Keep your back to stone, your fire low, and your exit boring.",
                    "Advice: The first sound in an ambush is usually the least honest one."
            );
            case "healer" -> List.of(
                    "Advice: Wash bites, warm frost, cool burns, and do not let proud people sleep with a fever.",
                    "Advice: The patient who says they are fine is either brave, foolish, or bleeding internally."
            );
            case "farmer" -> List.of(
                    "Advice: Watch animals before weather. They complain early and without politics.",
                    "Advice: If weeds all lean away from a path, pick another path."
            );
            case "scout" -> List.of(
                    "Advice: When tracks vanish, look up. When birds vanish, leave.",
                    "Advice: A trail that looks too easy has been prepared by someone who dislikes you."
            );
            case "quartermaster" -> List.of(
                    "Advice: Spare rope, dry socks, clean water. Everything else is a debate.",
                    "Advice: Never split supplies evenly. Split them by who is most likely to run."
            );
            default -> List.of(
                    "Advice: Ask two locals the same question. If both answer quickly, worry.",
                    "Advice: Keep one hand free and one promise unpaid."
            );
        };
        return pick(pool, random);
    }

    private static String rumorFollowDialog(String biomeContext, Random random) {
        List<String> pool = switch (biomeContext) {
            case "mountain" -> List.of(
                    "Rumor: They heard it from a courier who would not remove their mittens indoors.",
                    "Rumor: Snow muffles lies poorly. That is why mountain rumors arrive half true."
            );
            case "desert" -> List.of(
                    "Rumor: A well-keeper told it first, and well-keepers do not waste breath in the heat.",
                    "Rumor: In the dunes, a story surviving until morning is already suspicious."
            );
            case "marsh" -> List.of(
                    "Rumor: It came from reedcutters, which means it is either exact truth or a joke with boots.",
                    "Rumor: Marsh stories grow in the telling, but the roots are usually real."
            );
            case "forest" -> List.of(
                    "Rumor: Oakhaven children heard it from the old road, and children are better listeners than adults admit.",
                    "Rumor: The forest changes details, not endings."
            );
            default -> List.of(
                    "Rumor: It passed through three towns and lost only one corpse in the retelling.",
                    "Rumor: I do not believe all of it. That is why I believe part of it."
            );
        };
        return pick(pool, random);
    }

    private static String biomeFollowDialog(String biomeContext, Random random) {
        List<String> pool = switch (biomeContext) {
            case "mountain" -> List.of(
                    "Local: Watch for snow that falls sideways but leaves no drift. That means something larger is moving nearby.",
                    "Local: Keep metal wrapped at night. Cold makes honest tools spiteful."
            );
            case "desert" -> List.of(
                    "Local: If you see rain in the desert, check whether anything else can see it too.",
                    "Local: The hottest hours belong to lizards, spirits, and fools. Travel around all three."
            );
            case "marsh" -> List.of(
                    "Local: Step on roots, not shine. Shiny mud has plans.",
                    "Local: If your reflection moves late, stop looking and back away."
            );
            case "forest" -> List.of(
                    "Local: Do not answer voices from the trees unless they use your name correctly twice.",
                    "Local: Moss grows thickest where people stopped hurrying."
            );
            case "badlands" -> List.of(
                    "Local: Red dust hides tracks until sunset, then gives them back in gold light.",
                    "Local: Caves out there breathe warm before storms. That is useful and unpleasant."
            );
            case "grave" -> List.of(
                    "Local: Bring charcoal, salt, and manners near old stones.",
                    "Local: Never count graves aloud unless you know whether the number changed."
            );
            default -> List.of(
                    "Local: Watch what locals avoid stepping on. They learned for you.",
                    "Local: Every place has a warning sound. Survive long enough to learn this one."
            );
        };
        return pick(pool, random);
    }

    private static String biomeDialog(String biomeContext, Random random) {
        List<String> pool = switch (biomeContext) {
            case "mountain" -> List.of(
                    "Local: The snow never really stops up here. It pauses, listens at the shutters, then finds a new way into your boots.",
                    "Local: Mountain folk complain about snow the way sailors complain about water: constantly, correctly, and with affection.",
                    "Local: If the wind drops suddenly in the pass, people stop talking until it explains itself."
            );
            case "desert" -> List.of(
                    "Local: My grandmother said she saw rain on the dunes once. People still ask whether she meant water or a mirage with confidence.",
                    "Local: Desert stories always begin with a well, a shadow, or someone trusting the wrong shimmer.",
                    "Local: You can hear heat here. It ticks in roof beams and makes honest people short-tempered."
            );
            case "marsh" -> List.of(
                    "Local: The marsh keeps every secret twice: once under water, once under fog. Step where the reeds bend back.",
                    "Local: Mire paths move after heavy rain. People pretend that is mud. People are optimistic.",
                    "Local: If the frogs go quiet, check your boots, your purse, and your courage."
            );
            case "forest" -> List.of(
                    "Local: The trees are friendly by daylight. At dusk they begin repeating sounds they should not know.",
                    "Local: Forest people judge weather by leaf backs, birdsong, and whether the old roots ache.",
                    "Local: Oakhaven calls this green country. The green country calls us temporary."
            );
            case "badlands" -> List.of(
                    "Local: Red dust gets into bread, bandages, letters, prayers. After a week you stop fighting it and start naming shades.",
                    "Local: Badlands wind polishes bones and secrets with equal patience.",
                    "Local: There are places out there where your echo comes back tired."
            );
            case "water" -> List.of(
                    "Local: River people can tell depth by color, current by smell, and bad weather by how quiet the gulls get.",
                    "Local: Water roads are roads that remember every mistake.",
                    "Local: If the river looks still, it is either deep, cold, or thinking."
            );
            case "grave" -> List.of(
                    "Local: Around the old graves, people lower their voices even when no one taught them to.",
                    "Local: Graveyard weather feels colder because names keep shade.",
                    "Local: The old stones lean like listeners. I try not to give them fresh gossip."
            );
            default -> List.of(
                    "Local: This place has its own weather, its own warnings, and its own way of testing travelers.",
                    "Local: Every road in Alderfall has a favorite lie. Learning it is how you survive.",
                    "Local: People here read the sky before they read letters."
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

        private DialogueSession(Map<String, DialogueNode> nodes) {
            this.nodes = nodes;
        }

        public String line() {
            return line(0);
        }

        public String line(int relationship) {
            String line = currentNode().line();
            if (line.contains("{{relationship}}")) {
                line = line.replace("{{relationship}}", relationshipLabel(relationship) + " (" + relationship + ")");
            }
            String bridge = inSessionBridge();
            if (!bridge.isBlank()) {
                line = bridge + " " + line;
            }
            return line;
        }

        public List<String> optionLabels() {
            return currentNode().choices().stream()
                    .map(DialogueChoice::label)
                    .toList();
        }

        public DialogueChoiceResult choose(int index) {
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

        private String inSessionBridge() {
            DialogueNode node = currentNode();
            DialogueIntent currentIntent = node.intent();
            if (currentIntent == null || previousIntent == null || currentIntent == previousIntent || "root".equals(node.id())) {
                return "";
            }
            int visits = nodeVisits.getOrDefault(node.id(), 0);
            if (visits > 1) {
                return "They notice you circling back to the same point before answering again.";
            }
            return switch (currentIntent) {
                case QUEST_PRACTICAL -> switch (previousIntent) {
                    case QUEST_PERSONAL, FEELING, CONCERN, TRUST, ROMANCE ->
                            "They carry the personal thread into the practical answer instead of dropping it.";
                    default -> "";
                };
                case QUEST_PERSONAL, FEELING -> switch (previousIntent) {
                    case QUEST_PRACTICAL, NEXT_STEP ->
                            "The practical answer has not erased the feeling under it.";
                    default -> "";
                };
                case QUEST_WARNING -> "The answer turns from what happened to what could still hurt.";
                case MEMORY -> "The conversation slows as the present makes room for what already happened.";
                case OPINION -> "They treat the question as part of the same trust, not a separate curiosity.";
                case HOME, FUTURE -> "The thread moves from survival toward what it would mean to stay.";
                case ROMANCE -> "The air between you changes because the question is no longer only practical.";
                default -> "";
            };
        }
    }

    private record DialogueNode(String id, String line, List<DialogueChoice> choices, DialogueIntent intent, String repeatKey) {
    }

    public record DialogueChoiceResult(int relationshipDelta, String effect, String repeatKey) {
        public DialogueChoiceResult(int relationshipDelta) {
            this(relationshipDelta, "", "");
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
