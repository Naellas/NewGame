package com.alderfall.game;

final class VesperDialogue {
    private VesperDialogue() {
    }

    static CompanionDialogueProfile profile() {
        return new CompanionDialogueProfile(
                "Vesper",
                "Snowrest grovekeeper",
                "What's happening outside Snowrest?",
                "Something is wrong outside Snowrest. A root circle thawed through fresh snow, and I need to know why before anyone starts cutting.",
                "How do you know so much about Snowrest's grove?",
                "It does. My family tended the old grove before Snowrest learned to fear it. That is not the same as understanding what they did.",
                "I grew up believing I had to survive winter by myself. You made that feel less necessary.",
                "Yes. I want a future with you, but I want us to choose it honestly, not run into it because we are afraid.",
                "I am not easy to reach. Somehow you reached me anyway."
        );
    }

    static String characterBeat(DialogueLibrary.DialogueIntent intent) {
        return switch (intent) {
            case QUEST_ROOT, QUEST_CLARIFY, QUEST_PERSONAL -> "Vesper glances toward the Snowrest road before answering.";
            case QUEST_PRACTICAL, NEXT_STEP, QUEST_WARNING -> "Vesper keeps her voice low and practical.";
            case CONCERN, FEELING, MEMORY -> "Vesper takes a moment before she answers.";
            case ROMANCE, FUTURE, HOME -> "Vesper lets the guarded look soften, just a little.";
            default -> "Vesper studies you for a moment before speaking.";
        };
    }

    static String intentAcknowledgement(DialogueLibrary.DialogueIntent intent) {
        return switch (intent) {
            case CONCERN -> "You are asking what this costs me, not only what it costs the road. I notice that.";
            case QUEST_CLARIFY -> "Good. Ask for the plain answer first. We can deal with the rest after.";
            case MEMORY -> "I can talk about it. I may need a moment, but I can talk.";
            case HOME -> "Home is a difficult word for me. I will answer anyway.";
            case REQUEST -> "If I ask for something, I will try to say it clearly.";
            default -> "";
        };
    }

    static String[][] outcomeTopics() {
        return new String[][] {
                {"vesper_first_root", "The thawed root circle"},
                {"vesper_practical_care", "The Snowrest road breaks"},
                {"vesper_family_grove", "The sealed family grove"},
                {"vesper_buried_spring", "Halwen's sealed spring"},
                {"vesper_spring_return", "The freed spring"},
                {"vesper_oathstead_growth", "Vesper's Oathstead planting"}
        };
    }

    static String helpResponse() {
        return "The Snowrest road is cracking where old roots cross it. I need you to inspect the sap before anyone starts cutting. Black sap means danger. Green means it may still be alive.";
    }

    static String questCommitLabel() {
        return "I will inspect the road breaks before cutting roots.";
    }

    static String questCommitLabel(Quest quest) {
        if (quest == null) {
            return questCommitLabel();
        }
        return switch (quest.id) {
            case "vesper_chain_1" -> "I will inspect the root circle before judging it.";
            case "vesper_chain_2" -> "I will inspect the road breaks before cutting roots.";
            case "vesper_chain_3" -> "I will check the burrows before blaming the wolves.";
            case "vesper_chain_4" -> "I will inspect the grove before blaming anyone.";
            case "vesper_chain_5" -> "I will hear Halwen before judging the burial.";
            case "vesper_chain_6" -> "I will trace the conduit before cutting roots.";
            case "vesper_chain_7" -> "I will free the spring without forcing it.";
            case "vesper_chain_8" -> "I will test the ground, cut away the blight, and plant only if Oathstead earns it.";
            default -> questCommitLabel();
        };
    }

    static String questPersonalCommitLabel() {
        return "Then I will ask what your family sealed before judging it.";
    }

    static String questPracticalCommitLabel() {
        return "Show me what to inspect first.";
    }

    static String questAcceptedLine() {
        return "Good. Start with the damage we can prove. Check the root, the sap, and where it points. After that, we decide what has to be cut.";
    }

