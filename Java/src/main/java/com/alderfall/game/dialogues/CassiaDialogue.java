package com.alderfall.game;

final class CassiaDialogue {
    private CassiaDialogue() {
    }

    static CompanionDialogueProfile profile() {
        return new CompanionDialogueProfile(
                "Cassia",
                "old orders",
                "What happened at the gate?",
                "The gate should have held. It did not. I have been arguing with that fact longer than is dignified.",
                "Whose order broke you?",
                "Mine, partly. Someone gave the command, but I obeyed it. That is the part honor keeps returning to me.",
                "I used to believe vows were walls. With you, they feel more like a hand at my back.",
                "Yes. I will stand beside you by choice, not command.",
                "I do not say this lightly: when the line breaks, I look for you first."
        );
    }

    static String characterBeat(DialogueLibrary.DialogueIntent intent) {
        return switch (intent) {
            case QUEST_ROOT, QUEST_CLARIFY, QUEST_PERSONAL -> "Cassia answers like someone holding a line others once abandoned.";
            case QUEST_PRACTICAL, NEXT_STEP, QUEST_WARNING -> "Cassia makes the answer practical before fear can start giving orders.";
            case OPINION, VALUES, TRUST -> "Cassia meets the question squarely, no salute to hide behind.";
            case ROMANCE, FUTURE, HOME -> "Cassia's posture stays disciplined, but her voice does not.";
            default -> "Cassia steadies her shoulders, but her answer is for you rather than the room.";
        };
    }

    static String intentAcknowledgement(DialogueLibrary.DialogueIntent intent) {
        return switch (intent) {
            case QUEST_PRACTICAL -> "The practical line, then. It keeps fear from giving orders.";
            case QUEST_WARNING -> "What breaks first. That is the soldier's question.";
            case TRUST -> "Trust is not a speech. It is where I stand when the line moves.";
            case FEELING -> "You want the answer under the armor. Fine.";
            default -> "";
        };
    }
    static String helpResponse() {
        return "Stand where the breach is, and do not dress fear as discipline. That will help.";
    }

    static String questCommitLabel() {
        return "I will stand where this breaks.";
    }

    static String questPersonalCommitLabel() {
        return "Then I will carry the cost, not just the order.";
    }

    static String questPracticalCommitLabel() {
        return "Tell me where the line needs holding.";
    }

    static String questAcceptedLine() {
        return "Good. Then we do this with discipline. Courage without attention is just noise in armor.";
    }

    static String chapterAcceptanceLine(String focus) {
        return "Good. Judgment first, noise never. " + focus;
    }

    static String chapterClarifyLine(String focus) {
        return "This is about who was protected, who was abandoned, and who gave the order. " + focus;
    }

    static String chapterPersonalLine(String title) {
        return "Because " + title + " touches the part of me that once mistook obedience for courage. I need facts, but I also need not to hide inside them.";
    }

    static String defaultObjectiveLine() {
        return "Go where the pressure is strongest and see what still holds.";
    }

    static String objectiveVoiceWarning() {
        return "Do not confuse a clean order with a worthy one.";
    }

    static String supportResponse() {
        return "Then stand where the work is, not where it looks impressive.";
    }

    static String warningResponse() {
        return "The danger is assuming a first breach is the only breach.";
    }

    static String chapterWarningLine() {
        return "Do not let discipline become a hiding place. Orders still need judgment.";
    }

    static String searchFocusClose() {
        return "before memory turns itself into an order no one questions.";
    }

    static String gatherFocusClose() {
        return "because proof has to stand after discipline stops speaking.";
    }

    static String defeatFocusClose() {
        return "and noticing whether fear is trying to command the line.";
    }

    static String progressRootGreeting() {
        return "We have movement. That does not mean safety; it means attention.";
    }

    static String readyRootGreeting() {
        return "You found what holds. Now we test whether I can hear it.";
    }

    static String progressLine() {
        return "Hold the line of facts. Do not let urgency move it.";
    }

    static String originLine() {
        return "A gate remembers every order that made it open or close.";
    }

    static String concernLine() {
        return "Duty without judgment is just obedience hoping no one counts the dead.";
    }

    static String woundLine() {
        return "I obeyed an order and people paid for it. Context matters. So does blame.";
    }

    static String practicalClose() {
        return "Bring back what can stand under questioning.";
    }

    static String marriedGreeting() {
        return "There you are. The line holds better when I know where you stand.";
    }

    static String romancedGreeting() {
        return "You are here. Good. I will pretend that did not change my breathing.";
    }

