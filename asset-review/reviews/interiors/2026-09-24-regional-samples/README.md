# Regional interior samples

Status: rendered and visually inspected; design acceptance pending. These are proposals using the actual Java renderer, supplied architectural sheets, and the existing furnishing catalog. No settlement-wide rollout or save migration is included.

[Open the sample gallery](../../../../Java/tools/reviews/interiors/index.html).

| Sample | Place / role | Material | Layout / seed |
| --- | --- | --- | --- |
| [Oakhaven cottage](01-oakhaven-cottage.png) | Hearthlands ? Village | Rustic timber | `home`, 1 |
| [Snowrest winter refuge](02-snowrest-refuge.png) | Stormbound Holds ? Village | Rustic timber | `rescue_lodge`, 1 |
| [Riverside charter hall](03-riverside-charter-hall.png) | River and Thorn ? City | Timber and plaster | `ferry_lodge`, 2 |
| [Archive reading room](04-archive-reading-room.png) | Crownlands ? City | Paneled walls and parquet | `study`, 0 |
| [Highwall castle records hall](05-highwall-castle-hall.png) | Stormbound Holds ? Castle proposal | Stone masonry | `remembrance_hall`, 0 |
| [Sanctum water hall](06-sanctum-water-hall.png) | Sunrealm ? Landmark proposal | Stone masonry | `cistern_house`, 2 |
| [Mireford bellkeeper?s workshop](07-mireford-bellkeeper.png) | Belltower Fenlands ? Village | Rustic timber | `bellhouse`, 1 |

### Oakhaven cottage

A hearth, kitchen stores and a household table form the shared room; a partition shelters the bed. The seed bowl and herb storage follow the returning-seed and household-protection customs.

### Snowrest winter refuge

Dark timber, shared meals, linen stores and two sleeping bays support winter hospitality. The records desk gives the northern duty to account for people under one?s protection a practical place.

### Riverside charter hall

Charter records, a repair bench, waiting benches and a guest table distinguish civic work from hospitality. The side store and open meeting floor reflect bridge and ferry agreements, with ivy and teal textiles.

### Archive reading room

Connected book stacks, grouped reading desks and a screened consultation area make the city institution legible. The paneled sheet supplies its formal surfaces and curtained window.

### Highwall castle records hall

Masonry and stone columns give the hall civic weight. Separate record storage, hearing benches and a quiet memorial room follow the northern emphasis on witnessed deeds and named remembrance. The banner is a provisional heraldic choice.

### Sanctum water hall

Covered water stores, an allocation desk, a first-cup hospitality counter and a maintenance bench put water rights and guest law into the layout. Warm masonry contrasts with the cool northern stone sample.

### Mireford bellkeeper?s workshop

A warning bell, repair benches, supplies and a records desk support a marsh community?s ordinary work. Shaded timber and herbs give it a damp, inhabited character without treating the Fenlands as the Lantern Isles.

## Design rules and sources

Building use chooses the room groups. Building stature chooses the structure: timber for ordinary village rooms, timber/plaster or paneling for city institutions, masonry for castles and durable civic landmarks. Region then shapes furnishings, textiles, tone and everyday practices. A village region is not automatically assigned its capital?s luxury materials.

The references are the project?s [world framework](../../../../Story%20premise/world-framework.md), especially sections 5?8, [Hearthlands customs](../../../../Java/docs/hearthlands-worldbuilding.md), [western hospitality and charters](../../../../Java/docs/western-reach-folklore.md), and [current interior styles](../../../../Java/docs/interior-rendering.md). These visual proposals do not add new folklore claims or quests. Highwall and Sanctum labels are building design proposals, not newly placed canonical landmarks.

The [preserved sheets and crop recipe](../../../../art-source/interiors/2026-09-24-modular-materials/README.md) now also supply a wooden face, worn plank floor and stone support column. Rustic surfaces reuse the timber caps, posts and baseboards. The Highwall sample adds two stone columns and substitutes a banner for the domestic herb rack; all other furnishing groups come from the named regional layout. Placement is checked through the same furniture API used by generation; the exporter rejects omitted props.

## Reproduce

From `Java/`:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build.ps1 -OutputDirectory temp/regional-interior-samples/classes -IncludeTests -IncludeReviews
java '-Djava.awt.headless=true' -cp temp/regional-interior-samples/classes com.alderfall.game.InteriorDesignPreview temp/regional-interior-samples/captures regional
```

The seven named captures are retained here. Screenshots use noon lighting, full room framing and hidden HUD. Material overrides are confined to sample map identities; existing generated building assignments remain unchanged. The eastern sheet is retained for a future Lantern Isles proposal, rather than applied to the Fenlands.

Validation: full build, ConnectedInteriorWallsTest, InteriorFurnishingsTest (75 catalog assets), and the two architecture importer tests passed. All seven samples placed their expected furniture. Repository policy tests passed; full hygiene remains blocked by the same five existing root files (.vscode settings and the four uploaded originals). Review links and structure checks are run separately.
