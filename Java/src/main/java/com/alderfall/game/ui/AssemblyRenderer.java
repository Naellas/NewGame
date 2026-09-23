package com.alderfall.game.ui;

import com.alderfall.game.*;
import com.alderfall.game.inventory.Equipment;
import java.awt.*;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Two-stage workshop: prepare reusable components, then assemble selected parts. */
final class AssemblyRenderer {
    private final AssetStore assets;
    private final GameState state;
    private final InventoryRenderer.Effects effects;
    private int blueprintIndex;
    private AssemblyCrafting.Slot focusedSlot = AssemblyCrafting.Slot.BLADE;
    private boolean assembling;
    private boolean choosingBlueprint;
    private AssemblyCrafting.Slot pickerSlot;
    private int pickerPage;
    private boolean drawingPicker;
    private static final int PICKER_ROWS = 12;
    private enum Sort { TIER("Tier"), AVAILABILITY("Availability"), LEVEL("Level"), RARITY("Rarity");
        final String label;
        Sort(String label) { this.label = label; }
    }
    private Sort pickerSort = Sort.TIER;
    private boolean descending;
    private final Map<String, java.awt.image.BufferedImage> missingSprites = new java.util.LinkedHashMap<>() {
        protected boolean removeEldestEntry(Map.Entry<String, java.awt.image.BufferedImage> entry) { return size() > 128; }
    };
    private final java.util.Set<AssemblyCrafting.Slot> plannedOptional = java.util.EnumSet.noneOf(AssemblyCrafting.Slot.class);
    private final Map<AssemblyCrafting.Slot, Integer> materialIndices = new EnumMap<>(AssemblyCrafting.Slot.class);
    private final Map<AssemblyCrafting.Slot, String> selectedParts = new EnumMap<>(AssemblyCrafting.Slot.class);

    AssemblyRenderer(AssetStore assets, GameState state, InventoryRenderer.Effects effects) { this.assets = assets; this.state = state; this.effects = effects; }

