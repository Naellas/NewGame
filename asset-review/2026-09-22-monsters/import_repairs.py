from pathlib import Path
from PIL import Image
import json,re,hashlib,shutil
import numpy as np
r=Path('asset-review/2026-09-22-monsters');jobs=json.loads((r/'generation.json').read_text());baseline={j['path']:j for j in json.loads((r/'inventory.json').read_text())}
if (r/'extra-generation.json').exists():jobs.extend(json.loads((r/'extra-generation.json').read_text()))
for file in ['family-generation.json','green-generation.json']:
 if (r/file).exists():jobs.extend(json.loads((r/file).read_text()))
styles={j['name']:j for j in json.loads((r/'style-generation.json').read_text())} if (r/'style-generation.json').exists() else {}
result=[]
digest=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
for j in jobs:
 initial=Path(re.search(r' as (.+?\.png) by default',j['hint']).group(1))
 chosen=styles.get(j['name'],j)
 src=Path(re.search(r' as (.+?\.png) by default',chosen['hint']).group(1));p=Path(j['target']);b=r/'before'/p
 assert digest(b)==baseline[j['target']]['sha256']
 assert digest(p) in (digest(src),digest(b),digest(initial)),f'Concurrent edit: {p}'
 im=Image.open(src).convert('RGBA');a=np.array(im)[:,:,3];ys,xs=np.where(a>8);bbox=[int(xs.min()),int(ys.min()),int(xs.max()+1),int(ys.max()+1)]
 assert a[0,0]==0 and a[-1,-1]==0,j['name']
 assert bbox[0]>0 and bbox[1]>0 and bbox[2]<im.width and bbox[3]<im.height,(j['name'],bbox,im.size)
 master=Path('Java/assets/source/monster-integrity-2026-09-22')/p.name;master.parent.mkdir(parents=True,exist_ok=True)
 shutil.copy2(src,master);shutil.copy2(src,p)
 result.append(dict(target=j['target'],before_sha256=digest(b),after_sha256=digest(p),size=im.size,visible_bbox=bbox,source=str(src)))
(r/'repair-manifest.json').write_text(json.dumps(result,indent=2));print('Imported',len(result),'verified replacements.')
