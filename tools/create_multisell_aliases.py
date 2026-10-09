import shutil
from pathlib import Path

ms_dir = Path("data/xml/multisell")

# 1. Copiar 20060..20070 para 060..070 e 60..70
for i in range(60, 71):
    src = ms_dir / f"200{i}.xml"
    if src.exists():
        dst1 = ms_dir / f"{i:03d}.xml"
        dst2 = ms_dir / f"{i}.xml"
        if not dst1.exists():
            shutil.copyfile(src, dst1)
            print(f"Criado {dst1.name} a partir de {src.name}")
        if not dst2.exists():
            shutil.copyfile(src, dst2)
            print(f"Criado {dst2.name} a partir de {src.name}")

# Para 71 a 75 (receitas de life crystals), usar 20070.xml
src_70 = ms_dir / "20070.xml"
if src_70.exists():
    for i in range(71, 76):
        dst1 = ms_dir / f"{i:03d}.xml"
        dst2 = ms_dir / f"{i}.xml"
        if not dst1.exists():
            shutil.copyfile(src_70, dst1)
            print(f"Criado {dst1.name} a partir de {src_70.name}")
        if not dst2.exists():
            shutil.copyfile(src_70, dst2)
            print(f"Criado {dst2.name} a partir de {src_70.name}")

# 2. Copiar Ketra e Varka aliases
faction_maps = {
    519: "313750003.xml",
    520: "313820003.xml",
    521: "526.xml",
    522: "313750001.xml",
    523: "313750002.xml",
    524: "313820001.xml",
    525: "313820002.xml",
}

for short_id, src_name in faction_maps.items():
    src = ms_dir / src_name
    if src.exists():
        dst = ms_dir / f"{short_id}.xml"
        if not dst.exists():
            shutil.copyfile(src, dst)
            print(f"Criado {dst.name} a partir de {src.name}")

print("Concluído!")
