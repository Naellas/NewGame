# Title screen refresh

Eight dialogue sprites replace the small world models: Rowan, Cleric, Knight,
Maelis, Mage, Selene, Ranger and Elric. Leaf-and-gold frames decorate the scene,
menu panel and buttons. Foreground fern silhouettes and animated fireflies add depth.
World movement sprites are unchanged. Six replacements are shared by menu and dialogue.

## Assets

Generated using the built-in imagegen tool, with existing sprites as identity references.
Native RGBA transparency is preserved; no background removal or raster editing scripts.

- `Java/assets/characters/npcs/story/npcs/maelis/npc_story_maelis_dialogue_sprite.png`
- `Java/assets/characters/npcs/story/npcs/elder_rowan/npc_story_elder_rowan_dialogue_sprite.png`
- `Java/assets/characters/npcs/story/npcs/selene/npc_story_selene_dialogue_sprite.png`
- `Java/assets/characters/npcs/story/npcs/captain_elric_snowrest/npc_story_captain_elric_snowrest_dialogue_sprite.png`
- `Java/assets/characters/player/classes/mage/class_mage_dialogue_sprite.png`
- `Java/assets/characters/player/classes/ranger/class_ranger_dialogue_sprite.png`

## Generation prompts

Maelis (references: original Maelis and original Mage):

> Use case: style-transfer. Create replacement game dialogue sprite for MAELIS (first reference is edit target; second reference mage is supporting crisp linework reference only). Preserve Maelis identity, brown hair bun, adult woman face, dark teal leather tunic, olive leaf cloak with round clasp, rust scarf, trousers and boots, hand on hip and full body standing pose. Redraw with sharp clean illustrated fantasy RPG linework, moderate detail, clear restrained cel shaded planes. Midpoint between blurry textured first reference and overly intricate second reference: remove noise, no micro filigree, no blur, distinct readable face, coherent anatomy and hands. Full length isolated single character including boots, generous transparent margins, genuinely transparent alpha background. No text, no floor or shadow. Save output image file and return its path for integration into local game project.

Maelis final pass (reference: generated Maelis):

> Use case: background-extraction. Keep this Maelis character artwork exactly unchanged. Remove ALL background: black and brown and olive glow, every background pixel. Output isolated full body game sprite on genuinely transparent RGBA alpha background, including transparency between legs and around cloak. No shadow no glow no backdrop. This will be composited in a game; a black background is not transparency.

Mage, Rowan, Selene and Elric used this prompt template, each with their original sprite:

> Use case: style-transfer. Redraw reference as replacement fantasy game dialogue full body sprite. Preserve identity costume colors props and pose: {description}. Crisp clean ink outlines with restrained cel-shaded painted planes, moderate detail, clear facial features and materials, no blurry texture no noisy tiny filigree. A unified illustrated RPG style between simple pixel sprite and hyper-detailed anime illustration. Full body including shoes, centered with margins. CRITICAL genuinely transparent RGBA alpha background, NO black background, NO colored backdrop, NO glow, NO ground or shadow, no text. Single isolated character cutout ready for game compositing.

Descriptions substituted verbatim:

- Mage: white-haired young adult male mage, blue violet robes, blue crystal staff, gold edging. Reduce dense gold microembroidery to a few larger decorative motifs
- Rowan: elder Rowan, elderly man with long grey beard, olive green druid cloak, simple gold leaf clasp, wooden staff
- Selene: Selene, adult dark-haired woman with bun, glasses and forehead mark, charcoal scholarly robe with restrained gold borders, large closed book
- Elric: captain Elric, adult dark-haired bearded man, silver armor, blue cloak with pale fur collar, sword pointed down

Ranger (references: original Ranger, refreshed Mage):

> Use case: style-transfer. First reference is edit target: ranger full-body dialogue game sprite. Second image is style reference only. Redraw ranger with crisp illustrated fantasy RPG linework and restrained cel shaded planes, moderate detail matching mage reference. Preserve young adult male identity, brown hair, green hood and cloak, brown leather armor, bow held across legs and quiver on back, boots. No pixelation, no blur, no excessive microembroidery. Full standing body with all boots and bow visible, isolated single character, genuinely transparent RGBA background. No glow, no backdrop, no floor, no text.

## Review

`Java/tools/TitleMenuReview.java` renders the actual GamePanel at 1920x1080,
1440x900 and 1280x720. Compile it with the game sources and run
`com.alderfall.game.TitleMenuReview Java` from the repository root.
Exports live beside this file. Full source compilation and SmokeTest passed.
