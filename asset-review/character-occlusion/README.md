# Character occlusion

Buildings, settlement sprites, mountains, and raised world props now share ground-contact depth ordering with characters. Foreground scenery becomes transparent in a circle around characters behind it: strongest near the body, smoothly returning to full opacity at the perimeter. Ground cover stays below characters. Quest and hover labels remain above the scene.

- `preview.png`: building, tree, and mountain examples, with a character behind (top) and in front (bottom).
- `town.png`: actual GamePanel capture at 125% zoom, with the player behind a warehouse.

The cutaway uses a reusable alpha surface only for scenery near a hidden character. Its radius scales with the character and tile size; transparency eases near the scenery's ground line. This is a spatial falloff, without a separate timed fade.

Validation: full Java compilation, SmokeTest, and WorldDepthRenderTest passed. Pixel checks cover foreground/background ordering, feathering, transparent source pixels, frame reset, camera translation, clipping, zoom, multiple characters, and labels.

Regenerate the comparison after building, from `Java/`:

```powershell
java '-Djava.awt.headless=true' -cp out com.alderfall.game.WorldDepthRenderTest ../asset-review/character-occlusion/preview.png
```
