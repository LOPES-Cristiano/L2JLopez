import glob, re
import xml.etree.ElementTree as ET

tree = ET.parse('data/xml/world/teleports.xml')
current_ids = {int(x.attrib['id']) for x in tree.getroot().findall('.//teleport')}

tree_interlude = ET.parse('../L2Interlude-main/L2Interlude-main/game/data/xml/teleports.xml')
interlude_teleports = {int(x.attrib['id']): x for x in tree_interlude.getroot().findall('.//teleport')}

html_gotos = {}
for f in glob.glob('data/html/**/*.htm', recursive=True):
    try:
        content = open(f, 'r', encoding='utf-8', errors='ignore').read()
        for m in re.finditer(r'<a\s+[^>]*action=["\']bypass\s+-h\s+(?:npc_%objectId%_)?goto\s+(\d+)["\'][^>]*>(.*?)</a>', content, re.IGNORECASE):
            tid = int(m.group(1))
            if tid not in current_ids and tid not in html_gotos:
                text = m.group(2).strip()
                html_gotos[tid] = (f, text)
    except Exception:
        pass

print(f"Total missing: {len(html_gotos)}")
in_interlude = []
not_in_interlude = []
for tid, (f, text) in sorted(html_gotos.items()):
    if tid in interlude_teleports:
        in_interlude.append((tid, f, text))
    else:
        not_in_interlude.append((tid, f, text))

print(f"In L2Interlude-main: {len(in_interlude)}")
for tid, f, text in in_interlude[:10]:
    print(f"  {tid}: {text} ({f})")

print(f"NOT in L2Interlude-main: {len(not_in_interlude)}")
for tid, f, text in not_in_interlude[:25]:
    print(f"  {tid}: {text} ({f})")
