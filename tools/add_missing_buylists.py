import xml.etree.ElementTree as ET

tree = ET.parse('data/xml/world/buylists.xml')
root = tree.getroot()

b8 = None
b400 = None
for b in root.findall('.//buylist'):
    if b.attrib.get('id') == '8':
        b8 = b
    elif b.attrib.get('id') == '400':
        b400 = b

b10 = ET.SubElement(root, 'buylist', {'id': '10', 'npcId': '30093'})
for p in b8.findall('.//product'):
    ET.SubElement(b10, 'product', dict(p.attrib))

b3200700 = ET.SubElement(root, 'buylist', {'id': '3200700', 'npcId': '32007'})
for p in b400.findall('.//product'):
    ET.SubElement(b3200700, 'product', dict(p.attrib))

def indent(elem, level=0):
    i = '\n' + level * '  '
    if len(elem):
        if not elem.text or not elem.text.strip():
            elem.text = i + '  '
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
with open('data/xml/world/buylists.xml', 'w', encoding='utf-8') as out:
    out.write(xml_str)

print('Added buylists 10 and 3200700 successfully!')
