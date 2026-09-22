package com.alderfall.game;

import java.util.Locale;

final class AmbientTownConversationLibrary {
    private AmbientTownConversationLibrary() {
    }

    static Draft draftFor(Npc speaker, Npc listener, String phase, String place, int seed) {
        // Keep both sides authored together so a regional custom gets a relevant reply.
        String[] local = speaker == null ? null : HearthlandsFolklore.conversation(speaker.mapId(), seed / 3);
        if (local != null && listener != null && speaker.mapId().equals(listener.mapId())
                && Math.floorMod(seed, 3) != 0) {
            return new Draft(speaker.name() + ": " + local[0], listener.name() + ": " + local[1],
                    "hearthlands:" + speaker.mapId() + ":" + Math.floorMod(seed / 3, 6));
        }
        Role speakerRole = roleFor(speaker);
        Role listenerRole = roleFor(listener);
        String normalizedPhase = normalize(phase);
        String normalizedPlace = place == null ? "town" : place;
        String speakerLine = speakerLine(speaker, speakerRole, normalizedPlace, normalizedPhase, seed);
        String listenerLine = listenerLine(listener, listenerRole, speakerRole, normalizedPlace, normalizedPhase, seed + 17);
        String key = normalize(speakerRole.name()) + ":" + normalize(listenerRole.name()) + ":"
                + Math.floorMod(seed, 3) + ":" + normalize(normalizedPhase) + ":" + normalize(normalizedPlace);
        return new Draft(speakerLine, listenerLine, key);
    }

    private static String speakerLine(Npc speaker, Role role, String place, String phase, int seed) {
        String name = speaker == null ? "Citizen" : speaker.name();
        return switch (role) {
            case FARMER -> pick(seed,
                    name + ": The field rows looked kind at dawn, but grain turns sour on us if the wagons wait too long.",
                    name + ": Everyone praises bread at supper and forgets it began arguing with the soil before sunrise.",
                    name + ": If threshing slips again, the whole market will taste the delay by tomorrow."
            );
            case WOODCUTTER -> pick(seed,
                    name + ": The timber came down clean today. Now I need the carts to stay honest on the road back.",
                    name + ": Fresh-cut wood lies about its weight until you ask your shoulders instead of your eyes.",
                    name + ": If the saw pit runs late again, every stove in town will blame the weather instead of the yard."
            );
            case HERBALIST -> pick(seed,
                    name + ": I can tell who bruised the herb baskets by how fast the whole street starts smelling bitter.",
                    name + ": Roots gathered after sunrise keep less of their good sense. People notice when the tonics do too.",
                    name + ": Drying racks are full already. That means either a good season or a worried town."
            );
            case GUARD -> pick(seedWithPhase(seed, phase),
                    name + ": Morning watch turned up more carts than trouble. I would like that ratio kept.",
                    name + ": Dusk makes everyone walk like the gate might close behind them.",
                    name + ": Night watch hears every loose hinge in the district and every liar pretending it is just the wind."
            );
            case MERCHANT -> pick(seed,
                    name + ": Half my day is counting what arrived. The other half is explaining why counting mattered.",
                    name + ": The market looks cheerful until the third wagon misses its turn and every promise starts slipping.",
                    name + ": Coin is easy. Getting the right goods through the right gate before rain is the real trick."
            );
            case SMITH -> pick(seed,
                    name + ": If the forge runs cold for an hour, the whole town discovers three urgent repairs at once.",
                    name + ": People only notice hinges, nails, and blades when one of them fails in public.",
                    name + ": The anvil has listened to more honest complaints today than the council has all week."
            );
            case SCHOLAR -> pick(seed,
                    name + ": By midday I have copied the same mistake three times and people still call it tradition.",
                    name + ": Records would settle half these arguments if anyone read past the first convenient line.",
                    name + ": Maps are gentle until someone asks them to prove a rumor."
            );
            case HEALER -> pick(seed,
                    name + ": Everyone swears they are fine until the limp becomes visible from across the square.",
                    name + ": I spent the morning convincing people that rest is cheaper than turning a bruise into a prayer.",
                    name + ": Bandages vanish faster on busy days, even when pride insists it did not need them."
            );
            case COOK -> pick(seed,
                    name + ": The pot tells me more about the town than the council does. Hungry people are honest.",
                    name + ": If the bread lands late, every friendly conversation in " + place + " gets sharper around the edges.",
                    name + ": I trust an oven more than rumor. At least heat admits what it is doing."
            );
            case WORKER -> pick(seed,
                    name + ": Mortar and timber both complain less than people, but they remember neglect longer.",
                    name + ": Every loose brace in town waits for the busiest hour to prove it was ignored.",
                    name + ": Repairs always sound optional right up until the floor argues back."
            );
            case RESIDENT -> pick(seedWithPhase(seed, phase),
                    name + ": " + place + " sounds busier every week.",
                    name + ": You can tell what kind of day it is here by how fast the square starts arguing.",
                    name + ": A calm street is nice. A useful street is better."
            );
        };
    }

