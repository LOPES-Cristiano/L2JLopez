import os

tele_dir = r'd:\Cristiano\Lineage\L2JLopez\data\html\teleporter'

# 31211: Race Track Guide
racetrack_html = """<html><body>Race Track Guide:<br>
Welcome to the Monster Derby Track! Here you can watch the races, buy betting tickets, or relax.<br>
When you are ready to return to your departure point, I will send you back safely.<br>
<a action="bypass -h npc_%objectId%_Quest 1101_teleport_to_race_track">Return to Departure Town</a>
</body></html>"""
with open(os.path.join(tele_dir, '31211.htm'), 'w', encoding='utf-8') as f:
    f.write(racetrack_html)

# 31212 to 31224: Event Gatekeepers
event_html = """<html><body>Event Gatekeeper:<br>
Who seeks the Throne of Chaos at the dawn of this new era?<br>
During the event period, I can teleport you to nearby hunting grounds at a discounted rate!<br>
<a action="bypass -h npc_%objectId%_Chat 1">Teleport</a><br>
<a action="bypass -h npc_%objectId%_Quest">Quest</a>
</body></html>"""
for nid in range(31212, 31225):
    with open(os.path.join(tele_dir, f'{nid}.htm'), 'w', encoding='utf-8') as f:
        f.write(event_html)

# 31861: Gatekeeper
gk_html = """<html><body>Gatekeeper:<br>
Greetings! May the light guide your journeys.<br>
Where would you like to travel today?<br>
<a action="bypass -h npc_%objectId%_Chat 1">Teleport</a><br>
<a action="bypass -h npc_%objectId%_Quest">Quest</a>
</body></html>"""
with open(os.path.join(tele_dir, '31861.htm'), 'w', encoding='utf-8') as f:
    f.write(gk_html)

print('Added missing teleporter htmls successfully.')
