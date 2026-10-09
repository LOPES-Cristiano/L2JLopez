import os
import re

actor_dir = r'd:\Cristiano\Lineage\L2JDreamV2\java\com\dream\game\model\actor'
results = {}

for root, dirs, files in os.walk(actor_dir):
    for f in files:
        if f.endswith('.java'):
            path = os.path.join(root, f)
            with open(path, 'r', encoding='utf-8', errors='ignore') as fp:
                code = fp.read()
                
                # Search getHtmlPath
                gh_match = re.findall(r'(public\s+String\s+getHtmlPath\([^)]*\)\s*\{[\s\S]*?\n\t\})', code)
                # Search showChatWindow
                sc_match = re.findall(r'(public\s+void\s+showChatWindow\([^)]*\)\s*\{[\s\S]*?\n\t\})', code)
                
                if gh_match or sc_match:
                    results[f] = {
                        'getHtmlPath': gh_match,
                        'showChatWindow': sc_match
                    }

print(f"Found {len(results)} classes with custom html handling in L2JDreamV2:")
for fname, data in sorted(results.items()):
    print(f"\n==================== {fname} ====================")
    for m in data['getHtmlPath']:
        print("--- getHtmlPath ---")
        print(m[:500])
    for m in data['showChatWindow']:
        print("--- showChatWindow ---")
        print(m[:500])
