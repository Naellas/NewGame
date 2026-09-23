from pathlib import Path
from PIL import Image,ImageDraw
from datetime import datetime
import hashlib,json,re
out=Path('asset-review/2026-09-22-latest-buildings');root=Path('Java/assets')
texts={p.as_posix():p.read_text(encoding='utf-8-sig',errors='replace') for p in Path('Java/src/main/java').rglob('*.java') if 'diagnostics' not in p.parts and not p.stem.endswith('Test')}
groups={'regional':['city/regional','city/folklore','city/landmarks'],'overworld':['city/overworld','locations'],'buildings':['city/buildings'],'walls':['city/walls']}
rows=[]
for group,folders in groups.items():
 items=[]
 for folder in folders:
  for p in (root/folder).rglob('*.png'):
   refs=[name for name,txt in texts.items() if '"'+p.stem+'"' in txt]
   if group=='overworld' and 'imagegen' not in p.stem:continue
   im=Image.open(p).convert('RGBA');a=im.getchannel('A');box=a.point(lambda v:255 if v>8 else 0).getbbox()
   r={'path':p.as_posix(),'group':group,'modified':datetime.fromtimestamp(p.stat().st_mtime).isoformat(),'size':im.size,'bbox':box,'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'direct_refs':refs,'alpha_extrema':a.getextrema(),'edge_contact':bool(box and (box[0]==0 or box[1]==0 or box[2]==im.width or box[3]==im.height))}
   rows.append(r);items.append(r)
 items.sort(key=lambda r:r['modified'],reverse=True)
 for k in range(0,len(items),25):
  part=items[k:k+25];sheet=Image.new('RGB',(1250,270*((len(part)+4)//5)),(66,72,74));d=ImageDraw.Draw(sheet)
  for i,r in enumerate(part):
   im=Image.open(r['path']).convert('RGBA');im.thumbnail((240,220),Image.Resampling.LANCZOS);x=i%5*250;y=i//5*270;sheet.paste(im,(x+(250-im.width)//2,y),im);name=Path(r['path']).stem
   d.text((x+4,y+221),name[:34],fill='white');d.text((x+4,y+234),name[34:68],fill='white');d.text((x+4,y+250),r['modified'][:10]+(' direct' if r['direct_refs'] else ' dynamic?'),fill='white')
  sheet.save(out/f'{group}-{k//25+1}.png')
(out/'inventory.json').write_text(json.dumps(rows,indent=2));print('Assets:',len(rows));print('Groups:',{g:sum(r['group']==g for r in rows)for g in groups});print('Edge contact:',[r['path'] for r in rows if r['edge_contact']])
