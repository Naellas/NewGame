# Supplied settlement sheets: review and integration

Source: `C:/Users/Maciej/Downloads/town_assets_directional_swamp_eastern_port.zip`.
The four original PNGs are preserved in `assets/source/settlement-refresh/` at the repository root.
The ZIP contains street furniture, swamp, eastern, and port sheets. It does not contain the
temperate, snowy, or desert sheets shown earlier in chat. Each supplied image is 1448x1086 RGB
with an opaque charcoal background, not an alpha sprite atlas.

## Selection

| Sheet | Assessment | Runtime decision |
| --- | --- | --- |
| Directional town props | Strong replacement for repeated produce crates and oversized planters; useful lamp orientations and real canopy stalls | 21 selected cutouts imported and used alongside existing props |
| Swamp | Strong coherent set for Fen settlements; raised floors, stairs and stilts make existing rectangular foundations a poor automatic match | Original retained; buildings not mapped into existing lots |
| Eastern | Internally consistent architecture and matching furniture, but no corresponding established regional profile | Original retained as a complete regional candidate |
| Port | Boatwright, netshed, fish shop and dock props offer more specific functions than generic houses | Original retained; no automatic replacement of named buildings or functional docks |

Large sheet buildings are not interchangeable with individual modular house sprites. Their doors
are small relative to the complete building; fitting the full image into current bounds can shrink
the doorway below character scale. Stilt buildings additionally need raised entrance anchors and
authored footprints. These are visual suitability findings, not claims of in-game validation for
the unimported buildings.

## Imported assets and sizing

Runtime files: `Java/assets/environments/settlements/city/props/refresh/city_prop_refresh_*.png`.
`crops.tsv` records source crop bounds and maximum dimensions at base tile size.
`SettlementSpriteScale` supplies the same dimensions to the renderer without the generic prop
size boost. Images preserve aspect ratio and use tight alpha bounds with two pixels of padding.

- Crates, sacks and baskets: 26–28 px.
- Barrel, pottery and small planters: 30–34 px.
- Bench and tall planter: 42–44 px; cart: 48 px.
- Market stalls: 78 px; street lamps: 90 px.

Stalls and lamps were increased after comparison with characters in actual rendered scenes.
Lamps face an adjacent road to their left or right; north/south shoulders use a vertical iron lamp.
Existing lamp spacing and entrance exclusion rules remain active. Player-built settlements are
excluded from the dressing pass. Building art and collision geometry were not changed by this import.
The new decorative furniture follows the existing settlement prop collision behavior.

## Cutout provenance and rebuild

`street-alpha.png` was produced with the built-in imagegen tool, using the original street sheet as
an edit target. Original files were not overwritten. The exact prompt was:

> Background extraction edit of the supplied sprite sheet. Remove ONLY the charcoal background and background shadows, replacing them with genuine alpha transparency. Preserve every sprite, all details, colors, dimensions, placement, orientation and spacing exactly. Do not add or redesign objects. Keep entire original sheet layout and canvas aspect ratio. This is a game asset extraction, not a reinterpretation. Save transparent PNG.

`ImportSettlementRefresh.java` slices this alpha sheet using the reviewed crop table and the existing
`ImportCleanFlora.trim` helper. It does not color-key the artwork. Three crop rectangles were tightened
after detecting neighboring sprite fragments. `scale-preview.png` shows all selected sprites on green.

Run from `Java/`:

```
javac -d temp/import-refresh tools/assets/environments/ImportCleanFlora.java tools/assets/environments/ImportSettlementRefresh.java
java -cp temp/import-refresh ImportSettlementRefresh
```

## Validation

- Full Java source compilation.
- `SettlementSpriteScaleTest`: 21 assets indexed, alpha checks, cargo/canopy/lamp scale limits,
  proportional sizing at 36/48/72 px tiles, road-facing lamps in four settlements.
- `SettlementCompositionTest`: lamp spacing, clear doors, asset availability, preserved player
  settlement, upper building wall collision and recovery from an old position inside a building.
- `BuildingCollisionTest`: 77 buildings, swept movement, door access and roof/side clearance.
- Actual game render captures for Sanctum, Embermarket, Highwall and Briarbridge:
  `../settlement-composition/*-refresh.png`. Sanctum and Briarbridge were visually inspected.

The source archive contains no instructions or executable files.
