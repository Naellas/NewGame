"""Package imagegen artwork into the existing NPC asset format; no color-keying or repainting."""
from pathlib import Path
from PIL import Image, ImageDraw
import numpy as np
import hashlib, json, re, shutil, sys

ROOT = Path('asset-review/2026-09-22-npcs')
BASE = {x['path']: x for x in json.loads((ROOT / 'full-inventory.json').read_text())}
digest = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
manifest = []
installed = {x['target']: x['after_sha256'] for x in json.loads((ROOT/'repair-manifest.json').read_text())} if (ROOT/'repair-manifest.json').exists() else {}

def output(image, target):
    path = ROOT / 'candidates' / target
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)
    manifest.append({'target': target, 'candidate': path.as_posix(), 'before_sha256': BASE[target]['sha256'], 'after_sha256': digest(path)})

def metadata(text, target):
    path = ROOT / 'candidates' / target
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text)
    manifest.append({'target': target, 'candidate': path.as_posix(), 'before_sha256': BASE[target]['sha256'], 'after_sha256': digest(path)})

jobs = json.loads((ROOT / 'generation.json').read_text())
for job in jobs:
    source = Path(re.search(r' as (.+?\.png) by default', job['hint']).group(1))
    master = ROOT / 'masters' / (job['name'] + '.png')
    master.parent.mkdir(exist_ok=True)
    shutil.copy2(source, master)
    sheet = Image.open(source).convert('RGBA')
    rows = job.get('rows', 4)
    columns = job.get('columns', 4)
    # Generated layout spacing can drift. Split only through actual transparent
    # gutters so a nominal grid boundary cannot remove shoes or equipment.
    alpha = np.array(sheet.getchannel('A'))
    def gutters(axis, count):
        occupied = (alpha > 8).any(axis=axis)
        length = len(occupied)
        cuts = [0]
        for i in range(1, count):
            ideal = round(i*length/count)
            radius = round(length/count*0.22)
            choices = [v for v in range(max(1, ideal-radius), min(length-1, ideal+radius)) if not occupied[v-1:v+2].any()]
            assert choices, (job['name'], axis, i, 'no transparent gutter')
            cuts.append(min(choices, key=lambda v: abs(v-ideal)))
        return cuts + [length]
    xs, ys = gutters(0, columns), gutters(1, rows)
    cells = []
    for row in range(rows):
        for col in range(columns):
            cell = sheet.crop((xs[col], ys[row], xs[col+1], ys[row+1]))
            box = cell.getchannel('A').point(lambda a: 255 if a > 8 else 0).getbbox()
            assert box, (job['name'], row, col, 'empty cell')
            assert box[0] > 0 and box[1] > 0 and box[2] < cell.width and box[3] < cell.height, (job['name'], row, col, 'clipped cell', box, cell.size)
            cells.append(cell.crop(box))
    # Reusing a neutral passing pose in both halves of a walk cycle keeps
    # handed equipment fixed without repainting or mirroring character art.
    for target, source_cell in job.get('frame_overrides', {}).items():
        tr, tc = map(int, target.split(':'))
        sr, sc = source_cell
        cells[tr*columns+tc] = cells[sr*columns+sc].copy()
    scale = min(88 / max(c.width for c in cells), 120 / max(c.height for c in cells))
    frames = []
    for cell in cells:
        cell = cell.resize((round(cell.width*scale), round(cell.height*scale)), Image.Resampling.LANCZOS)
        frame = Image.new('RGBA', (96, 128))
        frame.alpha_composite(cell, ((96-cell.width)//2, 124-cell.height))
        frames.append(frame)
    stem = 'npc_' + job.get('character', job['name']) + '_model'
    for row in range(rows):
        direction = ['down', 'left', 'right', 'up'][row]
        action = job.get('action', 'walk')
        strip = Image.new('RGBA', (96*columns, 128))
        for col in range(columns): strip.alpha_composite(frames[row*columns+col], (col*96, 0))
        output(strip, f'Java/assets/npcs/animations/{stem}_{direction}_{action}_anim.png')
        metadata(f'{columns}\n', f'Java/assets/npcs/animations/{stem}_{direction}_{action}_anim.frames')
        if action == 'walk': output(frames[row*columns], f'Java/assets/npcs/{stem}_{direction}.png')
    if job.get('action', 'walk') == 'walk':
        output(frames[0], f'Java/assets/npcs/{stem}.png')
        portrait = Image.new('RGBA', (96, 96))
        figure = frames[0].crop(frames[0].getbbox())
        figure.thumbnail((88, 88), Image.Resampling.LANCZOS)
        portrait.alpha_composite(figure, ((96-figure.width)//2, (96-figure.height)//2))
        output(portrait, f'Java/assets/npcs/{stem.removesuffix("_model")}.png')

(ROOT / 'candidate-manifest.json').write_text(json.dumps(manifest, indent=2))
print('Packaged', len(jobs), 'generated sheets into', len(manifest), 'candidate assets and metadata files.')

if '--install' in sys.argv:
    for item in manifest:
        p = Path(item['target'])
        assert digest(ROOT/'before'/p) == item['before_sha256']
        assert digest(p) in (item['before_sha256'], item['after_sha256'], installed.get(item['target'])), f'Concurrent change: {p}'
    for item in manifest: shutil.copy2(item['candidate'], item['target'])
    for master in (ROOT/'masters').glob('*.png'):
        p = Path('Java/assets/source/npc-refresh-2026-09-22')/master.name
        p.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(master, p)
    (ROOT/'repair-manifest.json').write_text(json.dumps(manifest, indent=2))
    print('Installed verified candidates; originals preserved.')
