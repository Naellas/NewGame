# Orthogonal regional city-wall kit

Generated with the built-in Codex image-generation tool on 2026-09-22. The runtime uses the six cropped
modules; the atlases are retained as editable source sheets.

## Master prompt

Create a production-ready transparent PNG sprite atlas for a fixed-camera 2D medieval fantasy town wall
system. The canvas is exactly three equal columns by two equal rows. This is not isometric: horizontal
walls run perfectly screen-left to screen-right, vertical walls run perfectly screen-top to screen-bottom,
and no module uses a diagonal or receding perspective. Keep each isolated object inside its cell.

The six modules are: horizontal wall; vertical wall; square four-socket corner/junction tower; horizontal
gatehouse with a visibly open walkable arch; vertical gate aligned top-to-bottom with an open passage; and
a compact four-socket end/watch tower. Every module uses identical gray fieldstone, masonry scale, wall
thickness, battlement height, and centered socket width. Sockets are square-ended and towers hide joins.
Use detailed hand-painted pixel art for a classic 32–48 px fantasy RPG overworld. Transparent background,
clean alpha, no labels, ground tiles, people, UI, diagonal geometry, or full scene.

## Regional material edits

Each edit preserved the master canvas, six-cell layout, scale, silhouettes, connector positions, wall
thickness, gate openings, and strict screen-axis geometry.

- `hearth`: gray fieldstone, slate caps, restrained weathering and tiny ivy accents.
- `north`: dark cold granite and slate, pale mortar, settled snow, frost crusts and restrained icicles.
- `fen`: damp charcoal fieldstone, moss in lower courses and seams, sparse ivy and wet highlights.
- `sun`: honey/ochre sandstone, sun-bleached edges, reddish timber accents and subtle sand at tower bases.
- `freeholds`: salt-weathered blue-gray/brown stone, tarred timber, mineral streaks, sea-green lichen and
  rust-dark metal details.

The sandstone horizontal gate received a final edit removing its wooden door and restoring a fully open,
walkable arch. All edits explicitly prohibited rotation, diagonalization, opaque backgrounds, blocked
passages, and connector changes.

The runtime `horizontal_seamless` and `vertical_seamless` assets are mechanically cropped center courses
from the generated straight modules. They omit the terminal socket blocks so multi-tile repeats overlap
cleanly; the original socketed modules remain in the kit for editing and junction work.

The runtime `gate_horizontal_clean` and `gate_vertical_clean` assets are expanded atlas crops that retain
the complete gatehouse while excluding disconnected artwork from the neighboring atlas cell.

## Atlas order

| Cell | Module |
|---|---|
| top-left | horizontal |
| top-center | vertical |
| top-right | tower |
| bottom-left | gate_horizontal |
| bottom-center | gate_vertical |
| bottom-right | end_tower |
