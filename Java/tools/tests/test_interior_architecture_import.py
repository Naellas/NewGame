"""Crop recipe reproducibility and validation, using tiny disposable atlases."""
import hashlib
import json
from pathlib import Path
import tempfile
import unittest
from PIL import Image
from tools.assets.environments.extract_interior_seamless_tiles import import_recipe


class InteriorArchitectureImportTest(unittest.TestCase):
    def test_cutout_keeps_enclosed_dark_panes(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = Image.new("RGB", (8, 8), (8, 12, 16))
            source.paste((150, 70, 20), (1, 1, 7, 7))
            source.paste((8, 12, 16), (2, 2, 6, 6))
            source.save(root / "sheet.png")
            recipe = {"sources": {"window": {"file": "sheet.png", "sha256": hashlib.sha256((root / "sheet.png").read_bytes()).hexdigest()}},
                      "crops": [{"source": "window", "box": [0, 0, 8, 8], "size": [8, 8],
                                 "clear_navy_background": True, "asset_file": "environments/interiors/window.png"}]}
            path = root / "recipe.json"
            path.write_text(json.dumps(recipe), encoding="utf-8")
            with Image.open(import_recipe(path, root / "out")[0]) as result:
                self.assertEqual(result.getpixel((0, 0))[3], 0)
                self.assertEqual(result.getpixel((3, 3)), (8, 12, 16, 255))

    def test_recipe_extracts_only_named_rectangle_and_rotates(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = Image.new("RGB", (8, 8), (255, 0, 0))
            source.paste((10, 100, 200), (2, 2, 4, 6))
            source.save(root / "sheet.png")
            recipe = {"sources": {"test": {"file": "sheet.png", "sha256": hashlib.sha256((root / "sheet.png").read_bytes()).hexdigest()}},
                      "crops": [{"source": "test", "box": [2, 2, 4, 6], "rotate": 90,
                                 "size": [4, 2], "asset_file": "environments/interiors/piece.png"}]}
            path = root / "recipe.json"
            path.write_text(json.dumps(recipe), encoding="utf-8")
            outputs = import_recipe(path, root / "out")
            self.assertEqual(len(outputs), 1)
            with Image.open(outputs[0]) as result:
                self.assertEqual(result.size, (4, 2))
                self.assertEqual({result.getpixel((x, y)) for y in range(2) for x in range(4)}, {(10, 100, 200, 255)})
            recipe["crops"][0]["box"] = [2, 2, 20, 6]
            path.write_text(json.dumps(recipe), encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "Invalid crop"):
                import_recipe(path, root / "out")
            recipe["sources"]["test"]["sha256"] = "wrong"
            path.write_text(json.dumps(recipe), encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "checksum mismatch"):
                import_recipe(path, root / "out")


if __name__ == "__main__":
    unittest.main()
