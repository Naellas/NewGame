package com.alderfall.game;

final class MaeraDialogue {
    private MaeraDialogue() {
    }

    static CompanionDialogueProfile profile() {
        return new CompanionDialogueProfile(
                "Maera",
                "forbidden maps",
                "Why is this map forbidden?",
                "The star map shows a road the royal record says never existed. Someone cut out the star that proves the road was there. I need the map, the route entry, and Miri's testimony before the Archive files the lie as policy.",
                "What did your mother hide from you?",
                "A route, a warning, and proof that the Archive rewarded silence before it punished curiosity. I can defend a page. I am less practiced at defending the person who kept it alive.",
                "My whole life was footnotes and locked shelves. You made a road through them. I would like to keep walking it with you.",
                "Yes. But our vows will include at least one clause about never hiding maps from each other.",
                "I have indexed every reason this is reckless. None of them made me want you less."
        );
    }

    static String characterBeat(DialogueLibrary.DialogueIntent intent) {
        return switch (intent) {
            case QUEST_ROOT, QUEST_CLARIFY, MEMORY -> "Maera follows the thought like a line of ink across a dangerous margin.";
            case QUEST_PRACTICAL, NEXT_STEP -> "Maera organizes the answer into steps before emotion can scatter it.";
            case OPINION, VALUES, TRUST -> "Maera looks up from the inner record she keeps of you.";
            case ROMANCE, FUTURE, HOME -> "Maera softens around the question, as if surprised the page is still blank.";
            default -> "Maera sorts the thought as if it were a page with dangerous margins.";
        };
    }

    static String intentAcknowledgement(DialogueLibrary.DialogueIntent intent) {
        return switch (intent) {
            case QUEST_CLARIFY -> "You want the underlying record, not the summary. Sensible.";
            case VALUES -> "My assessment, as honest as a living source permits.";
            case MEMORY -> "If we are reopening the record, we should do it deliberately.";
            case FUTURE -> "Speculating about after. I have notes, unfortunately.";
            default -> "";
        };
    }
    static String helpResponse() {
        return "Stay with the evidence when it stops being elegant. The missing piece matters because the official story wants it gone.";
    }

    static String questCommitLabel() {
        return "I will follow the evidence with you.";
    }

    static String questPersonalCommitLabel() {
        return "Then I will not flatten this into a theory.";
    }

    static String questPracticalCommitLabel() {
        return "Show me where the record starts breaking.";
    }

    static String questAcceptedLine() {
        return "Good. Follow what contradicts the official version, even if it makes the map uglier.";
    }

    static String chapterAcceptanceLine(String focus) {
        return "Good. Keep the evidence in one piece. " + focus;
    }

    static String chapterClarifyLine(String focus) {
        return "The map, the route record, and the witness cannot all be mistakes. Someone made the official version contradict the physical evidence. " + focus;
    }

    static String chapterPersonalLine(String title) {
        return "Because " + title + " is not only about being correct. The Archive trained me to protect pages and abandon witnesses. I am asking you to help me refuse that lesson.";
    }

    static String defaultObjectiveLine() {
        return "Find the detail that refuses to fit the official map.";
    }

    static String objectiveVoiceWarning() {
        return "Bring me the map, record, or testimony intact; a damaged proof lets the Archive call truth unreliable.";
    }

    static String supportResponse() {
        return "Then stay with the evidence when it becomes inconvenient.";
    }

    static String warningResponse() {
        return "The danger is a tidy answer. Tidy answers often mean someone burned the rough draft.";
    }

    static String chapterWarningLine() {
        return "Do not accept the clean version because it is easier to cite. Check the cut mark, the date, and the witness before anyone decides which truth is convenient.";
    }

    static String searchFocusClose() {
        return "before the archive sands the contradiction into something convenient.";
    }

    static String gatherFocusClose() {
        return "because a theory with nothing in its hands is only a clever guess.";
    }

    static String defeatFocusClose() {
        return "and noticing what knowledge it was set to guard.";
    }

    static String progressRootGreeting() {
        return "The record is changing as we read it. Keep your place.";
    }

    static String readyRootGreeting() {
        return "The page is no longer blank. Now we decide how loudly it speaks.";
    }

    static String progressLine() {
        return "Keep the contradiction intact; it may be the most honest thing here.";
    }

    static String originLine() {
        return "A forbidden map is usually a confession folded into geography.";
    }

    static String concernLine() {
        return "If the map is wrong on purpose, every road built from it is a lie with mile markers.";
    }

    static String woundLine() {
        return "My mother hid truth where only stubborn love would find it. I am angry enough to be grateful.";
    }

    static String practicalClose() {
        return "Bring back the contradiction, not a cleaned-up story.";
    }

    static String marriedGreeting() {
        return "There you are. I was about to annotate the silence, which would have been undignified.";
    }

