# Hearthlands: outward from Oathstead

This pass implements environmental storytelling from `Story premise/world-framework.md` (central hearth customs, western hospitality, wards and Oathstead) and `Story premise/faith-cults-and-dungeons.md` (seedhouses, ordinary religious life and inhabited camps).

## Playable additions

- Oakhaven's west cottage becomes the **House of Returning Seed**; Elderford's north cottage becomes the **Elderford Seed Exchange**. Their original lot keys and coordinates are preserved. Both have building collision, usable doors, named interiors, a seed keeper and an apprentice. They reuse the working bakery interior layout for the shared oven and stores.
- Sheltered communal ovens and small household wards appear in **Briarbridge** and **Moonspire**. Elderford gains a maintained household ward. More wards mark actual road approaches toward Elderford, Briarbridge and Moonspire.
- Local paired conversations connect practical customs to food, shelter, seed loans, the seedhouse roof dispute, contested maps and western gifts. Both lines are authored together. Ordinary job conversations still appear; main quest dialogue is not replaced.
- Nearby observations describe the new props. Pressing **E** beside them repeats the observation when no NPC, doorway or quest interaction takes priority. Existing Oathstead and Redcap provisions also receive observations. These are descriptions, not new resource or quest rewards.

## Art and placement

New built-in imagegen assets live in `assets/environments/settlements/city/folklore/`: `folklore_shared_oven.png`, `folklore_seedhouse.png`, and `folklore_hearth_ward.png`. Full prompts and the selected outputs' intended use are recorded in `hearthlands-prompts.md` there. Oakhaven uses the existing `folklore_returning_seedhouse.png` for its new building lot.

The images retain generated alpha and use existing aspect-preserving sprite scaling, depth order, shadows and lighting. Muted terracotta, cream plaster, dark timber and mossy stone connect them to neighbouring buildings. Haloed wayshrine variants were rejected.

Deterministic placement preserves roads, building footprints, entrances, NPC anchors and substantial scenery. Only small grass, leaf, flower and pebble decorations may be cleared for an object's footprint. Sparse decoration extends the existing settlements rather than adding new map transitions or enemy factions.

## Verification

From `Java/`, after compiling sources to `out-hearthlands-check`:

```powershell
java '-Djava.awt.headless=true' -cp out-hearthlands-check com.alderfall.game.HearthlandsWorldTest --render
java '-Djava.awt.headless=true' -cp out-hearthlands-check com.alderfall.game.FolkloreWorldTest
```

The extension check covers three world seeds (0, 42, 1024), asset lookup and alpha, placement/indexing, routes to props, seedhouse entry and exit, named interiors and resident keepers. Renderer captures are written to `asset-review/hearthlands/` at repository root.

The full `SmokeTest` currently stops at `Road sign objective count did not match remaining progress`. A separate compiled copy with the folklore population calls disabled reproduces that assertion; removing these decorations does not resolve it.

## Boundaries

The roof/garden dispute and Hearthkin story appear as local testimony, not a newly playable branching quest. Seed trading, a maintenance stair, underground root beds, an interactive ward-repair rite and the full Still Harvest story are not implemented here. Existing saves retain lot-based interior identities; an already materialized interior can keep its previously saved customization. No player saves are rewritten by this pass.