    static String chapterAcceptanceLine(String focus) {
        return "Good. Start with what you can see and prove. " + focus;
    }

    static String chapterClarifyLine(String focus) {
        return "The root circle is warm when it should be frozen. It might be healthy growth, or it might be rot spreading under the road. " + focus;
    }

    static String chapterPersonalLine(String title) {
        return "I care because my family tended Snowrest's old grove. They told people it died. What we are seeing now makes me think that was not the whole truth.";
    }

    static String offerReasonQuestion() {
        return "Is anyone in Snowrest hurt?";
    }

    static String offerPersonalQuestion() {
        return "You sound like you already know this place.";
    }

    static String offerPracticalQuestion() {
        return "What do I inspect before anyone panics?";
    }

    static String offerPushQuestion() {
        return "Say it plainly. What is happening?";
    }

    static String offerClarifyFollowup() {
        return "So this could hurt Snowrest.";
    }

    static String offerPersonalFollowup() {
        return "What did your family tell people?";
    }

    static String offerPracticalFollowup() {
        return "I know what to check first.";
    }

    static String defaultObjectiveLine() {
        return "Start with the broken road, the color of the sap, and the person who saw it first.";
    }

    static String objectiveVoiceWarning() {
        return "Cut only the black-sapped branch. A clear root may be holding the road up as much as breaking it.";
    }

    static String supportResponse() {
        return "Then do not rush the answer. If we panic, we will cut first and understand too late.";
    }

    static String warningResponse() {
        return "The danger is assuming quiet means safe. The roots were quiet until they broke the road.";
    }

    static String chapterWarningLine() {
        return "Do not force a quick answer just because this frightens us. That is how people turn a problem into a disaster.";
    }

    static String searchFocusClose() {
        return "before anyone damages the grove or the road by guessing.";
    }

    static String gatherFocusClose() {
        return "and take only what we need for proof.";
    }

    static String defeatFocusClose() {
        return "but remember that frightened creatures are not the same as monsters.";
    }

    static String progressRootGreeting() {
        return "The road damage points toward the old grove. We follow the sap next.";
    }

    static String readyRootGreeting() {
        return "We have the evidence now. Bring it here, and we will decide what it means.";
    }

    static String progressLine() {
        return "Keep following the damage in order. It should tell us where this started.";
    }

    static String originLine() {
        return "My family may have sealed the grove and called it mercy. If that is true, the road is showing us what their mercy cost.";
    }

    static String concernLine() {
        return "If the black sap reaches Snowrest's wells, this becomes a village crisis, not my private history.";
    }

    static String woundLine() {
        return "I thought the grove died when I was a child. Now I think the adults around me knew more than they admitted.";
    }

    static String practicalDetailStageOne() {
        return "Check color, warmth, and direction. Green means living. Black means rot or corruption. Warmth under snow means something is spending strength.";
    }

    static String practicalClose() {
        return "Bring back three things: where the sap points, who saw the first crack, and whether any root is already too rotten to save.";
    }

    static String readyLine() {
        return "Tell me what you found plainly. I would rather have an ugly fact than a comforting guess.";
    }

    static String afterQuestStageOne() {
        return "The first root is still alive, and the black sap points toward my family's grove. That gives us a next step.";
    }

    static String marriedGreeting() {
        return "There you are. I was hoping you would come by.";
    }

    static String romancedGreeting() {
        return "You are early. I am glad, though I may deny saying that later.";
    }

    static String oathsteadAssignedGreeting(String station) {
        return "The " + station + " is holding together. I have been checking supplies, soil, and who actually knows how to keep a garden alive.";
    }

    static String oathsteadGreeting() {
        return "Oathstead is not my old grove. That helps. I can let it become its own place.";
    }

    static String oathsteadNeedLine() {
        return "A working garden. Food first, pretty meanings later.";
    }

