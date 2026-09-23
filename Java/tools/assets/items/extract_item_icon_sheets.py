from __future__ import annotations

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[3]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT

from pathlib import Path

from PIL import Image

from tools.assets.shared.universal_cutout import CutoutSettings, fit, universal_cutout


ROOT = JAVA_ROOT
SOURCE = ROOT / "assets" / "source"
OUT = ROOT / "assets" / "items"

GROUPS = {
    "imagegen-item-swords-sheet.png": [
        "rustbit_saber", "riverguard_blade", "cinderedge", "frostvein_sword", "thorncarver",
        "moonlit_rapier", "sunfall_longsword", "stormglass_blade", "duskwake_sword", "kingsroad_claymore",
        "marshlight_cutlass", "obsidian_fang", "starforged_sword", "heartstone_blade", "oathbreaker_edge",
    ],
    "imagegen-item-staves-sheet.png": [
        "oakheart_staff", "ashwind_staff", "embercore_staff", "frostroot_staff", "stormcall_staff",
        "moonwell_staff", "sunspire_staff", "thornbinder_staff", "bogsong_staff", "glassdune_staff",
        "archive_scepter", "starfall_staff", "heartstone_crook", "voidglass_staff", "dawnweave_staff",
    ],
    "imagegen-item-axes-sheet.png": [
        "woodcutter_axe", "raider_hatchet", "ironbeard_axe", "steelcleaver", "frosthew_axe",
        "embermaul_axe", "stormsplitter", "thornbite_axe", "bonehook_axe", "obsidian_chopper",
        "sunward_axe", "marshreaper", "mountainfall_axe", "royal_halberd_axe", "starbreaker_axe",
    ],
    "imagegen-item-shields-sheet.png": [
        "river_buckler", "oaken_roundshield", "iron_kite_shield", "towerguard_shield", "frostguard_aegis",
        "emberward_shield", "stormwall_shield", "suncrest_shield", "shadowglass_shield", "thornwall_shield",
        "marshreed_ward", "bonebound_shield", "royal_heater", "starforged_aegis", "heartstone_bulwark",
    ],
    "imagegen-item-light-armors-sheet.png": [
        "padded_gambeson", "scout_leathers", "ranger_jerkin", "duelist_jacket", "nightweave_coat",
        "marshrunner_mantle", "dunewrap_vest", "frostleaf_cloak", "thornsilk_armor", "stormhide_jacket",
        "suncloth_mantle", "archive_mantle", "starweave_robes", "moonwater_coat", "heartwood_vest",
    ],
    "imagegen-item-heavy-armors-sheet.png": [
        "riveted_mail", "guard_cuirass", "steel_bastion_plate", "mountain_plate", "boneguard_plate",
        "obsidian_plate", "frostbound_plate", "emberforged_plate", "stormguard_plate", "sunwarden_plate",
        "shadowplate_harness", "thornplate_mail", "marshbulwark_plate", "royal_wardplate", "starforged_plate",
    ],
    "imagegen-item-accessories-sheet.png": [
        "copper_signet", "river_pearl_charm", "emberglass_ring", "frostdrop_pendant", "stormbead_brooch",
        "sunward_medallion", "moonthread_necklace", "thornroot_charm", "marshlight_seal", "dunestar_talisman",
        "archive_lens", "shadowband", "heartstone_locket", "starrelic_ring", "phoenix_crown_pin",
    ],
}


def split_sheet(path: Path, names: list[str]) -> None:
    sheet = Image.open(path).convert("RGBA")
    cols = 5
    rows = 3
    cell_w = sheet.width // cols
    cell_h = sheet.height // rows
    if len(names) != cols * rows:
        raise ValueError(f"{path.name} has {len(names)} names, expected {cols * rows}.")
    for index, name in enumerate(names):
        col = index % cols
        row = index // cols
        left = col * cell_w
        top = row * cell_h
        right = sheet.width if col == cols - 1 else (col + 1) * cell_w
        bottom = sheet.height if row == rows - 1 else (row + 1) * cell_h
        cell = sheet.crop((left, top, right, bottom))
        cutout = universal_cutout(
            cell,
            CutoutSettings(mode="magenta", padding=4, square=True, spill_passes=6),
        )
        fit(cutout, 64, 64, bottom_align=False, margin=4).save(OUT / f"{name}.png")


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    for filename, names in GROUPS.items():
        source = SOURCE / filename
        if not source.exists():
            raise FileNotFoundError(f"Missing source sheet: {source}")
        split_sheet(source, names)
    print(f"Extracted {sum(len(names) for names in GROUPS.values())} item icons to {OUT}")


if __name__ == "__main__":
    main()
