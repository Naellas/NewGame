from pathlib import Path
from PIL import Image,ImageDraw
import json,hashlib,re,datetime
root=Path.cwd(); out=root/'asset-review/2026-09-22-monsters';out.mkdir(exist_ok=True)
source=(root/'Java/src/main/java/com/alderfall/game/content/GameData.java').read_text()
used=set(re.findall(r'monster\("[^"]+",\s*"[^"]+",\s*"[^"]+",\s*"([^"]+)"',source))
rows=[]
for p in sorted((root/'Java/assets/monsters').glob('*.png'),key=lambda p:p.stat().st_mtime):
 im=Image.open(p).convert('RGBA');a=im.getchannel('A');bbox=a.point(lambda v:255 if v>8 else 0).getbbox()
 rows.append(dict(path=p.relative_to(root).as_posix(),stem=p.stem,used=p.stem in used,modified=datetime.datetime.fromtimestamp(p.stat().st_mtime).isoformat(),sha256=hashlib.sha256(p.read_bytes()).hexdigest(),size=im.size,bbox=bbox))
(out/'inventory.json').write_text(json.dumps(rows,indent=2))
for start in range(0,len(rows),16):
 sheet=Image.new('RGB',(1200,1000),(52,58,66));d=ImageDraw.Draw(sheet)
 for i,row in enumerate(rows[start:start+16]):
  im=Image.open(root/row['path']).convert('RGBA');im.thumbnail((280,210));x=i%4*300;y=i//4*250
  sheet.paste(im,(x+(300-im.width)//2,y+25),im);d.text((x+5,y+5),row['stem']+(' [used]' if row['used'] else ' [?]'),fill='white')
 sheet.save(out/f'monsters-{start//16+1}.png')
print(len(rows),'sprites;',len(used),'unique catalog assets;',sum(r['used'] for r in rows),'matched')