    static String oathsteadLine(DialogueLibrary.DialogueContext context) {
        if (!context.questOutcome("vesper_oathstead_growth").isBlank()) {
            return "The snowroot cutting is taking in Oathstead's soil. I still check it too often, but it is growing without me hovering over it.";
        }
        if (!context.questOutcome("vesper_spring_return").isBlank()) {
            return "After the spring was freed, Oathstead feels less temporary to me. It makes me wonder what we should plant here on purpose.";
        }
        if (context.stationedAtOathstead()) {
            return "I thought this place would feel temporary. It still might be, but people here are trying to build instead of hide.";
        }
        return "Oathstead is rough, but honest. I prefer that.";
    }

    static String oathsteadHomeLine(DialogueLibrary.DialogueContext context) {
        if (!context.questOutcome("vesper_oathstead_growth").isBlank()) {
            return "It feels like a place that made room before asking me to stay. That matters to me.";
        }
        if (!context.questOutcome("vesper_family_grove").isBlank()) {
            return "It does not feel like my family grove, and I am grateful for that. I do not need a replacement for grief.";
        }
        return "It feels unfinished. That is not a bad thing.";
    }

    static String checkInOpeningTrusted() {
        return "You are asking carefully. I appreciate that.";
    }

    static String feelingLineCommitted() {
        return "Exposed. Better than before, but exposed.";
    }

    static String feelingLineTrusted() {
        return "Tired, but not numb. That is an improvement.";
    }

    static String nextStepLine() {
        return "Follow the evidence before you trust anyone's old story, including mine.";
    }

    static String memoryOpening() {
        return "All right. Which part do you want to talk about?";
    }

    static String memoryReflection(String remembered) {
        String memory = remembered == null || remembered.isBlank() ? "that moment" : remembered.strip();
        return "I remember it. " + memory + " I am still sorting out what it changed.";
    }

    static String memorySharedResponse() {
        return "Good. I did not want to be the only one carrying it.";
    }

    static String memoryForwardResponse() {
        return "Forward, then. But not as if it never happened.";
    }

    static String outcomeMemoryRootLabel() {
        return "Can we talk about the choices we made in Snowrest?";
    }

    static String outcomeMemoryOpening() {
        return "Yes. Those choices still matter. Which one are you thinking about?";
    }

    static String outcomeMemoryLine(String outcomeKey, DialogueLibrary.DialogueContext context) {
        String tone = CompanionMemoryTone.describe(context.questOutcome(outcomeKey));
        return switch (outcomeKey) {
            case "vesper_first_root" -> "When you inspected the thawed root circle before judging it, " + tone
                    + ". That mattered because you did not treat fear as proof.";
            case "vesper_practical_care" -> "When the Snowrest road cracked open, " + tone
                    + ". You checked sap, witnesses, and risk before deciding. That is why I trusted you near the grove.";
            case "vesper_family_grove" -> "In my family grove, " + tone
                    + ". You treated the carved names as people, not as scenery for my grief.";
            case "vesper_buried_spring" -> "When the old druid's sealed spring came into the light, " + tone
                    + ". You helped me see that a choice can save lives and still leave a debt behind.";
            case "vesper_spring_return" -> "At the sealed spring, " + tone
                    + ". That told me what kind of future you were willing to protect.";
            case "vesper_oathstead_growth" -> "At Oathstead, " + tone
                    + ". Planting the cutting there made staying feel less like a trap.";
            default -> "I remember that choice. It grew quietly afterward, which is often how important things behave.";
        };
    }

    static String outcomeMemorySharedLine(String outcomeKey) {
        return switch (outcomeKey) {
            case "vesper_family_grove" -> "Then remember the names with me as people. I need that more than another explanation.";
            case "vesper_buried_spring" -> "Then remember that understanding Halwen is not the same as excusing him. I need help keeping those apart.";
            case "vesper_spring_return" -> "Then remember the spring as work we chose carefully, not as a miracle that fixes everything.";
            case "vesper_oathstead_growth" -> "Then remember that the first planting needed care every day. That kind of hope I can trust.";
            default -> "Then remember it honestly. I am tired of clean versions of hard things.";
        };
    }

