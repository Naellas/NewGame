package com.alderfall.game;

final class LyraDialogue {
    private LyraDialogue() {
    }

    static CompanionDialogueProfile profile() {
        return new CompanionDialogueProfile(
                "Lyra",
                "mercy work",
                "Who needs help at the clinic?",
                "The clinic is out of clean cloth, good sleep, and patient gods. Bring any two and I will improvise the third.",
                "Who taught you mercy costs this much?",
                "Every fever bed. Every person I could not save. Mercy is not soft. It is work that keeps standing after hope sits down.",
                "I have held too many hands at endings. I want yours at beginnings too.",
                "Yes. No grand cure, no perfect promise. Just us, choosing care every day.",
                "When you leave, I count supplies twice. When you return, I breathe once."
        );
    }

    static String characterBeat(DialogueLibrary.DialogueIntent intent) {
        return switch (intent) {
            case QUEST_ROOT, QUEST_CLARIFY, QUEST_PERSONAL -> "Lyra listens first, the way a healer listens for breath under pain.";
            case QUEST_PRACTICAL, NEXT_STEP, NEED -> "Lyra counts what can be done before grief can make the room too large.";
            case CONCERN, FEELING, MEMORY -> "Lyra lets the gentleness stay, even when the answer does not.";
            case ROMANCE, FUTURE, HOME -> "Lyra's expression warms with the relief of being asked to rest, not only mend.";
            default -> "Lyra studies your face first, gentle as a healer checking an old wound.";
        };
    }

    static String intentAcknowledgement(DialogueLibrary.DialogueIntent intent) {
        return switch (intent) {
            case CONCERN -> "You are checking the healer for wounds. That is irritatingly fair.";
            case NEED -> "Need, before I collapse into being useful. I heard that.";
            case FUTURE -> "A future where care is allowed to rest. I want that answer too.";
            case MEMORY -> "If we touch that memory, we do it gently and all the way awake.";
            default -> "";
        };
    }
    static String helpResponse() {
        return "Help the people who will bleed if we delay. Everything else is decoration.";
    }

    static String questCommitLabel() {
        return "I will help before more people get hurt.";
    }

    static String questPersonalCommitLabel() {
        return "Then I will treat the wound, not admire it.";
    }

    static String questPracticalCommitLabel() {
        return "Tell me who gets hurt if I wait.";
    }

    static String questAcceptedLine() {
        return "Good. Then we move before someone else becomes a lesson no one deserved.";
    }

    static String chapterAcceptanceLine(String focus) {
        return "Good. We move before care arrives too late. " + focus;
    }

    static String chapterClarifyLine(String focus) {
        return "People die while heroes debate symbols. We start with care. " + focus;
    }

    static String chapterPersonalLine(String title) {
        return "Because " + title + " is not abstract to me. Every delay has a face, a cot, a pulse I may or may not get back.";
    }

    static String defaultObjectiveLine() {
        return "Start where someone living can still be helped.";
    }

    static String objectiveVoiceWarning() {
        return "If a choice appears, protect whoever can still be protected.";
    }

    static String supportResponse() {
        return "Then keep your hands clean and your attention kinder than your pride.";
    }

    static String warningResponse() {
        return "The danger is moving so fast that we miss who is already bleeding.";
    }

    static String chapterWarningLine() {
        return "Do not move so fast that the living become background. Check who still needs help.";
    }

    static String searchFocusClose() {
        return "before another living detail becomes only a name on a cot.";
    }

    static String gatherFocusClose() {
        return "because care needs supplies before it earns speeches.";
    }

    static String defeatFocusClose() {
        return "and ending the harm before it creates more patients.";
    }

    static String progressRootGreeting() {
        return "Something has shifted. Check the living before counting the proof.";
    }

    static String readyRootGreeting() {
        return "You brought the piece that matters. Let us not make grief wait longer.";
    }

    static String progressLine() {
        return "Good. Now make sure the next step still protects the living.";
    }

    static String originLine() {
        return "A clinic teaches you that mercy without movement becomes furniture.";
    }

    static String concernLine() {
        return "Supplies are not small. A clean bandage can be the difference between grief and tomorrow.";
    }

    static String woundLine() {
        return "I waited for permission once. Someone died while obedience kept its hands clean.";
    }

    static String practicalClose() {
        return "Bring back what helps us prevent the next wound.";
    }

    static String marriedGreeting() {
        return "There you are. Sit if you can. I am trying to learn that welcome is also care.";
    }

    static String romancedGreeting() {
        return "There you are. I was not waiting. I was... pausing with intent.";
    }

    static String oathsteadNeedLine() {
        return "Clean water, spare cloth, and fewer heroes pretending infection respects bravery.";
    }

    static String nextStepLine() {
        return "Supplies first. Heroics after clean water and bandages.";
    }

