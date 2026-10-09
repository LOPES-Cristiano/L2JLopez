import os, re

config_dir = 'config'
src_dir = 'src/main/java'

props_by_file = {}
all_prop_keys = set()
prop_to_files = {}

for root, dirs, files in os.walk(config_dir):
    for f in files:
        if f.endswith('.properties'):
            pf = os.path.join(root, f)
            rel = os.path.relpath(pf, config_dir).replace('\\', '/')
            keys = []
            with open(pf, 'r', encoding='utf-8', errors='ignore') as file:
                for line in file:
                    line = line.strip()
                    if line and not line.startswith('#') and not line.startswith('!'):
                        if '=' in line:
                            k = line.split('=', 1)[0].strip()
                            keys.append(k)
                            all_prop_keys.add(k)
                            prop_to_files.setdefault(k, []).append(rel)
            props_by_file[rel] = keys

config_java_path = 'src/main/java/com/lopez/l2j/config/Config.java'
config_loader_path = 'src/main/java/com/lopez/l2j/config/ConfigLoader.java'

with open(config_java_path, 'r', encoding='utf-8', errors='ignore') as f:
    config_code = f.read()

with open(config_loader_path, 'r', encoding='utf-8', errors='ignore') as f:
    loader_code = f.read()

combined_config_code = config_code + "\n" + loader_code

# Detectar quais chaves de properties aparecem como literal de string nos arquivos de config
keys_in_config = set()
for k in all_prop_keys:
    if f'"{k}"' in combined_config_code or f'"{k.lower()}"' in combined_config_code.lower():
        keys_in_config.add(k)

missing_in_config = all_prop_keys - keys_in_config

# Extrair campos estáticos de Config.java
static_fields = set()
for line in config_code.splitlines():
    match = re.search(r'public\s+static\s+(?:final\s+)?(?:volatile\s+)?[\w<>\[\],\s]+\s+([A-Za-z0-9_]+)\s*(=|;)', line)
    if match:
        fn = match.group(1)
        if fn not in ('instance', 'log', 'TAG'):
            static_fields.add(fn)

# Extrair todos os tokens de todo o codebase Java
all_java_tokens = set()
for root, dirs, files in os.walk(src_dir):
    for f in files:
        if f.endswith('.java'):
            filepath = os.path.join(root, f)
            if 'config/Config.java' in filepath.replace('\\', '/') or 'config/ConfigLoader.java' in filepath.replace('\\', '/'):
                continue
            with open(filepath, 'r', encoding='utf-8', errors='ignore') as jf:
                text = jf.read()
                tokens = re.findall(r'\b[A-Za-z0-9_]+\b', text)
                all_java_tokens.update(tokens)

used_fields = static_fields.intersection(all_java_tokens)
unused_fields = static_fields - used_fields

print(f"=== RESULTADOS DA AUDITORIA ===")
print(f"Total de arquivos .properties: {len(props_by_file)}")
print(f"Total de chaves brutas em properties: {sum(len(v) for v in props_by_file.values())}")
print(f"Total de chaves distintas em properties: {len(all_prop_keys)}")
print(f"Chaves lidas explicitamente no Config/ConfigLoader: {len(keys_in_config)} ({len(keys_in_config)*100.0/len(all_prop_keys):.1f}%)")
print(f"Chaves NÃO lidas diretamente: {len(missing_in_config)}")
print(f"Campos estáticos declarados em Config.java: {len(static_fields)}")
print(f"Campos estáticos de Config USADOS em outros subsistemas: {len(used_fields)}")
print(f"Campos estáticos de Config NÃO USADOS (apenas declarados/carregados): {len(unused_fields)}")

# Relatório detalhado por arquivo .properties
report = []
report.append("# Relatório Detalhado de Cobertura de Configurações")
report.append(f"Total Arquivos: {len(props_by_file)} | Chaves: {len(all_prop_keys)}\n")

for rel, keys in sorted(props_by_file.items()):
    in_cfg = [k for k in keys if k in keys_in_config]
    not_in_cfg = [k for k in keys if k not in keys_in_config]
    pct = len(in_cfg)*100.0/len(keys) if keys else 100.0
    report.append(f"### Arquivo: `{rel}` ({len(keys)} props - {pct:.1f}% ligado)")
    report.append(f"- No Config: {len(in_cfg)} / {len(keys)}")
    if not_in_cfg:
        report.append(f"- Faltando no Config ({len(not_in_cfg)}):")
        for k in not_in_cfg:
            report.append(f"    * {k}")
    report.append("")

with open("tools/audit_report.txt", "w", encoding="utf-8") as out:
    out.write("\n".join(report))

print("\nRelatório completo gravado em tools/audit_report.txt")