    static String outcomeMemoryQuestionLine(String outcome) {
        if ("protect".equals(outcome)) {
            return "Perhaps I would protect less silently. Protection can become another sealed door if nobody is allowed to ask why it is locked.";
        }
        if ("accountability".equals(outcome)) {
            return "Perhaps I would leave more room for grief to explain itself. Consequences matter, but so does hearing why someone chose badly.";
        }
        if ("mercy".equals(outcome)) {
            return "Perhaps I would forgive more slowly. Forgiveness becomes dangerous when it asks everyone to stop remembering what happened.";
        }
        if ("truth".equals(outcome)) {
            return "Perhaps I would speak it softer. Truth does not become false because it is kind.";
        }
        return "Perhaps. I know I changed after that choice. That does not make the old choice meaningless.";
    }

    static String flirtChoice() {
        return "I like being near you.";
    }

    static String flirtRootLabel() {
        return "Can I say something personal?";
    }

    static String marriedFlirtOpening() {
        return "I still notice when you come close. I like that I still notice.";
    }

    static String flirtBoldChoice() {
        return "I want to be closer to you.";
    }

    static String flirtSoftLine() {
        return "Near is enough for now. I am not pulling away.";
    }

    static String flirtBoldLine() {
        return "Then be careful with me. I am not saying no.";
    }

    static String flirtBoundaryLine() {
        return "No. You asked gently. That matters.";
    }

    static String flirtAfterLine() {
        return "I know. Let us take it slowly and not turn it into a performance.";
    }

    static String romanceDateOpeningAtPlace(String place) {
        return "At " + place + ", I can almost stop listening for the next problem. Sit with me a while.";
    }

    static String romanceDateOpeningInvite() {
        return "Find me somewhere quiet. An inn table, Oathstead after rain, anywhere I do not have to be useful for a moment.";
    }

    static String romanceDateFlirtLine() {
        return "Say that softly. I believe you more when you are not trying to impress anyone.";
    }

    static String romanceDateSincereLine() {
        return "Then I will not rush to name it. I want to understand it before I make it a promise.";
    }

    static String romanceDatePlaceLine(String place) {
        return "Then we stay here for a while. No task. No emergency. Just this.";
    }

    static String romanceDateBoundaryLine() {
        return "Good. Slow is easier for me to trust.";
    }

    static String recruitId() {
        return "vesper";
    }

    static String recruitmentRootLabel() {
        return "Ask Vesper to come with you";
    }

    static String recruitmentOpening() {
        return "If you are asking me to travel with you, ask plainly. I do not like being managed into a decision.";
    }

    static String recruitmentRoadChoice() {
        return "Come with me. I could use your help out there.";
    }

    static String recruitmentOathsteadChoice() {
        return "Oathstead could use someone who understands living things.";
    }

    static String recruitmentRoadAcceptLine() {
        return "Then I come with you. By choice. If this becomes foolish, I reserve the right to say I warned you.";
    }

    static String recruitmentOathsteadAcceptLine() {
        return "Then I will help there. Food first, symbols later. A place should be useful before it tries to be inspiring.";
    }

    static String recruitmentWaitLine() {
        return "Good. I would rather decide without being pushed.";
    }

    static String milestoneTruthLine(int threshold) {
        return switch (threshold) {
            case 50 -> "You try not to harm what you do not understand. I noticed.";
            case 100 -> "I blamed myself for what happened to the grove. I am starting to believe that blame was not fair.";
            case 150 -> "If I stay beside you, it is because I choose to. That is the only kind of loyalty I trust.";
            case 180 -> "I care about you. I am saying it plainly because hiding it would be cowardice.";
            case 250 -> "A future with you sounds possible. Frightening, but possible.";
            default -> "The honest version is simple and therefore terrifying: you matter.";
        };
    }

