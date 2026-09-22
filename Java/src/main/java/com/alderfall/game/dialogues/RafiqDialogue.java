package com.alderfall.game;

final class RafiqDialogue {
    private RafiqDialogue() {
    }

    static CompanionDialogueProfile profile() {
        return new CompanionDialogueProfile(
                "Rafiq",
                "a debt with a blade",
                "Who wants you dead?",
                "If anyone asks, I am handling a debt. If anyone armed asks, you have never met me.",
                "Who wants you dead?",
                "A patron with silk gloves, a duelist with better cheekbones than judgment, and one ledger I should have burned.",
                "I have run from creditors, duelists, and my own better sense. I am tired. Ask me to stay.",
                "Yes. Magnificently unwise, emotionally ruinous, and yes.",
                "I usually flirt to avoid honesty. This is the part where honesty wins, inconveniently."
        );
    }

    static String characterBeat(DialogueLibrary.DialogueIntent intent) {
        return switch (intent) {
            case QUEST_ROOT, QUEST_CLARIFY, QUEST_PERSONAL -> "Rafiq tests a smile, then lets the dangerous truth go first.";
            case QUEST_PRACTICAL, NEXT_STEP, QUEST_WARNING -> "Rafiq gives the answer quickly, before style can get in the way of survival.";
            case OPINION, VALUES, TRUST -> "Rafiq lets charm stand aside long enough for honesty to be seen.";
            case ROMANCE, FUTURE, HOME -> "Rafiq's humor thins into something warmer and far less practiced.";
            default -> "Rafiq almost reaches for a joke, then thinks better of hiding there.";
        };
    }

    static String intentAcknowledgement(DialogueLibrary.DialogueIntent intent) {
        return switch (intent) {
            case CONCERN -> "You are inviting sincerity. Reckless. I will attempt not to ruin it immediately.";
            case OPINION -> "If you want my opinion, I can make it charming or true. Today I will risk true.";
            case ROMANCE -> "The honest version of my attention. Bold, dangerous, well dressed.";
            case QUEST_CHALLENGE -> "You are calling the bluff. Excellent. I was getting tired of holding it alone.";
            default -> "";
        };
    }
    static String helpResponse() {
        return "Keep me honest when charm would be easier. I realize this is a cruel request.";
    }

    static String questCommitLabel() {
        return "I will help you survive the truth.";
    }

    static String questPersonalCommitLabel() {
        return "Then I will stay honest when charming would be easier.";
    }

    static String questPracticalCommitLabel() {
        return "Point me at the danger with manners.";
    }

    static String questAcceptedLine() {
        return "Good. If I start joking too quickly, assume the truth is close and ugly.";
    }

    static String chapterAcceptanceLine(String focus) {
        return "Good. Keep me honest if I start making it charming. " + focus;
    }

    static String chapterClarifyLine(String focus) {
        return "Charm stopped paying the debt. Now the receipt is ugly. " + focus;
    }

    static String chapterPersonalLine(String title) {
        return "Because " + title + " is where the joke starts costing more than the debt. I am tired of surviving by making the truth entertaining.";
    }

    static String defaultObjectiveLine() {
        return "If it looks theatrical, assume someone expected us to admire it.";
    }

    static String objectiveVoiceWarning() {
        return "And if I would be tempted to make a joke, assume the knife is close.";
    }

    static String supportResponse() {
        return "Then help me survive the truth without making it dull.";
    }

    static String warningResponse() {
        return "The danger is the elegant solution. Elegant solutions get people stabbed politely.";
    }

    static String chapterWarningLine() {
        return "Beware the elegant answer. It usually arrives well dressed and carrying a knife.";
    }

    static String searchFocusClose() {
        return "before charm makes the lie easier to applaud.";
    }

    static String gatherFocusClose() {
        return "because the truth needs something less slippery than my phrasing.";
    }

    static String defeatFocusClose() {
        return "and noticing who expected blood to clean up the story.";
    }

    static String progressRootGreeting() {
        return "The debt is beginning to name its collectors. Terrible manners. Useful.";
    }

    static String readyRootGreeting() {
        return "You found the proof. I am choosing not to make a joke before it cuts me.";
    }

