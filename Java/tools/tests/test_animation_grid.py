import unittest
from PIL import Image
from tools.assets.shared.import_animation_grid import assemble


class AnimationGridTest(unittest.TestCase):
    def test_row_order_alpha_and_relative_size(self):
        source = Image.new('RGBA', (80, 80))
        for index, width in enumerate((12, 24, 16, 20)):
            for y in range(10):
                for x in range(width):
                    source.putpixel((index % 2 * 40 + x + 5, index // 2 * 40 + y + 8),
                                    (50 + index * 50, 90, 120, 128))
        strip = assemble(source, 2, 2, 40, 24)
        self.assertEqual(strip.size, (160, 24))
        bounds = []
        for index in range(4):
            frame = strip.crop((index * 40, 0, (index + 1) * 40, 24))
            bounds.append(frame.getchannel('A').point(lambda a: 255 if a > 60 else 0).getbbox())
            self.assertLessEqual(abs(frame.getpixel((20, 14))[0] - (50 + index * 50)), 2)
            self.assertLess(frame.getpixel((20, 14))[3], 160)
            self.assertEqual(frame.getpixel((0, 0))[3], 0)
        self.assertGreater(bounds[1][2] - bounds[1][0], (bounds[0][2] - bounds[0][0]) * 1.7)
        self.assertEqual(len({b[3] for b in bounds}), 1)

    def test_empty_frame_rejected(self):
        with self.assertRaisesRegex(ValueError, 'Empty frame'):
            assemble(Image.new('RGBA', (80, 40)), 2, 1, 40, 24)

    def test_invisible_noise_does_not_shrink_frames(self):
        source = Image.new('RGBA', (80, 80))
        source.paste((30, 160, 180, 180), (10, 30, 70, 45))
        source.putpixel((0, 0), (30, 30, 30, 1))
        strip = assemble(source, 1, 1, 64, 24)
        box = strip.getchannel('A').getbbox()
        self.assertGreater(box[2] - box[0], 55)


if __name__ == '__main__':
    unittest.main()
