package com.alderfall.game;

final class SeraphineDialogue {
    private SeraphineDialogue() {
    }

    static CompanionDialogueProfile profile() {
        return new CompanionDialogueProfile(
                "Seraphine",
                "a stolen contract",
                "What are you really stealing back?",
                "Before you do anything heroic, know this: the drawer was already open when I found it. Probably.",
                "What are you stealing?",
                "Technically? My own name. Less technically, a paper trail that says I belong to someone who forgot I learned knives before etiquette.",
                "I spent years burning contracts that called themselves promises. If you ask for one now, make it ours and make it honest.",
                "No ownership. No chains. No clever clauses hidden under candle wax. Yes, I will marry you.",
                "I think about you when you are gone. It is inconvenient, obvious, and apparently terminal."
        );
    }

    static String characterBeat(DialogueLibrary.DialogueIntent intent) {
        return switch (intent) {
            case QUEST_ROOT, QUEST_CLARIFY, QUEST_PERSONAL -> "Seraphine studies the wording as if the lie might be hiding in the punctuation.";
            case QUEST_PRACTICAL, NEXT_STEP -> "Seraphine's answer sharpens into instructions, all velvet stripped from the edge.";
            case OPINION, VALUES, TRUST -> "Seraphine lets the truth sit on the table without dressing it up.";
            case ROMANCE, FUTURE, HOME -> "Seraphine allows the pause to become almost tender before she speaks.";
            default -> "Seraphine weighs your wording before she lets it pass.";
        };
    }

    static String intentAcknowledgement(DialogueLibrary.DialogueIntent intent) {
        return switch (intent) {
            case VALUES -> "If you want my honest measure of you, I will not hide it under pretty language.";
            case OPINION -> "Since you brought a name to the table, I will not pretend neutrality is the same as truth.";
            case REQUEST -> "Need without a price attached. That changes the answer.";
            case TRUST -> "Trust, then. No contracts, no ownership, no hidden ink.";
            case ROMANCE -> "The uncontracted part of me, then. Careful. That part has teeth.";
            default -> "";
        };
    }
    static String helpResponse() {
        return "Treat it like more than a job. Someone wrote a cage and called it normal; help me make that expensive.";
    }

    static String questCommitLabel() {
        return "I will help, but no pretty promises.";
    }

    static String questPersonalCommitLabel() {
        return "Then I will not make your name another contract.";
    }

    static String questPracticalCommitLabel() {
        return "Point me at the first lie.";
    }

    static String questAcceptedLine() {
        return "Good. Keep the promise small enough to stay honest. We expose the lie first; speeches can queue politely.";
    }

    static String chapterAcceptanceLine(String focus) {
        return "Good. No pretty promises. " + focus;
    }

    static String chapterClarifyLine(String focus) {
        return "Someone made harm look properly filed. I want the first lie exposed. " + focus;
    }

    static String chapterPersonalLine(String title) {
        return "Because " + title + " keeps circling the same bruise: I was treated like a clause someone else could inherit. Help me prove a name can belong to itself.";
    }

    static String defaultObjectiveLine() {
        return "Follow the nearest contradiction. Clean ink worries me more than spilled ink.";
    }

    static String objectiveVoiceWarning() {
        return "If the answer looks too official, assume someone paid for the polish.";
    }

    static String supportResponse() {
        return "Then help me make the lie expensive for the people profiting from it.";
    }

    static String warningResponse() {
        return "The danger is that whoever benefits from the lie has already priced our silence.";
    }

    static String chapterWarningLine() {
        return "Do not trust the official shape of the harm. If it looks too clean, someone paid for the polish.";
    }

    static String searchFocusClose() {
        return "before the official version hardens around the lie.";
    }

    static String gatherFocusClose() {
        return "because evidence embarrasses lies better than outrage does.";
    }

    static String defeatFocusClose() {
        return "and noticing who paid for violence to look inevitable.";
    }

    static String progressRootGreeting() {
        return "The ink is starting to smear. Good. Clean lies make me nervous.";
    }

    static String readyRootGreeting() {
        return "You brought enough ink to make someone powerful uncomfortable.";
    }

    static String progressLine() {
        return "Every piece we find makes the lie less profitable.";
    }

    static String originLine() {
        return "A contract can become a cage if enough rich people agree to call it law.";
    }

    static String concernLine() {
        return "Ink is tidy because the cruelty happened before it dried.";
    }

    static String woundLine() {
        return "My family was taught to call survival a debt. I am trying to stop speaking that language.";
    }

    static String practicalClose() {
        return "Bring me the part they thought no one would dare read.";
    }

