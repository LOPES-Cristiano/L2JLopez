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
            ntype = m.group(12)
            if ntype == 'L2Teleporter':
                npcs.append((nid, m.group(3), m.group(5)))

tele_dir = r'd:\Cristiano\Lineage\L2JLopez\data\html\teleporter'
tele_files = set(f.lower() for f in os.listdir(tele_dir))

has_base = 0
has_chat1 = 0
missing_base = []
for nid, name, title in npcs:
    base_file = f"{nid}.htm"
    chat1_file = f"{nid}-1.htm"
    if base_file in tele_files:
        has_base += 1
    else:
        missing_base.append((nid, name, title))
    if chat1_file in tele_files:
        has_chat1 += 1

print(f"Total L2Teleporter: {len(npcs)}")
print(f"Has <id>.htm in teleporter/: {has_base}")
print(f"Has <id>-1.htm in teleporter/: {has_chat1}")
print(f"Missing <id>.htm: {len(missing_base)}")
print("Sample missing <id>.htm:")
for m in missing_base[:25]:
    print(f"   {m[0]}: {m[1]} ({m[2]})")
