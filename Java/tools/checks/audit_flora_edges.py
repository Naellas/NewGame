"""Read-only before/after edge-color audit for the regenerated outdoor sprites."""

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[2]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT
import csv
from pathlib import Path
from PIL import Image, ImageFilter
from tools.assets.shared.asset_paths import asset_relative

ROOT = REPO_ROOT
REVIEW = ROOT / "asset-review" / "flora-refresh"


def inspect(path):
    image = Image.open(path).convert("RGBA")
    minimum_alpha = image.getchannel("A").filter(ImageFilter.MinFilter(3))
    edges = magenta = 0
    for (r, g, b, a), minimum in zip(image.getdata(), minimum_alpha.getdata()):
        if a < 32 or minimum >= 32:
            continue
        edges += 1
        if r > 100 and b > 100 and r > g * 1.5 and b > g * 1.4:
            magenta += 1
    return magenta, edges


def main():
    rows = []
    with (REVIEW / "manifest.tsv").open(encoding="utf-8") as manifest:
        for entry in csv.DictReader(manifest, delimiter="\t"):
            relative = entry["asset"]
            before, before_edges = inspect(REVIEW / "before" / relative)
            after, after_edges = inspect(ROOT / "Java" / "assets" / asset_relative(relative))
            rows.append((relative, before, after, before_edges, after_edges))
    with (REVIEW / "edge-audit.tsv").open("w", encoding="utf-8", newline="") as output:
        writer = csv.writer(output, delimiter="\t")
        writer.writerow(("asset", "before_magenta", "after_magenta", "before_edge", "after_edge"))
        writer.writerows(rows)
    print(f"Audited {len(rows)} sprites: flagged edge pixels {sum(r[1] for r in rows)} -> {sum(r[2] for r in rows)}")
    print("Counts include intentional pink/purple petals; this is a review metric, not a recoloring rule.")


if __name__ == "__main__":
    main()
