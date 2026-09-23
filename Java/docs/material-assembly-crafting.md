# Material assembly and craftsmanship

Open **Craft → Equipment Workshop**. Choose an equipment type, prepare components at their listed stations, then switch to **Stage 2: Assemble**. Select components from your pack and review the exact stats before assembling. Optional slots default to empty. Prepared components are reusable across compatible blueprints and persist in inventory until consumed.

Each slot opens a paginated sprite grid filtered to compatible materials or owned components. Hover a tile for its name, tier, rarity, stats, skill requirements, predicted quality, and exact quantities. Each tile shows `owned / required`. Unowned sprites are grayscale; any owned material retains its original color, with amber counts for insufficient quantities and green counts when sufficient. Unowned choices remain selectable for planning. Sorting buttons select Tier, Availability, Level, or Rarity; clicking the active button reverses direction. Availability defaults to enough material first, then partial stacks, then missing materials. Optional slots include an empty tile. Sorting resets pagination and preserves the selected material's identity.

Supported blueprints: sword/dagger (blade + hilt), axe (head + shaft), spear (blade + shaft), staff (shaft + gem), bow (shaft + bowstring), mail (plates + lining), robe (fabric + lining), leather armor (leather + lining), and ring (setting + gem). Also available: helmet, pauldrons and leggings (plates + lining), gloves and boots (leather + lining), belt (leather + setting), necklace (setting + gem), and shield (plates + hilt). These 18 types cover all 11 `GameData.EQUIPMENT_SLOTS`. All support one ornament and one rune. The Type button opens a category selection grid.

Metal components require refined ingots; existing smelting recipes remain available in the Recipe Book. Carpenter tables support woodwork, tailoring, and leatherwork; anvils support smithing, armorcraft, and jewellery; alchemy stations prepare runes. Every stage is a timed task and awards its specialist profession XP. Existing recipe unlocks and fixed-output recipes remain compatible.

Materials have five tiers, rarity, level, and explicit attack, defense, health, mana, spell damage, critical chance, and healing contributions. Tier skill requirements are 1/2/4/6/8; equipment level requirements are 1/5/9/13/17. Higher-tier material stats and affinities are authored in `MaterialCatalog`. Story socket stones are not expendable assembly ingredients.

Monster drops can change final stats: horn and venom sacs improve critical chance, scales reinforce defense and health, ember shards improve spell damage, and frost shards add defense, mana, and spell damage. These are stat contributions, not new elemental or poison proc effects.

The panel includes a persistent category/material-aware item image and predicted stats. Stage 1 previews the selected material plan, including explicitly chosen optional materials and shows the calculation for the selected slot (click its heading). Stage 2 previews the exact assembled result and shows the assembly calculation, next quality threshold, and any component-quality limit.

Quality uses the same calculation in the recipe generator and the UI:

```text
profession contribution = 10 * (profession level + practice/training bonuses)
attribute contribution  = min(30, 0.6 * primary attribute + 0.4 * secondary attribute)
material difficulty     = 4 * (material tier - 1)
quality score           = max(0, profession contribution + attribute contribution - material difficulty)
```

| Required job | Primary (60%) | Secondary (40%) |
| --- | --- | --- |
| Smithing / Armorcraft | Strength | Constitution |
| Carpentry | Dexterity | Strength |
| Tailoring / Jewellery | Dexterity | Intelligence |
| Leatherworking | Dexterity | Constitution |
| Alchemy | Intelligence | Willpower |

Attributes use current character stats, including equipped bonuses. For a component, difficulty uses that material's tier; assembly uses the highest selected material tier, including optional parts. Attribute bonuses never bypass minimum profession requirements.

| Quality | Score | Stat multiplier |
| --- | ---: | ---: |
| Common | Below 30 | 100% |
| Uncommon | 30-49.9 | 115% |
| Masterwork | 50-69.9 | 135% |
| Rare | 70-99.9 | 155% |
| Legendary | 100+ | 185% |