    void draw(Graphics2D g, int x, int y) {
        AssemblyCrafting.Blueprint blueprint = AssemblyCrafting.BLUEPRINTS.get(blueprintIndex);
        text(g, "Equipment Workshop", x + 36, y + 48, 25);
        button(g, x + 36, y + 70, 230, "Type: " + blueprint.label() + " >", () -> choosingBlueprint = true, true);
        button(g, x + 282, y + 70, 220, assembling ? "Stage 2: Assemble >" : "Stage 1: Prepare parts >",
                () -> assembling = !assembling, true);
        text(g, state.config.creativeCraftingMode ? "CREATIVE: instant / free / no XP" : "Station: " + (state.currentWorkstation() == null ? "Field" : state.currentWorkstation().label()), x + 525, y + 91, 13);
        text(g, assembling ? "Select prepared parts to see your exact result and quality calculation."
                : "Select materials. Click a slot heading to inspect its job and quality calculation.", x + 36, y + 126, 13);
        int rowY = y + 143;
        Map<AssemblyCrafting.Slot, String> selection = new EnumMap<>(AssemblyCrafting.Slot.class);
        CraftingCalculation.Result focusedCalculation = null;
        for (AssemblyCrafting.Slot slot : blueprint.slots()) {
            g.setColor(slot == focusedSlot ? new Color(37, 46, 57) : new Color(26, 31, 43));
            g.fillRoundRect(x + 32, rowY, 535, 92, 8, 8);
            button(g, x + 44, rowY + 5, 295, slot.label + (slot.optional() ? " (optional)" : " (required)")
                    + " / " + slot.profession.label(), () -> focusedSlot = slot, true);
            if (assembling) {
                List<AssemblyCrafting.Component> owned = AssemblyCrafting.ownedParts(state.player, slot);
                String selected = selectedParts.get(slot);
                if (selected != null && !state.player.hasItem(selected)) { selectedParts.remove(slot); selected = null; }
                if (selected == null && !slot.optional() && !owned.isEmpty()) {
                    selected = owned.get(0).key(); selectedParts.put(slot, selected);
                }
                if (selected != null) selection.put(slot, selected);
                button(g, x + 44, rowY + 37, 508, selected == null ? "Select " + slot.label + "..."
                        : GameData.itemName(selected) + " - Select...", () -> openPicker(slot), true);
                line(g, selected == null ? "Prepare a compatible component in Stage 1."
                        : AssemblyCrafting.component(selected).material().summary(), x + 44, rowY + 83, 508, 11);
            } else {
                List<MaterialCatalog.Material> materials = AssemblyCrafting.materials(slot);
                int index = Math.floorMod(materialIndices.getOrDefault(slot, 0), materials.size());
                MaterialCatalog.Material material = materials.get(index);
                CraftingSystem.Recipe recipe = AssemblyCrafting.componentRecipe(state.player, slot, material.key());
                CraftingCalculation.Result calc = CraftingCalculation.calculate(state.player, slot.profession, material.tier());
                if (slot == focusedSlot) focusedCalculation = calc;
                if (!slot.optional() || plannedOptional.contains(slot)) selection.put(slot, recipe.resultKey());
                button(g, x + 44, rowY + 37, 508, slot.optional() && !plannedOptional.contains(slot) ? "Select " + slot.label + " material..." : GameData.itemName(material.key()) + " ("
                        + state.player.inventory.getOrDefault(material.key(), 0) + "/" + slot.amount + ") - Select...",
                        () -> openPicker(slot), true);
                boolean ready = (!slot.optional() || plannedOptional.contains(slot)) && (state.config.creativeCraftingMode || (recipe.workstation() == state.currentWorkstation()
                        && CraftingSystem.canCraft(state.player, recipe))) && !state.crafting.active();
                button(g, x + 349, rowY + 5, 203, "Prepare " + slot.label,
                        () -> state.craftComponent(slot, material.key()), ready);
                line(g, "T" + material.tier() + " | " + calc.quality().label + " | Needs " + slot.profession.label() + " "
                        + material.requiredSkill() + " | " + recipe.workstation().label(), x + 44, rowY + 83, 508, 11);
            }
            rowY += 99;
        }
        CraftingSystem.Recipe recipe = AssemblyCrafting.assemblyRecipe(state.player, blueprint, selection);
        Equipment gear = recipe == null ? null : GameData.equipment(recipe.resultKey());
        CraftingCalculation.Result calc = assembling
                ? AssemblyCrafting.assemblyCalculation(state.player, blueprint, selection) : focusedCalculation;
        drawPreview(g, x + 585, y + 143, blueprint, gear, calc, selection, recipe);
        text(g, "Quality score: " + AssemblyCrafting.Quality.STANDARD.label + " <30 | " + AssemblyCrafting.Quality.FINE.label + " 30 | Masterwork 50", x + 36, y + 560, 12);
        text(g, "Rare 70 | Legendary 100. Required parts limit final quality.", x + 36, y + 579, 12);
        line(g, state.status, x + 36, y + 604, 866, 12);
        if (pickerSlot != null) drawPicker(g, x, y);
        if (choosingBlueprint) drawTypePicker(g, x, y);
    }