    static String marriedGreeting() {
        return "There you are. No summons, no contract, and still you came back. I remain suspiciously fond of that.";
    }

    static String romancedGreeting() {
        return "You have terrible timing. I am beginning to suspect I like being interrupted by you.";
    }

    static String oathsteadNeedLine() {
        return "Clear agreements. Fair shares. No paper that owns a person.";
    }

    static String nextStepLine() {
        return "Follow the paper until it points at someone powerful enough to hate being named.";
    }

    static String memoryOpening() {
        return "Memory is a witness. Unreliable, yes, but rarely useless.";
    }

    static String memoryReflection(String remembered) {
        return "I remember: " + remembered + " It matters because choice has to be witnessed, not merely claimed.";
    }

    static String memorySharedResponse() {
        return "Then we are both bound to the better version of what happened.";
    }

    static String memoryForwardResponse() {
        return "Forward, then. But not forgetfully.";
    }

    static String opinionAboutPlayer(DialogueLibrary.DialogueContext context) {
        return "You walk into old contracts like they are doors. I like that you keep checking for locks.";
    }

    static String flirtChoice() {
        return "No hidden clauses. I just want you.";
    }

    static String flirtRootLabel() {
        return "No contracts. Just you and me.";
    }

    static String flirtBoldChoice() {
        return "You are impossible to negotiate with when you look at me like that.";
    }

    static String flirtSoftLine() {
        return "Near is acceptable. Near does not own. Near chooses, and I like choices.";
    }

    static String flirtBoldLine() {
        return "Careful. I might start believing you know exactly what you are promising.";
    }

    static String flirtBoundaryLine() {
        return "Not too much if it stays honest. I flinch from claims, not wanting.";
    }

    static String flirtAfterLine() {
        return "I know now. Try not to look smug; I am already fond enough to find it charming.";
    }

    static String romanceDateFlirtLine() {
        return "Careful. That sounded almost like desire without paperwork. I may need to hear it again.";
    }

    static String romanceDateSincereLine() {
        return "Then I will sit here as myself. Not borrowed, not bought, not fleeing. With you.";
    }

    static String romanceDateBoundaryLine() {
        return "Good. Wanting without pressure is the only version I trust.";
    }

    static String loyalOpening() {
        return "I keep choosing your side without checking for a loophole first. Disturbing progress.";
    }

    static String trustedOpening() {
        return "There are truths I used to keep under lock. You have become annoyingly difficult to lock out.";
    }

    static String friendOpening() {
        return "You keep arriving before I can decide whether I am pleased. Evidence is accumulating.";
    }

    static String friendlyOpening() {
        return "You are either useful or very well disguised as useful. I am still investigating.";
    }

    static String acquaintanceOpening() {
        return "We have compatible suspicions. That is not friendship, but it has opened worse doors.";
    }

    static String guardedOpening() {
        return "You are asking questions. I respect that less than answers and more than silence.";
    }

    static String recruitId() {
        return "seraphine";
    }

    static String recruitmentRootLabel() {
        return "Ask Seraphine to choose the road";
    }

    static String recruitmentOpening() {
        return "You are about to ask for a promise. I can hear the hinges creak. Say it plainly, and do not make it sound like ownership.";
    }

    static String recruitmentRoadChoice() {
        return "Travel with me by choice, no clauses.";
    }

    static String recruitmentOathsteadChoice() {
        return "Oathstead has room without owning you.";
    }

    static String recruitmentRoadAcceptLine() {
        return "Then I choose the road, not the debt. If you ever forget the difference, I will remind you with elegance and possibly knives.";
    }

    static String recruitmentOathsteadAcceptLine() {
        return "Oathstead without ownership. A suspiciously decent offer. I accept, and reserve the right to inspect every promise.";
    }

    static String recruitmentWaitLine() {
        return "Good. Asking without pressing is a rare talent. Practice it. I may reward consistency.";
    }

    static String practicalDetailStageOne() {
        return "Follow the ink that looks too clean.";
    }

    static String readyLine() {
        return "Put it where I can see the fraud without letting rage edit the evidence.";
    }

    static String afterQuestStageOne() {
        return "First page found. Now the contract knows someone is reading back.";
    }

    static String checkInOpeningTrusted() {
        return "Most people ask that when they want leverage. You keep forgetting the leverage.";
    }

    static String feelingLineCommitted() {
        return "Less owned by old ink. More annoyed by how much that matters.";
    }

    static String sharedTableStayLineCommitted() {
        return "Then sit close enough that I do not have to perform being fine.";
    }

    static String oathsteadAssignedGreeting(String station) {
        return "The " + station + " has fewer hidden clauses than I expected. I am improving it anyway.";
    }

