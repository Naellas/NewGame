import json, re, hashlib, shutil
from pathlib import Path
from PIL import Image
import numpy as np
root=Path.cwd(); review=root/'asset-review/2026-09-22-latest-buildings'
jobs=json.loads((review/'generation.json').read_text())
corrections={j['key']:j for j in json.loads((review/'corrections.json').read_text())}
final=review/'sun-final.json'
if final.exists(): corrections['sun_gate_horizontal']=json.loads(final.read_text())
baseline={j['path']:j for j in json.loads((review/'inventory.json').read_text())}
manifest=[]
for j in jobs:
    if j['key']=='sun_gate_horizontal' and not final.exists(): continue
    chosen=corrections.get(j['key'],j)
    src=Path(re.search(r' as (.+?\.png) by default',chosen['hint']).group(1))
    target=root/j['target']; backup=review/'before'/j['target']
    digest=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
    assert digest(backup)==baseline[j['target']]['sha256']
    assert digest(target) in (digest(backup),digest(src)), f'Concurrent edit: {target}'
    im=Image.open(src).convert('RGBA'); a=np.array(im)[:,:,3]
    ys,xs=np.where(a>8); bbox=[int(xs.min()),int(ys.min()),int(xs.max()+1),int(ys.max()+1)]
    assert a[0,0]==0 and a[-1,-1]==0, j['key']
    assert bbox[0]>0 and bbox[1]>0 and bbox[2]<im.width and bbox[3]<im.height, (j['key'],bbox)
    master=root/'Java/assets/source/latest-building-integrity-2026-09-22'/target.name
    master.parent.mkdir(parents=True,exist_ok=True)
    shutil.copy2(src,master);shutil.copy2(src,target)
    manifest.append(dict(key=j['key'],target=j['target'],before_sha256=digest(backup),after_sha256=digest(target),source=str(src),size=im.size,visible_bbox=bbox,opaque_pixels=int((a==255).sum()),transparent_pixels=int((a==0).sum())))
(review/'repair-manifest.json').write_text(json.dumps(manifest,indent=2))
print(f'Imported {len(manifest)} repairs with verified backups and transparent margins.')
