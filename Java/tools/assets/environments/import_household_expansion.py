"""Extract the supplied September household sheets without synthesizing artwork.

All candidates are named in the review archive; only curated, nonredundant assets
enter the runtime catalog. Coordinates refer to the original 1448 x 1086 sheets.
Run from any directory. --install applies the reviewed selection.
"""
from __future__ import annotations

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[3]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT

import argparse
import html
import json
import shutil
from pathlib import Path

from PIL import Image, ImageDraw
from tools.assets.shared.universal_cutout import alpha_components, component_box, component_gap

ROOT = JAVA_ROOT
SOURCE = ROOT / "assets/source/household-expansion"
REVIEW = ROOT.parent / "asset-review/household-expansion"
OUT = ROOT / "assets/environments/interiors/props/household"

# Rows consist of explicit boundaries; unusual tall/offset objects use boxes below.
ROWS = {
    "household": [
        (40, 222, [22,165,324,628,731,820,923,1027,1252,1420],
         "round_table square_table long_table chair_wood stool chair_red chair_green sofa bench"),
        (224, 437, [22,150,323,484,660,834,1023,1211,1425],
         "bedside_cabinet chest_drawers wardrobe crockery_cupboard bookshelf wall_books low_cupboard drinks_counter"),
        (442, 620, [22,298,507,659,766,892,1017,1216,1324,1423],
         "rug_red rug_green rug_blue rug_runner leafy_plant flower_planter wall_herbs hanging_lantern standing_lantern"),
        (614, 784, [20,180,309,460,678,834,1028,1245,1422],
         "fireplace firewood_basket logs provision_baskets washstand plate_rack prep_table iron_chest"),
        (787, 956, [22,283,405,538,725,852,979,1082,1245,1423],
         "room_screen coat_stand wall_banner wall_landscape wall_flowers wall_mirror wall_clock wall_antlers wall_candle_shelf"),
        (949, 1070, [22,160,275,384,458,536,617,716,821,924,999,1081,1208,1305,1424],
         "tabletop_books tabletop_scrolls tabletop_ink tabletop_candle tabletop_tankard tabletop_jug tabletop_wine tabletop_apples tabletop_fruit sack_grain sack_tied crates barrel crate"),
    ],
    "warehouse": [
        (10, 178, [20,121,295,491,678,801,917,1037,1155,1263,1430],
         "crate crates_stack crates_wide shipping_crate apples_crate cabbages_crate corn_crate potatoes_crate crate_tall pallet"),
        (179, 310, [16,96,224,330,478,583,739,817,927,1059,1186,1302,1435],
         "barrel barrels barrel_stand cask_stack cask_cradle sacks_stack sack sacks_pair bales_stack bale bales_small logs"),
        (314, 543, [17,203,408,603,777],
         "cask_shelf supplies_shelf tools_shelf pantry_shelf"),
        (314, 485, [778,955,1137,1288,1424],
         "wall_pantry wall_tools wall_parcels wall_shelf"),
        (551, 701, [17,297,584,835,1057],
         "packing_desk workbench merchant_counter welcome_counter"),
        (704, 843, [17,286,478,560,632,725,817,894,971,1064,1188,1289,1425],
         "wall_rope_lantern wall_dried_food wall_lantern standing_lantern basket_square basket_tall basket_apples basket_demijohn basket_jug chest_open iron_chest bottles_cabinet_lower"),
        (843, 1012, [22,247,394,469,563,653,746,836,927,1002,1099,1240,1303,1347,1427],
         "wood_screen pottery sack_small produce_crate fish_crate potato_crate stone_crate bottle_crate coal_crate grain_spill bench stool broom bucket"),
    ],
    "directional": [
        (12, 144, [22,98,180,262,348,435,512,591,674,785,866,946,1027,1118,1193,1264,1340,1427],
         "chair_wood_front_a chair_wood_front_b chair_wood_right chair_wood_left chair_red_front_a chair_red_front_b chair_red_right chair_red_left chair_green_front_a chair_green_front_b chair_green_right chair_green_left stool_a stool_b stool_c chair_low_right chair_low_left"),
        (149, 264, [22,220,305,541,622,751,842,929,1034,1206,1354,1430],
         "bench_h bench_v bench_back bench_back_v table_small table_square table_v table_round table_h table_slatted table_slatted_v"),
        (265, 440, [22,168,268,419,514,669,769,870,1033,1135,1329,1430],
         "drawers_front drawers_side wardrobe_front wardrobe_side bookshelf_front bookshelf_side cupboard_side crockery_bookshelf crockery_side provisions_shelf provisions_side"),
        (445, 560, [22,243,312,509,606,818,914,1114,1205,1331,1430],
         "bar_front bar_side workbench_front workbench_side reading_counter reading_counter_side bench_low bench_low_side linen_shelf linen_shelf_side"),
        (563, 690, [22,103,187,275,386,488,576,663,758,866,1004,1190,1331,1430],
         "barrel_a barrel_b cask_front cask_side barrel_pair barrel_c crate_a crate_b crate_c crates_a crates_b crates_c crate_side"),
        (695, 817, [22,115,208,320,415,521,655,845,967,1050,1190,1331,1430],
         "sack_a sack_b sack_pair sack_round bales_a bales_b bales_pair iron_chest_front iron_chest_side chest_front chest_open chest_side"),
        (823, 926, [22,133,247,349,466,604,686],
         "bin_front_a bin_front_b bin_side bin_corner bin_wide bin_front_c"),
        (925, 1058, [24,270,470,668],
         "handcart_side handcart_flat handcart_front"),
    ],
}
BOXES = {
    "warehouse": [
        ("handcart_flat", (855,434,1024,580)), ("handcart", (1025,474,1157,588)),
        ("ladder", (1168,423,1254,595)), ("stepladder", (1270,480,1383,605)),
        ("stool_square", (1064,609,1126,700)), ("drawers", (1141,602,1283,708)),
        ("bottles_cabinet", (1288,601,1421,837)),
    ],
    "directional": [
        ("ladder_left", (686,816,812,1051)), ("ladder_right", (813,816,927,1051)),
        ("ladder_front", (935,824,1033,1051)), ("bench_short", (1034,840,1152,924)),
        ("bench_side", (1158,835,1206,1051)), ("bench_long", (1209,839,1352,924)),
        ("bench_end", (1360,827,1424,926)), ("side_table", (1045,930,1157,1031)),
        ("console_table", (1204,925,1353,1024)), ("console_side", (1360,928,1424,1035)),
    ],
}

