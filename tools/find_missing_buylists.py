import xml.etree.ElementTree as ET

tree = ET.parse('data/xml/world/buylists.xml')
for b in tree.getroot().findall('.//buylist'):
    bid = b.attrib.get('id')
    npc = b.attrib.get('npcId')
    try:
        if int(npc) in range(30080, 30100):
            print(f"NPC {npc} -> Buylist {bid} ({len(b.findall('.//product'))} items)")
    except Exception:
        pass


