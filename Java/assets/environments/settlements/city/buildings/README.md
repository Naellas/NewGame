# Building sprites by region

sun = Sunrealm; river = Western Reach; north = Stormbound Holds;
freehold = Northroad Freeholds; fen = Fenlands; hearth = Hearthlands.

shared contains generic city facades, player-village upgrade buildings, and
buildings used across regions. It prevents copying one asset into several regions.
Regional houses and institutions are consolidated here alongside city and village
buildings. Walls, street props, landmarks, and overworld settlement markers keep
their own distinct folders. Filenames and game IDs are unchanged.

Use config/asset-placements.json and the shared asset_file helper when maintaining
older import recipes. Do not recreate buildings/city, buildings/village or regional.