    static String milestoneOpening(int threshold, DialogueLibrary.DialogueContext context) {
        return switch (threshold) {
            case 50 -> "I used to watch what you damaged without noticing. Lately, I have also seen what you stop to protect.";
            case 100 -> "There is something grief taught me, and I think it was wrong. If I say it aloud, I cannot keep using it as armor.";
            case 150 -> "I keep choosing to stand beside you. I want that named before either of us starts treating it as automatic.";
            case 180 -> context.sharedTableConversation()
                    ? "This table is too warm for evasions. I care about you, and I am tired of pretending I only mean that tactically."
                    : "Ask me somewhere quiet, and I will try to say the thing directly.";
            case 250 -> "I helped bring the spring back and still feared asking anything good to stay. With you, I have begun wondering what staying could look like.";
            default -> "Something has changed between us. I would rather talk about it than step around it.";
        };
    }

    static String milestoneWarmChoice(int threshold) {
        return switch (threshold) {
            case 50 -> "I will be more careful.";
            case 100 -> "Tell me what grief taught you.";
            case 150 -> "Stay because you choose to.";
            case 180 -> "Let me take you somewhere quiet.";
            case 250 -> "Imagine the future with me.";
            default -> "Let us talk honestly.";
        };
    }

    static String milestoneBoundaryChoice(int threshold) {
        return switch (threshold) {
            case 50 -> "Keep watching until you are sure.";
            case 100 -> "Only open what will not wound you again.";
            case 150 -> "I do not want you to feel trapped.";
            case 180 -> "We can let this grow slowly.";
            case 250 -> "Do not promise a future out of fear.";
            default -> "Careful is allowed.";
        };
    }

    static String milestoneWarmLine(int threshold) {
        return switch (threshold) {
            case 150 -> "Then I stay. Quietly, stubbornly, and because I choose it.";
            case 250 -> "Then we do not force the future. We make room for it and see if it holds.";
            default -> "Then let this be spoken plainly enough to survive tomorrow.";
        };
    }

    static String milestoneWarmLine(int threshold, DialogueLibrary.DialogueContext context) {
        return switch (threshold) {
            case 50 -> "Then start with small choices. Be careful when it would be easier not to be. I will notice.";
            case 100 -> "Grief told me every loss was my fault. You make that lie harder to believe.";
            case 150 -> "Then I stay. Not because I cannot leave. Because staying has become a choice I can live with.";
            case 180 -> context.sharedTableConversation()
                    ? "Then stay close. I do not want to pretend this is only the room being warm."
                    : "Then ask me again somewhere quiet. I want to answer without hiding behind the road.";
            case 250 -> context.married()
                    ? "Then we keep making the promise in useful ways: honest mornings, hard work, no pretending good things do not need care."
                    : "Then we make room for a future without making promises we are not ready to keep.";
            default -> "Then we handle this carefully and see what survives daylight.";
        };
    }

    static String milestoneBoundaryLine(int threshold) {
        return switch (threshold) {
            case 50 -> "Good. Trust that grows too fast usually breaks just as fast.";
            case 100 -> "Thank you. Some truths need air, but not a crowd. Not even a kind crowd.";
            case 150 -> "That is why I can offer loyalty at all. If it felt like being trapped, I would leave.";
            case 180 -> "Slow is not refusal. Slow is how I know I am not being dragged by fear.";
            case 250 -> "Then let the future stay unnamed tonight. It is not less real because we do not force it.";
            default -> "Careful is not cold. It is one way of staying kind.";
        };
    }

    static String milestoneAcceptLine(int threshold) {
        return switch (threshold) {
            case 50 -> "Then there is respect between us. Not deep trust yet, but real.";
            case 100 -> "Then I will speak more as I stop believing that every loss was my fault.";
            case 150 -> "Then we hold by choice. That is the only loyalty I trust.";
            case 180 -> "Then we let this become honest without forcing it.";
            case 250 -> "Then the future stays possible. Not trapped. Not hidden for its own good.";
            default -> "Then we keep talking about it honestly.";
        };
    }

