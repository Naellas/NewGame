package com.alderfall.game.ui;

import com.alderfall.game.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class QuestLogRenderer {
    private final AssetStore assets;
    private final GameState state;
    private final Effects effects;
    private int travelPartyPage;

    public QuestLogRenderer(AssetStore assets, GameState state, Effects effects) {
        this.assets = assets;
        this.state = state;
        this.effects = effects;
    }


    private static String shortText(String text, int maxChars) {
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, Math.max(0, maxChars - 3)) + "...";
    }

    private static List<String> wrappedTooltipLines(FontMetrics metrics, String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (metrics.stringWidth(candidate) > width && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    private static String fitWithEllipsis(FontMetrics metrics, String text, int width) {
        String output = text;
        while (metrics.stringWidth(output + "...") > width && output.length() > 1) {
            output = output.substring(0, output.length() - 1);
        }
        return output + (output.length() < text.length() ? "..." : "");
    }

    public void drawQuestLog(Graphics2D g) {
        int panelW = Math.min(1120, Math.max(760, effects.gameAreaWidth() - 96));
        int panelX = effects.gameAreaCenteredX(panelW);
        int panelY = 58;
        int panelH = Math.min(780, Math.max(650, effects.viewHeight() - 104));
        effects.drawOverlayBase(g, panelX, panelY, panelW, panelH);

        int activeCount = questLogEntries(false).size();
        int completedCount = questLogEntries(true).size();
        List<Quest> quests = questLogEntries(effects.questLogCompletedTab());
        normalizeSelectedQuestLogId(quests);

        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(244, 239, 220));
        g.drawString("Quest Log", panelX + 36, panelY + 46);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(new Color(190, 196, 211));
        g.drawString("Accepted work, field notes, rewards, and where the next step lives.", panelX + 36, panelY + 72);

        int tabY = panelY + 92;
        drawQuestLogTab(g, panelX + 36, tabY, 148, "Active", activeCount, !effects.questLogCompletedTab());
        drawQuestLogTab(g, panelX + 194, tabY, 166, "Completed", completedCount, effects.questLogCompletedTab());
        effects.actionButton(g, panelX + panelW - 122, tabY, 86, 30, "Close", state::toggleQuestLog,
                new Color(58, 72, 100), new Color(107, 126, 166), true);

        int contentY = panelY + 134;
        int contentH = panelH - 190;
        int listW = Math.min(410, Math.max(340, panelW * 38 / 100));
        int listX = panelX + 36;
        int detailX = listX + listW + 24;
        int detailW = panelX + panelW - 36 - detailX;
        drawQuestLogList(g, quests, listX, contentY, listW, contentH);
        drawQuestLogDetails(g, selectedQuestLogQuest(quests), detailX, contentY, detailW, contentH);

        g.setColor(new Color(196, 198, 205));
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.drawString("Q or Esc close", panelX + 40, panelY + panelH - 24);
        g.drawString("Sorted by tracked, ready, story, companion, progress", detailX, panelY + panelH - 24);
    }

    private void drawQuestLogTab(Graphics2D g, int x, int y, int w, String label, int count, boolean active) {
        effects.actionButton(g, x, y, w, 30, label + " (" + count + ")", () -> {
            effects.setQuestLogCompletedTab("Completed".equals(label));
            effects.setQuestLogScroll(0);
            effects.setSelectedQuestLogId("");
        }, active ? new Color(79, 90, 60) : new Color(46, 54, 72),
                active ? new Color(139, 154, 96) : new Color(91, 103, 132), true);
    }

    private void drawQuestLogList(Graphics2D g, List<Quest> quests, int x, int y, int w, int h) {
        g.setColor(new Color(9, 12, 18, 210));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(74, 82, 105));
        g.drawRoundRect(x, y, w, h, 8, 8);

        if (quests.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 15));
            g.setColor(new Color(205, 210, 222));
            String empty = effects.questLogCompletedTab() ? "No completed quests yet." : "No active quests yet.";
            effects.drawWrapped(g, empty, x + 18, y + 34, w - 36, 18, 2);
            return;
        }

        int rowH = 86;
        int gap = 10;
        int visibleRows = Math.max(1, (h - 28) / (rowH + gap));
        int maxScroll = Math.max(0, quests.size() - visibleRows);
        effects.setQuestLogScroll(effects.clamp(effects.questLogScroll(), 0, maxScroll));
        int start = Math.min(effects.questLogScroll(), Math.max(0, quests.size() - 1));
        int end = Math.min(quests.size(), start + visibleRows);
        int rowY = y + 14;
        for (int i = start; i < end; i++) {
            drawQuestLogRow(g, quests.get(i), x + 12, rowY, w - 24, rowH);
            rowY += rowH + gap;
        }
        if (maxScroll > 0) {
            int trackX = x + w - 10;
            int trackY = y + 14;
            int trackH = h - 28;
            int thumbH = Math.max(24, trackH * visibleRows / Math.max(visibleRows + maxScroll, 1));
            int thumbTravel = Math.max(1, trackH - thumbH);
            int thumbY = trackY + (int) Math.round(effects.questLogScroll() / (double) maxScroll * thumbTravel);
            g.setColor(new Color(38, 43, 58));
            g.fillRoundRect(trackX, trackY, 4, trackH, 4, 4);
            g.setColor(new Color(126, 141, 184));
            g.fillRoundRect(trackX, thumbY, 4, thumbH, 4, 4);
        }
    }

    private void drawQuestLogRow(Graphics2D g, Quest quest, int x, int y, int w, int h) {
        Color accent = questAccent(quest);
        boolean selected = quest.id.equals(effects.selectedQuestLogId());
        Rectangle bounds = new Rectangle(x, y, w, h);
        effects.addButton(bounds, "quest-log-row:" + quest.id, () -> {
            effects.setSelectedQuestLogId(quest.id);
            effects.repaintPanel();
        });
        boolean hovered = effects.hoverPoint() != null && bounds.contains(effects.hoverPoint());

        Color fill = selected ? new Color(28, 34, 46, 238) : new Color(14, 17, 25, 218);
        if (hovered) {
            fill = effects.blend(fill, Color.WHITE, 0.08);
        }
        g.setColor(fill);
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(selected ? accent : new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 145));
        g.drawRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), selected ? 90 : 50));
        g.fillRect(x + 1, y + 1, 5, h - 2);

        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.setColor(accent);
        effects.drawWrapped(g, quest.title, x + 14, y + 22, w - 126, 16, 1);
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.setColor(new Color(226, 229, 238));
        drawRightAligned(g, isTrackedQuest(quest) ? "Tracked" : shortQuestTypeLabel(quest), x + w - 12, y + 21);

        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(191, 198, 212));
        effects.drawWrapped(g, questStatusLine(quest), x + 14, y + 43, w - 28, 14, 1);
        drawQuestProgressBar(g, quest, x + 14, y + h - 24, Math.min(190, w - 28), accent);
        g.setColor(new Color(171, 179, 196));
        drawRightAligned(g, quest.progress + "/" + quest.activeNeeded(), x + w - 14, y + h - 14);
    }

    private void drawQuestLogDetails(Graphics2D g, Quest quest, int x, int y, int w, int h) {
        g.setColor(new Color(9, 12, 18, 222));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(74, 82, 105));
        g.drawRoundRect(x, y, w, h, 8, 8);
        if (quest == null) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 15));
            g.setColor(new Color(205, 210, 222));
            effects.drawWrapped(g, "Select a quest to view field notes, giver context, dialogue, rewards, and next steps.",
                    x + 22, y + 34, w - 44, 18, 4);
            return;
        }

        Shape oldClip = g.getClip();
        g.setClip(new Rectangle(x + 1, y + 1, w - 2, h - 2));
        Color accent = questAccent(quest);
        int contentX = x + 24;
        int contentW = w - 48;
        int cursorY = y + 34;
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.setColor(accent);
        effects.drawWrapped(g, quest.title, contentX, cursorY, contentW - 130, 24, 2);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(new Color(228, 232, 241));
        drawRightAligned(g, questTypeLabel(quest), x + w - 24, cursorY - 2);
        cursorY += 42;

        if (!quest.completed) {
            String trackLabel = isTrackedQuest(quest) ? "Untrack" : "Track";
            effects.actionButton(g, x + w - 116, y + 46, 92, 28, trackLabel, () -> toggleTrackedQuest(quest),
                    isTrackedQuest(quest) ? new Color(86, 69, 50) : new Color(45, 60, 78),
                    isTrackedQuest(quest) ? new Color(188, 147, 74) : new Color(103, 132, 166), true);
        }

        int detailProgressW = Math.min(260, Math.max(150, contentW / 2));
        drawQuestProgressBar(g, quest, contentX, cursorY, detailProgressW, accent);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(232, 235, 242));
        if (contentW >= 430) {
            g.drawString(questStatusLine(quest), contentX + detailProgressW + 18, cursorY + 11);
            cursorY += 42;
        } else {
            effects.drawWrapped(g, questStatusLine(quest), contentX, cursorY + 32, contentW, 16, 2);
            cursorY += 64;
        }

        drawQuestDetailTab(g, contentX, cursorY, 86, "Brief", !effects.questDetailTimelineTab());
        drawQuestDetailTab(g, contentX + 96, cursorY, 108, "Dialogue", effects.questDetailTimelineTab());
        cursorY += 46;

        if (effects.questDetailTimelineTab()) {
            drawQuestDialogueTimeline(g, quest, contentX, cursorY, contentW, y + h - 24);
        } else {
            cursorY = drawQuestDetailSection(g, "Giver", questGiverDetail(quest), contentX, cursorY, contentW, 3);
            cursorY = drawQuestDetailSection(g, "Context", questContextDetail(quest), contentX, cursorY, contentW, 5);
            cursorY = drawQuestDetailSection(g, "Current Step", questObjectiveDetail(quest) + " " + questLocationHint(quest),
                    contentX, cursorY, contentW, 4);
            drawQuestDetailSection(g, "Reward", quest.rewardGold + " gold, " + quest.rewardXp + " XP.", contentX, cursorY, contentW, 2);
        }
        g.setClip(oldClip);
    }

    private void drawQuestDetailTab(Graphics2D g, int x, int y, int w, String label, boolean active) {
        effects.actionButton(g, x, y, w, 30, label, () -> {
            effects.setQuestDetailTimelineTab("Dialogue".equals(label));
            effects.repaintPanel();
        }, active ? new Color(79, 90, 60) : new Color(46, 54, 72),
                active ? new Color(139, 154, 96) : new Color(91, 103, 132), true);
    }

    private void drawQuestDialogueTimeline(Graphics2D g, Quest quest, int x, int y, int w, int bottom) {
        int rowY = y;
        rowY = drawQuestTimelineStep(g, quest, x, rowY, w, bottom, "Accepted",
                cleanQuestDialogueLine(quest, quest.activeStartDialog()), quest.accepted || quest.completed,
                !quest.completed && !quest.ready() && quest.progress == 0);
        rowY = drawQuestTimelineStep(g, quest, x, rowY, w, bottom, "In Progress",
                cleanQuestDialogueLine(quest, quest.activeProgressDialog()), quest.progress > 0 || quest.ready() || quest.completed,
                !quest.completed && !quest.ready() && quest.progress > 0);
        rowY = drawQuestTimelineStep(g, quest, x, rowY, w, bottom, "Recorded Findings",
                QuestNarrative.findings(quest), !quest.observedStages.isEmpty(), quest.ready());
        drawQuestTimelineStep(g, quest, x, rowY, w, bottom, "Completed",
                cleanQuestDialogueLine(quest, quest.activeCompleteDialog()), quest.completed, quest.completed);
    }

    private int drawQuestTimelineStep(
            Graphics2D g,
            Quest quest,
            int x,
            int y,
            int w,
            int bottom,
            String label,
            String body,
            boolean available,
            boolean current
    ) {
        if (y + 40 > bottom) {
            return y;
        }
        Color accent = current ? questAccent(quest) : available ? new Color(144, 215, 150) : new Color(100, 108, 128);
        int rowH = 84;
        g.setColor(current ? new Color(26, 32, 43, 230) : new Color(13, 16, 23, 205));
        g.fillRoundRect(x, y, w, rowH, 8, 8);
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), current ? 210 : 125));
        g.drawRoundRect(x, y, w, rowH, 8, 8);
        int dotX = x + 16;
        int dotY = y + 22;
        g.setColor(accent);
        g.fillOval(dotX - 5, dotY - 5, 10, 10);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(available ? new Color(238, 240, 246) : new Color(145, 151, 166));
        g.drawString(label + (current ? "  Current" : available ? "  Logged" : "  Pending"), x + 34, y + 26);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(available ? new Color(204, 211, 225) : new Color(126, 133, 150));
        drawQuestDetailText(g, available ? body : "This note will unlock when the quest reaches this state.",
                x + 34, y + 48, w - 50, 15, 2);
        return y + rowH + 10;
    }

    private int drawQuestDetailSection(Graphics2D g, String heading, String body, int x, int y, int w, int maxBodyLines) {
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(245, 214, 117));
        g.drawString(heading, x, y);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(204, 211, 225));
        int bodyY = y + 19;
        int drawn = drawQuestDetailText(g, body, x, bodyY, w, 16, maxBodyLines);
        return bodyY + drawn * 16 + 16;
    }

    private int drawQuestDetailText(Graphics2D g, String text, int x, int y, int width, int lineHeight, int maxLines) {
        FontMetrics metrics = g.getFontMetrics();
        List<String> lines = new ArrayList<>();
        String source = text == null || text.isBlank() ? "No notes recorded." : text.strip();
        for (String paragraph : source.split("\\R", -1)) {
            if (paragraph.isBlank()) {
                lines.add("");
            } else {
                lines.addAll(wrappedTooltipLines(metrics, paragraph, width));
            }
        }
        int count = Math.min(lines.size(), Math.max(1, maxLines));
        for (int i = 0; i < count; i++) {
            String line = lines.get(i);
            if (i == count - 1 && lines.size() > count) {
                line = fitWithEllipsis(metrics, line, width);
            }
            g.drawString(line, x, y + i * lineHeight);
        }
        return count;
    }

    public List<Quest> questLogEntries(boolean completed) {
        List<Quest> entries = new ArrayList<>();
        for (Quest quest : state.quests.values()) {
            if (completed) {
                if (quest.completed) {
                    entries.add(quest);
                }
            } else if (quest.accepted && !quest.completed) {
                entries.add(quest);
            }
        }
        entries.sort(completed ? completedQuestComparator() : activeQuestComparator());
        return entries;
    }

    private Comparator<Quest> activeQuestComparator() {
        return Comparator
                .comparing((Quest quest) -> !isTrackedQuest(quest))
                .thenComparing(quest -> !quest.ready())
                .thenComparingInt(this::questTypeSortRank)
                .thenComparing((Quest quest) -> questProgressRatio(quest), Comparator.reverseOrder())
                .thenComparing(quest -> state.questReturnLocation(quest.id))
                .thenComparing(quest -> quest.title);
    }

    private Comparator<Quest> completedQuestComparator() {
        return Comparator
                .comparingInt(this::questTypeSortRank)
                .thenComparing(quest -> state.questReturnLocation(quest.id))
                .thenComparing(quest -> quest.title);
    }

    private int questTypeSortRank(Quest quest) {
        if (quest.mainStoryQuest()) {
            return 0;
        }
        if (quest.companionQuest()) {
            return 1;
        }
        return 2;
    }

    private double questProgressRatio(Quest quest) {
        if (quest.activeNeeded() <= 0) {
            return 1.0;
        }
        return Math.max(0.0, Math.min(1.0, quest.progress / (double) quest.activeNeeded()));
    }

    private void normalizeSelectedQuestLogId(List<Quest> quests) {
        boolean found = false;
        for (Quest quest : quests) {
            if (quest.id.equals(effects.selectedQuestLogId())) {
                found = true;
                break;
            }
        }
        if (!found) {
            effects.setSelectedQuestLogId(quests.isEmpty() ? "" : quests.get(0).id);
            effects.setQuestLogScroll(0);
        }
    }

    public Quest selectedQuestLogQuest(List<Quest> quests) {
        for (Quest quest : quests) {
            if (quest.id.equals(effects.selectedQuestLogId())) {
                return quest;
            }
        }
        return quests.isEmpty() ? null : quests.get(0);
    }

    private Color questAccent(Quest quest) {
        return quest.completed ? new Color(144, 215, 150)
                : quest.mainStoryQuest() ? new Color(239, 83, 80)
                : quest.companionQuest() ? new Color(245, 157, 73)
                : new Color(164, 211, 255);
    }

    public void drawTrackedQuestHud(Graphics2D g) {
        Quest quest = trackedQuest();
        if (quest == null) {
            return;
        }
        int w = Math.min(380, Math.max(280, effects.gameAreaWidth() - 48));
        int x = 24;
        int y = 24;
        int h = 100;
        Color accent = questAccent(quest);
        g.setColor(new Color(8, 11, 17, 218));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 185));
        g.drawRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 55));
        g.fillRect(x + 1, y + 1, 5, h - 2);

        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(new Color(245, 214, 117));
        g.drawString("Tracked Quest", x + 14, y + 22);
        effects.actionButton(g, x + w - 76, y + 12, 58, 26, "Open", () -> {
            effects.setQuestLogCompletedTab(false);
            effects.setSelectedQuestLogId(quest.id);
            effects.setQuestLogScroll(0);
            state.toggleQuestLog();
        }, new Color(45, 60, 78), new Color(103, 132, 166), true);

        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.setColor(accent);
        effects.drawWrapped(g, quest.title, x + 14, y + 44, w - 100, 16, 1);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(204, 211, 225));
        effects.drawWrapped(g, quest.ready() ? questLocationHint(quest) : questStatusLine(quest), x + 14, y + 65, w - 28, 14, 1);
        drawQuestProgressBar(g, quest, x + 14, y + h - 22, Math.min(210, w - 110), accent);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(new Color(220, 225, 236));
        drawRightAligned(g, quest.progress + "/" + quest.activeNeeded(), x + w - 16, y + h - 12);
    }

    public void drawTravelPartyHud(Graphics2D g, int x, int y, int w, int h) {
        List<Actor> party = state.partyMembers();
        if (party.isEmpty()) {
            return;
        }
        g.setColor(new Color(8, 11, 17, 214));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(82, 92, 116, 190));
        g.drawRoundRect(x, y, w, h, 8, 8);

        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(230, 225, 206));
        g.drawString("Travel", x + 10, y + 18);

        List<GameState.WorldAbilityOption> options = state.worldAbilityOptions();
        int buttonY = y + 26;
        int buttonGap = 6;
        int columns = 2;
        int buttonW = (w - 20 - buttonGap) / columns;
        int buttonH = 34;
        int visible = Math.min(4, options.size());
        for (int i = 0; i < visible; i++) {
            GameState.WorldAbilityOption option = options.get(i);
            int col = i % columns;
            int row = i / columns;
            int index = i;
            drawWorldAbilityButton(g, option, index,
                    x + 10 + col * (buttonW + buttonGap),
                    buttonY + row * (buttonH + buttonGap),
                    buttonW,
                    buttonH);
        }
        if (visible == 0) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.setColor(new Color(145, 154, 174));
            effects.drawWrapped(g, "Prepare travel-ready abilities in the loadout tab.", x + 10, buttonY + 16, w - 20, 15, 2);
        }

        int chipX = x + 10;
        int abilityRows = Math.max(1, (visible + columns - 1) / columns);
        int chipY = buttonY + abilityRows * (buttonH + buttonGap);
        for (String effect : List.of("travel_speed", "gather_focus", "encounter_ward", "encounter_lure", "battle_advantage")) {
            int remaining = state.worldAbilityRemaining(effect);
            if (remaining <= 0) {
                continue;
            }
            String label = state.worldAbilityLabel(effect) + " " + remaining;
            int chipW = Math.min(w - 20, Math.max(72, g.getFontMetrics().stringWidth(label) + 18));
            if (chipX + chipW > x + w - 10) {
                break;
            }
            g.setColor(new Color(38, 58, 50, 225));
            g.fillRoundRect(chipX, chipY, chipW, 20, 7, 7);
            g.setColor(new Color(130, 207, 153));
            g.drawRoundRect(chipX, chipY, chipW, 20, 7, 7);
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            g.setColor(new Color(235, 236, 240));
            effects.drawCenteredIn(g, label, chipX, chipY + 14, chipW);
            chipX += chipW + 6;
        }

        int memberY = chipY + (chipX > x + 10 ? 26 : 0);
        int memberAreaH = Math.max(32, y + h - memberY - 10);
        int memberGap = 6;
        boolean paged = party.size() * 70 - memberGap > memberAreaH;
        if (paged) memberAreaH = Math.max(32, memberAreaH - 30);
        int capacity = Math.max(1, (memberAreaH + memberGap) / 70);
        int pages = (party.size() + capacity - 1) / capacity;
        travelPartyPage = Math.min(travelPartyPage, pages - 1);
        int first = travelPartyPage * capacity;
        int rows = Math.min(capacity, party.size() - first);
        int memberH = Math.min(180, (memberAreaH - memberGap * (rows - 1)) / rows);
        for (int row = 0; row < rows; row++) {
            drawTravelPartyMember(g, party.get(first + row), x + 10,
                    memberY + row * (memberH + memberGap), w - 20, memberH);
        }
        if (paged) {
            int pageY = y + h - 34;
            effects.actionButton(g, x + 10, pageY, 70, 24, "Previous",
                    () -> travelPartyPage--, new Color(35, 43, 58), new Color(82, 92, 116), travelPartyPage > 0);
            effects.actionButton(g, x + w - 80, pageY, 70, 24, "Next",
                    () -> travelPartyPage++, new Color(35, 43, 58), new Color(82, 92, 116), travelPartyPage < pages - 1);
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g.setColor(new Color(198, 202, 211));
            effects.drawCenteredIn(g, (travelPartyPage + 1) + " / " + pages, x + 80, pageY + 16, w - 160);
        }
    }

    private void drawWorldAbilityButton(Graphics2D g, GameState.WorldAbilityOption option, int index,
                                        int x, int y, int w, int h) {
        boolean enabled = option.actor().mp >= option.ability().cost();
        Rectangle bounds = new Rectangle(x, y, w, h);
        if (enabled) {
            effects.addButton(bounds, "world-ability:" + index, () -> {
                state.useWorldAbility(index);
                effects.repaintPanel();
            });
        }
        boolean hovered = enabled && effects.hoverPoint() != null && bounds.contains(effects.hoverPoint());
        Color accent = worldAbilityAccent(option.effectKey());
        g.setColor(enabled ? new Color(35, 43, 58, 232) : new Color(42, 44, 52, 210));
        if (hovered) {
            g.setColor(effects.blend(new Color(35, 43, 58, 232), accent, 0.22));
        }
        g.fillRoundRect(x, y, w, h, 7, 7);
        g.setColor(enabled ? accent : new Color(73, 75, 84));
        g.drawRoundRect(x, y, w, h, 7, 7);
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.setColor(enabled ? new Color(235, 236, 240) : new Color(145, 154, 174));
        effects.drawClippedString(g, (index + 1) + " " + option.ability().name(), x + 7, y + 15, w - 14);
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.setColor(enabled ? new Color(197, 205, 221) : new Color(118, 124, 138));
        effects.drawClippedString(g, shortWorldAbilityLabel(option.label()) + " - " + shortText(option.actor().name, 8),
                x + 7, y + 30, w - 14);
        effects.addTooltip(bounds, option.ability().name(),
                option.label() + "\n" + option.description(), effects.abilityIconName(option.ability()), accent);
    }

    private Color worldAbilityAccent(String effectKey) {
        return switch (effectKey) {
            case "travel_speed" -> new Color(116, 203, 151);
            case "gather_focus" -> new Color(151, 184, 98);
            case "encounter_ward" -> new Color(125, 166, 246);
            case "encounter_lure" -> new Color(232, 126, 92);
            case "battle_advantage" -> new Color(237, 185, 104);
            default -> new Color(103, 132, 166);
        };
    }

    private String shortWorldAbilityLabel(String label) {
        return switch (label) {
            case "Swift Travel" -> "Speed";
            case "Gather Focus" -> "Forage";
            case "Quiet Road" -> "Quiet";
            case "Challenge Call" -> "Lure";
            case "Opening Advantage" -> "Edge";
            default -> label;
        };
    }

    private void drawTravelPartyMember(Graphics2D g, Actor actor, int x, int y, int w, int h) {
        Rectangle bounds = new Rectangle(x, y, w, h);
        g.setColor(new Color(24, 28, 38, 232));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(82, 92, 116));
        g.drawRoundRect(x, y, w, h, 8, 8);
        effects.addPartyPortraitZone(bounds, actor);
        int portraitW = Math.min(w / 2, Math.max(70, h));
        int portraitH = Math.max(1, h - 8);
        g.drawImage(assets.portrait(dialoguePortraitSprite(actor), portraitW, portraitH), x + 4, y + 4, null);
        int textX = x + portraitW + 10;
        int textW = Math.max(34, w - portraitW - 16);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(235, 236, 240));
        int textY = y + Math.max(17, (h - 42) / 2);
        effects.drawClippedString(g, actor.name, textX, textY, textW);
        int barY = textY + 10;
        int barH = 8;
        int barGap = 14;
        drawMiniResourceBar(g, textX, barY, textW, barH, actor.hp, actor.maxHp, new Color(205, 85, 101));
        drawMiniResourceBar(g, textX, barY + barGap, textW, barH, actor.mp, actor.maxMp, new Color(91, 137, 214));
        effects.addTooltip(bounds, actor.name, partyMemberTooltip(actor),
                "sprite:" + dialoguePortraitSprite(actor), new Color(126, 154, 220));
    }

    private String partyMemberTooltip(Actor actor) {
        actor.sanitizeAbilityLoadout();
        String prepared = actor.activeAbilities().isEmpty()
                ? "none"
                : String.join(", ", actor.activeAbilities().stream().map(Ability::name).toList());
        return "HP " + actor.hp + "/" + actor.maxHp + "   MP " + actor.mp + "/" + actor.maxMp
                + "\nATK " + actor.attack + "   Armor " + actor.defense + "   DR " + actor.damageReductionBonus + "%"
                + "\nSTR " + actor.strength + "   INT " + actor.intelligence + "   DEX " + actor.dexterity
                + "\nCON " + actor.constitution + "   WIL " + actor.willpower + "   CHA " + actor.charisma
                + "\nCrit " + percent(actor.criticalChanceAgainst(null, null))
                + "   Crit Dmg " + Math.round(actor.criticalMultiplier(null) * 100.0) + "%"
                + "\nDodge " + percent(actor.dodgeChanceAgainst(null))
                + "   Parry " + percent(actor.parryChanceAgainst(null))
                + "\nPrepared: " + prepared;
    }

    private String percent(double chance) {
        return Math.round(chance * 100.0) + "%";
    }

    public String dialoguePortraitSprite(Actor actor) {
        if (actor == null) {
            return state.player.worldSprite;
        }
        String sprite = actor.sprite + "_dialogue_sprite";
        return assets.hasSprite(sprite) ? sprite : actor.worldSprite;
    }

    private void drawMiniResourceBar(Graphics2D g, int x, int y, int w, int h, int value, int max, Color fill) {
        g.setColor(new Color(40, 44, 56));
        g.fillRoundRect(x, y, w, h, 4, 4);
        int filled = max <= 0 ? 0 : Math.max(1, Math.min(w, (int) Math.round(w * value / (double) max)));
        g.setColor(fill);
        g.fillRoundRect(x, y, filled, h, 4, 4);
    }

    private Quest trackedQuest() {
        if (effects.trackedQuestId().isBlank()) {
            return null;
        }
        Quest quest = state.quests.get(effects.trackedQuestId());
        if (quest == null || !quest.accepted || quest.completed) {
            effects.setTrackedQuestId("");
            return null;
        }
        return quest;
    }

    private boolean isTrackedQuest(Quest quest) {
        return quest != null && quest.id.equals(effects.trackedQuestId()) && quest.accepted && !quest.completed;
    }

    public void toggleTrackedQuest(Quest quest) {
        if (quest == null || quest.completed) {
            return;
        }
        effects.setTrackedQuestId(isTrackedQuest(quest) ? "" : quest.id);
        state.status = effects.trackedQuestId().isBlank() ? "Quest tracking cleared." : "Tracking quest: " + quest.title + ".";
    }

    private void drawQuestProgressBar(Graphics2D g, Quest quest, int x, int y, int w, Color fill) {
        int h = 12;
        g.setColor(new Color(35, 39, 54));
        g.fillRoundRect(x, y, w, h, 5, 5);
        int filled = quest.activeNeeded() <= 0 ? w : (int) Math.round(w * Math.max(0.0, Math.min(1.0, quest.progress / (double) quest.activeNeeded())));
        g.setColor(fill);
        g.fillRoundRect(x, y, filled, h, 5, 5);
        g.setColor(new Color(78, 88, 116));
        g.drawRoundRect(x, y, w, h, 5, 5);
    }

    private String questTypeLabel(Quest quest) {
        if (quest.completed) {
            return "Completed";
        }
        if (quest.ready()) {
            return "Ready to turn in";
        }
        if (quest.mainStoryQuest()) {
            return "Main story";
        }
        if (quest.companionQuest()) {
            return "Companion";
        }
        return "Side quest";
    }

    private String shortQuestTypeLabel(Quest quest) {
        if (quest.completed) {
            return "Done";
        }
        if (quest.ready()) {
            return "Ready";
        }
        if (quest.mainStoryQuest()) {
            return "Story";
        }
        if (quest.companionQuest()) {
            return "Ally";
        }
        return "Side";
    }

    private void drawRightAligned(Graphics2D g, String text, int rightX, int baselineY) {
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, rightX - metrics.stringWidth(text), baselineY);
    }

    private String questStatusLine(Quest quest) {
        String stage = quest.stagedQuest() ? "Stage " + (quest.stageIndex + 1) + "/" + quest.stages.size() + " - " : "";
        return stage + quest.objectiveAction() + " " + quest.activeTarget() + "  " + quest.progress + "/" + quest.activeNeeded();
    }

    private String questObjectiveDetail(Quest quest) {
        if (PartyDialogue.isShared(quest)) return quest.description;
        if (quest.completed) {
            return "Finished with " + state.questGiverName(quest.id) + ". The reward has been claimed.";
        }
        int remaining = Math.max(0, quest.activeNeeded() - quest.progress);
        return switch (quest.activeObjectiveKind()) {
            case DEFEAT -> remaining + " visible " + quest.activeTarget() + " target" + (remaining == 1 ? "" : "s")
                    + " now roam the overworld. Engage them from the map or by walking into them.";
            case RESCUE -> remaining + " threatened " + quest.activeTarget() + " encounter" + (remaining == 1 ? "" : "s")
                    + " remain. Reach the marked danger and win the fight before returning.";
            case DEFEND -> "Hold " + remaining + " more " + quest.activeTarget() + " pressure point" + (remaining == 1 ? "" : "s")
                    + " by defeating the attackers at the marked site.";
            case RAID_DEFENSE -> "Repel " + quest.activeTarget() + " through the local raid defense. Win the minigame, then report back.";
            case GATHER -> "Gather " + remaining + " more " + quest.activeTarget() + " from the marked field objects.";
            case DELIVER -> "Deliver " + quest.activeTarget() + " through dialogue with the marked recipient.";
            case VISIT -> "Inspect " + remaining + " marked " + quest.activeTarget() + " point" + (remaining == 1 ? "" : "s") + ".";
            case SEARCH -> "Search " + remaining + " marked " + quest.activeTarget() + " clue" + (remaining == 1 ? "" : "s") + ".";
            case TALK -> "Talk with " + quest.activeTarget() + " and ask the question directly.";
            case ASK_AROUND -> "Ask " + remaining + " more local " + (remaining == 1 ? "person" : "people") + " about " + quest.activeTarget() + ".";
            case REPORT -> "Report what you learned to " + quest.activeTarget() + ".";
            case ESCORT -> "Reach " + remaining + " marked " + quest.activeTarget() + " waypoint" + (remaining == 1 ? "" : "s") + " safely.";
            case CHOICE -> "Resolve the choice about " + quest.activeTarget() + " through dialogue.";
        };
    }

    private String questLocationHint(Quest quest) {
        if (PartyDialogue.isShared(quest)) return "Choose any dungeon. Both named companions must take part in its boss battle.";
        if (quest.ready()) {
            return "Return to " + state.questGiverName(quest.id) + " in " + state.questReturnLocation(quest.id) + ".";
        }
        if (quest.activeObjectiveKind().combatObjective()) {
            return "Hunting ground: within riding distance of " + state.questReturnLocation(quest.id)
                    + ", favoring the creature's natural biome.";
        }
        if (quest.activeObjectiveKind().defenseMinigameObjective()) {
            return "Raid defense: " + state.world.label(quest.activeObjectiveMapId()) + ".";
        }
        if (quest.hasWorldObjective()) {
            return "Marked on the world map and vicinity map.";
        }
        return "Bring the required item back to " + state.questGiverName(quest.id) + ".";
    }

    private String questGiverDetail(Quest quest) {
        if (PartyDialogue.isShared(quest)) return PartyDialogue.pair(quest).names() + ". Shared expedition; rewards are granted on victory.";
        String giver = state.questGiverName(quest.id);
        String location = state.questReturnLocation(quest.id);
        String type = quest.mainStoryQuest() ? "Main story contact"
                : quest.companionQuest() ? "Companion request"
                : "Local request";
        String chain = quest.chainOwnerId == null || quest.chainOwnerId.isBlank()
                ? ""
                : " Arc: " + readableId(quest.chainOwnerId) + ".";
        return giver + " in " + location + ". " + type + "." + chain;
    }

    private String questContextDetail(Quest quest) {
        String map = quest.activeObjectiveMapId() == null || quest.activeObjectiveMapId().isBlank()
                ? state.questReturnLocation(quest.id)
                : state.world.label(quest.activeObjectiveMapId());
        String place = quest.activeObjectiveLocationKind() == null || quest.activeObjectiveLocationKind().isBlank()
                || "story".equals(quest.activeObjectiveLocationKind())
                ? ""
                : " The work points toward " + readableId(quest.activeObjectiveLocationKind()) + ".";
        String outcome = state.questBranchOutcome(quest);
        String branch = outcome.isBlank()
                ? ""
                : " Remembered choice: " + state.readableQuestOutcome(outcome) + ".";
        return quest.description + " " + questFlavorText(quest) + " Objective area: " + map + "." + place + branch
                + CompanionQuestContent.cargoSummary(quest);
    }

    private String questDialogueDetail(Quest quest) {
        if (quest.completed) {
            return cleanQuestDialogueLine(quest, quest.activeCompleteDialog());
        }
        if (quest.ready()) {
            return cleanQuestDialogueLine(quest, quest.activeReadyDialog());
        }
        if (quest.progress > 0) {
            return cleanQuestDialogueLine(quest, quest.activeProgressDialog());
        }
        return cleanQuestDialogueLine(quest, quest.activeStartDialog());
    }

    private String cleanQuestDialogueLine(Quest quest, String line) {
        if (line == null || line.isBlank()) {
            return "No dialogue note recorded.";
        }
        String cleaned = line.strip();
        String giver = state.questGiverName(quest.id);
        String prefix = giver + ":";
        if (cleaned.startsWith(prefix)) {
            cleaned = cleaned.substring(prefix.length()).strip();
        }
        return cleaned;
    }

    private String readableId(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String[] parts = value.replace('-', '_').split("_+");
        List<String> words = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            words.add(part.substring(0, 1).toUpperCase() + part.substring(1).toLowerCase());
        }
        return String.join(" ", words);
    }

    private String questFlavorText(Quest quest) {
        String stake = quest.mainStoryQuest()
                ? "The kingdoms will measure this by what it proves, not by how tidy it looks."
                : quest.companionQuest()
                ? "This is personal work; the reward is only the part written down."
                : "Local trouble grows teeth when everyone waits for someone else.";
        String texture = switch (quest.activeObjectiveKind()) {
            case DEFEAT -> quest.activeTarget() + " sightings have turned from rumor into route planning.";
            case RESCUE -> "Someone is alive only while the road is reached in time.";
            case DEFEND -> "Holding ground can matter as much as taking it.";
            case RAID_DEFENSE -> "A camp only becomes a home when it survives being tested.";
            case GATHER -> "Small supplies decide whether people travel, heal, and eat.";
            case DELIVER -> "A carried thing can change hands more cleanly than a rumor can.";
            case VISIT -> "Old marks and quiet places keep better records than frightened witnesses.";
            case SEARCH -> "The truth is present, but it has learned to hide in ordinary details.";
            case TALK -> "Some answers only open when a person is asked plainly.";
            case ASK_AROUND -> "Rumor becomes useful when enough separate mouths point the same way.";
            case REPORT -> "News has weight only when it reaches someone able to act.";
            case ESCORT -> "A safe arrival can be the whole victory.";
            case CHOICE -> "What matters now is not only what happened, but what the player chooses to carry forward.";
        };
        return stake + " " + texture;
    }


    public interface Effects {
        int gameAreaWidth();

        int gameAreaCenteredX(int width);

        int viewHeight();

        boolean questLogCompletedTab();

        void setQuestLogCompletedTab(boolean value);

        int questLogScroll();

        void setQuestLogScroll(int value);

        String selectedQuestLogId();

        void setSelectedQuestLogId(String value);

        boolean questDetailTimelineTab();

        void setQuestDetailTimelineTab(boolean value);

        String trackedQuestId();

        void setTrackedQuestId(String value);

        Point hoverPoint();

        void drawOverlayBase(Graphics2D g, int x, int y, int w, int h);

        void actionButton(Graphics2D g, int x, int y, int w, int h, String label, Runnable action, Color fill, Color border, boolean enabled);

        void drawWrapped(Graphics2D g, String text, int x, int y, int width, int lineHeight, int maxLines);

        void drawClippedString(Graphics2D g, String text, int x, int y, int maxWidth);

        void drawCenteredIn(Graphics2D g, String text, int x, int y, int width);

        void drawScrollIndicator(Graphics2D g, int x, int y, int h, int totalRows, int scroll, int visibleRows);

        void addButton(Rectangle bounds, String label, Runnable action);

        void addTooltip(Rectangle bounds, String title, String body, String icon, Color accent);

        void addPartyPortraitZone(Rectangle bounds, Actor actor);

        String abilityIconName(Ability ability);

        void repaintPanel();

        int clamp(int value, int min, int max);

        Color blend(Color first, Color second, double amount);
    }
}
