import glob, re
import xml.etree.ElementTree as ET

tree = ET.parse('data/xml/world/teleports.xml')
current_ids = {int(x.attrib['id']) for x in tree.getroot().findall('.//teleport')}

tree_interlude = ET.parse('../L2Interlude-main/L2Interlude-main/game/data/xml/teleports.xml')
interlude_ids = {int(x.attrib['id']) for x in tree_interlude.getroot().findall('.//teleport')}

all_known = current_ids.union(interlude_ids)

missing_entries = {}
for f in glob.glob('data/html/**/*.htm', recursive=True):
    try:
        content = open(f, 'r', encoding='utf-8', errors='ignore').read()
        for m in re.finditer(r'<a\s+[^>]*action=["\']bypass\s+-h\s+(?:npc_%objectId%_)?goto\s+(\d+)["\'][^>]*>(.*?)</a>', content, re.IGNORECASE):
            tid = int(m.group(1))
            if tid not in all_known and tid not in missing_entries:
                raw_text = m.group(2).strip()
                # extract msg if present
                full_tag = m.group(0)
                msg_match = re.search(r'msg=["\']\d+;([^"\']+)["\']', full_tag)
                msg_text = msg_match.group(1) if msg_match else ""
                clean_text = re.sub(r'<[^>]+>', '', raw_text)
                clean_text = re.sub(r'-\s*\d+(\.\d+)?\s*(adena)?', '', clean_text, flags=re.IGNORECASE).strip()
                missing_entries[tid] = (clean_text, msg_text, f)
    except Exception:
        pass

print(f"Total entries to map: {len(missing_entries)}")
unique_names = set(v[0] for v in missing_entries.values())
print(f"Total unique destination names: {len(unique_names)}")
for n in sorted(unique_names):
    print(f"  '{n}'")