    static String milestoneChangeLine(int threshold) {
        return switch (threshold) {
            case 50 -> "Yes. Naming it changes it. Sometimes that is useful.";
            case 100 -> "Yes. It changes the shape of the grief. I am learning that change is not always theft.";
            case 150 -> "Then we keep choosing it out loud. Loyalty should never become something we assume.";
            case 180 -> "Time is allowed. Wanting something does not mean we have to rush it.";
            case 250 -> "A future built slowly may last longer than one sworn too loudly.";
            default -> "Then we give the change room.";
        };
    }

    static String valuesTrustLine() {
        return "You check what is actually happening before you act. I trust that more than speeches.";
    }

    static String valuesTrustLine(DialogueLibrary.DialogueContext context) {
        if (!context.questOutcome("vesper_spring_return").isBlank()) {
            return "You stood at the spring seal and did not mistake speed for courage. That changed how I hear your promises.";
        }
        if (!context.questOutcome("vesper_family_grove").isBlank()) {
            return "You saw the carved names as people, not scenery for my grief. That matters more than I expected.";
        }
        return "You keep checking whether something can still be saved. That matters to me.";
    }

    static String valuesWorryLine() {
        return "That you will move too quickly because you want the hopeful answer to be true.";
    }

    static String valuesRequestLine() {
        return "When something looks lost, look once more before you decide it is gone.";
    }

    static String sharedTableStayLineCommitted() {
        return "Good. I would like a quiet moment with you and no emergency attached.";
    }

    static String sharedTableRequestLine(DialogueLibrary.DialogueContext context) {
        if (!context.questOutcome("vesper_oathstead_growth").isBlank()) {
            return "Tonight I only ask this: if Oathstead starts treating the snowroot like a banner, remind them it needs water before meaning.";
        }
        if (!context.questOutcome("vesper_buried_spring").isBlank()) {
            return "Tonight I only ask this: if I begin defending Halwen too quickly, remind me that understanding the burial does not repair it.";
        }
        return "";
    }

    static String sharedTableOpening(int relationship, DialogueLibrary.DialogueContext context, String place) {
        if (!context.questOutcome("vesper_oathstead_growth").isBlank() && relationship >= 150) {
            return "At " + place
                    + ", I keep expecting someone to need me. No one does right now. Sit with me while I learn what to do with that.";
        }
        if (!context.questOutcome("vesper_spring_return").isBlank() && relationship >= 120) {
            return place + " is loud with ordinary life. After the unsealed spring, ordinary feels better than I expected.";
        }
        if (!context.questOutcome("vesper_family_grove").isBlank() && relationship >= 100) {
            return "At " + place + ", nobody knows the Snowroot names unless I choose to speak them. That makes the quiet easier.";
        }
        if (relationship >= 150) {
            return "At " + place + ", I can sit still for a while. Stay if you want to.";
        }
        if (relationship >= 60) {
            return place + " makes people talk more honestly than they do on the road. I may use that.";
        }
        return place + " is warmer than the road. That is useful, even if I do not fully trust it.";
    }

    static String sharedTableStayLine(int relationship, DialogueLibrary.DialogueContext context) {
        if (!context.questOutcome("vesper_oathstead_growth").isBlank()) {
            return "Good. The cutting can survive one evening without me checking it. I would like to find out whether I can do the same.";
        }
        if (!context.questOutcome("vesper_spring_return").isBlank() && relationship >= 120) {
            return "Good. The spring is running without my hands in the water. I can sit still for a while.";
        }
        if (!context.questOutcome("vesper_family_grove").isBlank() && relationship >= 120) {
            return "Good. I have spent enough time with old names tonight. I would rather sit with someone living.";
        }
        if (relationship >= 150) {
            return sharedTableStayLineCommitted();
        }
        return "A while, then. No grand confessions. Just enough quiet to prove we can survive it.";
    }

