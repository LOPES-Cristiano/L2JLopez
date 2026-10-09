import os
import re

DATA_HTML = "data/html"
all_htmls = set()
for root, dirs, files in os.walk(DATA_HTML):
    for f in files:
        if f.endswith(('.htm', '.html')):
            rel = os.path.relpath(os.path.join(root, f), DATA_HTML).replace('\\', '/').lower()
            all_htmls.add(rel)

def folder_for_type(npc_type):
    if not npc_type:
        return "default"
    clean = npc_type.lower()
    if clean.startswith("l2"):
        clean = clean[2:]
    if clean.endswith("instance"):
        clean = clean[:-len("instance")]
    
    mapping = {
        "teleporter": "teleporter",
        "castleteleporter": "castleteleporter",
        "merchant": "merchant",
        "guard": "guard", "guardnohtml": "guard", "fortsiegeguard": "guard", "siegeguard": "guard", "warden": "guard",
        "warehouse": "warehouse",
        "castlewarehouse": "castlewarehouse",
        "trainer": "trainer", "mysticmaster": "trainer", "priestmaster": "trainer",
        "villagemaster": "villagemaster",
        "fisherman": "fisherman",
        "symbolmaker": "symbolmaker",
        "doormen": "doormen", "doorman": "doormen",
        "newbiehelper": "newbiehelper",
        "adventurer": "adventurer_guildsman", "adventurer_guildsman": "adventurer_guildsman",
        "castleblacksmith": "castleblacksmith",
        "blacksmith": "default",
        "castlemagician": "castlemagician", "magician": "castlemagician",
        "chamberlain": "chamberlain", "castlechamberlain": "chamberlain",
        "clanhallmanager": "clanhallmanager",
        "classmaster": "classmaster",
        "olympiad": "olympiad", "olympiadmanager": "olympiad",
        "seven_signs": "seven_signs", "sevensigns": "seven_signs", "signspriest": "seven_signs", "festivalguide": "seven_signs",
        "manormanager": "manormanager",
        "auctioneer": "auction", "auction": "auction",
        "wyvernmanager": "wyvernmanager", "fortwyvernmanager": "wyvernmanager",
        "observation": "observation",
        "sepulchernpc": "sepulchernpc",
        "mercmanager": "mercmanager",
        "siegenpc": "siege", "siege": "siege",
        "fortmanager": "fortress", "fortsupportunit": "fortress", "fortcommander": "fortress", "fortenvoy": "fortress", "fortsiegenpc": "fortress"
    }
    return mapping.get(clean, "default")

def get_special_seven_signs(npc_id):
    if npc_id == 31092: return "seven_signs/blkmrkt_1.htm"
    if npc_id == 31113: return "seven_signs/mammmerch_1.htm"
    if npc_id == 31126: return "seven_signs/mammblack_1.htm"
    if npc_id == 31111: return "seven_signs/spirit_dawn.htm"
    if npc_id == 31112: return "seven_signs/spirit_dusk.htm"
    if 31127 <= npc_id <= 31131: return "seven_signs/festival/dawn_guide.htm"
    if 31137 <= npc_id <= 31141: return "seven_signs/festival/dusk_guide.htm"
    if 31132 <= npc_id <= 31136 or 31142 <= npc_id <= 31146: return "seven_signs/festival/festival_witch.htm"
    if 31865 <= npc_id <= 31918: return "seven_signs/rift/GuardianOfBorder.htm"
    if npc_id in (31688, 31690, 31769, 31770, 31771, 31772): return "olympiad/monument.htm"
    if 31078 <= npc_id <= 31091: return f"seven_signs/{npc_id}.htm"
    if npc_id in (31168, 31169, 31692, 31693, 31694, 31695, 31997, 31998): return f"seven_signs/{npc_id}.htm"
    return None

def resolve_shared(folder, npc_id):
    if folder == "symbolmaker": return "symbolmaker/symbolmaker.htm"
    if folder == "manormanager": return "manormanager/manager.htm"
    if folder == "auction": return "auction/auction.htm"
    if folder == "clanhallmanager": return "clanhallmanager/chamberlain.htm"
    if folder == "castleblacksmith": return "castleblacksmith/castleblacksmith.htm"
    if folder == "castlewarehouse": return "castlewarehouse/castlewarehouse.htm"
    if folder == "castleteleporter": return "castleteleporter/massgk.htm"
    if folder == "teleporter" and 35092 <= npc_id <= 35565: return "castleteleporter/massgk.htm"
    if folder == "chamberlain":
        if f"chamberlain/{npc_id}-d.htm" in all_htmls:
            return f"chamberlain/{npc_id}-d.htm"
        return "chamberlain/chamberlain.htm"
    if folder == "castlemagician": return "castlemagician/magician.htm"
    if folder == "mercmanager": return "mercmanager/mercmanager.htm"
    if folder == "wyvernmanager": return "wyvernmanager/wyvernmanager.htm"
    if folder == "classmaster": return "classmaster/classmaster.htm"
    if folder == "fortress": return "fortress/supportunit.htm"
    if folder == "siege": return f"siege/{npc_id}-busy.htm"
    if folder == "doormen":
        if f"doormen/{npc_id}-no.htm" in all_htmls:
            return f"doormen/{npc_id}-no.htm"
        return "doormen/35602-no.htm"
    return None

