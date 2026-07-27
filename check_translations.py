#!/usr/bin/env python3
import os
import sys
from pathlib import Path

def read_properties_file(filepath):
    """Read a .properties file and return a dict of key-value pairs."""
    data = {}
    with open(filepath, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith('#'):
                if '=' in line:
                    key, value = line.split('=', 1)
                    data[key] = value
    return data

def main():
    lang_dir = '/home/hephaestus/git/CaDoodle-Application/bin/main/lang'
    en_file = os.path.join(lang_dir, 'Messages_en.properties')
    
    if not os.path.exists(en_file):
        print(f"English file not found: {en_file}")
        sys.exit(1)
    
    en_keys = read_properties_file(en_file)
    print(f"English keys count: {len(en_keys)}")
    
    missing_translations = []
    extra_keys = {}
    
    for filename in os.listdir(lang_dir):
        if filename.startswith('Messages_') and filename != 'Messages_en.properties':
            filepath = os.path.join(lang_dir, filename)
            lang_data = read_properties_file(filepath)
            lang_code = filename.replace('Messages_', '').replace('.properties', '')
            
            # Check for missing keys
            missing = []
            for key in en_keys:
                if key not in lang_data:
                    missing.append(key)
                elif not lang_data[key].strip():
                    # Key exists but value is empty or whitespace
                    missing.append(key)
            
            # Check for extra keys
            extra = [key for key in lang_data if key not in en_keys]
            
            if missing or extra:
                missing_translations.append({
                    'lang': lang_code,
                    'file': filename,
                    'missing': missing,
                    'extra': extra
                })
    
    if missing_translations:
        print("\n" + "="*80)
        print("MISSING TRANSLATIONS OR KEYS:")
        print("="*80)
        
        for item in sorted(missing_translations, key=lambda x: x['lang']):
            print(f"\n{item['lang']}. {item['file']}")
            print("-" * 60)
            
            if item['missing']:
                print(f"  Missing keys ({len(item['missing'])}):")
                for key in item['missing'][:20]:  # Show first 20
                    print(f"    - {key}")
                if len(item['missing']) > 20:
                    print(f"    ... and {len(item['missing']) - 20} more")
            
            if item['extra']:
                print(f"  Extra keys ({len(item['extra'])}):")
                for key in item['extra'][:20]:
                    print(f"    - {key}")
                if len(item['extra']) > 20:
                    print(f"    ... and {len(item['extra']) - 20} more")
    else:
        print("\nAll language packs have the same keys as English!")
    
    # Summary
    total_langs = len([f for f in os.listdir(lang_dir) if f.startswith('Messages_') and f != 'Messages_en.properties'])
    langs_with_issues = len(missing_translations)
    
    print("\n" + "="*80)
    print("SUMMARY:")
    print(f"  Total language packs: {total_langs}")
    print(f"  Languages with issues: {langs_with_issues}")
    print(f"  Languages complete: {total_langs - langs_with_issues}")
    print("="*80)

if __name__ == '__main__':
    main()
