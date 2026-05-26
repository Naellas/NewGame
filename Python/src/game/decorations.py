from __future__ import annotations

from dataclasses import dataclass


LAND_BASE_DECORATIONS = (
    "deco_imagen_stone_stack",
    "deco_imagen_flat_stone_stack",
    "deco_imagen_milestone",
)

WET_BASE_DECORATIONS = (
    "deco_imagen_lily_patch",
    "deco_imagen_reed_islet",
    "deco_imagen_driftwood",
)

ROAD_DECORATIONS = (
    "deco_road_milestone",
    "deco_road_signpost",
    "deco_imagen_signpost",
    "deco_imagen_milestone",
    "deco_imagen_road_camp",
)

LOCATION_GROUND_ASSETS = {
    "farmland": (
        "location_farmland_tilled",
        "location_farmland_wheat",
    ),
    "goblin_camp": (
        "location_graveyard_dirt",
        "location_graveyard_path",
    ),
    "graveyard": (
        "location_graveyard_dirt",
        "location_graveyard_path",
    ),
}

LOCATION_DECORATIONS = {
    "farmland": (
        "location_farmland_scarecrow",
        "location_farmland_hay_bales",
        "location_farmland_fence",
        "location_farmland_wheat",
    ),
    "goblin_camp": (
        "location_camp_tent",
        "location_camp_fire",
        "location_camp_palisade",
        "location_camp_crates",
        "location_graveyard_skull_marker",
    ),
    "graveyard": (
        "location_graveyard_tombstones",
        "location_crypt_entrance",
        "location_graveyard_iron_fence",
        "location_graveyard_dead_stump",
        "location_graveyard_skull_marker",
    ),
}

BIOME_DECORATIONS = {
    "g": (
        "deco_bush",
        "deco_flowers",
        "deco_grass_clump",
        "deco_grass_wildflowers",
        "deco_grass_herb_patch",
        "deco_grass_pond",
        "deco_grass_stone_stack",
        "deco_imagen_grass_pond",
        "deco_imagen_wildflower_tuft",
        "deco_imagen_meadow_blooms",
        "deco_imagen_mossy_pond",
        *LAND_BASE_DECORATIONS,
    ),
    "f": (
        "deco_tree_pine",
        "deco_tree_blue_pine",
        "deco_tree_oak",
        "deco_tree_round",
        "deco_tree_young",
        "deco_forest_blue_mushroom_ring",
        "deco_forest_shrine_stone",
        "deco_forest_fairy_pool",
        "deco_forest_ancient_roots",
        "deco_imagen_forest_roots",
        "deco_imagen_fallen_roots",
        "deco_imagen_mushroom_circle",
        "deco_imagen_pale_mushroom_ring",
        "deco_imagen_shrine_stone",
        "deco_imagen_green_rune_stone",
        *LAND_BASE_DECORATIONS,
    ),
    "s": (
        "deco_cactus",
        "deco_dry_grass",
        "deco_desert_rocks",
        "deco_desert_blooming_cactus",
        "deco_desert_sun_bleached_bones",
        "deco_desert_oasis_pool",
        "deco_desert_jar_cache",
        "deco_imagen_desert_oasis",
        "deco_imagen_sandy_pool",
        "deco_imagen_clay_jars",
        "deco_imagen_small_jars",
        "deco_imagen_cactus_cluster",
        *LAND_BASE_DECORATIONS,
    ),
    "n": (
        "deco_snow_pine",
        "deco_snow_mound",
        "deco_tundra_rocks",
        "deco_tundra_ice_crystals",
        "deco_tundra_frost_bush",
        "deco_tundra_thaw_pond",
        "deco_tundra_rune_stone",
        "deco_imagen_tundra_thaw_pool",
        "deco_imagen_tundra_rune_stone",
        *LAND_BASE_DECORATIONS,
    ),
    "v": (
        "deco_reeds",
        "deco_mushrooms",
        "deco_bog_grass",
        "deco_marsh_lily_pool",
        "deco_marsh_twisted_roots",
        "deco_marsh_bubble_pool",
        "deco_marsh_firefly_reeds",
        "deco_imagen_marsh_bubble_pool",
        "deco_imagen_reed_clump",
        "deco_imagen_reed_islet",
        "deco_imagen_lily_patch",
        "deco_imagen_mossy_pond",
    ),
    "b": (
        "deco_badlands_rocks",
        "deco_badlands_dry_grass",
        "deco_badlands_red_spire",
        "deco_badlands_skull_marker",
        "deco_badlands_dust_bowl",
        "deco_badlands_totem_stones",
        "deco_imagen_badlands_spires",
        "deco_imagen_skull_marker",
        "deco_imagen_cairn_stack",
        *LAND_BASE_DECORATIONS,
    ),
    "m": (
        "deco_mountain_rocks",
        "deco_mountain_crystal_cluster",
        "deco_mountain_scrub_pine",
        "deco_mountain_spring_pool",
        "deco_mountain_cairn",
        "deco_imagen_crystal_cluster",
        "deco_imagen_mountain_spring",
        "deco_imagen_cairn_stack",
        *LAND_BASE_DECORATIONS,
    ),
    "q": (
        "deco_mountain_rocks",
        "deco_rocks",
        "deco_mountain_crystal_cluster",
        "deco_mountain_pass_snowmelt_pool",
        "deco_mountain_pass_way_cairn",
        "deco_imagen_mountain_spring",
        "deco_imagen_cairn_stack",
        "deco_imagen_signpost",
        *LAND_BASE_DECORATIONS,
    ),
    "w": (
        "deco_water_lilies",
        "deco_water_foam_reeds",
        "deco_water_driftwood",
        "deco_water_reed_islet",
        *WET_BASE_DECORATIONS,
    ),
    "r": ROAD_DECORATIONS,
}

