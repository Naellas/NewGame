# Alderfall location library

Generated from `StoryLocationCatalog` by `StoryLocationTest`. This is the current location reference for the 26 main quests and their supporting settlements. Use these names in offers, journal entries, directions, and map labels. A location's identity and regional anchor are fixed; its exact walkable tile can adjust to generated terrain.

The 18 outdoor destinations have generated surface layouts, road approaches, permanent world-map icons, hover descriptions, and named quest interactions. Map labels avoid one another; zoom or hover over an icon to read a crowded place's name. Camp, graveyard, and watchpost layouts reuse the existing location templates. Orchard, reed-bed, landing, caravan, tollhouse, and guest-shrine layouts use their placement rules and existing assets. The shrine and forge occupy separate wings of one compound. Only locations described as existing entrances have a separate accessible map. The existing shrine/vault interiors and settlement maps remain in use.

Read with [the dialogue clarity rules](dialogue-clarity-and-place-review.md), [the story bible](world-framework.md), and [the actual campaign dialogue](../Java/docs/main-story-dialogue.md).

## Riverside City

Identity: `riverside`. Region: Western river roads.

**What it is:** The western river city organizing bridge tolls and grain transport.

**Who matters here:** Mirella manages supply commitments; Seraphine knows its court households.

**How the player finds it:** Use Riverside Gate on the world map to enter Riverside City.

**What the player can recognize:** Mirella's NPC marker and the city's river streets.

Game map: `city_riverside`.

## Archive City

Identity: `archive_city`. Region: Central Crownlands.

**What it is:** The central city holding the ward records and the entrance to the Old Oath Vault.

**Who matters here:** Selene is the archivist; Maera studies routes; Hollis maintains burial records.

**How the player finds it:** Enter through Archive Gate on the world map.

**What the player can recognize:** Selene, Ward Reading Station, and Old Oath Vault entrance.

Game map: `city_archive`.

## Highwall City

Identity: `highwall`. Region: Northern March.

**What it is:** The northern garrison supplying the roadwatch along the mountain approaches.

**Who matters here:** Odrick commands the roadwatch; Cassia's history concerns its gate orders.

**How the player finds it:** Enter through Highwall Gate on the world map.

**What the player can recognize:** Odrick's NPC marker and Highwall Gate.

Game map: `city_highwall`.

## Sanctum City

Identity: `sanctum`. Region: Southern Sunrealm.

**What it is:** The southern temple city serving travelers on the caravan roads.

**Who matters here:** Solari oversees shrine rites; Samir knows the fire-vessel tradition.

**How the player finds it:** Enter through Sanctum Gate on the world map.

**What the player can recognize:** Solari's NPC marker; the guest shrine and forge lie southwest outside the city.

Game map: `city_sanctum`.

## Belltower City

Identity: `belltower`. Region: Fenlands.

**What it is:** The Fenland harbor whose warning bells and ferries connect the marsh villages.

**Who matters here:** Ysra coordinates boat warnings; Lyra treats people along the flooded routes.

**How the player finds it:** Enter through Belltower Gate on the world map.

**What the player can recognize:** Ysra's NPC marker and the approaches to Reedbank Bell Landing.

Game map: `city_belltower`.

## Briarbridge

Identity: `briarbridge`. Region: Central-western crossing.

**What it is:** The bridge town between Riverside's roads and the central settlements; its Guest Abbey has an accessible orchard and undercroft.

**Who matters here:** Bridge workers, abbey gardeners, and travelers use the crossing.

**How the player finds it:** Enter the Briarbridge settlement from its world-map marker.

**What the player can recognize:** Briarbridge gate, Guest Abbey entrance, and the tollhouse south of town.

Game map: `town_briarbridge`.

## Oakhaven

Identity: `oakhaven`. Region: Central Hearthlands.

**What it is:** The farming village beside the ward orchard and woodland paths used in Aria's investigation.

**Who matters here:** Elder Rowan tends the orchard; Aria investigates her sister's disappearance along the western paths.

**How the player finds it:** Enter Oakhaven near Oathstead on the world map.

**What the player can recognize:** Elder Rowan and the orchard approach southeast of the village.

Game map: `village_oakhaven`.

## Snowrest

Identity: `snowrest`. Region: Northern roads.

**What it is:** The winter village whose road supplies and grove protections sustain its households.

**Who matters here:** Captain Elric organizes supplies; Goatkeeper Una can give Vesper direct testimony.

**How the player finds it:** Find Snowrest in the northwest of the world map.

**What the player can recognize:** Elric, Una, and the eastward winter-pass road.

