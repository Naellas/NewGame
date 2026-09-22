package com.alderfall.game;

final class AriaDialogue {
    private AriaDialogue() {
    }

    static CompanionDialogueProfile profile() {
        return new CompanionDialogueProfile(
                "Aria",
                "a family trail",
                "What happened to your sister?",
                "An ambush site has tracks too neat to be real. I know bait when it puts on my sister's ribbon.",
                "What happened to your sister?",
                "She left through briars and debt, and every false trail I found afterward was too neat. Someone wanted me chasing the wrong pain.",
                "I have slept under hedges and called it freedom. With you, a door sounds less like a trap.",
                "Yes. But if anyone calls me tamed, I am borrowing your best knife.",
                "I keep pretending this is just another trail. It is not. It is you."
        );
    }

    static String characterBeat(DialogueLibrary.DialogueIntent intent) {
        return switch (intent) {
            case QUEST_ROOT, QUEST_CLARIFY -> "Aria reads the road in her head before she gives you the part that matters.";
            case QUEST_PERSONAL, MEMORY -> "Aria's gaze shifts away from the exits and toward the old hurt.";
            case QUEST_PRACTICAL, NEXT_STEP, QUEST_WARNING -> "Aria's answer turns spare and useful, the way trail signs are useful.";
            case CONCERN, FEELING, NEED -> "Aria lets the silence stretch before deciding not to dodge the question.";
            case OPINION, VALUES, TRUST -> "Aria measures the truth carefully, then leaves it where you can see it.";
            case ROMANCE, FUTURE, HOME -> "Aria stays close enough that leaving would have to be a choice.";
            default -> "Aria gives you her attention without pretending attention is easy.";
        };
    }

    static String intentAcknowledgement(DialogueLibrary.DialogueIntent intent) {
        return switch (intent) {
            case CONCERN -> "I usually say yes before checking.";
            case QUEST_PERSONAL -> "You want the part of the trail I usually cover with leaves.";
            case QUEST_WARNING -> "Finally, a question with survival instincts.";
            case HOME -> "Staying is not a small word. I know every exit, so the answer matters.";
            case ROMANCE -> "Close without tactics. Suspicious.";
            default -> "";
        };
    }
    static String helpResponse() {
        return "Move quietly with me. Notice the false trail before grief starts running after it.";
    }

    static String questCommitLabel() {
        return "I know the road may be bait. I am still coming.";
    }

    static String questPersonalCommitLabel() {
        return "Then I will follow carefully, not loudly.";
    }

    static String questPracticalCommitLabel() {
        return "Show me the first false trail.";
    }

    static String questAcceptedLine() {
        return "Good. Say it back to yourself: the road may be bait. If you still come, come with your eyes open.";
    }

    static String chapterAcceptanceLine(String focus) {
        return "Good. The road may be bait, so read it twice. " + focus;
    }

    static String chapterClarifyLine(String focus) {
        return "This is a road problem first. Road problems kill quietly and call it accident. " + focus;
    }

    static String chapterPersonalLine(String title) {
        return "Because " + title + " keeps finding the old fear: if someone walks with me, will they help me choose the road or start choosing it for me?";
    }

    static String defaultObjectiveLine() {
        return "Read the ground before you trust the story above it.";
    }

    static String objectiveVoiceWarning() {
        return "Look twice at anything arranged to be found once.";
    }

    static String supportResponse() {
        return "Then move quietly with me and notice what the road hopes we miss.";
    }

    static String warningResponse() {
        return "The danger is following the trail they wanted us to find.";
    }

    static String chapterWarningLine() {
        return "Do not follow the trail meant for us. Look for the mark that does not want attention.";
    }

    static String searchFocusClose() {
        return "before the false trail teaches people to obey it.";
    }

    static String gatherFocusClose() {
        return "because hands remember what frightened witnesses forget.";
    }

    static String defeatFocusClose() {
        return "and reading the footprints it leaves when it stops pretending.";
    }

    static String progressRootGreeting() {
        return "The trail is answering. Do not trust it just because it speaks.";
    }

