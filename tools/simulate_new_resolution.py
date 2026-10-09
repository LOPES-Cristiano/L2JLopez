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
        'teleporter': 'teleporter',
        'castleteleporter': 'castleteleporter',
        'merchant': 'merchant',
        'guard': 'guard', 'guardnohtml': 'guard', 'fortguard': 'guard', 'siegeguard': 'guard',
        'warehouse': 'warehouse',
        'castlewarehouse': 'castlewarehouse',
        'trainer': 'trainer', 'mysticmaster': 'trainer', 'priestmaster': 'trainer',
        'villagemaster': 'villagemaster',
        'fisherman': 'fisherman',
        'symbolmaker': 'symbolmaker',
        'doormen': 'doormen', 'doorman': 'doormen',
        'newbiehelper': 'newbiehelper',
        'adventurer': 'adventurer_guildsman', 'adventurer_guildsman': 'adventurer_guildsman',
        'castleblacksmith': 'castleblacksmith',
        'blacksmith': 'default',
        'castlemagician': 'castlemagician', 'magician': 'castlemagician',
        'chamberlain': 'chamberlain', 'castlechamberlain': 'chamberlain',
        'clanhallmanager': 'clanHallManager',
        'classmaster': 'classmaster',
        'olympiad': 'olympiad', 'olympiadmanager': 'olympiad',
        'seven_signs': 'seven_signs', 'sevensigns': 'seven_signs', 'signspriest': 'seven_signs',
        'manormanager': 'manormanager',
        'auctioneer': 'auction',
        'wyvernmanager': 'wyvernmanager', 'fortwyvernmanager': 'wyvernmanager',
        'observation': 'observation',
        'sepulchernpc': 'SepulcherNpc',
        'fortmanager': 'fortress', 'fortsupportunit': 'fortress', 'fortcommander': 'fortress',
        'fortenvoy': 'fortress', 'fortsiegenpc': 'fortress',
    }
    return folder_map.get(clean, 'default')

def has_file(rel):
    return os.path.isfile(os.path.join(html_base, rel))

def resolve_shared(folder, npc_id, val):
    suffix = f"-{val}" if val > 0 else ""
    if folder == 'symbolmaker':
        h = f"symbolmaker/SymbolMaker{suffix}.htm"
        return h if has_file(h) else "symbolmaker/SymbolMaker.htm"
    elif folder == 'manormanager':
        h = f"manormanager/manager{suffix}.htm"
        return h if has_file(h) else "manormanager/manager.htm"
    elif folder == 'auction':
        h = f"auction/auction{suffix}.htm"
        return h if has_file(h) else "auction/auction.htm"
    elif folder == 'clanHallManager':
        h = "clanHallManager/chamberlain.htm"
        return h if has_file(h) else "clanHallManager/manage.htm"
    elif folder == 'castleblacksmith':
        h = f"castleblacksmith/castleblacksmith{suffix}.htm"
        return h if has_file(h) else "castleblacksmith/castleblacksmith.htm"
    elif folder == 'castlewarehouse':
        h = f"castlewarehouse/castlewarehouse{suffix}.htm"
        return h if has_file(h) else "castlewarehouse/castlewarehouse.htm"
    elif folder == 'castleteleporter':
        h = f"castleteleporter/MassGK{suffix}.htm"
        return h if has_file(h) else "teleporter/castleteleporter.htm"
    elif folder == 'chamberlain':
        h = f"chamberlain/{npc_id}-d.htm"
        return h if has_file(h) else "chamberlain/chamberlain.htm"
    elif folder == 'castlemagician':
        return "castlemagician/magician.htm"
    elif folder == 'mercmanager':
        return "mercmanager/mercmanager.htm"
    elif folder == 'wyvernmanager':
        return "wyvernmanager/wyvernmanager.htm"
    elif folder == 'classmaster':
        return "classmaster/classmaster.htm"
    return None

