import os
import re

DATA_HTML = "data/html"
goto_pattern = re.compile(r'bypass\s+(?:-h\s+)?(?:npc_%objectId%_)?goto\s+(\d+)', re.IGNORECASE)
buy_pattern = re.compile(r'bypass\s+(?:-h\s+)?(?:npc_%objectId%_)?(?:Buy|Wear)\s+(\d+)', re.IGNORECASE)
multisell_pattern = re.compile(r'bypass\s+(?:-h\s+)?(?:npc_%objectId%_)?(?:exc_multisell|multisell)\s+(\d+)', re.IGNORECASE)

goto_ids = set()
buy_ids = set()
multisell_ids = set()

for root, dirs, files in os.walk(DATA_HTML):
    for f in files:
        if f.endswith(('.htm', '.html')):
            with open(os.path.join(root, f), 'r', encoding='utf-8', errors='ignore') as fp:
                content = fp.read()
                for m in goto_pattern.finditer(content):
                    goto_ids.add(int(m.group(1)))
                for m in buy_pattern.finditer(content):
                    buy_ids.add(int(m.group(1)))
                for m in multisell_pattern.finditer(content):
                    multisell_ids.add(int(m.group(1)))

print(f"Distinct goto IDs in HTML: {len(goto_ids)} (e.g. {sorted(list(goto_ids))[:10]})")
print(f"Distinct Buy/Wear IDs in HTML: {len(buy_ids)} (e.g. {sorted(list(buy_ids))[:10]})")
print(f"Distinct multisell IDs in HTML: {len(multisell_ids)} (e.g. {sorted(list(multisell_ids))[:10]})")