    private void drawPreview(Graphics2D g, int x, int y, AssemblyCrafting.Blueprint blueprint, Equipment gear,
                             CraftingCalculation.Result calc, Map<AssemblyCrafting.Slot, String> selection,
                             CraftingSystem.Recipe recipe) {
        g.setColor(new Color(21, 27, 37)); g.fillRoundRect(x, y, 320, 440, 10, 10);
        text(g, assembling ? "Final item preview" : "Planned item preview", x + 14, y + 23, 16);
        // Resolve through the same category/material appearance renderer as inventory.
        String icon = GameData.itemIcon(gear == null ? AssemblyCrafting.categoryPreviewKey(blueprint) : gear.key());
        if (!assets.hasSprite(icon)) icon = blueprint.icon();
        g.drawImage(assets.sprite(icon, 90), x + 10, y + 33, null);
        wrapped(g, gear == null ? blueprint.label() + " (incomplete)" : gear.name(), x + 111, y + 54, 196, 14, 3);
        line(g, gear == null ? "Choose required parts" : gear.rarityLine() + " / Equip Lv " + gear.minLevel(),
                x + 111, y + 110, 196, 12);
        wrapped(g, gear == null ? "Stats appear when all required parts are selected."
                : String.join(" / ", gear.statLines()), x + 14, y + 139, 290, 12, 3);
        text(g, assembling ? "Assembly quality calculation" : focusedSlot.label + " quality calculation", x + 14, y + 196, 14);
        if (calc != null) {
            int lineY = y + 217;
            for (String line : calc.lines()) { line(g, line, x + 14, lineY, 290, 12); lineY += 17; }
            line(g, calc.nextQuality(), x + 14, y + 340, 290, 12);
        }
        if (assembling && gear != null) {
            String quality = gear.key().split("~", 4)[2];
            AssemblyCrafting.Quality actual = AssemblyCrafting.Quality.valueOf(quality);
            line(g, "Final: " + actual.label + " / " + actual.percent + "% stats", x + 14, y + 360, 290, 13);
            String limit = calc != null && actual.ordinal() < calc.quality().ordinal()
                    ? "Limited by required component quality" : "Within required component quality";
            line(g, limit, x + 14, y + 379, 290, 11);
            int need = recipe.professionRequirements().get(blueprint.profession().id());
            line(g, "Needs " + blueprint.profession().label() + " " + need + " / " + blueprint.station().label(), x + 14, y + 396, 290, 11);
            button(g, x + 14, y + 403, 290, "Assemble", () -> state.assembleEquipment(blueprint, Map.copyOf(selection)),
                    (state.config.creativeCraftingMode || (blueprint.station() == state.currentWorkstation() && CraftingSystem.canCraft(state.player, recipe)))
                            && !state.crafting.active());
        } else {
            line(g, "Score = profession + attributes - difficulty", x + 14, y + 363, 290, 11);
            line(g, "Attributes use current character stats.", x + 14, y + 382, 290, 11);
            wrapped(g, assembling ? "Select both required components to assemble."
                    : "Plan uses selected materials. Prepare components before assembly.", x + 14, y + 405, 290, 11, 2);
        }
    }

    private static void line(Graphics2D g, String label, int x, int y, int width, int size) {
        g.setFont(new Font("SansSerif", Font.PLAIN, size));
        if (g.getFontMetrics().stringWidth(label) > width) {
            while (!label.isEmpty() && g.getFontMetrics().stringWidth(label + "...") > width) label = label.substring(0, label.length() - 1);
            label += "...";
        }
        text(g, label, x, y, size);
    }
    private static void wrapped(Graphics2D g, String label, int x, int y, int width, int size, int maxLines) {
        g.setFont(new Font("SansSerif", Font.PLAIN, size));
        String current = ""; int row = 0;
        for (String word : label.split(" ")) {
            String next = current.isEmpty() ? word : current + " " + word;
            if (!current.isEmpty() && g.getFontMetrics().stringWidth(next) > width) {
                line(g, current, x, y + row * (size + 4), width, size);
                if (++row >= maxLines) return;
                current = word;
            } else current = next;
        }
        if (row < maxLines) line(g, current, x, y + row * (size + 4), width, size);
    }