    static String oathsteadGreeting() {
        return "Oathstead has not asked me to sign away a single piece of myself. Suspicious place.";
    }

    static String marriedFlirtOpening() {
        return "A dangerous opening, spouse. I have no legal objection.";
    }

    static String romanceDateOpeningAtPlace(String place) {
        return "A quiet hour at " + place + ", with no one selling, signing, or owning anything. Suspiciously luxurious.";
    }

    static String romanceDateOpeningInvite() {
        return "Ask me somewhere with no ledgers. I want to learn what I say when nothing is being negotiated.";
    }

    static String romanceDatePlaceLine(String place) {
        return "Then this is ours for an hour. No witnesses with claims, no ink with teeth.";
    }

    static String milestoneTruthLine(int threshold) {
        return switch (threshold) {
            case 50 -> "Guarded respect, then. I dislike how legal that sounds, but it fits: limited terms, honestly entered.";
            case 100 -> "I still expect kindness to hide terms. With you, I sometimes forget to look for the trap first.";
            case 150 -> "If I stay, it is not because you own the clause I signed. It is because you keep leaving the door open and I keep choosing not to use it.";
            case 180 -> "I want you without turning wanting into ownership. That is the frightening part: no contract, no cage, just choice.";
            case 250 -> "A future with you looks like unlocked doors, fair terms, and my name belonging to me while I choose to answer when you say it.";
            default -> "The honest version is simple and therefore terrifying: you matter.";
        };
    }

    static String milestoneWarmLine(int threshold) {
        return switch (threshold) {
            case 150 -> "Then I choose the open door and the person beside it. That may be the freest thing I have done.";
            case 250 -> "No hidden ink, then. Just the terrifying plain text of wanting a life.";
            default -> "Then let this be spoken plainly enough to survive tomorrow.";
        };
    }

    static String milestoneOpening(int threshold, DialogueLibrary.DialogueContext context) {
        return switch (threshold) {
            case 50 -> "I have stopped counting exits before your sentences finish. Do not look too pleased. It means guarded respect, not a parade.";
            case 100 -> "There is a difference between being saved and being owned. I know it in theory. With you, I am starting to know it in my body.";
            case 150 -> "If I stay, it is not because you own the clause I signed. It is because the door stayed open and I still chose the room.";
            case 180 -> context.sharedTableConversation()
                        ? "This table has no contract between us, which makes what I want both simpler and more dangerous."
                        : "Ask me somewhere with poor witnesses and no ledgers. If I flirt, understand that I mean it more than is convenient.";
            case 250 -> "A future with you looks like unlocked doors, fair terms, and my name belonging to me while I choose to answer when you say it.";
            default -> "Something has changed between us. It deserves better than being stepped around.";
        };
    }

    static String valuesTrustLine() {
        return "You keep checking whether a choice is actually free. I trust that more than any oath dressed in gold.";
    }

    static String valuesWorryLine() {
        return "That you will let duty become a prettier word for ownership if enough people praise you for it.";
    }

    static String valuesRequestLine() {
        return "When someone asks you to save them, make sure they are not handing you a chain with flowers on it.";
    }

    static String outcomeMemoryRootLabel() {
        return "Can we talk about the Vale choices?";
    }

    static String outcomeMemoryOpening() {
        return "Vale did not become free because one paper burned. It became free because we kept asking who benefited from every beautiful chain.";
    }

    static String outcomeMemorySharedLine(String outcomeKey) {
        return switch (outcomeKey) {
            case "seraphine_clerk_truth" -> "Then remember that a witness is not a tool because testimony is useful. He stayed a person first.";
            case "seraphine_red_notary" -> "Then remember that freedom did not become clean just because the seal broke. We chose the mess we could defend.";
            case "seraphine_oathstead_promise" -> "Then remember that this promise keeps its door open. That is the part I trust.";
            default -> "Then remember it without making me grateful for the wound. Witness is enough. Ownership is not invited.";
        };
    }

    static String outcomeMemoryLine(String outcomeKey, DialogueLibrary.DialogueContext context) {
        String tone = CompanionMemoryTone.describe(context.questOutcome(outcomeKey));
        return switch (outcomeKey) {
                    case "seraphine_first_lie" -> "In the counting house, " + tone
                            + ". That was when I began suspecting you understood evidence is not freedom until someone can use it and survive using it.";
                    case "seraphine_clerk_truth" -> "With the hidden clerk, " + tone
                            + ". I once let another witness stand alone. You made that harder to repeat.";
                    case "seraphine_family_debt" -> "At the Vale counting room, " + tone
                            + ". You did not mistake my father's desperation for consent. I remember that more than I admit.";
                    case "seraphine_red_notary" -> "After the Red Notary fell, " + tone
                            + ". That choice freed more than my name, or showed exactly what freedom still costs.";
                    case "seraphine_oathstead_promise" -> "At Oathstead, " + tone
                            + ". That was the first promise in a long time that did not feel like someone else's handwriting.";
                    default -> "I remember that choice. It had clauses nobody wrote down and consequences nobody could fully audit.";
                };
    }