# Tight overrides avoid neighboring sprites that overlap the broad row bands.
OVERRIDES = {
    'household/crates': (1082, 940, 1208, 1070),
    'household/iron_chest': (1245, 639, 1422, 784),
    'household/wall_antlers': (1082, 787, 1245, 942),
    'warehouse/wall_parcels': (1137, 314, 1288, 415),
    'warehouse/welcome_counter': (845, 589, 1057, 701),
    'warehouse/merchant_counter': (584, 522, 835, 701),
    'warehouse/broom': (1303, 839, 1347, 1012),
    'warehouse/coal_crate': (927, 875, 997, 973),
    'directional/sack_round': (326, 695, 415, 817),
}

# candidate -> existing identity. Preserve save IDs and editor entries.
REPLACE = {
    "household/bookshelf": "interior_bookshelf",
    "household/crates": "interior_crates",
    "household/low_cupboard": "interior_low_cupboard",
    "warehouse/barrels": "interior_barrels",
}

RETAIN = {
    'household/round_table': ('interior_round_table', 'At game scale the existing round table has a clearer rim and a broader usable tabletop; retain its silhouette.'),
    'household/leafy_plant': ('interior_floor_leafy_plant', 'The existing brighter leaf clusters stay more readable in dim rooms at one-tile scale.'),
    'household/flower_planter': ('interior_floor_flower_planter', 'Existing saturated flowers and patterned pot remain more distinct at game scale.'),
    'household/wall_herbs': ('interior_wall_herb_rack', 'Existing separated herb bunches read more clearly at the shared wall-decor scale.'),
    'household/chair_wood': ('interior_chair_south', 'Keep the coherent existing four-direction seating set; the supplied front views do not include a true rear view.'),
    'directional/chair_wood_right': ('interior_chair_west', 'Keep the complete existing seating set and its consistent table-facing orientation.'),
    'household/chair_red': ('interior_chair_south_alt', 'Keep the existing padded directional set instead of a near-duplicate single chair.'),
    'household/bench': ('interior_bench_h', 'Existing bench remains consistent with the modular dining groups and paired vertical bench.'),
    'directional/bench_v': ('interior_bench_v', 'Keep the paired modular bench orientations.'),
    'household/long_table': ('interior_table_h_middle', 'Existing connected table segments support expandable dining groups.'),
    'household/prep_table': ('interior_cooking_station', 'Existing preparation equipment remains clearer as a working kitchen station at game scale.'),
    'household/tabletop_candle': ('interior_tabletop_candle', 'Existing candle centerpiece is legible at small scale and already matches the light and dining composition.'),
    'household/rug_red': ('interior_rug_red', 'Keep the existing continuous tiled rug system, whose borders join beneath furniture.'),
    'household/rug_green': ('interior_rug_teal', 'Keep region-aware continuous rugs rather than introduce competing floor stamps.'),
    'household/rug_runner': ('interior_rug_runner', 'Existing runner remains consistent with the tiled carpets.'),
    'warehouse/packing_desk': ('interior_study_desk_h', 'Existing desk already supplies the writing function with readable paper and ink.'),
    'warehouse/sacks_stack': ('interior_grain_sacks_v', 'Retain the established grain-storage silhouette and layout footprint.'),
    'warehouse/merchant_counter': ('interior_shop_counter', 'Existing merchant counter already fills this role; avoid another equivalent editor entry.'),
}

