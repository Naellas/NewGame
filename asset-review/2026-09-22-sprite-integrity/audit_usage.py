from pathlib import Path
import re,json
out=Path('asset-review/2026-09-22-sprite-integrity');rows=json.loads((out/'inventory.json').read_text())
java=[p for p in Path('Java/src/main/java').rglob('*.java') if 'diagnostics' not in p.parts and not p.stem.endswith('Test')]
texts={p.as_posix():p.read_text(encoding='utf-8-sig',errors='replace') for p in java}
literals={}
for p,t in texts.items():
 for s in re.findall(r'"([^"\n]+)"',t):literals.setdefault(s,[]).append(p)
extra=[]
for root in ['Java/config','Java/saves','saves','Java/exports']:
 for p in Path(root).rglob('*'):
  if p.is_file() and p.suffix.lower() in ('.json','.txt','.properties','.map','.save') and 'asset-cleanup' not in p.parts:
   extra.append((p.as_posix(),p.read_text(encoding='utf-8-sig',errors='replace')))
result=[]
for r in rows:
 name=Path(r['path']).stem
 direct=literals.get(name,[])+literals.get(name+'.png',[])
 persisted=[p for p,t in extra if name in t]
 # Keep dynamic animation families; runtime constructs these names from actor and action.
 family= ('/animations/' in r['path'] or name.endswith('_anim'))
 dynamic=[s for s in literals if len(s)>=7 and s.endswith('_') and name.startswith(s) and s not in ('city_','village_','location_','deco_','player_village_')]
 status='direct' if direct else 'saved/config/export' if persisted else 'animation-family' if family else 'possible-dynamic' if dynamic else 'no-reference-found'
 result.append({'path':r['path'],'modified':r['modified'],'status':status,'direct':direct[:5],'persisted':persisted[:3],'dynamic':dynamic[:8]})
(out/'usage.json').write_text(json.dumps(result,indent=2))
print('Oldest no-reference candidates:')
for r in result[:285]:
 if r['status']=='no-reference-found':print(r['path'])
print('Counts:',{s:sum(r['status']==s for r in result) for s in set(r['status'] for r in result)})
