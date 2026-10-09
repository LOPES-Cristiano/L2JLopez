import os
import re

# Load NPCs
sql_path = r'd:\Cristiano\Lineage\L2JLopez\src\main\resources\db\data\V1098__data_npc.sql'
row_pattern = re.compile(
    r"\('(\d+)',\s*'(\d+)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)'"
)
npcs = {}
with open(sql_path, 'r', encoding='utf-8', errors='ignore') as f:
    for line in f:
        m = row_pattern.search(line)
        if m:
            nid = int(m.group(1))
            npcs[nid] = {'id': nid, 'tpl': int(m.group(2)), 'name': m.group(3), 'title': m.group(5), 'type': m.group(12)}

# Check in other packs
dream_html = r'd:\Cristiano\Lineage\L2JDreamV2\game\data\html'
inter_html = r'd:\Cristiano\Lineage\L2Interlude-main\L2Interlude-main\game\data\html'
lopez_html = r'd:\Cristiano\Lineage\L2JLopez\data\html'
lopez_scripts = r'd:\Cristiano\Lineage\L2JLopez\data\scripts'

def index_pack(base_path):
    idx = {}
    for root, dirs, files in os.walk(base_path):
        for f in files:
            if f.endswith('.htm') or f.endswith('.html'):
                rel = os.path.relpath(os.path.join(root, f), base_path).replace('\\', '/')
                idx.setdefault(f.lower(), []).append(rel)
    return idx

dream_idx = index_pack(dream_html)
inter_idx = index_pack(inter_html)
lopez_idx = index_pack(lopez_html)
scripts_idx = index_pack(lopez_scripts)

# Teleporters missing exact in lopez
teleporters = [n for n in npcs.values() if n['type'] == 'L2Teleporter']
print(f"Total L2Teleporter: {len(teleporters)}")

missing_in_lopez_folder = []
for t in teleporters:
    expected = f"teleporter/{t['id']}.htm"
    if not os.path.exists(os.path.join(lopez_html, expected)):
        missing_in_lopez_folder.append(t)

print(f"Teleporters without teleporter/<id>.htm: {len(missing_in_lopez_folder)}")
print("\nSample missing teleporters and where they exist in other packs:")
for t in missing_in_lopez_folder[:20]:
    nid = t['id']
    fn = f"{nid}.htm"
    in_lopez_idx = lopez_idx.get(fn, [])
    in_scripts = scripts_idx.get(fn, [])
    in_dream = dream_idx.get(fn, [])
    in_inter = inter_idx.get(fn, [])
    print(f"NPC {nid} ({t['name']} - {t['title']}):")
    print(f"   In Lopez html:    {in_lopez_idx}")
    print(f"   In Lopez scripts: {in_scripts}")
    print(f"   In Dream:         {in_dream}")
    print(f"   In Interlude:     {in_inter}")
