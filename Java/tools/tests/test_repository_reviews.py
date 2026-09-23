
import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[2]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT
import hashlib
import json
from pathlib import Path
import tempfile
import unittest

from tools.checks.check_review_site import check_archive, check_links


class ReviewIntegrityTest(unittest.TestCase):
    def test_links_handle_queries_fragments_and_missing_files(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            site = root / 'sites'
            (site / 'characters').mkdir(parents=True)
            page = site / 'characters/index.html'
            page.write_text('<a href="picture.png?v=2#frame">Image</a>')
            self.assertEqual(1, len(check_links(root, site)))
            (page.parent / 'picture.png').write_bytes(b'image')
            self.assertEqual([], check_links(root, site))
            page.write_text('<iframe data-src="missing.html"></iframe>')
            self.assertEqual(1, len(check_links(root, site)))

    def test_external_urls_are_not_fetched(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'characters').mkdir()
            (root / 'characters/index.html').write_text('<a href="https://example.invalid/">External</a>')
            self.assertEqual([], check_links(root, root))

    def make_batch(self, root):
        archive = root / 'archive'
        batch = archive / 'batch'
        files = batch / 'files/Java/assets'
        files.mkdir(parents=True)
        (batch / 'README.md').write_text('Retired candidate; restore before use.')
        content = b'original'
        (files / 'old.png').write_bytes(content)
        manifest = [{'path': 'Java/assets/old.png', 'reason': 'test fixture',
                     'bytes': len(content), 'sha256': hashlib.sha256(content).hexdigest()}]
        (batch / 'manifest.json').write_text(json.dumps(manifest))
        return archive, batch, manifest

    def test_archive_detects_tampering(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            archive, batch, _ = self.make_batch(root)
            self.assertEqual([], check_archive(root, archive))
            (batch / 'files/Java/assets/old.png').write_bytes(b'changed!')
            self.assertTrue(check_archive(root, archive))

    def test_archive_rejects_path_traversal(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            archive, batch, manifest = self.make_batch(root)
            manifest[0]['path'] = '../outside.png'
            (batch / 'manifest.json').write_text(json.dumps(manifest))
            self.assertIn('unsafe original path', check_archive(root, archive)[0])

    def test_archive_requires_manifest_coverage(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            archive, batch, _ = self.make_batch(root)
            (batch / 'files/unrecorded.png').write_bytes(b'extra')
            self.assertIn('does not cover', check_archive(root, archive)[0])


if __name__ == '__main__':
    unittest.main()
