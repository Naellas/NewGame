package com.alderfall.game;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public final class CompanionDialogueQaExport {
    private static final int[] TRUST_LEVELS = {0, 20, 50, 100, 150, 180, 250};
    private static final int MAX_FINDINGS_PER_COMPANION = 12;
    private static final int MAX_OPTION_SUMMARY = 5;
    private static final int PREVIEW_LENGTH = 120;
    private static final List<String> DETAIL_PATHS = List.of(
            "Can we talk,|How are you holding up?|How are you feeling?",
            "Can we talk,|How do you see me?",
            "Can we talk,|Do you trust me?",
            "Can we talk,|What do you think of|Me, honestly",
            "Can we talk,|What do you think of|Known companion",
            "How does Oathstead feel|How does Oathstead feel|What does Oathstead need next",
            "Can we talk,|Come here a moment.|I missed being close to you.",
            "Can we talk,|Stay with me here.",
            "Can we talk,|Can we talk about our promise?"
    );

    private CompanionDialogueQaExport() {
    }

    public static void main(String[] args) throws IOException {
        Path output = args.length > 0 && !args[0].isBlank()
                ? Path.of(args[0])
                : Path.of("..", "Story premise", "companion-dialogue-qa.md");
        List<Npc> companions = storyCompanions();
        List<String> knownNames = companions.stream()
                .map(Npc::name)
                .toList();
        List<CompanionQa> companionReports = companions.stream()
                .map(companion -> {
                    Quest firstQuest = firstCompanionQuest(companion.recruitId());
                    return new CompanionQa(companion, firstQuest, qaFindingsFor(companion, firstQuest, knownNames));
                })
                .toList();
        int totalFindings = companionReports.stream()
                .mapToInt(companion -> companion.findings().size())
                .sum();

        StringBuilder report = new StringBuilder();
        report.append("# Companion Dialogue QA\n\n");
        report.append("Generated: ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .append("\n\n");
        report.append("This document samples companion dialogue roots and representative branches so dialogue can be reviewed as conversation, not as disconnected menu text.\n\n");
        report.append("Use it to look for repeated labels, vague roots, NPC replies that ignore the player's line, missing quest-stage options, and trust states that do not feel distinct.\n\n");

        appendQaSummary(report, companionReports, totalFindings);

        for (CompanionQa companionReport : companionReports) {
            appendCompanionReport(report, companionReport.companion(), companionReport.firstQuest(), knownNames, companionReport.findings());
        }

        Files.createDirectories(output.normalize().getParent());
        Files.writeString(output, report.toString(), StandardCharsets.UTF_8);
        System.out.println("Wrote companion dialogue QA report to " + output.normalize().toAbsolutePath());
    }

    private static List<Npc> storyCompanions() {
        List<Npc> companions = new ArrayList<>();
        for (Npc npc : GameData.NPCS) {
            if (npc.recruitId() == null || npc.recruitId().isBlank()) {
                continue;
            }
            if (npc.questId() == null || npc.questId().isBlank()) {
                continue;
            }
            Quest quest = GameData.QUESTS.get(npc.questId());
            if (quest != null && quest.companionQuest()) {
                companions.add(npc);
            }
        }
        companions.sort(Comparator.comparing(Npc::name));
        return companions;
    }

    private static Quest firstCompanionQuest(String recruitId) {
        return GameData.QUESTS.values().stream()
                .filter(Quest::companionQuest)
                .filter(quest -> recruitId != null && recruitId.equals(quest.chainOwnerId))
                .sorted(Comparator.comparing(quest -> quest.id))
                .findFirst()
                .map(Quest::copy)
                .orElse(null);
    }

    private static List<Quest> questStates(Quest baseQuest) {
        if (baseQuest == null) {
            return List.of();
        }
        List<Quest> states = new ArrayList<>();
        states.add(baseQuest.copy());

        Quest accepted = baseQuest.copy();
        accepted.accepted = true;
        accepted.progress = Math.max(0, accepted.activeNeeded() / 2);
        states.add(accepted);

        Quest ready = baseQuest.copy();
        ready.accepted = true;
        ready.progress = ready.activeNeeded();
        states.add(ready);

        Quest laterStage = baseQuest.copy();
        laterStage.accepted = true;
        laterStage.stageIndex = Math.min(1, Math.max(0, laterStage.stages.size() - 1));
        laterStage.progress = 0;
        states.add(laterStage);

        Quest completed = baseQuest.copy();
        completed.accepted = true;
        completed.completed = true;
        completed.stageIndex = Math.max(0, completed.stages.size() - 1);
        completed.progress = completed.activeNeeded();
        states.add(completed);
        return states;
    }

    private static void appendQaSummary(StringBuilder report, List<CompanionQa> companionReports, int totalFindings) {
        report.append("## QA Findings Summary\n\n");
        report.append("- Total findings: ").append(totalFindings).append("\n");
        if (totalFindings == 0) {
            report.append("- All sampled companion paths passed the structural QA checks.\n");
        } else {
            for (CompanionQa companionReport : companionReports) {
                int count = companionReport.findings().size();
                if (count > 0) {
                    report.append("- ")
                            .append(companionReport.companion().name())
                            .append(": ")
                            .append(count)
                            .append("\n");
                }
            }
        }
        report.append("\n");
    }

    private static void appendCompanionReport(
            StringBuilder report,
            Npc companion,
            Quest firstQuest,
            List<String> knownNames,
            List<String> findings
    ) {
        report.append("## ").append(companion.name()).append("\n\n");
        report.append("- Recruit id: `").append(companion.recruitId()).append("`\n");
        report.append("- Starting quest: ").append(firstQuest == null ? "_none_" : "`" + firstQuest.title + "`").append("\n");
        if (firstQuest != null) {
            report.append("- Quest stages: ").append(firstQuest.stages.size()).append("\n");
        }
        report.append("\n");

        report.append("### QA Findings\n\n");
        if (findings.isEmpty()) {
            report.append("_No QA findings detected in sampled paths._\n\n");
        } else {
            for (String finding : findings) {
                report.append("- ").append(finding).append("\n");
            }
            report.append("\n");
        }

        report.append("### Trust Snapshot Roots\n\n");
        for (int trust : TRUST_LEVELS) {
            Quest quest = questForTrust(firstQuest, trust);
            DialogueLibrary.DialogueContext context = contextFor(companion, knownNames, trust, false);
            DialogueLibrary.DialogueSession session = session(companion, quest, trust, context);
            report.append("#### Trust ").append(trust).append("\n\n");
            appendNode(report, "NPC", session.line(trust), session.optionLabels());
            report.append("\n");
        }

        report.append("### Quest Stage Branches\n\n");
        if (firstQuest == null) {
            report.append("_No companion quest found._\n\n");
        } else {
            for (Quest questState : questStates(firstQuest)) {
                appendQuestState(report, companion, questState, knownNames);
            }
        }

        report.append("### Representative Conversation Paths\n\n");
        for (String path : DETAIL_PATHS) {
            int trust = trustForPath(path);
            DialogueLibrary.DialogueContext context = contextFor(companion, knownNames, trust, false);
            appendPath(report, companion, null, trust, context, path);
        }

        report.append("### Repeat-Aware Sample\n\n");
        DialogueLibrary.DialogueContext repeatContext = contextFor(companion, knownNames, 150, true);
        appendPath(report, companion, null, 150, repeatContext, "Can we talk,|How are you holding up?|How are you feeling?");
        report.append("\n");
    }

    private static Quest questForTrust(Quest firstQuest, int trust) {
        if (firstQuest == null) {
            return null;
        }
        Quest quest = firstQuest.copy();
        if (trust >= 50) {
            quest.accepted = true;
            quest.progress = Math.max(0, quest.activeNeeded() / 2);
        }
        if (trust >= 100) {
            quest.accepted = true;
            quest.progress = quest.activeNeeded();
        }
        if (trust >= 150 && quest.stages.size() > 1) {
            quest.accepted = true;
            quest.stageIndex = Math.min(1, quest.stages.size() - 1);
            quest.progress = Math.max(0, quest.activeNeeded() / 2);
        }
        if (trust >= 250) {
            quest.accepted = true;
            quest.completed = true;
            quest.stageIndex = Math.max(0, quest.stages.size() - 1);
            quest.progress = quest.activeNeeded();
        }
        return quest;
    }

    private static void appendQuestState(StringBuilder report, Npc companion, Quest quest, List<String> knownNames) {
        String state = quest.completed
                ? "completed"
                : quest.ready()
                ? "ready"
                : quest.accepted
                ? "accepted"
                : "offer";
        int trust = quest.completed ? 150 : 100;
        DialogueLibrary.DialogueContext context = contextFor(companion, knownNames, trust, false);
        report.append("#### ")
                .append(quest.title)
                .append(" / ")
                .append(state)
                .append(" / stage ")
                .append(quest.stageIndex + 1)
                .append(" of ")
                .append(quest.stages.size())
                .append("\n\n");
        report.append("- Stage title: `").append(quest.activeStage().title()).append("`\n");
        report.append("- Objective: `").append(quest.activeObjectiveKind()).append("` ")
                .append(quest.activeTarget())
                .append(" (")
                .append(quest.progress)
                .append("/")
                .append(quest.activeNeeded())
                .append(")\n\n");
        appendPath(report, companion, quest, trust, context, questPath(companion, quest));
    }

    private static String questPath(Npc companion, Quest quest) {
        String root = quest.completed
                ? "What changed after"
                : quest.ready()
                ? "I found what you needed"
                : quest.accepted
                ? "Where do things stand"
                : quest.title;
        if (!quest.accepted) {
            return root + "|" + offerClarifyProbe(companion, quest) + "|" + acceptanceProbe(quest);
        }
        if (quest.completed) {
            return root + "|What is still unresolved";
        }
        if (quest.ready()) {
            return root;
        }
        return root + "|" + practicalProbe(quest);
    }

    private static String offerClarifyProbe(Npc companion, Quest quest) {
        if (companion != null && "vesper".equals(companion.recruitId())) {
            return "Is anyone in Snowrest hurt";
        }
        return switch (quest.activeObjectiveKind()) {
            case DEFEAT -> "Who gets hurt if this keeps hunting";
            case RESCUE -> "Who are we trying not to lose";
            case DEFEND -> "What breaks if this place falls";
            case RAID_DEFENSE -> "What are they trying to take";
            case GATHER -> "Why does this need to come back";
            case DELIVER -> "Why can this not pass";
            case VISIT -> "What made this place worth checking";
            case SEARCH -> "What are we looking for";
            case TALK -> "Why does this conversation matter";
            case ASK_AROUND -> "Which rumor could get someone killed";
            case REPORT -> "Who needs the unpolished truth";
            case ESCORT -> "Why does this road need protection";
            case CHOICE -> "Why put the choice in my hands";
        };
    }

    private static String practicalProbe(Quest quest) {
        return switch (quest.activeObjectiveKind()) {
            case DEFEAT -> "Where was it last seen";
            case RESCUE -> "Where do I reach them";
            case DEFEND -> "Where does the line need to hold";
            case RAID_DEFENSE -> "Where do we make our stand";
            case GATHER -> "Where do I find what you need";
            case DELIVER -> "Where does this need to land";
            case VISIT -> "Show me the place";
            case SEARCH -> "Where do I start looking";
            case TALK -> "Where do I find the right person";
            case ASK_AROUND -> "Where should I start asking";
            case REPORT -> "Where do I bring the answer";
            case ESCORT -> "Which road are we taking";
            case CHOICE -> "Where does the decision happen";
        };
    }

    private static String acceptanceProbe(Quest quest) {
        String owner = quest == null || quest.chainOwnerId == null ? "" : quest.chainOwnerId;
        return switch (owner) {
            case "seraphine" -> "I will help, but no pretty promises";
            case "maera" -> "I will follow the evidence";
            case "cassia" -> "I will stand where this breaks";
            case "lyra" -> "I will help before more people get hurt";
            case "samir" -> "I will carry the question";
            case "aria" -> "I know the road may be bait";
            case "vesper" -> quest != null && "vesper_chain_1".equals(quest.id)
                    ? "I will inspect the root circle before judging it"
                    : "I will inspect the road breaks before cutting roots";
            case "rafiq" -> "I will help you survive the truth";
            case "calder" -> "I will show up with both hands";
            default -> "I will";
        };
    }

    private static void appendPath(
            StringBuilder report,
            Npc companion,
            Quest quest,
            int trust,
            DialogueLibrary.DialogueContext context,
            String pathSpec
    ) {
        DialogueLibrary.DialogueSession session = session(companion, quest, trust, context);
        String[] steps = pathSpec.split("\\|");
        report.append("#### ").append(pathSpec.replace("|", " -> ")).append("\n\n");
        appendNode(report, "NPC", session.line(trust), session.optionLabels());
        for (String step : steps) {
            int optionIndex = findOption(session.optionLabels(), step);
            if (optionIndex < 0) {
                report.append("- Missing option containing: `").append(step).append("`\n\n");
                return;
            }
            String playerLine = session.optionLabels().get(optionIndex);
            session.choose(optionIndex);
            report.append("- Player: ").append(playerLine).append("\n");
            appendNode(report, "NPC", session.line(trust), session.optionLabels());
        }
        report.append("\n");
    }

    private static List<String> qaFindingsFor(Npc companion, Quest firstQuest, List<String> knownNames) {
        QaFindings findings = new QaFindings();
        checkTrustSnapshots(findings, companion, firstQuest, knownNames);
        if (firstQuest == null) {
            findings.add("Missing companion quest, so quest-stage dialogue samples cannot be generated.");
        } else {
            for (Quest questState : questStates(firstQuest)) {
                int trust = questState.completed ? 150 : 100;
                DialogueLibrary.DialogueContext context = contextFor(companion, knownNames, trust, false);
                checkPathSample(findings, companion, questState, trust, context, questPath(companion, questState), true);
            }
        }
        for (String path : DETAIL_PATHS) {
            int trust = trustForPath(path);
            DialogueLibrary.DialogueContext context = contextFor(companion, knownNames, trust, false);
            checkPathSample(findings, companion, null, trust, context, path, false);
        }
        DialogueLibrary.DialogueContext repeatContext = contextFor(companion, knownNames, 150, true);
        checkPathSample(
                findings,
                companion,
                null,
                150,
                repeatContext,
                "Can we talk,|How are you holding up?|How are you feeling?",
                false
        );
        return findings.values();
    }

    private static void checkTrustSnapshots(
            QaFindings findings,
            Npc companion,
            Quest firstQuest,
            List<String> knownNames
    ) {
        for (int trust : TRUST_LEVELS) {
            Quest quest = questForTrust(firstQuest, trust);
            DialogueLibrary.DialogueContext context = contextFor(companion, knownNames, trust, false);
            DialogueLibrary.DialogueSession session = session(companion, quest, trust, context);
            String sampleName = "trust " + trust + " snapshot";
            checkNodeBasics(findings, sampleName, "root", session.line(trust), session.optionLabels(), false);
            String milestonePrompt = pendingMilestonePrompt(trust);
            if (!milestonePrompt.isBlank() && !containsOption(session.optionLabels(), milestonePrompt)) {
                findings.add("Missing expected trust option in " + sampleName + ": `" + milestonePrompt + "`.");
            }
        }
    }

    private static void checkPathSample(
            QaFindings findings,
            Npc companion,
            Quest quest,
            int trust,
            DialogueLibrary.DialogueContext context,
            String pathSpec,
            boolean questStageSample
    ) {
        DialogueLibrary.DialogueSession session = session(companion, quest, trust, context);
        Map<String, String> seenLines = new LinkedHashMap<>();
        String sampleName = pathSpec.replace("|", " -> ");
        checkSampleNode(findings, sampleName, "opening", session.line(trust), session.optionLabels(), seenLines, questStageSample);
        for (String step : pathSpec.split("\\|")) {
            List<String> options = session.optionLabels();
            int optionIndex = findOption(options, step);
            if (optionIndex < 0) {
                findings.add("Player path cannot be followed in `" + sampleName + "` at step `" + step
                        + "`; available options: " + optionSummary(options) + ".");
                return;
            }
            String playerLine = options.get(optionIndex);
            session.choose(optionIndex);
            checkSampleNode(
                    findings,
                    sampleName,
                    "after `" + playerLine + "`",
                    session.line(trust),
                    session.optionLabels(),
                    seenLines,
                    questStageSample
            );
        }
    }

    private static void checkSampleNode(
            QaFindings findings,
            String sampleName,
            String nodeLabel,
            String line,
            List<String> options,
            Map<String, String> seenLines,
            boolean questStageSample
    ) {
        checkNodeBasics(findings, sampleName, nodeLabel, line, options, questStageSample);
        String combinedLine = DialogueLibrary.DialogueLine.from(line).combined();
        String normalizedLine = normalizeLine(combinedLine);
        if (normalizedLine.isBlank()) {
            return;
        }
        String firstNode = seenLines.putIfAbsent(normalizedLine, nodeLabel);
        if (firstNode != null) {
            findings.add("Duplicate NPC line in sampled path `" + sampleName + "` at " + nodeLabel
                    + "; first seen at " + firstNode + ": `" + preview(combinedLine) + "`.");
        }
    }

    private static void checkNodeBasics(
            QaFindings findings,
            String sampleName,
            String nodeLabel,
            String line,
            List<String> options,
            boolean questStageSample
    ) {
        DialogueLibrary.DialogueLine parts = DialogueLibrary.DialogueLine.from(line);
        if (parts.combined().isBlank()) {
            findings.add("Empty NPC line in `" + sampleName + "` at " + nodeLabel + ".");
        }
        if (options.isEmpty()) {
            String prefix = questStageSample ? "Quest-stage sample" : "Sample";
            findings.add(prefix + " `" + sampleName + "` has no choices at " + nodeLabel + ".");
        }
        checkRepeatedOptionLabels(findings, sampleName, nodeLabel, options);
    }

    private static void checkRepeatedOptionLabels(
            QaFindings findings,
            String sampleName,
            String nodeLabel,
            List<String> options
    ) {
        Map<String, String> firstLabels = new LinkedHashMap<>();
        for (String option : options) {
            String normalized = normalize(option);
            if (normalized.isBlank()) {
                findings.add("Empty option label in `" + sampleName + "` at " + nodeLabel + ".");
                continue;
            }
            String firstLabel = firstLabels.putIfAbsent(normalized, option);
            if (firstLabel != null) {
                findings.add("Repeated option label in `" + sampleName + "` at " + nodeLabel
                        + ": `" + firstLabel + "` / `" + option + "`.");
            }
        }
    }

    private static boolean containsOption(List<String> options, String expected) {
        String normalizedExpected = normalize(expected);
        for (String option : options) {
            if (normalize(option).contains(normalizedExpected)) {
                return true;
            }
        }
        return false;
    }

    private static String optionSummary(List<String> options) {
        if (options.isEmpty()) {
            return "_none_";
        }
        List<String> labels = new ArrayList<>();
        int limit = Math.min(options.size(), MAX_OPTION_SUMMARY);
        for (int i = 0; i < limit; i++) {
            labels.add("`" + preview(options.get(i)) + "`");
        }
        if (options.size() > limit) {
            labels.add("+" + (options.size() - limit) + " more");
        }
        return String.join(", ", labels);
    }

    private static String normalizeLine(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT)
                .replaceAll("[\"'`]", "")
                .replaceAll("\\s+", " ")
                .strip();
    }

    private static String preview(String text) {
        String cleaned = text == null ? "" : text.replaceAll("\\s+", " ").strip();
        if (cleaned.length() <= PREVIEW_LENGTH) {
            return cleaned;
        }
        return cleaned.substring(0, PREVIEW_LENGTH - 3) + "...";
    }

    private static void appendNode(StringBuilder report, String speaker, String line, List<String> options) {
        DialogueLibrary.DialogueLine parts = DialogueLibrary.DialogueLine.from(line);
        if (!parts.narration().isBlank()) {
            report.append("- ").append(speaker).append(" narration: ").append(parts.narration()).append("\n");
        }
        if (!parts.speech().isBlank()) {
            report.append("- ").append(speaker).append(" says: ").append(parts.speech()).append("\n");
        }
        if (!options.isEmpty()) {
            report.append("- Options:\n");
            for (int i = 0; i < options.size(); i++) {
                report.append("  ").append(i + 1).append(". ").append(options.get(i)).append("\n");
            }
        }
    }

    private static int findOption(List<String> options, String query) {
        String rawQuery = query == null ? "" : query.toLowerCase(Locale.ROOT).strip();
        if (rawQuery.startsWith("can we talk,")) {
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).toLowerCase(Locale.ROOT).strip().startsWith("can we talk,")) {
                    return i;
                }
            }
        }
        if ("known companion".equals(rawQuery)) {
            for (int i = 0; i < options.size(); i++) {
                String option = normalize(options.get(i));
                if (!option.startsWith("me ") && !option.startsWith("back ")) {
                    return i;
                }
            }
        }
        String normalizedQuery = normalize(query);
        for (int i = 0; i < options.size(); i++) {
            if (normalize(options.get(i)).contains(normalizedQuery)) {
                return i;
            }
        }
        return -1;
    }

    private static String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT)
                .replace("...", "")
                .replaceAll("[?.!,]", "")
                .strip();
    }

    private static int trustForPath(String path) {
        String normalizedPath = path.toLowerCase(Locale.ROOT);
        if (normalizedPath.contains("promise") || normalizedPath.contains("future")) {
            return 250;
        }
        if (normalizedPath.contains("quiet") || normalizedPath.contains("stay with me")
                || normalizedPath.contains("come here") || normalizedPath.contains("close to you")) {
            return 180;
        }
        if (normalizedPath.contains("oathstead") || normalizedPath.contains("trust")) {
            return 150;
        }
        return 100;
    }

    private static DialogueLibrary.DialogueSession session(
            Npc companion,
            Quest quest,
            int trust,
            DialogueLibrary.DialogueContext context
    ) {
        return DialogueLibrary.startSession(companion, quest, "forest", trust, new Random(seedFor(companion, trust, quest)), context);
    }

    private static long seedFor(Npc companion, int trust, Quest quest) {
        int questHash = quest == null ? 0 : quest.id.hashCode();
        return companion.name().hashCode() * 31L + trust * 17L + questHash;
    }

    private static DialogueLibrary.DialogueContext contextFor(Npc companion, List<String> knownNames, int trust, boolean repeatSample) {
        boolean recruited = trust >= 20;
        boolean stationed = trust >= 100;
        boolean romanced = trust >= 180;
        boolean married = trust >= 250;
        String role = roleFor(companion);
        Map<String, Integer> topicCounts = repeatSample
                ? Map.of("checkin", 3, "feeling", 2, "values", 2, "opinion", 1, "oathstead", 1)
                : Map.of();
        Map<String, String> outcomes = outcomeMap(companion, trust);
        return new DialogueLibrary.DialogueContext(
                recruited,
                trust >= 20,
                stationed,
                romanced,
                married,
                stationed ? "Oathstead " + role : "",
                role,
                knownNames.stream().filter(name -> !name.equals(companion.name())).toList(),
                List.of("Fought beside the player after a hard ambush.", "Saw the player choose mercy when truth was easier to weaponize."),
                romanced,
                trust >= 180 ? "the inn table near Oathstead's road" : "",
                approvalMotive(companion),
                trust >= 250,
                trust >= 100 ? requestFor(companion) : "",
                trust >= 150,
                trust >= 180,
                trust >= 250,
                pendingMilestoneTrust(trust),
                pendingMilestoneLabel(trust),
                topicCounts,
                outcomes
        );
    }

    private static int pendingMilestoneTrust(int trust) {
        if (trust < 50) {
            return 50;
        }
        if (trust < 100) {
            return 100;
        }
        if (trust < 150) {
            return 150;
        }
        if (trust < 180) {
            return 180;
        }
        if (trust < 250) {
            return 250;
        }
        return 0;
    }

    private static String pendingMilestoneLabel(int trust) {
        return switch (pendingMilestoneTrust(trust)) {
            case 50 -> "where we stand";
            case 100 -> "what changed between us";
            case 150 -> "staying together by choice";
            case 180 -> "what this feeling is becoming";
            case 250 -> "the future";
            default -> "";
        };
    }

    private static String pendingMilestonePrompt(int trust) {
        return switch (pendingMilestoneTrust(trust)) {
            case 50 -> "Can we talk about where we stand?";
            case 100 -> "Can we talk about what changed between us?";
            case 150 -> "Can we talk about staying together by choice?";
            case 180 -> "Can we talk about what this feeling is becoming?";
            case 250 -> "Can we talk about the future?";
            default -> "";
        };
    }

    private static String roleFor(Npc companion) {
        return switch (companion.recruitId()) {
            case "seraphine" -> "ledger room";
            case "maera" -> "map desk";
            case "cassia" -> "watch gate";
            case "lyra" -> "clinic corner";
            case "samir" -> "lantern post";
            case "aria" -> "roadwatch";
            case "vesper" -> "winter garden";
            case "rafiq" -> "practice yard";
            case "calder" -> "repair yard";
            default -> "hall";
        };
    }

    private static String requestFor(Npc companion) {
        return switch (companion.recruitId()) {
            case "seraphine" -> "Find a quiet hour where no one asks her to sign anything.";
            case "maera" -> "Bring her a blank map margin and a rumor that contradicts the official record.";
            case "cassia" -> "Walk the watch line with her after dusk.";
            case "lyra" -> "Restock clean cloth before the next patient arrives.";
            case "samir" -> "Visit a place where the light is ordinary and let him talk without doctrine.";
            case "aria" -> "Check a road marker with her before morning fog.";
            case "vesper" -> "Sit somewhere warm without demanding that she thaw quickly.";
            case "rafiq" -> "Share wine where no creditor can find the table.";
            case "calder" -> "Inspect the low bridge after rain and say what you honestly see.";
            default -> "";
        };
    }

    private static String approvalMotive(Npc companion) {
        return switch (companion.recruitId()) {
            case "seraphine" -> "free choice";
            case "maera" -> "evidence";
            case "cassia" -> "steadiness";
            case "lyra" -> "mercy";
            case "samir" -> "honest faith";
            case "aria" -> "quiet competence";
            case "vesper" -> "patient warmth";
            case "rafiq" -> "honest risk";
            case "calder" -> "shared weight";
            default -> "trust";
        };
    }

    private static Map<String, String> outcomeMap(Npc companion, int trust) {
        if (trust < 150) {
            return Map.of();
        }
        Map<String, String> outcomes = new LinkedHashMap<>();
        switch (companion.recruitId()) {
            case "seraphine" -> {
                outcomes.put("seraphine_first_lie", "witness_first");
                outcomes.put("seraphine_clerk_truth", "witness_protected");
                outcomes.put("seraphine_red_notary", "names_reclaimed");
                outcomes.put("seraphine_oathstead_promise", "chosen_daily");
            }
            case "maera" -> outcomes.put("maera_oathstead_archive", "open-record");
            case "cassia" -> outcomes.put("cassia_oathstead_duty", "chosen-duty");
            case "lyra" -> outcomes.put("lyra_oathstead_clinic", "care-first");
            case "aria" -> {
                outcomes.put("aria_false_trail_choice", "protect");
                outcomes.put("aria_oathstead_roadmarks", "hidden-markers");
                outcomes.put("aria_sister_truth", "hope-named");
                outcomes.put("aria_oathstead_future", "roadwatch");
            }
            case "vesper" -> {
                outcomes.put("vesper_first_root", "truth");
                outcomes.put("vesper_practical_care", "protect");
                outcomes.put("vesper_family_grove", "accountability");
                outcomes.put("vesper_buried_spring", "mercy");
                outcomes.put("vesper_spring_return", "protect");
                outcomes.put("vesper_oathstead_growth", "patient-bloom");
            }
            case "rafiq" -> outcomes.put("rafiq_oathstead_chance", "stay-before-earned");
            case "calder" -> outcomes.put("calder_oathstead_work", "shared-foundation");
            default -> {
            }
        }
        return outcomes;
    }

    private record CompanionQa(Npc companion, Quest firstQuest, List<String> findings) {
    }

    private static final class QaFindings {
        private final List<String> findings = new ArrayList<>();

        private void add(String finding) {
            if (findings.size() >= MAX_FINDINGS_PER_COMPANION || findings.contains(finding)) {
                return;
            }
            findings.add(finding);
        }

        private List<String> values() {
            return List.copyOf(findings);
        }
    }
}