    static String progressLine() {
        return "Excellent. The story is becoming inconvenient for someone other than me.";
    }

    static String originLine() {
        return "A debt with a blade is still a debt, just more honest about the ending.";
    }

    static String concernLine() {
        return "Charm can buy time. It cannot pay the debt forever.";
    }

    static String woundLine() {
        return "I bought time for someone I loved and let everyone call it vanity. It was easier to wear.";
    }

    static String practicalClose() {
        return "Bring back the proof before I improve the story for entertainment.";
    }

    static String marriedGreeting() {
        return "There you are. I have been terribly composed in your absence. A tragedy.";
    }

    static String romancedGreeting() {
        return "Ah. My favorite complication has arrived.";
    }

    static String oathsteadNeedLine() {
        return "Better drainage, worse music, and a corner where second chances are not announced too loudly.";
    }

    static String nextStepLine() {
        return "Choose the useful danger over the dramatic one. I am trying this new discipline.";
    }

    static String memoryOpening() {
        return "Ah, memory. My most theatrical and least obedient colleague.";
    }

    static String memoryReflection(String remembered) {
        return "I remember: " + remembered + " I made several excellent remarks internally and at least one useful decision.";
    }

    static String memorySharedResponse() {
        return "Excellent. If memory must misbehave, at least it has company.";
    }

    static String memoryForwardResponse() {
        return "Forward, preferably with fewer dramatic injuries. I am flexible.";
    }

    static String opinionAboutPlayer(DialogueLibrary.DialogueContext context) {
        return "You make sincerity look survivable. I resent the example and may follow it.";
    }

    static String flirtChoice() {
        return "Flirt with me before I become responsible.";
    }

    static String flirtRootLabel() {
        return "Flirt with me before you behave.";
    }

    static String flirtBoldChoice() {
        return "You are unfairly pretty when you pretend to be sensible.";
    }

    static String flirtSoftLine() {
        return "Near is dangerous. Luckily, I have always believed in useful danger.";
    }

    static String flirtBoldLine() {
        return "At last, someone appreciates my commitment to irresponsible beauty.";
    }

    static String flirtBoundaryLine() {
        return "Too much? From you? Alarming concept. Still, I like being asked.";
    }

    static String flirtAfterLine() {
        return "I know now, and I am going to be insufferable about it in moderation.";
    }

    static String romanceDateFlirtLine() {
        return "At last, a battlefield suited to my talents. Continue.";
    }

    static String romanceDateSincereLine() {
        return "Then I will be sincere for the length of one drink. Possibly two, if you are devastating.";
    }

    static String romanceDateBoundaryLine() {
        return "Gentle. Terrifyingly mature. I will attempt not to ruin it with charm.";
    }

    static String loyalOpening() {
        return "I have stopped rehearsing my exit whenever you speak. Alarming. Meaningful.";
    }

    static String trustedOpening() {
        return "I could lie charmingly. I find I do not want to. Very inconvenient.";
    }

    static String friendOpening() {
        return "There you are. I had almost achieved responsible solitude.";
    }

    static String friendlyOpening() {
        return "You continue to be entertainingly useful. Please do not become smug.";
    }

    static String acquaintanceOpening() {
        return "This is a promising arrangement of danger and curiosity. I approve conditionally.";
    }

    static String guardedOpening() {
        return "If this is about my debts, take a number. If it is about survival, speak quickly.";
    }

    static String recruitId() {
        return "rafiq";
    }

    static String recruitmentRootLabel() {
        return "Ask Rafiq to join the party";
    }

    static String recruitmentOpening() {
        return "Ah. The formal invitation. Try to make it sound daring. I am allergic to administrative affection.";
    }

    static String recruitmentRoadChoice() {
        return "Join me. I could use your trouble.";
    }

    static String recruitmentOathsteadChoice() {
        return "Oathstead needs a second chance with style.";
    }

    static String recruitmentRoadAcceptLine() {
        return "Then I join you. History will say you needed brilliance. I will allow a footnote about mutual trust.";
    }

    static String recruitmentOathsteadAcceptLine() {
        return "A corner for second chances, fewer creditors, and people too busy surviving to ask about my best lies. Excellent.";
    }

