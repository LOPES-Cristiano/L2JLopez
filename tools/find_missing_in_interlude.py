import os
import re

sql_path = r'd:\Cristiano\Lineage\L2JLopez\src\main\resources\db\data\V1098__data_npc.sql'
row_pattern = re.compile(
    r"\('(\d+)',\s*'(\d+)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)'"
)

npcs = {}
with open(sql_path, 'r', encoding='utf-8', errors='ignore') as f:
    for line in f:
        m = row_pattern.search(line)
        if m:
            npcs[int(m.group(1))] = (m.group(3), m.group(12))

lopez_files = set()
for r, d, files in os.walk(r'd:\Cristiano\Lineage\L2JLopez\data\html'):
    for f in files:
        lopez_files.add(f.lower())

inter_html = r'd:\Cristiano\Lineage\L2Interlude-main\L2Interlude-main\game\data\html'
found_in_inter = {}
for r, d, files in os.walk(inter_html):
    for f in files:
        if f.lower() not in lopez_files:
            m = re.match(r'^(\d+)(?:-.*)?\.(?:htm|html)$', f, re.I)
            if m:
                nid = int(m.group(1))
                if nid in npcs:
                    rel = os.path.relpath(os.path.join(r, f), inter_html).replace('\\', '/')
                    found_in_inter.setdefault(nid, []).append(rel)

print(f"Found {len(found_in_inter)} missing NPCs in L2Interlude html files!")
for nid in sorted(found_in_inter.keys())[:30]:
    name, ntype = npcs[nid]
    print(f"{nid:5d} ({name:25s} | {ntype:15s}): {len(found_in_inter[nid])} files -> {found_in_inter[nid][:3]}")
