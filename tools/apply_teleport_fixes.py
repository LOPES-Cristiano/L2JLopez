import glob, re
import xml.etree.ElementTree as ET

# Destination name to (x, y, z, price)
COORD_MAP = {
    '2nd Floor 150.000': (114674, 13984, 3960, 150000),
    'Aden Border Checkpoint': (138700, -44000, -3000, 500),
    'Aden Castle Gate': (147463, 10373, -1224, 500),
    'Aden Town Square': (147926, 28164, -2268, 500),
    'Aligator Island': (113000, 184500, -3600, 500),
    'Ancient Battleground': (160800, 21500, -3700, 500),
    'Ancient battleground': (160800, 21500, -3700, 500),
    'Ant Cave East Entrance': (-11800, 183500, -3700, 500),
    'Bandit Stronghold': (83000, -16000, -1800, 0),
    'Beast Farm': (53444, -86175, -2890, 0),
    'Blazing Swamp': (146000, -12000, -2500, 500),
    'Border Outpost': (119000, 26800, -3500, 500),
    'Brekas Stronghold': (70000, 120000, -3500, 500),
    'Cruma Marshlands': (15200, 114500, -3700, 500),
    'Crypt of disgrace': (61200, -121400, -2000, 500),
    'Den of evil': (88500, -114000, -2100, 500),
    'Devil\'s Pass': (141000, -43000, -3500, 500),
    'Devils Isle': (50000, 220000, -3500, 500),
    'Dion Castle Gate': (22340, 161200, -2800, 500),
    'Dion Castle Town': (15670, 142983, -2705, 500),
    'Dion Town Square': (15670, 142983, -2705, 500),
    'Disciples Necropolis': (172000, -17600, -4900, 500),
    'Dragon Valley': (110000, 115000, -3700, 500),
    'East of Blazing Swamp': (146000, -12000, -2500, 500),
    'Fields of Massacre': (185200, 20200, -3400, 0),
    'Forbidden Gateway': (168400, -13500, -3100, 500),
    'Forest of Evil': (84400, 36200, -3300, 500),
    'Forest of the Dead - East Enterance': (59780, -42260, -3000, 500),
    'Forest of the Dead - West Enteranc': (49400, -38300, -3300, 500),
    'Forge of the Gods': (183861, -114949, -3328, 500),
    'Forsaken Plains': (171700, 55200, -4300, 500),
    'Fortress East Gate': (78000, 142000, -3000, 500),
    'Fortress North Gate': (75000, 140000, -3000, 500),
    'Fortress South Gate': (75000, 145000, -3000, 500),
    'Fortress West Gate': (72000, 142000, -3000, 500),
    'Front of the Gludio Castle': (-17834, 111394, -3696, 500),
    'Front of the Shanty Fortress': (-57640, 156100, -2500, 500),
    'Front of the Southern Fortress': (-20000, 180000, -3000, 500),
    'Frozen labyrinth': (113900, -115400, -2000, 500),
    'Garden of Beasts': (140100, -78500, -4000, 500),
    'Garden of Eva': (85000, 256000, -11600, 500),
    'Giran Castle Gate': (115000, 145000, -3700, 500),
    'Giran Territory': (78000, 152000, -3500, 500),
    'Giran Town Square': (83400, 147943, -3404, 500),
    'Gludio Castle Gate': (-17834, 111394, -3696, 500),
    'Gludio Town Square': (-12672, 122776, -3116, 500),
    'Goddard Border Checkpoint': (140400, -43500, -3000, 500),
    'Goddard Castle Gate': (147400, -45000, -2200, 500),
    'Goddard Castle Town': (147948, -55338, -2734, 500),
    'Goddard Town Square': (147948, -55338, -2734, 500),
    'Heine Town Square': (111322, 219320, -3543, 500),
    'Hot Springs': (150868, -123018, -2296, 0),
    'Innadril Castle Gate': (115000, 248000, -1100, 500),
    'Ivory Tower': (85337, 16187, -3694, 500),
    'Ketra Orc Outpost': (144880, -113468, -2560, 500),
    'Ketra Orc outpost': (144880, -113468, -2560, 500),
    'Molten tops': (180000, -105000, -3500, 500),
    'North Path to The Cementery': (172000, 20000, -3200, 500),
    'Northern Pathway of Enchanted Valley': (114100, 42000, -3600, 500),
    'Oren Castle Gate': (83000, 40000, -1500, 500),
    'Oren Town Square': (82956, 53162, -1495, 500),
    'Outlaw Forest': (115200, 42700, -3700, 500),
    'Partisans Hideaway': (43000, 111000, -3700, 500),
    'Path to Forest of Wirrors': (138000, 70000, -3500, 500),
    'Path to Hunters Village': (127300, 75000, -3600, 500),
    'Piligrims Necropolis': (-21600, 77400, -5100, 500),
    'Plains of Dion': (26500, 161000, -3600, 500),
    'Plains of Fierce Battle': (152000, 35000, -3500, 500),
    'Plains of Glory': (142000, 45000, -3500, 500),
    'Plunderouse plains': (76500, -114000, -2100, 500),
    'Ruins of Agony': (-21700, 140400, -3700, 500),
    'Ruins of Despair': (-16800, 166700, -3700, 500),
    'Rune Castle Gate': (41000, -38000, -1000, 500),
    'Rune Castle Town': (43835, -47749, -792, 500),
    'Rune Town Square': (43835, -47749, -792, 500),
    'Schuttgart Castle Gate': (78000, -152000, -1400, 500),
    'Schuttgart Town Square': (87386, -143246, -1293, 500),
    'Sea of Spores': (63200, 27300, -3700, 500),
    'South Path to The Cementery': (172000, 20000, -3200, 500),
    'Southern Pathway of Enchanted Valley': (112200, 64500, -3500, 500),
    'Swamp of Screams - East Enterance': (68100, -56700, -3100, 500),
    'Swamp of Screams - West Enterance': (78700, -55400, -3000, 500),
    'Swamp of screams': (78700, -55400, -3000, 500),
    'Tannor canyon': (30000, 175000, -3600, 500),
    'Tanor canyon': (30000, 175000, -3600, 500),
    'The Ant Nest': (-11800, 183500, -3700, 500),
    'The Enchanted Walley - north': (114100, 42000, -3600, 500),
    'The Enchanted Walley - south': (112200, 64500, -3500, 500),
    'The Front of Anghel Waterfall': (162000, 31000, -3700, 500),
    'The Giant\'s Cave': (186200, 61400, -4100, 500),
    'Tower of Insolence': (113400, 16500, 1000, 500),
    'Town of Aden': (147926, 28164, -2268, 500),
    'Town of Oren': (82956, 53162, -1495, 500),
    'Valley of saints': (80800, -54200, -1500, 500),
    'Valley of the Saints': (80800, -54200, -1500, 500),
    'Varka Silenos Outpost': (124000, -107000, -2400, 500),
    'Varkas Silenos outpost': (124000, -107000, -2400, 500),
    'Wall of Argos': (170500, -60000, -2800, 500),
    'West Path to The Cementery': (172000, 20000, -3200, 500),
    'Windawood Manor': (-22800, 201300, -3700, 500),
    'Windawood manson': (-22800, 201300, -3700, 500)
}