    static String epilogueLine(int relationship, DialogueLibrary.DialogueContext context) {
        if (context.epilogueReady()) {
            if (!context.questOutcome("vesper_oathstead_growth").isBlank()) {
                return "After the crisis, the Oathstead snowroot became part of daily life. People watered it, argued over it, and learned to care for it without making it a legend.";
            }
            if (!context.questOutcome("vesper_spring_return").isBlank()) {
                return "After the crisis, the Snowroot spring did not become perfect. Some mornings it ran clear, some mornings it needed work. I learned that was still worth saving.";
            }
            return "The crisis ended, but it left work behind. I prefer that to an ending that pretends nothing was broken.";
        }
        if (!context.questOutcome("vesper_spring_return").isBlank() && relationship >= 150) {
            return "After the spring seal cracked, I started imagining a future that does not bury good things just to keep them safe. It frightens me less when you are in it.";
        }
        if (!context.questOutcome("vesper_buried_spring").isBlank() && relationship >= 120) {
            return "After Halwen, I understand how people can call fear a duty and hurt others with it. I am trying not to do that myself.";
        }
        if (relationship >= 150) {
            return "After all this? I try not to name the future too often. But I have started imagining one.";
        }
        return "After all this, we count who survived and try not to lie about what it cost.";
    }

    static String opinionAboutPlayer(DialogueLibrary.DialogueContext context) {
        if (context.married()) {
            return "You feel like home and trouble at once. I chose both.";
        }
        if (context.romanced()) {
            return "You make danger feel less lonely. I am still deciding whether to forgive you for that.";
        }
        return "You are not always gentle. But you come back to what needs care.";
    }

    static String opinionAboutVesperFrom(String speaker) {
        return switch (speaker) {
            case "SERAPHINE" -> "Vesper says little because she is deciding what is true before she speaks.";
            case "MAERA" -> "Vesper knows what families leave unsaid. She notices omissions better than most witnesses.";
            case "CASSIA" -> "Vesper is patient with injured land and impatient with careless people. I understand both.";
            case "LYRA" -> "Vesper has seen living things survive badly and still deserve care.";
            case "SAMIR" -> "Vesper listens before she acts. That is rarer than it should be.";
            case "ARIA" -> "Vesper is not unfriendly. She is deciding whether a conversation is honest enough to enter.";
            case "RAFIQ" -> "Vesper can make a careless person feel personally accountable to a shrub.";
            case "CALDER" -> "Vesper knows small damage can become serious if everyone ignores it.";
            default -> "Vesper is guarded, practical, and more caring than she wants strangers to know.";
        };
    }

    static String opinionAbout(String subject) {
        return switch (subject) {
            case "SERAPHINE" -> "Seraphine is sharper than she looks, and she already looks sharp.";
            case "MAERA" -> "Maera wants the record corrected. I understand that more than I expected.";
            case "CASSIA" -> "Cassia holds herself together because people once needed her to. I respect that.";
            case "LYRA" -> "Lyra keeps choosing care even when care is exhausting.";
            case "SAMIR" -> "Samir asks hard questions and still wants the answer to be kind.";
            case "ARIA" -> "Aria notices exits before anyone else notices the room.";
            case "RAFIQ" -> "Rafiq jokes when the truth gets too close. That does not mean he misses it.";
            case "CALDER" -> "Calder respects weight. That is close to wisdom.";
            default -> "I do not know them well enough to answer fairly.";
        };
    }

    static String loyalOpening() {
        return "Hello. I am still here, which means I chose to be.";
    }

    static String trustedOpening() {
        return "Hello. You know enough now that I can answer you plainly.";
    }

    static String friendOpening() {
        return "Hello. You came back. I am getting used to that.";
    }

    static String friendlyOpening() {
        return "Hello. You have helped more than I expected.";
    }

    static String acquaintanceOpening() {
        return "Hello. You have been careful so far. That is a good start.";
    }

    static String guardedOpening() {
        return "Hello. If you are here about the root circle, say so. I would rather not guess.";
    }
}