    static String memoryOpening() {
        return "Careful. Remembering can heal, but it can also reopen what was only resting.";
    }

    static String memoryReflection(String remembered) {
        return "I remember: " + remembered + " I keep it gently. Some proof belongs beside the heart, not in a ledger.";
    }

    static String memorySharedResponse() {
        return "Then it is a little less lonely in the remembering.";
    }

    static String memoryForwardResponse() {
        return "Forward, and we check the wound before calling it healed.";
    }

    static String opinionAboutPlayer(DialogueLibrary.DialogueContext context) {
        return "You get hurt often enough to worry me and keep helping often enough to make the worry worthwhile.";
    }

    static String flirtChoice() {
        return "Your smile is terrible for my discipline.";
    }

    static String flirtRootLabel() {
        return "I like seeing you smile.";
    }

    static String flirtBoldChoice() {
        return "I think you are bad for my pulse.";
    }

    static String flirtSoftLine() {
        return "Near is a very good medicine when administered by someone I trust.";
    }

    static String flirtBoldLine() {
        return "Unfair. I am trained for fevers, not for you saying things like that.";
    }

    static String flirtBoundaryLine() {
        return "No. Tenderness is safer when both people are allowed to breathe.";
    }

    static String flirtAfterLine() {
        return "I know. And because you said it plainly, I can keep it somewhere soft.";
    }

    static String romanceDateFlirtLine() {
        return "That is unfair. I know several remedies for fever and none for you.";
    }

    static String romanceDateSincereLine() {
        return "Then I will put down the work for a moment. Not because it is done. Because you asked me to rest with you.";
    }

    static String romanceDateBoundaryLine() {
        return "Good. Tender things bruise when hurried, and I am tired of treating preventable wounds.";
    }

    static String loyalOpening() {
        return "I trust you with the part of care that keeps working after the room goes quiet.";
    }

    static String trustedOpening() {
        return "I can be tired in front of you without feeling like I have failed the room.";
    }

    static String friendOpening() {
        return "You came back with all your limbs and at least one thoughtful expression. Promising.";
    }

    static String friendlyOpening() {
        return "You look like trouble with decent intentions. I can work with half of that.";
    }

    static String acquaintanceOpening() {
        return "You are less useless than expected. I am choosing to treat that as a medical improvement.";
    }

    static String guardedOpening() {
        return "If you are here to help, good. If you are here to hover, stand where no one bleeds on you.";
    }

    static String recruitId() {
        return "lyra";
    }

    static String recruitmentRootLabel() {
        return "Ask Lyra to travel with you";
    }

    static String recruitmentOpening() {
        return "Traveling with you means more wounds, worse weather, and probably fewer sensible rest breaks. I am listening anyway.";
    }

    static String recruitmentRoadChoice() {
        return "Come with me. We will keep people alive together.";
    }

    static String recruitmentOathsteadChoice() {
        return "Oathstead needs care before crisis.";
    }

    static String recruitmentRoadAcceptLine() {
        return "Then I come with you. Someone has to count bandages, interrupt heroics, and make sure mercy arrives before the funeral.";
    }

    static String recruitmentOathsteadAcceptLine() {
        return "Then I will help make care arrive before panic. Build me shelves, clean water, and a place where heroes wash their hands.";
    }

    static String recruitmentWaitLine() {
        return "Thank you. Consent is healthier when it has room to breathe.";
    }

    static String practicalDetailStageOne() {
        return "Look for what a careful hand would never leave behind.";
    }

    static String readyLine() {
        return "Show me carefully. Proof can bruise too, if handled like a victory.";
    }

    static String afterQuestStageOne() {
        return "First wound traced. Now we keep it from becoming someone else's.";
    }

    static String checkInOpeningTrusted() {
        return "You remembered that healers bruise too. Sensible. Inconveniently kind.";
    }

    static String feelingLineCommitted() {
        return "Tired in the hands. Better in the heart than I expected.";
    }

    static String sharedTableStayLineCommitted() {
        return "Stay, then. I am tired of every kindness having a departure planned inside it.";
    }

    static String oathsteadAssignedGreeting(String station) {
        return "The " + station + " needs clean hands, calmer voices, and fewer heroic entrances. I am working on two.";
    }

    static String oathsteadGreeting() {
        return "Oathstead smells of smoke, linen, wet boots, and people trying again.";
    }

    static String marriedFlirtOpening() {
        return "If this is how you ask me to rest, I may diagnose it as effective.";
    }

    static String romanceDateOpeningAtPlace(String place) {
        return place + " is warm enough, quiet enough, and not currently bleeding. A rare medical recommendation.";
    }

    static String romanceDateOpeningInvite() {
        return "An inn. A bench. Any place where no one asks me to triage the evening.";
    }

