package com.alderfall.game;

import java.util.List;

/** Fixed identities for campaign places. Terrain may vary; a quest never means 'camp number 5'. */
public final class StoryLocationCatalog {
    private StoryLocationCatalog() { }

    public record Place(String id, String name, String region, String mapId, String adventureId,
                        int x, int y, String route, String purpose, String people, String landmark,
                        String asset, List<String> quests) {
        public boolean outdoorSite() { return mapId.equals("overworld") && x > 0; }
        /** Surface layouts reuse the world generator's structured placement rules. */
        public String template() {
            return switch (id) {
                case "crowhook", "highwall_pass" -> "bandit_camp";
                case "redcap_supply_camp" -> "goblin_camp";
                case "banner_cairn" -> "graveyard";
                case "highwall_watch", "glimmerfen_bellworks" -> "ruined_watchpost";
                case "oathstead_timber_road", "snowrest_pass" -> "old_road_marker";
                case "sunken_guest_shrine", "sanctum_forge" -> "guest_shrine";
                case "glass_caravan" -> "caravan_halt";
                case "reedbank_landing" -> "bell_landing";
                case "mireford_reedbed" -> "reed_beds";
                case "briarbridge_tollhouse" -> "tollhouse";
                case "oakhaven_orchard" -> "orchard";
                case "miredepth" -> "cave";
                case "stonegate" -> "crypt";
                case "blackvault" -> "abandoned_castle";
                default -> "existing_map";
            };
        }
    }