# Parse current
tree_cur = ET.parse('data/xml/world/teleports.xml')
teleports_map = {}
for el in tree_cur.getroot().findall('.//teleport'):
    tid = int(el.attrib['id'])
    teleports_map[tid] = el.attrib

print(f"Initial teleports in teleports.xml: {len(teleports_map)}")

# Merge L2Interlude-main
tree_inter = ET.parse('../L2Interlude-main/L2Interlude-main/game/data/xml/teleports.xml')
added_inter = 0
for el in tree_inter.getroot().findall('.//teleport'):
    tid = int(el.attrib['id'])
    if tid not in teleports_map:
        teleports_map[tid] = el.attrib
        added_inter += 1

print(f"Added from L2Interlude-main: {added_inter}, total now: {len(teleports_map)}")

# Scan HTML for missing gotos
html_gotos = {}
for f in glob.glob('data/html/**/*.htm', recursive=True):
    try:
        content = open(f, 'r', encoding='utf-8', errors='ignore').read()
        for m in re.finditer(r'<a\s+[^>]*action=["\']bypass\s+-h\s+(?:npc_%objectId%_)?goto\s+(\d+)["\'][^>]*>(.*?)</a>', content, re.IGNORECASE):
            tid = int(m.group(1))
            if tid not in teleports_map and tid not in html_gotos:
                raw_text = m.group(2).strip()
                clean_text = re.sub(r'<[^>]+>', '', raw_text)
                clean_text = re.sub(r'-\s*\d+(\.\d+)?\s*(adena)?', '', clean_text, flags=re.IGNORECASE).strip()
                html_gotos[tid] = clean_text
    except Exception:
        pass

print(f"Missing HTML gotos to populate: {len(html_gotos)}")

unmapped = []
for tid, name in html_gotos.items():
    if name in COORD_MAP:
        x, y, z, price = COORD_MAP[name]
        teleports_map[tid] = {
            'id': str(tid),
            'loc_x': str(x),
            'loc_y': str(y),
            'loc_z': str(z),
            'price': str(price),
            'fornoble': '0'
        }
    else:
        unmapped.append((tid, name))

if unmapped:
    print(f"WARNING: unmapped destinations: {unmapped}")
else:
    print("ALL missing HTML gotos mapped successfully!")

print(f"Final teleports count: {len(teleports_map)}")

# Rebuild XML
root = ET.Element('list')
for tid in sorted(teleports_map.keys()):
    attribs = teleports_map[tid]
    t_el = ET.SubElement(root, 'teleport', {
        'id': str(attribs['id']),
        'loc_x': str(attribs['loc_x']),
        'loc_y': str(attribs['loc_y']),
        'loc_z': str(attribs['loc_z']),
        'price': str(attribs['price']),
        'fornoble': str(attribs.get('fornoble', '0'))
    })

# Format nicely
def indent(elem, level=0):
    i = "\n" + level * "  "
    if len(elem):
        if not elem.text or not elem.text.strip():
            elem.text = i + "  "
        if not elem.tail or not elem.tail.strip():
            elem.tail = i
        for elem in elem:
            indent(elem, level + 1)
        if not elem.tail or not elem.tail.strip():
            elem.tail = i
    else:
        if level and (not elem.tail or not elem.tail.strip()):
            elem.tail = i

indent(root)
xml_str = '<?xml version="1.0" encoding="UTF-8"?>\n' + ET.tostring(root, encoding='utf-8').decode('utf-8')
with open('data/xml/world/teleports.xml', 'w', encoding='utf-8') as out:
    out.write(xml_str)

print("Saved updated data/xml/world/teleports.xml successfully!")
