package com.alderfall.game;

import com.alderfall.game.inventory.Equipment;
import com.alderfall.game.inventory.ItemRarity;
import java.util.*;
import static com.alderfall.game.MaterialCatalog.Family.*;

/** Versioned, self-describing item keys preserve parts and quality through existing saves. */
public final class AssemblyCrafting {
    private AssemblyCrafting() {}
    public enum Quality {
        // Enum names are preserved because they are stored in existing part and gear keys.
        STANDARD("Common", 1, 100, ItemRarity.COMMON), FINE("Uncommon", 3, 115, ItemRarity.UNCOMMON),
        MASTERWORK("Masterwork", 5, 135, ItemRarity.RARE), RARE("Rare", 7, 155, ItemRarity.RARE),
        LEGENDARY("Legendary", 10, 185, ItemRarity.LEGENDARY);
        public final String label;
        public final int skill, percent;
        public final ItemRarity rarity;
        Quality(String label, int skill, int percent, ItemRarity rarity) {
            this.label = label; this.skill = skill; this.percent = percent; this.rarity = rarity;
        }
        public static Quality forSkill(int skill) {
            Quality result = STANDARD;
            for (Quality q : values()) if (skill >= q.skill) result = q;
            return result;
        }
    }

    /** Curated assembled equipment gives every market stock that uses it a real material recipe. */
    public static List<String> vendorStock(String shopId) {
        return switch (shopId) {
            case "riverside" -> List.of(
                    vendorGear("sword", Quality.STANDARD, "copper_ingot", "oak_wood", "ember_shard"),
                    vendorGear("dagger", Quality.FINE, "iron_ingot", "skin", "crystal_dust"),
                    vendorGear("bow", Quality.STANDARD, "oak_wood", "skin", "horn"),
                    vendorGear("staff", Quality.FINE, "ash_wood", "crystal_dust", "ember_shard"),
                    vendorGear("mail", Quality.STANDARD, "iron_ingot", "wool", "bone"),
                    vendorGear("robe", Quality.FINE, "wool", "plant_fiber", "crystal_dust"),
                    vendorGear("leather_armor", Quality.STANDARD, "skin", "wool", "horn"),
                    vendorGear("ring", Quality.FINE, "silver_ingot", "crystal_dust", "seashell"));
            case "highwall" -> List.of(
                    vendorGear("axe", Quality.RARE, "steel_ingot", "ironwood", "ember_shard"),
                    vendorGear("spear", Quality.MASTERWORK, "cobalt_ingot", "ash_wood", "frost_shard"),
                    vendorGear("bow", Quality.RARE, "maple_wood", "scale", "ember_shard"),
                    vendorGear("staff", Quality.MASTERWORK, "elder_wood", "frost_shard", "ember_shard"),
                    vendorGear("mail", Quality.RARE, "steel_ingot", "wool", "silver_ingot"),
                    vendorGear("robe", Quality.MASTERWORK, "wool", "plant_fiber", "ember_shard"),
                    vendorGear("leather_armor", Quality.RARE, "scale", "wool", "ember_shard"),
                    vendorGear("ring", Quality.MASTERWORK, "gold_ingot", "ember_shard", "frost_shard"));
            case "crypt_vendor" -> List.of(
                    vendorGear("sword", Quality.LEGENDARY, "sunmetal_ingot", "ancient_wood", "ember_shard"),
                    vendorGear("dagger", Quality.LEGENDARY, "mithril_ingot", "scale", "frost_shard"),
                    vendorGear("axe", Quality.LEGENDARY, "adamantite_ingot", "ironwood", "ember_shard"),
                    vendorGear("spear", Quality.LEGENDARY, "verdant_ingot", "ancient_wood", "ember_shard"),
                    vendorGear("bow", Quality.LEGENDARY, "ancient_wood", "scale", "ember_shard"),
                    vendorGear("staff", Quality.LEGENDARY, "magic_wood", "frost_shard", "ember_shard"),
                    vendorGear("mail", Quality.LEGENDARY, "adamantite_ingot", "wool", "sunmetal_ingot"),
                    vendorGear("robe", Quality.LEGENDARY, "wool", "plant_fiber", "ember_shard"),
                    vendorGear("leather_armor", Quality.LEGENDARY, "scale", "wool", "verdant_ingot"),
                    vendorGear("ring", Quality.LEGENDARY, "sunmetal_ingot", "ember_shard", "frost_shard"));
            default -> List.of();
        };
    }

