package com.alderfall.game;

final class SamirDialogue {
    private SamirDialogue() {
    }

    static CompanionDialogueProfile profile() {
        return new CompanionDialogueProfile(
                "Samir",
                "difficult light",
                "Why is the reliquary burning?",
                "The reliquary flame is eating its own shadow. That is either a miracle with poor manners or a warning with teeth.",
                "What did the light take from you?",
                "Certainty. Then pride. Then the comfort of thinking faith excuses fear. I am better without some of those things.",
                "I once mistook devotion for surrender. With you, devotion feels awake.",
                "Yes. Let the vow be light enough to carry and strong enough to return to.",
                "I have prayed for guidance. Annoyingly, the answer keeps wearing your face."
        );
    }

    static String characterBeat(DialogueLibrary.DialogueIntent intent) {
        return switch (intent) {
            case QUEST_ROOT, QUEST_CLARIFY, QUEST_PERSONAL -> "Samir lets the question pass through silence before giving it shape.";
            case QUEST_PRACTICAL, NEXT_STEP, QUEST_WARNING -> "Samir's voice steadies, light turned toward the work instead of the wound.";
            case OPINION, VALUES, TRUST -> "Samir answers without making faith do the thinking for him.";
            case ROMANCE, FUTURE, HOME -> "Samir looks at you as if warmth itself has become a question worth honoring.";
            default -> "Samir lets the silence breathe before he answers.";
        };
    }

    static String intentAcknowledgement(DialogueLibrary.DialogueIntent intent) {
        return switch (intent) {
            case QUEST_CLARIFY -> "The shadow behind the light. That is where truth usually waits.";
            case VALUES -> "You ask how I see you. I will answer without making faith do the work for me.";
            case TRUST -> "Trust should not kneel. It should look up and choose.";
            case ROMANCE -> "Warmth without worship. That is rarer than it should be.";
            default -> "";
        };
    }
    static String helpResponse() {
        return "Carry the question with me. Do not polish it into certainty before we know what it costs.";
    }

    static String questCommitLabel() {
        return "I will carry the question with you.";
    }

    static String questPersonalCommitLabel() {
        return "Then I will not force certainty where grief is speaking.";
    }

    static String questPracticalCommitLabel() {
        return "Show me where the light turns wrong.";
    }

    static String questAcceptedLine() {
        return "Good. Then we carry the question without worshipping the first answer that glows.";
    }

    static String chapterAcceptanceLine(String focus) {
        return "Good. Carry the question with me. " + focus;
    }

    static String chapterClarifyLine(String focus) {
        return "The light was taught to command instead of reveal. I need to see where it turns wrong. " + focus;
    }

    static String chapterPersonalLine(String title) {
        return "Because " + title + " asks whether my faith was ever mine, or only a polished chain I learned to call devotion.";
    }

    static String defaultObjectiveLine() {
        return "Follow the sign, then check the shadow beside it.";
    }

    static String objectiveVoiceWarning() {
        return "If certainty arrives too quickly, make it wait outside.";
    }

    static String supportResponse() {
        return "Then carry the question with me without trying to turn it into certainty.";
    }

    static String warningResponse() {
        return "The danger is mistaking bright signs for honest ones.";
    }

    static String chapterWarningLine() {
        return "Do not mistake brightness for honesty. Bad light loves familiar shapes.";
    }

    static String searchFocusClose() {
        return "before the wrong light claims it was always holy.";
    }

    static String gatherFocusClose() {
        return "because faith should touch what it claims to understand.";
    }

    static String defeatFocusClose() {
        return "and refusing to let force call itself revelation.";
    }

    static String progressRootGreeting() {
        return "The light has moved, and so has the shadow behind it.";
    }

    static String readyRootGreeting() {
        return "The sign is here. Now I have to decide what faith does with evidence.";
    }

    static String progressLine() {
        return "If the evidence troubles the old lesson, trust the trouble.";
    }

    static String originLine() {
        return "A holy flame can warm a room or teach people to kneel. I am learning the difference.";
    }

