package com.alderfall.game;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class Actor {
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
    public int level = 1;
    public int xp = 0;
    public int gold = 0;
    public final List<Ability> abilities = new ArrayList<>();
    public final Map<String, Integer> inventory = new LinkedHashMap<>();
    public final Map<String, Integer> skillAllocations = new LinkedHashMap<>();
    public final Map<String, String> equipment = new LinkedHashMap<>();
    public int skillPoints = 0;

    public Actor(String name, String sprite, String className, int maxHp, int maxMp, int attack, int defense) {
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
    }

    public boolean alive() {
        return hp > 0;
    }

    public void healFull() {
        hp = maxHp;
        mp = maxMp;
    }

    public int takeDamage(int amount) {
        int damage = Math.max(1, amount - defense);
        hp = Math.max(0, hp - damage);
        return damage;
    }

    public int basicDamage(Random random) {
        int min = Math.max(1, attack - 2);
        int raw = min + random.nextInt(attack + 4 - min + 1);
        raw += skillRank("blade_flurry") * 3;
        if (skillRank("keen_edge") > 0 && random.nextDouble() < skillRank("keen_edge") * 0.08) {
            raw += 8;
        }
        return raw;
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
            int hpGain = 6 + level / 3;
            int mpGain = 2 + level / 4;
            int attackGain = 1 + (level % 3 == 0 ? 1 : 0);
            int defenseGain = level % 2 == 0 ? 1 : 0;
            maxHp += hpGain;
            maxMp += mpGain;
            attack += attackGain;
            defense += defenseGain;
            notes.add("Level " + level + ": +" + hpGain + " HP, +" + mpGain + " MP, +" + attackGain + " ATK, +" + defenseGain + " DEF, +1 skill point.");
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

    public int skillRank(String skillKey) {
        return Math.max(0, skillAllocations.getOrDefault(skillKey, 0));
    }

    public Equipment equippedItem(String slot) {
        String key = equipment.get(slot);
        return key == null ? null : GameData.EQUIPMENT.get(key);
    }

    public String equipItem(String itemKey) {
        Equipment item = GameData.EQUIPMENT.get(itemKey);
        if (item == null) {
            return "That item cannot be equipped.";
        }
        if (!hasItem(itemKey)) {
            return "You do not have " + item.name() + ".";
        }
        String oldKey = equipment.get(item.slot());
        if (oldKey != null) {
            Equipment oldItem = GameData.EQUIPMENT.get(oldKey);
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
        Equipment item = GameData.EQUIPMENT.get(itemKey);
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
        if (direction > 0) {
            hp += item.hpBonus();
            mp += item.mpBonus();
        }
    }
}