# New semantic identity, category, placement, collision width/depth, draw height.
# Display height can exceed collision depth (e.g. tall cupboards against a wall).
SELECT = {
    "household/sofa": ("sofa_red", "Seating", "FLOOR", 2, 1, 1.2),
    "household/chair_green": ("armchair_green", "Seating", "FLOOR", 1, 1, 1.2),
    "household/stool": ("stool", "Seating", "FLOOR", 1, 1, .7),
    "household/chest_drawers": ("chest_of_drawers", "Storage", "SURFACE", 1, 1, 1.1),
    "household/wardrobe": ("wardrobe", "Storage", "FLOOR", 1, 1, 1.65),
    "household/crockery_cupboard": ("crockery_cupboard", "Storage", "FLOOR", 1, 1, 1.5),
    "household/wall_books": ("wall_books", "Wall-mounted", "WALL", 2, 1, 1),
    "household/fireplace": ("fireplace", "Workstations", "FLOOR", 1, 1, 1.3),
    "household/firewood_basket": ("firewood_basket", "Storage", "FLOOR", 1, 1, .8),
    "household/washstand": ("washstand", "Workstations", "FLOOR", 1, 1, 1.2),
    "household/plate_rack": ("plate_rack", "Storage", "FLOOR", 1, 1, 1),
    "household/room_screen": ("room_screen", "Misc", "FLOOR", 2, 1, 1.3),
    "household/coat_stand": ("coat_stand", "Misc", "FLOOR", 1, 1, 1.35),
    "household/wall_banner": ("wall_banner_lion", "Wall-mounted", "WALL", 1, 1, 1),
    "household/wall_landscape": ("wall_landscape", "Wall-mounted", "WALL", 2, 1, 1),
    "household/wall_flowers": ("wall_flower_painting", "Wall-mounted", "WALL", 1, 1, 1),
    "household/wall_mirror": ("wall_mirror", "Wall-mounted", "WALL", 1, 1, 1),
    "household/wall_clock": ("wall_clock", "Wall-mounted", "WALL", 1, 1, 1),
    "household/wall_antlers": ("wall_antlers", "Wall-mounted", "WALL", 1, 1, 1),
    "household/tabletop_books": ("tabletop_books", "Decorations", "TABLETOP", 1, 1, .45),
    "household/tabletop_scrolls": ("tabletop_scrolls", "Decorations", "TABLETOP", 1, 1, .4),
    "household/tabletop_ink": ("tabletop_inkwell", "Decorations", "TABLETOP", 1, 1, .45),
    "household/tabletop_tankard": ("tabletop_tankard", "Decorations", "TABLETOP", 1, 1, .35),
    "household/tabletop_jug": ("tabletop_jug", "Decorations", "TABLETOP", 1, 1, .45),
    "household/tabletop_wine": ("tabletop_wine", "Decorations", "TABLETOP", 1, 1, .5),
    "household/tabletop_fruit": ("tabletop_fruit", "Decorations", "TABLETOP", 1, 1, .4),
    "warehouse/apples_crate": ("produce_apples", "Storage", "FLOOR", 1, 1, .8),
    "warehouse/cabbages_crate": ("produce_cabbages", "Storage", "FLOOR", 1, 1, .8),
    "warehouse/corn_crate": ("produce_corn", "Storage", "FLOOR", 1, 1, .8),
    "warehouse/fish_crate": ("produce_fish", "Storage", "FLOOR", 1, 1, .8),
    "warehouse/potato_crate": ("produce_potatoes", "Storage", "FLOOR", 1, 1, .8),
    "warehouse/bottle_crate": ("bottle_crate", "Storage", "FLOOR", 1, 1, .8),
    "warehouse/coal_crate": ("coal_crate", "Storage", "FLOOR", 1, 1, .7),
    "warehouse/supplies_shelf": ("supplies_shelf", "Storage", "FLOOR", 1, 1, 1.5),
    "warehouse/tools_shelf": ("tools_shelf", "Storage", "FLOOR", 1, 1, 1.5),
    "warehouse/pantry_shelf": ("pantry_shelf", "Storage", "FLOOR", 1, 1, 1.5),
    "warehouse/wall_tools": ("wall_tools", "Wall-mounted", "WALL", 2, 1, 1),
    "warehouse/wall_parcels": ("wall_parcels", "Wall-mounted", "WALL", 2, 1, 1),
    "warehouse/pallet": ("pallet", "Storage", "FLOOR", 2, 1, .65),
    "warehouse/bales_stack": ("tied_bales", "Storage", "FLOOR", 1, 1, .85),
    "warehouse/pottery": ("pottery_cluster", "Storage", "FLOOR", 1, 1, 1),
    "warehouse/basket_square": ("basket_empty", "Storage", "FLOOR", 1, 1, .65),
    "warehouse/basket_demijohn": ("basket_demijohn", "Storage", "FLOOR", 1, 1, .8),
    "warehouse/bucket": ("bucket", "Misc", "FLOOR", 1, 1, .7),
    "warehouse/broom": ("broom", "Misc", "FLOOR", 1, 1, 1.3),
    "warehouse/stepladder": ("stepladder", "Workstations", "FLOOR", 1, 1, 1.2),
    "directional/cask_side": ("cask_on_side", "Storage", "FLOOR", 1, 1, .75),
    "directional/iron_chest_front": ("ironbound_chest", "Storage", "FLOOR", 1, 1, .9),
    "directional/bin_wide": ("open_storage_bin", "Storage", "FLOOR", 1, 1, .8),
    "directional/handcart_side": ("handcart", "Workstations", "FLOOR", 2, 1, 1),
    "directional/ladder_front": ("leaning_ladder", "Workstations", "FLOOR", 1, 1, 1.6),
}