    static String concernLine() {
        return "Light that forbids questions has already begun casting the wrong shadow.";
    }

    static String woundLine() {
        return "My family grief was dressed as doctrine until I almost mistook silence for faith.";
    }

    static String practicalClose() {
        return "Bring back what still feels true after doubt touches it.";
    }

    static String marriedGreeting() {
        return "There you are. The room feels less like waiting when you enter it.";
    }

    static String romancedGreeting() {
        return "You come in like ordinary light. That is harder to resist than miracles.";
    }

    static String oathsteadNeedLine() {
        return "A light people can gather around without being ordered to kneel.";
    }

    static String nextStepLine() {
        return "Find the light that can survive questions. Leave the obedient flames behind.";
    }

    static String memoryOpening() {
        return "The past is not sacred because it happened. It matters because it still asks things of us.";
    }

    static String memoryReflection(String remembered) {
        return "I remember: " + remembered + " It changed the shape of the light around us, whether we admitted it or not.";
    }

    static String memorySharedResponse() {
        return "Two witnesses make a steadier flame.";
    }

    static String memoryForwardResponse() {
        return "Forward, carrying enough light to know why.";
    }

    static String opinionAboutPlayer(DialogueLibrary.DialogueContext context) {
        return "You ask for light without demanding obedience from it. That is rarer than doctrine admits.";
    }

    static String flirtChoice() {
        return "You are brighter than my better judgment.";
    }

    static String flirtRootLabel() {
        return "You make ordinary light difficult.";
    }

    static String flirtBoldChoice() {
        return "If this is temptation, I am becoming fond of the theology.";
    }

    static String flirtSoftLine() {
        return "Then stay near without making it a sermon. I would like that better.";
    }

    static String flirtBoldLine() {
        return "I should object on principle. I am discovering several principles are negotiable.";
    }

    static String flirtBoundaryLine() {
        return "No. I would rather name the boundary than pretend holiness means silence.";
    }

    static String flirtAfterLine() {
        return "Then let it be true without demanding a miracle to justify it.";
    }

    static String romanceDateFlirtLine() {
        return "Do not make me call this temptation. I am trying to have a better theology about your eyes.";
    }

    static String romanceDateSincereLine() {
        return "Then I will let the quiet be enough. No sermon. No sign. Just your hand near mine.";
    }

    static String romanceDateBoundaryLine() {
        return "Then we let it be honest before we ask it to be holy.";
    }

    static String loyalOpening() {
        return "I trust your questions near my faith. That is not a small permission.";
    }

    static String trustedOpening() {
        return "Some doubts are safer when spoken near you.";
    }

    static String friendOpening() {
        return "You keep standing near the questions instead of fleeing the answer. I notice.";
    }

    static String friendlyOpening() {
        return "You ask better questions than most people who want simple answers.";
    }

    static String acquaintanceOpening() {
        return "Your questions have not insulted the light so far. Continue carefully.";
    }

    static String guardedOpening() {
        return "Ask carefully. Some doors open because they should. Some because they are bait.";
    }

    static String recruitId() {
        return "samir";
    }

    static String recruitmentRootLabel() {
        return "Ask Samir to walk beside you";
    }

    static String recruitmentOpening() {
        return "If you ask me to come, do not ask for obedience. Ask for witness, questions, and light that argues back.";
    }

    static String recruitmentRoadChoice() {
        return "Walk beside me, questions and all.";
    }

    static String recruitmentOathsteadChoice() {
        return "Oathstead needs light that can be questioned.";
    }

    static String recruitmentRoadAcceptLine() {
        return "Then I walk beside you. I will bring light, doubt, and the habit of asking whether victory still has clean hands.";
    }

    static String recruitmentOathsteadAcceptLine() {
        return "Then I will keep a lamp there, but not a throne. Let people gather around light without kneeling to it.";
    }

    static String recruitmentWaitLine() {
        return "Then the question waits in honest light. That is better than forcing an answer in a shadow.";
    }