    private void drawTypePicker(Graphics2D g, int x, int y) {
        g.setColor(new Color(9, 13, 21)); g.fillRoundRect(x + 30, y + 136, 880, 444, 12, 12);
        text(g, "Choose an equipment type", x + 48, y + 168, 20);
        text(g, "Every equipment slot has a craftable blueprint.", x + 48, y + 190, 12);
        drawingPicker = true;
        try {
            button(g, x + 780, y + 148, 110, "Cancel", () -> choosingBlueprint = false, true);
            for (int i = 0; i < AssemblyCrafting.BLUEPRINTS.size(); i++) {
                int index = i;
                AssemblyCrafting.Blueprint b = AssemblyCrafting.BLUEPRINTS.get(i);
                int bx = x + 48 + (i % 3) * 283, by = y + 207 + (i / 3) * 59;
                g.drawImage(assets.sprite(GameData.itemIcon(AssemblyCrafting.categoryPreviewKey(b)), 40), bx, by, null);
                button(g, bx + 44, by, 223, b.label(), () -> {
                    blueprintIndex = index; selectedParts.clear(); plannedOptional.clear(); focusedSlot = b.slots().get(0); choosingBlueprint = false;
                }, true);
                text(g, b.profession().label(), bx + 50, by + 45, 11);
            }
        } finally { drawingPicker = false; }
    }

