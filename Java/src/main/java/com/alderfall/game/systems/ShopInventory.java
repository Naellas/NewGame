package com.alderfall.game;

import java.util.*;

/** Per-merchant inventory; browsing, saving and level changes cannot reroll a delivery. */
public final class ShopInventory {
    private record Delivery(int day, Map<String, Integer> items) {}
    private final Map<String, Delivery> deliveries = new LinkedHashMap<>();

    public Map<String, Integer> stock(String merchant, Shop shop, InteriorStyle region, int day, int interval) {
        Delivery previous = deliveries.get(merchant);
        if (previous == null || day >= (long) previous.day + GameConfig.clampShopRestockDays(interval)) {
            int deliveryDay = previous == null ? day : previous.day
                    + ((day - previous.day) / GameConfig.clampShopRestockDays(interval)) * GameConfig.clampShopRestockDays(interval);
            Map<String, Integer> items = new LinkedHashMap<>();
            Set<String> candidates = new TreeSet<>(MerchantStock.candidates(shop));
            AssemblyCrafting.regionalVendorStock(region, Objects.hash(merchant, deliveryDay)).stream()
                    .filter(k -> MerchantStock.accepts(shop.trade(), k, false)).forEach(candidates::add);
            for (String key : candidates) {
                if (!MerchantStock.local(key, region)) continue;
                Random random = new Random(Objects.hash(merchant, deliveryDay, key));
                // Keep curated staples; rotate the wider variant pool independently per item.
                if (!shop.stock().contains(key) && random.nextInt(100) >= 45) continue;
                int amount = GameData.isEquipment(key) ? 1 : MaterialCatalog.get(key) != null
                        ? 4 + random.nextInt(9) : 2 + random.nextInt(5);
                String stockedKey = !shop.stock().contains(key) && GameData.isEquipment(key)
                        ? GameData.rollAffixedKey(key, random) : key;
                items.put(stockedKey, amount);
            }
            previous = new Delivery(deliveryDay, items);
            deliveries.put(merchant, previous);
        }
        return Collections.unmodifiableMap(previous.items);
    }

    public boolean purchase(String merchant, String key) {
        Delivery delivery = deliveries.get(merchant);
        if (delivery == null || delivery.items.getOrDefault(key, 0) <= 0) return false;
        delivery.items.computeIfPresent(key, (k, n) -> n - 1);
        return true;
    }

    public int nextDay(String merchant, int interval) {
        Delivery delivery = deliveries.get(merchant);
        return delivery == null ? 0 : delivery.day + GameConfig.clampShopRestockDays(interval);
    }

    public void clear() { deliveries.clear(); }

    public void write(Properties props) {
        props.setProperty("shops.count", Integer.toString(deliveries.size()));
        int index = 0;
        for (var entry : deliveries.entrySet()) {
            String prefix = "shops." + index++ + ".";
            props.setProperty(prefix + "id", entry.getKey());
            props.setProperty(prefix + "day", Integer.toString(entry.getValue().day));
            StringJoiner items = new StringJoiner(",");
            entry.getValue().items.forEach((k, n) -> items.add(k + ":" + n));
            props.setProperty(prefix + "items", items.toString());
        }
    }

    public void read(Properties props) {
        clear();
        int count = Math.min(100000, number(props.getProperty("shops.count"), 0));
        for (int i = 0; i < count; i++) {
            String prefix = "shops." + i + ".";
            String id = props.getProperty(prefix + "id", "");
            if (id.isBlank()) continue;
            Map<String, Integer> items = new LinkedHashMap<>();
            for (String item : props.getProperty(prefix + "items", "").split(",")) {
                int colon = item.lastIndexOf(':');
                if (colon <= 0) continue;
                String key = item.substring(0, colon);
                int amount = number(item.substring(colon + 1), -1);
                if (amount >= 0 && GameData.itemCost(key) > 0) items.put(key, Math.min(amount, 10000));
            }
            deliveries.put(id, new Delivery(Math.max(1, number(props.getProperty(prefix + "day"), 1)), items));
        }
    }

    private static int number(String text, int fallback) {
        try { return Integer.parseInt(text); } catch (NumberFormatException e) { return fallback; }
    }
}
