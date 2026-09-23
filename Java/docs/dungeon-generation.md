# Contextual dungeon generation

Dungeon generation is deterministic for a world seed. It is organized around one site profile and
one floor plan per storey rather than independent terrain, prop, and monster rolls.

## Context pipeline

`WorldMap` resolves the entrance's nearby biome and owning kingdom before creating the interior.
`DungeonGenerator.SiteProfile` combines:

- site ID and name;
- exterior type (`cave`, `crypt`, `prison`, `sewer`, occupied camp, or ruined keep);
- nearby surface biome;
- kingdom/region;
- a site-specific or regional folklore tag;
- vertical direction; keeps and bandit towers ascend, while caves, crypts, prisons, sewers, and
  warrens descend.

This stable profile is shared by every floor. Adding a site-specific identity belongs in
`folkloreFor`; broad regional fallback belongs in the region/exterior rules, so map IDs are not the
only source of meaning.

## Floor grammar

Each floor selects and varies a topology from a library of room graphs. Rooms are connected in a
guaranteed critical route, then receive an extra loop. Room sizes, corridor bends, role order, and
topology vary by site, seed, and floor. The start and forward stair positions come from the generated
plan and need not occupy the same coordinates on adjacent storeys.

Every room has a functional role:

`ENTRY → GUARD / HABITATION / WORKS / BURIAL / SHRINE / TREASURE → EXIT or BOSS`

Theme-safe floor accents make roles legible. A shrine or working room receives one warm-light
landmark and the final objective receives one carved sigil, so special art aids navigation without
visibly repeating.

## Exterior blending

Surface influence enters only as coarse secondary patches on non-critical room tiles. Flood, bell,
and storm traditions carry damp moss inward; winter and cairn sites carry cold cracked stone;
Sunrealm and dry sites remain dusty; green Crownlands sites can show shallow moss ingress. Dungeon
walls and the critical route are never replaced, and the renderer blends only compatible floor
materials—not walls—at their boundaries.

## Props

Props are selected as a three-part room motif:

1. room function supplies the anchor object (beds and stores in habitation, chains at a
   guard post, graves in burial rooms, altar pieces in shrines);
2. a theme-specific modular detail supplies silhouette variation (bandit equipment, cave growths,
   crypt offerings, goblin scrap, or vampire furnishings);
3. folklore and region choose a restrained light/accent (wet stones and reeds for bell/flood
   traditions, crystals and
   cairns for winter/mountain traditions, urns and fire for the Sunrealm, written memorial objects
   for witnessed-name traditions).

Each room receives a small bounded cluster at its edge. Critical corridors and stair approaches are
reserved before placement. This replaces whole-floor random scatter and fixed global coordinates.

## Encounters

Encounter slots are authored from room roles and form small groups within a room. `GameState` uses
the room role and site bestiary context to choose monsters, then leashes each monster to its encounter
zone. Bosses and their escorts only appear in the terminal room of the terminal floor.

## Required invariants

`DungeonGenerationTest` checks five world seeds and requires:

- every floor tile and stair is connected;
- all sites carry exterior, region, folklore, verticality, and floor-count context;
- floor layouts within a site are unique;
- prop density is bounded and props do not stack;
- directional stair art matches transition count;
- reviewed terrain is native-size, opaque, seamless, and free of white/checker artifacts;
- each floor has a functional light landmark, terminal floors have exactly one sigil, and at least one site
  carries a surface material inward;
- encounter slots do not overlap props or stairs;
- only terminal floors contain one boss;
- the world contains both ascending and descending multi-storey sites.

`DungeonPreview` exports real-render screenshots to `exports/dungeon-layouts` for visual review.