Game map: `village_snowrest`.

## Dunewick

Identity: `dunewick`. Region: Southern well roads.

**What it is:** A southern village where water and healing supplies matter to Rafiq's return.

**Who matters here:** Elder Safa receives the quest supplies during Rafiq's revised second chapter.

**How the player finds it:** Find Dunewick west of Sanctum on the southern roads.

**What the player can recognize:** The village paths and Safa's active quest marker.

Game map: `village_dunewick`.

## Mireford

Identity: `mireford`. Region: Fenlands.

**What it is:** The village whose sick residents need medicine and dependable deliveries.

**Who matters here:** Eda and clinic attendant Sen appear in Lyra's revised care and delivery chapters.

**How the player finds it:** Find Mireford north of Belltower on the marsh road.

**What the player can recognize:** Clinic witness markers when their quests are active; fever-reed beds lie south of the village.

Game map: `village_mireford`.

## Glimmerfen

Identity: `glimmerfen`. Region: Fenlands.

**What it is:** The bellwright's village serving the eastern Fenland warning line.

**Who matters here:** Bellwright Nessa plans the bell-foundation repairs.

**How the player finds it:** Find Glimmerfen northeast of Belltower on the road from Mireford.

**What the player can recognize:** Nessa and the bell foundations southeast of the village.

Game map: `village_glimmerfen`.

## Redcairn

Identity: `redcairn`. Region: Southern badlands.

**What it is:** The settlement from which Blackvault's stored magic is investigated.

**Who matters here:** Ash-Scribe Damar studies Blackvault's maintenance records.

**How the player finds it:** Follow Sanctum's eastern road to Redcairn.

**What the player can recognize:** Damar's NPC marker; Blackvault Ruins lie west of the village.

Game map: `village_redcairn`.

## Burned Road Shrine

Identity: `road_shrine`. Region: Oathstead.

**What it is:** A travelers' sanctuary where Vaelthara broke a protective enchantment cut into the altar.

**Who matters here:** Maelis shelters the survivor in Oathstead; Selene in Archive City can read the surviving carving.

**How the player finds it:** Enter the Burned Road Shrine from the shrine-path entrance inside Oathstead.

**What the player can recognize:** Burned altar, cracked stone socket, carved fragment, ash deposit.

Game map: `story_road_shrine`. Quest bindings: ms_wake_ashes, ms_road_dust

## Oathstead Timber Road

Identity: `oathstead_timber_road`. Region: Oathstead.

**What it is:** The short supply road used by Oathstead's timber workers.

**Who matters here:** Maelis organizes the workers; six wolves threaten their route.

**How the player finds it:** Leave Oathstead for the timber road beside the camp; use the named red quest marker.

**What the player can recognize:** A timber-stack marker beside the camp road.

Game map: `overworld`. Preferred world anchor: (117, 153). Layout: `old_road_marker`. Seed-0 map marker: (117, 151). Quest bindings: ms_oathstead_stand

## Archive Ward Reading Station

Identity: `archive_reading_station`. Region: Crownlands.

**What it is:** The public reading station holding the shrine maintenance drawings and the index of builders' instructions.

**Who matters here:** Selene is the archivist responsible for explaining these records.

**How the player finds it:** Find Selene in Archive City, then the marked Ward Maintenance Record and Damaged Ward Index in the city reading station.

**What the player can recognize:** Two labeled document bundles: Ward Maintenance Record and Damaged Ward Index.

Game map: `city_archive`. Quest bindings: ms_names_dust

## Crowhook Bandit Camp

Identity: `crowhook`. Region: Belltower approaches.

**What it is:** A bandit camp holding the Archive papers named in a ransom demand.

**Who matters here:** Three cutthroats guard the papers. Selene in Archive City needs the keeper's instructions recovered.

**How the player finds it:** Find Crowhook Bandit Camp on the southern approach to Belltower. The red quest markers identify the three cache guards, then the Stolen Vault Instructions.

**What the player can recognize:** Crowhook's named camp entrance and the Stolen Vault Instructions cache.

Game map: `overworld`. Preferred world anchor: (214, 218). Layout: `bandit_camp`. Seed-0 map marker: (215, 200). Existing adventure entrance: `dungeon_crowhook_outpost_1`. Quest bindings: ms_stolen_index

## Old Oath Vault

Identity: `oath_vault`. Region: Archive City.

**What it is:** The chamber beneath the Archive containing the Stone of Memory and a mural of the twelve stone sockets.

**Who matters here:** Selene interprets the recovered keeper's instructions; the player performs the inspection.

