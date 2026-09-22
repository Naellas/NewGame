package com.alderfall.game;

final class CalderDialogue {
    private CalderDialogue() {
    }

    static CompanionDialogueProfile profile() {
        return new CompanionDialogueProfile(
                "Calder",
                "a failing bridge",
                "Why is the bridge failing?",
                "Bridges fail in pieces first. A loose peg, a lazy inspection, a person pretending water is patient.",
                "Who did the bridge take from you?",
                "Someone I should have reached faster. Since then I count ropes, beams, and regrets in the same voice.",
                "I trust stone because it tells you when it cracks. I trust you because you stayed long enough to hear it.",
                "Aye. We build it daily, then. No pretty nonsense. Just a promise with foundations.",
                "Love is a structure. I am terrified by how sound this one feels."
        );
    }

    static String characterBeat(DialogueLibrary.DialogueIntent intent) {
        return switch (intent) {
            case QUEST_ROOT, QUEST_CLARIFY, QUEST_PERSONAL -> "Calder sets the truth down carefully, like weight on tested timber.";
            case QUEST_PRACTICAL, NEXT_STEP, QUEST_WARNING -> "Calder's answer becomes a repair plan, plain enough to trust.";
            case OPINION, VALUES, TRUST -> "Calder inspects the question like a brace that has to hold.";
            case ROMANCE, FUTURE, HOME -> "Calder's voice roughens around the feeling, but it does not move away.";
            default -> "Calder sets the answer down carefully, like weight on tested timber.";
        };
    }

    static String intentAcknowledgement(DialogueLibrary.DialogueIntent intent) {
        return switch (intent) {
            case QUEST_PRACTICAL -> "What needs doing. Good. A sound answer starts there.";
            case NEED -> "Needs are easier to meet when named before the beam cracks.";
            case TRUST -> "Trust is load-bearing. Best to inspect it honestly.";
            case FUTURE -> "You ask about after. That means we plan for weather, not just sunlight.";
            default -> "";
        };
    }
    static String helpResponse() {
        return "Put your hands under the weight before making speeches about repair.";
    }

    static String questCommitLabel() {
        return "I will show up with both hands.";
    }

    static String questPersonalCommitLabel() {
        return "Then I will help carry the weight, not praise it.";
    }

    static String questPracticalCommitLabel() {
        return "Show me where the structure is failing.";
    }

    static String questAcceptedLine() {
        return "Good. Bring your hands, not just your agreement. Weight does not care about intentions.";
    }

    static String chapterAcceptanceLine(String focus) {
        return "Good. Bring your hands, not just your agreement. " + focus;
    }

    static String chapterClarifyLine(String focus) {
        return "Something failed before the collapse became visible. Good craft listens early. " + focus;
    }

    static String chapterPersonalLine(String title) {
        return "Because " + title + " puts weight exactly where I pretend I can carry it alone. I know bridges. I am still learning people.";
    }

    static String defaultObjectiveLine() {
        return "Check the strain first, then the excuse built around it.";
    }

    static String objectiveVoiceWarning() {
        return "Weight tells the truth before people do.";
    }

    static String supportResponse() {
        return "Then put your weight under the beam before praising the bridge.";
    }

    static String warningResponse() {
        return "The danger is trusting the visible crack and ignoring the pressure behind it.";
    }

    static String chapterWarningLine() {
        return "Do not fix only the visible crack. Find where the weight is really shifting.";
    }

    static String searchFocusClose() {
        return "before the crack hides under fresh paint.";
    }

    static String gatherFocusClose() {
        return "because weight in the hand keeps judgment honest.";
    }

    static String defeatFocusClose() {
        return "and stopping the pressure before it brings the whole span down.";
    }

    static String progressRootGreeting() {
        return "The first strain showed. Now we find what else is carrying it.";
    }

    static String readyRootGreeting() {
        return "You brought the weight back. Set it down where I can see it.";
    }

    static String progressLine() {
        return "A single crack is warning. Two cracks are instruction.";
    }

