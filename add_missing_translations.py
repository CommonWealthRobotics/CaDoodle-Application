#!/usr/bin/env python3
import os
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

def write_properties_file(filepath, data):
    """Write a dict to a .properties file."""
    with open(filepath, 'w', encoding='utf-8') as f:
        for key, value in sorted(data.items()):
            f.write(f"{key}={value}\n")

def main():
    lang_dir = '/home/hephaestus/git/CaDoodle-Application/bin/main/lang'
    en_file = os.path.join(lang_dir, 'Messages_en.properties')
    
    if not os.path.exists(en_file):
        print(f"English file not found: {en_file}")
        return
    
    en_keys = read_properties_file(en_file)
    print(f"English keys count: {len(en_keys)}")
    
    added_count = 0
    updated_files = []
    
    for filename in os.listdir(lang_dir):
        if filename.startswith('Messages_') and filename != 'Messages_en.properties':
            filepath = os.path.join(lang_dir, filename)
            lang_data = read_properties_file(filepath)
            
            # Find missing keys or empty values
            missing_keys = []
            for key in en_keys:
                if key not in lang_data or not lang_data[key].strip():
                    missing_keys.append(key)
            
            if missing_keys:
                # Add missing keys with English translation as fallback
                for key in missing_keys:
                    lang_data[key] = en_keys[key]
                
                # Write updated file
                write_properties_file(filepath, lang_data)
                added_count += len(missing_keys)
                updated_files.append((filename, len(missing_keys)))
    
    print("\n" + "="*80)
    print("UPDATED LANGUAGE PACKS:")
    print("="*80)
    for filename, count in sorted(updated_files):
        print(f"  {filename}: Added/Fixed {count} keys")
    
    print(f"\nTotal keys added/fixed: {added_count}")
    print("="*80)

if __name__ == '__main__':
    main()
