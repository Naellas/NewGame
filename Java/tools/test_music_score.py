"""Run with python -m unittest discover -s tools -p test_music_score.py."""
import unittest
import wave

import numpy as np

from musicgen import OUT, PRESETS, NOTE_INDEX, SCALES
from music_score import RATE, ENCOUNTER_SCORES, render_score


class MusicScoreTest(unittest.TestCase):
    def test_encounter_variety(self):
        signatures = {(p[1], p[2], p[3], p[4], p[5]) for p in ENCOUNTER_SCORES.values()}
        self.assertEqual(len(signatures), len(ENCOUNTER_SCORES))
        self.assertEqual(sum(k.startswith("battle_elite") for k in ENCOUNTER_SCORES), 3)
        self.assertEqual(sum(k.startswith("battle_boss") for k in ENCOUNTER_SCORES), 4)
        self.assertEqual(sum(k.startswith("dungeon") for k in ENCOUNTER_SCORES), 10)

    def test_repeatable_score_and_development(self):
        name = "region_riverside"
        a = render_score(name, PRESETS[name], 8, NOTE_INDEX, SCALES)
        b = render_score(name, PRESETS[name], 8, NOTE_INDEX, SCALES)
        np.testing.assert_array_equal(a, b)
        sections = np.array_split(a, 4)
        levels = [float(np.sqrt(np.mean(s ** 2))) for s in sections]
        self.assertGreater(levels[3], levels[2] * 1.2)
        self.assertGreater(float(np.std(a[:, 0] - a[:, 1])), .005)

    def test_shipped_tracks(self):
        for name in PRESETS:
            with self.subTest(track=name), wave.open(str(OUT / (name + ".wav")), "rb") as stream:
                self.assertEqual((stream.getnchannels(), stream.getsampwidth(), stream.getframerate()), (2, 2, RATE))
                audio = np.frombuffer(stream.readframes(stream.getnframes()), dtype="<i2").reshape(-1, 2) / 32768.0
                self.assertGreater(len(audio) / RATE, 40)
                self.assertLessEqual(float(np.max(np.abs(audio))), .881)
                self.assertGreater(float(np.sqrt(np.mean(audio ** 2))), .04)
                # A seam should be no worse than ordinary waveform movement.
                seam = float(np.max(np.abs(audio[0] - audio[-1])))
                self.assertLess(seam, max(.025, float(np.quantile(np.abs(np.diff(audio, axis=0)), .999))))
                for block in np.array_split(audio, 32):
                    self.assertGreater(float(np.sqrt(np.mean(block ** 2))), .01)


if __name__ == "__main__":
    unittest.main()
