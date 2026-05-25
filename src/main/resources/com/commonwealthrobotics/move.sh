#!/bin/bash
# theme-images.sh
# Run this from the directory containing your image files.
# Moves originals into light/, creates dark-transformed copies in dark/.
# Requires: python3, Pillow (pip install Pillow)

set -e

mkdir -p light dark

# ── Python color transformation ──────────────────────────────────────────────
cat > /tmp/dark_transform.py << 'PYEOF'
import sys
import numpy as np
from PIL import Image

def transform(src, dst):
    img = Image.open(src).convert("RGBA")
    data = np.array(img, dtype=np.float32)
    out  = data.copy()

    r, g, b, a = data[:,:,0], data[:,:,1], data[:,:,2], data[:,:,3]
    visible = a > 10

    # Near-black → white
    # Catches black, dark grey, and very dark colours
    black = visible & (r < 60) & (g < 60) & (b < 60)
    out[black, 0] = 255
    out[black, 1] = 255
    out[black, 2] = 255

    # Dark blue #263d8c (R:38 G:61 B:140) → light blue #89b4fa (R:137 G:180 B:250)
    # Fuzz range catches anti-aliased edges around blue fills
    blue = (
        visible & ~black &
        (r >  10) & (r <  80) &
        (g >  30) & (g < 110) &
        (b > 100) & (b < 185) &
        (b > r + 50)           # must be dominantly blue, not purple/teal
    )
    out[blue, 0] = 137
    out[blue, 1] = 180
    out[blue, 2] = 250

    # Reds and greens are left untouched

    Image.fromarray(out.astype(np.uint8), 'RGBA').save(dst)
    print(f"  ✓  {src}  →  {dst}")

transform(sys.argv[1], sys.argv[2])
PYEOF

# ── Image list ────────────────────────────────────────────────────────────────
IMAGES=(
    # MainWindow
    "newRobot.png"
    "CADoodleHome.png"
    "biglogo.png"
    "home.png"
    "fit.png"
    "zoomIn.png"
    "zoomOut.png"
    "holes.png"
    "Sketcher.png"
    "ruler.png"
    "notes.png"
    "search.png"
    "distribute.png"
    "boltHolePattern.png"
    "extrude.png"
    "fillet.png"
    "showMenu.png"
    "intersectMenu.png"
    "group.png"
    "ungroup.png"
    "align.png"
    "mirror.png"
    "magnet.png"
    "workplaneToObject.png"
    "drop.png"
    "copy.png"
    "paste.png"
    "trash.png"
    "back.png"
    "forward.png"
    # ExportPanel
    "Script-Tab-Stl.png"
    "Script-Tab-SVG.png"
    "Script-Tab-Blender.png"
    "Script-Tab-FreeCAD.png"
    "ObjImg.png"
    "3mf.png"
    "export-button.png"
    # ProjectPanel
    "newFile.png"
)

# ── Process ───────────────────────────────────────────────────────────────────
MISSING=()
for img in "${IMAGES[@]}"; do
    if [ -f "$img" ]; then
        mv "$img" "light/$img"
        python3 /tmp/dark_transform.py "light/$img" "dark/$img"
    else
        echo "  ✗  $img not found — skipping"
        MISSING+=("$img")
    fi
done

rm /tmp/dark_transform.py

echo ""
echo "Done. ${#MISSING[@]} file(s) not found."
if [ ${#MISSING[@]} -gt 0 ]; then
    printf '  - %s\n' "${MISSING[@]}"
fi