    static String readyRootGreeting() {
        return "You found the mark. Now we decide who was meant to follow it.";
    }

    static String progressLine() {
        return "The second sign matters more than the first; patterns do not lie as easily.";
    }

    static String originLine() {
        return "A trail can save you, trap you, or lead you back to the person you failed to follow.";
    }

    static String concernLine() {
        return "False trails are made by people who know what real grief will chase.";
    }

    static String woundLine() {
        return "I followed the wrong trail because it hurt less than admitting someone had shaped my fear.";
    }

    static String practicalClose() {
        return "Bring back a mark I can trust more than rumor.";
    }

    static String marriedGreeting() {
        return "There you are. I left the door in my mind open and pretended it was strategy.";
    }

    static String romancedGreeting() {
        return "You found me before I could decide whether I wanted finding. That is becoming familiar.";
    }

    static String oathsteadNeedLine() {
        return "Watchers on the roads, quiet signals, and someone checking the paths children use.";
    }

    static String nextStepLine() {
        return "Watch the trails people hope we are too hurried to notice.";
    }

    static String memoryOpening() {
        return "If this is sentiment, walk lightly. If it is useful, I am listening.";
    }

    static String memoryReflection(String remembered) {
        return "I remember: " + remembered + " I noticed who stayed, who flinched, and who pretended not to care.";
    }

    static String memorySharedResponse() {
        return "Fine. But if you make it ceremonial, I am leaving first.";
    }

    static String memoryForwardResponse() {
        return "Forward works. It keeps people from staring too long.";
    }

    static String opinionAboutPlayer(DialogueLibrary.DialogueContext context) {
        return "You keep walking into trouble like it owes you rent. Annoying. Useful. Occasionally brave.";
    }

    static String flirtChoice() {
        return "Stay close enough to be trouble.";
    }

    static String flirtRootLabel() {
        return "You keep making me look back.";
    }

    static String flirtBoldChoice() {
        return "You make danger look like a place I might stay.";
    }

    static String flirtSoftLine() {
        return "Near, but not trapping. You are learning.";
    }

    static String flirtBoldLine() {
        return "That road is trouble. I noticed you did not ask me to avoid it.";
    }

    static String flirtBoundaryLine() {
        return "No. You gave me an exit before I needed one. That matters.";
    }

    static String flirtAfterLine() {
        return "I know. I may even stop pretending I did not follow that sign willingly.";
    }

    static String romanceDateFlirtLine() {
        return "Trouble, then. But the kind I might walk toward on purpose.";
    }

    static String romanceDateSincereLine() {
        return "Then do not make a cage of it. Let it be a trail we both choose again tomorrow.";
    }

    static String romanceDateBoundaryLine() {
        return "Gentle, with exits. That I can understand.";
    }

    static String loyalOpening() {
        return "I keep finding roads back to you before I admit I chose them.";
    }

    static String trustedOpening() {
        return "There is a truth I would not hand to a stranger. You are no longer convenient enough to be one.";
    }

    static String friendOpening() {
        return "You have a habit of being present before I decide whether I need anyone present.";
    }

    static String friendlyOpening() {
        return "You notice traps before stepping into them. I am trying not to look impressed.";
    }

    static String acquaintanceOpening() {
        return "Our problems overlap. That is not trust yet, but it is a trail.";
    }

    static String guardedOpening() {
        return "You came armed with questions. I will decide which are dangerous.";
    }

    static String recruitId() {
        return "aria";
    }

    static String recruitmentRootLabel() {
        return "Ask Aria to scout with you";
    }

    static String recruitmentOpening() {
        return "Careful. Ask a scout to stay near you and she starts checking whether you understand exits.";
    }

    static String recruitmentRoadChoice() {
        return "Scout with me. Keep the road honest.";
    }

    static String recruitmentOathsteadChoice() {
        return "Oathstead needs roads watched by someone sharp.";
    }

