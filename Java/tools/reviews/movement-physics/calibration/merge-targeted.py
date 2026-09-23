"""Merge an actor-limited rerun after changes confined to those actors."""
from pathlib import Path
import csv
import json
import shutil
ROOT=Path(__file__).resolve().parent

def read(path):
    with path.open(encoding='utf-8',newline='') as f:
        return list(csv.DictReader(f))

def merge(filename,keys):
    old=read(ROOT/filename);new=read(ROOT/'targeted'/filename)
    key=lambda r:tuple(r[k] for k in keys)
    replacements={key(r):r for r in new}
    assert set(replacements)<=set(map(key,old)), 'Targeted run introduced unknown cases'
    result=[replacements.get(key(r),r) for r in old]
    with (ROOT/filename).open('w',encoding='utf-8',newline='') as f:
        writer=csv.DictWriter(f,fieldnames=list(old[0]));writer.writeheader();writer.writerows(result)
    return result,new

rows,changed=merge('checks.csv',['actor','direction'])
profiles,_=merge('measured-profiles.csv',['sprite'])
for actor in {r['actor'] for r in changed}:
    assert actor.replace('_','').isalnum()
    shutil.copyfile(ROOT/'targeted'/'sheets'/f'{actor}.png',ROOT/'sheets'/f'{actor}.png')
data=[dict(actor=r['actor'],direction=r['direction'],source=r['source'],status=r['status'],physics=r['physics_status'],fallback=r['source']!=r['actor']+'_model_'+r['direction'],flags=r['flags']) for r in rows]
summary=dict(actors=len({r['actor'] for r in rows}),cases=len(rows),sources=len(profiles),poses=sum(int(r['poses_checked']) for r in rows),geometryPassed=sum(r['status']=='checks-pass' for r in rows),physicsPassed=sum(r['physics_status']=='checks-pass' for r in rows),fallbacks=sum(r['fallback'] for r in data))
(ROOT/'results.js').write_text('window.calibrationResults='+json.dumps(data)+';\nwindow.calibrationSummary='+json.dumps(summary)+';',encoding='utf-8')
print(summary)
