from __future__ import annotations

from universal_cutout import (
    chroma_cutout,
    clean_spill_edges,
    clear_stray_components,
    component_gap,
    detect_key_kind,
    fit,
    is_green_key,
    is_key,
    is_magenta_key,
    is_spill,
    neighbor_replacement,
    remove_boundary_spill,
    remove_connected_key,
    remove_key_pixels,
    save_chroma_cutout,
    square_trim,
    touches_transparency,
    trim_alpha,
)


__all__ = [
    "chroma_cutout",
    "clean_spill_edges",
    "clear_stray_components",
    "component_gap",
    "detect_key_kind",
    "fit",
    "is_green_key",
    "is_key",
    "is_magenta_key",
    "is_spill",
    "neighbor_replacement",
    "remove_boundary_spill",
    "remove_connected_key",
    "remove_key_pixels",
    "save_chroma_cutout",
    "square_trim",
    "touches_transparency",
    "trim_alpha",
]
