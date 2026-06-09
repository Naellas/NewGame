package com.alderfall.game.ui;

import com.alderfall.game.*;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SaveMenuRenderer {
    private static final DateTimeFormatter SAVE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private final GameState state;
    private final SaveSystem saveSystem;
    private final Effects effects;

    public SaveMenuRenderer(GameState state, SaveSystem saveSystem, Effects effects) {
        this.state = state;
        this.saveSystem = saveSystem;
        this.effects = effects;
    }

    private static String shortText(String text, int maxChars) {
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, Math.max(0, maxChars - 3)) + "...";
    }


    private static final class LoadFolder {
        private final String characterId;
        private final String name;
        private final String className;
        private final long latestSavedAtMillis;
        private int saveCount;
        private int maxLevel;

        private LoadFolder(SaveSystem.SaveSummary summary) {
            this.characterId = summary.characterId();
            this.name = summary.name();
            this.className = summary.className();
            this.latestSavedAtMillis = summary.savedAtMillis();
        }

        private void include(SaveSystem.SaveSummary summary) {
            saveCount++;
            maxLevel = Math.max(maxLevel, summary.level());
        }
    }

    public void drawSaveMenu(Graphics2D g, boolean overlay) {
        if (overlay) {
            g.setColor(new Color(8, 11, 16, 170));
            g.fillRect(0, 0, effects.viewWidth(), effects.viewHeight());
        }
        int w = 756;
        int h = 650;
        int x = overlay ? effects.gameAreaCenteredX(w) : effects.centeredX(w);
        int y = effects.centeredY(h);
        effects.drawOverlayBase(g, x, y, w, h);
        g.setFont(new Font("Serif", Font.BOLD, 32));
        g.setColor(new Color(244, 213, 141));
        String title = state.saveMenuCanSave ? "Save / Load Adventure" : effects.importingCharacter() ? "Import Character" : "Load Adventure";
        effects.drawCenteredIn(g, title, x, y + 48, w);

        int rowY = y + 92;
        if (state.saveMenuCanSave) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g.setColor(new Color(198, 202, 211));
            g.drawString("Save Name", x + 52, rowY + 14);
            g.setColor(new Color(22, 27, 38, 235));
            g.fillRoundRect(x + 52, rowY + 24, 376, 40, 7, 7);
            g.setColor(new Color(98, 112, 142));
            g.drawRoundRect(x + 52, rowY + 24, 376, 40, 7, 7);
            String saveName = state.pendingSaveName.isBlank() ? effects.nextDefaultSaveName() : state.pendingSaveName;
            g.setFont(new Font("SansSerif", Font.PLAIN, 18));
            g.setColor(state.pendingSaveName.isBlank() ? new Color(142, 151, 168) : new Color(244, 239, 220));
            g.drawString(shortText(saveName, 34) + (effects.frame() % 24 < 12 ? "_" : ""), x + 68, rowY + 50);
            effects.actionButton(g, x + 446, rowY + 24, 210, 40, "Save Adventure", effects::saveGame, new Color(66, 93, 49), new Color(126, 176, 95), true);
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.setColor(new Color(170, 178, 194));
            g.drawString("Character: " + state.player.name + " (" + state.player.className + ")   Folder: "
                    + SaveSystem.saveIdFor(state.player.name), x + 52, rowY + 86);
            rowY += 108;
        }

        if (!state.saveMenuCanSave) {
            drawLoadMenuContent(g, x, y, w, h, rowY);
            return;
        }

        boolean filterCurrent = state.saveMenuCanSave && effects.saveListCurrentCharacterOnly();
        List<SaveSystem.SaveSummary> summaries = filterCurrent
                ? saveSystem.listSavesForCharacter(SaveSystem.saveIdFor(state.player.name))
                : saveSystem.listSaves();
        g.setFont(new Font("SansSerif", Font.BOLD, 17));
        g.setColor(new Color(230, 225, 206));
        g.drawString(filterCurrent ? state.player.name + "'s Saves" : "All Saved Adventures", x + 52, rowY + 18);
        if (state.saveMenuCanSave) {
            effects.actionButton(g, x + w - 278, rowY - 6, 100, 28, "This Hero",
                    () -> {
                        effects.setSaveListCurrentCharacterOnly(true);
                        effects.setSaveListScroll(0);
                    },
                    effects.saveListCurrentCharacterOnly() ? new Color(66, 93, 49) : new Color(35, 39, 54),
                    effects.saveListCurrentCharacterOnly() ? new Color(126, 176, 95) : new Color(86, 98, 128),
                    true);
            effects.actionButton(g, x + w - 166, rowY - 6, 96, 28, "All Saves",
                    () -> {
                        effects.setSaveListCurrentCharacterOnly(false);
                        effects.setSaveListScroll(0);
                    },
                    !effects.saveListCurrentCharacterOnly() ? new Color(66, 93, 49) : new Color(35, 39, 54),
                    !effects.saveListCurrentCharacterOnly() ? new Color(126, 176, 95) : new Color(86, 98, 128),
                    true);
        }
        rowY += 34;
        if (summaries.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 17));
            g.setColor(new Color(198, 202, 211));
            effects.drawCenteredIn(g, "No saved adventures yet.", x, rowY + 60, w);
        } else {
            int rowH = 78;
            int listBottom = y + h - 82;
            int visibleRows = Math.max(1, (listBottom - rowY) / rowH);
            int maxScroll = Math.max(0, summaries.size() - visibleRows);
            effects.setSaveListScroll(Math.max(0, Math.min(effects.saveListScroll(), maxScroll)));
            int end = Math.min(summaries.size(), effects.saveListScroll() + visibleRows);
            int listTop = rowY;
            for (int i = effects.saveListScroll(); i < end; i++) {
                SaveSystem.SaveSummary summary = summaries.get(i);
                int buttonIndex = i + 1;
                String saveId = summary.saveId();
                g.setColor(new Color(28, 32, 43, 238));
                g.fillRoundRect(x + 52, rowY, w - 104, 68, 8, 8);
                g.setColor(new Color(82, 92, 116));
                g.drawRoundRect(x + 52, rowY, w - 104, 68, 8, 8);
                g.setFont(new Font("SansSerif", Font.BOLD, 16));
                g.setColor(new Color(235, 236, 240));
                g.drawString(buttonIndex + ". " + shortText(summary.saveName(), 34), x + 70, rowY + 24);
                g.setFont(new Font("SansSerif", Font.PLAIN, 13));
                g.setColor(new Color(176, 182, 196));
                g.drawString(summary.name() + " (" + summary.className() + ")  Level " + summary.level() + "  "
                        + state.world.label(summary.mapId()) + "  " + summary.gold() + "g", x + 70, rowY + 43);
                g.setColor(new Color(147, 154, 172));
                g.drawString("Saved " + formatSaveTime(summary.savedAtMillis()) + "  "
                        + summary.completedQuests() + " done  " + summary.activeQuests() + " active  Party "
                        + summary.partySize(), x + 70, rowY + 60);
                boolean canOverwrite = state.saveMenuCanSave
                        && summary.characterId().equals(SaveSystem.saveIdFor(state.player.name));
                if (canOverwrite) {
                    effects.actionButton(g, x + w - 292, rowY + 18, 108, 32, "Overwrite", () -> effects.requestOverwrite(summary), new Color(95, 67, 50), new Color(171, 115, 82), true);
                }
                effects.actionButton(g, x + w - 172, rowY + 18, 104, 32, "Load", () -> effects.loadGame(saveId), new Color(57, 71, 102), new Color(110, 127, 160), true);
                rowY += rowH;
            }
            if (summaries.size() > visibleRows) {
                effects.drawScrollIndicator(g, x + w - 40, listTop, listBottom - listTop, summaries.size(), effects.saveListScroll(), visibleRows);
            }
        }
        drawSaveMenuStatus(g, x + 52, y + h - 55, w - 250);
        effects.actionButton(g, x + w - 170, y + h - 58, 120, 34, "Back", effects::closeSaveMenuOrLoadFolder, new Color(48, 55, 70), new Color(89, 102, 125), true);
        if (effects.hasPendingOverwrite()) {
            drawOverwriteConfirmation(g, x, y, w, h);
        }
    }

    private void drawLoadMenuContent(Graphics2D g, int x, int y, int w, int h, int rowY) {
        if (effects.selectedLoadCharacterId().isBlank()) {
            drawLoadFolders(g, x, y, w, h, rowY);
        } else {
            drawLoadSavesForSelectedFolder(g, x, y, w, h, rowY);
        }
        drawSaveMenuStatus(g, x + 52, y + h - 55, w - 250);
        effects.actionButton(g, x + w - 170, y + h - 58, 120, 34,
                effects.selectedLoadCharacterId().isBlank() ? "Back" : "Folders",
                effects::closeSaveMenuOrLoadFolder, new Color(48, 55, 70), new Color(89, 102, 125), true);
    }

    private void drawLoadFolders(Graphics2D g, int x, int y, int w, int h, int rowY) {
        List<LoadFolder> folders = loadFoldersByLatestSave();
        g.setFont(new Font("SansSerif", Font.BOLD, 17));
        g.setColor(new Color(230, 225, 206));
        g.drawString(effects.importingCharacter() ? "Choose Character" : "Character Folders", x + 52, rowY + 18);
        rowY += 34;
        if (folders.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 17));
            g.setColor(new Color(198, 202, 211));
            effects.drawCenteredIn(g, "No saved adventures yet.", x, rowY + 60, w);
            return;
        }
        int rowH = 78;
        int listBottom = y + h - 82;
        int visibleRows = Math.max(1, (listBottom - rowY) / rowH);
        int maxScroll = Math.max(0, folders.size() - visibleRows);
        effects.setSaveListScroll(Math.max(0, Math.min(effects.saveListScroll(), maxScroll)));
        int end = Math.min(folders.size(), effects.saveListScroll() + visibleRows);
        int listTop = rowY;
        for (int i = effects.saveListScroll(); i < end; i++) {
            LoadFolder folder = folders.get(i);
            int buttonIndex = i + 1;
            g.setColor(new Color(28, 32, 43, 238));
            g.fillRoundRect(x + 52, rowY, w - 104, 68, 8, 8);
            g.setColor(new Color(82, 92, 116));
            g.drawRoundRect(x + 52, rowY, w - 104, 68, 8, 8);
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.setColor(new Color(235, 236, 240));
            g.drawString(buttonIndex + ". " + shortText(folder.name, 34), x + 70, rowY + 24);
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.setColor(new Color(176, 182, 196));
            g.drawString(folder.className + "  Level " + folder.maxLevel + "  Folder " + folder.characterId, x + 70, rowY + 43);
            g.setColor(new Color(147, 154, 172));
            g.drawString(folder.saveCount + " saves  Latest " + formatSaveTime(folder.latestSavedAtMillis), x + 70, rowY + 60);
            effects.actionButton(g, x + w - 172, rowY + 18, 104, 32, "Open",
                    () -> setSelectedLoadFolder(folder.characterId), new Color(57, 71, 102), new Color(110, 127, 160), true);
            rowY += rowH;
        }
        if (folders.size() > visibleRows) {
            effects.drawScrollIndicator(g, x + w - 40, listTop, listBottom - listTop, folders.size(), effects.saveListScroll(), visibleRows);
        }
    }

    private void drawLoadSavesForSelectedFolder(Graphics2D g, int x, int y, int w, int h, int rowY) {
        List<SaveSystem.SaveSummary> summaries = selectedLoadFolderSaves();
        g.setFont(new Font("SansSerif", Font.BOLD, 17));
        g.setColor(new Color(230, 225, 206));
        g.drawString(selectedLoadFolderTitle(summaries), x + 52, rowY + 18);
        rowY += 34;
        if (summaries.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 17));
            g.setColor(new Color(198, 202, 211));
            effects.drawCenteredIn(g, "No saves in this folder.", x, rowY + 60, w);
            return;
        }
        int rowH = 78;
        int listBottom = y + h - 82;
        int visibleRows = Math.max(1, (listBottom - rowY) / rowH);
        int maxScroll = Math.max(0, summaries.size() - visibleRows);
        effects.setSaveListScroll(Math.max(0, Math.min(effects.saveListScroll(), maxScroll)));
        int end = Math.min(summaries.size(), effects.saveListScroll() + visibleRows);
        int listTop = rowY;
        for (int i = effects.saveListScroll(); i < end; i++) {
            SaveSystem.SaveSummary summary = summaries.get(i);
            int buttonIndex = i + 1;
            String saveId = summary.saveId();
            g.setColor(new Color(28, 32, 43, 238));
            g.fillRoundRect(x + 52, rowY, w - 104, 68, 8, 8);
            g.setColor(new Color(82, 92, 116));
            g.drawRoundRect(x + 52, rowY, w - 104, 68, 8, 8);
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.setColor(new Color(235, 236, 240));
            g.drawString(buttonIndex + ". " + shortText(summary.saveName(), 34), x + 70, rowY + 24);
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.setColor(new Color(176, 182, 196));
            g.drawString(summary.name() + " (" + summary.className() + ")  Level " + summary.level() + "  "
                    + state.world.label(summary.mapId()) + "  " + summary.gold() + "g", x + 70, rowY + 43);
            g.setColor(new Color(147, 154, 172));
            g.drawString("Saved " + formatSaveTime(summary.savedAtMillis()) + "  "
                    + summary.completedQuests() + " done  " + summary.activeQuests() + " active  Party "
                    + summary.partySize(), x + 70, rowY + 60);
            String actionLabel = effects.importingCharacter() ? "Import" : "Load";
            effects.actionButton(g, x + w - 172, rowY + 18, 104, 32, actionLabel,
                    () -> effects.loadOrImportGame(saveId), effects.importingCharacter() ? new Color(89, 68, 43) : new Color(57, 71, 102),
                    effects.importingCharacter() ? new Color(171, 124, 76) : new Color(110, 127, 160), true);
            rowY += rowH;
        }
        if (summaries.size() > visibleRows) {
            effects.drawScrollIndicator(g, x + w - 40, listTop, listBottom - listTop, summaries.size(), effects.saveListScroll(), visibleRows);
        }
    }

    List<LoadFolder> loadFoldersByLatestSave() {
        Map<String, LoadFolder> folders = new LinkedHashMap<>();
        for (SaveSystem.SaveSummary summary : saveSystem.listSaves()) {
            folders.computeIfAbsent(summary.characterId(), key -> new LoadFolder(summary)).include(summary);
        }
        return new ArrayList<>(folders.values());
    }

    List<SaveSystem.SaveSummary> selectedLoadFolderSaves() {
        if (effects.selectedLoadCharacterId().isBlank()) {
            return List.of();
        }
        return saveSystem.listSavesForCharacter(effects.selectedLoadCharacterId());
    }

    private String selectedLoadFolderTitle(List<SaveSystem.SaveSummary> summaries) {
        if (summaries.isEmpty()) {
            return effects.selectedLoadCharacterId() + "'s Saves";
        }
        SaveSystem.SaveSummary latest = summaries.get(0);
        return latest.name() + "'s Saves";
    }

    void setSelectedLoadFolder(String characterId) {
        effects.setSelectedLoadCharacterId(characterId);
        effects.setSaveListScroll(0);
    }

    private void drawSaveMenuStatus(Graphics2D g, int x, int y, int w) {
        if (state.status == null || state.status.isBlank()) {
            return;
        }
        boolean failed = state.status.toLowerCase().contains("failed");
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(failed ? new Color(235, 150, 136) : new Color(181, 187, 201));
        effects.drawClippedString(g, state.status, x, y + 21, w);
    }

    private void drawOverwriteConfirmation(Graphics2D g, int x, int y, int w, int h) {
        effects.addButton(new Rectangle(x, y, w, h), "overwrite-modal", () -> {
        });
        g.setColor(new Color(5, 7, 11, 190));
        g.fillRoundRect(x + 34, y + 118, w - 68, 256, 10, 10);
        g.setColor(new Color(171, 115, 82));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(x + 34, y + 118, w - 68, 256, 10, 10);
        g.setFont(new Font("Serif", Font.BOLD, 28));
        g.setColor(new Color(244, 213, 141));
        effects.drawCenteredIn(g, "Overwrite Save?", x + 34, y + 162, w - 68);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(218, 221, 229));
        effects.drawCenteredIn(g, "This will replace \"" + shortText(effects.pendingOverwriteSaveName(), 38) + "\" with your current adventure.", x + 64, y + 206, w - 128);
        g.setColor(new Color(181, 187, 201));
        effects.drawCenteredIn(g, "A new copy will not be created.", x + 64, y + 232, w - 128);
        String saveName = state.pendingSaveName.isBlank() ? effects.nextDefaultSaveName() : state.pendingSaveName;
        g.setColor(new Color(147, 154, 172));
        effects.drawCenteredIn(g, "New name: " + shortText(saveName, 42), x + 64, y + 260, w - 128);
        effects.actionButton(g, x + 170, y + 306, 150, 38, "Cancel", effects::cancelOverwrite, new Color(48, 55, 70), new Color(89, 102, 125), true);
        effects.actionButton(g, x + w - 320, y + 306, 150, 38, "Overwrite", effects::confirmOverwriteSave, new Color(111, 58, 48), new Color(190, 103, 85), true);
    }

    private String formatSaveTime(long savedAtMillis) {
        if (savedAtMillis <= 0) {
            return "Saved recently";
        }
        return SAVE_TIME_FORMAT.format(Instant.ofEpochMilli(savedAtMillis));
    }


    public interface Effects {
        int viewWidth();

        int viewHeight();

        int centeredX(int width);

        int centeredY(int height);

        int gameAreaCenteredX(int width);

        int frame();

        boolean importingCharacter();

        boolean saveListCurrentCharacterOnly();

        void setSaveListCurrentCharacterOnly(boolean value);

        int saveListScroll();

        void setSaveListScroll(int value);

        String selectedLoadCharacterId();

        void setSelectedLoadCharacterId(String value);

        String pendingOverwriteSaveName();

        public void drawOverlayBase(Graphics2D g, int x, int y, int w, int h);

        void actionButton(Graphics2D g, int x, int y, int w, int h, String label, Runnable action, Color fill, Color border, boolean enabled);

        public void drawCenteredIn(Graphics2D g, String text, int x, int y, int width);

        public void drawClippedString(Graphics2D g, String text, int x, int y, int maxWidth);

        public void drawScrollIndicator(Graphics2D g, int x, int y, int h, int totalRows, int scroll, int visibleRows);

        void addButton(Rectangle bounds, String label, Runnable action);

        String nextDefaultSaveName();

        void saveGame();

        void requestOverwrite(SaveSystem.SaveSummary summary);

        void loadGame(String saveId);

        void loadOrImportGame(String saveId);

        void closeSaveMenuOrLoadFolder();

        boolean hasPendingOverwrite();

        void cancelOverwrite();

        void confirmOverwriteSave();
    }
}
