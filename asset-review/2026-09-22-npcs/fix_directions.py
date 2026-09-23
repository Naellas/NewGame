"""Correct mislabeled directions by reusing byte-exact original artwork."""
from pathlib import Path
import hashlib, json, shutil

r = Path('asset-review/2026-09-22-npcs')
digest = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
names = ['elowen', 'garruk', 'kael', 'ren', 'torin', 'vexa', 'quartermaster']
items = []
for name in names:
    for direction, opposite in [('left', 'right'), ('right', 'left')]:
        for folder, suffix in [('', ''), ('animations/', '_walk_anim')]:
            target = Path(f'Java/assets/npcs/{folder}npc_{name}_model_{direction}{suffix}.png')
            source = r/'before'/f'Java/assets/npcs/{folder}npc_{name}_model_{opposite}{suffix}.png'
            before = r/'before'/target
            assert digest(target) in (digest(before), digest(source)), f'Concurrent change: {target}'
            if suffix:
                assert (r/'before'/target.with_suffix('.frames')).read_text() == source.with_suffix('.frames').read_text()
            items.append(dict(target=target.as_posix(), source=source.as_posix(), before_sha256=digest(before), after_sha256=digest(source)))
for item in items: shutil.copy2(item['source'], item['target'])
(r/'direction-manifest.json').write_text(json.dumps(items, indent=2))
print('Corrected standing and walking directions for', len(names), 'NPCs:', ', '.join(names))
