from pathlib import Path
from PIL import Image, ImageDraw
import hashlib, json

r=Path('asset-review/2026-09-22-npcs')
base=json.loads((r/'full-inventory.json').read_text())
rep=json.loads((r/'repair-manifest.json').read_text())
directions=json.loads((r/'direction-manifest.json').read_text())
expected={i['target']:i['after_sha256'] for i in rep+directions}
digest=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
changed=[]; errors=[]; resized=[]
for item in base:
    p=Path(item['path']);before=r/'before'/p
    if digest(before)!=item['sha256']: errors.append('archive hash: '+str(p))
    actual=digest(p)
    if actual!=expected.get(item['path'],item['sha256']): errors.append('live hash: '+str(p))
    if actual!=item['sha256']: changed.append(item['path'])
    if p.suffix=='.png' and Image.open(p).size!=Image.open(before).size:
        # Work strips now share the 96x128 cell format of the walking sprites.
        if p.stem not in ('npc_blacksmith_model_down_hammer_anim','npc_merchant_model_down_inspect_anim'):
            errors.append('unexpected dimensions: '+str(p))
        else: resized.append({'path':p.as_posix(),'before':Image.open(before).size,'after':Image.open(p).size})
    if p.suffix=='.frames' and int(p.read_text())!=int(before.read_text()): errors.append('frame count changed: '+str(p))
for item in rep:
    p=Path(item['target'])
    if p.suffix!='.png': continue
    im=Image.open(p).convert('RGBA')
    count=int(p.with_suffix('.frames').read_text()) if p.stem.endswith('_anim') else 1
    for i in range(count):
        f=im.crop((i*im.width//count,0,(i+1)*im.width//count,im.height))
        b=f.getchannel('A').point(lambda a:255 if a>8 else 0).getbbox()
        if not b or min(b[:2])<1 or b[2]>=f.width or b[3]>=f.height: errors.append('frame bounds: '+str(p)+':'+str(i))
summary=dict(refreshed_characters=6,direction_corrected_characters=7,baseline_files=len(base),changed_pngs=sum(p.endswith('.png') for p in changed),changed_metadata=sum(not p.endswith('.png') for p in changed),unchanged_files=len(base)-len(changed),archived_originals_verified=len(base),normalized_work_strips=resized,errors=errors)
(r/'verification.json').write_text(json.dumps(summary,indent=2))
print(json.dumps(summary,indent=2));assert not errors
out=Image.new('RGB',(720,6*150),(234,227,211));d=ImageDraw.Draw(out)
for row,n in enumerate(['bartender','blacksmith','citizen_man','citizen_woman','merchant','orin']):
    d.text((4,row*150+4),n,fill=(20,20,20))
    for col,dr in enumerate(['down','left','right','up']):
        im=Image.open(f'Java/assets/npcs/npc_{n}_model_{dr}.png').convert('RGBA')
        out.paste(im,(col*180+38,row*150+20),im)
out.save(r/'light-background-check.png')