**How the player finds it:** Use the Old Oath Vault entrance inside Archive City. Inspect the Vault Entry Seal, then the Memory Pedestal.

**What the player can recognize:** Entry seal, twelve-socket mural, and pedestal labeled Memory.

Game map: `story_oath_vault`. Quest bindings: ms_first_socket

## Highwall Cairn Watch

Identity: `highwall_watch`. Region: Highwall March.

**What it is:** A roadwatch post beside the stone burial mounds of Highwall's former watchmen.

**Who matters here:** Odrick commands the living watch. Local burial keepers maintain the cairns and recite the dead watchmen's names each spring.

**How the player finds it:** Leave Highwall Gate and follow the named Highwall Cairn Watch marker southwest of the city. Inspect the Split Signal Bell, Cairn Watch Names, and Copied Watch Signal.

**What the player can recognize:** A split signal bell, a watch-name inscription, and a scratched safe-passage signal.

Game map: `overworld`. Preferred world anchor: (198, 86). Layout: `ruined_watchpost`. Seed-0 map marker: (183, 86). Quest bindings: ms_watchtower_bells

## Highwall Supply Pass

Identity: `highwall_pass`. Region: Highwall March.

**What it is:** The cart road between Highwall's gate and its southern supply routes.

**Who matters here:** Odrick needs three orc brutes removed before he sends roadwatch and repair crews.

**How the player finds it:** Follow Highwall's southeastern road to Highwall Supply Pass. The three red encounter markers identify the orc brutes blocking the carts.

**What the player can recognize:** Road barricade and abandoned supply crates.

Game map: `overworld`. Preferred world anchor: (217, 91). Layout: `bandit_camp`. Seed-0 map marker: (224, 91). Quest bindings: ms_raiders_pass

## Banner Cairn

Identity: `banner_cairn`. Region: Highwall March.

**What it is:** A northern burial mound occupied by Kharvok, who has sewn the names of dead watchmen into his command banner.

**Who matters here:** Kharvok is the hostile commander. Odrick wants the altered banner examined after the battle.

**How the player finds it:** Find Banner Cairn north of Highwall Gate. Defeat Kharvok at the red encounter marker, then inspect Kharvok's Fallen Standard at the same cairn.

**What the player can recognize:** A named burial mound and the fallen standard revealed after Kharvok's defeat.

Game map: `overworld`. Preferred world anchor: (204, 65). Layout: `graveyard`. Seed-0 map marker: (215, 71). Quest bindings: ms_frosthollow_standard

## Sunken Guest Shrine

Identity: `sunken_guest_shrine`. Region: Sanctum Sunrealm.

**What it is:** A well-road sanctuary where a fire spirit was welcomed into a vessel to warm and protect travelers.

**Who matters here:** Solari is the Sanctum priest investigating why the vessel began burning its keepers.

**How the player finds it:** Go southwest from Sanctum Gate to Sunken Guest Shrine. Inspect the Cracked Guest Cup, Old Welcome Inscription, and Sealed Vessel Outlet.

**What the player can recognize:** Guest cup, words cut into its stone stand, and an iron-plugged outlet.

Game map: `overworld`. Preferred world anchor: (141, 242). Layout: `guest_shrine`. Seed-0 map marker: (141, 261). Quest bindings: ms_shrine_shadow

## Glass Caravan Halt

Identity: `glass_caravan`. Region: Sanctum Sunrealm.

**What it is:** The stopped caravan carrying glass fire vessels and lamp supplies for the southern shrines.

**Who matters here:** Solari needs the six imps driven off so a recovery party can reach the stores.

**How the player finds it:** Follow the road southwest from Sanctum toward Embermarket. Find Glass Caravan Halt and the six marked ember imps around it.

**What the player can recognize:** Caravan crates beside an extinguished camp hearth.

Game map: `overworld`. Preferred world anchor: (132, 249). Layout: `caravan_halt`. Seed-0 map marker: (129, 254). Quest bindings: ms_caravan_glass

## Sunken Shrine Forge

Identity: `sanctum_forge`. Region: Sanctum Sunrealm.

**What it is:** The workshop behind Sunken Guest Shrine's altar. Its transfer channel connects Ember to the same fire vessel examined in the shrine investigation.

**Who matters here:** Solari authorizes disconnecting the stone and opening the spirit's exit.

**How the player finds it:** Return to Sunken Guest Shrine southwest of Sanctum. Its workshop is marked Sunken Shrine Forge. Inspect Ember Transfer Cradle, use Ember Cradle Release, then open Guest Vessel Outlet.

