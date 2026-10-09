import os
import re
from collections import Counter

pattern = re.compile(r'bypass\s+(?:-h\s+)?([^\s"\'<>]+)', re.IGNORECASE)
no_npc_prefixes = Counter()
examples = {}

for r, d, files in os.walk('data/html'):
    for f in files:
        if f.endswith(('.htm', '.html')):
            path = os.path.join(r, f)
            with open(path, 'r', encoding='utf-8', errors='ignore') as fp:
                for m in pattern.finditer(fp.read()):
                    cmd = m.group(1).strip()
                    if not (cmd.startswith('npc_') or cmd.startswith('%objectId%') or cmd.startswith('admin_')):
                        prefix = cmd.split()[0].split('?')[0].split(':')[0]
                        no_npc_prefixes[prefix] += 1
                        if prefix not in examples:
                            examples[prefix] = (path, cmd)

print(f"Total non-admin bypass prefixes without npc_%objectId%: {len(no_npc_prefixes)}")
for p, count in no_npc_prefixes.most_common(30):
    print(f"  {p}: {count} | e.g. from {examples[p][0]}: {examples[p][1]}")
