with open('tools/siege_missing_gen.txt', 'r', encoding='utf-8') as f:
    text = f.read()

parts = text.split('// LOADERS\n')
fields_part = parts[0].replace('// FIELDS\n', '')
loaders_part = parts[1]

target_file = 'src/main/java/com/lopez/l2j/config/Config.java'
with open(target_file, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Inserir declarações de campos
target_field_anchor = 'public static boolean DISABLE_CHANGE_SIEGE_TIME = false;\n'
if target_field_anchor not in content:
    raise Exception(f'Anchor not found: {target_field_anchor}')

content = content.replace(target_field_anchor, target_field_anchor + '\n\t// SIEGE TOWERS, ARTEFACTS & FORTRESSES (siege.properties)\n' + fields_part)

# 2. Inserir carregamento
target_load_anchor = 'DISABLE_CHANGE_SIEGE_TIME = ConfigLoader.getBoolean("DisableChangeSiegeTime", false);\n'
if target_load_anchor not in content:
    raise Exception(f'Anchor not found: {target_load_anchor}')

content = content.replace(target_load_anchor, target_load_anchor + '\n\t\t// Siege towers, artefacts & fortresses (siege.properties)\n' + loaders_part)

with open(target_file, 'w', encoding='utf-8') as f:
    f.write(content)

print('Successfully applied siege properties to Config.java!')
