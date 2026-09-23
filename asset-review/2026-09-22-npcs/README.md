# NPC sprite review — 2026-09-22

Refreshed six older NPC sets and corrected mislabeled directions in seven others. The review covers the 21 legacy NPC sets in `Java/assets/characters/npcs/townsfolk`; the newer companion artwork supplied a style reference. No unused assets were removed or relocated.

## Updated artwork

- Bartender: cleaner face, clothing and silhouette; complete head in rear views; removed detached fragments.
- Blacksmith: complete rear head, clearer leather/tool shapes, consistent directional views and matching hammering animation.
- Male citizen: cleaner face, clothing and silhouette with correctly oriented side views.
- Female citizen: clearer dress, apron and shawl, opaque clothing and consistent side views.
- Merchant: consistent backpack, feathered cap, lantern and clothing; matching ledger-inspection animation with the lantern retained in the same hand.
- Orin: restored full head, removed stray fragments, clearer armor and green cape, consistent hammer placement.

Each set includes its square portrait, base world model, four directional sprites and four eight-frame walking strips. The blacksmith and merchant retain six-frame work animations. Orin's front walk reuses the same clean passing pose in both halves of the cycle to keep the hammer in the correct hand. The two work strips now use 96×128 cells, matching the walking assets; their six-frame counts are unchanged.

Elowen, Garruk, Kael, Ren, Torin, Vexa and the quartermaster had reversed left/right artwork. Their standing sprites and walking strips were reassigned together using byte-exact original images.

In total, 90 PNG files changed: 62 regenerated assets and 28 direction corrections. Two frame metadata files were rewritten with the same numeric counts. The other 208 files in the initial 300-file NPC snapshot remain unchanged. Runtime Java source was not changed for this pass.

## Previews

- [Before/after and actual gameplay sizes](npc-before-after.png)
- [Corrected directions](corrected-directions.png)
- [Work animations](work-animations.png)
- [Light-background cutout check](light-background-check.png)
- Actual game captures: [merchant](scenes/merchant.png), [merchant dialogue](scenes/merchant-dialogue.png), [blacksmith](scenes/blacksmith.png), [Orin](scenes/orin.png)

## Generation and preservation

Artwork was generated with the built-in imagegen tool using existing identities and the newer Calder sprite as a style reference. The [accepted prompt set and source locations](generation.json) and [initial draft prompts](draft-generation.json) are retained. The importer only extracts, fits and packages generated art; it does not recolor or apply background-key removal. It finds transparent gutters and uses the renderer's alpha threshold for bounding boxes, avoiding clipped extremities and unstable placement caused by faint transparent pixels.

Live assets are in `Java/assets/characters/npcs/townsfolk/` and its `animations/` subfolder. Accepted generated masters are in `Java/assets/source/npc-refresh-2026-09-22/`. These paths are relative to the project root. A byte-exact snapshot of the original NPC folder is preserved under `before/Java/assets/characters/npcs/townsfolk/` here. No project asset depends on a file outside the workspace.

## Validation

Full Java source and review diagnostics compiled with JDK 21. Runtime checks passed for all 39 unique NPC references in `GameData.NPCS`, all 126 static NPC assets and all 87 animation strips. The diagnostics rendered 2,070 animation frames across three display sizes. Town and dialogue screenshots were captured through GamePanel.

Hash verification passed for all 300 archived originals, all installed replacements and all untouched files. All regenerated frames have clear transparent margins, portrait/model dimensions remain compatible, and every animation retains its original frame count. See [integrity results](verification.json) and [runtime results](runtime-verification.txt). This is targeted rendering and asset-integrity validation, not a complete gameplay regression suite.