    static String outcomeMemoryQuestionLine(String outcome) {
        if ("clause_copied".equals(outcome)) {
                        return "Perhaps I would have copied two pages. Riverside taught me never to trust a single piece of leverage near a candle.";
                    }
                    if ("public_record".equals(outcome)) {
                        return "Perhaps I would have made the public record louder and the exits clearer. Exposure without escape can become another trap.";
                    }
                    if ("witness_first".equals(outcome)) {
                        return "No. Hiding the proof until the witness could breathe was not cowardice. It was sequencing.";
                    }
                    if ("witness_protected".equals(outcome)) {
                        return "Perhaps I would protect less quietly. Hidden witnesses become easy for noble rooms to misplace.";
                    }
                    if ("testimony_public".equals(outcome)) {
                        return "Perhaps I would have made more copies before the speech. Truth deserves witnesses of its own.";
                    }
                    if ("leverage_traded".equals(outcome)) {
                        return "Yes. I would bargain harder for the person inside the leverage. Useful is a dangerous word when fear is listening.";
                    }
                    if ("survival_named".equals(outcome)) {
                        return "No. That was the sentence I needed: desperation is not consent, even when it leaves a signature.";
                    }
                    if ("legal_lie_named".equals(outcome)) {
                        return "No. Legal and false can live in the same room. Naming both was the only way to open a window.";
                    }
                    if ("blame_signed".equals(outcome)) {
                        return "Yes. I would be kinder to the people inside the trap. Shame is how old contracts keep collecting interest.";
                    }
                    if ("ledger_published".equals(outcome)) {
                        return "Perhaps I would publish with more shelter ready. Daylight helps, but it does not walk frightened people home.";
                    }
                    if ("names_reclaimed".equals(outcome)) {
                        return "No. Keeping the records for victims was untidy, dangerous, and exactly why it mattered.";
                    }
                    if ("safety_bargain".equals(outcome)) {
                        return "Perhaps. Safety bought with records always asks for another payment later.";
                    }
                    if ("records_burned".equals(outcome)) {
                        return "Yes. The fire was clean. Too clean. Some names still needed proof more than ash.";
                    }
                    if ("refuge_ledger".equals(outcome)) {
                        return "No. A ledger that helps people leave chains is the first beautiful one I have met.";
                    }
                    if ("witness_bench".equals(outcome)) {
                        return "No. Witnesses before oaths. I like promises better when they have to listen first.";
                    }
                    if ("chosen_daily".equals(outcome)) {
                        return "No. A promise I can choose again tomorrow is the only kind I know how to trust.";
                    }
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

    static String opinionAbout(String subject) {
        return switch (subject) {
                    case "MAERA" -> "Maera treats truth like contraband. Excellent instincts, terrible hiding posture.";
                    case "CASSIA" -> "Cassia is what happens when guilt learns shield discipline.";
                    case "LYRA" -> "Lyra makes mercy sound like logistics. That is why I trust it.";
                    case "SAMIR" -> "Samir questions light as if it might answer honestly. Brave, or beautifully doomed.";
                    case "ARIA" -> "Aria notices exits first. I respect a woman with priorities.";
                    case "VESPER" -> VesperDialogue.opinionAboutVesperFrom("SERAPHINE");
                    case "RAFIQ" -> "Rafiq jokes like a man checking whether the floor will hold.";
                    case "CALDER" -> "Calder could make a bridge out of bad news and shame the river into behaving.";
                    default -> "They are useful. That is not the same as simple.";
                };
    }

    static String feelingLineTrusted() {
        return "Threadbare in places I prefer to keep expensive-looking.";
    }

    static String milestoneWarmLine(int threshold, DialogueLibrary.DialogueContext context) {
        return switch (threshold) {
            case 100 -> "Then I will tell you the parts that do not flatter me. Kindly refrain from looking like someone who can be trusted. It is distracting.";
            case 150 -> "Then I choose the open door and the person beside it. That may be the freest thing I have done.";
            case 250 -> "No hidden ink, then. Just the terrifying plain text of wanting a life.";
            default -> "Then let this be spoken plainly enough to survive tomorrow.";
        };
    }

}