    static String originLine() {
        return "A bridge fails long before it falls. People do too.";
    }

    static String concernLine() {
        return "Maintenance is care with a hammer. Neglect is harm with better excuses.";
    }

    static String woundLine() {
        return "I signed off on work that failed. The river did the killing, but I gave it a place to start.";
    }

    static String practicalClose() {
        return "Bring back something solid enough to bear weight.";
    }

    static String marriedGreeting() {
        return "There you are. Roof held, table held, promise held. Good start.";
    }

    static String romancedGreeting() {
        return "You are here. Good. The day was leaning oddly without you.";
    }

    static String oathsteadNeedLine() {
        return "Foundations, braces, nails, and people willing to maintain what they love.";
    }

    static String nextStepLine() {
        return "Check what holds. Roads, gates, promises. Same principle.";
    }

    static String memoryOpening() {
        return "A remembered thing can hold a person steady if it was set straight.";
    }

    static String memoryReflection(String remembered) {
        return "I remember: " + remembered + " It became part of the structure. Small brace, real weight.";
    }

    static String memorySharedResponse() {
        return "Good. Things held by two hands are harder to drop.";
    }

    static String memoryForwardResponse() {
        return "Forward, with maintenance. Always maintenance.";
    }

    static String opinionAboutPlayer(DialogueLibrary.DialogueContext context) {
        return "You are not finished. Good. Finished things cannot be repaired.";
    }

    static String flirtChoice() {
        return "You are becoming part of the foundation.";
    }

    static String flirtRootLabel() {
        return "You feel like somewhere solid.";
    }

    static String flirtBoldChoice() {
        return "I keep thinking about what it would mean to come home to you.";
    }

    static String flirtSoftLine() {
        return "Near is honest. It puts weight where words cannot dodge it.";
    }

    static String flirtBoldLine() {
        return "That lands harder than you think. Fortunately, I am built for weight.";
    }

    static String flirtBoundaryLine() {
        return "No. A checked load holds better.";
    }

    static String flirtAfterLine() {
        return "I know. That gives the day a better foundation than it had.";
    }

    static String romanceDateFlirtLine() {
        return "That was not subtle. Good. Subtle things get missed in bad weather.";
    }

    static String romanceDateSincereLine() {
        return "Then we give the hour a foundation: truth, patience, and no pretending this is nothing.";
    }

    static String romanceDateBoundaryLine() {
        return "Slow is not weak. Slow is how foundations learn the ground.";
    }

    static String loyalOpening() {
        return "I trust your weight on the beam. That is higher praise than it sounds.";
    }

    static String trustedOpening() {
        return "Some cracks should be named before weather tests them. You may hear this one.";
    }

    static String friendOpening() {
        return "You show up before the supports fail. Sensible habit.";
    }

    static String friendlyOpening() {
        return "You are useful in ways that do not immediately collapse. Good.";
    }

    static String acquaintanceOpening() {
        return "You have not leaned wrong on the structure. That earns another conversation.";
    }

    static String guardedOpening() {
        return "Say what you need. I dislike guessing when things may already be cracking.";
    }

    static String recruitId() {
        return "calder";
    }

    static String recruitmentRootLabel() {
        return "Ask Calder to help build the road";
    }

    static String recruitmentOpening() {
        return "You are asking whether I will put my weight behind your road. Good. Ask it like you know weight matters.";
    }

    static String recruitmentRoadChoice() {
        return "Join me. Help me make the road hold.";
    }

    static String recruitmentOathsteadChoice() {
        return "Oathstead needs hands that know repairs.";
    }

    static String recruitmentRoadAcceptLine() {
        return "Then I am with you. Roads fail where people stop maintaining them. I will bring the hammer and the uncomfortable reminders.";
    }

    static String recruitmentOathsteadAcceptLine() {
        return "Then give me tools, bad plans to correct, and a roof that admits it needs work. I can do something with that.";
    }