    private static String listenerLine(Npc listener, Role listenerRole, Role speakerRole, String place, String phase, int seed) {
        String name = listener == null ? "Citizen" : listener.name();
        if (speakerRole == Role.FARMER && listenerRole == Role.MERCHANT) {
            return pick(seed,
                    name + ": Then I want scales ready before the mill queue starts blaming the road.",
                    name + ": Good. I would rather price grain than explain an empty stall.",
                    name + ": Then I am opening the ledger early, before the market turns impatient."
            );
        }
        if (speakerRole == Role.HERBALIST && listenerRole == Role.HEALER) {
            return pick(seed,
                    name + ": Good. Bruised leaves make weak medicine and twice the work for me.",
                    name + ": Then send me the clean bundles first. Patients notice when shortcuts touch the cup.",
                    name + ": Better a full drying rack than another line of people pretending they can wait."
            );
        }
        if (speakerRole == Role.WOODCUTTER && (listenerRole == Role.SMITH || listenerRole == Role.WORKER)) {
            return pick(seed,
                    name + ": Good timber keeps my work from becoming apology work.",
                    name + ": Then stack the straight pieces where sensible hands can reach them first.",
                    name + ": Fine. Honest wood saves us from dishonest repairs."
            );
        }
        if (speakerRole == Role.GUARD && listenerRole == Role.MERCHANT) {
            return pick(seedWithPhase(seed, phase),
                    name + ": Good. Busy gates are easier to trust than quiet ones with excuses.",
                    name + ": Then I will keep the front stall moving before the queue becomes a rumor mill.",
                    name + ": Better that than another evening spent arguing over whose cart was almost first."
            );
        }
        return switch (listenerRole) {
            case FARMER -> pick(seed,
                    name + ": Then let the wagons hurry for once. Fields do not wait because townfolk got distracted.",
                    name + ": Good. A late hand in the field becomes a late table by sundown.",
                    name + ": Then we keep the rows moving and let the gossip catch up later."
            );
            case WOODCUTTER -> pick(seed,
                    name + ": Good. Wet timber lies to everyone except the saw.",
                    name + ": Then stack it right the first time and the yard stays honest.",
                    name + ": Better that than spending dusk teaching a cartwheel the same lesson twice."
            );
            case HERBALIST -> pick(seed,
                    name + ": Good. Leaves keep their manners longer when nobody bruises them on the way in.",
                    name + ": Then I will sort the clean bundles first and let the careless ones explain themselves.",
                    name + ": Better a little extra drying time than another bad tincture with a pretty label."
            );
            case GUARD -> pick(seedWithPhase(seed, phase),
                    name + ": Good. I would rather count workers than excuses before the lamps come on.",
                    name + ": Then the gate can stay orderly instead of dramatic.",
                    name + ": Busy streets are fine. Streets that stop paying attention are not."
            );
            case MERCHANT -> pick(seed,
                    name + ": Then I want the stalls ready before the square remembers how to complain.",
                    name + ": Good. Supply makes people generous right up until it disappears.",
                    name + ": Better that than another hour of customers acting surprised that roads matter."
            );
            case SMITH -> pick(seed,
                    name + ": Good. If the bolts hold, nobody notices me, and that is usually a compliment.",
                    name + ": Then I can keep the forge on work instead of emergency theater.",
                    name + ": Fine by me. I prefer metal heated for plans, not panic."
            );
            case SCHOLAR -> pick(seed,
                    name + ": Then I will write down the useful part before someone improves it into nonsense.",
                    name + ": Good. Facts travel badly once the square starts decorating them.",
                    name + ": Then perhaps today’s ledger will survive the telling."
            );
            case HEALER -> pick(seed,
                    name + ": Good. Tired people always try to bargain with their own bodies and lose.",
                    name + ": Then I will keep bandages ready before pride gets another vote.",
                    name + ": Better that than patching preventable trouble after dusk."
            );
            case COOK -> pick(seed,
                    name + ": Good. Feed them on time and half the town stops inventing reasons to be cruel.",
                    name + ": Then I will keep the pots ahead of the queue for once.",
                    name + ": Fine. Hunger makes every ordinary complaint sound like prophecy."
            );
            case WORKER -> pick(seed,
                    name + ": Good. Honest materials save us from decorative disasters later.",
                    name + ": Then we keep the braces, mortar, and hands moving in the same direction.",
                    name + ": Better that than explaining why a quick repair became a public problem."
            );
            case RESIDENT -> pick(seedWithPhase(seed, phase),
                    name + ": Busy is good. Busy means people still expect tomorrow to arrive.",
                    name + ": I will take that over a silent street any day.",
                    name + ": Good. A town that still works still believes in itself a little."
            );
        };
    }