    static String romancedGreeting() {
        return "You arrived before I finished sorting my thoughts. Annoyingly, that may improve them.";
    }

    static String oathsteadNeedLine() {
        return "A map table, good light, and the courage to correct old routes.";
    }

    static String nextStepLine() {
        return "Trust the contradiction. That is usually where truth failed to stay buried.";
    }

    static String memoryOpening() {
        return "Good. Let us compare the record in your head with the one in mine.";
    }

    static String memoryReflection(String remembered) {
        return "I remember: " + remembered + " The detail still refuses to become simple, which usually means it is true.";
    }

    static String memorySharedResponse() {
        return "Shared testimony. Stronger than either account alone.";
    }

    static String memoryForwardResponse() {
        return "Forward with annotations. I can tolerate that.";
    }

    static String opinionAboutPlayer(DialogueLibrary.DialogueContext context) {
        return "You are evidence with boots. Occasionally muddy evidence, but persuasive.";
    }

    static String flirtChoice() {
        return "Your footnotes are distracting me.";
    }

    static String flirtRootLabel() {
        return "You keep stealing my attention.";
    }

    static String flirtBoldChoice() {
        return "I am trying very hard not to memorize your mouth.";
    }

    static String flirtSoftLine() {
        return "Good. I have been pretending the same thing was merely tactical positioning.";
    }

    static String flirtBoldLine() {
        return "That is not a scholarly observation. I approve of the methodological collapse.";
    }

    static String flirtBoundaryLine() {
        return "Not too much. Just undocumented. I can survive one unsorted feeling.";
    }

    static String flirtAfterLine() {
        return "Noted, badly filed, and likely to distract me later.";
    }

    static String romanceDateFlirtLine() {
        return "If you keep looking at me like that, I will lose my place in three separate arguments.";
    }

    static String romanceDateSincereLine() {
        return "Then let the record be incomplete for once. I would rather live this hour than annotate it.";
    }

    static String romanceDateBoundaryLine() {
        return "Careful is acceptable. Some truths deserve proper handling.";
    }

    static String loyalOpening() {
        return "I have begun leaving space for you in plans I pretend are only theoretical.";
    }

    static String trustedOpening() {
        return "You are no longer a source note. You are in the argument itself.";
    }

    static String friendOpening() {
        return "Your timing interrupts my notes. I have begun leaving room for it.";
    }

    static String friendlyOpening() {
        return "I am beginning to file you under 'useful complications.'";
    }

    static String acquaintanceOpening() {
        return "You are an adjacent entry in a risky index. I have not decided the cross-reference.";
    }

    static String guardedOpening() {
        return "If you came for an approved version, you are standing in the wrong margin.";
    }

    static String recruitId() {
        return "maera";
    }

    static String recruitmentRootLabel() {
        return "Ask Maera to join the search";
    }

    static String recruitmentOpening() {
        return "If this is an invitation, be precise. I have followed enough badly labeled roads for one lifetime.";
    }

    static String recruitmentRoadChoice() {
        return "Walk with me. We will follow the truth together.";
    }

    static String recruitmentOathsteadChoice() {
        return "Oathstead needs your maps and questions.";
    }

    static String recruitmentRoadAcceptLine() {
        return "Then I am coming. Bring spare ink, patience for corrections, and the humility to admit when the map insults us correctly.";
    }

    static String recruitmentOathsteadAcceptLine() {
        return "A young settlement with editable records and no royal censor in the rafters. Fine. I am interested.";
    }

    static String recruitmentWaitLine() {
        return "Deferred, not rejected. I appreciate accurate labels.";
    }

    static String practicalDetailStageOne() {
        return "Start with the scraped star, then compare the route entry, then ask Miri who moved the folio.";
    }

    static String readyLine() {
        return "Set it beside the record. I want the lie and the correction in the same light.";
    }

    static String afterQuestStageOne() {
        return "You kept the scraped star, the altered route entry, and Miri's testimony together. That was the first time this lie had to answer all three at once.";
    }

    static String checkInOpeningTrusted() {
        return "You ask as if the answer belongs in the record. I am not sure whether to be annoyed or relieved.";
    }

    static String feelingLineCommitted() {
        return "Overfull. Evidence, fear, fondness. My mental shelves are badly arranged.";
    }

    static String sharedTableStayLineCommitted() {
        return "Good. I have spent too long with records that cannot answer back.";
    }

    static String oathsteadAssignedGreeting(String station) {
        return "The " + station + " keeps producing questions. I have claimed a corner for the dangerous ones.";
    }

    static String oathsteadGreeting() {
        return "Oathstead keeps becoming a place worth footnoting. I am trying not to overdo it.";
    }

    static String marriedFlirtOpening() {
        return "The record notes a familiar look and refuses to remain objective.";
    }

    static String romanceDateOpeningAtPlace(String place) {
        return place + " has poor archival discipline and excellent potential for being interrupted by honesty.";
    }