    static String recruitmentRoadAcceptLine() {
        return "Then I scout with you. I will find the snares, mock the obvious paths, and come back when I said I would. Mostly.";
    }

    static String recruitmentOathsteadAcceptLine() {
        return "Then I will watch the roads in and out. Do not call it settling. Call it knowing where to return.";
    }

    static String recruitmentWaitLine() {
        return "Fine. I will keep pretending I did not appreciate the way you asked.";
    }

    static String practicalDetailStageOne() {
        return "Read the ground before you trust the story people tell above it.";
    }

    static String readyLine() {
        return "Show me the mark. Slowly. I want to know which trail it opens.";
    }

    static String afterQuestStageOne() {
        return "First trail cut free. Now I know the road was lying on purpose.";
    }

    static String checkInOpeningTrusted() {
        return "You keep asking before I can turn the answer into a joke about roads.";
    }

    static String feelingLineCommitted() {
        return "Like I might stay if no one says the word too loudly.";
    }

    static String sharedTableStayLineCommitted() {
        return "Fine. But if I relax, you are legally required not to mention it.";
    }

    static String oathsteadAssignedGreeting(String station) {
        return "The " + station + " gives me roads to watch and reasons to come back from them.";
    }

    static String oathsteadGreeting() {
        return "Oathstead has roads that return. I keep noticing.";
    }

    static String marriedFlirtOpening() {
        return "I know that look. I still check the exits, but I no longer plan to use them.";
    }

    static String romanceDateOpeningAtPlace(String place) {
        return place + " is crowded enough to vanish in and quiet enough to stay. I am choosing the second.";
    }

    static String romanceDateOpeningInvite() {
        return "Somewhere with exits and no speeches. If I stay anyway, you may take that as significant.";
    }

    static String romanceDatePlaceLine(String place) {
        return "Then I stay. Not trapped. Not cornered. Here.";
    }

    static String milestoneTruthLine(int threshold) {
        return switch (threshold) {
            case 50 -> "You watch the road better than most. You also listen when I say the road is lying.";
            case 100 -> "I learned to leave before anyone could choose it for me. Staying near you is making that habit clumsy.";
            case 150 -> "I know every exit from here. That is why it matters that I am still standing close enough for you to reach me.";
            case 180 -> "I want to stay near you when there is no tactical reason. Do you understand how suspicious that is?";
            case 250 -> "A future with you is a door I use more than once, a trail that comes back, and someone who knows I may still need the sky.";
            default -> "The honest version is simple and therefore terrifying: you matter.";
        };
    }

    static String milestoneWarmLine(int threshold) {
        return switch (threshold) {
            case 150 -> "Then do not block the exits. I will stay because I can leave.";
            case 250 -> "Then promise me windows. I can learn doors if there are windows.";
            default -> "Then let this be spoken plainly enough to survive tomorrow.";
        };
    }

    static String milestoneOpening(int threshold, DialogueLibrary.DialogueContext context) {
        return switch (threshold) {
            case 50 -> "You read warnings better now. More importantly, you listen when I say the road is lying.";
            case 100 -> "I learned to leave before anyone could choose it for me. Staying near you is making that habit clumsy.";
            case 150 -> "I know every exit from here. That is why it matters that I am still close enough for you to reach me.";
            case 180 -> context.sharedTableConversation()
                        ? "I want to stay near you when there is no tactical reason. Do you understand how suspicious that is?"
                        : "Ask me somewhere with exits and no speeches. If I stay anyway, you may take that as an answer.";
            case 250 -> "A future with you is a door I use more than once, a trail that comes back, and someone who knows I may still need the sky.";
            default -> "Something has changed between us. It deserves better than being stepped around.";
        };
    }

    static String valuesTrustLine() {
        return "You notice who gets left behind on the road. You do not always say it, but you notice.";
    }

    static String valuesWorryLine() {
        return "That you will walk into danger because people have started expecting you to.";
    }

    static String valuesRequestLine() {
        return "When I joke about leaving, hear the part that is checking whether I can stay.";
    }

    static String outcomeMemoryRootLabel() {
        return "Can we talk about the roadwatch choices?";
    }