    private void openPicker(AssemblyCrafting.Slot slot) {
        focusedSlot = slot;
        pickerSlot = slot;
        pickerPage = 0;
    }
    private record Choice(String key, String title, String detail) {}
    private MaterialCatalog.Material choiceMaterial(Choice choice) {
        var part = AssemblyCrafting.component(choice.key());
        return part == null ? MaterialCatalog.get(choice.key()) : part.material();
    }
    private int owned(Choice choice) { return state.player.inventory.getOrDefault(choice.key(), 0); }
    private int required() { return assembling ? 1 : pickerSlot.amount; }
    private int availability(Choice choice) { return owned(choice) >= required() ? 2 : owned(choice) > 0 ? 1 : 0; }
    private int rarity(Choice choice) {
        var part = AssemblyCrafting.component(choice.key());
        int material = choiceMaterial(choice).rarity().ordinal();
        return part == null ? material : Math.max(material, part.quality().rarity.ordinal());
    }
    private void sortChoices(java.util.List<Choice> choices) {
        java.util.Comparator<Choice> compare = switch (pickerSort) {
            case TIER -> java.util.Comparator.comparingInt(c -> choiceMaterial(c).tier());
            case LEVEL -> java.util.Comparator.comparingInt(c -> choiceMaterial(c).level());
            case RARITY -> java.util.Comparator.comparingInt(this::rarity);
            case AVAILABILITY -> java.util.Comparator.comparingInt(this::availability).thenComparingInt(this::owned);
        };
        if (descending) compare = compare.reversed();
        final var order = compare.thenComparing(Choice::key);
        choices.sort((a, b) -> a.key().isEmpty() ? (b.key().isEmpty() ? 0 : -1) : b.key().isEmpty() ? 1 : order.compare(a, b));
    }
    private java.awt.image.BufferedImage materialSprite(String icon, boolean missing) {
        if (!missing) return assets.sprite(icon, 64);
        return missingSprites.computeIfAbsent(icon, key -> {
            var source = assets.sprite(key, 64);
            var gray = new java.awt.image.BufferedImage(source.getWidth(), source.getHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
            for (int yy = 0; yy < source.getHeight(); yy++) for (int xx = 0; xx < source.getWidth(); xx++) {
                int pixel = source.getRGB(xx, yy);
                int value = (int) (((pixel >> 16 & 255) * .299 + (pixel >> 8 & 255) * .587 + (pixel & 255) * .114) * .65);
                gray.setRGB(xx, yy, (pixel & 0xff000000) | value << 16 | value << 8 | value);
            }
            return gray;
        });
    }

    private void drawPicker(Graphics2D g, int x, int y) {
        AssemblyCrafting.Slot slot = pickerSlot;
        java.util.ArrayList<Choice> choices = new java.util.ArrayList<>();
        if (assembling) {
            if (slot.optional()) choices.add(new Choice("", "Leave " + slot.label + " empty", "Do not consume an optional component."));
            for (AssemblyCrafting.Component part : AssemblyCrafting.ownedParts(state.player, slot))
                choices.add(new Choice(part.key(), part.name() + " | Owned: " + state.player.inventory.get(part.key()),
                        part.material().summary()));
        } else {
            if (slot.optional()) choices.add(new Choice("", "Leave " + slot.label + " empty", "Omit this optional part from the planned preview."));
            for (MaterialCatalog.Material material : AssemblyCrafting.materials(slot)) {
                int count = state.player.inventory.getOrDefault(material.key(), 0);
                choices.add(new Choice(material.key(), GameData.itemName(material.key()) + " | Owned: " + count
                        + " / Need: " + slot.amount + " | " + slot.profession.label() + " " + material.requiredSkill(),
                        material.summary() + " | Quality: " + CraftingCalculation.calculate(state.player, slot.profession, material.tier()).quality().label
                                + (count < slot.amount ? " | Need more material" : "")));
            }
        }
        sortChoices(choices);
        int pages = Math.max(1, (choices.size() + PICKER_ROWS - 1) / PICKER_ROWS);
        pickerPage = Math.min(pickerPage, pages - 1);
        g.setColor(new Color(9, 13, 21));
        g.fillRoundRect(x + 30, y + 136, 880, 444, 12, 12);
        g.setColor(new Color(118, 150, 161));
        g.drawRoundRect(x + 30, y + 136, 880, 444, 12, 12);
        text(g, "Choose " + slot.label + (assembling ? " component" : " material"), x + 48, y + 164, 19);
        text(g, "Hover for stats. Counts show owned / required. Unowned materials are gray.",
                x + 48, y + 188, 12);
        drawingPicker = true;
        try {
            button(g, x + 780, y + 148, 110, "Cancel", () -> pickerSlot = null, true);
            for (Sort sort : Sort.values()) {
                int sx = x + 48 + sort.ordinal() * 164;
                String label = sort.label + (pickerSort == sort ? descending ? " v" : " ^" : "");
                button(g, sx, y + 203, 155, label, () -> {
                    descending = pickerSort == sort ? !descending : sort == Sort.AVAILABILITY;
                    pickerSort = sort; pickerPage = 0;
                }, true);
            }
            if (choices.isEmpty()) text(g, "No compatible parts in your pack. Prepare this component in Stage 1.", x + 48, y + 280, 14);
            for (int i = pickerPage * PICKER_ROWS; i < Math.min(choices.size(), (pickerPage + 1) * PICKER_ROWS); i++) {
                Choice choice = choices.get(i);
                int index = i % PICKER_ROWS;
                Rectangle tile = new Rectangle(x + 48 + (index % 6) * 142, y + 247 + (index / 6) * 136, 132, 124);
                drawChoiceTile(g, choice, slot, tile);
            }
            button(g, x + 48, y + 537, 130, "Previous", () -> pickerPage--, pickerPage > 0);
            text(g, "Page " + (pickerPage + 1) + " / " + pages, x + 405, y + 558, 13);
            button(g, x + 760, y + 537, 130, "Next", () -> pickerPage++, pickerPage + 1 < pages);
        } finally { drawingPicker = false; }
    }

    private void drawChoiceTile(Graphics2D g, Choice choice, AssemblyCrafting.Slot slot, Rectangle tile) {
        boolean empty = choice.key().isEmpty(), missing = !empty && owned(choice) == 0;
        boolean hovered = effects.hoverPoint() != null && tile.contains(effects.hoverPoint());
        String selected = assembling ? selectedParts.get(slot) : (!slot.optional() || plannedOptional.contains(slot))
                ? AssemblyCrafting.materials(slot).get(Math.floorMod(materialIndices.getOrDefault(slot, 0), AssemblyCrafting.materials(slot).size())).key() : null;
        boolean chosen = empty ? selected == null : choice.key().equals(selected);
        Color accent = empty || missing ? new Color(100, 108, 121) : ShopRenderer.rarityColorForItem(choice.key());
        g.setColor(hovered ? new Color(43, 53, 69) : new Color(25, 31, 43));
        g.fillRoundRect(tile.x, tile.y, tile.width, tile.height, 8, 8);
        g.setColor(chosen ? new Color(245, 208, 117) : hovered ? new Color(202, 218, 231) : accent);
        g.drawRoundRect(tile.x, tile.y, tile.width, tile.height, 8, 8);
        if (empty) {
            g.drawOval(tile.x + 45, tile.y + 28, 42, 42);
            g.drawLine(tile.x + 49, tile.y + 64, tile.x + 82, tile.y + 34);
            text(g, "Empty", tile.x + 47, tile.y + 108, 12);
        } else {
            var material = choiceMaterial(choice);
            String icon = GameData.itemIcon(choice.key());
            g.drawImage(materialSprite(icon, missing), tile.x + 34, tile.y + 24, null);
            text(g, "T" + material.tier(), tile.x + 9, tile.y + 17, 11);
            text(g, "Lv " + material.level(), tile.x + 82, tile.y + 17, 11);
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.setColor(missing ? new Color(137, 143, 155) : owned(choice) < required() ? new Color(242, 194, 112) : new Color(172, 221, 160));
            String quantity = owned(choice) + " / " + required();
            g.drawString(quantity, tile.x + (tile.width - g.getFontMetrics().stringWidth(quantity)) / 2, tile.y + 109);
            String body = "Stats\n" + material.summary() + "\n\nMaterials\nOwned: " + owned(choice)
                    + " / Required: " + required() + (owned(choice) < required() ? "\nMissing: " + (required() - owned(choice)) : "\nEnough material")
                    + "\n\nCrafting\n" + slot.profession.label() + " " + material.requiredSkill()
                    + " required. Station: " + AssemblyCrafting.station(slot).label()
                    + "\nQuality: " + (assembling ? AssemblyCrafting.component(choice.key()).quality().label
                            : CraftingCalculation.calculate(state.player, slot.profession, material.tier()).quality().label)
                    + (missing ? "\nNot owned. Select to plan your craft." : "");
            var info = CraftingSystem.CRAFTING_ITEMS.get(material.key());
            if (info != null) body += "\n\nMaterial\n" + info.detail();
            effects.tooltipZones().add(new TooltipZone(tile, GameData.itemName(choice.key()), body, icon, accent));
        }
        if (chosen) { g.setColor(new Color(245, 208, 117)); g.fillOval(tile.x + 7, tile.y + tile.height - 12, 5, 5); }
        if (empty) effects.tooltipZones().add(new TooltipZone(tile, choice.title(), choice.detail()));
        effects.buttons().add(new UiButton(tile, choice.title(), () -> {
            if (assembling) {
                if (empty) selectedParts.remove(slot); else selectedParts.put(slot, choice.key());
            } else {
                if (empty) plannedOptional.remove(slot); else if (slot.optional()) plannedOptional.add(slot);
                var materials = AssemblyCrafting.materials(slot);
                for (int j = 0; j < materials.size(); j++) if (materials.get(j).key().equals(choice.key())) materialIndices.put(slot, j);
            }
            pickerSlot = null;
        }));
    }

    private void button(Graphics2D g, int x, int y, int width, String label, Runnable action, boolean enabled) {
        effects.actionButton(g, x, y, width, 30, label, action, new Color(44, 57, 68), new Color(118, 150, 161), enabled && ((pickerSlot == null && !choosingBlueprint) || drawingPicker));
    }
    private static void text(Graphics2D g, String label, int x, int y, int size) {
        g.setFont(new Font("SansSerif", Font.PLAIN, size)); g.setColor(new Color(231, 229, 213)); g.drawString(label, x, y);
    }
}
