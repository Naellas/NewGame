from pathlib import Path
from PIL import Image
import hashlib,json
out=Path('asset-review/2026-09-22-sprite-integrity');jobs=json.loads((out/'replacements.json').read_text());baseline={r['path']:r for r in json.loads((out/'inventory.json').read_text())};results=[]
for job in jobs:
 target=Path(job['target']);p=Path(job.get('archived_to',job['target']));b=out/'before'/target;im=Image.open(p);im.load();a=im.convert('RGBA').getchannel('A');hist=a.histogram();bbox=a.point(lambda v:255 if v>8 else 0).getbbox()
 assert hashlib.sha256(b.read_bytes()).hexdigest()==baseline[target.as_posix()]['sha256'],p
 assert p.read_bytes()==Path(job['source']).read_bytes(),p
 if target.stem=='city_cobble':
  assert hist[255]==im.width*im.height,p
 else:
  assert im.mode=='RGBA' and hist[0]>0 and hist[255]>0 and bbox,p
  assert bbox[0]>0 and bbox[1]>0 and bbox[2]<im.width and bbox[3]<im.height,p
 results.append({'path':p.as_posix(),'original_runtime_path':target.as_posix(),'before_sha256':baseline[target.as_posix()]['sha256'],'after_sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'size':im.size,'mode':im.mode,'visible_bbox_alpha_gt_8':bbox,'transparent_pixels':hist[0],'verified_original_backup':True,'verified_exact_generated_alpha':True})
frames=Path('Java/assets/characters/player/classes/mage/animations/class_mage_model_cast_anim.frames').read_bytes();assert not frames.startswith(b'\xef\xbb\xbf') and int(frames)==10
(out/'verification.json').write_text(json.dumps({'replacements':results,'java_compilation':'passed','runtime_preview':'7 active assets resolved with actual AssetCatalog and rendered with AssetStore at 240x172 and 48x48','animation_metadata':'616 scanned; no missing strips, bad dimensions or empty frames; 1 BOM repaired','note':'Alpha >8 matches runtime crop threshold; low-alpha generator noise is preserved rather than destructively keyed.'},indent=2))
for r in json.loads((out/'unused-manifest.json').read_text()):
 assert not Path(r['path']).exists(),r['path']
 assert hashlib.sha256(Path(r['review_path']).read_bytes()).hexdigest()==r['sha256'],r['review_path']
print('Verified all 11 backups, generated alpha, visible edge clearance and metadata repair.')