    static String recruitmentWaitLine() {
        return "Good. Measure twice, ask once, and do not rush the join.";
    }

    static String practicalDetailStageOne() {
        return "Check the first crack before arguing about the collapse.";
    }

    static String readyLine() {
        return "Set it down. We test the weight before deciding what it means.";
    }

    static String afterQuestStageOne() {
        return "First crack found. The structure has begun telling the truth.";
    }

    static String checkInOpeningTrusted() {
        return "You ask like a person checking the supports after rain. I respect that.";
    }

    static String feelingLineCommitted() {
        return "Sound enough. Some cracks. None spreading today.";
    }

    static String sharedTableStayLineCommitted() {
        return "Good. Sit. Even beams need to stop bearing weight sometimes.";
    }

    static String oathsteadAssignedGreeting(String station) {
        return "The " + station + " is uneven, overworked, and worth maintaining. Familiar virtues.";
    }

    static String oathsteadGreeting() {
        return "Oathstead still leans toward standing. That counts for more than it sounds.";
    }

    static String marriedFlirtOpening() {
        return "You look at me like the roof will hold. I am still learning how good that feels.";
    }

    static String romanceDateOpeningAtPlace(String place) {
        return place + " has bad chairs and a sound roof. Good enough for truth if we sit carefully.";
    }

    static String romanceDateOpeningInvite() {
        return "A roof, a table, and time enough that neither of us calls it maintenance. That would do.";
    }

    static String romanceDatePlaceLine(String place) {
        return "Then we sit, and we let the roof do its job while we do ours.";
    }

    static String milestoneTruthLine(int threshold) {
        return switch (threshold) {
            case 50 -> "You hold under load. Not perfectly. Nothing good does at first.";
            case 100 -> "I trust structures because people failed me. You are making that distinction inconvenient.";
            case 150 -> "A bridge proves itself under weight. So do people. You have weight, and I am still here.";
            case 180 -> "I trust what holds after weather. This feeling has taken weather. I am beginning to respect its joinery.";
            case 250 -> "A future with you is a roof checked before rain, a table scarred by use, and promises maintained before they crack.";
            default -> "The honest version is simple and therefore terrifying: you matter.";
        };
    }

    static String milestoneWarmLine(int threshold) {
        return switch (threshold) {
            case 150 -> "Then we brace it properly and trust it under load.";
            case 250 -> "Then we maintain it. Daily. No pretty ruin, no neglected vow.";
            default -> "Then let this be spoken plainly enough to survive tomorrow.";
        };
    }

    static String valuesTrustLine() {
        return "You maintain what you claim to love. Not perfectly. Enough to matter.";
    }

    static String valuesWorryLine() {
        return "That you will build too fast and forget every promise needs maintenance.";
    }

    static String valuesRequestLine() {
        return "When I call something sound, ask when we inspect it again.";
    }

    static String oathsteadHomeLine(DialogueLibrary.DialogueContext context) {
        return "It feels uneven. Repairable. Most worthwhile things are.";
    }

    static String opinionAbout(String subject) {
        return switch (subject) {
                    case "SERAPHINE" -> "Seraphine knows where a thing is weak before anyone admits it is load-bearing.";
                    case "MAERA" -> "Maera measures roads in truth. Good material, if brittle.";
                    case "CASSIA" -> "Cassia holds. Sometimes too hard. Still, holding matters.";
                    case "LYRA" -> "Lyra repairs people who pretend they are not structures.";
                    case "SAMIR" -> "Samir keeps checking whether the light is honest. Good habit.";
                    case "ARIA" -> "Aria sees bad footing before the rest of us find mud.";
                    case "VESPER" -> VesperDialogue.opinionAboutVesperFrom("CALDER");
                    case "RAFIQ" -> "Rafiq is a bridge with too much paint. Still might hold.";
                    default -> "They have weight. Worth accounting for.";
                };
    }

    static String feelingLineTrusted() {
        return "A little strained. Still holding.";
    }

}
