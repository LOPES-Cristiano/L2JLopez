import os
import re
from collections import Counter, defaultdict

sql_path = r'd:\Cristiano\Lineage\L2JLopez\src\main\resources\db\data\V1098__data_npc.sql'
row_pattern = re.compile(
    r"\('(\d+)',\s*'(\d+)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)'"
)

npcs = []
with open(sql_path, 'r', encoding='utf-8', errors='ignore') as f:
    for line in f:
        m = row_pattern.search(line)
        if m:
            nid = int(m.group(1))
            tpl = int(m.group(2))
            name = m.group(3)
            title = m.group(5)
            ntype = m.group(12)
            npcs.append({'id': nid, 'tpl': tpl, 'name': name, 'title': title, 'type': ntype})

print(f"Total parsed NPCs from V1098: {len(npcs)}")

# Let's inspect L2JLopez/data/html
html_base = r'd:\Cristiano\Lineage\L2JLopez\data\html'
indexed_files = {}
for root, dirs, files in os.walk(html_base):
    for f in files:
        if f.endswith('.htm') or f.endswith('.html'):
            rel = os.path.relpath(os.path.join(root, f), html_base).replace('\\', '/')
            indexed_files[f.lower()] = rel

print(f"Total HTML files in L2JLopez data/html: {len(indexed_files)}")

# Check scripts HTMLs
scripts_base = r'd:\Cristiano\Lineage\L2JLopez\data\scripts'
scripts_files = {}
if os.path.exists(scripts_base):
    for root, dirs, files in os.walk(scripts_base):
        for f in files:
            if f.endswith('.htm') or f.endswith('.html'):
                rel = os.path.relpath(os.path.join(root, f), scripts_base).replace('\\', '/')
                scripts_files[f.lower()] = rel

print(f"Total HTML files in L2JLopez data/scripts: {len(scripts_files)}")

# Check coverage by NPC type
by_type = defaultdict(list)
for n in npcs:
    by_type[n['type']].append(n)

print("\n--- ANALYSIS BY NPC TYPE ---")
non_mob_types = [t for t, l in by_type.items() if not any(x in t.lower() for x in ['monster', 'pet', 'squash', 'minion', 'boss', 'chest', 'point'])]
non_mob_types.sort(key=lambda t: len(by_type[t]), reverse=True)

types_to_check = ['L2Teleporter', 'L2Merchant', 'L2Trainer', 'L2Warehouse', 'L2VillageMaster', 'L2Guard', 'L2Doormen', 'L2Npc', 'L2Fisherman', 'L2SignsPriest', 'L2SymbolMaker', 'L2ManorManager', 'L2ClanHallManager', 'L2Auctioneer']

for t in types_to_check:
    list_npc = by_type[t]
    has_exact = 0
    has_indexed = 0
    has_script = 0
    missing = []
    
    clean = t.lower()
    if clean.startswith('l2'):
        clean = clean[2:]
    
    folder_map = {
        'teleporter': 'teleporter',
        'merchant': 'merchant',
        'guard': 'guard',
        'warehouse': 'warehouse',
        'trainer': 'trainer',
        'villagemaster': 'villagemaster',
        'fisherman': 'fisherman',
        'symbolmaker': 'symbolmaker',
        'doormen': 'doormen',
        'clanhallmanager': 'clanHallManager',
        'signspriest': 'seven_signs',
        'manormanager': 'manormanager',
        'auctioneer': 'auction',
        'npc': 'default'
    }
    folder = folder_map.get(clean, 'default')
    
    for n in list_npc:
        nid = n['id']
        expected_path = f"{folder}/{nid}.htm"
        cand_names = [f"{nid}.htm", f"{nid}-1.htm", f"{nid}-01.htm", f"{nid}.html"]
        if os.path.exists(os.path.join(html_base, folder, f"{nid}.htm")):
            has_exact += 1
        elif any(c in indexed_files for c in cand_names):
            has_indexed += 1
        elif any(c in scripts_files for c in cand_names):
            has_script += 1
        else:
            missing.append(n)
            
    print(f"Type: {t:20s} | Total: {len(list_npc):4d} | Exact In Folder: {has_exact:4d} | Elsewhere: {has_indexed:4d} | In scripts: {has_script:4d} | MISSING: {len(missing):4d}")
    if missing:
        missing_ids = [f"{m['id']} ({m['name']})" for m in missing[:6]]
        print(f"   Missing sample: {', '.join(missing_ids)}")

