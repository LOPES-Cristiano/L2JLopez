import glob, os, re

existing_multisells = set()
for f in glob.glob('data/xml/multisell/**/*.xml', recursive=True):
    base = os.path.splitext(os.path.basename(f))[0]
    existing_multisells.add(base.lower())
    try:
        existing_multisells.add(str(int(base)))
    except Exception:
        pass

all_xml_files = glob.glob('data/xml/multisell/**/*.xml', recursive=True)
print(f"Existing multisell XMLs in data/xml/multisell: {len(all_xml_files)}")

html_multisells = {}
for f in glob.glob('data/html/**/*.htm', recursive=True):
    try:
        content = open(f, 'r', encoding='utf-8', errors='ignore').read()
        for m in re.finditer(r'bypass\s+-h\s+(?:npc_%objectId%_)?multisell\s+([0-9a-zA-Z_]+)', content, re.IGNORECASE):
            mid = m.group(1)
            if mid not in html_multisells:
                html_multisells[mid] = f
    except Exception:
        pass

print(f"HTML multisell references: {len(html_multisells)}")

missing = []
for mid, src in html_multisells.items():
    m_clean = mid.lower()
    matched = False
    if m_clean in existing_multisells:
        matched = True
    else:
        try:
            val = int(m_clean)
            if str(val) in existing_multisells:
                matched = True
            elif f"{val:03d}" in existing_multisells:
                matched = True
            elif f"{val:04d}" in existing_multisells:
                matched = True
        except Exception:
            pass
    if not matched:
        missing.append((mid, src))

print(f"Missing multisells: {len(missing)}")
for mid, src in sorted(missing):
    print(f"  {mid} (from {src})")
