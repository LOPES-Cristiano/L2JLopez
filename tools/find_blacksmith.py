import os

def find_file(pattern, bases):
    for label, base in bases.items():
        matches = []
        for root, dirs, files in os.walk(base):
            for f in files:
                if pattern in f.lower():
                    matches.append(os.path.relpath(os.path.join(root, f), base).replace('\\', '/'))
        print(f"[{label}] {pattern}: {len(matches)} matches")
        for m in matches[:10]:
            print(f"   {m}")

bases = {
    'L2JLopez data/html': r'd:\Cristiano\Lineage\L2JLopez\data\html',
    'L2JLopez data/scripts': r'd:\Cristiano\Lineage\L2JLopez\data\scripts',
    'L2JDreamV2 data/html': r'd:\Cristiano\Lineage\L2JDreamV2\game\data\html',
    'L2Interlude data/html': r'd:\Cristiano\Lineage\L2Interlude-main\L2Interlude-main\game\data\html',
}

# Check blacksmiths in Dion, Giran, Talking Island, Gludio
# Let's find NPC IDs of blacksmiths in V1098
import re
sql_path = r'd:\Cristiano\Lineage\L2JLopez\src\main\resources\db\data\V1098__data_npc.sql'
row_pattern = re.compile(
    r"\('(\d+)',\s*'(\d+)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)'"
)
bs_ids = []
with open(sql_path, 'r', encoding='utf-8', errors='ignore') as f:
    for line in f:
        m = row_pattern.search(line)
        if m and ('blacksmith' in m.group(3).lower() or 'blacksmith' in m.group(5).lower() or 'blacksmith' in m.group(12).lower()):
            bs_ids.append((int(m.group(1)), m.group(3), m.group(5), m.group(12)))

print(f"Found {len(bs_ids)} blacksmith NPCs:")
for b in bs_ids[:10]:
    print(f"   {b}")

for b in bs_ids[:3]:
    find_file(str(b[0]), bases)
