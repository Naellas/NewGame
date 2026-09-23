# Dungeon floor replacement prompts

Generation mode: `imagegen`, new raster assets guided by existing safe 48 px dungeon terrain.

All four prompts used this shared specification:

> Use case: stylized-concept. Asset type: seamless game-ready square terrain tile for a 2D
> orthogonal fantasy RPG dungeon. Detailed hand-painted pixel art matching the reference dungeon
> terrain, crisp deliberate pixel clusters, subdued dark-fantasy palette. Perfectly top-down
> orthographic square texture, full bleed, tileable on every edge, no perspective or horizon.
> Entirely opaque; no transparent pixels, white, checkerboard, border, frame, bevel, margin, empty
> strip, text, characters, props, walls, or watermark. Uniform lighting with no directional shadow;
> readable at 48x48 pixels.

Asset-specific additions:

- `dungeon_crypt_floor_source.png`: fitted ancient charcoal-grey crypt flagstones with subtle
  irregular joints, faint age wear, and sparse muted bone-beige mineral scratches. The retired
  reference supplied the subject only; avoid its bright repeated dashes, strong center, symbols,
  runes, arrows, or obvious grid repetition.
- `dungeon_moss_floor_source.png`: fitted worn charcoal-grey crypt flagstones, irregular subtle
  joints, restrained dark-green moss and damp staining crossing edges. Avoid bright green, puddles,
  grass, a focal symbol, or a repeating center.
- `dungeon_rubble_floor_source.png`: worn dark-grey flagstone partly broken into chips and shallow
  cracks, with rubble integrated into a walkable surface. Avoid large blockers, bright outlines, a
  focal object, or an identical crack grid.
- `dungeon_torch_floor_source.png`: dark worn slabs with a restrained diffuse amber firelight stain
  and subtle soot. Avoid a visible light prop, circular spotlight, hard boundary, or repeated bright
  center.
- `dungeon_boss_sigil_floor_source.png`: a restrained ancient geometric ward carved into continuous
  charcoal flagstone, centered as a single special tile. Avoid glow, letters, religious symbols,
  pentagrams, frames, platforms, transparent edges, or cropped ornament.

`tools/assets/environments/import_dungeon_floor_replacements.ps1` center-crops each generated master, resamples it to the
native 48 px terrain size, reconciles opposite edge bands, verifies opacity, and writes the runtime
asset. Generated masters are retained here so the import remains reproducible.