    private static String vendorGear(String blueprintId, Quality quality, String primary, String secondary, String ornament) {
        Blueprint blueprint = blueprint(blueprintId);
        return "gear1~" + blueprintId + "~" + quality.name() + "~"
                + primary + "~" + quality.name() + "+" + secondary + "~" + quality.name()
                + "+" + ornament + "~" + quality.name() + "+-";
    }

    /** Rotate every blueprint/quality using valid parts that can be sourced locally. */
    public static List<String> regionalVendorStock(InteriorStyle region, long seed) {
        List<String> stock = new ArrayList<>();
        for (Blueprint blueprint : BLUEPRINTS) for (Quality quality : Quality.values()) {
            Random random = new Random(seed ^ Objects.hash(blueprint.id(), quality.name()));
            List<String> parts = new ArrayList<>();
            for (Slot slot : blueprint.slots().subList(0, 3)) {
                List<MaterialCatalog.Material> pool = MaterialCatalog.all().stream()
                        .filter(slot::accepts).filter(m -> m.tier() <= quality.ordinal() + 1)
                        .filter(m -> MerchantStock.local(m.key(), region))
                        .sorted(Comparator.comparing(MaterialCatalog.Material::key)).toList();
                if (pool.isEmpty()) break;
                parts.add(pool.get(random.nextInt(pool.size())).key());
            }
            if (parts.size() == 3) stock.add(vendorGear(blueprint.id(), quality, parts.get(0), parts.get(1), parts.get(2)));
        }
        return List.copyOf(stock);
    }
    public enum Slot {
        BLADE("Blade", Profession.SMITHING, 3, METAL, GLASS),
        HILT("Hilt", Profession.CARPENTRY, 1, WOOD, HIDE, MONSTER),
        HEAD("Head", Profession.SMITHING, 3, METAL, STONE, GLASS),
        SHAFT("Shaft", Profession.CARPENTRY, 2, WOOD),
        GEM("Gem", Profession.JEWELLERY, 1, MaterialCatalog.Family.GEM),
        STRING("Bowstring", Profession.TAILORING, 1, CLOTH, HIDE),
        PLATES("Plates", Profession.ARMORCRAFT, 3, METAL),
        LINING("Lining", Profession.TAILORING, 1, CLOTH),
        FABRIC("Fabric", Profession.TAILORING, 3, CLOTH),
        LEATHER("Leather", Profession.LEATHERWORKING, 3, HIDE),
        SETTING("Setting", Profession.JEWELLERY, 2, METAL),
        ORNAMENT("Ornament", Profession.JEWELLERY, 1, MONSTER, HIDE, MaterialCatalog.Family.GEM, METAL, GLASS),
        RUNE("Rune", Profession.ALCHEMY, 1, MaterialCatalog.Family.GEM, REAGENT, MONSTER);
        public final String label;
        public final Profession profession;
        public final int amount;
        private final Set<MaterialCatalog.Family> families;
        Slot(String label, Profession profession, int amount, MaterialCatalog.Family... families) {
            this.label = label; this.profession = profession; this.amount = amount; this.families = Set.of(families);
        }
        public boolean optional() { return this == ORNAMENT || this == RUNE; }
        public boolean accepts(MaterialCatalog.Material material) {
            return material != null && families.contains(material.family())
                    && (material.family() != METAL || material.key().endsWith("_ingot"));
        }
    }
    public record Blueprint(String id, String label, String equipmentSlot, String icon,
                            Profession profession, CraftingSystem.Workstation station, List<Slot> slots) {}
    public static final List<Blueprint> BLUEPRINTS = List.of(
            blueprint("sword", "Sword", "weapon", "icon_sword", Profession.SMITHING, Slot.BLADE, Slot.HILT),
            blueprint("dagger", "Dagger", "weapon", "new_weapon_briarhook_dagger", Profession.SMITHING, Slot.BLADE, Slot.HILT),
            blueprint("axe", "Axe", "weapon", "woodcutter_axe", Profession.SMITHING, Slot.HEAD, Slot.SHAFT),
            blueprint("spear", "Spear", "weapon", "new_weapon_oakwarden_spear", Profession.SMITHING, Slot.BLADE, Slot.SHAFT),
            blueprint("staff", "Staff", "weapon", "glassdune_staff", Profession.CARPENTRY, Slot.SHAFT, Slot.GEM),
            blueprint("bow", "Bow", "weapon", "new_weapon_willow_shortbow", Profession.CARPENTRY, Slot.SHAFT, Slot.STRING),
            blueprint("mail", "Mail", "chestpiece", "guard_cuirass", Profession.ARMORCRAFT, Slot.PLATES, Slot.LINING),
            blueprint("robe", "Robe", "chestpiece", "starweave_robes", Profession.TAILORING, Slot.FABRIC, Slot.LINING),
            blueprint("leather_armor", "Leather Armor", "chestpiece", "scout_leathers", Profession.LEATHERWORKING, Slot.LEATHER, Slot.LINING),
            blueprint("ring", "Ring", "ring", "starrelic_ring", Profession.JEWELLERY, Slot.SETTING, Slot.GEM),
            blueprint("helmet", "Helmet", "helmet", "equipment-look|helmet|0|copper_ingot|wool|gold_ingot", Profession.ARMORCRAFT, Slot.PLATES, Slot.LINING),
            blueprint("pauldrons", "Pauldrons", "pauldrons", "equipment-look|pauldrons|0|copper_ingot|wool|gold_ingot", Profession.ARMORCRAFT, Slot.PLATES, Slot.LINING),
            blueprint("gloves", "Gloves", "gloves", "equipment-look|gloves|0|skin|wool|gold_ingot", Profession.LEATHERWORKING, Slot.LEATHER, Slot.LINING),
            blueprint("belt", "Belt", "belt", "equipment-look|belt|0|skin|copper_ingot|gold_ingot", Profession.LEATHERWORKING, Slot.LEATHER, Slot.SETTING),
            blueprint("leggings", "Leggings", "leggings", "equipment-look|leggings|0|copper_ingot|wool|gold_ingot", Profession.ARMORCRAFT, Slot.PLATES, Slot.LINING),
            blueprint("boots", "Boots", "boots", "equipment-look|boots|0|skin|wool|gold_ingot", Profession.LEATHERWORKING, Slot.LEATHER, Slot.LINING),
            blueprint("necklace", "Necklace", "necklace", "equipment-look|necklace|0|copper_ingot|crystal_dust|gold_ingot", Profession.JEWELLERY, Slot.SETTING, Slot.GEM),
            blueprint("shield", "Shield", "shield", "equipment-look|shield|0|copper_ingot|wood|gold_ingot", Profession.ARMORCRAFT, Slot.PLATES, Slot.HILT));
    private static Blueprint blueprint(String id, String label, String equipmentSlot, String icon,
                                       Profession profession, Slot... required) {
        List<Slot> slots = new ArrayList<>(List.of(required));
        slots.add(Slot.ORNAMENT); slots.add(Slot.RUNE);
        CraftingSystem.Workstation station = profession == Profession.CARPENTRY || profession == Profession.TAILORING
                || profession == Profession.LEATHERWORKING ? CraftingSystem.Workstation.CARPENTER : CraftingSystem.Workstation.ANVIL;
        return new Blueprint(id, label, equipmentSlot, icon, profession, station, List.copyOf(slots));
    }
    /** A valid default material plan used only to draw category artwork before parts are selected. */
    public static String categoryPreviewKey(Blueprint blueprint) {
        List<String> encoded = new ArrayList<>();
        for (Slot slot : blueprint.slots()) {
            if (slot.optional()) encoded.add("-");
            else encoded.add(materials(slot).get(0).key() + "~STANDARD");
        }
        return "gear1~" + blueprint.id() + "~STANDARD~" + String.join("+", encoded);
    }

