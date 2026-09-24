"""Signal integrity and deterministic regeneration for the original SFX batch."""
import unittest
import sys
from pathlib import Path
if not __package__:
    sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from tools.audio.sfxgen import SURFACES, sample_names, synthesize


class SfxGenerationTest(unittest.TestCase):
    def test_signal_integrity(self):
        names = sample_names()
        self.assertEqual(len(names), len(set(names)))
        for name in names:
            with self.subTest(name=name):
                signal = synthesize(name)
                self.assertEqual(signal[0], 0)
                self.assertLess(abs(signal[-1]), .002)
                self.assertLessEqual(max(map(abs, signal)), .851)
                self.assertGreater(sum(s*s for s in signal) / len(signal), .00001)

    def test_footsteps_stay_soft_and_varied(self):
        for surface in SURFACES:
            variants = [synthesize(f"foot_{surface}_{v}") for v in range(1, 9)]
            self.assertEqual(len({tuple(s) for s in variants}), 8)
            self.assertGreater(len({len(s) for s in variants}), 4)
            for signal in variants:
                self.assertLess(max(map(abs, signal)), .12)
                self.assertLess(sum(s*s for s in signal) / len(signal), .0005)

    def test_variation_and_reproducibility(self):
        a = synthesize("foot_stone_1")
        self.assertEqual(a, synthesize("foot_stone_1"))
        self.assertNotEqual(a, synthesize("foot_stone_2"))


if __name__ == "__main__":
    unittest.main()