Example: Smithing 4, no training bonus, STR 20 and CON 10 yields `40 + min(30, 12 + 4) - 0 = 56`: Masterwork for tier-1 material. Tier-3 material subtracts 8, yielding 48: Uncommon. One effective training level adds 10 score.

Quality is deterministic, with no hidden success roll. Final quality is the lower of calculated assembly quality and the lowest-quality required component. Optional workmanship strengthens that component's stat contribution without directly capping structural quality; its material tier can still increase assembly difficulty. Material rarity sets a floor for final item rarity, separate from craftsmanship. Critical chance from one assembled item is capped at 20%.

New resources are registered end-to-end: obsidian shards are honed into obsidian glass for blades, heads, or ornaments; raw amber is polished for gems, ornaments, or runes; cypress works in wood slots; rock salt is refined for cooking or healing/warding runes. Raw and refined forms each have tier, rarity, and stat profiles. Existing refinement and gathering paths remain in use; raw amber and rock salt cannot substitute for their refined assembly ingredients.

Smithing, tailoring, carpentry, armorcraft, alchemy, and jewellery have separate XP and training/mastery trees; leatherworking uses its existing tree. Training improves effective skill; mastery also raises the profession level cap. All professions appear in the character screen.

Versioned `part1~` and `gear1~` item keys encode the blueprint, material choices, and workmanship. Existing inventory/equipment save serialization preserves these without a new save format. Final stats resolve from the catalog, so future balance changes will also affect saved assembled gear. Output is fixed at task start, preventing skill gains during the timer from changing the advertised result. Assembly never receives legacy batch-output bonuses.

Validation: `AssemblyCraftingTest` covers material comparisons, workmanship, optional drops, invalid slots, resource accounting, XP, all blueprints, malformed keys, and existing save serialization. `AssemblyWorkshopTest` covers navigation, rendering, workstation enforcement, and assembling through the game UI. It can write previews using `java -cp temp/checks/classes com.alderfall.game.AssemblyWorkshopTest out/workshop`.

`CraftingCalculationTest` checks exact job/attribute arithmetic, difficulty, attribute caps, profession gates, refinement paths, and reverse coverage of the crafting material registry. `ItemAppearanceTest` verifies category/material/quality artwork.

The right-hand preview uses the same appearance descriptor as the inventory item. Primary material colors the body, secondary material colors fittings/lining or the inset gem, and ornaments color gold-coded accents. The additional eight categories use `assets/items/crafting_slots_atlas.png`; atlas cells have neutral body, red secondary, and gold ornament channels. Paired gloves/boots/legs retain both disconnected silhouettes. `CraftingAppearanceTest` verifies independent body and secondary color changes for every added category.

Review images: `../../asset-review/crafting/workshop-assemble.png`, `../../asset-review/crafting/workshop-types.png`, and `../../asset-review/crafting/material-colors.png`. The built-in imagegen asset and exact prompt are saved in [crafting_slots_atlas.png](../assets/items/crafting_slots_atlas.png) and [crafting-slots-prompt.md](../assets/items/crafting-slots-prompt.md).

## Creative Crafting (testing)

Settings -> Creative Crafting is off by default and persists in `config/settings.properties`. When enabled, component preparation, equipment assembly, and recipe-book crafting are instant and consume no resources. Workstation, profession, and recipe-unlock gates are bypassed; the real attribute/profession/material calculation still determines quality. Crafts award no character or profession XP, and the workshop stays open. All recipe-book recipes are visible. Toggle off to restore normal requirements. Material quantities and grayscale states always reflect real inventory, even in creative mode.

`CreativeCraftingTest` covers settings persistence, free instant outputs, profession/station bypasses, zero XP, legacy recipes, and restoring normal mode. `AssemblyWorkshopTest` also checks sprite colors, hover details, sort directions, stable selection and creative-mode button availability.