def candidates():
    for sheet, rows in ROWS.items():
        for top, bottom, edges, names in rows:
            names = names.split()
            assert len(edges) == len(names) + 1, (sheet, names)
            for name, left, right in zip(names, edges, edges[1:]):
                # The cabinet belongs to one tall crop, not a second lower-half item.
                if name == "bottles_cabinet_lower":
                    continue
                yield sheet, name, (left, top, right, bottom)
        for name, box in BOXES.get(sheet, []):
            yield sheet, name, box


def extract(source, box, key):
    sprite = source.crop(box).convert("RGBA")
    # These objects interleave across rows; rectangular crops alone include
    # part of a neighbor even though the main object is completely inside.
    masks = {
        'warehouse/merchant_counter': (584, 522, 782, 552),
        'warehouse/handcart_flat': (855, 434, 944, 483),
        'warehouse/wall_tools': (955, 440, 1040, 485),
    }
    if key in masks:
        left,top,right,bottom=masks[key]
        sprite.paste((0,0,0,0),(left-box[0],top-box[1],right-box[0],bottom-box[1]))
    if source.mode != "RGBA":
        # The matte is near-neutral black. Use a conservative threshold, not the
        # broad 'dark' chroma key, which also erases dark brown timber and iron.
        pixels = sprite.get_flattened_data() if hasattr(sprite, 'get_flattened_data') else sprite.getdata()
        sprite.putdata([(0, 0, 0, 0) if max(r,g,b) <= 12 and max(r,g,b)-min(r,g,b) <= 6
                        else (r,g,b,a) for r,g,b,a in pixels])
    # Remove neighboring-object fragments while preserving interior islands
    # (flames, highlights) and intentionally separated groups such as wine + cup.
    components=alpha_components(sprite)
    main=max(components,key=len)
    main_box=component_box(main)
    grouped=any(word in key for word in ('tabletop_wine','provision_baskets','pottery','barrels','sacks_pair','grain_spill'))
    px=sprite.load()
    for component in components:
        box2=component_box(component)
        if component is main or component_gap(main_box,box2)<=4 or grouped:
            continue
        for x,y in component:px[x,y]=(0,0,0,0)
    bbox = sprite.getchannel("A").getbbox()
    if bbox is None:
        raise ValueError(f"Empty crop: {box}")
    trimmed = sprite.crop(bbox)
    padded = Image.new("RGBA", (trimmed.width+4, trimmed.height+4))
    padded.alpha_composite(trimmed, (2,2))
    return padded


