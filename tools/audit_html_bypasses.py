import os
import re
from collections import Counter

DATA_HTML = "data/html"
bypass_pattern = re.compile(r'bypass\s+(?:-h\s+)?([^\s"\'<>]+)', re.IGNORECASE)

prefixes = Counter()
full_commands = Counter()
total_matches = 0

for root, dirs, files in os.walk(DATA_HTML):
    for f in files:
        if f.endswith(('.htm', '.html')):
            p = os.path.join(root, f)
            try:
                with open(p, 'r', encoding='utf-8', errors='ignore') as fp:
                    content = fp.read()
                    for m in bypass_pattern.finditer(content):
                        raw_cmd = m.group(1).strip()
                        # normalize %objectId%
                        clean_cmd = raw_cmd.replace('npc_%objectId%_', '').replace('%objectId%_', '')
                        clean_cmd = clean_cmd.replace('npc_%object_id%_', '')
                        prefix = clean_cmd.split()[0].split('?')[0].split(':')[0]
                        prefixes[prefix] += 1
                        total_matches += 1
            except Exception as e:
                pass

print(f"Total bypass instances found: {total_matches}")
print(f"Distinct bypass prefixes found: {len(prefixes)}")
print("\nTop 50 most common bypass prefixes in data/html:")
for prefix, count in prefixes.most_common(50):
    print(f"  {prefix}: {count}")
