"""Policy regression tests: python -m unittest discover -s Java/tools -p test_repository_hygiene.py"""

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[2]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT
import unittest
import subprocess
import tempfile
from pathlib import Path
from tools.checks.check_repository_hygiene import repository_paths, violations
from tools.checks.check_structure import duplicate_errors


class HygieneTest(unittest.TestCase):
    def test_asset_ambiguity_baseline_cannot_cover_new_siblings(self):
        known = {'mage': ['sources/old.png', 'walk/old.png']}
        self.assertEqual([], duplicate_errors({'mage': {'sources/old.png', 'walk/old.png'}}, known))
        self.assertTrue(duplicate_errors({'mage': {'sources/old.png', 'walk/old.png', 'new/mage.png'}}, known))
        self.assertTrue(duplicate_errors({'new': {'a/new.png', 'b/new.png'}}, known))

    def test_accepted_layout(self):
        self.assertEqual(set(), violations([
            'AGENTS.md', 'Java/assets/characters/monsters/wolf.png',
            'Java/assets/characters/monsters/animations/wolf.frames',
            'art-source/monsters/2026-09-23/wolf.png',
            'asset-review/reviews/monsters/2026-09-23/README.md',
            'Java/tools/assets/monsters/import_wolf.py', '.github/workflows/repository-hygiene.yml',
        ]))

    def test_sources_in_runtime_and_legacy_root(self):
        result = violations(['Java/assets/characters/companions/aria/sources/a.png', 'assets/source/a.png'])
        self.assertIn('source-in-runtime:Java/assets/characters/companions/aria/sources/a.png', result)
        self.assertIn('unapproved-root:assets/source/a.png', result)

    def test_tracked_local_outputs_are_rejected(self):
        result = violations(['Java/out-trial/A.class', 'Java/config/settings.properties',
                             'Java/temp/capture.png', 'Java/tools/__pycache__/x.pyc'])
        self.assertIn('local-output:Java/out-trial/A.class', result)
        self.assertIn('generated-file:Java/out-trial/A.class', result)
        self.assertIn('local-settings:Java/config/settings.properties', result)
        self.assertIn('local-output:Java/temp/capture.png', result)
        self.assertIn('generated-file:Java/tools/__pycache__/x.pyc', result)

    def test_exception_does_not_cover_new_siblings(self):
        old = violations(['Java/assets/source/old.png'])
        current = violations(['Java/assets/source/old.png', 'Java/assets/source/new.png'])
        self.assertEqual({'source-in-runtime:Java/assets/source/new.png'}, current - old)

    def test_moved_review_sites_cannot_grow_in_old_location(self):
        self.assertEqual(set(), violations(['asset-review/characters/index.html']))
        self.assertIn('review-outside-tools:asset-review/characters/extra.png',
                      violations(['asset-review/characters/extra.png']))
        self.assertIn('review-outside-tools:asset-review/dialogue-motion/index.html',
                      violations(['asset-review/dialogue-motion/index.html']))

    def test_new_source_and_asset_boundaries(self):
        findings = violations(['Java/src/main/java/com/alderfall/game/NewTest.java',
                               'Java/tools/import_new.py', 'Java/assets/monsters/new.png'])
        self.assertIn('diagnostic-in-production:Java/src/main/java/com/alderfall/game/NewTest.java', findings)
        self.assertIn('ungrouped-tool:Java/tools/import_new.py', findings)
        self.assertIn('retired-asset-family:Java/assets/monsters/new.png', findings)
        self.assertEqual(set(), violations(['Java/src/test/java/com/alderfall/game/NewTest.java']))

    def test_git_inventory_catches_force_added_output_but_skips_local_scratch(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            subprocess.run(['git', 'init', '-q', str(root)], check=True)
            (root / '.gitignore').write_text('*.class\n')
            (root / 'tracked.class').write_bytes(b'tracked')
            (root / 'local.class').write_bytes(b'local')
            subprocess.run(['git', '-C', str(root), 'add', '-f', 'tracked.class'], check=True)
            paths = repository_paths(root)
            self.assertIn('tracked.class', paths)
            self.assertNotIn('local.class', paths)
            self.assertIn('generated-file:tracked.class', violations(paths))


if __name__ == '__main__':
    unittest.main()