    static String romanceDatePlaceLine(String place) {
        return "Then we rest here. If the world needs us, it can wait until the cup is empty.";
    }

    static String milestoneTruthLine(int threshold) {
        return switch (threshold) {
            case 50 -> "I have begun believing your concern may survive inconvenience. That is rarer than kindness.";
            case 100 -> "I know how to hold everyone together. I do not know how to let someone hold me without counting it as failure.";
            case 150 -> "I have spent my life staying because people hurt. I stay with you because care can be more than triage.";
            case 180 -> "When you are hurt, my hands know what to do. When you smile at me, I lose the whole medical tradition.";
            case 250 -> "A future with you is clean water, mended sleeves, laughter from the next room, and care that is allowed to rest.";
            default -> "The honest version is simple and therefore terrifying: you matter.";
        };
    }

    static String milestoneWarmLine(int threshold) {
        return switch (threshold) {
            case 150 -> "Then let care be something we choose, not only something crisis demands.";
            case 250 -> "Then we make room for beginnings, not only recoveries.";
            default -> "Then let this be spoken plainly enough to survive tomorrow.";
        };
    }

    static String milestoneOpening(int threshold, DialogueLibrary.DialogueContext context) {
        return switch (threshold) {
            case 50 -> "I have started believing your concern can survive inconvenience. That is rarer than kindness in a crisis.";
            case 100 -> "I know how to hold everyone together. I do not know how to let someone hold me without counting it as failure.";
            case 150 -> "I have spent my life staying because people hurt. I stay with you because care can be more than triage.";
            case 180 -> context.sharedTableConversation()
                        ? "When you are hurt, my hands know what to do. When you smile at me here, I lose the whole medical tradition."
                        : "Ask me somewhere no one needs saving for one hour. I want to learn what care says when it is not working.";
            case 250 -> "A future with you is clean water, mended sleeves, laughter from the next room, and care that is allowed to rest.";
            default -> "Something has changed between us. It deserves better than being stepped around.";
        };
    }

    static String valuesTrustLine() {
        return "You return to people after the danger passes. That is when care becomes real.";
    }

    static String valuesWorryLine() {
        return "That you will spend yourself like supplies no one has to replace.";
    }

    static String valuesRequestLine() {
        return "When I start caring for everyone except myself, interrupt me.";
    }

    static String outcomeMemoryRootLabel() {
        return "Can we talk about the healing choices?";
    }

    static String outcomeMemoryOpening() {
        return "Healing was never only salves and stitches. It was choosing who gets care before panic makes the decision for us.";
    }

    static String outcomeMemorySharedLine(String outcomeKey) {
        return "Then remember the cost and the care together. I do not trust mercy that edits out either one.";
    }

    static String outcomeMemoryLine(String outcomeKey, DialogueLibrary.DialogueContext context) {
        String tone = CompanionMemoryTone.describe(context.questOutcome(outcomeKey));
        return switch (outcomeKey) {
                    case "lyra_first_triage" -> "At the first triage, " + tone
                            + ". Care had to become judgment there. I still dislike that, which is probably healthy.";
                    case "lyra_names_on_cot" -> "With the clean-labeled crates, " + tone
                            + ". Patients become easier to neglect when harm learns to look official.";
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

    static String outcomeMemoryQuestionLine(String outcome) {
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

    static String opinionAbout(String subject) {
        return switch (subject) {
                    case "SERAPHINE" -> "Seraphine has old scars dressed beautifully. I do not confuse that for healed.";
                    case "MAERA" -> "Maera forgets to eat when truth is nearby. Someone should supervise scholarship.";
                    case "CASSIA" -> "Cassia carries blame like armor. Heavy. Familiar.";
                    case "SAMIR" -> "Samir is learning that light can comfort without commanding.";
                    case "ARIA" -> "Aria says she is fine too quickly. That is a symptom.";
                    case "VESPER" -> VesperDialogue.opinionAboutVesperFrom("LYRA");
                    case "RAFIQ" -> "Rafiq laughs near pain. I keep bandages ready anyway.";
                    case "CALDER" -> "Calder treats broken things with respect. People included, when he remembers.";
                    default -> "They need rest more than advice.";
                };
    }

    static String feelingLineTrusted() {
        return "Tired in the hands. Better because someone noticed.";
    }

    static String milestoneWarmLine(int threshold, DialogueLibrary.DialogueContext context) {
        return switch (threshold) {
            case 100 -> "Then I will let the care point toward me for once. I may complain. Continue anyway.";
            case 150 -> "Then let care be something we choose, not only something crisis demands.";
            case 250 -> "Then we make room for beginnings, not only recoveries.";
            default -> "Then let this be spoken plainly enough to survive tomorrow.";
        };
    }

}
