import glob, re

for action in ['goto', 'multisell', 'Buy', 'Sell', 'WithdrawP', 'DepositP']:
    count = 0
    samples = []
    for f in glob.glob('data/html/**/*.htm', recursive=True):
        try:
            c = open(f, 'r', encoding='utf-8', errors='ignore').read()
            for m in re.finditer(r'bypass\s+-h\s+' + action + r'\b', c, re.I):
                count += 1
                if len(samples) < 3:
                    samples.append(f)
        except Exception:
            pass
    print(f"Action '{action}' without npc_%objectId%_: {count} times (e.g. {samples})")
