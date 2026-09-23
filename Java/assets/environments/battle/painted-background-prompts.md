# Painted background style pilot — 2026-09-23

Built-in image_gen edit using the forest-edge background and Seraphine/goblin archer sprites as references. Saved asset: `battle_forest_edge_backdrop_painted.png`.

BattleRenderer prefers a `_painted` sibling when available; otherwise it uses the original background. Only forest-edge has been converted so far. This corrects the mismatch between coarse environmental pixel clusters and finely illustrated actor sprites. The original remains available.

## Exact prompt

Use case: style-transfer. Repaint Image 1 forest-edge battle environment at substantially higher visual fidelity to match the finely illustrated fantasy sprites in Images 2 and 3. Images 2/3 are STYLE REFERENCES ONLY: do not include characters or monsters. Image 1 composition reference: woodland to left, distant hills and sky, broad open grassy-earth battle clearing bottom 60%, horizon around 38%. Retain scene identity and combat staging; omit distant castle. Render a polished hand-painted 2D RPG environment with fine crisp natural contour work, nuanced bark, individual grass blades and small stones, smooth shaded volumes, refined foliage shapes. Dense small-scale detail like a high-resolution illustrated fantasy game, not coarse pixel art. NO oversized square pixels, mosaic patterns, dithering, chunky 16-bit clusters, blur filters, photorealism or 3D rendering. Restrained earth greens, less neon yellow, atmospheric distant hills. Keep the ground visually calm enough for sprites and all tall objects behind stage/edges. Output landscape 3:2 ideally 2304x1536 or higher, true finely drawn detail, no text, UI, people or creatures.