    public static Blueprint blueprint(String id) {
        return BLUEPRINTS.stream().filter(b -> b.id().equals(id)).findFirst().orElse(null);
    }
    public record Component(Slot slot, MaterialCatalog.Material material, Quality quality) {
        public String key() { return "part1~" + slot.name() + "~" + material.key() + "~" + quality.name(); }
        public String name() { return quality.label + " " + materialName(material.key()) + " " + slot.label; }
    }
    private static String materialName(String key) {
        String name = CraftingSystem.itemName(key);
        return name == null ? key : name.replace(" Ingot", "").replace(" Wood", "");
    }
    public static Component component(String key) {
        if (key == null || !key.startsWith("part1~")) return null;
        try {
            String[] parts = key.split("~", -1);
            if (parts.length != 4) return null;
            Slot slot = Slot.valueOf(parts[1]);
            MaterialCatalog.Material material = MaterialCatalog.get(parts[2]);
            return slot.accepts(material) ? new Component(slot, material, Quality.valueOf(parts[3])) : null;
        } catch (IllegalArgumentException ex) { return null; }
    }
    public static int effectiveSkill(Actor actor, Profession profession) {
        return actor.professionLevel(profession.id()) + actor.professionPracticeBonus(profession.id());
    }
    public static List<MaterialCatalog.Material> materials(Slot slot) {
        return MaterialCatalog.all().stream().filter(slot::accepts)
                .sorted(Comparator.comparingInt(MaterialCatalog.Material::tier).thenComparing(MaterialCatalog.Material::key)).toList();
    }
    public static CraftingSystem.Workstation station(Slot slot) {
        return switch (slot.profession) {
            case CARPENTRY, TAILORING, LEATHERWORKING -> CraftingSystem.Workstation.CARPENTER;
            case ALCHEMY -> CraftingSystem.Workstation.ALCHEMY;
            default -> CraftingSystem.Workstation.ANVIL;
        };
    }
    public static CraftingSystem.Recipe componentRecipe(Actor actor, Slot slot, String materialKey) {
        MaterialCatalog.Material material = MaterialCatalog.get(materialKey);
        if (!slot.accepts(material)) return null;
        Component part = new Component(slot, material, CraftingCalculation.calculate(actor, slot.profession, material.tier()).quality());
        return new CraftingSystem.Recipe("prepare_" + slot.name(), part.name(), station(slot),
                Map.of(materialKey, slot.amount), part.key(), 1, 65 + material.tier() * 15, slot.profession.id(),
                Map.of(slot.profession.id(), material.requiredSkill()), CraftingSystem.RecipeCategory.MATERIAL,
                material.summary() + ". Quality: " + part.quality().label);
    }
    public static List<Component> ownedParts(Actor actor, Slot slot) {
        return actor.inventory.entrySet().stream().filter(e -> e.getValue() > 0).map(e -> component(e.getKey()))
                .filter(Objects::nonNull).filter(c -> c.slot() == slot)
                .sorted(Comparator.comparing(Component::key)).toList();
    }
    public static CraftingSystem.Recipe assemblyRecipe(Actor actor, Blueprint blueprint, Map<Slot, String> selected) {
        if (blueprint == null || !BLUEPRINTS.contains(blueprint) || selected.keySet().stream().anyMatch(s -> !blueprint.slots().contains(s))) return null;
        Quality quality = assemblyCalculation(actor, blueprint, selected).quality();
        int required = 1;
        Map<String, Integer> cost = new LinkedHashMap<>();
        List<String> encoded = new ArrayList<>();
        for (Slot slot : blueprint.slots()) {
            String key = selected.get(slot);
            if (key == null && slot.optional()) { encoded.add("-"); continue; }
            Component part = component(key);
            if (part == null || part.slot() != slot) return null;
            if (!slot.optional() && part.quality().ordinal() < quality.ordinal()) quality = part.quality();
            required = Math.max(required, part.material().requiredSkill());
            encoded.add(part.material().key() + "~" + part.quality().name());
            cost.merge(key, 1, Integer::sum);
        }
        String result = "gear1~" + blueprint.id() + "~" + quality.name() + "~" + String.join("+", encoded);
        Equipment gear = equipment(result);
        if (gear == null) return null;
        return new CraftingSystem.Recipe("assemble_" + blueprint.id(), gear.name(), blueprint.station(), cost,
                result, 1, 120, blueprint.profession().id(), Map.of(blueprint.profession().id(), required),
                blueprint.equipmentSlot().equals("weapon") ? CraftingSystem.RecipeCategory.WEAPON : CraftingSystem.RecipeCategory.ARMOR,
                gear.description());
    }
    public static CraftingCalculation.Result assemblyCalculation(Actor actor, Blueprint blueprint, Map<Slot, String> selected) {
        int tier = selected.values().stream().map(AssemblyCrafting::component).filter(Objects::nonNull)
                .mapToInt(c -> c.material().tier()).max().orElse(1);
        return CraftingCalculation.calculate(actor, blueprint.profession(), tier);
    }

