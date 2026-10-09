import re
from collections import Counter

types = Counter()
with open('src/main/resources/db/data/V1098__data_npc.sql', 'r', encoding='utf-8', errors='ignore') as f:
    for line in f:
        # Pattern in SQL: `type` is one of the columns
        # Example: (30001, 30001, 'Newbie Helper', 0, '', 0, 'npc', 8.00, 23.00, 70, 'male', 'L2NewbieHelper', ...
        m = re.search(r"'(male|female)',\s*'([^']+)'", line)
        if m:
            types[m.group(2)] += 1

for t, c in types.most_common():
    print(f"{t}: {c}")