**What the player can recognize:** Transfer cradle, release lever, and the vessel's outlet.

Game map: `overworld`. Preferred world anchor: (142, 244). Layout: `guest_shrine`. Seed-0 map marker: (143, 264). Quest bindings: ms_ember_socket_rite

## Reedbank Bell Landing

Identity: `reedbank_landing`. Region: Belltower Fenlands.

**What it is:** A ferry landing whose bell should guide boats to the bank; an unattended bell has been sounding there.

**Who matters here:** Ysra directs the bellkeepers and navigators who use this signal.

**How the player finds it:** Follow Belltower's southeastern water road to Reedbank Bell Landing. Check the three marked bell-rope fittings.

**What the player can recognize:** Landing bell, three rope fittings, and a reed shrine.

Game map: `overworld`. Preferred world anchor: (241, 192). Layout: `bell_landing`. Seed-0 map marker: (250, 198). Quest bindings: ms_bell_alone

## Mireford Fever-Reed Beds

Identity: `mireford_reedbed`. Region: Belltower Fenlands.

**What it is:** The reed beds supplying material for Mireford's fever remedies.

**Who matters here:** Ysra arranges the gathering; preparation and patient care follow afterward.

**How the player finds it:** Leave Mireford by its southern road. Collect six marked fever-reed samples at Mireford Fever-Reed Beds, then return to Ysra in Belltower.

**What the player can recognize:** Six labeled Fever Reed gathering points.

Game map: `overworld`. Preferred world anchor: (245, 160). Layout: `reed_beds`. Seed-0 map marker: (218, 161). Quest bindings: ms_medicine_mireford

## Miredepth Cave

Identity: `miredepth`. Region: Belltower Fenlands.

**What it is:** A flooded cave associated with bells that call the drowned dead against living travelers.

**Who matters here:** Velmora is the hostile power called Bell-Drowned. Ysra knows the current threat, not a complete account of Velmora's life.

**How the player finds it:** Find Miredepth Cave east of Belltower. The red quest encounter and the cave lead to Velmora's threat; report her defeat to Ysra.

**What the player can recognize:** Miredepth's named cave entrance and Velmora's encounter marker.

Game map: `overworld`. Preferred world anchor: (255, 177). Layout: `cave`. Seed-0 map marker: (245, 185). Existing adventure entrance: `dungeon_miredepth_1`. Quest bindings: ms_miredepth_below

## Briarbridge Tollhouse

Identity: `briarbridge_tollhouse`. Region: Western river roads.

**What it is:** The bridge toll station recording grain shipments along the road toward Oathstead.

**Who matters here:** Mirella administers Riverside's supply arrangements; the written entries are the player's evidence.

**How the player finds it:** Find Briarbridge Tollhouse just south of Briarbridge. Read the three marked Toll Ledger entries, then report to Mirella in Riverside.

**What the player can recognize:** Tollhouse document table and three labeled ledger entries.

Game map: `overworld`. Preferred world anchor: (128, 128). Layout: `tollhouse`. Seed-0 map marker: (116, 130). Quest bindings: ms_toll_ledger

## Redcap Supply Camp

Identity: `redcap_supply_camp`. Region: Riverside Reach.

**What it is:** A raider camp intercepting the grain route between Riverside and the central settlements.

**Who matters here:** Mirella needs the eight raiders stopped; the follow-up identifies one scavenger for the Hunger recovery.

**How the player finds it:** Take Riverside's southeastern road to Redcap Supply Camp. Follow the marked raiders, then the scavenger carrying the stolen stone.

**What the player can recognize:** Stolen supply crates and red quest encounter markers.

Game map: `overworld`. Preferred world anchor: (93, 119). Layout: `goblin_camp`. Seed-0 map marker: (93, 119). Quest bindings: ms_redcap_trade, ms_glowing_mud

## Oakhaven Ward Orchard

Identity: `oakhaven_orchard`. Region: Central Hearthlands.

**What it is:** The orchard feeding Oakhaven, where protective magic passes through the trees and a stag once received the first fallen apple.

**Who matters here:** Elder Rowan tends the orchard. Rootmaw is the guardian now attacking its workers.

**How the player finds it:** Leave Oakhaven for the orchard southeast of the village. Find Rootmaw at the Oakhaven Ward Orchard encounter marker.

**What the player can recognize:** Old orchard ward stone and Rootmaw's encounter.

Game map: `overworld`. Preferred world anchor: (130, 157). Layout: `orchard`. Seed-0 map marker: (132, 157). Quest bindings: ms_orchard_ward

## Stonegate Crypt

