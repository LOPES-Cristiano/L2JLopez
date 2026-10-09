import re

with open('config/game/main/siege.properties', 'r', encoding='utf-8') as f:
    lines = f.readlines()

fields = []
loaders = []

for line in lines:
    line = line.strip()
    if not line or line.startswith('#'):
        continue
    if '=' in line:
        k, v = line.split('=', 1)
        k = k.strip()
        v = v.strip()
        # determine type
        if v.lower() in ('true', 'false'):
            jtype = 'boolean'
            jval = v.lower()
            getter = f'ConfigLoader.getBoolean("{k}", {jval})'
        elif v.isdigit() or (v.startswith('-') and v[1:].isdigit()):
            jtype = 'int'
            jval = v
            getter = f'ConfigLoader.getInt("{k}", {jval})'
        else:
            jtype = 'String'
            esc_v = v.replace('\\', '\\\\').replace('"', '\\"')
            jval = f'"{esc_v}"'
            getter = f'ConfigLoader.getProperty("{k}", "{esc_v}")'
        
        s1 = re.sub(r'(.)([A-Z][a-z]+)', r'\1_\2', k)
        var_name = re.sub(r'([a-z0-9])([A-Z])', r'\1_\2', s1).upper()
        var_name = re.sub(r'([A-Z]+)([0-9]+)', r'\1_\2', var_name)
        
        fields.append((jtype, var_name, jval))
        loaders.append((var_name, getter))

print(f'Total properties in siege.properties: {len(fields)}')
with open('tools/siege_gen.txt', 'w', encoding='utf-8') as out:
    out.write('// FIELDS\n')
    for jtype, vname, jval in fields:
        out.write(f'\tpublic static {jtype} {vname} = {jval};\n')
    out.write('\n// LOADERS\n')
    for vname, getter in loaders:
        out.write(f'\t\t{vname} = {getter};\n')
print('Wrote tools/siege_gen.txt successfully')
