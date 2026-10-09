import os
import re
import glob
from pathlib import Path

def main():
    print("=== AUDITORIA COMPLETA: NPCS, QUESTS E MULTISELL ===")
    
    html_dir = Path("data/html")
    xml_ms_dir = Path("data/xml/multisell")
    quest_impl_dir = Path("src/main/java/com/lopez/l2j/game/quest/impl")
    
    # 1. Carregar listas de multisell existentes
    existing_multisells = set()
    for xml_file in xml_ms_dir.glob("*.xml"):
        name = xml_file.stem
        try:
            val = int(name)
            existing_multisells.add(val)
        except ValueError:
            pass
            
    print(f"Total de arquivos XML de multisell em data/xml/multisell: {len(existing_multisells)}")
    
    # 2. Varrer HTMLs procurando chamadas de multisell
    # regex para multisell e exc_multisell
    ms_pattern = re.compile(r'multisell\s+([0-9]+)', re.IGNORECASE)
    
    referenced_multisells = {}
    total_html_scanned = 0
    
    for root, dirs, files in os.walk(html_dir):
        for f in files:
            if f.endswith(".htm") or f.endswith(".html"):
                total_html_scanned += 1
                filepath = os.path.join(root, f)
                try:
                    with open(filepath, "r", encoding="utf-8", errors="ignore") as fp:
                        content = fp.read()
                        matches = ms_pattern.findall(content)
                        for m in matches:
                            ms_id = int(m)
                            if ms_id not in referenced_multisells:
                                referenced_multisells[ms_id] = []
                            rel_path = os.path.relpath(filepath, html_dir)
                            if rel_path not in referenced_multisells[ms_id]:
                                referenced_multisells[ms_id].append(rel_path)
                except Exception as e:
                    pass
                    
    print(f"Total de arquivos HTML escaneados: {total_html_scanned}")
    print(f"Total de IDs distintos de multisell chamados nos HTMLs: {len(referenced_multisells)}")
    
    missing_multisells = {}
    for ms_id, files in referenced_multisells.items():
        if ms_id not in existing_multisells:
            # Verifica formatos com zeros à esquerda (ex: 001.xml para id=1)
            found = False
            for fmt in [f"{ms_id:03d}", f"{ms_id:04d}", f"{ms_id:02d}"]:
                if (xml_ms_dir / f"{fmt}.xml").exists():
                    found = True
                    break
            if not found:
                missing_multisells[ms_id] = files
                
    print(f"IDs de multisell referenciados em HTMLs mas AUSENTES em data/xml/multisell: {len(missing_multisells)}")
    if missing_multisells:
        for ms_id in sorted(missing_multisells.keys()):
            sample_files = missing_multisells[ms_id][:3]
            print(f"  - Multisell {ms_id} faltando! Usado em: {sample_files}")
            
    # 3. Verificar se há multisells no L2JDreamV2 que podem suprir os faltantes
    dream_ms_dir = Path("D:/Cristiano/Lineage/L2JDreamV2/game/data/xml/multisell")
    if dream_ms_dir.exists():
        dream_files = {f.stem: f for f in dream_ms_dir.glob("*.xml")}
        print(f"Verificando {len(missing_multisells)} faltantes no L2JDreamV2 ({len(dream_files)} disponíveis)...")
        found_in_dream = []
        for ms_id in missing_multisells:
            for cand in [str(ms_id), f"{ms_id:03d}", f"{ms_id:04d}", f"{ms_id:02d}"]:
                if cand in dream_files:
                    found_in_dream.append((ms_id, dream_files[cand]))
                    break
        print(f"Dos {len(missing_multisells)} faltantes, {len(found_in_dream)} encontrados no L2JDreamV2!")
        for ms_id, path in found_in_dream:
            print(f"   -> MultiSell {ms_id} disponível em {path.name}")
            
    # 4. Auditoria de Quests e NPCs
    print("\n--- Auditoria de Quests vs NPCs ---")
    quest_files = list(quest_impl_dir.glob("*.java"))
    print(f"Total de classes de quests em quest/impl: {len(quest_files)}")
    
    start_npc_pattern = re.compile(r'addStartNpc\(([^)]+)\)')
    talk_npc_pattern = re.compile(r'addTalkId\(([^)]+)\)')
    
    all_quest_npcs = set()
    quests_without_start_or_talk = []
    
    for qf in quest_files:
        try:
            with open(qf, "r", encoding="utf-8", errors="ignore") as fp:
                code = fp.read()
                has_start = "addStartNpc" in code
                has_talk = "addTalkId" in code
                if not has_start and not has_talk:
                    quests_without_start_or_talk.append(qf.name)
        except Exception:
            pass
            
    print(f"Quests com start/talk devidamente mapeados: {len(quest_files) - len(quests_without_start_or_talk)}/{len(quest_files)}")
    if quests_without_start_or_talk:
        print(f"Quests sem addStartNpc ou addTalkId explícito: {len(quests_without_start_or_talk)}")
        for q in quests_without_start_or_talk[:5]:
            print(f"  - {q}")

if __name__ == "__main__":
    main()