Identity: `stonegate`. Region: Northern Crownlands.

**What it is:** A burial place whose damaged names may prevent the dead from being released from their duties.

**Who matters here:** Hollis maintains the burial records. The Nameless Warden attacks visitors.

**How the player finds it:** Find Stonegate Crypt northwest of Highwall. Stop the Nameless Warden at its marked encounter or in the crypt, then return to Gravekeeper Hollis in Archive City.

**What the player can recognize:** Named crypt entrance and the Warden's encounter marker.

Game map: `overworld`. Preferred world anchor: (196, 62). Layout: `crypt`. Seed-0 map marker: (196, 52). Existing adventure entrance: `dungeon_stonegate_1`. Quest bindings: ms_names_cold_stone

## Snowrest Winter Pass

Identity: `snowrest_pass`. Region: Northern roads.

**What it is:** Snowrest's winter supply approach, blocked by a mountain predator.

**Who matters here:** Captain Elric organizes the supply runs; the Hailback Broodmother is a local beast, not a proven servant of Vaelthara.

**How the player finds it:** Follow the road east from Snowrest to Snowrest Winter Pass. Stop the Hailback Broodmother at its red encounter marker.

**What the player can recognize:** Winter road marker and the Broodmother's encounter.

Game map: `overworld`. Preferred world anchor: (94, 67). Layout: `old_road_marker`. Seed-0 map marker: (107, 67). Quest bindings: ms_cold_road

## Glimmerfen Bell Foundations

Identity: `glimmerfen_bellworks`. Region: Belltower Fenlands.

**What it is:** The old supports for the warning line between Glimmerfen and neighboring landings.

**Who matters here:** Nessa is the bellwright planning the repair and recovering the Stone of Bells.

**How the player finds it:** Find Glimmerfen Bell Foundations southeast of Glimmerfen. Inspect both marked foundations, then report to Bellwright Nessa in the village.

**What the player can recognize:** Two labeled Old Bell Foundation inspection points.

Game map: `overworld`. Preferred world anchor: (272, 145). Layout: `ruined_watchpost`. Seed-0 map marker: (276, 142). Quest bindings: ms_missing_bell_rope

## Blackvault Ruins

Identity: `blackvault`. Region: Southern badlands.

**What it is:** The old repository for spent magical power from the region's protective enchantments.

**Who matters here:** Damar studies the maintenance books. Sareth blocks recovery of the Stone of Ash.

**How the player finds it:** Find Blackvault Ruins west of Redcairn. Defeat Sareth at the marked encounter or in the ruins, then return to Ash-Scribe Damar in Redcairn.

**What the player can recognize:** Blackvault's named entrance and Sareth's encounter marker.

Game map: `overworld`. Preferred world anchor: (194, 235). Layout: `abandoned_castle`. Seed-0 map marker: (194, 235). Existing adventure entrance: `dungeon_blackvault_1`. Quest bindings: ms_blackvault_mark

## Oathstead Camp

Identity: `oathstead`. Region: Central crossroads.

**What it is:** The refuge that shelters the player after the shrine attack, and the home the campaign asks them to protect.

**Who matters here:** Maelis organizes its residents. Mirella, Odrick, Selene, Ysra, and Solari are contacted in their respective cities.

**How the player finds it:** Return to Maelis in Oathstead. Use the camp defense point for Morvane's raid; carry the mainland representatives' answers back to Maelis afterward.

**What the player can recognize:** Camp hearth, Maelis, and the defense point.

Game map: `village_oathstead_camp`. Quest bindings: ms_camp_defending, ms_kingdoms_answer

## Old Gate of Alderfall

Identity: `old_gate`. Region: Ancient command route.

**What it is:** The twelve-stone portal through which the existing game starts the confrontation with Vaelthara at the Hollow Throne.

**Who matters here:** Vaelthara demands the stones and control over Oathstead's people. Maelis remains responsible for the camp.

**How the player finds it:** Accept Maelis's Gate quest after collecting all twelve stones. Follow the Old Gate of Alderfall marker and interact with the portal.

**What the player can recognize:** The existing twelve-stone portal marker.

Game map: `overworld`. Quest bindings: ms_twelve_stones_gate

## Referenced places beyond this catalog

The Lantern Isles, individual Briar Court halls such as Thorn Hall, the complete Hollow Throne exploration map, and future folklore-boss lairs are setting references or design targets. Do not give directions into them as if their proposed interiors and encounters already exist. The current Old Gate starts Vaelthara's battle directly. Later companion objectives still using numbered generic sites require individual migration to named places; this catalog does not silently relocate those stages.