def simulate_new_get_npc_html(npc_id, npc_type, val=0):
    # 1. Special Seven Signs, Mammon, Rift, Olympiad
    if npc_id == 31092: return 'SPECIAL_OVERRIDE', "seven_signs/blkmrkt_1.htm"
    if npc_id == 31113: return 'SPECIAL_OVERRIDE', "seven_signs/mammmerch_1.htm"
    if npc_id == 31126: return 'SPECIAL_OVERRIDE', "seven_signs/mammblack_1.htm"
    if npc_id == 31111: return 'SPECIAL_OVERRIDE', "seven_signs/spirit_dawn.htm"
    if npc_id == 31112: return 'SPECIAL_OVERRIDE', "seven_signs/spirit_exit.htm"
    if 31127 <= npc_id <= 31131: return 'SPECIAL_OVERRIDE', "seven_signs/festival/dawn_guide.htm"
    if 31137 <= npc_id <= 31141: return 'SPECIAL_OVERRIDE', "seven_signs/festival/dusk_guide.htm"
    if (31132 <= npc_id <= 31136) or (31142 <= npc_id <= 31146): return 'SPECIAL_OVERRIDE', "seven_signs/festival/festival_witch.htm"
    if 31865 <= npc_id <= 31918: return 'SPECIAL_OVERRIDE', "seven_signs/rift/GuardianOfBorder.htm"
    if npc_id == 31688: return 'SPECIAL_OVERRIDE', ("olympiad/noble_menu" + str(val) + ".htm" if val > 0 else "olympiad/noble_main.htm")
    if npc_id == 31690 or (31769 <= npc_id <= 31772): return 'SPECIAL_OVERRIDE', "olympiad/hero_main.htm"

    folder = folder_for_type(npc_type)
    suffix = f"-{val}" if val > 0 else ""
    
    # 2. folder/id.htm
    p_type = f"{folder}/{npc_id}{suffix}.htm"
    if has_file(p_type):
        return 'EXACT_TYPE_FOLDER', p_type
        
    if val > 0:
        alt_type = f"{folder}/{npc_id}-0{val}.htm"
        if has_file(alt_type):
            return 'EXACT_TYPE_FOLDER', alt_type
            
    # 3. default/id.htm
    if folder != 'default':
        p_def = f"default/{npc_id}{suffix}.htm"
        if has_file(p_def):
            return 'DEFAULT_FOLDER', p_def
        if val > 0:
            alt_def = f"default/{npc_id}-0{val}.htm"
            if has_file(alt_def):
                return 'DEFAULT_FOLDER', alt_def

    # 4. Indexed search
    cands = [f"{npc_id}-{val}.htm", f"{npc_id}-0{val}.htm", f"{npc_id}-{val}.html"] if val > 0 else [f"{npc_id}.htm", f"{npc_id}-1.htm", f"{npc_id}-01.htm", f"{npc_id}.html"]
    for c in cands:
        indexed_path = indexed_files.get(c.lower())
        if indexed_path and has_file(indexed_path):
            return 'INDEXED_DIRECT', indexed_path

    # 5. Shared template
    shared = resolve_shared(folder, npc_id, val)
    if shared and has_file(shared):
        return 'SHARED_TEMPLATE', shared

    # 6. Guard default
    if folder == 'guard' and has_file('guard/guard.htm'):
        return 'GUARD_DEFAULT', 'guard/guard.htm'

    # 7. Fallback npcdefault.htm
    if has_file('npcdefault.htm'):
        return 'NPCDEFAULT_HTM', 'npcdefault.htm'

    return 'FALLBACK_HARDCODED', None

by_type_results = defaultdict(Counter)
results = Counter()

for n in npcs:
    status, path = simulate_new_get_npc_html(n['id'], n['type'], 0)
    results[status] += 1
    by_type_results[n['type']][status] += 1

print("--- OVERALL RESULTS WITH NEW CANONICAL RESOLUTION ---")
for s, c in results.most_common():
    print(f"{s:25s}: {c:5d} ({c/len(npcs)*100:.1f}%)")

print("\n--- NON-MONSTER NPC TYPES RESULTS ---")
types_to_show = [
    'L2Teleporter', 'L2Merchant', 'L2Warehouse', 'L2Trainer',
    'L2VillageMaster', 'L2Guard', 'L2Doormen', 'L2Fisherman', 'L2SymbolMaker',
    'L2SignsPriest', 'L2NewbieHelper', 'L2Adventurer',
    'L2CastleTeleporter', 'L2CastleBlacksmith', 'L2CastleWarehouse', 'L2CastleChamberlain',
    'L2CastleMagician', 'L2MercManager', 'L2ClanHallManager', 'L2Auctioneer',
    'L2ManorManager', 'L2WyvernManager', 'L2FortSupportUnit', 'L2FortManager',
    'L2OlympiadManager', 'L2Observation', 'L2ClassMaster', 'L2SepulcherNpc', 'L2Npc'
]

for t in types_to_show:
    c = by_type_results[t]
    total = sum(c.values())
    exact = c['EXACT_TYPE_FOLDER']
    def_f = c['DEFAULT_FOLDER']
    idx = c['INDEXED_DIRECT']
    spec = c['SPECIAL_OVERRIDE']
    shared = c['SHARED_TEMPLATE']
    guard = c['GUARD_DEFAULT']
    npc_def = c['NPCDEFAULT_HTM']
    
    resolved = exact + def_f + idx + spec + shared + guard
    print(f"{t:22s} ({total:4d}): RESOLVED={resolved:4d} (Exact={exact:3d}, Def={def_f:3d}, Idx={idx:3d}, Spec={spec:3d}, Shared={shared:3d}, Guard={guard:3d}) | npcdefault={npc_def:3d}")
