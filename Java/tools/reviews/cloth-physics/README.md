# Cloth and hair physics study

Open `../characters/index.html#cloth-physics` or this folder's `index.html`.
The third tab compares current game frames with experimental cape/robe/hair
motion for Mage, Seraphine and Knight. Toggle physics, inspect colliders and
nodes, pause or scrub. Files work locally without a server.

This is a review-only sprite-layer experiment. Seven-node horizontal spring
chains use damping, a small periodic force and a pinned root. Cape/robe outer
edges collide with capsule shapes along both thighs and shins. Source sprite
rows follow the interpolated displacement; the skeleton and held equipment
keep their own transforms. Seraphine's ponytail has a separate chain. Short
hair stays attached. The simulation is warmed for 24 cycles and baked into
32-frame strips, so frame scrubbing is deterministic.

This is not a full cloth mesh: collisions are checked at the coarse edge nodes,
not every fabric pixel. No self-collision, 3D wrapping, stretching, turn response
or world geometry is simulated yet. Physics remains disabled in the game.

The separate sideways stride correction is active in the game. Previously a
saved 0.5x cadence doubled the planted stride and forced a low, overextended
pose. Grounded cadence now has a direction-dependent reach floor: 0.9x sideways,
0.5x diagonally/vertically. The clock and renderer share this calculation;
travel speed, saved preferences and 1x default are unchanged. Previous movement
modes keep their original cadence behavior. Before/after Mage frames are in
`side-stride-before.png` and `side-stride-after.png`.

Compile the Java files in `source/` together with the game sources to a separate
output directory, then run `com.alderfall.game.ExportClothStudy` from `Java/`.
Export checks validate finite nodes, pinned roots, loop continuity and no
cloth-edge-node penetration of leg capsules across all 288 simulated poses.
The browser smoke check lives at `../grounded-movement/source/check_preview.py`.