def resolve_npc(npc_id, npc_type):
    # 1. Special Seven Signs
    ss = get_special_seven_signs(npc_id)
    if ss and ss.lower() in all_htmls:
        return ss, "SpecialSevenSigns"
    
    # 2. Friend NPCs (Ketra / Varka / Primeval Isle)
    friend = f"npc_friend/{npc_id}.htm"
    if friend.lower() in all_htmls:
        return friend, "NpcFriend"
    
    folder = folder_for_type(npc_type)
    
    # 3. Exact folder/<npcId>.htm
    exact_folder = f"{folder}/{npc_id}.htm"
    if exact_folder.lower() in all_htmls:
        return exact_folder, "ExactFolder"
    
    # 4. Fortress subfolder for doormen
    if folder == "doormen":
        fort_door = f"doormen/fortress/{npc_id}.htm"
        if fort_door.lower() in all_htmls:
            return fort_door, "DoormenFortress"
        fort_door_busy = f"doormen/fortress/{npc_id}-busy.htm"
        if fort_door_busy.lower() in all_htmls:
            return fort_door_busy, "DoormenFortress"
    
    # 5. Exact default/<npcId>.htm
    exact_default = f"default/{npc_id}.htm"
    if exact_default.lower() in all_htmls:
        return exact_default, "ExactDefault"
    
    # 6. Cross-folder search
    for f in ["trainer", "merchant", "teleporter", "warehouse", "guard", "villagemaster", 
              "doormen", "seven_signs", "adventurer_guildsman", "fisherman", "newbiehelper", 
              "noblesse", "fortress", "siege", "classmaster", "npc_friend"]:
        candidate = f"{f}/{npc_id}.htm"
        if candidate.lower() in all_htmls:
            return candidate, "CrossFolderSearch"
    
    # 7. Shared template
    shared = resolve_shared(folder, npc_id)
    if shared and shared.lower() in all_htmls:
        return shared, "SharedTemplate"
    
    # 8. Guard fallback
    if folder == "guard":
        return "guard/guard.htm", "GuardGeneric"
    
    # 9. Default fallback
    if "npcdefault.htm" in all_htmls:
        return "npcdefault.htm", "NpcDefaultFallback"
    
    return "HARDCODED_FALLBACK", "Hardcoded"

# All interactive NPCs that have chat windows
interactive_types = {
    'L2Teleporter', 'L2CastleTeleporter', 'L2Merchant', 'L2Trainer', 'L2MysticMaster', 'L2PriestMaster',
    'L2Warehouse', 'L2CastleWarehouse', 'L2VillageMaster', 'L2Fisherman', 'L2SymbolMaker', 'L2Doormen',
    'L2NewbieHelper', 'L2Adventurer', 'L2CastleBlacksmith', 'L2CastleMagician', 'L2CastleChamberlain',
    'L2ClanHallManager', 'L2ClassMaster', 'L2OlympiadManager', 'L2SignsPriest',
    'L2FestivalGuide', 'L2ManorManager', 'L2Auctioneer', 'L2WyvernManager', 'L2FortWyvernManager',
    'L2Observation', 'L2MercManager', 'L2SiegeNpc', 'L2FortManager', 'L2FortSupportUnit',
    'L2FortCommander', 'L2FortEnvoy', 'L2FortSiegeNpc', 'L2Guard', 'L2GuardNoHTML', 'L2FortSiegeGuard',
    'L2SiegeGuard', 'L2Warden', 'L2Npc', 'L2NpcWalker'
}

npcs = []
pattern = re.compile(r"\('(\d+)',\s*'(\d+)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([0-9.]+)',\s*'([0-9.]+)',\s*'(\d+)',\s*'([^']*)',\s*'([^']*)'")
with open('src/main/resources/db/data/V1098__data_npc.sql', 'r', encoding='utf-8', errors='ignore') as f:
    for line in f:
        m = pattern.search(line)
        if m:
            npc_id = int(m.group(1))
            name = m.group(3)
            npc_type = m.group(12)
            if npc_type in interactive_types:
                npcs.append((npc_id, name, npc_type))

print(f"Total interactive NPCs parsed: {len(npcs)}")

stats = {}
unresolved = []
for npc_id, name, npc_type in npcs:
    resolved_path, method = resolve_npc(npc_id, npc_type)
    stats[method] = stats.get(method, 0) + 1
    if method in ("NpcDefaultFallback", "Hardcoded") and npc_type not in ('L2Npc', 'L2NpcWalker'):
        unresolved.append((npc_id, name, npc_type, resolved_path, method))

print("\nResolution Statistics:")
for k, v in sorted(stats.items(), key=lambda x: x[1], reverse=True):
    print(f"  {k}: {v}")

print(f"\nNon-L2Npc falling to default fallback: {len(unresolved)}")
by_type = {}
for u in unresolved:
    by_type[u[2]] = by_type.get(u[2], 0) + 1
for t, cnt in sorted(by_type.items(), key=lambda x: x[1], reverse=True):
    print(f"  {t}: {cnt}")

if unresolved:
    print("\nSamples of unresolved non-L2Npc:")
    for u in unresolved[:20]:
        print(f"  ID={u[0]}, Name='{u[1]}', Type={u[2]}")