FOREST_FLOOR_DECORATIONS = (
    "deco_forest_fern",
    "deco_forest_log",
    "deco_forest_mushrooms",
    "deco_forest_moss_rock",
)

FOREST_FEATURE_DECORATIONS = (
    "deco_forest_blue_mushroom_ring",
    "deco_forest_shrine_stone",
    "deco_forest_fairy_pool",
    "deco_forest_ancient_roots",
    "deco_imagen_forest_roots",
    "deco_imagen_fallen_roots",
    "deco_imagen_mushroom_circle",
    "deco_imagen_pale_mushroom_ring",
    "deco_imagen_shrine_stone",
    "deco_imagen_green_rune_stone",
)

FOREST_TREE_DECORATIONS = (
    "deco_tree_pine",
    "deco_tree_pine",
    "deco_tree_blue_pine",
    "deco_tree_blue_pine",
    "deco_tree_oak",
    "deco_tree_round",
)

DECORATION_SIZE_RULES = {
    "deco_snow_pine": (1.18, 3, 0.09),
    "deco_cactus": (0.98, 3, 0.08),
    "deco_desert_blooming_cactus": (1.06, 3, 0.08),
    "deco_tundra_ice_crystals": (0.96, 3, 0.08),
    "deco_tundra_rune_stone": (0.94, 3, 0.08),
    "deco_mountain_crystal_cluster": (0.96, 3, 0.08),
    "deco_mountain_scrub_pine": (1.08, 3, 0.08),
    "deco_mountain_cairn": (0.92, 3, 0.07),
    "deco_mountain_pass_way_cairn": (0.92, 3, 0.07),
    "deco_badlands_red_spire": (1.08, 3, 0.08),
    "deco_badlands_totem_stones": (0.98, 3, 0.08),
    "deco_road_milestone": (0.92, 3, 0.06),
    "deco_road_signpost": (0.98, 3, 0.07),
    "deco_imagen_cactus_cluster": (1.12, 3, 0.08),
    "deco_imagen_badlands_spires": (1.08, 3, 0.08),
    "deco_imagen_crystal_cluster": (1.02, 3, 0.08),
    "deco_imagen_cairn_stack": (0.94, 3, 0.07),
    "deco_imagen_road_camp": (1.12, 3, 0.08),
    "deco_imagen_signpost": (0.98, 3, 0.07),
    "deco_imagen_milestone": (0.92, 3, 0.06),
    "deco_imagen_shrine_stone": (1.02, 3, 0.08),
    "deco_imagen_green_rune_stone": (1.02, 3, 0.08),
    "deco_imagen_tundra_rune_stone": (1.02, 3, 0.08),
    "location_camp_palisade": (1.18, 3, 0.08),
    "location_camp_tent": (1.14, 3, 0.08),
    "location_crypt_entrance": (1.26, 3, 0.08),
    "location_farmland_fence": (1.05, 3, 0.06),
    "location_farmland_scarecrow": (1.10, 3, 0.07),
    "location_graveyard_iron_fence": (1.08, 3, 0.06),
    "location_graveyard_tombstones": (1.08, 3, 0.07),
}
DECORATION_DEFAULT_SIZE = (0.72, 3, 0.08)


