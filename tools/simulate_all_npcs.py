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

# Replicate HtmCache logic in Python
html_base = r'd:\Cristiano\Lineage\L2JLopez\data\html'
indexed_files = {}
for root, dirs, files in os.walk(html_base):
    for f in files:
        if f.endswith('.htm') or f.endswith('.html'):
            rel = os.path.relpath(os.path.join(root, f), html_base).replace('\\', '/')
            name = f.lower()
            if name not in indexed_files:
                indexed_files[name] = rel

def folder_for_type(npc_type):
    if not npc_type:
        return 'default'
    clean = npc_type.lower()
    if clean.startswith('l2'):
        clean = clean[2:]
    if clean.endswith('instance'):
        clean = clean[:-len('instance')]
    folder_map = {
        'teleporter': 'teleporter', 'castleteleporter': 'teleporter',
        'merchant': 'merchant',
        'guard': 'guard', 'guardnohtml': 'guard', 'fortguard': 'guard', 'siegeguard': 'guard',
        'warehouse': 'warehouse', 'castlewarehouse': 'warehouse',
        'trainer': 'trainer',
        'villagemaster': 'villagemaster',
        'fisherman': 'fisherman',
        'symbolmaker': 'symbolmaker',
        'doormen': 'doormen', 'doorman': 'doormen',
        'newbiehelper': 'newbiehelper',
        'adventurer_guildsman': 'adventurer_guildsman',
        'blacksmith': 'castleblacksmith', 'castleblacksmith': 'castleblacksmith',
        'magician': 'castlemagician', 'castlemagician': 'castlemagician',
        'chamberlain': 'chamberlain',
        'clanhallmanager': 'clanHallManager',
        'classmaster': 'classmaster',
        'olympiad': 'olympiad',
        'seven_signs': 'seven_signs',
    }
    return folder_map.get(clean, 'default')

def is_seven_signs(npc_id):
    return (31078 <= npc_id <= 31091) or npc_id in (31168, 31169, 31692, 31693, 31694, 31695, 31997, 31998)

def get_html_content(rel_path):
    p = os.path.join(html_base, rel_path)
    if os.path.isfile(p):
        with open(p, 'r', encoding='utf-8', errors='ignore') as fp:
            return fp.read()
    return None

def simulate_get_npc_html(npc_id, npc_type, val=0):
    folder = folder_for_type(npc_type)
    suffix = f"-{val}" if val > 0 else ""
    
    # 1. folder/id.htm
    path_type = f"{folder}/{npc_id}{suffix}.htm"
    h = get_html_content(path_type)
    if h:
        return 'EXACT_TYPE_FOLDER', path_type, h
        
    # 2. default/id.htm
    if folder != 'default':
        path_def = f"default/{npc_id}{suffix}.htm"
        h = get_html_content(path_def)
        if h:
            return 'DEFAULT_FOLDER', path_def, h
            
    # 3. indexed search
    cands = [f"{npc_id}-{val}.htm", f"{npc_id}-0{val}.htm", f"{npc_id}-{val}.html"] if val > 0 else [f"{npc_id}.htm", f"{npc_id}-1.htm", f"{npc_id}-01.htm", f"{npc_id}.html"]
    
    lower_type = npc_type.lower() if npc_type else ""
    is_ss = is_seven_signs(npc_id)
    is_func = not is_ss and any(x in lower_type for x in ['teleport', 'merchant', 'trader', 'grocer', 'blacksmith', 'trainer', 'master', 'teacher', 'warehouse', 'guard', 'fisherman', 'symbolmaker', 'priest'])
    
    for cand in cands:
        indexed_path = indexed_files.get(cand.lower())
        if indexed_path:
            h = get_html_content(indexed_path)
            if h:
                if not is_ss and (is_func and 'bypass' not in h or 'I have nothing to say' in h):
                    return 'INDEXED_ENRICHED', indexed_path, h
                return 'INDEXED_DIRECT', indexed_path, h
                
    if is_func or (not is_ss and val > 0):
        return 'SYNTHETIC_SMART', None, None
        
    def_h = get_html_content('npcdefault.htm')
    if def_h and 'I have nothing to say' not in def_h:
        return 'NPCDEFAULT_HTM', 'npcdefault.htm', def_h
        
    return 'SYNTHETIC_FALLBACK', None, None

results = Counter()
by_type_results = defaultdict(Counter)

for n in npcs:
    status, path, content = simulate_get_npc_html(n['id'], n['type'], 0)
    results[status] += 1
    by_type_results[n['type']][status] += 1

print("--- OVERALL SIMULATION RESULTS (val=0) ---")
for s, c in results.most_common():
    print(f"{s:25s}: {c:5d} ({c/len(npcs)*100:.1f}%)")

print("\n--- RESULTS BY NPC TYPE (Non-monsters) ---")
for t in sorted(by_type_results.keys()):
    if any(x in t.lower() for x in ['monster', 'pet', 'squash', 'minion', 'boss', 'chest', 'point', 'tower', 'tree']):
        continue
    c = by_type_results[t]
    total = sum(c.values())
    exact = c['EXACT_TYPE_FOLDER']
    def_f = c['DEFAULT_FOLDER']
    idx = c['INDEXED_DIRECT']
    enrich = c['INDEXED_ENRICHED']
    smart = c['SYNTHETIC_SMART']
    fallback = c['SYNTHETIC_FALLBACK']
    print(f"{t:22s} (total {total:4d}): EXACT={exact:3d}, DEF={def_f:3d}, IDX={idx:3d}, ENRICH={enrich:3d}, SMART={smart:3d}, FALLBACK={fallback:3d}")