    static String oathsteadNeedLine() {
        return "A gate that opens for the right people and closes for the right reasons.";
    }

    static String nextStepLine() {
        return "Secure the road behind us before chasing glory ahead.";
    }

    static String memoryOpening() {
        return "Some moments should be named. Otherwise they become weight with no handle.";
    }

    static String memoryReflection(String remembered) {
        return "I remember: " + remembered + " It did not fix everything. Good memories rarely do. They give us footing.";
    }

    static String memorySharedResponse() {
        return "Good. I would rather not carry that one by myself.";
    }

    static String memoryForwardResponse() {
        return "Forward is allowed. Running is not required.";
    }

    static String opinionAboutPlayer(DialogueLibrary.DialogueContext context) {
        return "You bend under weight, then stand again. I trust that more than speeches.";
    }

    static String flirtChoice() {
        return "You make standing guard difficult.";
    }

    static String flirtRootLabel() {
        return "You are ruining my discipline.";
    }

    static String flirtBoldChoice() {
        return "Permission to be a terrible distraction?";
    }

    static String flirtSoftLine() {
        return "Then stand there a moment. I can guard the world badly for one breath.";
    }

    static String flirtBoldLine() {
        return "Permission granted, briefly. Abuse it and I will make you earn the next smile.";
    }

    static String flirtBoundaryLine() {
        return "No. But ask like that if you are unsure. Discipline applies to tenderness too.";
    }

    static String flirtAfterLine() {
        return "Then I will carry it. Quietly. Carefully. More gladly than I intended.";
    }

    static String romanceDateFlirtLine() {
        return "You are making my pulse tactically unhelpful. I am choosing not to object.";
    }

    static String romanceDateSincereLine() {
        return "Then I can stop scanning the horizon for one breath and let myself be found.";
    }

    static String romanceDateBoundaryLine() {
        return "Gentle does not mean uncertain. It means disciplined.";
    }

    static String loyalOpening() {
        return "If the line moves, I look for you before I look for orders.";
    }

    static String trustedOpening() {
        return "I can speak plainly with you. Do not make me regret discovering that.";
    }

    static String friendOpening() {
        return "You are present again. I am learning not to treat that as a tactical error.";
    }

    static String friendlyOpening() {
        return "You have not wasted my time yet. That is a better start than most.";
    }

    static String acquaintanceOpening() {
        return "You are standing near the same trouble. That earns clarity, not trust.";
    }

    static String guardedOpening() {
        return "Speak clearly. Half-truths waste time and get people killed.";
    }

    static String recruitId() {
        return "cassia";
    }

    static String recruitmentRootLabel() {
        return "Ask Cassia to stand with you";
    }

    static String recruitmentOpening() {
        return "You want me beside you beyond this one task. Say so directly. Orders ruined enough things; clarity will do better.";
    }

    static String recruitmentRoadChoice() {
        return "Stand with me on the road.";
    }

    static String recruitmentOathsteadChoice() {
        return "Oathstead needs someone who understands duty.";
    }

    static String recruitmentRoadAcceptLine() {
        return "Then I stand with you. Not by command. By name, by judgment, and because the road still has people worth defending.";
    }

    static String recruitmentOathsteadAcceptLine() {
        return "If Oathstead needs a line held for the right reasons, I will stand there. I may even learn to rest between alarms.";
    }

    static String recruitmentWaitLine() {
        return "Good. A choice made under pressure is too close to an order.";
    }

    static String practicalDetailStageOne() {
        return "Check the place duty failed before asking who failed it.";
    }

    static String readyLine() {
        return "Show me. I will not salute the truth until I have looked it in the face.";
    }

    static String afterQuestStageOne() {
        return "First breach named. That matters more than a clean report.";
    }

    static String checkInOpeningTrusted() {
        return "You ask after the person inside the armor. That is still not standard procedure.";
    }

    static String feelingLineCommitted() {
        return "Steady enough to stand. Honest enough to admit standing is not the same as healed.";
    }

    static String sharedTableStayLineCommitted() {
        return "A while is acceptable. Longer if no one calls it rest too loudly.";
    }

    static String oathsteadAssignedGreeting(String station) {
        return "The " + station + " is not a wall, but people lean on it. That is enough for my attention.";
    }

    static String oathsteadGreeting() {
        return "Oathstead is still standing. So are the people inside it. That is not nothing.";
    }

    static String marriedFlirtOpening() {
        return "I know that tone. It still disarms me more efficiently than any blade.";
    }

    static String romanceDateOpeningAtPlace(String place) {
        return "This corner of " + place + " has two exits, a tolerable sightline, and you. I am trying to notice the third thing most.";
    }