@dataclass(frozen=True)
class DecorationSpawnProfile:
    chance_scale: float
    cluster_gain_scale: float
    max_chance: int
    spacing_radius: int
    spacing_tags: frozenset[str]


DECORATION_TAG_PICK_WEIGHTS = {
    "ground": 12,
    "tree": 4,
    "feature": 5,
    "water": 5,
    "road": 3,
}

DECORATION_SPAWN_PROFILES = {
    "ground": DecorationSpawnProfile(0.88, 0.70, 42, 0, frozenset()),
    "tree": DecorationSpawnProfile(0.66, 0.42, 34, 1, frozenset({"tree", "feature", "water", "road"})),
    "feature": DecorationSpawnProfile(0.56, 0.38, 30, 1, frozenset({"tree", "feature", "water", "road"})),
    "water": DecorationSpawnProfile(0.60, 0.40, 32, 1, frozenset({"tree", "feature", "water", "road"})),
    "road": DecorationSpawnProfile(0.42, 0.28, 20, 2, frozenset({"tree", "feature", "water", "road"})),
}

DECORATION_TAG_OVERRIDES = {
    "deco_bush": "ground",
    "deco_flowers": "ground",
    "deco_grass_clump": "ground",
    "deco_grass_wildflowers": "ground",
    "deco_grass_herb_patch": "ground",
    "deco_dry_grass": "ground",
    "deco_snow_mound": "ground",
    "deco_reeds": "ground",
    "deco_mushrooms": "ground",
    "deco_bog_grass": "ground",
    "deco_imagen_wildflower_tuft": "ground",
    "deco_imagen_meadow_blooms": "ground",
    "location_farmland_wheat": "ground",
    "location_graveyard_dirt": "ground",
    "location_graveyard_path": "ground",
}


def decoration_primary_tag(asset: str) -> str:
    override = DECORATION_TAG_OVERRIDES.get(asset)
    if override:
        return override
    if asset in ROAD_DECORATIONS or any(part in asset for part in ("road", "signpost", "milestone")):
        return "road"
    if any(part in asset for part in ("pond", "pool", "lily", "reed_islet", "driftwood", "oasis", "spring", "snowmelt", "thaw", "bubble")):
        return "water"
    if any(part in asset for part in ("tree", "pine", "cactus", "scrub")):
        return "tree"
    if any(
        part in asset
        for part in (
            "bone",
            "cairn",
            "camp",
            "crates",
            "crystal",
            "crypt",
            "fence",
            "hay",
            "jars",
            "mushroom_circle",
            "mushroom_ring",
            "palisade",
            "rock",
            "roots",
            "rune",
            "scarecrow",
            "shrine",
            "skull",
            "spire",
            "stone",
            "tent",
            "tombstone",
            "totem",
        )
    ):
        return "feature"
    return "ground"


def decoration_spawn_profile(asset: str) -> DecorationSpawnProfile:
    return DECORATION_SPAWN_PROFILES[decoration_primary_tag(asset)]


def weighted_decoration_choice(options: tuple[str, ...], seed: int) -> str:
    total = sum(DECORATION_TAG_PICK_WEIGHTS[decoration_primary_tag(asset)] for asset in options)
    roll = seed % max(1, total)
    for asset in options:
        roll -= DECORATION_TAG_PICK_WEIGHTS[decoration_primary_tag(asset)]
        if roll < 0:
            return asset
    return options[-1]


def all_decoration_assets() -> tuple[str, ...]:
    names: set[str] = set()
    for assets in BIOME_DECORATIONS.values():
        names.update(assets)
    for assets in LOCATION_GROUND_ASSETS.values():
        names.update(assets)
    for assets in LOCATION_DECORATIONS.values():
        names.update(assets)
    names.update(FOREST_FLOOR_DECORATIONS)
    names.update(FOREST_FEATURE_DECORATIONS)
    names.update(FOREST_TREE_DECORATIONS)
    return tuple(sorted(names))


def decoration_size(asset: str, seed: int, tile_size: int) -> int:
    base_ratio, steps, step_ratio = DECORATION_SIZE_RULES.get(asset, DECORATION_DEFAULT_SIZE)
    ratio = base_ratio + ((seed >> 16) % steps) * step_ratio
    return max(12, int(round(tile_size * ratio)))
