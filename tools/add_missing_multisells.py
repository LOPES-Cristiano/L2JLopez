import os, shutil

# 1. 544 and 545 from Lucera2
src_544 = '../L2JLucera2/L2JLucera2/game/data/multisell/544.xml'
src_545 = '../L2JLucera2/L2JLucera2/game/data/multisell/545.xml'
dst_dir = 'data/xml/multisell'

if os.path.exists(src_544):
    shutil.copy(src_544, os.path.join(dst_dir, '544.xml'))
    shutil.copy(src_544, os.path.join(dst_dir, '320825001.xml'))
    print("Copied 544.xml and 320825001.xml")

if os.path.exists(src_545):
    shutil.copy(src_545, os.path.join(dst_dir, '545.xml'))
    shutil.copy(src_545, os.path.join(dst_dir, '320825002.xml'))
    print("Copied 545.xml and 320825002.xml")

# 2. 317325003.xml - Exchange Life Crystals for Adventurer's Mark (8176)
content_317325003 = """<?xml version='1.0' encoding='utf-8'?>
<!-- Adventure guildsman - Exchange previous Life Crystals for new Life Crystals -->
<list>
	<item id="1"><ingredient id="8158" count="1" /><production id="8176" count="1" /></item>
	<item id="2"><ingredient id="8159" count="1" /><production id="8176" count="2" /></item>
	<item id="3"><ingredient id="8160" count="1" /><production id="8176" count="3" /></item>
	<item id="4"><ingredient id="8161" count="1" /><production id="8176" count="1" /></item>
	<item id="5"><ingredient id="8162" count="1" /><production id="8176" count="2" /></item>
	<item id="6"><ingredient id="8163" count="1" /><production id="8176" count="3" /></item>
	<item id="7"><ingredient id="8164" count="1" /><production id="8176" count="1" /></item>
	<item id="8"><ingredient id="8165" count="1" /><production id="8176" count="2" /></item>
	<item id="9"><ingredient id="8166" count="1" /><production id="8176" count="3" /></item>
	<item id="10"><ingredient id="8167" count="1" /><production id="8176" count="1" /></item>
	<item id="11"><ingredient id="8168" count="1" /><production id="8176" count="2" /></item>
	<item id="12"><ingredient id="8169" count="1" /><production id="8176" count="3" /></item>
	<item id="13"><ingredient id="8170" count="1" /><production id="8176" count="1" /></item>
	<item id="14"><ingredient id="8171" count="1" /><production id="8176" count="2" /></item>
	<item id="15"><ingredient id="8172" count="1" /><production id="8176" count="3" /></item>
</list>
"""
with open(os.path.join(dst_dir, '317325003.xml'), 'w', encoding='utf-8') as f:
    f.write(content_317325003)
print("Created 317325003.xml")

# 3. Fortress multisells 36114001, 36114002, 36114003, 36114004, 36142001
content_36114001 = """<?xml version='1.0' encoding='utf-8'?>
<!-- Fortress Support Unit Captain - Necessary Items -->
<list>
	<item id="1"><ingredient id="57" count="1000" /><production id="736" count="1" /></item>
	<item id="2"><ingredient id="57" count="5000" /><production id="1538" count="1" /></item>
	<item id="3"><ingredient id="57" count="10000" /><production id="3936" count="1" /></item>
	<item id="4"><ingredient id="57" count="15000" /><production id="1835" count="10" /></item>
</list>
"""
with open(os.path.join(dst_dir, '36114001.xml'), 'w', encoding='utf-8') as f:
    f.write(content_36114001)

content_36114002 = """<?xml version='1.0' encoding='utf-8'?>
<!-- Fortress Support Unit Captain - Bracelets -->
<list>
	<item id="1"><ingredient id="57" count="50000" /><production id="9201" count="1" /></item>
	<item id="2"><ingredient id="57" count="100000" /><production id="9202" count="1" /></item>
</list>
"""
with open(os.path.join(dst_dir, '36114002.xml'), 'w', encoding='utf-8') as f:
    f.write(content_36114002)

content_36114003 = """<?xml version='1.0' encoding='utf-8'?>
<!-- Fortress Support Unit Captain - Shirts -->
<list>
	<item id="1"><ingredient id="57" count="20000" /><production id="21" count="1" /></item>
	<item id="2"><ingredient id="57" count="50000" /><production id="22" count="1" /></item>
</list>
"""
with open(os.path.join(dst_dir, '36114003.xml'), 'w', encoding='utf-8') as f:
    f.write(content_36114003)

content_36114004 = """<?xml version='1.0' encoding='utf-8'?>
<!-- Fortress Support Unit Captain - Enchanted Shirts Trade -->
<list>
	<item id="1"><ingredient id="22" count="1" /><ingredient id="57" count="50000" /><production id="23" count="1" /></item>
</list>
"""
with open(os.path.join(dst_dir, '36114004.xml'), 'w', encoding='utf-8') as f:
    f.write(content_36114004)

content_36142001 = """<?xml version='1.0' encoding='utf-8'?>
<!-- Fortress Merchant - Battlefield Items -->
<list>
	<item id="1"><ingredient id="57" count="1000" /><production id="1060" count="5" /></item>
	<item id="2"><ingredient id="57" count="2000" /><production id="1061" count="5" /></item>
	<item id="3"><ingredient id="57" count="5000" /><production id="1538" count="1" /></item>
</list>
"""
with open(os.path.join(dst_dir, '36142001.xml'), 'w', encoding='utf-8') as f:
    f.write(content_36142001)

# 4. Tournament multisell
content_tournament = """<?xml version='1.0' encoding='utf-8'?>
<!-- Arena Duel / Tournament Shop - Exchange Medal (3470) -->
<list>
	<item id="1"><ingredient id="3470" count="5" /><production id="1538" count="5" /></item>
	<item id="2"><ingredient id="3470" count="10" /><production id="3936" count="5" /></item>
	<item id="3"><ingredient id="3470" count="25" /><production id="6577" count="1" /></item>
	<item id="4"><ingredient id="3470" count="25" /><production id="6578" count="1" /></item>
	<item id="5"><ingredient id="3470" count="50" /><production id="6622" count="1" /></item>
</list>
"""
with open(os.path.join(dst_dir, 'tournament.xml'), 'w', encoding='utf-8') as f:
    f.write(content_tournament)
print("Created fortress and tournament multisell XML files!")