    static String romanceDateOpeningInvite() {
        return "Find me a table, a bad candle, and an hour the archives cannot claim. I will bring a dangerously honest question.";
    }

    static String romanceDatePlaceLine(String place) {
        return "Then I will stop cataloging exits and start remembering the shape of your voice in " + place + ".";
    }

    static String milestoneTruthLine(int threshold) {
        return switch (threshold) {
            case 50 -> "The honest version is that you have become a source I do not immediately distrust.";
            case 100 -> "I am afraid the truth will cost more than I can pay, and more afraid I will pay it anyway because you are watching.";
            case 150 -> "I have corrected kings in margins and feared my own name in ink. With you, loyalty feels like citing the truth aloud.";
            case 180 -> "I keep trying to file this under alliance, gratitude, shared evidence. The index refuses. It keeps writing your name.";
            case 250 -> "A future with you has maps on the table, arguments in the margins, and no locked drawer between us.";
            default -> "The honest version is simple and therefore terrifying: you matter.";
        };
    }

    static String milestoneWarmLine(int threshold) {
        return switch (threshold) {
            case 150 -> "Then let the record show this was chosen under no false citation.";
            case 250 -> "Then we draft the future in pencil first, and trust each other enough to revise.";
            default -> "Then let this be spoken plainly enough to survive tomorrow.";
        };
    }

    static String milestoneOpening(int threshold, DialogueLibrary.DialogueContext context) {
        return switch (threshold) {
            case 50 -> "I have begun treating you as a source worth preserving, which is more intimate than it sounds and less flattering than you hope.";
            case 100 -> "There is a correction I keep avoiding because it is written in me, not in a book. I dislike that filing category.";
            case 150 -> "Loyalty, properly cited: I have seen enough evidence to choose this road without calling it an error.";
            case 180 -> context.sharedTableConversation()
                        ? "I keep trying to classify this as alliance, gratitude, shared evidence. The index refuses and keeps writing your name."
                        : "Find me a table, a bad candle, and an hour the archives cannot claim. I have a dangerously honest question.";
            case 250 -> "A future with you has maps on the table, arguments in the margins, and no locked drawer between us.";
            default -> "Something has changed between us. It deserves better than being stepped around.";
        };
    }

    static String valuesTrustLine() {
        return "You let evidence inconvenience you. That is rarer than courage and more useful.";
    }

    static String valuesWorryLine() {
        return "That urgency will teach you to accept simple answers because they move faster.";
    }

    static String valuesRequestLine() {
        return "When truth becomes inconvenient, do not make it lonely.";
    }

    static String outcomeMemoryRootLabel() {
        return "Can we talk about the archive choices?";
    }

    static String outcomeMemoryOpening() {
        return "The archive road was never about finding one true page. It was about deciding what truth deserved once we had proof.";
    }

    static String outcomeMemorySharedLine(String outcomeKey) {
        return "Then remember it with the margins intact. The uncomfortable part is usually where the record begins telling the truth.";
    }

    static String outcomeMemoryLine(String outcomeKey, DialogueLibrary.DialogueContext context) {
        String tone = CompanionMemoryTone.describe(context.questOutcome(outcomeKey));
        return switch (outcomeKey) {
                    case "maera_first_truth" -> "With the forbidden footnote, " + tone
                            + ". You kept the cut map, the route date, and Miri's testimony together instead of letting the Archive isolate them.";
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

    static String outcomeMemoryQuestionLine(String outcome) {
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

    static String opinionAbout(String subject) {
        return switch (subject) {
                    case "SERAPHINE" -> "Seraphine is a legal argument with knives. I would cite her carefully.";
                    case "CASSIA" -> "Cassia is a primary source on duty hurting people. She hates that, which makes her valuable.";
                    case "LYRA" -> "Lyra records pain because forgetting is another wound.";
                    case "SAMIR" -> "Samir is revising faith in real time. Fascinating. Painful footnotes.";
                    case "ARIA" -> "Aria reads tracks the way I read margins.";
                    case "VESPER" -> VesperDialogue.opinionAboutVesperFrom("MAERA");
                    case "RAFIQ" -> "Rafiq is unreliable only when pretending not to care.";
                    case "CALDER" -> "Calder believes structures tell the truth. I am fond of that hypothesis.";
                    default -> "They are a useful contradiction.";
                };
    }

    static String feelingLineTrusted() {
        return "Like I need another shelf for thoughts I refuse to misfile.";
    }

    static String milestoneWarmLine(int threshold, DialogueLibrary.DialogueContext context) {
        return switch (threshold) {
            case 100 -> "Then I will cite the wound directly. Do not interrupt unless I start footnoting my own panic.";
            case 150 -> "Then let the record show this was chosen under no false citation.";
            case 250 -> "Then we draft the future in pencil first, and trust each other enough to revise.";
            default -> "Then let this be spoken plainly enough to survive tomorrow.";
        };
    }

}
