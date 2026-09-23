"""Check local HTML links and archive checksums without changing files."""
from __future__ import annotations

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[2]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT

import hashlib
from html.parser import HTMLParser
import json
from pathlib import Path
from urllib.parse import unquote, urlsplit

ROOT = REPO_ROOT
SITES = ROOT / 'Java/tools/reviews'
ARCHIVE = ROOT / 'Java/archive/pending-deletion'


class Links(HTMLParser):
    def __init__(self):
        super().__init__()
        self.urls = []

    def handle_starttag(self, tag, attrs):
        self.urls.extend(value for key, value in attrs
                         if key in {'src', 'href', 'data-src'} and value)


def check_links(root: Path, site: Path) -> list[str]:
    errors = []
    if not (site / 'characters/index.html').is_file():
        errors.append('Missing character review entrypoint')
    for page in sorted(site.rglob('*.html')):
        parser = Links()
        parser.feed(page.read_text(encoding='utf-8-sig'))
        for url in parser.urls:
            parsed = urlsplit(url)
            # Template strings require browser validation, not literal lookup.
            if parsed.scheme or parsed.netloc or not parsed.path or '${' in url:
                continue
            target = (page.parent / unquote(parsed.path)).resolve()
            if not target.is_relative_to(root.resolve()) or not target.exists():
                errors.append(f'{page.relative_to(root)}: missing/outside link {url}')
    return errors


def check_archive(root: Path, archive: Path) -> list[str]:
    errors = []
    if not archive.is_dir():
        return ['Missing pending-deletion archive']
    for batch in sorted(p for p in archive.iterdir() if p.is_dir()):
        try:
            if not (batch / 'README.md').is_file():
                raise ValueError('missing README.md')
            entries = json.loads((batch / 'manifest.json').read_text(encoding='utf-8-sig'))
            seen = set()
            for entry in entries:
                original = entry['path']
                relative = Path(original)
                if relative.is_absolute() or '..' in relative.parts or '\\' in original or ':' in original:
                    raise ValueError(f'unsafe original path: {original}')
                file = (batch / 'files' / relative).resolve()
                if not file.is_relative_to((batch / 'files').resolve()):
                    raise ValueError(f'unsafe archive path: {original}')
                if original in seen:
                    raise ValueError(f'duplicate entry: {original}')
                seen.add(original)
                if not entry.get('reason'):
                    raise ValueError(f'missing reason: {original}')
                if file.stat().st_size != entry['bytes'] or hashlib.sha256(file.read_bytes()).hexdigest() != entry['sha256']:
                    raise ValueError(f'checksum/size mismatch: {original}')
            actual = {p.relative_to(batch / 'files').as_posix()
                      for p in (batch / 'files').rglob('*') if p.is_file()}
            if actual != seen:
                raise ValueError('manifest does not cover exactly the archived files')
        except (OSError, ValueError, TypeError, KeyError) as exc:
            errors.append(f'{batch.relative_to(root)}: {exc}')
    return errors


def main() -> int:
    errors = check_links(ROOT, SITES) + check_archive(ROOT, ARCHIVE)
    # Exporters must regenerate at the new location, not recreate retired sites.
    from tools.checks.check_repository_hygiene import MOVED_REVIEWS
    for tree in (ROOT / 'Java/src', ROOT / 'Java/tools'):
        for source in tree.rglob('*.java'):
            text = source.read_text(encoding='utf-8-sig')
            if any('../asset-review/' + name in text for name in MOVED_REVIEWS):
                errors.append(f'{source.relative_to(ROOT)}: retired review output path')
    if errors:
        print('\n'.join(errors))
        return 1
    print('PASS: review HTML links and archive manifests/checksums.')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
