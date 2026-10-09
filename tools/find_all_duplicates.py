import os
import re
from collections import defaultdict

def main():
    report_lines = []
    def log(msg=""):
        report_lines.append(str(msg))

    log("==================================================================")
    log("1. AUDITING Config.java FOR DUPLICATE FIELDS AND LOADERS")
    log("==================================================================")
    config_path = r"d:\Cristiano\Lineage\L2JLopez\src\main\java\com\lopez\l2j\config\Config.java"
    if os.path.exists(config_path):
        with open(config_path, "r", encoding="utf-8", errors="ignore") as f:
            lines = f.readlines()

        field_pattern = re.compile(r'^\s*public\s+static\s+(?:final\s+)?([\w<>?,\[\]\s]+)\s+([A-Z0-9_]+)\s*(=|;)')
        fields = defaultdict(list)
        
        loader_pattern = re.compile(r'^\s*([A-Z0-9_]+)\s*=\s*ConfigLoader\.get')
        loaders = defaultdict(list)

        for idx, line in enumerate(lines, start=1):
            m = field_pattern.match(line)
            if m:
                ftype, fname = m.group(1).strip(), m.group(2).strip()
                fields[fname].append((idx, ftype, line.strip()))
            
            m_load = loader_pattern.match(line)
            if m_load:
                vname = m_load.group(1)
                loaders[vname].append((idx, line.strip()))

        dup_fields = {k: v for k, v in fields.items() if len(v) > 1}
        log(f"Total unique static fields in Config.java: {len(fields)}")
        log(f"Duplicate static fields in Config.java: {len(dup_fields)}")
        for k, v in dup_fields.items():
            log(f"  Field: {k}")
            for item in v:
                log(f"    Line {item[0]}: {item[2]}")

        dup_loaders = {k: v for k, v in loaders.items() if len(v) > 1}
        log(f"\nDuplicate assignments in Config.init(): {len(dup_loaders)}")
        for k, v in dup_loaders.items():
            log(f"  Variable: {k}")
            for item in v:
                log(f"    Line {item[0]}: {item[1]}")

    log("\n==================================================================")
    log("2. AUDITING .properties FILES FOR DUPLICATE KEYS")
    log("==================================================================")
    config_dir = r"d:\Cristiano\Lineage\L2JLopez\config"
    total_files = 0
    total_dups_intra_file = 0
    global_keys = defaultdict(list)

    for root, dirs, files in sorted(os.walk(config_dir)):
        for file in sorted(files):
            if file.endswith(".properties"):
                total_files += 1
                fpath = os.path.join(root, file)
                rel_path = os.path.relpath(fpath, config_dir).replace("\\", "/")
                file_keys = defaultdict(list)
                
                with open(fpath, "r", encoding="utf-8", errors="ignore") as f:
                    for line_num, line in enumerate(f, start=1):
                        line = line.strip()
                        if not line or line.startswith("#") or line.startswith(";"):
                            continue
                        if "=" in line:
                            key = line.split("=", 1)[0].strip()
                            val = line.split("=", 1)[1].strip()
                            file_keys[key].append((line_num, val))
                            global_keys[key].append((rel_path, line_num, val))

                # Check duplicates in same file
                for k, v in file_keys.items():
                    if len(v) > 1:
                        total_dups_intra_file += 1
                        log(f"Duplicate key in '{rel_path}': '{k}'")
                        for item in v:
                            log(f"   Line {item[0]}: value = '{item[1]}'")

    log(f"\nProcessed {total_files} properties files. Intra-file duplicate keys: {total_dups_intra_file}")

    cross_file_dups = {k: v for k, v in global_keys.items() if len(set(x[0] for x in v)) > 1}
    log(f"\nKeys defined in MULTIPLE different .properties files: {len(cross_file_dups)}")
    for k, v in sorted(cross_file_dups.items()):
        files_involved = sorted(list(set(x[0] for x in v)))
        log(f"  Key '{k}' in {len(files_involved)} files: {', '.join(files_involved)}")
        for x in v:
            log(f"     [{x[0]}:{x[1]}] = '{x[2]}'")

    log("\n==================================================================")
    log("3. AUDITING FOR DUPLICATE CLASS NAMES IN src/main/java")
    log("==================================================================")
    src_dir = r"d:\Cristiano\Lineage\L2JLopez\src\main\java"
    class_map = defaultdict(list)

    for root, dirs, files in os.walk(src_dir):
        for file in files:
            if file.endswith(".java"):
                cname = file[:-5]
                fpath = os.path.join(root, file)
                rel_path = os.path.relpath(fpath, src_dir).replace("\\", "/")
                class_map[cname].append(rel_path)

    dup_classes = {k: v for k, v in class_map.items() if len(v) > 1}
    log(f"Duplicate class names found: {len(dup_classes)}")
    for k, v in sorted(dup_classes.items()):
        log(f"  Class {k}:")
        for p in v:
            log(f"    - {p}")

    log("\n==================================================================")
    log("4. AUDITING XML FILES (teleports.xml, buylists.xml, staticobjects.xml)")
    log("==================================================================")
    # Check teleports.xml for duplicate teleport IDs
    tele_path = r"d:\Cristiano\Lineage\L2JLopez\data\xml\world\teleports.xml"
    if os.path.exists(tele_path):
        tele_ids = defaultdict(list)
        with open(tele_path, "r", encoding="utf-8", errors="ignore") as f:
            for idx, line in enumerate(f, start=1):
                m = re.search(r'id=["\'](\d+)["\']', line)
                if m:
                    tele_ids[m.group(1)].append(idx)
        dup_teles = {k: v for k, v in tele_ids.items() if len(v) > 1}
        log(f"teleports.xml: {len(tele_ids)} total IDs, {len(dup_teles)} duplicate IDs")
        for k, v in list(dup_teles.items())[:10]:
            log(f"  Duplicate teleport id '{k}' at lines: {v}")

    # Check buylists.xml for duplicate list IDs
    buy_path = r"d:\Cristiano\Lineage\L2JLopez\data\xml\world\buylists.xml"
    if os.path.exists(buy_path):
        buy_ids = defaultdict(list)
        with open(buy_path, "r", encoding="utf-8", errors="ignore") as f:
            for idx, line in enumerate(f, start=1):
                m = re.search(r'<buylist\s+id=["\'](\d+)["\']', line)
                if m:
                    buy_ids[m.group(1)].append(idx)
        dup_buys = {k: v for k, v in buy_ids.items() if len(v) > 1}
        log(f"buylists.xml: {len(buy_ids)} total lists, {len(dup_buys)} duplicate IDs")
        for k, v in list(dup_buys.items())[:10]:
            log(f"  Duplicate buylist id '{k}' at lines: {v}")

    out_file = r"d:\Cristiano\Lineage\L2JLopez\tools\duplicates_report.txt"
    with open(out_file, "w", encoding="utf-8") as f:
        f.write("\n".join(report_lines))
    print(f"Report written to {out_file}, {len(report_lines)} lines")

if __name__ == "__main__":
    main()
