"""Focused regression checks for shared image mechanics and canonical paths."""
import sys
from pathlib import Path
if not __package__:
    sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
import unittest
import tempfile
from PIL import Image
from tools.assets.shared import image_ops
from tools.assets.shared.asset_paths import asset_relative, player_dir, companion_dir, animation_dir, find_asset


class ImageHelpersTest(unittest.TestCase):
    def test_building_and_equipment_routes(self):
        cases = {
            'city/regional/regional_sun_cistern_house.png': 'environments/settlements/city/buildings/sun/regional_sun_cistern_house.png',
            'items/generated/new_armor_mended_linen_hood.png': 'items/helmets/new_armor_mended_linen_hood.png',
            'items/heartstone_crook.png': 'items/weapons/staves/heartstone_crook.png',
            'items/material_oak_wood.png': 'items/materials/material_oak_wood.png',
        }
        for old, new in cases.items():
            self.assertEqual(new, asset_relative(old))
            self.assertEqual(new, asset_relative(new))


    def test_reparenting_preserves_legacy_importer_selection(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / 'source/monster-attacks/demon_queen.png'
            sprite = root / 'characters/npcs/story/demon_queen.png'
            source.parent.mkdir(parents=True)
            sprite.parent.mkdir(parents=True)
            source.write_bytes(b'source')
            sprite.write_bytes(b'sprite')
            self.assertEqual(source, find_asset(root, 'demon_queen.png'))

    def test_clipped_blit_preserves_only_overlap(self):
        out = Image.new('RGBA', (2, 2))
        image_ops.clip_blit(out, Image.new('RGBA', (2, 2), 'red'), -1, -1)
        self.assertEqual([(255, 0, 0, 255), (0, 0, 0, 0), (0, 0, 0, 0), (0, 0, 0, 0)], list(out.getdata()))

    def test_chroma_policy_is_supplied_by_importer(self):
        source = Image.new('RGBA', (1, 1), (220, 220, 220, 255))
        kept = image_ops.chroma_to_alpha(source, keyed_alpha=lambda p: p[3], crop_alpha=lambda p: p)
        removed = image_ops.chroma_to_alpha(source, keyed_alpha=lambda p: 0, crop_alpha=lambda p: p)
        self.assertEqual(255, kept.getpixel((0, 0))[3])
        self.assertEqual((0, 0, 0, 0), removed.getpixel((0, 0)))

    def test_footprint_profiles_remain_distinct(self):
        source = Image.new('RGBA', (4, 4), 'red')
        smooth = image_ops.fit_footprint_lanczos(source, 20, 20)
        pixel = image_ops.fit_footprint_nearest(source, 20, 20)
        self.assertEqual((1, 1, 19, 19), smooth.getbbox())
        self.assertEqual((4, 4, 16, 16), pixel.getbbox())

    def test_empty_and_nonempty_crop_geometry(self):
        blank = Image.new('RGBA', (4, 4))
        self.assertEqual(blank.size, image_ops.trim(blank).size)
        self.assertEqual((2, 3, 11, 15), image_ops.component_box([(2, 3), (10, 9), (4, 14)]))

    def test_owner_specific_animation_paths(self):
        root = Path('assets')
        self.assertEqual(root / 'characters/player/classes/mage', player_dir(root, 'class_mage'))
        self.assertEqual(root / 'characters/companions/aria', companion_dir(root, 'npc_aria'))
        self.assertEqual(root / 'characters/monsters/animations', animation_dir(root, 'wolf_attack_anim'))
        self.assertEqual(root / 'characters/companions/aria/animations', animation_dir(root, 'npc_aria_model_walk_anim'))

    def test_legacy_path_mapping_preserves_nested_roles(self):
        self.assertEqual('environments/settlements/city/regional/house.png', asset_relative('city/regional/house.png'))
        self.assertEqual('effects/animations/x.png', asset_relative('effects/animations/x.png'))
        with self.assertRaises(ValueError):
            asset_relative('../outside.png')


if __name__ == '__main__':
    unittest.main()