    static String practicalDetailStageOne() {
        return "Watch where the light behaves like it is hiding something.";
    }

    static String readyLine() {
        return "Let me see it plainly. If it burns away certainty, so be it.";
    }

    static String afterQuestStageOne() {
        return "First shadow named. The light can stop pretending it stands alone.";
    }

    static String checkInOpeningTrusted() {
        return "You ask gently enough that I cannot hide behind doctrine.";
    }

    static String feelingLineCommitted() {
        return "Bright in places I used to keep locked. Frightening, but not unwelcome.";
    }

    static String sharedTableStayLineCommitted() {
        return "A shared silence can be prayer if no one tries to own it.";
    }

    static String oathsteadAssignedGreeting(String station) {
        return "The " + station + " has ordinary work and honest shadows. I find that useful.";
    }

    static String oathsteadGreeting() {
        return "Oathstead has light in ordinary places. I am learning to trust that.";
    }

    static String marriedFlirtOpening() {
        return "You still make vows feel less like weight and more like light.";
    }

    static String romanceDateOpeningAtPlace(String place) {
        return "The light in " + place + " is ordinary. That makes what I feel near you harder to blame on miracles.";
    }

    static String romanceDateOpeningInvite() {
        return "Take me somewhere with ordinary light. I want to see whether this still feels holy without ceremony.";
    }

    static String romanceDatePlaceLine(String place) {
        return "Then let " + place + " hold the ordinary miracle of two people choosing not to hurry.";
    }

    static String milestoneTruthLine(int threshold) {
        return switch (threshold) {
            case 50 -> "You have not demanded faith from me. You have earned attention. That is the first clean step.";
            case 100 -> "I have preached light while fearing what it would show in me. You make hiding feel less holy.";
            case 150 -> "I used to think loyalty meant kneeling to the brightest thing in the room. Now I think it means walking beside a light that asks questions.";
            case 180 -> "I have prayed for clean answers. This is not clean. It is warm, difficult, and more honest than certainty.";
            case 250 -> "A future with you is a lamp in a window, questions at breakfast, and faith that survives being spoken plainly.";
            default -> "The honest version is simple and therefore terrifying: you matter.";
        };
    }

    static String milestoneWarmLine(int threshold) {
        return switch (threshold) {
            case 150 -> "Then I walk beside you, eyes open, light unforced.";
            case 250 -> "Then let it be a vow that can stand in daylight.";
            default -> "Then let this be spoken plainly enough to survive tomorrow.";
        };
    }

    static String valuesTrustLine() {
        return "You do not demand that light obey you. You ask what it reveals.";
    }

    static String valuesWorryLine() {
        return "That victory will tempt you to stop asking whether the light is honest.";
    }

    static String valuesRequestLine() {
        return "When faith sounds too clean, ask what it had to wash away.";
    }

    static String oathsteadHomeLine(DialogueLibrary.DialogueContext context) {
        return "It feels like a lamp someone is allowed to question.";
    }

    static String opinionAbout(String subject) {
        return switch (subject) {
                    case "SERAPHINE" -> "Seraphine taught herself freedom with sharp tools. There is holiness in refusing chains.";
                    case "MAERA" -> "Maera seeks truth without asking whether it is permitted. I admire that sin.";
                    case "CASSIA" -> "Cassia knows obedience can wound. That knowledge may yet become light.";
                    case "LYRA" -> "Lyra's mercy has calluses. That makes it trustworthy.";
                    case "ARIA" -> "Aria survives by reading shadows. I hope one day she trusts dawn too.";
                    case "VESPER" -> VesperDialogue.opinionAboutVesperFrom("SAMIR");
                    case "RAFIQ" -> "Rafiq makes confession wear perfume. Still confession.";
                    case "CALDER" -> "Calder's faith is in foundations. Mine could learn from it.";
                    default -> "They carry a light I do not fully understand.";
                };
    }

    static String feelingLineTrusted() {
        return "Unsteady, but not dimmed.";
    }

}