    static String recruitmentWaitLine() {
        return "Wise. Let anticipation improve my entrance.";
    }

    static String practicalDetailStageOne() {
        return "Start with the clue someone expected me to laugh off.";
    }

    static String readyLine() {
        return "Show me before I pretend I am ready. I may even tell the truth first.";
    }

    static String afterQuestStageOne() {
        return "First debt marker exposed. I dislike how relieved I am.";
    }

    static String checkInOpeningTrusted() {
        return "You ask with the tragic confidence of someone expecting honesty from me.";
    }

    static String feelingLineCommitted() {
        return "Tragically sincere. I am enduring it with style.";
    }

    static String sharedTableStayLineCommitted() {
        return "At last, an audience for my restraint. I will be magnificent and mostly quiet.";
    }

    static String oathsteadAssignedGreeting(String station) {
        return "The " + station + " has survived my standards so far. Heroic little structure.";
    }

    static String oathsteadGreeting() {
        return "Oathstead remains muddy, sincere, and alarmingly difficult to mock.";
    }

    static String marriedFlirtOpening() {
        return "Marriage has done nothing to improve my resistance to you. Tragic.";
    }

    static String romanceDateOpeningAtPlace(String place) {
        return place + " is almost worthy of us. I will forgive its flaws if you sit close enough.";
    }

    static String romanceDateOpeningInvite() {
        return "Invite me properly at an inn, and I will pretend not to have been waiting for it.";
    }

    static String romanceDatePlaceLine(String place) {
        return "Then " + place + " becomes historic. I recommend a plaque and another drink.";
    }

    static String milestoneTruthLine(int threshold) {
        return switch (threshold) {
            case 50 -> "You are becoming difficult to dismiss. Very rude. Possibly admirable.";
            case 100 -> "I made charm out of panic. It worked so well I forgot where I put the honest man.";
            case 150 -> "I have escaped finer rooms than this feeling. Somehow I am still here, making the alarming choice to be reliable.";
            case 180 -> "I could make a joke. A devastating one. Instead I will say the disastrous truth: I want you to choose me when no one is watching.";
            case 250 -> "A future with you is scandalously domestic: good wine, fewer creditors, and someone laughing before I finish the lie.";
            default -> "The honest version is simple and therefore terrifying: you matter.";
        };
    }

    static String milestoneWarmLine(int threshold) {
        return switch (threshold) {
            case 150 -> "Then witness my finest trick: sincerity without an escape route.";
            case 250 -> "Then I will try domestic courage. I expect applause.";
            default -> "Then let this be spoken plainly enough to survive tomorrow.";
        };
    }

    static String valuesTrustLine() {
        return "You make second chances look less ridiculous. I remain offended and grateful.";
    }

    static String valuesWorryLine() {
        return "That you will become necessary to everyone and never ask who holds you.";
    }

    static String valuesRequestLine() {
        return "When I make pain charming, do not applaud too quickly.";
    }

    static String oathsteadHomeLine(DialogueLibrary.DialogueContext context) {
        return "It feels muddy, earnest, and dangerously sincere. I am adapting with heroic grace.";
    }

    static String opinionAbout(String subject) {
        return switch (subject) {
                    case "SERAPHINE" -> "Seraphine could rob a courthouse and make the courthouse apologize.";
                    case "MAERA" -> "Maera makes treason sound well organized. Attractive quality in a scholar.";
                    case "CASSIA" -> "Cassia's glare could reinforce a gate. I am careful where I admire it.";
                    case "LYRA" -> "Lyra saves lives with terrifying competence and no appreciation for dramatic bleeding.";
                    case "SAMIR" -> "Samir is proof that holiness can have excellent posture and troubling questions.";
                    case "ARIA" -> "Aria distrusts applause. I am trying to understand this medical condition.";
                    case "VESPER" -> VesperDialogue.opinionAboutVesperFrom("RAFIQ");
                    case "CALDER" -> "Calder is what stone would be if it learned disappointment and loyalty.";
                    default -> "They improve the room, usually by making me look subtler.";
                };
    }

    static String feelingLineTrusted() {
        return "Dramatically fine. Which is to say: not fine, but well dressed.";
    }

}
