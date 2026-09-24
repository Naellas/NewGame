package com.alderfall.game.ui;

import com.alderfall.game.*;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class DialogueRenderer {
    private final AssetStore assets;
    private final GameState state;
    private final Effects effects;
    private final DialogueFigureAnimation figureAnimation = new DialogueFigureAnimation();
    private final long animationStartedAtNanos = System.nanoTime();

    public DialogueRenderer(AssetStore assets, GameState state, Effects effects) {
        this.assets = assets;
        this.state = state;
        this.effects = effects;
    }

    public void drawDialog(Graphics2D g) {
        if (state.roamingEventPromptActive()) {
            drawWorldEventCard(g);
            return;
        }
        Npc npc = state.activeNpc;
        if (npc == null) {
            return;
        }
        if (companionDialogueSprite(npc) != null) {
            drawCompanionDialog(g, npc);
            return;
        }
        int panelW = Math.min(1120, effects.gameAreaWidth() - 96);
        int panelH = Math.min(560, Math.max(430, effects.viewHeight() - 96));
        int panelX = effects.gameAreaCenteredX(panelW);
        int panelY = effects.viewHeight() - panelH - 76;
        effects.drawOverlayBase(g, panelX, panelY, panelW, panelH);
        effects.drawNpcPortraitCard(g, npc, panelX + 26, panelY + 32, 118, 132);

        int textX = panelX + 166;
        int textW = panelW - 214;
        g.setFont(new Font("SansSerif", Font.BOLD, 25));
        g.setColor(new Color(244, 239, 220));
        String displayName = state.npcDisplayName(npc);
        g.drawString(displayName, textX, panelY + 54);
        FontMetrics nameMetrics = g.getFontMetrics();
        effects.addTooltip(new Rectangle(textX, panelY + 30, nameMetrics.stringWidth(displayName), 30),
                displayName, npcTooltip(npc));
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.setColor(new Color(218, 220, 226));
        String line = state.revealedActiveNpcDialogLine();
        effects.drawWrapped(g, line, textX, panelY + 88, textW, 24, 3);
        Quest quest = state.questForNpc(npc);

        List<DialogOption> options = dialogOptions(npc, quest);
        int optionsY = panelY + 172;
        g.setColor(new Color(196, 198, 205));
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.drawString(state.roamingEventPromptActive() ? "Available actions" : "Conversation options", textX, optionsY - 12);
        int listW = Math.min(760, textW);
        int listH = Math.max(112, panelY + panelH - optionsY - 28);
        drawDialogOptionList(g, options, textX, optionsY, listW, listH);

        if (npc.recruitId() != null && npc.recruitCost() > 0 && !state.isRecruited(npc.recruitId())) {
            g.setColor(new Color(196, 198, 205));
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.drawString("Contract available: " + npc.recruitCost() + "g", textX + listW + 18, optionsY + 22);
        } else if (npc.recruitId() != null && state.isRecruited(npc.recruitId())) {
            g.setColor(new Color(144, 215, 150));
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.drawString("Travels with you", textX + listW + 18, optionsY + 22);
        }
    }

    private void drawWorldEventCard(Graphics2D g) {
        GameState.RoamingEventPrompt event = state.roamingEventPrompt();
        int width = Math.min(680, effects.gameAreaWidth() - 48);
        int height = Math.min(440, effects.viewHeight() - 64);
        int x = effects.gameAreaCenteredX(width), y = (effects.viewHeight() - height) / 2;
        effects.drawOverlayBase(g, x, y, width, height);
        g.setColor(new Color(236, 216, 165));
        g.setFont(new Font("SansSerif", Font.BOLD, 23));
        g.drawString(event.title(), x + 24, y + 39);
        boolean objectArt = event.sprite().startsWith("event_");
        if (objectArt) g.drawImage(assets.spriteFit(event.sprite(), 96, 96), x + width - 124, y + 54, null);
        g.setColor(new Color(222, 222, 215));
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        effects.drawWrapped(g, state.revealedActiveNpcDialogLine(), x + 24, y + 72, width - (objectArt ? 156 : 48), 22, 5);
        List<DialogOption> options = dialogOptions(state.activeNpc, null);
        drawDialogOptionList(g, options, x + 24, y + 185, width - 48, height - 205);
    }

    private void drawCompanionDialog(Graphics2D g, Npc npc) {
        DialogueLibrary.DialogueLine fullLine = state.activeNpcDialogLineParts();
        DialogueLibrary.DialogueLine revealedLine = state.revealedActiveNpcDialogLineParts();
        long animationMs = (System.nanoTime() - animationStartedAtNanos) / 1_000_000L;
        int mouth = DialogueFigureAnimation.mouthPose(revealedLine.speech(),
                revealedLine.speech().equals(fullLine.speech()), animationMs);
        boolean speaking = !revealedLine.speech().isBlank() && !revealedLine.speech().equals(fullLine.speech());
        int stageW = effects.gameAreaWidth();
        String npcSprite = companionDialogueSprite(npc);
        Color npcAccent = companionAccent(npc);
        int figureH = Math.max(720, effects.viewHeight() - 52);
        int figureW = Math.max(380, Math.min(540, stageW / 3));
        int figureY = Math.max(8, effects.viewHeight() - figureH - 10);
        int npcX = Math.max(8, stageW / 7 - figureW / 2);
        int playerX = Math.min(stageW - figureW - 8, stageW - stageW / 7 - figureW / 2);
        drawDialogueStandingSprite(g, npcSprite, state.npcDisplayName(npc), npcX, figureY, figureW, figureH, npcAccent, false, animationMs, mouth, speaking, false, 1);
        drawDialogueStandingSprite(g, playerDialogueSprite(), state.player.name, playerX, figureY, figureW, figureH, playerDialogueAccent(), false, animationMs, 0, false, speaking, -1);

        int panelW = Math.min(1080, stageW - 360);
        panelW = Math.max(720, Math.min(panelW, stageW - 48));
        int panelH = Math.min(444, Math.max(404, effects.viewHeight() - 164));
        int panelX = effects.gameAreaCenteredX(panelW);
        int panelY = effects.viewHeight() - panelH - 38;
        effects.drawOverlayBase(g, panelX, panelY, panelW, panelH);

        g.setColor(new Color(npcAccent.getRed(), npcAccent.getGreen(), npcAccent.getBlue(), 130));
        g.drawRoundRect(panelX + 2, panelY + 2, panelW - 4, panelH - 4, 8, 8);

        int textX = panelX + 32;
        int textW = panelW - 64;
        String displayName = state.npcDisplayName(npc);
        g.setFont(new Font("SansSerif", Font.BOLD, 27));
        g.setColor(new Color(244, 239, 220));
        effects.drawClippedString(g, displayName, textX, panelY + 54, textW);
        FontMetrics nameMetrics = g.getFontMetrics();
        effects.addTooltip(new Rectangle(textX, panelY + 28, Math.min(textW, nameMetrics.stringWidth(displayName)), 32),
                displayName, npcTooltip(npc), "sprite:" + npcSprite, npcAccent);

        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(npcAccent);
        effects.drawClippedString(g, companionOriginLine(npc), textX, panelY + 76, textW);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(new Color(207, 211, 222));
        effects.drawWrapped(g, companionPresenceLine(npc), textX, panelY + 100, textW, 18, 1);

        int dialogueBottom = drawCompanionDialogueLine(
                g,
                revealedLine,
                textX,
                panelY + 124,
                textW,
                npcAccent
        );

        Quest quest = state.questForNpc(npc);

        List<DialogOption> options = dialogOptions(npc, quest);
        int optionsY = Math.max(panelY + 214, dialogueBottom + 18);
        g.setColor(new Color(196, 198, 205));
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.drawString(state.roamingEventPromptActive() ? "Available actions" : "Conversation options", textX, optionsY - 12);
        int listW = textW;
        int listH = Math.max(126, panelY + panelH - optionsY - 28);
        drawDialogOptionList(g, options, textX, optionsY, listW, listH);
    }

    private int drawCompanionDialogueLine(
            Graphics2D g,
            DialogueLibrary.DialogueLine line,
            int x,
            int y,
            int w,
            Color accent
    ) {
        String narration = line == null ? "" : line.narration();
        String speech = line == null ? "" : line.speech();
        if (speech.isBlank() && narration.isBlank()) {
            speech = "...";
        }
        int blockH = 80;
        int gap = 22;
        boolean split = !narration.isBlank() && w >= 720;
        if (split) {
            int narrationW = Math.max(230, Math.min(360, (w - gap) * 38 / 100));
            int speechX = x + narrationW + gap;
            int speechW = Math.max(260, w - narrationW - gap);
            g.setFont(new Font("SansSerif", Font.ITALIC, 14));
            g.setColor(new Color(184, 191, 205));
            effects.drawWrapped(g, narration, x, y + 18, narrationW, 19, 3);
            drawCompanionSpeechBox(g, speech, speechX, y - 6, speechW, blockH, accent);
            return y + blockH;
        }
        if (!narration.isBlank()) {
            g.setFont(new Font("SansSerif", Font.ITALIC, 14));
            g.setColor(new Color(184, 191, 205));
            effects.drawWrapped(g, narration, x, y, w, 18, 2);
            y += 42;
        }
        drawCompanionSpeechBox(g, speech, x, y - 6, w, blockH - 16, accent);
        return y + blockH - 16;
    }

    private void drawCompanionSpeechBox(Graphics2D g, String speech, int x, int y, int w, int h, Color accent) {
        g.setColor(new Color(9, 13, 22, 178));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 150));
        g.drawRoundRect(x, y, w, h, 8, 8);
        g.setFont(new Font("SansSerif", Font.PLAIN, 17));
        g.setColor(new Color(238, 239, 244));
        String spoken = speech == null || speech.isBlank() ? "..." : "\"" + speech + "\"";
        effects.drawWrapped(g, spoken, x + 14, y + 27, w - 28, 22, 3);
    }

    private void drawDialogOptionList(Graphics2D g, List<DialogOption> options, int x, int y, int w, int h) {
        String scrollKey = state.activeNpc == null
                ? ""
                : state.activeNpc.mapId() + ":" + state.activeNpc.sprite() + ":" + state.activeNpc.x() + ":" + state.activeNpc.y() + ":" + options.size();
        if (!scrollKey.equals(effects.dialogOptionScrollKey())) {
            effects.setDialogOptionScroll(0);
            effects.setDialogOptionScrollKey(scrollKey);
        }
        effects.setDialogOptionScrollBounds(new Rectangle(x, y, w, h));
        g.setColor(new Color(8, 11, 18, 136));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(93, 107, 143, 145));
        g.drawRoundRect(x, y, w, h, 8, 8);
        int columns = w >= 680 ? 2 : 1;
        int rowH = columns == 2 ? 52 : 46;
        int gap = 5;
        int columnGap = columns == 2 ? 8 : 0;
        int visibleRows = Math.max(1, (h - 14 + gap) / (rowH + gap));
        int totalRows = Math.max(1, (options.size() + columns - 1) / columns);
        int maxScroll = Math.max(0, totalRows - visibleRows);
        effects.setDialogOptionScroll(effects.clamp(effects.dialogOptionScroll(), 0, maxScroll));
        int contentW = maxScroll > 0 ? w - 30 : w - 16;
        int rowW = columns == 1
                ? contentW
                : Math.max(180, (contentW - columnGap) / 2);
        int startRow = effects.dialogOptionScroll();
        int endRow = Math.min(totalRows, startRow + visibleRows);
        for (int row = startRow; row < endRow; row++) {
            int rowY = y + 7 + (row - startRow) * (rowH + gap);
            for (int col = 0; col < columns; col++) {
                int optionIndex = row * columns + col;
                if (optionIndex >= options.size()) {
                    break;
                }
                int rowX = x + 8 + col * (rowW + columnGap);
                drawDialogOptionRow(g, rowX, rowY, rowW, rowH, optionIndex + 1, options.get(optionIndex));
            }
        }
        if (maxScroll <= 0) {
            return;
        }
        int trackX = x + w - 16;
        int trackY = y + 8;
        int trackH = h - 16;
        g.setColor(new Color(35, 39, 54, 190));
        g.fillRoundRect(trackX, trackY, 6, trackH, 4, 4);
        int thumbH = Math.max(22, (int) Math.round(trackH * (visibleRows / (double) totalRows)));
        int thumbTravel = Math.max(1, trackH - thumbH);
        int thumbY = trackY + (int) Math.round(effects.dialogOptionScroll() / (double) maxScroll * thumbTravel);
        g.setColor(new Color(145, 166, 214, 190));
        g.fillRoundRect(trackX, thumbY, 6, thumbH, 4, 4);
    }

    private void drawDialogueStandingSprite(Graphics2D g, String sprite, String label, int x, int y, int w, int h, Color accent, boolean drawLabel, long animationMs, int mouth, boolean speaking, boolean listening, int direction) {
        drawDialogueFigureGlow(g, x, y, w, h, accent);
        g.setColor(new Color(7, 9, 14, 90));
        g.fillOval(x + w / 6, y + h - 32, w * 2 / 3, 18);
        figureAnimation.draw(g, assets.spriteFit(sprite, w, h - 28), sprite, x, y, animationMs, mouth, speaking, listening, direction);
        if (!drawLabel) {
            return;
        }
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        FontMetrics metrics = g.getFontMetrics();
        int labelW = Math.min(w, metrics.stringWidth(label) + 20);
        int labelX = x + (w - labelW) / 2;
        int labelY = y + h - 24;
        g.setColor(new Color(9, 11, 17, 176));
        g.fillRoundRect(labelX, labelY, labelW, 24, 8, 8);
        g.setColor(accent);
        g.drawRoundRect(labelX, labelY, labelW, 24, 8, 8);
        g.setColor(new Color(247, 240, 220));
        effects.drawClippedString(g, label, labelX + 10, labelY + 16, labelW - 20);
    }

    private void drawDialogueFigureGlow(Graphics2D g, int x, int y, int w, int h, Color accent) {
        Paint oldPaint = g.getPaint();
        Composite oldComposite = g.getComposite();
        float[] fractions = {0.0f, 0.58f, 1.0f};
        Color[] colors = {
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 70),
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 24),
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0)
        };
        g.setComposite(AlphaComposite.SrcOver);
        g.setPaint(new RadialGradientPaint(
                new Point(x + w / 2, y + h / 2),
                Math.max(w, h) * 0.56f,
                fractions,
                colors
        ));
        g.fillOval(x - w / 5, y + h / 10, w + w * 2 / 5, h - h / 8);
        g.setPaint(oldPaint);
        g.setComposite(oldComposite);
    }

    private String companionDialogueSprite(Npc npc) {
        if (npc == null || (npc.recruitId() == null && state.activePartyTalkActor == null && !storyDialogueNpc(npc))) {
            return null;
        }
        String sprite = npc.sprite() + "_dialogue_sprite";
        return assets.hasSprite(sprite) ? sprite : null;
    }

    private String playerDialogueSprite() {
        String sprite = state.player.sprite + "_dialogue_sprite";
        return assets.hasSprite(sprite) ? sprite : state.player.worldSprite;
    }

    public String battleActorSprite(Actor actor) {
        if (actor == null) {
            return null;
        }
        String expanded = actor.sprite + "_combat_v4";
        if (assets.hasSprite(expanded)) return expanded;
        expanded = actor.sprite + "_combat_v3";
        if (assets.hasSprite(expanded)) return expanded;
        String refreshed = actor.sprite + "_combat_v2";
        if (assets.hasSprite(refreshed)) return refreshed;
        String sprite = actor.sprite + "_battle_sprite";
        return assets.hasSprite(sprite) ? sprite : actor.worldSprite;
    }

    public Color playerDialogueAccent() {
        return switch (state.player.className) {
            case "Knight" -> new Color(162, 178, 204);
            case "Mage" -> new Color(126, 111, 218);
            case "Ranger" -> new Color(111, 190, 103);
            case "Cleric" -> new Color(232, 203, 116);
            case "Rogue" -> new Color(92, 170, 120);
            default -> new Color(126, 171, 228);
        };
    }

    private Color companionAccent(Npc npc) {
        return switch (companionKey(npc)) {
            case "story_maelis" -> new Color(218, 75, 72);
            case "story_selene" -> new Color(124, 142, 218);
            case "story_odrick" -> new Color(172, 185, 198);
            case "story_solari" -> new Color(232, 183, 75);
            case "story_ysra" -> new Color(95, 178, 164);
            case "story_mirella" -> new Color(89, 154, 216);
            case "story_elder_rowan" -> new Color(126, 185, 102);
            case "story_gravekeeper_hollis" -> new Color(155, 164, 176);
            case "story_captain_elric_snowrest" -> new Color(144, 190, 220);
            case "story_bellwright_nessa" -> new Color(202, 154, 82);
            case "story_ash_scribe_damar" -> new Color(218, 103, 72);
            case "aria" -> new Color(111, 190, 103);
            case "seraphine" -> new Color(212, 77, 92);
            case "maera" -> new Color(105, 151, 227);
            case "cassia" -> new Color(220, 105, 74);
            case "lyra" -> new Color(121, 205, 185);
            case "samir" -> new Color(235, 185, 76);
            case "vesper" -> new Color(170, 220, 190);
            case "rafiq" -> new Color(188, 122, 218);
            case "calder" -> new Color(164, 133, 82);
            default -> new Color(245, 157, 73);
        };
    }

    private String companionOriginLine(Npc npc) {
        String className = state.activePartyTalkActor != null
                ? state.activePartyTalkActor.className
                : storyDialogueNpc(npc)
                ? "Main Story"
                : npc.recruitId() != null && GameData.RECRUITS.containsKey(npc.recruitId())
                ? GameData.RECRUITS.get(npc.recruitId()).className()
                : "Companion";
        String origin = switch (companionKey(npc)) {
            case "story_maelis" -> "Oathstead settlement leader";
            case "story_selene" -> "Archive City royal archivist";
            case "story_odrick" -> "Highwall roadwatch commander";
            case "story_solari" -> "Sanctum sun priestess";
            case "story_ysra" -> "Belltower Bellwarden";
            case "story_mirella" -> "Riverside noble negotiator";
            case "story_elder_rowan" -> "Oakhaven orchard elder";
            case "story_gravekeeper_hollis" -> "Stonegate gravekeeper";
            case "story_captain_elric_snowrest" -> "Snowrest frost-road captain";
            case "story_bellwright_nessa" -> "Glimmerfen bellwright";
            case "story_ash_scribe_damar" -> "Redcairn ash-scribe";
            case "aria" -> "Oakhaven road scout";
            case "seraphine" -> "Riverside contract-breaker";
            case "maera" -> "Archive star-route scholar";
            case "cassia" -> "Highwall gate veteran";
            case "lyra" -> "Belltower field healer";
            case "samir" -> "Sanctum dawn witness";
            case "vesper" -> "Snowrest winter druid";
            case "rafiq" -> "Dunewick glassstep duelist";
            case "calder" -> "Mireford bridgewright";
            default -> state.world.label(npc.mapId());
        };
        return origin + " | " + className;
    }

    private String companionPresenceLine(Npc npc) {
        return switch (companionKey(npc)) {
            case "story_maelis" -> "Mud on her cloak, counted supplies at her belt, and no patience for heroic speeches.";
            case "story_selene" -> "Dark archive robes and a sealed ledger make every careful word feel like evidence.";
            case "story_odrick" -> "Frost-scored iron and roadwatch discipline turn fear into a problem with a formation.";
            case "story_solari" -> "Gold desert vestments and ember rites make her calm feel severe rather than soft.";
            case "story_ysra" -> "Reed charms and small bronze bells move with her, as if the marsh is listening too.";
            case "story_mirella" -> "River-blue formal travel clothes and a controlled stare make politics look almost honest.";
            case "story_elder_rowan" -> "Old orchard cloth, a carved stick, and practical eyes carry village memory better than books.";
            case "story_gravekeeper_hollis" -> "A dark coat, lantern, and tired hands speak for everyone whose name was cut from stone.";
            case "story_captain_elric_snowrest" -> "Winter gear, road dust, and exhaustion make him look like a commander who counts medicine before glory.";
            case "story_bellwright_nessa" -> "Rope, hooks, and a tool belt mark her as someone who trusts maintenance more than miracles.";
            case "story_ash_scribe_damar" -> "Ash-stained robes, scrolls, and watchful eyes make every ruin feel like a pending appointment.";
            case "aria" -> "Leaf-green leathers, auburn hair, and a ready bow make her look harmless only to people who are not paying attention.";
            case "seraphine" -> "Crimson silk and black leather turn every pause into negotiation; the dagger is simply the honest part.";
            case "maera" -> "Blue moonmarked robes and a heavy satchel make forbidden knowledge look almost ceremonial.";
            case "cassia" -> "Bright hair, polished steel, and a guarded throat-scarf carry Highwall's old breach into every sentence.";
            case "lyra" -> "White cloth, teal trim, and travel-worn boots say healer first, but not helpless.";
            case "samir" -> "Gold sunbursts over white vestments make his doubt feel brighter, not weaker.";
            case "vesper" -> "Pale hair, green winter cloth, and a branch staff mark her as a Snowrest druid used to harsh weather and harder questions.";
            case "rafiq" -> "Cream desert robes, a purple sash, and a curved blade give his jokes the shape of a duel.";
            case "calder" -> "Moss-brown work leathers, mud boots, and a hammer make every answer sound load-bearing.";
            default -> "Their clothes and posture say as much as their words.";
        };
    }

    private String companionKey(Npc npc) {
        if (npc == null) {
            return "";
        }
        if (npc.recruitId() != null && !npc.recruitId().isBlank()) {
            return npc.recruitId();
        }
        String sprite = npc.sprite();
        return sprite.startsWith("npc_") ? sprite.substring(4) : sprite;
    }

    private boolean storyDialogueNpc(Npc npc) {
        return npc != null && npc.sprite().startsWith("npc_story_");
    }

    private void drawDialogOptionRow(Graphics2D g, int x, int y, int w, int h, int number, DialogOption option) {
        Rectangle bounds = new Rectangle(x, y, w, h);
        boolean hovered = option.enabled() && effects.hoverPoint() != null && bounds.contains(effects.hoverPoint());
        if (option.enabled()) {
            effects.addButton(bounds, option.label(), () -> {
                if (!state.activeDialogueRevealComplete()) {
                    state.revealActiveDialogueLineInstantly();
                    effects.repaintPanel();
                    return;
                }
                option.action().run();
                effects.repaintPanel();
            });
        }
        effects.addTooltip(bounds, "Player response", option.label());
        int drawY = hovered ? y - 1 : y;
        Color fill = option.enabled() ? option.fill() : new Color(42, 44, 52);
        Color border = option.enabled() ? option.border() : new Color(73, 75, 84);
        if (hovered) {
            fill = brighten(fill, 18);
            border = brighten(border, 30);
            g.setColor(new Color(0, 0, 0, 72));
            g.fillRoundRect(x + 2, drawY + 3, w - 4, h, 6, 6);
        }
        g.setColor(fill);
        g.fillRoundRect(x, drawY, w, h, 6, 6);
        if (hovered) {
            g.setColor(new Color(border.getRed(), border.getGreen(), border.getBlue(), 70));
            g.drawRoundRect(x - 1, drawY - 1, w + 2, h + 2, 8, 8);
            g.setColor(new Color(255, 255, 255, 28));
            g.fillRoundRect(x + 1, drawY + 1, Math.max(12, w / 3), 8, 6, 6);
        }
        g.setColor(border);
        g.drawRoundRect(x, drawY, w, h, 6, 6);
        if (hovered) {
            g.setColor(new Color(border.getRed(), border.getGreen(), border.getBlue(), 200));
            g.fillRoundRect(x + 8, drawY + 7, 4, h - 14, 3, 3);
        }
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(option.enabled()
                ? hovered ? new Color(252, 252, 255) : new Color(238, 239, 244)
                : new Color(142, 146, 156));
        String numberLabel = number + ".";
        g.drawString(numberLabel, x + 18, drawY + 18);
        int labelX = x + 48;
        int maxW = w - 56;
        effects.drawWrapped(g, option.label(), labelX, drawY + 17, maxW, 16, 2);
    }

    public List<DialogOption> dialogOptions(Npc npc, Quest quest) {
        List<DialogOption> options = new ArrayList<>();
        List<String> lines = state.activeNpcDialogOptions();
        List<GameState.DialogueOptionIntent> intents = state.activeNpcDialogOptionIntents();
        for (int i = 0; i < lines.size(); i++) {
            int index = i;
            GameState.DialogueOptionIntent intent = optionIntent(intents, index);
            DialogOptionStyle style = dialogOptionStyle(intent, index == state.dialogIndex);
            options.add(new DialogOption(dialogOptionLabel(lines.get(i), i, intent), () -> state.selectDialogOption(index),
                    style.fill(), style.border(), true));
        }
        if (state.roamingEventPromptActive()) {
            return options;
        }
        if (state.activePartyTalkActor != null) {
            String questActionLabel = questActionLabel(quest);
            if (questActionLabel != null) {
                options.add(new DialogOption(dialogQuestActionLabel(quest, questActionLabel), state::handleActiveNpcQuestAction,
                        new Color(88, 73, 44), new Color(169, 137, 74), true));
            }
            options.add(new DialogOption("Leave", state::closeOverlay,
                    new Color(83, 61, 61), new Color(149, 96, 88), true));
            return options;
        }
        if (state.activeNpcIntroductionPending() || !state.activeNpcDialogueAtRoot()) {
            options.add(new DialogOption("Leave", state::closeOverlay,
                    new Color(83, 61, 61), new Color(149, 96, 88), true));
            return options;
        }
        String questActionLabel = questActionLabel(quest);
        if (questActionLabel != null) {
            options.add(new DialogOption(dialogQuestActionLabel(quest, questActionLabel), state::handleActiveNpcQuestAction,
                    new Color(88, 73, 44), new Color(169, 137, 74), true));
        }
        if (state.activeNpcCanTeachRecipe()) {
            options.add(new DialogOption("Ask for recipe advice", state::learnRecipeFromActiveNpc,
                    new Color(59, 72, 92), new Color(118, 148, 184), true));
        }
        if (activeDialogWillOpenShop(npc)) {
            options.add(new DialogOption("Open shop", state::openActiveShop,
                    new Color(52, 79, 92), new Color(92, 140, 160), true));
        }
        if (npc.recruitId() != null && npc.recruitCost() > 0 && !state.isRecruited(npc.recruitId())) {
            options.add(new DialogOption("Hire " + npc.recruitCost() + "g", state::hireActiveRecruit,
                    new Color(69, 62, 88), new Color(125, 107, 166), true));
        }
        if (state.canRecruitActiveNpcToParty()) {
            options.add(new DialogOption("Join party", state::recruitActiveNpcToParty,
                    new Color(69, 62, 88), new Color(125, 107, 166), true));
        }
        if (npc != null) {
            boolean canSettle = state.canRecruitActiveNpcToVillage();
            String settleLabel = canSettle
                    ? "Join Oathstead"
                    : "Oathstead trust " + state.npcRelationship(npc) + "/" + state.villageRecruitThreshold(npc);
            options.add(new DialogOption(settleLabel, state::recruitActiveNpcToVillage,
                    canSettle ? new Color(49, 84, 64) : new Color(44, 48, 56),
                    canSettle ? new Color(104, 170, 116) : new Color(80, 84, 96),
                    canSettle));
        }
        options.add(new DialogOption("Leave", state::closeOverlay,
                new Color(83, 61, 61), new Color(149, 96, 88), true));
        return options;
    }

    private String dialogOptionTopic(String line, int index) {
        String compact = line.replaceAll("\\s+", " ").strip();
        return compact.isBlank() ? "Talk " + (index + 1) : compact;
    }

    private GameState.DialogueOptionIntent optionIntent(List<GameState.DialogueOptionIntent> intents, int index) {
        if (index >= 0 && index < intents.size()) {
            return intents.get(index);
        }
        return new GameState.DialogueOptionIntent("topic", "", 0);
    }

    private String dialogOptionLabel(String line, int index, GameState.DialogueOptionIntent intent) {
        String topic = dialogOptionTopic(line, index);
        String prefix = intentPrefix(intent);
        if (prefix.isBlank()) {
            return topic;
        }
        String lowered = topic.toLowerCase();
        if (lowered.startsWith(prefix.toLowerCase() + ":") || lowered.startsWith(prefix.toLowerCase() + " ")) {
            return topic;
        }
        return prefix + ": " + topic;
    }

    private String intentPrefix(GameState.DialogueOptionIntent intent) {
        String kind = intent == null ? "" : intent.kind();
        return switch (kind) {
            case "quest_accept" -> "Accept quest";
            case "quest_turnin" -> "Turn in";
            case "quest_choice" -> "Choose";
            case "recruit" -> "Recruit";
            case "romance" -> "Romance";
            case "romance_end" -> "Set boundary";
            case "marriage" -> "Marriage";
            case "milestone" -> "Trust";
            case "promise" -> "Promise";
            case "scene" -> "Scene";
            default -> "";
        };
    }

    private DialogOptionStyle dialogOptionStyle(GameState.DialogueOptionIntent intent, boolean selected) {
        String kind = intent == null ? "topic" : intent.kind();
        Color fill = switch (kind) {
            case "quest_accept" -> new Color(58, 83, 48);
            case "quest_turnin" -> new Color(88, 73, 44);
            case "quest_choice" -> new Color(73, 58, 96);
            case "recruit", "romance", "marriage", "milestone", "promise", "scene" -> new Color(76, 58, 92);
            case "romance_end", "rapport_risk" -> new Color(84, 55, 54);
            case "rapport_gain" -> new Color(48, 75, 58);
            case "shop", "shop_info", "info" -> new Color(52, 72, 92);
            case "continue" -> new Color(45, 58, 76);
            default -> new Color(35, 39, 54);
        };
        Color border = switch (kind) {
            case "quest_accept" -> new Color(128, 188, 96);
            case "quest_turnin" -> new Color(169, 137, 74);
            case "quest_choice" -> new Color(156, 124, 206);
            case "recruit", "romance", "marriage", "milestone", "promise", "scene" -> new Color(151, 118, 184);
            case "romance_end", "rapport_risk" -> new Color(168, 96, 88);
            case "rapport_gain" -> new Color(104, 170, 116);
            case "shop", "shop_info", "info" -> new Color(92, 140, 170);
            case "continue" -> new Color(106, 130, 166);
            default -> new Color(86, 98, 128);
        };
        if (!selected) {
            return new DialogOptionStyle(fill, border);
        }
        return new DialogOptionStyle(brighten(fill, 24), brighten(border, 34));
    }

    private Color brighten(Color color, int amount) {
        return new Color(
                Math.min(255, color.getRed() + amount),
                Math.min(255, color.getGreen() + amount),
                Math.min(255, color.getBlue() + amount),
                color.getAlpha()
        );
    }

    private record DialogOptionStyle(Color fill, Color border) {
    }

    public boolean activeDialogWillOpenShop(Npc npc) {
        return state.activeShop != null || state.activeNpcCanOpenAssignedShop();
    }

    public String npcTooltip(Npc npc) {
        int count = state.npcKnowledgeCount(npc);
        int level = state.npcKnowledgeLevel(npc);
        String knowledge = "Knowledge: " + npcKnowledgeLabel(level) + " (" + count + (count == 1 ? " clue" : " clues")
                + ", INT " + state.player.intelligence + ").";
        if (!state.knowsNpc(npc)) {
            return knowledge + "\nYou have not been introduced. Talk to learn this person's name.";
        }
        List<String> parts = new ArrayList<>();
        parts.add(knowledge);
        String role = npcRoleLabel(npc);
        if (!role.isBlank()) {
            parts.add("Role: " + role + ".");
        }
        parts.add("Skills: " + npcSkillSummary(npc) + ".");
        parts.add("Equipment: " + npcEquipmentSummary(npc) + ".");
        if (npc.recruitId() != null && npc.recruitCost() > 0) {
            parts.add(state.isRecruited(npc.recruitId())
                    ? "Status: travels with you."
                    : "Contract: " + npc.recruitCost() + " gold.");
        }
        return String.join(" ", parts);
    }

    private String npcKnowledgeLabel(int level) {
        return switch (level) {
            case 0 -> "Unknown";
            case 1 -> "Introduced";
            case 2 -> "Acquainted";
            case 3 -> "Personal clues";
            case 4 -> "Trusted";
            default -> "Well known";
        };
    }

    private String npcRoleLabel(Npc npc) {
        if (npc.recruitId() != null) {
            GameData.RecruitSpec spec = GameData.RECRUITS.get(npc.recruitId());
            if (spec != null) {
                return spec.className();
            }
        }
        return NpcIdentity.role(npc).label;
    }

    private String npcSkillSummary(Npc npc) {
        List<String> parts = new ArrayList<>();
        for (Profession profession : Profession.ALL) {
            int level = npc.professionLevel(profession.id());
            if (level > 1) {
                parts.add(profession.label() + " " + level);
            }
        }
        return parts.isEmpty() ? "general survival 1" : String.join(", ", parts);
    }

    private String npcEquipmentSummary(Npc npc) {
        if (npc.recruitId() != null) {
            GameData.RecruitSpec spec = GameData.RECRUITS.get(npc.recruitId());
            if (spec != null && !spec.inventory().isEmpty()) {
                return itemList(spec.inventory(), 3);
            }
        }
        if (Shop.forNpc(npc) != null) {
            Shop shop = Shop.forNpc(npc);
            if (shop != null) {
                List<String> stock = state.shopStock(shop);
                if (!stock.isEmpty()) {
                    List<String> names = new ArrayList<>();
                    for (String key : stock) {
                        names.add(GameData.itemName(key));
                        if (names.size() >= 3) {
                            break;
                        }
                    }
                    return String.join(", ", names);
                }
            }
        }
        return NpcIdentity.role(npc).equipment;
    }

    private String itemList(Map<String, Integer> items, int limit) {
        List<String> names = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            String count = entry.getValue() > 1 ? entry.getValue() + " " : "";
            names.add(count + GameData.itemName(entry.getKey()));
            if (names.size() >= limit) {
                break;
            }
        }
        return String.join(", ", names);
    }

    private String questActionLabel(Quest quest) {
        if (quest == null || quest.completed) {
            return null;
        }
        if (!quest.accepted) {
            return null;
        }
        if (quest.ready()) {
            return "Turn In";
        }
        return null;
    }

    private String dialogQuestActionLabel(Quest quest, String fallback) {
        if (quest == null) {
            return fallback;
        }
        if (!quest.accepted) {
            return DialogueLibrary.questCommitLabel(quest);
        }
        if (quest.ready()) {
            return quest.companionQuest() ? "Turn in quest: Tell them what happened" : "Turn in quest: Report back";
        }
        if (quest.companionQuest()) {
            return DialogueLibrary.questTopicLabel(quest);
        }
        return "Talk through " + quest.title;
    }


    public interface Effects {
        int gameAreaWidth();

        int gameAreaCenteredX(int width);

        int viewHeight();

        int dialogOptionScroll();

        void setDialogOptionScroll(int value);

        Rectangle dialogOptionScrollBounds();

        void setDialogOptionScrollBounds(Rectangle bounds);

        String dialogOptionScrollKey();

        void setDialogOptionScrollKey(String key);

        void drawOverlayBase(Graphics2D g, int x, int y, int w, int h);

        void drawWrapped(Graphics2D g, String text, int x, int y, int width, int lineHeight, int maxLines);

        void drawClippedString(Graphics2D g, String text, int x, int y, int maxWidth);

        void addTooltip(Rectangle bounds, String title, String body);

        void addTooltip(Rectangle bounds, String title, String body, String icon, Color accent);

        void addButton(Rectangle bounds, String label, Runnable action);

        void repaintPanel();

        int clamp(int value, int min, int max);

        void drawNpcPortraitCard(Graphics2D g, Npc npc, int x, int y, int w, int h);

        Point hoverPoint();
    }
}
