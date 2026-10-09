import os
import re

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
            name = m.group(3)
            title = m.group(5)
            ntype = m.group(12)
            npcs.append({'id': nid, 'name': name, 'title': title, 'type': ntype})

html_base = r'd:\Cristiano\Lineage\L2JLopez\data\html'
indexed_files = {}
for root, dirs, files in os.walk(html_base):
    for f in files:
        if f.endswith('.htm') or f.endswith('.html'):
            rel = os.path.relpath(os.path.join(root, f), html_base).replace('\\', '/')
            name = f.lower()
            if name not in indexed_files:
                indexed_files[name] = rel

def has_file(rel):
    return os.path.isfile(os.path.join(html_base, rel))

# Check missing in teleporter
missing_tele = []
for n in npcs:
    if n['type'] == 'L2Teleporter':
        nid = n['id']
        # Check special
        if nid in (31092, 31113, 31126, 31111, 31112) or (31127 <= nid <= 31131) or (31137 <= nid <= 31141) or (31865 <= nid <= 31918):
            continue
        if has_file(f"teleporter/{nid}.htm") or has_file(f"default/{nid}.htm") or f"{nid}.htm" in indexed_files:
            continue
        missing_tele.append(n)

print(f"Teleporters still without HTML: {len(missing_tele)}")
for m in missing_tele:
    print(f"   {m['id']} {m['name']} ({m['title']})")
