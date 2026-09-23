"""Validate generated resource PNG packaging and write a local artwork gallery.

Run from any directory: python Java/tools/audit_resource_art.py
This reads image pixels for validation only; it does not modify artwork.
"""
from pathlib import Path
from hashlib import sha256
from html import escape
import json
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "assets"
manifest = json.loads((ASSETS / "source/resource-art-prompts.json").read_text(encoding="utf-8"))
seen = set()
cards = []
errors = []
for entry in manifest["assets"]:
    path = ASSETS / entry["file"]
    if not path.is_file():
        errors.append(f"Missing sprite: {entry['file']}")
        continue
    expected = 96 if entry["file"].startswith("items/") else 256 if "tree_" in path.name else 192
    with Image.open(path) as image:
        if image.mode != "RGBA" or image.size != (expected, expected):
            errors.append(f"Wrong format/size: {entry['file']}: {image.mode} {image.size}")
            continue
        alpha = image.getchannel("A")
        low, high = alpha.getextrema()
        if low != 0 or high != 255:
            errors.append(f"Missing transparent background or opaque subject: {entry['file']}")
        bbox = alpha.point(lambda a: 255 if a > 32 else 0).getbbox()
        if bbox is None or bbox[0] < 1 or bbox[1] < 1 or bbox[2] >= expected or bbox[3] >= expected:
            errors.append(f"Sprite is empty or clipped: {entry['file']}")
    digest = sha256(path.read_bytes()).hexdigest()
    if digest in seen:
        errors.append(f"Duplicate artwork: {entry['file']}")
    seen.add(digest)
    provenance = ASSETS / "source/resources-painted" / (path.stem + ".json")
    if not provenance.is_file():
        errors.append(f"Missing generation provenance: {entry['file']}")
    label = path.stem.removeprefix("material_").removeprefix("deco_ore_").removeprefix("deco_tree_").replace("_", " ").title()
    url = "../assets/" + entry["file"]
    cards.append(f'<article><a class="images" href="{escape(url)}"><img src="{escape(url)}" alt="{escape(label)}" width="96" height="96"><img class="game" src="{escape(url)}" alt="" width="40" height="40"></a><h2>{escape(label)}</h2><p>{expected} x {expected} RGBA PNG</p></article>')

if errors:
    raise SystemExit("\n".join(errors))

gallery = ROOT / "docs/resource-art-gallery.html"
gallery.write_text('''<!doctype html>
<html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Alderfall resource artwork</title>
<style>
body{margin:0;background:#18221e;color:#e4e8d5;font:16px system-ui,sans-serif;padding:32px}
h1{font-size:30px;margin:0 0 10px}header{max-width:900px;margin-bottom:28px}header p{color:#bbc9b7;line-height:1.6}
main{display:grid;grid-template-columns:repeat(auto-fill,minmax(205px,1fr));gap:16px;max-width:1400px}
article{background:#25342c;border:1px solid #425447;border-radius:8px;padding:16px}
.images{display:flex;align-items:center;justify-content:space-around;background:#304336;height:112px;border-radius:5px}
img{object-fit:contain}h2{font-size:15px;margin:14px 0 6px}article p{font-size:12px;color:#a6b8a5;margin:0}
</style><header><h1>Alderfall · Resource artwork</h1><p>Transparent painted fantasy sprites for deposits, timber and refined materials. Each card shows the artwork at 96 pixels and at a 40-pixel game scale. Ore nodes appear only in their appropriate biomes; the matching materials feed refining and crafting recipes.</p></header><main>
''' + "\n".join(cards) + "\n</main></html>\n", encoding="utf-8")
print(f"Validated {len(cards)} distinct transparent PNG sprites. Gallery: {gallery}")