    public static final List<Place> PLACES = List.of(
            local("riverside", "Riverside City", "Western river roads", "city_riverside",
                    "Use Riverside Gate on the world map to enter Riverside City.",
                    "The western river city organizing bridge tolls and grain transport.", "Mirella manages supply commitments; Seraphine knows its court households.",
                    "Mirella's NPC marker and the city's river streets."),
            local("archive_city", "Archive City", "Central Crownlands", "city_archive",
                    "Enter through Archive Gate on the world map.",
                    "The central city holding the ward records and the entrance to the Old Oath Vault.", "Selene is the archivist; Maera studies routes; Hollis maintains burial records.",
                    "Selene, Ward Reading Station, and Old Oath Vault entrance."),
            local("highwall", "Highwall City", "Northern March", "city_highwall",
                    "Enter through Highwall Gate on the world map.",
                    "The northern garrison supplying the roadwatch along the mountain approaches.", "Odrick commands the roadwatch; Cassia's history concerns its gate orders.",
                    "Odrick's NPC marker and Highwall Gate."),
            local("sanctum", "Sanctum City", "Southern Sunrealm", "city_sanctum",
                    "Enter through Sanctum Gate on the world map.",
                    "The southern temple city serving travelers on the caravan roads.", "Solari oversees shrine rites; Samir knows the fire-vessel tradition.",
                    "Solari's NPC marker; the guest shrine and forge lie southwest outside the city."),
            local("belltower", "Belltower City", "Fenlands", "city_belltower",
                    "Enter through Belltower Gate on the world map.",
                    "The Fenland harbor whose warning bells and ferries connect the marsh villages.", "Ysra coordinates boat warnings; Lyra treats people along the flooded routes.",
                    "Ysra's NPC marker and the approaches to Reedbank Bell Landing."),
            local("briarbridge", "Briarbridge", "Central-western crossing", "town_briarbridge",
                    "Enter the Briarbridge settlement from its world-map marker.",
                    "The bridge town between Riverside's roads and the central settlements; its Guest Abbey has an accessible orchard and undercroft.",
                    "Bridge workers, abbey gardeners, and travelers use the crossing.", "Briarbridge gate, Guest Abbey entrance, and the tollhouse south of town."),
            local("oakhaven", "Oakhaven", "Central Hearthlands", "village_oakhaven",
                    "Enter Oakhaven near Oathstead on the world map.",
                    "The farming village beside the ward orchard and woodland paths used in Aria's investigation.", "Elder Rowan tends the orchard; Aria investigates her sister's disappearance along the western paths.",
                    "Elder Rowan and the orchard approach southeast of the village."),
            local("snowrest", "Snowrest", "Northern roads", "village_snowrest",
                    "Find Snowrest in the northwest of the world map.",
                    "The winter village whose road supplies and grove protections sustain its households.", "Captain Elric organizes supplies; Goatkeeper Una can give Vesper direct testimony.",
                    "Elric, Una, and the eastward winter-pass road."),
            local("dunewick", "Dunewick", "Southern well roads", "village_dunewick",
                    "Find Dunewick west of Sanctum on the southern roads.",
                    "A southern village where water and healing supplies matter to Rafiq's return.", "Elder Safa receives the quest supplies during Rafiq's revised second chapter.",
                    "The village paths and Safa's active quest marker."),
            local("mireford", "Mireford", "Fenlands", "village_mireford",
                    "Find Mireford north of Belltower on the marsh road.",
                    "The village whose sick residents need medicine and dependable deliveries.", "Eda and clinic attendant Sen appear in Lyra's revised care and delivery chapters.",
                    "Clinic witness markers when their quests are active; fever-reed beds lie south of the village."),
            local("glimmerfen", "Glimmerfen", "Fenlands", "village_glimmerfen",
                    "Find Glimmerfen northeast of Belltower on the road from Mireford.",
                    "The bellwright's village serving the eastern Fenland warning line.", "Bellwright Nessa plans the bell-foundation repairs.",
                    "Nessa and the bell foundations southeast of the village."),
            local("redcairn", "Redcairn", "Southern badlands", "village_redcairn",
                    "Follow Sanctum's eastern road to Redcairn.",
                    "The settlement from which Blackvault's stored magic is investigated.", "Ash-Scribe Damar studies Blackvault's maintenance records.",
                    "Damar's NPC marker; Blackvault Ruins lie west of the village."),
            local("road_shrine", "Burned Road Shrine", "Oathstead", "story_road_shrine",
                    "Enter the Burned Road Shrine from the shrine-path entrance inside Oathstead.",
                    "A travelers' sanctuary where Vaelthara broke a protective enchantment cut into the altar.",
                    "Maelis shelters the survivor in Oathstead; Selene in Archive City can read the surviving carving.",
                    "Burned altar, cracked stone socket, carved fragment, ash deposit.", "ms_wake_ashes", "ms_road_dust"),
            outdoor("oathstead_timber_road", "Oathstead Timber Road", "Oathstead", "", 117, 153,
                    "Leave Oathstead for the timber road beside the camp; use the named red quest marker.",
                    "The short supply road used by Oathstead's timber workers.", "Maelis organizes the workers; six wolves threaten their route.",
                    "A timber-stack marker beside the camp road.", "location_camp_crates", "ms_oathstead_stand"),
            local("archive_reading_station", "Archive Ward Reading Station", "Crownlands", "city_archive",
                    "Find Selene in Archive City, then the marked Ward Maintenance Record and Damaged Ward Index in the city reading station.",
                    "The public reading station holding the shrine maintenance drawings and the index of builders' instructions.",
                    "Selene is the archivist responsible for explaining these records.",
                    "Two labeled document bundles: Ward Maintenance Record and Damaged Ward Index.", "ms_names_dust"),
            outdoor("crowhook", "Crowhook Bandit Camp", "Belltower approaches", "dungeon_crowhook_outpost_1", 214, 218,
                    "Find Crowhook Bandit Camp on the southern approach to Belltower. The red quest markers identify the three cache guards, then the Stolen Vault Instructions.",
                    "A bandit camp holding the Archive papers named in a ransom demand.",
                    "Three cutthroats guard the papers. Selene in Archive City needs the keeper's instructions recovered.",
                    "Crowhook's named camp entrance and the Stolen Vault Instructions cache.", "location_camp_crates", "ms_stolen_index"),
            local("oath_vault", "Old Oath Vault", "Archive City", "story_oath_vault",
                    "Use the Old Oath Vault entrance inside Archive City. Inspect the Vault Entry Seal, then the Memory Pedestal.",
                    "The chamber beneath the Archive containing the Stone of Memory and a mural of the twelve stone sockets.",
                    "Selene interprets the recovered keeper's instructions; the player performs the inspection.",
                    "Entry seal, twelve-socket mural, and pedestal labeled Memory.", "ms_first_socket"),
            outdoor("highwall_watch", "Highwall Cairn Watch", "Highwall March", "", 198, 86,
                    "Leave Highwall Gate and follow the named Highwall Cairn Watch marker southwest of the city. Inspect the Split Signal Bell, Cairn Watch Names, and Copied Watch Signal.",
                    "A roadwatch post beside the stone burial mounds of Highwall's former watchmen.",
                    "Odrick commands the living watch. Local burial keepers maintain the cairns and recite the dead watchmen's names each spring.",
                    "A split signal bell, a watch-name inscription, and a scratched safe-passage signal.", "location_graveyard_tombstones", "ms_watchtower_bells"),
            outdoor("highwall_pass", "Highwall Supply Pass", "Highwall March", "", 217, 91,
                    "Follow Highwall's southeastern road to Highwall Supply Pass. The three red encounter markers identify the orc brutes blocking the carts.",
                    "The cart road between Highwall's gate and its southern supply routes.",
                    "Odrick needs three orc brutes removed before he sends roadwatch and repair crews.",
                    "Road barricade and abandoned supply crates.", "location_camp_crates", "ms_raiders_pass"),
            outdoor("banner_cairn", "Banner Cairn", "Highwall March", "", 204, 65,
                    "Find Banner Cairn north of Highwall Gate. Defeat Kharvok at the red encounter marker, then inspect Kharvok's Fallen Standard at the same cairn.",
                    "A northern burial mound occupied by Kharvok, who has sewn the names of dead watchmen into his command banner.",
                    "Kharvok is the hostile commander. Odrick wants the altered banner examined after the battle.",
                    "A named burial mound and the fallen standard revealed after Kharvok's defeat.", "location_graveyard_tombstones", "ms_frosthollow_standard"),
            outdoor("sunken_guest_shrine", "Sunken Guest Shrine", "Sanctum Sunrealm", "", 141, 242,
                    "Go southwest from Sanctum Gate to Sunken Guest Shrine. Inspect the Cracked Guest Cup, Old Welcome Inscription, and Sealed Vessel Outlet.",
                    "A well-road sanctuary where a fire spirit was welcomed into a vessel to warm and protect travelers.",
                    "Solari is the Sanctum priest investigating why the vessel began burning its keepers.",
                    "Guest cup, words cut into its stone stand, and an iron-plugged outlet.", "location_camp_fire", "ms_shrine_shadow"),
            outdoor("glass_caravan", "Glass Caravan Halt", "Sanctum Sunrealm", "", 132, 249,
                    "Follow the road southwest from Sanctum toward Embermarket. Find Glass Caravan Halt and the six marked ember imps around it.",
                    "The stopped caravan carrying glass fire vessels and lamp supplies for the southern shrines.",
                    "Solari needs the six imps driven off so a recovery party can reach the stores.",
                    "Caravan crates beside an extinguished camp hearth.", "location_camp_crates", "ms_caravan_glass"),
            outdoor("sanctum_forge", "Sunken Shrine Forge", "Sanctum Sunrealm", "", 142, 244,
                    "Return to Sunken Guest Shrine southwest of Sanctum. Its workshop is marked Sunken Shrine Forge. Inspect Ember Transfer Cradle, use Ember Cradle Release, then open Guest Vessel Outlet.",
                    "The workshop behind Sunken Guest Shrine's altar. Its transfer channel connects Ember to the same fire vessel examined in the shrine investigation.",
                    "Solari authorizes disconnecting the stone and opening the spirit's exit.",
                    "Transfer cradle, release lever, and the vessel's outlet.", "location_camp_fire", "ms_ember_socket_rite"),
            outdoor("reedbank_landing", "Reedbank Bell Landing", "Belltower Fenlands", "", 241, 192,
                    "Follow Belltower's southeastern water road to Reedbank Bell Landing. Check the three marked bell-rope fittings.",
                    "A ferry landing whose bell should guide boats to the bank; an unattended bell has been sounding there.",
                    "Ysra directs the bellkeepers and navigators who use this signal.",
                    "Landing bell, three rope fittings, and a reed shrine.", "location_graveyard_skull_marker", "ms_bell_alone"),
            outdoor("mireford_reedbed", "Mireford Fever-Reed Beds", "Belltower Fenlands", "", 245, 160,
                    "Leave Mireford by its southern road. Collect six marked fever-reed samples at Mireford Fever-Reed Beds, then return to Ysra in Belltower.",
                    "The reed beds supplying material for Mireford's fever remedies.",
                    "Ysra arranges the gathering; preparation and patient care follow afterward.",
                    "Six labeled Fever Reed gathering points.", "deco_soft_water_reeds_gold", "ms_medicine_mireford"),
            outdoor("miredepth", "Miredepth Cave", "Belltower Fenlands", "dungeon_miredepth_1", 255, 177,
                    "Find Miredepth Cave east of Belltower. The red quest encounter and the cave lead to Velmora's threat; report her defeat to Ysra.",
                    "A flooded cave associated with bells that call the drowned dead against living travelers.",
                    "Velmora is the hostile power called Bell-Drowned. Ysra knows the current threat, not a complete account of Velmora's life.",
                    "Miredepth's named cave entrance and Velmora's encounter marker.", "location_dungeon_stair_entrance", "ms_miredepth_below"),
            outdoor("briarbridge_tollhouse", "Briarbridge Tollhouse", "Western river roads", "", 128, 128,
                    "Find Briarbridge Tollhouse just south of Briarbridge. Read the three marked Toll Ledger entries, then report to Mirella in Riverside.",
                    "The bridge toll station recording grain shipments along the road toward Oathstead.",
                    "Mirella administers Riverside's supply arrangements; the written entries are the player's evidence.",
                    "Tollhouse document table and three labeled ledger entries.", "quest_document_bundle", "ms_toll_ledger"),
            outdoor("redcap_supply_camp", "Redcap Supply Camp", "Riverside Reach", "", 93, 119,
                    "Take Riverside's southeastern road to Redcap Supply Camp. Follow the marked raiders, then the scavenger carrying the stolen stone.",
                    "A raider camp intercepting the grain route between Riverside and the central settlements.",
                    "Mirella needs the eight raiders stopped; the follow-up identifies one scavenger for the Hunger recovery.",
                    "Stolen supply crates and red quest encounter markers.", "location_camp_crates", "ms_redcap_trade", "ms_glowing_mud"),
            outdoor("oakhaven_orchard", "Oakhaven Ward Orchard", "Central Hearthlands", "", 130, 157,
                    "Leave Oakhaven for the orchard southeast of the village. Find Rootmaw at the Oakhaven Ward Orchard encounter marker.",
                    "The orchard feeding Oakhaven, where protective magic passes through the trees and a stag once received the first fallen apple.",
                    "Elder Rowan tends the orchard. Rootmaw is the guardian now attacking its workers.",
                    "Old orchard ward stone and Rootmaw's encounter.", "quest_ward_marker", "ms_orchard_ward"),
            outdoor("stonegate", "Stonegate Crypt", "Northern Crownlands", "dungeon_stonegate_1", 196, 62,
                    "Find Stonegate Crypt northwest of Highwall. Stop the Nameless Warden at its marked encounter or in the crypt, then return to Gravekeeper Hollis in Archive City.",
                    "A burial place whose damaged names may prevent the dead from being released from their duties.",
                    "Hollis maintains the burial records. The Nameless Warden attacks visitors.",
                    "Named crypt entrance and the Warden's encounter marker.", "location_graveyard_tombstones", "ms_names_cold_stone"),
            outdoor("snowrest_pass", "Snowrest Winter Pass", "Northern roads", "", 94, 67,
                    "Follow the road east from Snowrest to Snowrest Winter Pass. Stop the Hailback Broodmother at its red encounter marker.",
                    "Snowrest's winter supply approach, blocked by a mountain predator.",
                    "Captain Elric organizes the supply runs; the Hailback Broodmother is a local beast, not a proven servant of Vaelthara.",
                    "Winter road marker and the Broodmother's encounter.", "deco_imagen_signpost", "ms_cold_road"),
            outdoor("glimmerfen_bellworks", "Glimmerfen Bell Foundations", "Belltower Fenlands", "", 272, 145,
                    "Find Glimmerfen Bell Foundations southeast of Glimmerfen. Inspect both marked foundations, then report to Bellwright Nessa in the village.",
                    "The old supports for the warning line between Glimmerfen and neighboring landings.",
                    "Nessa is the bellwright planning the repair and recovering the Stone of Bells.",
                    "Two labeled Old Bell Foundation inspection points.", "quest_ward_marker", "ms_missing_bell_rope"),
            outdoor("blackvault", "Blackvault Ruins", "Southern badlands", "dungeon_blackvault_1", 194, 235,
                    "Find Blackvault Ruins west of Redcairn. Defeat Sareth at the marked encounter or in the ruins, then return to Ash-Scribe Damar in Redcairn.",
                    "The old repository for spent magical power from the region's protective enchantments.",
                    "Damar studies the maintenance books. Sareth blocks recovery of the Stone of Ash.",
                    "Blackvault's named entrance and Sareth's encounter marker.", "quest_ward_marker", "ms_blackvault_mark"),
            local("oathstead", "Oathstead Camp", "Central crossroads", "village_oathstead_camp",
                    "Return to Maelis in Oathstead. Use the camp defense point for Morvane's raid; carry the mainland representatives' answers back to Maelis afterward.",
                    "The refuge that shelters the player after the shrine attack, and the home the campaign asks them to protect.",
                    "Maelis organizes its residents. Mirella, Odrick, Selene, Ysra, and Solari are contacted in their respective cities.",
                    "Camp hearth, Maelis, and the defense point.", "ms_camp_defending", "ms_kingdoms_answer"),
            local("old_gate", "Old Gate of Alderfall", "Ancient command route", "overworld",
                    "Accept Maelis's Gate quest after collecting all twelve stones. Follow the Old Gate of Alderfall marker and interact with the portal.",
                    "The twelve-stone portal through which the existing game starts the confrontation with Vaelthara at the Hollow Throne.",
                    "Vaelthara demands the stones and control over Oathstead's people. Maelis remains responsible for the camp.",
                    "The existing twelve-stone portal marker.", "ms_twelve_stones_gate")
    );

    public static Place forQuest(String questId) {
        return PLACES.stream().filter(p -> p.quests().contains(questId)).findFirst().orElse(null);
    }
    public static Place byId(String id) {
        return PLACES.stream().filter(p -> p.id().equals(id)).findFirst().orElseThrow();
    }
    private static Place outdoor(String id, String name, String region, String adventure, int x, int y,
                                 String route, String purpose, String people, String landmark, String asset, String... quests) {
        return new Place(id, name, region, "overworld", adventure, x, y, route, purpose, people, landmark, asset, List.of(quests));
    }
    private static Place local(String id, String name, String region, String map, String route, String purpose,
                               String people, String landmark, String... quests) {
        return new Place(id, name, region, map, "", 0, 0, route, purpose, people, landmark, "quest_ward_marker", List.of(quests));
    }
}