    static String outcomeMemoryOpening() {
        return "Roads remember choices. So do scouts, even when they pretend they only remember tracks.";
    }

    static String outcomeMemorySharedLine(String outcomeKey) {
        return "Then remember where we stood and who got to walk away. Roads are made of those details.";
    }

    static String outcomeMemoryLine(String outcomeKey, DialogueLibrary.DialogueContext context) {
        String tone = CompanionMemoryTone.describe(context.questOutcome(outcomeKey));
        return switch (outcomeKey) {
                    case "aria_ambush_lesson" -> "After the ambush, " + tone
                            + ". You noticed the lesson without making me teach it twice. That is rarer than good aim.";
                    case "aria_false_trail_choice" -> "At the false trail, " + tone
                            + ". You did not call caution cowardice or hope a plan. That kept people breathing.";
                    case "aria_oathstead_roadmarks" -> "When we marked Oathstead's roads, " + tone
                            + ". You helped me build signs that protect people without making a cage of the road.";
                    case "aria_sister_truth" -> "At my sister's trail, " + tone
                            + ". You let hope and proof stand in the same clearing without forcing one to shoot the other.";
                    case "aria_oathstead_future" -> "At Oathstead, " + tone
                            + ". That was when a road back stopped sounding like surrender.";
                    default -> "I remember that choice. Trails do not end just because people stop looking.";
                };
    }

    static String outcomeMemoryQuestionLine(String outcome) {
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
                    if ("road_scouts".equals(outcome)) {
                        return "Perhaps I would teach the scouts the rude signs first. Polite roads get people killed.";
                    }
                    if ("hidden_markers".equals(outcome)) {
                        return "Perhaps I would mark one trail for children and one for people who think children do not notice roads.";
                    }
                    if ("counter_traps".equals(outcome)) {
                        return "Perhaps I would count the cost twice. Teeth are useful, but roads should not start enjoying them.";
                    }
                    if ("hope_named".equals(outcome)) {
                        return "Perhaps I would say her name sooner. Hope becomes less feral when it has somewhere honest to stand.";
                    }
                    if ("witness_protected".equals(outcome)) {
                        return "Perhaps I would thank you before making the joke. Do not look smug; I said perhaps.";
                    }
                    if ("debt_pursued".equals(outcome)) {
                        return "Perhaps I would sharpen the knife after sleeping. Debt collectors count on tired anger making mistakes.";
                    }
                    return "Maybe. Roads look different from the far side. That does not mean the old tracks were false.";
    }

    static String opinionAbout(String subject) {
        return switch (subject) {
                    case "SERAPHINE" -> "Seraphine smiles like a locked drawer. I like her.";
                    case "MAERA" -> "Maera would chase a forbidden map through a burning library and complain about the smoke damaging notes.";
                    case "CASSIA" -> "Cassia watches gates like they might apologize. I like her. From a distance.";
                    case "LYRA" -> "Lyra sees when people limp emotionally. Inconvenient talent.";
                    case "SAMIR" -> "Samir glows with questions. Hard to hide with, easy to trust.";
                    case "VESPER" -> VesperDialogue.opinionAboutVesperFrom("ARIA");
                    case "RAFIQ" -> "Rafiq is what happens when trouble learns choreography.";
                    case "CALDER" -> "Calder makes roads less likely to kill children. Hard to mock that.";
                    default -> "They are less predictable than tracks.";
                };
    }

    static String feelingLineTrusted() {
        return "Like I have been walking too long and only just noticed I can stop.";
    }

    static String milestoneWarmLine(int threshold, DialogueLibrary.DialogueContext context) {
        return switch (threshold) {
            case 100 -> "Then do not block the exits. I will stay because I can leave.";
            case 150 -> "Then do not make a ceremony of it. I am staying, which is ceremony enough.";
            case 250 -> "Then promise me windows. I can learn doors if there are windows.";
            default -> "Then let this be spoken plainly enough to survive tomorrow.";
        };
    }

}
