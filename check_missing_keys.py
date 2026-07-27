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
    
    # Get the missing keys list from the first language (they're all the same)
    first_lang = 'de'
    first_lang_file = os.path.join(lang_dir, f'Messages_{first_lang}.properties')
    de_keys = read_properties_file(first_lang_file)
    
    missing = []
    for key in en_keys:
        if key not in de_keys:
            missing.append(key)
    
    print("\n" + "="*80)
    print("ALL MISSING KEYS FROM ALL LANGUAGE PACKS (sample from German):")
    print("="*80)
    for key in missing:
        print(f"  - {key}")
    
    print(f"\nTotal missing keys: {len(missing)}")
    
    # Group missing keys by category
    mainwindow_missing = [k for k in missing if k.startswith('mainwindow.')]
    timeline_missing = [k for k in missing if k.startswith('timeline.')]
    shape_missing = [k for k in missing if k.startswith('shape.')]
    
    print("\n" + "="*80)
    print("CATEGORY BREAKDOWN:")
    print("="*80)
    print(f"  mainwindow.*: {len(mainwindow_missing)}")
    for k in mainwindow_missing:
        print(f"    - {k}")
    
    print(f"\n  timeline.*: {len(timeline_missing)}")
    for k in timeline_missing:
        print(f"    - {k}")
    
    print(f"\n  shape.*: {len(shape_missing)}")
    for k in shape_missing[:20]:
        print(f"    - {k}")
    if len(shape_missing) > 20:
        print(f"    ... and {len(shape_missing) - 20} more")

if __name__ == '__main__':
    main()