    static String romanceDateOpeningInvite() {
        return "Somewhere defensible, quiet, and not pretending to be a battlefield. I am told such places exist.";
    }

    static String romanceDatePlaceLine(String place) {
        return "Then I stand down. Briefly. Do not make me regret discovering I can.";
    }

    static String milestoneTruthLine(int threshold) {
        return switch (threshold) {
            case 50 -> "I respect that you bend and return. People who never bend usually break other people first.";
            case 100 -> "I have survived by being useful. I do not know who I am when someone stays after the use is done.";
            case 150 -> "I was trained to hold lines for people who never learned my face. This time I know exactly whose shoulder I am guarding.";
            case 180 -> "I do not have a soldier's word for wanting to be looked for after the battle. I only know I want it from you.";
            case 250 -> "A future with you is a gate I do not have to guard alone, and a home that does not require orders to be worth defending.";
            default -> "The honest version is simple and therefore terrifying: you matter.";
        };
    }

    static String milestoneWarmLine(int threshold) {
        return switch (threshold) {
            case 150 -> "Then I stand with you by name, not by command.";
            case 250 -> "Then we build somewhere worth returning to after the line holds.";
            default -> "Then let this be spoken plainly enough to survive tomorrow.";
        };
    }

    static String milestoneOpening(int threshold, DialogueLibrary.DialogueContext context) {
        return switch (threshold) {
            case 50 -> "I respect that you stand after bending. People who refuse to bend usually make everyone else pay for it.";
            case 100 -> "I survived by becoming useful. I do not know who I am when someone stays after the use is done.";
            case 150 -> "I was trained to hold lines for people who never learned my face. This time I know whose shoulder I am guarding.";
            case 180 -> context.sharedTableConversation()
                        ? "No orders, no line to hold, and I still want you near. I do not have a soldier's word for that."
                        : "Ask me somewhere defensible and quiet. If I stop watching the door, you may take that as significant.";
            case 250 -> "A future with you is a gate I do not have to guard alone, and a home that does not require orders to be worth defending.";
            default -> "Something has changed between us. It deserves better than being stepped around.";
        };
    }

    static String valuesTrustLine() {
        return "You stand where standing costs something. I trust weight better than speeches.";
    }

    static String valuesWorryLine() {
        return "That you will mistake endurance for healing and call the wound useful.";
    }

    static String valuesRequestLine() {
        return "When I say I can hold, ask whether I should have to.";
    }

    static String outcomeMemoryRootLabel() {
        return "Can we talk about the duty choices?";
    }

    static String outcomeMemoryOpening() {
        return "Duty kept changing shape under us. I want to remember where it protected people and where it only protected orders.";
    }

    static String outcomeMemorySharedLine(String outcomeKey) {
        return "Then remember that we chose with weight on us. That does not excuse everything. It does mean the choice was real.";
    }

    static String outcomeMemoryLine(String outcomeKey, DialogueLibrary.DialogueContext context) {
        String tone = CompanionMemoryTone.describe(context.questOutcome(outcomeKey));
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

    static String outcomeMemoryQuestionLine(String outcome) {
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

    static String opinionAbout(String subject) {
        return switch (subject) {
                    case "SERAPHINE" -> "Seraphine hides wounds behind elegance. The blade is honest, at least.";
                    case "MAERA" -> "Maera questions orders written by the dead. Good.";
                    case "LYRA" -> "Lyra holds the line after the line breaks.";
                    case "SAMIR" -> "Samir has courage. Not loud courage. Better kind.";
                    case "ARIA" -> "Aria moves like someone who expects betrayal. I hope she learns otherwise.";
                    case "VESPER" -> VesperDialogue.opinionAboutVesperFrom("CASSIA");
                    case "RAFIQ" -> "Rafiq jokes when cornered. That is either courage or bad armor. Possibly both.";
                    case "CALDER" -> "Calder understands that strength without maintenance fails.";
                    default -> "They can stand if the day asks it.";
                };
    }

    static String feelingLineTrusted() {
        return "Bruised under the discipline. Still standing.";
    }

    static String milestoneWarmLine(int threshold, DialogueLibrary.DialogueContext context) {
        return switch (threshold) {
            case 100 -> "Then I will say it plainly. I am tired of being useful and calling that a self.";
            case 150 -> "Then I stand with you by name, not by command.";
            case 250 -> "Then we build somewhere worth returning to after the line holds.";
            default -> "Then let this be spoken plainly enough to survive tomorrow.";
        };
    }

}