    private static Role roleFor(Npc npc) {
        if (npc == null) {
            return Role.RESIDENT;
        }
        if (npc.job() != null) {
            return switch (npc.job().kind()) {
                case FARMER -> Role.FARMER;
                case WOODCUTTER -> Role.WOODCUTTER;
                case HERBALIST -> Role.HERBALIST;
            };
        }
        if (npc.shopId() != null && GameData.SHOPS.containsKey(npc.shopId())) {
            String shopName = GameData.SHOPS.get(npc.shopId()).name().toLowerCase(Locale.ROOT);
            if (shopName.contains("smith") || shopName.contains("forge")) {
                return Role.SMITH;
            }
            if (shopName.contains("apothecary") || shopName.contains("healer") || shopName.contains("alchemist") || shopName.contains("clinic")) {
                return Role.HEALER;
            }
            if (shopName.contains("inn") || shopName.contains("bakery") || shopName.contains("restaurant") || shopName.contains("tavern")) {
                return Role.COOK;
            }
            return Role.MERCHANT;
        }
        String text = (npc.name() + " " + npc.sprite() + " " + String.join(" ", npc.dialog())).toLowerCase(Locale.ROOT);
        if (text.contains("guard") || text.contains("captain") || text.contains("warden") || text.contains("marshal")) {
            return Role.GUARD;
        }
        if (text.contains("farmer") || text.contains("field") || text.contains("orchard")) {
            return Role.FARMER;
        }
        if (text.contains("woodcutter") || text.contains("logger") || text.contains("timber")) {
            return Role.WOODCUTTER;
        }
        if (text.contains("herbalist") || text.contains("forager") || text.contains("herb")) {
            return Role.HERBALIST;
        }
        if (text.contains("smith") || text.contains("forge") || text.contains("hammer") || text.contains("anvil")) {
            return Role.SMITH;
        }
        if (text.contains("scribe") || text.contains("archive") || text.contains("scholar") || text.contains("cartographer")) {
            return Role.SCHOLAR;
        }
        if (text.contains("healer") || text.contains("medic") || text.contains("physician") || text.contains("apothecary")) {
            return Role.HEALER;
        }
        if (text.contains("baker") || text.contains("cook") || text.contains("inn") || text.contains("bartender")) {
            return Role.COOK;
        }
        if (text.contains("merchant") || text.contains("trader") || text.contains("clerk") || text.contains("quartermaster")) {
            return Role.MERCHANT;
        }
        if (text.contains("worker") || text.contains("mason") || text.contains("carpenter") || text.contains("bridge")) {
            return Role.WORKER;
        }
        return Role.RESIDENT;
    }

    private static int seedWithPhase(int seed, String phase) {
        return seed + (phase == null ? 0 : phase.hashCode());
    }

    private static String pick(int seed, String... lines) {
        if (lines == null || lines.length == 0) {
            return "";
        }
        return lines[Math.floorMod(seed, lines.length)];
    }

    private static String normalize(String text) {
        return text == null ? "" : text.strip().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    enum Role {
        FARMER,
        WOODCUTTER,
        HERBALIST,
        GUARD,
        MERCHANT,
        SMITH,
        SCHOLAR,
        HEALER,
        COOK,
        WORKER,
        RESIDENT
    }

    record Draft(String speakerLine, String listenerLine, String key) {
        Draft {
            speakerLine = speakerLine == null ? "" : speakerLine.replaceAll("\\s+", " ").strip();
            listenerLine = listenerLine == null ? "" : listenerLine.replaceAll("\\s+", " ").strip();
            key = key == null || key.isBlank() ? speakerLine + "|" + listenerLine : key.strip();
        }
    }
}