def contact_sheet(entries, path):
    canvas = Image.new("RGB", (1000, ((len(entries)+5)//6)*155), "#343a40")
    draw = ImageDraw.Draw(canvas)
    for i,(name,im) in enumerate(entries):
        x,y=(i%6)*166,(i//6)*155
        thumb=im.copy();thumb.thumbnail((145,116),Image.Resampling.NEAREST)
        canvas.paste(thumb,(x+(166-thumb.width)//2,y+116-thumb.height),thumb)
        # Two short lines keep every unique filename visible.
        words=name.replace('interior_','').replace('_',' ').split()
        lines=['']
        for w in words:
            if len(lines[-1])+len(w)>23: lines.append('')
            lines[-1]+=(' ' if lines[-1] else '')+w
        draw.text((x+4,y+119),'\n'.join(lines),fill='white')
    canvas.save(path)


def game_scale(image, asset):
    """Comparable fitted game-scale samples, enlarged only for the review sheet."""
    width=96 if any(s in asset for s in ('bench_h','table_h','station','counter','cupboard','herb_rack','desk_h')) else 48
    height=96 if asset in ('interior_bench_v','interior_grain_sacks_v') else 65 if asset=='interior_bookshelf' else 48
    im=image.crop(image.getchannel('A').getbbox())
    scale=min(width/im.width,height/im.height)
    im=im.resize((max(1,round(im.width*scale)),max(1,round(im.height*scale))),Image.Resampling.NEAREST)
    canvas=Image.new('RGBA',(width,height));canvas.alpha_composite(im,((width-im.width)//2,height-im.height))
    return canvas.resize((width*2,height*2),Image.Resampling.NEAREST)


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--install',action='store_true')
    args=parser.parse_args()
    REVIEW.mkdir(parents=True,exist_ok=True)
    sheets={name:Image.open(SOURCE/(name+'.png')) for name in ROWS}
    manifest=[]; galleries={name:[] for name in ROWS}; comparisons=[]
    all_ids=set()
    for sheet,name,box in candidates():
        key=sheet+'/'+name
        assert key not in all_ids,key
        all_ids.add(key)
        box=OVERRIDES.get(key,box)
        im=extract(sheets[sheet],box,key)
        candidate=REVIEW/'candidates'/sheet/(name+'.png')
        candidate.parent.mkdir(parents=True,exist_ok=True);im.save(candidate)
        galleries[sheet].append((name,im))
        item={'candidate':key,'box':box,'size':im.size,'decision':'archive',
              'reason':'Overlapping style/orientation or less useful than the curated selection; not added to the editor.'}
        if key in REPLACE:
            asset=REPLACE[key]
            existing=list((ROOT/'assets/environments/interiors').rglob(asset+'.png'))
            assert len(existing)==1,(asset,existing)
            old=REVIEW/'before'/(asset+'.png');old.parent.mkdir(exist_ok=True)
            if not old.exists():shutil.copy2(existing[0],old)
            comparisons.extend([(asset+' BEFORE',game_scale(Image.open(old).convert('RGBA'),asset)),(asset+' AFTER',game_scale(im,asset))])
            item.update(decision='replace',asset=asset,path=str(existing[0].relative_to(ROOT)),
                        reason='Clearer front elevation and finer material detail; reuse the existing editor/save identity.')
            if args.install:im.save(existing[0])
        elif key in SELECT:
            suffix,category,placement,w,d,h=SELECT[key]
            asset='interior_'+suffix
            item.update(decision='add',asset=asset,label=suffix.replace('_',' ').title(),category=category,
                        placement=placement,width=w,depth=d,draw_height=h,
                        reason='Distinct furnishing, supply type, or activity not represented by the retained core furniture.')
            if args.install:
                OUT.mkdir(parents=True,exist_ok=True);im.save(OUT/(asset+'.png'))
        elif key in RETAIN:
            asset,reason=RETAIN[key]
            existing=list((ROOT/'assets/environments/interiors').rglob(asset+'.png'))
            assert len(existing)==1,(asset,existing)
            saved=REVIEW/'retained'/(asset+'.png');saved.parent.mkdir(exist_ok=True)
            shutil.copy2(existing[0],saved)
            comparisons.extend([(asset+' KEEP',game_scale(Image.open(saved).convert('RGBA'),asset)),(name+' UNUSED',game_scale(im,asset))])
            item.update(decision='keep existing',asset=asset,reason=reason)
        manifest.append(item)
    assert set(REPLACE)|set(SELECT) <= all_ids
    for name,entries in galleries.items(): contact_sheet(entries,REVIEW/(name+'-cutouts.png'))
    contact_sheet(comparisons,REVIEW/'comparison.png')
    (REVIEW/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
    cards=[]
    for item in manifest:
        key=item['candidate'];decision=item['decision']
        previous=''
        if decision in ('replace','keep existing'):
            folder='before' if decision=='replace' else 'retained'
            previous=f'<figure><img src="{folder}/{item["asset"]}.png"><figcaption>Existing</figcaption></figure>'
        cards.append(f'<article data-decision="{decision}"><h2>{html.escape(item.get("label",key))}</h2>'
                     f'<p class="badge">{decision}</p><div class="pair">{previous}<figure><img src="candidates/{key}.png">'
                     f'<figcaption>Supplied</figcaption></figure></div><p>{html.escape(item["reason"])}</p>'
                     f'<small>{html.escape(item.get("asset",key))}</small></article>')
    page='''<!doctype html><meta charset="utf-8"><title>Household furniture review</title>
<style>body{background:#23282e;color:#eee;font:16px system-ui;margin:32px}h1{margin-bottom:8px}
button,input{font:inherit;padding:8px;margin:4px;border-radius:5px}main{display:grid;grid-template-columns:repeat(auto-fill,minmax(310px,1fr));gap:18px}
article{background:#353c44;padding:18px;border-radius:8px}h2{font-size:17px}img{width:130px;height:130px;object-fit:contain;image-rendering:pixelated}
.pair{display:flex}figure{margin:6px}.badge{color:#aee7ae}small{overflow-wrap:anywhere;color:#bdc6ce}p{line-height:1.4}</style>
<h1>Household furniture review</h1><p>216 named cutouts. 51 additions and 4 replacements enter the game. Other candidates remain outside the editor palette.</p>
<p><a href="comparison.png" style="color:#aee7ae">Game-scale comparison sheet</a> Â· <a href="manifest.json" style="color:#aee7ae">Crop and decision manifest</a></p>
<p><input id="search" placeholder="Find furniture" aria-label="Find furniture"><button data-filter="all">All</button><button data-filter="add">Added</button>
<button data-filter="replace">Replaced</button><button data-filter="keep existing">Existing kept</button><button data-filter="archive">Archived</button></p><main>
'''+''.join(cards)+'''</main><script>let filter='all';function refresh(){document.querySelectorAll('article').forEach(c=>c.hidden=!(filter==='all'||c.dataset.decision===filter)||!c.textContent.toLowerCase().includes(document.querySelector('#search').value.toLowerCase()))}
document.querySelectorAll('button').forEach(b=>b.onclick=()=>{filter=b.dataset.filter;refresh()});document.querySelector('#search').oninput=refresh;</script>'''
    (REVIEW/'index.html').write_text(page,encoding='utf-8')
    print(f'{len(manifest)} cutouts; {len(REPLACE)} replacements; {len(SELECT)} additions; install={args.install}')


if __name__=='__main__':main()

