package com.alderfall.game;

import com.alderfall.game.inventory.Equipment;
import java.util.*;

/** Persistent containers; an empty entry is retained so looted chests never refill. */
public final class ChestSystem {
    private final Map<String, Map<String, Integer>> containers = new LinkedHashMap<>();

    public static boolean isChest(WorldProp prop) {
        return Set.of("dungeon_prop_castle_chest", "interior_ironbound_chest",
                "interior_inn_screen_chest").contains(prop.asset());
    }

    private static String id(String mapId, WorldProp prop) {
        return mapId + ":" + prop.x() + ":" + prop.y() + ":" + prop.asset();
    }

    public Map<String, Integer> contents(String mapId, WorldProp prop) {
        return containers.computeIfAbsent(id(mapId, prop), key -> loot(key));
    }

    private static Map<String, Integer> loot(String key) {
        Random random = new Random(0x43484553544CL ^ key.hashCode());
        Map<String, Integer> result = new LinkedHashMap<>();
        List<String> materials = MaterialCatalog.all().stream().filter(m -> m.tier() <= 2)
                .map(MaterialCatalog.Material::key).sorted().toList();
        for (int i = 0, rolls = 2 + random.nextInt(3); i < rolls; i++) {
            result.merge(materials.get(random.nextInt(materials.size())), 2 + random.nextInt(5), Integer::sum);
        }
        List<Equipment> weapons = GameData.EQUIPMENT.values().stream()
                .filter(e -> e.slot().equals("weapon") && e.minLevel() <= 3)
                .sorted(Comparator.comparing(Equipment::key)).toList();
        if (!weapons.isEmpty()) result.put(weapons.get(random.nextInt(weapons.size())).key(), 1);
        result.put(random.nextBoolean() ? "health_potion" : "mana_potion", 1 + random.nextInt(3));
        return result;
    }

    public static int transfer(Map<String, Integer> from, Map<String, Integer> to, String key, int requested) {
        if (from == to || key == null || requested <= 0) return 0;
        int available = from.getOrDefault(key, 0);
        int existing = to.getOrDefault(key, 0);
        int amount = (int) Math.min(Math.min((long) available, requested), (long) Integer.MAX_VALUE - existing);
        if (amount <= 0) return 0;
        if (amount == available) from.remove(key); else from.put(key, available - amount);
        to.put(key, existing + amount);
        return amount;
    }

    public void clear() { containers.clear(); }

    public void write(Properties props) {
        props.setProperty("chests.count", Integer.toString(containers.size()));
        int index = 0;
        for (var entry : containers.entrySet()) {
            String prefix = "chests." + index++ + ".";
            props.setProperty(prefix + "id", entry.getKey());
            StringJoiner items = new StringJoiner(",");
            entry.getValue().forEach((key, count) -> items.add(key + ":" + count));
            props.setProperty(prefix + "items", items.toString());
        }
    }

    public void read(Properties props) {
        clear();
        int count = number(props.getProperty("chests.count", "0"));
        for (int i = 0; i < Math.min(count, 100000); i++) {
            String prefix = "chests." + i + ".";
            String id = props.getProperty(prefix + "id", "");
            if (id.isBlank()) continue;
            Map<String, Integer> items = new LinkedHashMap<>();
            for (String item : props.getProperty(prefix + "items", "").split(",")) {
                int colon = item.lastIndexOf(':');
                if (colon <= 0) continue;
                int amount = number(item.substring(colon + 1));
                if (amount > 0) items.put(item.substring(0, colon), amount);
            }
            containers.put(id, items);
        }
    }

    private static int number(String value) {
        try { return Integer.parseInt(value); } catch (NumberFormatException ignored) { return 0; }
    }
}