    public static Equipment equipment(String key) {
        if (key == null || !key.startsWith("gear1~")) return null;
        try {
            String[] fields = key.split("~", 4);
            if (fields.length != 4) return null;
            Blueprint b = blueprint(fields[1]);
            Quality q = Quality.valueOf(fields[2]);
            if (b == null) return null;
            String[] parts = fields[3].split("\\+", -1);
            if (parts.length != b.slots().size()) return null;
            int atk = 0, def = 0, hp = 0, mp = 0, spell = 0, crit = 0, heal = 0, level = 1, tier = 1;
            String mainMaterial = "";
            List<String> provenance = new ArrayList<>();
            ItemRarity rarity = q.rarity;
            for (int i = 0; i < parts.length; i++) {
                Slot slot = b.slots().get(i);
                if (parts[i].equals("-") && slot.optional()) continue;
                Component c = component("part1~" + slot.name() + "~" + parts[i]);
                if (c == null || (!slot.optional() && c.quality().ordinal() < q.ordinal())) return null;
                MaterialCatalog.Material m = c.material();
                if (i == 0) mainMaterial = materialName(m.key());
                MaterialCatalog.Stats s = m.stats();
                int weight = slot.optional() ? 1 : slot.amount;
                // Optional workmanship matters without limiting the required structure's quality.
                int scale = slot.optional() ? c.quality().percent : 100;
                atk += s.attack() * weight * scale / 100; def += s.defense() * weight * scale / 100;
                hp += s.hp() * weight * scale / 100; mp += s.mp() * weight * scale / 100;
                spell += s.spell() * weight * scale / 100; crit += s.crit() * scale / 100; heal += s.healing() * scale / 100;
                level = Math.max(level, m.level()); tier = Math.max(tier, m.tier());
                if (m.rarity().ordinal() > rarity.ordinal()) rarity = m.rarity();
                provenance.add(slot.label + ": " + c.name());
            }
            boolean weapon = b.equipmentSlot().equals("weapon");
            if (!weapon) atk /= 3;
            if (b.id().equals("dagger")) { atk = atk * 3 / 4; crit += 3; }
            if (b.id().equals("axe")) { atk += 3; crit = Math.max(0, crit - 1); }
            return new Equipment(key, q.label + " " + mainMaterial + " " + b.label(), b.equipmentSlot(), b.icon(), rarity,
                    scale(atk, q), scale(def, q), scale(hp, q), scale(mp, q), 0, 0, 0, 0, 0, 0,
                    scale(spell, q), Math.min(20, scale(crit, q)), 0, scale(heal, q), 0, level, 99,
                    (20 + tier * 30) * q.percent / 100, "",
                    q.label + " craftsmanship (" + q.percent + "% stats). " + String.join("; ", provenance));
        } catch (IllegalArgumentException ex) { return null; }
    }
    private static int scale(int stat, Quality quality) { return (stat * quality.percent + 50) / 100; }
}
