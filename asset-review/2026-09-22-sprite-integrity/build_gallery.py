from pathlib import Path
import json,html
out=Path('asset-review/2026-09-22-sprite-integrity');rows=json.loads((out/'inventory.json').read_text())
reviewed={r['path'] for r in rows[:285]}
archived={r['path']:r for r in json.loads((out/'unused-manifest.json').read_text())}
repairs={r['target']:r for r in json.loads((out/'replacements.json').read_text())}
usage={r['path']:r for r in json.loads((out/'usage.json').read_text())}
issues={
'city_roof_detail':'Neighboring roof fragment included; regenerated.',
'city_house_front':'Ground floor missing; regenerated and installed.',
'city_building_house_wide':'Same cropped roof as city_house_front; regenerated and installed.',
'city_wall_gate':'Half of arch missing; regenerated and installed.',
'city_building_town_manor':'Two unrelated buildings included below manor; regenerated and installed.',
'city_building_stone_hall':'Two unrelated buildings included below hall; regenerated and installed.',
'city_prop_source_crate_tall':'Wooden lower panel removed by cutout; regenerated and installed.',
'village_prop_well':'White matte inside supports and bottom crop; regenerated and installed.',
'city_prop_banner_blue':'Stray pixels from adjacent sheet content; needs repair.',
'city_prop_barrel_stack':'Filename describes barrels but artwork depicts a crate; needs usage/source review.',
'city_prop_crate':'Filename describes crate but artwork depicts barrel; needs usage/source review.',
'city_plaza_round':'Dark sheet border and neighboring snow at bottom; needs terrain-specific repair.',
'city_road_snow':'Dark sheet gutter at left edge; needs terrain-specific repair.',
'orc':'Detached neighboring fragment on left; needs repair.'}
for r in rows:
 s=Path(r['path']).stem
 if s in issues:r['visual_review']=issues[s]
 elif s.startswith('deco_soft_') or s in ('deco_reeds','deco_flowers','deco_grass_clump','deco_bush','deco_water_cattails'):
  r['visual_review']='May 26 vegetation/stone family: visible magenta or blue fringe in contact-sheet review; individual/source inspection needed.'
 elif r['path'] in reviewed:r['visual_review']='Contact-sheet inspected; no individual-pixel clearance implied.'
 else:r['visual_review']='Not visually reviewed yet.'
 if 'generated_source' in s:r['visual_review']='Source reference stored outside source folder; intentional opaque color key, not a runtime cutout repair.'
for r in rows:
 r['usage']=usage.get(r['path'],{}).get('status','unknown')
 if r['path'] in repairs:r['visual_review']='Regenerated; original preserved in before/. '+('Archived (no gameplay reference).' if r['path'] in archived else 'Active replacement; checked through game renderer.')
 if r['path'] in archived:
  r['review_path']=archived[r['path']]['review_path']
  r['visual_review']='ARCHIVED: '+archived[r['path']]['reason']
(out/'review-queue.json').write_text(json.dumps(rows,indent=2))
page='''<!doctype html><html lang="en"><meta charset="utf-8"><title>Sprite integrity — oldest first</title><style>
body{background:#20242a;color:#eee;font:15px system-ui;margin:24px}header{position:sticky;top:0;background:#20242a;padding:12px;z-index:2}input,select,button{padding:8px;margin:5px}main{display:grid;grid-template-columns:repeat(auto-fill,minmax(260px,1fr));gap:12px}article{background:#30363e;padding:12px;overflow-wrap:anywhere}figure{margin:0; height:220px;display:flex;align-items:center;justify-content:center;background:repeating-conic-gradient(#737373 0% 25%,#aaa 0% 50%) 50% /20px 20px}img{max-width:100%;max-height:210px;image-rendering:pixelated}small{color:#ccd}a{color:#addcff}.status{min-height:64px}</style><header><h1>Sprite integrity — oldest first</h1><p>2,576 PNGs decoded; no empty images. Age uses pre-repair filesystem modification time, not guaranteed creation time. Git history begins May 26. First 285 entries reviewed in contact sheets. 42 unused assets archived. Originals for every replacement are in before/. Flags are triage, not proof of corruption.</p><input id="search" placeholder="Filter name or finding" aria-label="Filter name or finding"><select id="scope" aria-label="Review scope"><option value="active">Gameplay references / dynamic families</option><option value="all">All assets</option><option value="archived">Archived</option><option value="old">Oldest 285</option><option value="issues">Flagged for review</option></select><button id="bg">Change preview background</button><span id="count"></span></header><main>'''
for i,r in enumerate(rows):
 p=r.get('review_path',r['path']);msg=r['visual_review']; flagged=msg not in ('Contact-sheet inspected; no individual-pixel clearance implied.','Not visually reviewed yet.')
 page+=f'<article data-active="{str(r['usage'] in ('direct','possible-dynamic','animation-family','saved/config/export') and r['path'] not in archived).lower()}" data-archived="{str(r['path'] in archived).lower()}" data-old="{str(i<285).lower()}" data-issue="{str(flagged).lower()}"><figure><img loading="lazy" src="../../{html.escape(p)}" alt="{html.escape(Path(p).stem)}"></figure><h3>{html.escape(Path(p).stem)}</h3><small>{r["modified"][:19]} | {r["size"][0]} × {r["size"][1]}</small><p class="status">{html.escape(msg)}</p><a href="../../{html.escape(p)}">Open asset</a></article>'
page+='''</main><script>const search=document.getElementById('search'),scope=document.getElementById('scope'),count=document.getElementById('count'),bg=document.getElementById('bg');const cards=[...document.querySelectorAll('article')];function filter(){let n=0;for(const c of cards){const ok=c.textContent.toLowerCase().includes(search.value.toLowerCase())&&(scope.value==='all'||scope.value==='active'&&c.dataset.active==='true'||scope.value==='archived'&&c.dataset.archived==='true'||scope.value==='old'&&c.dataset.old==='true'||scope.value==='issues'&&c.dataset.issue==='true');c.hidden=!ok;if(ok)n++}count.textContent=n+' assets'}search.oninput=scope.onchange=filter;let b=0;bg.onclick=()=>{b=(b+1)%4;document.querySelectorAll('figure').forEach(f=>f.style.background=['','#eee','#17241b','#554360'][b])};filter();</script></html>'''
(out/'gallery.html').write_text(page,encoding='utf-8')
print('Wrote age-ordered gallery and review queue')

