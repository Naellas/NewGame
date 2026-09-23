package com.alderfall.game;

import com.alderfall.game.inventory.Equipment;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class Actor {
    public static final int MAX_ABILITIES = 48;
    public static final int MAX_ACTIVE_ABILITIES = 10;
    public static final int HP_PER_CONSTITUTION = 5;
    public static final int MP_PER_WILLPOWER = 3;
    public final String name;
    public final String sprite;
    public final String className;
    public final String worldSprite;
    public int maxHp;
    public int hp;
    public int maxMp;
    public int mp;
    public int attack;
    public int defense;
    public int strength;
    public int intelligence;
    public int dexterity;
    public int charisma;
    public int constitution;
    public int willpower;
    public int spellDamageBonus;
    public int critChanceBonus;
    public int critDamageBonus;
    public int healingPowerBonus;
    public int damageReductionBonus;
    public int level = 1;
    public int xp = 0;
    public int gold = 0;
    public final List<Ability> abilities = new ArrayList<>();
    public final List<String> activeAbilityNames = new ArrayList<>();
    public final Map<String, List<String>> abilityLoadoutPresets = new LinkedHashMap<>();
    public final Map<String, Integer> inventory = new LinkedHashMap<>();
    public final Map<String, Integer> skillAllocations = new LinkedHashMap<>();
    public final Map<String, Integer> professionXp = new LinkedHashMap<>();
    public final Map<String, String> equipment = new LinkedHashMap<>();
    public int skillPoints = 0;
    public int statPoints = 0;
    public int professionSkillPoints = 0;

    public Actor(String name, String sprite, String className, int maxHp, int maxMp, int attack, int defense) {
        this(name, sprite, className, maxHp, maxMp, attack, defense, defaultStrength(className),
                defaultIntelligence(className), defaultDexterity(className), defaultCharisma(className),
                defaultConstitution(className), defaultWillpower(className));
    }

    public Actor(
            String name,
            String sprite,
            String className,
            int maxHp,
            int maxMp,
            int attack,
            int defense,
            int strength,
            int intelligence,
            int dexterity,
            int charisma,
            int constitution,
            int willpower
    ) {
        this.name = name;
        this.sprite = sprite;
        this.className = className;
        this.worldSprite = sprite + "_model";
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.maxMp = maxMp;
        this.mp = maxMp;
        this.attack = attack;
        this.defense = defense;
        this.strength = Math.max(1, strength);
        this.intelligence = Math.max(1, intelligence);
        this.dexterity = Math.max(1, dexterity);
        this.charisma = Math.max(1, charisma);
        this.constitution = Math.max(1, constitution);
        this.willpower = Math.max(1, willpower);
        professionXp.putAll(Profession.seedXp(name, sprite, className));
    }

    public boolean alive() {
        return hp > 0;
    }

    public void healFull() {
        hp = maxHp;
        mp = maxMp;
    }

    public int takeDamage(int amount) {
        int damage = previewDamage(amount);
        hp = Math.max(0, hp - damage);
        return damage;
    }

    public int previewDamage(int amount) {
        int damage = Math.max(1, amount - defense);
        if (damageReductionBonus > 0) {
            damage = Math.max(1, (int) Math.round(damage * Math.max(0, 100 - damageReductionBonus) / 100.0));
        }
        return damage;
    }

    public int basicDamage(Random random) {
        int baseAttack = attack + strength / 2 + dexterity / 4;
        int min = Math.max(1, baseAttack - 2);
        int raw = min + random.nextInt(baseAttack + 4 - min + 1);
        raw += skillRank("blade_flurry") * 3;
        return raw;
    }

    public int abilityDamageBonus(Ability ability) {
        int bonus = abilityScalingBonus(ability);
        if (isMentalAbility(ability)) {
            bonus += spellDamageBonus;
        }
        return bonus;
    }

    public int abilityScalingBonus(Ability ability) {
        Ability.ScalingProfile scaling = ability == null ? Ability.ScalingProfile.WEAPON : ability.scaling();
        return Math.max(0, switch (scaling) {
            case WEAPON -> strength / 2 + dexterity / 5 + willpower / 6;
            case AGILITY -> dexterity / 2 + attack / 3 + strength / 6;
            case ARCANE -> intelligence / 2 + willpower / 4 + dexterity / 8;
            case DIVINE -> willpower / 2 + charisma / 3 + intelligence / 6;
            case NATURE -> willpower / 3 + charisma / 3 + intelligence / 4 + constitution / 8;
            case GUARD -> defense / 2 + constitution / 4 + willpower / 4;
            case TRIAGE -> willpower / 3 + charisma / 3 + intelligence / 4 + dexterity / 8;
        });
    }

    public int healingBonus() {
        return Math.max(0, healingPowerBonus);
    }

    public int guardBonus() {
        return Math.max(0, willpower / 3 + strength / 5);
    }

    public double criticalChanceAgainst(Actor target, Ability ability) {
        int focus = abilityFocusStat(ability);
        double chance = 0.03 + dexterity * 0.007 + focus * 0.0015;
        chance += critChanceBonus / 100.0;
        chance += skillRank("keen_edge") * 0.08 + skillRank("hawk_eye") * 0.05;
        if (target != null) {
            chance -= Math.max(0, target.dexterity - dexterity) * 0.002;
        }
        return clampChance(chance, 0.02, 0.40);
    }

    public double dodgeChanceAgainst(Actor attacker) {
        int attackerDexterity = attacker == null ? dexterity : attacker.dexterity;
        int attackerAttack = attacker == null ? attack : attacker.attack;
        double chance = 0.02 + dexterity * 0.005;
        chance -= Math.max(0, attackerDexterity - dexterity) * 0.0025;
        chance -= Math.max(0, attackerAttack - attack) * 0.0015;
        return clampChance(chance, 0.0, 0.30);
    }

    public double parryChanceAgainst(Actor attacker) {
        int attackerStrength = attacker == null ? strength : attacker.strength;
        int attackerAttack = attacker == null ? attack : attacker.attack;
        double chance = 0.012 + strength * 0.0025 + dexterity * 0.0025 + constitution * 0.0025 + defense * 0.0015;
        chance -= Math.max(0, attackerStrength - strength) * 0.0015;
        chance -= Math.max(0, attackerAttack - attack) * 0.001;
        return clampChance(chance, 0.0, 0.25);
    }

    public double criticalMultiplier(Ability ability) {
        int focus = abilityFocusStat(ability);
        return 1.45 + Math.min(0.45, (focus + dexterity) / 120.0) + critDamageBonus / 100.0;
    }

    public int xpToNext() {
        return 20 + level * 12;
    }

    public List<String> gainXp(int amount) {
        List<String> notes = new ArrayList<>();
        xp += amount;
        while (xp >= xpToNext()) {
            xp -= xpToNext();
            level++;
            skillPoints++;
            statPoints += 3;
            int hpGain = 6 + level / 3;
            int mpGain = 2 + level / 4;
            int attackGain = 1 + (level % 3 == 0 ? 1 : 0);
            int defenseGain = level % 2 == 0 ? 1 : 0;
            maxHp += hpGain;
            maxMp += mpGain;
            attack += attackGain;
            defense += defenseGain;
            notes.add("Level " + level + ": +" + hpGain + " HP, +" + mpGain + " MP, +" + attackGain + " ATK, +"
                    + defenseGain + " DEF, +1 skill point, +3 stat points.");
        }
        if (!notes.isEmpty()) {
            healFull();
        }
        return notes;
    }

    public void addItem(String key, int amount) {
        if (amount <= 0) {
            return;
        }
        inventory.merge(key, amount, Integer::sum);
    }

    public boolean hasItem(String key) {
        return inventory.getOrDefault(key, 0) > 0;
    }

    public boolean consumeItem(String key) {
        int count = inventory.getOrDefault(key, 0);
        if (count <= 0) {
            return false;
        }
        if (count == 1) {
            inventory.remove(key);
        } else {
            inventory.put(key, count - 1);
        }
        return true;
    }

    public boolean hasAbility(String abilityName) {
        return abilities.stream().anyMatch(ability -> ability.name().equals(abilityName));
    }

    public List<Ability> activeAbilities() {
        sanitizeAbilityLoadout();
        List<Ability> active = new ArrayList<>();
        for (String abilityName : activeAbilityNames) {
            abilityByName(abilityName).ifPresent(active::add);
        }
        return List.copyOf(active);
    }

    public boolean isAbilityActive(String abilityName) {
        sanitizeAbilityLoadout();
        return activeAbilityNames.contains(abilityName);
    }

    public boolean toggleAbilityLoadout(String abilityName) {
        sanitizeAbilityLoadout();
        if (!hasAbility(abilityName)) {
            return false;
        }
        if (activeAbilityNames.remove(abilityName)) {
            return true;
        }
        if (activeAbilityNames.size() >= MAX_ACTIVE_ABILITIES) {
            return false;
        }
        activeAbilityNames.add(abilityName);
        return true;
    }

    public boolean removeAbilityFromLoadout(String abilityName) {
        sanitizeAbilityLoadout();
        return activeAbilityNames.remove(abilityName);
    }

    public boolean placeAbilityInLoadout(String abilityName, int slot) {
        sanitizeAbilityLoadout();
        if (!hasAbility(abilityName)) {
            return false;
        }
        int target = Math.max(0, Math.min(MAX_ACTIVE_ABILITIES - 1, slot));
        activeAbilityNames.remove(abilityName);
        if (target < activeAbilityNames.size()) {
            if (activeAbilityNames.size() >= MAX_ACTIVE_ABILITIES) {
                activeAbilityNames.remove(target);
            }
            activeAbilityNames.add(target, abilityName);
        } else if (activeAbilityNames.size() < MAX_ACTIVE_ABILITIES) {
            activeAbilityNames.add(abilityName);
        } else {
            activeAbilityNames.set(MAX_ACTIVE_ABILITIES - 1, abilityName);
        }
        sanitizeAbilityLoadout();
        return true;
    }

    public void setAbilityLoadout(List<String> abilityNames) {
        activeAbilityNames.clear();
        if (abilityNames != null) {
            activeAbilityNames.addAll(abilityNames);
        }
        sanitizeAbilityLoadout();
    }

    public boolean saveAbilityLoadoutPreset(String presetName) {
        sanitizeAbilityLoadout();
        String key = normalizePresetName(presetName);
        if (key.isBlank()) {
            return false;
        }
        abilityLoadoutPresets.put(key, List.copyOf(activeAbilityNames));
        return true;
    }

    public boolean applyAbilityLoadoutPreset(String presetName) {
        List<String> preset = abilityLoadoutPresets.get(normalizePresetName(presetName));
        if (preset == null) {
            return false;
        }
        setAbilityLoadout(preset);
        return true;
    }

    public void sanitizeAbilityLoadoutPresets() {
        List<String> keys = new ArrayList<>(abilityLoadoutPresets.keySet());
        for (String key : keys) {
            List<String> sanitized = sanitizedAbilityNames(abilityLoadoutPresets.get(key));
            if (sanitized.isEmpty()) {
                abilityLoadoutPresets.remove(key);
            } else {
                abilityLoadoutPresets.put(key, sanitized);
            }
        }
    }

    public void sanitizeAbilityLoadout() {
        List<String> sanitized = sanitizedAbilityNames(activeAbilityNames);
        if (sanitized.isEmpty()) {
            for (Ability ability : abilities) {
                if (sanitized.size() >= MAX_ACTIVE_ABILITIES) {
                    break;
                }
                if (!sanitized.contains(ability.name())) {
                    sanitized.add(ability.name());
                }
            }
        }
        activeAbilityNames.clear();
        activeAbilityNames.addAll(sanitized);
    }

    private List<String> sanitizedAbilityNames(List<String> abilityNames) {
        List<String> sanitized = new ArrayList<>();
        if (abilityNames == null) {
            return sanitized;
        }
        for (String abilityName : abilityNames) {
            if (abilityName != null && hasAbility(abilityName) && !sanitized.contains(abilityName)
                    && sanitized.size() < MAX_ACTIVE_ABILITIES) {
                sanitized.add(abilityName);
            }
        }
        return sanitized;
    }

    private String normalizePresetName(String presetName) {
        if (presetName == null) {
            return "";
        }
        return switch (presetName.strip().toLowerCase()) {
            case "travel" -> "Travel";
            case "boss" -> "Boss";
            case "support" -> "Support";
            default -> presetName.strip();
        };
    }

    private java.util.Optional<Ability> abilityByName(String abilityName) {
        return abilities.stream().filter(ability -> ability.name().equals(abilityName)).findFirst();
    }

    public int skillRank(String skillKey) {
        return Math.max(0, skillAllocations.getOrDefault(skillKey, 0));
    }

    public int professionLevel(String professionId) {
        return Profession.levelForXp(professionXp.getOrDefault(professionId, 0), professionLevelCap(professionId));
    }

    public int professionLevelCap(String professionId) {
        int cap = Profession.BASE_MAX_LEVEL + skillRank(professionId + "_mastery")
                + skillRank("trade_foundations")
                + skillRank("master_of_trades");
        cap += switch (professionId) {
            case "woodcutting" -> skillRank("forester_path") * 2 + skillRank("coppice_planning") + skillRank("heartwood_harvest");
            case "fishing" -> skillRank("angler_path") * 2 + skillRank("tide_reader") + skillRank("deepwater_bounty");
            case "mining" -> skillRank("prospector_path") * 2 + skillRank("seam_sense") + skillRank("gem_cutting");
            case "crafting" -> skillRank("artisan_path") * 2 + skillRank("measured_cuts") + skillRank("masterwork_fittings");
            case "weaving" -> skillRank("tailor_path") + skillRank("loom_logic") * 2 + skillRank("sailcloth_patterns");
            case "leatherworking" -> skillRank("tailor_path") + skillRank("tanner_path") * 2 + skillRank("saddle_stitch");
            case "cooking" -> skillRank("provisioner_path") + skillRank("spice_blends") * 2 + skillRank("feast_planning");
            case "survival" -> skillRank("provisioner_path") + skillRank("trailcraft_path") * 2 + skillRank("emergency_cache");
            default -> 0;
        };
        return Math.max(1, Math.min(Profession.ABSOLUTE_MAX_LEVEL, cap));
    }

    public int professionPracticeBonus(String professionId) {
        int bonus = skillRank("trade_foundations") + skillRank("master_of_trades")
                + skillRank(professionId + "_training") + skillRank(professionId + "_mastery");
        bonus += switch (professionId) {
            case "woodcutting" -> skillRank("forester_path") + skillRank("resin_tapping") + skillRank("heartwood_harvest");
            case "fishing" -> skillRank("angler_path") + skillRank("netcraft") + skillRank("deepwater_bounty");
            case "mining" -> skillRank("prospector_path") + skillRank("blast_mining") + skillRank("gem_cutting");
            case "crafting" -> skillRank("artisan_path") + skillRank("jig_templates") + skillRank("masterwork_fittings");
            case "weaving" -> skillRank("tailor_path") + skillRank("dye_baths") + skillRank("sailcloth_patterns");
            case "leatherworking" -> skillRank("tailor_path") + skillRank("curing_racks") + skillRank("reinforced_hide");
            case "cooking" -> skillRank("provisioner_path") + skillRank("stockpot_rhythm") + skillRank("feast_planning");
            case "survival" -> skillRank("provisioner_path") + skillRank("weather_eye") + skillRank("emergency_cache");
            default -> 0;
        };
        return Math.max(0, bonus);
    }

    public List<String> gainProfessionXp(String professionId, int amount) {
        List<String> notes = new ArrayList<>();
        Profession profession = Profession.byId(professionId);
        if (profession == null || amount <= 0) {
            return notes;
        }
        int before = professionLevel(professionId);
        professionXp.merge(professionId, amount, Integer::sum);
        int after = professionLevel(professionId);
        if (after > before) {
            professionSkillPoints += after - before;
            notes.add(profession.label() + " " + after + " (+" + (after - before) + " profession point)");
        }
        return notes;
    }

    public Equipment equippedItem(String slot) {
        String key = equipment.get(slot);
        return key == null ? null : GameData.equipment(key);
    }

    public List<Equipment> equippedItems() {
        List<Equipment> items = new ArrayList<>();
        for (String key : equipment.values()) {
            Equipment item = GameData.equipment(key);
            if (item != null) {
                items.add(item);
            }
        }
        return List.copyOf(items);
    }

    public boolean hasEquipmentEffect(String fragment) {
        if (fragment == null || fragment.isBlank()) {
            return false;
        }
        String needle = fragment.toLowerCase();
        for (Equipment item : equippedItems()) {
            String text = (item.key() + " " + item.name() + " " + item.uniqueEffect() + " " + item.description()).toLowerCase();
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    public String equipItem(String itemKey) {
        Equipment item = GameData.equipment(itemKey);
        if (item == null) {
            return "That item cannot be equipped.";
        }
        if (!hasItem(itemKey)) {
            return "You do not have " + item.name() + ".";
        }
        if (!item.canEquipAt(level)) {
            return item.name() + " requires level " + item.minLevel() + ".";
        }
        String oldKey = equipment.get(item.slot());
        if (oldKey != null) {
            Equipment oldItem = GameData.equipment(oldKey);
            if (oldItem != null) {
                applyEquipmentBonus(oldItem, -1);
                addItem(oldKey, 1);
            }
        }
        consumeItem(itemKey);
        equipment.put(item.slot(), itemKey);
        applyEquipmentBonus(item, 1);
        hp = Math.min(hp, maxHp);
        mp = Math.min(mp, maxMp);
        return "Equipped " + item.name() + ".";
    }

    public String unequipSlot(String slot) {
        String itemKey = equipment.get(slot);
        if (itemKey == null) {
            return "Nothing is equipped there.";
        }
        Equipment item = GameData.equipment(itemKey);
        if (item == null) {
            equipment.remove(slot);
            return "Removed unknown gear.";
        }
        applyEquipmentBonus(item, -1);
        addItem(itemKey, 1);
        equipment.remove(slot);
        hp = Math.min(hp, maxHp);
        mp = Math.min(mp, maxMp);
        return "Unequipped " + item.name() + ".";
    }

    public void applyEquipmentBonus(Equipment item, int direction) {
        attack += item.attackBonus() * direction;
        defense += item.defenseBonus() * direction;
        maxHp += item.hpBonus() * direction;
        maxMp += item.mpBonus() * direction;
        strength = Math.max(1, strength + item.strengthBonus() * direction);
        intelligence = Math.max(1, intelligence + item.intelligenceBonus() * direction);
        dexterity = Math.max(1, dexterity + item.dexterityBonus() * direction);
        charisma = Math.max(1, charisma + item.charismaBonus() * direction);
        changeConstitution(item.constitutionBonus() * direction, direction > 0);
        changeWillpower(item.willpowerBonus() * direction, direction > 0);
        spellDamageBonus = Math.max(0, spellDamageBonus + item.spellDamageBonus() * direction);
        critChanceBonus = Math.max(0, critChanceBonus + item.critChanceBonus() * direction);
        critDamageBonus = Math.max(0, critDamageBonus + item.critDamageBonus() * direction);
        healingPowerBonus = Math.max(0, healingPowerBonus + item.healingPowerBonus() * direction);
        damageReductionBonus = Math.max(0, damageReductionBonus + item.damageReductionBonus() * direction);
        if (direction > 0) {
            hp += item.hpBonus();
            mp += item.mpBonus();
        }
    }

    public boolean allocateStat(String statKey) {
        if (statPoints <= 0) {
            return false;
        }
        if (!increaseStat(statKey, 1)) {
            return false;
        }
        statPoints--;
        return true;
    }

    public boolean increaseStat(String statKey, int amount) {
        if (amount == 0) {
            return true;
        }
        switch (statKey) {
            case "strength" -> strength = Math.max(1, strength + amount);
            case "intelligence" -> intelligence = Math.max(1, intelligence + amount);
            case "dexterity" -> dexterity = Math.max(1, dexterity + amount);
            case "charisma" -> charisma = Math.max(1, charisma + amount);
            case "constitution" -> changeConstitution(amount, amount > 0);
            case "willpower" -> changeWillpower(amount, amount > 0);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void changeConstitution(int amount, boolean restoreAddedResource) {
        int before = constitution;
        constitution = Math.max(1, constitution + amount);
        int actual = constitution - before;
        if (actual != 0) {
            int hpDelta = actual * HP_PER_CONSTITUTION;
            maxHp = Math.max(1, maxHp + hpDelta);
            if (restoreAddedResource && hpDelta > 0) {
                hp += hpDelta;
            }
            hp = Math.max(0, Math.min(maxHp, hp));
        }
    }

    private void changeWillpower(int amount, boolean restoreAddedResource) {
        int before = willpower;
        willpower = Math.max(1, willpower + amount);
        int actual = willpower - before;
        if (actual != 0) {
            int mpDelta = actual * MP_PER_WILLPOWER;
            maxMp = Math.max(0, maxMp + mpDelta);
            if (restoreAddedResource && mpDelta > 0) {
                mp += mpDelta;
            }
            mp = Math.max(0, Math.min(maxMp, mp));
        }
    }

    private int abilityFocusStat(Ability ability) {
        if (ability == null) {
            return strength;
        }
        return switch (ability.scaling()) {
            case ARCANE -> intelligence;
            case DIVINE, NATURE, GUARD, TRIAGE -> willpower;
            case AGILITY -> dexterity;
            case WEAPON -> strength;
        };
    }

    private boolean isMentalAbility(Ability ability) {
        if (ability == null) {
            return false;
        }
        if (ability.scaling() == Ability.ScalingProfile.ARCANE
                || ability.scaling() == Ability.ScalingProfile.DIVINE
                || ability.scaling() == Ability.ScalingProfile.NATURE
                || ability.scaling() == Ability.ScalingProfile.TRIAGE) {
            return true;
        }
        String lowerName = ability.name().toLowerCase();
        if (lowerName.contains("fire") || lowerName.contains("frost") || lowerName.contains("arcane")
                || lowerName.contains("radiant") || lowerName.contains("thorn") || lowerName.contains("sun")
                || lowerName.contains("moon") || lowerName.contains("star") || lowerName.contains("bloom")
                || lowerName.contains("mend") || lowerName.contains("heal") || lowerName.contains("ward")
                || lowerName.contains("prayer") || lowerName.contains("chorus") || lowerName.contains("refuge")) {
            return true;
        }
        return switch (className) {
            case "Mage", "Cleric", "Medic", "Battle Medic", "Wildspeaker", "Sunwarden", "Thornbinder", "Grovekeeper" -> true;
            default -> false;
        };
    }

    private static double clampChance(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int defaultStrength(String className) {
        return switch (className) {
            case "Mage" -> 7;
            case "Ranger" -> 10;
            case "Cleric", "Medic", "Battle Medic" -> 8;
            case "Rogue", "Dune Guide", "Veilrunner", "Nightblade" -> 9;
            case "Ironwall", "Stonebreaker" -> 14;
            case "Bladedancer" -> 11;
            case "Wildspeaker", "Thornbinder", "Grovekeeper" -> 8;
            case "Sunwarden" -> 10;
            case "Monster" -> 10;
            default -> 13;
        };
    }

    private static int defaultIntelligence(String className) {
        return switch (className) {
            case "Mage" -> 14;
            case "Ranger" -> 8;
            case "Cleric", "Medic", "Battle Medic" -> 10;
            case "Rogue", "Dune Guide", "Veilrunner", "Nightblade" -> 8;
            case "Wildspeaker", "Thornbinder", "Grovekeeper" -> 11;
            case "Sunwarden" -> 9;
            case "Monster" -> 6;
            default -> 7;
        };
    }

    private static int defaultDexterity(String className) {
        return switch (className) {
            case "Mage" -> 8;
            case "Ranger" -> 13;
            case "Cleric", "Medic", "Battle Medic" -> 8;
            case "Rogue", "Dune Guide", "Veilrunner", "Nightblade" -> 14;
            case "Ironwall", "Stonebreaker" -> 7;
            case "Bladedancer" -> 13;
            case "Monster" -> 8;
            default -> 8;
        };
    }

    private static int defaultCharisma(String className) {
        return switch (className) {
            case "Mage" -> 10;
            case "Ranger" -> 9;
            case "Cleric", "Medic", "Battle Medic" -> 12;
            case "Rogue", "Dune Guide", "Veilrunner", "Nightblade" -> 10;
            case "Wildspeaker", "Thornbinder", "Grovekeeper", "Sunwarden" -> 11;
            case "Monster" -> 5;
            default -> 8;
        };
    }

    private static int defaultConstitution(String className) {
        return switch (className) {
            case "Mage" -> 7;
            case "Ranger" -> 10;
            case "Cleric", "Medic", "Battle Medic" -> 10;
            case "Rogue", "Dune Guide", "Veilrunner", "Nightblade" -> 8;
            case "Ironwall", "Stonebreaker" -> 14;
            case "Bladedancer" -> 10;
            case "Wildspeaker", "Thornbinder", "Grovekeeper" -> 10;
            case "Sunwarden" -> 11;
            case "Monster" -> 10;
            default -> 13;
        };
    }

    private static int defaultWillpower(String className) {
        return switch (className) {
            case "Mage" -> 12;
            case "Ranger" -> 9;
            case "Cleric", "Medic", "Battle Medic" -> 13;
            case "Rogue", "Dune Guide", "Veilrunner", "Nightblade" -> 8;
            case "Ironwall", "Stonebreaker" -> 12;
            case "Wildspeaker", "Thornbinder", "Grovekeeper", "Sunwarden" -> 13;
            case "Monster" -> 8;
            default -> 11;
        };
    }

}
