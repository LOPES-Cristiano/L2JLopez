import os
import re

DATA_HTML = "data/html"
GAME_SESSION = "src/main/java/com/lopez/l2j/network/game/GameSession.java"

# Read GameSession.java to find all handles
with open(GAME_SESSION, 'r', encoding='utf-8') as f:
    gs_code = f.read()

bypass_pattern = re.compile(r'bypass\s+(?:-h\s+)?([^\s"\'<>]+)', re.IGNORECASE)

actions = {}

for root, dirs, files in os.walk(DATA_HTML):
    for f in files:
        if f.endswith(('.htm', '.html')):
            p = os.path.join(root, f)
            try:
                with open(p, 'r', encoding='utf-8', errors='ignore') as fp:
                    content = fp.read()
                    for m in bypass_pattern.finditer(content):
                        raw = m.group(1).strip()
                        # normalize
                        norm = raw.replace('npc_%objectId%_', '').replace('%objectId%_', '')
                        norm = norm.replace('npc_%object_id%_', '').replace('npc_0_', '').replace('npc__', '')
                        if norm.startswith('admin_'):
                            continue
                        prefix = norm.split()[0].split('?')[0].split(':')[0]
                        if prefix not in actions:
                            actions[prefix] = []
                        if len(actions[prefix]) < 3:
                            actions[prefix].append((raw, p))
            except Exception:
                pass

print(f"Total non-admin bypass prefixes: {len(actions)}")
print("Checking which prefixes are missing from GameSession.java:")

missing = []
for p, examples in sorted(actions.items()):
    # Check if p is mentioned in gs_code
    if f'"{p}"' not in gs_code and f'.startsWith("{p}")' not in gs_code and f'.equals("{p}")' not in gs_code and f'.contains("{p}")' not in gs_code:
        # Check case insensitive
        if p.lower() not in gs_code.lower():
            missing.append((p, len(actions[p]), examples[0]))

print(f"\nMissing prefixes ({len(missing)}):")
for p, count, ex in missing:
    print(f"Prefix: {p} | Example: {ex[0]} in {ex[1]}")
